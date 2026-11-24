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
import dev.galacticraft.mod.recipe.ElectrolyticRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class ElectrolyticEmiRecipe extends BasicEmiRecipe {
    private final boolean solidInput;
    private final long primaryMb;
    private final long secondaryMb;
    private final long traceMb;
    private final int processingTime;
    private final long energyPerTick;

    public ElectrolyticEmiRecipe(RecipeHolder<ElectrolyticRecipe> holder) {
        super(GalacticraftEmiPlugin.ELECTROLYTIC, holder.id(), 160, 56);
        ElectrolyticRecipe recipe = holder.value();
        this.solidInput = recipe.usesSolidInput();
        this.primaryMb = recipe.getPrimaryOutputMb();
        this.secondaryMb = recipe.getSecondaryOutputMb();
        this.traceMb = recipe.getTraceGasMb();
        this.processingTime = recipe.getProcessingTime();
        this.energyPerTick = recipe.getEnergyPerTick();

        if (this.solidInput) {
            this.inputs.add(EmiIngredient.of(recipe.getSolidInput()));
        } else if (recipe.getInputFluid() != null && recipe.getInputFluid() != Fluids.EMPTY) {
            this.inputs.add(EmiStack.of(recipe.getInputFluid(), recipe.getInputFluidDroplets()));
        }

        this.outputs.add(EmiStack.of(recipe.getPrimaryOutputFluid(), recipe.getPrimaryOutputDroplets()));
        this.outputs.add(EmiStack.of(recipe.getSecondaryOutputFluid(), recipe.getSecondaryOutputDroplets()));
        Fluid trace = recipe.getTraceGasFluid();
        if (trace != null && trace != Fluids.EMPTY && this.traceMb > 0L) {
            this.outputs.add(EmiStack.of(trace, recipe.getTraceGasDroplets()));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        if (!this.inputs.isEmpty()) {
            if (this.solidInput) {
                widgets.addSlot(this.inputs.get(0), 10, 12);
            } else {
                widgets.addTank(this.inputs.get(0), 10, 4, 18, 34, (int) Math.max(1000L, this.primaryMb * 81L));
            }
        }
        widgets.addFillingArrow(38, 13, this.processingTime * 50);
        widgets.addTank(this.outputs.get(0), 72, 4, 18, 34, (int) Math.max(1000L, this.primaryMb * 81L)).recipeContext(this);
        widgets.addTank(this.outputs.get(1), 96, 4, 18, 34, (int) Math.max(1000L, this.secondaryMb * 81L)).recipeContext(this);
        if (this.outputs.size() > 2) {
            widgets.addTank(this.outputs.get(2), 120, 4, 18, 34, (int) Math.max(1000L, this.traceMb * 81L)).recipeContext(this);
        }
        String label = this.primaryMb + "mB LH2 + " + this.secondaryMb + "mB LOX | " + this.energyPerTick + " GJ/t";
        widgets.addText(Component.literal(label), 6, 44, 0xFF404040, false);
    }
}
