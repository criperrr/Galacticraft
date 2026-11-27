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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroSkyRenderer;
import dev.galacticraft.mod.client.render.astro.aurora.AuroraRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Sky renderer for planetary moons (Phase 4 Outer Moons: Titan, Enceladus, Titania, Oberon, Triton;
 * compatible with Phase 3 inner/Jovian moons).
 * Uses AstroSkyEngine with moon-mounted observers so the giant parent planet (Saturn with rings,
 * Uranus, Neptune) and sibling moons appear at their real Keplerian angular sizes and phases.
 */
public class MoonParentSkyRenderer extends SpaceSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    public static final MoonParentSkyRenderer PHOBOS = new MoonParentSkyRenderer(
            Constant.CelestialBody.PHOBOS,
            Constant.CelestialBody.MARS,
            32.0F,
            false,
            0.0F, 0.0F, 0.0F
    );

    public static final MoonParentSkyRenderer DEIMOS = new MoonParentSkyRenderer(
            Constant.CelestialBody.DEIMOS,
            Constant.CelestialBody.MARS,
            14.0F,
            false,
            0.0F, 0.0F, 0.0F
    );

    public static final MoonParentSkyRenderer IO = new MoonParentSkyRenderer(
            Constant.CelestialBody.IO,
            Constant.CelestialBody.JUPITER,
            26.0F,
            false,
            0.65F, 0.50F, 0.12F
    );

    public static final MoonParentSkyRenderer EUROPA = new MoonParentSkyRenderer(
            Constant.CelestialBody.EUROPA,
            Constant.CelestialBody.JUPITER,
            18.0F,
            false,
            0.30F, 0.55F, 0.75F
    );

    public static final MoonParentSkyRenderer GANYMEDE = new MoonParentSkyRenderer(
            Constant.CelestialBody.GANYMEDE,
            Constant.CelestialBody.JUPITER,
            12.0F,
            true,
            0.25F, 0.40F, 0.55F
    );

    public static final MoonParentSkyRenderer CALLISTO = new MoonParentSkyRenderer(
            Constant.CelestialBody.CALLISTO,
            Constant.CelestialBody.JUPITER,
            8.0F,
            false,
            0.15F, 0.18F, 0.22F
    );

    public static final MoonParentSkyRenderer TITAN = new MoonParentSkyRenderer(
            Constant.CelestialBody.TITAN,
            Constant.CelestialBody.SATURN,
            16.0F,
            false,
            0.85F, 0.52F, 0.18F,
            0.40F
    );

    public static final MoonParentSkyRenderer ENCELADUS = new MoonParentSkyRenderer(
            Constant.CelestialBody.ENCELADUS,
            Constant.CelestialBody.SATURN,
            34.0F,
            false,
            0.65F, 0.85F, 0.96F
    );

    public static final MoonParentSkyRenderer TITANIA = new MoonParentSkyRenderer(
            Constant.CelestialBody.TITANIA,
            Constant.CelestialBody.URANUS,
            18.0F,
            false,
            0.35F, 0.78F, 0.86F
    );

    public static final MoonParentSkyRenderer OBERON = new MoonParentSkyRenderer(
            Constant.CelestialBody.OBERON,
            Constant.CelestialBody.URANUS,
            14.0F,
            false,
            0.30F, 0.55F, 0.65F
    );

    public static final MoonParentSkyRenderer TRITON = new MoonParentSkyRenderer(
            Constant.CelestialBody.TRITON,
            Constant.CelestialBody.NEPTUNE,
            22.0F,
            false,
            0.32F, 0.52F, 0.92F
    );

    private final ResourceLocation moonTexture;
    private final ResourceLocation parentTexture;
    private final float parentSize;
    private final boolean enableAurora;
    private final float haloR;
    private final float haloG;
    private final float haloB;
    private final float skyTransparency;

    public MoonParentSkyRenderer(
            ResourceLocation moonTexture,
            ResourceLocation parentTexture,
            float parentSize,
            boolean enableAurora,
            float haloR,
            float haloG,
            float haloB
    ) {
        this(moonTexture, parentTexture, parentSize, enableAurora, haloR, haloG, haloB, 1.0F);
    }

    public MoonParentSkyRenderer(
            ResourceLocation moonTexture,
            ResourceLocation parentTexture,
            float parentSize,
            boolean enableAurora,
            float haloR,
            float haloG,
            float haloB,
            float skyTransparency
    ) {
        this.moonTexture = moonTexture;
        this.parentTexture = parentTexture;
        this.parentSize = parentSize;
        this.enableAurora = enableAurora;
        this.haloR = haloR;
        this.haloG = haloG;
        this.haloB = haloB;
        this.skyTransparency = skyTransparency;
    }

    @Override
    public void render(WorldRenderContext context) {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack matrices = new PoseStack();
        matrices.mulPose(context.positionMatrix());
        float celestialAngle = context.world().getTimeOfDay(partialTicks);
        var player = Minecraft.getInstance().player;
        double playerY = player != null ? (player.getVehicle() != null ? player.getVehicle().getY() : player.getY()) : 64.0D;

        float effectiveTransparency = this.skyTransparency;
        if (effectiveTransparency < 1.0F && playerY > Constant.ATMOSPHERE_FADE_START) {
            float progress = Mth.clamp(
                    (float) ((playerY - Constant.ATMOSPHERE_FADE_START)
                            / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FADE_START)),
                    0.0F,
                    1.0F
            );
            effectiveTransparency = Mth.lerp(progress, this.skyTransparency, 1.0F);
        }

        if (AstroSkyEngine.isInitialized()) {
            context.profiler().push("astro_moon_parent_sky");
            AstroSkyRenderer.renderSpaceDimensionSky(matrices, context.projectionMatrix(), effectiveTransparency, false);
            if (this.enableAurora && AstroSkyEngine.CONFIG.enableAurora && AstroSkyRenderer.getHorizontalMatrix() != null) {
                AuroraRenderer.render(AstroSkyRenderer.getHorizontalMatrix());
            }
            context.profiler().pop();
        } else {
            context.profiler().push("moon_parent_fallback_sky");
            matrices.pushPose();
            StarManager.rotateForCelestialAngle(matrices, celestialAngle);
            float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
            starBrightness = (starBrightness * starBrightness * 0.5F + 0.3F) * effectiveTransparency;
            this.starManager.render(matrices, context.projectionMatrix(), context.world(), partialTicks, starBrightness);
            matrices.popPose();

            renderFallbackParentPlanet(matrices, effectiveTransparency);
            context.profiler().pop();
        }

        PlanetUnderPlayerRenderer.renderPlanetBelow(
                matrices,
                context.world(),
                this.moonTexture,
                null,
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

    private void renderFallbackParentPlanet(PoseStack matrices, float alpha) {
        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(45.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(-35.0F));
        Matrix4f pose = matrices.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        RenderSystem.setShaderTexture(0, this.parentTexture);

        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float s = this.parentSize;
        buffer.addVertex(pose, -s, 100.0F, -s).setUv(0.0F, 0.0F);
        buffer.addVertex(pose, s, 100.0F, -s).setUv(1.0F, 0.0F);
        buffer.addVertex(pose, s, 100.0F, s).setUv(1.0F, 1.0F);
        buffer.addVertex(pose, -s, 100.0F, s).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        matrices.popPose();
    }
}
