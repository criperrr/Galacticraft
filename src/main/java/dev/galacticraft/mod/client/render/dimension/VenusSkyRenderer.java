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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Authoritative sky renderer for Venus, faithfully restored from Galacticraft 4 legacy
 * (micdoodle8). Features a 360-degree atmospheric sky enclosure (no cut-off horizon or void),
 * retrograde celestial movement, rich sulfuric solar corona, distant Earth, and pixel-art
 * Venus planet body rendered underneath the player at high altitudes.
 */
public class VenusSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    public static final VenusSkyRenderer INSTANCE = new VenusSkyRenderer();

    private final StarManager starManager = new StarManager();
    private final VertexBuffer skyBuffer;
    private final float sunSize = 30.0F * (1.0F / 0.75F);

    public VenusSkyRenderer() {
        this.skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.skyBuffer.bind();
        this.skyBuffer.upload(buildAtmosphericEnclosure(Tesselator.getInstance(), 16.0F));
        VertexBuffer.unbind();
    }

    /**
     * Builds a complete 360-degree atmospheric skybox enclosure with inverted faces
     * so that the atmosphere smoothly envelops the observer in all directions.
     */
    public static MeshData buildAtmosphericEnclosure(Tesselator tesselator, float size) {
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        // Top (+Y)
        buffer.addVertex(-size, size, -size)
                .addVertex(-size, size, size)
                .addVertex(size, size, size)
                .addVertex(size, size, -size);
        // Bottom (-Y)
        buffer.addVertex(-size, -size, -size)
                .addVertex(size, -size, -size)
                .addVertex(size, -size, size)
                .addVertex(-size, -size, size);
        // North (-Z)
        buffer.addVertex(-size, -size, -size)
                .addVertex(-size, size, -size)
                .addVertex(size, size, -size)
                .addVertex(size, -size, -size);
        // South (+Z)
        buffer.addVertex(size, -size, size)
                .addVertex(size, size, size)
                .addVertex(-size, size, size)
                .addVertex(-size, -size, size);
        // West (-X)
        buffer.addVertex(-size, -size, size)
                .addVertex(-size, size, size)
                .addVertex(-size, size, -size)
                .addVertex(-size, -size, -size);
        // East (+X)
        buffer.addVertex(size, -size, -size)
                .addVertex(size, size, -size)
                .addVertex(size, size, size)
                .addVertex(size, -size, size);
        return buffer.buildOrThrow();
    }

    @Override
    public void render(WorldRenderContext context) {
        ClientLevel level = context.world();
        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(context.positionMatrix());
        float celestialAngle = level.getTimeOfDay(partialTicks);

        Player player = Minecraft.getInstance().player;
        double playerY = player != null ? player.getY() : 64.0D;

        // As the player ascends past Y=180 into space (up to Y=450), the yellow atmosphere fades away
        float spaceProgress = Mth.clamp((float) ((playerY - 180.0D) / 270.0D), 0.0F, 1.0F);
        float atmosphereAlpha = 1.0F - spaceProgress;

        Vec3 skyColor = level.getSkyColor(context.camera().getPosition(), partialTicks);
        float rSky = (float) skyColor.x;
        float gSky = (float) skyColor.y;
        float bSky = (float) skyColor.z;

        // 1. Full 360-degree Atmospheric Sky Enclosure (NO black void!)
        if (atmosphereAlpha > 0.0F) {
            RenderSystem.depthMask(false);
            FogRenderer.levelFogColor();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 771, 1, 0);
            RenderSystem.setShaderColor(rSky, gSky, bSky, atmosphereAlpha);
            this.skyBuffer.bind();
            this.skyBuffer.drawWithShader(poseStack.last().pose(), context.projectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
        }

        // 2. Stars (Retrograde rotation for Venus - rises in west, sets in east)
        float starBrightness = Math.max(level.getStarBrightness(partialTicks), spaceProgress);
        if (starBrightness > 0.0F) {
            poseStack.pushPose();
            StarManager.rotateForRetrogradeCelestialAngle(poseStack, celestialAngle);
            this.starManager.render(poseStack, context.projectionMatrix(), level, partialTicks, starBrightness);
            poseStack.popPose();
        }

        // 3. Venusian Sun Aura (Smooth sulfuric corona in the atmosphere)
        float auraFactor = (1.0F - level.getStarBrightness(partialTicks)) * atmosphereAlpha;
        if (auraFactor > 0.0F) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.XN.rotationDegrees(celestialAngle * 360.0F));

            float auraR = 255.0F / 255.0F * auraFactor;
            float auraG = 210.0F / 255.0F * auraFactor;
            float auraB = 140.0F / 255.0F * auraFactor;
            float auraAlpha = 0.35F * auraFactor;

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder auraBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f auraMatrix = poseStack.last().pose();

            auraBuffer.addVertex(auraMatrix, 0.0F, 100.0F, 0.0F).setColor(auraR, auraG, auraB, auraAlpha * 2.0F);
            float f10 = 24.0F;
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

            // Outer corona
            auraBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            auraBuffer.addVertex(auraMatrix, 0.0F, 100.0F, 0.0F).setColor(auraR, auraG, auraB, auraAlpha);
            f10 = 48.0F;
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

            poseStack.popPose();
        }

        // 4. Sun Disk (Atmospheric in clouds, Orbital in space)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XN.rotationDegrees(celestialAngle * 360.0F));

        // Occulting black quad
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        float blackSize = this.sunSize / 3.5F;
        Matrix4f sunMatrix = poseStack.last().pose();
        BufferBuilder quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        quadBuffer.addVertex(sunMatrix, -blackSize, 99.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 99.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 99.9F, blackSize)
                .addVertex(sunMatrix, -blackSize, 99.9F, blackSize);
        BufferUploader.drawWithShader(quadBuffer.buildOrThrow());

        // Sun Texture (Atmospheric sun when in atmosphere, Orbital sun when in space)
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
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

        poseStack.popPose();

        // 5. Earth in the Venusian sky (morning / evening star)
        poseStack.pushPose();
        poseStack.scale(0.6F, 0.6F, 0.6F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(40.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));

        Matrix4f earthMatrix = poseStack.last().pose();
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

        poseStack.popPose();

        // 6. Venus Planet Below Player (Pixel art rendered below at high altitudes)
        PlanetUnderPlayerRenderer.renderPlanetBelow(
                poseStack,
                level,
                Constant.CelestialBody.VENUS,
                Constant.Skybox.ATMOSPHERE,
                playerY,
                partialTicks,
                180.0F,
                450.0F,
                70.0F,
                1.0F,
                0.85F,
                0.4F
        );

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
