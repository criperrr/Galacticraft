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

package dev.galacticraft.mod.content.rocket.physics;

import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.MountedPartNode;
import dev.galacticraft.api.rocket.assembly.RadialBoosterGroup;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Pure, deterministic aerospace flight dynamics engine for Galacticraft 5 modular rockets.
 *
 * <p>Computes:
 * <ul>
 *   <li>Exponential altitude-dependent atmospheric pressure {@code P_a(Y)} and density {@code rho(Y)}</li>
 *   <li>Altitude-interpolated specific impulse {@code Isp(P_a)}, thrust scaling, and vacuum-nozzle over-expansion checks</li>
 *   <li>Shaped-grain SRB Max-Q thrust curve ({@code 1.00x -> 0.82x -> 0.95x -> tail-off})</li>
 *   <li>Topological Asparagus crossfeed propellant drain ({@code layer 0 -> layer 1 -> core}) vs isolated burn</li>
 *   <li>Automatic Gravity Turn pitch schedule, Max-Q auto-throttle ({@code 0.65x..0.75x}), hot-staging pre-ignition,
 *       fairing jettison threshold ({@code P_a < 0.05 atm}), and manual WASD/G override blending</li>
 * </ul>
 */
public final class ModularFlightPhysics {
    public static final double MIN_LIFTOFF_TWR = 1.15;
    public static final double VACUUM_NOZZLE_DANGER_PA_ATM = RocketFlightValidator.VACUUM_NOZZLE_MAX_SAFE_ATM;
    public static final double FAIRING_JETTISON_PA_ATM = 0.05;
    public static final double OVERWORLD_FAIRING_MIN_Y = 400.0;
    public static final double MARS_FAIRING_MIN_Y = 250.0;
    public static final double EARTH_SCALE_HEIGHT_BLOCKS = 110.0;
    public static final double EARTH_SEA_LEVEL_DENSITY_KG_M3 = 1.225;
    public static final double MAX_Q_DYNAMIC_PRESSURE_PA = 32_000.0;
    public static final double SAFE_TOUCHDOWN_SPEED_MS = 5.0;
    public static final double SAFE_TOUCHDOWN_SPEED_BLOCKS_PER_TICK = SAFE_TOUCHDOWN_SPEED_MS / 20.0;
    public static final int HOT_STAGING_PRE_IGNITION_TICKS = 30;
    public static final int LES_ABORT_BURN_TICKS = 40;
    public static final double LES_ABORT_MIN_TWR = 6.5;

    private ModularFlightPhysics() {
    }

    /**
     * Computes ambient atmospheric pressure (in atm) at world altitude {@code altitudeY} above
     * surface elevation {@code surfaceY}:
     * {@code P_a(Y) = P_surface * exp(-max(0, Y - Y_0) / H_scale)}.
     */
    public static double computeAmbientPressureAtm(
            @NotNull PlanetaryEnvironment env,
            double altitudeY,
            double surfaceY
    ) {
        Objects.requireNonNull(env, "env");
        if (env.pressureAtm() <= 0.0) {
            return 0.0;
        }
        double heightAboveSurface = Math.max(0.0, altitudeY - surfaceY);
        double scaleHeight = resolveScaleHeightBlocks(env);
        return env.pressureAtm() * Math.exp(-heightAboveSurface / scaleHeight);
    }

    /**
     * Resolves atmospheric scale height in Minecraft blocks so Overworld crosses {@code P_a < 0.05 atm}
     * near {@code Y = 400} (from sea level {@code Y_0 = 64}) and Mars crosses near {@code Y = 250}.
     */
    public static double resolveScaleHeightBlocks(@NotNull PlanetaryEnvironment env) {
        if (env.pressureAtm() <= 0.0) {
            return EARTH_SCALE_HEIGHT_BLOCKS;
        }
        if (env.pressureAtm() > 5.0) {
            // Dense atmospheres (Venus, gas giants) extend higher
            return 140.0;
        }
        if (env.pressureAtm() < 0.1) {
            // Thin atmospheres (Mars) decay faster
            return 75.0;
        }
        return EARTH_SCALE_HEIGHT_BLOCKS;
    }

    /**
     * Computes atmospheric density {@code rho} in kg/m^3 proportional to ambient pressure and inverse temperature.
     */
    public static double computeAirDensityKgM3(double ambientPressureAtm, double tempKelvin) {
        if (!Double.isFinite(ambientPressureAtm) || ambientPressureAtm <= 0.0) {
            return 0.0;
        }
        double safeTemp = (Double.isFinite(tempKelvin) && tempKelvin > 20.0) ? tempKelvin : 288.15;
        return EARTH_SEA_LEVEL_DENSITY_KG_M3 * ambientPressureAtm * (288.15 / safeTemp);
    }

    /**
     * Computes aerodynamic dynamic pressure {@code q = 0.5 * rho * v^2} (Pascals).
     */
    public static double computeDynamicPressurePa(double airDensityKgM3, double speedMetersPerSecond) {
        if (!Double.isFinite(airDensityKgM3) || !Double.isFinite(speedMetersPerSecond) || airDensityKgM3 <= 0.0) {
            return 0.0;
        }
        return 0.5 * airDensityKgM3 * speedMetersPerSecond * speedMetersPerSecond;
    }

    /**
     * Computes the shaped solid-grain SRB thrust curve multiplier as a function of burn progress
     * {@code progress in [0.0, 1.0]}:
     * <ul>
     *   <li>Liftoff ({@code 0.00..0.25}): {@code 1.00x}</li>
     *   <li>Max-Q bucket ({@code 0.35..0.60}): smoothly dips to {@code 0.82x}</li>
     *   <li>Post Max-Q recovery ({@code 0.72..0.90}): climbs to {@code 0.95x}</li>
     *   <li>Tail-off ({@code 0.90..1.00}): ramps down smoothly to {@code 0.45x}</li>
     * </ul>
     */
    public static double computeSrbThrustCurveMultiplier(double burnProgress) {
        if (!Double.isFinite(burnProgress)) {
            return 1.0;
        }
        double p = Math.clamp(burnProgress, 0.0, 1.0);
        if (p <= 0.20) {
            return 1.0;
        }
        if (p < 0.40) {
            double t = (p - 0.20) / 0.20;
            return 1.0 - 0.18 * smoothstep(t);
        }
        if (p <= 0.60) {
            return 0.82;
        }
        if (p < 0.75) {
            double t = (p - 0.60) / 0.15;
            return 0.82 + 0.13 * smoothstep(t);
        }
        if (p <= 0.90) {
            return 0.95;
        }
        double t = (p - 0.90) / 0.10;
        return 0.95 - 0.50 * smoothstep(t);
    }

    /**
     * Computes effective altitude-scaled thrust (kN) for a single modular engine at ambient pressure {@code Pa},
     * interpolating linearly between sea-level thrust and vacuum thrust in proportion to {@code Isp(Pa) / Isp_vac}.
     */
    public static double computeAtmosphericThrustKn(
            @NotNull MountedPartNode engineNode,
            double ambientPressureAtm,
            double throttle,
            double srbBurnProgress
    ) {
        RocketPartConfig cfg = engineNode.config();
        if (cfg.category() != RocketPartCategory.ENGINE || engineNode.stats().thrustKn() <= 0.0) {
            return 0.0;
        }
        PropellantType prop = PropellantType.forEngine(cfg);
        double vacIsp = RocketFlightValidator.computeEngineIsp(prop, engineNode.composition().orElse(null));
        double atmIsp = RocketFlightValidator.computeAtmosphericIsp(cfg, prop, engineNode.composition().orElse(null), ambientPressureAtm);
        double pressureRatio = vacIsp > 0.0 ? Math.clamp(atmIsp / vacIsp, 0.10, 1.0) : 1.0;

        double baseThrust = engineNode.stats().thrustKn() * pressureRatio;
        if (prop == PropellantType.SOLID) {
            // SRBs are unthrottleable once ignited and follow the shaped grain curve
            return baseThrust * computeSrbThrustCurveMultiplier(srbBurnProgress);
        }
        double clampedThrottle = Math.clamp(throttle, 0.0, 1.0);
        return baseThrust * clampedThrottle;
    }

    /**
     * Computes automatic Gravity Turn pitch angle (degrees off vertical, {@code 0° = straight up},
     * {@code 45° = downrange gravity turn}) based on altitude above surface and rocket height.
     * Holds vertical until clearing the gantry tower ({@code Y > Y_0 + H_rocket + 10}).
     */
    public static float computeAutoGravityTurnPitchDeg(
            double altitudeY,
            double surfaceY,
            double rocketHeightBlocks
    ) {
        double clearanceY = surfaceY + Math.max(4.0, rocketHeightBlocks) + 10.0;
        if (altitudeY <= clearanceY) {
            return 0.0F;
        }
        double turnSpan = 520.0;
        double t = Math.clamp((altitudeY - clearanceY) / turnSpan, 0.0, 1.0);
        // Smooth S-curve peaking around 42 degrees before flattening toward orbital escape
        return (float) (42.0 * smoothstep(t));
    }

    /**
     * Computes Max-Q auto-throttle multiplier in {@code [0.65, 1.00]} when ascending through
     * the transonic Max-Q zone ({@code Y in [80, 220]} on Earth-like worlds or elevated dynamic pressure).
     */
    public static float computeMaxQAutoThrottle(
            @NotNull PlanetaryEnvironment env,
            double altitudeY,
            double surfaceY,
            double dynamicPressurePa,
            float requestedThrottle
    ) {
        float clampedReq = Math.clamp(requestedThrottle, 0.0F, 1.0F);
        if (env.pressureAtm() < 0.05 || clampedReq <= 0.65F) {
            return clampedReq;
        }
        double agl = altitudeY - surfaceY;
        // Max-Q altitude band roughly Y = 80..220 (AGL 16..160) on Overworld
        boolean inAltitudeBand = altitudeY >= 80.0 && altitudeY <= 220.0 && agl >= 16.0;
        double qFraction = Math.clamp(dynamicPressurePa / MAX_Q_DYNAMIC_PRESSURE_PA, 0.0, 1.0);

        if (!inAltitudeBand && qFraction < 0.35) {
            return clampedReq;
        }
        double bandWeight = 0.0;
        if (inAltitudeBand) {
            // Peak at Y = 145
            double norm = Math.abs(altitudeY - 145.0) / 75.0;
            bandWeight = Math.max(0.0, 1.0 - norm);
        }
        double intensity = Math.max(bandWeight, qFraction);
        // Throttle bucket: dips down to 0.68x at peak Max-Q (within 0.65x..0.75x spec)
        double maxAllowed = 1.0 - 0.32 * Math.clamp(intensity, 0.0, 1.0);
        maxAllowed = Math.clamp(maxAllowed, 0.65, 1.0);
        return (float) Math.min(clampedReq, maxAllowed);
    }

    /**
     * Returns whether aerodynamic fairings should automatically jettison at the current altitude and pressure.
     */
    public static boolean shouldAutoJettisonFairings(
            @NotNull RocketAssemblyGraph graph,
            @NotNull PlanetaryEnvironment env,
            double altitudeY,
            double surfaceY
    ) {
        if (!graph.hasFairing()) {
            return false;
        }
        double clearanceY = surfaceY + Math.max(4.0, graph.totalHeightBlocks()) + 12.0;
        if (altitudeY < clearanceY) {
            return false;
        }
        if (env.pressureAtm() <= 0.0) {
            return altitudeY >= clearanceY + 20.0;
        }
        double ambientPa = computeAmbientPressureAtm(env, altitudeY, surfaceY);
        if (env.id().getPath().contains("mars")) {
            return altitudeY >= MARS_FAIRING_MIN_Y || ambientPa < FAIRING_JETTISON_PA_ATM;
        }
        if (env.pressureAtm() >= 0.8 && env.pressureAtm() <= 1.2) {
            return altitudeY >= OVERWORLD_FAIRING_MIN_Y && ambientPa < FAIRING_JETTISON_PA_ATM;
        }
        return ambientPa < FAIRING_JETTISON_PA_ATM;
    }

    /**
     * Computes instantaneous propellant consumption (in droplets/tick) for an engine producing
     * {@code activeThrustKn} at {@code effectiveIspSeconds} with propellant {@code propellant}.
     */
    public static double computeConsumptionDropletsPerTick(
            double activeThrustKn,
            double effectiveIspSeconds,
            @NotNull PropellantType propellant
    ) {
        if (activeThrustKn <= 0.0 || effectiveIspSeconds <= 0.0) {
            return 0.0;
        }
        // Mass flow kg/s = F(N) / (Isp * g0)
        double massFlowKgPerSec = (activeThrustKn * 1000.0) / (effectiveIspSeconds * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
        double bucketsPerSec = massFlowKgPerSec / Math.max(1.0, propellant.densityKgPerBucket());
        // Playable Minecraft flight scale factor (keeps ascent 25-60s exhilarating while preserving relative Isp ratios)
        double dropletsPerTick = (bucketsPerSec * RocketFlightValidator.DROPLETS_PER_BUCKET) / 20.0;
        // Scale propellant flow to Minecraft's 1.1 km orbital ascent (Y=64..1200) so a single stage
        // burns for ~9-25 seconds (~175-500 ticks) and requires multi-stage separation to reach orbit
        return Math.max(1.0, dropletsPerTick * 1.60);
    }

    /**
     * Result of a single propulsion &amp; propellant-drain tick on the active {@link RocketStageNode}.
     */
    public record PropulsionTickResult(
            @NotNull RocketStageNode updatedStage,
            double totalThrustKn,
            double coreThrustKn,
            double boosterThrustKn,
            double effectiveIspSeconds,
            boolean vacuumNozzleOverexpansion,
            @NotNull List<String> depletedRadialGroupIds,
            boolean coreDepleted
    ) {
    }

    /**
     * Executes one atomic flight-tick propellant drain and thrust evaluation on {@code stage},
     * enforcing topological Asparagus crossfeed ({@code drainPriorityLayer} ascending: outermost
     * boosters drain first while keeping inner boosters and core at 100% fuel) and isolated SRB burn.
     */
    public static @NotNull PropulsionTickResult stepStagePropulsion(
            @NotNull RocketStageNode stage,
            double ambientPressureAtm,
            double throttle,
            boolean hotStageUpperSupport
    ) {
        Objects.requireNonNull(stage, "stage");
        if (stage.decoupled()) {
            return new PropulsionTickResult(stage, 0.0, 0.0, 0.0, 0.0, false, List.of(), true);
        }

        PropellantType coreProp = resolvePropellant(stage.propellantId());
        boolean coreHasFuelSource = stage.currentFuelDroplets() > 0L
                || (coreProp == PropellantType.SOLID && stage.fuelCapacityDroplets() == 0L);

        // Gather active radial groups sorted by drainPriorityLayer ascending (0 = outermost)
        List<RadialBoosterGroup> activeGroups = new ArrayList<>();
        for (RadialBoosterGroup g : stage.radialGroups()) {
            if (!g.decoupled()) {
                activeGroups.add(g);
            }
        }
        activeGroups.sort(Comparator.comparingInt(RadialBoosterGroup::drainPriorityLayer));

        // Check if any Asparagus liquid crossfeed booster currently has fuel to feed the core
        boolean asparagusCanFeedCore = false;
        for (RadialBoosterGroup g : activeGroups) {
            if (!g.solidBooster()
                    && g.crossfeedMode() == CrossfeedMode.CONTINUOUS_CROSSFEED
                    && g.currentFuelDroplets() > 0L) {
                asparagusCanFeedCore = true;
                break;
            }
        }

        double coreThrustKn = 0.0;
        double coreFlowWeight = 0.0;
        double coreDemandDroplets = 0.0;
        boolean vacHazard = false;

        boolean coreCanFire = (coreHasFuelSource || asparagusCanFeedCore) && (throttle > 0.001 || coreProp == PropellantType.SOLID);
        if (coreCanFire) {
            double coreSrbProgress = stage.fuelCapacityDroplets() > 0L
                    ? 1.0 - ((double) stage.currentFuelDroplets() / (double) stage.fuelCapacityDroplets())
                    : 0.0;
            for (MountedPartNode part : stage.coreParts()) {
                if (part.config().category() == RocketPartCategory.ENGINE && part.stats().thrustKn() > 0.0) {
                    PropellantType engProp = PropellantType.forEngine(part.config());
                    double tKn = computeAtmosphericThrustKn(part, ambientPressureAtm, throttle, coreSrbProgress);
                    double isp = RocketFlightValidator.computeAtmosphericIsp(
                            part.config(), engProp, part.composition().orElse(null), ambientPressureAtm);
                    if (tKn > 0.0 && isp > 0.0) {
                        coreThrustKn += tKn;
                        coreFlowWeight += tKn / isp;
                        coreDemandDroplets += computeConsumptionDropletsPerTick(tKn, isp, engProp);
                    }
                    if (RocketFlightValidator.isVacuumOptimizedEngine(part.config())
                            && ambientPressureAtm > VACUUM_NOZZLE_DANGER_PA_ATM
                            && tKn > 0.0) {
                        vacHazard = true;
                    }
                }
            }
        }

        // Compute per-group booster thrust & fuel demand
        double boosterThrustKn = 0.0;
        double boosterFlowWeight = 0.0;
        Map<String, Double> boosterDemands = new LinkedHashMap<>();
        Map<String, Long> nextBoosterFuels = new LinkedHashMap<>();
        for (RadialBoosterGroup g : stage.radialGroups()) {
            nextBoosterFuels.put(g.groupId(), g.currentFuelDroplets());
        }

        for (RadialBoosterGroup g : activeGroups) {
            boolean hasOwnFuel = g.currentFuelDroplets() > 0L || (g.solidBooster() && g.fuelCapacityDroplets() == 0L);
            boolean canFeedAsparagus = !g.solidBooster()
                    && g.crossfeedMode() == CrossfeedMode.CONTINUOUS_CROSSFEED
                    && (hasOwnFuel || hasLowerLayerFuel(activeGroups, g.drainPriorityLayer()));
            if (!hasOwnFuel && !canFeedAsparagus) {
                continue;
            }
            if (!g.solidBooster() && throttle <= 0.001) {
                continue;
            }
            double burnProgress = g.fuelCapacityDroplets() > 0L
                    ? 1.0 - ((double) g.currentFuelDroplets() / (double) g.fuelCapacityDroplets())
                    : 0.0;
            double gThrust = 0.0;
            double gDemand = 0.0;
            for (MountedPartNode part : g.parts()) {
                if (part.config().category() == RocketPartCategory.ENGINE && part.stats().thrustKn() > 0.0) {
                    PropellantType engProp = PropellantType.forEngine(part.config());
                    double tKn = computeAtmosphericThrustKn(part, ambientPressureAtm, throttle, burnProgress);
                    double isp = RocketFlightValidator.computeAtmosphericIsp(
                            part.config(), engProp, part.composition().orElse(null), ambientPressureAtm);
                    if (tKn > 0.0 && isp > 0.0) {
                        gThrust += tKn;
                        boosterFlowWeight += tKn / isp;
                        gDemand += computeConsumptionDropletsPerTick(tKn, isp, engProp);
                    }
                    if (RocketFlightValidator.isVacuumOptimizedEngine(part.config())
                            && ambientPressureAtm > VACUUM_NOZZLE_DANGER_PA_ATM
                            && tKn > 0.0) {
                        vacHazard = true;
                    }
                }
            }
            boosterThrustKn += gThrust;
            boosterDemands.put(g.groupId(), gDemand);
        }

        // Drain propellant: Asparagus layers first (lowest drainPriorityLayer -> highest -> core)
        long nextCoreFuel = stage.currentFuelDroplets();
        double totalAsparagusLiquidDemand = 0.0;
        boolean coreDrawsFromAsparagus = asparagusCanFeedCore && coreProp != PropellantType.SOLID;
        if (coreDrawsFromAsparagus) {
            totalAsparagusLiquidDemand += coreDemandDroplets;
        } else if (coreDemandDroplets > 0.0) {
            long drain = Math.max(1L, Math.round(coreDemandDroplets));
            nextCoreFuel = Math.max(0L, nextCoreFuel - drain);
        }

        for (RadialBoosterGroup g : activeGroups) {
            double demand = boosterDemands.getOrDefault(g.groupId(), 0.0);
            if (demand <= 0.0) {
                continue;
            }
            if (!g.solidBooster() && g.crossfeedMode() == CrossfeedMode.CONTINUOUS_CROSSFEED) {
                totalAsparagusLiquidDemand += demand;
            } else {
                // Isolated liquid or SRB booster drains strictly from its own tanks
                long drain = Math.max(1L, Math.round(demand));
                long cur = nextBoosterFuels.getOrDefault(g.groupId(), 0L);
                nextBoosterFuels.put(g.groupId(), Math.max(0L, cur - drain));
            }
        }

        // Distribute totalAsparagusLiquidDemand strictly by topological drainPriorityLayer (0 -> 1 -> ... -> core)
        if (totalAsparagusLiquidDemand > 0.0) {
            long remainingDemand = Math.max(1L, Math.round(totalAsparagusLiquidDemand));
            for (RadialBoosterGroup g : activeGroups) {
                if (remainingDemand <= 0L) {
                    break;
                }
                if (g.solidBooster() || g.crossfeedMode() != CrossfeedMode.CONTINUOUS_CROSSFEED) {
                    continue;
                }
                long avail = nextBoosterFuels.getOrDefault(g.groupId(), 0L);
                if (avail > 0L) {
                    long take = Math.min(avail, remainingDemand);
                    nextBoosterFuels.put(g.groupId(), avail - take);
                    remainingDemand -= take;
                }
            }
            if (remainingDemand > 0L && nextCoreFuel > 0L) {
                long take = Math.min(nextCoreFuel, remainingDemand);
                nextCoreFuel -= take;
            }
        }

        // Build updated radial groups & collect depleted group IDs
        List<RadialBoosterGroup> updatedGroups = new ArrayList<>(stage.radialGroups().size());
        List<String> depletedGroups = new ArrayList<>();
        for (RadialBoosterGroup g : stage.radialGroups()) {
            long nf = nextBoosterFuels.getOrDefault(g.groupId(), g.currentFuelDroplets());
            RadialBoosterGroup ug = g.withCurrentFuel(nf);
            updatedGroups.add(ug);
            if (!ug.decoupled() && ug.fuelCapacityDroplets() > 0L && nf <= 0L) {
                depletedGroups.add(ug.groupId());
            }
        }

        RocketStageNode updatedStage = stage.withUpdatedFuelAndBoosters(nextCoreFuel, updatedGroups);
        double totalThrustKn = coreThrustKn + boosterThrustKn;
        double totalFlowWeight = coreFlowWeight + boosterFlowWeight;
        double effectiveIsp = (totalThrustKn > 0.0 && totalFlowWeight > 0.0) ? (totalThrustKn / totalFlowWeight) : 0.0;
        boolean coreDepleted = updatedStage.fuelCapacityDroplets() > 0L && updatedStage.currentFuelDroplets() <= 0L
                && !hasAnyActiveAsparagusFuel(updatedGroups);

        return new PropulsionTickResult(
                updatedStage,
                totalThrustKn,
                coreThrustKn,
                boosterThrustKn,
                effectiveIsp,
                vacHazard,
                List.copyOf(depletedGroups),
                coreDepleted
        );
    }

    /**
     * Computes total current wet mass (kg) of all active stages in {@code graph}.
     */
    public static double computeTotalWetMassKg(@NotNull RocketAssemblyGraph graph) {
        double mass = 0.0;
        for (RocketStageNode stage : graph.stages()) {
            if (stage.decoupled()) {
                continue;
            }
            PropellantType coreProp = resolvePropellant(stage.propellantId());
            mass += stage.coreDryMassKg();
            mass += (stage.currentFuelDroplets() / RocketFlightValidator.DROPLETS_PER_BUCKET) * coreProp.densityKgPerBucket();
            for (RadialBoosterGroup group : stage.radialGroups()) {
                if (group.decoupled()) {
                    continue;
                }
                PropellantType gProp = resolvePropellant(group.propellantId());
                mass += group.dryMassKg();
                mass += (group.currentFuelDroplets() / RocketFlightValidator.DROPLETS_PER_BUCKET) * gProp.densityKgPerBucket();
            }
        }
        return mass;
    }

    /**
     * Computes real fueled Stage 0 liftoff TWR against {@code env.gravityG()} at {@code env.pressureAtm()}.
     * Unfueled core or radial engines that cannot actually ignite at T-0 contribute zero thrust so
     * Hold-Down Clamps cleanly abort instead of releasing a falling rocket.
     */
    public static double computeLiftoffTwr(@NotNull RocketAssemblyGraph graph, @NotNull PlanetaryEnvironment env) {
        RocketStageNode stage = graph.activeStage();
        if (stage == null) {
            return 0.0;
        }
        double wetMassKg = computeTotalWetMassKg(graph);
        double thrustKn = stepStagePropulsion(stage, env.pressureAtm(), 1.0, false).totalThrustKn();
        return RocketFlightValidator.computeTwr(thrustKn, wetMassKg, env.gravityG());
    }

    /**
     * Resolves a {@link PropellantType} by serialized stage propellant ID.
     */
    public static @NotNull PropellantType resolvePropellant(@Nullable String propellantId) {
        if (propellantId == null || propellantId.isBlank()) {
            return PropellantType.RP1;
        }
        String norm = propellantId.toUpperCase(Locale.ROOT);
        for (PropellantType type : PropellantType.values()) {
            if (type.name().equals(norm)) {
                return type;
            }
        }
        return PropellantType.RP1;
    }

    private static boolean hasLowerLayerFuel(List<RadialBoosterGroup> sortedGroups, int layer) {
        for (RadialBoosterGroup g : sortedGroups) {
            if (g.drainPriorityLayer() < layer
                    && !g.solidBooster()
                    && g.crossfeedMode() == CrossfeedMode.CONTINUOUS_CROSSFEED
                    && g.currentFuelDroplets() > 0L) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyActiveAsparagusFuel(List<RadialBoosterGroup> groups) {
        for (RadialBoosterGroup g : groups) {
            if (!g.decoupled()
                    && !g.solidBooster()
                    && g.crossfeedMode() == CrossfeedMode.CONTINUOUS_CROSSFEED
                    && g.currentFuelDroplets() > 0L) {
                return true;
            }
        }
        return false;
    }

    private static double smoothstep(double t) {
        double c = Math.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }
}
