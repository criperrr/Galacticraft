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

package dev.galacticraft.mod.content.teleporters;

import com.mojang.serialization.Codec;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.api.universe.celestialbody.landable.teleporter.config.CelestialTeleporterConfig;
import dev.galacticraft.api.universe.celestialbody.landable.teleporter.type.CelestialTeleporterType;
import dev.galacticraft.impl.universe.celestialbody.landable.teleporter.config.DefaultCelestialTeleporterConfig;
import dev.galacticraft.mod.attachments.GCServerPlayer;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.ParachestEntity;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Celestial teleporter for Gas &amp; Ice Giants ({@code galacticraft:aerostatic}) that deploys
 * a safe 7x7 floating aerostatic arrival platform at {@code Y = 216} in the upper atmosphere
 * ({@code 0.85–1.25 bar}) so astronauts and modular rockets never plunge into the crushing abyss.
 */
public class AerostaticCelestialTeleporterType<Config extends CelestialTeleporterConfig> extends CelestialTeleporterType<Config> {
    public static final AerostaticCelestialTeleporterType<DefaultCelestialTeleporterConfig> INSTANCE =
            new AerostaticCelestialTeleporterType<>(DefaultCelestialTeleporterConfig.CODEC);

    public static final int PLATFORM_Y = 216;
    public static final int PLATFORM_HALF_SIZE = 3; // 7x7 platform (-3..+3)

    public AerostaticCelestialTeleporterType(Codec<Config> codec) {
        super(codec);
    }

    /**
     * Generates a 7x7 floating aerostatic landing platform centered at {@code (centerX, PLATFORM_Y, centerZ)}.
     * Only places blocks into air/replaceable spaces so existing player structures are never overwritten.
     */
    public static void generateAerostaticPlatform(ServerLevel level, int centerX, int centerZ) {
        BlockState centerDeck = GCBlocks.ALUMINUM_DECORATION.block().defaultBlockState();
        BlockState innerDeck = GCBlocks.STEEL_DECORATION.block().defaultBlockState();
        BlockState rimDeck = GCBlocks.TIN_DECORATION.block().defaultBlockState();
        BlockState truss = GCBlocks.GANTRY_TRUSS.defaultBlockState();
        BlockState cornerLight = GCBlocks.GLOWSTONE_TORCH.defaultBlockState();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -PLATFORM_HALF_SIZE; dx <= PLATFORM_HALF_SIZE; dx++) {
            for (int dz = -PLATFORM_HALF_SIZE; dz <= PLATFORM_HALF_SIZE; dz++) {
                int wx = centerX + dx;
                int wz = centerZ + dz;
                int chebyshev = Math.max(Math.abs(dx), Math.abs(dz));

                cursor.set(wx, PLATFORM_Y, wz);
                BlockState existing = level.getBlockState(cursor);
                if (existing.isAir() || existing.canBeReplaced()) {
                    BlockState deckState = switch (chebyshev) {
                        case 0, 1 -> centerDeck;
                        case 2 -> innerDeck;
                        default -> rimDeck;
                    };
                    level.setBlock(cursor, deckState, Block.UPDATE_ALL);
                }

                // Aerostatic buoyancy stabilizer trusses beneath the 7x7 deck
                if ((Math.abs(dx) == 2 && Math.abs(dz) == 2) || (dx == 0 && dz == 0)) {
                    cursor.set(wx, PLATFORM_Y - 1, wz);
                    BlockState below = level.getBlockState(cursor);
                    if (below.isAir() || below.canBeReplaced()) {
                        level.setBlock(cursor, truss, Block.UPDATE_ALL);
                    }
                }

                // Corner beacon lights at Y = 217
                if (Math.abs(dx) == PLATFORM_HALF_SIZE && Math.abs(dz) == PLATFORM_HALF_SIZE) {
                    cursor.set(wx, PLATFORM_Y + 1, wz);
                    if (level.getBlockState(cursor).isAir()) {
                        level.setBlock(cursor, cornerLight, Block.UPDATE_ALL);
                    }
                }
            }
        }

        // Ensure clear arrival headspace above the central 3x3 pad
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 3; dy++) {
                    cursor.set(centerX + dx, PLATFORM_Y + dy, centerZ + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir() && state.canBeReplaced()) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    @Override
    public void onEnterAtmosphere(ServerLevel level, Entity entity, CelestialBody<?, ?> body, CelestialBody<?, ?> fromBody, Config config) {
        int centerX = Mth.floor(entity.getX());
        int centerZ = Mth.floor(entity.getZ());

        generateAerostaticPlatform(level, centerX, centerZ);

        if (entity instanceof ServerPlayer player) {
            GCServerPlayer gcPlayer = GCServerPlayer.get(player);
            if (gcPlayer.hasModularRocketGraph()) {
                double entryY = level.getMaxBuildHeight() + 80.0D;
                player.teleportTo(level, centerX + 0.5D, entryY, centerZ + 0.5D, NO_RELATIVE_MOVEMENT, 0.0F, 0.0F);
                ModularRocketFlightEntity descentRocket = new ModularRocketFlightEntity(GCEntityTypes.MODULAR_ROCKET_FLIGHT, level);
                descentRocket.setPos(centerX + 0.5D, entryY, centerZ + 0.5D);
                descentRocket.initializeFlightState(
                        gcPlayer.getModularRocketGraph(),
                        gcPlayer.getModularRocketEnergy(),
                        gcPlayer.getModularRocketOxygen(),
                        Direction.NORTH,
                        null,
                        PLATFORM_Y + 1,
                        ModularRocketFlightEntity.FlightPhase.RETRO_DESCENT,
                        List.of(player.getUUID()),
                        Map.of()
                );
                descentRocket.loadCargoStacks(gcPlayer.getRocketStacks());
                gcPlayer.getRocketStacks().clear();
                gcPlayer.clearModularRocketState();
                if (level.addFreshEntity(descentRocket)) {
                    player.startRiding(descentRocket, true);
                    return;
                }
            }

            // Deliver any carried classic rocket inventory safely onto the aerostatic deck
            NonNullList<ItemStack> rocketInv = gcPlayer.getRocketStacks();
            boolean hasCargo = false;
            for (ItemStack stack : rocketInv) {
                if (!stack.isEmpty()) {
                    hasCargo = true;
                    break;
                }
            }
            if (hasCargo) {
                ParachestEntity chest = new ParachestEntity(GCEntityTypes.PARACHEST, level, rocketInv, DyeColor.LIGHT_BLUE, gcPlayer.getFuel());
                rocketInv.clear();
                chest.setPos(centerX + 2.5D, PLATFORM_Y + 2.0D, centerZ + 0.5D);
                level.addFreshEntity(chest);
            }
        }

        entity.teleportTo(level, centerX + 0.5D, PLATFORM_Y + 1.1D, centerZ + 0.5D, NO_RELATIVE_MOVEMENT, entity.getYRot(), entity.getXRot());
        entity.setDeltaMovement(Vec3.ZERO);
        entity.resetFallDistance();
    }
}
