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

import dev.galacticraft.mod.recipe.ElectrolyticRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

public class ElectrolyticRecipeBuilder {
    private Optional<ResourceLocation> inputFluid = Optional.empty();
    private long inputFluidMb = 0L;
    private Ingredient solidInput = Ingredient.EMPTY;
    private final ResourceLocation primaryOutputFluid;
    private final long primaryOutputMb;
    private final ResourceLocation secondaryOutputFluid;
    private final long secondaryOutputMb;
    private Optional<ResourceLocation> traceGasFluid = Optional.empty();
    private long traceGasMb = 0L;
    private int processingTime = 20;
    private long energyPerTick = 120L;
    private String group = "";

    private ElectrolyticRecipeBuilder(
            ResourceLocation primaryOutputFluid,
            long primaryOutputMb,
            ResourceLocation secondaryOutputFluid,
            long secondaryOutputMb
    ) {
        this.primaryOutputFluid = primaryOutputFluid;
        this.primaryOutputMb = primaryOutputMb;
        this.secondaryOutputFluid = secondaryOutputFluid;
        this.secondaryOutputMb = secondaryOutputMb;
    }

    public static ElectrolyticRecipeBuilder fromFluid(
            Fluid inputFluid,
            long inputFluidMb,
            Fluid primaryOutputFluid,
            long primaryOutputMb,
            Fluid secondaryOutputFluid,
            long secondaryOutputMb
    ) {
        ElectrolyticRecipeBuilder builder = new ElectrolyticRecipeBuilder(
                BuiltInRegistries.FLUID.getKey(primaryOutputFluid),
                primaryOutputMb,
                BuiltInRegistries.FLUID.getKey(secondaryOutputFluid),
                secondaryOutputMb
        );
        builder.inputFluid = Optional.of(BuiltInRegistries.FLUID.getKey(inputFluid));
        builder.inputFluidMb = inputFluidMb;
        return builder;
    }

    public static ElectrolyticRecipeBuilder fromSolid(
            ItemLike solidInput,
            Fluid primaryOutputFluid,
            long primaryOutputMb,
            Fluid secondaryOutputFluid,
            long secondaryOutputMb
    ) {
        ElectrolyticRecipeBuilder builder = new ElectrolyticRecipeBuilder(
                BuiltInRegistries.FLUID.getKey(primaryOutputFluid),
                primaryOutputMb,
                BuiltInRegistries.FLUID.getKey(secondaryOutputFluid),
                secondaryOutputMb
        );
        builder.solidInput = Ingredient.of(solidInput);
        return builder;
    }

    public ElectrolyticRecipeBuilder traceGas(Fluid traceGas, long traceGasMb) {
        this.traceGasFluid = Optional.of(BuiltInRegistries.FLUID.getKey(traceGas));
        this.traceGasMb = traceGasMb;
        return this;
    }

    public ElectrolyticRecipeBuilder time(int processingTime) {
        this.processingTime = processingTime;
        return this;
    }

    public ElectrolyticRecipeBuilder energyPerTick(long energyPerTick) {
        this.energyPerTick = energyPerTick;
        return this;
    }

    public ElectrolyticRecipeBuilder group(String group) {
        this.group = group;
        return this;
    }

    public void save(RecipeOutput output, ResourceLocation id) {
        output.accept(
                id.withPrefix("electrolytic/"),
                new ElectrolyticRecipe(
                        this.group,
                        this.inputFluid,
                        this.inputFluidMb,
                        this.solidInput,
                        this.primaryOutputFluid,
                        this.primaryOutputMb,
                        this.secondaryOutputFluid,
                        this.secondaryOutputMb,
                        this.traceGasFluid,
                        this.traceGasMb,
                        this.processingTime,
                        this.energyPerTick
                ),
                null
        );
    }
}
