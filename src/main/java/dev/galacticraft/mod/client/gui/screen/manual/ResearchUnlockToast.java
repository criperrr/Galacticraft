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

package dev.galacticraft.mod.client.gui.screen.manual;

import dev.galacticraft.mod.client.GCKeyBinds;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.manual.ManualEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sci-fi notification Toast displayed when new Aerospace R&amp;D research unlocks an entry
 * in the {@code AEROSPACE_MANUAL}, remembering the unlocked entry so pressing the manual
 * hotkey jumps straight to the newly discovered page.
 */
@Environment(EnvType.CLIENT)
public final class ResearchUnlockToast implements Toast {
    private static final long DISPLAY_TIME_MS = 5500L;
    private static final int WIDTH = 178;
    private static final int HEIGHT = 34;

    private static volatile @Nullable ResourceLocation recentUnlockedEntryId;
    private static volatile long recentUnlockedAtMs;

    private final ManualEntry entry;
    private final ItemStack iconStack;
    private final Component header;
    private final Component title;
    private final Component hint;

    public ResearchUnlockToast(@NotNull ManualEntry entry) {
        this.entry = entry;
        Item item = BuiltInRegistries.ITEM.get(entry.iconItem());
        this.iconStack = item != Items.AIR ? new ItemStack(item) : new ItemStack(GCItems.AEROSPACE_MANUAL);
        this.header = Component.translatable("manual.galacticraft.toast.unlocked_header");
        this.title = Component.translatable(entry.titleKey());
        this.hint = Component.translatable("manual.galacticraft.toast.open_hint", GCKeyBinds.OPEN_AEROSPACE_MANUAL.getTranslatedKeyMessage());
        recentUnlockedEntryId = entry.id();
        recentUnlockedAtMs = System.currentTimeMillis();
    }

    public static @Nullable ResourceLocation consumeRecentUnlockedEntry() {
        long elapsed = System.currentTimeMillis() - recentUnlockedAtMs;
        if (recentUnlockedEntryId != null && elapsed <= 15_000L) {
            ResourceLocation id = recentUnlockedEntryId;
            recentUnlockedEntryId = null;
            return id;
        }
        return null;
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    @Override
    public @NotNull Visibility render(@NotNull GuiGraphics graphics, @NotNull ToastComponent toastComponent, long timeSinceLastVisible) {
        int accent = this.entry.tab().accentColor();
        graphics.fill(0, 0, WIDTH, HEIGHT, 0xEE0B1424);
        graphics.fill(0, 0, WIDTH, 1, accent);
        graphics.fill(0, HEIGHT - 1, WIDTH, HEIGHT, 0xFF1F3B5C);
        graphics.fill(0, 0, 2, HEIGHT, accent);
        graphics.fill(WIDTH - 1, 0, WIDTH, HEIGHT, 0xFF1F3B5C);

        graphics.renderFakeItem(this.iconStack, 7, 9);

        Font font = toastComponent.getMinecraft().font;
        graphics.drawString(font, this.header, 28, 4, 0xFF38BDF8, false);
        String trimmedTitle = font.plainSubstrByWidth(this.title.getString(), WIDTH - 34);
        graphics.drawString(font, trimmedTitle, 28, 14, 0xFFF1F5F9, false);
        String trimmedHint = font.plainSubstrByWidth(this.hint.getString(), WIDTH - 34);
        graphics.drawString(font, trimmedHint, 28, 24, 0xFF94A3B8, false);

        double progress = Math.min(1.0, (double) timeSinceLastVisible / (double) DISPLAY_TIME_MS);
        int barW = (int) Math.round((WIDTH - 4) * (1.0 - progress));
        graphics.fill(2, HEIGHT - 2, 2 + Math.max(0, barW), HEIGHT - 1, accent);

        return timeSinceLastVisible >= DISPLAY_TIME_MS ? Visibility.HIDE : Visibility.SHOW;
    }
}
