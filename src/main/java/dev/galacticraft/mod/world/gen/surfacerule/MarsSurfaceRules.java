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

import com.mojang.serialization.MapCodec;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.world.biome.GCBiomes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MarsSurfaceRules {
    private static final SurfaceRules.RuleSource BEDROCK = block(Blocks.BEDROCK);
    private static final SurfaceRules.RuleSource BLUE_ICE = block(Blocks.BLUE_ICE);
    private static final SurfaceRules.RuleSource MAGMA_BLOCK = block(Blocks.MAGMA_BLOCK);
    public static final SurfaceRules.RuleSource SURFACE_ROCK = block(GCBlocks.MARS_SURFACE_ROCK);
    public static final SurfaceRules.RuleSource SUB_SURFACE_ROCK = block(GCBlocks.MARS_SUB_SURFACE_ROCK);
    public static final SurfaceRules.RuleSource STONE = block(GCBlocks.MARS_STONE);
    public static final SurfaceRules.RuleSource BASALT = block(GCBlocks.MARS_BASALT);
    public static final SurfaceRules.RuleSource SILTSTONE = block(GCBlocks.MARS_SILTSTONE);
    public static final SurfaceRules.RuleSource BRECCIA = block(GCBlocks.MARS_BRECCIA);
    public static final SurfaceRules.RuleSource BASALTIC_SAND = block(GCBlocks.MARS_BASALTIC_SAND);
    public static final SurfaceRules.RuleSource FROST_REGOLITH = block(GCBlocks.MARS_FROST_REGOLITH);
    public static final SurfaceRules.RuleSource DEEP_STONE = block(GCBlocks.MARS_DEEP_STONE);
    public static final SurfaceRules.RuleSource DENSE_ICE = block(GCBlocks.DENSE_ICE);

    private static final SurfaceRules.ConditionSource CLIFF_SLOPE = new SlopeRangeCondition(4, 99);
    private static final SurfaceRules.ConditionSource SCREE_SLOPE = new SlopeRangeCondition(2, 3);
    private static final SurfaceRules.ConditionSource CANYON_STRATA_WIDE = new WarpedStrataCondition(14, 5, 4.5);
    private static final SurfaceRules.ConditionSource CANYON_STRATA_THIN = new WarpedStrataCondition(9, 2, 3.0);
    private static final SurfaceRules.ConditionSource POLAR_STRATA = new WarpedStrataCondition(8, 3, 2.5);

    // 1. Valles Marineris: warped sedimentary siltstone/stone cliff bands + breccia talus scree + sandy rift beds
    private static final SurfaceRules.RuleSource CANYON_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(CLIFF_SLOPE, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(CANYON_STRATA_WIDE, SILTSTONE),
                    SurfaceRules.ifTrue(CANYON_STRATA_THIN, BASALT),
                    STONE
            )),
            SurfaceRules.ifTrue(SCREE_SLOPE, SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, BRECCIA)),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25, 1.0), BASALTIC_SAND),
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.20, 0.25), SILTSTONE),
                    SURFACE_ROCK
            )),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SUB_SURFACE_ROCK),
            STONE
    );

    // 2. Polar Layered Deposits (PLD): alternating Dense Ice and Frost Regolith strata
    private static final SurfaceRules.RuleSource POLAR_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(CLIFF_SLOPE, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(POLAR_STRATA, DENSE_ICE),
                    FROST_REGOLITH
            )),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.15, 1.0), DENSE_ICE),
                    FROST_REGOLITH
            )),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, FROST_REGOLITH),
            SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, SUB_SURFACE_ROCK),
            STONE
    );

    // 3. Tharsis Volcanic Province / Olympus Mons: dark volcanic basalt shields, breccia scree, frost on high calderas
    private static final SurfaceRules.RuleSource THARSIS_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(CLIFF_SLOPE, BASALT),
            SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(215), 0), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.15, 1.0), FROST_REGOLITH),
                            BASALT
                    )),
                    SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, BASALT)
            )),
            SurfaceRules.ifTrue(SCREE_SLOPE, SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, BRECCIA)),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, -0.10, 1.0), BASALT),
                    SURFACE_ROCK
            )),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, BASALT),
            STONE
    );

    // 4. Martian Highlands: stratified cliffs, breccia scree, frost permafrost on high peaks (Y >= 175)
    private static final SurfaceRules.RuleSource HIGHLANDS_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(CLIFF_SLOPE, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(CANYON_STRATA_WIDE, SILTSTONE),
                    STONE
            )),
            SurfaceRules.ifTrue(SCREE_SLOPE, SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, BRECCIA)),
            SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(175), 0), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25, 1.0), DENSE_ICE),
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.10, 1.0), FROST_REGOLITH),
                            SURFACE_ROCK
                    )),
                    SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SUB_SURFACE_ROCK)
            )),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SURFACE_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SUB_SURFACE_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, SUB_SURFACE_ROCK),
            STONE
    );

    // 5. Basaltic Dune Seas: dark aeolian basaltic sand sheets over compacted regolith (no zebra-stripe stone bands)
    private static final SurfaceRules.RuleSource DUNE_SEA_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.45, 1.0), SURFACE_ROCK),
                    BASALTIC_SAND
            )),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, BASALTIC_SAND),
            SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, SUB_SURFACE_ROCK),
            STONE
    );

    // 6. Impact Basins (Jezero / Hellas): crater breccia floors, siltstone rims, and dusty regolith
    private static final SurfaceRules.RuleSource IMPACT_BASIN_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(CLIFF_SLOPE, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(CANYON_STRATA_THIN, SILTSTONE),
                    STONE
            )),
            SurfaceRules.ifTrue(SCREE_SLOPE, SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, BRECCIA)),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.20, 1.0), BRECCIA),
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -1.0, -0.45), SILTSTONE),
                    SURFACE_ROCK
            )),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SUB_SURFACE_ROCK),
            STONE
    );

    // 7. Ferruginous Plains: classic rusty Martian regolith with scree gullies and siltstone outcrops
    private static final SurfaceRules.RuleSource FLAT_SURFACE = SurfaceRules.sequence(
            SurfaceRules.ifTrue(CLIFF_SLOPE, STONE),
            SurfaceRules.ifTrue(SCREE_SLOPE, SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, BRECCIA)),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -1.0, -0.58), SILTSTONE),
                    SURFACE_ROCK
            )),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SUB_SURFACE_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, SUB_SURFACE_ROCK),
            STONE
    );

    private static final SurfaceRules.RuleSource SURFACE_GENERATION = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_POLAR_ICE_CAP), POLAR_SURFACE),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_CANYON), CANYON_SURFACE),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_THARSIS), THARSIS_SURFACE),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_HIGHLANDS), HIGHLANDS_SURFACE),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_DUNE_SEA), DUNE_SEA_SURFACE),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_IMPACT_BASIN), IMPACT_BASIN_SURFACE),
            FLAT_SURFACE
    );

    // Subterranean interior floor rules (never carpet ores; purely geological lithology)
    private static final SurfaceRules.RuleSource CAVE_FLOOR_RULES = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_LAVA_TUBES), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(42), 0)),
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.45, 1.0), MAGMA_BLOCK)),
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.35, 1.0), BRECCIA),
                    BASALT
            )),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_ICE_CAVERNS), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25, 1.0), BLUE_ICE),
                    DENSE_ICE
            )),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_DEEP_CAVERNS), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.42, 1.0), BASALTIC_SAND),
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, 0.22, 1.0), BRECCIA),
                    DEEP_STONE
            ))
    );

    // Subterranean interior ceiling rules
    private static final SurfaceRules.RuleSource CAVE_CEILING_RULES = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_LAVA_TUBES), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(CANYON_STRATA_THIN, SILTSTONE),
                    BASALT
            )),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_ICE_CAVERNS), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.10, 1.0), DENSE_ICE),
                    FROST_REGOLITH
            )),
            SurfaceRules.ifTrue(SurfaceRules.isBiome(GCBiomes.Mars.MARS_DEEP_CAVERNS), SurfaceRules.sequence(
                    SurfaceRules.ifTrue(CANYON_STRATA_WIDE, SILTSTONE),
                    DEEP_STONE
            ))
    );

    public static final SurfaceRules.RuleSource MARS = createDefaultRule();

    public static @NotNull SurfaceRules.RuleSource createDefaultRule() {
        return SurfaceRules.sequence(
                // 1. Bedrock floor [-64, -59]
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                // 2. Exterior surface ONLY (isolated from deep caves)
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SURFACE_GENERATION),
                // 3. Subterranean cave interiors (floor & ceiling)
                SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(0, true, 0, CaveSurface.FLOOR), CAVE_FLOOR_RULES),
                SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.CEILING), CAVE_CEILING_RULES),
                // 4. Deep Martian mantle stratification below Y = 14
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("mars_deep_stone", VerticalAnchor.absolute(0), VerticalAnchor.absolute(14)), DEEP_STONE),
                STONE
        );
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_CONDITION, Constant.id("slope_range"), SlopeRangeCondition.CODEC_MAP);
        Registry.register(BuiltInRegistries.MATERIAL_CONDITION, Constant.id("warped_strata"), WarpedStrataCondition.CODEC_MAP);
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("mars"), MapCodec.unit(MARS));
    }

    @Contract("_ -> new")
    private static @NotNull SurfaceRules.RuleSource block(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}
