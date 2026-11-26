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
 * Biome builders for Phase 4 Outer Moons:
 * Titan &amp; Enceladus (Saturn), Titania &amp; Oberon (Uranus), and Triton (Neptune).
 */
public final class OuterMoonBiomes {
    private OuterMoonBiomes() {
    }

    public static void bootstrapRegistries(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        // TITAN (Dense amber-orange N2/CH4 haze, hydrocarbon dunes & liquid methane seas)
        context.register(GCBiomes.Titan.METHANE_SEAS, createTitanBiome(features, carvers, -0.65F, 0xC87A2E, 0xD98B38, 0x2A3644));
        context.register(GCBiomes.Titan.THOLIN_DUNES, createTitanBiome(features, carvers, -0.60F, 0xD48632, 0xE0943E, 0x333C46));
        context.register(GCBiomes.Titan.ICE_HIGHLANDS, createTitanBiome(features, carvers, -0.70F, 0xBA722C, 0xCC8238, 0x2B3848));
        context.register(GCBiomes.Titan.CRYO_CAVERNS, createTitanBiome(features, carvers, -0.75F, 0x5C3A1B, 0x6E4520, 0x1D2834));

        // ENCELADUS (High-albedo snow/ice, Tiger Stripes vents & hydrothermal subsurface ocean)
        context.register(GCBiomes.Enceladus.SNOW_PLAINS, createEnceladusBiome(features, carvers, -0.90F, 0x0F1722, 0x2B729E));
        context.register(GCBiomes.Enceladus.TIGER_STRIPES, createEnceladusBiome(features, carvers, -0.75F, 0x102230, 0x2E8BB8));
        context.register(GCBiomes.Enceladus.CRATERED_RIDGES, createEnceladusBiome(features, carvers, -0.85F, 0x0D141E, 0x286A92));
        context.register(GCBiomes.Enceladus.HYDROTHERMAL_OCEAN, createEnceladusBiome(features, carvers, -0.40F, 0x091D2E, 0x1E7CA8));

        // TITANIA (Messina Chasmata tectonic canyons, ice-silicate frost plains)
        context.register(GCBiomes.Titania.MESSINA_CHASMATA, createTitaniaBiome(features, carvers, true, -0.80F, 0x0A0E14));
        context.register(GCBiomes.Titania.FROST_PLAINS, createTitaniaBiome(features, carvers, false, -0.85F, 0x0C1118));
        context.register(GCBiomes.Titania.SCARP_HIGHLANDS, createTitaniaBiome(features, carvers, true, -0.82F, 0x0B1016));
        context.register(GCBiomes.Titania.FAULT_CAVERNS, createTitaniaBiome(features, carvers, false, -0.75F, 0x070A0F));

        // OBERON (Dark reddish cratered plains & impact peaks)
        context.register(GCBiomes.Oberon.RED_PLAINS, createOberonBiome(features, carvers, -0.80F, 0x120B0C));
        context.register(GCBiomes.Oberon.IMPACT_PEAKS, createOberonBiome(features, carvers, -0.85F, 0x100B0E));
        context.register(GCBiomes.Oberon.DARK_BASINS, createOberonBiome(features, carvers, -0.78F, 0x0C090B));
        context.register(GCBiomes.Oberon.ANCIENT_CAVERNS, createOberonBiome(features, carvers, -0.75F, 0x080608));

        // TRITON (Cantaloupe terrain, nitrogen cryogeysers & superionic caverns)
        context.register(GCBiomes.Triton.CANTALOUPE_TERRAIN, createTritonBiome(features, carvers, -0.95F, 0x141016));
        context.register(GCBiomes.Triton.NITROGEN_GEYSERS, createTritonBiome(features, carvers, -0.92F, 0x101622));
        context.register(GCBiomes.Triton.POLAR_ICE_CAP, createTritonBiome(features, carvers, -0.98F, 0x16141C));
        context.register(GCBiomes.Triton.SUPERIONIC_CAVERNS, createTritonBiome(features, carvers, -0.90F, 0x0A1424));
    }

    private static Biome createTitanBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, float temp, int skyColor, int fogColor, int waterColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.TITAN_CAVE_CARVER);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_MAGNESIUM_TITAN);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_IRON_TITAN);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_SILICON_TITAN);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DENSE_ICE_TITAN);
        return buildMoonBiome(gen, temp, skyColor, fogColor, waterColor, 0x1E2630);
    }

    private static Biome createEnceladusBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, float temp, int fogColor, int waterColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.ENCELADUS_CAVE_CARVER);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_MAGNESIUM_ENCELADUS);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_NIOBIUM_ENCELADUS);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DENSE_ICE_ENCELADUS);
        return buildMoonBiome(gen, temp, 0x000000, fogColor, waterColor, 0x0D3856);
    }

    private static Biome createTitaniaBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, boolean canyonZones, float temp, int fogColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.TITANIA_CAVE_CARVER);
        if (canyonZones) {
            gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.TITANIA_CANYON_CARVER);
        }
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_URANIUM_TITANIA);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_LONSDALEITE_TITANIA);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_TITANIA);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DENSE_ICE_TITANIA);
        return buildMoonBiome(gen, temp, 0x000000, fogColor, 0x2B5472, 0x091A2B);
    }

    private static Biome createOberonBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, float temp, int fogColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.OBERON_CAVE_CARVER);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_URANIUM_OBERON);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_NIOBIUM_OBERON);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DENSE_ICE_OBERON);
        return buildMoonBiome(gen, temp, 0x000000, fogColor, 0x3A2E38, 0x161016);
    }

    private static Biome createTritonBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers, float temp, int fogColor) {
        var gen = new BiomeGenerationSettings.Builder(features, carvers);
        gen.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.TRITON_CAVE_CARVER);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_SUPERIONIC_TRITON);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_XENON_CLATHRATE_TRITON);
        gen.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_DENSE_ICE_TRITON);
        return buildMoonBiome(gen, temp, 0x000000, fogColor, 0x3E74A8, 0x0C2240);
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
