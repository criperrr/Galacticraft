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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public class FusionReactorRecipe implements Recipe<RecipeInput> {
    private final String group;
    private final ResourceLocation primaryGas;
    private final long primaryGasMb;
    private final ResourceLocation secondaryGas;
    private final long secondaryGasMb;
    private final long energyPerTick;
    private final int cycleTicks;
    private final ItemStack outputCell;

    public FusionReactorRecipe(
            String group,
            ResourceLocation primaryGas,
            long primaryGasMb,
            ResourceLocation secondaryGas,
            long secondaryGasMb,
            long energyPerTick,
            int cycleTicks,
            ItemStack outputCell
    ) {
        this.group = group;
        this.primaryGas = primaryGas;
        this.primaryGasMb = primaryGasMb;
        this.secondaryGas = secondaryGas;
        this.secondaryGasMb = secondaryGasMb;
        this.energyPerTick = energyPerTick;
        this.cycleTicks = cycleTicks;
        this.outputCell = outputCell;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return this.outputCell.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.outputCell;
    }

    public ResourceLocation getPrimaryGasId() {
        return this.primaryGas;
    }

    public Fluid getPrimaryGas() {
        return BuiltInRegistries.FLUID.get(this.primaryGas);
    }

    public long getPrimaryGasMb() {
        return this.primaryGasMb;
    }

    public long getPrimaryGasDroplets() {
        return this.primaryGasMb * FluidUtil.MILLIBUCKET;
    }

    public ResourceLocation getSecondaryGasId() {
        return this.secondaryGas;
    }

    public Fluid getSecondaryGas() {
        return BuiltInRegistries.FLUID.get(this.secondaryGas);
    }

    public long getSecondaryGasMb() {
        return this.secondaryGasMb;
    }

    public long getSecondaryGasDroplets() {
        return this.secondaryGasMb * FluidUtil.MILLIBUCKET;
    }

    public long getEnergyPerTick() {
        return this.energyPerTick;
    }

    public int getCycleTicks() {
        return this.cycleTicks;
    }

    public ItemStack getOutputCell() {
        return this.outputCell;
    }

    @Override
    public @NotNull String getGroup() {
        return this.group;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return GCRecipes.FUSION_REACTOR_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.FUSION_REACTOR_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<FusionReactorRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<FusionReactorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                ResourceLocation.CODEC.fieldOf("primary_gas").forGetter(r -> r.primaryGas),
                Codec.LONG.fieldOf("primary_gas_mb").forGetter(r -> r.primaryGasMb),
                ResourceLocation.CODEC.fieldOf("secondary_gas").forGetter(r -> r.secondaryGas),
                Codec.LONG.fieldOf("secondary_gas_mb").forGetter(r -> r.secondaryGasMb),
                Codec.LONG.fieldOf("energy_per_tick").forGetter(r -> r.energyPerTick),
                Codec.INT.optionalFieldOf("cycle_ticks", 40).forGetter(r -> r.cycleTicks),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("output_cell", ItemStack.EMPTY).forGetter(r -> r.outputCell)
        ).apply(instance, FusionReactorRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FusionReactorRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public @NotNull FusionReactorRecipe decode(RegistryFriendlyByteBuf buf) {
                String group = ByteBufCodecs.STRING_UTF8.decode(buf);
                ResourceLocation primaryGas = ResourceLocation.STREAM_CODEC.decode(buf);
                long primaryGasMb = ByteBufCodecs.VAR_LONG.decode(buf);
                ResourceLocation secondaryGas = ResourceLocation.STREAM_CODEC.decode(buf);
                long secondaryGasMb = ByteBufCodecs.VAR_LONG.decode(buf);
                long energyPerTick = ByteBufCodecs.VAR_LONG.decode(buf);
                int cycleTicks = ByteBufCodecs.VAR_INT.decode(buf);
                ItemStack outputCell = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                return new FusionReactorRecipe(group, primaryGas, primaryGasMb, secondaryGas, secondaryGasMb, energyPerTick, cycleTicks, outputCell);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, FusionReactorRecipe recipe) {
                ByteBufCodecs.STRING_UTF8.encode(buf, recipe.group);
                ResourceLocation.STREAM_CODEC.encode(buf, recipe.primaryGas);
                ByteBufCodecs.VAR_LONG.encode(buf, recipe.primaryGasMb);
                ResourceLocation.STREAM_CODEC.encode(buf, recipe.secondaryGas);
                ByteBufCodecs.VAR_LONG.encode(buf, recipe.secondaryGasMb);
                ByteBufCodecs.VAR_LONG.encode(buf, recipe.energyPerTick);
                ByteBufCodecs.VAR_INT.encode(buf, recipe.cycleTicks);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.outputCell);
            }
        };

        @Override
        public @NotNull MapCodec<FusionReactorRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, FusionReactorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
