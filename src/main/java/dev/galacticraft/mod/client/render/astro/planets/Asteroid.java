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

package dev.galacticraft.mod.client.render.astro.planets;

import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroStarRenderer;
import dev.galacticraft.mod.client.render.astro.planets.position.Positioner;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Asteroid extends Body {
    protected double absMagnitude;
    protected double slope;
    protected Vector3f color;

    public Asteroid(String id, Positioner positioner, double absMagnitude, double slope, Vector3f color) {
        super(id, positioner);
        this.absMagnitude = absMagnitude;
        this.slope = slope;
        this.color = color;
    }

    protected double getPhaseIntegral(double phase) {
        double a = Math.exp(-3.332 * Math.pow(Math.tan(Math.toRadians(phase / 2.0)), 0.631));
        double b = Math.exp(-1.862 * Math.pow(Math.tan(Math.toRadians(phase / 2.0)), 1.218));
        double c = a * (1.0 - this.slope) + b * this.slope;
        return -2.5 * Math.log10(c);
    }

    @Override
    public double getTotalMagnitude(StarBody sun, Body obs, double time) {
        Vector3d pos = this.getPosition(time);
        Vector3d toObs = obs.getPosition(time).sub(pos);
        Vector3d toSun = sun.getPosition(time).sub(pos);
        double distObs = toObs.length();
        double distSun = toSun.length();
        double phase = Math.toDegrees(toObs.angle(toSun));
        double sunMagnitude = sun.getAbsoluteMagnitude(time);
        return sunMagnitude + 26.74 + this.absMagnitude + this.getPhaseIntegral(phase) + 5.0 * Math.log10(distSun * distObs) + AstroSkyEngine.CONFIG.asteroidBonus;
    }

    @Override
    protected void render(StarBody sun, Body obs, double time) {
        if (AstroSkyEngine.CONFIG.enableAsteroids) {
            Vector3d viewPos = this.getPosition(time).sub(obs.getPosition(time));
            double magn = this.getTotalMagnitude(sun, obs, time);
            AstroStarRenderer.addDynamicStar(viewPos, magn, 0.0, this.color);
        }
    }
}
