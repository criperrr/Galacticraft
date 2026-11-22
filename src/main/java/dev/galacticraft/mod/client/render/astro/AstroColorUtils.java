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

import org.joml.Vector3f;

import java.util.HashMap;

public class AstroColorUtils {
    public static final Vector3f WHITE = new Vector3f(1.0F, 1.0F, 1.0F);
    private static final HashMap<Integer, Vector3f> TEMPERATURE_CACHE = new HashMap<>();

    public static Vector3f fromHex(int hex) {
        float red = (float) ((hex & 0xFF0000) >> 16) / 255.0F;
        float green = (float) ((hex & 0xFF00) >> 8) / 255.0F;
        float blue = (float) (hex & 0xFF) / 255.0F;
        return new Vector3f(red, green, blue);
    }

    public static int toHex(Vector3f color) {
        int red = (int) (255.0F * color.x);
        int green = (int) (255.0F * color.y);
        int blue = (int) (255.0F * color.z);
        return (red << 16) + (green << 8) + blue;
    }

    public static Vector3f fromTemperature(double tempK) {
        int cacheIndex = (int) Math.round(tempK / 100.0);
        Vector3f cached = TEMPERATURE_CACHE.get(cacheIndex);
        if (cached != null) {
            return cached;
        }
        float red = 1.0F;
        float green = 1.0F;
        float blue = 1.0F;
        double t = Math.max(1.0, tempK / 100.0);
        if (t > 66.0) {
            red = (float) AstroMath.clamp(329.698727446 * AstroMath.pow(t - 60.0, -0.13320247592) / 256.0, 0.0, 1.0);
            green = (float) AstroMath.clamp(288.1221695283 * AstroMath.pow(t - 60.0, -0.0755148492) / 256.0, 0.0, 1.0);
        } else {
            green = (float) AstroMath.clamp((99.4708025861 * Math.log(t) - 161.1195681661) / 256.0, 0.0, 1.0);
            if (t < 19.0) {
                blue = 0.0F;
            } else {
                blue = (float) AstroMath.clamp((138.5177312231 * Math.log(t - 10.0) - 305.0447927307) / 256.0, 0.0, 1.0);
            }
        }
        Vector3f color = new Vector3f(red, green, blue);
        TEMPERATURE_CACHE.put(cacheIndex, color);
        return color;
    }

    public static double temperatureFromIndex(double bvi) {
        return 4600.0 * (1.0 / (0.92 * bvi + 1.7) + 1.0 / (0.92 * bvi + 0.62));
    }

    public static Vector3f fromIndex(double bvi) {
        return fromTemperature(temperatureFromIndex(bvi));
    }

    public static Vector3f fromRelativeAlbedo(Vector3f norm, double albedo) {
        double normLum = getLuminosity(norm);
        double mult = Math.pow(albedo, 0.45454545454545453) / normLum;
        return new Vector3f(norm).mul((float) mult);
    }

    public static int fromRelativeAlbedo(int hex, double albedo) {
        return toHex(ceil(fromRelativeAlbedo(fromHex(hex), albedo)));
    }

    public static Vector3f normalize(Vector3f color) {
        float max = color.get(color.maxComponent());
        if (max <= 0.0F) {
            return new Vector3f(1.0F, 1.0F, 1.0F);
        }
        return new Vector3f(color).div(max);
    }

    public static Vector3f ceil(Vector3f color) {
        float max = color.get(color.maxComponent());
        return max > 1.0F ? new Vector3f(color).div(max) : color;
    }

    private static double getLuminosity(Vector3f color) {
        return (double) color.x * 0.298702 + (double) color.y * 0.518469 + (double) color.z * 0.182829;
    }

    public static double getRelativeAlbedo(Vector3f color) {
        return Math.pow(getLuminosity(color), 2.2);
    }
}
