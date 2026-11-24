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

package dev.galacticraft.mod.screen;

import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.menu.MenuData;
import dev.galacticraft.mod.content.block.entity.machine.ElectrolyticSeparatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ElectrolyticSeparatorMenu extends MachineMenu<ElectrolyticSeparatorBlockEntity> {
    private int progress;
    private int maxProgress = 20;
    private int electrodeAmps;
    private int cellVoltageMv;

    public ElectrolyticSeparatorMenu(int syncId, @NotNull ServerPlayer player, @NotNull ElectrolyticSeparatorBlockEntity machine) {
        super(GCMenuTypes.ELECTROLYTIC_SEPARATOR, syncId, player, machine);
    }

    public ElectrolyticSeparatorMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.ELECTROLYTIC_SEPARATOR, syncId, inventory, pos, 8, 122);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getMaxProgress, this::setMaxProgress);
        data.registerInt(this.be::getElectrodeAmps, this::setElectrodeAmps);
        data.registerInt(this.be::getCellVoltageMv, this::setCellVoltageMv);
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = Math.max(1, maxProgress);
    }

    public int getElectrodeAmps() {
        return this.electrodeAmps;
    }

    public void setElectrodeAmps(int electrodeAmps) {
        this.electrodeAmps = electrodeAmps;
    }

    public int getCellVoltageMv() {
        return this.cellVoltageMv;
    }

    public void setCellVoltageMv(int cellVoltageMv) {
        this.cellVoltageMv = cellVoltageMv;
    }
}
