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
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SaturnBiomes {

    private static void addSaturnCarversAndOres(BiomeGenerationSettings.Builder builder, boolean canyon) {
        builder.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.SATURN_CAVE_CARVER);
        if (canyon) {
            builder.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.SATURN_CANYON_CARVER);
        }
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_SATURN);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TIN_SATURN);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_IRON_SATURN);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_SATURN);
    }

    public static Biome createHexagonPolarPlateau(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addSaturnCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.SATURN_HEXAGON_TERRACE);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.SATURN_RING_RAIN_MONOLITH);

        return createBiome(genBuilder, mobBuilder, -0.4F, 0xBF9E5A, 0xAB8C4E, 0x8F743E);
    }

    public static Biome createGoldenHazePlains(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addSaturnCarversAndOres(genBuilder, false);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.SATURN_HEXAGON_TERRACE);
        genBuilder.addFeature(GenerationStep.Decoration.LAKES, GCPlacedFeatures.SATURN_AMMONIA_HYDROCARBON_LAKE);

        return createBiome(genBuilder, mobBuilder, 0.2F, 0xC9A762, 0xB59555, 0x967B44);
    }

    public static Biome createRingShadowShatterlands(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addSaturnCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.SATURN_RING_RAIN_MONOLITH);
        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.SATURN_RING_ICE_IMPACT);

        return createBiome(genBuilder, mobBuilder, -0.3F, 0xBA9A58, 0xA6884C, 0x8A703D);
    }

    public static Biome createAmmoniaDunes(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addSaturnCarversAndOres(genBuilder, false);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.SATURN_HEXAGON_TERRACE);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.SATURN_RING_RAIN_MONOLITH);

        return createBiome(genBuilder, mobBuilder, 0.5F, 0xCCA964, 0xB89757, 0x997D46);
    }

    public static Biome createHydrocarbonBasin(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addSaturnCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.LAKES, GCPlacedFeatures.SATURN_AMMONIA_HYDROCARBON_LAKE);
        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.SATURN_HEXAGON_TERRACE);

        return createBiome(genBuilder, mobBuilder, 0.3F, 0xC2A05D, 0xAE8E50, 0x917641);
    }

    public static Biome createHeliumRainCaverns(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addSaturnCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.SATURN_HELIUM_RAIN_SPELEOTHEM);
        genBuilder.addFeature(GenerationStep.Decoration.LAKES, GCPlacedFeatures.SATURN_AMMONIA_HYDROCARBON_LAKE);

        return createBiome(genBuilder, mobBuilder, 0.8F, 0xB89756, 0xA4864B, 0x876E3C);
    }

    public static Biome createRingIceGlaciers(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addSaturnCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.SATURN_RING_RAIN_MONOLITH);
        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.SATURN_RING_ICE_IMPACT);

        return createBiome(genBuilder, mobBuilder, -0.5F, 0xB59657, 0xA1844B, 0x856D3D);
    }

    public static Biome createMetallicMantleTrench(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addSaturnCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.SATURN_HELIUM_RAIN_SPELEOTHEM);
        genBuilder.addFeature(GenerationStep.Decoration.LAKES, GCPlacedFeatures.SATURN_AMMONIA_HYDROCARBON_LAKE);

        return createBiome(genBuilder, mobBuilder, 1.4F, 0xB08F50, 0x9C7E45, 0x806738);
    }

    private static Biome createBiome(
            BiomeGenerationSettings.Builder genBuilder,
            MobSpawnSettings.Builder mobBuilder,
            float temp,
            int skyColor,
            int fogColor,
            int waterColor
    ) {
        var biomeBuilder = new Biome.BiomeBuilder();
        var effectsBuilder = new BiomeSpecialEffects.Builder();
        BiomeDefaultFeatures.monsters(mobBuilder, 95, 5, 100, false);

        effectsBuilder.fogColor(fogColor)
                .waterColor(waterColor)
                .waterFogColor(0x524123)
                .skyColor(skyColor)
                .grassColorOverride(0xBD9D5A)
                .foliageColorOverride(0xA6894C)
                .backgroundMusic(new Music(GCSounds.MUSIC_ORBIT, 12000, 24000, true));

        return biomeBuilder
                .hasPrecipitation(false)
                .temperature(temp)
                .downfall(0.0F)
                .specialEffects(effectsBuilder.build())
                .mobSpawnSettings(mobBuilder.build())
                .generationSettings(genBuilder.build())
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .build();
    }

    public static void bootstrapRegistries(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(net.minecraft.core.registries.Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> worldCarvers = context.lookup(net.minecraft.core.registries.Registries.CONFIGURED_CARVER);

        register(context, GCBiomes.Saturn.HEXAGON_POLAR_PLATEAU, createHexagonPolarPlateau(placedFeatures, worldCarvers));
        register(context, GCBiomes.Saturn.GOLDEN_HAZE_PLAINS, createGoldenHazePlains(placedFeatures, worldCarvers));
        register(context, GCBiomes.Saturn.RING_SHADOW_SHATTERLANDS, createRingShadowShatterlands(placedFeatures, worldCarvers));
        register(context, GCBiomes.Saturn.AMMONIA_DUNES, createAmmoniaDunes(placedFeatures, worldCarvers));
        register(context, GCBiomes.Saturn.HYDROCARBON_BASIN, createHydrocarbonBasin(placedFeatures, worldCarvers));
        register(context, GCBiomes.Saturn.HELIUM_RAIN_CAVERNS, createHeliumRainCaverns(placedFeatures, worldCarvers));
        register(context, GCBiomes.Saturn.RING_ICE_GLACIERS, createRingIceGlaciers(placedFeatures, worldCarvers));
        register(context, GCBiomes.Saturn.METALLIC_MANTLE_TRENCH, createMetallicMantleTrench(placedFeatures, worldCarvers));
    }

    private static void register(BootstrapContext<Biome> context, ResourceKey<Biome> key, Biome biome) {
        context.register(key, biome);
    }
}
