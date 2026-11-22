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

import dev.galacticraft.api.rocket.RocketPrefabs;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.mod.api.data.recipe.GCShapedRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.PartAssemblerRecipeBuilder;
import dev.galacticraft.mod.api.data.recipe.RocketRecipeBuilder;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.tag.GCItemTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class GCRocketRecipes extends FabricRecipeProvider {
    public GCRocketRecipes(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        GCShapedRecipeBuilder.crafting(RecipeCategory.DECORATIONS, GCBlocks.ROCKET_WORKBENCH)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('C', ConventionalItemTags.PLAYER_WORKSTATIONS_CRAFTING_TABLES)
                .define('L', Items.LEVER)
                .define('W', GCItems.ADVANCED_WAFER)
                .define('R', Items.REDSTONE_TORCH)
                .pattern("SCS")
                .pattern("LWL")
                .pattern("SRS")
                .unlockedBy(getHasName(GCItems.ADVANCED_WAFER), has(GCItems.ADVANCED_WAFER))
                .emiDefaultRecipe(true)
                .save(output);

        // Modular Platform Blocks
        GCShapedRecipeBuilder.crafting(RecipeCategory.BUILDING_BLOCKS, GCBlocks.REINFORCED_LAUNCH_PAD, 4)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('I', GCItems.COMPRESSED_IRON)
                .pattern("PPP")
                .pattern("SSS")
                .pattern("III")
                .unlockedBy(getHasName(GCItems.TIER_1_HEAVY_DUTY_PLATE), has(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.BUILDING_BLOCKS, GCBlocks.GANTRY_TRUSS, 6)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .pattern("S S")
                .pattern(" A ")
                .pattern("S S")
                .unlockedBy(getHasName(GCItems.COMPRESSED_STEEL), has(GCItems.COMPRESSED_STEEL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.BUILDING_BLOCKS, GCBlocks.GANTRY_CRANE_RAIL, 4)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .pattern("SSS")
                .pattern("PAP")
                .pattern("SSS")
                .unlockedBy(getHasName(GCItems.COMPRESSED_STEEL), has(GCItems.COMPRESSED_STEEL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, GCBlocks.PLATFORM_UPGRADE_SOCKET)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .define('W', GCItems.BASIC_WAFER)
                .define('I', Items.IRON_BLOCK)
                .pattern("SAS")
                .pattern("PWP")
                .pattern("SIS")
                .unlockedBy(getHasName(GCItems.BASIC_WAFER), has(GCItems.BASIC_WAFER))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, GCBlocks.HANGAR_TERMINAL)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('G', GCBlocks.VACUUM_GLASS)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .define('W', GCItems.ADVANCED_WAFER)
                .define('R', Items.REDSTONE_BLOCK)
                .pattern("SGS")
                .pattern("PWP")
                .pattern("SRS")
                .unlockedBy(getHasName(GCItems.ADVANCED_WAFER), has(GCItems.ADVANCED_WAFER))
                .emiDefaultRecipe(true)
                .save(output);

        // Modular Part Category Schematics
        GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, GCItems.PART_SCHEMATIC_COMMAND)
                .define('P', Items.PAPER)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .define('W', GCItems.ADVANCED_WAFER)
                .pattern("PAP")
                .pattern("AWA")
                .pattern("PAP")
                .unlockedBy(getHasName(GCItems.ADVANCED_WAFER), has(GCItems.ADVANCED_WAFER))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, GCItems.PART_SCHEMATIC_ENGINE)
                .define('P', Items.PAPER)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('C', GCItems.COMPRESSED_COPPER)
                .pattern("PSP")
                .pattern("SCS")
                .pattern("PSP")
                .unlockedBy(getHasName(GCItems.COMPRESSED_STEEL), has(GCItems.COMPRESSED_STEEL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, GCItems.PART_SCHEMATIC_TANK)
                .define('P', Items.PAPER)
                .define('T', GCItems.COMPRESSED_TIN)
                .define('C', GCItems.TIN_CANISTER)
                .pattern("PTP")
                .pattern("TCT")
                .pattern("PTP")
                .unlockedBy(getHasName(GCItems.TIN_CANISTER), has(GCItems.TIN_CANISTER))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, GCItems.PART_SCHEMATIC_ADAPTER)
                .define('P', Items.PAPER)
                .define('I', GCItems.COMPRESSED_IRON)
                .define('S', GCItems.COMPRESSED_STEEL)
                .pattern("PIP")
                .pattern("ISI")
                .pattern("PIP")
                .unlockedBy(getHasName(GCItems.COMPRESSED_STEEL), has(GCItems.COMPRESSED_STEEL))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, GCItems.PART_SCHEMATIC_PAYLOAD)
                .define('P', Items.PAPER)
                .define('A', GCItems.COMPRESSED_ALUMINUM)
                .define('C', ConventionalItemTags.WOODEN_CHESTS)
                .pattern("PAP")
                .pattern("ACA")
                .pattern("PAP")
                .unlockedBy(getHasName(GCItems.COMPRESSED_ALUMINUM), has(GCItems.COMPRESSED_ALUMINUM))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.MISC, GCItems.PART_SCHEMATIC_UTILITY)
                .define('P', Items.PAPER)
                .define('I', GCItems.COMPRESSED_IRON)
                .define('W', GCItems.BASIC_WAFER)
                .pattern("PIP")
                .pattern("IWI")
                .pattern("PIP")
                .unlockedBy(getHasName(GCItems.BASIC_WAFER), has(GCItems.BASIC_WAFER))
                .emiDefaultRecipe(true)
                .save(output);

        // Rocket Part Items
        GCShapedRecipeBuilder.crafting(RecipeCategory.TRANSPORTATION, GCItems.NOSE_CONE)
                .define('R', Items.REDSTONE_TORCH)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .pattern(" R ")
                .pattern(" P ")
                .pattern("P P")
                .unlockedBy(getHasName(GCItems.TIER_1_HEAVY_DUTY_PLATE), has(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.TRANSPORTATION, GCItems.HEAVY_NOSE_CONE)
                .define('R', Items.REDSTONE_TORCH)
                .define('P', GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .pattern(" R ")
                .pattern(" P ")
                .pattern("P P")
                .unlockedBy(getHasName(GCItems.TIER_3_HEAVY_DUTY_PLATE), has(GCItems.TIER_3_HEAVY_DUTY_PLATE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.TRANSPORTATION, GCItems.ROCKET_FIN)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .pattern(" S ")
                .pattern("PSP")
                .pattern("P P")
                .unlockedBy(getHasName(GCItems.TIER_1_HEAVY_DUTY_PLATE), has(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.TRANSPORTATION, GCItems.HEAVY_ROCKET_FIN)
                .define('T', GCItems.TIER_2_HEAVY_DUTY_PLATE)
                .define('P', GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .pattern(" T ")
                .pattern("PTP")
                .pattern("P P")
                .unlockedBy(getHasName(GCItems.TIER_3_HEAVY_DUTY_PLATE), has(GCItems.TIER_3_HEAVY_DUTY_PLATE))
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.TRANSPORTATION, GCItems.HEAVY_ROCKET_FIN)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .define('P', GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .pattern(" T ")
                .pattern("PTP")
                .pattern("P P")
                .unlockedBy(getHasName(GCItems.TIER_3_HEAVY_DUTY_PLATE), has(GCItems.TIER_3_HEAVY_DUTY_PLATE))
                .save(output, getItemName(GCItems.HEAVY_ROCKET_FIN) + "_alt");

        GCShapedRecipeBuilder.crafting(RecipeCategory.TRANSPORTATION, GCItems.ROCKET_ENGINE)
                .define('F', Items.FLINT_AND_STEEL)
                .define('B', ItemTags.STONE_BUTTONS)
                .define('V', GCItems.OXYGEN_VENT)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .define('T', GCItemTags.TIN_CANISTERS)
                .pattern(" FB")
                .pattern("PTP")
                .pattern("PVP")
                .unlockedBy(getHasName(GCItems.TIER_1_HEAVY_DUTY_PLATE), has(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.TRANSPORTATION, GCItems.HEAVY_ROCKET_ENGINE)
                .define('F', Items.FLINT_AND_STEEL)
                .define('B', ItemTags.STONE_BUTTONS)
                .define('V', GCItems.OXYGEN_VENT)
                .define('P', GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .define('T', GCItemTags.TIN_CANISTERS)
                .pattern(" FB")
                .pattern("PTP")
                .pattern("PVP")
                .unlockedBy(getHasName(GCItems.TIER_3_HEAVY_DUTY_PLATE), has(GCItems.TIER_3_HEAVY_DUTY_PLATE))
                .emiDefaultRecipe(true)
                .save(output);

        GCShapedRecipeBuilder.crafting(RecipeCategory.TRANSPORTATION, GCItems.ROCKET_BOOSTER)
                .define('M', GCItems.COMPRESSED_METEORIC_IRON)
                .define('Y', Items.YELLOW_WOOL)
                .define('F', GCItemTags.FUEL_BUCKETS)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .define('V', GCItems.OXYGEN_VENT)
                .pattern("MYM")
                .pattern("MFM")
                .pattern("PVP")
                .unlockedBy(getHasName(GCItems.COMPRESSED_METEORIC_IRON), has(GCItems.COMPRESSED_METEORIC_IRON))
                .emiDefaultRecipe(true)
                .save(output);

        // Part Assembler Recipes
        PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                .define('R', Items.REDSTONE_TORCH)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .pattern(" R ")
                .pattern(" P ")
                .pattern("P P")
                .assemblyTime(200)
                .partStats(new RocketPartStats(0.0, 250.0, 0L, 2.0, 1200.0))
                .unlockedBy(getHasName(GCItems.BASIC_ROCKET_CONE_SCHEMATIC), has(GCItems.BASIC_ROCKET_CONE_SCHEMATIC))
                .unlockedBy(getHasName(GCItems.TIER_1_HEAVY_DUTY_PLATE), has(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                .save(output);

        PartAssemblerRecipeBuilder.create(GCItems.HEAVY_NOSE_CONE)
                .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                .define('R', Items.REDSTONE_TORCH)
                .define('P', GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .pattern(" R ")
                .pattern(" P ")
                .pattern("P P")
                .scrapByproduct(GCItems.COMPRESSED_TITANIUM, 1)
                .assemblyTime(250)
                .partStats(new RocketPartStats(0.0, 450.0, 0L, 5.0, 2500.0))
                .unlockedBy(getHasName(GCItems.BASIC_ROCKET_CONE_SCHEMATIC), has(GCItems.BASIC_ROCKET_CONE_SCHEMATIC))
                .unlockedBy(getHasName(GCItems.TIER_3_HEAVY_DUTY_PLATE), has(GCItems.TIER_3_HEAVY_DUTY_PLATE))
                .save(output);

        PartAssemblerRecipeBuilder.create(GCItems.ROCKET_FIN)
                .schematic(GCItems.BASIC_ROCKET_FINS_SCHEMATIC)
                .define('S', GCItems.COMPRESSED_STEEL)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .pattern(" S ")
                .pattern("PSP")
                .pattern("P P")
                .scrapByproduct(GCItems.COMPRESSED_STEEL, 1)
                .assemblyTime(200)
                .partStats(new RocketPartStats(0.0, 150.0, 0L, 2.5, 1100.0))
                .unlockedBy(getHasName(GCItems.BASIC_ROCKET_FINS_SCHEMATIC), has(GCItems.BASIC_ROCKET_FINS_SCHEMATIC))
                .unlockedBy(getHasName(GCItems.TIER_1_HEAVY_DUTY_PLATE), has(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                .save(output);

        PartAssemblerRecipeBuilder.create(GCItems.HEAVY_ROCKET_FIN)
                .schematic(GCItems.BASIC_ROCKET_FINS_SCHEMATIC)
                .define('T', GCItems.TIER_2_HEAVY_DUTY_PLATE)
                .define('P', GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .pattern(" T ")
                .pattern("PTP")
                .pattern("P P")
                .scrapByproduct(GCItems.COMPRESSED_TITANIUM, 1)
                .assemblyTime(250)
                .partStats(new RocketPartStats(0.0, 300.0, 0L, 6.0, 2200.0))
                .unlockedBy(getHasName(GCItems.BASIC_ROCKET_FINS_SCHEMATIC), has(GCItems.BASIC_ROCKET_FINS_SCHEMATIC))
                .unlockedBy(getHasName(GCItems.TIER_3_HEAVY_DUTY_PLATE), has(GCItems.TIER_3_HEAVY_DUTY_PLATE))
                .save(output);

        PartAssemblerRecipeBuilder.create(GCItems.HEAVY_ROCKET_FIN)
                .schematic(GCItems.BASIC_ROCKET_FINS_SCHEMATIC)
                .define('T', GCItems.COMPRESSED_TITANIUM)
                .define('P', GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .pattern(" T ")
                .pattern("PTP")
                .pattern("P P")
                .scrapByproduct(GCItems.COMPRESSED_TITANIUM, 1)
                .assemblyTime(250)
                .partStats(new RocketPartStats(0.0, 300.0, 0L, 6.0, 2200.0))
                .unlockedBy(getHasName(GCItems.BASIC_ROCKET_FINS_SCHEMATIC), has(GCItems.BASIC_ROCKET_FINS_SCHEMATIC))
                .unlockedBy(getHasName(GCItems.TIER_3_HEAVY_DUTY_PLATE), has(GCItems.TIER_3_HEAVY_DUTY_PLATE))
                .save(output, getItemName(GCItems.HEAVY_ROCKET_FIN) + "_alt");

        PartAssemblerRecipeBuilder.create(GCItems.ROCKET_ENGINE)
                .schematic(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC)
                .define('F', Items.FLINT_AND_STEEL)
                .define('B', ItemTags.STONE_BUTTONS)
                .define('V', GCItems.OXYGEN_VENT)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .define('T', GCItemTags.TIN_CANISTERS)
                .pattern(" FB")
                .pattern("PTP")
                .pattern("PVP")
                .scrapByproduct(GCItems.COMPRESSED_TIN, 1)
                .assemblyTime(300)
                .partStats(new RocketPartStats(1200.0, 850.0, 0L, 3.5, 2200.0))
                .unlockedBy(getHasName(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC), has(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC))
                .unlockedBy(getHasName(GCItems.TIER_1_HEAVY_DUTY_PLATE), has(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                .save(output);

        PartAssemblerRecipeBuilder.create(GCItems.HEAVY_ROCKET_ENGINE)
                .schematic(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC)
                .define('F', Items.FLINT_AND_STEEL)
                .define('B', ItemTags.STONE_BUTTONS)
                .define('V', GCItems.OXYGEN_VENT)
                .define('P', GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .define('T', GCItemTags.TIN_CANISTERS)
                .pattern(" FB")
                .pattern("PTP")
                .pattern("PVP")
                .scrapByproduct(GCItems.COMPRESSED_METEORIC_IRON, 1)
                .assemblyTime(400)
                .partStats(new RocketPartStats(2800.0, 1600.0, 0L, 7.0, 3200.0))
                .unlockedBy(getHasName(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC), has(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC))
                .unlockedBy(getHasName(GCItems.TIER_3_HEAVY_DUTY_PLATE), has(GCItems.TIER_3_HEAVY_DUTY_PLATE))
                .save(output);

        PartAssemblerRecipeBuilder.create(GCItems.ROCKET_BOOSTER)
                .schematic(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC)
                .define('M', GCItems.COMPRESSED_METEORIC_IRON)
                .define('Y', Items.YELLOW_WOOL)
                .define('F', GCItemTags.FUEL_BUCKETS)
                .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .define('V', GCItems.OXYGEN_VENT)
                .pattern("MYM")
                .pattern("MFM")
                .pattern("PVP")
                .scrapByproduct(GCItems.COMPRESSED_IRON, 2)
                .assemblyTime(350)
                .partStats(new RocketPartStats(1800.0, 1100.0, 405000L, 4.0, 2600.0))
                .unlockedBy(getHasName(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC), has(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC))
                .unlockedBy(getHasName(GCItems.COMPRESSED_METEORIC_IRON), has(GCItems.COMPRESSED_METEORIC_IRON))
                .save(output);

        RocketRecipeBuilder.create(GCItems.ROCKET)
                .rocketData(RocketPrefabs.TIER_1)
                .cone(GCItems.NOSE_CONE)
                .body(GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .bodyHeight(4)
                .fins(GCItems.ROCKET_FIN)
                .engine(GCItems.ROCKET_ENGINE)
                .unlockedBy(getHasName(GCItems.TIER_1_HEAVY_DUTY_PLATE), has(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                .save(output);

        RocketRecipeBuilder.create(GCItems.ROCKET)
                .rocketData(RocketPrefabs.TIER_1_STORAGE_UPGRADE)
                .cone(GCItems.NOSE_CONE)
                .body(GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .bodyHeight(4)
                .fins(GCItems.ROCKET_FIN)
                .engine(GCItems.ROCKET_ENGINE)
                .storage(GCItemTags.ROCKET_STORAGE_UPGRADE_ITEMS)
                .unlockedBy(getHasName(GCItems.TIER_1_HEAVY_DUTY_PLATE), has(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                .save(output, getItemName(GCItems.ROCKET) + "_with_storage_upgrade");
    }

    @Override
    public @NotNull String getName() {
        return "Rocket Recipes";
    }
}
