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

import dev.galacticraft.mod.recipe.SabatierRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

public class SabatierRecipeBuilder {
    private final ResourceLocation reagentA;
    private final long reagentAMb;
    private Optional<ResourceLocation> reagentB = Optional.empty();
    private long reagentBMb = 0L;
    private Ingredient solidReagent = Ingredient.EMPTY;
    private Ingredient catalyst = Ingredient.EMPTY;
    private final ResourceLocation productA;
    private final long productAMb;
    private Optional<ResourceLocation> productB = Optional.empty();
    private long productBMb = 0L;
    private int targetTempC = 350;
    private int processingTime = 40;
    private long energyPerTick = 110L;
    private String formulaLabel = "";
    private String group = "";

    private SabatierRecipeBuilder(ResourceLocation reagentA, long reagentAMb, ResourceLocation productA, long productAMb) {
        this.reagentA = reagentA;
        this.reagentAMb = reagentAMb;
        this.productA = productA;
        this.productAMb = productAMb;
    }

    public static SabatierRecipeBuilder create(Fluid reagentA, long reagentAMb, Fluid productA, long productAMb) {
        return new SabatierRecipeBuilder(
                BuiltInRegistries.FLUID.getKey(reagentA),
                reagentAMb,
                BuiltInRegistries.FLUID.getKey(productA),
                productAMb
        );
    }

    public SabatierRecipeBuilder reagentB(Fluid fluid, long amountMb) {
        this.reagentB = Optional.of(BuiltInRegistries.FLUID.getKey(fluid));
        this.reagentBMb = amountMb;
        return this;
    }

    public SabatierRecipeBuilder solidReagent(ItemLike item) {
        this.solidReagent = Ingredient.of(item);
        return this;
    }

    public SabatierRecipeBuilder catalyst(ItemLike... items) {
        this.catalyst = Ingredient.of(items);
        return this;
    }

    public SabatierRecipeBuilder productB(Fluid fluid, long amountMb) {
        this.productB = Optional.of(BuiltInRegistries.FLUID.getKey(fluid));
        this.productBMb = amountMb;
        return this;
    }

    public SabatierRecipeBuilder targetTemp(int targetTempC) {
        this.targetTempC = targetTempC;
        return this;
    }

    public SabatierRecipeBuilder time(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    public SabatierRecipeBuilder energyPerTick(long energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    public SabatierRecipeBuilder formula(String formulaLabel) {
        this.formulaLabel = formulaLabel;
        return this;
    }

    public SabatierRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(
                id.withPrefix("sabatier/"),
                new SabatierRecipe(
                        this.group,
                        this.reagentA,
                        this.reagentAMb,
                        this.reagentB,
                        this.reagentBMb,
                        this.solidReagent,
                        this.catalyst,
                        this.productA,
                        this.productAMb,
                        this.productB,
                        this.productBMb,
                        this.targetTempC,
                        this.processingTime,
                        this.energyPerTick,
                        this.formulaLabel
                ),
                null
        );
    }
}
