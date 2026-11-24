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

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.component.TelemetryData;
import dev.galacticraft.machinelib.client.api.screen.MachineScreen;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.block.entity.machine.TelemetryAnalyzerBlockEntity;
import dev.galacticraft.mod.research.ResearchDomain;
import dev.galacticraft.mod.screen.TelemetryAnalyzerMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Aerospace Flight Telemetry Analyzer HUD ({@code 176x178}):
 * decodes inserted {@code RECORDED_DATA_DRIVE} telemetry payloads, highlights
 * {@code THERMAL_FAILURE (+200%)} black-box recoveries and atmospheric {@code GasComposition},
 * and tracks cross-correlation progress into the 5 Scientific Domains.
 */
@Environment(EnvType.CLIENT)
public class TelemetryAnalyzerScreen extends MachineScreen<TelemetryAnalyzerBlockEntity, TelemetryAnalyzerMenu> {
    private static final int SCREEN_W = 176;
    private static final int SCREEN_H = 178;

    public TelemetryAnalyzerScreen(TelemetryAnalyzerMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.TELEMETRY_ANALYZER_SCREEN);
        this.imageWidth = SCREEN_W;
        this.imageHeight = SCREEN_H;
        this.capacitorX = 8;
        this.capacitorY = 18;
        this.titleLabelX = 32;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 85;
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;

        // Aerospace dark-slate panel
        graphics.fill(x, y, x + SCREEN_W, y + SCREEN_H, 0xFF1A222D);
        graphics.renderOutline(x, y, SCREEN_W, SCREEN_H, 0xFF475B73);
        graphics.renderOutline(x + 1, y + 1, SCREEN_W - 2, SCREEN_H - 2, 0xFF0E141C);

        // Header strip
        graphics.fill(x + 2, y + 2, x + SCREEN_W - 2, y + 16, 0xFF101822);
        graphics.fill(x + 2, y + 16, x + SCREEN_W - 2, y + 17, 0xFF28435C);

        // Energy bar frame + battery slot
        graphics.fill(x + 7, y + 17, x + 25, y + 67, 0xFF0B111A);
        graphics.renderOutline(x + 7, y + 17, 18, 50, 0xFF2E435A);
        drawSlotBox(graphics, x + 7, y + 69);

        // Input slots: Drive (36,26), Sample (36,52), Reagent (62,52), Output (148,39)
        drawSlotBox(graphics, x + 35, y + 25);
        drawSlotBox(graphics, x + 35, y + 51);
        drawSlotBox(graphics, x + 61, y + 51);
        drawSlotBox(graphics, x + 147, y + 38);

        graphics.drawString(this.font, "DRV", x + 36, y + 18, 0xFF5CE0FF, false);
        graphics.drawString(this.font, "SMP", x + 36, y + 70, 0xFF34D399, false);
        graphics.drawString(this.font, "RGT", x + 62, y + 70, 0xFFF59E0B, false);
        graphics.drawString(this.font, "OUT", x + 148, y + 29, 0xFF9AC2D8, false);

        // Telemetry decoder oscilloscope & readout box (x+58..143, y+19..48)
        int dx = x + 58;
        int dy = y + 19;
        int dw = 85;
        int dh = 28;
        graphics.fill(dx, dy, dx + dw, dy + dh, 0xFF08111B);
        graphics.renderOutline(dx, dy, dw, dh, 0xFF28435C);

        ItemStack drive = this.menu.itemStorage.getItem(TelemetryAnalyzerBlockEntity.DATA_DRIVE_SLOT);
        TelemetryData telemetry = drive.get(GCDataComponents.TELEMETRY_DATA);
        if (telemetry != null) {
            int hdrCol = telemetry.isThermalFailureBlackBox() ? 0xFFFACC15 : 0xFF7BE8FF;
            String line1 = this.font.plainSubstrByWidth(telemetry.planetName() + " " + telemetry.qualityPct() + "%", dw - 6);
            String line2 = this.font.plainSubstrByWidth(
                    !telemetry.gasCompositionSummary().isEmpty() ? telemetry.gasCompositionSummary() : telemetry.eventType().displayName(),
                    dw - 6
            );
            graphics.drawString(this.font, line1, dx + 3, dy + 3, hdrCol, false);
            graphics.drawString(this.font, line2, dx + 3, dy + 13, 0xFF9AC2D8, false);
        } else {
            graphics.drawString(this.font, "INSERT DRIVE", dx + 8, dy + 10, 0xFF475569, false);
        }

        // Progress bar (x+84..143, y+55..63)
        int bx = x + 84;
        int by = y + 56;
        int bw = 59;
        graphics.fill(bx, by, bx + bw, by + 6, 0xFF0A121C);
        graphics.renderOutline(bx, by, bw, 6, 0xFF28435C);
        float frac = Mth.clamp((float) this.menu.getProgress() / (float) this.menu.getMaxProgress(), 0.0F, 1.0F);
        int fill = Math.round((bw - 2) * frac);
        if (fill > 0) {
            graphics.fill(bx + 1, by + 1, bx + 1 + fill, by + 5, 0xFF38BDF8);
        }

        // Last yield readout
        if (this.menu.getLastSpAwarded() > 0) {
            ResearchDomain[] doms = ResearchDomain.values();
            ResearchDomain dom = doms[Math.floorMod(this.menu.getLastDomainOrdinal(), doms.length)];
            String yieldTxt = "+" + this.menu.getLastSpAwarded() + "SP +" + this.menu.getLastDomainAwarded() + dom.shortCode();
            graphics.drawString(this.font, this.font.plainSubstrByWidth(yieldTxt, 86), x + 84, y + 66, dom.colorArgb(), false);
        }

        // Player inventory grid backgrounds (8, 96)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(graphics, x + 7 + col * 18, y + 95 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBox(graphics, x + 7 + col * 18, y + 153);
        }
    }

    private static void drawSlotBox(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF0A111A);
        g.renderOutline(x, y, 18, 18, 0xFF2B3D52);
    }
}
