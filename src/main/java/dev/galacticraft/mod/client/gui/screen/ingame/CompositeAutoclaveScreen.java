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
import dev.galacticraft.mod.content.block.entity.machine.CompositeAutoclaveBlockEntity;
import dev.galacticraft.mod.screen.CompositeAutoclaveMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class CompositeAutoclaveScreen extends MachineScreen<CompositeAutoclaveBlockEntity, CompositeAutoclaveMenu> {
    public CompositeAutoclaveScreen(CompositeAutoclaveMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.COMPOSITE_AUTOCLAVE_SCREEN);
        this.imageWidth = 176;
        this.imageHeight = 192;
        this.capacitorX = 8;
        this.capacitorY = 20;
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        // Curing progress bar
        int barX = this.leftPos + 82;
        int barY = this.topPos + 43;
        int barW = 44;
        int barH = 8;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF23252B);
        int fill = Mth.clamp((int) ((float) this.menu.getProgress() / (float) this.menu.getMaxProgress() * barW), 0, barW);
        if (fill > 0) {
            graphics.fill(barX, barY, barX + fill, barY + barH, 0xFFE87A2C);
        }

        // Thermal gauge
        int temp = this.menu.getTemperature();
        int targetTemp = this.menu.getTargetTemperature();
        int tempColor = temp >= targetTemp ? 0xFFE65100 : 0xFF555555;
        graphics.drawString(this.font, temp + "/" + targetTemp + "°C", this.leftPos + 36, this.topPos + 72, tempColor, false);
        int tBarX = this.leftPos + 36;
        int tBarY = this.topPos + 82;
        int tBarW = 60;
        graphics.fill(tBarX, tBarY, tBarX + tBarW, tBarY + 5, 0xFF23252B);
        int tempFill = Mth.clamp((int) ((float) temp / (float) CompositeAutoclaveBlockEntity.MAX_TEMPERATURE * tBarW), 0, tBarW);
        if (tempFill > 0) {
            graphics.fill(tBarX, tBarY, tBarX + tempFill, tBarY + 5, 0xFFFF6E1A);
        }

        // Pressure gauge
        int bar = this.menu.getPressureBar();
        int targetBar = this.menu.getTargetPressureBar();
        int pColor = bar >= targetBar ? 0xFF1565C0 : 0xFF555555;
        graphics.drawString(this.font, bar + "/" + targetBar + " bar", this.leftPos + 104, this.topPos + 72, pColor, false);
        int pBarX = this.leftPos + 104;
        int pBarY = this.topPos + 82;
        int pBarW = 58;
        graphics.fill(pBarX, pBarY, pBarX + pBarW, pBarY + 5, 0xFF23252B);
        int pressFill = Mth.clamp((int) ((float) bar / (float) CompositeAutoclaveBlockEntity.MAX_PRESSURE_BAR * pBarW), 0, pBarW);
        if (pressFill > 0) {
            graphics.fill(pBarX, pBarY, pBarX + pressFill, pBarY + 5, 0xFF3FA9F5);
        }

        // Stage caption
        String stage = (temp < targetTemp || bar < targetBar) ? "Pressurizing Chamber..." : "Autoclave Curing";
        graphics.drawString(this.font, stage, this.leftPos + 36, this.topPos + 92, 0xFF404040, false);
    }
}
