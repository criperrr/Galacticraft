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

package dev.galacticraft.api.rocket.part.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable per-slot material composition stamped onto a manufactured modular rocket part and
 * persisted across {@link net.minecraft.world.item.ItemStack} data components, network packets,
 * and world-placed block entities.
 *
 * @param entries Ordered list of slot material assignments sorted by ascending {@code slotIndex}.
 */
public record RocketPartComposition(@NotNull List<SlotMaterialEntry> entries) {
    /**
     * Single slot assignment recording which material/sub-component was installed at a grid slot
     * and which passive traits it granted.
     *
     * @param slotIndex Grid index ({@code 0..8}) of the active slot in the Rocket Workbench.
     * @param slotRole  Serialized {@link RocketMaterialProfile.SlotRole} or human-readable role key.
     * @param itemId    Registry identifier of the item consumed from the slot.
     * @param traits    Ordered, deduplicated list of engineering traits granted by this slot.
     */
    public record SlotMaterialEntry(
            int slotIndex,
            @NotNull String slotRole,
            @NotNull ResourceLocation itemId,
            @NotNull List<RocketMaterialTrait> traits
    ) {
        public static final Codec<SlotMaterialEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("slot_index").forGetter(SlotMaterialEntry::slotIndex),
                Codec.STRING.fieldOf("slot_role").forGetter(SlotMaterialEntry::slotRole),
                ResourceLocation.CODEC.fieldOf("item_id").forGetter(SlotMaterialEntry::itemId),
                RocketMaterialTrait.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(SlotMaterialEntry::traits)
        ).apply(instance, SlotMaterialEntry::new));

        public static final StreamCodec<ByteBuf, SlotMaterialEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SlotMaterialEntry::slotIndex,
                ByteBufCodecs.STRING_UTF8, SlotMaterialEntry::slotRole,
                ResourceLocation.STREAM_CODEC, SlotMaterialEntry::itemId,
                RocketMaterialTrait.STREAM_CODEC.apply(ByteBufCodecs.list()), SlotMaterialEntry::traits,
                SlotMaterialEntry::new
        );

        public SlotMaterialEntry {
            if (slotIndex < 0) {
                throw new IllegalArgumentException("slotIndex must be >= 0, got " + slotIndex);
            }
            Objects.requireNonNull(slotRole, "slotRole");
            if (slotRole.isBlank()) {
                throw new IllegalArgumentException("slotRole must not be blank");
            }
            Objects.requireNonNull(itemId, "itemId");
            Objects.requireNonNull(traits, "traits");
            traits = List.copyOf(new LinkedHashSet<>(traits));
        }

        public static @NotNull SlotMaterialEntry of(
                int slotIndex,
                @NotNull RocketMaterialProfile.SlotRole role,
                @NotNull ResourceLocation itemId,
                @NotNull List<RocketMaterialTrait> traits
        ) {
            Objects.requireNonNull(role, "role");
            return new SlotMaterialEntry(slotIndex, role.getSerializedName(), itemId, traits);
        }

        public static @NotNull SlotMaterialEntry fromProfile(
                int slotIndex,
                @NotNull RocketMaterialProfile.SlotRole role,
                @NotNull ResourceLocation itemId,
                @NotNull RocketMaterialProfile profile
        ) {
            Objects.requireNonNull(profile, "profile");
            return of(slotIndex, role, itemId, profile.traits());
        }
    }

    public static final RocketPartComposition EMPTY = new RocketPartComposition(List.of());

    public static final Codec<RocketPartComposition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SlotMaterialEntry.CODEC.listOf().fieldOf("slots").forGetter(RocketPartComposition::entries)
    ).apply(instance, RocketPartComposition::new));

    public static final StreamCodec<ByteBuf, RocketPartComposition> STREAM_CODEC = SlotMaterialEntry.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(RocketPartComposition::new, RocketPartComposition::entries);

    public RocketPartComposition {
        Objects.requireNonNull(entries, "entries");
        List<SlotMaterialEntry> sorted = new ArrayList<>(entries.size());
        for (SlotMaterialEntry entry : entries) {
            sorted.add(Objects.requireNonNull(entry, "entry"));
        }
        sorted.sort(Comparator.comparingInt(SlotMaterialEntry::slotIndex));
        entries = List.copyOf(sorted);
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    public @NotNull Optional<SlotMaterialEntry> entryForSlot(int slotIndex) {
        for (SlotMaterialEntry entry : this.entries) {
            if (entry.slotIndex() == slotIndex) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all distinct engineering traits across every slot entry in deterministic slot order.
     */
    public @NotNull List<RocketMaterialTrait> allTraits() {
        LinkedHashSet<RocketMaterialTrait> collected = new LinkedHashSet<>();
        for (SlotMaterialEntry entry : this.entries) {
            collected.addAll(entry.traits());
        }
        return List.copyOf(collected);
    }

    public boolean hasTrait(@NotNull RocketMaterialTrait trait) {
        Objects.requireNonNull(trait, "trait");
        for (SlotMaterialEntry entry : this.entries) {
            if (entry.traits().contains(trait)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns distinct consumed material item IDs in ascending slot order.
     */
    public @NotNull List<ResourceLocation> distinctItemIds() {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        for (SlotMaterialEntry entry : this.entries) {
            ids.add(entry.itemId());
        }
        return List.copyOf(ids);
    }
}
