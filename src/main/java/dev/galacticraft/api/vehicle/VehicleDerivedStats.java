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
        boolean hasSolarRecharge,
        boolean hasRtgTrickle,
        @NotNull List<SeatSpec> seats,
        @NotNull List<String> missingRequiredHardpoints
) {
    public boolean isDrivable() {
        return this.missingRequiredHardpoints.isEmpty()
                && this.totalDriveForceN > 0.0
                && this.totalPowerW > 0.0;
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
        boolean hasSolar = false;
        boolean hasRtg = false;
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
            if (part.type() == HardpointType.AI_CORE) {
                hasAi = true;
            }
            if (part.type() == HardpointType.SEAT && part.seatRole() != null) {
                seats.add(new SeatSpec(hp.id(), hp.localOffset(), part.seatRole(), part.exitSideOffset()));
                if (part.seatRole() == SeatRole.PILOT) {
                    hasPilot = true;
                }
            }
        }

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
                hasSolar,
                hasRtg,
                List.copyOf(seats),
                List.copyOf(missing)
        );
    }
}
