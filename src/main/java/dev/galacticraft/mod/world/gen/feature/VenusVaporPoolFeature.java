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
 * Port of GC4 Legacy {@code WorldGenVaporPool} and GalaxySpace {@code WorldGenVaporPool}.
 * Generates an underground spherical chamber of {@link GCBlocks#SOFT_VENUS_ROCK} half-filled with
 * {@link GCBlocks#SULFURIC_ACID}, connected via a vertical shaft to a surface {@link GCBlocks#VAPOR_SPOUT}.
 */
public class VenusVaporPoolFeature extends Feature<NoneFeatureConfiguration> {
    public VenusVaporPoolFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
        if (surfaceY <= 48 || surfaceY >= 120) {
            return false;
        }

        int poolCenterY = 32 + rand.nextInt(Math.max(1, surfaceY - 42));
        BlockPos center = new BlockPos(origin.getX(), poolCenterY, origin.getZ());

        int radius = 5 + rand.nextInt(4);
        int rSq = radius * radius;
        int innerRSq = (radius - 1) * (radius - 1);

        BlockState softRock = GCBlocks.SOFT_VENUS_ROCK.defaultBlockState();
        BlockState acid = GCBlocks.SULFURIC_ACID.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dy = -radius; dy <= radius; ++dy) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq <= rSq) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (distSq >= innerRSq) {
                            level.setBlock(pos, softRock, 2);
                        } else if (dy <= 0) {
                            level.setBlock(pos, acid, 2);
                        } else {
                            level.setBlock(pos, air, 2);
                        }
                    }
                }
            }
        }

        BlockPos lastSolidPos = null;
        for (int y = poolCenterY + 1; y <= surfaceY + 4; ++y) {
            BlockPos shaftPos = new BlockPos(center.getX(), y, center.getZ());
            BlockState current = level.getBlockState(shaftPos);
            if (!current.isAir() && !current.liquid()) {
                lastSolidPos = shaftPos;
                level.setBlock(shaftPos, air, 2);
                // Reinforce shaft walls with soft venus rock so acid vapor chimney is enclosed
                for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos wallPos = shaftPos.relative(dir);
                    if (level.getBlockState(wallPos).isAir() && y < surfaceY - 1) {
                        level.setBlock(wallPos, softRock, 2);
                    }
                }
            }
        }

        if (lastSolidPos != null) {
            level.setBlock(lastSolidPos, GCBlocks.VAPOR_SPOUT.defaultBlockState(), 2);
            return true;
        }

        return false;
    }
}
