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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Generates weathered Martian surface boulder outcrops and impact ejecta rocks (Mars Breccia,
 * Mars Basalt, and Mars Siltstone) embedded into flat plains, impact basins, and canyon beds.
 */
public class MarsSurfaceOutcropFeature extends Feature<NoneFeatureConfiguration> {

    public MarsSurfaceOutcropFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        BlockPos below = origin.below();
        if (!level.getBlockState(below).is(GCBlockTags.MARS_CARVER_REPLACEABLES)) {
            return false;
        }

        BlockState mainRock = switch (random.nextInt(3)) {
            case 0 -> GCBlocks.MARS_BRECCIA.defaultBlockState();
            case 1 -> GCBlocks.MARS_BASALT.defaultBlockState();
            default -> GCBlocks.MARS_SILTSTONE.defaultBlockState();
        };
        BlockState cobble = GCBlocks.MARS_COBBLESTONE.defaultBlockState();

        int radiusX = 1 + random.nextInt(2);
        int radiusZ = 1 + random.nextInt(2);
        int height = 1 + random.nextInt(2);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                double norm = (double) (dx * dx) / (radiusX * radiusX + 0.4) + (double) (dz * dz) / (radiusZ * radiusZ + 0.4);
                if (norm > 1.0 + random.nextDouble() * 0.25) {
                    continue;
                }
                int colHeight = norm < 0.45 ? height : 1;
                for (int dy = -1; dy < colHeight; dy++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (dy < 0 || level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, random.nextInt(4) == 0 ? cobble : mainRock, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return true;
    }
}
