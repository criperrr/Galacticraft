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

package dev.galacticraft.mod.client;

import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import dev.galacticraft.mod.attachments.GCAttachments;
import dev.galacticraft.mod.attachments.ManualReaderAttachment;
import dev.galacticraft.mod.client.gui.screen.ingame.CelestialSelectionScreen;
import dev.galacticraft.mod.client.gui.screen.manual.AerospaceManualScreen;
import dev.galacticraft.mod.client.gui.screen.manual.ResearchUnlockToast;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartPlacementPreviewRenderer;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.network.c2s.ModularRocketControlPayload;
import dev.galacticraft.mod.network.c2s.OpenRocketPayload;
import dev.galacticraft.mod.network.c2s.UpdateManualStatePayload;
import dev.galacticraft.mod.util.Translations;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("UnstableApiUsage")
public class GCKeyBinds {
    public static final KeyMapping OPEN_CELESTIAL_SCREEN = new KeyMapping(Translations.Keybindings.OPEN_CELESTIAL_SCREEN, GLFW.GLFW_KEY_M, KeyMapping.CATEGORY_MISC);
    public static final KeyMapping OPEN_ROCKET_INVENTORY = new KeyMapping(Translations.Keybindings.ROCKET_INVENTORY, GLFW.GLFW_KEY_F, KeyMapping.CATEGORY_INVENTORY);
    public static final KeyMapping OPEN_AEROSPACE_MANUAL = new KeyMapping(Translations.Keybindings.OPEN_AEROSPACE_MANUAL, GLFW.GLFW_KEY_G, KeyMapping.CATEGORY_MISC);
    public static final KeyMapping CYCLE_ROCKET_SYMMETRY = new KeyMapping("key.galacticraft.cycle_rocket_symmetry", GLFW.GLFW_KEY_R, KeyMapping.CATEGORY_MISC);
    public static final KeyMapping ROCKET_LES_ABORT = new KeyMapping("key.galacticraft.rocket_les_abort", GLFW.GLFW_KEY_BACKSPACE, KeyMapping.CATEGORY_MISC);
    public static final KeyMapping ROCKET_LANDING_GEAR = new KeyMapping("key.galacticraft.rocket_landing_gear", GLFW.GLFW_KEY_V, KeyMapping.CATEGORY_MISC);
    public static final KeyMapping ROCKET_ENGINE_CUTOFF = new KeyMapping("key.galacticraft.rocket_engine_cutoff", GLFW.GLFW_KEY_X, KeyMapping.CATEGORY_MISC);

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_CELESTIAL_SCREEN);
        KeyBindingHelper.registerKeyBinding(OPEN_ROCKET_INVENTORY);
        KeyBindingHelper.registerKeyBinding(OPEN_AEROSPACE_MANUAL);
        KeyBindingHelper.registerKeyBinding(CYCLE_ROCKET_SYMMETRY);
        KeyBindingHelper.registerKeyBinding(ROCKET_LES_ABORT);
        KeyBindingHelper.registerKeyBinding(ROCKET_LANDING_GEAR);
        KeyBindingHelper.registerKeyBinding(ROCKET_ENGINE_CUTOFF);
    }

    public static void handleKeybinds(Minecraft client) {
        if (client.level != null && client.player != null) {
            if (client.screen == null) {
                ModularRocketFlightEntity modularRocket = client.player.getVehicle() instanceof ModularRocketFlightEntity r ? r : null;
                if (modularRocket != null) {
                    while (ROCKET_LES_ABORT.consumeClick()) {
                        ClientPlayNetworking.send(new ModularRocketControlPayload(
                                modularRocket.getId(), 0.0F, 0.0F, 0.0F, ModularRocketControlPayload.FLAG_LES_ABORT));
                    }
                    while (ROCKET_LANDING_GEAR.consumeClick()) {
                        ClientPlayNetworking.send(new ModularRocketControlPayload(
                                modularRocket.getId(), 0.0F, 0.0F, 0.0F, ModularRocketControlPayload.FLAG_TOGGLE_LANDING_GEAR));
                    }
                    while (ROCKET_ENGINE_CUTOFF.consumeClick()) {
                        ClientPlayNetworking.send(new ModularRocketControlPayload(
                                modularRocket.getId(), 0.0F, 0.0F, 0.0F, ModularRocketControlPayload.FLAG_ENGINE_CUTOFF));
                    }
                    while (OPEN_ROCKET_INVENTORY.consumeClick()) {
                        ClientPlayNetworking.send(OpenRocketPayload.INSTANCE);
                    }
                } else {
                    while (ROCKET_LES_ABORT.consumeClick()) {
                    }
                    while (ROCKET_LANDING_GEAR.consumeClick()) {
                    }
                    while (ROCKET_ENGINE_CUTOFF.consumeClick()) {
                    }
                }
                while (OPEN_CELESTIAL_SCREEN.consumeClick()) {
                    client.setScreen(new CelestialSelectionScreen(true, null, false, null, null));
                }
                while (OPEN_AEROSPACE_MANUAL.consumeClick()) {
                    if (modularRocket != null) {
                        int flag = Screen.hasShiftDown()
                                ? ModularRocketControlPayload.FLAG_TOGGLE_AUTOPILOT
                                : ModularRocketControlPayload.FLAG_MANUAL_STAGE;
                        ClientPlayNetworking.send(new ModularRocketControlPayload(
                                modularRocket.getId(), 0.0F, 0.0F, 0.0F, flag));
                    } else if (Screen.hasShiftDown()) {
                        ManualReaderAttachment current = client.player.getAttached(GCAttachments.MANUAL_READER);
                        if (current != null && current.pinnedMission().isPresent()) {
                            client.player.setAttached(GCAttachments.MANUAL_READER, current.withPinCollapsedToggled());
                            ClientPlayNetworking.send(UpdateManualStatePayload.togglePinCollapse());
                        }
                    } else {
                        client.setScreen(new AerospaceManualScreen(ResearchUnlockToast.consumeRecentUnlockedEntry()));
                    }
                }
                while (CYCLE_ROCKET_SYMMETRY.consumeClick()) {
                    boolean holdingPart = client.player.getMainHandItem().getItem() instanceof ModularRocketPartBlockItem
                            || client.player.getOffhandItem().getItem() instanceof ModularRocketPartBlockItem;
                    if (holdingPart && Screen.hasShiftDown()) {
                        RadialSymmetry next = RocketPartPlacementPreviewRenderer.cycleSymmetry();
                        client.player.displayClientMessage(
                                Component.literal("[VAB] Simetria Radial: " + next.getSerializedName())
                                        .withStyle(ChatFormatting.AQUA),
                                true
                        );
                    }
                }
            }
        }
    }
}
