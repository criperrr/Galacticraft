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

package dev.galacticraft.mod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.mod.util.FluidUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Data-driven recipe for the Electrolytic Separator.
 * Supports either a liquid feedstock (e.g., Water) or a solid cryo-ice feedstock (e.g., Dense Ice),
 * outputting stoichiometric cathode/anode propellants (2x LH2 + 1x LOX) and optional heavy-isotope trace gas (Deuterium).
 */
public class ElectrolyticRecipe implements Recipe<ElectrolyticRecipe.Input> {
    private final String group;
    private final Optional<ResourceLocation> inputFluid;
    private final long inputFluidMb;
    private final Ingredient solidInput;
    private final ResourceLocation primaryOutputFluid;
    private final long primaryOutputMb;
    private final ResourceLocation secondaryOutputFluid;
    private final long secondaryOutputMb;
    private final Optional<ResourceLocation> traceGasFluid;
    private final long traceGasMb;
    private final int processingTime;
    private final long energyPerTick;

    public ElectrolyticRecipe(
            String group,
            Optional<ResourceLocation> inputFluid,
            long inputFluidMb,
            Ingredient solidInput,
            ResourceLocation primaryOutputFluid,
            long primaryOutputMb,
            ResourceLocation secondaryOutputFluid,
            long secondaryOutputMb,
            Optional<ResourceLocation> traceGasFluid,
            long traceGasMb,
            int processingTime,
            long energyPerTick
    ) {
        this.group = group;
        this.inputFluid = inputFluid;
        this.inputFluidMb = inputFluidMb;
        this.solidInput = solidInput;
        this.primaryOutputFluid = primaryOutputFluid;
        this.primaryOutputMb = primaryOutputMb;
        this.secondaryOutputFluid = secondaryOutputFluid;
        this.secondaryOutputMb = secondaryOutputMb;
        this.traceGasFluid = traceGasFluid;
        this.traceGasMb = traceGasMb;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (!this.solidInput.isEmpty()) {
            return !input.solidStack().isEmpty() && this.solidInput.test(input.solidStack());
        }
        if (this.inputFluid.isPresent() && this.inputFluidMb > 0L) {
            Fluid expected = BuiltInRegistries.FLUID.get(this.inputFluid.get());
            return input.fluid() != Fluids.EMPTY
                    && input.fluid() == expected
                    && input.fluidDroplets() >= this.getInputFluidDroplets();
        }
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        if (!this.solidInput.isEmpty()) {
            list.add(this.solidInput);
        }
        return list;
    }

    public boolean usesSolidInput() {
        return !this.solidInput.isEmpty();
    }

    public Optional<ResourceLocation> getInputFluidId() {
        return this.inputFluid;
    }

    public @Nullable Fluid getInputFluid() {
        return this.inputFluid.map(BuiltInRegistries.FLUID::get).orElse(null);
    }

    public long getInputFluidMb() {
        return this.inputFluidMb;
    }

    public long getInputFluidDroplets() {
        return this.inputFluidMb * FluidUtil.MILLIBUCKET;
    }

    public Ingredient getSolidInput() {
        return this.solidInput;
    }

    public ResourceLocation getPrimaryOutputFluidId() {
        return this.primaryOutputFluid;
    }

    public Fluid getPrimaryOutputFluid() {
        return BuiltInRegistries.FLUID.get(this.primaryOutputFluid);
    }

    public long getPrimaryOutputMb() {
        return this.primaryOutputMb;
    }

    public long getPrimaryOutputDroplets() {
        return this.primaryOutputMb * FluidUtil.MILLIBUCKET;
    }

    public ResourceLocation getSecondaryOutputFluidId() {
        return this.secondaryOutputFluid;
    }

    public Fluid getSecondaryOutputFluid() {
        return BuiltInRegistries.FLUID.get(this.secondaryOutputFluid);
    }

    public long getSecondaryOutputMb() {
        return this.secondaryOutputMb;
    }

    public long getSecondaryOutputDroplets() {
        return this.secondaryOutputMb * FluidUtil.MILLIBUCKET;
    }

    public Optional<ResourceLocation> getTraceGasFluidId() {
        return this.traceGasFluid;
    }

    public @Nullable Fluid getTraceGasFluid() {
        return this.traceGasFluid.map(BuiltInRegistries.FLUID::get).orElse(null);
    }

    public long getTraceGasMb() {
        return this.traceGasMb;
    }

    public long getTraceGasDroplets() {
        return this.traceGasMb * FluidUtil.MILLIBUCKET;
    }

    public int getProcessingTime() {
        return this.processingTime;
    }

    public long getEnergyPerTick() {
        return this.energyPerTick;
    }

    @Override
    public @NotNull String getGroup() {
        return this.group;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return GCRecipes.ELECTROLYTIC_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.ELECTROLYTIC_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(Fluid fluid, long fluidDroplets, ItemStack solidStack) implements RecipeInput {
        @Override
        public @NotNull ItemStack getItem(int index) {
            return index == 0 ? this.solidStack : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return this.solidStack.isEmpty() && (this.fluid == Fluids.EMPTY || this.fluidDroplets <= 0L);
        }
    }

    public static class Serializer implements RecipeSerializer<ElectrolyticRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<ElectrolyticRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                ResourceLocation.CODEC.optionalFieldOf("input_fluid").forGetter(r -> r.inputFluid),
                Codec.LONG.optionalFieldOf("input_fluid_mb", 0L).forGetter(r -> r.inputFluidMb),
                Ingredient.CODEC.optionalFieldOf("solid_input", Ingredient.EMPTY).forGetter(r -> r.solidInput),
                ResourceLocation.CODEC.fieldOf("primary_output_fluid").forGetter(r -> r.primaryOutputFluid),
                Codec.LONG.fieldOf("primary_output_mb").forGetter(r -> r.primaryOutputMb),
                ResourceLocation.CODEC.fieldOf("secondary_output_fluid").forGetter(r -> r.secondaryOutputFluid),
                Codec.LONG.fieldOf("secondary_output_mb").forGetter(r -> r.secondaryOutputMb),
                ResourceLocation.CODEC.optionalFieldOf("trace_gas_fluid").forGetter(r -> r.traceGasFluid),
                Codec.LONG.optionalFieldOf("trace_gas_mb", 0L).forGetter(r -> r.traceGasMb),
                Codec.INT.optionalFieldOf("processing_time", 20).forGetter(r -> r.processingTime),
                Codec.LONG.optionalFieldOf("energy_per_tick", 120L).forGetter(r -> r.energyPerTick)
        ).apply(instance, ElectrolyticRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ElectrolyticRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public @NotNull ElectrolyticRecipe decode(RegistryFriendlyByteBuf buf) {
                String group = ByteBufCodecs.STRING_UTF8.decode(buf);
                Optional<ResourceLocation> inputFluid = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf);
                long inputFluidMb = ByteBufCodecs.VAR_LONG.decode(buf);
                Ingredient solidInput = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                ResourceLocation primaryFluid = ResourceLocation.STREAM_CODEC.decode(buf);
                long primaryMb = ByteBufCodecs.VAR_LONG.decode(buf);
                ResourceLocation secondaryFluid = ResourceLocation.STREAM_CODEC.decode(buf);
                long secondaryMb = ByteBufCodecs.VAR_LONG.decode(buf);
                Optional<ResourceLocation> traceGas = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf);
                long traceGasMb = ByteBufCodecs.VAR_LONG.decode(buf);
                int processingTime = ByteBufCodecs.VAR_INT.decode(buf);
                long energyPerTick = ByteBufCodecs.VAR_LONG.decode(buf);
                return new ElectrolyticRecipe(
                        group, inputFluid, inputFluidMb, solidInput,
                        primaryFluid, primaryMb, secondaryFluid, secondaryMb,
                        traceGas, traceGasMb, processingTime, energyPerTick
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ElectrolyticRecipe r) {
                ByteBufCodecs.STRING_UTF8.encode(buf, r.group);
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, r.inputFluid);
                ByteBufCodecs.VAR_LONG.encode(buf, r.inputFluidMb);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.solidInput);
                ResourceLocation.STREAM_CODEC.encode(buf, r.primaryOutputFluid);
                ByteBufCodecs.VAR_LONG.encode(buf, r.primaryOutputMb);
                ResourceLocation.STREAM_CODEC.encode(buf, r.secondaryOutputFluid);
                ByteBufCodecs.VAR_LONG.encode(buf, r.secondaryOutputMb);
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, r.traceGasFluid);
                ByteBufCodecs.VAR_LONG.encode(buf, r.traceGasMb);
                ByteBufCodecs.VAR_INT.encode(buf, r.processingTime);
                ByteBufCodecs.VAR_LONG.encode(buf, r.energyPerTick);
            }
        };

        @Override
        public @NotNull MapCodec<ElectrolyticRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ElectrolyticRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
