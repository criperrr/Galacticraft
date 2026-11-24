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
import dev.galacticraft.mod.recipe.SabatierRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class SabatierEmiRecipe extends BasicEmiRecipe {
    private final boolean hasFluidB;
    private final boolean hasSolid;
    private final int targetTempC;
    private final int processingTime;
    private final String formula;

    public SabatierEmiRecipe(RecipeHolder<SabatierRecipe> holder) {
        super(GalacticraftEmiPlugin.SABATIER, holder.id(), 166, 56);
        SabatierRecipe recipe = holder.value();
        this.targetTempC = recipe.getTargetTempC();
        this.processingTime = recipe.getProcessingTime();
        this.formula = recipe.getFormulaLabel().isBlank()
                ? (this.targetTempC + "°C | " + recipe.getEnergyPerTick() + " GJ/t")
                : (recipe.getFormulaLabel() + " (" + this.targetTempC + "°C)");

        this.inputs.add(EmiStack.of(recipe.getReagentA(), recipe.getReagentADroplets()));
        Fluid reagentB = recipe.getReagentB();
        this.hasFluidB = reagentB != null && reagentB != Fluids.EMPTY && recipe.getReagentBMb() > 0L;
        if (this.hasFluidB) {
            this.inputs.add(EmiStack.of(reagentB, recipe.getReagentBDroplets()));
        }
        this.hasSolid = !recipe.getSolidReagent().isEmpty();
        if (this.hasSolid) {
            this.inputs.add(EmiIngredient.of(recipe.getSolidReagent()));
        }
        if (!recipe.getCatalyst().isEmpty()) {
            this.catalysts.add(EmiIngredient.of(recipe.getCatalyst()));
        }

        this.outputs.add(EmiStack.of(recipe.getProductA(), recipe.getProductADroplets()));
        Fluid productB = recipe.getProductB();
        if (productB != null && productB != Fluids.EMPTY && recipe.getProductBMb() > 0L) {
            this.outputs.add(EmiStack.of(productB, recipe.getProductBDroplets()));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 4;
        widgets.addTank(this.inputs.get(0), x, 4, 18, 34, 81_000);
        x += 20;
        if (this.hasFluidB) {
            widgets.addTank(this.inputs.get(1), x, 4, 18, 34, 81_000);
            x += 20;
        }
        if (this.hasSolid) {
            int idx = this.hasFluidB ? 2 : 1;
            widgets.addSlot(this.inputs.get(idx), x, 12);
            x += 20;
        }
        if (!this.catalysts.isEmpty()) {
            widgets.addSlot(this.catalysts.get(0), x, 12).catalyst(true);
            x += 22;
        }
        widgets.addFillingArrow(x, 13, this.processingTime * 50);
        x += 30;
        widgets.addTank(this.outputs.get(0), x, 4, 18, 34, 81_000).recipeContext(this);
        if (this.outputs.size() > 1) {
            widgets.addTank(this.outputs.get(1), x + 22, 4, 18, 34, 81_000).recipeContext(this);
        }
        widgets.addText(Component.literal(this.formula), 4, 44, 0xFF404040, false);
    }
}
