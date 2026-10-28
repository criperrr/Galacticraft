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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

/**
 * Authoritative sky renderer for Mars, faithfully restored with its authentic thin atmosphere:
 * subtle terracotta/reddish horizon atmospheric haze and daylight scattering, radiant orbital Sun
 * with solar corona, brilliant space stars visible 24/7 and overhead, distant Earth, Martian moons
 * (Phobos and Deimos), and Mars planet body rendered underneath the player at high altitudes.
 */
public class MarsSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    public static final MarsSkyRenderer INSTANCE = new MarsSkyRenderer();

    private final StarManager starManager = new StarManager();
    private final float sunSize = 17.5F;

    public MarsSkyRenderer() {
    }

    /**
     * Builds a semi-transparent horizon atmospheric dome where alpha is strongest at the horizon
     * and smoothly fades out towards the zenith, keeping overhead stars visible in the thin Martian atmosphere.
     */
    private static void renderHorizonAtmosphericDome(PoseStack poseStack, Matrix4f projectionMatrix, float r, float g, float b, float horizonAlpha) {
        if (horizonAlpha <= 0.005F) {
            return;
        }

        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        int segments = 32;
        float radius = 256.0F;
        float halfHeight = 128.0F;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * 2 * Math.PI / segments);
            float a2 = (float) ((i + 1) * 2 * Math.PI / segments);
            float x1 = radius * Mth.cos(a1);
            float z1 = radius * Mth.sin(a1);
            float x2 = radius * Mth.cos(a2);
            float z2 = radius * Mth.sin(a2);

            // Upper dome (zenith has alpha = 0, horizon rim has alpha = horizonAlpha)
            buffer.addVertex(matrix, 0.0F, halfHeight, 0.0F).setColor(r, g, b, 0.0F);
            buffer.addVertex(matrix, x1, 0.0F, z1).setColor(r, g, b, horizonAlpha);
            buffer.addVertex(matrix, x2, 0.0F, z2).setColor(r, g, b, horizonAlpha);

            // Lower dome (bottom has alpha = 0, horizon rim has alpha = horizonAlpha)
            buffer.addVertex(matrix, 0.0F, -halfHeight, 0.0F).setColor(r, g, b, 0.0F);
            buffer.addVertex(matrix, x2, 0.0F, z2).setColor(r, g, b, horizonAlpha);
            buffer.addVertex(matrix, x1, 0.0F, z1).setColor(r, g, b, horizonAlpha);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void renderCoronaFan(Matrix4f matrix, float radius, float r, float g, float b, float centerAlpha) {
        float[][] rim = new float[][]{
                {-radius, -radius},
                {0.0F, -radius * 1.5F},
                {radius, -radius},
                {radius * 1.5F, 0.0F},
                {radius, radius},
                {0.0F, radius * 1.5F},
                {-radius, radius},
                {-radius * 1.5F, 0.0F}
        };
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder auraBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 8; i++) {
            int next = (i + 1) % 8;
            auraBuffer.addVertex(matrix, 0.0F, 100.0F, 0.0F).setColor(r, g, b, centerAlpha);
            auraBuffer.addVertex(matrix, rim[i][0], 100.0F, rim[i][1]).setColor(r, g, b, 0.0F);
            auraBuffer.addVertex(matrix, rim[next][0], 100.0F, rim[next][1]).setColor(r, g, b, 0.0F);
        }
        BufferUploader.drawWithShader(auraBuffer.buildOrThrow());
    }

    @Override
    public void render(WorldRenderContext context) {
        ClientLevel level = context.world();
        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack matrices = new PoseStack();
        matrices.mulPose(context.positionMatrix());
        float celestialAngle = level.getTimeOfDay(partialTicks);

        Player player = Minecraft.getInstance().player;
        double playerY = player != null ? (player.getVehicle() != null ? player.getVehicle().getY() : player.getY()) : 64.0D;

        float spaceProgress = Mth.clamp((float) ((playerY - 180.0D) / 270.0D), 0.0F, 1.0F);
        float spaceAlpha = 1.0F - spaceProgress;

        float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
        float f_day = 1.0F - starBrightness;

        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        // 1. Semi-transparent horizon atmospheric dome (Terracotta haze & daylight scattering)
        if (spaceAlpha > 0.0F && f_day > 0.0F) {
            context.profiler().push("atmosphere");
            float horizonAlpha = 0.32F * f_day * spaceAlpha;
            float rSky = 154.0F / 255.0F;
            float gSky = 114.0F / 255.0F;
            float bSky = 66.0F / 255.0F;
            renderHorizonAtmosphericDome(matrices, context.projectionMatrix(), rSky, gSky, bSky, horizonAlpha);
            context.profiler().pop();
        }

        // 2. Stars (Visible 24/7 like the Moon, with authentic GC4 celestial sphere rotation)
        context.profiler().push("stars");
        matrices.pushPose();
        StarManager.rotateForCelestialAngle(matrices, celestialAngle);

        float renderStarBrightness = starBrightness * starBrightness * 0.5F + 0.3F;
        this.starManager.render(matrices, context.projectionMatrix(), level, partialTicks, renderStarBrightness);

        matrices.popPose();
        context.profiler().pop();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 3. Solar Corona & Radiant Orbital Sun
        context.profiler().push("sun");
        matrices.pushPose();
        matrices.mulPose(Axis.YN.rotationDegrees(90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));

        Matrix4f sunMatrix = matrices.last().pose();

        // Authentic GC4 Mars Solar Corona / Aura
        if (spaceAlpha > 0.0F && f_day > 0.0F) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            float auraAlpha = 0.30F * f_day * spaceAlpha;
            float auraR = 255.0F / 255.0F;
            float auraG = 194.0F / 255.0F;
            float auraB = 180.0F / 255.0F;
            renderCoronaFan(sunMatrix, 20.0F, auraR, auraG, auraB, auraAlpha * 1.5F);
            renderCoronaFan(sunMatrix, 40.0F, auraR, auraG, auraB, auraAlpha);
            RenderSystem.disableBlend();
        }

        // Occulting black quad to prevent stars from shining through
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        float blackSize = this.sunSize / 3.5F;
        BufferBuilder quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        quadBuffer.addVertex(sunMatrix, -blackSize, 99.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 99.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 99.9F, blackSize)
                .addVertex(sunMatrix, -blackSize, 99.9F, blackSize);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        // Radiant orbital sun texture with additive blending (SRC_ALPHA, ONE)
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.Skybox.SUN_ORBITAL);
        quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quadBuffer.addVertex(sunMatrix, -this.sunSize, 100.0F, -this.sunSize).setUv(0.0F, 0.0F)
                .addVertex(sunMatrix, this.sunSize, 100.0F, -this.sunSize).setUv(1.0F, 0.0F)
                .addVertex(sunMatrix, this.sunSize, 100.0F, this.sunSize).setUv(1.0F, 1.0F)
                .addVertex(sunMatrix, -this.sunSize, 100.0F, this.sunSize).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        // 4. Earth in the Martian sky (distant blue marble)
        context.profiler().push("earth");
        matrices.pushPose();
        matrices.scale(0.6F, 0.6F, 0.6F);
        matrices.mulPose(Axis.ZP.rotationDegrees(40.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(200.0F));

        Matrix4f earthMatrix = matrices.last().pose();
        float earthSize = 0.5F;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.CelestialBody.EARTH);
        quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quadBuffer.addVertex(earthMatrix, -earthSize, -100.0F, earthSize).setUv(0.0F, 1.0F)
                .addVertex(earthMatrix, earthSize, -100.0F, earthSize).setUv(1.0F, 1.0F)
                .addVertex(earthMatrix, earthSize, -100.0F, -earthSize).setUv(1.0F, 0.0F)
                .addVertex(earthMatrix, -earthSize, -100.0F, -earthSize).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        // 5. Phobos & Deimos (Martian Moons)
        // Phobos (fast, close moon)
        context.profiler().push("phobos");
        matrices.pushPose();
        matrices.scale(0.8F, 0.8F, 0.8F);
        matrices.mulPose(Axis.ZP.rotationDegrees(celestialAngle * 360.0F + 80.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(40.0F));

        Matrix4f phobosMatrix = matrices.last().pose();
        float phobosSize = 2.5F;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.CelestialBody.PHOBOS);
        quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quadBuffer.addVertex(phobosMatrix, -phobosSize, -100.0F, phobosSize).setUv(0.0F, 1.0F)
                .addVertex(phobosMatrix, phobosSize, -100.0F, phobosSize).setUv(1.0F, 1.0F)
                .addVertex(phobosMatrix, phobosSize, -100.0F, -phobosSize).setUv(1.0F, 0.0F)
                .addVertex(phobosMatrix, -phobosSize, -100.0F, -phobosSize).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        // Deimos (smaller, further moon)
        context.profiler().push("deimos");
        matrices.pushPose();
        matrices.scale(0.8F, 0.8F, 0.8F);
        matrices.mulPose(Axis.XN.rotationDegrees(80.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees(140.0F));

        Matrix4f deimosMatrix = matrices.last().pose();
        float deimosSize = 1.2F;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.CelestialBody.DEIMOS);
        quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quadBuffer.addVertex(deimosMatrix, -deimosSize, -100.0F, deimosSize).setUv(0.0F, 1.0F)
                .addVertex(deimosMatrix, deimosSize, -100.0F, deimosSize).setUv(1.0F, 1.0F)
                .addVertex(deimosMatrix, deimosSize, -100.0F, -deimosSize).setUv(1.0F, 0.0F)
                .addVertex(deimosMatrix, -deimosSize, -100.0F, -deimosSize).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        // 6. Mars Planet Below Player (Pixel art rendered below at high altitudes)
        PlanetUnderPlayerRenderer.renderPlanetBelow(
                matrices,
                level,
                Constant.CelestialBody.MARS,
                Constant.Skybox.ATMOSPHERE,
                playerY,
                partialTicks,
                180.0F,
                1.0F,
                0.85F,
                0.50F,
                0.30F
        );

        RenderSystem.depthMask(true);
    }
}
