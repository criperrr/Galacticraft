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
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class AsteroidSkyRenderer extends SpaceSkyRenderer {
    public static final AsteroidSkyRenderer INSTANCE = new AsteroidSkyRenderer();

    @Override
    public void render(WorldRenderContext context) {
        context.profiler().push("asteroid_sky_renderer");
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack matrices = new PoseStack();
        matrices.mulPose(context.positionMatrix());
        float celestialAngle = context.world().getTimeOfDay(partialTicks);

        // 1. Stars (Brilliant star field in deep space vacuum)
        context.profiler().push("stars");
        matrices.pushPose();
        StarManager.rotateForCelestialAngle(matrices, celestialAngle);
        this.starManager.render(matrices, context.projectionMatrix(), context.world(), partialTicks, 0.9F);
        matrices.popPose();
        context.profiler().pop();

        // 2. Distant Spiral Galaxy background (Iconic GC4 Asteroids feature)
        context.profiler().push("galaxy");
        matrices.pushPose();
        matrices.mulPose(Axis.YN.rotationDegrees(90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(35.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees(20.0F));
        Matrix4f galaxyMatrix = matrices.last().pose();
        float galaxySize = 65.0F;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(0.8F, 0.85F, 1.0F, 0.45F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.Skybox.GALAXY);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(galaxyMatrix, -galaxySize, 100.0F, -galaxySize).setUv(0.0F, 0.0F)
                .addVertex(galaxyMatrix, galaxySize, 100.0F, -galaxySize).setUv(1.0F, 0.0F)
                .addVertex(galaxyMatrix, galaxySize, 100.0F, galaxySize).setUv(1.0F, 1.0F)
                .addVertex(galaxyMatrix, -galaxySize, 100.0F, galaxySize).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        // 3. Sun (Radiant orbital sun with occulting black square behind it)
        context.profiler().push("sun");
        matrices.pushPose();
        matrices.mulPose(Axis.YN.rotationDegrees(90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));

        float sunSize = 14.5F;
        float blackSize = sunSize / 4.2F;
        Matrix4f sunMatrix = matrices.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(sunMatrix, -blackSize, 89.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 89.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 89.9F, blackSize)
                .addVertex(sunMatrix, -blackSize, 89.9F, blackSize);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.Skybox.SUN_ORBITAL);

        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(sunMatrix, -sunSize, 90.0F, -sunSize).setUv(0.0F, 0.0F)
                .addVertex(sunMatrix, sunSize, 90.0F, -sunSize).setUv(1.0F, 0.0F)
                .addVertex(sunMatrix, sunSize, 90.0F, sunSize).setUv(1.0F, 1.0F)
                .addVertex(sunMatrix, -sunSize, 90.0F, sunSize).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        // 4. Distant Earth (Home as seen from the Asteroid Belt)
        context.profiler().push("earth");
        matrices.pushPose();

        float earthSize = 0.5F;
        matrices.scale(0.6F, 0.6F, 0.6F);
        matrices.mulPose(Axis.ZP.rotationDegrees(40.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(200.0F));

        Matrix4f earthMatrix = matrices.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.CelestialBody.EARTH);

        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(earthMatrix, -earthSize, -100.0F, earthSize).setUv(0.0F, 1.0F)
                .addVertex(earthMatrix, earthSize, -100.0F, earthSize).setUv(1.0F, 1.0F)
                .addVertex(earthMatrix, earthSize, -100.0F, -earthSize).setUv(1.0F, 0.0F)
                .addVertex(earthMatrix, -earthSize, -100.0F, -earthSize).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        // 5. Dwarf Planet Ceres below when ascending to high altitudes
        var player = Minecraft.getInstance().player;
        double playerY = player != null ? player.getY() : 64.0D;
        PlanetUnderPlayerRenderer.renderPlanetBelow(
                matrices,
                context.world(),
                Constant.CelestialBody.CERES,
                null,
                playerY,
                partialTicks,
                180.0F,
                450.0F,
                60.0F,
                0.0F,
                0.0F,
                0.0F
        );

        RenderSystem.depthMask(true);
        context.profiler().pop();
    }
}
