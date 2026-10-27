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

public class UranusBiomes {

    private static void addUranusCarversAndOres(BiomeGenerationSettings.Builder builder, boolean canyon) {
        builder.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.URANUS_CAVE_CARVER);
        if (canyon) {
            builder.addCarver(GenerationStep.Carving.AIR, GCConfiguredCarvers.URANUS_CANYON_CARVER);
        }
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_COPPER_URANUS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TIN_URANUS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_ALUMINUM_URANUS);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GCOrePlacedFeatures.ORE_TITANIUM_URANUS);
    }

    public static Biome createMirandaCliffscarps(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addUranusCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.URANUS_METHANE_ICE_SPIRE);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.URANUS_FROZEN_SULFIDE_OUTCROP);

        return createBiome(genBuilder, mobBuilder, -1.6F, 0x5EA8B8, 0x4E92A1, 0x3D7885);
    }

    public static Biome createCyanMethaneGlaciers(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addUranusCarversAndOres(genBuilder, false);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.URANUS_METHANE_ICE_SPIRE);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.URANUS_COMET_IMPACT);

        return createBiome(genBuilder, mobBuilder, -1.8F, 0x66B0BF, 0x5599A8, 0x437E8C);
    }

    public static Biome createTiltedPolarWastes(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addUranusCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.URANUS_METHANE_ICE_SPIRE);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.URANUS_COMET_IMPACT);

        return createBiome(genBuilder, mobBuilder, -2.0F, 0x62ACBC, 0x5295A4, 0x407B88);
    }

    public static Biome createRimeIcePlains(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addUranusCarversAndOres(genBuilder, false);

        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.URANUS_FROZEN_SULFIDE_OUTCROP);
        genBuilder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, GCPlacedFeatures.URANUS_METHANE_ICE_SPIRE);

        return createBiome(genBuilder, mobBuilder, -1.4F, 0x6AB4C4, 0x589DAD, 0x468290);
    }

    public static Biome createCryovolcanicRift(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addUranusCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.URANUS_FROZEN_SULFIDE_OUTCROP);
        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.URANUS_SUPERIONIC_ICE_PILLAR);

        return createBiome(genBuilder, mobBuilder, -1.0F, 0x5AA2B2, 0x4B8D9B, 0x3A7380);
    }

    public static Biome createSuperionicIceOcean(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addUranusCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.URANUS_SUPERIONIC_ICE_PILLAR);
        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.URANUS_DIAMOND_RAIN_GEODE);

        return createBiome(genBuilder, mobBuilder, -0.6F, 0x569CAC, 0x478796, 0x376E7A);
    }

    public static Biome createDiamondMantleCaverns(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addUranusCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.URANUS_SUPERIONIC_ICE_PILLAR);
        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.URANUS_DIAMOND_RAIN_GEODE);

        return createBiome(genBuilder, mobBuilder, -0.4F, 0x5296A6, 0x448290, 0x346975);
    }

    public static Biome createSubglacialAmmoniaSea(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        var mobBuilder = new MobSpawnSettings.Builder();
        var genBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);

        BiomeDefaultFeatures.addDefaultCrystalFormations(genBuilder);
        addUranusCarversAndOres(genBuilder, true);

        genBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, GCPlacedFeatures.URANUS_SUPERIONIC_ICE_PILLAR);
        genBuilder.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, GCPlacedFeatures.URANUS_FROZEN_SULFIDE_OUTCROP);

        return createBiome(genBuilder, mobBuilder, -0.8F, 0x589FAF, 0x498A99, 0x39707D);
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
                .waterFogColor(0x24474F)
                .skyColor(skyColor)
                .grassColorOverride(0x63ADBD)
                .foliageColorOverride(0x5296A6)
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

        register(context, GCBiomes.Uranus.MIRANDA_CLIFFSCARPS, createMirandaCliffscarps(placedFeatures, worldCarvers));
        register(context, GCBiomes.Uranus.CYAN_METHANE_GLACIERS, createCyanMethaneGlaciers(placedFeatures, worldCarvers));
        register(context, GCBiomes.Uranus.TILTED_POLAR_WASTES, createTiltedPolarWastes(placedFeatures, worldCarvers));
        register(context, GCBiomes.Uranus.RIME_ICE_PLAINS, createRimeIcePlains(placedFeatures, worldCarvers));
        register(context, GCBiomes.Uranus.CRYOVOLCANIC_RIFT, createCryovolcanicRift(placedFeatures, worldCarvers));
        register(context, GCBiomes.Uranus.SUPERIONIC_ICE_OCEAN, createSuperionicIceOcean(placedFeatures, worldCarvers));
        register(context, GCBiomes.Uranus.DIAMOND_MANTLE_CAVERNS, createDiamondMantleCaverns(placedFeatures, worldCarvers));
        register(context, GCBiomes.Uranus.SUBGLACIAL_AMMONIA_SEA, createSubglacialAmmoniaSea(placedFeatures, worldCarvers));
    }

    private static void register(BootstrapContext<Biome> context, ResourceKey<Biome> key, Biome biome) {
        context.register(key, biome);
    }
}
