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

import com.google.gson.annotations.JsonAdapter;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroColorUtils;
import dev.galacticraft.mod.client.render.astro.AstroStarRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

public class CompactStarFile extends DeepskyObject {
    @JsonAdapter(ResourceLocation.Serializer.class)
    public ResourceLocation data;
    public double magnitudeLimit;
    public double[] color_palette;
    public long scatter_seed;
    public double right_ascension_scatter;
    public double declination_scatter;
    public double magnitude_scatter;
    final ArrayList<AstroStarRenderer.Star> stars = new ArrayList<>();

    @Override
    public double getArea() {
        return 64800.0;
    }

    private int loadLEB128(DataInputStream input) throws IOException {
        int byteValue = input.readUnsignedByte();
        return byteValue >= 128 ? byteValue - 128 + this.loadLEB128(input) * 128 : byteValue;
    }

    private void loadCompactStars(InputStream stream) {
        try (DataInputStream input = new DataInputStream(stream)) {
            RandomSource rand = RandomSource.create(this.scatter_seed);
            int morton = 0;

            while (true) {
                int app = input.readUnsignedByte();
                int mc = app % 32;
                int cc = app / 32;
                Vector3f color = AstroColorUtils.fromIndex(this.color_palette[cc]);
                float magn = (float) ((double) mc / 32.0 * (this.magnitudeLimit - this.magnitude) + this.magnitude + rand.nextDouble() * this.magnitude_scatter);
                int delta = this.loadLEB128(input);
                morton += delta;
                int px = 0;
                int py = 0;

                for (int i = 0; i < 14; i++) {
                    py += (morton & (1 << (i * 2))) >> i;
                    px += (morton & (1 << (i * 2 + 1))) >> (i + 1);
                }

                double rightAscension = (double) px / 16384.0 * 360.0 + rand.nextDouble() * this.right_ascension_scatter;
                double declination = (double) py / 16384.0 * 180.0 - 90.0 + rand.nextDouble() * this.declination_scatter;
                Vector3f pos = AstroStarRenderer.getPosition(rightAscension, declination);
                this.stars.add(new AstroStarRenderer.Star(pos, color, magn, 0.0F));
            }
        } catch (EOFException ignored) {
        } catch (Exception e) {
            Constant.LOGGER.error("[AstroSky] Failed to load compact star file {}", this.data, e);
            this.stars.clear();
        }

        this.stars.sort(Comparator.comparingDouble(star -> (double) star.magn()));
    }

    @Override
    public boolean process(ResourceManager manager) {
        try {
            InputStream stream = manager.open(this.data.withPrefix("astro/background/").withSuffix(".dat"));
            this.loadCompactStars(stream);
            return true;
        } catch (Exception e) {
            Constant.LOGGER.error("[AstroSky] Failed to open compact star file {}", this.data, e);
            return false;
        }
    }

    @Override
    public void render(Matrix4f matrix, float scale, double bonus) {
    }

    @Override
    public Iterator<AstroStarRenderer.Star> iterator() {
        return this.stars.iterator();
    }
}
