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

package dev.galacticraft.mod.client.render.rocket.part;

import dev.galacticraft.mod.Constant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazy on-demand client cache for binary {@code .gcrm} modular rocket part runtime models.
 *
 * <p>On resource reload, this listener only clears its in-memory cache and stores the active
 * {@link ResourceManager}. Individual {@code .gcrm} models are decoded on first demand (and negative
 * lookups are memoized so missing assets never thrash disk), keeping active RAM and VRAM minimal.
 */
@Environment(EnvType.CLIENT)
public final class RocketPartModelCache implements SimpleSynchronousResourceReloadListener {
    public static final RocketPartModelCache INSTANCE = new RocketPartModelCache();
    public static final ResourceLocation ID = Constant.id("rocket_part_runtime_models");

    public static final int FORMAT_VERSION = 1;
    private static final int MAX_BONES = 4096;
    private static final int MAX_QUADS_PER_BONE = 250_000;
    private static final int MAX_KEYS_PER_TRACK = 100_000;

    private volatile @Nullable ResourceManager resourceManager;
    private final Map<ResourceLocation, Optional<RocketPartRuntimeModel>> cache = new ConcurrentHashMap<>();

    private RocketPartModelCache() {
    }

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.cache.clear();
        Constant.LOGGER.debug("Cleared modular rocket part runtime model cache for lazy on-demand loading.");
    }

    /**
     * Returns the cached {@link RocketPartRuntimeModel} for {@code meshLocation}, decoding the binary
     * {@code .gcrm} asset lazily on first access. Missing or invalid assets are cached as negative
     * lookups until the next resource reload.
     */
    public @Nullable RocketPartRuntimeModel getOrLoad(@Nullable ResourceLocation meshLocation) {
        if (meshLocation == null) {
            return null;
        }

        Optional<RocketPartRuntimeModel> cached = this.cache.get(meshLocation);
        if (cached != null) {
            return cached.orElse(null);
        }

        ResourceManager manager = this.resourceManager;
        if (manager == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                manager = mc.getResourceManager();
            }
        }
        if (manager == null) {
            return null;
        }

        Optional<RocketPartRuntimeModel> loaded = loadFromResourceManager(manager, meshLocation);
        this.cache.put(meshLocation, loaded);
        return loaded.orElse(null);
    }

    public void clear() {
        this.cache.clear();
    }

    public int getLoadedModelCount() {
        int count = 0;
        for (Optional<RocketPartRuntimeModel> entry : this.cache.values()) {
            if (entry.isPresent()) {
                count++;
            }
        }
        return count;
    }

    private static Optional<RocketPartRuntimeModel> loadFromResourceManager(
            ResourceManager manager,
            ResourceLocation meshLocation
    ) {
        Optional<Resource> resourceOpt = manager.getResource(meshLocation);
        if (resourceOpt.isEmpty()) {
            Constant.LOGGER.warn("Missing modular rocket part runtime model: {}", meshLocation);
            return Optional.empty();
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(resourceOpt.get().open()))) {
            return Optional.of(parseBinary(meshLocation, in));
        } catch (Exception e) {
            Constant.LOGGER.error("Failed to parse binary rocket part model {}", meshLocation, e);
            return Optional.empty();
        }
    }

    /**
     * Parses a big-endian binary {@code .gcrm} stream into a {@link RocketPartRuntimeModel}.
     */
    public static RocketPartRuntimeModel parseBinary(ResourceLocation location, DataInputStream in) throws IOException {
        int b0 = in.readUnsignedByte();
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();
        if (b0 != 'G' || b1 != 'C' || b2 != 'R' || b3 != 'M') {
            throw new IOException("Invalid GCRM magic header in " + location);
        }

        int version = in.readInt();
        if (version != FORMAT_VERSION) {
            throw new IOException("Unsupported GCRM version " + version + " in " + location);
        }

        float duration = sanitizeFinite(in.readFloat(), 30.0F);
        float minX = sanitizeFinite(in.readFloat(), 0.0F);
        float minY = sanitizeFinite(in.readFloat(), 0.0F);
        float minZ = sanitizeFinite(in.readFloat(), 0.0F);
        float maxX = sanitizeFinite(in.readFloat(), 0.0F);
        float maxY = sanitizeFinite(in.readFloat(), 0.0F);
        float maxZ = sanitizeFinite(in.readFloat(), 0.0F);

        int boneCount = in.readInt();
        if (boneCount < 0 || boneCount > MAX_BONES) {
            throw new IOException("Invalid GCRM bone count " + boneCount + " in " + location);
        }

        List<RocketPartRuntimeModel.Bone> bones = new ArrayList<>(boneCount);
        for (int i = 0; i < boneCount; i++) {
            String name = in.readUTF();
            int parentIndex = in.readInt();
            boolean lodDetail = in.readUnsignedByte() != 0;

            float originX = sanitizeFinite(in.readFloat(), 0.0F);
            float originY = sanitizeFinite(in.readFloat(), 0.0F);
            float originZ = sanitizeFinite(in.readFloat(), 0.0F);

            float rotX = sanitizeFinite(in.readFloat(), 0.0F);
            float rotY = sanitizeFinite(in.readFloat(), 0.0F);
            float rotZ = sanitizeFinite(in.readFloat(), 0.0F);

            float[] opaqueQuads = readQuads(in, location);
            float[] translucentQuads = readQuads(in, location);
            KeyframeTrack posTrack = readKeyframes(in, location);
            KeyframeTrack rotTrack = readKeyframes(in, location);

            bones.add(new RocketPartRuntimeModel.Bone(
                    name,
                    parentIndex,
                    lodDetail,
                    originX,
                    originY,
                    originZ,
                    rotX,
                    rotY,
                    rotZ,
                    opaqueQuads,
                    translucentQuads,
                    posTrack.times,
                    posTrack.values,
                    rotTrack.times,
                    rotTrack.values
            ));
        }

        return new RocketPartRuntimeModel(
                location,
                duration,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                bones
        );
    }

    private static float[] readQuads(DataInputStream in, ResourceLocation location) throws IOException {
        int quadCount = in.readInt();
        if (quadCount < 0 || quadCount > MAX_QUADS_PER_BONE) {
            throw new IOException("Invalid GCRM quad count " + quadCount + " in " + location);
        }
        if (quadCount == 0) {
            return RocketPartRuntimeModel.EMPTY_FLOATS;
        }
        int floatCount = quadCount * RocketPartRuntimeModel.FLOATS_PER_QUAD;
        float[] data = new float[floatCount];
        for (int i = 0; i < floatCount; i++) {
            data[i] = sanitizeFinite(in.readFloat(), 0.0F);
        }
        return data;
    }

    private static KeyframeTrack readKeyframes(DataInputStream in, ResourceLocation location) throws IOException {
        int keyCount = in.readInt();
        if (keyCount < 0 || keyCount > MAX_KEYS_PER_TRACK) {
            throw new IOException("Invalid GCRM keyframe count " + keyCount + " in " + location);
        }
        if (keyCount == 0) {
            return KeyframeTrack.EMPTY;
        }

        float[] times = new float[keyCount];
        float[] values = new float[keyCount * 3];
        boolean sorted = true;
        float prevTime = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < keyCount; i++) {
            float t = sanitizeFinite(in.readFloat(), 0.0F);
            float x = sanitizeFinite(in.readFloat(), 0.0F);
            float y = sanitizeFinite(in.readFloat(), 0.0F);
            float z = sanitizeFinite(in.readFloat(), 0.0F);
            if (t < prevTime) {
                sorted = false;
            }
            prevTime = t;
            times[i] = t;
            int base = i * 3;
            values[base] = x;
            values[base + 1] = y;
            values[base + 2] = z;
        }

        if (!sorted) {
            Integer[] order = new Integer[keyCount];
            for (int i = 0; i < keyCount; i++) {
                order[i] = i;
            }
            Arrays.sort(order, Comparator.comparingDouble(idx -> times[idx]));
            float[] sortedTimes = new float[keyCount];
            float[] sortedValues = new float[keyCount * 3];
            for (int i = 0; i < keyCount; i++) {
                int src = order[i];
                sortedTimes[i] = times[src];
                sortedValues[i * 3] = values[src * 3];
                sortedValues[i * 3 + 1] = values[src * 3 + 1];
                sortedValues[i * 3 + 2] = values[src * 3 + 2];
            }
            return new KeyframeTrack(sortedTimes, sortedValues);
        }

        return new KeyframeTrack(times, values);
    }

    private static float sanitizeFinite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private record KeyframeTrack(float[] times, float[] values) {
        static final KeyframeTrack EMPTY = new KeyframeTrack(
                RocketPartRuntimeModel.EMPTY_FLOATS,
                RocketPartRuntimeModel.EMPTY_FLOATS
        );
    }
}
