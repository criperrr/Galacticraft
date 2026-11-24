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
import dev.galacticraft.api.component.TelemetryData;
import dev.galacticraft.mod.content.item.GCItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Data-driven recipe for the {@code TELEMETRY_ANALYZER}, cross-correlating a
 * {@code RECORDED_DATA_DRIVE} + planetary specimen + chemical reagent into Science Points,
 * 5 Scientific Domain points, and a recycled/processed output item.
 * Cached in O(1) via {@link net.minecraft.world.item.crafting.RecipeManager#createCheck}.
 */
public class TelemetryAnalysisRecipe implements Recipe<TelemetryAnalysisRecipeInput> {
    private final String group;
    private final Ingredient sample;
    private final Ingredient reagent;
    private final Optional<ResourceLocation> targetPlanet;
    private final int bonusSp;
    private final int bonusDomainPoints;
    private final ItemStack outputItem;
    private final int processingTime;

    public TelemetryAnalysisRecipe(
            String group,
            Ingredient sample,
            Ingredient reagent,
            Optional<ResourceLocation> targetPlanet,
            int bonusSp,
            int bonusDomainPoints,
            ItemStack outputItem,
            int processingTime
    ) {
        this.group = group;
        this.sample = sample;
        this.reagent = reagent;
        this.targetPlanet = targetPlanet;
        this.bonusSp = Math.max(0, bonusSp);
        this.bonusDomainPoints = Math.max(0, bonusDomainPoints);
        this.outputItem = outputItem;
        this.processingTime = Math.max(10, processingTime);
    }

    @Override
    public boolean matches(@NotNull TelemetryAnalysisRecipeInput input, @NotNull Level level) {
        ItemStack drive = input.dataDrive();
        if (drive.isEmpty() || !drive.is(GCItems.RECORDED_DATA_DRIVE)) {
            return false;
        }
        TelemetryData telemetry = drive.get(GCDataComponents.TELEMETRY_DATA);
        if (telemetry == null) {
            return false;
        }
        if (this.targetPlanet.isPresent()
                && !this.targetPlanet.get().getPath().equalsIgnoreCase(telemetry.planetId().getPath())) {
            return false;
        }
        return this.sample.test(input.sample()) && this.reagent.test(input.reagent());
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull TelemetryAnalysisRecipeInput input, HolderLookup.@NotNull Provider registries) {
        return this.outputItem.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return this.outputItem;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(Ingredient.of(GCItems.RECORDED_DATA_DRIVE));
        list.add(this.sample);
        list.add(this.reagent);
        return list;
    }

    public Ingredient getSample() {
        return this.sample;
    }

    public Ingredient getReagent() {
        return this.reagent;
    }

    public Optional<ResourceLocation> getTargetPlanet() {
        return this.targetPlanet;
    }

    public int getBonusSp() {
        return this.bonusSp;
    }

    public int getBonusDomainPoints() {
        return this.bonusDomainPoints;
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
        return GCRecipes.TELEMETRY_ANALYSIS_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.TELEMETRY_ANALYSIS_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<TelemetryAnalysisRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<TelemetryAnalysisRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Ingredient.CODEC.fieldOf("sample").forGetter(r -> r.sample),
                Ingredient.CODEC.fieldOf("reagent").forGetter(r -> r.reagent),
                ResourceLocation.CODEC.optionalFieldOf("target_planet").forGetter(r -> r.targetPlanet),
                Codec.INT.optionalFieldOf("bonus_sp", 15).forGetter(r -> r.bonusSp),
                Codec.INT.optionalFieldOf("bonus_domain_points", 12).forGetter(r -> r.bonusDomainPoints),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("output_item", ItemStack.EMPTY).forGetter(r -> r.outputItem),
                Codec.INT.optionalFieldOf("processing_time", 80).forGetter(r -> r.processingTime)
        ).apply(instance, TelemetryAnalysisRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, TelemetryAnalysisRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public @NotNull TelemetryAnalysisRecipe decode(@NotNull RegistryFriendlyByteBuf buf) {
                String group = ByteBufCodecs.STRING_UTF8.decode(buf);
                Ingredient sample = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                Ingredient reagent = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                Optional<ResourceLocation> planet = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf);
                int sp = ByteBufCodecs.VAR_INT.decode(buf);
                int dom = ByteBufCodecs.VAR_INT.decode(buf);
                ItemStack out = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                int time = ByteBufCodecs.VAR_INT.decode(buf);
                return new TelemetryAnalysisRecipe(group, sample, reagent, planet, sp, dom, out, time);
            }

            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull TelemetryAnalysisRecipe r) {
                ByteBufCodecs.STRING_UTF8.encode(buf, r.group);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.sample);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.reagent);
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, r.targetPlanet);
                ByteBufCodecs.VAR_INT.encode(buf, r.bonusSp);
                ByteBufCodecs.VAR_INT.encode(buf, r.bonusDomainPoints);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.outputItem);
                ByteBufCodecs.VAR_INT.encode(buf, r.processingTime);
            }
        };

        @Override
        public @NotNull MapCodec<TelemetryAnalysisRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, TelemetryAnalysisRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
