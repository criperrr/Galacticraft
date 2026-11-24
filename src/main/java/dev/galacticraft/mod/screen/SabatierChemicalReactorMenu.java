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
import dev.galacticraft.mod.content.block.entity.machine.SabatierChemicalReactorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class SabatierChemicalReactorMenu extends MachineMenu<SabatierChemicalReactorBlockEntity> {
    private int progress;
    private int maxProgress = 40;
    private int temperatureC = SabatierChemicalReactorBlockEntity.AMBIENT_TEMP_C;
    private int targetTempC = 350;
    private int catalystTier;
    private int activeReactionCode;

    public SabatierChemicalReactorMenu(int syncId, @NotNull ServerPlayer player, @NotNull SabatierChemicalReactorBlockEntity machine) {
        super(GCMenuTypes.SABATIER_CHEMICAL_REACTOR, syncId, player, machine);
    }

    public SabatierChemicalReactorMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.SABATIER_CHEMICAL_REACTOR, syncId, inventory, pos, 8, 122);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getMaxProgress, this::setMaxProgress);
        data.registerInt(this.be::getTemperatureC, this::setTemperatureC);
        data.registerInt(this.be::getTargetTempC, this::setTargetTempC);
        data.registerInt(this.be::getCatalystTier, this::setCatalystTier);
        data.registerInt(this.be::getActiveReactionCode, this::setActiveReactionCode);
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

    public int getTemperatureC() {
        return this.temperatureC;
    }

    public void setTemperatureC(int temperatureC) {
        this.temperatureC = temperatureC;
    }

    public int getTargetTempC() {
        return this.targetTempC;
    }

    public void setTargetTempC(int targetTempC) {
        this.targetTempC = Math.max(1, targetTempC);
    }

    public int getCatalystTier() {
        return this.catalystTier;
    }

    public void setCatalystTier(int catalystTier) {
        this.catalystTier = catalystTier;
    }

    public int getActiveReactionCode() {
        return this.activeReactionCode;
    }

    public void setActiveReactionCode(int activeReactionCode) {
        this.activeReactionCode = activeReactionCode;
    }
}
