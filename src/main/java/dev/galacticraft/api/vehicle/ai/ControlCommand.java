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

package dev.galacticraft.api.vehicle.ai;

import org.jetbrains.annotations.NotNull;

/**
 * Domain-agnostic control setpoint produced each server tick by a {@link VehicleNavigator}
 * and applied directly to the vehicle's physical motion integrator.
 */
public record ControlCommand(
        double targetSpeed,
        float steeringAngleDeg,
        float yawRateDegPerTick,
        float throttle,
        float collective,
        float cyclicPitchDeg,
        float cyclicRollDeg,
        boolean brake,
        boolean pointTurn,
        boolean gearDown,
        boolean spoolDownRotors,
        boolean vtolHover
) {
    private static final ControlCommand STOP = new ControlCommand(
            0.0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, true, false, true, true, false
    );

    public static @NotNull ControlCommand stop() {
        return STOP;
    }

    public static @NotNull ControlCommand ground(
            double targetSpeed,
            float steeringAngleDeg,
            float yawRateDegPerTick,
            boolean brake,
            boolean pointTurn
    ) {
        return new ControlCommand(
                targetSpeed,
                Math.clamp(steeringAngleDeg, -35.0F, 35.0F),
                Math.clamp(yawRateDegPerTick, -10.0F, 10.0F),
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                brake,
                pointTurn,
                true,
                true,
                false
        );
    }

    public static @NotNull ControlCommand rotorcraft(
            float yawRateDegPerTick,
            float collective,
            float cyclicPitchDeg,
            float cyclicRollDeg,
            boolean gearDown,
            boolean spoolDownRotors
    ) {
        return new ControlCommand(
                0.0,
                0.0F,
                Math.clamp(yawRateDegPerTick, -8.0F, 8.0F),
                0.0F,
                Math.clamp(collective, -1.0F, 1.0F),
                Math.clamp(cyclicPitchDeg, -22.0F, 22.0F),
                Math.clamp(cyclicRollDeg, -22.0F, 22.0F),
                spoolDownRotors,
                false,
                gearDown,
                spoolDownRotors,
                false
        );
    }

    public static @NotNull ControlCommand fixedWing(
            float throttle,
            float yawRateDegPerTick,
            float targetPitchDeg,
            float targetRollDeg,
            boolean brake,
            boolean gearDown,
            boolean vtolHover,
            float vtolCollective
    ) {
        return new ControlCommand(
                0.0,
                Math.clamp(yawRateDegPerTick * 6.0F, -26.0F, 26.0F),
                Math.clamp(yawRateDegPerTick, -6.0F, 6.0F),
                Math.clamp(throttle, 0.0F, 1.0F),
                Math.clamp(vtolCollective, -1.0F, 1.0F),
                Math.clamp(targetPitchDeg, -24.0F, 24.0F),
                Math.clamp(targetRollDeg, -30.0F, 30.0F),
                brake,
                false,
                gearDown,
                brake && throttle <= 0.01F,
                vtolHover
        );
    }
}
