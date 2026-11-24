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
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.CompositeAutoclaveRecipe;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.screen.CompositeAutoclaveMenu;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompositeAutoclaveBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int INPUT_SLOT_0 = 1;
    public static final int INPUT_SLOT_1 = 2;
    public static final int INPUT_SLOT_2 = 3;
    public static final int INPUT_SLOT_3 = 4;
    public static final int OUTPUT_SLOT = 5;

    public static final int GRID_START = INPUT_SLOT_0;
    public static final int GRID_SIZE = 4;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 80L;

    public static final int AMBIENT_TEMPERATURE = 20;
    public static final int MAX_TEMPERATURE = 600;
    public static final int HEAT_RATE = 25;
    public static final int COOL_RATE = 12;

    public static final int AMBIENT_PRESSURE_BAR = 1;
    public static final int MAX_PRESSURE_BAR = 20;
    public static final int PRESSURIZE_RATE = 1;
    public static final int DEPRESSURIZE_RATE = 1;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 74)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(38, 30),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(56, 30),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(38, 48),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(56, 48),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(136, 39)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            )
    );

    private int temperature = AMBIENT_TEMPERATURE;
    private int pressureBar = AMBIENT_PRESSURE_BAR;
    private int targetTemperature = 450;
    private int targetPressureBar = 12;
    private int progress = 0;
    private int maxProgress = 120;

    public CompositeAutoclaveBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.COMPOSITE_AUTOCLAVE, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        List<ItemStack> gridStacks = new ArrayList<>(GRID_SIZE);
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemResourceSlot slot = this.itemStorage().slot(GRID_START + i);
            gridStacks.add(slot.isEmpty() ? ItemStack.EMPTY : new ItemStack(slot.getResource(), 1));
        }
        CompositeAutoclaveRecipe.Input recipeInput = new CompositeAutoclaveRecipe.Input(gridStacks);
        Optional<RecipeHolder<CompositeAutoclaveRecipe>> match = level.getRecipeManager()
                .getRecipeFor(GCRecipes.COMPOSITE_AUTOCLAVE_TYPE, recipeInput, level);

        if (match.isEmpty()) {
            this.progress = 0;
            this.coolAndDepressurize();
            return MachineStatuses.INVALID_RECIPE;
        }

        CompositeAutoclaveRecipe recipe = match.get().value();
        this.maxProgress = Math.max(1, recipe.getProcessingTime());
        this.targetTemperature = recipe.getTargetTemperature();
        this.targetPressureBar = recipe.getTargetPressureBar();

        ItemStack result = recipe.getResult();
        ItemResourceSlot outputSlot = this.itemStorage().slot(OUTPUT_SLOT);
        if (!outputSlot.canInsert(result.getItem(), result.getCount())) {
            this.coolAndDepressurize();
            return MachineStatuses.OUTPUT_FULL;
        }

        // Stage 1: Heat and pressurize the autoclave chamber to target curing envelope
        if (this.temperature < this.targetTemperature || this.pressureBar < this.targetPressureBar) {
            try (Transaction tx = Transaction.openOuter()) {
                long usedEnergy = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
                if (usedEnergy < ENERGY_CONSUMPTION) {
                    this.coolAndDepressurize();
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                tx.commit();
            }
            this.temperature = Math.min(MAX_TEMPERATURE, this.temperature + HEAT_RATE);
            this.pressureBar = Math.min(MAX_PRESSURE_BAR, this.pressureBar + PRESSURIZE_RATE);
            return GCMachineStatuses.PRESSURIZING;
        }

        // Stage 2: Autoclave curing under heat and pressure
        if (this.progress + 1 >= this.maxProgress) {
            int[] matchedSlots = recipe.matchSlots(recipeInput);
            if (matchedSlots == null) {
                this.progress = 0;
                return MachineStatuses.INVALID_RECIPE;
            }
            try (Transaction tx = Transaction.openOuter()) {
                long usedEnergy = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
                if (usedEnergy < ENERGY_CONSUMPTION) {
                    this.coolAndDepressurize();
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                for (int gridIdx : matchedSlots) {
                    ItemResourceSlot slot = this.itemStorage().slot(GRID_START + gridIdx);
                    Item item = slot.getResource();
                    if (item == null || slot.extract(item, null, 1L, tx) < 1L) {
                        return MachineStatuses.INVALID_RECIPE;
                    }
                }
                long inserted = outputSlot.insert(result.getItem(), DataComponentPatch.EMPTY, result.getCount(), tx);
                if (inserted < result.getCount()) {
                    return MachineStatuses.OUTPUT_FULL;
                }
                tx.commit();
            }
            this.progress = 0;
            this.setChanged();
            return GCMachineStatuses.CURING;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long usedEnergy = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (usedEnergy < ENERGY_CONSUMPTION) {
                this.coolAndDepressurize();
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        return GCMachineStatuses.CURING;
    }

    @Override
    protected void tickDisabled(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        this.coolAndDepressurize();
    }

    private void coolAndDepressurize() {
        if (this.temperature > AMBIENT_TEMPERATURE) {
            this.temperature = Math.max(AMBIENT_TEMPERATURE, this.temperature - COOL_RATE);
        }
        if (this.pressureBar > AMBIENT_PRESSURE_BAR) {
            this.pressureBar = Math.max(AMBIENT_PRESSURE_BAR, this.pressureBar - DEPRESSURIZE_RATE);
        }
    }

    public int getTemperature() {
        return this.temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = Math.max(AMBIENT_TEMPERATURE, Math.min(MAX_TEMPERATURE, temperature));
    }

    public int getPressureBar() {
        return this.pressureBar;
    }

    public void setPressureBar(int pressureBar) {
        this.pressureBar = Math.max(AMBIENT_PRESSURE_BAR, Math.min(MAX_PRESSURE_BAR, pressureBar));
    }

    public int getTargetTemperature() {
        return this.targetTemperature;
    }

    public int getTargetPressureBar() {
        return this.targetPressureBar;
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
        tag.putInt("pressure_bar", this.pressureBar);
        tag.putInt("target_temperature", this.targetTemperature);
        tag.putInt("target_pressure_bar", this.targetPressureBar);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.temperature = tag.contains("temperature") ? tag.getInt("temperature") : AMBIENT_TEMPERATURE;
        this.pressureBar = tag.contains("pressure_bar") ? tag.getInt("pressure_bar") : AMBIENT_PRESSURE_BAR;
        this.targetTemperature = tag.contains("target_temperature") ? tag.getInt("target_temperature") : 450;
        this.targetPressureBar = tag.contains("target_pressure_bar") ? tag.getInt("target_pressure_bar") : 12;
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : 120;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new CompositeAutoclaveMenu(syncId, (ServerPlayer) player, this);
    }
}
