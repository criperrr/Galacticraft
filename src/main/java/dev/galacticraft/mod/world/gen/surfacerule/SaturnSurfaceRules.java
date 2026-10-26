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

public class SaturnSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);

    // Dedicated Kronian lithology from GCBlocks
    private static final RuleSource SATURN_SURFACE = state(GCBlocks.SATURN_SURFACE);
    private static final RuleSource SATURN_SUB_SURFACE = state(GCBlocks.SATURN_SUB_SURFACE);
    private static final RuleSource SATURN_STONE = state(GCBlocks.SATURN_STONE);
    private static final RuleSource SATURN_GOLDEN_BAND_ROCK = state(GCBlocks.SATURN_GOLDEN_BAND_ROCK);
    private static final RuleSource SATURN_RING_ICE_ROCK = state(GCBlocks.SATURN_RING_ICE_ROCK);
    private static final RuleSource SATURN_HYDROCARBON_SLATE = state(GCBlocks.SATURN_HYDROCARBON_SLATE);
    private static final RuleSource SATURN_HELIUM_MANTLE_ROCK = state(GCBlocks.SATURN_HELIUM_MANTLE_ROCK);

    public static final RuleSource SATURN = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        // Cheap Y-bound conditions first (base terrain around Y=75..85)
        ConditionSource isAbove68 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(68), 0);
        ConditionSource isAbove90 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(90), 0);
        ConditionSource isAbove115 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(115), 0);
        ConditionSource isBelow56 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(56), 0));
        ConditionSource isBelow24 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(24), 0));

        // Biome predicates
        ConditionSource isHexagonPolarPlateau = biome(GCBiomes.Saturn.HEXAGON_POLAR_PLATEAU);
        ConditionSource isGoldenHazePlains = biome(GCBiomes.Saturn.GOLDEN_HAZE_PLAINS);
        ConditionSource isRingShadowShatterlands = biome(GCBiomes.Saturn.RING_SHADOW_SHATTERLANDS);
        ConditionSource isAmmoniaDunes = biome(GCBiomes.Saturn.AMMONIA_DUNES);
        ConditionSource isHydrocarbonBasin = biome(GCBiomes.Saturn.HYDROCARBON_BASIN);
        ConditionSource isHeliumRainCaverns = biome(GCBiomes.Saturn.HELIUM_RAIN_CAVERNS);
        ConditionSource isRingIceGlaciers = biome(GCBiomes.Saturn.RING_ICE_GLACIERS);
        ConditionSource isMetallicMantleTrench = biome(GCBiomes.Saturn.METALLIC_MANTLE_TRENCH);

        // Gradual noise conditions for smooth transitions between neighboring biomes
        ConditionSource primaryBlendNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.10D, 0.45D);
        ConditionSource secondaryBlendNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.05D, 0.55D);
        ConditionSource finePatchNoise = SurfaceRules.noiseCondition(Noises.GRAVEL, 0.12D, 0.65D);
        ConditionSource bandStrataNoise = SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, -0.05D, 0.40D);

        // 1. Steep cliff rules BEFORE ON_FLOOR so hexagonal terraces and canyon walls show stratified bands
        RuleSource steepCliffRules = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                isAbove115,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_RING_ICE_ROCK),
                                        SATURN_GOLDEN_BAND_ROCK
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isAbove90,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(bandStrataNoise, SATURN_GOLDEN_BAND_ROCK),
                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_HYDROCARBON_SLATE),
                                        SATURN_SUB_SURFACE
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isBelow56,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_HELIUM_MANTLE_ROCK),
                                        SATURN_HYDROCARBON_SLATE
                                )
                        ),
                        SATURN_STONE
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
                                        // High polar peaks (Y >= 115) blend Ring Ice Rock with Golden Band Rock and Saturn Surface
                                        SurfaceRules.ifTrue(
                                                isAbove115,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_RING_ICE_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_GOLDEN_BAND_ROCK),
                                                        SATURN_SURFACE
                                                )
                                        ),
                                        // Hexagon Polar Plateau: gradual blend of Golden Band Rock, Hydrocarbon Slate, and Saturn Surface
                                        SurfaceRules.ifTrue(
                                                isHexagonPolarPlateau,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_GOLDEN_BAND_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_HYDROCARBON_SLATE),
                                                        SATURN_SURFACE
                                                )
                                        ),
                                        // Golden Haze Plains: continuous Saturn Surface with soft Golden Band Rock waves
                                        SurfaceRules.ifTrue(
                                                isGoldenHazePlains,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_GOLDEN_BAND_ROCK),
                                                        SATURN_SURFACE
                                                )
                                        ),
                                        // Ring Shadow Shatterlands: Ring Ice Rock and Hydrocarbon Slate blended into Saturn Surface
                                        SurfaceRules.ifTrue(
                                                isRingShadowShatterlands,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_RING_ICE_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_HYDROCARBON_SLATE),
                                                        SATURN_SURFACE
                                                )
                                        ),
                                        // Ammonia Dunes: Saturn Surface and Golden Band Rock with subtle Sub-Surface ripples
                                        SurfaceRules.ifTrue(
                                                isAmmoniaDunes,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_GOLDEN_BAND_ROCK),
                                                        SurfaceRules.ifTrue(finePatchNoise, SATURN_SUB_SURFACE),
                                                        SATURN_SURFACE
                                                )
                                        ),
                                        // Hydrocarbon Basin: Hydrocarbon Slate blended with Golden Band Rock and Saturn Surface
                                        SurfaceRules.ifTrue(
                                                isHydrocarbonBasin,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_HYDROCARBON_SLATE),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_GOLDEN_BAND_ROCK),
                                                        SATURN_SURFACE
                                                )
                                        ),
                                        // Ring Ice Glaciers (when exposed on surface): Ring Ice Rock blended with Saturn Surface
                                        SurfaceRules.ifTrue(
                                                isRingIceGlaciers,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_RING_ICE_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_GOLDEN_BAND_ROCK),
                                                        SATURN_SURFACE
                                                )
                                        ),
                                        // Default planet surface fallback
                                        SurfaceRules.ifTrue(
                                                isAbove68,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(finePatchNoise, SATURN_GOLDEN_BAND_ROCK),
                                                        SATURN_SURFACE
                                                )
                                        ),
                                        SATURN_SUB_SURFACE
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                isRingShadowShatterlands,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_RING_ICE_ROCK),
                                                        SATURN_SUB_SURFACE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                isHydrocarbonBasin,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_HYDROCARBON_SLATE),
                                                        SATURN_SUB_SURFACE
                                                )
                                        ),
                                        SATURN_SUB_SURFACE
                                )
                        )
                )
        );

        // 3. Subterranean 3D cave rules (below Y=56, cheap Y check before noise)
        RuleSource caveRules = SurfaceRules.ifTrue(
                isBelow56,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(
                                isHeliumRainCaverns,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_HELIUM_MANTLE_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_HYDROCARBON_SLATE),
                                                        SATURN_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_HELIUM_MANTLE_ROCK),
                                                        SATURN_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_GOLDEN_BAND_ROCK),
                                                        SATURN_STONE
                                                )
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isRingIceGlaciers,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_RING_ICE_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_HYDROCARBON_SLATE),
                                                        SATURN_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_RING_ICE_ROCK),
                                                        SATURN_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_RING_ICE_ROCK),
                                                        SATURN_STONE
                                                )
                                        )
                                )
                        ),
                        SurfaceRules.ifTrue(
                                isMetallicMantleTrench,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_HELIUM_MANTLE_ROCK),
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_HYDROCARBON_SLATE),
                                                        SATURN_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.UNDER_FLOOR,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_HELIUM_MANTLE_ROCK),
                                                        SATURN_STONE
                                                )
                                        ),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.ON_CEILING,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_HELIUM_MANTLE_ROCK),
                                                        SATURN_STONE
                                                )
                                        )
                                )
                        ),
                        // Deep helium mantle transition near bedrock (Y < 24)
                        SurfaceRules.ifTrue(
                                isBelow24,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryBlendNoise, SATURN_HELIUM_MANTLE_ROCK),
                                        SurfaceRules.ifTrue(secondaryBlendNoise, SATURN_HYDROCARBON_SLATE),
                                        SATURN_STONE
                                )
                        )
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                caveRules,
                SATURN_STONE
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
