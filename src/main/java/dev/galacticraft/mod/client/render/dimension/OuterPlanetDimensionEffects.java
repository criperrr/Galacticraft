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

import dev.galacticraft.mod.api.dimension.GalacticDimensionEffects;
import dev.galacticraft.mod.events.GCGasGiantAtmosphereHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Dimension effects for outer gas and ice giants (Jupiter, Saturn, Uranus, Neptune),
 * capturing their deep, rich atmospheric gas colors, day/night cycles, and smooth transition to orbital space.
 */
@Environment(EnvType.CLIENT)
public class OuterPlanetDimensionEffects extends GalacticDimensionEffects {
    public static final OuterPlanetDimensionEffects JUPITER = new OuterPlanetDimensionEffects(
            Float.NaN,
            215.0F / 255.0F, 128.0F / 255.0F, 62.0F / 255.0F,
            108.0F / 255.0F, 52.0F / 255.0F, 24.0F / 255.0F,
            232.0F / 255.0F, 148.0F / 255.0F, 78.0F / 255.0F,
            84.0F / 255.0F, 40.0F / 255.0F, 22.0F / 255.0F
    );

    public static final OuterPlanetDimensionEffects SATURN = new OuterPlanetDimensionEffects(
            Float.NaN,
            228.0F / 255.0F, 194.0F / 255.0F, 122.0F / 255.0F,
            112.0F / 255.0F, 88.0F / 255.0F, 48.0F / 255.0F,
            242.0F / 255.0F, 212.0F / 255.0F, 144.0F / 255.0F,
            88.0F / 255.0F, 68.0F / 255.0F, 38.0F / 255.0F
    );

    public static final OuterPlanetDimensionEffects URANUS = new OuterPlanetDimensionEffects(
            Float.NaN,
            92.0F / 255.0F, 198.0F / 255.0F, 210.0F / 255.0F,
            34.0F / 255.0F, 92.0F / 255.0F, 108.0F / 255.0F,
            116.0F / 255.0F, 218.0F / 255.0F, 228.0F / 255.0F,
            28.0F / 255.0F, 76.0F / 255.0F, 92.0F / 255.0F
    );

    public static final OuterPlanetDimensionEffects NEPTUNE = new OuterPlanetDimensionEffects(
            Float.NaN,
            52.0F / 255.0F, 104.0F / 255.0F, 212.0F / 255.0F,
            20.0F / 255.0F, 44.0F / 255.0F, 112.0F / 255.0F,
            66.0F / 255.0F, 124.0F / 255.0F, 234.0F / 255.0F,
            20.0F / 255.0F, 44.0F / 255.0F, 118.0F / 255.0F
    );

    private final float dayFogR, dayFogG, dayFogB;
    private final float nightFogR, nightFogG, nightFogB;
    private final float daySkyR, daySkyG, daySkyB;
    private final float nightSkyR, nightSkyG, nightSkyB;

    public OuterPlanetDimensionEffects(
            float cloudLevel,
            float dayFogR, float dayFogG, float dayFogB,
            float nightFogR, float nightFogG, float nightFogB,
            float daySkyR, float daySkyG, float daySkyB,
            float nightSkyR, float nightSkyG, float nightSkyB
    ) {
        super(cloudLevel, false, SkyType.NORMAL, false, false);
        this.dayFogR = dayFogR;
        this.dayFogG = dayFogG;
        this.dayFogB = dayFogB;
        this.nightFogR = nightFogR;
        this.nightFogG = nightFogG;
        this.nightFogB = nightFogB;
        this.daySkyR = daySkyR;
        this.daySkyG = daySkyG;
        this.daySkyB = daySkyB;
        this.nightSkyR = nightSkyR;
        this.nightSkyG = nightSkyG;
        this.nightSkyB = nightSkyB;
    }

    @Override
    public Vec3 getFogColor(ClientLevel level, float partialTicks, Vec3 cameraPos, CubicSampler.Vec3Fetcher fetcher) {
        float starBrightness = getStarBrightness(level, partialTicks);
        float day = 1.0F - starBrightness;
        float night = starBrightness;

        // Beer-Lambert optical sunlight extinction with rich deep-atmosphere colored scatter floor (never pitch-black void)
        float transmittance = GCGasGiantAtmosphereHandler.computeOpticalTransmittance(level.dimension(), cameraPos.y());
        float effectiveDay = day * (0.14F + 0.86F * transmittance);
        float effectiveNight = night * (0.48F + 0.52F * transmittance) + day * (1.0F - transmittance) * 0.58F;
        float r = this.dayFogR * effectiveDay + this.nightFogR * effectiveNight;
        float g = this.dayFogG * effectiveDay + this.nightFogG * effectiveNight;
        float b = this.dayFogB * effectiveDay + this.nightFogB * effectiveNight;

        if (cameraPos.y() > dev.galacticraft.mod.Constant.ATMOSPHERE_FADE_START) {
            float spaceProgress = Mth.clamp(
                    (float) ((cameraPos.y() - dev.galacticraft.mod.Constant.ATMOSPHERE_FADE_START)
                            / (double) (dev.galacticraft.mod.Constant.ATMOSPHERE_EXIT_END - dev.galacticraft.mod.Constant.ATMOSPHERE_FADE_START)),
                    0.0F,
                    1.0F
            );
            float atmosphereAlpha = 1.0F - spaceProgress * spaceProgress * (3.0F - 2.0F * spaceProgress);
            return new Vec3(r * atmosphereAlpha, g * atmosphereAlpha, b * atmosphereAlpha);
        }

        return new Vec3(r, g, b);
    }

    @Override
    public Vec3 getSkyColor(ClientLevel level, float partialTicks) {
        float starBrightness = getStarBrightness(level, partialTicks);
        float day = 1.0F - starBrightness;
        float night = starBrightness;
        var player = Minecraft.getInstance().player;
        double playerY = player != null ? (player.getVehicle() != null ? player.getVehicle().getY() : player.getY()) : GCGasGiantAtmosphereHandler.AEROSTATIC_REFERENCE_Y;
        float transmittance = GCGasGiantAtmosphereHandler.computeOpticalTransmittance(level.dimension(), playerY);
        float effectiveDay = day * (0.14F + 0.86F * transmittance);
        float effectiveNight = night * (0.48F + 0.52F * transmittance) + day * (1.0F - transmittance) * 0.58F;
        return new Vec3(
                this.daySkyR * effectiveDay + this.nightSkyR * effectiveNight,
                this.daySkyG * effectiveDay + this.nightSkyG * effectiveNight,
                this.daySkyB * effectiveDay + this.nightSkyB * effectiveNight
        );
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean tickRain(ClientLevel level, Camera camera, int ticks) {
        return false;
    }

    @Override
    public boolean isFoggyAt(int camX, int camY) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float skyAngle, float tickDelta) {
        return null;
    }
}
