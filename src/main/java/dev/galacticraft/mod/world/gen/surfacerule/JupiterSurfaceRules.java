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
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class JupiterSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);

    // Dedicated Jovian lithology from GCBlocks
    private static final RuleSource JUPITER_SURFACE = state(GCBlocks.JUPITER_SURFACE);
    private static final RuleSource JUPITER_SUB_SURFACE = state(GCBlocks.JUPITER_SUB_SURFACE);
    private static final RuleSource JUPITER_STONE = state(GCBlocks.JUPITER_STONE);
    private static final RuleSource JUPITER_RED_STORM_ROCK = state(GCBlocks.JUPITER_RED_STORM_ROCK);
    private static final RuleSource JUPITER_BANDED_OCHRE_ROCK = state(GCBlocks.JUPITER_BANDED_OCHRE_ROCK);
    private static final RuleSource JUPITER_METALLIC_HYDROGEN_ROCK = state(GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK);
    private static final RuleSource JUPITER_CORE_BRECCIA = state(GCBlocks.JUPITER_CORE_BRECCIA);

    public static final RuleSource JUPITER = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        // Cheap Y-bound conditions first (base terrain around Y=75..85)
        ConditionSource isAbove68 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(68), 0);
        ConditionSource isAbove88 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(88), 0);
        ConditionSource isAbove115 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(115), 0);
        ConditionSource isBelow56 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(56), 0));
        ConditionSource isBelow24 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(24), 0));

        // Biome predicates
        ConditionSource isRedStormVortex = biome(GCBiomes.Jupiter.RED_STORM_VORTEX);
        ConditionSource isOchreBandPlateau = biome(GCBiomes.Jupiter.OCHRE_BAND_PLATEAU);
        ConditionSource isAmmoniaCloudBasin = biome(GCBiomes.Jupiter.AMMONIA_CLOUD_BASIN);
        ConditionSource isJovianBadlands = biome(GCBiomes.Jupiter.JOVIAN_BADLANDS);
        ConditionSource isFulguriteShatterlands = biome(GCBiomes.Jupiter.FULGURITE_SHATTERLANDS);
        ConditionSource isMetallicHydrogenOcean = biome(GCBiomes.Jupiter.METALLIC_HYDROGEN_OCEAN);
        ConditionSource isHyperbaricCoreDepths = biome(GCBiomes.Jupiter.HYPERBARIC_CORE_DEPTHS);
        ConditionSource isThunderheadCaverns = biome(GCBiomes.Jupiter.THUNDERHEAD_CAVERNS);

        // Gradual noise conditions for smooth organic blending across biome boundaries
        ConditionSource primaryBlendNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.12D, 0.45D);
        ConditionSource secondaryBlendNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.05D, 0.55D);
        ConditionSource finePatchNoise = SurfaceRules.noiseCondition(Noises.GRAVEL, 0.12D, 0.65D);
        ConditionSource bandStrataNoise = SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, -0.05D, 0.40D);

        // 1. Steep cliff rules BEFORE ON_FLOOR so vertical walls expose banded Jovian strata
        RuleSource steepCliffRules = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                isAbove115,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_RED_STORM_ROCK),
                                        JUPITER_BANDED_OCHRE_ROCK
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isAbove88,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(bandStrataNoise, JUPITER_BANDED_OCHRE_ROCK),
                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_RED_STORM_ROCK),
                                        JUPITER_SUB_SURFACE
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isBelow56,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_CORE_BRECCIA),
                                        JUPITER_METALLIC_HYDROGEN_ROCK
                                )
                        ),
                        JUPITER_STONE
                )
        );

        // 2. Smoothly blended surface rules (above preliminary surface)
        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffRules,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        // High storm ridges (Y >= 115) blend into Red Storm Rock and Banded Ochre Rock
                                        SurfaceRules.ifTrue(
                                                isAbove115,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_RED_STORM_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_BANDED_OCHRE_ROCK),
                                                        JUPITER_SURFACE
                                                )
                                        ),
                                        // Red Storm Vortex: gradual blend between JUPITER_RED_STORM_ROCK, JUPITER_BANDED_OCHRE_ROCK, and JUPITER_SURFACE
                                        SurfaceRules.ifTrue(
                                                isRedStormVortex,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_RED_STORM_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_BANDED_OCHRE_ROCK),
                                                        JUPITER_SURFACE
                                                )
                                        ),
                                        // Ochre Band Plateau: gradual blend between JUPITER_BANDED_OCHRE_ROCK and JUPITER_SURFACE
                                        SurfaceRules.ifTrue(
                                                isOchreBandPlateau,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_BANDED_OCHRE_ROCK),
                                                        SurfaceRules.ifTrue(finePatchNoise, JUPITER_RED_STORM_ROCK),
                                                        JUPITER_SURFACE
                                                )
                                        ),
                                        // Ammonia Cloud Basin: predominantly JUPITER_SURFACE with soft ochre and sub-surface patches
                                        SurfaceRules.ifTrue(
                                                isAmmoniaCloudBasin,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_BANDED_OCHRE_ROCK),
                                                        SurfaceRules.ifTrue(finePatchNoise, JUPITER_SUB_SURFACE),
                                                        JUPITER_SURFACE
                                                )
                                        ),
                                        // Jovian Badlands: banded ochre and red storm strata blended into JUPITER_SURFACE
                                        SurfaceRules.ifTrue(
                                                isJovianBadlands,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(bandStrataNoise, JUPITER_BANDED_OCHRE_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_RED_STORM_ROCK),
                                                        JUPITER_SURFACE
                                                )
                                        ),
                                        // Fulgurite Shatterlands: core breccia and red storm outcrops blended into JUPITER_SURFACE
                                        SurfaceRules.ifTrue(
                                                isFulguriteShatterlands,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_CORE_BRECCIA),
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_RED_STORM_ROCK),
                                                        JUPITER_SURFACE
                                                )
                                        ),
                                        // Default planet surface fallback
                                        SurfaceRules.ifTrue(
                                                isAbove68,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(finePatchNoise, JUPITER_BANDED_OCHRE_ROCK),
                                                        JUPITER_SURFACE
                                                )
                                        ),
                                        JUPITER_SUB_SURFACE
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                isRedStormVortex,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_RED_STORM_ROCK),
                                                        JUPITER_SUB_SURFACE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                isOchreBandPlateau,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_BANDED_OCHRE_ROCK),
                                                        JUPITER_SUB_SURFACE
                                                )
                                        ),
                                        JUPITER_SUB_SURFACE
                                )
                        )
                )
        );

        // 3. Subterranean 3D cave rules (below Y=56, cheap Y check before noise)
        RuleSource caveRules = SurfaceRules.ifTrue(
                isBelow56,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                isMetallicHydrogenOcean,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_METALLIC_HYDROGEN_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_CORE_BRECCIA),
                                                        JUPITER_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_METALLIC_HYDROGEN_ROCK),
                                                        JUPITER_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_METALLIC_HYDROGEN_ROCK),
                                                        JUPITER_STONE
                                                )
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isHyperbaricCoreDepths,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_CORE_BRECCIA),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_METALLIC_HYDROGEN_ROCK),
                                                        JUPITER_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_CORE_BRECCIA),
                                                        JUPITER_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_CORE_BRECCIA),
                                                        JUPITER_STONE
                                                )
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isThunderheadCaverns,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_RED_STORM_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_BANDED_OCHRE_ROCK),
                                                        JUPITER_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_BANDED_OCHRE_ROCK),
                                                        JUPITER_STONE
                                                )
                                        )
                                )
                        ),
                        // Deep hyperbaric transition near bedrock (Y < 24)
                        SurfaceRules.ifTrue(
                                isBelow24,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, JUPITER_CORE_BRECCIA),
                                        SurfaceRules.ifTrue(secondaryBlendNoise, JUPITER_METALLIC_HYDROGEN_ROCK),
                                        JUPITER_STONE
                                )
                        )
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                caveRules,
                JUPITER_STONE
        );
    }

    @Contract("_ -> new")
    private static @NotNull ConditionSource biome(ResourceKey<Biome> biome) {
        return SurfaceRules.isBiome(biome);
    }

    @Contract("_ -> new")
    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
    }
}
