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

import dev.galacticraft.mod.recipe.FusionReactorRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class FusionReactorRecipeBuilder {
    private final ResourceLocation primaryGas;
    private final long primaryGasMb;
    private final ResourceLocation secondaryGas;
    private final long secondaryGasMb;
    private final long energyPerTick;
    private int cycleTicks = 40;
    private ItemStack outputCell = ItemStack.EMPTY;
    private String group = "";

    private FusionReactorRecipeBuilder(
            ResourceLocation primaryGas,
            long primaryGasMb,
            ResourceLocation secondaryGas,
            long secondaryGasMb,
            long energyPerTick
    ) {
        this.primaryGas = primaryGas;
        this.primaryGasMb = primaryGasMb;
        this.secondaryGas = secondaryGas;
        this.secondaryGasMb = secondaryGasMb;
        this.energyPerTick = energyPerTick;
    }

    public static FusionReactorRecipeBuilder create(
            ResourceLocation primaryGas,
            long primaryGasMb,
            ResourceLocation secondaryGas,
            long secondaryGasMb,
            long energyPerTick
    ) {
        return new FusionReactorRecipeBuilder(primaryGas, primaryGasMb, secondaryGas, secondaryGasMb, energyPerTick);
    }

    public FusionReactorRecipeBuilder cycleTicks(int cycleTicks) {
        this.cycleTicks = cycleTicks;
        return this;
    }

    public FusionReactorRecipeBuilder outputCell(ItemLike item) {
        this.outputCell = new ItemStack(item);
        return this;
    }

    public FusionReactorRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(
                id.withPrefix("fusion_reactor/"),
                new FusionReactorRecipe(
                        this.group,
                        this.primaryGas,
                        this.primaryGasMb,
                        this.secondaryGas,
                        this.secondaryGasMb,
                        this.energyPerTick,
                        this.cycleTicks,
                        this.outputCell
                ),
                null
        );
    }
}
