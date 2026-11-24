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

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import dev.galacticraft.api.rocket.part.data.PartDamageState;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile.SlotRole;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition.SlotMaterialEntry;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.util.ItemStackUtil;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.AeroRefurbishmentBenchBlockEntity;
import dev.galacticraft.mod.content.block.platform.HangarTerminalBlock;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Headless Minecraft GameTests validating Plan 09 emergent rocket physics, cryogenic boil-off with
 * active cryocooler power, Venus environmental integrity wear, and the Aerospace Refurbishment Bench.
 */
public class EmergentPhysicsAndRefurbishmentTestSuite implements GalacticraftGameTest {
    private static final String PLATFORM_ARENA = "galacticraft-test:platform_arena";

    private static ItemStack createPartStack(String modelId, RocketPartCategory fallback, RocketPartComposition comp) {
        RocketPartSpec spec = RocketPartCatalog.getInstance().getPart(modelId);
        RocketPartConfig cfg = spec != null
                ? spec.toConfig(RocketLivery.WHITE)
                : RocketPartCatalog.getInstance().defaultConfigFor(fallback);
        RocketPartCostCalculator.CostProfile profile = RocketPartCostCalculator.calculate(cfg, comp);
        return ModularRocketPartBlockItem.createConfiguredStack(
                cfg,
                profile.stats(),
                PartIntegrity.pristine(profile.maxIntegrity()),
                comp
        );
    }

    private static RocketPartComposition hullComp(Item item, RocketMaterialTrait trait) {
        var id = BuiltInRegistries.ITEM.getKey(item);
        return new RocketPartComposition(List.of(
                SlotMaterialEntry.of(3, SlotRole.STRUCTURAL_HULL, id, List.of(trait)),
                SlotMaterialEntry.of(5, SlotRole.STRUCTURAL_HULL, id, List.of(trait))
        ));
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "emergentPhysics", timeoutTicks = 200)
    public void testRefurbishmentBenchRestoresVenusDamagedPartAndAppliesLeadCoating(GameTestHelper helper) {
        BlockPos benchRel = new BlockPos(1, 2, 1);
        helper.setBlock(benchRel, GCBlocks.AERO_REFURBISHMENT_BENCH.defaultBlockState());

        AeroRefurbishmentBenchBlockEntity bench = helper.getBlockEntity(benchRel);
        helper.assertTrue(bench != null, "Expected AeroRefurbishmentBenchBlockEntity at " + benchRel);

        // Create an unprotected aluminum capsule and subject it to Venus (92 atm, 735 K, H2SO4)
        ItemStack capsule = createPartStack(
                "capsule_vostok_1x1_1b",
                RocketPartCategory.COMMAND,
                hullComp(GCItems.COMPRESSED_ALUMINUM, RocketMaterialTrait.LIGHTWEIGHT_ALLOY)
        );
        PartIntegrity damaged = RocketFlightValidator.applyEnvironmentalWearToStack(
                capsule, PlanetaryEnvironment.VENUS, 15.0
        );
        helper.assertTrue(damaged.fraction() < 0.85,
                "Expected Venus to degrade unprotected capsule integrity, got " + damaged.fraction());

        // Power the bench and insert damaged capsule + Lead Ingot (Anti-Acid Cladding)
        bench.energyStorage().setEnergy(AeroRefurbishmentBenchBlockEntity.ENERGY_CAPACITY);
        bench.itemStorage().slot(AeroRefurbishmentBenchBlockEntity.PART_INPUT_SLOT)
                .insert(capsule.getItem(), capsule.getComponentsPatch(), 1L);
        bench.itemStorage().slot(AeroRefurbishmentBenchBlockEntity.MATERIAL_INPUT_SLOT)
                .insert(GCItems.LEAD_INGOT, 1L);

        helper.runAfterDelay(AeroRefurbishmentBenchBlockEntity.COATING_TICKS + 10, () -> {
            ItemResourceSlot outSlot = bench.itemStorage().slot(AeroRefurbishmentBenchBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(!outSlot.isEmpty(), "Expected refurbished part in output slot");

            ItemStack result = ItemStackUtil.create(outSlot);
            PartIntegrity restored = result.get(GCDataComponents.PART_INTEGRITY);
            RocketPartComposition comp = result.get(GCDataComponents.ROCKET_PART_COMPOSITION);
            helper.assertTrue(restored != null && restored.fraction() >= 0.999
                            && restored.damageState() == PartDamageState.NOMINAL,
                    "Expected integrity restored to 100% NOMINAL, got " + restored);
            helper.assertTrue(comp != null && comp.hasTrait(RocketMaterialTrait.ACID_RADIATION_SHIELD),
                    "Expected LEAD_INGOT coating to stamp ACID_RADIATION_SHIELD trait");
            helper.assertTrue(bench.energyStorage().getAmount() < AeroRefurbishmentBenchBlockEntity.ENERGY_CAPACITY,
                    "Expected refurbishment bench to consume electrical energy");
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM_ARENA, batch = "emergentPhysics", timeoutTicks = 200)
    public void testCryocoolerPoweredZerosLh2BoilOffOnLaunchPad(GameTestHelper helper) {
        BlockPos termRel = new BlockPos(8, 1, 13);
        helper.setBlock(termRel, GCBlocks.HANGAR_TERMINAL.defaultBlockState()
                .setValue(HangarTerminalBlock.FACING, Direction.SOUTH));

        HangarTerminalBlockEntity terminal = helper.getBlockEntity(termRel);
        helper.assertTrue(terminal != null, "Expected HangarTerminalBlockEntity");
        terminal.setPlatformType(PlatformType.VERTICAL);
        terminal.setPlatformSize(PlatformSize.SIZE_3X3);
        helper.assertTrue(terminal.assemblePlatform(true), "Creative platform assembly should succeed");

        // Build 3x3 tank + active cryocooler upgrade + battery bank on pad
        ItemStack tank = createPartStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK, null);
        ItemStack cryocooler = createPartStack("tank_upgrade_cryocooler_3x3_1b", RocketPartCategory.TANK, null);
        ItemStack battery = createPartStack("power_battery_bank_3x3_1b", RocketPartCategory.UTILITY, null);

        terminal.getRocketSlots().setItem(0, tank);
        terminal.getRocketSlots().setItem(1, cryocooler);
        terminal.getRocketSlots().setItem(2, battery);
        helper.assertTrue(terminal.buildRocketFromSlots(), "Rocket with cryocooler should assemble on pad");

        HangarTerminalBlockEntity.RocketPadSummary mounted = terminal.scanRocketOnPad();
        long initialLh2 = Math.min(81_000L * 10L, mounted.totalFuelDroplets());
        long initialEnergy = Math.min(20_000L, mounted.energyCapacity());
        helper.assertTrue(initialLh2 > 0L && initialEnergy >= 1000L,
                "Expected positive fuel & energy capacity, got fuelCap=" + mounted.totalFuelDroplets() + ", energyCap=" + mounted.energyCapacity());
        terminal.setRocketFuel(FluidVariant.of(GCFluids.LIQUID_HYDROGEN), initialLh2);
        terminal.setRocketEnergy(initialEnergy);

        // Phase 1: Powered cryocooler for 45 ticks (2 full 20-tick cycles) -> 0 LH2 lost, energy consumed
        helper.runAfterDelay(45, () -> {
            long fuelAfterPowered = terminal.getRocketFuelDroplets();
            long energyAfterPowered = terminal.getRocketEnergy();
            helper.assertTrue(fuelAfterPowered == initialLh2,
                    "Powered cryocooler must zero LH2 boil-off; expected " + initialLh2 + ", got " + fuelAfterPowered);
            helper.assertTrue(energyAfterPowered < initialEnergy,
                    "Powered cryocooler must draw electrical energy; expected < " + initialEnergy + ", got " + energyAfterPowered);

            // Phase 2: Cut power to 0 -> LH2 must begin boiling off
            terminal.setRocketEnergy(0L);
            helper.runAfterDelay(45, () -> {
                long fuelAfterUnpowered = terminal.getRocketFuelDroplets();
                helper.assertTrue(fuelAfterUnpowered < initialLh2,
                        "Unpowered cryocooler must allow LH2 boil-off; expected < " + initialLh2 + ", got " + fuelAfterUnpowered);
                helper.succeed();
            });
        });
    }

    @GameTest(template = PLATFORM_ARENA, batch = "emergentPhysics", timeoutTicks = 120)
    public void testJupiterTwrAndVenusWearOnAssembledPadRocket(GameTestHelper helper) {
        BlockPos termRel = new BlockPos(8, 1, 14);
        helper.setBlock(termRel, GCBlocks.HANGAR_TERMINAL.defaultBlockState()
                .setValue(HangarTerminalBlock.FACING, Direction.SOUTH));

        HangarTerminalBlockEntity terminal = helper.getBlockEntity(termRel);
        helper.assertTrue(terminal != null, "Expected HangarTerminalBlockEntity");
        terminal.setPlatformType(PlatformType.VERTICAL);
        terminal.setPlatformSize(PlatformSize.SIZE_5X5);
        helper.assertTrue(terminal.assemblePlatform(true), "Creative 5x5 platform assembly should succeed");

        RocketPartComposition steelComp = hullComp(GCItems.COMPRESSED_STEEL, RocketMaterialTrait.BRUTE_RIGIDITY);
        terminal.getRocketSlots().setItem(0, createPartStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE, steelComp));
        terminal.getRocketSlots().setItem(1, createPartStack("tank_methalox_bulkhead_5x5_3b", RocketPartCategory.TANK, steelComp));
        terminal.getRocketSlots().setItem(2, createPartStack("tank_methalox_bulkhead_5x5_3b", RocketPartCategory.TANK, steelComp));
        terminal.getRocketSlots().setItem(3, createPartStack("capsule_orion_5x5_3b", RocketPartCategory.COMMAND, steelComp));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Heavy RP-1 rocket should assemble");

        RocketFlightValidator.FlightValidationReport jupiterReport = terminal.evaluateFlight(PlanetaryEnvironment.JUPITER);
        helper.assertTrue(!jupiterReport.canLiftoffDestination() && jupiterReport.destinationTwr() <= 1.0,
                "Heavy steel RP-1 rocket must fail T/W on Jupiter (2.36G), got " + jupiterReport.destinationTwr());

        // Apply Venus exposure to assembled rocket and verify PartIntegrity degrades in-world
        double minIntegrityAfterVenus = terminal.applyEnvironmentalWearToMountedParts(PlanetaryEnvironment.VENUS, 10.0);
        helper.assertTrue(minIntegrityAfterVenus < 0.90,
                "Expected Venus environmental exposure to degrade assembled steel rocket integrity, got " + minIntegrityAfterVenus);
        helper.succeed();
    }
}
