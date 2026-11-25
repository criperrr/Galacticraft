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

import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Authoritative atmospheric physics handler for Gas &amp; Ice Giants (Jupiter, Saturn, Uranus, Neptune):
 * <ul>
 *   <li>{@code Y = 260..320}: Exosphere / orbital limit (&lt; 0.2–0.5 bar)</li>
 *   <li>{@code Y = 180..260}: Upper Atmosphere / Aerostatic Zone (0.5–2.5 bar, safe for floating bases, arrival at Y=216)</li>
 *   <li>{@code Y = 40..180}: Dense cloud decks, banded zonal jet streams, storm lightning (Jupiter/Saturn), 3–55 bar</li>
 *   <li>{@code Y = -64..-20}: Abyssal hyperbaric core (80–200+ bar), requiring BOTH {@code SHIELD_CONTROLLER} and full {@code ISOTHERMAL_PADDING}</li>
 * </ul>
 */
public final class GCGasGiantAtmosphereHandler {
    public static final double EXOSPHERE_START_Y = 260.0D;
    public static final double AEROSTATIC_MIN_Y = 180.0D;
    public static final double AEROSTATIC_REFERENCE_Y = 216.0D;
    public static final double HYPERBARIC_HAZARD_Y = 175.0D;
    public static final double CLOUD_DECK_MIN_Y = 40.0D;
    public static final double ABYSSAL_CORE_THRESHOLD_Y = 0.0D;

    private GCGasGiantAtmosphereHandler() {
    }

    public static boolean isGasGiant(@Nullable ResourceKey<Level> dimension) {
        return dimension != null && (
                dimension.equals(GCDimensions.JUPITER)
                        || dimension.equals(GCDimensions.SATURN)
                        || dimension.equals(GCDimensions.URANUS)
                        || dimension.equals(GCDimensions.NEPTUNE)
        );
    }

    /**
     * Computes hydrostatic barometric pressure in bars as an exponential function of altitude:
     * {@code P(y) = P_ref * exp((216.0 - y) / H)}, preserving Jupiter &gt; Saturn &gt; Neptune &gt; Uranus at every altitude.
     *
     * <ul>
     *   <li>{@code Y = 320}: ~0.11–0.13 bar (exosphere &lt; 0.2 bar)</li>
     *   <li>{@code Y = 216}: ~0.85–1.25 bar (safe aerostatic landing zone)</li>
     *   <li>{@code Y = 100}: ~8.6–15.5 bar (dense cloud decks)</li>
     *   <li>{@code Y = -20}: ~95–211 bar (crushing hyperbaric abyssal core)</li>
     * </ul>
     */
    public static double computePressureBar(@Nullable ResourceKey<Level> dimension, double y) {
        if (dimension == null) {
            return 0.0D;
        }
        double pRef;
        double scaleHeight;
        if (dimension.equals(GCDimensions.JUPITER)) {
            pRef = 1.25D;
            scaleHeight = 46.0D;
        } else if (dimension.equals(GCDimensions.SATURN)) {
            pRef = 1.05D;
            scaleHeight = 48.0D;
        } else if (dimension.equals(GCDimensions.NEPTUNE)) {
            pRef = 1.00D;
            scaleHeight = 49.0D;
        } else if (dimension.equals(GCDimensions.URANUS)) {
            pRef = 0.85D;
            scaleHeight = 50.0D;
        } else {
            return 0.0D;
        }
        double clampedY = Mth.clamp(y, -80.0D, 512.0D);
        return pRef * Math.exp((AEROSTATIC_REFERENCE_Y - clampedY) / scaleHeight);
    }

    /**
     * Optical sunlight transmittance in {@code [0.0, 1.0]} due to Beer-Lambert atmospheric extinction
     * as the player descends into dense cloud decks and abyssal depths.
     */
    public static float computeOpticalTransmittance(@Nullable ResourceKey<Level> dimension, double y) {
        if (!isGasGiant(dimension) || y >= 220.0D) {
            return 1.0F;
        }
        double p = computePressureBar(dimension, y);
        double pTop = computePressureBar(dimension, 220.0D);
        double deltaP = Math.max(0.0D, p - pTop);
        double k = dimension.equals(GCDimensions.JUPITER) ? 0.048D
                : dimension.equals(GCDimensions.SATURN) ? 0.044D
                : dimension.equals(GCDimensions.NEPTUNE) ? 0.042D
                : 0.038D;
        return (float) Mth.clamp(Math.exp(-k * deltaP), 0.0D, 1.0D);
    }

    /**
     * Progressive fog distance multiplier in {@code [0.14, 1.0]} that tightens visibility
     * continuously as barometric pressure rises during descent.
     */
    public static float computeFogDistanceScale(@Nullable ResourceKey<Level> dimension, double y) {
        if (!isGasGiant(dimension) || y >= 210.0D) {
            return 1.0F;
        }
        double p = computePressureBar(dimension, y);
        double pTop = computePressureBar(dimension, 210.0D);
        double ratio = Math.max(1.0D, p / Math.max(0.05D, pTop));
        // log10(ratio): 0 at Y=210 -> ~1.1 at Y=90 -> ~2.0 at Y=-20
        double t = Mth.clamp(Math.log10(ratio) / 2.0D, 0.0D, 1.0D);
        return (float) Mth.lerp(t, 1.0D, 0.24D);
    }

    /**
     * Computes banded zonal jet-stream horizontal acceleration (blocks/tick along X) in cloud layers.
     */
    public static double computeZonalWindAccel(@Nullable ResourceKey<Level> dimension, double y, double z) {
        if (!isGasGiant(dimension) || y > 185.0D || y < 15.0D) {
            return 0.0D;
        }
        double baseStrength;
        if (dimension.equals(GCDimensions.NEPTUNE)) {
            baseStrength = 0.045D; // Supersonic 2,100 km/h equatorial jet streams
        } else if (dimension.equals(GCDimensions.JUPITER)) {
            baseStrength = 0.032D; // Intense banded zonal winds
        } else if (dimension.equals(GCDimensions.SATURN)) {
            baseStrength = 0.028D; // Fast equatorial winds
        } else {
            baseStrength = 0.022D; // Uranus polar shear winds
        }
        // Smooth envelope peaking in the mid-cloud deck (Y = 70..150)
        double upperFade = Mth.clamp((185.0D - y) / 30.0D, 0.0D, 1.0D);
        double lowerFade = Mth.clamp((y - 15.0D) / 30.0D, 0.0D, 1.0D);
        double bandSign = Math.sin(z * 0.012D) >= 0.0D ? 1.0D : -1.0D;
        return baseStrength * upperFade * lowerFade * bandSign;
    }

    /**
     * Computes progressive hyperbaric crushing damage per 20-tick interval for unprotected players below Y = 175.
     */
    public static float computeHyperbaricDamage(double pressureBar) {
        if (pressureBar <= 2.5D) {
            return 0.0F;
        }
        return (float) Mth.clamp(1.0D + 2.4D * Math.log10(Math.max(1.0D, pressureBar / 2.0D)), 1.0D, 12.0D);
    }

    public static boolean hasShieldController(ServerPlayer player) {
        Container accessories = player.galacticraft$getAccessories();
        if (accessories != null) {
            for (int i = 0; i < accessories.getContainerSize(); i++) {
                if (accessories.getItem(i).is(GCItems.SHIELD_CONTROLLER)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasFullIsothermalPadding(ServerPlayer player) {
        Container thermal = player.galacticraft$getThermalArmor();
        if (thermal != null && thermal.getContainerSize() >= 4) {
            return thermal.getItem(0).is(GCItems.ISOTHERMAL_PADDING_HELMET)
                    && thermal.getItem(1).is(GCItems.ISOTHERMAL_PADDING_CHESTPIECE)
                    && thermal.getItem(2).is(GCItems.ISOTHERMAL_PADDING_LEGGINGS)
                    && thermal.getItem(3).is(GCItems.ISOTHERMAL_PADDING_BOOTS);
        }
        return false;
    }

    public static boolean hasThermalColdProtection(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || hasShieldController(player) || hasFullIsothermalPadding(player)) {
            return true;
        }
        Container thermal = player.galacticraft$getThermalArmor();
        if (thermal != null && thermal.getContainerSize() >= 4) {
            return (thermal.getItem(0).is(GCItems.THERMAL_PADDING_HELMET) || thermal.getItem(0).is(GCItems.ISOTHERMAL_PADDING_HELMET))
                    && (thermal.getItem(1).is(GCItems.THERMAL_PADDING_CHESTPIECE) || thermal.getItem(1).is(GCItems.ISOTHERMAL_PADDING_CHESTPIECE))
                    && (thermal.getItem(2).is(GCItems.THERMAL_PADDING_LEGGINGS) || thermal.getItem(2).is(GCItems.ISOTHERMAL_PADDING_LEGGINGS))
                    && (thermal.getItem(3).is(GCItems.THERMAL_PADDING_BOOTS) || thermal.getItem(3).is(GCItems.ISOTHERMAL_PADDING_BOOTS));
        }
        return false;
    }

    /**
     * Checks whether a player is protected from hyperbaric crushing pressure at altitude {@code y}:
     * <ul>
     *   <li>{@code Y >= 175}: safe aerostatic/exosphere zone (always protected from crushing pressure).</li>
     *   <li>{@code 0 <= Y < 175}: cloud decks (protected by {@code SHIELD_CONTROLLER} OR full {@code ISOTHERMAL_PADDING}).</li>
     *   <li>{@code Y < 0}: abyssal core (&gt;= 80–200+ bar), requiring BOTH {@code SHIELD_CONTROLLER} AND full {@code ISOTHERMAL_PADDING}.</li>
     * </ul>
     */
    public static boolean isProtectedFromPressure(ServerPlayer player, double y) {
        if (player.isCreative() || player.isSpectator() || y >= HYPERBARIC_HAZARD_Y) {
            return true;
        }
        boolean shield = hasShieldController(player);
        boolean isothermal = hasFullIsothermalPadding(player);
        if (y < ABYSSAL_CORE_THRESHOLD_Y) {
            return shield && isothermal;
        }
        return shield || isothermal;
    }

    public static void onWorldTick(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        if (!isGasGiant(dimension)) {
            return;
        }

        long gameTime = level.getGameTime();
        boolean tick10 = (gameTime % 10L == 0L);
        boolean tick20 = (gameTime % 20L == 0L);
        boolean tick40 = (gameTime % 40L == 0L);
        boolean hasStormLightning = dimension.equals(GCDimensions.JUPITER) || dimension.equals(GCDimensions.SATURN);
        boolean ultraCryoIceGiant = dimension.equals(GCDimensions.URANUS) || dimension.equals(GCDimensions.NEPTUNE);

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            BlockPos pos = player.blockPosition();
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            double y = player.getY();
            boolean breathable = level.isBreathable(pos);

            // 1. Zonal Jet Stream Winds in Cloud Layers (Y = 15..185)
            double windAccel = computeZonalWindAccel(dimension, y, player.getZ());
            if (Math.abs(windAccel) > 1.0E-4D && !breathable && !player.getAbilities().flying) {
                double effectiveAccel = player.onGround() ? windAccel * 0.45D : windAccel;
                Vec3 delta = player.getDeltaMovement();
                double newX = Mth.clamp(delta.x + effectiveAccel, -0.75D, 0.75D);
                player.setDeltaMovement(newX, delta.y, delta.z);
                player.hurtMarked = true;
            }

            // 2. Ambient Cloud Streaks & Electrical Storm Discharges (intensifying in lower storm decks)
            if (tick10 && y >= -50.0D && y <= 215.0D && !breathable) {
                spawnCloudDeckParticles(level, player, dimension, windAccel);
            }

            if (tick20 && y >= -50.0D && y <= 185.0D) {
                boolean deepStormLayer = y < 120.0D;
                int lightningChance;
                if (hasStormLightning) {
                    lightningChance = dimension.equals(GCDimensions.JUPITER)
                            ? (deepStormLayer ? 4 : 9)
                            : (deepStormLayer ? 6 : 14);
                } else {
                    // Ice giants (Uranus / Neptune) also unleash cryo-electric discharges in deep storm layers
                    lightningChance = deepStormLayer ? 10 : 28;
                }
                if (level.random.nextInt(lightningChance) == 0) {
                    spawnCloudLightning(level, player);
                }
            }

            if (player.isCreative()) {
                continue;
            }

            // 3. Cryogenic Cold (all altitudes outside sealed habitats without thermal padding)
            if (!breathable && !hasThermalColdProtection(player)) {
                int freezeStep = ultraCryoIceGiant ? 5 : 3;
                player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze() + 40, player.getTicksFrozen() + freezeStep));
                if (tick40 && player.isFullyFrozen()) {
                    float coldDamage = ultraCryoIceGiant ? 2.0F : 1.0F;
                    player.hurt(level.damageSources().freeze(), coldDamage);
                }
            }

            // 4. Progressive Hyperbaric Crushing Pressure below Y = 175
            if (tick20 && !breathable && y < HYPERBARIC_HAZARD_Y) {
                double pressureBar = computePressureBar(dimension, y);
                if (!isProtectedFromPressure(player, y)) {
                    float damage = computeHyperbaricDamage(pressureBar);
                    // Partial protection in the abyssal core halves damage to signal BOTH shield + isothermal are required
                    if (y < ABYSSAL_CORE_THRESHOLD_Y && (hasShieldController(player) || hasFullIsothermalPadding(player))) {
                        damage *= 0.5F;
                    }
                    if (damage > 0.0F) {
                        player.hurt(level.damageSources().generic(), damage);
                        int slowAmp = pressureBar >= 80.0D ? 2 : (pressureBar >= 15.0D ? 1 : 0);
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slowAmp));
                        if (y < ABYSSAL_CORE_THRESHOLD_Y) {
                            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1));
                        }
                    }
                }
            }
        }
    }

    private static void spawnCloudDeckParticles(ServerLevel level, ServerPlayer player, ResourceKey<Level> dimension, double windAccel) {
        double windSign = windAccel == 0.0D ? 1.0D : Math.signum(windAccel);
        boolean deepStorm = player.getY() < 130.0D;
        int count = deepStorm ? 5 : 3;
        for (int i = 0; i < count; i++) {
            double px = player.getX() + (level.random.nextDouble() - 0.5D) * 26.0D;
            double py = player.getY() + (level.random.nextDouble() - 0.5D) * 12.0D;
            double pz = player.getZ() + (level.random.nextDouble() - 0.5D) * 26.0D;
            level.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, windSign * 0.55D, 0.02D, 0.05D, 0.14D);
            int sparkChance = deepStorm ? 2 : 5;
            if (level.random.nextInt(sparkChance) == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, deepStorm ? 4 : 2, 0.45D, 0.35D, 0.45D, 0.12D);
            }
        }
    }

    private static void spawnCloudLightning(ServerLevel level, ServerPlayer player) {
        double angle = level.random.nextDouble() * Mth.TWO_PI;
        double dist = 18.0D + level.random.nextDouble() * 32.0D;
        double lx = player.getX() + Math.cos(angle) * dist;
        double lz = player.getZ() + Math.sin(angle) * dist;
        double ly = Mth.clamp(player.getY() + (level.random.nextDouble() - 0.5D) * 28.0D, -45.0D, 185.0D);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(lx, ly, lz);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, lx, ly, lz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.2F, 0.65F + level.random.nextFloat() * 0.35F);
    }
}
