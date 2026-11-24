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

package dev.galacticraft.mod.data.recipes;

import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.api.block.entity.PipeColor;
import dev.galacticraft.mod.api.data.recipe.CentrifugeRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.ChemicalLeachingRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.CircuitFabricatorRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.CompositeAutoclaveRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.ElectrolyticRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.FusionReactorRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.GCShapedRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.GCShapelessRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.MetallurgicInfusingRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.SabatierRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.ThermalDesorptionRecipeBuilder;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.machine.VacuumMetallurgicInfuserBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.tag.GCItemTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class GCMachineRecipes extends FabricRecipeProvider {
    public GCMachineRecipes(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        // Machine Blocks
        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ADVANCED_SOLAR_PANEL)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('F', GCItems.SOLAR_ARRAY_PANEL)
                .define('P', GCItems.STEEL_POLE)
                .define('W', GCBlocks.HEAVY_SEALABLE_ALUMINUM_WIRE)
                .define('A', GCItems.ADVANCED_WAFER)
                .pattern("SFS")
                .pattern("SPS")
                .pattern("WAW")
                .unlockedBy(getHasName(GCItems.SOLAR_ARRAY_PANEL), has(GCItems.SOLAR_ARRAY_PANEL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.BASIC_SOLAR_PANEL)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('F', GCItems.FULL_SOLAR_PANEL)
                .define('P', GCItems.STEEL_POLE)
                .define('W', GCBlocks.ALUMINUM_WIRE)
                .define('B', GCItems.BASIC_WAFER)
                .pattern("SFS")
                .pattern("SPS")
                .pattern("WBW")
                .unlockedBy(getHasName(GCItems.BASIC_WAFER), has(GCItems.BASIC_WAFER))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.GEOTHERMAL_GENERATOR)
                .define('B', GCItems.COMPRESSED_BRONZE)
                .define('V', GCItems.ATMOSPHERIC_VALVE)
                .define('W', GCBlocks.ALUMINUM_WIRE)
                .define('S', GCBlocks.BASIC_SOLAR_PANEL)
                .define('F', GCItems.ISOTHERMAL_FABRIC)
                .pattern("BVB")
                .pattern("WSW")
                .pattern("BFB")
                .unlockedBy(getHasName(GCItems.ISOTHERMAL_FABRIC), has(GCItems.ISOTHERMAL_FABRIC))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.CIRCUIT_FABRICATOR)
                .define('A', GCItemTags.ALUMINUM_INGOTS)
                .define('L', Items.LEVER)
                .define('B', ItemTags.STONE_BUTTONS)
                .define('F', ConventionalItemTags.PLAYER_WORKSTATIONS_FURNACES)
                .define('W', GCBlocks.ALUMINUM_WIRE)
                .define('R', Items.REDSTONE_TORCH)
                .pattern("ALA")
                .pattern("BFB")
                .pattern("WRW")
                .unlockedBy(getHasName(GCItems.ALUMINUM_INGOT), has(GCItemTags.ALUMINUM_INGOTS))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.COAL_GENERATOR)
                .define('C', ConventionalItemTags.COPPER_INGOTS)
                .define('A', GCItemTags.ALUMINUM_INGOTS)
                .define('F', ConventionalItemTags.PLAYER_WORKSTATIONS_FURNACES)
                .define('W', GCBlocks.ALUMINUM_WIRE)
                .pattern("CCC")
                .pattern("AFA")
                .pattern("AWA")
                .unlockedBy(getHasName(GCItems.ALUMINUM_INGOT), has(GCItemTags.ALUMINUM_INGOTS))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.COMPRESSOR)
                .define('I', GCItemTags.ALUMINUM_INGOTS)
                .define('A', Items.ANVIL)
                .define('C', ConventionalItemTags.COPPER_INGOTS)
                .define('W', GCItems.BASIC_WAFER)
                .pattern("IAI")
                .pattern("ICI")
                .pattern("IWI")
                .unlockedBy(getHasName(GCItems.ALUMINUM_INGOT), has(GCItemTags.ALUMINUM_INGOTS))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ELECTRIC_ARC_FURNACE)
                .define('H', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .define('C', GCItems.SUPERCONDUCTING_COIL)
                .define('E', GCBlocks.ELECTRIC_FURNACE)
                .define('M', GCItems.METEORIC_IRON_INGOT)
                .define('W', GCItems.ADVANCED_WAFER)
                .pattern("HCH")
                .pattern("HEH")
                .pattern("MWM")
                .unlockedBy(getHasName(GCItems.SUPERCONDUCTING_COIL), has(GCItems.SUPERCONDUCTING_COIL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ELECTRIC_COMPRESSOR)
                .define('F', GCItems.COMPRESSED_FERRONIOBIUM)
                .define('A', Items.ANVIL)
                .define('B', GCItems.COMPRESSED_BRONZE)
                .define('W', GCItems.ADVANCED_WAFER)
                .define('I', GCBlocks.ALUMINUM_WIRE)
                .pattern("FAF")
                .pattern("FBF")
                .pattern("IWI")
                .unlockedBy(getHasName(GCItems.COMPRESSED_FERRONIOBIUM), has(GCItems.COMPRESSED_FERRONIOBIUM))
                .save(output);
        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ELECTRIC_COMPRESSOR)
                .define('F', GCItems.COMPRESSED_FERRONIOBIUM)
                .define('T', GCItems.COMPRESSED_TIN)
                .define('C', GCBlocks.COMPRESSOR)
                .define('W', GCItems.ADVANCED_WAFER)
                .define('I', GCBlocks.ALUMINUM_WIRE)
                .pattern("FTF")
                .pattern("FCF")
                .pattern("IWI")
                .unlockedBy(getHasName(GCItems.COMPRESSED_FERRONIOBIUM), has(GCItems.COMPRESSED_FERRONIOBIUM))
                .save(output, RecipeBuilder.getDefaultRecipeId(GCBlocks.ELECTRIC_COMPRESSOR).withSuffix("_upgrade"));

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ELECTRIC_FURNACE)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('F', ConventionalItemTags.PLAYER_WORKSTATIONS_FURNACES)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .define('W', GCItems.BASIC_WAFER)
                .pattern("SSS")
                .pattern("SFS")
                .pattern("AWA")
                .unlockedBy(getHasName(GCItems.BASIC_WAFER), has(GCItems.BASIC_WAFER))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ENERGY_STORAGE_MODULE)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('B', GCItems.BATTERY)
                .define('C', GCItems.SUPERCONDUCTING_COIL)
                .pattern("SSS")
                .pattern("BCB")
                .pattern("SSS")
                .unlockedBy(getHasName(GCItems.SUPERCONDUCTING_COIL), has(GCItems.SUPERCONDUCTING_COIL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.OXYGEN_BUBBLE_DISTRIBUTOR)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('F', GCItems.OXYGEN_FAN)
                .define('V', GCItems.OXYGEN_VENT)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .pattern("SFS")
                .pattern("VAV")
                .pattern("SFS")
                .unlockedBy(getHasName(GCItems.OXYGEN_FAN), has(GCItems.OXYGEN_FAN))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.OXYGEN_COLLECTOR)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('F', GCItems.OXYGEN_FAN)
                .define('T', GCItems.TIN_CANISTER)
                .define('V', GCItems.OXYGEN_VENT)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .define('C', GCItems.OXYGEN_CONCENTRATOR)
                .pattern("SSS")
                .pattern("FTV")
                .pattern("ACA")
                .unlockedBy(getHasName(GCItems.OXYGEN_CONCENTRATOR), has(GCItems.OXYGEN_CONCENTRATOR))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.OXYGEN_DETECTOR)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('W', GCItems.BASIC_WAFER)
                .define('V', GCItems.OXYGEN_VENT)
                .define('R', Items.REDSTONE)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .pattern("SSS")
                .pattern("VWV")
                .pattern("RAR")
                .unlockedBy(getHasName(GCItems.OXYGEN_CONCENTRATOR), has(GCItems.OXYGEN_CONCENTRATOR))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.OXYGEN_COMPRESSOR)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('B', GCItems.COMPRESSED_BRONZE)
                .define('C', GCItems.OXYGEN_CONCENTRATOR)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .pattern("SAS")
                .pattern("ACA")
                .pattern("SBS")
                .unlockedBy(getHasName(GCItems.OXYGEN_CONCENTRATOR), has(GCItems.OXYGEN_CONCENTRATOR))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.OXYGEN_DECOMPRESSOR)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('R', Items.REDSTONE_TORCH)
                .define('C', GCItems.OXYGEN_CONCENTRATOR)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .define('F', GCItems.OXYGEN_FAN)
                .pattern("SFS")
                .pattern("ACA")
                .pattern("SRS")
                .unlockedBy(getHasName(GCItems.OXYGEN_CONCENTRATOR), has(GCItems.OXYGEN_CONCENTRATOR))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.FOOD_CANNER)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('P', Items.PISTON)
                .define('C', GCItems.EMPTY_CAN)
                .define('B', GCItems.BASIC_WAFER)
                .define('A', GCBlocks.ALUMINUM_WIRE)
                .pattern("SPS")
                .pattern("SCS")
                .pattern("ABA")
                .unlockedBy(getHasName(GCItems.EMPTY_CAN), has(GCItems.EMPTY_CAN))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.OXYGEN_SEALER)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('F', GCItems.OXYGEN_FAN)
                .define('V', GCItems.OXYGEN_VENT)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .pattern("ASA")
                .pattern("VFV")
                .pattern("ASA")
                .unlockedBy(getHasName(GCItems.OXYGEN_FAN), has(GCItems.OXYGEN_FAN))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.OXYGEN_STORAGE_MODULE)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('T', GCItems.LARGE_OXYGEN_TANK)
                .pattern("SSS")
                .pattern("TTT")
                .pattern("SSS")
                .unlockedBy(getHasName(GCItems.LARGE_OXYGEN_TANK), has(GCItems.LARGE_OXYGEN_TANK))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.REFINERY)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('C', GCItems.COPPER_CANISTER)
                .define('F', Items.BLAST_FURNACE)
                .pattern("SCS")
                .pattern("SCS")
                .pattern("SFS")
                .unlockedBy(getHasName(GCItems.CRUDE_OIL_BUCKET), has(GCItems.CRUDE_OIL_BUCKET))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.REGOLITH_THERMAL_DESORBER)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .define('F', GCBlocks.ELECTRIC_FURNACE)
                .define('M', GCItems.FLUID_MANIPULATOR)
                .define('W', GCItems.ADVANCED_WAFER)
                .pattern("TMT")
                .pattern("TFT")
                .pattern("TWT")
                .unlockedBy(getHasName(GCItems.COMPRESSED_TITANIUM), has(GCItems.COMPRESSED_TITANIUM))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ISOTOPIC_CENTRIFUGE)
                .define('N', GCItems.COMPRESSED_NIOBIUM)
                .define('C', GCItems.SUPERCONDUCTING_COIL)
                .define('M', GCItems.FLUID_MANIPULATOR)
                .define('W', GCItems.ADVANCED_WAFER)
                .pattern("NMN")
                .pattern("NCN")
                .pattern("NWN")
                .unlockedBy(getHasName(GCItems.SUPERCONDUCTING_COIL), has(GCItems.SUPERCONDUCTING_COIL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.HELIUM3_FUSION_REACTOR)
                .define('N', GCItems.COMPRESSED_NIOBIUM)
                .define('C', GCItems.SUPERCONDUCTING_COIL)
                .define('H', GCItems.HELIUM3_CELL)
                .define('S', GCItems.LUNAR_SAPPHIRE)
                .define('W', GCItems.ADVANCED_WAFER)
                .pattern("NCN")
                .pattern("SHS")
                .pattern("NWN")
                .unlockedBy(getHasName(GCItems.HELIUM3_CELL), has(GCItems.HELIUM3_CELL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.FUSION_REACTOR_CASING, 4)
                .define('N', GCItems.COMPRESSED_NIOBIUM)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .define('P', GCItems.TIER_2_HEAVY_DUTY_PLATE)
                .pattern("NTN")
                .pattern("TPT")
                .pattern("NTN")
                .unlockedBy(getHasName(GCItems.COMPRESSED_NIOBIUM), has(GCItems.COMPRESSED_NIOBIUM))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.FUSION_MAGNET_COIL, 2)
                .define('N', GCItems.COMPRESSED_NIOBIUM)
                .define('C', GCItems.SUPERCONDUCTING_COIL)
                .define('K', GCItems.COMPRESSED_COPPER)
                .pattern("NKN")
                .pattern("KCK")
                .pattern("NKN")
                .unlockedBy(getHasName(GCItems.SUPERCONDUCTING_COIL), has(GCItems.SUPERCONDUCTING_COIL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.FUSION_PLASMA_GLASS, 4)
                .define('G', GCBlocks.STRONG_VACUUM_GLASS)
                .define('S', GCItems.LUNAR_SAPPHIRE)
                .define('N', GCItems.COMPRESSED_NIOBIUM)
                .pattern("NGN")
                .pattern("GSG")
                .pattern("NGN")
                .unlockedBy(getHasName(GCItems.LUNAR_SAPPHIRE), has(GCItems.LUNAR_SAPPHIRE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.VACUUM_METALLURGIC_INFUSER)
                .define('F', GCItems.COMPRESSED_FERRONIOBIUM)
                .define('C', GCItems.TIN_CANISTER)
                .define('U', ConventionalItemTags.PLAYER_WORKSTATIONS_FURNACES)
                .define('W', GCItems.BASIC_WAFER)
                .pattern("FCF")
                .pattern("FUF")
                .pattern("FWF")
                .unlockedBy(getHasName(GCItems.COMPRESSED_FERRONIOBIUM), has(GCItems.COMPRESSED_FERRONIOBIUM))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.CHEMICAL_LEACHING_PLANT)
                .define('L', GCItemTags.LEAD_INGOTS)
                .define('C', GCItems.COPPER_CANISTER)
                .define('P', GCBlocks.GLASS_FLUID_PIPE)
                .define('M', GCItems.FLUID_MANIPULATOR)
                .pattern("LCL")
                .pattern("PMP")
                .pattern("LPL")
                .unlockedBy(getHasName(GCItems.LEAD_INGOT), has(GCItemTags.LEAD_INGOTS))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.COMPOSITE_AUTOCLAVE)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('V', GCItems.ATMOSPHERIC_VALVE)
                .define('D', GCItems.COMPRESSED_DESH)
                .define('T', GCItems.AMBIENT_THERMAL_CONTROLLER)
                .pattern("SVS")
                .pattern("DTD")
                .pattern("SDS")
                .unlockedBy(getHasName(GCItems.COMPRESSED_DESH), has(GCItems.COMPRESSED_DESH))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ELECTROLYTIC_SEPARATOR)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('C', GCItems.SUPERCONDUCTING_COIL)
                .define('M', GCItems.FLUID_MANIPULATOR)
                .define('P', GCBlocks.GLASS_FLUID_PIPE)
                .define('W', GCItems.ADVANCED_WAFER)
                .pattern("SPS")
                .pattern("CMC")
                .pattern("SWS")
                .unlockedBy(getHasName(GCItems.FLUID_MANIPULATOR), has(GCItems.FLUID_MANIPULATOR))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ATMOSPHERIC_FRACTIONATOR)
                .define('D', GCItems.COMPRESSED_DESH)
                .define('V', GCItems.ATMOSPHERIC_VALVE)
                .define('F', GCItems.OXYGEN_FAN)
                .define('O', GCItems.OXYGEN_CONCENTRATOR)
                .define('W', GCItems.ADVANCED_WAFER)
                .pattern("DVD")
                .pattern("FOF")
                .pattern("DWD")
                .unlockedBy(getHasName(GCItems.ATMOSPHERIC_VALVE), has(GCItems.ATMOSPHERIC_VALVE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.SABATIER_CHEMICAL_REACTOR)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .define('N', GCItems.COMPRESSED_NIOBIUM)
                .define('M', GCItems.FLUID_MANIPULATOR)
                .define('H', GCItems.AMBIENT_THERMAL_CONTROLLER)
                .define('W', GCItems.ADVANCED_WAFER)
                .pattern("TMT")
                .pattern("NHN")
                .pattern("TWT")
                .unlockedBy(getHasName(GCItems.COMPRESSED_NIOBIUM), has(GCItems.COMPRESSED_NIOBIUM))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.AERO_REFURBISHMENT_BENCH)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .define('W', GCItems.STANDARD_WRENCH)
                .define('R', GCBlocks.ROCKET_WORKBENCH)
                .define('B', GCItems.BEAM_CORE)
                .define('C', GCItems.ADVANCED_WAFER)
                .pattern("TWT")
                .pattern("BRB")
                .pattern("TCT")
                .unlockedBy(getHasName(GCBlocks.ROCKET_WORKBENCH), has(GCBlocks.ROCKET_WORKBENCH))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.FUEL_LOADER)
                .define('U', GCItems.COMPRESSED_COPPER)
                .define('C', GCItems.TIN_CANISTER)
                .define('T', GCItems.COMPRESSED_TIN)
                .define('W', GCItems.BASIC_WAFER)
                .pattern("UUU")
                .pattern("UCU")
                .pattern("TWT")
                .unlockedBy(getHasName(GCItems.FUEL_BUCKET), has(GCItems.FUEL_BUCKET))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.AIR_LOCK_FRAME, 4)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('C', GCItems.OXYGEN_CONCENTRATOR)
                .pattern("AAA")
                .pattern("SCS")
                .pattern("AAA")
                .unlockedBy(getHasName(GCItems.OXYGEN_CONCENTRATOR), has(GCItems.OXYGEN_CONCENTRATOR))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.AIR_LOCK_CONTROLLER)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('M', GCItems.COMPRESSED_METEORIC_IRON)
                .define('W', GCItems.BASIC_WAFER)
                .pattern("SSS")
                .pattern("MWM")
                .pattern("SSS")
                .unlockedBy(getHasName(GCItems.COMPRESSED_METEORIC_IRON), has(GCItems.COMPRESSED_METEORIC_IRON))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.CRYOGENIC_CHAMBER)
                .define('D', GCItems.COMPRESSED_DESH)
                .define('P', GCItems.TIER_2_HEAVY_DUTY_PLATE)
                .define('T', Items.TINTED_GLASS)
                .define('B', ItemTags.BEDS)
                .pattern("DTT")
                .pattern("DBT")
                .pattern("DPT")
                .unlockedBy(getHasName(GCItems.TIER_2_HEAVY_DUTY_PLATE), has(GCItems.TIER_2_HEAVY_DUTY_PLATE))
                .emiDefaultRecipe(true)
                .save(output);

        // Pipes from panes
        for (PipeColor color : PipeColor.values()) {
            GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.GLASS_FLUID_PIPES.get(color), 6)
                    .define('X', color.glassPane())
                    .pattern("XXX")
                    .pattern("   ")
                    .pattern("XXX")
                    .unlockedBy(getHasName(Items.GLASS_PANE), has(Items.GLASS_PANE))
                    .emiDefaultRecipe(color.equals(PipeColor.CLEAR))
                    .save(output);
        }

        // Dye fluid pipes
        for (DyeColor dye : DyeColor.values()) {
            PipeColor color = PipeColor.fromDye(dye);
            ItemLike pipe = GCBlocks.GLASS_FLUID_PIPES.get(color);
            GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, pipe, 8)
                    .define('P', GCBlocks.GLASS_FLUID_PIPE)
                    .define('D', color.dye())
                    .pattern("PPP")
                    .pattern("PDP")
                    .pattern("PPP")
                    .unlockedBy(getHasName(GCBlocks.GLASS_FLUID_PIPE), has(GCBlocks.GLASS_FLUID_PIPE))
                    .save(output, RecipeBuilder.getDefaultRecipeId(pipe).withPrefix("dye_"));
        }

        // Wash fluid pipes
        GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, GCBlocks.GLASS_FLUID_PIPE, 8)
                .define('P', GCItemTags.STAINED_GLASS_FLUID_PIPES)
                .define('W', Items.WATER_BUCKET)
                .pattern("PPP")
                .pattern("PWP")
                .pattern("PPP")
                .unlockedBy(getHasName(GCBlocks.GLASS_FLUID_PIPE), has(GCBlocks.GLASS_FLUID_PIPE))
                .save(output, Constant.id("_wash_stained_glass_fluid_pipe")); // Leading _ here makes REI show it after the pane recipe

        // Wires
        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ALUMINUM_WIRE, 6)
                .define('W', ItemTags.WOOL)
                .define('A', GCItemTags.ALUMINUM_INGOTS)
                .pattern("WWW")
                .pattern("AAA")
                .pattern("WWW")
                .unlockedBy(getHasName(GCItems.ALUMINUM_INGOT), has(GCItemTags.ALUMINUM_INGOTS))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.HEAVY_ALUMINUM_WIRE)
                .define('A', GCItemTags.ALUMINUM_INGOTS)
                .define('W', GCBlocks.ALUMINUM_WIRE)
                .define('L', ItemTags.WOOL)
                .pattern("A")
                .pattern("W")
                .pattern("L")
                .unlockedBy(getHasName(GCBlocks.ALUMINUM_WIRE), has(GCBlocks.ALUMINUM_WIRE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.HEAVY_ALUMINUM_WIRE)
                .define('A', GCItemTags.ALUMINUM_INGOTS)
                .define('W', GCBlocks.ALUMINUM_WIRE)
                .define('L', ItemTags.WOOL)
                .pattern("L")
                .pattern("W")
                .pattern("A")
                .unlockedBy(getHasName(GCBlocks.ALUMINUM_WIRE), has(GCBlocks.ALUMINUM_WIRE))
                .save(output, RecipeBuilder.getDefaultRecipeId(GCBlocks.HEAVY_ALUMINUM_WIRE).withSuffix("_mirrored"));

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.SEALABLE_ALUMINUM_WIRE)
                .define('T', GCBlocks.TIN_DECORATION)
                .define('W', GCBlocks.ALUMINUM_WIRE)
                .pattern("TWT")
                .unlockedBy(getHasName(GCBlocks.ALUMINUM_WIRE), has(GCBlocks.ALUMINUM_WIRE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.HEAVY_SEALABLE_ALUMINUM_WIRE)
                .define('T', GCBlocks.TIN_DECORATION)
                .define('W', GCBlocks.HEAVY_ALUMINUM_WIRE)
                .pattern("TWT")
                .unlockedBy(getHasName(GCBlocks.HEAVY_ALUMINUM_WIRE), has(GCBlocks.HEAVY_ALUMINUM_WIRE))
                .emiDefaultRecipe(true)
                .save(output);

        for (PipeColor color : PipeColor.values()) {
            ItemLike pipe = GCBlocks.GLASS_FLUID_PIPES.get(color);
            GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.SEALABLE_FLUID_PIPES.get(color))
                    .define('T', GCBlocks.TIN_DECORATION)
                    .define('P', pipe)
                    .pattern("TPT")
                    .unlockedBy(getHasName(pipe), has(pipe))
                    .emiDefaultRecipe(true)
                    .save(output);
        }

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.WALKWAY, 5)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .pattern("TTT")
                .pattern(" T ")
                .unlockedBy(getHasName(GCItems.COMPRESSED_TITANIUM), has(GCItems.COMPRESSED_TITANIUM))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.WIRE_WALKWAY, 5)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .define('W', GCBlocks.ALUMINUM_WIRE)
                .pattern("TTT")
                .pattern("WTW")
                .pattern("WWW")
                .unlockedBy(getHasName(GCItems.COMPRESSED_TITANIUM), has(GCItems.COMPRESSED_TITANIUM))
                .save(output);

        GCShapelessRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.WIRE_WALKWAY)
                .requires(GCBlocks.WALKWAY)
                .requires(GCBlocks.ALUMINUM_WIRE)
                .unlockedBy(getHasName(GCBlocks.WALKWAY), has(GCBlocks.WALKWAY))
                .save(output, RecipeBuilder.getDefaultRecipeId(GCBlocks.WIRE_WALKWAY).withSuffix("_shapeless"));

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.HEAVY_WIRE_WALKWAY, 5)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .define('W', GCBlocks.HEAVY_ALUMINUM_WIRE)
                .pattern("TTT")
                .pattern("WTW")
                .pattern("WWW")
                .unlockedBy(getHasName(GCItems.COMPRESSED_TITANIUM), has(GCItems.COMPRESSED_TITANIUM))
                .save(output);

        GCShapelessRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.HEAVY_WIRE_WALKWAY)
                .requires(GCBlocks.WALKWAY)
                .requires(GCBlocks.HEAVY_ALUMINUM_WIRE)
                .unlockedBy(getHasName(GCBlocks.WALKWAY), has(GCBlocks.WALKWAY))
                .save(output, RecipeBuilder.getDefaultRecipeId(GCBlocks.HEAVY_WIRE_WALKWAY).withSuffix("_shapeless"));

        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.FLUID_PIPE_WALKWAY, 5)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .define('P', GCItemTags.GLASS_FLUID_PIPES)
                .pattern("TTT")
                .pattern("PTP")
                .pattern("PPP")
                .unlockedBy(getHasName(GCItems.COMPRESSED_TITANIUM), has(GCItems.COMPRESSED_TITANIUM))
                .save(output);

        GCShapelessRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.FLUID_PIPE_WALKWAY)
                .requires(GCBlocks.WALKWAY)
                .requires(GCItemTags.GLASS_FLUID_PIPES)
                .unlockedBy(getHasName(GCBlocks.WALKWAY), has(GCBlocks.WALKWAY))
                .save(output, RecipeBuilder.getDefaultRecipeId(GCBlocks.FLUID_PIPE_WALKWAY).withSuffix("_shapeless"));

        // Compressor
        GCShapelessRecipeBuilder.compressing(GCItems.CRACKER, 3)
                .unlockedBy(getHasName(Items.BREAD), has(Items.BREAD))
                .requires(Items.BREAD)
                .requires(Items.BREAD)
                .time(100)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_ALUMINUM)
                .unlockedBy(getHasName(GCItems.ALUMINUM_INGOT), has(GCItemTags.ALUMINUM_INGOTS))
                .requires(GCItemTags.ALUMINUM_INGOTS)
                .requires(GCItemTags.ALUMINUM_INGOTS)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_BRONZE)
                .unlockedBy(getHasName(GCItems.COMPRESSED_COPPER), has(GCItems.COMPRESSED_COPPER))
                .requires(GCItems.COMPRESSED_COPPER)
                .requires(GCItems.COMPRESSED_TIN)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_BRONZE)
                .unlockedBy("has_bronze_ingot", has(GCItemTags.BRONZE_INGOTS))
                .requires(GCItemTags.BRONZE_INGOTS)
                .requires(GCItemTags.BRONZE_INGOTS)
                .save(output, RecipeBuilder.getDefaultRecipeId(GCItems.COMPRESSED_BRONZE).withSuffix("_from_ingots"));
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_COPPER)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(ConventionalItemTags.COPPER_INGOTS))
                .requires(ConventionalItemTags.COPPER_INGOTS)
                .requires(ConventionalItemTags.COPPER_INGOTS)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_DESH)
                .unlockedBy(getHasName(GCItems.DESH_INGOT), has(GCItems.DESH_INGOT))
                .requires(GCItems.DESH_INGOT)
                .requires(GCItems.DESH_INGOT)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_IRON)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(ConventionalItemTags.IRON_INGOTS))
                .requires(ConventionalItemTags.IRON_INGOTS)
                .requires(ConventionalItemTags.IRON_INGOTS)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_METEORIC_IRON)
                .unlockedBy(getHasName(GCItems.METEORIC_IRON_INGOT), has(GCItems.METEORIC_IRON_INGOT))
                .requires(GCItems.METEORIC_IRON_INGOT)
                .requires(GCItems.METEORIC_IRON_INGOT)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_STEEL)
                .unlockedBy(getHasName(GCItems.COMPRESSED_IRON), has(GCItems.COMPRESSED_IRON))
                .requires(ItemTags.COALS)
                .requires(GCItems.COMPRESSED_IRON)
                .requires(ItemTags.COALS)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_STEEL)
                .unlockedBy(getHasName(GCItems.CARBON_FRAGMENTS), has(GCItems.CARBON_FRAGMENTS))
                .requires(GCItems.COMPRESSED_IRON)
                .requires(GCItems.CARBON_FRAGMENTS)
                .time(100)
                .save(output, RecipeBuilder.getDefaultRecipeId(GCItems.COMPRESSED_STEEL).withSuffix("_from_carbon_fragments"));
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_STEEL)
                .unlockedBy("has_steel_ingot", has(GCItemTags.STEEL_INGOTS))
                .requires(GCItemTags.STEEL_INGOTS)
                .requires(GCItemTags.STEEL_INGOTS)
                .save(output, RecipeBuilder.getDefaultRecipeId(GCItems.COMPRESSED_STEEL).withSuffix("_from_ingots"));
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_TIN)
                .unlockedBy(getHasName(GCItems.TIN_INGOT), has(GCItemTags.TIN_INGOTS))
                .requires(GCItemTags.TIN_INGOTS)
                .requires(GCItemTags.TIN_INGOTS)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_TITANIUM)
                .unlockedBy(getHasName(GCItems.TITANIUM_INGOT), has(GCItems.TITANIUM_INGOT))
                .requires(GCItems.TITANIUM_INGOT)
                .requires(GCItems.TITANIUM_INGOT)
                .emiDefaultRecipe(true)
                .save(output);

        // Niobium Metallurgy (Aluminothermic, Carbothermic, HSLA & C-103 Superalloy)
        GCShapelessRecipeBuilder.compressing(GCItems.FERRONIOBIUM_INGOT)
                .unlockedBy(getHasName(GCItems.NIOBIUM_OXIDE), has(GCItems.NIOBIUM_OXIDE))
                .requires(GCItems.NIOBIUM_OXIDE)
                .requires(GCItemTags.ALUMINUM_INGOTS)
                .requires(ConventionalItemTags.IRON_INGOTS)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_FERRONIOBIUM)
                .unlockedBy(getHasName(GCItems.FERRONIOBIUM_INGOT), has(GCItemTags.FERRONIOBIUM_INGOTS))
                .requires(GCItemTags.FERRONIOBIUM_INGOTS)
                .requires(GCItemTags.FERRONIOBIUM_INGOTS)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.NIOBIUM_INGOT)
                .unlockedBy(getHasName(GCItems.NIOBIUM_OXIDE), has(GCItems.NIOBIUM_OXIDE))
                .requires(GCItems.NIOBIUM_OXIDE)
                .requires(GCItems.CARBON_FRAGMENTS)
                .time(300)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_NIOBIUM)
                .unlockedBy(getHasName(GCItems.NIOBIUM_INGOT), has(GCItemTags.NIOBIUM_INGOTS))
                .requires(GCItemTags.NIOBIUM_INGOTS)
                .requires(GCItemTags.TITANIUM_INGOTS)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_NIOBIUM)
                .unlockedBy(getHasName(GCItems.NIOBIUM_INGOT), has(GCItemTags.NIOBIUM_INGOTS))
                .requires(GCItemTags.NIOBIUM_INGOTS)
                .requires(GCItemTags.METEORIC_IRON_INGOTS)
                .save(output, RecipeBuilder.getDefaultRecipeId(GCItems.COMPRESSED_NIOBIUM).withSuffix("_from_meteoric_iron"));

        // Outer Planet Exotic Alloy Plates (Palladium & Magnesium)
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_PALLADIUM)
                .unlockedBy(getHasName(GCItems.PALLADIUM_INGOT), has(GCItemTags.PALLADIUM_INGOTS))
                .requires(GCItemTags.PALLADIUM_INGOTS)
                .requires(GCItemTags.PALLADIUM_INGOTS)
                .emiDefaultRecipe(true)
                .save(output);
        GCShapelessRecipeBuilder.compressing(GCItems.COMPRESSED_MAGNESIUM)
                .unlockedBy(getHasName(GCItems.MAGNESIUM_INGOT), has(GCItemTags.MAGNESIUM_INGOTS))
                .requires(GCItemTags.MAGNESIUM_INGOTS)
                .requires(GCItemTags.MAGNESIUM_INGOTS)
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.compressing(GCItems.TIER_1_HEAVY_DUTY_PLATE, 2)
                .unlockedBy(getHasName(GCItems.COMPRESSED_FERRONIOBIUM), has(GCItems.COMPRESSED_FERRONIOBIUM))
                .define('F', GCItems.COMPRESSED_FERRONIOBIUM)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .define('B', GCItems.COMPRESSED_BRONZE)
                .pattern("FAB")
                .pattern("SAB")
                .emiDefaultRecipe(true)
                .save(output);

        GCShapelessRecipeBuilder.compressing(GCItems.TIER_2_HEAVY_DUTY_PLATE)
                .unlockedBy(getHasName(GCItems.COMPRESSED_METEORIC_IRON), has(GCItems.COMPRESSED_METEORIC_IRON))
                .requires(GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .requires(GCItems.COMPRESSED_METEORIC_IRON)
                .emiDefaultRecipe(true)
                .save(output);

        GCShapelessRecipeBuilder.compressing(GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .unlockedBy(getHasName(GCItems.COMPRESSED_METEORIC_IRON), has(GCItems.COMPRESSED_METEORIC_IRON))
                .requires(GCItems.TIER_2_HEAVY_DUTY_PLATE)
                .requires(GCItems.COMPRESSED_DESH)
                .emiDefaultRecipe(true)
                .save(output);

        // Circuit Fabricator
        CircuitFabricatorRecipeBuilder.create(GCItems.BASIC_WAFER, 3)
                .requires(Items.REDSTONE_TORCH)
                .emiDefaultRecipe(true)
                .save(output);
        CircuitFabricatorRecipeBuilder.create(GCItems.ADVANCED_WAFER)
                .requires(GCItems.NIOBIUM_OXIDE)
                .emiDefaultRecipe(true)
                .save(output);

        CircuitFabricatorRecipeBuilder.create(GCItems.BLUE_SOLAR_WAFER, 9)
                .requires(ConventionalItemTags.LAPIS_GEMS)
                .emiDefaultRecipe(true)
                .save(output);
        CircuitFabricatorRecipeBuilder.create(GCItems.SOLAR_ARRAY_WAFER, 3)
                .requires(GCItemTags.SOLAR_DUSTS)
                .emiDefaultRecipe(true)
                .save(output);

        // Regolith Thermal Desorber Recipes (Vacuum heating >700C -> SOLAR_VOLATILE_GAS + solid byproducts)
        ThermalDesorptionRecipeBuilder.create(GCBlocks.MOON_TURF, 100L)
                .primaryByproduct(GCItems.SILICON, 1)
                .time(80)
                .save(output, Constant.id("moon_turf"));
        ThermalDesorptionRecipeBuilder.create(GCBlocks.MOON_DIRT, 100L)
                .primaryByproduct(GCItems.SILICON, 1)
                .time(80)
                .save(output, Constant.id("moon_dirt"));
        ThermalDesorptionRecipeBuilder.create(GCBlocks.MOON_BASALT, 200L)
                .primaryByproduct(GCItems.SILICON, 1)
                .time(100)
                .save(output, Constant.id("moon_basalt"));
        ThermalDesorptionRecipeBuilder.create(GCBlocks.MOON_ILMENITE_ORE, 1000L)
                .primaryByproduct(GCItems.RAW_TITANIUM, 1)
                .secondaryByproduct(GCItems.SILICON, 1)
                .time(120)
                .save(output, Constant.id("moon_ilmenite_ore"));
        ThermalDesorptionRecipeBuilder.create(GCBlocks.MERCURY_HOLLOW_DEPOSIT, 450L)
                .primaryByproduct(GCItems.SILICON, 1)
                .time(100)
                .save(output, Constant.id("mercury_hollow_deposit"));
        ThermalDesorptionRecipeBuilder.create(GCBlocks.SATURN_HELIUM_MANTLE_ROCK, 1200L)
                .time(100)
                .save(output, Constant.id("saturn_helium_mantle_rock"));

        // Aneutronic D-3He MHD Fusion Reactor Recipe
        FusionReactorRecipeBuilder.create(Gases.HELIUM_3_ID, 10L, Gases.DEUTERIUM_ID, 10L, 2500L)
                .cycleTicks(40)
                .outputCell(GCItems.HELIUM3_FUSION_CELL)
                .save(output, Constant.id("d_he3_mhd_fusion"));

        // Vacuum Metallurgic Infuser Recipes (60 GJ/t, internal Mekanism-style agent buffer)
        MetallurgicInfusingRecipeBuilder.create(
                        ConventionalItemTags.IRON_INGOTS,
                        VacuumMetallurgicInfuserBlockEntity.INFUSION_CARBON,
                        10,
                        GCItems.COMPRESSED_STEEL,
                        1)
                .time(80)
                .save(output, Constant.id("compressed_steel_from_iron"));
        MetallurgicInfusingRecipeBuilder.create(
                        GCItems.COMPRESSED_IRON,
                        VacuumMetallurgicInfuserBlockEntity.INFUSION_CARBON,
                        10,
                        GCItems.COMPRESSED_STEEL,
                        2)
                .time(90)
                .save(output, Constant.id("compressed_steel_from_compressed_iron"));
        MetallurgicInfusingRecipeBuilder.create(
                        ConventionalItemTags.IRON_INGOTS,
                        VacuumMetallurgicInfuserBlockEntity.INFUSION_NIOBIUM_OXIDE,
                        20,
                        GCItems.FERRONIOBIUM_INGOT,
                        1)
                .time(100)
                .save(output, Constant.id("ferroniobium_from_iron"));
        MetallurgicInfusingRecipeBuilder.create(
                        GCItems.COMPRESSED_STEEL,
                        VacuumMetallurgicInfuserBlockEntity.INFUSION_NIOBIUM_OXIDE,
                        20,
                        GCItems.COMPRESSED_FERRONIOBIUM,
                        1)
                .time(100)
                .save(output, Constant.id("compressed_ferroniobium_from_steel"));
        MetallurgicInfusingRecipeBuilder.create(
                        GCItemTags.NIOBIUM_INGOTS,
                        VacuumMetallurgicInfuserBlockEntity.INFUSION_TITANIUM,
                        20,
                        GCItems.COMPRESSED_NIOBIUM,
                        1)
                .time(120)
                .save(output, Constant.id("compressed_niobium_c103"));
        MetallurgicInfusingRecipeBuilder.create(
                        GCItemTags.NIOBIUM_INGOTS,
                        VacuumMetallurgicInfuserBlockEntity.INFUSION_TIN,
                        20,
                        GCItems.SUPERCONDUCTING_COIL,
                        1)
                .time(140)
                .save(output, Constant.id("superconducting_coil_nb3sn"));

        // Chemical Leaching Plant Recipes (90 GJ/t, 2x-3x refractory ore multiplication)
        ChemicalLeachingRecipeBuilder.create(GCItems.RAW_NIOBIUM, GCFluids.SULFURIC_ACID, 250L, GCItems.NIOBIUM_OXIDE, 3)
                .byproduct(GCItems.RAW_URANIUM, 1, 0.25F)
                .time(100)
                .save(output, Constant.id("raw_niobium_sulfuric_acid"));
        ChemicalLeachingRecipeBuilder.create(GCItems.RAW_NIOBIUM, Fluids.WATER, 500L, GCItems.NIOBIUM_OXIDE, 2)
                .time(140)
                .save(output, Constant.id("raw_niobium_water"));
        ChemicalLeachingRecipeBuilder.create(GCItems.RAW_TITANIUM, GCFluids.SULFURIC_ACID, 250L, GCItems.TITANIUM_INGOT, 2)
                .byproduct(Items.RAW_IRON, 1, 1.0F)
                .time(110)
                .save(output, Constant.id("raw_titanium_sulfuric_acid"));
        ChemicalLeachingRecipeBuilder.create(GCBlocks.ILMENITE_ORE, GCFluids.SULFURIC_ACID, 250L, GCItems.TITANIUM_INGOT, 2)
                .byproduct(Items.RAW_IRON, 1, 1.0F)
                .time(120)
                .save(output, Constant.id("ilmenite_ore_sulfuric_acid"));
        ChemicalLeachingRecipeBuilder.create(GCBlocks.MOON_ILMENITE_ORE, GCFluids.SULFURIC_ACID, 250L, GCItems.TITANIUM_INGOT, 2)
                .byproduct(Items.RAW_IRON, 1, 1.0F)
                .time(120)
                .save(output, Constant.id("moon_ilmenite_ore_sulfuric_acid"));
        ChemicalLeachingRecipeBuilder.create(GCItems.RAW_DESH, GCFluids.SULFURIC_ACID, 250L, GCItems.DESH_INGOT, 2)
                .byproduct(GCItems.DESH_INGOT, 1, 0.50F)
                .time(110)
                .save(output, Constant.id("raw_desh_sulfuric_acid"));
        ChemicalLeachingRecipeBuilder.create(GCItems.RAW_ALUMINUM, GCFluids.SULFURIC_ACID, 250L, GCItems.ALUMINUM_INGOT, 2)
                .byproduct(GCItems.ALUMINUM_INGOT, 1, 0.50F)
                .time(100)
                .save(output, Constant.id("raw_aluminum_sulfuric_acid"));

        // Aerospace Composite Autoclave Recipes (80 GJ/t, pressurized thermal curing)
        CompositeAutoclaveRecipeBuilder.create(GCItems.CARBON_FIBER_PLATE, 1)
                .requires(GCItems.CARBON_FRAGMENTS)
                .requires(GCItems.CANVAS)
                .temperature(400)
                .pressure(12)
                .time(100)
                .save(output, Constant.id("carbon_fiber_plate"));
        CompositeAutoclaveRecipeBuilder.create(GCItems.ABLATIVE_HEAT_TILE, 1)
                .requires(GCItems.OLIVINE_SHARD)
                .requires(GCItems.CARBON_FRAGMENTS)
                .requires(GCItems.SILICON)
                .temperature(550)
                .pressure(16)
                .time(120)
                .save(output, Constant.id("ablative_heat_tile"));
        CompositeAutoclaveRecipeBuilder.create(GCItems.SOFI_INSULATION, 1)
                .requires(GCItems.ISOTHERMAL_FABRIC)
                .requires(GCItems.DESH_INGOT)
                .temperature(350)
                .pressure(10)
                .time(100)
                .save(output, Constant.id("sofi_insulation"));

        // Electrolytic Separator Recipes (120 GJ/t, 2 H2O -> 2 LH2 + 1 LOX + trace Deuterium from planetary ice)
        ElectrolyticRecipeBuilder.fromFluid(Fluids.WATER, 200L, GCFluids.LIQUID_HYDROGEN, 200L, GCFluids.LIQUID_OXYGEN, 100L)
                .time(20)
                .energyPerTick(120L)
                .save(output, Constant.id("water_electrolysis"));
        ElectrolyticRecipeBuilder.fromSolid(GCBlocks.DENSE_ICE, GCFluids.LIQUID_HYDROGEN, 500L, GCFluids.LIQUID_OXYGEN, 250L)
                .traceGas(Gases.DEUTERIUM, 25L)
                .time(40)
                .energyPerTick(120L)
                .save(output, Constant.id("dense_ice_electrolysis"));
        ElectrolyticRecipeBuilder.fromSolid(Items.BLUE_ICE, GCFluids.LIQUID_HYDROGEN, 400L, GCFluids.LIQUID_OXYGEN, 200L)
                .traceGas(Gases.DEUTERIUM, 15L)
                .time(36)
                .energyPerTick(120L)
                .save(output, Constant.id("blue_ice_electrolysis"));
        ElectrolyticRecipeBuilder.fromSolid(Items.PACKED_ICE, GCFluids.LIQUID_HYDROGEN, 250L, GCFluids.LIQUID_OXYGEN, 125L)
                .traceGas(Gases.DEUTERIUM, 8L)
                .time(30)
                .energyPerTick(120L)
                .save(output, Constant.id("packed_ice_electrolysis"));
        ElectrolyticRecipeBuilder.fromSolid(Items.ICE, GCFluids.LIQUID_HYDROGEN, 150L, GCFluids.LIQUID_OXYGEN, 75L)
                .traceGas(Gases.DEUTERIUM, 4L)
                .time(25)
                .energyPerTick(120L)
                .save(output, Constant.id("ice_electrolysis"));

        // Sabatier Chemical Reactor Recipes (110 GJ/t, catalytic synthesis with Nb/Pd bed; Pd grants 1.5x speed)
        SabatierRecipeBuilder.create(Gases.CARBON_DIOXIDE, 100L, GCFluids.LIQUID_METHANE, 100L)
                .reagentB(GCFluids.LIQUID_HYDROGEN, 400L)
                .productB(Fluids.WATER, 200L)
                .catalyst(GCItems.NIOBIUM_INGOT, GCItems.PALLADIUM_INGOT)
                .targetTemp(350)
                .time(40)
                .energyPerTick(110L)
                .formula("CO2 + 4 LH2 -> CH4 + 2 H2O")
                .save(output, Constant.id("sabatier_methalox_lh2"));
        SabatierRecipeBuilder.create(Gases.CARBON_DIOXIDE, 100L, GCFluids.LIQUID_METHANE, 100L)
                .reagentB(Gases.HYDROGEN, 400L)
                .productB(Fluids.WATER, 200L)
                .catalyst(GCItems.NIOBIUM_INGOT, GCItems.PALLADIUM_INGOT)
                .targetTemp(350)
                .time(40)
                .energyPerTick(110L)
                .formula("CO2 + 4 H2 -> CH4 + 2 H2O")
                .save(output, Constant.id("sabatier_methalox_h2"));
        SabatierRecipeBuilder.create(Gases.NITROGEN, 100L, GCFluids.MONOPROPELLANT, 100L)
                .reagentB(GCFluids.LIQUID_HYDROGEN, 200L)
                .catalyst(GCItems.NIOBIUM_INGOT, GCItems.PALLADIUM_INGOT)
                .targetTemp(420)
                .time(45)
                .energyPerTick(110L)
                .formula("N2 + 2 LH2 -> N2H4")
                .save(output, Constant.id("hydrazine_monopropellant_lh2"));
        SabatierRecipeBuilder.create(Gases.NITROGEN, 100L, GCFluids.MONOPROPELLANT, 100L)
                .reagentB(Gases.HYDROGEN, 200L)
                .catalyst(GCItems.NIOBIUM_INGOT, GCItems.PALLADIUM_INGOT)
                .targetTemp(420)
                .time(45)
                .energyPerTick(110L)
                .formula("N2 + 2 H2 -> N2H4")
                .save(output, Constant.id("hydrazine_monopropellant_h2"));
        SabatierRecipeBuilder.create(Fluids.WATER, 250L, GCFluids.SULFURIC_ACID, 250L)
                .solidReagent(GCBlocks.MERCURY_SULFIDE_ORE)
                .catalyst(GCItems.NIOBIUM_INGOT, GCItems.PALLADIUM_INGOT)
                .targetTemp(300)
                .time(50)
                .energyPerTick(110L)
                .formula("HgS + H2O -> H2SO4")
                .save(output, Constant.id("sulfuric_acid_from_mercury_sulfide"));
        SabatierRecipeBuilder.create(Gases.XENON, 100L, GCFluids.LIQUID_XENON, 100L)
                .reagentB(GCFluids.LIQUID_OXYGEN, 50L)
                .productB(Gases.OXYGEN, 50L)
                .catalyst(GCItems.NIOBIUM_INGOT, GCItems.PALLADIUM_INGOT)
                .targetTemp(180)
                .time(35)
                .energyPerTick(110L)
                .formula("Xe + LOX -> LXe + O2")
                .save(output, Constant.id("cryo_xenon_condensation"));

        // Isotopic Centrifuge Recipes (magnetic separation, radioisotope refinement, and metal scrap recovery)
        CentrifugeRecipeBuilder.create(GCBlocks.DENSE_ICE)
                .outputGas(Gases.DEUTERIUM, 250L)
                .time(60)
                .save(output, Constant.id("deuterium_from_dense_ice"));
        CentrifugeRecipeBuilder.create(GCBlocks.SATURN_HELIUM_MANTLE_ROCK)
                .outputGas(Gases.HELIUM_3, 350L)
                .time(60)
                .save(output, Constant.id("helium3_from_saturn_mantle"));
        CentrifugeRecipeBuilder.create(GCBlocks.MOON_ILMENITE_ORE)
                .outputGas(Gases.HELIUM_3, 200L)
                .outputItem(GCItems.TITANIUM_NUGGET, 1)
                .time(60)
                .save(output, Constant.id("helium3_from_moon_ilmenite"));
        CentrifugeRecipeBuilder.create(GCBlocks.MERCURY_HOLLOW_DEPOSIT)
                .outputGas(Gases.HELIUM_3, 150L)
                .outputItem(GCItems.SILICON, 1)
                .time(60)
                .save(output, Constant.id("helium3_from_mercury_hollow"));
        CentrifugeRecipeBuilder.create(GCBlocks.MOON_BASALT)
                .outputGas(Gases.HELIUM_3, 40L)
                .outputItem(GCItems.SILICON, 1)
                .time(60)
                .save(output, Constant.id("helium3_from_moon_basalt"));
        CentrifugeRecipeBuilder.create(GCBlocks.MOON_TURF)
                .outputGas(Gases.HELIUM_3, 20L)
                .outputItem(GCItems.SILICON, 1)
                .time(60)
                .save(output, Constant.id("helium3_from_moon_turf"));
        CentrifugeRecipeBuilder.create(GCBlocks.MOON_DIRT)
                .outputGas(Gases.HELIUM_3, 20L)
                .outputItem(GCItems.SILICON, 1)
                .time(60)
                .save(output, Constant.id("helium3_from_moon_dirt"));
        CentrifugeRecipeBuilder.create(GCBlocks.MERCURY_SULFIDE_ORE)
                .outputItem(GCItems.RADIOISOTOPE_CORE, 1)
                .time(80)
                .save(output, Constant.id("radioisotope_core_from_mercury_sulfide"));
        CentrifugeRecipeBuilder.create(GCItems.URANIUM_INGOT)
                .outputItem(GCItems.RADIOISOTOPE_CORE, 2)
                .time(80)
                .save(output, Constant.id("radioisotope_core_from_uranium"));
        CentrifugeRecipeBuilder.create(GCItems.TIN_CANISTER)
                .outputItem(GCItems.TIN_INGOT, 2)
                .time(40)
                .save(output, Constant.id("scrap_tin_canister"));
        CentrifugeRecipeBuilder.create(GCItems.COPPER_CANISTER)
                .outputItem(Items.COPPER_INGOT, 2)
                .time(40)
                .save(output, Constant.id("scrap_copper_canister"));
        CentrifugeRecipeBuilder.create(GCItems.EMPTY_CAN)
                .outputItem(GCItems.TIN_INGOT, 1)
                .time(30)
                .save(output, Constant.id("scrap_empty_can"));
        CentrifugeRecipeBuilder.create(GCBlocks.ALUMINUM_WIRE)
                .outputItem(GCItems.ALUMINUM_INGOT, 1)
                .time(40)
                .save(output, Constant.id("scrap_aluminum_wire"));
    }

    @Override
    public @NotNull String getName() {
        return "Machine Recipes";
    }
}
