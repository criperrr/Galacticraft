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

package dev.galacticraft.mod.gametest;

import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.recipe.CompressingRecipe;
import dev.galacticraft.mod.recipe.FabricationRecipe;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.tag.GCBlockTags;
import dev.galacticraft.mod.tag.GCItemTags;
import dev.galacticraft.mod.world.gen.feature.GCOrePlacedFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class NiobiumMetallurgyTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void niobiumOverworldOreFeaturesRegistered(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        var placedRegistry = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        if (!placedRegistry.containsKey(GCOrePlacedFeatures.ORE_NIOBIUM)) {
            context.fail("Missing placed feature galacticraft:ore_niobium");
            return;
        }
        if (!placedRegistry.containsKey(GCOrePlacedFeatures.ORE_NIOBIUM_DEEP)) {
            context.fail("Missing placed feature galacticraft:ore_niobium_deep");
            return;
        }

        Holder<Biome> plains = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        var features = plains.value().getGenerationSettings().features();
        int stepIdx = GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();
        if (stepIdx >= features.size()) {
            context.fail("Overworld biome missing UNDERGROUND_ORES feature step");
            return;
        }

        boolean hasShallow = false;
        boolean hasDeep = false;
        for (Holder<PlacedFeature> holder : features.get(stepIdx)) {
            if (holder.is(GCOrePlacedFeatures.ORE_NIOBIUM)) {
                hasShallow = true;
            }
            if (holder.is(GCOrePlacedFeatures.ORE_NIOBIUM_DEEP)) {
                hasDeep = true;
            }
        }
        if (!hasShallow || !hasDeep) {
            context.fail("Overworld plains biome missing Niobium ore placed features (shallow=" + hasShallow + ", deep=" + hasDeep + ")");
            return;
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void niobiumOreDropsAndToolTags(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BlockPos origin = context.absolutePos(BlockPos.ZERO);

        // Verify mining tags and conventional tags
        if (!GCBlocks.NIOBIUM_ORE.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                || !GCBlocks.NIOBIUM_ORE.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL)
                || !GCBlocks.DEEPSLATE_NIOBIUM_ORE.defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL)
                || !GCBlocks.NIOBIUM_ORE.defaultBlockState().is(GCBlockTags.NIOBIUM_ORES)) {
            context.fail("Niobium ore blocks missing expected pickaxe/iron-tool/ores tags");
            return;
        }
        if (!new ItemStack(GCItems.NIOBIUM_INGOT).is(GCItemTags.NIOBIUM_INGOTS)) {
            context.fail("NIOBIUM_INGOT missing c:ingots/niobium tag");
            return;
        }

        // Normal iron pickaxe drop -> exactly 1 RAW_NIOBIUM
        ItemStack ironPick = new ItemStack(Items.IRON_PICKAXE);
        LootParams.Builder normalParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                .withParameter(LootContextParams.TOOL, ironPick);
        List<ItemStack> normalDrops = GCBlocks.NIOBIUM_ORE.defaultBlockState().getDrops(normalParams);
        if (normalDrops.size() != 1 || !normalDrops.get(0).is(GCItems.RAW_NIOBIUM) || normalDrops.get(0).getCount() != 1) {
            context.fail("Expected 1x RAW_NIOBIUM from NIOBIUM_ORE without Fortune, got: " + normalDrops);
            return;
        }

        // Fortune III pickaxe -> should drop >= 1 and exceed 1 across trials
        ItemStack fortunePick = new ItemStack(Items.DIAMOND_PICKAXE);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        fortunePick.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), 3);
        int totalFortuneDrops = 0;
        for (int i = 0; i < 20; i++) {
            LootParams.Builder fortuneParams = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                    .withParameter(LootContextParams.TOOL, fortunePick);
            List<ItemStack> drops = GCBlocks.DEEPSLATE_NIOBIUM_ORE.defaultBlockState().getDrops(fortuneParams);
            if (drops.isEmpty() || !drops.get(0).is(GCItems.RAW_NIOBIUM)) {
                context.fail("Expected RAW_NIOBIUM from DEEPSLATE_NIOBIUM_ORE with Fortune, got: " + drops);
                return;
            }
            totalFortuneDrops += drops.get(0).getCount();
        }
        if (totalFortuneDrops <= 20) {
            context.fail("Fortune III did not increase RAW_NIOBIUM yield over 20 trials (total=" + totalFortuneDrops + ")");
            return;
        }

        // Silk Touch -> drops the ore block itself
        ItemStack silkPick = new ItemStack(Items.DIAMOND_PICKAXE);
        silkPick.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        LootParams.Builder silkParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                .withParameter(LootContextParams.TOOL, silkPick);
        List<ItemStack> silkDrops = GCBlocks.NIOBIUM_ORE.defaultBlockState().getDrops(silkParams);
        if (silkDrops.size() != 1 || !silkDrops.get(0).is(GCBlocks.NIOBIUM_ORE.asItem())) {
            context.fail("Expected NIOBIUM_ORE block from Silk Touch, got: " + silkDrops);
            return;
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void niobiumMetallurgyChainResolvesInRecipeManager(GameTestHelper context) {
        RecipeManager manager = context.getLevel().getRecipeManager();
        var registryAccess = context.getLevel().registryAccess();

        // 1. Calcination: RAW_NIOBIUM -> NIOBIUM_OXIDE (both smelting and blasting)
        SingleRecipeInput rawInput = new SingleRecipeInput(new ItemStack(GCItems.RAW_NIOBIUM));
        Optional<RecipeHolder<SmeltingRecipe>> smelt = manager.getRecipeFor(RecipeType.SMELTING, rawInput, context.getLevel());
        if (smelt.isEmpty() || !smelt.get().value().getResultItem(registryAccess).is(GCItems.NIOBIUM_OXIDE)) {
            context.fail("RAW_NIOBIUM smelting must produce NIOBIUM_OXIDE");
            return;
        }
        Optional<RecipeHolder<BlastingRecipe>> blast = manager.getRecipeFor(RecipeType.BLASTING, rawInput, context.getLevel());
        if (blast.isEmpty() || !blast.get().value().getResultItem(registryAccess).is(GCItems.NIOBIUM_OXIDE)) {
            context.fail("RAW_NIOBIUM blasting must produce NIOBIUM_OXIDE");
            return;
        }

        // 2. Aluminothermic reduction: NIOBIUM_OXIDE + ALUMINUM_INGOT + IRON_INGOT -> FERRONIOBIUM_INGOT
        CraftingInput alumInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(GCItems.NIOBIUM_OXIDE),
                new ItemStack(GCItems.ALUMINUM_INGOT),
                new ItemStack(Items.IRON_INGOT),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));
        Optional<RecipeHolder<CompressingRecipe>> feNbRecipe = manager.getRecipeFor(GCRecipes.COMPRESSING_TYPE, alumInput, context.getLevel());
        if (feNbRecipe.isEmpty() || !feNbRecipe.get().value().getResultItem(registryAccess).is(GCItems.FERRONIOBIUM_INGOT)) {
            context.fail("Compressor failed to match aluminothermic FERRONIOBIUM_INGOT recipe");
            return;
        }

        // 3. Carbothermic reduction: NIOBIUM_OXIDE + CARBON_FRAGMENTS -> NIOBIUM_INGOT
        CraftingInput carboInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(GCItems.NIOBIUM_OXIDE),
                new ItemStack(GCItems.CARBON_FRAGMENTS),
                ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));
        Optional<RecipeHolder<CompressingRecipe>> nbRecipe = manager.getRecipeFor(GCRecipes.COMPRESSING_TYPE, carboInput, context.getLevel());
        if (nbRecipe.isEmpty() || !nbRecipe.get().value().getResultItem(registryAccess).is(GCItems.NIOBIUM_INGOT)) {
            context.fail("Compressor failed to match carbothermic NIOBIUM_INGOT recipe");
            return;
        }

        // 4. Superalloy C-103: NIOBIUM_INGOT + TITANIUM_INGOT -> COMPRESSED_NIOBIUM
        CraftingInput c103Input = CraftingInput.of(3, 3, List.of(
                new ItemStack(GCItems.NIOBIUM_INGOT),
                new ItemStack(GCItems.TITANIUM_INGOT),
                ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));
        Optional<RecipeHolder<CompressingRecipe>> c103Recipe = manager.getRecipeFor(GCRecipes.COMPRESSING_TYPE, c103Input, context.getLevel());
        if (c103Recipe.isEmpty() || !c103Recipe.get().value().getResultItem(registryAccess).is(GCItems.COMPRESSED_NIOBIUM)) {
            context.fail("Compressor failed to match C-103 COMPRESSED_NIOBIUM recipe");
            return;
        }

        // 5. Superconducting Coil (Nb3Sn) shaped crafting
        CraftingInput coilInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(GCItems.NIOBIUM_INGOT), new ItemStack(GCItems.NIOBIUM_INGOT), new ItemStack(GCItems.NIOBIUM_INGOT),
                new ItemStack(GCBlocks.HEAVY_ALUMINUM_WIRE), new ItemStack(GCBlocks.HEAVY_ALUMINUM_WIRE), new ItemStack(GCBlocks.HEAVY_ALUMINUM_WIRE),
                new ItemStack(GCItems.TIN_INGOT), new ItemStack(GCItems.TIN_INGOT), new ItemStack(GCItems.TIN_INGOT)
        ));
        var coilRecipe = manager.getRecipeFor(RecipeType.CRAFTING, coilInput, context.getLevel());
        if (coilRecipe.isEmpty() || !coilRecipe.get().value().getResultItem(registryAccess).is(GCItems.SUPERCONDUCTING_COIL)) {
            context.fail("Crafting table failed to match SUPERCONDUCTING_COIL recipe");
            return;
        }

        // 6. Circuit Fabricator: ADVANCED_WAFER uses NIOBIUM_OXIDE
        Optional<RecipeHolder<FabricationRecipe>> advWafer = manager.getAllRecipesFor(GCRecipes.FABRICATION_TYPE).stream()
                .filter(h -> h.value().getResultItem(registryAccess).is(GCItems.ADVANCED_WAFER))
                .findFirst();
        if (advWafer.isEmpty() || !advWafer.get().value().getIngredients().get(4).test(new ItemStack(GCItems.NIOBIUM_OXIDE))) {
            context.fail("ADVANCED_WAFER fabrication recipe must require NIOBIUM_OXIDE");
            return;
        }

        context.succeed();
    }
}
