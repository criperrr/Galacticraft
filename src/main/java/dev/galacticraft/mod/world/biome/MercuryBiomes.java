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

public class MercuryBiomes {

    /**
     * Helper base to create Mercury biomes with:
     * - ZERO monster spawning (completely sterile world as requested)
     * - Perpetual black sky and extreme thermal atmospheric conditions
     */
    public static Biome mercury(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup, BiomeGenerationSettings.Builder generation, float temperature) {
        var spawns = new MobSpawnSettings.Builder(); // NO mob spawns added! Strictly sterile.

        var effects = new BiomeSpecialEffects.Builder()
                .fogColor(0x050505)
                .skyColor(0x000000)
                .waterColor(0x3f76e4)
                .waterFogColor(0x050533)
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .backgroundMusic(Musics.createGameMusic(GCSounds.MUSIC_ORBIT));

        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(temperature)
                .downfall(0.0F)
                .specialEffects(effects.build())
                .mobSpawnSettings(spawns.build())
                .generationSettings(generation.build())
                .build();
    }

    private static void addStandardMercuryOres(BiomeGenerationSettings.Builder generation) {
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_ALUMINUM_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_NATIVE_IRON_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_GRAPHITE_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_SULFIDE_MERCURY);
    }

    // ==========================================
    // SURFACE BIOMES (Base Y = 120)
    // ==========================================

    public static Biome createInterCraterPlains(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_COLLAPSED_RILLES_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_SKYLIGHT_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_TECTONIC_FISSURE_CARVER);
        addStandardMercuryOres(generation);
        return mercury(featureLookup, carverLookup, generation, 2.0F);
    }

    public static Biome createSmoothLavaPlains(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_COLLAPSED_RILLES_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_SKYLIGHT_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_SUBLIMATION_FUNNEL_CARVER);
        addStandardMercuryOres(generation);
        return mercury(featureLookup, carverLookup, generation, 2.0F);
    }

    public static Biome createCalorisBasin(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_SCARP_PORTAL_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_TECTONIC_FISSURE_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_SKYLIGHT_CARVER);
        addStandardMercuryOres(generation);
        return mercury(featureLookup, carverLookup, generation, 2.0F);
    }

    public static Biome createChaoticTerrain(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_SCARP_PORTAL_CARVER);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_TECTONIC_FISSURE_CARVER);
        addStandardMercuryOres(generation);
        return mercury(featureLookup, carverLookup, generation, 2.0F);
    }

    public static Biome createPolarDarkCraters(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_SCARP_PORTAL_CARVER);
        addStandardMercuryOres(generation);
        return mercury(featureLookup, carverLookup, generation, -0.5F); // Extreme cold in permanent shadow
    }

    // ==========================================
    // MONUMENTAL SUBTERRANEAN 3D BIOMES
    // ==========================================

    /**
     * Tubos de Lava Monumentais (Mercury Colossal Lava Tubes) [Y = 20 to 95]
     * Continuous horizontal conduits, smooth cooling terraces, rich in Titanium and Aluminum.
     */
    public static Biome createColossalLavaTubes(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_ALUMINUM_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.MERCURY_MEGABRECCIA_SPELEOTHEM);
        return mercury(featureLookup, carverLookup, generation, 2.0F);
    }

    /**
     * Salões Abobadados de Megabrecha (Megabreccia Impact Caverns) [Y = -30 to 50]
     * Colossal domed halls, breccia walls with graphite (LRM), rich in Titanium and Native Iron.
     */
    public static Biome createMegabrecciaCaverns(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.MERCURY_MEGABRECCIA_SPELEOTHEM);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_NATIVE_IRON_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_GRAPHITE_MERCURY);
        return mercury(featureLookup, carverLookup, generation, 2.0F);
    }

    /**
     * Fendas Tectônicas Abissais (Tectonic Chasm Caverns) [Y = -30 to 110]
     * Extensional shear fractures, vertical abysses with bridge blocks, rich in Copper and Titanium.
     */
    public static Biome createTectonicChasms(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_TECTONIC_FISSURE_CARVER);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_MERCURY);
        return mercury(featureLookup, carverLookup, generation, 2.0F);
    }

    /**
     * Abismo Magmático Profundo (Mercury Magma Depths) [Y = -64 to -10]
     * Basal gallery with basaltic magma lakes, massive rock pillars, rich in Exotic Sulfide ore crusts.
     */
    public static Biome createMagmaDepths(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addFeature(GenerationStep.Decoration.LAKES, GCPlacedFeatures.MERCURY_LAVA_LAKE);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.MAGMA_PATCH_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_NATIVE_IRON_MERCURY);
        return mercury(featureLookup, carverLookup, generation, 2.0F);
    }

    /**
     * Labirinto Esponjoso de Voláteis (Volatile Hollow Labyrinth) [Y = 65 to 115]
     * Globular desvolatilization network, white sublimation dust, high-albedo deposits and graphite.
     */
    public static Biome createHollowLabyrinth(HolderGetter<PlacedFeature> featureLookup, HolderGetter<ConfiguredWorldCarver<?>> carverLookup) {
        var generation = new BiomeGenerationSettings.Builder(featureLookup, carverLookup);
        generation.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.MERCURY_SUBLIMATION_FUNNEL_CARVER);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_ALUMINUM_MERCURY);
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_GRAPHITE_MERCURY);
        return mercury(featureLookup, carverLookup, generation, 2.0F);
    }
}
