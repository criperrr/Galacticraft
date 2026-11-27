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

package dev.galacticraft.mod.client.render.vehicle;

import com.mojang.blaze3d.platform.InputConstants;
import dev.galacticraft.api.vehicle.VehicleDerivedStats;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.network.c2s.DroneRemoteControlPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Client-side remote flight controller for {@code SMALL} unmanned electric drones
 * ({@code DroneRemoteControllerItem}). Supports both 1st-person FPV gimbal view and
 * 3rd-person chase camera (toggled via {@code V}), locks the standing pilot's body in place,
 * streams flight commands via {@link DroneRemoteControlPayload}, and renders an aerospace HUD.
 */
@Environment(EnvType.CLIENT)
public final class ClientDroneRemoteController {
    private static boolean active = false;
    private static int droneEntityId = -1;
    private static boolean firstPersonFpv = false;
    private static float cameraPitch = 14.0F;
    private static float pendingYawDelta = 0.0F;
    private static @Nullable CameraType savedCameraType = null;
    private static int enterGraceTicks = 0;
    private static boolean vKeyWasDown = false;
    private static boolean lKeyWasDown = false;

    private ClientDroneRemoteController() {
    }

    public static void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientDroneRemoteController::onClientTick);
        HudRenderCallback.EVENT.register(ClientDroneRemoteController::onRenderHud);
    }

    public static void enterControl(@NotNull ModularVehicleEntity drone) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (active && droneEntityId == drone.getId()) {
            exitControl();
            return;
        }
        active = true;
        droneEntityId = drone.getId();
        cameraPitch = firstPersonFpv ? 6.0F : 16.0F;
        pendingYawDelta = 0.0F;
        savedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        enterGraceTicks = 10;
        vKeyWasDown = false;
        lKeyWasDown = false;
        mc.setScreen(null);
        mc.mouseHandler.grabMouse();
        mc.player.playSound(SoundEvents.BEACON_ACTIVATE, 0.55F, 1.45F);
    }

    public static void exitControl() {
        if (!active) {
            return;
        }
        active = false;
        droneEntityId = -1;
        pendingYawDelta = 0.0F;
        Minecraft mc = Minecraft.getInstance();
        if (savedCameraType != null) {
            mc.options.setCameraType(savedCameraType);
            savedCameraType = null;
        }
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.BEACON_DEACTIVATE, 0.5F, 1.2F);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isFirstPersonFpv() {
        return firstPersonFpv;
    }

    public static @Nullable ModularVehicleEntity getActiveDrone() {
        if (!active || droneEntityId < 0) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        Entity entity = mc.level.getEntity(droneEntityId);
        if (entity instanceof ModularVehicleEntity vehicle && !vehicle.isRemoved() && vehicle.isRemoteControllableDrone()) {
            return vehicle;
        }
        return null;
    }

    public static void onMouseTurn(double yawDelta, double pitchDelta) {
        if (!active) {
            return;
        }
        pendingYawDelta = Mth.clamp(pendingYawDelta + (float) (yawDelta * 0.14), -16.0F, 16.0F);
        cameraPitch = Mth.clamp(cameraPitch + (float) (pitchDelta * 0.12), firstPersonFpv ? -45.0F : -20.0F, 65.0F);
    }

    public static float getCameraYaw(float partialTick) {
        ModularVehicleEntity drone = getActiveDrone();
        if (drone == null) {
            return 0.0F;
        }
        return Mth.rotLerp(partialTick, drone.yRotO, drone.getYRot());
    }

    public static float getCameraPitch(float partialTick) {
        ModularVehicleEntity drone = getActiveDrone();
        if (drone == null) {
            return cameraPitch;
        }
        return firstPersonFpv ? Mth.clamp(cameraPitch + drone.clientPitch * 0.35F, -60.0F, 70.0F) : cameraPitch;
    }

    public static @NotNull Vec3 getCameraPosition(float partialTick) {
        ModularVehicleEntity drone = getActiveDrone();
        if (drone == null) {
            return Vec3.ZERO;
        }
        Vec3 pos = drone.getPosition(partialTick);
        float yawRad = getCameraYaw(partialTick) * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yawRad);
        double fz = Mth.cos(yawRad);
        if (firstPersonFpv) {
            return pos.add(fx * 0.42, 0.56, fz * 0.42);
        }
        // 3rd-person chase camera behind & slightly above the drone
        return pos.add(-fx * 3.45, 1.38, -fz * 3.45);
    }

    private static void onClientTick(Minecraft mc) {
        if (!active) {
            return;
        }
        LocalPlayer player = mc.player;
        ModularVehicleEntity drone = getActiveDrone();
        if (player == null || mc.level == null || mc.screen != null || drone == null) {
            exitControl();
            return;
        }
        double maxDist = DroneRemoteControlPayload.MAX_RANGE_BLOCKS;
        if (player.distanceToSqr(drone) > maxDist * maxDist) {
            exitControl();
            return;
        }

        if (enterGraceTicks > 0) {
            enterGraceTicks--;
        }

        long window = mc.getWindow().getWindow();
        if (enterGraceTicks == 0 && InputConstants.isKeyDown(window, GLFW.GLFW_KEY_X)) {
            exitControl();
            return;
        }

        // Toggle 1st-person FPV <-> 3rd-person Chase Camera with V
        boolean vDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_V);
        if (vDown && !vKeyWasDown) {
            firstPersonFpv = !firstPersonFpv;
            cameraPitch = firstPersonFpv ? 6.0F : 16.0F;
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, firstPersonFpv ? 1.5F : 1.1F);
        }
        vKeyWasDown = vDown;

        boolean lDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_L);
        boolean toggleLights = lDown && !lKeyWasDown;
        lKeyWasDown = lDown;

        float forward = (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_W) ? 1.0F : 0.0F)
                - (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_S) ? 1.0F : 0.0F);
        float strafe = (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_D) ? 1.0F : 0.0F)
                - (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_A) ? 1.0F : 0.0F);
        float collective = (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_SPACE) ? 1.0F : 0.0F)
                - ((InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)) ? 1.0F : 0.0F);

        float keyYaw = ((InputConstants.isKeyDown(window, GLFW.GLFW_KEY_E) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT)) ? 3.5F : 0.0F)
                - ((InputConstants.isKeyDown(window, GLFW.GLFW_KEY_Q) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT)) ? 3.5F : 0.0F);
        float yawDelta = Mth.clamp(pendingYawDelta + keyYaw, -16.0F, 16.0F);
        pendingYawDelta = 0.0F;

        ClientPlayNetworking.send(new DroneRemoteControlPayload(
                drone.getId(), forward, strafe, yawDelta, collective, toggleLights, false
        ));
    }

    private static void onRenderHud(GuiGraphics g, DeltaTracker deltaTracker) {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ModularVehicleEntity drone = getActiveDrone();
        if (player == null || drone == null) {
            return;
        }

        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int cy = h / 2;

        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(drone.level());
        VehicleDerivedStats stats = drone.getDerivedStats();
        double dist = player.distanceTo(drone);
        double agl = drone.computeAltitudeAgl();
        double twr = stats.computeRotorTwr(env.gravityG(), env.pressureAtm());
        long cap = Math.max(1L, stats.energyCapacity());
        int batPct = Math.round((drone.getConfiguration().storedEnergy() * 100.0F) / cap);
        int rpm = Math.round(drone.getRotorRpm());

        // FPV viewfinder brackets + artificial horizon reticle
        if (firstPersonFpv) {
            int pad = 18;
            int len = 22;
            int col = 0xAA38BDF8;
            g.fill(pad, pad, pad + len, pad + 2, col);
            g.fill(pad, pad, pad + 2, pad + len, col);
            g.fill(w - pad - len, pad, w - pad, pad + 2, col);
            g.fill(w - pad - 2, pad, w - pad, pad + len, col);
            g.fill(pad, h - pad - 2, pad + len, h - pad, col);
            g.fill(pad, h - pad - len, pad + 2, h - pad, col);
            g.fill(w - pad - len, h - pad - 2, w - pad, h - pad, col);
            g.fill(w - pad - 2, h - pad - len, w - pad, h - pad, col);

            // Pitch/horizon bars around crosshair
            int pitchOffset = Math.round(drone.clientPitch * 1.1F);
            g.fill(cx - 32, cy + pitchOffset, cx - 10, cy + pitchOffset + 1, 0xCC5BF5A0);
            g.fill(cx + 11, cy + pitchOffset, cx + 33, cy + pitchOffset + 1, 0xCC5BF5A0);
            g.fill(cx - 4, cy, cx + 5, cy + 1, 0xEEFFFFFF);
            g.fill(cx, cy - 4, cx + 1, cy + 5, 0xEEFFFFFF);
        }

        // Top Aerospace Telemetry Strip
        String modeTag = firstPersonFpv ? "DRONE FPV" : "DRONE 3P CHASE";
        String strip = String.format(
                Locale.ROOT,
                "%s  |  DIST %.0fm/128m  |  AGL %.1fm  |  RPM %d  |  BAT %d%%  |  TWR %.2f",
                modeTag, dist, agl, rpm, batPct, Math.min(twr, 99.0)
        );
        int sw = font.width(strip) + 16;
        g.fill(cx - sw / 2, 8, cx + sw / 2, 22, 0xCC0A1320);
        g.renderOutline(cx - sw / 2, 8, sw, 14, 0xFF2ED1FF);
        g.drawString(font, strip, cx - font.width(strip) / 2, 11, 0xFFEAF6FF, true);

        // Vacuum / Low-TWR Alert Banner
        if (stats.isBlockedByVacuum(env.pressureAtm())) {
            String alert = "SEM ATMOSFERA — INSTALE PROPULSORES VTOL ELÉTRICOS!";
            int aw = font.width(alert) + 14;
            g.fill(cx - aw / 2, 26, cx + aw / 2, 40, 0xD0450A0A);
            g.renderOutline(cx - aw / 2, 26, aw, 14, 0xFFFF5252);
            g.drawString(font, alert, cx - font.width(alert) / 2, 29, 0xFFFFB0B0, true);
        } else if (stats.isDrivable() && twr < 1.0) {
            String alert = String.format(Locale.ROOT, "EMPUXO INSUFICIENTE (TWR %.2f) — USE ROTORES DE MARTE OU VTOL", twr);
            int aw = font.width(alert) + 14;
            g.fill(cx - aw / 2, 26, cx + aw / 2, 40, 0xD0422506);
            g.renderOutline(cx - aw / 2, 26, aw, 14, 0xFFFFB830);
            g.drawString(font, alert, cx - font.width(alert) / 2, 29, 0xFFFFE099, true);
        }

        // Bottom Controls Bar
        String controls = "[WASD] Voar  [SPACE/SHIFT] Subir/Descer  [MOUSE/Q/E] Girar  [V] FPV/3ª Pessoa  [L] Luzes  [ESC/X] Sair";
        int cw = font.width(controls) + 14;
        g.fill(cx - cw / 2, h - 22, cx + cw / 2, h - 8, 0xCC0A1320);
        g.renderOutline(cx - cw / 2, h - 22, cw, 14, 0xFF3A526E);
        g.drawString(font, controls, cx - font.width(controls) / 2, h - 19, 0xFFB8D4E8, true);
    }
}
