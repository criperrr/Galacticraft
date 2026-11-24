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

package dev.galacticraft.mod.api.data.recipe;

import dev.galacticraft.mod.recipe.ChemicalLeachingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

public class ChemicalLeachingRecipeBuilder {
    private final Ingredient oreIngredient;
    private final ResourceLocation fluidId;
    private final long fluidAmountMb;
    private final ItemStack primaryOutput;
    private ItemStack byproductOutput = ItemStack.EMPTY;
    private float byproductChance = 1.0F;
    private int processingTime = 120;
    private String group = "";

    private ChemicalLeachingRecipeBuilder(Ingredient oreIngredient, ResourceLocation fluidId, long fluidAmountMb, ItemStack primaryOutput) {
        this.oreIngredient = oreIngredient;
        this.fluidId = fluidId;
        this.fluidAmountMb = fluidAmountMb;
        this.primaryOutput = primaryOutput;
    }

    public static ChemicalLeachingRecipeBuilder create(ItemLike ore, Fluid fluid, long fluidAmountMb, ItemLike primaryOutput, int primaryCount) {
        return new ChemicalLeachingRecipeBuilder(
                Ingredient.of(ore),
                BuiltInRegistries.FLUID.getKey(fluid),
                fluidAmountMb,
                new ItemStack(primaryOutput, primaryCount)
        );
    }

    public static ChemicalLeachingRecipeBuilder create(TagKey<Item> oreTag, Fluid fluid, long fluidAmountMb, ItemLike primaryOutput, int primaryCount) {
        return new ChemicalLeachingRecipeBuilder(
                Ingredient.of(oreTag),
                BuiltInRegistries.FLUID.getKey(fluid),
                fluidAmountMb,
                new ItemStack(primaryOutput, primaryCount)
        );
    }

    public ChemicalLeachingRecipeBuilder byproduct(ItemLike item, int count, float chance) {
        this.byproductOutput = new ItemStack(item, count);
        this.byproductChance = chance;
        return this;
    }

    public ChemicalLeachingRecipeBuilder time(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    public ChemicalLeachingRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(
                id.withPrefix("chemical_leaching/"),
                new ChemicalLeachingRecipe(
                        this.group,
                        this.oreIngredient,
                        this.fluidId,
                        this.fluidAmountMb,
                        this.primaryOutput,
                        this.byproductOutput,
                        this.byproductChance,
                        this.processingTime
                ),
                null
        );
    }
}
