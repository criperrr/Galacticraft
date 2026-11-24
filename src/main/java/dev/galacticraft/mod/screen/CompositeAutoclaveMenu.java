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
import dev.galacticraft.mod.content.block.entity.machine.CompositeAutoclaveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class CompositeAutoclaveMenu extends MachineMenu<CompositeAutoclaveBlockEntity> {
    private int temperature = CompositeAutoclaveBlockEntity.AMBIENT_TEMPERATURE;
    private int pressureBar = CompositeAutoclaveBlockEntity.AMBIENT_PRESSURE_BAR;
    private int targetTemperature = 450;
    private int targetPressureBar = 12;
    private int progress;
    private int maxProgress = 120;

    public CompositeAutoclaveMenu(int syncId, @NotNull ServerPlayer player, @NotNull CompositeAutoclaveBlockEntity machine) {
        super(GCMenuTypes.COMPOSITE_AUTOCLAVE, syncId, player, machine);
    }

    public CompositeAutoclaveMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.COMPOSITE_AUTOCLAVE, syncId, inventory, pos, 8, 110);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getTemperature, this::setTemperature);
        data.registerInt(this.be::getPressureBar, this::setPressureBar);
        data.registerInt(this.be::getTargetTemperature, this::setTargetTemperature);
        data.registerInt(this.be::getTargetPressureBar, this::setTargetPressureBar);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getMaxProgress, this::setMaxProgress);
    }

    public int getTemperature() {
        return this.temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getPressureBar() {
        return this.pressureBar;
    }

    public void setPressureBar(int pressureBar) {
        this.pressureBar = pressureBar;
    }

    public int getTargetTemperature() {
        return this.targetTemperature;
    }

    public void setTargetTemperature(int targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public int getTargetPressureBar() {
        return this.targetPressureBar;
    }

    public void setTargetPressureBar(int targetPressureBar) {
        this.targetPressureBar = targetPressureBar;
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
}
