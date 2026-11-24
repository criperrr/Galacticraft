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
import dev.galacticraft.mod.content.block.entity.machine.ResearchWorkbenchBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ResearchWorkbenchBlockEntity.PrototypeExperiment;
import dev.galacticraft.mod.network.c2s.ResearchMachineActionPayload;
import dev.galacticraft.mod.screen.ResearchWorkbenchMenu;
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
 * Challenging 3-Axis Coupled Blueprint Tolerance Calibration HUD ({@code 224x210}) for {@code RESEARCH_WORKBENCH} (0 GJ/t).
 * Without {@code OPTICAL_BLUEPRINT_PROJECTOR}, target bands are hidden and must be solved using live directional stress
 * indicators ({@code < / > / OK}) and structural resonance ({@code FIT >= 88%}) against strong cross-axis coupling.
 */
@Environment(EnvType.CLIENT)
public class ResearchWorkbenchScreen extends MachineScreen<ResearchWorkbenchBlockEntity, ResearchWorkbenchMenu> {
    private static final int SCREEN_W = 224;
    private static final int SCREEN_H = 210;

    private float rigidity = 20.0F;
    private float conductivity = 80.0F;
    private float thermal = 22.0F;
    private int lastExpId = -1;
    private int draggingSlider = -1;

    public ResearchWorkbenchScreen(ResearchWorkbenchMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.RESEARCH_WORKBENCH_SCREEN);
        this.imageWidth = SCREEN_W;
        this.imageHeight = SCREEN_H;
        this.capacitorHeight = 0;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 32;
        this.inventoryLabelY = 116;
    }

    @Override
    protected void drawTitle(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font, "RESEARCH WORKBENCH", 8, 6, 0xFFEAF6FF, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;
        int expId = this.menu.getActiveExperimentId();
        PrototypeExperiment exp = ResearchWorkbenchBlockEntity.byId(expId);
        int metrology = this.menu.getMetrologyTier();
        boolean hasStylus = this.menu.getHasStylusFlag() != 0;

        if (expId != this.lastExpId) {
            this.lastExpId = expId;
            this.rigidity = 16.0F + ((expId * 23) % 24);
            this.conductivity = 84.0F - ((expId * 17) % 24);
            this.thermal = 18.0F + ((expId * 29) % 24);
        }

        drawChassis(graphics, x, y, SCREEN_W, SCREEN_H);
        graphics.drawString(this.font, "0 GJ/t", x + SCREEN_W - 42, y + 6, 0xFF34D399, false);

        drawSlot(graphics, x + 11, y + 25, 0xFF2B3D52);
        drawSlot(graphics, x + 11, y + 47, 0xFF2B3D52);
        drawSlot(graphics, x + 11, y + 69, 0xFF2B3D52);
        if (this.menu.itemStorage.slot(ResearchWorkbenchBlockEntity.SPECIMEN_SLOT).isEmpty()) {
            graphics.drawString(this.font, "ORE", x + 12, y + 30, 0xFF536B85, false);
        }
        if (this.menu.itemStorage.slot(ResearchWorkbenchBlockEntity.CATALYST_SLOT).isEmpty()) {
            graphics.drawString(this.font, "CAT", x + 12, y + 52, 0xFF536B85, false);
        }
        if (this.menu.itemStorage.slot(ResearchWorkbenchBlockEntity.PAPER_SLOT).isEmpty()) {
            graphics.drawString(this.font, "PAP", x + 12, y + 74, 0xFF536B85, false);
        }

        drawSlot(graphics, x + 195, y + 25, 0xFF34D399);
        drawSlot(graphics, x + 195, y + 53, 0xFF38BDF8);
        drawSlot(graphics, x + 195, y + 75, 0xFFC084FC);
        if (this.menu.itemStorage.slot(ResearchWorkbenchBlockEntity.METROLOGY_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "MET", x + 196, y + 58, 0xFF5A7A8C, false);
        }
        if (this.menu.itemStorage.slot(ResearchWorkbenchBlockEntity.STYLUS_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "CAD", x + 196, y + 80, 0xFF5A7A8C, false);
        }

        drawPlayerInvGrid(graphics, x + 31, y + 125);

        int bx = x + 36;
        int by = y + 24;
        int bw = 152;
        int bh = 92;
        graphics.fill(bx, by, bx + bw, by + bh, 0xFF0A1D33);
        graphics.renderOutline(bx, by, bw, bh, 0xFF38BDF8);
        for (int gx = bx + 12; gx < bx + bw; gx += 12) {
            graphics.fill(gx, by + 1, gx + 1, by + bh - 1, 0xFF0F2947);
        }
        for (int gy = by + 12; gy < by + bh; gy += 12) {
            graphics.fill(bx + 1, gy, bx + bw - 1, gy + 1, 0xFF0F2947);
        }

        if (exp == null) {
            graphics.drawString(this.font, "DRAFTING BOARD IDLE", bx + 20, by + 18, 0xFF7BE8FF, false);
            graphics.drawString(this.font, "Insert Raw Ore + Catalyst", bx + 10, by + 36, 0xFF9AC2D8, false);
            graphics.drawString(this.font, "+ Paper to solve coupled", bx + 12, by + 48, 0xFF9AC2D8, false);
            graphics.drawString(this.font, "engineering tolerances.", bx + 14, by + 60, 0xFF9AC2D8, false);
            return;
        }

        int tol = metrology >= 1 ? 11 : 7;
        boolean showBands = metrology >= 2;
        int acc = computeAccuracy(exp, tol);
        boolean locked = acc >= 88;

        graphics.drawString(this.font, this.font.plainSubstrByWidth(exp.shortName(), 88), bx + 4, by + 4, 0xFFEAF6FF, false);
        String accStr = "FIT:" + acc + "%";
        graphics.drawString(this.font, accStr, bx + bw - this.font.width(accStr) - 4, by + 4, locked ? 0xFF34D399 : 0xFFF59E0B, false);

        drawParamSlider(graphics, bx + 5, by + 18, bw - 10, "RIG", this.rigidity, exp.targetRigidity(), tol, showBands);
        drawParamSlider(graphics, bx + 5, by + 37, bw - 10, "CND", this.conductivity, exp.targetConductivity(), tol, showBands);
        drawParamSlider(graphics, bx + 5, by + 56, bw - 10, "THM", this.thermal, exp.targetThermal(), tol, showBands);

        int btnX = bx + 6;
        int btnY = by + 74;
        int btnW = bw - 12;
        int btnH = 14;
        boolean hov = mouseIn(mouseX, mouseY, btnX, btnY, btnW, btnH);
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, locked ? (hov ? 0xFF15803D : 0xFF166534) : 0xFF1E293B);
        graphics.renderOutline(btnX, btnY, btnW, btnH, locked ? 0xFF34D399 : 0xFF475569);
        String lbl = locked ? "SEAL BLUEPRINT (" + acc + "%)" : (hasStylus ? "SOLVE TO >=88% (CAD)" : "BALANCE 3 AXES >=88%");
        graphics.drawString(this.font, lbl, btnX + (btnW - this.font.width(lbl)) / 2, btnY + 3, locked ? 0xFF68F5B2 : 0xFF94A3B8, false);

        if (this.menu.itemStorage.slot(ResearchWorkbenchBlockEntity.METROLOGY_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 195, y + 53, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Metrology Tool Bay").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Vernier Caliper (+tol) | Blueprint Projector (reveals target bands)").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ResearchWorkbenchBlockEntity.STYLUS_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 195, y + 75, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Calibration Arm Bay").withStyle(ChatFormatting.LIGHT_PURPLE).getVisualOrderText(),
                    Component.literal("CAD Holographic Stylus (-58% cross-axis coupling)").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        }
    }

    private void drawParamSlider(GuiGraphics g, int x, int y, int w, String label, float val, int target, int tol, boolean revealBand) {
        g.drawString(this.font, label, x, y, 0xFF9AC2D8, false);
        int tx = x + 26;
        int tw = w - 44;
        g.fill(tx, y + 1, tx + tw, y + 8, 0xFF06101B);
        g.renderOutline(tx, y + 1, tw, 7, 0xFF28435C);

        float diff = target - val;
        boolean inZone = Math.abs(diff) <= tol;
        if (revealBand) {
            int z0 = tx + Math.round(Mth.clamp((target - tol) / 100.0F, 0.0F, 1.0F) * tw);
            int z1 = tx + Math.round(Mth.clamp((target + tol) / 100.0F, 0.0F, 1.0F) * tw);
            g.fill(z0, y + 2, z1, y + 7, 0x5534D399);
            int gx = tx + Math.round((target / 100.0F) * tw);
            g.fill(gx, y + 1, gx + 1, y + 8, 0xFF68F5B2);
        }

        int kx = tx + Math.round((val / 100.0F) * (tw - 4));
        g.fill(kx, y, kx + 4, y + 9, inZone ? 0xFF34D399 : 0xFF38BDF8);

        // Directional stress cue on right (OK / >> / <<)
        String cue = inZone ? "OK" : (diff > 0 ? ">>" : "<<");
        int cueCol = inZone ? 0xFF34D399 : (Math.abs(diff) < 18 ? 0xFFF59E0B : 0xFFFF6B6B);
        g.drawString(this.font, cue, tx + tw + 4, y, cueCol, false);
    }

    private int computeAccuracy(PrototypeExperiment exp, int tol) {
        float d1 = Math.max(0.0F, Math.abs(this.rigidity - exp.targetRigidity()) - tol * 0.25F);
        float d2 = Math.max(0.0F, Math.abs(this.conductivity - exp.targetConductivity()) - tol * 0.25F);
        float d3 = Math.max(0.0F, Math.abs(this.thermal - exp.targetThermal()) - tol * 0.25F);
        float avgErr = (d1 + d2 + d3) / 3.0F;
        return Mth.clamp(Math.round(100.0F - avgErr * 2.8F), 0, 100);
    }

    private void updateSliderFromMouse(int sliderIdx, double mouseX) {
        int tx = this.leftPos + 36 + 5 + 26;
        int tw = (152 - 10) - 44;
        float nextVal = Mth.clamp((float) ((mouseX - tx) / tw) * 100.0F, 0.0F, 100.0F);
        float coupling = this.menu.getHasStylusFlag() != 0 ? 0.20F : 0.48F;
        if (sliderIdx == 0) {
            float delta = nextVal - this.rigidity;
            this.rigidity = nextVal;
            this.conductivity = Mth.clamp(this.conductivity - delta * coupling, 0.0F, 100.0F);
            this.thermal = Mth.clamp(this.thermal + delta * coupling * 0.45F, 0.0F, 100.0F);
        } else if (sliderIdx == 1) {
            float delta = nextVal - this.conductivity;
            this.conductivity = nextVal;
            this.thermal = Mth.clamp(this.thermal + delta * coupling, 0.0F, 100.0F);
            this.rigidity = Mth.clamp(this.rigidity - delta * coupling * 0.45F, 0.0F, 100.0F);
        } else if (sliderIdx == 2) {
            float delta = nextVal - this.thermal;
            this.thermal = nextVal;
            this.rigidity = Mth.clamp(this.rigidity - delta * coupling * 0.85F, 0.0F, 100.0F);
            this.conductivity = Mth.clamp(this.conductivity + delta * coupling * 0.35F, 0.0F, 100.0F);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int bx = this.leftPos + 36;
            int by = this.topPos + 24;
            int tx = bx + 31;
            int tw = 152 - 54;
            for (int i = 0; i < 3; i++) {
                int sy = by + 16 + i * 19;
                if (mouseIn(mouseX, mouseY, tx - 4, sy, tw + 8, 13)) {
                    this.draggingSlider = i;
                    updateSliderFromMouse(i, mouseX);
                    return true;
                }
            }
            PrototypeExperiment exp = ResearchWorkbenchBlockEntity.byId(this.menu.getActiveExperimentId());
            if (exp != null && mouseIn(mouseX, mouseY, bx + 6, by + 74, 140, 14)) {
                int tol = this.menu.getMetrologyTier() >= 1 ? 11 : 7;
                int acc = computeAccuracy(exp, tol);
                if (acc >= 88) {
                    ClientPlayNetworking.send(new ResearchMachineActionPayload(
                            this.menu.be.getBlockPos(),
                            ResearchMachineActionPayload.WORKBENCH_SEAL_PROTOTYPE,
                            acc
                    ));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingSlider >= 0) {
            updateSliderFromMouse(this.draggingSlider, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingSlider >= 0) {
            this.draggingSlider = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    static void drawChassis(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF1A222D);
        g.renderOutline(x, y, w, h, 0xFF475B73);
        g.renderOutline(x + 1, y + 1, w - 2, h - 2, 0xFF0E141C);
        g.fill(x + 2, y + 2, x + w - 2, y + 19, 0xFF101822);
        g.fill(x + 2, y + 19, x + w - 2, y + 20, 0xFF28435C);
    }

    static void drawSlot(GuiGraphics g, int x, int y, int border) {
        g.fill(x, y, x + 18, y + 18, 0xFF0A111A);
        g.renderOutline(x, y, 18, 18, border);
    }

    static void drawPlayerInvGrid(GuiGraphics g, int startX, int startY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, startX + col * 18, startY + row * 18, 0xFF2B3D52);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(g, startX + col * 18, startY + 58, 0xFF2B3D52);
        }
    }
}
