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
    public static final SurfaceRules.RuleSource GALENA = block(GCBlocks.GALENA_ORE);

    // GC4 BiomeGenVenusValley: low valley floors (Y < 60, corresponding to GC4 j1 < 53) have Pumice (ROCK_VOLCANIC_DEPOSIT)
    // and Volcanic Rock (ROCK_MAGMA) over Hard Venus Rock; higher valley slopes have Soft Venus Rock.
    private static final SurfaceRules.RuleSource VALLEY_LOW_FLOOR = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25, 1.0), VOLCANIC_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.35, 1.0), SCORCHED_ROCK),
            PUMICE
    );

    private static final SurfaceRules.RuleSource VALLEY_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(
                    SurfaceRules.yBlockCheck(VerticalAnchor.absolute(60), 0),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SOFT_ROCK),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SOFT_ROCK),
                            HARD_ROCK
                    )
            ),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, VALLEY_LOW_FLOOR),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, HARD_ROCK),
            HARD_ROCK
    );

    // GC4 BiomeGenVenusMountain + GalaxySpace Io_Volcano / ExtraPlanets volcanic mountains:
    // Steep volcanic slopes expose Volcanic Rock (ROCK_MAGMA); normal mountain tops have Soft Venus Rock over Hard Venus Rock.
    private static final SurfaceRules.RuleSource MOUNTAIN_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), VOLCANIC_ROCK),
            SurfaceRules.ifTrue(
                    SurfaceRules.yBlockCheck(VerticalAnchor.absolute(115), 0),
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.15, 1.0), VOLCANIC_ROCK),
                            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SOFT_ROCK),
                            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, HARD_ROCK)
                    )
            ),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SOFT_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SOFT_ROCK),
            HARD_ROCK
    );

    // GC4 BiomeGenFlatVenus: Soft Venus Rock (ROCK_SOFT) on surface and sub-surface over Hard Venus Rock (ROCK_HARD)
    private static final SurfaceRules.RuleSource FLAT_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SOFT_ROCK),
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
