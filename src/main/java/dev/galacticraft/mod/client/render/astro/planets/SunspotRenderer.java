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

package dev.galacticraft.mod.client.render.astro.planets;

import dev.galacticraft.mod.client.render.astro.AstroRenderUtils;
import dev.galacticraft.mod.client.render.astro.AstroSpyglassManager;
import dev.galacticraft.mod.client.render.astro.AstroStarRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Random;

public class SunspotRenderer {
    private final Random random;
    private final ArrayList<ActiveRegion> activeRegions = new ArrayList<>();

    public SunspotRenderer(long seed) {
        this.random = new Random(seed);
        for (int i = 0; i < 12; i++) {
            this.activeRegions.add(this.genAlpha());
        }
        for (int i = 0; i < 20; i++) {
            this.activeRegions.add(this.genBeta());
        }
        for (int i = 0; i < 6; i++) {
            this.activeRegions.add(this.genGamma());
        }
        for (int i = 0; i < 2; i++) {
            this.activeRegions.add(this.genDelta());
        }
    }

    private ActiveRegion genAlpha() {
        return new ActiveRegion() {
            @Override
            public Iterable<Sunspot> getSunspots() {
                ArrayList<Sunspot> sunspots = new ArrayList<>();
                sunspots.add(new Sunspot((float) SunspotRenderer.this.random.nextGaussian(1.5, 0.25), 0.0F, 0.0F));
                for (int i = 0; i < 5; i++) {
                    sunspots.add(this.genRandomSunspot(0.5F, 0.0F, 0.0F, 1.5F, 1.5F));
                }
                return sunspots;
            }
        };
    }

    private ActiveRegion genBeta() {
        return new ActiveRegion() {
            @Override
            public Iterable<Sunspot> getSunspots() {
                ArrayList<Sunspot> sunspots = new ArrayList<>();
                sunspots.add(new Sunspot((float) SunspotRenderer.this.random.nextGaussian(1.0, 0.2), -2.0F, 1.0F));
                sunspots.add(new Sunspot((float) SunspotRenderer.this.random.nextGaussian(1.0, 0.2), 2.0F, -1.0F));
                for (int i = 0; i < 5; i++) {
                    sunspots.add(this.genRandomSunspot(0.5F, -3.0F, 1.0F, 2.5F, 1.0F));
                    sunspots.add(this.genRandomSunspot(0.5F, 3.0F, -1.0F, 2.5F, 1.0F));
                }
                return sunspots;
            }
        };
    }

    private ActiveRegion genGamma() {
        return new ActiveRegion() {
            @Override
            public Iterable<Sunspot> getSunspots() {
                ArrayList<Sunspot> sunspots = new ArrayList<>();
                sunspots.add(new Sunspot((float) SunspotRenderer.this.random.nextGaussian(1.5, 0.3), -2.0F, 1.0F));
                sunspots.add(new Sunspot((float) SunspotRenderer.this.random.nextGaussian(1.5, 0.3), 2.0F, -1.0F));
                for (int i = 0; i < 25; i++) {
                    sunspots.add(this.genRandomSunspot(1.0F, 0.0F, 0.0F, 4.0F, 1.5F));
                }
                return sunspots;
            }
        };
    }

    private ActiveRegion genDelta() {
        return new ActiveRegion() {
            @Override
            public Iterable<Sunspot> getSunspots() {
                ArrayList<Sunspot> sunspots = new ArrayList<>();
                sunspots.add(new Sunspot((float) SunspotRenderer.this.random.nextGaussian(2.5, 1.0), 0.0F, 0.0F));
                for (int i = 0; i < 35; i++) {
                    sunspots.add(this.genRandomSunspot(1.5F, 0.0F, 0.0F, 5.0F, 2.5F));
                }
                return sunspots;
            }
        };
    }

    public void render(Matrix4f matrix, float diskRadius) {
        float t = (float) (PlanetManager.getPlanetTime() / 22.0 % 1.0);
        AstroRenderUtils.useBlend(true, true, false);
        for (ActiveRegion activeRegion : this.activeRegions) {
            for (ActiveRegion.Sunspot sunspot : activeRegion.getSunspots()) {
                sunspot.render(matrix, diskRadius, t);
            }
        }
    }

    public abstract class ActiveRegion {
        private final float initialLongitude;
        private final boolean hemisphere;
        private final float haleOffset;
        private final ArrayList<Float> periodTerms = new ArrayList<>();
        private final ArrayList<Float> amplitudeTerms = new ArrayList<>();

        public ActiveRegion() {
            this.initialLongitude = 360.0F * SunspotRenderer.this.random.nextFloat();
            this.hemisphere = SunspotRenderer.this.random.nextBoolean();
            this.haleOffset = (float) SunspotRenderer.this.random.nextGaussian(0.0, 0.2);
            float period = 0.5F;
            float amplitudeLeft = 0.5F;
            this.periodTerms.add(period);
            this.amplitudeTerms.add(0.5F);
            for (int i = 0; i < 10; i++) {
                period *= SunspotRenderer.this.random.nextFloat();
                float amplitude = amplitudeLeft * SunspotRenderer.this.random.nextFloat();
                amplitudeLeft -= amplitude;
                this.periodTerms.add(period);
                this.amplitudeTerms.add(amplitude);
            }
        }

        public float getIntensity(float haleTime) {
            float total = -0.2F;
            for (int i = 0; i < this.periodTerms.size(); i++) {
                total -= this.amplitudeTerms.get(i) * Mth.cos((float) (Math.PI * 2.0) * haleTime / this.periodTerms.get(i));
            }
            return total;
        }

        public float getLatitude(float haleTime) {
            haleTime += this.haleOffset;
            float t = (2.0F * haleTime + 10.0F) % 1.0F;
            float cosValue = Mth.cos((float) (Math.PI / 2.0) * t);
            float signValue = (haleTime % 1.0F < 0.5F ^ this.hemisphere) ? 1.0F : -1.0F;
            return (30.0F * cosValue * cosValue + 5.0F) * signValue;
        }

        public float getLongitude(float haleTime) {
            haleTime += this.haleOffset;
            float t = 2.0F * haleTime % 1.0F;
            float period = 14.695F * t + 13.733F * (1.0F - t);
            return this.initialLongitude + 360.0F * t * 4017.75F / period;
        }

        public Sunspot genRandomSunspot(float maxSize, float centerX, float centerY, float spreadX, float spreadY) {
            float size = maxSize * SunspotRenderer.this.random.nextFloat();
            float x = (float) SunspotRenderer.this.random.nextGaussian((double) centerX, (double) spreadX);
            float y = (float) SunspotRenderer.this.random.nextGaussian((double) centerY, (double) spreadY);
            return new Sunspot(size, x, y);
        }

        public abstract Iterable<Sunspot> getSunspots();

        public class Sunspot {
            private final float size;
            private final float x;
            private final float y;

            public Sunspot(float size, float x, float y) {
                this.size = size;
                this.x = x;
                this.y = y;
            }

            public void render(Matrix4f matrix, float diskRadius, float t) {
                float intensity = ActiveRegion.this.getIntensity(t);
                float longitude = (ActiveRegion.this.getLongitude(t) + this.x * intensity) % 360.0F;
                float regionLat = ActiveRegion.this.getLatitude(t);
                float latitude = regionLat + this.y * intensity * Math.signum(regionLat);
                float r = this.size * intensity;
                if (!(r < 0.0F)) {
                    float w = r / 180.0F * (float) Math.PI * diskRadius * Mth.cos(longitude / 180.0F * (float) Math.PI);
                    float h = r / 180.0F * (float) Math.PI * diskRadius * Mth.cos(latitude / 180.0F * (float) Math.PI);
                    if (!((double) w * AstroSpyglassManager.getZoom() < AstroStarRenderer.getResolution() / 180.0 * Math.PI)) {
                        float u = diskRadius * Mth.sin(longitude / 180.0F * (float) Math.PI);
                        float v = diskRadius * Mth.sin(latitude / 180.0F * (float) Math.PI);
                        Matrix4f center = new Matrix4f(matrix).rotateZ(v).rotateX(u);
                        new AstroRenderUtils.BufferHelper(h, w).texture().draw(center);
                    }
                }
            }
        }
    }
}
