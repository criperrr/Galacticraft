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
import dev.galacticraft.machinelib.api.util.FluidSource;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.recipe.SabatierRecipe;
import dev.galacticraft.mod.screen.SabatierChemicalReactorMenu;
import dev.galacticraft.mod.util.FluidUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
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

/**
 * Catalytic Sabatier Chemical Reactor for planetary ISRU propellant synthesis:
 * - Mars Sabatier cycle: CO2 + 4 H2 -> Liquid Methane (CH4) + 2 H2O (recycled water)
 * - Hydrazine RCS/OMS cycle: N2 + 2 H2 -> Monopropellant (N2H4)
 * - Contact Acid cycle: Mercury Sulfide + H2O -> Sulfuric Acid (H2SO4)
 * - Cryo Xenon liquefaction: Xenon + LOX -> Liquid Xenon + O2
 * Uses a non-consumed Niobium (1.0x) or Palladium (1.5x supercritical) catalyst bed.
 */
public class SabatierChemicalReactorBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int REAGENT_A_CANISTER_SLOT = 1;
    public static final int REAGENT_B_CANISTER_SLOT = 2;
    public static final int CATALYST_SLOT = 3;
    public static final int SOLID_REAGENT_SLOT = 4;
    public static final int PRODUCT_A_CANISTER_SLOT = 5;
    public static final int PRODUCT_B_CANISTER_SLOT = 6;

    public static final int REAGENT_A_TANK = 0;
    public static final int REAGENT_B_TANK = 1;
    public static final int PRODUCT_A_TANK = 2;
    public static final int PRODUCT_B_TANK = 3;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 110L;
    public static final long TANK_CAPACITY = FluidUtil.bucketsToDroplets(16);

    public static final int AMBIENT_TEMP_C = 20;
    public static final int MAX_TEMP_C = 450;

    public static final int CATALYST_NONE = 0;
    public static final int CATALYST_NIOBIUM = 1;
    public static final int CATALYST_PALLADIUM = 2;

    public static final int REACTION_STANDBY = 0;
    public static final int REACTION_METHALOX = 1;
    public static final int REACTION_HYDRAZINE = 2;
    public static final int REACTION_SULFURIC = 3;
    public static final int REACTION_XENON = 4;

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
                            .filter(ResourceFilters.providesApi(FluidStorage.ITEM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(52, 76)
                            .capacity(1)
                            .filter(ResourceFilters.providesApi(FluidStorage.ITEM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(77, 23)
                            .capacity(1)
                            .filter((item, components) -> item == GCItems.NIOBIUM_INGOT || item == GCItems.PALLADIUM_INGOT),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(77, 52)
                            .filter((item, components) -> item == GCBlocks.MERCURY_SULFIDE_ORE.asItem()),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(128, 76)
                            .capacity(1)
                            .filter(ResourceFilters.providesApi(FluidStorage.ITEM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(152, 76)
                            .capacity(1)
                            .filter(ResourceFilters.providesApi(FluidStorage.ITEM))
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
                            .capacity(TANK_CAPACITY),
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(52, 22)
                            .capacity(TANK_CAPACITY),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(128, 22)
                            .capacity(TANK_CAPACITY),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(152, 22)
                            .capacity(TANK_CAPACITY)
            )
    );

    private final FluidSource fluidSource = new FluidSource(this);
    private final RecipeManager.CachedCheck<SabatierRecipe.Input, SabatierRecipe> quickCheck =
            RecipeManager.createCheck(GCRecipes.SABATIER_TYPE);
    private int progress = 0;
    private int maxProgress = 40;
    private int temperatureC = AMBIENT_TEMP_C;
    private int targetTempC = 350;
    private int catalystTier = CATALYST_NONE;
    private int activeReactionCode = REACTION_STANDBY;

    public SabatierChemicalReactorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.SABATIER_CHEMICAL_REACTOR, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
        this.takeFluidFromSlot(REAGENT_A_CANISTER_SLOT, REAGENT_A_TANK);
        this.takeFluidFromSlot(REAGENT_B_CANISTER_SLOT, REAGENT_B_TANK);
        this.drainFluidToSlot(PRODUCT_A_CANISTER_SLOT, PRODUCT_A_TANK);
        this.drainFluidToSlot(PRODUCT_B_CANISTER_SLOT, PRODUCT_B_TANK);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        profiler.push("transfer");
        this.fluidSource.trySpreadFluids(level, pos, state);
        profiler.pop();

        ItemResourceSlot catalystSlot = this.itemStorage().slot(CATALYST_SLOT);
        Item catalystItem = catalystSlot.isEmpty() ? null : catalystSlot.getResource();
        if (catalystItem == GCItems.PALLADIUM_INGOT) {
            this.catalystTier = CATALYST_PALLADIUM;
        } else if (catalystItem == GCItems.NIOBIUM_INGOT) {
            this.catalystTier = CATALYST_NIOBIUM;
        } else {
            this.catalystTier = CATALYST_NONE;
        }

        FluidResourceSlot tankA = this.fluidStorage().slot(REAGENT_A_TANK);
        FluidResourceSlot tankB = this.fluidStorage().slot(REAGENT_B_TANK);
        FluidResourceSlot outA = this.fluidStorage().slot(PRODUCT_A_TANK);
        FluidResourceSlot outB = this.fluidStorage().slot(PRODUCT_B_TANK);
        ItemResourceSlot solidSlot = this.itemStorage().slot(SOLID_REAGENT_SLOT);

        if (tankA.isEmpty() && tankB.isEmpty() && solidSlot.isEmpty()) {
            this.progress = 0;
            this.activeReactionCode = REACTION_STANDBY;
            this.coolReactor();
            return MachineStatuses.INVALID_RECIPE;
        }

        Fluid fluidA = tankA.isEmpty() ? Fluids.EMPTY : tankA.getResource();
        Fluid fluidB = tankB.isEmpty() ? Fluids.EMPTY : tankB.getResource();
        ItemStack solidStack = solidSlot.isEmpty() ? ItemStack.EMPTY : new ItemStack(solidSlot.getResource(), 1);
        ItemStack catalystStack = catalystItem == null ? ItemStack.EMPTY : new ItemStack(catalystItem, 1);

        SabatierRecipe.Input recipeInput = new SabatierRecipe.Input(
                fluidA, tankA.getAmount(),
                fluidB, tankB.getAmount(),
                solidStack, catalystStack
        );
        Optional<RecipeHolder<SabatierRecipe>> match = this.quickCheck.getRecipeFor(recipeInput, level);

        if (match.isEmpty()) {
            this.progress = 0;
            this.activeReactionCode = REACTION_STANDBY;
            this.coolReactor();
            if (catalystItem == null && (!tankA.isEmpty() || !tankB.isEmpty())) {
                return GCMachineStatuses.MISSING_CATALYST;
            }
            return MachineStatuses.INVALID_RECIPE;
        }

        SabatierRecipe recipe = match.get().value();
        this.targetTempC = Math.max(100, recipe.getTargetTempC());
        int baseTicks = Math.max(1, recipe.getProcessingTime());
        // Palladium supercritical catalysis delivers 1.5x faster reaction kinetics
        this.maxProgress = this.catalystTier == CATALYST_PALLADIUM ? Math.max(1, (baseTicks * 2) / 3) : baseTicks;
        this.activeReactionCode = classifyReaction(recipe.getProductA());

        Fluid prodA = recipe.getProductA();
        long prodADroplets = recipe.getProductADroplets();
        Fluid prodB = recipe.getProductB();
        long prodBDroplets = recipe.getProductBDroplets();

        if (outA.tryInsert(prodA, prodADroplets) < prodADroplets) {
            this.coolReactor();
            return GCMachineStatuses.GAS_TANK_FULL;
        }
        if (prodB != null && prodBDroplets > 0L && outB.tryInsert(prodB, prodBDroplets) < prodBDroplets) {
            this.coolReactor();
            return GCMachineStatuses.GAS_TANK_FULL;
        }

        long energyCost = Math.max(1L, recipe.getEnergyPerTick());

        // Stage 1: Heat catalytic bed to activation temperature
        if (this.temperatureC < this.targetTempC) {
            try (Transaction tx = Transaction.openOuter()) {
                long drawn = this.energyStorage().extract(energyCost, tx);
                if (drawn < energyCost) {
                    this.coolReactor();
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                tx.commit();
            }
            int heatStep = this.catalystTier == CATALYST_PALLADIUM ? 22 : 15;
            this.temperatureC = Math.min(this.targetTempC, this.temperatureC + heatStep);
            return GCMachineStatuses.HEATING_CATALYST;
        }

        // Final tick: atomic synthesis transaction
        if (this.progress + 1 >= this.maxProgress) {
            boolean directOrder = recipe.matchesDirect(recipeInput);
            try (Transaction tx = Transaction.openOuter()) {
                long drawn = this.energyStorage().extract(energyCost, tx);
                if (drawn < energyCost) {
                    this.coolReactor();
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }

                FluidResourceSlot slotForReqA = directOrder ? tankA : tankB;
                FluidResourceSlot slotForReqB = directOrder ? tankB : tankA;

                long reqADroplets = recipe.getReagentADroplets();
                if (slotForReqA.extract(recipe.getReagentA(), null, reqADroplets, tx) < reqADroplets) {
                    return MachineStatuses.INVALID_RECIPE;
                }
                if (recipe.getReagentB() != null && recipe.getReagentBMb() > 0L) {
                    long reqBDroplets = recipe.getReagentBDroplets();
                    if (slotForReqB.extract(recipe.getReagentB(), null, reqBDroplets, tx) < reqBDroplets) {
                        return MachineStatuses.INVALID_RECIPE;
                    }
                }
                if (!recipe.getSolidReagent().isEmpty()) {
                    Item solidItem = solidSlot.getResource();
                    if (solidItem == null || solidSlot.extract(solidItem, null, 1L, tx) < 1L) {
                        return MachineStatuses.INVALID_RECIPE;
                    }
                }

                if (outA.insert(prodA, DataComponentPatch.EMPTY, prodADroplets, tx) < prodADroplets) {
                    return GCMachineStatuses.GAS_TANK_FULL;
                }
                if (prodB != null && prodBDroplets > 0L) {
                    if (outB.insert(prodB, DataComponentPatch.EMPTY, prodBDroplets, tx) < prodBDroplets) {
                        return GCMachineStatuses.GAS_TANK_FULL;
                    }
                }
                tx.commit();
            }

            this.progress = 0;
            this.temperatureC = Math.min(MAX_TEMP_C, this.temperatureC + 4);
            this.setChanged();
            return GCMachineStatuses.SYNTHESIZING;
        }

        // Intermediate tick: atomic energy draw
        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(energyCost, tx);
            if (drawn < energyCost) {
                this.coolReactor();
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        return GCMachineStatuses.SYNTHESIZING;
    }

    @Override
    protected void tickDisabled(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        this.coolReactor();
    }

    private void coolReactor() {
        if (this.temperatureC > AMBIENT_TEMP_C) {
            this.temperatureC = Math.max(AMBIENT_TEMP_C, this.temperatureC - 5);
        }
    }

    private static int classifyReaction(Fluid productA) {
        if (productA == GCFluids.LIQUID_METHANE) {
            return REACTION_METHALOX;
        }
        if (productA == GCFluids.MONOPROPELLANT) {
            return REACTION_HYDRAZINE;
        }
        if (productA == GCFluids.SULFURIC_ACID) {
            return REACTION_SULFURIC;
        }
        if (productA == GCFluids.LIQUID_XENON) {
            return REACTION_XENON;
        }
        return REACTION_STANDBY;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    public int getTemperatureC() {
        return this.temperatureC;
    }

    public void setTemperatureC(int temperatureC) {
        this.temperatureC = temperatureC;
    }

    public int getTargetTempC() {
        return this.targetTempC;
    }

    public int getCatalystTier() {
        return this.catalystTier;
    }

    public int getActiveReactionCode() {
        return this.activeReactionCode;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
        tag.putInt("temperature_c", this.temperatureC);
        tag.putInt("target_temp_c", this.targetTempC);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : 40;
        this.temperatureC = tag.contains("temperature_c") ? tag.getInt("temperature_c") : AMBIENT_TEMP_C;
        this.targetTempC = tag.contains("target_temp_c") ? tag.getInt("target_temp_c") : 350;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new SabatierChemicalReactorMenu(syncId, (ServerPlayer) player, this);
    }
}
