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
import dev.galacticraft.mod.client.GCKeyBinds;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.network.c2s.VehicleControlActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Client-side dedicated aircraft Throttle Quadrant controller and live Glass-Cockpit HUD
 * for Phase 3 Electric Fixed-Wing Airplanes:
 * <ul>
 *   <li>Dedicated rebindable throttle keys ({@code R}/{@code Up} increase, {@code C}/{@code Down} decrease,
 *       {@code X} instant IDLE cutoff) with real power-lever retention ({@code 0%..100%}).</li>
 *   <li>Live vertical throttle gauge, IAS airspeed vs planetary {@code V_stall} indicator,
 *       AGL altitude, retractable landing gear status, battery charge, and stall/vacuum warnings.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class FixedWingFlightHud {
    private static float lastSyncedThrottle = -1.0F;
    private static int syncCooldownTicks = 0;

    private FixedWingFlightHud() {
    }

    public static void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(FixedWingFlightHud::onClientTick);
        HudRenderCallback.EVENT.register(FixedWingFlightHud::onRenderHud);
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof ModularVehicleEntity plane) || !plane.isFixedWing()) {
            lastSyncedThrottle = -1.0F;
            return;
        }
        if (plane.getControllingPassenger() != mc.player) {
            return;
        }

        long window = mc.getWindow().getWindow();
        boolean upKey = GCKeyBinds.AIRCRAFT_THROTTLE_UP.isDown()
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_UP);
        boolean downKey = GCKeyBinds.AIRCRAFT_THROTTLE_DOWN.isDown()
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DOWN);
        boolean cutoff = GCKeyBinds.AIRCRAFT_THROTTLE_CUTOFF.consumeClick();

        boolean changed = false;
        if (cutoff) {
            plane.setThrottleDirect(0.0F);
            mc.player.playSound(SoundEvents.LEVER_CLICK, 0.55F, 0.85F);
            changed = true;
        } else if (upKey && !downKey) {
            if (plane.isEngineRunning() && !plane.isParkingBrakeOn() && plane.getDerivedStats().isDrivable()) {
                plane.adjustThrottleDirect(0.04F);
                changed = true;
            }
        } else if (downKey && !upKey) {
            plane.adjustThrottleDirect(-0.045F);
            changed = true;
        }

        if (syncCooldownTicks > 0) {
            syncCooldownTicks--;
        }
        float current = plane.getThrottle();
        if (changed && (cutoff || syncCooldownTicks == 0 || Math.abs(current - lastSyncedThrottle) >= 0.04F)) {
            ClientPlayNetworking.send(new VehicleControlActionPayload(
                    plane.getId(), "set_throttle", "", String.format(Locale.ROOT, "%.3f", current)
            ));
            lastSyncedThrottle = current;
            syncCooldownTicks = 2;
        }
    }

    private static void onRenderHud(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof ModularVehicleEntity plane) || !plane.isFixedWing()) {
            return;
        }

        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        VehicleDerivedStats stats = plane.getDerivedStats();
        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(plane.level());
        float throttle = Mth.clamp(plane.getThrottle(), 0.0F, 1.0F);
        double speedBpt = Math.abs(plane.getSyncedSpeed());
        double iasKmh = speedBpt * 72.0;
        double maxKmh = Math.max(40.0, stats.maxSpeedBlocksPerTick() * 72.0);
        double stallBpt = stats.computeStallSpeedBlocksPerTick(env.gravityG(), env.pressureAtm());
        double stallKmh = Double.isFinite(stallBpt) ? stallBpt * 72.0 : -1.0;
        double agl = plane.computeAltitudeAgl();
        long energy = plane.getConfiguration().storedEnergy();
        long cap = Math.max(1L, stats.energyCapacity());
        int batPct = Math.round(Mth.clamp((float) energy / (float) cap, 0.0F, 1.0F) * 100.0F);
        boolean vacuumBlocked = stats.isBlockedByVacuum(env.pressureAtm());
        boolean stalling = plane.isStalling();
        boolean hybridVtol = stats.isHybridVtolFixedWing();

        // 1. Top center critical flight warning banner (Stall or Vacuum)
        if (vacuumBlocked) {
            String warn = "⚠ SEM ATMOSFERA — VOO AERODINÂMICO DESATIVADO (INSTALE VTOL)";
            int bw = font.width(warn) + 14;
            int bx = (sw - bw) / 2;
            g.fill(bx, 14, bx + bw, 30, 0xCC3A0B0B);
            g.renderOutline(bx, 14, bw, 16, 0xFFFF4D4D);
            g.drawCenteredString(font, warn, sw / 2, 18, 0xFFFF8888);
        } else if (stalling) {
            boolean flash = (plane.tickCount / 5) % 2 == 0;
            String warn = "⚠ ESTOL! ACELERE [R/W] OU BAIXE O NARIZ [SHIFT]";
            int bw = font.width(warn) + 14;
            int bx = (sw - bw) / 2;
            g.fill(bx, 14, bx + bw, 30, flash ? 0xDD6A1010 : 0xCC320A0A);
            g.renderOutline(bx, 14, bw, 16, 0xFFFF4040);
            g.drawCenteredString(font, warn, sw / 2, 18, flash ? 0xFFFFFFFF : 0xFFFF9090);
        }

        // 2. Bottom-right NASA Glass-Cockpit Throttle Quadrant & Telemetry Card
        int boxW = 166;
        int boxH = 92;
        int x = sw - boxW - 10;
        int y = sh - boxH - 12;

        g.fill(x, y, x + boxW, y + boxH, 0xCC101620);
        g.fill(x + 1, y + 1, x + boxW - 1, y + 3, hybridVtol ? 0xFF5BF5A0 : 0xFF2ED1FF);
        g.renderOutline(x, y, boxW, boxH, 0xFF475B75);

        // Left vertical Throttle Lever Gauge (0%..100%)
        int tx = x + 10;
        int ty = y + 14;
        int tw = 16;
        int th = 58;
        g.fill(tx, ty, tx + tw, ty + th, 0xFF0A0E14);
        g.renderOutline(tx, ty, tw, th, 0xFF566B85);

        // Detent tick marks (IDLE 0%, CRZ 50%, MAX 100%)
        int midY = ty + th / 2;
        g.fill(tx - 3, ty, tx, ty + 1, 0xFFFF944D);
        g.fill(tx - 3, midY, tx, midY + 1, 0xFF7CE8FF);
        g.fill(tx - 3, ty + th - 1, tx, ty + th, 0xFF6B7C93);

        // Filled throttle bar
        int fillH = Math.round((th - 2) * throttle);
        if (fillH > 0) {
            int barCol = throttle > 0.85F ? 0xFFFF7A33 : (throttle > 0.45F ? 0xFF3BF078 : 0xFF2ED1FF);
            g.fill(tx + 2, ty + th - 1 - fillH, tx + tw - 2, ty + th - 1, barCol);
        }
        // Bright horizontal lever knob indicator
        int knobY = ty + th - 2 - fillH;
        g.fill(tx - 2, knobY - 1, tx + tw + 2, knobY + 2, 0xFFEAF6FF);
        g.drawCenteredString(font, "THR", tx + tw / 2, y + 5, 0xFF9AC2E6);
        g.drawCenteredString(font, Math.round(throttle * 100.0F) + "%", tx + tw / 2, ty + th + 3, 0xFFFFFFFF);

        // Right telemetry stack: IAS vs Vstall, AGL, Gear, Battery, Mode
        int rx = x + 36;
        String modeLabel = hybridVtol ? "HÍBRIDO VTOL" : "ASA FIXA ELÉTR.";
        g.drawString(font, modeLabel, rx, y + 6, hybridVtol ? 0xFF5BF5A0 : 0xFF7CE8FF, false);

        // IAS Airspeed readout + horizontal speed bar with red V_stall marker
        int iasCol = (stallKmh > 0 && iasKmh < stallKmh && !plane.onGround()) ? 0xFFFF5A5A : 0xFFEAF6FF;
        String iasTxt = stallKmh > 0
                ? String.format("IAS %.0f / Estol %.0f km/h", iasKmh, stallKmh)
                : String.format("IAS %.0f km/h (Vácuo)", iasKmh);
        g.drawString(font, iasTxt, rx, y + 18, iasCol, false);

        int sbW = boxW - 44;
        int sbY = y + 29;
        g.fill(rx, sbY, rx + sbW, sbY + 6, 0xFF0A0E14);
        g.renderOutline(rx, sbY, sbW, 6, 0xFF475B75);
        int iasFill = Math.round((sbW - 2) * (float) Mth.clamp(iasKmh / maxKmh, 0.0, 1.0));
        if (iasFill > 0) {
            g.fill(rx + 1, sbY + 1, rx + 1 + iasFill, sbY + 5, stalling ? 0xFFFF4D4D : 0xFF2ED1FF);
        }
        if (stallKmh > 0) {
            int stallX = rx + 1 + Math.round((sbW - 2) * (float) Mth.clamp(stallKmh / maxKmh, 0.0, 1.0));
            g.fill(stallX, sbY - 1, stallX + 1, sbY + 7, 0xFFFF3B3B);
        }

        // Altitude AGL + Gear status + Pitch
        float gear = plane.getGearProgress();
        String gearTxt = gear > 0.85F ? "RECOLH." : (gear < 0.15F ? "BAIXADO" : "MOVENDO");
        int gearCol = gear > 0.85F ? 0xFF5BF5A0 : (gear < 0.15F ? 0xFF7CE8FF : 0xFFFFD84D);
        g.drawString(font, String.format("AGL %.1fm  |  TREM:", agl), rx, y + 40, 0xFFB8CCE0, false);
        g.drawString(font, gearTxt, rx + 82, y + 40, gearCol, false);

        // Battery & Prop RPM
        int batCol = batPct <= 15 ? 0xFFFF5252 : (batPct <= 40 ? 0xFFFFC14D : 0xFF5BF5A0);
        g.drawString(font, String.format("BAT %d%%  |  %d RPM", batPct, Math.round(plane.getRotorRpm())), rx, y + 52, batCol, false);

        // Compact control hints
        g.drawString(font, "[R/↑]+ [C/↓]- [X]0%", rx, y + 66, 0xFF8AA4BF, false);
        g.drawString(font, "[SPC/SHF] Nariz [P] Painel", rx, y + 77, 0xFF7088A3, false);
    }
}
