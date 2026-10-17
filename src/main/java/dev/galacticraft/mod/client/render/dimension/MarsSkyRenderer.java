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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.galacticraft.mod.Constant;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Authoritative sky renderer for Mars, faithfully restored from Galacticraft 4 legacy
 * (micdoodle8) with atmospheric sun aura, occulting solar disk, distant Earth/Moon,
 * and Martian moons (Phobos and Deimos).
 */
public class MarsSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    public static final MarsSkyRenderer INSTANCE = new MarsSkyRenderer();

    private final StarManager starManager = new StarManager();
    private final VertexBuffer skyBuffer;
    private final float sunSize = 17.5F;

    public MarsSkyRenderer() {
        this.skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        final byte byte2 = 64;
        final int i = 256 / byte2 + 2;
        float f = 16.0F;
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for (int j = -byte2 * i; j <= byte2 * i; j += byte2) {
            for (int l = -byte2 * i; l <= byte2 * i; l += byte2) {
                buffer.addVertex(j, f, l)
                        .addVertex(j + byte2, f, l)
                        .addVertex(j + byte2, f, l + byte2)
                        .addVertex(j, f, l + byte2);
            }
        }
        this.skyBuffer.bind();
        this.skyBuffer.upload(buffer.buildOrThrow());
        VertexBuffer.unbind();
    }

    @Override
    public void render(WorldRenderContext context) {
        ClientLevel level = context.world();
        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack matrices = new PoseStack();
        matrices.mulPose(context.positionMatrix());
        float celestialAngle = level.getTimeOfDay(partialTicks);

        Vec3 skyColor = level.getSkyColor(context.camera().getPosition(), partialTicks);
        float rSky = (float) skyColor.x;
        float gSky = (float) skyColor.y;
        float bSky = (float) skyColor.z;

        // 1. Atmospheric sky dome
        RenderSystem.depthMask(false);
        FogRenderer.levelFogColor();
        RenderSystem.setShaderColor(rSky, gSky, bSky, 1.0F);
        this.skyBuffer.bind();
        this.skyBuffer.drawWithShader(matrices.last().pose(), context.projectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);

        // 2. Stars (fading out in daytime, bright at night)
        float starBrightness = level.getStarBrightness(partialTicks);
        if (starBrightness > 0.0F) {
            matrices.pushPose();
            StarManager.rotateForCelestialAngle(matrices, celestialAngle);
            this.starManager.render(matrices, context.projectionMatrix(), level, partialTicks, starBrightness);
            matrices.popPose();
        }

        // 3. Martian Sun Aura (2-tier smooth colored triangle fan corona)
        float sunAuraFactor = 1.0F - starBrightness;
        if (sunAuraFactor > 0.0F) {
            matrices.pushPose();
            matrices.mulPose(Axis.YN.rotationDegrees(90.0F));
            matrices.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));

            float auraR = 255.0F / 255.0F * sunAuraFactor;
            float auraG = 194.0F / 255.0F * sunAuraFactor;
            float auraB = 180.0F / 255.0F * sunAuraFactor;
            float auraAlpha = 0.3F * sunAuraFactor;

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder auraBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f auraMatrix = matrices.last().pose();

            // Inner aura (radius 20)
            auraBuffer.addVertex(auraMatrix, 0.0F, 100.0F, 0.0F).setColor(auraR, auraG, auraB, auraAlpha * 2.0F);
            float f10 = 20.0F;
            auraBuffer.addVertex(auraMatrix, -f10, 100.0F, -f10).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, 0.0F, 100.0F, -f10 * 1.5F).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, f10, 100.0F, -f10).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, f10 * 1.5F, 100.0F, 0.0F).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, f10, 100.0F, f10).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, 0.0F, 100.0F, f10 * 1.5F).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, -f10, 100.0F, f10).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, -f10 * 1.5F, 100.0F, 0.0F).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, -f10, 100.0F, -f10).setColor(auraR, auraG, auraB, 0.0F);
            BufferUploader.drawWithShader(auraBuffer.buildOrThrow());

            // Outer aura (radius 40)
            auraBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            auraBuffer.addVertex(auraMatrix, 0.0F, 100.0F, 0.0F).setColor(auraR, auraG, auraB, auraAlpha);
            f10 = 40.0F;
            auraBuffer.addVertex(auraMatrix, -f10, 100.0F, -f10).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, 0.0F, 100.0F, -f10 * 1.5F).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, f10, 100.0F, -f10).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, f10 * 1.5F, 100.0F, 0.0F).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, f10, 100.0F, f10).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, 0.0F, 100.0F, f10 * 1.5F).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, -f10, 100.0F, f10).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, -f10 * 1.5F, 100.0F, 0.0F).setColor(auraR, auraG, auraB, 0.0F)
                    .addVertex(auraMatrix, -f10, 100.0F, -f10).setColor(auraR, auraG, auraB, 0.0F);
            BufferUploader.drawWithShader(auraBuffer.buildOrThrow());

            matrices.popPose();
        }

        // 4. Sun Disk (with occulting black square and atmospheric sun)
        matrices.pushPose();
        matrices.mulPose(Axis.YN.rotationDegrees(90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));

        // Occulting black quad
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        float blackSize = this.sunSize / 3.5F;
        Matrix4f sunMatrix = matrices.last().pose();
        BufferBuilder quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        quadBuffer.addVertex(sunMatrix, -blackSize, 99.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 99.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 99.9F, blackSize)
                .addVertex(sunMatrix, -blackSize, 99.9F, blackSize);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        // Atmospheric sun texture
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.Skybox.SUN_ATMOSPHERIC);
        quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quadBuffer.addVertex(sunMatrix, -this.sunSize, 100.0F, -this.sunSize).setUv(0.0F, 0.0F)
                .addVertex(sunMatrix, this.sunSize, 100.0F, -this.sunSize).setUv(1.0F, 0.0F)
                .addVertex(sunMatrix, this.sunSize, 100.0F, this.sunSize).setUv(1.0F, 1.0F)
                .addVertex(sunMatrix, -this.sunSize, 100.0F, this.sunSize).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        matrices.popPose();

        // 5. Earth in the Martian sky (distant blue marble)
        matrices.pushPose();
        matrices.scale(0.6F, 0.6F, 0.6F);
        matrices.mulPose(Axis.ZP.rotationDegrees(40.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(200.0F));

        Matrix4f earthMatrix = matrices.last().pose();
        float earthSize = 0.5F;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, Constant.CelestialBody.EARTH);
        quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quadBuffer.addVertex(earthMatrix, -earthSize, -100.0F, earthSize).setUv(0.0F, 1.0F)
                .addVertex(earthMatrix, earthSize, -100.0F, earthSize).setUv(1.0F, 1.0F)
                .addVertex(earthMatrix, earthSize, -100.0F, -earthSize).setUv(1.0F, 0.0F)
                .addVertex(earthMatrix, -earthSize, -100.0F, -earthSize).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        matrices.popPose();

        // 6. Phobos & Deimos (Martian Moons)
        // Phobos (fast, close moon)
        matrices.pushPose();
        matrices.scale(0.8F, 0.8F, 0.8F);
        matrices.mulPose(Axis.ZP.rotationDegrees(celestialAngle * 360.0F + 80.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(40.0F));

        Matrix4f phobosMatrix = matrices.last().pose();
        float phobosSize = 2.5F;
        RenderSystem.setShaderTexture(0, Constant.CelestialBody.PHOBOS);
        quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quadBuffer.addVertex(phobosMatrix, -phobosSize, -100.0F, phobosSize).setUv(0.0F, 1.0F)
                .addVertex(phobosMatrix, phobosSize, -100.0F, phobosSize).setUv(1.0F, 1.0F)
                .addVertex(phobosMatrix, phobosSize, -100.0F, -phobosSize).setUv(1.0F, 0.0F)
                .addVertex(phobosMatrix, -phobosSize, -100.0F, -phobosSize).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        matrices.popPose();

        // Deimos (smaller, further moon)
        matrices.pushPose();
        matrices.scale(0.8F, 0.8F, 0.8F);
        matrices.mulPose(Axis.XN.rotationDegrees(80.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees(140.0F));

        Matrix4f deimosMatrix = matrices.last().pose();
        float deimosSize = 1.2F;
        RenderSystem.setShaderTexture(0, Constant.CelestialBody.DEIMOS);
        quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quadBuffer.addVertex(deimosMatrix, -deimosSize, -100.0F, deimosSize).setUv(0.0F, 1.0F)
                .addVertex(deimosMatrix, deimosSize, -100.0F, deimosSize).setUv(1.0F, 1.0F)
                .addVertex(deimosMatrix, deimosSize, -100.0F, -deimosSize).setUv(1.0F, 0.0F)
                .addVertex(deimosMatrix, -deimosSize, -100.0F, -deimosSize).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        matrices.popPose();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }
}
