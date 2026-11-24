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
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchDomain;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.research.ResearchMilestone;
import dev.galacticraft.mod.research.TeamResearchState;
import dev.galacticraft.mod.screen.ResearchDataLinkMenu;
import dev.galacticraft.mod.tag.GCItemTags;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interplanetary Research Data Link ({@code RESEARCH_DATA_LINK}).
 * Features motorized 3D parabolic dish aiming, interactive Dual-Wave Carrier Phase Lock (CommNet style),
 * and 2 exclusive RF/optical upgrade bays:
 * <ul>
 *   <li><b>RF Feedhorn:</b> {@code LOW_NOISE_RF_AMPLIFIER} or {@code DEEP_SPACE_KA_BAND_FEED}</li>
 *   <li><b>Carrier Modulator:</b> {@code PHASE_LOCKED_LOOP_MODULE} or {@code LASER_COMM_TRANSPONDER}</li>
 * </ul>
 */
public class ResearchDataLinkBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int FREQUENCY_MODULE_SLOT = 1;
    public static final int RF_FEED_UPGRADE_SLOT = 2;
    public static final int MODULATOR_UPGRADE_SLOT = 3;

    public static final long ENERGY_CAPACITY = 30_000L;
    public static final long ENERGY_CONSUMPTION = 20L;
    public static final int SCAN_RADIUS = 16;
    public static final int HEARTBEAT_INTERVAL_TICKS = 40;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER).pos(8, 78).capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(198, 26).capacity(1)
                            .filter((item, components) -> item == GCItems.FREQUENCY_MODULE || new ItemStack(item).is(GCItemTags.FREQUENCY_MODULES)),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(198, 54).capacity(1)
                            .filter((item, c) -> item == GCItems.LOW_NOISE_RF_AMPLIFIER || item == GCItems.DEEP_SPACE_KA_BAND_FEED),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(198, 78).capacity(1)
                            .filter((item, c) -> item == GCItems.PHASE_LOCKED_LOOP_MODULE || item == GCItems.LASER_COMM_TRANSPONDER)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            )
    );

    private int frequencyChannel = 1;
    private int linkedLabCount = 0;
    private int signalStrengthPct = 0;
    private int heartbeatTimer = 0;
    private boolean registeredUnlock = false;
    private int dishAzimuthDeg = 45;
    private int dishElevationDeg = 52;
    private int phaseLockQualityPct = 75;
    private int rfFeedTier = 0;     // 0=base, 1=Low-Noise RF Amp, 2=Deep-Space Ka-Band Feed
    private int modulatorTier = 0;  // 0=base, 1=PLL Module, 2=Laser Comm Transponder

    public ResearchDataLinkBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.RESEARCH_DATA_LINK, pos, state, SPEC);
    }

    public void adjustFrequencyChannel(int delta) {
        int next = this.frequencyChannel + delta;
        if (next < 1) next = 16;
        if (next > 16) next = 1;
        this.frequencyChannel = next;
        this.heartbeatTimer = HEARTBEAT_INTERVAL_TICKS;
        this.setChanged();
    }

    public void adjustDishAim(int dAzimuth, int dElevation) {
        this.dishAzimuthDeg = Math.floorMod(this.dishAzimuthDeg + dAzimuth, 360);
        this.dishElevationDeg = Mth.clamp(this.dishElevationDeg + dElevation, 10, 88);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Server-validated CommNet carrier phase lock triggered from the oscilloscope HUD.
     */
    public boolean lockCarrierPhase(@NotNull ServerLevel level, int qualityPct) {
        if (this.itemStorage().slot(FREQUENCY_MODULE_SLOT).isEmpty()) {
            return false;
        }
        this.phaseLockQualityPct = Mth.clamp(qualityPct, 50, 100);
        this.heartbeatTimer = HEARTBEAT_INTERVAL_TICKS;
        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(level);
        ResearchManager.pulseRelay(
                level, env.displayName(), this.worldPosition, this.frequencyChannel,
                scanNearbyLaboratories(level, this.worldPosition),
                this.rfFeedTier == 2, this.modulatorTier == 2
        );
        ResearchManager.recordScienceExperiment(
                level,
                "commnet_lock_ch_" + this.frequencyChannel,
                "CommNet Carrier Lock CH-" + String.format("%02d", this.frequencyChannel),
                "Research Data Link",
                env.displayName(),
                ResearchDomain.ELECTROMAGNETISM,
                28,
                18,
                0.80F + (this.phaseLockQualityPct / 100.0F) * 0.45F,
                ResearchCatalog.INTERPLANETARY_SCIENCE_NETWORK,
                ResearchMilestone.COMMNET_RELAY_LOCKED
        );
        this.registeredUnlock = true;
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        Item rf = this.itemStorage().slot(RF_FEED_UPGRADE_SLOT).getResource();
        this.rfFeedTier = rf == GCItems.DEEP_SPACE_KA_BAND_FEED ? 2 : (rf == GCItems.LOW_NOISE_RF_AMPLIFIER ? 1 : 0);
        Item mod = this.itemStorage().slot(MODULATOR_UPGRADE_SLOT).getResource();
        this.modulatorTier = mod == GCItems.LASER_COMM_TRANSPONDER ? 2 : (mod == GCItems.PHASE_LOCKED_LOOP_MODULE ? 1 : 0);

        ItemResourceSlot moduleSlot = this.itemStorage().slot(FREQUENCY_MODULE_SLOT);
        if (moduleSlot.isEmpty()) {
            this.signalStrengthPct = Math.max(0, this.signalStrengthPct - 15);
            return GCMachineStatuses.MISSING_FREQUENCY_MODULE;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (drawn < ENERGY_CONSUMPTION) {
                this.signalStrengthPct = Math.max(0, this.signalStrengthPct - 15);
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        int prevSig = this.signalStrengthPct;
        boolean hasSky = level.canSeeSky(pos.above());
        int targetSignal = hasSky ? 100 : (this.rfFeedTier >= 1 ? 88 : 72);
        if (this.signalStrengthPct < targetSignal) {
            this.signalStrengthPct = Math.min(targetSignal, this.signalStrengthPct + 12);
        } else if (this.signalStrengthPct > targetSignal) {
            this.signalStrengthPct = Math.max(targetSignal, this.signalStrengthPct - 8);
        }
        if ((prevSig == 0) != (this.signalStrengthPct == 0)) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        this.heartbeatTimer++;
        if (this.heartbeatTimer >= HEARTBEAT_INTERVAL_TICKS || !this.registeredUnlock) {
            this.heartbeatTimer = 0;
            this.linkedLabCount = scanNearbyLaboratories(level, pos);
            PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(level);
            ResearchManager.pulseRelay(
                    level, env.displayName(), pos, this.frequencyChannel, this.linkedLabCount,
                    this.rfFeedTier == 2, this.modulatorTier == 2
            );

            if (!this.registeredUnlock) {
                this.registeredUnlock = true;
                TeamResearchState research = ResearchManager.get(level);
                if (!research.isUnlocked(ResearchCatalog.INTERPLANETARY_SCIENCE_NETWORK)) {
                    ResearchManager.recordScienceExperiment(
                            level,
                            "commnet_uplink_" + this.frequencyChannel,
                            "Relay Uplink CH-" + String.format("%02d", this.frequencyChannel),
                            "Research Data Link",
                            env.displayName(),
                            ResearchDomain.ELECTROMAGNETISM,
                            24,
                            18,
                            1.0F,
                            ResearchCatalog.INTERPLANETARY_SCIENCE_NETWORK,
                            ResearchMilestone.COMMNET_RELAY_LOCKED
                    );
                }
            }
        }

        return GCMachineStatuses.UPLINK_ACTIVE;
    }

    private static int scanNearbyLaboratories(@NotNull ServerLevel level, @NotNull BlockPos center) {
        int count = 0;
        int minChunkX = (center.getX() - SCAN_RADIUS) >> 4;
        int maxChunkX = (center.getX() + SCAN_RADIUS) >> 4;
        int minChunkZ = (center.getZ() - SCAN_RADIUS) >> 4;
        int maxChunkZ = (center.getZ() + SCAN_RADIUS) >> 4;
        int rSq = SCAN_RADIUS * SCAN_RADIUS;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be == null || be.getBlockPos().equals(center)) {
                        continue;
                    }
                    if (be.getBlockPos().distSqr(center) <= rSq && isLaboratoryMachine(be)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean isLaboratoryMachine(BlockEntity be) {
        return be instanceof ResearchWorkbenchBlockEntity
                || be instanceof ObservatoryTelescopeBlockEntity
                || be instanceof GeologicalAnalyzerBlockEntity
                || be instanceof EnvironmentalTestChamberBlockEntity
                || be instanceof ZeroGResearchChamberBlockEntity
                || be instanceof AerospaceRdTerminalBlockEntity;
    }

    public int getFrequencyChannel() {
        return this.frequencyChannel;
    }

    public int getLinkedLabCount() {
        return this.linkedLabCount;
    }

    public int getSignalStrengthPct() {
        return this.signalStrengthPct;
    }

    public int getDishAzimuthDeg() {
        return this.dishAzimuthDeg;
    }

    public int getDishElevationDeg() {
        return this.dishElevationDeg;
    }

    public int getPhaseLockQualityPct() {
        return this.phaseLockQualityPct;
    }

    public int getRfFeedTier() {
        return this.rfFeedTier;
    }

    public int getModulatorTier() {
        return this.modulatorTier;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("frequency_channel", this.frequencyChannel);
        tag.putBoolean("registered_unlock", this.registeredUnlock);
        tag.putInt("dish_az", this.dishAzimuthDeg);
        tag.putInt("dish_el", this.dishElevationDeg);
        tag.putInt("phase_lock", this.phaseLockQualityPct);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.frequencyChannel = tag.contains("frequency_channel") ? Mth.clamp(tag.getInt("frequency_channel"), 1, 16) : 1;
        this.registeredUnlock = tag.getBoolean("registered_unlock");
        if (tag.contains("dish_az")) {
            this.dishAzimuthDeg = Math.floorMod(tag.getInt("dish_az"), 360);
        }
        if (tag.contains("dish_el")) {
            this.dishElevationDeg = Mth.clamp(tag.getInt("dish_el"), 10, 88);
        }
        if (tag.contains("phase_lock")) {
            this.phaseLockQualityPct = Mth.clamp(tag.getInt("phase_lock"), 0, 100);
        }
        if (tag.contains("signal_pct")) {
            this.signalStrengthPct = Mth.clamp(tag.getInt("signal_pct"), 0, 100);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("dish_az", this.dishAzimuthDeg);
        tag.putInt("dish_el", this.dishElevationDeg);
        tag.putInt("signal_pct", this.signalStrengthPct);
        return tag;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchManager.syncToPlayer(serverPlayer);
            return new ResearchDataLinkMenu(syncId, serverPlayer, this);
        }
        return null;
    }
}
