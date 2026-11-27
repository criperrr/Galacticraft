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

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.MountedPartNode;
import dev.galacticraft.api.rocket.assembly.RadialBoosterGroup;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Emergent aerospace physics engine replacing artificial numerical rocket tiers with physical
 * evaluations computed directly from modular rocket parts, Tinkers' material compositions,
 * loaded propellants, and destination {@link PlanetaryEnvironment} conditions.
 *
 * <p>Evaluates:
 * <ul>
 *   <li><b>Thrust-to-Weight ({@code T/W > 1.0})</b> against origin and destination gravity</li>
 *   <li><b>Tsiolkovsky Delta-V</b> using thrust-weighted engine {@code Isp} and propellant density</li>
 *   <li><b>Thermal &amp; Pressure Survival</b> (never artificially locks launch; predicts and applies
 *       {@link PartIntegrity} degradation on undersized parts)</li>
 *   <li><b>Sulfuric Acid Corrosion</b> (Venus without {@link RocketMaterialTrait#ACID_RADIATION_SHIELD})</li>
 *   <li><b>Inverse-Square Solar Irradiance ({@code 1/r^2})</b> &amp; Direct Fusion Drive (DFD) positive power</li>
 *   <li><b>Cryogenic Boil-Off</b> (SOFI 0.15x, active cryocooler 0.0x with power, He-3 {@code <2K} 0.0x passive)</li>
 * </ul>
 */
public final class RocketFlightValidator {
    public static final double DROPLETS_PER_BUCKET = 81_000.0;
    public static final long BASE_AVIONICS_DRAW_GJ_PER_TICK = 10L;
    public static final long CRYOCOOLER_DRAW_GJ_PER_TICK = 15L;
    public static final long DFD_BURN_GENERATION_BASE_GJ_PER_TICK = 80L;
    public static final double SOFI_BOIL_OFF_MULTIPLIER = 0.15;

    private RocketFlightValidator() {
    }

    /**
     * Immutable snapshot of a single modular rocket part used for physics validation.
     */
    public record PartSnapshot(
            @NotNull RocketPartConfig config,
            @NotNull RocketPartStats stats,
            @NotNull PartIntegrity integrity,
            @Nullable RocketPartComposition composition,
            double boilOffMultiplier
    ) {
        public PartSnapshot {
            Objects.requireNonNull(config, "config");
            Objects.requireNonNull(stats, "stats");
            Objects.requireNonNull(integrity, "integrity");
            boilOffMultiplier = Double.isFinite(boilOffMultiplier) ? Math.max(0.0, boilOffMultiplier) : 1.0;
        }

        public boolean hasTrait(@NotNull RocketMaterialTrait trait) {
            return this.composition != null && this.composition.hasTrait(trait);
        }

        public static @NotNull PartSnapshot fromParts(
                @NotNull RocketPartConfig config,
                @Nullable RocketPartStats stats,
                @Nullable PartIntegrity integrity,
                @Nullable RocketPartComposition composition
        ) {
            RocketPartCostCalculator.CostProfile profile = RocketPartCostCalculator.calculate(config, composition);
            RocketPartStats resolvedStats = stats != null ? stats : profile.stats();
            PartIntegrity resolvedIntegrity = integrity != null ? integrity : PartIntegrity.pristine(profile.maxIntegrity());
            return new PartSnapshot(config, resolvedStats, resolvedIntegrity, composition, profile.boilOffMultiplier());
        }

        public static @Nullable PartSnapshot fromStack(@Nullable ItemStack stack) {
            if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
                return null;
            }
            RocketPartConfig config = partItem.getConfig(stack);
            PartIntegrity integrity = stack.get(GCDataComponents.PART_INTEGRITY);
            RocketPartComposition composition = stack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
            return fromParts(config, null, integrity, composition);
        }
    }

    /**
     * Complete vehicle snapshot evaluated against an origin and destination {@link PlanetaryEnvironment}.
     */
    public record VehicleSnapshot(
            @NotNull List<PartSnapshot> parts,
            long currentFuelDroplets,
            long currentEnergyGj,
            @Nullable Fluid loadedFuelFluid
    ) {
        public VehicleSnapshot {
            parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
            currentFuelDroplets = Math.max(0L, currentFuelDroplets);
            currentEnergyGj = Math.max(0L, currentEnergyGj);
        }

        public static @NotNull VehicleSnapshot fromStacks(
                @NotNull List<ItemStack> stacks,
                long currentFuelDroplets,
                long currentEnergyGj,
                @Nullable Fluid loadedFuelFluid
        ) {
            List<PartSnapshot> snapshots = new ArrayList<>(stacks.size());
            for (ItemStack stack : stacks) {
                PartSnapshot snap = PartSnapshot.fromStack(stack);
                if (snap != null) {
                    snapshots.add(snap);
                }
            }
            return new VehicleSnapshot(snapshots, currentFuelDroplets, currentEnergyGj, loadedFuelFluid);
        }

        public long totalFuelCapacityDroplets() {
            long sum = 0L;
            for (PartSnapshot part : this.parts) {
                sum += part.stats().fuelCapacityDroplets();
            }
            return sum;
        }
    }

    public enum HazardSeverity {
        INFO,
        WARNING,
        CRITICAL
    }

    public enum HazardCode {
        NO_PARTS,
        LOW_ORIGIN_TWR,
        LOW_DESTINATION_TWR,
        INSUFFICIENT_DELTA_V,
        THERMAL_HAZARD,
        PRESSURE_HAZARD,
        ACID_CORROSION,
        LOW_SOLAR_POWER,
        PROPELLANT_MISMATCH,
        CRYOGENIC_BOIL_OFF,
        DAMAGED_STRUCTURE,
        VACUUM_NOZZLE_OVEREXPANSION,
        PAD_OVERWEIGHT,
        DIAMETER_MISMATCH
    }

    /**
     * Per-stage atmospheric propulsion and Tsiolkovsky breakdown inside a
     * {@link GraphFlightValidationReport}.
     */
    public record StageFlightReport(
            int stageIndex,
            @NotNull String propellantId,
            double ambientPressureAtm,
            double stageDryMassKg,
            double stageWetMassKg,
            double activeThrustKn,
            double stageTwr,
            double effectiveIspSeconds,
            double maxStageDeltaV,
            double currentStageDeltaV,
            boolean hotStaging,
            boolean vacuumNozzleHazard,
            int activeRadialGroupCount
    ) {
    }

    /**
     * Complete multi-stage validation result produced by {@link #evaluateGraph}.
     */
    public record GraphFlightValidationReport(
            @NotNull FlightValidationReport summary,
            @NotNull List<StageFlightReport> stageReports,
            double padMaxMassKg,
            boolean withinPadMassLimit,
            boolean vacuumNozzleWarning
    ) {
        public GraphFlightValidationReport {
            Objects.requireNonNull(summary, "summary");
            stageReports = List.copyOf(Objects.requireNonNull(stageReports, "stageReports"));
        }
    }

    public record FlightHazard(
            @NotNull HazardCode code,
            @NotNull HazardSeverity severity,
            @NotNull String summary
    ) {
    }

    /**
     * Full emergent physics telemetry and risk report for a mission from {@code origin} to {@code destination}.
     */
    public record FlightValidationReport(
            @NotNull PlanetaryEnvironment origin,
            @NotNull PlanetaryEnvironment destination,
            double dryMassKg,
            double maxPropellantMassKg,
            double currentPropellantMassKg,
            double wetMassKg,
            double currentWetMassKg,
            double totalThrustKn,
            double originTwr,
            double destinationTwr,
            double effectiveIspSeconds,
            double currentDeltaV,
            double maxDeltaV,
            double requiredDeltaV,
            double minThermalToleranceKelvin,
            double destinationPeakTempKelvin,
            boolean thermalSafe,
            double minPressureToleranceAtm,
            double destinationPressureAtm,
            boolean pressureSafe,
            boolean acidProtected,
            boolean acidHazard,
            double solarIrradianceFactor,
            long passivePowerGenPerTick,
            long dfdBurnPowerGenPerTick,
            long powerDrawPerTick,
            long netPowerPerTick,
            boolean powerDeficit,
            double boilOffDropletsPerMinute,
            boolean zeroBoilOffActive,
            boolean cryocoolerPowered,
            boolean superfluidCryo2kActive,
            boolean propellantCompatible,
            @Nullable PropellantType expectedPropellant,
            @NotNull String propellantStatusMessage,
            boolean canLiftoffOrigin,
            boolean canLiftoffDestination,
            boolean hasEnoughDeltaV,
            double estimatedEntryIntegrityLoss,
            @NotNull List<FlightHazard> hazards
    ) {
        public boolean hasCriticalHazards() {
            for (FlightHazard hazard : this.hazards) {
                if (hazard.severity() == HazardSeverity.CRITICAL) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasHazard(@NotNull HazardCode code) {
            for (FlightHazard hazard : this.hazards) {
                if (hazard.code() == code) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Evaluates all emergent aerospace physics constraints for {@code vehicle} flying from
     * {@code origin} to {@code destination}.
     */
    public static @NotNull FlightValidationReport evaluate(
            @NotNull VehicleSnapshot vehicle,
            @NotNull PlanetaryEnvironment origin,
            @NotNull PlanetaryEnvironment destination
    ) {
        Objects.requireNonNull(vehicle, "vehicle");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");

        List<PartSnapshot> parts = vehicle.parts();
        if (parts.isEmpty()) {
            return new FlightValidationReport(
                    origin, destination,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, destination.requiredDeltaVFrom(origin),
                    0.0, destination.peakTempKelvin(), false,
                    0.0, destination.pressureAtm(), false,
                    false, destination.corrosiveAcid(),
                    destination.solarIrradianceFactor(), 0L, 0L, 0L, 0L, false,
                    0.0, false, false, false,
                    false, null, "No rocket modules",
                    false, false, false, 0.0,
                    List.of(new FlightHazard(HazardCode.NO_PARTS, HazardSeverity.CRITICAL, "Sem módulos no pad"))
            );
        }

        double dryMassKg = 0.0;
        double totalThrustKn = 0.0;
        long totalFuelCapacityDroplets = 0L;
        double weightedDensitySum = 0.0;
        double weightedIspSum = 0.0;
        double engineThrustSum = 0.0;

        double minThermalK = Double.MAX_VALUE;
        double sumThermalK = 0.0;
        double minPressureAtm = Double.MAX_VALUE;
        double minIntegrityFrac = 1.0;

        boolean hasThermalDuctility = false;
        boolean hasFlightComputer = false;
        boolean hasLightweightAlloy = false;
        boolean hasAblativeTps = false;
        boolean hasParachute = false;
        boolean hasCryocoolerModule = false;
        boolean hasSuperfluidCryo2k = false;
        boolean allCriticalAcidProtected = true;
        boolean anyAcidShield = false;
        int criticalPartCount = 0;

        long solarBaseGenPerTick = 0L;
        long nonSolarGenPerTick = 0L;
        long dfdBurnGenPerTick = 0L;

        PropellantType dominantEnginePropellant = null;
        double dominantEngineThrust = -1.0;
        Set<PropellantType> tankTypes = EnumSet.noneOf(PropellantType.class);

        for (PartSnapshot part : parts) {
            RocketPartConfig cfg = part.config();
            RocketPartStats stats = part.stats();
            RocketPartComposition comp = part.composition();

            dryMassKg += stats.dryWeightKg();
            totalThrustKn += stats.thrustKn();
            minIntegrityFrac = Math.min(minIntegrityFrac, part.integrity().fraction());

            if (stats.thermalToleranceKelvin() > 0.0) {
                minThermalK = Math.min(minThermalK, stats.thermalToleranceKelvin());
                sumThermalK += stats.thermalToleranceKelvin();
            }
            if (stats.pressureToleranceAtm() > 0.0) {
                minPressureAtm = Math.min(minPressureAtm, stats.pressureToleranceAtm());
            }

            if (part.hasTrait(RocketMaterialTrait.THERMAL_DUCTILITY)) {
                hasThermalDuctility = true;
            }
            if (part.hasTrait(RocketMaterialTrait.FLIGHT_COMPUTER)) {
                hasFlightComputer = true;
            }
            if (part.hasTrait(RocketMaterialTrait.LIGHTWEIGHT_ALLOY)) {
                hasLightweightAlloy = true;
            }
            if (part.hasTrait(RocketMaterialTrait.ABLATIVE_TPS)) {
                hasAblativeTps = true;
            }
            if (part.hasTrait(RocketMaterialTrait.ATMOSPHERIC_PARACHUTE) || part.hasTrait(RocketMaterialTrait.SUPERSONIC_CHUTE)) {
                hasParachute = true;
            }
            if (PropellantType.isCryocoolerModule(cfg) || part.hasTrait(RocketMaterialTrait.ACTIVE_ZERO_BOILOFF)) {
                hasCryocoolerModule = true;
            }
            if (part.hasTrait(RocketMaterialTrait.SUPERFLUID_CRYO_2K)) {
                hasSuperfluidCryo2k = true;
            }

            boolean partAcidImmune = part.hasTrait(RocketMaterialTrait.ACID_RADIATION_SHIELD)
                    || part.hasTrait(RocketMaterialTrait.CRYSTALLINE_LATTICE);
            if (partAcidImmune) {
                anyAcidShield = true;
            }
            if (isCriticalStructuralPart(cfg.category())) {
                criticalPartCount++;
                if (!partAcidImmune) {
                    allCriticalAcidProtected = false;
                }
            }

            if (stats.fuelCapacityDroplets() > 0L) {
                PropellantType tankProp = cfg.category() == RocketPartCategory.ENGINE
                        ? PropellantType.forEngine(cfg)
                        : PropellantType.forTank(cfg);
                tankTypes.add(tankProp);
                totalFuelCapacityDroplets += stats.fuelCapacityDroplets();
                weightedDensitySum += stats.fuelCapacityDroplets() * tankProp.densityKgPerBucket();
            }

            if (cfg.category() == RocketPartCategory.ENGINE && stats.thrustKn() > 0.0) {
                PropellantType engProp = PropellantType.forEngine(cfg);
                double engIsp = computeEngineIsp(engProp, comp);
                weightedIspSum += stats.thrustKn() * engIsp;
                engineThrustSum += stats.thrustKn();
                if (stats.thrustKn() > dominantEngineThrust) {
                    dominantEngineThrust = stats.thrustKn();
                    dominantEnginePropellant = engProp;
                }
                if (part.hasTrait(RocketMaterialTrait.ANEUTRONIC_HE3_PLASMA)) {
                    // Direct Fusion Drive (DFD) generates net positive electrical power while firing!
                    double scale = Math.max(1.0, cfg.diameter() * 0.5);
                    dfdBurnGenPerTick += Math.round(DFD_BURN_GENERATION_BASE_GJ_PER_TICK * scale);
                }
            }

            long partGen = RocketPartCostCalculator.calculateEnergyGenerationPerTick(cfg, comp);
            if (partGen > 0L) {
                boolean isSolar = cfg.familyId().toLowerCase(Locale.ROOT).contains("power_solar_array");
                if (isSolar) {
                    // Separate onboard D-3He reactor trait if installed on a solar utility module
                    long aneutronic = (comp != null && comp.hasTrait(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR))
                            ? RocketPartCostCalculator.ANEUTRONIC_BOARD_REACTOR_GEN_PER_TICK
                            : 0L;
                    solarBaseGenPerTick += Math.max(0L, partGen - aneutronic);
                    nonSolarGenPerTick += aneutronic;
                } else {
                    nonSolarGenPerTick += partGen;
                }
            }
        }

        if (minThermalK == Double.MAX_VALUE) {
            minThermalK = 300.0;
        }
        if (minPressureAtm == Double.MAX_VALUE) {
            minPressureAtm = 1.0;
        }
        // Thermal Ductility equalizes thermal loads with adjacent parts (+15% toward vehicle average)
        if (hasThermalDuctility && !parts.isEmpty()) {
            double avgThermal = sumThermalK / parts.size();
            if (avgThermal > minThermalK) {
                minThermalK = minThermalK + (avgThermal - minThermalK) * 0.35;
            }
        }

        double avgDensityKgPerBucket = totalFuelCapacityDroplets > 0L
                ? (weightedDensitySum / totalFuelCapacityDroplets)
                : PropellantType.RP1.densityKgPerBucket();
        double maxPropMassKg = (totalFuelCapacityDroplets / DROPLETS_PER_BUCKET) * avgDensityKgPerBucket;
        long clampedCurrentFuel = Math.min(vehicle.currentFuelDroplets(), totalFuelCapacityDroplets);
        double curPropMassKg = (clampedCurrentFuel / DROPLETS_PER_BUCKET) * avgDensityKgPerBucket;
        double wetMassKg = dryMassKg + maxPropMassKg;
        double currentWetMassKg = dryMassKg + curPropMassKg;

        double originTwr = computeTwr(totalThrustKn, wetMassKg, origin.gravityG());
        double destTwr = computeTwr(totalThrustKn, wetMassKg, destination.gravityG());
        boolean canLiftoffOrigin = totalThrustKn > 0.0 && (origin.gravityG() <= 0.001 || originTwr > 1.0);
        boolean canLiftoffDest = totalThrustKn > 0.0 && (destination.gravityG() <= 0.001 || destTwr > 1.0);

        double effectiveIsp = engineThrustSum > 0.0 ? (weightedIspSum / engineThrustSum) : PropellantType.RP1.baseIspSeconds();
        if (hasFlightComputer) {
            effectiveIsp *= 1.10;
        }
        if (hasLightweightAlloy) {
            effectiveIsp *= 1.04;
        }

        double maxDeltaV = computeDeltaV(effectiveIsp, dryMassKg, wetMassKg);
        double currentDeltaV = computeDeltaV(effectiveIsp, dryMassKg, currentWetMassKg);
        double requiredDeltaV = destination.requiredDeltaVFrom(origin);
        boolean hasEnoughDeltaV = maxDeltaV >= requiredDeltaV;

        // Solar 1/r^2 electrical balance at destination
        double solarFactor = destination.solarIrradianceFactor();
        long scaledSolarGen = Math.round(solarBaseGenPerTick * solarFactor);
        long passivePowerGen = scaledSolarGen + nonSolarGenPerTick;
        long cryocoolerDraw = (hasCryocoolerModule && !hasSuperfluidCryo2k) ? CRYOCOOLER_DRAW_GJ_PER_TICK : 0L;
        long totalDraw = BASE_AVIONICS_DRAW_GJ_PER_TICK + cryocoolerDraw;
        long netPower = passivePowerGen - totalDraw;
        boolean powerDeficit = netPower < 0L && dfdBurnGenPerTick <= 0L;
        boolean cryocoolerPowered = hasCryocoolerModule && (vehicle.currentEnergyGj() > 0L || passivePowerGen >= totalDraw);

        // Cryogenic boil-off rate at destination mean ambient temperature
        PropellantType activePropellant = vehicle.loadedFuelFluid() != null
                ? PropellantType.fromFluid(vehicle.loadedFuelFluid())
                : dominantEnginePropellant;
        boolean hasCryoPropellant = (vehicle.loadedFuelFluid() != null && PropellantType.isCryogenicFluid(vehicle.loadedFuelFluid()))
                || (activePropellant != null && activePropellant.isCryogenic());

        double boilOffDropletsPerMinute = 0.0;
        if (hasCryoPropellant && totalFuelCapacityDroplets > 0L) {
            long effectiveFuelForRate = clampedCurrentFuel > 0L ? clampedCurrentFuel : totalFuelCapacityDroplets;
            boilOffDropletsPerMinute = computeVehicleBoilOffDropletsPerMinute(
                    parts,
                    vehicle.loadedFuelFluid(),
                    activePropellant,
                    effectiveFuelForRate,
                    destination.meanTempKelvin(),
                    hasCryocoolerModule,
                    cryocoolerPowered,
                    hasSuperfluidCryo2k
            );
        }
        boolean zeroBoilOffActive = hasCryoPropellant && boilOffDropletsPerMinute <= 0.001;

        // Propellant compatibility check
        boolean propCompatible = true;
        String propStatus = "Nominal";
        if (dominantEnginePropellant != null) {
            if (vehicle.loadedFuelFluid() != null) {
                if (!dominantEnginePropellant.acceptsFluid(vehicle.loadedFuelFluid())) {
                    propCompatible = false;
                    propStatus = "Incompatível: requer " + dominantEnginePropellant.requiredFluidLabel();
                } else {
                    propStatus = "OK: " + dominantEnginePropellant.displayName();
                }
            } else if (!tankTypes.isEmpty()) {
                boolean anyMatch = false;
                for (PropellantType tt : tankTypes) {
                    if (dominantEnginePropellant.acceptsTankType(tt)) {
                        anyMatch = true;
                        break;
                    }
                }
                if (!anyMatch) {
                    propCompatible = false;
                    propStatus = "Tanque incompatível p/ " + dominantEnginePropellant.displayName();
                } else {
                    propStatus = dominantEnginePropellant.displayName();
                }
            } else if (dominantEnginePropellant != PropellantType.SOLID) {
                propCompatible = false;
                propStatus = "Sem tanque p/ " + dominantEnginePropellant.displayName();
            } else {
                propStatus = dominantEnginePropellant.displayName();
            }
        }

        // Thermal & pressure survival + estimated reentry integrity degradation
        double entryHeating = destination.atmosphericEntryHeatingKelvin();
        if (hasAblativeTps) {
            entryHeating *= 0.35;
        }
        if (hasParachute && destination.pressureAtm() >= 0.05) {
            entryHeating *= 0.70;
        }
        double effectiveThermalDemandK = destination.peakTempKelvin() + entryHeating;
        boolean thermalSafe = minThermalK >= effectiveThermalDemandK;
        boolean pressureSafe = minPressureAtm >= destination.pressureAtm();
        boolean acidProtected = (criticalPartCount > 0 && allCriticalAcidProtected) || anyAcidShield;
        boolean acidHazard = destination.corrosiveAcid() && !acidProtected;

        double estimatedEntryLoss = 0.0;
        for (PartSnapshot part : parts) {
            double partLoss = computeEntryIntegrityDamage(part, destination, entryHeating);
            estimatedEntryLoss = Math.max(estimatedEntryLoss, partLoss);
        }

        List<FlightHazard> hazards = new ArrayList<>();
        if (!canLiftoffOrigin) {
            hazards.add(new FlightHazard(
                    HazardCode.LOW_ORIGIN_TWR,
                    HazardSeverity.CRITICAL,
                    String.format(Locale.ROOT, "T/W origem %.2f <= 1.0 (%.2fG)", originTwr, origin.gravityG())
            ));
        }
        if (!canLiftoffDest && destination.gravityG() > 0.001) {
            hazards.add(new FlightHazard(
                    HazardCode.LOW_DESTINATION_TWR,
                    HazardSeverity.WARNING,
                    String.format(Locale.ROOT, "T/W destino %.2f <= 1.0 (%.2fG)", destTwr, destination.gravityG())
            ));
        }
        if (!hasEnoughDeltaV) {
            hazards.add(new FlightHazard(
                    HazardCode.INSUFFICIENT_DELTA_V,
                    HazardSeverity.WARNING,
                    String.format(Locale.ROOT, "dV %.0f < %.0f m/s", maxDeltaV, requiredDeltaV)
            ));
        }
        if (!thermalSafe) {
            hazards.add(new FlightHazard(
                    HazardCode.THERMAL_HAZARD,
                    HazardSeverity.CRITICAL,
                    String.format(Locale.ROOT, "Calor %.0fK > %.0fK (dano casco)", effectiveThermalDemandK, minThermalK)
            ));
        }
        if (!pressureSafe) {
            hazards.add(new FlightHazard(
                    HazardCode.PRESSURE_HAZARD,
                    HazardSeverity.CRITICAL,
                    String.format(Locale.ROOT, "Pressão %.1f > %.1f atm", destination.pressureAtm(), minPressureAtm)
            ));
        }
        if (acidHazard) {
            hazards.add(new FlightHazard(
                    HazardCode.ACID_CORROSION,
                    HazardSeverity.CRITICAL,
                    "Ácido H2SO4: sem Chumbo/Lonsdaleíta"
            ));
        }
        if (powerDeficit) {
            hazards.add(new FlightHazard(
                    HazardCode.LOW_SOLAR_POWER,
                    HazardSeverity.WARNING,
                    String.format(Locale.ROOT, "Solar %.4fx (use RTG/3He)", solarFactor)
            ));
        }
        if (!propCompatible) {
            hazards.add(new FlightHazard(
                    HazardCode.PROPELLANT_MISMATCH,
                    HazardSeverity.CRITICAL,
                    propStatus
            ));
        }
        if (boilOffDropletsPerMinute > 0.0) {
            hazards.add(new FlightHazard(
                    HazardCode.CRYOGENIC_BOIL_OFF,
                    HazardSeverity.INFO,
                    String.format(Locale.ROOT, "Boil-off: %.2f B/min", boilOffDropletsPerMinute / DROPLETS_PER_BUCKET)
            ));
        }
        if (minIntegrityFrac < 0.70) {
            hazards.add(new FlightHazard(
                    HazardCode.DAMAGED_STRUCTURE,
                    HazardSeverity.WARNING,
                    String.format(Locale.ROOT, "Integridade %.0f%% (recondicione)", minIntegrityFrac * 100.0)
            ));
        }

        return new FlightValidationReport(
                origin,
                destination,
                dryMassKg,
                maxPropMassKg,
                curPropMassKg,
                wetMassKg,
                currentWetMassKg,
                totalThrustKn,
                originTwr,
                destTwr,
                effectiveIsp,
                currentDeltaV,
                maxDeltaV,
                requiredDeltaV,
                minThermalK,
                effectiveThermalDemandK,
                thermalSafe,
                minPressureAtm,
                destination.pressureAtm(),
                pressureSafe,
                acidProtected,
                acidHazard,
                solarFactor,
                passivePowerGen,
                dfdBurnGenPerTick,
                totalDraw,
                netPower,
                powerDeficit,
                boilOffDropletsPerMinute,
                zeroBoilOffActive,
                cryocoolerPowered,
                hasSuperfluidCryo2k,
                propCompatible,
                dominantEnginePropellant,
                propStatus,
                canLiftoffOrigin,
                canLiftoffDest,
                hasEnoughDeltaV,
                estimatedEntryLoss,
                List.copyOf(hazards)
        );
    }

    public static final double VACUUM_NOZZLE_MAX_SAFE_ATM = 0.40;

    /**
     * Evaluates a compiled multi-stage {@link RocketAssemblyGraph} against {@code origin} and
     * {@code destination} planetary environments using recursive per-stage Tsiolkovsky delta-V,
     * altitude/pressure-interpolated {@code Isp(P_a)}, vacuum nozzle over-expansion checks
     * ({@code P_a > 0.40 atm}), Asparagus radial booster crossfeed staging, and pad mass limit checks.
     */
    public static @NotNull GraphFlightValidationReport evaluateGraph(
            @NotNull RocketAssemblyGraph graph,
            @NotNull PlanetaryEnvironment origin,
            @NotNull PlanetaryEnvironment destination
    ) {
        return evaluateGraph(graph, origin, destination, Double.POSITIVE_INFINITY, 0L);
    }

    /**
     * Evaluates a compiled multi-stage {@link RocketAssemblyGraph} with explicit launch pad mass
     * capacity ({@code padMaxMassKg}) and stored electrical energy ({@code currentEnergyGj}).
     */
    public static @NotNull GraphFlightValidationReport evaluateGraph(
            @NotNull RocketAssemblyGraph graph,
            @NotNull PlanetaryEnvironment origin,
            @NotNull PlanetaryEnvironment destination,
            double padMaxMassKg,
            long currentEnergyGj
    ) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");

        List<RocketStageNode> activeStages = graph.activeStages();
        if (activeStages.isEmpty()) {
            FlightValidationReport empty = evaluate(new VehicleSnapshot(List.of(), 0L, currentEnergyGj, null), origin, destination);
            return new GraphFlightValidationReport(empty, List.of(), padMaxMassKg, true, false);
        }

        // Convert all active parts into PartSnapshots for unified thermal, pressure, acid, power, and boil-off checks
        List<MountedPartNode> allActiveParts = graph.allActiveParts();
        List<PartSnapshot> allSnapshots = new ArrayList<>(allActiveParts.size());
        boolean hasFlightComputer = false;
        boolean hasLightweightAlloy = false;
        for (MountedPartNode node : allActiveParts) {
            PartSnapshot snap = PartSnapshot.fromParts(
                    node.config(),
                    node.stats(),
                    node.integrity(),
                    node.composition().orElse(null)
            );
            allSnapshots.add(snap);
            if (snap.hasTrait(RocketMaterialTrait.FLIGHT_COMPUTER)) {
                hasFlightComputer = true;
            }
            if (snap.hasTrait(RocketMaterialTrait.LIGHTWEIGHT_ALLOY)) {
                hasLightweightAlloy = true;
            }
        }

        double ispBonusMultiplier = 1.0;
        if (hasFlightComputer) {
            ispBonusMultiplier *= 1.10;
        }
        if (hasLightweightAlloy) {
            ispBonusMultiplier *= 1.04;
        }

        FlightValidationReport baseReport = evaluate(
                new VehicleSnapshot(allSnapshots, graph.totalCurrentFuelDroplets(), currentEnergyGj, null),
                origin,
                destination
        );

        // Precompute per-stage full wet mass and current wet mass for recursive upper-payload stacking
        int stageCount = activeStages.size();
        double[] stageMaxWetKg = new double[stageCount];
        double[] stageCurWetKg = new double[stageCount];
        double[] stageDryKg = new double[stageCount];
        double[] stageMaxPropKg = new double[stageCount];
        double[] stageCurPropKg = new double[stageCount];

        for (int i = 0; i < stageCount; i++) {
            RocketStageNode stage = activeStages.get(i);
            PropellantType coreProp = resolvePropellantById(stage.propellantId());
            double coreDry = stage.coreDryMassKg();
            double coreMaxProp = (stage.fuelCapacityDroplets() / DROPLETS_PER_BUCKET) * coreProp.densityKgPerBucket();
            double coreCurProp = (stage.currentFuelDroplets() / DROPLETS_PER_BUCKET) * coreProp.densityKgPerBucket();

            double radDry = 0.0;
            double radMaxProp = 0.0;
            double radCurProp = 0.0;
            for (RadialBoosterGroup group : stage.radialGroups()) {
                if (group.decoupled()) {
                    continue;
                }
                PropellantType gProp = resolvePropellantById(group.propellantId());
                radDry += group.dryMassKg();
                radMaxProp += (group.fuelCapacityDroplets() / DROPLETS_PER_BUCKET) * gProp.densityKgPerBucket();
                radCurProp += (group.currentFuelDroplets() / DROPLETS_PER_BUCKET) * gProp.densityKgPerBucket();
            }

            stageDryKg[i] = coreDry + radDry;
            stageMaxPropKg[i] = coreMaxProp + radMaxProp;
            stageCurPropKg[i] = coreCurProp + radCurProp;
            stageMaxWetKg[i] = stageDryKg[i] + stageMaxPropKg[i];
            stageCurWetKg[i] = stageDryKg[i] + stageCurPropKg[i];
        }

        double totalDryMassKg = 0.0;
        double totalMaxPropMassKg = 0.0;
        double totalCurPropMassKg = 0.0;
        for (int i = 0; i < stageCount; i++) {
            totalDryMassKg += stageDryKg[i];
            totalMaxPropMassKg += stageMaxPropKg[i];
            totalCurPropMassKg += stageCurPropKg[i];
        }
        double totalWetMassKg = totalDryMassKg + totalMaxPropMassKg;
        double totalCurrentWetMassKg = totalDryMassKg + totalCurPropMassKg;

        // Recursive per-stage Tsiolkovsky delta-V & atmospheric Isp evaluation
        List<StageFlightReport> stageReports = new ArrayList<>(stageCount);
        double totalMaxDeltaV = 0.0;
        double totalCurrentDeltaV = 0.0;
        boolean anyVacuumNozzleWarning = false;
        double liftoffThrustKn = 0.0;
        double liftoffEffectiveIsp = 0.0;
        double finalStageDestTwr = baseReport.destinationTwr();

        for (int i = 0; i < stageCount; i++) {
            RocketStageNode stage = activeStages.get(i);
            double ambientPa = (i == 0) ? origin.pressureAtm() : 0.0;

            double upperMaxMassKg = 0.0;
            double upperCurMassKg = 0.0;
            for (int j = i + 1; j < stageCount; j++) {
                upperMaxMassKg += stageMaxWetKg[j];
                upperCurMassKg += stageCurWetKg[j];
            }

            StageDeltaVResult maxRes = computeStageDeltaV(stage, ambientPa, upperMaxMassKg, ispBonusMultiplier, true);
            StageDeltaVResult curRes = computeStageDeltaV(stage, ambientPa, upperCurMassKg, ispBonusMultiplier, false);

            double totalStackMassAtStageIgnition = upperMaxMassKg + stageMaxWetKg[i];
            double stageGravityG = (i == 0) ? origin.gravityG() : destination.gravityG();
            double stageTwr = computeTwr(maxRes.initialThrustKn, totalStackMassAtStageIgnition, stageGravityG);

            boolean vacHazard = maxRes.hasVacuumEngine && ambientPa > VACUUM_NOZZLE_MAX_SAFE_ATM;
            anyVacuumNozzleWarning |= vacHazard;

            int activeRadialCount = 0;
            for (RadialBoosterGroup g : stage.radialGroups()) {
                if (!g.decoupled()) {
                    activeRadialCount++;
                }
            }

            if (i == 0) {
                liftoffThrustKn = maxRes.initialThrustKn;
                liftoffEffectiveIsp = maxRes.effectiveIspSeconds;
            }
            if (i == stageCount - 1) {
                // Uppermost propulsion stage determines destination landing/ascent capability when engines exist
                if (maxRes.initialThrustKn > 0.0) {
                    finalStageDestTwr = computeTwr(maxRes.initialThrustKn, stageMaxWetKg[i], destination.gravityG());
                } else {
                    finalStageDestTwr = computeTwr(liftoffThrustKn, totalWetMassKg, destination.gravityG());
                }
            }

            totalMaxDeltaV += maxRes.deltaV;
            totalCurrentDeltaV += curRes.deltaV;

            stageReports.add(new StageFlightReport(
                    stage.stageIndex(),
                    stage.propellantId(),
                    ambientPa,
                    stageDryKg[i],
                    stageMaxWetKg[i],
                    maxRes.initialThrustKn,
                    stageTwr,
                    maxRes.effectiveIspSeconds,
                    maxRes.deltaV,
                    curRes.deltaV,
                    stage.hotStaging(),
                    vacHazard,
                    activeRadialCount
            ));
        }

        // Compute origin & destination liftoff checks using Stage 0 thrust against total wet mass
        double originTwr = computeTwr(liftoffThrustKn, totalWetMassKg, origin.gravityG());
        double destTwr = finalStageDestTwr;
        boolean canLiftoffOrigin = liftoffThrustKn > 0.0 && (origin.gravityG() <= 0.001 || originTwr > 1.0);
        boolean canLiftoffDest = (liftoffThrustKn > 0.0 || destTwr > 0.0) && (destination.gravityG() <= 0.001 || destTwr > 1.0);
        double requiredDeltaV = destination.requiredDeltaVFrom(origin);
        boolean hasEnoughDeltaV = totalMaxDeltaV >= requiredDeltaV;
        boolean withinPadMassLimit = !Double.isFinite(padMaxMassKg) || totalWetMassKg <= padMaxMassKg;

        // Rebuild hazard list with accurate staged TWR, staged delta-V, vacuum nozzle, and pad mass limit checks
        List<FlightHazard> hazards = new ArrayList<>();
        for (FlightHazard existing : baseReport.hazards()) {
            // Re-evaluate TWR and Delta-V hazards with multi-stage figures
            if (existing.code() == HazardCode.LOW_ORIGIN_TWR
                    || existing.code() == HazardCode.LOW_DESTINATION_TWR
                    || existing.code() == HazardCode.INSUFFICIENT_DELTA_V) {
                continue;
            }
            hazards.add(existing);
        }
        if (!canLiftoffOrigin) {
            hazards.add(new FlightHazard(
                    HazardCode.LOW_ORIGIN_TWR,
                    HazardSeverity.CRITICAL,
                    String.format(Locale.ROOT, "T/W Estágio 0 %.2f <= 1.0 (%.2fG)", originTwr, origin.gravityG())
            ));
        }
        if (!canLiftoffDest && destination.gravityG() > 0.001) {
            hazards.add(new FlightHazard(
                    HazardCode.LOW_DESTINATION_TWR,
                    HazardSeverity.WARNING,
                    String.format(Locale.ROOT, "T/W destino %.2f <= 1.0 (%.2fG)", destTwr, destination.gravityG())
            ));
        }
        if (!hasEnoughDeltaV) {
            hazards.add(new FlightHazard(
                    HazardCode.INSUFFICIENT_DELTA_V,
                    HazardSeverity.WARNING,
                    String.format(Locale.ROOT, "dV multi-estágio %.0f < %.0f m/s", totalMaxDeltaV, requiredDeltaV)
            ));
        }
        if (anyVacuumNozzleWarning) {
            hazards.add(new FlightHazard(
                    HazardCode.VACUUM_NOZZLE_OVEREXPANSION,
                    HazardSeverity.WARNING,
                    String.format(Locale.ROOT, "Bocal de vácuo em %.2f atm (> 0.40 atm): perda de Isp/vibração", origin.pressureAtm())
            ));
        }
        if (!withinPadMassLimit) {
            hazards.add(new FlightHazard(
                    HazardCode.PAD_OVERWEIGHT,
                    HazardSeverity.CRITICAL,
                    String.format(Locale.ROOT, "Massa %.1ft excede limite do pad (%.1ft)", totalWetMassKg / 1000.0, padMaxMassKg / 1000.0)
            ));
        }

        double effectiveIsp = liftoffEffectiveIsp > 0.0 ? liftoffEffectiveIsp : baseReport.effectiveIspSeconds();

        FlightValidationReport mergedSummary = new FlightValidationReport(
                origin,
                destination,
                totalDryMassKg,
                totalMaxPropMassKg,
                totalCurPropMassKg,
                totalWetMassKg,
                totalCurrentWetMassKg,
                liftoffThrustKn,
                originTwr,
                destTwr,
                effectiveIsp,
                totalCurrentDeltaV,
                totalMaxDeltaV,
                requiredDeltaV,
                baseReport.minThermalToleranceKelvin(),
                baseReport.destinationPeakTempKelvin(),
                baseReport.thermalSafe(),
                baseReport.minPressureToleranceAtm(),
                baseReport.destinationPressureAtm(),
                baseReport.pressureSafe(),
                baseReport.acidProtected(),
                baseReport.acidHazard(),
                baseReport.solarIrradianceFactor(),
                baseReport.passivePowerGenPerTick(),
                baseReport.dfdBurnPowerGenPerTick(),
                baseReport.powerDrawPerTick(),
                baseReport.netPowerPerTick(),
                baseReport.powerDeficit(),
                baseReport.boilOffDropletsPerMinute(),
                baseReport.zeroBoilOffActive(),
                baseReport.cryocoolerPowered(),
                baseReport.superfluidCryo2kActive(),
                baseReport.propellantCompatible(),
                baseReport.expectedPropellant(),
                baseReport.propellantStatusMessage(),
                canLiftoffOrigin,
                canLiftoffDest,
                hasEnoughDeltaV,
                baseReport.estimatedEntryIntegrityLoss(),
                List.copyOf(hazards)
        );

        return new GraphFlightValidationReport(
                mergedSummary,
                stageReports,
                padMaxMassKg,
                withinPadMassLimit,
                anyVacuumNozzleWarning
        );
    }

    /**
     * Returns whether {@code config} is a high-expansion-ratio vacuum engine nozzle that suffers
     * flow separation and rapid wear if fired at {@code P_a > 0.40 atm}.
     */
    public static boolean isVacuumOptimizedEngine(@NotNull RocketPartConfig config) {
        if (config.category() != RocketPartCategory.ENGINE) {
            return false;
        }
        String id = (config.familyId() + "_" + config.modelId()).toLowerCase(Locale.ROOT);
        return id.contains("vac")
                || id.contains("rl10")
                || id.contains("nerva")
                || id.contains("ntr")
                || id.contains("ion")
                || id.contains("vasimr")
                || id.contains("plasma")
                || id.contains("draco");
    }

    /**
     * Computes altitude/pressure-interpolated specific impulse {@code Isp(P_a)}:
     * {@code Isp(P_a) = Isp_vac - min(1.0, P_a) * (Isp_vac - Isp_SL)}.
     */
    public static double computeAtmosphericIsp(
            @NotNull RocketPartConfig engineConfig,
            @NotNull PropellantType propellant,
            @Nullable RocketPartComposition composition,
            double ambientPressureAtm
    ) {
        double vacIsp = computeEngineIsp(propellant, composition);
        double slFactor;
        if (propellant == PropellantType.XENON_ELECTRIC) {
            slFactor = 0.15;
        } else if (isVacuumOptimizedEngine(engineConfig)) {
            slFactor = 0.42;
        } else {
            slFactor = 0.88;
        }
        double slIsp = vacIsp * slFactor;
        double clampedPa = Math.clamp(ambientPressureAtm, 0.0, 1.0);
        return vacIsp - clampedPa * (vacIsp - slIsp);
    }

    private record EngineGroupMetrics(double thrustKn, double effectiveIsp, boolean hasVacuumEngine) {
    }

    private record StageDeltaVResult(double deltaV, double initialThrustKn, double effectiveIspSeconds, boolean hasVacuumEngine) {
    }

    private static EngineGroupMetrics computeEngineMetrics(
            List<MountedPartNode> parts,
            double ambientPa,
            double ispBonusMultiplier
    ) {
        double totalThrustKn = 0.0;
        double massFlowSum = 0.0;
        boolean hasVac = false;
        for (MountedPartNode part : parts) {
            RocketPartConfig cfg = part.config();
            if (cfg.category() == RocketPartCategory.ENGINE && part.stats().thrustKn() > 0.0) {
                PropellantType engProp = PropellantType.forEngine(cfg);
                double isp = computeAtmosphericIsp(cfg, engProp, part.composition().orElse(null), ambientPa) * ispBonusMultiplier;
                if (isp > 0.0) {
                    totalThrustKn += part.stats().thrustKn();
                    massFlowSum += part.stats().thrustKn() / isp;
                }
                hasVac |= isVacuumOptimizedEngine(cfg);
            }
        }
        double effIsp = (totalThrustKn > 0.0 && massFlowSum > 0.0) ? (totalThrustKn / massFlowSum) : 0.0;
        return new EngineGroupMetrics(totalThrustKn, effIsp, hasVac);
    }

    private static StageDeltaVResult computeStageDeltaV(
            RocketStageNode stage,
            double ambientPa,
            double upperPayloadMassKg,
            double ispBonusMultiplier,
            boolean useMaxFuel
    ) {
        PropellantType coreProp = resolvePropellantById(stage.propellantId());
        EngineGroupMetrics coreEng = computeEngineMetrics(stage.coreParts(), ambientPa, ispBonusMultiplier);
        double coreDryKg = stage.coreDryMassKg();
        long coreFuelDroplets = useMaxFuel ? stage.fuelCapacityDroplets() : stage.currentFuelDroplets();
        double remainingCorePropKg = (coreFuelDroplets / DROPLETS_PER_BUCKET) * coreProp.densityKgPerBucket();

        // Collect active radial booster groups ordered by drainPriorityLayer ascending (0 = outermost first)
        List<ActiveBoosterState> boosters = new ArrayList<>();
        for (RadialBoosterGroup group : stage.radialGroups()) {
            if (group.decoupled()) {
                continue;
            }
            PropellantType gProp = resolvePropellantById(group.propellantId());
            EngineGroupMetrics gEng = computeEngineMetrics(group.parts(), ambientPa, ispBonusMultiplier);
            long gFuelDroplets = useMaxFuel ? group.fuelCapacityDroplets() : group.currentFuelDroplets();
            double gPropKg = (gFuelDroplets / DROPLETS_PER_BUCKET) * gProp.densityKgPerBucket();
            boosters.add(new ActiveBoosterState(
                    group.drainPriorityLayer(),
                    group.crossfeedMode(),
                    group.solidBooster(),
                    group.dryMassKg(),
                    gPropKg,
                    gEng.thrustKn,
                    gEng.effectiveIsp,
                    gEng.hasVacuumEngine
            ));
        }
        boosters.sort(Comparator.comparingInt(b -> b.drainPriorityLayer));

        double initialThrustKn = coreEng.thrustKn;
        double initialMassFlow = (coreEng.thrustKn > 0.0 && coreEng.effectiveIsp > 0.0)
                ? (coreEng.thrustKn / coreEng.effectiveIsp)
                : 0.0;
        boolean hasVac = coreEng.hasVacuumEngine;
        for (ActiveBoosterState b : boosters) {
            initialThrustKn += b.thrustKn;
            if (b.thrustKn > 0.0 && b.isp > 0.0) {
                initialMassFlow += b.thrustKn / b.isp;
            }
            hasVac |= b.hasVacuumEngine;
        }
        double initialIsp = (initialThrustKn > 0.0 && initialMassFlow > 0.0)
                ? (initialThrustKn / initialMassFlow)
                : coreEng.effectiveIsp;

        double totalDeltaV = 0.0;

        // Burn through radial booster layers outermost -> innermost, simulating Asparagus vs Isolated crossfeed
        int startIdx = 0;
        while (startIdx < boosters.size()) {
            int currentLayer = boosters.get(startIdx).drainPriorityLayer;
            int endIdx = startIdx;
            while (endIdx < boosters.size() && boosters.get(endIdx).drainPriorityLayer == currentLayer) {
                endIdx++;
            }

            double layerPropKg = 0.0;
            double layerThrustKn = 0.0;
            double layerMassFlowKgPerSec = 0.0;
            boolean layerAsparagus = false;
            for (int k = startIdx; k < endIdx; k++) {
                ActiveBoosterState b = boosters.get(k);
                layerPropKg += b.remainingPropKg;
                layerThrustKn += b.thrustKn;
                if (b.thrustKn > 0.0 && b.isp > 0.0) {
                    layerMassFlowKgPerSec += (b.thrustKn * 1000.0) / (b.isp * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
                }
                if (!b.solidBooster && b.crossfeedMode == CrossfeedMode.CONTINUOUS_CROSSFEED) {
                    layerAsparagus = true;
                }
            }

            // Compute active engines + total vehicle mass at start of this booster burn phase
            double activeThrustKn = coreEng.thrustKn;
            double activeFlowKgPerSec = (coreEng.thrustKn > 0.0 && coreEng.effectiveIsp > 0.0)
                    ? (coreEng.thrustKn * 1000.0) / (coreEng.effectiveIsp * PlanetaryEnvironment.STANDARD_GRAVITY_MS2)
                    : 0.0;
            double activeBoostersMassKg = 0.0;
            for (int k = startIdx; k < boosters.size(); k++) {
                ActiveBoosterState b = boosters.get(k);
                activeBoostersMassKg += b.dryMassKg + b.remainingPropKg;
                activeThrustKn += b.thrustKn;
                if (b.thrustKn > 0.0 && b.isp > 0.0) {
                    activeFlowKgPerSec += (b.thrustKn * 1000.0) / (b.isp * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
                }
            }

            double mStart = upperPayloadMassKg + coreDryKg + remainingCorePropKg + activeBoostersMassKg;
            if (activeThrustKn > 0.0 && activeFlowKgPerSec > 0.0 && layerPropKg > 0.0) {
                double phaseIsp = (activeThrustKn * 1000.0) / (activeFlowKgPerSec * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
                double propBurnedKg;

                if (layerAsparagus) {
                    // Asparagus crossfeed: all active liquid engines feed from outermost layer first,
                    // keeping inner boosters and core at 100% propellant at jettison!
                    double liquidFeedFlow = activeFlowKgPerSec;
                    double srbCoBurnFlow = 0.0;
                    for (int k = endIdx; k < boosters.size(); k++) {
                        ActiveBoosterState inner = boosters.get(k);
                        if (inner.solidBooster && inner.thrustKn > 0.0 && inner.isp > 0.0) {
                            double f = (inner.thrustKn * 1000.0) / (inner.isp * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
                            liquidFeedFlow -= f;
                            srbCoBurnFlow += f;
                        }
                    }
                    double burnSec = liquidFeedFlow > 0.0 ? (layerPropKg / liquidFeedFlow) : 0.0;
                    propBurnedKg = layerPropKg;
                    if (srbCoBurnFlow > 0.0 && burnSec > 0.0) {
                        for (int k = endIdx; k < boosters.size(); k++) {
                            ActiveBoosterState inner = boosters.get(k);
                            if (inner.solidBooster && inner.thrustKn > 0.0 && inner.isp > 0.0) {
                                double f = (inner.thrustKn * 1000.0) / (inner.isp * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
                                double used = Math.min(inner.remainingPropKg, f * burnSec);
                                inner.remainingPropKg -= used;
                                propBurnedKg += used;
                            }
                        }
                    }
                } else {
                    // Isolated boosters (or SRBs): burn their own propellant while core & inner boosters burn proportionally
                    double burnSec = layerMassFlowKgPerSec > 0.0 ? (layerPropKg / layerMassFlowKgPerSec) : 0.0;
                    propBurnedKg = layerPropKg;
                    if (burnSec > 0.0 && coreEng.thrustKn > 0.0 && coreEng.effectiveIsp > 0.0) {
                        double coreFlow = (coreEng.thrustKn * 1000.0) / (coreEng.effectiveIsp * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
                        double coreUsed = Math.min(remainingCorePropKg, coreFlow * burnSec);
                        remainingCorePropKg -= coreUsed;
                        propBurnedKg += coreUsed;
                    }
                    if (burnSec > 0.0) {
                        for (int k = endIdx; k < boosters.size(); k++) {
                            ActiveBoosterState inner = boosters.get(k);
                            if (inner.thrustKn > 0.0 && inner.isp > 0.0) {
                                double innerFlow = (inner.thrustKn * 1000.0) / (inner.isp * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
                                double innerUsed = Math.min(inner.remainingPropKg, innerFlow * burnSec);
                                inner.remainingPropKg -= innerUsed;
                                propBurnedKg += innerUsed;
                            }
                        }
                    }
                }

                double mEnd = Math.max(upperPayloadMassKg + coreDryKg, mStart - propBurnedKg);
                if (mStart > mEnd && mEnd > 0.0) {
                    totalDeltaV += computeDeltaV(phaseIsp, mEnd, mStart);
                }
            }

            // Jettison this booster layer's dry mass before advancing to next inner layer / core
            startIdx = endIdx;
        }

        // Core solo burn phase after all radial boosters have been jettisoned
        if (coreEng.thrustKn > 0.0 && coreEng.effectiveIsp > 0.0 && remainingCorePropKg > 0.0) {
            double mDryCorePhase = upperPayloadMassKg + coreDryKg;
            double mWetCorePhase = mDryCorePhase + remainingCorePropKg;
            totalDeltaV += computeDeltaV(coreEng.effectiveIsp, mDryCorePhase, mWetCorePhase);
        }

        return new StageDeltaVResult(totalDeltaV, initialThrustKn, initialIsp, hasVac);
    }

    private static PropellantType resolvePropellantById(@Nullable String propellantId) {
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

    private static final class ActiveBoosterState {
        final int drainPriorityLayer;
        final CrossfeedMode crossfeedMode;
        final boolean solidBooster;
        final double dryMassKg;
        double remainingPropKg;
        final double thrustKn;
        final double isp;
        final boolean hasVacuumEngine;

        ActiveBoosterState(
                int drainPriorityLayer,
                CrossfeedMode crossfeedMode,
                boolean solidBooster,
                double dryMassKg,
                double remainingPropKg,
                double thrustKn,
                double isp,
                boolean hasVacuumEngine
        ) {
            this.drainPriorityLayer = drainPriorityLayer;
            this.crossfeedMode = crossfeedMode;
            this.solidBooster = solidBooster;
            this.dryMassKg = dryMassKg;
            this.remainingPropKg = remainingPropKg;
            this.thrustKn = thrustKn;
            this.isp = isp;
            this.hasVacuumEngine = hasVacuumEngine;
        }
    }

    /**
     * Computes Thrust-to-Weight ratio {@code T / (m * g_local)}.
     */
    public static double computeTwr(double thrustKn, double massKg, double gravityG) {
        if (thrustKn <= 0.0 || massKg <= 0.0) {
            return 0.0;
        }
        if (gravityG <= 0.001) {
            return Double.POSITIVE_INFINITY;
        }
        return (thrustKn * 1000.0) / (massKg * gravityG * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
    }

    /**
     * Tsiolkovsky rocket equation {@code deltaV = Isp * g0 * ln(m_wet / m_dry)}.
     */
    public static double computeDeltaV(double ispSeconds, double dryMassKg, double wetMassKg) {
        if (ispSeconds <= 0.0 || dryMassKg <= 0.0 || wetMassKg <= dryMassKg) {
            return 0.0;
        }
        return ispSeconds * PlanetaryEnvironment.STANDARD_GRAVITY_MS2 * Math.log(wetMassKg / dryMassKg);
    }

    /**
     * Computes the effective specific impulse ({@code Isp} in seconds) for an engine with optional
     * Tinkers' material composition (including DFD He-3 plasma +40%, supercritical catalysis +15%,
     * and staged combustion +15%).
     */
    public static double computeEngineIsp(
            @NotNull PropellantType propellant,
            @Nullable RocketPartComposition composition
    ) {
        double isp = propellant.baseIspSeconds();
        if (composition != null && !composition.isEmpty()) {
            if (composition.hasTrait(RocketMaterialTrait.ANEUTRONIC_HE3_PLASMA)) {
                isp *= 1.40;
            }
            if (composition.hasTrait(RocketMaterialTrait.SUPERCRITICAL_CATALYSIS)) {
                isp *= 1.15;
            }
            if (composition.hasTrait(RocketMaterialTrait.STAGED_COMBUSTION)) {
                isp *= 1.15;
            }
        }
        return isp;
    }

    /**
     * Computes cryogenic boil-off rate in droplets per real-time minute for an assembled vehicle.
     *
     * <ul>
     *   <li>{@link RocketMaterialTrait#SUPERFLUID_CRYO_2K}: {@code 0.0x} boil-off with zero electrical drain</li>
     *   <li>{@code tank_upgrade_cryocooler} / {@link RocketMaterialTrait#ACTIVE_ZERO_BOILOFF} with power: {@code 0.0x}</li>
     *   <li>{@link RocketMaterialTrait#SOFI_INSULATION} / {@code ISOTHERMAL_FABRIC}: {@code <= 0.15x}</li>
     *   <li>Uninsulated metallic tank: scales with ambient temperature relative to 293.15 K</li>
     * </ul>
     */
    public static double computeVehicleBoilOffDropletsPerMinute(
            @NotNull List<PartSnapshot> parts,
            @Nullable Fluid loadedFluid,
            @Nullable PropellantType propellantHint,
            long storedFuelDroplets,
            double ambientTempKelvin,
            boolean hasCryocoolerModule,
            boolean cryocoolerPowered,
            boolean hasSuperfluidCryo2k
    ) {
        if (storedFuelDroplets <= 0L) {
            return 0.0;
        }
        double baseRate = loadedFluid != null
                ? PropellantType.baseBoilOffFractionForFluid(loadedFluid)
                : (propellantHint != null ? propellantHint.baseBoilOffFractionPerMinute() : 0.0);
        if (baseRate <= 0.0) {
            return 0.0;
        }
        if (hasSuperfluidCryo2k) {
            return 0.0;
        }
        if (hasCryocoolerModule && cryocoolerPowered) {
            return 0.0;
        }

        // Weighted average of tank boil-off multipliers across all propellant-bearing tanks
        double weightedMult = 0.0;
        long totalTankCap = 0L;
        for (PartSnapshot part : parts) {
            long cap = part.stats().fuelCapacityDroplets();
            if (cap <= 0L) {
                continue;
            }
            double mult = part.boilOffMultiplier();
            if (part.hasTrait(RocketMaterialTrait.SUPERFLUID_CRYO_2K)) {
                mult = 0.0;
            } else if (part.hasTrait(RocketMaterialTrait.SOFI_INSULATION)) {
                mult = Math.min(mult, SOFI_BOIL_OFF_MULTIPLIER);
            }
            weightedMult += cap * mult;
            totalTankCap += cap;
        }
        double effectiveMult = totalTankCap > 0L ? (weightedMult / totalTankCap) : 1.0;
        if (effectiveMult <= 0.0) {
            return 0.0;
        }

        double tempFactor = Math.clamp(ambientTempKelvin / 293.15, 0.20, 3.0);
        return storedFuelDroplets * baseRate * effectiveMult * tempFactor;
    }

    /**
     * Computes continuous environmental integrity wear (in integrity points per second) inflicted
     * on a single modular part exposed to {@code env} (thermal excess, crushing pressure, and
     * sulfuric acid corrosion on Venus).
     */
    public static double computeEnvironmentalWearPerSecond(
            @NotNull PartSnapshot part,
            @NotNull PlanetaryEnvironment env
    ) {
        double wear = 0.0;
        RocketPartStats stats = part.stats();

        // 1. Ambient thermal overload
        double peakTemp = env.peakTempKelvin();
        if (peakTemp > stats.thermalToleranceKelvin()) {
            double excessK = peakTemp - stats.thermalToleranceKelvin();
            double thermalWear = (excessK / 120.0) * 1.4;
            if (part.hasTrait(RocketMaterialTrait.VACUUM_REFRACTORY)) {
                thermalWear *= 0.35;
            }
            wear += thermalWear;
        }

        // 2. Ambient pressure overload (e.g., Venus 92 atm, Jupiter 12 atm)
        if (env.pressureAtm() > stats.pressureToleranceAtm()) {
            double ratio = (env.pressureAtm() - stats.pressureToleranceAtm()) / Math.max(1.0, stats.pressureToleranceAtm());
            wear += Math.min(8.0, 0.8 + ratio * 2.2);
        }

        // 3. Corrosive H2SO4 atmosphere (Venus)
        if (env.corrosiveAcid()) {
            boolean acidImmune = part.hasTrait(RocketMaterialTrait.ACID_RADIATION_SHIELD)
                    || part.hasTrait(RocketMaterialTrait.CRYSTALLINE_LATTICE);
            if (!acidImmune) {
                wear += 1.25;
            }
        }

        return wear;
    }

    /**
     * Computes total integrity points lost during atmospheric entry and landing on {@code env}.
     */
    public static double computeEntryIntegrityDamage(
            @NotNull PartSnapshot part,
            @NotNull PlanetaryEnvironment env,
            double entryHeatingKelvin
    ) {
        double damage = 0.0;
        RocketPartStats stats = part.stats();

        double entryTemp = env.peakTempKelvin() + entryHeatingKelvin;
        if (entryTemp > stats.thermalToleranceKelvin()) {
            double excessK = entryTemp - stats.thermalToleranceKelvin();
            double thermalDamage = (excessK / 30.0);
            if (part.hasTrait(RocketMaterialTrait.ABLATIVE_TPS)) {
                thermalDamage *= 0.35;
            }
            damage += thermalDamage;
        }

        if (env.pressureAtm() > stats.pressureToleranceAtm()) {
            double ratio = (env.pressureAtm() - stats.pressureToleranceAtm()) / Math.max(1.0, stats.pressureToleranceAtm());
            damage += 10.0 + ratio * 25.0;
        }

        if (env.corrosiveAcid()) {
            boolean acidImmune = part.hasTrait(RocketMaterialTrait.ACID_RADIATION_SHIELD)
                    || part.hasTrait(RocketMaterialTrait.CRYSTALLINE_LATTICE);
            if (!acidImmune) {
                damage += 18.0;
            }
        }

        if (part.hasTrait(RocketMaterialTrait.BRUTE_RIGIDITY)) {
            damage *= 0.80;
        }
        return Math.min(part.integrity().maxIntegrity(), damage);
    }

    /**
     * Applies environmental stress for {@code exposureSeconds} directly onto a modular rocket part
     * {@link ItemStack}, mutating its {@link GCDataComponents#PART_INTEGRITY} component and returning
     * the updated {@link PartIntegrity}.
     */
    public static @NotNull PartIntegrity applyEnvironmentalWearToStack(
            @NotNull ItemStack stack,
            @NotNull PlanetaryEnvironment env,
            double exposureSeconds
    ) {
        PartSnapshot snap = PartSnapshot.fromStack(stack);
        if (snap == null || exposureSeconds <= 0.0) {
            PartIntegrity existing = stack.get(GCDataComponents.PART_INTEGRITY);
            return existing != null ? existing : PartIntegrity.pristine();
        }
        double wear = computeEnvironmentalWearPerSecond(snap, env) * exposureSeconds;
        PartIntegrity updated = snap.integrity().damage(wear);
        stack.set(GCDataComponents.PART_INTEGRITY, updated);
        return updated;
    }

    private static boolean isCriticalStructuralPart(RocketPartCategory category) {
        return category == RocketPartCategory.COMMAND
                || category == RocketPartCategory.TANK
                || category == RocketPartCategory.ENGINE;
    }
}
