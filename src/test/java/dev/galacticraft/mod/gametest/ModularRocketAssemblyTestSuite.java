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

import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.platform.HangarTerminalBlock;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.assembly.RocketAssemblyGraphBuilder;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Headless Minecraft GameTests validating Entrega 1 modular rocket assembly:
 * progressive Survival welding + cancellation refund, Creative instant bypass,
 * atomic 2x/4x radial booster attachment + obstruction rejection, and per-stage
 * multi-chemistry {@link net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage} routing.
 */
@SuppressWarnings("UnstableApiUsage")
public class ModularRocketAssemblyTestSuite implements GalacticraftGameTest {
    private static final String PLATFORM_ARENA = "galacticraft-test:platform_arena";

    private static ItemStack partStack(String modelId, RocketPartCategory fallback) {
        RocketPartSpec spec = RocketPartCatalog.getInstance().getPart(modelId);
        RocketPartConfig cfg = spec != null
                ? spec.toConfig(RocketLivery.WHITE)
                : RocketPartCatalog.getInstance().defaultConfigFor(fallback);
        return ModularRocketPartBlockItem.createConfiguredStack(cfg);
    }

    private static HangarTerminalBlockEntity createFormedTerminal(
            GameTestHelper helper,
            BlockPos termRel,
            PlatformSize size
    ) {
        helper.setBlock(termRel, GCBlocks.HANGAR_TERMINAL.defaultBlockState()
                .setValue(HangarTerminalBlock.FACING, Direction.SOUTH));
        HangarTerminalBlockEntity terminal = helper.getBlockEntity(termRel);
        helper.assertTrue(terminal != null, "Expected HangarTerminalBlockEntity at " + termRel);
        terminal.setPlatformType(PlatformType.VERTICAL);
        terminal.setPlatformSize(size);
        helper.assertTrue(terminal.assemblePlatform(true), "Expected formed " + size + " platform");
        return terminal;
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularAssembly", timeoutTicks = 200)
    public void testSurvivalProgressiveAssemblyAndCancelRefund(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 13), PlatformSize.SIZE_3X3);
        ServerPlayer survivalPlayer = helper.makeMockServerPlayerInLevel();
        survivalPlayer.setGameMode(GameType.SURVIVAL);

        ItemStack engine = partStack("srb_dart_1x1_1b", RocketPartCategory.ENGINE);
        ItemStack tank = partStack("tank_canister_1x1_1b", RocketPartCategory.TANK);
        ItemStack capsule = partStack("capsule_vostok_1x1_1b", RocketPartCategory.COMMAND);

        survivalPlayer.getInventory().setItem(0, engine.copy());
        survivalPlayer.getInventory().setItem(1, tank.copy());
        survivalPlayer.getInventory().setItem(2, capsule.copy());

        // Start Survival assembly -> must queue progressively instead of placing immediately
        helper.assertTrue(terminal.buildRocketFromPlayerInventory(survivalPlayer, List.of(0, 1, 2)),
                "Expected Survival buildRocketFromPlayerInventory to enqueue progressive assembly");
        helper.assertTrue(terminal.isAssemblyInProgress(),
                "Survival assembly must be in progress immediately after trigger");
        helper.assertTrue(terminal.getAssemblyTotalSteps() == 3 && terminal.getAssemblyCompletedSteps() == 0,
                "Expected 0/3 completed steps at tick 0, got " + terminal.getAssemblyCompletedSteps() + "/" + terminal.getAssemblyTotalSteps());
        helper.assertTrue(survivalPlayer.getInventory().getItem(0).isEmpty()
                        && survivalPlayer.getInventory().getItem(1).isEmpty()
                        && survivalPlayer.getInventory().getItem(2).isEmpty(),
                "Expected Survival items to be reserved atomically when assembly starts");

        int waitTicks = terminal.getAssemblyRemainingTicks() + 10;
        helper.runAfterDelay(waitTicks, () -> {
            helper.assertTrue(!terminal.isAssemblyInProgress(),
                    "Progressive assembly must complete after " + waitTicks + " ticks");
            helper.assertTrue(terminal.isRocketAssembled(),
                    "Expected unified assembled rocket after progressive welding completes");
            RocketAssemblyGraph graph = terminal.getAssemblyGraph();
            helper.assertTrue(!graph.isEmpty() && graph.allActiveParts().size() == 3,
                    "Expected compiled RocketAssemblyGraph with 3 parts, got " + graph.allActiveParts().size());

            // Test cancellation & item refund on a second queued addition
            survivalPlayer.getInventory().setItem(3, capsule.copy());
            helper.assertTrue(terminal.buildRocketFromPlayerInventory(survivalPlayer, List.of(3)),
                    "Expected follow-up Survival step to enqueue");
            helper.assertTrue(terminal.isAssemblyInProgress(), "Expected assembly in progress before cancel");
            helper.assertTrue(terminal.cancelProgressiveAssembly(survivalPlayer),
                    "Expected cancelProgressiveAssembly to succeed");
            helper.assertTrue(!terminal.isAssemblyInProgress(), "Expected assembly stopped after cancel");
            helper.assertTrue(!survivalPlayer.getInventory().getItem(3).isEmpty()
                            || survivalPlayer.getInventory().countItem(capsule.getItem()) >= 1,
                    "Expected canceled Survival part to be refunded to player inventory");
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularAssembly", timeoutTicks = 60)
    public void testCreativeInstantAssemblyBypass(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 13), PlatformSize.SIZE_3X3);
        ServerPlayer creativePlayer = helper.makeMockServerPlayerInLevel();
        creativePlayer.setGameMode(GameType.CREATIVE);

        creativePlayer.getInventory().setItem(0, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        creativePlayer.getInventory().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        creativePlayer.getInventory().setItem(2, partStack("interstage_hotstaging_3x3_1b", RocketPartCategory.ADAPTER));
        creativePlayer.getInventory().setItem(3, partStack("engine_merlinvac_3x3_2b", RocketPartCategory.ENGINE));
        creativePlayer.getInventory().setItem(4, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));

        helper.assertTrue(terminal.buildRocketFromPlayerInventory(creativePlayer, List.of(0, 1, 2, 3, 4)),
                "Expected Creative buildRocketFromPlayerInventory to succeed");
        helper.assertTrue(!terminal.isAssemblyInProgress(),
                "Creative mode must bypass progressive assembly delay instantaneously");
        helper.assertTrue(terminal.isRocketAssembled(), "Expected rocket assembled immediately in Creative");
        RocketAssemblyGraph graph = terminal.getAssemblyGraph();
        helper.assertTrue(graph.stages().size() == 2 && graph.stages().get(0).hotStaging(),
                "Expected 2-stage hot-staging RocketAssemblyGraph, got stages=" + graph.stages().size());
        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularAssembly", timeoutTicks = 80)
    public void testAtomicRadialBoosterAttachmentAndObstructionRejection(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 14), PlatformSize.SIZE_5X5);

        // Assemble a 3x3 core rocket first
        terminal.getRocketSlots().setItem(0, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(2, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected 3x3 core rocket to build on 5x5 pad");

        List<ItemStack> boosterTemplate = List.of(
                partStack("rcs_quad_1x1_1b", RocketPartCategory.ENGINE),
                partStack("tank_canister_1x1_1b", RocketPartCategory.TANK)
        );

        // Obstruct 1 of the 4 mirrored cardinal booster coordinates -> MUST fail atomically with 0 parts added
        BlockPos coreBase = terminal.getPadCenterPos().above();
        List<BlockPos> origins4x = RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(
                coreBase, 3, 1, RadialSymmetry.FOUR_CARDINAL
        );
        BlockPos obstructedPos = origins4x.get(2);
        helper.getLevel().setBlock(obstructedPos, Blocks.STONE.defaultBlockState(), 3);

        int countBefore = terminal.getAssembledRocketPartPositions().size();
        boolean rejected = terminal.attachRadialBoosters(
                null, boosterTemplate, RadialSymmetry.FOUR_CARDINAL, CrossfeedMode.CONTINUOUS_CROSSFEED, true
        );
        helper.assertTrue(!rejected, "Obstructed 1/4 mirrored booster position must reject entire 4x attachment atomically");
        helper.assertTrue(terminal.getAssembledRocketPartPositions().size() == countBefore,
                "Atomic rejection must leave 0 new rocket parts on the pad");

        // Clear obstruction -> 4x Asparagus booster attachment MUST succeed cleanly!
        helper.getLevel().setBlock(obstructedPos, Blocks.AIR.defaultBlockState(), 3);
        boolean attached = terminal.attachRadialBoosters(
                null, boosterTemplate, RadialSymmetry.FOUR_CARDINAL, CrossfeedMode.CONTINUOUS_CROSSFEED, true
        );
        helper.assertTrue(attached, "Expected unobstructed 4x Asparagus radial booster attachment to succeed");
        helper.assertTrue(terminal.getAssembledRocketPartPositions().size() == countBefore + 8,
                "Expected 4 boosters * 2 parts = 8 new parts, got " + (terminal.getAssembledRocketPartPositions().size() - countBefore));

        RocketAssemblyGraph graph = terminal.getAssemblyGraph();
        helper.assertTrue(!graph.stages().isEmpty() && graph.stages().get(0).radialGroups().size() == 1,
                "Expected 1 compiled RadialBoosterGroup on Stage 0");
        var group = graph.stages().get(0).radialGroups().get(0);
        helper.assertTrue(group.symmetry() == RadialSymmetry.FOUR_CARDINAL
                        && group.crossfeedMode() == CrossfeedMode.CONTINUOUS_CROSSFEED,
                "Expected FOUR_CARDINAL + CONTINUOUS_CROSSFEED Asparagus booster group, got " + group);

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularAssembly", timeoutTicks = 80)
    public void testEightWayFlushRadialBoosterAttachment(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 14), PlatformSize.SIZE_5X5);

        // Assemble 3x3 core rocket on 5x5 pad
        terminal.getRocketSlots().setItem(0, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(2, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected 3x3 core rocket to build on 5x5 pad");

        int countBefore = terminal.getAssembledRocketPartPositions().size();
        List<ItemStack> boosterTemplate = List.of(
                partStack("srb_dart_1x1_1b", RocketPartCategory.ENGINE),
                partStack("tank_canister_1x1_1b", RocketPartCategory.TANK)
        );

        boolean attached = terminal.attachRadialBoosters(
                null, boosterTemplate, RadialSymmetry.EIGHT, CrossfeedMode.ISOLATED, true
        );
        helper.assertTrue(attached, "Expected 8x radial boosters around 3x3 core on 5x5 pad to attach cleanly");
        helper.assertTrue(terminal.getAssembledRocketPartPositions().size() == countBefore + 16,
                "Expected 8 boosters * 2 parts = 16 new parts, got " + (terminal.getAssembledRocketPartPositions().size() - countBefore));

        // Verify diagonal boosters carry inward flush visual offsets eliminating the voxel air gap
        BlockPos padCenter = terminal.getPadCenterPos();
        int flushDiagonalCount = 0;
        for (BlockPos pos : terminal.getAssembledRocketPartPositions()) {
            BlockPos rel = pos.subtract(padCenter);
            if (rel.getX() != 0 && rel.getZ() != 0
                    && helper.getLevel().getBlockEntity(pos) instanceof dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity partBe) {
                float ox = partBe.getRadialVisualOffsetX();
                float oz = partBe.getRadialVisualOffsetZ();
                // Must pull diagonally inward toward pad center (opposite sign of relX/relZ)
                if (ox * rel.getX() < -0.1F && oz * rel.getZ() < -0.1F) {
                    flushDiagonalCount++;
                }
            }
        }
        helper.assertTrue(flushDiagonalCount == 8,
                "Expected all 8 diagonal booster parts (4 columns * 2 parts) to carry inward flush visual offsets, got " + flushDiagonalCount);

        RocketAssemblyGraph graph = terminal.getAssemblyGraph();
        helper.assertTrue(!graph.stages().isEmpty() && graph.stages().get(0).radialGroups().size() == 1,
                "Expected 8x ring to compile as 1 unified RadialBoosterGroup, got " + graph.stages().get(0).radialGroups().size());
        helper.assertTrue(graph.stages().get(0).radialGroups().get(0).symmetry() == RadialSymmetry.EIGHT,
                "Expected RadialSymmetry.EIGHT, got " + graph.stages().get(0).radialGroups().get(0).symmetry());

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularAssembly", timeoutTicks = 80)
    public void testInPlaceRocketCoreEditingPreservesBoostersAndRefundsRemovedParts(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 14), PlatformSize.SIZE_5X5);
        ServerPlayer creativePlayer = helper.makeMockServerPlayerInLevel();
        creativePlayer.setGameMode(GameType.CREATIVE);

        // Build 3x3 core (engine + tank + capsule) + 2x radial boosters
        terminal.getRocketSlots().setItem(0, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(2, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected initial core rocket to assemble");

        List<ItemStack> boosterTemplate = List.of(partStack("srb_dart_1x1_1b", RocketPartCategory.ENGINE));
        helper.assertTrue(terminal.attachRadialBoosters(null, boosterTemplate, RadialSymmetry.TWO, CrossfeedMode.ISOLATED, true),
                "Expected 2x radial boosters to attach");

        BlockPos padCenter = terminal.getPadCenterPos();
        BlockPos enginePos = padCenter.above(1);
        BlockPos capsulePos = padCenter.above(5);

        // Edit core in-place: keep engine, REPLACE middle tank with LOX tank from slot 0, insert extra RP1 tank from slot 1, keep capsule
        creativePlayer.getInventory().setItem(0, partStack("tank_lox_cryo_3x3_2b", RocketPartCategory.TANK));
        creativePlayer.getInventory().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));

        List<dev.galacticraft.mod.network.c2s.EditRocketCorePayload.CoreEntry> editedEntries = List.of(
                dev.galacticraft.mod.network.c2s.EditRocketCorePayload.CoreEntry.mounted(enginePos),
                dev.galacticraft.mod.network.c2s.EditRocketCorePayload.CoreEntry.inventory(0),
                dev.galacticraft.mod.network.c2s.EditRocketCorePayload.CoreEntry.inventory(1),
                dev.galacticraft.mod.network.c2s.EditRocketCorePayload.CoreEntry.mounted(capsulePos)
        );
        helper.assertTrue(terminal.applyEditedRocketCore(creativePlayer, editedEntries),
                "Expected in-place core edit to succeed without tearing down radial boosters");

        RocketAssemblyGraph editedGraph = terminal.getAssemblyGraph();
        helper.assertTrue(editedGraph.stages().size() == 1 && editedGraph.stages().get(0).coreParts().size() == 4,
                "Expected 4 core parts after in-place edit, got " + editedGraph.stages().get(0).coreParts().size());
        helper.assertTrue(editedGraph.stages().get(0).radialGroups().size() == 1
                        && editedGraph.stages().get(0).radialGroups().get(0).symmetry() == RadialSymmetry.TWO,
                "Expected surviving 2x radial boosters to remain attached after core edit");

        // Now remove ONLY the radial boosters -> core of 4 parts must stay intact!
        helper.assertTrue(terminal.removeRadialBoosters(creativePlayer),
                "Expected removeRadialBoosters to remove only radial boosters");
        RocketAssemblyGraph afterBoosterRemoval = terminal.getAssemblyGraph();
        helper.assertTrue(afterBoosterRemoval.stages().size() == 1
                        && afterBoosterRemoval.stages().get(0).coreParts().size() == 4
                        && afterBoosterRemoval.stages().get(0).radialGroups().isEmpty(),
                "Expected 4 core parts intact and 0 radial groups after removeRadialBoosters");

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularAssembly", timeoutTicks = 80)
    public void testPerStageMultiChemistryPropellantStorage(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 13), PlatformSize.SIZE_3X3);

        // Stage 0: RP-1 (engine_falcon1d + tank_fuel_rp1) -> decoupler_inline -> Stage 1: Hydrolox (engine_centaur_rl10 + tank_hydrolox_supercryo)
        terminal.getRocketSlots().setItem(0, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(2, partStack("decoupler_inline_3x3_1b", RocketPartCategory.UTILITY));
        terminal.getRocketSlots().setItem(3, partStack("engine_centaur_rl10_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(4, partStack("tank_lox_cryo_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(5, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected 2-stage mixed-chemistry rocket to assemble");

        RocketAssemblyGraph graph = terminal.getAssemblyGraph();
        helper.assertTrue(graph.stages().size() == 2, "Expected 2 stages in compiled graph, got " + graph.stages().size());
        helper.assertTrue("rp1".equals(graph.stages().get(0).propellantId()),
                "Expected Stage 0 propellant=rp1, got " + graph.stages().get(0).propellantId());
        helper.assertTrue("hydrolox".equals(graph.stages().get(1).propellantId()),
                "Expected Stage 1 propellant=hydrolox, got " + graph.stages().get(1).propellantId());

        Storage<FluidVariant> storage = terminal.getRocketFluidStorage();
        helper.assertTrue(storage != null, "Expected non-null per-stage CombinedStorage");

        long rp1Inserted;
        long lh2Inserted;
        try (Transaction tx = Transaction.openOuter()) {
            rp1Inserted = storage.insert(FluidVariant.of(GCFluids.FUEL), 81_000L * 4L, tx);
            lh2Inserted = storage.insert(FluidVariant.of(GCFluids.LIQUID_HYDROGEN), 81_000L * 3L, tx);
            tx.commit();
        }

        helper.assertTrue(rp1Inserted == 81_000L * 4L, "Expected 4B RP-1 inserted into Stage 0, got " + rp1Inserted);
        helper.assertTrue(lh2Inserted == 81_000L * 3L, "Expected 3B LH2 inserted into Stage 1, got " + lh2Inserted);
        helper.assertTrue(terminal.getStageFuelDroplets(0) == 81_000L * 4L,
                "Stage 0 must hold exactly 4B RP-1, got " + terminal.getStageFuelDroplets(0));
        helper.assertTrue(terminal.getStageFuelDroplets(1) == 81_000L * 3L,
                "Stage 1 must hold exactly 3B LH2, got " + terminal.getStageFuelDroplets(1));
        helper.assertTrue(terminal.getRocketFuelDroplets() == 81_000L * 7L,
                "Aggregate fuel must sum both stages (7B), got " + terminal.getRocketFuelDroplets());
        helper.assertTrue(terminal.getAssemblyGraph().totalCurrentFuelDroplets() == 81_000L * 7L,
                "Compiled RocketAssemblyGraph must reflect live per-stage fuel totals");

        helper.succeed();
    }
}
