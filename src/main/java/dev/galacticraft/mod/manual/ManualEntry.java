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

package dev.galacticraft.mod.manual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.api.accessor.ResearchAccessor;
import dev.galacticraft.mod.Constant;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Data-driven entry in the Galacticraft 5 Aerospace Manual ({@code data/galacticraft/manual_entries/<tab>/<id>.json}).
 *
 * <p>Each entry links to an optional {@code required_research} node in the Aerospace R&amp;D Tech Tree
 * (or {@link #UNLOCKED_SENTINEL} / empty for starter chapters) and carries a guided {@code lock_hint_key}
 * so locked entries always instruct the player where and how to research them rather than hiding everything.
 */
public record ManualEntry(
        @NotNull ResourceLocation id,
        @NotNull ManualTab tab,
        int era,
        int order,
        @NotNull String titleKey,
        @NotNull ResourceLocation iconItem,
        @NotNull Optional<ResourceLocation> requiredResearch,
        @NotNull String lockHintKey,
        @NotNull List<ResourceLocation> relatedItems,
        @NotNull List<ManualPage> pages
) {
    public static final ResourceLocation UNLOCKED_SENTINEL = Constant.id("unlocked");

    public static final Codec<ManualEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(ManualEntry::id),
            ManualTab.CODEC.fieldOf("tab").forGetter(ManualEntry::tab),
            Codec.INT.optionalFieldOf("era", 1).forGetter(ManualEntry::era),
            Codec.INT.optionalFieldOf("order", 0).forGetter(ManualEntry::order),
            Codec.STRING.fieldOf("title_key").forGetter(ManualEntry::titleKey),
            ResourceLocation.CODEC.fieldOf("icon_item").forGetter(ManualEntry::iconItem),
            ResourceLocation.CODEC.optionalFieldOf("required_research").forGetter(ManualEntry::requiredResearch),
            Codec.STRING.optionalFieldOf("lock_hint_key", "manual.galacticraft.hint.default").forGetter(ManualEntry::lockHintKey),
            ResourceLocation.CODEC.listOf().optionalFieldOf("related_items", List.of()).forGetter(ManualEntry::relatedItems),
            ManualPage.CODEC.listOf().fieldOf("pages").forGetter(ManualEntry::pages)
    ).apply(instance, ManualEntry::new));

    public boolean requiresResearch() {
        return this.requiredResearch.isPresent() && !UNLOCKED_SENTINEL.equals(this.requiredResearch.get());
    }

    public @Nullable ResourceLocation requiredResearchId() {
        return requiresResearch() ? this.requiredResearch.get() : null;
    }

    public boolean isUnlocked(@NotNull Set<ResourceLocation> unlockedResearchNodes) {
        if (!requiresResearch()) {
            return true;
        }
        return unlockedResearchNodes.contains(this.requiredResearch.get());
    }

    public boolean isUnlocked(@Nullable ResearchAccessor accessor) {
        if (!requiresResearch()) {
            return true;
        }
        return accessor != null && accessor.galacticraft$isUnlocked(this.requiredResearch.get());
    }
}
