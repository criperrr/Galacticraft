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

package dev.galacticraft.mod.client.render.astro.planets.position;

public class SatelliteOrbit extends Orbit {
    protected double periodNodalPrec = 0.0;
    protected double periodApsidalPrec = 0.0;

    public SatelliteOrbit(double semiMajorAxis, double period, double eccentricity, double inclination) {
        super(semiMajorAxis, period, eccentricity, inclination);
    }

    public SatelliteOrbit addPrecession(double periodNodal, double periodApsidal) {
        this.periodNodalPrec = periodNodal;
        this.periodApsidalPrec = periodApsidal;
        return this;
    }

    @Override
    protected double getLongAscending(double time) {
        return this.periodNodalPrec == 0.0 ? this.longAscendingAE : this.longAscendingAE - 360.0 * time / this.periodNodalPrec;
    }

    @Override
    protected double getArgPeriapsis(double time) {
        return this.periodApsidalPrec == 0.0
                ? this.longPeriapsisAE
                : this.longPeriapsisAE - this.getLongAscending(time) + 360.0 * time / this.periodApsidalPrec;
    }

    @Override
    protected double getMeanAnomaly(double time) {
        return this.longMeanAE - this.getLongAscending(time) - this.getArgPeriapsis(time) + 360.0 * this.getTimeSinceEpoch(time) / this.period;
    }
}
