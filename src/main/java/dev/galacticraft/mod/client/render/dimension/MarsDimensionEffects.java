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
 * Dimension effects for Mars, faithfully restoring its authentic thin terracotta atmosphere:
 * subtle reddish-orange horizon atmospheric fog, daylight scattering, and sunset glow.
 */
@Environment(EnvType.CLIENT)
public class MarsDimensionEffects extends GalacticDimensionEffects {
    public static final MarsDimensionEffects INSTANCE = new MarsDimensionEffects();
    private final float[] sunriseColor = new float[4];

    public MarsDimensionEffects() {
        super(Float.NaN, false, SkyType.NORMAL, false, false);
    }

    public MarsDimensionEffects(float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight) {
        super(cloudLevel, hasGround, skyType, forceBrightLightmap, constantAmbientLight);
    }

    @Override
    public Vec3 getFogColor(ClientLevel level, float partialTicks, Vec3 cameraPos, CubicSampler.Vec3Fetcher fetcher) {
        float daylightFactor = 1.0F - getStarBrightness(level, partialTicks);
        return new Vec3(
                (210.0 / 255.0) * daylightFactor,
                (120.0 / 255.0) * daylightFactor,
                (59.0 / 255.0) * daylightFactor
        );
    }

    @Override
    public Vec3 getSkyColor(ClientLevel level, float partialTicks) {
        float daylightFactor = 1.0F - getStarBrightness(level, partialTicks);
        return new Vec3(
                (154.0 / 255.0) * daylightFactor,
                (114.0 / 255.0) * daylightFactor,
                (66.0 / 255.0) * daylightFactor
        );
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int camX, int camY) {
        return false;
    }

    @Override
    public boolean tickRain(ClientLevel level, Camera camera, int ticks) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float skyAngle, float tickDelta) {
        float f = Mth.cos(skyAngle * Mth.TWO_PI);
        if (f >= -0.4F && f <= 0.4F) {
            float f1 = (f - -0.0F) / 0.4F * 0.5F + 0.5F;
            float f2 = 1.0F - (1.0F - Mth.sin(f1 * (float) Math.PI)) * 0.99F;
            f2 *= f2;
            this.sunriseColor[0] = 255.0F / 255.0F;
            this.sunriseColor[1] = 194.0F / 255.0F;
            this.sunriseColor[2] = 180.0F / 255.0F;
            this.sunriseColor[3] = 0.30F * f2;
            return this.sunriseColor;
        }
        return null;
    }
}
