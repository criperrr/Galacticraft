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
import dev.galacticraft.mod.content.block.entity.machine.Helium3FusionReactorBlockEntity;
import dev.galacticraft.mod.screen.Helium3FusionReactorMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class Helium3FusionReactorScreen extends MachineScreen<Helium3FusionReactorBlockEntity, Helium3FusionReactorMenu> {
    public Helium3FusionReactorScreen(Helium3FusionReactorMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.HELIUM3_FUSION_REACTOR_SCREEN);
        this.imageWidth = 176;
        this.imageHeight = 192;
        this.capacitorX = 8;
        this.capacitorY = 20;
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        // Multiblock Torus validation readout
        int blocks = this.menu.getValidStructureBlocks();
        boolean formed = this.menu.isMultiblockFormed();
        int torusColor = formed ? 0xFF1B8A3A : 0xFFC62828;
        graphics.drawString(this.font, "TORUS " + blocks + "/" + Helium3FusionReactorBlockEntity.REQUIRED_MULTIBLOCK_BLOCKS,
                this.leftPos + 103, this.topPos + 21, torusColor, false);

        // Plasma core temperature (MK = Million Kelvin)
        int tempMk = this.menu.getPlasmaTempMk();
        int tempColor = tempMk >= Helium3FusionReactorBlockEntity.IGNITION_TEMP_MK ? 0xFF00838F : 0xFF555555;
        graphics.drawString(this.font, tempMk + " MK", this.leftPos + 103, this.topPos + 34, tempColor, false);

        // Magnetic field strength (Tesla)
        String field = this.menu.isCryoCooled() ? "18.5 T" : (formed ? "12.0 T" : "0.0 T");
        int fieldColor = this.menu.isCryoCooled() ? 0xFF00838F : 0xFF455A64;
        graphics.drawString(this.font, field, this.leftPos + 103, this.topPos + 47, fieldColor, false);

        // Cell synthesis mini-bar under output cell slot
        int cellBarX = this.leftPos + 148;
        int cellBarY = this.topPos + 55;
        graphics.fill(cellBarX, cellBarY, cellBarX + 16, cellBarY + 4, 0xFF23252B);
        int cellFill = Mth.clamp((int) ((float) this.menu.getCellSynthesisProgress() / (float) Helium3FusionReactorBlockEntity.CELL_SYNTHESIS_TICKS * 16), 0, 16);
        if (cellFill > 0) {
            graphics.fill(cellBarX, cellBarY, cellBarX + cellFill, cellBarY + 4, 0xFF64FFDA);
        }

        // MHD Plasma burn bar
        int barX = this.leftPos + 78;
        int barY = this.topPos + 73;
        int barW = 88;
        int barH = 6;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF20222A);
        if (this.menu.getBurnTime() > 0) {
            int fill = Mth.clamp((int) ((float) this.menu.getBurnTime() / (float) this.menu.getBurnDuration() * barW), 1, barW);
            int color = this.menu.isCryoCooled() ? 0xFF00E5FF : 0xFF5C6BC0;
            graphics.fill(barX, barY, barX + fill, barY + barH, color);
        } else if (tempMk > 0 && formed) {
            int fill = Mth.clamp((int) ((float) tempMk / (float) Helium3FusionReactorBlockEntity.IGNITION_TEMP_MK * barW), 1, barW);
            graphics.fill(barX, barY, barX + fill, barY + barH, 0xFFFFA726);
        }

        // Power output readout
        int rate = this.menu.getActiveGenerationRate();
        String label;
        int textColor;
        if (!formed) {
            label = "BUILD 3x3x3 TORUS";
            textColor = 0xFFC62828;
        } else if (rate > 0) {
            label = rate + " GJ/t" + (this.menu.isCryoCooled() ? " <2K" : "");
            textColor = this.menu.isCryoCooled() ? 0xFF00838F : 0xFF1B8A3A;
        } else if (tempMk > 0) {
            label = "IGNITING " + tempMk + "MK";
            textColor = 0xFFE65100;
        } else {
            label = "STANDBY";
            textColor = 0xFF606060;
        }
        graphics.drawString(this.font, label, this.leftPos + 78, this.topPos + 83, textColor, false);

        // Tank labels
        graphics.drawString(this.font, "3He", this.leftPos + 32, this.topPos + 95, 0xFF007B9E, false);
        graphics.drawString(this.font, "D2", this.leftPos + 56, this.topPos + 95, 0xFF3F51B5, false);
    }
}
