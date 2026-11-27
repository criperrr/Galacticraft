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

package dev.galacticraft.mod.content.entity.vehicle;

import dev.galacticraft.api.entity.IgnoreShift;
import dev.galacticraft.api.rocket.RocketData;
import dev.galacticraft.api.rocket.RocketPrefabs;
import dev.galacticraft.api.rocket.assembly.MountedPartNode;
import dev.galacticraft.api.rocket.assembly.RadialBoosterGroup;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph.LocalSeatOffset;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.Galacticraft;
import dev.galacticraft.mod.attachments.GCServerPlayer;
import dev.galacticraft.mod.config.GCConfigUtil;
import dev.galacticraft.mod.content.GCSounds;
import dev.galacticraft.mod.content.GCStats;
import dev.galacticraft.mod.content.advancements.GCTriggers;
import dev.galacticraft.mod.content.block.multiblock.AbstractBoundsProxyBlock;
import dev.galacticraft.mod.content.block.platform.HangarTerminalBlock;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.entity.damage.GCDamageTypes;
import dev.galacticraft.mod.content.entity.data.GCEntityDataSerializers;
import dev.galacticraft.mod.content.entity.orbital.FlightTelemetryEngine;
import dev.galacticraft.mod.content.rocket.failure.LaunchFailureEngine;
import dev.galacticraft.mod.content.rocket.launch.RocketStateConverter;
import dev.galacticraft.mod.content.rocket.physics.ModularFlightPhysics;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.content.rocket.physics.PropellantType;
import dev.galacticraft.mod.network.s2c.OpenCelestialScreenPayload;
import dev.galacticraft.mod.network.s2c.ResetPerspectivePacket;
import dev.galacticraft.mod.particle.EntityParticleOption;
import dev.galacticraft.mod.particle.GCParticleTypes;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Unified powered flight entity for Galacticraft 5 modular rockets.
 *
 * <p>Instantiated atomically at {@code T = 0.00s} by {@link RocketStateConverter} from a static
 * launchpad {@link RocketAssemblyGraph}, and atomically reconverted back into static blocks upon
 * safe propulsive touchdown ({@code v <= 5.00 m/s}).
 */
public class ModularRocketFlightEntity extends Entity implements IgnoreShift {
    public static final int CRASH_ARM_DELAY_TICKS = 20;
    public static final int PASSENGER_WATCHDOG_TICKS = 60;
    public static final int VACUUM_NOZZLE_BLOWOUT_TICKS = 40;

    /**
     * Optional client-only cache cleanup hook invoked from {@link #onClientRemoval()}.
     */
    public static volatile @Nullable IntConsumer CLIENT_REMOVAL_HOOK = null;

    public enum FlightPhase {
        ASCENT,
        HOT_STAGING,
        COASTING,
        LES_ABORT,
        RETRO_DESCENT,
        LANDED
    }

    private static final EntityDataAccessor<RocketAssemblyGraph> ASSEMBLY_GRAPH = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, GCEntityDataSerializers.ROCKET_ASSEMBLY_GRAPH);
    private static final EntityDataAccessor<Integer> FLIGHT_PHASE = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH_DEG = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW_DEG = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL_DEG = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TVC_PITCH_DEG = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TVC_YAW_DEG = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DYNAMIC_PRESSURE_PA = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AMBIENT_PRESSURE_ATM = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_TWR = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_G_LOAD = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> AUTOPILOT_ENABLED = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MANUAL_OVERRIDE_ACTIVE = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LANDING_GEAR_DEPLOYED = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PARACHUTES_DEPLOYED = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTIVE_FAILURE_MASK = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HOT_STAGE_TICKS = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CAMERA_SHAKE = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TOTAL_HEIGHT_BLOCKS = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_RADIUS_BLOCKS = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AGL_BLOCKS = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HOVER_THROTTLE = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SUICIDE_BURN_ALT = SynchedEntityData.defineId(
            ModularRocketFlightEntity.class, EntityDataSerializers.FLOAT);

    private float pitchDegO = 0.0F;
    private float yawDegO = 0.0F;
    private float rollDegO = 0.0F;

    private double launchSurfaceY = 64.0;
    private double maxReachedAgl = 0.0;
    private long storedEnergyGj = 0L;
    private long storedOxygenDroplets = 0L;
    private Direction padFacing = Direction.NORTH;
    private @Nullable BlockPos originTerminalPos = null;
    private final List<UUID> crewUuids = new ArrayList<>();
    private final Map<BlockPos, ItemStack> partScienceDrives = new LinkedHashMap<>();
    private final SimpleContainer cargoInventory = new SimpleContainer(27);

    // Control inputs & runtime counters
    private float pilotPitchInput = 0.0F;
    private float pilotYawInput = 0.0F;
    private float targetThrottle = 1.0F;
    private boolean pilotRetroBurnHeld = false;
    private int manualOverrideHoldTicks = 0;

    private int flightTicks = 0;
    private int emptyWatchdogTicks = 0;
    private boolean watchdogBeaconSent = false;
    private int lesBurnTicksRemaining = 0;
    private int vacuumOverexpansionTicks = 0;
    private int cabinBreachTicks = 0;
    private int flameoutTicks = 0;
    private boolean maxQFailureChecked = false;
    private boolean vacuumFailureChecked = false;
    private boolean crashed = false;
    private boolean suborbitalAscentAttempt = false;
    private double lastVerticalSpeedBlocksPerTick = 0.0;

    public ModularRocketFlightEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ASSEMBLY_GRAPH, RocketAssemblyGraph.EMPTY);
        builder.define(FLIGHT_PHASE, FlightPhase.ASCENT.ordinal());
        builder.define(THROTTLE, 1.0F);
        builder.define(PITCH_DEG, 0.0F);
        builder.define(YAW_DEG, 0.0F);
        builder.define(ROLL_DEG, 0.0F);
        builder.define(TVC_PITCH_DEG, 0.0F);
        builder.define(TVC_YAW_DEG, 0.0F);
        builder.define(DYNAMIC_PRESSURE_PA, 0.0F);
        builder.define(AMBIENT_PRESSURE_ATM, 1.0F);
        builder.define(CURRENT_TWR, 0.0F);
        builder.define(CURRENT_G_LOAD, 1.0F);
        builder.define(AUTOPILOT_ENABLED, true);
        builder.define(MANUAL_OVERRIDE_ACTIVE, false);
        builder.define(LANDING_GEAR_DEPLOYED, false);
        builder.define(PARACHUTES_DEPLOYED, false);
        builder.define(ACTIVE_FAILURE_MASK, LaunchFailureEngine.FAILURE_NONE);
        builder.define(HOT_STAGE_TICKS, 0);
        builder.define(CAMERA_SHAKE, 0.0F);
        builder.define(TOTAL_HEIGHT_BLOCKS, 4.0F);
        builder.define(MAX_RADIUS_BLOCKS, 1.5F);
        builder.define(AGL_BLOCKS, 0.0F);
        builder.define(HOVER_THROTTLE, 0.5F);
        builder.define(SUICIDE_BURN_ALT, 40.0F);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ASSEMBLY_GRAPH.equals(key) || TOTAL_HEIGHT_BLOCKS.equals(key) || MAX_RADIUS_BLOCKS.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        float w = Math.max(1.5F, this.getMaxRadiusBlocks() * 2.0F);
        float h = Math.max(2.0F, this.getTotalHeightBlocks());
        return EntityDimensions.scalable(w, h);
    }

    /**
     * Initializes this flight entity from a compiled {@link RocketAssemblyGraph} snapshot.
     */
    public void initializeFlightState(
            @NotNull RocketAssemblyGraph graph,
            long energyGj,
            long oxygenDroplets,
            @NotNull Direction padFacing,
            @Nullable BlockPos originTerminalPos,
            double surfaceY,
            @NotNull FlightPhase initialPhase,
            @NotNull List<UUID> crewUuids,
            @NotNull Map<BlockPos, ItemStack> scienceDrives
    ) {
        this.setAssemblyGraph(graph);
        this.storedEnergyGj = Math.max(0L, energyGj);
        this.storedOxygenDroplets = Math.max(0L, oxygenDroplets);
        this.padFacing = padFacing;
        this.originTerminalPos = originTerminalPos != null ? originTerminalPos.immutable() : null;
        this.launchSurfaceY = surfaceY;
        this.suborbitalAscentAttempt = (initialPhase == FlightPhase.ASCENT);
        this.setFlightPhase(initialPhase);
        this.crewUuids.clear();
        this.crewUuids.addAll(crewUuids);
        this.partScienceDrives.clear();
        for (Map.Entry<BlockPos, ItemStack> entry : scienceDrives.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                this.partScienceDrives.put(entry.getKey().immutable(), entry.getValue().copy());
            }
        }
        if (initialPhase == FlightPhase.RETRO_DESCENT) {
            // Start planetary descent in manual Landing Minigame mode (0% throttle; player controls Espaço/Shift/X/L or Shift+G for Auto)
            this.entityData.set(THROTTLE, 0.0F);
            this.targetThrottle = 0.0F;
            this.entityData.set(AUTOPILOT_ENABLED, false);
            this.entityData.set(LANDING_GEAR_DEPLOYED, false);
        } else {
            this.entityData.set(THROTTLE, 1.0F);
            this.targetThrottle = 1.0F;
            this.entityData.set(AUTOPILOT_ENABLED, true);
        }
        this.refreshDimensions();
    }

    public @NotNull RocketAssemblyGraph getAssemblyGraph() {
        return this.entityData.get(ASSEMBLY_GRAPH);
    }

    public void setAssemblyGraph(@NotNull RocketAssemblyGraph graph) {
        this.entityData.set(ASSEMBLY_GRAPH, graph);
        this.entityData.set(TOTAL_HEIGHT_BLOCKS, (float) Math.max(2, graph.totalHeightBlocks()));
        this.entityData.set(MAX_RADIUS_BLOCKS, (float) Math.max(0.75, graph.maxRadiusBlocks()));
        this.refreshDimensions();
    }

    public @NotNull FlightPhase getFlightPhase() {
        int ord = this.entityData.get(FLIGHT_PHASE);
        FlightPhase[] values = FlightPhase.values();
        return (ord >= 0 && ord < values.length) ? values[ord] : FlightPhase.ASCENT;
    }

    public void setFlightPhase(@NotNull FlightPhase phase) {
        this.entityData.set(FLIGHT_PHASE, phase.ordinal());
    }

    public float getThrottle() {
        return this.entityData.get(THROTTLE);
    }

    public float getPitchDeg() {
        return this.entityData.get(PITCH_DEG);
    }

    public float getYawDeg() {
        return this.entityData.get(YAW_DEG);
    }

    public float getRollDeg() {
        return this.entityData.get(ROLL_DEG);
    }

    public float getInterpolatedPitchDeg(float partialTick) {
        return Mth.lerp(partialTick, this.pitchDegO, this.getPitchDeg());
    }

    public float getInterpolatedYawDeg(float partialTick) {
        return Mth.lerp(partialTick, this.yawDegO, this.getYawDeg());
    }

    public float getInterpolatedRollDeg(float partialTick) {
        return Mth.lerp(partialTick, this.rollDegO, this.getRollDeg());
    }

    public float getTvcPitchDeg() {
        return this.entityData.get(TVC_PITCH_DEG);
    }

    public float getTvcYawDeg() {
        return this.entityData.get(TVC_YAW_DEG);
    }

    public float getDynamicPressurePa() {
        return this.entityData.get(DYNAMIC_PRESSURE_PA);
    }

    public float getAmbientPressureAtm() {
        return this.entityData.get(AMBIENT_PRESSURE_ATM);
    }

    public float getCurrentTwr() {
        return this.entityData.get(CURRENT_TWR);
    }

    public float getCurrentGLoad() {
        return this.entityData.get(CURRENT_G_LOAD);
    }

    public boolean isAutopilotEnabled() {
        return this.entityData.get(AUTOPILOT_ENABLED);
    }

    public boolean isManualOverrideActive() {
        return this.entityData.get(MANUAL_OVERRIDE_ACTIVE);
    }

    public boolean isLandingGearDeployed() {
        return this.entityData.get(LANDING_GEAR_DEPLOYED);
    }

    public boolean isParachutesDeployed() {
        return this.entityData.get(PARACHUTES_DEPLOYED);
    }

    public int getActiveFailureMask() {
        return this.entityData.get(ACTIVE_FAILURE_MASK);
    }

    public int getHotStageTicks() {
        return this.entityData.get(HOT_STAGE_TICKS);
    }

    public float getCameraShake() {
        return this.entityData.get(CAMERA_SHAKE);
    }

    public float getTotalHeightBlocks() {
        return this.entityData.get(TOTAL_HEIGHT_BLOCKS);
    }

    public float getMaxRadiusBlocks() {
        return this.entityData.get(MAX_RADIUS_BLOCKS);
    }

    public float getAglBlocks() {
        return this.entityData.get(AGL_BLOCKS);
    }

    public float getHoverThrottle() {
        return this.entityData.get(HOVER_THROTTLE);
    }

    public float getSuicideBurnAlt() {
        return this.entityData.get(SUICIDE_BURN_ALT);
    }

    public long getStoredEnergyGj() {
        return this.storedEnergyGj;
    }

    public long getStoredOxygenDroplets() {
        return this.storedOxygenDroplets;
    }

    public @NotNull Direction getPadFacing() {
        return this.padFacing;
    }

    public @Nullable BlockPos getOriginTerminalPos() {
        return this.originTerminalPos;
    }

    public @NotNull List<UUID> getCrewUuids() {
        return Collections.unmodifiableList(this.crewUuids);
    }

    public @NotNull Map<BlockPos, ItemStack> getPartScienceDrives() {
        return Collections.unmodifiableMap(this.partScienceDrives);
    }

    public int getEmptyWatchdogTicks() {
        return this.emptyWatchdogTicks;
    }

    public int getFlightTicks() {
        return this.flightTicks;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldIgnoreShiftExit() {
        return this.getFlightPhase() != FlightPhase.LANDED;
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        int maxSeats = Math.max(1, this.getAssemblyGraph().crewSeats().size());
        return this.getPassengers().size() < maxSeats;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        // Return null so the server stays 100% authoritative for rocket position (matching RocketEntity)
        // and prevents LocalPlayer's ServerboundMoveVehiclePacket from snapping the rocket back to the pad!
        return null;
    }

    public @NotNull SimpleContainer getCargoInventory() {
        return this.cargoInventory;
    }

    public void loadCargoStacks(@Nullable NonNullList<ItemStack> stacks) {
        this.cargoInventory.clearContent();
        if (stacks == null) {
            return;
        }
        int count = Math.min(stacks.size(), this.cargoInventory.getContainerSize());
        for (int i = 0; i < count; i++) {
            this.cargoInventory.setItem(i, stacks.get(i).copy());
        }
    }

    public @NotNull NonNullList<ItemStack> copyCargoStacks() {
        int size = this.cargoInventory.getContainerSize();
        NonNullList<ItemStack> copy = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) {
            copy.set(i, this.cargoInventory.getItem(i).copy());
        }
        return copy;
    }

    public void openCargoInventory(@NotNull ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, playerInv, p) -> ChestMenu.threeRows(syncId, playerInv, this.cargoInventory),
                Component.literal("Inventário do Foguete Modular")
        ));
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
            this.openCargoInventory(serverPlayer);
            return InteractionResult.CONSUME;
        }
        if (!player.isPassenger() && this.canAddPassenger(player)) {
            player.startRiding(this, true);
            if (!this.crewUuids.contains(player.getUUID())) {
                this.crewUuids.add(player.getUUID());
            }
            if (this.getFlightPhase() == FlightPhase.LANDED && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.literal("[FOGUETE POUSADO] [Espaço] Decolar | [F] Inventário | [Shift] Desembarcar")
                                .withStyle(ChatFormatting.AQUA),
                        true
                );
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        RocketAssemblyGraph graph = this.getAssemblyGraph();
        double minPartY = computeMinPartY(graph);
        int seatIdx = Math.max(0, this.getPassengers().indexOf(passenger));
        List<LocalSeatOffset> seats = graph.crewSeats();
        double lx = 0.0;
        double ly = Math.max(0.35, this.getTotalHeightBlocks() - 2.0);
        double lz = 0.0;
        if (!seats.isEmpty()) {
            LocalSeatOffset seat = seats.get(Math.min(seatIdx, seats.size() - 1));
            lx = seat.x();
            // Convert pad-relative seat Y into rocket-base-local Y and seat pilot squarely inside the capsule
            ly = Math.max(0.20, (seat.y() - minPartY) - 0.65);
            lz = seat.z();
        } else if (seatIdx > 0) {
            double angle = (Math.PI * 2.0 * seatIdx) / Math.max(1, this.getPassengers().size());
            lx = Math.cos(angle) * 0.65;
            lz = Math.sin(angle) * 0.65;
        }

        // Rotate seat offset by rocket yaw/pitch/roll quaternion around rocket base
        Quaternionf rot = buildAttitudeQuaternion(this.getYawDeg(), this.getPitchDeg(), this.getRollDeg());
        Vector3f localVec = new Vector3f((float) lx, (float) ly, (float) lz);
        rot.transform(localVec);

        callback.accept(
                passenger,
                this.getX() + localVec.x,
                this.getY() + localVec.y,
                this.getZ() + localVec.z
        );
    }

    private static double computeMinPartY(RocketAssemblyGraph graph) {
        int minY = Integer.MAX_VALUE;
        for (MountedPartNode part : graph.allActiveParts()) {
            minY = Math.min(minY, part.relativePos().getY());
        }
        return minY == Integer.MAX_VALUE ? 0.0 : (double) minY;
    }

    public static @NotNull Quaternionf buildAttitudeQuaternion(float yawDeg, float pitchDeg, float rollDeg) {
        return new Quaternionf()
                .rotateY(-yawDeg * Mth.DEG_TO_RAD)
                .rotateX(pitchDeg * Mth.DEG_TO_RAD)
                .rotateZ(rollDeg * Mth.DEG_TO_RAD);
    }

    /**
     * Applies server-validated pilot control inputs received via {@link dev.galacticraft.mod.network.c2s.ModularRocketControlPayload}.
     */
    public void applyPilotControl(
            @NotNull ServerPlayer pilot,
            float pitchInput,
            float yawInput,
            float throttleDelta,
            boolean manualStage,
            boolean toggleAutopilot,
            boolean toggleLandingGear,
            boolean retroBurn,
            boolean engineCutoff
    ) {
        this.pilotPitchInput = Math.clamp(pitchInput, -1.0F, 1.0F);
        this.pilotYawInput = Math.clamp(yawInput, -1.0F, 1.0F);
        this.pilotRetroBurnHeld = retroBurn;

        // Re-launch directly from a unified landed rocket when pilot holds Space!
        if (this.getFlightPhase() == FlightPhase.LANDED) {
            if (throttleDelta > 0.01F || retroBurn) {
                RocketAssemblyGraph graph = this.getAssemblyGraph();
                RocketStageNode active = graph.activeStage();
                boolean hasProp = active != null
                        && active.activeThrustKn() > 0.0
                        && (active.totalCurrentFuelDroplets() > 0L
                        || ModularFlightPhysics.resolvePropellant(active.propellantId()) == PropellantType.SOLID);
                if (hasProp) {
                    this.launchSurfaceY = this.getY();
                    this.maxReachedAgl = 0.0;
                    this.flightTicks = 0;
                    this.targetThrottle = 1.0F;
                    this.entityData.set(THROTTLE, 1.0F);
                    this.entityData.set(AUTOPILOT_ENABLED, true);
                    this.entityData.set(LANDING_GEAR_DEPLOYED, false);
                    this.entityData.set(PARACHUTES_DEPLOYED, false);
                    this.suborbitalAscentAttempt = true;
                    this.setFlightPhase(FlightPhase.ASCENT);
                    this.level().playSound(null, this.blockPosition(), GCSounds.ROCKET_IGNITE, SoundSource.NEUTRAL, 1.6F, 1.0F);
                    pilot.displayClientMessage(Component.literal("[IGNIÇÃO] Decolagem planetária iniciada!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
                } else {
                    pilot.displayClientMessage(Component.literal("[SEM PROPELENTE] Estágio ativo sem combustível ou motor para decolar!").withStyle(ChatFormatting.RED), true);
                }
            }
            return;
        }

        if (Math.abs(this.pilotPitchInput) > 0.05F || Math.abs(this.pilotYawInput) > 0.05F) {
            this.manualOverrideHoldTicks = 40;
            this.entityData.set(MANUAL_OVERRIDE_ACTIVE, true);
        }
        if (engineCutoff) {
            this.targetThrottle = 0.0F;
            this.entityData.set(THROTTLE, 0.0F);
            if (this.getFlightPhase() == FlightPhase.RETRO_DESCENT && this.isAutopilotEnabled()) {
                this.entityData.set(AUTOPILOT_ENABLED, false);
            }
            this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.7F, 0.85F);
            pilot.displayClientMessage(Component.literal("[MECO] Motor cortado (0%)").withStyle(ChatFormatting.AQUA), true);
        } else if (Math.abs(throttleDelta) > 0.01F) {
            float step = throttleDelta > 0.0F ? 0.045F : 0.055F;
            this.targetThrottle = Math.clamp(this.targetThrottle + throttleDelta * step, 0.0F, 1.0F);
            if (this.getFlightPhase() == FlightPhase.RETRO_DESCENT && this.isAutopilotEnabled()) {
                this.entityData.set(AUTOPILOT_ENABLED, false);
            }
        }
        if (toggleAutopilot) {
            boolean next = !this.isAutopilotEnabled();
            this.entityData.set(AUTOPILOT_ENABLED, next);
            pilot.displayClientMessage(
                    Component.literal("[GUIDANCE] Piloto Automático: " + (next ? "ATIVO" : "MANUAL"))
                            .withStyle(next ? ChatFormatting.GREEN : ChatFormatting.GOLD),
                    true
            );
        }
        if (toggleLandingGear) {
            if (this.getAssemblyGraph().hasLandingGear()) {
                boolean next = !this.isLandingGearDeployed();
                this.entityData.set(LANDING_GEAR_DEPLOYED, next);
                this.level().playSound(null, this.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 0.8F, next ? 0.9F : 1.2F);
                pilot.displayClientMessage(
                        Component.literal("[TREM DE POUSO] " + (next ? "ESTENDIDO" : "RECOLHIDO"))
                                .withStyle(next ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                        true
                );
            } else {
                pilot.displayClientMessage(Component.literal("[TREM DE POUSO] Módulo landing_gear não equipado.").withStyle(ChatFormatting.YELLOW), true);
            }
        }
        if (manualStage && this.level() instanceof ServerLevel serverLevel) {
            this.triggerManualStage(serverLevel, pilot);
        }
    }

    /**
     * Triggers emergency Launch Escape System (LES) abort: cuts main engines, decouples the crew
     * capsule from the booster stack, fires high-TWR (&gt; 6.0) solid escape motors for 40 ticks,
     * and auto-deploys parachutes during descent.
     */
    public boolean triggerLesAbort(@Nullable ServerPlayer initiator) {
        FlightPhase phase = this.getFlightPhase();
        if (phase == FlightPhase.LES_ABORT || phase == FlightPhase.LANDED) {
            return false;
        }
        RocketAssemblyGraph currentGraph = this.getAssemblyGraph();
        if (!currentGraph.hasLesAbort() && !hasCommandModule(currentGraph)) {
            if (initiator != null) {
                initiator.displayClientMessage(
                        Component.literal("[LES] Sem torre de escape ou cápsula habitável equipada!").withStyle(ChatFormatting.RED),
                        true
                );
            }
            return false;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            // Spawn jettisoned lower booster stack behind the escaping capsule
            List<MountedPartNode> boosterParts = collectNonCapsuleParts(currentGraph);
            if (!boosterParts.isEmpty()) {
                Vec3 boosterVel = this.getDeltaMovement().add(0.0, -0.15, 0.0);
                DetachedStageEntity jettisoned = DetachedStageEntity.create(
                        serverLevel,
                        this.position(),
                        boosterVel,
                        boosterParts,
                        DetachedStageEntity.SeparationKind.LES_JETTISONED_BOOSTER,
                        this.getPitchDeg(),
                        this.getYawDeg(),
                        this.getRollDeg(),
                        1.2F,
                        0.4F,
                        0.8F,
                        0.0,
                        this.originTerminalPos
                );
                serverLevel.addFreshEntity(jettisoned);
            }

            // Trim active vehicle to command capsule only
            RocketAssemblyGraph capsuleOnly = currentGraph.forLesAbortCapsule();
            this.setAssemblyGraph(capsuleOnly);
            this.setFlightPhase(FlightPhase.LES_ABORT);
            this.lesBurnTicksRemaining = ModularFlightPhysics.LES_ABORT_BURN_TICKS;
            this.entityData.set(THROTTLE, 1.0F);
            this.entityData.set(CAMERA_SHAKE, 1.0F);
            this.entityData.set(ACTIVE_FAILURE_MASK, LaunchFailureEngine.FAILURE_NONE);

            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.6F, 1.35F);
            serverLevel.playSound(null, this.blockPosition(), GCSounds.ROCKET_IGNITE, SoundSource.NEUTRAL, 1.4F, 1.4F);
            this.broadcastCrewMessage(
                    Component.literal("[LES ABORT] Cápsula ejetada! Motores de escape ativos (TWR > 6.0)!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            );
            return true;
        }
        return false;
    }

    /**
     * Triggers manual staging (drops radial boosters first, or initiates hot-staging/in-line staging).
     */
    public boolean triggerManualStage(@NotNull ServerLevel serverLevel, @Nullable ServerPlayer pilot) {
        RocketAssemblyGraph graph = this.getAssemblyGraph();
        RocketStageNode active = graph.activeStage();
        if (active == null) {
            return false;
        }
        // First detach any remaining radial booster groups on the active stage
        List<String> activeGroupIds = new ArrayList<>();
        for (RadialBoosterGroup group : active.radialGroups()) {
            if (!group.decoupled()) {
                activeGroupIds.add(group.groupId());
            }
        }
        if (!activeGroupIds.isEmpty()) {
            this.detachRadialBoosterGroup(serverLevel, active, activeGroupIds.get(0));
            return true;
        }
        // Otherwise stage the active core stage if an upper stage exists
        if (graph.activeStages().size() > 1) {
            if (active.hotStaging() && this.getFlightPhase() != FlightPhase.HOT_STAGING) {
                this.beginHotStaging(serverLevel);
            } else {
                this.decoupleActiveStage(serverLevel, active);
            }
            return true;
        }
        if (pilot != null) {
            pilot.displayClientMessage(Component.literal("[STAGING] Último estágio ativo.").withStyle(ChatFormatting.YELLOW), true);
        }
        return false;
    }

    @Override
    public void tick() {
        this.pitchDegO = this.getPitchDeg();
        this.yawDegO = this.getYawDeg();
        this.rollDegO = this.getRollDeg();
        super.tick();
        this.flightTicks++;

        if (this.level().isClientSide) {
            this.spawnClientFlightParticles();
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel) || this.crashed) {
            return;
        }

        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(serverLevel);
        double ambientPa = ModularFlightPhysics.computeAmbientPressureAtm(env, this.getY(), this.launchSurfaceY);
        double rho = ModularFlightPhysics.computeAirDensityKgM3(ambientPa, env.meanTempKelvin());
        double speedMs = this.getDeltaMovement().length() * 20.0;
        double dynamicQ = ModularFlightPhysics.computeDynamicPressurePa(rho, speedMs);

        this.entityData.set(AMBIENT_PRESSURE_ATM, (float) ambientPa);
        this.entityData.set(DYNAMIC_PRESSURE_PA, (float) dynamicQ);
        this.maxReachedAgl = Math.max(this.maxReachedAgl, Math.max(0.0, this.getY() - this.launchSurfaceY));

        // Track crew UUIDs and run autonomous 60-tick passenger watchdog
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof ServerPlayer sp && !this.crewUuids.contains(sp.getUUID())) {
                this.crewUuids.add(sp.getUUID());
            }
        }
        this.tickPassengerWatchdog(serverLevel);

        if (this.manualOverrideHoldTicks > 0) {
            this.manualOverrideHoldTicks--;
            if (this.manualOverrideHoldTicks == 0) {
                this.entityData.set(MANUAL_OVERRIDE_ACTIVE, false);
            }
        }

        FlightPhase phase = this.getFlightPhase();
        if (phase == FlightPhase.LANDED) {
            this.entityData.set(THROTTLE, 0.0F);
            this.entityData.set(CAMERA_SHAKE, 0.0F);
            this.entityData.set(CURRENT_TWR, 0.0F);
            this.entityData.set(CURRENT_G_LOAD, (float) env.gravityG());
            Vec3 settle = new Vec3(0.0, this.onGround() ? 0.0 : -0.06, 0.0);
            this.setDeltaMovement(settle);
            this.move(MoverType.SELF, settle);
            this.pilotPitchInput = 0.0F;
            this.pilotYawInput = 0.0F;
            return;
        }

        switch (phase) {
            case ASCENT, HOT_STAGING -> this.tickPoweredAscent(serverLevel, env, ambientPa, dynamicQ, phase == FlightPhase.HOT_STAGING);
            case LES_ABORT -> this.tickLesAbort(serverLevel, env, ambientPa);
            case RETRO_DESCENT -> this.tickRetroDescent(serverLevel, env, ambientPa);
            case COASTING -> this.tickCoasting(serverLevel, env, ambientPa);
            case LANDED -> this.setDeltaMovement(Vec3.ZERO);
        }

        // Apply active failure consequences (Cabin breach suffocation, fuel leak, flameout)
        this.tickActiveFailures(serverLevel, env);

        Vec3 preMove = this.getDeltaMovement();
        this.lastVerticalSpeedBlocksPerTick = preMove.y;
        this.moveWithHangarClearance(serverLevel, preMove);

        // Post-move touchdown / crash resolution (respecting 20-tick liftoff crash-arming delay)
        if (this.flightTicks > CRASH_ARM_DELAY_TICKS) {
            this.checkTouchdownOrCrash(serverLevel, preMove);
        }

        // Reset one-tick pilot inputs
        this.pilotPitchInput *= 0.5F;
        this.pilotYawInput *= 0.5F;
    }

    private void moveWithHangarClearance(ServerLevel serverLevel, Vec3 preMove) {
        FlightPhase phase = this.getFlightPhase();
        boolean ascending = preMove.y > 0.0
                && (phase == FlightPhase.ASCENT
                || phase == FlightPhase.HOT_STAGING
                || phase == FlightPhase.LES_ABORT
                || phase == FlightPhase.COASTING);
        if (ascending) {
            // Check whether solid non-hangar terrain obstructs the nose centerline; bypass hangar gantry/crane proxies cleanly!
            boolean blockedByNaturalCeiling = false;
            BlockPos noseCheck = BlockPos.containing(this.getX() + preMove.x, this.getY() + this.getTotalHeightBlocks() + preMove.y, this.getZ() + preMove.z);
            if (serverLevel.isInWorldBounds(noseCheck) && serverLevel.isLoaded(noseCheck)) {
                var state = serverLevel.getBlockState(noseCheck);
                if (!state.isAir()
                        && !state.getCollisionShape(serverLevel, noseCheck).isEmpty()
                        && !(state.getBlock() instanceof AbstractBoundsProxyBlock)
                        && !(state.getBlock() instanceof HangarTerminalBlock)
                        && !(state.getBlock() instanceof ModularRocketPartBlock)) {
                    blockedByNaturalCeiling = true;
                }
            }
            if (!blockedByNaturalCeiling) {
                this.setPos(this.getX() + preMove.x, this.getY() + preMove.y, this.getZ() + preMove.z);
                this.setOnGround(false);
                this.horizontalCollision = false;
                this.verticalCollision = false;
                return;
            }
        }
        this.move(MoverType.SELF, preMove);
    }

    private void tickPassengerWatchdog(ServerLevel serverLevel) {
        FlightPhase phase = this.getFlightPhase();
        if (phase == FlightPhase.LANDED) {
            return;
        }
        if (!this.getPassengers().isEmpty()) {
            this.emptyWatchdogTicks = 0;
            this.watchdogBeaconSent = false;
            return;
        }
        // Try to remount any disconnected/reconnected crew member within range
        for (UUID uuid : this.crewUuids) {
            if (serverLevel.getPlayerByUUID(uuid) instanceof ServerPlayer sp
                    && sp.isAlive()
                    && !sp.isPassenger()
                    && sp.distanceToSqr(this) <= 64.0 * 64.0) {
                sp.startRiding(this, true);
                this.emptyWatchdogTicks = 0;
                this.watchdogBeaconSent = false;
                return;
            }
        }
        if (this.crewUuids.isEmpty()) {
            // Unmanned probe flight — no watchdog needed
            return;
        }
        this.emptyWatchdogTicks++;
        if (this.emptyWatchdogTicks >= PASSENGER_WATCHDOG_TICKS && phase != FlightPhase.RETRO_DESCENT && phase != FlightPhase.LES_ABORT) {
            this.entityData.set(THROTTLE, 0.0F);
            this.targetThrottle = 0.0F;
            this.entityData.set(PARACHUTES_DEPLOYED, true);
            this.entityData.set(LANDING_GEAR_DEPLOYED, this.getAssemblyGraph().hasLandingGear());
            this.setFlightPhase(FlightPhase.RETRO_DESCENT);
            if (!this.watchdogBeaconSent) {
                this.watchdogBeaconSent = true;
                String msg = String.format(Locale.ROOT,
                        "[WATCHDOG] Nave sem tripulação a bordo! Descida balística de recuperação iniciada em (%d, %d, %d).",
                        this.blockPosition().getX(), this.blockPosition().getY(), this.blockPosition().getZ());
                for (UUID uuid : this.crewUuids) {
                    if (serverLevel.getPlayerByUUID(uuid) instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.YELLOW), false);
                    }
                }
            }
        }
    }

    private void tickPoweredAscent(
            ServerLevel serverLevel,
            PlanetaryEnvironment env,
            double ambientPa,
            double dynamicQ,
            boolean isHotStaging
    ) {
        RocketAssemblyGraph graph = this.getAssemblyGraph();
        RocketStageNode activeStage = graph.activeStage();
        if (activeStage == null) {
            this.setFlightPhase(FlightPhase.COASTING);
            return;
        }

        // Auto fairing jettison when P_a < 0.05 atm (Y >= 400 Overworld, Y >= 250 Mars)
        if (ModularFlightPhysics.shouldAutoJettisonFairings(graph, env, this.getY(), this.launchSurfaceY)) {
            this.jettisonFairings(serverLevel);
            graph = this.getAssemblyGraph();
            activeStage = graph.activeStage();
            if (activeStage == null) {
                return;
            }
        }

        // Hot-staging pre-ignition countdown (30 ticks = 1.50s)
        if (isHotStaging) {
            int rem = Math.max(0, this.getHotStageTicks() - 1);
            this.entityData.set(HOT_STAGE_TICKS, rem);
            if (rem == 0) {
                this.decoupleActiveStage(serverLevel, activeStage);
                this.setFlightPhase(FlightPhase.ASCENT);
                graph = this.getAssemblyGraph();
                activeStage = graph.activeStage();
                if (activeStage == null) {
                    return;
                }
            }
        }

        // Determine effective throttle: Max-Q auto-throttle when autopilot active (never dip below TWR 1.25)
        float requestedThrottle = this.isAutopilotEnabled() && !this.isManualOverrideActive()
                ? ModularFlightPhysics.computeMaxQAutoThrottle(env, this.getY(), this.launchSurfaceY, dynamicQ, this.targetThrottle)
                : this.targetThrottle;
        if (requestedThrottle < this.targetThrottle && this.getCurrentTwr() > 0.01F && this.getThrottle() > 0.01F) {
            float fullTwrEst = this.getCurrentTwr() / this.getThrottle();
            if (fullTwrEst > 0.01F) {
                float minThrottleForClimb = Math.clamp(1.25F / fullTwrEst, 0.0F, this.targetThrottle);
                requestedThrottle = Math.max(requestedThrottle, minThrottleForClimb);
            }
        }
        this.entityData.set(THROTTLE, requestedThrottle);

        // Execute per-tick propulsion & Asparagus crossfeed drain
        ModularFlightPhysics.PropulsionTickResult propRes = ModularFlightPhysics.stepStagePropulsion(
                activeStage,
                ambientPa,
                requestedThrottle,
                isHotStaging
        );
        graph = graph.withUpdatedStage(propRes.updatedStage());
        this.setAssemblyGraph(graph);

        // If hot-staging, add upper stage pre-ignition thrust so acceleration stays >= +1.0G
        double effectiveThrustKn = propRes.totalThrustKn();
        if (isHotStaging && graph.activeStages().size() > 1) {
            RocketStageNode upper = graph.activeStages().get(1);
            for (MountedPartNode part : upper.coreParts()) {
                effectiveThrustKn += ModularFlightPhysics.computeAtmosphericThrustKn(part, ambientPa, 0.85, 0.0);
            }
        }

        // Apply Flameout thrust oscillation if active (Failure Mode 4)
        if ((this.getActiveFailureMask() & LaunchFailureEngine.FAILURE_ENGINE_FLAMEOUT) != 0) {
            effectiveThrustKn *= LaunchFailureEngine.computeFlameoutThrustFactor(this.flameoutTicks);
        }

        // Vacuum nozzle over-expansion hazard (warn pilot, without artificial blowout stall when TWR > 1.0)
        if (propRes.vacuumNozzleOverexpansion()) {
            this.vacuumOverexpansionTicks++;
            this.addFailureFlag(LaunchFailureEngine.FAILURE_VACUUM_NOZZLE);
            if (this.vacuumOverexpansionTicks == 1) {
                this.broadcastCrewMessage(
                        Component.literal("[ALERTA] Bocal de vácuo em alta pressão (" + String.format(Locale.ROOT, "%.2f", ambientPa) + " atm)! Eficiência reduzida na atmosfera.").withStyle(ChatFormatting.YELLOW)
                );
            }
        } else {
            this.vacuumOverexpansionTicks = Math.max(0, this.vacuumOverexpansionTicks - 2);
        }

        // Attitude & TVC gimbal: Automatic Gravity Turn by default + manual WASD override
        float currentPitch = this.getPitchDeg();
        float currentYaw = this.getYawDeg();
        float tvcPitch;
        float tvcYaw;

        if (this.isManualOverrideActive() || !this.isAutopilotEnabled()) {
            float deltaPitch = this.pilotPitchInput * 1.4F;
            float deltaYaw = this.pilotYawInput * 1.8F;
            currentPitch = Mth.clamp(currentPitch + deltaPitch, -65.0F, 65.0F);
            currentYaw = (currentYaw + deltaYaw) % 360.0F;
            tvcPitch = Mth.clamp(this.pilotPitchInput * 10.0F, -12.0F, 12.0F);
            tvcYaw = Mth.clamp(this.pilotYawInput * 10.0F, -12.0F, 12.0F);
        } else {
            float targetPitch = ModularFlightPhysics.computeAutoGravityTurnPitchDeg(
                    this.getY(), this.launchSurfaceY, graph.totalHeightBlocks());
            float err = targetPitch - currentPitch;
            currentPitch += Mth.clamp(err * 0.08F, -0.8F, 0.8F);
            tvcPitch = Mth.clamp(err * 0.65F, -8.0F, 8.0F);
            tvcYaw = 0.0F;
        }

        // Flameout adds asymmetric tumbling torque
        if ((this.getActiveFailureMask() & LaunchFailureEngine.FAILURE_ENGINE_FLAMEOUT) != 0) {
            currentPitch = Mth.clamp(currentPitch + (serverLevel.random.nextFloat() - 0.5F) * 4.5F, -80.0F, 80.0F);
            this.entityData.set(ROLL_DEG, (this.getRollDeg() + 4.5F) % 360.0F);
        }

        this.entityData.set(PITCH_DEG, currentPitch);
        this.entityData.set(YAW_DEG, currentYaw);
        this.entityData.set(TVC_PITCH_DEG, tvcPitch);
        this.entityData.set(TVC_YAW_DEG, tvcYaw);

        // Compute kinematic acceleration from TWR and gravity
        double wetMassKg = Math.max(200.0, ModularFlightPhysics.computeTotalWetMassKg(graph));
        double twr = env.gravityG() > 0.001
                ? (effectiveThrustKn * 1000.0) / (wetMassKg * env.gravityG() * PlanetaryEnvironment.STANDARD_GRAVITY_MS2)
                : (effectiveThrustKn * 1000.0) / (wetMassKg * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
        double netAccelG = Math.max(-env.gravityG(), (effectiveThrustKn * 1000.0) / (wetMassKg * PlanetaryEnvironment.STANDARD_GRAVITY_MS2) - env.gravityG());
        this.entityData.set(CURRENT_TWR, (float) twr);
        this.entityData.set(CURRENT_G_LOAD, (float) Math.max(0.0, (effectiveThrustKn * 1000.0) / (wetMassKg * PlanetaryEnvironment.STANDARD_GRAVITY_MS2)));

        // Thrust direction vector from pitch & yaw (pitch=0 is straight +Y)
        float pitchRad = currentPitch * Mth.DEG_TO_RAD;
        float yawRad = currentYaw * Mth.DEG_TO_RAD;
        double dirY = Math.cos(pitchRad);
        double horiz = Math.sin(pitchRad);
        double dirX = -Math.sin(yawRad) * horiz;
        double dirZ = Math.cos(yawRad) * horiz;

        // Realistic progressive ascent integration: net vertical acceleration scales with (TWR - 1.0) * g,
        // producing a smooth liftoff off the clamps whenever TWR > 1.0 and accelerating as propellant burns off
        double effectiveTwr = Math.max(twr, effectiveThrustKn > 0.0 ? 1.04 : 0.0);
        double gravityStep = env.gravityG() * 0.016;
        double effectiveAccelG = (effectiveThrustKn * 1000.0) / (wetMassKg * PlanetaryEnvironment.STANDARD_GRAVITY_MS2);
        double ramp = Math.min(1.0, 0.35 + 0.65 * (this.flightTicks / 60.0));
        double netG = Math.max(0.04, effectiveTwr - 1.0) * ramp;
        double accelStep = env.gravityG() > 0.001
                ? Math.min(0.11, netG * gravityStep)
                : Math.min(0.11, Math.max(0.0, effectiveAccelG * 0.018));
        double dragFactor = 1.0 - Math.min(0.015, ambientPa * 0.005);

        Vec3 vel = this.getDeltaMovement();
        double vx = (vel.x + dirX * accelStep * 0.35) * dragFactor;
        double vy = (vel.y + dirY * accelStep) * dragFactor;
        double vz = (vel.z + dirZ * accelStep * 0.35) * dragFactor;
        // Clamp terminal vertical speed so chunk streaming & rendering stay butter-smooth
        vy = Mth.clamp(vy, -2.5, 3.20);
        vx = Mth.clamp(vx, -1.2, 1.2);
        vz = Mth.clamp(vz, -1.2, 1.2);
        this.setDeltaMovement(vx, vy, vz);

        // Camera shake intensity: strong during tower clear (T+0..T+5s) and Max-Q
        float liftoffShake = this.flightTicks < 100 ? (1.0F - this.flightTicks / 100.0F) * 0.85F : 0.0F;
        float maxQShake = (float) Math.clamp(dynamicQ / ModularFlightPhysics.MAX_Q_DYNAMIC_PRESSURE_PA, 0.0, 1.0) * 0.55F;
        float flameoutShake = (this.getActiveFailureMask() & LaunchFailureEngine.FAILURE_ENGINE_FLAMEOUT) != 0 ? 0.75F : 0.0F;
        this.entityData.set(CAMERA_SHAKE, Math.min(1.0F, Math.max(liftoffShake, Math.max(maxQShake, flameoutShake))));

        // Evaluate Weibull in-flight failure checkpoints (Max-Q & Vacuum transition)
        this.evaluateInFlightWeibullChecks(serverLevel, env, ambientPa, dynamicQ);

        // Auto-staging: radial boosters first (Korolev Cross!, never inside the launch tower on tick 1..9), then hot-staging / in-line staging
        if (this.isAutopilotEnabled()) {
            if (this.flightTicks >= 10 && !propRes.depletedRadialGroupIds().isEmpty()) {
                for (String groupId : propRes.depletedRadialGroupIds()) {
                    this.detachRadialBoosterGroup(serverLevel, propRes.updatedStage(), groupId);
                }
            }
            if (propRes.coreDepleted() && !isHotStaging) {
                RocketAssemblyGraph refreshed = this.getAssemblyGraph();
                if (refreshed.activeStages().size() > 1) {
                    RocketStageNode current = refreshed.activeStage();
                    if (current != null && current.hotStaging()) {
                        this.beginHotStaging(serverLevel);
                    } else if (current != null) {
                        this.decoupleActiveStage(serverLevel, current);
                    }
                } else {
                    // All fuel exhausted on final stage -> coast
                    this.setFlightPhase(FlightPhase.COASTING);
                    this.entityData.set(THROTTLE, 0.0F);
                    this.broadcastCrewMessage(
                            Component.literal("[MECO SUBORBITAL] Combustível esgotado antes da órbita — paraquedas de emergência armados para retorno seguro!").withStyle(ChatFormatting.AQUA)
                    );
                }
            }
        }

        // Trigger advancements
        if (this.flightTicks == 5) {
            for (Entity passenger : this.getPassengers()) {
                if (passenger instanceof ServerPlayer sp) {
                    sp.awardStat(GCStats.LAUNCH_ROCKET);
                    GCTriggers.LAUNCH_ROCKET.trigger(sp);
                    FlightTelemetryEngine.onRocketAscent(serverLevel, sp);
                }
            }
        }

        // Orbital escape boundary (Y >= 1200) -> Open Celestial Screen!
        if (this.getY() >= Constant.ESCAPE_HEIGHT) {
            this.handleOrbitalEscape(serverLevel);
        }
    }

    private void tickLesAbort(ServerLevel serverLevel, PlanetaryEnvironment env, double ambientPa) {
        Vec3 vel = this.getDeltaMovement();
        if (this.lesBurnTicksRemaining > 0) {
            this.lesBurnTicksRemaining--;
            // High-TWR (> 6.0) solid escape thrust pulling capsule upward and slightly laterally clear of booster
            double escapeAccel = 0.24;
            this.setDeltaMovement(vel.x * 0.96 + 0.04, Math.min(1.6, vel.y + escapeAccel), vel.z * 0.96);
            this.entityData.set(CURRENT_TWR, (float) ModularFlightPhysics.LES_ABORT_MIN_TWR);
            this.entityData.set(CURRENT_G_LOAD, (float) ModularFlightPhysics.LES_ABORT_MIN_TWR);
            this.entityData.set(CAMERA_SHAKE, 0.85F);
            this.entityData.set(THROTTLE, 1.0F);
            return;
        }
        // Escape burn finished: coast over apogee then auto-deploy drogues + main parachutes
        this.entityData.set(THROTTLE, 0.0F);
        this.entityData.set(CAMERA_SHAKE, 0.05F);
        double gravityStep = env.gravityG() * 0.032;
        if (vel.y < -0.05) {
            this.entityData.set(PARACHUTES_DEPLOYED, true);
            this.entityData.set(PITCH_DEG, this.getPitchDeg() * 0.88F);
            this.entityData.set(ROLL_DEG, this.getRollDeg() * 0.88F);
            // Safe parachute terminal speed <= 3.6 m/s (0.18 blocks/tick, safely under 5.0 m/s touchdown limit)
            this.setDeltaMovement(vel.x * 0.90, Math.max(vel.y - gravityStep, -0.18), vel.z * 0.90);
        } else {
            this.setDeltaMovement(vel.x * 0.97, vel.y - gravityStep, vel.z * 0.97);
        }
    }

    private void tickRetroDescent(ServerLevel serverLevel, PlanetaryEnvironment env, double ambientPa) {
        RocketAssemblyGraph graph = this.getAssemblyGraph();
        double groundY = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.blockPosition().getX(), this.blockPosition().getZ());
        double agl = Math.max(0.0, this.getY() - groundY);
        this.entityData.set(AGL_BLOCKS, (float) agl);

        // Attitude control for landing minigame: WASD tilts rocket smoothly to translate over landing site & auto-levels when released
        float nextPitch = Mth.clamp(this.getPitchDeg() * 0.86F + this.pilotPitchInput * 1.4F, -22.0F, 22.0F);
        float nextRoll = Mth.clamp(this.getRollDeg() * 0.86F - this.pilotYawInput * 1.4F, -22.0F, 22.0F);
        this.entityData.set(PITCH_DEG, nextPitch);
        this.entityData.set(ROLL_DEG, nextRoll);
        this.entityData.set(TVC_PITCH_DEG, Mth.clamp(this.pilotPitchInput * 8.0F, -10.0F, 10.0F));
        this.entityData.set(TVC_YAW_DEG, Mth.clamp(this.pilotYawInput * 8.0F, -10.0F, 10.0F));

        Vec3 vel = this.getDeltaMovement();
        // Realistic planet-scaled descent gravity (Moon ~0.006/t, Mars ~0.011/t, Earth ~0.026/t)
        double gravityStep = Math.max(0.005, env.gravityG() * 0.026);
        double nextVy = vel.y - gravityStep;

        boolean hasParachutes = this.isParachutesDeployed() || hasParachutePart(graph);
        if (hasParachutes && env.pressureAtm() >= 0.05 && agl <= 180.0 && vel.y < -0.10) {
            this.entityData.set(PARACHUTES_DEPLOYED, true);
            nextVy = Math.max(nextVy, -0.22);
        }

        RocketStageNode activeStage = graph.activeStage();
        double wetMassKg = Math.max(200.0, ModularFlightPhysics.computeTotalWetMassKg(graph));
        double maxStageThrustKn = activeStage != null ? activeStage.activeThrustKn() : 0.0;
        boolean hasRetroPropellant = activeStage != null
                && (activeStage.totalCurrentFuelDroplets() > 0L || ModularFlightPhysics.resolvePropellant(activeStage.propellantId()) == PropellantType.SOLID)
                && maxStageThrustKn > 0.0;

        // Compute exact Hover Throttle (where TWR == 1.0) and Suicide Burn braking window for Landing HUD
        double weightKn = (wetMassKg * Math.max(0.05, env.gravityG()) * PlanetaryEnvironment.STANDARD_GRAVITY_MS2) / 1000.0;
        float hoverThrottle = maxStageThrustKn > 0.0
                ? (float) Math.clamp(weightKn / maxStageThrustKn, 0.08, 0.85)
                : 0.50F;
        // Max net upward braking deceleration at 100% throttle (blocks/tick^2)
        double maxBrakingStep = Math.max(0.018, gravityStep * ((1.0 / Math.max(0.12, hoverThrottle)) - 1.0));
        double currentDownSpeed = Math.max(0.0, -vel.y);
        float suicideBurnAlt = (float) Math.clamp((currentDownSpeed * currentDownSpeed) / (2.0 * Math.max(0.008, maxBrakingStep * 0.72)) + 6.0, 4.0, 260.0);

        this.entityData.set(HOVER_THROTTLE, hoverThrottle);
        this.entityData.set(SUICIDE_BURN_ALT, suicideBurnAlt);

        // Determine effective throttle: manual minigame (full 0..100% + X cutoff) vs optional Autopilot (Shift+G)
        float effectiveThrottle;
        if (this.isAutopilotEnabled()) {
            if (graph.hasLandingGear() && agl <= 120.0 && !this.isLandingGearDeployed()) {
                this.entityData.set(LANDING_GEAR_DEPLOYED, true);
            }
            if (!hasRetroPropellant) {
                effectiveThrottle = 0.0F;
            } else if (agl <= suicideBurnAlt + 12.0 || vel.y < -0.55) {
                double targetVy = agl <= 22.0 ? -0.11 : (agl <= 55.0 ? -0.22 : -0.40);
                double err = targetVy - vel.y; // positive means falling too fast -> need more throttle
                effectiveThrottle = (float) Math.clamp(hoverThrottle + err * 2.4, 0.0, 1.0);
            } else {
                effectiveThrottle = 0.0F;
            }
            this.targetThrottle = effectiveThrottle;
        } else {
            effectiveThrottle = hasRetroPropellant ? Math.clamp(this.targetThrottle, 0.0F, 1.0F) : 0.0F;
        }

        if (effectiveThrottle > 0.005F && hasRetroPropellant && activeStage != null) {
            ModularFlightPhysics.PropulsionTickResult res = ModularFlightPhysics.stepStagePropulsion(activeStage, ambientPa, effectiveThrottle, false);
            this.setAssemblyGraph(graph.withUpdatedStage(res.updatedStage()));
            this.entityData.set(THROTTLE, effectiveThrottle);
            this.entityData.set(CAMERA_SHAKE, effectiveThrottle * 0.28F);

            // Real proportional thrust acceleration: throttle == hoverThrottle hovers, > hover brakes/climbs, < hover descends!
            double thrustAccel = gravityStep * (effectiveThrottle / Math.max(0.10, hoverThrottle));
            double cosTilt = Math.cos(Math.toRadians(Math.hypot(nextPitch, nextRoll)));
            nextVy = vel.y + (thrustAccel * cosTilt) - gravityStep;
            double effectiveTwr = effectiveThrottle / Math.max(0.10, hoverThrottle);
            this.entityData.set(CURRENT_TWR, (float) effectiveTwr);
            this.entityData.set(CURRENT_G_LOAD, (float) (effectiveTwr * Math.max(0.05, env.gravityG())));
        } else {
            this.entityData.set(THROTTLE, 0.0F);
            this.entityData.set(CAMERA_SHAKE, 0.0F);
            this.entityData.set(CURRENT_TWR, 0.0F);
            this.entityData.set(CURRENT_G_LOAD, 0.0F);
        }

        nextVy = Mth.clamp(nextVy, -1.65, 0.85);
        // Horizontal RCS / tilted-thrust steering so the player can align smoothly over flat terrain
        double thrustLateralBoost = 1.0 + effectiveThrottle * 1.4;
        double steerX = this.pilotYawInput * 0.022 * thrustLateralBoost;
        double steerZ = this.pilotPitchInput * 0.022 * thrustLateralBoost;
        this.setDeltaMovement(
                Mth.clamp(vel.x * 0.92 + steerX, -0.65, 0.65),
                nextVy,
                Mth.clamp(vel.z * 0.92 + steerZ, -0.65, 0.65)
        );
    }

    private void tickCoasting(ServerLevel serverLevel, PlanetaryEnvironment env, double ambientPa) {
        this.entityData.set(THROTTLE, 0.0F);
        this.entityData.set(CAMERA_SHAKE, 0.0F);
        double gravityStep = env.gravityG() * 0.032;
        Vec3 vel = this.getDeltaMovement();
        double nextVx = vel.x * 0.995;
        double nextVy = vel.y - gravityStep;
        double nextVz = vel.z * 0.995;

        if (this.suborbitalAscentAttempt && vel.y < -0.05) {
            if (!this.isParachutesDeployed()) {
                this.entityData.set(PARACHUTES_DEPLOYED, true);
                this.broadcastCrewMessage(
                        Component.literal("[RECUPERAÇÃO SUBORBITAL] Paraquedas de emergência acionados no apogeu — descida controlada sem explosão.").withStyle(ChatFormatting.GREEN)
                );
            }
            if (this.getAssemblyGraph().hasLandingGear()) {
                this.entityData.set(LANDING_GEAR_DEPLOYED, true);
            }
            nextVx *= 0.92;
            nextVz *= 0.92;
            nextVy = Math.max(nextVy, -0.18);
            this.entityData.set(PITCH_DEG, this.getPitchDeg() * 0.85F);
            this.entityData.set(ROLL_DEG, this.getRollDeg() * 0.85F);
        }

        this.setDeltaMovement(nextVx, nextVy, nextVz);

        if (this.getY() >= Constant.ESCAPE_HEIGHT) {
            this.handleOrbitalEscape(serverLevel);
            return;
        }
        if (!this.suborbitalAscentAttempt && vel.y < -0.25) {
            this.setFlightPhase(FlightPhase.RETRO_DESCENT);
        }
    }

    private void evaluateInFlightWeibullChecks(
            ServerLevel serverLevel,
            PlanetaryEnvironment env,
            double ambientPa,
            double dynamicQ
    ) {
        RocketAssemblyGraph graph = this.getAssemblyGraph();
        // Checkpoint 1: Max-Q aerodynamic buffeting (Y in 80..180, P_a > 0.05)
        if (!this.maxQFailureChecked && env.pressureAtm() >= 0.05 && this.getY() >= 90.0 && this.getY() <= 180.0) {
            this.maxQFailureChecked = true;
            float stress = LaunchFailureEngine.computeMaxQBendingStress(
                    graph.totalHeightBlocks(),
                    Math.max(1.0, graph.maxRadiusBlocks() * 2.0),
                    this.getPitchDeg(),
                    graph.hasFairing()
            );
            for (MountedPartNode part : graph.allActiveParts()) {
                if (part.config().category() == RocketPartCategory.TANK
                        && LaunchFailureEngine.rollFailure(part.integrity(), stress, serverLevel.random)) {
                    this.addFailureFlag(LaunchFailureEngine.FAILURE_FUEL_LEAK);
                    this.broadcastCrewMessage(
                            Component.literal("[FALHA MAX-Q] Vazamento de propelente detectado!").withStyle(ChatFormatting.GOLD)
                    );
                    break;
                }
                if (part.config().category() == RocketPartCategory.ENGINE
                        && LaunchFailureEngine.rollFailure(part.integrity(), stress, serverLevel.random)) {
                    this.addFailureFlag(LaunchFailureEngine.FAILURE_ENGINE_FLAMEOUT);
                    this.broadcastCrewMessage(
                            Component.literal("[FALHA MAX-Q] Instabilidade de combustão / Flameout no motor! Considere LES Abort (Backspace)!").withStyle(ChatFormatting.RED)
                    );
                    break;
                }
            }
        }

        // Checkpoint 2: Vacuum boundary transition (Y in 350..450)
        if (!this.vacuumFailureChecked && this.getY() >= 350.0) {
            this.vacuumFailureChecked = true;
            for (MountedPartNode part : graph.allActiveParts()) {
                if (part.config().category() == RocketPartCategory.COMMAND
                        && LaunchFailureEngine.rollFailure(part.integrity(), LaunchFailureEngine.STRESS_VACUUM_TRANSITION, serverLevel.random)) {
                    this.addFailureFlag(LaunchFailureEngine.FAILURE_CABIN_BREACH);
                    this.broadcastCrewMessage(
                            Component.literal("[ALERTA CRÍTICO] Despressurização da cabine! Verifique traje espacial!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    );
                    break;
                }
            }
        }
    }

    private void tickActiveFailures(ServerLevel serverLevel, PlanetaryEnvironment env) {
        int mask = this.getActiveFailureMask();
        if (mask == LaunchFailureEngine.FAILURE_NONE) {
            return;
        }
        // Mode 2: Fuel Leak drains active stage tank rapidly + spawns vapor trail
        if ((mask & LaunchFailureEngine.FAILURE_FUEL_LEAK) != 0) {
            RocketAssemblyGraph graph = this.getAssemblyGraph();
            RocketStageNode stage = graph.activeStage();
            if (stage != null && stage.currentFuelDroplets() > 0L) {
                double minTankI = 1.0;
                for (MountedPartNode p : stage.coreParts()) {
                    if (p.config().category() == RocketPartCategory.TANK) {
                        minTankI = Math.min(minTankI, p.integrity().fraction());
                    }
                }
                long leak = LaunchFailureEngine.computeFuelLeakDropletsPerTick(minTankI, this.getDeltaMovement().length() * 20.0, 45.0);
                this.setAssemblyGraph(graph.withUpdatedStage(stage.withCoreFuel(Math.max(0L, stage.currentFuelDroplets() - leak))));
                if ((this.flightTicks % 2) == 0) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 2.0, this.getZ(), 4, 0.4, 0.4, 0.4, 0.05);
                }
            }
        }
        // Mode 3: Cabin Breach depressurization -> suffocates unsuited crew every 20 ticks
        if ((mask & LaunchFailureEngine.FAILURE_CABIN_BREACH) != 0) {
            this.cabinBreachTicks++;
            double pressureFrac = LaunchFailureEngine.computeCabinPressureFraction(this.cabinBreachTicks);
            if (pressureFrac < 0.25 && (this.cabinBreachTicks % 20) == 0) {
                for (Entity passenger : this.getPassengers()) {
                    if (passenger instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator()) {
                        if (!sp.galacticraft$hasMaskAndGear()) {
                            sp.hurt(serverLevel.damageSources().source(GCDamageTypes.SUFFOCATION), 4.0F);
                        }
                    }
                }
            }
        }
        // Mode 4: Engine Flameout counter
        if ((mask & LaunchFailureEngine.FAILURE_ENGINE_FLAMEOUT) != 0) {
            this.flameoutTicks++;
            if ((this.flameoutTicks % 4) == 0) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 5, 0.5, 0.2, 0.5, 0.06);
            }
        }
    }

    private void beginHotStaging(ServerLevel serverLevel) {
        this.setFlightPhase(FlightPhase.HOT_STAGING);
        this.entityData.set(HOT_STAGE_TICKS, ModularFlightPhysics.HOT_STAGING_PRE_IGNITION_TICKS);
        serverLevel.playSound(null, this.blockPosition(), GCSounds.ROCKET_IGNITE, SoundSource.NEUTRAL, 1.3F, 1.25F);
        this.broadcastCrewMessage(
                Component.literal("[HOT-STAGING] Pré-ignição do estágio superior (1.5s)...").withStyle(ChatFormatting.GOLD)
        );
    }

    /**
     * Separates a radial booster group using Korolev Cross kinematics:
     * outward radial impulse ({@code 2.50..3.50 m/s}) + nose pitch-out ({@code 18.0 deg/s}).
     */
    public void detachRadialBoosterGroup(@NotNull ServerLevel serverLevel, @NotNull RocketStageNode stage, @NotNull String groupId) {
        RadialBoosterGroup target = null;
        for (RadialBoosterGroup g : stage.radialGroups()) {
            if (!g.decoupled() && g.groupId().equals(groupId)) {
                target = g;
                break;
            }
        }
        if (target == null) {
            return;
        }

        // Partition booster parts by radial (X, Z) column so each physical booster peels outward in a true Korolev Cross!
        Map<Long, List<MountedPartNode>> columns = new LinkedHashMap<>();
        for (MountedPartNode part : target.parts()) {
            long colKey = (((long) part.relativePos().getX()) << 32) ^ (part.relativePos().getZ() & 0xFFFFFFFFL);
            columns.computeIfAbsent(colKey, k -> new ArrayList<>()).add(part);
        }

        double residualFuelFrac = target.fuelCapacityDroplets() > 0L
                ? (double) target.currentFuelDroplets() / (double) target.fuelCapacityDroplets()
                : 0.0;
        Quaternionf rot = buildAttitudeQuaternion(this.getYawDeg(), this.getPitchDeg(), this.getRollDeg());

        for (List<MountedPartNode> colParts : columns.values()) {
            MountedPartNode base = colParts.get(0);
            double rx = base.relativePos().getX();
            double rz = base.relativePos().getZ();
            double dist = Math.hypot(rx, rz);
            double nx = dist > 1.0e-4 ? (rx / dist) : 1.0;
            double nz = dist > 1.0e-4 ? (rz / dist) : 0.0;

            Vector3f radialWorld = new Vector3f((float) nx, 0.0F, (float) nz);
            rot.transform(radialWorld);
            Vector3f offsetWorld = new Vector3f((float) rx, (float) base.relativePos().getY(), (float) rz);
            rot.transform(offsetWorld);

            double sepSpeedBlocksPerTick = (2.5 + serverLevel.random.nextDouble()) / 20.0; // 2.50..3.50 m/s
            Vec3 sepVel = this.getDeltaMovement().add(
                    radialWorld.x * sepSpeedBlocksPerTick,
                    radialWorld.y * sepSpeedBlocksPerTick - 0.02,
                    radialWorld.z * sepSpeedBlocksPerTick
            );

            // Re-center part relative positions around the detached booster's own base
            List<MountedPartNode> localParts = new ArrayList<>(colParts.size());
            int minY = Integer.MAX_VALUE;
            for (MountedPartNode p : colParts) {
                minY = Math.min(minY, p.relativePos().getY());
            }
            for (MountedPartNode p : colParts) {
                localParts.add(new MountedPartNode(
                        new BlockPos(0, p.relativePos().getY() - minY, 0),
                        p.config(),
                        p.stats(),
                        p.integrity(),
                        p.composition(),
                        0,
                        false
                ));
            }

            // Anti-boomerang outward nose pitch-out angular velocity (~18 deg/s) aligned away from core!
            float pitchOutSign = (float) (nz >= 0.0 ? 1.0 : -1.0);
            float rollOutSign = (float) (nx >= 0.0 ? -1.0 : 1.0);
            float angPitch = (Math.abs(nz) >= Math.abs(nx))
                    ? (pitchOutSign * DetachedStageEntity.KOROLEV_PITCH_OUT_DEG_PER_TICK)
                    : 0.0F;
            float angRoll = (Math.abs(nx) > Math.abs(nz))
                    ? (rollOutSign * DetachedStageEntity.KOROLEV_PITCH_OUT_DEG_PER_TICK)
                    : 0.0F;
            if (angPitch == 0.0F && angRoll == 0.0F) {
                angPitch = DetachedStageEntity.KOROLEV_PITCH_OUT_DEG_PER_TICK;
            }

            DetachedStageEntity boosterEntity = DetachedStageEntity.create(
                    serverLevel,
                    this.position().add(offsetWorld.x, offsetWorld.y, offsetWorld.z),
                    sepVel,
                    localParts,
                    DetachedStageEntity.SeparationKind.RADIAL_BOOSTER,
                    this.getPitchDeg(),
                    this.getYawDeg(),
                    this.getRollDeg(),
                    angPitch,
                    0.0F,
                    angRoll,
                    residualFuelFrac,
                    this.originTerminalPos
            );
            serverLevel.addFreshEntity(boosterEntity);
        }

        RocketAssemblyGraph updated = this.getAssemblyGraph().withoutRadialGroup(stage.stageIndex(), groupId);
        this.setAssemblyGraph(updated);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 1.4F, 0.75F);
        this.broadcastCrewMessage(
                Component.literal("[KOROLEV CROSS] Boosters radiais " + target.symmetry().getSerializedName() + " separados!").withStyle(ChatFormatting.AQUA)
        );
    }

    /**
     * Decouples the active lower stage, spawning a {@link DetachedStageEntity} behind the rocket
     * along {@code -u_axis * 2.50 m/s} and advancing propulsion to the next upper stage.
     */
    public void decoupleActiveStage(@NotNull ServerLevel serverLevel, @NotNull RocketStageNode stage) {
        if (stage.decoupled()) {
            return;
        }
        // Detach any remaining radial boosters along with this stage
        List<MountedPartNode> detachedParts = stage.allActiveParts();
        if (!detachedParts.isEmpty()) {
            double residualFuelFrac = stage.fuelCapacityDroplets() > 0L
                    ? (double) stage.currentFuelDroplets() / (double) stage.fuelCapacityDroplets()
                    : 0.0;
            double sepSpeed = DetachedStageEntity.INLINE_SEPARATION_SPEED_MS / 20.0;
            Vec3 sepVel = this.getDeltaMovement().add(0.0, -sepSpeed, 0.0);
            DetachedStageEntity lower = DetachedStageEntity.create(
                    serverLevel,
                    this.position(),
                    sepVel,
                    detachedParts,
                    DetachedStageEntity.SeparationKind.INLINE_STAGE,
                    this.getPitchDeg(),
                    this.getYawDeg(),
                    this.getRollDeg(),
                    0.35F,
                    0.15F,
                    0.20F,
                    residualFuelFrac,
                    this.originTerminalPos
            );
            serverLevel.addFreshEntity(lower);
        }

        RocketAssemblyGraph updated = this.getAssemblyGraph().withoutStage(stage.stageIndex());
        this.setAssemblyGraph(updated);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 1.5F, 0.7F);
        serverLevel.playSound(null, this.blockPosition(), GCSounds.ROCKET_IGNITE, SoundSource.NEUTRAL, 1.2F, 1.15F);
        this.broadcastCrewMessage(
                Component.literal("[MECO / STAGING] Estágio " + stage.stageIndex() + " separado! Estágio superior ativo!").withStyle(ChatFormatting.GREEN)
        );
    }

    /**
     * Ejects clamshell fairing halves laterally at {@code +/- 4.00 m/s} and strips fairings from the
     * active {@link RocketAssemblyGraph}.
     */
    public void jettisonFairings(@NotNull ServerLevel serverLevel) {
        RocketAssemblyGraph graph = this.getAssemblyGraph();
        if (!graph.hasFairing()) {
            return;
        }
        List<MountedPartNode> fairingParts = new ArrayList<>();
        for (MountedPartNode part : graph.allActiveParts()) {
            if (RocketAssemblyGraph.isFairingPart(part)) {
                fairingParts.add(part);
            }
        }
        if (!fairingParts.isEmpty()) {
            double latSpeed = DetachedStageEntity.FAIRING_SEPARATION_SPEED_MS / 20.0;
            for (int side : new int[]{-1, 1}) {
                Vec3 sideVel = this.getDeltaMovement().add(side * latSpeed, 0.02, 0.0);
                DetachedStageEntity half = DetachedStageEntity.create(
                        serverLevel,
                        this.position().add(side * 1.2, Math.max(2.0, graph.totalHeightBlocks() - 3.0), 0.0),
                        sideVel,
                        fairingParts,
                        DetachedStageEntity.SeparationKind.FAIRING_HALF,
                        this.getPitchDeg(),
                        this.getYawDeg(),
                        this.getRollDeg(),
                        0.6F,
                        0.0F,
                        side * -1.4F,
                        0.0,
                        null
                );
                serverLevel.addFreshEntity(half);
            }
        }
        this.setAssemblyGraph(graph.withoutFairings());
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.NEUTRAL, 1.3F, 1.3F);
        this.broadcastCrewMessage(
                Component.literal("[FAIRING JETTISON] Carenagens aerodinâmicas ejetadas (+Delta-V)!").withStyle(ChatFormatting.AQUA)
        );
    }

    private void handleOrbitalEscape(ServerLevel serverLevel) {
        RocketAssemblyGraph currentGraph = this.getAssemblyGraph();
        List<Entity> passengers = new ArrayList<>(this.getPassengers());
        if (passengers.isEmpty()) {
            this.discard();
            return;
        }

        for (Entity entity : passengers) {
            if (entity instanceof ServerPlayer sp) {
                FlightTelemetryEngine.onRocketReachOrbit(serverLevel, sp);
                GCServerPlayer gcPlayer = GCServerPlayer.get(sp);
                gcPlayer.setModularRocketGraph(currentGraph);
                gcPlayer.setModularRocketEnergy(this.storedEnergyGj);
                gcPlayer.setModularRocketOxygen(this.storedOxygenDroplets);
                gcPlayer.setFuel(currentGraph.totalCurrentFuelDroplets());
                gcPlayer.setRocketStacks(this.copyCargoStacks());

                // Provide synthetic RocketData so CelestialSelectionScreen allows selecting any reachable planet
                RocketData navData = RocketPrefabs.TIER_1;
                gcPlayer.setRocketData(navData);

                sp.galacticraft$openCelestialScreen(navData);
                ServerPlayNetworking.send(sp, new OpenCelestialScreenPayload(
                        navData,
                        serverLevel.galacticraft$getCelestialBody(),
                        Galacticraft.CONFIG.enableSpaceStationCreation(),
                        GCConfigUtil.disabledCelestialScreenDestinations(sp.getServer())
                ));
            }
        }
        this.ejectPassengers();
        this.remove(RemovalReason.UNLOADED_WITH_PLAYER);
    }

    private void checkTouchdownOrCrash(ServerLevel serverLevel, Vec3 preMove) {
        if (!this.onGround() && !this.verticalCollision) {
            return;
        }
        double impactSpeedMs = Math.abs(preMove.y) * 20.0;
        double horizSpeedMs = Math.hypot(preMove.x, preMove.z) * 20.0;
        double tiltDeg = Math.hypot(this.getPitchDeg(), this.getRollDeg());
        FlightPhase phase = this.getFlightPhase();

        // Never explode or trigger landing-minigame rank while still inside the launch tower before clearing it!
        if ((phase == FlightPhase.ASCENT || phase == FlightPhase.HOT_STAGING) && this.maxReachedAgl < 8.0) {
            this.performTowerAbortRecovery(serverLevel);
            return;
        }

        if (phase == FlightPhase.LES_ABORT && (this.isParachutesDeployed() || impactSpeedMs <= ModularFlightPhysics.SAFE_TOUCHDOWN_SPEED_MS + 2.0)) {
            this.performSafeTouchdown(serverLevel, impactSpeedMs, horizSpeedMs, tiltDeg);
            return;
        }

        if (this.suborbitalAscentAttempt || this.isParachutesDeployed()) {
            this.performSafeTouchdown(
                    serverLevel,
                    Math.min(impactSpeedMs, ModularFlightPhysics.SAFE_TOUCHDOWN_SPEED_MS),
                    Math.min(horizSpeedMs, 1.0),
                    0.0
            );
            return;
        }

        if (impactSpeedMs <= ModularFlightPhysics.SAFE_TOUCHDOWN_SPEED_MS && tiltDeg <= 22.0) {
            this.performSafeTouchdown(serverLevel, impactSpeedMs, horizSpeedMs, tiltDeg);
        } else {
            this.triggerCrashExplosion(serverLevel);
        }
    }

    private void performTowerAbortRecovery(ServerLevel serverLevel) {
        BlockPos landingPos = BlockPos.containing(this.getX(), Math.floor(this.getY() + 0.1), this.getZ());
        List<Entity> passengers = new ArrayList<>(this.getPassengers());
        boolean reconverted = RocketStateConverter.convertToStaticBlocks(serverLevel, landingPos, this);
        serverLevel.playSound(null, landingPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.7F);
        String msg = reconverted
                ? "[ABORTAGEM NO PAD] Empuxo insuficiente para deixar a torre — foguete preservado no pad!"
                : "[ABORTAGEM NO PAD] Lançamento cancelado junto à torre — tripulação desembarcada com segurança.";
        for (Entity passenger : passengers) {
            if (passenger instanceof ServerPlayer sp) {
                ServerPlayNetworking.send(sp, new ResetPerspectivePacket());
                sp.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), false);
            }
        }
        if (!reconverted) {
            this.setFlightPhase(FlightPhase.LANDED);
            this.setDeltaMovement(Vec3.ZERO);
            this.ejectPassengers();
            this.discard();
        }
    }

    private void performSafeTouchdown(ServerLevel serverLevel, double impactSpeedMs, double horizSpeedMs, double tiltDeg) {
        BlockPos landingPos = BlockPos.containing(this.getX(), Math.floor(this.getY() + 0.1), this.getZ());
        List<Entity> passengers = new ArrayList<>(this.getPassengers());
        boolean gearOk = !this.getAssemblyGraph().hasLandingGear() || this.isLandingGearDeployed();

        // Grade the landing minigame performance!
        String rankLabel;
        ChatFormatting rankColor;
        if (impactSpeedMs <= 2.2 && tiltDeg <= 6.0 && horizSpeedMs <= 2.0 && gearOk) {
            rankLabel = "★ RANK S — POUSO PERFEITO";
            rankColor = ChatFormatting.AQUA;
            serverLevel.playSound(null, landingPos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9F, 1.0F);
        } else if (impactSpeedMs <= 3.8 && tiltDeg <= 12.0 && gearOk) {
            rankLabel = "★ RANK A — POUSO NOMINAL";
            rankColor = ChatFormatting.GREEN;
            serverLevel.playSound(null, landingPos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.15F);
        } else {
            rankLabel = gearOk ? "RANK B — POUSO BRUSCO" : "RANK C — TREM RECOLHIDO";
            rankColor = ChatFormatting.YELLOW;
            serverLevel.playSound(null, landingPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.9F);
        }

        // Keep the rocket standing intact as a single unified structure (no dismantling!) so players can inspect inventory, board, and re-launch
        this.setFlightPhase(FlightPhase.LANDED);
        this.targetThrottle = 0.0F;
        this.entityData.set(THROTTLE, 0.0F);
        this.entityData.set(CAMERA_SHAKE, 0.0F);
        this.entityData.set(PITCH_DEG, 0.0F);
        this.entityData.set(ROLL_DEG, 0.0F);
        this.entityData.set(TVC_PITCH_DEG, 0.0F);
        this.entityData.set(TVC_YAW_DEG, 0.0F);
        this.entityData.set(PARACHUTES_DEPLOYED, false);
        this.entityData.set(CURRENT_TWR, 0.0F);
        this.setDeltaMovement(Vec3.ZERO);

        String statsSummary = String.format(
                Locale.ROOT,
                " [%s | Vy: -%.1f m/s | Inclinação: %.1f° | Nave estacionada intacta — Shift+Clique Dir. para carga ou Clique Dir. para embarcar]",
                rankLabel,
                impactSpeedMs,
                tiltDeg
        );

        // Safely dismount passengers onto solid ground beside the landed rocket
        this.ejectPassengers();
        double safeOffset = Math.max(2.2, this.getMaxRadiusBlocks() + 1.35);
        for (int i = 0; i < passengers.size(); i++) {
            Entity passenger = passengers.get(i);
            double angle = (Math.PI * 2.0 * i) / Math.max(1, passengers.size());
            double px = this.getX() + Math.cos(angle) * safeOffset;
            double pz = this.getZ() + Math.sin(angle) * safeOffset;
            double py = Math.max(this.getY() + 0.25, serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(px, this.getY(), pz).getX(), BlockPos.containing(px, this.getY(), pz).getZ()));
            passenger.setDeltaMovement(Vec3.ZERO);
            passenger.fallDistance = 0.0F;
            passenger.teleportTo(px, py, pz);
            if (passenger instanceof ServerPlayer sp) {
                sp.awardStat(GCStats.SAFE_LANDING);
                GCTriggers.SAFE_LANDING.trigger(sp);
                ServerPlayNetworking.send(sp, new ResetPerspectivePacket());
                sp.displayClientMessage(Component.literal(statsSummary).withStyle(rankColor, ChatFormatting.BOLD), false);
            }
        }
    }

    private void triggerCrashExplosion(ServerLevel serverLevel) {
        this.crashed = true;
        float blastPower = LaunchFailureEngine.calculateExplosionPower(this.getAssemblyGraph().totalCurrentFuelDroplets());
        for (Entity passenger : new ArrayList<>(this.getPassengers())) {
            passenger.stopRiding();
            if (passenger instanceof ServerPlayer sp) {
                sp.awardStat(GCStats.CRASH_LANDING);
                ServerPlayNetworking.send(sp, new ResetPerspectivePacket());
            }
        }
        serverLevel.explode(
                this,
                serverLevel.damageSources().source(GCDamageTypes.CRASH_LANDING),
                null,
                this.getX(),
                this.getY() + 1.0,
                this.getZ(),
                blastPower,
                true,
                Level.ExplosionInteraction.TNT
        );
        Containers.dropContents(serverLevel, this.blockPosition(), this.cargoInventory);
        this.discard();
    }

    private void spawnClientFlightParticles() {
        FlightPhase phase = this.getFlightPhase();
        float throttle = this.getThrottle();
        if (throttle <= 0.02F && phase != FlightPhase.LES_ABORT) {
            return;
        }
        Level level = this.level();
        int count = phase == FlightPhase.LES_ABORT ? 4 : Math.max(1, Math.round(throttle * 4.0F));
        Entity pilot = this.getFirstPassenger();
        EntityParticleOption flameOption = new EntityParticleOption(
                GCParticleTypes.LAUNCH_FLAME_LAUNCHED,
                pilot != null ? pilot.getUUID() : this.getUUID()
        );
        for (int i = 0; i < count; i++) {
            double ox = (level.random.nextDouble() - 0.5) * Math.max(0.6, this.getMaxRadiusBlocks());
            double oz = (level.random.nextDouble() - 0.5) * Math.max(0.6, this.getMaxRadiusBlocks());
            level.addParticle(
                    flameOption,
                    this.getX() + ox,
                    this.getY() - 0.35,
                    this.getZ() + oz,
                    (level.random.nextDouble() - 0.5) * 0.12,
                    -0.75 - level.random.nextDouble() * 0.4,
                    (level.random.nextDouble() - 0.5) * 0.12
            );
            if (level.random.nextInt(2) == 0) {
                level.addParticle(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        this.getX() + ox * 1.4,
                        this.getY() - 0.8,
                        this.getZ() + oz * 1.4,
                        (level.random.nextDouble() - 0.5) * 0.18,
                        -0.25,
                        (level.random.nextDouble() - 0.5) * 0.18
                );
            }
        }
    }

    private void addFailureFlag(int flag) {
        this.entityData.set(ACTIVE_FAILURE_MASK, this.getActiveFailureMask() | flag);
    }

    private void broadcastCrewMessage(Component message) {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof ServerPlayer sp) {
                sp.displayClientMessage(message, false);
            }
        }
    }

    private static boolean hasCommandModule(RocketAssemblyGraph graph) {
        for (MountedPartNode part : graph.allActiveParts()) {
            if (part.config().category() == RocketPartCategory.COMMAND) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasParachutePart(RocketAssemblyGraph graph) {
        for (MountedPartNode part : graph.allActiveParts()) {
            String id = (part.config().familyId() + "_" + part.config().modelId()).toLowerCase(Locale.ROOT);
            if (id.contains("parachute") || id.contains("chute")) {
                return true;
            }
        }
        return false;
    }

    private static List<MountedPartNode> collectNonCapsuleParts(RocketAssemblyGraph graph) {
        List<MountedPartNode> result = new ArrayList<>();
        for (MountedPartNode part : graph.allActiveParts()) {
            if (part.config().category() != RocketPartCategory.COMMAND) {
                result.add(part);
            }
        }
        return result;
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        IntConsumer hook = CLIENT_REMOVAL_HOOK;
        if (hook != null) {
            hook.accept(this.getId());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("AssemblyGraph")) {
            RocketAssemblyGraph.CODEC.parse(NbtOps.INSTANCE, tag.get("AssemblyGraph"))
                    .result()
                    .ifPresent(this::setAssemblyGraph);
        }
        this.entityData.set(FLIGHT_PHASE, tag.getInt("FlightPhase"));
        this.entityData.set(THROTTLE, tag.contains("Throttle") ? tag.getFloat("Throttle") : 1.0F);
        this.targetThrottle = this.getThrottle();
        this.entityData.set(PITCH_DEG, tag.getFloat("PitchDeg"));
        this.entityData.set(YAW_DEG, tag.getFloat("YawDeg"));
        this.entityData.set(ROLL_DEG, tag.getFloat("RollDeg"));
        this.entityData.set(AUTOPILOT_ENABLED, !tag.contains("Autopilot") || tag.getBoolean("Autopilot"));
        this.entityData.set(LANDING_GEAR_DEPLOYED, tag.getBoolean("LandingGearDeployed"));
        this.entityData.set(PARACHUTES_DEPLOYED, tag.getBoolean("ParachutesDeployed"));
        this.entityData.set(ACTIVE_FAILURE_MASK, tag.getInt("FailureMask"));
        this.entityData.set(HOT_STAGE_TICKS, tag.getInt("HotStageTicks"));
        this.launchSurfaceY = tag.contains("LaunchSurfaceY") ? tag.getDouble("LaunchSurfaceY") : 64.0;
        this.maxReachedAgl = Math.max(0.0, tag.getDouble("MaxReachedAgl"));
        this.storedEnergyGj = Math.max(0L, tag.getLong("StoredEnergyGj"));
        this.storedOxygenDroplets = Math.max(0L, tag.getLong("StoredOxygen"));
        Direction loadedFacing = Direction.byName(tag.getString("PadFacing"));
        this.padFacing = (loadedFacing != null && loadedFacing.getAxis().isHorizontal()) ? loadedFacing : Direction.NORTH;
        this.originTerminalPos = tag.contains("OriginTerminalPos", Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong("OriginTerminalPos"))
                : null;
        this.flightTicks = tag.getInt("FlightTicks");
        this.emptyWatchdogTicks = tag.getInt("EmptyWatchdogTicks");
        this.lesBurnTicksRemaining = tag.getInt("LesBurnTicks");
        this.suborbitalAscentAttempt = tag.getBoolean("SuborbitalAscentAttempt");

        this.crewUuids.clear();
        if (tag.contains("CrewUuids", Tag.TAG_LIST)) {
            ListTag list = tag.getList("CrewUuids", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.hasUUID("Id")) {
                    this.crewUuids.add(entry.getUUID("Id"));
                }
            }
        }
        NonNullList<ItemStack> loadedCargo = NonNullList.withSize(this.cargoInventory.getContainerSize(), ItemStack.EMPTY);
        if (tag.contains("Cargo", Tag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(tag.getCompound("Cargo"), loadedCargo, this.registryAccess());
        }
        this.loadCargoStacks(loadedCargo);
        this.refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        RocketAssemblyGraph.CODEC.encodeStart(NbtOps.INSTANCE, this.getAssemblyGraph())
                .result()
                .ifPresent(encoded -> tag.put("AssemblyGraph", encoded));
        tag.putInt("FlightPhase", this.entityData.get(FLIGHT_PHASE));
        tag.putFloat("Throttle", this.getThrottle());
        tag.putFloat("PitchDeg", this.getPitchDeg());
        tag.putFloat("YawDeg", this.getYawDeg());
        tag.putFloat("RollDeg", this.getRollDeg());
        tag.putBoolean("Autopilot", this.isAutopilotEnabled());
        tag.putBoolean("LandingGearDeployed", this.isLandingGearDeployed());
        tag.putBoolean("ParachutesDeployed", this.isParachutesDeployed());
        tag.putInt("FailureMask", this.getActiveFailureMask());
        tag.putInt("HotStageTicks", this.getHotStageTicks());
        tag.putDouble("LaunchSurfaceY", this.launchSurfaceY);
        tag.putDouble("MaxReachedAgl", this.maxReachedAgl);
        tag.putLong("StoredEnergyGj", this.storedEnergyGj);
        tag.putLong("StoredOxygen", this.storedOxygenDroplets);
        tag.putString("PadFacing", this.padFacing.getSerializedName());
        if (this.originTerminalPos != null) {
            tag.putLong("OriginTerminalPos", this.originTerminalPos.asLong());
        }
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putInt("EmptyWatchdogTicks", this.emptyWatchdogTicks);
        tag.putInt("LesBurnTicks", this.lesBurnTicksRemaining);
        tag.putBoolean("SuborbitalAscentAttempt", this.suborbitalAscentAttempt);

        if (!this.crewUuids.isEmpty()) {
            ListTag list = new ListTag();
            for (UUID uuid : this.crewUuids) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Id", uuid);
                list.add(entry);
            }
            tag.put("CrewUuids", list);
        }
        CompoundTag cargoTag = new CompoundTag();
        ContainerHelper.saveAllItems(cargoTag, this.copyCargoStacks(), this.registryAccess());
        tag.put("Cargo", cargoTag);
    }
}
