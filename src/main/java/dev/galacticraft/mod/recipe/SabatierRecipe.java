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
 * Data-driven catalytic chemical synthesis recipe for the Sabatier Chemical Reactor.
 * Supports 2 reagent fluid/gas inputs (order-independent matching across tanks A and B),
 * an optional solid feedstock (e.g., Mercury Sulfide Ore for Sulfuric Acid),
 * a non-consumed catalytic bed (Niobium or Palladium), and 1-2 fluid/gas outputs.
 */
public class SabatierRecipe implements Recipe<SabatierRecipe.Input> {
    private final String group;
    private final ResourceLocation reagentA;
    private final long reagentAMb;
    private final Optional<ResourceLocation> reagentB;
    private final long reagentBMb;
    private final Ingredient solidReagent;
    private final Ingredient catalyst;
    private final ResourceLocation productA;
    private final long productAMb;
    private final Optional<ResourceLocation> productB;
    private final long productBMb;
    private final int targetTempC;
    private final int processingTime;
    private final long energyPerTick;
    private final String formulaLabel;

    public SabatierRecipe(
            String group,
            ResourceLocation reagentA,
            long reagentAMb,
            Optional<ResourceLocation> reagentB,
            long reagentBMb,
            Ingredient solidReagent,
            Ingredient catalyst,
            ResourceLocation productA,
            long productAMb,
            Optional<ResourceLocation> productB,
            long productBMb,
            int targetTempC,
            int processingTime,
            long energyPerTick,
            String formulaLabel
    ) {
        this.group = group;
        this.reagentA = reagentA;
        this.reagentAMb = reagentAMb;
        this.reagentB = reagentB;
        this.reagentBMb = reagentBMb;
        this.solidReagent = solidReagent;
        this.catalyst = catalyst;
        this.productA = productA;
        this.productAMb = productAMb;
        this.productB = productB;
        this.productBMb = productBMb;
        this.targetTempC = targetTempC;
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
        this.formulaLabel = formulaLabel;
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (!this.catalyst.isEmpty() && (input.catalystStack().isEmpty() || !this.catalyst.test(input.catalystStack()))) {
            return false;
        }
        if (!this.solidReagent.isEmpty() && (input.solidStack().isEmpty() || !this.solidReagent.test(input.solidStack()))) {
            return false;
        }
        return this.matchesDirect(input) || this.matchesSwapped(input);
    }

    public boolean matchesDirect(Input input) {
        Fluid reqA = this.getReagentA();
        if (input.fluidA() != reqA || input.amountADroplets() < this.getReagentADroplets()) {
            return false;
        }
        if (this.reagentB.isPresent() && this.reagentBMb > 0L) {
            Fluid reqB = this.getReagentB();
            return input.fluidB() == reqB && input.amountBDroplets() >= this.getReagentBDroplets();
        }
        return true;
    }

    public boolean matchesSwapped(Input input) {
        Fluid reqA = this.getReagentA();
        if (input.fluidB() != reqA || input.amountBDroplets() < this.getReagentADroplets()) {
            return false;
        }
        if (this.reagentB.isPresent() && this.reagentBMb > 0L) {
            Fluid reqB = this.getReagentB();
            return input.fluidA() == reqB && input.amountADroplets() >= this.getReagentBDroplets();
        }
        return true;
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
        if (!this.solidReagent.isEmpty()) {
            list.add(this.solidReagent);
        }
        if (!this.catalyst.isEmpty()) {
            list.add(this.catalyst);
        }
        return list;
    }

    public ResourceLocation getReagentAId() {
        return this.reagentA;
    }

    public Fluid getReagentA() {
        return BuiltInRegistries.FLUID.get(this.reagentA);
    }

    public long getReagentAMb() {
        return this.reagentAMb;
    }

    public long getReagentADroplets() {
        return this.reagentAMb * FluidUtil.MILLIBUCKET;
    }

    public Optional<ResourceLocation> getReagentBId() {
        return this.reagentB;
    }

    public @Nullable Fluid getReagentB() {
        return this.reagentB.map(BuiltInRegistries.FLUID::get).orElse(null);
    }

    public long getReagentBMb() {
        return this.reagentBMb;
    }

    public long getReagentBDroplets() {
        return this.reagentBMb * FluidUtil.MILLIBUCKET;
    }

    public Ingredient getSolidReagent() {
        return this.solidReagent;
    }

    public Ingredient getCatalyst() {
        return this.catalyst;
    }

    public ResourceLocation getProductAId() {
        return this.productA;
    }

    public Fluid getProductA() {
        return BuiltInRegistries.FLUID.get(this.productA);
    }

    public long getProductAMb() {
        return this.productAMb;
    }

    public long getProductADroplets() {
        return this.productAMb * FluidUtil.MILLIBUCKET;
    }

    public Optional<ResourceLocation> getProductBId() {
        return this.productB;
    }

    public @Nullable Fluid getProductB() {
        return this.productB.map(BuiltInRegistries.FLUID::get).orElse(null);
    }

    public long getProductBMb() {
        return this.productBMb;
    }

    public long getProductBDroplets() {
        return this.productBMb * FluidUtil.MILLIBUCKET;
    }

    public int getTargetTempC() {
        return this.targetTempC;
    }

    public int getProcessingTime() {
        return this.processingTime;
    }

    public long getEnergyPerTick() {
        return this.energyPerTick;
    }

    public String getFormulaLabel() {
        return this.formulaLabel;
    }

    @Override
    public @NotNull String getGroup() {
        return this.group;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return GCRecipes.SABATIER_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return GCRecipes.SABATIER_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(
            Fluid fluidA,
            long amountADroplets,
            Fluid fluidB,
            long amountBDroplets,
            ItemStack solidStack,
            ItemStack catalystStack
    ) implements RecipeInput {
        @Override
        public @NotNull ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> this.solidStack;
                case 1 -> this.catalystStack;
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return this.solidStack.isEmpty()
                    && this.catalystStack.isEmpty()
                    && (this.fluidA == Fluids.EMPTY || this.amountADroplets <= 0L)
                    && (this.fluidB == Fluids.EMPTY || this.amountBDroplets <= 0L);
        }
    }

    public static class Serializer implements RecipeSerializer<SabatierRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        public static final MapCodec<SabatierRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                ResourceLocation.CODEC.fieldOf("reagent_a").forGetter(r -> r.reagentA),
                Codec.LONG.fieldOf("reagent_a_mb").forGetter(r -> r.reagentAMb),
                ResourceLocation.CODEC.optionalFieldOf("reagent_b").forGetter(r -> r.reagentB),
                Codec.LONG.optionalFieldOf("reagent_b_mb", 0L).forGetter(r -> r.reagentBMb),
                Ingredient.CODEC.optionalFieldOf("solid_reagent", Ingredient.EMPTY).forGetter(r -> r.solidReagent),
                Ingredient.CODEC.optionalFieldOf("catalyst", Ingredient.EMPTY).forGetter(r -> r.catalyst),
                ResourceLocation.CODEC.fieldOf("product_a").forGetter(r -> r.productA),
                Codec.LONG.fieldOf("product_a_mb").forGetter(r -> r.productAMb),
                ResourceLocation.CODEC.optionalFieldOf("product_b").forGetter(r -> r.productB),
                Codec.LONG.optionalFieldOf("product_b_mb", 0L).forGetter(r -> r.productBMb),
                Codec.INT.optionalFieldOf("target_temp_c", 350).forGetter(r -> r.targetTempC),
                Codec.INT.optionalFieldOf("processing_time", 40).forGetter(r -> r.processingTime),
                Codec.LONG.optionalFieldOf("energy_per_tick", 110L).forGetter(r -> r.energyPerTick),
                Codec.STRING.optionalFieldOf("formula", "").forGetter(r -> r.formulaLabel)
        ).apply(instance, SabatierRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SabatierRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public @NotNull SabatierRecipe decode(RegistryFriendlyByteBuf buf) {
                String group = ByteBufCodecs.STRING_UTF8.decode(buf);
                ResourceLocation reagentA = ResourceLocation.STREAM_CODEC.decode(buf);
                long reagentAMb = ByteBufCodecs.VAR_LONG.decode(buf);
                Optional<ResourceLocation> reagentB = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf);
                long reagentBMb = ByteBufCodecs.VAR_LONG.decode(buf);
                Ingredient solidReagent = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                ResourceLocation productA = ResourceLocation.STREAM_CODEC.decode(buf);
                long productAMb = ByteBufCodecs.VAR_LONG.decode(buf);
                Optional<ResourceLocation> productB = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf);
                long productBMb = ByteBufCodecs.VAR_LONG.decode(buf);
                int targetTempC = ByteBufCodecs.VAR_INT.decode(buf);
                int processingTime = ByteBufCodecs.VAR_INT.decode(buf);
                long energyPerTick = ByteBufCodecs.VAR_LONG.decode(buf);
                String formulaLabel = ByteBufCodecs.STRING_UTF8.decode(buf);
                return new SabatierRecipe(
                        group, reagentA, reagentAMb, reagentB, reagentBMb,
                        solidReagent, catalyst, productA, productAMb, productB, productBMb,
                        targetTempC, processingTime, energyPerTick, formulaLabel
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, SabatierRecipe r) {
                ByteBufCodecs.STRING_UTF8.encode(buf, r.group);
                ResourceLocation.STREAM_CODEC.encode(buf, r.reagentA);
                ByteBufCodecs.VAR_LONG.encode(buf, r.reagentAMb);
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, r.reagentB);
                ByteBufCodecs.VAR_LONG.encode(buf, r.reagentBMb);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.solidReagent);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.catalyst);
                ResourceLocation.STREAM_CODEC.encode(buf, r.productA);
                ByteBufCodecs.VAR_LONG.encode(buf, r.productAMb);
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, r.productB);
                ByteBufCodecs.VAR_LONG.encode(buf, r.productBMb);
                ByteBufCodecs.VAR_INT.encode(buf, r.targetTempC);
                ByteBufCodecs.VAR_INT.encode(buf, r.processingTime);
                ByteBufCodecs.VAR_LONG.encode(buf, r.energyPerTick);
                ByteBufCodecs.STRING_UTF8.encode(buf, r.formulaLabel);
            }
        };

        @Override
        public @NotNull MapCodec<SabatierRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SabatierRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
