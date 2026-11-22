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
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroSkyRenderer;
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
    private VertexBuffer blackSkyBuffer = null;

    private VertexBuffer getBlackSkyBuffer() {
        if (this.blackSkyBuffer == null) {
            this.blackSkyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            this.blackSkyBuffer.bind();
            this.blackSkyBuffer.upload(VenusSkyRenderer.buildAtmosphericDome(Tesselator.getInstance(), 256.0F, 128.0F));
            VertexBuffer.unbind();
        }
        return this.blackSkyBuffer;
    }

    public MarsSkyRenderer() {
    }

    /**
     * Builds a rich Martian atmospheric dome with warm butterscotch-terracotta dust scattering
     * strongest near the horizon and slightly deeper dusty ochre overhead, smoothly fading to
     * pure space black as the player ascends past Y=500 -> Y=780.
     */
    private static void renderMartianAtmosphericDome(
            PoseStack poseStack,
            float hr, float hg, float hb, float horizonAlpha,
            float zr, float zg, float zb, float zenithAlpha
    ) {
        if (horizonAlpha <= 0.005F && zenithAlpha <= 0.005F) {
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

            // Upper dome (dusty ochre zenith -> bright butterscotch horizon)
            buffer.addVertex(matrix, 0.0F, halfHeight, 0.0F).setColor(zr, zg, zb, zenithAlpha);
            buffer.addVertex(matrix, x1, 0.0F, z1).setColor(hr, hg, hb, horizonAlpha);
            buffer.addVertex(matrix, x2, 0.0F, z2).setColor(hr, hg, hb, horizonAlpha);

            // Lower dome (mirrors dusty haze below horizon)
            buffer.addVertex(matrix, 0.0F, -halfHeight, 0.0F).setColor(zr, zg, zb, zenithAlpha);
            buffer.addVertex(matrix, x2, 0.0F, z2).setColor(hr, hg, hb, horizonAlpha);
            buffer.addVertex(matrix, x1, 0.0F, z1).setColor(hr, hg, hb, horizonAlpha);
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

        float rawProgress = Mth.clamp(
                (float) ((playerY - Constant.ATMOSPHERE_FADE_START) / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FADE_START)),
                0.0F,
                1.0F
        );
        float spaceProgress = rawProgress * rawProgress * (3.0F - 2.0F * rawProgress);
        float spaceAlpha = 1.0F - spaceProgress;

        float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
        float f_day = 1.0F - starBrightness;

        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        // 0. Pure-black deep space sky dome base (guarantees pitch-black space at Y >= 700 even during daytime)
        net.minecraft.client.renderer.FogRenderer.setupNoFog();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        VertexBuffer blackBuffer = getBlackSkyBuffer();
        blackBuffer.bind();
        blackBuffer.drawWithShader(matrices.last().pose(), context.projectionMatrix(), GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        RenderSystem.enableCull();
        // Reset shader color modulator before drawing vertex-colored atmospheric dome!
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 1. Martian atmospheric dome driven by both time of day and altitude (Y=300 -> Y=700 -> pure black)
        if (spaceAlpha > 0.005F) {
            context.profiler().push("atmosphere");
            // Stronger warm butterscotch-terracotta at noon, weakening smoothly to a faint dusty horizon veil at night
            float horizonAlpha = Mth.lerp(f_day, 0.08F, 0.94F) * spaceAlpha;
            float zenithAlpha = (0.78F * f_day) * spaceAlpha;
            float hr = Mth.lerp(f_day, 92.0F / 255.0F, 204.0F / 255.0F);
            float hg = Mth.lerp(f_day, 52.0F / 255.0F, 132.0F / 255.0F);
            float hb = Mth.lerp(f_day, 38.0F / 255.0F, 84.0F / 255.0F);
            float zr = Mth.lerp(f_day, 42.0F / 255.0F, 148.0F / 255.0F);
            float zg = Mth.lerp(f_day, 24.0F / 255.0F, 88.0F / 255.0F);
            float zb = Mth.lerp(f_day, 22.0F / 255.0F, 56.0F / 255.0F);
            renderMartianAtmosphericDome(matrices, hr, hg, hb, horizonAlpha, zr, zg, zb, zenithAlpha);
            context.profiler().pop();
        }

        // 2-5. Realistic AstroCraft Martian sky (Phobos, Deimos, Earth, Sun, Milky Way, stars & meteors)
        // Daytime dust washes out faint stars on the surface; stars emerge brilliantly at night or in space (Y=500+)
        float marsStarVis = Math.max(spaceProgress, Mth.lerp(f_day, 1.0F, 0.14F));
        if (AstroSkyEngine.isInitialized()) {
            context.profiler().push("astro_mars_sky");
            AstroSkyRenderer.renderSpaceDimensionSky(matrices, context.projectionMatrix(), marsStarVis, true);
            context.profiler().pop();
        } else {
            context.profiler().push("stars");
            matrices.pushPose();
            StarManager.rotateForCelestialAngle(matrices, celestialAngle);
            float renderStarBrightness = starBrightness * starBrightness * 0.5F + 0.3F;
            this.starManager.render(matrices, context.projectionMatrix(), level, partialTicks, renderStarBrightness);
            matrices.popPose();
            context.profiler().pop();
        }

        // 6. Mars Planet Below Player (Pixel art rendered below at high altitudes)
        PlanetUnderPlayerRenderer.renderPlanetBelow(
                matrices,
                level,
                Constant.CelestialBody.MARS,
                Constant.Skybox.ATMOSPHERE,
                playerY,
                partialTicks,
                Constant.ATMOSPHERE_EXIT_START,
                1.0F,
                0.85F,
                0.50F,
                0.30F
        );

        RenderSystem.depthMask(true);
    }
}
