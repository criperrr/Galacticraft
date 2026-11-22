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
import dev.galacticraft.mod.client.render.astro.PlanetaryRingSkyRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

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

        float rawProgress = Mth.clamp(
                (float) ((playerY - Constant.ATMOSPHERE_FADE_START) / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FADE_START)),
                0.0F,
                1.0F
        );
        float spaceProgress = rawProgress * rawProgress * (3.0F - 2.0F * rawProgress);

        float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
        float day = 1.0F - starBrightness;
        float night = starBrightness;
        float timeOpacity = Mth.lerp(day, 0.28F, 1.0F);
        float atmosphereAlpha = (1.0F - spaceProgress) * timeOpacity;

        float rSky = this.daySkyR * day + this.nightSkyR * night;
        float gSky = this.daySkyG * day + this.nightSkyG * night;
        float bSky = this.daySkyB * day + this.nightSkyB * night;

        // 1. Deep space black base + thinning atmospheric gas dome (Y=500 -> Y=780)
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        FogRenderer.levelFogColor();
        VertexBuffer buffer = getSkyBuffer();
        buffer.bind();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        buffer.drawWithShader(matrices.last().pose(), context.projectionMatrix(), GameRenderer.getPositionShader());
        if (atmosphereAlpha > 0.005F) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderColor(rSky, gSky, bSky, atmosphereAlpha);
            buffer.drawWithShader(matrices.last().pose(), context.projectionMatrix(), GameRenderer.getPositionShader());
            RenderSystem.disableBlend();
        }
        VertexBuffer.unbind();
        RenderSystem.enableCull();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        // 2-5. Realistic AstroCraft outer-planet sky (real Keplerian moons, distant Sun, Milky Way & stars)
        float starAlpha = Math.max(spaceProgress, night * 0.85F + 0.15F);
        if (AstroSkyEngine.isInitialized()) {
            context.profiler().push("astro_outer_planet_sky");
            AstroSkyRenderer.renderSpaceDimensionSky(matrices, context.projectionMatrix(), starAlpha, false);
            context.profiler().pop();
        } else if (starAlpha > 0.01F) {
            context.profiler().push("stars");
            matrices.pushPose();
            StarManager.rotateForCelestialAngle(matrices, celestialAngle);
            float renderStarBrightness = (starBrightness * starBrightness * 0.5F + 0.3F) * starAlpha;
            this.starManager.render(matrices, context.projectionMatrix(), context.world(), partialTicks, renderStarBrightness);
            matrices.popPose();
            context.profiler().pop();
        }

        // 5. Realistic 3D Planetary Ring Arch across the sky (Saturn & Uranus)
        if (this.ringTexture != null) {
            context.profiler().push("planetary_rings");
            PlanetaryRingSkyRenderer.RingProfile ringProfile = this.ringTexture.equals(Constant.CelestialBody.URANUS_RINGS)
                    ? PlanetaryRingSkyRenderer.URANUS
                    : PlanetaryRingSkyRenderer.SATURN;
            PlanetaryRingSkyRenderer.render(matrices, ringProfile, celestialAngle, spaceProgress, rSky, gSky, bSky);
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
                Constant.ATMOSPHERE_EXIT_START,
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
