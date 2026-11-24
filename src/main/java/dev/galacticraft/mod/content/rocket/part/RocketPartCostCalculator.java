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

package dev.galacticraft.mod.content.rocket.part;

import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile.SlotRole;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition.SlotMaterialEntry;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Deterministic parametric crafting cost and physical performance calculator for modular rocket parts,
 * supporting both baseline geometry profiles and Tinkers'-style per-slot {@link RocketPartComposition}
 * material equations.
 */
public final class RocketPartCostCalculator {
    public static final long BASE_ENERGY_PER_TICK = 120L;
    public static final int MIN_ASSEMBLY_TICKS = 160;
    public static final int MAX_ASSEMBLY_TICKS = 800;
    public static final long ANEUTRONIC_BOARD_REACTOR_GEN_PER_TICK = 120L;

    private static final double MIN_MASS_MULTIPLIER = 0.25;
    private static final double MIN_THRUST_MULTIPLIER = 0.25;
    private static final double MIN_CAPACITY_MULTIPLIER = 0.25;
    private static final double MIN_THERMAL_KELVIN = 300.0;
    private static final double MIN_PRESSURE_ATM = 1.0;
    private static final double MIN_INTEGRITY_MULTIPLIER = 0.50;
    private static final double MAX_INTEGRITY_MULTIPLIER = 4.00;

    private RocketPartCostCalculator() {
    }

    /**
     * Complete fabrication cost and derived aerospace telemetry profile for a configured part.
     *
     * @param slotItemMultiplier Multiplier applied to each active crafting slot's base ingredient requirement.
     * @param assemblyTimeTicks  Fabrication duration in server ticks ({@code 160..800}).
     * @param energyPerTick      MachineLib energy consumption per active fabrication tick.
     * @param stats              Derived physical stats stamped onto the manufactured item.
     * @param scrapCount         Scrap metal byproduct count produced upon completion.
     * @param boilOffMultiplier  Cryogenic propellant boil-off rate multiplier ({@code 0.0} = zero boil-off, {@code 1.0} = baseline).
     * @param maxIntegrity       Certified maximum structural integrity points stamped into {@link PartIntegrity}.
     */
    public record CostProfile(
            int slotItemMultiplier,
            int assemblyTimeTicks,
            long energyPerTick,
            @NotNull RocketPartStats stats,
            int scrapCount,
            double boilOffMultiplier,
            double maxIntegrity
    ) {
        public CostProfile(
                int slotItemMultiplier,
                int assemblyTimeTicks,
                long energyPerTick,
                @NotNull RocketPartStats stats,
                int scrapCount
        ) {
            this(slotItemMultiplier, assemblyTimeTicks, energyPerTick, stats, scrapCount, 1.0, PartIntegrity.DEFAULT_MAX_INTEGRITY);
        }

        public CostProfile {
            slotItemMultiplier = Math.max(1, slotItemMultiplier);
            assemblyTimeTicks = Math.max(MIN_ASSEMBLY_TICKS, Math.min(MAX_ASSEMBLY_TICKS, assemblyTimeTicks));
            energyPerTick = Math.max(1L, energyPerTick);
            Objects.requireNonNull(stats, "stats");
            scrapCount = Math.max(1, scrapCount);
            boilOffMultiplier = Double.isFinite(boilOffMultiplier) ? Math.max(0.0, boilOffMultiplier) : 1.0;
            maxIntegrity = Double.isFinite(maxIntegrity) ? Math.max(10.0, maxIntegrity) : PartIntegrity.DEFAULT_MAX_INTEGRITY;
        }
    }

    /**
     * Detailed per-slot physical contribution computed from a single {@link SlotMaterialEntry},
     * shared by both {@code PartAssemblerScreen} and {@code ModularRocketPartBlockItem} SHIFT tooltips.
     */
    public record SlotContribution(
            int slotIndex,
            @NotNull String slotRole,
            @NotNull String roleLabel,
            @NotNull ResourceLocation itemId,
            @Nullable RocketMaterialProfile profile,
            double massDeltaFraction,
            double thermalDeltaKelvin,
            double pressureFactor,
            double pressureDeltaAtm,
            double thrustDeltaFraction,
            double capacityDeltaFraction,
            double integrityDeltaFraction,
            double boilOffFactor,
            @NotNull List<RocketMaterialTrait> traits
    ) {
    }

    private record SlotWeights(
            double massWeight,
            double thermalWeight,
            double pressureExponent,
            double integrityWeight,
            double thrustWeight,
            double capacityWeight,
            double boilOffExponent
    ) {
    }

    /**
     * Calculates baseline crafting cost scaling and deterministic physical attributes for {@code config}.
     */
    public static @NotNull CostProfile calculate(@NotNull RocketPartConfig config) {
        return calculate(config, null);
    }

    /**
     * Calculates crafting cost scaling and Tinkers'-style parametric physical attributes for
     * {@code config} combined with per-slot material {@code composition}.
     *
     * <p>Implements the Section 5.3 equations:
     * <ul>
     *   <li>{@code Mass_final = Mass_base * max(0.25, 1.0 + sum(w_s * dM_s))}</li>
     *   <li>{@code Temp_final = max(300, Temp_base + sum(k_s * dT_s))}</li>
     *   <li>{@code Pressure_final = max(1.0, Pressure_base * prod(P_s ^ alpha_s))}</li>
     *   <li>{@code Thrust_final = Thrust_base * max(0.25, 1.0 + sum(t_s * dThrust_s))}</li>
     *   <li>{@code Capacity_final = Volume_base * max(0.25, 1.0 + sum(c_s * dCap_s))}</li>
     *   <li>{@code BoilOff_final = 0.0} if any slot provides {@code 0.0}, else {@code prod(B_s ^ beta_s)}</li>
     *   <li>{@code MaxIntegrity = 100.0 * clamp(1.0 + sum(i_s * (I_s - 1.0)), 0.50, 4.00)}</li>
     * </ul>
     */
    public static @NotNull CostProfile calculate(
            @NotNull RocketPartConfig config,
            @Nullable RocketPartComposition composition
    ) {
        Objects.requireNonNull(config, "config");

        double diameter = config.radial() ? 1.0 : config.diameter();
        double topDiameter = config.radial() ? 1.0 : config.topDiameter();
        double height = config.heightBlocks();

        double avgArea = Math.max(1.0, (diameter * diameter + topDiameter * topDiameter) * 0.5);
        double effectiveVolume = avgArea * height;

        // Smooth volume-power curve hitting canonical anchor points:
        // 1x1x1 -> 1, 3x3x2 -> 2, 5x5x3 -> 4, 7x7x4 -> 8, 9x9x5 -> 12, 11x11x6 -> 16
        int slotItemMultiplier = Math.clamp(Math.round(Math.pow(effectiveVolume, 0.57) * 0.38), 1, 16);
        int assemblyTimeTicks = Math.clamp(
                MIN_ASSEMBLY_TICKS + Math.round((Math.pow(effectiveVolume, 0.57) - 1.0) * 15.3),
                MIN_ASSEMBLY_TICKS,
                MAX_ASSEMBLY_TICKS
        );
        int scrapCount = Math.clamp((slotItemMultiplier + 1) / 2, 1, 16);

        RocketPartStats baseStats = computeStats(config, diameter, effectiveVolume);
        if (composition == null || composition.isEmpty()) {
            return new CostProfile(
                    slotItemMultiplier,
                    assemblyTimeTicks,
                    BASE_ENERGY_PER_TICK,
                    baseStats,
                    scrapCount,
                    1.0,
                    PartIntegrity.DEFAULT_MAX_INTEGRITY
            );
        }

        double massDeltaSum = 0.0;
        double thermalDeltaSum = 0.0;
        double pressureProduct = 1.0;
        double thrustDeltaSum = 0.0;
        double capacityDeltaSum = 0.0;
        double integrityDeltaSum = 0.0;
        double boilOffProduct = 1.0;
        boolean zeroBoilOff = false;

        for (SlotMaterialEntry entry : composition.entries()) {
            RocketMaterialProfile profile = RocketMaterialRegistry.resolve(entry).orElse(null);
            if (profile == null) {
                continue;
            }
            SlotWeights weights = weightsForSlot(config.category(), entry.slotIndex(), SlotRole.byName(entry.slotRole()));

            massDeltaSum += weights.massWeight() * profile.massModifier();
            thermalDeltaSum += weights.thermalWeight() * profile.thermalBonusKelvin();
            pressureProduct *= Math.pow(profile.pressureMultiplier(), weights.pressureExponent());
            integrityDeltaSum += weights.integrityWeight() * (profile.integrityMultiplier() - 1.0);

            if (profile.thrustBonus() != 0.0 && weights.thrustWeight() > 0.0) {
                thrustDeltaSum += weights.thrustWeight() * profile.thrustBonus();
            }
            if (profile.capacityBonus() != 0.0 && weights.capacityWeight() > 0.0) {
                capacityDeltaSum += weights.capacityWeight() * profile.capacityBonus();
            }
            if (weights.boilOffExponent() > 0.0) {
                if (profile.boilOffMultiplier() <= 0.0) {
                    zeroBoilOff = true;
                } else if (profile.boilOffMultiplier() != 1.0) {
                    boilOffProduct *= Math.pow(profile.boilOffMultiplier(), weights.boilOffExponent());
                }
            }
        }

        double finalMass = round1(baseStats.dryWeightKg() * Math.max(MIN_MASS_MULTIPLIER, 1.0 + massDeltaSum));
        double finalThermal = round1(Math.max(MIN_THERMAL_KELVIN, baseStats.thermalToleranceKelvin() + thermalDeltaSum));
        double finalPressure = round1(Math.max(MIN_PRESSURE_ATM, baseStats.pressureToleranceAtm() * pressureProduct));
        double finalThrust = baseStats.thrustKn() > 0.0
                ? round1(baseStats.thrustKn() * Math.max(MIN_THRUST_MULTIPLIER, 1.0 + thrustDeltaSum))
                : 0.0;
        long finalFuel = baseStats.fuelCapacityDroplets() > 0L
                ? Math.max(1L, Math.round(baseStats.fuelCapacityDroplets() * Math.max(MIN_CAPACITY_MULTIPLIER, 1.0 + capacityDeltaSum)))
                : 0L;
        double finalBoilOff = zeroBoilOff ? 0.0 : round2(Math.clamp(boilOffProduct, 0.0, 2.0));
        double integrityFactor = Math.clamp(1.0 + integrityDeltaSum, MIN_INTEGRITY_MULTIPLIER, MAX_INTEGRITY_MULTIPLIER);
        double finalMaxIntegrity = round1(PartIntegrity.DEFAULT_MAX_INTEGRITY * integrityFactor);

        RocketPartStats finalStats = new RocketPartStats(
                finalThrust,
                finalMass,
                finalFuel,
                finalPressure,
                finalThermal
        );

        return new CostProfile(
                slotItemMultiplier,
                assemblyTimeTicks,
                BASE_ENERGY_PER_TICK,
                finalStats,
                scrapCount,
                finalBoilOff,
                finalMaxIntegrity
        );
    }

    /**
     * Computes individual slot-by-slot physical deltas for {@code composition} against {@code config}'s
     * baseline telemetry.
     */
    public static @NotNull List<SlotContribution> computeSlotContributions(
            @NotNull RocketPartConfig config,
            @Nullable RocketPartComposition composition
    ) {
        Objects.requireNonNull(config, "config");
        if (composition == null || composition.isEmpty()) {
            return List.of();
        }
        RocketPartStats baseStats = calculate(config).stats();
        SchematicGridLayout layout = SchematicGridLayout.forConfig(config);
        List<SlotContribution> results = new ArrayList<>(composition.size());

        for (SlotMaterialEntry entry : composition.entries()) {
            RocketMaterialProfile profile = RocketMaterialRegistry.resolve(entry).orElse(null);
            SlotRole role = SlotRole.byName(entry.slotRole());
            SchematicGridLayout.SlotRequirement req = layout.requirement(entry.slotIndex());
            String roleLabel = req != null ? req.hintLabel() : (role != null ? role.displayName() : entry.slotRole());

            if (profile == null) {
                results.add(new SlotContribution(
                        entry.slotIndex(),
                        entry.slotRole(),
                        roleLabel,
                        entry.itemId(),
                        null,
                        0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                        entry.traits()
                ));
                continue;
            }

            SlotWeights weights = weightsForSlot(config.category(), entry.slotIndex(), role);
            double massDelta = weights.massWeight() * profile.massModifier();
            double thermalDelta = round1(weights.thermalWeight() * profile.thermalBonusKelvin());
            double pressureFactor = Math.pow(profile.pressureMultiplier(), weights.pressureExponent());
            double pressureDeltaAtm = round1(baseStats.pressureToleranceAtm() * (pressureFactor - 1.0));
            double thrustDelta = baseStats.thrustKn() > 0.0 ? weights.thrustWeight() * profile.thrustBonus() : 0.0;
            double capDelta = baseStats.fuelCapacityDroplets() > 0L ? weights.capacityWeight() * profile.capacityBonus() : 0.0;
            double integrityDelta = weights.integrityWeight() * (profile.integrityMultiplier() - 1.0);
            double boilOffFactor = weights.boilOffExponent() > 0.0
                    ? (profile.boilOffMultiplier() <= 0.0 ? 0.0 : Math.pow(profile.boilOffMultiplier(), weights.boilOffExponent()))
                    : 1.0;

            results.add(new SlotContribution(
                    entry.slotIndex(),
                    entry.slotRole(),
                    roleLabel,
                    entry.itemId(),
                    profile,
                    massDelta,
                    thermalDelta,
                    pressureFactor,
                    pressureDeltaAtm,
                    thrustDelta,
                    capDelta,
                    integrityDelta,
                    round2(boilOffFactor),
                    entry.traits()
            ));
        }

        return List.copyOf(results);
    }

    /**
     * Calculates deterministic onboard electrical energy storage capacity (in GJ) contributed by
     * {@code config} when mounted on an assembled modular rocket.
     */
    public static long calculateEnergyCapacity(@NotNull RocketPartConfig config) {
        return calculateEnergyCapacity(config, null);
    }

    /**
     * Calculates onboard electrical energy storage capacity (in GJ), factoring in installed avionics
     * traits such as {@link RocketMaterialTrait#FLIGHT_COMPUTER} (+25%) and
     * {@link RocketMaterialTrait#SUPERCONDUCTING_BUS} (+100%).
     */
    public static long calculateEnergyCapacity(
            @NotNull RocketPartConfig config,
            @Nullable RocketPartComposition composition
    ) {
        Objects.requireNonNull(config, "config");
        double effectiveVolume = computeEffectiveVolume(config);
        String idHint = (config.familyId() + "_" + config.modelId()).toLowerCase(Locale.ROOT);

        long base = 0L;
        if (idHint.contains("power_battery_bank")) {
            base = Math.round(45_000.0 * Math.pow(effectiveVolume, 0.75));
        } else if (idHint.contains("power_fuel_cell")) {
            base = Math.round(25_000.0 * Math.pow(effectiveVolume, 0.70));
        } else if (idHint.contains("power_rtg")) {
            base = Math.round(15_000.0 * Math.pow(effectiveVolume, 0.65));
        } else if (idHint.contains("power_solar_array")) {
            base = Math.round(10_000.0 * Math.pow(effectiveVolume, 0.60));
        } else if (idHint.contains("avionics_ring")) {
            base = Math.round(18_000.0 * Math.pow(effectiveVolume, 0.65));
        } else if (idHint.contains("science_orbital_laboratory")) {
            base = Math.round(15_000.0 * Math.pow(effectiveVolume, 0.65));
        } else if (config.category() == RocketPartCategory.COMMAND) {
            boolean crewed = config.walkableInterior() || idHint.contains("capsule") || idHint.contains("cockpit");
            base = Math.round((crewed ? 20_000.0 : 12_000.0) * Math.pow(effectiveVolume, 0.65));
        }

        if (base <= 0L || composition == null || composition.isEmpty()) {
            return base;
        }
        double multiplier = 1.0;
        if (composition.hasTrait(RocketMaterialTrait.FLIGHT_COMPUTER)) {
            multiplier += 0.25;
        }
        if (composition.hasTrait(RocketMaterialTrait.SUPERCONDUCTING_BUS)) {
            multiplier += 1.00;
        }
        return Math.round(base * multiplier);
    }

    /**
     * Calculates passive electrical power generation rate (in GJ/tick) provided by solar arrays,
     * RTGs, or fuel cells mounted on an assembled modular rocket.
     */
    public static long calculateEnergyGenerationPerTick(@NotNull RocketPartConfig config) {
        return calculateEnergyGenerationPerTick(config, null);
    }

    /**
     * Calculates passive electrical power generation rate (in GJ/tick), including onboard
     * {@link RocketMaterialTrait#ANEUTRONIC_BOARD_REACTOR} (+120 GJ/t clean D-3He power).
     */
    public static long calculateEnergyGenerationPerTick(
            @NotNull RocketPartConfig config,
            @Nullable RocketPartComposition composition
    ) {
        Objects.requireNonNull(config, "config");
        double effectiveVolume = computeEffectiveVolume(config);
        String idHint = (config.familyId() + "_" + config.modelId()).toLowerCase(Locale.ROOT);

        long base = 0L;
        if (idHint.contains("power_fuel_cell")) {
            base = Math.round(60.0 * Math.pow(effectiveVolume, 0.65));
        } else if (idHint.contains("power_solar_array")) {
            base = Math.round(40.0 * Math.pow(effectiveVolume, 0.65));
        } else if (idHint.contains("power_rtg")) {
            base = Math.round(25.0 * Math.pow(effectiveVolume, 0.65));
        }

        if (composition != null && composition.hasTrait(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR)) {
            base += ANEUTRONIC_BOARD_REACTOR_GEN_PER_TICK;
        }
        return base;
    }

    /**
     * Calculates pressurized life-support oxygen reserve capacity (in Fabric Transfer API droplets;
     * 81,000 droplets = 1 bucket) contributed by crewed cabins, ECLSS modules, or pressurized payloads.
     */
    public static long calculateOxygenCapacityDroplets(@NotNull RocketPartConfig config) {
        Objects.requireNonNull(config, "config");
        double effectiveVolume = computeEffectiveVolume(config);
        String idHint = (config.familyId() + "_" + config.modelId()).toLowerCase(Locale.ROOT);

        if (idHint.contains("eclss_")) {
            return Math.round(8.0 * effectiveVolume) * 81_000L;
        }
        if (config.category() == RocketPartCategory.COMMAND
                && (config.walkableInterior() || idHint.contains("capsule") || idHint.contains("cockpit"))) {
            return Math.round(4.0 * effectiveVolume) * 81_000L;
        }
        if (idHint.contains("cargo_bay_pressurized") || idHint.contains("science_orbital_laboratory")) {
            return Math.round(3.0 * effectiveVolume) * 81_000L;
        }
        return 0L;
    }

    private static SlotWeights weightsForSlot(RocketPartCategory category, int slotIndex, @Nullable SlotRole role) {
        if (role == SlotRole.PROTECTIVE_COATING || slotIndex >= 9) {
            return new SlotWeights(1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0);
        }
        return switch (category) {
            case COMMAND -> switch (slotIndex) {
                case 1 -> new SlotWeights(0.25, 1.00, 0.35, 0.20, 0.0, 0.0, 0.0);
                case 3, 5 -> new SlotWeights(0.50, 0.50, 0.50, 0.35, 0.0, 0.0, 0.0);
                case 4 -> new SlotWeights(0.20, 1.00, 0.65, 0.20, 0.0, 0.0, 0.0);
                case 7 -> new SlotWeights(0.20, 0.50, 0.25, 0.15, 0.0, 0.0, 0.0);
                default -> new SlotWeights(0.25, 0.50, 0.35, 0.20, 0.0, 0.0, 0.0);
            };
            case TANK -> switch (slotIndex) {
                case 0, 2 -> new SlotWeights(0.50, 0.50, 0.50, 0.30, 0.0, 0.50, 0.0);
                case 3, 5 -> new SlotWeights(0.25, 0.40, 0.35, 0.20, 0.0, 0.50, 0.50);
                case 6, 8 -> new SlotWeights(0.25, 0.30, 0.25, 0.15, 0.0, 0.50, 0.50);
                default -> new SlotWeights(0.25, 0.35, 0.30, 0.20, 0.0, 0.50, 0.50);
            };
            case ENGINE -> switch (slotIndex) {
                case 1 -> new SlotWeights(0.15, 0.35, 0.20, 0.15, 0.50, 0.0, 0.0);
                case 3 -> new SlotWeights(0.20, 0.40, 0.25, 0.15, 1.00, 0.0, 0.0);
                case 4 -> new SlotWeights(0.25, 0.60, 0.40, 0.20, 1.00, 0.0, 0.0);
                case 6, 8 -> new SlotWeights(0.35, 0.50, 0.40, 0.25, 0.50, 0.0, 0.0);
                case 7 -> new SlotWeights(0.25, 0.65, 0.35, 0.25, 0.50, 0.0, 0.0);
                default -> new SlotWeights(0.25, 0.50, 0.30, 0.20, 0.50, 0.0, 0.0);
            };
            case ADAPTER -> switch (slotIndex) {
                case 1, 3, 5 -> new SlotWeights(0.30, 0.35, 0.30, 0.25, 0.0, 0.0, 0.0);
                case 7 -> new SlotWeights(0.25, 0.60, 0.35, 0.25, 0.0, 0.0, 0.0);
                default -> new SlotWeights(0.25, 0.35, 0.30, 0.25, 0.0, 0.0, 0.0);
            };
            case PAYLOAD -> switch (slotIndex) {
                case 0, 2, 6, 8 -> new SlotWeights(0.25, 0.25, 0.25, 0.20, 0.0, 0.0, 0.0);
                case 4 -> new SlotWeights(0.20, 0.50, 0.35, 0.20, 0.0, 0.0, 1.00);
                default -> new SlotWeights(0.25, 0.30, 0.25, 0.20, 0.0, 0.0, 0.0);
            };
            case UTILITY -> switch (slotIndex) {
                case 1 -> new SlotWeights(0.40, 0.40, 0.40, 0.35, 0.0, 0.0, 0.0);
                case 3, 5 -> new SlotWeights(0.30, 0.35, 0.30, 0.25, 0.0, 0.0, 0.0);
                case 4 -> new SlotWeights(0.20, 0.35, 0.25, 0.15, 0.0, 0.0, 0.0);
                default -> new SlotWeights(0.25, 0.35, 0.30, 0.25, 0.0, 0.0, 0.0);
            };
        };
    }

    private static double computeEffectiveVolume(@NotNull RocketPartConfig config) {
        double diameter = config.radial() ? 1.0 : config.diameter();
        double topDiameter = config.radial() ? 1.0 : config.topDiameter();
        double height = config.heightBlocks();
        double avgArea = Math.max(1.0, (diameter * diameter + topDiameter * topDiameter) * 0.5);
        return avgArea * height;
    }

    private static RocketPartStats computeStats(RocketPartConfig config, double diameter, double effectiveVolume) {
        String idHint = (config.familyId() + "_" + config.modelId()).toLowerCase(Locale.ROOT);
        RocketPartStats.Builder builder = RocketPartStats.builder();

        switch (config.category()) {
            case ENGINE -> {
                double dryMass = round1(180.0 * Math.pow(effectiveVolume, 0.72));
                double baseThrust = 120.0 * Math.pow(effectiveVolume, 0.85);
                double thermal = 2200.0 + diameter * 60.0;
                double pressure = 12.0 + diameter * 4.0;
                double solidFuelBuckets = 0.0;

                if (idHint.contains("ion") || idHint.contains("plasma") || idHint.contains("vasimr")) {
                    baseThrust *= 0.15;
                    thermal += 400.0;
                } else if (idHint.contains("rcs")) {
                    baseThrust *= 0.08;
                    dryMass = round1(dryMass * 0.55);
                } else if (idHint.contains("srb")) {
                    baseThrust *= 1.45;
                    solidFuelBuckets = 2.0 * effectiveVolume;
                    pressure += 8.0;
                } else if (idHint.contains("ntr") || idHint.contains("nerva")) {
                    baseThrust *= 0.65;
                    thermal += 650.0;
                    dryMass = round1(dryMass * 1.25);
                } else if (idHint.contains("starcluster") || idHint.contains("novaf5")) {
                    baseThrust *= 1.35;
                    thermal += 450.0;
                    pressure += 10.0;
                }

                builder.dryWeight(dryMass)
                        .thrust(round1(baseThrust))
                        .fuelBuckets(solidFuelBuckets)
                        .pressureTolerance(round1(pressure))
                        .thermalTolerance(round1(thermal));
            }
            case TANK -> {
                boolean isUpgrade = idHint.contains("upgrade") || idHint.contains("cryocooler") || idHint.contains("copv_helium");
                double dryMass = round1((isUpgrade ? 120.0 : 95.0) * Math.pow(effectiveVolume, 0.78));
                double fuelBuckets = effectiveVolume * (isUpgrade ? 1.0 : 4.0);
                double pressure = (isUpgrade ? 28.0 : 18.0) + diameter * 5.0;
                double thermal = (idHint.contains("supercryo") || idHint.contains("cryo") ? 1350.0 : 1100.0) + diameter * 40.0;

                builder.dryWeight(dryMass)
                        .fuelBuckets(fuelBuckets)
                        .pressureTolerance(round1(pressure))
                        .thermalTolerance(round1(thermal));
            }
            case COMMAND -> {
                boolean crewed = config.walkableInterior() || idHint.contains("capsule") || idHint.contains("cockpit");
                double dryMass = round1((crewed ? 240.0 : 145.0) * Math.pow(effectiveVolume, 0.70));
                double rcsReserveBuckets = crewed ? 0.5 * effectiveVolume : 0.0;
                double pressure = (crewed ? 26.0 : 16.0) + diameter * 6.0;
                double thermal = (crewed ? 2500.0 : 1650.0) + diameter * 50.0;

                builder.dryWeight(dryMass)
                        .fuelBuckets(rcsReserveBuckets)
                        .pressureTolerance(round1(pressure))
                        .thermalTolerance(round1(thermal));
            }
            case ADAPTER -> {
                double dryMass = round1(110.0 * Math.pow(effectiveVolume, 0.68));
                double pressure = 15.0 + diameter * 4.0;
                double thermal = (idHint.contains("hotstaging") ? 2100.0 : 1600.0) + diameter * 45.0;

                builder.dryWeight(dryMass)
                        .pressureTolerance(round1(pressure))
                        .thermalTolerance(round1(thermal));
            }
            case PAYLOAD -> {
                double massFactor = config.walkableInterior() ? 185.0 : 145.0;
                double dryMass = round1(massFactor * Math.pow(effectiveVolume, 0.72));
                double pressure = (config.walkableInterior() ? 22.0 : 12.0) + diameter * 4.5;
                double thermal = 1350.0 + diameter * 35.0;

                builder.dryWeight(dryMass)
                        .pressureTolerance(round1(pressure))
                        .thermalTolerance(round1(thermal));
            }
            case UTILITY -> {
                double dryMass = config.radial()
                        ? round1(85.0 * config.heightBlocks())
                        : round1(130.0 * Math.pow(effectiveVolume, 0.68));
                double pressure = 14.0 + diameter * 4.0;
                double thermal = (idHint.contains("radiator") || idHint.contains("grid_fin") ? 1900.0 : 1500.0) + diameter * 40.0;

                builder.dryWeight(dryMass)
                        .pressureTolerance(round1(pressure))
                        .thermalTolerance(round1(thermal));
            }
        }

        return builder.build();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
