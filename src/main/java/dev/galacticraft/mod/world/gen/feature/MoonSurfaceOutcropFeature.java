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
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates natural lunar regolith/basalt rock patches and erratic boulder clusters with exposed
 * lunar ores across the Moon's surface, inspired by GalaxySpace's {@code WorldGenCircleBlock}
 * and MorePlanets's lunar surface rock decorators.
 */
public class MoonSurfaceOutcropFeature extends Feature<NoneFeatureConfiguration> {

    // On the Moon surface, only Moon Cheese Ore is exposed naturally
    private static final BlockState[] MOON_SURFACE_ORES = new BlockState[]{
            GCBlocks.MOON_CHEESE_ORE.defaultBlockState()
    };

    public MoonSurfaceOutcropFeature(Codec<NoneFeatureConfiguration> codec) {
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
        if (centerY < 48 || centerY > 165) {
            return false;
        }

        BlockPos centerPos = new BlockPos(centerX, centerY, centerZ);
        BlockState surface = level.getBlockState(centerPos);
        if (surface.isAir() || !surface.getFluidState().isEmpty()) {
            return false;
        }

        Holder<Biome> biome = level.getBiome(centerPos);
        int patchRadius = 2 + random.nextInt(4); // 2 to 5 blocks radius
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // 1. Irregular geological surface rock / basalt / ice transition circle (GalaxySpace WorldGenCircleBlock style)
        for (int dx = -patchRadius; dx <= patchRadius; dx++) {
            for (int dz = -patchRadius; dz <= patchRadius; dz++) {
                double distSq = dx * dx + dz * dz;
                double jitterRadius = patchRadius + (random.nextDouble() - 0.5) * 1.4;
                double maxDistSq = jitterRadius * jitterRadius;
                if (distSq > maxDistSq) {
                    continue;
                }

                int bx = centerX + dx;
                int bz = centerZ + dz;
                int by = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bx, bz) - 1;
                if (by < 45) {
                    continue;
                }

                pos.set(bx, by, bz);
                BlockState existing = level.getBlockState(pos);
                if (existing.isAir() || !existing.getFluidState().isEmpty()) {
                    continue;
                }

                BlockState replacement = pickPatchBlock(random, biome, distSq / maxDistSq);
                level.setBlock(pos, replacement, Block.UPDATE_CLIENTS);

                if (distSq < maxDistSq * 0.4 && random.nextBoolean()) {
                    pos.set(bx, by - 1, bz);
                    level.setBlock(pos, GCBlocks.MOON_ROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }

        // 2. Erratic lunar ejecta boulders with exposed ore veins
        int boulderClusters = 1 + random.nextInt(2);
        for (int c = 0; c < boulderClusters; c++) {
            int cx = centerX + random.nextInt(patchRadius) - random.nextInt(patchRadius);
            int cz = centerZ + random.nextInt(patchRadius) - random.nextInt(patchRadius);
            int cy = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
            BlockPos clusterCenter = new BlockPos(cx, cy, cz);

            if (!level.getBlockState(clusterCenter.below()).isAir()) {
                level.setBlock(clusterCenter, pickBoulderRock(random, biome), Block.UPDATE_CLIENTS);
            }

            int blocksInBoulder = 5 + random.nextInt(5);
            for (int i = 0; i < blocksInBoulder; i++) {
                int ox = cx + random.nextInt(2) - random.nextInt(2);
                int oz = cz + random.nextInt(2) - random.nextInt(2);
                int oy = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, ox, oz) - (random.nextInt(3) == 0 ? 0 : 1);
                BlockPos target = new BlockPos(ox, oy, oz);
                if (!level.getBlockState(target.below()).isAir()) {
                    level.setBlock(target, pickBoulderRock(random, biome), Block.UPDATE_CLIENTS);
                }
            }
        }

        return true;
    }

    private static BlockState pickPatchBlock(RandomSource random, Holder<Biome> biome, double normalizedDist) {
        int roll = random.nextInt(24);
        if (roll == 0) {
            return MOON_SURFACE_ORES[random.nextInt(MOON_SURFACE_ORES.length)];
        }
        if (biome.is(GCBiomes.Moon.COMET_TUNDRA)) {
            if (normalizedDist < 0.5 && roll < 12) {
                return GCBlocks.DENSE_ICE.defaultBlockState();
            }
            return roll < 18 ? GCBlocks.MOON_SURFACE_ROCK.defaultBlockState() : GCBlocks.COBBLED_MOON_ROCK.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.OLIVINE_SPIKES)) {
            if (roll < 6) {
                return GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState();
            } else if (roll < 15) {
                return GCBlocks.OLIVINE_BASALT.defaultBlockState();
            }
            return GCBlocks.MOON_BASALT.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.BASALTIC_MARE)) {
            if (roll < 14) {
                return GCBlocks.MOON_BASALT.defaultBlockState();
            } else if (roll < 20) {
                return GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
            }
            return GCBlocks.OLIVINE_BASALT.defaultBlockState();
        }
        if (roll < 12) {
            return GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
        } else if (roll < 18) {
            return GCBlocks.COBBLED_MOON_ROCK.defaultBlockState();
        } else if (roll < 22) {
            return GCBlocks.MOON_ROCK.defaultBlockState();
        } else {
            return GCBlocks.MOON_BASALT.defaultBlockState();
        }
    }

    private static BlockState pickBoulderRock(RandomSource random, Holder<Biome> biome) {
        if (random.nextInt(4) == 0) {
            return MOON_SURFACE_ORES[random.nextInt(MOON_SURFACE_ORES.length)];
        }
        if (biome.is(GCBiomes.Moon.COMET_TUNDRA) && random.nextInt(3) == 0) {
            return GCBlocks.DENSE_ICE.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.OLIVINE_SPIKES)) {
            return random.nextInt(3) == 0
                    ? GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState()
                    : GCBlocks.OLIVINE_BASALT.defaultBlockState();
        }
        int roll = random.nextInt(10);
        if (roll < 4) {
            return GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
        } else if (roll < 7) {
            return GCBlocks.COBBLED_MOON_ROCK.defaultBlockState();
        } else if (roll < 9) {
            return GCBlocks.MOON_BASALT.defaultBlockState();
        } else {
            return GCBlocks.MOON_ROCK.defaultBlockState();
        }
    }
}
