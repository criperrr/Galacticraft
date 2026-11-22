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
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Comet extends Asteroid {
    private static final Vector3f COMA_COLOR = AstroColorUtils.fromHex(0xC0E0FF);
    private static final Vector3f GAS_TAIL_COLOR = AstroColorUtils.fromHex(0x0F003F);
    private static final Vector3f DUST_TAIL_COLOR = AstroColorUtils.fromHex(0xC0E0FF);

    public Comet(String id, Positioner positioner, double absMagnitude, double slopeK) {
        super(id, positioner, absMagnitude, slopeK, COMA_COLOR);
    }

    private double getTotalMagnitude(StarBody sun, double time) {
        double sunMagnitude = sun.getAbsoluteMagnitude(time);
        Vector3d pos = this.getPosition(time);
        Vector3d toSun = sun.getPosition(time).sub(pos);
        double distSun = toSun.length();
        return sunMagnitude + 26.74 + this.absMagnitude + this.slope * Math.log10(distSun) + AstroSkyEngine.CONFIG.cometBonus;
    }

    @Override
    public double getTotalMagnitude(StarBody sun, Body obs, double time) {
        Vector3d pos = this.getPosition(time);
        Vector3d toObs = obs.getPosition(time).sub(pos);
        double distObs = toObs.length();
        return this.getTotalMagnitude(sun, time) + 5.0 * Math.log10(distObs);
    }

    @Override
    protected void render(StarBody sun, Body obs, double time) {
        if (!AstroSkyEngine.CONFIG.enableComets) {
            return;
        }
        Vector3d position = this.getPosition(time);
        double magn = this.getTotalMagnitude(sun, obs, time);
        Vector3d comaPos = new Vector3d(position).sub(obs.getPosition(time));
        if (magn <= 5.0) {
            float tailLength = (float) Math.min(5.0, Math.pow(1.5, -this.getTotalMagnitude(sun, time)));
            Vector3d sunPos = sun.getPosition(time);
            double tailDelayMult = comaPos.distanceSquared(sunPos);
            Vector3d gasFront = this.getPosition(time).sub(sunPos).normalize((double) tailLength * 0.1);
            Vector3d gasRear = this.getPosition(time - 0.002 * tailDelayMult).sub(sunPos).normalize((double) tailLength * 0.12);
            Vector3d dustFront = this.getPosition(time - 0.005 * tailDelayMult).sub(sunPos).normalize((double) tailLength * 0.07);
            Vector3d dustRear = this.getPosition(time - 0.012 * tailDelayMult).sub(sunPos).normalize((double) tailLength * 0.09);
            Vector3d gasFrontPos = new Vector3d(comaPos).add(gasFront);
            Vector3d gasRearPos = new Vector3d(comaPos).add(gasRear);
            Vector3d dustFrontPos = new Vector3d(comaPos).add(dustFront);
            Vector3d dustRearPos = new Vector3d(comaPos).add(dustRear);
            PlanetRenderer.Render render = PlanetRenderer.genRender(comaPos, sunPos, magn, COMA_COLOR);
            render.addTail(gasFrontPos, gasRearPos, GAS_TAIL_COLOR);
            render.addTail(dustFrontPos, dustRearPos, DUST_TAIL_COLOR);
        } else {
            AstroStarRenderer.addDynamicStar(comaPos, magn, 0.0, COMA_COLOR);
        }
    }
}
