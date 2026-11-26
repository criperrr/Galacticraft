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
import dev.galacticraft.mod.tag.GCBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates Martian basaltic/sedimentary speleothems (stalactites, stalagmites, and fused cavern
 * columns) anchored to solid ceilings and floors inside Mars lava tubes and deep terraced caverns.
 */
public class MarsSpeleothemFeature extends Feature<NoneFeatureConfiguration> {

    public MarsSpeleothemFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        BlockPos.MutableBlockPos probe = origin.mutable();
        if (!level.getBlockState(probe).isAir()) {
            boolean foundAir = false;
            for (int dy = 1; dy <= 16; dy++) {
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

        BlockPos ceilingPos = null;
        for (int y = probe.getY(); y < Math.min(level.getMaxBuildHeight() - 5, probe.getY() + 36); y++) {
            probe.setY(y);
            BlockState state = level.getBlockState(probe);
            if (!state.isAir()) {
                if (state.is(GCBlockTags.MARS_CARVER_REPLACEABLES)) {
                    ceilingPos = probe.immutable();
                }
                break;
            }
        }

        BlockPos floorPos = null;
        for (int y = probe.getY(); y > Math.max(level.getMinBuildHeight() + 5, probe.getY() - 36); y--) {
            probe.setY(y);
            BlockState state = level.getBlockState(probe);
            if (!state.isAir()) {
                if (state.is(GCBlockTags.MARS_CARVER_REPLACEABLES)) {
                    floorPos = probe.immutable();
                }
                break;
            }
        }

        if (ceilingPos == null || floorPos == null) {
            return false;
        }

        int clearance = ceilingPos.getY() - floorPos.getY();
        if (clearance < 6 || clearance > 48) {
            return false;
        }

        BlockState primary = floorPos.getY() < 0
                ? GCBlocks.MARS_DEEP_STONE.defaultBlockState()
                : (random.nextBoolean() ? GCBlocks.MARS_BASALT.defaultBlockState() : GCBlocks.MARS_SILTSTONE.defaultBlockState());
        BlockState accent = GCBlocks.MARS_BRECCIA.defaultBlockState();

        int roll = random.nextInt(10);
        if (roll < 3 && clearance <= 28) {
            // Fused hourglass column connecting floor to ceiling
            placeColumn(level, random, floorPos.getX(), floorPos.getZ(), floorPos.getY(), ceilingPos.getY(), primary, accent);
        } else if (roll < 7) {
            // Hanging stalactite + matching floor stalagmite
            int stalactiteLen = Math.min(clearance / 2, 3 + random.nextInt(7));
            int stalagmiteLen = Math.min(clearance / 2, 2 + random.nextInt(6));
            placeTaperedCone(level, random, ceilingPos.getX(), ceilingPos.getZ(), ceilingPos.getY() - 1, -1, stalactiteLen, primary, accent);
            placeTaperedCone(level, random, floorPos.getX(), floorPos.getZ(), floorPos.getY() + 1, 1, stalagmiteLen, primary, accent);
        } else {
            // Single hanging stalactite
            int stalactiteLen = Math.min(clearance - 2, 4 + random.nextInt(9));
            placeTaperedCone(level, random, ceilingPos.getX(), ceilingPos.getZ(), ceilingPos.getY() - 1, -1, stalactiteLen, primary, accent);
        }
        return true;
    }

    private static void placeColumn(WorldGenLevel level, RandomSource random, int cx, int cz, int floorY, int ceilingY, BlockState primary, BlockState accent) {
        int height = ceilingY - floorY;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= height; dy++) {
            float t = (float) dy / Math.max(1, height);
            float pinch = Math.abs(t - 0.5F) * 2.0F;
            float radius = 0.85F + pinch * pinch * 1.45F;
            int rInt = Mth.ceil(radius);
            for (int dx = -rInt; dx <= rInt; dx++) {
                for (int dz = -rInt; dz <= rInt; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        pos.set(cx + dx, floorY + dy, cz + dz);
                        if (level.getBlockState(pos).isAir() || dy == 0 || dy == height) {
                            level.setBlock(pos, random.nextInt(5) == 0 ? accent : primary, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
    }

    private static void placeTaperedCone(WorldGenLevel level, RandomSource random, int cx, int cz, int startY, int stepY, int length, BlockState primary, BlockState accent) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < length; i++) {
            float t = 1.0F - ((float) i / Math.max(1, length));
            float radius = t * 1.55F;
            int rInt = Mth.floor(radius);
            int y = startY + i * stepY;
            for (int dx = -rInt; dx <= rInt; dx++) {
                for (int dz = -rInt; dz <= rInt; dz++) {
                    if (dx * dx + dz * dz <= radius * radius + 0.15F) {
                        pos.set(cx + dx, y, cz + dz);
                        if (level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, random.nextInt(6) == 0 ? accent : primary, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
    }
}
