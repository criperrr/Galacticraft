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

import dev.galacticraft.api.entity.ControllableEntity;
import dev.galacticraft.api.entity.IgnoreShift;
import dev.galacticraft.api.vehicle.HardpointType;
import dev.galacticraft.api.vehicle.SeatRole;
import dev.galacticraft.api.vehicle.SeatSpec;
import dev.galacticraft.api.vehicle.VehicleAnimState;
import dev.galacticraft.api.vehicle.VehicleChassisDefinition;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import dev.galacticraft.api.vehicle.VehicleDerivedStats;
import dev.galacticraft.api.vehicle.VehicleHardpoint;
import dev.galacticraft.api.vehicle.VehiclePartDefinition;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.api.vehicle.VehicleSizeClass;
import dev.galacticraft.api.vehicle.ai.AutonomousVehicleContext;
import dev.galacticraft.api.vehicle.ai.ControlCommand;
import dev.galacticraft.api.vehicle.ai.VehicleAiState;
import dev.galacticraft.api.vehicle.ai.VehicleMissionConfig;
import dev.galacticraft.api.vehicle.ai.VehicleMissionRole;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.data.GCEntityDataSerializers;
import dev.galacticraft.mod.content.item.DroneRemoteControllerItem;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.VehicleCrateItem;
import dev.galacticraft.mod.content.item.VehiclePartItem;
import dev.galacticraft.mod.content.item.VehicleWrenchItem;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.content.vehicle.ai.VehicleMissionController;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Modular planetary vehicle entity supporting physical hardpoint assembly in-world,
 * {@link PlanetaryEnvironment}-coupled ground physics, multi-seat mounting,
 * multi-part child hitboxes for {@code MEDIUM}/{@code LARGE} chassis, and
 * packing into a {@link VehicleCrateItem} for rocket cargo transport.
 */
public class ModularVehicleEntity extends GCVehicle implements ControllableEntity, IgnoreShift, AutonomousVehicleContext {
    public static final float MAX_CYCLIC_TILT_DEG = 22.0F;
    public static final float BLUR_DISC_RPM_THRESHOLD = 600.0F;
    public static final double GEAR_RETRACT_AGL_BLOCKS = 4.0;

    private static final EntityDataAccessor<VehicleConfiguration> DATA_CONFIG = SynchedEntityData.defineId(
            ModularVehicleEntity.class, GCEntityDataSerializers.VEHICLE_CONFIGURATION
    );
    private static final EntityDataAccessor<VehicleAnimState> DATA_ANIM_STATE = SynchedEntityData.defineId(
            ModularVehicleEntity.class, GCEntityDataSerializers.VEHICLE_ANIM_STATE
    );
    private static final EntityDataAccessor<Float> DATA_STEERING = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Boolean> DATA_ENGINE_ON = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_HEADLIGHTS_ON = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_DOORS_OPEN = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_PARKING_BRAKE = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Float> DATA_ROTOR_RPM = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DATA_CYCLIC_PITCH = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DATA_CYCLIC_ROLL = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DATA_GEAR_PROGRESS = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DATA_THROTTLE = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DATA_ELEVATOR = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DATA_AILERON = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DATA_RUDDER = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Boolean> DATA_STALLING = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<VehicleAiState> DATA_AI_STATE = SynchedEntityData.defineId(
            ModularVehicleEntity.class, GCEntityDataSerializers.VEHICLE_AI_STATE
    );
    private static final EntityDataAccessor<VehicleMissionConfig> DATA_MISSION_CONFIG = SynchedEntityData.defineId(
            ModularVehicleEntity.class, GCEntityDataSerializers.VEHICLE_MISSION_CONFIG
    );
    private static final EntityDataAccessor<Integer> DATA_AI_DATA_COUNT = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Boolean> DATA_CHUNK_TICKET_ACTIVE = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );

    private VehicleDerivedStats derivedStats = VehicleRegistry.computeStats(VehicleConfiguration.emptyChassis(VehicleRegistry.CHASSIS_SCOUT_ROVER));
    private SimpleContainer cargoInventory = new SimpleContainer(0);
    private final List<VehiclePartEntity> childHitboxes = new ArrayList<>();
    private final VehicleMissionController missionController = new VehicleMissionController();

    // Transient input state from pilot C2S/Client inputTick
    private boolean inputForward;
    private boolean inputBackward;
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputBrake;
    private boolean inputDescend;
    private int inputHoldTicks;

    // Transient remote drone pilot state (server-authoritative for SMALL unmanned drones)
    private float remoteForward;
    private float remoteStrafe;
    private float remoteYawDelta;
    private float remoteCollective;
    private int remoteHoldTicks;
    private @Nullable Player lastRemotePilot;
    private int warningCooldownTicks;
    private @Nullable PlanetaryEnvironment environmentOverrideForTest;

    // Physics state
    private double forwardSpeed;
    private double horizVx;
    private double horizVz;
    private float steeringAngle;
    private float localRotorRpm;
    private float localCyclicPitch;
    private float localCyclicRoll;
    private float localGearProgress;
    private float localThrottle;
    private float localElevator;
    private float localAileron;
    private float localRudder;
    private boolean localStalling;
    public float clientWheelSpin;
    public float clientDoorProgress;
    public float clientPitch;
    public float clientRoll;
    public float clientRotorAngle;
    public float clientGearProgress;
    public float clientElevator;
    public float clientAileron;
    public float clientRudder;

    public ModularVehicleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CONFIG, VehicleConfiguration.emptyChassis(VehicleRegistry.CHASSIS_SCOUT_ROVER));
        builder.define(DATA_ANIM_STATE, VehicleAnimState.NONE);
        builder.define(DATA_STEERING, 0.0F);
        builder.define(DATA_SPEED, 0.0F);
        builder.define(DATA_ENGINE_ON, true);
        builder.define(DATA_HEADLIGHTS_ON, true);
        builder.define(DATA_DOORS_OPEN, false);
        builder.define(DATA_PARKING_BRAKE, false);
        builder.define(DATA_ROTOR_RPM, 0.0F);
        builder.define(DATA_CYCLIC_PITCH, 0.0F);
        builder.define(DATA_CYCLIC_ROLL, 0.0F);
        builder.define(DATA_GEAR_PROGRESS, 0.0F);
        builder.define(DATA_THROTTLE, 0.0F);
        builder.define(DATA_ELEVATOR, 0.0F);
        builder.define(DATA_AILERON, 0.0F);
        builder.define(DATA_RUDDER, 0.0F);
        builder.define(DATA_STALLING, false);
        builder.define(DATA_AI_STATE, VehicleAiState.IDLE);
        builder.define(DATA_MISSION_CONFIG, VehicleMissionConfig.DEFAULT);
        builder.define(DATA_AI_DATA_COUNT, 0);
        builder.define(DATA_CHUNK_TICKET_ACTIVE, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_CONFIG.equals(key)) {
            this.recomputeDerivedState(false);
        }
    }

    public @NotNull VehicleConfiguration getConfiguration() {
        return this.entityData.get(DATA_CONFIG);
    }

    public void setConfiguration(@NotNull VehicleConfiguration config) {
        this.entityData.set(DATA_CONFIG, config);
        this.recomputeDerivedState(true);
    }

    public @NotNull VehicleDerivedStats getDerivedStats() {
        return this.derivedStats;
    }

    /**
     * Returns {@code true} when this rover has an enclosed pressurized cabin
     * (e.g. NASA SEV Lunar Cruiser or Mars 6x6 Mobile Habitat), allowing passengers
     * to breathe safely in vacuum/toxic atmospheres without consuming suit oxygen.
     */
    public boolean isPressurizedCabin() {
        return this.derivedStats.pressurizedCabin();
    }

    public @NotNull VehicleChassisDefinition getChassisDefinition() {
        return VehicleRegistry.getChassisOrDefault(this.getConfiguration().chassisId());
    }

    public @NotNull VehicleAnimState getAnimState() {
        return this.entityData.get(DATA_ANIM_STATE);
    }

    public void setAnimState(@NotNull VehicleAnimState state) {
        this.entityData.set(DATA_ANIM_STATE, state);
    }

    public float getSteeringAngle() {
        return this.isControlledByLocalInstance() ? this.steeringAngle : this.entityData.get(DATA_STEERING);
    }

    public float getSyncedSpeed() {
        return this.isControlledByLocalInstance() ? (float) this.forwardSpeed : this.entityData.get(DATA_SPEED);
    }

    public boolean isRotorcraft() {
        return this.derivedStats.isRotorcraft();
    }

    public boolean isFixedWing() {
        return this.derivedStats.isFixedWing();
    }

    public boolean isAircraft() {
        return this.derivedStats.isAircraft();
    }

    /**
     * Returns {@code true} for unmanned {@code SMALL} rotorcraft drones (e.g., Micro-Drone / Quadcopter)
     * that can be paired and piloted remotely via {@link DroneRemoteControllerItem}.
     */
    public boolean isRemoteControllableDrone() {
        return this.isRotorcraft() && this.derivedStats.sizeClass() == VehicleSizeClass.SMALL;
    }

    public float getRotorRpm() {
        return this.isControlledByLocalInstance() ? this.localRotorRpm : this.entityData.get(DATA_ROTOR_RPM);
    }

    public float getCyclicPitchDeg() {
        return this.isControlledByLocalInstance() ? this.localCyclicPitch : this.entityData.get(DATA_CYCLIC_PITCH);
    }

    public float getCyclicRollDeg() {
        return this.isControlledByLocalInstance() ? this.localCyclicRoll : this.entityData.get(DATA_CYCLIC_ROLL);
    }

    public float getGearProgress() {
        return this.isControlledByLocalInstance() ? this.localGearProgress : this.entityData.get(DATA_GEAR_PROGRESS);
    }

    public float getThrottle() {
        return this.isControlledByLocalInstance() ? this.localThrottle : this.entityData.get(DATA_THROTTLE);
    }

    /**
     * Adjusts the fixed-wing electric throttle quadrant by {@code delta} (clamped to {@code [0.0, 1.0]}).
     */
    public void adjustThrottleDirect(float delta) {
        this.setThrottleDirect(this.localThrottle + delta);
    }

    /**
     * Sets the fixed-wing electric throttle lever directly (clamped to {@code [0.0, 1.0]}),
     * retaining power level like a real aircraft throttle quadrant.
     */
    public void setThrottleDirect(float value) {
        this.localThrottle = Mth.clamp(value, 0.0F, 1.0F);
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_THROTTLE, this.localThrottle);
        }
    }

    public float getElevatorInput() {
        return this.isControlledByLocalInstance() ? this.localElevator : this.entityData.get(DATA_ELEVATOR);
    }

    public float getAileronInput() {
        return this.isControlledByLocalInstance() ? this.localAileron : this.entityData.get(DATA_AILERON);
    }

    public float getRudderInput() {
        return this.isControlledByLocalInstance() ? this.localRudder : this.entityData.get(DATA_RUDDER);
    }

    public boolean isStalling() {
        return this.isControlledByLocalInstance() ? this.localStalling : this.entityData.get(DATA_STALLING);
    }

    public boolean isEngineRunning() {
        return this.entityData.get(DATA_ENGINE_ON);
    }

    public boolean areHeadlightsOn() {
        return this.entityData.get(DATA_HEADLIGHTS_ON);
    }

    public boolean areDoorsOpen() {
        return this.entityData.get(DATA_DOORS_OPEN);
    }

    public boolean isParkingBrakeOn() {
        return this.entityData.get(DATA_PARKING_BRAKE);
    }

    public @NotNull VehicleAiState getAiState() {
        return this.entityData.get(DATA_AI_STATE);
    }

    public @NotNull VehicleMissionConfig getMissionConfig() {
        return this.entityData.get(DATA_MISSION_CONFIG);
    }

    public void setMissionConfig(@NotNull VehicleMissionConfig config) {
        this.entityData.set(DATA_MISSION_CONFIG, config);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.missionController.onConfigUpdated(serverLevel, this, config);
            this.entityData.set(DATA_AI_STATE, this.missionController.getState());
            this.entityData.set(DATA_CHUNK_TICKET_ACTIVE, this.missionController.getChunkTickets().hasActiveTicket());
        }
    }

    public @NotNull VehicleMissionController getMissionController() {
        return this.missionController;
    }

    public int getCollectedScienceDataCount() {
        return this.level().isClientSide ? this.entityData.get(DATA_AI_DATA_COUNT) : this.missionController.getCollectedDataCount();
    }

    public boolean isChunkTicketActive() {
        return this.level().isClientSide
                ? this.entityData.get(DATA_CHUNK_TICKET_ACTIVE)
                : this.missionController.getChunkTickets().hasActiveTicket();
    }

    public void setDoorsOpenServer(boolean open) {
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_DOORS_OPEN, open);
        }
    }

    public void setStoredEnergyServer(long energy) {
        if (!this.level().isClientSide) {
            VehicleConfiguration cfg = this.getConfiguration();
            long clamped = Math.max(0L, Math.min(this.derivedStats.energyCapacity(), energy));
            this.entityData.set(DATA_CONFIG, cfg.withResources(clamped, 0L));
        }
    }

    /**
     * Returns {@code true} when an installed {@code AI_CORE} module has active server-authoritative
     * control over this vehicle. In {@link VehicleMissionRole#PASSENGER_TAXI} mode, seated players
     * ride as passengers without disabling autonomous control; in other roles a seated human pilot
     * overrides AI manually.
     */
    public boolean isAutonomousModeActive() {
        if (!this.derivedStats.hasAiCore()) {
            return false;
        }
        VehicleMissionConfig cfg = this.getMissionConfig();
        if (!cfg.enabled()) {
            return false;
        }
        if (cfg.role() != VehicleMissionRole.PASSENGER_TAXI && this.hasSeatedHumanPilot()) {
            return false;
        }
        return true;
    }

    private boolean hasSeatedHumanPilot() {
        List<Entity> passengers = this.getPassengers();
        List<SeatSpec> seats = this.derivedStats.seats();
        for (int i = 0; i < passengers.size() && i < seats.size(); i++) {
            if (seats.get(i).role() == SeatRole.PILOT && passengers.get(i) instanceof Player) {
                return true;
            }
        }
        return false;
    }

    // --- AutonomousVehicleContext implementation ---
    @Override
    public @NotNull Vec3 velocity() {
        return this.getDeltaMovement();
    }

    @Override
    public float yawDeg() {
        return this.getYRot();
    }

    @Override
    public float pitchDeg() {
        return this.localCyclicPitch;
    }

    @Override
    public double forwardSpeed() {
        return this.forwardSpeed;
    }

    @Override
    public boolean isOnGround() {
        return this.onGround();
    }

    @Override
    public double altitudeAgl() {
        return this.computeAltitudeAgl();
    }

    @Override
    public float entityWidth() {
        return this.getBbWidth();
    }

    @Override
    public float entityHeight() {
        return this.getBbHeight();
    }

    @Override
    public double wheelbaseBlocks() {
        return this.getChassisDefinition().wheelbaseBlocks();
    }

    @Override
    public @NotNull VehicleDerivedStats derivedStats() {
        return this.derivedStats;
    }

    @Override
    public double gravityG() {
        return this.resolveEnvironment().gravityG();
    }

    @Override
    public double pressureAtm() {
        return this.resolveEnvironment().pressureAtm();
    }

    @Override
    public int runwayHeadingDeg() {
        return this.getMissionConfig().runwayHeadingDeg();
    }

    @Override
    public @Nullable Entity asEntity() {
        return this;
    }

    @Override
    public boolean shouldIgnoreShiftExit() {
        return this.isAircraft() && (!this.onGround() || this.computeAltitudeAgl() > 0.35);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        if (this.isAircraft()) {
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        super.lerpTo(x, y, z, yaw, pitch, this.isAircraft() ? 3 : interpolationSteps);
    }

    /**
     * Applies remote-pilot flight inputs streamed from {@link dev.galacticraft.mod.network.c2s.DroneRemoteControlPayload}.
     */
    public void applyRemoteControlInput(
            @NotNull Player pilot,
            float forward,
            float strafe,
            float yawDelta,
            float collective,
            boolean toggleLights,
            boolean toggleEngine
    ) {
        if (!this.isRemoteControllableDrone()) {
            return;
        }
        this.remoteForward = Mth.clamp(forward, -1.0F, 1.0F);
        this.remoteStrafe = Mth.clamp(strafe, -1.0F, 1.0F);
        this.remoteYawDelta = Mth.clamp(yawDelta, -18.0F, 18.0F);
        this.remoteCollective = Mth.clamp(collective, -1.0F, 1.0F);
        this.remoteHoldTicks = 4;
        this.lastRemotePilot = pilot;
        if (toggleLights) {
            this.handlePanelAction(pilot, "toggle_headlights", "", "");
        }
        if (toggleEngine) {
            this.handlePanelAction(pilot, "toggle_ignition", "", "");
        }
    }

    /**
     * Measures vertical clearance above solid ground in blocks (up to 12 blocks) for landing gear automation
     * and cockpit altitude AGL telemetry.
     */
    public double computeAltitudeAgl() {
        if (this.onGround()) {
            return 0.0;
        }
        BlockPos start = this.blockPosition();
        for (int dy = 0; dy <= 12; dy++) {
            BlockPos check = start.below(dy);
            if (!this.level().getBlockState(check).getCollisionShape(this.level(), check).isEmpty()) {
                return Math.max(0.0, this.getY() - (check.getY() + 1.0));
            }
        }
        return 12.0;
    }

    public void handlePanelAction(@NotNull Player player, @NotNull String action, @NotNull String targetSlot, @NotNull String value) {
        switch (action) {
            case "toggle_ignition" -> {
                boolean next = !this.isEngineRunning();
                this.entityData.set(DATA_ENGINE_ON, next);
                this.level().playSound(null, this.blockPosition(), next ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.55F, 1.25F);
            }
            case "toggle_headlights" -> {
                this.entityData.set(DATA_HEADLIGHTS_ON, !this.areHeadlightsOn());
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.5F, 1.3F);
            }
            case "toggle_doors" -> {
                boolean next = !this.areDoorsOpen();
                this.entityData.set(DATA_DOORS_OPEN, next);
                this.level().playSound(null, this.blockPosition(), next ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 0.7F, 0.95F);
            }
            case "toggle_brake" -> {
                this.entityData.set(DATA_PARKING_BRAKE, !this.isParkingBrakeOn());
                this.level().playSound(null, this.blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 0.45F, 1.4F);
            }
            case "set_throttle" -> {
                if (this.isFixedWing()) {
                    try {
                        float parsed = Float.parseFloat(value);
                        float prev = this.localThrottle;
                        this.setThrottleDirect(parsed);
                        if (parsed <= 0.001F && prev > 0.05F) {
                            this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.45F, 0.85F);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            case "open_cargo" -> {
                if (this.cargoInventory.getContainerSize() > 0) {
                    this.openCargoMenu(player);
                }
            }
            case "ai_toggle" -> {
                if (this.derivedStats.hasAiCore()) {
                    VehicleMissionConfig cur = this.getMissionConfig();
                    this.setMissionConfig(cur.withEnabled(!cur.enabled()));
                    this.level().playSound(null, this.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.55F, 1.3F);
                }
            }
            case "ai_depart_now" -> {
                if (this.derivedStats.hasAiCore()) {
                    this.missionController.requestImmediateDeparture();
                    this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.5F, 1.4F);
                }
            }
            case "set_gauge" -> {
                VehicleHardpoint hp = this.getChassisDefinition().findHardpoint(targetSlot);
                if (hp == null || hp.type() != HardpointType.DASHBOARD) {
                    return;
                }
                VehicleConfiguration cfg = this.getConfiguration();
                if (value.isBlank()) {
                    ResourceLocation prev = cfg.getPart(targetSlot);
                    if (prev != null) {
                        this.setConfiguration(cfg.withoutPart(targetSlot));
                        if (!player.getAbilities().instabuild) {
                            ItemStack ret = GCItems.createVehiclePartStack(prev);
                            if (!ret.isEmpty() && !player.getInventory().add(ret)) {
                                this.spawnAtLocation(ret);
                            }
                        }
                    }
                } else {
                    ResourceLocation nextId = ResourceLocation.tryParse(value);
                    VehiclePartDefinition part = nextId != null ? VehicleRegistry.getPart(nextId) : null;
                    if (part != null && hp.accepts(part.type(), part.sizeClass())) {
                        ResourceLocation prev = cfg.getPart(targetSlot);
                        if (nextId.equals(prev)) {
                            return;
                        }
                        if (!player.getAbilities().instabuild) {
                            ItemStack needed = GCItems.createVehiclePartStack(nextId);
                            int slotIdx = -1;
                            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                ItemStack inv = player.getInventory().getItem(i);
                                if (!inv.isEmpty() && inv.is(needed.getItem())) {
                                    slotIdx = i;
                                    break;
                                }
                            }
                            if (slotIdx < 0) {
                                player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.missing_gauge_item", needed.getHoverName()), true);
                                return;
                            }
                            player.getInventory().getItem(slotIdx).shrink(1);
                            if (prev != null) {
                                ItemStack ret = GCItems.createVehiclePartStack(prev);
                                if (!ret.isEmpty() && !player.getInventory().add(ret)) {
                                    this.spawnAtLocation(ret);
                                }
                            }
                        }
                        this.setConfiguration(cfg.withPart(targetSlot, nextId));
                        this.level().playSound(null, this.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.6F, 1.2F);
                    }
                }
            }
            default -> {
            }
        }
    }

    public @NotNull SimpleContainer getCargoInventory() {
        return this.cargoInventory;
    }

    private void recomputeDerivedState(boolean resizeCargo) {
        VehicleConfiguration cfg = this.getConfiguration();
        this.derivedStats = VehicleRegistry.computeStats(cfg);
        this.refreshDimensions();
        if (resizeCargo) {
            this.resizeCargoInventory(this.derivedStats.totalCargoSlots());
        }
        if (!this.level().isClientSide) {
            this.rebuildChildHitboxes();
        }
    }

    private void resizeCargoInventory(int targetSlots) {
        int clamped = Math.max(0, Math.min(54, targetSlots));
        if (this.cargoInventory.getContainerSize() == clamped) {
            return;
        }
        SimpleContainer previous = this.cargoInventory;
        this.cargoInventory = new SimpleContainer(clamped);
        int keep = Math.min(previous.getContainerSize(), clamped);
        for (int i = 0; i < keep; i++) {
            this.cargoInventory.setItem(i, previous.getItem(i).copy());
        }
        if (!this.level().isClientSide && previous.getContainerSize() > clamped) {
            for (int i = clamped; i < previous.getContainerSize(); i++) {
                ItemStack overflow = previous.getItem(i);
                if (!overflow.isEmpty()) {
                    this.spawnAtLocation(overflow);
                }
            }
        }
    }

    private void rebuildChildHitboxes() {
        for (VehiclePartEntity child : this.childHitboxes) {
            if (!child.isRemoved()) {
                child.discard();
            }
        }
        this.childHitboxes.clear();

        VehicleSizeClass size = this.derivedStats.sizeClass();
        if (size == VehicleSizeClass.SMALL) {
            return;
        }

        if (this.isFixedWing()) {
            if (size == VehicleSizeClass.LARGE) {
                // Full 16.2m Cargo Airplane solid fuselage hull collision & localized interaction sections
                this.spawnChildPart("front", new Vec3(0.0, 0.20, 4.55), 2.55F, 2.40F, true);
                this.spawnChildPart("front", new Vec3(0.0, 0.20, 2.20), 3.25F, 3.20F, true);
                this.spawnChildPart("cargo", new Vec3(0.0, 0.20, -2.20), 3.25F, 3.20F, true);
                this.spawnChildPart("tail", new Vec3(0.0, 1.20, -5.60), 2.10F, 2.60F, true);
                this.spawnChildPart("tail", new Vec3(0.0, 2.20, -8.30), 1.40F, 3.60F, true);
            } else {
                // Full 9.4m Bush Plane solid fuselage hull collision & localized interaction sections
                this.spawnChildPart("front", new Vec3(0.0, 0.15, 2.10), 1.65F, 1.85F, true);
                this.spawnChildPart("cargo", new Vec3(0.0, 0.15, -1.75), 1.70F, 1.90F, true);
                this.spawnChildPart("tail", new Vec3(0.0, 0.70, -4.10), 1.15F, 1.75F, true);
            }
            return;
        }

        if (VehicleRegistry.CHASSIS_HEAVY_HAULER.equals(this.getChassisDefinition().id())) {
            // Full 8.6m Mars 6x6 Mobile Habitat solid multi-section hull collision
            this.spawnChildPart("front", new Vec3(0.0, 0.15, 2.65), 3.30F, 3.25F, true);
            this.spawnChildPart("cargo", new Vec3(0.0, 0.15, -2.55), 3.35F, 3.45F, true);
            return;
        }
        if (VehicleRegistry.CHASSIS_UTILITY_BUGGY.equals(this.getChassisDefinition().id())) {
            // Compact 4.25m NASA SEV pressurized rover collision
            this.spawnChildPart("front", new Vec3(0.0, 0.10, 1.05), 2.35F, 2.05F, true);
            this.spawnChildPart("cargo", new Vec3(0.0, 0.10, -1.05), 2.35F, 2.05F, true);
            return;
        }

        double zSpan = size == VehicleSizeClass.LARGE ? 1.45 : 0.95;
        float boxW = size == VehicleSizeClass.LARGE ? 1.95F : 1.55F;
        float boxH = size == VehicleSizeClass.LARGE ? 1.45F : 1.15F;

        this.spawnChildPart("front", new Vec3(0.0, 0.1, zSpan), boxW, boxH, true);
        this.spawnChildPart("cargo", new Vec3(0.0, 0.1, -zSpan), boxW, boxH, true);
    }

    private void spawnChildPart(@NotNull String section, @NotNull Vec3 localOffset, float width, float height, boolean solid) {
        VehiclePartEntity part = GCEntityTypes.VEHICLE_PART.create(this.level());
        if (part != null) {
            part.attachTo(this, section, localOffset, width, height, solid);
            this.level().addFreshEntity(part);
            this.childHitboxes.add(part);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.missionController.releaseChunkTicket(serverLevel, this);
        }
        for (VehiclePartEntity child : this.childHitboxes) {
            if (!child.isRemoved()) {
                child.discard();
            }
        }
        this.childHitboxes.clear();
        super.remove(reason);
    }

    @Override
    public @Nullable Entity changeDimension(DimensionTransition transition) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.missionController.releaseChunkTicket(serverLevel, this);
        }
        return super.changeDimension(transition);
    }

    @Override
    public float maxUpStep() {
        return this.derivedStats.sizeClass().maxUpStep();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(Pose pose) {
        ResourceLocation chassisId = this.getChassisDefinition().id();
        if (VehicleRegistry.CHASSIS_MICRO_DRONE.equals(chassisId)) {
            return EntityDimensions.scalable(1.65F, 0.82F);
        }
        if (VehicleRegistry.CHASSIS_UTILITY_BUGGY.equals(chassisId)) {
            return EntityDimensions.scalable(2.55F, 2.25F);
        }
        if (VehicleRegistry.CHASSIS_HEAVY_HAULER.equals(chassisId)) {
            return EntityDimensions.scalable(3.65F, 3.65F);
        }
        VehicleSizeClass size = this.derivedStats.sizeClass();
        return EntityDimensions.scalable(size.baseWidth(), size.baseHeight());
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        if (other instanceof VehiclePartEntity || this.hasPassenger(other)) {
            return false;
        }
        return super.canCollideWith(other);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void inputTick(float leftImpulse, float forwardImpulse, boolean up, boolean down, boolean left, boolean right, boolean jumping, boolean shiftKeyDown, boolean invertControls) {
        this.inputForward = up || forwardImpulse > 0.05F;
        this.inputBackward = down || forwardImpulse < -0.05F;
        this.inputLeft = left || leftImpulse > 0.05F;
        this.inputRight = right || leftImpulse < -0.05F;
        this.inputBrake = jumping;
        this.inputDescend = shiftKeyDown;
        this.inputHoldTicks = 2;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.warningCooldownTicks > 0) {
            this.warningCooldownTicks--;
        }

        if (this.isAutonomousModeActive()) {
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                this.tickAutonomousServer(serverLevel);
            }
        } else {
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                if (this.missionController.getChunkTickets().hasActiveTicket() || this.getAiState() != VehicleAiState.IDLE) {
                    this.missionController.releaseChunkTicket(serverLevel, this);
                    this.entityData.set(DATA_AI_STATE, VehicleAiState.IDLE);
                    this.entityData.set(DATA_CHUNK_TICKET_ACTIVE, false);
                }
            }
            if (this.isControlledByLocalInstance() || (!this.level().isClientSide && this.environmentOverrideForTest != null)) {
                if (this.isRotorcraft()) {
                    this.tickRotorcraftPhysics();
                } else if (this.isFixedWing()) {
                    this.tickFixedWingPhysics();
                } else {
                    this.tickDrivePhysics();
                }
            } else if (!this.level().isClientSide && this.isRotorcraft()) {
                this.tickServerSeatedRotorcraftSync();
            } else if (!this.level().isClientSide && this.isFixedWing()) {
                this.tickServerSeatedFixedWingSync();
            }
        }

        if (!this.level().isClientSide) {
            this.tickPowerGeneration();
            for (VehiclePartEntity child : this.childHitboxes) {
                child.syncPositionToParent();
            }
        } else {
            this.tickClientVisuals();
        }

        if (this.inputHoldTicks > 0) {
            this.inputHoldTicks--;
        } else {
            this.inputForward = false;
            this.inputBackward = false;
            this.inputLeft = false;
            this.inputRight = false;
            this.inputBrake = false;
            this.inputDescend = false;
        }
        if (this.remoteHoldTicks > 0) {
            this.remoteHoldTicks--;
        } else {
            this.remoteForward = 0.0F;
            this.remoteStrafe = 0.0F;
            this.remoteYawDelta = 0.0F;
            this.remoteCollective = 0.0F;
            this.lastRemotePilot = null;
        }
    }

    /**
     * Optional planetary environment override for headless GameTests running on the flat test server.
     */
    public void setEnvironmentForTest(@Nullable PlanetaryEnvironment environment) {
        this.environmentOverrideForTest = environment;
    }

    public @NotNull PlanetaryEnvironment resolveEnvironment() {
        return this.environmentOverrideForTest != null
                ? this.environmentOverrideForTest
                : PlanetaryEnvironment.fromLevel(this.level());
    }

    /**
     * Server-authoritative autonomous AI control loop executed exclusively on {@code !level().isClientSide}.
     * Delegates navigation and mission roles to {@link VehicleMissionController} and integrates the
     * resulting {@link ControlCommand} into ground, rotorcraft, or fixed-wing vehicle physics.
     */
    private void tickAutonomousServer(@NotNull ServerLevel level) {
        ControlCommand cmd = this.missionController.tickServer(level, this);
        if (this.isRotorcraft()) {
            this.applyAutonomousRotorcraftControl(cmd);
        } else if (this.isFixedWing()) {
            this.applyAutonomousFixedWingControl(cmd);
        } else {
            this.applyAutonomousGroundControl(cmd);
        }

        this.entityData.set(DATA_AI_STATE, this.missionController.getState());
        this.entityData.set(DATA_AI_DATA_COUNT, this.missionController.getCollectedDataCount());
        this.entityData.set(DATA_CHUNK_TICKET_ACTIVE, this.missionController.getChunkTickets().hasActiveTicket());
    }

    private void applyAutonomousGroundControl(@NotNull ControlCommand cmd) {
        PlanetaryEnvironment env = this.resolveEnvironment();
        double accel = this.derivedStats.computeGroundAcceleration(env.gravityG());
        double maxSpeed = Math.max(0.22, this.derivedStats.maxSpeedBlocksPerTick());

        this.steeringAngle = Mth.lerp(0.35F, this.steeringAngle, Mth.clamp(cmd.steeringAngleDeg(), -35.0F, 35.0F));

        if (cmd.brake() || cmd.pointTurn()) {
            this.forwardSpeed *= this.onGround() ? 0.65 : 0.94;
        } else {
            double clampedTarget = Mth.clamp(cmd.targetSpeed(), -maxSpeed * 0.55, maxSpeed);
            double delta = clampedTarget - this.forwardSpeed;
            double maxStep = Math.max(0.015, accel * 1.45);
            this.forwardSpeed += Mth.clamp(delta, -maxStep, maxStep);
            if (Math.abs(this.forwardSpeed) > 0.01) {
                this.consumeDrivePower();
            }
        }
        if (Math.abs(this.forwardSpeed) < 0.002) {
            this.forwardSpeed = 0.0;
        }

        // Steering / yaw update (Ackermann vs Skid-Steer point-turn)
        if (cmd.pointTurn()) {
            float yawStep = Mth.clamp(cmd.yawRateDegPerTick(), -6.5F, 6.5F);
            this.setYRot(this.getYRot() + yawStep);
        } else if (Math.abs(this.forwardSpeed) > 0.008 && this.onGround()) {
            if (this.derivedStats.skidSteerEnabled() || Math.abs(this.steeringAngle) > 0.35F || Math.abs(cmd.yawRateDegPerTick()) > 0.05F) {
                double wheelbase = Math.max(1.2, this.getChassisDefinition().wheelbaseBlocks());
                double ackermannDeg = Math.toDegrees((this.forwardSpeed * Math.tan(Math.toRadians(this.steeringAngle))) / wheelbase);
                double chosenTurn = this.derivedStats.skidSteerEnabled()
                        ? Mth.clamp(cmd.yawRateDegPerTick(), -5.5, 5.5)
                        : Mth.clamp(ackermannDeg, -6.0, 6.0);
                this.setYRot(this.getYRot() + (float) chosenTurn);
            }
        }

        double yawRad = Math.toRadians(this.getYRot());
        double vx = -Math.sin(yawRad) * this.forwardSpeed;
        double vz = Math.cos(yawRad) * this.forwardSpeed;
        double gravityStep = -0.08 * Math.clamp(env.gravityG(), 0.16, 2.5);
        double vy = this.onGround() ? Math.max(0.0, this.getDeltaMovement().y) : Math.max(-1.5, this.getDeltaMovement().y + gravityStep);

        this.setDeltaMovement(vx, vy, vz);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision) {
            this.forwardSpeed *= 0.25;
        }

        this.entityData.set(DATA_STEERING, this.steeringAngle);
        this.entityData.set(DATA_SPEED, (float) this.forwardSpeed);
    }

    private void applyAutonomousRotorcraftControl(@NotNull ControlCommand cmd) {
        PlanetaryEnvironment env = this.resolveEnvironment();
        double twr = this.derivedStats.computeRotorTwr(env.gravityG(), env.pressureAtm());
        boolean vacuumBlocked = this.derivedStats.isBlockedByVacuum(env.pressureAtm());
        boolean canLift = !cmd.spoolDownRotors() && this.isEngineRunning() && !this.isParkingBrakeOn()
                && this.derivedStats.isDrivable() && !vacuumBlocked && twr >= 1.0 && this.hasAvailablePower();

        // 1. Rotor RPM spool-up / spool-down
        float targetRpm = 0.0F;
        if (canLift) {
            targetRpm = 960.0F + Math.max(0.0F, cmd.collective()) * 320.0F;
        }
        float rpmLerp = targetRpm > this.localRotorRpm ? 0.16F : 0.10F;
        this.localRotorRpm = Mth.lerp(rpmLerp, this.localRotorRpm, targetRpm);
        if (this.localRotorRpm < 1.5F) {
            this.localRotorRpm = 0.0F;
        }

        // 2. Yaw & cyclic attitude
        if (Math.abs(cmd.yawRateDegPerTick()) > 0.01F && canLift) {
            this.setYRot(this.getYRot() + Mth.clamp(cmd.yawRateDegPerTick(), -7.5F, 7.5F));
        }
        float targetPitch = (!this.onGround() && canLift) ? Mth.clamp(cmd.cyclicPitchDeg(), -MAX_CYCLIC_TILT_DEG, MAX_CYCLIC_TILT_DEG) : 0.0F;
        float targetRoll = (!this.onGround() && canLift) ? Mth.clamp(cmd.cyclicRollDeg(), -MAX_CYCLIC_TILT_DEG, MAX_CYCLIC_TILT_DEG) : 0.0F;
        this.localCyclicPitch = Mth.lerp(0.24F, this.localCyclicPitch, targetPitch);
        this.localCyclicRoll = Mth.lerp(0.24F, this.localCyclicRoll, targetRoll);

        // 3. Vertical collective lift
        double gravityStep = 0.08 * Math.clamp(env.gravityG(), 0.0, 2.5);
        double vy = this.getDeltaMovement().y;
        boolean readyToFly = canLift && this.localRotorRpm >= 420.0F;
        if (readyToFly) {
            double maxClimbSpeed = Math.clamp(0.28 + Math.min(twr, 4.0) * 0.065, 0.28, 0.54);
            double climbAccel = Math.clamp((twr - 1.0) * 0.04 + 0.042, 0.032, 0.09);
            float col = cmd.collective();
            if (col > 0.04F) {
                vy = Math.min(maxClimbSpeed * col, vy + climbAccel);
            } else if (col < -0.04F) {
                vy = Math.max(-0.36 * Math.abs(col), vy - 0.042);
            } else {
                vy = this.onGround() ? 0.0 : vy * 0.72;
                if (Math.abs(vy) < 0.004) {
                    vy = 0.0;
                }
            }
            this.consumeRotorPower(env, col > 0.1F);
        } else {
            vy = this.onGround() ? Math.max(0.0, vy) : Math.max(-0.85, vy - gravityStep * 0.75);
        }

        // 4. Horizontal velocity coupled to cyclic tilt
        double maxHorizSpeed = Math.max(0.38, this.derivedStats.maxSpeedBlocksPerTick());
        if (!this.onGround() && readyToFly) {
            double yawRad = Math.toRadians(this.getYRot());
            double fx = -Math.sin(yawRad);
            double fz = Math.cos(yawRad);
            double rx = Math.cos(yawRad);
            double rz = Math.sin(yawRad);

            double pitchFactor = Math.sin(Math.toRadians(this.localCyclicPitch));
            double rollFactor = Math.sin(Math.toRadians(this.localCyclicRoll));
            double horizAccel = 0.095 * (this.derivedStats.sizeClass() == VehicleSizeClass.SMALL ? 1.25 : 1.0);
            this.horizVx += (fx * pitchFactor + rx * rollFactor) * horizAccel;
            this.horizVz += (fz * pitchFactor + rz * rollFactor) * horizAccel;

            double drag = env.pressureAtm() >= VehicleDerivedStats.MIN_AERO_PRESSURE_ATM ? 0.88 : 0.92;
            this.horizVx *= drag;
            this.horizVz *= drag;
            double hSpeed = Math.hypot(this.horizVx, this.horizVz);
            if (hSpeed > maxHorizSpeed) {
                double scale = maxHorizSpeed / hSpeed;
                this.horizVx *= scale;
                this.horizVz *= scale;
            }
        } else {
            double damp = this.onGround() ? 0.65 : 0.94;
            this.horizVx *= damp;
            this.horizVz *= damp;
        }
        if (Math.hypot(this.horizVx, this.horizVz) < 0.002) {
            this.horizVx = 0.0;
            this.horizVz = 0.0;
        }

        this.forwardSpeed = Math.hypot(this.horizVx, this.horizVz);
        this.setDeltaMovement(this.horizVx, vy, this.horizVz);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision) {
            this.horizVx *= 0.25;
            this.horizVz *= 0.25;
        }

        // 5. Retractable landing gear automation (preserve mission animations when grounded)
        double agl = this.computeAltitudeAgl();
        float targetGear = (!this.onGround() && !cmd.gearDown() && agl > GEAR_RETRACT_AGL_BLOCKS) ? 1.0F : 0.0F;
        this.localGearProgress = Mth.lerp(0.14F, this.localGearProgress, targetGear);
        if (Math.abs(this.localGearProgress - targetGear) < 0.01F) {
            this.localGearProgress = targetGear;
        }

        this.entityData.set(DATA_ROTOR_RPM, this.localRotorRpm);
        this.entityData.set(DATA_CYCLIC_PITCH, this.localCyclicPitch);
        this.entityData.set(DATA_CYCLIC_ROLL, this.localCyclicRoll);
        this.entityData.set(DATA_GEAR_PROGRESS, this.localGearProgress);
        this.entityData.set(DATA_SPEED, (float) this.forwardSpeed);
        if (this.localGearProgress > 0.04F && this.localGearProgress < 0.96F) {
            this.entityData.set(DATA_ANIM_STATE, VehicleAnimState.GEAR_TRANSIT);
        } else if (this.getAnimState() == VehicleAnimState.GEAR_TRANSIT) {
            this.entityData.set(DATA_ANIM_STATE, VehicleAnimState.NONE);
        }
    }

    private void applyAutonomousFixedWingControl(@NotNull ControlCommand cmd) {
        PlanetaryEnvironment env = this.resolveEnvironment();
        boolean canOperate = !cmd.spoolDownRotors() && this.isEngineRunning() && !this.isParkingBrakeOn()
                && this.derivedStats.isDrivable() && this.hasAvailablePower();
        boolean vacuumBlocked = this.derivedStats.isBlockedByVacuum(env.pressureAtm());
        boolean canHoverVtol = this.derivedStats.canHoverIn(env.gravityG(), env.pressureAtm());
        double stallSpeed = this.derivedStats.computeStallSpeedBlocksPerTick(env.gravityG(), env.pressureAtm());
        double maxSpeed = Math.max(0.55, this.derivedStats.maxSpeedBlocksPerTick());

        this.localThrottle = canOperate ? Mth.lerp(0.25F, this.localThrottle, Mth.clamp(cmd.throttle(), 0.0F, 1.0F))
                : Math.max(0.0F, this.localThrottle - 0.08F);

        float targetRpm = canOperate ? (280.0F + this.localThrottle * 1420.0F) : 0.0F;
        if (canOperate && cmd.vtolHover() && canHoverVtol) {
            targetRpm = Math.max(targetRpm, 980.0F);
        }
        this.localRotorRpm = Mth.lerp(targetRpm > this.localRotorRpm ? 0.16F : 0.08F, this.localRotorRpm, targetRpm);
        if (this.localRotorRpm < 1.5F) {
            this.localRotorRpm = 0.0F;
        }

        double aglNow = this.computeAltitudeAgl();
        boolean airborne = !this.onGround() && aglNow >= 0.45;
        boolean vtolHoverActive = canOperate && canHoverVtol && cmd.vtolHover();
        boolean stalling = airborne && !vtolHoverActive
                && (vacuumBlocked || !Double.isFinite(stallSpeed) || this.forwardSpeed < stallSpeed * 0.96);
        this.localStalling = stalling;

        if (stalling) {
            this.localCyclicPitch = Math.min(24.0F, this.localCyclicPitch + 2.0F);
        } else {
            this.localCyclicPitch = Mth.lerp(0.22F, this.localCyclicPitch, Mth.clamp(cmd.cyclicPitchDeg(), -24.0F, 24.0F));
        }
        this.localCyclicRoll = Mth.lerp(0.20F, this.localCyclicRoll, Mth.clamp(cmd.cyclicRollDeg(), -30.0F, 30.0F));
        this.localRudder = Mth.clamp(cmd.yawRateDegPerTick() / 3.0F, -1.0F, 1.0F);
        this.localAileron = Mth.clamp(this.localCyclicRoll / 30.0F, -1.0F, 1.0F);
        this.localElevator = Mth.clamp(-this.localCyclicPitch / 20.0F, -1.0F, 1.0F);
        this.steeringAngle = this.localRudder * 26.0F;

        if (Math.abs(cmd.yawRateDegPerTick()) > 0.01F && (vtolHoverActive || !this.onGround() || Math.abs(this.forwardSpeed) > 0.015)) {
            this.setYRot(this.getYRot() + Mth.clamp(cmd.yawRateDegPerTick(), -4.5F, 4.5F));
        }

        double propThrustN = this.derivedStats.computeForwardPropThrustN(env.pressureAtm()) * this.localThrottle;
        double groundTaxiN = this.onGround() ? this.derivedStats.totalDriveForceN() * 0.55 * this.localThrottle : 0.0;
        double maxCl = Math.max(0.5, this.derivedStats.effectiveWingMaxCl());
        double noseUpDeg = -this.localCyclicPitch;
        double clRaw = Mth.clamp(0.45 + (noseUpDeg / 18.0) * (maxCl - 0.45), 0.12, maxCl);
        double effectiveCl = stalling ? (clRaw * 0.32) : clRaw;
        double dragN = this.derivedStats.computeFixedWingDragN(this.forwardSpeed, env.pressureAtm(), effectiveCl);
        double mass = Math.max(200.0, this.derivedStats.totalMassKg());

        this.forwardSpeed += ((propThrustN + groundTaxiN) / mass) / 145.0;
        this.forwardSpeed = Math.max(0.0, this.forwardSpeed - (dragN / mass) / 380.0);
        if (!this.onGround() && !vtolHoverActive) {
            this.forwardSpeed = Math.max(0.0, this.forwardSpeed + Math.sin(Math.toRadians(this.localCyclicPitch)) * 0.032 * Math.clamp(env.gravityG(), 0.15, 2.0));
        }
        if (this.onGround()) {
            this.forwardSpeed *= cmd.brake() ? 0.78 : (this.localThrottle <= 0.02F ? 0.92 : 0.985);
        } else {
            this.forwardSpeed *= 0.992;
        }
        this.forwardSpeed = Math.clamp(this.forwardSpeed, 0.0, maxSpeed);
        if (this.forwardSpeed < 0.002) {
            this.forwardSpeed = 0.0;
        }

        double gravityStep = 0.08 * Math.clamp(env.gravityG(), 0.0, 2.5);
        double vy = this.getDeltaMovement().y;
        if (vtolHoverActive) {
            float col = cmd.collective();
            if (col > 0.04F) {
                vy = Math.min(0.42 * col, vy + 0.055);
            } else if (col < -0.04F) {
                vy = Math.max(-0.34 * Math.abs(col), vy - 0.045);
            } else {
                vy = this.onGround() ? 0.0 : vy * 0.74;
            }
        } else {
            double liftN = this.derivedStats.computeFixedWingLiftN(this.forwardSpeed, env.pressureAtm(), effectiveCl);
            double weightN = this.derivedStats.computeWeightN(env.gravityG());
            double liftRatio = weightN > 1.0 ? (liftN / weightN) : 0.0;
            if (!airborne) {
                boolean liftOff = !vacuumBlocked && liftRatio >= 1.0 && (noseUpDeg >= 4.0 || this.forwardSpeed >= stallSpeed * 1.12);
                vy = liftOff ? Math.max(0.16, (liftRatio - 0.85) * 0.28) : Math.min(vy, -0.04);
            } else if (stalling) {
                vy = Math.max(-0.85, vy - gravityStep * Math.max(0.35, 1.0 - liftRatio * 0.55));
            } else {
                double desiredVy = this.forwardSpeed * Math.sin(Math.toRadians(noseUpDeg)) * 0.75;
                if (liftRatio < 1.0) {
                    desiredVy = Math.min(desiredVy, -(1.0 - liftRatio) * 0.28);
                }
                vy = Mth.lerp(0.24, vy, Math.clamp(desiredVy, -0.58, 0.46));
            }
        }

        double yawRad = Math.toRadians(this.getYRot());
        this.horizVx = -Math.sin(yawRad) * this.forwardSpeed;
        this.horizVz = Math.cos(yawRad) * this.forwardSpeed;
        this.setDeltaMovement(this.horizVx, vy, this.horizVz);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision) {
            this.forwardSpeed *= 0.25;
        }

        if (canOperate && (this.localThrottle > 0.05F || vtolHoverActive)) {
            this.consumeFixedWingPower(vtolHoverActive);
        }

        double agl = this.computeAltitudeAgl();
        float targetGear = (!this.onGround() && !cmd.gearDown() && agl > GEAR_RETRACT_AGL_BLOCKS) ? 1.0F : 0.0F;
        this.localGearProgress = Mth.lerp(0.14F, this.localGearProgress, targetGear);
        if (Math.abs(this.localGearProgress - targetGear) < 0.01F) {
            this.localGearProgress = targetGear;
        }

        this.entityData.set(DATA_ROTOR_RPM, this.localRotorRpm);
        this.entityData.set(DATA_CYCLIC_PITCH, this.localCyclicPitch);
        this.entityData.set(DATA_CYCLIC_ROLL, this.localCyclicRoll);
        this.entityData.set(DATA_GEAR_PROGRESS, this.localGearProgress);
        this.entityData.set(DATA_SPEED, (float) this.forwardSpeed);
        this.entityData.set(DATA_STEERING, this.steeringAngle);
        this.entityData.set(DATA_THROTTLE, this.localThrottle);
        this.entityData.set(DATA_ELEVATOR, this.localElevator);
        this.entityData.set(DATA_AILERON, this.localAileron);
        this.entityData.set(DATA_RUDDER, this.localRudder);
        this.entityData.set(DATA_STALLING, this.localStalling);
        if (this.localGearProgress > 0.04F && this.localGearProgress < 0.96F) {
            this.entityData.set(DATA_ANIM_STATE, VehicleAnimState.GEAR_TRANSIT);
        } else if (this.getAnimState() == VehicleAnimState.GEAR_TRANSIT) {
            this.entityData.set(DATA_ANIM_STATE, VehicleAnimState.NONE);
        }
    }

    private void tickRotorcraftPhysics() {
        PlanetaryEnvironment env = this.resolveEnvironment();
        LivingEntity seatedPilot = this.getControllingPassenger();
        boolean hasSeatedPilot = seatedPilot != null;
        boolean hasRemotePilot = !hasSeatedPilot && this.remoteHoldTicks > 0;
        boolean hasAnyPilot = hasSeatedPilot || hasRemotePilot;

        double twr = this.derivedStats.computeRotorTwr(env.gravityG(), env.pressureAtm());
        boolean vacuumBlocked = this.derivedStats.isBlockedByVacuum(env.pressureAtm());
        boolean canLift = hasAnyPilot && this.isEngineRunning() && !this.isParkingBrakeOn()
                && this.derivedStats.isDrivable() && !vacuumBlocked && twr >= 1.0 && this.hasAvailablePower();

        float fwdInput = hasSeatedPilot
                ? (this.inputForward ? 1.0F : 0.0F) - (this.inputBackward ? 1.0F : 0.0F)
                : (hasRemotePilot ? this.remoteForward : 0.0F);
        float strafeInput = hasRemotePilot ? this.remoteStrafe : 0.0F;
        float yawInput = hasSeatedPilot
                ? ((this.inputRight ? 1.0F : 0.0F) - (this.inputLeft ? 1.0F : 0.0F)) * 3.1F
                : (hasRemotePilot ? this.remoteYawDelta : 0.0F);
        float collective = hasSeatedPilot
                ? (this.inputBrake ? 1.0F : 0.0F) - (this.inputDescend ? 1.0F : 0.0F)
                : (hasRemotePilot ? this.remoteCollective : 0.0F);

        // Warn pilot if attempting takeoff while missing parts, out of battery, engine off, vacuum-blocked, or low TWR
        if (hasAnyPilot && collective > 0.1F && !canLift && this.warningCooldownTicks == 0) {
            Player targetPlayer = seatedPilot instanceof Player p ? p : this.lastRemotePilot;
            if (targetPlayer != null) {
                if (!this.derivedStats.missingRequiredHardpoints().isEmpty()) {
                    targetPlayer.displayClientMessage(
                            Component.translatable("message.galacticraft.vehicle.missing_parts",
                                    String.join(", ", this.derivedStats.missingRequiredHardpoints())).withStyle(ChatFormatting.RED),
                            true
                    );
                    this.warningCooldownTicks = 40;
                } else if (!this.hasAvailablePower()) {
                    targetPlayer.displayClientMessage(
                            Component.translatable("message.galacticraft.vehicle.no_battery").withStyle(ChatFormatting.RED),
                            true
                    );
                    this.warningCooldownTicks = 40;
                } else if (!this.isEngineRunning()) {
                    targetPlayer.displayClientMessage(
                            Component.translatable("message.galacticraft.vehicle.engine_off").withStyle(ChatFormatting.YELLOW),
                            true
                    );
                    this.warningCooldownTicks = 40;
                } else if (vacuumBlocked) {
                    targetPlayer.displayClientMessage(
                            Component.translatable("message.galacticraft.vehicle.vacuum_rotor_blocked").withStyle(ChatFormatting.RED),
                            true
                    );
                    this.warningCooldownTicks = 40;
                } else if (this.derivedStats.isDrivable() && twr < 1.0) {
                    targetPlayer.displayClientMessage(
                            Component.translatable("message.galacticraft.vehicle.insufficient_twr", String.format("%.2f", twr)).withStyle(ChatFormatting.GOLD),
                            true
                    );
                    this.warningCooldownTicks = 40;
                }
            }
        }

        // 1. Rotor RPM spool-up / spool-down (blur disc engages cleanly above 600 RPM)
        float targetRpm = 0.0F;
        if (this.isEngineRunning() && this.derivedStats.isDrivable() && this.hasAvailablePower()) {
            if (canLift && (!this.onGround() || collective > 0.05F)) {
                targetRpm = 940.0F + Math.max(0.0F, collective) * 340.0F;
            } else if (hasAnyPilot) {
                targetRpm = vacuumBlocked ? 260.0F : 480.0F;
            } else {
                targetRpm = 0.0F;
            }
        }
        float rpmLerp = targetRpm > this.localRotorRpm ? 0.12F : 0.06F;
        this.localRotorRpm = Mth.lerp(rpmLerp, this.localRotorRpm, targetRpm);
        if (this.localRotorRpm < 1.0F) {
            this.localRotorRpm = 0.0F;
        }

        // 2. Yaw rotation
        if (Math.abs(yawInput) > 0.01F && (!this.onGround() || hasRemotePilot || canLift)) {
            this.setYRot(this.getYRot() + yawInput);
        }

        // 3. Cyclic pitch & roll tilt (clamped to +/- 22 deg, coupled directly to horizontal acceleration)
        float targetPitch = (!this.onGround() && canLift) ? Mth.clamp(fwdInput * MAX_CYCLIC_TILT_DEG, -MAX_CYCLIC_TILT_DEG, MAX_CYCLIC_TILT_DEG) : 0.0F;
        float targetRoll;
        if (!this.onGround() && canLift) {
            if (hasRemotePilot) {
                targetRoll = Mth.clamp(strafeInput * MAX_CYCLIC_TILT_DEG, -MAX_CYCLIC_TILT_DEG, MAX_CYCLIC_TILT_DEG);
            } else {
                targetRoll = Mth.clamp(-yawInput * 5.2F, -18.0F, 18.0F);
            }
        } else {
            targetRoll = 0.0F;
        }
        this.localCyclicPitch = Mth.lerp(0.22F, this.localCyclicPitch, targetPitch);
        this.localCyclicRoll = Mth.lerp(0.22F, this.localCyclicRoll, targetRoll);

        // 4. Vertical collective lift vs planetary gravity
        double gravityStep = 0.08 * Math.clamp(env.gravityG(), 0.0, 2.5);
        double vy = this.getDeltaMovement().y;
        boolean readyToFly = canLift && this.localRotorRpm >= 420.0F;
        if (readyToFly) {
            double maxClimbSpeed = Math.clamp(0.28 + Math.min(twr, 4.0) * 0.065, 0.28, 0.54);
            double climbAccel = Math.clamp((twr - 1.0) * 0.04 + 0.036, 0.028, 0.085);
            if (collective > 0.05F) {
                vy = Math.min(maxClimbSpeed * collective, vy + climbAccel);
            } else if (collective < -0.05F) {
                vy = Math.max(-0.38 * Math.abs(collective), vy - 0.042);
            } else {
                // Stabilized hover hold when airborne; stay resting cleanly when grounded
                vy = this.onGround() ? 0.0 : vy * 0.78;
                if (Math.abs(vy) < 0.004) {
                    vy = 0.0;
                }
            }
            if (!this.level().isClientSide) {
                this.consumeRotorPower(env, collective > 0.1F);
            }
        } else {
            if (this.onGround()) {
                vy = Math.max(0.0, vy);
            } else {
                // Soft autorotation descent if blades are spinning in atmosphere
                double fallScale = (!vacuumBlocked && this.localRotorRpm > 200.0F) ? 0.55 : 1.0;
                double terminal = (!vacuumBlocked && this.localRotorRpm > 200.0F) ? -0.45 : -1.25;
                vy = Math.max(terminal, vy - gravityStep * fallScale);
            }
        }

        // 5. Horizontal velocity coupled to cyclic tilt vector
        double maxHorizSpeed = Math.max(0.38, this.derivedStats.maxSpeedBlocksPerTick());
        if (!this.onGround() && readyToFly) {
            double yawRad = Math.toRadians(this.getYRot());
            double fx = -Math.sin(yawRad);
            double fz = Math.cos(yawRad);
            double rx = Math.cos(yawRad);
            double rz = Math.sin(yawRad);

            double pitchFactor = Math.sin(Math.toRadians(this.localCyclicPitch));
            double rollFactor = hasRemotePilot ? Math.sin(Math.toRadians(this.localCyclicRoll)) : 0.0;
            double horizAccel = 0.095 * (this.derivedStats.sizeClass() == VehicleSizeClass.SMALL ? 1.25 : 1.0);
            this.horizVx += (fx * pitchFactor + rx * rollFactor) * horizAccel;
            this.horizVz += (fz * pitchFactor + rz * rollFactor) * horizAccel;

            double drag = env.pressureAtm() >= VehicleDerivedStats.MIN_AERO_PRESSURE_ATM ? 0.90 : 0.94;
            this.horizVx *= drag;
            this.horizVz *= drag;
            double hSpeed = Math.hypot(this.horizVx, this.horizVz);
            if (hSpeed > maxHorizSpeed) {
                double scale = maxHorizSpeed / hSpeed;
                this.horizVx *= scale;
                this.horizVz *= scale;
            }
        } else {
            double damp = this.onGround() ? 0.72 : 0.96;
            this.horizVx *= damp;
            this.horizVz *= damp;
        }
        if (Math.hypot(this.horizVx, this.horizVz) < 0.002) {
            this.horizVx = 0.0;
            this.horizVz = 0.0;
        }

        this.forwardSpeed = Math.hypot(this.horizVx, this.horizVz);
        this.setDeltaMovement(this.horizVx, vy, this.horizVz);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision) {
            this.horizVx *= 0.25;
            this.horizVz *= 0.25;
        }

        // 6. Retractable landing gear automation (GEAR_TRANSIT above 4.0 blocks AGL)
        double agl = this.computeAltitudeAgl();
        float targetGear = (!this.onGround() && agl > GEAR_RETRACT_AGL_BLOCKS) ? 1.0F : 0.0F;
        this.localGearProgress = Mth.lerp(0.12F, this.localGearProgress, targetGear);
        if (Math.abs(this.localGearProgress - targetGear) < 0.01F) {
            this.localGearProgress = targetGear;
        }

        if (!this.level().isClientSide) {
            this.entityData.set(DATA_ROTOR_RPM, this.localRotorRpm);
            this.entityData.set(DATA_CYCLIC_PITCH, this.localCyclicPitch);
            this.entityData.set(DATA_CYCLIC_ROLL, this.localCyclicRoll);
            this.entityData.set(DATA_GEAR_PROGRESS, this.localGearProgress);
            this.entityData.set(DATA_SPEED, (float) this.forwardSpeed);
            this.entityData.set(DATA_ANIM_STATE,
                    (this.localGearProgress > 0.04F && this.localGearProgress < 0.96F)
                            ? VehicleAnimState.GEAR_TRANSIT
                            : VehicleAnimState.NONE);
        }
    }

    /**
     * Keeps synced rotorcraft telemetry, landing gear animation state, pilot warnings, and
     * electric power draw authoritative on the server while a seated player runs client-predicted flight.
     */
    private void tickServerSeatedRotorcraftSync() {
        PlanetaryEnvironment env = this.resolveEnvironment();
        LivingEntity pilot = this.getControllingPassenger();
        boolean hasPilot = pilot != null;
        double twr = this.derivedStats.computeRotorTwr(env.gravityG(), env.pressureAtm());
        boolean vacuumBlocked = this.derivedStats.isBlockedByVacuum(env.pressureAtm());
        boolean canLift = hasPilot && this.isEngineRunning() && !this.isParkingBrakeOn()
                && this.derivedStats.isDrivable() && !vacuumBlocked && twr >= 1.0 && this.hasAvailablePower();

        float collective = hasPilot ? (this.inputBrake ? 1.0F : 0.0F) - (this.inputDescend ? 1.0F : 0.0F) : 0.0F;
        float fwdInput = hasPilot ? (this.inputForward ? 1.0F : 0.0F) - (this.inputBackward ? 1.0F : 0.0F) : 0.0F;
        float yawInput = hasPilot ? ((this.inputRight ? 1.0F : 0.0F) - (this.inputLeft ? 1.0F : 0.0F)) * 3.1F : 0.0F;

        if (hasPilot && collective > 0.1F && !canLift && this.warningCooldownTicks == 0 && pilot instanceof Player p) {
            if (vacuumBlocked) {
                p.displayClientMessage(Component.translatable("message.galacticraft.vehicle.vacuum_rotor_blocked").withStyle(ChatFormatting.RED), true);
                this.warningCooldownTicks = 40;
            } else if (this.derivedStats.isDrivable() && twr < 1.0) {
                p.displayClientMessage(Component.translatable("message.galacticraft.vehicle.insufficient_twr", String.format("%.2f", twr)).withStyle(ChatFormatting.GOLD), true);
                this.warningCooldownTicks = 40;
            }
        }

        float targetRpm = 0.0F;
        if (this.isEngineRunning() && this.derivedStats.isDrivable() && this.hasAvailablePower()) {
            if (canLift && (!this.onGround() || collective > 0.05F)) {
                targetRpm = 940.0F + Math.max(0.0F, collective) * 340.0F;
            } else if (hasPilot) {
                targetRpm = vacuumBlocked ? 260.0F : 480.0F;
            }
        }
        this.localRotorRpm = Mth.lerp(targetRpm > this.localRotorRpm ? 0.12F : 0.06F, this.localRotorRpm, targetRpm);
        float targetPitch = (!this.onGround() && canLift) ? Mth.clamp(fwdInput * MAX_CYCLIC_TILT_DEG, -MAX_CYCLIC_TILT_DEG, MAX_CYCLIC_TILT_DEG) : 0.0F;
        float targetRoll = (!this.onGround() && canLift) ? Mth.clamp(-yawInput * 5.2F, -18.0F, 18.0F) : 0.0F;
        this.localCyclicPitch = Mth.lerp(0.22F, this.localCyclicPitch, targetPitch);
        this.localCyclicRoll = Mth.lerp(0.22F, this.localCyclicRoll, targetRoll);

        double agl = this.computeAltitudeAgl();
        float targetGear = (!this.onGround() && agl > GEAR_RETRACT_AGL_BLOCKS) ? 1.0F : 0.0F;
        this.localGearProgress = Mth.lerp(0.12F, this.localGearProgress, targetGear);

        if (canLift && (!this.onGround() || collective > 0.05F)) {
            this.consumeRotorPower(env, collective > 0.1F);
        }

        this.entityData.set(DATA_ROTOR_RPM, this.localRotorRpm);
        this.entityData.set(DATA_CYCLIC_PITCH, this.localCyclicPitch);
        this.entityData.set(DATA_CYCLIC_ROLL, this.localCyclicRoll);
        this.entityData.set(DATA_GEAR_PROGRESS, this.localGearProgress);
        this.entityData.set(DATA_ANIM_STATE,
                (this.localGearProgress > 0.04F && this.localGearProgress < 0.96F)
                        ? VehicleAnimState.GEAR_TRANSIT
                        : VehicleAnimState.NONE);
    }

    private void consumeRotorPower(@NotNull PlanetaryEnvironment env, boolean climbing) {
        if (this.tickCount % 10 != 0) {
            return;
        }
        VehicleConfiguration cfg = this.getConfiguration();
        long energy = cfg.storedEnergy();
        if (energy <= 0L) {
            return;
        }
        boolean vacuumMode = env.pressureAtm() < VehicleDerivedStats.MIN_AERO_PRESSURE_ATM && this.derivedStats.hasVacuumThrusters();
        long baseDrain = vacuumMode ? 22L : 12L;
        if (climbing) {
            baseDrain += 6L;
        }
        this.entityData.set(DATA_CONFIG, cfg.withResources(Math.max(0L, energy - baseDrain), 0L));
    }

    /**
     * Full fixed-wing aerodynamic flight simulation coupled to {@link PlanetaryEnvironment}:
     * runway taxi and takeoff roll, real lift {@code L = 0.5 * rho * V^2 * S * C_L(alpha)},
     * induced drag, stall speed {@code V_stall} with smooth nose-down recovery ({@code +2.0 deg/tick}),
     * vacuum flight guard ({@code pressureAtm < 0.005}), optional hybrid electric VTOL thruster lift,
     * animated control surfaces (ailerons, elevator, rudder), and retractable landing gear ({@code GEAR_TRANSIT}).
     */
    private void tickFixedWingPhysics() {
        PlanetaryEnvironment env = this.resolveEnvironment();
        LivingEntity pilot = this.getControllingPassenger();
        boolean hasPilot = pilot != null;
        boolean canOperate = hasPilot && this.isEngineRunning() && !this.isParkingBrakeOn()
                && this.derivedStats.isDrivable() && this.hasAvailablePower();

        boolean vacuumBlocked = this.derivedStats.isBlockedByVacuum(env.pressureAtm());
        boolean canHoverVtol = this.derivedStats.canHoverIn(env.gravityG(), env.pressureAtm());
        double stallSpeed = this.derivedStats.computeStallSpeedBlocksPerTick(env.gravityG(), env.pressureAtm());
        double maxSpeed = Math.max(0.55, this.derivedStats.maxSpeedBlocksPerTick());

        // 1. Electric throttle quadrant (retains pilot's lever setting from 0% to 100%; W/R/+ increases, S/C/- decreases, X cuts to 0%)
        if (canOperate) {
            if (this.inputForward) {
                this.localThrottle = Mth.clamp(this.localThrottle + 0.04F, 0.0F, 1.0F);
            } else if (this.inputBackward) {
                this.localThrottle = Mth.clamp(this.localThrottle - 0.05F, 0.0F, 1.0F);
            }
        } else {
            this.localThrottle = Math.max(0.0F, this.localThrottle - 0.06F);
        }

        // 2. Pilot warnings when attempting takeoff without parts/power/atmosphere
        if (hasPilot && (this.inputForward || this.inputBrake) && this.warningCooldownTicks == 0 && pilot instanceof Player p) {
            if (!this.derivedStats.missingRequiredHardpoints().isEmpty()) {
                p.displayClientMessage(
                        Component.translatable("message.galacticraft.vehicle.missing_parts",
                                String.join(", ", this.derivedStats.missingRequiredHardpoints())).withStyle(ChatFormatting.RED),
                        true
                );
                this.warningCooldownTicks = 40;
            } else if (!this.hasAvailablePower()) {
                p.displayClientMessage(Component.translatable("message.galacticraft.vehicle.no_battery").withStyle(ChatFormatting.RED), true);
                this.warningCooldownTicks = 40;
            } else if (!this.isEngineRunning()) {
                p.displayClientMessage(Component.translatable("message.galacticraft.vehicle.engine_off").withStyle(ChatFormatting.YELLOW), true);
                this.warningCooldownTicks = 40;
            } else if (vacuumBlocked) {
                p.displayClientMessage(Component.translatable("message.galacticraft.vehicle.vacuum_fixed_wing_blocked").withStyle(ChatFormatting.RED), true);
                this.warningCooldownTicks = 40;
            } else if (!this.derivedStats.canFlyFixedWingIn(env.gravityG(), env.pressureAtm()) && this.localThrottle > 0.75F) {
                p.displayClientMessage(Component.translatable("message.galacticraft.vehicle.thin_atmosphere_wings").withStyle(ChatFormatting.GOLD), true);
                this.warningCooldownTicks = 60;
            }
        }

        // 3. Electric propeller RPM spool (blur disc engages cleanly above 600 RPM)
        float targetRpm = 0.0F;
        if (this.isEngineRunning() && this.derivedStats.isDrivable() && this.hasAvailablePower()) {
            if (hasPilot) {
                targetRpm = 280.0F + this.localThrottle * 1420.0F;
                if (canHoverVtol && (!this.onGround() || this.inputBrake)) {
                    targetRpm = Math.max(targetRpm, 960.0F);
                }
            }
        }
        this.localRotorRpm = Mth.lerp(targetRpm > this.localRotorRpm ? 0.14F : 0.07F, this.localRotorRpm, targetRpm);
        if (this.localRotorRpm < 1.0F) {
            this.localRotorRpm = 0.0F;
        }

        // 4. Control surfaces (rudder, ailerons, elevator)
        float targetRudder = hasPilot ? ((this.inputRight ? 1.0F : 0.0F) - (this.inputLeft ? 1.0F : 0.0F)) : 0.0F;
        float targetAileron = (!this.onGround() && hasPilot) ? targetRudder : 0.0F;
        float targetElevator = hasPilot ? ((this.inputBrake ? 1.0F : 0.0F) - (this.inputDescend ? 1.0F : 0.0F)) : 0.0F;
        this.localRudder = Mth.lerp(0.28F, this.localRudder, targetRudder);
        this.localAileron = Mth.lerp(0.28F, this.localAileron, targetAileron);
        this.localElevator = Mth.lerp(0.28F, this.localElevator, targetElevator);
        this.steeringAngle = this.localRudder * 26.0F;

        // 5. Hybrid VTOL mode check (when VTOL_THRUSTER pods are installed and plane is below stall speed or in vacuum)
        double aglNow = this.computeAltitudeAgl();
        boolean airborne = !this.onGround() && aglNow >= 0.45;
        boolean vtolHoverActive = canOperate && canHoverVtol
                && (env.pressureAtm() < VehicleDerivedStats.MIN_FIXED_WING_PRESSURE_ATM || this.forwardSpeed < stallSpeed * 0.92)
                && (this.inputBrake || airborne);

        // 6. Stall detection (airborne without active VTOL lift when airspeed < V_stall or in vacuum)
        boolean stalling = airborne && !vtolHoverActive
                && (vacuumBlocked || !Double.isFinite(stallSpeed) || this.forwardSpeed < stallSpeed * 0.96);
        this.localStalling = stalling;

        if (stalling && hasPilot && this.warningCooldownTicks == 0 && pilot instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.galacticraft.vehicle.stall_warning").withStyle(ChatFormatting.RED), true);
            this.warningCooldownTicks = 30;
        }

        // 7. Pitch attitude (negative = nose UP, positive = nose DOWN) and bank roll
        if (vtolHoverActive) {
            float targetPitch = Mth.clamp(this.localThrottle * 8.0F - targetElevator * 6.0F, -14.0F, 14.0F);
            float targetRoll = Mth.clamp(targetRudder * 18.0F, -18.0F, 18.0F);
            this.localCyclicPitch = Mth.lerp(0.18F, this.localCyclicPitch, targetPitch);
            this.localCyclicRoll = Mth.lerp(0.20F, this.localCyclicRoll, targetRoll);
        } else if (stalling) {
            // Smooth nose-down stall recovery (+2.0 deg/tick) so gravity restores airspeed instead of freezing mid-air
            this.localCyclicPitch = Math.min(24.0F, this.localCyclicPitch + 2.0F);
            this.localCyclicRoll = Mth.lerp(0.12F, this.localCyclicRoll, targetAileron * 12.0F);
        } else if (!airborne) {
            boolean readyToRotate = !vacuumBlocked && Double.isFinite(stallSpeed) && this.forwardSpeed >= stallSpeed * 0.72;
            float targetPitch = (readyToRotate && targetElevator > 0.1F) ? -14.0F : 0.0F;
            this.localCyclicPitch = Mth.lerp(0.28F, this.localCyclicPitch, targetPitch);
            this.localCyclicRoll = Mth.lerp(0.28F, this.localCyclicRoll, 0.0F);
        } else {
            // Smooth airborne pitch & coordinated bank roll
            if (Math.abs(targetElevator) > 0.05F) {
                this.localCyclicPitch = Mth.clamp(this.localCyclicPitch - targetElevator * 2.4F, -28.0F, 28.0F);
            } else {
                this.localCyclicPitch = Mth.lerp(0.06F, this.localCyclicPitch, 0.0F);
            }
            float targetRoll = Mth.clamp(targetAileron * 32.0F, -32.0F, 32.0F);
            this.localCyclicRoll = Mth.lerp(0.18F, this.localCyclicRoll, targetRoll);
        }

        // 8. Yaw steering (nose-wheel/rudder on runway; coordinated banked turn + rudder in flight)
        if (this.onGround()) {
            if (Math.abs(this.localRudder) > 0.04F && (Math.abs(this.forwardSpeed) > 0.015 || vtolHoverActive)) {
                this.setYRot(this.getYRot() + this.localRudder * 2.4F);
            }
        } else if (vtolHoverActive) {
            if (Math.abs(this.localRudder) > 0.04F) {
                this.setYRot(this.getYRot() + this.localRudder * 2.8F);
            }
        } else {
            float bankYaw = this.localCyclicRoll * 0.085F + this.localRudder * 0.65F;
            if (Math.abs(bankYaw) > 0.02F) {
                this.setYRot(this.getYRot() + bankYaw);
            }
        }

        // 9. Forward propulsion, runway braking, aerodynamic drag, and pitch-gravity energy exchange
        double propThrustN = this.derivedStats.computeForwardPropThrustN(env.pressureAtm()) * this.localThrottle;
        double groundTaxiN = this.onGround() ? this.derivedStats.totalDriveForceN() * 0.55 * this.localThrottle : 0.0;
        double totalForwardN = propThrustN + groundTaxiN;

        double maxCl = Math.max(0.5, this.derivedStats.effectiveWingMaxCl());
        double noseUpDeg = -this.localCyclicPitch;
        double clRaw = Mth.clamp(0.45 + (noseUpDeg / 18.0) * (maxCl - 0.45), 0.12, maxCl);
        double effectiveCl = stalling ? (clRaw * 0.32) : clRaw;
        double dragN = this.derivedStats.computeFixedWingDragN(this.forwardSpeed, env.pressureAtm(), effectiveCl);

        double mass = Math.max(200.0, this.derivedStats.totalMassKg());
        double thrustAccel = (totalForwardN / mass) / 145.0;
        double dragDecel = (dragN / mass) / 380.0;
        this.forwardSpeed += thrustAccel;
        this.forwardSpeed = Math.max(0.0, this.forwardSpeed - dragDecel);

        // Gravity accelerates plane in a nose-down dive (stall recovery!) and gently decelerates steep climbs
        if (!this.onGround() && !vtolHoverActive) {
            double pitchRad = Math.toRadians(this.localCyclicPitch); // positive = nose down
            double gravityGlide = Math.sin(pitchRad) * 0.032 * Math.clamp(env.gravityG(), 0.15, 2.0);
            this.forwardSpeed = Math.max(0.0, this.forwardSpeed + gravityGlide);
        }

        // Runway braking & rolling resistance
        if (this.onGround()) {
            if (this.isParkingBrakeOn() || (this.inputBackward && this.localThrottle <= 0.05F)) {
                this.forwardSpeed *= 0.80;
            } else if (this.localThrottle <= 0.02F) {
                this.forwardSpeed *= 0.94;
            } else {
                this.forwardSpeed *= 0.985;
            }
            if (vacuumBlocked) {
                // In vacuum with no VTOL thrusters, only slow electric wheel-hub taxiing is possible
                this.forwardSpeed = Math.min(this.forwardSpeed, 0.24);
            }
        } else {
            double airDamp = env.pressureAtm() >= VehicleDerivedStats.MIN_FIXED_WING_PRESSURE_ATM ? 0.992 : 0.988;
            this.forwardSpeed *= airDamp;
        }
        this.forwardSpeed = Math.clamp(this.forwardSpeed, 0.0, maxSpeed);
        if (this.forwardSpeed < 0.002) {
            this.forwardSpeed = 0.0;
        }

        // 10. Vertical lift L vs Weight W (or Hybrid VTOL vertical thrust)
        double gravityStep = 0.08 * Math.clamp(env.gravityG(), 0.0, 2.5);
        double vy = this.getDeltaMovement().y;
        if (vtolHoverActive) {
            double twr = this.derivedStats.computeRotorTwr(env.gravityG(), env.pressureAtm());
            double maxClimb = Math.clamp(0.28 + Math.min(twr, 4.0) * 0.06, 0.28, 0.52);
            double climbAccel = Math.clamp((twr - 1.0) * 0.04 + 0.036, 0.028, 0.082);
            if (targetElevator > 0.05F) {
                vy = Math.min(maxClimb * targetElevator, vy + climbAccel);
            } else if (targetElevator < -0.05F) {
                vy = Math.max(-0.36 * Math.abs(targetElevator), vy - 0.04);
            } else {
                vy = this.onGround() ? 0.0 : vy * 0.78;
                if (Math.abs(vy) < 0.004) {
                    vy = 0.0;
                }
            }
        } else {
            double liftN = this.derivedStats.computeFixedWingLiftN(this.forwardSpeed, env.pressureAtm(), effectiveCl);
            double weightN = this.derivedStats.computeWeightN(env.gravityG());
            double liftRatio = weightN > 1.0 ? (liftN / weightN) : 0.0;

            if (!airborne) {
                boolean liftOff = !vacuumBlocked && liftRatio >= 1.0 && (targetElevator > 0.1F || this.forwardSpeed >= stallSpeed * 1.12);
                if (liftOff) {
                    vy = Math.max(0.16, (liftRatio - 0.85) * 0.28);
                } else {
                    // Maintain slight downward wheel contact so Entity#move keeps onGround() true on flat runways
                    vy = Math.min(vy, -0.04);
                }
            } else if (stalling) {
                // Stall: sharply reduced lift -> natural sink while nose pitches down to recover speed
                double netGravity = gravityStep * Math.max(0.35, 1.0 - liftRatio * 0.55);
                vy = Math.max(-0.85, vy - netGravity);
            } else {
                // Smooth aerodynamic flight path guided by pitch attitude and real L/W ratio
                double desiredVy = this.forwardSpeed * Math.sin(Math.toRadians(noseUpDeg)) * 0.75;
                if (liftRatio < 1.0) {
                    desiredVy = Math.min(desiredVy, -(1.0 - liftRatio) * 0.28);
                }
                vy = Mth.lerp(0.24, vy, Math.clamp(desiredVy, -0.58, 0.46));
            }
        }

        // 11. Apply 3D velocity & movement
        double yawRad = Math.toRadians(this.getYRot());
        this.horizVx = -Math.sin(yawRad) * this.forwardSpeed;
        this.horizVz = Math.cos(yawRad) * this.forwardSpeed;
        this.setDeltaMovement(this.horizVx, vy, this.horizVz);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision) {
            this.forwardSpeed *= 0.25;
        }

        // 12. Electric power draw (100% electric)
        if (!this.level().isClientSide && canOperate && (this.localThrottle > 0.05F || vtolHoverActive)) {
            this.consumeFixedWingPower(vtolHoverActive);
        }

        // 13. Retractable landing gear automation (GEAR_TRANSIT above 4.0 blocks AGL)
        double agl = this.computeAltitudeAgl();
        float targetGear = (!this.onGround() && agl > GEAR_RETRACT_AGL_BLOCKS) ? 1.0F : 0.0F;
        this.localGearProgress = Mth.lerp(0.12F, this.localGearProgress, targetGear);
        if (Math.abs(this.localGearProgress - targetGear) < 0.01F) {
            this.localGearProgress = targetGear;
        }

        if (!this.level().isClientSide) {
            this.entityData.set(DATA_ROTOR_RPM, this.localRotorRpm);
            this.entityData.set(DATA_CYCLIC_PITCH, this.localCyclicPitch);
            this.entityData.set(DATA_CYCLIC_ROLL, this.localCyclicRoll);
            this.entityData.set(DATA_GEAR_PROGRESS, this.localGearProgress);
            this.entityData.set(DATA_SPEED, (float) this.forwardSpeed);
            this.entityData.set(DATA_STEERING, this.steeringAngle);
            this.entityData.set(DATA_THROTTLE, this.localThrottle);
            this.entityData.set(DATA_ELEVATOR, this.localElevator);
            this.entityData.set(DATA_AILERON, this.localAileron);
            this.entityData.set(DATA_RUDDER, this.localRudder);
            this.entityData.set(DATA_STALLING, this.localStalling);
            this.entityData.set(DATA_ANIM_STATE,
                    (this.localGearProgress > 0.04F && this.localGearProgress < 0.96F)
                            ? VehicleAnimState.GEAR_TRANSIT
                            : VehicleAnimState.NONE);
        }
    }

    /**
     * Keeps synced fixed-wing telemetry, control surfaces, landing gear state, stall/vacuum warnings,
     * and electric battery drain authoritative on the server while a seated player runs client-predicted flight.
     */
    private void tickServerSeatedFixedWingSync() {
        PlanetaryEnvironment env = this.resolveEnvironment();
        LivingEntity pilot = this.getControllingPassenger();
        boolean hasPilot = pilot != null;
        boolean canOperate = hasPilot && this.isEngineRunning() && !this.isParkingBrakeOn()
                && this.derivedStats.isDrivable() && this.hasAvailablePower();
        boolean vacuumBlocked = this.derivedStats.isBlockedByVacuum(env.pressureAtm());
        boolean canHoverVtol = this.derivedStats.canHoverIn(env.gravityG(), env.pressureAtm());
        double stallSpeed = this.derivedStats.computeStallSpeedBlocksPerTick(env.gravityG(), env.pressureAtm());

        if (canOperate) {
            if (this.inputForward) {
                this.localThrottle = Mth.clamp(this.localThrottle + 0.045F, 0.0F, 1.0F);
            } else if (this.inputBackward) {
                this.localThrottle = Mth.clamp(this.localThrottle - 0.055F, 0.0F, 1.0F);
            }
        } else {
            this.localThrottle = Math.max(0.0F, this.localThrottle - 0.06F);
        }

        if (hasPilot && (this.inputForward || this.inputBrake) && this.warningCooldownTicks == 0 && pilot instanceof Player p) {
            if (vacuumBlocked) {
                p.displayClientMessage(Component.translatable("message.galacticraft.vehicle.vacuum_fixed_wing_blocked").withStyle(ChatFormatting.RED), true);
                this.warningCooldownTicks = 40;
            } else if (!this.derivedStats.canFlyFixedWingIn(env.gravityG(), env.pressureAtm()) && this.localThrottle > 0.75F) {
                p.displayClientMessage(Component.translatable("message.galacticraft.vehicle.thin_atmosphere_wings").withStyle(ChatFormatting.GOLD), true);
                this.warningCooldownTicks = 60;
            }
        }

        float targetRpm = (this.isEngineRunning() && this.derivedStats.isDrivable() && this.hasAvailablePower() && hasPilot)
                ? (280.0F + this.localThrottle * 1420.0F)
                : 0.0F;
        this.localRotorRpm = Mth.lerp(targetRpm > this.localRotorRpm ? 0.14F : 0.07F, this.localRotorRpm, targetRpm);

        float targetRudder = hasPilot ? ((this.inputRight ? 1.0F : 0.0F) - (this.inputLeft ? 1.0F : 0.0F)) : 0.0F;
        float targetAileron = (!this.onGround() && hasPilot) ? targetRudder : 0.0F;
        float targetElevator = hasPilot ? ((this.inputBrake ? 1.0F : 0.0F) - (this.inputDescend ? 1.0F : 0.0F)) : 0.0F;
        this.localRudder = Mth.lerp(0.28F, this.localRudder, targetRudder);
        this.localAileron = Mth.lerp(0.28F, this.localAileron, targetAileron);
        this.localElevator = Mth.lerp(0.28F, this.localElevator, targetElevator);

        double horizSpeed = Math.hypot(this.getX() - this.xo, this.getZ() - this.zo);
        boolean vtolHoverActive = canOperate && canHoverVtol
                && (env.pressureAtm() < VehicleDerivedStats.MIN_FIXED_WING_PRESSURE_ATM || horizSpeed < stallSpeed * 0.92)
                && (this.inputBrake || !this.onGround());
        this.localStalling = !this.onGround() && !vtolHoverActive
                && (vacuumBlocked || !Double.isFinite(stallSpeed) || horizSpeed < stallSpeed * 0.96);

        double agl = this.computeAltitudeAgl();
        float targetGear = (!this.onGround() && agl > GEAR_RETRACT_AGL_BLOCKS) ? 1.0F : 0.0F;
        this.localGearProgress = Mth.lerp(0.12F, this.localGearProgress, targetGear);

        if (canOperate && (this.localThrottle > 0.05F || vtolHoverActive)) {
            this.consumeFixedWingPower(vtolHoverActive);
        }

        this.entityData.set(DATA_ROTOR_RPM, this.localRotorRpm);
        this.entityData.set(DATA_GEAR_PROGRESS, this.localGearProgress);
        this.entityData.set(DATA_SPEED, (float) horizSpeed);
        this.entityData.set(DATA_THROTTLE, this.localThrottle);
        this.entityData.set(DATA_ELEVATOR, this.localElevator);
        this.entityData.set(DATA_AILERON, this.localAileron);
        this.entityData.set(DATA_RUDDER, this.localRudder);
        this.entityData.set(DATA_STALLING, this.localStalling);
        this.entityData.set(DATA_ANIM_STATE,
                (this.localGearProgress > 0.04F && this.localGearProgress < 0.96F)
                        ? VehicleAnimState.GEAR_TRANSIT
                        : VehicleAnimState.NONE);
    }

    private void consumeFixedWingPower(boolean vtolHoverActive) {
        if (this.tickCount % 10 != 0) {
            return;
        }
        VehicleConfiguration cfg = this.getConfiguration();
        long energy = cfg.storedEnergy();
        if (energy <= 0L) {
            return;
        }
        long drain = vtolHoverActive ? 24L : Math.max(6L, Math.round(14.0F * this.localThrottle));
        this.entityData.set(DATA_CONFIG, cfg.withResources(Math.max(0L, energy - drain), 0L));
    }

    private void tickDrivePhysics() {
        PlanetaryEnvironment env = this.resolveEnvironment();
        boolean hasPilot = this.getControllingPassenger() != null;
        boolean canDrive = hasPilot && this.isEngineRunning() && !this.isParkingBrakeOn()
                && this.derivedStats.isDrivable() && this.hasAvailablePower();

        // Steering update
        float targetSteer = 0.0F;
        if (hasPilot) {
            if (this.inputLeft) {
                targetSteer -= 32.0F;
            }
            if (this.inputRight) {
                targetSteer += 32.0F;
            }
        }
        this.steeringAngle = Mth.lerp(0.32F, this.steeringAngle, targetSteer);

        // Acceleration & braking
        double accel = this.derivedStats.computeGroundAcceleration(env.gravityG());
        double maxSpeed = this.derivedStats.maxSpeedBlocksPerTick();
        if (canDrive) {
            if (this.inputForward) {
                this.forwardSpeed = Math.min(maxSpeed, this.forwardSpeed + accel);
                if (!this.level().isClientSide) {
                    this.consumeDrivePower();
                }
            } else if (this.inputBackward) {
                this.forwardSpeed = Math.max(-maxSpeed * 0.55, this.forwardSpeed - accel * 0.85);
                if (!this.level().isClientSide) {
                    this.consumeDrivePower();
                }
            } else {
                this.forwardSpeed *= this.onGround() ? 0.90 : 0.98;
            }
            if (this.inputBrake) {
                this.forwardSpeed *= 0.72;
            }
        } else {
            this.forwardSpeed *= this.onGround() ? 0.85 : 0.98;
        }

        if (Math.abs(this.forwardSpeed) < 0.002) {
            this.forwardSpeed = 0.0;
        }

        // Yaw turning: Ackermann vs 6x6/Tracked Skid-Steer (allows in-place point turn)
        if (Math.abs(this.steeringAngle) > 0.5F && this.onGround()) {
            if (this.derivedStats.skidSteerEnabled()) {
                float turnRate = (this.steeringAngle / 32.0F) * (Math.abs(this.forwardSpeed) < 0.03 ? 3.2F : 2.6F);
                this.setYRot(this.getYRot() + turnRate);
            } else if (Math.abs(this.forwardSpeed) > 0.01) {
                double wheelbase = this.getChassisDefinition().wheelbaseBlocks();
                double turnRad = (this.forwardSpeed * Math.tan(Math.toRadians(this.steeringAngle))) / wheelbase;
                this.setYRot(this.getYRot() + (float) Math.toDegrees(turnRad));
            }
        }

        // Convert forward speed to world velocity + planetary gravity
        double yawRad = Math.toRadians(this.getYRot());
        double vx = -Math.sin(yawRad) * this.forwardSpeed;
        double vz = Math.cos(yawRad) * this.forwardSpeed;
        double gravityStep = -0.08 * Math.clamp(env.gravityG(), 0.16, 2.5);
        double vy = this.onGround() ? Math.max(0.0, this.getDeltaMovement().y) : Math.max(-1.5, this.getDeltaMovement().y + gravityStep);

        this.setDeltaMovement(vx, vy, vz);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision) {
            this.forwardSpeed *= 0.35;
        }

        if (!this.level().isClientSide) {
            this.entityData.set(DATA_STEERING, this.steeringAngle);
            this.entityData.set(DATA_SPEED, (float) this.forwardSpeed);
        }
    }

    private void tickPowerGeneration() {
        if (this.tickCount % 20 != 0) {
            return;
        }
        VehicleConfiguration cfg = this.getConfiguration();
        long cap = this.derivedStats.energyCapacity();
        if (cap <= 0L || cfg.storedEnergy() >= cap) {
            return;
        }
        long gain = 0L;
        if (this.derivedStats.hasRtgTrickle()) {
            gain += 45L;
        }
        if (this.derivedStats.hasSolarRecharge() && this.level().isDay() && this.level().canSeeSky(this.blockPosition().above())) {
            PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(this.level());
            gain += Math.round(60.0 * env.solarIrradianceFactor());
        }
        if (gain > 0L) {
            long nextEnergy = Math.min(cap, cfg.storedEnergy() + gain);
            this.entityData.set(DATA_CONFIG, cfg.withResources(nextEnergy, cfg.storedFuel()));
        }
    }

    private boolean hasAvailablePower() {
        if (this.derivedStats.energyCapacity() <= 0L) {
            return true;
        }
        return this.getConfiguration().storedEnergy() > 0L;
    }

    private void consumeDrivePower() {
        if (this.tickCount % 10 != 0) {
            return;
        }
        VehicleConfiguration cfg = this.getConfiguration();
        long energy = cfg.storedEnergy();
        if (energy > 0L) {
            this.entityData.set(DATA_CONFIG, cfg.withResources(Math.max(0L, energy - 8L), 0L));
        }
    }

    private void tickClientVisuals() {
        float speed = this.getSyncedSpeed();
        this.clientWheelSpin += speed * 1.45F;
        float targetDoor = (this.areDoorsOpen() || this.getAnimState() == VehicleAnimState.BOARDING_RAMP) ? 1.0F : 0.0F;
        this.clientDoorProgress = Mth.lerp(0.18F, this.clientDoorProgress, targetDoor);

        if (this.isAircraft()) {
            float rpm = this.getRotorRpm();
            // Convert RPM to radians per tick (rpm / 60 rev/s / 20 ticks/s * 2pi)
            this.clientRotorAngle = (this.clientRotorAngle + (float) (rpm * (Math.PI / 600.0))) % Mth.TWO_PI;
            this.clientPitch = Mth.lerp(0.28F, this.clientPitch, this.getCyclicPitchDeg());
            this.clientRoll = Mth.lerp(0.28F, this.clientRoll, this.getCyclicRollDeg());
            this.clientGearProgress = Mth.lerp(0.20F, this.clientGearProgress, this.getGearProgress());
            if (this.isFixedWing()) {
                this.clientElevator = Mth.lerp(0.30F, this.clientElevator, this.getElevatorInput());
                this.clientAileron = Mth.lerp(0.30F, this.clientAileron, this.getAileronInput());
                this.clientRudder = Mth.lerp(0.30F, this.clientRudder, this.getRudderInput());
            }
        } else {
            // 4-corner wheel contact raycast sampling for smooth ground terrain pitch and roll
            VehicleSizeClass size = this.derivedStats.sizeClass();
            double halfW = size == VehicleSizeClass.LARGE ? 1.74 : (size == VehicleSizeClass.MEDIUM ? 1.24 : 0.85);
            double halfL = size == VehicleSizeClass.LARGE ? 2.85 : (size == VehicleSizeClass.MEDIUM ? 1.32 : 0.95);
            double yawRad = Math.toRadians(this.getYRot());
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);
            double hFL = this.sampleWheelGroundY(-halfW, halfL, cos, sin);
            double hFR = this.sampleWheelGroundY(halfW, halfL, cos, sin);
            double hRL = this.sampleWheelGroundY(-halfW, -halfL, cos, sin);
            double hRR = this.sampleWheelGroundY(halfW, -halfL, cos, sin);

            double frontAvg = (hFL + hFR) * 0.5;
            double rearAvg = (hRL + hRR) * 0.5;
            double leftAvg = (hFL + hRL) * 0.5;
            double rightAvg = (hFR + hRR) * 0.5;
            float targetPitch = (float) Mth.clamp(Math.toDegrees(Math.atan2(rearAvg - frontAvg, halfL * 2.0)), -18.0, 18.0);
            float targetRoll = (float) Mth.clamp(Math.toDegrees(Math.atan2(leftAvg - rightAvg, halfW * 2.0)), -16.0, 16.0);
            this.clientPitch = Mth.lerp(0.20F, this.clientPitch, targetPitch);
            this.clientRoll = Mth.lerp(0.20F, this.clientRoll, targetRoll);
        }
        dev.galacticraft.mod.client.gui.screen.ingame.VehicleClientBridge.tickClientEffects(this);
    }

    private double sampleWheelGroundY(double lx, double lz, double cos, double sin) {
        double wx = this.getX() + lx * cos - lz * sin;
        double wz = this.getZ() + lx * sin + lz * cos;
        Vec3 from = new Vec3(wx, this.getY() + 0.85, wz);
        Vec3 to = new Vec3(wx, this.getY() - 0.95, wz);
        BlockHitResult hit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().y : this.getY();
    }

    /**
     * Returns the exact vehicle-local bounding box for each individual hardpoint
     * (wheel-by-wheel hubs, rotor nacelles, wings, propellers, landing gear, electric motor,
     * powerplant, seat, cargo, dashboard dial), shared between the server-side 3D raycast
     * and client-side wireframe highlights.
     */
    public @NotNull List<net.minecraft.world.phys.AABB> getHardpointLocalBoxes(@NotNull VehicleHardpoint hp) {
        VehicleSizeClass size = this.getChassisDefinition().sizeClass();
        double x = hp.localX();
        double y = hp.localY();
        double z = hp.localZ();
        return switch (hp.type()) {
            case LOCOMOTION -> {
                double wheelR = size == VehicleSizeClass.LARGE ? 0.82 : (size == VehicleSizeClass.MEDIUM ? 0.50 : 0.40);
                double pad = wheelR + 0.08;
                double halfThick = size == VehicleSizeClass.LARGE ? 0.44 : 0.30;
                yield List.of(new net.minecraft.world.phys.AABB(x - halfThick, Math.max(0.0, y - pad), z - pad, x + halfThick, y + pad, z + pad));
            }
            case ROTOR -> {
                double rad = size == VehicleSizeClass.LARGE ? 0.72 : (size == VehicleSizeClass.MEDIUM ? 0.56 : 0.42);
                yield List.of(new net.minecraft.world.phys.AABB(x - rad, y - 0.24, z - rad, x + rad, y + 0.34, z + rad));
            }
            case WING -> {
                double span = size == VehicleSizeClass.LARGE ? 7.8 : 4.6;
                double chord = size == VehicleSizeClass.LARGE ? 1.45 : 0.85;
                double x0 = x < 0.0 ? x - span : x;
                double x1 = x < 0.0 ? x : x + span;
                yield List.of(new net.minecraft.world.phys.AABB(x0, y - 0.36, z - chord, x1, y + 0.42, z + chord));
            }
            case PROPELLER -> {
                double r = size == VehicleSizeClass.LARGE ? 1.05 : 0.92;
                yield List.of(new net.minecraft.world.phys.AABB(x - r, y - r, z - 0.36, x + r, y + r, z + 0.48));
            }
            case LANDING_GEAR -> {
                double drop = size == VehicleSizeClass.LARGE ? 0.88 : 0.64;
                yield List.of(new net.minecraft.world.phys.AABB(x - 0.38, Math.max(0.0, y - drop), z - 0.42, x + 0.38, y + 0.36, z + 0.42));
            }
            case MOTOR -> List.of(new net.minecraft.world.phys.AABB(x - 0.42, y, z - 0.38, x + 0.42, y + 0.52, z + 0.38));
            case DASHBOARD -> List.of(new net.minecraft.world.phys.AABB(x - 0.24, y - 0.22, z - 0.22, x + 0.24, y + 0.22, z + 0.22));
            case SEAT -> List.of(new net.minecraft.world.phys.AABB(x - 0.36, y, z - 0.34, x + 0.36, y + 0.86, z + 0.34));
            case POWERPLANT -> List.of(new net.minecraft.world.phys.AABB(x - 0.46, y, z - 0.40, x + 0.46, y + 0.62, z + 0.40));
            case CARGO -> {
                double cw = size == VehicleSizeClass.LARGE ? 1.06 : 0.64;
                double ch = size == VehicleSizeClass.LARGE ? 0.74 : 0.50;
                double cd = cw * 0.65;
                yield List.of(new net.minecraft.world.phys.AABB(x - cw, y, z - cd, x + cw, y + ch, z + cd));
            }
            default -> List.of(new net.minecraft.world.phys.AABB(x - 0.38, y - 0.12, z - 0.38, x + 0.38, y + 0.60, z + 0.38));
        };
    }

    /**
     * Checks if the player is aiming directly at an animated cabin door or rear airlock ramp
     * so Shift+Right-Click can open/close doors directly in-world.
     */
    public boolean isAimingAtDoor(@NotNull Player player) {
        VehicleSizeClass size = this.getChassisDefinition().sizeClass();
        if (size == VehicleSizeClass.SMALL) {
            return false;
        }
        Vec3 eyeWorld = player.getEyePosition(1.0F);
        Vec3 lookWorld = player.getLookAngle();
        double yawRad = Math.toRadians(this.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        Vec3 rel = eyeWorld.subtract(this.getX(), this.getY(), this.getZ());
        Vec3 localOrigin = new Vec3(rel.x * cos + rel.z * sin, rel.y, -rel.x * sin + rel.z * cos);
        Vec3 localDir = new Vec3(lookWorld.x * cos + lookWorld.z * sin, lookWorld.y, -lookWorld.x * sin + lookWorld.z * cos);
        Vec3 localEnd = localOrigin.add(localDir.scale(6.5));

        ResourceLocation chassisId = this.getChassisDefinition().id();
        if (VehicleRegistry.CHASSIS_LIGHT_HELICOPTER.equals(chassisId)) {
            var leftDoor = new net.minecraft.world.phys.AABB(-1.18, 0.72, -0.26, -0.88, 1.98, 1.22);
            var rightDoor = new net.minecraft.world.phys.AABB(0.88, 0.72, -0.26, 1.18, 1.98, 1.22);
            return leftDoor.clip(localOrigin, localEnd).isPresent() || rightDoor.clip(localOrigin, localEnd).isPresent();
        }
        if (VehicleRegistry.CHASSIS_LIGHT_AIRPLANE.equals(chassisId)) {
            var leftDoor = new net.minecraft.world.phys.AABB(-1.18, 0.92, -0.40, -0.82, 2.26, 0.86);
            var rightDoor = new net.minecraft.world.phys.AABB(0.82, 0.92, -0.40, 1.18, 2.26, 0.86);
            return leftDoor.clip(localOrigin, localEnd).isPresent() || rightDoor.clip(localOrigin, localEnd).isPresent();
        }
        if (VehicleRegistry.CHASSIS_HEAVY_VTOL.equals(chassisId)) {
            var vtolRamp = new net.minecraft.world.phys.AABB(-1.08, 0.80, -2.92, 1.08, 2.40, -2.45);
            return vtolRamp.clip(localOrigin, localEnd).isPresent();
        }
        if (VehicleRegistry.CHASSIS_CARGO_AIRPLANE.equals(chassisId)) {
            var cargoRamp = new net.minecraft.world.phys.AABB(-1.42, 1.00, -5.65, 1.42, 2.68, -4.15);
            return cargoRamp.clip(localOrigin, localEnd).isPresent();
        }
        if (size == VehicleSizeClass.MEDIUM) {
            var leftDoor = new net.minecraft.world.phys.AABB(-1.34, 0.96, -0.08, -1.04, 2.08, 1.20);
            var rightDoor = new net.minecraft.world.phys.AABB(1.04, 0.96, -0.08, 1.34, 2.08, 1.20);
            return leftDoor.clip(localOrigin, localEnd).isPresent() || rightDoor.clip(localOrigin, localEnd).isPresent();
        }
        var rearRamp = new net.minecraft.world.phys.AABB(-1.24, 1.42, -4.28, 1.24, 3.06, -3.85);
        return rearRamp.clip(localOrigin, localEnd).isPresent();
    }

    /**
     * Immersive-Vehicles-style 3D per-slot raycast: transforms the player's eye ray into
     * vehicle-local coordinates and returns the closest intersecting {@link VehicleHardpoint}.
     */
    public @Nullable VehicleHardpoint findTargetedHardpoint(
            @NotNull Player player,
            boolean requireEmpty,
            @Nullable VehiclePartDefinition filterPart
    ) {
        Vec3 eyeWorld = player.getEyePosition(1.0F);
        Vec3 lookWorld = player.getLookAngle();
        double yawRad = Math.toRadians(this.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);

        // Inverse transform from world space into vehicle-local space
        Vec3 rel = eyeWorld.subtract(this.getX(), this.getY(), this.getZ());
        Vec3 localOrigin = new Vec3(rel.x * cos + rel.z * sin, rel.y, -rel.x * sin + rel.z * cos);
        Vec3 localDir = new Vec3(lookWorld.x * cos + lookWorld.z * sin, lookWorld.y, -lookWorld.x * sin + lookWorld.z * cos);
        double reach = this.isFixedWing() ? 11.0 : 7.5;
        Vec3 localEnd = localOrigin.add(localDir.scale(reach));

        VehicleConfiguration cfg = this.getConfiguration();
        VehicleHardpoint closest = null;
        double closestDistSqr = Double.MAX_VALUE;
        for (VehicleHardpoint hp : this.getChassisDefinition().hardpoints()) {
            boolean occupied = cfg.hasPart(hp.id());
            if (requireEmpty && occupied) {
                continue;
            }
            if (!requireEmpty && !occupied) {
                continue;
            }
            if (filterPart != null && !hp.accepts(filterPart.type(), filterPart.sizeClass())) {
                continue;
            }
            for (net.minecraft.world.phys.AABB box : this.getHardpointLocalBoxes(hp)) {
                var hit = box.clip(localOrigin, localEnd);
                if (hit.isPresent()) {
                    double d2 = hit.get().distanceToSqr(localOrigin);
                    if (d2 < closestDistSqr) {
                        closestDistSqr = d2;
                        closest = hp;
                    }
                }
            }
        }
        return closest;
    }

    @Override
    public @NotNull InteractionResult interact(Player player, InteractionHand hand) {
        return this.interactFromSection(player, hand, "body");
    }

    public @NotNull InteractionResult interactFromSection(@NotNull Player player, @NotNull InteractionHand hand, @NotNull String section) {
        ItemStack held = player.getItemInHand(hand);

        // 1. Physical in-world part installation
        if (held.getItem() instanceof VehiclePartItem partItem) {
            if (!this.level().isClientSide) {
                return this.tryInstallPart(player, held, partItem.getPartId());
            }
            return InteractionResult.SUCCESS;
        }

        // 1b. Drone Remote Controller pairing on SMALL unmanned rotorcraft drones
        if (held.getItem() instanceof DroneRemoteControllerItem) {
            if (!this.level().isClientSide) {
                if (!this.isRemoteControllableDrone()) {
                    player.displayClientMessage(Component.translatable("message.galacticraft.drone_remote.only_small_drone").withStyle(ChatFormatting.YELLOW), true);
                    return InteractionResult.CONSUME;
                }
                DroneRemoteControllerItem.linkDrone(held, this);
                this.level().playSound(null, this.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.35F);
                player.displayClientMessage(Component.translatable("message.galacticraft.drone_remote.linked").withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 2. Wrench: Shift+Right-Click packs into VehicleCrateItem; Right-Click detaches aimed part
        if (held.getItem() instanceof VehicleWrenchItem) {
            if (!this.level().isClientSide) {
                if (player.isShiftKeyDown()) {
                    return this.packIntoCrate(player);
                }
                return this.tryDetachPart(player);
            }
            return InteractionResult.SUCCESS;
        }

        // 3. Clicking rear cargo section opens cargo inventory directly
        if ("cargo".equals(section) && this.cargoInventory.getContainerSize() > 0) {
            if (!this.level().isClientSide) {
                this.openCargoMenu(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 4. Sneak + Right-Click with empty hand: toggles door/ramp directly when aimed at it,
        // otherwise opens the bottom Immersive-Vehicles-style Cockpit Control & Instrument Bar
        if (player.isShiftKeyDown()) {
            if (this.isAimingAtDoor(player)) {
                if (!this.level().isClientSide) {
                    this.handlePanelAction(player, "toggle_doors", "", "");
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (this.level().isClientSide) {
                dev.galacticraft.mod.client.gui.screen.ingame.VehicleClientBridge.openControlPanel(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 5. Mount seat (or open AI Mission Console / direct player to Drone Remote Controller for unmanned micro-drones)
        if (this.isRemoteControllableDrone()) {
            if (this.derivedStats.hasAiCore()) {
                if (this.level().isClientSide) {
                    dev.galacticraft.mod.client.gui.screen.ingame.VehicleClientBridge.openAiMissionScreen(this);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (!this.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.drone_use_remote").withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.level().isClientSide) {
            if (this.derivedStats.seats().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.no_seat_installed"), true);
                return InteractionResult.CONSUME;
            }
            if (!this.derivedStats.missingRequiredHardpoints().isEmpty()) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.galacticraft.vehicle.missing_parts",
                                String.join(", ", this.derivedStats.missingRequiredHardpoints())
                        ),
                        true
                );
            }
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    public @NotNull InteractionResult tryInstallPart(@NotNull Player player, @NotNull ItemStack stack, @NotNull ResourceLocation partId) {
        VehiclePartDefinition part = VehicleRegistry.getPart(partId);
        if (part == null) {
            return InteractionResult.PASS;
        }
        VehicleChassisDefinition chassis = this.getChassisDefinition();
        VehicleConfiguration current = this.getConfiguration();

        VehicleHardpoint bestTarget = this.findTargetedHardpoint(player, true, part);
        if (bestTarget == null) {
            for (VehicleHardpoint hp : chassis.hardpoints()) {
                if (!current.hasPart(hp.id()) && hp.accepts(part.type(), part.sizeClass())) {
                    bestTarget = hp;
                    break;
                }
            }
        }
        if (bestTarget == null) {
            player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.no_matching_hardpoint"), true);
            return InteractionResult.CONSUME;
        }

        VehicleConfiguration updated = current.withPart(bestTarget.id(), partId);
        // Fully charge newly installed electric powerplant (or motor buffer) so hand-assembled vehicles work immediately
        if (part.type() == HardpointType.POWERPLANT || part.type() == HardpointType.MOTOR) {
            long newCap = this.derivedStats.energyCapacity() + part.energyCapacity();
            long nextEnergy = Math.min(updated.storedEnergy() + part.energyCapacity(), newCap);
            long nextFuel = Math.min(updated.storedFuel() + part.fuelCapacity(), this.derivedStats.fuelCapacity() + part.fuelCapacity());
            updated = updated.withResources(nextEnergy, nextFuel);
        }
        this.setConfiguration(updated);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.55F, 1.35F);
        player.displayClientMessage(
                Component.translatable("message.galacticraft.vehicle.part_installed", Component.translatable(part.translationKey()), bestTarget.id()),
                true
        );
        return InteractionResult.CONSUME;
    }

    public @NotNull InteractionResult tryDetachPart(@NotNull Player player) {
        VehicleConfiguration current = this.getConfiguration();
        if (current.installedParts().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.empty_chassis"), true);
            return InteractionResult.CONSUME;
        }
        // Remove ONLY the exact installed hardpoint the player is aiming at (Immersive Vehicles wrench behavior)
        VehicleHardpoint aimed = this.findTargetedHardpoint(player, false, null);
        if (aimed == null) {
            player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.aim_at_part"), true);
            return InteractionResult.CONSUME;
        }
        String targetHp = aimed.id();
        ResourceLocation targetPartId = current.getPart(targetHp);
        if (targetHp == null || targetPartId == null) {
            return InteractionResult.PASS;
        }
        this.setConfiguration(current.withoutPart(targetHp));
        ItemStack partStack = GCItems.createVehiclePartStack(targetPartId);
        if (!partStack.isEmpty()) {
            if (!player.getInventory().add(partStack)) {
                this.spawnAtLocation(partStack);
            }
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.NEUTRAL, 0.7F, 1.2F);
        player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.part_removed", targetHp), true);
        return InteractionResult.CONSUME;
    }

    public @NotNull InteractionResult packIntoCrate(@NotNull Player player) {
        if (!this.getPassengers().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.cannot_pack_occupied"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack crate = VehicleCrateItem.createCrate(this.getConfiguration(), this.getMissionConfig());
        // Drop any stored cargo items safely so nothing is lost when crating
        if (!this.cargoInventory.isEmpty()) {
            Containers.dropContents(this.level(), this.blockPosition(), this.cargoInventory);
            this.cargoInventory.clearContent();
        }
        if (!player.getInventory().add(crate)) {
            this.spawnAtLocation(crate);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.NEUTRAL, 0.9F, 0.9F);
        VehicleSizeClass size = this.derivedStats.sizeClass();
        player.displayClientMessage(
                Component.translatable("message.galacticraft.vehicle.packed_crate", size.cargoWidth(), size.cargoHeight()),
                true
        );
        this.discard();
        return InteractionResult.CONSUME;
    }

    private void openCargoMenu(@NotNull Player player) {
        int rows = Math.max(1, Math.min(6, (this.cargoInventory.getContainerSize() + 8) / 9));
        MenuType<ChestMenu> menuType = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ChestMenu(menuType, syncId, inv, this.cargoInventory, rows),
                Component.translatable("container.galacticraft.vehicle_cargo")
        ));
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.derivedStats.seats().size();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        if (this.isAutonomousModeActive()) {
            return null;
        }
        List<Entity> passengers = this.getPassengers();
        List<SeatSpec> seats = this.derivedStats.seats();
        for (int i = 0; i < passengers.size() && i < seats.size(); i++) {
            if (seats.get(i).role() == SeatRole.PILOT && passengers.get(i) instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private @Nullable SeatSpec resolveSeatForPassengerIndex(int passengerIdx) {
        List<SeatSpec> seats = this.derivedStats.seats();
        if (seats.isEmpty() || passengerIdx < 0) {
            return null;
        }
        if (this.isAutonomousModeActive()) {
            // Seat passengers in PASSENGER / NPC_CREW seats first while AI_CORE drives autonomously
            List<SeatSpec> ordered = new ArrayList<>(seats.size());
            for (SeatSpec s : seats) {
                if (s.role() != SeatRole.PILOT) {
                    ordered.add(s);
                }
            }
            for (SeatSpec s : seats) {
                if (s.role() == SeatRole.PILOT) {
                    ordered.add(s);
                }
            }
            return passengerIdx < ordered.size() ? ordered.get(passengerIdx) : ordered.get(0);
        }
        return passengerIdx < seats.size() ? seats.get(passengerIdx) : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        int idx = this.getPassengers().indexOf(passenger);
        SeatSpec seat = this.resolveSeatForPassengerIndex(idx);
        Vec3 local = seat != null ? seat.localOffset() : new Vec3(0.0, 0.65, 0.0);

        double lx = local.x;
        double ly = local.y + (this.isAircraft() ? 0.06 : 0.14);
        double lz = local.z;

        // Match aircraft pitch/roll attitude around center-of-mass pivot so pilot stays locked in seat during flight
        if (this.isAircraft()) {
            VehicleSizeClass size = this.derivedStats.sizeClass();
            double pivotY = size == VehicleSizeClass.LARGE ? 1.55 : (size == VehicleSizeClass.MEDIUM ? 1.25 : 0.45);
            double dy = ly - pivotY;
            float rollDeg = this.level().isClientSide ? this.clientRoll : this.getCyclicRollDeg();
            float pitchDeg = this.level().isClientSide ? this.clientPitch : this.getCyclicPitchDeg();
            double rollRad = Math.toRadians(this.isFixedWing() ? rollDeg : -rollDeg);
            double cosR = Math.cos(rollRad);
            double sinR = Math.sin(rollRad);
            double tx = lx * cosR - dy * sinR;
            double ty1 = lx * sinR + dy * cosR;

            double pitchRad = Math.toRadians(pitchDeg);
            double cosP = Math.cos(pitchRad);
            double sinP = Math.sin(pitchRad);
            double ty2 = ty1 * cosP - lz * sinP;
            double tz = ty1 * sinP + lz * cosP;

            lx = tx;
            ly = pivotY + ty2;
            lz = tz;
        }

        // Match exact renderer transform Axis.YP.rotationDegrees(-yRot):
        // local +Z is forward (-sin(yRot), +cos(yRot)), local +X is right (+cos(yRot), +sin(yRot))
        double yawRad = Math.toRadians(this.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double rx = lx * cos - lz * sin;
        double rz = lx * sin + lz * cos;

        // Place passenger's hips directly on the seat cushion using MC 1.21.1 vehicle attachment point
        Vec3 attach = passenger.getVehicleAttachmentPoint(this);
        moveFunction.accept(passenger, this.getX() + rx - attach.x, this.getY() + ly - attach.y, this.getZ() + rz - attach.z);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        int idx = this.getPassengers().indexOf(passenger);
        List<SeatSpec> seats = this.derivedStats.seats();
        double sideOffset = (idx >= 0 && idx < seats.size()) ? seats.get(idx).exitSideOffset() : 1.5;
        if (Math.abs(sideOffset) < 0.5) {
            sideOffset = 1.5;
        }
        if (this.isFixedWing()) {
            double minClear = this.derivedStats.sizeClass() == VehicleSizeClass.LARGE ? 2.65 : 1.85;
            sideOffset = Math.copySign(Math.max(Math.abs(sideOffset), minClear), sideOffset);
        } else if (VehicleRegistry.CHASSIS_HEAVY_HAULER.equals(this.getChassisDefinition().id())) {
            sideOffset = Math.copySign(Math.max(Math.abs(sideOffset), 2.55), sideOffset);
        }
        double yawRad = Math.toRadians(this.getYRot());
        double dx = Math.cos(yawRad) * sideOffset;
        double dz = Math.sin(yawRad) * sideOffset;
        BlockPos candidate = BlockPos.containing(this.getX() + dx, this.getY() + 0.5, this.getZ() + dz);
        Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), candidate, true);
        if (safe != null) {
            return safe;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public ItemStack getDropItem() {
        return VehicleCrateItem.createCrate(this.getConfiguration(), this.getMissionConfig());
    }

    @Override
    protected void destroy(DamageSource source) {
        if (!this.level().isClientSide && !this.cargoInventory.isEmpty()) {
            Containers.dropContents(this.level(), this.blockPosition(), this.cargoInventory);
            this.cargoInventory.clearContent();
        }
        super.destroy(source);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("VehicleConfig", Tag.TAG_COMPOUND)) {
            VehicleConfiguration.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("VehicleConfig"))
                    .result()
                    .ifPresent(this::setConfiguration);
        }
        if (tag.contains("VehicleMission", Tag.TAG_COMPOUND)) {
            VehicleMissionConfig.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("VehicleMission"))
                    .result()
                    .ifPresent(cfg -> this.entityData.set(DATA_MISSION_CONFIG, cfg));
        }
        this.missionController.readAdditional(tag);
        this.entityData.set(DATA_AI_STATE, this.missionController.getState());
        this.entityData.set(DATA_AI_DATA_COUNT, this.missionController.getCollectedDataCount());
        if (tag.contains("CargoItems", Tag.TAG_LIST)) {
            ListTag list = tag.getList("CargoItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < this.cargoInventory.getContainerSize()) {
                    ItemStack.parse(this.registryAccess(), itemTag).ifPresent(stack -> this.cargoInventory.setItem(slot, stack));
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        VehicleConfiguration.CODEC.encodeStart(NbtOps.INSTANCE, this.getConfiguration())
                .result()
                .ifPresent(encoded -> tag.put("VehicleConfig", encoded));
        VehicleMissionConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.getMissionConfig())
                .result()
                .ifPresent(encoded -> tag.put("VehicleMission", encoded));
        this.missionController.saveAdditional(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < this.cargoInventory.getContainerSize(); i++) {
            ItemStack stack = this.cargoInventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                list.add(stack.save(this.registryAccess(), itemTag));
            }
        }
        tag.put("CargoItems", list);
    }
}
