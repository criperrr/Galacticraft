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
import dev.galacticraft.mod.content.block.entity.machine.AtmosphericFractionatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AtmosphericFractionatorMenu extends MachineMenu<AtmosphericFractionatorBlockEntity> {
    private int progress;
    private int intakePressurePct;
    private int planetCode;
    private int pressureMilliAtm;
    private final int[] channelFluidIds = new int[]{-1, -1, -1, -1};

    public AtmosphericFractionatorMenu(int syncId, @NotNull ServerPlayer player, @NotNull AtmosphericFractionatorBlockEntity machine) {
        super(GCMenuTypes.ATMOSPHERIC_FRACTIONATOR, syncId, player, machine);
    }

    public AtmosphericFractionatorMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.ATMOSPHERIC_FRACTIONATOR, syncId, inventory, pos, 8, 122);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getProgress, this::setProgress);
        data.registerInt(this.be::getIntakePressurePct, this::setIntakePressurePct);
        data.registerInt(this.be::getPlanetCode, this::setPlanetCode);
        data.registerInt(this.be::getPressureMilliAtm, this::setPressureMilliAtm);
        data.registerInt(() -> this.be.getChannelFluidId(0), v -> this.channelFluidIds[0] = v);
        data.registerInt(() -> this.be.getChannelFluidId(1), v -> this.channelFluidIds[1] = v);
        data.registerInt(() -> this.be.getChannelFluidId(2), v -> this.channelFluidIds[2] = v);
        data.registerInt(() -> this.be.getChannelFluidId(3), v -> this.channelFluidIds[3] = v);
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getIntakePressurePct() {
        return this.intakePressurePct;
    }

    public void setIntakePressurePct(int intakePressurePct) {
        this.intakePressurePct = intakePressurePct;
    }

    public int getPlanetCode() {
        return this.planetCode;
    }

    public void setPlanetCode(int planetCode) {
        this.planetCode = planetCode;
    }

    public int getPressureMilliAtm() {
        return this.pressureMilliAtm;
    }

    public void setPressureMilliAtm(int pressureMilliAtm) {
        this.pressureMilliAtm = pressureMilliAtm;
    }

    public int getChannelFluidId(int index) {
        return index >= 0 && index < 4 ? this.channelFluidIds[index] : -1;
    }
}
