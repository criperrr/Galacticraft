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

public class UranusSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);

    // Dedicated Uranian lithology from GCBlocks
    private static final RuleSource URANUS_SURFACE = state(GCBlocks.URANUS_SURFACE);
    private static final RuleSource URANUS_SUB_SURFACE = state(GCBlocks.URANUS_SUB_SURFACE);
    private static final RuleSource URANUS_STONE = state(GCBlocks.URANUS_STONE);
    private static final RuleSource URANUS_METHANE_ICE = state(GCBlocks.URANUS_METHANE_ICE);
    private static final RuleSource URANUS_VERONA_SCARP_ROCK = state(GCBlocks.URANUS_VERONA_SCARP_ROCK);
    private static final RuleSource URANUS_SUPERIONIC_MANTLE_ROCK = state(GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK);
    private static final RuleSource URANUS_DIAMOND_BEARING_ROCK = state(GCBlocks.URANUS_DIAMOND_BEARING_ROCK);

    public static final RuleSource URANUS = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        // Cheap Y-bound conditions first (base terrain around Y=100..110)
        ConditionSource isAbove92 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(92), 0);
        ConditionSource isAbove118 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(118), 0);
        ConditionSource isAbove140 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(140), 0);
        ConditionSource isBelow80 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(80), 0));
        ConditionSource isBelow32 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(32), 0));

        // Biome predicates
        ConditionSource isMirandaCliffscarps = biome(GCBiomes.Uranus.MIRANDA_CLIFFSCARPS);
        ConditionSource isCyanMethaneGlaciers = biome(GCBiomes.Uranus.CYAN_METHANE_GLACIERS);
        ConditionSource isTiltedPolarWastes = biome(GCBiomes.Uranus.TILTED_POLAR_WASTES);
        ConditionSource isRimeIcePlains = biome(GCBiomes.Uranus.RIME_ICE_PLAINS);
        ConditionSource isCryovolcanicRift = biome(GCBiomes.Uranus.CRYOVOLCANIC_RIFT);
        ConditionSource isSuperionicIceOcean = biome(GCBiomes.Uranus.SUPERIONIC_ICE_OCEAN);
        ConditionSource isDiamondMantleCaverns = biome(GCBiomes.Uranus.DIAMOND_MANTLE_CAVERNS);
        ConditionSource isSubglacialAmmoniaSea = biome(GCBiomes.Uranus.SUBGLACIAL_AMMONIA_SEA);

        // Gradual noise conditions for seamless transitions between neighboring biomes
        ConditionSource primaryBlendNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.10D, 0.45D);
        ConditionSource secondaryBlendNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.05D, 0.55D);
        ConditionSource finePatchNoise = SurfaceRules.noiseCondition(Noises.GRAVEL, 0.12D, 0.65D);
        ConditionSource scarpStrataNoise = SurfaceRules.noiseCondition(Noises.ICE, -0.05D, 0.40D);

        // 1. Steep cliff rules BEFORE ON_FLOOR so Verona Rupes fault cliffs expose stratified ice and scarp rock
        RuleSource steepCliffRules = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                isAbove140,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_METHANE_ICE),
                                        URANUS_VERONA_SCARP_ROCK
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isAbove118,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(scarpStrataNoise, URANUS_VERONA_SCARP_ROCK),
                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_METHANE_ICE),
                                        URANUS_SUB_SURFACE
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isBelow80,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_SUPERIONIC_MANTLE_ROCK),
                                        URANUS_VERONA_SCARP_ROCK
                                )
                        ),
                        URANUS_STONE
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
                                        // High fault peaks (Y >= 140) blend Methane Ice with Verona Scarp Rock and Uranus Surface
                                        SurfaceRules.ifTrue(
                                                isAbove140,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_METHANE_ICE),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_VERONA_SCARP_ROCK),
                                                        URANUS_SURFACE
                                                )
                                        ),
                                        // Miranda Cliffscarps: Verona Scarp Rock and Methane Ice blended into Uranus Surface
                                        SurfaceRules.ifTrue(
                                                isMirandaCliffscarps,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_VERONA_SCARP_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_METHANE_ICE),
                                                        URANUS_SURFACE
                                                )
                                        ),
                                        // Cyan Methane Glaciers: Methane Ice gradually blended with Uranus Surface
                                        SurfaceRules.ifTrue(
                                                isCyanMethaneGlaciers,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_METHANE_ICE),
                                                        SurfaceRules.ifTrue(finePatchNoise, URANUS_SUB_SURFACE),
                                                        URANUS_SURFACE
                                                )
                                        ),
                                        // Tilted Polar Wastes: Uranus Surface blended with Methane Ice and Verona Scarp Rock
                                        SurfaceRules.ifTrue(
                                                isTiltedPolarWastes,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_METHANE_ICE),
                                                        SurfaceRules.ifTrue(finePatchNoise, URANUS_VERONA_SCARP_ROCK),
                                                        URANUS_SURFACE
                                                )
                                        ),
                                        // Rime Ice Plains: predominantly Uranus Surface with gentle Methane Ice drifts
                                        SurfaceRules.ifTrue(
                                                isRimeIcePlains,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_METHANE_ICE),
                                                        URANUS_SURFACE
                                                )
                                        ),
                                        // Cryovolcanic Rift: Verona Scarp Rock and Superionic Mantle outcrops blended into Uranus Surface
                                        SurfaceRules.ifTrue(
                                                isCryovolcanicRift,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_VERONA_SCARP_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_SUPERIONIC_MANTLE_ROCK),
                                                        URANUS_SURFACE
                                                )
                                        ),
                                        // Default planet surface fallback
                                        SurfaceRules.ifTrue(
                                                isAbove92,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(finePatchNoise, URANUS_METHANE_ICE),
                                                        URANUS_SURFACE
                                                )
                                        ),
                                        URANUS_SUB_SURFACE
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                isCyanMethaneGlaciers,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_METHANE_ICE),
                                                        URANUS_SUB_SURFACE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                isMirandaCliffscarps,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_VERONA_SCARP_ROCK),
                                                        URANUS_SUB_SURFACE
                                                )
                                        ),
                                        URANUS_SUB_SURFACE
                                )
                        )
                )
        );

        // 3. Subterranean 3D cave rules (below Y=80, cheap Y check before noise)
        RuleSource caveRules = SurfaceRules.ifTrue(
                isBelow80,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                isSuperionicIceOcean,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_SUPERIONIC_MANTLE_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_METHANE_ICE),
                                                        URANUS_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_SUPERIONIC_MANTLE_ROCK),
                                                        URANUS_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_METHANE_ICE),
                                                        URANUS_STONE
                                                )
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isDiamondMantleCaverns,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_DIAMOND_BEARING_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_SUPERIONIC_MANTLE_ROCK),
                                                        URANUS_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_DIAMOND_BEARING_ROCK),
                                                        URANUS_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_DIAMOND_BEARING_ROCK),
                                                        URANUS_STONE
                                                )
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isSubglacialAmmoniaSea,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_VERONA_SCARP_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_SUPERIONIC_MANTLE_ROCK),
                                                        URANUS_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_METHANE_ICE),
                                                        URANUS_STONE
                                                )
                                        )
                                )
                        ),
                        // Deep diamond-bearing mantle transition near bedrock (Y < 32)
                        SurfaceRules.ifTrue(
                                isBelow32,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, URANUS_DIAMOND_BEARING_ROCK),
                                        SurfaceRules.ifTrue(secondaryBlendNoise, URANUS_SUPERIONIC_MANTLE_ROCK),
                                        URANUS_STONE
                                )
                        )
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                caveRules,
                URANUS_STONE
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
