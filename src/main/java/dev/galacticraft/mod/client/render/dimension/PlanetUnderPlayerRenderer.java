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
 * faithfully restored from Galacticraft Legacy (micdoodle8) and its classic addons.
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
            float maxFadeHeight,
            float baseScale,
            float haloR,
            float haloG,
            float haloB
    ) {
        if (playerY <= startHeight) {
            return;
        }

        float progress = (float) ((playerY - startHeight) / (maxFadeHeight - startHeight));
        float alpha = Mth.clamp(progress, 0.0F, 1.0F);
        if (alpha <= 0.01F) {
            return;
        }

        // Illumination from the celestial angle (day vs night side of the planet)
        float celestialAngle = level.getTimeOfDay(partialTicks);
        float light = Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 0.7F + 0.6F, 0.25F, 1.0F);

        // Apparent angular scale: wide when just clearing the terrain/clouds, gradually receding
        float heightFactor = Mth.clamp((float) ((playerY - startHeight) / 1200.0D), 0.0F, 0.6F);
        float size = baseScale * (1.25F - 0.45F * heightFactor);

        poseStack.pushPose();

        // Orient planet below player at nadir (Y = -100)
        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);

        // 1. Optional Atmospheric Halo / Rim below the planet
        if (atmosphereTexture != null) {
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderColor(haloR * light, haloG * light, haloB * light, alpha * 0.4F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, atmosphereTexture);
            float haloSize = size * 1.12F;
            BufferBuilder haloBuffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            haloBuffer.addVertex(matrix, -haloSize, -100.2F, haloSize).setUv(0.0F, 1.0F)
                    .addVertex(matrix, haloSize, -100.2F, haloSize).setUv(1.0F, 1.0F)
                    .addVertex(matrix, haloSize, -100.2F, -haloSize).setUv(1.0F, 0.0F)
                    .addVertex(matrix, -haloSize, -100.2F, -haloSize).setUv(0.0F, 0.0F);
            BufferUploader.drawWithShader(haloBuffer.buildOrThrow());
        }

        // 2. Planet Pixel Art Body
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(light, light, light, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, planetTexture);

        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -size, -100.0F, size).setUv(0.0F, 1.0F)
                .addVertex(matrix, size, -100.0F, size).setUv(1.0F, 1.0F)
                .addVertex(matrix, size, -100.0F, -size).setUv(1.0F, 0.0F)
                .addVertex(matrix, -size, -100.0F, -size).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        poseStack.popPose();
    }
}
