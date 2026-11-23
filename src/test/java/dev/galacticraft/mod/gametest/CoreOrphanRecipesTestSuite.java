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

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.recipe.CompressingRecipe;
import dev.galacticraft.mod.recipe.GCRecipes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import java.util.List;
import java.util.Optional;

public class CoreOrphanRecipesTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void coreOrphanRecipesResolveInRecipeManager(GameTestHelper context) {
        RecipeManager manager = context.getLevel().getRecipeManager();
        var registryAccess = context.getLevel().registryAccess();

        // 1. Target shaped recipes resolve by ID
        for (String id : List.of(
                "radioisotope_core",
                "olivine_glass",
                "shield_controller",
                "player_transport_tube",
                "advanced_solar_panel",
                "beam_core_sapphire",
                "sensor_lens")) {
            Optional<RecipeHolder<?>> holder = manager.byKey(Constant.id(id));
            if (holder.isEmpty()) {
                context.fail("Missing RecipeHolder in RecipeManager for galacticraft:" + id);
                return;
            }
        }

        // 2. Compressor: 1x COMPRESSED_IRON + 1x CARBON_FRAGMENTS -> COMPRESSED_STEEL
        CraftingInput compressorInput = CraftingInput.of(3, 3, List.of(
                new ItemStack(GCItems.COMPRESSED_IRON),
                new ItemStack(GCItems.CARBON_FRAGMENTS),
                ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
        ));
        Optional<RecipeHolder<CompressingRecipe>> steelRecipe = manager.getRecipeFor(GCRecipes.COMPRESSING_TYPE, compressorInput, context.getLevel());
        if (steelRecipe.isEmpty()) {
            context.fail("Compressor failed to match COMPRESSED_IRON + CARBON_FRAGMENTS -> COMPRESSED_STEEL!");
            return;
        }
        ItemStack steelResult = steelRecipe.get().value().getResultItem(registryAccess);
        if (!steelResult.is(GCItems.COMPRESSED_STEEL)) {
            context.fail("Expected COMPRESSED_STEEL from carbon_fragments compressing recipe, got: " + steelResult);
            return;
        }

        // 3. Mercury Sulfide Ore smelting & blasting -> SOLAR_DUST with 0.9f XP
        SingleRecipeInput sulfideInput = new SingleRecipeInput(new ItemStack(GCBlocks.MERCURY_SULFIDE_ORE));
        Optional<RecipeHolder<SmeltingRecipe>> smeltHolder = manager.getRecipeFor(RecipeType.SMELTING, sulfideInput, context.getLevel());
        if (smeltHolder.isEmpty() || !smeltHolder.get().value().getResultItem(registryAccess).is(GCItems.SOLAR_DUST)) {
            context.fail("MERCURY_SULFIDE_ORE failed to resolve smelting recipe to SOLAR_DUST!");
            return;
        }
        if (Math.abs(smeltHolder.get().value().getExperience() - 0.9f) > 1e-4f) {
            context.fail("Expected 0.9f XP for MERCURY_SULFIDE_ORE smelting, got: " + smeltHolder.get().value().getExperience());
            return;
        }

        Optional<RecipeHolder<BlastingRecipe>> blastHolder = manager.getRecipeFor(RecipeType.BLASTING, sulfideInput, context.getLevel());
        if (blastHolder.isEmpty() || !blastHolder.get().value().getResultItem(registryAccess).is(GCItems.SOLAR_DUST)) {
            context.fail("MERCURY_SULFIDE_ORE failed to resolve blasting recipe to SOLAR_DUST!");
            return;
        }

        context.succeed();
    }
}
