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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable modular vehicle component definition installed onto a matching {@link VehicleHardpoint}.
 */
public record VehiclePartDefinition(
        @NotNull ResourceLocation id,
        @NotNull String translationKey,
        @NotNull HardpointType type,
        @NotNull VehicleSizeClass sizeClass,
        double massKg,
        double driveForceN,
        double maxSpeedBlocksPerTick,
        double powerOutputW,
        long energyCapacity,
        long fuelCapacity,
        int cargoSlots,
        @Nullable SeatRole seatRole,
        double exitSideOffset,
        boolean skidSteerCapable,
        boolean solarRecharging,
        boolean rtgTrickle,
        double rotorSweptAreaM2,
        double rotorLiftFactor,
        double vacuumThrustN
) {
    public VehiclePartDefinition(
            @NotNull ResourceLocation id,
            @NotNull String translationKey,
            @NotNull HardpointType type,
            @NotNull VehicleSizeClass sizeClass,
            double massKg,
            double driveForceN,
            double maxSpeedBlocksPerTick,
            double powerOutputW,
            long energyCapacity,
            long fuelCapacity,
            int cargoSlots,
            @Nullable SeatRole seatRole,
            double exitSideOffset,
            boolean skidSteerCapable,
            boolean solarRecharging,
            boolean rtgTrickle
    ) {
        this(
                id, translationKey, type, sizeClass, massKg, driveForceN, maxSpeedBlocksPerTick,
                powerOutputW, energyCapacity, fuelCapacity, cargoSlots, seatRole, exitSideOffset,
                skidSteerCapable, solarRecharging, rtgTrickle, 0.0, 0.0, 0.0
        );
    }

    public VehiclePartDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(translationKey, "translationKey");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(sizeClass, "sizeClass");
        massKg = Math.max(1.0, massKg);
        driveForceN = Math.max(0.0, driveForceN);
        maxSpeedBlocksPerTick = Math.max(0.0, maxSpeedBlocksPerTick);
        powerOutputW = Math.max(0.0, powerOutputW);
        energyCapacity = Math.max(0L, energyCapacity);
        fuelCapacity = Math.max(0L, fuelCapacity);
        cargoSlots = Math.max(0, cargoSlots);
        rotorSweptAreaM2 = Math.max(0.0, rotorSweptAreaM2);
        rotorLiftFactor = Math.max(0.0, rotorLiftFactor);
        vacuumThrustN = Math.max(0.0, vacuumThrustN);
    }

    public boolean isVacuumThruster() {
        return this.type == HardpointType.ROTOR && this.vacuumThrustN > 0.0;
    }
}
