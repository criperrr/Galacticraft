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

package dev.galacticraft.mod.screen;

import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.menu.RecipeMachineMenu;
import dev.galacticraft.machinelib.api.menu.SynchronizedMenuType;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.block.entity.machine.*;
import dev.galacticraft.mod.recipe.CompressingRecipe;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.*;

public class GCMenuTypes {
    public static final MenuType<CoalGeneratorMenu> COAL_GENERATOR = SynchronizedMenuType.create(CoalGeneratorMenu::new);
    public static final MenuType<GeothermalGeneratorMenu> GEOTHERMAL_GENERATOR = SynchronizedMenuType.create(GeothermalGeneratorMenu::new);
    public static final MenuType<SolarPanelMenu<BasicSolarPanelBlockEntity>> BASIC_SOLAR_PANEL = SynchronizedMenuType.create(SolarPanelMenu::new);
    public static final MenuType<SolarPanelMenu<AdvancedSolarPanelBlockEntity>> ADVANCED_SOLAR_PANEL = SynchronizedMenuType.create(SolarPanelMenu::new);

    public static final MenuType<CircuitFabricatorMenu> CIRCUIT_FABRICATOR = SynchronizedMenuType.create(CircuitFabricatorMenu::new);
    public static final MenuType<CompressorMenu> COMPRESSOR = SynchronizedMenuType.create(CompressorMenu::new);

    public static final MenuType<RecipeMachineMenu<CraftingInput, CompressingRecipe, ElectricCompressorBlockEntity>> ELECTRIC_COMPRESSOR = SynchronizedMenuType.createSimple(RecipeMachineMenu::new);
    public static final MenuType<RecipeMachineMenu<SingleRecipeInput, SmeltingRecipe, ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE = SynchronizedMenuType.createSimple(RecipeMachineMenu::new);
    public static final MenuType<RecipeMachineMenu<SingleRecipeInput, BlastingRecipe, ElectricArcFurnaceBlockEntity>> ELECTRIC_ARC_FURNACE = SynchronizedMenuType.createSimple(RecipeMachineMenu::new);

    public static final MenuType<MachineMenu<RefineryBlockEntity>> REFINERY = SynchronizedMenuType.create(MachineMenu::new, 84);
    public static final MenuType<RegolithThermalDesorberMenu> REGOLITH_THERMAL_DESORBER = SynchronizedMenuType.create(RegolithThermalDesorberMenu::new);
    public static final MenuType<IsotopicCentrifugeMenu> ISOTOPIC_CENTRIFUGE = SynchronizedMenuType.create(IsotopicCentrifugeMenu::new);
    public static final MenuType<Helium3FusionReactorMenu> HELIUM3_FUSION_REACTOR = SynchronizedMenuType.create(Helium3FusionReactorMenu::new);
    public static final MenuType<VacuumMetallurgicInfuserMenu> VACUUM_METALLURGIC_INFUSER = SynchronizedMenuType.create(VacuumMetallurgicInfuserMenu::new);
    public static final MenuType<ChemicalLeachingPlantMenu> CHEMICAL_LEACHING_PLANT = SynchronizedMenuType.create(ChemicalLeachingPlantMenu::new);
    public static final MenuType<CompositeAutoclaveMenu> COMPOSITE_AUTOCLAVE = SynchronizedMenuType.create(CompositeAutoclaveMenu::new);
    public static final MenuType<ElectrolyticSeparatorMenu> ELECTROLYTIC_SEPARATOR = SynchronizedMenuType.create(ElectrolyticSeparatorMenu::new);
    public static final MenuType<AtmosphericFractionatorMenu> ATMOSPHERIC_FRACTIONATOR = SynchronizedMenuType.create(AtmosphericFractionatorMenu::new);
    public static final MenuType<SabatierChemicalReactorMenu> SABATIER_CHEMICAL_REACTOR = SynchronizedMenuType.create(SabatierChemicalReactorMenu::new);
    public static final MenuType<AeroRefurbishmentBenchMenu> AERO_REFURBISHMENT_BENCH = SynchronizedMenuType.create(AeroRefurbishmentBenchMenu::new);
    public static final MenuType<ResearchWorkbenchMenu> RESEARCH_WORKBENCH = SynchronizedMenuType.create(ResearchWorkbenchMenu::new);
    public static final MenuType<ObservatoryTelescopeMenu> OBSERVATORY_TELESCOPE = SynchronizedMenuType.create(ObservatoryTelescopeMenu::new);
    public static final MenuType<GeologicalAnalyzerMenu> GEOLOGICAL_ANALYZER = SynchronizedMenuType.create(GeologicalAnalyzerMenu::new);
    public static final MenuType<EnvironmentalTestChamberMenu> ENVIRONMENTAL_TEST_CHAMBER = SynchronizedMenuType.create(EnvironmentalTestChamberMenu::new);
    public static final MenuType<ZeroGResearchChamberMenu> ZERO_G_RESEARCH_CHAMBER = SynchronizedMenuType.create(ZeroGResearchChamberMenu::new);
    public static final MenuType<AerospaceRdTerminalMenu> AEROSPACE_RD_TERMINAL = SynchronizedMenuType.create(AerospaceRdTerminalMenu::new);
    public static final MenuType<ResearchDataLinkMenu> RESEARCH_DATA_LINK = SynchronizedMenuType.create(ResearchDataLinkMenu::new);

    public static final MenuType<OxygenCollectorMenu> OXYGEN_COLLECTOR = SynchronizedMenuType.create(OxygenCollectorMenu::new);
    public static final MenuType<OxygenDetectorMenu> OXYGEN_DETECTOR = SynchronizedMenuType.create(OxygenDetectorMenu::new);

    public static final MenuType<MachineMenu<OxygenCompressorBlockEntity>> OXYGEN_COMPRESSOR = SynchronizedMenuType.createSimple(MachineMenu::new);
    public static final MenuType<MachineMenu<OxygenDecompressorBlockEntity>> OXYGEN_DECOMPRESSOR = SynchronizedMenuType.createSimple(MachineMenu::new);
    public static final MenuType<OxygenSealerMenu> OXYGEN_SEALER = SynchronizedMenuType.create(OxygenSealerMenu::new);
    public static final MenuType<OxygenBubbleDistributorMenu> OXYGEN_BUBBLE_DISTRIBUTOR = SynchronizedMenuType.create(OxygenBubbleDistributorMenu::new);

    public static final MenuType<MachineMenu<OxygenStorageModuleBlockEntity>> OXYGEN_STORAGE_MODULE = SynchronizedMenuType.createSimple(MachineMenu::new);
    public static final MenuType<MachineMenu<EnergyStorageModuleBlockEntity>> ENERGY_STORAGE_MODULE = SynchronizedMenuType.createSimple(MachineMenu::new);

    public static final MenuType<FuelLoaderMenu> FUEL_LOADER = SynchronizedMenuType.create(FuelLoaderMenu::new);

    public static final MenuType<FoodCannerMenu> FOOD_CANNER = SynchronizedMenuType.create(FoodCannerMenu::new);

    public static final MenuType<GCPlayerInventoryMenu> PLAYER_INV_GC = new MenuType<>(GCPlayerInventoryMenu::new, FeatureFlags.VANILLA_SET);
    public static final MenuType<GCPetInventoryMenu> PET_INV_GC = new ExtendedScreenHandlerType<>(GCPetInventoryMenu::new, ByteBufCodecs.INT);

    public static final MenuType<AirlockControllerMenu> AIRLOCK_CONTROLLER_MENU = new MenuType<>(AirlockControllerMenu::new, FeatureFlags.VANILLA_SET);
    public static final MenuType<RocketWorkbenchMenu> ROCKET_WORKBENCH = new ExtendedScreenHandlerType<>(RocketWorkbenchMenu::new, RocketWorkbenchMenu.OpeningData.CODEC);
    public static final MenuType<HangarTerminalMenu> HANGAR_TERMINAL = new ExtendedScreenHandlerType<>(HangarTerminalMenu::new, HangarTerminalOpeningData.STREAM_CODEC);
    public static final MenuType<PartAssemblerMenu> PART_ASSEMBLER = SynchronizedMenuType.create(PartAssemblerMenu::new);
    public static final MenuType<RocketMenu> ROCKET = new ExtendedScreenHandlerType<>(RocketMenu::new, ByteBufCodecs.INT);
    public static final MenuType<ParachestMenu> PARACHEST = new ExtendedScreenHandlerType<>(ParachestMenu::new, ParachestMenu.OpeningData.STREAM_CODEC);

    public static void register() {
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.PLAYER_INVENTORY_MENU), PLAYER_INV_GC);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.PET_INVENTORY_MENU), PET_INV_GC);

        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.COAL_GENERATOR_MENU), COAL_GENERATOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.GEOTHERMAL_GENERATOR_MENU), GEOTHERMAL_GENERATOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.BASIC_SOLAR_PANEL_MENU), BASIC_SOLAR_PANEL);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ADVANCED_SOLAR_PANEL_MENU), ADVANCED_SOLAR_PANEL);

        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.CIRCUIT_FABRICATOR_MENU), CIRCUIT_FABRICATOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.COMPRESSOR_MENU), COMPRESSOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ELECTRIC_COMPRESSOR_MENU), ELECTRIC_COMPRESSOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ELECTRIC_FURNACE_MENU), ELECTRIC_FURNACE);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ELECTRIC_ARC_FURNACE_MENU), ELECTRIC_ARC_FURNACE);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.REFINERY_MENU), REFINERY);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.REGOLITH_THERMAL_DESORBER_MENU), REGOLITH_THERMAL_DESORBER);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ISOTOPIC_CENTRIFUGE_MENU), ISOTOPIC_CENTRIFUGE);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.HELIUM3_FUSION_REACTOR_MENU), HELIUM3_FUSION_REACTOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.VACUUM_METALLURGIC_INFUSER_MENU), VACUUM_METALLURGIC_INFUSER);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.CHEMICAL_LEACHING_PLANT_MENU), CHEMICAL_LEACHING_PLANT);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.COMPOSITE_AUTOCLAVE_MENU), COMPOSITE_AUTOCLAVE);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ELECTROLYTIC_SEPARATOR_MENU), ELECTROLYTIC_SEPARATOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ATMOSPHERIC_FRACTIONATOR_MENU), ATMOSPHERIC_FRACTIONATOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.SABATIER_CHEMICAL_REACTOR_MENU), SABATIER_CHEMICAL_REACTOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.AERO_REFURBISHMENT_BENCH_MENU), AERO_REFURBISHMENT_BENCH);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.RESEARCH_WORKBENCH_MENU), RESEARCH_WORKBENCH);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.OBSERVATORY_TELESCOPE_MENU), OBSERVATORY_TELESCOPE);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.GEOLOGICAL_ANALYZER_MENU), GEOLOGICAL_ANALYZER);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ENVIRONMENTAL_TEST_CHAMBER_MENU), ENVIRONMENTAL_TEST_CHAMBER);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ZERO_G_RESEARCH_CHAMBER_MENU), ZERO_G_RESEARCH_CHAMBER);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.AEROSPACE_RD_TERMINAL_MENU), AEROSPACE_RD_TERMINAL);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.RESEARCH_DATA_LINK_MENU), RESEARCH_DATA_LINK);

        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.OXYGEN_COLLECTOR_MENU), OXYGEN_COLLECTOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.OXYGEN_DETECTOR_MENU), OXYGEN_DETECTOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.OXYGEN_COMPRESSOR_MENU), OXYGEN_COMPRESSOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.OXYGEN_DECOMPRESSOR_MENU), OXYGEN_DECOMPRESSOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.BUBBLE_DISTRIBUTOR_MENU), OXYGEN_BUBBLE_DISTRIBUTOR);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.OXYGEN_SEALER_MENU), OXYGEN_SEALER);

        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ENERGY_STORAGE_MODULE_MENU), ENERGY_STORAGE_MODULE);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.OXYGEN_STORAGE_MODULE_MENU), OXYGEN_STORAGE_MODULE);

        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.FUEL_LOADER_MENU), FUEL_LOADER);

        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.FOOD_CANNER_MENU), FOOD_CANNER);

        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.AIR_LOCK_CONTROLLER_MENU), AIRLOCK_CONTROLLER_MENU);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ROCKET_WORKBENCH_MENU), ROCKET_WORKBENCH);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.HANGAR_TERMINAL_MENU), HANGAR_TERMINAL);
        Registry.register(BuiltInRegistries.MENU, Constant.id("part_assembler_menu"), PART_ASSEMBLER);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.ROCKET), ROCKET);
        Registry.register(BuiltInRegistries.MENU, Constant.id(Constant.Menu.PARACHEST), PARACHEST);
    }
}
