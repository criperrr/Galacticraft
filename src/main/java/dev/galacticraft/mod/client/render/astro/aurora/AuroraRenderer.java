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

package dev.galacticraft.mod.client.render.astro.aurora;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Procedural 3D Aurora Borealis renderer inspired by LabyMod Atmosphere & Real Sky.
 * Renders multi-layered undulating geomagnetic light curtains with vertical ray striations
 * and smooth vertical spectral color gradients (oxygen green -> cyan -> nitrogen violet/crimson)
 * in very cold biomes at night.
 */
public class AuroraRenderer {
    private static final int RIBBON_COUNT = 4;
    private static final int HORIZONTAL_SEGMENTS = 48;
    private static final float[] HEIGHT_FRACS = {0.0F, 0.16F, 0.55F, 1.0F};
    private static final float[] ALPHA_PROFILE = {0.0F, 1.0F, 0.42F, 0.0F};

    public static void render(Matrix4f horizontalMatrix) {
        ClientLevel level = AstroSkyEngine.getWorld();
        AuroraManager.update(level);

        float visibility = AuroraManager.getVisibility();
        if (visibility <= 0.005F || horizontalMatrix == null) {
            return;
        }

        AuroraManager.Palette palette = AuroraManager.getPalette();
        Random rand = new Random(AuroraManager.getLayoutSeed());

        // Real-time smooth animation in seconds
        float time = (float) ((System.currentTimeMillis() % 3_600_000L) / 1000.0);

        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int r = 0; r < RIBBON_COUNT; r++) {
            float baseRadius = 115.0F + r * 26.0F + rand.nextFloat() * 14.0F;
            float baseY = 38.0F + r * 8.5F + rand.nextFloat() * 10.0F;
            float curtainHeight = 36.0F + rand.nextFloat() * 24.0F;
            float arcStartDeg = -105.0F + (rand.nextFloat() - 0.5F) * 40.0F;
            float arcSpanDeg = 145.0F + rand.nextFloat() * 65.0F;
            float phaseOffset = rand.nextFloat() * Mth.TWO_PI;
            float speed = 0.24F + rand.nextFloat() * 0.18F;
            float ribbonAlpha = visibility * (0.42F - r * 0.055F);

            renderRibbon(
                    bb,
                    horizontalMatrix,
                    baseRadius,
                    baseY,
                    curtainHeight,
                    arcStartDeg,
                    arcSpanDeg,
                    phaseOffset,
                    speed,
                    time,
                    ribbonAlpha,
                    palette
            );
        }

        MeshData mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    private static void renderRibbon(
            BufferBuilder bb,
            Matrix4f matrix,
            float baseRadius,
            float baseY,
            float curtainHeight,
            float arcStartDeg,
            float arcSpanDeg,
            float phaseOffset,
            float speed,
            float time,
            float baseAlpha,
            AuroraManager.Palette palette
    ) {
        int cols = HORIZONTAL_SEGMENTS + 1;
        int rows = HEIGHT_FRACS.length;

        float[][] vx = new float[rows][cols];
        float[][] vy = new float[rows][cols];
        float[][] vz = new float[rows][cols];
        float[][] cr = new float[rows][cols];
        float[][] cg = new float[rows][cols];
        float[][] cb = new float[rows][cols];
        float[][] ca = new float[rows][cols];

        for (int i = 0; i < cols; i++) {
            float u = (float) i / (float) HORIZONTAL_SEGMENTS;
            // Smooth fade at both ends of the ribbon arc
            float endFade = Mth.sin(u * Mth.PI);
            endFade = endFade * endFade;

            float angleRad = (float) Math.toRadians(arcStartDeg + u * arcSpanDeg);

            // Layered organic wave undulation
            float wave1 = Mth.sin(u * 9.5F + time * speed + phaseOffset);
            float wave2 = Mth.sin(u * 19.0F - time * speed * 0.65F + phaseOffset * 1.7F);
            float radialOffset = (wave1 * 14.0F + wave2 * 6.5F);
            float yWave = Mth.cos(u * 6.2F + time * speed * 0.5F + phaseOffset) * 5.0F;

            // Vertical ray striations shimmering along magnetic field lines
            float ray1 = 0.5F + 0.5F * Mth.sin(u * 42.0F + time * 1.35F + phaseOffset);
            float ray2 = 0.5F + 0.5F * Mth.sin(u * 73.0F - time * 0.95F + phaseOffset * 2.3F);
            float rayIntensity = 0.52F + 0.48F * (ray1 * 0.65F + ray2 * 0.35F);

            float radius = baseRadius + radialOffset;
            float baseX = Mth.sin(angleRad) * radius;
            float baseZ = -Mth.cos(angleRad) * radius;

            // Slight inward tilt as curtain rises (perspective of geomagnetic field lines)
            float tiltScale = 0.08F;

            for (int j = 0; j < rows; j++) {
                float hFrac = HEIGHT_FRACS[j];
                float h = hFrac * curtainHeight;
                float rScale = 1.0F - hFrac * tiltScale;
                float topWave = Mth.sin(u * 13.0F + time * speed * 1.2F + hFrac * 2.0F) * (3.5F * hFrac);

                vx[j][i] = baseX * rScale + Mth.cos(angleRad) * topWave;
                vy[j][i] = baseY + yWave + h;
                vz[j][i] = baseZ * rScale + Mth.sin(angleRad) * topWave;

                Vector3f col = samplePaletteColor(palette, hFrac);
                cr[j][i] = col.x;
                cg[j][i] = col.y;
                cb[j][i] = col.z;
                ca[j][i] = Mth.clamp(baseAlpha * endFade * ALPHA_PROFILE[j] * rayIntensity, 0.0F, 1.0F);
            }
        }

        for (int j = 0; j < rows - 1; j++) {
            for (int i = 0; i < HORIZONTAL_SEGMENTS; i++) {
                // Triangle 1
                bb.addVertex(matrix, vx[j][i], vy[j][i], vz[j][i]).setColor(cr[j][i], cg[j][i], cb[j][i], ca[j][i]);
                bb.addVertex(matrix, vx[j][i + 1], vy[j][i + 1], vz[j][i + 1]).setColor(cr[j][i + 1], cg[j][i + 1], cb[j][i + 1], ca[j][i + 1]);
                bb.addVertex(matrix, vx[j + 1][i + 1], vy[j + 1][i + 1], vz[j + 1][i + 1]).setColor(cr[j + 1][i + 1], cg[j + 1][i + 1], cb[j + 1][i + 1], ca[j + 1][i + 1]);

                // Triangle 2
                bb.addVertex(matrix, vx[j][i], vy[j][i], vz[j][i]).setColor(cr[j][i], cg[j][i], cb[j][i], ca[j][i]);
                bb.addVertex(matrix, vx[j + 1][i + 1], vy[j + 1][i + 1], vz[j + 1][i + 1]).setColor(cr[j + 1][i + 1], cg[j + 1][i + 1], cb[j + 1][i + 1], ca[j + 1][i + 1]);
                bb.addVertex(matrix, vx[j + 1][i], vy[j + 1][i], vz[j + 1][i]).setColor(cr[j + 1][i], cg[j + 1][i], cb[j + 1][i], ca[j + 1][i]);
            }
        }
    }

    private static Vector3f samplePaletteColor(AuroraManager.Palette palette, float hFrac) {
        if (hFrac < 0.45F) {
            float t = hFrac / 0.45F;
            return new Vector3f(palette.lowColor).lerp(palette.midColor, t);
        }
        float t = (hFrac - 0.45F) / 0.55F;
        return new Vector3f(palette.midColor).lerp(palette.highColor, t);
    }
}
