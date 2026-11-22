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

package dev.galacticraft.mod.content.rocket.part;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.Constant;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lazy-loaded catalog indexing all exported modular rocket part specifications and parametric families
 * from {@code /data/galacticraft/rocket_parts/catalog.json}.
 */
public final class RocketPartCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_CATALOG_RESOURCE = "/data/galacticraft/rocket_parts/catalog.json";
    private static final String ASSET_CATALOG_RESOURCE = "/assets/galacticraft/models/rocket_part/catalog.json";

    private static volatile @Nullable RocketPartCatalog instance;

    private final Map<String, RocketPartSpec> partsByModelId;
    private final List<RocketPartSpec> allParts;
    private final Map<String, RocketPartFamily> familiesById;
    private final Map<RocketPartCategory, List<RocketPartFamily>> familiesByCategory;
    private final Map<RocketPartCategory, RocketPartSpec> defaultsByCategory;

    private RocketPartCatalog(List<RocketPartSpec> loadedSpecs) {
        List<RocketPartSpec> effectiveSpecs = new ArrayList<>(loadedSpecs);

        // Guarantee every category has at least one valid fallback specification
        EnumMap<RocketPartCategory, Boolean> hasCategory = new EnumMap<>(RocketPartCategory.class);
        for (RocketPartSpec spec : effectiveSpecs) {
            hasCategory.put(spec.category(), Boolean.TRUE);
        }
        for (RocketPartCategory category : RocketPartCategory.values()) {
            if (!Boolean.TRUE.equals(hasCategory.get(category))) {
                effectiveSpecs.add(createFallbackSpec(category));
            }
        }

        Map<String, RocketPartSpec> byId = new LinkedHashMap<>(effectiveSpecs.size());
        Map<String, List<RocketPartSpec>> groupedByFamily = new LinkedHashMap<>();

        for (RocketPartSpec spec : effectiveSpecs) {
            byId.put(spec.modelId(), spec);
            String familyKey = spec.category().getSerializedName() + ":" + spec.familyId();
            groupedByFamily.computeIfAbsent(familyKey, ignored -> new ArrayList<>()).add(spec);
        }

        Map<String, RocketPartFamily> byFamilyId = new LinkedHashMap<>();
        EnumMap<RocketPartCategory, List<RocketPartFamily>> byCategory = new EnumMap<>(RocketPartCategory.class);
        for (RocketPartCategory category : RocketPartCategory.values()) {
            byCategory.put(category, new ArrayList<>());
        }

        for (List<RocketPartSpec> group : groupedByFamily.values()) {
            RocketPartSpec representative = group.get(0);
            RocketPartFamily family = RocketPartFamily.fromVariants(
                    representative.familyId(),
                    representative.displayName(),
                    representative.category(),
                    group
            );
            byFamilyId.putIfAbsent(family.familyId(), family);
            byCategory.get(family.category()).add(family);
        }

        Comparator<RocketPartFamily> familyOrder = Comparator
                .comparing(RocketPartFamily::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(RocketPartFamily::familyId);

        EnumMap<RocketPartCategory, List<RocketPartFamily>> immutableByCategory = new EnumMap<>(RocketPartCategory.class);
        EnumMap<RocketPartCategory, RocketPartSpec> defaults = new EnumMap<>(RocketPartCategory.class);
        for (RocketPartCategory category : RocketPartCategory.values()) {
            List<RocketPartFamily> list = byCategory.get(category);
            list.sort(familyOrder);
            immutableByCategory.put(category, List.copyOf(list));
            defaults.put(category, selectDefaultSpec(category, list));
        }

        this.partsByModelId = Collections.unmodifiableMap(byId);
        this.allParts = List.copyOf(byId.values());
        this.familiesById = Collections.unmodifiableMap(byFamilyId);
        this.familiesByCategory = Collections.unmodifiableMap(immutableByCategory);
        this.defaultsByCategory = Collections.unmodifiableMap(defaults);
    }

    public static @NotNull RocketPartCatalog getInstance() {
        RocketPartCatalog current = instance;
        if (current != null) {
            return current;
        }
        synchronized (RocketPartCatalog.class) {
            current = instance;
            if (current == null) {
                current = loadDefault();
                instance = current;
            }
            return current;
        }
    }

    private static @NotNull RocketPartCatalog loadDefault() {
        InputStream stream = RocketPartCatalog.class.getResourceAsStream(DATA_CATALOG_RESOURCE);
        String sourcePath = DATA_CATALOG_RESOURCE;
        if (stream == null) {
            stream = RocketPartCatalog.class.getResourceAsStream(ASSET_CATALOG_RESOURCE);
            sourcePath = ASSET_CATALOG_RESOURCE;
        }
        if (stream == null) {
            LOGGER.warn("Modular rocket part catalog not found on classpath ({} or {}); using built-in fallback defaults.",
                    DATA_CATALOG_RESOURCE, ASSET_CATALOG_RESOURCE);
            return new RocketPartCatalog(List.of());
        }
        try (InputStream in = stream) {
            return loadFromStream(in);
        } catch (Exception e) {
            LOGGER.error("Failed to parse modular rocket part catalog from {}; using built-in fallback defaults.", sourcePath, e);
            return new RocketPartCatalog(List.of());
        }
    }

    /**
     * Parses a {@link RocketPartCatalog} from a JSON input stream following the canonical {@code catalog.json} schema.
     */
    public static @NotNull RocketPartCatalog loadFromStream(@NotNull InputStream stream) {
        Objects.requireNonNull(stream, "stream");
        Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        JsonElement rootElement = JsonParser.parseReader(reader);
        if (!rootElement.isJsonObject()) {
            return new RocketPartCatalog(List.of());
        }
        JsonObject root = rootElement.getAsJsonObject();
        JsonArray partsArray = root.getAsJsonArray("parts");
        if (partsArray == null) {
            return new RocketPartCatalog(List.of());
        }

        List<RocketPartSpec> specs = new ArrayList<>(partsArray.size());
        for (JsonElement element : partsArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            RocketPartSpec spec = parseSpec(element.getAsJsonObject());
            if (spec != null) {
                specs.add(spec);
            }
        }
        return new RocketPartCatalog(specs);
    }

    private static @Nullable RocketPartSpec parseSpec(JsonObject obj) {
        String modelId = getString(obj, "model_id", null);
        String categoryName = getString(obj, "category", null);
        RocketPartCategory category = RocketPartCategory.byName(categoryName);
        if (modelId == null || modelId.isBlank() || category == null) {
            return null;
        }

        String familyId = getString(obj, "family_id", modelId);
        String displayName = getString(obj, "display_name", formatReadableName(familyId));
        boolean parametric = getBoolean(obj, "parametric", false);
        int diameter = Math.max(1, getInt(obj, "diameter", 1));
        int topDiameter = Math.max(1, getInt(obj, "top_diameter", diameter));
        int heightBlocks = Math.max(1, getInt(obj, "height_blocks", 1));
        boolean walkableInterior = getBoolean(obj, "walkable_interior", false);
        boolean radial = getBoolean(obj, "radial", false);

        String meshStr = getString(
                obj,
                "mesh",
                Constant.MOD_ID + ":models/rocket_part/" + category.assetFolder() + "/" + modelId + ".gcrm"
        );
        ResourceLocation meshLocation = parseResourceLocation(
                meshStr,
                Constant.id("models/rocket_part/" + category.assetFolder() + "/" + modelId + ".gcrm")
        );

        EnumMap<RocketLivery, ResourceLocation> textures = new EnumMap<>(RocketLivery.class);
        if (obj.has("textures") && obj.get("textures").isJsonObject()) {
            JsonObject texObj = obj.getAsJsonObject("textures");
            for (RocketLivery livery : RocketLivery.values()) {
                String texStr = getString(texObj, livery.getSerializedName(), null);
                if (texStr != null && !texStr.isBlank()) {
                    ResourceLocation parsed = ResourceLocation.tryParse(texStr);
                    if (parsed != null) {
                        textures.put(livery, parsed);
                    }
                }
            }
        }

        return new RocketPartSpec(
                modelId,
                familyId,
                displayName,
                category,
                parametric,
                diameter,
                topDiameter,
                heightBlocks,
                walkableInterior,
                radial,
                meshLocation,
                textures
        );
    }

    private static RocketPartSpec selectDefaultSpec(RocketPartCategory category, List<RocketPartFamily> families) {
        String preferredModelId = switch (category) {
            case COMMAND -> "capsule_vostok_1x1_1b";
            case ENGINE -> "engine_falcon1d_3x3_2b";
            case TANK -> "tank_canister_1x1_1b";
            case ADAPTER -> "adapter_3x3_to_1x1_1b";
            case PAYLOAD -> "science_flight_recorder_1x1_1b";
            case UTILITY -> "decoupler_inline_1x1_1b";
        };
        for (RocketPartFamily family : families) {
            for (RocketPartSpec spec : family.variants()) {
                if (spec.modelId().equals(preferredModelId)) {
                    return spec;
                }
            }
        }
        return families.get(0).defaultVariant();
    }

    private static RocketPartSpec createFallbackSpec(RocketPartCategory category) {
        return switch (category) {
            case COMMAND -> fallback("capsule_vostok_1x1_1b", "capsule_vostok", "Capsule Vostok", category, 1, 1, 1, false, false);
            case ENGINE -> fallback("engine_falcon1d_3x3_2b", "engine_falcon1d", "Engine Falcon1d", category, 3, 3, 2, false, false);
            case TANK -> fallback("tank_canister_1x1_1b", "tank_canister", "Tank Canister", category, 1, 1, 1, false, false);
            case ADAPTER -> fallback("adapter_3x3_to_1x1_1b", "adapter", "Adapter", category, 3, 1, 1, false, false);
            case PAYLOAD -> fallback("science_flight_recorder_1x1_1b", "science_flight_recorder", "Science Flight Recorder", category, 1, 1, 1, false, false);
            case UTILITY -> fallback("decoupler_inline_1x1_1b", "decoupler_inline", "Decoupler Inline", category, 1, 1, 1, false, false);
        };
    }

    private static RocketPartSpec fallback(
            String modelId,
            String familyId,
            String displayName,
            RocketPartCategory category,
            int diameter,
            int topDiameter,
            int heightBlocks,
            boolean walkableInterior,
            boolean radial
    ) {
        String folder = category.assetFolder();
        ResourceLocation mesh = Constant.id("models/rocket_part/" + folder + "/" + modelId + ".gcrm");
        EnumMap<RocketLivery, ResourceLocation> textures = new EnumMap<>(RocketLivery.class);
        for (RocketLivery livery : RocketLivery.values()) {
            textures.put(livery, Constant.id("textures/rocket_part/" + folder + "/" + modelId + "_" + livery.getSerializedName() + ".png"));
        }
        return new RocketPartSpec(
                modelId,
                familyId,
                displayName,
                category,
                false,
                diameter,
                topDiameter,
                heightBlocks,
                walkableInterior,
                radial,
                mesh,
                textures
        );
    }

    private static @Nullable String getString(JsonObject obj, String key, @Nullable String fallback) {
        JsonElement el = obj.get(key);
        if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            String value = el.getAsString();
            if (!value.isBlank()) {
                return value;
            }
        }
        return fallback;
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        JsonElement el = obj.get(key);
        if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return el.getAsInt();
        }
        return fallback;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        JsonElement el = obj.get(key);
        if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
            return el.getAsBoolean();
        }
        return fallback;
    }

    private static ResourceLocation parseResourceLocation(@Nullable String value, ResourceLocation fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        return parsed != null ? parsed : fallback;
    }

    private static String formatReadableName(String rawId) {
        String[] tokens = rawId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                sb.append(token.substring(1));
            }
        }
        return sb.isEmpty() ? rawId : sb.toString();
    }

    public @Nullable RocketPartSpec getPart(@Nullable String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return null;
        }
        return this.partsByModelId.get(modelId);
    }

    public @NotNull List<RocketPartSpec> allParts() {
        return this.allParts;
    }

    public int size() {
        return this.allParts.size();
    }

    public @NotNull List<RocketPartFamily> familiesFor(@NotNull RocketPartCategory category) {
        Objects.requireNonNull(category, "category");
        return this.familiesByCategory.getOrDefault(category, List.of());
    }

    public @Nullable RocketPartFamily getFamily(@Nullable String familyId) {
        if (familyId == null || familyId.isBlank()) {
            return null;
        }
        return this.familiesById.get(familyId);
    }

    public @NotNull RocketPartSpec defaultSpecFor(@NotNull RocketPartCategory category) {
        Objects.requireNonNull(category, "category");
        return this.defaultsByCategory.get(category);
    }

    public @NotNull RocketPartConfig defaultConfigFor(@NotNull RocketPartCategory category) {
        return this.defaultSpecFor(category).toConfig(RocketLivery.WHITE);
    }
}
