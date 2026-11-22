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

package dev.galacticraft.mod.client.render.astro.overlays;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class SkyLine {
    public double srcRA = 0.0;
    public double srcDE = 0.0;
    public double dstRA = 0.0;
    public double dstDE = 0.0;

    public SkyLine() {
    }

    public SkyLine(double sra, double sde, double dra, double dde) {
        this.srcRA = sra;
        this.srcDE = sde;
        this.dstRA = dra;
        this.dstDE = dde;
    }

    public void render(BufferBuilder bufferBuilder, Matrix4f matrix, Vector4f color) {
        Matrix4f src = new Matrix4f(matrix)
                .rotateX((float) (-this.srcRA) * (float) Math.PI / 180.0F + (float) Math.PI)
                .rotateZ((float) (-this.srcDE) * (float) Math.PI / 180.0F);
        Matrix4f dst = new Matrix4f(matrix)
                .rotateX((float) (-this.dstRA) * (float) Math.PI / 180.0F + (float) Math.PI)
                .rotateZ((float) (-this.dstDE) * (float) Math.PI / 180.0F);
        bufferBuilder.addVertex(src, 0.0F, -100.0F, 0.0F).setColor(color.x, color.y, color.z, color.w);
        bufferBuilder.addVertex(dst, 0.0F, -100.0F, 0.0F).setColor(color.x, color.y, color.z, color.w);
    }
}
