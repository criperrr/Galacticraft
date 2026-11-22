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
import dev.galacticraft.mod.client.render.astro.AstroRenderUtils;
import dev.galacticraft.mod.client.render.astro.AstroStarRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

public class Globular extends DeepskyObject {
    public double bvindex;
    @JsonAdapter(DSOLoader.AngleDeserializer.class)
    public double hmradius;
    private Vector3f color;

    @Override
    public boolean process(ResourceManager manager) {
        super.process(manager);
        this.color = AstroColorUtils.fromIndex(this.bvindex);
        return true;
    }

    @Override
    protected ResourceLocation getDefaultTexture() {
        return Constant.id("astro/deepsky/globular");
    }

    @Override
    public double getArea() {
        return 4.0 * this.hmradius * this.hmradius;
    }

    @Override
    public Vector3f getColor() {
        return this.color;
    }

    @Override
    public void render(Matrix4f matrix, float scale, double bonus) {
        this.setupRender((double) scale, bonus);
        float radius = 4.0F * (float) this.hmradius;
        new AstroRenderUtils.BufferHelper((float) (Math.PI / 180.0) * radius).texture().draw(this.genMatrix(matrix));
    }

    @Override
    public Iterator<AstroStarRenderer.Star> iterator() {
        return this.populatefrom < 0.0 ? Collections.emptyIterator() : new StarGenerator(this.populatefrom);
    }

    private class StarGenerator implements Iterator<AstroStarRenderer.Star> {
        private static final double BASE_MAGNITUDE = 4.0;
        private final Random rand = new Random(Globular.this.id != null ? Globular.this.id.hashCode() : 0L);
        private int starIndex;

        public StarGenerator(double brightestMagnitude) {
            this.starIndex = (int) Math.pow(10.0, 0.4 * (brightestMagnitude - Globular.this.magnitude - BASE_MAGNITUDE));
        }

        @Override
        public boolean hasNext() {
            return this.starIndex < 10000;
        }

        @Override
        public AstroStarRenderer.Star next() {
            double u = this.rand.nextDouble();
            double a = (Math.PI * 2.0) * this.rand.nextDouble();
            double radius = Globular.this.hmradius * Math.sqrt(u / (1.0 - u));
            double de = Globular.this.declination + radius * Math.cos(a);
            double ra = Globular.this.right_ascension + radius * Math.sin(a) / Math.cos(Math.toRadians(de));
            Vector3f pos = AstroStarRenderer.getPosition(ra, de);
            Vector3f starColor = AstroColorUtils.fromIndex(Globular.this.bvindex + Math.abs(this.rand.nextGaussian() * 0.75));
            double magn = Globular.this.magnitude + 2.5 * Math.log10((double) this.starIndex + 1.0) + BASE_MAGNITUDE;
            this.starIndex++;
            return new AstroStarRenderer.Star(pos, starColor, (float) magn, 0.0F);
        }
    }
}
