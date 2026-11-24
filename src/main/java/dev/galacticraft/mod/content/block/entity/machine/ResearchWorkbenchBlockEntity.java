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

import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchDomain;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.screen.ResearchWorkbenchMenu;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Early-game non-electric Research &amp; Prototyping Workbench ({@code RESEARCH_WORKBENCH}).
 * Features interactive 3-axis Blueprint Tolerance Calibration (Rigidity, Conductivity, Thermal Margin)
 * and 2 dedicated workbench-only upgrade bays:
 * <ul>
 *   <li><b>Metrology Bay:</b> {@code PRECISION_VERNIER_CALIPER} or {@code OPTICAL_BLUEPRINT_PROJECTOR}</li>
 *   <li><b>Calibration Arm:</b> {@code CAD_HOLOGRAPHIC_STYLUS}</li>
 * </ul>
 */
public class ResearchWorkbenchBlockEntity extends MachineBlockEntity {
    public static final int SPECIMEN_SLOT = 0;
    public static final int CATALYST_SLOT = 1;
    public static final int PAPER_SLOT = 2;
    public static final int PROTOTYPE_OUTPUT_SLOT = 3;
    public static final int METROLOGY_UPGRADE_SLOT = 4;
    public static final int STYLUS_UPGRADE_SLOT = 5;

    public static final int DEFAULT_DURATION = 60;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.INPUT).pos(12, 26),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(12, 48),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(12, 70)
                            .filter((item, components) -> item == Items.PAPER || item == Items.BOOK),
                    ItemResourceSlot.builder(TransferType.OUTPUT).pos(196, 26),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(196, 54).capacity(1)
                            .filter((item, c) -> item == GCItems.PRECISION_VERNIER_CALIPER || item == GCItems.OPTICAL_BLUEPRINT_PROJECTOR),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(196, 76).capacity(1)
                            .filter((item, c) -> item == GCItems.CAD_HOLOGRAPHIC_STYLUS)
            )
    );

    public static final List<PrototypeExperiment> EXPERIMENTS = List.of(
            new PrototypeExperiment(
                    1, "Base Machines", "Niobium + Silicon -> Machines",
                    GCItems.RAW_NIOBIUM, GCItems.SILICON, true,
                    ResearchCatalog.EARTH_PROTOTYPING, ResearchDomain.STRUCTURES, 24,
                    GCItems.BASIC_WAFER, 1, 60, 68, 54, 42
            ),
            new PrototypeExperiment(
                    2, "Base Machines", "Aluminum + Tin -> Machines",
                    GCItems.RAW_ALUMINUM, GCItems.RAW_TIN, false,
                    ResearchCatalog.EARTH_PROTOTYPING, ResearchDomain.STRUCTURES, 20,
                    GCItems.COMPRESSED_ALUMINUM, 1, 60, 52, 62, 46
            ),
            new PrototypeExperiment(
                    3, "Ferroniobium", "Niobium + Iron -> FeNb HSLA",
                    GCItems.RAW_NIOBIUM, Items.IRON_INGOT, false,
                    ResearchCatalog.FERRONIOBIUM_ALUMINOTHERMY, ResearchDomain.THERMODYNAMICS, 30,
                    GCItems.FERRONIOBIUM_INGOT, 1, 70, 78, 36, 72
            ),
            new PrototypeExperiment(
                    4, "Pyrochlore Oxide", "Niobium + Coal -> Nb2O5",
                    GCItems.RAW_NIOBIUM, Items.COAL, false,
                    ResearchCatalog.FERRONIOBIUM_ALUMINOTHERMY, ResearchDomain.THERMODYNAMICS, 25,
                    GCItems.NIOBIUM_OXIDE, 2, 60, 44, 48, 80
            ),
            new PrototypeExperiment(
                    5, "Wafer Lithography", "Silicon + Redstone -> Wafers",
                    GCItems.SILICON, Items.REDSTONE, false,
                    ResearchCatalog.EARTH_PROTOTYPING, ResearchDomain.ELECTROMAGNETISM, 25,
                    GCItems.BASIC_WAFER, 1, 60, 38, 84, 52
            ),
            new PrototypeExperiment(
                    6, "Oxygen Support", "Tin + Glass -> O2 Canister",
                    GCItems.RAW_TIN, Items.GLASS, false,
                    ResearchCatalog.BASIC_OXYGEN_GEAR, ResearchDomain.CRYOGENICS, 24,
                    GCItems.TIN_CANISTER, 1, 60, 62, 34, 68
            ),
            new PrototypeExperiment(
                    7, "Era 1 Rocketry", "Aluminum + Coal -> RP-1 Plate",
                    GCItems.RAW_ALUMINUM, Items.COAL, true,
                    ResearchCatalog.ERA1_ROCKETRY, ResearchDomain.THERMODYNAMICS, 34,
                    GCItems.TIER_1_HEAVY_DUTY_PLATE, 1, 80, 74, 46, 78
            )
    );

    private int progress = 0;
    private int maxProgress = DEFAULT_DURATION;
    private int activeExperimentId = 0;
    private int completedCount = 0;
    private int metrologyTier = 0; // 0=none, 1=vernier caliper, 2=blueprint projector
    private int hasStylusFlag = 0;

    public ResearchWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.RESEARCH_WORKBENCH, pos, state, SPEC);
    }

    public static @Nullable PrototypeExperiment matchExperiment(
            @Nullable Item specimen,
            @Nullable Item catalyst,
            boolean hasPaper
    ) {
        if (specimen == null) {
            return null;
        }
        for (PrototypeExperiment exp : EXPERIMENTS) {
            if (exp.requiresPaper() && !hasPaper) {
                continue;
            }
            if ((specimen == exp.specimen() && catalyst == exp.catalyst())
                    || (catalyst == exp.specimen() && specimen == exp.catalyst())) {
                return exp;
            }
        }
        if (hasPaper) {
            if (specimen == GCItems.RAW_NIOBIUM) {
                return EXPERIMENTS.get(0);
            }
            if (specimen == GCItems.RAW_ALUMINUM || specimen == GCItems.RAW_TIN) {
                return EXPERIMENTS.get(1);
            }
            if (specimen == GCItems.SILICON) {
                return EXPERIMENTS.get(4);
            }
        }
        return null;
    }

    public static @Nullable PrototypeExperiment byId(int id) {
        for (PrototypeExperiment exp : EXPERIMENTS) {
            if (exp.id() == id) {
                return exp;
            }
        }
        return null;
    }

    /**
     * Server-validated interactive blueprint seal triggered when the player aligns the 3 coupled
     * parameter tolerances inside the green zone and clicks {@code SEAL BLUEPRINT}.
     */
    public boolean sealCalibratedPrototype(@NotNull ServerLevel level, int accuracyPct) {
        ItemResourceSlot specSlot = this.itemStorage().slot(SPECIMEN_SLOT);
        ItemResourceSlot catSlot = this.itemStorage().slot(CATALYST_SLOT);
        ItemResourceSlot paperSlot = this.itemStorage().slot(PAPER_SLOT);
        ItemResourceSlot outSlot = this.itemStorage().slot(PROTOTYPE_OUTPUT_SLOT);

        Item specItem = specSlot.isEmpty() ? null : specSlot.getResource();
        Item catItem = catSlot.isEmpty() ? null : catSlot.getResource();
        boolean hasPaper = !paperSlot.isEmpty();
        PrototypeExperiment exp = matchExperiment(specItem, catItem, hasPaper);
        if (exp == null || !outSlot.canInsert(exp.prototypeOutput(), DataComponentPatch.EMPTY, exp.outputCount())) {
            return false;
        }

        int clampedAcc = Mth.clamp(accuracyPct, 50, 100);
        float quality = 0.85F + (clampedAcc / 100.0F) * 0.45F;
        if (this.metrologyTier == 2 && clampedAcc >= 85) {
            quality *= 1.20F; // Optical Blueprint Projector precision bonus
        }
        return completePrototype(level, exp, specSlot, catSlot, paperSlot, outSlot, specItem, catItem, hasPaper, quality);
    }

    private boolean completePrototype(
            @NotNull ServerLevel level,
            @NotNull PrototypeExperiment exp,
            ItemResourceSlot specSlot,
            ItemResourceSlot catSlot,
            ItemResourceSlot paperSlot,
            ItemResourceSlot outSlot,
            Item specItem,
            @Nullable Item catItem,
            boolean hasPaper,
            float qualityMultiplier
    ) {
        boolean directPair = (specItem == exp.specimen() && catItem == exp.catalyst())
                || (catItem == exp.specimen() && specItem == exp.catalyst());
        try (Transaction tx = Transaction.openOuter()) {
            long e1 = specSlot.extract(specItem, DataComponentPatch.EMPTY, 1L, tx);
            if (e1 < 1L) {
                return false;
            }
            if (directPair && catItem != null) {
                long e2 = catSlot.extract(catItem, DataComponentPatch.EMPTY, 1L, tx);
                if (e2 < 1L) {
                    return false;
                }
            }
            if (hasPaper && (exp.requiresPaper() || !directPair)) {
                Item paperItem = paperSlot.getResource();
                long ep = paperSlot.extract(paperItem, DataComponentPatch.EMPTY, 1L, tx);
                if (ep < 1L) {
                    return false;
                }
            }
            long inserted = outSlot.insert(exp.prototypeOutput(), DataComponentPatch.EMPTY, exp.outputCount(), tx);
            if (inserted < exp.outputCount()) {
                return false;
            }
            tx.commit();
        }

        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(level);
        ResearchManager.recordScienceExperiment(
                level,
                "workbench_proto_" + exp.id(),
                "Blueprint Prototype: " + exp.shortName(),
                "Research Workbench",
                env.displayName(),
                exp.domain(),
                exp.domainPoints(),
                Math.max(8, (int) Math.round(exp.domainPoints() * 0.7)),
                qualityMultiplier,
                exp.unlockNodeId(),
                null
        );
        this.progress = 0;
        this.completedCount++;
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        Item met = this.itemStorage().slot(METROLOGY_UPGRADE_SLOT).getResource();
        this.metrologyTier = met == GCItems.OPTICAL_BLUEPRINT_PROJECTOR ? 2
                : (met == GCItems.PRECISION_VERNIER_CALIPER ? 1 : 0);
        this.hasStylusFlag = this.itemStorage().slot(STYLUS_UPGRADE_SLOT).getResource() == GCItems.CAD_HOLOGRAPHIC_STYLUS ? 1 : 0;

        ItemResourceSlot specSlot = this.itemStorage().slot(SPECIMEN_SLOT);
        ItemResourceSlot catSlot = this.itemStorage().slot(CATALYST_SLOT);
        ItemResourceSlot paperSlot = this.itemStorage().slot(PAPER_SLOT);
        ItemResourceSlot outSlot = this.itemStorage().slot(PROTOTYPE_OUTPUT_SLOT);

        Item specItem = specSlot.isEmpty() ? null : specSlot.getResource();
        Item catItem = catSlot.isEmpty() ? null : catSlot.getResource();
        boolean hasPaper = !paperSlot.isEmpty();

        PrototypeExperiment exp = matchExperiment(specItem, catItem, hasPaper);
        if (exp == null) {
            this.progress = 0;
            this.activeExperimentId = 0;
            return specItem == null ? GCMachineStatuses.MISSING_SPECIMEN : MachineStatuses.INVALID_RECIPE;
        }

        if (!outSlot.canInsert(exp.prototypeOutput(), DataComponentPatch.EMPTY, exp.outputCount())) {
            return MachineStatuses.OUTPUT_FULL;
        }

        int prevExp = this.activeExperimentId;
        this.activeExperimentId = exp.id();
        this.maxProgress = this.hasStylusFlag != 0 ? Math.max(25, (exp.durationTicks() * 2) / 3) : exp.durationTicks();
        if (this.activeExperimentId != prevExp) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        // Slow baseline drafting fallback (players who calibrate interactively get instant completion + up to 1.5x SP!)
        if (this.progress + 1 >= this.maxProgress) {
            if (!completePrototype(level, exp, specSlot, catSlot, paperSlot, outSlot, specItem, catItem, hasPaper, 0.80F)) {
                return MachineStatuses.OUTPUT_FULL;
            }
            return GCMachineStatuses.RESEARCHING;
        }

        this.progress++;
        return GCMachineStatuses.RESEARCHING;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    public int getActiveExperimentId() {
        return this.activeExperimentId;
    }

    public int getCompletedCount() {
        return this.completedCount;
    }

    public int getMetrologyTier() {
        return this.metrologyTier;
    }

    public int getHasStylusFlag() {
        return this.hasStylusFlag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
        tag.putInt("active_experiment_id", this.activeExperimentId);
        tag.putInt("completed_count", this.completedCount);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = Math.max(0, tag.getInt("progress"));
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : DEFAULT_DURATION;
        this.activeExperimentId = tag.getInt("active_experiment_id");
        this.completedCount = Math.max(0, tag.getInt("completed_count"));
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("active_experiment_id", this.activeExperimentId);
        return tag;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new ResearchWorkbenchMenu(syncId, (ServerPlayer) player, this);
    }

    public record PrototypeExperiment(
            int id,
            @NotNull String shortName,
            @NotNull String formulaText,
            @NotNull Item specimen,
            @NotNull Item catalyst,
            boolean requiresPaper,
            @NotNull ResourceLocation unlockNodeId,
            @NotNull ResearchDomain domain,
            int domainPoints,
            @NotNull Item prototypeOutput,
            int outputCount,
            int durationTicks,
            int targetRigidity,
            int targetConductivity,
            int targetThermal
    ) {
    }
}
