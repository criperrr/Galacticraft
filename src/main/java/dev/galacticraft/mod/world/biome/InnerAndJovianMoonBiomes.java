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
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.sounds.Music;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Biome builders for Phase 3 Inner &amp; Jovian Moons:
 * Phobos, Deimos (Mars) and Io, Europa, Ganymede, Callisto (Jupiter).
 */
public final class InnerAndJovianMoonBiomes {
    private InnerAndJovianMoonBiomes() {
    }

    public static void bootstrapRegistries(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        // PHOBOS
        context.register(GCBiomes.Phobos.STICKNEY_BASIN, createPhobosBiome(features, carvers, 0.2F, 0x0A0908));
        context.register(GCBiomes.Phobos.TIDAL_GROOVES, createPhobosBiome(features, carvers, 0.25F, 0x0C0A09));
        context.register(GCBiomes.Phobos.REGOLITH_HIGHLANDS, createPhobosBiome(features, carvers, 0.15F, 0x0D0B0A));
        context.register(GCBiomes.Phobos.FRACTURE_CAVERNS, createPhobosBiome(features, carvers, 0.1F, 0x070606));

        // DEIMOS
        context.register(GCBiomes.Deimos.SMOOTH_PLAINS, createDeimosBiome(features, carvers, 0.2F, 0x0C0B0A));
        context.register(GCBiomes.Deimos.CRATER_SLOPES, createDeimosBiome(features, carvers, 0.15F, 0x0E0D0B));
        context.register(GCBiomes.Deimos.CORE_CAVERNS, createDeimosBiome(features, carvers, 0.1F, 0x070606));

        // IO
        context.register(GCBiomes.Io.SULFUR_PLAINS, createIoBiome(features, carvers, false, 1.4F, 0x2B240C, 0x4A3A10));
        context.register(GCBiomes.Io.VOLCANIC_CALDERA, createIoBiome(features, carvers, true, 2.4F, 0x3D1E0B, 0x6E2A0E));
        context.register(GCBiomes.Io.SILICATE_FLOWS, createIoBiome(features, carvers, true, 1.8F, 0x261B12, 0x3C2616));
        context.register(GCBiomes.Io.MAGMA_TUBES, createIoBiome(features, carvers, false, 2.2F, 0x2A1008, 0x521808));

        // EUROPA
        context.register(GCBiomes.Europa.ICE_PLAINS, createEuropaBiome(features, carvers, -0.8F, 0x0C131C, 0x2A6A94));
        context.register(GCBiomes.Europa.LINEAE_RIDGES, createEuropaBiome(features, carvers, -0.7F, 0x121216, 0x2E6288));
        context.register(GCBiomes.Europa.CHAOS_TERRAIN, createEuropaBiome(features, carvers, -0.75F, 0x0E141E, 0x246E9C));
        context.register(GCBiomes.Europa.SUBSURFACE_OCEAN, createEuropaBiome(features, carvers, -0.4F, 0x081B2C, 0x1A6B9C));

        // GANYMEDE (cold polar frost temp <= 0.15F triggers AstroSky AuroraRenderer!)
        context.register(GCBiomes.Ganymede.SULCI_RIDGES, createGanymedeBiome(features, carvers, -0.3F, 0x0C1016));
        context.register(GCBiomes.Ganymede.DARK_REGIO, createGanymedeBiome(features, carvers, -0.2F, 0x0A0B0E));
        context.register(GCBiomes.Ganymede.POLAR_FROST, createGanymedeBiome(features, carvers, -0.9F, 0x0B1520));
        context.register(GCBiomes.Ganymede.ICE_CAVERNS, createGanymedeBiome(features, carvers, -0.5F, 0x080D14));

        // CALLISTO
        context.register(GCBiomes.Callisto.CRATERED_PLAINS, createCallistoBiome(features, carvers, -0.4F, 0x090A0C));
        context.register(GCBiomes.Callisto.VALHALLA_RINGS, createCallistoBiome(features, carvers, -0.35F, 0x0B0C10));
        context.register(GCBiomes.Callisto.ICE_PEAKS, createCallistoBiome(features, carvers, -0.7F, 0x0D1218));
        context.register(GCBiomes.Callisto.ANCIENT_CAVERNS, createCallistoBiome(features, carvers, -0.45F, 0x07080A));
    }

    private static Biome createPhobosBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, float temp, int fogColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.PHOBOS_CAVE_CARVER);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_METEORIC_IRON_PHOBOS);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_GRAPHITE_PHOBOS);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_IRON_PHOBOS);
        return buildMoonBiome(gen, temp, 0x000000, fogColor, 0x3F76E4, 0x050533);
    }

    private static Biome createDeimosBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, float temp, int fogColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.DEIMOS_CAVE_CARVER);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_SILICON_DEIMOS);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_ALUMINUM_DEIMOS);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_IRON_DEIMOS);
        return buildMoonBiome(gen, temp, 0x000000, fogColor, 0x3F76E4, 0x050533);
    }

    private static Biome createIoBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, boolean volcanicSurface, float temp, int fogColor, int waterColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.IO_CAVE_CARVER);
        if (volcanicSurface) {
            gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.IO_CANYON_CARVER);
            gen.addFeature(GenerationStep.Decoration.LAKES, GCPlacedFeatures.MERCURY_LAVA_LAKE);
            gen.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.VENUS_VOLCANO);
        }
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_PALLADIUM_IO);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_SULFIDE_IO);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_IO);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.MAGMA_PATCH_IO);
        return buildMoonBiome(gen, temp, 0x100B04, fogColor, waterColor, 0x3B1A08);
    }

    private static Biome createEuropaBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, float temp, int fogColor, int waterColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.EUROPA_CAVE_CARVER);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DENSE_ICE_EUROPA);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_NIOBIUM_EUROPA);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_IRON_EUROPA);
        return buildMoonBiome(gen, temp, 0x000000, fogColor, waterColor, 0x0E3A5C);
    }

    private static Biome createGanymedeBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, float temp, int fogColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.GANYMEDE_CAVE_CARVER);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_NIOBIUM_GANYMEDE);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_GANYMEDE);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_PALLADIUM_GANYMEDE);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DENSE_ICE_GANYMEDE);
        return buildMoonBiome(gen, temp, 0x000000, fogColor, 0x2B5D84, 0x0B243A);
    }

    private static Biome createCallistoBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, float temp, int fogColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.CALLISTO_CAVE_CARVER);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_IRON_CALLISTO);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_ALUMINUM_CALLISTO);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_NIOBIUM_CALLISTO);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DENSE_ICE_CALLISTO);
        return buildMoonBiome(gen, temp, 0x000000, fogColor, 0x2C5472, 0x091A2B);
    }

    private static Biome buildMoonBiome(
            BiomeGenerationSettings.Builder genBuilder,
            float temperature,
            int skyColor,
            int fogColor,
            int waterColor,
            int waterFogColor
    ) {
        var effects = new BiomeSpecialEffects.Builder()
                .skyColor(skyColor)
                .fogColor(fogColor)
                .waterColor(waterColor)
                .waterFogColor(waterFogColor)
                .grassColorOverride(0x6D7872)
                .foliageColorOverride(0x5E6B64)
                .backgroundMusic(new Music(GCSounds.MUSIC_ORBIT, 12000, 24000, true));

        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(temperature)
                .downfall(0.0F)
                .specialEffects(effects.build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(genBuilder.build())
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .build();
    }
}
