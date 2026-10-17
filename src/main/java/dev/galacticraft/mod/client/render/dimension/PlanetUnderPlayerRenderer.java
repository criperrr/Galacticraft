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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Renders the iconic pixel-art planetary celestial body directly underneath the player
 * when ascending to high altitudes (e.g. rocket ascent or high-altitude flight),
 * faithfully restored from Galacticraft 4 Legacy (micdoodle8) and its classic addons (GalaxySpace/AsmodeusCore).
 *
 * Mathematical model from micdoodle8 (SkyProviderOverworld.java):
 *  - theta = sqrt((playerY - startHeight) / 1000.0) * 400.0
 *  - alpha = clamp((theta / 100.0 - 0.2) * 2.0, 0.0, 1.0)
 *  - scale = 850.0 * (0.25 - theta / 10000.0)
 *  - translate(0.0, -playerY, 0.0) puts the horizontal XZ planar quad at Y = 0 (planetary sea level),
 *    causing the planet to naturally shrink into the distance as altitude increases.
 */
public class PlanetUnderPlayerRenderer {

    public static void renderPlanetBelow(
            PoseStack poseStack,
            Level level,
            ResourceLocation planetTexture,
            @Nullable ResourceLocation atmosphereTexture,
            double playerY,
            float partialTicks,
            float startHeight,
            float baseScaleMultiplier,
            float haloR,
            float haloG,
            float haloB
    ) {
        if (playerY <= startHeight) {
            return;
        }

        float theta = (float) Math.sqrt(Math.max((playerY - startHeight) / 1000.0D, 0.0D)) * 400.0F;
        float alpha = Mth.clamp((theta / 100.0F - 0.2F) * 2.0F, 0.0F, 1.0F);
        if (alpha <= 0.01F) {
            return;
        }

        // Illumination from celestial angle (day vs night side of the planet below)
        float celestialAngle = level.getTimeOfDay(partialTicks);
        float light = Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 0.75F + 0.6F, 0.2F, 1.0F);

        float scale = 850.0F * (0.25F - theta / 10000.0F) * baseScaleMultiplier;
        scale = Math.max(scale, 0.2F);

        poseStack.pushPose();

        // Authentic GC4 Legacy ground projection:
        // Scale horizontally on X and Z, and translate down by playerY to ground level (Y = 0)
        poseStack.scale(scale, 1.0F, scale);
        poseStack.translate(0.0F, -(float) playerY, 0.0F);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // 1. Optional Atmospheric Halo / Rim below the planet
        if (atmosphereTexture != null) {
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderColor(haloR * light, haloG * light, haloB * light, alpha * 0.55F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, atmosphereTexture);
            float haloSize = 1.08F;
            BufferBuilder haloBuffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            haloBuffer.addVertex(matrix, -haloSize, -0.2F, haloSize).setUv(0.0F, 1.0F);
            haloBuffer.addVertex(matrix, haloSize, -0.2F, haloSize).setUv(1.0F, 1.0F);
            haloBuffer.addVertex(matrix, haloSize, -0.2F, -haloSize).setUv(1.0F, 0.0F);
            haloBuffer.addVertex(matrix, -haloSize, -0.2F, -haloSize).setUv(0.0F, 0.0F);
            BufferUploader.drawWithShader(haloBuffer.buildOrThrow());
        }

        // 2. Planet Pixel Art Body (XZ horizontal planar quad)
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(light, light, light, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, planetTexture);

        float size = 1.0F;
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -size, 0.0F, size).setUv(0.0F, 1.0F);
        buffer.addVertex(matrix, size, 0.0F, size).setUv(1.0F, 1.0F);
        buffer.addVertex(matrix, size, 0.0F, -size).setUv(1.0F, 0.0F);
        buffer.addVertex(matrix, -size, 0.0F, -size).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);

        poseStack.popPose();
    }

    /**
     * Specialized orbital projection for space stations / satellites in orbit around a planet.
     */
    public static void renderPlanetBelowOrbit(
            PoseStack poseStack,
            Level level,
            ResourceLocation planetTexture,
            @Nullable ResourceLocation atmosphereTexture,
            float partialTicks,
            float size,
            float haloR,
            float haloG,
            float haloB
    ) {
        float celestialAngle = level.getTimeOfDay(partialTicks);
        float light = Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 0.75F + 0.6F, 0.2F, 1.0F);

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (atmosphereTexture != null) {
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderColor(haloR * light, haloG * light, haloB * light, 0.6F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, atmosphereTexture);
            float haloSize = size * 1.1F;
            BufferBuilder haloBuffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            haloBuffer.addVertex(matrix, -haloSize, -100.2F, haloSize).setUv(0.0F, 1.0F);
            haloBuffer.addVertex(matrix, haloSize, -100.2F, haloSize).setUv(1.0F, 1.0F);
            haloBuffer.addVertex(matrix, haloSize, -100.2F, -haloSize).setUv(1.0F, 0.0F);
            haloBuffer.addVertex(matrix, -haloSize, -100.2F, -haloSize).setUv(0.0F, 0.0F);
            BufferUploader.drawWithShader(haloBuffer.buildOrThrow());
        }

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(light, light, light, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, planetTexture);

        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -size, -100.0F, size).setUv(0.0F, 1.0F);
        buffer.addVertex(matrix, size, -100.0F, size).setUv(1.0F, 1.0F);
        buffer.addVertex(matrix, size, -100.0F, -size).setUv(1.0F, 0.0F);
        buffer.addVertex(matrix, -size, -100.0F, -size).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);

        poseStack.popPose();
    }
}
