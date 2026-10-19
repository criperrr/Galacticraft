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
import dev.galacticraft.mod.world.gen.GCNoiseData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MercurySurfaceRules {
    // Surface biomes
    private static final ConditionSource IS_POLAR = biome(GCBiomes.Mercury.POLAR_DARK_CRATERS);
    private static final ConditionSource IS_CALORIS = biome(GCBiomes.Mercury.CALORIS_BASIN);
    private static final ConditionSource IS_CHAOTIC = biome(GCBiomes.Mercury.CHAOTIC_TERRAIN);
    private static final ConditionSource IS_SMOOTH_LAVA = biome(GCBiomes.Mercury.SMOOTH_LAVA_PLAINS);
    private static final ConditionSource IS_INTER_CRATER = biome(GCBiomes.Mercury.INTER_CRATER_PLAINS);

    // Subterranean 3D biomes
    private static final ConditionSource IS_LAVA_TUBES = biome(GCBiomes.Mercury.COLOSSAL_LAVA_TUBES);
    private static final ConditionSource IS_MEGABRECCIA = biome(GCBiomes.Mercury.MEGABRECCIA_CAVERNS);
    private static final ConditionSource IS_TECTONIC_CHASM = biome(GCBiomes.Mercury.TECTONIC_CHASMS);
    private static final ConditionSource IS_MAGMA_DEPTHS = biome(GCBiomes.Mercury.MAGMA_DEPTHS);
    private static final ConditionSource IS_HOLLOW_LABYRINTH = biome(GCBiomes.Mercury.HOLLOW_LABYRINTH);

    private static final RuleSource BEDROCK = block(Blocks.BEDROCK);
    private static final RuleSource LAVA = block(Blocks.LAVA);
    private static final RuleSource PACKED_ICE = block(Blocks.PACKED_ICE);

    private static final RuleSource MERCURY_REGOLITH = block(GCBlocks.MERCURY_REGOLITH);
    private static final RuleSource MERCURY_COMPACTED_REGOLITH = block(GCBlocks.MERCURY_COMPACTED_REGOLITH);
    private static final RuleSource MERCURY_FROST_REGOLITH = block(GCBlocks.MERCURY_FROST_REGOLITH);
    private static final RuleSource MERCURY_HOLLOW_DEPOSIT = block(GCBlocks.MERCURY_HOLLOW_DEPOSIT);
    private static final RuleSource MERCURY_BASALT = block(GCBlocks.MERCURY_BASALT);
    private static final RuleSource MERCURY_BASALT_BRICK = block(GCBlocks.MERCURY_BASALT_BRICK);
    private static final RuleSource MERCURY_SHOCK_BRECCIA = block(GCBlocks.MERCURY_SHOCK_BRECCIA);
    private static final RuleSource MERCURY_GRAPHITE_ROCK = block(GCBlocks.MERCURY_GRAPHITE_ROCK);
    private static final RuleSource MAGMA_BLOCK = block(Blocks.MAGMA_BLOCK);

    /**
     * Top surface floor rules:
     * - Polar Dark Craters: Frost regolith with pure water ice sheets on crater bottoms (Y <= 100)
     * - Caloris Basin: Exhumed dark graphite (LRM) in deep crater floors, smooth basalt elsewhere
     * - Hollows: High-albedo volatile deposits scattered on depression slopes
     * - Plains & Chaos: Dark ultramafic regolith
     */
    private static final RuleSource FLOOR_MATERIAL = SurfaceRules.sequence(
            SurfaceRules.ifTrue(
                    IS_POLAR,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(100), 0)), PACKED_ICE),
                            MERCURY_FROST_REGOLITH
                    )
            ),
            SurfaceRules.ifTrue(
                    IS_CALORIS,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(85), 0)), MERCURY_GRAPHITE_ROCK),
                            MERCURY_REGOLITH
                    )
            ),
            SurfaceRules.ifTrue(
                    SurfaceRules.noiseCondition(GCNoiseData.MOON_LOWLAND_NOISE, 0.45, 1.0),
                    MERCURY_HOLLOW_DEPOSIT
            ),
            MERCURY_REGOLITH
    );

    /**
     * Subsurface rules (1-4 blocks below floor):
     * - Steep slopes: Exposes basalt directly
     * - General: Compacted regolith
     */
    private static final RuleSource SECONDARY_MATERIAL = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), MERCURY_BASALT),
            MERCURY_COMPACTED_REGOLITH
    );

    private static final RuleSource SURFACE_GENERATION = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, FLOOR_MATERIAL),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SECONDARY_MATERIAL)
    );

    /**
     * Subterranean monumental cave floor & feature rules:
     * Geological stratification modeled on planetary thermal contraction, impact excavation, and volatile venting.
     * Absolutely NO ores carpet cave floors; ores generate strictly as natural 3D veins via Feature.ORE.
     * Absolutely NO regolith underground; subterranean floors are basalt, shock breccia, or graphite.
     */
     private static final RuleSource UNDERGROUND_DECORATION = SurfaceRules.sequence(
            // Magma Depths: Basaltic lava pools on deep basal floor (Y <= -48), magma block crusts/margins, and solid basalt floors
            SurfaceRules.ifTrue(
                    IS_MAGMA_DEPTHS,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(-48), 0)), LAVA),
                            // Magma shores and volcanic scoria crusts in lower basin (Y <= -40)
                            SurfaceRules.ifTrue(
                                    SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(-40), 0)),
                                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.MERCURY_BASE_NOISE, -0.05, 1.0), MAGMA_BLOCK)
                            ),
                            // Magma veins and crusts on higher floor (Y > -40)
                            SurfaceRules.ifTrue(
                                    SurfaceRules.noiseCondition(GCNoiseData.MERCURY_BASE_NOISE, 0.25, 1.0),
                                    MAGMA_BLOCK
                            ),
                            MERCURY_BASALT
                    )
            ),
            // Hollow Labyrinth: High-albedo white sublimation deposits coating cavern floors and porous graphite walls
            SurfaceRules.ifTrue(
                    IS_HOLLOW_LABYRINTH,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(
                                    SurfaceRules.noiseCondition(GCNoiseData.MERCURY_HOLLOW_SPONGE, -0.20, 1.0),
                                    MERCURY_HOLLOW_DEPOSIT
                            ),
                            SurfaceRules.ifTrue(
                                    SurfaceRules.noiseCondition(GCNoiseData.MERCURY_BASE_NOISE, 0.30, 1.0),
                                    MERCURY_GRAPHITE_ROCK
                            ),
                            MERCURY_BASALT
                    )
            ),
            // Megabreccia Caverns: Impact breccia floor with intercalated pure graphite (LRM) strata
            SurfaceRules.ifTrue(
                    IS_MEGABRECCIA,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.MERCURY_MEGABRECCIA_CHEESE, 0.25, 1.0), MERCURY_GRAPHITE_ROCK),
                            MERCURY_SHOCK_BRECCIA
                    )
            ),
            // Colossal Lava Tubes: Polished basalt floor and cooling terrace basalt bricks
            SurfaceRules.ifTrue(
                    IS_LAVA_TUBES,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(
                                    SurfaceRules.noiseCondition(GCNoiseData.MERCURY_LAVA_TUBES, 0.35, 1.0),
                                    MERCURY_BASALT_BRICK
                            ),
                            MERCURY_BASALT
                    )
            ),
            // Tectonic Chasms: Deep fault plunge with sheared shock breccia and basalt walls
            SurfaceRules.ifTrue(
                    IS_TECTONIC_CHASM,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(
                                    SurfaceRules.noiseCondition(GCNoiseData.MERCURY_BASE_NOISE, 0.35, 1.0),
                                    MERCURY_SHOCK_BRECCIA
                            ),
                            MERCURY_BASALT
                    )
            ),
            // Universal fallback for any other subterranean cave floor: Basalt or deep Shock Breccia
            SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(40), 0), MERCURY_BASALT),
            MERCURY_SHOCK_BRECCIA
    );

    public static final RuleSource MERCURY = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        return SurfaceRules.sequence(
                // 1. Bedrock at world floor (Y = -64)
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),

                // 2. Surface regolith layers (strictly for exterior planetary surface Y >= 95, never inside deep caves)
                SurfaceRules.ifTrue(
                        SurfaceRules.abovePreliminarySurface(),
                        SurfaceRules.ifTrue(
                                SurfaceRules.yBlockCheck(VerticalAnchor.absolute(95), 0),
                                SURFACE_GENERATION
                        )
                ),

                // 3. Underground cavern floor and feature decoration (NO regolith, NO solid ore blankets)
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, UNDERGROUND_DECORATION),

                // 4. Underground cavern ceiling decoration (sublimation crusts and graphite veins in hollows/megabreccia)
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_CEILING,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        IS_HOLLOW_LABYRINTH,
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.noiseCondition(GCNoiseData.MERCURY_HOLLOW_SPONGE, -0.10, 1.0),
                                                MERCURY_HOLLOW_DEPOSIT
                                        )
                                ),
                                SurfaceRules.ifTrue(
                                        IS_MEGABRECCIA,
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.noiseCondition(GCNoiseData.MERCURY_MEGABRECCIA_CHEESE, 0.30, 1.0),
                                                MERCURY_GRAPHITE_ROCK
                                        )
                                )
                        )
                ),

                // 5. Dense Shock Breccia in deep crust (Y = -64 to 40)
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("mercury_shock_breccia", VerticalAnchor.absolute(35), VerticalAnchor.absolute(45)), MERCURY_SHOCK_BRECCIA),

                // 6. Default Base Rock is Ultramafic Basalt (Y = 40 to 120+)
                MERCURY_BASALT
        );
    }

    @Contract("_ -> new")
    private static @NotNull RuleSource block(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    @Contract("_ -> new")
    public static SurfaceRules.@NotNull ConditionSource biome(@NotNull TagKey<Biome> biome) {
        return new BiomeTagRule(biome);
    }

    @SafeVarargs
    @Contract("_ -> new")
    public static SurfaceRules.@NotNull ConditionSource biome(@NotNull ResourceKey<Biome> @NotNull ... keys) {
        return SurfaceRules.isBiome(keys);
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("mercury"), MapCodec.unit(MERCURY));
    }
}
