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
 * Rare 2D Voronoi vertical collapse shafts (Martian lava tube skylights / sinkholes) that cleanly
 * pierce the crust between {@code minY} and {@code maxY} with seamless chunk boundaries and natural
 * flared rims.
 */
public record SkylightShaftDensityFunction(
        double cellSize,
        double rarity,
        double topRadius,
        double bottomRadius,
        int minY,
        int maxY,
        long seed
) implements DensityFunction.SimpleFunction {

    public static final MapCodec<SkylightShaftDensityFunction> CODEC_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("cell_size").forGetter(SkylightShaftDensityFunction::cellSize),
            Codec.DOUBLE.fieldOf("rarity").forGetter(SkylightShaftDensityFunction::rarity),
            Codec.DOUBLE.fieldOf("top_radius").forGetter(SkylightShaftDensityFunction::topRadius),
            Codec.DOUBLE.fieldOf("bottom_radius").forGetter(SkylightShaftDensityFunction::bottomRadius),
            Codec.INT.fieldOf("min_y").forGetter(SkylightShaftDensityFunction::minY),
            Codec.INT.fieldOf("max_y").forGetter(SkylightShaftDensityFunction::maxY),
            Codec.LONG.fieldOf("seed").forGetter(SkylightShaftDensityFunction::seed)
    ).apply(instance, SkylightShaftDensityFunction::new));

    public static final KeyDispatchDataCodec<SkylightShaftDensityFunction> CODEC = KeyDispatchDataCodec.of(CODEC_MAP);

    @Override
    public double compute(FunctionContext ctx) {
        int y = ctx.blockY();
        if (y < this.minY || y > this.maxY) {
            return 0.0;
        }

        double wx = ctx.blockX();
        double wz = ctx.blockZ();
        double invCell = 1.0 / Math.max(32.0, this.cellSize);
        int ix = fastFloor(wx * invCell);
        int iz = fastFloor(wz * invCell);

        double yT = (double) (y - this.minY) / Math.max(1.0, (double) (this.maxY - this.minY));
        double baseRadius = this.bottomRadius + (this.topRadius - this.bottomRadius) * (yT * yT);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = ix + dx;
                int cz = iz + dz;
                long h = hash2(cx, cz, this.seed);
                double roll = ((h >>> 40) & 0xFFFFL) / 65536.0;
                if (roll > this.rarity) {
                    continue;
                }

                double centerX = (cx + 0.25 + ((h & 0xFFFFL) / 65536.0) * 0.50) * this.cellSize;
                double centerZ = (cz + 0.25 + (((h >>> 16) & 0xFFFFL) / 65536.0) * 0.50) * this.cellSize;
                double rx = wx - centerX;
                double rz = wz - centerZ;
                double maxR = baseRadius * 1.25;
                double rSq = rx * rx + rz * rz;
                if (rSq >= maxR * maxR) {
                    continue;
                }

                double angle = Math.atan2(rz, rx);
                double jitterR = baseRadius * (1.0 + 0.14 * Math.sin(angle * 3.0 + y * 0.09) + 0.08 * Math.cos(angle * 5.0 - y * 0.07));
                double r = Math.sqrt(rSq);
                if (r < jitterR) {
                    return -4.0 * (1.0 - r / jitterR);
                }
            }
        }
        return 0.0;
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
