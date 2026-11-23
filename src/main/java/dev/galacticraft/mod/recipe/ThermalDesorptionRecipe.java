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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ThermalDesorptionRecipe implements Recipe<SingleRecipeInput> {
    private final String group;
    private final Ingredient ingredient;
    private final long gasAmountMb;
    private final ItemStack primaryByproduct;
    private final ItemStack secondaryByproduct;
    private final int processingTime;

    public ThermalDesorptionRecipe(
            String group,
            Ingredient ingredient,
            long gasAmountMb,
            ItemStack primaryByproduct,
            ItemStack secondaryByproduct,
            int processingTime
    ) {
        this.group = group;
        this.ingredient = ingredient;
        this.gasAmountMb = gasAmountMb;
        this.primaryByproduct = primaryByproduct;
        this.secondaryByproduct = secondaryByproduct;
        this.processingTime = processingTime;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public @NotNull ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return this.primaryByproduct.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.primaryByproduct;
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

    public long getGasAmountMb() {
        return this.gasAmountMb;
    }

    public long getGasAmountDroplets() {
        return this.gasAmountMb * FluidUtil.MILLIBUCKET;
    }

    public ItemStack getPrimaryByproduct() {
        return this.primaryByproduct;
    }

    public ItemStack getSecondaryByproduct() {
        return this.secondaryByproduct;
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
        return GCRecipes.THERMAL_DESORPTION_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.THERMAL_DESORPTION_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<ThermalDesorptionRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<ThermalDesorptionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Ingredient.CODEC.fieldOf("ingredient").forGetter(r -> r.ingredient),
                Codec.LONG.fieldOf("gas_amount_mb").forGetter(r -> r.gasAmountMb),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("primary_byproduct", ItemStack.EMPTY).forGetter(r -> r.primaryByproduct),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("secondary_byproduct", ItemStack.EMPTY).forGetter(r -> r.secondaryByproduct),
                Codec.INT.optionalFieldOf("processing_time", 100).forGetter(r -> r.processingTime)
        ).apply(instance, ThermalDesorptionRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ThermalDesorptionRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.group,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.ingredient,
                ByteBufCodecs.VAR_LONG, r -> r.gasAmountMb,
                ItemStack.OPTIONAL_STREAM_CODEC, r -> r.primaryByproduct,
                ItemStack.OPTIONAL_STREAM_CODEC, r -> r.secondaryByproduct,
                ByteBufCodecs.VAR_INT, r -> r.processingTime,
                ThermalDesorptionRecipe::new
        );

        @Override
        public @NotNull MapCodec<ThermalDesorptionRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ThermalDesorptionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
