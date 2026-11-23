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
import dev.galacticraft.mod.screen.IsotopicCentrifugeMenu;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IsotopicCentrifugeBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int COIL_SLOT = 1;
    public static final int SOLID_INPUT_SLOT = 2;
    public static final int INPUT_CANISTER_SLOT = 3;
    public static final int OUTPUT_CANISTER_SLOT = 4;
    public static final int BYPRODUCT_SLOT = 5;

    public static final int INPUT_GAS_TANK = 0;
    public static final int OUTPUT_GAS_TANK = 1;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 110L;
    public static final long GAS_TANK_CAPACITY = FluidUtil.bucketsToDroplets(16);

    public static final long GAS_INPUT_PER_CYCLE_MB = 50L;
    public static final long GAS_OUTPUT_PER_CYCLE_MB = 20L;
    public static final int GAS_CYCLE_TICKS = 10;
    public static final int SOLID_CYCLE_TICKS = 60;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 62)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(74, 18)
                            .capacity(1)
                            .filter(ResourceFilters.ofResource(GCItems.SUPERCONDUCTING_COIL)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(74, 46),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(42, 62)
                            .capacity(1)
                            .filter(ResourceFilters.canExtractFluid(Gases.SOLAR_VOLATILE_GAS))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(152, 62)
                            .capacity(1)
                            .filter(ResourceFilters.canInsertFluid(Gases.HELIUM_3))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(106, 46)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(42, 8)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.SOLAR_VOLATILE_GAS)),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(152, 8)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.HELIUM_3))
            )
    );

    private final FluidSource fluidSource = new FluidSource(this);
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
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        profiler.push("transfer");
        this.fluidSource.trySpreadFluids(level, pos, state);
        profiler.pop();

        ItemResourceSlot coilSlot = this.itemStorage().slot(COIL_SLOT);
        if (coilSlot.isEmpty() || coilSlot.getResource() != GCItems.SUPERCONDUCTING_COIL) {
            this.progress = 0;
            return GCMachineStatuses.MISSING_COIL;
        }

        FluidResourceSlot inputTank = this.fluidStorage().slot(INPUT_GAS_TANK);
        FluidResourceSlot outputTank = this.fluidStorage().slot(OUTPUT_GAS_TANK);

        long gasInputDroplets = GAS_INPUT_PER_CYCLE_MB * FluidUtil.MILLIBUCKET;
        long gasOutputDroplets = GAS_OUTPUT_PER_CYCLE_MB * FluidUtil.MILLIBUCKET;

        // Primary mode: Magnetic isotopic separation of SOLAR_VOLATILE_GAS -> HELIUM_3
        if (!inputTank.isEmpty() && inputTank.getResource() == Gases.SOLAR_VOLATILE_GAS && inputTank.getAmount() >= gasInputDroplets) {
            this.maxProgress = GAS_CYCLE_TICKS;
            if (outputTank.tryInsert(Gases.HELIUM_3, gasOutputDroplets) < gasOutputDroplets) {
                return GCMachineStatuses.GAS_TANK_FULL;
            }
            if (!this.energyStorage().canExtract(ENERGY_CONSUMPTION)) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }

            this.energyStorage().extract(ENERGY_CONSUMPTION);
            this.progress++;
            if (this.progress >= GAS_CYCLE_TICKS) {
                inputTank.extract(Gases.SOLAR_VOLATILE_GAS, gasInputDroplets);
                outputTank.insert(Gases.HELIUM_3, gasOutputDroplets);
                this.progress = 0;
                this.setChanged();
            }
            return GCMachineStatuses.SEPARATING;
        }

        // Secondary mode: Direct solid mineral isotopic extraction
        ItemResourceSlot solidSlot = this.itemStorage().slot(SOLID_INPUT_SLOT);
        if (!solidSlot.isEmpty()) {
            SolidSeparationSpec solidSpec = getSolidSeparationSpec(solidSlot.getResource());
            if (solidSpec != null) {
                this.maxProgress = SOLID_CYCLE_TICKS;
                long solidGasDroplets = solidSpec.helium3Mb * FluidUtil.MILLIBUCKET;
                if (outputTank.tryInsert(Gases.HELIUM_3, solidGasDroplets) < solidGasDroplets) {
                    return GCMachineStatuses.GAS_TANK_FULL;
                }
                ItemResourceSlot byproductSlot = this.itemStorage().slot(BYPRODUCT_SLOT);
                if (solidSpec.byproduct != null && !byproductSlot.canInsert(solidSpec.byproduct, 1)) {
                    return MachineStatuses.OUTPUT_FULL;
                }
                if (!this.energyStorage().canExtract(ENERGY_CONSUMPTION)) {
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }

                this.energyStorage().extract(ENERGY_CONSUMPTION);
                this.progress++;
                if (this.progress >= SOLID_CYCLE_TICKS) {
                    solidSlot.extractOne();
                    outputTank.insert(Gases.HELIUM_3, solidGasDroplets);
                    if (solidSpec.byproduct != null) {
                        byproductSlot.insert(solidSpec.byproduct, 1);
                    }
                    this.progress = 0;
                    this.setChanged();
                }
                return GCMachineStatuses.SEPARATING;
            }
        }

        this.progress = 0;
        return MachineStatuses.INVALID_RECIPE;
    }

    private static @Nullable SolidSeparationSpec getSolidSeparationSpec(Item item) {
        if (item == GCBlocks.SATURN_HELIUM_MANTLE_ROCK.asItem()) {
            return new SolidSeparationSpec(350L, null);
        }
        if (item == GCBlocks.MOON_ILMENITE_ORE.asItem()) {
            return new SolidSeparationSpec(200L, GCItems.TITANIUM_NUGGET);
        }
        if (item == GCBlocks.MERCURY_HOLLOW_DEPOSIT.asItem()) {
            return new SolidSeparationSpec(150L, GCItems.SILICON);
        }
        if (item == GCBlocks.MOON_BASALT.asItem()) {
            return new SolidSeparationSpec(40L, GCItems.SILICON);
        }
        if (item == GCBlocks.MOON_TURF.asItem() || item == GCBlocks.MOON_DIRT.asItem()) {
            return new SolidSeparationSpec(20L, GCItems.SILICON);
        }
        return null;
    }

    private record SolidSeparationSpec(long helium3Mb, @Nullable Item byproduct) {
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
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : GAS_CYCLE_TICKS;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new IsotopicCentrifugeMenu(syncId, (ServerPlayer) player, this);
    }
}
