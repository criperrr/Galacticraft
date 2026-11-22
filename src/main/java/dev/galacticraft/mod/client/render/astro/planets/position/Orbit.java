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

import org.joml.Vector3d;

public class Orbit extends Positioner {
    protected double semiMajorAxis;
    protected double period;
    protected double eccentricity;
    protected double inclination;
    protected double epoch = 0.0;
    protected double longAscendingAE = 0.0;
    protected double longPeriapsisAE = 0.0;
    protected double longMeanAE = 0.0;

    public Orbit(double semiMajorAxis, double period, double eccentricity, double inclination) {
        this.semiMajorAxis = semiMajorAxis;
        this.period = period;
        this.eccentricity = eccentricity;
        this.inclination = inclination;
    }

    public Orbit sync(double epoch, double longAscending, double longPeriapsis, double longMean) {
        this.epoch = epoch;
        this.longAscendingAE = longAscending;
        this.longPeriapsisAE = longPeriapsis;
        this.longMeanAE = longMean;
        return this;
    }

    protected double getTimeSinceEpoch(double time) {
        return time - this.epoch;
    }

    protected double getLongAscending(double time) {
        return this.longAscendingAE;
    }

    protected double getArgPeriapsis(double time) {
        return this.longPeriapsisAE - this.longAscendingAE;
    }

    protected double getMeanAnomaly(double time) {
        return this.longMeanAE - this.longPeriapsisAE + 360.0 * this.getTimeSinceEpoch(time) / this.period;
    }

    protected double getEccentricAnomaly(double meanAnomaly) {
        double m = toRadians(mod(meanAnomaly + 180.0, 360.0) - 180.0);
        double e = abs(m) + abs(this.eccentricity * Math.sin(m)) + this.eccentricity * 0.5;

        for (int i = 0; i < 32; i++) {
            double change = (e - this.eccentricity * Math.sin(e) - abs(m)) / (1.0 - this.eccentricity * Math.cos(e));
            e -= change;
            if (abs(change) < 1.0E-8) {
                break;
            }
        }

        return m >= 0.0 ? toDegrees(e) : 360.0 - toDegrees(e);
    }

    protected double getTrueAnomaly(double eccAnomaly) {
        double b = this.eccentricity / (1.0 + sqrt(1.0 - this.eccentricity * this.eccentricity));
        return eccAnomaly + 2.0 * (360.0 + atan2(b * sin(eccAnomaly), 1.0 - b * cos(eccAnomaly))) % 360.0;
    }

    protected double getRadius(double eccAnomaly) {
        return this.semiMajorAxis * (1.0 - this.eccentricity * cos(eccAnomaly));
    }

    @Override
    public Vector3d getPosition(double time) {
        double meanAnomaly = this.getMeanAnomaly(time);
        double eccAnomaly = this.getEccentricAnomaly(meanAnomaly);
        double trueAnomaly = this.getTrueAnomaly(eccAnomaly);
        double radius = this.getRadius(eccAnomaly);
        Vector3d pos = new Vector3d(radius, 0.0, 0.0);
        pos.rotateY(toRadians(trueAnomaly));
        pos.rotateY(toRadians(this.getArgPeriapsis(time)));
        pos.rotateX(toRadians(this.inclination));
        pos.rotateY(toRadians(this.getLongAscending(time) + 180.0));
        return pos;
    }

    @Override
    public Vector3d getPositionRelative(double time, Vector3d relative) {
        Vector3d pos = new Vector3d(relative);
        pos.rotateX(toRadians(this.inclination));
        pos.rotateY(toRadians(this.getLongAscending(time)));
        pos.add(this.getPosition(time));
        return pos;
    }
}
