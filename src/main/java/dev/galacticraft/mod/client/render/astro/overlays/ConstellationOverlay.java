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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroSkyRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ConstellationOverlay {
    private List<Constellation> constellations = List.of();

    public ConstellationOverlay(ResourceManager manager, ResourceLocation id) {
        try (InputStream stream = manager.open(id);
             JsonReader reader = new JsonReader(new InputStreamReader(stream))) {
            Gson gson = new Gson();
            List<Constellation> parsed = gson.fromJson(reader, new TypeToken<List<Constellation>>() {}.getType());
            if (parsed != null) {
                this.constellations = parsed;
            }
        } catch (Exception e) {
            Constant.LOGGER.warn("[AstroSky] Failed to load constellations overlay {}", id, e);
        }
    }

    public void render(BufferBuilder bufferBuilder) {
        Matrix4f matrix = AstroSkyRenderer.getEquatorialMatrix();
        if (matrix == null) {
            return;
        }
        for (Constellation constellation : this.constellations) {
            constellation.render(bufferBuilder, matrix);
        }
    }

    private static class Constellation {
        public String name;
        public String type;
        public ArrayList<SkyLine> lines;

        public void render(BufferBuilder bufferBuilder, Matrix4f matrix) {
            if (this.lines == null) {
                return;
            }
            Vector3f color = "zodiac".equals(this.type) ? new Vector3f(0.2F, 0.15F, 0.12F) : new Vector3f(0.12F, 0.15F, 0.12F);
            Vector4f colorAlpha = new Vector4f(color.x, color.y, color.z, 1.0F);
            for (SkyLine line : this.lines) {
                line.render(bufferBuilder, matrix, colorAlpha);
            }
        }
    }
}
