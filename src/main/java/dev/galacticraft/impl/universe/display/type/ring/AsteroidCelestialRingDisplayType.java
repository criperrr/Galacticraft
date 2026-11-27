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

package dev.galacticraft.impl.universe.display.type.ring;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.api.universe.display.ring.CelestialRingDisplayType;
import dev.galacticraft.impl.universe.display.config.ring.DefaultCelestialRingDisplayConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.gui.screen.ingame.CelestialSelectionScreen;
import dev.galacticraft.mod.client.util.Graphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class AsteroidCelestialRingDisplayType extends CelestialRingDisplayType<DefaultCelestialRingDisplayConfig> {
    public static final AsteroidCelestialRingDisplayType INSTANCE = new AsteroidCelestialRingDisplayType(DefaultCelestialRingDisplayConfig.CODEC);

    public AsteroidCelestialRingDisplayType(Codec<DefaultCelestialRingDisplayConfig> codec) {
        super(codec);
    }

    @Override
    public boolean render(CelestialBody<?, ?> body, GuiGraphics graphics, int count, Vector3f systemOffset, float lineScale, float alpha, double mouseX, double mouseY, float delta, DefaultCelestialRingDisplayConfig config) {
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        PoseStack matrices = graphics.pose();
        matrices.pushPose();
        Screen screen = Minecraft.getInstance().screen;
        float[] color = screen instanceof CelestialSelectionScreen ? new float[]{0.7F, 0.0F, 0.0F, alpha / 2.0F} : new float[]{0.3F, 0.1F, 0.1F, 1.0F};
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        final float theta = Mth.TWO_PI / 90;
        final float cos = Mth.cos(theta);
        final float sin = Mth.sin(theta);
        final float theta2 = Mth.TWO_PI / -90f;
        final float cos2 = Mth.cos(theta2);
        final float sin2 = Mth.sin(theta2);

        float min = 72.0F;
        float max = 78.0F;

        float x = max * body.position().lineScale();
        float y = 0;

        float temp;
        float x1 = x;
        float y1 = y;
        Matrix4f model = matrices.last().pose();
        for (int i = 0; i < 180; i++) {
            buffer.addVertex(model, x, y, 0).setColor(color[0], color[1], color[2], color[3]);
            if (i < 90) {
                buffer.setNormal(1, 1, 1);
            } else {
                buffer.setNormal(1, -1, -1);
            }

            temp = x;
            x = cos * x - sin * y;
            y = sin * temp + cos * y;
        }

        buffer.addVertex(model, x1, y1, 0).setColor(color[0], color[1], color[2], color[3]).setNormal(1, 1, 1); //LINE_LOOP is gone
        x = x1;
        y = y1;

        for (int i = 0; i < 180; i++) {
            buffer.addVertex(model, x, y, 0).setColor(color[0], color[1], color[2], color[3]);
            if (i < 90) {
                buffer.setNormal(1, 1, 1);
            } else {
                buffer.setNormal(1, -1, -1);
            }

            temp = x;
            x = cos2 * x - sin2 * y;
            y = sin2 * temp + cos2 * y;
        }
        buffer.addVertex(model, x1, y1, 0).setColor(color[0], color[1], color[2], color[3]).setNormal(1, 1, 1); //LINE_LOOP is gone

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        x = min * body.position().lineScale();
        y = 0;

        x1 = x;
        y1 = y;

        for (int i = 0; i < 180; i++) {
            buffer.addVertex(model, x, y, 0).setColor(color[0], color[1], color[2], color[3]);
            if (i < 90) {
                buffer.setNormal(1, 1, 1);
            } else {
                buffer.setNormal(1, -1, -1);
            }

            temp = x;
            x = cos * x - sin * y;
            y = sin * temp + cos * y;
        }

        buffer.addVertex(model, x1, y1, 0).setColor(color[0], color[1], color[2], color[3]).setNormal(1, 1, 1); //LINE_LOOP is gone
        x = x1;
        y = y1;

        for (int i = 0; i < 180; i++) {
            buffer.addVertex(model, x, y, 0).setColor(color[0], color[1], color[2], color[3]);
            if (i < 90) {
                buffer.setNormal(1, 1, 1);
            } else {
                buffer.setNormal(1, -1, -1);
            }

            temp = x;
            x = cos2 * x - sin2 * y;
            y = sin2 * temp + cos2 * y;
        }
        buffer.addVertex(model, x1, y1, 0).setColor(color[0], color[1], color[2], color[3]).setNormal(1, 1, 1); //LINE_LOOP is gone

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        x = min * body.position().lineScale();
        y = 0;
        float x2 = max * body.position().lineScale();
        float y2 = 0;

        for (int i = 0; i < 90; i++) {
            buffer.addVertex(model, x2, y2, 0).setColor(0.7F, 0.0F, 0.0F, alpha / 10.0F);
            buffer.addVertex(model, x, y, 0).setColor(0.7F, 0.0F, 0.0F, alpha / 10.0F);

            temp = x;
            x = cos * x - sin * y;
            y = sin * temp + cos * y;
            temp = x2;
            x2 = cos * x2 - sin * y2;
            y2 = sin * temp + cos * y2;

            buffer.addVertex(model, x, y, 0).setColor(0.7F, 0.0F, 0.0F, alpha / 10.0F);
            buffer.addVertex(model, x2, y2, 0).setColor(0.7F, 0.0F, 0.0F, alpha / 10.0F);
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // Animated mini pixel-art asteroids orbiting along the belt
        if (alpha > 0.05F) {
            long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
            float time = (gameTime + delta) * 0.0022F;
            int asteroidCount = 28;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * 0.85F);
            for (int i = 0; i < asteroidCount; i++) {
                float hash = Mth.sin(i * 12.9898F) * 43758.5453F;
                float frac = hash - Mth.floor(hash);
                float hash2 = Mth.sin((i + 17) * 78.233F) * 24634.6345F;
                float frac2 = hash2 - Mth.floor(hash2);

                float speed = 0.7F + frac * 0.6F;
                float angle = (i / (float) asteroidCount) * Mth.TWO_PI + time * speed + frac2 * 0.25F;
                float radius = Mth.lerp(frac, min + 0.8F, max - 0.8F) * body.position().lineScale();
                float ax = Mth.sin(angle) * radius;
                float ay = Mth.cos(angle) * radius;
                float size = 1.6F + frac2 * 1.4F;
                float spin = ((gameTime + delta) * (0.8F + frac * 1.4F) + i * 37.0F) % 360.0F;

                matrices.pushPose();
                matrices.translate(ax, ay, 0.0F);
                matrices.mulPose(Axis.ZP.rotationDegrees(45.0F));
                matrices.mulPose(Axis.XN.rotationDegrees(55.0F));
                matrices.mulPose(Axis.ZP.rotationDegrees(spin));
                Graphics.blitCentered(matrices.last().pose(), 0.0F, 0.0F, size, size, 0.0F, 0, 0, 16, 16, 16, 16, Constant.CelestialBody.ASTEROID);
                matrices.popPose();
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        matrices.popPose();
        return false;
    }
}
