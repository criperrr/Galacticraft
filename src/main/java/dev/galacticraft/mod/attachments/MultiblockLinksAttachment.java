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

import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.LongStream;

public final class MultiblockLinksAttachment {
    private static final long MISSING = Long.MIN_VALUE;

    public static final MultiblockLinksAttachment EMPTY = new MultiblockLinksAttachment(createEmptyMap());

    public static final Codec<MultiblockLinksAttachment> CODEC = Codec.LONG_STREAM.xmap(
            MultiblockLinksAttachment::fromLongStream,
            MultiblockLinksAttachment::toLongStream
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockLinksAttachment> STREAM_CODEC = StreamCodec.of(
            MultiblockLinksAttachment::encode,
            MultiblockLinksAttachment::decode
    );

    private final Long2LongMap links;

    private MultiblockLinksAttachment(Long2LongOpenHashMap links) {
        this.links = links;
    }

    private static Long2LongOpenHashMap createEmptyMap() {
        Long2LongOpenHashMap map = new Long2LongOpenHashMap(0);
        map.defaultReturnValue(MISSING);
        return map;
    }

    private static MultiblockLinksAttachment fromLongStream(LongStream stream) {
        long[] values = stream.toArray();
        int pairCount = values.length / 2;
        if (pairCount == 0) {
            return EMPTY;
        }
        Long2LongOpenHashMap map = new Long2LongOpenHashMap(pairCount);
        map.defaultReturnValue(MISSING);
        for (int i = 0; i < pairCount; i++) {
            map.put(values[i * 2], values[i * 2 + 1]);
        }
        return map.isEmpty() ? EMPTY : new MultiblockLinksAttachment(map);
    }

    private LongStream toLongStream() {
        if (this.links.isEmpty()) {
            return LongStream.empty();
        }
        long[] values = new long[this.links.size() * 2];
        int index = 0;
        for (Long2LongMap.Entry entry : Long2LongMaps.fastIterable(this.links)) {
            values[index++] = entry.getLongKey();
            values[index++] = entry.getLongValue();
        }
        return Arrays.stream(values);
    }

    private static final int MAX_COLUMNS_PER_CHUNK = 256;
    private static final int MAX_Y_SPAN = 4096;

    private static int encodeZigZag(int value) {
        return (value << 1) ^ (value >> 31);
    }

    private static int decodeZigZag(int value) {
        return (value >>> 1) ^ -(value & 1);
    }

    /**
     * Compact wire encoding: groups links by {@code masterPos -> chunkPos -> (localX, localZ) column}
     * and run-length-encodes consecutive Y coordinates so large multiblocks (such as the 11x11 platform
     * with 3,000+ collision voxels) stay well below Fabric's 32,502-byte synced attachment packet limit.
     */
    private static void encode(RegistryFriendlyByteBuf buf, MultiblockLinksAttachment value) {
        if (value.links.isEmpty()) {
            buf.writeVarInt(0);
            return;
        }

        // masterPos -> (chunkPos -> (colByte -> Y list))
        Long2ObjectOpenHashMap<Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<IntArrayList>>> grouped = new Long2ObjectOpenHashMap<>();
        for (Long2LongMap.Entry entry : Long2LongMaps.fastIterable(value.links)) {
            long packedProxy = entry.getLongKey();
            long packedMaster = entry.getLongValue();

            int x = BlockPos.getX(packedProxy);
            int y = BlockPos.getY(packedProxy);
            int z = BlockPos.getZ(packedProxy);
            long chunkKey = ChunkPos.asLong(x >> 4, z >> 4);
            int col = ((x & 15) << 4) | (z & 15);

            Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<IntArrayList>> byChunk = grouped.get(packedMaster);
            if (byChunk == null) {
                byChunk = new Long2ObjectOpenHashMap<>(2);
                grouped.put(packedMaster, byChunk);
            }
            Int2ObjectOpenHashMap<IntArrayList> byCol = byChunk.get(chunkKey);
            if (byCol == null) {
                byCol = new Int2ObjectOpenHashMap<>();
                byChunk.put(chunkKey, byCol);
            }
            IntArrayList ys = byCol.get(col);
            if (ys == null) {
                ys = new IntArrayList(4);
                byCol.put(col, ys);
            }
            ys.add(y);
        }

        buf.writeVarInt(grouped.size());
        for (Long2ObjectMap.Entry<Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<IntArrayList>>> masterEntry : grouped.long2ObjectEntrySet()) {
            buf.writeLong(masterEntry.getLongKey());
            Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<IntArrayList>> byChunk = masterEntry.getValue();
            buf.writeVarInt(byChunk.size());

            for (Long2ObjectMap.Entry<Int2ObjectOpenHashMap<IntArrayList>> chunkEntry : byChunk.long2ObjectEntrySet()) {
                long chunkKey = chunkEntry.getLongKey();
                buf.writeVarInt(encodeZigZag(ChunkPos.getX(chunkKey)));
                buf.writeVarInt(encodeZigZag(ChunkPos.getZ(chunkKey)));

                Int2ObjectOpenHashMap<IntArrayList> byCol = chunkEntry.getValue();
                buf.writeVarInt(byCol.size());

                for (Int2ObjectMap.Entry<IntArrayList> colEntry : byCol.int2ObjectEntrySet()) {
                    buf.writeByte(colEntry.getIntKey());
                    IntArrayList ys = colEntry.getValue();
                    ys.sort(null);

                    // Count consecutive Y runs
                    int size = ys.size();
                    int runCount = 1;
                    for (int i = 1; i < size; i++) {
                        if (ys.getInt(i) != ys.getInt(i - 1) + 1) {
                            runCount++;
                        }
                    }

                    buf.writeVarInt(runCount);
                    int runStart = ys.getInt(0);
                    int runLen = 1;
                    int prevRunEnd = 0;
                    boolean firstRun = true;

                    for (int i = 1; i <= size; i++) {
                        if (i < size && ys.getInt(i) == runStart + runLen) {
                            runLen++;
                        } else {
                            if (firstRun) {
                                buf.writeVarInt(encodeZigZag(runStart));
                                firstRun = false;
                            } else {
                                buf.writeVarInt(runStart - prevRunEnd);
                            }
                            buf.writeVarInt(runLen);
                            prevRunEnd = runStart + runLen;
                            if (i < size) {
                                runStart = ys.getInt(i);
                                runLen = 1;
                            }
                        }
                    }
                }
            }
        }
    }

    private static MultiblockLinksAttachment decode(RegistryFriendlyByteBuf buf) {
        int masterCount = buf.readVarInt();
        if (masterCount == 0) {
            return EMPTY;
        }
        int maxPossibleMasters = buf.readableBytes() / (Long.BYTES + 1);
        if (masterCount < 0 || masterCount > maxPossibleMasters) {
            throw new DecoderException("Invalid MultiblockLinksAttachment master count " + masterCount + " (readable bytes: " + buf.readableBytes() + ")");
        }

        Long2LongOpenHashMap map = new Long2LongOpenHashMap();
        map.defaultReturnValue(MISSING);

        for (int m = 0; m < masterCount; m++) {
            if (buf.readableBytes() < Long.BYTES + 1) {
                throw new DecoderException("Truncated MultiblockLinksAttachment master entry");
            }
            long packedMaster = buf.readLong();
            int chunkCount = buf.readVarInt();
            if (chunkCount <= 0 || chunkCount > buf.readableBytes() / 3) {
                throw new DecoderException("Invalid MultiblockLinksAttachment chunk count " + chunkCount);
            }

            for (int c = 0; c < chunkCount; c++) {
                int chunkX = decodeZigZag(buf.readVarInt());
                int chunkZ = decodeZigZag(buf.readVarInt());
                int baseX = chunkX << 4;
                int baseZ = chunkZ << 4;
                int columnCount = buf.readVarInt();
                if (columnCount <= 0 || columnCount > MAX_COLUMNS_PER_CHUNK || columnCount > buf.readableBytes() / 2) {
                    throw new DecoderException("Invalid MultiblockLinksAttachment column count " + columnCount);
                }

                for (int colIdx = 0; colIdx < columnCount; colIdx++) {
                    if (buf.readableBytes() < 2) {
                        throw new DecoderException("Truncated MultiblockLinksAttachment column entry");
                    }
                    int col = buf.readUnsignedByte();
                    int worldX = baseX | ((col >>> 4) & 15);
                    int worldZ = baseZ | (col & 15);

                    int runCount = buf.readVarInt();
                    if (runCount <= 0 || runCount > buf.readableBytes() / 2) {
                        throw new DecoderException("Invalid MultiblockLinksAttachment run count " + runCount);
                    }

                    int prevRunEnd = 0;
                    for (int r = 0; r < runCount; r++) {
                        int runStart;
                        if (r == 0) {
                            runStart = decodeZigZag(buf.readVarInt());
                        } else {
                            int gap = buf.readVarInt();
                            if (gap <= 0) {
                                throw new DecoderException("Invalid MultiblockLinksAttachment Y gap " + gap);
                            }
                            runStart = prevRunEnd + gap;
                        }
                        int runLen = buf.readVarInt();
                        if (runLen <= 0 || runLen > MAX_Y_SPAN) {
                            throw new DecoderException("Invalid MultiblockLinksAttachment Y run length " + runLen);
                        }
                        for (int dy = 0; dy < runLen; dy++) {
                            map.put(BlockPos.asLong(worldX, runStart + dy, worldZ), packedMaster);
                        }
                        prevRunEnd = runStart + runLen;
                    }
                }
            }
        }
        return map.isEmpty() ? EMPTY : new MultiblockLinksAttachment(map);
    }

    public @Nullable BlockPos getMasterPos(BlockPos proxyPos) {
        if (proxyPos == null || this.links.isEmpty()) {
            return null;
        }
        long packedMaster = this.links.get(proxyPos.asLong());
        return packedMaster == MISSING ? null : BlockPos.of(packedMaster);
    }

    public boolean containsProxy(long packedProxy) {
        return this.links.containsKey(packedProxy);
    }

    public int size() {
        return this.links.size();
    }

    public boolean isEmpty() {
        return this.links.isEmpty();
    }

    public MultiblockLinksAttachment withLink(long proxy, long master) {
        if (this.links.containsKey(proxy) && this.links.get(proxy) == master) {
            return this;
        }
        Long2LongOpenHashMap copy = new Long2LongOpenHashMap(this.links);
        copy.defaultReturnValue(MISSING);
        copy.put(proxy, master);
        return new MultiblockLinksAttachment(copy);
    }

    public MultiblockLinksAttachment withLinks(LongList proxies, long master) {
        if (proxies == null || proxies.isEmpty()) {
            return this;
        }
        boolean changed = false;
        for (int i = 0, n = proxies.size(); i < n; i++) {
            long proxy = proxies.getLong(i);
            if (!this.links.containsKey(proxy) || this.links.get(proxy) != master) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return this;
        }
        Long2LongOpenHashMap copy = new Long2LongOpenHashMap(this.links.size() + proxies.size());
        copy.defaultReturnValue(MISSING);
        copy.putAll(this.links);
        for (int i = 0, n = proxies.size(); i < n; i++) {
            copy.put(proxies.getLong(i), master);
        }
        return new MultiblockLinksAttachment(copy);
    }

    public MultiblockLinksAttachment withoutLink(long proxy) {
        if (!this.links.containsKey(proxy)) {
            return this;
        }
        if (this.links.size() == 1) {
            return EMPTY;
        }
        Long2LongOpenHashMap copy = new Long2LongOpenHashMap(this.links);
        copy.defaultReturnValue(MISSING);
        copy.remove(proxy);
        return copy.isEmpty() ? EMPTY : new MultiblockLinksAttachment(copy);
    }

    public MultiblockLinksAttachment withoutLinks(LongList proxies) {
        if (this.links.isEmpty() || proxies == null || proxies.isEmpty()) {
            return this;
        }
        boolean anyPresent = false;
        for (int i = 0, n = proxies.size(); i < n; i++) {
            if (this.links.containsKey(proxies.getLong(i))) {
                anyPresent = true;
                break;
            }
        }
        if (!anyPresent) {
            return this;
        }
        Long2LongOpenHashMap copy = new Long2LongOpenHashMap(this.links);
        copy.defaultReturnValue(MISSING);
        for (int i = 0, n = proxies.size(); i < n; i++) {
            copy.remove(proxies.getLong(i));
        }
        return copy.isEmpty() ? EMPTY : new MultiblockLinksAttachment(copy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MultiblockLinksAttachment other)) {
            return false;
        }
        return this.links.equals(other.links);
    }

    @Override
    public int hashCode() {
        return this.links.hashCode();
    }

    @Override
    public String toString() {
        return "MultiblockLinksAttachment[size=" + this.links.size() + "]";
    }
}
