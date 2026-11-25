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
import dev.galacticraft.mod.events.GCGasGiantAtmosphereHandler;
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
            232.0F / 255.0F, 148.0F / 255.0F, 78.0F / 255.0F,
            84.0F / 255.0F, 40.0F / 255.0F, 22.0F / 255.0F,
            0.94F, 0.66F, 0.36F
    );

    public static final OuterPlanetSkyRenderer SATURN = new OuterPlanetSkyRenderer(
            6.5F,
            Constant.CelestialBody.SATURN,
            new CelestialMoon[]{
                    new CelestialMoon(Constant.CelestialBody.TITAN, 2.2F, 50.0F, 70.0F, 0.6F),
                    new CelestialMoon(Constant.CelestialBody.ENCELADUS, 1.2F, 130.0F, 140.0F, 1.0F)
            },
            Constant.CelestialBody.SATURN_RINGS,
            242.0F / 255.0F, 212.0F / 255.0F, 144.0F / 255.0F,
            88.0F / 255.0F, 68.0F / 255.0F, 38.0F / 255.0F,
            0.96F, 0.86F, 0.58F
    );

    public static final OuterPlanetSkyRenderer URANUS = new OuterPlanetSkyRenderer(
            4.5F,
            Constant.CelestialBody.URANUS,
            new CelestialMoon[]{
                    new CelestialMoon(Constant.CelestialBody.TITANIA, 1.8F, 60.0F, 90.0F, 0.7F),
                    new CelestialMoon(Constant.CelestialBody.OBERON, 1.6F, 150.0F, 170.0F, 0.5F)
            },
            Constant.CelestialBody.URANUS_RINGS,
            116.0F / 255.0F, 218.0F / 255.0F, 228.0F / 255.0F,
            28.0F / 255.0F, 76.0F / 255.0F, 92.0F / 255.0F,
            0.52F, 0.92F, 0.96F
    );

    public static final OuterPlanetSkyRenderer NEPTUNE = new OuterPlanetSkyRenderer(
            3.5F,
            Constant.CelestialBody.NEPTUNE,
            new CelestialMoon[]{
                    new CelestialMoon(Constant.CelestialBody.TRITON, 2.0F, 80.0F, 100.0F, 0.6F)
            },
            null,
            66.0F / 255.0F, 124.0F / 255.0F, 234.0F / 255.0F,
            20.0F / 255.0F, 44.0F / 255.0F, 118.0F / 255.0F,
            0.34F, 0.62F, 0.98F
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
        double playerY = player != null ? (player.getVehicle() != null ? player.getVehicle().getY() : player.getY()) : GCGasGiantAtmosphereHandler.AEROSTATIC_REFERENCE_Y;

        float rawProgress = Mth.clamp(
                (float) ((playerY - Constant.ATMOSPHERE_FADE_START) / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FADE_START)),
                0.0F,
                1.0F
        );
        float spaceProgress = rawProgress * rawProgress * (3.0F - 2.0F * rawProgress);

        // Beer-Lambert optical sunlight extinction when descending into dense cloud decks and abyssal depths
        float opticalTransmittance = GCGasGiantAtmosphereHandler.computeOpticalTransmittance(context.world().dimension(), playerY);

        float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
        float day = 1.0F - starBrightness;
        float night = starBrightness;
        float baseTimeOpacity = Mth.lerp(day, 0.58F, 1.0F);
        float timeOpacity = Mth.lerp(1.0F - opticalTransmittance, baseTimeOpacity, 1.0F);
        float atmosphereAlpha = (1.0F - spaceProgress) * timeOpacity;

        float effectiveDay = day * (0.14F + 0.86F * opticalTransmittance);
        float effectiveNight = night * (0.48F + 0.52F * opticalTransmittance) + day * (1.0F - opticalTransmittance) * 0.58F;
        float rSky = Mth.clamp(this.daySkyR * effectiveDay + this.nightSkyR * effectiveNight, 0.0F, 1.0F);
        float gSky = Mth.clamp(this.daySkyG * effectiveDay + this.nightSkyG * effectiveNight, 0.0F, 1.0F);
        float bSky = Mth.clamp(this.daySkyB * effectiveDay + this.nightSkyB * effectiveNight, 0.0F, 1.0F);

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

        // 2-4. Realistic AstroCraft outer-planet sky (real Keplerian moons, distant Sun, Milky Way & stars)
        // Attenuated by cloud optical depth; completely extinguished deep below cloud decks
        float starAlpha = Math.max(spaceProgress, (night * 0.85F + 0.15F) * opticalTransmittance);
        if (opticalTransmittance > 0.02F) {
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
        }

        // 5. Realistic 3D Planetary Ring Arch across the sky (Saturn & Uranus)
        if (this.ringTexture != null && opticalTransmittance > 0.01F) {
            context.profiler().push("planetary_rings");
            PlanetaryRingSkyRenderer.RingProfile ringProfile = this.ringTexture.equals(Constant.CelestialBody.URANUS_RINGS)
                    ? PlanetaryRingSkyRenderer.URANUS
                    : PlanetaryRingSkyRenderer.SATURN;
            PlanetaryRingSkyRenderer.render(matrices, ringProfile, celestialAngle, spaceProgress, rSky, gSky, bSky, opticalTransmittance);
            context.profiler().pop();
        }

        // 5b. Opaque Banded Planetary Cloud-Ocean Lower Hemisphere & Luminous Horizon Veil:
        // Masks all stars/void below the horizon with an infinite, animated swirling ocean of planetary clouds!
        if (spaceProgress < 0.99F) {
            double animSec = ((context.world().getGameTime() & 0x3FFFFL) + partialTicks) * 0.05D;
            renderPlanetaryCloudHorizonDome(matrices.last().pose(), rSky, gSky, bSky, 1.0F - spaceProgress, opticalTransmittance, animSec);
        }

        // 5c. Smooth Beer-Lambert cloud extinction veil over upper sky when descending below upper cloud tops
        if (opticalTransmittance < 0.98F && spaceProgress < 0.05F) {
            float cloudVeilAlpha = Mth.clamp((1.0F - opticalTransmittance) * 0.96F, 0.0F, 0.96F);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            buffer.bind();
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.setShaderColor(rSky, gSky, bSky, cloudVeilAlpha);
            buffer.drawWithShader(matrices.last().pose(), context.projectionMatrix(), GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
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

    /**
     * Renders an opaque lower hemisphere of animated banded planetary clouds plus a soft atmospheric
     * horizon glow band (+18 deg to -90 deg), ensuring stars and space void NEVER show beneath the player
     * while inside a gas/ice giant's atmosphere.
     */
    private void renderPlanetaryCloudHorizonDome(
            Matrix4f matrix,
            float rSky,
            float gSky,
            float bSky,
            float atmOpacity,
            float opticalTransmittance,
            double animSec
    ) {
        final float radius = 180.0F;
        final int yawSegs = 40;
        // Pitch rings from +20 deg above horizon down to -90 deg nadir
        final float[] pitchDegs = new float[]{20.0F, 10.0F, 3.0F, -2.0F, -10.0F, -22.0F, -38.0F, -56.0F, -74.0F, -90.0F};
        final float[] ringAlphas = new float[]{0.0F, 0.34F, 0.82F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float drift = (float) (animSec * 0.14D);

        for (int rIdx = 0; rIdx < pitchDegs.length - 1; rIdx++) {
            float p0 = (float) Math.toRadians(pitchDegs[rIdx]);
            float p1 = (float) Math.toRadians(pitchDegs[rIdx + 1]);
            float y0 = radius * Mth.sin(p0);
            float h0 = radius * Mth.cos(p0);
            float y1 = radius * Mth.sin(p1);
            float h1 = radius * Mth.cos(p1);
            float a0 = Mth.clamp(ringAlphas[rIdx] * atmOpacity, 0.0F, 1.0F);
            float a1 = Mth.clamp(ringAlphas[rIdx + 1] * atmOpacity, 0.0F, 1.0F);

            for (int i = 0; i < yawSegs; i++) {
                float yaw0 = ((float) i / yawSegs) * Mth.TWO_PI;
                float yaw1 = ((float) (i + 1) / yawSegs) * Mth.TWO_PI;

                float x00 = h0 * Mth.cos(yaw0);
                float z00 = h0 * Mth.sin(yaw0);
                float x10 = h0 * Mth.cos(yaw1);
                float z10 = h0 * Mth.sin(yaw1);
                float x11 = h1 * Mth.cos(yaw1);
                float z11 = h1 * Mth.sin(yaw1);
                float x01 = h1 * Mth.cos(yaw0);
                float z01 = h1 * Mth.sin(yaw0);

                float[] c00 = computeHorizonBandColor(pitchDegs[rIdx], yaw0, drift, rSky, gSky, bSky, opticalTransmittance);
                float[] c10 = computeHorizonBandColor(pitchDegs[rIdx], yaw1, drift, rSky, gSky, bSky, opticalTransmittance);
                float[] c11 = computeHorizonBandColor(pitchDegs[rIdx + 1], yaw1, drift, rSky, gSky, bSky, opticalTransmittance);
                float[] c01 = computeHorizonBandColor(pitchDegs[rIdx + 1], yaw0, drift, rSky, gSky, bSky, opticalTransmittance);

                bb.addVertex(matrix, x00, y0, z00).setColor(c00[0], c00[1], c00[2], a0);
                bb.addVertex(matrix, x10, y0, z10).setColor(c10[0], c10[1], c10[2], a0);
                bb.addVertex(matrix, x11, y1, z11).setColor(c11[0], c11[1], c11[2], a1);

                bb.addVertex(matrix, x00, y0, z00).setColor(c00[0], c00[1], c00[2], a0);
                bb.addVertex(matrix, x11, y1, z11).setColor(c11[0], c11[1], c11[2], a1);
                bb.addVertex(matrix, x01, y1, z01).setColor(c01[0], c01[1], c01[2], a1);
            }
        }

        MeshData mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private float[] computeHorizonBandColor(
            float pitchDeg,
            float yawRad,
            float drift,
            float rSky,
            float gSky,
            float bSky,
            float opticalTransmittance
    ) {
        // Luminous atmospheric limb scattering near the horizon (pitch ~ 0 deg)
        float horizonGlow = Mth.clamp(1.0F - Math.abs(pitchDeg) / 22.0F, 0.0F, 1.0F);
        // Animated zonal cloud striping across the lower cloud ocean
        float belowFactor = Mth.clamp(-pitchDeg / 25.0F, 0.0F, 1.0F);
        float swirl = Mth.sin(pitchDeg * 0.24F + Mth.sin(yawRad * 2.0F + drift) * 0.65F) * 0.5F
                + Mth.cos(pitchDeg * 0.52F - yawRad * 3.0F + drift * 1.4F) * 0.25F;
        float bandMod = 1.0F + belowFactor * (swirl * 0.26F - 0.14F);
        float nadirDepth = Mth.clamp((-pitchDeg - 35.0F) / 55.0F, 0.0F, 1.0F);
        float depthShade = Mth.lerp(nadirDepth, 1.0F, 0.52F);

        float boost = 1.0F + horizonGlow * 0.28F * (0.4F + 0.6F * opticalTransmittance);
        float r = Mth.clamp(Mth.lerp(horizonGlow * 0.35F, rSky, this.haloR * 0.95F) * bandMod * depthShade * boost, 0.0F, 1.0F);
        float g = Mth.clamp(Mth.lerp(horizonGlow * 0.35F, gSky, this.haloG * 0.95F) * bandMod * depthShade * boost, 0.0F, 1.0F);
        float b = Mth.clamp(Mth.lerp(horizonGlow * 0.35F, bSky, this.haloB * 0.95F) * bandMod * depthShade * boost, 0.0F, 1.0F);
        return new float[]{r, g, b};
    }

    public record CelestialMoon(ResourceLocation texture, float size, float angleX, float angleZ, float orbitSpeed) {
    }
}
