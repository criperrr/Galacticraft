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

package dev.galacticraft.mod.machine;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.block.entity.machine.OxygenSealerBlockEntity;
import dev.galacticraft.mod.tag.GCBlockTags;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static dev.galacticraft.mod.content.block.entity.machine.OxygenSealerBlockEntity.SEAL_CHECK_TIME;

public class SealerManager {
    private static final Direction[] DIRECTIONS = Direction.values();

    private static class SpaceToSeal {

        private final List<OxygenSealerBlockEntity> sealers = new ArrayList<>();
        private final LongOpenHashSet blocksToSeal = new LongOpenHashSet();
        private final LongArrayFIFOQueue floodFillQueue = new LongArrayFIFOQueue();

        public SpaceToSeal(OxygenSealerBlockEntity sealer) {
            sealers.add(sealer);
            BlockPos pos = sealer.getBlockPos();
            floodFillQueue.enqueue(BlockPos.asLong(pos.getX(), pos.getY() + 1, pos.getZ()));
        }

        public boolean willSealSucceed() {
            return floodFillQueue.isEmpty();
        }

    }

    private final Level level;
    private final Map<BlockPos, OxygenSealerBlockEntity> sealers = new HashMap<>();
    private LongOpenHashSet sealedBlocks = new LongOpenHashSet();

    public SealerManager(Level level) {
        this.level = level;
    }

    private static final double MAX_SEALER_VOLUME = 1024;

    public void tick() {
        // Update sealing status periodically
        if (this.level.getGameTime() % SEAL_CHECK_TIME == 0) {
            updateSealedBlocks();
        }
    }

    public void updateSealedBlocks() {
        if (level.isClientSide) return;

        // If the level is breathable oxygen sealers don't change anything
        if (level.getDefaultBreathable()) return;

        if (level.getServer() != null && !level.getServer().isReady()) {
            Constant.LOGGER.info("World is not fully loaded, skipping sealing calculation");
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Set<SpaceToSeal> spacesToSeal = new HashSet<>();
        for (Map.Entry<BlockPos, OxygenSealerBlockEntity> entry : this.sealers.entrySet()) {
            OxygenSealerBlockEntity sealer = entry.getValue();
            if (!sealer.hasEnergy()) continue;
            if (!sealer.hasOxygen()) continue;
            if (sealer.isBlocked()) continue;

            // Flood fill to find all blocks this sealer is trying to seal
            SpaceToSeal spaceToSeal = new SpaceToSeal(sealer);
            while (!spaceToSeal.floodFillQueue.isEmpty()) {
                long packed = spaceToSeal.floodFillQueue.dequeueLong();
                if (spaceToSeal.blocksToSeal.contains(packed)) continue;
                int x = BlockPos.getX(packed);
                int y = BlockPos.getY(packed);
                int z = BlockPos.getZ(packed);
                cursor.set(x, y, z);
                // TODO: Better check to account for non-full blocks
                BlockState blockState = this.level.getBlockState(cursor);
                if (blockState.is(GCBlockTags.SEALABLE)) continue;
                if (blockState.isCollisionShapeFullBlock(this.level, cursor) && !blockState.is(GCBlockTags.UNSEALABLE)) continue;

                spaceToSeal.blocksToSeal.add(packed);
                for (Direction direction : DIRECTIONS) {
                    spaceToSeal.floodFillQueue.enqueue(BlockPos.asLong(
                            x + direction.getStepX(),
                            y + direction.getStepY(),
                            z + direction.getStepZ()
                    ));
                }

                boolean willAlreadySeal = false;
                for (SpaceToSeal otherSpace : spacesToSeal) {
                    if (spaceToSeal == otherSpace) continue;
                    if (!otherSpace.blocksToSeal.contains(packed)) continue;
                    // We have encountered a block that another sealer is trying to seal,
                    // so both sealers must be within the same space, so we combine them.
                    spaceToSeal.sealers.addAll(otherSpace.sealers);
                    spaceToSeal.blocksToSeal.addAll(otherSpace.blocksToSeal);
                    while (!otherSpace.floodFillQueue.isEmpty()) {
                        spaceToSeal.floodFillQueue.enqueue(otherSpace.floodFillQueue.dequeueLong());
                    }
                    willAlreadySeal = otherSpace.willSealSucceed();
                    spacesToSeal.remove(otherSpace);
                    break;
                }
                // If we just combined with a space that was already filled,
                // we must be in the same space, so we don't need to continue flood fill
                if (willAlreadySeal) break;

                // If the space has become too large to fill, stop performing flood fill
                if (spaceToSeal.blocksToSeal.size() > spaceToSeal.sealers.size() * MAX_SEALER_VOLUME) break;
            }
            spacesToSeal.add(spaceToSeal);
        }

        LongOpenHashSet newSealedBlocks = new LongOpenHashSet();

        for (SpaceToSeal spaceToSeal : spacesToSeal) {
            for (OxygenSealerBlockEntity sealer : spaceToSeal.sealers) sealer.setSealed(spaceToSeal.willSealSucceed());
            if (!spaceToSeal.willSealSucceed()) continue;
            LongIterator it = spaceToSeal.blocksToSeal.iterator();
            while (it.hasNext()) {
                long packed = it.nextLong();
                newSealedBlocks.add(packed);
                sealedBlocks.remove(packed);
                level.setBreathable(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed), true);
            }
        }

        // Reset all sealed blocks from the previous update to not be breathable
        LongIterator unsealIt = sealedBlocks.iterator();
        while (unsealIt.hasNext()) {
            long packed = unsealIt.nextLong();
            level.setBreathable(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed), false);
        }

        sealedBlocks = newSealedBlocks;
    }

    public boolean isBlockSealed(BlockPos pos) {
        return this.sealedBlocks.contains(pos.asLong());
    }

    public void addSealer(OxygenSealerBlockEntity sealer) {
        BlockPos pos = sealer.getBlockPos();
        Constant.LOGGER.info("Adding sealer at {} in dimension {}", pos, level.dimension().location());
        this.sealers.put(pos, sealer);
    }

    public void removeSealer(OxygenSealerBlockEntity sealer) {
        BlockPos pos = sealer.getBlockPos();
        Constant.LOGGER.info("Removing sealer at {} in dimension {}", pos, level.dimension().location());
        this.sealers.remove(pos);
    }

}
