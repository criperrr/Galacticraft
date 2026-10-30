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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
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
            185.0F / 255.0F, 95.0F / 255.0F, 35.0F / 255.0F,
            60.0F / 255.0F, 25.0F / 255.0F, 10.0F / 255.0F,
            215.0F / 255.0F, 125.0F / 255.0F, 55.0F / 255.0F,
            40.0F / 255.0F, 15.0F / 255.0F, 8.0F / 255.0F
    );

    public static final OuterPlanetDimensionEffects SATURN = new OuterPlanetDimensionEffects(
            Float.NaN,
            210.0F / 255.0F, 175.0F / 255.0F, 105.0F / 255.0F,
            75.0F / 255.0F, 55.0F / 255.0F, 30.0F / 255.0F,
            230.0F / 255.0F, 195.0F / 255.0F, 125.0F / 255.0F,
            50.0F / 255.0F, 38.0F / 255.0F, 20.0F / 255.0F
    );

    public static final OuterPlanetDimensionEffects URANUS = new OuterPlanetDimensionEffects(
            Float.NaN,
            75.0F / 255.0F, 175.0F / 255.0F, 185.0F / 255.0F,
            18.0F / 255.0F, 50.0F / 255.0F, 60.0F / 255.0F,
            95.0F / 255.0F, 200.0F / 255.0F, 210.0F / 255.0F,
            15.0F / 255.0F, 40.0F / 255.0F, 48.0F / 255.0F
    );

    public static final OuterPlanetDimensionEffects NEPTUNE = new OuterPlanetDimensionEffects(
            Float.NaN,
            35.0F / 255.0F, 75.0F / 255.0F, 175.0F / 255.0F,
            8.0F / 255.0F, 20.0F / 255.0F, 55.0F / 255.0F,
            45.0F / 255.0F, 95.0F / 255.0F, 210.0F / 255.0F,
            10.0F / 255.0F, 22.0F / 255.0F, 65.0F / 255.0F
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
        float r = this.dayFogR * day + this.nightFogR * night;
        float g = this.dayFogG * day + this.nightFogG * night;
        float b = this.dayFogB * day + this.nightFogB * night;

        if (cameraPos.y() > 200.0D) {
            float spaceProgress = Mth.clamp((float) ((cameraPos.y() - 200.0D) / 250.0D), 0.0F, 1.0F);
            float atmosphereAlpha = 1.0F - spaceProgress;
            return new Vec3(r * atmosphereAlpha, g * atmosphereAlpha, b * atmosphereAlpha);
        }

        return new Vec3(r, g, b);
    }

    @Override
    public Vec3 getSkyColor(ClientLevel level, float partialTicks) {
        float starBrightness = getStarBrightness(level, partialTicks);
        float day = 1.0F - starBrightness;
        float night = starBrightness;
        return new Vec3(
                this.daySkyR * day + this.nightSkyR * night,
                this.daySkyG * day + this.nightSkyG * night,
                this.daySkyB * day + this.nightSkyB * night
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
