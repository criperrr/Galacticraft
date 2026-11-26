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
 * Polynomial smooth minimum (Inigo Quilez {@code smin}) for organic junction blending between
 * cave chambers, tunnels, and structural pillars without sharp boolean seams.
 */
public record SmoothMinDensityFunction(
        DensityFunction a,
        DensityFunction b,
        double k
) implements DensityFunction {

    public static final MapCodec<SmoothMinDensityFunction> CODEC_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("a").forGetter(SmoothMinDensityFunction::a),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("b").forGetter(SmoothMinDensityFunction::b),
            Codec.DOUBLE.fieldOf("k").forGetter(SmoothMinDensityFunction::k)
    ).apply(instance, SmoothMinDensityFunction::new));

    public static final KeyDispatchDataCodec<SmoothMinDensityFunction> CODEC = KeyDispatchDataCodec.of(CODEC_MAP);

    private static final ThreadLocal<double[]> SCRATCH_B = ThreadLocal.withInitial(() -> new double[0]);

    @Override
    public double compute(FunctionContext ctx) {
        double va = this.a.compute(ctx);
        double vb = this.b.compute(ctx);
        if (this.k <= 1e-6) {
            return Math.min(va, vb);
        }
        double h = Math.max(this.k - Math.abs(va - vb), 0.0) / this.k;
        return Math.min(va, vb) - h * h * this.k * 0.25;
    }

    @Override
    public void fillArray(double[] out, ContextProvider provider) {
        double[] bufB = SCRATCH_B.get();
        if (bufB.length < out.length) {
            bufB = new double[out.length];
            SCRATCH_B.set(bufB);
        }
        // Only allocate an exact-length view if lengths differ (Minecraft cell arrays are fixed size per chunk)
        if (bufB.length != out.length) {
            bufB = new double[out.length];
            SCRATCH_B.set(bufB);
        }
        this.a.fillArray(out, provider);
        this.b.fillArray(bufB, provider);
        if (this.k <= 1e-6) {
            for (int i = 0; i < out.length; i++) {
                out[i] = Math.min(out[i], bufB[i]);
            }
            return;
        }
        double invK = 1.0 / this.k;
        double quarterK = this.k * 0.25;
        for (int i = 0; i < out.length; i++) {
            double va = out[i];
            double vb = bufB[i];
            double h = Math.max(this.k - Math.abs(va - vb), 0.0) * invK;
            out[i] = Math.min(va, vb) - h * h * quarterK;
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new SmoothMinDensityFunction(this.a.mapAll(visitor), this.b.mapAll(visitor), this.k));
    }

    @Override
    public double minValue() {
        return Math.min(this.a.minValue(), this.b.minValue()) - Math.max(0.0, this.k) * 0.25;
    }

    @Override
    public double maxValue() {
        return Math.min(this.a.maxValue(), this.b.maxValue());
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
