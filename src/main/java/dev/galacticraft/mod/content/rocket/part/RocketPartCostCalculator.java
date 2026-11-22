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

import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

/**
 * Deterministic parametric crafting cost and physical performance calculator for modular rocket parts.
 */
public final class RocketPartCostCalculator {
    public static final long BASE_ENERGY_PER_TICK = 120L;
    public static final int MIN_ASSEMBLY_TICKS = 160;
    public static final int MAX_ASSEMBLY_TICKS = 800;

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
     */
    public record CostProfile(
            int slotItemMultiplier,
            int assemblyTimeTicks,
            long energyPerTick,
            @NotNull RocketPartStats stats,
            int scrapCount
    ) {
        public CostProfile {
            slotItemMultiplier = Math.max(1, slotItemMultiplier);
            assemblyTimeTicks = Math.max(MIN_ASSEMBLY_TICKS, Math.min(MAX_ASSEMBLY_TICKS, assemblyTimeTicks));
            energyPerTick = Math.max(1L, energyPerTick);
            Objects.requireNonNull(stats, "stats");
            scrapCount = Math.max(1, scrapCount);
        }
    }

    /**
     * Calculates crafting cost scaling and deterministic physical attributes for {@code config}.
     */
    public static @NotNull CostProfile calculate(@NotNull RocketPartConfig config) {
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

        RocketPartStats stats = computeStats(config, diameter, effectiveVolume);
        return new CostProfile(slotItemMultiplier, assemblyTimeTicks, BASE_ENERGY_PER_TICK, stats, scrapCount);
    }

    /**
     * Calculates deterministic onboard electrical energy storage capacity (in GJ) contributed by
     * {@code config} when mounted on an assembled modular rocket.
     */
    public static long calculateEnergyCapacity(@NotNull RocketPartConfig config) {
        Objects.requireNonNull(config, "config");
        double effectiveVolume = computeEffectiveVolume(config);
        String idHint = (config.familyId() + "_" + config.modelId()).toLowerCase(Locale.ROOT);

        if (idHint.contains("power_battery_bank")) {
            return Math.round(45_000.0 * Math.pow(effectiveVolume, 0.75));
        }
        if (idHint.contains("power_fuel_cell")) {
            return Math.round(25_000.0 * Math.pow(effectiveVolume, 0.70));
        }
        if (idHint.contains("power_rtg")) {
            return Math.round(15_000.0 * Math.pow(effectiveVolume, 0.65));
        }
        if (idHint.contains("power_solar_array")) {
            return Math.round(10_000.0 * Math.pow(effectiveVolume, 0.60));
        }
        if (idHint.contains("avionics_ring")) {
            return Math.round(18_000.0 * Math.pow(effectiveVolume, 0.65));
        }
        if (idHint.contains("science_orbital_laboratory")) {
            return Math.round(15_000.0 * Math.pow(effectiveVolume, 0.65));
        }
        if (config.category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.COMMAND) {
            boolean crewed = config.walkableInterior() || idHint.contains("capsule") || idHint.contains("cockpit");
            return Math.round((crewed ? 20_000.0 : 12_000.0) * Math.pow(effectiveVolume, 0.65));
        }
        return 0L;
    }

    /**
     * Calculates passive electrical power generation rate (in GJ/tick) provided by solar arrays,
     * RTGs, or fuel cells mounted on an assembled modular rocket.
     */
    public static long calculateEnergyGenerationPerTick(@NotNull RocketPartConfig config) {
        Objects.requireNonNull(config, "config");
        double effectiveVolume = computeEffectiveVolume(config);
        String idHint = (config.familyId() + "_" + config.modelId()).toLowerCase(Locale.ROOT);

        if (idHint.contains("power_fuel_cell")) {
            return Math.round(60.0 * Math.pow(effectiveVolume, 0.65));
        }
        if (idHint.contains("power_solar_array")) {
            return Math.round(40.0 * Math.pow(effectiveVolume, 0.65));
        }
        if (idHint.contains("power_rtg")) {
            return Math.round(25.0 * Math.pow(effectiveVolume, 0.65));
        }
        return 0L;
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
        if (config.category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.COMMAND
                && (config.walkableInterior() || idHint.contains("capsule") || idHint.contains("cockpit"))) {
            return Math.round(4.0 * effectiveVolume) * 81_000L;
        }
        if (idHint.contains("cargo_bay_pressurized") || idHint.contains("science_orbital_laboratory")) {
            return Math.round(3.0 * effectiveVolume) * 81_000L;
        }
        return 0L;
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
}
