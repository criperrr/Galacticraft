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
import dev.galacticraft.mod.content.block.environment.CavernousVines;
import dev.galacticraft.mod.tag.GCBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Replicates GC4 {@code MapGenCavernMars} subterranean ceiling vine generation:
 * hangs {@link GCBlocks#CAVERNOUS_VINES_PLANT} and {@link GCBlocks#CAVERNOUS_VINES}
 * from Martian cavern ceilings at {@code Y <= 65}.
 */
public class MarsCavernVinesFeature extends Feature<NoneFeatureConfiguration> {
    public MarsCavernVinesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        if (origin.getY() > 65 || origin.getY() < level.getMinBuildHeight() + 6) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = origin.mutable();
        BlockPos startPos = null;
        for (int dy = 0; dy <= 12; dy++) {
            int checkY = origin.getY() + dy;
            if (checkY > 65) {
                break;
            }
            cursor.set(origin.getX(), checkY, origin.getZ());
            if (level.isEmptyBlock(cursor)) {
                BlockPos abovePos = cursor.above();
                BlockState ceilingState = level.getBlockState(abovePos);
                if (ceilingState.is(GCBlockTags.MARS_CARVER_REPLACEABLES)
                        && ceilingState.isFaceSturdy(level, abovePos, Direction.DOWN)) {
                    startPos = cursor.immutable();
                    break;
                }
            }
        }

        if (startPos == null) {
            return false;
        }

        int maxLength = 2 + random.nextInt(9);
        int actualLength = 0;
        for (int i = 0; i < maxLength; i++) {
            BlockPos checkPos = startPos.below(i);
            // GC4 MapGenCavernMars stops growing when the block below is non-air so vines hang above the floor
            if (!level.isEmptyBlock(checkPos) || !level.isEmptyBlock(checkPos.below())) {
                break;
            }
            actualLength++;
        }

        if (actualLength <= 0) {
            return false;
        }

        for (int i = 0; i < actualLength; i++) {
            BlockPos placePos = startPos.below(i);
            boolean poisonous = random.nextFloat() < CavernousVines.CHANCE_OF_POISONOUS_VINES;
            if (i == actualLength - 1) {
                BlockState headState = GCBlocks.CAVERNOUS_VINES.defaultBlockState()
                        .setValue(CavernousVines.POISONOUS, poisonous)
                        .setValue(GrowingPlantHeadBlock.AGE, 16 + random.nextInt(10));
                level.setBlock(placePos, headState, Block.UPDATE_CLIENTS);
            } else {
                BlockState bodyState = GCBlocks.CAVERNOUS_VINES_PLANT.defaultBlockState()
                        .setValue(CavernousVines.POISONOUS, poisonous);
                level.setBlock(placePos, bodyState, Block.UPDATE_CLIENTS);
            }
        }

        return true;
    }
}
