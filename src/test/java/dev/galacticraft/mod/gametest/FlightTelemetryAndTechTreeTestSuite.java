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

import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.component.OrbitalSurveyData;
import dev.galacticraft.api.component.TelemetryData;
import dev.galacticraft.api.registry.AddonRegistries;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.block.entity.machine.AerospaceRdTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.TelemetryAnalyzerBlockEntity;
import dev.galacticraft.mod.content.entity.orbital.FlightTelemetryEngine;
import dev.galacticraft.mod.content.entity.orbital.ReactiveSampleContainmentHandler;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchDomain;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.research.ResearchMilestone;
import dev.galacticraft.mod.research.ResearchNode;
import dev.galacticraft.mod.research.ResearchRecipeGate;
import dev.galacticraft.mod.research.TeamResearchState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Automated GameTest suite for Plan 11 — Flight Telemetry ({@code science_*} modules),
 * {@code DATA_DRIVE} components, {@code TELEMETRY_ANALYZER}, data-driven Research Tree Codec,
 * and soft recipe gating linked to {@code AEROSPACE_RD_TERMINAL}.
 */
public class FlightTelemetryAndTechTreeTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE, batch = "flightTelemetry", timeoutTicks = 100)
    public void marsSpectrometerCapturesRealGasCompositionTest(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> bodies = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> marsBody = bodies.get(GCCelestialBodies.MARS);
        if (marsBody == null) {
            context.fail("Missing Mars CelestialBody in registry");
            return;
        }

        ItemStack blankDrive = new ItemStack(GCItems.BLANK_DATA_DRIVE);
        var result = FlightTelemetryEngine.executeProbeMission(
                context.getLevel(),
                PlanetaryEnvironment.MARS,
                marsBody,
                List.of(FlightTelemetryEngine.MOD_SPECTROMETER),
                blankDrive,
                false,
                new BlockPos(128, 70, -64)
        );

        if (!result.hasRecordedDrive() || !result.recordedDrive().is(GCItems.RECORDED_DATA_DRIVE)) {
            context.fail("Spectrometer probe mission to Mars must output a RECORDED_DATA_DRIVE");
            return;
        }
        TelemetryData data = result.recordedDrive().get(GCDataComponents.TELEMETRY_DATA);
        if (data == null) {
            context.fail("RECORDED_DATA_DRIVE missing GCDataComponents.TELEMETRY_DATA");
            return;
        }
        if (!data.planetId().equals(GCCelestialBodies.MARS.location())
                || data.eventType() != TelemetryData.EventType.ATMOSPHERE_ENTRY) {
            context.fail("Expected Mars ATMOSPHERE_ENTRY telemetry, got " + data.planetId() + " / " + data.eventType());
            return;
        }
        if (!data.gasCompositionSummary().contains("CO2")) {
            context.fail("Mars atmospheric spectrometer must capture real CO2 gas composition, got: " + data.gasCompositionSummary());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "flightTelemetry", timeoutTicks = 100)
    public void flightRecorderThermalFailureGrants200PercentBonusTest(GameTestHelper context) {
        ItemStack nominalDrive = FlightTelemetryEngine.createRecordedDataDrive(
                PlanetaryEnvironment.VENUS,
                null,
                FlightTelemetryEngine.MOD_FLIGHT_RECORDER,
                TelemetryData.EventType.ASCENT_MAX_Q,
                null
        );
        var failureResult = FlightTelemetryEngine.executeProbeMission(
                context.getLevel(),
                PlanetaryEnvironment.VENUS,
                null,
                List.of(FlightTelemetryEngine.MOD_FLIGHT_RECORDER),
                new ItemStack(GCItems.BLANK_DATA_DRIVE),
                true,
                BlockPos.ZERO
        );

        TelemetryData nominal = nominalDrive.get(GCDataComponents.TELEMETRY_DATA);
        TelemetryData disaster = failureResult.recordedDrive().get(GCDataComponents.TELEMETRY_DATA);
        if (nominal == null || disaster == null) {
            context.fail("Expected valid TelemetryData on both nominal and disaster drives");
            return;
        }
        if (!disaster.isThermalFailureBlackBox() || disaster.qualityPct() != 300) {
            context.fail("Thermal failure black box must set THERMAL_FAILURE and qualityPct=300 (+200%), got "
                    + disaster.eventType() + " / " + disaster.qualityPct());
            return;
        }
        if (disaster.sciencePoints() < nominal.sciencePoints() * 3
                || disaster.domainPoints() < nominal.domainPoints() * 3) {
            context.fail("Thermal failure must grant +200% (3x) SP & Domain points: nominal="
                    + nominal.sciencePoints() + "/" + nominal.domainPoints()
                    + " vs disaster=" + disaster.sciencePoints() + "/" + disaster.domainPoints());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "flightTelemetry", timeoutTicks = 120)
    public void allScienceModulesCapabilitiesTest(GameTestHelper context) {
        // 1. Multispectral Camera generates ORBITAL_SURVEY_MAP with real coordinates
        ItemStack map = FlightTelemetryEngine.createOrbitalSurveyMap(context.getLevel(), PlanetaryEnvironment.MARS, new BlockPos(32, 64, 32));
        OrbitalSurveyData survey = map.get(GCDataComponents.ORBITAL_SURVEY_DATA);
        if (!map.is(GCItems.ORBITAL_SURVEY_MAP) || survey == null || survey.oreLabel().isEmpty()) {
            context.fail("Multispectral camera must generate ORBITAL_SURVEY_MAP with valid OrbitalSurveyData");
            return;
        }

        // 2. ISRU Harvester harvests Mars RAW_DESH automatically
        ItemStack harvested = FlightTelemetryEngine.harvestIsruSurfaceSample(PlanetaryEnvironment.MARS);
        if (!harvested.is(GCItems.RAW_DESH) || harvested.getCount() < 1) {
            context.fail("ISRU Harvester on Mars must collect RAW_DESH, got " + harvested);
            return;
        }

        // 3. Orbital Laboratory works in 0.0G (Asteroids) and refuses Earth (1.0G)
        ItemStack earthLab = FlightTelemetryEngine.runOrbitalLaboratoryCycle(context.getLevel(), PlanetaryEnvironment.EARTH, null);
        ItemStack zeroGLab = FlightTelemetryEngine.runOrbitalLaboratoryCycle(context.getLevel(), PlanetaryEnvironment.ASTEROIDS, null);
        if (!earthLab.isEmpty() || !zeroGLab.is(GCItems.RECORDED_DATA_DRIVE)) {
            context.fail("Orbital Laboratory must refuse Earth 1.0G and produce 0G telemetry drive in Asteroids");
            return;
        }

        // 4. Radiation Counter on Jupiter & Magnetometer Boom
        ItemStack radDrive = FlightTelemetryEngine.createRecordedDataDrive(
                PlanetaryEnvironment.JUPITER, null, FlightTelemetryEngine.MOD_RADIATION_COUNTER,
                TelemetryData.EventType.RADIATION_BELT, null);
        ItemStack magDrive = FlightTelemetryEngine.createRecordedDataDrive(
                PlanetaryEnvironment.SATURN, null, FlightTelemetryEngine.MOD_MAGNETOMETER,
                TelemetryData.EventType.MAGNETOSPHERE, null);
        TelemetryData rad = radDrive.get(GCDataComponents.TELEMETRY_DATA);
        TelemetryData mag = magDrive.get(GCDataComponents.TELEMETRY_DATA);
        if (rad == null || !ResearchDomain.NUCLEAR_PLASMA.id().equals(rad.primaryDomainId())
                || mag == null || !ResearchDomain.ELECTROMAGNETISM.id().equals(mag.primaryDomainId())) {
            context.fail("Radiation counter must yield NUCL and Magnetometer boom must yield EMAG");
            return;
        }

        // 5. Reactive sample detection for Sample Containment Bay
        if (!ReactiveSampleContainmentHandler.isReactiveSample(new ItemStack(GCItems.METALLIC_HYDROGEN_CELL))
                || !ReactiveSampleContainmentHandler.isReactiveSample(new ItemStack(GCItems.SUPERIONIC_ICE_CRYSTAL))
                || !ReactiveSampleContainmentHandler.isReactiveSample(new ItemStack(GCItems.SULFURIC_ACID_BUCKET))
                || ReactiveSampleContainmentHandler.isReactiveSample(new ItemStack(GCItems.RAW_DESH))) {
            context.fail("ReactiveSampleContainmentHandler classification mismatch");
            return;
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "flightTelemetryAnalyzer", timeoutTicks = 160)
    public void telemetryAnalyzerAndTechTreeUnlockAndSoftGatingTest(GameTestHelper context) {
        TeamResearchState state = ResearchManager.get(context.getLevel());
        state.resetAll();

        // Verify soft recipe gating: Earth basics always unlocked, Era 3 Sabatier Reactor softly gated!
        if (!ResearchRecipeGate.isRecipeUnlocked(context.getLevel(), null, Constant.id("compressor"), new ItemStack(GCBlocks.COMPRESSOR))) {
            context.fail("Basic Earth machine COMPRESSOR must remain ungated");
            return;
        }
        if (ResearchRecipeGate.isRecipeUnlocked(context.getLevel(), null, Constant.id("sabatier_chemical_reactor"), new ItemStack(GCBlocks.SABATIER_CHEMICAL_REACTOR))) {
            context.fail("Era 3 SABATIER_CHEMICAL_REACTOR must be softly gated until MARTIAN_SABATIER_ISRU is unlocked");
            return;
        }

        // Verify ResearchNode.CODEC round-trip preserves unlocked_recipes
        ResearchNode sabatierNode = ResearchCatalog.get(ResearchCatalog.MARTIAN_SABATIER_ISRU);
        if (sabatierNode == null || sabatierNode.unlockedRecipes().isEmpty()) {
            context.fail("MARTIAN_SABATIER_ISRU node must carry data-driven unlockedRecipes");
            return;
        }
        var encoded = ResearchNode.CODEC.encodeStart(JsonOps.INSTANCE, sabatierNode).getOrThrow();
        ResearchNode decoded = ResearchNode.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        if (!decoded.equals(sabatierNode)) {
            context.fail("ResearchNode.CODEC round-trip failed");
            return;
        }

        // Place Telemetry Analyzer & Aerospace R&D Terminal
        BlockPos analyzerPos = new BlockPos(0, 1, 0);
        BlockPos terminalPos = new BlockPos(2, 1, 0);
        context.setBlock(analyzerPos, GCBlocks.TELEMETRY_ANALYZER);
        context.setBlock(terminalPos, GCBlocks.AEROSPACE_RD_TERMINAL);

        TelemetryAnalyzerBlockEntity analyzer = context.getBlockEntity(analyzerPos);
        AerospaceRdTerminalBlockEntity terminal = context.getBlockEntity(terminalPos);

        // Verify BLANK_SCHEMATIC_DISC is accepted by AerospaceRdTerminal media slot
        if (!terminal.itemStorage().slot(AerospaceRdTerminalBlockEntity.SCHEMATIC_MEDIA_SLOT)
                .getFilter().test(GCItems.BLANK_SCHEMATIC_DISC, DataComponentPatch.EMPTY)) {
            context.fail("Aerospace R&D Terminal must accept BLANK_SCHEMATIC_DISC in schematic media slot");
            return;
        }

        ItemStack marsDrive = FlightTelemetryEngine.createRecordedDataDrive(
                PlanetaryEnvironment.MARS,
                null,
                FlightTelemetryEngine.MOD_SPECTROMETER,
                TelemetryData.EventType.ATMOSPHERE_ENTRY,
                "Mars CO2 entry telemetry"
        );
        analyzer.energyStorage().setEnergy(40_000L);
        analyzer.itemStorage().slot(TelemetryAnalyzerBlockEntity.DATA_DRIVE_SLOT)
                .insert(marsDrive.getItem(), marsDrive.getComponentsPatch(), 1L);
        analyzer.itemStorage().slot(TelemetryAnalyzerBlockEntity.SAMPLE_SLOT).set(GCItems.RAW_DESH, 1);
        analyzer.itemStorage().slot(TelemetryAnalyzerBlockEntity.REAGENT_SLOT).set(GCItems.NIOBIUM_OXIDE, 1);

        context.runAfterDelay(90, () -> {
            if (analyzer.getLastSpAwarded() <= 0 || analyzer.getLastDomainAwarded() <= 0) {
                context.fail("Telemetry Analyzer must award SP and Domain points after processing Mars drive + sample + reagent");
                return;
            }
            if (analyzer.itemStorage().slot(TelemetryAnalyzerBlockEntity.OUTPUT_SLOT).getResource() != GCItems.BLANK_DATA_DRIVE) {
                context.fail("Telemetry Analyzer must output recycled BLANK_DATA_DRIVE");
                return;
            }
            if (!state.hasMilestone(ResearchMilestone.FLIGHT_TELEMETRY_ANALYZED)
                    || !state.hasMilestone(ResearchMilestone.ATMOSPHERIC_SPECTRUM_CAPTURED)) {
                context.fail("Telemetry Analyzer must complete FLIGHT_TELEMETRY_ANALYZED & ATMOSPHERIC_SPECTRUM_CAPTURED milestones");
                return;
            }
            if (state.getDomainPoints(ResearchDomain.CRYOGENICS) <= 0) {
                context.fail("Mars spectrometer analysis must grant CRYOGENICS domain points");
                return;
            }

            // Unlock EARTH_PROTOTYPING from Aerospace R&D Terminal using earned SP
            boolean unlocked = ResearchManager.tryUnlockNodeFromTerminal(context.getLevel(), null, ResearchCatalog.EARTH_PROTOTYPING);
            if (!unlocked || !state.isUnlocked(ResearchCatalog.EARTH_PROTOTYPING)) {
                context.fail("Expected earned telemetry SP (" + state.getSciencePoints() + ") to unlock EARTH_PROTOTYPING in R&D Terminal");
                return;
            }

            // Unlock MARTIAN_SABATIER_ISRU and confirm soft recipe gating releases SABATIER_CHEMICAL_REACTOR!
            state.unlockNode(ResearchCatalog.MARTIAN_SABATIER_ISRU);
            if (!ResearchRecipeGate.isRecipeUnlocked(context.getLevel(), null, Constant.id("sabatier_chemical_reactor"), new ItemStack(GCBlocks.SABATIER_CHEMICAL_REACTOR))) {
                context.fail("SABATIER_CHEMICAL_REACTOR must unlock immediately once MARTIAN_SABATIER_ISRU is researched");
                return;
            }

            context.succeed();
        });
    }
}
