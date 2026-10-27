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
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.CubicSampler;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class OuterPlanetDimensionEffects extends GalacticDimensionEffects {
    public static final OuterPlanetDimensionEffects JUPITER = new OuterPlanetDimensionEffects(
            230.0F,
            212.0F / 255.0F, 122.0F / 255.0F, 68.0F / 255.0F,
            45.0F / 255.0F, 24.0F / 255.0F, 14.0F / 255.0F,
            190.0F / 255.0F, 100.0F / 255.0F, 52.0F / 255.0F,
            25.0F / 255.0F, 12.0F / 255.0F, 7.0F / 255.0F
    );

    public static final OuterPlanetDimensionEffects SATURN = new OuterPlanetDimensionEffects(
            235.0F,
            230.0F / 255.0F, 198.0F / 255.0F, 132.0F / 255.0F,
            50.0F / 255.0F, 42.0F / 255.0F, 26.0F / 255.0F,
            205.0F / 255.0F, 175.0F / 255.0F, 110.0F / 255.0F,
            28.0F / 255.0F, 22.0F / 255.0F, 12.0F / 255.0F
    );

    public static final OuterPlanetDimensionEffects URANUS = new OuterPlanetDimensionEffects(
            245.0F,
            115.0F / 255.0F, 214.0F / 255.0F, 226.0F / 255.0F,
            20.0F / 255.0F, 45.0F / 255.0F, 52.0F / 255.0F,
            82.0F / 255.0F, 178.0F / 255.0F, 196.0F / 255.0F,
            10.0F / 255.0F, 24.0F / 255.0F, 30.0F / 255.0F
    );

    public static final OuterPlanetDimensionEffects NEPTUNE = new OuterPlanetDimensionEffects(
            245.0F,
            40.0F / 255.0F, 92.0F / 255.0F, 212.0F / 255.0F,
            8.0F / 255.0F, 18.0F / 255.0F, 48.0F / 255.0F,
            24.0F / 255.0F, 64.0F / 255.0F, 168.0F / 255.0F,
            4.0F / 255.0F, 10.0F / 255.0F, 28.0F / 255.0F
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
        super(cloudLevel, true, SkyType.NORMAL, true, true);
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
        float night = getStarBrightness(level, 1.0F);
        float day = 1.0F - night;
        return new Vec3(
                this.dayFogR * day + this.nightFogR * night,
                this.dayFogG * day + this.nightFogG * night,
                this.dayFogB * day + this.nightFogB * night
        );
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public Vec3 getSkyColor(ClientLevel level, float partialTicks) {
        float night = getStarBrightness(level, 1.0F);
        float day = 1.0F - night;
        return new Vec3(
                this.daySkyR * day + this.nightSkyR * night,
                this.daySkyG * day + this.nightSkyG * night,
                this.daySkyB * day + this.nightSkyB * night
        );
    }

    @Override
    public boolean tickRain(@NotNull ClientLevel level, Camera camera, int ticks) {
        return false;
    }
}
