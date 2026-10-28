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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Authoritative space sky renderer for Mercury, featuring a massive blazing orbital Sun,
 * dense 24/7 star field in deep space vacuum, and Mercury planet body rendered underneath
 * the player at high altitudes.
 */
public class MercurySkyRenderer extends SpaceSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    public static final MercurySkyRenderer INSTANCE = new MercurySkyRenderer();

    private final float sunSize = 35.0F;

    public MercurySkyRenderer() {
    }

    @Override
    public void render(WorldRenderContext context) {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack matrices = new PoseStack();
        matrices.mulPose(context.positionMatrix());
        float celestialAngle = context.world().getTimeOfDay(partialTicks);

        // 1. Stars (Brilliant star field in deep space vacuum, visible 24/7 like the Moon)
        context.profiler().push("stars");
        matrices.pushPose();
        StarManager.rotateForCelestialAngle(matrices, celestialAngle);

        float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
        starBrightness = starBrightness * starBrightness * 0.5F + 0.3F;
        this.starManager.render(matrices, context.projectionMatrix(), context.world(), partialTicks, starBrightness);

        matrices.popPose();
        context.profiler().pop();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 2. Huge Sun (Prominent blazing orbital sun close to Mercury)
        context.profiler().push("sun");
        matrices.pushPose();
        matrices.mulPose(Axis.YN.rotationDegrees(90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));

        // Occulting black square behind sun to prevent stars from shining through
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        float blackSize = this.sunSize / 3.5F;
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
        buffer.addVertex(sunMatrix, -this.sunSize, 100.0F, -this.sunSize).setUv(0.0F, 0.0F)
                .addVertex(sunMatrix, this.sunSize, 100.0F, -this.sunSize).setUv(1.0F, 0.0F)
                .addVertex(sunMatrix, this.sunSize, 100.0F, this.sunSize).setUv(1.0F, 1.0F)
                .addVertex(sunMatrix, -this.sunSize, 100.0F, this.sunSize).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
        matrices.popPose();
        context.profiler().pop();

        // 3. Mercury Planet Below Player (Pixel art rendered below at high altitudes)
        var player = Minecraft.getInstance().player;
        double playerY = player != null ? (player.getVehicle() != null ? player.getVehicle().getY() : player.getY()) : 64.0D;
        PlanetUnderPlayerRenderer.renderPlanetBelow(
                matrices,
                context.world(),
                Constant.CelestialBody.MERCURY,
                null,
                playerY,
                partialTicks,
                180.0F,
                1.0F,
                0.0F,
                0.0F,
                0.0F
        );

        RenderSystem.depthMask(true);
    }
}
