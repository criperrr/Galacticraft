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
import dev.galacticraft.mod.world.gen.feature.config.OuterPlanetStructureConfig;
import dev.galacticraft.mod.world.gen.feature.config.PlanetaryMeteorConfig;
import dev.galacticraft.mod.world.gen.feature.config.PlanetaryVolcanoConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

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

    // =========================================================================
    // JUPITER (Base Surface Y ~ 80 | Deep Metallic Mantle down to Y = -64)
    // =========================================================================
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUPITER_CYCLONE_VOLCANO = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("jupiter_cyclone_volcano"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUPITER_LIGHTNING_FULGURITE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("jupiter_lightning_fulgurite"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUPITER_AMMONIA_SPIRE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("jupiter_ammonia_spire"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUPITER_HYDROCARBON_POOL = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("jupiter_hydrocarbon_pool"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUPITER_METALLIC_HYDROGEN_COLUMN = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("jupiter_metallic_hydrogen_column"));

    // =========================================================================
    // SATURN (Base Surface Y ~ 80 | Deep Helium-Rain Mantle down to Y = -64)
    // =========================================================================
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATURN_HEXAGON_TERRACE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("saturn_hexagon_terrace"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATURN_RING_RAIN_MONOLITH = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("saturn_ring_rain_monolith"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATURN_RING_ICE_IMPACT = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("saturn_ring_ice_impact"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATURN_HELIUM_RAIN_SPELEOTHEM = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("saturn_helium_rain_speleothem"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> SATURN_AMMONIA_HYDROCARBON_LAKE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("saturn_ammonia_hydrocarbon_lake"));

    // =========================================================================
    // URANUS (High Base Surface Y ~ 105 | 169 Blocks of Deep Mantle down to Y = -64)
    // =========================================================================
    public static final ResourceKey<ConfiguredFeature<?, ?>> URANUS_METHANE_ICE_SPIRE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("uranus_methane_ice_spire"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> URANUS_FROZEN_SULFIDE_OUTCROP = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("uranus_frozen_sulfide_outcrop"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> URANUS_DIAMOND_RAIN_GEODE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("uranus_diamond_rain_geode"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> URANUS_SUPERIONIC_ICE_PILLAR = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("uranus_superionic_ice_pillar"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> URANUS_COMET_IMPACT = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("uranus_comet_impact"));

    // =========================================================================
    // NEPTUNE (High Base Surface Y ~ 105 | 169 Blocks of Deep Mantle down to Y = -64)
    // =========================================================================
    public static final ResourceKey<ConfiguredFeature<?, ?>> NEPTUNE_NITROGEN_CRYOGEYSER = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("neptune_nitrogen_cryogeyser"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> NEPTUNE_SUPERSONIC_WIND_RIDGE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("neptune_supersonic_wind_ridge"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> NEPTUNE_GREAT_DARK_VORTEX = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("neptune_great_dark_vortex"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> NEPTUNE_DIAMONDBERG = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("neptune_diamondberg"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> NEPTUNE_CRYO_SPELEOTHEM = ResourceKey.create(Registries.CONFIGURED_FEATURE, Constant.id("neptune_cryo_speleothem"));

    public static void bootstrapRegistries(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(OIL_LAKE, new ConfiguredFeature<>(Feature.LAKE,
                new LakeFeature.Configuration(BlockStateProvider.simple(GCBlocks.CRUDE_OIL), BlockStateProvider.simple(Blocks.STONE))));
        context.register(MARS_CAVERN_VINES, new ConfiguredFeature<>(GCFeatures.MARS_CAVERN_VINES, FeatureConfiguration.NONE));
        context.register(VENUS_LAVA_LAKE, new ConfiguredFeature<>(GCFeatures.VENUS_LAVA_LAKE, FeatureConfiguration.NONE));
        context.register(VENUS_VAPOR_POOL, new ConfiguredFeature<>(GCFeatures.VENUS_VAPOR_POOL, FeatureConfiguration.NONE));

        // TFCVolcanoes-based customizable volcano system for Venus:
        // Large Voronoi regionSize (560 blocks) with per-biome frequency map so volcanoes are monumental, rare, and natural
        registerPlanetVolcano(context, VENUS_VOLCANO, PlanetaryVolcanoConfig.builder()
                .regionSize(560.0)
                .spread(0.75)
                .defaultFrequency(0.0F)
                .biomeFrequency(GCBiomes.Venus.VENUS_MOUNTAIN, 0.34F)
                .biomeFrequency(GCBiomes.Venus.VENUS_VALLEY, 0.18F)
                .biomeFrequency(GCBiomes.Venus.VENUS_FLAT, 0.04F)
                .radiusRange(36, 62)
                .heightRange(24, 46)
                .craterRadiusRange(8, 15)
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

        // =====================================================================
        // 1. JUPITER CONFIGURED FEATURES (Base Y ~ 80)
        // =====================================================================
        registerPlanetVolcano(context, JUPITER_CYCLONE_VOLCANO, PlanetaryVolcanoConfig.builder()
                .regionSize(540.0)
                .spread(0.75)
                .defaultFrequency(0.22F)
                .radiusRange(38, 64)
                .heightRange(22, 42)
                .craterRadiusRange(9, 16)
                .volcanoType(PlanetaryVolcanoConfig.VolcanoType.SHIELD)
                .rocks(
                        GCBlocks.JUPITER_RED_STORM_ROCK.defaultBlockState(),
                        GCBlocks.JUPITER_STONE.defaultBlockState(),
                        GCBlocks.JUPITER_CORE_BRECCIA.defaultBlockState(),
                        Blocks.MAGMA_BLOCK.defaultBlockState(),
                        Blocks.LAVA.defaultBlockState()
                )
                .addPyroclasticOre(GCBlocks.JUPITER_IRON_ORE.defaultBlockState())
                .addPyroclasticOre(GCBlocks.JUPITER_COPPER_ORE.defaultBlockState())
                .addPyroclasticOre(GCBlocks.JUPITER_ALUMINUM_ORE.defaultBlockState())
                .pyroclasticOreChance(4)
                .spawnPyroclasticScoria(true)
                .spawnFumaroles(true)
                .build()
        );

        context.register(JUPITER_LIGHTNING_FULGURITE, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.LIGHTNING_FULGURITE,
                        8, 16, 2, 4, 0.35F,
                        BlockStateProvider.simple(GCBlocks.JUPITER_CORE_BRECCIA),
                        BlockStateProvider.simple(GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK),
                        BlockStateProvider.simple(GCBlocks.JUPITER_RED_STORM_ROCK),
                        BlockStateProvider.simple(Blocks.MAGMA_BLOCK)
                )));

        context.register(JUPITER_AMMONIA_SPIRE, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.TILTED_SPIRE,
                        6, 13, 2, 3, 0.40F,
                        BlockStateProvider.simple(GCBlocks.JUPITER_BANDED_OCHRE_ROCK),
                        BlockStateProvider.simple(GCBlocks.JUPITER_SURFACE),
                        BlockStateProvider.simple(GCBlocks.JUPITER_SUB_SURFACE),
                        BlockStateProvider.simple(GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK)
                )));

        context.register(JUPITER_HYDROCARBON_POOL, new ConfiguredFeature<>(Feature.LAKE,
                new LakeFeature.Configuration(
                        BlockStateProvider.simple(GCBlocks.CRUDE_OIL.defaultBlockState()),
                        BlockStateProvider.simple(GCBlocks.JUPITER_CORE_BRECCIA.defaultBlockState())
                )));

        context.register(JUPITER_METALLIC_HYDROGEN_COLUMN, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.ABYSSAL_SPELEOTHEM,
                        12, 48, 3, 6, 0.0F,
                        BlockStateProvider.simple(GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK),
                        BlockStateProvider.simple(GCBlocks.JUPITER_CORE_BRECCIA),
                        BlockStateProvider.simple(GCBlocks.JUPITER_IRON_ORE),
                        BlockStateProvider.simple(GCBlocks.JUPITER_STONE)
                )));

        // =====================================================================
        // 2. SATURN CONFIGURED FEATURES (Base Y ~ 80)
        // =====================================================================
        context.register(SATURN_HEXAGON_TERRACE, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.HEXAGON_TERRACE,
                        4, 8, 5, 9, 0.0F,
                        BlockStateProvider.simple(GCBlocks.SATURN_GOLDEN_BAND_ROCK),
                        BlockStateProvider.simple(GCBlocks.SATURN_SURFACE),
                        BlockStateProvider.simple(GCBlocks.SATURN_HYDROCARBON_SLATE),
                        BlockStateProvider.simple(GCBlocks.SATURN_RING_ICE_ROCK)
                )));

        context.register(SATURN_RING_RAIN_MONOLITH, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.TILTED_SPIRE,
                        7, 15, 2, 4, 0.55F,
                        BlockStateProvider.simple(GCBlocks.SATURN_RING_ICE_ROCK),
                        BlockStateProvider.simple(GCBlocks.SATURN_SURFACE),
                        BlockStateProvider.simple(GCBlocks.SATURN_GOLDEN_BAND_ROCK),
                        BlockStateProvider.simple(GCBlocks.SATURN_SUB_SURFACE)
                )));

        registerPlanetMeteor(context, SATURN_RING_ICE_IMPACT, PlanetaryMeteorConfig.builder()
                .cellularGrid(600.0, 0.75)
                .spawnProbability(0.24F)
                .craterRadiusRange(12, 22)
                .meteorRadiusRange(2, 4)
                .meteorBlocks(
                        GCBlocks.SATURN_RING_ICE_ROCK.defaultBlockState(),
                        GCBlocks.SATURN_HELIUM_MANTLE_ROCK.defaultBlockState()
                )
                .craterBlocks(
                        GCBlocks.SATURN_SUB_SURFACE.defaultBlockState(),
                        GCBlocks.SATURN_HYDROCARBON_SLATE.defaultBlockState()
                )
                .addEjectaOre(GCBlocks.SATURN_TITANIUM_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.SATURN_IRON_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.SATURN_TIN_ORE.defaultBlockState())
                .oreChance(4)
                .build()
        );

        context.register(SATURN_HELIUM_RAIN_SPELEOTHEM, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.ABYSSAL_SPELEOTHEM,
                        10, 44, 2, 5, 0.0F,
                        BlockStateProvider.simple(GCBlocks.SATURN_HELIUM_MANTLE_ROCK),
                        BlockStateProvider.simple(GCBlocks.SATURN_STONE),
                        BlockStateProvider.simple(GCBlocks.SATURN_GOLDEN_BAND_ROCK),
                        BlockStateProvider.simple(GCBlocks.SATURN_TITANIUM_ORE)
                )));

        context.register(SATURN_AMMONIA_HYDROCARBON_LAKE, new ConfiguredFeature<>(Feature.LAKE,
                new LakeFeature.Configuration(
                        BlockStateProvider.simple(GCBlocks.FUEL.defaultBlockState()),
                        BlockStateProvider.simple(GCBlocks.SATURN_HYDROCARBON_SLATE.defaultBlockState())
                )));

        // =====================================================================
        // 3. URANUS CONFIGURED FEATURES (High Base Y ~ 105 | Deep Mantle -64..105)
        // =====================================================================
        context.register(URANUS_METHANE_ICE_SPIRE, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.TILTED_SPIRE,
                        9, 19, 2, 4, 0.85F,
                        BlockStateProvider.simple(GCBlocks.URANUS_METHANE_ICE),
                        BlockStateProvider.simple(GCBlocks.URANUS_SURFACE),
                        BlockStateProvider.simple(GCBlocks.URANUS_SUB_SURFACE),
                        BlockStateProvider.simple(GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK)
                )));

        context.register(URANUS_FROZEN_SULFIDE_OUTCROP, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.WIND_RIDGE,
                        2, 5, 4, 7, 0.25F,
                        BlockStateProvider.simple(GCBlocks.URANUS_VERONA_SCARP_ROCK),
                        BlockStateProvider.simple(GCBlocks.URANUS_SUB_SURFACE),
                        BlockStateProvider.simple(GCBlocks.URANUS_METHANE_ICE),
                        BlockStateProvider.simple(GCBlocks.URANUS_TITANIUM_ORE)
                )));

        context.register(URANUS_DIAMOND_RAIN_GEODE, new ConfiguredFeature<>(Feature.GEODE,
                new GeodeConfiguration(
                        new GeodeBlockSettings(
                                BlockStateProvider.simple(Blocks.AIR),
                                BlockStateProvider.simple(GCBlocks.URANUS_METHANE_ICE),
                                BlockStateProvider.simple(GCBlocks.URANUS_DIAMOND_BEARING_ROCK),
                                BlockStateProvider.simple(GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK),
                                BlockStateProvider.simple(GCBlocks.URANUS_STONE),
                                List.of(
                                        GCBlocks.URANUS_DIAMOND_BEARING_ROCK.defaultBlockState(),
                                        Blocks.AMETHYST_CLUSTER.defaultBlockState()
                                ),
                                BlockTags.FEATURES_CANNOT_REPLACE,
                                BlockTags.INVALID_SPAWN_INSIDE
                        ),
                        new GeodeLayerSettings(1.7D, 2.2D, 3.2D, 4.2D),
                        new GeodeCrackSettings(0.95D, 2.0D, 2),
                        0.35D,
                        0.12D,
                        true,
                        UniformInt.of(4, 6),
                        UniformInt.of(3, 4),
                        UniformInt.of(1, 2),
                        -16,
                        16,
                        0.05D,
                        1
                )));

        context.register(URANUS_SUPERIONIC_ICE_PILLAR, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.ABYSSAL_SPELEOTHEM,
                        14, 58, 3, 6, 0.0F,
                        BlockStateProvider.simple(GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK),
                        BlockStateProvider.simple(GCBlocks.URANUS_STONE),
                        BlockStateProvider.simple(GCBlocks.URANUS_METHANE_ICE),
                        BlockStateProvider.simple(GCBlocks.URANUS_DIAMOND_BEARING_ROCK)
                )));

        registerPlanetMeteor(context, URANUS_COMET_IMPACT, PlanetaryMeteorConfig.builder()
                .cellularGrid(640.0, 0.75)
                .spawnProbability(0.20F)
                .craterRadiusRange(14, 24)
                .meteorRadiusRange(2, 4)
                .meteorBlocks(
                        GCBlocks.FALLEN_METEOR.defaultBlockState().setValue(FallenMeteorBlock.HEAT, 0),
                        GCBlocks.URANUS_DIAMOND_BEARING_ROCK.defaultBlockState()
                )
                .craterBlocks(
                        GCBlocks.URANUS_VERONA_SCARP_ROCK.defaultBlockState(),
                        GCBlocks.URANUS_METHANE_ICE.defaultBlockState()
                )
                .addEjectaOre(GCBlocks.URANUS_TITANIUM_ORE.defaultBlockState())
                .addEjectaOre(GCBlocks.URANUS_ALUMINUM_ORE.defaultBlockState())
                .oreChance(5)
                .build()
        );

        // =====================================================================
        // 4. NEPTUNE CONFIGURED FEATURES (High Base Y ~ 105 | Deep Mantle -64..105)
        // =====================================================================
        context.register(NEPTUNE_NITROGEN_CRYOGEYSER, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.CRYO_GEYSER,
                        5, 10, 5, 7, 0.45F,
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_NITROGEN_ICE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_DARK_VORTEX_ROCK),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_CRYO_SLATE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK)
                )));

        context.register(NEPTUNE_SUPERSONIC_WIND_RIDGE, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.WIND_RIDGE,
                        3, 7, 5, 9, 0.65F,
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_CRYO_SLATE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_SURFACE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_NITROGEN_ICE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_DARK_VORTEX_ROCK)
                )));

        registerPlanetVolcano(context, NEPTUNE_GREAT_DARK_VORTEX, PlanetaryVolcanoConfig.builder()
                .regionSize(620.0)
                .spread(0.75)
                .defaultFrequency(0.18F)
                .radiusRange(34, 56)
                .heightRange(16, 28)
                .craterRadiusRange(12, 20)
                .volcanoType(PlanetaryVolcanoConfig.VolcanoType.TUFF_RING)
                .rocks(
                        GCBlocks.NEPTUNE_DARK_VORTEX_ROCK.defaultBlockState(),
                        GCBlocks.NEPTUNE_CRYO_SLATE.defaultBlockState(),
                        GCBlocks.NEPTUNE_STONE.defaultBlockState(),
                        GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK.defaultBlockState(),
                        GCBlocks.NEPTUNE_NITROGEN_ICE.defaultBlockState()
                )
                .addPyroclasticOre(GCBlocks.NEPTUNE_TITANIUM_ORE.defaultBlockState())
                .addPyroclasticOre(GCBlocks.NEPTUNE_IRON_ORE.defaultBlockState())
                .pyroclasticOreChance(5)
                .spawnPyroclasticScoria(true)
                .spawnFumaroles(true)
                .build()
        );

        context.register(NEPTUNE_DIAMONDBERG, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.DIAMONDBERG,
                        10, 22, 4, 7, 0.15F,
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_NITROGEN_ICE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_CRYO_SLATE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_TITANIUM_ORE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK)
                )));

        context.register(NEPTUNE_CRYO_SPELEOTHEM, new ConfiguredFeature<>(GCFeatures.OUTER_PLANET_STRUCTURE,
                new OuterPlanetStructureConfig(
                        OuterPlanetStructureConfig.StructureKind.ABYSSAL_SPELEOTHEM,
                        12, 54, 3, 6, 0.0F,
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_CRYO_SLATE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_NITROGEN_ICE),
                        BlockStateProvider.simple(GCBlocks.NEPTUNE_STONE)
                )));
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
