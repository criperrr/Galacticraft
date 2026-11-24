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
import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.machinelib.api.util.FluidSource;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.ElectrolyticRecipe;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.screen.ElectrolyticSeparatorMenu;
import dev.galacticraft.mod.util.FluidUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Cryogenic PEM Electrolytic Separator: splits Water or planetary Dense Ice into
 * stoichiometric Liquid Hydrogen (2x LH2) and Liquid Oxygen (1x LOX), while gradually
 * concentrating heavy-water Deuterium (Gases.DEUTERIUM) from Dense Ice for D-3He fusion.
 */
public class ElectrolyticSeparatorBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int WATER_BUCKET_SLOT = 1;
    public static final int SOLID_ICE_SLOT = 2;
    public static final int LH2_CANISTER_SLOT = 3;
    public static final int LOX_CANISTER_SLOT = 4;
    public static final int DEUTERIUM_CANISTER_SLOT = 5;

    public static final int WATER_INPUT_TANK = 0;
    public static final int HYDROGEN_OUTPUT_TANK = 1;
    public static final int OXYGEN_OUTPUT_TANK = 2;
    public static final int DEUTERIUM_OUTPUT_TANK = 3;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 120L;
    public static final long TANK_CAPACITY = FluidUtil.bucketsToDroplets(16);

    public static final int OPERATING_AMPS = 240;
    public static final int OPERATING_VOLTAGE_MV = 2050; // 2.05V operating cell stack

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 76)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(30, 76)
                            .capacity(1)
                            .filter(ResourceFilters.canExtractFluid(Fluids.WATER))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(67, 24)
                            .filter((item, components) -> item == GCBlocks.DENSE_ICE.asItem()
                                    || item == Items.BLUE_ICE
                                    || item == Items.PACKED_ICE
                                    || item == Items.ICE),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(104, 76)
                            .capacity(1)
                            .filter(ResourceFilters.canInsertFluid(GCFluids.LIQUID_HYDROGEN))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(128, 76)
                            .capacity(1)
                            .filter(ResourceFilters.canInsertFluid(GCFluids.LIQUID_OXYGEN))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(152, 76)
                            .capacity(1)
                            .filter(ResourceFilters.canInsertFluid(Gases.DEUTERIUM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET))
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(30, 22)
                            .capacity(TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Fluids.WATER)),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(104, 22)
                            .capacity(TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(GCFluids.LIQUID_HYDROGEN)),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(128, 22)
                            .capacity(TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(GCFluids.LIQUID_OXYGEN)),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(152, 22)
                            .capacity(TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.DEUTERIUM))
            )
    );

    private final FluidSource fluidSource = new FluidSource(this);
    private final RecipeManager.CachedCheck<ElectrolyticRecipe.Input, ElectrolyticRecipe> quickCheck =
            RecipeManager.createCheck(GCRecipes.ELECTROLYTIC_TYPE);
    private int progress = 0;
    private int maxProgress = 20;
    private int electrodeAmps = 0;
    private int cellVoltageMv = 0;

    public ElectrolyticSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.ELECTROLYTIC_SEPARATOR, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
        this.takeFluidFromSlot(WATER_BUCKET_SLOT, WATER_INPUT_TANK, Fluids.WATER);
        this.drainFluidToSlot(LH2_CANISTER_SLOT, HYDROGEN_OUTPUT_TANK);
        this.drainFluidToSlot(LOX_CANISTER_SLOT, OXYGEN_OUTPUT_TANK);
        this.drainFluidToSlot(DEUTERIUM_CANISTER_SLOT, DEUTERIUM_OUTPUT_TANK);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        profiler.push("transfer");
        this.fluidSource.trySpreadFluids(level, pos, state);
        profiler.pop();

        ItemResourceSlot iceSlot = this.itemStorage().slot(SOLID_ICE_SLOT);
        FluidResourceSlot waterTank = this.fluidStorage().slot(WATER_INPUT_TANK);
        FluidResourceSlot lh2Tank = this.fluidStorage().slot(HYDROGEN_OUTPUT_TANK);
        FluidResourceSlot loxTank = this.fluidStorage().slot(OXYGEN_OUTPUT_TANK);
        FluidResourceSlot d2Tank = this.fluidStorage().slot(DEUTERIUM_OUTPUT_TANK);

        // Prefer solid ice feedstock when present (so Dense Ice always produces Deuterium), else fluid water
        Optional<RecipeHolder<ElectrolyticRecipe>> match = Optional.empty();
        if (!iceSlot.isEmpty()) {
            ElectrolyticRecipe.Input solidInput = new ElectrolyticRecipe.Input(
                    Fluids.EMPTY, 0L, new ItemStack(iceSlot.getResource(), 1)
            );
            match = this.quickCheck.getRecipeFor(solidInput, level);
        }
        if (match.isEmpty() && !waterTank.isEmpty()) {
            ElectrolyticRecipe.Input fluidInput = new ElectrolyticRecipe.Input(
                    waterTank.getResource(), waterTank.getAmount(), ItemStack.EMPTY
            );
            match = this.quickCheck.getRecipeFor(fluidInput, level);
        }

        if (match.isEmpty()) {
            this.progress = 0;
            this.rampDownCell();
            return iceSlot.isEmpty() && waterTank.isEmpty() ? GCMachineStatuses.MISSING_FEEDSTOCK : MachineStatuses.INVALID_RECIPE;
        }

        ElectrolyticRecipe recipe = match.get().value();
        this.maxProgress = Math.max(1, recipe.getProcessingTime());
        long energyCost = Math.max(1L, recipe.getEnergyPerTick());

        Fluid primaryFluid = recipe.getPrimaryOutputFluid();
        long primaryDroplets = recipe.getPrimaryOutputDroplets();
        Fluid secondaryFluid = recipe.getSecondaryOutputFluid();
        long secondaryDroplets = recipe.getSecondaryOutputDroplets();
        Fluid traceGas = recipe.getTraceGasFluid();
        long traceDroplets = recipe.getTraceGasDroplets();

        // Check output tank space for main stoichiometric products (LH2 + LOX)
        if (lh2Tank.tryInsert(primaryFluid, primaryDroplets) < primaryDroplets
                || loxTank.tryInsert(secondaryFluid, secondaryDroplets) < secondaryDroplets) {
            this.rampDownCell();
            return GCMachineStatuses.GAS_TANK_FULL;
        }

        // Final tick: execute energy + feedstock extraction + LH2/LOX/D2 insertion atomically
        if (this.progress + 1 >= this.maxProgress) {
            try (Transaction tx = Transaction.openOuter()) {
                long drawn = this.energyStorage().extract(energyCost, tx);
                if (drawn < energyCost) {
                    this.rampDownCell();
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }

                if (recipe.usesSolidInput()) {
                    Item solidItem = iceSlot.getResource();
                    if (solidItem == null || iceSlot.extract(solidItem, null, 1L, tx) < 1L) {
                        return MachineStatuses.INVALID_RECIPE;
                    }
                } else {
                    Fluid inFluid = recipe.getInputFluid();
                    long inDroplets = recipe.getInputFluidDroplets();
                    if (inFluid == null || waterTank.extract(inFluid, null, inDroplets, tx) < inDroplets) {
                        return GCMachineStatuses.MISSING_FEEDSTOCK;
                    }
                }

                if (lh2Tank.insert(primaryFluid, DataComponentPatch.EMPTY, primaryDroplets, tx) < primaryDroplets) {
                    return GCMachineStatuses.GAS_TANK_FULL;
                }
                if (loxTank.insert(secondaryFluid, DataComponentPatch.EMPTY, secondaryDroplets, tx) < secondaryDroplets) {
                    return GCMachineStatuses.GAS_TANK_FULL;
                }
                // Trace Deuterium gas collected when tank has headroom (non-blocking if D2 tank is full)
                if (traceGas != null && traceDroplets > 0L) {
                    d2Tank.insert(traceGas, DataComponentPatch.EMPTY, traceDroplets, tx);
                }
                tx.commit();
            }

            this.progress = 0;
            this.rampUpCell();
            this.setChanged();
            return GCMachineStatuses.ELECTROLYZING;
        }

        // Intermediate tick: atomic energy draw
        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(energyCost, tx);
            if (drawn < energyCost) {
                this.rampDownCell();
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        this.rampUpCell();
        return GCMachineStatuses.ELECTROLYZING;
    }

    @Override
    protected void tickDisabled(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        this.rampDownCell();
    }

    private void rampUpCell() {
        this.electrodeAmps = Math.min(OPERATING_AMPS, this.electrodeAmps + 30);
        this.cellVoltageMv = Math.min(OPERATING_VOLTAGE_MV, Math.max(1230, this.cellVoltageMv + 120));
    }

    private void rampDownCell() {
        this.electrodeAmps = Math.max(0, this.electrodeAmps - 40);
        this.cellVoltageMv = Math.max(0, this.cellVoltageMv - 250);
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    public int getElectrodeAmps() {
        return this.electrodeAmps;
    }

    public int getCellVoltageMv() {
        return this.cellVoltageMv;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
        tag.putInt("electrode_amps", this.electrodeAmps);
        tag.putInt("cell_voltage_mv", this.cellVoltageMv);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : 20;
        this.electrodeAmps = tag.getInt("electrode_amps");
        this.cellVoltageMv = tag.getInt("cell_voltage_mv");
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new ElectrolyticSeparatorMenu(syncId, (ServerPlayer) player, this);
    }
}
