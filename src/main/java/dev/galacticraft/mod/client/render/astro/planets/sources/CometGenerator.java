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

package dev.galacticraft.mod.client.render.astro.planets.sources;

import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.planets.Body;
import dev.galacticraft.mod.client.render.astro.planets.Comet;
import dev.galacticraft.mod.client.render.astro.planets.position.Orbit;

import java.util.ArrayList;
import java.util.Random;

public class CometGenerator implements Source {
    private static final double PERIHELIC_MAX = 8.0;
    private final String primary;
    private final double semiMajorAxis;
    private final double period;
    private final double perYear;

    public CometGenerator(String primary, double count, double perYear) {
        this.primary = primary;
        this.period = count / perYear;
        this.semiMajorAxis = Math.cbrt(this.period * this.period);
        this.perYear = perYear;
    }

    public static CometGenerator buildWithArgs(String primary, String count, String perYear) {
        return new CometGenerator(primary, Double.parseDouble(count), Double.parseDouble(perYear));
    }

    public Comet generateNonPeriodic(long cycle) {
        Random rand = new Random(cycle * 651415236L + 1L);
        double eccentricValue = rand.nextDouble();
        double eccentricity = 1.0 - Math.pow(eccentricValue, 3.0) * 0.01 * PERIHELIC_MAX / (0.01 * this.semiMajorAxis);
        double inclination = 180.0 * rand.nextDouble();
        double nodalLongitude = 360.0 * rand.nextDouble();
        double longitude = 360.0 * rand.nextDouble();
        double epoch = (double) cycle / this.perYear;
        Orbit orbit = new Orbit(this.semiMajorAxis, this.period, eccentricity, inclination);
        orbit.sync(epoch, nodalLongitude, longitude, longitude);
        double m = 8.0 - 4.0 * rand.nextDouble() * rand.nextDouble();
        double k = 6.0 + 5.0 * rand.nextDouble();
        return new Comet(this.primary + ".comet-" + cycle, orbit, m, k);
    }

    @Override
    public Iterable<Body> getBodies() {
        if (AstroSkyEngine.CONFIG.enableComets && !AstroSkyEngine.CONFIG.planetTimeOptions.liveMode) {
            ArrayList<Body> comets = new ArrayList<>();
            for (long cycle = 0L; (double) cycle < this.perYear * this.period; cycle++) {
                comets.add(this.generateNonPeriodic(cycle));
            }
            return comets;
        }
        return new ArrayList<>();
    }
}
