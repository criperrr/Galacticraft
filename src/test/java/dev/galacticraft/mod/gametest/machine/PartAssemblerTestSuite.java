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

package dev.galacticraft.mod.gametest.machine;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.PartDamageState;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.machinelib.api.gametest.RecipeGameTest;
import dev.galacticraft.machinelib.api.gametest.annotation.MachineTest;
import dev.galacticraft.machinelib.api.gametest.annotation.TestSuite;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.RocketWorkbenchBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.PartAssemblerRecipe;
import dev.galacticraft.mod.recipe.PartAssemblerRecipeInput;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;

@TestSuite("part_assembler")
public final class PartAssemblerTestSuite extends RecipeGameTest<PartAssemblerRecipeInput, PartAssemblerRecipe, RocketWorkbenchBlockEntity> {
    public static final int HEAVY_NOSE_CONE_TIME = 250;
    public static final int ROCKET_ENGINE_TIME = 300;

    public PartAssemblerTestSuite() {
        super(GCBlocks.ROCKET_WORKBENCH, List.of(
                machine -> machine.energyStorage().setEnergy(RocketWorkbenchBlockEntity.ENERGY_CAPACITY),
                machine -> machine.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT).set(GCItems.BASIC_ROCKET_CONE_SCHEMATIC, 1),
                machine -> machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 1).set(Items.REDSTONE_TORCH, 1),
                machine -> machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 4).set(GCItems.TIER_3_HEAVY_DUTY_PLATE, 1),
                machine -> machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 6).set(GCItems.TIER_3_HEAVY_DUTY_PLATE, 1),
                machine -> machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 8).set(GCItems.TIER_3_HEAVY_DUTY_PLATE, 1)
        ), RocketWorkbenchBlockEntity.OUTPUT_SLOT, 2, HEAVY_NOSE_CONE_TIME);
    }

    private static void populateHeavyNoseConeInputs(RocketWorkbenchBlockEntity machine) {
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT).set(GCItems.BASIC_ROCKET_CONE_SCHEMATIC, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 1).set(Items.REDSTONE_TORCH, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 4).set(GCItems.TIER_3_HEAVY_DUTY_PLATE, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 6).set(GCItems.TIER_3_HEAVY_DUTY_PLATE, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 8).set(GCItems.TIER_3_HEAVY_DUTY_PLATE, 1);
    }

    private static void populateRocketEngineInputs(RocketWorkbenchBlockEntity machine) {
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT).set(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 1).set(Items.FLINT_AND_STEEL, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 2).set(Items.STONE_BUTTON, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 3).set(GCItems.TIER_1_HEAVY_DUTY_PLATE, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 4).set(GCItems.TIN_CANISTER, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 5).set(GCItems.TIER_1_HEAVY_DUTY_PLATE, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 6).set(GCItems.TIER_1_HEAVY_DUTY_PLATE, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 7).set(GCItems.OXYGEN_VENT, 1);
        machine.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + 8).set(GCItems.TIER_1_HEAVY_DUTY_PLATE, 1);
    }

    @MachineTest(workTime = 10)
    public Runnable noEnergyPreventsProgress(RocketWorkbenchBlockEntity machine) {
        Assertions.assertEquals(60_000L, machine.energyStorage().getCapacity(), "Expected 60,000 GJ energy capacity");
        machine.energyStorage().setEnergy(0L);
        populateHeavyNoseConeInputs(machine);

        return () -> {
            Assertions.assertEquals(MachineStatuses.NOT_ENOUGH_ENERGY, machine.getState().getStatus(), "Expected NOT_ENOUGH_ENERGY status when energy is 0");
            Assertions.assertEquals(0, machine.getProgress(), "Machine must not progress without energy");
            Assertions.assertTrue(machine.itemStorage().slot(RocketWorkbenchBlockEntity.OUTPUT_SLOT).isEmpty(), "Output slot must remain empty without energy");
            Assertions.assertTrue(machine.itemStorage().slot(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT).isEmpty(), "Byproduct slot must remain empty without energy");
        };
    }

    @MachineTest(workTime = 10)
    public Runnable energySuppliedAdvancesProgressAndConsumesRate(RocketWorkbenchBlockEntity machine) {
        machine.energyStorage().setEnergy(RocketWorkbenchBlockEntity.ENERGY_CAPACITY);
        populateHeavyNoseConeInputs(machine);

        return () -> {
            Assertions.assertEquals(GCMachineStatuses.FABRICATING, machine.getState().getStatus(), "Expected FABRICATING status while powered and crafting");
            Assertions.assertEquals(10, machine.getProgress(), "Expected progress to advance by 10 after 10 ticks");
            long expectedEnergy = RocketWorkbenchBlockEntity.ENERGY_CAPACITY - (10L * RocketWorkbenchBlockEntity.ENERGY_CONSUMPTION_RATE);
            Assertions.assertEquals(expectedEnergy, machine.energyStorage().getAmount(), "Expected 120 GJ/t consumption over 10 ticks");
        };
    }

    @MachineTest(workTime = HEAVY_NOSE_CONE_TIME)
    public Runnable craftHeavyNoseConePreservesSchematicAndComponents(RocketWorkbenchBlockEntity machine) {
        machine.energyStorage().setEnergy(RocketWorkbenchBlockEntity.ENERGY_CAPACITY);
        populateHeavyNoseConeInputs(machine);

        return () -> {
            // 1. Verify energy consumption (250 ticks * 120 GJ/t = 30,000 GJ consumed, 30,000 GJ remaining)
            long expectedRemainingEnergy = RocketWorkbenchBlockEntity.ENERGY_CAPACITY - (HEAVY_NOSE_CONE_TIME * RocketWorkbenchBlockEntity.ENERGY_CONSUMPTION_RATE);
            Assertions.assertEquals(expectedRemainingEnergy, machine.energyStorage().getAmount(), "Expected exact energy drain after 250-tick craft");

            // 2. Verify Slot 1 schematic blueprint is NOT consumed
            ItemResourceSlot schematicSlot = machine.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT);
            Assertions.assertFalse(schematicSlot.isEmpty(), "Schematic in Slot 1 must not be consumed");
            Assertions.assertEquals(GCItems.BASIC_ROCKET_CONE_SCHEMATIC, schematicSlot.getResource(), "Schematic item in Slot 1 must remain intact");
            Assertions.assertEquals(1L, schematicSlot.getAmount(), "Schematic count in Slot 1 must remain 1");

            // 3. Verify 3x3 crafting grid (Slots 2..10) was consumed
            for (int i = 0; i < RocketWorkbenchBlockEntity.INPUT_GRID_LEN; i++) {
                int slotIdx = RocketWorkbenchBlockEntity.INPUT_GRID_START + i;
                Assertions.assertTrue(machine.itemStorage().slot(slotIdx).isEmpty(), "Grid slot " + slotIdx + " should be empty after crafting");
            }

            // 4. Verify Slot 11 primary output item and DataComponents (ROCKET_PART_STATS & PART_INTEGRITY)
            ItemStack outputStack = machine.itemStorage().getItem(RocketWorkbenchBlockEntity.OUTPUT_SLOT);
            Assertions.assertFalse(outputStack.isEmpty(), "Slot 11 must contain the crafted Heavy Nose Cone");
            Assertions.assertEquals(GCItems.HEAVY_NOSE_CONE, outputStack.getItem(), "Slot 11 item must be HEAVY_NOSE_CONE");
            Assertions.assertEquals(1, outputStack.getCount(), "Slot 11 count must be 1");

            RocketPartStats expectedStats = new RocketPartStats(0.0, 450.0, 0L, 5.0, 2500.0);
            Assertions.assertEquals(expectedStats, outputStack.get(GCDataComponents.ROCKET_PART_STATS), "Crafted part in Slot 11 must preserve ROCKET_PART_STATS");

            PartIntegrity integrity = outputStack.get(GCDataComponents.PART_INTEGRITY);
            Assertions.assertNotNull(integrity, "Crafted part in Slot 11 must carry PART_INTEGRITY");
            Assertions.assertEquals(PartIntegrity.pristine(100.0), integrity, "Crafted part must have pristine(100.0) integrity");
            Assertions.assertTrue(integrity.isPristine(), "Crafted part integrity must report isPristine()");
            Assertions.assertEquals(PartDamageState.NOMINAL, integrity.damageState(), "Crafted part damage state must be NOMINAL");

            // 5. Verify Slot 12 scrap byproduct output
            ItemStack byproductStack = machine.itemStorage().getItem(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT);
            Assertions.assertFalse(byproductStack.isEmpty(), "Slot 12 must contain scrap byproduct");
            Assertions.assertEquals(GCItems.COMPRESSED_TITANIUM, byproductStack.getItem(), "Slot 12 byproduct must be COMPRESSED_TITANIUM");
            Assertions.assertEquals(1, byproductStack.getCount(), "Slot 12 byproduct count must be 1");
        };
    }

    @MachineTest(workTime = ROCKET_ENGINE_TIME)
    public Runnable craftRocketEngineFullCycle(RocketWorkbenchBlockEntity machine) {
        machine.energyStorage().setEnergy(RocketWorkbenchBlockEntity.ENERGY_CAPACITY);
        populateRocketEngineInputs(machine);

        return () -> {
            // 1. Verify energy consumption (300 ticks * 120 GJ/t = 36,000 GJ consumed, 24,000 GJ remaining)
            long expectedRemainingEnergy = RocketWorkbenchBlockEntity.ENERGY_CAPACITY - (ROCKET_ENGINE_TIME * RocketWorkbenchBlockEntity.ENERGY_CONSUMPTION_RATE);
            Assertions.assertEquals(expectedRemainingEnergy, machine.energyStorage().getAmount(), "Expected exact energy drain after 300-tick craft");

            // 2. Verify Slot 1 schematic non-consumption
            ItemResourceSlot schematicSlot = machine.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT);
            Assertions.assertEquals(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC, schematicSlot.getResource(), "Engine schematic in Slot 1 must not be consumed");
            Assertions.assertEquals(1L, schematicSlot.getAmount(), "Engine schematic count must remain 1");

            // 3. Verify Slot 11 Rocket Engine output and thrust stats
            ItemStack outputStack = machine.itemStorage().getItem(RocketWorkbenchBlockEntity.OUTPUT_SLOT);
            Assertions.assertEquals(GCItems.ROCKET_ENGINE, outputStack.getItem(), "Slot 11 must contain ROCKET_ENGINE");
            Assertions.assertEquals(1, outputStack.getCount(), "Slot 11 count must be 1");
            RocketPartStats expectedEngineStats = new RocketPartStats(1200.0, 850.0, 0L, 3.5, 2200.0);
            Assertions.assertEquals(expectedEngineStats, outputStack.get(GCDataComponents.ROCKET_PART_STATS), "Rocket Engine must carry 1200 kN thrust ROCKET_PART_STATS");
            Assertions.assertEquals(PartIntegrity.pristine(100.0), outputStack.get(GCDataComponents.PART_INTEGRITY), "Rocket Engine must carry pristine(100.0) PART_INTEGRITY");

            // 4. Verify Slot 12 Compressed Tin scrap byproduct
            ItemStack byproductStack = machine.itemStorage().getItem(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT);
            Assertions.assertEquals(GCItems.COMPRESSED_TIN, byproductStack.getItem(), "Slot 12 byproduct must be COMPRESSED_TIN");
            Assertions.assertEquals(1, byproductStack.getCount(), "Slot 12 byproduct count must be 1");
        };
    }

    @MachineTest(workTime = 1)
    public Runnable sidedEnergyLookupExposesAllFacesByDefaultForWireConnection(RocketWorkbenchBlockEntity machine) {
        return () -> {
            for (Direction direction : Direction.values()) {
                EnergyStorage storage = EnergyStorage.SIDED.find(machine.getLevel(), machine.getBlockPos(), direction);
                Assertions.assertNotNull(storage, "EnergyStorage.SIDED must be exposed on " + direction + " by default so Aluminum Wire connects immediately");
                Assertions.assertTrue(storage.supportsInsertion(), "EnergyStorage on " + direction + " must support energy insertion");
            }
        };
    }

    @Override
    @GameTestGenerator
    public @NotNull List<TestFunction> registerTests() {
        List<TestFunction> tests = super.registerTests();
        tests.add(this.createChargeFromEnergyItemTest(RocketWorkbenchBlockEntity.CHARGE_SLOT, GCItems.INFINITE_BATTERY));
        return tests;
    }
}
