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

package dev.galacticraft.mod.world.gen;

import dev.galacticraft.mod.world.gen.density.AeolianDuneDensityFunction;
import dev.galacticraft.mod.world.gen.density.AnalyticalErosionDensity;
import dev.galacticraft.mod.world.gen.density.CraterFieldDensityFunction;
import dev.galacticraft.mod.world.gen.density.ShieldVolcanoDensityFunction;
import dev.galacticraft.mod.world.gen.density.SkylightShaftDensityFunction;
import dev.galacticraft.mod.world.gen.density.SmoothMinDensityFunction;
import dev.galacticraft.mod.world.gen.density.WorleyCaveDensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarsWorldgenTest {

    private static void assertDensityBoundsAndFillArray(DensityFunction fn) {
        int count = 64;
        DensityFunction.SinglePointContext[] points = new DensityFunction.SinglePointContext[count];
        for (int i = 0; i < count; i++) {
            points[i] = new DensityFunction.SinglePointContext((i * 37) - 900, -40 + (i * 4), (i * 53) - 1100);
        }

        double[] bulk = new double[count];
        fn.fillArray(bulk, new DensityFunction.ContextProvider() {
            @Override
            public DensityFunction.FunctionContext forIndex(int idx) {
                return points[idx];
            }

            @Override
            public void fillAllDirectly(double[] densities, DensityFunction densityFunction) {
                for (int i = 0; i < densities.length; i++) {
                    densities[i] = densityFunction.compute(points[i]);
                }
            }
        });

        for (int i = 0; i < count; i++) {
            double single = fn.compute(points[i]);
            assertFalse(Double.isNaN(single), "compute() returned NaN at index " + i);
            assertFalse(Double.isInfinite(single), "compute() returned Infinity at index " + i);
            assertTrue(single >= fn.minValue() - 1e-6 && single <= fn.maxValue() + 1e-6,
                    () -> "value " + single + " outside [" + fn.minValue() + ", " + fn.maxValue() + "]");
            assertEquals(single, bulk[i], 1e-12, "fillArray() mismatch vs compute() at index " + i);
        }
    }

    @Test
    void analyticalErosionBoundsAndVariation() {
        var fn = new AnalyticalErosionDensity(0.0045, 3, 0.14, 0.12, 0.65);
        assertDensityBoundsAndFillArray(fn);

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int x = -400; x <= 400; x += 25) {
            for (int z = -400; z <= 400; z += 25) {
                double v = fn.compute(new DensityFunction.SinglePointContext(x, 100, z));
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }
        assertTrue(max - min > 0.20, "Expected meaningful erosion relief variation, got " + (max - min));
    }

    @Test
    void craterFieldGeneratesBowlsAndRims() {
        var fn = new CraterFieldDensityFunction(240.0, 0.18, 0.06, 0.05, 0.48, 0x4D41525301L);
        assertDensityBoundsAndFillArray(fn);

        boolean foundBowl = false;
        boolean foundRim = false;
        for (int x = -800; x <= 800; x += 12) {
            for (int z = -800; z <= 800; z += 12) {
                double v = fn.compute(new DensityFunction.SinglePointContext(x, 90, z));
                if (v < -0.05) {
                    foundBowl = true;
                }
                if (v > 0.02) {
                    foundRim = true;
                }
            }
        }
        assertTrue(foundBowl, "Expected negative crater bowl excavation");
        assertTrue(foundRim, "Expected positive crater rim uplift");
    }

    @Test
    void shieldVolcanoNonNegativeAndLocalized() {
        var fn = new ShieldVolcanoDensityFunction(720.0, 0.45, 0.72, 135.0, 26.0, 0.19, 0x4F4C594DL);
        assertDensityBoundsAndFillArray(fn);

        boolean foundPeak = false;
        boolean foundFlat = false;
        for (int x = -1500; x <= 1500; x += 40) {
            for (int z = -1500; z <= 1500; z += 40) {
                double v = fn.compute(new DensityFunction.SinglePointContext(x, 140, z));
                assertTrue(v >= 0.0, "Shield volcano density must be >= 0");
                if (v > 0.30) {
                    foundPeak = true;
                }
                if (v == 0.0) {
                    foundFlat = true;
                }
            }
        }
        assertTrue(foundPeak, "Expected at least one Tharsis shield volcano dome");
        assertTrue(foundFlat, "Expected zero density outside volcano influence radius");
    }

    @Test
    void aeolianDunesProduceAsymmetricRidges() {
        var fn = new AeolianDuneDensityFunction(76.0, 0.032, 1.45, 28.0);
        assertDensityBoundsAndFillArray(fn);

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int x = 0; x < 240; x += 4) {
            double v = fn.compute(new DensityFunction.SinglePointContext(x, 90, x));
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        assertTrue(min < 0.0 && max > 0.0, "Dunes should oscillate around zero");
    }

    @Test
    void smoothMinMathematicalInvariants() {
        DensityFunction a = DensityFunctions.constant(0.5);
        DensityFunction b = DensityFunctions.constant(0.5);
        var smin = new SmoothMinDensityFunction(a, b, 0.4);
        assertDensityBoundsAndFillArray(smin);

        double blended = smin.compute(new DensityFunction.SinglePointContext(0, 0, 0));
        // When a == b, smin(a, b, k) == a - k / 4
        assertEquals(0.4, blended, 1e-9);

        // When |a - b| >= k, smin equals exact min(a, b)
        var far = new SmoothMinDensityFunction(DensityFunctions.constant(1.0), DensityFunctions.constant(0.2), 0.4);
        assertEquals(0.2, far.compute(new DensityFunction.SinglePointContext(0, 0, 0)), 1e-9);
    }

    @Test
    void skylightShaftRespectsVerticalBounds() {
        var fn = new SkylightShaftDensityFunction(160.0, 0.25, 6.5, 4.2, 40, 165, 0x4D415253L);
        assertDensityBoundsAndFillArray(fn);

        // Strictly zero outside [minY, maxY]
        for (int x = -400; x <= 400; x += 20) {
            assertEquals(0.0, fn.compute(new DensityFunction.SinglePointContext(x, 39, x)));
            assertEquals(0.0, fn.compute(new DensityFunction.SinglePointContext(x, 166, x)));
        }
    }

    @Test
    void worleyCavesSupportBubblesAndCellularNetworks() {
        var bubbles = new WorleyCaveDensityFunction(30.0, 4.5, 0.44, false);
        var network = new WorleyCaveDensityFunction(36.0, 5.5, 0.15, true);
        assertDensityBoundsAndFillArray(bubbles);
        assertDensityBoundsAndFillArray(network);

        boolean foundBubbleCarve = false;
        boolean foundNetworkCarve = false;
        for (int x = -120; x <= 120; x += 8) {
            for (int y = -30; y <= 30; y += 10) {
                for (int z = -120; z <= 120; z += 8) {
                    var ctx = new DensityFunction.SinglePointContext(x, y, z);
                    if (bubbles.compute(ctx) < -0.1) {
                        foundBubbleCarve = true;
                    }
                    if (network.compute(ctx) < -0.1) {
                        foundNetworkCarve = true;
                    }
                }
            }
        }
        assertTrue(foundBubbleCarve, "Expected Worley F1 bubble carving");
        assertTrue(foundNetworkCarve, "Expected Worley F2-F1 cellular network carving");
    }
}
