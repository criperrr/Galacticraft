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

public class NeptuneSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);

    // Dedicated Neptunian geological strata from GCBlocks (zero artificial/vanilla decorative blocks)
    private static final RuleSource NEPTUNE_SURFACE = state(GCBlocks.NEPTUNE_SURFACE);
    private static final RuleSource NEPTUNE_SUB_SURFACE = state(GCBlocks.NEPTUNE_SUB_SURFACE);
    private static final RuleSource NEPTUNE_STONE = state(GCBlocks.NEPTUNE_STONE);
    private static final RuleSource NEPTUNE_NITROGEN_ICE = state(GCBlocks.NEPTUNE_NITROGEN_ICE);
    private static final RuleSource NEPTUNE_DARK_VORTEX_ROCK = state(GCBlocks.NEPTUNE_DARK_VORTEX_ROCK);
    private static final RuleSource NEPTUNE_CRYO_SLATE = state(GCBlocks.NEPTUNE_CRYO_SLATE);
    private static final RuleSource NEPTUNE_ABYSSAL_MANTLE_ROCK = state(GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK);

    public static final RuleSource NEPTUNE = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        // Cheap Y-bound conditions first (base terrain around Y=100..110)
        ConditionSource isAbove92 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(92), 0);
        ConditionSource isAbove118 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(118), 0);
        ConditionSource isAbove140 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(140), 0);
        ConditionSource isBelow80 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(80), 0));
        ConditionSource isBelow32 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(32), 0));

        // Biome predicates
        ConditionSource isDarkSpotVortex = biome(GCBiomes.Neptune.DARK_SPOT_VORTEX);
        ConditionSource isSupersonicWindPlains = biome(GCBiomes.Neptune.SUPERSONIC_WIND_PLAINS);
        ConditionSource isTritonGeyserFields = biome(GCBiomes.Neptune.TRITON_GEYSER_FIELDS);
        ConditionSource isNitrogenIceShelves = biome(GCBiomes.Neptune.NITROGEN_ICE_SHELVES);
        ConditionSource isCobaltStormHighlands = biome(GCBiomes.Neptune.COBALT_STORM_HIGHLANDS);
        ConditionSource isDiamondRainCaverns = biome(GCBiomes.Neptune.DIAMOND_RAIN_CAVERNS);
        ConditionSource isCryoMagmaTubes = biome(GCBiomes.Neptune.CRYO_MAGMA_TUBES);
        ConditionSource isAbyssalMantleDepths = biome(GCBiomes.Neptune.ABYSSAL_MANTLE_DEPTHS);

        // Gradual noise conditions for seamless transitions between neighboring biomes
        ConditionSource primaryBlendNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.10D, 0.45D);
        ConditionSource secondaryBlendNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.05D, 0.55D);
        ConditionSource finePatchNoise = SurfaceRules.noiseCondition(Noises.GRAVEL, 0.12D, 0.65D);
        ConditionSource windRidgeNoise = SurfaceRules.noiseCondition(Noises.ICE, -0.05D, 0.40D);

        // 1. Steep cliff rules BEFORE ON_FLOOR so wind-scoured escarpments expose natural cryo-slate and vortex rock
        RuleSource steepCliffRules = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                isAbove140,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_NITROGEN_ICE),
                                        NEPTUNE_DARK_VORTEX_ROCK
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isAbove118,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(windRidgeNoise, NEPTUNE_DARK_VORTEX_ROCK),
                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                        NEPTUNE_SUB_SURFACE
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isBelow80,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_ABYSSAL_MANTLE_ROCK),
                                        NEPTUNE_CRYO_SLATE
                                )
                        ),
                        NEPTUNE_STONE
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
                                        // High storm ridges (Y >= 140) blend Nitrogen Ice with Dark Vortex Rock and Neptune Surface
                                        SurfaceRules.ifTrue(
                                                isAbove140,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_NITROGEN_ICE),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_DARK_VORTEX_ROCK),
                                                        NEPTUNE_SURFACE
                                                )
                                        ),
                                        // Dark Spot Vortex: gradual blend of Dark Vortex Rock, Cryo Slate, and Neptune Surface
                                        SurfaceRules.ifTrue(
                                                isDarkSpotVortex,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_DARK_VORTEX_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                                        NEPTUNE_SURFACE
                                                )
                                        ),
                                        // Supersonic Wind Plains: continuous Neptune Surface with wind-swept Cryo Slate and Nitrogen Ice streaks
                                        SurfaceRules.ifTrue(
                                                isSupersonicWindPlains,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                                        SurfaceRules.ifTrue(finePatchNoise, NEPTUNE_NITROGEN_ICE),
                                                        NEPTUNE_SURFACE
                                                )
                                        ),
                                        // Triton Geyser Fields: Nitrogen Ice and Cryo Slate blended into Neptune Surface
                                        SurfaceRules.ifTrue(
                                                isTritonGeyserFields,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_NITROGEN_ICE),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                                        NEPTUNE_SURFACE
                                                )
                                        ),
                                        // Nitrogen Ice Shelves: Nitrogen Ice gradually blended with Neptune Surface and Sub-Surface
                                        SurfaceRules.ifTrue(
                                                isNitrogenIceShelves,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_NITROGEN_ICE),
                                                        SurfaceRules.ifTrue(finePatchNoise, NEPTUNE_SUB_SURFACE),
                                                        NEPTUNE_SURFACE
                                                )
                                        ),
                                        // Cobalt Storm Highlands: Dark Vortex Rock and Nitrogen Ice blended into Neptune Surface
                                        SurfaceRules.ifTrue(
                                                isCobaltStormHighlands,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_DARK_VORTEX_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_NITROGEN_ICE),
                                                        NEPTUNE_SURFACE
                                                )
                                        ),
                                        // Default planet surface fallback
                                        SurfaceRules.ifTrue(
                                                isAbove92,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(finePatchNoise, NEPTUNE_NITROGEN_ICE),
                                                        NEPTUNE_SURFACE
                                                )
                                        ),
                                        NEPTUNE_SUB_SURFACE
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                isNitrogenIceShelves,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_NITROGEN_ICE),
                                                        NEPTUNE_SUB_SURFACE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                isDarkSpotVortex,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_DARK_VORTEX_ROCK),
                                                        NEPTUNE_SUB_SURFACE
                                                )
                                        ),
                                        NEPTUNE_SUB_SURFACE
                                )
                        )
                )
        );

        // 3. Realistic subterranean 3D cave stratigraphy (below Y=80, using only Neptune geological rocks)
        RuleSource caveRules = SurfaceRules.ifTrue(
                isBelow80,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                isDiamondRainCaverns,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_ABYSSAL_MANTLE_ROCK),
                                                        NEPTUNE_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                                        NEPTUNE_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_NITROGEN_ICE),
                                                        NEPTUNE_CRYO_SLATE
                                                )
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isCryoMagmaTubes,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_NITROGEN_ICE),
                                                        NEPTUNE_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                                        NEPTUNE_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_NITROGEN_ICE),
                                                        NEPTUNE_STONE
                                                )
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isAbyssalMantleDepths,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_ABYSSAL_MANTLE_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                                        NEPTUNE_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_ABYSSAL_MANTLE_ROCK),
                                                        NEPTUNE_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_ABYSSAL_MANTLE_ROCK),
                                                        NEPTUNE_STONE
                                                )
                                        )
                                )
                        ),
                        // Deep abyssal mantle transition near bedrock (Y < 32)
                        SurfaceRules.ifTrue(
                                isBelow32,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, NEPTUNE_ABYSSAL_MANTLE_ROCK),
                                        SurfaceRules.ifTrue(secondaryBlendNoise, NEPTUNE_CRYO_SLATE),
                                        NEPTUNE_STONE
                                )
                        )
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                caveRules,
                NEPTUNE_STONE
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
