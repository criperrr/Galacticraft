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

import dev.galacticraft.mod.client.render.astro.AstroColorUtils;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroStarRenderer;
import dev.galacticraft.mod.client.render.astro.planets.position.Positioner;
import net.minecraft.util.Mth;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Random;

public class StarBody extends Body {
    protected double diameter;
    protected double absMagnitude;
    protected Vector3f color;
    protected double scatter = 0.0;
    protected double variablePeriod = 0.0;
    protected double variableAmplitude = 0.0;
    protected double eclipseEpoch = 0.0;
    protected double eclipsePeriod = 0.0;
    protected double eclipseAmplitude = 0.0;
    protected double eclipseDuration = 0.0;
    protected double eclipseSecondary = 0.0;

    public StarBody(String id, Positioner positioner, double absMagnitude, double diameter, Vector3f color) {
        super(id, positioner);
        this.diameter = diameter;
        this.absMagnitude = absMagnitude - 4.83 - 26.74;
        this.color = color;
    }

    public StarBody(String id, Positioner positioner, double absMagnitude, double temperature, double diameter) {
        this(id, positioner, absMagnitude, diameter, AstroColorUtils.fromTemperature(temperature));
    }

    public StarBody setScatter(double scatter) {
        this.scatter = scatter;
        return this;
    }

    public StarBody setPeriodicVariability(double period, double amplitude) {
        this.variablePeriod = period;
        this.variableAmplitude = amplitude;
        return this;
    }

    public StarBody setPeriodicEclipse(double epoch, double period, double amplitude, double duration, double secondary) {
        this.eclipseEpoch = epoch;
        this.eclipsePeriod = period;
        this.eclipseAmplitude = amplitude;
        this.eclipseDuration = duration;
        this.eclipseSecondary = secondary;
        return this;
    }

    public double getAbsoluteMagnitude(double time) {
        double magn = this.absMagnitude;
        if (this.eclipsePeriod > 0.0) {
            double x = (100.0 * (time - this.eclipseEpoch) * 365.2425 / this.eclipsePeriod + this.eclipseDuration / 2.0) % 100.0;
            if (x < this.eclipseDuration) {
                magn += this.eclipseAmplitude;
            } else if (x >= 50.0 && x < 50.0 + this.eclipseDuration * this.eclipseSecondary) {
                magn += this.eclipseAmplitude * this.eclipseSecondary;
            }
        }

        if (this.scatter > 0.0) {
            long seed = (long) ((time + this.scatter) * 141412.1);
            double a = new Random(seed).nextDouble() * 2.0 - 1.0;
            double b = new Random(seed + 1L).nextDouble() * 2.0 - 1.0;
            magn += this.scatter * Mth.lerp((time + this.scatter) * 141412.0 % 1.0, a, b);
        }

        if (this.variablePeriod > 0.0) {
            magn += this.variableAmplitude * Math.sin((Math.PI * 2.0) * time * 365.2425 / this.variablePeriod);
        }

        return magn;
    }

    @Override
    public double getTotalMagnitude(StarBody sun, Body obs, double time) {
        Vector3d pos = this.getPosition(time);
        Vector3d toObs = obs.getPosition(time).sub(pos);
        double distObs = toObs.length();
        return this.getAbsoluteMagnitude(time) + 5.0 * Math.log10(distObs);
    }

    @Override
    protected void render(StarBody sun, Body obs, double time) {
        Vector3d viewPos = this.getPosition(time).sub(obs.getPosition(time));
        double magn = this.getTotalMagnitude(sun, obs, time);
        if (magn < 1.0) {
            ModelManager.Model model = ModelManager.getModel("star");
            PlanetRenderer.Render render = PlanetRenderer.genRender(viewPos, viewPos, magn, AstroColorUtils.normalize(this.color));
            render.setDisk(model, this.diameter * (double) AstroSkyEngine.CONFIG.diameterScale);
            double nextTime = time + PlanetManager.getUpdateInterval();
            render.setMotion(time, nextTime, this.getPosition(nextTime).sub(obs.getPosition(nextTime)));
        } else {
            AstroStarRenderer.addDynamicStar(viewPos, magn, 0.0, AstroColorUtils.normalize(this.color));
        }
    }

    @Override
    public void renderTree(StarBody sun, Body obs, double time) {
        if (this != obs) {
            this.render(sun, obs, time);
        }
        for (Body child : this.children.values()) {
            child.renderTree(this, obs, time);
        }
    }
}
