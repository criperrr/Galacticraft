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
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Reusable planetary meteor impact crater and fallen meteor generator.
 * Can be registered on any planet or biome via {@link PlanetaryMeteorConfig} to give players a configurable
 * probability of finding impact craters with hot {@link FallenMeteorBlock}, raw meteoric iron cores,
 * basalt/scorched ejecta, and rich scattered surface ores.
 */
public class PlanetaryMeteorFeature extends Feature<PlanetaryMeteorConfig> {

    public PlanetaryMeteorFeature(Codec<PlanetaryMeteorConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<PlanetaryMeteorConfig> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        PlanetaryMeteorConfig config = context.config();

        if (random.nextFloat() > config.spawnProbability()) {
            return false;
        }

        BlockPos origin = context.origin();
        int centerX = origin.getX();
        int centerZ = origin.getZ();
        int centerY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX, centerZ) - 1;
        if (centerY < 45 || centerY > 175) {
            return false;
        }

        BlockState groundState = level.getBlockState(new BlockPos(centerX, centerY, centerZ));
        if (groundState.isAir() || !groundState.getFluidState().isEmpty()) {
            return false;
        }

        // Ensure impact site is reasonably level (avoid sheer cliff walls)
        int minH = centerY;
        int maxH = centerY;
        int checkStep = 5;
        for (int dx = -checkStep; dx <= checkStep; dx += checkStep) {
            for (int dz = -checkStep; dz <= checkStep; dz += checkStep) {
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX + dx, centerZ + dz) - 1;
                minH = Math.min(minH, h);
                maxH = Math.max(maxH, h);
            }
        }
        if (maxH - minH > 8) {
            return false;
        }

        int craterRadius = Mth.randomBetweenInclusive(random, config.minCraterRadius(), config.maxCraterRadius());
        int craterDepth = Math.max(2, craterRadius / 2);
        int meteorRadius = Mth.randomBetweenInclusive(random, config.minMeteorRadius(), config.maxMeteorRadius());

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // 1. Excavate crater bowl & build scorched/basalt raised rim
        for (int dx = -craterRadius - 2; dx <= craterRadius + 2; dx++) {
            for (int dz = -craterRadius - 2; dz <= craterRadius + 2; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz) + (random.nextDouble() - 0.5) * 0.85;
                double norm = dist / (double) craterRadius;
                if (norm > 1.25) {
                    continue;
                }

                int colX = centerX + dx;
                int colZ = centerZ + dz;
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, colX, colZ) - 1;

                if (norm <= 0.85) {
                    // Parabolic crater excavation
                    double bowlFactor = 1.0 - (norm / 0.85) * (norm / 0.85);
                    int excavate = Math.max(1, (int) Math.round(craterDepth * bowlFactor));
                    int floorY = surfaceY - excavate;

                    for (int y = surfaceY + 2; y > floorY; y--) {
                        pos.set(colX, y, colZ);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }

                    // Scorched/magma floor lining
                    for (int d = 0; d <= 1; d++) {
                        pos.set(colX, floorY - d, colZ);
                        BlockState floorBlock;
                        if (d == 0 && norm < 0.45 && random.nextInt(3) == 0) {
                            floorBlock = config.craterFloorBlock().getState(random, pos);
                        } else if (random.nextInt(3) == 0) {
                            floorBlock = Blocks.BASALT.defaultBlockState();
                        } else {
                            floorBlock = config.scorchedRimBlock().getState(random, pos);
                        }
                        level.setBlock(pos, floorBlock, Block.UPDATE_CLIENTS);
                    }
                } else {
                    // Raised impact rim with basalt, scorched rock, and ore ejecta
                    double rimFactor = 1.0 - Math.abs(norm - 1.02) / 0.23;
                    int rimHeight = rimFactor > 0.35 ? 1 : 0;
                    for (int y = surfaceY; y <= surfaceY + rimHeight; y++) {
                        pos.set(colX, y, colZ);
                        BlockState rimBlock;
                        if (!config.ejectaOres().isEmpty() && random.nextInt(config.oreChance() * 2) == 0) {
                            rimBlock = config.ejectaOres().get(random.nextInt(config.ejectaOres().size()));
                        } else if (random.nextInt(3) == 0) {
                            rimBlock = Blocks.BASALT.defaultBlockState();
                        } else if (random.nextInt(6) == 0) {
                            rimBlock = Blocks.OBSIDIAN.defaultBlockState();
                        } else {
                            rimBlock = config.scorchedRimBlock().getState(random, pos);
                        }
                        level.setBlock(pos, rimBlock, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        // 2. Place Fallen Meteor & Meteoric Iron core in the center of the crater
        int meteorCenterY = centerY - craterDepth + Math.max(1, meteorRadius / 2);
        for (int dx = -meteorRadius; dx <= meteorRadius; dx++) {
            for (int dy = -meteorRadius; dy <= meteorRadius; dy++) {
                for (int dz = -meteorRadius; dz <= meteorRadius; dz++) {
                    double distSq = dx * dx + dy * dy + dz * dz;
                    double rSq = (meteorRadius + 0.35) * (meteorRadius + 0.35);
                    if (distSq <= rSq) {
                        pos.set(centerX + dx, meteorCenterY + dy, centerZ + dz);
                        BlockState state;
                        if (distSq <= (meteorRadius - 0.75) * (meteorRadius - 0.75) && meteorRadius >= 2) {
                            // Inner core of raw meteoric iron or rich ore
                            state = config.meteorCoreBlock().getState(random, pos);
                        } else {
                            // Hot fallen meteor shell
                            BlockState shell = config.meteorShellBlock().getState(random, pos);
                            if (shell.is(GCBlocks.FALLEN_METEOR)) {
                                shell = shell.setValue(FallenMeteorBlock.HEAT, Mth.randomBetweenInclusive(random, 4, 5));
                            }
                            state = shell;
                        }
                        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        // 3. Scatter TFCVolcanoes PyroclasticBomb-style ore & basalt ejecta clusters around the impact crater
        int bombCount = 4 + random.nextInt(5);
        for (int i = 0; i < bombCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = craterRadius * (0.75 + random.nextDouble() * 0.85);
            int bx = centerX + (int) Math.round(Math.cos(angle) * dist);
            int bz = centerZ + (int) Math.round(Math.sin(angle) * dist);
            int by = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bx, bz) - 1;
            if (by > 40 && by < 190) {
                placeEjectaCluster(level, new BlockPos(bx, by, bz), random, config);
            }
        }

        return true;
    }

    private void placeEjectaCluster(WorldGenLevel level, BlockPos pos, RandomSource random, PlanetaryMeteorConfig config) {
        level.setBlock(pos, sampleEjectaRock(random, pos, config), Block.UPDATE_CLIENTS);
        for (int i = 0; i < 6; i++) {
            BlockPos target = pos.offset(
                    random.nextInt(3) - random.nextInt(3),
                    random.nextInt(2) - random.nextInt(2),
                    random.nextInt(3) - random.nextInt(3)
            );
            int surfY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, target.getX(), target.getZ()) - 1;
            BlockPos grounded = new BlockPos(target.getX(), Math.min(target.getY(), surfY), target.getZ());
            if (!level.getBlockState(grounded.below()).isAir()) {
                level.setBlock(grounded, sampleEjectaRock(random, grounded, config), Block.UPDATE_CLIENTS);
            }
        }
    }

    private BlockState sampleEjectaRock(RandomSource random, BlockPos pos, PlanetaryMeteorConfig config) {
        if (!config.ejectaOres().isEmpty() && random.nextInt(config.oreChance()) == 0) {
            return config.ejectaOres().get(random.nextInt(config.ejectaOres().size()));
        }
        int roll = random.nextInt(10);
        if (roll < 4) {
            return Blocks.BASALT.defaultBlockState();
        } else if (roll < 6) {
            return config.scorchedRimBlock().getState(random, pos);
        } else if (roll < 8) {
            return config.craterFloorBlock().getState(random, pos);
        } else if (roll == 8) {
            return Blocks.OBSIDIAN.defaultBlockState();
        } else {
            return GCBlocks.FALLEN_METEOR.defaultBlockState().setValue(FallenMeteorBlock.HEAT, 4);
        }
    }
}
