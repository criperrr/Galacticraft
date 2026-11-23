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
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.machine.Helium3FusionReactorBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.IsotopicCentrifugeBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.RegolithThermalDesorberBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.util.FluidUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

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

        // Pre-heat powered desorbers to target temperature (>700C) and supply energy
        ilmeniteDesorber.setTemperature(RegolithThermalDesorberBlockEntity.TARGET_TEMPERATURE);
        ilmeniteDesorber.energyStorage().setEnergy(50_000L);
        ilmeniteDesorber.itemStorage().slot(RegolithThermalDesorberBlockEntity.INPUT_SLOT_1)
                .set(GCBlocks.MOON_ILMENITE_ORE.asItem(), 1);

        turfDesorber.setTemperature(RegolithThermalDesorberBlockEntity.TARGET_TEMPERATURE);
        turfDesorber.energyStorage().setEnergy(50_000L);
        turfDesorber.itemStorage().slot(RegolithThermalDesorberBlockEntity.INPUT_SLOT_1)
                .set(GCBlocks.MOON_TURF.asItem(), 1);

        // Unpowered desorber must NOT consume input or emit gas (transactional safety)
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

        context.runAfterDelay(30, () -> {
            FluidResourceSlot outTank = centrifuge.fluidStorage().slot(IsotopicCentrifugeBlockEntity.OUTPUT_GAS_TANK);
            FluidResourceSlot inTank = centrifuge.fluidStorage().slot(IsotopicCentrifugeBlockEntity.INPUT_GAS_TANK);
            if (outTank.isEmpty() || outTank.getResource() != Gases.HELIUM_3) {
                context.fail("Expected Isotopic Centrifuge with SUPERCONDUCTING_COIL to produce HELIUM_3 gas, got " + outTank.getResource() + " (" + outTank.getAmount() + ")");
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

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 160)
    public void aneutronicFusionReactorMhdTest(GameTestHelper context) {
        BlockPos basePos = new BlockPos(0, 1, 0);
        BlockPos cryoPos = new BlockPos(2, 1, 0);

        context.setBlock(basePos, GCBlocks.HELIUM3_FUSION_REACTOR);
        context.setBlock(cryoPos, GCBlocks.HELIUM3_FUSION_REACTOR);

        Helium3FusionReactorBlockEntity baseReactor = context.getBlockEntity(basePos);
        Helium3FusionReactorBlockEntity cryoReactor = context.getBlockEntity(cryoPos);

        // Baseline MHD reactor: D2 + 3He + SUPERCONDUCTING_COIL -> 2,500 GJ/t
        baseReactor.itemStorage().slot(Helium3FusionReactorBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        baseReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.HELIUM3_TANK).set(Gases.HELIUM_3, 1000L * FluidUtil.MILLIBUCKET);
        baseReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.DEUTERIUM_TANK).set(Gases.DEUTERIUM, 1000L * FluidUtil.MILLIBUCKET);

        // Cryo <2K MHD reactor: D2 + 3He + SUPERCONDUCTING_COIL + HELIUM3_CELL -> 5,000 GJ/t
        cryoReactor.itemStorage().slot(Helium3FusionReactorBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        cryoReactor.itemStorage().slot(Helium3FusionReactorBlockEntity.CRYO_COOLANT_SLOT).set(GCItems.HELIUM3_CELL, 1);
        cryoReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.HELIUM3_TANK).set(Gases.HELIUM_3, 1000L * FluidUtil.MILLIBUCKET);
        cryoReactor.fluidStorage().slot(Helium3FusionReactorBlockEntity.DEUTERIUM_TANK).set(Gases.DEUTERIUM, 1000L * FluidUtil.MILLIBUCKET);

        context.runAfterDelay(20, () -> {
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

            context.runAfterDelay(5, () -> {
                context.assertBlockPresent(GCBlocks.HELIUM3_FUSION_REACTOR, cryoPos);
                if (cryoReactor.getActiveGenerationRate() != 0) {
                    context.fail("Expected quench-safe shutdown to drop generation to 0 GJ/t without explosion, got " + cryoReactor.getActiveGenerationRate());
                    return;
                }
                context.succeed();
            });
        });
    }
}
