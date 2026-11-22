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

import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.multiblock.MultiVoxelMaster;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.mod.attachments.GCAttachments;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.attachments.MultiblockLinksAttachment;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.block.multiblock.AbstractBoundsProxyBlock;
import dev.galacticraft.mod.content.block.platform.HangarTerminalBlock;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprint;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprintRegistry;
import dev.galacticraft.mod.content.block.platform.PlatformCollisionRegistry;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import team.reborn.energy.api.EnergyStorage;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class MultiblockPlatformTestSuite implements GalacticraftGameTest {
    private static final String PLATFORM_ARENA = "galacticraft-test:platform_arena";

    private static final BlockPos VERTICAL_PAD_CENTER_REL = new BlockPos(8, 1, 8);
    private static final BlockPos VERTICAL_TERMINAL_REL = new BlockPos(8, 1, 10);

    @GameTest(template = PLATFORM_ARENA)
    public void testChunkAttachmentLinkAndUnlink(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos proxyRel1 = new BlockPos(4, 1, 4);
        BlockPos proxyRel2 = proxyRel1.above();
        BlockPos masterRel = new BlockPos(6, 1, 4);

        helper.setBlock(proxyRel1, GCBlocks.PLATFORM_PROXY.defaultBlockState().setValue(AbstractBoundsProxyBlock.SHAPE, BoundsShape.SLAB_8PX));
        helper.setBlock(proxyRel2, GCBlocks.PLATFORM_PROXY.defaultBlockState().setValue(AbstractBoundsProxyBlock.SHAPE, BoundsShape.FULL));
        helper.setBlock(masterRel, GCBlocks.HANGAR_TERMINAL.defaultBlockState());

        BlockPos proxyAbs1 = helper.absolutePos(proxyRel1);
        BlockPos proxyAbs2 = helper.absolutePos(proxyRel2);
        BlockPos masterAbs = helper.absolutePos(masterRel);

        helper.assertTrue(MultiblockLinkManager.getMasterPos(level, proxyAbs1) == null,
                "Unlinked proxy must have null masterPos");
        helper.assertTrue(MultiblockLinkManager.getMaster(level, proxyAbs1) == null,
                "Unlinked proxy must have null MultiVoxelMaster");

        MultiblockLinkManager.link(level, proxyAbs1, masterAbs);
        MultiblockLinkManager.link(level, proxyAbs2, masterAbs);

        helper.assertTrue(masterAbs.equals(MultiblockLinkManager.getMasterPos(level, proxyAbs1)),
                "Expected proxy1 masterPos to resolve to " + masterAbs);
        helper.assertTrue(masterAbs.equals(MultiblockLinkManager.getMasterPos(level, proxyAbs2)),
                "Expected proxy2 masterPos to resolve to " + masterAbs);

        MultiVoxelMaster resolvedMaster = MultiblockLinkManager.getMaster(level, proxyAbs1);
        helper.assertTrue(resolvedMaster instanceof HangarTerminalBlockEntity,
                "Expected getMaster() to resolve HangarTerminalBlockEntity, got " + resolvedMaster);
        helper.assertTrue(masterAbs.equals(resolvedMaster.getMasterPos()),
                "Expected resolved master's getMasterPos() to equal " + masterAbs);

        LevelChunk chunk = level.getChunkAt(proxyAbs1);
        MultiblockLinksAttachment attached = chunk.getAttached(GCAttachments.MULTIBLOCK_LINKS);
        helper.assertTrue(attached != null && !attached.isEmpty(),
                "Expected MULTIBLOCK_LINKS chunk attachment to be present after linking");
        helper.assertTrue(attached.containsProxy(proxyAbs1.asLong()) && attached.containsProxy(proxyAbs2.asLong()),
                "Expected chunk attachment to contain both linked proxy coordinates");
        helper.assertTrue(masterAbs.equals(attached.getMasterPos(proxyAbs1)),
                "Expected chunk attachment entry for proxy1 to point at master");

        // Unlinking proxy1 must leave proxy2 intact in the same chunk
        MultiblockLinkManager.unlink(level, proxyAbs1);
        helper.assertTrue(MultiblockLinkManager.getMasterPos(level, proxyAbs1) == null,
                "Expected proxy1 masterPos to be null after unlink");
        helper.assertTrue(masterAbs.equals(MultiblockLinkManager.getMasterPos(level, proxyAbs2)),
                "Expected proxy2 link to remain after unlinking proxy1");

        // Unlinking the final proxy in this chunk must clean up the attachment completely
        MultiblockLinkManager.unlink(level, proxyAbs2);
        helper.assertTrue(MultiblockLinkManager.getMasterPos(level, proxyAbs2) == null,
                "Expected proxy2 masterPos to be null after unlink");
        MultiblockLinksAttachment afterCleanup = chunk.getAttached(GCAttachments.MULTIBLOCK_LINKS);
        helper.assertTrue(afterCleanup == null || (!afterCleanup.containsProxy(proxyAbs1.asLong()) && !afterCleanup.containsProxy(proxyAbs2.asLong())),
                "Expected chunk attachment to remove unlinked proxy entries cleanly");

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testPadAutoDetectionVertical1x1(GameTestHelper helper) {
        BlockPos padCenterRel = new BlockPos(7, 1, 7);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(padCenterRel.offset(dx, 0, dz), GCBlocks.REINFORCED_LAUNCH_PAD.defaultBlockState());
            }
        }

        BlockPos terminalRel = new BlockPos(7, 1, 9);
        helper.setBlock(terminalRel, GCBlocks.HANGAR_TERMINAL.defaultBlockState().setValue(HangarTerminalBlock.FACING, Direction.SOUTH));

        HangarTerminalBlockEntity terminal = requireTerminal(helper, terminalRel);
        boolean detected = terminal.autoDetectPad();
        helper.assertTrue(detected, "Expected autoDetectPad() to detect adjacent 3x3 reinforced launch pad deck");
        helper.assertTrue(terminal.getDetectedPadSpan() == 3,
                "Expected detectedPadSpan == 3, got " + terminal.getDetectedPadSpan());
        helper.assertTrue(terminal.getPlatformType() == PlatformType.VERTICAL,
                "Expected platformType == VERTICAL, got " + terminal.getPlatformType());
        helper.assertTrue(terminal.getPlatformSize() == PlatformSize.SIZE_1X1,
                "Expected platformSize == SIZE_1X1, got " + terminal.getPlatformSize());

        BlockPos expectedCenterAbs = helper.absolutePos(padCenterRel);
        helper.assertTrue(expectedCenterAbs.equals(terminal.getPadCenterPos()),
                "Expected padCenter " + expectedCenterAbs + ", got " + terminal.getPadCenterPos());

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testPadAutoDetectionHorizontal1x1(GameTestHelper helper) {
        BlockPos padCenterRel = new BlockPos(7, 1, 7);
        // 3x9 horizontal launch pad deck: X in [6..8], Z in [3..11]
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                helper.setBlock(padCenterRel.offset(dx, 0, dz), GCBlocks.REINFORCED_LAUNCH_PAD.defaultBlockState());
            }
        }

        BlockPos terminalRel = new BlockPos(5, 1, 7);
        helper.setBlock(terminalRel, GCBlocks.HANGAR_TERMINAL.defaultBlockState().setValue(HangarTerminalBlock.FACING, Direction.WEST));

        HangarTerminalBlockEntity terminal = requireTerminal(helper, terminalRel);
        boolean detected = terminal.autoDetectPad();
        helper.assertTrue(detected, "Expected autoDetectPad() to detect adjacent 3x9 horizontal launch pad deck");
        helper.assertTrue(terminal.getPlatformType() == PlatformType.HORIZONTAL,
                "Expected platformType == HORIZONTAL for 3x9 deck, got " + terminal.getPlatformType());
        helper.assertTrue(terminal.getPlatformSize() == PlatformSize.SIZE_1X1,
                "Expected platformSize == SIZE_1X1 for 3x9 deck, got " + terminal.getPlatformSize());
        helper.assertTrue(terminal.getDetectedPadSpan() == 9,
                "Expected detectedPadSpan == 9 for 3x9 deck, got " + terminal.getDetectedPadSpan());

        BlockPos expectedCenterAbs = helper.absolutePos(padCenterRel);
        helper.assertTrue(expectedCenterAbs.equals(terminal.getPadCenterPos()),
                "Expected padCenter " + expectedCenterAbs + ", got " + terminal.getPadCenterPos());

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testVertical1x1AssembleAndDisassemble(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction facing = Direction.NORTH;
        PlatformBlueprint blueprint = PlatformBlueprintRegistry.VERTICAL_1X1;

        HangarTerminalBlockEntity terminal = setupVertical1x1Skeleton(helper, VERTICAL_PAD_CENTER_REL, VERTICAL_TERMINAL_REL, facing);
        BlockPos padCenterAbs = helper.absolutePos(VERTICAL_PAD_CENTER_REL);
        BlockPos terminalAbs = helper.absolutePos(VERTICAL_TERMINAL_REL);

        PlatformBlueprint.ValidationSummary preSummary = terminal.validateStructure();
        helper.assertTrue(preSummary.readyToAssemble(),
                "Expected complete VERTICAL 1x1 skeleton to be readyToAssemble (missing="
                        + preSummary.missingCount() + ", wrong=" + preSummary.wrongCount()
                        + ", blocked=" + preSummary.clearanceBlockedCount() + ")");
        helper.assertTrue(preSummary.totalPlaced() == blueprint.entries().size(),
                "Expected all " + blueprint.entries().size() + " blueprint blocks to be counted as placed");

        Map<BlockPos, BlockState> originalStates = captureBlueprintStates(level, blueprint, padCenterAbs, facing);

        boolean assembled = terminal.assemblePlatform();
        helper.assertTrue(assembled, "Expected assemblePlatform() to succeed on valid VERTICAL 1x1 skeleton");
        helper.assertTrue(terminal.isFormed(), "Expected terminal.isFormed() == true after assembly");
        helper.assertTrue(level.getBlockState(terminalAbs).getValue(HangarTerminalBlock.ACTIVE),
                "Expected terminal block state ACTIVE == true after assembly");
        helper.assertTrue(!terminal.assemblePlatform(),
                "Expected second assemblePlatform() call while already formed to return false");

        java.util.List<PlatformCollisionRegistry.CollisionVoxel> collisionVoxels =
                PlatformCollisionRegistry.get(PlatformType.VERTICAL, PlatformSize.SIZE_1X1);
        helper.assertTrue(!collisionVoxels.isEmpty(), "Expected VERTICAL 1x1 collision voxel map to be non-empty");

        for (PlatformCollisionRegistry.CollisionVoxel voxel : collisionVoxels) {
            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, voxel.localPos());
            if (worldPos.equals(terminalAbs) || level.getBlockState(worldPos).is(Blocks.BARRIER)) {
                continue;
            }
            BlockState proxyState = level.getBlockState(worldPos);
            helper.assertTrue(proxyState.is(GCBlocks.PLATFORM_PROXY),
                    "Expected PLATFORM_PROXY at collision voxel " + worldPos + ", got " + proxyState);

            BoundsShape actualShape = proxyState.getValue(AbstractBoundsProxyBlock.SHAPE);
            helper.assertTrue(actualShape == voxel.shape(),
                    "Expected BoundsShape " + voxel.shape() + " at " + worldPos + ", got " + actualShape);

            BlockPos linkedMaster = MultiblockLinkManager.getMasterPos(level, worldPos);
            helper.assertTrue(terminalAbs.equals(linkedMaster),
                    "Expected MultiblockLinkManager masterPos at " + worldPos + " to equal terminal " + terminalAbs + ", got " + linkedMaster);
            helper.assertTrue(terminal == MultiblockLinkManager.getMaster(level, worldPos),
                    "Expected MultiblockLinkManager.getMaster() to return terminal at " + worldPos);
            helper.assertTrue(terminal.isPartOfStructure(worldPos),
                    "Expected terminal.isPartOfStructure(" + worldPos + ") == true while formed");
        }

        // Rocket bay directly above pad center must remain clear air for rocket assembly
        helper.assertTrue(level.getBlockState(padCenterAbs.above()).isAir(),
                "Expected rocket bay above pad center to remain clear AIR");

        terminal.disassemblePlatform(null, false);

        helper.assertTrue(!terminal.isFormed(), "Expected terminal.isFormed() == false after disassembly");
        helper.assertTrue(!level.getBlockState(terminalAbs).getValue(HangarTerminalBlock.ACTIVE),
                "Expected terminal block state ACTIVE == false after disassembly");

        for (PlatformBlueprint.Entry entry : blueprint.entries()) {
            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, entry.localPos());
            BlockState expectedOriginal = originalStates.get(worldPos);
            BlockState actualRestored = level.getBlockState(worldPos);
            helper.assertTrue(actualRestored.equals(expectedOriginal),
                    "Expected exact original BlockState " + expectedOriginal + " at " + worldPos + ", got " + actualRestored);
            helper.assertTrue(MultiblockLinkManager.getMasterPos(level, worldPos) == null,
                    "Expected MultiblockLinkManager link at " + worldPos + " to be removed after disassembly");
            helper.assertTrue(!terminal.isPartOfStructure(worldPos),
                    "Expected terminal.isPartOfStructure(" + worldPos + ") == false after disassembly");
        }

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testBreakingProxyUnformsAndRestoresRemainingBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction facing = Direction.NORTH;
        PlatformBlueprint blueprint = PlatformBlueprintRegistry.VERTICAL_1X1;

        HangarTerminalBlockEntity terminal = setupVertical1x1Skeleton(helper, VERTICAL_PAD_CENTER_REL, VERTICAL_TERMINAL_REL, facing);
        BlockPos padCenterAbs = helper.absolutePos(VERTICAL_PAD_CENTER_REL);
        BlockPos terminalAbs = helper.absolutePos(VERTICAL_TERMINAL_REL);
        Map<BlockPos, BlockState> originalStates = captureBlueprintStates(level, blueprint, padCenterAbs, facing);

        helper.assertTrue(terminal.assemblePlatform(), "Expected VERTICAL 1x1 platform to assemble before break test");
        helper.assertTrue(terminal.isFormed(), "Expected terminal to be formed");

        // Target a pad proxy cell guaranteed to exist in both blueprint and collision map
        BlockPos brokenProxyPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, new BlockPos(0, 0, 0));
        helper.assertTrue(level.getBlockState(brokenProxyPos).is(GCBlocks.PLATFORM_PROXY),
                "Expected target break cell " + brokenProxyPos + " to be PLATFORM_PROXY");

        MultiVoxelMaster master = MultiblockLinkManager.getMaster(level, brokenProxyPos);
        helper.assertTrue(master == terminal,
                "Expected MultiblockLinkManager.getMaster() to resolve terminal for broken proxy");

        master.onProxyBroken(level, brokenProxyPos, null, false);

        helper.assertTrue(!terminal.isFormed(), "Expected terminal.isFormed() == false after proxy broken");
        helper.assertTrue(!level.getBlockState(terminalAbs).getValue(HangarTerminalBlock.ACTIVE),
                "Expected terminal ACTIVE == false after proxy broken");
        helper.assertTrue(level.getBlockState(brokenProxyPos).isAir(),
                "Expected broken proxy position " + brokenProxyPos + " to become AIR, got " + level.getBlockState(brokenProxyPos));

        for (PlatformBlueprint.Entry entry : blueprint.entries()) {
            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, entry.localPos());
            helper.assertTrue(MultiblockLinkManager.getMasterPos(level, worldPos) == null,
                    "Expected zero stale proxy links at " + worldPos + " after proxy break");
            helper.assertTrue(MultiblockLinkManager.getMaster(level, worldPos) == null,
                    "Expected null master lookup at " + worldPos + " after proxy break");
            if (!worldPos.equals(brokenProxyPos)) {
                BlockState expectedOriginal = originalStates.get(worldPos);
                BlockState actualState = level.getBlockState(worldPos);
                helper.assertTrue(actualState.getBlock() == expectedOriginal.getBlock(),
                        "Expected remaining cell " + worldPos + " to restore block " + expectedOriginal.getBlock() + ", got " + actualState);
            }
        }

        PlatformBlueprint.ValidationSummary postBreak = terminal.validateStructure();
        helper.assertTrue(postBreak.missingCount() == 1 && !postBreak.readyToAssemble(),
                "Expected exactly 1 missing block after breaking one proxy, got missing=" + postBreak.missingCount());

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testClearanceObstructionPreventsAssembly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction facing = Direction.NORTH;
        PlatformBlueprint blueprint = PlatformBlueprintRegistry.VERTICAL_1X1;

        HangarTerminalBlockEntity terminal = setupVertical1x1Skeleton(helper, VERTICAL_PAD_CENTER_REL, VERTICAL_TERMINAL_REL, facing);
        BlockPos padCenterAbs = helper.absolutePos(VERTICAL_PAD_CENTER_REL);
        BlockPos terminalAbs = helper.absolutePos(VERTICAL_TERMINAL_REL);
        Map<BlockPos, BlockState> originalStates = captureBlueprintStates(level, blueprint, padCenterAbs, facing);

        helper.assertTrue(terminal.validateStructure().readyToAssemble(),
                "Expected unobstructed VERTICAL 1x1 skeleton to be ready before placing obstruction");

        // Place a solid stone block inside the rocket clearance volume (-1..1, 1..5, -1..1)
        BlockPos obstructionPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, new BlockPos(0, 2, 0));
        level.setBlock(obstructionPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        PlatformBlueprint.ValidationSummary blockedSummary = terminal.validateStructure();
        helper.assertTrue(blockedSummary.clearanceBlockedCount() > 0,
                "Expected clearanceBlockedCount > 0 with stone inside rocket bay, got " + blockedSummary.clearanceBlockedCount());
        helper.assertTrue(blockedSummary.clearanceBlockedCount() == 1,
                "Expected exactly 1 blocked clearance voxel, got " + blockedSummary.clearanceBlockedCount());
        helper.assertTrue(blockedSummary.missingCount() == 0 && blockedSummary.wrongCount() == 0,
                "Expected zero missing/wrong skeleton blocks when only clearance is obstructed");
        helper.assertTrue(!blockedSummary.readyToAssemble(),
                "Expected readyToAssemble() == false while clearance is obstructed");

        boolean assembled = terminal.assemblePlatform();
        helper.assertTrue(!assembled, "Expected assemblePlatform() to return false when clearance is obstructed");
        helper.assertTrue(!terminal.isFormed(), "Expected terminal to remain unformed after blocked assembly attempt");
        helper.assertTrue(!level.getBlockState(terminalAbs).getValue(HangarTerminalBlock.ACTIVE),
                "Expected terminal ACTIVE == false after blocked assembly attempt");

        for (PlatformBlueprint.Entry entry : blueprint.entries()) {
            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, entry.localPos());
            helper.assertTrue(level.getBlockState(worldPos).equals(originalStates.get(worldPos)),
                    "Expected skeleton block at " + worldPos + " to remain untouched after failed assembly");
            helper.assertTrue(MultiblockLinkManager.getMasterPos(level, worldPos) == null,
                    "Expected no proxy links created at " + worldPos + " after failed assembly");
        }

        // Clearing the obstruction must restore readyToAssemble() == true
        level.setBlock(obstructionPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        PlatformBlueprint.ValidationSummary clearedSummary = terminal.validateStructure();
        helper.assertTrue(clearedSummary.clearanceBlockedCount() == 0 && clearedSummary.readyToAssemble(),
                "Expected removing stone obstruction to restore readyToAssemble() == true");

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testCreativeAssemblyWithoutBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction facing = Direction.NORTH;
        PlatformBlueprint blueprint = PlatformBlueprintRegistry.VERTICAL_1X1;

        helper.setBlock(VERTICAL_TERMINAL_REL, GCBlocks.HANGAR_TERMINAL.defaultBlockState().setValue(HangarTerminalBlock.FACING, facing));
        HangarTerminalBlockEntity terminal = requireTerminal(helper, VERTICAL_TERMINAL_REL);
        terminal.setPlatformType(PlatformType.VERTICAL);
        terminal.setPlatformSize(PlatformSize.SIZE_1X1);

        // Without blocks placed, normal survival assembly must fail
        helper.assertTrue(!terminal.validateStructure().readyToAssemble(),
                "Expected empty arena not to be readyToAssemble in survival");
        helper.assertTrue(!terminal.assemblePlatform(false),
                "Expected survival assemblePlatform(false) to fail when blocks are missing");

        // Creative bypass must succeed without placing any blocks
        helper.assertTrue(terminal.assemblePlatform(true),
                "Expected creative assemblePlatform(true) to succeed without placing blocks");
        helper.assertTrue(terminal.isFormed(), "Expected terminal to be formed after creative assembly");

        BlockPos padCenterAbs = terminal.getPadCenterPos();
        helper.assertTrue(level.getBlockState(padCenterAbs).is(GCBlocks.PLATFORM_PROXY),
                "Expected PLATFORM_PROXY at pad center after creative assembly");

        // Disassembling must restore the canonical structural blueprint blocks
        terminal.disassemblePlatform(null, false);
        helper.assertTrue(!terminal.isFormed(), "Expected terminal to be unformed after disassembly");
        for (PlatformBlueprint.Entry entry : blueprint.entries()) {
            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, entry.localPos());
            if (worldPos.equals(helper.absolutePos(VERTICAL_TERMINAL_REL))) {
                continue;
            }
            helper.assertTrue(entry.role().matches(level.getBlockState(worldPos)),
                    "Expected restored structural block for role " + entry.role() + " at " + worldPos);
        }

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testBuildAndRecallRocketFromTerminalSlots(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction facing = Direction.NORTH;
        HangarTerminalBlockEntity terminal = setupVertical1x1Skeleton(helper, VERTICAL_PAD_CENTER_REL, VERTICAL_TERMINAL_REL, facing);
        BlockPos padCenterAbs = helper.absolutePos(VERTICAL_PAD_CENTER_REL);

        helper.assertTrue(terminal.assemblePlatform(), "Expected VERTICAL 1x1 platform to assemble");

        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        dev.galacticraft.mod.content.rocket.part.RocketPartSpec engineSpec = catalog.allParts().stream()
                .filter(p -> p.category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.ENGINE && p.diameter() == 1)
                .findFirst().orElseThrow();
        dev.galacticraft.mod.content.rocket.part.RocketPartSpec tankSpec = catalog.allParts().stream()
                .filter(p -> p.category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.TANK && p.diameter() == 1)
                .findFirst().orElseThrow();
        dev.galacticraft.mod.content.rocket.part.RocketPartSpec cmdSpec = catalog.allParts().stream()
                .filter(p -> p.category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.COMMAND && p.diameter() == 1)
                .findFirst().orElseThrow();

        ItemStack engine = ModularRocketPartBlockItem.createConfiguredStack(engineSpec.toConfig(RocketLivery.WHITE));
        ItemStack tank = ModularRocketPartBlockItem.createConfiguredStack(tankSpec.toConfig(RocketLivery.WHITE));
        ItemStack capsule = ModularRocketPartBlockItem.createConfiguredStack(cmdSpec.toConfig(RocketLivery.WHITE));

        terminal.getRocketSlots().setItem(0, engine);
        terminal.getRocketSlots().setItem(1, tank);
        terminal.getRocketSlots().setItem(2, capsule);

        HangarTerminalBlockEntity.RocketPadSummary preview = terminal.previewRocketFromSlots();
        helper.assertTrue(preview.parts().size() == 3 && preview.hasEngine() && preview.hasTank() && preview.hasCommand(),
                "Expected slot preview to detect all 3 staged rocket parts");

        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected buildRocketFromSlots() to stack rocket onto pad");
        helper.assertTrue(!terminal.hasStagedRocketParts(), "Expected rocketSlots to be emptied after building");

        HangarTerminalBlockEntity.RocketPadSummary mounted = terminal.scanRocketOnPad();
        helper.assertTrue(mounted.parts().size() == 3, "Expected 3 mounted parts on pad, got " + mounted.parts().size());
        int tankY = 1 + engineSpec.heightBlocks();
        int cmdY = tankY + tankSpec.heightBlocks();
        helper.assertTrue(level.getBlockEntity(padCenterAbs.above(1)) != null, "Expected engine at pad+1");
        helper.assertTrue(level.getBlockEntity(padCenterAbs.above(tankY)) != null, "Expected tank at pad+" + tankY);
        helper.assertTrue(level.getBlockEntity(padCenterAbs.above(cmdY)) != null, "Expected command probe at pad+" + cmdY);

        helper.assertTrue(terminal.recallRocketToSlots(null), "Expected recallRocketToSlots() to retrieve mounted rocket");
        helper.assertTrue(terminal.scanRocketOnPad().parts().isEmpty(), "Expected pad to be empty after recall");
        helper.assertTrue(terminal.hasStagedRocketParts(), "Expected recalled parts back in rocketSlots");

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testPlatformProxiesDoNotOpenMenuOnlyTerminalClickable(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction facing = Direction.NORTH;
        HangarTerminalBlockEntity terminal = setupVertical1x1Skeleton(helper, VERTICAL_PAD_CENTER_REL, VERTICAL_TERMINAL_REL, facing);
        BlockPos padCenterAbs = helper.absolutePos(VERTICAL_PAD_CENTER_REL);
        BlockPos terminalAbs = helper.absolutePos(VERTICAL_TERMINAL_REL);

        helper.assertTrue(terminal.assemblePlatform(), "Expected VERTICAL 1x1 platform to assemble");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // Right-clicking platform proxy voxels must return PASS (never open the terminal menu)
        BlockPos proxyPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, new BlockPos(0, 0, 0));
        BlockState proxyState = level.getBlockState(proxyPos);
        helper.assertTrue(proxyState.is(GCBlocks.PLATFORM_PROXY), "Expected PLATFORM_PROXY at pad center");
        BlockHitResult proxyHit = new BlockHitResult(Vec3.atCenterOf(proxyPos), Direction.UP, proxyPos, false);

        InteractionResult proxyResult = proxyState.useWithoutItem(level, player, proxyHit);
        helper.assertTrue(proxyResult == InteractionResult.PASS,
                "Expected clicking PLATFORM_PROXY to return PASS (not clickable), got " + proxyResult);
        helper.assertTrue(terminal.onProxyUse(level, proxyPos, player, proxyHit) == InteractionResult.PASS,
                "Expected terminal.onProxyUse() to return PASS");

        // Right-clicking the HangarTerminalBlock itself must still be clickable and return CONSUME
        BlockState terminalState = level.getBlockState(terminalAbs);
        BlockHitResult terminalHit = new BlockHitResult(Vec3.atCenterOf(terminalAbs), Direction.NORTH, terminalAbs, false);
        InteractionResult terminalResult = terminalState.useWithoutItem(level, player, terminalHit);
        helper.assertTrue(terminalResult == InteractionResult.CONSUME,
                "Expected clicking HANGAR_TERMINAL to return CONSUME, got " + terminalResult);

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testAssembledRocketUnifiedStructureAndResourceStorage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction facing = Direction.NORTH;
        HangarTerminalBlockEntity terminal = setupVertical1x1Skeleton(helper, VERTICAL_PAD_CENTER_REL, VERTICAL_TERMINAL_REL, facing);
        BlockPos padCenterAbs = helper.absolutePos(VERTICAL_PAD_CENTER_REL);
        BlockPos terminalAbs = helper.absolutePos(VERTICAL_TERMINAL_REL);

        helper.assertTrue(terminal.assemblePlatform(), "Expected VERTICAL 1x1 platform to assemble");

        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        dev.galacticraft.mod.content.rocket.part.RocketPartSpec engineSpec = catalog.allParts().stream()
                .filter(p -> p.category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.ENGINE && p.diameter() == 1)
                .findFirst().orElseThrow();
        dev.galacticraft.mod.content.rocket.part.RocketPartSpec tankSpec = catalog.allParts().stream()
                .filter(p -> p.category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.TANK && p.diameter() == 1)
                .findFirst().orElseThrow();
        dev.galacticraft.mod.content.rocket.part.RocketPartSpec cmdSpec = catalog.allParts().stream()
                .filter(p -> p.category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.COMMAND && p.diameter() == 1)
                .findFirst().orElseThrow();

        terminal.getRocketSlots().setItem(0, ModularRocketPartBlockItem.createConfiguredStack(engineSpec.toConfig(RocketLivery.WHITE)));
        terminal.getRocketSlots().setItem(1, ModularRocketPartBlockItem.createConfiguredStack(tankSpec.toConfig(RocketLivery.WHITE)));
        terminal.getRocketSlots().setItem(2, ModularRocketPartBlockItem.createConfiguredStack(cmdSpec.toConfig(RocketLivery.WHITE)));

        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected buildRocketFromSlots() to construct unified rocket");
        helper.assertTrue(terminal.isRocketAssembled(), "Expected terminal.isRocketAssembled() == true after building");
        helper.assertTrue(terminal.getAssembledRocketPartPositions().size() == 3,
                "Expected 3 linked parts in unified rocket, got " + terminal.getAssembledRocketPartPositions().size());

        BlockPos enginePos = padCenterAbs.above(1);
        BlockPos tankPos = padCenterAbs.above(1 + engineSpec.heightBlocks());
        BlockPos cmdPos = padCenterAbs.above(1 + engineSpec.heightBlocks() + tankSpec.heightBlocks());

        for (BlockPos p : new BlockPos[]{enginePos, tankPos, cmdPos}) {
            helper.assertTrue(level.getBlockEntity(p) instanceof ModularRocketPartBlockEntity partBe
                            && terminalAbs.equals(partBe.getAssembledTerminalPos()),
                    "Expected part at " + p + " to be bound to terminal " + terminalAbs);
        }

        AABB unifiedBounds = terminal.computeAssembledRocketWorldBounds();
        int expectedTotalHeight = engineSpec.heightBlocks() + tankSpec.heightBlocks() + cmdSpec.heightBlocks();
        helper.assertTrue(unifiedBounds != null
                        && unifiedBounds.minY == enginePos.getY()
                        && unifiedBounds.maxY == enginePos.getY() + expectedTotalHeight,
                "Expected unified AABB height " + expectedTotalHeight + ", got " + unifiedBounds);

        // Verify Fabric Transfer API transactional fuel & energy insertion (aborted vs committed)
        Storage<FluidVariant> fluidStorage = terminal.getRocketFluidStorage();
        EnergyStorage energyStorage = terminal.getRocketEnergyStorage();
        helper.assertTrue(fluidStorage != null && energyStorage != null,
                "Expected non-null unified fluid and energy storages on assembled rocket");

        try (Transaction aborted = Transaction.openOuter()) {
            long insertedFuel = fluidStorage.insert(FluidVariant.of(GCFluids.FUEL), 81_000L, aborted);
            long insertedEnergy = energyStorage.insert(1_000L, aborted);
            helper.assertTrue(insertedFuel > 0L && insertedEnergy > 0L,
                    "Expected positive simulated insertion for fuel and energy");
            // Intentionally abort without commit
        }
        helper.assertTrue(terminal.getRocketFuelDroplets() == 0L && terminal.getRocketEnergy() == 0L,
                "Aborted transaction must roll back fuel and energy to 0");

        long committedFuel;
        long committedEnergy;
        try (Transaction tx = Transaction.openOuter()) {
            committedFuel = fluidStorage.insert(FluidVariant.of(GCFluids.FUEL), 81_000L, tx);
            committedEnergy = energyStorage.insert(2_500L, tx);
            tx.commit();
        }
        helper.assertTrue(committedFuel == 81_000L && terminal.getRocketFuelDroplets() == 81_000L,
                "Expected 81,000 droplets of committed fuel, got " + terminal.getRocketFuelDroplets());
        helper.assertTrue(committedEnergy == 2_500L && terminal.getRocketEnergy() == 2_500L,
                "Expected 2,500 GJ of committed energy, got " + terminal.getRocketEnergy());

        HangarTerminalBlockEntity.RocketPadSummary telemetry = terminal.scanRocketOnPad();
        helper.assertTrue(telemetry.unifiedAssembly(), "Expected RocketPadSummary.unifiedAssembly() == true");
        helper.assertTrue(telemetry.currentFuelDroplets() == 81_000L && telemetry.currentEnergy() == 2_500L,
                "Expected telemetry to report live fuel and energy levels");
        helper.assertTrue(telemetry.currentWetMassKg() > telemetry.dryMassKg() && telemetry.currentDeltaV() > 0.0,
                "Expected loaded fuel to increase currentWetMassKg and produce positive currentDeltaV");

        // Verify creative fill toggle fills to 100% and second toggle drains to 0
        helper.assertTrue(terminal.creativeToggleFillRocket(), "Expected creativeToggleFillRocket() to fill rocket");
        helper.assertTrue(terminal.getRocketFuelDroplets() == telemetry.totalFuelDroplets()
                        && terminal.getRocketEnergy() == telemetry.energyCapacity(),
                "Expected creative fill to saturate fuel and energy to 100%");
        helper.assertTrue(terminal.creativeToggleFillRocket(), "Expected second creativeToggleFillRocket() to drain rocket");
        helper.assertTrue(terminal.getRocketFuelDroplets() == 0L && terminal.getRocketEnergy() == 0L,
                "Expected second creative toggle to drain fuel and energy back to 0");

        // Breaking ANY single part of the unified rocket must atomically tear down the ENTIRE rocket
        ModularRocketPartBlockEntity topPart = (ModularRocketPartBlockEntity) level.getBlockEntity(cmdPos);
        helper.assertTrue(topPart != null, "Expected command part at " + cmdPos);
        topPart.tearDown(level, cmdPos, null, false);

        helper.assertTrue(!terminal.isRocketAssembled(), "Expected rocketAssembled == false after breaking one part");
        helper.assertTrue(terminal.getAssembledRocketPartPositions().isEmpty(),
                "Expected assembledRocketParts to be empty after unified teardown");
        for (BlockPos p : new BlockPos[]{enginePos, tankPos, cmdPos}) {
            helper.assertTrue(level.getBlockState(p).isAir(),
                    "Expected entire unified rocket to tear down atomically; position " + p + " was " + level.getBlockState(p));
        }

        helper.succeed();
    }

    private static HangarTerminalBlockEntity setupVertical1x1Skeleton(
            GameTestHelper helper,
            BlockPos padCenterRel,
            BlockPos terminalRel,
            Direction facing
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos padCenterAbs = helper.absolutePos(padCenterRel);

        for (PlatformBlueprint.Entry entry : PlatformBlueprintRegistry.VERTICAL_1X1.entries()) {
            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, entry.localPos());
            level.setBlock(worldPos, entry.role().getBlock().defaultBlockState(), Block.UPDATE_ALL);
        }

        helper.setBlock(terminalRel, GCBlocks.HANGAR_TERMINAL.defaultBlockState().setValue(HangarTerminalBlock.FACING, facing));
        HangarTerminalBlockEntity terminal = requireTerminal(helper, terminalRel);

        boolean detected = terminal.autoDetectPad();
        helper.assertTrue(detected, "Expected autoDetectPad() to detect VERTICAL 1x1 skeleton pad");
        helper.assertTrue(terminal.getPlatformType() == PlatformType.VERTICAL,
                "Expected detected platformType == VERTICAL");
        helper.assertTrue(terminal.getPlatformSize() == PlatformSize.SIZE_1X1,
                "Expected detected platformSize == SIZE_1X1");
        helper.assertTrue(padCenterAbs.equals(terminal.getPadCenterPos()),
                "Expected detected padCenter to match " + padCenterAbs + ", got " + terminal.getPadCenterPos());

        return terminal;
    }

    private static Map<BlockPos, BlockState> captureBlueprintStates(
            ServerLevel level,
            PlatformBlueprint blueprint,
            BlockPos padCenterAbs,
            Direction facing
    ) {
        Map<BlockPos, BlockState> states = new LinkedHashMap<>(blueprint.entries().size());
        for (PlatformBlueprint.Entry entry : blueprint.entries()) {
            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, entry.localPos());
            states.put(worldPos, level.getBlockState(worldPos));
        }
        return states;
    }

    private static HangarTerminalBlockEntity requireTerminal(GameTestHelper helper, BlockPos terminalRel) {
        BlockEntity be = helper.getBlockEntity(terminalRel);
        if (!(be instanceof HangarTerminalBlockEntity terminal)) {
            helper.fail("Expected HangarTerminalBlockEntity at " + terminalRel + ", found " + be, terminalRel);
            throw new IllegalStateException("Unreachable after GameTestHelper.fail");
        }
        return terminal;
    }
}
