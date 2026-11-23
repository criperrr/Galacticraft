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
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.machine.FusionReactorPartBlock;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.FusionReactorRecipe;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.screen.Helium3FusionReactorMenu;
import dev.galacticraft.mod.util.FluidUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 3x3x3 Multiblock Aneutronic D-3He Magnetic-Hydrodynamic (MHD) Fusion Reactor.
 * <p>
 * Structure (centered 1 block behind the controller):
 * - Bottom & Top layers (y-1, y+1): 8x FUSION_REACTOR_CASING perimeter + 1x FUSION_MAGNET_COIL center
 * - Middle layer (y): 4x FUSION_MAGNET_COIL corners, controller in front center, 3x FUSION_PLASMA_GLASS
 *   (or FUSION_REACTOR_CASING) on the remaining 3 faces, and a hollow air core in the center for the plasma vortex.
 * <p>
 * Reaction: D + 3He -> 4He + p+ + 18.3 MeV (direct MHD conversion, zero neutron radiation).
 * Ramps plasma temperature to 150 MK ignition threshold, then generates 2,500 GJ/t baseline (12.0 T)
 * or 5,000 GJ/t with <2K superfluid Helium-3 Cell cryogenic cooling (18.5 T).
 */
public class Helium3FusionReactorBlockEntity extends MachineBlockEntity {
    public static final int HELIUM3_CANISTER_SLOT = 0;
    public static final int DEUTERIUM_CANISTER_SLOT = 1;
    public static final int COIL_SLOT = 2;
    public static final int CRYO_COOLANT_SLOT = 3;
    public static final int OUTPUT_CELL_SLOT = 4;
    public static final int IGNITION_CHARGE_SLOT = 5;

    public static final int HELIUM3_TANK = 0;
    public static final int DEUTERIUM_TANK = 1;

    public static final int REQUIRED_MULTIBLOCK_BLOCKS = 26;

    public static final long ENERGY_CAPACITY = 1_000_000L;
    public static final long MAX_ENERGY_INPUT = 2_000L;
    public static final long MAX_ENERGY_OUTPUT = 10_000L;
    public static final long BASE_GENERATION_RATE = 2_500L;
    public static final long CRYO_GENERATION_RATE = 5_000L;
    public static final long GAS_TANK_CAPACITY = FluidUtil.bucketsToDroplets(16);

    public static final int IGNITION_TEMP_MK = 150;
    public static final int MAX_PLASMA_TEMP_MK = 220;
    public static final int PLASMA_HEAT_RATE_MK = 15;
    public static final int PLASMA_COOL_RATE_MK = 12;

    public static final long DEFAULT_CYCLE_GAS_MB = 10L;
    public static final int DEFAULT_CYCLE_TICKS = 40;
    public static final int CELL_SYNTHESIS_TICKS = 100;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(32, 74)
                            .capacity(1)
                            .filter(ResourceFilters.canExtractFluid(Gases.HELIUM_3))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(54, 74)
                            .capacity(1)
                            .filter(ResourceFilters.canExtractFluid(Gases.DEUTERIUM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(82, 22)
                            .capacity(1)
                            .filter(ResourceFilters.ofResource(GCItems.SUPERCONDUCTING_COIL)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(82, 48)
                            .capacity(1)
                            .filter(ResourceFilters.ofResource(GCItems.HELIUM3_CELL)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(148, 35)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_INSERT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 74)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY))
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    MAX_ENERGY_INPUT,
                    MAX_ENERGY_OUTPUT
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(32, 20)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.HELIUM_3)),
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(54, 20)
                            .capacity(GAS_TANK_CAPACITY)
                            .filter(ResourceFilters.ofResource(Gases.DEUTERIUM))
            )
    );

    private final EnergySource energySource = new EnergySource(this);
    private int validStructureBlocks = 0;
    private boolean multiblockFormed = false;
    private int plasmaTempMk = 0;
    private int burnTime = 0;
    private int burnDuration = DEFAULT_CYCLE_TICKS;
    private int cellSynthesisProgress = 0;
    private long activeGenerationRate = 0L;
    private boolean cryoCooled = false;

    public Helium3FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.HELIUM3_FUSION_REACTOR, pos, state, SPEC);
    }

    public static BlockPos getCorePos(BlockPos controllerPos, BlockState state) {
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        return controllerPos.relative(facing.getOpposite());
    }

    /**
     * Builds the canonical 3x3x3 toroidal multiblock around the given controller position.
     */
    public static void buildMultiblock(Level level, BlockPos controllerPos, Direction facing) {
        BlockState controllerState = GCBlocks.HELIUM3_FUSION_REACTOR.defaultBlockState();
        if (controllerState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            controllerState = controllerState.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        level.setBlock(controllerPos, controllerState, 3);
        BlockPos core = controllerPos.relative(facing.getOpposite());

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = core.offset(dx, dy, dz);
                    if (p.equals(controllerPos)) {
                        continue;
                    }
                    if (dx == 0 && dy == 0 && dz == 0) {
                        level.removeBlock(p, false);
                    } else if (dy != 0) {
                        if (dx == 0 && dz == 0) {
                            level.setBlock(p, GCBlocks.FUSION_MAGNET_COIL.defaultBlockState(), 3);
                        } else {
                            level.setBlock(p, GCBlocks.FUSION_REACTOR_CASING.defaultBlockState(), 3);
                        }
                    } else {
                        if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
                            level.setBlock(p, GCBlocks.FUSION_MAGNET_COIL.defaultBlockState(), 3);
                        } else {
                            level.setBlock(p, GCBlocks.FUSION_PLASMA_GLASS.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    public int validateMultiblock(Level level, BlockPos controllerPos, BlockState state) {
        BlockPos core = getCorePos(controllerPos, state);
        if (!level.getBlockState(core).isAir()) {
            this.validStructureBlocks = 0;
            this.multiblockFormed = false;
            return 0;
        }

        int valid = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = core.offset(dx, dy, dz);
                    if (p.equals(controllerPos)) {
                        valid++;
                        continue;
                    }
                    BlockState bs = level.getBlockState(p);
                    if (dy != 0) {
                        if (dx == 0 && dz == 0) {
                            if (bs.is(GCBlocks.FUSION_MAGNET_COIL)) valid++;
                        } else {
                            if (bs.is(GCBlocks.FUSION_REACTOR_CASING)) valid++;
                        }
                    } else {
                        if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
                            if (bs.is(GCBlocks.FUSION_MAGNET_COIL)) valid++;
                        } else {
                            if (bs.is(GCBlocks.FUSION_PLASMA_GLASS) || bs.is(GCBlocks.FUSION_REACTOR_CASING)) valid++;
                        }
                    }
                }
            }
        }

        boolean previouslyFormed = this.multiblockFormed;
        this.validStructureBlocks = valid;
        this.multiblockFormed = (valid == REQUIRED_MULTIBLOCK_BLOCKS);
        if (this.multiblockFormed != previouslyFormed && !level.isClientSide) {
            setStructureFormedState(level, core, controllerPos, this.multiblockFormed);
        }
        return valid;
    }

    public void unformMultiblock(Level level) {
        if (this.multiblockFormed) {
            this.multiblockFormed = false;
            this.validStructureBlocks = 0;
            this.burnTime = 0;
            this.activeGenerationRate = 0L;
            this.cryoCooled = false;
            BlockPos core = getCorePos(this.worldPosition, this.getBlockState());
            setStructureFormedState(level, core, this.worldPosition, false);
            if (level instanceof ServerLevel serverLevel) {
                this.syncRenderState(serverLevel, this.worldPosition, this.getBlockState());
            }
        }
    }

    private static void setStructureFormedState(Level level, BlockPos core, BlockPos controllerPos, boolean formed) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = core.offset(dx, dy, dz);
                    if (p.equals(controllerPos) || (dx == 0 && dy == 0 && dz == 0)) {
                        continue;
                    }
                    BlockState bs = level.getBlockState(p);
                    if (bs.getBlock() instanceof FusionReactorPartBlock && bs.getValue(FusionReactorPartBlock.FORMED) != formed) {
                        level.setBlock(p, bs.setValue(FusionReactorPartBlock.FORMED, formed), 3);
                    }
                }
            }
        }
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(IGNITION_CHARGE_SLOT);
        this.takeFluidFromSlot(HELIUM3_CANISTER_SLOT, HELIUM3_TANK, Gases.HELIUM_3);
        this.takeFluidFromSlot(DEUTERIUM_CANISTER_SLOT, DEUTERIUM_TANK, Gases.DEUTERIUM);
        this.drainPowerToSlot(OUTPUT_CELL_SLOT);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        profiler.push("multiblock");
        this.validateMultiblock(level, pos, state);
        profiler.pop();

        profiler.push("energy_spread");
        this.energySource.trySpreadEnergy(level, pos, state);
        profiler.pop();

        if (!this.multiblockFormed) {
            boolean wasActive = this.burnTime > 0 || this.plasmaTempMk > 0;
            this.burnTime = 0;
            this.activeGenerationRate = 0L;
            this.cryoCooled = false;
            this.coolPlasma();
            this.syncRenderState(level, pos, state);
            return wasActive ? GCMachineStatuses.MAGNETIC_QUENCH_SAFE : GCMachineStatuses.MULTIBLOCK_INCOMPLETE;
        }

        ItemResourceSlot coilSlot = this.itemStorage().slot(COIL_SLOT);
        if (coilSlot.isEmpty() || coilSlot.getResource() != GCItems.SUPERCONDUCTING_COIL) {
            // Magnetic quench-safe shutdown: no explosion, no radiation, no block damage
            if (this.burnTime > 0 || this.plasmaTempMk > 0) {
                this.burnTime = 0;
                this.activeGenerationRate = 0L;
                this.cryoCooled = false;
                this.coolPlasma();
                this.syncRenderState(level, pos, state);
                return GCMachineStatuses.MAGNETIC_QUENCH_SAFE;
            }
            this.activeGenerationRate = 0L;
            this.cryoCooled = false;
            this.syncRenderState(level, pos, state);
            return GCMachineStatuses.MISSING_COIL;
        }

        FusionReactorRecipe recipe = this.findFusionRecipe(level);
        long he3Required = recipe != null ? recipe.getPrimaryGasDroplets() : DEFAULT_CYCLE_GAS_MB * FluidUtil.MILLIBUCKET;
        long d2Required = recipe != null ? recipe.getSecondaryGasDroplets() : DEFAULT_CYCLE_GAS_MB * FluidUtil.MILLIBUCKET;
        long baseRate = recipe != null ? recipe.getEnergyPerTick() : BASE_GENERATION_RATE;
        int cycleTicks = recipe != null ? Math.max(1, recipe.getCycleTicks()) : DEFAULT_CYCLE_TICKS;

        FluidResourceSlot he3Tank = this.fluidStorage().slot(HELIUM3_TANK);
        FluidResourceSlot d2Tank = this.fluidStorage().slot(DEUTERIUM_TANK);
        boolean hasFuel = !he3Tank.isEmpty() && he3Tank.getResource() == Gases.HELIUM_3 && he3Tank.getAmount() >= he3Required
                && !d2Tank.isEmpty() && d2Tank.getResource() == Gases.DEUTERIUM && d2Tank.getAmount() >= d2Required;

        // Stage 1: Ramp plasma to 150 MK ignition threshold before sustained D-3He MHD burn
        if (this.burnTime <= 0 && this.plasmaTempMk < IGNITION_TEMP_MK) {
            if (!hasFuel) {
                this.activeGenerationRate = 0L;
                this.cryoCooled = false;
                this.coolPlasma();
                return GCMachineStatuses.MISSING_FUSION_FUEL;
            }
            this.plasmaTempMk = Math.min(IGNITION_TEMP_MK, this.plasmaTempMk + PLASMA_HEAT_RATE_MK);
            this.activeGenerationRate = 0L;
            this.spawnCoreParticles(level, pos, state, false);
            return GCMachineStatuses.IGNITING_PLASMA;
        }

        if (this.burnTime <= 0) {
            if (!hasFuel) {
                this.activeGenerationRate = 0L;
                this.cryoCooled = false;
                this.coolPlasma();
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
        this.plasmaTempMk = Math.min(MAX_PLASMA_TEMP_MK, this.plasmaTempMk + 3);

        this.energyStorage().insert(generationRate);
        this.energySource.trySpreadEnergy(level, pos, state);
        this.burnTime--;

        this.spawnCoreParticles(level, pos, state, this.cryoCooled);

        if (this.energyStorage().isFull()) {
            this.syncRenderState(level, pos, state);
            return MachineStatuses.CAPACITOR_FULL;
        }
        this.syncRenderState(level, pos, state);
        return this.cryoCooled ? GCMachineStatuses.FUSING_CRYO : GCMachineStatuses.FUSING;
    }

    private boolean lastSyncedFormed = false;
    private boolean lastSyncedFusing = false;
    private boolean lastSyncedCryo = false;

    private void syncRenderState(ServerLevel level, BlockPos pos, BlockState state) {
        boolean fusingNow = this.burnTime > 0 && this.multiblockFormed;
        if (this.lastSyncedFormed != this.multiblockFormed
                || this.lastSyncedFusing != fusingNow
                || this.lastSyncedCryo != this.cryoCooled) {
            this.lastSyncedFormed = this.multiblockFormed;
            this.lastSyncedFusing = fusingNow;
            this.lastSyncedCryo = this.cryoCooled;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private void spawnCoreParticles(ServerLevel level, BlockPos pos, BlockState state, boolean cryo) {
        if ((level.getGameTime() & 3L) != 0L) {
            return;
        }
        BlockPos core = getCorePos(pos, state);
        double cx = core.getX() + 0.5;
        double cy = core.getY() + 0.5;
        double cz = core.getZ() + 0.5;
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, cryo ? 5 : 3, 0.22, 0.22, 0.22, 0.01);
        if (cryo) {
            level.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 2, 0.18, 0.18, 0.18, 0.01);
        }
    }

    private void coolPlasma() {
        if (this.plasmaTempMk > 0) {
            this.plasmaTempMk = Math.max(0, this.plasmaTempMk - PLASMA_COOL_RATE_MK);
        }
    }

    @Override
    protected void tickDisabled(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        this.activeGenerationRate = 0L;
        this.cryoCooled = false;
        this.coolPlasma();
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

    public int getValidStructureBlocks() {
        return this.validStructureBlocks;
    }

    public boolean isMultiblockFormed() {
        return this.multiblockFormed;
    }

    public int getPlasmaTempMk() {
        return this.plasmaTempMk;
    }

    public void setPlasmaTempMk(int plasmaTempMk) {
        this.plasmaTempMk = plasmaTempMk;
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
        tag.putBoolean("multiblock_formed", this.multiblockFormed);
        tag.putBoolean("cryo_cooled", this.cryoCooled);
        tag.putInt("plasma_temp_mk", this.plasmaTempMk);
        tag.putInt("burn_time", this.burnTime);
        tag.putInt("burn_duration", this.burnDuration);
        tag.putInt("cell_synthesis_progress", this.cellSynthesisProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.multiblockFormed = tag.getBoolean("multiblock_formed");
        this.cryoCooled = tag.getBoolean("cryo_cooled");
        this.plasmaTempMk = tag.getInt("plasma_temp_mk");
        this.burnTime = tag.getInt("burn_time");
        this.burnDuration = tag.contains("burn_duration") ? Math.max(1, tag.getInt("burn_duration")) : DEFAULT_CYCLE_TICKS;
        this.cellSynthesisProgress = tag.getInt("cell_synthesis_progress");
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
        tag.putBoolean("multiblock_formed", this.multiblockFormed);
        tag.putBoolean("cryo_cooled", this.cryoCooled);
        tag.putInt("plasma_temp_mk", this.plasmaTempMk);
        tag.putInt("burn_time", this.burnTime);
        return tag;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new Helium3FusionReactorMenu(syncId, (ServerPlayer) player, this);
    }
}
