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
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import io.netty.buffer.ByteBuf;
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
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Data-driven recipe for the Part Assembler machine, combining a schematic blueprint
 * and a 3x3 material pattern to manufacture modular rocket parts with attached
 * {@link RocketPartStats} and {@link PartIntegrity} data components.
 */
public class PartAssemblerRecipe implements Recipe<PartAssemblerRecipeInput> {
    private final String group;
    private final Ingredient schematic;
    private final ShapedRecipePattern pattern;
    private final ItemStack result;
    private final ItemStack scrapByproduct;
    private final int assemblyTime;
    private final Optional<RocketPartStats> partStats;

    public PartAssemblerRecipe(
            String group,
            Ingredient schematic,
            ShapedRecipePattern pattern,
            ItemStack result,
            ItemStack scrapByproduct,
            int assemblyTime,
            Optional<RocketPartStats> partStats
    ) {
        this.group = group != null ? group : "";
        this.schematic = schematic;
        this.pattern = pattern;
        this.result = result;
        this.scrapByproduct = (scrapByproduct != null && !scrapByproduct.isEmpty()) ? scrapByproduct.copy() : ItemStack.EMPTY;
        this.assemblyTime = Math.max(1, assemblyTime);
        this.partStats = partStats != null ? partStats : Optional.empty();
    }

    @Override
    public boolean matches(PartAssemblerRecipeInput input, Level level) {
        if (!this.schematic.test(input.schematic())) {
            return false;
        }
        return this.pattern.matches(input.grid());
    }

    @Override
    public @NotNull ItemStack assemble(PartAssemblerRecipeInput input, HolderLookup.Provider registries) {
        ItemStack assembled = this.result.copy();
        this.partStats.ifPresent(stats -> assembled.set(GCDataComponents.ROCKET_PART_STATS, stats));
        if (!assembled.has(GCDataComponents.PART_INTEGRITY)
                && (this.partStats.isPresent() || assembled.has(GCDataComponents.ROCKET_PART_STATS))) {
            assembled.set(GCDataComponents.PART_INTEGRITY, PartIntegrity.pristine(100.0));
        }
        return assembled;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack preview = this.result.copy();
        this.partStats.ifPresent(stats -> preview.set(GCDataComponents.ROCKET_PART_STATS, stats));
        if (!preview.has(GCDataComponents.PART_INTEGRITY)
                && (this.partStats.isPresent() || preview.has(GCDataComponents.ROCKET_PART_STATS))) {
            preview.set(GCDataComponents.PART_INTEGRITY, PartIntegrity.pristine(100.0));
        }
        return preview;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> all = NonNullList.createWithCapacity(1 + this.pattern.ingredients().size());
        all.add(this.schematic);
        all.addAll(this.pattern.ingredients());
        return all;
    }

    public @NotNull Ingredient getSchematic() {
        return this.schematic;
    }

    public @NotNull ShapedRecipePattern getPattern() {
        return this.pattern;
    }

    public @NotNull ItemStack getScrapByproduct() {
        return this.scrapByproduct.copy();
    }

    public int getAssemblyTime() {
        return this.assemblyTime;
    }

    public @NotNull Optional<RocketPartStats> getPartStats() {
        return this.partStats;
    }

    @Override
    public @NotNull String getGroup() {
        return this.group;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return GCRecipes.PART_ASSEMBLER_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.PART_ASSEMBLER_TYPE;
    }

    public static class Serializer implements RecipeSerializer<PartAssemblerRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<PartAssemblerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Ingredient.CODEC_NONEMPTY.fieldOf("schematic").forGetter(r -> r.schematic),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("scrap_byproduct", ItemStack.EMPTY).forGetter(r -> r.scrapByproduct),
                Codec.INT.optionalFieldOf("assembly_time", 200).forGetter(r -> r.assemblyTime),
                RocketPartStats.CODEC.optionalFieldOf("part_stats").forGetter(r -> r.partStats)
        ).apply(instance, PartAssemblerRecipe::new));

        private static final StreamCodec<ByteBuf, Optional<RocketPartStats>> OPTIONAL_STATS_STREAM_CODEC =
                ByteBufCodecs.optional(RocketPartStats.STREAM_CODEC);

        public static final StreamCodec<RegistryFriendlyByteBuf, PartAssemblerRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::encode,
                Serializer::decode
        );

        private static void encode(RegistryFriendlyByteBuf buf, PartAssemblerRecipe recipe) {
            ByteBufCodecs.STRING_UTF8.encode(buf, recipe.group);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.schematic);
            ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.scrapByproduct);
            ByteBufCodecs.VAR_INT.encode(buf, recipe.assemblyTime);
            OPTIONAL_STATS_STREAM_CODEC.encode(buf, recipe.partStats);
        }

        private static PartAssemblerRecipe decode(RegistryFriendlyByteBuf buf) {
            String group = ByteBufCodecs.STRING_UTF8.decode(buf);
            Ingredient schematic = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            ItemStack scrapByproduct = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int assemblyTime = ByteBufCodecs.VAR_INT.decode(buf);
            Optional<RocketPartStats> partStats = OPTIONAL_STATS_STREAM_CODEC.decode(buf);
            return new PartAssemblerRecipe(group, schematic, pattern, result, scrapByproduct, assemblyTime, partStats);
        }

        @Override
        public @NotNull MapCodec<PartAssemblerRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, PartAssemblerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
