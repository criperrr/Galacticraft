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
import dev.galacticraft.mod.content.block.entity.machine.ElectrolyticSeparatorBlockEntity;
import dev.galacticraft.mod.screen.ElectrolyticSeparatorMenu;
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
public class ElectrolyticSeparatorScreen extends MachineScreen<ElectrolyticSeparatorBlockEntity, ElectrolyticSeparatorMenu> {
    public ElectrolyticSeparatorScreen(ElectrolyticSeparatorMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.ELECTROLYTIC_SEPARATOR_SCREEN);
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
        int amps = this.menu.getElectrodeAmps();
        int mv = this.menu.getCellVoltageMv();
        boolean active = amps > 0;

        // Animated right-header telemetry LED
        int ledColor = active
                ? (((timeMs / 350L) & 1L) == 0L ? 0xFF3BF0FF : 0xFF1AA8CC)
                : 0xFF4A5568;
        graphics.fill(this.leftPos + 166, this.topPos + 7, this.leftPos + 169, this.topPos + 14, ledColor);

        // Live PEM Electrolysis Cell Viewport (x=54..96, y=46..68)
        int cellX = this.leftPos + 54;
        int cellY = this.topPos + 46;
        int cellW = 42;
        int cellH = 22;

        // Subtle electrolyte fluid glow at base of cell
        graphics.fill(cellX + 2, cellY + 12, cellX + cellW - 2, cellY + cellH - 1, active ? 0x552288CC : 0x33183A52);

        // Copper cathode (-) on left, platinum anode (+) on right
        int cathodeCol = active ? 0xFFE89B4C : 0xFF8C5B32;
        int anodeCol = active ? 0xFFDCEBFF : 0xFF74849A;
        graphics.fill(cellX + 6, cellY + 3, cellX + 9, cellY + cellH - 3, cathodeCol);
        graphics.fill(cellX + cellW - 9, cellY + 3, cellX + cellW - 6, cellY + cellH - 3, anodeCol);

        // Micro polarity markers
        graphics.fill(cellX + 11, cellY + 5, cellX + 14, cellY + 6, 0xFF64E8FF); // '-'
        graphics.fill(cellX + cellW - 14, cellY + 5, cellX + cellW - 11, cellY + 6, 0xFFFF7A7A); // '+'
        graphics.fill(cellX + cellW - 13, cellY + 4, cellX + cellW - 12, cellY + 7, 0xFFFF7A7A);

        if (active) {
            // Animated electric arc across electrolyte membrane
            int arcY = cellY + 11 + (int) (Math.sin(timeMs / 90.0) * 3.0);
            graphics.fill(cellX + 9, arcY, cellX + cellW - 9, arcY + 1, 0xAA66F3FF);
            int sparkX = cellX + 11 + (int) ((timeMs / 45L) % Math.max(1, cellW - 22));
            graphics.fill(sparkX, arcY - 1, sparkX + 3, arcY + 2, 0xFFFFFFFF);

            // Rising H2 (left) and O2 (right) electrolysis micro-bubbles
            for (int i = 0; i < 4; i++) {
                int rise = (int) (((timeMs / 80L) + i * 5L) % 16L);
                int bxL = cellX + 10 + (i % 2) * 3;
                int byL = cellY + cellH - 3 - rise;
                graphics.fill(bxL, byL, bxL + 2, byL + 2, 0xFF9DF2FF);

                int bxR = cellX + cellW - 13 - (i % 2) * 3;
                int byR = cellY + cellH - 3 - ((rise + 7) % 16);
                graphics.fill(bxR, byR, bxR + 2, byR + 2, 0xFF56B8FF);
            }
        } else {
            // Center PEM membrane dashed divider when idle
            for (int dy = 3; dy < cellH - 3; dy += 4) {
                graphics.fill(cellX + cellW / 2, cellY + dy, cellX + cellW / 2 + 1, cellY + dy + 2, 0xFF34445A);
            }
        }

        // Segmented glowing progress bar (x=52, y=74, w=46, h=6)
        int barX = this.leftPos + 52;
        int barY = this.topPos + 74;
        int barW = 46;
        int barH = 6;
        int fill = Mth.clamp((int) ((float) this.menu.getProgress() / (float) this.menu.getMaxProgress() * barW), 0, barW);
        if (fill > 0) {
            graphics.fillGradient(barX, barY, barX + fill, barY + barH, 0xFF66F0FF, 0xFF238BE6);
            // Subtle segment dividers for aerospace gauge feel
            for (int sx = 6; sx < fill; sx += 6) {
                graphics.fill(barX + sx, barY, barX + sx + 1, barY + barH, 0x440D1824);
            }
        }

        // Compact voltage / current telemetry readout below bar
        String electricText = active
                ? String.format("%.2fV %dA", mv / 1000.0F, amps)
                : "STANDBY";
        int textColor = active ? 0xFF0A5C78 : 0xFF525A66;
        int textX = this.leftPos + 75 - this.font.width(electricText) / 2;
        graphics.drawString(this.font, electricText, textX, this.topPos + 84, textColor, false);

        // Subtle "ICE" ghost label inside empty SOLID_ICE_SLOT (67, 24) so players instantly know its purpose
        if (this.menu.itemStorage.slot(ElectrolyticSeparatorBlockEntity.SOLID_ICE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "ICE", this.leftPos + 68, this.topPos + 28, 0xFF7FA6C4, false);
        }

        // Bottom obsidian OLED badge strip aligned directly under the 4 fluid tanks
        int lcdY = this.topPos + 100;
        graphics.drawString(this.font, "H2O", this.leftPos + 29, lcdY, 0xFF58B0FF, false);
        graphics.drawString(this.font, "2H2+O2", this.leftPos + 58, lcdY, active ? 0xFF7BE8FF : 0xFF4C5E74, false);
        graphics.drawString(this.font, "LH2", this.leftPos + 104, lcdY, 0xFFA4F2FF, false);
        graphics.drawString(this.font, "LOX", this.leftPos + 128, lcdY, 0xFF5CC4FF, false);
        graphics.drawString(this.font, "D2", this.leftPos + 155, lcdY, 0xFF93AAFF, false);

        // Hover tooltip strictly over the PEM cell viewport (never overlaps slots or REI progress bar)
        if (mouseIn(mouseX, mouseY, this.leftPos + 54, this.topPos + 46, 42, 22)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("PEM Cryogenic Electrolyzer").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("2 H2O -> 2 LH2 + LOX").withStyle(ChatFormatting.GRAY).getVisualOrderText(),
                    Component.literal("Dense Ice yields trace Deuterium (D2)").withStyle(ChatFormatting.BLUE).getVisualOrderText(),
                    Component.literal(String.format("Stack: %.2f V | %d A | 120 GJ/t", mv / 1000.0F, amps))
                            .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ElectrolyticSeparatorBlockEntity.SOLID_ICE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, this.leftPos + 66, this.topPos + 23, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Cryo Ice Feedstock").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Insert Dense Ice, Blue Ice, Packed Ice, or Ice").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ElectrolyticSeparatorBlockEntity.WATER_BUCKET_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, this.leftPos + 29, this.topPos + 75, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Water Input Slot").withStyle(ChatFormatting.BLUE).getVisualOrderText(),
                    Component.literal("Insert Water Bucket or Fluid Canister").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ElectrolyticSeparatorBlockEntity.LH2_CANISTER_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, this.leftPos + 103, this.topPos + 75, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Liquid Hydrogen (LH2) Drain").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Insert Empty Bucket or Fluid Canister").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ElectrolyticSeparatorBlockEntity.LOX_CANISTER_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, this.leftPos + 127, this.topPos + 75, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Liquid Oxygen (LOX) Drain").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Insert Empty Bucket or Fluid Canister").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ElectrolyticSeparatorBlockEntity.DEUTERIUM_CANISTER_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, this.leftPos + 151, this.topPos + 75, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Deuterium Gas (D2) Drain").withStyle(ChatFormatting.BLUE).getVisualOrderText(),
                    Component.literal("Insert Empty Fluid Canister").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        }
    }
}
