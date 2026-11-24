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

import dev.galacticraft.machinelib.api.util.ItemStackUtil;
import dev.galacticraft.machinelib.client.api.screen.MachineScreen;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.block.entity.machine.AeroRefurbishmentBenchBlockEntity;
import dev.galacticraft.mod.screen.AeroRefurbishmentBenchMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Environment(EnvType.CLIENT)
public class AeroRefurbishmentBenchScreen extends MachineScreen<AeroRefurbishmentBenchBlockEntity, AeroRefurbishmentBenchMenu> {
    public AeroRefurbishmentBenchScreen(AeroRefurbishmentBenchMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.AERO_REFURBISHMENT_BENCH_SCREEN);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.capacitorX = 8;
        this.capacitorY = 22;
        this.titleLabelY = 6;
        this.inventoryLabelY = 112;
    }

    @Override
    protected void drawTitle(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFEAF6FF, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        long timeMs = System.currentTimeMillis();
        int mode = this.menu.getOperationMode();
        int progress = this.menu.getProgress();
        int maxProgress = this.menu.getMaxProgress();
        int integrityPct = Mth.clamp(this.menu.getInputIntegrityPct(), 0, 100);
        int coatingId = this.menu.getActiveCoatingId();
        boolean working = mode != AeroRefurbishmentBenchBlockEntity.MODE_IDLE && progress > 0;
        boolean hasPart = !this.menu.itemStorage.slot(AeroRefurbishmentBenchBlockEntity.PART_INPUT_SLOT).isEmpty();
        boolean hasMat = !this.menu.itemStorage.slot(AeroRefurbishmentBenchBlockEntity.MATERIAL_INPUT_SLOT).isEmpty();

        // Right-header telemetry LED (matches Sabatier / Electrolytic aerospace HUD)
        int ledColor = working
                ? (((timeMs / 240L) & 1L) == 0L ? 0xFF42F5C8 : 0xFF38BDF8)
                : (mode != AeroRefurbishmentBenchBlockEntity.MODE_IDLE ? 0xFFF5B041 : 0xFF4A5568);
        graphics.fill(this.leftPos + 166, this.topPos + 7, this.leftPos + 169, this.topPos + 14, ledColor);

        // Integrity bar below Part Input Slot (x=31..59, y=49..55)
        int intBarX = this.leftPos + 31;
        int intBarY = this.topPos + 49;
        int intBarW = 28;
        int intBarH = 6;
        int intColor = !hasPart ? 0xFF334155
                : (integrityPct >= 80 ? 0xFF34D399 : (integrityPct >= 50 ? 0xFFFACC15 : 0xFFF87171));
        int intFill = hasPart ? Mth.clamp((int) (intBarW * (integrityPct / 100.0F)), 0, intBarW) : 0;
        if (intFill > 0) {
            graphics.fill(intBarX, intBarY, intBarX + intFill, intBarY + intBarH, intColor);
        }
        String intReadout = hasPart ? (integrityPct + "%") : "--";
        graphics.drawString(
                this.font,
                intReadout,
                this.leftPos + 45 - this.font.width(intReadout) / 2,
                this.topPos + 58,
                hasPart ? (integrityPct >= 80 ? 0xFF1F6E4A : 0xFF9A3412) : 0xFF556070,
                false
        );

        // Subtle ghost labels inside empty input slots
        if (!hasPart) {
            graphics.drawString(this.font, "PRT", this.leftPos + 37, this.topPos + 30, 0xFF4B627D, false);
        }
        if (!hasMat) {
            graphics.drawString(this.font, "MAT", this.leftPos + 37, this.topPos + 80, 0xFF7D6B4B, false);
        }

        // Output readiness pill above Output Slot (x=136..164, y=24..34)
        String outTag = working ? (Math.round(((float) progress / (float) maxProgress) * 100.0F) + "%")
                : (mode != AeroRefurbishmentBenchBlockEntity.MODE_IDLE ? "OK" : "OUT");
        int outTagColor = working ? 0xFF42F5C8
                : (mode != AeroRefurbishmentBenchBlockEntity.MODE_IDLE ? 0xFF4ADE80 : 0xFF6B7B90);
        graphics.drawString(
                this.font,
                outTag,
                this.leftPos + 150 - this.font.width(outTag) / 2,
                this.topPos + 26,
                outTagColor,
                false
        );

        // Central Laser Refurbishment Chamber OLED (x=74..122, y=24..72)
        int oledX = this.leftPos + 74;
        int oledY = this.topPos + 24;
        int oledW = 48;
        int oledH = 48;

        // Top mode caption inside OLED
        String modeCap = switch (mode) {
            case AeroRefurbishmentBenchBlockEntity.MODE_REPAIR -> "REPAIR";
            case AeroRefurbishmentBenchBlockEntity.MODE_COAT -> "COATING";
            case AeroRefurbishmentBenchBlockEntity.MODE_REPAIR_AND_COAT -> "FIX+COAT";
            default -> hasPart ? "NOMINAL" : "STANDBY";
        };
        int modeCapColor = switch (mode) {
            case AeroRefurbishmentBenchBlockEntity.MODE_REPAIR -> 0xFF4ADE80;
            case AeroRefurbishmentBenchBlockEntity.MODE_COAT -> 0xFF38BDF8;
            case AeroRefurbishmentBenchBlockEntity.MODE_REPAIR_AND_COAT -> 0xFF5EEAD4;
            default -> 0xFF64748B;
        };
        graphics.drawString(this.font, modeCap, oledX + oledW / 2 - this.font.width(modeCap) / 2, oledY + 3, modeCapColor, false);

        // Preview icon of the part inside the refurbishment bay
        if (hasPart) {
            ItemStack partPreview = ItemStackUtil.create(this.menu.itemStorage.slot(AeroRefurbishmentBenchBlockEntity.PART_INPUT_SLOT));
            if (!partPreview.isEmpty()) {
                graphics.renderItem(partPreview, oledX + 16, oledY + 17);
            }
        } else {
            // Subtle empty reticle inside OLED
            graphics.renderOutline(oledX + 16, oledY + 17, 16, 16, 0xFF283A52);
        }

        // Animated laser scanning beam sweeping vertically across the part while working
        if (working) {
            int scanRange = 20;
            int phase = (int) ((timeMs / 45L) % (scanRange * 2L));
            int scanOff = phase < scanRange ? phase : (scanRange * 2 - phase);
            int beamY = oledY + 15 + scanOff;
            int beamColor = mode == AeroRefurbishmentBenchBlockEntity.MODE_COAT ? 0xCC38BDF8 : 0xCC4ADE80;
            int coreColor = mode == AeroRefurbishmentBenchBlockEntity.MODE_COAT ? 0xFFE0F7FF : 0xFFD1FAE5;
            graphics.fill(oledX + 6, beamY, oledX + oledW - 6, beamY + 2, beamColor);
            graphics.fill(oledX + 10, beamY, oledX + oledW - 10, beamY + 1, coreColor);
        }

        // Bottom progress bar inside OLED
        int pBarX = oledX + 4;
        int pBarY = oledY + oledH - 7;
        int pBarW = oledW - 8;
        graphics.fill(pBarX, pBarY, pBarX + pBarW, pBarY + 3, 0xFF0A1018);
        int pFill = Mth.clamp((int) ((float) progress / (float) maxProgress * pBarW), 0, pBarW);
        if (pFill > 0) {
            int fillCol = mode == AeroRefurbishmentBenchBlockEntity.MODE_COAT ? 0xFF38BDF8 : 0xFF42F5C8;
            graphics.fill(pBarX, pBarY, pBarX + pFill, pBarY + 3, fillCol);
        }

        // Coating Indicator Strip (x=73..164, y=77..92) — lights up active coating badge
        int stripX = this.leftPos + 76;
        int stripY = this.topPos + 81;
        drawCoatingPip(graphics, stripX, stripY, "Pb", coatingId == AeroRefurbishmentBenchBlockEntity.COATING_LEAD, 0xFF94A3B8);
        drawCoatingPip(graphics, stripX + 18, stripY, "TPS", coatingId == AeroRefurbishmentBenchBlockEntity.COATING_TPS, 0xFF8FBC5A);
        drawCoatingPip(graphics, stripX + 41, stripY, "SOFI", coatingId == AeroRefurbishmentBenchBlockEntity.COATING_SOFI, 0xFFF59E0B);
        drawCoatingPip(graphics, stripX + 67, stripY, "LNS", coatingId == AeroRefurbishmentBenchBlockEntity.COATING_LONSDALEITE, 0xFF7FE8FF);

        // Bottom Slim Obsidian OLED Telemetry Bar (x=8..168, y=98..110)
        String footerText = switch (mode) {
            case AeroRefurbishmentBenchBlockEntity.MODE_REPAIR -> "REPAIRING HULL: " + integrityPct + "% -> 100%";
            case AeroRefurbishmentBenchBlockEntity.MODE_COAT -> "COATING: " + AeroRefurbishmentBenchBlockEntity.coatingShortName(coatingId);
            case AeroRefurbishmentBenchBlockEntity.MODE_REPAIR_AND_COAT -> "FIX + " + AeroRefurbishmentBenchBlockEntity.coatingShortName(coatingId);
            default -> "STANDBY - INSERT PART & MATERIAL";
        };
        int footerColor = working ? 0xFF5CF2C8
                : (mode != AeroRefurbishmentBenchBlockEntity.MODE_IDLE ? 0xFFFFBA5A : 0xFF6B7B90);
        graphics.drawString(
                this.font,
                this.font.plainSubstrByWidth(footerText, 152),
                this.leftPos + 89 - Math.min(152, this.font.width(footerText)) / 2,
                this.topPos + 100,
                footerColor,
                false
        );

        // Contextual tooltips strictly on OLED / empty slots / coating strip
        if (mouseIn(mouseX, mouseY, oledX, oledY, oledW, oledH)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Laser Refurbishment Chamber").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Mode: " + modeCap + " | Power: 50 GJ/t").withStyle(ChatFormatting.GRAY).getVisualOrderText(),
                    Component.literal("Part Integrity: " + (hasPart ? integrityPct + "% -> 100%" : "No Part")).withStyle(ChatFormatting.GREEN).getVisualOrderText(),
                    Component.literal("Active Coating: " + AeroRefurbishmentBenchBlockEntity.coatingShortName(coatingId)).withStyle(ChatFormatting.GOLD).getVisualOrderText()
            ));
        } else if (mouseIn(mouseX, mouseY, this.leftPos + 73, this.topPos + 77, 91, 15)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Supported Protective Coatings").withStyle(ChatFormatting.GOLD).getVisualOrderText(),
                    Component.literal("Pb (Lead Ingot): Acid & Radiation Shield (Venus)").withStyle(ChatFormatting.GRAY).getVisualOrderText(),
                    Component.literal("TPS (Ablative Tile): +1,200 K Thermal Protection").withStyle(ChatFormatting.GREEN).getVisualOrderText(),
                    Component.literal("SOFI (Insulation): 0.15x Cryo Boil-Off").withStyle(ChatFormatting.YELLOW).getVisualOrderText(),
                    Component.literal("LNS (Lonsdaleite): 2.5x Pressure + 1,500 K").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("C-103 (Compressed Niobium): +900 K Refractory / Repair").withStyle(ChatFormatting.BLUE).getVisualOrderText()
            ));
        } else if (!hasPart && mouseIn(mouseX, mouseY, this.leftPos + 35, this.topPos + 25, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Modular Rocket Part Input").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Insert any damaged or uncoated modular rocket part").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (!hasMat && mouseIn(mouseX, mouseY, this.leftPos + 35, this.topPos + 75, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Repair Material or Coating").withStyle(ChatFormatting.GOLD).getVisualOrderText(),
                    Component.literal("Repair: Hull Metal, Compressed Niobium, Ablative Tile").withStyle(ChatFormatting.GREEN).getVisualOrderText(),
                    Component.literal("Coat: Lead, Ablative Tile, SOFI, Lonsdaleite, C-103").withStyle(ChatFormatting.AQUA).getVisualOrderText()
            ));
        }
    }

    private void drawCoatingPip(GuiGraphics graphics, int x, int y, String label, boolean active, int activeColor) {
        int col = active ? activeColor : 0xFF475569;
        graphics.drawString(this.font, label, x, y, col, false);
        if (active) {
            graphics.fill(x, y + 9, x + this.font.width(label), y + 10, activeColor);
        }
    }
}
