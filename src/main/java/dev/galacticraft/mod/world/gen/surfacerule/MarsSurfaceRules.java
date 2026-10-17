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

public class MarsSurfaceRules {
    private static final SurfaceRules.RuleSource BEDROCK = block(Blocks.BEDROCK);
    public static final SurfaceRules.RuleSource SURFACE_ROCK = block(GCBlocks.MARS_SURFACE_ROCK);
    public static final SurfaceRules.RuleSource SUB_SURFACE_ROCK = block(GCBlocks.MARS_SUB_SURFACE_ROCK);
    public static final SurfaceRules.RuleSource STONE = block(GCBlocks.MARS_STONE);
    public static final SurfaceRules.RuleSource DENSE_ICE = block(GCBlocks.DENSE_ICE);

    // GC4 VALLEY_HEIGHT_MOD & GalaxySpace Mars_Ravine:
    // Steep canyon walls expose Martian Stone (meta 9); canyon floors mix Sub-Surface Rock (meta 6) and Surface Rock (meta 5)
    private static final SurfaceRules.RuleSource CANYON_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), STONE),
            SurfaceRules.ifTrue(
                    SurfaceRules.ON_FLOOR,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.15, 1.0), SURFACE_ROCK),
                            SUB_SURFACE_ROCK
                    )
            ),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SUB_SURFACE_ROCK),
            STONE
    );

    // GC4 MOUNTAIN_HEIGHT_MOD & GalaxySpace Mars_Mountains / Mars_Triangle_Mountains:
    // Steep mountain cliffs expose bare Mars Stone; high peaks (Y >= 145) feature Dense Ice permafrost patches and rocky ridges
    private static final SurfaceRules.RuleSource HIGHLANDS_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), STONE),
            SurfaceRules.ifTrue(
                    SurfaceRules.yBlockCheck(VerticalAnchor.absolute(145), 0),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(
                                    SurfaceRules.ON_FLOOR,
                                    SurfaceRules.sequence(
                                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.35, 1.0), DENSE_ICE),
                                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.30, 1.0), STONE),
                                            SURFACE_ROCK
                                    )
                            ),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SUB_SURFACE_ROCK)
                    )
            ),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SURFACE_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SUB_SURFACE_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, SUB_SURFACE_ROCK),
            STONE
    );

    // GC4 BiomeGenFlatMars + ChunkProviderSpace.replaceBlocksForBiome & GalaxySpace Mars_Plains / Mars_High_Plains:
    // Top layer is Mars Surface Rock (meta 5) with occasional bare Mars Stone outcrops (GC4 var12 <= 0),
    // underlain by a 3-4 block layer of Mars Sub-Surface Rock (meta 6) over Mars Stone (meta 9).
    private static final SurfaceRules.RuleSource FLAT_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(
                    SurfaceRules.ON_FLOOR,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -1.0, -0.55), STONE),
                            SURFACE_ROCK
                    )
            ),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SUB_SURFACE_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, SUB_SURFACE_ROCK),
            STONE
    );

    private static final SurfaceRules.RuleSource SURFACE_GENERATION = SurfaceRules.sequence(
            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(GCBiomes.Mars.MARS_CANYON),
                    CANYON_SURFACE
            ),
            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(GCBiomes.Mars.MARS_HIGHLANDS),
                    HIGHLANDS_SURFACE
            ),
            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(GCBiomes.Mars.MARS_FLAT),
                    FLAT_SURFACE
            ),
            FLAT_SURFACE
    );

    public static final SurfaceRules.RuleSource MARS = createDefaultRule();

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
