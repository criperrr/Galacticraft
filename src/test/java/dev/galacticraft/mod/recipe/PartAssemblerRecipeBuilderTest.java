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

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.api.data.recipe.PartAssemblerRecipeBuilder;
import dev.galacticraft.mod.content.item.GCItems;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Part Assembler Recipe Builder Test Suite")
class PartAssemblerRecipeBuilderTest {

    private static RegistryAccess registryAccess;
    private static RegistryOps<JsonElement> registryOps;

    @BeforeAll
    static void setupMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        registryOps = registryAccess.createSerializationContext(JsonOps.INSTANCE);
    }

    @Nested
    @DisplayName("Builder Validation Failures")
    class ValidationFailures {

        @Test
        @DisplayName("Missing criteria throws IllegalStateException")
        void buildWithoutCriteria_throwsIllegalStateException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .pattern(" R ")
                    .define('R', Items.REDSTONE_TORCH);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    builder.createRecipe(Constant.id("test_nose_cone")));
            assertTrue(ex.getMessage().contains("No way of obtaining recipe"));
        }

        @Test
        @DisplayName("Missing schematic throws IllegalStateException")
        void buildWithoutSchematic_throwsIllegalStateException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .pattern(" R ")
                    .define('R', Items.REDSTONE_TORCH);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    builder.createRecipe(Constant.id("test_no_schematic")));
            assertTrue(ex.getMessage().contains("has no schematic defined"));
        }

        @Test
        @DisplayName("Missing pattern rows throws IllegalStateException")
        void buildWithoutPattern_throwsIllegalStateException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    builder.createRecipe(Constant.id("test_no_pattern")));
            assertTrue(ex.getMessage().contains("No pattern is defined"));
        }

        @Test
        @DisplayName("Pattern with mismatched row widths throws IllegalArgumentException")
        void patternWithMismatchedWidth_throwsIllegalArgumentException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE);
            builder.pattern(" R ");

            assertThrows(IllegalArgumentException.class, () -> builder.pattern("  "));
        }

        @Test
        @DisplayName("Defining whitespace symbol throws IllegalArgumentException")
        void defineSpaceCharacter_throwsIllegalArgumentException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE);

            assertThrows(IllegalArgumentException.class, () -> builder.define(' ', Items.REDSTONE_TORCH));
        }

        @Test
        @DisplayName("Defining duplicate symbol throws IllegalArgumentException")
        void defineDuplicateSymbol_throwsIllegalArgumentException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .define('R', Items.REDSTONE_TORCH);

            assertThrows(IllegalArgumentException.class, () -> builder.define('R', Items.LEVER));
        }
    }

    @Nested
    @DisplayName("Builder Construction & Attribute Verification")
    class SuccessfulConstruction {

        @Test
        @DisplayName("Valid builder correctly configures PartAssemblerRecipe")
        void buildValidRecipe_createsExpectedPartAssemblerRecipe() {
            RocketPartStats stats = new RocketPartStats(1200.0, 850.0, 0L, 3.5, 2200.0);
            PartAssemblerRecipe recipe = (PartAssemblerRecipe) PartAssemblerRecipeBuilder.create(GCItems.ROCKET_ENGINE, 1)
                    .schematic(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC)
                    .define('F', Items.FLINT_AND_STEEL)
                    .define('P', GCItems.TIER_1_HEAVY_DUTY_PLATE)
                    .pattern(" F ")
                    .pattern(" P ")
                    .pattern(" P ")
                    .assemblyTime(320)
                    .scrapByproduct(GCItems.COMPRESSED_TIN, 1)
                    .partStats(stats)
                    .unlockedBy("has_schematic", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC))
                    .createRecipe(Constant.id("test_rocket_engine"));

            assertNotNull(recipe);
            assertEquals(320, recipe.getAssemblyTime());
            assertTrue(recipe.getSchematic().test(new ItemStack(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC)));
            assertFalse(recipe.getSchematic().test(new ItemStack(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)));
            assertEquals(1, recipe.getScrapByproduct().getCount());
            assertEquals(GCItems.COMPRESSED_TIN, recipe.getScrapByproduct().getItem());
            assertTrue(recipe.getPartStats().isPresent());
            assertEquals(stats, recipe.getPartStats().get());

            ItemStack resultStack = recipe.getResultItem(registryAccess);
            assertEquals(GCItems.ROCKET_ENGINE, resultStack.getItem());
            assertEquals(stats, resultStack.get(GCDataComponents.ROCKET_PART_STATS));
            assertNotNull(resultStack.get(GCDataComponents.PART_INTEGRITY));
        }

        @Test
        @DisplayName("Default assembly time is 200 ticks")
        void defaultAssemblyTime_is200Ticks() {
            PartAssemblerRecipe recipe = (PartAssemblerRecipe) PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern(" R ")
                    .unlockedBy("has_cone", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.BASIC_ROCKET_CONE_SCHEMATIC))
                    .createRecipe(Constant.id("test_default_time"));

            assertEquals(200, recipe.getAssemblyTime());
            assertTrue(recipe.getScrapByproduct().isEmpty());
            assertTrue(recipe.getPartStats().isEmpty());
        }

        @Test
        @DisplayName("Assembly time is clamped to at least 1 tick")
        void assemblyTime_clampedToAtLeastOne() {
            PartAssemblerRecipe recipeNegative = (PartAssemblerRecipe) PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern(" R ")
                    .assemblyTime(-50)
                    .unlockedBy("has_cone", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.BASIC_ROCKET_CONE_SCHEMATIC))
                    .createRecipe(Constant.id("test_clamped_time"));

            PartAssemblerRecipe recipeZero = (PartAssemblerRecipe) PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern(" R ")
                    .assemblyTime(0)
                    .unlockedBy("has_cone", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.BASIC_ROCKET_CONE_SCHEMATIC))
                    .createRecipe(Constant.id("test_clamped_zero_time"));

            PartAssemblerRecipe recipeMinValue = (PartAssemblerRecipe) PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern(" R ")
                    .assemblyTime(Integer.MIN_VALUE)
                    .unlockedBy("has_cone", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.BASIC_ROCKET_CONE_SCHEMATIC))
                    .createRecipe(Constant.id("test_clamped_min_time"));

            assertEquals(1, recipeNegative.getAssemblyTime());
            assertEquals(1, recipeZero.getAssemblyTime());
            assertEquals(1, recipeMinValue.getAssemblyTime());
        }

        @Test
        @DisplayName("Scrap byproduct defensively copies external ItemStack before and after createRecipe")
        void scrapByproduct_defensivelyCopiesExternalItemStack() {
            ItemStack externalStack = new ItemStack(GCItems.COMPRESSED_STEEL, 2);
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern(" R ")
                    .scrapByproduct(externalStack)
                    .unlockedBy("has_cone", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.BASIC_ROCKET_CONE_SCHEMATIC));

            // Mutate external stack after passing to builder, before createRecipe
            externalStack.setCount(64);

            PartAssemblerRecipe recipe = (PartAssemblerRecipe) builder.createRecipe(Constant.id("test_defensive_copy"));
            assertEquals(2, recipe.getScrapByproduct().getCount());
            assertEquals(GCItems.COMPRESSED_STEEL, recipe.getScrapByproduct().getItem());

            // Mutate external stack again and also mutate stack returned by getScrapByproduct()
            externalStack.setCount(0);
            ItemStack retrieved = recipe.getScrapByproduct();
            retrieved.setCount(32);

            assertEquals(2, recipe.getScrapByproduct().getCount());
            assertEquals(GCItems.COMPRESSED_STEEL, recipe.getScrapByproduct().getItem());

            // Also verify direct constructor defensive copying when a mutable stack is passed directly
            ItemStack directByproduct = new ItemStack(GCItems.COMPRESSED_IRON, 4);
            PartAssemblerRecipe directRecipe = new PartAssemblerRecipe(
                    "",
                    recipe.getSchematic(),
                    recipe.getPattern(),
                    new ItemStack(GCItems.NOSE_CONE),
                    directByproduct,
                    200,
                    recipe.getPartStats()
            );
            directByproduct.setCount(64);
            assertEquals(4, directRecipe.getScrapByproduct().getCount());
        }

        @Test
        @DisplayName("Scrap byproduct helper methods assign correct ItemStacks")
        void byproductHelpers_produceCorrectItemStack() {
            PartAssemblerRecipeBuilder builder1 = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .scrapByproduct(GCItems.COMPRESSED_TITANIUM);
            PartAssemblerRecipeBuilder builder2 = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .scrapByproduct(GCItems.COMPRESSED_IRON, 2);
            PartAssemblerRecipeBuilder builder3 = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .scrapByproduct(new ItemStack(GCItems.COMPRESSED_STEEL, 3));

            builder1.schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC).pattern("X").define('X', Items.IRON_INGOT)
                    .unlockedBy("u", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT));
            builder2.schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC).pattern("X").define('X', Items.IRON_INGOT)
                    .unlockedBy("u", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT));
            builder3.schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC).pattern("X").define('X', Items.IRON_INGOT)
                    .unlockedBy("u", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT));

            PartAssemblerRecipe r1 = (PartAssemblerRecipe) builder1.createRecipe(Constant.id("r1"));
            PartAssemblerRecipe r2 = (PartAssemblerRecipe) builder2.createRecipe(Constant.id("r2"));
            PartAssemblerRecipe r3 = (PartAssemblerRecipe) builder3.createRecipe(Constant.id("r3"));

            assertEquals(GCItems.COMPRESSED_TITANIUM, r1.getScrapByproduct().getItem());
            assertEquals(1, r1.getScrapByproduct().getCount());

            assertEquals(GCItems.COMPRESSED_IRON, r2.getScrapByproduct().getItem());
            assertEquals(2, r2.getScrapByproduct().getCount());

            assertEquals(GCItems.COMPRESSED_STEEL, r3.getScrapByproduct().getItem());
            assertEquals(3, r3.getScrapByproduct().getCount());
        }

        @Test
        @DisplayName("getResult returns the target item")
        void getResult_returnsResultItem() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.HEAVY_ROCKET_FIN);
            assertEquals(GCItems.HEAVY_ROCKET_FIN, builder.getResult());
        }
    }

    @Nested
    @DisplayName("Codec Roundtrip of Builder Output")
    class SerializationRoundtrip {

        @Test
        @DisplayName("Recipe created via builder encodes and decodes losslessly via MapCodec")
        void testBuilderCreatedRecipe_codecRoundtrip() {
            RocketPartStats stats = new RocketPartStats(1800.0, 1100.0, 405000L, 4.0, 2600.0);
            PartAssemblerRecipe original = (PartAssemblerRecipe) PartAssemblerRecipeBuilder.create(GCItems.ROCKET_BOOSTER)
                    .schematic(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC)
                    .define('M', GCItems.COMPRESSED_METEORIC_IRON)
                    .define('Y', Items.YELLOW_WOOL)
                    .pattern("MYM")
                    .assemblyTime(350)
                    .scrapByproduct(GCItems.COMPRESSED_IRON, 2)
                    .partStats(stats)
                    .unlockedBy("u", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC))
                    .createRecipe(Constant.id("test_booster_roundtrip"));

            JsonElement json = PartAssemblerRecipe.Serializer.CODEC.codec()
                    .encodeStart(registryOps, original)
                    .getOrThrow(msg -> new AssertionError("Encoding failed: " + msg));

            assertTrue(json.isJsonObject());
            assertEquals(350, json.getAsJsonObject().get("assembly_time").getAsInt());
            assertEquals("galacticraft:basic_rocket_engine_schematic",
                    json.getAsJsonObject().getAsJsonObject("schematic").get("item").getAsString());

            PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.CODEC.codec()
                    .parse(registryOps, json)
                    .getOrThrow(msg -> new AssertionError("Decoding failed: " + msg));

            assertEquals(original.getAssemblyTime(), decoded.getAssemblyTime());
            assertEquals(original.getScrapByproduct().getItem(), decoded.getScrapByproduct().getItem());
            assertEquals(original.getScrapByproduct().getCount(), decoded.getScrapByproduct().getCount());
            assertEquals(original.getPartStats(), decoded.getPartStats());
            assertEquals(original.getPattern().width(), decoded.getPattern().width());
            assertEquals(original.getPattern().height(), decoded.getPattern().height());
        }
    }
}
