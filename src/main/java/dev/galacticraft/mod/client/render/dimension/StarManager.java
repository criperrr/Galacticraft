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

package dev.galacticraft.mod.client.render.dimension;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Manages rendering of the iconic Galacticraft celestial star field.
 * Restored from the authoritative Galacticraft 4 legacy algorithm (micdoodle8) with seed 10842L,
 * realistic astronomical depth spread, and accurate celestial coordinates.
 */
public class StarManager {
    private final VertexBuffer starBuffer;

    public StarManager() {
        this.starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        final Random random = new Random(10842L);
        final BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        for (int i = 0; i < 20000; ++i) {
            double j = random.nextFloat() * 2.0F - 1.0F;
            double k = random.nextFloat() * 2.0F - 1.0F;
            double l = random.nextFloat() * 2.0F - 1.0F;
            double m = 0.15F + random.nextFloat() * 0.1F;
            double n = j * j + k * k + l * l;

            if (n < 1.0D && n > 0.01D) {
                n = 1.0D / Math.sqrt(n);
                j *= n;
                k *= n;
                l *= n;
                double dist = random.nextDouble() * 100.0D + 150.0D;
                double o = j * dist;
                double p = k * dist;
                double q = l * dist;
                double r = Math.atan2(j, l);
                double s = Math.sin(r);
                double t = Math.cos(r);
                double u = Math.atan2(Math.sqrt(j * j + l * l), k);
                double v = Math.sin(u);
                double w = Math.cos(u);
                double x = random.nextDouble() * Math.PI * 2.0D;
                double y = Math.sin(x);
                double z = Math.cos(x);

                for (int a = 0; a < 4; ++a) {
                    double b = 0.0D;
                    double c = ((a & 2) - 1) * m;
                    double d = ((a + 1 & 2) - 1) * m;
                    double e = c * z - d * y;
                    double f = d * z + c * y;
                    double g = e * v + b * w;
                    double h = b * v - e * w;
                    double aa = h * s - f * t;
                    double ab = f * s + h * t;
                    buffer.addVertex((float) (o + aa), (float) (p + g), (float) (q + ab));
                }
            }
        }
        this.starBuffer.bind();
        this.starBuffer.upload(buffer.buildOrThrow());
        VertexBuffer.unbind();
    }

    public void tick() {
    }

    /**
     * Applies standard Galacticraft legacy celestial sphere rotation for stars.
     * Corresponds to GL11: glRotatef(-90, 0, 1, 0), glRotatef(celestialAngle * 360, 1, 0, 0), glRotatef(-19, 0, 1, 0).
     */
    public static void rotateForCelestialAngle(PoseStack poseStack, float celestialAngle) {
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));
        poseStack.mulPose(Axis.YN.rotationDegrees(19.0F));
    }

    /**
     * Applies retrograde Galacticraft legacy celestial sphere rotation for stars (e.g. Venus).
     */
    public static void rotateForRetrogradeCelestialAngle(PoseStack poseStack, float celestialAngle) {
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XN.rotationDegrees(celestialAngle * 360.0F));
        poseStack.mulPose(Axis.YN.rotationDegrees(19.0F));
    }

    public void render(PoseStack poseStack, Matrix4f projectionMatrix, Level level, float partialTicks, float brightness) {
        if (brightness <= 0.0F) {
            return;
        }
        RenderSystem.setShaderColor(1.0F, 0.98F, 0.95F, brightness);
        FogRenderer.setupNoFog();
        this.starBuffer.bind();
        this.starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
    }

    public void render(PoseStack poseStack, Matrix4f projectionMatrix, Level level, float partialTicks) {
        this.render(poseStack, projectionMatrix, level, partialTicks, getStarBrightness(level, partialTicks));
    }

    public float getStarBrightness(Level world, float delta) {
        final float skyAngle = world.getTimeOfDay(delta);
        float brightness = Mth.clamp(1.0F - (Mth.cos((float) (skyAngle * Math.PI * 2.0D) * 2.0F + 0.25F)), 0.0F, 1.0F);
        return brightness * brightness * 0.5F + 0.3F;
    }
}
