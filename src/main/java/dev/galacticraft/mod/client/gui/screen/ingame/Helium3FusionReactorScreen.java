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
        this.capacitorX = 8;
        this.capacitorY = 8;
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        // MHD Plasma bar
        int barX = this.leftPos + 82;
        int barY = this.topPos + 67;
        int barW = 78;
        int barH = 5;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF20222A);
        if (this.menu.getBurnTime() > 0) {
            int fill = Mth.clamp((int) ((float) this.menu.getBurnTime() / (float) this.menu.getBurnDuration() * barW), 1, barW);
            int color = this.menu.isCryoCooled() ? 0xFF3BF5FF : 0xFF7C8CFF;
            graphics.fill(barX, barY, barX + fill, barY + barH, color);
        }

        // Output readout
        int rate = this.menu.getActiveGenerationRate();
        String label = rate > 0 ? (rate + " GJ/t" + (this.menu.isCryoCooled() ? " <2K" : "")) : "IDLE";
        int textColor = this.menu.isCryoCooled() ? 0xFF3BF5FF : (rate > 0 ? 0xFF76FF7A : 0xFF999999);
        graphics.drawString(this.font, label, this.leftPos + 82, this.topPos + 74, textColor, false);
    }
}
