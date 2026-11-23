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

import dev.galacticraft.mod.recipe.ThermalDesorptionRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class ThermalDesorptionRecipeBuilder {
    private final Ingredient ingredient;
    private final long gasAmountMb;
    private ItemStack primaryByproduct = ItemStack.EMPTY;
    private ItemStack secondaryByproduct = ItemStack.EMPTY;
    private int processingTime = 100;
    private String group = "";

    private ThermalDesorptionRecipeBuilder(Ingredient ingredient, long gasAmountMb) {
        this.ingredient = ingredient;
        this.gasAmountMb = gasAmountMb;
    }

    public static ThermalDesorptionRecipeBuilder create(ItemLike input, long gasAmountMb) {
        return new ThermalDesorptionRecipeBuilder(Ingredient.of(input), gasAmountMb);
    }

    public ThermalDesorptionRecipeBuilder primaryByproduct(ItemLike item, int count) {
        this.primaryByproduct = new ItemStack(item, count);
        return this;
    }

    public ThermalDesorptionRecipeBuilder secondaryByproduct(ItemLike item, int count) {
        this.secondaryByproduct = new ItemStack(item, count);
        return this;
    }

    public ThermalDesorptionRecipeBuilder time(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    public ThermalDesorptionRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(
                id.withPrefix("thermal_desorption/"),
                new ThermalDesorptionRecipe(
                        this.group,
                        this.ingredient,
                        this.gasAmountMb,
                        this.primaryByproduct,
                        this.secondaryByproduct,
                        this.processingTime
                ),
                null
        );
    }
}
