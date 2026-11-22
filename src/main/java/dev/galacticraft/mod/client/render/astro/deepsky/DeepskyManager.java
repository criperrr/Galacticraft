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

package dev.galacticraft.mod.client.render.astro.deepsky;

import dev.galacticraft.mod.client.render.astro.AstroLightingManager;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroSpyglassManager;
import dev.galacticraft.mod.client.render.astro.AstroStarRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;

import java.util.ArrayList;

public class DeepskyManager {
    private static Iterable<DeepskyObject> objects = new ArrayList<>();
    private static float baseBrightness = 0.0F;

    public static void loadBackground(ResourceManager manager, ResourceLocation backgroundID) {
        objects = DSOLoader.loadBackground(manager, backgroundID);
    }

    public static float useTextureBrightness(double magnitude, double area) {
        double surfaceFlux = area > 0.0 ? Math.pow(10.0, -0.4 * magnitude) / area : 0.0;
        return baseBrightness * (float) Math.cbrt(surfaceFlux);
    }

    public static Iterable<DeepskyObject> getObjects() {
        return objects;
    }

    public static void render(Matrix4f matrix) {
        baseBrightness = 0.02F
                * (float) AstroSkyEngine.CONFIG.deepskyBrightness
                * (float) AstroSkyEngine.CONFIG.magnitudeLimit
                / 10.0F
                * AstroLightingManager.getDeepskyVisibility();
        if (baseBrightness <= 1.0E-5F) {
            return;
        }
        double bonus = AstroSpyglassManager.getMagnitudeBonus();
        float scale = (float) AstroSpyglassManager.getZoom();

        for (DeepskyObject dso : getObjects()) {
            if (dso.getArea() * (double) scale * (double) scale >= AstroStarRenderer.getResolution() * AstroStarRenderer.getResolution()) {
                dso.render(matrix, scale, bonus);
            }
        }
    }
}
