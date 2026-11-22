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

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class AstroLightingManager {
    private static final AstroCurveConfig VANILLA_CURVE = new AstroCurveConfig(
            List.of(-14.5, 0.0, 14.5),
            List.of(0.015, 0.5, 1.0),
            AstroCurveConfig.Interpolation.LINEAR
    );
    private static final AstroCurveConfig REALISTIC_CURVE = new AstroCurveConfig(
            List.of(-18.0, -12.0, -6.0, 0.0, 30.0, 90.0),
            List.of(0.015, 0.04, 0.15, 0.5, 0.9, 1.0),
            AstroCurveConfig.Interpolation.LINEAR
    );

    public static final LightingConfig options = AstroSkyEngine.CONFIG.lightingOptions;

    private static float totalSkyLight = 0.0F;
    private static float totalDirectLight = 0.0F;
    private static float sunsetAzimuth = 0.0F;
    private static float sunsetAltitude = 0.5F;
    private static double sunsetMagn = -10.0;
    private static boolean hasComputedLighting = false;

    private static float spaceVisibilityOverride = -1.0F;

    public static void pushSpaceOverride(float starVisibility) {
        spaceVisibilityOverride = starVisibility;
    }

    public static void popSpaceOverride() {
        spaceVisibilityOverride = -1.0F;
    }

    public static boolean hasComputedLighting() {
        return hasComputedLighting;
    }

    public static void reset() {
        totalSkyLight = 0.0F;
        totalDirectLight = 0.0F;
        sunsetAzimuth = 0.0F;
        sunsetAltitude = 0.5F;
        sunsetMagn = -10.0;
        hasComputedLighting = true;
    }

    public static void addLight(Matrix4f lightMatrix, double magn, double radius) {
        if (Double.isInfinite(magn) || magn > -4.0) {
            return;
        }
        if (!AstroSkyEngine.CONFIG.enableMoonlight && magn > -18.0) {
            return;
        }
        Matrix4f horizontal = AstroSkyRenderer.getHorizontalMatrix();
        if (horizontal == null) {
            return;
        }
        Vector4f pos = new Vector4f(0.0F, -1.0F, 0.0F, 0.0F).mul(lightMatrix);
        Vector4f up = new Vector4f(0.0F, 1.0F, 0.0F, 0.0F).mul(horizontal);
        double altitude = Math.toDegrees(Math.asin(Mth.clamp(pos.angleCos(up), -1.0F, 1.0F)));
        double linear = Math.pow(2.512, -magn - 26.74);
        double atm = Math.max(1.0E-4, getAtmosphere());
        totalSkyLight = (float) ((double) totalSkyLight + Math.sqrt(atm) * Math.max(0.0, linear * getSkyMultiplier(altitude, radius)));
        totalDirectLight = (float) ((double) totalDirectLight + Math.min(2.0, 1.0 / Math.sqrt(atm)) * Math.max(0.0, linear * getBlockMultiplier(altitude, radius)));
        if (magn < sunsetMagn) {
            Vector4f west = new Vector4f(-1.0F, 0.0F, 0.0F, 0.0F).mul(horizontal);
            sunsetAzimuth = new Vector3f(pos.x, pos.y, pos.z).angleSigned(new Vector3f(west.x, west.y, west.z), new Vector3f(up.x, up.y, up.z));
            sunsetAltitude = (float) altitude / 360.0F + 0.25F;
            sunsetMagn = magn;
        }
    }

    public static double getAtmosphere() {
        if (spaceVisibilityOverride >= 0.0F) {
            return Math.max(0.0, 1.0 - spaceVisibilityOverride);
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return options.surfacePressure;
        }
        double y = client.player.getVehicle() != null ? client.player.getVehicle().getY() : client.player.getY();
        double multiplier = Math.exp(-(y - (double) AstroSkyEngine.CONFIG.spaceStart) / (double) AstroSkyEngine.CONFIG.spaceScale);
        return options.surfacePressure * Math.min(1.0, multiplier);
    }

    private static double getSkyMultiplier(double altitude, double radius) {
        return getSkyMultiplier(Math.min(90.0, altitude + radius));
    }

    private static double getSkyMultiplier(double altitude) {
        AstroCurveConfig curve = options.curveMode == LightingConfig.Mode.REALISTIC ? REALISTIC_CURVE : VANILLA_CURVE;
        return Math.pow(Math.max(0.0, curve.get(altitude)), 1.0 / options.skyLightPower);
    }

    private static double getBlockMultiplier(double altitude, double radius) {
        double altMult = getBlockMultiplier(Math.min(90.0, altitude + radius));
        double aboveHorizon = radius > 0.0 ? Math.min(1.0, Math.max(0.0, (altitude + radius) / radius * 0.5)) : (altitude >= 0.0 ? 1.0 : 0.0);
        return altMult * aboveHorizon;
    }

    private static double getBlockMultiplier(double altitude) {
        return Math.max(0.0, Math.sin(Math.toRadians(altitude)));
    }

    public static float getBlockBrightness() {
        double totalLight = (double) totalDirectLight + (double) totalSkyLight * 0.01 * (double) options.blockSkyLightMultiplier;
        return (float) (options.blockLightMultiplier * Math.pow(Math.max(0.0, totalLight), options.blockLightPower));
    }

    public static float getSkyBrightness() {
        float brightness = (float) (options.skyLightMultiplier * Math.pow(Math.max(0.0, totalSkyLight), options.skyLightPower));
        if (brightness <= 0.0F) {
            return 0.0F;
        }
        if (brightness <= 1.0F) {
            return brightness;
        }
        return options.clampSkyBrightness ? Mth.clamp(brightness, 0.0F, 1.0F) : brightness * 2.0F / (brightness + 1.0F);
    }

    public static float getFogBrightness() {
        float darken = options.darkenFog ? 0.06F : 0.0F;
        return (1.0F + darken) * getSkyBrightness() - darken;
    }

    public static float getSunsetAzimuth() {
        return sunsetAzimuth;
    }

    public static float getSunsetAltitude() {
        return sunsetAltitude;
    }

    private static float getSkyClearness() {
        Minecraft client = Minecraft.getInstance();
        return client.level == null ? 1.0F : 1.0F - client.level.getRainLevel(0.0F);
    }

    public static float getStarVisibility() {
        if (spaceVisibilityOverride >= 0.0F) {
            return Mth.clamp(spaceVisibilityOverride, 0.0F, 1.0F);
        }
        float maximum = 0.01F * (float) AstroSkyEngine.CONFIG.starRenderingOptions.starOpacity;
        float minimum = AstroSkyEngine.CONFIG.daytimeStars ? 0.2F : 0.0F;
        return Mth.clamp(1.0F - Mth.sqrt(getSkyBrightness() * 100.0F / (float) options.starVisibilityLimit), minimum, maximum);
    }

    public static float getDiskVisibility() {
        if (spaceVisibilityOverride >= 0.0F) {
            return 1.0F;
        }
        float lightVisibility = Mth.clamp(1.0F - Mth.sqrt(getSkyBrightness()), 0.5F, 1.0F);
        return lightVisibility * getSkyClearness();
    }

    public static float getDeepskyVisibility() {
        if (spaceVisibilityOverride >= 0.0F) {
            return Mth.clamp(spaceVisibilityOverride, 0.0F, 1.0F);
        }
        float lightVisibility = Mth.clamp(
                1.0F - Mth.sqrt(getSkyBrightness() * 100.0F / (float) options.deepskyVisibilityLimit),
                0.0F,
                1.0F
        );
        return lightVisibility * getSkyClearness();
    }

    public static class LightingConfig {
        public int ambientLight = 5;
        public double blockLightPower = 0.25;
        public double blockLightMultiplier = 1.2;
        public double surfacePressure = 1.0;
        public double skyLightPower = 0.35;
        public double skyLightMultiplier = 1.2;
        public int blockSkyLightMultiplier = 10;
        public double occultationPower = 2.0;
        public Mode curveMode = Mode.REALISTIC;
        public int starVisibilityLimit = 75;
        public int deepskyVisibilityLimit = 10;
        public boolean darkenFog = true;
        public boolean clampSkyBrightness = true;

        public enum Mode {
            VANILLA,
            REALISTIC
        }
    }
}
