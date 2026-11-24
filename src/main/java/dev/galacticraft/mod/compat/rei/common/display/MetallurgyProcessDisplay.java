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

package dev.galacticraft.mod.compat.rei.common.display;

import dev.architectury.fluid.FluidStack;
import dev.galacticraft.mod.compat.rei.common.GalacticraftREIServerPlugin;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.machine.VacuumMetallurgicInfuserBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.recipe.ChemicalLeachingRecipe;
import dev.galacticraft.mod.recipe.CompositeAutoclaveRecipe;
import dev.galacticraft.mod.recipe.MetallurgicInfusingRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

public class MetallurgyProcessDisplay extends BasicDisplay {
    private final CategoryIdentifier<MetallurgyProcessDisplay> category;
    private final String summary;

    public MetallurgyProcessDisplay(
            CategoryIdentifier<MetallurgyProcessDisplay> category,
            List<EntryIngredient> inputs,
            List<EntryIngredient> outputs,
            String summary
    ) {
        super(inputs, outputs);
        this.category = category;
        this.summary = summary;
    }

    public static MetallurgyProcessDisplay fromInfusing(RecipeHolder<MetallurgicInfusingRecipe> holder) {
        MetallurgicInfusingRecipe recipe = holder.value();
        EntryIngredient agent = EntryIngredients.of(VacuumMetallurgicInfuserBlockEntity.representativeAgentItem(recipe.getInfusionType()));
        EntryIngredient base = EntryIngredients.ofIngredient(recipe.getBaseIngredient());
        List<EntryIngredient> inputs = List.of(agent, base);
        List<EntryIngredient> outputs = List.of(EntryIngredients.of(recipe.getResult()));
        String agentLabel = VacuumMetallurgicInfuserBlockEntity.idToDisplayName(
                VacuumMetallurgicInfuserBlockEntity.typeToId(recipe.getInfusionType()));
        String summary = agentLabel + " " + recipe.getInfusionAmount() + " u | 60 GJ/t | " + (recipe.getProcessingTime() / 20.0) + "s";
        return new MetallurgyProcessDisplay(GalacticraftREIServerPlugin.METALLURGIC_INFUSING, inputs, outputs, summary);
    }

    public static MetallurgyProcessDisplay fromLeaching(RecipeHolder<ChemicalLeachingRecipe> holder) {
        ChemicalLeachingRecipe recipe = holder.value();
        Fluid fluid = recipe.getFluid();
        EntryIngredient.Builder fluidIng = EntryIngredient.builder()
                .add(EntryStacks.of(FluidStack.create(fluid, recipe.getFluidAmountMb() * FluidStack.bucketAmount() / 1000L)));
        if (fluid == GCFluids.SULFURIC_ACID) {
            fluidIng.add(EntryStacks.of(GCItems.SULFURIC_ACID_BUCKET));
        } else if (fluid == Fluids.WATER) {
            fluidIng.add(EntryStacks.of(Items.WATER_BUCKET));
        }
        List<EntryIngredient> inputs = List.of(fluidIng.build(), EntryIngredients.ofIngredient(recipe.getOreIngredient()));
        List<EntryIngredient> outputs = new ArrayList<>();
        outputs.add(EntryIngredients.of(recipe.getPrimaryOutput()));
        if (!recipe.getByproductOutput().isEmpty()) {
            outputs.add(EntryIngredients.of(recipe.getByproductOutput()));
        }
        String chanceText = recipe.getByproductOutput().isEmpty()
                ? ""
                : (" | +" + Math.round(recipe.getByproductChance() * 100.0F) + "%");
        String summary = recipe.getFluidAmountMb() + " mB | 90 GJ/t" + chanceText;
        return new MetallurgyProcessDisplay(GalacticraftREIServerPlugin.CHEMICAL_LEACHING, inputs, outputs, summary);
    }

    public static MetallurgyProcessDisplay fromAutoclave(RecipeHolder<CompositeAutoclaveRecipe> holder) {
        CompositeAutoclaveRecipe recipe = holder.value();
        List<EntryIngredient> inputs = new ArrayList<>();
        for (Ingredient ing : recipe.getIngredients()) {
            inputs.add(EntryIngredients.ofIngredient(ing));
        }
        List<EntryIngredient> outputs = List.of(EntryIngredients.of(recipe.getResult()));
        String summary = recipe.getTargetTemperature() + "°C | " + recipe.getTargetPressureBar() + " bar | 80 GJ/t";
        return new MetallurgyProcessDisplay(GalacticraftREIServerPlugin.COMPOSITE_AUTOCLAVE, inputs, outputs, summary);
    }

    public String getSummary() {
        return this.summary;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return this.category;
    }
}
