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
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.CentrifugeRecipe;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.screen.IsotopicCentrifugeMenu;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class IsotopicCentrifugeBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int COIL_SLOT = 1;
    public static final int SOLID_INPUT_SLOT = 2;
    public static final int INPUT_CANISTER_SLOT = 3;
    public static final int OUTPUT_CANISTER_SLOT = 4;
    public static final int BYPRODUCT_SLOT = 5;
    public static final int TAILINGS_CANISTER_SLOT = 6;

    public static final int INPUT_GAS_TANK = 0;
    public static final int OUTPUT_GAS_TANK = 1;
    public static final int TAILINGS_GAS_TANK = 2;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 110L;
    public static final long GAS_TANK_CAPACITY = FluidUtil.bucketsToDroplets(16);

    public static final long GAS_INPUT_PER_CYCLE_MB = 50L;
    public static final long GAS_HE3_OUTPUT_MB = 20L;
    public static final long GAS_HE4_TAILINGS_MB = 30L;
    public static final int GAS_CYCLE_TICKS = 10;
    public static final int SOLID_CYCLE_TICKS = 60;

    public static final int TARGET_RPM = 40_000;
    public static final int MAX_RPM = 60_000;
    public static final int RPM_ACCEL = 2_500;
    public static final int RPM_DECAY = 1_500;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 74)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(68, 22)
                            .capacity(1)
                            .filter(ResourceFilters.ofResource(GCItems.SUPERCONDUCTING_COIL)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(68, 52),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(32, 74)
                            .capacity(1)
                            .filter(ResourceFilters.canExtractFluid(Gases.SOLAR_VOLATILE_GAS))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(126, 74)
                            .capacity(1)
                            .filter((item, components) -> ResourceFilters.canInsertFluid(Gases.HELIUM_3).test(item, components)
                                    || ResourceFilters.canInsertFluid(Gases.DEUTERIUM).test(item, components))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(98, 52),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(150, 74)
                            .capacity(1)
                            .filter(ResourceFilters.canInsertFluid(Gases.HELIUM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET))
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(32, 20)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.SOLAR_VOLATILE_GAS)),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(126, 20)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter((fluid, components) -> fluid == Gases.HELIUM_3 || fluid == Gases.DEUTERIUM),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(150, 20)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.HELIUM))
            )
    );

    private final FluidSource fluidSource = new FluidSource(this);
    private int rotorRpm = 0;
    private int progress = 0;
    private int maxProgress = GAS_CYCLE_TICKS;

    public IsotopicCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.ISOTOPIC_CENTRIFUGE, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
        this.takeFluidFromSlot(INPUT_CANISTER_SLOT, INPUT_GAS_TANK, Gases.SOLAR_VOLATILE_GAS);
        this.drainFluidToSlot(OUTPUT_CANISTER_SLOT, OUTPUT_GAS_TANK);
        this.drainFluidToSlot(TAILINGS_CANISTER_SLOT, TAILINGS_GAS_TANK);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        profiler.push("transfer");
        this.fluidSource.trySpreadFluids(level, pos, state);
        profiler.pop();

        ItemResourceSlot coilSlot = this.itemStorage().slot(COIL_SLOT);
        if (coilSlot.isEmpty() || coilSlot.getResource() != GCItems.SUPERCONDUCTING_COIL) {
            this.progress = 0;
            this.spinDown();
            return GCMachineStatuses.MISSING_COIL;
        }

        FluidResourceSlot inputTank = this.fluidStorage().slot(INPUT_GAS_TANK);
        FluidResourceSlot outputTank = this.fluidStorage().slot(OUTPUT_GAS_TANK);
        FluidResourceSlot tailingsTank = this.fluidStorage().slot(TAILINGS_GAS_TANK);

        long gasInputDroplets = GAS_INPUT_PER_CYCLE_MB * FluidUtil.MILLIBUCKET;
        long he3OutputDroplets = GAS_HE3_OUTPUT_MB * FluidUtil.MILLIBUCKET;
        long he4OutputDroplets = GAS_HE4_TAILINGS_MB * FluidUtil.MILLIBUCKET;

        boolean hasGasJob = !inputTank.isEmpty() && inputTank.getResource() == Gases.SOLAR_VOLATILE_GAS && inputTank.getAmount() >= gasInputDroplets;
        ItemResourceSlot solidSlot = this.itemStorage().slot(SOLID_INPUT_SLOT);
        SolidSeparationSpec solidSpec = !hasGasJob && !solidSlot.isEmpty()
                ? resolveSolidSeparationSpec(level, solidSlot.getResource())
                : null;

        if (!hasGasJob && solidSpec == null) {
            this.progress = 0;
            this.spinDown();
            return MachineStatuses.INVALID_RECIPE;
        }

        if (hasGasJob) {
            if (outputTank.tryInsert(Gases.HELIUM_3, he3OutputDroplets) < he3OutputDroplets) {
                this.spinDown();
                return GCMachineStatuses.GAS_TANK_FULL;
            }
        } else {
            if (solidSpec.outputGas != null && solidSpec.gasMb > 0L) {
                long solidGasDroplets = solidSpec.gasMb * FluidUtil.MILLIBUCKET;
                if (outputTank.tryInsert(solidSpec.outputGas, solidGasDroplets) < solidGasDroplets) {
                    this.spinDown();
                    return GCMachineStatuses.GAS_TANK_FULL;
                }
            }
            ItemResourceSlot byproductSlot = this.itemStorage().slot(BYPRODUCT_SLOT);
            if (solidSpec.byproduct != null && solidSpec.byproductCount > 0
                    && !byproductSlot.canInsert(solidSpec.byproduct, solidSpec.byproductCount)) {
                this.spinDown();
                return MachineStatuses.OUTPUT_FULL;
            }
        }

        // Stage 1: Spin up magnetic centrifuge rotor to >= 40,000 RPM for 3u vs 4u mass separation
        if (this.rotorRpm < TARGET_RPM) {
            try (Transaction tx = Transaction.openOuter()) {
                if (this.energyStorage().extract(ENERGY_CONSUMPTION, tx) < ENERGY_CONSUMPTION) {
                    this.spinDown();
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                tx.commit();
            }
            this.rotorRpm = Math.min(MAX_RPM, this.rotorRpm + RPM_ACCEL);
            return GCMachineStatuses.SPINNING_UP;
        }

        if (hasGasJob) {
            this.maxProgress = GAS_CYCLE_TICKS;
            if (this.progress + 1 >= GAS_CYCLE_TICKS) {
                try (Transaction tx = Transaction.openOuter()) {
                    if (this.energyStorage().extract(ENERGY_CONSUMPTION, tx) < ENERGY_CONSUMPTION) {
                        this.spinDown();
                        return MachineStatuses.NOT_ENOUGH_ENERGY;
                    }
                    if (inputTank.extract(Gases.SOLAR_VOLATILE_GAS, null, gasInputDroplets, tx) < gasInputDroplets) {
                        return MachineStatuses.INVALID_RECIPE;
                    }
                    if (outputTank.insert(Gases.HELIUM_3, DataComponentPatch.EMPTY, he3OutputDroplets, tx) < he3OutputDroplets) {
                        return GCMachineStatuses.GAS_TANK_FULL;
                    }
                    // Capture depleted 4He tailings into secondary tank when space is available
                    tailingsTank.insert(Gases.HELIUM, DataComponentPatch.EMPTY, he4OutputDroplets, tx);
                    tx.commit();
                }
                this.rotorRpm = Math.min(MAX_RPM, this.rotorRpm + 500);
                this.progress = 0;
                this.setChanged();
                return GCMachineStatuses.SEPARATING;
            }

            try (Transaction tx = Transaction.openOuter()) {
                if (this.energyStorage().extract(ENERGY_CONSUMPTION, tx) < ENERGY_CONSUMPTION) {
                    this.spinDown();
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                tx.commit();
            }
            this.rotorRpm = Math.min(MAX_RPM, this.rotorRpm + 500);
            this.progress++;
            return GCMachineStatuses.SEPARATING;
        }

        // Direct solid mineral / actinide / dense ice / scrap isotopic extraction
        int cycleTicks = Math.max(1, solidSpec.processingTime);
        this.maxProgress = cycleTicks;
        if (this.progress + 1 >= cycleTicks) {
            Item inputItem = solidSlot.getResource();
            try (Transaction tx = Transaction.openOuter()) {
                if (this.energyStorage().extract(ENERGY_CONSUMPTION, tx) < ENERGY_CONSUMPTION) {
                    this.spinDown();
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                if (inputItem == null || solidSlot.extract(inputItem, null, 1L, tx) < 1L) {
                    return MachineStatuses.INVALID_RECIPE;
                }
                if (solidSpec.outputGas != null && solidSpec.gasMb > 0L) {
                    long solidGasDroplets = solidSpec.gasMb * FluidUtil.MILLIBUCKET;
                    if (outputTank.insert(solidSpec.outputGas, DataComponentPatch.EMPTY, solidGasDroplets, tx) < solidGasDroplets) {
                        return GCMachineStatuses.GAS_TANK_FULL;
                    }
                    if (solidSpec.outputGas == Gases.HELIUM_3) {
                        long he4Tailings = (solidSpec.gasMb / 2L) * FluidUtil.MILLIBUCKET;
                        tailingsTank.insert(Gases.HELIUM, DataComponentPatch.EMPTY, he4Tailings, tx);
                    }
                }
                if (solidSpec.byproduct != null && solidSpec.byproductCount > 0) {
                    ItemResourceSlot byproductSlot = this.itemStorage().slot(BYPRODUCT_SLOT);
                    if (byproductSlot.insert(solidSpec.byproduct, DataComponentPatch.EMPTY, solidSpec.byproductCount, tx) < solidSpec.byproductCount) {
                        return MachineStatuses.OUTPUT_FULL;
                    }
                }
                tx.commit();
            }
            this.rotorRpm = Math.min(MAX_RPM, this.rotorRpm + 500);
            this.progress = 0;
            this.setChanged();
            return GCMachineStatuses.SEPARATING;
        }

        try (Transaction tx = Transaction.openOuter()) {
            if (this.energyStorage().extract(ENERGY_CONSUMPTION, tx) < ENERGY_CONSUMPTION) {
                this.spinDown();
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }
        this.rotorRpm = Math.min(MAX_RPM, this.rotorRpm + 500);
        this.progress++;
        return GCMachineStatuses.SEPARATING;
    }

    @Override
    protected void tickDisabled(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        this.spinDown();
    }

    private void spinDown() {
        if (this.rotorRpm > 0) {
            this.rotorRpm = Math.max(0, this.rotorRpm - RPM_DECAY);
        }
    }

    private static @Nullable SolidSeparationSpec resolveSolidSeparationSpec(ServerLevel level, Item item) {
        Optional<RecipeHolder<CentrifugeRecipe>> match = level.getRecipeManager()
                .getRecipeFor(GCRecipes.CENTRIFUGE_TYPE, new SingleRecipeInput(new ItemStack(item, 1)), level);
        if (match.isPresent()) {
            CentrifugeRecipe recipe = match.get().value();
            ItemStack outStack = recipe.getOutputItem();
            return new SolidSeparationSpec(
                    recipe.getOutputGas(),
                    recipe.getOutputGasMb(),
                    outStack.isEmpty() ? null : outStack.getItem(),
                    outStack.isEmpty() ? 0 : outStack.getCount(),
                    recipe.getProcessingTime()
            );
        }
        return getSolidSeparationSpecFallback(item);
    }

    private static @Nullable SolidSeparationSpec getSolidSeparationSpecFallback(Item item) {
        if (item == GCBlocks.DENSE_ICE.asItem()) {
            return new SolidSeparationSpec(Gases.DEUTERIUM, 250L, null, 0, SOLID_CYCLE_TICKS);
        }
        if (item == GCBlocks.SATURN_HELIUM_MANTLE_ROCK.asItem()) {
            return new SolidSeparationSpec(Gases.HELIUM_3, 350L, null, 0, SOLID_CYCLE_TICKS);
        }
        if (item == GCBlocks.MOON_ILMENITE_ORE.asItem()) {
            return new SolidSeparationSpec(Gases.HELIUM_3, 200L, GCItems.TITANIUM_NUGGET, 1, SOLID_CYCLE_TICKS);
        }
        if (item == GCBlocks.MERCURY_HOLLOW_DEPOSIT.asItem()) {
            return new SolidSeparationSpec(Gases.HELIUM_3, 150L, GCItems.SILICON, 1, SOLID_CYCLE_TICKS);
        }
        if (item == GCBlocks.MOON_BASALT.asItem()) {
            return new SolidSeparationSpec(Gases.HELIUM_3, 40L, GCItems.SILICON, 1, SOLID_CYCLE_TICKS);
        }
        if (item == GCBlocks.MOON_TURF.asItem() || item == GCBlocks.MOON_DIRT.asItem()) {
            return new SolidSeparationSpec(Gases.HELIUM_3, 20L, GCItems.SILICON, 1, SOLID_CYCLE_TICKS);
        }
        if (item == GCBlocks.MERCURY_SULFIDE_ORE.asItem()) {
            return new SolidSeparationSpec(null, 0L, GCItems.RADIOISOTOPE_CORE, 1, 80);
        }
        if (item == GCItems.URANIUM_INGOT) {
            return new SolidSeparationSpec(null, 0L, GCItems.RADIOISOTOPE_CORE, 2, 80);
        }
        return null;
    }

    private record SolidSeparationSpec(@Nullable Fluid outputGas, long gasMb, @Nullable Item byproduct, int byproductCount, int processingTime) {
    }

    public int getRotorRpm() {
        return this.rotorRpm;
    }

    public void setRotorRpm(int rotorRpm) {
        this.rotorRpm = rotorRpm;
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
        tag.putInt("rotor_rpm", this.rotorRpm);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.rotorRpm = tag.getInt("rotor_rpm");
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : GAS_CYCLE_TICKS;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new IsotopicCentrifugeMenu(syncId, (ServerPlayer) player, this);
    }
}
