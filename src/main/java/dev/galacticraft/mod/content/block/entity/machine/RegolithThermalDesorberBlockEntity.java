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
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.recipe.ThermalDesorptionRecipe;
import dev.galacticraft.mod.screen.RegolithThermalDesorberMenu;
import dev.galacticraft.mod.util.FluidUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RegolithThermalDesorberBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int INPUT_SLOT_1 = 1;
    public static final int INPUT_SLOT_2 = 2;
    public static final int BYPRODUCT_SLOT_1 = 3;
    public static final int BYPRODUCT_SLOT_2 = 4;
    public static final int CANISTER_OUTPUT_SLOT = 5;

    public static final int GAS_TANK = 0;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 95L;
    public static final long GAS_TANK_CAPACITY = FluidUtil.bucketsToDroplets(16);

    public static final int AMBIENT_TEMPERATURE = 20;
    public static final int TARGET_TEMPERATURE = 700;
    public static final int MAX_TEMPERATURE = 850;
    public static final int HEAT_RATE = 18;
    public static final int COOL_RATE = 10;

    public static final int TARGET_VACUUM = 90;
    public static final int MAX_VACUUM = 100;
    public static final int VACUUM_PUMP_RATE = 5;
    public static final int VACUUM_DECAY_RATE = 2;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 74)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(36, 26),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(36, 48),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(108, 26),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(108, 48),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(148, 74)
                            .capacity(1)
                            .filter(ResourceFilters.canInsertFluid(Gases.SOLAR_VOLATILE_GAS))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET))
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(148, 20)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.SOLAR_VOLATILE_GAS))
            )
    );

    private static final int[] INPUT_SLOTS = {INPUT_SLOT_1, INPUT_SLOT_2};

    private final FluidSource fluidSource = new FluidSource(this);
    private final RecipeManager.CachedCheck<SingleRecipeInput, ThermalDesorptionRecipe> quickCheck =
            RecipeManager.createCheck(GCRecipes.THERMAL_DESORPTION_TYPE);
    private int temperature = AMBIENT_TEMPERATURE;
    private int vacuumPercent = 0;
    private int progress = 0;
    private int maxProgress = 100;

    public RegolithThermalDesorberBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.REGOLITH_THERMAL_DESORBER, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
        this.drainFluidToSlot(CANISTER_OUTPUT_SLOT, GAS_TANK);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        profiler.push("transfer");
        this.fluidSource.trySpreadFluids(level, pos, state);
        profiler.pop();

        int activeInputSlot = -1;
        ThermalDesorptionRecipe activeRecipe = null;

        for (int slotIdx : INPUT_SLOTS) {
            ItemResourceSlot slot = this.itemStorage().slot(slotIdx);
            if (!slot.isEmpty()) {
                ItemStack candidate = new ItemStack(slot.getResource(), 1);
                Optional<RecipeHolder<ThermalDesorptionRecipe>> found = this.quickCheck
                        .getRecipeFor(new SingleRecipeInput(candidate), level);
                if (found.isPresent()) {
                    activeInputSlot = slotIdx;
                    activeRecipe = found.get().value();
                    break;
                }
            }
        }

        if (activeRecipe == null) {
            this.progress = 0;
            this.coolDown();
            return MachineStatuses.INVALID_RECIPE;
        }

        this.maxProgress = Math.max(1, activeRecipe.getProcessingTime());

        FluidResourceSlot gasTank = this.fluidStorage().slot(GAS_TANK);
        long gasDroplets = activeRecipe.getGasAmountDroplets();
        if (gasTank.tryInsert(Gases.SOLAR_VOLATILE_GAS, gasDroplets) < gasDroplets) {
            this.coolDown();
            return GCMachineStatuses.GAS_TANK_FULL;
        }

        if (!this.canFitByproducts(activeRecipe)) {
            this.coolDown();
            return MachineStatuses.OUTPUT_FULL;
        }

        if (!this.energyStorage().canExtract(ENERGY_CONSUMPTION)) {
            this.coolDown();
            return MachineStatuses.NOT_ENOUGH_ENERGY;
        }

        this.energyStorage().extract(ENERGY_CONSUMPTION);

        // Stage 1: Evacuate vacuum chamber before high-temperature volatile desorption
        if (this.vacuumPercent < TARGET_VACUUM) {
            this.vacuumPercent = Math.min(MAX_VACUUM, this.vacuumPercent + VACUUM_PUMP_RATE);
            this.temperature = Math.min(TARGET_TEMPERATURE / 2, this.temperature + HEAT_RATE / 2);
            return GCMachineStatuses.EVACUATING_CHAMBER;
        }
        this.vacuumPercent = Math.min(MAX_VACUUM, this.vacuumPercent + 2);

        // Stage 2: Heat chamber above 700C in vacuum
        if (this.temperature < TARGET_TEMPERATURE) {
            this.temperature = Math.min(MAX_TEMPERATURE, this.temperature + HEAT_RATE);
            return GCMachineStatuses.WARMING_UP;
        }

        // Stage 3: Desorb solar volatile gas + separate mineral byproducts
        this.temperature = Math.min(MAX_TEMPERATURE, this.temperature + 5);
        this.progress++;

        if (this.progress >= this.maxProgress) {
            ItemResourceSlot inputSlot = this.itemStorage().slot(activeInputSlot);
            if (!inputSlot.isEmpty()
                    && gasTank.tryInsert(Gases.SOLAR_VOLATILE_GAS, gasDroplets) >= gasDroplets
                    && this.canFitByproducts(activeRecipe)) {
                inputSlot.extractOne();
                gasTank.insert(Gases.SOLAR_VOLATILE_GAS, gasDroplets);
                this.insertByproduct(activeRecipe.getPrimaryByproduct(), BYPRODUCT_SLOT_1);
                this.insertByproduct(activeRecipe.getSecondaryByproduct(), BYPRODUCT_SLOT_2);
                this.progress = 0;
                this.setChanged();
            }
        }

        return GCMachineStatuses.DESORBING;
    }

    @Override
    protected void tickDisabled(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        this.coolDown();
    }

    private void coolDown() {
        if (this.temperature > AMBIENT_TEMPERATURE) {
            this.temperature = Math.max(AMBIENT_TEMPERATURE, this.temperature - COOL_RATE);
        }
        if (this.vacuumPercent > 0) {
            this.vacuumPercent = Math.max(0, this.vacuumPercent - VACUUM_DECAY_RATE);
        }
    }

    private boolean canFitByproducts(ThermalDesorptionRecipe recipe) {
        return canFitInSlot(recipe.getPrimaryByproduct(), BYPRODUCT_SLOT_1)
                && canFitInSlot(recipe.getSecondaryByproduct(), BYPRODUCT_SLOT_2);
    }

    private boolean canFitInSlot(ItemStack stack, int slotIndex) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemResourceSlot slot = this.itemStorage().slot(slotIndex);
        return slot.canInsert(stack.getItem(), stack.getCount());
    }

    private void insertByproduct(ItemStack stack, int slotIndex) {
        if (!stack.isEmpty()) {
            this.itemStorage().slot(slotIndex).insert(stack.getItem(), stack.getCount());
        }
    }

    public int getTemperature() {
        return this.temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getVacuumPercent() {
        return this.vacuumPercent;
    }

    public void setVacuumPercent(int vacuumPercent) {
        this.vacuumPercent = vacuumPercent;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("temperature", this.temperature);
        tag.putInt("vacuum_percent", this.vacuumPercent);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.temperature = tag.contains("temperature") ? tag.getInt("temperature") : AMBIENT_TEMPERATURE;
        this.vacuumPercent = tag.getInt("vacuum_percent");
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : 100;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new RegolithThermalDesorberMenu(syncId, (ServerPlayer) player, this);
    }
}
