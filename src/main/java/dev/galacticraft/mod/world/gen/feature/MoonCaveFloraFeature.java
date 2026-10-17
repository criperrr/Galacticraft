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
import dev.galacticraft.mod.content.block.environment.MoonTangleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates subterranean lunar cave ecosystems deep beneath the Moon's surface (never exposed to sky),
 * placing {@code MOON_MOSS} on cave floors and ceilings, hanging bioluminescent {@code MOON_TANGLE}
 * vines, {@code MOON_WEED} and {@code MOON_SHRUBS} on mossy floors, and rare subterranean
 * lunar cheese spore formations ({@code MOON_CHEESE_LOG}, {@code MOON_CHEESE_LEAVES}, {@code MOON_CHEESE_BLOCK})
 * inspired by MorePlanets's {@code WorldGenCheeseSporeTree}.
 */
public class MoonCaveFloraFeature extends Feature<NoneFeatureConfiguration> {

    public MoonCaveFloraFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        if (origin.getY() >= surfaceY - 8) {
            return false;
        }

        // Scan vertically near origin to locate an underground cave air pocket
        BlockPos.MutableBlockPos scan = origin.mutable();
        boolean foundAir = false;
        for (int i = 0; i < 10; i++) {
            if (level.getBlockState(scan).isAir()) {
                foundAir = true;
                break;
            }
            scan.move(Direction.DOWN);
        }
        if (!foundAir) {
            return false;
        }

        int radius = 3 + random.nextInt(4); // 3 to 6 blocks radius
        int placedCount = 0;
        BlockPos.MutableBlockPos col = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distSq = dx * dx + dz * dz;
                double maxDistSq = (radius + 0.5) * (radius + 0.5);
                if (distSq > maxDistSq) {
                    continue;
                }

                int bx = scan.getX() + dx;
                int bz = scan.getZ() + dz;
                int colSurfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, bx, bz);
                if (scan.getY() >= colSurfaceY - 7) {
                    continue;
                }

                // 1. Cave ceiling: find solid ceiling above the air pocket and place MOON_MOSS + hanging MOON_TANGLE
                col.set(bx, scan.getY(), bz);
                int stepsUp = 0;
                while (stepsUp < 10 && level.getBlockState(col).isAir()) {
                    col.move(Direction.UP);
                    stepsUp++;
                }
                if (stepsUp > 0 && isValidCaveRock(level.getBlockState(col)) && col.getY() < colSurfaceY - 6) {
                    level.setBlock(col, GCBlocks.MOON_MOSS.defaultBlockState(), Block.UPDATE_CLIENTS);
                    placedCount++;

                    if (random.nextInt(3) == 0) {
                        placeHangingTangle(level, col.below(), random);
                    }
                }

                // 2. Cave floor: find solid floor below the air pocket and place MOON_MOSS + MOON_WEED / MOON_SHRUBS
                col.set(bx, scan.getY(), bz);
                int stepsDown = 0;
                while (stepsDown < 10 && level.getBlockState(col).isAir()) {
                    col.move(Direction.DOWN);
                    stepsDown++;
                }
                if (stepsDown > 0 && isValidCaveRock(level.getBlockState(col)) && col.getY() < colSurfaceY - 8) {
                    level.setBlock(col, GCBlocks.MOON_MOSS.defaultBlockState(), Block.UPDATE_CLIENTS);
                    placedCount++;

                    BlockPos aboveFloor = col.above();
                    if (level.getBlockState(aboveFloor).isAir()) {
                        if ( dx == 0 && dz == 0 && random.nextInt(10) == 0) {
                            placeSubterraneanCheeseSpore(level, aboveFloor, random);
                        } else if (random.nextInt(3) == 0) {
                            BlockState flora = random.nextBoolean()
                                    ? GCBlocks.MOON_WEED.defaultBlockState()
                                    : GCBlocks.MOON_SHRUBS.defaultBlockState();
                            level.setBlock(aboveFloor, flora, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }

        return placedCount > 0;
    }

    private static boolean isValidCaveRock(BlockState state) {
        return state.is(GCBlocks.MOON_ROCK)
                || state.is(GCBlocks.LUNASLATE)
                || state.is(GCBlocks.MOON_DIRT)
                || state.is(GCBlocks.MOON_BASALT)
                || state.is(GCBlocks.OLIVINE_BASALT);
    }

    private static void placeHangingTangle(WorldGenLevel level, BlockPos startPos, RandomSource random) {
        int maxLen = 1 + random.nextInt(5); // 1 to 5 blocks long
        int actualLen = 0;
        BlockPos.MutableBlockPos cursor = startPos.mutable();

        while (actualLen < maxLen && level.getBlockState(cursor).isAir() && level.getBlockState(cursor.below()).isAir()) {
            actualLen++;
            cursor.move(Direction.DOWN);
        }
        if (actualLen == 0) {
            return;
        }

        boolean hasFruit = actualLen >= 2 && random.nextInt(3) == 0 && level.getBlockState(startPos.below(actualLen)).isAir();
        for (int i = 0; i < actualLen; i++) {
            BlockPos pos = startPos.below(i);
            if (i < actualLen - 1) {
                level.setBlock(pos, GCBlocks.MOON_TANGLE.defaultBlockState().setValue(MoonTangleBlock.PART, MoonTangleBlock.Part.MIDDLE), Block.UPDATE_CLIENTS);
            } else if (hasFruit) {
                level.setBlock(pos, GCBlocks.MOON_TANGLE.defaultBlockState().setValue(MoonTangleBlock.PART, MoonTangleBlock.Part.FRUIT_TOP), Block.UPDATE_CLIENTS);
                level.setBlock(pos.below(), GCBlocks.MOON_TANGLE.defaultBlockState().setValue(MoonTangleBlock.PART, MoonTangleBlock.Part.FRUIT_BOTTOM), Block.UPDATE_CLIENTS);
            } else {
                level.setBlock(pos, GCBlocks.MOON_TANGLE.defaultBlockState().setValue(MoonTangleBlock.PART, MoonTangleBlock.Part.END), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void placeSubterraneanCheeseSpore(WorldGenLevel level, BlockPos basePos, RandomSource random) {
        int stemHeight = 2 + random.nextInt(2); // 2 to 3 blocks tall
        for (int dy = 0; dy <= stemHeight + 1; dy++) {
            if (!level.getBlockState(basePos.above(dy)).isAir()) {
                return;
            }
        }

        for (int dy = 0; dy < stemHeight; dy++) {
            level.setBlock(basePos.above(dy), GCBlocks.MOON_CHEESE_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        BlockPos capCenter = basePos.above(stemHeight);
        level.setBlock(capCenter, GCBlocks.MOON_CHEESE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    BlockPos topCap = capCenter.above();
                    if (level.getBlockState(topCap).isAir()) {
                        level.setBlock(topCap, GCBlocks.MOON_CHEESE_LEAVES.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    continue;
                }
                if (Math.abs(dx) == 1 && Math.abs(dz) == 1 && random.nextBoolean()) {
                    continue;
                }
                BlockPos leafPos = capCenter.offset(dx, 0, dz);
                if (level.getBlockState(leafPos).isAir()) {
                    level.setBlock(leafPos, GCBlocks.MOON_CHEESE_LEAVES.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
