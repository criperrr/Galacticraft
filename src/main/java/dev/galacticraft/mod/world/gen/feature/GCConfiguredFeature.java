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

package dev.galacticraft.mod.world.gen.feature;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.environment.FallenMeteorBlock;
import dev.galacticraft.mod.world.biome.GCBiomes;
import dev.galacticraft.mod.world.gen.feature.config.PlanetaryMeteorConfig;
import dev.galacticraft.mod.world.gen.feature.config.PlanetaryVolcanoConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class GCConfiguredFeature {
    public static final ResourceKey<ConfiguredFeature<?, ?>> OIL_LAKE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("oil_lake"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> MARS_CAVERN_VINES = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("mars_cavern_vines"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> VENUS_LAVA_LAKE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("venus_lava_lake"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> VENUS_VAPOR_POOL = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("venus_vapor_pool"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> VENUS_VOLCANO = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("venus_volcano"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> VENUS_METEOR = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("venus_meteor"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> VENUS_SURFACE_OUTCROP = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("venus_surface_outcrop"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> VENUS_CRASHED_PROBE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("venus_crashed_probe"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOON_METEOR = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("moon_meteor"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOON_SURFACE_OUTCROP = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("moon_surface_outcrop"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOON_OLIVINE_SPIKE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("moon_olivine_spike"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOON_CAVE_FLORA = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("moon_cave_flora"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> MERCURY_MEGABRECCIA_SPELEOTHEM = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("mercury_megabreccia_speleothem"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> MERCURY_LAVA_LAKE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("mercury_lava_lake"));

    public static void bootstrapRegistries(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(OIL_LAKE, new ConfiguredFeature<>(Feature.LAKE,
                new LakeFeature.Configuration(BlockStateProvider.simple(GCBlocks.CRUDE_OIL), BlockStateProvider.simple(Blocks.STONE))));
        context.register(MARS_CAVERN_VINES, new ConfiguredFeature<>(GCFeatures.MARS_CAVERN_VINES, FeatureConfiguration.NONE));
        context.register(VENUS_LAVA_LAKE, new ConfiguredFeature<>(GCFeatures.VENUS_LAVA_LAKE, FeatureConfiguration.NONE));
        context.register(VENUS_VAPOR_POOL, new ConfiguredFeature<>(GCFeatures.VENUS_VAPOR_POOL, FeatureConfiguration.NONE));

        // TFCVolcanoes-based customizable volcano system for Venus:
        // Large Voronoi regionSize (560 blocks) with elevated mountain placement and monumental scales
        registerPlanetVolcano(context, VENUS_VOLCANO, PlanetaryVolcanoConfig.builder()
                .regionSize(560.0)
                .spread(0.75)
                .defaultFrequency(0.0F)
                .biomeFrequency(GCBiomes.Venus.VENUS_MOUNTAIN, 0.52F)
                .biomeFrequency(GCBiomes.Venus.VENUS_FLAT, 0.08F)
                .biomeFrequency(GCBiomes.Venus.VENUS_VALLEY, 0.0F)
                .radiusRange(52, 88)
                .heightRange(34, 62)
                .craterRadiusRange(12, 22)
                .volcanoType(PlanetaryVolcanoConfig.VolcanoType.MIXED)
                .rocks(
                        Blocks.BASALT.defaultBlockState(),
                        GCBlocks.HARD_VENUS_ROCK.defaultBlockState(),
                        GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState(),
                        Blocks.MAGMA_BLOCK.defaultBlockState(),
                        Blocks.LAVA.defaultBlockState()
                )
                .addPyroclasticOre(GCBlocks.GALENA_ORE.defaultBlockState())
                .addPyroclasticOre(GCBlocks.SOLAR_ORE.defaultBlockState())
                .addPyroclasticOre(GCBlocks.VENUS_COPPER_ORE.defaultBlockState())
                .addPyroclasticOre(GCBlocks.VENUS_TIN_ORE.defaultBlockState())
                .addPyroclasticOre(GCBlocks.VENUS_ALUMINUM_ORE.defaultBlockState())
                .pyroclasticOreChance(5)
                .spawnPyroclasticScoria(true)
                .spawnFumaroles(true)
                .build()
        );

        // Customizable multi-chunk planetary meteor impact crater & asteroid ore deposit for Venus:
        // Rare Voronoi regionSize (680 blocks, 22% cell probability), large crater bowl (radius 15..26 -> diameter 30..52 blocks),
        // and diverse ores brought by the impacting asteroid
        registerPlanetMeteor(context, VENUS_METEOR, PlanetaryMeteorConfig.builder()
                .cellularGrid(680.0, 0.75)
                .spawnProbability(0.22F)
                .craterRadiusRange(15, 26)
                .meteorRadiusRange(2, 4)
                .meteorBlocks(
                        GCBlocks.FALLEN_METEOR.defaultBlockState().setValue(FallenMeteorBlock.HEAT, 5),
                        GCBlocks.RAW_METEORIC_IRON_BLOCK.defaultBlockState()
                )
                .craterBlocks(
                        GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState(),
                        Blocks.MAGMA_BLOCK.defaultBlockState()
                )
                .addEjectaOre(GCBlocks.RAW_METEORIC_IRON_BLOCK.defaultBlockState())
                .addEjectaOre(GCBlocks.GALENA_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.SOLAR_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.ILMENITE_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.DESH_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.VENUS_COPPER_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.VENUS_TIN_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.VENUS_ALUMINUM_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.SILICON_ORE.defaultBlockState())
                .addEjectaOre(Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState())
                .addEjectaOre(Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState())
                .addEjectaOre(Blocks.RAW_IRON_BLOCK.defaultBlockState())
                .addEjectaOre(Blocks.RAW_GOLD_BLOCK.defaultBlockState())
                .oreChance(4)
                .build()
        );

        context.register(VENUS_SURFACE_OUTCROP, new ConfiguredFeature<>(GCFeatures.VENUS_SURFACE_OUTCROP, FeatureConfiguration.NONE));
        context.register(VENUS_CRASHED_PROBE, new ConfiguredFeature<>(GCFeatures.VENUS_CRASHED_PROBE, FeatureConfiguration.NONE));

        // Multi-chunk planetary meteor impact crater & meteoric iron/sapphire/olivine ejecta deposit for the Moon:
        // Airless Moon preserves large impact bowls (radius 14..24 -> diameter 28..48 blocks) with cooled FALLEN_METEOR (HEAT=1)
        registerPlanetMeteor(context, MOON_METEOR, PlanetaryMeteorConfig.builder()
                .cellularGrid(560.0, 0.75)
                .spawnProbability(0.24F)
                .craterRadiusRange(14, 24)
                .meteorRadiusRange(2, 4)
                .meteorBlocks(
                        GCBlocks.FALLEN_METEOR.defaultBlockState().setValue(FallenMeteorBlock.HEAT, 1),
                        GCBlocks.RAW_METEORIC_IRON_BLOCK.defaultBlockState()
                )
                .craterBlocks(
                        GCBlocks.MOON_SURFACE_ROCK.defaultBlockState(),
                        GCBlocks.COBBLED_MOON_ROCK.defaultBlockState()
                )
                .addEjectaOre(GCBlocks.MOON_CHEESE_ORE.defaultBlockState())
                .oreChance(8)
                .build()
        );

        context.register(MOON_SURFACE_OUTCROP, new ConfiguredFeature<>(GCFeatures.MOON_SURFACE_OUTCROP, FeatureConfiguration.NONE));
        context.register(MOON_OLIVINE_SPIKE, new ConfiguredFeature<>(GCFeatures.MOON_OLIVINE_SPIKE, FeatureConfiguration.NONE));
        context.register(MOON_CAVE_FLORA, new ConfiguredFeature<>(GCFeatures.MOON_CAVE_FLORA, FeatureConfiguration.NONE));
        context.register(MERCURY_MEGABRECCIA_SPELEOTHEM, new ConfiguredFeature<>(GCFeatures.MERCURY_SPELEOTHEM, FeatureConfiguration.NONE));
        context.register(MERCURY_LAVA_LAKE, new ConfiguredFeature<>(Feature.LAKE,
                new LakeFeature.Configuration(BlockStateProvider.simple(Blocks.LAVA), BlockStateProvider.simple(Blocks.MAGMA_BLOCK))));
    }

    /**
     * Helper method to register a custom {@link PlanetaryVolcanoConfig} for any planet or dimension.
     */
    public static void registerPlanetVolcano(
            BootstrapContext<ConfiguredFeature<?, ?>> context,
            ResourceKey<ConfiguredFeature<?, ?>> key,
            PlanetaryVolcanoConfig config
    ) {
        context.register(key, new ConfiguredFeature<>(GCFeatures.PLANETARY_VOLCANO, config));
    }

    /**
     * Helper method to register a custom {@link PlanetaryMeteorConfig} for any planet or dimension.
     */
    public static void registerPlanetMeteor(
            BootstrapContext<ConfiguredFeature<?, ?>> context,
            ResourceKey<ConfiguredFeature<?, ?>> key,
            PlanetaryMeteorConfig config
    ) {
        context.register(key, new ConfiguredFeature<>(GCFeatures.PLANETARY_METEOR, config));
    }
}
