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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Planetary Multi-Ore Stratigraphy DataGen Validation")
class PlanetaryMultiOreStratigraphyTest {
    private static final Path GENERATED_DATA_DIR = resolveProjectPath("src/main/generated/data/galacticraft");
    private static final Path GENERATED_ASSETS_DIR = resolveProjectPath("src/main/generated/assets/galacticraft");
    private static final Path RESOURCES_ASSETS_DIR = resolveProjectPath("src/main/resources/assets/galacticraft");
    private static final Path GENERATED_RECIPES_DIR = GENERATED_DATA_DIR.resolve("recipe");
    private static final Path GENERATED_BIOME_DIR = GENERATED_DATA_DIR.resolve("worldgen/biome");
    private static final Path GENERATED_PLACED_DIR = GENERATED_DATA_DIR.resolve("worldgen/placed_feature");
    private static final Path GENERATED_CONFIGURED_DIR = GENERATED_DATA_DIR.resolve("worldgen/configured_feature");

    private static final Map<String, String> NEW_ORE_SMELTING_OUTPUTS = Map.ofEntries(
            // Moon
            Map.entry("moon_aluminum_ore", "aluminum_ingot"),
            Map.entry("moon_silicon_ore", "silicon"),
            Map.entry("moon_ilmenite_ore", "titanium_ingot"),
            Map.entry("moon_niobium_ore", "niobium_oxide"),
            Map.entry("moon_gold_ore", "gold_ingot"),
            Map.entry("moon_redstone_ore", "redstone"),
            Map.entry("moon_iron_ore", "iron_ingot"),
            // Mars
            Map.entry("mars_silicon_ore", "silicon"),
            Map.entry("mars_aluminum_ore", "aluminum_ingot"),
            Map.entry("mars_niobium_ore", "niobium_oxide"),
            Map.entry("mars_gold_ore", "gold_ingot"),
            Map.entry("mars_redstone_ore", "redstone"),
            Map.entry("mars_titanium_ore", "titanium_ingot"),
            Map.entry("mars_graphite_ore", "carbon_fragments"),
            // Asteroids
            Map.entry("asteroid_niobium_ore", "niobium_oxide"),
            Map.entry("asteroid_gold_ore", "gold_ingot"),
            Map.entry("asteroid_graphite_ore", "carbon_fragments"),
            Map.entry("asteroid_copper_ore", "copper_ingot"),
            Map.entry("asteroid_redstone_ore", "redstone"),
            // Mercury
            Map.entry("mercury_niobium_ore", "niobium_oxide"),
            Map.entry("mercury_gold_ore", "gold_ingot"),
            Map.entry("mercury_lead_ore", "lead_ingot"),
            // Venus
            Map.entry("venus_iron_ore", "iron_ingot"),
            Map.entry("venus_niobium_ore", "niobium_oxide"),
            Map.entry("venus_gold_ore", "gold_ingot"),
            Map.entry("venus_redstone_ore", "redstone"),
            Map.entry("venus_silicon_ore", "silicon"),
            Map.entry("venus_titanium_ore", "titanium_ingot")
    );

    private static Path resolveProjectPath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }
        Path parentRelative = Paths.get("..").resolve(relativePath).normalize();
        return Files.exists(parentRelative) ? parentRelative : direct;
    }

    private static JsonObject readJson(Path filePath) throws Exception {
        assertTrue(Files.isRegularFile(filePath), "Expected generated JSON to exist: " + filePath);
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonElement element = JsonParser.parseReader(reader);
            assertTrue(element.isJsonObject(), "JSON must be an object: " + filePath);
            return element.getAsJsonObject();
        }
    }

    @Test
    @DisplayName("All 28 new planetary ore blocks have textures, models, Silk Touch + Fortune loot tables, and smelting/blasting recipes")
    void allNewPlanetaryOresHaveCompleteAssetsLootAndRecipes() throws Exception {
        for (Map.Entry<String, String> entry : NEW_ORE_SMELTING_OUTPUTS.entrySet()) {
            String ore = entry.getKey();
            String smeltOutput = entry.getValue();

            // Texture, blockstate, block model, item model
            assertTrue(Files.isRegularFile(RESOURCES_ASSETS_DIR.resolve("textures/block/" + ore + ".png")), "Missing texture for " + ore);
            assertTrue(Files.isRegularFile(GENERATED_ASSETS_DIR.resolve("blockstates/" + ore + ".json")), "Missing blockstate for " + ore);
            assertTrue(Files.isRegularFile(GENERATED_ASSETS_DIR.resolve("models/block/" + ore + ".json")), "Missing block model for " + ore);
            assertTrue(Files.isRegularFile(GENERATED_ASSETS_DIR.resolve("models/item/" + ore + ".json")), "Missing item model for " + ore);

            // Loot table with Silk Touch and Fortune
            JsonObject loot = readJson(GENERATED_DATA_DIR.resolve("loot_table/blocks/" + ore + ".json"));
            String lootStr = loot.toString();
            assertTrue(lootStr.contains("minecraft:silk_touch"), "Loot table missing Silk Touch: " + ore);
            assertTrue(lootStr.contains("minecraft:fortune"), "Loot table missing Fortune: " + ore);

            // Smelting & Blasting recipes
            JsonObject smelt = readJson(GENERATED_RECIPES_DIR.resolve(smeltOutput + "_from_smelting_" + ore + ".json"));
            assertEquals("minecraft:smelting", smelt.get("type").getAsString());

            JsonObject blast = readJson(GENERATED_RECIPES_DIR.resolve(smeltOutput + "_from_blasting_" + ore + ".json"));
            assertEquals("minecraft:blasting", blast.get("type").getAsString());
        }
    }

    @Test
    @DisplayName("All Moon, Mars, Mercury, and Venus biomes reference only valid PlacedFeatures backed by valid ConfiguredFeatures")
    void innerPlanetBiomesReferenceValidPlacedAndConfiguredFeatures() throws Exception {
        List<String> innerPrefixes = List.of("moon_", "lunar_", "basaltic_", "comet_", "olivine_", "mars_", "mercury_", "venus_");
        int checkedBiomes = 0;

        try (var stream = Files.list(GENERATED_BIOME_DIR)) {
            for (Path biomeFile : stream.filter(p -> p.toString().endsWith(".json")).toList()) {
                String name = biomeFile.getFileName().toString();
                if (innerPrefixes.stream().noneMatch(name::startsWith)) {
                    continue;
                }
                checkedBiomes++;
                JsonObject biomeJson = readJson(biomeFile);
                JsonArray featureSteps = biomeJson.getAsJsonArray("features");
                for (JsonElement stepElem : featureSteps) {
                    for (JsonElement featureElem : stepElem.getAsJsonArray()) {
                        String featureId = featureElem.getAsString();
                        if (!featureId.startsWith("galacticraft:")) {
                            continue;
                        }
                        String idPath = featureId.substring("galacticraft:".length());
                        JsonObject placedJson = readJson(GENERATED_PLACED_DIR.resolve(idPath + ".json"));
                        String configuredId = placedJson.get("feature").getAsString();
                        if (configuredId.startsWith("galacticraft:")) {
                            String cfgPath = configuredId.substring("galacticraft:".length());
                            readJson(GENERATED_CONFIGURED_DIR.resolve(cfgPath + ".json"));
                        }
                    }
                }
            }
        }

        assertTrue(checkedBiomes >= 18, "Expected at least 18 inner-planet biomes to be verified, found: " + checkedBiomes);
    }

    @Test
    @DisplayName("Stratigraphy abundance hierarchy matches planetary differentiation rules")
    void planetaryAbundanceHierarchyMatchesSpec() throws Exception {
        // Mars: Iron (2.5x) > Aluminum (0.7x) > Graphite (0.2x)
        int marsIronCount = getPlacementCount("ore_iron_middle_mars");
        int marsAluminumCount = getPlacementCount("ore_aluminum_mars");
        int marsGraphiteCount = getPlacementCount("ore_graphite_mars");
        assertTrue(marsIronCount > marsAluminumCount && marsAluminumCount > marsGraphiteCount,
                "Mars abundance hierarchy invalid: iron=" + marsIronCount + ", aluminum=" + marsAluminumCount + ", graphite=" + marsGraphiteCount);

        // Mercury: Native Iron (3.5x) & Graphite (3.5x) > Niobium/Gold (2.0x) > Lead (0.8x)
        int mercuryIron = getPlacementCount("ore_native_iron_mercury");
        int mercuryGraphite = getPlacementCount("ore_graphite_mercury");
        int mercuryNiobium = getPlacementCount("ore_niobium_mercury");
        int mercuryLead = getPlacementCount("ore_lead_mercury");
        assertTrue(mercuryIron > mercuryNiobium && mercuryGraphite > mercuryNiobium && mercuryNiobium > mercuryLead,
                "Mercury abundance hierarchy invalid");

        // Venus: Galena (3.0x) > Gold/Redstone (1.8x) > Titanium (0.8x)
        int venusGalena = getPlacementCount("ore_galena_venus");
        int venusGold = getPlacementCount("ore_gold_venus");
        int venusTitanium = getPlacementCount("ore_titanium_venus");
        assertTrue(venusGalena > venusGold && venusGold > venusTitanium,
                "Venus abundance hierarchy invalid");

        // Moon: Aluminum (2.0x) > Niobium/Gold/Redstone (0.3x), and Highlands has bonus aluminum
        int moonAluminum = getPlacementCount("ore_aluminum_moon");
        int moonNiobium = getPlacementCount("ore_niobium_moon");
        assertTrue(moonAluminum > moonNiobium, "Moon aluminum must be more abundant than rare niobium");

        JsonObject highlands = readJson(GENERATED_BIOME_DIR.resolve("lunar_highlands.json"));
        assertTrue(highlands.toString().contains("galacticraft:ore_aluminum_highlands_moon"),
                "Lunar Highlands must include anorthosite bonus aluminum feature");
    }

    private static int getPlacementCount(String placedFeatureName) throws Exception {
        JsonObject placed = readJson(GENERATED_PLACED_DIR.resolve(placedFeatureName + ".json"));
        for (JsonElement elem : placed.getAsJsonArray("placement")) {
            JsonObject mod = elem.getAsJsonObject();
            if ("minecraft:count".equals(mod.get("type").getAsString())) {
                return mod.get("count").getAsInt();
            }
        }
        throw new AssertionError("No minecraft:count placement modifier found in " + placedFeatureName);
    }
}
