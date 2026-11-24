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
import dev.galacticraft.mod.content.block.entity.machine.ObservatoryTelescopeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ObservatoryTelescopeMenu extends MachineMenu<ObservatoryTelescopeBlockEntity> {
    private int progress;
    private int maxProgress = ObservatoryTelescopeBlockEntity.BASE_SCAN_TICKS;
    private int selectedTargetIndex;
    private int opticalClarityPct;
    private int maxReachTier = ObservatoryTelescopeBlockEntity.TIER_EARTH_ORBITAL;
    private int skyClearFlag;
    private int nightOrSpaceFlag;
    private int apertureTier = 1;
    private int opticsTier;
    private int auxDriveType;
    private int airmassHundredths = 100;

    public ObservatoryTelescopeMenu(int syncId, @NotNull ServerPlayer player, @NotNull ObservatoryTelescopeBlockEntity machine) {
        super(GCMenuTypes.OBSERVATORY_TELESCOPE, syncId, player, machine);
    }

    public ObservatoryTelescopeMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.OBSERVATORY_TELESCOPE, syncId, inventory, pos, 44, 130);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getMaxProgress, this::setMaxProgress);
        data.registerInt(this.be::getSelectedTargetIndex, this::setSelectedTargetIndex);
        data.registerInt(this.be::getOpticalClarityPct, this::setOpticalClarityPct);
        data.registerInt(this.be::getMaxReachTier, this::setMaxReachTier);
        data.registerInt(this.be::getSkyClearFlag, this::setSkyClearFlag);
        data.registerInt(this.be::getNightOrSpaceFlag, this::setNightOrSpaceFlag);
        data.registerInt(this.be::getApertureTier, this::setApertureTier);
        data.registerInt(this.be::getOpticsTier, this::setOpticsTier);
        data.registerInt(this.be::getAuxDriveType, this::setAuxDriveType);
        data.registerInt(this.be::getAirmassHundredths, this::setAirmassHundredths);
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

    public int getSelectedTargetIndex() {
        return this.selectedTargetIndex;
    }

    public void setSelectedTargetIndex(int selectedTargetIndex) {
        this.selectedTargetIndex = selectedTargetIndex;
    }

    public int getOpticalClarityPct() {
        return this.opticalClarityPct;
    }

    public void setOpticalClarityPct(int opticalClarityPct) {
        this.opticalClarityPct = opticalClarityPct;
    }

    public int getMaxReachTier() {
        return this.maxReachTier;
    }

    public void setMaxReachTier(int maxReachTier) {
        this.maxReachTier = maxReachTier;
    }

    public int getSkyClearFlag() {
        return this.skyClearFlag;
    }

    public void setSkyClearFlag(int skyClearFlag) {
        this.skyClearFlag = skyClearFlag;
    }

    public int getNightOrSpaceFlag() {
        return this.nightOrSpaceFlag;
    }

    public void setNightOrSpaceFlag(int nightOrSpaceFlag) {
        this.nightOrSpaceFlag = nightOrSpaceFlag;
    }

    public int getApertureTier() {
        return this.apertureTier;
    }

    public void setApertureTier(int apertureTier) {
        this.apertureTier = apertureTier;
    }

    public int getOpticsTier() {
        return this.opticsTier;
    }

    public void setOpticsTier(int opticsTier) {
        this.opticsTier = opticsTier;
    }

    public int getAuxDriveType() {
        return this.auxDriveType;
    }

    public void setAuxDriveType(int auxDriveType) {
        this.auxDriveType = auxDriveType;
    }

    public int getAirmassHundredths() {
        return this.airmassHundredths;
    }

    public void setAirmassHundredths(int airmassHundredths) {
        this.airmassHundredths = airmassHundredths;
    }
}
