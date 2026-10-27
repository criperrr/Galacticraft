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

public class NeptuneBiomes {

    private static void addNeptuneCarversAndOres(BiomeGenerationSettings.Builder builder, boolean canyon) {
        builder.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.NEPTUNE_CAVE_CARVER);
        if (canyon) {
            builder.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.NEPTUNE_CANYON_CARVER);
        }
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_NEPTUNE);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TIN_NEPTUNE);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_IRON_NEPTUNE);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_NEPTUNE);
    }

    public static Biome createDarkSpotVortex(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addNeptuneCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.NEPTUNE_GREAT_DARK_VORTEX);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.NEPTUNE_SUPERSONIC_WIND_RIDGE);

        return createBiome(genBuilder, mobBuilder, -1.8F, 0x264D8A, 0x1E3F73, 0x18325C);
    }

    public static Biome createSupersonicWindPlains(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addNeptuneCarversAndOres(genBuilder, false);

        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.NEPTUNE_SUPERSONIC_WIND_RIDGE);
        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.NEPTUNE_NITROGEN_CRYOGEYSER);

        return createBiome(genBuilder, mobBuilder, -1.6F, 0x2C5696, 0x23477E, 0x1C3966);
    }

    public static Biome createTritonGeyserFields(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addNeptuneCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.NEPTUNE_NITROGEN_CRYOGEYSER);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.NEPTUNE_SUPERSONIC_WIND_RIDGE);

        return createBiome(genBuilder, mobBuilder, -1.5F, 0x2E5899, 0x254982, 0x1E3B69);
    }

    public static Biome createNitrogenIceShelves(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addNeptuneCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.NEPTUNE_NITROGEN_CRYOGEYSER);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.NEPTUNE_SUPERSONIC_WIND_RIDGE);

        return createBiome(genBuilder, mobBuilder, -2.0F, 0x315C9E, 0x274C87, 0x203E6E);
    }

    public static Biome createCobaltStormHighlands(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addNeptuneCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.NEPTUNE_GREAT_DARK_VORTEX);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.NEPTUNE_SUPERSONIC_WIND_RIDGE);

        return createBiome(genBuilder, mobBuilder, -1.7F, 0x295190, 0x204378, 0x1A3561);
    }

    public static Biome createDiamondRainCaverns(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addNeptuneCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.NEPTUNE_CRYO_SPELEOTHEM);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.NEPTUNE_SUPERSONIC_WIND_RIDGE);

        return createBiome(genBuilder, mobBuilder, -0.8F, 0x244882, 0x1C3B6C, 0x162F57);
    }

    public static Biome createCryoMagmaTubes(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addNeptuneCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.NEPTUNE_CRYO_SPELEOTHEM);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.NEPTUNE_SUPERSONIC_WIND_RIDGE);

        return createBiome(genBuilder, mobBuilder, -0.6F, 0x22447C, 0x1A3866, 0x152D52);
    }

    public static Biome createAbyssalMantleDepths(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addNeptuneCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.NEPTUNE_CRYO_SPELEOTHEM);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.NEPTUNE_SUPERSONIC_WIND_RIDGE);

        return createBiome(genBuilder, mobBuilder, -0.4F, 0x1F4075, 0x183460, 0x132A4D);
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
                .waterFogColor(0x0E1F3B)
                .skyColor(skyColor)
                .grassColorOverride(0x2B5494)
                .foliageColorOverride(0x224378)
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

        register(context, GCBiomes.Neptune.DARK_SPOT_VORTEX, createDarkSpotVortex(placedFeatures, worldCarvers));
        register(context, GCBiomes.Neptune.SUPERSONIC_WIND_PLAINS, createSupersonicWindPlains(placedFeatures, worldCarvers));
        register(context, GCBiomes.Neptune.TRITON_GEYSER_FIELDS, createTritonGeyserFields(placedFeatures, worldCarvers));
        register(context, GCBiomes.Neptune.NITROGEN_ICE_SHELVES, createNitrogenIceShelves(placedFeatures, worldCarvers));
        register(context, GCBiomes.Neptune.COBALT_STORM_HIGHLANDS, createCobaltStormHighlands(placedFeatures, worldCarvers));
        register(context, GCBiomes.Neptune.DIAMOND_RAIN_CAVERNS, createDiamondRainCaverns(placedFeatures, worldCarvers));
        register(context, GCBiomes.Neptune.CRYO_MAGMA_TUBES, createCryoMagmaTubes(placedFeatures, worldCarvers));
        register(context, GCBiomes.Neptune.ABYSSAL_MANTLE_DEPTHS, createAbyssalMantleDepths(placedFeatures, worldCarvers));
    }

    private static void register(BootstrapContext<Biome> context, ResourceKey<Biome> key, Biome biome) {
        context.register(key, biome);
    }
}
