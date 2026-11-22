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

package dev.galacticraft.mod.client.render.astro.meteors;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.galacticraft.mod.client.render.astro.AstroMath;
import dev.galacticraft.mod.client.render.astro.AstroRenderUtils;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.List;

public class MeteorRenderer {
    public static double getTime() {
        return (double) AstroSkyEngine.getWorldTime() + AstroMath.mod(AstroSkyEngine.getSolarHourAngle() / 360.0 * 24000.0, 1.0);
    }

    public static void render(Matrix4f equatorialMatrix) {
        if (!AstroSkyEngine.CONFIG.enableMeteors) {
            return;
        }
        double time = getTime();
        List<Meteor> active = MeteorGenerator.getActive(time);
        if (active.isEmpty()) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        AstroRenderUtils.useBlend(false, false);

        // Bright bolide meteors (triangles)
        BufferBuilder triBuffer = AstroRenderUtils.setupBuffer(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (Meteor meteor : active) {
            meteor.renderTriangle(triBuffer, equatorialMatrix, time);
        }
        MeshData triMesh = triBuffer.build();
        if (triMesh != null) {
            BufferUploader.drawWithShader(triMesh);
        }

        // Standard shooting stars (lines)
        BufferBuilder lineBuffer = AstroRenderUtils.setupBuffer(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (Meteor meteor : active) {
            meteor.renderLine(lineBuffer, equatorialMatrix, time);
        }
        MeshData lineMesh = lineBuffer.build();
        if (lineMesh != null) {
            BufferUploader.drawWithShader(lineMesh);
        }
    }
}
