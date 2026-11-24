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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Data-driven solid separation, actinide enrichment, and scrap metal recovery recipe
 * for the Isotopic Centrifuge (requires Superconducting Coil Nb3Sn + >=40,000 RPM rotor).
 */
public class CentrifugeRecipe implements Recipe<SingleRecipeInput> {
    private final String group;
    private final Ingredient ingredient;
    private final Optional<ResourceLocation> outputGas;
    private final long outputGasMb;
    private final ItemStack outputItem;
    private final int processingTime;

    public CentrifugeRecipe(
            String group,
            Ingredient ingredient,
            Optional<ResourceLocation> outputGas,
            long outputGasMb,
            ItemStack outputItem,
            int processingTime
    ) {
        this.group = group;
        this.ingredient = ingredient;
        this.outputGas = outputGas;
        this.outputGasMb = outputGasMb;
        this.outputItem = outputItem;
        this.processingTime = processingTime;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public @NotNull ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return this.outputItem.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.outputItem;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.ingredient);
        return list;
    }

    public Ingredient getIngredient() {
        return this.ingredient;
    }

    public Optional<ResourceLocation> getOutputGasId() {
        return this.outputGas;
    }

    public @Nullable Fluid getOutputGas() {
        return this.outputGas.map(BuiltInRegistries.FLUID::get).orElse(null);
    }

    public long getOutputGasMb() {
        return this.outputGasMb;
    }

    public long getOutputGasDroplets() {
        return this.outputGasMb * FluidUtil.MILLIBUCKET;
    }

    public ItemStack getOutputItem() {
        return this.outputItem;
    }

    public int getProcessingTime() {
        return this.processingTime;
    }

    @Override
    public @NotNull String getGroup() {
        return this.group;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return GCRecipes.CENTRIFUGE_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.CENTRIFUGE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<CentrifugeRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<CentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Ingredient.CODEC.fieldOf("ingredient").forGetter(r -> r.ingredient),
                ResourceLocation.CODEC.optionalFieldOf("output_gas").forGetter(r -> r.outputGas),
                Codec.LONG.optionalFieldOf("output_gas_mb", 0L).forGetter(r -> r.outputGasMb),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("output_item", ItemStack.EMPTY).forGetter(r -> r.outputItem),
                Codec.INT.optionalFieldOf("processing_time", 60).forGetter(r -> r.processingTime)
        ).apply(instance, CentrifugeRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.group,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.ingredient,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), r -> r.outputGas,
                ByteBufCodecs.VAR_LONG, r -> r.outputGasMb,
                ItemStack.OPTIONAL_STREAM_CODEC, r -> r.outputItem,
                ByteBufCodecs.VAR_INT, r -> r.processingTime,
                CentrifugeRecipe::new
        );

        @Override
        public @NotNull MapCodec<CentrifugeRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
