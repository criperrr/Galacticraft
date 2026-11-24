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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class MetallurgicInfusingRecipe implements Recipe<MetallurgicInfusingRecipe.Input> {
    private final String group;
    private final Ingredient baseIngredient;
    private final String infusionType;
    private final int infusionAmount;
    private final ItemStack result;
    private final int processingTime;

    public MetallurgicInfusingRecipe(
            String group,
            Ingredient baseIngredient,
            String infusionType,
            int infusionAmount,
            ItemStack result,
            int processingTime
    ) {
        this.group = group;
        this.baseIngredient = baseIngredient;
        this.infusionType = infusionType;
        this.infusionAmount = Math.max(1, infusionAmount);
        this.result = result;
        this.processingTime = Math.max(1, processingTime);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return this.baseIngredient.test(input.baseMetal())
                && this.infusionType.equals(input.infusionType())
                && input.infusionAmount() >= this.infusionAmount;
    }

    @Override
    public @NotNull ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.baseIngredient);
        return list;
    }

    public Ingredient getBaseIngredient() {
        return this.baseIngredient;
    }

    public String getInfusionType() {
        return this.infusionType;
    }

    public int getInfusionAmount() {
        return this.infusionAmount;
    }

    public ItemStack getResult() {
        return this.result;
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
        return GCRecipes.METALLURGIC_INFUSING_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.METALLURGIC_INFUSING_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(ItemStack baseMetal, String infusionType, int infusionAmount) implements RecipeInput {
        @Override
        public @NotNull ItemStack getItem(int index) {
            return index == 0 ? this.baseMetal : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class Serializer implements RecipeSerializer<MetallurgicInfusingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<MetallurgicInfusingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Ingredient.CODEC_NONEMPTY.fieldOf("base_ingredient").forGetter(r -> r.baseIngredient),
                Codec.STRING.fieldOf("infusion_type").forGetter(r -> r.infusionType),
                Codec.INT.optionalFieldOf("infusion_amount", 10).forGetter(r -> r.infusionAmount),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("processing_time", 100).forGetter(r -> r.processingTime)
        ).apply(instance, MetallurgicInfusingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MetallurgicInfusingRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.group,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.baseIngredient,
                ByteBufCodecs.STRING_UTF8, r -> r.infusionType,
                ByteBufCodecs.VAR_INT, r -> r.infusionAmount,
                ItemStack.STREAM_CODEC, r -> r.result,
                ByteBufCodecs.VAR_INT, r -> r.processingTime,
                MetallurgicInfusingRecipe::new
        );

        @Override
        public @NotNull MapCodec<MetallurgicInfusingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MetallurgicInfusingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
