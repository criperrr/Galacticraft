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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates monumental speleothems (mega-stalactites, mega-stalagmites, fused impact pillars,
 * and fallen shock-breccia boulders) inside Mercury's colossal subterranean caverns,
 * particularly in {@code Megabreccia Impact Caverns} and {@code Colossal Lava Tubes}.
 */
public class MercurySpeleothemFeature extends Feature<NoneFeatureConfiguration> {

    public MercurySpeleothemFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // 1. Locate open cavern air cavity around origin
        BlockPos.MutableBlockPos probe = origin.mutable();
        if (!level.getBlockState(probe).isAir()) {
            boolean foundAir = false;
            for (int dy = 1; dy <= 24; dy++) {
                probe.set(origin.getX(), origin.getY() + dy, origin.getZ());
                if (level.getBlockState(probe).isAir()) {
                    foundAir = true;
                    break;
                }
                probe.set(origin.getX(), origin.getY() - dy, origin.getZ());
                if (level.getBlockState(probe).isAir()) {
                    foundAir = true;
                    break;
                }
            }
            if (!foundAir) {
                return false;
            }
        }

        // 2. Scan up to locate solid cavern ceiling
        BlockPos ceilingPos = null;
        for (int y = probe.getY(); y < Math.min(level.getMaxBuildHeight() - 5, probe.getY() + 50); y++) {
            probe.setY(y);
            BlockState state = level.getBlockState(probe);
            if (!state.isAir()) {
                if (isValidCeiling(state)) {
                    ceilingPos = probe.immutable();
                }
                break;
            }
        }

        // 3. Scan down to locate solid cavern floor
        BlockPos floorPos = null;
        for (int y = probe.getY(); y > Math.max(level.getMinBuildHeight() + 5, probe.getY() - 50); y--) {
            probe.setY(y);
            BlockState state = level.getBlockState(probe);
            if (!state.isAir()) {
                if (isValidFloor(state)) {
                    floorPos = probe.immutable();
                }
                break;
            }
        }

        if (ceilingPos == null || floorPos == null) {
            return false;
        }

        int clearance = ceilingPos.getY() - floorPos.getY();
        if (clearance < 10 || clearance > 75) {
            return false;
        }

        int originX = origin.getX();
        int originZ = origin.getZ();

        // Roll speleothem formation type:
        // 0: Fused monolithic pillar (25%)
        // 1: Mega-stalactite hanging from ceiling (40%)
        // 2: Mega-stalagmite rising from floor (35%)
        int formationType = random.nextInt(100);

        if (formationType < 25) {
            // Fused Monolithic Pillar (Fluted column connecting floor to ceiling)
            buildFusedPillar(level, random, originX, originZ, floorPos.getY(), ceilingPos.getY(), clearance);
        } else if (formationType < 65) {
            // Mega-Stalactite hanging from ceiling
            int stalactiteLength = Mth.clamp((int) (clearance * (0.45 + random.nextDouble() * 0.35)), 8, 28);
            double baseRadius = 2.8 + random.nextDouble() * 2.2; // base diameter 5.6 to 10 blocks
            buildStalactite(level, random, originX, originZ, ceilingPos.getY(), stalactiteLength, baseRadius);

            // 50% chance for a smaller matching stalagmite below
            if (random.nextBoolean()) {
                int stalagmiteHeight = Mth.clamp((int) (clearance * (0.25 + random.nextDouble() * 0.20)), 5, 15);
                double stBaseRadius = 2.0 + random.nextDouble() * 1.5;
                buildStalagmite(level, random, originX, originZ, floorPos.getY(), stalagmiteHeight, stBaseRadius);
            }
        } else {
            // Mega-Stalagmite rising from floor
            int stalagmiteHeight = Mth.clamp((int) (clearance * (0.45 + random.nextDouble() * 0.35)), 7, 26);
            double baseRadius = 3.0 + random.nextDouble() * 2.5; // base diameter 6 to 11 blocks
            buildStalagmite(level, random, originX, originZ, floorPos.getY(), stalagmiteHeight, baseRadius);

            // Scatter fallen impact megablocks / boulders around floor base
            scatterFallenMegablocks(level, random, originX, floorPos.getY(), originZ);
        }

        return true;
    }

    /**
     * Builds a continuous monolithic rock pillar connecting the floor to the ceiling.
     * Fluted hourglass shape: wide capital at ceiling, wide base at floor, slightly narrower waist.
     */
    private void buildFusedPillar(
            WorldGenLevel level,
            RandomSource random,
            int centerX,
            int centerZ,
            int floorY,
            int ceilingY,
            int clearance
    ) {
        double floorRadius = 3.2 + random.nextDouble() * 2.2;
        double ceilingRadius = 3.0 + random.nextDouble() * 2.0;
        double waistRadius = Math.min(floorRadius, ceilingRadius) * 0.65;
        double waistY = floorY + clearance * (0.40 + random.nextDouble() * 0.20);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = floorY; y <= ceilingY; y++) {
            double radius;
            if (y < waistY) {
                double t = (double) (y - floorY) / Math.max(1.0, waistY - floorY);
                radius = Mth.lerp(t, floorRadius, waistRadius);
            } else {
                double t = (double) (y - waistY) / Math.max(1.0, ceilingY - waistY);
                radius = Mth.lerp(t, waistRadius, ceilingRadius);
            }

            int rCeil = Mth.ceil(radius + 0.6);
            for (int dx = -rCeil; dx <= rCeil; dx++) {
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    double threshold = radius + (random.nextDouble() - 0.5) * 0.45;
                    if (dist > threshold) {
                        continue;
                    }

                    cursor.set(centerX + dx, y, centerZ + dz);
                    if (canReplaceForSpeleothem(level.getBlockState(cursor))) {
                        BlockState block = pickBrecciaOreBlock(random, dist / Math.max(0.5, radius));
                        level.setBlock(cursor, block, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /**
     * Builds a tapered mega-stalactite hanging downwards from the cavern ceiling.
     */
    private void buildStalactite(
            WorldGenLevel level,
            RandomSource random,
            int centerX,
            int centerZ,
            int ceilingY,
            int length,
            double baseRadius
    ) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int i = 0; i <= length; i++) {
            int y = ceilingY - i;
            double progress = (double) i / (double) length;
            // Taper with slight parabolic curvature towards the tip
            double currentRadius = Math.max(0.6, baseRadius * (1.0 - Math.pow(progress, 0.85)));
            int rCeil = Mth.ceil(currentRadius + 0.5);

            for (int dx = -rCeil; dx <= rCeil; dx++) {
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    double threshold = currentRadius + (random.nextDouble() - 0.5) * 0.4;
                    if (dist > threshold) {
                        continue;
                    }

                    cursor.set(centerX + dx, y, centerZ + dz);
                    if (canReplaceForSpeleothem(level.getBlockState(cursor))) {
                        BlockState block = pickBrecciaOreBlock(random, dist / Math.max(0.5, currentRadius));
                        level.setBlock(cursor, block, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /**
     * Builds a tapered mega-stalagmite rising upwards from the cavern floor.
     */
    private void buildStalagmite(
            WorldGenLevel level,
            RandomSource random,
            int centerX,
            int centerZ,
            int floorY,
            int height,
            double baseRadius
    ) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int i = 0; i <= height; i++) {
            int y = floorY + i;
            double progress = (double) i / (double) height;
            // Upward conical taper
            double currentRadius = Math.max(0.7, baseRadius * (1.0 - progress * 0.82));
            int rCeil = Mth.ceil(currentRadius + 0.5);

            for (int dx = -rCeil; dx <= rCeil; dx++) {
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    double threshold = currentRadius + (random.nextDouble() - 0.5) * 0.45;
                    if (dist > threshold) {
                        continue;
                    }

                    cursor.set(centerX + dx, y, centerZ + dz);
                    if (canReplaceForSpeleothem(level.getBlockState(cursor))) {
                        BlockState block = pickBrecciaOreBlock(random, dist / Math.max(0.5, currentRadius));
                        level.setBlock(cursor, block, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /**
     * Scatters 2 to 4 fallen megablocks (fallen breccia boulders 2x2 to 3x3) around the floor base.
     */
    private void scatterFallenMegablocks(WorldGenLevel level, RandomSource random, int centerX, int floorY, int centerZ) {
        int boulderCount = 2 + random.nextInt(3);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int b = 0; b < boulderCount; b++) {
            int bx = centerX + (random.nextInt(15) - 7);
            int bz = centerZ + (random.nextInt(15) - 7);
            int sizeX = 2 + random.nextInt(2);
            int sizeY = 2 + random.nextInt(2);
            int sizeZ = 2 + random.nextInt(2);

            for (int ox = 0; ox < sizeX; ox++) {
                for (int oy = 0; oy < sizeY; oy++) {
                    for (int oz = 0; oz < sizeZ; oz++) {
                        cursor.set(bx + ox, floorY + oy, bz + oz);
                        if (level.getBlockState(cursor).isAir()) {
                            BlockState state = random.nextInt(4) == 0
                                    ? GCBlocks.MERCURY_GRAPHITE_ROCK.defaultBlockState()
                                    : GCBlocks.MERCURY_SHOCK_BRECCIA.defaultBlockState();
                            level.setBlock(cursor, state, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
    }

    private static BlockState pickBrecciaOreBlock(RandomSource random, double normalizedDist) {
        // Core vs outer mantle composition
        int roll = random.nextInt(100);
        if (roll < 60) {
            return GCBlocks.MERCURY_SHOCK_BRECCIA.defaultBlockState();
        } else if (roll < 80) {
            return GCBlocks.MERCURY_GRAPHITE_ROCK.defaultBlockState();
        } else if (roll < 90) {
            return GCBlocks.MERCURY_TITANIUM_ORE.defaultBlockState();
        } else if (roll < 96) {
            return GCBlocks.MERCURY_NATIVE_IRON_ORE.defaultBlockState();
        } else {
            return GCBlocks.MERCURY_BASALT.defaultBlockState();
        }
    }

    private static boolean canReplaceForSpeleothem(BlockState state) {
        return state.isAir()
                || state.is(GCBlocks.MERCURY_REGOLITH)
                || state.is(GCBlocks.MERCURY_COMPACTED_REGOLITH)
                || state.is(GCBlocks.MERCURY_BASALT)
                || state.is(GCBlocks.MERCURY_SHOCK_BRECCIA)
                || state.is(GCBlocks.MERCURY_GRAPHITE_ROCK);
    }

    private static boolean isValidCeiling(BlockState state) {
        return !state.isAir()
                && !state.is(Blocks.BEDROCK)
                && (state.is(GCBlocks.MERCURY_BASALT)
                || state.is(GCBlocks.MERCURY_SHOCK_BRECCIA)
                || state.is(GCBlocks.MERCURY_GRAPHITE_ROCK)
                || state.is(GCBlocks.MERCURY_COMPACTED_REGOLITH));
    }

    private static boolean isValidFloor(BlockState state) {
        return !state.isAir()
                && !state.is(Blocks.BEDROCK)
                && (state.is(GCBlocks.MERCURY_BASALT)
                || state.is(GCBlocks.MERCURY_SHOCK_BRECCIA)
                || state.is(GCBlocks.MERCURY_GRAPHITE_ROCK)
                || state.is(GCBlocks.MERCURY_COMPACTED_REGOLITH)
                || state.is(GCBlocks.MERCURY_REGOLITH));
    }
}
