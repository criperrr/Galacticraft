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

package dev.galacticraft.mod.client.gui.screen.ingame;

import dev.galacticraft.machinelib.client.api.screen.MachineScreen;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.ClientTelescopeController;
import dev.galacticraft.mod.content.block.entity.machine.ObservatoryTelescopeBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ObservatoryTelescopeBlockEntity.ObservatoryTarget;
import dev.galacticraft.mod.network.c2s.ResearchMachineActionPayload;
import dev.galacticraft.mod.research.ClientResearchCache;
import dev.galacticraft.mod.screen.ObservatoryTelescopeMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Redesigned spacious Observatory Control Console ({@code 248x214}) for {@code OBSERVATORY_TELESCOPE}:
 * live Sky Dome Horizon Radar, Aperture &amp; Altitude Airmass telemetry, clearly labeled Primary Optics
 * and Auxiliary Drive upgrade bays, and a full-width {@code ENTER 1ST-PERSON EYEPIECE} launch bar.
 */
@Environment(EnvType.CLIENT)
public class ObservatoryTelescopeScreen extends MachineScreen<ObservatoryTelescopeBlockEntity, ObservatoryTelescopeMenu> {
    private static final int SCREEN_W = 248;
    private static final int SCREEN_H = 214;

    public ObservatoryTelescopeScreen(ObservatoryTelescopeMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.OBSERVATORY_TELESCOPE_SCREEN);
        this.imageWidth = SCREEN_W;
        this.imageHeight = SCREEN_H;
        this.capacitorX = 8;
        this.capacitorY = 24;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 44;
        this.inventoryLabelY = 120;
    }

    @Override
    protected void drawTitle(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font, "OBSERVATORY CONTROL", 8, 6, 0xFFEAF6FF, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;
        long timeMs = System.currentTimeMillis();

        int clarity = this.menu.getOpticalClarityPct();
        int reachTier = this.menu.getMaxReachTier();
        int aperture = this.menu.getApertureTier();
        int optics = this.menu.getOpticsTier();
        int aux = this.menu.getAuxDriveType();
        float airmass = this.menu.getAirmassHundredths() / 100.0F;
        boolean skyClear = this.menu.getSkyClearFlag() != 0;
        boolean nightOrSpace = this.menu.getNightOrSpaceFlag() != 0;
        ObservatoryTarget target = ObservatoryTelescopeBlockEntity.TARGETS.get(
                Math.floorMod(this.menu.getSelectedTargetIndex(), ObservatoryTelescopeBlockEntity.TARGETS.size())
        );

        ResearchWorkbenchScreen.drawChassis(graphics, x, y, SCREEN_W, SCREEN_H);

        // Header Aperture Pill + Clarity LED
        String apBadge = aperture == 3 ? "3x3 CASSEGRAIN" : (aperture == 2 ? "2x2 REFLECTOR" : "1x2 REFRACTOR");
        int apW = this.font.width(apBadge) + 8;
        int apX = x + SCREEN_W - apW - 14;
        graphics.fill(apX, y + 4, apX + apW, y + 16, 0xFF0B2538);
        graphics.renderOutline(apX, y + 4, apW, 12, aperture >= 2 ? 0xFF34D399 : 0xFF38BDF8);
        graphics.drawString(this.font, apBadge, apX + 4, y + 6, aperture >= 2 ? 0xFF68F5B2 : 0xFF7BE8FF, false);
        graphics.fill(x + SCREEN_W - 10, y + 6, x + SCREEN_W - 6, y + 14, !skyClear ? 0xFFFF4242 : (clarity >= 65 ? 0xFF34D399 : 0xFFF59E0B));

        // Left Energy Bay (Capacitor + Charge Slot)
        graphics.fill(x + 7, y + 23, x + 25, y + 73, 0xFF0B111A);
        graphics.renderOutline(x + 7, y + 23, 18, 50, 0xFF2E435A);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 7, y + 77, 0xFF2B3D52);

        // Center-Left Sky Dome & Atmosphere Radar Panel (x+30..146, y+24..96, 116x72)
        int rx = x + 30;
        int ry = y + 24;
        int rw = 116;
        int rh = 72;
        graphics.fill(rx, ry, rx + rw, ry + rh, 0xFF07101B);
        graphics.renderOutline(rx, ry, rw, rh, 0xFF28435C);

        // Circular polar sky dome diagram on left half
        int dcx = rx + 30;
        int dcy = ry + 36;
        drawCircleOutline(graphics, dcx, dcy, 24, 0xFF1C3650);
        drawCircleOutline(graphics, dcx, dcy, 12, 0xFF15293E);
        graphics.fill(dcx - 24, dcy, dcx + 25, dcy + 1, 0x4438BDF8);
        graphics.fill(dcx, dcy - 24, dcx + 1, dcy + 25, 0x4438BDF8);
        graphics.drawString(this.font, "N", dcx - 2, dcy - 31, 0xFF5CE0FF, false);

        // Current physical telescope aim vector dot on sky dome
        float yawRad = (float) Math.toRadians(this.menu.be.getAimYawDeg());
        float zenithFrac = Mth.clamp((90.0F + this.menu.be.getAimPitchDeg()) / 90.0F, 0.05F, 0.95F);
        int tx = dcx + Math.round(Mth.sin(yawRad) * 22.0F * zenithFrac);
        int ty = dcy - Math.round(Mth.cos(yawRad) * 22.0F * zenithFrac);
        graphics.fill(tx - 2, ty - 2, tx + 3, ty + 3, skyClear ? 0xFF34D399 : 0xFFFF5555);

        // Right half of sky dome panel: live optical telemetry
        int infoX = rx + 60;
        graphics.drawString(this.font, "CLR:" + clarity + "%", infoX, ry + 6, clarity >= 65 ? 0xFF34D399 : 0xFFF59E0B, false);
        graphics.drawString(this.font, String.format(Locale.ROOT, "AIR:%.2f", airmass), infoX, ry + 17, 0xFF9AC2D8, false);
        graphics.drawString(this.font, "Y=" + this.menu.be.getBlockPos().getY(), infoX, ry + 28, 0xFF7BE8FF, false);
        int maxZoom = Math.round(ObservatoryTelescopeBlockEntity.maxZoomForAperture(aperture));
        graphics.drawString(this.font, "MAX:" + maxZoom + "x", infoX, ry + 39, 0xFFC084FC, false);
        String skyTxt = !skyClear ? "BLOCKED" : (nightOrSpace ? "SKY OK" : "DAYTIME");
        graphics.drawString(this.font, skyTxt, infoX, ry + 50, !skyClear ? 0xFFFF5555 : (nightOrSpace ? 0xFF34D399 : 0xFFF59E0B), false);
        graphics.drawString(this.font, "ATLAS:" + ClientResearchCache.astroAtlas().size(), infoX, ry + 60, 0xFFEAF6FF, false);

        // Center-Right Dedicated Upgrade & Hardware Panel (x+150..242, y+24..96, 92x72)
        int ux = x + 150;
        int uy = y + 24;
        int uw = 92;
        int uh = 72;
        graphics.fill(ux, uy, ux + uw, uy + uh, 0xFF0C1521);
        graphics.renderOutline(ux, uy, uw, uh, 0xFF28435C);

        // Primary Optics Bay (slot at 220, 32 -> box 219, 31)
        graphics.drawString(this.font, "OPTICS", ux + 5, uy + 8, 0xFF5CE0FF, false);
        String optLabel = optics == 3 ? "LONSDAL." : (optics == 2 ? "SAPPHIRE" : (optics == 1 ? "ACHROMAT" : "STANDARD"));
        graphics.drawString(this.font, optLabel, ux + 5, uy + 18, optics > 0 ? 0xFF34D399 : 0xFF64748B, false);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 219, y + 31, 0xFF38BDF8);
        if (this.menu.itemStorage.slot(ObservatoryTelescopeBlockEntity.OPTICS_SLOT).isEmpty()) {
            graphics.drawString(this.font, "OPT", x + 220, y + 36, 0xFF536B85, false);
        }

        // Auxiliary Drive Bay (slot at 220, 62 -> box 219, 61)
        graphics.fill(ux + 4, uy + 33, ux + uw - 4, uy + 34, 0xFF1D3147);
        graphics.drawString(this.font, "AUX DRIVE", ux + 5, uy + 38, 0xFFC084FC, false);
        String auxLabel = aux == 3 ? "IR BOLOM" : (aux == 2 ? "ADAPTIVE" : (aux == 1 ? "SIDEREAL" : "MANUAL"));
        graphics.drawString(this.font, auxLabel, ux + 5, uy + 48, aux > 0 ? 0xFF68F5B2 : 0xFF64748B, false);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 219, y + 61, 0xFFC084FC);
        if (this.menu.itemStorage.slot(ObservatoryTelescopeBlockEntity.AUX_DRIVE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "AUX", x + 220, y + 66, 0xFF536B85, false);
        }

        // Auto-survey target mini strip at bottom of hardware box
        graphics.drawString(this.font, "AUTO:" + target.shortCode() + " T" + reachTier, ux + 5, uy + 61, 0xFF9AC2D8, false);

        // Full-Width 1st-Person Eyepiece Launch Bar (x+30..242, y+100..118, 212x18)
        int epX = x + 30;
        int epY = y + 100;
        int epW = 212;
        int epH = 18;
        boolean epHov = mouseIn(mouseX, mouseY, epX, epY, epW, epH);
        int epBg = skyClear ? (epHov ? 0xFF185075 : 0xFF0E344E) : 0xFF28191E;
        int epBrd = skyClear ? ((((timeMs / 400L) & 1L) == 0L) ? 0xFF38BDF8 : 0xFF34D399) : 0xFFEF4444;
        graphics.fill(epX, epY, epX + epW, epY + epH, epBg);
        graphics.renderOutline(epX, epY, epW, epH, epBrd);
        String epLbl = skyClear ? "ENTER 1ST-PERSON EYEPIECE (REAL SKY)" : "SKY OBSTRUCTED - CLEAR ROOF ABOVE";
        graphics.drawString(this.font, epLbl, epX + (epW - this.font.width(epLbl)) / 2, epY + 5, skyClear ? 0xFFEAF6FF : 0xFFFF7B7B, true);

        // Player Inventory Grid centered cleanly at (44, 130) -> boxes at (43, 129)
        ResearchWorkbenchScreen.drawPlayerInvGrid(graphics, x + 43, y + 129);

        // Helpful tooltips
        if (mouseIn(mouseX, mouseY, epX, epY, epW, epH)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("First-Person Astronomical Eyepiece").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Scroll = Zoom | Z / C = Focus Ring | Arrows = Fine Pan").withStyle(ChatFormatting.GREEN).getVisualOrderText(),
                    Component.literal("Shift / X / ESC = Exit | Build 2x2 or 3x3 ring around base for 500x!").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ObservatoryTelescopeBlockEntity.OPTICS_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 219, y + 31, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Primary Optics Bay").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Achromat Lens | Sapphire Cassegrain | Lonsdaleite Mirror").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ObservatoryTelescopeBlockEntity.AUX_DRIVE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 219, y + 61, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Auxiliary Drive / Sensor Bay").withStyle(ChatFormatting.LIGHT_PURPLE).getVisualOrderText(),
                    Component.literal("Sidereal Drive | Adaptive Optics | Infrared Bolometer").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        }
    }

    private static void drawCircleOutline(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int deg = 0; deg < 360; deg += 10) {
            double rad = Math.toRadians(deg);
            int px = cx + (int) Math.round(Math.cos(rad) * r);
            int py = cy + (int) Math.round(Math.sin(rad) * r);
            g.fill(px, py, px + 1, py + 1, col);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            if (mouseIn(mx, my, this.leftPos + 30, this.topPos + 100, 212, 18) && this.menu.getSkyClearFlag() != 0) {
                ClientTelescopeController.enterEyepiece(
                        this.menu.be.getBlockPos(),
                        this.menu.getApertureTier(),
                        this.menu.getOpticsTier(),
                        this.menu.getAuxDriveType(),
                        this.menu.getOpticalClarityPct(),
                        this.menu.getAirmassHundredths() / 100.0F
                );
                return true;
            }
            // Clicking sky dome cycles auto-survey target
            if (mouseIn(mx, my, this.leftPos + 30, this.topPos + 24, 116, 72)) {
                ClientPlayNetworking.send(new ResearchMachineActionPayload(this.menu.be.getBlockPos(), ResearchMachineActionPayload.TELESCOPE_CYCLE_TARGET, 1));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
