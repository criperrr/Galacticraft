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

import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.world.biome.GCBiomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class VenusSurfaceRules {
    private static final SurfaceRules.RuleSource BEDROCK = block(Blocks.BEDROCK);
    public static final SurfaceRules.RuleSource SOFT_ROCK = block(GCBlocks.SOFT_VENUS_ROCK);
    public static final SurfaceRules.RuleSource HARD_ROCK = block(GCBlocks.HARD_VENUS_ROCK);
    public static final SurfaceRules.RuleSource SCORCHED_ROCK = block(GCBlocks.SCORCHED_VENUS_ROCK);
    public static final SurfaceRules.RuleSource VOLCANIC_ROCK = block(GCBlocks.VOLCANIC_ROCK);
    public static final SurfaceRules.RuleSource MAGMA = block(Blocks.MAGMA_BLOCK);
    public static final SurfaceRules.RuleSource BASALT = block(Blocks.BASALT);
    public static final SurfaceRules.RuleSource GALENA = block(GCBlocks.GALENA_ORE);

    private static final SurfaceRules.RuleSource VALLEY_FLOOR = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.2, 1.0), MAGMA),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.0, 1.0), BASALT),
            VOLCANIC_ROCK
    );

    private static final SurfaceRules.RuleSource VALLEY_UNDER_FLOOR = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25, 1.0), MAGMA),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.1, 1.0), BASALT),
            VOLCANIC_ROCK
    );

    private static final SurfaceRules.RuleSource MOUNTAIN_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), VOLCANIC_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, HARD_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, HARD_ROCK)
    );

    private static final SurfaceRules.RuleSource FLAT_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SCORCHED_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SOFT_ROCK),
            HARD_ROCK
    );

    private static final SurfaceRules.RuleSource PEAK_METALLIC_SNOW = SurfaceRules.ifTrue(
            SurfaceRules.yBlockCheck(VerticalAnchor.absolute(160), 0),
            SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, GALENA),
                    SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, GALENA)
            )
    );

    private static final SurfaceRules.RuleSource SURFACE_GENERATION = SurfaceRules.sequence(
            // Above Y >= 160: Maxwell Montes heavy metal snow of Galena/Bismuth
            PEAK_METALLIC_SNOW,

            // Biome specific surface generation
            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(GCBiomes.Venus.VENUS_VALLEY),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, VALLEY_FLOOR),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, VALLEY_UNDER_FLOOR),
                            VOLCANIC_ROCK
                    )
            ),
            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(GCBiomes.Venus.VENUS_MOUNTAIN),
                    MOUNTAIN_SURFACE
            ),
            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(GCBiomes.Venus.VENUS_FLAT),
                    FLAT_SURFACE
            ),

            // Default fallback
            FLAT_SURFACE
    );

    public static final SurfaceRules.RuleSource VENUS = createDefaultRule();

    public static @NotNull SurfaceRules.RuleSource createDefaultRule() {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SURFACE_GENERATION)
        );
    }

    @Contract("_ -> new")
    private static @NotNull SurfaceRules.RuleSource block(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}
