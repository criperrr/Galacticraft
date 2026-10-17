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

public class MarsDimensionEffects extends GalacticDimensionEffects {
    public static final MarsDimensionEffects INSTANCE = new MarsDimensionEffects(Float.NaN, true, SkyType.NORMAL, true, true);

    public MarsDimensionEffects(float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight) {
        super(cloudLevel, hasGround, skyType, forceBrightLightmap, constantAmbientLight);
    }

    @Override
    public Vec3 getFogColor(ClientLevel level, float partialTicks, Vec3 cameraPos, CubicSampler.Vec3Fetcher fetcher) {
        float night = getStarBrightness(level, 1.0F);
        float day = 1.0F - night;
        // GC4 WorldProviderMars fog RGB (210, 120, 59) during day, darker reddish-brown at night
        float dayColR = 210.0F / 255.0F;
        float dayColG = 120.0F / 255.0F;
        float dayColB = 59.0F / 255.0F;
        float nightColR = 42.0F / 255.0F;
        float nightColG = 24.0F / 255.0F;
        float nightColB = 12.0F / 255.0F;
        return new Vec3(dayColR * day + nightColR * night, dayColG * day + nightColG * night, dayColB * day + nightColB * night);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public Vec3 getSkyColor(ClientLevel level, float partialTicks) {
        float night = getStarBrightness(level, 1.0F);
        float day = 1.0F - night;
        // GC4 WorldProviderMars sky RGB (154, 114, 66)
        float dayColR = 154.0F / 255.0F;
        float dayColG = 114.0F / 255.0F;
        float dayColB = 66.0F / 255.0F;
        float nightColR = 15.0F / 255.0F;
        float nightColG = 11.0F / 255.0F;
        float nightColB = 6.0F / 255.0F;
        return new Vec3(dayColR * day + nightColR * night, dayColG * day + nightColG * night, dayColB * day + nightColB * night);
    }

    @Override
    public boolean tickRain(@NotNull ClientLevel level, Camera camera, int ticks) {
        return false;
    }
}
