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
import com.mojang.math.Axis;
import dev.galacticraft.mod.client.render.astro.AstroColorUtils;
import dev.galacticraft.mod.client.render.astro.AstroSkyConfig;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroSkyRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.function.Supplier;

public class GridOverlay {
    private static final double STEP = 5.0;
    private final AstroSkyConfig.GridOptions options;
    private final Supplier<Matrix4f> matrixSupplier;

    public GridOverlay(AstroSkyConfig.GridOptions options, Supplier<Matrix4f> matrixSupplier) {
        this.options = options;
        this.matrixSupplier = matrixSupplier;
    }

    private void renderHorizontal(BufferBuilder bb, Matrix4f matrix, double altitude, Vector4f color) {
        for (double azimuth = 0.0; azimuth < 360.0; azimuth += STEP) {
            new SkyLine(azimuth, -altitude, azimuth + STEP, -altitude).render(bb, matrix, color);
        }
    }

    private void renderVertical(BufferBuilder bb, Matrix4f matrix, double azimuth, Vector4f color) {
        for (double altitude = -90.0; altitude < 90.0; altitude += STEP) {
            new SkyLine(180.0 + azimuth, altitude, 180.0 + azimuth, altitude + STEP).render(bb, matrix, color);
        }
    }

    public void renderGrid(BufferBuilder bufferBuilder) {
        Matrix4f matrix = this.matrixSupplier.get();
        if (matrix == null) {
            return;
        }
        Vector3f color = AstroColorUtils.fromHex(this.options.color);
        Vector4f majorColor = new Vector4f(color.x, color.y, color.z, 1.0F).mul(0.15F, 0.15F, 0.15F, 1.0F);
        Vector4f minorColor = new Vector4f(color.x, color.y, color.z, 1.0F).mul(0.05F, 0.05F, 0.05F, 1.0F);

        for (int i = -8; i < 9; i++) {
            this.renderHorizontal(bufferBuilder, matrix, 10.0 * (double) i, majorColor);
            if (AstroSkyEngine.isUsingSpyglass()) {
                for (int j = 0; j < 10; j++) {
                    this.renderHorizontal(bufferBuilder, matrix, 10.0 * (double) i + (double) j, minorColor);
                }
            }
        }

        for (int i = 0; i < 24; i++) {
            this.renderVertical(bufferBuilder, matrix, 15.0 * (double) i, majorColor);
            if (AstroSkyEngine.isUsingSpyglass()) {
                for (int j = 0; j < 15; j++) {
                    this.renderVertical(bufferBuilder, matrix, 15.0 * (double) i + (double) j, minorColor);
                }
            }
        }
    }

    public void renderCircle(BufferBuilder bufferBuilder) {
        Matrix4f matrix = this.matrixSupplier.get();
        if (matrix == null) {
            return;
        }
        Vector3f color = AstroColorUtils.fromHex(this.options.color);
        Vector4f circleColor = new Vector4f(color.x, color.y, color.z, 1.0F).mul(0.35F, 0.35F, 0.35F, 1.0F);
        this.renderHorizontal(bufferBuilder, matrix, 0.0, circleColor);
    }

    public void render(BufferBuilder bufferBuilder) {
        if (this.options.showCircle) {
            this.renderCircle(bufferBuilder);
        }
        if (this.options.showGrid) {
            this.renderGrid(bufferBuilder);
        }
    }

    public static Matrix4f getAzimuthalMatrix() {
        Matrix4f h = AstroSkyRenderer.getHorizontalMatrix();
        return h != null ? new Matrix4f(h).rotate(Axis.ZP.rotationDegrees(90.0F)) : null;
    }

    public static Matrix4f getEquatorialMatrix() {
        Matrix4f eq = AstroSkyRenderer.getEquatorialMatrix();
        return eq != null ? new Matrix4f(eq).rotate(Axis.YP.rotationDegrees(23.45F - (float) AstroSkyRenderer.getAxialTilt())) : null;
    }

    public static Matrix4f getEclipticMatrix() {
        Matrix4f eq = AstroSkyRenderer.getEquatorialMatrix();
        return eq != null ? new Matrix4f(eq).rotate(Axis.YP.rotationDegrees(23.45F)) : null;
    }

    public static Matrix4f getGalacticMatrix() {
        Matrix4f eq = AstroSkyRenderer.getEquatorialMatrix();
        return eq != null ? new Matrix4f(eq)
                .rotate(Axis.XP.rotationDegrees(-86.40512F))
                .rotate(Axis.ZN.rotationDegrees(-28.962F))
                .rotate(Axis.YN.rotationDegrees(58.68F))
                .rotate(Axis.XN.rotationDegrees((float) (23.45 - AstroSkyRenderer.getAxialTilt()))) : null;
    }
}
