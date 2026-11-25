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
import dev.galacticraft.mod.manual.ManualClientBridge;
import dev.galacticraft.mod.manual.ManualEntry;
import dev.galacticraft.mod.manual.ManualManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Optional;

/**
 * Registers client-only hooks for the Aerospace Manual:
 * <ul>
 *   <li>{@link ManualClientBridge} handlers for opening the screen &amp; dispatching {@link ResearchUnlockToast}s</li>
 *   <li>{@link ItemTooltipCallback} hint on every item referenced in the manual</li>
 *   <li>{@link ScreenKeyboardEvents} quick-jump from hovered inventory slots via the manual hotkey (zero mixins)</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class ManualTooltipAndKeyBridge {
    private static @Nullable Field hoveredSlotField;
    private static boolean hoveredSlotFieldResolved = false;

    private ManualTooltipAndKeyBridge() {
    }

    public static void init() {
        ManualClientBridge.registerClientHandlers(
                ManualTooltipAndKeyBridge::openManual,
                ManualTooltipAndKeyBridge::notifyUnlockedNodes
        );

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (!stack.isEmpty() && ManualManager.INSTANCE.findEntryForItem(stack.getItem()).isPresent()) {
                lines.add(Component.translatable(
                        "tooltip.galacticraft.open_aerospace_manual",
                        GCKeyBinds.OPEN_AEROSPACE_MANUAL.getTranslatedKeyMessage()
                ).withStyle(ChatFormatting.AQUA));
            }
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                ScreenKeyboardEvents.afterKeyPress(screen).register((s, key, scancode, modifiers) -> {
                    if (GCKeyBinds.OPEN_AEROSPACE_MANUAL.matches(key, scancode)) {
                        Slot slot = getHoveredSlot(containerScreen);
                        if (slot != null && slot.hasItem()) {
                            ItemStack hovered = slot.getItem();
                            Optional<ResourceLocation> entryId = ManualManager.INSTANCE.findEntryForItem(hovered.getItem());
                            entryId.ifPresent(id -> client.setScreen(new AerospaceManualScreen(id)));
                        }
                    }
                });
            }
        });
    }

    private static void openManual(@Nullable ResourceLocation entryId) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation target = entryId != null ? entryId : ResearchUnlockToast.consumeRecentUnlockedEntry();
        mc.setScreen(new AerospaceManualScreen(target));
    }

    private static void notifyUnlockedNodes(Collection<ResourceLocation> newlyUnlockedNodeIds) {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation nodeId : newlyUnlockedNodeIds) {
            for (ManualEntry entry : ManualManager.INSTANCE.entriesUnlockedBy(nodeId)) {
                mc.getToasts().addToast(new ResearchUnlockToast(entry));
            }
        }
    }

    private static @Nullable Slot getHoveredSlot(AbstractContainerScreen<?> screen) {
        if (!hoveredSlotFieldResolved) {
            hoveredSlotFieldResolved = true;
            for (Field f : AbstractContainerScreen.class.getDeclaredFields()) {
                if (f.getType() == Slot.class) {
                    f.setAccessible(true);
                    hoveredSlotField = f;
                    break;
                }
            }
        }
        if (hoveredSlotField == null) {
            return null;
        }
        try {
            return (Slot) hoveredSlotField.get(screen);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
