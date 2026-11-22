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

package dev.galacticraft.api.rocket.part.modular;

import dev.galacticraft.api.multiblock.BoundsShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Multi-voxel collision footprint topology modes and geometry helpers for modular rocket parts.
 *
 * <p>Local coordinates place the bottom-center master anchor block at {@code (0, 0, 0)},
 * vertical layers in {@code [0 .. heightBlocks - 1]}, and the front airlock hatch at {@code +Z}.
 */
public enum FootprintMode {
    SOLID_OCTAGON,
    HOLLOW_WITH_HATCH,
    RADIAL_1B;

    /**
     * Computes the local-space footprint mapping every occupied cell (including the master anchor
     * at {@code (0, 0, 0)}) to its corresponding {@link BoundsShape}.
     *
     * <p>For {@link #HOLLOW_WITH_HATCH}, strictly interior cells above {@code y == 0} (and below
     * the roof layer when {@code heightBlocks >= 3}) are omitted from the map so players can walk
     * inside the cabin or bay.
     */
    public static @NotNull Map<BlockPos, BoundsShape> computeLocalFootprint(@NotNull RocketPartConfig config, boolean hatchOpen) {
        Objects.requireNonNull(config, "config");
        FootprintMode mode = config.footprintMode();
        int diameter = config.diameter();
        int height = config.heightBlocks();
        Map<BlockPos, BoundsShape> footprint = new LinkedHashMap<>();

        if (mode == RADIAL_1B || diameter <= 1) {
            for (int y = 0; y < height; y++) {
                footprint.put(new BlockPos(0, y, 0), BoundsShape.FULL);
            }
            return Collections.unmodifiableMap(footprint);
        }

        int radius = diameter / 2;
        if (mode == SOLID_OCTAGON) {
            for (int y = 0; y < height; y++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        if (isInsideOctagon(dx, dz, diameter)) {
                            footprint.put(new BlockPos(dx, y, dz), BoundsShape.FULL);
                        }
                    }
                }
            }
            footprint.putIfAbsent(BlockPos.ZERO, BoundsShape.FULL);
            return Collections.unmodifiableMap(footprint);
        }

        // HOLLOW_WITH_HATCH (walkable interior with operable airlock hatch at (0, y, +radius))
        for (int y = 0; y < height; y++) {
            boolean isFloor = y == 0;
            boolean isRoof = height >= 3 && y == height - 1;
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (!isInsideOctagon(dx, dz, diameter)) {
                        continue;
                    }
                    boolean interior = isStrictlyInterior(dx, dz, diameter);
                    if (isFloor) {
                        footprint.put(new BlockPos(dx, y, dz), interior ? BoundsShape.FLOOR_4PX : BoundsShape.FULL);
                    } else if (isRoof) {
                        footprint.put(new BlockPos(dx, y, dz), BoundsShape.FULL);
                    } else if (!interior) {
                        // Interior volume cells are intentionally left as AIR for player movement
                        footprint.put(new BlockPos(dx, y, dz), BoundsShape.FULL);
                    }
                }
            }
        }

        BoundsShape hatchShape = hatchOpen ? BoundsShape.OPEN_HATCH : BoundsShape.FULL;
        for (BlockPos hatchPos : computeLocalHatchPositions(config)) {
            footprint.put(hatchPos, hatchShape);
        }

        footprint.putIfAbsent(BlockPos.ZERO, BoundsShape.FLOOR_4PX);
        return Collections.unmodifiableMap(footprint);
    }

    /**
     * Returns the local-space block positions forming the operable front airlock hatch at {@code (0, y, +r)},
     * or an empty list if this configuration does not have a walkable hollow interior.
     */
    public static @NotNull List<BlockPos> computeLocalHatchPositions(@NotNull RocketPartConfig config) {
        Objects.requireNonNull(config, "config");
        if (config.footprintMode() != HOLLOW_WITH_HATCH) {
            return List.of();
        }
        int radius = config.diameter() / 2;
        int height = config.heightBlocks();
        int minY;
        int maxY;
        if (height <= 1) {
            minY = 0;
            maxY = 0;
        } else if (height == 2) {
            minY = 0;
            maxY = 1;
        } else {
            minY = 1;
            maxY = Math.min(2, height - 1);
        }

        List<BlockPos> hatchCells = new ArrayList<>(maxY - minY + 1);
        for (int y = minY; y <= maxY; y++) {
            hatchCells.add(new BlockPos(0, y, radius));
        }
        return Collections.unmodifiableList(hatchCells);
    }

    /**
     * Tests whether a local horizontal offset {@code (dx, dz)} lies within the octagonal cross-section
     * of a rocket part with the given {@code diameter}.
     */
    public static boolean isInsideOctagon(int dx, int dz, int diameter) {
        if (diameter <= 1) {
            return dx == 0 && dz == 0;
        }
        int radius = diameter / 2;
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        if (diameter <= 3) {
            return ax <= radius && az <= radius;
        }
        return ax <= radius && az <= radius && (ax + az <= radius + (radius + 1) / 2);
    }

    /**
     * Tests whether a local horizontal offset {@code (dx, dz)} lies strictly inside the outer perimeter
     * wall of an octagonal cross-section.
     */
    public static boolean isStrictlyInterior(int dx, int dz, int diameter) {
        if (diameter < 3) {
            return false;
        }
        int radius = diameter / 2;
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        if (diameter <= 3) {
            return ax < radius && az < radius;
        }
        return ax < radius && az < radius && (ax + az < radius + (radius + 1) / 2);
    }

    /**
     * Rotates a local offset around the Y axis using the same convention as {@code PlatformBlueprint}:
     * {@code NORTH = 0 deg}, {@code EAST = 90 deg}, {@code SOUTH = 180 deg}, {@code WEST = 270 deg}.
     */
    public static @NotNull BlockPos rotateLocalPos(@NotNull BlockPos local, @NotNull Direction facing) {
        int lx = local.getX();
        int ly = local.getY();
        int lz = local.getZ();
        return switch (facing) {
            case EAST -> new BlockPos(-lz, ly, lx);
            case SOUTH -> new BlockPos(-lx, ly, -lz);
            case WEST -> new BlockPos(lz, ly, -lx);
            default -> new BlockPos(lx, ly, lz);
        };
    }

    /**
     * Rotates a local offset by {@code facing} and translates it relative to the world-space {@code origin}.
     */
    public static @NotNull BlockPos toWorldPos(@NotNull BlockPos origin, @NotNull Direction facing, @NotNull BlockPos local) {
        BlockPos rotated = rotateLocalPos(local, facing);
        return origin.offset(rotated.getX(), rotated.getY(), rotated.getZ());
    }

    /**
     * Computes the local-space axis-aligned bounding box covering the full rocket part footprint
     * relative to the master anchor block at {@code (0, 0, 0)}.
     */
    public static @NotNull AABB computeBoundsAABB(@NotNull RocketPartConfig config) {
        Objects.requireNonNull(config, "config");
        if (config.footprintMode() == RADIAL_1B || config.diameter() <= 1) {
            return new AABB(0.0, 0.0, 0.0, 1.0, config.heightBlocks(), 1.0);
        }
        int radius = config.diameter() / 2;
        return new AABB(-radius, 0.0, -radius, radius + 1.0, config.heightBlocks(), radius + 1.0);
    }
}
