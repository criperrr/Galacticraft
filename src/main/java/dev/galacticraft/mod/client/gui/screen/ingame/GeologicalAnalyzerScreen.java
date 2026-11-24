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
import dev.galacticraft.mod.content.block.entity.machine.GeologicalAnalyzerBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.GeologicalAnalyzerBlockEntity.GeologicalAssay;
import dev.galacticraft.mod.network.c2s.ResearchMachineActionPayload;
import dev.galacticraft.mod.screen.GeologicalAnalyzerMenu;
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

/**
 * Dual-Parameter Laser Mass Spectrometry HUD ({@code 224x210}) for {@code GEOLOGICAL_ANALYZER}.
 * Players must tune BOTH Laser Wavelength ({@code 200-700 nm}) AND Excitation Power ({@code 10-100 mW})
 * without thermally scorching the mineral sample, holding {@code SNR >= 90%} for 1.2s to lock.
 */
@Environment(EnvType.CLIENT)
public class GeologicalAnalyzerScreen extends MachineScreen<GeologicalAnalyzerBlockEntity, GeologicalAnalyzerMenu> {
    private static final int SCREEN_W = 224;
    private static final int SCREEN_H = 210;

    private float tunedWavelengthNm = 520.0F;
    private float laserPowerMw = 45.0F;
    private float lockHoldSec = 0.0F;
    private long lastMs = 0L;
    private int lastAssayId = -1;
    private int draggingControl = 0; // 0=none, 1=wavelength, 2=power

    public GeologicalAnalyzerScreen(GeologicalAnalyzerMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.GEOLOGICAL_ANALYZER_SCREEN);
        this.imageWidth = SCREEN_W;
        this.imageHeight = SCREEN_H;
        this.capacitorX = 8;
        this.capacitorY = 24;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 32;
        this.inventoryLabelY = 116;
    }

    @Override
    protected void drawTitle(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font, "GEOLOGICAL SPECTROMETER", 8, 6, 0xFFEAF6FF, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;
        long now = System.currentTimeMillis();
        float dt = this.lastMs == 0L ? 0.016F : Mth.clamp((now - this.lastMs) / 1000.0F, 0.001F, 0.1F);
        this.lastMs = now;

        int assayId = this.menu.getActiveAssayId();
        GeologicalAssay assay = GeologicalAnalyzerBlockEntity.lookupById(assayId);
        int laserTier = this.menu.getLaserTier();
        int detTier = this.menu.getDetectorTier();
        int boost = this.menu.getReagentBoostPct();
        boolean nativeBonus = this.menu.getNativeInSituBonus() != 0;

        if (assayId != this.lastAssayId) {
            this.lastAssayId = assayId;
            this.tunedWavelengthNm = laserTier >= 2 ? 470.0F : 550.0F;
            this.laserPowerMw = 30.0F;
            this.lockHoldSec = 0.0F;
        }

        ResearchWorkbenchScreen.drawChassis(graphics, x, y, SCREEN_W, SCREEN_H);

        graphics.fill(x + 7, y + 23, x + 25, y + 73, 0xFF0B111A);
        graphics.renderOutline(x + 7, y + 23, 18, 50, 0xFF2E435A);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 7, y + 77, 0xFF2B3D52);

        ResearchWorkbenchScreen.drawSlot(graphics, x + 31, y + 25, 0xFF38BDF8);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 31, y + 51, 0xFF2B3D52);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 31, y + 77, 0xFF2B3D52);

        graphics.fill(x + 173, y + 25, x + 191, y + 75, 0xFF0B111A);
        graphics.renderOutline(x + 173, y + 25, 18, 50, 0xFF34D399);

        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 25, 0xFF34D399);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 53, 0xFFFF6B6B);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 77, 0xFFC084FC);
        if (this.menu.itemStorage.slot(GeologicalAnalyzerBlockEntity.LASER_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "LSR", x + 198, y + 58, 0xFF5A7A8C, false);
        }
        if (this.menu.itemStorage.slot(GeologicalAnalyzerBlockEntity.DETECTOR_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "DET", x + 198, y + 82, 0xFF5A7A8C, false);
        }

        ResearchWorkbenchScreen.drawPlayerInvGrid(graphics, x + 31, y + 125);

        int sx = x + 54;
        int sy = y + 24;
        int sw = 114;
        int sh = 92;
        graphics.fill(sx, sy, sx + sw, sy + sh, 0xFF08121B);
        graphics.renderOutline(sx, sy, sw, sh, 0xFF28435C);

        if (assay == null) {
            graphics.drawString(this.font, "INSERT SPECIMEN", sx + 14, sy + 24, 0xFF94A3B8, false);
            graphics.drawString(this.font, "Match Laser nm", sx + 16, sy + 40, 0xFF5CE0FF, false);
            graphics.drawString(this.font, "& Power (mW)!", sx + 18, sy + 52, 0xFFF59E0B, false);
            return;
        }

        float minNm = laserTier >= 2 ? 200.0F : 360.0F;
        float maxNm = 700.0F;
        this.tunedWavelengthNm = Mth.clamp(this.tunedWavelengthNm, minNm, maxNm);

        int peakNm = assay.peakWavelengthNm();
        int idealPower = idealPowerFor(assay);
        boolean scorched = this.laserPowerMw > idealPower + 18.0F;
        int snr = computeSnr(peakNm, idealPower, laserTier, detTier, boost);
        if (snr >= 90) {
            this.lockHoldSec = Math.min(1.2F, this.lockHoldSec + dt);
        } else {
            this.lockHoldSec = Math.max(0.0F, this.lockHoldSec - dt * 2.0F);
        }
        boolean ready = this.lockHoldSec >= 1.15F;

        graphics.drawString(this.font, this.font.plainSubstrByWidth(assay.specimenLabel(), 64), sx + 3, sy + 3, 0xFFEAF6FF, false);
        String snrTxt = scorched ? "SCORCH!" : ("SNR:" + snr + "%");
        graphics.drawString(this.font, snrTxt, sx + sw - this.font.width(snrTxt) - 3, sy + 3, scorched ? 0xFFFF5555 : (snr >= 90 ? 0xFF34D399 : 0xFFF59E0B), false);

        // Spectral CRT (sx+4..sx+110, sy+14..sy+44, h=30)
        int px = sx + 4;
        int py = sy + 14;
        int pw = sw - 8;
        int ph = 30;
        graphics.fill(px, py, px + pw, py + ph, 0xFF050B12);

        if (detTier == 1) {
            float normTarget = Mth.clamp((peakNm - minNm) / (maxNm - minNm), 0.0F, 1.0F);
            int hx = px + Math.round(normTarget * pw);
            graphics.fill(Math.max(px, hx - 7), py, Math.min(px + pw, hx + 7), py + ph, 0x2238BDF8);
        }

        float powerFactor = scorched ? 0.35F : Mth.clamp(1.0F - Math.abs(this.laserPowerMw - idealPower) / 48.0F, 0.15F, 1.0F);
        float noiseAmp = detTier >= 1 ? 2.5F : 8.0F;
        for (int i = 0; i < pw; i += 2) {
            float nm = minNm + (i / (float) pw) * (maxNm - minNm);
            float distPeak = Math.abs(nm - peakNm);
            float reveal = (float) Math.exp(-(Math.pow(this.tunedWavelengthNm - peakNm, 2.0) / 1600.0));
            float peakHeight = (float) Math.exp(-(distPeak * distPeak) / 85.0) * (0.14F + 0.86F * reveal) * powerFactor;
            float noise = (float) (Math.sin(i * 2.1 + now * 0.015) * 0.5 + 0.5) * (noiseAmp / ph);
            float normH = Mth.clamp(peakHeight + noise, 0.05F, 0.96F);
            int barH = Math.round(normH * (ph - 3));
            int col = scorched ? 0xFFFF5555 : (Math.abs(nm - this.tunedWavelengthNm) < 10.0F ? (snr >= 90 ? 0xFF34D399 : wavelengthToColor(nm)) : 0xFF285878);
            graphics.fill(px + i, py + ph - barH, px + i + 2, py + ph, col);
        }

        float lamFrac = (this.tunedWavelengthNm - minNm) / (maxNm - minNm);
        int cx = px + Math.round(lamFrac * (pw - 2));
        graphics.fill(cx, py, cx + 1, py + ph, 0xFFFFFFFF);

        // Slider 1: Wavelength nm (sy+47..56)
        graphics.drawString(this.font, Math.round(this.tunedWavelengthNm) + "nm", px, sy + 46, 0xFF7BE8FF, false);
        graphics.fill(px + 34, sy + 47, px + pw, sy + 53, 0xFF102030);
        int lamKnob = px + 34 + Math.round(lamFrac * (pw - 38));
        graphics.fill(lamKnob, sy + 46, lamKnob + 4, sy + 54, wavelengthToColor(this.tunedWavelengthNm));

        // Slider 2: Laser Power mW (sy+60..69)
        graphics.drawString(this.font, Math.round(this.laserPowerMw) + "mW", px, sy + 59, scorched ? 0xFFFF5555 : 0xFFF59E0B, false);
        graphics.fill(px + 34, sy + 60, px + pw, sy + 66, 0xFF102030);
        float powFrac = (this.laserPowerMw - 10.0F) / 90.0F;
        int powKnob = px + 34 + Math.round(powFrac * (pw - 38));
        graphics.fill(powKnob, sy + 59, powKnob + 4, sy + 67, scorched ? 0xFFFF5555 : 0xFFF59E0B);

        // Lock progress + Confirm Button (sy+73..87)
        int btnX = sx + 4;
        int btnY = sy + 74;
        int btnW = sw - 8;
        int btnH = 14;
        boolean hov = mouseIn(mouseX, mouseY, btnX, btnY, btnW, btnH);
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, ready ? (hov ? 0xFF15803D : 0xFF166534) : 0xFF1E293B);
        if (!ready && this.lockHoldSec > 0.02F) {
            int holdW = Math.round((this.lockHoldSec / 1.2F) * btnW);
            graphics.fill(btnX, btnY, btnX + holdW, btnY + btnH, 0x5534D399);
        }
        graphics.renderOutline(btnX, btnY, btnW, btnH, ready ? 0xFF34D399 : 0xFF475569);
        String bLbl = ready ? (nativeBonus ? "LOCK (+50% NAT)" : "LOCK SPECTRUM")
                : (peakNm < 360 && laserTier < 2 ? "NEED UV LASER" : (snr >= 90 ? "HOLDING LOCK..." : "TUNE nm + mW"));
        graphics.drawString(this.font, bLbl, btnX + (btnW - this.font.width(bLbl)) / 2, btnY + 3, ready ? 0xFF68F5B2 : 0xFF94A3B8, false);

        if (this.menu.itemStorage.slot(GeologicalAnalyzerBlockEntity.LASER_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 197, y + 53, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Laser Emitter Bay").withStyle(ChatFormatting.RED).getVisualOrderText(),
                    Component.literal("Tunable Diode Laser | UV Excimer Laser (200-400nm)").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(GeologicalAnalyzerBlockEntity.DETECTOR_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 197, y + 77, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Mass Detector Bay").withStyle(ChatFormatting.LIGHT_PURPLE).getVisualOrderText(),
                    Component.literal("Diffraction Grating Prism | Quadrupole Mass Filter").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        }
    }

    private static int idealPowerFor(GeologicalAssay assay) {
        // Fragile ices & flora need gentle excitation; refractory ores & diamond need high power
        int id = assay.id();
        if (id == 7 || id == 8 || id == 12) return 32;
        if (id == 5 || id == 15 || id == 25 || id == 27) return 82;
        return 38 + ((id * 19) % 48);
    }

    private int computeSnr(int peakNm, int idealPower, int laserTier, int detTier, int boostPct) {
        float lamDiff = Math.abs(this.tunedWavelengthNm - peakNm);
        float lamSigma = laserTier >= 1 ? 15.0F : 11.0F;
        float lamMatch = (float) Math.exp(-(lamDiff * lamDiff) / (2.0F * lamSigma * lamSigma));

        float powDiff = this.laserPowerMw - idealPower;
        float powMatch = powDiff > 18.0F ? 0.25F : (float) Math.exp(-(powDiff * powDiff) / (2.0F * 14.0F * 14.0F));

        int base = Math.round(lamMatch * powMatch * 94.0F) + (detTier >= 1 ? 6 : 0) + (boostPct >= 140 ? 4 : 0);
        return Mth.clamp(base, 0, 100);
    }

    private static int wavelengthToColor(float nm) {
        if (nm < 380.0F) return 0xFFC084FC;
        if (nm < 450.0F) return 0xFF60A5FA;
        if (nm < 510.0F) return 0xFF38BDF8;
        if (nm < 575.0F) return 0xFF34D399;
        if (nm < 610.0F) return 0xFFFACC15;
        return 0xFFFF6B6B;
    }

    private void updateFromMouse(double mouseX) {
        int barX = this.leftPos + 58 + 34;
        int barW = 106 - 34;
        float frac = Mth.clamp((float) ((mouseX - barX) / barW), 0.0F, 1.0F);
        if (this.draggingControl == 1) {
            float minNm = this.menu.getLaserTier() >= 2 ? 200.0F : 360.0F;
            this.tunedWavelengthNm = minNm + frac * (700.0F - minNm);
        } else if (this.draggingControl == 2) {
            this.laserPowerMw = 10.0F + frac * 90.0F;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int sx = this.leftPos + 54;
            int sy = this.topPos + 24;
            if (mouseIn(mouseX, mouseY, sx + 34, sy + 44, 76, 12)) {
                this.draggingControl = 1;
                updateFromMouse(mouseX);
                return true;
            }
            if (mouseIn(mouseX, mouseY, sx + 34, sy + 57, 76, 12)) {
                this.draggingControl = 2;
                updateFromMouse(mouseX);
                return true;
            }
            GeologicalAssay assay = GeologicalAnalyzerBlockEntity.lookupById(this.menu.getActiveAssayId());
            if (assay != null && mouseIn(mouseX, mouseY, sx + 4, sy + 74, 106, 14) && this.lockHoldSec >= 1.15F) {
                int snr = computeSnr(assay.peakWavelengthNm(), idealPowerFor(assay), this.menu.getLaserTier(), this.menu.getDetectorTier(), this.menu.getReagentBoostPct());
                ClientPlayNetworking.send(new ResearchMachineActionPayload(
                        this.menu.be.getBlockPos(),
                        ResearchMachineActionPayload.GEO_CONFIRM_SPECTRUM,
                        snr
                ));
                this.lockHoldSec = 0.0F;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingControl != 0) {
            updateFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingControl != 0) {
            this.draggingControl = 0;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
