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
import dev.galacticraft.mod.world.biome.GCBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of GC4 Legacy {@code WorldGenLakesVenus} combined with GalaxySpace {@code BiomeDecoratorIo.sulfurlakesGen}.
 * Generates volcanic lava lakes (and sulfuric acid lakes in Venus valleys) bordered by {@link GCBlocks#VOLCANIC_ROCK}.
 */
public class VenusLavaLakeFeature extends Feature<NoneFeatureConfiguration> {
    public VenusLavaLakeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos position = context.origin().below(4);
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();

        if (position.getY() <= level.getMinBuildHeight() + 8) {
            return false;
        }

        boolean isAcidLake = level.getBiome(position).is(GCBiomes.Venus.VENUS_VALLEY) && rand.nextInt(4) == 0;
        BlockState fluidState = isAcidLake ? GCBlocks.SULFURIC_ACID.defaultBlockState() : Blocks.LAVA.defaultBlockState();
        BlockState rimState = isAcidLake ? GCBlocks.PUMICE.defaultBlockState() : GCBlocks.VOLCANIC_ROCK.defaultBlockState();

        boolean[] shape = new boolean[2048];
        int blobs = rand.nextInt(4) + 4;

        for (int i = 0; i < blobs; ++i) {
            double rx = rand.nextDouble() * 6.0D + 3.0D;
            double ry = rand.nextDouble() * 4.0D + 2.0D;
            double rz = rand.nextDouble() * 6.0D + 3.0D;
            double cx = rand.nextDouble() * (16.0D - rx - 2.0D) + 1.0D + rx / 2.0D;
            double cy = rand.nextDouble() * (8.0D - ry - 4.0D) + 2.0D + ry / 2.0D;
            double cz = rand.nextDouble() * (16.0D - rz - 2.0D) + 1.0D + rz / 2.0D;

            for (int x = 1; x < 15; ++x) {
                for (int z = 1; z < 15; ++z) {
                    for (int y = 1; y < 7; ++y) {
                        double dx = ((double) x - cx) / (rx / 2.0D);
                        double dy = ((double) y - cy) / (ry / 2.0D);
                        double dz = ((double) z - cz) / (rz / 2.0D);
                        if (dx * dx + dy * dy + dz * dz < 1.0D) {
                            shape[(x * 16 + z) * 8 + y] = true;
                        }
                    }
                }
            }
        }

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    boolean isBorder = !shape[(x * 16 + z) * 8 + y] && (
                            x < 15 && shape[((x + 1) * 16 + z) * 8 + y]
                                    || x > 0 && shape[((x - 1) * 16 + z) * 8 + y]
                                    || z < 15 && shape[(x * 16 + z + 1) * 8 + y]
                                    || z > 0 && shape[(x * 16 + (z - 1)) * 8 + y]
                                    || y < 7 && shape[(x * 16 + z) * 8 + y + 1]
                                    || y > 0 && shape[(x * 16 + z) * 8 + (y - 1)]
                    );

                    if (isBorder) {
                        BlockState checkState = level.getBlockState(position.offset(x, y, z));
                        if (y >= 4 && !checkState.liquid()) {
                            return false;
                        }
                        if (y < 4 && !checkState.isSolid() && !checkState.is(fluidState.getBlock())) {
                            return false;
                        }
                    }
                }
            }
        }

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    if (shape[(x * 16 + z) * 8 + y]) {
                        level.setBlock(position.offset(x, y, z), y >= 4 ? Blocks.AIR.defaultBlockState() : fluidState, 2);
                    }
                }
            }
        }

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    boolean isBorder = !shape[(x * 16 + z) * 8 + y] && (
                            x < 15 && shape[((x + 1) * 16 + z) * 8 + y]
                                    || x > 0 && shape[((x - 1) * 16 + z) * 8 + y]
                                    || z < 15 && shape[(x * 16 + z + 1) * 8 + y]
                                    || z > 0 && shape[(x * 16 + (z - 1)) * 8 + y]
                                    || y < 7 && shape[(x * 16 + z) * 8 + y + 1]
                                    || y > 0 && shape[(x * 16 + z) * 8 + (y - 1)]
                    );

                    if (isBorder && (y < 4 || rand.nextInt(2) != 0) && level.getBlockState(position.offset(x, y, z)).isSolid()) {
                        level.setBlock(position.offset(x, y, z), rimState, 2);
                    }
                }
            }
        }

        return true;
    }
}
