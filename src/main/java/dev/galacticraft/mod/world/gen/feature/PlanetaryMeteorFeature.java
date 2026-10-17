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
import dev.galacticraft.mod.content.block.environment.FallenMeteorBlock;
import dev.galacticraft.mod.world.gen.feature.config.PlanetaryMeteorConfig;
import dev.galacticraft.mod.world.gen.noise.DensityCellular2D;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.List;

/**
 * Multi-chunk planetary meteor impact crater and asteroid ore deposit generator.
 * <p>
 * Uses deterministic {@link DensityCellular2D} Voronoi cells so that:
 * <ul>
 *   <li>Meteor craters are genuinely rare landmarks (controlled by {@code regionSize} and {@code spawnProbability})</li>
 *   <li>Craters can be large (30 to 52+ blocks across) and deep without ever causing far-chunk {@code setBlock} errors</li>
 *   <li>The central asteroid mass is built of solid full blocks ({@code RAW_METEORIC_IRON_BLOCK}, {@code OBSIDIAN},
 *       {@code MAGMA_BLOCK}, and diverse meteor-borne ores), while individual {@link FallenMeteorBlock} fragments rest
 *       singly on solid ground (never stacked into floating mini-cubes)</li>
 * </ul>
 */
public class PlanetaryMeteorFeature extends Feature<PlanetaryMeteorConfig> {

    public PlanetaryMeteorFeature(Codec<PlanetaryMeteorConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<PlanetaryMeteorConfig> context) {
        WorldGenLevel level = context.level();
        PlanetaryMeteorConfig config = context.config();
        BlockPos origin = context.origin();
        long seed = level.getSeed();

        ChunkPos chunkPos = new ChunkPos(origin);
        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int sampleX = chunkPos.getMiddleBlockX();
        int sampleZ = chunkPos.getMiddleBlockZ();

        // 1. Sample deterministic Voronoi cell (offset seed from volcanoes so meteor craters and volcanoes never overlap)
        DensityCellular2D cellularNoise = new DensityCellular2D(seed ^ 0x4F1BBCDCBFA53E09L, config.regionSize(), config.spread(), 1, 3);
        DensityCellular2D.Cell cell = cellularNoise.cell(sampleX, sampleZ);

        if (!cellularNoise.checkCellRarity(cell, config.spawnProbability()) || cellularNoise.maxSafeDiameter(cell) <= 0.14) {
            return false;
        }

        int centerX = DensityCellular2D.fastFloor(cell.x());
        int centerZ = DensityCellular2D.fastFloor(cell.z());

        // Also ensure we are not overlapping a volcano cell center
        DensityCellular2D volcanoGrid = new DensityCellular2D(seed + 7653L, 560.0, 0.75, 1, 3);
        DensityCellular2D.Cell volcanoCell = volcanoGrid.cell(centerX, centerZ);
        if (Math.hypot(centerX - volcanoCell.x(), centerZ - volcanoCell.z()) < 85.0) {
            return false;
        }

        long cellHash = DensityCellular2D.hash(cell.id(), seed, 0x9E3779B97F4A7C15L);
        double craterRoll = DensityCellular2D.jitter(cellHash ^ 0xBF58476D1CE4E5B9L);
        double meteorRoll = DensityCellular2D.jitter(cellHash ^ 0x94D049BB133111EBL);

        int craterRadius = Mth.lerpInt((float) craterRoll, config.minCraterRadius(), config.maxCraterRadius());
        int craterDepth = Math.max(7, (int) Math.round(craterRadius * 0.56));
        int meteorRadius = Mth.lerpInt((float) meteorRoll, config.minMeteorRadius(), config.maxMeteorRadius());
        double outerApronRadius = craterRadius * 1.38;

        // Skip chunk immediately if it does not intersect the crater + ejecta apron
        double chunkDist = Math.hypot(sampleX - centerX, sampleZ - centerZ);
        if (chunkDist > outerApronRadius + 16.0) {
            return false;
        }

        // 2. Query terrain height at impact center via ChunkGenerator.getBaseHeight (safe across unloaded chunks)
        ChunkGenerator generator = context.chunkGenerator();
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        int centerGroundY = generator.getBaseHeight(centerX, centerZ, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState) - 1;
        if (centerGroundY < 50 || centerGroundY > 155) {
            return false;
        }

        int craterBottomY = Math.max(12, centerGroundY - craterDepth);
        int meteorCenterY = craterBottomY + Math.max(1, meteorRadius - 1);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean modified = false;

        // 3. Shape only columns within the current 16x16 chunk (100% chunk-safe!)
        for (int lx = 0; lx < 16; lx++) {
            int worldX = chunkMinX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int worldZ = chunkMinZ + lz;
                double dx = worldX - centerX;
                double dz = worldZ - centerZ;
                double rawDist = Math.hypot(dx, dz);

                if (rawDist > outerApronRadius) {
                    continue;
                }

                double angle = Math.atan2(dz, dx);
                double angularJitter = Math.sin(angle * 6.0 + DensityCellular2D.jitter(cellHash) * 6.28) * 0.06
                        + Math.cos(angle * 11.0 - DensityCellular2D.jitter(cellHash >> 8) * 6.28) * 0.04;
                double spatialJitter = DensityCellular2D.valueNoise2D(seed ^ 0x517CC1B7L, worldX * 0.09, worldZ * 0.09) * 0.07;

                double effectiveCraterRadius = craterRadius * (1.0 + angularJitter + spatialJitter);
                double norm = rawDist / Math.max(1.0, effectiveCraterRadius);
                if (norm > 1.35) {
                    continue;
                }

                int localSurfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, worldX, worldZ) - 1;
                if (localSurfaceY <= 10) {
                    continue;
                }

                RandomSource colRand = RandomSource.create(DensityCellular2D.hash(worldX, worldZ, seed ^ 0x27D4EB2FL));

                if (norm <= 0.92) {
                    // Inside the parabolic impact crater bowl
                    double bowlRatio = norm / 0.92;
                    // Steep inner crater walls and deep impact bowl
                    double depthCurve = 1.0 - Math.pow(bowlRatio, 1.85);
                    int excavate = Math.max(1, (int) Math.round(craterDepth * depthCurve));

                    // Smoothly anchor deep center to centerGroundY while outer wall blends to localSurfaceY
                    double refY = Mth.lerp(bowlRatio * bowlRatio, centerGroundY, localSurfaceY);
                    int floorY = Math.max(10, (int) Math.round(refY) - excavate);

                    // Carve air above crater floor
                    for (int y = localSurfaceY + 4; y > floorY; y--) {
                        cursor.set(worldX, y, worldZ);
                        if (!level.getBlockState(cursor).isAir()) {
                            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    }

                    // Line the crater floor and sub-floor (3 blocks thick) with impact melt, basalt, obsidian, and ores
                    for (int d = 0; d <= 2; d++) {
                        cursor.set(worldX, floorY - d, worldZ);
                        level.setBlock(cursor, pickCraterFloorBlock(config, colRand, cursor, bowlRatio, d), Block.UPDATE_CLIENTS);
                    }

                    // 4. Solid Asteroid & Rich Ore Core in the center of the crater (NO stacked non-full FallenMeteorBlock!)
                    if (rawDist <= meteorRadius + 1.25) {
                        for (int y = floorY - 1; y <= meteorCenterY + meteorRadius; y++) {
                            double dy = (y - meteorCenterY) * 1.15;
                            double dist3d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            double wobble = (colRand.nextDouble() - 0.5) * 0.45;
                            if (dist3d <= meteorRadius + wobble) {
                                cursor.set(worldX, y, worldZ);
                                BlockState boulderBlock = pickSolidMeteorBoulderBlock(config, colRand, cursor, dist3d / Math.max(1.0, meteorRadius));
                                level.setBlock(cursor, boulderBlock, Block.UPDATE_CLIENTS);
                            }
                        }

                        // Place a single grounded FallenMeteorBlock fragment resting on top of the solid boulder center
                        if (Math.abs(dx) <= 1.0 && Math.abs(dz) <= 1.0 && colRand.nextInt(2) == 0) {
                            int topSolidY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
                            cursor.set(worldX, topSolidY - 1, worldZ);
                            BlockState below = level.getBlockState(cursor);
                            if (!below.isAir() && !below.is(GCBlocks.FALLEN_METEOR)) {
                                cursor.set(worldX, topSolidY, worldZ);
                                BlockState fallen = config.meteorShellBlock().getState(colRand, cursor);
                                if (fallen.is(GCBlocks.FALLEN_METEOR)) {
                                    fallen = fallen.setValue(FallenMeteorBlock.HEAT, Mth.randomBetweenInclusive(colRand, 4, 5));
                                }
                                level.setBlock(cursor, fallen, Block.UPDATE_CLIENTS);
                            }
                        }
                    } else if (bowlRatio > 0.28 && bowlRatio < 0.78 && colRand.nextInt(45) == 0) {
                        // Occasional single FallenMeteorBlock fragment resting on the solid crater floor
                        cursor.set(worldX, floorY, worldZ);
                        if (!level.getBlockState(cursor).isAir() && !level.getBlockState(cursor).is(GCBlocks.FALLEN_METEOR)) {
                            cursor.set(worldX, floorY + 1, worldZ);
                            BlockState fallen = config.meteorShellBlock().getState(colRand, cursor);
                            if (fallen.is(GCBlocks.FALLEN_METEOR)) {
                                fallen = fallen.setValue(FallenMeteorBlock.HEAT, Mth.randomBetweenInclusive(colRand, 3, 5));
                            }
                            level.setBlock(cursor, fallen, Block.UPDATE_CLIENTS);
                        }
                    }

                    modified = true;
                } else {
                    // Raised impact crater rim & ejecta blanket (0.92 < norm <= 1.35)
                    double rimArch = Math.sin(((norm - 0.92) / 0.43) * Math.PI);
                    int rimBoost = (int) Math.round(rimArch * Math.max(2.0, craterDepth * 0.28));

                    if (rimBoost > 0 || colRand.nextFloat() < (float) (1.35 - norm) * 1.8F) {
                        for (int y = localSurfaceY - 1; y <= localSurfaceY + rimBoost; y++) {
                            cursor.set(worldX, y, worldZ);
                            level.setBlock(cursor, pickRimEjectaBlock(config, colRand, cursor, y == localSurfaceY + rimBoost), Block.UPDATE_CLIENTS);
                        }
                        modified = true;
                    }
                }
            }
        }

        return modified;
    }

    /**
     * Selects a solid full-cube block for the impacted asteroid core inside the crater:
     * Raw Meteoric Iron blocks, diverse planetary/asteroid ores, obsidian, crying obsidian, and magma.
     */
    private static BlockState pickSolidMeteorBoulderBlock(
            PlanetaryMeteorConfig config,
            RandomSource rand,
            BlockPos pos,
            double normalizedRadius
    ) {
        List<BlockState> ores = config.ejectaOres();
        // Inner core is rich in Raw Meteoric Iron and diverse ores brought by the meteor
        if (normalizedRadius < 0.55) {
            if (rand.nextInt(3) != 0) {
                return config.meteorCoreBlock().getState(rand, pos);
            }
            if (!ores.isEmpty()) {
                return ores.get(rand.nextInt(ores.size()));
            }
        }

        // Outer crust of the solid meteor boulder: meteoric iron, rich ores, obsidian, crying obsidian, and basalt
        if (!ores.isEmpty() && rand.nextInt(Math.max(2, config.oreChance() - 1)) == 0) {
            return ores.get(rand.nextInt(ores.size()));
        }

        int roll = rand.nextInt(12);
        if (roll < 3) {
            return config.meteorCoreBlock().getState(rand, pos);
        } else if (roll < 6) {
            return Blocks.OBSIDIAN.defaultBlockState();
        } else if (roll == 6) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        } else if (roll < 9) {
            return config.craterFloorBlock().getState(rand, pos);
        } else {
            return Blocks.BASALT.defaultBlockState();
        }
    }

    private static BlockState pickCraterFloorBlock(
            PlanetaryMeteorConfig config,
            RandomSource rand,
            BlockPos pos,
            double bowlRatio,
            int depth
    ) {
        List<BlockState> ores = config.ejectaOres();
        if (!ores.isEmpty() && rand.nextInt(config.oreChance() * (depth + 1)) == 0) {
            return ores.get(rand.nextInt(ores.size()));
        }
        if (depth == 0 && bowlRatio < 0.48 && rand.nextInt(3) == 0) {
            return config.craterFloorBlock().getState(rand, pos);
        }
        int roll = rand.nextInt(10);
        if (roll < 4) {
            return config.scorchedRimBlock().getState(rand, pos);
        } else if (roll < 7) {
            return Blocks.BASALT.defaultBlockState();
        } else if (roll < 9) {
            return Blocks.SMOOTH_BASALT.defaultBlockState();
        } else {
            return Blocks.OBSIDIAN.defaultBlockState();
        }
    }

    private static BlockState pickRimEjectaBlock(
            PlanetaryMeteorConfig config,
            RandomSource rand,
            BlockPos pos,
            boolean isTopSurface
    ) {
        List<BlockState> ores = config.ejectaOres();
        if (isTopSurface && !ores.isEmpty() && rand.nextInt(config.oreChance() * 2) == 0) {
            return ores.get(rand.nextInt(ores.size()));
        }
        int roll = rand.nextInt(12);
        if (roll < 5) {
            return config.scorchedRimBlock().getState(rand, pos);
        } else if (roll < 9) {
            return Blocks.BASALT.defaultBlockState();
        } else if (roll < 11) {
            return Blocks.SMOOTH_BASALT.defaultBlockState();
        } else {
            return isTopSurface ? config.craterFloorBlock().getState(rand, pos) : Blocks.OBSIDIAN.defaultBlockState();
        }
    }
}
