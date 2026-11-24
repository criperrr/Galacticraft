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
import dev.galacticraft.mod.content.block.entity.machine.AerospaceRdTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AerospaceRdTerminalMenu extends MachineMenu<AerospaceRdTerminalBlockEntity> {
    private int totalUnlockedCount;
    private int mappedBodyCount = 1;
    private int activeRelayCount;
    private int networkBonusPct = 100;
    private int upgradeTier;

    public AerospaceRdTerminalMenu(int syncId, @NotNull ServerPlayer player, @NotNull AerospaceRdTerminalBlockEntity machine) {
        super(GCMenuTypes.AEROSPACE_RD_TERMINAL, syncId, player, machine);
    }

    public AerospaceRdTerminalMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.AEROSPACE_RD_TERMINAL, syncId, inventory, pos, 8, 172);
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getTotalUnlockedCount, this::setTotalUnlockedCount);
        data.registerInt(this.be::getMappedBodyCount, this::setMappedBodyCount);
        data.registerInt(this.be::getActiveRelayCount, this::setActiveRelayCount);
        data.registerInt(this.be::getNetworkBonusPct, this::setNetworkBonusPct);
        data.registerInt(this.be::getUpgradeTier, this::setUpgradeTier);
    }

    public int getTotalUnlockedCount() {
        return this.totalUnlockedCount;
    }

    public void setTotalUnlockedCount(int totalUnlockedCount) {
        this.totalUnlockedCount = totalUnlockedCount;
    }

    public int getMappedBodyCount() {
        return this.mappedBodyCount;
    }

    public void setMappedBodyCount(int mappedBodyCount) {
        this.mappedBodyCount = mappedBodyCount;
    }

    public int getActiveRelayCount() {
        return this.activeRelayCount;
    }

    public void setActiveRelayCount(int activeRelayCount) {
        this.activeRelayCount = activeRelayCount;
    }

    public int getNetworkBonusPct() {
        return this.networkBonusPct;
    }

    public void setNetworkBonusPct(int networkBonusPct) {
        this.networkBonusPct = networkBonusPct;
    }

    public int getUpgradeTier() {
        return this.upgradeTier;
    }

    public void setUpgradeTier(int upgradeTier) {
        this.upgradeTier = upgradeTier;
    }
}
