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
import dev.galacticraft.mod.content.block.entity.machine.ResearchDataLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ResearchDataLinkMenu extends MachineMenu<ResearchDataLinkBlockEntity> {
    private int frequencyChannel = 1;
    private int linkedLabCount;
    private int signalStrengthPct;
    private int dishAzimuthDeg = 45;
    private int dishElevationDeg = 52;
    private int phaseLockQualityPct = 75;
    private int rfFeedTier;
    private int modulatorTier;

    public ResearchDataLinkMenu(int syncId, @NotNull ServerPlayer player, @NotNull ResearchDataLinkBlockEntity machine) {
        super(GCMenuTypes.RESEARCH_DATA_LINK, syncId, player, machine);
    }

    public ResearchDataLinkMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.RESEARCH_DATA_LINK, syncId, inventory, pos, 32, 126);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getFrequencyChannel, this::setFrequencyChannel);
        data.registerInt(this.be::getLinkedLabCount, this::setLinkedLabCount);
        data.registerInt(this.be::getSignalStrengthPct, this::setSignalStrengthPct);
        data.registerInt(this.be::getDishAzimuthDeg, this::setDishAzimuthDeg);
        data.registerInt(this.be::getDishElevationDeg, this::setDishElevationDeg);
        data.registerInt(this.be::getPhaseLockQualityPct, this::setPhaseLockQualityPct);
        data.registerInt(this.be::getRfFeedTier, this::setRfFeedTier);
        data.registerInt(this.be::getModulatorTier, this::setModulatorTier);
    }

    public int getFrequencyChannel() {
        return this.frequencyChannel;
    }

    public void setFrequencyChannel(int frequencyChannel) {
        this.frequencyChannel = frequencyChannel;
    }

    public int getLinkedLabCount() {
        return this.linkedLabCount;
    }

    public void setLinkedLabCount(int linkedLabCount) {
        this.linkedLabCount = linkedLabCount;
    }

    public int getSignalStrengthPct() {
        return this.signalStrengthPct;
    }

    public void setSignalStrengthPct(int signalStrengthPct) {
        this.signalStrengthPct = signalStrengthPct;
    }

    public int getDishAzimuthDeg() {
        return this.dishAzimuthDeg;
    }

    public void setDishAzimuthDeg(int dishAzimuthDeg) {
        this.dishAzimuthDeg = dishAzimuthDeg;
    }

    public int getDishElevationDeg() {
        return this.dishElevationDeg;
    }

    public void setDishElevationDeg(int dishElevationDeg) {
        this.dishElevationDeg = dishElevationDeg;
    }

    public int getPhaseLockQualityPct() {
        return this.phaseLockQualityPct;
    }

    public void setPhaseLockQualityPct(int phaseLockQualityPct) {
        this.phaseLockQualityPct = phaseLockQualityPct;
    }

    public int getRfFeedTier() {
        return this.rfFeedTier;
    }

    public void setRfFeedTier(int rfFeedTier) {
        this.rfFeedTier = rfFeedTier;
    }

    public int getModulatorTier() {
        return this.modulatorTier;
    }

    public void setModulatorTier(int modulatorTier) {
        this.modulatorTier = modulatorTier;
    }
}
