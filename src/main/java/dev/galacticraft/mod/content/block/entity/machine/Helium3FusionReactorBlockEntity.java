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
import dev.galacticraft.machinelib.api.util.EnergySource;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.FusionReactorRecipe;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.screen.Helium3FusionReactorMenu;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Aneutronic D-3He Magnetic-Hydrodynamic (MHD) Fusion Reactor.
 * <p>
 * Reaction: D + 3He -> 4He + p+ + 18.3 MeV (direct MHD conversion, zero neutron radiation).
 * Generates 2,500 GJ/t baseline with Nb3Sn Superconducting Coil, and 5,000 GJ/t with <2K superfluid
 * Helium-3 Cell cryogenic cooling. Performs a controlled magnetic quench-safe shutdown with zero
 * explosion or meltdown risk if confinement or fuel is lost.
 */
public class Helium3FusionReactorBlockEntity extends MachineBlockEntity {
    public static final int HELIUM3_CANISTER_SLOT = 0;
    public static final int DEUTERIUM_CANISTER_SLOT = 1;
    public static final int COIL_SLOT = 2;
    public static final int CRYO_COOLANT_SLOT = 3;
    public static final int OUTPUT_CELL_SLOT = 4;

    public static final int HELIUM3_TANK = 0;
    public static final int DEUTERIUM_TANK = 1;

    public static final long ENERGY_CAPACITY = 1_000_000L;
    public static final long MAX_ENERGY_OUTPUT = 10_000L;
    public static final long BASE_GENERATION_RATE = 2_500L;
    public static final long CRYO_GENERATION_RATE = 5_000L;
    public static final long GAS_TANK_CAPACITY = FluidUtil.bucketsToDroplets(16);

    public static final long DEFAULT_CYCLE_GAS_MB = 10L;
    public static final int DEFAULT_CYCLE_TICKS = 40;
    public static final int CELL_SYNTHESIS_TICKS = 100;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(34, 62)
                            .capacity(1)
                            .filter(ResourceFilters.canExtractFluid(Gases.HELIUM_3))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(58, 62)
                            .capacity(1)
                            .filter(ResourceFilters.canExtractFluid(Gases.DEUTERIUM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(96, 18)
                            .capacity(1)
                            .filter(ResourceFilters.ofResource(GCItems.SUPERCONDUCTING_COIL)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(96, 46)
                            .capacity(1)
                            .filter(ResourceFilters.ofResource(GCItems.HELIUM3_CELL)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(138, 32)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_INSERT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY))
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    0,
                    MAX_ENERGY_OUTPUT
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(34, 8)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.HELIUM_3)),
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(58, 8)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.DEUTERIUM))
            )
    );

    private final EnergySource energySource = new EnergySource(this);
    private int burnTime = 0;
    private int burnDuration = DEFAULT_CYCLE_TICKS;
    private int cellSynthesisProgress = 0;
    private long activeGenerationRate = 0L;
    private boolean cryoCooled = false;

    public Helium3FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.HELIUM3_FUSION_REACTOR, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.takeFluidFromSlot(HELIUM3_CANISTER_SLOT, HELIUM3_TANK, Gases.HELIUM_3);
        this.takeFluidFromSlot(DEUTERIUM_CANISTER_SLOT, DEUTERIUM_TANK, Gases.DEUTERIUM);
        this.drainPowerToSlot(OUTPUT_CELL_SLOT);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        profiler.push("energy_spread");
        this.energySource.trySpreadEnergy(level, pos, state);
        profiler.pop();

        ItemResourceSlot coilSlot = this.itemStorage().slot(COIL_SLOT);
        if (coilSlot.isEmpty() || coilSlot.getResource() != GCItems.SUPERCONDUCTING_COIL) {
            // Magnetic quench-safe shutdown: no explosion, no radiation, no block damage
            if (this.burnTime > 0) {
                this.burnTime = 0;
                this.activeGenerationRate = 0L;
                this.cryoCooled = false;
                return GCMachineStatuses.MAGNETIC_QUENCH_SAFE;
            }
            this.activeGenerationRate = 0L;
            this.cryoCooled = false;
            return GCMachineStatuses.MISSING_COIL;
        }

        FusionReactorRecipe recipe = this.findFusionRecipe(level);
        long he3Required = recipe != null ? recipe.getPrimaryGasDroplets() : DEFAULT_CYCLE_GAS_MB * FluidUtil.MILLIBUCKET;
        long d2Required = recipe != null ? recipe.getSecondaryGasDroplets() : DEFAULT_CYCLE_GAS_MB * FluidUtil.MILLIBUCKET;
        long baseRate = recipe != null ? recipe.getEnergyPerTick() : BASE_GENERATION_RATE;
        int cycleTicks = recipe != null ? Math.max(1, recipe.getCycleTicks()) : DEFAULT_CYCLE_TICKS;

        if (this.burnTime <= 0) {
            FluidResourceSlot he3Tank = this.fluidStorage().slot(HELIUM3_TANK);
            FluidResourceSlot d2Tank = this.fluidStorage().slot(DEUTERIUM_TANK);

            if (he3Tank.isEmpty() || he3Tank.getResource() != Gases.HELIUM_3 || he3Tank.getAmount() < he3Required
                    || d2Tank.isEmpty() || d2Tank.getResource() != Gases.DEUTERIUM || d2Tank.getAmount() < d2Required) {
                this.activeGenerationRate = 0L;
                this.cryoCooled = false;
                return GCMachineStatuses.MISSING_FUSION_FUEL;
            }

            if (this.energyStorage().isFull()) {
                this.activeGenerationRate = 0L;
                return MachineStatuses.CAPACITOR_FULL;
            }

            he3Tank.extract(Gases.HELIUM_3, he3Required);
            d2Tank.extract(Gases.DEUTERIUM, d2Required);
            this.burnTime = cycleTicks;
            this.burnDuration = cycleTicks;
        }

        ItemResourceSlot cryoSlot = this.itemStorage().slot(CRYO_COOLANT_SLOT);
        this.cryoCooled = !cryoSlot.isEmpty() && cryoSlot.getResource() == GCItems.HELIUM3_CELL;
        long generationRate = this.cryoCooled ? Math.max(CRYO_GENERATION_RATE, baseRate * 2L) : baseRate;
        this.activeGenerationRate = generationRate;

        this.energyStorage().insert(generationRate);
        this.energySource.trySpreadEnergy(level, pos, state);
        this.burnTime--;

        // Synthesize Helium-3 Fusion Cell into empty output slot during active fusion
        ItemResourceSlot outputSlot = this.itemStorage().slot(OUTPUT_CELL_SLOT);
        if (outputSlot.isEmpty()) {
            this.cellSynthesisProgress++;
            if (this.cellSynthesisProgress >= CELL_SYNTHESIS_TICKS) {
                ItemStack outStack = recipe != null && !recipe.getOutputCell().isEmpty()
                        ? recipe.getOutputCell()
                        : new ItemStack(GCItems.HELIUM3_FUSION_CELL);
                if (outputSlot.canInsert(outStack.getItem(), 1)) {
                    outputSlot.insert(outStack.getItem(), 1);
                    this.cellSynthesisProgress = 0;
                    this.setChanged();
                }
            }
        } else {
            this.cellSynthesisProgress = 0;
        }

        if (this.energyStorage().isFull()) {
            return MachineStatuses.CAPACITOR_FULL;
        }
        return this.cryoCooled ? GCMachineStatuses.FUSING_CRYO : GCMachineStatuses.FUSING;
    }

    @Override
    protected void tickDisabled(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        this.activeGenerationRate = 0L;
        this.cryoCooled = false;
    }

    private @Nullable FusionReactorRecipe findFusionRecipe(ServerLevel level) {
        List<RecipeHolder<FusionReactorRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(GCRecipes.FUSION_REACTOR_TYPE);
        for (RecipeHolder<FusionReactorRecipe> holder : recipes) {
            FusionReactorRecipe recipe = holder.value();
            if (recipe.getPrimaryGas() == Gases.HELIUM_3 && recipe.getSecondaryGas() == Gases.DEUTERIUM) {
                return recipe;
            }
        }
        return null;
    }

    public int getBurnTime() {
        return this.burnTime;
    }

    public int getBurnDuration() {
        return this.burnDuration;
    }

    public int getActiveGenerationRate() {
        return (int) Math.min(Integer.MAX_VALUE, this.activeGenerationRate);
    }

    public boolean isCryoCooled() {
        return this.cryoCooled;
    }

    public int getCellSynthesisProgress() {
        return this.cellSynthesisProgress;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("burn_time", this.burnTime);
        tag.putInt("burn_duration", this.burnDuration);
        tag.putInt("cell_synthesis_progress", this.cellSynthesisProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.burnTime = tag.getInt("burn_time");
        this.burnDuration = tag.contains("burn_duration") ? Math.max(1, tag.getInt("burn_duration")) : DEFAULT_CYCLE_TICKS;
        this.cellSynthesisProgress = tag.getInt("cell_synthesis_progress");
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new Helium3FusionReactorMenu(syncId, (ServerPlayer) player, this);
    }
}
