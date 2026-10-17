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
import dev.galacticraft.mod.world.gen.noise.DensityCellular2D;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates natural volcanic basalt/scorched rock patches with scattered surface ore blocks
 * and geothermal magma fissures across Venus, directly inspired by TFCVolcanoes's {@code PyroclasticBomb.placeBlocks}
 * and the earlier geological outcrop patches requested by the user.
 */
public class VenusSurfaceOutcropFeature extends Feature<NoneFeatureConfiguration> {

    private static final BlockState[] VENUS_SURFACE_ORES = new BlockState[]{
            GCBlocks.GALENA_ORE.defaultBlockState(),
            GCBlocks.SOLAR_ORE.defaultBlockState(),
            GCBlocks.VENUS_COPPER_ORE.defaultBlockState(),
            GCBlocks.VENUS_TIN_ORE.defaultBlockState(),
            GCBlocks.VENUS_ALUMINUM_ORE.defaultBlockState()
    };

    public VenusSurfaceOutcropFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int centerX = origin.getX();
        int centerZ = origin.getZ();
        int centerY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX, centerZ) - 1;
        if (centerY < 45 || centerY > 180) {
            return false;
        }

        BlockState surface = level.getBlockState(new BlockPos(centerX, centerY, centerZ));
        if (surface.isAir() || !surface.getFluidState().isEmpty()) {
            return false;
        }

        // Sample thermal hotspot intensity from TFCVolcanoes cellular/value noise
        double thermalIntensity = DensityCellular2D.sampleThermalHotspot(level.getSeed(), centerX, centerZ);
        boolean isHotZone = thermalIntensity > 0.58;

        int patchRadius = 3 + random.nextInt(5); // 3 to 7 blocks radius
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // 1. Irregular geological surface transition patch (basalt, smooth basalt, scorched rock, magma in hot zones)
        for (int dx = -patchRadius; dx <= patchRadius; dx++) {
            for (int dz = -patchRadius; dz <= patchRadius; dz++) {
                double distSq = dx * dx + dz * dz;
                double maxDistSq = (patchRadius + (random.nextDouble() - 0.5) * 1.6) * (patchRadius + (random.nextDouble() - 0.5) * 1.6);
                if (distSq > maxDistSq) {
                    continue;
                }

                int bx = centerX + dx;
                int bz = centerZ + dz;
                int by = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bx, bz) - 1;
                if (by < 40) {
                    continue;
                }

                pos.set(bx, by, bz);
                BlockState existing = level.getBlockState(pos);
                if (existing.isAir() || !existing.getFluidState().isEmpty()) {
                    continue;
                }

                BlockState replacement = pickPatchBlock(random, isHotZone, distSq / maxDistSq);
                level.setBlock(pos, replacement, Block.UPDATE_CLIENTS);

                if (distSq < maxDistSq * 0.45 && random.nextBoolean()) {
                    pos.set(bx, by - 1, bz);
                    level.setBlock(pos, Blocks.BASALT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }

        // 2. TFCVolcanoes PyroclasticBomb.placeBlocks-style rock & ore boulder clusters on the patch
        int boulderClusters = 1 + random.nextInt(3);
        for (int c = 0; c < boulderClusters; c++) {
            int cx = centerX + random.nextInt(patchRadius) - random.nextInt(patchRadius);
            int cz = centerZ + random.nextInt(patchRadius) - random.nextInt(patchRadius);
            int cy = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
            BlockPos clusterCenter = new BlockPos(cx, cy, cz);

            if (!level.getBlockState(clusterCenter.below()).isAir()) {
                level.setBlock(clusterCenter, getRandomOutcropRock(random, isHotZone), Block.UPDATE_CLIENTS);
            }

            // Directly mirrors PyroclasticBomb.placeBlocks (15 offsets around center)
            for (int i = 0; i < 10; i++) {
                int ox = cx + random.nextInt(2) - random.nextInt(2);
                int oz = cz + random.nextInt(2) - random.nextInt(2);
                int oy = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, ox, oz) - (random.nextInt(3) == 0 ? 0 : 1);
                BlockPos target = new BlockPos(ox, oy, oz);
                if (!level.getBlockState(target.below()).isAir()) {
                    level.setBlock(target, getRandomOutcropRock(random, isHotZone), Block.UPDATE_CLIENTS);
                }
            }
        }

        return true;
    }

    private BlockState pickPatchBlock(RandomSource random, boolean isHotZone, double normalizedDist) {
        int roll = random.nextInt(20);
        if (roll == 0) {
            return VENUS_SURFACE_ORES[random.nextInt(VENUS_SURFACE_ORES.length)];
        }
        if (isHotZone) {
            if (normalizedDist < 0.35 && roll < 7) {
                return Blocks.MAGMA_BLOCK.defaultBlockState();
            } else if (roll < 12) {
                return GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState();
            } else if (roll < 17) {
                return Blocks.BASALT.defaultBlockState();
            } else {
                return GCBlocks.VOLCANIC_ROCK.defaultBlockState();
            }
        } else {
            if (roll < 9) {
                return Blocks.BASALT.defaultBlockState();
            } else if (roll < 14) {
                return Blocks.SMOOTH_BASALT.defaultBlockState();
            } else if (roll < 18) {
                return GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState();
            } else {
                return GCBlocks.HARD_VENUS_ROCK.defaultBlockState();
            }
        }
    }

    private BlockState getRandomOutcropRock(RandomSource random, boolean isHotZone) {
        // High chance of scattered ore blocks on the outcrop, just like TFCVolcanoes PyroclasticBomb.getRandomRock()
        if (random.nextInt(3) == 0) {
            return VENUS_SURFACE_ORES[random.nextInt(VENUS_SURFACE_ORES.length)];
        }
        int roll = random.nextInt(10);
        if (roll < 4) {
            return Blocks.BASALT.defaultBlockState();
        } else if (roll < 6) {
            return Blocks.SMOOTH_BASALT.defaultBlockState();
        } else if (roll < 8) {
            return GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState();
        } else if (isHotZone && roll == 8) {
            return Blocks.MAGMA_BLOCK.defaultBlockState();
        } else {
            return Blocks.OBSIDIAN.defaultBlockState();
        }
    }
}
