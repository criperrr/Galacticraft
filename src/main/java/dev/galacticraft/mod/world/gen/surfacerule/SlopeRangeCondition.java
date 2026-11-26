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
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Custom geological {@link SurfaceRules.ConditionSource} that evaluates the local surface slope
 * ({@code maxDelta} across cardinal neighbors in {@link Heightmap.Types#WORLD_SURFACE_WG}) in O(1)
 * per column:
 * <ul>
 *     <li>{@code 0..1}: flat regolith / dust plains</li>
 *     <li>{@code 2..3}: talus / scree / impact breccia slopes</li>
 *     <li>{@code >= 4}: exposed bedrock / stratified canyon cliffs</li>
 * </ul>
 */
public record SlopeRangeCondition(int minDelta, int maxDelta) implements SurfaceRules.ConditionSource {

    public static final MapCodec<SlopeRangeCondition> CODEC_MAP = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("min_delta").forGetter(SlopeRangeCondition::minDelta),
            Codec.INT.fieldOf("max_delta").forGetter(SlopeRangeCondition::maxDelta)
    ).apply(instance, SlopeRangeCondition::new));

    public static final KeyDispatchDataCodec<SlopeRangeCondition> CODEC = KeyDispatchDataCodec.of(CODEC_MAP);

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        return new SlopePredicate(context, this.minDelta, this.maxDelta);
    }

    private static final class SlopePredicate extends SurfaceRules.LazyXZCondition {
        private final int minDelta;
        private final int maxDelta;

        private SlopePredicate(SurfaceRules.Context context, int minDelta, int maxDelta) {
            super(context);
            this.minDelta = minDelta;
            this.maxDelta = maxDelta;
        }

        @Override
        protected boolean compute() {
            int localX = this.context.blockX & 15;
            int localZ = this.context.blockZ & 15;
            ChunkAccess chunk = this.context.chunk;

            int north = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, Math.max(localZ - 1, 0));
            int south = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, Math.min(localZ + 1, 15));
            int west = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, Math.max(localX - 1, 0), localZ);
            int east = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, Math.min(localX + 1, 15), localZ);

            int maxSlope = Math.max(Math.abs(south - north), Math.abs(east - west));
            return maxSlope >= this.minDelta && maxSlope <= this.maxDelta;
        }
    }
}
