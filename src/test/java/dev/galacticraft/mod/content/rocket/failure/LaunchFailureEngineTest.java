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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LaunchFailureEngine} verifying calibrated Weibull reliability
 * ({@code eta = 0.65, beta = 1.80}), NaN/negative sanitization, square-root explosion blast
 * scaling, and physical failure mode equations.
 */
public class LaunchFailureEngineTest {

    @Test
    void testCalibratedWeibullProbabilitiesAtIgnitionShock() {
        float sigma = LaunchFailureEngine.STRESS_T0_IGNITION; // 1.20

        // Pristine parts (>= 0.95) have zero failure risk
        assertEquals(0.0F, LaunchFailureEngine.calculateFailureProbability(1.00F, sigma), 1.0e-6F);
        assertEquals(0.0F, LaunchFailureEngine.calculateFailureProbability(0.95F, sigma), 1.0e-6F);

        // Spec calibrated anchor points (eta = 0.65, beta = 1.80, sigma = 1.20):
        // I = 0.94 -> ~0.06%
        float p94 = LaunchFailureEngine.calculateFailureProbability(0.94F, sigma);
        assertEquals(0.0006F, p94, 0.0003F, "I=0.94 should yield ~0.06% risk");

        // I = 0.85 (Field Serviced repair ceiling) -> ~4.05% (< 5%)
        float p85 = LaunchFailureEngine.calculateFailureProbability(0.85F, sigma);
        assertTrue(p85 > 0.035F && p85 < 0.05F, "I=0.85 should be ~4.05% (< 5%), got " + p85);

        // I = 0.75 -> ~13.4%
        float p75 = LaunchFailureEngine.calculateFailureProbability(0.75F, sigma);
        assertEquals(0.134F, p75, 0.01F, "I=0.75 should yield ~13.4% risk");

        // I = 0.40 (Warped) -> ~58.9%
        float p40 = LaunchFailureEngine.calculateFailureProbability(0.40F, sigma);
        assertEquals(0.589F, p40, 0.02F, "I=0.40 should yield ~58.9% risk");

        // I = 0.10 (Critical) -> ~85.7%
        float p10 = LaunchFailureEngine.calculateFailureProbability(0.10F, sigma);
        assertEquals(0.857F, p10, 0.02F, "I=0.10 should yield ~85.7% risk");

        // Strictly monotonic with wear
        assertTrue(p94 < p85 && p85 < p75 && p75 < p40 && p40 < p10);
    }

    @Test
    void testInputSanitizationRejectsNaNAndNegativeStress() {
        assertEquals(0.0F, LaunchFailureEngine.calculateFailureProbability(Float.NaN, 1.2F));
        assertEquals(0.0F, LaunchFailureEngine.calculateFailureProbability(0.5F, Float.NaN));
        assertEquals(0.0F, LaunchFailureEngine.calculateFailureProbability(0.5F, -2.0F));
        assertEquals(0.0F, LaunchFailureEngine.calculateFailureProbability(0.5F, 0.0F));
        assertEquals(0.0F, LaunchFailureEngine.calculateFailureProbability(Float.POSITIVE_INFINITY, 1.2F));

        float extreme = LaunchFailureEngine.calculateFailureProbability(-0.5F, 5.0F);
        assertTrue(extreme >= 0.0F && extreme <= 1.0F, "Clamped within [0, 1]");
    }

    @Test
    void testSquareRootExplosionPowerDiminishingReturns() {
        assertEquals(4.0F, LaunchFailureEngine.calculateExplosionPower(0L), 1.0e-5F);
        assertEquals(4.0F, LaunchFailureEngine.calculateExplosionPower(-5000L), 1.0e-5F);

        // 12 buckets -> 4.0 + sqrt(12 * 0.75) = 4.0 + 3.0 = 7.0
        assertEquals(7.0F, LaunchFailureEngine.calculateExplosionPower(81_000L * 12L), 1.0e-4F);

        // 100 buckets -> 4.0 + sqrt(75) ~= 12.66
        float p100 = LaunchFailureEngine.calculateExplosionPower(81_000L * 100L);
        assertEquals(4.0F + (float) Math.sqrt(75.0), p100, 1.0e-3F);

        // Huge fuel load capped at 4.0 + 21.0 = 25.0
        assertEquals(25.0F, LaunchFailureEngine.calculateExplosionPower(81_000L * 10_000L), 1.0e-5F);
    }

    @Test
    void testFailureModePhysicalCurves() {
        // Cabin depressurization tau = 60 ticks -> e^-1 ~= 0.3679 at t=60
        assertEquals(1.0, LaunchFailureEngine.computeCabinPressureFraction(0), 1.0e-6);
        assertEquals(Math.exp(-1.0), LaunchFailureEngine.computeCabinPressureFraction(60), 1.0e-4);

        // Fuel leak scales with damage and speed
        long slowClean = LaunchFailureEngine.computeFuelLeakDropletsPerTick(0.85, 5.0, 45.0);
        long fastDamaged = LaunchFailureEngine.computeFuelLeakDropletsPerTick(0.30, 45.0, 45.0);
        assertTrue(fastDamaged > slowClean * 5, "Damaged high-speed tank must leak much faster");

        // Flameout oscillates between 0 and 1
        double minVal = 1.0;
        double maxVal = 0.0;
        for (int t = 0; t < 40; t++) {
            double f = LaunchFailureEngine.computeFlameoutThrustFactor(t);
            assertTrue(f >= 0.0 && f <= 1.0);
            minVal = Math.min(minVal, f);
            maxVal = Math.max(maxVal, f);
        }
        assertTrue(maxVal > 0.9 && minVal < 0.2, "Flameout must produce strong thrust oscillation");

        // Slenderness H/D > 6.0 increases Max-Q bending stress
        float stubbyStress = LaunchFailureEngine.computeMaxQBendingStress(15.0, 5.0, 5.0, true);
        float slenderStress = LaunchFailureEngine.computeMaxQBendingStress(48.0, 5.0, 20.0, false);
        assertEquals(LaunchFailureEngine.STRESS_MAX_Q, stubbyStress, 1.0e-5F);
        assertTrue(slenderStress > stubbyStress + 0.5F);
    }
}
