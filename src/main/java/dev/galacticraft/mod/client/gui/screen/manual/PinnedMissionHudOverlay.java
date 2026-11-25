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

import dev.galacticraft.mod.attachments.GCAttachments;
import dev.galacticraft.mod.attachments.ManualReaderAttachment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Compact, collapsible in-game HUD overlay showing the player's pinned Aerospace Manual
 * mission checklist, multiblock materials, or recipe reminder, persisted via
 * {@link GCAttachments#MANUAL_READER}.
 */
@Environment(EnvType.CLIENT)
@SuppressWarnings("UnstableApiUsage")
public final class PinnedMissionHudOverlay {
    private static final int CARD_X = 6;
    private static final int CARD_Y = 6;
    private static final int CARD_W = 156;

    private PinnedMissionHudOverlay() {
    }

    public static void onHudRender(@NotNull GuiGraphics graphics, @NotNull DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || mc.player.isSpectator()) {
            return;
        }
        ManualReaderAttachment state = mc.player.getAttached(GCAttachments.MANUAL_READER);
        if (state == null || state.pinnedMission().isEmpty()) {
            return;
        }

        ManualReaderAttachment.PinnedMission pin = state.pinnedMission().get();
        Font font = mc.font;
        boolean collapsed = pin.collapsed();

        List<String> lines = pin.lines();
        List<ResourceLocation> tracked = pin.trackedItems();
        int visibleLines = collapsed ? 0 : Math.min(6, lines.size());
        int visibleItems = collapsed ? 0 : Math.min(5, tracked.size());
        int cardH = 18 + (collapsed ? 0 : visibleLines * 10 + (visibleItems > 0 ? visibleItems * 13 + 4 : 0) + 4);

        // Sci-fi card frame (top-left, clear of OxygenOverlay on top-right)
        graphics.fill(CARD_X, CARD_Y, CARD_X + CARD_W, CARD_Y + cardH, 0xCC0A1322);
        graphics.fill(CARD_X, CARD_Y, CARD_X + CARD_W, CARD_Y + 1, 0xFF38BDF8);
        graphics.fill(CARD_X, CARD_Y + cardH - 1, CARD_X + CARD_W, CARD_Y + cardH, 0xFF1E3A5F);
        graphics.fill(CARD_X, CARD_Y, CARD_X + 2, CARD_Y + cardH, 0xFF38BDF8);
        graphics.fill(CARD_X + CARD_W - 1, CARD_Y, CARD_X + CARD_W, CARD_Y + cardH, 0xFF1E3A5F);

        String header = (collapsed ? "[+] " : "[-] ") + pin.title();
        String trimmedHeader = font.plainSubstrByWidth(header, CARD_W - 10);
        graphics.drawString(font, trimmedHeader, CARD_X + 5, CARD_Y + 5, 0xFF7DD3FC, false);

        if (collapsed) {
            return;
        }

        int cursorY = CARD_Y + 18;
        for (int i = 0; i < visibleLines; i++) {
            String trimmed = font.plainSubstrByWidth(lines.get(i), CARD_W - 12);
            graphics.drawString(font, trimmed, CARD_X + 6, cursorY, 0xFFCBD5E1, false);
            cursorY += 10;
        }

        if (visibleItems > 0) {
            cursorY += 2;
            for (int i = 0; i < visibleItems; i++) {
                ResourceLocation itemId = tracked.get(i);
                Item item = BuiltInRegistries.ITEM.get(itemId);
                if (item == Items.AIR) {
                    continue;
                }
                int owned = countItemInPlayer(mc.player, item);
                boolean ready = owned > 0;
                ItemStack icon = new ItemStack(item);
                graphics.pose().pushPose();
                graphics.pose().translate(CARD_X + 5, cursorY - 1, 0);
                graphics.pose().scale(0.68F, 0.68F, 1.0F);
                graphics.renderFakeItem(icon, 0, 0);
                graphics.pose().popPose();

                String itemName = font.plainSubstrByWidth(icon.getHoverName().getString(), CARD_W - 48);
                int nameColor = ready ? 0xFF4ADE80 : 0xFFE2E8F0;
                graphics.drawString(font, itemName, CARD_X + 19, cursorY + 1, nameColor, false);
                String badge = ready ? ("✔ " + owned) : "✖ 0";
                int badgeW = font.width(badge);
                graphics.drawString(font, badge, CARD_X + CARD_W - badgeW - 5, cursorY + 1, ready ? 0xFF4ADE80 : 0xFFF87171, false);
                cursorY += 13;
            }
        }
    }

    public static int countItemInPlayer(@NotNull Player player, @NotNull Item target) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.is(target)) {
                count += s.getCount();
            }
        }
        Container gear = player.galacticraft$getGearInv();
        if (gear != null) {
            for (int i = 0; i < gear.getContainerSize(); i++) {
                ItemStack s = gear.getItem(i);
                if (!s.isEmpty() && s.is(target)) {
                    count += s.getCount();
                }
            }
        }
        return count;
    }
}
