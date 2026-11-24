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
import com.mojang.serialization.DataResult;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CompositeAutoclaveRecipe implements Recipe<CompositeAutoclaveRecipe.Input> {
    private final String group;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int targetTemperature;
    private final int targetPressureBar;
    private final int processingTime;

    public CompositeAutoclaveRecipe(
            String group,
            List<Ingredient> ingredients,
            ItemStack result,
            int targetTemperature,
            int targetPressureBar,
            int processingTime
    ) {
        this.group = group;
        this.ingredients = NonNullList.createWithCapacity(ingredients.size());
        this.ingredients.addAll(ingredients);
        this.result = result;
        this.targetTemperature = Math.max(100, targetTemperature);
        this.targetPressureBar = Math.max(2, targetPressureBar);
        this.processingTime = Math.max(1, processingTime);
    }

    /**
     * Finds a bijective assignment mapping each recipe ingredient index to a distinct 2x2 grid slot index,
     * ensuring every non-empty grid slot is accounted for (exact shapeless match inside the 2x2 bay).
     */
    public int @Nullable [] matchSlots(Input input) {
        int nonEmptyCount = 0;
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                nonEmptyCount++;
            }
        }
        if (nonEmptyCount != this.ingredients.size()) {
            return null;
        }
        int[] assigned = new int[this.ingredients.size()];
        boolean[] used = new boolean[input.size()];
        if (assignRecursive(0, input, used, assigned)) {
            return assigned;
        }
        return null;
    }

    private boolean assignRecursive(int ingIdx, Input input, boolean[] used, int[] assigned) {
        if (ingIdx >= this.ingredients.size()) {
            return true;
        }
        Ingredient ingredient = this.ingredients.get(ingIdx);
        for (int slot = 0; slot < input.size(); slot++) {
            if (!used[slot]) {
                ItemStack stack = input.getItem(slot);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    used[slot] = true;
                    assigned[ingIdx] = slot;
                    if (assignRecursive(ingIdx + 1, input, used, assigned)) {
                        return true;
                    }
                    used[slot] = false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return this.matchSlots(input) != null;
    }

    @Override
    public @NotNull ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    public ItemStack getResult() {
        return this.result;
    }

    public int getTargetTemperature() {
        return this.targetTemperature;
    }

    public int getTargetPressureBar() {
        return this.targetPressureBar;
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
        return GCRecipes.COMPOSITE_AUTOCLAVE_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.COMPOSITE_AUTOCLAVE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(List<ItemStack> grid) implements RecipeInput {
        @Override
        public @NotNull ItemStack getItem(int index) {
            return index >= 0 && index < this.grid.size() ? this.grid.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return this.grid.size();
        }
    }

    public static class Serializer implements RecipeSerializer<CompositeAutoclaveRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final Codec<List<Ingredient>> INGREDIENTS_CODEC = Ingredient.CODEC_NONEMPTY.listOf().flatXmap(
                list -> {
                    if (list.isEmpty()) {
                        return DataResult.error(() -> "No ingredients for composite autoclave recipe");
                    }
                    if (list.size() > 4) {
                        return DataResult.error(() -> "Too many ingredients for 2x2 composite autoclave recipe (max 4)");
                    }
                    return DataResult.success(list);
                },
                DataResult::success
        );

        public static final MapCodec<CompositeAutoclaveRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(r -> r.ingredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("target_temperature", 450).forGetter(r -> r.targetTemperature),
                Codec.INT.optionalFieldOf("target_pressure_bar", 12).forGetter(r -> r.targetPressureBar),
                Codec.INT.optionalFieldOf("processing_time", 120).forGetter(r -> r.processingTime)
        ).apply(instance, CompositeAutoclaveRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CompositeAutoclaveRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.group,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
                ItemStack.STREAM_CODEC, r -> r.result,
                ByteBufCodecs.VAR_INT, r -> r.targetTemperature,
                ByteBufCodecs.VAR_INT, r -> r.targetPressureBar,
                ByteBufCodecs.VAR_INT, r -> r.processingTime,
                CompositeAutoclaveRecipe::new
        );

        @Override
        public @NotNull MapCodec<CompositeAutoclaveRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, CompositeAutoclaveRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
