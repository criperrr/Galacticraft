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
 * 3D domain-warped Worley/Cellular cave generator supporting both {@code F1} (organic bubble
 * cathedrals / glacial domes / volcanic geodes) and {@code F2 - F1} (interconnected honeycomb
 * cavern webs with natural rock bridges and arches).
 * Always wrap in {@code DensityFunctions.interpolated(...)} and cull with {@code rangeChoice}.
 */
public record WorleyCaveDensityFunction(
        double cellSize,
        double warpAmp,
        double threshold,
        boolean cellularNetwork
) implements DensityFunction.SimpleFunction {

    public static final MapCodec<WorleyCaveDensityFunction> CODEC_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("cell_size").forGetter(WorleyCaveDensityFunction::cellSize),
            Codec.DOUBLE.fieldOf("warp_amp").forGetter(WorleyCaveDensityFunction::warpAmp),
            Codec.DOUBLE.fieldOf("threshold").forGetter(WorleyCaveDensityFunction::threshold),
            Codec.BOOL.fieldOf("cellular_network").forGetter(WorleyCaveDensityFunction::cellularNetwork)
    ).apply(instance, WorleyCaveDensityFunction::new));

    public static final KeyDispatchDataCodec<WorleyCaveDensityFunction> CODEC = KeyDispatchDataCodec.of(CODEC_MAP);

    @Override
    public double compute(FunctionContext ctx) {
        double bx = ctx.blockX();
        double by = ctx.blockY();
        double bz = ctx.blockZ();

        double wx = bx + Math.sin(bz * 0.031 + by * 0.023) * this.warpAmp;
        double wy = by + Math.cos(bx * 0.029 - bz * 0.027) * (this.warpAmp * 0.5);
        double wz = bz + Math.sin(bx * 0.033 + by * 0.019) * this.warpAmp;

        double inv = 1.0 / Math.max(12.0, this.cellSize);
        double sx = wx * inv;
        double sy = wy * inv;
        double sz = wz * inv;
        int ix = fastFloor(sx);
        int iy = fastFloor(sy);
        int iz = fastFloor(sz);

        double f1Sq = Double.MAX_VALUE;
        double f2Sq = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int cx = ix + dx;
                    int cy = iy + dy;
                    int cz = iz + dz;
                    long h = hash3(cx, cy, cz);
                    double px = cx + 0.15 + ((h & 0xFFFFL) / 65536.0) * 0.70;
                    double py = cy + 0.15 + (((h >>> 16) & 0xFFFFL) / 65536.0) * 0.70;
                    double pz = cz + 0.15 + (((h >>> 32) & 0xFFFFL) / 65536.0) * 0.70;
                    double dSq = (px - sx) * (px - sx) + (py - sy) * (py - sy) + (pz - sz) * (pz - sz);
                    if (dSq < f1Sq) {
                        f2Sq = f1Sq;
                        f1Sq = dSq;
                    } else if (dSq < f2Sq) {
                        f2Sq = dSq;
                    }
                }
            }
        }

        double metric = this.cellularNetwork ? (Math.sqrt(f2Sq) - Math.sqrt(f1Sq)) : Math.sqrt(f1Sq);
        return Math.max(-4.0, Math.min(0.0, (metric - this.threshold) * 4.0));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
    }

    private static long hash3(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xBF58476D1CE4E5B9L ^ z * 0x94D049BB133111EBL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static int fastFloor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    @Override
    public double minValue() {
        return -4.0;
    }

    @Override
    public double maxValue() {
        return 0.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
