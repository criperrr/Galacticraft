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

import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile.SlotRole;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.machine.ChemicalLeachingPlantBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.CompositeAutoclaveBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.VacuumMetallurgicInfuserBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.rocket.part.RocketMaterialRegistry;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.SchematicGridLayout;
import dev.galacticraft.mod.util.FluidUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import team.reborn.energy.api.EnergyStorage;

public class MetallurgicalCompositeMachinesTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void chemicalLeachingTransactionRollbackWithoutEnergyTest(GameTestHelper context) {
        BlockPos zeroPowerPos = new BlockPos(0, 1, 0);
        BlockPos partialPowerPos = new BlockPos(2, 1, 0);

        context.setBlock(zeroPowerPos, GCBlocks.CHEMICAL_LEACHING_PLANT);
        context.setBlock(partialPowerPos, GCBlocks.CHEMICAL_LEACHING_PLANT);

        ChemicalLeachingPlantBlockEntity zeroPowerPlant = context.getBlockEntity(zeroPowerPos);
        ChemicalLeachingPlantBlockEntity partialPowerPlant = context.getBlockEntity(partialPowerPos);

        long initialAcid = 1000L * FluidUtil.MILLIBUCKET;
        long partialEnergy = ChemicalLeachingPlantBlockEntity.ENERGY_CONSUMPTION / 2L; // 45 GJ < 90 GJ/t

        zeroPowerPlant.energyStorage().setEnergy(0L);
        zeroPowerPlant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.ORE_INPUT_SLOT).set(GCItems.RAW_NIOBIUM, 2);
        zeroPowerPlant.fluidStorage().slot(ChemicalLeachingPlantBlockEntity.ACID_TANK).set(GCFluids.SULFURIC_ACID, initialAcid);

        partialPowerPlant.energyStorage().setEnergy(partialEnergy);
        partialPowerPlant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.ORE_INPUT_SLOT).set(GCItems.RAW_NIOBIUM, 2);
        partialPowerPlant.fluidStorage().slot(ChemicalLeachingPlantBlockEntity.ACID_TANK).set(GCFluids.SULFURIC_ACID, initialAcid);

        context.runAfterDelay(30, () -> {
            for (ChemicalLeachingPlantBlockEntity plant : new ChemicalLeachingPlantBlockEntity[]{zeroPowerPlant, partialPowerPlant}) {
                ItemResourceSlot ore = plant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.ORE_INPUT_SLOT);
                FluidResourceSlot tank = plant.fluidStorage().slot(ChemicalLeachingPlantBlockEntity.ACID_TANK);
                ItemResourceSlot out = plant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.PRIMARY_OUTPUT_SLOT);
                ItemResourceSlot byp = plant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.BYPRODUCT_OUTPUT_SLOT);

                if (ore.getResource() != GCItems.RAW_NIOBIUM || ore.getAmount() != 2) {
                    context.fail("Insufficient energy must rollback without consuming RAW_NIOBIUM, got " + ore.getAmount());
                    return;
                }
                if (tank.getResource() != GCFluids.SULFURIC_ACID || tank.getAmount() != initialAcid) {
                    context.fail("Insufficient energy must rollback without consuming SULFURIC_ACID, got " + tank.getAmount());
                    return;
                }
                if (!out.isEmpty() || !byp.isEmpty()) {
                    context.fail("Insufficient energy must not emit any leaching outputs");
                    return;
                }
            }
            if (partialPowerPlant.energyStorage().getAmount() != partialEnergy) {
                context.fail("Partial energy transaction must roll back cleanly, expected " + partialEnergy
                        + " GJ intact but got " + partialPowerPlant.energyStorage().getAmount());
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 220)
    public void chemicalLeachingYieldAndByproductsTest(GameTestHelper context) {
        BlockPos niobiumPos = new BlockPos(0, 1, 0);
        BlockPos ilmenitePos = new BlockPos(2, 1, 0);
        BlockPos deshPos = new BlockPos(4, 1, 0);

        context.setBlock(niobiumPos, GCBlocks.CHEMICAL_LEACHING_PLANT);
        context.setBlock(ilmenitePos, GCBlocks.CHEMICAL_LEACHING_PLANT);
        context.setBlock(deshPos, GCBlocks.CHEMICAL_LEACHING_PLANT);

        ChemicalLeachingPlantBlockEntity niobiumPlant = context.getBlockEntity(niobiumPos);
        ChemicalLeachingPlantBlockEntity ilmenitePlant = context.getBlockEntity(ilmenitePos);
        ChemicalLeachingPlantBlockEntity deshPlant = context.getBlockEntity(deshPos);

        long initialAcid = 1000L * FluidUtil.MILLIBUCKET;
        long expectedRemainingAcid = 750L * FluidUtil.MILLIBUCKET;

        niobiumPlant.energyStorage().setEnergy(50_000L);
        niobiumPlant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.ORE_INPUT_SLOT).set(GCItems.RAW_NIOBIUM, 1);
        niobiumPlant.fluidStorage().slot(ChemicalLeachingPlantBlockEntity.ACID_TANK).set(GCFluids.SULFURIC_ACID, initialAcid);

        ilmenitePlant.energyStorage().setEnergy(50_000L);
        ilmenitePlant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.ORE_INPUT_SLOT).set(GCBlocks.ILMENITE_ORE.asItem(), 1);
        ilmenitePlant.fluidStorage().slot(ChemicalLeachingPlantBlockEntity.ACID_TANK).set(GCFluids.SULFURIC_ACID, initialAcid);

        deshPlant.energyStorage().setEnergy(50_000L);
        deshPlant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.ORE_INPUT_SLOT).set(GCItems.RAW_DESH, 1);
        deshPlant.fluidStorage().slot(ChemicalLeachingPlantBlockEntity.ACID_TANK).set(GCFluids.SULFURIC_ACID, initialAcid);

        context.runAfterDelay(135, () -> {
            // 1. RAW_NIOBIUM + 250mB SULFURIC_ACID -> 3x NIOBIUM_OXIDE
            ItemResourceSlot nbOut = niobiumPlant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.PRIMARY_OUTPUT_SLOT);
            FluidResourceSlot nbTank = niobiumPlant.fluidStorage().slot(ChemicalLeachingPlantBlockEntity.ACID_TANK);
            if (nbOut.getResource() != GCItems.NIOBIUM_OXIDE || nbOut.getAmount() != 3) {
                context.fail("Expected RAW_NIOBIUM leaching to yield 3x NIOBIUM_OXIDE, got " + nbOut.getAmount() + "x " + nbOut.getResource());
                return;
            }
            if (nbTank.getAmount() != expectedRemainingAcid) {
                context.fail("Expected 250mB sulfuric acid consumed, remaining=" + nbTank.getAmount());
                return;
            }

            // 2. ILMENITE_ORE + 250mB SULFURIC_ACID -> 2x TITANIUM_INGOT + 1x RAW_IRON byproduct
            ItemResourceSlot tiOut = ilmenitePlant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.PRIMARY_OUTPUT_SLOT);
            ItemResourceSlot tiByp = ilmenitePlant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.BYPRODUCT_OUTPUT_SLOT);
            if (tiOut.getResource() != GCItems.TITANIUM_INGOT || tiOut.getAmount() != 2) {
                context.fail("Expected ILMENITE_ORE leaching to yield 2x TITANIUM_INGOT, got " + tiOut.getAmount() + "x " + tiOut.getResource());
                return;
            }
            if (tiByp.getResource() != Items.RAW_IRON || tiByp.getAmount() != 1) {
                context.fail("Expected ILMENITE_ORE leaching byproduct 1x RAW_IRON, got " + tiByp.getAmount() + "x " + tiByp.getResource());
                return;
            }

            // 3. RAW_DESH + 250mB SULFURIC_ACID -> 2x DESH_INGOT (+ 50% chance +1x DESH_INGOT)
            ItemResourceSlot deshOut = deshPlant.itemStorage().slot(ChemicalLeachingPlantBlockEntity.PRIMARY_OUTPUT_SLOT);
            if (deshOut.getResource() != GCItems.DESH_INGOT || deshOut.getAmount() != 2) {
                context.fail("Expected RAW_DESH leaching to yield 2x DESH_INGOT, got " + deshOut.getAmount() + "x " + deshOut.getResource());
                return;
            }

            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 240)
    public void vacuumMetallurgicInfuserRecipesTest(GameTestHelper context) {
        BlockPos steelPos = new BlockPos(0, 1, 0);
        BlockPos feNbPos = new BlockPos(2, 1, 0);
        BlockPos c103Pos = new BlockPos(4, 1, 0);
        BlockPos coilPos = new BlockPos(6, 1, 0);
        BlockPos unpoweredPos = new BlockPos(8, 1, 0);

        context.setBlock(steelPos, GCBlocks.VACUUM_METALLURGIC_INFUSER);
        context.setBlock(feNbPos, GCBlocks.VACUUM_METALLURGIC_INFUSER);
        context.setBlock(c103Pos, GCBlocks.VACUUM_METALLURGIC_INFUSER);
        context.setBlock(coilPos, GCBlocks.VACUUM_METALLURGIC_INFUSER);
        context.setBlock(unpoweredPos, GCBlocks.VACUUM_METALLURGIC_INFUSER);

        VacuumMetallurgicInfuserBlockEntity steelInfuser = context.getBlockEntity(steelPos);
        VacuumMetallurgicInfuserBlockEntity feNbInfuser = context.getBlockEntity(feNbPos);
        VacuumMetallurgicInfuserBlockEntity c103Infuser = context.getBlockEntity(c103Pos);
        VacuumMetallurgicInfuserBlockEntity coilInfuser = context.getBlockEntity(coilPos);
        VacuumMetallurgicInfuserBlockEntity unpoweredInfuser = context.getBlockEntity(unpoweredPos);

        // 1. IRON_INGOT + CARBON_FRAGMENTS (20u buffer -> consumes 10u -> 10u left) -> COMPRESSED_STEEL
        steelInfuser.energyStorage().setEnergy(50_000L);
        steelInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.INFUSION_AGENT_SLOT).set(GCItems.CARBON_FRAGMENTS, 1);
        steelInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.BASE_METAL_SLOT).set(Items.IRON_INGOT, 1);

        // 2. IRON_INGOT + NIOBIUM_OXIDE -> FERRONIOBIUM_INGOT
        feNbInfuser.energyStorage().setEnergy(50_000L);
        feNbInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.INFUSION_AGENT_SLOT).set(GCItems.NIOBIUM_OXIDE, 1);
        feNbInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.BASE_METAL_SLOT).set(Items.IRON_INGOT, 1);

        // 3. NIOBIUM_INGOT + TITANIUM_INGOT -> COMPRESSED_NIOBIUM (C-103)
        c103Infuser.energyStorage().setEnergy(50_000L);
        c103Infuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.INFUSION_AGENT_SLOT).set(GCItems.TITANIUM_INGOT, 1);
        c103Infuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.BASE_METAL_SLOT).set(GCItems.NIOBIUM_INGOT, 1);

        // 4. NIOBIUM_INGOT + TIN_INGOT -> SUPERCONDUCTING_COIL (Nb3Sn)
        coilInfuser.energyStorage().setEnergy(50_000L);
        coilInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.INFUSION_AGENT_SLOT).set(GCItems.TIN_INGOT, 1);
        coilInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.BASE_METAL_SLOT).set(GCItems.NIOBIUM_INGOT, 1);

        // 5. Unpowered infuser must not consume base metal or infusion buffer
        unpoweredInfuser.energyStorage().setEnergy(0L);
        unpoweredInfuser.setInfusionBuffer(VacuumMetallurgicInfuserBlockEntity.INFUSION_CARBON, 20);
        unpoweredInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.BASE_METAL_SLOT).set(Items.IRON_INGOT, 1);

        context.runAfterDelay(160, () -> {
            ItemResourceSlot steelOut = steelInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.OUTPUT_SLOT);
            if (steelOut.getResource() != GCItems.COMPRESSED_STEEL || steelOut.getAmount() != 1) {
                context.fail("Expected IRON_INGOT + carbon to produce 1x COMPRESSED_STEEL, got " + steelOut.getAmount() + "x " + steelOut.getResource());
                return;
            }
            if (steelInfuser.getInfusionAmount() != 10) {
                context.fail("Expected 10 remaining carbon units in internal infusion buffer, got " + steelInfuser.getInfusionAmount());
                return;
            }

            ItemResourceSlot feNbOut = feNbInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.OUTPUT_SLOT);
            if (feNbOut.getResource() != GCItems.FERRONIOBIUM_INGOT || feNbOut.getAmount() != 1) {
                context.fail("Expected IRON_INGOT + NIOBIUM_OXIDE to produce 1x FERRONIOBIUM_INGOT, got " + feNbOut.getAmount() + "x " + feNbOut.getResource());
                return;
            }

            ItemResourceSlot c103Out = c103Infuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.OUTPUT_SLOT);
            if (c103Out.getResource() != GCItems.COMPRESSED_NIOBIUM || c103Out.getAmount() != 1) {
                context.fail("Expected NIOBIUM_INGOT + TITANIUM_INGOT to produce 1x COMPRESSED_NIOBIUM (C-103), got " + c103Out.getAmount() + "x " + c103Out.getResource());
                return;
            }

            ItemResourceSlot coilOut = coilInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.OUTPUT_SLOT);
            if (coilOut.getResource() != GCItems.SUPERCONDUCTING_COIL || coilOut.getAmount() != 1) {
                context.fail("Expected NIOBIUM_INGOT + TIN_INGOT to produce 1x SUPERCONDUCTING_COIL (Nb3Sn), got " + coilOut.getAmount() + "x " + coilOut.getResource());
                return;
            }

            ItemResourceSlot unpoweredBase = unpoweredInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.BASE_METAL_SLOT);
            if (unpoweredBase.getAmount() != 1 || unpoweredInfuser.getInfusionAmount() != 20
                    || !unpoweredInfuser.itemStorage().slot(VacuumMetallurgicInfuserBlockEntity.OUTPUT_SLOT).isEmpty()) {
                context.fail("Unpowered Vacuum Metallurgic Infuser must not consume iron or carbon buffer");
                return;
            }

            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 240)
    public void compositeAutoclaveAndPartAssemblerIntegrationTest(GameTestHelper context) {
        BlockPos carbonPos = new BlockPos(0, 1, 0);
        BlockPos tilePos = new BlockPos(2, 1, 0);
        BlockPos sofiPos = new BlockPos(4, 1, 0);

        context.setBlock(carbonPos, GCBlocks.COMPOSITE_AUTOCLAVE);
        context.setBlock(tilePos, GCBlocks.COMPOSITE_AUTOCLAVE);
        context.setBlock(sofiPos, GCBlocks.COMPOSITE_AUTOCLAVE);

        CompositeAutoclaveBlockEntity carbonAutoclave = context.getBlockEntity(carbonPos);
        CompositeAutoclaveBlockEntity tileAutoclave = context.getBlockEntity(tilePos);
        CompositeAutoclaveBlockEntity sofiAutoclave = context.getBlockEntity(sofiPos);

        // 1. CARBON_FRAGMENTS + CANVAS -> CARBON_FIBER_PLATE
        carbonAutoclave.energyStorage().setEnergy(50_000L);
        carbonAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.INPUT_SLOT_0).set(GCItems.CARBON_FRAGMENTS, 1);
        carbonAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.INPUT_SLOT_3).set(GCItems.CANVAS, 1);

        // 2. OLIVINE_SHARD + CARBON_FRAGMENTS + SILICON -> ABLATIVE_HEAT_TILE
        tileAutoclave.energyStorage().setEnergy(50_000L);
        tileAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.INPUT_SLOT_0).set(GCItems.OLIVINE_SHARD, 1);
        tileAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.INPUT_SLOT_1).set(GCItems.CARBON_FRAGMENTS, 1);
        tileAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.INPUT_SLOT_2).set(GCItems.SILICON, 1);

        // 3. ISOTHERMAL_FABRIC + DESH_INGOT -> SOFI_INSULATION
        sofiAutoclave.energyStorage().setEnergy(50_000L);
        sofiAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.INPUT_SLOT_1).set(GCItems.ISOTHERMAL_FABRIC, 1);
        sofiAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.INPUT_SLOT_2).set(GCItems.DESH_INGOT, 1);

        context.runAfterDelay(165, () -> {
            ItemResourceSlot carbonOut = carbonAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.OUTPUT_SLOT);
            ItemResourceSlot tileOut = tileAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.OUTPUT_SLOT);
            ItemResourceSlot sofiOut = sofiAutoclave.itemStorage().slot(CompositeAutoclaveBlockEntity.OUTPUT_SLOT);

            if (carbonOut.getResource() != GCItems.CARBON_FIBER_PLATE || carbonOut.getAmount() != 1) {
                context.fail("Expected CARBON_FIBER_PLATE from Composite Autoclave, got " + carbonOut.getAmount() + "x " + carbonOut.getResource());
                return;
            }
            if (tileOut.getResource() != GCItems.ABLATIVE_HEAT_TILE || tileOut.getAmount() != 1) {
                context.fail("Expected ABLATIVE_HEAT_TILE from Composite Autoclave, got " + tileOut.getAmount() + "x " + tileOut.getResource());
                return;
            }
            if (sofiOut.getResource() != GCItems.SOFI_INSULATION || sofiOut.getAmount() != 1) {
                context.fail("Expected SOFI_INSULATION from Composite Autoclave, got " + sofiOut.getAmount() + "x " + sofiOut.getResource());
                return;
            }

            // Validate RocketMaterialRegistry + Part Assembler SchematicGridLayout integration
            RocketMaterialProfile carbonProfile = RocketMaterialRegistry.resolve(new ItemStack(GCItems.CARBON_FIBER_PLATE), SlotRole.STRUCTURAL_HULL).orElse(null);
            RocketMaterialProfile tileProfile = RocketMaterialRegistry.resolve(new ItemStack(GCItems.ABLATIVE_HEAT_TILE), SlotRole.COMMAND_NOSE_RECOVERY).orElse(null);
            RocketMaterialProfile sofiProfile = RocketMaterialRegistry.resolve(new ItemStack(GCItems.SOFI_INSULATION), SlotRole.TANK_PROPELLANT_CELL).orElse(null);

            if (carbonProfile == null || !carbonProfile.traits().contains(RocketMaterialTrait.COPV_WEAVE)) {
                context.fail("CARBON_FIBER_PLATE must resolve to COPV_WEAVE structural profile in RocketMaterialRegistry");
                return;
            }
            if (tileProfile == null || !tileProfile.traits().contains(RocketMaterialTrait.ABLATIVE_TPS) || tileProfile.thermalBonusKelvin() < 1600.0) {
                context.fail("ABLATIVE_HEAT_TILE must resolve to elite ABLATIVE_TPS (+1600K) profile in RocketMaterialRegistry");
                return;
            }
            if (sofiProfile == null || !sofiProfile.traits().contains(RocketMaterialTrait.SOFI_INSULATION) || sofiProfile.boilOffMultiplier() > 0.06) {
                context.fail("SOFI_INSULATION must resolve to elite SOFI_INSULATION (<=0.05 boil-off) profile in RocketMaterialRegistry");
                return;
            }

            RocketPartConfig gemini = RocketPartCatalog.getInstance().getPart("capsule_gemini_3x3_2b").toConfig(RocketLivery.WHITE);
            RocketPartConfig cryoTank = RocketPartCatalog.getInstance().getPart("tank_lox_cryo_3x3_2b").toConfig(RocketLivery.ORANGE);
            SchematicGridLayout geminiLayout = SchematicGridLayout.forConfig(gemini);
            SchematicGridLayout tankLayout = SchematicGridLayout.forConfig(cryoTank);

            if (!geminiLayout.requirement(1).matches(new ItemStack(GCItems.ABLATIVE_HEAT_TILE))) {
                context.fail("Part Assembler COMMAND nose slot must accept ABLATIVE_HEAT_TILE");
                return;
            }
            if (!geminiLayout.requirement(3).matches(new ItemStack(GCItems.CARBON_FIBER_PLATE))) {
                context.fail("Part Assembler COMMAND hull slot must accept CARBON_FIBER_PLATE");
                return;
            }
            if (!tankLayout.requirement(3).matches(new ItemStack(GCItems.SOFI_INSULATION))) {
                context.fail("Part Assembler TANK cryo cell slot must accept SOFI_INSULATION");
                return;
            }

            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
    public void sidedStorageLookupsConnectedTest(GameTestHelper context) {
        BlockPos infuserPos = new BlockPos(0, 1, 0);
        BlockPos leachingPos = new BlockPos(2, 1, 0);
        BlockPos autoclavePos = new BlockPos(4, 1, 0);

        context.setBlock(infuserPos, GCBlocks.VACUUM_METALLURGIC_INFUSER);
        context.setBlock(leachingPos, GCBlocks.CHEMICAL_LEACHING_PLANT);
        context.setBlock(autoclavePos, GCBlocks.COMPOSITE_AUTOCLAVE);

        for (BlockPos rel : new BlockPos[]{infuserPos, leachingPos, autoclavePos}) {
            MachineBlockEntity machine = context.getBlockEntity(rel);
            machine.getIOConfig().get(BlockFace.BOTTOM).setOption(ResourceType.ENERGY, ResourceFlow.BOTH);
            machine.getIOConfig().get(BlockFace.TOP).setOption(ResourceType.ITEM, ResourceFlow.BOTH);
            BlockPos abs = context.absolutePos(rel);
            if (EnergyStorage.SIDED.find(context.getLevel(), abs, Direction.DOWN) == null) {
                context.fail("EnergyStorage.SIDED missing for machine at " + rel);
                return;
            }
            if (ItemStorage.SIDED.find(context.getLevel(), abs, Direction.UP) == null) {
                context.fail("ItemStorage.SIDED missing for machine at " + rel);
                return;
            }
        }

        MachineBlockEntity leaching = context.getBlockEntity(leachingPos);
        leaching.getIOConfig().get(BlockFace.LEFT).setOption(ResourceType.FLUID, ResourceFlow.INPUT);
        if (FluidStorage.SIDED.find(context.getLevel(), context.absolutePos(leachingPos), Direction.EAST) == null
                && FluidStorage.SIDED.find(context.getLevel(), context.absolutePos(leachingPos), Direction.WEST) == null) {
            context.fail("FluidStorage.SIDED missing for Chemical Leaching Plant");
            return;
        }

        context.succeed();
    }
}
