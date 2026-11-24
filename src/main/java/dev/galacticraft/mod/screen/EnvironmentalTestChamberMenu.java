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
import dev.galacticraft.mod.content.block.entity.machine.EnvironmentalTestChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class EnvironmentalTestChamberMenu extends MachineMenu<EnvironmentalTestChamberBlockEntity> {
    private int progress;
    private int envModeCode;
    private int ambientPressureMilliAtm = 1000;
    private int ambientTempCelsius = 21;
    private int corrosiveAcidFlag;
    private int activeEnergyCost = (int) EnvironmentalTestChamberBlockEntity.SIMULATED_VACUUM_ENERGY;
    private int manualSimOverride;
    private int pressureUpgradeTier;
    private int thermalUpgradeTier;

    public EnvironmentalTestChamberMenu(int syncId, @NotNull ServerPlayer player, @NotNull EnvironmentalTestChamberBlockEntity machine) {
        super(GCMenuTypes.ENVIRONMENTAL_TEST_CHAMBER, syncId, player, machine);
    }

    public EnvironmentalTestChamberMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.ENVIRONMENTAL_TEST_CHAMBER, syncId, inventory, pos, 32, 126);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getEnvModeCode, this::setEnvModeCode);
        data.registerInt(this.be::getAmbientPressureMilliAtm, this::setAmbientPressureMilliAtm);
        data.registerInt(this.be::getAmbientTempCelsius, this::setAmbientTempCelsius);
        data.registerInt(this.be::getCorrosiveAcidFlag, this::setCorrosiveAcidFlag);
        data.registerInt(this.be::getActiveEnergyCost, this::setActiveEnergyCost);
        data.registerInt(this.be::getManualSimOverride, this::setManualSimOverride);
        data.registerInt(this.be::getPressureUpgradeTier, this::setPressureUpgradeTier);
        data.registerInt(this.be::getThermalUpgradeTier, this::setThermalUpgradeTier);
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getEnvModeCode() {
        return this.envModeCode;
    }

    public void setEnvModeCode(int envModeCode) {
        this.envModeCode = envModeCode;
    }

    public int getAmbientPressureMilliAtm() {
        return this.ambientPressureMilliAtm;
    }

    public void setAmbientPressureMilliAtm(int ambientPressureMilliAtm) {
        this.ambientPressureMilliAtm = ambientPressureMilliAtm;
    }

    public int getAmbientTempCelsius() {
        return this.ambientTempCelsius;
    }

    public void setAmbientTempCelsius(int ambientTempCelsius) {
        this.ambientTempCelsius = ambientTempCelsius;
    }

    public int getCorrosiveAcidFlag() {
        return this.corrosiveAcidFlag;
    }

    public void setCorrosiveAcidFlag(int corrosiveAcidFlag) {
        this.corrosiveAcidFlag = corrosiveAcidFlag;
    }

    public int getActiveEnergyCost() {
        return this.activeEnergyCost;
    }

    public void setActiveEnergyCost(int activeEnergyCost) {
        this.activeEnergyCost = activeEnergyCost;
    }

    public int getManualSimOverride() {
        return this.manualSimOverride;
    }

    public void setManualSimOverride(int manualSimOverride) {
        this.manualSimOverride = manualSimOverride;
    }

    public int getPressureUpgradeTier() {
        return this.pressureUpgradeTier;
    }

    public void setPressureUpgradeTier(int pressureUpgradeTier) {
        this.pressureUpgradeTier = pressureUpgradeTier;
    }

    public int getThermalUpgradeTier() {
        return this.thermalUpgradeTier;
    }

    public void setThermalUpgradeTier(int thermalUpgradeTier) {
        this.thermalUpgradeTier = thermalUpgradeTier;
    }
}
