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

import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.machine.Helium3FusionReactorBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.IsotopicCentrifugeBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.RegolithThermalDesorberBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.util.FluidUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import team.reborn.energy.api.EnergyStorage;

public class Helium3ChainTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 260)
    public void thermalDesorberIlmeniteYieldTest(GameTestHelper context) {
        BlockPos ilmenitePos = new BlockPos(0, 1, 0);
        BlockPos turfPos = new BlockPos(2, 1, 0);
        BlockPos noPowerPos = new BlockPos(4, 1, 0);

        context.setBlock(ilmenitePos, GCBlocks.REGOLITH_THERMAL_DESORBER);
        context.setBlock(turfPos, GCBlocks.REGOLITH_THERMAL_DESORBER);
        context.setBlock(noPowerPos, GCBlocks.REGOLITH_THERMAL_DESORBER);

        RegolithThermalDesorberBlockEntity ilmeniteDesorber = context.getBlockEntity(ilmenitePos);
        RegolithThermalDesorberBlockEntity turfDesorber = context.getBlockEntity(turfPos);
        RegolithThermalDesorberBlockEntity noPowerDesorber = context.getBlockEntity(noPowerPos);

        // Pre-evacuate and pre-heat powered desorbers to target vacuum + temperature (>700C)
        ilmeniteDesorber.setVacuumPercent(RegolithThermalDesorberBlockEntity.TARGET_VACUUM);
        ilmeniteDesorber.setTemperature(RegolithThermalDesorberBlockEntity.TARGET_TEMPERATURE);
        ilmeniteDesorber.energyStorage().setEnergy(50_000L);
        ilmeniteDesorber.itemStorage().slot(RegolithThermalDesorberBlockEntity.INPUT_SLOT_1)
                .set(GCBlocks.MOON_ILMENITE_ORE.asItem(), 1);

        turfDesorber.setVacuumPercent(RegolithThermalDesorberBlockEntity.TARGET_VACUUM);
        turfDesorber.setTemperature(RegolithThermalDesorberBlockEntity.TARGET_TEMPERATURE);
        turfDesorber.energyStorage().setEnergy(50_000L);
        turfDesorber.itemStorage().slot(RegolithThermalDesorberBlockEntity.INPUT_SLOT_1)
                .set(GCBlocks.MOON_TURF.asItem(), 1);

        // Unpowered desorber must NOT consume input or emit gas (transactional safety)
        noPowerDesorber.setVacuumPercent(RegolithThermalDesorberBlockEntity.TARGET_VACUUM);
        noPowerDesorber.setTemperature(RegolithThermalDesorberBlockEntity.TARGET_TEMPERATURE);
        noPowerDesorber.energyStorage().setEnergy(0L);
        noPowerDesorber.itemStorage().slot(RegolithThermalDesorberBlockEntity.INPUT_SLOT_1)
                .set(GCBlocks.MOON_ILMENITE_ORE.asItem(), 1);

        context.runAfterDelay(135, () -> {
            FluidResourceSlot ilmeniteTank = ilmeniteDesorber.fluidStorage().slot(RegolithThermalDesorberBlockEntity.GAS_TANK);
            FluidResourceSlot turfTank = turfDesorber.fluidStorage().slot(RegolithThermalDesorberBlockEntity.GAS_TANK);
            FluidResourceSlot noPowerTank = noPowerDesorber.fluidStorage().slot(RegolithThermalDesorberBlockEntity.GAS_TANK);

            long expectedIlmenite = 1000L * FluidUtil.MILLIBUCKET;
            long expectedTurf = 100L * FluidUtil.MILLIBUCKET;

            if (ilmeniteTank.getResource() != Gases.SOLAR_VOLATILE_GAS || ilmeniteTank.getAmount() != expectedIlmenite) {
                context.fail("Expected MOON_ILMENITE_ORE to yield 1000mB SOLAR_VOLATILE_GAS, got " + ilmeniteTank.getAmount() + " of " + ilmeniteTank.getResource());
                return;
            }
            if (turfTank.getResource() != Gases.SOLAR_VOLATILE_GAS || turfTank.getAmount() != expectedTurf) {
                context.fail("Expected MOON_TURF to yield 100mB SOLAR_VOLATILE_GAS, got " + turfTank.getAmount() + " of " + turfTank.getResource());
                return;
            }
            if (ilmeniteTank.getAmount() != turfTank.getAmount() * 10L) {
                context.fail("Expected MOON_ILMENITE_ORE yield to be exactly 10x MOON_TURF yield");
                return;
            }

            ItemResourceSlot byproduct1 = ilmeniteDesorber.itemStorage().slot(RegolithThermalDesorberBlockEntity.BYPRODUCT_SLOT_1);
            if (byproduct1.isEmpty() || byproduct1.getResource() != GCItems.RAW_TITANIUM) {
                context.fail("Expected RAW_TITANIUM byproduct from MOON_ILMENITE_ORE, got " + byproduct1.getResource());
                return;
            }

            ItemResourceSlot noPowerInput = noPowerDesorber.itemStorage().slot(RegolithThermalDesorberBlockEntity.INPUT_SLOT_1);
            if (noPowerInput.getAmount() != 1 || !noPowerTank.isEmpty()) {
                context.fail("Unpowered desorber must roll back and not consume input or produce gas");
                return;
            }

            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void isotopicHelium3SeparationTest(GameTestHelper context) {
        BlockPos withCoilPos = new BlockPos(0, 1, 0);
        BlockPos withoutCoilPos = new BlockPos(2, 1, 0);

        context.setBlock(withCoilPos, GCBlocks.ISOTOPIC_CENTRIFUGE);
        context.setBlock(withoutCoilPos, GCBlocks.ISOTOPIC_CENTRIFUGE);

        IsotopicCentrifugeBlockEntity centrifuge = context.getBlockEntity(withCoilPos);
        IsotopicCentrifugeBlockEntity noCoilCentrifuge = context.getBlockEntity(withoutCoilPos);

        centrifuge.energyStorage().setEnergy(50_000L);
        centrifuge.itemStorage().slot(IsotopicCentrifugeBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        centrifuge.fluidStorage().slot(IsotopicCentrifugeBlockEntity.INPUT_GAS_TANK)
                .set(Gases.SOLAR_VOLATILE_GAS, 500L * FluidUtil.MILLIBUCKET);

        noCoilCentrifuge.energyStorage().setEnergy(50_000L);
        noCoilCentrifuge.fluidStorage().slot(IsotopicCentrifugeBlockEntity.INPUT_GAS_TANK)
                .set(Gases.SOLAR_VOLATILE_GAS, 500L * FluidUtil.MILLIBUCKET);

        // Allow 40 ticks for rotor spin-up (40k RPM) + isotopic separation cycles
        context.runAfterDelay(45, () -> {
            FluidResourceSlot outTank = centrifuge.fluidStorage().slot(IsotopicCentrifugeBlockEntity.OUTPUT_GAS_TANK);
            FluidResourceSlot tailingsTank = centrifuge.fluidStorage().slot(IsotopicCentrifugeBlockEntity.TAILINGS_GAS_TANK);
            FluidResourceSlot inTank = centrifuge.fluidStorage().slot(IsotopicCentrifugeBlockEntity.INPUT_GAS_TANK);

            if (centrifuge.getRotorRpm() < IsotopicCentrifugeBlockEntity.TARGET_RPM) {
                context.fail("Expected Isotopic Centrifuge rotor to spin up >= 40000 RPM, got " + centrifuge.getRotorRpm());
                return;
            }
            if (outTank.isEmpty() || outTank.getResource() != Gases.HELIUM_3) {
                context.fail("Expected Isotopic Centrifuge with SUPERCONDUCTING_COIL to produce HELIUM_3 gas, got " + outTank.getResource() + " (" + outTank.getAmount() + ")");
                return;
            }
            if (tailingsTank.isEmpty() || tailingsTank.getResource() != Gases.HELIUM) {
                context.fail("Expected Isotopic Centrifuge to separate 4He tailings into secondary tank, got " + tailingsTank.getResource());
                return;
            }
            if (inTank.getAmount() >= 500L * FluidUtil.MILLIBUCKET) {
                context.fail("Expected Isotopic Centrifuge to consume SOLAR_VOLATILE_GAS");
                return;
            }
            if (!noCoilCentrifuge.fluidStorage().slot(IsotopicCentrifugeBlockEntity.OUTPUT_GAS_TANK).isEmpty()) {
                context.fail("Isotopic Centrifuge without SUPERCONDUCTING_COIL must not produce HELIUM_3");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void aneutronicFusionReactorMhdTest(GameTestHelper context) {
        // Standalone controller (no multiblock) must refuse to ignite
        BlockPos standaloneRel = new BlockPos(0, 2, 0);
        context.setBlock(standaloneRel, GCBlocks.HELIUM3_FUSION_REACTOR);
        Helium3FusionReactorBlockEntity standaloneReactor = context.getBlockEntity(standaloneRel);
        standaloneReactor.itemStorage().slot(Helium3FusionReactorBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        standaloneReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.HELIUM3_TANK).set(Gases.HELIUM_3, 1000L * FluidUtil.MILLIBUCKET);
        standaloneReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.DEUTERIUM_TANK).set(Gases.DEUTERIUM, 1000L * FluidUtil.MILLIBUCKET);

        // Two complete 3x3x3 toroidal multiblocks (baseline vs <2K cryo)
        BlockPos baseRel = new BlockPos(4, 2, 1);
        BlockPos cryoRel = new BlockPos(8, 2, 1);
        Helium3FusionReactorBlockEntity.buildMultiblock(context.getLevel(), context.absolutePos(baseRel), Direction.NORTH);
        Helium3FusionReactorBlockEntity.buildMultiblock(context.getLevel(), context.absolutePos(cryoRel), Direction.NORTH);

        Helium3FusionReactorBlockEntity baseReactor = context.getBlockEntity(baseRel);
        Helium3FusionReactorBlockEntity cryoReactor = context.getBlockEntity(cryoRel);

        // Baseline MHD reactor: 3x3x3 torus + D2 + 3He + SUPERCONDUCTING_COIL -> 2,500 GJ/t
        baseReactor.itemStorage().slot(Helium3FusionReactorBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        baseReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.HELIUM3_TANK).set(Gases.HELIUM_3, 1000L * FluidUtil.MILLIBUCKET);
        baseReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.DEUTERIUM_TANK).set(Gases.DEUTERIUM, 1000L * FluidUtil.MILLIBUCKET);

        // Cryo <2K MHD reactor: 3x3x3 torus + D2 + 3He + SUPERCONDUCTING_COIL + HELIUM3_CELL -> 5,000 GJ/t
        cryoReactor.itemStorage().slot(Helium3FusionReactorBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        cryoReactor.itemStorage().slot(Helium3FusionReactorBlockEntity.CRYO_COOLANT_SLOT).set(GCItems.HELIUM3_CELL, 1);
        cryoReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.HELIUM3_TANK).set(Gases.HELIUM_3, 1000L * FluidUtil.MILLIBUCKET);
        cryoReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.DEUTERIUM_TANK).set(Gases.DEUTERIUM, 1000L * FluidUtil.MILLIBUCKET);

        // Wait 30 ticks for 150 MK plasma ignition ramp + active MHD burn
        context.runAfterDelay(30, () -> {
            if (standaloneReactor.isMultiblockFormed() || standaloneReactor.getActiveGenerationRate() > 0) {
                context.fail("Standalone Fusion Reactor without 3x3x3 multiblock must NOT ignite or generate energy");
                return;
            }
            if (!baseReactor.isMultiblockFormed() || !cryoReactor.isMultiblockFormed()) {
                context.fail("Expected 3x3x3 Fusion Reactor multiblocks to form cleanly (26/26)");
                return;
            }
            if (baseReactor.getActiveGenerationRate() < Helium3FusionReactorBlockEntity.BASE_GENERATION_RATE) {
                context.fail("Expected baseline MHD fusion output >= 2500 GJ/t, got " + baseReactor.getActiveGenerationRate());
                return;
            }
            if (cryoReactor.getActiveGenerationRate() < Helium3FusionReactorBlockEntity.CRYO_GENERATION_RATE || !cryoReactor.isCryoCooled()) {
                context.fail("Expected <2K cryo MHD fusion output >= 5000 GJ/t, got " + cryoReactor.getActiveGenerationRate());
                return;
            }
            if (cryoReactor.energyStorage().getAmount() <= baseReactor.energyStorage().getAmount()) {
                context.fail("Expected <2K cryo reactor to accumulate more energy than baseline reactor");
                return;
            }

            // Trigger magnetic quench-safe shutdown by removing coil mid-burn
            cryoReactor.itemStorage().slot(Helium3FusionReactorBlockEntity.COIL_SLOT).extractOne();

            // Verify reactor does NOT spawn free batteries out of thin air
            if (!cryoReactor.itemStorage().slot(Helium3FusionReactorBlockEntity.OUTPUT_CELL_SLOT).isEmpty()) {
                context.fail("Fusion Reactor must NOT synthesize free HELIUM3_FUSION_CELL batteries out of thin air");
                return;
            }

            context.runAfterDelay(5, () -> {
                context.assertBlockPresent(GCBlocks.HELIUM3_FUSION_REACTOR, cryoRel);
                if (cryoReactor.getActiveGenerationRate() != 0) {
                    context.fail("Expected quench-safe shutdown to drop generation to 0 GJ/t without explosion, got " + cryoReactor.getActiveGenerationRate());
                    return;
                }
                context.succeed();
            });
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void pipesAndDeuteriumExtractionWorkTest(GameTestHelper context) {
        BlockPos desorberPos = new BlockPos(0, 1, 0);
        BlockPos centrifugePos = new BlockPos(2, 1, 0);
        BlockPos reactorPos = new BlockPos(4, 1, 0);

        context.setBlock(desorberPos, GCBlocks.REGOLITH_THERMAL_DESORBER);
        context.setBlock(centrifugePos, GCBlocks.ISOTOPIC_CENTRIFUGE);
        context.setBlock(reactorPos, GCBlocks.HELIUM3_FUSION_REACTOR);

        // Configure fluid I/O on TOP and energy I/O on BOTTOM, then verify FluidStorage.SIDED & EnergyStorage.SIDED lookups
        for (BlockPos rel : new BlockPos[]{desorberPos, centrifugePos, reactorPos}) {
            MachineBlockEntity machine = context.getBlockEntity(rel);
            machine.getIOConfig().get(BlockFace.TOP).setOption(ResourceType.FLUID, ResourceFlow.BOTH);
            machine.getIOConfig().get(BlockFace.BOTTOM).setOption(ResourceType.ENERGY, ResourceFlow.BOTH);
            BlockPos abs = context.absolutePos(rel);
            if (FluidStorage.SIDED.find(context.getLevel(), abs, Direction.UP) == null) {
                context.fail("FluidStorage.SIDED missing for machine at " + rel + " — Glass Fluid Pipes will not connect!");
                return;
            }
            if (EnergyStorage.SIDED.find(context.getLevel(), abs, Direction.DOWN) == null) {
                context.fail("EnergyStorage.SIDED missing for machine at " + rel + " — Aluminum Wires will not connect!");
                return;
            }
        }

        // Verify DENSE_ICE -> DEUTERIUM extraction in Isotopic Centrifuge
        IsotopicCentrifugeBlockEntity centrifuge = context.getBlockEntity(centrifugePos);
        centrifuge.setRotorRpm(IsotopicCentrifugeBlockEntity.TARGET_RPM);
        centrifuge.energyStorage().setEnergy(50_000L);
        centrifuge.itemStorage().slot(IsotopicCentrifugeBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        centrifuge.itemStorage().slot(IsotopicCentrifugeBlockEntity.SOLID_INPUT_SLOT).set(GCBlocks.DENSE_ICE.asItem(), 1);

        context.runAfterDelay(70, () -> {
            FluidResourceSlot outTank = centrifuge.fluidStorage().slot(IsotopicCentrifugeBlockEntity.OUTPUT_GAS_TANK);
            if (outTank.getResource() != Gases.DEUTERIUM || outTank.getAmount() != 250L * FluidUtil.MILLIBUCKET) {
                context.fail("Expected DENSE_ICE in Isotopic Centrifuge to yield 250mB DEUTERIUM, got " + outTank.getAmount() + " of " + outTank.getResource());
                return;
            }
            context.succeed();
        });
    }
}
