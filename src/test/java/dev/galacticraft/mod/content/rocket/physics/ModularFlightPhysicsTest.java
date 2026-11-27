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

package dev.galacticraft.mod.content.rocket.physics;

import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import dev.galacticraft.api.rocket.assembly.MountedPartNode;
import dev.galacticraft.api.rocket.assembly.RadialBoosterGroup;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ModularFlightPhysics}: altitude-dependent {@code Isp(Pa)} &amp; vacuum nozzle
 * hazards, shaped SRB Max-Q thrust curve, Asparagus topological crossfeed draining, Gravity Turn
 * schedule, Max-Q auto-throttle, and automatic fairing jettison thresholds.
 */
public class ModularFlightPhysicsTest {

    private static MountedPartNode engineNode(String modelId, String familyId, float thrustKn, int relX, int relZ) {
        RocketPartConfig cfg = new RocketPartConfig(
                modelId, familyId, RocketPartCategory.ENGINE, 3, 3, 2,
                RocketLivery.WHITE, false, false
        );
        RocketPartStats stats = new RocketPartStats(thrustKn, 1200.0, 0L, 2.0, 1500.0);
        return new MountedPartNode(new BlockPos(relX, 1, relZ), cfg, stats, PartIntegrity.pristine(), Optional.empty(), 0, relX != 0 || relZ != 0);
    }

    @Test
    void testAmbientPressureDecayAndAtmosphericIspAndVacuumNozzle() {
        double pSurface = ModularFlightPhysics.computeAmbientPressureAtm(PlanetaryEnvironment.EARTH, 64.0, 64.0);
        double pMid = ModularFlightPhysics.computeAmbientPressureAtm(PlanetaryEnvironment.EARTH, 180.0, 64.0);
        double pHigh = ModularFlightPhysics.computeAmbientPressureAtm(PlanetaryEnvironment.EARTH, 420.0, 64.0);
        double pMoon = ModularFlightPhysics.computeAmbientPressureAtm(PlanetaryEnvironment.MOON, 100.0, 64.0);

        assertEquals(1.0, pSurface, 1.0e-5);
        assertTrue(pMid < pSurface && pMid > pHigh, "Pressure must decay monotonically with altitude");
        assertTrue(pHigh < ModularFlightPhysics.FAIRING_JETTISON_PA_ATM, "Overworld Y=420 must be below 0.05 atm");
        assertEquals(0.0, pMoon, 1.0e-6);

        // Vacuum engine at sea level produces much less thrust than in vacuum and triggers vacuum hazard
        MountedPartNode vacEngine = engineNode("engine_centaur_rl10_3x3_2b", "engine_centaur_rl10", 400.0F, 0, 0);
        double slThrust = ModularFlightPhysics.computeAtmosphericThrustKn(vacEngine, 1.0, 1.0, 0.0);
        double vacThrust = ModularFlightPhysics.computeAtmosphericThrustKn(vacEngine, 0.0, 1.0, 0.0);
        assertTrue(vacThrust > slThrust * 2.0, "Vacuum nozzle must lose substantial SL performance vs vacuum");

        RocketStageNode vacStage = new RocketStageNode(
                0, Optional.empty(), false, "hydrolox", 81_000L * 20L, 81_000L * 20L,
                List.of(vacEngine), List.of(), false
        );
        var slTick = ModularFlightPhysics.stepStagePropulsion(vacStage, 1.0, 1.0, false);
        var vacTick = ModularFlightPhysics.stepStagePropulsion(vacStage, 0.02, 1.0, false);
        assertTrue(slTick.vacuumNozzleOverexpansion(), "Firing vacuum engine at 1.0 atm must flag over-expansion");
        assertFalse(vacTick.vacuumNozzleOverexpansion(), "Firing vacuum engine at 0.02 atm must be safe");
    }

    @Test
    void testSrbShapedGrainMaxQThrustBucketAndUnthrottleableLock() {
        double liftoff = ModularFlightPhysics.computeSrbThrustCurveMultiplier(0.05);
        double maxQBucket = ModularFlightPhysics.computeSrbThrustCurveMultiplier(0.50);
        double postMaxQ = ModularFlightPhysics.computeSrbThrustCurveMultiplier(0.82);
        double tailOff = ModularFlightPhysics.computeSrbThrustCurveMultiplier(0.98);

        assertEquals(1.00, liftoff, 1.0e-4);
        assertEquals(0.82, maxQBucket, 1.0e-4, "SRB must dip to ~0.82x during Max-Q bucket");
        assertEquals(0.95, postMaxQ, 1.0e-4, "SRB must recover to ~0.95x post Max-Q");
        assertTrue(tailOff < 0.70, "SRB must ramp down during tail-off");

        // SRB ignores throttle=0.20 (unthrottleable once lit)
        MountedPartNode srb = engineNode("srb_heavy_3x3_2b", "srb_heavy", 600.0F, 3, 0);
        double tAtLowThrottle = ModularFlightPhysics.computeAtmosphericThrustKn(srb, 1.0, 0.10, 0.05);
        double tAtFullThrottle = ModularFlightPhysics.computeAtmosphericThrustKn(srb, 1.0, 1.00, 0.05);
        assertEquals(tAtFullThrottle, tAtLowThrottle, 1.0e-4, "SRB thrust must be unthrottleable");
    }

    @Test
    void testAsparagusCrossfeedDrainsOuterLayerFirstKeepingCoreFull() {
        MountedPartNode coreEng = engineNode("engine_falcon1d_3x3_2b", "engine_falcon1d", 500.0F, 0, 0);
        MountedPartNode outerEng = engineNode("engine_falcon1d_3x3_2b", "engine_falcon1d", 400.0F, 4, 0);
        MountedPartNode innerEng = engineNode("engine_falcon1d_3x3_2b", "engine_falcon1d", 400.0F, 2, 0);

        long initialOuterFuel = 81_000L * 2L;
        long initialInnerFuel = 81_000L * 5L;
        long initialCoreFuel = 81_000L * 10L;

        RadialBoosterGroup outerGroup = new RadialBoosterGroup(
                "outer_0", RadialSymmetry.TWO, CrossfeedMode.CONTINUOUS_CROSSFEED, false,
                0, 4.0, "rp1", initialOuterFuel, initialOuterFuel, List.of(outerEng), false
        );
        RadialBoosterGroup innerGroup = new RadialBoosterGroup(
                "inner_1", RadialSymmetry.TWO, CrossfeedMode.CONTINUOUS_CROSSFEED, false,
                1, 2.0, "rp1", initialInnerFuel, initialInnerFuel, List.of(innerEng), false
        );

        RocketStageNode stage = new RocketStageNode(
                0, Optional.empty(), false, "rp1", initialCoreFuel, initialCoreFuel,
                List.of(coreEng), List.of(innerGroup, outerGroup), false
        );

        // Burn until outer_0 hits 0
        ModularFlightPhysics.PropulsionTickResult lastRes = null;
        for (int tick = 0; tick < 500; tick++) {
            lastRes = ModularFlightPhysics.stepStagePropulsion(stage, 0.8, 1.0, false);
            stage = lastRes.updatedStage();
            if (lastRes.depletedRadialGroupIds().contains("outer_0")) {
                break;
            }
        }
        assertTrue(lastRes != null && lastRes.depletedRadialGroupIds().contains("outer_0"),
                "Outermost Asparagus booster layer 0 must deplete first");
        assertEquals(initialCoreFuel, stage.currentFuelDroplets(),
                "Core tank must remain at 100.00% fuel when outermost Asparagus boosters jettison!");
    }

    @Test
    void testGravityTurnMaxQAutoThrottleAndFairingJettison() {
        // Vertical climb during tower clearance (Y <= Y0 + H + 10)
        assertEquals(0.0F, ModularFlightPhysics.computeAutoGravityTurnPitchDeg(75.0, 64.0, 20.0), 1.0e-5F);
        // Smooth gravity turn pitchover higher up
        float midPitch = ModularFlightPhysics.computeAutoGravityTurnPitchDeg(260.0, 64.0, 20.0);
        float highPitch = ModularFlightPhysics.computeAutoGravityTurnPitchDeg(700.0, 64.0, 20.0);
        assertTrue(midPitch > 5.0F && midPitch < highPitch && highPitch <= 45.0F);

        // Max-Q auto-throttle dips into 0.65..0.75 range at peak Max-Q (Y=145)
        float maxQThrottle = ModularFlightPhysics.computeMaxQAutoThrottle(PlanetaryEnvironment.EARTH, 145.0, 64.0, 30_000.0, 1.0F);
        assertTrue(maxQThrottle >= 0.65F && maxQThrottle <= 0.75F, "Expected Max-Q auto-throttle in [0.65, 0.75], got " + maxQThrottle);

        // Fairing jettison threshold on Overworld (Y >= 400 & Pa < 0.05)
        RocketAssemblyGraph fairedGraph = new RocketAssemblyGraph(List.of(), List.of(), 12, 1.5, false, false, true);
        assertFalse(ModularFlightPhysics.shouldAutoJettisonFairings(fairedGraph, PlanetaryEnvironment.EARTH, 200.0, 64.0));
        assertTrue(ModularFlightPhysics.shouldAutoJettisonFairings(fairedGraph, PlanetaryEnvironment.EARTH, 420.0, 64.0));
        assertTrue(ModularFlightPhysics.shouldAutoJettisonFairings(fairedGraph, PlanetaryEnvironment.MARS, 260.0, 64.0));
    }

    @Test
    void testLiftoffTwrIgnoresUnfueledEnginesAndBoosters() {
        MountedPartNode coreEng = engineNode("engine_falcon1d_3x3_2b", "engine_falcon1d", 40.0F, 0, 0);
        MountedPartNode boosterEng = engineNode("engine_falcon1d_3x3_2b", "engine_falcon1d", 800.0F, 3, 0);

        RadialBoosterGroup emptyBooster = new RadialBoosterGroup(
                "b0", RadialSymmetry.TWO, CrossfeedMode.ISOLATED, false,
                0, 3.0, "rp1", 81_000L * 10L, 0L, List.of(boosterEng), false
        );
        RadialBoosterGroup fueledBooster = emptyBooster.withCurrentFuel(81_000L * 10L);

        RocketStageNode stageWithEmptyBoosters = new RocketStageNode(
                0, Optional.empty(), false, "rp1", 81_000L * 5L, 81_000L * 5L,
                List.of(coreEng), List.of(emptyBooster), false
        );
        RocketStageNode stageWithFueledBoosters = new RocketStageNode(
                0, Optional.empty(), false, "rp1", 81_000L * 5L, 81_000L * 5L,
                List.of(coreEng), List.of(fueledBooster), false
        );

        RocketAssemblyGraph gEmpty = new RocketAssemblyGraph(List.of(stageWithEmptyBoosters), List.of(), 8, 3.0, false, false, false);
        RocketAssemblyGraph gFueled = new RocketAssemblyGraph(List.of(stageWithFueledBoosters), List.of(), 8, 3.0, false, false, false);

        double twrEmpty = ModularFlightPhysics.computeLiftoffTwr(gEmpty, PlanetaryEnvironment.EARTH);
        double twrFueled = ModularFlightPhysics.computeLiftoffTwr(gFueled, PlanetaryEnvironment.EARTH);

        assertTrue(twrFueled > twrEmpty * 3.0,
                "Fueled radial boosters must significantly boost liftoff TWR vs unfueled (" + twrFueled + " vs " + twrEmpty + ")");
        assertTrue(twrEmpty < ModularFlightPhysics.MIN_LIFTOFF_TWR,
                "Unfueled radial boosters must NOT inflate liftoff TWR above MIN_LIFTOFF_TWR (got " + twrEmpty + ")");
    }

    @Test
    void testRebalancedBurnDurationAndSolidCoreDepletion() {
        assertEquals(1.00, ModularFlightPhysics.MIN_LIFTOFF_TWR, 1.0e-6, "MIN_LIFTOFF_TWR must allow liftoff whenever TWR > 1.0");
        // 1-tank 3x3 RP-1 stage depletes quickly (~200-250 ticks), while 2-tank SSTO burns ~380-460 ticks
        var falconSpec = dev.galacticraft.mod.content.rocket.part.RocketPartCatalog.getInstance().getPart("engine_falcon1d_3x3_2b");
        var tankSpec = dev.galacticraft.mod.content.rocket.part.RocketPartCatalog.getInstance().getPart("tank_fuel_rp1_3x3_2b");
        var falconCfg = falconSpec.toConfig(RocketLivery.WHITE);
        var tankCfg = tankSpec.toConfig(RocketLivery.WHITE);
        var falconStats = dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.calculate(falconCfg).stats();
        var tankStats = dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.calculate(tankCfg).stats();

        MountedPartNode engNode = new MountedPartNode(new BlockPos(0, 1, 0), falconCfg, falconStats, PartIntegrity.pristine(), Optional.empty(), 0, false);
        long oneTankCap = tankStats.fuelCapacityDroplets();
        RocketStageNode oneTankStage = new RocketStageNode(
                0, Optional.empty(), false, "rp1", oneTankCap, oneTankCap,
                List.of(engNode), List.of(), false
        );

        int oneTankTicks = 0;
        for (int t = 0; t < 600; t++) {
            var res = ModularFlightPhysics.stepStagePropulsion(oneTankStage, 0.5, 1.0, false);
            oneTankStage = res.updatedStage();
            oneTankTicks++;
            if (res.coreDepleted()) {
                break;
            }
        }
        assertTrue(oneTankTicks >= 160 && oneTankTicks <= 280,
                "1-tank 3x3 RP-1 stage must deplete in [160, 280] ticks at full throttle, took " + oneTankTicks);
        assertEquals(0L, oneTankStage.currentFuelDroplets());

        long twoTankCap = oneTankCap * 2L;
        RocketStageNode twoTankStage = new RocketStageNode(
                0, Optional.empty(), false, "rp1", twoTankCap, twoTankCap,
                List.of(engNode), List.of(), false
        );
        int twoTankTicks = 0;
        for (int t = 0; t < 900; t++) {
            var res = ModularFlightPhysics.stepStagePropulsion(twoTankStage, 0.5, 1.0, false);
            twoTankStage = res.updatedStage();
            twoTankTicks++;
            if (res.coreDepleted()) {
                break;
            }
        }
        assertTrue(twoTankTicks >= 340 && twoTankTicks <= 520,
                "2-tank 3x3 RP-1 SSTO stage must burn in [340, 520] ticks to reach orbit, took " + twoTankTicks);

        // Solid core stage with fuelCapacityDroplets > 0 must stop firing once currentFuelDroplets == 0
        MountedPartNode srbNode = engineNode("srb_ares_3x3_2b", "srb_ares", 3000.0F, 0, 0);
        RocketStageNode depletedSrbStage = new RocketStageNode(
                0, Optional.empty(), false, "solid", 81_000L * 20L, 0L,
                List.of(srbNode), List.of(), false
        );
        var depletedRes = ModularFlightPhysics.stepStagePropulsion(depletedSrbStage, 1.0, 1.0, false);
        assertEquals(0.0, depletedRes.totalThrustKn(), 1e-6, "Depleted solid core stage must produce zero thrust");
        assertTrue(depletedRes.coreDepleted(), "Depleted solid core stage must report coreDepleted=true");
    }
}
