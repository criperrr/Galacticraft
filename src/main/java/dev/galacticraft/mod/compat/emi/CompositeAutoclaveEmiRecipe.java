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
import dev.galacticraft.mod.recipe.CompositeAutoclaveRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

public class CompositeAutoclaveEmiRecipe extends BasicEmiRecipe {
    private final int targetTemperature;
    private final int targetPressureBar;
    private final int processingTime;

    public CompositeAutoclaveEmiRecipe(RecipeHolder<CompositeAutoclaveRecipe> holder) {
        super(GalacticraftEmiPlugin.COMPOSITE_AUTOCLAVE_CATEGORY, holder.id(), 144, 54);
        CompositeAutoclaveRecipe recipe = holder.value();
        this.targetTemperature = recipe.getTargetTemperature();
        this.targetPressureBar = recipe.getTargetPressureBar();
        this.processingTime = recipe.getProcessingTime();
        for (Ingredient ingredient : recipe.getIngredients()) {
            this.inputs.add(EmiIngredient.of(ingredient));
        }
        this.outputs.add(EmiStack.of(recipe.getResult()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < 4; i++) {
            int x = 8 + (i % 2) * 18;
            int y = 4 + (i / 2) * 18;
            if (i < this.inputs.size()) {
                widgets.addSlot(this.inputs.get(i), x, y);
            } else {
                widgets.addSlot(EmiStack.EMPTY, x, y);
            }
        }
        widgets.addFillingArrow(58, 14, this.processingTime * 50);
        widgets.addSlot(this.outputs.getFirst(), 98, 13).recipeContext(this);
        widgets.addText(Component.literal(this.targetTemperature + "°C | " + this.targetPressureBar + " bar | 80 GJ/t"),
                6, 43, 0xFF404040, false);
    }
}
