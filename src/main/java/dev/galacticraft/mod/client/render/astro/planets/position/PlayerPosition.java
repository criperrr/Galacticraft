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

import net.minecraft.client.Minecraft;
import org.joml.Vector3d;

public class PlayerPosition extends Positioner {
    private double scale = 1.0;
    private Vector3d center = new Vector3d();

    public PlayerPosition() {
    }

    public PlayerPosition(double scale) {
        this.scale = scale;
    }

    public PlayerPosition(double scale, double x, double y, double z) {
        this(scale);
        this.center = new Vector3d(x, y, z);
    }

    @Override
    public Vector3d getPosition(double time) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return new Vector3d();
        }
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        return new Vector3d(x, y, z).sub(this.center).mul(this.scale);
    }

    @Override
    public Vector3d getPositionRelative(double time, Vector3d relative) {
        return this.getPosition(time).add(relative);
    }
}
