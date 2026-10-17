/*
 * Copyright (c) 2019-2026 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.mod.events;

import dev.galacticraft.api.registry.AcidTransformItemRegistry;
import dev.galacticraft.api.registry.ExtinguishableBlockRegistry;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.api.universe.celestialbody.landable.Landable;
import dev.galacticraft.api.universe.celestialbody.landable.teleporter.CelestialTeleporter;
import dev.galacticraft.impl.network.s2c.GearInvPayload;
import dev.galacticraft.impl.universe.position.config.OrbitalCelestialPositionConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.Galacticraft;
import dev.galacticraft.mod.accessor.ServerPlayerAccessor;
import dev.galacticraft.mod.config.GCConfigUtil;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.FallingMeteorEntity;
import dev.galacticraft.mod.misc.cape.ServerCapeManager;
import dev.galacticraft.mod.misc.footprint.FootprintManager;
import dev.galacticraft.mod.network.c2s.CapeSelectionPayload;
import dev.galacticraft.mod.network.s2c.FootprintRemovedPacket;
import dev.galacticraft.mod.util.Translations;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class GCEventHandlers {
    public static void init() {
        GCSleepEventHandlers.init();
        GCInteractionEventHandlers.init();

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(GCEventHandlers::onPlayerChangeWorld);
        ServerTickEvents.END_WORLD_TICK.register(GCEventHandlers::onWorldTick);
        ServerTickEvents.END_SERVER_TICK.register(GCEventHandlers::onServerTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            recoverPlayerFromUnavailableDimension(server, handler.player);
            CapeSelectionPayload.sendCapeSnapshot(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerCapeManager.remove(handler.player);
            CapeSelectionPayload.broadcastCapeSnapshot(server);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(GCConfigUtil::clearServer);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onPlayerChangePlanets(
            MinecraftServer server,
            ServerPlayer player,
            CelestialBody<?, ?> body,
            CelestialBody<?, ?> fromBody
    ) {
        if (!(body.type() instanceof Landable landable)) {
            player.connection.disconnect(Component.translatable(Translations.DimensionTp.INVALID_PACKET));
            return;
        }

        if (!player.galacticraft$isCelestialScreenActive()) {
            player.connection.disconnect(Component.translatable(Translations.DimensionTp.INVALID_PACKET));
            return;
        }

        if (player.galacticraft$getCelestialScreenState() != null && !player.galacticraft$getCelestialScreenState().canTravel(server.registryAccess(), fromBody, body)) {
            player.connection.disconnect(Component.translatable(Translations.DimensionTp.INVALID_PACKET));
            return;
        }

        ResourceKey<Level> destinationKey = landable.world(body.config());

        if (GCConfigUtil.isDimensionDisabled(server, destinationKey)) {
            Constant.LOGGER.warn(
                    "Blocked {} from travelling to disabled dimension {}.",
                    player.getScoreboardName(),
                    destinationKey.location()
            );

            player.galacticraft$closeCelestialScreen();
            return;
        }

        ServerLevel destination = server.getLevel(destinationKey);

        if (destination == null) {
            Constant.LOGGER.warn(
                    "Blocked {} from travelling to unavailable dimension {}.",
                    player.getScoreboardName(),
                    destinationKey.location()
            );

            player.galacticraft$closeCelestialScreen();
            return;
        }

        player.galacticraft$closeCelestialScreen();

        ((CelestialTeleporter) landable
                .teleporter(body.config())
                .value())
                .onEnterAtmosphere(
                        destination,
                        player,
                        body,
                        fromBody
                );
    }

    public static void onPlayerChangeWorld(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
        // Send gear inventory to player
        Container inv = player.galacticraft$getGearInv();
        ItemStack[] stacks = new ItemStack[inv.getContainerSize()];
        for (int i = 0; i < inv.getContainerSize(); i++) {
            stacks[i] = inv.getItem(i);
        }

        GearInvPayload packet = new GearInvPayload(player.getId(), stacks);
        ServerPlayNetworking.send(player, packet);
    }

    public static void onWorldTick(ServerLevel level) {
        FootprintManager footprintManager = level.galacticraft$getFootprintManager();
        if (!footprintManager.footprintBlockChanges.isEmpty()) {
            for (GlobalPos targetPoint : footprintManager.footprintBlockChanges) {
                if (level.dimension().location().equals(targetPoint.dimension().location())) {
                    long packedPos = ChunkPos.asLong(targetPoint.pos());
                    PlayerLookup.around(level, targetPoint.pos(), 50).forEach(player -> {
                        ServerPlayNetworking.send(player, new FootprintRemovedPacket(packedPos, targetPoint.pos()));
                    });
                }
            }

            footprintManager.footprintBlockChanges.clear();
        }
        level.galacticraft$getSealerManager().tick();
        GCVenusAtmosphereHandler.onWorldTick(level);
    }

    public static void onServerTick(MinecraftServer server) {
        double configMultiplier = Galacticraft.CONFIG.meteorSpawnMultiplier();
        if (configMultiplier <= 0.0) return;

        // Simulate visual falling meteors across all planetary dimensions (including Earth, Moon, Venus, Mars, Asteroids)
        // as a general function of each dimension's physical and orbital properties.
        PlayerLookup.all(server).forEach(player -> {
            if (player.isSpectator()) return;
            ServerLevel level = player.serverLevel();
            Holder<CelestialBody<?, ?>> celestialBody = level.galacticraft$getCelestialBody();
            if (celestialBody == null) return;

            float frequency = computePlanetaryMeteorFrequency(level, celestialBody);
            if (frequency <= 0.0f) return;

            frequency /= (float) configMultiplier;

            int chance = Math.max(1, (int) (frequency * 750.0f));
            if (level.random.nextInt(chance) == 0) {
                int smallSize = computeSmallMeteorSize(level, celestialBody);
                throwMeteor(server, level, player, smallSize);
            }

            if (level.random.nextInt(chance * 3) == 0) {
                int largeSize = computeLargeMeteorSize(level, celestialBody);
                throwMeteor(server, level, player, largeSize);
            }
        });
    }

    /**
     * Computes the GC4-style meteor frequency parameter {@code f} (where lower {@code f} means more frequent meteors,
     * and {@code chance = f * 750} ticks per player) as a general continuous function of the celestial body's
     * physical and orbital properties:
     * <ul>
     *   <li><b>Gravitational focusing ({@code gravity}):</b> Higher surface gravity increases the planet's effective
     *       capture cross-section {@code 1 + (v_esc / v_inf)^2} for passing bolides.</li>
     *   <li><b>Orbital bolide flux ({@code distanceFromCenter}):</b> Inner-system orbits (&lt; 0.9 AU, e.g. Venus)
     *       experience higher Keplerian impact velocities and dense Atira/Aten/Apollo asteroid crossing flux;
     *       orbits near the Main Asteroid Belt (1.2–1.6 AU, e.g. Mars &amp; Asteroids) experience belt resonance flux.</li>
     *   <li><b>Atmospheric filtering vs. hyperbaric thermal shock ({@code pressure}, {@code temperature}):</b>
     *       <ul>
     *         <li>Vacuum / exosphere bodies ({@code P < 0.05 bar}, e.g. Moon, Mars, Asteroids) allow even small
     *             meteoroids to reach the surface intact ({@code f ≈ 1.4 – 3.2}).</li>
     *         <li>Temperate N2/O2 atmospheres ({@code P ≈ 1 bar, T < 150°C}, e.g. Earth) ablate &gt;95% of small
     *             meteoroids in the mesosphere, making surface meteorite impacts rare ({@code f ≈ 18.0}).</li>
     *         <li>Superheated hyperbaric greenhouse atmospheres ({@code T >= 250°C}, e.g. Venus at 462°C, 92 bar)
     *             coupled with strong gravitational focusing (0.904g) and 0.75 AU inner-system bolide flux experience
     *             frequent incandescent iron/chondrite bolide falls ({@code f ≈ 1.6 – 2.2}, considerably higher than Earth).</li>
     *       </ul>
     *   </li>
     * </ul>
     */
    public static float computePlanetaryMeteorFrequency(ServerLevel level, Holder<CelestialBody<?, ?>> bodyHolder) {
        CelestialBody<?, ?> body = bodyHolder.value();
        float gravity = Math.max(0.0f, body.gravity());
        float pressure = Math.max(0.0f, body.atmosphere().pressure());
        double temperature = body.atmosphere().temperature();

        // 1. Extract orbital distance (lineScale) from celestial position config if available
        double orbitalDistance = 1.0;
        if (body.position().config() instanceof OrbitalCelestialPositionConfig orbitalConfig) {
            // Satellites/moons (planet() == false, e.g. Moon with distance=20.0 orbiting Earth) inherit ~1.0 AU solar flux
            orbitalDistance = orbitalConfig.planet() ? orbitalConfig.distance() : 1.0;
        }

        // 2. Gravitational focusing multiplier: 1.0 + 1.65 * g
        float gravFocusing = 1.0f + 1.65f * gravity;

        // 3. Orbital bolide flux multiplier (inner solar system surge + Main Asteroid Belt resonance)
        float innerFlux = (orbitalDistance < 0.95)
                ? (float) Math.pow(1.0 / Math.max(0.45, orbitalDistance), 2.15)
                : 1.0f;
        double beltDelta = (orbitalDistance - 1.375) / 0.18;
        float beltFlux = 1.0f + 3.2f * (float) Math.exp(-(beltDelta * beltDelta));
        float orbitalFlux = Math.max(innerFlux, beltFlux);

        // 4. Atmospheric survival / penetration factor
        float atmosphericFactor;
        if (pressure < 0.05f) {
            // Vacuum or near-vacuum (Moon, Mars, Asteroids): minimal ablation
            atmosphericFactor = 1.35f / (1.0f + 12.0f * pressure);
        } else if (temperature >= 250.0) {
            // Superheated volcanic/hyperbaric atmosphere (Venus): dense iron-nickel & chondrite bolides
            // fragment and rain incandescent meteorites onto the surface, intensified during storms
            float stormBoost = level.isThundering() ? 1.45f : (level.isRaining() ? 1.25f : 1.0f);
            atmosphericFactor = 0.58f * stormBoost;
        } else {
            // Temperate protective atmosphere (Earth Overworld): burns up >90% of small stony meteoroids
            atmosphericFactor = 0.105f / (float) Math.sqrt(Math.max(0.5f, pressure));
        }

        // Effective impact rate index R (higher R = more frequent impacts)
        float impactRateIndex = gravFocusing * orbitalFlux * atmosphericFactor;

        // Convert to GC4 frequency f = 6.0 / R (clamped to [1.2, 28.0])
        return Mth.clamp(6.0f / Math.max(0.15f, impactRateIndex), 1.2f, 28.0f);
    }

    private static int computeSmallMeteorSize(ServerLevel level, Holder<CelestialBody<?, ?>> bodyHolder) {
        float pressure = bodyHolder.value().atmosphere().pressure();
        // Dense atmospheres ablate tiny 1-size pebbles so surviving meteors are larger (size 2-3)
        if (pressure > 10.0f) {
            return 2 + level.random.nextInt(2);
        }
        return 1 + level.random.nextInt(2);
    }

    private static int computeLargeMeteorSize(ServerLevel level, Holder<CelestialBody<?, ?>> bodyHolder) {
        float pressure = bodyHolder.value().atmosphere().pressure();
        if (pressure > 10.0f) {
            return 4 + level.random.nextInt(4); // Size 4..7 bolides on Venus
        }
        return 3 + level.random.nextInt(4); // Size 3..6 on Moon/Mars/Earth
    }

    private static void recoverPlayerFromUnavailableDimension(
            MinecraftServer server,
            ServerPlayer player
    ) {
        ServerPlayerAccessor accessor = (ServerPlayerAccessor) player;

        ResourceLocation savedDimension =
                accessor.galacticraft$getSavedLoginDimension();

        /*
         * Clear it immediately. We only need this information during login.
         */
        accessor.galacticraft$clearSavedLoginDimension();

        if (savedDimension == null) {
            return;
        }

        ResourceKey<Level> savedKey = ResourceKey.create(
                Registries.DIMENSION,
                savedDimension
        );

        /*
         * This deliberately checks both:
         *
         * 1. Explicitly disabled dimensions.
         * 2. Dimensions which simply no longer exist.
         *
         * The second case also protects players after removing another mod or
         * datapack which previously supplied their current world.
         */
        boolean unavailable = GCConfigUtil.isDimensionDisabled(server, savedKey) || server.getLevel(savedKey) == null;

        if (!unavailable) {
            return;
        }

        ServerLevel overworld = server.overworld();

        BlockPos spawn = overworld.getSharedSpawnPos();

        Constant.LOGGER.warn(
                "Player {} was saved in unavailable dimension {}. " +
                        "Relocating them to the overworld spawn.",
                player.getScoreboardName(),
                savedDimension
        );

        /*
         * Prevent stale state from a rocket/vehicle or celestial selection
         * surviving the recovery.
         */
        player.galacticraft$closeCelestialScreen();

        if (player.isPassenger()) {
            player.stopRiding();
        }

        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        player.teleportTo(
                overworld,
                spawn.getX() + 0.5D,
                spawn.getY() + 0.1D,
                spawn.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );

        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
    }

    private static void throwMeteor(MinecraftServer server, ServerLevel level, Player targetPlayer, int meteorSize) {
        Player nearestPlayer = level.getNearestPlayer(targetPlayer, 100.0);
        if (nearestPlayer != null && nearestPlayer.getId() < targetPlayer.getId()) return;

        int maxRadius = Math.min(96, server.getPlayerList().getViewDistance() * Constant.Chunk.WIDTH - 16);
        if (maxRadius < 24) return;

        // Choose an impact landing zone 20..48 blocks from the player, biased toward the player's field of view
        // so the player can visually watch the meteor streak across the sky and impact nearby!
        float playerYawRad = targetPlayer.getYRot() * Mth.DEG_TO_RAD;
        double lookDirX = -Mth.sin(playerYawRad);
        double lookDirZ = Mth.cos(playerYawRad);
        double angleJitter = (level.random.nextDouble() - 0.5) * (Math.PI * 1.15);
        double cosA = Math.cos(angleJitter);
        double sinA = Math.sin(angleJitter);
        double dirX = lookDirX * cosA - lookDirZ * sinA;
        double dirZ = lookDirX * sinA + lookDirZ * cosA;

        double impactDist = 20.0 + level.random.nextDouble() * Math.min(30.0, maxRadius - 20.0);
        double targetX = targetPlayer.getX() + dirX * impactDist;
        double targetZ = targetPlayer.getZ() + dirZ * impactDist;

        BlockPos targetBlockPos = BlockPos.containing(targetX, targetPlayer.getY(), targetZ);
        if (!level.hasChunkAt(targetBlockPos)) return;

        double targetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetBlockPos.getX(), targetBlockPos.getZ());

        // Choose a high sky entry point 45..78 blocks horizontally offset from the impact zone
        double entryAngle = level.random.nextDouble() * (Math.PI * 2.0);
        double horizontalRun = 45.0 + level.random.nextDouble() * 33.0;
        double spawnX = targetX + Math.cos(entryAngle) * horizontalRun;
        double spawnZ = targetZ + Math.sin(entryAngle) * horizontalRun;
        double spawnY = Math.min(level.getMaxBuildHeight() + 32.0, Math.max(targetPlayer.getY(), targetY) + 110.0 + level.random.nextInt(30));

        if (!level.hasChunkAt(BlockPos.containing(spawnX, targetY, spawnZ))) {
            spawnX = targetX + Math.cos(entryAngle) * 24.0;
            spawnZ = targetZ + Math.sin(entryAngle) * 24.0;
        }

        // Compute flight time under FallingMeteorEntity gravity (g = 0.035) and initial downward velocity (-0.45)
        double dropHeight = Math.max(40.0, spawnY - targetY);
        double initialVy = -0.45;
        double gravity = 0.035;
        // 0.5 * g * t^2 + |initialVy| * t - dropHeight = 0
        double flightTicks = (-Math.abs(initialVy) + Math.sqrt(initialVy * initialVy + 2.0 * gravity * dropHeight)) / gravity;
        flightTicks = Math.max(35.0, flightTicks);

        double deltaX = (targetX - spawnX) / flightTicks;
        double deltaZ = (targetZ - spawnZ) / flightTicks;

        FallingMeteorEntity meteor = new FallingMeteorEntity(GCEntityTypes.FALLING_METEOR, level);
        meteor.setPos(spawnX, spawnY, spawnZ);
        meteor.setDeltaMovement(deltaX, initialVy, deltaZ);
        meteor.setSize(meteorSize);

        level.addFreshEntity(meteor);

        // Audible upper-atmosphere sonic boom rumble so the player knows to look up at the sky
        level.playSound(
                null,
                targetX,
                targetPlayer.getY() + 24.0,
                targetZ,
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER,
                1.6F,
                0.5F + level.random.nextFloat() * 0.2F
        );
    }

    public static boolean extinguishBlock(Level level, BlockPos pos, BlockState oldState) {
        ExtinguishableBlockRegistry.Entry entry = ExtinguishableBlockRegistry.INSTANCE.get(oldState.getBlock());
        if (entry == null) return false;
        BlockState newState = entry.transform(oldState);
        if (newState == null) return false;
        level.setBlockAndUpdate(pos, newState);
        entry.callback(new ExtinguishableBlockRegistry.Context(level, pos, oldState));
        return true;
    }

    public static boolean sulfuricAcidTransformItem(ItemEntity itemEntity, ItemStack original) {
        AcidTransformItemRegistry.Entry entry = AcidTransformItemRegistry.INSTANCE.get(original.getItem());
        if (entry == null) return false;
        ItemStack itemStack = entry.transform(original.copy());
        if (itemStack == null) return false;
        itemEntity.setItem(itemStack);
        entry.callback(new AcidTransformItemRegistry.Context(itemEntity, original));
        return true;
    }
}