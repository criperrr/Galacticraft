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

package dev.galacticraft.mod.client.render.astro.meteors;

import dev.galacticraft.mod.client.render.astro.AstroMath;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.planets.PlanetManager;
import org.joml.Vector3d;

import java.util.List;

public class MeteorShower extends AstroMath {
    public static final List<MeteorShower> SHOWERS = List.of(
            new MeteorShower(271.5, 34.0, 32.32, 10.51, 18.0),
            new MeteorShower(337.5, -1.0, 45.5, 26.28, 50.0),
            new MeteorShower(291.0, 43.0, 50.0, 7.228, 3.0),
            new MeteorShower(43.5, 24.0, 76.6, 26.28, 30.0),
            new MeteorShower(340.5, 15.0, 107.5, 6.57, 5.0),
            new MeteorShower(340.5, -25.0, 125.0, 16.42, 5.0),
            new MeteorShower(280.5, 51.0, 125.3, 3.942, 5.0),
            new MeteorShower(307.5, -10.0, 127.0, 28.25, 5.0),
            new MeteorShower(340.5, -16.0, 127.0, 26.94, 25.0),
            new MeteorShower(40.5, -11.0, 135.0, 13.14, 3.0),
            new MeteorShower(48.0, 58.0, 140.0, 24.97, 100.0),
            new MeteorShower(91.5, 39.0, 158.6, 11.17, 6.0),
            new MeteorShower(48.0, 40.0, 166.7, 10.51, 5.0),
            new MeteorShower(151.5, 0.0, 184.3, 20.37, 5.0),
            new MeteorShower(163.5, 79.0, 192.6, 0.657, 5.0),
            new MeteorShower(262.5, 54.0, 195.4, 2.628, 10.0),
            new MeteorShower(84.0, 44.0, 198.0, 5.257, 2.0),
            new MeteorShower(102.0, 27.0, 205.0, 8.542, 3.0),
            new MeteorShower(94.5, 16.0, 208.0, 23.0, 20.0),
            new MeteorShower(162.0, 37.0, 211.0, 5.257, 2.0),
            new MeteorShower(52.5, 15.0, 223.0, 19.71, 7.0),
            new MeteorShower(58.5, 22.0, 230.0, 13.14, 5.0),
            new MeteorShower(151.5, 22.0, 235.3, 15.77, 10.0, 33.83),
            new MeteorShower(91.5, 16.0, 246.0, 15.11, 3.0),
            new MeteorShower(123.0, -45.0, 255.0, 9.199, 10.0),
            new MeteorShower(124.5, 2.0, 257.0, 11.17, 7.0),
            new MeteorShower(100.5, 8.0, 257.0, 9.856, 3.0),
            new MeteorShower(112.5, 33.0, 262.2, 10.51, 150.0),
            new MeteorShower(157.5, 30.0, 264.0, 19.06, 3.0),
            new MeteorShower(217.5, 76.0, 270.7, 5.914, 10.0),
            new MeteorShower(22.95, 49.0, 283.1, 4.928, 220.0),
            new MeteorShower(228.0, 67.0, 298.0, 7.885, 3.0),
            new MeteorShower(210.0, -59.0, 319.2, 13.8, 6.0)
    );

    private final double ra;
    private final double de;
    private final double peak;
    private final double range;
    private final double intensity;
    private final double stormPeriod;

    public MeteorShower(double ra, double de, double peak, double range, double intensity, double stormPeriod) {
        this.ra = ra;
        this.de = de;
        this.peak = peak;
        this.range = range;
        this.intensity = intensity;
        this.stormPeriod = stormPeriod;
    }

    public MeteorShower(double ra, double de, double peak, double range, double intensity) {
        this(ra, de, peak, range, intensity, 0.0);
    }

    public boolean isActive() {
        double diff = PlanetManager.getTropicalAngle() + 0.73921 - this.peak;
        return diff > -this.range && diff < this.range / 2.0;
    }

    public double getIntensity() {
        double stormMult = 1.0;
        if (this.stormPeriod > Double.MIN_NORMAL) {
            stormMult = 0.5 + 180.0 / mod(AstroSkyEngine.getTimeAngle(this.stormPeriod, -37.0 * this.stormPeriod), 360.0);
        }
        double peakIntensity = min(1000.0, this.intensity * stormMult * stormMult);
        double trueRange = this.range / stormMult;
        double tangle = PlanetManager.getTropicalAngle();
        double prog = clamp(1.0 - abs(tangle - this.peak) / (tangle < this.peak ? trueRange : trueRange / 2.0), 0.0, 1.0);
        return peakIntensity * prog;
    }

    public Vector3d getRadiant() {
        float i = (float) (100.0 * -sin(this.de));
        float j = (float) (100.0 * -cos(this.ra - this.peak) * cos(this.de));
        float k = (float) (100.0 * sin(this.ra - this.peak) * cos(this.de));
        return new Vector3d(i, j, k).normalize();
    }
}
