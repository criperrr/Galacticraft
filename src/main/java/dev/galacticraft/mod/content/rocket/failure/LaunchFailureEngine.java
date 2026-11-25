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

package dev.galacticraft.mod.content.rocket.failure;

import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

/**
 * Calibrated Weibull dynamic reliability and physical failure engine for Galacticraft 5 modular rockets.
 *
 * <p>Implements the continuous stress-reliability model:
 * <pre>
 * P_fail(I, sigma) = 1.0 - exp(-[max(0.0, 0.95 - I) / eta]^beta * sigma)
 * </pre>
 * with calibrated parameters {@code eta = 0.65} and {@code beta = 1.80}, plus square-root
 * diminishing-returns explosion blast power and physical failure mode curves.
 */
public final class LaunchFailureEngine {
    public static final float ETA = 0.65F;
    public static final float BETA = 1.80F;
    public static final float PRISTINE_THRESHOLD = 0.95F;

    public static final float STRESS_PAD_IDLE = 0.10F;
    public static final float STRESS_T0_IGNITION = 1.20F;
    public static final float STRESS_MAX_Q = 1.50F;
    public static final float STRESS_VACUUM_TRANSITION = 0.80F;
    public static final float STRESS_REENTRY = 2.00F;
    public static final float STRESS_PARACHUTE = 1.50F;

    public static final int CABIN_DECOMPRESSION_TAU_TICKS = 60;
    public static final float BASE_LEAK_DROPLETS_PER_TICK = 240.0F;
    public static final double SLENDERNESS_BENDING_THRESHOLD = 6.0;

    // Bitmask flags for active in-flight failures synced to HUD / client
    public static final int FAILURE_NONE = 0;
    public static final int FAILURE_FUEL_LEAK = 1;
    public static final int FAILURE_CABIN_BREACH = 1 << 1;
    public static final int FAILURE_ENGINE_FLAMEOUT = 1 << 2;
    public static final int FAILURE_VACUUM_NOZZLE = 1 << 3;
    public static final int FAILURE_AERODYNAMIC_SHEAR = 1 << 4;
    public static final int FAILURE_PARACHUTE_SHRED = 1 << 5;

    private LaunchFailureEngine() {
    }

    /**
     * Calculates dynamic failure probability using the calibrated Weibull stress-reliability formula.
     * Sanitizes inputs to reject negative values and NaNs, returning a value clamped within {@code [0.0, 1.0]}.
     */
    public static float calculateFailureProbability(float integrityFraction, float environmentalStress) {
        if (!Float.isFinite(integrityFraction) || !Float.isFinite(environmentalStress)) {
            return 0.0F;
        }
        if (environmentalStress <= 0.0F || integrityFraction >= PRISTINE_THRESHOLD) {
            return 0.0F;
        }
        float clampedIntegrity = Math.clamp(integrityFraction, 0.0F, 1.0F);
        float delta = Math.clamp(PRISTINE_THRESHOLD - clampedIntegrity, 0.0F, PRISTINE_THRESHOLD);
        double exponent = Math.pow(delta / (double) ETA, BETA) * environmentalStress;
        return (float) Math.clamp(1.0 - Math.exp(-exponent), 0.0, 1.0);
    }

    /**
     * Rolls a probabilistic Weibull failure check for a single {@link PartIntegrity} under {@code stress}.
     */
    public static boolean rollFailure(@NotNull PartIntegrity part, float stress, @NotNull RandomSource random) {
        float fraction = (float) part.fraction();
        return rollFailure(fraction, stress, random);
    }

    /**
     * Rolls a probabilistic Weibull failure check for an integrity fraction in {@code [0.0, 1.0]}.
     */
    public static boolean rollFailure(float integrityFraction, float stress, @NotNull RandomSource random) {
        float probability = calculateFailureProbability(integrityFraction, stress);
        if (probability <= 0.0F) {
            return false;
        }
        return random.nextFloat() < probability;
    }

    /**
     * Calculates explosion blast power using the harmonized square-root diminishing-returns formula:
     * {@code BlastPower = 4.0 + min(21.0, sqrt(fuelBuckets * 0.75))}.
     */
    public static float calculateExplosionPower(long fuelAmountDroplets) {
        if (fuelAmountDroplets <= 0L) {
            return 4.0F;
        }
        double fuelBuckets = fuelAmountDroplets / 81_000.0;
        return 4.0F + (float) Math.min(21.0, Math.sqrt(fuelBuckets * 0.75));
    }

    /**
     * Failure Mode 2: Propellant leak rate in droplets/tick scaled by tank damage and aerodynamic speed.
     */
    public static long computeFuelLeakDropletsPerTick(
            double tankIntegrityFraction,
            double speedBlocksPerSec,
            double terminalSpeedBlocksPerSec
    ) {
        double clampedI = Double.isFinite(tankIntegrityFraction) ? Math.clamp(tankIntegrityFraction, 0.0, 1.0) : 1.0;
        double damageFactor = Math.max(0.05, (1.0 - clampedI) * (1.0 - clampedI));
        double denom = Math.max(1.0, terminalSpeedBlocksPerSec);
        double ratio = Double.isFinite(speedBlocksPerSec) ? Math.max(0.0, speedBlocksPerSec) / denom : 0.0;
        double rate = BASE_LEAK_DROPLETS_PER_TICK * damageFactor * (1.0 + 0.5 * ratio * ratio);
        return Math.max(1L, Math.round(rate));
    }

    /**
     * Failure Mode 3: Exponential cabin pressure decay fraction {@code exp(-t / tau)} ({@code tau = 60 ticks}).
     */
    public static double computeCabinPressureFraction(int ticksSinceBreach) {
        if (ticksSinceBreach <= 0) {
            return 1.0;
        }
        return Math.exp(-(double) ticksSinceBreach / (double) CABIN_DECOMPRESSION_TAU_TICKS);
    }

    /**
     * Failure Mode 4: Engine flameout/chuffing oscillating thrust factor {@code 0.4 + 0.6 * sin(omega * t)}.
     */
    public static double computeFlameoutThrustFactor(int ticksSinceFlameout) {
        double omega = 0.55;
        return Math.clamp(0.4 + 0.6 * Math.sin(omega * Math.max(0, ticksSinceFlameout)), 0.0, 1.0);
    }

    /**
     * Evaluates aerodynamic lateral bending stress multiplier during Max-Q for slender or unfaired stacks.
     * Returns {@code STRESS_MAX_Q} nominally, increasing when {@code H/D > 6.0} or when an unfaired vehicle
     * pitches aggressively into dense dynamic pressure.
     */
    public static float computeMaxQBendingStress(
            double heightBlocks,
            double baseDiameterBlocks,
            double pitchDegrees,
            boolean hasFairing
    ) {
        double slenderness = heightBlocks / Math.max(1.0, baseDiameterBlocks);
        double stress = STRESS_MAX_Q;
        if (slenderness > SLENDERNESS_BENDING_THRESHOLD) {
            double excess = slenderness - SLENDERNESS_BENDING_THRESHOLD;
            stress += excess * 0.18;
        }
        if (!hasFairing && Math.abs(pitchDegrees) > 12.0) {
            stress += (Math.abs(pitchDegrees) - 12.0) * 0.025;
        }
        return (float) Math.clamp(stress, STRESS_MAX_Q, 3.5);
    }
}
