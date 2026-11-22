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

package dev.galacticraft.mod.client.render.platform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.galacticraft.mod.Constant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client resource reload listener that loads baked platform runtime models from
 * {@code assets/<namespace>/models/platform_runtime/*.json}.
 */
@Environment(EnvType.CLIENT)
public final class PlatformModelLoader implements SimpleSynchronousResourceReloadListener {
    public static final PlatformModelLoader INSTANCE = new PlatformModelLoader();
    public static final ResourceLocation ID = Constant.id("platform_runtime_models");

    private static final String MODEL_DIRECTORY = "models/platform_runtime";
    private static final ResourceLocation DEFAULT_TEXTURE = Constant.id("textures/platform/platform_atlas.png");

    private volatile Map<String, PlatformRuntimeModel> models = Map.of();

    private PlatformModelLoader() {
    }

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        PlatformHologramRenderer.clearCache();

        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                MODEL_DIRECTORY,
                location -> location.getPath().endsWith(".json")
        );

        Map<String, PlatformRuntimeModel> loaded = new HashMap<>(resources.size() * 2);
        int totalQuads = 0;

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            String fileStem = extractFileStem(location.getPath());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (!rootElement.isJsonObject()) {
                    Constant.LOGGER.warn("Skipping platform runtime model {}: root is not a JSON object.", location);
                    continue;
                }
                PlatformRuntimeModel model = parseModel(rootElement.getAsJsonObject(), fileStem);
                loaded.put(model.id(), model);
                if (!fileStem.isEmpty() && !fileStem.equals(model.id())) {
                    loaded.putIfAbsent(fileStem, model);
                }
                totalQuads += model.totalQuadCount();
            } catch (Exception e) {
                Constant.LOGGER.error("Failed to load platform runtime model {}", location, e);
            }
        }

        this.models = Map.copyOf(loaded);
        Constant.LOGGER.info("Loaded {} platform runtime models ({} total quads).", this.models.size(), totalQuads);
    }

    public @Nullable PlatformRuntimeModel getModel(@Nullable String key) {
        return key != null ? this.models.get(key) : null;
    }

    public int getLoadedModelCount() {
        return this.models.size();
    }

    private static String extractFileStem(String path) {
        int slash = path.lastIndexOf('/');
        String filename = slash >= 0 ? path.substring(slash + 1) : path;
        if (filename.endsWith(".json")) {
            return filename.substring(0, filename.length() - 5);
        }
        return filename;
    }

    private static PlatformRuntimeModel parseModel(JsonObject root, String fallbackId) {
        String id = GsonHelper.getAsString(root, "id", fallbackId);
        if (id.isEmpty()) {
            id = fallbackId;
        }

        String textureStr = GsonHelper.getAsString(root, "texture", DEFAULT_TEXTURE.toString());
        ResourceLocation texture = ResourceLocation.tryParse(textureStr);
        if (texture == null) {
            Constant.LOGGER.warn("Invalid texture '{}' in platform model '{}', using default atlas.", textureStr, id);
            texture = DEFAULT_TEXTURE;
        }

        int uvSize = GsonHelper.getAsInt(root, "uv_size", 512);
        int texSize = GsonHelper.getAsInt(root, "tex_size", 2048);
        float duration = GsonHelper.getAsFloat(root, "duration", 30.0F);

        List<PlatformRuntimeModel.Bone> bones = new ArrayList<>();
        if (root.has("bones") && root.get("bones").isJsonArray()) {
            JsonArray boneArray = root.getAsJsonArray("bones");
            for (JsonElement element : boneArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                bones.add(parseBone(element.getAsJsonObject()));
            }
        }

        return new PlatformRuntimeModel(id, texture, uvSize, texSize, duration, bones);
    }

    private static PlatformRuntimeModel.Bone parseBone(JsonObject obj) {
        String name = GsonHelper.getAsString(obj, "name", "unnamed");
        String parent = null;
        if (obj.has("parent") && !obj.get("parent").isJsonNull()) {
            parent = obj.get("parent").getAsString();
            if (parent.isEmpty()) {
                parent = null;
            }
        }

        float[] origin = parseVec3(obj, "origin");
        float[] rotation = parseVec3(obj, "rotation");
        float[] opaqueQuads = parseQuads(obj, "opaque_quads");
        float[] translucentQuads = parseQuads(obj, "translucent_quads");

        KeyframeTrack posTrack = parseKeyframes(obj, "anim_pos");
        KeyframeTrack rotTrack = parseKeyframes(obj, "anim_rot");

        return new PlatformRuntimeModel.Bone(
                name,
                parent,
                origin[0], origin[1], origin[2],
                rotation[0], rotation[1], rotation[2],
                opaqueQuads,
                translucentQuads,
                posTrack.times,
                posTrack.values,
                rotTrack.times,
                rotTrack.values
        );
    }

    private static float[] parseVec3(JsonObject obj, String key) {
        float[] out = new float[3];
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return out;
        }
        JsonArray arr = obj.getAsJsonArray(key);
        int count = Math.min(3, arr.size());
        for (int i = 0; i < count; i++) {
            float v = arr.get(i).getAsFloat();
            out[i] = Float.isFinite(v) ? v : 0.0F;
        }
        return out;
    }

    private static float[] parseQuads(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return PlatformRuntimeModel.EMPTY_FLOATS;
        }
        JsonArray quadsArr = obj.getAsJsonArray(key);
        int rawCount = quadsArr.size();
        if (rawCount == 0) {
            return PlatformRuntimeModel.EMPTY_FLOATS;
        }

        float[] buffer = new float[rawCount * PlatformRuntimeModel.FLOATS_PER_QUAD];
        int validQuads = 0;

        for (int q = 0; q < rawCount; q++) {
            JsonElement elem = quadsArr.get(q);
            if (!elem.isJsonArray()) {
                continue;
            }
            JsonArray quad = elem.getAsJsonArray();
            if (quad.size() < PlatformRuntimeModel.FLOATS_PER_QUAD) {
                continue;
            }
            int dst = validQuads * PlatformRuntimeModel.FLOATS_PER_QUAD;
            boolean valid = true;
            for (int f = 0; f < PlatformRuntimeModel.FLOATS_PER_QUAD; f++) {
                float val = quad.get(f).getAsFloat();
                if (!Float.isFinite(val)) {
                    valid = false;
                    break;
                }
                buffer[dst + f] = val;
            }
            if (valid) {
                validQuads++;
            }
        }

        if (validQuads == 0) {
            return PlatformRuntimeModel.EMPTY_FLOATS;
        }
        if (validQuads == rawCount) {
            return buffer;
        }
        float[] trimmed = new float[validQuads * PlatformRuntimeModel.FLOATS_PER_QUAD];
        System.arraycopy(buffer, 0, trimmed, 0, trimmed.length);
        return trimmed;
    }

    private static KeyframeTrack parseKeyframes(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return KeyframeTrack.EMPTY;
        }
        JsonArray arr = obj.getAsJsonArray(key);
        int size = arr.size();
        if (size == 0) {
            return KeyframeTrack.EMPTY;
        }

        List<float[]> frames = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            JsonElement elem = arr.get(i);
            if (!elem.isJsonArray()) {
                continue;
            }
            JsonArray kf = elem.getAsJsonArray();
            if (kf.size() < 4) {
                continue;
            }
            float t = kf.get(0).getAsFloat();
            float x = kf.get(1).getAsFloat();
            float y = kf.get(2).getAsFloat();
            float z = kf.get(3).getAsFloat();
            if (Float.isFinite(t) && Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) {
                frames.add(new float[]{t, x, y, z});
            }
        }

        if (frames.isEmpty()) {
            return KeyframeTrack.EMPTY;
        }

        frames.sort(Comparator.comparingDouble(f -> f[0]));
        int count = frames.size();
        float[] times = new float[count];
        float[] values = new float[count * 3];
        for (int i = 0; i < count; i++) {
            float[] f = frames.get(i);
            times[i] = f[0];
            values[i * 3] = f[1];
            values[i * 3 + 1] = f[2];
            values[i * 3 + 2] = f[3];
        }
        return new KeyframeTrack(times, values);
    }

    private record KeyframeTrack(float[] times, float[] values) {
        static final KeyframeTrack EMPTY = new KeyframeTrack(PlatformRuntimeModel.EMPTY_FLOATS, PlatformRuntimeModel.EMPTY_FLOATS);
    }
}
