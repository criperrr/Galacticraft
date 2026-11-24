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
import dev.galacticraft.mod.content.block.entity.machine.VacuumMetallurgicInfuserBlockEntity;
import dev.galacticraft.mod.recipe.MetallurgicInfusingRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

public class MetallurgicInfusingEmiRecipe extends BasicEmiRecipe {
    private final String infusionType;
    private final int infusionAmount;
    private final int processingTime;

    public MetallurgicInfusingEmiRecipe(RecipeHolder<MetallurgicInfusingRecipe> holder) {
        super(GalacticraftEmiPlugin.METALLURGIC_INFUSING, holder.id(), 144, 44);
        MetallurgicInfusingRecipe recipe = holder.value();
        this.infusionType = recipe.getInfusionType();
        this.infusionAmount = recipe.getInfusionAmount();
        this.processingTime = recipe.getProcessingTime();
        this.inputs.add(EmiStack.of(VacuumMetallurgicInfuserBlockEntity.representativeAgentItem(this.infusionType)));
        this.inputs.add(EmiIngredient.of(recipe.getBaseIngredient()));
        this.outputs.add(EmiStack.of(recipe.getResult()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(this.inputs.get(0), 6, 8);
        widgets.addSlot(this.inputs.get(1), 36, 8);
        widgets.addFillingArrow(66, 9, this.processingTime * 50);
        widgets.addSlot(this.outputs.getFirst(), 106, 8).recipeContext(this);

        String agentName = VacuumMetallurgicInfuserBlockEntity.idToDisplayName(
                VacuumMetallurgicInfuserBlockEntity.typeToId(this.infusionType));
        widgets.addText(Component.literal(agentName + " " + this.infusionAmount + "u | 60 GJ/t"), 6, 32, 0xFF404040, false);
    }
}
