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
import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.ChemicalLeachingRecipe;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.screen.ChemicalLeachingPlantMenu;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ChemicalLeachingPlantBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int FLUID_INPUT_SLOT = 1;
    public static final int ORE_INPUT_SLOT = 2;
    public static final int PRIMARY_OUTPUT_SLOT = 3;
    public static final int BYPRODUCT_OUTPUT_SLOT = 4;

    public static final int ACID_TANK = 0;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 90L;
    public static final long ACID_TANK_CAPACITY = FluidUtil.bucketsToDroplets(16);

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 74)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(32, 74)
                            .capacity(1)
                            .filter((item, components) -> ResourceFilters.canExtractFluid(GCFluids.SULFURIC_ACID).test(item, components)
                                    || ResourceFilters.canExtractFluid(Fluids.WATER).test(item, components))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(66, 42),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(128, 30),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(128, 54)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(32, 20)
                            .capacity(ACID_TANK_CAPACITY)
                            .filter((fluid, components) -> fluid == GCFluids.SULFURIC_ACID || fluid == Fluids.WATER)
            )
    );

    private final RecipeManager.CachedCheck<ChemicalLeachingRecipe.Input, ChemicalLeachingRecipe> quickCheck =
            RecipeManager.createCheck(GCRecipes.CHEMICAL_LEACHING_TYPE);
    private int progress = 0;
    private int maxProgress = 120;

    public ChemicalLeachingPlantBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.CHEMICAL_LEACHING_PLANT, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
        this.takeFluidFromSlot(FLUID_INPUT_SLOT, ACID_TANK);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        ItemResourceSlot oreSlot = this.itemStorage().slot(ORE_INPUT_SLOT);
        if (oreSlot.isEmpty()) {
            this.progress = 0;
            return MachineStatuses.INVALID_RECIPE;
        }

        FluidResourceSlot acidTank = this.fluidStorage().slot(ACID_TANK);
        if (acidTank.isEmpty()) {
            this.progress = 0;
            return GCMachineStatuses.MISSING_LEACHING_FLUID;
        }

        Item oreItem = oreSlot.getResource();
        Fluid tankFluid = acidTank.getResource();
        long tankDroplets = acidTank.getAmount();

        ChemicalLeachingRecipe.Input recipeInput = new ChemicalLeachingRecipe.Input(
                new ItemStack(oreItem, 1),
                tankFluid,
                tankDroplets
        );
        Optional<RecipeHolder<ChemicalLeachingRecipe>> match = this.quickCheck.getRecipeFor(recipeInput, level);

        if (match.isEmpty()) {
            this.progress = 0;
            return MachineStatuses.INVALID_RECIPE;
        }

        ChemicalLeachingRecipe recipe = match.get().value();
        this.maxProgress = Math.max(1, recipe.getProcessingTime());

        ItemStack primary = recipe.getPrimaryOutput();
        ItemStack byproduct = recipe.getByproductOutput();
        ItemResourceSlot primarySlot = this.itemStorage().slot(PRIMARY_OUTPUT_SLOT);
        ItemResourceSlot byproductSlot = this.itemStorage().slot(BYPRODUCT_OUTPUT_SLOT);

        if (!primarySlot.canInsert(primary.getItem(), primary.getCount())) {
            return MachineStatuses.OUTPUT_FULL;
        }
        if (!byproduct.isEmpty() && !byproductSlot.canInsert(byproduct.getItem(), byproduct.getCount())) {
            return MachineStatuses.OUTPUT_FULL;
        }

        long requiredFluid = recipe.getFluidAmountDroplets();

        // Final tick: execute energy, ore, acid extraction and output insertion inside a single atomic Fabric Transaction
        if (this.progress + 1 >= this.maxProgress) {
            boolean emitByproduct = !byproduct.isEmpty()
                    && (recipe.getByproductChance() >= 1.0F || level.random.nextFloat() < recipe.getByproductChance());

            try (Transaction tx = Transaction.openOuter()) {
                long usedEnergy = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
                if (usedEnergy < ENERGY_CONSUMPTION) {
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                long extractedOre = oreSlot.extract(oreItem, null, 1L, tx);
                if (extractedOre < 1L) {
                    return MachineStatuses.INVALID_RECIPE;
                }
                long extractedFluid = acidTank.extract(tankFluid, null, requiredFluid, tx);
                if (extractedFluid < requiredFluid) {
                    return GCMachineStatuses.MISSING_LEACHING_FLUID;
                }
                long insertedPrimary = primarySlot.insert(primary.getItem(), DataComponentPatch.EMPTY, primary.getCount(), tx);
                if (insertedPrimary < primary.getCount()) {
                    return MachineStatuses.OUTPUT_FULL;
                }
                if (emitByproduct) {
                    long insertedByproduct = byproductSlot.insert(byproduct.getItem(), DataComponentPatch.EMPTY, byproduct.getCount(), tx);
                    if (insertedByproduct < byproduct.getCount()) {
                        return MachineStatuses.OUTPUT_FULL;
                    }
                }
                tx.commit();
            }

            this.progress = 0;
            this.setChanged();
            return GCMachineStatuses.LEACHING;
        }

        // Intermediate tick: transactional energy draw (automatically rolls back if energy < ENERGY_CONSUMPTION)
        try (Transaction tx = Transaction.openOuter()) {
            long usedEnergy = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (usedEnergy < ENERGY_CONSUMPTION) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        return GCMachineStatuses.LEACHING;
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
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : 120;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new ChemicalLeachingPlantMenu(syncId, (ServerPlayer) player, this);
    }
}
