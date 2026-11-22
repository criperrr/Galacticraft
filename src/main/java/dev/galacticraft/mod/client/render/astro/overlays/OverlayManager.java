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

package dev.galacticraft.mod.client.render.astro.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroRenderUtils;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import net.minecraft.server.packs.resources.ResourceManager;

public class OverlayManager {
    private static ConstellationOverlay constellations;
    private static GridOverlay azimuthal;
    private static GridOverlay equatorial;
    private static GridOverlay ecliptic;
    private static GridOverlay galactic;

    public static void load(ResourceManager manager) {
        constellations = new ConstellationOverlay(manager, Constant.id("astro/overlays/constellations.json"));
        azimuthal = new GridOverlay(AstroSkyEngine.CONFIG.azimuthalGridOptions, GridOverlay::getAzimuthalMatrix);
        equatorial = new GridOverlay(AstroSkyEngine.CONFIG.equatorialGridOptions, GridOverlay::getEquatorialMatrix);
        ecliptic = new GridOverlay(AstroSkyEngine.CONFIG.eclipticGridOptions, GridOverlay::getEclipticMatrix);
        galactic = new GridOverlay(AstroSkyEngine.CONFIG.galacticGridOptions, GridOverlay::getGalacticMatrix);
    }

    public static void render() {
        boolean anyActive = AstroSkyEngine.CONFIG.showConstellations
                || AstroSkyEngine.CONFIG.azimuthalGridOptions.showCircle || AstroSkyEngine.CONFIG.azimuthalGridOptions.showGrid
                || AstroSkyEngine.CONFIG.equatorialGridOptions.showCircle || AstroSkyEngine.CONFIG.equatorialGridOptions.showGrid
                || AstroSkyEngine.CONFIG.eclipticGridOptions.showCircle || AstroSkyEngine.CONFIG.eclipticGridOptions.showGrid
                || AstroSkyEngine.CONFIG.galacticGridOptions.showCircle || AstroSkyEngine.CONFIG.galacticGridOptions.showGrid;
        if (!anyActive) {
            return;
        }

        BufferBuilder bufferBuilder = AstroRenderUtils.setupBuffer(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.setShader(AstroRenderUtils.getPositionColorProgram());
        float v = AstroSkyEngine.getVisibility();
        RenderSystem.setShaderColor(v, v, v, 1.0F);
        AstroRenderUtils.useBlend(false, false);

        if (constellations != null && AstroSkyEngine.CONFIG.showConstellations) {
            constellations.render(bufferBuilder);
        }
        if (azimuthal != null) {
            azimuthal.render(bufferBuilder);
        }
        if (equatorial != null) {
            equatorial.render(bufferBuilder);
        }
        if (ecliptic != null) {
            ecliptic.render(bufferBuilder);
        }
        if (galactic != null) {
            galactic.render(bufferBuilder);
        }

        MeshData built = bufferBuilder.build();
        if (built != null) {
            BufferUploader.drawWithShader(built);
        }
    }
}
