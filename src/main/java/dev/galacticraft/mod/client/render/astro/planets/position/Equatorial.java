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
import org.joml.Vector3d;

public class Equatorial extends Positioner {
    private final Vector3d rectangle;
    private double epoch;
    private final Vector3d position;
    private Vector3d motion = null;

    public Equatorial(double rightAscension, double declination, double distance) {
        this.position = new Vector3d(rightAscension, declination, distance);
        this.rectangle = this.getRectangular(rightAscension, declination, distance);
    }

    public Equatorial setProperMotion(double epoch, double rightAscension, double declination, double distance) {
        this.epoch = epoch;
        this.motion = new Vector3d(rightAscension, declination, distance);
        return this;
    }

    private Vector3d getRectangular(double rightAscension, double declination, double distance) {
        double y = distance * AstroMath.sin(declination);
        double r = distance * AstroMath.cos(declination);
        double x = r * -AstroMath.cos(rightAscension);
        double z = r * AstroMath.sin(rightAscension);
        return new Vector3d(x, y, z);
    }

    @Override
    public Vector3d getPosition(double time) {
        if (this.motion == null) {
            return new Vector3d(this.rectangle);
        }
        double timeSinceEpoch = time - this.epoch;
        double ra = this.position.x + this.motion.x * timeSinceEpoch;
        double de = this.position.y + this.motion.y * timeSinceEpoch;
        double dist = this.position.z + this.motion.z * timeSinceEpoch;
        return this.getRectangular(ra, de, dist);
    }

    @Override
    public Vector3d getPositionRelative(double time, Vector3d relative) {
        return this.getPosition(time).add(relative);
    }
}
