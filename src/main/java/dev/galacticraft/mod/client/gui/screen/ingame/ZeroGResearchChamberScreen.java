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
import dev.galacticraft.mod.content.block.entity.machine.ZeroGResearchChamberBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ZeroGResearchChamberBlockEntity.ZeroGExperiment;
import dev.galacticraft.mod.network.c2s.ResearchMachineActionPayload;
import dev.galacticraft.mod.screen.ZeroGResearchChamberMenu;
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
 * True-Momentum 2D Magnetic Levitation Confinement Arena ({@code 224x210}) for
 * {@code ZERO_G_RESEARCH_CHAMBER} ({@code <= 0.01G}). Features near-frictionless orbital drift,
 * periodic micro-vibration kicks, repulsive thrust-vectoring clicks, and wall collision penalties.
 */
@Environment(EnvType.CLIENT)
public class ZeroGResearchChamberScreen extends MachineScreen<ZeroGResearchChamberBlockEntity, ZeroGResearchChamberMenu> {
    private static final int SCREEN_W = 224;
    private static final int SCREEN_H = 210;

    private float crystalX = 0.28F;
    private float crystalY = -0.22F;
    private float velX = 0.24F;
    private float velY = -0.19F;
    private float purity = 45.0F;
    private float impulseTimer = 0.0F;
    private long lastTickMs = 0L;

    public ZeroGResearchChamberScreen(ZeroGResearchChamberMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.ZERO_G_RESEARCH_CHAMBER_SCREEN);
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
        graphics.drawString(this.font, "ZERO-G LEVITATION DOME", 8, 6, 0xFFEAF6FF, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;
        long now = System.currentTimeMillis();
        float dt = this.lastTickMs == 0L ? 0.016F : Mth.clamp((now - this.lastTickMs) / 1000.0F, 0.001F, 0.1F);
        this.lastTickMs = now;

        boolean zeroG = this.menu.getMicrogravityValidFlag() != 0;
        float gravG = this.menu.getLocalGravityMilliG() / 1000.0F;
        int dome = this.menu.getDomeTier();
        int conf = this.menu.getConfinementTier();
        int cryo = this.menu.getCryoGimbalTier();
        ZeroGExperiment exp = ZeroGResearchChamberBlockEntity.byId(this.menu.getActiveExperimentId());
        float maxPurity = cryo == 2 ? 125.0F : 100.0F;

        if (zeroG && exp != null) {
            float damp = conf == 2 ? 0.38F : (conf == 1 ? 0.72F : 1.0F);
            float jitter = cryo >= 1 ? 0.22F : 0.75F;
            this.velX += (float) Math.sin(now * 0.0037) * jitter * damp * dt;
            this.velY += (float) Math.cos(now * 0.0031) * jitter * damp * dt;

            // Periodic sudden hull micro-vibration impulse (strongly suppressed by Gyro Gimbal)
            this.impulseTimer += dt;
            if (this.impulseTimer >= 1.25F) {
                this.impulseTimer = 0.0F;
                float kick = (cryo >= 1 ? 0.08F : 0.34F) * damp;
                this.velX += (float) Math.sin(now * 0.13) * kick;
                this.velY += (float) Math.cos(now * 0.17) * kick;
            }

            // Very low friction in microgravity unless superconducting coil installed
            float drag = conf == 2 ? 1.45F : (conf == 1 ? 0.65F : 0.22F);
            this.velX *= Math.max(0.0F, 1.0F - dt * drag);
            this.velY *= Math.max(0.0F, 1.0F - dt * drag);
            this.crystalX += this.velX * dt;
            this.crystalY += this.velY * dt;

            // Wall collision bounces and damages crystal purity!
            if (Math.abs(this.crystalX) > 0.94F) {
                this.crystalX = Mth.clamp(this.crystalX, -0.94F, 0.94F);
                this.velX *= -0.65F;
                this.purity = Math.max(20.0F, this.purity - 8.0F);
            }
            if (Math.abs(this.crystalY) > 0.94F) {
                this.crystalY = Mth.clamp(this.crystalY, -0.94F, 0.94F);
                this.velY *= -0.65F;
                this.purity = Math.max(20.0F, this.purity - 8.0F);
            }

            float r = (float) Math.sqrt(this.crystalX * this.crystalX + this.crystalY * this.crystalY);
            if (r <= 0.32F) {
                this.purity = Math.min(maxPurity, this.purity + dt * 16.0F);
            } else {
                this.purity = Math.max(20.0F, this.purity - dt * 22.0F);
            }
        } else {
            this.crystalX = 0.0F;
            this.crystalY = 0.0F;
            this.velX = 0.20F;
            this.velY = -0.15F;
            this.purity = 45.0F;
        }

        ResearchWorkbenchScreen.drawChassis(graphics, x, y, SCREEN_W, SCREEN_H);

        graphics.fill(x + 7, y + 23, x + 25, y + 73, 0xFF0B111A);
        graphics.renderOutline(x + 7, y + 23, 18, 50, 0xFF2E435A);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 7, y + 77, 0xFF2B3D52);

        ResearchWorkbenchScreen.drawSlot(graphics, x + 31, y + 29, 0xFF38BDF8);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 31, y + 61, 0xFFC084FC);

        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 25, 0xFF34D399);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 53, 0xFF38BDF8);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 77, 0xFF7BE8FF);
        if (this.menu.itemStorage.slot(ZeroGResearchChamberBlockEntity.CONFINEMENT_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "RNG", x + 198, y + 58, 0xFF5A7A8C, false);
        }
        if (this.menu.itemStorage.slot(ZeroGResearchChamberBlockEntity.CRYO_GIMBAL_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "CRY", x + 198, y + 82, 0xFF5A7A8C, false);
        }

        ResearchWorkbenchScreen.drawPlayerInvGrid(graphics, x + 31, y + 125);

        int cx = x + 54;
        int cy = y + 24;
        int cw = 136;
        int ch = 92;
        graphics.fill(cx, cy, cx + cw, cy + ch, 0xFF08101C);
        graphics.renderOutline(cx, cy, cw, ch, zeroG ? 0xFF38BDF8 : 0xFFEF4444);

        if (!zeroG) {
            graphics.drawString(this.font, "GRAVITY LOCKOUT!", cx + 18, cy + 16, 0xFFFF5555, true);
            graphics.drawString(this.font, String.format(Locale.ROOT, "Local: %.3f G", gravG), cx + 28, cy + 34, 0xFFF59E0B, false);
            graphics.drawString(this.font, "Requires <= 0.01G", cx + 20, cy + 48, 0xFF9AC2D8, false);
            graphics.drawString(this.font, "(Orbit / Asteroids)", cx + 16, cy + 62, 0xFF64748B, false);
            return;
        }

        String domeTag = dome == 3 ? "3x3 DOME" : (dome == 2 ? "2x2 DOME" : "1x1 DOME");
        graphics.drawString(this.font, domeTag + " | 0.00G", cx + 4, cy + 3, 0xFF34D399, false);
        String purStr = Math.round(this.purity) + "%";
        graphics.drawString(this.font, purStr, cx + cw - this.font.width(purStr) - 4, cy + 3, this.purity >= 88.0F ? 0xFF68F5B2 : 0xFF7BE8FF, false);

        int ax = cx + 42;
        int ay = cy + 44;
        int ar = 25;
        graphics.renderOutline(ax - ar, ay - ar, ar * 2, ar * 2, 0xFF1F3650);
        int innerR = 8; // Tighter purity ring!
        graphics.renderOutline(ax - innerR, ay - innerR, innerR * 2, innerR * 2, 0xFF34D399);
        graphics.fill(ax - ar, ay, ax + ar, ay + 1, 0x3338BDF8);
        graphics.fill(ax, ay - ar, ax + 1, ay + ar, 0x3338BDF8);

        int kx = ax + Math.round(this.crystalX * (ar - 4));
        int ky = ay + Math.round(this.crystalY * (ar - 4));
        int cCol = exp != null ? exp.crystalColorArgb() : 0xFF5CE0FF;
        graphics.fill(kx - 3, ky - 3, kx + 4, ky + 4, cCol);
        graphics.fill(kx - 1, ky - 1, kx + 2, ky + 2, 0xFFFFFFFF);

        graphics.drawString(this.font, "COIL THRUST", cx + 72, cy + 16, 0xFF9AC2D8, false);
        drawBtn(graphics, cx + 94, cy + 27, 16, 12, "N", mouseX, mouseY);
        drawBtn(graphics, cx + 76, cy + 40, 16, 12, "W", mouseX, mouseY);
        drawBtn(graphics, cx + 112, cy + 40, 16, 12, "E", mouseX, mouseY);
        drawBtn(graphics, cx + 94, cy + 53, 16, 12, "S", mouseX, mouseY);

        boolean ready = exp != null && this.purity >= 88.0F;
        int btnX = cx + 4;
        int btnY = cy + 75;
        int btnW = cw - 8;
        int btnH = 13;
        boolean hov = mouseIn(mouseX, mouseY, btnX, btnY, btnW, btnH);
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, ready ? (hov ? 0xFF15803D : 0xFF166534) : 0xFF1E293B);
        graphics.renderOutline(btnX, btnY, btnW, btnH, ready ? 0xFF34D399 : 0xFF475569);
        String bLbl = exp == null ? "INSERT 0G MATERIALS" : (ready ? "HARVEST (" + Math.round(this.purity) + "%)" : "HOLD IN RING >=88%");
        graphics.drawString(this.font, bLbl, btnX + (btnW - this.font.width(bLbl)) / 2, btnY + 3, ready ? 0xFF68F5B2 : 0xFF94A3B8, false);

        if (this.menu.itemStorage.slot(ZeroGResearchChamberBlockEntity.CONFINEMENT_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 197, y + 53, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Levitation Ring Bay").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("EM Stabilizer Ring | Superconducting Confinement Coil").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ZeroGResearchChamberBlockEntity.CRYO_GIMBAL_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 197, y + 77, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Cryo / Gimbal Bay").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Gyroscopic Gimbal | Helium Dilution Cryostat").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        }
    }

    private void drawBtn(GuiGraphics g, int x, int y, int w, int h, String lbl, int mx, int my) {
        boolean hov = mouseIn(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hov ? 0xFF1F4260 : 0xFF112233);
        g.renderOutline(x, y, w, h, hov ? 0xFF38BDF8 : 0xFF2D4660);
        g.drawString(this.font, lbl, x + (w - this.font.width(lbl)) / 2, y + 2, 0xFFEAF6FF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.getMicrogravityValidFlag() != 0) {
            int cx = this.leftPos + 54;
            int cy = this.topPos + 24;
            if (mouseIn(mouseX, mouseY, cx + 94, cy + 27, 16, 12)) {
                this.velY -= 0.42F;
                return true;
            }
            if (mouseIn(mouseX, mouseY, cx + 94, cy + 53, 16, 12)) {
                this.velY += 0.42F;
                return true;
            }
            if (mouseIn(mouseX, mouseY, cx + 76, cy + 40, 16, 12)) {
                this.velX -= 0.42F;
                return true;
            }
            if (mouseIn(mouseX, mouseY, cx + 112, cy + 40, 16, 12)) {
                this.velX += 0.42F;
                return true;
            }
            // Clicking inside arena fires a repulsive magnetic pulse AWAY from the click point!
            int ax = cx + 42;
            int ay = cy + 44;
            if (mouseIn(mouseX, mouseY, ax - 25, ay - 25, 50, 50)) {
                float clickX = (float) (mouseX - ax) / 21.0F;
                float clickY = (float) (mouseY - ay) / 21.0F;
                float dx = this.crystalX - clickX;
                float dy = this.crystalY - clickY;
                float len = Math.max(0.15F, (float) Math.sqrt(dx * dx + dy * dy));
                this.velX += (dx / len) * 0.44F;
                this.velY += (dy / len) * 0.44F;
                return true;
            }
            if (mouseIn(mouseX, mouseY, cx + 4, cy + 75, 128, 13)
                    && this.menu.getActiveExperimentId() > 0 && this.purity >= 88.0F) {
                ClientPlayNetworking.send(new ResearchMachineActionPayload(
                        this.menu.be.getBlockPos(),
                        ResearchMachineActionPayload.ZEROG_HARVEST_CRYSTAL,
                        Math.round(this.purity)
                ));
                this.purity = 45.0F;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
