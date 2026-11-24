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
import dev.galacticraft.mod.content.block.entity.machine.AeroRefurbishmentBenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AeroRefurbishmentBenchMenu extends MachineMenu<AeroRefurbishmentBenchBlockEntity> {
    private int progress;
    private int maxProgress = AeroRefurbishmentBenchBlockEntity.REPAIR_TICKS;
    private int operationMode;
    private int inputIntegrityPct = 100;
    private int activeCoatingId;

    public AeroRefurbishmentBenchMenu(int syncId, @NotNull ServerPlayer player, @NotNull AeroRefurbishmentBenchBlockEntity machine) {
        super(GCMenuTypes.AERO_REFURBISHMENT_BENCH, syncId, player, machine);
    }

    public AeroRefurbishmentBenchMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.AERO_REFURBISHMENT_BENCH, syncId, inventory, pos, 8, 122);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getMaxProgress, this::setMaxProgress);
        data.registerInt(this.be::getOperationMode, this::setOperationMode);
        data.registerInt(this.be::getInputIntegrityPct, this::setInputIntegrityPct);
        data.registerInt(this.be::getActiveCoatingId, this::setActiveCoatingId);
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

    public int getOperationMode() {
        return this.operationMode;
    }

    public void setOperationMode(int operationMode) {
        this.operationMode = operationMode;
    }

    public int getInputIntegrityPct() {
        return this.inputIntegrityPct;
    }

    public void setInputIntegrityPct(int inputIntegrityPct) {
        this.inputIntegrityPct = inputIntegrityPct;
    }

    public int getActiveCoatingId() {
        return this.activeCoatingId;
    }

    public void setActiveCoatingId(int activeCoatingId) {
        this.activeCoatingId = activeCoatingId;
    }
}
