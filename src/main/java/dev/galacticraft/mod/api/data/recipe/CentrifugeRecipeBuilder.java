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

import dev.galacticraft.mod.recipe.CentrifugeRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

public class CentrifugeRecipeBuilder {
    private final Ingredient ingredient;
    private Optional<ResourceLocation> outputGas = Optional.empty();
    private long outputGasMb = 0L;
    private ItemStack outputItem = ItemStack.EMPTY;
    private int processingTime = 60;
    private String group = "";

    private CentrifugeRecipeBuilder(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public static CentrifugeRecipeBuilder create(ItemLike input) {
        return new CentrifugeRecipeBuilder(Ingredient.of(input));
    }

    public CentrifugeRecipeBuilder outputGas(Fluid gas, long amountMb) {
        this.outputGas = Optional.of(BuiltInRegistries.FLUID.getKey(gas));
        this.outputGasMb = amountMb;
        return this;
    }

    public CentrifugeRecipeBuilder outputItem(ItemLike item, int count) {
        this.outputItem = new ItemStack(item, count);
        return this;
    }

    public CentrifugeRecipeBuilder time(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    public CentrifugeRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(
                id.withPrefix("centrifuge/"),
                new CentrifugeRecipe(
                        this.group,
                        this.ingredient,
                        this.outputGas,
                        this.outputGasMb,
                        this.outputItem,
                        this.processingTime
                ),
                null
        );
    }
}
