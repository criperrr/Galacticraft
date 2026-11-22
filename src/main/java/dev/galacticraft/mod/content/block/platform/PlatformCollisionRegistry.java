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

package dev.galacticraft.mod.content.block.platform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import dev.galacticraft.mod.Constant;
import net.minecraft.core.BlockPos;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads pre-voxelized 3D model collision maps from {@code data/galacticraft/platform_collision/<modelKey>.json}
 * so formed platforms place invisible collision proxies at the exact coordinates of their rendered 3D geometry.
 */
public final class PlatformCollisionRegistry {
    private static final Map<String, List<CollisionVoxel>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Bounds> BOUNDS_CACHE = new ConcurrentHashMap<>();

    private PlatformCollisionRegistry() {
    }

    public record CollisionVoxel(BlockPos localPos, BoundsShape shape) {
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    /**
     * Exact vertical elevator cabin track matching the animated {@code elevator_cab} bone
     * in {@code platform_runtime/*.json} so the server can synchronize walkable
     * {@link BoundsShape#ELEVATOR_FLOOR} proxies and lift riders smoothly.
     */
    public record ElevatorTrack(
            double originX,
            double originY,
            double originZ,
            double duration,
            double[][] timeAndDeltaY
    ) {
        public double sampleY(long gameTime) {
            if (this.duration <= 0.0 || this.timeAndDeltaY.length == 0) {
                return this.originY;
            }
            double t = ((double) gameTime / 20.0) % this.duration;
            if (t < 0.0) {
                t += this.duration;
            }
            double[][] kfs = this.timeAndDeltaY;
            if (t <= kfs[0][0]) {
                return this.originY + kfs[0][1];
            }
            for (int i = 0; i < kfs.length - 1; i++) {
                double t0 = kfs[i][0];
                double t1 = kfs[i + 1][0];
                if (t <= t1) {
                    double span = t1 - t0;
                    double alpha = span > 1.0E-6 ? (t - t0) / span : 0.0;
                    return this.originY + kfs[i][1] + (kfs[i + 1][1] - kfs[i][1]) * alpha;
                }
            }
            return this.originY + kfs[kfs.length - 1][1];
        }
    }

    private static final Map<String, ElevatorTrack> ELEVATOR_TRACKS = Map.of(
            "vertical_5x5", new ElevatorTrack(-8.5, 0.875, -6.375, 30.0,
                    new double[][]{{0.0, 0.0}, {8.0, 10.5}, {18.0, 17.0625}, {26.0, 5.25}, {30.0, 0.0}}),
            "vertical_7x7", new ElevatorTrack(0.0, 0.875, -9.625, 30.0,
                    new double[][]{{0.0, 0.0}, {10.0, 23.5625}, {20.0, 26.28125}, {30.0, 0.0}}),
            "vertical_9x9", new ElevatorTrack(-10.375, 0.875, 0.0, 30.0,
                    new double[][]{{0.0, 0.0}, {10.0, 30.875}, {20.0, 36.4}, {30.0, 0.0}}),
            "vertical_11x11", new ElevatorTrack(-10.0, 0.875, -2.875, 30.0,
                    new double[][]{{0.0, 0.0}, {12.0, 43.75}, {24.0, 31.25}, {30.0, 0.0}})
    );

    public static @org.jetbrains.annotations.Nullable ElevatorTrack getElevatorTrack(PlatformType type, PlatformSize size) {
        if (type != PlatformType.VERTICAL || size == null) {
            return null;
        }
        return ELEVATOR_TRACKS.get(size.modelKey(type));
    }

    public static List<CollisionVoxel> get(PlatformType type, PlatformSize size) {
        PlatformType resolvedType = type != null ? type : PlatformType.VERTICAL;
        PlatformSize resolvedSize = PlatformSize.clampForType(resolvedType, size != null ? size : PlatformSize.SIZE_1X1);
        String modelKey = resolvedSize.modelKey(resolvedType);
        return CACHE.computeIfAbsent(modelKey, PlatformCollisionRegistry::loadFromClasspath);
    }

    /**
     * Returns the full local-space bounding box occupied by the assembled platform (union of its
     * 3D collision voxels and structural blueprint bounds) so preview renderers can outline the
     * entire area the platform will occupy.
     */
    public static Bounds getOccupiedBounds(PlatformType type, PlatformSize size) {
        PlatformType resolvedType = type != null ? type : PlatformType.VERTICAL;
        PlatformSize resolvedSize = PlatformSize.clampForType(resolvedType, size != null ? size : PlatformSize.SIZE_1X1);
        String modelKey = resolvedSize.modelKey(resolvedType);
        return BOUNDS_CACHE.computeIfAbsent(modelKey, key -> {
            PlatformBlueprint bp = PlatformBlueprintRegistry.get(resolvedType, resolvedSize);
            int minX = bp.minX();
            int minY = 0;
            int minZ = bp.minZ();
            int maxX = bp.maxX();
            int maxY = bp.maxY();
            int maxZ = bp.maxZ();
            for (CollisionVoxel voxel : get(resolvedType, resolvedSize)) {
                BlockPos p = voxel.localPos();
                if (p.getX() < minX) minX = p.getX();
                if (p.getY() < minY) minY = p.getY();
                if (p.getZ() < minZ) minZ = p.getZ();
                if (p.getX() > maxX) maxX = p.getX();
                if (p.getY() > maxY) maxY = p.getY();
                if (p.getZ() > maxZ) maxZ = p.getZ();
            }
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        });
    }

    private static List<CollisionVoxel> loadFromClasspath(String modelKey) {
        String resourcePath = "data/galacticraft/platform_collision/" + modelKey + ".json";
        try (InputStream stream = PlatformCollisionRegistry.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                Constant.LOGGER.warn("Missing platform collision voxel map '{}'; falling back to blueprint entries.", resourcePath);
                return List.of();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray voxelsArr = root.getAsJsonArray("voxels");
                if (voxelsArr == null || voxelsArr.isEmpty()) {
                    return List.of();
                }
                List<CollisionVoxel> result = new ArrayList<>(voxelsArr.size());
                for (JsonElement elem : voxelsArr) {
                    JsonArray row = elem.getAsJsonArray();
                    if (row.size() < 4) {
                        continue;
                    }
                    int x = row.get(0).getAsInt();
                    int y = row.get(1).getAsInt();
                    int z = row.get(2).getAsInt();
                    BoundsShape shape = parseShape(row.get(3).getAsString());
                    result.add(new CollisionVoxel(new BlockPos(x, y, z), shape));
                }
                return List.copyOf(result);
            }
        } catch (Exception e) {
            Constant.LOGGER.error("Failed to load platform collision voxel map '{}'", resourcePath, e);
            return List.of();
        }
    }

    private static BoundsShape parseShape(String name) {
        if ("SLAB_8PX".equalsIgnoreCase(name)) {
            return BoundsShape.SLAB_8PX;
        }
        if ("FLOOR_4PX".equalsIgnoreCase(name)) {
            return BoundsShape.FLOOR_4PX;
        }
        return BoundsShape.FULL;
    }
}
