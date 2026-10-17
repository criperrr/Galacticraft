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

public class MoonSurfaceRules {
    private static final ConditionSource IS_MARE = biome(GCBiomes.Moon.BASALTIC_MARE);
    private static final ConditionSource IS_OLIVINE_SPIKES = biome(GCBiomes.Moon.OLIVINE_SPIKES);
    private static final ConditionSource IS_COMET_TUNDRA = biome(GCBiomes.Moon.COMET_TUNDRA);
    private static final ConditionSource IS_HIGHLANDS = biome(GCBiomes.Moon.LUNAR_HIGHLANDS);
    private static final ConditionSource IS_LOWLANDS = biome(GCBiomes.Moon.LUNAR_LOWLANDS);

    private static final RuleSource BEDROCK = block(Blocks.BEDROCK);
    private static final RuleSource LUNASLATE = block(GCBlocks.LUNASLATE);
    private static final RuleSource MOON_DIRT = block(GCBlocks.MOON_DIRT);
    private static final RuleSource MOON_ROCK = block(GCBlocks.MOON_ROCK);
    private static final RuleSource MOON_SURFACE_ROCK = block(GCBlocks.MOON_SURFACE_ROCK);
    private static final RuleSource MOON_TURF = block(GCBlocks.MOON_TURF);
    private static final RuleSource MOON_BASALT = block(GCBlocks.MOON_BASALT);
    private static final RuleSource OLIVINE_BASALT = block(GCBlocks.OLIVINE_BASALT);
    private static final RuleSource RICH_OLIVINE_BASALT = block(GCBlocks.RICH_OLIVINE_BASALT);
    private static final RuleSource DENSE_ICE = block(GCBlocks.DENSE_ICE);

    private static final RuleSource MARE_FLOOR = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.EROSION, 0.035, 0.068), MOON_SURFACE_ROCK),
            MOON_BASALT
    );

    private static final RuleSource OLIVINE_SPIKES_FLOOR = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.EROSION, 0.035, 0.075), RICH_OLIVINE_BASALT),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.BASALT_MARE, -0.15, 0.35), OLIVINE_BASALT),
            SurfaceRules.ifTrue(SurfaceRules.steep(), MOON_SURFACE_ROCK),
            MOON_BASALT
    );

    private static final RuleSource COMET_TUNDRA_FLOOR = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.EROSION, -0.065, 0.065), DENSE_ICE),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.MOON_BLEND_NOISE, 0.30, 1.0), DENSE_ICE),
            SurfaceRules.ifTrue(SurfaceRules.steep(), MOON_SURFACE_ROCK),
            MOON_TURF
    );

    private static final RuleSource HIGHLANDS_FLOOR = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), MOON_SURFACE_ROCK),
            SurfaceRules.ifTrue(
                    SurfaceRules.yBlockCheck(VerticalAnchor.absolute(112), 0),
                    SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.MOON_HIGHLAND_NOISE, 0.15, 1.0), MOON_SURFACE_ROCK)
            ),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.EROSION, 0.052, 0.082), MOON_SURFACE_ROCK),
            MOON_TURF
    );

    private static final RuleSource LOWLANDS_FLOOR = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.steep(), MOON_SURFACE_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.EROSION, 0.038, 0.054), MOON_SURFACE_ROCK),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.EROSION, 0.054, 0.069), MOON_BASALT),
            MOON_TURF
    );

    private static final RuleSource SURFACE_MATERIAL = SurfaceRules.sequence(
            SurfaceRules.ifTrue(IS_MARE, MARE_FLOOR),
            SurfaceRules.ifTrue(IS_OLIVINE_SPIKES, OLIVINE_SPIKES_FLOOR),
            SurfaceRules.ifTrue(IS_COMET_TUNDRA, COMET_TUNDRA_FLOOR),
            SurfaceRules.ifTrue(IS_HIGHLANDS, HIGHLANDS_FLOOR),
            SurfaceRules.ifTrue(IS_LOWLANDS, LOWLANDS_FLOOR),
            MOON_TURF
    );

    // GC4 ChunkProviderMoon.replaceBlocksForBiome: 3-4 blocks of sub-surface regolith (MOON_DIRT) or mare basalt
    private static final RuleSource SECONDARY_MATERIAL = SurfaceRules.sequence(
            SurfaceRules.ifTrue(IS_MARE, MOON_BASALT),
            SurfaceRules.ifTrue(
                    IS_OLIVINE_SPIKES,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.BASALT_MARE, 0.0, 0.45), OLIVINE_BASALT),
                            MOON_BASALT
                    )
            ),
            SurfaceRules.ifTrue(
                    IS_COMET_TUNDRA,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(GCNoiseData.MOON_BLEND_NOISE, 0.38, 1.0), DENSE_ICE),
                            MOON_DIRT
                    )
            ),
            SurfaceRules.ifTrue(
                    IS_HIGHLANDS,
                    SurfaceRules.sequence(
                            SurfaceRules.ifTrue(SurfaceRules.steep(), MOON_ROCK),
                            MOON_DIRT
                    )
            ),
            MOON_DIRT
    );

    private static final RuleSource SURFACE_GENERATION = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SURFACE_MATERIAL),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SECONDARY_MATERIAL)
    );

    public static final RuleSource MOON = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SURFACE_GENERATION),
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("lunaslate", VerticalAnchor.absolute(-4), VerticalAnchor.absolute(4)), LUNASLATE)
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
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("moon"), MapCodec.unit(MOON));
    }
}
