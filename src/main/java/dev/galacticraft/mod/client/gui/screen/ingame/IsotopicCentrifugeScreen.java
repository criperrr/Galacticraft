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
import dev.galacticraft.mod.content.block.entity.machine.IsotopicCentrifugeBlockEntity;
import dev.galacticraft.mod.screen.IsotopicCentrifugeMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class IsotopicCentrifugeScreen extends MachineScreen<IsotopicCentrifugeBlockEntity, IsotopicCentrifugeMenu> {
    public IsotopicCentrifugeScreen(IsotopicCentrifugeMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.ISOTOPIC_CENTRIFUGE_SCREEN);
        this.imageWidth = 176;
        this.imageHeight = 192;
        this.capacitorX = 8;
        this.capacitorY = 20;
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        // Separation progress bar with segmented cyan glow
        int barX = this.leftPos + 90;
        int barY = this.topPos + 27;
        int barW = 28;
        int barH = 8;
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF141B26);
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1C2432);
        int fill = Mth.clamp((int) ((float) this.menu.getProgress() / (float) this.menu.getMaxProgress() * barW), 0, barW);
        if (fill > 0) {
            graphics.fillGradient(barX, barY, barX + fill, barY + barH, 0xFF5CF2FF, 0xFF1590C8);
        }

        // Animated magnetic rotor indicator next to coil
        int rpm = this.menu.getRotorRpm();
        boolean locked = rpm >= IsotopicCentrifugeBlockEntity.TARGET_RPM;
        int rotorCx = this.leftPos + 59;
        int rotorCy = this.topPos + 30;
        graphics.fill(rotorCx - 5, rotorCy - 5, rotorCx + 6, rotorCy + 6, 0xFF121822);
        graphics.renderOutline(rotorCx - 5, rotorCy - 5, 11, 11, locked ? 0xFF39D5FF : 0xFF455264);
        double ang = rpm > 0 ? (System.currentTimeMillis() / Math.max(25.0, 260.0 - (rpm / 260.0))) : 0.0;
        int rx = rotorCx + (int) Math.round(Math.cos(ang) * 3.5);
        int ry = rotorCy + (int) Math.round(Math.sin(ang) * 3.5);
        int rx2 = rotorCx - (int) Math.round(Math.cos(ang) * 3.5);
        int ry2 = rotorCy - (int) Math.round(Math.sin(ang) * 3.5);
        int dotCol = locked ? 0xFF5CF2FF : 0xFFE6A23C;
        graphics.fill(rx, ry, rx + 1, ry + 1, dotCol);
        graphics.fill(rx2, ry2, rx2 + 1, ry2 + 1, dotCol);
        graphics.fill(rotorCx, rotorCy, rotorCx + 1, rotorCy + 1, 0xFFEAF6FF);

        // Dark obsidian RPM telemetry readout & segmented tachometer
        int panelX = this.leftPos + 54;
        int panelY = this.topPos + 74;
        graphics.fill(panelX, panelY, panelX + 66, panelY + 20, 0xFF141B26);
        graphics.renderOutline(panelX, panelY, 66, 20, locked ? 0xFF28B8D8 : 0xFF3A4454);

        int rpmColor = locked ? 0xFF5CF2FF : (rpm > 0 ? 0xFFFFC14D : 0xFF8C97A8);
        graphics.drawString(this.font, (rpm / 1000) + "k RPM", panelX + 4, panelY + 3, rpmColor, false);

        int rpmBarX = panelX + 3;
        int rpmBarY = panelY + 13;
        int rpmBarW = 60;
        graphics.fill(rpmBarX, rpmBarY, rpmBarX + rpmBarW, rpmBarY + 4, 0xFF0A0F16);
        int rpmFill = Mth.clamp((int) ((float) rpm / (float) IsotopicCentrifugeBlockEntity.MAX_RPM * rpmBarW), 0, rpmBarW);
        if (rpmFill > 0) {
            int topCol = locked ? 0xFF5CF2FF : 0xFFFFCA58;
            int botCol = locked ? 0xFF1084B8 : 0xFFD97418;
            graphics.fillGradient(rpmBarX, rpmBarY, rpmBarX + rpmFill, rpmBarY + 4, topCol, botCol);
        }

        // Tank labels with crisp badge pills
        graphics.fill(this.leftPos + 31, this.topPos + 94, this.leftPos + 49, this.topPos + 104, 0xFF141B26);
        graphics.fill(this.leftPos + 124, this.topPos + 94, this.leftPos + 144, this.topPos + 104, 0xFF141B26);
        graphics.fill(this.leftPos + 148, this.topPos + 94, this.leftPos + 168, this.topPos + 104, 0xFF141B26);
        graphics.drawString(this.font, "SV", this.leftPos + 35, this.topPos + 95, 0xFFE6C86E, false);
        graphics.drawString(this.font, "3He", this.leftPos + 126, this.topPos + 95, 0xFF5CF2FF, false);
        graphics.drawString(this.font, "4He", this.leftPos + 150, this.topPos + 95, 0xFF9EA8B8, false);
    }
}
