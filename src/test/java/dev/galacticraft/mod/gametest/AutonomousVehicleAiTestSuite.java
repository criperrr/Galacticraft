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
import dev.galacticraft.api.component.TelemetryData;
import dev.galacticraft.api.vehicle.VehicleAnimState;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.api.vehicle.ai.CargoLoadCondition;
import dev.galacticraft.api.vehicle.ai.VehicleAiState;
import dev.galacticraft.api.vehicle.ai.VehicleMissionConfig;
import dev.galacticraft.api.vehicle.ai.VehicleMissionRole;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.block.entity.machine.EnergyStorageModuleBlockEntity;
import dev.galacticraft.mod.content.entity.moon.MoonVillagerEntity;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.content.vehicle.ai.FixedWingNavigator3D;
import dev.galacticraft.mod.content.vehicle.ai.VehicleChunkTicketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.List;

/**
 * Headless GameTest suite verifying Phase 4 autonomous planetary vehicles ({@code AI_CORE})
 * across Ground 2.5D, Rotorcraft 3D, Fixed-Wing 3D, chunk ticket cleanup, and all mission roles.
 */
public class AutonomousVehicleAiTestSuite implements GalacticraftGameTest {

    private static void fillBox(ServerLevel level, BlockPos origin, int x0, int y0, int z0, int x1, int y1, int z1, net.minecraft.world.level.block.state.BlockState state) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z), state);
                }
            }
        }
    }

    private static ModularVehicleEntity spawnAutonomousVehicle(
            ServerLevel level,
            net.minecraft.resources.ResourceLocation chassisId,
            BlockPos pos,
            float yawDeg,
            PlanetaryEnvironment env
    ) {
        ModularVehicleEntity v = GCEntityTypes.MODULAR_VEHICLE.create(level);
        if (v == null) {
            throw new GameTestAssertException("Failed to create ModularVehicleEntity");
        }
        v.setPos(pos.getX() + 0.5, pos.getY() + 0.02, pos.getZ() + 0.5);
        v.setYRot(yawDeg);
        v.setOnGround(true);
        v.setEnvironmentForTest(env);
        v.setConfiguration(VehicleRegistry.createAutonomousPreset(chassisId));
        level.addFreshEntity(v);
        return v;
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void roverClimbsOneBlockStep(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 30, 0));
        ModularVehicleEntity rover = null;
        try {
            // Flat floor x=-2..6 at y=-1; raised 1-block plateau x=7..16 at y=0 (surface at y+1)
            fillBox(level, origin, -2, -1, -3, 16, -1, 7, Blocks.STONE.defaultBlockState());
            fillBox(level, origin, 7, 0, -3, 16, 0, 7, Blocks.STONE.defaultBlockState());

            BlockPos start = origin.offset(2, 0, 2);
            BlockPos goal = origin.offset(12, 1, 2);
            rover = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_SCOUT_ROVER, start, -90.0F, PlanetaryEnvironment.EARTH);
            rover.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withWaypoints(List.of(goal)));

            boolean climbed = false;
            boolean reached = false;
            for (int t = 0; t < 260; t++) {
                rover.tick();
                if (rover.getY() >= goal.getY() - 0.15) {
                    climbed = true;
                }
                double d = Math.hypot(rover.getX() - (goal.getX() + 0.5), rover.getZ() - (goal.getZ() + 0.5));
                if (d <= 1.2 && rover.getY() >= goal.getY() - 0.2) {
                    reached = true;
                    break;
                }
            }

            if (!climbed || !reached) {
                throw new GameTestAssertException(String.format(
                        "Rover failed to climb 1-block step to goal: pos=(%.2f, %.2f, %.2f) goal=%s state=%s",
                        rover.getX(), rover.getY(), rover.getZ(), goal, rover.getAiState()));
            }
        } finally {
            if (rover != null && !rover.isRemoved()) {
                rover.discard();
            }
            fillBox(level, origin, -2, -1, -3, 16, 2, 7, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void roverAvoidsCraterAndWall(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 48, 0));
        ModularVehicleEntity rover = null;
        try {
            // Solid floor slab with 3-block-deep crater beneath
            fillBox(level, origin, -2, -4, -6, 20, -1, 10, Blocks.STONE.defaultBlockState());
            // Carve 3-block-deep crater at x=5..7, z=1..3 (surface drops to y-3)
            fillBox(level, origin, 5, -3, 1, 7, -1, 3, Blocks.AIR.defaultBlockState());
            // Build 2-block-tall wall at x=11, z=-1..5 with a 1-block narrow gap at z=2
            fillBox(level, origin, 11, 0, -1, 11, 1, 5, Blocks.STONE.defaultBlockState());
            fillBox(level, origin, 11, 0, 2, 11, 1, 2, Blocks.AIR.defaultBlockState());

            BlockPos start = origin.offset(2, 0, 2);
            BlockPos goal = origin.offset(16, 0, 2);
            BlockPos narrowGap = origin.offset(11, 0, 2);

            rover = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_SCOUT_ROVER, start, -90.0F, PlanetaryEnvironment.EARTH);
            rover.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withWaypoints(List.of(goal)));

            boolean fellInCrater = false;
            boolean enteredNarrowGap = false;
            double bestDist = Double.MAX_VALUE;
            for (int t = 0; t < 420; t++) {
                rover.tick();
                if (rover.getY() < origin.getY() - 0.35) {
                    fellInCrater = true;
                }
                if (Mth.floor(rover.getX()) == narrowGap.getX() && Mth.floor(rover.getZ()) == narrowGap.getZ()) {
                    enteredNarrowGap = true;
                }
                double d = Math.hypot(rover.getX() - (goal.getX() + 0.5), rover.getZ() - (goal.getZ() + 0.5));
                bestDist = Math.min(bestDist, d);
                if (d <= 1.0) {
                    break;
                }
            }

            if (fellInCrater) {
                throw new GameTestAssertException("Rover fell into the 3-block-deep crater");
            }
            if (enteredNarrowGap) {
                throw new GameTestAssertException("Wide rover unlawfully entered 1-block narrow wall gap");
            }
            if (bestDist > 1.0) {
                throw new GameTestAssertException(String.format(
                        "Rover did not arrive within 1.00 block of goal: bestDist=%.2f pos=(%.2f,%.2f,%.2f) state=%s",
                        bestDist, rover.getX(), rover.getY(), rover.getZ(), rover.getAiState()));
            }
        } finally {
            if (rover != null && !rover.isRemoved()) {
                rover.discard();
            }
            fillBox(level, origin, -2, -4, -6, 20, 3, 10, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void roverStuckRecovery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 66, 0));
        ModularVehicleEntity rover = null;
        try {
            fillBox(level, origin, -2, -1, -6, 18, -1, 10, Blocks.STONE.defaultBlockState());

            BlockPos start = origin.offset(2, 0, 2);
            BlockPos goal = origin.offset(14, 0, 2);
            rover = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_SCOUT_ROVER, start, -90.0F, PlanetaryEnvironment.EARTH);
            rover.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withWaypoints(List.of(goal)));

            boolean wallSpawned = false;
            boolean observedStuckRecovery = false;
            double bestDist = Double.MAX_VALUE;
            for (int t = 0; t < 440; t++) {
                rover.tick();
                if (!wallSpawned && rover.getAiState() == VehicleAiState.TRANSIT && rover.getX() >= origin.getX() + 4.5) {
                    int wx = Mth.floor(rover.getX()) + 2 - origin.getX();
                    int wz = Mth.floor(rover.getZ()) - origin.getZ();
                    fillBox(level, origin, wx, 0, wz - 2, wx, 1, wz + 2, Blocks.COBBLESTONE.defaultBlockState());
                    wallSpawned = true;
                }
                if (rover.getAiState() == VehicleAiState.STUCK_RECOVERY) {
                    observedStuckRecovery = true;
                }
                double d = Math.hypot(rover.getX() - (goal.getX() + 0.5), rover.getZ() - (goal.getZ() + 0.5));
                bestDist = Math.min(bestDist, d);
                if (observedStuckRecovery && d <= 1.2) {
                    break;
                }
            }

            if (!wallSpawned) {
                throw new GameTestAssertException("Rover never started driving before dynamic wall spawn");
            }
            if (!observedStuckRecovery) {
                throw new GameTestAssertException("Rover did not enter STUCK_RECOVERY after dynamic wall placement");
            }
            if (bestDist > 1.2) {
                throw new GameTestAssertException(String.format(
                        "Rover did not replan and arrive after STUCK_RECOVERY: bestDist=%.2f state=%s",
                        bestDist, rover.getAiState()));
            }
        } finally {
            if (rover != null && !rover.isRemoved()) {
                rover.discard();
            }
            fillBox(level, origin, -2, -1, -6, 18, 3, 10, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void roverReturnsToChargerOnLowBattery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 84, 0));
        ModularVehicleEntity rover = null;
        try {
            fillBox(level, origin, -2, -1, -3, 18, -1, 7, Blocks.STONE.defaultBlockState());

            BlockPos chargerPos = origin.offset(2, 0, 2);
            BlockPos energyModulePos = origin.offset(2, 0, 0);
            BlockPos startPos = origin.offset(10, 0, 2);
            BlockPos farTarget = origin.offset(16, 0, 2);

            // Place a charged Galacticraft Energy Storage Module beside home pad to verify real Fabric Transfer API charging
            level.setBlockAndUpdate(energyModulePos, GCBlocks.ENERGY_STORAGE_MODULE.defaultBlockState());
            if (level.getBlockEntity(energyModulePos) instanceof EnergyStorageModuleBlockEntity esm) {
                esm.energyStorage().setEnergy(50_000L);
            }

            rover = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_SCOUT_ROVER, startPos, -90.0F, PlanetaryEnvironment.EARTH);
            rover.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withHomeChargerPos(chargerPos)
                    .withWaypoints(List.of(farTarget)));

            // Start mission briefly at full charge, then drop battery to 15%
            for (int t = 0; t < 4; t++) {
                rover.tick();
            }
            long cap = rover.getDerivedStats().energyCapacity();
            long lowEnergy = Math.round(cap * 0.15);
            rover.setStoredEnergyServer(lowEnergy);
            rover.tick();

            if (rover.getAiState() != VehicleAiState.RETURN_TO_BASE) {
                throw new GameTestAssertException("Expected immediate transition to RETURN_TO_BASE at 15% battery, got " + rover.getAiState());
            }

            boolean reachedCharging = false;
            long energyAtDock = 0L;
            for (int t = 0; t < 260; t++) {
                rover.tick();
                if (rover.getAiState() == VehicleAiState.CHARGING) {
                    reachedCharging = true;
                    energyAtDock = rover.getConfiguration().storedEnergy();
                    for (int c = 0; c < 15; c++) {
                        rover.tick();
                    }
                    break;
                }
            }

            double distToHome = Math.hypot(rover.getX() - (chargerPos.getX() + 0.5), rover.getZ() - (chargerPos.getZ() + 0.5));
            if (!reachedCharging || distToHome > 2.8) {
                throw new GameTestAssertException(String.format(
                        "Rover failed to dock at CHARGING on low battery: state=%s distHome=%.2f",
                        rover.getAiState(), distToHome));
            }
            if (rover.getConfiguration().storedEnergy() <= energyAtDock) {
                throw new GameTestAssertException("Expected docked rover in CHARGING state to draw electric energy from Energy Storage Module");
            }
        } finally {
            if (rover != null && !rover.isRemoved()) {
                rover.discard();
            }
            fillBox(level, origin, -2, -1, -3, 18, 2, 7, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void roverReleasesChunkTicketOnRemove(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 102, 0));
        ModularVehicleEntity rover = null;
        int baselineActive = VehicleChunkTicketManager.getActiveCount(level.dimension());
        try {
            fillBox(level, origin, -2, -1, -2, 16, -1, 6, Blocks.STONE.defaultBlockState());

            BlockPos start = origin.offset(2, 0, 2);
            BlockPos goal = origin.offset(14, 0, 2);
            rover = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_SCOUT_ROVER, start, -90.0F, PlanetaryEnvironment.EARTH);
            rover.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withChunkLoadingEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withWaypoints(List.of(goal)));

            for (int t = 0; t < 6; t++) {
                rover.tick();
            }
            if (!rover.isChunkTicketActive() || rover.getMissionController().getChunkTickets().getLoadedTicketChunk() == null) {
                throw new GameTestAssertException("Expected active moving chunk ticket while autonomous rover is in transit");
            }

            // Verify remove() cleanly releases ROVER_TICKET with zero leaks
            rover.remove(Entity.RemovalReason.DISCARDED);
            if (rover.getMissionController().getChunkTickets().hasActiveTicket()
                    || rover.getMissionController().getChunkTickets().getLoadedTicketChunk() != null) {
                throw new GameTestAssertException("Chunk ticket was not released on Entity#remove()");
            }
            if (VehicleChunkTicketManager.getActiveCount(level.dimension()) > baselineActive) {
                throw new GameTestAssertException("Leaked dimension chunk ticket count after rover removal");
            }
        } finally {
            if (rover != null && !rover.isRemoved()) {
                rover.remove(Entity.RemovalReason.DISCARDED);
            }
            fillBox(level, origin, -2, -1, -2, 16, 2, 6, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void helicopterTakeoffCruiseAndLandOnPad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 120, 0));
        ModularVehicleEntity heli = null;
        try {
            // Start pad, dest pad 24 blocks away, and a 6-block-tall wall directly in between
            fillBox(level, origin, -1, -1, -1, 5, -1, 5, Blocks.STONE.defaultBlockState());
            fillBox(level, origin, 23, -1, -1, 29, -1, 5, Blocks.STONE.defaultBlockState());
            fillBox(level, origin, 14, 0, -4, 14, 5, 8, Blocks.STONE.defaultBlockState());

            BlockPos startPad = origin.offset(2, 0, 2);
            BlockPos destPad = origin.offset(26, 0, 2);
            heli = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_LIGHT_HELICOPTER, startPad, -90.0F, PlanetaryEnvironment.EARTH);
            heli.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withWaypoints(List.of(destPad)));

            double maxY = heli.getY();
            float maxRpm = 0.0F;
            boolean landedOnDest = false;
            for (int t = 0; t < 560; t++) {
                heli.tick();
                maxY = Math.max(maxY, heli.getY());
                maxRpm = Math.max(maxRpm, heli.getRotorRpm());
                double horizDist = Math.hypot(heli.getX() - (destPad.getX() + 0.5), heli.getZ() - (destPad.getZ() + 0.5));
                if (horizDist <= 2.5 && heli.getY() <= destPad.getY() + 0.45 && heli.getAiState() == VehicleAiState.WORKING) {
                    landedOnDest = true;
                    // Tick a few more ticks to confirm rotors spool down on touchdown
                    for (int s = 0; s < 35; s++) {
                        heli.tick();
                    }
                    break;
                }
            }

            if (maxY < origin.getY() + 7.5) {
                throw new GameTestAssertException("Helicopter did not climb clear of 6-block wall, maxAgl=" + (maxY - origin.getY()));
            }
            if (maxRpm < ModularVehicleEntity.BLUR_DISC_RPM_THRESHOLD) {
                throw new GameTestAssertException("Expected flight rotor RPM >= 600, got " + maxRpm);
            }
            if (!landedOnDest) {
                throw new GameTestAssertException(String.format(
                        "Helicopter did not land on target pad: pos=(%.2f,%.2f,%.2f) dest=%s state=%s",
                        heli.getX(), heli.getY(), heli.getZ(), destPad, heli.getAiState()));
            }
            if (heli.getRotorRpm() > 250.0F) {
                throw new GameTestAssertException("Expected rotors to spool down after landing, rpm=" + heli.getRotorRpm());
            }
        } finally {
            if (heli != null && !heli.isRemoved()) {
                heli.discard();
            }
            fillBox(level, origin, -1, -1, -4, 29, 7, 8, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void helicopterAbortsLandingOnObstructedPad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 148, 0));
        ModularVehicleEntity heli = null;
        ArmorStand blocker = null;
        try {
            fillBox(level, origin, -1, -1, -1, 5, -1, 5, Blocks.STONE.defaultBlockState());
            fillBox(level, origin, 13, -1, -1, 19, -1, 5, Blocks.STONE.defaultBlockState());

            BlockPos startPad = origin.offset(2, 0, 2);
            BlockPos destPad = origin.offset(16, 0, 2);

            blocker = EntityType.ARMOR_STAND.create(level);
            if (blocker == null) {
                throw new GameTestAssertException("Failed to create blocking ArmorStand");
            }
            blocker.setPos(destPad.getX() + 0.5, destPad.getY(), destPad.getZ() + 0.5);
            level.addFreshEntity(blocker);

            heli = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_LIGHT_HELICOPTER, startPad, -90.0F, PlanetaryEnvironment.EARTH);
            heli.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withWaypoints(List.of(destPad)));

            boolean observedHoverHold = false;
            boolean collidedWithPad = false;
            for (int t = 0; t < 440; t++) {
                heli.tick();
                double distXZ = Math.hypot(heli.getX() - (destPad.getX() + 0.5), heli.getZ() - (destPad.getZ() + 0.5));
                if (heli.getAiState() == VehicleAiState.HOVER_HOLD) {
                    observedHoverHold = true;
                    if (heli.getY() < destPad.getY() + 2.0) {
                        collidedWithPad = true;
                    }
                    break;
                }
                if (distXZ < 2.0 && heli.getY() < destPad.getY() + 1.5 && t > 20) {
                    collidedWithPad = true;
                }
            }

            if (!observedHoverHold) {
                throw new GameTestAssertException(String.format(
                        "Expected helicopter to enter HOVER_HOLD above obstructed pad, final state=%s pos=(%.2f,%.2f,%.2f)",
                        heli.getAiState(), heli.getX(), heli.getY(), heli.getZ()));
            }
            if (collidedWithPad) {
                throw new GameTestAssertException("Helicopter descended into obstructing entity instead of holding safe hover altitude");
            }
        } finally {
            if (heli != null && !heli.isRemoved()) {
                heli.discard();
            }
            if (blocker != null && !blocker.isRemoved()) {
                blocker.discard();
            }
            fillBox(level, origin, -1, -1, -1, 19, 3, 5, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void cargoShuttleTransfersItemsBetweenPads(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 174, 0));
        ModularVehicleEntity shuttle = null;
        try {
            fillBox(level, origin, -2, -1, -3, 18, -1, 7, Blocks.STONE.defaultBlockState());

            BlockPos originPad = origin.offset(2, 0, 2);
            BlockPos destPad = origin.offset(14, 0, 2);
            BlockPos originChestPos = origin.offset(2, 0, 0);
            BlockPos destChestPos = origin.offset(14, 0, 0);

            level.setBlockAndUpdate(originChestPos, Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(destChestPos, Blocks.CHEST.defaultBlockState());

            ChestBlockEntity srcChest = (ChestBlockEntity) level.getBlockEntity(originChestPos);
            ChestBlockEntity dstChest = (ChestBlockEntity) level.getBlockEntity(destChestPos);
            if (srcChest == null || dstChest == null) {
                throw new GameTestAssertException("Failed to create origin/destination chests");
            }
            srcChest.setItem(0, new ItemStack(Items.IRON_INGOT, 16));

            shuttle = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_UTILITY_BUGGY, originPad, -90.0F, PlanetaryEnvironment.EARTH);
            shuttle.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.CARGO_SHUTTLE)
                    .withCargoLoadCondition(CargoLoadCondition.INACTIVITY_60_TICKS)
                    .withWaypoints(List.of(originPad, destPad)));

            boolean observedRamp = false;
            boolean delivered = false;
            for (int t = 0; t < 440; t++) {
                shuttle.tick();
                if (shuttle.getAnimState() == VehicleAnimState.BOARDING_RAMP) {
                    observedRamp = true;
                }
                int deliveredCount = countItem(dstChest, Items.IRON_INGOT);
                if (deliveredCount == 16 && countItem(srcChest, Items.IRON_INGOT) == 0) {
                    delivered = true;
                    break;
                }
            }

            if (!observedRamp) {
                throw new GameTestAssertException("Expected CARGO_SHUTTLE to trigger BOARDING_RAMP animation at pads");
            }
            if (!delivered) {
                throw new GameTestAssertException(String.format(
                        "Cargo shuttle failed to transfer 16 iron ingots: src=%d dst=%d state=%s pos=(%.2f,%.2f)",
                        countItem(srcChest, Items.IRON_INGOT), countItem(dstChest, Items.IRON_INGOT),
                        shuttle.getAiState(), shuttle.getX(), shuttle.getZ()));
            }
        } finally {
            if (shuttle != null && !shuttle.isRemoved()) {
                shuttle.discard();
            }
            fillBox(level, origin, -2, -1, -3, 18, 2, 7, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void passengerTaxiCarriesVillagerSafely(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 192, 0));
        ModularVehicleEntity taxi = null;
        MoonVillagerEntity villager = null;
        try {
            fillBox(level, origin, -2, -1, -3, 18, -1, 7, Blocks.STONE.defaultBlockState());

            BlockPos stationA = origin.offset(2, 0, 2);
            BlockPos stationB = origin.offset(14, 0, 2);

            villager = GCEntityTypes.MOON_VILLAGER.create(level);
            if (villager == null) {
                throw new GameTestAssertException("Failed to create MoonVillagerEntity");
            }
            villager.setPos(stationA.getX() + 1.4, stationA.getY(), stationA.getZ() + 1.4);
            level.addFreshEntity(villager);
            float initialHealth = villager.getHealth();

            taxi = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_UTILITY_BUGGY, stationA, -90.0F, PlanetaryEnvironment.EARTH);
            taxi.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.PASSENGER_TAXI)
                    .withAutoBoardNpcs(true)
                    .withWaypoints(List.of(stationA, stationB)));

            boolean observedBoarded = false;
            boolean observedDismountedAtDest = false;
            for (int t = 0; t < 480; t++) {
                taxi.tick();
                if (villager.getVehicle() == taxi) {
                    observedBoarded = true;
                }
                double villagerDistB = Math.hypot(villager.getX() - (stationB.getX() + 0.5), villager.getZ() - (stationB.getZ() + 0.5));
                if (observedBoarded && !villager.isPassenger() && villagerDistB <= 5.5) {
                    observedDismountedAtDest = true;
                    break;
                }
            }

            if (!observedBoarded) {
                throw new GameTestAssertException("MoonVillagerEntity was not boarded by PASSENGER_TAXI at Station A");
            }
            if (!observedDismountedAtDest) {
                throw new GameTestAssertException(String.format(
                        "MoonVillagerEntity was not transported & dismounted at Station B: riding=%s villager=(%.2f,%.2f) taxiState=%s",
                        villager.isPassenger(), villager.getX(), villager.getZ(), taxi.getAiState()));
            }
            if (!villager.isAlive() || villager.getHealth() < initialHealth) {
                throw new GameTestAssertException("MoonVillagerEntity took damage during PASSENGER_TAXI transport");
            }
            BlockPos underVillager = BlockPos.containing(villager.getX(), villager.getY() - 0.5, villager.getZ());
            if (level.getBlockState(underVillager).getCollisionShape(level, underVillager).isEmpty()) {
                throw new GameTestAssertException("MoonVillagerEntity was not dismounted onto solid ground");
            }
        } finally {
            if (villager != null && !villager.isRemoved()) {
                villager.discard();
            }
            if (taxi != null && !taxi.isRemoved()) {
                taxi.discard();
            }
            fillBox(level, origin, -2, -1, -3, 18, 2, 7, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void scienceRoverRunsScanAnimationAndCollectsData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 210, 0));
        ModularVehicleEntity rover = null;
        try {
            fillBox(level, origin, -2, -1, -3, 16, -1, 7, Blocks.STONE.defaultBlockState());
            BlockPos homePad = origin.offset(2, 0, 2);
            BlockPos surveySite = origin.offset(11, 0, 2);
            // Place a detectable iron ore signature directly beneath survey site
            level.setBlockAndUpdate(surveySite.below(), Blocks.IRON_ORE.defaultBlockState());

            rover = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_SCOUT_ROVER, homePad, -90.0F, PlanetaryEnvironment.EARTH);
            rover.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withHomeChargerPos(homePad)
                    .withWaypoints(List.of(surveySite)));

            boolean observedTransit = false;
            boolean observedScanAnim = false;
            boolean observedReturnToBase = false;
            for (int t = 0; t < 420; t++) {
                rover.tick();
                if (rover.getAiState() == VehicleAiState.TRANSIT) {
                    observedTransit = true;
                }
                if (rover.getAiState() == VehicleAiState.WORKING && rover.getAnimState() == VehicleAnimState.SCANNING_TERRAIN) {
                    observedScanAnim = true;
                }
                if (rover.getAiState() == VehicleAiState.RETURN_TO_BASE) {
                    observedReturnToBase = true;
                    break;
                }
            }

            if (!observedTransit) {
                throw new GameTestAssertException("Science rover never entered TRANSIT toward survey waypoint");
            }
            if (!observedScanAnim) {
                throw new GameTestAssertException("Science rover never triggered WORKING + SCANNING_TERRAIN animation");
            }
            if (!observedReturnToBase) {
                throw new GameTestAssertException("Science rover did not transition to RETURN_TO_BASE after survey, state=" + rover.getAiState());
            }

            // Verify Recorded Data Drive with real TelemetryData component was saved into cargo inventory
            TelemetryData recorded = null;
            for (int i = 0; i < rover.getCargoInventory().getContainerSize(); i++) {
                ItemStack st = rover.getCargoInventory().getItem(i);
                if (st.is(GCItems.RECORDED_DATA_DRIVE)) {
                    recorded = st.get(GCDataComponents.TELEMETRY_DATA);
                    break;
                }
            }
            if (recorded == null || rover.getCollectedScienceDataCount() < 1) {
                throw new GameTestAssertException("Science rover did not encode TelemetryData onto a Recorded Data Drive");
            }
        } finally {
            if (rover != null && !rover.isRemoved()) {
                rover.discard();
            }
            fillBox(level, origin, -2, -1, -3, 16, 2, 7, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fixedWingAutonomousTakeoffAndAirspeedProtection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 228, 0));
        ModularVehicleEntity plane = null;
        try {
            // Elevated unobstructed runway along +Z
            fillBox(level, origin, -2, -1, -2, 6, -1, 34, Blocks.STONE.defaultBlockState());

            BlockPos start = origin.offset(2, 0, 2);
            BlockPos dest = origin.offset(2, 0, 96);
            plane = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_LIGHT_AIRPLANE, start, 0.0F, PlanetaryEnvironment.EARTH);
            plane.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withRunwayHeadingDeg(0)
                    .withWaypoints(List.of(dest)));

            double startY = plane.getY();
            double maxY = startY;
            float maxThrottle = 0.0F;
            for (int t = 0; t < 110; t++) {
                plane.tick();
                maxY = Math.max(maxY, plane.getY());
                maxThrottle = Math.max(maxThrottle, plane.getThrottle());
            }

            if (maxThrottle < 0.85F) {
                throw new GameTestAssertException("Expected FixedWingNavigator3D to command full takeoff throttle, maxThrottle=" + maxThrottle);
            }
            if (maxY <= startY + 2.5) {
                throw new GameTestAssertException(String.format(
                        "Autonomous fixed-wing airplane failed to take off and climb: deltaY=%.2f speed=%.3f state=%s",
                        maxY - startY, plane.getSyncedSpeed(), plane.getAiState()));
            }
        } finally {
            if (plane != null && !plane.isRemoved()) {
                plane.discard();
            }
            fillBox(level, origin, -2, -1, -2, 6, 2, 34, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void escortPatrolFollowsOwnerAtSafeOffset(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 248, 0));
        ModularVehicleEntity escort = null;
        MoonVillagerEntity vip = null;
        try {
            fillBox(level, origin, -2, -1, -4, 22, -1, 8, Blocks.STONE.defaultBlockState());

            vip = GCEntityTypes.MOON_VILLAGER.create(level);
            if (vip == null) {
                throw new GameTestAssertException("Failed to create escorted VIP entity");
            }
            BlockPos vipPos = origin.offset(16, 0, 2);
            vip.setPos(vipPos.getX() + 0.5, vipPos.getY(), vipPos.getZ() + 0.5);
            vip.setYRot(-90.0F); // Facing +X so trailing offset sits at x ~= 10
            level.addFreshEntity(vip);

            BlockPos start = origin.offset(2, 0, 2);
            escort = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_SCOUT_ROVER, start, -90.0F, PlanetaryEnvironment.EARTH);
            escort.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.ESCORT_PATROL)
                    .withOwnerUuid(vip.getUUID())
                    .withEscortOffsetBlocks(6.0));

            double startDist = Math.hypot(escort.getX() - vip.getX(), escort.getZ() - vip.getZ());
            for (int t = 0; t < 220; t++) {
                escort.tick();
            }
            double endDist = Math.hypot(escort.getX() - vip.getX(), escort.getZ() - vip.getZ());
            if (endDist >= startDist - 3.5 || endDist > 9.5) {
                throw new GameTestAssertException(String.format(
                        "ESCORT_PATROL rover failed to close to safe trailing offset behind owner: startDist=%.2f endDist=%.2f state=%s",
                        startDist, endDist, escort.getAiState()));
            }
            if (endDist < 2.0) {
                throw new GameTestAssertException("ESCORT_PATROL rover collided too closely with owner instead of keeping trailing offset: endDist=" + endDist);
            }
        } finally {
            if (vip != null && !vip.isRemoved()) {
                vip.discard();
            }
            if (escort != null && !escort.isRemoved()) {
                escort.discard();
            }
            fillBox(level, origin, -2, -1, -4, 22, 2, 8, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fixedWingAbortsMisalignedApproachWithGoAround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 266, 0));
        ModularVehicleEntity plane = null;
        try {
            BlockPos runwayThreshold = origin.offset(2, 0, 20);
            // Spawn airborne airplane 8 blocks laterally off centerline near threshold (lateralError = 8 > 4.0)
            BlockPos misalignedApproach = origin.offset(10, 5, 14);
            plane = spawnAutonomousVehicle(level, VehicleRegistry.CHASSIS_LIGHT_AIRPLANE, misalignedApproach, 0.0F, PlanetaryEnvironment.EARTH);
            plane.setOnGround(false);
            plane.setThrottleDirect(0.75F);
            plane.setMissionConfig(VehicleMissionConfig.DEFAULT
                    .withEnabled(true)
                    .withRole(VehicleMissionRole.SCIENCE_EXPLORER)
                    .withRunwayHeadingDeg(0)
                    .withWaypoints(List.of(runwayThreshold)));

            boolean observedGoAround = false;
            for (int t = 0; t < 20; t++) {
                plane.tick();
                if (plane.getAiState() == VehicleAiState.GO_AROUND
                        && plane.getMissionController().getNavigator() instanceof FixedWingNavigator3D fw
                        && fw.getFlightPhase() == FixedWingNavigator3D.FlightPhase.GO_AROUND) {
                    observedGoAround = true;
                    break;
                }
            }
            if (!observedGoAround) {
                throw new GameTestAssertException("Expected FixedWingNavigator3D to abort misaligned threshold approach with GO_AROUND, state=" + plane.getAiState());
            }
            if (plane.getThrottle() < 0.70F) {
                throw new GameTestAssertException("Expected GO_AROUND to apply high climb throttle, throttle=" + plane.getThrottle());
            }
        } finally {
            if (plane != null && !plane.isRemoved()) {
                plane.discard();
            }
        }
        helper.succeed();
    }

    private static int countItem(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack st = chest.getItem(i);
            if (st.is(item)) {
                total += st.getCount();
            }
        }
        return total;
    }
}
