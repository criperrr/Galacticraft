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
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Authoritative space sky renderer for outer planets (Jupiter, Saturn, Uranus, Neptune),
 * rendering deep atmospheric gas colors below cloud top, a distant scaled orbital Sun,
 * planetary moons and rings, and the planet body rendered underneath the player at high altitudes.
 */
public class OuterPlanetSkyRenderer extends SpaceSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    public static final OuterPlanetSkyRenderer JUPITER = new OuterPlanetSkyRenderer(
            9.0F,
            Constant.CelestialBody.JUPITER,
            new CelestialMoon[]{
                    new CelestialMoon(Constant.CelestialBody.IO, 2.0F, 30.0F, 50.0F, 1.2F),
                    new CelestialMoon(Constant.CelestialBody.EUROPA, 1.6F, 70.0F, 110.0F, 0.8F),
                    new CelestialMoon(Constant.CelestialBody.GANYMEDE, 2.4F, 120.0F, 160.0F, 0.5F),
                    new CelestialMoon(Constant.CelestialBody.CALLISTO, 2.2F, 190.0F, 220.0F, 0.3F)
            },
            null,
            215.0F / 255.0F, 125.0F / 255.0F, 55.0F / 255.0F,
            40.0F / 255.0F, 15.0F / 255.0F, 8.0F / 255.0F,
            0.85F, 0.55F, 0.25F
    );

    public static final OuterPlanetSkyRenderer SATURN = new OuterPlanetSkyRenderer(
            6.5F,
            Constant.CelestialBody.SATURN,
            new CelestialMoon[]{
                    new CelestialMoon(Constant.CelestialBody.TITAN, 2.2F, 50.0F, 70.0F, 0.6F),
                    new CelestialMoon(Constant.CelestialBody.ENCELADUS, 1.2F, 130.0F, 140.0F, 1.0F)
            },
            Constant.CelestialBody.SATURN_RINGS,
            230.0F / 255.0F, 195.0F / 255.0F, 125.0F / 255.0F,
            50.0F / 255.0F, 38.0F / 255.0F, 20.0F / 255.0F,
            0.90F, 0.80F, 0.50F
    );

    public static final OuterPlanetSkyRenderer URANUS = new OuterPlanetSkyRenderer(
            4.5F,
            Constant.CelestialBody.URANUS,
            new CelestialMoon[]{
                    new CelestialMoon(Constant.CelestialBody.TITANIA, 1.8F, 60.0F, 90.0F, 0.7F),
                    new CelestialMoon(Constant.CelestialBody.OBERON, 1.6F, 150.0F, 170.0F, 0.5F)
            },
            Constant.CelestialBody.URANUS_RINGS,
            95.0F / 255.0F, 200.0F / 255.0F, 210.0F / 255.0F,
            15.0F / 255.0F, 40.0F / 255.0F, 48.0F / 255.0F,
            0.40F, 0.85F, 0.90F
    );

    public static final OuterPlanetSkyRenderer NEPTUNE = new OuterPlanetSkyRenderer(
            3.5F,
            Constant.CelestialBody.NEPTUNE,
            new CelestialMoon[]{
                    new CelestialMoon(Constant.CelestialBody.TRITON, 2.0F, 80.0F, 100.0F, 0.6F)
            },
            null,
            45.0F / 255.0F, 95.0F / 255.0F, 210.0F / 255.0F,
            10.0F / 255.0F, 22.0F / 255.0F, 65.0F / 255.0F,
            0.20F, 0.45F, 0.90F
    );

    private final float sunSize;
    private final ResourceLocation planetTexture;
    private final CelestialMoon[] moons;
    private final ResourceLocation ringTexture;
    private final float daySkyR, daySkyG, daySkyB;
    private final float nightSkyR, nightSkyG, nightSkyB;
    private final float haloR, haloG, haloB;

    private VertexBuffer skyBuffer = null;

    public OuterPlanetSkyRenderer(
            float sunSize,
            ResourceLocation planetTexture,
            CelestialMoon[] moons,
            ResourceLocation ringTexture,
            float daySkyR, float daySkyG, float daySkyB,
            float nightSkyR, float nightSkyG, float nightSkyB,
            float haloR, float haloG, float haloB
    ) {
        this.sunSize = sunSize;
        this.planetTexture = planetTexture;
        this.moons = moons != null ? moons : new CelestialMoon[0];
        this.ringTexture = ringTexture;
        this.daySkyR = daySkyR;
        this.daySkyG = daySkyG;
        this.daySkyB = daySkyB;
        this.nightSkyR = nightSkyR;
        this.nightSkyG = nightSkyG;
        this.nightSkyB = nightSkyB;
        this.haloR = haloR;
        this.haloG = haloG;
        this.haloB = haloB;
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

    @Override
    public void render(WorldRenderContext context) {
        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack matrices = new PoseStack();
        matrices.mulPose(context.positionMatrix());
        float celestialAngle = context.world().getTimeOfDay(partialTicks);

        var player = Minecraft.getInstance().player;
        double playerY = player != null ? (player.getVehicle() != null ? player.getVehicle().getY() : player.getY()) : 64.0D;

        float spaceProgress = Mth.clamp((float) ((playerY - 200.0D) / 250.0D), 0.0F, 1.0F);
        float atmosphereAlpha = 1.0F - spaceProgress;

        float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
        float day = 1.0F - starBrightness;
        float night = starBrightness;

        float rSky = this.daySkyR * day + this.nightSkyR * night;
        float gSky = this.daySkyG * day + this.nightSkyG * night;
        float bSky = this.daySkyB * day + this.nightSkyB * night;

        // 1. Deep atmospheric gas dome (Envelops player in the planet's atmospheric gas layer)
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
            buffer.drawWithShader(matrices.last().pose(), context.projectionMatrix(), GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }

        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        // 2. Stars (Visible brilliantly in space and through the night sky)
        float starAlpha = Math.max(spaceProgress, night * 0.75F);
        if (starAlpha > 0.01F) {
            context.profiler().push("stars");
            matrices.pushPose();
            StarManager.rotateForCelestialAngle(matrices, celestialAngle);

            float renderStarBrightness = (starBrightness * starBrightness * 0.5F + 0.3F) * starAlpha;
            this.starManager.render(matrices, context.projectionMatrix(), context.world(), partialTicks, renderStarBrightness);

            matrices.popPose();
            context.profiler().pop();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 3. Distant Orbital Sun
        context.profiler().push("sun");
        matrices.pushPose();
        matrices.mulPose(Axis.YN.rotationDegrees(90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));

        // Occulting black quad behind sun
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

        // Radiant sun texture
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
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

        // 4. Planetary Rings (if present)
        if (this.ringTexture != null) {
            context.profiler().push("rings");
            matrices.pushPose();
            matrices.scale(0.8F, 0.8F, 0.8F);
            matrices.mulPose(Axis.XP.rotationDegrees(25.0F));
            matrices.mulPose(Axis.YP.rotationDegrees(celestialAngle * 360.0F * 0.1F));

            Matrix4f ringMatrix = matrices.last().pose();
            float ringSize = 18.0F;
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.85F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, this.ringTexture);
            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.addVertex(ringMatrix, -ringSize, -100.0F, ringSize).setUv(0.0F, 1.0F)
                    .addVertex(ringMatrix, ringSize, -100.0F, ringSize).setUv(1.0F, 1.0F)
                    .addVertex(ringMatrix, ringSize, -100.0F, -ringSize).setUv(1.0F, 0.0F)
                    .addVertex(ringMatrix, -ringSize, -100.0F, -ringSize).setUv(0.0F, 0.0F);
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            RenderSystem.disableBlend();
            matrices.popPose();
            context.profiler().pop();
        }

        // 5. Moons
        if (this.moons.length > 0) {
            context.profiler().push("moons");
            for (CelestialMoon moon : this.moons) {
                matrices.pushPose();
                matrices.scale(0.7F, 0.7F, 0.7F);
                matrices.mulPose(Axis.ZP.rotationDegrees(celestialAngle * 360.0F * moon.orbitSpeed + moon.angleZ));
                matrices.mulPose(Axis.XP.rotationDegrees(moon.angleX));

                Matrix4f moonMatrix = matrices.last().pose();
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, moon.texture);
                buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                buffer.addVertex(moonMatrix, -moon.size, -100.0F, moon.size).setUv(0.0F, 1.0F)
                        .addVertex(moonMatrix, moon.size, -100.0F, moon.size).setUv(1.0F, 1.0F)
                        .addVertex(moonMatrix, moon.size, -100.0F, -moon.size).setUv(1.0F, 0.0F)
                        .addVertex(moonMatrix, -moon.size, -100.0F, -moon.size).setUv(0.0F, 0.0F);
                BufferUploader.drawWithShader(buffer.buildOrThrow());

                RenderSystem.disableBlend();
                matrices.popPose();
            }
            context.profiler().pop();
        }

        // 6. Planet Below Player (Pixel art rendered below at high altitudes)
        PlanetUnderPlayerRenderer.renderPlanetBelow(
                matrices,
                context.world(),
                this.planetTexture,
                Constant.Skybox.ATMOSPHERE,
                playerY,
                partialTicks,
                180.0F,
                1.0F,
                this.haloR,
                this.haloG,
                this.haloB
        );

        RenderSystem.depthMask(true);
    }

    public record CelestialMoon(ResourceLocation texture, float size, float angleX, float angleZ, float orbitSpeed) {
    }
}
