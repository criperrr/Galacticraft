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
 * 2D Voronoi power-law impact crater field generating parabolic bowls, Gaussian compression rim uplifts,
 * rebound central peaks for large complex craters, and faint radial ejecta rays.
 * Always wrap in {@code DensityFunctions.cache2d(...)} in the router DAG.
 */
public record CraterFieldDensityFunction(
        double cellSize,
        double depthScale,
        double rimHeight,
        double peakHeight,
        double density,
        long seed
) implements DensityFunction.SimpleFunction {

    public static final MapCodec<CraterFieldDensityFunction> CODEC_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("cell_size").forGetter(CraterFieldDensityFunction::cellSize),
            Codec.DOUBLE.fieldOf("depth_scale").forGetter(CraterFieldDensityFunction::depthScale),
            Codec.DOUBLE.fieldOf("rim_height").forGetter(CraterFieldDensityFunction::rimHeight),
            Codec.DOUBLE.fieldOf("peak_height").forGetter(CraterFieldDensityFunction::peakHeight),
            Codec.DOUBLE.fieldOf("density").forGetter(CraterFieldDensityFunction::density),
            Codec.LONG.fieldOf("seed").forGetter(CraterFieldDensityFunction::seed)
    ).apply(instance, CraterFieldDensityFunction::new));

    public static final KeyDispatchDataCodec<CraterFieldDensityFunction> CODEC = KeyDispatchDataCodec.of(CODEC_MAP);

    @Override
    public double compute(FunctionContext ctx) {
        double wx = ctx.blockX();
        double wz = ctx.blockZ();
        double invCell = 1.0 / Math.max(16.0, this.cellSize);
        int ix = fastFloor(wx * invCell);
        int iz = fastFloor(wz * invCell);

        double total = 0.0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = ix + dx;
                int cz = iz + dz;
                long h = hash2(cx, cz, this.seed);
                double rarityRoll = ((h >>> 40) & 0xFFFFL) / 65536.0;
                if (rarityRoll > this.density) {
                    continue;
                }

                double centerX = (cx + 0.18 + ((h & 0xFFFFL) / 65536.0) * 0.64) * this.cellSize;
                double centerZ = (cz + 0.18 + (((h >>> 16) & 0xFFFFL) / 65536.0) * 0.64) * this.cellSize;
                double sizeRoll = (((h >>> 32) & 0xFFL) / 255.0);
                // Power-law bias toward smaller craters, rarer complex basins
                double radius = this.cellSize * (0.14 + 0.28 * (sizeRoll * sizeRoll));
                double maxInfluence = radius * 1.85;

                double rx = wx - centerX;
                double rz = wz - centerZ;
                double rSq = rx * rx + rz * rz;
                if (rSq >= maxInfluence * maxInfluence) {
                    continue;
                }

                double r = Math.sqrt(rSq);
                double normR = r / radius;
                double profile = 0.0;

                // 1. Parabolic excavation bowl
                if (normR < 1.0) {
                    double bowl = 1.0 - normR * normR;
                    profile -= this.depthScale * (0.65 + 0.35 * sizeRoll) * bowl;
                }

                // 2. Gaussian rim uplift centered at r = R
                double rimDelta = (normR - 1.0) / 0.24;
                profile += this.rimHeight * Math.exp(-0.5 * rimDelta * rimDelta);

                // 3. Rebound central peak for complex craters
                if (sizeRoll > 0.55 && normR < 0.26) {
                    double peakFactor = 1.0 - (normR / 0.26);
                    profile += this.peakHeight * peakFactor * peakFactor;
                }

                // 4. Subtle radial ejecta rays outside rim
                if (normR > 0.95 && normR < 1.85) {
                    double angle = Math.atan2(rz, rx);
                    int rayCount = 5 + (int) (h & 3L);
                    double rayWave = 0.5 + 0.5 * Math.cos(angle * rayCount + sizeRoll * 6.2831853);
                    double decay = Math.exp(-(normR - 0.95) * 2.2);
                    profile += this.rimHeight * 0.18 * rayWave * decay;
                }

                total += profile;
            }
        }

        return Math.max(this.minValue(), Math.min(this.maxValue(), total));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
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

    @Override
    public double minValue() {
        return -this.depthScale * 1.5;
    }

    @Override
    public double maxValue() {
        return Math.max(this.rimHeight, this.peakHeight) * 1.5;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
