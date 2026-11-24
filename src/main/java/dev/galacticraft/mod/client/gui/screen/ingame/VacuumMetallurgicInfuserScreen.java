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
import dev.galacticraft.mod.content.block.entity.machine.VacuumMetallurgicInfuserBlockEntity;
import dev.galacticraft.mod.screen.VacuumMetallurgicInfuserMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class VacuumMetallurgicInfuserScreen extends MachineScreen<VacuumMetallurgicInfuserBlockEntity, VacuumMetallurgicInfuserMenu> {
    public VacuumMetallurgicInfuserScreen(VacuumMetallurgicInfuserMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.VACUUM_METALLURGIC_INFUSER_SCREEN);
        this.imageWidth = 176;
        this.imageHeight = 192;
        this.capacitorX = 8;
        this.capacitorY = 20;
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        // Vertical internal infusion buffer gauge (Mekanism-style) at (56, 20), 10x52
        int tankX = this.leftPos + 56;
        int tankY = this.topPos + 20;
        int tankW = 10;
        int tankH = 52;
        int amount = this.menu.getInfusionAmount();
        int typeId = this.menu.getInfusionTypeId();
        int fillH = Mth.clamp((int) ((float) amount / (float) VacuumMetallurgicInfuserBlockEntity.MAX_INFUSION * tankH), 0, tankH);
        if (fillH > 0) {
            int col = VacuumMetallurgicInfuserBlockEntity.idToBarColor(typeId);
            graphics.fill(tankX, tankY + tankH - fillH, tankX + tankW, tankY + tankH, col);
        }

        // Horizontal progress bar between base metal and output
        int barX = this.leftPos + 96;
        int barY = this.topPos + 46;
        int barW = 34;
        int barH = 8;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF23252B);
        int fill = Mth.clamp((int) ((float) this.menu.getProgress() / (float) this.menu.getMaxProgress() * barW), 0, barW);
        if (fill > 0) {
            graphics.fill(barX, barY, barX + fill, barY + barH, 0xFFFF9A2E);
        }

        // Infusion agent readout
        String agentName = VacuumMetallurgicInfuserBlockEntity.idToDisplayName(typeId);
        graphics.drawString(this.font, agentName + ": " + amount + "/" + VacuumMetallurgicInfuserBlockEntity.MAX_INFUSION,
                this.leftPos + 34, this.topPos + 80, 0xFF404040, false);
        graphics.drawString(this.font, "Vacuum Alloying (60 GJ/t)", this.leftPos + 34, this.topPos + 92, 0xFF555555, false);
    }
}
