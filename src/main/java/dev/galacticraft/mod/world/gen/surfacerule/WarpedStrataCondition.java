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

package dev.galacticraft.mod.world.gen.surfacerule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Lightweight 2D-warped horizontal geological strata condition ({@code < 0.03 ms/chunk}) for
 * sedimentary canyon walls (Valles Marineris) and Polar Layered Deposits (PLD) without 3D noise cost.
 */
public record WarpedStrataCondition(int bandHeight, int bandThickness, double warpAmp) implements SurfaceRules.ConditionSource {

    public static final MapCodec<WarpedStrataCondition> CODEC_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("band_height").forGetter(WarpedStrataCondition::bandHeight),
            Codec.INT.fieldOf("band_thickness").forGetter(WarpedStrataCondition::bandThickness),
            Codec.DOUBLE.fieldOf("warp_amp").forGetter(WarpedStrataCondition::warpAmp)
    ).apply(instance, WarpedStrataCondition::new));

    public static final KeyDispatchDataCodec<WarpedStrataCondition> CODEC = KeyDispatchDataCodec.of(CODEC_MAP);

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        return new StrataPredicate(context, Math.max(2, this.bandHeight), Math.max(1, this.bandThickness), this.warpAmp);
    }

    private static final class StrataPredicate extends SurfaceRules.LazyYCondition {
        private final int bandHeight;
        private final int bandThickness;
        private final double warpAmp;

        private StrataPredicate(SurfaceRules.Context context, int bandHeight, int bandThickness, double warpAmp) {
            super(context);
            this.bandHeight = bandHeight;
            this.bandThickness = bandThickness;
            this.warpAmp = warpAmp;
        }

        @Override
        protected boolean compute() {
            int x = this.context.blockX;
            int z = this.context.blockZ;
            double warp = this.warpAmp * (
                    Math.sin(x * 0.015 + z * 0.012) * 0.65
                            + Math.cos(x * 0.008 - z * 0.019) * 0.35
            );
            int warpedY = this.context.blockY + (int) Math.round(warp);
            return Math.floorMod(warpedY, this.bandHeight) < this.bandThickness;
        }
    }
}
