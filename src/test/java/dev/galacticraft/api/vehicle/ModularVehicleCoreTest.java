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
}
