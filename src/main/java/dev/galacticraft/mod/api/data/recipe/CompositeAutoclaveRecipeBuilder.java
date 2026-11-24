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

import dev.galacticraft.mod.recipe.CompositeAutoclaveRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

public class CompositeAutoclaveRecipeBuilder {
    private final List<Ingredient> ingredients = new ArrayList<>(4);
    private final ItemStack result;
    private int targetTemperature = 450;
    private int targetPressureBar = 12;
    private int processingTime = 120;
    private String group = "";

    private CompositeAutoclaveRecipeBuilder(ItemStack result) {
        this.result = result;
    }

    public static CompositeAutoclaveRecipeBuilder create(ItemLike result, int count) {
        return new CompositeAutoclaveRecipeBuilder(new ItemStack(result, count));
    }

    public CompositeAutoclaveRecipeBuilder requires(ItemLike item) {
        this.ingredients.add(Ingredient.of(item));
        return this;
    }

    public CompositeAutoclaveRecipeBuilder requires(TagKey<Item> tag) {
        this.ingredients.add(Ingredient.of(tag));
        return this;
    }

    public CompositeAutoclaveRecipeBuilder temperature(int targetTemperature) {
        this.targetTemperature = targetTemperature;
        return this;
    }

    public CompositeAutoclaveRecipeBuilder pressure(int targetPressureBar) {
        this.targetPressureBar = targetPressureBar;
        return this;
    }

    public CompositeAutoclaveRecipeBuilder time(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    public CompositeAutoclaveRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(
                id.withPrefix("composite_autoclave/"),
                new CompositeAutoclaveRecipe(
                        this.group,
                        List.copyOf(this.ingredients),
                        this.result,
                        this.targetTemperature,
                        this.targetPressureBar,
                        this.processingTime
                ),
                null
        );
    }
}
