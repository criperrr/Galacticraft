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

import dev.galacticraft.api.vehicle.VehicleDerivedStats;
import dev.galacticraft.api.vehicle.ai.AutonomousVehicleContext;
import dev.galacticraft.api.vehicle.ai.ControlCommand;
import dev.galacticraft.api.vehicle.ai.NavStatus;
import dev.galacticraft.api.vehicle.ai.VehicleNavigator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 3D Fixed-Wing airplane autonomous navigator implementing runway takeoff roll,
 * 3D Dubins curved cruise waypoints with minimum turning radius {@code R_min >= 16} blocks,
 * airspeed-floor stall protection ({@code V >= 1.25 * V_stall}), aligned {@code 6-10 deg}
 * glide-slope runway approach with touchdown flare and wheel braking, automatic {@code GO_AROUND}
 * on lateral misalignment or runway obstruction, and hybrid VTOL fallback when vacuum thrusters are fitted.
 */
public final class FixedWingNavigator3D implements VehicleNavigator {
    public static final double MIN_TURN_RADIUS_BLOCKS = 16.0;
    public static final double MAX_BANK_ANGLE_DEG = 30.0;
    public static final double AIRSPEED_FLOOR_MULTIPLIER = 1.25;
    public static final double APPROACH_FIX_DISTANCE_BLOCKS = 48.0;
    public static final double APPROACH_FIX_ALTITUDE_BLOCKS = 8.0;
    public static final double GLIDE_SLOPE_DEG = 8.0; // within 6.0..10.0 deg window
    public static final double MAX_THRESHOLD_LATERAL_ERROR = 4.0;
    public static final double CRUISE_AGL_OFFSET = 18.0;

    public enum FlightPhase {
        IDLE,
        RUNWAY_TAKEOFF,
        CRUISE_DUBINS,
        GLIDE_SLOPE_APPROACH,
        GO_AROUND,
        ROLLOUT_BRAKING
    }

    private @Nullable BlockPos targetPos;
    private NavStatus status = NavStatus.IDLE;
    private FlightPhase phase = FlightPhase.IDLE;
    private List<Vec3> dubinsWaypoints = List.of();
    private int currentWaypoint = 0;

    private float runwayHeadingDeg;
    private double cruiseAltitudeY;
    private Vec3 approachEntryFix = Vec3.ZERO;
    private int goAroundTicks = 0;
    private boolean usingHybridVtol = false;

    @Override
    public void setTarget(@NotNull BlockPos targetPos) {
        this.targetPos = targetPos.immutable();
        this.status = NavStatus.PLANNING;
        this.phase = FlightPhase.IDLE;
        this.dubinsWaypoints = List.of();
        this.currentWaypoint = 0;
        this.goAroundTicks = 0;
        this.usingHybridVtol = false;
    }

    @Override
    public @Nullable BlockPos getTarget() {
        return this.targetPos;
    }

    public @NotNull FlightPhase getFlightPhase() {
        return this.phase;
    }

    public @NotNull Vec3 getApproachEntryFix() {
        return this.approachEntryFix;
    }

    @Override
    public void blacklistCell(@NotNull BlockPos pos, long gameTime, long ttlTicks) {
        // Fixed-wing airspace avoids obstacles via cruise altitude clearance and traffic-pattern go-around
    }

    @Override
    public void reset() {
        this.targetPos = null;
        this.status = NavStatus.IDLE;
        this.phase = FlightPhase.IDLE;
        this.dubinsWaypoints = List.of();
        this.currentWaypoint = 0;
        this.goAroundTicks = 0;
        this.usingHybridVtol = false;
    }

    @Override
    public @NotNull NavStatus getStatus() {
        return this.status;
    }

    @Override
    public @NotNull List<Vec3> getCurrentPath() {
        return this.dubinsWaypoints;
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

        VehicleDerivedStats stats = vehicle.derivedStats();
        double g = vehicle.gravityG();
        double atm = vehicle.pressureAtm();
        if (!stats.canFlyFixedWingIn(g, atm)) {
            this.status = NavStatus.FAILED;
            return;
        }

        // Determine if hybrid VTOL mode is required (vacuum or ultra-thin atmosphere)
        double stallSpeed = stats.computeStallSpeedBlocksPerTick(g, atm);
        this.usingHybridVtol = stats.isHybridVtolFixedWing()
                && (atm < VehicleDerivedStats.MIN_FIXED_WING_PRESSURE_ATM || !Double.isFinite(stallSpeed) || stallSpeed >= stats.maxSpeedBlocksPerTick() * 0.90);

        Vec3 start = vehicle.position();
        Vec3 goal = new Vec3(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5);
        this.runwayHeadingDeg = vehicle.runwayHeadingDeg();

        this.cruiseAltitudeY = Math.min(
                level.getMaxBuildHeight() - 10.0,
                Math.max(start.y, goal.y) + CRUISE_AGL_OFFSET
        );

        // Compute approach entry fix 48 blocks behind destination runway threshold at +8 blocks (~9.46 deg glide slope)
        this.approachEntryFix = computeApproachEntryFix(goal, this.runwayHeadingDeg);

        // Scan highest terrain along direct corridor (budgeted) so cruise altitude clears hills/mountains
        int samples = Math.min(Math.max(8, maxNodesPerTick / 8), 32);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 1; i < samples; i++) {
            double t = (double) i / samples;
            int sx = Mth.floor(Mth.lerp(t, start.x, this.approachEntryFix.x));
            int sz = Mth.floor(Mth.lerp(t, start.z, this.approachEntryFix.z));
            cursor.set(sx, Mth.floor(this.cruiseAltitudeY), sz);
            if (level.isLoaded(cursor)) {
                int topY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sx, sz);
                if (topY + 10.0 > this.cruiseAltitudeY) {
                    this.cruiseAltitudeY = Math.min(level.getMaxBuildHeight() - 10.0, topY + 10.0);
                }
            }
        }

        double cruiseSpeed = Math.max(computeSafeAirspeedFloor(stallSpeed), Math.min(stats.maxSpeedBlocksPerTick() * 0.85, 0.85));
        double turnRadius = computeMinTurnRadiusBlocks(cruiseSpeed, g);
        this.dubinsWaypoints = buildDubins3DCruiseWaypoints(
                start,
                vehicle.yawDeg(),
                this.approachEntryFix,
                this.runwayHeadingDeg,
                this.cruiseAltitudeY,
                turnRadius
        );
        this.currentWaypoint = 0;

        if (vehicle.isOnGround() || vehicle.altitudeAgl() < 2.0) {
            this.phase = FlightPhase.RUNWAY_TAKEOFF;
        } else {
            // If already airborne along the final approach corridor, join GLIDE_SLOPE_APPROACH directly
            double rwRad = Math.toRadians(this.runwayHeadingDeg);
            double along = -((start.x - goal.x) * (-Math.sin(rwRad)) + (start.z - goal.z) * Math.cos(rwRad));
            if (along >= 0.0 && along <= APPROACH_FIX_DISTANCE_BLOCKS + 8.0) {
                this.phase = FlightPhase.GLIDE_SLOPE_APPROACH;
            } else {
                this.phase = FlightPhase.CRUISE_DUBINS;
            }
        }
        this.status = NavStatus.FOLLOWING;
    }

    @Override
    public @NotNull ControlCommand computeControl(@NotNull ServerLevel level, @NotNull AutonomousVehicleContext vehicle) {
        if (this.targetPos == null || this.status == NavStatus.IDLE || this.status == NavStatus.PLANNING || this.status == NavStatus.FAILED) {
            return ControlCommand.stop();
        }

        VehicleDerivedStats stats = vehicle.derivedStats();
        double g = vehicle.gravityG();
        double atm = vehicle.pressureAtm();
        double stallSpeed = stats.computeStallSpeedBlocksPerTick(g, atm);
        double safeFloorSpeed = computeSafeAirspeedFloor(stallSpeed);

        Vec3 pos = vehicle.position();
        Vec3 goal = new Vec3(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5);

        if (this.usingHybridVtol) {
            return this.computeHybridVtolControl(vehicle, pos, goal);
        }

        switch (this.phase) {
            case RUNWAY_TAKEOFF -> {
                this.status = NavStatus.FOLLOWING;
                double yawErr = Mth.wrapDegrees(this.runwayHeadingDeg - vehicle.yawDeg());
                float yawRate = (float) Mth.clamp(yawErr * 0.18, -2.5, 2.5);
                boolean readyToRotate = Double.isFinite(stallSpeed) && vehicle.forwardSpeed() >= stallSpeed * 1.15;
                float pitch = readyToRotate ? -12.0F : 0.0F; // negative = nose UP in ModularVehicleEntity
                boolean gearDown = vehicle.altitudeAgl() <= 4.0;
                if (!vehicle.isOnGround() && pos.y >= this.cruiseAltitudeY - 3.0) {
                    this.phase = FlightPhase.CRUISE_DUBINS;
                }
                return ControlCommand.fixedWing(1.0F, yawRate, pitch, 0.0F, false, gearDown, false, 0.0F);
            }
            case CRUISE_DUBINS -> {
                this.status = NavStatus.FOLLOWING;
                while (this.currentWaypoint < this.dubinsWaypoints.size() - 1) {
                    Vec3 wp = this.dubinsWaypoints.get(this.currentWaypoint);
                    if (Math.hypot(wp.x - pos.x, wp.z - pos.z) < 8.5) {
                        this.currentWaypoint++;
                    } else {
                        break;
                    }
                }
                Vec3 wp = this.currentWaypoint < this.dubinsWaypoints.size()
                        ? this.dubinsWaypoints.get(this.currentWaypoint)
                        : this.approachEntryFix;

                if (Math.hypot(this.approachEntryFix.x - pos.x, this.approachEntryFix.z - pos.z) < 10.0) {
                    this.phase = FlightPhase.GLIDE_SLOPE_APPROACH;
                }

                double dx = wp.x - pos.x;
                double dz = wp.z - pos.z;
                double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
                double yawErr = Mth.wrapDegrees(targetYaw - vehicle.yawDeg());

                // Bank angle clamped to +/- 30 deg -> honors R_min >= 16 blocks
                float bankRoll = (float) Mth.clamp(yawErr * 0.65, -MAX_BANK_ANGLE_DEG, MAX_BANK_ANGLE_DEG);
                float yawRate = bankRoll * 0.085F;

                // Airspeed-floor protection: NEVER allow V < 1.25 * V_stall
                double airspeed = vehicle.forwardSpeed();
                float throttle = airspeed < safeFloorSpeed ? 1.0F : (float) Mth.clamp(0.65 + (safeFloorSpeed - airspeed) * 1.4, 0.55, 1.0);
                double altErr = wp.y - pos.y;
                float pitch = (float) Mth.clamp(-altErr * 1.35, -14.0, 12.0);
                if (airspeed < safeFloorSpeed * 1.03) {
                    pitch = Math.max(pitch, 2.0F); // lower nose slightly to protect airspeed margin
                }

                return ControlCommand.fixedWing(throttle, yawRate, pitch, bankRoll, false, false, false, 0.0F);
            }
            case GLIDE_SLOPE_APPROACH -> {
                double runwayYawRad = Math.toRadians(this.runwayHeadingDeg);
                double fx = -Math.sin(runwayYawRad);
                double fz = Math.cos(runwayYawRad);
                double rx = -Math.cos(runwayYawRad);
                double rz = -Math.sin(runwayYawRad);

                // Vector from runway threshold to aircraft
                double relX = pos.x - goal.x;
                double relZ = pos.z - goal.z;
                double alongFromThreshold = -(relX * fx + relZ * fz); // positive while approaching threshold
                double lateralError = relX * rx + relZ * rz;

                // Automatic GO_AROUND if misaligned at threshold (> 4.0b) or runway obstructed
                boolean nearThreshold = alongFromThreshold <= 12.0 && alongFromThreshold >= -4.0;
                if (nearThreshold && (Math.abs(lateralError) > MAX_THRESHOLD_LATERAL_ERROR || isRunwayObstructed(level, vehicle, this.targetPos))) {
                    this.phase = FlightPhase.GO_AROUND;
                    this.goAroundTicks = 0;
                    this.status = NavStatus.GO_AROUND;
                    return ControlCommand.fixedWing(1.0F, 0.0F, -12.0F, 0.0F, false, false, false, 0.0F);
                }

                this.status = NavStatus.FOLLOWING;

                // Touchdown detection
                if (vehicle.isOnGround() && Math.hypot(relX, relZ) <= 14.0) {
                    this.phase = FlightPhase.ROLLOUT_BRAKING;
                    return ControlCommand.fixedWing(0.0F, 0.0F, 0.0F, 0.0F, true, true, false, 0.0F);
                }

                // Target 6-10 deg glide-slope altitude (nominal 8.0 deg)
                double desiredY = computeGlideSlopeAltitude(goal.y, Math.max(0.0, alongFromThreshold), GLIDE_SLOPE_DEG);
                double agl = vehicle.altitudeAgl();
                float pitch;
                float throttle;
                if (agl <= 2.2 && alongFromThreshold <= 8.0) {
                    // Touchdown flare (+4 deg nose-up => -4 deg pitch in vehicle coordinates)
                    pitch = -4.0F;
                    throttle = 0.0F;
                } else {
                    double altErr = desiredY - pos.y;
                    pitch = (float) Mth.clamp(-altErr * 1.6 + 5.0, -10.0, 12.0);
                    double approachTargetSpeed = Double.isFinite(stallSpeed) ? stallSpeed * 1.15 : 0.35;
                    throttle = vehicle.forwardSpeed() < approachTargetSpeed ? 0.85F : 0.32F;
                }

                // Localizer centerline tracking
                double interceptYaw = this.runwayHeadingDeg + Mth.clamp(-lateralError * 3.5, -18.0, 18.0);
                double yawErr = Mth.wrapDegrees(interceptYaw - vehicle.yawDeg());
                float bankRoll = (float) Mth.clamp(yawErr * 0.55, -18.0, 18.0);
                float yawRate = (float) Mth.clamp(yawErr * 0.14, -2.5, 2.5);
                return ControlCommand.fixedWing(throttle, yawRate, pitch, bankRoll, false, true, false, 0.0F);
            }
            case GO_AROUND -> {
                this.status = NavStatus.GO_AROUND;
                this.goAroundTicks++;
                // Climb at full throttle +12 deg nose-up, then turn onto traffic pattern back to approach entry fix
                float yawRate = this.goAroundTicks > 30 ? 2.2F : 0.0F;
                float bankRoll = this.goAroundTicks > 30 ? 24.0F : 0.0F;
                float pitch = pos.y < this.cruiseAltitudeY ? -12.0F : 0.0F;
                if (this.goAroundTicks >= 90 && pos.y >= this.cruiseAltitudeY - 4.0) {
                    this.currentWaypoint = Math.max(0, this.dubinsWaypoints.size() - 1);
                    this.phase = FlightPhase.CRUISE_DUBINS;
                }
                return ControlCommand.fixedWing(1.0F, yawRate, pitch, bankRoll, false, false, false, 0.0F);
            }
            case ROLLOUT_BRAKING -> {
                if (vehicle.forwardSpeed() <= 0.03) {
                    this.status = NavStatus.ARRIVED;
                    return ControlCommand.fixedWing(0.0F, 0.0F, 0.0F, 0.0F, true, true, false, 0.0F);
                }
                this.status = NavStatus.FOLLOWING;
                double yawErr = Mth.wrapDegrees(this.runwayHeadingDeg - vehicle.yawDeg());
                float yawRate = (float) Mth.clamp(yawErr * 0.15, -2.0, 2.0);
                return ControlCommand.fixedWing(0.0F, yawRate, 0.0F, 0.0F, true, true, false, 0.0F);
            }
            default -> {
                return ControlCommand.stop();
            }
        }
    }

    private @NotNull ControlCommand computeHybridVtolControl(@NotNull AutonomousVehicleContext vehicle, @NotNull Vec3 pos, @NotNull Vec3 goal) {
        double dx = goal.x - pos.x;
        double dz = goal.z - pos.z;
        double distXZ = Math.hypot(dx, dz);
        if (distXZ <= 2.0 && (vehicle.isOnGround() || pos.y <= goal.y + 0.3)) {
            this.status = NavStatus.ARRIVED;
            return ControlCommand.fixedWing(0.0F, 0.0F, 0.0F, 0.0F, true, true, true, 0.0F);
        }
        this.status = NavStatus.FOLLOWING;
        double targetY = distXZ > 3.0 ? Math.max(goal.y + 10.0, this.cruiseAltitudeY - 6.0) : goal.y;
        double desiredVy = Mth.clamp((targetY - pos.y) * 0.22, -0.18, 0.28);
        float collective = (float) Mth.clamp((desiredVy - vehicle.velocity().y) * 2.4, -0.75, 0.90);
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double yawErr = Mth.wrapDegrees(targetYaw - vehicle.yawDeg());
        float yawRate = (float) Mth.clamp(yawErr * 0.25, -4.5, 4.5);
        float throttle = (distXZ > 2.2 && Math.abs(yawErr) < 45.0) ? (float) Mth.clamp(distXZ * 0.08, 0.15, 0.75) : 0.0F;
        return ControlCommand.fixedWing(throttle, yawRate, 0.0F, 0.0F, false, distXZ <= 4.0, true, collective);
    }

    private static boolean isRunwayObstructed(
            @NotNull ServerLevel level,
            @NotNull AutonomousVehicleContext vehicle,
            @NotNull BlockPos thresholdPos
    ) {
        Entity self = vehicle.asEntity();
        AABB box = new AABB(
                thresholdPos.getX() - 3.0, thresholdPos.getY(), thresholdPos.getZ() - 3.0,
                thresholdPos.getX() + 4.0, thresholdPos.getY() + 3.5, thresholdPos.getZ() + 4.0
        );
        List<Entity> obstacles = level.getEntities(self, box, e ->
                !e.isRemoved()
                        && e.isPickable()
                        && !(e instanceof ItemEntity)
                        && (self == null || (!self.hasPassenger(e) && !e.is(self)))
                        && !(e instanceof dev.galacticraft.mod.content.entity.vehicle.VehiclePartEntity)
        );
        return !obstacles.isEmpty();
    }

    /**
     * Computes minimum safe turning radius in blocks ({@code >= 16.0} blocks) for bank angle {@code <= 30 deg}.
     */
    public static double computeMinTurnRadiusBlocks(double airspeedBlocksPerTick, double gravityG) {
        double vMs = Math.max(0.25, airspeedBlocksPerTick) * 20.0;
        double gMs2 = Math.max(1.5, Math.max(0.16, gravityG) * 9.80665);
        double rawRadius = (vMs * vMs) / (gMs2 * Math.tan(Math.toRadians(MAX_BANK_ANGLE_DEG)));
        return Math.max(MIN_TURN_RADIUS_BLOCKS, rawRadius);
    }

    /**
     * Computes the mandatory airborne airspeed floor {@code 1.25 * V_stall}.
     */
    public static double computeSafeAirspeedFloor(double stallSpeedBlocksPerTick) {
        if (!Double.isFinite(stallSpeedBlocksPerTick) || stallSpeedBlocksPerTick <= 0.0) {
            return 0.35;
        }
        return stallSpeedBlocksPerTick * AIRSPEED_FLOOR_MULTIPLIER;
    }

    /**
     * Computes the ideal glide-slope altitude for a given distance before the runway threshold
     * and glide-slope angle (clamped to {@code 6.0..10.0} degrees).
     */
    public static double computeGlideSlopeAltitude(double thresholdY, double distanceBeforeThreshold, double glideSlopeDeg) {
        double clampedAngle = Mth.clamp(glideSlopeDeg, 6.0, 10.0);
        return thresholdY + Math.max(0.0, distanceBeforeThreshold) * Math.tan(Math.toRadians(clampedAngle));
    }

    /**
     * Computes the approach entry fix {@code 48} blocks behind the runway threshold at {@code +8} blocks altitude.
     */
    public static @NotNull Vec3 computeApproachEntryFix(@NotNull Vec3 runwayThreshold, float runwayHeadingDeg) {
        double yawRad = Math.toRadians(runwayHeadingDeg);
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        return new Vec3(
                runwayThreshold.x - fx * APPROACH_FIX_DISTANCE_BLOCKS,
                runwayThreshold.y + APPROACH_FIX_ALTITUDE_BLOCKS,
                runwayThreshold.z - fz * APPROACH_FIX_DISTANCE_BLOCKS
        );
    }

    /**
     * Generates Dubins-smoothed 3D cruise waypoints from start pose to approach entry fix
     * honoring minimum turn radius {@code turnRadius >= 16} blocks.
     */
    public static @NotNull List<Vec3> buildDubins3DCruiseWaypoints(
            @NotNull Vec3 start,
            float startYawDeg,
            @NotNull Vec3 approachFix,
            float runwayHeadingDeg,
            double cruiseY,
            double turnRadius
    ) {
        double r = Math.max(MIN_TURN_RADIUS_BLOCKS, turnRadius);
        double startRad = Math.toRadians(startYawDeg);
        double sfx = -Math.sin(startRad);
        double sfz = Math.cos(startRad);

        // Climb-out lead point straight ahead along departure axis
        Vec3 departureFix = new Vec3(start.x + sfx * r, cruiseY, start.z + sfz * r);

        // Pre-approach turn-in point aligned behind the approach fix
        double rwRad = Math.toRadians(runwayHeadingDeg);
        double rfx = -Math.sin(rwRad);
        double rfz = Math.cos(rwRad);
        Vec3 baseTurnFix = new Vec3(approachFix.x - rfx * r, cruiseY, approachFix.z - rfz * r);

        List<Vec3> pts = new ArrayList<>();
        pts.add(departureFix);

        // Insert smooth midpoint arc waypoint if large heading change would violate minimum radius
        double segDist = Math.hypot(baseTurnFix.x - departureFix.x, baseTurnFix.z - departureFix.z);
        if (segDist > r * 1.5) {
            Vec3 mid = departureFix.lerp(baseTurnFix, 0.5);
            pts.add(new Vec3(mid.x, cruiseY, mid.z));
        }
        pts.add(baseTurnFix);
        pts.add(approachFix);
        return Collections.unmodifiableList(pts);
    }
}
