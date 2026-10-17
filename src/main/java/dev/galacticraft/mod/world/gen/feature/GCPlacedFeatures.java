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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

public class GCPlacedFeatures {
    public static final ResourceKey<PlacedFeature> OIL_LAKE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("oil_lake"));
    public static final ResourceKey<PlacedFeature> VENUS_LAVA_LAKE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_lava_lake"));
    public static final ResourceKey<PlacedFeature> VENUS_VALLEY_LAVA_LAKE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_valley_lava_lake"));
    public static final ResourceKey<PlacedFeature> VENUS_VAPOR_POOL = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_vapor_pool"));
    public static final ResourceKey<PlacedFeature> VENUS_FLAT_VAPOR_POOL = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_flat_vapor_pool"));
    public static final ResourceKey<PlacedFeature> VENUS_VOLCANO = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_volcano"));
    public static final ResourceKey<PlacedFeature> VENUS_CRASHED_PROBE = ResourceKey.create(Registries.PLACED_FEATURE, Constant.id("venus_crashed_probe"));

    public static void bootstrapRegistries(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatureLookup = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(OIL_LAKE, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.OIL_LAKE), List.of(
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                RarityFilter.onAverageOnceEvery(70),
                InSquarePlacement.spread(),
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
        // ExtraPlanets WorldGenVolcano + GalaxySpace volcanic cones
        context.register(VENUS_VOLCANO, new PlacedFeature(configuredFeatureLookup.getOrThrow(GCConfiguredFeature.VENUS_VOLCANO), List.of(
                RarityFilter.onAverageOnceEvery(22),
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

    public static void register() {
        BiomeModifications.addFeature(context -> context.hasFeature(MiscOverworldFeatures.LAKE_LAVA), GenerationStep.Decoration.LAKES, OIL_LAKE);
    }
}
