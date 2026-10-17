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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class SatelliteSkyRenderer extends SpaceSkyRenderer {
    public static final SatelliteSkyRenderer INSTANCE = new SatelliteSkyRenderer(Constant.Skybox.EARTH);
    protected final EarthManager earthManager = new EarthManager();
    private final ResourceLocation parentBody;

    public SatelliteSkyRenderer(ResourceLocation parentBody) {
        this.parentBody = parentBody;
    }

    @Override
    public void render(WorldRenderContext context) {
        context.profiler().push("satellite_sky_renderer");
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack matrices = new PoseStack();
        matrices.mulPose(context.positionMatrix());
        Tesselator tesselator = Tesselator.getInstance();

        float celestialAngle = context.world().getTimeOfDay(partialTicks);
        context.profiler().push("stars");
        matrices.pushPose();
        StarManager.rotateForCelestialAngle(matrices, celestialAngle);

        this.starManager.render(matrices, context.projectionMatrix(), context.world(), partialTicks, 0.9F);

        matrices.popPose();
        context.profiler().pop();

        context.profiler().push("sun");
        matrices.pushPose();
        matrices.mulPose(Axis.YN.rotationDegrees(90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));

        float sunSize = 16.0F;
        float blackSize = sunSize / 3.5F;
        Matrix4f matrix = matrices.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.addVertex(matrix, -blackSize, 99.9F, -blackSize)
                .addVertex(matrix, blackSize, 99.9F, -blackSize)
                .addVertex(matrix, blackSize, 99.9F, blackSize)
                .addVertex(matrix, -blackSize, 99.9F, blackSize);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Constant.Skybox.SUN_ORBITAL);
        buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -sunSize, 100.0F, -sunSize).setUv(0.0F, 0.0F)
                .addVertex(matrix, sunSize, 100.0F, -sunSize).setUv(1.0F, 0.0F)
                .addVertex(matrix, sunSize, 100.0F, sunSize).setUv(1.0F, 1.0F)
                .addVertex(matrix, -sunSize, 100.0F, sunSize).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        context.profiler().pop();
        context.profiler().push("moon");

        RenderSystem.setShaderTexture(0, Constant.Skybox.MOON_PHASES);
        float moonSize = 6.0F;
        final float phase = context.world().getMoonPhase();
        final int u = (int) (phase % 4);
        final int v = (int) (phase / 4 % 2);
        final float u0 = (u + 0.375F) / 4.0F;
        final float u1 = (u + 0.625F) / 4.0F;
        final float v0 = (v + 0.375F) / 2.0F;
        final float v1 = (v + 0.625F) / 2.0F;
        buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -moonSize, -100.0F, moonSize).setUv(u0, v1)
                .addVertex(matrix, moonSize, -100.0F, moonSize).setUv(u0, v0)
                .addVertex(matrix, moonSize, -100.0F, -moonSize).setUv(u1, v0)
                .addVertex(matrix, -moonSize, -100.0F, -moonSize).setUv(u1, v1);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        matrices.popPose();
        context.profiler().pop();

        context.profiler().push("earth");
        float light = Mth.clamp(Mth.cos(context.world().getTimeOfDay(partialTicks) * Mth.TWO_PI) * 2.0F + 1.5F, 0.5F, 1.0F);
        RenderSystem.setShaderColor(light, light, light, 1.0F);
        this.earthManager.render(matrices, context.world(), context.camera().getPosition().y() + 2048.0D, partialTicks, context.camera());
        context.profiler().pop();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        context.profiler().pop();
    }
}