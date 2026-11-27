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

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.api.dimension.GalacticDimensionEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Dense amber-orange nitrogen/methane atmospheric haze for Titan (1.45 bar),
 * smoothly clearing to orbital vacuum above {@link Constant#ATMOSPHERE_FADE_START}.
 */
@Environment(EnvType.CLIENT)
public class TitanDimensionEffects extends GalacticDimensionEffects {
    public static final TitanDimensionEffects INSTANCE = new TitanDimensionEffects();

    private static final float DAY_R = 216.0F / 255.0F;
    private static final float DAY_G = 136.0F / 255.0F;
    private static final float DAY_B = 52.0F / 255.0F;

    private static final float NIGHT_R = 72.0F / 255.0F;
    private static final float NIGHT_G = 40.0F / 255.0F;
    private static final float NIGHT_B = 18.0F / 255.0F;

    private TitanDimensionEffects() {
        super(Float.NaN, false, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getFogColor(ClientLevel level, float partialTicks, Vec3 cameraPos, CubicSampler.Vec3Fetcher fetcher) {
        float starBrightness = getStarBrightness(level, partialTicks);
        float day = 1.0F - starBrightness;
        float r = DAY_R * day + NIGHT_R * starBrightness;
        float g = DAY_G * day + NIGHT_G * starBrightness;
        float b = DAY_B * day + NIGHT_B * starBrightness;

        if (cameraPos.y() > Constant.ATMOSPHERE_FADE_START) {
            float progress = Mth.clamp(
                    (float) ((cameraPos.y() - Constant.ATMOSPHERE_FADE_START)
                            / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FADE_START)),
                    0.0F,
                    1.0F
            );
            float alpha = 1.0F - progress * progress * (3.0F - 2.0F * progress);
            return new Vec3(r * alpha, g * alpha, b * alpha);
        }
        return new Vec3(r, g, b);
    }

    @Override
    public Vec3 getSkyColor(ClientLevel level, float partialTicks) {
        float starBrightness = getStarBrightness(level, partialTicks);
        float day = 1.0F - starBrightness;
        return new Vec3(
                DAY_R * day + NIGHT_R * starBrightness,
                DAY_G * day + NIGHT_G * starBrightness,
                DAY_B * day + NIGHT_B * starBrightness
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
        return true;
    }

    @Override
    public float[] getSunriseColor(float skyAngle, float tickDelta) {
        return null;
    }
}
