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
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.recipe.CentrifugeRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class CentrifugeEmiRecipe extends BasicEmiRecipe {
    private final boolean hasGasOutput;
    private final long gasMb;
    private final int processingTime;

    public CentrifugeEmiRecipe(RecipeHolder<CentrifugeRecipe> holder) {
        super(GalacticraftEmiPlugin.CENTRIFUGE, holder.id(), 150, 54);
        CentrifugeRecipe recipe = holder.value();
        this.processingTime = recipe.getProcessingTime();
        this.inputs.add(EmiIngredient.of(recipe.getIngredient()));
        this.catalysts.add(EmiStack.of(GCItems.SUPERCONDUCTING_COIL));

        Fluid gas = recipe.getOutputGas();
        this.hasGasOutput = gas != null && gas != Fluids.EMPTY && recipe.getOutputGasMb() > 0L;
        this.gasMb = recipe.getOutputGasMb();
        if (this.hasGasOutput) {
            this.outputs.add(EmiStack.of(gas, recipe.getOutputGasDroplets()));
        }
        if (!recipe.getOutputItem().isEmpty()) {
            this.outputs.add(EmiStack.of(recipe.getOutputItem()));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(this.inputs.get(0), 8, 12);
        widgets.addSlot(this.catalysts.get(0), 30, 12).catalyst(true);
        widgets.addFillingArrow(56, 13, this.processingTime * 50);
        int x = 90;
        if (this.hasGasOutput) {
            widgets.addTank(this.outputs.get(0), x, 4, 18, 34, (int) Math.max(1000L, this.gasMb * 81L)).recipeContext(this);
            x += 24;
            if (this.outputs.size() > 1) {
                widgets.addSlot(this.outputs.get(1), x, 12).recipeContext(this);
            }
        } else if (!this.outputs.isEmpty()) {
            widgets.addSlot(this.outputs.get(0), x, 12).recipeContext(this);
        }
        String label = this.hasGasOutput ? ("40k RPM | " + this.gasMb + " mB Gas") : "40k RPM | Magnetic Separation";
        widgets.addText(Component.literal(label), 8, 42, 0xFF404040, false);
    }
}
