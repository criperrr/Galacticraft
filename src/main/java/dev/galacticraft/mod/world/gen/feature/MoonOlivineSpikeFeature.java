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

package dev.galacticraft.mod.world.gen.feature;

import com.mojang.serialization.Codec;
import dev.galacticraft.mod.content.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates volcanic olivine-basalt spires and crystalline olivine clusters on the surface of
 * the {@code olivine_spikes} biome and inside subterranean lunar caves, inspired by
 * MorePlanets's {@code WorldGenInfectedPurloniteCrystal} / {@code ChunkGeneratorKoentus},
 * Zollern-Galaxy's {@code WorldGenZolniumCrystals}, and GalaxySpace's crystal formations.
 */
public class MoonOlivineSpikeFeature extends Feature<NoneFeatureConfiguration> {

    public MoonOlivineSpikeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // Find solid ground at or just below origin
        BlockPos.MutableBlockPos basePos = origin.mutable();
        for (int i = 0; i < 6 && level.getBlockState(basePos).isAir(); i++) {
            basePos.move(Direction.DOWN);
        }

        BlockState ground = level.getBlockState(basePos);
        if (!isValidFoundation(ground)) {
            return false;
        }

        boolean isSurface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, basePos.getX(), basePos.getZ()) <= basePos.getY() + 2;
        int height = isSurface ? (5 + random.nextInt(7)) : (3 + random.nextInt(4)); // 5-11 on surface, 3-6 in caves
        double baseRadius = isSurface ? (1.8 + random.nextDouble() * 1.4) : (1.3 + random.nextDouble() * 0.9);

        // Slight tilt vector for natural volcanic/crystalline spire geometry
        double tiltX = (random.nextDouble() - 0.5) * 0.35;
        double tiltZ = (random.nextDouble() - 0.5) * 0.35;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // 1. Build basal collar and tapered spire column
        for (int dy = -2; dy <= height; dy++) {
            double progress = dy < 0 ? 0.0 : (double) dy / (double) height;
            double currentRadius = baseRadius * (1.0 - progress * 0.78);
            int rCeil = Mth.ceil(currentRadius + 0.5);

            double centerX = basePos.getX() + tiltX * Math.max(0, dy);
            double centerZ = basePos.getZ() + tiltZ * Math.max(0, dy);

            for (int dx = -rCeil; dx <= rCeil; dx++) {
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    double wx = basePos.getX() + dx;
                    double wz = basePos.getZ() + dz;
                    double dist = Math.sqrt((wx - centerX) * (wx - centerX) + (wz - centerZ) * (wz - centerZ));
                    if (dist > currentRadius + (random.nextDouble() - 0.5) * 0.35) {
                        continue;
                    }

                    cursor.set((int) wx, basePos.getY() + dy, (int) wz);
                    BlockState existing = level.getBlockState(cursor);
                    if (dy < 0 && !existing.isAir() && !isValidFoundation(existing)) {
                        continue;
                    }

                    BlockState blockToPlace = pickSpireBlock(random, progress, dist / Math.max(0.5, currentRadius));
                    level.setBlock(cursor, blockToPlace, Block.UPDATE_CLIENTS);

                    // Attach directional Olivine Clusters on exposed faces of olivine/rich-basalt blocks
                    if (dy >= 1 && (blockToPlace.is(GCBlocks.OLIVINE_BLOCK)
                            || blockToPlace.is(GCBlocks.BUDDING_OLIVINE)
                            || blockToPlace.is(GCBlocks.RICH_OLIVINE_BASALT))) {
                        tryAttachClusters(level, cursor, random);
                    }
                }
            }
        }

        // 2. Crown the spire tip with an upward Olivine Cluster crystal
        BlockPos tipPos = new BlockPos(
                Mth.floor(basePos.getX() + tiltX * height + 0.5),
                basePos.getY() + height + 1,
                Mth.floor(basePos.getZ() + tiltZ * height + 0.5)
        );
        if (level.getBlockState(tipPos).isAir() && !level.getBlockState(tipPos.below()).isAir()) {
            level.setBlock(
                    tipPos,
                    GCBlocks.OLIVINE_CLUSTER.defaultBlockState()
                            .setValue(AmethystClusterBlock.FACING, Direction.UP)
                            .setValue(AmethystClusterBlock.WATERLOGGED, false),
                    Block.UPDATE_CLIENTS
            );
        }

        return true;
    }

    private static boolean isValidFoundation(BlockState state) {
        return state.is(GCBlocks.MOON_BASALT)
                || state.is(GCBlocks.OLIVINE_BASALT)
                || state.is(GCBlocks.RICH_OLIVINE_BASALT)
                || state.is(GCBlocks.MOON_ROCK)
                || state.is(GCBlocks.MOON_SURFACE_ROCK)
                || state.is(GCBlocks.MOON_TURF)
                || state.is(GCBlocks.MOON_DIRT)
                || state.is(GCBlocks.LUNASLATE);
    }

    private static BlockState pickSpireBlock(RandomSource random, double heightProgress, double radialFraction) {
        // Core and upper tip expose crystalline Olivine & Budding Olivine; outer mantle is Olivine Basalt & Rich Olivine Basalt
        if (heightProgress > 0.72 || radialFraction < 0.38) {
            if (random.nextInt(5) == 0) {
                return GCBlocks.BUDDING_OLIVINE.defaultBlockState();
            }
            if (random.nextInt(3) != 0) {
                return GCBlocks.OLIVINE_BLOCK.defaultBlockState();
            }
            return GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState();
        }
        int roll = random.nextInt(10);
        if (roll < 3) {
            return GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState();
        } else if (roll < 7) {
            return GCBlocks.OLIVINE_BASALT.defaultBlockState();
        } else if (roll == 7) {
            return GCBlocks.OLIVINE_BLOCK.defaultBlockState();
        } else {
            return GCBlocks.MOON_BASALT.defaultBlockState();
        }
    }

    private static void tryAttachClusters(WorldGenLevel level, BlockPos solidPos, RandomSource random) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (random.nextInt(5) != 0) {
                continue;
            }
            BlockPos adj = solidPos.relative(dir);
            if (level.getBlockState(adj).isAir()) {
                level.setBlock(
                        adj,
                        GCBlocks.OLIVINE_CLUSTER.defaultBlockState()
                                .setValue(AmethystClusterBlock.FACING, dir)
                                .setValue(AmethystClusterBlock.WATERLOGGED, false),
                        Block.UPDATE_CLIENTS
                );
            }
        }
    }
}
