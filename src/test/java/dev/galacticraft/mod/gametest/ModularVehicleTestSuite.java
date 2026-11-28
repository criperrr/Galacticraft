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

import dev.galacticraft.api.vehicle.SpatialCargoItem;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.block.entity.VehiclePartFabricatorBlockEntity;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.entity.vehicle.RocketEntity;
import dev.galacticraft.mod.content.item.DroneRemoteControllerItem;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.VehicleCrateItem;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.content.vehicle.RocketCargoGrid;
import dev.galacticraft.mod.content.vehicle.VehicleFabricatorRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public class ModularVehicleTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void fabricatorCraftsChassisFromMaterials(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, GCBlocks.VEHICLE_PART_FABRICATOR);

        if (!(helper.getBlockEntity(pos) instanceof VehiclePartFabricatorBlockEntity be)) {
            throw new GameTestAssertException("VehiclePartFabricatorBlockEntity was not created");
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // Should fail without materials
        if (be.tryFabricate(player, VehicleRegistry.CHASSIS_SCOUT_ROVER)) {
            throw new GameTestAssertException("Fabricator should not craft without materials");
        }

        be.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 8));
        be.getInventory().setItem(1, new ItemStack(Items.COPPER_INGOT, 4));
        if (!be.tryFabricate(player, VehicleRegistry.CHASSIS_SCOUT_ROVER)) {
            throw new GameTestAssertException("Fabricator failed to craft Scout Rover chassis with exact materials");
        }

        ItemStack out = be.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT);
        if (!out.is(GCItems.SCOUT_ROVER_CHASSIS)) {
            throw new GameTestAssertException("Expected SCOUT_ROVER_CHASSIS in output, got " + out);
        }
        if (!be.getInventory().getItem(0).isEmpty() || !be.getInventory().getItem(1).isEmpty()) {
            throw new GameTestAssertException("Fabricator did not consume input materials");
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void physicalAssemblyAndCratePackingPreservesConfig(GameTestHelper helper) {
        ModularVehicleEntity rover = GCEntityTypes.MODULAR_VEHICLE.create(helper.getLevel());
        if (rover == null) {
            throw new GameTestAssertException("Failed to create ModularVehicleEntity");
        }
        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 2, 2));
        rover.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        rover.setConfiguration(VehicleConfiguration.emptyChassis(VehicleRegistry.CHASSIS_SCOUT_ROVER));
        helper.getLevel().addFreshEntity(rover);

        if (rover.getDerivedStats().isDrivable()) {
            throw new GameTestAssertException("Bare chassis should not be drivable before parts are installed");
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // Install 3 wheels + motor + battery + seat: still missing 4th wheel!
        rover.tryInstallPart(player, new ItemStack(GCItems.STANDARD_WHEEL_SET), VehicleRegistry.PART_STANDARD_WHEELS);
        rover.tryInstallPart(player, new ItemStack(GCItems.STANDARD_WHEEL_SET), VehicleRegistry.PART_STANDARD_WHEELS);
        rover.tryInstallPart(player, new ItemStack(GCItems.STANDARD_WHEEL_SET), VehicleRegistry.PART_STANDARD_WHEELS);
        rover.tryInstallPart(player, new ItemStack(GCItems.LOX_FUEL_ENGINE), VehicleRegistry.PART_LOX_ENGINE);
        rover.tryInstallPart(player, new ItemStack(GCItems.BATTERY_BANK), VehicleRegistry.PART_BATTERY_BANK);
        rover.tryInstallPart(player, new ItemStack(GCItems.PILOT_COCKPIT_SEAT), VehicleRegistry.PART_PILOT_SEAT);

        if (rover.getDerivedStats().isDrivable()) {
            throw new GameTestAssertException("Rover with only 3 of 4 wheels installed must not be drivable yet");
        }

        // Install 4th wheel -> now drivable!
        rover.tryInstallPart(player, new ItemStack(GCItems.STANDARD_WHEEL_SET), VehicleRegistry.PART_STANDARD_WHEELS);
        if (!rover.getDerivedStats().isDrivable()) {
            throw new GameTestAssertException("Assembled Scout Rover should be drivable after installing all 4 wheels, electric motor, battery, and pilot seat");
        }

        rover.packIntoCrate(player);
        if (!rover.isRemoved()) {
            throw new GameTestAssertException("Vehicle should be removed after packing into crate");
        }

        ItemStack crate = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate.is(GCItems.VEHICLE_CRATE)) {
                crate = candidate;
                break;
            }
        }
        if (crate.isEmpty()) {
            throw new GameTestAssertException("Player did not receive VehicleCrateItem");
        }
        if (SpatialCargoItem.widthOf(crate) != 2 || SpatialCargoItem.heightOf(crate) != 2) {
            throw new GameTestAssertException("Scout Rover crate must occupy 2x2 rocket cargo slots");
        }
        VehicleConfiguration unpackedCfg = VehicleCrateItem.getConfiguration(crate);
        if (unpackedCfg.installedParts().size() != 7) {
            throw new GameTestAssertException("Expected 7 preserved parts in crate, got " + unpackedCfg.installedParts().size());
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rocketCargoGridHandlesMultiSlotVehicleCrates(GameTestHelper helper) {
        RocketEntity rocket = GCEntityTypes.ROCKET.create(helper.getLevel());
        if (rocket == null) {
            throw new GameTestAssertException("Failed to create RocketEntity");
        }
        RocketCargoGrid grid = rocket.getCargoGrid();

        ItemStack smallCrate = VehicleCrateItem.createCrate(VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_SCOUT_ROVER));
        ItemStack mediumCrate = VehicleCrateItem.createCrate(VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_UTILITY_BUGGY));
        ItemStack largeCrate = VehicleCrateItem.createCrate(VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_HEAVY_HAULER));

        // Place 2x2 at (0,0)
        if (!grid.placeAt(0, 0, smallCrate)) {
            throw new GameTestAssertException("Failed to place 2x2 SMALL crate at (0,0)");
        }
        // Overlapping 3x2 at (1,1) MUST fail
        if (grid.placeAt(1, 1, mediumCrate)) {
            throw new GameTestAssertException("Overlapping 3x2 MEDIUM crate at (1,1) should be rejected");
        }
        // Place 3x2 at (2,0)
        if (!grid.placeAt(2, 0, mediumCrate)) {
            throw new GameTestAssertException("Failed to place 3x2 MEDIUM crate at (2,0)");
        }
        // Place 4x3 at (5,0)
        if (!grid.placeAt(5, 0, largeCrate)) {
            throw new GameTestAssertException("Failed to place 4x3 LARGE crate at (5,0)");
        }
        // Out of bounds 4x3 at (6,0) in 9-column grid MUST fail
        if (grid.canPlaceAt(6, 0, largeCrate)) {
            throw new GameTestAssertException("Out-of-bounds 4x3 crate at (6,0) should be rejected");
        }

        // Removing from a secondary covered cell (1,1) of the 2x2 crate must return the crate and free (0,0)
        ItemStack removed = grid.removeAt(grid.toIndex(1, 1));
        if (!removed.is(GCItems.VEHICLE_CRATE)) {
            throw new GameTestAssertException("Expected to remove SMALL crate from secondary covered cell (1,1)");
        }
        if (grid.isOccupied(grid.toIndex(0, 0)) || grid.isOccupied(grid.toIndex(1, 1))) {
            throw new GameTestAssertException("Cells of removed 2x2 crate were not freed");
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void specializedBenchesEnforceCategoryIsolation(GameTestHelper helper) {
        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;

        BlockPos chassisPos = new BlockPos(1, 2, 1);
        BlockPos motorPos = new BlockPos(2, 2, 1);
        BlockPos wheelPos = new BlockPos(3, 2, 1);
        BlockPos accPos = new BlockPos(4, 2, 1);

        helper.setBlock(chassisPos, GCBlocks.CHASSIS_BENCH);
        helper.setBlock(motorPos, GCBlocks.ELECTRIC_MOTOR_BENCH);
        helper.setBlock(wheelPos, GCBlocks.WHEEL_LOCOMOTION_BENCH);
        helper.setBlock(accPos, GCBlocks.VEHICLE_ACCESSORIES_BENCH);

        VehiclePartFabricatorBlockEntity chassisBe = (VehiclePartFabricatorBlockEntity) helper.getBlockEntity(chassisPos);
        VehiclePartFabricatorBlockEntity motorBe = (VehiclePartFabricatorBlockEntity) helper.getBlockEntity(motorPos);
        VehiclePartFabricatorBlockEntity wheelBe = (VehiclePartFabricatorBlockEntity) helper.getBlockEntity(wheelPos);
        VehiclePartFabricatorBlockEntity accBe = (VehiclePartFabricatorBlockEntity) helper.getBlockEntity(accPos);

        if (chassisBe == null || motorBe == null || wheelBe == null || accBe == null) {
            throw new GameTestAssertException("Specialized vehicle bench block entities failed to initialize");
        }

        // Cross-category crafting MUST be rejected even in Creative mode
        if (chassisBe.tryFabricate(creative, VehicleRegistry.PART_BATTERY_BANK)) {
            throw new GameTestAssertException("Chassis bench must reject electric motor blueprints");
        }
        if (motorBe.tryFabricate(creative, VehicleRegistry.CHASSIS_SCOUT_ROVER)) {
            throw new GameTestAssertException("Electric motor bench must reject chassis blueprints");
        }
        if (wheelBe.tryFabricate(creative, VehicleRegistry.PART_GAUGE_SPEEDOMETER)) {
            throw new GameTestAssertException("Wheel bench must reject dashboard gauge blueprints");
        }
        if (accBe.tryFabricate(creative, VehicleRegistry.PART_STANDARD_WHEELS)) {
            throw new GameTestAssertException("Accessories bench must reject wheel blueprints");
        }

        // Matching category blueprints MUST succeed and produce the exact item
        if (!chassisBe.tryFabricate(creative, VehicleRegistry.CHASSIS_SCOUT_ROVER)
                || !chassisBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.SCOUT_ROVER_CHASSIS)) {
            throw new GameTestAssertException("Chassis bench failed to craft Scout Rover chassis");
        }
        if (!motorBe.tryFabricate(creative, VehicleRegistry.PART_BATTERY_BANK)
                || !motorBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.BATTERY_BANK)) {
            throw new GameTestAssertException("Electric motor bench failed to craft Battery Bank");
        }
        if (!wheelBe.tryFabricate(creative, VehicleRegistry.PART_STANDARD_WHEELS)
                || !wheelBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.STANDARD_WHEEL_SET)) {
            throw new GameTestAssertException("Wheel bench failed to craft Standard Wheel Set");
        }
        if (!accBe.tryFabricate(creative, VehicleRegistry.PART_GAUGE_SPEEDOMETER)
                || !accBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.GAUGE_SPEEDOMETER)) {
            throw new GameTestAssertException("Accessories bench failed to craft Speedometer Gauge");
        }

        // Phase 2 Rotorcraft blueprints also respect bench specialization
        chassisBe.getInventory().setItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        motorBe.getInventory().setItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        wheelBe.getInventory().setItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        accBe.getInventory().setItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);

        if (!chassisBe.tryFabricate(creative, VehicleRegistry.CHASSIS_MICRO_DRONE)
                || !chassisBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.MICRO_DRONE_CHASSIS)) {
            throw new GameTestAssertException("Chassis bench failed to craft Micro-Drone chassis");
        }
        if (!motorBe.tryFabricate(creative, VehicleRegistry.PART_VTOL_THRUSTER)
                || !motorBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.VTOL_THRUSTER)) {
            throw new GameTestAssertException("Electric motor bench failed to craft Electric VTOL Thruster");
        }
        if (!wheelBe.tryFabricate(creative, VehicleRegistry.PART_COAXIAL_MARS_ROTOR)
                || !wheelBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.COAXIAL_MARS_ROTOR)) {
            throw new GameTestAssertException("Locomotion bench failed to craft Coaxial Mars Rotor");
        }
        if (!accBe.tryFabricate(creative, VehicleFabricatorRecipes.TOOL_DRONE_REMOTE)
                || !accBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.DRONE_REMOTE_CONTROLLER)) {
            throw new GameTestAssertException("Accessories bench failed to craft Drone Remote Controller");
        }

        // Phase 3 Fixed-Wing blueprints also respect bench specialization
        chassisBe.getInventory().setItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        motorBe.getInventory().setItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        wheelBe.getInventory().setItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);

        if (!chassisBe.tryFabricate(creative, VehicleRegistry.CHASSIS_LIGHT_AIRPLANE)
                || !chassisBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.LIGHT_AIRPLANE_CHASSIS)) {
            throw new GameTestAssertException("Chassis bench failed to craft Light Airplane chassis");
        }
        if (!motorBe.tryFabricate(creative, VehicleRegistry.PART_ELECTRIC_PROPELLER)
                || !motorBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.ELECTRIC_PROPELLER)) {
            throw new GameTestAssertException("Electric motor bench failed to craft Electric Propeller");
        }
        if (!wheelBe.tryFabricate(creative, VehicleRegistry.PART_STANDARD_WING)
                || !wheelBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.STANDARD_AIRPLANE_WING)) {
            throw new GameTestAssertException("Locomotion bench failed to craft Standard Airplane Wing");
        }
        wheelBe.getInventory().setItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        if (!wheelBe.tryFabricate(creative, VehicleRegistry.PART_RETRACTABLE_GEAR)
                || !wheelBe.getInventory().getItem(VehiclePartFabricatorBlockEntity.OUTPUT_SLOT).is(GCItems.RETRACTABLE_LANDING_GEAR)) {
            throw new GameTestAssertException("Locomotion bench failed to craft Retractable Landing Gear");
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void remoteDronePairsAndFliesInAtmosphere(GameTestHelper helper) {
        ModularVehicleEntity drone = GCEntityTypes.MODULAR_VEHICLE.create(helper.getLevel());
        if (drone == null) {
            throw new GameTestAssertException("Failed to create ModularVehicleEntity for Micro-Drone");
        }
        BlockPos spawnPos = helper.absolutePos(new BlockPos(2, 2, 2));
        drone.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 0.1, spawnPos.getZ() + 0.5);
        drone.setConfiguration(VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_MICRO_DRONE));
        helper.getLevel().addFreshEntity(drone);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() - 2.0);
        ItemStack remote = new ItemStack(GCItems.DRONE_REMOTE_CONTROLLER);
        player.setItemInHand(InteractionHand.MAIN_HAND, remote);

        // Right-clicking drone with remote pairs its UUID
        drone.interact(player, InteractionHand.MAIN_HAND);
        if (!drone.getUUID().equals(DroneRemoteControllerItem.getLinkedDroneUuid(remote))) {
            throw new GameTestAssertException("Drone Remote Controller failed to pair with Micro-Drone UUID");
        }
        if (DroneRemoteControllerItem.findLinkedDrone(helper.getLevel(), player, remote) != drone) {
            throw new GameTestAssertException("findLinkedDrone did not resolve paired Micro-Drone within range");
        }

        double startY = drone.getY();
        long startEnergy = drone.getConfiguration().storedEnergy();
        for (int t = 0; t < 28; t++) {
            drone.applyRemoteControlInput(player, 0.85F, 0.0F, 3.0F, 1.0F, false, false);
            drone.tick();
        }

        if (drone.getY() <= startY + 1.5) {
            throw new GameTestAssertException("Expected Micro-Drone to climb > 1.5 blocks in Earth atmosphere, deltaY=" + (drone.getY() - startY));
        }
        if (drone.getRotorRpm() < ModularVehicleEntity.BLUR_DISC_RPM_THRESHOLD) {
            throw new GameTestAssertException("Expected rotor RPM >= 600 in flight, got " + drone.getRotorRpm());
        }
        if (drone.getCyclicPitchDeg() <= 5.0F) {
            throw new GameTestAssertException("Expected forward cyclic pitch tilt > 5 deg, got " + drone.getCyclicPitchDeg());
        }
        if (drone.getConfiguration().storedEnergy() >= startEnergy) {
            throw new GameTestAssertException("Expected electric Micro-Drone to consume battery energy during flight");
        }

        drone.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void moonVacuumBlocksAeroRotorAndAllowsVtolThruster(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos basePos = helper.absolutePos(new BlockPos(2, 2, 2));

        // 1. Aerodynamic rotor drone in Moon vacuum (0.0 atm): MUST be blocked (zero upward lift)
        ModularVehicleEntity aeroDrone = GCEntityTypes.MODULAR_VEHICLE.create(helper.getLevel());
        if (aeroDrone == null) {
            throw new GameTestAssertException("Failed to create aerodynamic drone");
        }
        aeroDrone.setPos(basePos.getX() + 0.5, basePos.getY() + 0.1, basePos.getZ() + 0.5);
        aeroDrone.setOnGround(true);
        aeroDrone.setEnvironmentForTest(PlanetaryEnvironment.MOON);
        aeroDrone.setConfiguration(VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_MICRO_DRONE));
        helper.getLevel().addFreshEntity(aeroDrone);

        double aeroStartY = aeroDrone.getY();
        for (int t = 0; t < 25; t++) {
            aeroDrone.applyRemoteControlInput(player, 0.0F, 0.0F, 0.0F, 1.0F, false, false);
            aeroDrone.tick();
        }
        if (aeroDrone.getY() > aeroStartY + 0.1) {
            throw new GameTestAssertException("Aerodynamic drone without VTOL_THRUSTER must NOT lift off in Moon vacuum, deltaY=" + (aeroDrone.getY() - aeroStartY));
        }

        // 2. Same aerodynamic Mars-coaxial drone on Mars (0.006 atm, 0.38g): MUST climb cleanly!
        aeroDrone.setEnvironmentForTest(PlanetaryEnvironment.MARS);
        double marsStartY = aeroDrone.getY();
        for (int t = 0; t < 28; t++) {
            aeroDrone.applyRemoteControlInput(player, 0.0F, 0.0F, 0.0F, 1.0F, false, false);
            aeroDrone.tick();
        }
        if (aeroDrone.getY() <= marsStartY + 1.2) {
            throw new GameTestAssertException("Mars Coaxial Micro-Drone should lift off in Mars thin atmosphere, deltaY=" + (aeroDrone.getY() - marsStartY));
        }
        aeroDrone.discard();

        // 3. Electric VTOL_THRUSTER drone in Moon vacuum (0.0 atm): MUST take off smoothly!
        ModularVehicleEntity vtolDrone = GCEntityTypes.MODULAR_VEHICLE.create(helper.getLevel());
        if (vtolDrone == null) {
            throw new GameTestAssertException("Failed to create VTOL thruster drone");
        }
        vtolDrone.setPos(basePos.getX() + 3.5, basePos.getY() + 0.1, basePos.getZ() + 0.5);
        vtolDrone.setOnGround(true);
        vtolDrone.setEnvironmentForTest(PlanetaryEnvironment.MOON);
        vtolDrone.setConfiguration(VehicleRegistry.createVacuumVtolPreset(VehicleRegistry.CHASSIS_MICRO_DRONE));
        helper.getLevel().addFreshEntity(vtolDrone);

        double vtolStartY = vtolDrone.getY();
        for (int t = 0; t < 28; t++) {
            vtolDrone.applyRemoteControlInput(player, 0.0F, 0.0F, 0.0F, 1.0F, false, false);
            vtolDrone.tick();
        }
        if (vtolDrone.getY() <= vtolStartY + 1.5) {
            throw new GameTestAssertException("Electric VTOL_THRUSTER drone failed to climb in Moon vacuum, deltaY=" + (vtolDrone.getY() - vtolStartY));
        }
        vtolDrone.discard();

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fixedWingAssemblyTakeoffRollAndGearRetraction(GameTestHelper helper) {
        ModularVehicleEntity plane = GCEntityTypes.MODULAR_VEHICLE.create(helper.getLevel());
        if (plane == null) {
            throw new GameTestAssertException("Failed to create ModularVehicleEntity for Light Airplane");
        }
        // Build a clear elevated runway above adjacent GameTest boxes so the airplane has an unobstructed takeoff roll
        BlockPos runwayOrigin = helper.absolutePos(new BlockPos(2, 22, 2));
        for (int dz = -2; dz <= 28; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                helper.getLevel().setBlockAndUpdate(runwayOrigin.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
        }
        plane.setPos(runwayOrigin.getX() + 0.5, runwayOrigin.getY() + 0.02, runwayOrigin.getZ() + 0.5);
        plane.setYRot(0.0F);
        plane.setOnGround(true);
        plane.setEnvironmentForTest(PlanetaryEnvironment.EARTH);
        plane.setConfiguration(VehicleConfiguration.emptyChassis(VehicleRegistry.CHASSIS_LIGHT_AIRPLANE));
        helper.getLevel().addFreshEntity(plane);

        if (plane.getDerivedStats().isDrivable()) {
            throw new GameTestAssertException("Bare Light Airplane chassis must not be flyable before wings/prop/gear/motor/battery/seat are installed");
        }

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(runwayOrigin.getX() + 0.5, runwayOrigin.getY(), runwayOrigin.getZ() + 0.5);

        // Piece-by-piece Immersive-Vehicles-style assembly
        plane.tryInstallPart(player, new ItemStack(GCItems.STANDARD_AIRPLANE_WING), VehicleRegistry.PART_STANDARD_WING);
        plane.tryInstallPart(player, new ItemStack(GCItems.STANDARD_AIRPLANE_WING), VehicleRegistry.PART_STANDARD_WING);
        plane.tryInstallPart(player, new ItemStack(GCItems.ELECTRIC_PROPELLER), VehicleRegistry.PART_ELECTRIC_PROPELLER);
        plane.tryInstallPart(player, new ItemStack(GCItems.RETRACTABLE_LANDING_GEAR), VehicleRegistry.PART_RETRACTABLE_GEAR);
        plane.tryInstallPart(player, new ItemStack(GCItems.RETRACTABLE_LANDING_GEAR), VehicleRegistry.PART_RETRACTABLE_GEAR);
        plane.tryInstallPart(player, new ItemStack(GCItems.LOX_FUEL_ENGINE), VehicleRegistry.PART_LOX_ENGINE);
        plane.tryInstallPart(player, new ItemStack(GCItems.BATTERY_BANK), VehicleRegistry.PART_BATTERY_BANK);
        plane.tryInstallPart(player, new ItemStack(GCItems.PILOT_COCKPIT_SEAT), VehicleRegistry.PART_PILOT_SEAT);

        if (plane.getDerivedStats().isDrivable()) {
            throw new GameTestAssertException("Light Airplane missing 3rd landing gear must not be flyable yet");
        }
        plane.tryInstallPart(player, new ItemStack(GCItems.RETRACTABLE_LANDING_GEAR), VehicleRegistry.PART_RETRACTABLE_GEAR);
        if (!plane.getDerivedStats().isDrivable()) {
            throw new GameTestAssertException("Fully assembled Light Airplane should be flyable");
        }

        player.startRiding(plane, true);
        double startY = plane.getY();
        long startEnergy = plane.getConfiguration().storedEnergy();

        // Test dedicated throttle quadrant retention & cutoff via handlePanelAction("set_throttle")
        plane.handlePanelAction(player, "set_throttle", "", "0.65");
        plane.tick();
        if (Math.abs(plane.getThrottle() - 0.65F) > 0.02F) {
            throw new GameTestAssertException("Expected throttle quadrant to retain 65% setting without holding keys, got " + plane.getThrottle());
        }
        plane.handlePanelAction(player, "set_throttle", "", "0.0");
        if (plane.getThrottle() > 0.01F) {
            throw new GameTestAssertException("Expected throttle cutoff to zero lever immediately, got " + plane.getThrottle());
        }

        // Runway takeoff roll: full electric throttle (100% via throttle quadrant) + elevator rotation (Space)
        plane.handlePanelAction(player, "set_throttle", "", "1.0");
        for (int t = 0; t < 75; t++) {
            boolean pullUp = t >= 18;
            plane.inputTick(0.0F, 0.0F, false, false, false, false, pullUp, false, false);
            plane.tick();
        }

        double deltaY = plane.getY() - startY;
        float speed = plane.getSyncedSpeed();
        float throttle = plane.getThrottle();
        float rpm = plane.getRotorRpm();
        float gear = plane.getGearProgress();
        long endEnergy = plane.getConfiguration().storedEnergy();
        double agl = plane.computeAltitudeAgl();

        // Clean up temporary runway blocks before assertions/succeed
        player.stopRiding();
        plane.discard();
        for (int dz = -2; dz <= 28; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                helper.getLevel().setBlockAndUpdate(runwayOrigin.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }

        if (deltaY <= 2.5) {
            throw new GameTestAssertException(String.format(
                    "Expected Light Airplane to take off and climb > 2.5 blocks on Earth, deltaY=%.2f speed=%.3f thr=%.2f rpm=%.0f",
                    deltaY, speed, throttle, rpm));
        }
        if (rpm < ModularVehicleEntity.BLUR_DISC_RPM_THRESHOLD) {
            throw new GameTestAssertException("Expected electric propeller RPM >= 600 at full throttle, got " + rpm);
        }
        if (endEnergy >= startEnergy) {
            throw new GameTestAssertException("Expected electric airplane to draw battery energy during takeoff & climb");
        }
        if (agl > ModularVehicleEntity.GEAR_RETRACT_AGL_BLOCKS && gear <= 0.05F) {
            throw new GameTestAssertException("Expected retractable landing gear to retract above 4 blocks AGL, gearProgress=" + gear);
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fixedWingStallRecoveryAndMoonVacuumHybridVtol(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos basePos = helper.absolutePos(new BlockPos(2, 2, 2));

        // 1. Mid-air stall recovery on Earth: airplane at zero speed < V_stall MUST enter stall & pitch nose down smoothly
        ModularVehicleEntity stallPlane = GCEntityTypes.MODULAR_VEHICLE.create(helper.getLevel());
        if (stallPlane == null) {
            throw new GameTestAssertException("Failed to create stall test airplane");
        }
        stallPlane.setPos(basePos.getX() + 0.5, basePos.getY() + 20.0, basePos.getZ() + 0.5);
        stallPlane.setOnGround(false);
        stallPlane.setEnvironmentForTest(PlanetaryEnvironment.EARTH);
        stallPlane.setConfiguration(VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_LIGHT_AIRPLANE));
        helper.getLevel().addFreshEntity(stallPlane);
        player.startRiding(stallPlane, true);

        boolean observedStall = false;
        for (int t = 0; t < 16; t++) {
            stallPlane.inputTick(0.0F, 0.0F, false, false, false, false, false, false, false);
            stallPlane.tick();
            if (stallPlane.isStalling()) {
                observedStall = true;
            }
        }
        if (!observedStall) {
            throw new GameTestAssertException("Expected mid-air zero-speed fixed-wing airplane to detect aerodynamic stall");
        }
        if (stallPlane.getCyclicPitchDeg() <= 8.0F) {
            throw new GameTestAssertException("Expected stalled airplane to pitch nose downward smoothly (> 8 deg), got " + stallPlane.getCyclicPitchDeg());
        }
        if (stallPlane.getSyncedSpeed() <= 0.05F) {
            throw new GameTestAssertException("Expected nose-down stall dive to accelerate airspeed, got " + stallPlane.getSyncedSpeed());
        }
        player.stopRiding();
        stallPlane.discard();

        // 2. Pure aerodynamic airplane in Moon vacuum (0.0 atm): MUST NOT lift off
        ModularVehicleEntity vacuumPlane = GCEntityTypes.MODULAR_VEHICLE.create(helper.getLevel());
        if (vacuumPlane == null) {
            throw new GameTestAssertException("Failed to create Moon vacuum airplane");
        }
        vacuumPlane.setPos(basePos.getX() + 0.5, basePos.getY() + 0.05, basePos.getZ() + 0.5);
        vacuumPlane.setOnGround(true);
        vacuumPlane.setEnvironmentForTest(PlanetaryEnvironment.MOON);
        vacuumPlane.setConfiguration(VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_LIGHT_AIRPLANE));
        helper.getLevel().addFreshEntity(vacuumPlane);
        player.startRiding(vacuumPlane, true);

        double vacStartY = vacuumPlane.getY();
        for (int t = 0; t < 35; t++) {
            vacuumPlane.inputTick(0.0F, 1.0F, true, false, false, false, true, false, false);
            vacuumPlane.tick();
        }
        if (vacuumPlane.getY() > vacStartY + 0.1) {
            throw new GameTestAssertException("Pure aerodynamic airplane must NOT lift off in Moon vacuum, deltaY=" + (vacuumPlane.getY() - vacStartY));
        }
        player.stopRiding();
        vacuumPlane.discard();

        // 3. Hybrid VTOL airplane (with VTOL_THRUSTER pods) in Moon vacuum: MUST lift off cleanly!
        ModularVehicleEntity hybridPlane = GCEntityTypes.MODULAR_VEHICLE.create(helper.getLevel());
        if (hybridPlane == null) {
            throw new GameTestAssertException("Failed to create Hybrid VTOL airplane");
        }
        hybridPlane.setPos(basePos.getX() + 3.5, basePos.getY() + 0.05, basePos.getZ() + 0.5);
        hybridPlane.setOnGround(true);
        hybridPlane.setEnvironmentForTest(PlanetaryEnvironment.MOON);
        hybridPlane.setConfiguration(VehicleRegistry.createVacuumVtolPreset(VehicleRegistry.CHASSIS_LIGHT_AIRPLANE));
        helper.getLevel().addFreshEntity(hybridPlane);
        player.startRiding(hybridPlane, true);

        double hybridStartY = hybridPlane.getY();
        for (int t = 0; t < 28; t++) {
            hybridPlane.inputTick(0.0F, 0.0F, false, false, false, false, true, false, false);
            hybridPlane.tick();
        }
        if (hybridPlane.getY() <= hybridStartY + 1.5) {
            throw new GameTestAssertException("Hybrid VTOL airplane failed to lift off vertically in Moon vacuum, deltaY=" + (hybridPlane.getY() - hybridStartY));
        }
        player.stopRiding();
        hybridPlane.discard();

        helper.succeed();
    }
}
