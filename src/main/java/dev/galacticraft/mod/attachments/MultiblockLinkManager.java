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

package dev.galacticraft.mod.attachments;

import dev.galacticraft.api.multiblock.MultiVoxelMaster;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@SuppressWarnings("UnstableApiUsage")
public final class MultiblockLinkManager {
    private MultiblockLinkManager() {
    }

    private static @Nullable ChunkAccess getLoadedChunk(LevelReader level, int chunkX, int chunkZ) {
        return level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }

    public static @Nullable BlockPos getMasterPos(LevelReader level, BlockPos proxyPos) {
        if (level == null || proxyPos == null) {
            return null;
        }
        ChunkAccess chunk = getLoadedChunk(
                level,
                SectionPos.blockToSectionCoord(proxyPos.getX()),
                SectionPos.blockToSectionCoord(proxyPos.getZ())
        );
        if (chunk == null) {
            return null;
        }
        MultiblockLinksAttachment links = chunk.getAttached(GCAttachments.MULTIBLOCK_LINKS);
        return links == null || links.isEmpty() ? null : links.getMasterPos(proxyPos);
    }

    public static @Nullable MultiVoxelMaster getMaster(LevelReader level, BlockPos proxyPos) {
        BlockPos masterPos = getMasterPos(level, proxyPos);
        if (masterPos == null) {
            return null;
        }
        ChunkAccess masterChunk = getLoadedChunk(
                level,
                SectionPos.blockToSectionCoord(masterPos.getX()),
                SectionPos.blockToSectionCoord(masterPos.getZ())
        );
        if (masterChunk == null) {
            return null;
        }
        BlockEntity blockEntity = level instanceof Level actualLevel
                ? actualLevel.getBlockEntity(masterPos)
                : masterChunk.getBlockEntity(masterPos);
        return blockEntity instanceof MultiVoxelMaster master ? master : null;
    }

    public static void link(Level level, BlockPos proxyPos, BlockPos masterPos) {
        if (level == null || proxyPos == null || masterPos == null) {
            return;
        }
        ChunkAccess chunk = getLoadedChunk(
                level,
                SectionPos.blockToSectionCoord(proxyPos.getX()),
                SectionPos.blockToSectionCoord(proxyPos.getZ())
        );
        if (chunk == null) {
            return;
        }
        MultiblockLinksAttachment current = chunk.getAttached(GCAttachments.MULTIBLOCK_LINKS);
        MultiblockLinksAttachment base = current != null ? current : MultiblockLinksAttachment.EMPTY;
        MultiblockLinksAttachment updated = base.withLink(proxyPos.asLong(), masterPos.asLong());
        applyUpdate(chunk, base, updated);
    }

    public static void linkAll(Level level, Collection<BlockPos> proxyPositions, BlockPos masterPos) {
        if (level == null || proxyPositions == null || proxyPositions.isEmpty() || masterPos == null) {
            return;
        }
        Long2ObjectOpenHashMap<LongArrayList> byChunk = groupByChunk(proxyPositions);
        long packedMaster = masterPos.asLong();
        for (Long2ObjectMap.Entry<LongArrayList> entry : Long2ObjectMaps.fastIterable(byChunk)) {
            long packedChunk = entry.getLongKey();
            ChunkAccess chunk = getLoadedChunk(level, ChunkPos.getX(packedChunk), ChunkPos.getZ(packedChunk));
            if (chunk == null) {
                continue;
            }
            MultiblockLinksAttachment current = chunk.getAttached(GCAttachments.MULTIBLOCK_LINKS);
            MultiblockLinksAttachment base = current != null ? current : MultiblockLinksAttachment.EMPTY;
            MultiblockLinksAttachment updated = base.withLinks(entry.getValue(), packedMaster);
            applyUpdate(chunk, base, updated);
        }
    }

    public static void unlink(Level level, BlockPos proxyPos) {
        if (level == null || proxyPos == null) {
            return;
        }
        ChunkAccess chunk = getLoadedChunk(
                level,
                SectionPos.blockToSectionCoord(proxyPos.getX()),
                SectionPos.blockToSectionCoord(proxyPos.getZ())
        );
        if (chunk == null) {
            return;
        }
        MultiblockLinksAttachment current = chunk.getAttached(GCAttachments.MULTIBLOCK_LINKS);
        if (current == null || current.isEmpty()) {
            return;
        }
        MultiblockLinksAttachment updated = current.withoutLink(proxyPos.asLong());
        applyUpdate(chunk, current, updated);
    }

    public static void unlinkAll(Level level, Collection<BlockPos> proxyPositions) {
        if (level == null || proxyPositions == null || proxyPositions.isEmpty()) {
            return;
        }
        Long2ObjectOpenHashMap<LongArrayList> byChunk = groupByChunk(proxyPositions);
        for (Long2ObjectMap.Entry<LongArrayList> entry : Long2ObjectMaps.fastIterable(byChunk)) {
            long packedChunk = entry.getLongKey();
            ChunkAccess chunk = getLoadedChunk(level, ChunkPos.getX(packedChunk), ChunkPos.getZ(packedChunk));
            if (chunk == null) {
                continue;
            }
            MultiblockLinksAttachment current = chunk.getAttached(GCAttachments.MULTIBLOCK_LINKS);
            if (current == null || current.isEmpty()) {
                continue;
            }
            MultiblockLinksAttachment updated = current.withoutLinks(entry.getValue());
            applyUpdate(chunk, current, updated);
        }
    }

    private static Long2ObjectOpenHashMap<LongArrayList> groupByChunk(Collection<BlockPos> positions) {
        Long2ObjectOpenHashMap<LongArrayList> byChunk = new Long2ObjectOpenHashMap<>();
        for (BlockPos pos : positions) {
            if (pos == null) {
                continue;
            }
            long chunkKey = ChunkPos.asLong(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getZ())
            );
            LongArrayList list = byChunk.get(chunkKey);
            if (list == null) {
                list = new LongArrayList();
                byChunk.put(chunkKey, list);
            }
            list.add(pos.asLong());
        }
        return byChunk;
    }

    private static void applyUpdate(ChunkAccess chunk, MultiblockLinksAttachment previous, MultiblockLinksAttachment updated) {
        if (updated == previous) {
            return;
        }
        if (updated.isEmpty()) {
            chunk.removeAttached(GCAttachments.MULTIBLOCK_LINKS);
        } else {
            chunk.setAttached(GCAttachments.MULTIBLOCK_LINKS, updated);
        }
        chunk.setUnsaved(true);
    }
}
