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
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.block.platform.HangarTerminalBlock;
import dev.galacticraft.mod.content.entity.vehicle.DetachedStageEntity;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.launch.RocketStateConverter;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Headless Minecraft GameTests validating Entrega 2 modular rocket launch &amp; flight:
 * <ul>
 *   <li>T-10.00s Water Deluge -> T-3.00s Thrust Buildup under Hold-Down Clamps -> T-0.00s atomic liftoff</li>
 *   <li>Safe Pad Abort under Hold-Down Clamps when liftoff {@code TWR < 1.15} (zero blocks damaged)</li>
 *   <li>Strict atomic rollback when {@code addFreshEntity} fails (zero blocks cleared)</li>
 *   <li>Korolev Cross radial separation, Hot-Staging pre-ignition, Fairing Jettison, dynamic hitbox &amp; LES Abort</li>
 *   <li>Propulsive touchdown reconversion onto natural stone terrain via Landing Leg Foundation Exemption</li>
 * </ul>
 */
public class ModularRocketLaunchTestSuite implements GalacticraftGameTest {
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

    @GameTest(template = PLATFORM_ARENA, batch = "modularLaunch", timeoutTicks = 260)
    public void testLaunchCountdownDelugeClampsAndAtomicLiftoff(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 13), PlatformSize.SIZE_3X3);
        terminal.getRocketSlots().setItem(0, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(2, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected 3x3 rocket to assemble");
        helper.assertTrue(terminal.creativeToggleFillRocket(), "Expected rocket fueled to 100%");

        List<BlockPos> originalParts = new ArrayList<>(terminal.getAssembledRocketPartPositions());
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        pilot.setPos(terminal.getPadCenterPos().getX() + 0.5, terminal.getPadCenterPos().getY() + 2.0, terminal.getPadCenterPos().getZ() + 0.5);

        helper.assertTrue(terminal.startLaunchSequence(pilot), "Expected T-10.0s countdown to start");
        helper.assertTrue(terminal.getLaunchPhase() == HangarTerminalBlockEntity.LaunchSequencePhase.PRE_FLIGHT_DELUGE,
                "Expected PRE_FLIGHT_DELUGE phase at T-10.0s");

        // Verify transition into THRUST_BUILDUP (Hold-Down Clamps locked) around T-2.5s (after 150 ticks)
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(terminal.getLaunchPhase() == HangarTerminalBlockEntity.LaunchSequencePhase.THRUST_BUILDUP,
                    "Expected THRUST_BUILDUP phase at T-2.5s, got " + terminal.getLaunchPhase());
            helper.assertTrue(terminal.isHoldDownClampsEngaged(), "Hold-Down Clamps must be engaged during Thrust Buildup");
            helper.assertTrue(terminal.isRocketAssembled(), "Static blocks must remain locked until T-0");
        });

        // Verify T-0.00s atomic liftoff after 210 ticks
        helper.runAfterDelay(210, () -> {
            helper.assertTrue(terminal.getLaunchPhase() == HangarTerminalBlockEntity.LaunchSequencePhase.IDLE,
                    "Terminal must return to IDLE after liftoff, abort=" + terminal.getLastLaunchAbortReason());
            helper.assertTrue(!terminal.isRocketAssembled(), "Terminal must release assembled rocket after liftoff");

            ModularRocketFlightEntity launched = terminal.getLastLaunchedEntity();
            helper.assertTrue(launched != null && launched.isAlive(), "Expected live ModularRocketFlightEntity after T-0");
            helper.assertTrue(launched.getPassengers().contains(pilot), "Pilot must be mounted inside flight entity");

            // Zero ghost blocks remaining on the launch pad!
            for (BlockPos pos : originalParts) {
                helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(),
                        "Expected zero ghost blocks at " + pos + ", found " + helper.getLevel().getBlockState(pos));
            }
            launched.discard();
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularLaunch", timeoutTicks = 260)
    public void testSafePadAbortOnInsufficientTwrUnderHoldDownClamps(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 14), PlatformSize.SIZE_5X5);
        // Tiny 1x1 RCS engine under heavy 3x3 tanks -> TWR << 1.15 on Overworld!
        terminal.getRocketSlots().setItem(0, partStack("rcs_quad_1x1_1b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(2, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(3, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected low-TWR rocket to build");
        helper.assertTrue(terminal.creativeToggleFillRocket(), "Expected rocket fueled");

        int partsBefore = terminal.getAssembledRocketPartPositions().size();
        helper.assertTrue(terminal.startLaunchSequence(null), "Expected countdown to start");

        helper.runAfterDelay(210, () -> {
            helper.assertTrue(terminal.getLaunchPhase() == HangarTerminalBlockEntity.LaunchSequencePhase.IDLE,
                    "Expected Safe Pad Abort to return terminal to IDLE");
            helper.assertTrue(terminal.getLastLaunchedEntity() == null,
                    "Low-TWR rocket must NOT lift off");
            helper.assertTrue(!terminal.getLastLaunchAbortReason().isEmpty(),
                    "Expected recorded Safe Pad Abort reason");
            helper.assertTrue(terminal.isRocketAssembled()
                            && terminal.getAssembledRocketPartPositions().size() == partsBefore,
                    "Safe Pad Abort must leave rocket 100% intact and undamaged on the pad");
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularLaunch", timeoutTicks = 60)
    public void testAtomicConversionRollbackPreservesAllBlocksWhenSpawnRejected(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 13), PlatformSize.SIZE_3X3);
        terminal.getRocketSlots().setItem(0, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(2, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected rocket built");

        List<BlockPos> masters = new ArrayList<>(terminal.getAssembledRocketPartPositions());
        ServerLevel level = helper.getLevel();

        // Inject spawn failure (addFreshEntity returns false) -> MUST rollback with 0 blocks deleted!
        ModularRocketFlightEntity result = RocketStateConverter.convertToFlightEntity(
                level,
                terminal.getPadCenterPos(),
                terminal.getFacing(),
                terminal.getBlockPos(),
                terminal.getAssemblyGraph(),
                terminal.getRocketEnergy(),
                terminal.getRocketOxygenDroplets(),
                masters,
                List.of(),
                entity -> false
        );

        helper.assertTrue(result == null, "Rejected entity spawn must return null");
        for (BlockPos pos : masters) {
            helper.assertTrue(level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity,
                    "Rollback must preserve master rocket block at " + pos);
        }
        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularLaunch", timeoutTicks = 100)
    public void testKorolevCrossStagingFairingJettisonDynamicHitboxAndLesAbort(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 14), PlatformSize.SIZE_5X5);
        terminal.getRocketSlots().setItem(0, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(1, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(2, partStack("interstage_hotstaging_3x3_1b", RocketPartCategory.ADAPTER));
        terminal.getRocketSlots().setItem(3, partStack("engine_merlinvac_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(4, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        terminal.getRocketSlots().setItem(5, partStack("fairing_barrel_3x3_1b", RocketPartCategory.ADAPTER));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected 2-stage hot-staging rocket built");

        List<ItemStack> boosterTemplate = List.of(
                partStack("srb_dart_1x1_1b", RocketPartCategory.ENGINE),
                partStack("tank_canister_1x1_1b", RocketPartCategory.TANK)
        );
        helper.assertTrue(terminal.attachRadialBoosters(null, boosterTemplate, RadialSymmetry.TWO, CrossfeedMode.ISOLATED, true),
                "Expected 2x radial boosters attached");
        helper.assertTrue(terminal.creativeToggleFillRocket(), "Expected rocket fueled");

        ServerLevel level = helper.getLevel();
        RocketAssemblyGraph graph = terminal.getAssemblyGraph();
        ModularRocketFlightEntity rocket = new ModularRocketFlightEntity(GCEntityTypes.MODULAR_ROCKET_FLIGHT, level);
        BlockPos absPos = helper.absolutePos(new BlockPos(8, 12, 8));
        rocket.setPos(absPos.getX() + 0.5, absPos.getY(), absPos.getZ() + 0.5);
        rocket.initializeFlightState(graph, 0L, 0L, Direction.NORTH, terminal.getBlockPos(), 1.0,
                ModularRocketFlightEntity.FlightPhase.ASCENT, List.of(), Map.of());
        helper.assertTrue(level.addFreshEntity(rocket), "Expected flight entity spawned");

        EntityDimensions initialDims = rocket.getDimensions(Pose.STANDING);

        // 1. Trigger manual staging #1 -> Korolev Cross radial booster separation
        helper.assertTrue(rocket.triggerManualStage(level, null), "Expected radial booster separation");
        List<DetachedStageEntity> boosters = level.getEntitiesOfClass(DetachedStageEntity.class, new AABB(absPos).inflate(16.0));
        helper.assertTrue(boosters.size() == 2, "Expected 2 Korolev Cross DetachedStageEntity boosters, got " + boosters.size());
        boolean hasOutwardSpin = boosters.stream().anyMatch(b ->
                Math.abs(b.getAngularVelocityPitchDegPerTick()) > 0.1F || b.getDeltaMovement().horizontalDistance() > 0.05);
        helper.assertTrue(hasOutwardSpin, "Korolev Cross boosters must have outward separation velocity & pitch-out");

        // 2. Fairing Jettison -> removes fairings & spawns 2 clamshell halves
        rocket.jettisonFairings(level);
        helper.assertTrue(!rocket.getAssemblyGraph().hasFairing(), "Fairings must be stripped after jettison");

        // 3. Hot-Staging trigger -> enters HOT_STAGING for 30 ticks
        helper.assertTrue(rocket.triggerManualStage(level, null), "Expected hot-staging trigger");
        helper.assertTrue(rocket.getFlightPhase() == ModularRocketFlightEntity.FlightPhase.HOT_STAGING
                        && rocket.getHotStageTicks() == 30,
                "Expected HOT_STAGING with 30-tick pre-ignition, got phase=" + rocket.getFlightPhase());

        // 4. LES Abort -> detaches capsule, shrinks dynamic hitbox, enters LES_ABORT
        helper.assertTrue(rocket.triggerLesAbort(null), "Expected LES Abort to succeed");
        helper.assertTrue(rocket.getFlightPhase() == ModularRocketFlightEntity.FlightPhase.LES_ABORT,
                "Expected LES_ABORT flight phase");
        EntityDimensions capsuleDims = rocket.getDimensions(Pose.STANDING);
        helper.assertTrue(capsuleDims.height() < initialDims.height(),
                "Dynamic EntityDimensions height must shrink after LES abort (" + capsuleDims.height() + " vs " + initialDims.height() + ")");

        // Clean up entities
        for (DetachedStageEntity d : level.getEntitiesOfClass(DetachedStageEntity.class, new AABB(absPos).inflate(32.0))) {
            d.discard();
        }
        rocket.discard();
        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularLaunch", timeoutTicks = 60)
    public void testPropulsiveTouchdownReconversionWithLandingLegExemption(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 13), PlatformSize.SIZE_3X3);
        terminal.getRocketSlots().setItem(0, partStack("landing_gear_stabilizer_3x3_1b", RocketPartCategory.UTILITY));
        terminal.getRocketSlots().setItem(1, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(2, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected lander rocket with landing gear built");

        RocketAssemblyGraph landerGraph = terminal.getAssemblyGraph();
        helper.assertTrue(landerGraph.hasLandingGear(), "Compiled graph must detect landing_gear_*");

        // Prepare natural stone terrain (hardness 1.5 >= 0.50, NO launch pad!) at another corner of arena
        ServerLevel level = helper.getLevel();
        BlockPos landingBase = helper.absolutePos(new BlockPos(3, 2, 3));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(landingBase.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
                for (int dy = 0; dy < 10; dy++) {
                    level.setBlock(landingBase.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        ModularRocketFlightEntity lander = new ModularRocketFlightEntity(GCEntityTypes.MODULAR_ROCKET_FLIGHT, level);
        lander.setPos(landingBase.getX() + 0.5, landingBase.getY() + 0.1, landingBase.getZ() + 0.5);
        lander.initializeFlightState(landerGraph, 1000L, 500L, Direction.NORTH, null, landingBase.getY() - 1,
                ModularRocketFlightEntity.FlightPhase.RETRO_DESCENT, List.of(), Map.of());
        helper.assertTrue(level.addFreshEntity(lander), "Expected lander entity spawned");

        boolean reconverted = RocketStateConverter.convertToStaticBlocks(level, landingBase, lander);
        helper.assertTrue(reconverted, "Landing Leg Foundation Exemption must allow static reconversion on natural stone");
        helper.assertTrue(lander.isRemoved(), "Flight entity must be discarded after static reconversion");
        helper.assertTrue(level.getBlockEntity(landingBase) instanceof ModularRocketPartBlockEntity,
                "Expected reconstructed static ModularRocketPartBlockEntity at landingBase");

        helper.succeed();
    }

    @GameTest(template = PLATFORM_ARENA, batch = "modularLaunch", timeoutTicks = 60)
    public void testRetroDescentManualThrottleCutoffAndLandingMinigameTelemetry(GameTestHelper helper) {
        HangarTerminalBlockEntity terminal = createFormedTerminal(helper, new BlockPos(8, 1, 13), PlatformSize.SIZE_3X3);
        terminal.getRocketSlots().setItem(0, partStack("landing_gear_stabilizer_3x3_1b", RocketPartCategory.UTILITY));
        terminal.getRocketSlots().setItem(1, partStack("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE));
        terminal.getRocketSlots().setItem(2, partStack("tank_fuel_rp1_3x3_2b", RocketPartCategory.TANK));
        terminal.getRocketSlots().setItem(3, partStack("capsule_gemini_3x3_2b", RocketPartCategory.COMMAND));
        helper.assertTrue(terminal.buildRocketFromSlots(), "Expected lander rocket built");
        helper.assertTrue(terminal.creativeToggleFillRocket(), "Expected lander fueled to 100%");

        ServerLevel level = helper.getLevel();
        BlockPos startPos = helper.absolutePos(new BlockPos(8, 35, 8));
        ServerPlayer pilot = helper.makeMockServerPlayerInLevel();
        ModularRocketFlightEntity lander = new ModularRocketFlightEntity(GCEntityTypes.MODULAR_ROCKET_FLIGHT, level);
        lander.setPos(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
        lander.initializeFlightState(terminal.getAssemblyGraph(), 1000L, 500L, Direction.NORTH, null, 0,
                ModularRocketFlightEntity.FlightPhase.RETRO_DESCENT, List.of(pilot.getUUID()), Map.of());
        helper.assertTrue(level.addFreshEntity(lander), "Expected lander entity spawned");
        pilot.startRiding(lander, true);

        // Starts in manual landing minigame mode with engine off and gear retracted
        helper.assertFalse(lander.isAutopilotEnabled(), "RETRO_DESCENT must start in manual landing minigame mode");
        helper.assertTrue(lander.getThrottle() <= 0.001F, "RETRO_DESCENT must start with throttle at 0%");
        helper.assertFalse(lander.isLandingGearDeployed(), "Landing gear must start retracted for landing minigame");

        // Pilot holds Space (throttle up) for 5 ticks -> throttle ramps up cleanly
        for (int i = 0; i < 5; i++) {
            lander.applyPilotControl(pilot, 0.0F, 0.0F, 1.0F, false, false, false, true, false);
            lander.tick();
        }
        helper.assertTrue(lander.getThrottle() > 0.15F, "Holding retro burn must increase throttle (got " + lander.getThrottle() + ")");
        helper.assertTrue(lander.getHoverThrottle() > 0.05F && lander.getHoverThrottle() <= 0.85F,
                "Expected valid hover throttle telemetry (got " + lander.getHoverThrottle() + ")");
        helper.assertTrue(lander.getSuicideBurnAlt() >= 4.0F,
                "Expected valid suicide burn altitude telemetry (got " + lander.getSuicideBurnAlt() + ")");

        // Pilot presses X (engineCutoff) and V (landing gear) -> instant 0% cutoff & gear deployed
        lander.applyPilotControl(pilot, 0.0F, 0.0F, 0.0F, false, false, true, false, true);
        lander.tick();
        helper.assertTrue(lander.getThrottle() <= 0.001F, "Engine cutoff (X) must immediately cut throttle to 0% (got " + lander.getThrottle() + ")");
        helper.assertTrue(lander.isLandingGearDeployed(), "Landing gear toggle (V) must deploy landing gear");

        // Verify unified cargo inventory (27 slots) and re-launch from FlightPhase.LANDED
        lander.getCargoInventory().setItem(0, new ItemStack(Blocks.IRON_BLOCK, 3));
        helper.assertTrue(lander.copyCargoStacks().get(0).getCount() == 3, "Expected cargo inventory item preserved");
        lander.setFlightPhase(ModularRocketFlightEntity.FlightPhase.LANDED);
        lander.tick();
        helper.assertTrue(lander.isAlive() && lander.getFlightPhase() == ModularRocketFlightEntity.FlightPhase.LANDED,
                "Landed modular rocket must stay intact as a unified entity");
        lander.applyPilotControl(pilot, 0.0F, 0.0F, 1.0F, false, false, false, true, false);
        helper.assertTrue(lander.getFlightPhase() == ModularRocketFlightEntity.FlightPhase.ASCENT,
                "Pressing Space while seated in LANDED rocket must re-launch into ASCENT");

        pilot.stopRiding();
        lander.discard();
        helper.succeed();
    }
}
