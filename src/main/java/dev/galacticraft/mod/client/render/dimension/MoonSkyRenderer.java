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
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class MoonSkyRenderer extends SpaceSkyRenderer {
    public static final MoonSkyRenderer INSTANCE = new MoonSkyRenderer();

    @Override
    public void render(WorldRenderContext context) {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack matrices = new PoseStack();
        matrices.mulPose(context.positionMatrix());
        float celestialAngle = context.world().getTimeOfDay(partialTicks);

        // 1. Stars (Authoritative GC4 celestial rotation and seed 10842L)
        context.profiler().push("stars");
        matrices.pushPose();
        StarManager.rotateForCelestialAngle(matrices, celestialAngle);

        float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
        starBrightness = starBrightness * starBrightness * 0.5F + 0.3F;
        this.starManager.render(matrices, context.projectionMatrix(), context.world(), partialTicks, starBrightness);

        matrices.popPose();
        context.profiler().pop();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 2. Sun (with occulting black square behind radiant orbital solar corona)
        context.profiler().push("sun");
        matrices.pushPose();
        matrices.mulPose(Axis.YN.rotationDegrees(90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));

        float sunSize = 20.0F;

        // Occulting black square behind sun to prevent stars from shining through
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        float blackSize = sunSize / 3.5F;
        Matrix4f sunMatrix = matrices.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(sunMatrix, -blackSize, 99.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 99.9F, -blackSize)
                .addVertex(sunMatrix, blackSize, 99.9F, blackSize)
                .addVertex(sunMatrix, -blackSize, 99.9F, blackSize);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // Radiant sun texture with additive blending (SRC_ALPHA, ONE)
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.Skybox.SUN_ORBITAL);
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(sunMatrix, -sunSize, 100.0F, -sunSize).setUv(0.0F, 0.0F)
                .addVertex(sunMatrix, sunSize, 100.0F, -sunSize).setUv(1.0F, 0.0F)
                .addVertex(sunMatrix, sunSize, 100.0F, sunSize).setUv(1.0F, 1.0F)
                .addVertex(sunMatrix, -sunSize, 100.0F, sunSize).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        // 3. Earth (Home) as seen from the Moon
        context.profiler().push("earth");
        matrices.pushPose();

        float size = 10.0F;
        var player = Minecraft.getInstance().player;
        float earthRotation = player != null ? (float) (context.world().getSharedSpawnPos().getZ() - player.getZ()) * 0.01F : 0.0F;

        matrices.scale(0.6F, 0.6F, 0.6F);
        matrices.mulPose(Axis.XP.rotationDegrees(earthRotation));
        matrices.mulPose(Axis.XP.rotationDegrees(200.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F));

        Matrix4f earthMatrix = matrices.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.CelestialBody.EARTH);

        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(earthMatrix, -size, -100.0F, size).setUv(0.0F, 1.0F)
                .addVertex(earthMatrix, size, -100.0F, size).setUv(1.0F, 1.0F)
                .addVertex(earthMatrix, size, -100.0F, -size).setUv(1.0F, 0.0F)
                .addVertex(earthMatrix, -size, -100.0F, -size).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        RenderSystem.depthMask(true);
    }
}
