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
import dev.galacticraft.mod.content.block.entity.machine.ResearchWorkbenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ResearchWorkbenchMenu extends MachineMenu<ResearchWorkbenchBlockEntity> {
    private int progress;
    private int maxProgress = ResearchWorkbenchBlockEntity.DEFAULT_DURATION;
    private int activeExperimentId;
    private int completedCount;
    private int metrologyTier;
    private int hasStylusFlag;

    public ResearchWorkbenchMenu(int syncId, @NotNull ServerPlayer player, @NotNull ResearchWorkbenchBlockEntity machine) {
        super(GCMenuTypes.RESEARCH_WORKBENCH, syncId, player, machine);
    }

    public ResearchWorkbenchMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.RESEARCH_WORKBENCH, syncId, inventory, pos, 32, 126);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getMaxProgress, this::setMaxProgress);
        data.registerInt(this.be::getActiveExperimentId, this::setActiveExperimentId);
        data.registerInt(this.be::getCompletedCount, this::setCompletedCount);
        data.registerInt(this.be::getMetrologyTier, this::setMetrologyTier);
        data.registerInt(this.be::getHasStylusFlag, this::setHasStylusFlag);
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

    public int getActiveExperimentId() {
        return this.activeExperimentId;
    }

    public void setActiveExperimentId(int activeExperimentId) {
        this.activeExperimentId = activeExperimentId;
    }

    public int getCompletedCount() {
        return this.completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getMetrologyTier() {
        return this.metrologyTier;
    }

    public void setMetrologyTier(int metrologyTier) {
        this.metrologyTier = metrologyTier;
    }

    public int getHasStylusFlag() {
        return this.hasStylusFlag;
    }

    public void setHasStylusFlag(int hasStylusFlag) {
        this.hasStylusFlag = hasStylusFlag;
    }
}
