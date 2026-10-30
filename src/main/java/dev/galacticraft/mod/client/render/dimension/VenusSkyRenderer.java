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
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

/**
 * Authoritative sky renderer for Venus, faithfully restored from Galacticraft 4 legacy
 * (micdoodle8) with its dense sulfuric atmosphere:
 * 360-degree atmospheric sky enclosure below Y=200, rich sulfuric solar corona, distant Earth,
 * retrograde celestial movement, smooth Geoffrey Landis cloud layer transition to space,
 * and Venus planet body rendered underneath the player at high altitudes.
 */
public class VenusSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    public static final VenusSkyRenderer INSTANCE = new VenusSkyRenderer();

    private final StarManager starManager = new StarManager();
    private VertexBuffer skyBuffer = null;
    private final float sunSize = 30.0F * (1.0F / 0.75F);

    public VenusSkyRenderer() {
    }

    private VertexBuffer getSkyBuffer() {
        if (this.skyBuffer == null) {
            this.skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            this.skyBuffer.bind();
            this.skyBuffer.upload(buildAtmosphericDome(Tesselator.getInstance(), 256.0F, 128.0F));
            VertexBuffer.unbind();
        }
        return this.skyBuffer;
    }

    /**
     * Builds a complete, seamless 360-degree atmospheric enclosure (dome + cylinder + base)
     * so that the planetary atmosphere smoothly envelops the observer in all directions with NO black void.
     */
    public static MeshData buildAtmosphericDome(Tesselator tesselator, float radius, float halfHeight) {
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        int segments = 32;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * 2 * Math.PI / segments);
            float a2 = (float) ((i + 1) * 2 * Math.PI / segments);
            float x1 = radius * Mth.cos(a1);
            float z1 = radius * Mth.sin(a1);
            float x2 = radius * Mth.cos(a2);
            float z2 = radius * Mth.sin(a2);

            // Top disc (facing inward/downward into camera)
            buffer.addVertex(0.0F, halfHeight, 0.0F);
            buffer.addVertex(x1, halfHeight, z1);
            buffer.addVertex(x2, halfHeight, z2);

            // Bottom disc (facing inward/upward into camera)
            buffer.addVertex(0.0F, -halfHeight, 0.0F);
            buffer.addVertex(x2, -halfHeight, z2);
            buffer.addVertex(x1, -halfHeight, z1);

            // Cylinder wall quad (2 triangles)
            buffer.addVertex(x1, halfHeight, z1);
            buffer.addVertex(x1, -halfHeight, z1);
            buffer.addVertex(x2, -halfHeight, z2);

            buffer.addVertex(x1, halfHeight, z1);
            buffer.addVertex(x2, -halfHeight, z2);
            buffer.addVertex(x2, halfHeight, z2);
        }
        return buffer.buildOrThrow();
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
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(context.positionMatrix());
        float celestialAngle = level.getTimeOfDay(partialTicks);

        Player player = Minecraft.getInstance().player;
        double playerY = player != null ? (player.getVehicle() != null ? player.getVehicle().getY() : player.getY()) : 64.0D;

        // Geoffrey Landis cloud layer transition: as player ascends past Y=200 into space (up to Y=450),
        // the golden sulfurous atmosphere fades smoothly into space
        float spaceProgress = Mth.clamp((float) ((playerY - 200.0D) / 250.0D), 0.0F, 1.0F);
        float atmosphereAlpha = 1.0F - spaceProgress;

        // Authentic GC4 WorldProviderVenus celestial color model:
        // Day: [255, 207, 81] (rich golden sulfurous yellow), Night: [118, 89, 21] (warm glowing amber)
        float dayFactor = Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 2.0F + 0.5F, 0.0F, 1.0F);
        float nightFactor = 1.0F - dayFactor;

        float rSky = (255.0F / 255.0F) * dayFactor + (118.0F / 255.0F) * nightFactor;
        float gSky = (207.0F / 255.0F) * dayFactor + (89.0F / 255.0F) * nightFactor;
        float bSky = (81.0F / 255.0F) * dayFactor + (21.0F / 255.0F) * nightFactor;

        // 1. Full 360-degree Atmospheric Sky Enclosure (Envelops player below cloud top)
        if (atmosphereAlpha > 0.0F) {
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            FogRenderer.levelFogColor();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.setShaderColor(rSky, gSky, bSky, atmosphereAlpha);
            VertexBuffer buffer = getSkyBuffer();
            buffer.bind();
            buffer.drawWithShader(poseStack.last().pose(), context.projectionMatrix(), GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        // 2. Stars (Retrograde celestial rotation - rise in west, set in east)
        // Obscured on the ground during day by dense clouds, emerging brilliantly at night or in space
        float starAlpha = Math.max(spaceProgress, nightFactor * 0.75F);
        if (starAlpha > 0.01F) {
            context.profiler().push("stars");
            poseStack.pushPose();
            StarManager.rotateForRetrogradeCelestialAngle(poseStack, celestialAngle);

            float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
            starBrightness = (starBrightness * starBrightness * 0.5F + 0.3F) * starAlpha;
            this.starManager.render(poseStack, context.projectionMatrix(), level, partialTicks, starBrightness);

            poseStack.popPose();
            context.profiler().pop();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 3. Solar Corona / Aura around the Sun (Rich golden sulfurous aura in atmosphere)
        if (atmosphereAlpha > 0.0F && dayFactor > 0.0F) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-celestialAngle * 360.0F));

            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );

            Matrix4f auraMatrix = poseStack.last().pose();
            float auraAlpha = 0.35F * dayFactor * atmosphereAlpha;
            float auraR = 1.0F;
            float auraG = 0.85F;
            float auraB = 0.35F;

            renderCoronaFan(auraMatrix, 24.0F, auraR, auraG, auraB, auraAlpha * 2.0F);
            renderCoronaFan(auraMatrix, 48.0F, auraR, auraG, auraB, auraAlpha);

            RenderSystem.disableBlend();
            poseStack.popPose();
        }

        // 4. Sun Disk (Atmospheric diffuse sun in clouds, Radiant orbital sun in space)
        context.profiler().push("sun");
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-celestialAngle * 360.0F));

        Matrix4f sunMatrix = poseStack.last().pose();

        // Occulting black quad
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

        // Sun Texture (Atmospheric sun in clouds, Orbital sun in space)
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        ResourceLocation sunTex = spaceProgress > 0.5F ? Constant.Skybox.SUN_ORBITAL : Constant.Skybox.SUN_ATMOSPHERIC;
        RenderSystem.setShaderTexture(0, sunTex);
        quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quadBuffer.addVertex(sunMatrix, -this.sunSize, 100.0F, -this.sunSize).setUv(0.0F, 0.0F)
                .addVertex(sunMatrix, this.sunSize, 100.0F, -this.sunSize).setUv(1.0F, 0.0F)
                .addVertex(sunMatrix, this.sunSize, 100.0F, this.sunSize).setUv(1.0F, 1.0F)
                .addVertex(sunMatrix, -this.sunSize, 100.0F, this.sunSize).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        RenderSystem.disableBlend();
        poseStack.popPose();
        context.profiler().pop();

        // 5. Earth in the Venusian sky (morning / evening star)
        context.profiler().push("earth");
        poseStack.pushPose();
        poseStack.scale(0.6F, 0.6F, 0.6F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(40.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));

        Matrix4f earthMatrix = poseStack.last().pose();
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
        poseStack.popPose();
        context.profiler().pop();

        // 6. Venus Planet Below Player (Pixel art rendered below with glowing sulfuric atmosphere at high altitudes)
        PlanetUnderPlayerRenderer.renderPlanetBelow(
                poseStack,
                level,
                Constant.CelestialBody.VENUS,
                Constant.Skybox.ATMOSPHERE,
                playerY,
                partialTicks,
                180.0F,
                1.0F,
                1.0F,
                0.85F,
                0.35F
        );

        RenderSystem.depthMask(true);
    }
}
