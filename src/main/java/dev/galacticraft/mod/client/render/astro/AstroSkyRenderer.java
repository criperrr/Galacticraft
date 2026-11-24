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

package dev.galacticraft.mod.client.render.astro;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.galacticraft.mod.client.render.astro.aurora.AuroraRenderer;
import dev.galacticraft.mod.client.render.astro.deepsky.DeepskyManager;
import dev.galacticraft.mod.client.render.astro.meteors.MeteorRenderer;
import dev.galacticraft.mod.client.render.astro.overlays.OverlayManager;
import dev.galacticraft.mod.client.render.astro.planets.PlanetManager;
import dev.galacticraft.mod.client.render.astro.planets.PlanetRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class AstroSkyRenderer {
    public static final float DEPTH = 100.0F;
    public static final AstroSkyConfig.SkyRotationOptions options = AstroSkyEngine.CONFIG.skyRotationOptions;

    private static PoseStack.Pose horizontal;
    private static PoseStack.Pose equatorial;
    private static double spyglassAngleLock = Double.NaN;

    public static void calcPositionMatrices(PoseStack matrices) {
        matrices.pushPose();
        horizontal = matrices.last().copy();
        matrices.pushPose();
        makeMatrixEquatorial(matrices);
        equatorial = matrices.last().copy();
        matrices.popPose();
        matrices.popPose();
    }

    public static double getLatitude() {
        Minecraft client = Minecraft.getInstance();
        if (options.globeModeLatitude && client.player != null) {
            double z = client.player.getZ();
            return 360.0 * (z - options.globe.equatorZ) / options.globe.circumference;
        }
        return Math.min(89.99, Math.max(-89.99, (double) options.latitude));
    }

    public static double getLongitude() {
        Minecraft client = Minecraft.getInstance();
        if (options.globeModeLongitude && client.player != null) {
            double x = client.player.getX();
            return 360.0 * (x - options.globe.meridianX) / options.globe.circumference;
        }
        return (double) options.longitude;
    }

    public static double getAxialTilt() {
        return options.axialTilt == 23 ? (double) options.axialTilt + 0.45 : (double) options.axialTilt;
    }

    private static double getSkyAngle() {
        double worldAngle = (double) (AstroSkyEngine.getWorldTime() - 6000L) / 24000.0;
        double liveAngle = (double) System.currentTimeMillis() / 8.64E7 - 0.5;
        double timeAngle = Mth.frac(options.liveMode ? liveAngle : worldAngle);
        if (!options.liveMode) {
            timeAngle = (timeAngle * 2.0 + 0.5 - Math.cos(timeAngle * Math.PI) / 2.0) / 3.0;
        }

        double skyAngle = 360.0 * timeAngle + PlanetManager.getTropicalAngle();
        boolean lockSky = AstroSpyglassManager.isInUse()
                && (!ClientTelescopeController.isActive() || ClientTelescopeController.hasSiderealDrive());
        if (lockSky && !Double.isNaN(spyglassAngleLock)) {
            skyAngle = spyglassAngleLock;
        }

        spyglassAngleLock = skyAngle;
        return skyAngle + getLongitude();
    }

    private static void makeMatrixEquatorial(PoseStack matrices) {
        matrices.mulPose(Axis.XN.rotationDegrees((float) getLatitude()));
        matrices.mulPose(Axis.YP.rotationDegrees(-90.0F));
        matrices.mulPose(Axis.XP.rotationDegrees((float) getSkyAngle()));
        matrices.mulPose(Axis.YN.rotationDegrees(23.45F - (float) getAxialTilt()));
    }

    public static PoseStack.Pose getHorizontalMatrixEntry() {
        return horizontal;
    }

    public static Matrix4f getHorizontalMatrix() {
        return horizontal != null ? horizontal.pose() : null;
    }

    public static PoseStack.Pose getEquatorialMatrixEntry() {
        return equatorial;
    }

    public static Matrix4f getEquatorialMatrix() {
        return equatorial != null ? equatorial.pose() : null;
    }

    public static void renderMain() {
        if (!AstroSkyEngine.isInitialized() || getEquatorialMatrix() == null) {
            return;
        }
        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.colorMask(false, false, false, true);
        AstroRenderUtils.clearColorBuffer();
        RenderSystem.colorMask(true, true, true, true);

        PlanetRenderer.renderAll(getEquatorialMatrix());
        RenderSystem.setShader(AstroRenderUtils.getPositionTexProgram());
        if (AstroSkyEngine.CONFIG.enableDeepsky) {
            DeepskyManager.render(getEquatorialMatrix());
        }

        OverlayManager.render();
        MeteorRenderer.render(getEquatorialMatrix());
        if (AstroSkyEngine.CONFIG.enableAurora && getHorizontalMatrix() != null) {
            AuroraRenderer.render(getHorizontalMatrix());
        }
    }

    /**
     * Renders the complete realistic AstroCraft sky (planets, moons, comets, Milky Way, nebulae,
     * galaxies, real stars, and optional meteors) inside a GC5 space or planetary dimension sky renderer.
     *
     * @param basePoseStack        The camera-space PoseStack (already multiplied by positionMatrix).
     * @param projectionMatrix     The current projection matrix.
     * @param starVisibilityScale  Overall visibility multiplier for stars/deepsky (1.0 in vacuum, lower through dense clouds).
     * @param allowMeteors         Whether atmospheric shooting stars should render (e.g. true on Earth/Mars, false in pure vacuum).
     */
    public static void renderSpaceDimensionSky(PoseStack basePoseStack, Matrix4f projectionMatrix, float starVisibilityScale, boolean allowMeteors) {
        if (!AstroSkyEngine.isInitialized()) {
            return;
        }
        AstroSkyEngine.ensureUpToDate();

        AstroLightingManager.pushSpaceOverride(starVisibilityScale);
        try {
            PoseStack skyStack = new PoseStack();
            skyStack.mulPose(basePoseStack.last().pose());
            calcPositionMatrices(skyStack);

            Matrix4f eqMatrix = getEquatorialMatrix();
            if (eqMatrix == null) {
                return;
            }

            FogRenderer.setupNoFog();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();

            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
            RenderSystem.colorMask(false, false, false, true);
            AstroRenderUtils.clearColorBuffer();
            RenderSystem.colorMask(true, true, true, true);

            // Planets, moons, Sun, rings, eclipses, comet tails
            PlanetRenderer.renderAll(eqMatrix);

            // Milky Way, galaxies, nebulae, globular clusters
            RenderSystem.setShader(AstroRenderUtils.getPositionTexProgram());
            if (AstroSkyEngine.CONFIG.enableDeepsky && starVisibilityScale > 0.01F) {
                DeepskyManager.render(eqMatrix);
            }

            // Overlays (constellations/grids if enabled)
            OverlayManager.render();

            // Atmospheric meteors (only if planet has atmosphere)
            if (allowMeteors && starVisibilityScale > 0.05F) {
                MeteorRenderer.render(eqMatrix);
            }

            // Real star catalog + dynamic stars + live planet point sources
            if (starVisibilityScale > 0.01F) {
                float starVis = Math.max(0.0F, Math.min(1.0F, starVisibilityScale));
                RenderSystem.setShaderColor(starVis, starVis, starVis, starVis);
                AstroStarRenderer.drawAll(eqMatrix, projectionMatrix, GameRenderer.getPositionColorShader());
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        } finally {
            AstroLightingManager.popSpaceOverride();
        }
    }
}
