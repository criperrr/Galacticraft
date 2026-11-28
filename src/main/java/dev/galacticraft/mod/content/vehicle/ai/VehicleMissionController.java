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

package dev.galacticraft.mod.content.vehicle.ai;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.component.TelemetryData;
import dev.galacticraft.api.vehicle.VehicleAnimState;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import dev.galacticraft.api.vehicle.VehicleDerivedStats;
import dev.galacticraft.api.vehicle.VehicleDomain;
import dev.galacticraft.api.vehicle.ai.ControlCommand;
import dev.galacticraft.api.vehicle.ai.NavStatus;
import dev.galacticraft.api.vehicle.ai.VehicleAiState;
import dev.galacticraft.api.vehicle.ai.VehicleMissionConfig;
import dev.galacticraft.api.vehicle.ai.VehicleMissionRole;
import dev.galacticraft.api.vehicle.ai.VehicleNavigator;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative autonomous mission FSM and role executor composed onto {@link ModularVehicleEntity}.
 * Executes exclusively on {@code !level().isClientSide} and supports all 3 vehicle domains
 * ({@link GroundNavigator25D}, {@link RotorcraftNavigator3D}, {@link FixedWingNavigator3D})
 * across all 4 autonomous roles ({@code SCIENCE_EXPLORER}, {@code CARGO_SHUTTLE},
 * {@code PASSENGER_TAXI}, {@code ESCORT_PATROL}).
 */
public final class VehicleMissionController {
    public static final double LOW_BATTERY_THRESHOLD = 0.20;
    public static final double FULL_CHARGE_RESUME_THRESHOLD = 0.95;
    public static final int SCAN_DURATION_TICKS = 60;
    public static final int DRILL_DURATION_TICKS = 80;
    public static final int TAXI_BOARDING_WAIT_TICKS = 100;
    public static final int STUCK_REVERSE_TICKS = 12;
    public static final int STUCK_RECOVERY_TOTAL_TICKS = 25;
    public static final long OBSTACLE_BLACKLIST_TTL_TICKS = 200L;
    public static final int MAX_STUCK_RETRIES = 3;
    public static final int ESCORT_REPLAN_INTERVAL_TICKS = 15;
    public static final long CHARGING_TRANSFER_PER_TICK = 250L;

    private VehicleAiState state = VehicleAiState.IDLE;
    private @Nullable VehicleNavigator navigator;
    private @Nullable VehicleDomain navigatorDomain;
    private final VehicleChunkTicketManager chunkTickets = new VehicleChunkTicketManager();

    private int waypointIndex = 0;
    private int workTicks = 0;
    private int idleTransferTicks = 0;
    private int stuckRecoveryTicks = 0;
    private int stuckCounter = 0;
    private int stuckTurnDirection = 1;
    private int escortReplanTicks = 0;
    private int collectedDataCount = 0;
    private boolean returningToBase = false;
    private boolean pendingMissionAfterCharge = false;
    private boolean transferredAnyAtStop = false;
    private boolean hasMovedSinceLastBoard = false;
    private boolean departNowRequested = false;
    private @Nullable Vec3 lastDeparturePos;
    private final List<BlockPos> autoSurveyRoute = new ArrayList<>();
    private final Map<UUID, Long> recentDismountedNpcs = new HashMap<>();

    public @NotNull VehicleAiState getState() {
        return this.state;
    }

    public @Nullable VehicleNavigator getNavigator() {
        return this.navigator;
    }

    public @NotNull VehicleChunkTicketManager getChunkTickets() {
        return this.chunkTickets;
    }

    public int getCollectedDataCount() {
        return this.collectedDataCount;
    }

    public int getWaypointIndex() {
        return this.waypointIndex;
    }

    public void requestImmediateDeparture() {
        this.departNowRequested = true;
    }

    /**
     * Resets transient FSM state when the player updates mission configuration via UI or API.
     */
    public void onConfigUpdated(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        this.waypointIndex = 0;
        this.workTicks = 0;
        this.idleTransferTicks = 0;
        this.stuckRecoveryTicks = 0;
        this.stuckCounter = 0;
        this.returningToBase = false;
        this.pendingMissionAfterCharge = false;
        this.transferredAnyAtStop = false;
        this.hasMovedSinceLastBoard = false;
        this.departNowRequested = false;
        this.autoSurveyRoute.clear();
        if (this.navigator != null) {
            this.navigator.reset();
        }
        if (!cfg.enabled()) {
            this.enterIdle(level, vehicle);
        } else {
            this.state = VehicleAiState.IDLE;
        }
    }

    /**
     * Mandatory cleanup hook invoked from {@link ModularVehicleEntity#remove} and dimension changes.
     */
    public void releaseChunkTicket(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle) {
        this.chunkTickets.release(level, vehicle.getUUID());
    }

    /**
     * Advances the server-authoritative FSM by one tick and computes the physical {@link ControlCommand}.
     */
    public @NotNull ControlCommand tickServer(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle) {
        VehicleDerivedStats stats = vehicle.getDerivedStats();
        VehicleMissionConfig cfg = vehicle.getMissionConfig();

        // 1. Guard: AI_CORE installed, autopilot enabled, drivable, engine running, brake released
        if (!stats.hasAiCore() || !cfg.enabled() || !stats.isDrivable() || !vehicle.isEngineRunning() || vehicle.isParkingBrakeOn()) {
            this.enterIdle(level, vehicle);
            return ControlCommand.stop();
        }

        // Select/refresh pluggable navigator matching the active vehicle domain
        this.ensureNavigatorForDomain(stats.domain());
        if (this.navigator instanceof RotorcraftNavigator3D rotorNav) {
            rotorNav.setHoverAtTarget(!this.returningToBase && cfg.role() == VehicleMissionRole.ESCORT_PATROL);
        }

        // 2. Battery health checks (0% immediate stop + ticket release; <20% RETURN_TO_BASE)
        long energyCap = stats.energyCapacity();
        long storedEnergy = vehicle.getConfiguration().storedEnergy();
        double batteryRatio = energyCap > 0L ? (double) storedEnergy / (double) energyCap : 1.0;

        if (energyCap > 0L && storedEnergy <= 0L) {
            this.chunkTickets.release(level, vehicle.getUUID());
            if (cfg.homeChargerPos().isPresent() && isNearTarget(vehicle, cfg.homeChargerPos().get(), 2.8)) {
                this.enterCharging(level, vehicle);
            } else {
                this.enterIdle(level, vehicle);
            }
            return ControlCommand.stop();
        }

        if (energyCap > 0L
                && batteryRatio < LOW_BATTERY_THRESHOLD
                && cfg.homeChargerPos().isPresent()
                && this.state != VehicleAiState.RETURN_TO_BASE
                && this.state != VehicleAiState.CHARGING) {
            this.pendingMissionAfterCharge = true;
            this.beginReturnToBase(level, vehicle, cfg.homeChargerPos().get());
        }

        // 3. Safe opt-in chunk ticket management (ONLY while moving on an active mission with battery)
        boolean shouldForceLoad = cfg.chunkLoadingEnabled()
                && this.state.isMovingMissionState()
                && (energyCap <= 0L || storedEnergy > 0L);
        this.chunkTickets.update(level, vehicle.getUUID(), vehicle.chunkPosition(), vehicle.tickCount, shouldForceLoad);

        // Track whether taxi has physically moved away from previous boarding station
        if (this.lastDeparturePos != null && !this.hasMovedSinceLastBoard) {
            if (Math.hypot(vehicle.getX() - this.lastDeparturePos.x, vehicle.getZ() - this.lastDeparturePos.z) >= 3.0) {
                this.hasMovedSinceLastBoard = true;
            }
        }

        // Purge expired NPC dismount cooldowns
        long now = level.getGameTime();
        this.recentDismountedNpcs.entrySet().removeIf(e -> e.getValue() <= now);

        // 4. Execute current FSM state
        return switch (this.state) {
            case IDLE -> this.tickIdle(level, vehicle, cfg);
            case PLANNING -> this.tickPlanning(level, vehicle, cfg);
            case TRANSIT, HOVER_HOLD, GO_AROUND -> this.tickTransit(level, vehicle, cfg);
            case RETURN_TO_BASE -> this.tickReturnToBase(level, vehicle, cfg);
            case WORKING -> this.tickWorking(level, vehicle, cfg);
            case BOARDING -> this.tickBoarding(level, vehicle, cfg);
            case STUCK_RECOVERY -> this.tickStuckRecovery(level, vehicle, cfg);
            case CHARGING -> this.tickCharging(level, vehicle, cfg, batteryRatio);
        };
    }

    private void ensureNavigatorForDomain(@NotNull VehicleDomain domain) {
        if (this.navigator != null && this.navigatorDomain == domain) {
            return;
        }
        this.navigatorDomain = domain;
        this.navigator = switch (domain) {
            case ROTORCRAFT, HYBRID_VTOL -> new RotorcraftNavigator3D();
            case FIXED_WING -> new FixedWingNavigator3D();
            case GROUND -> new GroundNavigator25D();
        };
    }

    private void enterIdle(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle) {
        this.state = VehicleAiState.IDLE;
        this.returningToBase = false;
        this.workTicks = 0;
        this.idleTransferTicks = 0;
        this.stuckRecoveryTicks = 0;
        if (vehicle.getAnimState() == VehicleAnimState.SCANNING_TERRAIN
                || vehicle.getAnimState() == VehicleAnimState.DRILLING_SAMPLE
                || vehicle.getAnimState() == VehicleAnimState.BOARDING_RAMP) {
            vehicle.setAnimState(VehicleAnimState.NONE);
        }
        if (vehicle.areDoorsOpen()) {
            vehicle.setDoorsOpenServer(false);
        }
        this.chunkTickets.release(level, vehicle.getUUID());
    }

    private void enterCharging(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle) {
        this.state = VehicleAiState.CHARGING;
        this.returningToBase = false;
        this.workTicks = 0;
        if (vehicle.getAnimState() != VehicleAnimState.NONE) {
            vehicle.setAnimState(VehicleAnimState.NONE);
        }
        if (vehicle.areDoorsOpen()) {
            vehicle.setDoorsOpenServer(false);
        }
        this.chunkTickets.release(level, vehicle.getUUID());
    }

    private void beginReturnToBase(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull BlockPos homePos) {
        this.returningToBase = true;
        this.state = VehicleAiState.RETURN_TO_BASE;
        this.workTicks = 0;
        this.stuckCounter = 0;
        vehicle.setAnimState(VehicleAnimState.NONE);
        if (vehicle.areDoorsOpen()) {
            vehicle.setDoorsOpenServer(false);
        }
        if (isNearTarget(vehicle, homePos, 2.2) && vehicle.onGround()) {
            this.enterCharging(level, vehicle);
            return;
        }
        if (this.navigator != null) {
            this.navigator.setTarget(homePos);
            this.navigator.tickPlanning(level, vehicle, GroundNavigator25D.DEFAULT_MAX_NODES_PER_TICK);
        }
    }

    private @NotNull ControlCommand tickIdle(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        if (cfg.role() == VehicleMissionRole.ESCORT_PATROL) {
            if (this.escortReplanTicks > 0) {
                this.escortReplanTicks--;
                return ControlCommand.stop();
            }
            double desiredOffset = vehicle.isAircraft() ? Math.max(8.0, cfg.escortOffsetBlocks()) : cfg.escortOffsetBlocks();
            double ownerDist = this.distanceToEscortOwner(level, vehicle, cfg);
            if (!vehicle.isAircraft() && ownerDist <= desiredOffset + 2.5) {
                this.escortReplanTicks = ESCORT_REPLAN_INTERVAL_TICKS;
                return ControlCommand.stop();
            }
        }
        BlockPos nextTarget = this.resolveNextMissionTarget(level, vehicle, cfg);
        if (nextTarget == null) {
            return ControlCommand.stop();
        }
        // If already docked at the initial mission waypoint, start role work/boarding directly
        if (cfg.role() != VehicleMissionRole.ESCORT_PATROL && isNearTarget(vehicle, nextTarget, 2.2) && vehicle.onGround()) {
            this.onArrivedAtMissionTarget(level, vehicle, cfg);
            return ControlCommand.stop();
        }
        this.returningToBase = false;
        this.state = VehicleAiState.PLANNING;
        if (this.navigator != null) {
            this.navigator.setTarget(nextTarget);
            this.navigator.tickPlanning(level, vehicle, GroundNavigator25D.DEFAULT_MAX_NODES_PER_TICK);
        }
        return ControlCommand.stop();
    }

    private @NotNull ControlCommand tickPlanning(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        if (this.navigator == null || this.navigator.getTarget() == null) {
            this.state = VehicleAiState.IDLE;
            return ControlCommand.stop();
        }

        if (cfg.role() != VehicleMissionRole.ESCORT_PATROL && isNearTarget(vehicle, this.navigator.getTarget(), 2.0) && vehicle.onGround()) {
            this.onArrivedAtMissionTarget(level, vehicle, cfg);
            return ControlCommand.stop();
        }

        this.navigator.tickPlanning(level, vehicle, GroundNavigator25D.DEFAULT_MAX_NODES_PER_TICK);
        NavStatus navStatus = this.navigator.getStatus();
        if (navStatus == NavStatus.FOLLOWING) {
            this.state = VehicleAiState.TRANSIT;
            return this.navigator.computeControl(level, vehicle);
        }
        if (navStatus == NavStatus.ARRIVED) {
            this.onArrivedAtMissionTarget(level, vehicle, cfg);
            return ControlCommand.stop();
        }
        if (navStatus == NavStatus.FAILED) {
            if (cfg.role() == VehicleMissionRole.ESCORT_PATROL) {
                // Owner likely walked indoors into space too narrow for the vehicle footprint: pause safely
                this.escortReplanTicks = ESCORT_REPLAN_INTERVAL_TICKS;
                this.state = VehicleAiState.IDLE;
            } else {
                this.enterIdle(level, vehicle);
            }
        }
        return ControlCommand.stop();
    }

    private @NotNull ControlCommand tickTransit(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        if (this.navigator == null) {
            this.enterIdle(level, vehicle);
            return ControlCommand.stop();
        }

        // Dynamic escort retargeting every 15 ticks
        if (cfg.role() == VehicleMissionRole.ESCORT_PATROL) {
            this.escortReplanTicks++;
            if (this.escortReplanTicks >= ESCORT_REPLAN_INTERVAL_TICKS) {
                this.escortReplanTicks = 0;
                BlockPos escortPos = this.computeEscortTarget(level, vehicle, cfg);
                if (escortPos == null) {
                    this.enterIdle(level, vehicle);
                    return ControlCommand.stop();
                }
                double distToOwner = this.distanceToEscortOwner(level, vehicle, cfg);
                double desiredOffset = vehicle.isAircraft() ? Math.max(8.0, cfg.escortOffsetBlocks()) : cfg.escortOffsetBlocks();
                if (distToOwner <= desiredOffset + 1.5 && vehicle.onGround()) {
                    return ControlCommand.stop();
                }
                if (this.navigator.getTarget() == null || this.navigator.getTarget().distSqr(escortPos) > 9.0) {
                    this.navigator.setTarget(escortPos);
                    this.state = VehicleAiState.PLANNING;
                    return ControlCommand.stop();
                }
            }
        }

        if (this.navigator.getStatus() == NavStatus.PLANNING) {
            this.navigator.tickPlanning(level, vehicle, GroundNavigator25D.DEFAULT_MAX_NODES_PER_TICK);
            if (this.navigator.getStatus() == NavStatus.PLANNING) {
                return ControlCommand.stop();
            }
        }

        ControlCommand cmd = this.navigator.computeControl(level, vehicle);
        NavStatus navStatus = this.navigator.getStatus();
        switch (navStatus) {
            case FOLLOWING -> this.state = VehicleAiState.TRANSIT;
            case HOVER_HOLD -> this.state = VehicleAiState.HOVER_HOLD;
            case GO_AROUND -> this.state = VehicleAiState.GO_AROUND;
            case ARRIVED -> {
                this.stuckCounter = 0;
                this.onArrivedAtMissionTarget(level, vehicle, cfg);
                return ControlCommand.stop();
            }
            case STUCK -> {
                this.beginStuckRecovery(level, vehicle);
                return ControlCommand.stop();
            }
            case FAILED -> {
                this.enterIdle(level, vehicle);
                return ControlCommand.stop();
            }
            default -> {
            }
        }
        return cmd;
    }

    private @NotNull ControlCommand tickReturnToBase(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        BlockPos home = cfg.homeChargerPos().orElse(null);
        if (home == null || this.navigator == null) {
            this.enterIdle(level, vehicle);
            return ControlCommand.stop();
        }
        if (isNearTarget(vehicle, home, 2.2) && vehicle.onGround()) {
            this.enterCharging(level, vehicle);
            return ControlCommand.stop();
        }
        if (this.navigator.getTarget() == null || !this.navigator.getTarget().equals(home)) {
            this.navigator.setTarget(home);
        }
        if (this.navigator.getStatus() == NavStatus.PLANNING) {
            this.navigator.tickPlanning(level, vehicle, GroundNavigator25D.DEFAULT_MAX_NODES_PER_TICK);
            if (this.navigator.getStatus() == NavStatus.PLANNING) {
                return ControlCommand.stop();
            }
        }
        ControlCommand cmd = this.navigator.computeControl(level, vehicle);
        NavStatus navStatus = this.navigator.getStatus();
        if (navStatus == NavStatus.ARRIVED) {
            this.enterCharging(level, vehicle);
            return ControlCommand.stop();
        }
        if (navStatus == NavStatus.STUCK) {
            this.beginStuckRecovery(level, vehicle);
            return ControlCommand.stop();
        }
        if (navStatus == NavStatus.FAILED) {
            this.enterIdle(level, vehicle);
            return ControlCommand.stop();
        }
        return cmd;
    }

    private void beginStuckRecovery(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle) {
        this.stuckCounter++;
        if (this.stuckCounter > MAX_STUCK_RETRIES) {
            this.notifyOwner(level, vehicle, Component.translatable("message.galacticraft.vehicle.ai.stuck_aborted").withStyle(ChatFormatting.RED));
            this.enterIdle(level, vehicle);
            return;
        }
        this.state = VehicleAiState.STUCK_RECOVERY;
        this.stuckRecoveryTicks = 0;
        this.stuckTurnDirection = (this.stuckCounter % 2 == 1) ? 1 : -1;

        // Immediately blacklist the obstructing cell ahead for 200 ticks
        BlockPos obstructing = null;
        if (this.navigator instanceof GroundNavigator25D ground && ground.getLastBlockingCell() != null) {
            obstructing = ground.getLastBlockingCell();
        }
        if (obstructing == null) {
            double yawRad = Math.toRadians(vehicle.getYRot());
            obstructing = BlockPos.containing(
                    vehicle.getX() - Math.sin(yawRad) * 1.8,
                    vehicle.getY(),
                    vehicle.getZ() + Math.cos(yawRad) * 1.8
            );
        }
        if (this.navigator != null) {
            this.navigator.blacklistCell(obstructing, level.getGameTime(), OBSTACLE_BLACKLIST_TTL_TICKS);
        }
    }

    private @NotNull ControlCommand tickStuckRecovery(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        this.stuckRecoveryTicks++;
        if (this.stuckRecoveryTicks <= STUCK_REVERSE_TICKS) {
            // Phase 1: reverse ~1.5 blocks clear of obstacle
            return ControlCommand.ground(-0.15, 0.0F, 0.0F, false, false);
        }
        if (this.stuckRecoveryTicks <= STUCK_RECOVERY_TOTAL_TICKS) {
            // Phase 2: turn 45 degrees away from obstacle (13 ticks * 3.5 deg = 45.5 deg)
            float yawRate = 3.5F * this.stuckTurnDirection;
            return ControlCommand.ground(0.0, 35.0F * this.stuckTurnDirection, yawRate, false, true);
        }

        // Recovery complete -> trigger fresh replan toward active destination
        BlockPos resumeTarget = this.returningToBase
                ? cfg.homeChargerPos().orElse(null)
                : (this.navigator != null ? this.navigator.getTarget() : this.resolveNextMissionTarget(level, vehicle, cfg));
        if (resumeTarget != null && this.navigator != null) {
            this.navigator.setTarget(resumeTarget);
            this.state = this.returningToBase ? VehicleAiState.RETURN_TO_BASE : VehicleAiState.PLANNING;
        } else {
            this.enterIdle(level, vehicle);
        }
        return ControlCommand.stop();
    }

    private void onArrivedAtMissionTarget(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        this.workTicks = 0;
        this.idleTransferTicks = 0;
        this.transferredAnyAtStop = false;
        this.departNowRequested = false;

        switch (cfg.role()) {
            case SCIENCE_EXPLORER -> {
                this.state = VehicleAiState.WORKING;
                VehicleDerivedStats stats = vehicle.getDerivedStats();
                VehicleAnimState anim = (stats.hasDrillArm() && !stats.hasCameraMast())
                        ? VehicleAnimState.DRILLING_SAMPLE
                        : VehicleAnimState.SCANNING_TERRAIN;
                vehicle.setAnimState(anim);
                level.playSound(null, vehicle.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.55F, 1.45F);
            }
            case CARGO_SHUTTLE -> {
                this.state = VehicleAiState.WORKING;
                vehicle.setAnimState(VehicleAnimState.BOARDING_RAMP);
                vehicle.setDoorsOpenServer(true);
                level.playSound(null, vehicle.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 0.65F, 0.95F);
            }
            case PASSENGER_TAXI -> {
                this.state = VehicleAiState.BOARDING;
                vehicle.setAnimState(VehicleAnimState.BOARDING_RAMP);
                vehicle.setDoorsOpenServer(true);
                level.playSound(null, vehicle.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 0.65F, 1.05F);
                if (this.hasMovedSinceLastBoard) {
                    this.dismountNpcPassengersSafely(level, vehicle);
                    for (Entity rider : vehicle.getPassengers()) {
                        if (rider instanceof Player player) {
                            player.displayClientMessage(
                                    Component.translatable("message.galacticraft.vehicle.ai.taxi_arrived", this.waypointIndex + 1).withStyle(ChatFormatting.AQUA),
                                    true
                            );
                        }
                    }
                }
            }
            case ESCORT_PATROL -> {
                // Hold position cleanly beside owner until owner moves away
                this.state = VehicleAiState.IDLE;
                this.escortReplanTicks = 0;
            }
        }
    }

    private @NotNull ControlCommand tickWorking(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        this.workTicks++;
        if (cfg.role() == VehicleMissionRole.SCIENCE_EXPLORER) {
            return this.tickScienceWork(level, vehicle, cfg);
        }
        if (cfg.role() == VehicleMissionRole.CARGO_SHUTTLE) {
            return this.tickCargoShuttleWork(level, vehicle, cfg);
        }
        this.state = VehicleAiState.IDLE;
        return ControlCommand.stop();
    }

    private @NotNull ControlCommand tickScienceWork(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        boolean drilling = vehicle.getAnimState() == VehicleAnimState.DRILLING_SAMPLE;
        int duration = drilling ? DRILL_DURATION_TICKS : SCAN_DURATION_TICKS;

        // Emit subtle server-synced telemetry/drill particles & sound feedback
        if (this.workTicks % 10 == 0) {
            double yawRad = Math.toRadians(vehicle.getYRot());
            double fx = vehicle.getX() - Math.sin(yawRad) * 1.4;
            double fz = vehicle.getZ() + Math.cos(yawRad) * 1.4;
            if (drilling) {
                BlockState below = level.getBlockState(vehicle.blockPosition().below());
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), fx, vehicle.getY() + 0.15, fz, 6, 0.25, 0.15, 0.25, 0.04);
                level.playSound(null, vehicle.blockPosition(), SoundEvents.STONE_HIT, SoundSource.NEUTRAL, 0.45F, 0.75F);
            } else {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, fx, vehicle.getY() + 0.35, fz, 5, 0.35, 0.15, 0.35, 0.02);
            }
        }

        if (this.workTicks < duration) {
            return ControlCommand.stop();
        }

        // Survey/drilling complete: encode real planetary/biome/ore telemetry onto a Recorded Data Drive
        ItemStack dataDrive = this.createSurveyDataDrive(level, vehicle, drilling);
        this.insertIntoCargo(vehicle.getCargoInventory(), dataDrive);
        this.collectedDataCount++;
        level.playSound(null, vehicle.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.65F, 1.35F);

        vehicle.setAnimState(VehicleAnimState.NONE);
        this.workTicks = 0;

        List<BlockPos> route = this.resolveScienceRoute(vehicle, cfg);
        this.waypointIndex++;

        boolean routeFinished = this.waypointIndex >= route.size();
        boolean cargoFull = isCargoFull(vehicle.getCargoInventory());
        if (routeFinished || cargoFull) {
            this.waypointIndex = 0;
            if (cfg.homeChargerPos().isPresent()) {
                this.pendingMissionAfterCharge = false;
                this.beginReturnToBase(level, vehicle, cfg.homeChargerPos().get());
            } else {
                this.enterIdle(level, vehicle);
            }
        } else {
            BlockPos nextWp = route.get(this.waypointIndex);
            this.state = VehicleAiState.PLANNING;
            if (this.navigator != null) {
                this.navigator.setTarget(nextWp);
            }
        }
        return ControlCommand.stop();
    }

    private @NotNull ItemStack createSurveyDataDrive(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, boolean drilled) {
        PlanetaryEnvironment env = vehicle.resolveEnvironment();
        BlockPos center = vehicle.blockPosition();
        String biomePath = level.getBiome(center).unwrapKey()
                .map(k -> k.location().getPath())
                .orElse("unknown");

        // Scan 5x5x5 cube around target for ore signatures
        int oreBlocks = 0;
        String detectedOre = "regolith";
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(cursor).getBlock()).getPath();
                    if (path.contains("ore")) {
                        oreBlocks++;
                        detectedOre = path;
                    }
                }
            }
        }

        String instrumentId = drilled ? "geological_drill_arm" : "camera_mast_360";
        int sciencePoints = (drilled ? 45 : 30) + Math.min(25, oreBlocks * 5);
        int domainPoints = drilled ? 35 : 25;
        String atmSummary = String.format(Locale.ROOT, "g=%.2fg, P=%.3fatm, T=%.0fK", env.gravityG(), env.pressureAtm(), env.meanTempKelvin());
        String note = String.format(Locale.ROOT, "Biome=%s | Scan=%s (%d) @ [%d,%d,%d]",
                biomePath, detectedOre, oreBlocks, center.getX(), center.getY(), center.getZ());

        TelemetryData payload = new TelemetryData(
                env.id(),
                env.displayName(),
                TelemetryData.EventType.SURFACE_ISRU_SAMPLE,
                instrumentId,
                "geology",
                sciencePoints,
                domainPoints,
                atmSummary,
                note,
                drilled ? 125 : 100
        );

        ItemStack stack = new ItemStack(GCItems.RECORDED_DATA_DRIVE);
        stack.set(GCDataComponents.TELEMETRY_DATA, payload);
        return stack;
    }

    private @NotNull ControlCommand tickCargoShuttleWork(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        vehicle.setAnimState(VehicleAnimState.BOARDING_RAMP);
        vehicle.setDoorsOpenServer(true);

        boolean isOriginLoad = this.waypointIndex == 0;
        if (this.workTicks % 5 == 0) {
            long moved = this.transferItemsViaFabricApi(level, vehicle, isOriginLoad);
            this.tryDrawChargerEnergy(level, vehicle, vehicle.blockPosition());
            if (moved > 0L) {
                this.transferredAnyAtStop = true;
                this.idleTransferTicks = 0;
            } else {
                this.idleTransferTicks += 5;
            }
        }

        SimpleContainer cargo = vehicle.getCargoInventory();
        boolean shouldDepart = switch (cfg.cargoLoadCondition()) {
            case FULL -> isOriginLoad
                    ? (isCargoFull(cargo) || (this.transferredAnyAtStop && this.idleTransferTicks >= 60))
                    : (cargo.isEmpty() || this.idleTransferTicks >= 60);
            case EMPTY -> !isOriginLoad
                    ? cargo.isEmpty()
                    : (isCargoFull(cargo) || (this.transferredAnyAtStop && this.idleTransferTicks >= 60));
            case INACTIVITY_60_TICKS -> this.idleTransferTicks >= 60
                    || (this.workTicks >= 15 && isOriginLoad && isCargoFull(cargo))
                    || (this.workTicks >= 15 && !isOriginLoad && cargo.isEmpty() && this.transferredAnyAtStop);
            case TIMER_200_TICKS -> this.workTicks >= 200;
        };

        if (shouldDepart) {
            vehicle.setDoorsOpenServer(false);
            vehicle.setAnimState(VehicleAnimState.NONE);
            level.playSound(null, vehicle.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 0.65F, 0.95F);
            this.workTicks = 0;
            this.idleTransferTicks = 0;

            List<BlockPos> stops = cfg.waypoints();
            if (stops.size() <= 1) {
                this.enterIdle(level, vehicle);
            } else {
                this.waypointIndex = (this.waypointIndex + 1) % stops.size();
                BlockPos nextStop = stops.get(this.waypointIndex);
                this.state = VehicleAiState.PLANNING;
                if (this.navigator != null) {
                    this.navigator.setTarget(nextStop);
                }
            }
        }
        return ControlCommand.stop();
    }

    /**
     * Atomically transfers items between adjacent pad storages and the vehicle cargo bay
     * via the Fabric Transfer API ({@link ItemStorage#SIDED} and {@link Transaction#openOuter()}).
     */
    private long transferItemsViaFabricApi(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, boolean loadIntoVehicle) {
        SimpleContainer cargo = vehicle.getCargoInventory();
        if (cargo.getContainerSize() <= 0) {
            return 0L;
        }
        InventoryStorage vehicleStorage = InventoryStorage.of(cargo, null);
        BlockPos center = vehicle.blockPosition();
        long totalMoved = 0L;

        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    scan.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    Storage<ItemVariant> external = findItemStorage(level, scan);
                    if (external == null) {
                        continue;
                    }
                    try (Transaction tx = Transaction.openOuter()) {
                        long moved = loadIntoVehicle
                                ? StorageUtil.move(external, vehicleStorage, variant -> true, 64L, tx)
                                : StorageUtil.move(vehicleStorage, external, variant -> true, 64L, tx);
                        if (moved > 0L) {
                            tx.commit();
                            totalMoved += moved;
                        }
                    }
                }
            }
        }
        return totalMoved;
    }

    private static @Nullable Storage<ItemVariant> findItemStorage(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        Storage<ItemVariant> up = ItemStorage.SIDED.find(level, pos, Direction.UP);
        if (up != null) {
            return up;
        }
        Storage<ItemVariant> unDir = ItemStorage.SIDED.find(level, pos, null);
        if (unDir != null) {
            return unDir;
        }
        for (Direction dir : Direction.values()) {
            Storage<ItemVariant> found = ItemStorage.SIDED.find(level, pos, dir);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private @NotNull ControlCommand tickBoarding(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        this.workTicks++;
        vehicle.setAnimState(VehicleAnimState.BOARDING_RAMP);
        vehicle.setDoorsOpenServer(true);

        // Auto-board nearby waiting villagers / MoonVillagerEntity commuters into available seats
        if (cfg.autoBoardNpcs() && this.workTicks % 5 == 0) {
            this.tryBoardNearbyVillagers(level, vehicle);
        }

        if (this.workTicks >= TAXI_BOARDING_WAIT_TICKS || this.departNowRequested) {
            this.departNowRequested = false;
            vehicle.setDoorsOpenServer(false);
            vehicle.setAnimState(VehicleAnimState.NONE);
            level.playSound(null, vehicle.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 0.65F, 1.05F);
            this.workTicks = 0;
            this.lastDeparturePos = vehicle.position();
            this.hasMovedSinceLastBoard = false;

            List<BlockPos> stations = cfg.waypoints();
            if (stations.size() <= 1) {
                this.enterIdle(level, vehicle);
            } else {
                this.waypointIndex = (this.waypointIndex + 1) % stations.size();
                BlockPos nextStation = stations.get(this.waypointIndex);
                this.state = VehicleAiState.PLANNING;
                if (this.navigator != null) {
                    this.navigator.setTarget(nextStation);
                }
            }
        }
        return ControlCommand.stop();
    }

    private void tryBoardNearbyVillagers(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle) {
        int maxSeats = vehicle.getDerivedStats().seats().size();
        if (maxSeats <= 0 || vehicle.getPassengers().size() >= maxSeats) {
            return;
        }
        long now = level.getGameTime();
        AABB box = vehicle.getBoundingBox().inflate(5.0, 2.0, 5.0);
        List<Villager> commuters = level.getEntitiesOfClass(Villager.class, box, v ->
                v.isAlive()
                        && !v.isPassenger()
                        && !this.recentDismountedNpcs.containsKey(v.getUUID())
        );
        for (Villager commuter : commuters) {
            if (vehicle.getPassengers().size() >= maxSeats) {
                break;
            }
            if (commuter.startRiding(vehicle, true)) {
                this.recentDismountedNpcs.remove(commuter.getUUID());
                level.playSound(null, vehicle.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6F, 1.0F);
            }
        }
    }

    private void dismountNpcPassengersSafely(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle) {
        List<Entity> passengersCopy = new ArrayList<>(vehicle.getPassengers());
        long cooldownUntil = level.getGameTime() + 200L;
        for (Entity rider : passengersCopy) {
            if (rider instanceof Villager villager) {
                Vec3 safePos = findSafeExitPosition(level, vehicle);
                villager.stopRiding();
                villager.fallDistance = 0.0F;
                villager.teleportTo(safePos.x, safePos.y, safePos.z);
                this.recentDismountedNpcs.put(villager.getUUID(), cooldownUntil);
            }
        }
    }

    private static @NotNull Vec3 findSafeExitPosition(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle) {
        double yawRad = Math.toRadians(vehicle.getYRot());
        double sideDist = Math.max(1.8, vehicle.getBbWidth() * 0.6 + 1.1);
        double[] angles = {yawRad + Math.PI * 0.5, yawRad - Math.PI * 0.5, yawRad + Math.PI, yawRad};
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (double angle : angles) {
            int cx = Mth.floor(vehicle.getX() - Math.sin(angle) * sideDist);
            int cz = Mth.floor(vehicle.getZ() + Math.cos(angle) * sideDist);
            int baseY = Mth.floor(vehicle.getY());
            for (int dy = 1; dy >= -2; dy--) {
                int y = baseY + dy;
                cursor.set(cx, y - 1, cz);
                if (Costmap25D.isStandableFloor(level, cursor, level.getBlockState(cursor))
                        && Costmap25D.hasClearHeadroom(level, cursor, cx, y, cz, 2)) {
                    return new Vec3(cx + 0.5, y, cz + 0.5);
                }
            }
        }
        return new Vec3(vehicle.getX(), vehicle.getY() + 0.25, vehicle.getZ());
    }

    private @NotNull ControlCommand tickCharging(
            @NotNull ServerLevel level,
            @NotNull ModularVehicleEntity vehicle,
            @NotNull VehicleMissionConfig cfg,
            double batteryRatio
    ) {
        this.chunkTickets.release(level, vehicle.getUUID());
        BlockPos padCenter = cfg.homeChargerPos().orElse(vehicle.blockPosition());
        this.tryDrawChargerEnergy(level, vehicle, padCenter);

        VehicleDerivedStats stats = vehicle.getDerivedStats();
        long cap = stats.energyCapacity();
        long energy = vehicle.getConfiguration().storedEnergy();
        double updatedRatio = cap > 0L ? (double) energy / (double) cap : 1.0;

        if (updatedRatio >= FULL_CHARGE_RESUME_THRESHOLD) {
            if (this.pendingMissionAfterCharge && cfg.enabled()) {
                this.pendingMissionAfterCharge = false;
                this.returningToBase = false;
                this.state = VehicleAiState.IDLE;
            } else {
                this.enterIdle(level, vehicle);
            }
        }
        return ControlCommand.stop();
    }

    private void tryDrawChargerEnergy(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull BlockPos padCenter) {
        VehicleDerivedStats stats = vehicle.getDerivedStats();
        long cap = stats.energyCapacity();
        VehicleConfiguration cfg = vehicle.getConfiguration();
        long missing = cap - cfg.storedEnergy();
        if (cap <= 0L || missing <= 0L) {
            return;
        }

        long maxPull = Math.min(missing, CHARGING_TRANSFER_PER_TICK);
        long pulled = 0L;
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2 && pulled < maxPull; dx++) {
            for (int dy = -1; dy <= 1 && pulled < maxPull; dy++) {
                for (int dz = -2; dz <= 2 && pulled < maxPull; dz++) {
                    scan.set(padCenter.getX() + dx, padCenter.getY() + dy, padCenter.getZ() + dz);
                    EnergyStorage storage = findEnergyStorage(level, scan);
                    if (storage == null || !storage.supportsExtraction()) {
                        continue;
                    }
                    try (Transaction tx = Transaction.openOuter()) {
                        long extracted = storage.extract(maxPull - pulled, tx);
                        if (extracted > 0L) {
                            tx.commit();
                            pulled += extracted;
                        }
                    }
                }
            }
        }

        if (pulled > 0L) {
            vehicle.setStoredEnergyServer(Math.min(cap, cfg.storedEnergy() + pulled));
        }
    }

    private static @Nullable EnergyStorage findEnergyStorage(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        EnergyStorage up = EnergyStorage.SIDED.find(level, pos, Direction.UP);
        if (up != null) {
            return up;
        }
        EnergyStorage unDir = EnergyStorage.SIDED.find(level, pos, null);
        if (unDir != null) {
            return unDir;
        }
        for (Direction dir : Direction.values()) {
            EnergyStorage found = EnergyStorage.SIDED.find(level, pos, dir);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private @Nullable BlockPos resolveNextMissionTarget(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        return switch (cfg.role()) {
            case SCIENCE_EXPLORER -> {
                List<BlockPos> route = this.resolveScienceRoute(vehicle, cfg);
                if (route.isEmpty()) {
                    yield null;
                }
                int idx = Math.floorMod(this.waypointIndex, route.size());
                yield route.get(idx);
            }
            case CARGO_SHUTTLE, PASSENGER_TAXI -> {
                List<BlockPos> wps = cfg.waypoints();
                if (wps.isEmpty()) {
                    yield null;
                }
                int idx = Math.floorMod(this.waypointIndex, wps.size());
                yield wps.get(idx);
            }
            case ESCORT_PATROL -> this.computeEscortTarget(level, vehicle, cfg);
        };
    }

    private @NotNull List<BlockPos> resolveScienceRoute(@NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        if (!cfg.waypoints().isEmpty()) {
            return cfg.waypoints();
        }
        if (this.autoSurveyRoute.isEmpty()) {
            BlockPos anchor = cfg.homeChargerPos().orElse(vehicle.blockPosition());
            int r = Math.max(12, Math.min(cfg.patrolRadius(), 64));
            for (int i = 0; i < 6; i++) {
                double angle = i * (Math.PI / 3.0);
                double dist = r * (0.45 + 0.55 * ((i + 1) / 6.0));
                int wx = anchor.getX() + Mth.floor(Math.cos(angle) * dist);
                int wz = anchor.getZ() + Mth.floor(Math.sin(angle) * dist);
                this.autoSurveyRoute.add(new BlockPos(wx, anchor.getY(), wz));
            }
        }
        return this.autoSurveyRoute;
    }

    private @Nullable BlockPos computeEscortTarget(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        Entity owner = this.findOwnerEntity(level, cfg);
        if (owner == null || !owner.isAlive() || owner.level() != level) {
            return null;
        }
        double offset = vehicle.isAircraft() ? Math.max(8.0, cfg.escortOffsetBlocks()) : cfg.escortOffsetBlocks();
        double ownerYawRad = Math.toRadians(owner.getYRot());
        // Follow trailing behind owner's facing vector
        double tx = owner.getX() + Math.sin(ownerYawRad) * offset;
        double tz = owner.getZ() - Math.cos(ownerYawRad) * offset;
        double ty = vehicle.isAircraft() ? owner.getY() + 10.0 : owner.getY();
        return BlockPos.containing(tx, ty, tz);
    }

    private double distanceToEscortOwner(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull VehicleMissionConfig cfg) {
        Entity owner = this.findOwnerEntity(level, cfg);
        if (owner == null) {
            return Double.MAX_VALUE;
        }
        return Math.hypot(vehicle.getX() - owner.getX(), vehicle.getZ() - owner.getZ());
    }

    private @Nullable Entity findOwnerEntity(@NotNull ServerLevel level, @NotNull VehicleMissionConfig cfg) {
        UUID ownerId = cfg.parsedOwnerUuid();
        if (ownerId == null) {
            return null;
        }
        Player player = level.getPlayerByUUID(ownerId);
        if (player != null) {
            return player;
        }
        return level.getEntity(ownerId);
    }

    private void notifyOwner(@NotNull ServerLevel level, @NotNull ModularVehicleEntity vehicle, @NotNull Component message) {
        Entity owner = this.findOwnerEntity(level, vehicle.getMissionConfig());
        if (owner instanceof Player player) {
            player.displayClientMessage(message, true);
        }
    }

    private static boolean isNearTarget(@NotNull ModularVehicleEntity vehicle, @NotNull BlockPos target, double radius) {
        double dx = (target.getX() + 0.5) - vehicle.getX();
        double dz = (target.getZ() + 0.5) - vehicle.getZ();
        return (dx * dx + dz * dz) <= radius * radius;
    }

    private static boolean isCargoFull(@NotNull SimpleContainer cargo) {
        if (cargo.getContainerSize() <= 0) {
            return false;
        }
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack st = cargo.getItem(i);
            if (st.isEmpty() || st.getCount() < st.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private void insertIntoCargo(@NotNull SimpleContainer cargo, @NotNull ItemStack stack) {
        if (stack.isEmpty() || cargo.getContainerSize() <= 0) {
            return;
        }
        cargo.addItem(stack);
    }

    public void saveAdditional(@NotNull CompoundTag tag) {
        CompoundTag aiTag = new CompoundTag();
        aiTag.putString("AiState", this.state.getSerializedName());
        aiTag.putInt("WaypointIndex", this.waypointIndex);
        aiTag.putInt("CollectedDataCount", this.collectedDataCount);
        aiTag.putBoolean("ReturningToBase", this.returningToBase);
        aiTag.putBoolean("PendingAfterCharge", this.pendingMissionAfterCharge);
        aiTag.putBoolean("HasMovedSinceBoard", this.hasMovedSinceLastBoard);
        tag.put("VehicleAiRuntime", aiTag);
    }

    public void readAdditional(@NotNull CompoundTag tag) {
        if (!tag.contains("VehicleAiRuntime")) {
            return;
        }
        CompoundTag aiTag = tag.getCompound("VehicleAiRuntime");
        String stateName = aiTag.getString("AiState");
        for (VehicleAiState candidate : VehicleAiState.values()) {
            if (candidate.getSerializedName().equals(stateName)) {
                this.state = candidate;
                break;
            }
        }
        this.waypointIndex = Math.max(0, aiTag.getInt("WaypointIndex"));
        this.collectedDataCount = Math.max(0, aiTag.getInt("CollectedDataCount"));
        this.returningToBase = aiTag.getBoolean("ReturningToBase");
        this.pendingMissionAfterCharge = aiTag.getBoolean("PendingAfterCharge");
        this.hasMovedSinceLastBoard = aiTag.getBoolean("HasMovedSinceBoard");
    }
}
