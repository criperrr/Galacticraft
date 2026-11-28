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

package dev.galacticraft.mod.client.render.vehicle;

import dev.galacticraft.api.vehicle.HardpointType;
import dev.galacticraft.api.vehicle.VehicleChassisDefinition;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import dev.galacticraft.api.vehicle.VehicleHardpoint;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.mod.client.sounds.VehiclePropulsionSound;
import dev.galacticraft.mod.content.GCSounds;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client-side Phase 5 planetary polish handler for modular vehicles:
 * <ul>
 *   <li>Surface- and atmosphere-coupled planetary dust (lunar regolith, Martian ochre dust,
 *       Venusian amber grit, icy moon frost, Earth dirt) kicked up by wheels, rotor downwash,
 *       propeller slipstreams, and runway touchdowns.</li>
 *   <li>Vacuum electric Hall-thruster cyan plasma plumes under active {@code VTOL_THRUSTER} pods.</li>
 *   <li>Procedural looping electric motor, rotor, propeller, and thruster sounds plus landing gear hydraulics.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class VehicleClientEffects {
    private static final DustParticleOptions PLASMA_CYAN = new DustParticleOptions(new Vector3f(0.20F, 0.88F, 1.00F), 1.15F);
    private static final DustParticleOptions CONDENSATION_WHITE = new DustParticleOptions(new Vector3f(0.92F, 0.96F, 1.00F), 0.72F);

    private static final Map<ModularVehicleEntity, VehiclePropulsionSound> ACTIVE_SOUNDS = new WeakHashMap<>();
    private static final Map<ModularVehicleEntity, Float> LAST_GEAR_PROGRESS = new WeakHashMap<>();
    private static final Map<ModularVehicleEntity, Boolean> WAS_ON_GROUND = new WeakHashMap<>();

    private VehicleClientEffects() {
    }

    public static void tick(@NotNull ModularVehicleEntity vehicle) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (mc.player.distanceToSqr(vehicle) > 3600.0) {
            return;
        }
        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(vehicle.level());
        tickParticles(vehicle, env);
        tickSounds(vehicle, mc);
    }

    private static void tickParticles(@NotNull ModularVehicleEntity vehicle, @NotNull PlanetaryEnvironment env) {
        Level level = vehicle.level();
        RandomSource rand = level.getRandom();
        VehicleChassisDefinition chassis = vehicle.getChassisDefinition();
        VehicleConfiguration cfg = vehicle.getConfiguration();

        double yawRad = Math.toRadians(vehicle.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        float speed = vehicle.getSyncedSpeed();
        float absSpeed = Math.abs(speed);
        float rpm = vehicle.getRotorRpm();
        boolean engineOn = vehicle.isEngineRunning();
        double pressure = env.pressureAtm();
        double gravity = Math.max(0.08, env.gravityG());
        // Lower gravity lofts regolith higher on ballistic arcs (Apollo LRV lunar rooster-tails!)
        double loftBoost = Mth.clamp(1.0 / Math.sqrt(gravity), 0.85, 2.35);

        DustParticleOptions planetDust = resolvePlanetaryDust(env);

        // 1. Ground vehicle wheel regolith/dust rooster-tails
        if (!vehicle.isAircraft() && absSpeed > 0.08F && vehicle.onGround()) {
            int emitStride = absSpeed > 0.25F ? 1 : 2;
            if (vehicle.tickCount % emitStride == 0) {
                float dirSign = speed >= 0.0F ? -1.0F : 1.0F;
                for (VehicleHardpoint hp : chassis.hardpoints()) {
                    if (hp.type() != HardpointType.LOCOMOTION || !cfg.hasPart(hp.id())) {
                        continue;
                    }
                    double wx = vehicle.getX() + hp.localX() * cos - hp.localZ() * sin;
                    double wz = vehicle.getZ() + hp.localX() * sin + hp.localZ() * cos;
                    BlockPos groundPos = BlockPos.containing(wx, vehicle.getY() - 0.20, wz);
                    BlockState groundState = level.getBlockState(groundPos);
                    if (groundState.isAir()) {
                        continue;
                    }
                    double vx = (-sin * dirSign * absSpeed * 0.45) + (rand.nextDouble() - 0.5) * 0.08;
                    double vz = (cos * dirSign * absSpeed * 0.45) + (rand.nextDouble() - 0.5) * 0.08;
                    double vy = (0.06 + absSpeed * 0.14) * loftBoost;

                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                            wx + (rand.nextDouble() - 0.5) * 0.25,
                            vehicle.getY() + 0.08,
                            wz + (rand.nextDouble() - 0.5) * 0.25,
                            vx, vy, vz);
                    if (rand.nextFloat() < 0.65F) {
                        level.addParticle(planetDust,
                                wx + (rand.nextDouble() - 0.5) * 0.30,
                                vehicle.getY() + 0.14,
                                wz + (rand.nextDouble() - 0.5) * 0.30,
                                vx * 0.6, vy * 0.45, vz * 0.6);
                    }
                }
            }
        }

        // 2. Rotorcraft downwash dust rings & vacuum VTOL plasma plumes
        if ((vehicle.isRotorcraft() || vehicle.isFixedWing()) && rpm > 160.0F) {
            float rpmFrac = Mth.clamp(rpm / 900.0F, 0.0F, 1.0F);
            for (VehicleHardpoint hp : chassis.hardpoints()) {
                ResourceLocation partId = cfg.getPart(hp.id());
                if (partId == null) {
                    continue;
                }
                double hx = vehicle.getX() + hp.localX() * cos - hp.localZ() * sin;
                double hy = vehicle.getY() + hp.localY();
                double hz = vehicle.getZ() + hp.localX() * sin + hp.localZ() * cos;

                if (hp.type() == HardpointType.ROTOR) {
                    boolean isThruster = VehicleRegistry.PART_VTOL_THRUSTER.equals(partId);
                    if (isThruster && engineOn) {
                        // Electric Hall-thruster cyan plasma plume directly below nozzle
                        level.addParticle(PLASMA_CYAN,
                                hx + (rand.nextDouble() - 0.5) * 0.16,
                                hy - 0.32,
                                hz + (rand.nextDouble() - 0.5) * 0.16,
                                (rand.nextDouble() - 0.5) * 0.05,
                                -0.32 - rpmFrac * 0.38,
                                (rand.nextDouble() - 0.5) * 0.05);
                        if (rand.nextFloat() < 0.45F) {
                            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                                    hx, hy - 0.38, hz,
                                    (rand.nextDouble() - 0.5) * 0.08,
                                    -0.25,
                                    (rand.nextDouble() - 0.5) * 0.08);
                        }
                    }

                    // Surface dust downwash ring when hovering near terrain (AGL < 9 blocks)
                    if (isThruster || pressure >= 0.005) {
                        BlockPos groundPos = findGroundBelow(level, hx, hy, hz, 9);
                        if (groundPos != null) {
                            double agl = Math.max(0.4, hy - (groundPos.getY() + 1.0));
                            double intensity = Mth.clamp((1.0 - agl / 9.0) * rpmFrac * (isThruster ? 1.0 : Mth.clamp(pressure * 1.4, 0.25, 1.0)), 0.0, 1.0);
                            if (intensity > 0.08 && rand.nextFloat() < intensity * 0.85F) {
                                BlockState groundState = level.getBlockState(groundPos);
                                double angle = rand.nextDouble() * Mth.TWO_PI;
                                double ringR = 0.65 + (9.0 - agl) * 0.14 + rand.nextDouble() * 0.75;
                                double px = hx + Math.cos(angle) * ringR;
                                double pz = hz + Math.sin(angle) * ringR;
                                double py = groundPos.getY() + 1.08;
                                double outSpeed = (0.14 + intensity * 0.26) * (isThruster ? 1.25 : 1.0);
                                level.addParticle(planetDust, px, py, pz,
                                        Math.cos(angle) * outSpeed,
                                        0.04 * loftBoost,
                                        Math.sin(angle) * outSpeed);
                                if (rand.nextFloat() < 0.40F) {
                                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                                            px, py, pz,
                                            Math.cos(angle) * outSpeed * 0.8,
                                            0.08 * loftBoost,
                                            Math.sin(angle) * outSpeed * 0.8);
                                }
                            }
                        }
                    }
                } else if (hp.type() == HardpointType.PROPELLER && pressure >= 0.005) {
                    // Fixed-wing propeller slipstream dust vortex near runway
                    BlockPos groundPos = findGroundBelow(level, hx, hy, hz, 5);
                    if (groundPos != null && rand.nextFloat() < rpmFrac * 0.55F) {
                        double py = groundPos.getY() + 1.08;
                        double slipX = sin * (0.28 + rpmFrac * 0.35) + (rand.nextDouble() - 0.5) * 0.10;
                        double slipZ = -cos * (0.28 + rpmFrac * 0.35) + (rand.nextDouble() - 0.5) * 0.10;
                        level.addParticle(planetDust,
                                hx + (rand.nextDouble() - 0.5) * 0.45,
                                py,
                                hz + (rand.nextDouble() - 0.5) * 0.45,
                                slipX, 0.05 * loftBoost, slipZ);
                    }
                }
            }
        }

        // 3. Fixed-wing high-speed wingtip condensation trails in dense atmospheres
        if (vehicle.isFixedWing() && !vehicle.onGround() && pressure >= 0.45 && absSpeed > 0.42F) {
            for (VehicleHardpoint hp : chassis.hardpoints()) {
                if (hp.type() != HardpointType.WING || !cfg.hasPart(hp.id())) {
                    continue;
                }
                double tipSpan = hp.localX() < 0.0 ? -4.4 : 4.4;
                double lx = hp.localX() + tipSpan;
                double lz = hp.localZ() - 0.35;
                double wx = vehicle.getX() + lx * cos - lz * sin;
                double wy = vehicle.getY() + hp.localY() + 0.15;
                double wz = vehicle.getZ() + lx * sin + lz * cos;
                level.addParticle(CONDENSATION_WHITE, wx, wy, wz, 0.0, 0.0, 0.0);
            }
        }

        // 4. Aircraft runway touchdown tire smoke / regolith puff
        if (vehicle.isAircraft()) {
            boolean wasOnGround = WAS_ON_GROUND.getOrDefault(vehicle, vehicle.onGround());
            if (!wasOnGround && vehicle.onGround() && absSpeed > 0.14F) {
                BlockPos groundPos = BlockPos.containing(vehicle.getX(), vehicle.getY() - 0.25, vehicle.getZ());
                BlockState state = level.getBlockState(groundPos);
                for (int i = 0; i < 10; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 1.4;
                    double oz = (rand.nextDouble() - 0.5) * 1.4;
                    level.addParticle(planetDust, vehicle.getX() + ox, vehicle.getY() + 0.12, vehicle.getZ() + oz,
                            ox * 0.12, 0.06 * loftBoost, oz * 0.12);
                    if (!state.isAir()) {
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                                vehicle.getX() + ox, vehicle.getY() + 0.08, vehicle.getZ() + oz,
                                ox * 0.15, 0.10 * loftBoost, oz * 0.15);
                    }
                }
            }
            WAS_ON_GROUND.put(vehicle, vehicle.onGround());
        }
    }

    private static void tickSounds(@NotNull ModularVehicleEntity vehicle, @NotNull Minecraft mc) {
        // One-shot landing gear hydraulics cue when gear transitions
        if (vehicle.isAircraft()) {
            float gear = vehicle.getGearProgress();
            float prev = LAST_GEAR_PROGRESS.getOrDefault(vehicle, gear);
            if ((prev == 0.0F && gear > 0.02F) || (prev == 1.0F && gear < 0.98F)) {
                vehicle.level().playLocalSound(
                        vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                        GCSounds.VEHICLE_GEAR_HYDRAULICS,
                        SoundSource.NEUTRAL,
                        0.65F,
                        gear > prev ? 0.92F : 1.12F,
                        false
                );
            }
            LAST_GEAR_PROGRESS.put(vehicle, gear);
        }

        boolean shouldPlay = vehicle.isEngineRunning()
                || Math.abs(vehicle.getSyncedSpeed()) > 0.04F
                || vehicle.getRotorRpm() > 35.0F;

        VehiclePropulsionSound current = ACTIVE_SOUNDS.get(vehicle);
        if (!shouldPlay) {
            if (current != null && !current.isStopped()) {
                mc.getSoundManager().stop(current);
            }
            ACTIVE_SOUNDS.remove(vehicle);
            return;
        }

        VehiclePropulsionSound.Channel desired = resolveSoundChannel(vehicle);
        if (current == null || current.isStopped() || current.getChannel() != desired) {
            if (current != null && !current.isStopped()) {
                mc.getSoundManager().stop(current);
            }
            SoundEvent event = switch (desired) {
                case ELECTRIC_MOTOR -> GCSounds.VEHICLE_ELECTRIC_MOTOR;
                case ROTOR_BLADES -> GCSounds.VEHICLE_ROTOR_BLADES;
                case PROPELLER -> GCSounds.VEHICLE_PROPELLER;
                case VTOL_THRUSTER -> GCSounds.VEHICLE_VTOL_THRUSTER;
            };
            VehiclePropulsionSound next = new VehiclePropulsionSound(vehicle, event, desired);
            ACTIVE_SOUNDS.put(vehicle, next);
            mc.getSoundManager().play(next);
        }

        // Periodic cleanup of removed vehicles
        if (vehicle.tickCount % 100 == 0 && ACTIVE_SOUNDS.size() > 12) {
            Iterator<Map.Entry<ModularVehicleEntity, VehiclePropulsionSound>> it = ACTIVE_SOUNDS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ModularVehicleEntity, VehiclePropulsionSound> e = it.next();
                if (!e.getKey().isAlive() || e.getValue().isStopped()) {
                    it.remove();
                }
            }
        }
    }

    private static @NotNull VehiclePropulsionSound.Channel resolveSoundChannel(@NotNull ModularVehicleEntity vehicle) {
        if (vehicle.isFixedWing()) {
            boolean hybridHover = vehicle.getDerivedStats().isHybridVtolFixedWing()
                    && Math.abs(vehicle.getSyncedSpeed()) < 0.22F
                    && !vehicle.onGround();
            return hybridHover
                    ? VehiclePropulsionSound.Channel.VTOL_THRUSTER
                    : VehiclePropulsionSound.Channel.PROPELLER;
        }
        if (vehicle.isRotorcraft()) {
            return vehicle.getDerivedStats().hasVacuumThrusters()
                    ? VehiclePropulsionSound.Channel.VTOL_THRUSTER
                    : VehiclePropulsionSound.Channel.ROTOR_BLADES;
        }
        return VehiclePropulsionSound.Channel.ELECTRIC_MOTOR;
    }

    private static BlockPos findGroundBelow(@NotNull Level level, double x, double startY, double z, int maxDepth) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int topY = Mth.floor(startY);
        for (int dy = 0; dy <= maxDepth; dy++) {
            BlockPos pos = new BlockPos(bx, topY - dy, bz);
            if (!level.getBlockState(pos).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private static @NotNull DustParticleOptions resolvePlanetaryDust(@NotNull PlanetaryEnvironment env) {
        String path = env.id().getPath();
        if (path.contains("mars")) {
            return new DustParticleOptions(new Vector3f(0.78F, 0.39F, 0.24F), 1.05F);
        }
        if (path.contains("moon") || path.contains("mercury") || path.contains("asteroid") || path.contains("phobos") || path.contains("deimos")) {
            return new DustParticleOptions(new Vector3f(0.72F, 0.74F, 0.78F), 1.00F);
        }
        if (path.contains("venus") || path.contains("io")) {
            return new DustParticleOptions(new Vector3f(0.84F, 0.70F, 0.32F), 1.05F);
        }
        if (path.contains("titan")) {
            return new DustParticleOptions(new Vector3f(0.77F, 0.60F, 0.33F), 1.05F);
        }
        if (path.contains("europa") || path.contains("enceladus") || path.contains("triton") || path.contains("ganymede")) {
            return new DustParticleOptions(new Vector3f(0.80F, 0.91F, 0.98F), 0.95F);
        }
        return new DustParticleOptions(new Vector3f(0.62F, 0.56F, 0.48F), 0.95F);
    }
}
