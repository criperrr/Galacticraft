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
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

// TODO: Allow support for more planets
public class OverworldRenderer extends SpaceSkyRenderer {
    @Nullable
    private VertexBuffer skyBuffer;
    private Minecraft minecraft = Minecraft.getInstance();
    protected final EarthManager earthManager = new EarthManager();

    public OverworldRenderer() {
    }

    private VertexBuffer getSkyBuffer() {
        if (this.skyBuffer == null) {
            this.skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            this.skyBuffer.bind();
            this.skyBuffer.upload(VenusSkyRenderer.buildAtmosphericDome(Tesselator.getInstance(), 256.0F, 128.0F));
            VertexBuffer.unbind();
        }
        return this.skyBuffer;
    }

    public void renderOverworldSky(Player player, Matrix4f matrix4f, Matrix4f projectionMatrix, float partialTicks, Camera camera, boolean bl, Runnable runnable) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(matrix4f);
        Tesselator tesselator = Tesselator.getInstance();

        final Vec3 skyColor = this.getFogColor(this.minecraft.level, partialTicks, camera.getPosition());
        float x = (float) skyColor.x;
        float y = (float) skyColor.y;
        float z = (float) skyColor.z;

        double playerHeight = player.getVehicle() != null ? player.getVehicle().getY() : player.getY();
        float rawProgress = Mth.clamp(
                (float) ((playerHeight - Constant.ATMOSPHERE_FADE_START) / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FADE_START)),
                0.0F,
                1.0F
        );
        float fade = rawProgress * rawProgress * (3.0F - 2.0F * rawProgress);
        float dayFactor = Mth.clamp(Mth.cos(this.minecraft.level.getTimeOfDay(partialTicks) * Mth.TWO_PI) * 2.0F + 0.5F, 0.0F, 1.0F);
        float timeOpacity = Mth.lerp(dayFactor, 0.25F, 1.0F);
        float atmosphereAlpha = (1.0F - fade) * timeOpacity;

        FogRenderer.levelFogColor();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // Darken sky background smoothly to pure space black from Y=500 -> Y=780
        VertexBuffer vb = getSkyBuffer();
        vb.bind();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        vb.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());

        if (atmosphereAlpha > 0.005F) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderColor(x, y, z, atmosphereAlpha);
            vb.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
        }
        VertexBuffer.unbind();
        RenderSystem.enableCull();
        RenderSystem.enableBlend();

        ClientLevel level = (ClientLevel) player.level();
        float celestialAngle = level.getTimeOfDay(partialTicks);

        if (AstroSkyEngine.isInitialized()) {
            float starVis = Math.max(level.getStarBrightness(partialTicks), fade);
            AstroSkyRenderer.renderSpaceDimensionSky(poseStack, projectionMatrix, starVis, true);
        } else {
            poseStack.pushPose();
            StarManager.rotateForCelestialAngle(poseStack, celestialAngle);
            float starBrightness = Math.max(level.getStarBrightness(partialTicks), fade);
            this.starManager.render(poseStack, projectionMatrix, level, partialTicks, starBrightness);
            poseStack.popPose();
        }

        // 3. Earth visible below rocket or ascending player
        double pY = player.getVehicle() != null ? player.getVehicle().getY() : player.getY();
        PlanetUnderPlayerRenderer.renderPlanetBelow(
                poseStack,
                level,
                Constant.CelestialBody.EARTH,
                Constant.Skybox.ATMOSPHERE,
                pY,
                partialTicks,
                Constant.ATMOSPHERE_EXIT_START,
                1.0F,
                0.4F,
                0.7F,
                1.0F
        );

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static Vec3 getFogColor(ClientLevel level, float partialTicks, Vec3 cameraPos) {
        float progress = Mth.clamp(
                (float) ((cameraPos.y() - Constant.ATMOSPHERE_FADE_START) / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FADE_START)),
                0.0F,
                1.0F
        );
        float atmosphereRemaining = 1.0F - progress * progress * (3.0F - 2.0F * progress);
        Vec3 skyColor = level.getSkyColor(cameraPos, partialTicks);
        return skyColor.scale(atmosphereRemaining);
    }
}
