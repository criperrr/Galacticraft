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

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure derived performance and validation metrics computed from a {@link VehicleConfiguration}.
 */
public record VehicleDerivedStats(
        @NotNull VehicleSizeClass sizeClass,
        @NotNull VehicleDomain domain,
        double totalMassKg,
        double totalDriveForceN,
        double maxSpeedBlocksPerTick,
        double totalPowerW,
        long energyCapacity,
        long fuelCapacity,
        int totalCargoSlots,
        boolean skidSteerEnabled,
        boolean pressurizedCabin,
        boolean hasPilotSeat,
        boolean hasAiCore,
        boolean hasCameraMast,
        boolean hasDrillArm,
        boolean hasSolarRecharge,
        boolean hasRtgTrickle,
        double totalRotorAreaM2,
        double effectiveRotorLiftFactor,
        double totalVacuumThrustN,
        int rotorCount,
        double totalWingAreaM2,
        double effectiveWingMaxCl,
        double totalPropThrustN,
        int wingCount,
        int propellerCount,
        int landingGearCount,
        @NotNull List<SeatSpec> seats,
        @NotNull List<String> missingRequiredHardpoints
) {
    public static final double STANDARD_GRAVITY = 9.80665;
    public static final double MIN_AERO_PRESSURE_ATM = 0.001;
    public static final double MIN_FIXED_WING_PRESSURE_ATM = 0.005;
    public static final double SEA_LEVEL_AIR_DENSITY = 1.225;
    public static final double AERO_SPEED_SCALE = 52.0;

    public boolean isRotorcraft() {
        return this.domain == VehicleDomain.ROTORCRAFT || this.domain == VehicleDomain.HYBRID_VTOL;
    }

    public boolean isFixedWing() {
        return this.domain == VehicleDomain.FIXED_WING;
    }

    public boolean isHybridVtolFixedWing() {
        return this.isFixedWing() && this.totalVacuumThrustN > 0.0;
    }

    public boolean isAircraft() {
        return this.isRotorcraft() || this.isFixedWing();
    }

    public boolean hasVacuumThrusters() {
        return this.totalVacuumThrustN > 0.0;
    }

    public boolean hasScienceInstrument() {
        return this.hasCameraMast || this.hasDrillArm;
    }

    public boolean isDrivable() {
        if (!this.missingRequiredHardpoints.isEmpty() || this.totalPowerW <= 0.0) {
            return false;
        }
        if (this.isRotorcraft()) {
            return this.totalRotorAreaM2 > 0.0 || this.totalVacuumThrustN > 0.0;
        }
        if (this.isFixedWing()) {
            return this.totalWingAreaM2 > 0.0 && (this.totalPropThrustN > 0.0 || this.totalVacuumThrustN > 0.0);
        }
        return this.totalDriveForceN > 0.0;
    }

    /**
     * Computes gravity-scaled forward acceleration (blocks/tick^2) for planetary ground locomotion.
     */
    public double computeGroundAcceleration(double gravityG) {
        if (this.totalDriveForceN <= 0.0 || this.totalMassKg <= 0.0) {
            return 0.0;
        }
        double gripFactor = Math.clamp(Math.sqrt(Math.max(0.10, gravityG)), 0.35, 1.25);
        // Convert N/kg (m/s^2) to MC blocks/tick^2 (1 block/tick = 20 m/s, 20 TPS => divide by 400)
        // with responsive gameplay tuning factor
        double rawAccel = (this.totalDriveForceN / this.totalMassKg) * gripFactor / 180.0;
        return Math.clamp(rawAccel, 0.008, 0.085);
    }

    /**
     * Actuator-disc aerodynamic rotor thrust (Newtons):
     * {@code T_aero = k_rotor * (P * A_disc * rho_rel)^(1/3)}.
     * Returns {@code 0.0} in vacuum ({@code pressureAtm < 0.001}).
     */
    public double computeAerodynamicRotorThrustN(double pressureAtm) {
        double rhoRel = Math.clamp(pressureAtm, 0.0, 5.0);
        if (rhoRel < MIN_AERO_PRESSURE_ATM || this.totalRotorAreaM2 <= 0.0 || this.totalPowerW <= 0.0 || this.effectiveRotorLiftFactor <= 0.0) {
            return 0.0;
        }
        return this.effectiveRotorLiftFactor * Math.cbrt(this.totalPowerW * this.totalRotorAreaM2 * rhoRel);
    }

    /**
     * Total vertical lift thrust (Newtons) combining aerodynamic rotors and electric vacuum VTOL thrusters.
     */
    public double computeTotalRotorThrustN(double pressureAtm) {
        return this.computeAerodynamicRotorThrustN(pressureAtm) + this.totalVacuumThrustN;
    }

    /**
     * Computes fixed-wing aerodynamic lift force (Newtons):
     * {@code L = 0.5 * rho * V^2 * S_wing * C_L(alpha)}.
     * Returns {@code 0.0} in vacuum ({@code pressureAtm < 0.005}).
     */
    public double computeFixedWingLiftN(double airspeedBlocksPerTick, double pressureAtm, double liftCoefficient) {
        double rhoRel = Math.clamp(pressureAtm, 0.0, 5.0);
        if (rhoRel < MIN_FIXED_WING_PRESSURE_ATM || this.totalWingAreaM2 <= 0.0 || airspeedBlocksPerTick <= 0.0 || liftCoefficient <= 0.0) {
            return 0.0;
        }
        double rho = SEA_LEVEL_AIR_DENSITY * rhoRel;
        double vMs = airspeedBlocksPerTick * AERO_SPEED_SCALE;
        return 0.5 * rho * vMs * vMs * this.totalWingAreaM2 * liftCoefficient;
    }

    /**
     * Computes fixed-wing parasitic plus induced aerodynamic drag (Newtons):
     * {@code D = 0.5 * rho * V^2 * S_wing * (C_D0 + k * C_L^2)}.
     */
    public double computeFixedWingDragN(double airspeedBlocksPerTick, double pressureAtm, double liftCoefficient) {
        double rhoRel = Math.clamp(pressureAtm, 0.0, 5.0);
        if (rhoRel < MIN_FIXED_WING_PRESSURE_ATM || this.totalWingAreaM2 <= 0.0 || airspeedBlocksPerTick <= 0.0) {
            return 0.0;
        }
        double rho = SEA_LEVEL_AIR_DENSITY * rhoRel;
        double vMs = airspeedBlocksPerTick * AERO_SPEED_SCALE;
        double cd = 0.032 + 0.045 * liftCoefficient * liftCoefficient;
        return 0.5 * rho * vMs * vMs * this.totalWingAreaM2 * cd;
    }

    /**
     * Computes fixed-wing aerodynamic stall airspeed in Minecraft blocks/tick:
     * {@code V_stall = sqrt((2 * M * g) / (rho * S_wing * C_L_max)) / AERO_SPEED_SCALE}.
     * Returns {@link Double#POSITIVE_INFINITY} in vacuum ({@code pressureAtm < 0.005}) or with no wings.
     */
    public double computeStallSpeedBlocksPerTick(double gravityG, double pressureAtm) {
        double rhoRel = Math.clamp(pressureAtm, 0.0, 5.0);
        if (rhoRel < MIN_FIXED_WING_PRESSURE_ATM || this.totalWingAreaM2 <= 0.0 || this.effectiveWingMaxCl <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        double weightN = this.computeWeightN(gravityG);
        if (weightN <= 1.0) {
            return 0.0;
        }
        double rho = SEA_LEVEL_AIR_DENSITY * rhoRel;
        double vStallMs = Math.sqrt((2.0 * weightN) / (rho * this.totalWingAreaM2 * this.effectiveWingMaxCl));
        return vStallMs / AERO_SPEED_SCALE;
    }

    /**
     * Computes effective electric forward propulsion thrust (Newtons) combining atmospheric propellers
     * and optional hybrid vacuum VTOL thrusters.
     */
    public double computeForwardPropThrustN(double pressureAtm) {
        double rhoRel = Math.clamp(pressureAtm, 0.0, 5.0);
        double aeroProp = 0.0;
        if (rhoRel >= MIN_FIXED_WING_PRESSURE_ATM && this.totalPropThrustN > 0.0) {
            // Propellers retain useful thrust in thin air via variable-pitch high RPM, scaled by rho^(1/3)
            double densityScale = Math.clamp(Math.cbrt(rhoRel), 0.18, 1.35);
            aeroProp = this.totalPropThrustN * densityScale;
        }
        double vtolForward = this.totalVacuumThrustN * 0.45;
        return aeroProp + vtolForward;
    }

    /**
     * Planetary gravitational weight of this assembled vehicle in Newtons.
     */
    public double computeWeightN(double gravityG) {
        return this.totalMassKg * Math.max(0.0, gravityG) * STANDARD_GRAVITY;
    }

    /**
     * Computes Thrust-to-Weight Ratio (TWR) for rotorcraft or hybrid VTOL fixed-wing aircraft on the target planet.
     * On zero-gravity bodies (Asteroids), any positive thrust returns {@code 99.0} for full 6-DOF hover.
     */
    public double computeRotorTwr(double gravityG, double pressureAtm) {
        double thrust = this.computeTotalRotorThrustN(pressureAtm);
        if (thrust <= 0.0) {
            return 0.0;
        }
        double weight = this.computeWeightN(gravityG);
        if (weight <= 1.0) {
            return 99.0;
        }
        return thrust / weight;
    }

    /**
     * Returns {@code true} when an aerodynamic-only aircraft cannot generate lift due to vacuum.
     */
    public boolean isBlockedByVacuum(double pressureAtm) {
        if (this.isRotorcraft()) {
            return pressureAtm < MIN_AERO_PRESSURE_ATM && this.totalVacuumThrustN <= 0.0;
        }
        if (this.isFixedWing()) {
            return pressureAtm < MIN_FIXED_WING_PRESSURE_ATM && this.totalVacuumThrustN <= 0.0;
        }
        return false;
    }

    /**
     * Returns {@code true} when a rotorcraft or hybrid VTOL airplane has TWR &gt;= 1.0 for vertical takeoff/hover.
     */
    public boolean canHoverIn(double gravityG, double pressureAtm) {
        return this.isDrivable()
                && (this.isRotorcraft() || this.isHybridVtolFixedWing())
                && this.computeRotorTwr(gravityG, pressureAtm) >= 1.0;
    }

    /**
     * Returns {@code true} when a fixed-wing airplane can achieve aerodynamic takeoff on the target planet
     * (either via wing lift with {@code V_stall < V_max}, or via hybrid VTOL thrusters).
     */
    public boolean canFlyFixedWingIn(double gravityG, double pressureAtm) {
        if (!this.isDrivable() || !this.isFixedWing()) {
            return false;
        }
        if (this.canHoverIn(gravityG, pressureAtm)) {
            return true;
        }
        if (pressureAtm < MIN_FIXED_WING_PRESSURE_ATM) {
            return false;
        }
        double stallSpeed = this.computeStallSpeedBlocksPerTick(gravityG, pressureAtm);
        return Double.isFinite(stallSpeed) && stallSpeed < this.maxSpeedBlocksPerTick * 0.94;
    }

    public static @NotNull VehicleDerivedStats compute(
            @NotNull VehicleChassisDefinition chassis,
            @NotNull VehicleConfiguration config,
            @NotNull Map<ResourceLocation, VehiclePartDefinition> partLookup
    ) {
        double mass = chassis.dryMassKg();
        double driveForce = 0.0;
        double maxSpeed = 0.0;
        double power = 0.0;
        long energyCap = 0L;
        long fuelCap = 0L;
        int cargoSlots = 0;
        boolean skidSteer = chassis.skidSteerCapable();
        boolean hasPilot = false;
        boolean hasAi = false;
        boolean hasCameraMast = false;
        boolean hasDrillArm = false;
        boolean hasSolar = false;
        boolean hasRtg = false;
        double totalRotorArea = 0.0;
        double weightedLiftFactor = 0.0;
        double totalVacuumThrust = 0.0;
        int rotorCount = 0;
        double totalWingArea = 0.0;
        double weightedWingMaxCl = 0.0;
        double totalPropThrust = 0.0;
        int wingCount = 0;
        int propCount = 0;
        int gearCount = 0;
        List<SeatSpec> seats = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (VehicleHardpoint hp : chassis.hardpoints()) {
            ResourceLocation partId = config.getPart(hp.id());
            if (partId == null) {
                if (hp.required()) {
                    missing.add(hp.id());
                }
                continue;
            }
            VehiclePartDefinition part = partLookup.get(partId);
            if (part == null || !hp.accepts(part.type(), part.sizeClass())) {
                if (hp.required()) {
                    missing.add(hp.id());
                }
                continue;
            }

            mass += part.massKg();
            driveForce += part.driveForceN();
            if (part.maxSpeedBlocksPerTick() > maxSpeed) {
                maxSpeed = part.maxSpeedBlocksPerTick();
            }
            power += part.powerOutputW();
            energyCap += part.energyCapacity();
            fuelCap += part.fuelCapacity();
            cargoSlots += part.cargoSlots();
            skidSteer |= part.skidSteerCapable();
            hasSolar |= part.solarRecharging();
            hasRtg |= part.rtgTrickle();
            if (part.type() == HardpointType.ROTOR) {
                rotorCount++;
                totalRotorArea += part.rotorSweptAreaM2();
                weightedLiftFactor += part.rotorSweptAreaM2() * part.rotorLiftFactor();
                totalVacuumThrust += part.vacuumThrustN();
            }
            if (part.type() == HardpointType.WING) {
                wingCount++;
                totalWingArea += part.wingAreaM2();
                weightedWingMaxCl += part.wingAreaM2() * part.wingMaxCl();
            }
            if (part.type() == HardpointType.PROPELLER) {
                propCount++;
                totalPropThrust += part.propThrustN();
            }
            if (part.type() == HardpointType.LANDING_GEAR) {
                gearCount++;
            }
            if (part.type() == HardpointType.AI_CORE) {
                hasAi = true;
            }
            if (part.type() == HardpointType.INSTRUMENT) {
                if (VehicleRegistry.PART_DRILL_ARM.equals(part.id())) {
                    hasDrillArm = true;
                } else {
                    hasCameraMast = true;
                }
            }
            if (part.type() == HardpointType.SEAT && part.seatRole() != null) {
                seats.add(new SeatSpec(hp.id(), hp.localOffset(), part.seatRole(), part.exitSideOffset()));
                if (part.seatRole() == SeatRole.PILOT) {
                    hasPilot = true;
                }
            }
        }

        double effLiftFactor = totalRotorArea > 0.0 ? (weightedLiftFactor / totalRotorArea) : 0.0;
        double effWingMaxCl = totalWingArea > 0.0 ? (weightedWingMaxCl / totalWingArea) : 0.0;

        // Apply mass penalty to top speed when heavily loaded
        double massRatio = chassis.dryMassKg() / Math.max(chassis.dryMassKg(), mass);
        double effectiveMaxSpeed = maxSpeed * (0.65 + 0.35 * massRatio);

        return new VehicleDerivedStats(
                chassis.sizeClass(),
                chassis.defaultDomain(),
                mass,
                driveForce,
                effectiveMaxSpeed,
                power,
                energyCap,
                fuelCap,
                cargoSlots,
                skidSteer,
                chassis.pressurizedCabin(),
                hasPilot,
                hasAi,
                hasCameraMast,
                hasDrillArm,
                hasSolar,
                hasRtg,
                totalRotorArea,
                effLiftFactor,
                totalVacuumThrust,
                rotorCount,
                totalWingArea,
                effWingMaxCl,
                totalPropThrust,
                wingCount,
                propCount,
                gearCount,
                List.copyOf(seats),
                List.copyOf(missing)
        );
    }
}
