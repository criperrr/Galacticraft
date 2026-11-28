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
import dev.galacticraft.mod.content.entity.vehicle.GCVehicle;
import dev.galacticraft.mod.content.entity.vehicle.VehiclePartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
 * Ground autonomous navigator implementing a JPL ENav-inspired inflated 2.5D costmap,
 * budgeted A* / Theta* search ({@code <= 250} nodes/tick), greedy 2.5D Bresenham string pulling,
 * Pure Pursuit path tracking (Ackermann and 6x6 skid-steer point-turn), gravity-aware traction,
 * forward 3-ray hazard fan, and progress-based stuck detection.
 */
public final class GroundNavigator25D implements VehicleNavigator {
    public static final int DEFAULT_MAX_NODES_PER_TICK = 250;
    public static final int MAX_TOTAL_EXPANSIONS = 6000;
    public static final double ARRIVAL_TOLERANCE = 0.85;
    public static final double STUCK_CHECK_INTERVAL_TICKS = 20;
    public static final double STUCK_MIN_PROGRESS_BLOCKS = 0.15;
    public static final double HAZARD_RAY_LENGTH = 3.5;

    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DZ = {-1, -1, -1, 0, 0, 1, 1, 1};

    private @Nullable BlockPos targetPos;
    private NavStatus status = NavStatus.IDLE;
    private List<Vec3> prunedPath = List.of();
    private int currentSegment = 0;

    private @Nullable ActiveSearch activeSearch;
    private final Map<BlockPos, Long> tempBlacklist = new HashMap<>();

    // Exponential backoff on repeated replan failures (20..100 ticks)
    private int replanBackoffTicks = 20;
    private int replanCooldownTicks = 0;

    // Stuck progress tracker
    private @Nullable Vec3 lastProgressPos;
    private int ticksSinceProgressCheck = 0;
    private int hazardCheckTick = 0;
    private double cachedCenterClearance = HAZARD_RAY_LENGTH;
    private double cachedLeftClearance = HAZARD_RAY_LENGTH;
    private double cachedRightClearance = HAZARD_RAY_LENGTH;
    private boolean cachedEntityAhead = false;
    private boolean cachedVehicleAhead = false;
    private double cachedVehicleDist = Double.MAX_VALUE;
    private int vehicleBlockedTicks = 0;
    private int replanVehicleCooldownTicks = 0;
    private boolean cachedCliffAhead = false;
    private @Nullable BlockPos lastBlockingCell;

    @Override
    public void setTarget(@NotNull BlockPos targetPos) {
        this.targetPos = targetPos.immutable();
        this.activeSearch = null;
        this.prunedPath = List.of();
        this.currentSegment = 0;
        this.status = NavStatus.PLANNING;
        this.lastProgressPos = null;
        this.ticksSinceProgressCheck = 0;
        this.replanCooldownTicks = 0;
    }

    @Override
    public @Nullable BlockPos getTarget() {
        return this.targetPos;
    }

    @Override
    public void blacklistCell(@NotNull BlockPos pos, long gameTime, long ttlTicks) {
        this.tempBlacklist.put(pos.immutable(), gameTime + Math.max(20L, ttlTicks));
    }

    public @Nullable BlockPos getLastBlockingCell() {
        return this.lastBlockingCell;
    }

    @Override
    public void reset() {
        this.targetPos = null;
        this.activeSearch = null;
        this.prunedPath = List.of();
        this.currentSegment = 0;
        this.status = NavStatus.IDLE;
        this.lastProgressPos = null;
        this.ticksSinceProgressCheck = 0;
        this.replanBackoffTicks = 20;
        this.replanCooldownTicks = 0;
        this.lastBlockingCell = null;
    }

    @Override
    public @NotNull NavStatus getStatus() {
        return this.status;
    }

    @Override
    public @NotNull List<Vec3> getCurrentPath() {
        return this.prunedPath;
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
        if (this.replanCooldownTicks > 0) {
            this.replanCooldownTicks--;
            return;
        }

        // Purge expired blacklist entries
        long gameTime = level.getGameTime();
        this.tempBlacklist.entrySet().removeIf(e -> e.getValue() <= gameTime);

        if (this.activeSearch == null) {
            BlockPos startBlock = BlockPos.containing(vehicle.position().x, vehicle.position().y + 0.1, vehicle.position().z);
            Costmap25D costmap = Costmap25D.buildFromLevel(
                    level,
                    vehicle.asEntity(),
                    startBlock,
                    this.targetPos,
                    vehicle.entityWidth(),
                    vehicle.entityHeight(),
                    this.tempBlacklist,
                    gameTime
            );
            this.activeSearch = startBudgetedSearch(costmap, startBlock, this.targetPos);
            if (this.activeSearch == null) {
                this.markPlanFailed();
                return;
            }
        }

        int budget = Math.max(16, maxNodesPerTick > 0 ? maxNodesPerTick : DEFAULT_MAX_NODES_PER_TICK);
        SearchStepResult step = this.activeSearch.step(budget);
        if (step == SearchStepResult.FOUND) {
            this.prunedPath = this.activeSearch.extractPrunedWaypoints();
            this.activeSearch = null;
            this.currentSegment = 0;
            this.replanBackoffTicks = 20;
            this.lastProgressPos = vehicle.position();
            this.ticksSinceProgressCheck = 0;
            this.status = this.prunedPath.isEmpty() ? NavStatus.ARRIVED : NavStatus.FOLLOWING;
        } else if (step == SearchStepResult.FAILED) {
            this.activeSearch = null;
            this.markPlanFailed();
        }
    }

    private void markPlanFailed() {
        this.status = NavStatus.FAILED;
        this.replanCooldownTicks = this.replanBackoffTicks;
        this.replanBackoffTicks = Math.min(100, this.replanBackoffTicks * 2);
    }

    /**
     * Pure static solver usable in unit tests on synthetic {@link Costmap25D} instances
     * to validate budgeted node expansion, footprint avoidance, and string pulling deterministically.
     */
    public static @NotNull List<Vec3> planOnCostmap(
            @NotNull Costmap25D costmap,
            @NotNull BlockPos start,
            @NotNull BlockPos goal,
            int maxNodesPerTick,
            int maxTicks
    ) {
        ActiveSearch search = startBudgetedSearch(costmap, start, goal);
        if (search == null) {
            return List.of();
        }
        for (int t = 0; t < maxTicks; t++) {
            SearchStepResult res = search.step(maxNodesPerTick);
            if (res == SearchStepResult.FOUND) {
                return search.extractPrunedWaypoints();
            }
            if (res == SearchStepResult.FAILED) {
                return List.of();
            }
        }
        return List.of();
    }

    private static @Nullable ActiveSearch startBudgetedSearch(@NotNull Costmap25D costmap, @NotNull BlockPos start, @NotNull BlockPos goal) {
        int sx = costmap.toGridX(start.getX());
        int sz = costmap.toGridZ(start.getZ());
        int gx = costmap.toGridX(goal.getX());
        int gz = costmap.toGridZ(goal.getZ());
        if (!costmap.inBounds(sx, sz) || !costmap.inBounds(gx, gz)) {
            return null;
        }

        // If goal sits right next to a charger/structure wall, snap to closest inflated-passable cell within 1 block
        if (!costmap.isInflatedPassable(gx, gz)) {
            int bestGx = -1;
            int bestGz = -1;
            double bestDist = Double.MAX_VALUE;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int cx = gx + dx;
                    int cz = gz + dz;
                    if (costmap.isInflatedPassable(cx, cz)) {
                        double d = dx * dx + dz * dz;
                        if (d < bestDist) {
                            bestDist = d;
                            bestGx = cx;
                            bestGz = cz;
                        }
                    }
                }
            }
            if (bestGx < 0) {
                return null;
            }
            gx = bestGx;
            gz = bestGz;
        }

        if (!costmap.isRawPassable(sx, sz)) {
            return null;
        }
        return new ActiveSearch(costmap, sx, sz, gx, gz);
    }

    @Override
    public @NotNull ControlCommand computeControl(@NotNull ServerLevel level, @NotNull AutonomousVehicleContext vehicle) {
        if (this.targetPos == null || this.status != NavStatus.FOLLOWING) {
            return ControlCommand.stop();
        }

        Vec3 pos = vehicle.position();
        Vec3 goalCenter = new Vec3(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5);
        double distToGoal = horizontalDist(pos, goalCenter);
        if (distToGoal <= ARRIVAL_TOLERANCE) {
            this.status = NavStatus.ARRIVED;
            return ControlCommand.stop();
        }

        // 1. Advance current polyline segment monotonically
        if (this.prunedPath.isEmpty()) {
            this.status = NavStatus.PLANNING;
            return ControlCommand.stop();
        }
        while (this.currentSegment < this.prunedPath.size() - 1) {
            Vec3 nextWp = this.prunedPath.get(this.currentSegment + 1);
            if (horizontalDist(pos, nextWp) < 1.35) {
                this.currentSegment++;
            } else {
                break;
            }
        }

        // 2. Dynamic Pure Pursuit lookahead (2.50..5.00 blocks, tightened around corners and final approach)
        double currentSpeed = Math.abs(vehicle.forwardSpeed());
        double lookahead = Mth.clamp(2.50 + 4.00 * currentSpeed, 2.50, 5.00);
        if (this.currentSegment + 1 < this.prunedPath.size() - 1) {
            lookahead = Math.min(lookahead, 2.0);
        }
        if (distToGoal < lookahead) {
            lookahead = Math.max(1.0, distToGoal);
        }

        Vec3 lookaheadTarget = findLookaheadPoint(pos, this.prunedPath, this.currentSegment, lookahead);

        // 3. Heading error alpha in Minecraft yaw convention: forward = (-sin(yaw), cos(yaw))
        double dx = lookaheadTarget.x - pos.x;
        double dz = lookaheadTarget.z - pos.z;
        double targetYawDeg = Math.toDegrees(Math.atan2(-dx, dz));
        double alphaDeg = Mth.wrapDegrees(targetYawDeg - vehicle.yawDeg());
        double alphaRad = Math.toRadians(alphaDeg);

        // 4. Forward 3-ray hazard fan + downward cliff/crater edge probe every 2 ticks
        if (this.hazardCheckTick++ % 2 == 0) {
            this.sampleHazardFan(level, vehicle);
        }

        // 5. Gravity-aware traction scaling
        double gravity = Math.max(0.10, vehicle.gravityG());
        double gripFactor = Mth.clamp(Math.sqrt(gravity), 0.35, 1.25);
        double maxSpeed = Math.max(0.22, vehicle.derivedStats().maxSpeedBlocksPerTick() * 0.85);
        double effectiveLd = Math.max(1.25, horizontalDist(pos, lookaheadTarget));
        double curvature = (2.0 * Math.sin(alphaRad)) / effectiveLd;

        // 6. Stuck detector (checked every 20 ticks while trying to make forward progress)
        if (this.lastProgressPos == null) {
            this.lastProgressPos = pos;
            this.ticksSinceProgressCheck = 0;
        } else if (!this.cachedEntityAhead) {
            this.ticksSinceProgressCheck++;
            if (this.ticksSinceProgressCheck >= STUCK_CHECK_INTERVAL_TICKS) {
                double moved = horizontalDist(pos, this.lastProgressPos);
                this.lastProgressPos = pos;
                this.ticksSinceProgressCheck = 0;
                if (moved < STUCK_MIN_PROGRESS_BLOCKS) {
                    this.status = NavStatus.STUCK;
                    return ControlCommand.stop();
                }
            }
        }

        // 7. Dynamic vehicle vs pedestrian avoidance ahead
        if (this.replanVehicleCooldownTicks > 0) {
            this.replanVehicleCooldownTicks--;
        }
        if (this.cachedVehicleAhead) {
            this.vehicleBlockedTicks++;
            if (this.cachedVehicleDist < Math.max(2.3, vehicle.entityWidth() + 0.7)) {
                // Too close to swing around directly -> reverse and replan around the other vehicle
                this.vehicleBlockedTicks = 0;
                this.status = NavStatus.STUCK;
                return ControlCommand.stop();
            }
            if (this.vehicleBlockedTicks >= 4 && this.replanVehicleCooldownTicks == 0) {
                // Dynamically replan costmap with the other vehicle's live footprint stamped as IMPASSABLE
                this.vehicleBlockedTicks = 0;
                this.replanVehicleCooldownTicks = 25;
                this.activeSearch = null;
                this.status = NavStatus.PLANNING;
                return ControlCommand.stop();
            }
            return ControlCommand.ground(0.0, 0.0F, 0.0F, true, false);
        }
        this.vehicleBlockedTicks = 0;
        if (this.cachedEntityAhead) {
            return ControlCommand.ground(0.0, 0.0F, 0.0F, true, false);
        }

        // 8. All 3 forward hazard rays blocked at close range -> trigger STUCK_RECOVERY
        if (this.cachedCenterClearance < 1.65 && this.cachedLeftClearance < 1.85 && this.cachedRightClearance < 1.85) {
            this.status = NavStatus.STUCK;
            return ControlCommand.stop();
        }

        // 9. Differential / in-place pivot on large heading errors OR cliff/crater edge directly ahead
        // Prevents wide Ackermann arcs from swinging into craters or walls on tight corners
        boolean skidSteer = vehicle.derivedStats().skidSteerEnabled();
        double pivotThreshold = skidSteer ? 55.0 : 50.0;
        if (Math.abs(alphaDeg) > pivotThreshold || (this.cachedCliffAhead && Math.abs(alphaDeg) > 12.0)) {
            float yawRate = (float) Math.copySign(Math.clamp(Math.abs(alphaDeg) * 0.35, 2.5, 6.0), alphaDeg);
            this.ticksSinceProgressCheck = Math.max(0, this.ticksSinceProgressCheck - 1);
            return ControlCommand.ground(0.0, (float) Math.copySign(35.0, alphaDeg), yawRate, false, true);
        }
        if (this.cachedCliffAhead) {
            // Drop-off directly ahead even when aligned -> trigger STUCK_RECOVERY to back off & replan safely
            this.status = NavStatus.STUCK;
            return ControlCommand.stop();
        }

        double speedScale = Mth.clamp(1.0 - 0.65 * Math.abs(curvature), 0.25, 1.0);
        double targetSpeed = maxSpeed * speedScale * Mth.clamp(0.70 + 0.30 * gripFactor, 0.55, 1.0);
        if (distToGoal < 3.0) {
            double approachScale = Mth.clamp(distToGoal / 3.0, 0.22, 1.0) * Mth.clamp(gripFactor, 0.55, 1.0);
            targetSpeed = Math.min(targetSpeed, Math.max(0.08, maxSpeed * approachScale));
        }

        double wheelbase = Math.max(1.2, vehicle.wheelbaseBlocks());
        float steeringDeg = (float) Mth.clamp(Math.toDegrees(Math.atan(wheelbase * curvature)), -35.0, 35.0);
        float yawRate = (float) Mth.clamp(Math.toDegrees(targetSpeed * curvature), -5.0, 5.0);

        // Reactive center-ray hazard deflection
        if (this.cachedCenterClearance < HAZARD_RAY_LENGTH - 0.15) {
            targetSpeed *= 0.50;
            float avoidBias = this.cachedLeftClearance >= this.cachedRightClearance ? -22.0F : 22.0F;
            steeringDeg = Mth.clamp(steeringDeg + avoidBias, -35.0F, 35.0F);
            yawRate = Mth.clamp(yawRate + (avoidBias * 0.12F), -5.0F, 5.0F);
        }

        return ControlCommand.ground(targetSpeed, steeringDeg, yawRate, false, false);
    }

    private void sampleHazardFan(@NotNull ServerLevel level, @NotNull AutonomousVehicleContext vehicle) {
        Vec3 origin = vehicle.position().add(0.0, 0.55, 0.0);
        float baseYaw = vehicle.yawDeg();
        int floorY = Mth.floor(vehicle.position().y);

        this.cachedCenterClearance = castHazardRay(level, vehicle, origin, baseYaw, floorY, true);
        this.cachedLeftClearance = castHazardRay(level, vehicle, origin, baseYaw - 25.0F, floorY, false);
        this.cachedRightClearance = castHazardRay(level, vehicle, origin, baseYaw + 25.0F, floorY, false);

        // Downward cliff/crater drop-off probe ahead of the front bumper
        this.cachedCliffAhead = hasCraterDropAhead(level, vehicle.position(), baseYaw, floorY, vehicle.entityWidth());

        // Dynamic entity & other-vehicle check ahead (excluding self, own passengers, own VehiclePartEntity children, and dropped items)
        this.cachedEntityAhead = false;
        this.cachedVehicleAhead = false;
        this.cachedVehicleDist = Double.MAX_VALUE;
        Entity self = vehicle.asEntity();
        if (self != null) {
            double yawRad = Math.toRadians(baseYaw);
            double fx = -Math.sin(yawRad);
            double fz = Math.cos(yawRad);
            double probeOffset = Math.max(2.4, vehicle.entityWidth() * 0.8 + 1.4);
            Vec3 aheadCenter = vehicle.position().add(fx * probeOffset, 0.75, fz * probeOffset);
            double halfW = Math.max(1.1, vehicle.entityWidth() * 0.65);
            AABB aheadBox = new AABB(
                    aheadCenter.x - halfW, vehicle.position().y - 0.1, aheadCenter.z - halfW,
                    aheadCenter.x + halfW, vehicle.position().y + vehicle.entityHeight() + 0.4, aheadCenter.z + halfW
            );
            List<Entity> blocking = level.getEntities(self, aheadBox, e ->
                    !e.isRemoved()
                            && e.isPickable()
                            && !(e instanceof ItemEntity)
                            && !self.hasPassenger(e)
                            && !e.is(self)
            );
            for (Entity blocker : blocking) {
                if (blocker instanceof GCVehicle || blocker instanceof VehiclePartEntity) {
                    this.cachedVehicleAhead = true;
                    double d = horizontalDist(vehicle.position(), blocker.position());
                    if (d < this.cachedVehicleDist) {
                        this.cachedVehicleDist = d;
                        this.lastBlockingCell = blocker.blockPosition().immutable();
                    }
                } else {
                    this.cachedEntityAhead = true;
                }
            }
        }
    }

    private double castHazardRay(
            @NotNull ServerLevel level,
            @NotNull AutonomousVehicleContext vehicle,
            @NotNull Vec3 origin,
            float yawDeg,
            int vehicleFloorY,
            boolean recordBlockingCell
    ) {
        double yawRad = Math.toRadians(yawDeg);
        Vec3 end = origin.add(-Math.sin(yawRad) * HAZARD_RAY_LENGTH, 0.0, Math.cos(yawRad) * HAZARD_RAY_LENGTH);
        Entity self = vehicle.asEntity();
        BlockHitResult hit = level.clip(new ClipContext(
                origin,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                self != null ? net.minecraft.world.phys.shapes.CollisionContext.of(self) : net.minecraft.world.phys.shapes.CollisionContext.empty()
        ));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return HAZARD_RAY_LENGTH;
        }
        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        // Do NOT treat a climbable 1-block step as a blocking wall!
        if (hitPos.getY() <= vehicleFloorY && hitState.getFluidState().isEmpty() && !Costmap25D.isHazardousBlock(hitState)) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            int headroom = Math.max(2, Mth.ceil(vehicle.entityHeight()));
            if (Costmap25D.hasClearHeadroom(level, cursor, hitPos.getX(), hitPos.getY() + 1, hitPos.getZ(), headroom)) {
                return HAZARD_RAY_LENGTH;
            }
        }
        if (recordBlockingCell) {
            this.lastBlockingCell = hitPos.immutable();
        }
        return hit.getLocation().distanceTo(origin);
    }

    private static boolean hasCraterDropAhead(
            @NotNull ServerLevel level,
            @NotNull Vec3 pos,
            float yawDeg,
            int floorY,
            float entityWidth
    ) {
        double yawRad = Math.toRadians(yawDeg);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);
        double probeDist = Math.max(1.35, entityWidth * 0.75 + 0.65);
        double sideHalf = entityWidth * 0.35;

        double[] lateralOffsets = {0.0, -sideHalf, sideHalf};
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (double lat : lateralOffsets) {
            int px = Mth.floor(pos.x + fx * probeDist + rx * lat);
            int pz = Mth.floor(pos.z + fz * probeDist + rz * lat);
            boolean hasSafeGround = false;
            // Standable floor must exist within [floorY - 2, floorY] (at most a 1-block drop from floorY - 1)
            for (int y = floorY; y >= floorY - 2; y--) {
                cursor.set(px, y, pz);
                if (Costmap25D.isStandableFloor(level, cursor, level.getBlockState(cursor))) {
                    hasSafeGround = true;
                    break;
                }
            }
            if (!hasSafeGround) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes gravity-aware Pure Pursuit steering curvature for unit testing and control verification.
     */
    public static double computePurePursuitCurvature(double alphaRad, double lookaheadDist) {
        return (2.0 * Math.sin(alphaRad)) / Math.max(0.5, lookaheadDist);
    }

    private static @NotNull Vec3 findLookaheadPoint(@NotNull Vec3 pos, @NotNull List<Vec3> path, int startSeg, double lookahead) {
        for (int i = startSeg; i < path.size() - 1; i++) {
            Vec3 a = path.get(i);
            Vec3 b = path.get(i + 1);
            Vec3 hit = intersectCircleSegmentXZ(pos, lookahead, a, b);
            if (hit != null) {
                return hit;
            }
        }
        return path.get(path.size() - 1);
    }

    private static @Nullable Vec3 intersectCircleSegmentXZ(@NotNull Vec3 center, double radius, @NotNull Vec3 a, @NotNull Vec3 b) {
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        double fx = a.x - center.x;
        double fz = a.z - center.z;
        double qa = dx * dx + dz * dz;
        if (qa < 1e-6) {
            return null;
        }
        double qb = 2.0 * (fx * dx + fz * dz);
        double qc = (fx * fx + fz * fz) - radius * radius;
        double disc = qb * qb - 4.0 * qa * qc;
        if (disc < 0.0) {
            return null;
        }
        double sqrtDisc = Math.sqrt(disc);
        double t1 = (-qb + sqrtDisc) / (2.0 * qa);
        double t2 = (-qb - sqrtDisc) / (2.0 * qa);
        double t = -1.0;
        if (t1 >= 0.0 && t1 <= 1.0) {
            t = t1;
        } else if (t2 >= 0.0 && t2 <= 1.0) {
            t = t2;
        }
        if (t < 0.0) {
            return null;
        }
        return new Vec3(a.x + dx * t, Mth.lerp(t, a.y, b.y), a.z + dz * t);
    }

    private static double horizontalDist(@NotNull Vec3 a, @NotNull Vec3 b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    private enum SearchStepResult {
        IN_PROGRESS,
        FOUND,
        FAILED
    }

    /**
     * Resumable budgeted A* / Theta* priority queue search over a flat {@link Costmap25D}.
     */
    private static final class ActiveSearch {
        private static final byte UNVISITED = 0;
        private static final byte OPEN = 1;
        private static final byte CLOSED = 2;

        private final Costmap25D map;
        private final int startIdx;
        private final int goalIdx;
        private final int goalX;
        private final int goalZ;
        private final int goalY;
        private final double[] gScore;
        private final int[] parent;
        private final byte[] nodeState;
        private final PriorityQueue<OpenNode> openQueue;
        private int expandedNodes = 0;
        private boolean finished = false;
        private boolean found = false;

        ActiveSearch(@NotNull Costmap25D map, int sx, int sz, int gx, int gz) {
            this.map = map;
            this.startIdx = map.index(sx, sz);
            this.goalIdx = map.index(gx, gz);
            this.goalX = gx;
            this.goalZ = gz;
            this.goalY = map.surfaceY(gx, gz);

            int total = map.sizeX() * map.sizeZ();
            this.gScore = new double[total];
            this.parent = new int[total];
            this.nodeState = new byte[total];
            Arrays.fill(this.gScore, Double.POSITIVE_INFINITY);
            Arrays.fill(this.parent, -1);

            this.openQueue = new PriorityQueue<>(Comparator.comparingDouble(OpenNode::fScore));
            this.gScore[this.startIdx] = 0.0;
            this.parent[this.startIdx] = this.startIdx;
            this.nodeState[this.startIdx] = OPEN;
            this.openQueue.add(new OpenNode(this.startIdx, heuristic(sx, sz, map.surfaceY(sx, sz))));
        }

        SearchStepResult step(int maxNodes) {
            if (this.finished) {
                return this.found ? SearchStepResult.FOUND : SearchStepResult.FAILED;
            }
            int sizeZ = this.map.sizeZ();
            int processed = 0;

            while (!this.openQueue.isEmpty() && processed < maxNodes) {
                OpenNode current = this.openQueue.poll();
                int idx = current.index();
                if (this.nodeState[idx] == CLOSED) {
                    continue;
                }
                if (idx == this.goalIdx) {
                    this.finished = true;
                    this.found = true;
                    return SearchStepResult.FOUND;
                }

                this.nodeState[idx] = CLOSED;
                processed++;
                this.expandedNodes++;
                if (this.expandedNodes > MAX_TOTAL_EXPANSIONS) {
                    this.finished = true;
                    this.found = false;
                    return SearchStepResult.FAILED;
                }

                int cx = idx / sizeZ;
                int cz = idx % sizeZ;
                boolean allowStartEscape = !this.map.isInflatedPassable(cx, cz);

                for (int dir = 0; dir < 8; dir++) {
                    int nx = cx + DX[dir];
                    int nz = cz + DZ[dir];
                    if (!this.map.inBounds(nx, nz)) {
                        continue;
                    }
                    int nIdx = nx * sizeZ + nz;
                    if (this.nodeState[nIdx] == CLOSED) {
                        continue;
                    }

                    double edgeCost = this.map.transitionCost(cx, cz, nx, nz, allowStartEscape);
                    if (!Double.isFinite(edgeCost)) {
                        continue;
                    }

                    int candidateParent = idx;
                    double tentativeG = this.gScore[idx] + edgeCost;

                    // Theta* lazy line-of-sight relaxation on flat/gentle uniform segments
                    int pIdx = this.parent[idx];
                    if (pIdx >= 0 && pIdx != idx && this.map.isInflatedPassable(nx, nz)) {
                        int px = pIdx / sizeZ;
                        int pz = pIdx % sizeZ;
                        if (this.map.inflatedCost(nx, nz) == Costmap25D.BASE_FLAT
                                && this.map.surfaceY(px, pz) == this.map.surfaceY(nx, nz)
                                && this.map.hasInflatedLineOfSight(px, pz, nx, nz)) {
                            double losG = this.gScore[pIdx] + Math.hypot(nx - px, nz - pz);
                            if (losG + 0.05 < tentativeG) {
                                tentativeG = losG;
                                candidateParent = pIdx;
                            }
                        }
                    }

                    if (tentativeG < this.gScore[nIdx]) {
                        this.gScore[nIdx] = tentativeG;
                        this.parent[nIdx] = candidateParent;
                        this.nodeState[nIdx] = OPEN;
                        double f = tentativeG + heuristic(nx, nz, this.map.surfaceY(nx, nz));
                        this.openQueue.add(new OpenNode(nIdx, f));
                    }
                }
            }

            if (this.openQueue.isEmpty()) {
                this.finished = true;
                this.found = false;
                return SearchStepResult.FAILED;
            }
            return SearchStepResult.IN_PROGRESS;
        }

        private double heuristic(int gx, int gz, int gy) {
            int dx = Math.abs(gx - this.goalX);
            int dz = Math.abs(gz - this.goalZ);
            return (Math.max(dx, dz) + 0.41421356 * Math.min(dx, dz)) + Math.abs(gy - this.goalY) * 1.50;
        }

        @NotNull List<Vec3> extractPrunedWaypoints() {
            if (!this.found) {
                return List.of();
            }
            int sizeZ = this.map.sizeZ();
            List<int[]> rawCells = new ArrayList<>();
            int curr = this.goalIdx;
            int guard = 0;
            while (curr >= 0 && guard++ < MAX_TOTAL_EXPANSIONS) {
                rawCells.add(new int[]{curr / sizeZ, curr % sizeZ});
                if (curr == this.startIdx) {
                    break;
                }
                int next = this.parent[curr];
                if (next == curr) {
                    break;
                }
                curr = next;
            }
            Collections.reverse(rawCells);

            // Greedy 2.5D Bresenham string pulling on the inflated grid
            List<int[]> prunedCells = new ArrayList<>();
            int i = 0;
            prunedCells.add(rawCells.get(0));
            while (i < rawCells.size() - 1) {
                int bestJ = i + 1;
                int[] from = rawCells.get(i);
                for (int j = rawCells.size() - 1; j > i + 1; j--) {
                    int[] to = rawCells.get(j);
                    if (this.map.hasInflatedLineOfSight(from[0], from[1], to[0], to[1])) {
                        bestJ = j;
                        break;
                    }
                }
                prunedCells.add(rawCells.get(bestJ));
                i = bestJ;
            }

            List<Vec3> worldWaypoints = new ArrayList<>(prunedCells.size());
            for (int[] cell : prunedCells) {
                int wx = this.map.toWorldX(cell[0]);
                int wz = this.map.toWorldZ(cell[1]);
                int wy = this.map.surfaceY(cell[0], cell[1]);
                worldWaypoints.add(new Vec3(wx + 0.5, wy, wz + 0.5));
            }
            return Collections.unmodifiableList(worldWaypoints);
        }

        private record OpenNode(int index, double fScore) {
        }
    }
}
