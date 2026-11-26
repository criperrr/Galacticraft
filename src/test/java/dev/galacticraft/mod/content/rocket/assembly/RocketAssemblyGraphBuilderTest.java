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

package dev.galacticraft.mod.content.rocket.assembly;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.rocket.assembly.AttachmentNode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import dev.galacticraft.api.rocket.assembly.RadialBoosterGroup;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.rocket.assembly.RocketAssemblyGraphBuilder.BuildResult;
import dev.galacticraft.mod.content.rocket.assembly.RocketAssemblyGraphBuilder.IssueCode;
import dev.galacticraft.mod.content.rocket.assembly.RocketAssemblyGraphBuilder.PartInput;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.content.rocket.physics.PropellantType;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator.GraphFlightValidationReport;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator.HazardCode;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RocketAssemblyGraphBuilder & RocketFlightValidator.evaluateGraph Multi-Stage Physics")
class RocketAssemblyGraphBuilderTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
        GCBlocks.register();
    }

    private static RocketPartConfig part(String modelId) {
        RocketPartSpec spec = RocketPartCatalog.getInstance().getPart(modelId);
        assertNotNull(spec, "Expected catalog part: " + modelId);
        return spec.toConfig(RocketLivery.WHITE);
    }

    @Test
    @DisplayName("Diameter continuity rejects direct 5x5 -> 1x1 jump and accepts bridged 5x5 -> 3x3 -> 1x1 adapters")
    void diameterContinuityValidation() {
        RocketPartConfig engine5 = part("engine_vulcain_ssme_5x5_3b");
        RocketPartConfig cap1 = part("capsule_vostok_1x1_1b");
        RocketPartConfig adapter5to3 = part("adapter_5x5_to_3x3_2b");
        RocketPartConfig adapter3to1 = part("adapter_3x3_to_1x1_1b");

        // Direct 5x5 -> 1x1 jump MUST flag DIAMETER_MISMATCH
        BuildResult mismatched = RocketAssemblyGraphBuilder.build(
                List.of(
                        PartInput.fromConfig(new BlockPos(0, 1, 0), engine5),
                        PartInput.fromConfig(new BlockPos(0, 4, 0), cap1)
                ),
                PlatformSize.SIZE_5X5, null, Map.of(), Map.of(), CrossfeedMode.ISOLATED
        );
        assertTrue(mismatched.hasIssue(IssueCode.DIAMETER_MISMATCH), "Direct 5x5 -> 1x1 jump must report DIAMETER_MISMATCH");
        assertTrue(mismatched.hasErrors());

        // Bridged 5x5 -> 3x3 -> 1x1 with adapters MUST pass diameter check
        BuildResult bridged = RocketAssemblyGraphBuilder.build(
                List.of(
                        PartInput.fromConfig(new BlockPos(0, 1, 0), engine5),
                        PartInput.fromConfig(new BlockPos(0, 4, 0), adapter5to3),
                        PartInput.fromConfig(new BlockPos(0, 6, 0), adapter3to1),
                        PartInput.fromConfig(new BlockPos(0, 7, 0), cap1)
                ),
                PlatformSize.SIZE_5X5, null, Map.of(), Map.of(), CrossfeedMode.ISOLATED
        );
        assertFalse(bridged.hasIssue(IssueCode.DIAMETER_MISMATCH), "Properly bridged adapters must pass diameter continuity");
        assertTrue(bridged.isValid());
    }

    @Test
    @DisplayName("Stage separators split graph into Stage 0..N with hot-staging, crew seats, and capability flags")
    void multiStageDetectionAndMetadata() {
        RocketPartConfig eng0 = part("engine_falcon1d_3x3_2b");
        RocketPartConfig tank0 = part("tank_fuel_rp1_3x3_2b");
        RocketPartConfig hotInterstage = part("interstage_hotstaging_3x3_1b");
        RocketPartConfig eng1 = part("engine_merlinvac_3x3_2b");
        RocketPartConfig tank1 = part("tank_lox_cryo_3x3_2b");
        RocketPartConfig legs = part("landing_gear_stabilizer_3x3_1b");
        RocketPartConfig cap = part("capsule_gemini_3x3_2b");

        BuildResult built = RocketAssemblyGraphBuilder.build(
                List.of(
                        PartInput.fromConfig(new BlockPos(0, 1, 0), eng0),
                        PartInput.fromConfig(new BlockPos(0, 3, 0), tank0),
                        PartInput.fromConfig(new BlockPos(0, 5, 0), hotInterstage),
                        PartInput.fromConfig(new BlockPos(0, 6, 0), eng1),
                        PartInput.fromConfig(new BlockPos(0, 8, 0), tank1),
                        PartInput.fromConfig(new BlockPos(0, 10, 0), legs),
                        PartInput.fromConfig(new BlockPos(0, 11, 0), cap)
                ),
                PlatformSize.SIZE_3X3, null, Map.of(), Map.of(), CrossfeedMode.ISOLATED
        );

        assertTrue(built.isValid(), "Expected valid 2-stage rocket, got issues: " + built.issues());
        RocketAssemblyGraph graph = built.graph();
        assertEquals(2, graph.stages().size(), "Expected 2 stages separated by interstage_hotstaging");

        RocketStageNode s0 = graph.stages().get(0);
        RocketStageNode s1 = graph.stages().get(1);
        assertEquals(0, s0.stageIndex());
        assertTrue(s0.hotStaging(), "interstage_hotstaging must set hotStaging=true on Stage 0");
        assertTrue(s0.decouplerRelativePos().isPresent());
        assertEquals("rp1", s0.propellantId());

        assertEquals(1, s1.stageIndex());
        assertFalse(s1.hotStaging());
        assertEquals(2, graph.crewSeats().size(), "Gemini 3x3 capsule must yield 2 crew seats");
        assertTrue(graph.hasLandingGear());
        assertFalse(graph.hasLesAbort());
    }

    @Test
    @DisplayName("Multi-stage recursive Tsiolkovsky delta-V exceeds single-stage equivalent and Asparagus beats Isolated")
    void multiStageAndAsparagusDeltaVPhysics() {
        RocketPartConfig eng = part("engine_falcon1d_3x3_2b");
        RocketPartConfig tank = part("tank_fuel_rp1_3x3_2b");
        RocketPartConfig decoupler = part("decoupler_inline_3x3_1b");
        RocketPartConfig cap = part("capsule_gemini_3x3_2b");

        // Single-stage vehicle: 2 engines + 2 tanks + capsule
        RocketAssemblyGraph singleStage = RocketAssemblyGraphBuilder.build(
                List.of(
                        PartInput.fromConfig(new BlockPos(0, 1, 0), eng),
                        PartInput.fromConfig(new BlockPos(0, 3, 0), tank),
                        PartInput.fromConfig(new BlockPos(0, 5, 0), tank),
                        PartInput.fromConfig(new BlockPos(0, 7, 0), cap)
                ),
                PlatformSize.SIZE_3X3, null, Map.of(), Map.of(), CrossfeedMode.ISOLATED
        ).graph();

        // Two-stage vehicle: Stage 0 (eng + tank + decoupler) -> Stage 1 (eng + tank + cap)
        RocketAssemblyGraph twoStage = RocketAssemblyGraphBuilder.build(
                List.of(
                        PartInput.fromConfig(new BlockPos(0, 1, 0), eng),
                        PartInput.fromConfig(new BlockPos(0, 3, 0), tank),
                        PartInput.fromConfig(new BlockPos(0, 5, 0), decoupler),
                        PartInput.fromConfig(new BlockPos(0, 6, 0), eng),
                        PartInput.fromConfig(new BlockPos(0, 8, 0), tank),
                        PartInput.fromConfig(new BlockPos(0, 10, 0), cap)
                ),
                PlatformSize.SIZE_3X3, null, Map.of(), Map.of(), CrossfeedMode.ISOLATED
        ).graph();

        GraphFlightValidationReport singleReport = RocketFlightValidator.evaluateGraph(singleStage, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.MOON);
        GraphFlightValidationReport twoStageReport = RocketFlightValidator.evaluateGraph(twoStage, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.MOON);

        assertEquals(1, singleReport.stageReports().size());
        assertEquals(2, twoStageReport.stageReports().size());
        assertTrue(twoStageReport.summary().maxDeltaV() > singleReport.summary().maxDeltaV(),
                "2-stage rocket (" + twoStageReport.summary().maxDeltaV()
                        + " m/s) must exceed 1-stage equivalent (" + singleReport.summary().maxDeltaV() + " m/s)");

        // Radial Boosters: 4x liquid boosters (Isolated vs Asparagus Crossfeed)
        RocketPartConfig bEng = part("engine_falcon1d_3x3_2b");
        RocketPartConfig bTank = part("tank_fuel_rp1_3x3_2b");
        List<BlockPos> origins4x = RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(new BlockPos(0, 1, 0), 3, 3, RadialSymmetry.FOUR_CARDINAL);
        assertEquals(4, origins4x.size());

        List<PartInput> withBoosters = new ArrayList<>();
        withBoosters.add(PartInput.fromConfig(new BlockPos(0, 1, 0), eng));
        withBoosters.add(PartInput.fromConfig(new BlockPos(0, 3, 0), tank));
        withBoosters.add(PartInput.fromConfig(new BlockPos(0, 5, 0), cap));
        for (BlockPos base : origins4x) {
            withBoosters.add(PartInput.fromConfig(base, bEng));
            withBoosters.add(PartInput.fromConfig(base.above(2), bTank));
        }

        RocketAssemblyGraph isolatedGraph = RocketAssemblyGraphBuilder.build(
                withBoosters, PlatformSize.SIZE_9X9, null, Map.of(), Map.of(), CrossfeedMode.ISOLATED
        ).graph();
        RocketAssemblyGraph asparagusGraph = RocketAssemblyGraphBuilder.build(
                withBoosters, PlatformSize.SIZE_9X9, null, Map.of(), Map.of(), CrossfeedMode.CONTINUOUS_CROSSFEED
        ).graph();

        RadialBoosterGroup aspGroup = asparagusGraph.stages().get(0).radialGroups().get(0);
        assertEquals(RadialSymmetry.FOUR_CARDINAL, aspGroup.symmetry());
        assertEquals(CrossfeedMode.CONTINUOUS_CROSSFEED, aspGroup.crossfeedMode());
        assertEquals(0, aspGroup.drainPriorityLayer());

        double isolatedDv = RocketFlightValidator.evaluateGraph(isolatedGraph, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.MOON).summary().maxDeltaV();
        double asparagusDv = RocketFlightValidator.evaluateGraph(asparagusGraph, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.MOON).summary().maxDeltaV();

        assertTrue(asparagusDv > isolatedDv,
                "Asparagus crossfeed (" + asparagusDv + " m/s) must deliver strictly more delta-V than isolated boosters (" + isolatedDv + " m/s)");
    }

    @Test
    @DisplayName("Octagonal radial offset math and concentric outer-to-inner Asparagus drainPriorityLayer ordering")
    void radialOffsetAndAsparagusPriorityLayers() {
        assertEquals(2.25, RocketAssemblyGraphBuilder.computeRadialOffsetBlocks(3, 1), 1.0e-6);
        assertEquals(4.25, RocketAssemblyGraphBuilder.computeRadialOffsetBlocks(5, 3), 1.0e-6);
        assertEquals(2, RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(BlockPos.ZERO, 3, 1, RadialSymmetry.TWO).size());
        assertEquals(4, RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(BlockPos.ZERO, 3, 1, RadialSymmetry.FOUR_CARDINAL).size());
        assertEquals(4, RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(BlockPos.ZERO, 3, 1, RadialSymmetry.FOUR_DIAGONAL).size());

        // 5x5 chamfered octagonal core + 1x1 boosters in 8x: cardinal at +-3, diagonal flush at (+-2, +-2)
        List<BlockPos> origins5x5Eight = RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(BlockPos.ZERO, 5, 1, RadialSymmetry.EIGHT);
        assertEquals(8, origins5x5Eight.size());
        assertTrue(origins5x5Eight.contains(new BlockPos(3, 0, 0)), "Expected cardinal booster at (3,0,0)");
        assertTrue(origins5x5Eight.contains(new BlockPos(2, 0, 2)), "Expected chamfer-flush diagonal booster at (2,0,2)");

        // 3x3 core + 1x1 boosters in 8x: flush visual position snaps diagonals smoothly to R_core + R_booster + 0.0625 = 2.0625
        double[] flushDiag = RocketAssemblyGraphBuilder.computeFlushRadialVisualPosition(3, 1, 2, 2, 8);
        assertEquals(2.0625, Math.hypot(flushDiag[0], flushDiag[1]), 1.0e-5,
                "8x diagonal booster must sit flush against octagonal 3x3 hull with zero gap");
        double pad5Allowed = RocketAssemblyGraphBuilder.computeAllowedClearanceRadius(PlatformSize.SIZE_5X5, null);
        assertTrue(RocketAssemblyGraphBuilder.fitsWithinPadClearance(3, 3, 3, pad5Allowed),
                "Square 5x5 pad clearance must accept diagonal 8x boosters within pad bounds");

        // 3x3 core + 8x 1x1 boosters compiles into ONE unified RadialSymmetry.EIGHT RadialBoosterGroup
        RocketPartConfig coreEng = part("engine_falcon1d_3x3_2b");
        RocketPartConfig boosterTank = part("tank_canister_1x1_1b");
        List<PartInput> eightWayInputs = new ArrayList<>();
        eightWayInputs.add(PartInput.fromConfig(new BlockPos(0, 1, 0), coreEng));
        for (BlockPos o : RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(new BlockPos(0, 1, 0), 3, 1, RadialSymmetry.EIGHT)) {
            eightWayInputs.add(PartInput.fromConfig(o, boosterTank));
        }
        BuildResult eightWayBuild = RocketAssemblyGraphBuilder.build(
                eightWayInputs, PlatformSize.SIZE_5X5, null, Map.of(), Map.of(), CrossfeedMode.CONTINUOUS_CROSSFEED
        );
        assertFalse(eightWayBuild.hasIssue(IssueCode.CLEARANCE_OBSTRUCTED), "8x 1x1 boosters on 5x5 pad must pass clearance");
        assertEquals(1, eightWayBuild.graph().stages().get(0).radialGroups().size(),
                "8x cardinal+diagonal ring must compile into a single RadialBoosterGroup");
        assertEquals(RadialSymmetry.EIGHT, eightWayBuild.graph().stages().get(0).radialGroups().get(0).symmetry());

        // Inner pair at +-2, outer pair at +-4 -> outer must get drainPriorityLayer=0, inner=1
        BuildResult concentric = RocketAssemblyGraphBuilder.build(
                List.of(
                        PartInput.fromConfig(new BlockPos(0, 1, 0), coreEng),
                        PartInput.fromConfig(new BlockPos(2, 1, 0), boosterTank),
                        PartInput.fromConfig(new BlockPos(-2, 1, 0), boosterTank),
                        PartInput.fromConfig(new BlockPos(4, 1, 0), boosterTank),
                        PartInput.fromConfig(new BlockPos(-4, 1, 0), boosterTank)
                ),
                PlatformSize.SIZE_9X9, null, Map.of(), Map.of(), CrossfeedMode.CONTINUOUS_CROSSFEED
        );
        List<RadialBoosterGroup> groups = concentric.graph().stages().get(0).radialGroups();
        assertEquals(2, groups.size());
        assertEquals(0, groups.get(0).drainPriorityLayer());
        assertEquals(1, groups.get(1).drainPriorityLayer());
        assertTrue(groups.get(0).radialOffsetBlocks() > groups.get(1).radialOffsetBlocks(),
                "drainPriorityLayer 0 must correspond to the outermost radial booster ring");
    }

    @Test
    @DisplayName("Vacuum nozzle over-expansion triggers at Earth (1.0 atm > 0.40 atm) and clears on Moon vacuum (0.0 atm)")
    void vacuumNozzleAndPadMassLimitChecks() {
        RocketPartConfig vacEngine = part("engine_merlinvac_3x3_2b");
        RocketPartConfig tank = part("tank_fuel_rp1_3x3_2b");
        RocketPartConfig cap = part("capsule_gemini_3x3_2b");

        assertTrue(RocketFlightValidator.isVacuumOptimizedEngine(vacEngine));
        double ispVac = RocketFlightValidator.computeAtmosphericIsp(vacEngine, PropellantType.RP1, null, 0.0);
        double ispEarth = RocketFlightValidator.computeAtmosphericIsp(vacEngine, PropellantType.RP1, null, 1.0);
        assertTrue(ispVac > ispEarth * 1.8, "Vacuum nozzle must lose significant Isp at sea level");

        BuildResult built = RocketAssemblyGraphBuilder.build(
                List.of(
                        PartInput.fromConfig(new BlockPos(0, 1, 0), vacEngine),
                        PartInput.fromConfig(new BlockPos(0, 3, 0), tank),
                        PartInput.fromConfig(new BlockPos(0, 5, 0), cap)
                ),
                PlatformSize.SIZE_3X3, null, Map.of(), Map.of(), CrossfeedMode.ISOLATED
        );

        GraphFlightValidationReport earthLaunch = RocketFlightValidator.evaluateGraph(
                built.graph(), PlanetaryEnvironment.EARTH, PlanetaryEnvironment.MOON, 1_000.0, 0L
        );
        assertTrue(earthLaunch.vacuumNozzleWarning());
        assertTrue(earthLaunch.summary().hasHazard(HazardCode.VACUUM_NOZZLE_OVEREXPANSION));
        assertFalse(earthLaunch.withinPadMassLimit(), "Deliberately tiny 1t pad cap must fail withinPadMassLimit");
        assertTrue(earthLaunch.summary().hasHazard(HazardCode.PAD_OVERWEIGHT));

        GraphFlightValidationReport moonLaunch = RocketFlightValidator.evaluateGraph(
                built.graph(), PlanetaryEnvironment.MOON, PlanetaryEnvironment.EARTH, built.padMaxMassKg(), 0L
        );
        assertFalse(moonLaunch.vacuumNozzleWarning(), "Vacuum engine must have zero over-expansion hazard in lunar vacuum");
        assertFalse(moonLaunch.summary().hasHazard(HazardCode.VACUUM_NOZZLE_OVEREXPANSION));
        assertTrue(moonLaunch.withinPadMassLimit());

        // AttachmentNode generation smoke check
        List<AttachmentNode> tankNodes = RocketAssemblyGraphBuilder.computeAttachmentNodes(tank);
        assertTrue(tankNodes.size() >= 6, "Tank must expose coaxial top/bottom plus 4 radial attachment nodes");
    }

    @Test
    @DisplayName("Rebalanced engine thrust lifts tall stacks (TWR >= 1.4) while 1-stage chemical dV (< 2800 m/s) requires staging for orbit/Moon")
    void rebalancedSingleStageVsMultiStageDeltaVAndTwr() {
        RocketPartConfig eng = part("engine_falcon1d_3x3_2b");
        RocketPartConfig tank = part("tank_fuel_rp1_3x3_2b");
        RocketPartConfig decoupler = part("decoupler_inline_3x3_1b");
        RocketPartConfig vacEng = part("engine_centaur_rl10_3x3_2b");
        RocketPartConfig cryoTank = part("tank_lox_cryo_3x3_2b");
        RocketPartConfig cap = part("capsule_gemini_3x3_2b");

        // 1-stage 3x3 RP-1 rocket with 2 tanks: strong liftoff TWR on Earth, but < 2800 m/s dV (cannot reach orbit alone)
        RocketAssemblyGraph singleStage = RocketAssemblyGraphBuilder.build(
                List.of(
                        PartInput.fromConfig(new BlockPos(0, 1, 0), eng),
                        PartInput.fromConfig(new BlockPos(0, 3, 0), tank),
                        PartInput.fromConfig(new BlockPos(0, 5, 0), tank),
                        PartInput.fromConfig(new BlockPos(0, 7, 0), cap)
                ),
                PlatformSize.SIZE_3X3, null, Map.of(), Map.of(), CrossfeedMode.ISOLATED
        ).graph();

        GraphFlightValidationReport singleEarth = RocketFlightValidator.evaluateGraph(
                singleStage, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.EARTH
        );
        assertTrue(singleEarth.summary().originTwr() >= 1.40,
                "Rebalanced Falcon 1D must lift a 2-tank 3x3 stack with TWR >= 1.40, got " + singleEarth.summary().originTwr());
        assertTrue(singleEarth.summary().maxDeltaV() < PlanetaryEnvironment.EARTH.orbitAscentDeltaV(),
                "1-stage chemical RP-1 rocket must deliver < 2800 m/s so staging is required for orbit, got "
                        + singleEarth.summary().maxDeltaV());

        // 2-stage + 4x SRB boosters reaches Earth orbit and Moon!
        List<PartInput> twoStageInputs = new ArrayList<>(List.of(
                PartInput.fromConfig(new BlockPos(0, 1, 0), eng),
                PartInput.fromConfig(new BlockPos(0, 3, 0), tank),
                PartInput.fromConfig(new BlockPos(0, 5, 0), tank),
                PartInput.fromConfig(new BlockPos(0, 7, 0), decoupler),
                PartInput.fromConfig(new BlockPos(0, 8, 0), vacEng),
                PartInput.fromConfig(new BlockPos(0, 10, 0), cryoTank),
                PartInput.fromConfig(new BlockPos(0, 12, 0), cap)
        ));
        RocketPartConfig srb = part("srb_ares_3x3_2b");
        for (BlockPos origin : RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(new BlockPos(0, 1, 0), 3, 3, RadialSymmetry.FOUR_CARDINAL)) {
            twoStageInputs.add(PartInput.fromConfig(origin, srb));
        }

        RocketAssemblyGraph twoStageWithBoosters = RocketAssemblyGraphBuilder.build(
                twoStageInputs, PlatformSize.SIZE_9X9, null, Map.of(), Map.of(), CrossfeedMode.ISOLATED
        ).graph();
        GraphFlightValidationReport moonMission = RocketFlightValidator.evaluateGraph(
                twoStageWithBoosters, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.MOON
        );
        assertTrue(moonMission.summary().canLiftoffOrigin(),
                "2-stage + 4x SRB must lift off Earth cleanly, got TWR=" + moonMission.summary().originTwr());
        assertTrue(moonMission.summary().hasEnoughDeltaV(),
                "2-stage + 4x SRB must have enough delta-V for Moon transfer, got dV="
                        + moonMission.summary().maxDeltaV() + " vs req=" + moonMission.summary().requiredDeltaV());
    }
}
