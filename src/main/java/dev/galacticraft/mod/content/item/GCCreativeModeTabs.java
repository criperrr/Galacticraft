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

package dev.galacticraft.mod.content.item;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.api.rocket.RocketPrefabs;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.api.block.entity.PipeColor;
import dev.galacticraft.mod.content.GCBlockRegistry;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.util.Translations;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

import static dev.galacticraft.mod.content.GCBlocks.*;
import static dev.galacticraft.mod.content.item.GCItems.*;

public class GCCreativeModeTabs {
    public static final CreativeModeTab BLOCKS_GROUP = FabricItemGroup
            .builder()
            .icon(() -> new ItemStack(CHISELED_MOON_ROCK_BRICK))
            .title(Component.translatable(Translations.ItemGroup.BLOCKS))
            .displayItems((parameters, output) -> {
                // DECORATION BLOCKS
                for (GCBlockRegistry.DecorationSet decorationSet : BLOCKS.getDecorations()) {
                    output.accept(decorationSet.item());
                    output.accept(decorationSet.stairsItem());
                    output.accept(decorationSet.slabItem());
                    output.accept(decorationSet.wallItem());
                    output.accept(decorationSet.detailedItem());
                    output.accept(decorationSet.detailedStairsItem());
                    output.accept(decorationSet.detailedSlabItem());
                    output.accept(decorationSet.detailedWallItem());
                }

                // MOON NATURAL
                output.accept(MOON_TURF);
                output.accept(MOON_DIRT);
                output.accept(MOON_DIRT_PATH);
                output.accept(MOON_DUNGEON_BRICK);

                output.accept(MOON_SURFACE_ROCK);
                output.accept(MOON_SURFACE_ROCK_STAIRS);
                output.accept(MOON_SURFACE_ROCK_SLAB);
                output.accept(MOON_SURFACE_ROCK_WALL);

                output.accept(MOON_ROCK);
                output.accept(MOON_ROCK_STAIRS);
                output.accept(MOON_ROCK_SLAB);
                output.accept(MOON_ROCK_WALL);

                output.accept(MOON_ROCK_BRICK);
                output.accept(MOON_ROCK_BRICK_STAIRS);
                output.accept(MOON_ROCK_BRICK_SLAB);
                output.accept(MOON_ROCK_BRICK_WALL);

                output.accept(CRACKED_MOON_ROCK_BRICK);
                output.accept(CRACKED_MOON_ROCK_BRICK_STAIRS);
                output.accept(CRACKED_MOON_ROCK_BRICK_SLAB);
                output.accept(CRACKED_MOON_ROCK_BRICK_WALL);

                output.accept(POLISHED_MOON_ROCK);
                output.accept(POLISHED_MOON_ROCK_STAIRS);
                output.accept(POLISHED_MOON_ROCK_SLAB);
                output.accept(POLISHED_MOON_ROCK_WALL);

                output.accept(CHISELED_MOON_ROCK_BRICK);
                output.accept(MOON_ROCK_PILLAR);

                output.accept(COBBLED_MOON_ROCK);
                output.accept(COBBLED_MOON_ROCK_STAIRS);
                output.accept(COBBLED_MOON_ROCK_SLAB);
                output.accept(COBBLED_MOON_ROCK_WALL);

                output.accept(LUNASLATE);
                output.accept(LUNASLATE_STAIRS);
                output.accept(LUNASLATE_SLAB);
                output.accept(LUNASLATE_WALL);

                output.accept(COBBLED_LUNASLATE);
                output.accept(COBBLED_LUNASLATE_STAIRS);
                output.accept(COBBLED_LUNASLATE_SLAB);
                output.accept(COBBLED_LUNASLATE_WALL);

                output.accept(MOON_BASALT);
                output.accept(MOON_BASALT_STAIRS);
                output.accept(MOON_BASALT_SLAB);
                output.accept(MOON_BASALT_WALL);

                output.accept(MOON_BASALT_BRICK);
                output.accept(MOON_BASALT_BRICK_STAIRS);
                output.accept(MOON_BASALT_BRICK_SLAB);
                output.accept(MOON_BASALT_BRICK_WALL);

                output.accept(CRACKED_MOON_BASALT_BRICK);
                output.accept(CRACKED_MOON_BASALT_BRICK_STAIRS);
                output.accept(CRACKED_MOON_BASALT_BRICK_SLAB);
                output.accept(CRACKED_MOON_BASALT_BRICK_WALL);

                // MOON PLANTS
                output.accept(MOON_WEED);
                output.accept(MOON_SHRUBS);
                output.accept(MOON_MOSS);
                output.accept(MOON_TANGLE);

                // MARS NATURAL
                output.accept(MARS_SURFACE_ROCK);
                output.accept(MARS_SUB_SURFACE_ROCK);
                output.accept(MARS_STONE);
                output.accept(MARS_COBBLESTONE);
                output.accept(MARS_COBBLESTONE_STAIRS);
                output.accept(MARS_COBBLESTONE_SLAB);
                output.accept(MARS_COBBLESTONE_WALL);
                output.accept(MARS_BASALT);
                output.accept(MARS_SILTSTONE);
                output.accept(MARS_BRECCIA);
                output.accept(MARS_BASALTIC_SAND);
                output.accept(MARS_FROST_REGOLITH);
                output.accept(MARS_DEEP_STONE);

                // ASTEROID NATURAL
                output.accept(ASTEROID_ROCK);
                output.accept(ASTEROID_ROCK_1);
                output.accept(ASTEROID_ROCK_2);

                // VENUS NATURAL
                output.accept(SOFT_VENUS_ROCK);
                output.accept(HARD_VENUS_ROCK);
                output.accept(SCORCHED_VENUS_ROCK);
                output.accept(VOLCANIC_ROCK);
                output.accept(PUMICE);
                output.accept(VAPOR_SPOUT);

                // GAS & ICE GIANT HYPERBARIC CORE FORMATIONS
                output.accept(JUPITER_METALLIC_HYDROGEN_ROCK);
                output.accept(JUPITER_CORE_BRECCIA);
                output.accept(SATURN_HELIUM_MANTLE_ROCK);
                output.accept(URANUS_SUPERIONIC_MANTLE_ROCK);
                output.accept(URANUS_DIAMOND_BEARING_ROCK);
                output.accept(NEPTUNE_ABYSSAL_MANTLE_ROCK);

                // PHASE 4 OUTER MOONS
                output.accept(TITAN_THOLIN_SAND);
                output.accept(TITAN_HYDROCARBON_CRUST);
                output.accept(TITAN_WATER_ICE_ROCK);
                output.accept(TITAN_MAGNESIUM_ORE);
                output.accept(TITAN_IRON_ORE);
                output.accept(TITAN_SILICON_ORE);
                output.accept(ENCELADUS_ICE_CRUST);
                output.accept(ENCELADUS_TIGER_STRIPE_ICE);
                output.accept(ENCELADUS_SEABED_ROCK);
                output.accept(ENCELADUS_MAGNESIUM_ORE);
                output.accept(ENCELADUS_NIOBIUM_ORE);
                output.accept(TITANIA_ICE_SILICATE);
                output.accept(TITANIA_FROST_CRUST);
                output.accept(TITANIA_CHASM_SLATE);
                output.accept(TITANIA_URANIUM_ORE);
                output.accept(TITANIA_LONSDALEITE_ORE);
                output.accept(TITANIA_TITANIUM_ORE);
                output.accept(OBERON_DARK_ROCK);
                output.accept(OBERON_REDDISH_REGOLITH);
                output.accept(OBERON_IMPACT_BRECCIA);
                output.accept(OBERON_URANIUM_ORE);
                output.accept(OBERON_NIOBIUM_ORE);
                output.accept(TRITON_NITROGEN_ICE);
                output.accept(TRITON_CANTALOUPE_CRUST);
                output.accept(TRITON_CRYO_BASALT);
                output.accept(TRITON_SUPERIONIC_ORE);
                output.accept(TRITON_XENON_CLATHRATE_ORE);

                // ORES
                output.accept(SILICON_ORE);
                output.accept(DEEPSLATE_SILICON_ORE);
                output.accept(TIN_ORE);
                output.accept(DEEPSLATE_TIN_ORE);
                output.accept(ALUMINUM_ORE);
                output.accept(DEEPSLATE_ALUMINUM_ORE);
                output.accept(NIOBIUM_ORE);
                output.accept(DEEPSLATE_NIOBIUM_ORE);

                output.accept(MOON_COPPER_ORE);
                output.accept(LUNASLATE_COPPER_ORE);
                output.accept(MOON_TIN_ORE);
                output.accept(LUNASLATE_TIN_ORE);
                output.accept(MOON_ALUMINUM_ORE);
                output.accept(MOON_SILICON_ORE);
                output.accept(MOON_ILMENITE_ORE);
                output.accept(MOON_NIOBIUM_ORE);
                output.accept(MOON_GOLD_ORE);
                output.accept(MOON_REDSTONE_ORE);
                output.accept(MOON_IRON_ORE);
                output.accept(MOON_CHEESE_ORE);
                output.accept(LUNAR_SAPPHIRE_ORE);
                output.accept(OLIVINE_BASALT);
                output.accept(RICH_OLIVINE_BASALT);
                output.accept(FALLEN_METEOR);

                output.accept(MARS_IRON_ORE);
                output.accept(MARS_COPPER_ORE);
                output.accept(MARS_TIN_ORE);
                output.accept(MARS_SILICON_ORE);
                output.accept(MARS_ALUMINUM_ORE);
                output.accept(MARS_NIOBIUM_ORE);
                output.accept(MARS_GOLD_ORE);
                output.accept(MARS_REDSTONE_ORE);
                output.accept(MARS_TITANIUM_ORE);
                output.accept(MARS_GRAPHITE_ORE);
                output.accept(DESH_ORE);

                output.accept(ASTEROID_IRON_ORE);
                output.accept(ASTEROID_SILICON_ORE);
                output.accept(ASTEROID_ALUMINUM_ORE);
                output.accept(ASTEROID_NIOBIUM_ORE);
                output.accept(ASTEROID_GOLD_ORE);
                output.accept(ASTEROID_GRAPHITE_ORE);
                output.accept(ASTEROID_COPPER_ORE);
                output.accept(ASTEROID_REDSTONE_ORE);
                output.accept(ILMENITE_ORE);

                output.accept(MERCURY_ALUMINUM_ORE);
                output.accept(MERCURY_COPPER_ORE);
                output.accept(MERCURY_TITANIUM_ORE);
                output.accept(MERCURY_NATIVE_IRON_ORE);
                output.accept(MERCURY_GRAPHITE_ORE);
                output.accept(MERCURY_SULFIDE_ORE);
                output.accept(MERCURY_NIOBIUM_ORE);
                output.accept(MERCURY_GOLD_ORE);
                output.accept(MERCURY_LEAD_ORE);

                output.accept(VENUS_COPPER_ORE);
                output.accept(VENUS_TIN_ORE);
                output.accept(VENUS_ALUMINUM_ORE);
                output.accept(VENUS_IRON_ORE);
                output.accept(VENUS_NIOBIUM_ORE);
                output.accept(VENUS_GOLD_ORE);
                output.accept(VENUS_REDSTONE_ORE);
                output.accept(VENUS_SILICON_ORE);
                output.accept(VENUS_TITANIUM_ORE);
                output.accept(GALENA_ORE);
                output.accept(SOLAR_ORE);

                output.accept(JUPITER_PALLADIUM_ORE);
                output.accept(SATURN_MAGNESIUM_ORE);
                output.accept(URANUS_URANIUM_ORE);

                // COMPACT MINERAL BLOCKS
                output.accept(RAW_TIN_BLOCK);
                output.accept(RAW_ALUMINUM_BLOCK);
                output.accept(RAW_METEORIC_IRON_BLOCK);
                output.accept(RAW_DESH_BLOCK);
                output.accept(RAW_TITANIUM_BLOCK);
                output.accept(RAW_LEAD_BLOCK);
                output.accept(RAW_NIOBIUM_BLOCK);
                output.accept(RAW_PALLADIUM_BLOCK);
                output.accept(RAW_MAGNESIUM_BLOCK);
                output.accept(RAW_URANIUM_BLOCK);

                output.accept(SILICON_BLOCK);
                output.accept(LUNAR_SAPPHIRE_BLOCK);
                output.accept(OLIVINE_BLOCK);
                output.accept(LONSDALEITE_BLOCK);
                output.accept(BUDDING_OLIVINE);
                output.accept(OLIVINE_CLUSTER);

                output.accept(TIN_BLOCK);
                output.accept(ALUMINUM_BLOCK);
                output.accept(METEORIC_IRON_BLOCK);
                output.accept(DESH_BLOCK);
                output.accept(TITANIUM_BLOCK);
                output.accept(LEAD_BLOCK);
                output.accept(NIOBIUM_BLOCK);
                output.accept(FERRONIOBIUM_BLOCK);
                output.accept(PALLADIUM_BLOCK);
                output.accept(MAGNESIUM_BLOCK);
                output.accept(URANIUM_BLOCK);

                // CHEESE BLOCKS
                output.accept(MOON_CHEESE_BLOCK);
                output.accept(MOON_CHEESE_LOG);
                output.accept(MOON_CHEESE_LEAVES);

                // MOON VILLAGER SPECIAL
                output.accept(LUNAR_CARTOGRAPHY_TABLE);

                // TORCHES
                output.accept(GCItems.GLOWSTONE_TORCH);
                output.accept(GCItems.UNLIT_TORCH);
                output.accept(GCItems.UNLIT_SOUL_TORCH);

                // LANTERNS
                output.accept(GCItems.GLOWSTONE_LANTERN);
                output.accept(GCItems.UNLIT_LANTERN);
                output.accept(GCItems.UNLIT_SOUL_LANTERN);

                // FLAGS
                for (Item flag : GCItems.FLAGS.colorMap().values()) {
                    output.accept(flag);
                }

                // MISC DECOR
                output.accept(WALKWAY);
                output.accept(WIRE_WALKWAY);
                output.accept(HEAVY_WIRE_WALKWAY);
                output.accept(FLUID_PIPE_WALKWAY);
                output.accept(TIN_LADDER);
                output.accept(IRON_GRATING);
                output.accept(METEORIC_IRON_DOOR);

                // SPECIAL
                output.accept(ALUMINUM_WIRE);
                output.accept(HEAVY_ALUMINUM_WIRE);
                output.accept(SEALABLE_ALUMINUM_WIRE);
                output.accept(HEAVY_SEALABLE_ALUMINUM_WIRE);
                for (PipeColor color : PipeColor.byRainbowOrder()) {
                    output.accept(GLASS_FLUID_PIPES.get(color));
                }
                for (PipeColor color : PipeColor.byRainbowOrder()) {
                    output.accept(SEALABLE_FLUID_PIPES.get(color));
                }
                output.accept(ROCKET_LAUNCH_PAD);
                output.accept(REINFORCED_LAUNCH_PAD);
                output.accept(GANTRY_TRUSS);
                output.accept(GANTRY_CRANE_RAIL);
                output.accept(PLATFORM_UPGRADE_SOCKET);
                output.accept(HANGAR_TERMINAL);
                output.accept(FUELING_PAD);
                output.accept(SHORT_RANGE_TELEPAD);
                output.accept(ROCKET_WORKBENCH);

                // MISC MACHINES
                output.accept(CRYOGENIC_CHAMBER);
                output.accept(PLAYER_TRANSPORT_TUBE);

                for (DyeColor color : GCBlockRegistry.COLOR_ORDER) {
                    ItemStack stack = new ItemStack(PARACHEST);
                    stack.set(DataComponents.BASE_COLOR, color);
                    output.accept(stack);
                }

                // VACUUM GLASS
                output.accept(VACUUM_GLASS);
                output.accept(CLEAR_VACUUM_GLASS);
                output.accept(STRONG_VACUUM_GLASS);

                // MOON GLASSES
                output.accept(OLIVINE_GLASS);
                output.accept(OLIVINE_GLASS_PANE);
                output.accept(MOON_GLASS);
                output.accept(MOON_GLASS_PANE);

                // MISC WORLD GEN
                output.accept(CAVERNOUS_VINES);
            }).build();

    public static final CreativeModeTab MACHINES_GROUP = FabricItemGroup
            .builder()
            .icon(() -> new ItemStack(COAL_GENERATOR))
            .title(Component.translatable(Translations.ItemGroup.MACHINES))
            .displayItems((parameters, output) -> {
                output.accept(CIRCUIT_FABRICATOR);
                output.accept(COMPRESSOR);
                output.accept(ELECTRIC_COMPRESSOR);
                output.accept(COAL_GENERATOR);
                output.accept(BASIC_SOLAR_PANEL);
                output.accept(ADVANCED_SOLAR_PANEL);
                output.accept(ENERGY_STORAGE_MODULE);
                output.accept(ELECTRIC_FURNACE);
                output.accept(ELECTRIC_ARC_FURNACE);
                output.accept(REFINERY);
                output.accept(REGOLITH_THERMAL_DESORBER);
                output.accept(ISOTOPIC_CENTRIFUGE);
                output.accept(HELIUM3_FUSION_REACTOR);
                output.accept(VACUUM_METALLURGIC_INFUSER);
                output.accept(CHEMICAL_LEACHING_PLANT);
                output.accept(COMPOSITE_AUTOCLAVE);
                output.accept(ELECTROLYTIC_SEPARATOR);
                output.accept(ATMOSPHERIC_FRACTIONATOR);
                output.accept(SABATIER_CHEMICAL_REACTOR);
                output.accept(AERO_REFURBISHMENT_BENCH);
                output.accept(RESEARCH_WORKBENCH);
                output.accept(OBSERVATORY_TELESCOPE);
                output.accept(GEOLOGICAL_ANALYZER);
                output.accept(ENVIRONMENTAL_TEST_CHAMBER);
                output.accept(ZERO_G_RESEARCH_CHAMBER);
                output.accept(AEROSPACE_RD_TERMINAL);
                output.accept(RESEARCH_DATA_LINK);
                output.accept(TELEMETRY_ANALYZER);
                output.accept(FUSION_REACTOR_CASING);
                output.accept(FUSION_MAGNET_COIL);
                output.accept(FUSION_PLASMA_GLASS);
                output.accept(FUEL_LOADER);
                output.accept(OXYGEN_COLLECTOR);
                output.accept(OXYGEN_SEALER);
                output.accept(OXYGEN_BUBBLE_DISTRIBUTOR);
                output.accept(OXYGEN_DECOMPRESSOR);
                output.accept(OXYGEN_COMPRESSOR);
                output.accept(OXYGEN_DETECTOR);
                output.accept(OXYGEN_STORAGE_MODULE);
                output.accept(FOOD_CANNER);
                output.accept(AIR_LOCK_FRAME);
                output.accept(AIR_LOCK_CONTROLLER);
                output.accept(ROCKET_WORKBENCH);
                output.accept(HANGAR_TERMINAL);
            }).build();

    public static final CreativeModeTab ITEMS_GROUP = FabricItemGroup
            .builder()
            .icon(() -> new ItemStack(CANVAS))
            .title(Component.translatable(Translations.ItemGroup.ITEMS))
            .displayItems((parameters, output) -> {
                // BATTERIES
                output.accept(BATTERY);
                ItemStack chargedBattery = new ItemStack(BATTERY);
                BATTERY.setStoredEnergy(chargedBattery, BATTERY.getEnergyCapacity(chargedBattery));
                output.accept(chargedBattery);
                output.accept(INFINITE_BATTERY);

                // FLUID CANISTERS
                output.accept(FLUID_CANISTER);
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, GCFluids.CRUDE_OIL));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, GCFluids.FUEL));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, GCFluids.SULFURIC_ACID));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, GCFluids.LIQUID_OXYGEN));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, GCFluids.LIQUID_HYDROGEN));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, GCFluids.LIQUID_METHANE));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, GCFluids.MONOPROPELLANT));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, GCFluids.LIQUID_XENON));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, Gases.CARBON_DIOXIDE));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, Gases.NITROGEN));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, Gases.HYDROGEN));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, Gases.METHANE));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, Gases.OXYGEN));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, Gases.SOLAR_VOLATILE_GAS));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, Gases.HELIUM_3));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, Gases.DEUTERIUM));
                output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, Gases.XENON));
                //output.accept(FluidCanisterItem.getFilledCanister(FLUID_CANISTER, GCFluids.LIQUID_NITROGEN)); // Liquid nitrogen not added yet

                output.accept(SMALL_OXYGEN_TANK);
                output.accept(OxygenTankItem.getFullTank(SMALL_OXYGEN_TANK));
                output.accept(MEDIUM_OXYGEN_TANK);
                output.accept(OxygenTankItem.getFullTank(MEDIUM_OXYGEN_TANK));
                output.accept(LARGE_OXYGEN_TANK);
                output.accept(OxygenTankItem.getFullTank(LARGE_OXYGEN_TANK));
                output.accept(INFINITE_OXYGEN_TANK);

                // GEAR
                output.accept(OXYGEN_MASK);
                output.accept(OXYGEN_GEAR);
                output.accept(FREQUENCY_MODULE);
                output.accept(SHIELD_CONTROLLER);

                output.accept(PARACHUTE);
                for (DyeColor color : GCBlockRegistry.COLOR_ORDER) {
                    output.accept(DYED_PARACHUTES.get(color));
                }

                output.accept(EMERGENCY_KIT);

                // MATERIALS
                output.accept(RAW_TIN);
                output.accept(RAW_ALUMINUM);
                output.accept(RAW_METEORIC_IRON);
                output.accept(RAW_DESH);
                output.accept(RAW_TITANIUM);
                output.accept(RAW_LEAD);
                output.accept(RAW_NIOBIUM);
                output.accept(NIOBIUM_OXIDE);
                output.accept(SILICON);
                output.accept(LUNAR_SAPPHIRE);
                output.accept(OLIVINE_SHARD);

                output.accept(TIN_NUGGET);
                output.accept(ALUMINUM_NUGGET);
                output.accept(METEORIC_IRON_NUGGET);
                output.accept(DESH_NUGGET);
                output.accept(TITANIUM_NUGGET);
                output.accept(LEAD_NUGGET);
                output.accept(NIOBIUM_NUGGET);

                output.accept(TIN_INGOT);
                output.accept(ALUMINUM_INGOT);
                output.accept(METEORIC_IRON_INGOT);
                output.accept(DESH_INGOT);
                output.accept(TITANIUM_INGOT);
                output.accept(LEAD_INGOT);
                output.accept(FERRONIOBIUM_INGOT);
                output.accept(NIOBIUM_INGOT);

                output.accept(COMPRESSED_IRON);
                output.accept(COMPRESSED_COPPER);
                output.accept(COMPRESSED_TIN);
                output.accept(COMPRESSED_ALUMINUM);
                output.accept(COMPRESSED_STEEL);
                output.accept(COMPRESSED_BRONZE);
                output.accept(COMPRESSED_FERRONIOBIUM);
                output.accept(COMPRESSED_METEORIC_IRON);
                output.accept(COMPRESSED_DESH);
                output.accept(COMPRESSED_TITANIUM);
                output.accept(COMPRESSED_NIOBIUM);
                output.accept(SUPERCONDUCTING_COIL);
                output.accept(RAW_PALLADIUM);
                output.accept(PALLADIUM_INGOT);
                output.accept(COMPRESSED_PALLADIUM);
                output.accept(METALLIC_HYDROGEN_CELL);
                output.accept(RAW_MAGNESIUM);
                output.accept(MAGNESIUM_INGOT);
                output.accept(COMPRESSED_MAGNESIUM);
                output.accept(HELIUM3_CELL);
                output.accept(HELIUM3_FUSION_CELL);
                output.accept(LONSDALEITE_GEM);
                output.accept(RAW_URANIUM);
                output.accept(URANIUM_INGOT);
                output.accept(SUPERIONIC_ICE_CRYSTAL);

                output.accept(TIER_1_HEAVY_DUTY_PLATE);
                output.accept(TIER_2_HEAVY_DUTY_PLATE);
                output.accept(TIER_3_HEAVY_DUTY_PLATE);

                output.accept(COPPER_CANISTER);
                output.accept(TIN_CANISTER);
                output.accept(STEEL_POLE);
                output.accept(DESH_STICK);

                output.accept(BASIC_WAFER);
                output.accept(ADVANCED_WAFER);
                output.accept(BLUE_SOLAR_WAFER);
                output.accept(SINGLE_SOLAR_MODULE);
                output.accept(FULL_SOLAR_PANEL);
                output.accept(SOLAR_DUST);
                output.accept(SOLAR_ARRAY_WAFER);
                output.accept(SOLAR_ARRAY_PANEL);

                output.accept(OXYGEN_CONCENTRATOR);
                output.accept(OXYGEN_FAN);
                output.accept(OXYGEN_VENT);

                output.accept(CANVAS);
                output.accept(THERMAL_CLOTH);
                output.accept(ISOTHERMAL_FABRIC);
                output.accept(CARBON_FIBER_PLATE);
                output.accept(ABLATIVE_HEAT_TILE);
                output.accept(SOFI_INSULATION);

                output.accept(BEAM_CORE);
                output.accept(SENSOR_LENS);
                output.accept(CARBON_FRAGMENTS);
                output.accept(ATMOSPHERIC_VALVE);
                output.accept(FLUID_MANIPULATOR);
                output.accept(AMBIENT_THERMAL_CONTROLLER);
                output.accept(ORION_DRIVE);

                // BENCH-SPECIFIC LABORATORY UPGRADES
                output.accept(OPTICAL_ACHROMAT_LENS);
                output.accept(SAPPHIRE_CASSEGRAIN_MIRROR);
                output.accept(LONSDALEITE_PRIMARY_MIRROR);
                output.accept(EQUATORIAL_SIDEREAL_DRIVE);
                output.accept(ADAPTIVE_OPTICS_ACTUATOR);
                output.accept(INFRARED_BOLOMETER_ARRAY);
                output.accept(PRECISION_VERNIER_CALIPER);
                output.accept(OPTICAL_BLUEPRINT_PROJECTOR);
                output.accept(CAD_HOLOGRAPHIC_STYLUS);
                output.accept(TUNABLE_DIODE_LASER);
                output.accept(UV_EXCIMER_LASER);
                output.accept(DIFFRACTION_GRATING_PRISM);
                output.accept(QUADRUPOLE_MASS_FILTER);
                output.accept(TURBOMOLECULAR_VACUUM_PUMP);
                output.accept(HYPERBARIC_PISTON_MANIFOLD);
                output.accept(REGENERATIVE_THERMAL_EXCHANGER);
                output.accept(ABLATIVE_ACID_LINER);
                output.accept(ELECTROMAGNETIC_STABILIZER_RING);
                output.accept(SUPERCONDUCTING_CONFINEMENT_COIL);
                output.accept(GYROSCOPIC_DAMPENING_GIMBAL);
                output.accept(HELIUM_DILUTION_CRYOSTAT);
                output.accept(LOW_NOISE_RF_AMPLIFIER);
                output.accept(DEEP_SPACE_KA_BAND_FEED);
                output.accept(PHASE_LOCKED_LOOP_MODULE);
                output.accept(LASER_COMM_TRANSPONDER);
                output.accept(TELEMETRY_COPROCESSOR_CARD);
                output.accept(QUANTUM_ARCHIVE_CORE);
                output.accept(BLANK_DATA_DRIVE);
                output.accept(RECORDED_DATA_DRIVE);
                output.accept(BLANK_SCHEMATIC_DISC);
                output.accept(ORBITAL_SURVEY_MAP);

                // FOOD
                output.accept(MOON_CHEESE_CURD);
                output.accept(GCItems.MOON_CHEESE_WHEEL);
                output.accept(MOON_CHEESE_SLICE);
                output.accept(CRACKER);
                output.accept(CHEESE_CRACKER);
                output.accept(GROUND_BEEF);
                output.accept(BEEF_PATTY);
                output.accept(BURGER_BUN);
                output.accept(CHEESEBURGER);
                output.accept(MOON_TANGLE_FRUIT);

                output.accept(THROWABLE_METEOR_CHUNK);
                output.accept(HOT_THROWABLE_METEOR_CHUNK);

                // ROCKET PARTS
                output.accept(NOSE_CONE);
                output.accept(HEAVY_NOSE_CONE);
                output.accept(ROCKET_FIN);
                output.accept(HEAVY_ROCKET_FIN);
                output.accept(ROCKET_ENGINE);
                output.accept(HEAVY_ROCKET_ENGINE);
                output.accept(ROCKET_BOOSTER);

                // BUGGY PARTS
                output.accept(BUGGY_WHEEL);
                output.accept(BUGGY_SEAT);
                output.accept(BUGGY_STORAGE);

                // ROCKETS
                var tier1 = new ItemStack(ROCKET);
                tier1.set(GCDataComponents.ROCKET_DATA, RocketPrefabs.TIER_1);
                output.accept(tier1);

                var creativeRocket = new ItemStack(ROCKET);
                creativeRocket.set(GCDataComponents.CREATIVE, true);
                creativeRocket.set(GCDataComponents.ROCKET_DATA, RocketPrefabs.TIER_1);
                output.accept(creativeRocket);

                // SCHEMATICS
                output.accept(BASIC_ROCKET_CONE_SCHEMATIC);
                output.accept(BASIC_ROCKET_BODY_SCHEMATIC);
                output.accept(BASIC_ROCKET_FINS_SCHEMATIC);
                output.accept(BASIC_ROCKET_ENGINE_SCHEMATIC);
                output.accept(PART_SCHEMATIC_COMMAND);
                output.accept(PART_SCHEMATIC_ENGINE);
                output.accept(PART_SCHEMATIC_TANK);
                output.accept(PART_SCHEMATIC_ADAPTER);
                output.accept(PART_SCHEMATIC_PAYLOAD);
                output.accept(PART_SCHEMATIC_UTILITY);
                output.accept(TIER_2_ROCKET_SCHEMATIC);
                output.accept(CARGO_ROCKET_SCHEMATIC);
                output.accept(MOON_BUGGY_SCHEMATIC);
                output.accept(TIER_3_ROCKET_SCHEMATIC);
                output.accept(ASTRO_MINER_SCHEMATIC);

                // MODULAR ROCKET PARTS
                RocketPartCatalog partCatalog = RocketPartCatalog.getInstance();
                if (partCatalog != null) {
                    for (RocketPartCategory category : RocketPartCategory.values()) {
                        output.accept(ModularRocketPartBlockItem.createConfiguredStack(partCatalog.defaultConfigFor(category)));
                    }
                    addShowcaseRocketPart(output, partCatalog, "capsule_orion_5x5_3b", RocketLivery.WHITE);
                    addShowcaseRocketPart(output, partCatalog, "engine_raptor_3x3_2b", RocketLivery.BLUE);
                    addShowcaseRocketPart(output, partCatalog, "adapter_5x5_to_3x3_2b", RocketLivery.BLACK);
                    addShowcaseRocketPart(output, partCatalog, "cargo_bay_pressurized_5x5_3b", RocketLivery.WHITE);
                    addShowcaseRocketPart(output, partCatalog, "tank_hydrolox_supercryo_7x7_3b", RocketLivery.ORANGE);
                    addShowcaseRocketPart(output, partCatalog, "decoupler_radial_booster_1b", RocketLivery.OLIVE);
                }

                // SMITHING TEMPLATES
                output.accept(TITANTIUM_UPGRADE_SMITHING_TEMPLATE);

                // TOOLS + WEAPONS
                output.accept(HEAVY_DUTY_SWORD);
                output.accept(HEAVY_DUTY_SHOVEL);
                output.accept(HEAVY_DUTY_PICKAXE);
                output.accept(HEAVY_DUTY_AXE);
                output.accept(HEAVY_DUTY_HOE);

                output.accept(DESH_SWORD);
                output.accept(DESH_SHOVEL);
                output.accept(DESH_PICKAXE);
                output.accept(DESH_AXE);
                output.accept(DESH_HOE);

                output.accept(TITANIUM_SWORD);
                output.accept(TITANIUM_SHOVEL);
                output.accept(TITANIUM_PICKAXE);
                output.accept(TITANIUM_AXE);
                output.accept(TITANIUM_HOE);

                output.accept(STANDARD_WRENCH);
                output.accept(GRAPPLE_HOOK);
                output.accept(AEROSPACE_MANUAL);

                // ARMOR
                output.accept(HEAVY_DUTY_HELMET);
                output.accept(HEAVY_DUTY_CHESTPLATE);
                output.accept(HEAVY_DUTY_LEGGINGS);
                output.accept(HEAVY_DUTY_BOOTS);

                output.accept(DESH_HELMET);
                output.accept(DESH_CHESTPLATE);
                output.accept(DESH_LEGGINGS);
                output.accept(DESH_BOOTS);

                output.accept(TITANIUM_HELMET);
                output.accept(TITANIUM_CHESTPLATE);
                output.accept(TITANIUM_LEGGINGS);
                output.accept(TITANIUM_BOOTS);

                output.accept(SENSOR_GLASSES);

                // THERMAL PADDING
                output.accept(THERMAL_PADDING_HELMET);
                output.accept(THERMAL_PADDING_CHESTPIECE);
                output.accept(THERMAL_PADDING_LEGGINGS);
                output.accept(THERMAL_PADDING_BOOTS);

                output.accept(ISOTHERMAL_PADDING_HELMET);
                output.accept(ISOTHERMAL_PADDING_CHESTPIECE);
                output.accept(ISOTHERMAL_PADDING_LEGGINGS);
                output.accept(ISOTHERMAL_PADDING_BOOTS);

                // LEGACY MUSIC DISCS
                output.accept(LEGACY_MUSIC_DISC_MARS);
                output.accept(LEGACY_MUSIC_DISC_MIMAS);
                output.accept(LEGACY_MUSIC_DISC_ORBIT);
                output.accept(LEGACY_MUSIC_DISC_SPACERACE);

                // FLUID BUCKETS
                output.accept(CRUDE_OIL_BUCKET);
                output.accept(FUEL_BUCKET);
                output.accept(SULFURIC_ACID_BUCKET);
                output.accept(LIQUID_OXYGEN_BUCKET);
                output.accept(LIQUID_HYDROGEN_BUCKET);
                output.accept(LIQUID_METHANE_BUCKET);
                output.accept(MONOPROPELLANT_BUCKET);
                output.accept(LIQUID_XENON_BUCKET);
            })
            .build();

    public static final CreativeModeTab CANNED_FOOD_GROUP = FabricItemGroup
            .builder()
            .icon(() -> new ItemStack(GCItems.CANNED_FOOD))
            .title(Component.translatable(Translations.ItemGroup.CANNED_FOOD))
            .displayItems((parameters, output) -> {
                output.accept(EMPTY_CAN);
                CannedFoodItem.getDefaultCannedFoods().forEach(cannedFoodItem -> output.accept(cannedFoodItem));
            }).build();

    private static void addShowcaseRocketPart(CreativeModeTab.Output output, RocketPartCatalog catalog, String modelId, RocketLivery livery) {
        RocketPartSpec spec = catalog.getPart(modelId);
        if (spec != null) {
            output.accept(ModularRocketPartBlockItem.createConfiguredStack(spec.toConfig(livery)));
        }
    }

    public static void registerSpawnEggs() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(content -> {
            content.addAfter(ItemStack.EMPTY, MOON_VILLAGER_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, EVOLVED_ZOMBIE_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, EVOLVED_CREEPER_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, EVOLVED_SKELETON_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, EVOLVED_SPIDER_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, EVOLVED_ENDERMAN_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, EVOLVED_WITCH_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, EVOLVED_PILLAGER_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, EVOLVED_EVOKER_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, EVOLVED_VINDICATOR_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, GAZER_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, RUMBLER_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, COMET_CUBE_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, OLI_GRUB_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, GREY_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, ARCH_GREY_SPAWN_EGG);
            content.addAfter(ItemStack.EMPTY, SLIMELING_SPAWN_EGG);
        });
    }

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Constant.id("1." + Constant.Block.ITEM_GROUP_BLOCKS), BLOCKS_GROUP);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Constant.id("2." + Constant.Block.ITEM_GROUP_MACHINES), MACHINES_GROUP);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Constant.id("3." + Constant.Item.ITEM_GROUP), ITEMS_GROUP);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Constant.id("4." + Constant.Item.ITEM_GROUP_CANS), CANNED_FOOD_GROUP);
        registerSpawnEggs();
    }
}