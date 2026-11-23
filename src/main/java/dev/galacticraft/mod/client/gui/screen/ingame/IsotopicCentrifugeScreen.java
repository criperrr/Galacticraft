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

        // Separation progress bar
        int barX = this.leftPos + 90;
        int barY = this.topPos + 27;
        int barW = 28;
        int barH = 8;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF23252B);
        int fill = Mth.clamp((int) ((float) this.menu.getProgress() / (float) this.menu.getMaxProgress() * barW), 0, barW);
        if (fill > 0) {
            graphics.fill(barX, barY, barX + fill, barY + barH, 0xFF39D5FF);
        }

        // Rotor RPM readout & bar
        int rpm = this.menu.getRotorRpm();
        int rpmColor = rpm >= IsotopicCentrifugeBlockEntity.TARGET_RPM ? 0xFF007B9E : 0xFF555555;
        graphics.drawString(this.font, (rpm / 1000) + "k RPM", this.leftPos + 56, this.topPos + 76, rpmColor, false);

        int rpmBarX = this.leftPos + 56;
        int rpmBarY = this.topPos + 87;
        int rpmBarW = 62;
        graphics.fill(rpmBarX, rpmBarY, rpmBarX + rpmBarW, rpmBarY + 5, 0xFF23252B);
        int rpmFill = Mth.clamp((int) ((float) rpm / (float) IsotopicCentrifugeBlockEntity.MAX_RPM * rpmBarW), 0, rpmBarW);
        if (rpmFill > 0) {
            int barCol = rpm >= IsotopicCentrifugeBlockEntity.TARGET_RPM ? 0xFF2AE8FF : 0xFFE6A23C;
            graphics.fill(rpmBarX, rpmBarY, rpmBarX + rpmFill, rpmBarY + 5, barCol);
        }

        // Tank labels
        graphics.drawString(this.font, "SV", this.leftPos + 35, this.topPos + 95, 0xFF705820, false);
        graphics.drawString(this.font, "3He", this.leftPos + 126, this.topPos + 95, 0xFF007B9E, false);
        graphics.drawString(this.font, "4He", this.leftPos + 150, this.topPos + 95, 0xFF606060, false);
    }
}
