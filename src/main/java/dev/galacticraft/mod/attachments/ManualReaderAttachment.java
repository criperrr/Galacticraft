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
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent per-player reader state for the Aerospace Manual, stored via Fabric Data Attachment API
 * ({@code GCAttachments.MANUAL_READER}) with {@code persistent}, {@code copyOnDeath}, and {@code syncWith}
 * — zero Player mixins required.
 */
public record ManualReaderAttachment(
        @NotNull Set<ResourceLocation> readEntries,
        @NotNull Set<ResourceLocation> favoriteEntries,
        @NotNull Optional<PinnedMission> pinnedMission
) {
    public static final ManualReaderAttachment EMPTY = new ManualReaderAttachment(Set.of(), Set.of(), Optional.empty());

    public record PinnedMission(
            @NotNull ResourceLocation entryId,
            @NotNull String title,
            @NotNull List<String> lines,
            @NotNull List<ResourceLocation> trackedItems,
            boolean collapsed
    ) {
        public static final Codec<PinnedMission> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("entry_id").forGetter(PinnedMission::entryId),
                Codec.STRING.fieldOf("title").forGetter(PinnedMission::title),
                Codec.STRING.listOf().optionalFieldOf("lines", List.of()).forGetter(PinnedMission::lines),
                ResourceLocation.CODEC.listOf().optionalFieldOf("tracked_items", List.of()).forGetter(PinnedMission::trackedItems),
                Codec.BOOL.optionalFieldOf("collapsed", false).forGetter(PinnedMission::collapsed)
        ).apply(instance, PinnedMission::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, PinnedMission> STREAM_CODEC = StreamCodec.of(
                (buf, val) -> {
                    ResourceLocation.STREAM_CODEC.encode(buf, val.entryId);
                    ByteBufCodecs.STRING_UTF8.encode(buf, val.title);
                    ByteBufCodecs.VAR_INT.encode(buf, val.lines.size());
                    for (String line : val.lines) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, line);
                    }
                    ByteBufCodecs.VAR_INT.encode(buf, val.trackedItems.size());
                    for (ResourceLocation item : val.trackedItems) {
                        ResourceLocation.STREAM_CODEC.encode(buf, item);
                    }
                    ByteBufCodecs.BOOL.encode(buf, val.collapsed);
                },
                buf -> {
                    ResourceLocation entryId = ResourceLocation.STREAM_CODEC.decode(buf);
                    String title = ByteBufCodecs.STRING_UTF8.decode(buf);
                    int lineCount = Math.min(32, Math.max(0, ByteBufCodecs.VAR_INT.decode(buf)));
                    List<String> lines = new ArrayList<>(lineCount);
                    for (int i = 0; i < lineCount; i++) {
                        lines.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                    }
                    int itemCount = Math.min(32, Math.max(0, ByteBufCodecs.VAR_INT.decode(buf)));
                    List<ResourceLocation> items = new ArrayList<>(itemCount);
                    for (int i = 0; i < itemCount; i++) {
                        items.add(ResourceLocation.STREAM_CODEC.decode(buf));
                    }
                    boolean collapsed = ByteBufCodecs.BOOL.decode(buf);
                    return new PinnedMission(entryId, title, List.copyOf(lines), List.copyOf(items), collapsed);
                }
        );

        public PinnedMission withCollapsed(boolean nextCollapsed) {
            return new PinnedMission(this.entryId, this.title, this.lines, this.trackedItems, nextCollapsed);
        }
    }

    public static final Codec<ManualReaderAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("read_entries", List.of())
                    .forGetter(a -> List.copyOf(a.readEntries)),
            ResourceLocation.CODEC.listOf().optionalFieldOf("favorite_entries", List.of())
                    .forGetter(a -> List.copyOf(a.favoriteEntries)),
            PinnedMission.CODEC.optionalFieldOf("pinned_mission").forGetter(ManualReaderAttachment::pinnedMission)
    ).apply(instance, (reads, favs, pin) -> new ManualReaderAttachment(
            Collections.unmodifiableSet(new LinkedHashSet<>(reads)),
            Collections.unmodifiableSet(new LinkedHashSet<>(favs)),
            pin
    )));

    public static final StreamCodec<RegistryFriendlyByteBuf, ManualReaderAttachment> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ByteBufCodecs.VAR_INT.encode(buf, val.readEntries.size());
                for (ResourceLocation id : val.readEntries) {
                    ResourceLocation.STREAM_CODEC.encode(buf, id);
                }
                ByteBufCodecs.VAR_INT.encode(buf, val.favoriteEntries.size());
                for (ResourceLocation id : val.favoriteEntries) {
                    ResourceLocation.STREAM_CODEC.encode(buf, id);
                }
                ByteBufCodecs.BOOL.encode(buf, val.pinnedMission.isPresent());
                val.pinnedMission.ifPresent(pin -> PinnedMission.STREAM_CODEC.encode(buf, pin));
            },
            buf -> {
                int readCount = Math.min(512, Math.max(0, ByteBufCodecs.VAR_INT.decode(buf)));
                Set<ResourceLocation> reads = new LinkedHashSet<>(readCount);
                for (int i = 0; i < readCount; i++) {
                    reads.add(ResourceLocation.STREAM_CODEC.decode(buf));
                }
                int favCount = Math.min(512, Math.max(0, ByteBufCodecs.VAR_INT.decode(buf)));
                Set<ResourceLocation> favs = new LinkedHashSet<>(favCount);
                for (int i = 0; i < favCount; i++) {
                    favs.add(ResourceLocation.STREAM_CODEC.decode(buf));
                }
                boolean hasPin = ByteBufCodecs.BOOL.decode(buf);
                Optional<PinnedMission> pin = hasPin ? Optional.of(PinnedMission.STREAM_CODEC.decode(buf)) : Optional.empty();
                return new ManualReaderAttachment(
                        Collections.unmodifiableSet(reads),
                        Collections.unmodifiableSet(favs),
                        pin
                );
            }
    );

    public boolean isRead(@NotNull ResourceLocation entryId) {
        return this.readEntries.contains(entryId);
    }

    public boolean isFavorite(@NotNull ResourceLocation entryId) {
        return this.favoriteEntries.contains(entryId);
    }

    public @NotNull ManualReaderAttachment withRead(@NotNull ResourceLocation entryId) {
        if (this.readEntries.contains(entryId)) {
            return this;
        }
        Set<ResourceLocation> next = new LinkedHashSet<>(this.readEntries);
        next.add(entryId);
        return new ManualReaderAttachment(Collections.unmodifiableSet(next), this.favoriteEntries, this.pinnedMission);
    }

    public @NotNull ManualReaderAttachment withFavoriteToggled(@NotNull ResourceLocation entryId) {
        Set<ResourceLocation> next = new LinkedHashSet<>(this.favoriteEntries);
        if (!next.remove(entryId)) {
            next.add(entryId);
        }
        return new ManualReaderAttachment(this.readEntries, Collections.unmodifiableSet(next), this.pinnedMission);
    }

    public @NotNull ManualReaderAttachment withPinnedMission(@Nullable PinnedMission mission) {
        return new ManualReaderAttachment(this.readEntries, this.favoriteEntries, Optional.ofNullable(mission));
    }

    public @NotNull ManualReaderAttachment withPinCollapsedToggled() {
        if (this.pinnedMission.isEmpty()) {
            return this;
        }
        PinnedMission current = this.pinnedMission.get();
        return new ManualReaderAttachment(this.readEntries, this.favoriteEntries, Optional.of(current.withCollapsed(!current.collapsed())));
    }
}
