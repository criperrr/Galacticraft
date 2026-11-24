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

package dev.galacticraft.mod.gametest;

import dev.galacticraft.api.registry.AddonRegistries;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.machine.AerospaceRdTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.EnvironmentalTestChamberBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.GeologicalAnalyzerBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ObservatoryTelescopeBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ResearchDataLinkBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ResearchWorkbenchBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ZeroGResearchChamberBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchDomain;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.research.ResearchMilestone;
import dev.galacticraft.mod.research.TeamResearchState;
import dev.galacticraft.mod.util.FluidUtil;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class InSituResearchLaboratoriesTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE, batch = "inSituResearch", timeoutTicks = 160)
    public void zeroGResearchChamberGravityEnforcementTest(GameTestHelper context) {
        TeamResearchState state = ResearchManager.get(context.getLevel());

        Registry<CelestialBody<?, ?>> bodies = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> asteroidBody = bodies.get(GCCelestialBodies.ASTEROID);
        CelestialBody<?, ?> moonBody = bodies.get(GCCelestialBodies.MOON);
        if (asteroidBody == null || moonBody == null) {
            context.fail("Missing Asteroid or Moon CelestialBody in registry");
            return;
        }

        BlockPos earthLabPos = new BlockPos(0, 1, 0);
        BlockPos moonLabPos = new BlockPos(2, 1, 0);
        BlockPos zeroGLabPos = new BlockPos(4, 1, 0);

        context.setBlock(earthLabPos, GCBlocks.ZERO_G_RESEARCH_CHAMBER);
        context.setBlock(moonLabPos, GCBlocks.ZERO_G_RESEARCH_CHAMBER);
        context.setBlock(zeroGLabPos, GCBlocks.ZERO_G_RESEARCH_CHAMBER);

        ZeroGResearchChamberBlockEntity earthLab = context.getBlockEntity(earthLabPos);
        ZeroGResearchChamberBlockEntity moonLab = context.getBlockEntity(moonLabPos);
        ZeroGResearchChamberBlockEntity zeroGLab = context.getBlockEntity(zeroGLabPos);

        // Dedicated Zero-G confinement coil accepted; telescope mirror rejected
        if (!zeroGLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.CONFINEMENT_UPGRADE_SLOT)
                .getFilter().test(GCItems.SUPERCONDUCTING_CONFINEMENT_COIL, DataComponentPatch.EMPTY)
                || zeroGLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.CONFINEMENT_UPGRADE_SLOT)
                .getFilter().test(GCItems.SAPPHIRE_CASSEGRAIN_MIRROR, DataComponentPatch.EMPTY)) {
            context.fail("Zero-G confinement bay must accept only Zero-G rings and reject telescope optics");
            return;
        }

        earthLab.energyStorage().setEnergy(50_000L);
        earthLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.MATERIAL_A_SLOT).set(GCItems.CARBON_FRAGMENTS, 1);

        moonLab.setEnvironmentContextForTest(GCDimensions.MOON, moonBody);
        moonLab.energyStorage().setEnergy(50_000L);
        moonLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.MATERIAL_A_SLOT).set(GCItems.CARBON_FRAGMENTS, 1);

        zeroGLab.setEnvironmentContextForTest(GCDimensions.ASTEROID, asteroidBody);
        zeroGLab.energyStorage().setEnergy(50_000L);
        zeroGLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.MATERIAL_A_SLOT).set(GCItems.NIOBIUM_INGOT, 1);
        zeroGLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.MATERIAL_B_SLOT).set(GCItems.TIN_INGOT, 1);

        context.runAfterDelay(115, () -> {
            if (earthLab.getState().getStatus() != GCMachineStatuses.GRAVITY_TOO_HIGH) {
                context.fail("Zero-G chamber on Earth (1.0G) must report GRAVITY_TOO_HIGH, got " + earthLab.getState().getStatus());
                return;
            }
            if (earthLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.MATERIAL_A_SLOT).getAmount() != 1
                    || !earthLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.OUTPUT_SLOT).isEmpty()) {
                context.fail("Zero-G chamber on Earth must NOT consume inputs or produce output");
                return;
            }
            if (moonLab.getState().getStatus() != GCMachineStatuses.GRAVITY_TOO_HIGH) {
                context.fail("Zero-G chamber on Moon (0.166G) must report GRAVITY_TOO_HIGH, got " + moonLab.getState().getStatus());
                return;
            }
            if (zeroGLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.OUTPUT_SLOT).getResource() != GCItems.SUPERCONDUCTING_COIL) {
                context.fail("Zero-G chamber in Asteroid Belt (0.0G) must synthesize SUPERCONDUCTING_COIL, got "
                        + zeroGLab.itemStorage().slot(ZeroGResearchChamberBlockEntity.OUTPUT_SLOT).getResource());
                return;
            }
            if (!state.isUnlocked(ResearchCatalog.ZERO_G_SUPERCONDUCTORS_NB3SN)
                    || !state.hasMilestone(ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED)) {
                context.fail("Zero-G chamber must unlock ZERO_G_SUPERCONDUCTORS_NB3SN and ZERO_G_LEVITATION_CERTIFIED milestone");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "inSituResearch", timeoutTicks = 180)
    public void observatoryTelescopeAtmosphericReachTest(GameTestHelper context) {
        TeamResearchState state = ResearchManager.get(context.getLevel());

        Registry<CelestialBody<?, ?>> bodies = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> moonBody = bodies.get(GCCelestialBodies.MOON);
        CelestialBody<?, ?> marsBody = bodies.get(GCCelestialBodies.MARS);
        if (moonBody == null || marsBody == null) {
            context.fail("Missing Moon or Mars CelestialBody in registry");
            return;
        }

        // Verify altitude Y reduces Earth effective airmass & boosts clarity
        PlanetaryEnvironment earthEnv = PlanetaryEnvironment.byId(Level.OVERWORLD.location());
        double seaAirmass = ObservatoryTelescopeBlockEntity.computeEffectiveAirmass(earthEnv, 64);
        double peakAirmass = ObservatoryTelescopeBlockEntity.computeEffectiveAirmass(earthEnv, 220);
        if (peakAirmass >= seaAirmass * 0.35) {
            context.fail("Expected mountain altitude Y=220 to sharply lower airmass vs sea level Y=64");
            return;
        }

        BlockPos earthScopePos = new BlockPos(0, 1, 0);
        BlockPos blockedScopePos = new BlockPos(2, 1, 0);
        BlockPos moonScopePos = new BlockPos(4, 1, 0);
        BlockPos marsScopePos = new BlockPos(6, 1, 0);

        context.setBlock(earthScopePos, GCBlocks.OBSERVATORY_TELESCOPE);
        context.setBlock(blockedScopePos, GCBlocks.OBSERVATORY_TELESCOPE);
        context.setBlock(blockedScopePos.above(2), Blocks.STONE);
        context.setBlock(moonScopePos, GCBlocks.OBSERVATORY_TELESCOPE);
        context.setBlock(marsScopePos, GCBlocks.OBSERVATORY_TELESCOPE);

        ObservatoryTelescopeBlockEntity earthScope = context.getBlockEntity(earthScopePos);
        ObservatoryTelescopeBlockEntity blockedScope = context.getBlockEntity(blockedScopePos);
        ObservatoryTelescopeBlockEntity moonScope = context.getBlockEntity(moonScopePos);
        ObservatoryTelescopeBlockEntity marsScope = context.getBlockEntity(marsScopePos);

        // Verify dedicated telescope upgrade isolation (rejects superconducting coil!)
        if (earthScope.itemStorage().slot(ObservatoryTelescopeBlockEntity.OPTICS_SLOT)
                .getFilter().test(GCItems.SUPERCONDUCTING_COIL, DataComponentPatch.EMPTY)
                || !earthScope.itemStorage().slot(ObservatoryTelescopeBlockEntity.OPTICS_SLOT)
                .getFilter().test(GCItems.OPTICAL_ACHROMAT_LENS, DataComponentPatch.EMPTY)) {
            context.fail("Telescope optics bay must accept OPTICAL_ACHROMAT_LENS and reject SUPERCONDUCTING_COIL");
            return;
        }

        earthScope.setEnvironmentContextForTest(Level.OVERWORLD, null, true);
        earthScope.selectTargetIndex(0); // Moon
        earthScope.itemStorage().slot(ObservatoryTelescopeBlockEntity.OPTICS_SLOT).set(GCItems.OPTICAL_ACHROMAT_LENS, 1);
        earthScope.energyStorage().setEnergy(40_000L);

        blockedScope.setEnvironmentContextForTest(Level.OVERWORLD, null, true);
        blockedScope.energyStorage().setEnergy(40_000L);

        moonScope.setEnvironmentContextForTest(GCDimensions.MOON, moonBody, false);
        moonScope.selectTargetIndex(2); // Mars
        moonScope.itemStorage().slot(ObservatoryTelescopeBlockEntity.OPTICS_SLOT).set(GCItems.SAPPHIRE_CASSEGRAIN_MIRROR, 1);
        moonScope.energyStorage().setEnergy(40_000L);

        marsScope.setEnvironmentContextForTest(GCDimensions.MARS, marsBody, true);
        marsScope.selectTargetIndex(6); // Jupiter
        marsScope.itemStorage().slot(ObservatoryTelescopeBlockEntity.OPTICS_SLOT).set(GCItems.LONSDALEITE_PRIMARY_MIRROR, 1);
        marsScope.energyStorage().setEnergy(40_000L);

        context.runAfterDelay(90, () -> {
            if (blockedScope.getState().getStatus() != GCMachineStatuses.SKY_OBSTRUCTED) {
                context.fail("Obstructed telescope must report SKY_OBSTRUCTED, got " + blockedScope.getState().getStatus());
                return;
            }
            if (!state.isBodyMapped(GCCelestialBodies.MOON.location())
                    || !state.isBodyMapped(GCCelestialBodies.MARS.location())
                    || !state.isBodyMapped(GCCelestialBodies.JUPITER.location())) {
                context.fail("Telescope surveys must map Moon, Mars, and Jupiter");
                return;
            }
            if (!state.isUnlocked(ResearchCatalog.EARTH_LUNAR_EPHEMERIS)
                    || !state.isUnlocked(ResearchCatalog.LUNAR_DEEP_SKY_SURVEY)
                    || !state.isUnlocked(ResearchCatalog.OUTER_SYSTEM_EPHEMERIS)) {
                context.fail("Telescope surveys must unlock Earth, Lunar, and Outer System ephemeris nodes");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "inSituResearch", timeoutTicks = 140)
    public void researchWorkbenchZeroEnergyPrototypingTest(GameTestHelper context) {
        TeamResearchState state = ResearchManager.get(context.getLevel());

        BlockPos benchPos = new BlockPos(0, 1, 0);
        context.setBlock(benchPos, GCBlocks.RESEARCH_WORKBENCH);

        ResearchWorkbenchBlockEntity bench = context.getBlockEntity(benchPos);
        bench.itemStorage().slot(ResearchWorkbenchBlockEntity.SPECIMEN_SLOT).set(GCItems.RAW_NIOBIUM, 1);
        bench.itemStorage().slot(ResearchWorkbenchBlockEntity.CATALYST_SLOT).set(GCItems.SILICON, 1);
        bench.itemStorage().slot(ResearchWorkbenchBlockEntity.PAPER_SLOT).set(Items.PAPER, 1);
        bench.itemStorage().slot(ResearchWorkbenchBlockEntity.METROLOGY_UPGRADE_SLOT).set(GCItems.OPTICAL_BLUEPRINT_PROJECTOR, 1);

        // Verify interactive C2S blueprint seal completes immediately with 0 energy!
        boolean sealed = bench.sealCalibratedPrototype(context.getLevel(), 96);
        if (!sealed) {
            context.fail("Interactive sealCalibratedPrototype must complete immediately with valid inputs");
            return;
        }
        if (!state.isUnlocked(ResearchCatalog.EARTH_PROTOTYPING)) {
            context.fail("Research Workbench must unlock EARTH_PROTOTYPING with 0 energy");
            return;
        }
        if (bench.itemStorage().slot(ResearchWorkbenchBlockEntity.PROTOTYPE_OUTPUT_SLOT).getResource() != GCItems.BASIC_WAFER) {
            context.fail("Expected Research Workbench to produce prototype BASIC_WAFER");
            return;
        }
        if (state.getSciencePoints() < 15) {
            context.fail("Expected >= 15 Science Points (SP) from interactive blueprint seal, got " + state.getSciencePoints());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "inSituResearch", timeoutTicks = 160)
    public void geologicalAnalyzerAndVenusEnvironmentalChamberTest(GameTestHelper context) {
        TeamResearchState state = ResearchManager.get(context.getLevel());

        Registry<CelestialBody<?, ?>> bodies = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> moonBody = bodies.get(GCCelestialBodies.MOON);
        CelestialBody<?, ?> venusBody = bodies.get(GCCelestialBodies.VENUS);
        if (moonBody == null || venusBody == null) {
            context.fail("Missing Moon or Venus CelestialBody in registry");
            return;
        }

        BlockPos geoPos = new BlockPos(0, 1, 0);
        BlockPos venusEnvPos = new BlockPos(2, 1, 0);
        BlockPos earthEnvPos = new BlockPos(4, 1, 0);

        context.setBlock(geoPos, GCBlocks.GEOLOGICAL_ANALYZER);
        context.setBlock(venusEnvPos, GCBlocks.ENVIRONMENTAL_TEST_CHAMBER);
        context.setBlock(earthEnvPos, GCBlocks.ENVIRONMENTAL_TEST_CHAMBER);

        GeologicalAnalyzerBlockEntity geo = context.getBlockEntity(geoPos);
        geo.setEnvironmentContextForTest(GCDimensions.MOON, moonBody);
        geo.energyStorage().setEnergy(50_000L);
        geo.itemStorage().slot(GeologicalAnalyzerBlockEntity.SPECIMEN_SLOT).set(GCBlocks.MOON_ILMENITE_ORE.asItem(), 1);
        geo.itemStorage().slot(GeologicalAnalyzerBlockEntity.REAGENT_SLOT).set(GCItems.NIOBIUM_OXIDE, 1);
        geo.itemStorage().slot(GeologicalAnalyzerBlockEntity.DETECTOR_UPGRADE_SLOT).set(GCItems.QUADRUPOLE_MASS_FILTER, 1);
        geo.fluidStorage().slot(GeologicalAnalyzerBlockEntity.REAGENT_FLUID_TANK).set(GCFluids.SULFURIC_ACID, 500L * FluidUtil.MILLIBUCKET);

        EnvironmentalTestChamberBlockEntity venusEnv = context.getBlockEntity(venusEnvPos);
        venusEnv.setEnvironmentContextForTest(GCDimensions.VENUS, venusBody);
        venusEnv.energyStorage().setEnergy(50_000L);
        venusEnv.itemStorage().slot(EnvironmentalTestChamberBlockEntity.TEST_SPECIMEN_SLOT).set(GCItems.THERMAL_CLOTH, 1);

        EnvironmentalTestChamberBlockEntity earthEnv = context.getBlockEntity(earthEnvPos);
        earthEnv.energyStorage().setEnergy(50_000L);
        earthEnv.itemStorage().slot(EnvironmentalTestChamberBlockEntity.TEST_SPECIMEN_SLOT).set(Items.GLASS, 1);

        context.runAfterDelay(105, () -> {
            if (!state.isUnlocked(ResearchCatalog.LUNAR_HE3_THERMAL_DESORPTION)
                    || !state.hasMilestone(ResearchMilestone.LUNAR_HE3_SPECTRUM)) {
                context.fail("Geological Analyzer must unlock LUNAR_HE3_THERMAL_DESORPTION and LUNAR_HE3_SPECTRUM milestone");
                return;
            }
            if (geo.getLastAwardedPoints() < 100) {
                context.fail("Expected native Moon + reagent + acid + Quadrupole Filter boost (>=100 SP), got " + geo.getLastAwardedPoints());
                return;
            }
            if (geo.itemStorage().slot(GeologicalAnalyzerBlockEntity.OUTPUT_SLOT).getAmount() < 2) {
                context.fail("Quadrupole Mass Filter must double mineral byproduct yield to >= 2");
                return;
            }
            if (venusEnv.getEnvModeCode() != EnvironmentalTestChamberBlockEntity.ENV_MODE_VENUS_HYPERBARIC
                    || !state.hasMilestone(ResearchMilestone.VENUS_92ATM_CERTIFIED)) {
                context.fail("Venus Environmental Chamber must certify VENUS_92ATM_CERTIFIED milestone");
                return;
            }
            if (venusEnv.itemStorage().slot(EnvironmentalTestChamberBlockEntity.RESULT_SLOT).getResource() != GCItems.ISOTHERMAL_FABRIC) {
                context.fail("Venus Environmental Chamber should upgrade THERMAL_CLOTH specimen to ISOTHERMAL_FABRIC");
                return;
            }
            if (!state.isUnlocked(ResearchCatalog.EARTH_VACUUM_SEALING)) {
                context.fail("Earth Environmental Chamber must unlock EARTH_VACUUM_SEALING");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "inSituResearchRelay", timeoutTicks = 120)
    public void dataLinkRelayAndKspEconomyTest(GameTestHelper context) {
        TeamResearchState state = ResearchManager.get(context.getLevel());
        state.resetAll();

        BlockPos linkPos = new BlockPos(0, 1, 0);
        BlockPos termPos = new BlockPos(2, 1, 0);
        BlockPos benchPos = new BlockPos(4, 1, 0);

        context.setBlock(linkPos, GCBlocks.RESEARCH_DATA_LINK);
        context.setBlock(termPos, GCBlocks.AEROSPACE_RD_TERMINAL);
        context.setBlock(benchPos, GCBlocks.RESEARCH_WORKBENCH);

        ResearchDataLinkBlockEntity link = context.getBlockEntity(linkPos);
        link.energyStorage().setEnergy(25_000L);
        link.itemStorage().slot(ResearchDataLinkBlockEntity.FREQUENCY_MODULE_SLOT).set(GCItems.FREQUENCY_MODULE, 1);
        link.itemStorage().slot(ResearchDataLinkBlockEntity.RF_FEED_UPGRADE_SLOT).set(GCItems.DEEP_SPACE_KA_BAND_FEED, 1);

        AerospaceRdTerminalBlockEntity terminal = context.getBlockEntity(termPos);
        terminal.energyStorage().setEnergy(25_000L);
        terminal.itemStorage().slot(AerospaceRdTerminalBlockEntity.UPGRADE_SLOT).set(GCItems.QUANTUM_ARCHIVE_CORE, 1);

        context.runAfterDelay(55, () -> {
            if (link.getState().getStatus() != GCMachineStatuses.UPLINK_ACTIVE) {
                context.fail("Research Data Link must report UPLINK_ACTIVE, got " + link.getState().getStatus());
                return;
            }
            if (state.getNetworkBonusMultiplier() < 1.24) {
                context.fail("Deep-Space Ka-Band Feed must raise relay bonus to >= 1.25x, got " + state.getNetworkBonusMultiplier());
                return;
            }
            if (!state.isQuantumArchiveBoost()) {
                context.fail("Quantum Archive Core in R&D Terminal must activate quantumArchiveBoost");
                return;
            }

            // Test KSP diminishing returns (1st run 100%, 2nd run 50% with Quantum Core, 4th run 0%)
            var r1 = ResearchManager.recordScienceExperiment(context.getLevel(), "dim_test", "Dim Test", "Workbench", "Earth", ResearchDomain.STRUCTURES, 40, 20, 1.0F, null, null);
            var r2 = ResearchManager.recordScienceExperiment(context.getLevel(), "dim_test", "Dim Test", "Workbench", "Earth", ResearchDomain.STRUCTURES, 40, 20, 1.0F, null, null);
            var r3 = ResearchManager.recordScienceExperiment(context.getLevel(), "dim_test", "Dim Test", "Workbench", "Earth", ResearchDomain.STRUCTURES, 40, 20, 1.0F, null, null);
            var r4 = ResearchManager.recordScienceExperiment(context.getLevel(), "dim_test", "Dim Test", "Workbench", "Earth", ResearchDomain.STRUCTURES, 40, 20, 1.0F, null, null);
            if (r1.spAwarded() <= r2.spAwarded() || r2.spAwarded() <= r3.spAwarded() || r4.spAwarded() != 0) {
                context.fail("KSP diminishing returns failed: r1=" + r1.spAwarded() + " r2=" + r2.spAwarded() + " r3=" + r3.spAwarded() + " r4=" + r4.spAwarded());
                return;
            }

            // Test Hybrid KSP+ Terminal Unlock: SP + Domain + Milestone gating!
            // EARTH_LUNAR_EPHEMERIS requires 20 SP + 10 EMAG + MOON_OBSERVED milestone
            state.addSciencePoints(50);
            state.addDomainPoints(ResearchDomain.ELECTROMAGNETISM, 30);
            boolean blockedWithoutMilestone = ResearchManager.tryUnlockNodeFromTerminal(context.getLevel(), null, ResearchCatalog.EARTH_LUNAR_EPHEMERIS);
            if (blockedWithoutMilestone) {
                context.fail("EARTH_LUNAR_EPHEMERIS must NOT unlock before MOON_OBSERVED milestone is completed");
                return;
            }
            state.completeMilestone(ResearchMilestone.MOON_OBSERVED);
            boolean unlockedWithMilestone = ResearchManager.tryUnlockNodeFromTerminal(context.getLevel(), null, ResearchCatalog.EARTH_LUNAR_EPHEMERIS);
            if (!unlockedWithMilestone || !state.isUnlocked(ResearchCatalog.EARTH_LUNAR_EPHEMERIS)) {
                context.fail("EARTH_LUNAR_EPHEMERIS must unlock once SP, EMAG, and MOON_OBSERVED milestone are satisfied");
                return;
            }
            context.succeed();
        });
    }
}
