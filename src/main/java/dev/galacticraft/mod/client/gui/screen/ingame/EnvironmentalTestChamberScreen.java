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
import dev.galacticraft.mod.content.block.entity.machine.EnvironmentalTestChamberBlockEntity;
import dev.galacticraft.mod.network.c2s.ResearchMachineActionPayload;
import dev.galacticraft.mod.screen.EnvironmentalTestChamberMenu;
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
 * Inertial 2D Pressure &times; Thermal Stress Control HUD ({@code 224x210}) for
 * {@code ENVIRONMENTAL_TEST_CHAMBER}. Dragging/holding on the pad applies valve &amp; thermal thrust
 * (with real inertia!) to fight multi-frequency planetary surges &amp; Venusian 92 atm gusts.
 */
@Environment(EnvType.CLIENT)
public class EnvironmentalTestChamberScreen extends MachineScreen<EnvironmentalTestChamberBlockEntity, EnvironmentalTestChamberMenu> {
    private static final int SCREEN_W = 224;
    private static final int SCREEN_H = 210;

    private float pressureVal = 25.0F;
    private float thermalVal = 25.0F;
    private float velP = 0.0F;
    private float velT = 0.0F;
    private float steerTargetP = 50.0F;
    private float steerTargetT = 50.0F;
    private float stabilityPct = 0.0F;
    private long lastUpdateMs = 0L;
    private boolean holdingValve = false;

    public EnvironmentalTestChamberScreen(EnvironmentalTestChamberMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.ENVIRONMENTAL_TEST_CHAMBER_SCREEN);
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
        graphics.drawString(this.font, "ENVIRONMENTAL CHAMBER", 8, 6, 0xFFEAF6FF, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;
        long now = System.currentTimeMillis();
        float dt = this.lastUpdateMs == 0L ? 0.016F : Mth.clamp((now - this.lastUpdateMs) / 1000.0F, 0.001F, 0.1F);
        this.lastUpdateMs = now;

        int envMode = this.menu.getEnvModeCode();
        float atm = this.menu.getAmbientPressureMilliAtm() / 1000.0F;
        int tempC = this.menu.getAmbientTempCelsius();
        boolean acid = this.menu.getCorrosiveAcidFlag() != 0;
        int pUp = this.menu.getPressureUpgradeTier();
        int tUp = this.menu.getThermalUpgradeTier();
        boolean hasSample = !this.menu.itemStorage.slot(EnvironmentalTestChamberBlockEntity.TEST_SPECIMEN_SLOT).isEmpty();

        // Inertial physics: strong planetary surges + sudden pressure gusts vs player valve thrust
        if (hasSample) {
            float pTurb = (envMode == EnvironmentalTestChamberBlockEntity.ENV_MODE_VENUS_HYPERBARIC ? 68.0F : 36.0F)
                    * (pUp == 2 ? 0.45F : (pUp == 1 ? 0.72F : 1.0F));
            float tTurb = (acid && tUp < 2 ? 62.0F : 32.0F) * (tUp >= 1 ? 0.50F : 1.0F);
            float surgeP = (float) (Math.sin(now * 0.0031) + 0.65 * Math.sin(now * 0.0079)) * pTurb;
            float surgeT = (float) (Math.cos(now * 0.0027) + 0.65 * Math.cos(now * 0.0067)) * tTurb;
            this.velP += surgeP * dt;
            this.velT += surgeT * dt;

            if (this.holdingValve) {
                float pAuth = pUp >= 1 ? 82.0F : 58.0F;
                float tAuth = tUp >= 1 ? 92.0F : 56.0F;
                this.velP += Mth.clamp(this.steerTargetP - this.pressureVal, -22.0F, 22.0F) * (pAuth / 22.0F) * dt;
                this.velT += Mth.clamp(this.steerTargetT - this.thermalVal, -22.0F, 22.0F) * (tAuth / 22.0F) * dt;
            }
            // Damping & integration
            this.velP *= Math.max(0.0F, 1.0F - dt * 2.1F);
            this.velT *= Math.max(0.0F, 1.0F - dt * 2.1F);
            this.pressureVal = Mth.clamp(this.pressureVal + this.velP * dt, 4.0F, 96.0F);
            this.thermalVal = Mth.clamp(this.thermalVal + this.velT * dt, 4.0F, 96.0F);
        }

        float targetP = envMode == EnvironmentalTestChamberBlockEntity.ENV_MODE_VENUS_HYPERBARIC ? 76.0F
                : (envMode == EnvironmentalTestChamberBlockEntity.ENV_MODE_REAL_VACUUM ? 24.0F : 52.0F);
        float targetT = tempC > 200 ? 74.0F : (tempC < -50 ? 26.0F : 50.0F);
        float halfBand = pUp >= 2 ? 14.0F : 9.0F;
        boolean inWindow = hasSample && Math.abs(this.pressureVal - targetP) <= halfBand && Math.abs(this.thermalVal - targetT) <= halfBand;
        if (hasSample) {
            if (inWindow) {
                this.stabilityPct = Math.min(100.0F, this.stabilityPct + dt * (tUp >= 1 ? 26.0F : 18.0F));
            } else {
                this.stabilityPct = Math.max(0.0F, this.stabilityPct - dt * 28.0F);
            }
        } else {
            this.stabilityPct = 0.0F;
        }

        ResearchWorkbenchScreen.drawChassis(graphics, x, y, SCREEN_W, SCREEN_H);

        graphics.fill(x + 7, y + 23, x + 25, y + 73, 0xFF0B111A);
        graphics.renderOutline(x + 7, y + 23, 18, 50, 0xFF2E435A);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 7, y + 77, 0xFF2B3D52);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 31, y + 47, 0xFF38BDF8);

        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 25, 0xFF34D399);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 53, 0xFF38BDF8);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 77, 0xFFF59E0B);
        if (this.menu.itemStorage.slot(EnvironmentalTestChamberBlockEntity.PRESSURE_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "PRS", x + 198, y + 58, 0xFF5A7A8C, false);
        }
        if (this.menu.itemStorage.slot(EnvironmentalTestChamberBlockEntity.THERMAL_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "THM", x + 198, y + 82, 0xFF5A7A8C, false);
        }

        ResearchWorkbenchScreen.drawPlayerInvGrid(graphics, x + 31, y + 125);

        int cx = x + 54;
        int cy = y + 24;
        int cw = 136;
        int ch = 92;
        graphics.fill(cx, cy, cx + cw, cy + ch, 0xFF0A131E);
        graphics.renderOutline(cx, cy, cw, ch, acid ? 0xFFF59E0B : 0xFF28435C);

        String amb = String.format(Locale.ROOT, "%.1fatm %dC %dGJ/t", atm, tempC, this.menu.getActiveEnergyCost());
        graphics.drawString(this.font, this.font.plainSubstrByWidth(amb, 96), cx + 3, cy + 3, acid ? 0xFFFACC15 : 0xFF7BE8FF, false);
        boolean modeHov = mouseIn(mouseX, mouseY, cx + cw - 34, cy + 2, 31, 10);
        graphics.fill(cx + cw - 34, cy + 2, cx + cw - 3, cy + 12, modeHov ? 0xFF1F3C56 : 0xFF122334);
        graphics.renderOutline(cx + cw - 34, cy + 2, 31, 10, 0xFF38BDF8);
        graphics.drawString(this.font, this.menu.getManualSimOverride() == 1 ? "SIM" : "ENV", cx + cw - 28, cy + 3, 0xFFEAF6FF, false);

        int padX = cx + 4;
        int padY = cy + 15;
        int padW = 92;
        int padH = 57;
        graphics.fill(padX, padY, padX + padW, padY + padH, 0xFF060E17);
        graphics.renderOutline(padX, padY, padW, padH, 0xFF1E3247);

        int gx0 = padX + Math.round(((targetP - halfBand) / 100.0F) * padW);
        int gx1 = padX + Math.round(((targetP + halfBand) / 100.0F) * padW);
        int gy0 = padY + Math.round(((100.0F - (targetT + halfBand)) / 100.0F) * padH);
        int gy1 = padY + Math.round(((100.0F - (targetT - halfBand)) / 100.0F) * padH);
        graphics.fill(gx0, gy0, gx1, gy1, inWindow ? 0x5534D399 : 0x3334D399);
        graphics.renderOutline(gx0, gy0, Math.max(2, gx1 - gx0), Math.max(2, gy1 - gy0), 0xFF34D399);

        // Steer vector crosshair when player holds valve
        if (this.holdingValve) {
            int sx = padX + Math.round((this.steerTargetP / 100.0F) * padW);
            int sy = padY + Math.round(((100.0F - this.steerTargetT) / 100.0F) * padH);
            graphics.fill(sx - 4, sy, sx + 5, sy + 1, 0x8838BDF8);
            graphics.fill(sx, sy - 4, sx + 1, sy + 5, 0x8838BDF8);
        }

        int dx = padX + Math.round((this.pressureVal / 100.0F) * padW);
        int dy = padY + Math.round(((100.0F - this.thermalVal) / 100.0F) * padH);
        graphics.fill(dx - 3, dy - 3, dx + 4, dy + 4, inWindow ? 0xFF68F5B2 : 0xFFFF6B6B);
        graphics.drawString(this.font, "P", padX + padW - 8, padY + padH - 9, 0xFF5A7A8C, false);
        graphics.drawString(this.font, "T", padX + 2, padY + 2, 0xFF5A7A8C, false);

        int rx = cx + 100;
        graphics.drawString(this.font, "STAB", rx, cy + 16, 0xFF9AC2D8, false);
        graphics.drawString(this.font, Math.round(this.stabilityPct) + "%", rx, cy + 26, inWindow ? 0xFF34D399 : 0xFFF59E0B, false);
        graphics.fill(rx, cy + 38, rx + 30, cy + 72, 0xFF07111C);
        graphics.renderOutline(rx, cy + 38, 30, 34, 0xFF28435C);
        int fillH = Math.round((this.stabilityPct / 100.0F) * 32.0F);
        if (fillH > 0) {
            graphics.fill(rx + 1, cy + 71 - fillH, rx + 29, cy + 71, this.stabilityPct >= 90.0F ? 0xFF34D399 : 0xFF38BDF8);
        }

        boolean ready = hasSample && this.stabilityPct >= 90.0F;
        int btnX = cx + 4;
        int btnY = cy + 75;
        int btnW = cw - 8;
        int btnH = 13;
        boolean hov = mouseIn(mouseX, mouseY, btnX, btnY, btnW, btnH);
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, ready ? (hov ? 0xFF15803D : 0xFF166534) : 0xFF1E293B);
        graphics.renderOutline(btnX, btnY, btnW, btnH, ready ? 0xFF34D399 : 0xFF475569);
        String bLbl = !hasSample ? "INSERT TEST SAMPLE" : (ready ? "CERTIFY SAMPLE" : "HOLD VALVE -> 90%");
        graphics.drawString(this.font, bLbl, btnX + (btnW - this.font.width(bLbl)) / 2, btnY + 3, ready ? 0xFF68F5B2 : 0xFF94A3B8, false);

        if (this.menu.itemStorage.slot(EnvironmentalTestChamberBlockEntity.PRESSURE_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 197, y + 53, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Pressure Bay Upgrade").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Turbomolecular Pump | Hyperbaric Piston Manifold").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(EnvironmentalTestChamberBlockEntity.THERMAL_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 197, y + 77, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Thermal Liner Bay").withStyle(ChatFormatting.GOLD).getVisualOrderText(),
                    Component.literal("Regenerative Exchanger | Ablative Acid Liner").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        }
    }

    private void updateSteerTarget(double mouseX, double mouseY) {
        int padX = this.leftPos + 58;
        int padY = this.topPos + 39;
        this.steerTargetP = Mth.clamp((float) ((mouseX - padX) / 92.0) * 100.0F, 5.0F, 95.0F);
        this.steerTargetT = Mth.clamp(100.0F - (float) ((mouseY - padY) / 57.0) * 100.0F, 5.0F, 95.0F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int cx = this.leftPos + 54;
            int cy = this.topPos + 24;
            if (mouseIn(mouseX, mouseY, cx + 102, cy + 2, 31, 10)) {
                ClientPlayNetworking.send(new ResearchMachineActionPayload(
                        this.menu.be.getBlockPos(), ResearchMachineActionPayload.ENV_CHAMBER_CYCLE_MODE, 1
                ));
                return true;
            }
            if (mouseIn(mouseX, mouseY, cx + 4, cy + 15, 92, 57)) {
                this.holdingValve = true;
                updateSteerTarget(mouseX, mouseY);
                return true;
            }
            if (mouseIn(mouseX, mouseY, cx + 4, cy + 75, 128, 13) && this.stabilityPct >= 90.0F) {
                ClientPlayNetworking.send(new ResearchMachineActionPayload(
                        this.menu.be.getBlockPos(),
                        ResearchMachineActionPayload.ENV_CERTIFY_SAMPLE,
                        Math.round(this.stabilityPct)
                ));
                this.stabilityPct = 0.0F;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.holdingValve) {
            updateSteerTarget(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.holdingValve) {
            this.holdingValve = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
