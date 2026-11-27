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
import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import dev.galacticraft.api.rocket.assembly.RadialBoosterGroup;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.FootprintMode;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.GCSounds;
import dev.galacticraft.mod.content.block.multiblock.AbstractBoundsProxyBlock;
import dev.galacticraft.mod.content.block.multiblock.PlatformProxyBlock;
import dev.galacticraft.mod.content.block.platform.HangarTerminalBlock;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprint;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprintRegistry;
import dev.galacticraft.mod.content.block.platform.PlatformCollisionRegistry;
import dev.galacticraft.mod.content.block.platform.PlatformPadDetector;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.assembly.RocketAssemblyGraphBuilder;
import dev.galacticraft.mod.content.rocket.failure.LaunchFailureEngine;
import dev.galacticraft.mod.content.rocket.launch.RocketStateConverter;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.physics.ModularFlightPhysics;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.content.rocket.physics.PropellantType;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator;
import dev.galacticraft.mod.particle.EntityParticleOption;
import dev.galacticraft.mod.particle.GCParticleTypes;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.GameType;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private static final String TAG_ROCKET_FUEL_FLUID = "RocketFuelFluid";
    private static final String TAG_ROCKET_ENERGY = "RocketEnergy";
    private static final String TAG_ROCKET_OXYGEN = "RocketOxygen";
    private static final String TAG_ENTRY_POS = "Pos";
    private static final String TAG_ENTRY_STATE = "State";
    private static final String TAG_ASSEMBLY_GRAPH = "AssemblyGraph";
    private static final String TAG_SELECTED_SYMMETRY = "SelectedSymmetry";
    private static final String TAG_SELECTED_CROSSFEED = "SelectedCrossfeed";
    private static final String TAG_STAGE_FUELS = "StageFuels";
    private static final String TAG_BOOSTER_FUELS = "BoosterFuels";
    private static final String TAG_PENDING_ASSEMBLY = "PendingAssembly";
    private static final String TAG_ASSEMBLY_STEP_ELAPSED = "AssemblyStepElapsed";
    private static final String TAG_ASSEMBLY_TOTAL_ELAPSED = "AssemblyTotalElapsed";
    private static final String TAG_ASSEMBLY_TOTAL_DURATION = "AssemblyTotalDuration";
    private static final String TAG_ASSEMBLY_COMPLETED_STEPS = "AssemblyCompletedSteps";
    private static final String TAG_ASSEMBLY_TOTAL_STEPS = "AssemblyTotalSteps";
    private static final String TAG_LAUNCH_PHASE = "LaunchPhase";
    private static final String TAG_LAUNCH_COUNTDOWN_TICKS = "LaunchCountdownTicks";
    private static final String TAG_LAST_ABORT_REASON = "LastAbortReason";

    public static final int LAUNCH_COUNTDOWN_TOTAL_TICKS = 200; // T-10.00s
    public static final int THRUST_BUILDUP_START_TICKS = 60;    // T-3.00s
    public static final int HOLD_DOWN_CLAMP_CHECK_TICK = 10;    // T-0.50s

    public enum LaunchSequencePhase {
        IDLE,
        PRE_FLIGHT_DELUGE,
        THRUST_BUILDUP
    }

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
    private RocketAssemblyGraph assemblyGraph = RocketAssemblyGraph.EMPTY;
    private RadialSymmetry selectedSymmetry = RadialSymmetry.TWO;
    private CrossfeedMode selectedCrossfeed = CrossfeedMode.ISOLATED;
    private final Map<Integer, Long> stageFuelDroplets = new LinkedHashMap<>();
    private final Map<Integer, FluidVariant> stageFuelVariants = new LinkedHashMap<>();
    private final Map<String, Long> boosterFuelDroplets = new LinkedHashMap<>();
    private final Map<String, FluidVariant> boosterFuelVariants = new LinkedHashMap<>();
    private final List<PendingAssemblyStep> pendingAssemblySteps = new ArrayList<>();
    private int assemblyStepElapsedTicks = 0;
    private int assemblyTotalElapsedTicks = 0;
    private int assemblyTotalDurationTicks = 0;
    private int assemblyCompletedSteps = 0;
    private int assemblyTotalSteps = 0;
    private LaunchSequencePhase launchPhase = LaunchSequencePhase.IDLE;
    private int launchCountdownTicks = 0;
    private String lastLaunchAbortReason = "";
    private final List<java.util.UUID> launchCrewUuids = new ArrayList<>();
    private boolean launchClampCheckPassed = false;
    private @Nullable ModularRocketFlightEntity lastLaunchedEntity = null;
    private long rocketFuelDroplets = 0L;
    private FluidVariant rocketFuelVariant = FluidVariant.of(GCFluids.FUEL);
    private double pendingBoilOffFraction = 0.0;
    private long rocketEnergy = 0L;
    private long rocketOxygenDroplets = 0L;
    private final RocketEnergyStorage rocketEnergyStorage = new RocketEnergyStorage();
    private final RocketFluidTankStorage rocketFuelStorage = new RocketFluidTankStorage(true);
    private final RocketFluidTankStorage rocketOxygenStorage = new RocketFluidTankStorage(false);
    private Storage<FluidVariant> rocketCombinedFluidStorage = new CombinedStorage<>(List.of(
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

    public RocketAssemblyGraph getAssemblyGraph() {
        return this.assemblyGraph;
    }

    public RadialSymmetry getSelectedSymmetry() {
        return this.selectedSymmetry;
    }

    public void setSelectedSymmetry(RadialSymmetry symmetry) {
        if (symmetry != null && symmetry != this.selectedSymmetry) {
            this.selectedSymmetry = symmetry;
            this.sync();
        }
    }

    public CrossfeedMode getSelectedCrossfeed() {
        return this.selectedCrossfeed;
    }

    public void setSelectedCrossfeed(CrossfeedMode mode) {
        if (mode != null && mode != this.selectedCrossfeed) {
            this.selectedCrossfeed = mode;
            if (this.isRocketAssembled()) {
                this.rebuildAssemblyGraph();
            }
            this.sync();
        }
    }

    public void toggleSelectedCrossfeed() {
        this.setSelectedCrossfeed(this.selectedCrossfeed.toggleAsparagus());
    }

    public boolean isAssemblyInProgress() {
        return !this.pendingAssemblySteps.isEmpty() || (this.assemblyTotalSteps > 0 && this.assemblyCompletedSteps < this.assemblyTotalSteps);
    }

    public float getAssemblyProgress() {
        if (this.assemblyTotalDurationTicks <= 0) {
            return this.isAssemblyInProgress() ? 0.0F : 1.0F;
        }
        return Math.clamp((float) this.assemblyTotalElapsedTicks / (float) this.assemblyTotalDurationTicks, 0.0F, 1.0F);
    }

    public int getAssemblyCompletedSteps() {
        return this.assemblyCompletedSteps;
    }

    public int getAssemblyTotalSteps() {
        return this.assemblyTotalSteps;
    }

    public int getAssemblyRemainingTicks() {
        return Math.max(0, this.assemblyTotalDurationTicks - this.assemblyTotalElapsedTicks);
    }

    public LaunchSequencePhase getLaunchPhase() {
        return this.launchPhase;
    }

    public boolean isLaunchSequenceActive() {
        return this.launchPhase != LaunchSequencePhase.IDLE;
    }

    public boolean isHoldDownClampsEngaged() {
        return this.launchPhase == LaunchSequencePhase.THRUST_BUILDUP;
    }

    public int getLaunchCountdownTicks() {
        return this.launchCountdownTicks;
    }

    public float getLaunchCountdownSeconds() {
        return this.launchCountdownTicks / 20.0F;
    }

    public String getLastLaunchAbortReason() {
        return this.lastLaunchAbortReason;
    }

    public @Nullable ModularRocketFlightEntity getLastLaunchedEntity() {
        return this.lastLaunchedEntity;
    }

    /**
     * Initiates the realistic {@code T-10.00s -> T-0.00s} launch countdown sequence:
     * <ul>
     *   <li>{@code T-10.00s .. T-3.01s}: Pre-flight interlocks, access bridge retract, and Water Deluge steam plumes.</li>
     *   <li>{@code T-3.00s .. T-0.01s}: Main liquid Thrust Buildup under Hold-Down Clamps, verifying {@code TWR >= 1.15}
     *       and Weibull T-0 engine/plumbing reliability ({@code sigma = 1.20}) with Safe Pad Abort on failure.</li>
     *   <li>{@code T = 0.00s}: Hold-Down Clamps release and atomic block -> {@link ModularRocketFlightEntity} conversion.</li>
     * </ul>
     */
    public boolean startLaunchSequence(@Nullable ServerPlayer initiator) {
        if (!(this.level instanceof ServerLevel serverLevel) || !this.formed || this.tearingDown) {
            return false;
        }
        if (this.isAssemblyInProgress()) {
            this.notifyOperator(initiator, "Aguarde a conclusão da solda de montagem!", true);
            return false;
        }
        if (!this.isRocketAssembled()) {
            this.notifyOperator(initiator, "Nenhum foguete montado no pad!", true);
            return false;
        }
        if (this.launchPhase != LaunchSequencePhase.IDLE) {
            return false;
        }

        this.rebuildAssemblyGraph();
        if (this.assemblyGraph.isEmpty() || this.assemblyGraph.activeStage() == null) {
            this.notifyOperator(initiator, "Grafo de montagem vazio ou inválido!", true);
            return false;
        }

        // Validate pad mass limit M_max
        double wetMassKg = ModularFlightPhysics.computeTotalWetMassKg(this.assemblyGraph);
        double maxPadMassKg = RocketAssemblyGraphBuilder.maxMassKgForPad(this.platformSize);
        if (wetMassKg > maxPadMassKg) {
            String msg = String.format(Locale.ROOT, "PAD_OVERWEIGHT: %.1ft > limite %.1ft do pad!", wetMassKg / 1000.0, maxPadMassKg / 1000.0);
            this.lastLaunchAbortReason = msg;
            this.notifyOperator(initiator, msg, true);
            this.sync();
            return false;
        }

        RocketStageNode stage0 = this.assemblyGraph.activeStage();
        if (stage0.activeThrustKn() <= 0.0) {
            String msg = "Estágio 0 sem motores ativos!";
            this.lastLaunchAbortReason = msg;
            this.notifyOperator(initiator, msg, true);
            this.sync();
            return false;
        }

        boolean hasPropellant = stage0.totalCurrentFuelDroplets() > 0L
                || ModularFlightPhysics.resolvePropellant(stage0.propellantId()) == PropellantType.SOLID;
        if (!hasPropellant) {
            String msg = "Estágio 0 sem propelente carregado!";
            this.lastLaunchAbortReason = msg;
            this.notifyOperator(initiator, msg, true);
            this.sync();
            return false;
        }

        // Prevent unfueled liquid radial boosters from immediately detaching & exploding inside the launch tower at T+0.05s
        for (RadialBoosterGroup group : stage0.radialGroups()) {
            if (!group.decoupled() && !group.solidBooster() && group.fuelCapacityDroplets() > 0L && group.currentFuelDroplets() <= 0L) {
                String msg = "Boosters radiais sem propelente carregado! Abasteça antes de lançar.";
                this.lastLaunchAbortReason = msg;
                this.notifyOperator(initiator, msg, true);
                this.sync();
                return false;
            }
        }

        // Collect pilot + nearby crew inside the hangar/pad envelope (up to seat capacity)
        this.launchCrewUuids.clear();
        int maxSeats = Math.max(1, this.assemblyGraph.crewSeats().size());
        if (initiator != null) {
            this.launchCrewUuids.add(initiator.getUUID());
        }
        AABB padVolume = this.computeAssembledRocketWorldBounds();
        if (padVolume != null) {
            AABB crewZone = padVolume.inflate(4.0, 2.0, 4.0);
            for (ServerPlayer nearby : serverLevel.getEntitiesOfClass(ServerPlayer.class, crewZone)) {
                if (this.launchCrewUuids.size() >= maxSeats) {
                    break;
                }
                if (!this.launchCrewUuids.contains(nearby.getUUID()) && nearby.isAlive() && !nearby.isSpectator()) {
                    this.launchCrewUuids.add(nearby.getUUID());
                }
            }
        }

        this.launchPhase = LaunchSequencePhase.PRE_FLIGHT_DELUGE;
        this.launchCountdownTicks = LAUNCH_COUNTDOWN_TOTAL_TICKS;
        this.lastLaunchAbortReason = "";
        this.launchClampCheckPassed = false;
        this.lastLaunchedEntity = null;

        BlockPos padCenter = this.getPadCenterPos();
        serverLevel.playSound(null, padCenter.above(), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.2F, 0.85F);
        serverLevel.playSound(null, padCenter.above(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.1F, 0.7F);
        this.broadcastLaunchCrew(serverLevel,
                Component.literal("[T-10.0s] Sequência iniciada! Ponte de acesso recolhida — Water Deluge ativo!").withStyle(net.minecraft.ChatFormatting.AQUA));
        this.sync();
        return true;
    }

    /**
     * Executes a <b>Safe Pad Abort</b>: immediately cuts engines while Hold-Down Clamps stay locked,
     * preserving the rocket and all pad blocks 100% undamaged.
     */
    public boolean abortLaunchSequence(String reason) {
        if (this.launchPhase == LaunchSequencePhase.IDLE) {
            return false;
        }
        this.launchPhase = LaunchSequencePhase.IDLE;
        this.launchCountdownTicks = 0;
        this.launchClampCheckPassed = false;
        this.lastLaunchAbortReason = (reason != null && !reason.isBlank()) ? reason : "Safe Pad Abort";

        if (this.level instanceof ServerLevel serverLevel) {
            BlockPos padCenter = this.getPadCenterPos();
            serverLevel.playSound(null, padCenter.above(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.3F, 0.6F);
            serverLevel.playSound(null, padCenter.above(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.5F);
            this.broadcastLaunchCrew(serverLevel,
                    Component.literal("[SAFE PAD ABORT] " + this.lastLaunchAbortReason + " — Clamps travados, foguete intacto!")
                            .withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD));
        }
        this.launchCrewUuids.clear();
        this.sync();
        return true;
    }

    /**
     * Adopts a modular rocket that touched down safely on this formed launchpad via
     * {@link RocketStateConverter#convertToStaticBlocks}, restoring exact per-stage fuel, energy &amp; oxygen.
     */
    public void adoptLandedModularRocket(
            List<BlockPos> masterPartPositions,
            RocketAssemblyGraph landedGraph,
            long remainingEnergyGj,
            long remainingOxygenDroplets
    ) {
        if (this.level == null || this.level.isClientSide || !this.formed) {
            return;
        }
        this.assembledRocketParts.clear();
        for (BlockPos pos : masterPartPositions) {
            BlockPos immutable = pos.immutable();
            this.assembledRocketParts.add(immutable);
            if (this.level.getBlockEntity(immutable) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
                partBe.setAssembledTerminalPos(this.worldPosition);
            }
        }
        this.rocketAssembled = !this.assembledRocketParts.isEmpty();
        this.stageFuelDroplets.clear();
        this.boosterFuelDroplets.clear();
        for (RocketStageNode stage : landedGraph.stages()) {
            if (!stage.decoupled()) {
                this.stageFuelDroplets.put(stage.stageIndex(), stage.currentFuelDroplets());
                for (RadialBoosterGroup group : stage.radialGroups()) {
                    if (!group.decoupled()) {
                        this.boosterFuelDroplets.put(group.groupId(), group.currentFuelDroplets());
                    }
                }
            }
        }
        this.rocketEnergy = Math.max(0L, remainingEnergyGj);
        this.rocketOxygenDroplets = Math.max(0L, remainingOxygenDroplets);
        this.rebuildAssemblyGraph();
        this.sync();
    }

    private void tickLaunchSequence(Level level) {
        if (this.launchPhase == LaunchSequencePhase.IDLE) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!this.isRocketAssembled() || this.assemblyGraph.isEmpty()) {
            this.abortLaunchSequence("Estrutura do foguete alterada durante a contagem");
            return;
        }

        BlockPos padCenter = this.getPadCenterPos();
        double cx = padCenter.getX() + 0.5;
        double cy = padCenter.getY() + 1.0;
        double cz = padCenter.getZ() + 0.5;

        this.launchCountdownTicks = Math.max(0, this.launchCountdownTicks - 1);

        // Phase 1: T-10.00s .. T-3.01s -> Water Deluge steam plumes + acoustic suppression
        if (this.launchPhase == LaunchSequencePhase.PRE_FLIGHT_DELUGE) {
            if ((this.launchCountdownTicks % 2) == 0) {
                double spread = Math.max(1.5, this.platformSize.padDeckWidth() * 0.35);
                serverLevel.sendParticles(ParticleTypes.CLOUD, cx, cy, cz, 8, spread, 0.35, spread, 0.08);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, cx, cy + 0.2, cz, 4, spread, 0.25, spread, 0.04);
            }
            if ((this.launchCountdownTicks % 20) == 0 && this.launchCountdownTicks > THRUST_BUILDUP_START_TICKS) {
                int sec = this.launchCountdownTicks / 20;
                serverLevel.playSound(null, padCenter.above(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.9F, 0.75F);
                this.broadcastLaunchCrew(serverLevel,
                        Component.literal("[T-" + sec + "s] Water Deluge ativo...").withStyle(net.minecraft.ChatFormatting.AQUA));
            }
            if (this.launchCountdownTicks <= THRUST_BUILDUP_START_TICKS) {
                this.launchPhase = LaunchSequencePhase.THRUST_BUILDUP;
                serverLevel.playSound(null, padCenter.above(), GCSounds.ROCKET_IGNITE, SoundSource.BLOCKS, 1.8F, 0.85F);
                this.broadcastLaunchCrew(serverLevel,
                        Component.literal("[T-3.0s] Ignição principal — Hold-Down Clamps travados (Thrust Buildup)!").withStyle(net.minecraft.ChatFormatting.GOLD));
                this.sync();
            } else if ((this.launchCountdownTicks % 5) == 0) {
                this.sync();
            }
            return;
        }

        // Phase 2: T-3.00s .. T-0.01s -> Thrust Buildup under Hold-Down Clamps + TWR/Weibull safety interlock
        if (this.launchPhase == LaunchSequencePhase.THRUST_BUILDUP) {
            double spread = Math.max(1.2, this.assemblyGraph.maxRadiusBlocks() * 0.75);
            java.util.UUID exemptUuid = this.launchCrewUuids.isEmpty() ? net.minecraft.Util.NIL_UUID : this.launchCrewUuids.get(0);
            serverLevel.sendParticles(new EntityParticleOption(GCParticleTypes.LAUNCH_FLAME, exemptUuid), cx, cy + 0.1, cz, 6, spread * 0.5, 0.15, spread * 0.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.FLAME, cx, cy + 0.15, cz, 8, spread * 0.45, 0.15, spread * 0.45, 0.08);
            serverLevel.sendParticles(ParticleTypes.CLOUD, cx, cy + 0.2, cz, 10, spread * 1.4, 0.4, spread * 1.4, 0.12);

            if ((this.launchCountdownTicks % 20) == 0 && this.launchCountdownTicks > 0) {
                int sec = this.launchCountdownTicks / 20;
                this.broadcastLaunchCrew(serverLevel,
                        Component.literal("[T-" + sec + "s] Empuxo crescendo sob Hold-Down Clamps...").withStyle(net.minecraft.ChatFormatting.YELLOW));
            }

            // Safety interlock check under Hold-Down Clamps before release
            if (!this.launchClampCheckPassed && this.launchCountdownTicks <= HOLD_DOWN_CLAMP_CHECK_TICK) {
                if (!this.runHoldDownClampInterlock(serverLevel)) {
                    return; // Safe Pad Abort already triggered
                }
                this.launchClampCheckPassed = true;
            }

            if (this.launchCountdownTicks <= 0) {
                if (!this.launchClampCheckPassed && !this.runHoldDownClampInterlock(serverLevel)) {
                    return;
                }
                this.executeAtomicLiftoff(serverLevel);
            } else if ((this.launchCountdownTicks % 4) == 0) {
                this.sync();
            }
        }
    }

    /**
     * Runs the Hold-Down Clamp T-0 safety interlock:
     * <ol>
     *   <li>Verifies {@code TWR > 1.00} against origin {@link PlanetaryEnvironment#gravityG()}, warning on marginal TWR/Δv without blocking liftoff</li>
     *   <li>Rolls calibrated Weibull T-0 ignition check ({@code sigma = 1.20}) on Stage 0 engines &amp; tanks</li>
     * </ol>
     * On any failure, executes {@link #abortLaunchSequence} (Safe Pad Abort: engines cut, clamps stay locked,
     * rocket 100% undamaged).
     */
    private boolean runHoldDownClampInterlock(ServerLevel serverLevel) {
        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(serverLevel);
        double atmTwr = ModularFlightPhysics.computeLiftoffTwr(this.assemblyGraph, env);
        RocketStageNode stage0 = this.assemblyGraph.activeStage();
        long fueledStage0Droplets = stage0 != null ? stage0.totalCurrentFuelDroplets() : 0L;
        double nominalFueledTwr = (stage0 != null && fueledStage0Droplets > 0L)
                ? dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator.computeTwr(
                        stage0.activeThrustKn(),
                        ModularFlightPhysics.computeTotalWetMassKg(this.assemblyGraph),
                        env.gravityG())
                : atmTwr;
        double effectiveLiftoffTwr = Math.max(atmTwr, nominalFueledTwr);

        if (env.gravityG() > 0.001 && effectiveLiftoffTwr <= ModularFlightPhysics.MIN_LIFTOFF_TWR) {
            this.abortLaunchSequence(String.format(Locale.ROOT,
                    "TWR %.2f <= %.2f em %s (%.2fG)", effectiveLiftoffTwr, ModularFlightPhysics.MIN_LIFTOFF_TWR, env.displayName(), env.gravityG()));
            return false;
        }
        if (env.gravityG() > 0.001 && atmTwr < 1.25) {
            this.broadcastLaunchCrew(serverLevel, Component.literal(String.format(Locale.ROOT,
                    "[AVISO DE VOO] TWR marginal (%.2f atm / %.2f vácuo) em %s — decolagem autorizada, mas pode não atingir a órbita!",
                    atmTwr, effectiveLiftoffTwr, env.displayName()))
                    .withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD));
        }

        if (stage0 != null) {
            for (var part : stage0.allActiveParts()) {
                RocketPartCategory cat = part.config().category();
                if (cat == RocketPartCategory.ENGINE || cat == RocketPartCategory.TANK) {
                    if (LaunchFailureEngine.rollFailure(part.integrity(), LaunchFailureEngine.STRESS_T0_IGNITION, serverLevel.random)) {
                        this.abortLaunchSequence(String.format(Locale.ROOT,
                                "Falha Weibull T-0 em %s (Integridade %.0f%%)", part.config().modelId(), part.integrity().fraction() * 100.0));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void executeAtomicLiftoff(ServerLevel serverLevel) {
        BlockPos padCenter = this.getPadCenterPos();
        Direction facing = this.getFacing();
        List<BlockPos> partSnapshot = new ArrayList<>(this.assembledRocketParts);
        List<java.util.UUID> crewSnapshot = new ArrayList<>(this.launchCrewUuids);
        RocketAssemblyGraph graphSnapshot = this.assemblyGraph;
        long energySnapshot = this.rocketEnergy;
        long oxygenSnapshot = this.rocketOxygenDroplets;

        ModularRocketFlightEntity launched = RocketStateConverter.convertToFlightEntity(
                serverLevel,
                padCenter,
                facing,
                this.worldPosition,
                graphSnapshot,
                energySnapshot,
                oxygenSnapshot,
                partSnapshot,
                crewSnapshot
        );
        if (launched == null) {
            this.abortLaunchSequence("Falha de spawn atômico (rollback intacto)");
            return;
        }

        // Liftoff certified successful: clear terminal references without touching blocks (already cleared atomically)
        this.lastLaunchedEntity = launched;
        this.assembledRocketParts.clear();
        this.assemblyGraph = RocketAssemblyGraph.EMPTY;
        this.stageFuelDroplets.clear();
        this.stageFuelVariants.clear();
        this.boosterFuelDroplets.clear();
        this.boosterFuelVariants.clear();
        this.rocketAssembled = false;
        this.rocketFuelDroplets = 0L;
        this.rocketEnergy = 0L;
        this.rocketOxygenDroplets = 0L;
        this.launchPhase = LaunchSequencePhase.IDLE;
        this.launchCountdownTicks = 0;
        this.launchClampCheckPassed = false;
        this.launchCrewUuids.clear();
        this.rebuildFluidStorage();

        serverLevel.playSound(null, padCenter.above(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.5F, 0.7F);
        serverLevel.playSound(null, padCenter.above(), GCSounds.ROCKET_IGNITE, SoundSource.BLOCKS, 2.0F, 1.0F);
        this.sync();
    }

    private void notifyOperator(@Nullable ServerPlayer operator, String message, boolean error) {
        if (operator != null) {
            operator.displayClientMessage(
                    Component.literal("[LAUNCH] " + message)
                            .withStyle(error ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.AQUA),
                    true
            );
        }
    }

    private void broadcastLaunchCrew(ServerLevel serverLevel, Component message) {
        for (java.util.UUID uuid : this.launchCrewUuids) {
            if (serverLevel.getPlayerByUUID(uuid) instanceof ServerPlayer sp) {
                sp.displayClientMessage(message, true);
            }
        }
    }

    public long getStageFuelDroplets(int stageIndex) {
        return this.stageFuelDroplets.getOrDefault(stageIndex, 0L);
    }

    public FluidVariant getStageFuelVariant(int stageIndex) {
        return this.stageFuelVariants.getOrDefault(stageIndex, FluidVariant.of(GCFluids.FUEL));
    }

    public long getBoosterFuelDroplets(String groupId) {
        return this.boosterFuelDroplets.getOrDefault(groupId, 0L);
    }

    public FluidVariant getRocketFuelVariant() {
        return this.rocketFuelDroplets > 0L && !this.rocketFuelVariant.isBlank()
                ? this.rocketFuelVariant
                : FluidVariant.of(GCFluids.FUEL);
    }

    public void setRocketFuel(FluidVariant variant, long droplets) {
        long clamped = Math.max(0L, droplets);
        this.rocketFuelDroplets = clamped;
        if (variant != null && !variant.isBlank() && PropellantType.isRocketPropellantFluid(variant.getFluid())) {
            this.rocketFuelVariant = variant;
        }
        if (!this.assemblyGraph.isEmpty()) {
            long remaining = clamped;
            FluidVariant resolvedVar = this.rocketFuelVariant;
            // First try stages matching the propellant fluid chemistry
            for (RocketStageNode stage : this.assemblyGraph.stages()) {
                if (stage.decoupled() || stage.fuelCapacityDroplets() <= 0L) {
                    continue;
                }
                PropellantType stageProp = resolvePropellantById(stage.propellantId());
                if (resolvedVar.isBlank() || stageProp.acceptsFluid(resolvedVar.getFluid())) {
                    long put = Math.min(remaining, stage.fuelCapacityDroplets());
                    this.stageFuelDroplets.put(stage.stageIndex(), put);
                    if (put > 0L && !resolvedVar.isBlank()) {
                        this.stageFuelVariants.put(stage.stageIndex(), resolvedVar);
                    }
                    remaining -= put;
                }
            }
            // Fallback: distribute any remaining droplets into Stage 0..N (preserves tests loading cryo fluid into generic tank)
            if (remaining > 0L) {
                for (RocketStageNode stage : this.assemblyGraph.stages()) {
                    if (stage.decoupled() || stage.fuelCapacityDroplets() <= 0L) {
                        continue;
                    }
                    long cur = this.stageFuelDroplets.getOrDefault(stage.stageIndex(), 0L);
                    long space = Math.max(0L, stage.fuelCapacityDroplets() - cur);
                    long put = Math.min(remaining, space);
                    if (put > 0L) {
                        this.stageFuelDroplets.put(stage.stageIndex(), cur + put);
                        if (!resolvedVar.isBlank()) {
                            this.stageFuelVariants.put(stage.stageIndex(), resolvedVar);
                        }
                        remaining -= put;
                    }
                }
            }
            if (remaining > 0L) {
                for (RocketStageNode stage : this.assemblyGraph.stages()) {
                    for (RadialBoosterGroup group : stage.radialGroups()) {
                        if (group.decoupled() || group.solidBooster() || group.fuelCapacityDroplets() <= 0L) {
                            continue;
                        }
                        long put = Math.min(remaining, group.fuelCapacityDroplets());
                        this.boosterFuelDroplets.put(group.groupId(), put);
                        if (put > 0L && !resolvedVar.isBlank()) {
                            this.boosterFuelVariants.put(group.groupId(), resolvedVar);
                        }
                        remaining -= put;
                    }
                }
            }
            this.recomputeAggregateFuelAndGraph();
        }
        this.sync();
    }

    public void setRocketEnergy(long energy) {
        this.rocketEnergy = Math.max(0L, energy);
        this.sync();
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
            this.assemblyGraph = RocketAssemblyGraph.EMPTY;
            this.stageFuelDroplets.clear();
            this.stageFuelVariants.clear();
            this.boosterFuelDroplets.clear();
            this.boosterFuelVariants.clear();
            this.rebuildFluidStorage();
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
        this.assemblyGraph = RocketAssemblyGraph.EMPTY;
        this.stageFuelDroplets.clear();
        this.stageFuelVariants.clear();
        this.boosterFuelDroplets.clear();
        this.boosterFuelVariants.clear();
        this.rebuildFluidStorage();
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
        if (this.assemblyGraph.isEmpty()) {
            this.rebuildAssemblyGraph();
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
            this.stageFuelDroplets.clear();
            this.boosterFuelDroplets.clear();
            this.rocketFuelDroplets = 0L;
            this.rocketEnergy = 0L;
            this.rocketOxygenDroplets = 0L;
        } else {
            for (RocketStageNode stage : this.assemblyGraph.stages()) {
                if (!stage.decoupled() && stage.fuelCapacityDroplets() > 0L) {
                    this.stageFuelDroplets.put(stage.stageIndex(), stage.fuelCapacityDroplets());
                    this.stageFuelVariants.put(stage.stageIndex(), defaultFluidForPropellant(resolvePropellantById(stage.propellantId())));
                }
                for (RadialBoosterGroup group : stage.radialGroups()) {
                    if (!group.decoupled() && group.fuelCapacityDroplets() > 0L) {
                        this.boosterFuelDroplets.put(group.groupId(), group.fuelCapacityDroplets());
                        this.boosterFuelVariants.put(group.groupId(), defaultFluidForPropellant(resolvePropellantById(group.propellantId())));
                    }
                }
            }
            this.rocketFuelDroplets = fuelCap;
            this.rocketEnergy = energyCap;
            this.rocketOxygenDroplets = oxygenCap;
        }
        this.recomputeAggregateFuelAndGraph();
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
            RocketPartComposition comp = partBe.getComposition();
            RocketPartStats stats = partBe.getStats();
            if (stats == null) {
                stats = RocketPartCostCalculator.calculate(cfg, comp).stats();
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
            totalEnergyCapacity += RocketPartCostCalculator.calculateEnergyCapacity(cfg, comp);
            totalEnergyGenPerTick += RocketPartCostCalculator.calculateEnergyGenerationPerTick(cfg, comp);
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

        // Evaluate emergent multi-stage physics via RocketAssemblyGraphBuilder + RocketFlightValidator.evaluateGraph
        PlanetaryEnvironment localEnv = PlanetaryEnvironment.fromLevel(this.level);
        RocketAssemblyGraphBuilder.BuildResult graphBuild = RocketAssemblyGraphBuilder.buildFromPad(
                padCenter,
                foundEntities,
                this.platformSize,
                blueprint,
                unified ? this.stageFuelDroplets : Map.of(),
                unified ? this.boosterFuelDroplets : Map.of(),
                this.selectedCrossfeed
        );
        RocketFlightValidator.GraphFlightValidationReport graphReport = RocketFlightValidator.evaluateGraph(
                graphBuild.graph(),
                localEnv,
                localEnv,
                graphBuild.padMaxMassKg(),
                currentEnergy
        );
        RocketFlightValidator.FlightValidationReport localReport = graphReport.summary();
        double fullWetMassKg = localReport.wetMassKg();
        double currentWetMassKg = localReport.currentWetMassKg();
        double fullTwr = Double.isFinite(localReport.originTwr()) ? localReport.originTwr() : 99.99;
        double currentTwrRaw = RocketFlightValidator.computeTwr(localReport.totalThrustKn(), currentWetMassKg, localEnv.gravityG());
        double currentTwr = Double.isFinite(currentTwrRaw) ? currentTwrRaw : 99.99;
        double maxDeltaV = localReport.maxDeltaV();
        double currentDeltaV = localReport.currentDeltaV();
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
            this.cancelProgressiveAssembly(null);
        }
    }

    /**
     * Computes live telemetry from the modular rocket parts staged in {@link #rocketSlots}
     * (ordered bottom-to-top: slot 0 = base .. slot 7 = top) by compiling a {@link RocketAssemblyGraph}
     * and delegating to {@link RocketFlightValidator#evaluateGraph} against the active planetary environment.
     */
    public RocketPadSummary previewRocketFromSlots() {
        List<ItemStack> stagedStacks = new ArrayList<>(ROCKET_SLOT_COUNT);
        List<MountedRocketPart> parts = new ArrayList<>();
        double dryMassKg = 0.0;
        double totalThrustKn = 0.0;
        long totalFuelDroplets = 0L;
        long totalEnergyCap = 0L;
        long totalEnergyGen = 0L;
        long totalOxygenCap = 0L;
        int maxDiameter = 0;
        boolean hasCommand = false;
        boolean hasTank = false;
        boolean hasEngine = false;
        double minIntegrity = 1.0;
        double sumIntegrity = 0.0;
        double minThermal = Double.MAX_VALUE;
        double minPressure = Double.MAX_VALUE;

        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        BlockPos padCenter = this.getPadCenterPos();
        int currentRelY = 1;

        for (int i = 0; i < ROCKET_SLOT_COUNT; i++) {
            ItemStack stack = this.rocketSlots.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
                continue;
            }
            stagedStacks.add(stack);
            RocketPartConfig cfg = partItem.getConfig(stack);
            RocketPartComposition comp = stack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
            RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
            if (stats == null) {
                stats = RocketPartCostCalculator.calculate(cfg, comp).stats();
            }
            PartIntegrity integrity = stack.get(GCDataComponents.PART_INTEGRITY);
            double intFrac = integrity != null ? integrity.fraction() : 1.0;
            minIntegrity = Math.min(minIntegrity, intFrac);
            sumIntegrity += intFrac;

            RocketPartSpec spec = catalog.getPart(cfg.modelId());
            String displayName = spec != null ? spec.displayName() : cfg.modelId();

            dryMassKg += stats.dryWeightKg();
            totalThrustKn += stats.thrustKn();
            totalFuelDroplets += stats.fuelCapacityDroplets();
            totalEnergyCap += RocketPartCostCalculator.calculateEnergyCapacity(cfg, comp);
            totalEnergyGen += RocketPartCostCalculator.calculateEnergyGenerationPerTick(cfg, comp);
            totalOxygenCap += RocketPartCostCalculator.calculateOxygenCapacityDroplets(cfg);
            if (stats.thermalToleranceKelvin() > 0.0) {
                minThermal = Math.min(minThermal, stats.thermalToleranceKelvin());
            }
            if (stats.pressureToleranceAtm() > 0.0) {
                minPressure = Math.min(minPressure, stats.pressureToleranceAtm());
            }
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

        PlanetaryEnvironment localEnv = PlanetaryEnvironment.fromLevel(this.level);
        RocketAssemblyGraphBuilder.BuildResult buildResult = RocketAssemblyGraphBuilder.buildFromStacks(
                stagedStacks, this.platformSize, this.getActiveBlueprint()
        );
        RocketFlightValidator.GraphFlightValidationReport graphReport = RocketFlightValidator.evaluateGraph(
                buildResult.graph(), localEnv, localEnv, buildResult.padMaxMassKg(), 0L
        );
        RocketFlightValidator.FlightValidationReport report = graphReport.summary();

        double wetMassKg = report.wetMassKg();
        double fullTwr = Double.isFinite(report.originTwr()) ? report.originTwr() : 99.99;
        double dryTwrRaw = RocketFlightValidator.computeTwr(report.totalThrustKn(), dryMassKg, localEnv.gravityG());
        double dryTwr = Double.isFinite(dryTwrRaw) ? dryTwrRaw : 99.99;
        int allowedDiameter = this.platformSize.diameter();
        boolean diameterOk = maxDiameter == 0 || maxDiameter <= allowedDiameter;
        double avgIntegrity = sumIntegrity / parts.size();

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
                fullTwr,
                dryTwr,
                report.maxDeltaV(),
                0.0,
                maxDiameter,
                allowedDiameter,
                hasCommand,
                hasTank,
                hasEngine,
                diameterOk,
                minIntegrity,
                avgIntegrity,
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
        terminal.tickProgressiveAssembly(level);
        terminal.tickLaunchSequence(level);
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

    /**
     * Builds an immutable {@link RocketFlightValidator.VehicleSnapshot} of the modular rocket
     * currently mounted on this launch pad (unified or loose parts).
     */
    public RocketFlightValidator.VehicleSnapshot buildVehicleSnapshot() {
        if (this.level == null || !this.formed) {
            return new RocketFlightValidator.VehicleSnapshot(List.of(), 0L, 0L, null);
        }
        RocketPadSummary summary = this.scanRocketOnPad();
        List<ModularRocketPartBlockEntity> entities = new ArrayList<>(summary.parts().size());
        for (MountedRocketPart part : summary.parts()) {
            if (this.level.isLoaded(part.pos())
                    && this.level.getBlockEntity(part.pos()) instanceof ModularRocketPartBlockEntity partBe
                    && !partBe.isRemoved()) {
                entities.add(partBe);
            }
        }
        return this.buildSnapshotFromEntities(entities, summary.currentFuelDroplets(), summary.currentEnergy());
    }

    private RocketFlightValidator.VehicleSnapshot buildSnapshotFromEntities(
            List<ModularRocketPartBlockEntity> entities,
            long currentFuel,
            long currentEnergy
    ) {
        List<RocketFlightValidator.PartSnapshot> snapshots = new ArrayList<>(entities.size());
        for (ModularRocketPartBlockEntity partBe : entities) {
            snapshots.add(RocketFlightValidator.PartSnapshot.fromParts(
                    partBe.getConfig(),
                    partBe.getStats(),
                    partBe.getIntegrity(),
                    partBe.getComposition()
            ));
        }
        net.minecraft.world.level.material.Fluid loadedFluid = (currentFuel > 0L && !this.rocketFuelVariant.isBlank())
                ? this.rocketFuelVariant.getFluid()
                : null;
        return new RocketFlightValidator.VehicleSnapshot(snapshots, currentFuel, currentEnergy, loadedFluid);
    }

    /**
     * Compiles the live {@link RocketAssemblyGraph} from all modular rocket parts currently mounted
     * on this formed launch pad.
     */
    public RocketAssemblyGraphBuilder.BuildResult compileCurrentPadGraph() {
        if (this.level == null || !this.formed) {
            return new RocketAssemblyGraphBuilder.BuildResult(
                    RocketAssemblyGraph.EMPTY,
                    List.of(),
                    0.0,
                    RocketAssemblyGraphBuilder.maxMassKgForPad(this.platformSize)
            );
        }
        List<ModularRocketPartBlockEntity> entities = this.collectPadPartEntities();
        return RocketAssemblyGraphBuilder.buildFromPad(
                this.getPadCenterPos(),
                entities,
                this.platformSize,
                this.getActiveBlueprint(),
                this.isRocketAssembled() ? this.stageFuelDroplets : Map.of(),
                this.isRocketAssembled() ? this.boosterFuelDroplets : Map.of(),
                this.selectedCrossfeed
        );
    }

    /**
     * Evaluates the currently mounted rocket on this pad against {@code destination} using
     * multi-stage {@link RocketFlightValidator#evaluateGraph} physics.
     */
    public RocketFlightValidator.GraphFlightValidationReport evaluateGraphFlight(PlanetaryEnvironment destination) {
        PlanetaryEnvironment origin = PlanetaryEnvironment.fromLevel(this.level);
        RocketAssemblyGraphBuilder.BuildResult build = this.compileCurrentPadGraph();
        long energy = this.isRocketAssembled() ? this.rocketEnergy : 0L;
        return RocketFlightValidator.evaluateGraph(build.graph(), origin, destination, build.padMaxMassKg(), energy);
    }

    /**
     * Evaluates the currently mounted rocket on this pad against {@code destination} using
     * emergent aerospace physics ({@link RocketFlightValidator}).
     */
    public RocketFlightValidator.FlightValidationReport evaluateFlight(PlanetaryEnvironment destination) {
        return this.evaluateGraphFlight(destination).summary();
    }

    private List<ModularRocketPartBlockEntity> collectPadPartEntities() {
        if (this.level == null || !this.formed) {
            return List.of();
        }
        List<ModularRocketPartBlockEntity> found = new ArrayList<>();
        if (this.isRocketAssembled()) {
            for (BlockPos partPos : this.assembledRocketParts) {
                if (this.level.isLoaded(partPos)
                        && this.level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe
                        && !partBe.isRemoved()) {
                    found.add(partBe);
                }
            }
            return found;
        }
        PlatformBlueprint blueprint = this.getActiveBlueprint();
        BlockPos padCenter = this.getPadCenterPos();
        Direction facing = this.getFacing();
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
                        found.add(partBe);
                    }
                }
            }
        }
        return found;
    }

    /**
     * Executes an automated telemetry probe sortie for an assembled rocket on this pad carrying
     * {@code science_*} modules and a {@code BLANK_DATA_DRIVE}. Validates emergent flight physics
     * via {@link RocketFlightValidator}: if thermal/pressure/acid limits are exceeded and a
     * {@code science_flight_recorder} is mounted, records a <b>+200%</b> {@code THERMAL_FAILURE}
     * black-box drive!
     */
    public dev.galacticraft.mod.content.entity.orbital.FlightTelemetryEngine.ProbeMissionResult executeTelemetryProbeSortie(
            PlanetaryEnvironment destination,
            @Nullable dev.galacticraft.api.universe.celestialbody.CelestialBody<?, ?> destinationBody,
            @Nullable ServerPlayer operator
    ) {
        if (!(this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) || !this.formed) {
            return new dev.galacticraft.mod.content.entity.orbital.FlightTelemetryEngine.ProbeMissionResult(
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, "Pad not formed");
        }
        RocketPadSummary summary = this.scanRocketOnPad();
        List<String> scienceModules = new ArrayList<>();
        ModularRocketPartBlockEntity driveCarrier = null;
        for (MountedRocketPart part : summary.parts()) {
            if (this.level.isLoaded(part.pos())
                    && this.level.getBlockEntity(part.pos()) instanceof ModularRocketPartBlockEntity partBe
                    && !partBe.isRemoved()
                    && partBe.isSciencePayloadModule()) {
                scienceModules.add(partBe.getConfig().familyId());
                if (driveCarrier == null && partBe.getScienceDrive().is(dev.galacticraft.mod.content.item.GCItems.BLANK_DATA_DRIVE)) {
                    driveCarrier = partBe;
                }
            }
        }

        ItemStack inputDrive = driveCarrier != null ? driveCarrier.getScienceDrive() : ItemStack.EMPTY;
        if (inputDrive.isEmpty() && operator != null) {
            for (int i = 0; i < operator.getInventory().getContainerSize(); i++) {
                ItemStack s = operator.getInventory().getItem(i);
                if (s.is(dev.galacticraft.mod.content.item.GCItems.BLANK_DATA_DRIVE)) {
                    inputDrive = s.copyWithCount(1);
                    if (!operator.isCreative()) {
                        s.shrink(1);
                    }
                    break;
                }
            }
        }

        RocketFlightValidator.FlightValidationReport report = this.evaluateFlight(destination);
        boolean thermalOrStructuralFailure = !report.thermalSafe() || !report.pressureSafe() || report.acidHazard();

        var result = dev.galacticraft.mod.content.entity.orbital.FlightTelemetryEngine.executeProbeMission(
                serverLevel,
                destination,
                destinationBody,
                scienceModules,
                inputDrive,
                thermalOrStructuralFailure,
                this.getPadCenterPos()
        );

        if (driveCarrier != null && result.hasRecordedDrive()) {
            driveCarrier.setScienceDrive(result.recordedDrive());
        } else if (result.hasRecordedDrive()) {
            if (operator != null) {
                if (!operator.getInventory().add(result.recordedDrive().copy())) {
                    operator.drop(result.recordedDrive().copy(), false);
                }
            } else {
                Block.popResource(serverLevel, this.getPadCenterPos().above(), result.recordedDrive().copy());
            }
        }
        if (!result.orbitalSurveyMap().isEmpty()) {
            if (operator != null && !operator.getInventory().add(result.orbitalSurveyMap().copy())) {
                operator.drop(result.orbitalSurveyMap().copy(), false);
            } else if (operator == null) {
                Block.popResource(serverLevel, this.getPadCenterPos().above(), result.orbitalSurveyMap().copy());
            }
        }
        if (!result.harvestedSample().isEmpty()) {
            if (operator != null && !operator.getInventory().add(result.harvestedSample().copy())) {
                operator.drop(result.harvestedSample().copy(), false);
            } else if (operator == null) {
                Block.popResource(serverLevel, this.getPadCenterPos().above(), result.harvestedSample().copy());
            }
        }
        return result;
    }

    /**
     * Applies environmental stress (thermal overload, hyperbaric pressure, and Venusian sulfuric
     * acid corrosion) for {@code exposureSeconds} to all mounted parts on this platform.
     *
     * @return Minimum resulting integrity fraction across all mounted parts ({@code 0.0..1.0}).
     */
    public double applyEnvironmentalWearToMountedParts(PlanetaryEnvironment env, double exposureSeconds) {
        if (this.level == null || this.level.isClientSide || !this.formed || exposureSeconds <= 0.0) {
            return 1.0;
        }
        double minFrac = 1.0;
        boolean changed = false;
        List<MountedRocketPart> parts = this.scanRocketOnPad().parts();
        for (MountedRocketPart part : parts) {
            if (this.level.isLoaded(part.pos())
                    && this.level.getBlockEntity(part.pos()) instanceof ModularRocketPartBlockEntity partBe
                    && !partBe.isRemoved()) {
                RocketFlightValidator.PartSnapshot snap = RocketFlightValidator.PartSnapshot.fromParts(
                        partBe.getConfig(),
                        partBe.getStats(),
                        partBe.getIntegrity(),
                        partBe.getComposition()
                );
                double wear = RocketFlightValidator.computeEnvironmentalWearPerSecond(snap, env) * exposureSeconds;
                if (wear > 0.0) {
                    PartIntegrity updated = partBe.damageIntegrity(wear);
                    minFrac = Math.min(minFrac, updated.fraction());
                    changed = true;
                } else {
                    PartIntegrity current = partBe.getIntegrity();
                    minFrac = Math.min(minFrac, current != null ? current.fraction() : 1.0);
                }
            }
        }
        if (changed) {
            this.sync();
        }
        return minFrac;
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

        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(level);
        double solarFactor = env.solarIrradianceFactor();
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

        // Gather live part block entities for composition-aware power, cryocooler, boil-off, and environmental wear
        List<ModularRocketPartBlockEntity> liveParts = new ArrayList<>(this.assembledRocketParts.size());
        boolean hasCryocooler = false;
        boolean hasSuperfluidCryo2k = false;
        long generatedEnergy = 0L;
        boolean sunny = level.isDay() && !level.isRaining();

        for (BlockPos partPos : this.assembledRocketParts) {
            if (!level.isLoaded(partPos)) {
                continue;
            }
            if (!(level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe) || partBe.isRemoved()) {
                continue;
            }
            liveParts.add(partBe);
            RocketPartConfig cfg = partBe.getConfig();
            RocketPartComposition comp = partBe.getComposition();

            if (PropellantType.isCryocoolerModule(cfg)
                    || (comp != null && comp.hasTrait(RocketMaterialTrait.ACTIVE_ZERO_BOILOFF))) {
                hasCryocooler = true;
            }
            if (comp != null && comp.hasTrait(RocketMaterialTrait.SUPERFLUID_CRYO_2K)) {
                hasSuperfluidCryo2k = true;
            }

            long partGen = RocketPartCostCalculator.calculateEnergyGenerationPerTick(cfg, comp);
            if (partGen > 0L) {
                boolean isSolar = cfg.familyId().contains("power_solar_array");
                if (isSolar) {
                    long aneutronic = (comp != null && comp.hasTrait(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR))
                            ? RocketPartCostCalculator.ANEUTRONIC_BOARD_REACTOR_GEN_PER_TICK
                            : 0L;
                    long solarOnly = Math.max(0L, partGen - aneutronic);
                    generatedEnergy += aneutronic * 20L;
                    if (sunny && level.canSeeSky(partPos.above(Math.max(1, cfg.heightBlocks())))) {
                        generatedEnergy += Math.round(solarOnly * solarFactor * 20.0);
                    }
                } else {
                    generatedEnergy += partGen * 20L;
                }
            }

            // Apply continuous environmental integrity wear on extreme planets (e.g., Venus H2SO4/92atm/735K)
            RocketFlightValidator.PartSnapshot partSnap = RocketFlightValidator.PartSnapshot.fromParts(
                    cfg, partBe.getStats(), partBe.getIntegrity(), comp
            );
            double wearPerSec = RocketFlightValidator.computeEnvironmentalWearPerSecond(partSnap, env);
            if (wearPerSec > 0.0) {
                partBe.damageIntegrity(wearPerSec);
                modified = true;
            }
        }

        if (generatedEnergy > 0L && this.rocketEnergy < summary.energyCapacity()) {
            long nextEnergy = Math.min(summary.energyCapacity(), this.rocketEnergy + generatedEnergy);
            if (nextEnergy != this.rocketEnergy) {
                this.rocketEnergy = nextEnergy;
                modified = true;
            }
        }

        // Cryogenic boil-off & active cryocooler electrical draw (1 second step = 20 ticks)
        if (this.rocketFuelDroplets > 0L && !this.rocketFuelVariant.isBlank()
                && PropellantType.isCryogenicFluid(this.rocketFuelVariant.getFluid())) {
            boolean cryocoolerPowered = false;
            if (hasCryocooler && !hasSuperfluidCryo2k) {
                long cryocoolerCostPerSec = RocketFlightValidator.CRYOCOOLER_DRAW_GJ_PER_TICK * 20L;
                if (this.rocketEnergy >= cryocoolerCostPerSec) {
                    this.rocketEnergy -= cryocoolerCostPerSec;
                    cryocoolerPowered = true;
                    modified = true;
                }
            }

            RocketFlightValidator.VehicleSnapshot snap = this.buildSnapshotFromEntities(
                    liveParts, this.rocketFuelDroplets, this.rocketEnergy
            );
            double boilPerMin = RocketFlightValidator.computeVehicleBoilOffDropletsPerMinute(
                    snap.parts(),
                    this.rocketFuelVariant.getFluid(),
                    PropellantType.fromFluid(this.rocketFuelVariant.getFluid()),
                    this.rocketFuelDroplets,
                    env.meanTempKelvin(),
                    hasCryocooler,
                    cryocoolerPowered,
                    hasSuperfluidCryo2k
            );
            if (boilPerMin > 0.0) {
                this.pendingBoilOffFraction += boilPerMin / 60.0;
                long wholeLoss = (long) Math.floor(this.pendingBoilOffFraction);
                if (wholeLoss > 0L) {
                    this.pendingBoilOffFraction -= wholeLoss;
                    this.consumeBoilOffDroplets(wholeLoss);
                    modified = true;
                }
            }
        }

        if (modified) {
            this.sync();
        }
    }

    private void consumeBoilOffDroplets(long wholeLoss) {
        if (wholeLoss <= 0L) {
            return;
        }
        if (this.stageFuelDroplets.isEmpty() && this.boosterFuelDroplets.isEmpty()) {
            this.rocketFuelDroplets = Math.max(0L, this.rocketFuelDroplets - wholeLoss);
            return;
        }
        long remaining = wholeLoss;
        for (Map.Entry<Integer, Long> entry : this.stageFuelDroplets.entrySet()) {
            if (remaining <= 0L) {
                break;
            }
            long avail = entry.getValue();
            if (avail > 0L) {
                long take = Math.min(avail, remaining);
                entry.setValue(avail - take);
                remaining -= take;
            }
        }
        for (Map.Entry<String, Long> entry : this.boosterFuelDroplets.entrySet()) {
            if (remaining <= 0L) {
                break;
            }
            long avail = entry.getValue();
            if (avail > 0L) {
                long take = Math.min(avail, remaining);
                entry.setValue(avail - take);
                remaining -= take;
            }
        }
        this.recomputeAggregateFuelAndGraph();
    }

    /**
     * Recompiles the cached {@link RocketAssemblyGraph} from mounted pad parts and refreshes the
     * per-stage {@link CombinedStorage} routing table.
     */
    public void rebuildAssemblyGraph() {
        if (this.level == null || !this.isRocketAssembled()) {
            this.assemblyGraph = RocketAssemblyGraph.EMPTY;
            this.rebuildFluidStorage();
            return;
        }
        RocketAssemblyGraphBuilder.BuildResult built = this.compileCurrentPadGraph();
        this.assemblyGraph = built.graph();
        // Clamp existing stage/booster fuel records to compiled capacities
        for (RocketStageNode stage : this.assemblyGraph.stages()) {
            long cur = Math.clamp(this.stageFuelDroplets.getOrDefault(stage.stageIndex(), 0L), 0L, stage.fuelCapacityDroplets());
            this.stageFuelDroplets.put(stage.stageIndex(), cur);
            for (RadialBoosterGroup group : stage.radialGroups()) {
                long gCur = Math.clamp(
                        this.boosterFuelDroplets.getOrDefault(group.groupId(), group.solidBooster() ? group.fuelCapacityDroplets() : 0L),
                        0L,
                        group.fuelCapacityDroplets()
                );
                this.boosterFuelDroplets.put(group.groupId(), gCur);
            }
        }
        this.recomputeAggregateFuelAndGraph();
        this.rebuildFluidStorage();
    }

    private void recomputeAggregateFuelAndGraph() {
        long sum = 0L;
        if (!this.assemblyGraph.isEmpty()) {
            List<RocketStageNode> updatedStages = new ArrayList<>(this.assemblyGraph.stages().size());
            for (RocketStageNode stage : this.assemblyGraph.stages()) {
                long sFuel = Math.clamp(this.stageFuelDroplets.getOrDefault(stage.stageIndex(), 0L), 0L, stage.fuelCapacityDroplets());
                sum += sFuel;
                List<RadialBoosterGroup> updatedGroups = new ArrayList<>(stage.radialGroups().size());
                for (RadialBoosterGroup group : stage.radialGroups()) {
                    long gFuel = Math.clamp(
                            this.boosterFuelDroplets.getOrDefault(group.groupId(), group.currentFuelDroplets()),
                            0L,
                            group.fuelCapacityDroplets()
                    );
                    sum += gFuel;
                    updatedGroups.add(group.withCurrentFuel(gFuel));
                }
                updatedStages.add(new RocketStageNode(
                        stage.stageIndex(),
                        stage.decouplerRelativePos(),
                        stage.hotStaging(),
                        stage.propellantId(),
                        stage.fuelCapacityDroplets(),
                        sFuel,
                        stage.coreParts(),
                        updatedGroups,
                        stage.decoupled()
                ));
            }
            this.assemblyGraph = new RocketAssemblyGraph(
                    updatedStages,
                    this.assemblyGraph.crewSeats(),
                    this.assemblyGraph.totalHeightBlocks(),
                    this.assemblyGraph.maxRadiusBlocks(),
                    this.assemblyGraph.hasLandingGear(),
                    this.assemblyGraph.hasLesAbort(),
                    this.assemblyGraph.hasFairing()
            );
            this.rocketFuelDroplets = sum;
        }
    }

    private void rebuildFluidStorage() {
        List<Storage<FluidVariant>> storages = new ArrayList<>();
        boolean addedStageTank = false;
        if (!this.assemblyGraph.isEmpty()) {
            for (RocketStageNode stage : this.assemblyGraph.stages()) {
                if (!stage.decoupled() && stage.fuelCapacityDroplets() > 0L) {
                    PropellantType prop = resolvePropellantById(stage.propellantId());
                    if (prop != PropellantType.SOLID) {
                        java.util.EnumSet<PropellantType> tankProps = java.util.EnumSet.noneOf(PropellantType.class);
                        for (var part : stage.coreParts()) {
                            if (part.stats().fuelCapacityDroplets() > 0L && part.config().category() == RocketPartCategory.TANK) {
                                tankProps.add(PropellantType.forTank(part.config()));
                            }
                        }
                        storages.add(new StagePropellantSlotStorage(stage.stageIndex(), prop, tankProps, stage.fuelCapacityDroplets()));
                        addedStageTank = true;
                    }
                }
                for (RadialBoosterGroup group : stage.radialGroups()) {
                    if (!group.decoupled() && !group.solidBooster() && group.fuelCapacityDroplets() > 0L) {
                        PropellantType gProp = resolvePropellantById(group.propellantId());
                        storages.add(new BoosterPropellantSlotStorage(group.groupId(), gProp, group.fuelCapacityDroplets()));
                        addedStageTank = true;
                    }
                }
            }
        }
        if (!addedStageTank) {
            storages.add(this.rocketFuelStorage);
        }
        storages.add(this.rocketOxygenStorage);
        this.rocketCombinedFluidStorage = new CombinedStorage<>(storages);
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
        this.rebuildAssemblyGraph();
    }

    /**
     * Computes progressive Survival assembly welding duration (in ticks) for a single modular part,
     * scaled monotonically by its dry mass and diameter complexity.
     */
    public static int computeStepAssemblyTicks(RocketPartConfig config, RocketPartStats stats) {
        double massComponent = stats != null ? (stats.dryWeightKg() / 600.0) : 0.0;
        int raw = 16 + (int) Math.round(massComponent) + Math.max(1, config.diameter()) * 2;
        return Math.clamp(raw, 16, 80);
    }

    private void tickProgressiveAssembly(Level level) {
        if (this.pendingAssemblySteps.isEmpty()) {
            return;
        }
        PendingAssemblyStep current = this.pendingAssemblySteps.get(0);
        this.assemblyStepElapsedTicks++;
        this.assemblyTotalElapsedTicks++;

        // Welding spark & smoke particles + subtle welding audio while gantry builds the current module
        if (level instanceof ServerLevel serverLevel) {
            if ((this.assemblyTotalElapsedTicks % 4) == 0) {
                BlockPos target = current.worldPos();
                double cx = target.getX() + 0.5;
                double cy = target.getY() + 0.6;
                double cz = target.getZ() + 0.5;
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 5, 0.45, 0.35, 0.45, 0.08);
                serverLevel.sendParticles(ParticleTypes.SMOKE, cx, cy + 0.2, cz, 2, 0.30, 0.25, 0.30, 0.02);
            }
            if ((this.assemblyTotalElapsedTicks % 12) == 1) {
                level.playSound(null, current.worldPos(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35F, 1.35F);
            }
        }

        if (this.assemblyStepElapsedTicks >= current.durationTicks()) {
            this.pendingAssemblySteps.remove(0);
            this.assemblyStepElapsedTicks = 0;
            this.assemblyCompletedSteps++;

            BlockPos placed = this.placePartStackAt(
                    current.worldPos(), current.facing(), current.partStack(),
                    current.visualOffsetX(), current.visualOffsetZ()
            );
            if (placed != null) {
                this.registerAssembledRocketParts(List.of(placed));
                level.playSound(null, placed, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.75F, 1.15F);
            } else {
                // Spot became obstructed mid-assembly: refund item safely instead of voiding
                Block.popResource(level, this.worldPosition.above(), current.partStack().copy());
            }

            if (this.pendingAssemblySteps.isEmpty()) {
                this.assemblyTotalElapsedTicks = this.assemblyTotalDurationTicks;
                this.assemblyTotalSteps = 0;
                this.assemblyCompletedSteps = 0;
                this.assemblyTotalDurationTicks = 0;
                this.assemblyTotalElapsedTicks = 0;
                level.playSound(null, this.getPadCenterPos().above(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.85F, 1.15F);
            }
            this.sync();
        } else if ((this.assemblyTotalElapsedTicks % 5) == 0) {
            this.sync();
        }
    }

    /**
     * Cancels any active progressive Survival assembly and refunds all unplaced part stacks either
     * directly to {@code refundTarget} or onto the ground above the terminal.
     */
    public boolean cancelProgressiveAssembly(@Nullable Player refundTarget) {
        if (this.level == null || this.level.isClientSide || this.pendingAssemblySteps.isEmpty()) {
            return false;
        }
        List<PendingAssemblyStep> remaining = new ArrayList<>(this.pendingAssemblySteps);
        this.pendingAssemblySteps.clear();
        this.assemblyStepElapsedTicks = 0;
        this.assemblyTotalElapsedTicks = 0;
        this.assemblyTotalDurationTicks = 0;
        this.assemblyCompletedSteps = 0;
        this.assemblyTotalSteps = 0;

        for (PendingAssemblyStep step : remaining) {
            ItemStack refund = step.partStack().copy();
            if (refund.isEmpty()) {
                continue;
            }
            if (refundTarget != null) {
                if (!refundTarget.getInventory().add(refund)) {
                    refundTarget.drop(refund, false);
                }
            } else {
                Block.popResource(this.level, this.worldPosition.above(), refund);
            }
        }
        this.sync();
        return true;
    }

    private @Nullable BlockPos placePartStackAt(
            BlockPos origin,
            Direction facing,
            ItemStack stack,
            float visualOffsetX,
            float visualOffsetZ
    ) {
        if (this.level == null || stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
            return null;
        }
        RocketPartConfig cfg = partItem.getConfig(stack);
        if (origin.getY() + Math.max(1, cfg.heightBlocks()) > this.level.getMaxBuildHeight()) {
            return null;
        }
        if (!ModularRocketPartBlockItem.canPlaceFootprint(this.level, origin, cfg, facing)) {
            return null;
        }
        Block partBlock = ModularRocketPartBlock.getBlockForCategory(cfg.category());
        BlockState masterState = partBlock.defaultBlockState()
                .setValue(ModularRocketPartBlock.FACING, facing)
                .setValue(ModularRocketPartBlock.HATCH_OPEN, false);
        this.level.setBlock(origin, masterState, Block.UPDATE_ALL);

        if (this.level.getBlockEntity(origin) instanceof ModularRocketPartBlockEntity partBe) {
            RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
            PartIntegrity integrity = stack.get(GCDataComponents.PART_INTEGRITY);
            RocketPartComposition composition = stack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
            partBe.initializePart(cfg, stats, integrity, composition, facing);
            if (Math.abs(visualOffsetX) > 1.0e-4F || Math.abs(visualOffsetZ) > 1.0e-4F) {
                partBe.setRadialVisualOffset(visualOffsetX, visualOffsetZ);
            }
        }
        return origin.immutable();
    }

    private void enqueueProgressiveSteps(List<PendingAssemblyStep> steps) {
        if (steps.isEmpty()) {
            return;
        }
        this.pendingAssemblySteps.addAll(steps);
        int extraDuration = 0;
        for (PendingAssemblyStep step : steps) {
            extraDuration += step.durationTicks();
        }
        this.assemblyTotalDurationTicks += extraDuration;
        this.assemblyTotalSteps += steps.size();
        this.sync();
    }

    /**
     * Assembles an arbitrarily tall modular rocket coaxially above this formed launch pad
     * directly from the specified player inventory slots (ordered bottom-to-top) with no fixed slot cap.
     *
     * <p>Creative-mode players receive an instant bypass; Survival-mode players initiate progressive
     * gantry welding ({@code T_assembly}) with live spark particles and progress tracking.
     *
     * @return {@code true} if at least one rocket part was assembled or queued onto the pad.
     */
    public boolean buildRocketFromPlayerInventory(ServerPlayer player, List<Integer> inventorySlots) {
        if (this.level == null || this.level.isClientSide || !this.formed || this.tearingDown
                || player == null || inventorySlots == null || inventorySlots.isEmpty()) {
            return false;
        }

        Inventory inv = player.getInventory();
        boolean creative = isCreativeBypass(player);

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
        // Account for any already-queued progressive coaxial steps
        for (PendingAssemblyStep step : this.pendingAssemblySteps) {
            if (step.worldPos().getX() == padCenter.getX() && step.worldPos().getZ() == padCenter.getZ()
                    && step.partStack().getItem() instanceof ModularRocketPartBlockItem partItem) {
                RocketPartConfig queuedCfg = partItem.getConfig(step.partStack());
                int topY = step.worldPos().getY() + Math.max(1, queuedCfg.heightBlocks());
                if (topY > currentY) {
                    currentY = topY;
                }
            }
        }

        // Pre-validate every footprint atomically
        List<PendingAssemblyStep> steps = new ArrayList<>(inventorySlots.size());
        Set<Long> reservedVoxels = new HashSet<>();
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
                if (reservedVoxels.contains(wp.asLong())) {
                    clear = false;
                    break;
                }
                BlockState st = this.level.getBlockState(wp);
                if (!st.isAir() && !st.canBeReplaced()) {
                    clear = false;
                    break;
                }
            }
            if (!clear) {
                break;
            }
            for (BlockPos localPos : localFootprint.keySet()) {
                reservedVoxels.add(FootprintMode.toWorldPos(origin, facing, localPos).asLong());
            }
            RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
            if (stats == null) {
                stats = RocketPartCostCalculator.calculate(cfg, stack.get(GCDataComponents.ROCKET_PART_COMPOSITION)).stats();
            }
            int durationTicks = computeStepAssemblyTicks(cfg, stats);
            steps.add(new PendingAssemblyStep(origin, facing, stack.copyWithCount(1), durationTicks));
            currentY += Math.max(1, cfg.heightBlocks());
        }

        if (steps.isEmpty()) {
            return false;
        }

        // Consume player items atomically in Survival
        if (!creative) {
            int consumed = 0;
            for (int slotIdx : inventorySlots) {
                if (consumed >= steps.size()) {
                    break;
                }
                inv.getItem(slotIdx).shrink(1);
                consumed++;
            }
            player.inventoryMenu.broadcastChanges();
            this.enqueueProgressiveSteps(steps);
            return true;
        }

        // Creative instant bypass: place all parts immediately in 1 tick
        List<BlockPos> builtPositions = new ArrayList<>(steps.size());
        for (PendingAssemblyStep step : steps) {
            BlockPos placed = this.placePartStackAt(
                    step.worldPos(), step.facing(), step.partStack(),
                    step.visualOffsetX(), step.visualOffsetZ()
            );
            if (placed != null) {
                builtPositions.add(placed);
            }
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
     * Attaches a symmetric ring ({@code 2x / 4x / 4x_diag / 8x}) of radial boosters from the player's
     * inventory around the mounted rocket core with atomic clearance and mass-limit validation.
     */
    public boolean attachRadialBoostersFromInventory(
            ServerPlayer player,
            List<Integer> inventorySlots,
            RadialSymmetry symmetry,
            CrossfeedMode crossfeedMode
    ) {
        if (this.level == null || this.level.isClientSide || !this.formed || this.tearingDown
                || player == null || inventorySlots == null || inventorySlots.isEmpty()
                || symmetry == null || symmetry.count() < 2) {
            return false;
        }
        Inventory inv = player.getInventory();
        boolean creative = isCreativeBypass(player);
        int mult = symmetry.count();

        Map<Integer, Integer> requiredPerSlot = new LinkedHashMap<>();
        List<ItemStack> template = new ArrayList<>(inventorySlots.size());
        for (int slotIdx : inventorySlots) {
            if (slotIdx < 0 || slotIdx >= inv.getContainerSize()) {
                return false;
            }
            ItemStack stack = inv.getItem(slotIdx);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem)) {
                return false;
            }
            requiredPerSlot.merge(slotIdx, mult, Integer::sum);
            template.add(stack.copyWithCount(1));
        }
        if (!creative) {
            for (Map.Entry<Integer, Integer> entry : requiredPerSlot.entrySet()) {
                if (inv.getItem(entry.getKey()).getCount() < entry.getValue()) {
                    player.displayClientMessage(
                            Component.literal("[VAB] Itens insuficientes no inventário para simetria " + symmetry.count() + "x."),
                            false
                    );
                    return false;
                }
            }
        }

        boolean ok = this.attachRadialBoosters(player, template, symmetry, crossfeedMode, creative);
        if (ok && !creative) {
            for (Map.Entry<Integer, Integer> entry : requiredPerSlot.entrySet()) {
                inv.getItem(entry.getKey()).shrink(entry.getValue());
            }
            player.inventoryMenu.broadcastChanges();
        }
        return ok;
    }

    /**
     * Atomically validates and attaches a symmetric ring ({@code 2x / 4x / 4x_diag / 8x}) of radial
     * boosters around the core rocket standing on this launch pad.
     *
     * <p>If ANY mirrored booster position is obstructed, outside hangar clearance bounds, or would
     * exceed the pad's {@code M_max} mass limit, zero blocks are placed and {@code false} is returned.
     */
    public boolean attachRadialBoosters(
            @Nullable ServerPlayer player,
            List<ItemStack> boosterColumnTemplate,
            RadialSymmetry symmetry,
            CrossfeedMode crossfeedMode,
            boolean instant
    ) {
        if (this.level == null || this.level.isClientSide || !this.formed || this.tearingDown
                || boosterColumnTemplate == null || boosterColumnTemplate.isEmpty()
                || symmetry == null || symmetry.count() < 2) {
            return false;
        }

        List<ModularRocketPartBlockEntity> existingEntities = this.collectPadPartEntities();
        if (existingEntities.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.literal("[VAB] Construa o foguete central no pad antes de anexar boosters."), false);
            }
            return false;
        }

        BlockPos padCenter = this.getPadCenterPos();
        Direction facing = this.getFacing();
        PlatformBlueprint blueprint = this.getActiveBlueprint();

        int boosterMaxDiam = 1;
        int boosterTotalHeight = 0;
        for (ItemStack stack : boosterColumnTemplate) {
            if (stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
                return false;
            }
            RocketPartConfig cfg = partItem.getConfig(stack);
            boosterMaxDiam = Math.max(boosterMaxDiam, Math.max(cfg.diameter(), cfg.topDiameter()));
            boosterTotalHeight += Math.max(1, cfg.heightBlocks());
        }

        // Determine core base Y and maximum core diameter spanning the booster column height
        int lowestCoreY = Integer.MAX_VALUE;
        for (ModularRocketPartBlockEntity be : existingEntities) {
            BlockPos rel = be.getBlockPos().subtract(padCenter);
            if (rel.getX() == 0 && rel.getZ() == 0 && !be.getConfig().radial() && rel.getY() < lowestCoreY) {
                lowestCoreY = rel.getY();
            }
        }
        if (lowestCoreY == Integer.MAX_VALUE) {
            if (player != null) {
                player.displayClientMessage(Component.literal("[VAB] Nenhum núcleo coaxial encontrado no pad."), false);
            }
            return false;
        }

        int coreSpanMaxDiameter = 1;
        int boosterTopRelY = lowestCoreY + Math.max(1, boosterTotalHeight);
        for (ModularRocketPartBlockEntity be : existingEntities) {
            BlockPos rel = be.getBlockPos().subtract(padCenter);
            if (rel.getX() == 0 && rel.getZ() == 0 && !be.getConfig().radial()) {
                int partMinY = rel.getY();
                int partMaxY = partMinY + Math.max(1, be.getConfig().heightBlocks());
                if (partMinY < boosterTopRelY && partMaxY > lowestCoreY) {
                    coreSpanMaxDiameter = Math.max(coreSpanMaxDiameter, Math.max(be.getConfig().diameter(), be.getConfig().topDiameter()));
                }
            }
        }

        BlockPos coreBaseWorld = new BlockPos(padCenter.getX(), padCenter.getY() + lowestCoreY, padCenter.getZ());
        List<BlockPos> boosterBases = RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(
                coreBaseWorld, coreSpanMaxDiameter, boosterMaxDiam, symmetry
        );

        // Atomic validation pass: square pad clearance, non-overlapping footprints, world obstructions, and pad mass limit
        double allowedClearanceRadius = RocketAssemblyGraphBuilder.computeAllowedClearanceRadius(this.platformSize, blueprint);
        List<PendingAssemblyStep> steps = new ArrayList<>(boosterBases.size() * boosterColumnTemplate.size());
        Set<Long> reservedVoxels = new HashSet<>();
        List<RocketAssemblyGraphBuilder.PartInput> simulatedInputs = new ArrayList<>(existingEntities.size() + boosterBases.size() * boosterColumnTemplate.size());
        for (ModularRocketPartBlockEntity existing : existingEntities) {
            simulatedInputs.add(RocketAssemblyGraphBuilder.PartInput.fromBlockEntity(padCenter, existing));
        }

        for (BlockPos basePos : boosterBases) {
            int relX = basePos.getX() - padCenter.getX();
            int relZ = basePos.getZ() - padCenter.getZ();
            if (!RocketAssemblyGraphBuilder.fitsWithinPadClearance(relX, relZ, boosterMaxDiam, allowedClearanceRadius)) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("[VAB] Boosters excedem a folga da plataforma " + this.platformSize.getSerializedName() + "."), false);
                }
                return false;
            }
            double[] flushPos = RocketAssemblyGraphBuilder.computeFlushRadialVisualPosition(
                    coreSpanMaxDiameter, boosterMaxDiam, relX, relZ, symmetry.count()
            );
            float visOffX = (float) (flushPos[0] - relX);
            float visOffZ = (float) (flushPos[1] - relZ);

            int curY = basePos.getY();
            for (ItemStack stack : boosterColumnTemplate) {
                ModularRocketPartBlockItem partItem = (ModularRocketPartBlockItem) stack.getItem();
                RocketPartConfig cfg = partItem.getConfig(stack);
                BlockPos origin = new BlockPos(basePos.getX(), curY, basePos.getZ());
                if (origin.getY() + Math.max(1, cfg.heightBlocks()) > this.level.getMaxBuildHeight()) {
                    return false;
                }
                Map<BlockPos, dev.galacticraft.api.multiblock.BoundsShape> localFootprint = FootprintMode.computeLocalFootprint(cfg, false);
                for (BlockPos localPos : localFootprint.keySet()) {
                    BlockPos wp = FootprintMode.toWorldPos(origin, facing, localPos);
                    if (!reservedVoxels.add(wp.asLong())) {
                        if (player != null) {
                            player.displayClientMessage(Component.literal("[VAB] Os boosters sobrepõem-se com esta simetria (" + symmetry.getSerializedName() + ")."), false);
                        }
                        return false;
                    }
                    BlockState existingState = this.level.getBlockState(wp);
                    if (!existingState.isAir() && !existingState.canBeReplaced()) {
                        if (player != null) {
                            player.displayClientMessage(Component.literal("[VAB] Posição de booster obstruída em (" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ() + ")."), false);
                        }
                        return false;
                    }
                }
                RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
                if (stats == null) {
                    stats = RocketPartCostCalculator.calculate(cfg, stack.get(GCDataComponents.ROCKET_PART_COMPOSITION)).stats();
                }
                int stepTicks = computeStepAssemblyTicks(cfg, stats);
                steps.add(new PendingAssemblyStep(origin, facing, stack.copyWithCount(1), stepTicks, visOffX, visOffZ));
                RocketAssemblyGraphBuilder.PartInput simInput = RocketAssemblyGraphBuilder.PartInput.fromStack(origin.subtract(padCenter), stack);
                if (simInput != null) {
                    simulatedInputs.add(simInput);
                }
                curY += Math.max(1, cfg.heightBlocks());
            }
        }

        // Verify simulated combined vehicle does not violate pad mass limit M_max
        CrossfeedMode effectiveCrossfeed = crossfeedMode != null ? crossfeedMode : this.selectedCrossfeed;
        RocketAssemblyGraphBuilder.BuildResult simBuild = RocketAssemblyGraphBuilder.build(
                simulatedInputs,
                this.platformSize,
                blueprint,
                this.stageFuelDroplets,
                this.boosterFuelDroplets,
                effectiveCrossfeed
        );
        if (simBuild.hasIssue(RocketAssemblyGraphBuilder.IssueCode.PAD_OVERWEIGHT)) {
            if (player != null) {
                player.displayClientMessage(Component.literal("[VAB] Massa total com boosters excede o limite do pad."), false);
            }
            return false;
        }

        this.selectedSymmetry = symmetry;
        this.selectedCrossfeed = effectiveCrossfeed;

        if (!instant) {
            this.enqueueProgressiveSteps(steps);
            if (player != null) {
                player.displayClientMessage(Component.literal("[VAB] Iniciando soldagem de boosters " + symmetry.getSerializedName() + "..."), false);
            }
            return true;
        }

        List<BlockPos> placedPositions = new ArrayList<>(steps.size());
        for (PendingAssemblyStep step : steps) {
            BlockPos placed = this.placePartStackAt(
                    step.worldPos(), step.facing(), step.partStack(),
                    step.visualOffsetX(), step.visualOffsetZ()
            );
            if (placed != null) {
                placedPositions.add(placed);
            }
        }
        if (!placedPositions.isEmpty()) {
            boolean coreAlreadyFueled = this.rocketFuelDroplets > 0L;
            this.registerAssembledRocketParts(placedPositions);
            // In creative instant mode, if the core rocket was already fueled, auto-fill the newly attached boosters too
            if (coreAlreadyFueled && !this.assemblyGraph.isEmpty()) {
                for (RocketStageNode stage : this.assemblyGraph.stages()) {
                    for (RadialBoosterGroup group : stage.radialGroups()) {
                        if (!group.decoupled() && group.fuelCapacityDroplets() > 0L && this.boosterFuelDroplets.getOrDefault(group.groupId(), 0L) <= 0L) {
                            this.boosterFuelDroplets.put(group.groupId(), group.fuelCapacityDroplets());
                            this.boosterFuelVariants.put(group.groupId(), defaultFluidForPropellant(resolvePropellantById(group.propellantId())));
                        }
                    }
                }
                this.recomputeAggregateFuelAndGraph();
            }
            this.level.playSound(null, padCenter.above(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.9F, 1.1F);
            if (player != null) {
                player.displayClientMessage(Component.literal("[VAB] Boosters " + symmetry.getSerializedName() + " anexados com sucesso!"), false);
            }
            this.sync();
            return true;
        }
        return false;
    }

    /**
     * Removes ONLY the off-axis/radial booster parts currently mounted on this pad while keeping
     * the coaxial core rocket standing untouched. In Survival, removed boosters are refunded to {@code player}.
     */
    public boolean removeRadialBoosters(@Nullable ServerPlayer player) {
        if (this.level == null || this.level.isClientSide || !this.formed || this.tearingDown || this.tearingDownRocket
                || this.isAssemblyInProgress()) {
            return false;
        }
        BlockPos padCenter = this.getPadCenterPos();
        List<ModularRocketPartBlockEntity> allParts = this.collectPadPartEntities();
        List<ModularRocketPartBlockEntity> radialParts = new ArrayList<>();
        for (ModularRocketPartBlockEntity be : allParts) {
            BlockPos rel = be.getBlockPos().subtract(padCenter);
            if (rel.getX() != 0 || rel.getZ() != 0 || be.getConfig().radial()) {
                radialParts.add(be);
            }
        }
        if (radialParts.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.literal("[VAB] Nenhum booster radial montado no pad."), false);
            }
            return false;
        }

        boolean creative = isCreativeBypass(player);
        this.tearingDownRocket = true;
        int removed = 0;
        try {
            for (ModularRocketPartBlockEntity partBe : radialParts) {
                BlockPos partPos = partBe.getBlockPos().immutable();
                ItemStack refund = partBe.createConfiguredStack();
                partBe.clearAssembledTerminalPosQuiet();
                partBe.tearDown(this.level, null, player, false);
                this.assembledRocketParts.remove(partPos);
                if (!creative && !refund.isEmpty()) {
                    if (player != null) {
                        if (!player.getInventory().add(refund)) {
                            player.drop(refund, false);
                        }
                    } else {
                        Block.popResource(this.level, this.worldPosition.above(), refund);
                    }
                }
                removed++;
            }
            this.boosterFuelDroplets.clear();
            this.boosterFuelVariants.clear();
            if (this.assembledRocketParts.isEmpty()) {
                this.rocketAssembled = false;
                this.rocketFuelDroplets = 0L;
                this.rocketEnergy = 0L;
                this.rocketOxygenDroplets = 0L;
            }
        } finally {
            this.tearingDownRocket = false;
        }

        if (removed > 0) {
            this.rebuildAssemblyGraph();
            this.level.playSound(null, padCenter.above(), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.85F, 1.05F);
            if (player != null) {
                player.displayClientMessage(Component.literal("[VAB] Boosters radiais removidos (" + removed + " módulos)."), false);
            }
            this.sync();
            return true;
        }
        return false;
    }

    /**
     * Applies an in-place VAB core edit (reorder, remove, or insert core modules) directly on the pad
     * without requiring full rocket disassembly. Preserves existing mounted core parts (and their integrity,
     * composition, and stats) as well as any attached radial boosters, refunding removed core modules and
     * consuming only newly added inventory items in Survival.
     */
    public boolean applyEditedRocketCore(
            @Nullable ServerPlayer player,
            List<dev.galacticraft.mod.network.c2s.EditRocketCorePayload.CoreEntry> entries
    ) {
        if (this.level == null || this.level.isClientSide || !this.formed || this.tearingDown || this.tearingDownRocket
                || this.isAssemblyInProgress() || entries == null) {
            return false;
        }

        if (entries.isEmpty()) {
            boolean recalled = this.recallRocketToSlots(player);
            if (recalled && player != null) {
                player.displayClientMessage(Component.literal("[VAB] Foguete recolhido do pad."), false);
            }
            return recalled;
        }

        BlockPos padCenter = this.getPadCenterPos();
        Direction facing = this.getFacing();
        boolean creative = isCreativeBypass(player);

        // Partition currently mounted parts into coaxial core vs radial boosters
        List<ModularRocketPartBlockEntity> allMounted = this.collectPadPartEntities();
        Map<BlockPos, ModularRocketPartBlockEntity> existingCoreByPos = new LinkedHashMap<>();
        List<ModularRocketPartBlockEntity> existingCoreOrdered = new ArrayList<>();
        List<BlockPos> survivingRadialPositions = new ArrayList<>();
        for (ModularRocketPartBlockEntity be : allMounted) {
            BlockPos rel = be.getBlockPos().subtract(padCenter);
            if (rel.getX() == 0 && rel.getZ() == 0 && !be.getConfig().radial()) {
                existingCoreByPos.put(be.getBlockPos().immutable(), be);
                existingCoreOrdered.add(be);
            } else {
                survivingRadialPositions.add(be.getBlockPos().immutable());
            }
        }
        existingCoreOrdered.sort(Comparator.comparingInt(be -> be.getBlockPos().getY()));

        // Resolve and validate every requested entry atomically (preventing any duplicate pad references)
        Set<BlockPos> keptOldCorePositions = new HashSet<>();
        Map<Integer, Integer> requiredInvCounts = new LinkedHashMap<>();
        List<ItemStack> resolvedStacks = new ArrayList<>(entries.size());
        List<BlockPos> sourcePadPositions = new ArrayList<>(entries.size());

        Inventory inv = player != null ? player.getInventory() : null;
        for (var entry : entries) {
            if (entry.fromPad()) {
                BlockPos refPos = entry.padPos().immutable();
                ModularRocketPartBlockEntity existingBe = existingCoreByPos.get(refPos);
                if (existingBe == null || existingBe.isRemoved() || !keptOldCorePositions.add(refPos)) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("[VAB] Peça montada inválida ou duplicada na edição."), false);
                    }
                    return false;
                }
                resolvedStacks.add(existingBe.createConfiguredStack());
                sourcePadPositions.add(refPos);
            } else {
                if (inv == null || entry.inventorySlot() < 0 || entry.inventorySlot() >= inv.getContainerSize()) {
                    return false;
                }
                ItemStack invStack = inv.getItem(entry.inventorySlot());
                if (invStack.isEmpty() || !(invStack.getItem() instanceof ModularRocketPartBlockItem)) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("[VAB] Peça do inventário não encontrada para aplicar edição."), false);
                    }
                    return false;
                }
                requiredInvCounts.merge(entry.inventorySlot(), 1, Integer::sum);
                resolvedStacks.add(invStack.copyWithCount(1));
                sourcePadPositions.add(null);
            }
        }

        if (!creative && inv != null) {
            for (Map.Entry<Integer, Integer> req : requiredInvCounts.entrySet()) {
                if (inv.getItem(req.getKey()).getCount() < req.getValue()) {
                    player.displayClientMessage(Component.literal("[VAB] Itens insuficientes no inventário para aplicar a edição."), false);
                    return false;
                }
            }
        }

        // Collect all block voxels currently occupied by old core parts (ignorable during clearance check)
        Set<Long> ignorableOldCoreVoxels = new HashSet<>();
        for (ModularRocketPartBlockEntity oldBe : existingCoreOrdered) {
            Map<BlockPos, dev.galacticraft.api.multiblock.BoundsShape> fp = FootprintMode.computeLocalFootprint(oldBe.getConfig(), false);
            for (BlockPos local : fp.keySet()) {
                ignorableOldCoreVoxels.add(FootprintMode.toWorldPos(oldBe.getBlockPos(), oldBe.getFacing(), local).asLong());
            }
        }

        // Pre-validate every new core part's footprint against world blocks and surviving radial boosters
        List<PendingAssemblyStep> newSteps = new ArrayList<>(resolvedStacks.size());
        Set<Long> reservedNewVoxels = new HashSet<>();
        int curY = padCenter.getY() + 1;
        for (ItemStack stack : resolvedStacks) {
            if (!(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
                return false;
            }
            RocketPartConfig cfg = partItem.getConfig(stack);
            BlockPos origin = new BlockPos(padCenter.getX(), curY, padCenter.getZ());
            if (origin.getY() + Math.max(1, cfg.heightBlocks()) > this.level.getMaxBuildHeight()) {
                if (player != null) {
                    player.displayClientMessage(Component.literal("[VAB] Edição excede o limite de altura do mundo."), false);
                }
                return false;
            }
            Map<BlockPos, dev.galacticraft.api.multiblock.BoundsShape> fp = FootprintMode.computeLocalFootprint(cfg, false);
            for (BlockPos local : fp.keySet()) {
                BlockPos wp = FootprintMode.toWorldPos(origin, facing, local);
                if (!reservedNewVoxels.add(wp.asLong())) {
                    return false;
                }
                if (!ignorableOldCoreVoxels.contains(wp.asLong())) {
                    BlockState existingState = this.level.getBlockState(wp);
                    if (!existingState.isAir() && !existingState.canBeReplaced()) {
                        if (player != null) {
                            player.displayClientMessage(Component.literal(
                                    "[VAB] Edição bloqueada em (" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ()
                                            + ") — remova boosters ou ajuste o diâmetro."
                            ), false);
                        }
                        return false;
                    }
                }
            }
            RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
            if (stats == null) {
                stats = RocketPartCostCalculator.calculate(cfg, stack.get(GCDataComponents.ROCKET_PART_COMPOSITION)).stats();
            }
            newSteps.add(new PendingAssemblyStep(origin, facing, stack.copyWithCount(1), computeStepAssemblyTicks(cfg, stats)));
            curY += Math.max(1, cfg.heightBlocks());
        }

        // Snapshot removed old core items before teardown so Survival refunds them cleanly
        List<ItemStack> removedRefunds = new ArrayList<>();
        for (ModularRocketPartBlockEntity oldBe : existingCoreOrdered) {
            if (!keptOldCorePositions.contains(oldBe.getBlockPos())) {
                removedRefunds.add(oldBe.createConfiguredStack());
            }
        }

        // Compute unchanged bottom prefix count so Survival only welds changed/newly added upper modules
        int unchangedPrefix = 0;
        if (!creative) {
            int maxPrefix = Math.min(existingCoreOrdered.size(), newSteps.size());
            for (int i = 0; i < maxPrefix; i++) {
                BlockPos srcPos = sourcePadPositions.get(i);
                if (srcPos != null && srcPos.equals(existingCoreOrdered.get(i).getBlockPos())
                        && newSteps.get(i).worldPos().equals(srcPos)) {
                    unchangedPrefix++;
                } else {
                    break;
                }
            }
        }

        // Consume newly added inventory parts atomically in Survival
        if (!creative && inv != null && !requiredInvCounts.isEmpty()) {
            for (Map.Entry<Integer, Integer> req : requiredInvCounts.entrySet()) {
                inv.getItem(req.getKey()).shrink(req.getValue());
            }
            player.inventoryMenu.broadcastChanges();
        }

        // Tear down old core parts quietly (never touching surviving radial boosters)
        this.tearingDownRocket = true;
        try {
            for (ModularRocketPartBlockEntity oldBe : existingCoreOrdered) {
                BlockPos oldPos = oldBe.getBlockPos().immutable();
                oldBe.clearAssembledTerminalPosQuiet();
                oldBe.tearDown(this.level, null, player, false);
                this.assembledRocketParts.remove(oldPos);
            }
        } finally {
            this.tearingDownRocket = false;
        }

        // Refund removed core modules in Survival
        if (!creative && !removedRefunds.isEmpty()) {
            for (ItemStack refund : removedRefunds) {
                if (refund.isEmpty()) {
                    continue;
                }
                if (player != null) {
                    if (!player.getInventory().add(refund)) {
                        player.drop(refund, false);
                    }
                } else {
                    Block.popResource(this.level, this.worldPosition.above(), refund);
                }
            }
            if (player != null) {
                player.inventoryMenu.broadcastChanges();
            }
        }

        // Place instant steps (all steps in Creative, or unchanged prefix + pure removals/reorders with no new items? -> progressive for changed suffix in Survival)
        int immediateCount = creative ? newSteps.size() : (requiredInvCounts.isEmpty() ? newSteps.size() : unchangedPrefix);
        List<BlockPos> placedPositions = new ArrayList<>(immediateCount + survivingRadialPositions.size());
        for (int i = 0; i < immediateCount; i++) {
            PendingAssemblyStep step = newSteps.get(i);
            BlockPos placed = this.placePartStackAt(
                    step.worldPos(), step.facing(), step.partStack(),
                    step.visualOffsetX(), step.visualOffsetZ()
            );
            if (placed != null) {
                placedPositions.add(placed);
            }
        }
        placedPositions.addAll(survivingRadialPositions);
        if (!placedPositions.isEmpty()) {
            this.registerAssembledRocketParts(placedPositions);
        } else {
            this.rocketAssembled = false;
            this.rebuildAssemblyGraph();
        }

        if (immediateCount < newSteps.size()) {
            this.enqueueProgressiveSteps(newSteps.subList(immediateCount, newSteps.size()));
            if (player != null) {
                player.displayClientMessage(Component.literal("[VAB] Soldando módulos editados no pad..."), false);
            }
        } else {
            this.level.playSound(null, padCenter.above(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.9F, 1.15F);
            if (player != null) {
                player.displayClientMessage(Component.literal("[VAB] Foguete atualizado no pad!"), false);
            }
            this.sync();
        }
        return true;
    }

    public record PendingAssemblyStep(
            BlockPos worldPos,
            Direction facing,
            ItemStack partStack,
            int durationTicks,
            float visualOffsetX,
            float visualOffsetZ
    ) {
        public PendingAssemblyStep(BlockPos worldPos, Direction facing, ItemStack partStack, int durationTicks) {
            this(worldPos, facing, partStack, durationTicks, 0.0F, 0.0F);
        }
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
                var composition = stack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
                partBe.initializePart(cfg, stats, integrity, composition, facing);
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
            this.cancelProgressiveAssembly(null);
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
        if (!this.rocketFuelVariant.isBlank()) {
            tag.putString(TAG_ROCKET_FUEL_FLUID, BuiltInRegistries.FLUID.getKey(this.rocketFuelVariant.getFluid()).toString());
        }
        tag.putLong(TAG_ROCKET_ENERGY, this.rocketEnergy);
        tag.putLong(TAG_ROCKET_OXYGEN, this.rocketOxygenDroplets);
        tag.putString(TAG_SELECTED_SYMMETRY, this.selectedSymmetry.getSerializedName());
        tag.putString(TAG_SELECTED_CROSSFEED, this.selectedCrossfeed.getSerializedName());

        if (!this.assemblyGraph.isEmpty()) {
            RocketAssemblyGraph.CODEC.encodeStart(NbtOps.INSTANCE, this.assemblyGraph)
                    .result()
                    .ifPresent(encoded -> tag.put(TAG_ASSEMBLY_GRAPH, encoded));
        }
        if (!this.stageFuelDroplets.isEmpty()) {
            ListTag stageFuelList = new ListTag();
            for (Map.Entry<Integer, Long> entry : this.stageFuelDroplets.entrySet()) {
                CompoundTag st = new CompoundTag();
                st.putInt("Stage", entry.getKey());
                st.putLong("Amount", entry.getValue());
                FluidVariant fv = this.stageFuelVariants.get(entry.getKey());
                if (fv != null && !fv.isBlank()) {
                    st.putString("Fluid", BuiltInRegistries.FLUID.getKey(fv.getFluid()).toString());
                }
                stageFuelList.add(st);
            }
            tag.put(TAG_STAGE_FUELS, stageFuelList);
        }
        if (!this.boosterFuelDroplets.isEmpty()) {
            ListTag boosterFuelList = new ListTag();
            for (Map.Entry<String, Long> entry : this.boosterFuelDroplets.entrySet()) {
                CompoundTag bt = new CompoundTag();
                bt.putString("Group", entry.getKey());
                bt.putLong("Amount", entry.getValue());
                FluidVariant fv = this.boosterFuelVariants.get(entry.getKey());
                if (fv != null && !fv.isBlank()) {
                    bt.putString("Fluid", BuiltInRegistries.FLUID.getKey(fv.getFluid()).toString());
                }
                boosterFuelList.add(bt);
            }
            tag.put(TAG_BOOSTER_FUELS, boosterFuelList);
        }
        if (!this.pendingAssemblySteps.isEmpty()) {
            ListTag pendingList = new ListTag();
            for (PendingAssemblyStep step : this.pendingAssemblySteps) {
                CompoundTag pt = new CompoundTag();
                pt.putLong("Pos", step.worldPos().asLong());
                pt.putString("Facing", step.facing().getSerializedName());
                pt.put("Item", step.partStack().save(registries));
                pt.putInt("Duration", step.durationTicks());
                if (Math.abs(step.visualOffsetX()) > 1.0e-4F) {
                    pt.putFloat("VisOffX", step.visualOffsetX());
                }
                if (Math.abs(step.visualOffsetZ()) > 1.0e-4F) {
                    pt.putFloat("VisOffZ", step.visualOffsetZ());
                }
                pendingList.add(pt);
            }
            tag.put(TAG_PENDING_ASSEMBLY, pendingList);
        }
        tag.putInt(TAG_ASSEMBLY_STEP_ELAPSED, this.assemblyStepElapsedTicks);
        tag.putInt(TAG_ASSEMBLY_TOTAL_ELAPSED, this.assemblyTotalElapsedTicks);
        tag.putInt(TAG_ASSEMBLY_TOTAL_DURATION, this.assemblyTotalDurationTicks);
        tag.putInt(TAG_ASSEMBLY_COMPLETED_STEPS, this.assemblyCompletedSteps);
        tag.putInt(TAG_ASSEMBLY_TOTAL_STEPS, this.assemblyTotalSteps);
        tag.putString(TAG_LAUNCH_PHASE, this.launchPhase.name());
        tag.putInt(TAG_LAUNCH_COUNTDOWN_TICKS, this.launchCountdownTicks);
        if (!this.lastLaunchAbortReason.isEmpty()) {
            tag.putString(TAG_LAST_ABORT_REASON, this.lastLaunchAbortReason);
        }
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
        if (tag.contains(TAG_ROCKET_FUEL_FLUID, Tag.TAG_STRING)) {
            net.minecraft.resources.ResourceLocation fluidId = net.minecraft.resources.ResourceLocation.tryParse(tag.getString(TAG_ROCKET_FUEL_FLUID));
            if (fluidId != null && BuiltInRegistries.FLUID.containsKey(fluidId)) {
                net.minecraft.world.level.material.Fluid loaded = BuiltInRegistries.FLUID.get(fluidId);
                if (PropellantType.isRocketPropellantFluid(loaded)) {
                    this.rocketFuelVariant = FluidVariant.of(loaded);
                }
            }
        } else {
            this.rocketFuelVariant = FluidVariant.of(GCFluids.FUEL);
        }
        this.rocketEnergy = Math.max(0L, tag.getLong(TAG_ROCKET_ENERGY));
        this.rocketOxygenDroplets = Math.max(0L, tag.getLong(TAG_ROCKET_OXYGEN));

        if (tag.contains(TAG_SELECTED_SYMMETRY, Tag.TAG_STRING)) {
            String symName = tag.getString(TAG_SELECTED_SYMMETRY);
            RadialSymmetry parsedSym = RadialSymmetry.TWO;
            for (RadialSymmetry sym : RadialSymmetry.values()) {
                if (sym.getSerializedName().equals(symName)) {
                    parsedSym = sym;
                    break;
                }
            }
            this.selectedSymmetry = parsedSym;
        }
        if (tag.contains(TAG_SELECTED_CROSSFEED, Tag.TAG_STRING)) {
            String cfName = tag.getString(TAG_SELECTED_CROSSFEED);
            CrossfeedMode parsedCf = CrossfeedMode.ISOLATED;
            for (CrossfeedMode mode : CrossfeedMode.values()) {
                if (mode.getSerializedName().equals(cfName)) {
                    parsedCf = mode;
                    break;
                }
            }
            this.selectedCrossfeed = parsedCf;
        }
        if (tag.contains(TAG_ASSEMBLY_GRAPH)) {
            this.assemblyGraph = RocketAssemblyGraph.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_ASSEMBLY_GRAPH))
                    .result()
                    .orElse(RocketAssemblyGraph.EMPTY);
        } else {
            this.assemblyGraph = RocketAssemblyGraph.EMPTY;
        }

        this.stageFuelDroplets.clear();
        this.stageFuelVariants.clear();
        if (tag.contains(TAG_STAGE_FUELS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_STAGE_FUELS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag st = list.getCompound(i);
                int sIdx = st.getInt("Stage");
                long amt = Math.max(0L, st.getLong("Amount"));
                this.stageFuelDroplets.put(sIdx, amt);
                if (st.contains("Fluid", Tag.TAG_STRING)) {
                    ResourceLocation fid = ResourceLocation.tryParse(st.getString("Fluid"));
                    if (fid != null && BuiltInRegistries.FLUID.containsKey(fid)) {
                        this.stageFuelVariants.put(sIdx, FluidVariant.of(BuiltInRegistries.FLUID.get(fid)));
                    }
                }
            }
        }
        this.boosterFuelDroplets.clear();
        this.boosterFuelVariants.clear();
        if (tag.contains(TAG_BOOSTER_FUELS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_BOOSTER_FUELS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag bt = list.getCompound(i);
                String gid = bt.getString("Group");
                long amt = Math.max(0L, bt.getLong("Amount"));
                if (!gid.isBlank()) {
                    this.boosterFuelDroplets.put(gid, amt);
                    if (bt.contains("Fluid", Tag.TAG_STRING)) {
                        ResourceLocation fid = ResourceLocation.tryParse(bt.getString("Fluid"));
                        if (fid != null && BuiltInRegistries.FLUID.containsKey(fid)) {
                            this.boosterFuelVariants.put(gid, FluidVariant.of(BuiltInRegistries.FLUID.get(fid)));
                        }
                    }
                }
            }
        }
        this.pendingAssemblySteps.clear();
        if (tag.contains(TAG_PENDING_ASSEMBLY, Tag.TAG_LIST)) {
            ListTag pendingList = tag.getList(TAG_PENDING_ASSEMBLY, Tag.TAG_COMPOUND);
            for (int i = 0; i < pendingList.size(); i++) {
                CompoundTag pt = pendingList.getCompound(i);
                BlockPos pos = BlockPos.of(pt.getLong("Pos"));
                Direction dir = Direction.byName(pt.getString("Facing"));
                if (dir == null || !dir.getAxis().isHorizontal()) {
                    dir = Direction.NORTH;
                }
                ItemStack stack = ItemStack.parseOptional(registries, pt.getCompound("Item"));
                int duration = Math.max(1, pt.getInt("Duration"));
                float visOffX = pt.getFloat("VisOffX");
                float visOffZ = pt.getFloat("VisOffZ");
                if (!stack.isEmpty()) {
                    this.pendingAssemblySteps.add(new PendingAssemblyStep(pos, dir, stack, duration, visOffX, visOffZ));
                }
            }
        }
        this.assemblyStepElapsedTicks = Math.max(0, tag.getInt(TAG_ASSEMBLY_STEP_ELAPSED));
        this.assemblyTotalElapsedTicks = Math.max(0, tag.getInt(TAG_ASSEMBLY_TOTAL_ELAPSED));
        this.assemblyTotalDurationTicks = Math.max(0, tag.getInt(TAG_ASSEMBLY_TOTAL_DURATION));
        this.assemblyCompletedSteps = Math.max(0, tag.getInt(TAG_ASSEMBLY_COMPLETED_STEPS));
        this.assemblyTotalSteps = Math.max(0, tag.getInt(TAG_ASSEMBLY_TOTAL_STEPS));
        if (tag.contains(TAG_LAUNCH_PHASE, Tag.TAG_STRING)) {
            try {
                this.launchPhase = LaunchSequencePhase.valueOf(tag.getString(TAG_LAUNCH_PHASE));
            } catch (IllegalArgumentException ignored) {
                this.launchPhase = LaunchSequencePhase.IDLE;
            }
        } else {
            this.launchPhase = LaunchSequencePhase.IDLE;
        }
        this.launchCountdownTicks = Math.max(0, tag.getInt(TAG_LAUNCH_COUNTDOWN_TICKS));
        this.lastLaunchAbortReason = tag.contains(TAG_LAST_ABORT_REASON, Tag.TAG_STRING)
                ? tag.getString(TAG_LAST_ABORT_REASON)
                : "";
        this.rebuildFluidStorage();

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
                if (!PropellantType.isRocketPropellantFluid(variant.getFluid())) {
                    return false;
                }
                return HangarTerminalBlockEntity.this.rocketFuelDroplets <= 0L
                        || HangarTerminalBlockEntity.this.rocketFuelVariant.isBlank()
                        || HangarTerminalBlockEntity.this.rocketFuelVariant.equals(variant);
            }
            return variant.isOf(Gases.OXYGEN);
        }

        private FluidVariant canonicalVariant() {
            if (this.fuelTank) {
                return !HangarTerminalBlockEntity.this.rocketFuelVariant.isBlank()
                        ? HangarTerminalBlockEntity.this.rocketFuelVariant
                        : FluidVariant.of(GCFluids.FUEL);
            }
            return FluidVariant.of(Gases.OXYGEN);
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
                if (this.fuelTank && current <= 0L) {
                    HangarTerminalBlockEntity.this.rocketFuelVariant = resource;
                }
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

    private static PropellantType resolvePropellantById(@Nullable String propellantId) {
        if (propellantId == null || propellantId.isBlank()) {
            return PropellantType.RP1;
        }
        String norm = propellantId.toUpperCase(Locale.ROOT);
        for (PropellantType type : PropellantType.values()) {
            if (type.name().equals(norm)) {
                return type;
            }
        }
        return PropellantType.RP1;
    }

    private static FluidVariant defaultFluidForPropellant(PropellantType type) {
        return switch (type) {
            case HYDROLOX, NERVA -> FluidVariant.of(GCFluids.LIQUID_HYDROGEN);
            case METHALOX -> FluidVariant.of(GCFluids.LIQUID_METHANE);
            case XENON_ELECTRIC -> FluidVariant.of(GCFluids.LIQUID_XENON);
            case MONOPROPELLANT -> FluidVariant.of(GCFluids.MONOPROPELLANT);
            default -> FluidVariant.of(GCFluids.FUEL);
        };
    }

    private record FuelSlotSnapshot(long amount, FluidVariant variant) {
    }

    private static boolean isCreativeBypass(@Nullable ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (player.gameMode != null) {
            return player.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
        }
        return player.isCreative();
    }

    private boolean anyStageStrictlyAcceptsFluid(FluidVariant resource) {
        for (RocketStageNode stage : this.assemblyGraph.stages()) {
            if (!stage.decoupled() && stage.fuelCapacityDroplets() > 0L) {
                PropellantType stageProp = resolvePropellantById(stage.propellantId());
                if (stageProp != PropellantType.SOLID && stageProp.acceptsFluid(resource.getFluid())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Per-stage Fabric Transfer API fluid slot routing incoming propellants strictly to stages whose
     * {@link PropellantType} accepts that fluid (enabling mixed RP-1 + Hydrolox + Xenon rockets).
     */
    @SuppressWarnings("UnstableApiUsage")
    private final class StagePropellantSlotStorage extends SnapshotParticipant<FuelSlotSnapshot> implements SingleSlotStorage<FluidVariant> {
        private final int stageIndex;
        private final PropellantType expectedPropellant;
        private final Set<PropellantType> tankPropellants;
        private final long capacity;

        private StagePropellantSlotStorage(int stageIndex, PropellantType expectedPropellant, Set<PropellantType> tankPropellants, long capacity) {
            this.stageIndex = stageIndex;
            this.expectedPropellant = expectedPropellant;
            this.tankPropellants = tankPropellants;
            this.capacity = Math.max(0L, capacity);
        }

        private long currentAmount() {
            return HangarTerminalBlockEntity.this.stageFuelDroplets.getOrDefault(this.stageIndex, 0L);
        }

        private FluidVariant currentVariant() {
            return HangarTerminalBlockEntity.this.stageFuelVariants.getOrDefault(
                    this.stageIndex,
                    defaultFluidForPropellant(this.expectedPropellant)
            );
        }

        private boolean accepts(FluidVariant resource) {
            if (resource.isBlank()) {
                return false;
            }
            boolean accepted = this.expectedPropellant.acceptsFluid(resource.getFluid());
            if (!accepted && !HangarTerminalBlockEntity.this.anyStageStrictlyAcceptsFluid(resource)) {
                for (PropellantType tp : this.tankPropellants) {
                    if (tp.acceptsFluid(resource.getFluid())) {
                        accepted = true;
                        break;
                    }
                }
            }
            if (!accepted) {
                return false;
            }
            long cur = this.currentAmount();
            FluidVariant curVar = this.currentVariant();
            return cur <= 0L || curVar.isBlank() || curVar.equals(resource);
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!HangarTerminalBlockEntity.this.isRocketAssembled() || !this.accepts(resource)) {
                return 0L;
            }
            long cur = this.currentAmount();
            long space = Math.max(0L, this.capacity - cur);
            long inserted = Math.min(space, maxAmount);
            if (inserted > 0L) {
                this.updateSnapshots(transaction);
                HangarTerminalBlockEntity.this.stageFuelDroplets.put(this.stageIndex, cur + inserted);
                if (cur <= 0L) {
                    HangarTerminalBlockEntity.this.stageFuelVariants.put(this.stageIndex, resource);
                }
                if (HangarTerminalBlockEntity.this.rocketFuelDroplets <= 0L) {
                    HangarTerminalBlockEntity.this.rocketFuelVariant = resource;
                }
                HangarTerminalBlockEntity.this.recomputeAggregateFuelAndGraph();
            }
            return inserted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!HangarTerminalBlockEntity.this.isRocketAssembled() || !this.accepts(resource)) {
                return 0L;
            }
            long cur = this.currentAmount();
            long extracted = Math.min(cur, maxAmount);
            if (extracted > 0L) {
                this.updateSnapshots(transaction);
                HangarTerminalBlockEntity.this.stageFuelDroplets.put(this.stageIndex, cur - extracted);
                HangarTerminalBlockEntity.this.recomputeAggregateFuelAndGraph();
            }
            return extracted;
        }

        @Override
        public boolean isResourceBlank() {
            return this.currentAmount() <= 0L;
        }

        @Override
        public FluidVariant getResource() {
            return this.currentAmount() > 0L ? this.currentVariant() : FluidVariant.blank();
        }

        @Override
        public long getAmount() {
            return this.currentAmount();
        }

        @Override
        public long getCapacity() {
            return this.capacity;
        }

        @Override
        protected FuelSlotSnapshot createSnapshot() {
            return new FuelSlotSnapshot(this.currentAmount(), this.currentVariant());
        }

        @Override
        protected void readSnapshot(FuelSlotSnapshot snapshot) {
            HangarTerminalBlockEntity.this.stageFuelDroplets.put(this.stageIndex, snapshot.amount());
            HangarTerminalBlockEntity.this.stageFuelVariants.put(this.stageIndex, snapshot.variant());
            HangarTerminalBlockEntity.this.recomputeAggregateFuelAndGraph();
        }

        @Override
        protected void onFinalCommit() {
            HangarTerminalBlockEntity.this.sync();
        }
    }

    /**
     * Per-radial-booster-group Fabric Transfer API fluid slot routing liquid propellants to liquid
     * strap-on boosters (isolating solid SRBs).
     */
    @SuppressWarnings("UnstableApiUsage")
    private final class BoosterPropellantSlotStorage extends SnapshotParticipant<FuelSlotSnapshot> implements SingleSlotStorage<FluidVariant> {
        private final String groupId;
        private final PropellantType expectedPropellant;
        private final long capacity;

        private BoosterPropellantSlotStorage(String groupId, PropellantType expectedPropellant, long capacity) {
            this.groupId = groupId;
            this.expectedPropellant = expectedPropellant;
            this.capacity = Math.max(0L, capacity);
        }

        private long currentAmount() {
            return HangarTerminalBlockEntity.this.boosterFuelDroplets.getOrDefault(this.groupId, 0L);
        }

        private FluidVariant currentVariant() {
            return HangarTerminalBlockEntity.this.boosterFuelVariants.getOrDefault(
                    this.groupId,
                    defaultFluidForPropellant(this.expectedPropellant)
            );
        }

        private boolean accepts(FluidVariant resource) {
            if (resource.isBlank() || !this.expectedPropellant.acceptsFluid(resource.getFluid())) {
                return false;
            }
            long cur = this.currentAmount();
            FluidVariant curVar = this.currentVariant();
            return cur <= 0L || curVar.isBlank() || curVar.equals(resource);
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!HangarTerminalBlockEntity.this.isRocketAssembled() || !this.accepts(resource)) {
                return 0L;
            }
            long cur = this.currentAmount();
            long space = Math.max(0L, this.capacity - cur);
            long inserted = Math.min(space, maxAmount);
            if (inserted > 0L) {
                this.updateSnapshots(transaction);
                HangarTerminalBlockEntity.this.boosterFuelDroplets.put(this.groupId, cur + inserted);
                if (cur <= 0L) {
                    HangarTerminalBlockEntity.this.boosterFuelVariants.put(this.groupId, resource);
                }
                HangarTerminalBlockEntity.this.recomputeAggregateFuelAndGraph();
            }
            return inserted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!HangarTerminalBlockEntity.this.isRocketAssembled() || !this.accepts(resource)) {
                return 0L;
            }
            long cur = this.currentAmount();
            long extracted = Math.min(cur, maxAmount);
            if (extracted > 0L) {
                this.updateSnapshots(transaction);
                HangarTerminalBlockEntity.this.boosterFuelDroplets.put(this.groupId, cur - extracted);
                HangarTerminalBlockEntity.this.recomputeAggregateFuelAndGraph();
            }
            return extracted;
        }

        @Override
        public boolean isResourceBlank() {
            return this.currentAmount() <= 0L;
        }

        @Override
        public FluidVariant getResource() {
            return this.currentAmount() > 0L ? this.currentVariant() : FluidVariant.blank();
        }

        @Override
        public long getAmount() {
            return this.currentAmount();
        }

        @Override
        public long getCapacity() {
            return this.capacity;
        }

        @Override
        protected FuelSlotSnapshot createSnapshot() {
            return new FuelSlotSnapshot(this.currentAmount(), this.currentVariant());
        }

        @Override
        protected void readSnapshot(FuelSlotSnapshot snapshot) {
            HangarTerminalBlockEntity.this.boosterFuelDroplets.put(this.groupId, snapshot.amount());
            HangarTerminalBlockEntity.this.boosterFuelVariants.put(this.groupId, snapshot.variant());
            HangarTerminalBlockEntity.this.recomputeAggregateFuelAndGraph();
        }

        @Override
        protected void onFinalCommit() {
            HangarTerminalBlockEntity.this.sync();
        }
    }
}
