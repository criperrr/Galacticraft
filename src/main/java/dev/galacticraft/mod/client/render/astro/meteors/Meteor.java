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

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.galacticraft.mod.client.render.astro.AstroMath;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class Meteor extends AstroMath {
    private static final double START = 0.065;
    private static final double LENGTH = 0.51;
    private static final double WIDTH = 5.0E-6;
    private static final double THRESHOLD = 0.99;

    private final double start;
    private final double alpha;
    private final Vector3d radiant;
    private final Vector3d direction;
    private final Vector3d color;

    public Meteor(double time, double alpha, Vector3d radiant, Vector3d direction, Vector3d color) {
        this.start = time;
        this.alpha = alpha;
        this.radiant = radiant;
        this.direction = direction;
        this.color = color;
    }

    public double getStart() {
        return this.start;
    }

    public double getDuration() {
        return clamp(cbrt(this.alpha) * 6.0 / cbrt(1.0 - this.alpha), 0.35, 100.0);
    }

    public double getElapsed(double time) {
        return (time - this.start) / this.getDuration();
    }

    public boolean isBegun(double time) {
        return time >= this.start;
    }

    public boolean isFinished(double time) {
        return this.getElapsed(time) >= 1.0;
    }

    public Vector3d getRadiant() {
        return new Vector3d(this.radiant).mul(100.0);
    }

    public Vector3d getColor() {
        return this.color;
    }

    public Vector3d getPosition(double time) {
        double elapsed = this.getElapsed(time);
        double x = this.radiant.x + this.direction.x * (elapsed * LENGTH + START);
        double y = this.radiant.y + this.direction.y * (elapsed * LENGTH + START);
        double z = this.radiant.z + this.direction.z * (elapsed * LENGTH + START);
        return new Vector3d(x, y, z).normalize();
    }

    public void renderLine(BufferBuilder bufferBuilder, Matrix4f matrix, double time) {
        if (this.isBegun(time) && this.alpha < THRESHOLD) {
            Vector3d col = this.getColor();
            Vector3d rad = this.getRadiant();
            bufferBuilder.addVertex(matrix, (float) rad.x, (float) rad.y, (float) rad.z).setColor(0.0F, 0.0F, 0.0F, 0.0F);
            Vector3d end = this.getPosition(time).mul(100.0);
            float v = AstroSkyEngine.getVisibility();
            bufferBuilder.addVertex(matrix, (float) end.x, (float) end.y, (float) end.z)
                    .setColor((float) col.x, (float) col.y, (float) col.z, (float) (this.alpha * this.alpha) * v * v);
        }
    }

    public void renderTriangle(BufferBuilder bufferBuilder, Matrix4f matrix, double time) {
        if (this.isBegun(time) && this.alpha >= THRESHOLD) {
            Vector3d rad = this.getRadiant();
            Vector3d col = this.getColor();
            bufferBuilder.addVertex(matrix, (float) rad.x, (float) rad.y, (float) rad.z).setColor(0.0F, 0.0F, 0.0F, 0.0F);
            Vector3d end = this.getPosition(time);
            Vector3d cross = new Vector3d(rad).cross(end);
            cross.mul(WIDTH / Math.cbrt(1.0 - this.alpha));
            Vector3d end1 = new Vector3d(end).add(cross).normalize().mul(100.0);
            Vector3d end2 = new Vector3d(end).sub(cross).normalize().mul(100.0);
            float v = AstroSkyEngine.getVisibility();
            float a = (float) (this.alpha * this.alpha) * v * v;
            bufferBuilder.addVertex(matrix, (float) end1.x, (float) end1.y, (float) end1.z).setColor((float) col.x, (float) col.y, (float) col.z, a);
            bufferBuilder.addVertex(matrix, (float) end2.x, (float) end2.y, (float) end2.z).setColor((float) col.x, (float) col.y, (float) col.z, a);
        }
    }
}
