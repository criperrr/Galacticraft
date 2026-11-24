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

import dev.galacticraft.mod.recipe.MetallurgicInfusingRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class MetallurgicInfusingRecipeBuilder {
    private final Ingredient baseIngredient;
    private final String infusionType;
    private final int infusionAmount;
    private final ItemStack result;
    private int processingTime = 100;
    private String group = "";

    private MetallurgicInfusingRecipeBuilder(Ingredient baseIngredient, String infusionType, int infusionAmount, ItemStack result) {
        this.baseIngredient = baseIngredient;
        this.infusionType = infusionType;
        this.infusionAmount = infusionAmount;
        this.result = result;
    }

    public static MetallurgicInfusingRecipeBuilder create(ItemLike baseMetal, String infusionType, int infusionAmount, ItemLike result, int count) {
        return new MetallurgicInfusingRecipeBuilder(Ingredient.of(baseMetal), infusionType, infusionAmount, new ItemStack(result, count));
    }

    public static MetallurgicInfusingRecipeBuilder create(TagKey<Item> baseMetalTag, String infusionType, int infusionAmount, ItemLike result, int count) {
        return new MetallurgicInfusingRecipeBuilder(Ingredient.of(baseMetalTag), infusionType, infusionAmount, new ItemStack(result, count));
    }

    public MetallurgicInfusingRecipeBuilder time(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    public MetallurgicInfusingRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(
                id.withPrefix("metallurgic_infusing/"),
                new MetallurgicInfusingRecipe(
                        this.group,
                        this.baseIngredient,
                        this.infusionType,
                        this.infusionAmount,
                        this.result,
                        this.processingTime
                ),
                null
        );
    }
}
