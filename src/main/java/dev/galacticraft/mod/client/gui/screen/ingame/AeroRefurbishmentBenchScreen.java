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
import dev.galacticraft.mod.content.block.entity.machine.AeroRefurbishmentBenchBlockEntity;
import dev.galacticraft.mod.screen.AeroRefurbishmentBenchMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Environment(EnvType.CLIENT)
public class AeroRefurbishmentBenchScreen extends MachineScreen<AeroRefurbishmentBenchBlockEntity, AeroRefurbishmentBenchMenu> {
    public AeroRefurbishmentBenchScreen(AeroRefurbishmentBenchMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.AERO_REFURBISHMENT_BENCH_SCREEN);
        this.imageWidth = 176;
        this.imageHeight = 192;
        this.capacitorX = 8;
        this.capacitorY = 20;
        this.titleLabelY = 6;
        this.inventoryLabelY = 99;
    }

    @Override
    protected void drawTitle(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFEAF6FF, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        int mode = this.menu.getOperationMode();
        int progress = this.menu.getProgress();
        int maxProgress = this.menu.getMaxProgress();
        int integrityPct = Mth.clamp(this.menu.getInputIntegrityPct(), 0, 100);
        int coatingId = this.menu.getActiveCoatingId();
        boolean working = mode != AeroRefurbishmentBenchBlockEntity.MODE_IDLE && progress > 0;

        // Status LED top-right
        long timeMs = System.currentTimeMillis();
        int ledColor = working
                ? (((timeMs / 240L) & 1L) == 0L ? 0xFF42F5B3 : 0xFF2CC5E8)
                : (mode != AeroRefurbishmentBenchBlockEntity.MODE_IDLE ? 0xFFF5B041 : 0xFF475569);
        graphics.fill(this.leftPos + 165, this.topPos + 7, this.leftPos + 168, this.topPos + 14, ledColor);

        // Part Integrity Gauge bar above Part Slot (x=34..160, y=22..29)
        int barX = this.leftPos + 34;
        int barY = this.topPos + 22;
        int barW = 126;
        int barH = 7;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF101826);
        graphics.renderOutline(barX, barY, barW, barH, 0xFF334B6B);
        int intColor = integrityPct >= 80 ? 0xFF34D399 : (integrityPct >= 50 ? 0xFFFACC15 : 0xFFF87171);
        int intFill = Mth.clamp((int) ((barW - 2) * (integrityPct / 100.0F)), 0, barW - 2);
        if (intFill > 0) {
            graphics.fill(barX + 1, barY + 1, barX + 1 + intFill, barY + barH - 1, intColor);
        }

        // Progress bar between material input (x=78) and output (x=138)
        int pBarX = this.leftPos + 100;
        int pBarY = this.topPos + 46;
        int pBarW = 32;
        int pBarH = 8;
        graphics.fill(pBarX, pBarY, pBarX + pBarW, pBarY + pBarH, 0xFF101826);
        graphics.renderOutline(pBarX, pBarY, pBarW, pBarH, 0xFF334B6B);
        int pFill = Mth.clamp((int) ((float) progress / (float) maxProgress * (pBarW - 2)), 0, pBarW - 2);
        if (pFill > 0) {
            int pCol = mode == AeroRefurbishmentBenchBlockEntity.MODE_COAT ? 0xFF38BDF8 : 0xFF4ADE80;
            graphics.fill(pBarX + 1, pBarY + 1, pBarX + 1 + pFill, pBarY + pBarH - 1, pCol);
        }

        // Subtle ghost labels inside empty input slots
        if (this.menu.itemStorage.slot(AeroRefurbishmentBenchBlockEntity.PART_INPUT_SLOT).isEmpty()) {
            graphics.drawString(this.font, "PRT", this.leftPos + 45, this.topPos + 46, 0xFF6B7F99, false);
        }
        if (this.menu.itemStorage.slot(AeroRefurbishmentBenchBlockEntity.MATERIAL_INPUT_SLOT).isEmpty()) {
            graphics.drawString(this.font, "MAT", this.leftPos + 79, this.topPos + 46, 0xFF8C7A52, false);
        }

        // Bottom OLED telemetry bar (x=34..166, y=72..92)
        String modeLabel = switch (mode) {
            case AeroRefurbishmentBenchBlockEntity.MODE_REPAIR -> "REPAIR: " + integrityPct + "% -> 100%";
            case AeroRefurbishmentBenchBlockEntity.MODE_COAT -> "COAT: " + AeroRefurbishmentBenchBlockEntity.coatingShortName(coatingId);
            case AeroRefurbishmentBenchBlockEntity.MODE_REPAIR_AND_COAT -> "FIX+COAT: " + AeroRefurbishmentBenchBlockEntity.coatingShortName(coatingId);
            default -> "STANDBY - 50 GJ/t";
        };
        int modeColor = mode == AeroRefurbishmentBenchBlockEntity.MODE_IDLE ? 0xFF7B8FA6
                : (mode == AeroRefurbishmentBenchBlockEntity.MODE_COAT ? 0xFF5EEAD4 : 0xFF4ADE80);
        graphics.drawString(this.font, modeLabel, this.leftPos + 38, this.topPos + 76, modeColor, false);
        graphics.drawString(this.font, "Integrity: " + integrityPct + "%", this.leftPos + 38, this.topPos + 86, 0xFF94A3B8, false);

        // Helpful empty-slot hover tooltips
        if (this.menu.itemStorage.slot(AeroRefurbishmentBenchBlockEntity.PART_INPUT_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, this.leftPos + 43, this.topPos + 41, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Modular Rocket Part Slot").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Insert damaged or uncoated modular rocket part").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(AeroRefurbishmentBenchBlockEntity.MATERIAL_INPUT_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, this.leftPos + 77, this.topPos + 41, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Repair Material or Protective Coating").withStyle(ChatFormatting.GOLD).getVisualOrderText(),
                    Component.literal("Repair: Hull Material, Compressed Niobium, Ablative Tile").withStyle(ChatFormatting.GREEN).getVisualOrderText(),
                    Component.literal("Coatings: Lead (Acid), TPS Tile (+1200K), SOFI (0.15x), Lonsdaleite (2.5x atm)").withStyle(ChatFormatting.AQUA).getVisualOrderText()
            ));
        }
    }
}
