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

public class ChemicalLeachingRecipe implements Recipe<ChemicalLeachingRecipe.Input> {
    private final String group;
    private final Ingredient oreIngredient;
    private final ResourceLocation fluidId;
    private final long fluidAmountMb;
    private final ItemStack primaryOutput;
    private final ItemStack byproductOutput;
    private final float byproductChance;
    private final int processingTime;

    public ChemicalLeachingRecipe(
            String group,
            Ingredient oreIngredient,
            ResourceLocation fluidId,
            long fluidAmountMb,
            ItemStack primaryOutput,
            ItemStack byproductOutput,
            float byproductChance,
            int processingTime
    ) {
        this.group = group;
        this.oreIngredient = oreIngredient;
        this.fluidId = fluidId;
        this.fluidAmountMb = Math.max(1L, fluidAmountMb);
        this.primaryOutput = primaryOutput;
        this.byproductOutput = byproductOutput;
        this.byproductChance = Math.max(0.0F, Math.min(1.0F, byproductChance));
        this.processingTime = Math.max(1, processingTime);
    }

    @Override
    public boolean matches(Input input, Level level) {
        Fluid requiredFluid = this.getFluid();
        if (requiredFluid == Fluids.EMPTY) {
            return false;
        }
        return this.oreIngredient.test(input.ore())
                && input.fluid() == requiredFluid
                && input.fluidDroplets() >= this.getFluidAmountDroplets();
    }

    @Override
    public @NotNull ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.primaryOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.primaryOutput;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.oreIngredient);
        return list;
    }

    public Ingredient getOreIngredient() {
        return this.oreIngredient;
    }

    public ResourceLocation getFluidId() {
        return this.fluidId;
    }

    public Fluid getFluid() {
        return BuiltInRegistries.FLUID.get(this.fluidId);
    }

    public long getFluidAmountMb() {
        return this.fluidAmountMb;
    }

    public long getFluidAmountDroplets() {
        return this.fluidAmountMb * FluidUtil.MILLIBUCKET;
    }

    public ItemStack getPrimaryOutput() {
        return this.primaryOutput;
    }

    public ItemStack getByproductOutput() {
        return this.byproductOutput;
    }

    public float getByproductChance() {
        return this.byproductChance;
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
        return GCRecipes.CHEMICAL_LEACHING_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.CHEMICAL_LEACHING_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(ItemStack ore, Fluid fluid, long fluidDroplets) implements RecipeInput {
        @Override
        public @NotNull ItemStack getItem(int index) {
            return index == 0 ? this.ore : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class Serializer implements RecipeSerializer<ChemicalLeachingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<ChemicalLeachingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ore").forGetter(r -> r.oreIngredient),
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(r -> r.fluidId),
                Codec.LONG.optionalFieldOf("fluid_amount_mb", 250L).forGetter(r -> r.fluidAmountMb),
                ItemStack.STRICT_CODEC.fieldOf("primary_output").forGetter(r -> r.primaryOutput),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("byproduct_output", ItemStack.EMPTY).forGetter(r -> r.byproductOutput),
                Codec.FLOAT.optionalFieldOf("byproduct_chance", 1.0F).forGetter(r -> r.byproductChance),
                Codec.INT.optionalFieldOf("processing_time", 120).forGetter(r -> r.processingTime)
        ).apply(instance, ChemicalLeachingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChemicalLeachingRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public @NotNull ChemicalLeachingRecipe decode(@NotNull RegistryFriendlyByteBuf buf) {
                String group = ByteBufCodecs.STRING_UTF8.decode(buf);
                Ingredient ore = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                ResourceLocation fluidId = ResourceLocation.STREAM_CODEC.decode(buf);
                long fluidAmountMb = ByteBufCodecs.VAR_LONG.decode(buf);
                ItemStack primary = ItemStack.STREAM_CODEC.decode(buf);
                ItemStack byproduct = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                float chance = buf.readFloat();
                int time = ByteBufCodecs.VAR_INT.decode(buf);
                return new ChemicalLeachingRecipe(group, ore, fluidId, fluidAmountMb, primary, byproduct, chance, time);
            }

            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull ChemicalLeachingRecipe recipe) {
                ByteBufCodecs.STRING_UTF8.encode(buf, recipe.group);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.oreIngredient);
                ResourceLocation.STREAM_CODEC.encode(buf, recipe.fluidId);
                ByteBufCodecs.VAR_LONG.encode(buf, recipe.fluidAmountMb);
                ItemStack.STREAM_CODEC.encode(buf, recipe.primaryOutput);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.byproductOutput);
                buf.writeFloat(recipe.byproductChance);
                ByteBufCodecs.VAR_INT.encode(buf, recipe.processingTime);
            }
        };

        @Override
        public @NotNull MapCodec<ChemicalLeachingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ChemicalLeachingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
