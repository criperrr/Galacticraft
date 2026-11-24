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

package dev.galacticraft.mod.content.block.entity.machine;

import com.mojang.datafixers.util.Pair;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.research.ResearchNode;
import dev.galacticraft.mod.research.TeamResearchState;
import dev.galacticraft.mod.screen.AerospaceRdTerminalMenu;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central Aerospace R&amp;D Command Terminal ({@code AEROSPACE_RD_TERMINAL}) powering the KSP-style
 * Pan &amp; Zoom Tech Tree, Science Archives, Astro Atlas &amp; CommNet dashboard, schematic etching bay,
 * and dedicated Archive Core upgrade bay ({@code TELEMETRY_COPROCESSOR_CARD} / {@code QUANTUM_ARCHIVE_CORE}).
 */
public class AerospaceRdTerminalBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int SCHEMATIC_MEDIA_SLOT = 1;
    public static final int SCHEMATIC_OUTPUT_SLOT = 2;
    public static final int UPGRADE_SLOT = 3;

    public static final long ENERGY_CAPACITY = 50_000L;
    public static final long ENERGY_STANDBY = 15L;
    public static final long ETCH_ENERGY_COST = 500L;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(176, 228)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(204, 192)
                            .filter((item, c) -> item == Items.PAPER || item == GCItems.BASIC_WAFER || item == GCItems.ADVANCED_WAFER),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(244, 192),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(204, 228)
                            .capacity(1)
                            .filter((item, c) -> item == GCItems.TELEMETRY_COPROCESSOR_CARD || item == GCItems.QUANTUM_ARCHIVE_CORE)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    200L,
                    0
            )
    );

    private int totalUnlockedCount = 0;
    private int mappedBodyCount = 1;
    private int activeRelayCount = 0;
    private int networkBonusPct = 100;
    private int upgradeTier = 0; // 0=none, 1=coprocessor, 2=quantum archive core

    public AerospaceRdTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.AEROSPACE_RD_TERMINAL, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        TeamResearchState research = ResearchManager.get(level);
        this.totalUnlockedCount = research.getUnlockedNodes().size();
        this.mappedBodyCount = research.getMappedBodies().size();
        this.activeRelayCount = research.getActiveRelayCount();
        this.networkBonusPct = (int) Math.round(research.getNetworkBonusMultiplier() * 100.0);

        Item upgradeItem = this.itemStorage().slot(UPGRADE_SLOT).getResource();
        this.upgradeTier = upgradeItem == GCItems.QUANTUM_ARCHIVE_CORE ? 2
                : (upgradeItem == GCItems.TELEMETRY_COPROCESSOR_CARD ? 1 : 0);

        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(ENERGY_STANDBY, tx);
            if (drawn < ENERGY_STANDBY) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        if (this.upgradeTier == 2 && !research.isQuantumArchiveBoost()) {
            research.setQuantumArchiveBoost(true);
            ResearchManager.broadcastToAll(level.getServer(), research);
        }
        return GCMachineStatuses.UPLINK_ACTIVE;
    }

    /**
     * Etches an aerospace Part Schematic matching the branch/tier of an unlocked research node
     * using media (Paper / Wafer) in {@link #SCHEMATIC_MEDIA_SLOT}.
     */
    public boolean tryEtchSchematicForNode(
            @NotNull ServerLevel level,
            @NotNull ServerPlayer player,
            @NotNull ResourceLocation nodeId
    ) {
        ResearchNode node = ResearchCatalog.get(nodeId);
        if (node == null) {
            return false;
        }
        TeamResearchState research = ResearchManager.get(level);
        if (!player.isCreative() && !research.isUnlocked(nodeId)) {
            return false;
        }

        Item schematic = resolveSchematicForNode(node);
        ItemResourceSlot mediaSlot = this.itemStorage().slot(SCHEMATIC_MEDIA_SLOT);
        ItemResourceSlot outSlot = this.itemStorage().slot(SCHEMATIC_OUTPUT_SLOT);

        if (mediaSlot.isEmpty() || !outSlot.canInsert(schematic, DataComponentPatch.EMPTY, 1L)) {
            return false;
        }

        long cost = this.upgradeTier >= 1 ? Math.round(ETCH_ENERGY_COST * 0.85) : ETCH_ENERGY_COST;
        try (Transaction tx = Transaction.openOuter()) {
            if (!player.isCreative()) {
                long drawn = this.energyStorage().extract(cost, tx);
                if (drawn < cost) {
                    return false;
                }
            }
            long ex = mediaSlot.extract(mediaSlot.getResource(), DataComponentPatch.EMPTY, 1L, tx);
            if (ex < 1L) {
                return false;
            }
            long ins = outSlot.insert(schematic, DataComponentPatch.EMPTY, 1L, tx);
            if (ins < 1L) {
                return false;
            }
            tx.commit();
        }
        this.setChanged();
        return true;
    }

    private static Item resolveSchematicForNode(@NotNull ResearchNode node) {
        return switch (node.branch()) {
            case METALLURGY_MACHINES -> GCItems.PART_SCHEMATIC_ADAPTER;
            case LIFE_SUPPORT_BASES -> GCItems.PART_SCHEMATIC_TANK;
            case ENERGY_SUPERCONDUCTORS -> GCItems.PART_SCHEMATIC_UTILITY;
            case SUITS_TOOLS -> GCItems.PART_SCHEMATIC_PAYLOAD;
            case AEROSPACE_PROPULSION -> node.era() >= 3 ? GCItems.PART_SCHEMATIC_ENGINE : GCItems.PART_SCHEMATIC_COMMAND;
        };
    }

    public int getTotalUnlockedCount() {
        return this.totalUnlockedCount;
    }

    public int getMappedBodyCount() {
        return this.mappedBodyCount;
    }

    public int getActiveRelayCount() {
        return this.activeRelayCount;
    }

    public int getNetworkBonusPct() {
        return this.networkBonusPct;
    }

    public int getUpgradeTier() {
        return this.upgradeTier;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchManager.syncToPlayer(serverPlayer);
            return new AerospaceRdTerminalMenu(syncId, serverPlayer, this);
        }
        return null;
    }
}
