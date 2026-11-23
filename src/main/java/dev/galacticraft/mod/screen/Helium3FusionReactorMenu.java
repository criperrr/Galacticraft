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
import dev.galacticraft.mod.content.block.entity.machine.Helium3FusionReactorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class Helium3FusionReactorMenu extends MachineMenu<Helium3FusionReactorBlockEntity> {
    private int burnTime;
    private int burnDuration = 40;
    private int activeGenerationRate;
    private boolean cryoCooled;
    private int cellSynthesisProgress;

    public Helium3FusionReactorMenu(int syncId, @NotNull ServerPlayer player, @NotNull Helium3FusionReactorBlockEntity machine) {
        super(GCMenuTypes.HELIUM3_FUSION_REACTOR, syncId, player, machine);
    }

    public Helium3FusionReactorMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.HELIUM3_FUSION_REACTOR, syncId, inventory, pos, 8, 84);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getBurnTime, this::setBurnTime);
        data.registerInt(this.be::getBurnDuration, this::setBurnDuration);
        data.registerInt(this.be::getActiveGenerationRate, this::setActiveGenerationRate);
        data.registerBoolean(this.be::isCryoCooled, this::setCryoCooled);
        data.registerInt(this.be::getCellSynthesisProgress, this::setCellSynthesisProgress);
    }

    public int getBurnTime() {
        return this.burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public int getBurnDuration() {
        return this.burnDuration;
    }

    public void setBurnDuration(int burnDuration) {
        this.burnDuration = Math.max(1, burnDuration);
    }

    public int getActiveGenerationRate() {
        return this.activeGenerationRate;
    }

    public void setActiveGenerationRate(int activeGenerationRate) {
        this.activeGenerationRate = activeGenerationRate;
    }

    public boolean isCryoCooled() {
        return this.cryoCooled;
    }

    public void setCryoCooled(boolean cryoCooled) {
        this.cryoCooled = cryoCooled;
    }

    public int getCellSynthesisProgress() {
        return this.cellSynthesisProgress;
    }

    public void setCellSynthesisProgress(int cellSynthesisProgress) {
        this.cellSynthesisProgress = cellSynthesisProgress;
    }
}
