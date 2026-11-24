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
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.recipe.CentrifugeRecipe;
import dev.galacticraft.mod.recipe.ElectrolyticRecipe;
import dev.galacticraft.mod.recipe.SabatierRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

public class IsruProcessDisplay extends BasicDisplay {
    private final CategoryIdentifier<IsruProcessDisplay> category;
    private final String summary;
    private final boolean hasCatalyst;

    public IsruProcessDisplay(
            CategoryIdentifier<IsruProcessDisplay> category,
            List<EntryIngredient> inputs,
            List<EntryIngredient> outputs,
            String summary,
            boolean hasCatalyst
    ) {
        super(inputs, outputs);
        this.category = category;
        this.summary = summary;
        this.hasCatalyst = hasCatalyst;
    }

    public static EntryIngredient pureFluid(Fluid fluid, long mb) {
        long archAmount = Math.max(1L, mb * FluidStack.bucketAmount() / 1000L);
        return EntryIngredient.of(EntryStacks.of(FluidStack.create(fluid, archAmount)));
    }

    public static IsruProcessDisplay fromElectrolytic(RecipeHolder<ElectrolyticRecipe> holder) {
        ElectrolyticRecipe recipe = holder.value();
        List<EntryIngredient> inputs = new ArrayList<>();
        if (recipe.usesSolidInput()) {
            inputs.add(EntryIngredients.ofIngredient(recipe.getSolidInput()));
        } else if (recipe.getInputFluid() != null && recipe.getInputFluid() != Fluids.EMPTY) {
            inputs.add(pureFluid(recipe.getInputFluid(), recipe.getInputFluidMb()));
        }

        List<EntryIngredient> outputs = new ArrayList<>();
        outputs.add(pureFluid(recipe.getPrimaryOutputFluid(), recipe.getPrimaryOutputMb()));
        outputs.add(pureFluid(recipe.getSecondaryOutputFluid(), recipe.getSecondaryOutputMb()));
        Fluid trace = recipe.getTraceGasFluid();
        if (trace != null && trace != Fluids.EMPTY && recipe.getTraceGasMb() > 0L) {
            outputs.add(pureFluid(trace, recipe.getTraceGasMb()));
        }

        String summary = recipe.getPrimaryOutputMb() + "mB LH2 + " + recipe.getSecondaryOutputMb() + "mB LOX"
                + (recipe.getTraceGasMb() > 0L ? " + " + recipe.getTraceGasMb() + "mB D2" : "");
        return new IsruProcessDisplay(GalacticraftREIServerPlugin.ELECTROLYTIC, inputs, outputs, summary, false);
    }

    public static IsruProcessDisplay fromSabatier(RecipeHolder<SabatierRecipe> holder) {
        SabatierRecipe recipe = holder.value();
        List<EntryIngredient> inputs = new ArrayList<>();
        inputs.add(pureFluid(recipe.getReagentA(), recipe.getReagentAMb()));
        Fluid reagentB = recipe.getReagentB();
        if (reagentB != null && reagentB != Fluids.EMPTY && recipe.getReagentBMb() > 0L) {
            inputs.add(pureFluid(reagentB, recipe.getReagentBMb()));
        } else if (!recipe.getSolidReagent().isEmpty()) {
            inputs.add(EntryIngredients.ofIngredient(recipe.getSolidReagent()));
        }
        boolean catalystPresent = !recipe.getCatalyst().isEmpty();
        if (catalystPresent) {
            inputs.add(EntryIngredients.ofIngredient(recipe.getCatalyst()));
        }

        List<EntryIngredient> outputs = new ArrayList<>();
        outputs.add(pureFluid(recipe.getProductA(), recipe.getProductAMb()));
        Fluid productB = recipe.getProductB();
        if (productB != null && productB != Fluids.EMPTY && recipe.getProductBMb() > 0L) {
            outputs.add(pureFluid(productB, recipe.getProductBMb()));
        }

        String label = recipe.getFormulaLabel().isBlank()
                ? (recipe.getTargetTempC() + "°C | " + recipe.getEnergyPerTick() + " GJ/t")
                : (recipe.getFormulaLabel().replace(" -> ", "->") + " (" + recipe.getTargetTempC() + "°C)");
        return new IsruProcessDisplay(GalacticraftREIServerPlugin.SABATIER, inputs, outputs, label, catalystPresent);
    }

    public static Helium3ProcessDisplay toCentrifugeDisplay(RecipeHolder<CentrifugeRecipe> holder) {
        CentrifugeRecipe recipe = holder.value();
        List<EntryIngredient> inputs = List.of(
                EntryIngredients.ofIngredient(recipe.getIngredient()),
                EntryIngredients.of(GCItems.SUPERCONDUCTING_COIL)
        );
        List<EntryIngredient> outputs = new ArrayList<>();
        Fluid gas = recipe.getOutputGas();
        if (gas != null && gas != Fluids.EMPTY && recipe.getOutputGasMb() > 0L) {
            outputs.add(pureFluid(gas, recipe.getOutputGasMb()));
        }
        if (!recipe.getOutputItem().isEmpty()) {
            outputs.add(EntryIngredients.of(recipe.getOutputItem()));
        }
        String summary;
        if (gas != null && gas != Fluids.EMPTY && recipe.getOutputGasMb() > 0L) {
            summary = "40k RPM | " + recipe.getOutputGasMb() + "mB Gas";
        } else {
            summary = "40k RPM | " + recipe.getOutputItem().getCount() + "x " + recipe.getOutputItem().getHoverName().getString();
        }
        return new Helium3ProcessDisplay(GalacticraftREIServerPlugin.ISOTOPIC_SEPARATION, inputs, outputs, summary);
    }

    public String getSummary() {
        return this.summary;
    }

    public boolean hasCatalyst() {
        return this.hasCatalyst;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return this.category;
    }
}
