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

package dev.galacticraft.mod.world.gen.base;

import dev.galacticraft.mod.content.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Random;

/**
 * Generates an abandoned space research capsule inside hollow asteroids.
 */
public class MapGenAbandonedBase {
    public static void generateInChunk(ChunkAccess chunk, Random rand, int centerX, int centerY, int centerZ) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();

        BlockState wall = GCBlocks.TITANIUM_DECORATION.block().defaultBlockState();
        BlockState floor = GCBlocks.TIN_DECORATION.block().defaultBlockState();
        BlockState window = GCBlocks.VACUUM_GLASS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState light = GCBlocks.GLOWSTONE_LANTERN.defaultBlockState();

        int radius = 3;
        int height = 4;

        for (int dx = -radius; dx <= radius; dx++) {
            int wx = centerX + dx;
            if (wx < chunkMinX || wx > chunkMaxX) continue;

            for (int dz = -radius; dz <= radius; dz++) {
                int wz = centerZ + dz;
                if (wz < chunkMinZ || wz > chunkMaxZ) continue;

                boolean isEdgeX = Math.abs(dx) == radius;
                boolean isEdgeZ = Math.abs(dz) == radius;

                for (int dy = 0; dy <= height; dy++) {
                    int wy = centerY + dy;
                    BlockPos pos = new BlockPos(wx, wy, wz);

                    if (dy == 0) {
                        chunk.setBlockState(pos, floor, false);
                    } else if (dy == height) {
                        if (dx == 0 && dz == 0) {
                            chunk.setBlockState(pos, light, false);
                        } else {
                            chunk.setBlockState(pos, wall, false);
                        }
                    } else if (isEdgeX || isEdgeZ) {
                        // Doorway on one side, windows on others
                        if (dx == 0 && dz == -radius && (dy == 1 || dy == 2)) {
                            chunk.setBlockState(pos, air, false);
                        } else if (dy == 2 && (dx == 0 || dz == 0) && !(isEdgeX && isEdgeZ)) {
                            chunk.setBlockState(pos, window, false);
                        } else {
                            chunk.setBlockState(pos, wall, false);
                        }
                    } else {
                        // Interior
                        if (dy == 1 && dx == 0 && dz == radius - 1) {
                            chunk.setBlockState(pos, Blocks.CHEST.defaultBlockState(), false);
                        } else {
                            chunk.setBlockState(pos, air, false);
                        }
                    }
                }
            }
        }
    }
}
