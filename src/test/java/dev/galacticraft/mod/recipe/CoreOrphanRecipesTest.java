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

@DisplayName("Core Orphan Recipes DataGen Validation")
class CoreOrphanRecipesTest {
    private static final Path GENERATED_RECIPES_DIR = resolveProjectPath("src/main/generated/data/galacticraft/recipe");
    private static final Path EMI_DEFAULTS_PATH = resolveProjectPath("src/main/generated/assets/emi/recipe/defaults/galacticraft.json");

    private static Path resolveProjectPath(String relativePath) {
        Path direct = Paths.get(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }
        Path parentRelative = Paths.get("..").resolve(relativePath).normalize();
        return Files.exists(parentRelative) ? parentRelative : direct;
    }

    private static JsonObject readRecipeJson(String fileName) throws Exception {
        Path filePath = GENERATED_RECIPES_DIR.resolve(fileName);
        assertTrue(Files.isRegularFile(filePath), "Expected generated recipe JSON to exist: " + filePath);
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonElement element = JsonParser.parseReader(reader);
            assertTrue(element.isJsonObject(), "Recipe JSON must be an object: " + fileName);
            return element.getAsJsonObject();
        }
    }

    @Test
    @DisplayName("All newly added orphan-fix recipes are generated with expected outputs and ingredients")
    void allOrphanFixRecipesGeneratedCorrectly() throws Exception {
        // 1. radioisotope_core
        JsonObject radioisotopeCore = readRecipeJson("radioisotope_core.json");
        assertEquals("minecraft:crafting_shaped", radioisotopeCore.get("type").getAsString());
        assertEquals("galacticraft:radioisotope_core", radioisotopeCore.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, radioisotopeCore.getAsJsonObject("result").get("count").getAsInt());

        // 2. olivine_glass (count 2)
        JsonObject olivineGlass = readRecipeJson("olivine_glass.json");
        assertEquals("minecraft:crafting_shaped", olivineGlass.get("type").getAsString());
        assertEquals("galacticraft:olivine_glass", olivineGlass.getAsJsonObject("result").get("id").getAsString());
        assertEquals(2, olivineGlass.getAsJsonObject("result").get("count").getAsInt());

        // 3. shield_controller
        JsonObject shieldController = readRecipeJson("shield_controller.json");
        assertEquals("minecraft:crafting_shaped", shieldController.get("type").getAsString());
        assertEquals("galacticraft:shield_controller", shieldController.getAsJsonObject("result").get("id").getAsString());
        assertEquals("galacticraft:atomic_battery", shieldController.getAsJsonObject("key").getAsJsonObject("A").get("item").getAsString());

        // 4. player_transport_tube (count 4)
        JsonObject transportTube = readRecipeJson("player_transport_tube.json");
        assertEquals("minecraft:crafting_shaped", transportTube.get("type").getAsString());
        assertEquals("galacticraft:player_transport_tube", transportTube.getAsJsonObject("result").get("id").getAsString());
        assertEquals(4, transportTube.getAsJsonObject("result").get("count").getAsInt());

        // 5. advanced_solar_panel consumes solar_array_panel
        JsonObject advSolar = readRecipeJson("advanced_solar_panel.json");
        assertEquals("galacticraft:solar_array_panel", advSolar.getAsJsonObject("key").getAsJsonObject("F").get("item").getAsString());

        // 6. compressed_steel_from_carbon_fragments (faster 100-tick compressing recipe)
        JsonObject steelCarbon = readRecipeJson("compressing/compressed_steel_from_carbon_fragments.json");
        assertEquals("galacticraft:compressing_shapeless", steelCarbon.get("type").getAsString());
        assertEquals("galacticraft:compressed_steel", steelCarbon.getAsJsonObject("result").get("id").getAsString());
        assertEquals(100, steelCarbon.get("time").getAsInt());
        assertEquals(2, steelCarbon.getAsJsonArray("ingredients").size());

        // 7. mercury_sulfide_ore smelting & blasting -> solar_dust with 0.9 XP
        JsonObject sulfideSmelt = readRecipeJson("solar_dust_from_smelting_mercury_sulfide_ore.json");
        assertEquals("minecraft:smelting", sulfideSmelt.get("type").getAsString());
        assertEquals("galacticraft:mercury_sulfide_ore", sulfideSmelt.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals("galacticraft:solar_dust", sulfideSmelt.getAsJsonObject("result").get("id").getAsString());
        assertEquals(0.9f, sulfideSmelt.get("experience").getAsFloat(), 1e-5f);

        JsonObject sulfideBlast = readRecipeJson("solar_dust_from_blasting_mercury_sulfide_ore.json");
        assertEquals("minecraft:blasting", sulfideBlast.get("type").getAsString());
        assertEquals("galacticraft:mercury_sulfide_ore", sulfideBlast.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals("galacticraft:solar_dust", sulfideBlast.getAsJsonObject("result").get("id").getAsString());
        assertEquals(0.9f, sulfideBlast.get("experience").getAsFloat(), 1e-5f);

        // 8. lunar_sapphire integrated into beam_core_sapphire (2x) and sensor_lens
        JsonObject beamCoreSapphire = readRecipeJson("beam_core_sapphire.json");
        assertEquals("galacticraft:beam_core", beamCoreSapphire.getAsJsonObject("result").get("id").getAsString());
        assertEquals(2, beamCoreSapphire.getAsJsonObject("result").get("count").getAsInt());
        assertEquals("c:gems/lunar_sapphire", beamCoreSapphire.getAsJsonObject("key").getAsJsonObject("S").get("tag").getAsString());

        JsonObject sensorLens = readRecipeJson("sensor_lens.json");
        assertEquals("galacticraft:sensor_lens", sensorLens.getAsJsonObject("result").get("id").getAsString());
        assertEquals("c:gems/lunar_sapphire", sensorLens.getAsJsonObject("key").getAsJsonObject("S").get("tag").getAsString());
    }

    @Test
    @DisplayName("EMI default recipes contain no duplicate IDs and include new core items without cycles")
    void emiDefaultsHaveNoDuplicatesAndNoCycles() throws Exception {
        assertTrue(Files.isRegularFile(EMI_DEFAULTS_PATH), "EMI default recipe file missing: " + EMI_DEFAULTS_PATH);
        JsonObject emiJson;
        try (Reader reader = Files.newBufferedReader(EMI_DEFAULTS_PATH)) {
            emiJson = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonArray added = emiJson.getAsJsonArray("added");
        assertNotNull(added);
        Set<String> seen = new HashSet<>();
        for (JsonElement elem : added) {
            String id = elem.getAsString();
            assertTrue(seen.add(id), "Duplicate recipe ID in EMI defaults: " + id);
        }

        assertTrue(seen.contains("galacticraft:radioisotope_core"));
        assertTrue(seen.contains("galacticraft:olivine_glass"));
        assertTrue(seen.contains("galacticraft:shield_controller"));
        assertTrue(seen.contains("galacticraft:player_transport_tube"));

        // Verify no dependency cycles across the newly wired crafting chains
        Map<String, List<String>> graph = new HashMap<>();
        for (String recipeFile : List.of(
                "radioisotope_core.json",
                "atomic_battery.json",
                "shield_controller.json",
                "olivine_glass.json",
                "olivine_glass_pane.json",
                "player_transport_tube.json",
                "advanced_solar_panel.json",
                "solar_array_panel.json",
                "beam_core_sapphire.json",
                "sensor_lens.json")) {
            JsonObject obj = readRecipeJson(recipeFile);
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
