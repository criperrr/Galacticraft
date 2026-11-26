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
 * Broad, organically curved Martian aeolian dunes using 2D value-noise domain warping and
 * regional crest modulation so sand sheets roll naturally without rigid parallel striping.
 * Always wrap in {@code DensityFunctions.cache2d(...)} in the router DAG.
 */
public record AeolianDuneDensityFunction(
        double wavelength,
        double amplitude,
        double sharpness,
        double warpAmp
) implements DensityFunction.SimpleFunction {

    public static final MapCodec<AeolianDuneDensityFunction> CODEC_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("wavelength").forGetter(AeolianDuneDensityFunction::wavelength),
            Codec.DOUBLE.fieldOf("amplitude").forGetter(AeolianDuneDensityFunction::amplitude),
            Codec.DOUBLE.fieldOf("sharpness").forGetter(AeolianDuneDensityFunction::sharpness),
            Codec.DOUBLE.fieldOf("warp_amp").forGetter(AeolianDuneDensityFunction::warpAmp)
    ).apply(instance, AeolianDuneDensityFunction::new));

    public static final KeyDispatchDataCodec<AeolianDuneDensityFunction> CODEC = KeyDispatchDataCodec.of(CODEC_MAP);

    @Override
    public double compute(FunctionContext ctx) {
        double wx = ctx.blockX();
        double wz = ctx.blockZ();
        double invWave = (Math.PI * 2.0) / Math.max(16.0, this.wavelength);

        // Multi-scale value-noise warp curves dune crests organically (breaks parallel stripes)
        double n1 = valueNoise2D(wx * 0.008, wz * 0.008, 0x44554E4501L);
        double n2 = valueNoise2D(wx * 0.021, wz * 0.021, 0x44554E4502L);
        double warp = this.warpAmp * (n1 * 0.72 + n2 * 0.28);

        // Regional dune-field envelope so sand sheets swell and flatten naturally
        double envelope = 0.45 + 0.55 * (0.5 + 0.5 * valueNoise2D(wx * 0.0055, wz * 0.0055, 0x44554E4503L));

        double phase = (wx * 0.8 + wz * 0.6 + warp) * invWave;
        double primary = Math.pow(0.5 * (1.0 + Math.sin(phase)), Math.max(1.0, this.sharpness));

        // Gentle secondary cross-swell for natural barchan/star-dune intersection
        double crossPhase = (-wx * 0.5 + wz * 0.866 + warp * 0.65) * (invWave * 0.62);
        double secondary = 0.5 * (1.0 + Math.sin(crossPhase));

        double combined = primary * 0.78 + secondary * 0.22;
        return this.amplitude * envelope * (combined - 0.42);
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
    }

    private static double valueNoise2D(double x, double z, long seed) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        double fx = x - x0;
        double fz = z - z0;
        double ux = fx * fx * (3.0 - 2.0 * fx);
        double uz = fz * fz * (3.0 - 2.0 * fz);

        double v00 = hashUnit(x0, z0, seed);
        double v10 = hashUnit(x0 + 1, z0, seed);
        double v01 = hashUnit(x0, z0 + 1, seed);
        double v11 = hashUnit(x0 + 1, z0 + 1, seed);

        double nx0 = v00 + ux * (v10 - v00);
        double nx1 = v01 + ux * (v11 - v01);
        return nx0 + uz * (nx1 - nx0);
    }

    private static double hashUnit(int x, int z, long seed) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xBF58476D1CE4E5B9L ^ seed;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return ((h & 0xFFFFFFL) / 8388608.0) - 1.0;
    }

    private static int fastFloor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    @Override
    public double minValue() {
        return -this.amplitude * 0.45;
    }

    @Override
    public double maxValue() {
        return this.amplitude * 0.65;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
