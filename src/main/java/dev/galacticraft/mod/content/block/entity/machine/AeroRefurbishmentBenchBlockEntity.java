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
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile.SlotRole;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition.SlotMaterialEntry;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
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
import dev.galacticraft.machinelib.api.util.ItemStackUtil;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketMaterialRegistry;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.screen.AeroRefurbishmentBenchMenu;
import dev.galacticraft.mod.tag.GCItemTags;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Aerospace Refurbishment &amp; Coating Bench ({@code AERO_REFURBISHMENT_BENCH}) block entity.
 *
 * <ul>
 *   <li><b>Slot 1 ({@link #PART_INPUT_SLOT})</b>: Modular rocket part ({@link ModularRocketPartBlockItem})
 *       requiring structural repair ({@code PartIntegrity < 100%}) or protective coating.</li>
 *   <li><b>Slot 2 ({@link #MATERIAL_INPUT_SLOT})</b>: Repair material (any hull material from the part's
 *       {@link RocketPartComposition}, {@code COMPRESSED_NIOBIUM}, {@code ABLATIVE_HEAT_TILE}, etc.) OR
 *       protective coating item ({@code LEAD_INGOT}, {@code ABLATIVE_HEAT_TILE}, {@code SOFI_INSULATION},
 *       {@code LONSDALEITE_GEM}, {@code COMPRESSED_NIOBIUM}).</li>
 *   <li><b>Slot 3 ({@link #OUTPUT_SLOT})</b>: Refurbished part restored to {@code 100%} nominal integrity
 *       and/or stamped with the applied protective coating entry in its {@link RocketPartComposition}.</li>
 * </ul>
 */
public class AeroRefurbishmentBenchBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int PART_INPUT_SLOT = 1;
    public static final int MATERIAL_INPUT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;

    public static final int COATING_SLOT_INDEX = 9;

    public static final long ENERGY_CAPACITY = 45_000L;
    public static final long ENERGY_CONSUMPTION = 50L;
    public static final int REPAIR_TICKS = 100;
    public static final int COATING_TICKS = 140;

    public static final int MODE_IDLE = 0;
    public static final int MODE_REPAIR = 1;
    public static final int MODE_COAT = 2;
    public static final int MODE_REPAIR_AND_COAT = 3;

    public static final int COATING_NONE = 0;
    public static final int COATING_LEAD = 1;
    public static final int COATING_TPS = 2;
    public static final int COATING_SOFI = 3;
    public static final int COATING_LONSDALEITE = 4;
    public static final int COATING_NIOBIUM = 5;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 74)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(44, 42)
                            .capacity(1)
                            .filter((item, components) -> item instanceof ModularRocketPartBlockItem),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(78, 42)
                            .filter((item, components) -> isValidBenchMaterial(item)),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(138, 42)
                            .capacity(1)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            )
    );

    private int progress = 0;
    private int maxProgress = REPAIR_TICKS;
    private int operationMode = MODE_IDLE;
    private int inputIntegrityPct = 100;
    private int activeCoatingId = COATING_NONE;

    public AeroRefurbishmentBenchBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.AERO_REFURBISHMENT_BENCH, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        ItemResourceSlot partSlot = this.itemStorage().slot(PART_INPUT_SLOT);
        ItemResourceSlot matSlot = this.itemStorage().slot(MATERIAL_INPUT_SLOT);
        ItemResourceSlot outputSlot = this.itemStorage().slot(OUTPUT_SLOT);

        if (partSlot.isEmpty()) {
            this.resetIdle(100);
            return MachineStatuses.INVALID_RECIPE;
        }

        ItemStack partStack = ItemStackUtil.create(partSlot);
        if (partStack.isEmpty() || !(partStack.getItem() instanceof ModularRocketPartBlockItem)) {
            this.resetIdle(100);
            return MachineStatuses.INVALID_RECIPE;
        }

        PartIntegrity currentIntegrity = partStack.get(GCDataComponents.PART_INTEGRITY);
        double maxIntegrity = currentIntegrity != null && currentIntegrity.maxIntegrity() > 0.0
                ? currentIntegrity.maxIntegrity()
                : PartIntegrity.DEFAULT_MAX_INTEGRITY;
        double curIntegrity = currentIntegrity != null ? currentIntegrity.currentIntegrity() : maxIntegrity;
        this.inputIntegrityPct = (int) Math.round(Math.clamp(curIntegrity / Math.max(1.0, maxIntegrity), 0.0, 1.0) * 100.0);

        if (matSlot.isEmpty()) {
            this.progress = 0;
            this.operationMode = MODE_IDLE;
            this.activeCoatingId = COATING_NONE;
            return MachineStatuses.INVALID_RECIPE;
        }

        ItemStack materialStack = ItemStackUtil.create(matSlot);
        RefurbishPlan plan = evaluatePlan(partStack, materialStack);
        if (plan == null) {
            this.progress = 0;
            this.operationMode = MODE_IDLE;
            this.activeCoatingId = COATING_NONE;
            boolean alreadyNominal = curIntegrity >= maxIntegrity - 0.01;
            return alreadyNominal ? GCMachineStatuses.PART_ALREADY_NOMINAL : MachineStatuses.INVALID_RECIPE;
        }

        this.operationMode = plan.mode();
        this.activeCoatingId = plan.coatingId();
        this.maxProgress = plan.durationTicks();

        ItemStack result = plan.outputStack();
        if (!outputSlot.canInsert(result.getItem(), result.getComponentsPatch(), 1L)) {
            return MachineStatuses.OUTPUT_FULL;
        }

        if (this.progress + 1 >= this.maxProgress) {
            try (Transaction tx = Transaction.openOuter()) {
                long usedEnergy = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
                if (usedEnergy < ENERGY_CONSUMPTION) {
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                long extractedPart = partSlot.extract(partStack.getItem(), partStack.getComponentsPatch(), 1L, tx);
                if (extractedPart < 1L) {
                    return MachineStatuses.INVALID_RECIPE;
                }
                long extractedMat = matSlot.extract(materialStack.getItem(), materialStack.getComponentsPatch(), 1L, tx);
                if (extractedMat < 1L) {
                    return MachineStatuses.INVALID_RECIPE;
                }
                long insertedOut = outputSlot.insert(result.getItem(), result.getComponentsPatch(), 1L, tx);
                if (insertedOut < 1L) {
                    return MachineStatuses.OUTPUT_FULL;
                }
                tx.commit();
            }
            this.progress = 0;
            this.setChanged();
            return plan.mode() == MODE_COAT ? GCMachineStatuses.APPLYING_COATING : GCMachineStatuses.REFURBISHING;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long usedEnergy = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (usedEnergy < ENERGY_CONSUMPTION) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        return plan.mode() == MODE_COAT ? GCMachineStatuses.APPLYING_COATING : GCMachineStatuses.REFURBISHING;
    }

    private void resetIdle(int integrityPct) {
        this.progress = 0;
        this.operationMode = MODE_IDLE;
        this.activeCoatingId = COATING_NONE;
        this.inputIntegrityPct = integrityPct;
    }

    /**
     * Evaluates whether {@code partStack} and {@code materialStack} form a valid repair and/or
     * protective coating operation, returning the precomputed output {@link ItemStack} or {@code null}.
     */
    public static @Nullable RefurbishPlan evaluatePlan(@NotNull ItemStack partStack, @NotNull ItemStack materialStack) {
        if (partStack.isEmpty() || materialStack.isEmpty() || !(partStack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
            return null;
        }

        RocketPartConfig config = partItem.getConfig(partStack);
        RocketPartComposition existingComp = partStack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
        PartIntegrity existingIntegrity = partStack.get(GCDataComponents.PART_INTEGRITY);
        RocketPartCostCalculator.CostProfile existingProfile = RocketPartCostCalculator.calculate(config, existingComp);
        double maxIntegrity = existingIntegrity != null && existingIntegrity.maxIntegrity() > 0.0
                ? existingIntegrity.maxIntegrity()
                : existingProfile.maxIntegrity();
        double curIntegrity = existingIntegrity != null ? existingIntegrity.currentIntegrity() : maxIntegrity;
        boolean needsRepair = curIntegrity < (maxIntegrity - 0.01);

        Optional<RocketMaterialProfile> coatingOpt = RocketMaterialRegistry.resolve(materialStack, SlotRole.PROTECTIVE_COATING);
        boolean canApplyNewCoating = false;
        RocketMaterialProfile coatingProfile = coatingOpt.orElse(null);
        if (coatingProfile != null && !coatingProfile.traits().isEmpty()) {
            RocketMaterialTrait primaryTrait = coatingProfile.traits().getFirst();
            boolean alreadyHasTrait = existingComp != null && existingComp.hasTrait(primaryTrait);
            canApplyNewCoating = !alreadyHasTrait;
        }

        boolean validRepairMaterial = isRepairMaterialForPart(materialStack, existingComp);
        if (!canApplyNewCoating && !(needsRepair && validRepairMaterial)) {
            return null;
        }

        // Construct updated composition if a new coating is being stamped
        RocketPartComposition nextComp = existingComp;
        int coatingId = COATING_NONE;
        int mode;
        int duration;
        if (canApplyNewCoating && coatingProfile != null) {
            ResourceLocation matId = BuiltInRegistries.ITEM.getKey(materialStack.getItem());
            List<SlotMaterialEntry> entries = new ArrayList<>(existingComp != null ? existingComp.entries() : List.of());
            int nextSlotIdx = COATING_SLOT_INDEX;
            for (SlotMaterialEntry entry : entries) {
                if (entry.slotIndex() >= nextSlotIdx) {
                    nextSlotIdx = entry.slotIndex() + 1;
                }
            }
            entries.add(SlotMaterialEntry.fromProfile(nextSlotIdx, SlotRole.PROTECTIVE_COATING, matId, coatingProfile));
            nextComp = new RocketPartComposition(entries);
            coatingId = resolveCoatingId(materialStack);
            mode = needsRepair ? MODE_REPAIR_AND_COAT : MODE_COAT;
            duration = COATING_TICKS;
        } else {
            mode = MODE_REPAIR;
            duration = REPAIR_TICKS;
        }

        // Recalculate stats and restore integrity to 100% nominal
        RocketPartCostCalculator.CostProfile updatedProfile = RocketPartCostCalculator.calculate(config, nextComp);
        double restoredMax = Math.max(maxIntegrity, updatedProfile.maxIntegrity());
        ItemStack output = partStack.copyWithCount(1);
        output.set(GCDataComponents.ROCKET_PART_CONFIG, config);
        output.set(GCDataComponents.ROCKET_PART_STATS, updatedProfile.stats());
        output.set(GCDataComponents.PART_INTEGRITY, PartIntegrity.pristine(restoredMax));
        if (nextComp != null && !nextComp.isEmpty()) {
            output.set(GCDataComponents.ROCKET_PART_COMPOSITION, nextComp);
        }

        return new RefurbishPlan(mode, coatingId, duration, output);
    }

    /**
     * Checks whether {@code materialStack} can structurally repair a modular part with {@code composition}.
     */
    public static boolean isRepairMaterialForPart(
            @NotNull ItemStack materialStack,
            @Nullable RocketPartComposition composition
    ) {
        if (materialStack.isEmpty()) {
            return false;
        }
        Item item = materialStack.getItem();
        // Universal aerospace refurbishment materials
        if (item == GCItems.COMPRESSED_NIOBIUM
                || item == GCItems.ABLATIVE_HEAT_TILE
                || item == GCItems.COMPRESSED_TITANIUM
                || item == GCItems.COMPRESSED_ALUMINUM
                || item == GCItems.COMPRESSED_STEEL
                || item == GCItems.COMPRESSED_DESH
                || item == GCItems.COMPRESSED_FERRONIOBIUM
                || item == GCItems.CARBON_FIBER_PLATE) {
            return true;
        }
        if (composition != null && !composition.isEmpty()) {
            ResourceLocation matId = BuiltInRegistries.ITEM.getKey(item);
            if (composition.distinctItemIds().contains(matId)) {
                return true;
            }
            Optional<RocketMaterialProfile> matProfile = RocketMaterialRegistry.resolve(materialStack, SlotRole.STRUCTURAL_HULL);
            if (matProfile.isPresent()) {
                for (SlotMaterialEntry entry : composition.entries()) {
                    Optional<RocketMaterialProfile> entryProfile = RocketMaterialRegistry.resolve(entry);
                    if (entryProfile.isPresent() && entryProfile.get().id().equals(matProfile.get().id())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Slot filter checking whether an item is accepted as either a repair material or coating item.
     */
    public static boolean isValidBenchMaterial(@Nullable Item item) {
        if (item == null) {
            return false;
        }
        ItemStack probe = new ItemStack(item);
        if (RocketMaterialRegistry.resolve(probe, SlotRole.PROTECTIVE_COATING).isPresent()) {
            return true;
        }
        if (RocketMaterialRegistry.resolve(probe, SlotRole.STRUCTURAL_HULL).isPresent()) {
            return true;
        }
        return item == GCItems.ABLATIVE_HEAT_TILE
                || item == GCItems.SOFI_INSULATION
                || item == GCItems.LEAD_INGOT
                || item == GCItems.LONSDALEITE_GEM
                || item == GCItems.COMPRESSED_NIOBIUM
                || probe.is(GCItemTags.LEAD_INGOTS);
    }

    public static int resolveCoatingId(@NotNull ItemStack materialStack) {
        Item item = materialStack.getItem();
        if (item == GCItems.LEAD_INGOT || materialStack.is(GCItemTags.LEAD_INGOTS)) {
            return COATING_LEAD;
        }
        if (item == GCItems.ABLATIVE_HEAT_TILE) {
            return COATING_TPS;
        }
        if (item == GCItems.SOFI_INSULATION) {
            return COATING_SOFI;
        }
        if (item == GCItems.LONSDALEITE_GEM) {
            return COATING_LONSDALEITE;
        }
        if (item == GCItems.COMPRESSED_NIOBIUM) {
            return COATING_NIOBIUM;
        }
        return COATING_NONE;
    }

    public static @NotNull String coatingShortName(int coatingId) {
        return switch (coatingId) {
            case COATING_LEAD -> "Lead Anti-Acid";
            case COATING_TPS -> "Olivine TPS";
            case COATING_SOFI -> "SOFI Cryo";
            case COATING_LONSDALEITE -> "Lonsdaleite";
            case COATING_NIOBIUM -> "C-103 Nb";
            default -> "None";
        };
    }

    public record RefurbishPlan(int mode, int coatingId, int durationTicks, @NotNull ItemStack outputStack) {
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    public int getOperationMode() {
        return this.operationMode;
    }

    public int getInputIntegrityPct() {
        return this.inputIntegrityPct;
    }

    public int getActiveCoatingId() {
        return this.activeCoatingId;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
        tag.putInt("operation_mode", this.operationMode);
        tag.putInt("input_integrity_pct", this.inputIntegrityPct);
        tag.putInt("active_coating_id", this.activeCoatingId);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = Math.max(0, tag.getInt("progress"));
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : REPAIR_TICKS;
        this.operationMode = tag.getInt("operation_mode");
        this.inputIntegrityPct = tag.contains("input_integrity_pct") ? tag.getInt("input_integrity_pct") : 100;
        this.activeCoatingId = tag.getInt("active_coating_id");
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new AeroRefurbishmentBenchMenu(syncId, (ServerPlayer) player, this);
    }
}
