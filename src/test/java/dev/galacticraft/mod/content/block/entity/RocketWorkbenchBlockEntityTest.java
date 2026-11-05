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

package dev.galacticraft.mod.content.block.entity;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.special.RocketWorkbench;
import dev.galacticraft.mod.content.item.BatteryItem;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.recipe.PartAssemblerRecipe;
import dev.galacticraft.mod.recipe.PartAssemblerRecipeInput;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import team.reborn.energy.api.EnergyStorage;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RocketWorkbenchBlockEntity Adversarial Challenge Suite")
class RocketWorkbenchBlockEntityTest {

    private static RegistryAccess registryAccess;
    private static Level dummyLevel;

    static class TestRocketWorkbenchBlockEntity extends RocketWorkbenchBlockEntity {
        public TestRocketWorkbenchBlockEntity(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public void setChanged() {
            // No-op during isolated unit tests to avoid requiring full chunk provider and physics systems
        }

        public void publicExtractCraftingMaterials(RecipeHolder<PartAssemblerRecipe> recipe) {
            super.extractCraftingMaterials(recipe);
        }

        public void publicChargeFromSlot(int slot) {
            super.chargeFromSlot(slot);
        }

        public boolean publicCanOutputStacks(RecipeHolder<PartAssemblerRecipe> recipe) {
            return super.canOutputStacks(recipe);
        }

        public void publicOutputStacks(RecipeHolder<PartAssemblerRecipe> recipe) {
            super.outputStacks(recipe);
        }

        public MachineStatus publicHasResourcesToWork() {
            return super.hasResourcesToWork();
        }

        public void publicExtractResourcesToWork() {
            super.extractResourcesToWork();
        }

        public PartAssemblerRecipeInput publicCraftingInv() {
            return super.craftingInv();
        }
    }

    @BeforeAll
    static void setupBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(Constant.id(Constant.Recipe.PART_ASSEMBLER))) {
            GCRecipes.register();
        }
        GCBlocks.register();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        dummyLevel = createDummyLevel(registryAccess);
    }

    private static Level createDummyLevel(RegistryAccess access) {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            Level level = (Level) unsafe.allocateInstance(ServerLevel.class);
            Field regField = Level.class.getDeclaredField("registryAccess");
            regField.setAccessible(true);
            regField.set(level, access);
            return level;
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate dummy Level for testing", e);
        }
    }

    private static TestRocketWorkbenchBlockEntity createWorkbench() {
        TestRocketWorkbenchBlockEntity workbench = new TestRocketWorkbenchBlockEntity(BlockPos.ZERO, GCBlocks.ROCKET_WORKBENCH.defaultBlockState());
        workbench.setLevel(dummyLevel);
        return workbench;
    }

    private static ShapedRecipePattern create3x3Pattern(Ingredient fill) {
        Map<Character, Ingredient> key = Map.of('X', fill);
        return ShapedRecipePattern.of(
                key,
                "XXX",
                "XXX",
                "XXX"
        );
    }

    private static RecipeHolder<PartAssemblerRecipe> createRecipe(ItemStack result, ItemStack scrap, Optional<RocketPartStats> stats) {
        PartAssemblerRecipe recipe = new PartAssemblerRecipe(
                "rocket_engines",
                Ingredient.of(Items.PAPER),
                create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                result,
                scrap,
                200,
                stats
        );
        return new RecipeHolder<>(Constant.id("test_part_recipe"), recipe);
    }

    // =========================================================================
    // 1. StorageSpec & Slot Mapping Invariants
    // =========================================================================

    @Nested
    @DisplayName("1. StorageSpec & Slot Mapping Invariants")
    class SlotMappingTests {

        @Test
        @DisplayName("Total slot count must be exactly 13")
        void testTotalSlotCount() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            assertEquals(13, workbench.itemStorage().size(), "Workbench must have exactly 13 item slots");
        }

        @Test
        @DisplayName("Slot 0: Battery / Charge Slot (TRANSFER, capacity 1, filter CAN_EXTRACT_ENERGY)")
        void testSlot0Charge() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            ItemResourceSlot slot = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.CHARGE_SLOT);
            assertEquals(0, RocketWorkbenchBlockEntity.CHARGE_SLOT);
            assertEquals(TransferType.TRANSFER, slot.transferMode());
            assertEquals(1, slot.getCapacity());

            // Slot filter accepts battery with energy extraction capabilities
            ItemStack battery = new ItemStack(GCItems.BATTERY);
            ((BatteryItem) GCItems.BATTERY).setStoredEnergy(battery, 5000);
            assertTrue(slot.getFilter().test(battery.getItem(), battery.getComponentsPatch()), "Slot 0 filter must accept battery");

            // Slot filter rejects non-energy item
            assertFalse(slot.getFilter().test(Items.IRON_INGOT, DataComponentPatch.EMPTY), "Slot 0 filter must reject non-energy item");
        }

        @Test
        @DisplayName("Slot 1: Schematic Blueprint (INPUT, capacity 1, sits outside crafting inputSlots)")
        void testSlot1Schematic() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            ItemResourceSlot slot = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT);
            assertEquals(1, RocketWorkbenchBlockEntity.SCHEMATIC_SLOT);
            assertEquals(TransferType.INPUT, slot.transferMode());
            assertEquals(1, slot.getCapacity());
        }

        @Test
        @DisplayName("Slots 2..10: 3x3 Component Crafting Grid (INPUT, capacity 64)")
        void testSlots2To10Grid() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            assertEquals(2, RocketWorkbenchBlockEntity.INPUT_GRID_START);
            assertEquals(9, RocketWorkbenchBlockEntity.INPUT_GRID_LEN);

            for (int i = 2; i <= 10; i++) {
                ItemResourceSlot slot = workbench.itemStorage().slot(i);
                assertEquals(TransferType.INPUT, slot.transferMode(), "Slot " + i + " must be INPUT");
                assertEquals(64, slot.getCapacity(), "Slot " + i + " must have capacity 64");
            }
        }

        @Test
        @DisplayName("Slot 11: Part Output (OUTPUT, capacity 1)")
        void testSlot11Output() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            ItemResourceSlot slot = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.OUTPUT_SLOT);
            assertEquals(11, RocketWorkbenchBlockEntity.OUTPUT_SLOT);
            assertEquals(TransferType.OUTPUT, slot.transferMode());
            assertEquals(1, slot.getCapacity(), "Part output slot must have capacity 1");
        }

        @Test
        @DisplayName("Slot 12: Scrap Byproduct Output (OUTPUT, capacity 64)")
        void testSlot12Byproduct() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            ItemResourceSlot slot = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT);
            assertEquals(12, RocketWorkbenchBlockEntity.BYPRODUCT_SLOT);
            assertEquals(TransferType.OUTPUT, slot.transferMode());
            assertEquals(64, slot.getCapacity(), "Scrap byproduct slot must have capacity 64");
        }

        @Test
        @DisplayName("Crafting input inventory isolates Slot 1 (schematic) from 3x3 grid slots")
        void testCraftingInvMapping() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            workbench.itemStorage().slot(1).set(Items.PAPER, 1);
            workbench.itemStorage().slot(2).set(Items.IRON_INGOT, 5);

            PartAssemblerRecipeInput input = workbench.publicCraftingInv();
            assertEquals(Items.PAPER, input.schematic().getItem(), "Input schematic must match slot 1");
            assertEquals(Items.PAPER, input.getItem(0).getItem(), "Input getItem(0) must return schematic from slot 1");
            assertEquals(Items.IRON_INGOT, input.grid().getItem(0).getItem(), "Input grid slot 0 must match workbench slot 2");
            assertEquals(Items.IRON_INGOT, input.getItem(1).getItem(), "Input getItem(1) must match grid slot 0");
            assertEquals(5, input.getItem(1).getCount());
        }

        @Test
        @DisplayName("Non-consumption invariant: extracting materials decrements grid (2..10) but NEVER touches Slot 1")
        void testBlueprintNonConsumptionInvariant() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            // Populate schematic in slot 1
            workbench.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT).set(Items.PAPER, 1);
            // Populate grid slots 2..10
            for (int i = 2; i <= 10; i++) {
                workbench.itemStorage().slot(i).set(Items.IRON_INGOT, 4);
            }

            RecipeHolder<PartAssemblerRecipe> recipe = createRecipe(new ItemStack(Items.DIAMOND), ItemStack.EMPTY, Optional.empty());
            workbench.publicExtractCraftingMaterials(recipe);

            // Slot 1 must remain untouched!
            ItemResourceSlot blueprintSlot = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT);
            assertEquals(1, blueprintSlot.getAmount(), "Schematic blueprint in slot 1 must NOT be consumed");
            assertEquals(Items.PAPER, blueprintSlot.getResource());

            // Grid slots must have decremented by 1
            for (int i = 2; i <= 10; i++) {
                assertEquals(3, workbench.itemStorage().slot(i).getAmount(), "Grid slot " + i + " must decrement by 1");
            }
        }
    }

    // =========================================================================
    // 2. Dual Output Routing & canOutputStacks Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("2. Dual Output Routing & Output Full Matrix")
    class OutputRoutingTests {

        @Test
        @DisplayName("outputStacks routes primary part to slot 11 and scrap byproduct to slot 12 with DataComponents intact")
        void testOutputStacksDualRouting() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();

            RocketPartStats stats = new RocketPartStats(2500.0, 150.0, 3000L, 4.0, 800.0);
            ItemStack partOutput = new ItemStack(Items.NETHERITE_INGOT);
            ItemStack scrapOutput = new ItemStack(Items.IRON_NUGGET, 4);

            RecipeHolder<PartAssemblerRecipe> recipe = createRecipe(partOutput, scrapOutput, Optional.of(stats));

            workbench.publicOutputStacks(recipe);

            // Check Slot 11: Part output
            ItemResourceSlot slot11 = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.OUTPUT_SLOT);
            assertFalse(slot11.isEmpty(), "Slot 11 must contain manufactured part");
            assertEquals(Items.NETHERITE_INGOT, slot11.getResource());
            assertEquals(1, slot11.getAmount());
            // Verify DataComponent patch survival
            assertEquals(Optional.of(stats), slot11.getComponents().get(GCDataComponents.ROCKET_PART_STATS));
            assertEquals(Optional.of(PartIntegrity.pristine(100.0)), slot11.getComponents().get(GCDataComponents.PART_INTEGRITY));

            // Check Slot 12: Scrap byproduct
            ItemResourceSlot slot12 = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT);
            assertFalse(slot12.isEmpty(), "Slot 12 must contain scrap byproduct");
            assertEquals(Items.IRON_NUGGET, slot12.getResource());
            assertEquals(4, slot12.getAmount());
        }

        @Test
        @DisplayName("outputStacks with EMPTY scrap byproduct leaves slot 12 completely empty")
        void testOutputStacksEmptyByproduct() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            RecipeHolder<PartAssemblerRecipe> recipe = createRecipe(new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY, Optional.empty());

            workbench.publicOutputStacks(recipe);

            ItemResourceSlot slot11 = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.OUTPUT_SLOT);
            assertEquals(Items.GOLD_INGOT, slot11.getResource());
            assertEquals(1, slot11.getAmount());

            ItemResourceSlot slot12 = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT);
            assertTrue(slot12.isEmpty(), "Slot 12 must remain empty when recipe has no byproduct");
        }

        @Test
        @DisplayName("canOutputStacks returns TRUE when both slots 11 and 12 are empty")
        void testCanOutputStacksCleanSlate() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            RecipeHolder<PartAssemblerRecipe> recipe = createRecipe(new ItemStack(Items.DIAMOND), new ItemStack(Items.IRON_NUGGET, 2), Optional.empty());

            assertTrue(workbench.publicCanOutputStacks(recipe), "canOutputStacks must return true when outputs are empty");
        }

        @Test
        @DisplayName("canOutputStacks returns FALSE when slot 11 is full (capacity 1 reached)")
        void testCanOutputStacksSlot11Full() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            workbench.itemStorage().slot(RocketWorkbenchBlockEntity.OUTPUT_SLOT).set(Items.DIAMOND, 1);

            RecipeHolder<PartAssemblerRecipe> recipe = createRecipe(new ItemStack(Items.DIAMOND), ItemStack.EMPTY, Optional.empty());

            assertFalse(workbench.publicCanOutputStacks(recipe), "canOutputStacks must return false when slot 11 is full");
        }

        @Test
        @DisplayName("canOutputStacks returns FALSE when slot 12 is full (64 items) and recipe produces byproduct")
        void testCanOutputStacksSlot12FullWithByproduct() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            // Slot 11 is empty, but slot 12 is at max stack size 64
            workbench.itemStorage().slot(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT).set(Items.IRON_NUGGET, 64);

            RecipeHolder<PartAssemblerRecipe> recipe = createRecipe(new ItemStack(Items.DIAMOND), new ItemStack(Items.IRON_NUGGET, 2), Optional.empty());

            assertFalse(workbench.publicCanOutputStacks(recipe), "canOutputStacks must return false when slot 12 is full and byproduct is produced");
        }

        @Test
        @DisplayName("canOutputStacks returns FALSE when slot 12 contains a conflicting item and recipe produces byproduct")
        void testCanOutputStacksSlot12ConflictingItem() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            workbench.itemStorage().slot(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT).set(Items.COPPER_INGOT, 1);

            RecipeHolder<PartAssemblerRecipe> recipe = createRecipe(new ItemStack(Items.DIAMOND), new ItemStack(Items.IRON_NUGGET, 2), Optional.empty());

            assertFalse(workbench.publicCanOutputStacks(recipe), "canOutputStacks must return false when slot 12 holds a different item");
        }

        @Test
        @DisplayName("canOutputStacks returns TRUE when slot 12 is full BUT recipe has NO scrap byproduct")
        void testCanOutputStacksSlot12FullEmptyByproduct() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            // Slot 12 is completely jammed with cobblestone
            workbench.itemStorage().slot(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT).set(Items.COBBLESTONE, 64);

            // Recipe has EMPTY scrap byproduct
            RecipeHolder<PartAssemblerRecipe> recipe = createRecipe(new ItemStack(Items.DIAMOND), ItemStack.EMPTY, Optional.empty());

            assertTrue(workbench.publicCanOutputStacks(recipe), "canOutputStacks must return true if slot 12 is jammed but recipe produces NO byproduct");
        }

        @Test
        @DisplayName("canOutputStacks returns TRUE when slot 12 has matching scrap and space remaining")
        void testCanOutputStacksSlot12Partial() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            workbench.itemStorage().slot(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT).set(Items.IRON_NUGGET, 10);

            RecipeHolder<PartAssemblerRecipe> recipe = createRecipe(new ItemStack(Items.DIAMOND), new ItemStack(Items.IRON_NUGGET, 2), Optional.empty());

            assertTrue(workbench.publicCanOutputStacks(recipe), "canOutputStacks must return true when slot 12 has partial matching stack");
        }
    }

    // =========================================================================
    // 3. Energy Buffer & Operational Consumption
    // =========================================================================

    @Nested
    @DisplayName("3. Energy Buffer & Operational Consumption")
    class EnergyTests {

        @Test
        @DisplayName("Energy parameters: buffer = 60,000L GJ, operational drain = 120L GJ/t, max insert = 480L GJ/t")
        void testEnergyParameters() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            assertEquals(60_000L, RocketWorkbenchBlockEntity.ENERGY_CAPACITY);
            assertEquals(120L, RocketWorkbenchBlockEntity.ENERGY_CONSUMPTION_RATE);
            assertEquals(60_000L, workbench.energyStorage().getCapacity(), "Energy buffer capacity must be 60k GJ");
        }

        @Test
        @DisplayName("hasResourcesToWork returns NOT_ENOUGH_ENERGY when energy < 120L GJ, returns null when >= 120L GJ")
        void testHasResourcesToWorkThreshold() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();

            // Energy = 0
            assertEquals(MachineStatuses.NOT_ENOUGH_ENERGY, workbench.publicHasResourcesToWork(), "Energy 0 must yield NOT_ENOUGH_ENERGY");

            // Energy = 119 (1 below threshold)
            workbench.energyStorage().insert(119);
            assertEquals(MachineStatuses.NOT_ENOUGH_ENERGY, workbench.publicHasResourcesToWork(), "Energy 119 must yield NOT_ENOUGH_ENERGY");

            // Energy = 120 (exact operational threshold)
            workbench.energyStorage().insert(1);
            assertNull(workbench.publicHasResourcesToWork(), "Energy 120 must satisfy operational requirement (status null)");

            // Energy = 60,000 (fully charged)
            workbench.energyStorage().insert(59_880);
            assertEquals(60_000L, workbench.energyStorage().getAmount());
            assertNull(workbench.publicHasResourcesToWork(), "Full energy must satisfy operational requirement (status null)");
        }

        @Test
        @DisplayName("extractResourcesToWork drains exactly 120L GJ")
        void testExtractResourcesToWork() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            workbench.energyStorage().insert(500);

            workbench.publicExtractResourcesToWork();
            assertEquals(380L, workbench.energyStorage().getAmount(), "extractResourcesToWork must drain exactly 120 GJ");

            workbench.publicExtractResourcesToWork();
            assertEquals(260L, workbench.energyStorage().getAmount(), "Second extraction must drain another 120 GJ");
        }

        @Test
        @DisplayName("chargeFromSlot pulls energy from battery in slot 0 into energy buffer up to 480 GJ/t")
        void testChargeFromSlot0() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();

            ItemStack battery = new ItemStack(GCItems.BATTERY);
            ((BatteryItem) GCItems.BATTERY).setStoredEnergy(battery, 10_000L);
            workbench.itemStorage().slot(RocketWorkbenchBlockEntity.CHARGE_SLOT).set(battery.getItem(), battery.getComponentsPatch(), 1);

            assertEquals(0L, workbench.energyStorage().getAmount());

            // Tick chargeFromSlot
            workbench.publicChargeFromSlot(RocketWorkbenchBlockEntity.CHARGE_SLOT);

            // Rate is 480 GJ/t (ENERGY_CONSUMPTION_RATE * 4)
            assertEquals(480L, workbench.energyStorage().getAmount(), "chargeFromSlot must pull up to 480 GJ into energy storage");
            EnergyStorage itemEnergy = workbench.itemStorage().slot(RocketWorkbenchBlockEntity.CHARGE_SLOT).find(EnergyStorage.ITEM);
            assertNotNull(itemEnergy);
            assertEquals(9_520L, itemEnergy.getAmount());
        }
    }

    // =========================================================================
    // 4. Backward Compatibility Bridges
    // =========================================================================

    @Nested
    @DisplayName("4. Legacy Compatibility Bridges")
    class LegacyBridgeTests {

        @Test
        @DisplayName("Legacy containers and properties remain present and non-null")
        void testLegacyMembers() {
            TestRocketWorkbenchBlockEntity workbench = createWorkbench();
            assertNotNull(workbench.ingredients, "ingredients VariableSizedContainer must exist");
            assertEquals(14, workbench.ingredients.getTargetSize());
            assertNotNull(workbench.output, "output SimpleContainer must exist");
            assertEquals(1, workbench.output.getContainerSize());
            assertNotNull(workbench.chests, "chests SimpleContainer must exist");
            assertEquals(1, workbench.chests.getContainerSize());

            assertNotNull(RocketWorkbench.FACING, "RocketWorkbench.FACING DirectionProperty must exist");
        }
    }
}
