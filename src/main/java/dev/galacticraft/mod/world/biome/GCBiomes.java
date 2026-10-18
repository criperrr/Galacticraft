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

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCSounds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Musics;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class GCBiomes {
    public static final class Moon {
        public static final ResourceKey<Biome> COMET_TUNDRA = key("comet_tundra");
        public static final ResourceKey<Biome> BASALTIC_MARE = key("basaltic_mare");
        public static final ResourceKey<Biome> LUNAR_HIGHLANDS = key("lunar_highlands");
        public static final ResourceKey<Biome> LUNAR_LOWLANDS = key("lunar_lowlands");
        public static final ResourceKey<Biome> OLIVINE_SPIKES = key("olivine_spikes");
    }

    public static final class Mars {
        public static final ResourceKey<Biome> MARS_FLAT = key("mars_flat");
        public static final ResourceKey<Biome> MARS_CANYON = key("mars_canyon");
        public static final ResourceKey<Biome> MARS_HIGHLANDS = key("mars_highlands");
    }

    public static final class Venus {
        public static final ResourceKey<Biome> VENUS_VALLEY = key("venus_valley");
        public static final ResourceKey<Biome> VENUS_FLAT = key("venus_flat");
        public static final ResourceKey<Biome> VENUS_MOUNTAIN = key("venus_mountain");
    }

    public static final class Asteroid {
        public static final ResourceKey<Biome> ASTEROID_FIELD = key("asteroid_field");
    }

    public static final class Mercury {
        // Surface Biomes
        public static final ResourceKey<Biome> INTER_CRATER_PLAINS = key("mercury_inter_crater_plains");
        public static final ResourceKey<Biome> SMOOTH_LAVA_PLAINS = key("mercury_smooth_lava_plains");
        public static final ResourceKey<Biome> CALORIS_BASIN = key("mercury_caloris_basin");
        public static final ResourceKey<Biome> CHAOTIC_TERRAIN = key("mercury_chaotic_terrain");
        public static final ResourceKey<Biome> POLAR_DARK_CRATERS = key("mercury_polar_dark_craters");

        // Subterranean 3D Biomes
        public static final ResourceKey<Biome> COLOSSAL_LAVA_TUBES = key("mercury_colossal_lava_tubes");
        public static final ResourceKey<Biome> MEGABRECCIA_CAVERNS = key("mercury_megabreccia_caverns");
        public static final ResourceKey<Biome> TECTONIC_CHASMS = key("mercury_tectonic_chasms");
        public static final ResourceKey<Biome> MAGMA_DEPTHS = key("mercury_magma_depths");
        public static final ResourceKey<Biome> HOLLOW_LABYRINTH = key("mercury_hollow_labyrinth");
    }

    public static final ResourceKey<Biome> SPACE = ResourceKey.create(Registries.BIOME, Constant.id("space"));

    public static Biome createSpaceBiome(HolderGetter<PlacedFeature> holderGetter, HolderGetter<ConfiguredWorldCarver<?>> holderGetter2) {
        Biome.BiomeBuilder builder = new Biome.BiomeBuilder();
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        BiomeGenerationSettings.Builder genSettings = new BiomeGenerationSettings.Builder(holderGetter, holderGetter2);
        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder();
        effects.fogColor(0).waterColor(4159204).waterFogColor(329011).skyColor(0)
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .backgroundMusic(Musics.createGameMusic(GCSounds.MUSIC_ORBIT));

        return builder
                .downfall(0)
                .temperature(1)
                .specialEffects(effects.build())
                .mobSpawnSettings(spawns.build())
                .hasPrecipitation(false)
                .generationSettings(genSettings.build())
                .temperatureAdjustment(Biome.TemperatureModifier.NONE).build();
    }

    public static void bootstrapRegistries(BootstrapContext<Biome> context) { // moj-map typo :(
        HolderGetter<PlacedFeature> featureLookup = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carverLookup = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(SPACE, createSpaceBiome(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER)));
        context.register(Moon.COMET_TUNDRA, MoonBiomes.createCometTundra(featureLookup, carverLookup));
        context.register(Moon.BASALTIC_MARE, MoonBiomes.createBasalticMare(featureLookup, carverLookup));
        context.register(Moon.LUNAR_HIGHLANDS, MoonBiomes.createLunarHighlands(featureLookup, carverLookup));
        context.register(Moon.LUNAR_LOWLANDS, MoonBiomes.createLunarLowlands(featureLookup, carverLookup));
        context.register(Moon.OLIVINE_SPIKES, MoonBiomes.createOlivineSpikes(featureLookup, carverLookup));

        context.register(Mars.MARS_FLAT, MarsBiomes.createMarsFlat(featureLookup, carverLookup));
        context.register(Mars.MARS_CANYON, MarsBiomes.createMarsCanyon(featureLookup, carverLookup));
        context.register(Mars.MARS_HIGHLANDS, MarsBiomes.createMarsHighlands(featureLookup, carverLookup));

        context.register(Venus.VENUS_VALLEY, VenusBiomes.createVenusValley(featureLookup, carverLookup));
        context.register(Venus.VENUS_FLAT, VenusBiomes.createVenusFlat(featureLookup, carverLookup));
        context.register(Venus.VENUS_MOUNTAIN, VenusBiomes.createVenusMountain(featureLookup, carverLookup));

        context.register(Asteroid.ASTEROID_FIELD, AsteroidBiomes.asteroid(featureLookup, carverLookup));

        context.register(Mercury.INTER_CRATER_PLAINS, MercuryBiomes.createInterCraterPlains(featureLookup, carverLookup));
        context.register(Mercury.SMOOTH_LAVA_PLAINS, MercuryBiomes.createSmoothLavaPlains(featureLookup, carverLookup));
        context.register(Mercury.CALORIS_BASIN, MercuryBiomes.createCalorisBasin(featureLookup, carverLookup));
        context.register(Mercury.CHAOTIC_TERRAIN, MercuryBiomes.createChaoticTerrain(featureLookup, carverLookup));
        context.register(Mercury.POLAR_DARK_CRATERS, MercuryBiomes.createPolarDarkCraters(featureLookup, carverLookup));

        context.register(Mercury.COLOSSAL_LAVA_TUBES, MercuryBiomes.createColossalLavaTubes(featureLookup, carverLookup));
        context.register(Mercury.MEGABRECCIA_CAVERNS, MercuryBiomes.createMegabrecciaCaverns(featureLookup, carverLookup));
        context.register(Mercury.TECTONIC_CHASMS, MercuryBiomes.createTectonicChasms(featureLookup, carverLookup));
        context.register(Mercury.MAGMA_DEPTHS, MercuryBiomes.createMagmaDepths(featureLookup, carverLookup));
        context.register(Mercury.HOLLOW_LABYRINTH, MercuryBiomes.createHollowLabyrinth(featureLookup, carverLookup));
    }

    @Contract(pure = true)
    public static @NotNull ResourceKey<Biome> key(String id) {
        return Constant.key(Registries.BIOME, id);
    }
}
