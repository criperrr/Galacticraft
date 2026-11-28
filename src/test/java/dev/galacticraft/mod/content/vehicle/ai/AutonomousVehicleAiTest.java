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

import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.vehicle.VehicleDerivedStats;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.api.vehicle.ai.CargoLoadCondition;
import dev.galacticraft.api.vehicle.ai.VehicleAiState;
import dev.galacticraft.api.vehicle.ai.VehicleMissionConfig;
import dev.galacticraft.api.vehicle.ai.VehicleMissionRole;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AutonomousVehicleAiTest {
    @Test
    void missionConfigAndEnumsRoundTripCodecAndStreamCodec() {
        VehicleMissionConfig original = new VehicleMissionConfig(
                true,
                VehicleMissionRole.CARGO_SHUTTLE,
                List.of(new BlockPos(10, 64, 20), new BlockPos(-30, 65, 80)),
                Optional.of(new BlockPos(0, 64, 0)),
                CargoLoadCondition.FULL,
                96,
                true,
                false,
                10.0,
                UUID.randomUUID().toString(),
                135
        );

        var json = VehicleMissionConfig.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        var decodedJson = VehicleMissionConfig.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(original, decodedJson);

        var buf = Unpooled.buffer();
        VehicleMissionConfig.STREAM_CODEC.encode(buf, original);
        var decodedBuf = VehicleMissionConfig.STREAM_CODEC.decode(buf);
        assertEquals(original, decodedBuf);

        for (VehicleAiState state : VehicleAiState.values()) {
            var b = Unpooled.buffer();
            VehicleAiState.STREAM_CODEC.encode(b, state);
            assertEquals(state, VehicleAiState.STREAM_CODEC.decode(b));
        }
        for (VehicleMissionRole role : VehicleMissionRole.values()) {
            var b = Unpooled.buffer();
            VehicleMissionRole.STREAM_CODEC.encode(b, role);
            assertEquals(role, VehicleMissionRole.STREAM_CODEC.decode(b));
        }
    }

    @Test
    void costmapInflatesWideFootprintAndRejectsCratersAndNarrowGaps() {
        int sx = 20;
        int sz = 15;
        short[] y = new short[sx * sz];
        byte[] raw = new byte[sx * sz];
        Arrays.fill(y, (short) 64);
        Arrays.fill(raw, Costmap25D.BASE_FLAT);

        // 3-block-deep crater at x=6..7, z=6..7
        for (int cx = 6; cx <= 7; cx++) {
            for (int cz = 6; cz <= 7; cz++) {
                y[cx * sz + cz] = 61;
            }
        }
        // Wall at x=12, z=3..11 with a 1-block gap at z=7
        for (int cz = 3; cz <= 11; cz++) {
            if (cz == 7) {
                continue;
            }
            y[12 * sz + cz] = 67;
        }

        Costmap25D map = Costmap25D.fromSyntheticGrid(0, 0, sx, sz, y, raw, 1);

        // Crater rim must be inflated as IMPASSABLE so wide rover never drives a wheel into pit
        assertFalse(map.isInflatedPassable(5, 6), "Crater rim neighbor must be inflated impassable");
        assertFalse(map.isInflatedPassable(6, 6), "Crater cell must be impassable");
        // 1-block gap (12, 7) has wall neighbors (12, 6) and (12, 8) with |dy|=3 -> MUST be impassable
        assertFalse(map.isInflatedPassable(12, 7), "1-block narrow gap must be inflated impassable for wide rover");

        List<Vec3> path = GroundNavigator25D.planOnCostmap(map, new BlockPos(2, 64, 7), new BlockPos(17, 64, 7), 250, 20);
        assertFalse(path.isEmpty(), "Budgeted 2.5D A* must find clean detour around crater and wall");
        assertTrue(path.size() < 12, "String pulling must prune grid zig-zag into sparse waypoints, got " + path.size());
        for (Vec3 wp : path) {
            int gx = (int) Math.floor(wp.x);
            int gz = (int) Math.floor(wp.z);
            assertTrue(map.isInflatedPassable(gx, gz), "Every pruned waypoint must stay strictly inside inflated safe zone: " + wp);
            assertFalse(gx == 12 && gz == 7, "Rover must never route through 1-block gap");
        }
    }

    @Test
    void costmapAllowsOneBlockStepWithSlopePenaltyAndPenalizesLowTraction() {
        int sx = 12;
        int sz = 9;
        short[] y = new short[sx * sz];
        byte[] raw = new byte[sx * sz];
        for (int gx = 0; gx < sx; gx++) {
            for (int gz = 0; gz < sz; gz++) {
                y[gx * sz + gz] = (short) (gx >= 6 ? 65 : 64);
                raw[gx * sz + gz] = gz == 4 ? Costmap25D.BASE_FLAT : Costmap25D.LOW_TRACTION;
            }
        }
        Costmap25D map = Costmap25D.fromSyntheticGrid(0, 0, sx, sz, y, raw, 1);

        assertTrue(map.isInflatedPassable(5, 4));
        assertTrue(map.isInflatedPassable(6, 4));
        double climbCost = map.transitionCost(5, 4, 6, 4, false);
        double flatCost = map.transitionCost(4, 4, 5, 4, false);
        double slipCost = map.transitionCost(4, 4, 4, 3, false);

        assertTrue(Double.isFinite(climbCost), "1-block step must be climbable");
        assertEquals(flatCost + Costmap25D.SLOPE_UP_PENALTY, climbCost, 1e-6, "1-block upward step must apply +2.50 slope penalty");
        assertTrue(slipCost > flatCost + 1.4, "Low-traction cell must apply +1.50x slip penalty");
    }

    @Test
    void rotorcraftCoarse3DAStarRoutesOverWall() {
        int sx = 14;
        int sy = 10;
        int sz = 8;
        boolean[] free = new boolean[sx * sy * sz];
        Arrays.fill(free, true);
        // Solid wall across x=6 from y=0..4 (10 blocks tall at cellSize=2)
        for (int gy = 0; gy <= 4; gy++) {
            for (int gz = 0; gz < sz; gz++) {
                free[(6 * sy + gy) * sz + gz] = false;
            }
        }
        RotorcraftNavigator3D.CoarseGrid3D grid = RotorcraftNavigator3D.CoarseGrid3D.fromSynthetic(0, 60, 0, 2, sx, sy, sz, free);
        List<Vec3> path = RotorcraftNavigator3D.planCoarse3D(grid, new BlockPos(2, 62, 6), new BlockPos(22, 62, 6), 72, 200, 10);
        assertFalse(path.isEmpty(), "Coarse-grid 3D A* must find an over-the-wall flight path");
        double maxY = path.stream().mapToDouble(v -> v.y).max().orElse(0.0);
        assertTrue(maxY >= 70.0, "3D path must climb above 10-block wall, maxY=" + maxY);
    }

    @Test
    void fixedWingDubinsTurnRadiusAirspeedFloorAndGlideSlope() {
        VehicleDerivedStats earthPlane = VehicleRegistry.computeStats(
                VehicleRegistry.createAutonomousPreset(VehicleRegistry.CHASSIS_LIGHT_AIRPLANE)
        );
        assertTrue(earthPlane.hasAiCore());
        double vStall = earthPlane.computeStallSpeedBlocksPerTick(1.0, 1.0);
        double safeFloor = FixedWingNavigator3D.computeSafeAirspeedFloor(vStall);
        assertEquals(vStall * 1.25, safeFloor, 1e-6, "Airspeed floor must enforce V >= 1.25 * V_stall");

        double rMin = FixedWingNavigator3D.computeMinTurnRadiusBlocks(0.75, 1.0);
        assertTrue(rMin >= FixedWingNavigator3D.MIN_TURN_RADIUS_BLOCKS, "Minimum turn radius must be >= 16 blocks, got " + rMin);

        Vec3 threshold = new Vec3(100.5, 64.0, 100.5);
        Vec3 fix = FixedWingNavigator3D.computeApproachEntryFix(threshold, 0.0F);
        assertEquals(48.0, threshold.z - fix.z, 1e-6, "Approach fix must lie 48 blocks behind runway threshold");
        assertEquals(8.0, fix.y - threshold.y, 1e-6, "Approach fix must sit +8 blocks above threshold (~9.46 deg glide slope)");

        double glideAngleDeg = Math.toDegrees(Math.atan2(fix.y - threshold.y, threshold.z - fix.z));
        assertTrue(glideAngleDeg >= 6.0 && glideAngleDeg <= 10.0, "Approach glide slope must sit within 6-10 deg, got " + glideAngleDeg);

        double altAt24 = FixedWingNavigator3D.computeGlideSlopeAltitude(64.0, 24.0, 8.0);
        assertTrue(altAt24 > 66.5 && altAt24 < 68.5, "Glide-slope altitude at 24m should be ~67.37m, got " + altAt24);

        List<Vec3> dubins = FixedWingNavigator3D.buildDubins3DCruiseWaypoints(
                new Vec3(0.5, 64.0, 0.5), 90.0F, fix, 0.0F, 82.0, rMin
        );
        assertTrue(dubins.size() >= 3, "Dubins generator must produce curved departure, base, and approach fix waypoints");
        assertEquals(fix, dubins.get(dubins.size() - 1));
    }

    @Test
    void autonomousPresetsIncludeAiCoreAndInstruments() {
        VehicleDerivedStats rover = VehicleRegistry.computeStats(VehicleRegistry.createAutonomousPreset(VehicleRegistry.CHASSIS_SCOUT_ROVER));
        VehicleDerivedStats hauler = VehicleRegistry.computeStats(VehicleRegistry.createAutonomousPreset(VehicleRegistry.CHASSIS_HEAVY_HAULER));
        VehicleDerivedStats heli = VehicleRegistry.computeStats(VehicleRegistry.createAutonomousPreset(VehicleRegistry.CHASSIS_LIGHT_HELICOPTER));
        VehicleDerivedStats plane = VehicleRegistry.computeStats(VehicleRegistry.createAutonomousPreset(VehicleRegistry.CHASSIS_LIGHT_AIRPLANE));

        assertTrue(rover.hasAiCore() && rover.hasCameraMast() && rover.isDrivable());
        assertTrue(hauler.hasAiCore() && hauler.hasDrillArm() && hauler.isDrivable());
        assertTrue(heli.hasAiCore() && heli.isRotorcraft() && heli.isDrivable());
        assertTrue(plane.hasAiCore() && plane.isFixedWing() && plane.isDrivable());
    }
}
