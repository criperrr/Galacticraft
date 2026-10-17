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

import java.awt.*;

public class VenusBiomes {
    public static void monsters(MobSpawnSettings.Builder builder, int zombieWeight, int zombieVillagerWeight, int skeletonWeight) {
        // Venus is a sterile, hostile planetary environment with no fantasy monster spawns.
    }

    public static void addDefaultVenusCarvers(BiomeGenerationSettings.Builder builder) {
        builder.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.VENUS_CAVE_CARVER);
        builder.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.VENUS_CANYON_CARVER);
    }

    public static void addDefaultVenusOres(BiomeGenerationSettings.Builder builder) {
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_VENUS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_LARGE_VENUS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TIN_VENUS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_ALUMINUM_VENUS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_GALENA_VENUS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_SOLAR_VENUS);
    }

    public static void addDefaultVenusSurfaceFeatures(BiomeGenerationSettings.Builder builder) {
        // Multi-chunk TFCVolcanoes cellular volcano generator must be present in all Venus biomes
        // so that large volcano skirts crossing biome boundaries never get clipped at chunk edges!
        // Its internal per-biome frequency map controls which biomes actually spawn volcano centers.
        builder.addFeature(GenerationStep.Decoration.RAW_GENERATION, GCPlacedFeatures.VENUS_VOLCANO);
        builder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.VENUS_SURFACE_OUTCROP);
        builder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.VENUS_METEOR);
    }

    public static Biome createVenusFlat(
            HolderGetter<PlacedFeature> featureGetter, HolderGetter<ConfiguredWorldCarver<?>> carverGetter
    ) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(featureGetter, carverGetter);
        addDefaultVenusCarvers(generation);
        addDefaultVenusSurfaceFeatures(generation);
        generation.addFeature(GenerationStep.Decoration.LAKES, GCPlacedFeatures.VENUS_LAVA_LAKE);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.VENUS_FLAT_VAPOR_POOL);
        addDefaultVenusOres(generation);
        return createVenusBiome(generation, 2.0F, 0.3F);
    }

    public static Biome createVenusMountain(
            HolderGetter<PlacedFeature> featureGetter, HolderGetter<ConfiguredWorldCarver<?>> carverGetter
    ) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(featureGetter, carverGetter);
        addDefaultVenusCarvers(generation);
        addDefaultVenusSurfaceFeatures(generation);
        generation.addFeature(GenerationStep.Decoration.LAKES, GCPlacedFeatures.VENUS_LAVA_LAKE);
        addDefaultVenusOres(generation);
        return createVenusBiome(generation, 2.0F, 0.1F);
    }

    public static Biome createVenusValley(
            HolderGetter<PlacedFeature> featureGetter, HolderGetter<ConfiguredWorldCarver<?>> carverGetter
    ) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(featureGetter, carverGetter);
        addDefaultVenusCarvers(generation);
        addDefaultVenusSurfaceFeatures(generation);
        generation.addFeature(GenerationStep.Decoration.LAKES, GCPlacedFeatures.VENUS_VALLEY_LAVA_LAKE);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.VENUS_VAPOR_POOL);
        generation.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.VENUS_CRASHED_PROBE);
        addDefaultVenusOres(generation);
        return createVenusBiome(generation, 2.0F, 0.8F);
    }

    public static Biome venus(
            HolderGetter<PlacedFeature> featureGetter, HolderGetter<ConfiguredWorldCarver<?>> carverGetter
    ) {
        return createVenusFlat(featureGetter, carverGetter);
    }

    private static Biome createVenusBiome(BiomeGenerationSettings.Builder generation, float temperature, float downfall) {
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        monsters(spawnBuilder, 0, 0, 0);

        BiomeSpecialEffects.Builder specialEffects = new BiomeSpecialEffects.Builder();
        specialEffects.waterColor(4159204)
                .waterFogColor(329011)
                .fogColor(new Color(203, 147, 0).getRGB())
                .skyColor(new Color(255, 207, 81).getRGB())
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .backgroundMusic(Musics.createGameMusic(GCSounds.MUSIC_MOON));

        return new Biome.BiomeBuilder()
                .mobSpawnSettings(spawnBuilder.build())
                .hasPrecipitation(true)
                .temperature(temperature)
                .downfall(downfall)
                .specialEffects(specialEffects.build())
                .generationSettings(generation.build())
                .build();
    }
}
