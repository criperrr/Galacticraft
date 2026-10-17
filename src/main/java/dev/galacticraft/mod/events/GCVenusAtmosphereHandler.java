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

import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.entity.damage.GCDamageTypes;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import dev.galacticraft.mod.world.gen.feature.PlanetaryVolcanoFeature;
import dev.galacticraft.mod.world.gen.noise.DensityCellular2D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class GCVenusAtmosphereHandler {
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static int venusRainTimer = 1200;
    private static boolean venusRaining = false;

    public static boolean isProtected(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return true;

        Container accessories = player.galacticraft$getAccessories();
        if (accessories != null) {
            for (int i = 0; i < accessories.getContainerSize(); i++) {
                if (accessories.getItem(i).is(GCItems.SHIELD_CONTROLLER)) {
                    return true;
                }
            }
        }

        Container thermal = player.galacticraft$getThermalArmor();
        if (thermal != null && thermal.getContainerSize() >= 4) {
            return thermal.getItem(0).is(GCItems.ISOTHERMAL_PADDING_HELMET)
                    && thermal.getItem(1).is(GCItems.ISOTHERMAL_PADDING_CHESTPIECE)
                    && thermal.getItem(2).is(GCItems.ISOTHERMAL_PADDING_LEGGINGS)
                    && thermal.getItem(3).is(GCItems.ISOTHERMAL_PADDING_BOOTS);
        }
        return false;
    }

    public static void onWorldTick(ServerLevel level) {
        if (!level.dimension().equals(GCDimensions.VENUS)) return;

        // Replicates GC4 WorldProviderVenus.updateWeatherOverride():
        // Independent sulfuric acid rain cycle on Venus (1000..4600 ticks raining, 1000..3000 ticks dry)
        if (--venusRainTimer <= 0) {
            venusRaining = !venusRaining;
            if (venusRaining) {
                venusRainTimer = level.random.nextInt(3600) + 1000;
                level.setWeatherParameters(0, venusRainTimer, true, false);
            } else {
                venusRainTimer = level.random.nextInt(2000) + 1000;
                level.setWeatherParameters(venusRainTimer, 0, false, false);
            }
        }

        long gameTime = level.getGameTime();
        boolean tick10 = (gameTime % 10 == 0);
        boolean tick20 = (gameTime % 20 == 0);
        boolean tick40 = (gameTime % 40 == 0);
        boolean isRaining = level.isRaining();

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;

            BlockPos pos = player.blockPosition();
            if (!level.hasChunkAt(pos)) continue;

            boolean exposedToSky = level.canSeeSky(pos);
            boolean breathable = level.isBreathable(pos);
            boolean protectedPlayer = isProtected(player);

            // Ported from TFCVolcanoes (TremorEvent.trySpawnSurfaceSmoke & TFCVHelpers.spawnSmoke):
            // Spawn server-side volcanic smoke plumes & geothermal heat effects around active volcano centers and hot zones
            if (tick10) {
                spawnGeothermalAndVolcanoPlumes(level, player);
            }

            // Supercritical Fluid Drag on the Surface (Y < 180)
            // Supercritical CO2 fluid has density ~65 kg/m3. Apply subtle fluid drag when moving on the ground.
            if (player.getY() < 180.0 && player.onGround() && !breathable) {
                Vec3 delta = player.getDeltaMovement();
                if (Math.abs(delta.x) > 0.001 || Math.abs(delta.z) > 0.001) {
                    player.setDeltaMovement(delta.x * 0.94, delta.y, delta.z * 0.94);
                    player.hurtMarked = true;
                }
                if (player.isSprinting()) {
                    player.causeFoodExhaustion(0.02F);
                }
            }

            // Cloud Layer Super-Rotation Jet Stream (Y >= 220)
            // At high altitudes when exposed to sky, apply westward (-X) wind drift acceleration simulating 360 km/h winds.
            if (player.getY() >= 220.0 && exposedToSky && !player.getAbilities().flying) {
                Vec3 delta = player.getDeltaMovement();
                double newX = Math.max(delta.x - 0.025, -0.6);
                player.setDeltaMovement(newX, delta.y, delta.z);
                player.hurtMarked = true;
            }

            // If player is protected (SHIELD_CONTROLLER or full ISOTHERMAL_PADDING),
            // they are completely immune to acid, heat, and crushing pressure!
            if (protectedPlayer) continue;

            // Ambient corrosive vapor at Y < 180 corrodes unshielded armor durability (1 dura every 40 ticks)
            if (tick40 && !breathable && player.getY() < 180.0) {
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    ItemStack armor = player.getItemBySlot(slot);
                    if (!armor.isEmpty() && armor.isDamageableItem()) {
                        armor.hurtAndBreak(1, player, slot);
                    }
                }
            }

            // 20-tick atmospheric hazards
            if (tick20) {
                // Hyperbaric Pressure System:
                // Surface/crust (Y < 200): If player is not protected, apply barometric crushing damage (2.0F)
                // every 20 ticks and slowness effect (MobEffects.MOVEMENT_SLOWDOWN).
                // Geoffrey Landis Cloud Layer (Y >= 200): Ambient pressure is 1.0 bar (Earth standard!).
                // NO crushing damage at Y >= 200!
                if (!breathable && player.getY() < 200.0) {
                    player.hurt(level.damageSources().generic(), 2.0F);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));

                    // Extra geothermal heat in hot zones or when standing on scorching volcanic blocks
                    double hotspot = DensityCellular2D.sampleThermalHotspot(level.getSeed(), pos.getX(), pos.getZ());
                    BlockState belowState = level.getBlockState(pos.below());
                    if (hotspot > 0.65 || belowState.is(Blocks.MAGMA_BLOCK) || belowState.is(GCBlocks.SCORCHED_VENUS_ROCK) || belowState.is(GCBlocks.VOLCANIC_ROCK)) {
                        player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 60));
                    }
                }

                // Sulfuric acid rain burns unshielded players exposed to the sky
                if (isRaining && exposedToSky) {
                    player.hurt(level.damageSources().source(GCDamageTypes.SULFURIC_ACID), 2.0F);
                }
            }
        }
    }

    /**
     * Ported from {@code TremorEvent.trySpawnSurfaceSmoke} and {@code TFCVHelpers.spawnSmoke}:
     * Emits volcanic smoke plumes over discovered volcano centers and rising geothermal steam/embers in hot zones.
     */
    private static void spawnGeothermalAndVolcanoPlumes(ServerLevel level, ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();

        // 1. Check nearby active volcano centers (ported from TremorEvent.VOLCANO_CENTERS)
        Set<BlockPos> centers = PlanetaryVolcanoFeature.VOLCANO_CENTERS.get(level.dimension());
        if (centers != null) {
            for (BlockPos center : centers) {
                double distSq = center.distSqr(playerPos);
                if (distSq <= 96.0 * 96.0) {
                    for (int i = 0; i < 4; i++) {
                        double sx = center.getX() + (level.random.nextDouble() - 0.5) * 14.0;
                        double sz = center.getZ() + (level.random.nextDouble() - 0.5) * 14.0;
                        double sy = center.getY() + 1.0 + level.random.nextDouble() * 3.0;
                        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, sx, sy, sz, 2, 0.6, 0.8, 0.6, 0.04);
                        level.sendParticles(ParticleTypes.LARGE_SMOKE, sx, sy, sz, 2, 0.5, 0.5, 0.5, 0.05);
                        if (level.random.nextInt(3) == 0) {
                            level.sendParticles(ParticleTypes.LAVA, sx, sy, sz, 1, 0.3, 0.2, 0.3, 0.08);
                        }
                    }
                    if (level.random.nextInt(12) == 0) {
                        level.playSound(null, center, SoundEvents.LAVA_AMBIENT, SoundSource.AMBIENT, 1.4F, 0.55F + level.random.nextFloat() * 0.25F);
                    }
                }
            }
        }

        // 2. Spawn geothermal ground smoke & charring particles in hotter regions around the player
        for (int i = 0; i < 3; i++) {
            int gx = playerPos.getX() + level.random.nextInt(32) - 16;
            int gz = playerPos.getZ() + level.random.nextInt(32) - 16;
            double thermal = DensityCellular2D.sampleThermalHotspot(level.getSeed(), gx, gz);
            if (thermal > 0.58) {
                int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz) - 1;
                BlockPos ground = new BlockPos(gx, gy, gz);
                BlockState state = level.getBlockState(ground);
                if (state.is(Blocks.MAGMA_BLOCK) || state.is(GCBlocks.SCORCHED_VENUS_ROCK) || state.is(GCBlocks.VOLCANIC_ROCK) || state.is(Blocks.BASALT) || thermal > 0.68) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, gx + 0.5, gy + 1.05, gz + 0.5, 2, 0.25, 0.15, 0.25, 0.03);
                    if (level.random.nextInt(3) == 0) {
                        level.sendParticles(ParticleTypes.FLAME, gx + 0.5, gy + 1.05, gz + 0.5, 1, 0.2, 0.05, 0.2, 0.01);
                    }
                }
            }
        }
    }
}
