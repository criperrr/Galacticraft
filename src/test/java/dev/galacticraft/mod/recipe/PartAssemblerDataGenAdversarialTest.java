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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.api.data.recipe.PartAssemblerRecipeBuilder;
import dev.galacticraft.mod.content.item.GCItems;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Part Assembler DataGen Adversarial & Stress Test Suite")
class PartAssemblerDataGenAdversarialTest {

    private static RegistryAccess registryAccess;
    private static RegistryOps<JsonElement> registryOps;
    private static final Path GENERATED_RECIPES_DIR = resolveProjectPath("src/main/generated/data/galacticraft/recipe/part_assembler");
    private static final Path GENERATED_TAGS_DIR = resolveProjectPath("src/main/generated/data/galacticraft/tags/item");

    private static Path resolveProjectPath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }
        Path parentRelative = Paths.get("..").resolve(relativePath).normalize();
        return Files.exists(parentRelative) ? parentRelative : direct;
    }

    @BeforeAll
    static void setupMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        registryOps = registryAccess.createSerializationContext(JsonOps.INSTANCE);
    }

    @Nested
    @DisplayName("Adversarial Builder Input Stress Tests")
    class BuilderStressTests {

        @Test
        @DisplayName("Building with null schematic ingredient throws IllegalStateException")
        void nullSchematic_throwsIllegalStateException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic((Ingredient) null)
                    .pattern(" R ")
                    .define('R', Items.REDSTONE_TORCH);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    builder.createRecipe(Constant.id("test_null_schematic")));
            assertTrue(ex.getMessage().contains("has no schematic defined"));
        }

        @Test
        @DisplayName("Building with empty schematic ingredient throws IllegalStateException")
        void emptySchematic_throwsIllegalStateException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(Ingredient.EMPTY)
                    .pattern(" R ")
                    .define('R', Items.REDSTONE_TORCH);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    builder.createRecipe(Constant.id("test_empty_schematic")));
            assertTrue(ex.getMessage().contains("has no schematic defined"));
        }

        @Test
        @DisplayName("Building without any pattern rows throws IllegalStateException")
        void emptyPatternRows_throwsIllegalStateException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    builder.createRecipe(Constant.id("test_no_pattern")));
            assertTrue(ex.getMessage().contains("No pattern is defined"));
        }

        @Test
        @DisplayName("Jagged pattern rows with different lengths throw IllegalArgumentException")
        void jaggedPatternRows_throwsIllegalArgumentException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE);
            builder.pattern("RR");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    builder.pattern("R"));
            assertTrue(ex.getMessage().contains("same width on every line"));
        }

        @Test
        @DisplayName("Empty pattern row throws IllegalArgumentException during pattern validation")
        void emptyPatternRow_throwsIllegalArgumentException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .pattern("");

            assertThrows(IllegalArgumentException.class, () ->
                    builder.createRecipe(Constant.id("test_empty_row")));
        }

        @Test
        @DisplayName("Pattern row exceeding 3 columns throws IllegalArgumentException")
        void patternWidthExceeding3_throwsIllegalArgumentException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern("RRRR");

            assertThrows(IllegalArgumentException.class, () ->
                    builder.createRecipe(Constant.id("test_width_4")));
        }

        @Test
        @DisplayName("Pattern with more than 3 rows throws IllegalArgumentException")
        void patternHeightExceeding3_throwsIllegalArgumentException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern("R")
                    .pattern("R")
                    .pattern("R")
                    .pattern("R");

            assertThrows(IllegalArgumentException.class, () ->
                    builder.createRecipe(Constant.id("test_height_4")));
        }

        @Test
        @DisplayName("Pattern referencing undefined symbol throws IllegalArgumentException")
        void undefinedSymbolInPattern_throwsIllegalArgumentException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern("RX"); // 'X' is undefined!

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    builder.createRecipe(Constant.id("test_undefined_symbol")));
            assertTrue(ex.getMessage().contains("Pattern references symbol 'X' but it's not defined in the key"));
        }

        @Test
        @DisplayName("Key defining symbol unused in pattern throws IllegalArgumentException")
        void unusedDefinedSymbol_throwsIllegalArgumentException() {
            PartAssemblerRecipeBuilder builder = PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .define('U', Items.IRON_INGOT) // 'U' is never used in pattern!
                    .pattern("R");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    builder.createRecipe(Constant.id("test_unused_symbol")));
            assertTrue(ex.getMessage().contains("Key defines symbol 'U' that is not used in any pattern rows"));
        }

        @Test
        @DisplayName("Null scrap byproduct defaults safely to ItemStack.EMPTY without NPE")
        void nullScrapByproduct_defaultsToEmpty() {
            PartAssemblerRecipe recipe = (PartAssemblerRecipe) PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern("R")
                    .scrapByproduct((ItemStack) null)
                    .createRecipe(Constant.id("test_null_scrap"));

            assertNotNull(recipe.getScrapByproduct());
            assertTrue(recipe.getScrapByproduct().isEmpty());
        }

        @Test
        @DisplayName("Null part stats defaults to Optional.empty() without NPE")
        void nullPartStats_defaultsToEmptyOptional() {
            PartAssemblerRecipe recipe = (PartAssemblerRecipe) PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                    .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                    .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                    .define('R', Items.REDSTONE_TORCH)
                    .pattern("R")
                    .partStats(null)
                    .createRecipe(Constant.id("test_null_stats"));

            assertNotNull(recipe.getPartStats());
            assertTrue(recipe.getPartStats().isEmpty());
        }

        @Test
        @DisplayName("Assembly time <= 0 is clamped to >= 1")
        void nonPositiveAssemblyTime_clampedToAtLeastOne() {
            int[] testTimes = {-1000, -1, 0};
            for (int time : testTimes) {
                PartAssemblerRecipe recipe = (PartAssemblerRecipe) PartAssemblerRecipeBuilder.create(GCItems.NOSE_CONE)
                        .unlockedBy("has_plate", InventoryChangeTrigger.TriggerInstance.hasItems(GCItems.TIER_1_HEAVY_DUTY_PLATE))
                        .schematic(GCItems.BASIC_ROCKET_CONE_SCHEMATIC)
                        .define('R', Items.REDSTONE_TORCH)
                        .pattern("R")
                        .assemblyTime(time)
                        .createRecipe(Constant.id("test_clamp_" + Math.abs(time)));

                assertEquals(1, recipe.getAssemblyTime(), "Assembly time should be clamped to 1 for input: " + time);
            }
        }
    }

    @Nested
    @DisplayName("Generated Recipe JSON Codec Round-Trip Fidelity")
    class GeneratedRecipeRoundTrip {

        private static final String[] GENERATED_RECIPE_FILES = {
                "nose_cone.json",
                "heavy_nose_cone.json",
                "rocket_fin.json",
                "heavy_rocket_fin.json",
                "heavy_rocket_fin_alt.json",
                "rocket_engine.json",
                "heavy_rocket_engine.json",
                "rocket_booster.json"
        };

        @Test
        @DisplayName("All 8 generated Part Assembler recipe files round-trip with 100% fidelity via CODEC")
        void testAllGeneratedRecipes_roundTripFidelity() throws Exception {
            for (String recipeFileName : GENERATED_RECIPE_FILES) {
                Path filePath = GENERATED_RECIPES_DIR.resolve(recipeFileName);
                assertTrue(Files.exists(filePath), "Generated recipe file does not exist: " + filePath);

                JsonElement originalJson;
                try (Reader reader = Files.newBufferedReader(filePath)) {
                    originalJson = JsonParser.parseReader(reader);
                }
                assertTrue(originalJson.isJsonObject(), "Recipe must be a JsonObject: " + recipeFileName);
                JsonObject originalObj = originalJson.getAsJsonObject();

                assertEquals("galacticraft:part_assembler", originalObj.get("type").getAsString(),
                        "Recipe type must be galacticraft:part_assembler in " + recipeFileName);

                // 1. Decode original JSON via CODEC
                PartAssemblerRecipe recipe = PartAssemblerRecipe.Serializer.CODEC.codec()
                        .parse(registryOps, originalJson)
                        .getOrThrow(err -> new AssertionError("Failed to decode " + recipeFileName + ": " + err));

                assertNotNull(recipe, "Decoded recipe must not be null for " + recipeFileName);
                assertTrue(recipe.getAssemblyTime() >= 1, "Assembly time must be >= 1 tick for " + recipeFileName);
                assertNotNull(recipe.getSchematic(), "Schematic must not be null for " + recipeFileName);
                assertFalse(recipe.getSchematic().isEmpty(), "Schematic must not be empty for " + recipeFileName);
                assertNotNull(recipe.getPattern(), "Pattern must not be null for " + recipeFileName);
                assertTrue(recipe.getPattern().width() >= 1 && recipe.getPattern().width() <= 3, "Width must be 1..3 for " + recipeFileName);
                assertTrue(recipe.getPattern().height() >= 1 && recipe.getPattern().height() <= 3, "Height must be 1..3 for " + recipeFileName);

                // 2. Validate Result and Data Components attached
                ItemStack resultStack = recipe.getResultItem(registryAccess);
                assertFalse(resultStack.isEmpty(), "Result item must not be empty for " + recipeFileName);
                assertTrue(recipe.getPartStats().isPresent(), "All GC5 rocket parts in Part Assembler must configure RocketPartStats in " + recipeFileName);

                RocketPartStats stats = recipe.getPartStats().get();
                assertEquals(stats, resultStack.get(GCDataComponents.ROCKET_PART_STATS),
                        "Result item must carry exact configured RocketPartStats in " + recipeFileName);

                PartIntegrity integrity = resultStack.get(GCDataComponents.PART_INTEGRITY);
                assertNotNull(integrity, "Result item must carry PartIntegrity in " + recipeFileName);
                assertEquals(100.0, integrity.currentIntegrity(), 1e-6, "Fabricated part must have 100.0 current integrity in " + recipeFileName);
                assertTrue(integrity.isPristine(), "Fabricated part must be pristine in " + recipeFileName);

                // 3. Re-encode to JSON
                JsonElement reEncodedJson = PartAssemblerRecipe.Serializer.CODEC.codec()
                        .encodeStart(registryOps, recipe)
                        .getOrThrow(err -> new AssertionError("Failed to re-encode " + recipeFileName + ": " + err));

                assertTrue(reEncodedJson.isJsonObject());

                // 4. Decode again from re-encoded JSON and assert parity
                PartAssemblerRecipe roundTripRecipe = PartAssemblerRecipe.Serializer.CODEC.codec()
                        .parse(registryOps, reEncodedJson)
                        .getOrThrow(err -> new AssertionError("Failed to decode re-encoded " + recipeFileName + ": " + err));

                assertEquals(recipe.getAssemblyTime(), roundTripRecipe.getAssemblyTime(), "Assembly time mismatch for " + recipeFileName);
                assertEquals(recipe.getPartStats(), roundTripRecipe.getPartStats(), "PartStats mismatch for " + recipeFileName);
                assertEquals(recipe.getScrapByproduct().getItem(), roundTripRecipe.getScrapByproduct().getItem(), "Scrap item mismatch for " + recipeFileName);
                assertEquals(recipe.getScrapByproduct().getCount(), roundTripRecipe.getScrapByproduct().getCount(), "Scrap count mismatch for " + recipeFileName);
                assertEquals(recipe.getPattern().width(), roundTripRecipe.getPattern().width(), "Pattern width mismatch for " + recipeFileName);
                assertEquals(recipe.getPattern().height(), roundTripRecipe.getPattern().height(), "Pattern height mismatch for " + recipeFileName);
                assertEquals(recipe.getResultItem(registryAccess).getItem(), roundTripRecipe.getResultItem(registryAccess).getItem(), "Result item mismatch for " + recipeFileName);
                assertEquals(recipe.getResultItem(registryAccess).getCount(), roundTripRecipe.getResultItem(registryAccess).getCount(), "Result count mismatch for " + recipeFileName);
            }
        }

        @Test
        @DisplayName("All 8 generated Part Assembler recipe files are present and tracked on disk")
        void allEightGeneratedFilesExist() {
            for (String file : GENERATED_RECIPE_FILES) {
                Path p = GENERATED_RECIPES_DIR.resolve(file);
                assertTrue(Files.isRegularFile(p), "Missing expected generated recipe: " + file);
            }
        }
    }

    @Nested
    @DisplayName("Generated Tags & Physical Stats Validation")
    class TagsAndStatsValidation {

        @Test
        @DisplayName("rocket_schematics.json tag contains all 9 registered schematics")
        void rocketSchematicsTag_containsAllNineSchematics() throws Exception {
            Path tagPath = GENERATED_TAGS_DIR.resolve("rocket_schematics.json");
            assertTrue(Files.exists(tagPath), "Missing rocket_schematics.json tag file at " + tagPath);

            JsonElement tagJson;
            try (Reader reader = Files.newBufferedReader(tagPath)) {
                tagJson = JsonParser.parseReader(reader);
            }
            assertTrue(tagJson.isJsonObject());
            JsonArray values = tagJson.getAsJsonObject().getAsJsonArray("values");
            assertNotNull(values, "Tag must have 'values' array");
            assertEquals(9, values.size(), "Tag must contain exactly 9 schematics");

            List<String> schematicIds = new ArrayList<>();
            values.forEach(v -> schematicIds.add(v.getAsString()));

            assertTrue(schematicIds.contains("galacticraft:basic_rocket_cone_schematic"));
            assertTrue(schematicIds.contains("galacticraft:basic_rocket_body_schematic"));
            assertTrue(schematicIds.contains("galacticraft:basic_rocket_fins_schematic"));
            assertTrue(schematicIds.contains("galacticraft:basic_rocket_engine_schematic"));
            assertTrue(schematicIds.contains("galacticraft:tier_2_rocket_schematic"));
            assertTrue(schematicIds.contains("galacticraft:tier_3_rocket_schematic"));
            assertTrue(schematicIds.contains("galacticraft:cargo_rocket_schematic"));
            assertTrue(schematicIds.contains("galacticraft:moon_buggy_schematic"));
            assertTrue(schematicIds.contains("galacticraft:astro_miner_schematic"));
        }

        @Test
        @DisplayName("Rocket Booster recipe defines fuel capacity droplets and thrust correctly")
        void rocketBooster_physicalStatsVerified() throws Exception {
            Path boosterPath = GENERATED_RECIPES_DIR.resolve("rocket_booster.json");
            JsonElement boosterJson;
            try (Reader reader = Files.newBufferedReader(boosterPath)) {
                boosterJson = JsonParser.parseReader(reader);
            }

            PartAssemblerRecipe recipe = PartAssemblerRecipe.Serializer.CODEC.codec()
                    .parse(registryOps, boosterJson)
                    .getOrThrow(err -> new AssertionError("Failed to decode rocket_booster.json: " + err));

            assertTrue(recipe.getPartStats().isPresent());
            RocketPartStats stats = recipe.getPartStats().get();
            assertEquals(1800.0, stats.thrustKn());
            assertEquals(1100.0, stats.dryWeightKg());
            assertEquals(405000L, stats.fuelCapacityDroplets());
            assertEquals(4.0, stats.pressureToleranceAtm());
            assertEquals(2600.0, stats.thermalToleranceKelvin());

            assertEquals(2, recipe.getScrapByproduct().getCount());
            assertEquals(GCItems.COMPRESSED_IRON, recipe.getScrapByproduct().getItem());
        }
    }
}
