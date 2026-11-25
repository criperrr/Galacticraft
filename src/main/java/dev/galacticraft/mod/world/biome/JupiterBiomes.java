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
import dev.galacticraft.mod.world.gen.feature.GCOrePlacedFeatures;
import net.minecraft.core.HolderGetter;
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

public class JupiterBiomes {

    private static void addAbyssalCoreOres(BiomeGenerationSettings.Builder builder) {
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_PALLADIUM_JUPITER);
    }

    public static Biome createRedStormVortex(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        addAbyssalCoreOres(genBuilder);
        return createBiome(genBuilder, mobBuilder, 0.2F, 0x9E4B2E, 0x8A3E24, 0x7A3720);
    }

    public static Biome createOchreBandPlateau(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        addAbyssalCoreOres(genBuilder);
        return createBiome(genBuilder, mobBuilder, 0.1F, 0xAD5A38, 0x984C2C, 0x854226);
    }

    public static Biome createAmmoniaCloudBasin(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        addAbyssalCoreOres(genBuilder);
        return createBiome(genBuilder, mobBuilder, -0.2F, 0xB56842, 0x9E5734, 0x8A4B2C);
    }

    public static Biome createJovianBadlands(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        addAbyssalCoreOres(genBuilder);
        return createBiome(genBuilder, mobBuilder, 0.1F, 0xA65232, 0x914528, 0x7E3C22);
    }

    public static Biome createFulguriteShatterlands(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        addAbyssalCoreOres(genBuilder);
        return createBiome(genBuilder, mobBuilder, -0.3F, 0x9C4E30, 0x874126, 0x753820);
    }

    public static Biome createMetallicHydrogenOcean(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        addAbyssalCoreOres(genBuilder);
        return createBiome(genBuilder, mobBuilder, 0.4F, 0x94462A, 0x7F3B22, 0x6E331D);
    }

    public static Biome createHyperbaricCoreDepths(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        addAbyssalCoreOres(genBuilder);
        return createBiome(genBuilder, mobBuilder, 0.6F, 0x8C4026, 0x78361F, 0x662E1A);
    }

    public static Biome createThunderheadCaverns(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        addAbyssalCoreOres(genBuilder);
        return createBiome(genBuilder, mobBuilder, 0.3F, 0x99492C, 0x843D24, 0x72351F);
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

        effectsBuilder.fogColor(fogColor)
                .waterColor(waterColor)
                .waterFogColor(0x4A2214)
                .skyColor(skyColor)
                .grassColorOverride(0x9E5A38)
                .foliageColorOverride(0x8C4A2B)
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

        register(context, GCBiomes.Jupiter.RED_STORM_VORTEX, createRedStormVortex(placedFeatures, worldCarvers));
        register(context, GCBiomes.Jupiter.OCHRE_BAND_PLATEAU, createOchreBandPlateau(placedFeatures, worldCarvers));
        register(context, GCBiomes.Jupiter.AMMONIA_CLOUD_BASIN, createAmmoniaCloudBasin(placedFeatures, worldCarvers));
        register(context, GCBiomes.Jupiter.JOVIAN_BADLANDS, createJovianBadlands(placedFeatures, worldCarvers));
        register(context, GCBiomes.Jupiter.FULGURITE_SHATTERLANDS, createFulguriteShatterlands(placedFeatures, worldCarvers));
        register(context, GCBiomes.Jupiter.METALLIC_HYDROGEN_OCEAN, createMetallicHydrogenOcean(placedFeatures, worldCarvers));
        register(context, GCBiomes.Jupiter.HYPERBARIC_CORE_DEPTHS, createHyperbaricCoreDepths(placedFeatures, worldCarvers));
        register(context, GCBiomes.Jupiter.THUNDERHEAD_CAVERNS, createThunderheadCaverns(placedFeatures, worldCarvers));
    }

    private static void register(BootstrapContext<Biome> context, ResourceKey<Biome> key, Biome biome) {
        context.register(key, biome);
    }
}
