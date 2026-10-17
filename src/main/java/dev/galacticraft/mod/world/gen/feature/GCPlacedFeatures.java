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
        // Planetary meteor impact crater & fallen meteor placement on Venus
        registerPlanetMeteorPlacement(context, configuredFeatureLookup, VENUS_METEOR, GCConfiguredFeature.VENUS_METEOR, 42);
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
     * Reusable helper to register a planetary meteor impact crater placement with custom chunk rarity for any planet.
     */
    public static void registerPlanetMeteorPlacement(
            BootstrapContext<PlacedFeature> context,
            HolderGetter<ConfiguredFeature<?, ?>> configuredLookup,
            ResourceKey<PlacedFeature> placedKey,
            ResourceKey<ConfiguredFeature<?, ?>> configuredKey,
            int averageOnceEveryChunks
    ) {
        context.register(placedKey, new PlacedFeature(configuredLookup.getOrThrow(configuredKey), List.of(
                RarityFilter.onAverageOnceEvery(averageOnceEveryChunks),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        )));
    }

    public static void register() {
        BiomeModifications.addFeature(context -> context.hasFeature(MiscOverworldFeatures.LAKE_LAVA), GenerationStep.Decoration.LAKES, OIL_LAKE);
    }
}
