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

import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.recipe.TelemetryAnalysisRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TelemetryAnalysisRecipeBuilder {
    private final Ingredient sample;
    private final Ingredient reagent;
    private Optional<ResourceLocation> targetPlanet = Optional.empty();
    private int bonusSp = 15;
    private int bonusDomainPoints = 12;
    private ItemStack outputItem = new ItemStack(GCItems.BLANK_DATA_DRIVE);
    private int processingTime = 80;
    private String group = "";

    private TelemetryAnalysisRecipeBuilder(Ingredient sample, Ingredient reagent) {
        this.sample = sample;
        this.reagent = reagent;
    }

    public static TelemetryAnalysisRecipeBuilder create(ItemLike sample, ItemLike reagent) {
        return new TelemetryAnalysisRecipeBuilder(Ingredient.of(sample), Ingredient.of(reagent));
    }

    public static TelemetryAnalysisRecipeBuilder create(Ingredient sample, Ingredient reagent) {
        return new TelemetryAnalysisRecipeBuilder(sample, reagent);
    }

    public TelemetryAnalysisRecipeBuilder targetPlanet(@NotNull ResourceLocation planetId) {
        this.targetPlanet = Optional.of(planetId);
        return this;
    }

    public TelemetryAnalysisRecipeBuilder bonus(int sp, int domainPoints) {
        this.bonusSp = sp;
        this.bonusDomainPoints = domainPoints;
        return this;
    }

    public TelemetryAnalysisRecipeBuilder outputItem(ItemLike item, int count) {
        this.outputItem = new ItemStack(item, count);
        return this;
    }

    public TelemetryAnalysisRecipeBuilder time(int ticks) {
        this.processingTime = ticks;
        return this;
    }

    public TelemetryAnalysisRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(
                id.withPrefix("telemetry_analysis/"),
                new TelemetryAnalysisRecipe(
                        this.group,
                        this.sample,
                        this.reagent,
                        this.targetPlanet,
                        this.bonusSp,
                        this.bonusDomainPoints,
                        this.outputItem,
                        this.processingTime
                ),
                null
        );
    }
}
