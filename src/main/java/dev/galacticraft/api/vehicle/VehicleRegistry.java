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

import dev.galacticraft.api.APIConstants;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for modular electric vehicle chassis blueprints and installable parts,
 * using Immersive-Vehicles-style wheel-by-wheel hubs, separate electric traction motor +
 * battery/power bays, seats, cargo, instruments, and customizable dashboard dials.
 */
public final class VehicleRegistry {
    private static final Map<ResourceLocation, VehicleChassisDefinition> CHASSIS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, VehiclePartDefinition> PARTS = new LinkedHashMap<>();

    // Phase 1 Ground Chassis IDs
    public static final ResourceLocation CHASSIS_SCOUT_ROVER = APIConstants.id("scout_rover_chassis");
    public static final ResourceLocation CHASSIS_UTILITY_BUGGY = APIConstants.id("utility_buggy_chassis");
    public static final ResourceLocation CHASSIS_HEAVY_HAULER = APIConstants.id("heavy_hauler_chassis");

    // Phase 2 Rotorcraft & VTOL Chassis IDs (100% Electric)
    public static final ResourceLocation CHASSIS_MICRO_DRONE = APIConstants.id("micro_drone_chassis");
    public static final ResourceLocation CHASSIS_LIGHT_HELICOPTER = APIConstants.id("light_helicopter_chassis");
    public static final ResourceLocation CHASSIS_HEAVY_VTOL = APIConstants.id("heavy_vtol_chassis");

    // Phase 1 Part IDs (individual wheels, electric traction motor, electric energy sources, seats, cargo, gauges)
    public static final ResourceLocation PART_STANDARD_WHEELS = APIConstants.id("standard_wheel_set");
    public static final ResourceLocation PART_HEAVY_TRACKS = APIConstants.id("heavy_tracks");
    public static final ResourceLocation PART_ROCKER_BOGIE_6X6 = APIConstants.id("rocker_bogie_6x6");

    // Phase 2 Rotorcraft Lift & Electric Vacuum VTOL Thruster Parts
    public static final ResourceLocation PART_COAXIAL_MARS_ROTOR = APIConstants.id("coaxial_mars_rotor");
    public static final ResourceLocation PART_HEAVY_LIFT_ROTOR = APIConstants.id("heavy_lift_rotor");
    public static final ResourceLocation PART_VTOL_THRUSTER = APIConstants.id("vtol_thruster");

    public static final ResourceLocation PART_BATTERY_BANK = APIConstants.id("battery_bank");
    public static final ResourceLocation PART_SOLAR_ARRAY = APIConstants.id("solar_array_module");
    public static final ResourceLocation PART_RTG_CORE = APIConstants.id("rtg_core_module");
    public static final ResourceLocation PART_LOX_ENGINE = APIConstants.id("lox_fuel_engine");

    public static final ResourceLocation PART_PILOT_SEAT = APIConstants.id("pilot_cockpit_seat");
    public static final ResourceLocation PART_PASSENGER_SEAT = APIConstants.id("passenger_seat");
    public static final ResourceLocation PART_CREW_BENCH = APIConstants.id("crew_bench_seat");

    public static final ResourceLocation PART_CARGO_RACK = APIConstants.id("cargo_rack_bay");
    public static final ResourceLocation PART_HEAVY_CARGO_BAY = APIConstants.id("heavy_cargo_container");

    public static final ResourceLocation PART_CAMERA_MAST = APIConstants.id("camera_mast_360");
    public static final ResourceLocation PART_DRILL_ARM = APIConstants.id("geological_drill_arm");
    public static final ResourceLocation PART_AI_CORE = APIConstants.id("ai_core_module");

    // Customizable Cockpit Dashboard Gauges / Screens
    public static final ResourceLocation PART_GAUGE_SPEEDOMETER = APIConstants.id("gauge_speedometer");
    public static final ResourceLocation PART_GAUGE_POWER = APIConstants.id("gauge_battery_fuel");
    public static final ResourceLocation PART_GAUGE_ATMOSPHERE = APIConstants.id("gauge_atmosphere_o2");
    public static final ResourceLocation PART_GAUGE_COMPASS = APIConstants.id("gauge_compass_nav");
    public static final ResourceLocation PART_GAUGE_HORIZON = APIConstants.id("gauge_horizon_radar");

    static {
        registerDefaults();
    }

    private VehicleRegistry() {
    }

    public static void registerChassis(@NotNull VehicleChassisDefinition chassis) {
        CHASSIS.put(chassis.id(), chassis);
    }

    public static void registerPart(@NotNull VehiclePartDefinition part) {
        PARTS.put(part.id(), part);
    }

    public static @Nullable VehicleChassisDefinition getChassis(@NotNull ResourceLocation id) {
        return CHASSIS.get(id);
    }

    public static @NotNull VehicleChassisDefinition getChassisOrDefault(@NotNull ResourceLocation id) {
        VehicleChassisDefinition found = CHASSIS.get(id);
        return found != null ? found : CHASSIS.get(CHASSIS_SCOUT_ROVER);
    }

    public static @Nullable VehiclePartDefinition getPart(@NotNull ResourceLocation id) {
        return PARTS.get(id);
    }

    public static @NotNull Collection<VehicleChassisDefinition> allChassis() {
        return Collections.unmodifiableCollection(CHASSIS.values());
    }

    public static @NotNull Collection<VehiclePartDefinition> allParts() {
        return Collections.unmodifiableCollection(PARTS.values());
    }

    public static @NotNull Map<ResourceLocation, VehiclePartDefinition> partMap() {
        return Collections.unmodifiableMap(PARTS);
    }

    public static @NotNull VehicleDerivedStats computeStats(@NotNull VehicleConfiguration config) {
        VehicleChassisDefinition chassis = getChassisOrDefault(config.chassisId());
        return VehicleDerivedStats.compute(chassis, config, PARTS);
    }

    /**
     * Builds a ready-to-drive/fly starter configuration for a given chassis (creative crates, commands, tests).
     */
    public static @NotNull VehicleConfiguration createCompletePreset(@NotNull ResourceLocation chassisId) {
        if (CHASSIS_MICRO_DRONE.equals(chassisId)) {
            Map<String, ResourceLocation> parts = new LinkedHashMap<>();
            parts.put("rotor_fl", PART_COAXIAL_MARS_ROTOR);
            parts.put("rotor_fr", PART_COAXIAL_MARS_ROTOR);
            parts.put("rotor_rl", PART_COAXIAL_MARS_ROTOR);
            parts.put("rotor_rr", PART_COAXIAL_MARS_ROTOR);
            parts.put("motor_main", PART_LOX_ENGINE);
            parts.put("powerplant_main", PART_BATTERY_BANK);
            parts.put("instrument_mast", PART_CAMERA_MAST);
            return new VehicleConfiguration(CHASSIS_MICRO_DRONE, parts, 0xE6EAF0, 30_000L, 0L);
        }
        if (CHASSIS_LIGHT_HELICOPTER.equals(chassisId)) {
            Map<String, ResourceLocation> parts = new LinkedHashMap<>();
            parts.put("rotor_main", PART_HEAVY_LIFT_ROTOR);
            parts.put("rotor_tail", PART_HEAVY_LIFT_ROTOR);
            parts.put("motor_main", PART_LOX_ENGINE);
            parts.put("powerplant_main", PART_BATTERY_BANK);
            parts.put("seat_pilot", PART_PILOT_SEAT);
            parts.put("seat_passenger", PART_PASSENGER_SEAT);
            parts.put("cargo_rear", PART_CARGO_RACK);
            parts.put("instrument_mast", PART_CAMERA_MAST);
            parts.put("dash_left", PART_GAUGE_SPEEDOMETER);
            parts.put("dash_center", PART_GAUGE_HORIZON);
            parts.put("dash_right", PART_GAUGE_ATMOSPHERE);
            return new VehicleConfiguration(CHASSIS_LIGHT_HELICOPTER, parts, 0x3B7AC7, 30_000L, 0L);
        }
        if (CHASSIS_HEAVY_VTOL.equals(chassisId)) {
            Map<String, ResourceLocation> parts = new LinkedHashMap<>();
            parts.put("rotor_fl", PART_HEAVY_LIFT_ROTOR);
            parts.put("rotor_fr", PART_HEAVY_LIFT_ROTOR);
            parts.put("rotor_rl", PART_HEAVY_LIFT_ROTOR);
            parts.put("rotor_rr", PART_HEAVY_LIFT_ROTOR);
            parts.put("motor_main", PART_LOX_ENGINE);
            parts.put("powerplant_main", PART_RTG_CORE);
            parts.put("powerplant_aux", PART_BATTERY_BANK);
            parts.put("seat_pilot", PART_PILOT_SEAT);
            parts.put("seat_passenger_left", PART_PASSENGER_SEAT);
            parts.put("seat_passenger_right", PART_CREW_BENCH);
            parts.put("cargo_rear", PART_HEAVY_CARGO_BAY);
            parts.put("instrument_front", PART_DRILL_ARM);
            parts.put("dash_left", PART_GAUGE_SPEEDOMETER);
            parts.put("dash_center", PART_GAUGE_HORIZON);
            parts.put("dash_right", PART_GAUGE_POWER);
            return new VehicleConfiguration(CHASSIS_HEAVY_VTOL, parts, 0xD98B2B, 80_000L, 0L);
        }
        if (CHASSIS_HEAVY_HAULER.equals(chassisId)) {
            Map<String, ResourceLocation> parts = new LinkedHashMap<>();
            parts.put("wheel_fl", PART_ROCKER_BOGIE_6X6);
            parts.put("wheel_fr", PART_ROCKER_BOGIE_6X6);
            parts.put("wheel_ml", PART_ROCKER_BOGIE_6X6);
            parts.put("wheel_mr", PART_ROCKER_BOGIE_6X6);
            parts.put("wheel_rl", PART_ROCKER_BOGIE_6X6);
            parts.put("wheel_rr", PART_ROCKER_BOGIE_6X6);
            parts.put("motor_main", PART_LOX_ENGINE);
            parts.put("powerplant_main", PART_RTG_CORE);
            parts.put("powerplant_aux", PART_BATTERY_BANK);
            parts.put("seat_pilot", PART_PILOT_SEAT);
            parts.put("seat_passenger_left", PART_PASSENGER_SEAT);
            parts.put("seat_passenger_right", PART_CREW_BENCH);
            parts.put("cargo_rear", PART_HEAVY_CARGO_BAY);
            parts.put("instrument_front", PART_DRILL_ARM);
            parts.put("dash_left", PART_GAUGE_SPEEDOMETER);
            parts.put("dash_center", PART_GAUGE_COMPASS);
            parts.put("dash_right", PART_GAUGE_ATMOSPHERE);
            return new VehicleConfiguration(CHASSIS_HEAVY_HAULER, parts, 0xD98B2B, 80_000L, 0L);
        }
        if (CHASSIS_UTILITY_BUGGY.equals(chassisId)) {
            Map<String, ResourceLocation> parts = new LinkedHashMap<>();
            parts.put("wheel_fl", PART_STANDARD_WHEELS);
            parts.put("wheel_fr", PART_STANDARD_WHEELS);
            parts.put("wheel_rl", PART_STANDARD_WHEELS);
            parts.put("wheel_rr", PART_STANDARD_WHEELS);
            parts.put("motor_main", PART_LOX_ENGINE);
            parts.put("powerplant_main", PART_BATTERY_BANK);
            parts.put("seat_pilot", PART_PILOT_SEAT);
            parts.put("seat_passenger", PART_PASSENGER_SEAT);
            parts.put("cargo_rear", PART_CARGO_RACK);
            parts.put("instrument_mast", PART_CAMERA_MAST);
            parts.put("dash_left", PART_GAUGE_SPEEDOMETER);
            parts.put("dash_center", PART_GAUGE_POWER);
            parts.put("dash_right", PART_GAUGE_ATMOSPHERE);
            return new VehicleConfiguration(CHASSIS_UTILITY_BUGGY, parts, 0x3B7AC7, 30_000L, 0L);
        }
        Map<String, ResourceLocation> parts = new LinkedHashMap<>();
        parts.put("wheel_fl", PART_STANDARD_WHEELS);
        parts.put("wheel_fr", PART_STANDARD_WHEELS);
        parts.put("wheel_rl", PART_STANDARD_WHEELS);
        parts.put("wheel_rr", PART_STANDARD_WHEELS);
        parts.put("motor_main", PART_LOX_ENGINE);
        parts.put("powerplant_main", PART_BATTERY_BANK);
        parts.put("seat_pilot", PART_PILOT_SEAT);
        parts.put("cargo_rear", PART_CARGO_RACK);
        parts.put("instrument_mast", PART_CAMERA_MAST);
        parts.put("dash_center", PART_GAUGE_SPEEDOMETER);
        return new VehicleConfiguration(CHASSIS_SCOUT_ROVER, parts, 0xE6EAF0, 30_000L, 0L);
    }

    /**
     * Builds a vacuum-capable electric VTOL preset (fitted with {@link #PART_VTOL_THRUSTER} pods)
     * ready to fly on airless worlds such as the Moon, Phobos, Mercury, or Asteroids.
     */
    public static @NotNull VehicleConfiguration createVacuumVtolPreset(@NotNull ResourceLocation chassisId) {
        VehicleConfiguration base = createCompletePreset(chassisId);
        VehicleChassisDefinition chassis = getChassisOrDefault(chassisId);
        Map<String, ResourceLocation> swapped = new LinkedHashMap<>(base.installedParts());
        for (VehicleHardpoint hp : chassis.hardpoints()) {
            if (hp.type() == HardpointType.ROTOR) {
                swapped.put(hp.id(), PART_VTOL_THRUSTER);
            }
        }
        return new VehicleConfiguration(base.chassisId(), swapped, base.paintColor(), base.storedEnergy(), 0L);
    }

    private static void registerDefaults() {
        // 1. SMALL: NASA Apollo LRV / Artemis LTV (4 individual wheel hubs + motor + battery)
        registerChassis(new VehicleChassisDefinition(
                CHASSIS_SCOUT_ROVER,
                "vehicle.galacticraft.chassis.scout_rover",
                VehicleSizeClass.SMALL,
                VehicleDomain.GROUND,
                320.0,
                2.15,
                false,
                false,
                List.of(
                        new VehicleHardpoint("wheel_fl", HardpointType.LOCOMOTION, -1.10, 0.40, 1.00, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("wheel_fr", HardpointType.LOCOMOTION, 1.10, 0.40, 1.00, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("wheel_rl", HardpointType.LOCOMOTION, -1.10, 0.40, -1.00, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("wheel_rr", HardpointType.LOCOMOTION, 1.10, 0.40, -1.00, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("motor_main", HardpointType.MOTOR, 0.0, 0.68, 0.96, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("powerplant_main", HardpointType.POWERPLANT, 0.0, 0.72, -1.05, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("seat_pilot", HardpointType.SEAT, 0.0, 0.62, 0.12, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("cargo_rear", HardpointType.CARGO, 0.0, 0.92, -1.05, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("instrument_mast", HardpointType.INSTRUMENT, 0.68, 1.08, 0.85, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_center", HardpointType.DASHBOARD, 0.0, 1.02, 0.56, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("ai_core_slot", HardpointType.AI_CORE, -0.58, 0.92, 0.72, VehicleSizeClass.SMALL, false)
                )
        ));

        // 2. MEDIUM: NASA SEV / Artemis Pressurized Lunar Cruiser (4 individual wheel hubs + motor + battery)
        registerChassis(new VehicleChassisDefinition(
                CHASSIS_UTILITY_BUGGY,
                "vehicle.galacticraft.chassis.utility_buggy",
                VehicleSizeClass.MEDIUM,
                VehicleDomain.GROUND,
                680.0,
                2.95,
                false,
                true,
                List.of(
                        new VehicleHardpoint("wheel_fl", HardpointType.LOCOMOTION, -1.40, 0.54, 1.40, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("wheel_fr", HardpointType.LOCOMOTION, 1.40, 0.54, 1.40, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("wheel_rl", HardpointType.LOCOMOTION, -1.40, 0.54, -1.40, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("wheel_rr", HardpointType.LOCOMOTION, 1.40, 0.54, -1.40, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("motor_main", HardpointType.MOTOR, 0.0, 0.82, -0.78, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("powerplant_main", HardpointType.POWERPLANT, 0.0, 0.82, -1.48, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("seat_pilot", HardpointType.SEAT, -0.56, 1.00, 0.36, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("seat_passenger", HardpointType.SEAT, 0.56, 1.00, 0.36, VehicleSizeClass.MEDIUM, false),
                        new VehicleHardpoint("cargo_rear", HardpointType.CARGO, 0.0, 1.06, -1.42, VehicleSizeClass.MEDIUM, false),
                        new VehicleHardpoint("instrument_mast", HardpointType.INSTRUMENT, 0.92, 2.38, 0.65, VehicleSizeClass.MEDIUM, false),
                        new VehicleHardpoint("dash_left", HardpointType.DASHBOARD, -0.64, 1.40, 1.39, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_center", HardpointType.DASHBOARD, 0.0, 1.40, 1.39, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_right", HardpointType.DASHBOARD, 0.64, 1.40, 1.39, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("ai_core_slot", HardpointType.AI_CORE, 0.0, 1.15, 1.15, VehicleSizeClass.SMALL, false)
                )
        ));

        // 3. LARGE: NASA Mars 6x6 Pressurized Mobile Habitat (6 individual wheel hubs + motor + battery)
        registerChassis(new VehicleChassisDefinition(
                CHASSIS_HEAVY_HAULER,
                "vehicle.galacticraft.chassis.heavy_hauler",
                VehicleSizeClass.LARGE,
                VehicleDomain.GROUND,
                1450.0,
                4.20,
                true,
                true,
                List.of(
                        new VehicleHardpoint("wheel_fl", HardpointType.LOCOMOTION, -1.75, 0.66, 2.01, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("wheel_fr", HardpointType.LOCOMOTION, 1.75, 0.66, 2.01, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("wheel_ml", HardpointType.LOCOMOTION, -1.75, 0.66, 0.0, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("wheel_mr", HardpointType.LOCOMOTION, 1.75, 0.66, 0.0, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("wheel_rl", HardpointType.LOCOMOTION, -1.75, 0.66, -2.01, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("wheel_rr", HardpointType.LOCOMOTION, 1.75, 0.66, -2.01, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("motor_main", HardpointType.MOTOR, 0.0, 1.02, -0.80, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("powerplant_main", HardpointType.POWERPLANT, 0.0, 1.02, -2.05, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("powerplant_aux", HardpointType.POWERPLANT, 0.0, 2.86, -0.85, VehicleSizeClass.MEDIUM, false),
                        new VehicleHardpoint("seat_pilot", HardpointType.SEAT, -0.68, 1.24, 1.25, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("seat_passenger_left", HardpointType.SEAT, 0.68, 1.24, 1.25, VehicleSizeClass.LARGE, false),
                        new VehicleHardpoint("seat_passenger_right", HardpointType.SEAT, 0.0, 1.24, 0.45, VehicleSizeClass.LARGE, false),
                        new VehicleHardpoint("cargo_rear", HardpointType.CARGO, 0.0, 1.22, -1.75, VehicleSizeClass.LARGE, false),
                        new VehicleHardpoint("instrument_front", HardpointType.INSTRUMENT, 0.0, 1.18, 2.55, VehicleSizeClass.LARGE, false),
                        new VehicleHardpoint("dash_left", HardpointType.DASHBOARD, -0.76, 1.76, 1.88, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_center", HardpointType.DASHBOARD, 0.0, 1.76, 1.88, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_right", HardpointType.DASHBOARD, 0.76, 1.76, 1.88, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("ai_core_slot", HardpointType.AI_CORE, 0.0, 2.86, 0.85, VehicleSizeClass.SMALL, false)
                )
        ));

        // 4. SMALL ROTORCRAFT: NASA Ingenuity / Mars Scout Electric Quadcopter Drone (2x2 crate, remote piloted)
        registerChassis(new VehicleChassisDefinition(
                CHASSIS_MICRO_DRONE,
                "vehicle.galacticraft.chassis.micro_drone",
                VehicleSizeClass.SMALL,
                VehicleDomain.ROTORCRAFT,
                68.0,
                1.40,
                false,
                false,
                List.of(
                        new VehicleHardpoint("rotor_fl", HardpointType.ROTOR, -0.88, 0.70, 0.88, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("rotor_fr", HardpointType.ROTOR, 0.88, 0.70, 0.88, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("rotor_rl", HardpointType.ROTOR, -0.88, 0.70, -0.88, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("rotor_rr", HardpointType.ROTOR, 0.88, 0.70, -0.88, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("motor_main", HardpointType.MOTOR, 0.0, 0.34, 0.14, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("powerplant_main", HardpointType.POWERPLANT, 0.0, 0.34, -0.22, VehicleSizeClass.SMALL, true),
                        new VehicleHardpoint("cargo_belly", HardpointType.CARGO, 0.0, 0.12, 0.0, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("instrument_mast", HardpointType.INSTRUMENT, 0.0, 0.68, 0.34, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("ai_core_slot", HardpointType.AI_CORE, 0.0, 0.68, -0.20, VehicleSizeClass.SMALL, false)
                )
        ));

        // 5. MEDIUM ROTORCRAFT: NASA Atmospheric Pressurized Scout Helicopter (3x2 crate, 2 seats, retractable skids)
        registerChassis(new VehicleChassisDefinition(
                CHASSIS_LIGHT_HELICOPTER,
                "vehicle.galacticraft.chassis.light_helicopter",
                VehicleSizeClass.MEDIUM,
                VehicleDomain.ROTORCRAFT,
                480.0,
                2.80,
                false,
                true,
                List.of(
                        new VehicleHardpoint("rotor_main", HardpointType.ROTOR, 0.0, 2.52, 0.05, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("rotor_tail", HardpointType.ROTOR, 0.24, 1.56, -3.45, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("motor_main", HardpointType.MOTOR, 0.0, 1.55, -0.78, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("powerplant_main", HardpointType.POWERPLANT, 0.0, 0.86, -0.95, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("seat_pilot", HardpointType.SEAT, -0.46, 0.80, 0.42, VehicleSizeClass.MEDIUM, true),
                        new VehicleHardpoint("seat_passenger", HardpointType.SEAT, 0.46, 0.80, 0.42, VehicleSizeClass.MEDIUM, false),
                        new VehicleHardpoint("cargo_rear", HardpointType.CARGO, 0.0, 0.82, -0.45, VehicleSizeClass.MEDIUM, false),
                        new VehicleHardpoint("instrument_mast", HardpointType.INSTRUMENT, 0.0, 0.46, 1.62, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_left", HardpointType.DASHBOARD, -0.52, 1.24, 1.28, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_center", HardpointType.DASHBOARD, 0.0, 1.24, 1.28, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_right", HardpointType.DASHBOARD, 0.52, 1.24, 1.28, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("ai_core_slot", HardpointType.AI_CORE, 0.0, 1.85, -0.42, VehicleSizeClass.SMALL, false)
                )
        ));

        // 6. LARGE ROTORCRAFT: NASA Dragonfly Heavy Pressurized Cargo VTOL (4x3 crate, 3 seats, quad nacelles)
        registerChassis(new VehicleChassisDefinition(
                CHASSIS_HEAVY_VTOL,
                "vehicle.galacticraft.chassis.heavy_vtol",
                VehicleSizeClass.LARGE,
                VehicleDomain.ROTORCRAFT,
                1150.0,
                4.10,
                false,
                true,
                List.of(
                        new VehicleHardpoint("rotor_fl", HardpointType.ROTOR, -2.35, 2.28, 1.72, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("rotor_fr", HardpointType.ROTOR, 2.35, 2.28, 1.72, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("rotor_rl", HardpointType.ROTOR, -2.35, 2.28, -1.72, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("rotor_rr", HardpointType.ROTOR, 2.35, 2.28, -1.72, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("motor_main", HardpointType.MOTOR, 0.0, 2.15, -0.45, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("powerplant_main", HardpointType.POWERPLANT, 0.0, 1.00, -1.55, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("powerplant_aux", HardpointType.POWERPLANT, 0.0, 2.58, -0.85, VehicleSizeClass.MEDIUM, false),
                        new VehicleHardpoint("seat_pilot", HardpointType.SEAT, -0.58, 1.04, 1.48, VehicleSizeClass.LARGE, true),
                        new VehicleHardpoint("seat_passenger_left", HardpointType.SEAT, 0.58, 1.04, 1.48, VehicleSizeClass.LARGE, false),
                        new VehicleHardpoint("seat_passenger_right", HardpointType.SEAT, 0.0, 1.04, 0.62, VehicleSizeClass.LARGE, false),
                        new VehicleHardpoint("cargo_rear", HardpointType.CARGO, 0.0, 0.96, -1.25, VehicleSizeClass.LARGE, false),
                        new VehicleHardpoint("instrument_front", HardpointType.INSTRUMENT, 0.0, 0.72, 2.65, VehicleSizeClass.LARGE, false),
                        new VehicleHardpoint("dash_left", HardpointType.DASHBOARD, -0.66, 1.56, 2.12, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_center", HardpointType.DASHBOARD, 0.0, 1.56, 2.12, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("dash_right", HardpointType.DASHBOARD, 0.66, 1.56, 2.12, VehicleSizeClass.SMALL, false),
                        new VehicleHardpoint("ai_core_slot", HardpointType.AI_CORE, 0.0, 2.58, 0.82, VehicleSizeClass.SMALL, false)
                )
        ));

        // Individual Wheel / Locomotion Units (installed wheel-by-wheel onto each hub!)
        registerPart(new VehiclePartDefinition(
                PART_STANDARD_WHEELS, "item.galacticraft.standard_wheel_set",
                HardpointType.LOCOMOTION, VehicleSizeClass.SMALL,
                24.0, 720.0, 0.58, 0.0, 0L, 0L, 0, null, 0.0, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_HEAVY_TRACKS, "item.galacticraft.heavy_tracks",
                HardpointType.LOCOMOTION, VehicleSizeClass.MEDIUM,
                48.0, 1250.0, 0.46, 0.0, 0L, 0L, 0, null, 0.0, true, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_ROCKER_BOGIE_6X6, "item.galacticraft.rocker_bogie_6x6",
                HardpointType.LOCOMOTION, VehicleSizeClass.LARGE,
                62.0, 1400.0, 0.44, 0.0, 0L, 0L, 0, null, 0.0, true, false, false
        ));

        // Phase 2 Rotorcraft Blades & Electric Vacuum VTOL Thrusters (100% Electric, fuelCapacity == 0)
        registerPart(new VehiclePartDefinition(
                PART_COAXIAL_MARS_ROTOR, "item.galacticraft.coaxial_mars_rotor",
                HardpointType.ROTOR, VehicleSizeClass.SMALL,
                12.0, 0.0, 0.72, 0.0, 0L, 0L, 0, null, 0.0, false, false, false,
                7.0, 540.0, 0.0
        ));
        registerPart(new VehiclePartDefinition(
                PART_HEAVY_LIFT_ROTOR, "item.galacticraft.heavy_lift_rotor",
                HardpointType.ROTOR, VehicleSizeClass.MEDIUM,
                45.0, 0.0, 0.66, 0.0, 0L, 0L, 0, null, 0.0, false, false, false,
                16.0, 235.0, 0.0
        ));
        registerPart(new VehiclePartDefinition(
                PART_VTOL_THRUSTER, "item.galacticraft.vtol_thruster",
                HardpointType.ROTOR, VehicleSizeClass.SMALL,
                38.0, 0.0, 0.78, 0.0, 0L, 0L, 0, null, 0.0, false, false, false,
                0.0, 0.0, 5600.0
        ));

        // Electric Traction Motor (required in motor_main on every rover!)
        registerPart(new VehiclePartDefinition(
                PART_LOX_ENGINE, "item.galacticraft.lox_fuel_engine",
                HardpointType.MOTOR, VehicleSizeClass.SMALL,
                110.0, 1800.0, 0.64, 5200.0, 10_000L, 0L, 0, null, 0.0, false, false, false
        ));

        // 100% Electric Energy Sources (required in powerplant_main!)
        registerPart(new VehiclePartDefinition(
                PART_BATTERY_BANK, "item.galacticraft.battery_bank",
                HardpointType.POWERPLANT, VehicleSizeClass.SMALL,
                65.0, 0.0, 0.0, 4500.0, 30_000L, 0L, 0, null, 0.0, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_SOLAR_ARRAY, "item.galacticraft.solar_array_module",
                HardpointType.POWERPLANT, VehicleSizeClass.SMALL,
                55.0, 0.0, 0.0, 3200.0, 15_000L, 0L, 0, null, 0.0, false, true, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_RTG_CORE, "item.galacticraft.rtg_core_module",
                HardpointType.POWERPLANT, VehicleSizeClass.MEDIUM,
                140.0, 0.0, 0.0, 6800.0, 50_000L, 0L, 0, null, 0.0, false, false, true
        ));

        // Seats
        registerPart(new VehiclePartDefinition(
                PART_PILOT_SEAT, "item.galacticraft.pilot_cockpit_seat",
                HardpointType.SEAT, VehicleSizeClass.SMALL,
                35.0, 0.0, 0.0, 0.0, 0L, 0L, 0, SeatRole.PILOT, -1.35, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_PASSENGER_SEAT, "item.galacticraft.passenger_seat",
                HardpointType.SEAT, VehicleSizeClass.SMALL,
                30.0, 0.0, 0.0, 0.0, 0L, 0L, 0, SeatRole.PASSENGER, 1.35, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_CREW_BENCH, "item.galacticraft.crew_bench_seat",
                HardpointType.SEAT, VehicleSizeClass.MEDIUM,
                55.0, 0.0, 0.0, 0.0, 0L, 0L, 0, SeatRole.NPC_CREW, 1.55, false, false, false
        ));

        // Cargo bays
        registerPart(new VehiclePartDefinition(
                PART_CARGO_RACK, "item.galacticraft.cargo_rack_bay",
                HardpointType.CARGO, VehicleSizeClass.SMALL,
                45.0, 0.0, 0.0, 0.0, 0L, 0L, 18, null, 0.0, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_HEAVY_CARGO_BAY, "item.galacticraft.heavy_cargo_container",
                HardpointType.CARGO, VehicleSizeClass.LARGE,
                135.0, 0.0, 0.0, 0.0, 0L, 0L, 36, null, 0.0, false, false, false
        ));

        // Instruments & AI Core (AI logic arrives in Phase 4)
        registerPart(new VehiclePartDefinition(
                PART_CAMERA_MAST, "item.galacticraft.camera_mast_360",
                HardpointType.INSTRUMENT, VehicleSizeClass.SMALL,
                22.0, 0.0, 0.0, 0.0, 0L, 0L, 0, null, 0.0, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_DRILL_ARM, "item.galacticraft.geological_drill_arm",
                HardpointType.INSTRUMENT, VehicleSizeClass.MEDIUM,
                95.0, 0.0, 0.0, 0.0, 0L, 0L, 0, null, 0.0, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_AI_CORE, "item.galacticraft.ai_core_module",
                HardpointType.AI_CORE, VehicleSizeClass.SMALL,
                18.0, 0.0, 0.0, 0.0, 0L, 0L, 0, null, 0.0, false, false, false
        ));

        // Customizable Cockpit Dashboard Gauges / Screens
        registerPart(new VehiclePartDefinition(
                PART_GAUGE_SPEEDOMETER, "item.galacticraft.gauge_speedometer",
                HardpointType.DASHBOARD, VehicleSizeClass.SMALL,
                3.0, 0.0, 0.0, 0.0, 0L, 0L, 0, null, 0.0, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_GAUGE_POWER, "item.galacticraft.gauge_battery_fuel",
                HardpointType.DASHBOARD, VehicleSizeClass.SMALL,
                3.0, 0.0, 0.0, 0.0, 0L, 0L, 0, null, 0.0, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_GAUGE_ATMOSPHERE, "item.galacticraft.gauge_atmosphere_o2",
                HardpointType.DASHBOARD, VehicleSizeClass.SMALL,
                3.0, 0.0, 0.0, 0.0, 0L, 0L, 0, null, 0.0, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_GAUGE_COMPASS, "item.galacticraft.gauge_compass_nav",
                HardpointType.DASHBOARD, VehicleSizeClass.SMALL,
                3.0, 0.0, 0.0, 0.0, 0L, 0L, 0, null, 0.0, false, false, false
        ));
        registerPart(new VehiclePartDefinition(
                PART_GAUGE_HORIZON, "item.galacticraft.gauge_horizon_radar",
                HardpointType.DASHBOARD, VehicleSizeClass.SMALL,
                3.0, 0.0, 0.0, 0.0, 0L, 0L, 0, null, 0.0, false, false, false
        ));
    }
}
