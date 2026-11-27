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

package dev.galacticraft.mod.world.biome;

import dev.galacticraft.mod.content.GCSounds;
import dev.galacticraft.mod.world.gen.carver.GCConfiguredCarvers;
import dev.galacticraft.mod.world.gen.feature.GCOrePlacedFeatures;
import dev.galacticraft.mod.world.gen.feature.GCPlacedFeatures;
import net.minecraft.core.HolderGetter;
import net.minecraft.sounds.Musics;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class MarsBiomes {
    // GC4 WorldProviderMars sky RGB (154, 114, 66) -> 0x9A7242; fog RGB (210, 120, 59) -> 0xD2783B
    public static final int MARS_SKY_COLOR = 0x9A7242;
    public static final int MARS_FOG_COLOR = 0xD2783B;
    public static final int MARS_DUNE_FOG_COLOR = 0xC66B33;
    public static final int MARS_THARSIS_FOG_COLOR = 0x8F4D32;
    public static final int MARS_POLAR_SKY_COLOR = 0xA8958A;
    public static final int MARS_POLAR_FOG_COLOR = 0xC9B4A6;
    public static final int MARS_CAVE_FOG_COLOR = 0x3D1E14;
    public static final int MARS_ICE_CAVE_FOG_COLOR = 0x4E687A;

    public static Biome createMarsFlat(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CRATER_CARVER);
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.MARS_SURFACE_OUTCROP);
        return mars(generation, MARS_SKY_COLOR, MARS_FOG_COLOR, true, false);
    }

    public static Biome createMarsCanyon(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.MARS_SURFACE_OUTCROP);
        return mars(generation, MARS_SKY_COLOR, MARS_FOG_COLOR, true, false);
    }

    public static Biome createMarsHighlands(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CRATER_CARVER);
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.MARS_SURFACE_OUTCROP);
        return mars(generation, MARS_SKY_COLOR, MARS_FOG_COLOR, true, false);
    }

    public static Biome createMarsImpactBasin(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CRATER_CARVER);
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.MARS_SURFACE_OUTCROP);
        return mars(generation, MARS_SKY_COLOR, MARS_FOG_COLOR, true, false);
    }

    public static Biome createMarsDuneSea(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        return mars(generation, MARS_SKY_COLOR, MARS_DUNE_FOG_COLOR, true, false);
    }

    public static Biome createMarsTharsis(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.MARS_SURFACE_OUTCROP);
        return mars(generation, MARS_SKY_COLOR, MARS_THARSIS_FOG_COLOR, true, false);
    }

    public static Biome createMarsPolarIceCap(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CRATER_CARVER);
        return mars(generation, MARS_POLAR_SKY_COLOR, MARS_POLAR_FOG_COLOR, false, false);
    }

    public static Biome createMarsLavaTubes(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        return mars(generation, MARS_SKY_COLOR, MARS_CAVE_FOG_COLOR, false, true);
    }

    public static Biome createMarsDeepCaverns(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        return mars(generation, MARS_SKY_COLOR, MARS_CAVE_FOG_COLOR, true, true);
    }

    public static Biome createMarsIceCaverns(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MARS_CAVE_CARVER);
        return mars(generation, MARS_POLAR_SKY_COLOR, MARS_ICE_CAVE_FOG_COLOR, false, false);
    }

    public static void monsters(MobSpawnSettings.Builder builder, int zombieWeight, int skeletonWeight) {
        // Mars is a hostile, sterile celestial body with no living creature spawns.
    }

    public static void addDefaultMarsOres(BiomeGenerationSettings.Builder builder) {
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_LARGE_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TIN_SMALL_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TIN_MIDDLE_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TIN_UPPER_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_IRON_SMALL_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_IRON_MIDDLE_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_SILICON_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_ALUMINUM_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_NIOBIUM_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_GOLD_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_REDSTONE_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_GRAPHITE_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DESH_SMALL_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DESH_MIDDLE_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_ICE_MARS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_SUB_SURFACE_MARS);
    }

    public static Biome mars(
            HolderGetter<PlacedFeature> featureGetter, HolderGetter<ConfiguredWorldCarver<?>> carverGetter
    ) {
        return createMarsFlat(featureGetter, carverGetter);
    }

    private static Biome mars(
            BiomeGenerationSettings.Builder generation,
            int skyColor,
            int fogColor,
            boolean addVines,
            boolean addSpeleothems
    ) {
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        BiomeSpecialEffects.Builder specialEffects = new BiomeSpecialEffects.Builder();
        specialEffects.waterColor(4159204)
                .waterFogColor(329011)
                .fogColor(fogColor)
                .skyColor(skyColor)
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .backgroundMusic(Musics.createGameMusic(GCSounds.MUSIC_MARS));

        MarsBiomes.addDefaultMarsOres(generation);
        if (addVines) {
            generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.MARS_CAVERN_VINES);
        }
        if (addSpeleothems) {
            generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.MARS_SPELEOTHEM);
        }
        MarsBiomes.monsters(spawnBuilder, 0, 0);

        return new Biome.BiomeBuilder()
                .mobSpawnSettings(spawnBuilder.build())
                .hasPrecipitation(false)
                .temperature(2.0F) // temp is hot to prevent vanilla snow weather
                .downfall(0.0F)
                .specialEffects(specialEffects.build())
                .generationSettings(generation.build())
                .build();
    }
}
