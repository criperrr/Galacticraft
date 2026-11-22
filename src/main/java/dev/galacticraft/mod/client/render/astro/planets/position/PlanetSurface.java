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

import dev.galacticraft.mod.client.render.astro.AstroMath;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroSkyRenderer;
import dev.galacticraft.mod.client.render.astro.planets.PlanetManager;
import net.minecraft.client.Minecraft;
import org.joml.Vector3d;

public class PlanetSurface extends Positioner {
    private final double radius;

    public PlanetSurface(double radius) {
        this.radius = radius;
    }

    private double getAltitude() {
        Minecraft client = Minecraft.getInstance();
        double y = client.player != null ? client.player.getY() : 0.0;
        return Math.max(0.0, (y - (double) AstroSkyEngine.CONFIG.spaceStart) / (double) AstroSkyEngine.CONFIG.spaceScale * 5.6819E-8);
    }

    @Override
    public Vector3d getPosition(double time) {
        double longitude = (double) (-(AstroSkyEngine.getWorldTime() - 6000L)) / 24000.0 * 360.0;
        longitude -= PlanetManager.getTropicalAngle();
        longitude -= AstroSkyRenderer.getLongitude();
        double latitude = AstroSkyRenderer.getLatitude();
        double altRadius = this.radius + this.getAltitude();
        double y = altRadius * AstroMath.sin(latitude);
        double r = altRadius * AstroMath.cos(latitude);
        double x = r * AstroMath.cos(longitude);
        double z = r * AstroMath.sin(longitude);
        return new Vector3d(-x, -y, -z).rotateX(Math.toRadians(23.45));
    }

    @Override
    public Vector3d getPositionRelative(double time, Vector3d relative) {
        return this.getPosition(time).add(relative);
    }
}
