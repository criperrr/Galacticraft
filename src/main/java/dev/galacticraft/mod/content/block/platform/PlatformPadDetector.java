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

import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Bounded horizontal flood-fill detector that locates an adjacent {@link ReinforcedLaunchPadBlock}
 * deck near a {@link HangarTerminalBlock} and infers its center, dimensions, and suggested platform blueprint.
 */
public final class PlatformPadDetector {
    public static final int MAX_BFS_BLOCKS = 512;
    private static final int SEARCH_RADIUS = 2;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private PlatformPadDetector() {
    }

    public record DetectionResult(
            BlockPos padCenter,
            int widthX,
            int depthZ,
            PlatformSize suggestedSize,
            PlatformType suggestedType,
            int padBlockCount
    ) {
    }

    /**
     * Searches within horizontal radius {@value #SEARCH_RADIUS} around {@code terminalPos}
     * (at {@code terminalPos.getY()} and {@code terminalPos.getY() - 1}) for an adjacent
     * {@link ReinforcedLaunchPadBlock}, then runs a bounded horizontal BFS (up to {@value #MAX_BFS_BLOCKS} blocks)
     * to determine the connected pad footprint and recommended platform configuration.
     */
    public static @Nullable DetectionResult detectAdjacentPad(LevelReader level, BlockPos terminalPos, Direction facing) {
        BlockPos seed = findSeedPad(level, terminalPos, facing);
        if (seed == null) {
            return null;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        LongSet visited = new LongOpenHashSet();
        queue.add(seed);
        visited.add(seed.asLong());

        int minX = seed.getX();
        int maxX = seed.getX();
        int minZ = seed.getZ();
        int maxZ = seed.getZ();
        int count = 0;

        while (!queue.isEmpty() && count < MAX_BFS_BLOCKS) {
            BlockPos current = queue.poll();
            count++;

            int x = current.getX();
            int z = current.getZ();
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (z > maxZ) {
                maxZ = z;
            }

            if (visited.size() >= MAX_BFS_BLOCKS) {
                continue;
            }

            for (Direction dir : HORIZONTAL) {
                BlockPos next = current.relative(dir);
                if (visited.size() >= MAX_BFS_BLOCKS) {
                    break;
                }
                long packed = next.asLong();
                if (!visited.contains(packed) && isPadBlock(level, next)) {
                    visited.add(packed);
                    queue.add(next);
                }
            }
        }

        int widthX = maxX - minX + 1;
        int depthZ = maxZ - minZ + 1;
        int centerX = Math.floorDiv(minX + maxX, 2);
        int centerZ = Math.floorDiv(minZ + maxZ, 2);
        BlockPos padCenter = new BlockPos(centerX, seed.getY(), centerZ);

        int minAxis = Math.min(widthX, depthZ);
        int maxAxis = Math.max(widthX, depthZ);
        double aspectRatio = (double) maxAxis / Math.max(1, minAxis);

        PlatformType suggestedType;
        PlatformSize suggestedSize;
        if (aspectRatio >= 2.0) {
            suggestedType = PlatformType.HORIZONTAL;
            suggestedSize = PlatformSize.clampForType(PlatformType.HORIZONTAL, PlatformSize.forPadSpan(minAxis));
        } else {
            suggestedType = PlatformType.VERTICAL;
            suggestedSize = PlatformSize.forPadSpan(maxAxis);
        }

        return new DetectionResult(padCenter, widthX, depthZ, suggestedSize, suggestedType, count);
    }

    private static @Nullable BlockPos findSeedPad(LevelReader level, BlockPos terminalPos, Direction facing) {
        Direction preferredDir = facing != null && facing.getAxis().isHorizontal() ? facing.getOpposite() : Direction.NORTH;

        // First prioritize directly behind the terminal at Y and Y - 1
        for (int dy : new int[]{0, -1}) {
            BlockPos behind1 = terminalPos.relative(preferredDir, 1).offset(0, dy, 0);
            if (isPadBlock(level, behind1)) {
                return behind1;
            }
            BlockPos behind2 = terminalPos.relative(preferredDir, 2).offset(0, dy, 0);
            if (isPadBlock(level, behind2)) {
                return behind2;
            }
        }

        // Otherwise search expanding rings within radius 2 at Y and Y - 1
        for (int dy : new int[]{0, -1}) {
            for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        BlockPos candidate = terminalPos.offset(dx, dy, dz);
                        if (isPadBlock(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isPadBlock(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ReinforcedLaunchPadBlock;
    }
}
