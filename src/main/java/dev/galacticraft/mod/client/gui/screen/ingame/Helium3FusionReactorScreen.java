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

        // Dark obsidian MHD telemetry display behind Torus/MK/Tesla readouts
        int mhdX = this.leftPos + 100;
        int mhdY = this.topPos + 18;
        int blocks = this.menu.getValidStructureBlocks();
        boolean formed = this.menu.isMultiblockFormed();
        int tempMk = this.menu.getPlasmaTempMk();
        boolean cryo = this.menu.isCryoCooled();
        int rate = this.menu.getActiveGenerationRate();

        graphics.fill(mhdX, mhdY, mhdX + 44, mhdY + 42, 0xFF141B26);
        graphics.renderOutline(mhdX, mhdY, 44, 42, cryo ? 0xFF39F0FF : (formed ? 0xFF2E8B57 : 0xFF8C2D2D));

        // Multiblock Torus validation readout
        int torusColor = formed ? 0xFF56F08A : 0xFFFF6666;
        graphics.drawString(this.font, blocks + "/" + Helium3FusionReactorBlockEntity.REQUIRED_MULTIBLOCK_BLOCKS,
                mhdX + 4, mhdY + 4, torusColor, false);

        // Plasma core temperature (MK = Million Kelvin)
        int tempColor = tempMk >= Helium3FusionReactorBlockEntity.IGNITION_TEMP_MK ? 0xFF4CF0FF : 0xFF9EA8B8;
        graphics.drawString(this.font, tempMk + "MK", mhdX + 4, mhdY + 17, tempColor, false);

        // Magnetic field strength (Tesla)
        String field = cryo ? "18.5T" : (formed ? "12.0T" : "0.0T");
        int fieldColor = cryo ? 0xFF5CF2FF : 0xFFB0BEC5;
        graphics.drawString(this.font, field, mhdX + 4, mhdY + 30, fieldColor, false);

        // Cell synthesis mini-bar under output cell slot
        int cellBarX = this.leftPos + 148;
        int cellBarY = this.topPos + 55;
        graphics.fill(cellBarX, cellBarY, cellBarX + 16, cellBarY + 4, 0xFF141B26);
        int cellFill = Mth.clamp((int) ((float) this.menu.getCellSynthesisProgress() / (float) Helium3FusionReactorBlockEntity.CELL_SYNTHESIS_TICKS * 16), 0, 16);
        if (cellFill > 0) {
            graphics.fill(cellBarX, cellBarY, cellBarX + cellFill, cellBarY + 4, 0xFF64FFDA);
        }

        // MHD Plasma burn bar & obsidian status bar
        int barX = this.leftPos + 78;
        int barY = this.topPos + 72;
        int barW = 88;
        int barH = 6;
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF141B26);
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1D2432);
        if (this.menu.getBurnTime() > 0) {
            int fill = Mth.clamp((int) ((float) this.menu.getBurnTime() / (float) this.menu.getBurnDuration() * barW), 1, barW);
            int topC = cryo ? 0xFF64F4FF : 0xFF7B8CFF;
            int botC = cryo ? 0xFF00A8CC : 0xFF3F51B5;
            graphics.fillGradient(barX, barY, barX + fill, barY + barH, topC, botC);
        } else if (tempMk > 0 && formed) {
            int fill = Mth.clamp((int) ((float) tempMk / (float) Helium3FusionReactorBlockEntity.IGNITION_TEMP_MK * barW), 1, barW);
            graphics.fillGradient(barX, barY, barX + fill, barY + barH, 0xFFFFCA58, 0xFFE65100);
        }

        // Power output obsidian readout strip
        graphics.fill(barX - 1, this.topPos + 81, barX + barW + 1, this.topPos + 93, 0xFF141B26);
        String label;
        int textColor;
        if (!formed) {
            label = "BUILD 3x3x3";
            textColor = 0xFFFF6666;
        } else if (rate > 0) {
            label = rate + " GJ/t" + (cryo ? " <2K" : "");
            textColor = cryo ? 0xFF5CF2FF : 0xFF56F08A;
        } else if (tempMk >= Helium3FusionReactorBlockEntity.IGNITION_TEMP_MK) {
            label = "CAPACITOR FULL";
            textColor = 0xFF56F08A;
        } else if (tempMk > 0) {
            label = "IGNITING " + tempMk + "MK";
            textColor = 0xFFFFB74D;
        } else {
            label = "MHD STANDBY";
            textColor = 0xFF8C97A8;
        }
        graphics.drawString(this.font, label, barX + 3, this.topPos + 83, textColor, false);

        // Tank labels in crisp obsidian pill badges
        graphics.fill(this.leftPos + 30, this.topPos + 94, this.leftPos + 50, this.topPos + 104, 0xFF141B26);
        graphics.fill(this.leftPos + 53, this.topPos + 94, this.leftPos + 71, this.topPos + 104, 0xFF141B26);
        graphics.drawString(this.font, "3He", this.leftPos + 32, this.topPos + 95, 0xFF5CF2FF, false);
        graphics.drawString(this.font, "D2", this.leftPos + 56, this.topPos + 95, 0xFF8CA4FF, false);
    }
}
