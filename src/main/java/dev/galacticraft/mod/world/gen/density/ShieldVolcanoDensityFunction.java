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
import dev.galacticraft.mod.world.gen.noise.DensityCellular2D;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Broad Gaussian planetary shield volcano (Olympus Mons / Tharsis Montes archetype) anchored on
 * non-overlapping {@link DensityCellular2D} Voronoi centers, with subtle flank lava-channel modulation,
 * crisp basal scarp rolloff, and a recessed summit caldera.
 * Always wrap in {@code DensityFunctions.cache2d(...)} in the router DAG.
 */
public record ShieldVolcanoDensityFunction(
        double regionSize,
        double frequency,
        double maxHeight,
        double sigma,
        double calderaRadius,
        double calderaDepth,
        long seed
) implements DensityFunction.SimpleFunction {

    public static final MapCodec<ShieldVolcanoDensityFunction> CODEC_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("region_size").forGetter(ShieldVolcanoDensityFunction::regionSize),
            Codec.DOUBLE.fieldOf("frequency").forGetter(ShieldVolcanoDensityFunction::frequency),
            Codec.DOUBLE.fieldOf("max_height").forGetter(ShieldVolcanoDensityFunction::maxHeight),
            Codec.DOUBLE.fieldOf("sigma").forGetter(ShieldVolcanoDensityFunction::sigma),
            Codec.DOUBLE.fieldOf("caldera_radius").forGetter(ShieldVolcanoDensityFunction::calderaRadius),
            Codec.DOUBLE.fieldOf("caldera_depth").forGetter(ShieldVolcanoDensityFunction::calderaDepth),
            Codec.LONG.fieldOf("seed").forGetter(ShieldVolcanoDensityFunction::seed)
    ).apply(instance, ShieldVolcanoDensityFunction::new));

    public static final KeyDispatchDataCodec<ShieldVolcanoDensityFunction> CODEC = KeyDispatchDataCodec.of(CODEC_MAP);

    private static final ThreadLocal<DensityCellular2D> CELLULAR_CACHE = new ThreadLocal<>();

    @Override
    public double compute(FunctionContext ctx) {
        DensityCellular2D cellular = CELLULAR_CACHE.get();
        if (cellular == null || cellular.seed != this.seed || cellular.regionSize != Math.max(16.0, this.regionSize)) {
            cellular = new DensityCellular2D(this.seed, this.regionSize, 1.0, 1, 1);
            CELLULAR_CACHE.set(cellular);
        }
        double wx = ctx.blockX();
        double wz = ctx.blockZ();
        DensityCellular2D.Cell cell = cellular.cell(wx, wz);

        if (!cellular.checkCellRarity(cell, (float) this.frequency) || cellular.maxSafeDiameter(cell) <= 0.18) {
            return 0.0;
        }

        double dx = wx - cell.x();
        double dz = wz - cell.z();
        double rSq = dx * dx + dz * dz;
        double cutoff = this.sigma * 2.25;
        if (rSq >= cutoff * cutoff) {
            return 0.0;
        }

        double r = Math.sqrt(rSq);
        double angle = Math.atan2(dz, dx);
        // Subtle radial flank channels (lava flows radiating down the shield)
        double flankWarp = 1.0 + 0.06 * Math.sin(angle * 7.0 + (cell.id() & 0xFFL))
                + 0.03 * Math.cos(angle * 13.0 - r * 0.035);
        double effR = r * flankWarp;

        double norm = effR / Math.max(1.0, this.sigma);
        double shield = this.maxHeight * Math.exp(-(norm * norm));

        // Crisp Olympus-style basal scarp rolloff near r = 1.65..2.25 * sigma
        if (norm > 1.55) {
            double t = Math.max(0.0, Math.min(1.0, (2.25 - norm) / 0.70));
            shield *= t * t * (3.0 - 2.0 * t);
        }

        // Recessed summit caldera
        if (effR < this.calderaRadius && this.calderaRadius > 0.0) {
            double cNorm = effR / this.calderaRadius;
            double calderaBowl = 1.0 - cNorm * cNorm;
            shield -= this.calderaDepth * calderaBowl;
        }

        return Math.max(0.0, Math.min(this.maxHeight, shield));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider provider) {
        provider.fillAllDirectly(densities, this);
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return this.maxHeight;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
