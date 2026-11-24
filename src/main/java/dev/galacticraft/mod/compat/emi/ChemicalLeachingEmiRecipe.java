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

package dev.galacticraft.mod.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.galacticraft.mod.recipe.ChemicalLeachingRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ChemicalLeachingEmiRecipe extends BasicEmiRecipe {
    private final long fluidAmountMb;
    private final float byproductChance;
    private final int processingTime;

    public ChemicalLeachingEmiRecipe(RecipeHolder<ChemicalLeachingRecipe> holder) {
        super(GalacticraftEmiPlugin.CHEMICAL_LEACHING, holder.id(), 152, 52);
        ChemicalLeachingRecipe recipe = holder.value();
        this.fluidAmountMb = recipe.getFluidAmountMb();
        this.byproductChance = recipe.getByproductChance();
        this.processingTime = recipe.getProcessingTime();
        this.inputs.add(EmiStack.of(recipe.getFluid(), recipe.getFluidAmountDroplets()));
        this.inputs.add(EmiIngredient.of(recipe.getOreIngredient()));
        this.outputs.add(EmiStack.of(recipe.getPrimaryOutput()));
        if (!recipe.getByproductOutput().isEmpty()) {
            this.outputs.add(EmiStack.of(recipe.getByproductOutput()).setChance(this.byproductChance));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTank(this.inputs.get(0), 6, 4, 18, 34, (int) Math.max(1000L, this.fluidAmountMb * 81L));
        widgets.addSlot(this.inputs.get(1), 32, 12);
        widgets.addFillingArrow(60, 13, this.processingTime * 50);
        widgets.addSlot(this.outputs.get(0), 96, 12).recipeContext(this);
        if (this.outputs.size() > 1) {
            widgets.addSlot(this.outputs.get(1), 118, 12).recipeContext(this);
        }
        widgets.addText(Component.literal(this.fluidAmountMb + " mB | 90 GJ/t"), 6, 41, 0xFF404040, false);
    }
}
