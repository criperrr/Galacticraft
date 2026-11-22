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

package dev.galacticraft.mod.content.block.entity;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.api.multiblock.MultiVoxelMaster;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.FootprintMode;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.multiblock.AbstractBoundsProxyBlock;
import dev.galacticraft.mod.content.block.multiblock.PlatformProxyBlock;
import dev.galacticraft.mod.content.block.platform.HangarTerminalBlock;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprint;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprintRegistry;
import dev.galacticraft.mod.content.block.platform.PlatformCollisionRegistry;
import dev.galacticraft.mod.content.block.platform.PlatformPadDetector;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.screen.HangarTerminalMenu;
import dev.galacticraft.mod.screen.HangarTerminalOpeningData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Master {@link BlockEntity} for the {@link HangarTerminalBlock}, managing platform configuration,
 * auto-detected pad alignment, hologram preview state, and multiblock assembly/disassembly with
 * lossless original-block restoration.
 */
public class HangarTerminalBlockEntity extends BlockEntity implements MultiVoxelMaster, ExtendedScreenHandlerFactory<HangarTerminalOpeningData> {
    public static final int ROCKET_SLOT_COUNT = 8;

    private static final String TAG_PLATFORM_TYPE = "PlatformType";
    private static final String TAG_PLATFORM_SIZE = "PlatformSize";
    private static final String TAG_PREVIEW_ENABLED = "PreviewEnabled";
    private static final String TAG_AXIS_GUIDE_ENABLED = "AxisGuideEnabled";
    private static final String TAG_PREVIEW_LAYER_Y = "PreviewLayerY";
    private static final String TAG_FORMED = "Formed";
    private static final String TAG_PAD_CENTER_OFFSET = "PadCenterOffset";
    private static final String TAG_DETECTED_PAD_SPAN = "DetectedPadSpan";
    private static final String TAG_INSTALLED_UPGRADE_MASK = "InstalledUpgradeMask";
    private static final String TAG_SAVED_BLOCKS = "SavedBlocks";
    private static final String TAG_ACTIVE_PROXIES = "ActiveProxies";
    private static final String TAG_ROCKET_SLOTS = "RocketSlots";
    private static final String TAG_ROCKET_ASSEMBLED = "RocketAssembled";
    private static final String TAG_ASSEMBLED_ROCKET_PARTS = "AssembledRocketParts";
    private static final String TAG_ROCKET_FUEL = "RocketFuel";
    private static final String TAG_ROCKET_ENERGY = "RocketEnergy";
    private static final String TAG_ROCKET_OXYGEN = "RocketOxygen";
    private static final String TAG_ENTRY_POS = "Pos";
    private static final String TAG_ENTRY_STATE = "State";

    private static final Set<HangarTerminalBlockEntity> CLIENT_TERMINALS = ConcurrentHashMap.newKeySet();
    private static final Set<HangarTerminalBlockEntity> CLIENT_TERMINALS_VIEW = Collections.unmodifiableSet(CLIENT_TERMINALS);

    private PlatformType platformType = PlatformType.VERTICAL;
    private PlatformSize platformSize = PlatformSize.SIZE_1X1;
    private boolean previewEnabled = true;
    private boolean axisGuideEnabled = true;
    private int previewLayerY = -1;
    private boolean formed = false;
    private BlockPos padCenterOffset;
    private int detectedPadSpan = 0;
    private int installedUpgradeMask = 0;
    private boolean tearingDown = false;
    private boolean rocketAssembled = false;
    private boolean tearingDownRocket = false;
    private final List<BlockPos> assembledRocketParts = new ArrayList<>();
    private long rocketFuelDroplets = 0L;
    private long rocketEnergy = 0L;
    private long rocketOxygenDroplets = 0L;
    private final RocketEnergyStorage rocketEnergyStorage = new RocketEnergyStorage();
    private final RocketFluidTankStorage rocketFuelStorage = new RocketFluidTankStorage(true);
    private final RocketFluidTankStorage rocketOxygenStorage = new RocketFluidTankStorage(false);
    private final Storage<FluidVariant> rocketCombinedFluidStorage = new CombinedStorage<>(List.of(
            this.rocketFuelStorage,
            this.rocketOxygenStorage
    ));
    private final Map<Long, BlockState> savedBlocks = new LinkedHashMap<>();
    private final List<BlockPos> activeProxies = new ArrayList<>();
    private final List<BlockPos> activeElevatorProxies = new ArrayList<>();
    private int lastElevatorBlockY = Integer.MIN_VALUE;
    private double lastElevatorWorldY = Double.NaN;
    private final SimpleContainer rocketSlots = new SimpleContainer(ROCKET_SLOT_COUNT) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof ModularRocketPartBlockItem;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            HangarTerminalBlockEntity.this.setChanged();
            if (HangarTerminalBlockEntity.this.level != null && !HangarTerminalBlockEntity.this.level.isClientSide) {
                HangarTerminalBlockEntity.this.sync();
            }
        }
    };
    private long clientConfigVersion = 0L;

    public HangarTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.HANGAR_TERMINAL, pos, state);
        Direction facing = state.hasProperty(HangarTerminalBlock.FACING)
                ? state.getValue(HangarTerminalBlock.FACING)
                : Direction.NORTH;
        this.padCenterOffset = computeDefaultPadCenterOffset(facing, this.platformType, this.platformSize);
    }

    public static Set<HangarTerminalBlockEntity> getActiveClientTerminals() {
        return CLIENT_TERMINALS_VIEW;
    }

    public static void clearClientTerminals() {
        CLIENT_TERMINALS.clear();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null && level.isClientSide) {
            CLIENT_TERMINALS.add(this);
        } else {
            CLIENT_TERMINALS.remove(this);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null && this.level.isClientSide) {
            CLIENT_TERMINALS.add(this);
        }
    }

    @Override
    public void setRemoved() {
        CLIENT_TERMINALS.remove(this);
        super.setRemoved();
    }

    public PlatformType getPlatformType() {
        return this.platformType;
    }

    public PlatformSize getPlatformSize() {
        return this.platformSize;
    }

    public boolean isPreviewEnabled() {
        return this.previewEnabled;
    }

    public boolean isAxisGuideEnabled() {
        return this.axisGuideEnabled;
    }

    public void toggleAxisGuide() {
        this.axisGuideEnabled = !this.axisGuideEnabled;
        this.sync();
    }

    public int getPreviewLayerY() {
        return this.previewLayerY;
    }

    public boolean isFormed() {
        return this.formed;
    }

    public boolean isRocketAssembled() {
        return this.formed && this.rocketAssembled && !this.assembledRocketParts.isEmpty();
    }

    public boolean isTearingDownRocket() {
        return this.tearingDownRocket;
    }

    public boolean isPartOfAssembledRocket(BlockPos pos) {
        return this.rocketAssembled && pos != null && this.assembledRocketParts.contains(pos);
    }

    public List<BlockPos> getAssembledRocketPartPositions() {
        return Collections.unmodifiableList(this.assembledRocketParts);
    }

    public long getRocketFuelDroplets() {
        return this.rocketFuelDroplets;
    }

    public long getRocketEnergy() {
        return this.rocketEnergy;
    }

    public long getRocketOxygenDroplets() {
        return this.rocketOxygenDroplets;
    }

    public @Nullable EnergyStorage getRocketEnergyStorage() {
        return this.isRocketAssembled() ? this.rocketEnergyStorage : null;
    }

    public @Nullable Storage<FluidVariant> getRocketFluidStorage() {
        return this.isRocketAssembled() ? this.rocketCombinedFluidStorage : null;
    }

    /**
     * Computes a single unified world-space bounding box enclosing all parts of the rocket
     * currently assembled on this launch platform, or {@code null} if no unified rocket exists.
     */
    public @Nullable AABB computeAssembledRocketWorldBounds() {
        if (this.level == null || !this.isRocketAssembled()) {
            return null;
        }
        AABB combined = null;
        for (BlockPos partPos : this.assembledRocketParts) {
            if (!this.level.isLoaded(partPos)) {
                continue;
            }
            if (this.level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
                AABB partBounds = FootprintMode.computeBoundsAABB(partBe.getConfig())
                        .move(partPos.getX(), partPos.getY(), partPos.getZ());
                combined = combined == null ? partBounds : combined.minmax(partBounds);
            }
        }
        return combined;
    }

    /**
     * Atomically tears down the entire unified rocket assembled on this platform when any part or
     * proxy voxel is broken or exploded. Drops every configured part item when {@code dropItems} is true.
     */
    public void tearDownAssembledRocket(Level level, @Nullable BlockPos brokenPos, @Nullable Player player, boolean dropItems) {
        if (level == null || level.isClientSide || this.tearingDownRocket) {
            return;
        }
        this.tearingDownRocket = true;
        try {
            List<BlockPos> targets = new ArrayList<>(this.assembledRocketParts);
            this.assembledRocketParts.clear();
            this.rocketAssembled = false;
            this.rocketFuelDroplets = 0L;
            this.rocketEnergy = 0L;
            this.rocketOxygenDroplets = 0L;

            for (BlockPos partPos : targets) {
                if (level.isLoaded(partPos)
                        && level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe
                        && !partBe.isRemoved()) {
                    partBe.clearAssembledTerminalPosQuiet();
                    partBe.tearDown(level, brokenPos != null ? brokenPos : partPos, player, dropItems);
                }
            }
            this.sync();
        } finally {
            this.tearingDownRocket = false;
        }
    }

    /**
     * Safely detaches all unified rocket part links without destroying the physical parts on the pad
     * (used when the platform itself is disassembled).
     */
    private void unbindAssembledRocket() {
        if (this.level != null && !this.level.isClientSide) {
            for (BlockPos partPos : this.assembledRocketParts) {
                if (this.level.isLoaded(partPos)
                        && this.level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe
                        && !partBe.isRemoved()) {
                    partBe.setAssembledTerminalPos(null);
                }
            }
        }
        this.assembledRocketParts.clear();
        this.rocketAssembled = false;
        this.rocketFuelDroplets = 0L;
        this.rocketEnergy = 0L;
        this.rocketOxygenDroplets = 0L;
    }

    /**
     * Creative-mode helper that fills fuel, electrical energy, and oxygen reserves on the unified
     * assembled rocket to 100% capacity, or drains them to 0 if already completely full.
     */
    public boolean creativeToggleFillRocket() {
        if (this.level == null || this.level.isClientSide || !this.isRocketAssembled()) {
            return false;
        }
        RocketPadSummary summary = this.scanRocketOnPad();
        long fuelCap = summary.totalFuelDroplets();
        long energyCap = summary.energyCapacity();
        long oxygenCap = summary.oxygenCapacityDroplets();
        if (fuelCap <= 0L && energyCap <= 0L && oxygenCap <= 0L) {
            return false;
        }
        boolean alreadyFull = (fuelCap <= 0L || this.rocketFuelDroplets >= fuelCap)
                && (energyCap <= 0L || this.rocketEnergy >= energyCap)
                && (oxygenCap <= 0L || this.rocketOxygenDroplets >= oxygenCap);
        if (alreadyFull) {
            this.rocketFuelDroplets = 0L;
            this.rocketEnergy = 0L;
            this.rocketOxygenDroplets = 0L;
        } else {
            this.rocketFuelDroplets = fuelCap;
            this.rocketEnergy = energyCap;
            this.rocketOxygenDroplets = oxygenCap;
        }
        this.level.playSound(
                null,
                this.getPadCenterPos().above(),
                alreadyFull ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL,
                SoundSource.BLOCKS,
                0.8F,
                1.1F
        );
        this.sync();
        return true;
    }

    public BlockPos getPadCenterOffset() {
        return this.padCenterOffset;
    }

    public BlockPos getPadCenterPos() {
        return this.worldPosition.offset(this.padCenterOffset);
    }

    public Direction getFacing() {
        BlockState state = this.getBlockState();
        if (state.hasProperty(HangarTerminalBlock.FACING)) {
            return state.getValue(HangarTerminalBlock.FACING);
        }
        return Direction.NORTH;
    }

    public int getDetectedPadSpan() {
        return this.detectedPadSpan;
    }

    public int getInstalledUpgradeMask() {
        return this.installedUpgradeMask;
    }

    public long getClientConfigVersion() {
        return this.clientConfigVersion;
    }

    public PlatformBlueprint getActiveBlueprint() {
        return PlatformBlueprintRegistry.get(this.platformType, this.platformSize);
    }

    public PlatformBlueprint.ValidationSummary validateStructure() {
        PlatformBlueprint blueprint = this.getActiveBlueprint();
        if (this.level == null) {
            return blueprint.emptySummary();
        }
        return blueprint.validate(this.level, this.getPadCenterPos(), this.getFacing(), this.worldPosition);
    }

    /**
     * Scans the clearance volume above this formed launch pad (or resolves the unified assembled
     * rocket part registry when built via the platform) and aggregates live resources, environmental
     * tolerances, structural integrity, flight telemetry, and triggerable part controls.
     */
    public RocketPadSummary scanRocketOnPad() {
        if (this.level == null || !this.formed) {
            return RocketPadSummary.EMPTY;
        }
        PlatformBlueprint blueprint = this.getActiveBlueprint();
        BlockPos padCenter = this.getPadCenterPos();
        Direction facing = this.getFacing();

        List<ModularRocketPartBlockEntity> foundEntities = new ArrayList<>();
        boolean unified = this.isRocketAssembled();

        if (unified) {
            for (BlockPos partPos : this.assembledRocketParts) {
                if (!this.level.isLoaded(partPos)) {
                    continue;
                }
                if (this.level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
                    foundEntities.add(partBe);
                }
            }
        } else {
            int minX = blueprint.clearanceMin().getX();
            int maxX = blueprint.clearanceMax().getX();
            int minZ = blueprint.clearanceMin().getZ();
            int maxZ = blueprint.clearanceMax().getZ();
            int maxScanY = Math.min(this.level.getMaxBuildHeight() - 1 - padCenter.getY(), Math.max(blueprint.maxY() + 24, 32));
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            for (int ly = 1; ly <= maxScanY; ly++) {
                for (int lx = minX; lx <= maxX; lx++) {
                    for (int lz = minZ; lz <= maxZ; lz++) {
                        BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenter, facing, new BlockPos(lx, ly, lz));
                        cursor.set(worldPos);
                        if (!this.level.isLoaded(cursor)) {
                            continue;
                        }
                        if (this.level.getBlockEntity(cursor) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
                            foundEntities.add(partBe);
                        }
                    }
                }
            }
        }

        if (foundEntities.isEmpty()) {
            return RocketPadSummary.EMPTY;
        }

        List<MountedRocketPart> parts = new ArrayList<>(foundEntities.size());
        double dryMassKg = 0.0;
        double totalThrustKn = 0.0;
        long totalFuelCapacityDroplets = 0L;
        long totalEnergyCapacity = 0L;
        long totalEnergyGenPerTick = 0L;
        long totalOxygenCapacityDroplets = 0L;
        int maxDiameter = 0;
        boolean hasCommand = false;
        boolean hasTank = false;
        boolean hasEngine = false;
        double minIntegrity = 1.0;
        double sumIntegrity = 0.0;
        double minThermalK = Double.MAX_VALUE;
        double minPressureAtm = Double.MAX_VALUE;

        RocketPartCatalog catalog = RocketPartCatalog.getInstance();

        for (ModularRocketPartBlockEntity partBe : foundEntities) {
            RocketPartConfig cfg = partBe.getConfig();
            RocketPartStats stats = partBe.getStats();
            if (stats == null) {
                stats = RocketPartCostCalculator.calculate(cfg).stats();
            }
            PartIntegrity integrity = partBe.getIntegrity();
            double integrityFrac = integrity != null ? integrity.fraction() : 1.0;
            minIntegrity = Math.min(minIntegrity, integrityFrac);
            sumIntegrity += integrityFrac;
            if (stats.thermalToleranceKelvin() > 0.0) {
                minThermalK = Math.min(minThermalK, stats.thermalToleranceKelvin());
            }
            if (stats.pressureToleranceAtm() > 0.0) {
                minPressureAtm = Math.min(minPressureAtm, stats.pressureToleranceAtm());
            }

            RocketPartSpec spec = catalog.getPart(cfg.modelId());
            String displayName = spec != null ? spec.displayName() : cfg.modelId();

            dryMassKg += stats.dryWeightKg();
            totalThrustKn += stats.thrustKn();
            totalFuelCapacityDroplets += stats.fuelCapacityDroplets();
            totalEnergyCapacity += RocketPartCostCalculator.calculateEnergyCapacity(cfg);
            totalEnergyGenPerTick += RocketPartCostCalculator.calculateEnergyGenerationPerTick(cfg);
            totalOxygenCapacityDroplets += RocketPartCostCalculator.calculateOxygenCapacityDroplets(cfg);
            maxDiameter = Math.max(maxDiameter, Math.max(cfg.diameter(), cfg.topDiameter()));

            if (cfg.category() == RocketPartCategory.COMMAND) {
                hasCommand = true;
            } else if (cfg.category() == RocketPartCategory.TANK) {
                hasTank = true;
            } else if (cfg.category() == RocketPartCategory.ENGINE) {
                hasEngine = true;
            }

            int relY = partBe.getBlockPos().getY() - padCenter.getY();
            parts.add(new MountedRocketPart(
                    partBe.getBlockPos().immutable(),
                    displayName,
                    cfg.category(),
                    cfg.diameter(),
                    cfg.heightBlocks(),
                    relY,
                    partBe.supportsAnimationTrigger(),
                    partBe.isAnimationActive(),
                    cfg.walkableInterior(),
                    partBe.isHatchOpen(),
                    cfg
            ));
        }

        parts.sort(Comparator.comparingInt(MountedRocketPart::relativeY).thenComparing(MountedRocketPart::displayName));

        long currentFuel = unified ? Math.min(this.rocketFuelDroplets, totalFuelCapacityDroplets) : 0L;
        long currentEnergy = unified ? Math.min(this.rocketEnergy, totalEnergyCapacity) : 0L;
        long currentOxygen = unified ? Math.min(this.rocketOxygenDroplets, totalOxygenCapacityDroplets) : 0L;

        // 1 bucket (81,000 droplets) of cryo propellant ~ 850 kg
        double maxFuelMassKg = (totalFuelCapacityDroplets / 81_000.0) * 850.0;
        double currentFuelMassKg = (currentFuel / 81_000.0) * 850.0;
        double fullWetMassKg = dryMassKg + maxFuelMassKg;
        double currentWetMassKg = dryMassKg + currentFuelMassKg;
        double fullTwr = fullWetMassKg > 0.0 ? (totalThrustKn * 1000.0) / (fullWetMassKg * 9.81) : 0.0;
        double currentTwr = currentWetMassKg > 0.0 ? (totalThrustKn * 1000.0) / (currentWetMassKg * 9.81) : 0.0;
        double maxDeltaV = (dryMassKg > 0.0 && fullWetMassKg > dryMassKg)
                ? 310.0 * 9.81 * Math.log(fullWetMassKg / dryMassKg)
                : 0.0;
        double currentDeltaV = (dryMassKg > 0.0 && currentWetMassKg > dryMassKg)
                ? 310.0 * 9.81 * Math.log(currentWetMassKg / dryMassKg)
                : 0.0;
        int allowedDiameter = this.platformSize.diameter();
        boolean diameterOk = maxDiameter == 0 || maxDiameter <= allowedDiameter;
        double avgIntegrity = sumIntegrity / parts.size();

        return new RocketPadSummary(
                List.copyOf(parts),
                unified,
                dryMassKg,
                fullWetMassKg,
                currentWetMassKg,
                totalThrustKn,
                currentFuel,
                totalFuelCapacityDroplets,
                currentEnergy,
                totalEnergyCapacity,
                totalEnergyGenPerTick,
                currentOxygen,
                totalOxygenCapacityDroplets,
                fullTwr,
                currentTwr,
                maxDeltaV,
                currentDeltaV,
                maxDiameter,
                allowedDiameter,
                hasCommand,
                hasTank,
                hasEngine,
                diameterOk,
                minIntegrity,
                avgIntegrity,
                minThermalK == Double.MAX_VALUE ? 0.0 : minThermalK,
                minPressureAtm == Double.MAX_VALUE ? 0.0 : minPressureAtm
        );
    }

    public SimpleContainer getRocketSlots() {
        return this.rocketSlots;
    }

    public boolean hasStagedRocketParts() {
        for (int i = 0; i < ROCKET_SLOT_COUNT; i++) {
            if (!this.rocketSlots.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void dropRocketSlots() {
        if (this.level != null && !this.level.isClientSide) {
            Containers.dropContents(this.level, this.worldPosition, this.rocketSlots);
        }
    }

    /**
     * Computes live telemetry from the modular rocket parts staged in {@link #rocketSlots}
     * (ordered bottom-to-top: slot 0 = base .. slot 7 = top) so the player can preview TWR,
     * delta-V, and flight readiness before clicking "Construir Foguete".
     */
    public RocketPadSummary previewRocketFromSlots() {
        List<MountedRocketPart> parts = new ArrayList<>();
        double dryMassKg = 0.0;
        double totalThrustKn = 0.0;
        long totalFuelDroplets = 0L;
        int maxDiameter = 0;
        boolean hasCommand = false;
        boolean hasTank = false;
        boolean hasEngine = false;

        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        BlockPos padCenter = this.getPadCenterPos();
        int currentRelY = 1;

        for (int i = 0; i < ROCKET_SLOT_COUNT; i++) {
            ItemStack stack = this.rocketSlots.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
                continue;
            }
            RocketPartConfig cfg = partItem.getConfig(stack);
            RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
            if (stats == null) {
                stats = RocketPartCostCalculator.calculate(cfg).stats();
            }
            RocketPartSpec spec = catalog.getPart(cfg.modelId());
            String displayName = spec != null ? spec.displayName() : cfg.modelId();

            dryMassKg += stats.dryWeightKg();
            totalThrustKn += stats.thrustKn();
            totalFuelDroplets += stats.fuelCapacityDroplets();
            maxDiameter = Math.max(maxDiameter, Math.max(cfg.diameter(), cfg.topDiameter()));

            if (cfg.category() == RocketPartCategory.COMMAND) {
                hasCommand = true;
            } else if (cfg.category() == RocketPartCategory.TANK) {
                hasTank = true;
            } else if (cfg.category() == RocketPartCategory.ENGINE) {
                hasEngine = true;
            }

            parts.add(new MountedRocketPart(
                    padCenter.above(currentRelY),
                    displayName,
                    cfg.category(),
                    cfg.diameter(),
                    cfg.heightBlocks(),
                    currentRelY,
                    false,
                    false,
                    cfg.walkableInterior(),
                    false,
                    cfg
            ));
            currentRelY += Math.max(1, cfg.heightBlocks());
        }

        if (parts.isEmpty()) {
            return RocketPadSummary.EMPTY;
        }

        double fuelMassKg = (totalFuelDroplets / 81_000.0) * 850.0;
        double wetMassKg = dryMassKg + fuelMassKg;
        double twr = wetMassKg > 0.0 ? (totalThrustKn * 1000.0) / (wetMassKg * 9.81) : 0.0;
        double deltaV = (dryMassKg > 0.0 && wetMassKg > dryMassKg)
                ? 310.0 * 9.81 * Math.log(wetMassKg / dryMassKg)
                : 0.0;
        int allowedDiameter = this.platformSize.diameter();
        boolean diameterOk = maxDiameter == 0 || maxDiameter <= allowedDiameter;

        long totalEnergyCap = 0L;
        long totalEnergyGen = 0L;
        long totalOxygenCap = 0L;
        double minThermal = Double.MAX_VALUE;
        double minPressure = Double.MAX_VALUE;
        for (MountedRocketPart part : parts) {
            if (part.config() != null) {
                totalEnergyCap += RocketPartCostCalculator.calculateEnergyCapacity(part.config());
                totalEnergyGen += RocketPartCostCalculator.calculateEnergyGenerationPerTick(part.config());
                totalOxygenCap += RocketPartCostCalculator.calculateOxygenCapacityDroplets(part.config());
                RocketPartStats st = RocketPartCostCalculator.calculate(part.config()).stats();
                if (st.thermalToleranceKelvin() > 0.0) {
                    minThermal = Math.min(minThermal, st.thermalToleranceKelvin());
                }
                if (st.pressureToleranceAtm() > 0.0) {
                    minPressure = Math.min(minPressure, st.pressureToleranceAtm());
                }
            }
        }

        return new RocketPadSummary(
                List.copyOf(parts),
                false,
                dryMassKg,
                wetMassKg,
                dryMassKg,
                totalThrustKn,
                0L,
                totalFuelDroplets,
                0L,
                totalEnergyCap,
                totalEnergyGen,
                0L,
                totalOxygenCap,
                twr,
                dryMassKg > 0.0 ? (totalThrustKn * 1000.0) / (dryMassKg * 9.81) : 0.0,
                deltaV,
                0.0,
                maxDiameter,
                allowedDiameter,
                hasCommand,
                hasTank,
                hasEngine,
                diameterOk,
                1.0,
                1.0,
                minThermal == Double.MAX_VALUE ? 0.0 : minThermal,
                minPressure == Double.MAX_VALUE ? 0.0 : minPressure
        );
    }

    /**
     * Server-side ticker that synchronizes walkable {@link dev.galacticraft.api.multiblock.BoundsShape#ELEVATOR_FLOOR}
     * collision proxies with the animated tower elevator cabin and smoothly carries standing entities.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, HangarTerminalBlockEntity terminal) {
        if (level.isClientSide || !terminal.formed || terminal.tearingDown) {
            return;
        }
        terminal.tickAssembledRocket(level);

        PlatformCollisionRegistry.ElevatorTrack track = PlatformCollisionRegistry.getElevatorTrack(
                terminal.platformType,
                terminal.platformSize
        );
        if (track == null) {
            return;
        }

        BlockPos padCenter = terminal.getPadCenterPos();
        Direction facing = terminal.getFacing();
        double localY = track.sampleY(level.getGameTime());
        double worldY = padCenter.getY() + localY;
        int blockY = net.minecraft.util.Mth.floor(localY);

        int centerLx = (int) Math.round(track.originX());
        int centerLz = (int) Math.round(track.originZ());

        if (blockY != terminal.lastElevatorBlockY) {
            // Clear previous elevator floor proxies
            if (!terminal.activeElevatorProxies.isEmpty()) {
                MultiblockLinkManager.unlinkAll(level, terminal.activeElevatorProxies);
                for (BlockPos oldPos : terminal.activeElevatorProxies) {
                    BlockState oldState = level.getBlockState(oldPos);
                    if (oldState.getBlock() instanceof PlatformProxyBlock
                            && oldState.getValue(AbstractBoundsProxyBlock.SHAPE) == dev.galacticraft.api.multiblock.BoundsShape.ELEVATOR_FLOOR) {
                        level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
                terminal.activeElevatorProxies.clear();
            }

            // Place 3x3 walkable elevator floor proxies at the cabin's current height
            BlockState floorState = GCBlocks.PLATFORM_PROXY.defaultBlockState()
                    .setValue(AbstractBoundsProxyBlock.SHAPE, dev.galacticraft.api.multiblock.BoundsShape.ELEVATOR_FLOOR);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos floorPos = PlatformBlueprint.toWorldPos(padCenter, facing, new BlockPos(centerLx + dx, blockY, centerLz + dz)).immutable();
                    if (floorPos.equals(terminal.worldPosition) || !level.isInWorldBounds(floorPos)) {
                        continue;
                    }
                    BlockState existing = level.getBlockState(floorPos);
                    if (existing.isAir() || existing.canBeReplaced()) {
                        level.setBlock(floorPos, floorState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        terminal.activeElevatorProxies.add(floorPos);
                    }
                }
            }
            if (!terminal.activeElevatorProxies.isEmpty()) {
                MultiblockLinkManager.linkAll(level, terminal.activeElevatorProxies, terminal.worldPosition);
            }
            terminal.lastElevatorBlockY = blockY;
        }

        // Smoothly carry players/entities riding inside the elevator cabin
        if (!Double.isNaN(terminal.lastElevatorWorldY)) {
            double deltaY = worldY - terminal.lastElevatorWorldY;
            if (Math.abs(deltaY) > 1.0E-4 && Math.abs(deltaY) < 1.5) {
                BlockPos cabCenter = PlatformBlueprint.toWorldPos(padCenter, facing, new BlockPos(centerLx, blockY, centerLz));
                net.minecraft.world.phys.AABB cabBox = new net.minecraft.world.phys.AABB(
                        cabCenter.getX() - 1.1,
                        Math.min(terminal.lastElevatorWorldY, worldY) - 0.5,
                        cabCenter.getZ() - 1.1,
                        cabCenter.getX() + 2.1,
                        Math.max(terminal.lastElevatorWorldY, worldY) + 2.2,
                        cabCenter.getZ() + 2.1
                );
                for (net.minecraft.world.entity.Entity entity : level.getEntities(null, cabBox)) {
                    if (entity.isSpectator() || entity.isNoGravity()) {
                        continue;
                    }
                    double targetFeetY = worldY + 0.125;
                    if (deltaY > 0.0 && entity.getY() < targetFeetY + 0.35) {
                        entity.setPos(entity.getX(), Math.max(entity.getY() + deltaY, targetFeetY), entity.getZ());
                        entity.fallDistance = 0.0F;
                        entity.setOnGround(true);
                        if (entity instanceof ServerPlayer sp) {
                            sp.connection.resetPosition();
                        }
                    } else if (deltaY < 0.0 && entity.getY() <= targetFeetY + 0.65) {
                        entity.fallDistance = 0.0F;
                    }
                }
            }
        }
        terminal.lastElevatorWorldY = worldY;
    }

    private void tickAssembledRocket(Level level) {
        if (!this.rocketAssembled || this.tearingDownRocket || (level.getGameTime() % 20L) != 0L) {
            return;
        }
        boolean modified = false;
        this.assembledRocketParts.removeIf(partPos -> {
            if (!level.isLoaded(partPos)) {
                return false;
            }
            return !(level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe) || partBe.isRemoved();
        });
        if (this.assembledRocketParts.isEmpty()) {
            this.rocketAssembled = false;
            this.rocketFuelDroplets = 0L;
            this.rocketEnergy = 0L;
            this.rocketOxygenDroplets = 0L;
            this.sync();
            return;
        }

        RocketPadSummary summary = this.scanRocketOnPad();
        long clampedFuel = Math.min(this.rocketFuelDroplets, summary.totalFuelDroplets());
        long clampedEnergy = Math.min(this.rocketEnergy, summary.energyCapacity());
        long clampedOxygen = Math.min(this.rocketOxygenDroplets, summary.oxygenCapacityDroplets());
        if (clampedFuel != this.rocketFuelDroplets || clampedEnergy != this.rocketEnergy || clampedOxygen != this.rocketOxygenDroplets) {
            this.rocketFuelDroplets = clampedFuel;
            this.rocketEnergy = clampedEnergy;
            this.rocketOxygenDroplets = clampedOxygen;
            modified = true;
        }

        if (summary.energyGenerationPerTick() > 0L && this.rocketEnergy < summary.energyCapacity()) {
            long generated = 0L;
            boolean sunny = level.isDay() && !level.isRaining();
            for (MountedRocketPart part : summary.parts()) {
                if (part.config() == null) {
                    continue;
                }
                long partGen = RocketPartCostCalculator.calculateEnergyGenerationPerTick(part.config());
                if (partGen <= 0L) {
                    continue;
                }
                boolean isSolar = part.config().familyId().contains("power_solar_array");
                if (!isSolar || (sunny && level.canSeeSky(part.pos().above(part.heightBlocks())))) {
                    generated += partGen * 20L;
                }
            }
            if (generated > 0L) {
                long nextEnergy = Math.min(summary.energyCapacity(), this.rocketEnergy + generated);
                if (nextEnergy != this.rocketEnergy) {
                    this.rocketEnergy = nextEnergy;
                    modified = true;
                }
            }
        }

        if (modified) {
            this.sync();
        }
    }

    private void registerAssembledRocketParts(List<BlockPos> newlyBuiltParts) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        // Unify any previously existing parts on the pad plus all newly built parts into one structure
        if (!this.rocketAssembled) {
            RocketPadSummary existing = this.scanRocketOnPad();
            for (MountedRocketPart mounted : existing.parts()) {
                BlockPos pos = mounted.pos().immutable();
                if (!this.assembledRocketParts.contains(pos)) {
                    this.assembledRocketParts.add(pos);
                }
                if (this.level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
                    partBe.setAssembledTerminalPos(this.worldPosition);
                }
            }
        }
        for (BlockPos pos : newlyBuiltParts) {
            BlockPos immutable = pos.immutable();
            if (!this.assembledRocketParts.contains(immutable)) {
                this.assembledRocketParts.add(immutable);
            }
            if (this.level.getBlockEntity(immutable) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
                partBe.setAssembledTerminalPos(this.worldPosition);
            }
        }
        this.rocketAssembled = !this.assembledRocketParts.isEmpty();
    }

    /**
     * Assembles an arbitrarily tall modular rocket coaxially above this formed launch pad
     * directly from the specified player inventory slots (ordered bottom-to-top) with no fixed slot cap.
     *
     * @return {@code true} if at least one rocket part was assembled onto the pad.
     */
    public boolean buildRocketFromPlayerInventory(ServerPlayer player, List<Integer> inventorySlots) {
        if (this.level == null || this.level.isClientSide || !this.formed || this.tearingDown
                || player == null || inventorySlots == null || inventorySlots.isEmpty()) {
            return false;
        }

        Inventory inv = player.getInventory();
        boolean creative = player.isCreative();

        // Validate every referenced slot contains a modular rocket part and has sufficient count in Survival
        Map<Integer, Integer> requiredPerSlot = new LinkedHashMap<>();
        for (int slotIdx : inventorySlots) {
            if (slotIdx < 0 || slotIdx >= inv.getContainerSize()) {
                return false;
            }
            ItemStack stack = inv.getItem(slotIdx);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem)) {
                return false;
            }
            requiredPerSlot.merge(slotIdx, 1, Integer::sum);
        }
        if (!creative) {
            for (Map.Entry<Integer, Integer> entry : requiredPerSlot.entrySet()) {
                if (inv.getItem(entry.getKey()).getCount() < entry.getValue()) {
                    return false;
                }
            }
        }

        BlockPos padCenter = this.getPadCenterPos();
        Direction facing = this.getFacing();
        int currentY = padCenter.getY() + 1;
        RocketPadSummary existing = this.scanRocketOnPad();
        for (MountedRocketPart mounted : existing.parts()) {
            int topY = mounted.pos().getY() + Math.max(1, mounted.heightBlocks());
            if (topY > currentY) {
                currentY = topY;
            }
        }

        List<BlockPos> builtPositions = new ArrayList<>();
        for (int slotIdx : inventorySlots) {
            ItemStack stack = inv.getItem(slotIdx);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
                continue;
            }
            RocketPartConfig cfg = partItem.getConfig(stack);
            BlockPos origin = new BlockPos(padCenter.getX(), currentY, padCenter.getZ());

            if (origin.getY() + Math.max(1, cfg.heightBlocks()) > this.level.getMaxBuildHeight()) {
                break;
            }
            Map<BlockPos, dev.galacticraft.api.multiblock.BoundsShape> localFootprint = FootprintMode.computeLocalFootprint(cfg, false);
            boolean clear = true;
            for (BlockPos localPos : localFootprint.keySet()) {
                BlockPos wp = FootprintMode.toWorldPos(origin, facing, localPos);
                BlockState st = this.level.getBlockState(wp);
                if (!st.isAir() && !st.canBeReplaced()) {
                    clear = false;
                    break;
                }
            }
            if (!clear) {
                break;
            }

            Block partBlock = ModularRocketPartBlock.getBlockForCategory(cfg.category());
            BlockState masterState = partBlock.defaultBlockState()
                    .setValue(ModularRocketPartBlock.FACING, facing)
                    .setValue(ModularRocketPartBlock.HATCH_OPEN, false);
            this.level.setBlock(origin, masterState, Block.UPDATE_ALL);

            if (this.level.getBlockEntity(origin) instanceof ModularRocketPartBlockEntity partBe) {
                RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
                PartIntegrity integrity = stack.get(GCDataComponents.PART_INTEGRITY);
                partBe.initializePart(cfg, stats, integrity, facing);
            }

            if (!creative) {
                stack.shrink(1);
            }
            builtPositions.add(origin);
            currentY += Math.max(1, cfg.heightBlocks());
        }

        if (!builtPositions.isEmpty()) {
            this.registerAssembledRocketParts(builtPositions);
            player.inventoryMenu.broadcastChanges();
            this.level.playSound(null, padCenter.above(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.85F, 1.15F);
            this.sync();
            return true;
        }
        return false;
    }

    /**
     * Automatically stacks all modular rocket parts staged in {@link #rocketSlots} (bottom-to-top,
     * slot 0 -> slot 7) coaxially above the formed launch pad center, eliminating manual hand placement.
     *
     * @return {@code true} if at least one rocket part was assembled onto the pad.
     */
    public boolean buildRocketFromSlots() {
        if (this.level == null || this.level.isClientSide || !this.formed || this.tearingDown) {
            return false;
        }

        // Determine starting Y: either right above the pad deck, or on top of any already-mounted rocket parts
        BlockPos padCenter = this.getPadCenterPos();
        Direction facing = this.getFacing();
        int currentY = padCenter.getY() + 1;
        RocketPadSummary existing = this.scanRocketOnPad();
        for (MountedRocketPart mounted : existing.parts()) {
            int topY = mounted.pos().getY() + Math.max(1, mounted.heightBlocks());
            if (topY > currentY) {
                currentY = topY;
            }
        }

        List<BlockPos> builtPositions = new ArrayList<>();
        for (int i = 0; i < ROCKET_SLOT_COUNT; i++) {
            ItemStack stack = this.rocketSlots.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
                continue;
            }
            RocketPartConfig cfg = partItem.getConfig(stack);
            BlockPos origin = new BlockPos(padCenter.getX(), currentY, padCenter.getZ());

            // Check build height and footprint clearance (allow air/replaceable)
            if (origin.getY() + Math.max(1, cfg.heightBlocks()) > this.level.getMaxBuildHeight()) {
                break;
            }
            Map<BlockPos, dev.galacticraft.api.multiblock.BoundsShape> localFootprint = FootprintMode.computeLocalFootprint(cfg, false);
            boolean clear = true;
            for (BlockPos localPos : localFootprint.keySet()) {
                BlockPos wp = FootprintMode.toWorldPos(origin, facing, localPos);
                BlockState st = this.level.getBlockState(wp);
                if (!st.isAir() && !st.canBeReplaced()) {
                    clear = false;
                    break;
                }
            }
            if (!clear) {
                break;
            }

            Block partBlock = ModularRocketPartBlock.getBlockForCategory(cfg.category());
            BlockState masterState = partBlock.defaultBlockState()
                    .setValue(ModularRocketPartBlock.FACING, facing)
                    .setValue(ModularRocketPartBlock.HATCH_OPEN, false);
            this.level.setBlock(origin, masterState, Block.UPDATE_ALL);

            if (this.level.getBlockEntity(origin) instanceof ModularRocketPartBlockEntity partBe) {
                RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
                PartIntegrity integrity = stack.get(GCDataComponents.PART_INTEGRITY);
                partBe.initializePart(cfg, stats, integrity, facing);
            }

            this.rocketSlots.setItem(i, ItemStack.EMPTY);
            builtPositions.add(origin);
            currentY += Math.max(1, cfg.heightBlocks());
        }

        if (!builtPositions.isEmpty()) {
            this.registerAssembledRocketParts(builtPositions);
            this.level.playSound(null, padCenter.above(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.85F, 1.15F);
            this.sync();
            return true;
        }
        return false;
    }

    /**
     * Tears down all mounted modular rocket parts currently standing on this formed launch pad
     * without dropping them in-world and returns them into {@link #rocketSlots} (bottom-to-top),
     * overflowing into the player's inventory (or dropping at the terminal if full).
     */
    public boolean recallRocketToSlots(@Nullable Player player) {
        if (this.level == null || this.level.isClientSide || !this.formed || this.tearingDown || this.tearingDownRocket) {
            return false;
        }
        List<MountedRocketPart> mounted = this.scanRocketOnPad().parts();
        if (mounted.isEmpty()) {
            if (this.rocketAssembled || !this.assembledRocketParts.isEmpty()) {
                this.assembledRocketParts.clear();
                this.rocketAssembled = false;
                this.rocketFuelDroplets = 0L;
                this.rocketEnergy = 0L;
                this.rocketOxygenDroplets = 0L;
                this.sync();
            }
            return false;
        }

        this.tearingDownRocket = true;
        int recalled = 0;
        try {
            for (MountedRocketPart part : mounted) {
                if (!(this.level.getBlockEntity(part.pos()) instanceof ModularRocketPartBlockEntity partBe) || partBe.isRemoved()) {
                    continue;
                }
                ItemStack itemStack = partBe.createConfiguredStack();
                partBe.clearAssembledTerminalPosQuiet();
                partBe.tearDown(this.level, null, player, false);

                if (player != null) {
                    if (!player.getInventory().add(itemStack)) {
                        player.drop(itemStack, false);
                    }
                } else {
                    boolean placedInSlot = false;
                    for (int i = 0; i < ROCKET_SLOT_COUNT; i++) {
                        if (this.rocketSlots.getItem(i).isEmpty()) {
                            this.rocketSlots.setItem(i, itemStack);
                            placedInSlot = true;
                            break;
                        }
                    }
                    if (!placedInSlot) {
                        Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5, itemStack);
                    }
                }
                recalled++;
            }

            this.assembledRocketParts.clear();
            this.rocketAssembled = false;
            this.rocketFuelDroplets = 0L;
            this.rocketEnergy = 0L;
            this.rocketOxygenDroplets = 0L;
        } finally {
            this.tearingDownRocket = false;
        }

        if (recalled > 0) {
            this.level.playSound(null, this.getPadCenterPos().above(), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.85F, 1.0F);
            this.sync();
            return true;
        }
        return false;
    }

    /**
     * Triggers or retracts mechanical animations for mounted rocket parts on this formed platform.
     * {@code index == -1} activates all triggerable parts; {@code index == -2} retracts all parts;
     * {@code index >= 0} toggles the specific mounted part at that index.
     */
    public boolean triggerRocketPartAnimation(int index) {
        if (this.level == null || this.level.isClientSide || !this.formed) {
            return false;
        }
        RocketPadSummary summary = this.scanRocketOnPad();
        List<MountedRocketPart> parts = summary.parts();
        if (parts.isEmpty()) {
            return false;
        }
        boolean changed = false;
        if (index == -1 || index == -2) {
            boolean targetState = index == -1;
            for (MountedRocketPart part : parts) {
                if (part.supportsAnimation()
                        && this.level.getBlockEntity(part.pos()) instanceof ModularRocketPartBlockEntity partBe) {
                    changed |= partBe.setAnimationActive(targetState);
                }
            }
            return changed;
        }
        if (index < 0 || index >= parts.size()) {
            return false;
        }
        MountedRocketPart target = parts.get(index);
        if (this.level.getBlockEntity(target.pos()) instanceof ModularRocketPartBlockEntity partBe) {
            return partBe.toggleAnimation();
        }
        return false;
    }

    /**
     * Toggles the walkable airlock hatch on a mounted rocket part on this formed platform.
     */
    public boolean toggleRocketPartHatch(int index) {
        if (this.level == null || this.level.isClientSide || !this.formed) {
            return false;
        }
        List<MountedRocketPart> parts = this.scanRocketOnPad().parts();
        if (index < 0 || index >= parts.size()) {
            return false;
        }
        MountedRocketPart target = parts.get(index);
        if (this.level.getBlockEntity(target.pos()) instanceof ModularRocketPartBlockEntity partBe) {
            return partBe.toggleHatch();
        }
        return false;
    }

    public record MountedRocketPart(
            BlockPos pos,
            String displayName,
            RocketPartCategory category,
            int diameter,
            int heightBlocks,
            int relativeY,
            boolean supportsAnimation,
            boolean animationActive,
            boolean walkableInterior,
            boolean hatchOpen,
            @Nullable RocketPartConfig config
    ) {
    }

    public record RocketPadSummary(
            List<MountedRocketPart> parts,
            boolean unifiedAssembly,
            double dryMassKg,
            double wetMassKg,
            double currentWetMassKg,
            double totalThrustKn,
            long currentFuelDroplets,
            long totalFuelDroplets,
            long currentEnergy,
            long energyCapacity,
            long energyGenerationPerTick,
            long currentOxygenDroplets,
            long oxygenCapacityDroplets,
            double twr,
            double currentTwr,
            double estimatedDeltaV,
            double currentDeltaV,
            int maxDiameter,
            int allowedDiameter,
            boolean hasCommand,
            boolean hasTank,
            boolean hasEngine,
            boolean diameterWithinLimit,
            double minIntegrityFraction,
            double avgIntegrityFraction,
            double minThermalToleranceKelvin,
            double minPressureToleranceAtm
    ) {
        public static final RocketPadSummary EMPTY = new RocketPadSummary(
                List.of(), false, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                0.0, 0.0, 0.0, 0.0, 0, 1, false, false, false, true, 1.0, 1.0, 0.0, 0.0
        );

        public boolean readyForLaunch() {
            return !this.parts.isEmpty()
                    && this.hasCommand
                    && this.hasTank
                    && this.hasEngine
                    && this.twr >= 1.0
                    && this.diameterWithinLimit;
        }
    }

    public void setPlatformType(PlatformType type) {
        if (this.formed || type == null) {
            return;
        }
        this.platformType = type;
        this.platformSize = PlatformSize.clampForType(this.platformType, this.platformSize);
        this.clampPreviewLayerY();
        if (this.detectedPadSpan == 0) {
            this.padCenterOffset = computeDefaultPadCenterOffset(this.getFacing(), this.platformType, this.platformSize);
        }
        this.sync();
    }

    public void setPlatformSize(PlatformSize size) {
        if (this.formed || size == null) {
            return;
        }
        this.platformSize = PlatformSize.clampForType(this.platformType, size);
        this.clampPreviewLayerY();
        if (this.detectedPadSpan == 0) {
            this.padCenterOffset = computeDefaultPadCenterOffset(this.getFacing(), this.platformType, this.platformSize);
        }
        this.sync();
    }

    public void setPreviewEnabled(boolean previewEnabled) {
        this.previewEnabled = previewEnabled;
        this.sync();
    }

    public void togglePreview() {
        this.previewEnabled = !this.previewEnabled;
        this.sync();
    }

    public void setPreviewLayerY(int layerY) {
        int maxY = this.getActiveBlueprint().maxY();
        if (layerY < -1) {
            this.previewLayerY = -1;
        } else {
            this.previewLayerY = Math.min(layerY, maxY);
        }
        this.sync();
    }

    private void clampPreviewLayerY() {
        int maxY = this.getActiveBlueprint().maxY();
        if (this.previewLayerY > maxY) {
            this.previewLayerY = maxY;
        }
    }

    /**
     * Scans near this terminal for an adjacent {@link dev.galacticraft.mod.content.block.platform.ReinforcedLaunchPadBlock}
     * deck and snaps pad center, platform type, and size to match it; falls back to a safe offset behind the terminal
     * when no pad is found.
     *
     * @return {@code true} if an adjacent launch pad deck was detected.
     */
    public boolean autoDetectPad() {
        if (this.level == null || this.formed) {
            return false;
        }
        Direction facing = this.getFacing();
        PlatformPadDetector.DetectionResult result = PlatformPadDetector.detectAdjacentPad(this.level, this.worldPosition, facing);
        if (result != null) {
            this.padCenterOffset = result.padCenter().subtract(this.worldPosition);
            this.platformType = result.suggestedType();
            this.platformSize = PlatformSize.clampForType(this.platformType, result.suggestedSize());
            this.detectedPadSpan = Math.max(result.widthX(), result.depthZ());
            this.clampPreviewLayerY();
            this.sync();
            return true;
        }
        this.detectedPadSpan = 0;
        this.padCenterOffset = computeDefaultPadCenterOffset(facing, this.platformType, this.platformSize);
        this.sync();
        return false;
    }

    /**
     * Validates and assembles the configured platform multiblock on the server, replacing all blueprint
     * structural blocks with shape-matched {@link dev.galacticraft.mod.content.block.multiblock.PlatformProxyBlock}s
     * linked via chunk attachments while persisting every original {@link BlockState} for lossless restoration.
     *
     * @return {@code true} if the platform assembled successfully.
     */
    public boolean assemblePlatform() {
        return this.assemblePlatform(false);
    }

    /**
     * Assembles the configured platform multiblock on the server. When {@code creativeBypass} is {@code true},
     * structural validation is skipped so creative-mode players can form the platform directly without placing
     * every skeleton block first; any missing blueprint positions are recorded as their canonical structural
     * blocks so disassembling restores a complete structure.
     *
     * @param creativeBypass Whether to bypass structural block validation (creative mode only).
     * @return {@code true} if the platform assembled successfully.
     */
    public boolean assemblePlatform(boolean creativeBypass) {
        if (this.level == null || this.level.isClientSide || this.formed || this.tearingDown) {
            return false;
        }

        PlatformBlueprint blueprint = this.getActiveBlueprint();
        BlockPos padCenter = this.getPadCenterPos();
        Direction facing = this.getFacing();
        if (!creativeBypass) {
            PlatformBlueprint.ValidationSummary summary = blueprint.validate(this.level, padCenter, facing, this.worldPosition);
            if (!summary.readyToAssemble()) {
                return false;
            }
        }

        // Pass 1: capture every original structural BlockState before modifying any block, then clear them
        Map<Long, BlockState> captured = new LinkedHashMap<>(blueprint.entries().size());
        Map<Long, BlockState> rollbackStates = new LinkedHashMap<>(blueprint.entries().size());
        for (PlatformBlueprint.Entry entry : blueprint.entries()) {
            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenter, facing, entry.localPos());
            if (worldPos.equals(this.worldPosition)) {
                continue;
            }
            BlockState existing = this.level.getBlockState(worldPos);
            rollbackStates.put(worldPos.asLong(), existing);
            BlockState stateToSave = (creativeBypass && !entry.role().matches(existing))
                    ? entry.role().getBlock().defaultBlockState()
                    : existing;
            captured.put(worldPos.asLong(), stateToSave);
        }

        this.savedBlocks.clear();
        this.savedBlocks.putAll(captured);
        this.activeProxies.clear();

        try {
            for (Long packedPos : captured.keySet()) {
                this.level.setBlock(BlockPos.of(packedPos), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }

            // Pass 2: place shape-matched invisible collision proxies at the exact voxelized 3D model coordinates
            List<PlatformCollisionRegistry.CollisionVoxel> collisionVoxels = PlatformCollisionRegistry.get(this.platformType, this.platformSize);
            BlockState proxyBase = GCBlocks.PLATFORM_PROXY.defaultBlockState();

            if (!collisionVoxels.isEmpty()) {
                for (PlatformCollisionRegistry.CollisionVoxel voxel : collisionVoxels) {
                    BlockPos proxyPos = PlatformBlueprint.toWorldPos(padCenter, facing, voxel.localPos()).immutable();
                    if (proxyPos.equals(this.worldPosition) || !this.level.isInWorldBounds(proxyPos)) {
                        continue;
                    }
                    BlockState existingState = this.level.getBlockState(proxyPos);
                    if (!existingState.isAir() && !existingState.canBeReplaced() && !(existingState.getBlock() instanceof PlatformProxyBlock)) {
                        continue;
                    }
                    BlockState proxyState = proxyBase.setValue(AbstractBoundsProxyBlock.SHAPE, voxel.shape());
                    this.level.setBlock(proxyPos, proxyState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    this.activeProxies.add(proxyPos);
                }
            } else {
                // Fallback to blueprint entries if collision voxel map is unavailable
                for (PlatformBlueprint.Entry entry : blueprint.entries()) {
                    BlockPos proxyPos = PlatformBlueprint.toWorldPos(padCenter, facing, entry.localPos()).immutable();
                    if (proxyPos.equals(this.worldPosition)) {
                        continue;
                    }
                    BlockState proxyState = proxyBase.setValue(AbstractBoundsProxyBlock.SHAPE, entry.formedShape());
                    this.level.setBlock(proxyPos, proxyState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    this.activeProxies.add(proxyPos);
                }
            }

            // Batch-link all proxy coordinates to this master in a single chunk-grouped update
            if (!this.activeProxies.isEmpty()) {
                MultiblockLinkManager.linkAll(this.level, this.activeProxies, this.worldPosition);
            }

            this.formed = true;
            BlockState currentState = this.getBlockState();
            if (currentState.hasProperty(HangarTerminalBlock.ACTIVE) && !currentState.getValue(HangarTerminalBlock.ACTIVE)) {
                this.level.setBlock(this.worldPosition, currentState.setValue(HangarTerminalBlock.ACTIVE, true), Block.UPDATE_ALL);
            }
            this.sync();
            return true;
        } catch (RuntimeException e) {
            // Roll back cleanly so an unexpected failure never destroys the player's structural blocks
            dev.galacticraft.mod.Constant.LOGGER.error("Failed to assemble platform {} {} at {}; rolling back structural blocks",
                    this.platformType, this.platformSize, this.worldPosition, e);
            if (!this.activeProxies.isEmpty()) {
                try {
                    MultiblockLinkManager.unlinkAll(this.level, this.activeProxies);
                } catch (RuntimeException ignored) {
                }
                for (BlockPos proxyPos : this.activeProxies) {
                    if (this.level.getBlockState(proxyPos).getBlock() instanceof PlatformProxyBlock) {
                        this.level.setBlock(proxyPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
            for (Map.Entry<Long, BlockState> entry : rollbackStates.entrySet()) {
                this.level.setBlock(BlockPos.of(entry.getKey()), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            this.activeProxies.clear();
            this.savedBlocks.clear();
            this.formed = false;
            this.sync();
            return false;
        }
    }

    /**
     * Disassembles a formed platform multiblock on the server, unlinking all proxies and restoring every
     * original structural block except an optional broken proxy position.
     *
     * @param brokenProxyPos  Optional proxy position that was broken to trigger disassembly.
     * @param dropBrokenBlock Whether to drop the original structural block's loot at {@code brokenProxyPos}.
     */
    public void disassemblePlatform(@Nullable BlockPos brokenProxyPos, boolean dropBrokenBlock) {
        if (this.level == null || this.level.isClientSide || !this.formed || this.tearingDown) {
            return;
        }

        this.tearingDown = true;
        try {
            this.unbindAssembledRocket();

            List<BlockPos> toClear = !this.activeProxies.isEmpty()
                    ? new ArrayList<>(this.activeProxies)
                    : new ArrayList<>(this.savedBlocks.size());
            if (toClear.isEmpty()) {
                for (Long packedPos : this.savedBlocks.keySet()) {
                    toClear.add(BlockPos.of(packedPos));
                }
            }

            if (!this.activeElevatorProxies.isEmpty()) {
                toClear.addAll(this.activeElevatorProxies);
            }
            MultiblockLinkManager.unlinkAll(this.level, toClear);

            for (BlockPos proxyPos : toClear) {
                BlockState current = this.level.getBlockState(proxyPos);
                if (current.getBlock() instanceof PlatformProxyBlock) {
                    this.level.setBlock(proxyPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }

            for (Map.Entry<Long, BlockState> entry : this.savedBlocks.entrySet()) {
                BlockPos pos = BlockPos.of(entry.getKey());
                BlockState originalState = entry.getValue();
                if (pos.equals(brokenProxyPos)) {
                    this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    if (dropBrokenBlock && !originalState.isAir()) {
                        Block.dropResources(originalState, this.level, pos);
                    }
                } else {
                    this.level.setBlock(pos, originalState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }

            this.activeProxies.clear();
            this.activeElevatorProxies.clear();
            this.lastElevatorBlockY = Integer.MIN_VALUE;
            this.lastElevatorWorldY = Double.NaN;
            this.savedBlocks.clear();
            this.formed = false;

            BlockState currentTerminalState = this.level.getBlockState(this.worldPosition);
            if (currentTerminalState.getBlock() instanceof HangarTerminalBlock
                    && currentTerminalState.hasProperty(HangarTerminalBlock.ACTIVE)
                    && currentTerminalState.getValue(HangarTerminalBlock.ACTIVE)) {
                this.level.setBlock(this.worldPosition, currentTerminalState.setValue(HangarTerminalBlock.ACTIVE, false), Block.UPDATE_ALL);
            }

            this.sync();
        } finally {
            this.tearingDown = false;
        }
    }

    private static BlockPos computeDefaultPadCenterOffset(Direction facing, PlatformType type, PlatformSize size) {
        PlatformBlueprint blueprint = PlatformBlueprintRegistry.get(type, size);
        int halfExtent = Math.max(Math.abs(blueprint.minZ()), Math.abs(blueprint.maxZ()));
        Direction behind = facing != null && facing.getAxis().isHorizontal() ? facing.getOpposite() : Direction.SOUTH;
        int distance = halfExtent + 2;
        return new BlockPos(behind.getStepX() * distance, 0, behind.getStepZ() * distance);
    }

    public void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    // --- MultiVoxelMaster ---

    @Override
    public BlockPos getMasterPos() {
        return this.worldPosition;
    }

    @Override
    public void onProxyBroken(Level level, BlockPos proxyPos, @Nullable Player player, boolean dropItems) {
        if (!level.isClientSide && this.formed && !this.tearingDown) {
            this.disassemblePlatform(proxyPos, dropItems);
        }
    }

    @Override
    public InteractionResult onProxyUse(Level level, BlockPos proxyPos, Player player, BlockHitResult hit) {
        // Only the HangarTerminalBlock console opens the platform menu; structural proxy blocks
        // pass interaction through so future platform machines and normal interactions work unimpeded.
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable Storage<FluidVariant> getProxyFluidStorage(BlockPos proxyPos, @Nullable Direction side) {
        return this.getRocketFluidStorage();
    }

    @Override
    public @Nullable EnergyStorage getProxyEnergyStorage(BlockPos proxyPos, @Nullable Direction side) {
        return this.getRocketEnergyStorage();
    }

    @Override
    public ItemStack getProxyCloneItemStack(LevelReader level, BlockPos proxyPos) {
        BlockState saved = this.savedBlocks.get(proxyPos.asLong());
        if (saved != null && !saved.isAir()) {
            return new ItemStack(saved.getBlock());
        }
        return new ItemStack(GCBlocks.HANGAR_TERMINAL);
    }

    @Override
    public boolean isPartOfStructure(BlockPos pos) {
        return this.formed && (pos.equals(this.worldPosition)
                || this.activeProxies.contains(pos)
                || this.savedBlocks.containsKey(pos.asLong()));
    }

    // --- ExtendedScreenHandlerFactory ---

    @Override
    public HangarTerminalOpeningData getScreenOpeningData(ServerPlayer player) {
        return new HangarTerminalOpeningData(this.worldPosition);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(GCBlocks.HANGAR_TERMINAL.getDescriptionId());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new HangarTerminalMenu(syncId, playerInventory, this);
    }

    // --- Persistence & Sync ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_PLATFORM_TYPE, this.platformType.getSerializedName());
        tag.putString(TAG_PLATFORM_SIZE, this.platformSize.getSerializedName());
        tag.putBoolean(TAG_PREVIEW_ENABLED, this.previewEnabled);
        tag.putBoolean(TAG_AXIS_GUIDE_ENABLED, this.axisGuideEnabled);
        tag.putInt(TAG_PREVIEW_LAYER_Y, this.previewLayerY);
        tag.putBoolean(TAG_FORMED, this.formed);
        tag.putLong(TAG_PAD_CENTER_OFFSET, this.padCenterOffset.asLong());
        tag.putInt(TAG_DETECTED_PAD_SPAN, this.detectedPadSpan);
        tag.putInt(TAG_INSTALLED_UPGRADE_MASK, this.installedUpgradeMask);

        if (!this.savedBlocks.isEmpty()) {
            ListTag savedList = new ListTag();
            for (Map.Entry<Long, BlockState> entry : this.savedBlocks.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putLong(TAG_ENTRY_POS, entry.getKey());
                entryTag.put(TAG_ENTRY_STATE, NbtUtils.writeBlockState(entry.getValue()));
                savedList.add(entryTag);
            }
            tag.put(TAG_SAVED_BLOCKS, savedList);
        }

        if (!this.activeProxies.isEmpty()) {
            long[] packed = new long[this.activeProxies.size()];
            for (int i = 0; i < this.activeProxies.size(); i++) {
                packed[i] = this.activeProxies.get(i).asLong();
            }
            tag.putLongArray(TAG_ACTIVE_PROXIES, packed);
        }

        NonNullList<ItemStack> slotItems = NonNullList.withSize(ROCKET_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < ROCKET_SLOT_COUNT; i++) {
            slotItems.set(i, this.rocketSlots.getItem(i));
        }
        CompoundTag slotsTag = new CompoundTag();
        ContainerHelper.saveAllItems(slotsTag, slotItems, registries);
        tag.put(TAG_ROCKET_SLOTS, slotsTag);

        tag.putBoolean(TAG_ROCKET_ASSEMBLED, this.rocketAssembled);
        if (!this.assembledRocketParts.isEmpty()) {
            long[] packedParts = new long[this.assembledRocketParts.size()];
            for (int i = 0; i < this.assembledRocketParts.size(); i++) {
                packedParts[i] = this.assembledRocketParts.get(i).asLong();
            }
            tag.putLongArray(TAG_ASSEMBLED_ROCKET_PARTS, packedParts);
        }
        tag.putLong(TAG_ROCKET_FUEL, this.rocketFuelDroplets);
        tag.putLong(TAG_ROCKET_ENERGY, this.rocketEnergy);
        tag.putLong(TAG_ROCKET_OXYGEN, this.rocketOxygenDroplets);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.platformType = PlatformType.byName(tag.getString(TAG_PLATFORM_TYPE), PlatformType.VERTICAL);
        this.platformSize = PlatformSize.clampForType(
                this.platformType,
                PlatformSize.byName(tag.getString(TAG_PLATFORM_SIZE), PlatformSize.SIZE_1X1)
        );
        this.previewEnabled = !tag.contains(TAG_PREVIEW_ENABLED) || tag.getBoolean(TAG_PREVIEW_ENABLED);
        this.axisGuideEnabled = !tag.contains(TAG_AXIS_GUIDE_ENABLED) || tag.getBoolean(TAG_AXIS_GUIDE_ENABLED);
        this.previewLayerY = tag.contains(TAG_PREVIEW_LAYER_Y) ? tag.getInt(TAG_PREVIEW_LAYER_Y) : -1;
        this.clampPreviewLayerY();
        this.formed = tag.getBoolean(TAG_FORMED);
        if (tag.contains(TAG_PAD_CENTER_OFFSET, Tag.TAG_LONG)) {
            this.padCenterOffset = BlockPos.of(tag.getLong(TAG_PAD_CENTER_OFFSET));
        } else {
            this.padCenterOffset = computeDefaultPadCenterOffset(this.getFacing(), this.platformType, this.platformSize);
        }
        this.detectedPadSpan = tag.getInt(TAG_DETECTED_PAD_SPAN);
        this.installedUpgradeMask = tag.getInt(TAG_INSTALLED_UPGRADE_MASK);

        this.savedBlocks.clear();
        if (tag.contains(TAG_SAVED_BLOCKS, Tag.TAG_LIST)) {
            HolderGetter<Block> blockLookup = registries.lookup(Registries.BLOCK)
                    .<HolderGetter<Block>>map(lookup -> lookup)
                    .orElseGet(BuiltInRegistries.BLOCK::asLookup);
            ListTag savedList = tag.getList(TAG_SAVED_BLOCKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < savedList.size(); i++) {
                CompoundTag entryTag = savedList.getCompound(i);
                if (entryTag.contains(TAG_ENTRY_POS, Tag.TAG_LONG) && entryTag.contains(TAG_ENTRY_STATE, Tag.TAG_COMPOUND)) {
                    long packedPos = entryTag.getLong(TAG_ENTRY_POS);
                    BlockState state = NbtUtils.readBlockState(blockLookup, entryTag.getCompound(TAG_ENTRY_STATE));
                    if (!state.isAir()) {
                        this.savedBlocks.put(packedPos, state);
                    }
                }
            }
        }

        this.activeProxies.clear();
        if (tag.contains(TAG_ACTIVE_PROXIES, Tag.TAG_LONG_ARRAY)) {
            for (long packedPos : tag.getLongArray(TAG_ACTIVE_PROXIES)) {
                this.activeProxies.add(BlockPos.of(packedPos));
            }
        }

        this.rocketSlots.clearContent();
        if (tag.contains(TAG_ROCKET_SLOTS, Tag.TAG_COMPOUND)) {
            NonNullList<ItemStack> slotItems = NonNullList.withSize(ROCKET_SLOT_COUNT, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound(TAG_ROCKET_SLOTS), slotItems, registries);
            for (int i = 0; i < ROCKET_SLOT_COUNT; i++) {
                this.rocketSlots.setItem(i, slotItems.get(i));
            }
        }

        this.rocketAssembled = tag.getBoolean(TAG_ROCKET_ASSEMBLED);
        this.assembledRocketParts.clear();
        if (tag.contains(TAG_ASSEMBLED_ROCKET_PARTS, Tag.TAG_LONG_ARRAY)) {
            for (long packedPos : tag.getLongArray(TAG_ASSEMBLED_ROCKET_PARTS)) {
                this.assembledRocketParts.add(BlockPos.of(packedPos));
            }
        }
        this.rocketFuelDroplets = Math.max(0L, tag.getLong(TAG_ROCKET_FUEL));
        this.rocketEnergy = Math.max(0L, tag.getLong(TAG_ROCKET_ENERGY));
        this.rocketOxygenDroplets = Math.max(0L, tag.getLong(TAG_ROCKET_OXYGEN));

        if (this.level != null && this.level.isClientSide) {
            this.clientConfigVersion++;
            CLIENT_TERMINALS.add(this);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private long getAssembledFuelCapacityDroplets() {
        if (!this.isRocketAssembled() || this.level == null) {
            return 0L;
        }
        long total = 0L;
        for (BlockPos partPos : this.assembledRocketParts) {
            if (this.level.isLoaded(partPos)
                    && this.level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe
                    && !partBe.isRemoved()) {
                RocketPartStats stats = partBe.getStats();
                if (stats == null) {
                    stats = RocketPartCostCalculator.calculate(partBe.getConfig()).stats();
                }
                total += stats.fuelCapacityDroplets();
            }
        }
        return total;
    }

    private long getAssembledEnergyCapacity() {
        if (!this.isRocketAssembled() || this.level == null) {
            return 0L;
        }
        long total = 0L;
        for (BlockPos partPos : this.assembledRocketParts) {
            if (this.level.isLoaded(partPos)
                    && this.level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe
                    && !partBe.isRemoved()) {
                total += RocketPartCostCalculator.calculateEnergyCapacity(partBe.getConfig());
            }
        }
        return total;
    }

    private long getAssembledOxygenCapacityDroplets() {
        if (!this.isRocketAssembled() || this.level == null) {
            return 0L;
        }
        long total = 0L;
        for (BlockPos partPos : this.assembledRocketParts) {
            if (this.level.isLoaded(partPos)
                    && this.level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe
                    && !partBe.isRemoved()) {
                total += RocketPartCostCalculator.calculateOxygenCapacityDroplets(partBe.getConfig());
            }
        }
        return total;
    }

    private final class RocketEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
        @Override
        public boolean supportsInsertion() {
            return HangarTerminalBlockEntity.this.isRocketAssembled() && this.getCapacity() > 0L;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            if (!HangarTerminalBlockEntity.this.isRocketAssembled()) {
                return 0L;
            }
            long capacity = this.getCapacity();
            long space = Math.max(0L, capacity - HangarTerminalBlockEntity.this.rocketEnergy);
            long inserted = Math.min(space, maxAmount);
            if (inserted > 0L) {
                this.updateSnapshots(transaction);
                HangarTerminalBlockEntity.this.rocketEnergy += inserted;
            }
            return inserted;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            return 0L;
        }

        @Override
        public long getAmount() {
            return HangarTerminalBlockEntity.this.rocketEnergy;
        }

        @Override
        public long getCapacity() {
            return HangarTerminalBlockEntity.this.getAssembledEnergyCapacity();
        }

        @Override
        protected Long createSnapshot() {
            return HangarTerminalBlockEntity.this.rocketEnergy;
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            HangarTerminalBlockEntity.this.rocketEnergy = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            HangarTerminalBlockEntity.this.sync();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private final class RocketFluidTankStorage extends SnapshotParticipant<Long> implements SingleSlotStorage<FluidVariant> {
        private final boolean fuelTank;

        private RocketFluidTankStorage(boolean fuelTank) {
            this.fuelTank = fuelTank;
        }

        private boolean accepts(FluidVariant variant) {
            if (variant.isBlank()) {
                return false;
            }
            if (this.fuelTank) {
                return variant.isOf(GCFluids.FUEL);
            }
            return variant.isOf(GCFluids.LIQUID_OXYGEN) || variant.isOf(Gases.OXYGEN);
        }

        private FluidVariant canonicalVariant() {
            return FluidVariant.of(this.fuelTank ? GCFluids.FUEL : GCFluids.LIQUID_OXYGEN);
        }

        private long currentStored() {
            return this.fuelTank
                    ? HangarTerminalBlockEntity.this.rocketFuelDroplets
                    : HangarTerminalBlockEntity.this.rocketOxygenDroplets;
        }

        private void setStored(long amount) {
            if (this.fuelTank) {
                HangarTerminalBlockEntity.this.rocketFuelDroplets = amount;
            } else {
                HangarTerminalBlockEntity.this.rocketOxygenDroplets = amount;
            }
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!HangarTerminalBlockEntity.this.isRocketAssembled() || !this.accepts(resource)) {
                return 0L;
            }
            long capacity = this.getCapacity();
            long current = this.currentStored();
            long space = Math.max(0L, capacity - current);
            long inserted = Math.min(space, maxAmount);
            if (inserted > 0L) {
                this.updateSnapshots(transaction);
                this.setStored(current + inserted);
            }
            return inserted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!HangarTerminalBlockEntity.this.isRocketAssembled() || !this.accepts(resource)) {
                return 0L;
            }
            long current = this.currentStored();
            long extracted = Math.min(current, maxAmount);
            if (extracted > 0L) {
                this.updateSnapshots(transaction);
                this.setStored(current - extracted);
            }
            return extracted;
        }

        @Override
        public boolean isResourceBlank() {
            return this.currentStored() <= 0L;
        }

        @Override
        public FluidVariant getResource() {
            return this.currentStored() > 0L ? this.canonicalVariant() : FluidVariant.blank();
        }

        @Override
        public long getAmount() {
            return this.currentStored();
        }

        @Override
        public long getCapacity() {
            return this.fuelTank
                    ? HangarTerminalBlockEntity.this.getAssembledFuelCapacityDroplets()
                    : HangarTerminalBlockEntity.this.getAssembledOxygenCapacityDroplets();
        }

        @Override
        protected Long createSnapshot() {
            return this.currentStored();
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            this.setStored(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            HangarTerminalBlockEntity.this.sync();
        }
    }
}
