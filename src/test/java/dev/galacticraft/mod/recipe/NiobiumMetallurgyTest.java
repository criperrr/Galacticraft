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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Niobium Metallurgy & Overworld Worldgen DataGen Validation")
class NiobiumMetallurgyTest {
    private static final Path GENERATED_DATA_DIR = resolveProjectPath("src/main/generated/data/galacticraft");
    private static final Path GENERATED_COMMON_TAGS_DIR = resolveProjectPath("src/main/generated/data/c/tags");
    private static final Path GENERATED_MC_TAGS_DIR = resolveProjectPath("src/main/generated/data/minecraft/tags");
    private static final Path GENERATED_RECIPES_DIR = GENERATED_DATA_DIR.resolve("recipe");
    private static final Path EMI_DEFAULTS_PATH = resolveProjectPath("src/main/generated/assets/emi/recipe/defaults/galacticraft.json");

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
    @DisplayName("Overworld Niobium configured and placed features are generated with expected height ranges")
    void niobiumWorldgenFeaturesGenerated() throws Exception {
        JsonObject configured = readJson(GENERATED_DATA_DIR.resolve("worldgen/configured_feature/ore_niobium.json"));
        assertEquals("minecraft:ore", configured.get("type").getAsString());
        assertEquals(6, configured.getAsJsonObject("config").get("size").getAsInt());

        JsonObject configuredDeep = readJson(GENERATED_DATA_DIR.resolve("worldgen/configured_feature/ore_niobium_deep.json"));
        assertEquals("minecraft:ore", configuredDeep.get("type").getAsString());
        assertEquals(9, configuredDeep.getAsJsonObject("config").get("size").getAsInt());

        JsonObject placed = readJson(GENERATED_DATA_DIR.resolve("worldgen/placed_feature/ore_niobium.json"));
        assertEquals("galacticraft:ore_niobium", placed.get("feature").getAsString());

        JsonObject placedDeep = readJson(GENERATED_DATA_DIR.resolve("worldgen/placed_feature/ore_niobium_deep.json"));
        assertEquals("galacticraft:ore_niobium_deep", placedDeep.get("feature").getAsString());
    }

    @Test
    @DisplayName("Niobium ores have fortune-enabled loot tables and require iron pickaxe")
    void niobiumLootTablesAndMiningTagsGenerated() throws Exception {
        for (String ore : List.of("niobium_ore", "deepslate_niobium_ore")) {
            Path lootPath = GENERATED_DATA_DIR.resolve("loot_table/blocks/" + ore + ".json");
            JsonObject loot = readJson(lootPath);
            String rawJson = loot.toString();
            assertTrue(rawJson.contains("galacticraft:raw_niobium"), "Ore loot table must drop raw_niobium: " + ore);
            assertTrue(rawJson.contains("minecraft:fortune"), "Ore loot table must support Fortune: " + ore);
            assertTrue(rawJson.contains("minecraft:silk_touch"), "Ore loot table must support Silk Touch: " + ore);
        }

        JsonObject needsIron = readJson(GENERATED_MC_TAGS_DIR.resolve("block/needs_iron_tool.json"));
        String ironStr = needsIron.getAsJsonArray("values").toString();
        assertTrue(ironStr.contains("galacticraft:niobium_ore"));
        assertTrue(ironStr.contains("galacticraft:deepslate_niobium_ore"));
        assertTrue(ironStr.contains("galacticraft:niobium_block"));
        assertTrue(ironStr.contains("galacticraft:ferroniobium_block"));
        assertTrue(ironStr.contains("galacticraft:raw_niobium_block"));

        JsonObject commonOres = readJson(GENERATED_COMMON_TAGS_DIR.resolve("block/ores/niobium.json"));
        assertTrue(commonOres.getAsJsonArray("values").toString().contains("galacticraft:niobium_ore"));
        assertTrue(commonOres.getAsJsonArray("values").toString().contains("galacticraft:deepslate_niobium_ore"));

        JsonObject commonIngots = readJson(GENERATED_COMMON_TAGS_DIR.resolve("item/ingots/niobium.json"));
        assertTrue(commonIngots.getAsJsonArray("values").toString().contains("galacticraft:niobium_ingot"));
    }

    @Test
    @DisplayName("All 3-stage Niobium metallurgy recipes and 4-pillar integrations are generated cleanly")
    void niobiumMetallurgyRecipesGenerated() throws Exception {
        // Stage 1: Calcination -> niobium_oxide (smelting & blasting)
        for (String input : List.of("raw_niobium", "niobium_ore", "deepslate_niobium_ore")) {
            JsonObject smelt = readJson(GENERATED_RECIPES_DIR.resolve("niobium_oxide_from_smelting_" + input + ".json"));
            assertEquals("minecraft:smelting", smelt.get("type").getAsString());
            assertEquals("galacticraft:niobium_oxide", smelt.getAsJsonObject("result").get("id").getAsString());

            JsonObject blast = readJson(GENERATED_RECIPES_DIR.resolve("niobium_oxide_from_blasting_" + input + ".json"));
            assertEquals("minecraft:blasting", blast.get("type").getAsString());
            assertEquals("galacticraft:niobium_oxide", blast.getAsJsonObject("result").get("id").getAsString());
        }

        // Stage 2: Aluminothermic & Carbothermic reduction + alloys
        JsonObject ferroniobium = readJson(GENERATED_RECIPES_DIR.resolve("compressing/ferroniobium_ingot.json"));
        assertEquals("galacticraft:ferroniobium_ingot", ferroniobium.getAsJsonObject("result").get("id").getAsString());
        assertEquals(3, ferroniobium.getAsJsonArray("ingredients").size());

        JsonObject compFerroniobium = readJson(GENERATED_RECIPES_DIR.resolve("compressing/compressed_ferroniobium.json"));
        assertEquals("galacticraft:compressed_ferroniobium", compFerroniobium.getAsJsonObject("result").get("id").getAsString());

        JsonObject pureNiobium = readJson(GENERATED_RECIPES_DIR.resolve("compressing/niobium_ingot.json"));
        assertEquals("galacticraft:niobium_ingot", pureNiobium.getAsJsonObject("result").get("id").getAsString());
        assertEquals(300, pureNiobium.get("time").getAsInt());

        JsonObject c103 = readJson(GENERATED_RECIPES_DIR.resolve("compressing/compressed_niobium.json"));
        assertEquals("galacticraft:compressed_niobium", c103.getAsJsonObject("result").get("id").getAsString());

        JsonObject c103Alt = readJson(GENERATED_RECIPES_DIR.resolve("compressing/compressed_niobium_from_meteoric_iron.json"));
        assertEquals("galacticraft:compressed_niobium", c103Alt.getAsJsonObject("result").get("id").getAsString());

        JsonObject coil = readJson(GENERATED_RECIPES_DIR.resolve("superconducting_coil.json"));
        assertEquals("galacticraft:superconducting_coil", coil.getAsJsonObject("result").get("id").getAsString());

        // Stage 3: 4-pillar integrations
        JsonObject advWafer = readJson(GENERATED_RECIPES_DIR.resolve("fabrication/advanced_wafer.json"));
        assertEquals("galacticraft:niobium_oxide", advWafer.getAsJsonObject("ingredient").get("item").getAsString());

        JsonObject t1Plate = readJson(GENERATED_RECIPES_DIR.resolve("compressing/heavy_plating.json"));
        assertTrue(t1Plate.toString().contains("galacticraft:compressed_ferroniobium"));

        JsonObject launchPad = readJson(GENERATED_RECIPES_DIR.resolve("reinforced_launch_pad.json"));
        assertTrue(launchPad.toString().contains("galacticraft:compressed_ferroniobium"));

        JsonObject gantryTruss = readJson(GENERATED_RECIPES_DIR.resolve("gantry_truss.json"));
        assertTrue(gantryTruss.toString().contains("galacticraft:compressed_ferroniobium"));

        JsonObject elecComp = readJson(GENERATED_RECIPES_DIR.resolve("electric_compressor.json"));
        assertTrue(elecComp.toString().contains("galacticraft:compressed_ferroniobium"));

        JsonObject energyMod = readJson(GENERATED_RECIPES_DIR.resolve("energy_storage_module.json"));
        assertTrue(energyMod.toString().contains("galacticraft:superconducting_coil"));

        JsonObject telepad = readJson(GENERATED_RECIPES_DIR.resolve("short_range_telepad.json"));
        assertTrue(telepad.toString().contains("galacticraft:superconducting_coil"));

        JsonObject arcFurnace = readJson(GENERATED_RECIPES_DIR.resolve("electric_arc_furnace.json"));
        assertTrue(arcFurnace.toString().contains("galacticraft:superconducting_coil"));
    }

    @Test
    @DisplayName("Niobium progression graph has no circular dependencies and EMI defaults are duplicate-free")
    void noDependencyCyclesAndCleanEmiDefaults() throws Exception {
        JsonObject emiJson = readJson(EMI_DEFAULTS_PATH);
        JsonArray added = emiJson.getAsJsonArray("added");
        assertNotNull(added);
        Set<String> seen = new HashSet<>();
        for (JsonElement elem : added) {
            String id = elem.getAsString();
            assertTrue(seen.add(id), "Duplicate recipe ID in EMI defaults: " + id);
        }

        Map<String, List<String>> graph = new HashMap<>();
        for (String recipeFile : List.of(
                "superconducting_coil.json",
                "energy_storage_module.json",
                "short_range_telepad.json",
                "electric_arc_furnace.json",
                "electric_compressor.json",
                "reinforced_launch_pad.json",
                "gantry_truss.json")) {
            JsonObject obj = readJson(GENERATED_RECIPES_DIR.resolve(recipeFile));
            String resultId = obj.getAsJsonObject("result").get("id").getAsString();
            Set<String> deps = new HashSet<>();
            if (obj.has("key")) {
                for (var entry : obj.getAsJsonObject("key").entrySet()) {
                    JsonObject val = entry.getValue().getAsJsonObject();
                    if (val.has("item")) {
                        deps.add(val.get("item").getAsString());
                    }
                }
            }
            assertFalse(deps.contains(resultId), "Direct self-cycle in recipe: " + recipeFile);
            graph.put(resultId, List.copyOf(deps));
        }

        for (String node : graph.keySet()) {
            assertFalse(hasCycle(node, graph, new HashSet<>(), new HashSet<>()), "Cycle detected starting at: " + node);
        }
    }

    private static boolean hasCycle(String node, Map<String, List<String>> graph, Set<String> visiting, Set<String> visited) {
        if (!visiting.add(node)) {
            return true;
        }
        if (visited.contains(node)) {
            visiting.remove(node);
            return false;
        }
        for (String dep : graph.getOrDefault(node, List.of())) {
            if (graph.containsKey(dep) && hasCycle(dep, graph, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }
}
