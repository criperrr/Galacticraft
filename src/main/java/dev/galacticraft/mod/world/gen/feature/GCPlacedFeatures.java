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
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

public class GCPlacedFeatures {
    public static final ResourceKey<PlacedFeature> OIL_LAKE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("oil_lake"));
    public static final ResourceKey<PlacedFeature> MARS_CAVERN_VINES = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("mars_cavern_vines"));
    public static final ResourceKey<PlacedFeature> VENUS_LAVA_LAKE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_lava_lake"));
    public static final ResourceKey<PlacedFeature> VENUS_VALLEY_LAVA_LAKE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_valley_lava_lake"));
    public static final ResourceKey<PlacedFeature> VENUS_VAPOR_POOL = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_vapor_pool"));
    public static final ResourceKey<PlacedFeature> VENUS_FLAT_VAPOR_POOL = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_flat_vapor_pool"));
    public static final ResourceKey<PlacedFeature> VENUS_VOLCANO = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_volcano"));
    public static final ResourceKey<PlacedFeature> VENUS_METEOR = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_meteor"));
    public static final ResourceKey<PlacedFeature> VENUS_SURFACE_OUTCROP = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_surface_outcrop"));
    public static final ResourceKey<PlacedFeature> VENUS_CRASHED_PROBE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_crashed_probe"));
    public static final ResourceKey<PlacedFeature> MOON_METEOR = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("moon_meteor"));
    public static final ResourceKey<PlacedFeature> MOON_SURFACE_OUTCROP = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("moon_surface_outcrop"));
    public static final ResourceKey<PlacedFeature> MOON_OLIVINE_SPIKE_SURFACE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("moon_olivine_spike_surface"));
    public static final ResourceKey<PlacedFeature> MOON_OLIVINE_SPIKE_CAVE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("moon_olivine_spike_cave"));
    public static final ResourceKey<PlacedFeature> MOON_CAVE_FLORA = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("moon_cave_flora"));
    public static final ResourceKey<PlacedFeature> MERCURY_MEGABRECCIA_SPELEOTHEM = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("mercury_megabreccia_speleothem"));
    public static final ResourceKey<PlacedFeature> MERCURY_LAVA_LAKE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("mercury_lava_lake"));

    // JUPITER (Base Y ~ 80)
    public static final ResourceKey<PlacedFeature> JUPITER_CYCLONE_VOLCANO = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("jupiter_cyclone_volcano"));
    public static final ResourceKey<PlacedFeature> JUPITER_LIGHTNING_FULGURITE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("jupiter_lightning_fulgurite"));
    public static final ResourceKey<PlacedFeature> JUPITER_AMMONIA_SPIRE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("jupiter_ammonia_spire"));
    public static final ResourceKey<PlacedFeature> JUPITER_HYDROCARBON_POOL = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("jupiter_hydrocarbon_pool"));
    public static final ResourceKey<PlacedFeature> JUPITER_METALLIC_HYDROGEN_COLUMN = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("jupiter_metallic_hydrogen_column"));

    // SATURN (Base Y ~ 80)
    public static final ResourceKey<PlacedFeature> SATURN_HEXAGON_TERRACE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("saturn_hexagon_terrace"));
    public static final ResourceKey<PlacedFeature> SATURN_RING_RAIN_MONOLITH = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("saturn_ring_rain_monolith"));
    public static final ResourceKey<PlacedFeature> SATURN_RING_ICE_IMPACT = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("saturn_ring_ice_impact"));
    public static final ResourceKey<PlacedFeature> SATURN_HELIUM_RAIN_SPELEOTHEM = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("saturn_helium_rain_speleothem"));
    public static final ResourceKey<PlacedFeature> SATURN_AMMONIA_HYDROCARBON_LAKE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("saturn_ammonia_hydrocarbon_lake"));

    // URANUS (High Base Y ~ 105)
    public static final ResourceKey<PlacedFeature> URANUS_METHANE_ICE_SPIRE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("uranus_methane_ice_spire"));
    public static final ResourceKey<PlacedFeature> URANUS_FROZEN_SULFIDE_OUTCROP = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("uranus_frozen_sulfide_outcrop"));
    public static final ResourceKey<PlacedFeature> URANUS_DIAMOND_RAIN_GEODE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("uranus_diamond_rain_geode"));
    public static final ResourceKey<PlacedFeature> URANUS_SUPERIONIC_ICE_PILLAR = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("uranus_superionic_ice_pillar"));
    public static final ResourceKey<PlacedFeature> URANUS_COMET_IMPACT = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("uranus_comet_impact"));

    // NEPTUNE (High Base Y ~ 105)
    public static final ResourceKey<PlacedFeature> NEPTUNE_NITROGEN_CRYOGEYSER = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("neptune_nitrogen_cryogeyser"));
    public static final ResourceKey<PlacedFeature> NEPTUNE_SUPERSONIC_WIND_RIDGE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("neptune_supersonic_wind_ridge"));
    public static final ResourceKey<PlacedFeature> NEPTUNE_GREAT_DARK_VORTEX = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("neptune_great_dark_vortex"));
    public static final ResourceKey<PlacedFeature> NEPTUNE_DIAMONDBERG = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("neptune_diamondberg"));
    public static final ResourceKey<PlacedFeature> NEPTUNE_CRYO_SPELEOTHEM = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("neptune_cryo_speleothem"));

    public static void bootstrapRegistries(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatureLookup = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(OIL_LAKE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.OIL_LAKE), List.of(
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                RarityFilter.onAverageOnceEvery(70),
                InSquarePlacement.spread(),
                BiomeFilter.biome()
        )));
        // GC4 MapGenCavernMars: hanging Cavernous Vines in Martian subterranean caverns (Y = 8..65)
        context.register(MARS_CAVERN_VINES, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.MARS_CAVERN_VINES), List.of(
                CountPlacement.of(24),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(8), VerticalAnchor.absolute(65)),
                BiomeFilter.biome()
        )));
        // GC4 ChunkProviderVenus: 1-in-10 lava lake in standard Venus biomes
        context.register(VENUS_LAVA_LAKE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.VENUS_LAVA_LAKE), List.of(
                RarityFilter.onAverageOnceEvery(10),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        // GC4 ChunkProviderVenus: 1-in-3 lava/acid lake in BiomeGenVenusValley
        context.register(VENUS_VALLEY_LAVA_LAKE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.VENUS_LAVA_LAKE), List.of(
                RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        // GC4 ChunkProviderVenus: 1-in-5 vapor pool & spout in BiomeGenVenusValley
        context.register(VENUS_VAPOR_POOL, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.VENUS_VAPOR_POOL), List.of(
                RarityFilter.onAverageOnceEvery(5),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        // GalaxySpace BiomeDecoratorIo: rarer vapor pool & spout on volcanic plains
        context.register(VENUS_FLAT_VAPOR_POOL, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.VENUS_VAPOR_POOL), List.of(
                RarityFilter.onAverageOnceEvery(18),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        // TFCVolcanoes cellular multi-chunk volcano placement: runs once per chunk so all chunks overlapping
        // a rare Voronoi cell center shape their 16x16 slice of the volcano seamlessly
        registerPlanetVolcanoPlacement(context, configuredFeatureLookup, VENUS_VOLCANO, GCConfiguredFeature.VENUS_VOLCANO);
        // Multi-chunk cellular planetary meteor impact crater placement on Venus
        registerPlanetMeteorPlacement(context, configuredFeatureLookup, VENUS_METEOR, GCConfiguredFeature.VENUS_METEOR);
        // Geological basalt/scorched rock patches with scattered surface ore blocks
        context.register(VENUS_SURFACE_OUTCROP, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.VENUS_SURFACE_OUTCROP), List.of(
                RarityFilter.onAverageOnceEvery(6),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        // GC4 ChunkProviderVenus: 1-in-190 crashed probe crater in BiomeGenVenusValley
        context.register(VENUS_CRASHED_PROBE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.VENUS_CRASHED_PROBE), List.of(
                RarityFilter.onAverageOnceEvery(190),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));

        // Multi-chunk cellular planetary meteor impact crater placement on the Moon
        registerPlanetMeteorPlacement(context, configuredFeatureLookup, MOON_METEOR, GCConfiguredFeature.MOON_METEOR);
        // Lunar regolith/basalt rock patches and erratic ore-bearing boulders
        context.register(MOON_SURFACE_OUTCROP, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.MOON_SURFACE_OUTCROP), List.of(
                RarityFilter.onAverageOnceEvery(5),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        // Volcanic olivine-basalt and olivine crystal spires in the Olivine Spikes biome
        context.register(MOON_OLIVINE_SPIKE_SURFACE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.MOON_OLIVINE_SPIKE), List.of(
                RarityFilter.onAverageOnceEvery(2),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        // Subterranean olivine crystal outcrops in deep lunar caves
        context.register(MOON_OLIVINE_SPIKE_CAVE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.MOON_OLIVINE_SPIKE), List.of(
                CountPlacement.of(3),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(42)),
                BiomeFilter.biome()
        )));
        // Subterranean bioluminescent lunar cave flora (Moon Moss, Moon Tangle, Moon Weed, Moon Shrubs, Cheese Spores)
        context.register(MOON_CAVE_FLORA, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.MOON_CAVE_FLORA), List.of(
                CountPlacement.of(5),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(52)),
                BiomeFilter.biome()
        )));
        // Subterranean monumental speleothems in Mercury's deep megabreccia caverns
        context.register(MERCURY_MEGABRECCIA_SPELEOTHEM, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.MERCURY_MEGABRECCIA_SPELEOTHEM), List.of(
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-25), VerticalAnchor.absolute(45)),
                BiomeFilter.biome()
        )));
        // Subterranean lava lakes in deep basal magma depths
        context.register(MERCURY_LAVA_LAKE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.MERCURY_LAVA_LAKE), List.of(
                RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-52), VerticalAnchor.absolute(-24)),
                BiomeFilter.biome()
        )));

        // =====================================================================
        // 1. JUPITER PLACED FEATURES (Surface Y ~ 80 | Underground Y = -55..35)
        // =====================================================================
        registerPlanetVolcanoPlacement(context, configuredFeatureLookup, JUPITER_CYCLONE_VOLCANO, GCConfiguredFeature.JUPITER_CYCLONE_VOLCANO);
        context.register(JUPITER_LIGHTNING_FULGURITE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.JUPITER_LIGHTNING_FULGURITE), List.of(
                RarityFilter.onAverageOnceEvery(4),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        context.register(JUPITER_AMMONIA_SPIRE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.JUPITER_AMMONIA_SPIRE), List.of(
                RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        context.register(JUPITER_HYDROCARBON_POOL, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.JUPITER_HYDROCARBON_POOL), List.of(
                RarityFilter.onAverageOnceEvery(8),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        context.register(JUPITER_METALLIC_HYDROGEN_COLUMN, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.JUPITER_METALLIC_HYDROGEN_COLUMN), List.of(
                CountPlacement.of(3),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-55), VerticalAnchor.absolute(35)),
                BiomeFilter.biome()
        )));

        // =====================================================================
        // 2. SATURN PLACED FEATURES (Surface Y ~ 80 | Underground Y = -50..40)
        // =====================================================================
        context.register(SATURN_HEXAGON_TERRACE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.SATURN_HEXAGON_TERRACE), List.of(
                RarityFilter.onAverageOnceEvery(4),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        context.register(SATURN_RING_RAIN_MONOLITH, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.SATURN_RING_RAIN_MONOLITH), List.of(
                RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        registerPlanetMeteorPlacement(context, configuredFeatureLookup, SATURN_RING_ICE_IMPACT, GCConfiguredFeature.SATURN_RING_ICE_IMPACT);
        context.register(SATURN_HELIUM_RAIN_SPELEOTHEM, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.SATURN_HELIUM_RAIN_SPELEOTHEM), List.of(
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-50), VerticalAnchor.absolute(40)),
                BiomeFilter.biome()
        )));
        context.register(SATURN_AMMONIA_HYDROCARBON_LAKE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.SATURN_AMMONIA_HYDROCARBON_LAKE), List.of(
                RarityFilter.onAverageOnceEvery(9),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));

        // =====================================================================
        // 3. URANUS PLACED FEATURES (High Surface Y ~ 105 | Deep Mantle -54..68)
        // =====================================================================
        context.register(URANUS_METHANE_ICE_SPIRE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.URANUS_METHANE_ICE_SPIRE), List.of(
                RarityFilter.onAverageOnceEvery(2),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        context.register(URANUS_FROZEN_SULFIDE_OUTCROP, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.URANUS_FROZEN_SULFIDE_OUTCROP), List.of(
                RarityFilter.onAverageOnceEvery(5),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        context.register(URANUS_DIAMOND_RAIN_GEODE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.URANUS_DIAMOND_RAIN_GEODE), List.of(
                RarityFilter.onAverageOnceEvery(16),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(58)),
                BiomeFilter.biome()
        )));
        context.register(URANUS_SUPERIONIC_ICE_PILLAR, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.URANUS_SUPERIONIC_ICE_PILLAR), List.of(
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-54), VerticalAnchor.absolute(68)),
                BiomeFilter.biome()
        )));
        registerPlanetMeteorPlacement(context, configuredFeatureLookup, URANUS_COMET_IMPACT, GCConfiguredFeature.URANUS_COMET_IMPACT);

        // =====================================================================
        // 4. NEPTUNE PLACED FEATURES (High Surface Y ~ 105 | Deep Mantle -58..70)
        // =====================================================================
        context.register(NEPTUNE_NITROGEN_CRYOGEYSER, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.NEPTUNE_NITROGEN_CRYOGEYSER), List.of(
                RarityFilter.onAverageOnceEvery(5),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        context.register(NEPTUNE_SUPERSONIC_WIND_RIDGE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.NEPTUNE_SUPERSONIC_WIND_RIDGE), List.of(
                RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
        registerPlanetVolcanoPlacement(context, configuredFeatureLookup, NEPTUNE_GREAT_DARK_VORTEX, GCConfiguredFeature.NEPTUNE_GREAT_DARK_VORTEX);
        context.register(NEPTUNE_DIAMONDBERG, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.NEPTUNE_DIAMONDBERG), List.of(
                RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-58), VerticalAnchor.absolute(-12)),
                BiomeFilter.biome()
        )));
        context.register(NEPTUNE_CRYO_SPELEOTHEM, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.NEPTUNE_CRYO_SPELEOTHEM), List.of(
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-45), VerticalAnchor.absolute(70)),
                BiomeFilter.biome()
        )));
    }

    /**
     * Reusable helper to register a TFCVolcanoes-style multi-chunk cellular volcano placement for any planet.
     */
    public static void registerPlanetVolcanoPlacement(
            BootstrapContext<PlacedFeature> context,
            HolderGetter<ConfiguredFeature<?, ?>> configuredLookup,
            ResourceKey<PlacedFeature> placedKey,
            ResourceKey<ConfiguredFeature<?, ?>> configuredKey
    ) {
        context.register(placedKey, new PlacedFeature(configuredLookup.getOrThrow(configuredKey), List.of(
                CountPlacement.of(1),
                BiomeFilter.biome()
        )));
    }

    /**
     * Reusable helper to register a multi-chunk cellular planetary meteor impact crater placement for any planet.
     */
    public static void registerPlanetMeteorPlacement(
            BootstrapContext<PlacedFeature> context,
            HolderGetter<ConfiguredFeature<?, ?>> configuredLookup,
            ResourceKey<PlacedFeature> placedKey,
            ResourceKey<ConfiguredFeature<?, ?>> configuredKey
    ) {
        context.register(placedKey, new PlacedFeature(configuredLookup.getOrThrow(configuredKey), List.of(
                CountPlacement.of(1),
                BiomeFilter.biome()
        )));
    }

    public static void register() {
        BiomeModifications.addFeature(context -> context.hasFeature(MiscOverworldFeatures.LAKE_LAVA), GenerationStep.Decoration.LAKES, OIL_LAKE);
    }
}
