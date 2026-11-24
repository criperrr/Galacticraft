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

package dev.galacticraft.mod.machine;

import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.mod.util.Translations;
import net.minecraft.ChatFormatting;

public final class GCMachineStatuses {
    // Energy Generation
    public static final MachineStatus GENERATING = MachineStatus.create(Translations.MachineStatus.GENERATING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    // Energy Generation - Coal Generator
    public static final MachineStatus NO_FUEL = MachineStatus.create(Translations.MachineStatus.NO_FUEL, ChatFormatting.RED, MachineStatus.Type.MISSING_ITEMS);
    public static final MachineStatus WARMING_UP = MachineStatus.create(Translations.MachineStatus.WARMING_UP, ChatFormatting.GOLD, MachineStatus.Type.PARTIALLY_WORKING);
    public static final MachineStatus COOLING_DOWN = MachineStatus.create(Translations.MachineStatus.COOLING_DOWN, ChatFormatting.AQUA, MachineStatus.Type.PARTIALLY_WORKING);
    // Energy Generation - Solar Panels
    public static final MachineStatus PARTIALLY_GENERATING = MachineStatus.create(Translations.MachineStatus.PARTIALLY_GENERATING, ChatFormatting.YELLOW, MachineStatus.Type.PARTIALLY_WORKING);
    public static final MachineStatus NOT_GENERATING = MachineStatus.create(Translations.MachineStatus.NOT_GENERATING, ChatFormatting.DARK_GRAY, MachineStatus.Type.MISSING_RESOURCE);
    public static final MachineStatus BLOCKED = MachineStatus.create(Translations.MachineStatus.BLOCKED, ChatFormatting.DARK_RED, MachineStatus.Type.OTHER);

    public static final MachineStatus FABRICATING = MachineStatus.create(Translations.MachineStatus.FABRICATING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus COMPRESSING = MachineStatus.create(Translations.MachineStatus.COMPRESSING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus SMELTING = MachineStatus.create(Translations.MachineStatus.SMELTING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);

    // Oxygen
    public static final MachineStatus NOT_ENOUGH_OXYGEN = MachineStatus.create(Translations.MachineStatus.NOT_ENOUGH_OXYGEN, ChatFormatting.RED, MachineStatus.Type.MISSING_RESOURCE);
    // Oxygen - Collector
    public static final MachineStatus COLLECTING = MachineStatus.create(Translations.MachineStatus.COLLECTING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    // Oxygen - (De)compressor
    public static final MachineStatus COMPRESSING_OXYGEN = MachineStatus.create(Translations.MachineStatus.COMPRESSING_OXYGEN, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus DECOMPRESSING = MachineStatus.create(Translations.MachineStatus.DECOMPRESSING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus MISSING_OXYGEN_TANK = MachineStatus.create(Translations.MachineStatus.MISSING_OXYGEN_TANK, ChatFormatting.RED, MachineStatus.Type.MISSING_ITEMS);
    public static final MachineStatus OXYGEN_TANK_FULL = MachineStatus.create(Translations.MachineStatus.OXYGEN_TANK_FULL, ChatFormatting.GOLD, MachineStatus.Type.OTHER);
    public static final MachineStatus EMPTY_OXYGEN_TANK = MachineStatus.create(Translations.MachineStatus.EMPTY_OXYGEN_TANK, ChatFormatting.RED, MachineStatus.Type.MISSING_RESOURCE);
    // Oxygen - Sealer
    public static final MachineStatus ALREADY_SEALED = MachineStatus.create(Translations.MachineStatus.ALREADY_SEALED, ChatFormatting.RED, MachineStatus.Type.OTHER);
    public static final MachineStatus AREA_TOO_LARGE = MachineStatus.create(Translations.MachineStatus.AREA_TOO_LARGE, ChatFormatting.RED, MachineStatus.Type.PARTIALLY_WORKING);
    public static final MachineStatus SEALED = MachineStatus.create(Translations.MachineStatus.SEALED, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    // Oxygen - Bubble Distributor
    public static final MachineStatus DISTRIBUTING = MachineStatus.create(Translations.MachineStatus.DISTRIBUTING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    // Oxygen - Detector
    public static final MachineStatus OXYGEN_DETECTED = MachineStatus.create(Translations.MachineStatus.OXYGEN_DETECTED, ChatFormatting.AQUA, MachineStatus.Type.WORKING);
    public static final MachineStatus NO_OXYGEN_DETECTED = MachineStatus.create(Translations.MachineStatus.NO_OXYGEN_DETECTED, ChatFormatting.BLUE, MachineStatus.Type.MISSING_RESOURCE);

    // Refinery
    public static final MachineStatus REFINING = MachineStatus.create(Translations.MachineStatus.REFINING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus MISSING_OIL = MachineStatus.create(Translations.MachineStatus.MISSING_OIL, ChatFormatting.RED, MachineStatus.Type.MISSING_FLUIDS);
    public static final MachineStatus FUEL_TANK_FULL = MachineStatus.create(Translations.MachineStatus.FUEL_TANK_FULL, ChatFormatting.GOLD, MachineStatus.Type.OTHER);

    // Fuel Loader
    public static final MachineStatus PREPARING = MachineStatus.create(Translations.MachineStatus.PREPARING, ChatFormatting.YELLOW, MachineStatus.Type.OTHER);
    public static final MachineStatus LOADING = MachineStatus.create(Translations.MachineStatus.LOADING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus NOT_ENOUGH_FUEL = MachineStatus.create(Translations.MachineStatus.NOT_ENOUGH_FUEL, ChatFormatting.RED, MachineStatus.Type.MISSING_FLUIDS);
    public static final MachineStatus NO_ROCKET = MachineStatus.create(Translations.MachineStatus.NO_ROCKET, ChatFormatting.RED, MachineStatus.Type.MISSING_RESOURCE);
    public static final MachineStatus ROCKET_IS_FULL = MachineStatus.create(Translations.MachineStatus.ROCKET_IS_FULL, ChatFormatting.GOLD, MachineStatus.Type.OUTPUT_FULL);

    // Food Canner
    public static final MachineStatus CANNING = MachineStatus.create(Translations.MachineStatus.CANNING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus TRANSFERRING_CAN = MachineStatus.create(Translations.MachineStatus.TRANSFERRING_CAN, ChatFormatting.GREEN, MachineStatus.Type.PARTIALLY_WORKING);
    public static final MachineStatus NO_FOOD = MachineStatus.create(Translations.MachineStatus.NO_FOOD, ChatFormatting.RED, MachineStatus.Type.MISSING_ITEMS);
    public static final MachineStatus MISSING_EMPTY_CAN = MachineStatus.create(Translations.MachineStatus.MISSING_EMPTY_CAN, ChatFormatting.RED, MachineStatus.Type.MISSING_ITEMS);

    // Helium-3 Extraction & Aneutronic MHD Fusion
    public static final MachineStatus DESORBING = MachineStatus.create(Translations.MachineStatus.DESORBING, ChatFormatting.GOLD, MachineStatus.Type.WORKING);
    public static final MachineStatus SEPARATING = MachineStatus.create(Translations.MachineStatus.SEPARATING, ChatFormatting.AQUA, MachineStatus.Type.WORKING);
    public static final MachineStatus FUSING = MachineStatus.create(Translations.MachineStatus.FUSING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus FUSING_CRYO = MachineStatus.create(Translations.MachineStatus.FUSING_CRYO, ChatFormatting.AQUA, MachineStatus.Type.WORKING);
    public static final MachineStatus MISSING_COIL = MachineStatus.create(Translations.MachineStatus.MISSING_COIL, ChatFormatting.RED, MachineStatus.Type.MISSING_ITEMS);
    public static final MachineStatus MISSING_FUSION_FUEL = MachineStatus.create(Translations.MachineStatus.MISSING_FUSION_FUEL, ChatFormatting.RED, MachineStatus.Type.MISSING_FLUIDS);
    public static final MachineStatus GAS_TANK_FULL = MachineStatus.create(Translations.MachineStatus.GAS_TANK_FULL, ChatFormatting.GOLD, MachineStatus.Type.OUTPUT_FULL);
    public static final MachineStatus MAGNETIC_QUENCH_SAFE = MachineStatus.create(Translations.MachineStatus.MAGNETIC_QUENCH_SAFE, ChatFormatting.YELLOW, MachineStatus.Type.OTHER);
    public static final MachineStatus MULTIBLOCK_INCOMPLETE = MachineStatus.create(Translations.MachineStatus.MULTIBLOCK_INCOMPLETE, ChatFormatting.RED, MachineStatus.Type.OTHER);
    public static final MachineStatus SPINNING_UP = MachineStatus.create(Translations.MachineStatus.SPINNING_UP, ChatFormatting.YELLOW, MachineStatus.Type.PARTIALLY_WORKING);
    public static final MachineStatus IGNITING_PLASMA = MachineStatus.create(Translations.MachineStatus.IGNITING_PLASMA, ChatFormatting.GOLD, MachineStatus.Type.PARTIALLY_WORKING);
    public static final MachineStatus EVACUATING_CHAMBER = MachineStatus.create(Translations.MachineStatus.EVACUATING_CHAMBER, ChatFormatting.YELLOW, MachineStatus.Type.PARTIALLY_WORKING);

    // Sector 1: Metallurgy, Acid Leaching & Composite Autoclave
    public static final MachineStatus INFUSING = MachineStatus.create(Translations.MachineStatus.INFUSING, ChatFormatting.GOLD, MachineStatus.Type.WORKING);
    public static final MachineStatus MISSING_INFUSION_AGENT = MachineStatus.create(Translations.MachineStatus.MISSING_INFUSION_AGENT, ChatFormatting.RED, MachineStatus.Type.MISSING_ITEMS);
    public static final MachineStatus LEACHING = MachineStatus.create(Translations.MachineStatus.LEACHING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus MISSING_LEACHING_FLUID = MachineStatus.create(Translations.MachineStatus.MISSING_LEACHING_FLUID, ChatFormatting.RED, MachineStatus.Type.MISSING_FLUIDS);
    public static final MachineStatus PRESSURIZING = MachineStatus.create(Translations.MachineStatus.PRESSURIZING, ChatFormatting.YELLOW, MachineStatus.Type.PARTIALLY_WORKING);
    public static final MachineStatus CURING = MachineStatus.create(Translations.MachineStatus.CURING, ChatFormatting.GOLD, MachineStatus.Type.WORKING);

    // Sector 2: ISRU Chemistry & Liquid Propellants
    public static final MachineStatus ELECTROLYZING = MachineStatus.create(Translations.MachineStatus.ELECTROLYZING, ChatFormatting.AQUA, MachineStatus.Type.WORKING);
    public static final MachineStatus MISSING_FEEDSTOCK = MachineStatus.create(Translations.MachineStatus.MISSING_FEEDSTOCK, ChatFormatting.RED, MachineStatus.Type.MISSING_RESOURCE);
    public static final MachineStatus FRACTIONATING = MachineStatus.create(Translations.MachineStatus.FRACTIONATING, ChatFormatting.AQUA, MachineStatus.Type.WORKING);
    public static final MachineStatus NO_ATMOSPHERE = MachineStatus.create(Translations.MachineStatus.NO_ATMOSPHERE, ChatFormatting.DARK_RED, MachineStatus.Type.MISSING_RESOURCE);
    public static final MachineStatus HEATING_CATALYST = MachineStatus.create(Translations.MachineStatus.HEATING_CATALYST, ChatFormatting.GOLD, MachineStatus.Type.PARTIALLY_WORKING);
    public static final MachineStatus SYNTHESIZING = MachineStatus.create(Translations.MachineStatus.SYNTHESIZING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus MISSING_CATALYST = MachineStatus.create(Translations.MachineStatus.MISSING_CATALYST, ChatFormatting.RED, MachineStatus.Type.MISSING_ITEMS);

    // Planetary Laboratories & In-Situ Research
    public static final MachineStatus RESEARCHING = MachineStatus.create(Translations.MachineStatus.RESEARCHING, ChatFormatting.AQUA, MachineStatus.Type.WORKING);
    public static final MachineStatus MISSING_SPECIMEN = MachineStatus.create(Translations.MachineStatus.MISSING_SPECIMEN, ChatFormatting.RED, MachineStatus.Type.MISSING_ITEMS);
    public static final MachineStatus OBSERVING_SKY = MachineStatus.create(Translations.MachineStatus.OBSERVING_SKY, ChatFormatting.LIGHT_PURPLE, MachineStatus.Type.WORKING);
    public static final MachineStatus SKY_OBSTRUCTED = MachineStatus.create(Translations.MachineStatus.SKY_OBSTRUCTED, ChatFormatting.RED, MachineStatus.Type.OTHER);
    public static final MachineStatus WAITING_FOR_NIGHT = MachineStatus.create(Translations.MachineStatus.WAITING_FOR_NIGHT, ChatFormatting.YELLOW, MachineStatus.Type.OTHER);
    public static final MachineStatus ATMOSPHERE_TOO_DENSE = MachineStatus.create(Translations.MachineStatus.ATMOSPHERE_TOO_DENSE, ChatFormatting.DARK_RED, MachineStatus.Type.OTHER);
    public static final MachineStatus ANALYZING_SPECIMEN = MachineStatus.create(Translations.MachineStatus.ANALYZING_SPECIMEN, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus RUNNING_ENV_TEST = MachineStatus.create(Translations.MachineStatus.RUNNING_ENV_TEST, ChatFormatting.GOLD, MachineStatus.Type.WORKING);
    public static final MachineStatus MICROGRAVITY_SYNTHESIS = MachineStatus.create(Translations.MachineStatus.MICROGRAVITY_SYNTHESIS, ChatFormatting.AQUA, MachineStatus.Type.WORKING);
    public static final MachineStatus GRAVITY_TOO_HIGH = MachineStatus.create(Translations.MachineStatus.GRAVITY_TOO_HIGH, ChatFormatting.DARK_RED, MachineStatus.Type.OTHER);
    public static final MachineStatus UPLINK_ACTIVE = MachineStatus.create(Translations.MachineStatus.UPLINK_ACTIVE, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus MISSING_FREQUENCY_MODULE = MachineStatus.create(Translations.MachineStatus.MISSING_FREQUENCY_MODULE, ChatFormatting.RED, MachineStatus.Type.MISSING_ITEMS);

    // Sector 3: Aerospace Refurbishment & Protective Coating
    public static final MachineStatus REFURBISHING = MachineStatus.create(Translations.MachineStatus.REFURBISHING, ChatFormatting.GREEN, MachineStatus.Type.WORKING);
    public static final MachineStatus APPLYING_COATING = MachineStatus.create(Translations.MachineStatus.APPLYING_COATING, ChatFormatting.AQUA, MachineStatus.Type.WORKING);
    public static final MachineStatus PART_ALREADY_NOMINAL = MachineStatus.create(Translations.MachineStatus.PART_ALREADY_NOMINAL, ChatFormatting.YELLOW, MachineStatus.Type.OTHER);

    public static void register() {
    }
}
