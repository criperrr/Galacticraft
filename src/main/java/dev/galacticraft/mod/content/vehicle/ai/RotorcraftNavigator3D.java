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

import dev.galacticraft.api.vehicle.ai.AutonomousVehicleContext;
import dev.galacticraft.api.vehicle.ai.ControlCommand;
import dev.galacticraft.api.vehicle.ai.NavStatus;
import dev.galacticraft.api.vehicle.ai.VehicleNavigator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * 3D VTOL / Rotorcraft autonomous navigator using a coarse-grid 3D A* planner
 * ({@code 2x2x2} near pads/canyons or {@code 4x4x4} for long-range airspace, budgeted to {@code <= 200}
 * macro-cells/tick) plus 3D Bresenham string pulling and a 4-phase flight profile:
 * {@code TAKEOFF_CLIMB}, {@code CRUISE_3D}, {@code TERMINAL_HOVER_DESCENT} (with pad obstruction
 * {@code HOVER_HOLD} and {@code GO_AROUND}), and {@code TOUCHDOWN}.
 */
public final class RotorcraftNavigator3D implements VehicleNavigator {
    public static final int MAX_MACRO_NODES_PER_TICK = 200;
    public static final int MAX_TOTAL_MACRO_EXPANSIONS = 4000;
    public static final double DEFAULT_TAKEOFF_CLEARANCE = 12.0;
    public static final int MAX_HOVER_HOLD_TICKS = 100;

    public enum FlightPhase {
        IDLE,
        TAKEOFF_CLIMB,
        CRUISE_3D,
        TERMINAL_HOVER_DESCENT,
        GO_AROUND,
        TOUCHDOWN
    }

    private @Nullable BlockPos targetPos;
    private NavStatus status = NavStatus.IDLE;
    private FlightPhase phase = FlightPhase.IDLE;
    private List<Vec3> waypoints = List.of();
    private int currentWaypoint = 0;

    private @Nullable CoarseSearch3D activeSearch;
    private final Map<BlockPos, Long> tempBlacklist = new HashMap<>();

    private double takeoffTargetY;
    private int hoverHoldTicks;
    private int goAroundTicks;
    private Vec3 goAroundPoint = Vec3.ZERO;
    private boolean hoverAtTarget = false;

    public void setHoverAtTarget(boolean hoverAtTarget) {
        this.hoverAtTarget = hoverAtTarget;
    }

    public boolean isHoverAtTarget() {
        return this.hoverAtTarget;
    }

    @Override
    public void setTarget(@NotNull BlockPos targetPos) {
        this.targetPos = targetPos.immutable();
        this.activeSearch = null;
        this.waypoints = List.of();
        this.currentWaypoint = 0;
        this.status = NavStatus.PLANNING;
        this.phase = FlightPhase.IDLE;
        this.hoverHoldTicks = 0;
        this.goAroundTicks = 0;
    }

    @Override
    public @Nullable BlockPos getTarget() {
        return this.targetPos;
    }

    public @NotNull FlightPhase getFlightPhase() {
        return this.phase;
    }

    public int getHoverHoldTicks() {
        return this.hoverHoldTicks;
    }

    @Override
    public void blacklistCell(@NotNull BlockPos pos, long gameTime, long ttlTicks) {
        this.tempBlacklist.put(pos.immutable(), gameTime + Math.max(20L, ttlTicks));
    }

    @Override
    public void reset() {
        this.targetPos = null;
        this.activeSearch = null;
        this.waypoints = List.of();
        this.currentWaypoint = 0;
        this.status = NavStatus.IDLE;
        this.phase = FlightPhase.IDLE;
        this.hoverHoldTicks = 0;
        this.goAroundTicks = 0;
    }

    @Override
    public @NotNull NavStatus getStatus() {
        return this.status;
    }

    @Override
    public @NotNull List<Vec3> getCurrentPath() {
        return this.waypoints;
    }

    @Override
    public void tickPlanning(@NotNull ServerLevel level, @NotNull AutonomousVehicleContext vehicle, int maxNodesPerTick) {
        if (this.targetPos == null) {
            this.status = NavStatus.IDLE;
            return;
        }
        if (this.status != NavStatus.PLANNING) {
            return;
        }

        // Verify planetary thrust capability before wasting CPU planning
        if (!vehicle.derivedStats().canHoverIn(vehicle.gravityG(), vehicle.pressureAtm())) {
            this.status = NavStatus.FAILED;
            return;
        }

        long gameTime = level.getGameTime();
        this.tempBlacklist.entrySet().removeIf(e -> e.getValue() <= gameTime);

        if (this.activeSearch == null) {
            BlockPos start = BlockPos.containing(vehicle.position().x, vehicle.position().y + 0.25, vehicle.position().z);
            double dist = Math.hypot(this.targetPos.getX() - start.getX(), this.targetPos.getZ() - start.getZ());
            int cellSize = dist < 48.0 ? 2 : 4;
            int cruiseBandY = Math.min(level.getMaxBuildHeight() - 8, Math.max(start.getY(), this.targetPos.getY()) + (int) DEFAULT_TAKEOFF_CLEARANCE);
            BlockPos climbStart = new BlockPos(start.getX(), Math.max(start.getY() + 2, cruiseBandY - 2), start.getZ());
            BlockPos approachGoal = new BlockPos(this.targetPos.getX(), Math.max(this.targetPos.getY() + 3, cruiseBandY - 2), this.targetPos.getZ());

            CoarseGrid3D grid = CoarseGrid3D.buildFromLevel(
                    level,
                    climbStart,
                    approachGoal,
                    cellSize,
                    Math.max(1, Mth.ceil(vehicle.entityWidth() * 0.5F)),
                    cruiseBandY,
                    this.tempBlacklist,
                    gameTime
            );
            this.activeSearch = new CoarseSearch3D(grid, climbStart, approachGoal, cruiseBandY);
        }

        int budget = Math.min(MAX_MACRO_NODES_PER_TICK, Math.max(24, maxNodesPerTick));
        CoarseSearch3D.StepResult result = this.activeSearch.step(budget);
        if (result == CoarseSearch3D.StepResult.FOUND) {
            this.waypoints = this.activeSearch.extractPrunedWaypoints();
            this.activeSearch = null;
            this.currentWaypoint = 0;

            double minCruiseY = Math.max(vehicle.position().y, this.targetPos.getY()) + DEFAULT_TAKEOFF_CLEARANCE;
            if (!this.waypoints.isEmpty()) {
                minCruiseY = Math.max(minCruiseY, this.waypoints.get(0).y);
            }
            this.takeoffTargetY = Math.min(level.getMaxBuildHeight() - 6.0, minCruiseY);
            this.phase = (vehicle.position().y < this.takeoffTargetY - 2.0 && vehicle.altitudeAgl() < 6.0)
                    ? FlightPhase.TAKEOFF_CLIMB
                    : FlightPhase.CRUISE_3D;
            this.status = NavStatus.FOLLOWING;
        } else if (result == CoarseSearch3D.StepResult.FAILED) {
            this.activeSearch = null;
            this.status = NavStatus.FAILED;
        }
    }

    @Override
    public @NotNull ControlCommand computeControl(@NotNull ServerLevel level, @NotNull AutonomousVehicleContext vehicle) {
        if (this.targetPos == null || this.status == NavStatus.IDLE || this.status == NavStatus.PLANNING || this.status == NavStatus.FAILED) {
            return ControlCommand.stop();
        }
        if (!vehicle.derivedStats().canHoverIn(vehicle.gravityG(), vehicle.pressureAtm())) {
            this.status = NavStatus.FAILED;
            return ControlCommand.stop();
        }

        Vec3 pos = vehicle.position();
        Vec3 vel = vehicle.velocity();
        Vec3 padCenter = new Vec3(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5);
        double horizDistToPad = Math.hypot(padCenter.x - pos.x, padCenter.z - pos.z);

        switch (this.phase) {
            case TAKEOFF_CLIMB -> {
                this.status = NavStatus.FOLLOWING;
                if (pos.y >= this.takeoffTargetY - 0.8) {
                    this.phase = FlightPhase.CRUISE_3D;
                }
                double altErr = this.takeoffTargetY - pos.y;
                float collective = (float) Mth.clamp(altErr * 0.28 - vel.y * 0.65, 0.25, 0.90);
                boolean gearDown = vehicle.altitudeAgl() <= 4.0;
                return ControlCommand.rotorcraft(0.0F, collective, 0.0F, 0.0F, gearDown, false);
            }
            case CRUISE_3D -> {
                this.status = NavStatus.FOLLOWING;
                if (horizDistToPad < 1.65) {
                    if (this.hoverAtTarget) {
                        // Aerial escort hover-loiter directly above owner offset without landing
                        double holdY = Math.max(padCenter.y, pos.y - 0.5);
                        double desiredVy = Mth.clamp((holdY - pos.y) * 0.25, -0.15, 0.25);
                        float collective = (float) Mth.clamp((desiredVy - vel.y) * 2.5, -0.55, 0.85);
                        return computePadHoldCommand(pos, vel, padCenter, vehicle.yawDeg(), collective);
                    }
                    this.phase = FlightPhase.TERMINAL_HOVER_DESCENT;
                    this.hoverHoldTicks = 0;
                    return ControlCommand.rotorcraft(0.0F, 0.0F, 0.0F, 0.0F, true, false);
                }

                // Advance 3D waypoints
                while (this.currentWaypoint < this.waypoints.size() - 1) {
                    Vec3 wp = this.waypoints.get(this.currentWaypoint);
                    if (Math.hypot(wp.x - pos.x, wp.z - pos.z) < 3.5) {
                        this.currentWaypoint++;
                    } else {
                        break;
                    }
                }
                Vec3 activeWp = this.currentWaypoint < this.waypoints.size()
                        ? this.waypoints.get(this.currentWaypoint)
                        : new Vec3(padCenter.x, this.takeoffTargetY, padCenter.z);

                // Final approach aligns directly over target pad at cruise altitude
                if (this.currentWaypoint >= this.waypoints.size() - 1) {
                    activeWp = new Vec3(padCenter.x, Math.max(activeWp.y, padCenter.y + 4.0), padCenter.z);
                }

                double dx = activeWp.x - pos.x;
                double dz = activeWp.z - pos.z;
                double distXZ = Math.hypot(dx, dz);
                double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
                double yawErr = Mth.wrapDegrees(targetYaw - vehicle.yawDeg());
                float yawRate = (float) Mth.clamp(yawErr * 0.25, -6.5, 6.5);

                // Altitude PID
                double targetY = Math.max(activeWp.y, this.targetPos.getY() + 4.0);
                double desiredVy = Mth.clamp((targetY - pos.y) * 0.22, -0.25, 0.30);
                float collective = (float) Mth.clamp((desiredVy - vel.y) * 2.4, -0.75, 0.90);

                // Forward cyclic tilt coupled to heading alignment and distance braking
                float cyclicPitch = 0.0F;
                if (Math.abs(yawErr) < 55.0) {
                    double desiredSpeed = Mth.clamp(distXZ * 0.14, 0.08, 0.42);
                    double alignFactor = Math.cos(Math.toRadians(yawErr));
                    cyclicPitch = (float) Mth.clamp(desiredSpeed * 42.0 * alignFactor, 0.0, 20.0);
                } else {
                    // Slow down while rotating nose toward waypoint
                    cyclicPitch = 0.0F;
                }
                float cyclicRoll = Mth.clamp(-yawRate * 2.2F, -14.0F, 14.0F);
                boolean gearDown = vehicle.altitudeAgl() <= 4.0;
                return ControlCommand.rotorcraft(yawRate, collective, cyclicPitch, cyclicRoll, gearDown, false);
            }
            case TERMINAL_HOVER_DESCENT -> {
                boolean padObstructed = this.isPadObstructed(level, vehicle, this.targetPos);
                if (padObstructed) {
                    this.hoverHoldTicks++;
                    if (this.hoverHoldTicks >= MAX_HOVER_HOLD_TICKS) {
                        this.phase = FlightPhase.GO_AROUND;
                        this.goAroundTicks = 0;
                        double yawRad = Math.toRadians(vehicle.yawDeg() + 90.0);
                        this.goAroundPoint = new Vec3(
                                padCenter.x - Math.sin(yawRad) * 6.0,
                                Math.max(pos.y, padCenter.y + 8.0),
                                padCenter.z + Math.cos(yawRad) * 6.0
                        );
                        this.status = NavStatus.GO_AROUND;
                        return ControlCommand.rotorcraft(0.0F, 0.70F, 0.0F, 0.0F, true, false);
                    }
                    this.status = NavStatus.HOVER_HOLD;
                    double holdY = Math.max(padCenter.y + 4.5, pos.y);
                    double desiredVy = Mth.clamp((holdY - pos.y) * 0.25, -0.10, 0.28);
                    float collective = (float) Mth.clamp((desiredVy - vel.y) * 2.6, -0.45, 0.85);
                    return computePadHoldCommand(pos, vel, padCenter, vehicle.yawDeg(), collective);
                }

                this.hoverHoldTicks = 0;
                this.status = NavStatus.FOLLOWING;

                // Touchdown check
                double altAbovePad = pos.y - padCenter.y;
                if ((vehicle.isOnGround() || altAbovePad <= 0.28) && horizDistToPad <= 2.2) {
                    this.phase = FlightPhase.TOUCHDOWN;
                    this.status = NavStatus.ARRIVED;
                    return ControlCommand.rotorcraft(0.0F, 0.0F, 0.0F, 0.0F, true, true);
                }

                // Vertical descent PID: -0.15 b/t above 2 blocks, slowing to -0.06 b/t in final 2 blocks
                double targetVy = altAbovePad > 2.0 ? -0.15 : -0.06;
                float collective = (float) Mth.clamp((targetVy - vel.y) * 2.5, -0.75, 0.65);
                return computePadHoldCommand(pos, vel, padCenter, vehicle.yawDeg(), collective);
            }
            case GO_AROUND -> {
                this.status = NavStatus.GO_AROUND;
                this.goAroundTicks++;
                double dx = this.goAroundPoint.x - pos.x;
                double dz = this.goAroundPoint.z - pos.z;
                double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
                double yawErr = Mth.wrapDegrees(targetYaw - vehicle.yawDeg());
                float yawRate = (float) Mth.clamp(yawErr * 0.25, -6.0, 6.0);
                double desiredVy = Mth.clamp((this.goAroundPoint.y - pos.y) * 0.25, -0.15, 0.30);
                float collective = (float) Mth.clamp((desiredVy - vel.y) * 2.5, -0.5, 0.90);
                float pitch = Math.hypot(dx, dz) > 1.5 && Math.abs(yawErr) < 55.0 ? 12.0F : 0.0F;
                if (this.goAroundTicks >= 60) {
                    // Re-attempt approach from go-around altitude
                    this.hoverHoldTicks = 0;
                    this.phase = FlightPhase.CRUISE_3D;
                }
                return ControlCommand.rotorcraft(yawRate, collective, pitch, 0.0F, false, false);
            }
            case TOUCHDOWN -> {
                this.status = NavStatus.ARRIVED;
                return ControlCommand.rotorcraft(0.0F, 0.0F, 0.0F, 0.0F, true, true);
            }
            default -> {
                return ControlCommand.stop();
            }
        }
    }

    private static @NotNull ControlCommand computePadHoldCommand(
            @NotNull Vec3 pos,
            @NotNull Vec3 vel,
            @NotNull Vec3 padCenter,
            float yawDeg,
            float collective
    ) {
        // Project horizontal position error into craft local forward (+Z) and right (-X) axes
        double ex = padCenter.x - pos.x;
        double ez = padCenter.z - pos.z;
        double yawRad = Math.toRadians(yawDeg);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);

        double localFwdErr = ex * fx + ez * fz;
        double localRightErr = ex * rx + ez * rz;
        double localFwdVel = vel.x * fx + vel.z * fz;
        double localRightVel = vel.x * rx + vel.z * rz;

        float pitch = (float) Mth.clamp(localFwdErr * 6.5 - localFwdVel * 24.0, -10.0, 10.0);
        float roll = (float) Mth.clamp(localRightErr * 6.5 - localRightVel * 24.0, -10.0, 10.0);
        return ControlCommand.rotorcraft(0.0F, collective, pitch, roll, true, false);
    }

    /**
     * Scans the destination pad footprint for solid block obstructions or blocking entities
     * (other vehicles, mobs, armor stands, or players standing on the pad).
     */
    public boolean isPadObstructed(
            @NotNull ServerLevel level,
            @NotNull AutonomousVehicleContext vehicle,
            @NotNull BlockPos padPos
    ) {
        // 1. Check solid blocks directly occupying the landing envelope above padPos
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 2; dy++) {
            cursor.set(padPos.getX(), padPos.getY() + dy, padPos.getZ());
            BlockState st = level.getBlockState(cursor);
            if (!st.getFluidState().isEmpty() || !st.getCollisionShape(level, cursor).isEmpty()) {
                return true;
            }
        }

        // 2. Check blocking entities resting on the landing pad
        Entity self = vehicle.asEntity();
        double halfSpan = Math.max(1.2, vehicle.entityWidth() * 0.65);
        AABB padBox = new AABB(
                padPos.getX() + 0.5 - halfSpan, padPos.getY(), padPos.getZ() + 0.5 - halfSpan,
                padPos.getX() + 0.5 + halfSpan, padPos.getY() + Math.max(2.5, vehicle.entityHeight() + 0.8), padPos.getZ() + 0.5 + halfSpan
        );
        List<Entity> obstacles = level.getEntities(self, padBox, e ->
                !e.isRemoved()
                        && e.isPickable()
                        && !(e instanceof ItemEntity)
                        && (self == null || (!self.hasPassenger(e) && !e.is(self)))
                        && !(e instanceof dev.galacticraft.mod.content.entity.vehicle.VehiclePartEntity)
        );
        return !obstacles.isEmpty();
    }

    /**
     * Pure static coarse-grid 3D solver helper for deterministic unit tests.
     */
    public static @NotNull List<Vec3> planCoarse3D(
            @NotNull CoarseGrid3D grid,
            @NotNull BlockPos start,
            @NotNull BlockPos goal,
            int preferredCruiseY,
            int maxNodesPerTick,
            int maxTicks
    ) {
        CoarseSearch3D search = new CoarseSearch3D(grid, start, goal, preferredCruiseY);
        for (int t = 0; t < maxTicks; t++) {
            CoarseSearch3D.StepResult res = search.step(maxNodesPerTick);
            if (res == CoarseSearch3D.StepResult.FOUND) {
                return search.extractPrunedWaypoints();
            }
            if (res == CoarseSearch3D.StepResult.FAILED) {
                return List.of();
            }
        }
        return List.of();
    }

    /**
     * Compact 3D coarse occupancy grid ({@code cellSize} = 2 or 4 blocks) with 3D Bresenham LOS.
     */
    public static final class CoarseGrid3D {
        private final int originX;
        private final int originY;
        private final int originZ;
        private final int cellSize;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final boolean[] free;

        private CoarseGrid3D(int originX, int originY, int originZ, int cellSize, int sizeX, int sizeY, int sizeZ, boolean[] free) {
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.cellSize = cellSize;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.free = free;
        }

        public static @NotNull CoarseGrid3D fromSynthetic(
                int originX,
                int originY,
                int originZ,
                int cellSize,
                int sizeX,
                int sizeY,
                int sizeZ,
                boolean[] free
        ) {
            return new CoarseGrid3D(originX, originY, originZ, cellSize, sizeX, sizeY, sizeZ, Arrays.copyOf(free, free.length));
        }

        public static @NotNull CoarseGrid3D buildFromLevel(
                @NotNull ServerLevel level,
                @NotNull BlockPos start,
                @NotNull BlockPos goal,
                int cellSize,
                int safetyMarginBlocks,
                int cruiseBandY,
                @NotNull Map<BlockPos, Long> tempBlacklist,
                long gameTime
        ) {
            int padXZ = Math.max(8, cellSize * 3);
            int minWx = Math.min(start.getX(), goal.getX()) - padXZ;
            int maxWx = Math.max(start.getX(), goal.getX()) + padXZ;
            int minWz = Math.min(start.getZ(), goal.getZ()) - padXZ;
            int maxWz = Math.max(start.getZ(), goal.getZ()) + padXZ;
            int minWy = Math.max(level.getMinBuildHeight() + 2, Math.min(start.getY(), goal.getY()) - cellSize * 2);
            int maxWy = Math.min(level.getMaxBuildHeight() - 4, Math.max(cruiseBandY + 12, Math.max(start.getY(), goal.getY()) + 16));

            int sizeX = Math.max(1, (maxWx - minWx) / cellSize + 1);
            int sizeY = Math.max(1, (maxWy - minWy) / cellSize + 1);
            int sizeZ = Math.max(1, (maxWz - minWz) / cellSize + 1);

            // Cap grid dimensions to avoid excessive memory on extreme distances
            sizeX = Math.min(sizeX, 48);
            sizeY = Math.min(sizeY, 24);
            sizeZ = Math.min(sizeZ, 48);

            boolean[] free = new boolean[sizeX * sizeY * sizeZ];
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            int margin = Math.min(1, safetyMarginBlocks);

            for (int gx = 0; gx < sizeX; gx++) {
                int baseX = minWx + gx * cellSize;
                for (int gy = 0; gy < sizeY; gy++) {
                    int baseY = minWy + gy * cellSize;
                    for (int gz = 0; gz < sizeZ; gz++) {
                        int baseZ = minWz + gz * cellSize;
                        boolean cellClear = true;
                        for (int ox = -margin; ox < cellSize + margin && cellClear; ox++) {
                            for (int oy = 0; oy < cellSize + margin && cellClear; oy++) {
                                for (int oz = -margin; oz < cellSize + margin; oz++) {
                                    cursor.set(baseX + ox, baseY + oy, baseZ + oz);
                                    if (!level.isLoaded(cursor)) {
                                        continue;
                                    }
                                    BlockState st = level.getBlockState(cursor);
                                    if (!st.getFluidState().isEmpty() || !st.getCollisionShape(level, cursor).isEmpty()) {
                                        cellClear = false;
                                        break;
                                    }
                                }
                            }
                        }
                        free[(gx * sizeY + gy) * sizeZ + gz] = cellClear;
                    }
                }
            }

            for (Map.Entry<BlockPos, Long> entry : tempBlacklist.entrySet()) {
                if (entry.getValue() <= gameTime) {
                    continue;
                }
                BlockPos bp = entry.getKey();
                int gx = (bp.getX() - minWx) / cellSize;
                int gy = (bp.getY() - minWy) / cellSize;
                int gz = (bp.getZ() - minWz) / cellSize;
                if (gx >= 0 && gx < sizeX && gy >= 0 && gy < sizeY && gz >= 0 && gz < sizeZ) {
                    free[(gx * sizeY + gy) * sizeZ + gz] = false;
                }
            }

            return new CoarseGrid3D(minWx, minWy, minWz, cellSize, sizeX, sizeY, sizeZ, free);
        }

        public int toCellX(int wx) {
            return Mth.clamp((wx - this.originX) / this.cellSize, 0, this.sizeX - 1);
        }

        public int toCellY(int wy) {
            return Mth.clamp((wy - this.originY) / this.cellSize, 0, this.sizeY - 1);
        }

        public int toCellZ(int wz) {
            return Mth.clamp((wz - this.originZ) / this.cellSize, 0, this.sizeZ - 1);
        }

        public Vec3 cellCenter(int gx, int gy, int gz) {
            double half = this.cellSize * 0.5;
            return new Vec3(this.originX + gx * this.cellSize + half, this.originY + gy * this.cellSize + half, this.originZ + gz * this.cellSize + half);
        }

        public int index(int gx, int gy, int gz) {
            return (gx * this.sizeY + gy) * this.sizeZ + gz;
        }

        public boolean inBounds(int gx, int gy, int gz) {
            return gx >= 0 && gx < this.sizeX && gy >= 0 && gy < this.sizeY && gz >= 0 && gz < this.sizeZ;
        }

        public boolean isFree(int gx, int gy, int gz) {
            return inBounds(gx, gy, gz) && this.free[index(gx, gy, gz)];
        }

        public boolean hasLineOfSight3D(int x0, int y0, int z0, int x1, int y1, int z1) {
            int dx = Math.abs(x1 - x0);
            int dy = Math.abs(y1 - y0);
            int dz = Math.abs(z1 - z0);
            int steps = Math.max(dx, Math.max(dy, dz));
            if (steps == 0) {
                return isFree(x0, y0, z0);
            }
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                int cx = (int) Math.round(x0 + (x1 - x0) * t);
                int cy = (int) Math.round(y0 + (y1 - y0) * t);
                int cz = (int) Math.round(z0 + (z1 - z0) * t);
                if (!isFree(cx, cy, cz)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class CoarseSearch3D {
        enum StepResult {
            IN_PROGRESS,
            FOUND,
            FAILED
        }

        private static final int[][] DIRS_6 = {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
                {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}
        };

        private final CoarseGrid3D grid;
        private final int startIdx;
        private final int goalIdx;
        private final int gx;
        private final int gy;
        private final int gz;
        private final int preferredCellY;
        private final double[] gScore;
        private final int[] parent;
        private final byte[] state;
        private final PriorityQueue<Node> open;
        private int expanded = 0;
        private boolean finished = false;
        private boolean found = false;

        CoarseSearch3D(@NotNull CoarseGrid3D grid, @NotNull BlockPos start, @NotNull BlockPos goal, int preferredCruiseY) {
            this.grid = grid;
            int sx = grid.toCellX(start.getX());
            int sy = findNearestFreeY(grid, sx, grid.toCellY(start.getY()), grid.toCellZ(start.getZ()));
            int sz = grid.toCellZ(start.getZ());

            this.gx = grid.toCellX(goal.getX());
            this.gz = grid.toCellZ(goal.getZ());
            this.gy = findNearestFreeY(grid, this.gx, grid.toCellY(goal.getY()), this.gz);
            this.preferredCellY = grid.toCellY(preferredCruiseY);

            this.startIdx = grid.index(sx, sy, sz);
            this.goalIdx = grid.index(this.gx, this.gy, this.gz);

            int total = grid.sizeX * grid.sizeY * grid.sizeZ;
            this.gScore = new double[total];
            this.parent = new int[total];
            this.state = new byte[total];
            Arrays.fill(this.gScore, Double.POSITIVE_INFINITY);
            Arrays.fill(this.parent, -1);

            this.open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
            this.gScore[this.startIdx] = 0.0;
            this.parent[this.startIdx] = this.startIdx;
            this.state[this.startIdx] = 1;
            this.open.add(new Node(this.startIdx, heuristic(sx, sy, sz)));
        }

        private static int findNearestFreeY(CoarseGrid3D grid, int cx, int cy, int cz) {
            if (grid.isFree(cx, cy, cz)) {
                return cy;
            }
            for (int dy = 1; dy < grid.sizeY; dy++) {
                if (cy + dy < grid.sizeY && grid.isFree(cx, cy + dy, cz)) {
                    return cy + dy;
                }
                if (cy - dy >= 0 && grid.isFree(cx, cy - dy, cz)) {
                    return cy - dy;
                }
            }
            return cy;
        }

        StepResult step(int maxNodes) {
            if (this.finished) {
                return this.found ? StepResult.FOUND : StepResult.FAILED;
            }
            int processed = 0;
            int syz = this.grid.sizeY * this.grid.sizeZ;
            int szLen = this.grid.sizeZ;

            while (!this.open.isEmpty() && processed < maxNodes) {
                Node cur = this.open.poll();
                int idx = cur.idx();
                if (this.state[idx] == 2) {
                    continue;
                }
                if (idx == this.goalIdx) {
                    this.finished = true;
                    this.found = true;
                    return StepResult.FOUND;
                }
                this.state[idx] = 2;
                processed++;
                this.expanded++;
                if (this.expanded > MAX_TOTAL_MACRO_EXPANSIONS) {
                    this.finished = true;
                    this.found = false;
                    return StepResult.FAILED;
                }

                int cx = idx / syz;
                int rem = idx % syz;
                int cy = rem / szLen;
                int cz = rem % szLen;

                for (int[] d : DIRS_6) {
                    int nx = cx + d[0];
                    int ny = cy + d[1];
                    int nz = cz + d[2];
                    if (!this.grid.isFree(nx, ny, nz)) {
                        continue;
                    }
                    int nIdx = this.grid.index(nx, ny, nz);
                    if (this.state[nIdx] == 2) {
                        continue;
                    }
                    double moveCost = (d[0] != 0 && d[2] != 0) ? 1.414 : 1.0;
                    double altBandPenalty = Math.abs(ny - this.preferredCellY) * 0.18;
                    double tentativeG = this.gScore[idx] + moveCost + altBandPenalty;
                    if (tentativeG < this.gScore[nIdx]) {
                        this.gScore[nIdx] = tentativeG;
                        this.parent[nIdx] = idx;
                        this.state[nIdx] = 1;
                        this.open.add(new Node(nIdx, tentativeG + heuristic(nx, ny, nz)));
                    }
                }
            }

            if (this.open.isEmpty()) {
                this.finished = true;
                this.found = false;
                return StepResult.FAILED;
            }
            return StepResult.IN_PROGRESS;
        }

        private double heuristic(int x, int y, int z) {
            return Math.sqrt((x - this.gx) * (x - this.gx) + (y - this.gy) * (y - this.gy) + (z - this.gz) * (z - this.gz));
        }

        @NotNull List<Vec3> extractPrunedWaypoints() {
            if (!this.found) {
                return List.of();
            }
            int syz = this.grid.sizeY * this.grid.sizeZ;
            int szLen = this.grid.sizeZ;
            List<int[]> raw = new ArrayList<>();
            int cur = this.goalIdx;
            int guard = 0;
            while (cur >= 0 && guard++ < MAX_TOTAL_MACRO_EXPANSIONS) {
                int cx = cur / syz;
                int rem = cur % syz;
                raw.add(new int[]{cx, rem / szLen, rem % szLen});
                if (cur == this.startIdx) {
                    break;
                }
                int next = this.parent[cur];
                if (next == cur) {
                    break;
                }
                cur = next;
            }
            Collections.reverse(raw);

            // 3D Bresenham string pulling
            List<int[]> pruned = new ArrayList<>();
            pruned.add(raw.get(0));
            int i = 0;
            while (i < raw.size() - 1) {
                int bestJ = i + 1;
                int[] a = raw.get(i);
                for (int j = raw.size() - 1; j > i + 1; j--) {
                    int[] b = raw.get(j);
                    if (this.grid.hasLineOfSight3D(a[0], a[1], a[2], b[0], b[1], b[2])) {
                        bestJ = j;
                        break;
                    }
                }
                pruned.add(raw.get(bestJ));
                i = bestJ;
            }

            List<Vec3> out = new ArrayList<>(pruned.size());
            for (int[] c : pruned) {
                out.add(this.grid.cellCenter(c[0], c[1], c[2]));
            }
            return Collections.unmodifiableList(out);
        }

        private record Node(int idx, double f) {
        }
    }
}
