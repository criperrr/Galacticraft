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
import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.mod.compat.rei.common.GalacticraftREIServerPlugin;
import dev.galacticraft.mod.content.item.FluidCanisterItem;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.recipe.FusionReactorRecipe;
import dev.galacticraft.mod.recipe.ThermalDesorptionRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

public class Helium3ProcessDisplay extends BasicDisplay {
    private final CategoryIdentifier<Helium3ProcessDisplay> category;
    private final String summary;

    public Helium3ProcessDisplay(
            CategoryIdentifier<Helium3ProcessDisplay> category,
            List<EntryIngredient> inputs,
            List<EntryIngredient> outputs,
            String summary
    ) {
        super(inputs, outputs);
        this.category = category;
        this.summary = summary;
    }

    public static Helium3ProcessDisplay fromThermalDesorption(RecipeHolder<ThermalDesorptionRecipe> holder) {
        ThermalDesorptionRecipe recipe = holder.value();
        List<EntryIngredient> inputs = List.of(EntryIngredients.ofIngredient(recipe.getIngredient()));
        List<EntryIngredient> outputs = new ArrayList<>();
        outputs.add(gasAndCanister(Gases.SOLAR_VOLATILE_GAS, recipe.getGasAmountMb()));
        if (!recipe.getPrimaryByproduct().isEmpty()) {
            outputs.add(EntryIngredients.of(recipe.getPrimaryByproduct()));
        }
        if (!recipe.getSecondaryByproduct().isEmpty()) {
            outputs.add(EntryIngredients.of(recipe.getSecondaryByproduct()));
        }
        String summary = ">700°C | " + recipe.getGasAmountMb() + " mB SV | " + (recipe.getProcessingTime() / 20.0) + "s";
        return new Helium3ProcessDisplay(GalacticraftREIServerPlugin.THERMAL_DESORPTION, inputs, outputs, summary);
    }

    public static Helium3ProcessDisplay fromFusionReactor(RecipeHolder<FusionReactorRecipe> holder) {
        FusionReactorRecipe recipe = holder.value();
        List<EntryIngredient> inputs = List.of(
                gasAndCanister(recipe.getPrimaryGas(), recipe.getPrimaryGasMb()),
                gasAndCanister(recipe.getSecondaryGas(), recipe.getSecondaryGasMb()),
                EntryIngredients.of(GCItems.SUPERCONDUCTING_COIL),
                EntryIngredients.of(GCItems.HELIUM3_CELL)
        );
        List<EntryIngredient> outputs = List.of(
                EntryIngredients.of(GCItems.HELIUM3_FUSION_CELL)
        );
        String summary = "3x3x3 Torus | " + recipe.getEnergyPerTick() + "-" + (recipe.getEnergyPerTick() * 2) + " GJ/t";
        return new Helium3ProcessDisplay(GalacticraftREIServerPlugin.MHD_FUSION, inputs, outputs, summary);
    }

    public static EntryIngredient gasAndCanister(Fluid gas, long mb) {
        ItemStack filledCanister = FluidCanisterItem.getFilledCanister(GCItems.FLUID_CANISTER, gas);
        return EntryIngredient.builder()
                .add(EntryStacks.of(FluidStack.create(gas, mb * FluidStack.bucketAmount() / 1000L)))
                .add(EntryStacks.of(filledCanister))
                .build();
    }

    public String getSummary() {
        return this.summary;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return this.category;
    }
}
