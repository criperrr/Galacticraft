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
import dev.galacticraft.mod.content.block.entity.machine.ZeroGResearchChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ZeroGResearchChamberMenu extends MachineMenu<ZeroGResearchChamberBlockEntity> {
    private int progress;
    private int activeExperimentId;
    private int localGravityMilliG = 1000;
    private int microgravityValidFlag;
    private int domeTier = 1;
    private int confinementTier;
    private int cryoGimbalTier;

    public ZeroGResearchChamberMenu(int syncId, @NotNull ServerPlayer player, @NotNull ZeroGResearchChamberBlockEntity machine) {
        super(GCMenuTypes.ZERO_G_RESEARCH_CHAMBER, syncId, player, machine);
    }

    public ZeroGResearchChamberMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.ZERO_G_RESEARCH_CHAMBER, syncId, inventory, pos, 32, 126);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getActiveExperimentId, this::setActiveExperimentId);
        data.registerInt(this.be::getLocalGravityMilliG, this::setLocalGravityMilliG);
        data.registerInt(this.be::getMicrogravityValidFlag, this::setMicrogravityValidFlag);
        data.registerInt(this.be::getDomeTier, this::setDomeTier);
        data.registerInt(this.be::getConfinementTier, this::setConfinementTier);
        data.registerInt(this.be::getCryoGimbalTier, this::setCryoGimbalTier);
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getActiveExperimentId() {
        return this.activeExperimentId;
    }

    public void setActiveExperimentId(int activeExperimentId) {
        this.activeExperimentId = activeExperimentId;
    }

    public int getLocalGravityMilliG() {
        return this.localGravityMilliG;
    }

    public void setLocalGravityMilliG(int localGravityMilliG) {
        this.localGravityMilliG = localGravityMilliG;
    }

    public int getMicrogravityValidFlag() {
        return this.microgravityValidFlag;
    }

    public void setMicrogravityValidFlag(int microgravityValidFlag) {
        this.microgravityValidFlag = microgravityValidFlag;
    }

    public int getDomeTier() {
        return this.domeTier;
    }

    public void setDomeTier(int domeTier) {
        this.domeTier = domeTier;
    }

    public int getConfinementTier() {
        return this.confinementTier;
    }

    public void setConfinementTier(int confinementTier) {
        this.confinementTier = confinementTier;
    }

    public int getCryoGimbalTier() {
        return this.cryoGimbalTier;
    }

    public void setCryoGimbalTier(int cryoGimbalTier) {
        this.cryoGimbalTier = cryoGimbalTier;
    }
}
