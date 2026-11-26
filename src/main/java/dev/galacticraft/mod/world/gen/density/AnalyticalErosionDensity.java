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

package dev.galacticraft.mod.world.gen.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * O(1) analytical erosion combining Swiss/Jordan Turbulence (ridged octaves weighted by accumulated
 * slope/height) with 2D Phacelle Noise (per-cell tangent-aligned cosine kernels with slope fade)
 * to carve dendritic dry Martian gullies without hydraulic iteration or parallel striping.
 * Always wrap in {@code DensityFunctions.cache2d(...)} in the router DAG.
 */
public record AnalyticalErosionDensity(
        double scale,
        int octaves,
        double gullyStrength,
        double slopeFadeMin,
        double slopeFadeMax
) implements DensityFunction.SimpleFunction {

    public static final MapCodec<AnalyticalErosionDensity> CODEC_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("scale").forGetter(AnalyticalErosionDensity::scale),
            Codec.INT.fieldOf("octaves").forGetter(AnalyticalErosionDensity::octaves),
            Codec.DOUBLE.fieldOf("gully_strength").forGetter(AnalyticalErosionDensity::gullyStrength),
            Codec.DOUBLE.fieldOf("slope_fade_min").forGetter(AnalyticalErosionDensity::slopeFadeMin),
            Codec.DOUBLE.fieldOf("slope_fade_max").forGetter(AnalyticalErosionDensity::slopeFadeMax)
    ).apply(instance, AnalyticalErosionDensity::new));

    public static final KeyDispatchDataCodec<AnalyticalErosionDensity> CODEC = KeyDispatchDataCodec.of(CODEC_MAP);

    @Override
    public double compute(FunctionContext ctx) {
        double px = ctx.blockX() * this.scale;
        double pz = ctx.blockZ() * this.scale;

        double height = 0.0;
        double gx = 0.0;
        double gz = 0.0;
        double amp = 0.50;
        double freq = 1.00;
        double weight = 1.00;
        int clampedOctaves = Math.max(1, Math.min(4, this.octaves));

        for (int i = 0; i < clampedOctaves; i++) {
            double sx = px * freq;
            double sz = pz * freq;
            int x0 = fastFloor(sx);
            int z0 = fastFloor(sz);
            double fx = sx - x0;
            double fz = sz - z0;

            double ux = fx * fx * fx * (fx * (fx * 6.0 - 15.0) + 10.0);
            double uz = fz * fz * fz * (fz * (fz * 6.0 - 15.0) + 10.0);
            double dux = 30.0 * fx * fx * (fx - 1.0) * (fx - 1.0);
            double duz = 30.0 * fz * fz * (fz - 1.0) * (fz - 1.0);

            double v00 = valueAt(x0, z0, i);
            double v10 = valueAt(x0 + 1, z0, i);
            double v01 = valueAt(x0, z0 + 1, i);
            double v11 = valueAt(x0 + 1, z0 + 1, i);

            double k0 = v00;
            double k1 = v10 - v00;
            double k2 = v01 - v00;
            double k3 = v00 - v10 - v01 + v11;

            double n = k0 + k1 * ux + k2 * uz + k3 * ux * uz;
            double dnx = dux * (k1 + k3 * uz);
            double dnz = duz * (k2 + k3 * ux);

            double ridge = 1.0 - Math.abs(n);
            double sign = n >= 0.0 ? -1.0 : 1.0;
            double sharp = ridge * ridge;
            double dSharp = 2.0 * ridge * sign;

            height += sharp * amp * weight;
            gx += dnx * dSharp * freq * amp * weight;
            gz += dnz * dSharp * freq * amp * weight;

            weight = Math.max(0.0, Math.min(1.0, sharp * 1.80));
            freq *= 2.05;
            amp *= 0.48;
        }

        double slope = Math.hypot(gx, gz);
        double fade = smoothstep(this.slopeFadeMin, this.slopeFadeMax, slope);
        if (fade > 0.001 && this.gullyStrength > 0.0) {
            double invS = 1.0 / (slope + 1e-6);
            double tx = -gz * invS;
            double tz = gx * invS;
            double phacelle = samplePhacelle2D(px * 3.20, pz * 3.20, tx, tz, 4.20);
            double gully = (1.0 - Math.abs(phacelle)) * fade;
            height -= gully * this.gullyStrength;
        }

        // Center roughly around 0.0 for additive terrain relief
        return Math.max(-1.5, Math.min(1.5, height - 0.38));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
    }

    private static double samplePhacelle2D(double x, double z, double tx, double tz, double waveFreq) {
        int ix = fastFloor(x);
        int iz = fastFloor(z);
        double sum = 0.0;
        double wSum = 1e-6;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = ix + dx;
                int cz = iz + dz;
                long h = hash2(cx, cz, 0x517CC1B727220A95L);
                double ox = cx + 0.15 + ((h & 0xFFFFL) / 65536.0) * 0.70;
                double oz = cz + 0.15 + (((h >>> 16) & 0xFFFFL) / 65536.0) * 0.70;
                double rx = x - ox;
                double rz = z - oz;
                double d2 = rx * rx + rz * rz;
                if (d2 >= 1.44) {
                    continue;
                }
                double w = 1.0 - d2 / 1.44;
                w *= w;
                double phaseOffset = (((h >>> 32) & 0xFFFFL) / 65536.0) * (Math.PI * 2.0);
                sum += w * Math.cos((rx * tx + rz * tz) * waveFreq + phaseOffset);
                wSum += w;
            }
        }
        return sum / wSum;
    }

    private static double valueAt(int x, int z, int octave) {
        long h = hash2(x, z, 0x9E3779B97F4A7C15L * (octave + 1));
        return ((h & 0xFFFFFFL) / 8388608.0) - 1.0;
    }

    private static long hash2(int x, int z, long seed) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xBF58476D1CE4E5B9L ^ seed;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static int fastFloor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static double smoothstep(double e0, double e1, double x) {
        if (e1 <= e0) {
            return x >= e1 ? 1.0 : 0.0;
        }
        double t = Math.max(0.0, Math.min(1.0, (x - e0) / (e1 - e0)));
        return t * t * (3.0 - 2.0 * t);
    }

    @Override
    public double minValue() {
        return -1.5;
    }

    @Override
    public double maxValue() {
        return 1.5;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
