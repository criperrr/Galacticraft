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
    public static final SurfaceRules.RuleSource PUMICE = block(GCBlocks.PUMICE);
    public static final SurfaceRules.RuleSource BASALT = block(Blocks.BASALT);
    public static final SurfaceRules.RuleSource SMOOTH_BASALT = block(Blocks.SMOOTH_BASALT);
    public static final SurfaceRules.RuleSource MAGMA = block(Blocks.MAGMA_BLOCK);
    public static final SurfaceRules.RuleSource GALENA = block(GCBlocks.GALENA_ORE);

    // Subtle geological basalt & scorched thermal patches across Venus surface
    private static final SurfaceRules.RuleSource GEOLOGICAL_PATCHES = SurfaceRules.sequence(
            // Hot geothermal pockets (Scorched Venus Rock + occasional Magma Block)
            SurfaceRules.ifTrue(
                    SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.58, 1.0),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.62, 1.0), MAGMA),
                            SCORCHED_ROCK
                    )
            ),
            // Natural volcanic basalt flow patches
            SurfaceRules.ifTrue(
                    SurfaceRules.noiseCondition(Noises.SURFACE, 0.48, 1.0),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.35, 1.0), SMOOTH_BASALT),
                            BASALT
                    )
            )
    );

    // GC4 BiomeGenVenusValley: Soft Venus Rock over Hard Venus Rock on valley terrain;
    // Pumice (ROCK_VOLCANIC_DEPOSIT) and Scorched/Basalt patches only in the deepest valley depressions (Y < 52).
    private static final SurfaceRules.RuleSource VALLEY_DEEP_FLOOR = SurfaceRules.sequence(
            GEOLOGICAL_PATCHES,
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25, 1.0), PUMICE),
            SOFT_ROCK
    );

    private static final SurfaceRules.RuleSource VALLEY_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), HARD_ROCK),
            SurfaceRules.ifTrue(
                    SurfaceRules.yBlockCheck(VerticalAnchor.absolute(52), 0),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(GEOLOGICAL_PATCHES, SOFT_ROCK)),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, HARD_ROCK),
                            HARD_ROCK
                    )
            ),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, VALLEY_DEEP_FLOOR),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, HARD_ROCK),
            HARD_ROCK
    );

    // GC4 BiomeGenVenusMountain: Hard Venus Rock on steep craggy slopes (GC4 k == 0 check),
    // Soft Venus Rock over Hard Venus Rock on normal slopes, with basalt/scorched patches on ridges.
    private static final SurfaceRules.RuleSource MOUNTAIN_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), HARD_ROCK),
            SurfaceRules.ifTrue(
                    SurfaceRules.yBlockCheck(VerticalAnchor.absolute(115), 0),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(GEOLOGICAL_PATCHES, HARD_ROCK)),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, HARD_ROCK)
                    )
            ),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(GEOLOGICAL_PATCHES, SOFT_ROCK)),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SOFT_ROCK),
            HARD_ROCK
    );

    // GC4 BiomeGenFlatVenus: Soft Venus Rock (ROCK_SOFT) on surface and sub-surface over Hard Venus Rock (ROCK_HARD),
    // broken up by natural basalt & scorched rock geological patches.
    private static final SurfaceRules.RuleSource FLAT_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), HARD_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(GEOLOGICAL_PATCHES, SOFT_ROCK)),
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
            PEAK_METALLIC_SNOW,
            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(GCBiomes.Venus.VENUS_VALLEY),
                    VALLEY_SURFACE
            ),
            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(GCBiomes.Venus.VENUS_MOUNTAIN),
                    MOUNTAIN_SURFACE
            ),
            SurfaceRules.ifTrue(
                    SurfaceRules.isBiome(GCBiomes.Venus.VENUS_FLAT),
                    FLAT_SURFACE
            ),
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
