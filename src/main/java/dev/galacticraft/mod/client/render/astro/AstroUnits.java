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

package dev.galacticraft.mod.client.render.astro;

import java.util.Map;

public class AstroUnits {
    private static final Map<Length, Double> LENGTH_UNITS = Map.ofEntries(
            Map.entry(Length.KM, 6.68459E-9),
            Map.entry(Length.AU, 1.0),
            Map.entry(Length.LY, 63241.1),
            Map.entry(Length.PC, 206265.0)
    );

    public static double convert(double value, Length sourceUnit, Length targetUnit) {
        Double source = LENGTH_UNITS.get(sourceUnit);
        Double target = LENGTH_UNITS.get(targetUnit);
        if (source == null || target == null || Double.isNaN(source) || Double.isNaN(target)) {
            return 0.0;
        }
        return value * source / target;
    }

    public enum Length {
        KM,
        AU,
        LY,
        PC
    }
}
