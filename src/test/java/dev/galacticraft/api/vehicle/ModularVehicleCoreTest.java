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

package dev.galacticraft.api.vehicle;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModularVehicleCoreTest {
    @Test
    void configurationCodecAndStreamCodecRoundTrip() {
        VehicleConfiguration original = VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_HEAVY_HAULER);

        var json = VehicleConfiguration.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        var decodedJson = VehicleConfiguration.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(original, decodedJson);

        var buf = Unpooled.buffer();
        VehicleConfiguration.STREAM_CODEC.encode(buf, original);
        var decodedBuf = VehicleConfiguration.STREAM_CODEC.decode(buf);
        assertEquals(original, decodedBuf);
    }

    @Test
    void emptyChassisReportsMissingRequiredHardpointsAndCompletePresetIsDrivable() {
        VehicleConfiguration bare = VehicleConfiguration.emptyChassis(VehicleRegistry.CHASSIS_SCOUT_ROVER);
        VehicleDerivedStats bareStats = VehicleRegistry.computeStats(bare);
        assertFalse(bareStats.isDrivable());
        assertTrue(bareStats.missingRequiredHardpoints().contains("wheel_fl"));
        assertTrue(bareStats.missingRequiredHardpoints().contains("wheel_fr"));
        assertTrue(bareStats.missingRequiredHardpoints().contains("wheel_rl"));
        assertTrue(bareStats.missingRequiredHardpoints().contains("wheel_rr"));
        assertTrue(bareStats.missingRequiredHardpoints().contains("motor_main"));
        assertTrue(bareStats.missingRequiredHardpoints().contains("powerplant_main"));
        assertTrue(bareStats.missingRequiredHardpoints().contains("seat_pilot"));

        VehicleConfiguration complete = VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_SCOUT_ROVER);
        VehicleDerivedStats completeStats = VehicleRegistry.computeStats(complete);
        assertTrue(completeStats.isDrivable());
        assertTrue(completeStats.missingRequiredHardpoints().isEmpty());
        assertTrue(completeStats.hasPilotSeat());
        assertEquals(18, completeStats.totalCargoSlots());
    }

    @Test
    void sizeClassesDeclareExpectedRocketCargoFootprints() {
        assertEquals(2, VehicleSizeClass.SMALL.cargoWidth());
        assertEquals(2, VehicleSizeClass.SMALL.cargoHeight());
        assertEquals(3, VehicleSizeClass.MEDIUM.cargoWidth());
        assertEquals(2, VehicleSizeClass.MEDIUM.cargoHeight());
        assertEquals(4, VehicleSizeClass.LARGE.cargoWidth());
        assertEquals(3, VehicleSizeClass.LARGE.cargoHeight());
    }

    @Test
    void planetaryGravityScalesGroundAcceleration() {
        VehicleDerivedStats stats = VehicleRegistry.computeStats(
                VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_UTILITY_BUGGY)
        );
        double earthAccel = stats.computeGroundAcceleration(1.0);
        double marsAccel = stats.computeGroundAcceleration(0.38);
        double moonAccel = stats.computeGroundAcceleration(0.166);
        assertTrue(earthAccel > marsAccel, "Earth grip should accelerate faster than Mars");
        assertTrue(marsAccel > moonAccel, "Mars grip should accelerate faster than Moon");
        assertTrue(moonAccel > 0.0, "Moon acceleration must remain positive");
    }

    @Test
    void mediumAndLargeNasaRoversHavePressurizedCabins() {
        assertFalse(VehicleRegistry.getChassisOrDefault(VehicleRegistry.CHASSIS_SCOUT_ROVER).pressurizedCabin());
        assertTrue(VehicleRegistry.getChassisOrDefault(VehicleRegistry.CHASSIS_UTILITY_BUGGY).pressurizedCabin());
        assertTrue(VehicleRegistry.getChassisOrDefault(VehicleRegistry.CHASSIS_HEAVY_HAULER).pressurizedCabin());
    }

    @Test
    void allVehiclePowerplantsArePureElectric() {
        for (VehiclePartDefinition part : VehicleRegistry.allParts()) {
            assertEquals(0L, part.fuelCapacity(), "All modular vehicles must be pure electric: " + part.id());
        }
    }

    @Test
    void rotorcraftAtmosphericLiftAndVacuumThrusterPhysics() {
        // 1. Micro-Drone with Mars Coaxial Rotors: flies on Earth, Mars, and Titan; blocked in Moon vacuum
        VehicleDerivedStats marsDrone = VehicleRegistry.computeStats(
                VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_MICRO_DRONE)
        );
        assertTrue(marsDrone.isRotorcraft());
        assertTrue(marsDrone.isDrivable());
        assertTrue(marsDrone.canHoverIn(1.0, 1.0), "Micro-Drone must hover on Earth");
        assertTrue(marsDrone.canHoverIn(0.38, 0.006), "Mars coaxial Micro-Drone must hover in thin Mars atmosphere");
        assertTrue(marsDrone.canHoverIn(0.138, 1.45), "Micro-Drone must hover on Titan");
        assertTrue(marsDrone.isBlockedByVacuum(0.0), "Aerodynamic drone must be flagged as blocked in vacuum");
        assertFalse(marsDrone.canHoverIn(0.166, 0.0), "Aerodynamic drone must not hover on the Moon without VTOL_THRUSTER");
        assertEquals(0.0, marsDrone.computeTotalRotorThrustN(0.0), 1e-6);

        // 2. Light Helicopter with Heavy Lift Rotors: hovers on Earth & Titan, underpowered on Mars, blocked on Moon
        VehicleDerivedStats earthHeli = VehicleRegistry.computeStats(
                VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_LIGHT_HELICOPTER)
        );
        assertTrue(earthHeli.canHoverIn(1.0, 1.0), "Light Helicopter with heavy rotors must hover on Earth");
        assertTrue(earthHeli.canHoverIn(0.138, 1.45), "Light Helicopter with heavy rotors must hover on Titan");
        assertFalse(earthHeli.canHoverIn(0.38, 0.006), "Standard heavy rotors should lack TWR on Mars thin air");
        assertTrue(earthHeli.isBlockedByVacuum(0.0), "Aerodynamic helicopter must be blocked in vacuum");

        // 3. Swapping to Electric VTOL_THRUSTER enables vacuum flight on Moon & Asteroids (0g)
        VehicleDerivedStats moonVtol = VehicleRegistry.computeStats(
                VehicleRegistry.createVacuumVtolPreset(VehicleRegistry.CHASSIS_LIGHT_HELICOPTER)
        );
        assertTrue(moonVtol.hasVacuumThrusters());
        assertFalse(moonVtol.isBlockedByVacuum(0.0));
        assertTrue(moonVtol.canHoverIn(0.166, 0.0), "VTOL_THRUSTER helicopter must hover on the Moon");
        assertTrue(moonVtol.canHoverIn(0.0, 0.0), "VTOL_THRUSTER helicopter must maneuver in 0g Asteroids");

        // 4. Heavy VTOL pressurized cabin and 4x3 cargo footprint
        VehicleChassisDefinition heavyVtol = VehicleRegistry.getChassisOrDefault(VehicleRegistry.CHASSIS_HEAVY_VTOL);
        assertTrue(heavyVtol.pressurizedCabin());
        assertEquals(VehicleSizeClass.LARGE, heavyVtol.sizeClass());
        assertEquals(4, heavyVtol.sizeClass().cargoWidth());
        assertEquals(3, heavyVtol.sizeClass().cargoHeight());

        // 5. Hardpoint type safety: rotor hubs reject ground wheels
        VehicleHardpoint rotorHp = heavyVtol.findHardpoint("rotor_fl");
        assertNotNull(rotorHp);
        assertFalse(rotorHp.accepts(HardpointType.LOCOMOTION, VehicleSizeClass.SMALL));
        assertTrue(rotorHp.accepts(HardpointType.ROTOR, VehicleSizeClass.SMALL));
        assertTrue(rotorHp.accepts(HardpointType.ROTOR, VehicleSizeClass.MEDIUM));
    }
}
