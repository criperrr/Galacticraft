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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of ExtraPlanets {@code WorldGenVolcano} and GalaxySpace volcanic vents.
 * Generates a natural volcanic cone of {@link GCBlocks#VOLCANIC_ROCK} and {@link GCBlocks#SCORCHED_VENUS_ROCK}
 * with a central magma chimney and bubbling {@link Blocks#LAVA} caldera.
 */
public class VenusVolcanoFeature extends Feature<NoneFeatureConfiguration> {
    public VenusVolcanoFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();

        int baseY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        if (baseY <= 52 || baseY >= 150) {
            return false;
        }

        // Check that surrounding ground is reasonably supported
        int minCornerY = baseY;
        for (int dx = -6; dx <= 6; dx += 6) {
            for (int dz = -6; dz <= 6; dz += 6) {
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX() + dx, origin.getZ() + dz);
                if (Math.abs(y - baseY) > 10) {
                    return false;
                }
                minCornerY = Math.min(minCornerY, y);
            }
        }

        int baseRadius = 8 + rand.nextInt(4); // 8..11
        int height = 7 + rand.nextInt(5);     // 7..11
        int craterRadius = 2;

        BlockState volcanicRock = GCBlocks.VOLCANIC_ROCK.defaultBlockState();
        BlockState scorchedRock = GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState();
        BlockState hardRock = GCBlocks.HARD_VENUS_ROCK.defaultBlockState();
        BlockState lava = Blocks.LAVA.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        BlockPos baseCenter = new BlockPos(origin.getX(), minCornerY - 2, origin.getZ());

        for (int dy = 0; dy <= height + 2; ++dy) {
            double progress = (double) dy / (double) height;
            double layerRadius = baseRadius * (1.0D - 0.72D * Math.pow(progress, 0.85D));

            for (int dx = -baseRadius - 1; dx <= baseRadius + 1; ++dx) {
                for (int dz = -baseRadius - 1; dz <= baseRadius + 1; ++dz) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    // Slight organic irregularity on outer slopes
                    double jitter = ((dx * 31 + dz * 17 + dy * 13) & 3) * 0.22D;
                    BlockPos pos = baseCenter.offset(dx, dy, dz);

                    if (dy <= height) {
                        if (dist <= craterRadius + (dy > height - 2 ? 0.6D : 0.0D)) {
                            if (dy <= height - 2) {
                                level.setBlock(pos, lava, 2);
                            } else {
                                level.setBlock(pos, air, 2);
                            }
                        } else if (dist <= layerRadius + jitter) {
                            if (dist <= craterRadius + 1.5D || dy >= height - 1) {
                                level.setBlock(pos, rand.nextInt(3) == 0 ? scorchedRock : volcanicRock, 2);
                            } else {
                                level.setBlock(pos, rand.nextInt(5) == 0 ? hardRock : volcanicRock, 2);
                            }
                        }
                    } else if (dist <= craterRadius + 1.5D) {
                        level.setBlock(pos, air, 2);
                    }
                }
            }
        }

        // Deep lava magma throat extending down 10 blocks beneath the volcano
        for (int dy = -10; dy < 0; ++dy) {
            for (int dx = -craterRadius; dx <= craterRadius; ++dx) {
                for (int dz = -craterRadius; dz <= craterRadius; ++dz) {
                    if (dx * dx + dz * dz <= craterRadius * craterRadius + 1) {
                        BlockPos pos = baseCenter.offset(dx, dy, dz);
                        level.setBlock(pos, dx * dx + dz * dz <= 1 ? lava : volcanicRock, 2);
                    }
                }
            }
        }

        return true;
    }
}
