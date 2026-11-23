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
import dev.galacticraft.mod.content.block.entity.machine.RegolithThermalDesorberBlockEntity;
import dev.galacticraft.mod.screen.RegolithThermalDesorberMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class RegolithThermalDesorberScreen extends MachineScreen<RegolithThermalDesorberBlockEntity, RegolithThermalDesorberMenu> {
    public RegolithThermalDesorberScreen(RegolithThermalDesorberMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.REGOLITH_THERMAL_DESORBER_SCREEN);
        this.capacitorX = 8;
        this.capacitorY = 8;
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        // Progress bar between input and byproduct slots
        int barX = this.leftPos + 62;
        int barY = this.topPos + 38;
        int barW = 28;
        int barH = 6;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF2B2B30);
        int fill = Mth.clamp((int) ((float) this.menu.getProgress() / (float) this.menu.getMaxProgress() * barW), 0, barW);
        if (fill > 0) {
            graphics.fill(barX, barY, barX + fill, barY + barH, 0xFFE6A23C);
        }

        // Thermal indicator (>700C threshold)
        int temp = this.menu.getTemperature();
        int tempColor = temp >= RegolithThermalDesorberBlockEntity.TARGET_TEMPERATURE ? 0xFFFF7A26 : 0xFFAAAAAA;
        graphics.drawString(this.font, temp + "°C", this.leftPos + 62, this.topPos + 24, tempColor, false);
    }
}
