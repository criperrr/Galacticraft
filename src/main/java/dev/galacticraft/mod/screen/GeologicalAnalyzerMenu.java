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
import dev.galacticraft.mod.content.block.entity.machine.GeologicalAnalyzerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GeologicalAnalyzerMenu extends MachineMenu<GeologicalAnalyzerBlockEntity> {
    private int progress;
    private int activeAssayId;
    private int reagentBoostPct = 100;
    private int nativeInSituBonus;
    private int lastAwardedPoints;
    private int laserTier;
    private int detectorTier;

    public GeologicalAnalyzerMenu(int syncId, @NotNull ServerPlayer player, @NotNull GeologicalAnalyzerBlockEntity machine) {
        super(GCMenuTypes.GEOLOGICAL_ANALYZER, syncId, player, machine);
    }

    public GeologicalAnalyzerMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.GEOLOGICAL_ANALYZER, syncId, inventory, pos, 32, 126);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getActiveAssayId, this::setActiveAssayId);
        data.registerInt(this.be::getReagentBoostPct, this::setReagentBoostPct);
        data.registerInt(this.be::getNativeInSituBonus, this::setNativeInSituBonus);
        data.registerInt(this.be::getLastAwardedPoints, this::setLastAwardedPoints);
        data.registerInt(this.be::getLaserTier, this::setLaserTier);
        data.registerInt(this.be::getDetectorTier, this::setDetectorTier);
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getActiveAssayId() {
        return this.activeAssayId;
    }

    public void setActiveAssayId(int activeAssayId) {
        this.activeAssayId = activeAssayId;
    }

    public int getReagentBoostPct() {
        return this.reagentBoostPct;
    }

    public void setReagentBoostPct(int reagentBoostPct) {
        this.reagentBoostPct = reagentBoostPct;
    }

    public int getNativeInSituBonus() {
        return this.nativeInSituBonus;
    }

    public void setNativeInSituBonus(int nativeInSituBonus) {
        this.nativeInSituBonus = nativeInSituBonus;
    }

    public int getLastAwardedPoints() {
        return this.lastAwardedPoints;
    }

    public void setLastAwardedPoints(int lastAwardedPoints) {
        this.lastAwardedPoints = lastAwardedPoints;
    }

    public int getLaserTier() {
        return this.laserTier;
    }

    public void setLaserTier(int laserTier) {
        this.laserTier = laserTier;
    }

    public int getDetectorTier() {
        return this.detectorTier;
    }

    public void setDetectorTier(int detectorTier) {
        this.detectorTier = detectorTier;
    }
}
