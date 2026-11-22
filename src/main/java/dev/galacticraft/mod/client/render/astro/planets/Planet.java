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
import dev.galacticraft.mod.client.render.astro.planets.position.PlanetSurface;
import dev.galacticraft.mod.client.render.astro.planets.position.Positioner;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Planet extends Body {
    private final String modelID;
    private final double diameter;
    private final double albedo;
    private final Vector3f color;
    private boolean hasRings = false;
    private double diameterScale = 1.0;
    private double positionScale = 1.0;
    private double magnBonus = 0.0;

    public Planet(String id, Positioner positioner, String modelID, double diameter, double albedo, Vector3f color) {
        super(id, positioner);
        this.modelID = modelID;
        this.diameter = diameter;
        this.albedo = albedo;
        this.color = color;
        if (AstroSkyEngine.CONFIG.bigPlanets) {
            this.scale(250.0, 250.0, -5.0);
        }
    }

    public Planet scale(double diameter, double position, double magnBonus) {
        this.diameterScale *= diameter;
        this.positionScale *= position;
        this.magnBonus += magnBonus;
        return this;
    }

    @Override
    public Vector3d getPosition(double time, Vector3d offset) {
        return super.getPosition(time, new Vector3d(offset).mul(this.positionScale));
    }

    public Planet addRings() {
        this.hasRings = true;
        return this;
    }

    @Override
    protected Positioner genMountPositioner(String mountType, double[] mountArgs) {
        if ("surface".equals(mountType)) {
            return new PlanetSurface(this.diameter / 2.0);
        }
        return super.genMountPositioner(mountType, mountArgs);
    }

    protected double getAbsoluteMagnitude(double time) {
        return 5.0 * Math.log10(8.883816E-6 / this.diameter / Math.sqrt(this.albedo)) + this.magnBonus;
    }

    @Override
    public double getTotalMagnitude(StarBody sun, Body obs, double time) {
        Vector3d pos = this.getPosition(time);
        Vector3d toObs = obs.getPosition(time).sub(pos);
        Vector3d toSun = sun.getPosition(time).sub(pos);
        double distObs = toObs.length();
        double distSun = toSun.length();
        if (distObs <= 1.0E-12 || distSun <= 1.0E-12) {
            return Double.POSITIVE_INFINITY;
        }
        double phase = Math.toDegrees(toObs.angle(toSun));
        double sunMagnitude = sun.getAbsoluteMagnitude(time);
        ModelManager.Model model = ModelManager.getModel(this.modelID);
        return sunMagnitude
                + 26.74
                + this.getAbsoluteMagnitude(time)
                + (model != null ? model.getPhaseReduction(phase) : 0.0)
                + 5.0 * Math.log10(distSun * distObs);
    }

    @Override
    protected void render(StarBody sun, Body obs, double time) {
        if (!AstroSkyEngine.CONFIG.enablePlanets || this == obs.getParent()) {
            return;
        }
        double magn = this.getTotalMagnitude(sun, obs, time);
        if (!Double.isFinite(magn) || magn > 20.0) {
            return;
        }
        Vector3d viewPos = this.getPosition(time).sub(obs.getPosition(time));
        if (viewPos.lengthSquared() <= 1.0E-24) {
            return;
        }
        Vector3d sunPos = this.getPosition(time).sub(sun.getPosition(time));
        ModelManager.Model model = ModelManager.getModel(this.modelID);
        PlanetRenderer.Render render = PlanetRenderer.genRender(viewPos, sunPos, magn, this.color);
        render.setDisk(model, this.diameter * this.diameterScale);
        if (this.hasRings) {
            render.addRing(2.0, 12.0, this.color);
        }

        double nextTime = time + PlanetManager.getUpdateInterval();
        render.setMotion(time, nextTime, this.getPosition(nextTime).sub(obs.getPosition(nextTime)));
    }
}
