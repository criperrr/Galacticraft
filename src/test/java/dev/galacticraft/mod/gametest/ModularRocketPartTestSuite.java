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
import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.FootprintMode;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.mod.attachments.GCAttachments;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.attachments.MultiblockLinksAttachment;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.block.entity.RocketWorkbenchBlockEntity;
import dev.galacticraft.mod.content.block.multiblock.AbstractBoundsProxyBlock;
import dev.galacticraft.mod.content.block.platform.HangarTerminalBlock;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprint;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprintRegistry;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.CostProfile;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.part.SchematicGridLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ModularRocketPartTestSuite implements GalacticraftGameTest {
    private static final String PLATFORM_ARENA = "galacticraft-test:platform_arena";

    @GameTest(template = PLATFORM_ARENA)
    public void testModularPartPlacementAndAtomicTearDown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos masterRel = new BlockPos(8, 2, 8);
        BlockPos masterAbs = helper.absolutePos(masterRel);

        helper.setBlock(masterRel, GCBlocks.ROCKET_PART_TANK.defaultBlockState());
        ModularRocketPartBlockEntity be = requireModularPart(helper, masterRel);

        RocketPartSpec tankSpec = RocketPartCatalog.getInstance().getPart("tank_fuel_rp1_3x3_2b");
        helper.assertTrue(tankSpec != null, "Expected tank_fuel_rp1_3x3_2b in RocketPartCatalog");
        RocketPartConfig tankConfig = tankSpec.toConfig(RocketLivery.BLUE);

        be.initializePart(tankConfig, null, null, Direction.NORTH);

        List<BlockPos> occupiedProxies = new ArrayList<>(be.getOccupiedProxies());
        helper.assertTrue(occupiedProxies.size() == 17,
                "Expected 17 proxy voxels for 3x3x2 solid tank (18 - 1 master), got " + occupiedProxies.size());

        for (BlockPos proxyPos : occupiedProxies) {
            BlockState proxyState = level.getBlockState(proxyPos);
            helper.assertTrue(proxyState.is(GCBlocks.ROCKET_PART_BOUNDS),
                    "Expected ROCKET_PART_BOUNDS at " + proxyPos + ", got " + proxyState);
            helper.assertTrue(proxyState.getValue(AbstractBoundsProxyBlock.SHAPE) == BoundsShape.FULL,
                    "Expected BoundsShape.FULL at " + proxyPos);
            helper.assertTrue(masterAbs.equals(MultiblockLinkManager.getMasterPos(level, proxyPos)),
                    "Expected proxy " + proxyPos + " to link to master " + masterAbs);
            helper.assertTrue(be == MultiblockLinkManager.getMaster(level, proxyPos),
                    "Expected MultiblockLinkManager.getMaster() to resolve ModularRocketPartBlockEntity at " + proxyPos);
            helper.assertTrue(be.isPartOfStructure(proxyPos),
                    "Expected be.isPartOfStructure(" + proxyPos + ") == true");
        }

        BlockPos firstProxy = occupiedProxies.get(0);
        be.onProxyBroken(level, firstProxy, null, false);

        helper.assertTrue(level.getBlockState(masterAbs).isAir(),
                "Expected master block at " + masterAbs + " to become AIR after atomic teardown");
        for (BlockPos proxyPos : occupiedProxies) {
            helper.assertTrue(level.getBlockState(proxyPos).isAir(),
                    "Expected proxy voxel at " + proxyPos + " to become AIR after atomic teardown");
            helper.assertTrue(MultiblockLinkManager.getMasterPos(level, proxyPos) == null,
                    "Expected chunk link at " + proxyPos + " to be removed after atomic teardown");
            helper.assertTrue(MultiblockLinkManager.getMaster(level, proxyPos) == null,
                    "Expected null master lookup at " + proxyPos + " after atomic teardown");
            LevelChunk chunk = level.getChunkAt(proxyPos);
            MultiblockLinksAttachment attached = chunk.getAttached(GCAttachments.MULTIBLOCK_LINKS);
            helper.assertTrue(attached == null || !attached.containsProxy(proxyPos.asLong()),
                    "Expected chunk attachment to contain zero stale entries for " + proxyPos);
        }

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testWalkableInteriorHatchToggle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos masterRel = new BlockPos(8, 2, 8);
        BlockPos masterAbs = helper.absolutePos(masterRel);

        helper.setBlock(masterRel, GCBlocks.ROCKET_PART_COMMAND.defaultBlockState());
        ModularRocketPartBlockEntity be = requireModularPart(helper, masterRel);

        RocketPartSpec orionSpec = RocketPartCatalog.getInstance().getPart("capsule_orion_5x5_3b");
        helper.assertTrue(orionSpec != null, "Expected capsule_orion_5x5_3b in RocketPartCatalog");
        RocketPartConfig orionConfig = orionSpec.toConfig(RocketLivery.WHITE);
        helper.assertTrue(orionConfig.walkableInterior() && orionConfig.diameter() == 5 && orionConfig.heightBlocks() == 3,
                "Expected capsule_orion_5x5_3b to be a 5x5x3 walkable interior part");

        be.initializePart(orionConfig, null, null, Direction.NORTH);
        helper.assertTrue(!be.isHatchOpen(), "Expected hatch to start closed");

        // 1. Verify strictly interior cells at y=1 are AIR so the player can walk inside
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                helper.assertTrue(FootprintMode.isStrictlyInterior(dx, dz, 5),
                        "Expected (" + dx + ", " + dz + ") to be strictly interior for diameter 5");
                BlockPos cabinAirPos = FootprintMode.toWorldPos(masterAbs, Direction.NORTH, new BlockPos(dx, 1, dz));
                helper.assertTrue(level.getBlockState(cabinAirPos).isAir(),
                        "Expected interior cabin cell at " + cabinAirPos + " (y=1) to remain walkable AIR, got " + level.getBlockState(cabinAirPos));
                helper.assertTrue(MultiblockLinkManager.getMasterPos(level, cabinAirPos) == null,
                        "Walkable interior AIR cell at " + cabinAirPos + " must not be linked as a proxy");

                // 2. Verify interior floor cells at y=0 (excluding master at (0,0,0)) are ROCKET_PART_BOUNDS with FLOOR_4PX
                if (dx != 0 || dz != 0) {
                    BlockPos floorPos = FootprintMode.toWorldPos(masterAbs, Direction.NORTH, new BlockPos(dx, 0, dz));
                    BlockState floorState = level.getBlockState(floorPos);
                    helper.assertTrue(floorState.is(GCBlocks.ROCKET_PART_BOUNDS),
                            "Expected ROCKET_PART_BOUNDS at interior floor " + floorPos + ", got " + floorState);
                    helper.assertTrue(floorState.getValue(AbstractBoundsProxyBlock.SHAPE) == BoundsShape.FLOOR_4PX,
                            "Expected BoundsShape.FLOOR_4PX at interior floor " + floorPos + ", got " + floorState.getValue(AbstractBoundsProxyBlock.SHAPE));
                }
            }
        }

        // 3. Verify hatch cells start FULL with solid collision
        List<BlockPos> localHatchCells = FootprintMode.computeLocalHatchPositions(orionConfig);
        helper.assertTrue(!localHatchCells.isEmpty(), "Expected non-empty hatch positions for capsule_orion_5x5_3b");
        for (BlockPos localHatch : localHatchCells) {
            BlockPos worldHatch = FootprintMode.toWorldPos(masterAbs, Direction.NORTH, localHatch);
            BlockState hatchState = level.getBlockState(worldHatch);
            helper.assertTrue(hatchState.is(GCBlocks.ROCKET_PART_BOUNDS),
                    "Expected ROCKET_PART_BOUNDS at hatch cell " + worldHatch);
            helper.assertTrue(hatchState.getValue(AbstractBoundsProxyBlock.SHAPE) == BoundsShape.FULL,
                    "Expected closed hatch cell at " + worldHatch + " to have BoundsShape.FULL");
            helper.assertTrue(!hatchState.getCollisionShape(level, worldHatch).isEmpty(),
                    "Closed hatch cell at " + worldHatch + " must have a non-empty collision shape");
        }

        // 4. Toggle hatch open and assert OPEN_HATCH with empty collision shape
        boolean toggledOpen = be.toggleHatch();
        helper.assertTrue(toggledOpen, "Expected toggleHatch() to return true on walkable interior part");
        helper.assertTrue(be.isHatchOpen(), "Expected be.isHatchOpen() == true after toggling open");
        helper.assertTrue(level.getBlockState(masterAbs).getValue(ModularRocketPartBlock.HATCH_OPEN),
                "Expected master block state HATCH_OPEN == true");

        for (BlockPos localHatch : localHatchCells) {
            BlockPos worldHatch = FootprintMode.toWorldPos(masterAbs, Direction.NORTH, localHatch);
            BlockState hatchState = level.getBlockState(worldHatch);
            helper.assertTrue(hatchState.is(GCBlocks.ROCKET_PART_BOUNDS),
                    "Expected ROCKET_PART_BOUNDS to remain at hatch cell " + worldHatch);
            helper.assertTrue(hatchState.getValue(AbstractBoundsProxyBlock.SHAPE) == BoundsShape.OPEN_HATCH,
                    "Expected open hatch cell at " + worldHatch + " to switch to BoundsShape.OPEN_HATCH");
            helper.assertTrue(hatchState.getCollisionShape(level, worldHatch).isEmpty(),
                    "Open hatch cell at " + worldHatch + " must have an empty collision shape");
            helper.assertTrue(masterAbs.equals(MultiblockLinkManager.getMasterPos(level, worldHatch)),
                    "Open hatch cell at " + worldHatch + " must stay linked to master");
        }

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testCoaxialAutoSnapPlacement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos engineMasterRel = new BlockPos(8, 2, 8);
        BlockPos engineMasterAbs = helper.absolutePos(engineMasterRel);

        helper.setBlock(engineMasterRel, GCBlocks.ROCKET_PART_ENGINE.defaultBlockState());
        ModularRocketPartBlockEntity engineBe = requireModularPart(helper, engineMasterRel);

        RocketPartSpec engineSpec = RocketPartCatalog.getInstance().getPart("engine_falcon1d_3x3_2b");
        helper.assertTrue(engineSpec != null, "Expected engine_falcon1d_3x3_2b in RocketPartCatalog");
        RocketPartConfig engineConfig = engineSpec.toConfig(RocketLivery.WHITE);
        engineBe.initializePart(engineConfig, null, null, Direction.NORTH);

        // Target the UP face of an offset top-corner proxy voxel at relative (9, 3, 9)
        BlockPos targetProxyRel = new BlockPos(9, 3, 9);
        BlockPos targetProxyAbs = helper.absolutePos(targetProxyRel);
        helper.assertTrue(level.getBlockState(targetProxyAbs).is(GCBlocks.ROCKET_PART_BOUNDS),
                "Expected offset voxel " + targetProxyAbs + " to be ROCKET_PART_BOUNDS");
        helper.assertTrue(engineMasterAbs.equals(MultiblockLinkManager.getMasterPos(level, targetProxyAbs)),
                "Expected offset proxy " + targetProxyAbs + " to link to engine master " + engineMasterAbs);

        RocketPartSpec tankSpec = RocketPartCatalog.getInstance().getPart("tank_fuel_rp1_3x3_2b");
        helper.assertTrue(tankSpec != null, "Expected tank_fuel_rp1_3x3_2b in RocketPartCatalog");
        RocketPartConfig tankConfig = tankSpec.toConfig(RocketLivery.ORANGE);
        ItemStack tankStack = ModularRocketPartBlockItem.createConfiguredStack(tankConfig);

        BlockHitResult hitUp = new BlockHitResult(
                Vec3.atCenterOf(targetProxyAbs).add(0.0, 0.5, 0.0),
                Direction.UP,
                targetProxyAbs,
                false
        );
        BlockPlaceContext placeContext = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, tankStack, hitUp);

        BlockPos resolvedOrigin = ModularRocketPartBlockItem.resolvePlacementOrigin(
                level,
                placeContext,
                tankConfig,
                Direction.NORTH
        );
        BlockPos expectedCoaxialOrigin = helper.absolutePos(new BlockPos(8, 4, 8));
        helper.assertTrue(expectedCoaxialOrigin.equals(resolvedOrigin),
                "Expected coaxial snap origin " + expectedCoaxialOrigin + " above engine master (8,2,8), got " + resolvedOrigin);
        helper.assertTrue(ModularRocketPartBlockItem.canPlaceFootprint(level, resolvedOrigin, tankConfig, Direction.NORTH),
                "Expected coaxial destination footprint at " + resolvedOrigin + " to be valid and unobstructed");

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA, timeoutTicks = 400)
    public void testTinkersPartBuilderModularCrafting(GameTestHelper helper) {
        BlockPos workbenchRel = new BlockPos(8, 1, 8);
        helper.setBlock(workbenchRel, GCBlocks.ROCKET_WORKBENCH.defaultBlockState());
        RocketWorkbenchBlockEntity be = requireWorkbench(helper, workbenchRel);

        be.energyStorage().setEnergy(RocketWorkbenchBlockEntity.ENERGY_CAPACITY);
        be.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT).set(GCItems.PART_SCHEMATIC_ADAPTER, 1);

        helper.assertTrue(be.getActiveSchematicCategory() == RocketPartCategory.ADAPTER,
                "Expected PART_SCHEMATIC_ADAPTER to activate RocketPartCategory.ADAPTER");

        be.setModularSelection("adapter_structural", 3, 1, 1, RocketLivery.ORANGE);
        if (!"adapter".equals(be.getSelectedFamilyId())) {
            be.setModularSelection("adapter", 3, 1, 1, RocketLivery.ORANGE);
        }

        RocketPartConfig resolvedConfig = be.getResolvedModularConfig();
        helper.assertTrue(resolvedConfig != null, "Expected non-null resolved modular RocketPartConfig");
        helper.assertTrue(resolvedConfig.category() == RocketPartCategory.ADAPTER
                        && resolvedConfig.diameter() == 3
                        && resolvedConfig.topDiameter() == 1
                        && resolvedConfig.heightBlocks() == 1
                        && resolvedConfig.livery() == RocketLivery.ORANGE,
                "Expected 3x3->1x1x1 ORANGE adapter config, got " + resolvedConfig);

        CostProfile cost = RocketPartCostCalculator.calculate(resolvedConfig);
        SchematicGridLayout layout = SchematicGridLayout.forCategory(RocketPartCategory.ADAPTER);
        helper.assertTrue(layout.isModular() && layout.activeSlotCount() == 4,
                "Expected ADAPTER SchematicGridLayout to have 4 active silhouette slots");

        // Populate ONLY the active silhouette slots with exact scaled requirements
        for (int i = 0; i < SchematicGridLayout.GRID_SIZE; i++) {
            int machineSlot = RocketWorkbenchBlockEntity.INPUT_GRID_START + i;
            if (!layout.isSlotActive(i)) {
                helper.assertTrue(be.itemStorage().slot(machineSlot).isEmpty(),
                        "Inactive silhouette slot " + i + " must remain empty");
                continue;
            }
            SchematicGridLayout.SlotRequirement req = layout.requirement(i);
            helper.assertTrue(req != null, "Expected SlotRequirement for active ADAPTER grid slot " + i);
            ItemStack requiredItem = req.primaryDisplayStack();
            helper.assertTrue(!requiredItem.isEmpty(), "Expected non-empty ingredient display stack for slot " + i);
            int requiredCount = req.requiredCount(cost.slotItemMultiplier());
            be.itemStorage().slot(machineSlot).set(requiredItem.getItem(), requiredCount);
        }

        int waitTicks = cost.assemblyTimeTicks() + 5;
        helper.runAfterDelay(waitTicks, () -> {
            // 1. Verify OUTPUT_SLOT holds configured ROCKET_PART_ADAPTER
            ItemStack outputStack = be.itemStorage().getItem(RocketWorkbenchBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(!outputStack.isEmpty(),
                    "Expected OUTPUT_SLOT to contain fabricated modular adapter after " + waitTicks + " ticks");
            helper.assertTrue(outputStack.is(GCBlocks.ROCKET_PART_ADAPTER.asItem()),
                    "Expected OUTPUT_SLOT item to be ROCKET_PART_ADAPTER, got " + outputStack.getItem());
            helper.assertTrue(outputStack.getCount() == 1,
                    "Expected OUTPUT_SLOT stack count == 1, got " + outputStack.getCount());

            RocketPartConfig outConfig = outputStack.get(GCDataComponents.ROCKET_PART_CONFIG);
            helper.assertTrue(resolvedConfig.equals(outConfig),
                    "Expected crafted ROCKET_PART_CONFIG " + resolvedConfig + ", got " + outConfig);
            helper.assertTrue(outConfig.diameter() == 3 && outConfig.topDiameter() == 1 && outConfig.livery() == RocketLivery.ORANGE,
                    "Expected diameter=3, topDiameter=1, livery=ORANGE on crafted output");

            RocketPartStats outStats = outputStack.get(GCDataComponents.ROCKET_PART_STATS);
            helper.assertTrue(cost.stats().equals(outStats),
                    "Expected crafted ROCKET_PART_STATS " + cost.stats() + ", got " + outStats);

            PartIntegrity outIntegrity = outputStack.get(GCDataComponents.PART_INTEGRITY);
            helper.assertTrue(PartIntegrity.pristine(100.0).equals(outIntegrity),
                    "Expected crafted PART_INTEGRITY pristine(100.0), got " + outIntegrity);

            // 2. Verify SCHEMATIC_SLOT still holds PART_SCHEMATIC_ADAPTER (non-consumed!)
            ItemResourceSlot schematicSlot = be.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT);
            helper.assertTrue(!schematicSlot.isEmpty()
                            && schematicSlot.getResource() == GCItems.PART_SCHEMATIC_ADAPTER
                            && schematicSlot.getAmount() == 1L,
                    "Expected PART_SCHEMATIC_ADAPTER in SCHEMATIC_SLOT to remain non-consumed");

            // 3. Verify all crafting grid slots were consumed cleanly
            for (int i = 0; i < SchematicGridLayout.GRID_SIZE; i++) {
                int machineSlot = RocketWorkbenchBlockEntity.INPUT_GRID_START + i;
                helper.assertTrue(be.itemStorage().slot(machineSlot).isEmpty(),
                        "Expected grid slot " + i + " to be empty after modular fabrication");
            }

            // 4. Verify scrap metal byproduct in BYPRODUCT_SLOT
            ItemStack scrapStack = be.itemStorage().getItem(RocketWorkbenchBlockEntity.BYPRODUCT_SLOT);
            helper.assertTrue(scrapStack.is(GCItems.ALUMINUM_NUGGET) && scrapStack.getCount() == cost.scrapCount(),
                    "Expected " + cost.scrapCount() + " ALUMINUM_NUGGET scrap in BYPRODUCT_SLOT, got " + scrapStack);

            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testPartBuilderEjectsInactiveSlotItemsOnSchematicChange(GameTestHelper helper) {
        BlockPos workbenchRel = new BlockPos(8, 1, 8);
        helper.setBlock(workbenchRel, GCBlocks.ROCKET_WORKBENCH.defaultBlockState());
        RocketWorkbenchBlockEntity be = requireWorkbench(helper, workbenchRel);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SchematicGridLayout adapterLayout = SchematicGridLayout.forCategory(RocketPartCategory.ADAPTER);

        int inactiveGridIndex = -1;
        int activeGridIndex = -1;
        for (int i = 0; i < SchematicGridLayout.GRID_SIZE; i++) {
            if (!adapterLayout.isSlotActive(i) && inactiveGridIndex < 0) {
                inactiveGridIndex = i;
            } else if (adapterLayout.isSlotActive(i) && activeGridIndex < 0) {
                activeGridIndex = i;
            }
        }
        helper.assertTrue(inactiveGridIndex >= 0 && activeGridIndex >= 0,
                "Expected ADAPTER schematic to have both active and inactive grid slots");

        // Place items in both slots BEFORE inserting the modular schematic
        be.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + inactiveGridIndex).set(Items.IRON_INGOT, 5);
        be.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + activeGridIndex).set(Items.GOLD_INGOT, 3);

        // Insert the ADAPTER schematic and trigger inactive slot ejection
        be.itemStorage().slot(RocketWorkbenchBlockEntity.SCHEMATIC_SLOT).set(GCItems.PART_SCHEMATIC_ADAPTER, 1);
        int ejected = be.ejectInactiveGridItems(player);
        helper.assertTrue(ejected == 1, "Expected 1 inactive slot stack to be ejected, got " + ejected);

        helper.assertTrue(be.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + inactiveGridIndex).isEmpty(),
                "Expected item in newly inactive slot to be ejected from workbench");
        helper.assertTrue(be.itemStorage().slot(RocketWorkbenchBlockEntity.INPUT_GRID_START + activeGridIndex).getAmount() == 3L,
                "Expected item in active slot to remain inside workbench");
        helper.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 5,
                "Expected 5 ejected iron ingots to be returned to player inventory, got " + player.getInventory().countItem(Items.IRON_INGOT));

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testPlacedPartStaticByDefaultAndTriggeredOnlyViaPlatform(GameTestHelper helper) {
        BlockPos padCenterRel = new BlockPos(8, 1, 8);
        BlockPos terminalRel = new BlockPos(8, 1, 10);
        Direction facing = Direction.NORTH;
        PlatformBlueprint blueprint = PlatformBlueprintRegistry.VERTICAL_1X1;

        BlockPos padCenterAbs = helper.absolutePos(padCenterRel);
        for (PlatformBlueprint.Entry entry : blueprint.entries()) {
            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenterAbs, facing, entry.localPos());
            helper.getLevel().setBlock(worldPos, entry.role().getBlock().defaultBlockState(), 3);
        }
        helper.setBlock(terminalRel, GCBlocks.HANGAR_TERMINAL.defaultBlockState().setValue(HangarTerminalBlock.FACING, facing));
        HangarTerminalBlockEntity terminal = (HangarTerminalBlockEntity) helper.getBlockEntity(terminalRel);
        helper.assertTrue(terminal != null && terminal.autoDetectPad(), "Expected terminal to detect pad");
        helper.assertTrue(terminal.assemblePlatform(), "Expected VERTICAL 1x1 platform to assemble");

        // Place an animated/deployable rocket part on the formed pad
        BlockPos partRel = padCenterRel.above();
        helper.setBlock(partRel, GCBlocks.ROCKET_PART_ENGINE.defaultBlockState());
        ModularRocketPartBlockEntity engineBe = requireModularPart(helper, partRel);
        RocketPartSpec engineSpec = RocketPartCatalog.getInstance().getPart("engine_falcon1d_1x1_1b");
        if (engineSpec == null) {
            engineSpec = RocketPartCatalog.getInstance().defaultSpecFor(RocketPartCategory.ENGINE);
        }
        helper.assertTrue(engineSpec != null, "Expected an engine spec in RocketPartCatalog");
        engineBe.initializePart(engineSpec.toConfig(RocketLivery.WHITE), null, null, Direction.NORTH);

        // Must be static by default!
        helper.assertTrue(!engineBe.isAnimationActive(), "Placed rocket part must be static (animationActive == false) by default");

        // Formed platform scans the rocket and triggers animation
        HangarTerminalBlockEntity.RocketPadSummary scan = terminal.scanRocketOnPad();
        helper.assertTrue(scan.parts().size() == 1 && scan.hasEngine(),
                "Expected formed platform scan to detect 1 engine part on pad, got " + scan.parts().size());
        helper.assertTrue(terminal.triggerRocketPartAnimation(0),
                "Expected platform rocket menu trigger to activate part animation");
        helper.assertTrue(engineBe.isAnimationActive(),
                "Expected engine animationActive == true after platform trigger");

        helper.assertTrue(terminal.triggerRocketPartAnimation(-2),
                "Expected platform retract-all trigger to stop active animations");
        helper.assertTrue(!engineBe.isAnimationActive(),
                "Expected engine animationActive == false after retract-all trigger");

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testRadialDecouplerLateralPlacement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos decouplerRel = new BlockPos(8, 3, 8);
        BlockPos decouplerAbs = helper.absolutePos(decouplerRel);

        helper.setBlock(decouplerRel, GCBlocks.ROCKET_PART_UTILITY.defaultBlockState());
        ModularRocketPartBlockEntity decouplerBe = requireModularPart(helper, decouplerRel);

        RocketPartSpec radialSpec = RocketPartCatalog.getInstance().getPart("decoupler_radial_booster_1b");
        helper.assertTrue(radialSpec != null && radialSpec.radial(),
                "Expected decoupler_radial_booster_1b radial spec in RocketPartCatalog");
        decouplerBe.initializePart(radialSpec.toConfig(RocketLivery.WHITE), null, null, Direction.EAST);

        RocketPartSpec boosterSpec = RocketPartCatalog.getInstance().getPart("tank_fuel_rp1_3x3_2b");
        helper.assertTrue(boosterSpec != null, "Expected tank_fuel_rp1_3x3_2b in RocketPartCatalog");
        RocketPartConfig boosterConfig = boosterSpec.toConfig(RocketLivery.WHITE);
        ItemStack boosterStack = ModularRocketPartBlockItem.createConfiguredStack(boosterConfig);

        // Aim at the EAST horizontal face of the radial decoupler
        BlockHitResult hitEast = new BlockHitResult(
                Vec3.atCenterOf(decouplerAbs).add(0.5, 0.0, 0.0),
                Direction.EAST,
                decouplerAbs,
                false
        );
        BlockPlaceContext placeContext = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, boosterStack, hitEast);
        BlockPos resolvedOrigin = ModularRocketPartBlockItem.resolvePlacementOrigin(
                level,
                placeContext,
                boosterConfig,
                Direction.NORTH
        );
        // 3x3 booster has half-span 1 -> center at decouplerPos.relative(EAST, 1 + 1) = (10, 3, 8)
        BlockPos expectedLateralOrigin = decouplerAbs.relative(Direction.EAST, 2);
        helper.assertTrue(expectedLateralOrigin.equals(resolvedOrigin),
                "Expected lateral booster origin " + expectedLateralOrigin + " beside radial decoupler, got " + resolvedOrigin);
        helper.assertTrue(ModularRocketPartBlockItem.canPlaceFootprint(level, resolvedOrigin, boosterConfig, Direction.NORTH),
                "Expected lateral booster footprint at " + resolvedOrigin + " to be unobstructed");

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA)
    public void testRocketPartCompositionPlacementNbtAndDropRoundtrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos padRel = new BlockPos(8, 1, 8);
        BlockPos padAbs = helper.absolutePos(padRel);
        helper.setBlock(padRel, GCBlocks.REINFORCED_LAUNCH_PAD.defaultBlockState());

        RocketPartSpec geminiSpec = RocketPartCatalog.getInstance().getPart("capsule_gemini_3x3_2b");
        helper.assertTrue(geminiSpec != null, "Expected capsule_gemini_3x3_2b in RocketPartCatalog");
        RocketPartConfig geminiConfig = geminiSpec.toConfig(RocketLivery.ORANGE);

        RocketPartComposition hybridComposition = new RocketPartComposition(List.of(
                RocketPartComposition.SlotMaterialEntry.of(
                        1,
                        RocketMaterialProfile.SlotRole.COMMAND_NOSE_RECOVERY,
                        BuiltInRegistries.ITEM.getKey(GCItems.OLIVINE_SHARD),
                        List.of(RocketMaterialTrait.ABLATIVE_TPS)
                ),
                RocketPartComposition.SlotMaterialEntry.of(
                        3,
                        RocketMaterialProfile.SlotRole.STRUCTURAL_HULL,
                        BuiltInRegistries.ITEM.getKey(GCItems.COMPRESSED_TITANIUM),
                        List.of(RocketMaterialTrait.AEROSPACE_GRADE)
                ),
                RocketPartComposition.SlotMaterialEntry.of(
                        4,
                        RocketMaterialProfile.SlotRole.COMMAND_VIEWPORT,
                        BuiltInRegistries.ITEM.getKey(GCBlocks.OLIVINE_GLASS.asItem()),
                        List.of(RocketMaterialTrait.REFRACTORY_CANOPY)
                ),
                RocketPartComposition.SlotMaterialEntry.of(
                        5,
                        RocketMaterialProfile.SlotRole.STRUCTURAL_HULL,
                        BuiltInRegistries.ITEM.getKey(GCItems.LEAD_INGOT),
                        List.of(RocketMaterialTrait.ACID_RADIATION_SHIELD)
                ),
                RocketPartComposition.SlotMaterialEntry.of(
                        7,
                        RocketMaterialProfile.SlotRole.COMMAND_AVIONICS,
                        BuiltInRegistries.ITEM.getKey(GCItems.HELIUM3_FUSION_CELL),
                        List.of(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR)
                )
        ));

        ItemStack capsuleStack = ModularRocketPartBlockItem.createConfiguredStack(
                geminiConfig,
                null,
                PartIntegrity.pristine(150.0),
                hybridComposition
        );
        helper.assertTrue(hybridComposition.equals(capsuleStack.get(GCDataComponents.ROCKET_PART_COMPOSITION)),
                "Expected ROCKET_PART_COMPOSITION on newly created ItemStack");

        // Place via ModularRocketPartBlockItem.place(...) onto the launch pad
        BlockHitResult hitPadTop = new BlockHitResult(
                Vec3.atCenterOf(padAbs).add(0.0, 0.5, 0.0),
                Direction.UP,
                padAbs,
                false
        );
        BlockPlaceContext placeContext = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, capsuleStack, hitPadTop);
        InteractionResult placeResult = ((ModularRocketPartBlockItem) capsuleStack.getItem()).place(placeContext);
        helper.assertTrue(placeResult.consumesAction(), "Expected ModularRocketPartBlockItem.place() to succeed");

        BlockPos masterRel = padRel.above();
        BlockPos masterAbs = padAbs.above();
        ModularRocketPartBlockEntity placedBe = requireModularPart(helper, masterRel);
        helper.assertTrue(hybridComposition.equals(placedBe.getComposition()),
                "Expected placed ModularRocketPartBlockEntity to preserve exact RocketPartComposition");

        // Verify NBT save/load + update tag roundtrip
        CompoundTag savedTag = placedBe.saveWithoutMetadata(level.registryAccess());
        CompoundTag updateTag = placedBe.getUpdateTag(level.registryAccess());
        helper.assertTrue(savedTag.contains("PartComposition"), "Expected PartComposition in saved NBT");
        helper.assertTrue(updateTag.contains("PartComposition"), "Expected PartComposition in client update tag");

        ModularRocketPartBlockEntity reloadedBe = new ModularRocketPartBlockEntity(masterAbs, level.getBlockState(masterAbs));
        reloadedBe.loadWithComponents(savedTag, level.registryAccess());
        helper.assertTrue(hybridComposition.equals(reloadedBe.getComposition()),
                "Expected NBT-reloaded BlockEntity to preserve exact RocketPartComposition");

        // Verify pick-block (master and proxy) preserves composition
        ItemStack masterClone = level.getBlockState(masterAbs).getBlock().getCloneItemStack(level, masterAbs, level.getBlockState(masterAbs));
        helper.assertTrue(hybridComposition.equals(masterClone.get(GCDataComponents.ROCKET_PART_COMPOSITION)),
                "Expected master pick-block clone stack to preserve RocketPartComposition");
        BlockPos proxyPos = placedBe.getOccupiedProxies().getFirst();
        ItemStack proxyClone = placedBe.getProxyCloneItemStack(level, proxyPos);
        helper.assertTrue(hybridComposition.equals(proxyClone.get(GCDataComponents.ROCKET_PART_COMPOSITION)),
                "Expected proxy pick-block clone stack to preserve RocketPartComposition");

        // Break/teardown with dropItem=true and verify dropped ItemEntity preserves exact composition & traits
        placedBe.tearDown(level, masterAbs, null, true);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(masterAbs).inflate(2.5));
        helper.assertTrue(drops.size() == 1, "Expected exactly 1 dropped ItemEntity after teardown, got " + drops.size());

        ItemStack droppedStack = drops.getFirst().getItem();
        RocketPartComposition droppedComp = droppedStack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
        helper.assertTrue(hybridComposition.equals(droppedComp),
                "Expected dropped ItemStack to preserve exact RocketPartComposition, got " + droppedComp);
        helper.assertTrue(droppedComp.hasTrait(RocketMaterialTrait.AEROSPACE_GRADE)
                        && droppedComp.hasTrait(RocketMaterialTrait.ACID_RADIATION_SHIELD)
                        && droppedComp.hasTrait(RocketMaterialTrait.REFRACTORY_CANOPY)
                        && droppedComp.hasTrait(RocketMaterialTrait.ABLATIVE_TPS)
                        && droppedComp.hasTrait(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR),
                "Expected dropped ItemStack composition to retain all 5 hybrid traits");
        helper.assertTrue(PartIntegrity.pristine(150.0).equals(droppedStack.get(GCDataComponents.PART_INTEGRITY)),
                "Expected dropped ItemStack to preserve custom PartIntegrity");

        helper.succeed();
    }

    private static ModularRocketPartBlockEntity requireModularPart(GameTestHelper helper, BlockPos relPos) {
        BlockEntity be = helper.getBlockEntity(relPos);
        if (!(be instanceof ModularRocketPartBlockEntity partBe)) {
            helper.fail("Expected ModularRocketPartBlockEntity at " + relPos + ", found " + be, relPos);
            throw new IllegalStateException("Unreachable after GameTestHelper.fail");
        }
        return partBe;
    }

    private static RocketWorkbenchBlockEntity requireWorkbench(GameTestHelper helper, BlockPos relPos) {
        BlockEntity be = helper.getBlockEntity(relPos);
        if (!(be instanceof RocketWorkbenchBlockEntity workbenchBe)) {
            helper.fail("Expected RocketWorkbenchBlockEntity at " + relPos + ", found " + be, relPos);
            throw new IllegalStateException("Unreachable after GameTestHelper.fail");
        }
        return workbenchBe;
    }
}
