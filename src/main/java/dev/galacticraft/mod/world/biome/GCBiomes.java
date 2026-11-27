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
        public static final ResourceKey<Biome> MARS_IMPACT_BASIN = key("mars_impact_basin");
        public static final ResourceKey<Biome> MARS_DUNE_SEA = key("mars_dune_sea");
        public static final ResourceKey<Biome> MARS_THARSIS = key("mars_tharsis");
        public static final ResourceKey<Biome> MARS_POLAR_ICE_CAP = key("mars_polar_ice_cap");
        public static final ResourceKey<Biome> MARS_LAVA_TUBES = key("mars_lava_tubes");
        public static final ResourceKey<Biome> MARS_DEEP_CAVERNS = key("mars_deep_caverns");
        public static final ResourceKey<Biome> MARS_ICE_CAVERNS = key("mars_ice_caverns");
    }

    public static final class Venus {
        public static final ResourceKey<Biome> VENUS_VALLEY = key("venus_valley");
        public static final ResourceKey<Biome> VENUS_FLAT = key("venus_flat");
        public static final ResourceKey<Biome> VENUS_MOUNTAIN = key("venus_mountain");
    }

    public static final class Asteroid {
        public static final ResourceKey<Biome> ASTEROID_FIELD = key("asteroid_field");
        public static final ResourceKey<Biome> INNER_SILICATE_BELT = key("asteroid_inner_silicate_belt");
        public static final ResourceKey<Biome> METALLIC_CORE_CLUSTER = key("asteroid_metallic_core_cluster");
        public static final ResourceKey<Biome> KIRKWOOD_RESONANCE_GAP = key("asteroid_kirkwood_gap");
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

    public static final class Jupiter {
        // Surface Biomes
        public static final ResourceKey<Biome> AMMONIA_PLAINS = key("jupiter_ammonia_plains");
        public static final ResourceKey<Biome> GREAT_RED_SPOT = key("jupiter_great_red_spot");
        public static final ResourceKey<Biome> SULFURIC_BELTS = key("jupiter_sulfuric_belts");
        public static final ResourceKey<Biome> METALLIC_RIDGES = key("jupiter_metallic_ridges");
        public static final ResourceKey<Biome> POLAR_AURORA_CRATERS = key("jupiter_polar_aurora_craters");

        // Subterranean 3D Biomes
        public static final ResourceKey<Biome> METALLIC_HYDROGEN_CAVERNS = key("jupiter_metallic_hydrogen_caverns");
        public static final ResourceKey<Biome> CYCLONIC_RIFTS = key("jupiter_cyclonic_rifts");
        public static final ResourceKey<Biome> CORE_MAGMA_DEPTHS = key("jupiter_core_magma_depths");

        // Aliases used by JupiterBiomes & JupiterSurfaceRules
        public static final ResourceKey<Biome> RED_STORM_VORTEX = GREAT_RED_SPOT;
        public static final ResourceKey<Biome> OCHRE_BAND_PLATEAU = SULFURIC_BELTS;
        public static final ResourceKey<Biome> AMMONIA_CLOUD_BASIN = AMMONIA_PLAINS;
        public static final ResourceKey<Biome> JOVIAN_BADLANDS = METALLIC_RIDGES;
        public static final ResourceKey<Biome> FULGURITE_SHATTERLANDS = POLAR_AURORA_CRATERS;
        public static final ResourceKey<Biome> METALLIC_HYDROGEN_OCEAN = METALLIC_HYDROGEN_CAVERNS;
        public static final ResourceKey<Biome> HYPERBARIC_CORE_DEPTHS = CORE_MAGMA_DEPTHS;
        public static final ResourceKey<Biome> THUNDERHEAD_CAVERNS = CYCLONIC_RIFTS;
    }

    public static final class Saturn {
        // Surface Biomes
        public static final ResourceKey<Biome> GOLDEN_HAZE_PLAINS = key("saturn_golden_haze_plains");
        public static final ResourceKey<Biome> HYDROCARBON_SEAS = key("saturn_hydrocarbon_seas");
        public static final ResourceKey<Biome> RING_SHADOW_HIGHLANDS = key("saturn_ring_shadow_highlands");
        public static final ResourceKey<Biome> AMMONIA_STORM_BANDS = key("saturn_ammonia_storm_bands");
        public static final ResourceKey<Biome> HEXAGON_POLAR_VORTEX = key("saturn_hexagon_polar_vortex");

        // Subterranean 3D Biomes
        public static final ResourceKey<Biome> HELIUM_RAIN_CAVERNS = key("saturn_helium_rain_caverns");
        public static final ResourceKey<Biome> CRYSTALLINE_RING_TUBES = key("saturn_crystalline_ring_tubes");
        public static final ResourceKey<Biome> MANTLE_CORE_ABYSS = key("saturn_mantle_core_abyss");

        // Aliases used by SaturnBiomes & SaturnSurfaceRules
        public static final ResourceKey<Biome> HEXAGON_POLAR_PLATEAU = HEXAGON_POLAR_VORTEX;
        public static final ResourceKey<Biome> RING_SHADOW_SHATTERLANDS = RING_SHADOW_HIGHLANDS;
        public static final ResourceKey<Biome> AMMONIA_DUNES = AMMONIA_STORM_BANDS;
        public static final ResourceKey<Biome> HYDROCARBON_BASIN = HYDROCARBON_SEAS;
        public static final ResourceKey<Biome> RING_ICE_GLACIERS = CRYSTALLINE_RING_TUBES;
        public static final ResourceKey<Biome> METALLIC_MANTLE_TRENCH = MANTLE_CORE_ABYSS;
    }

    public static final class Uranus {
        // Surface Biomes
        public static final ResourceKey<Biome> FROZEN_METHANE_PLAINS = key("uranus_frozen_methane_plains");
        public static final ResourceKey<Biome> MIRANDA_VERONA_CLIFFS = key("uranus_miranda_verona_cliffs");
        public static final ResourceKey<Biome> CRYO_GEYSER_BASIN = key("uranus_cryo_geyser_basin");
        public static final ResourceKey<Biome> TILTED_POLAR_CAPS = key("uranus_tilted_polar_caps");
        public static final ResourceKey<Biome> RING_DEBRIS_BADLANDS = key("uranus_ring_debris_badlands");

        // Subterranean 3D Biomes
        public static final ResourceKey<Biome> DIAMOND_MANTLE_CAVERNS = key("uranus_diamond_mantle_caverns");
        public static final ResourceKey<Biome> GLACIAL_FAULT_CHASMS = key("uranus_glacial_fault_chasms");
        public static final ResourceKey<Biome> SUPERIONIC_ICE_DEPTHS = key("uranus_superionic_ice_depths");

        // Aliases used by UranusBiomes & UranusSurfaceRules
        public static final ResourceKey<Biome> MIRANDA_CLIFFSCARPS = MIRANDA_VERONA_CLIFFS;
        public static final ResourceKey<Biome> CYAN_METHANE_GLACIERS = FROZEN_METHANE_PLAINS;
        public static final ResourceKey<Biome> TILTED_POLAR_WASTES = TILTED_POLAR_CAPS;
        public static final ResourceKey<Biome> RIME_ICE_PLAINS = RING_DEBRIS_BADLANDS;
        public static final ResourceKey<Biome> CRYOVOLCANIC_RIFT = CRYO_GEYSER_BASIN;
        public static final ResourceKey<Biome> SUPERIONIC_ICE_OCEAN = SUPERIONIC_ICE_DEPTHS;
        public static final ResourceKey<Biome> SUBGLACIAL_AMMONIA_SEA = GLACIAL_FAULT_CHASMS;
    }

    public static final class Neptune {
        // Surface Biomes
        public static final ResourceKey<Biome> AZURE_METHANE_PLAINS = key("neptune_azure_methane_plains");
        public static final ResourceKey<Biome> GREAT_DARK_SPOT = key("neptune_great_dark_spot");
        public static final ResourceKey<Biome> TRITON_NITROGEN_GEYSERS = key("neptune_triton_nitrogen_geysers");
        public static final ResourceKey<Biome> SUPERSONIC_WIND_RIDGES = key("neptune_supersonic_wind_ridges");
        public static final ResourceKey<Biome> CRYOGENIC_POLAR_ABYSS = key("neptune_cryogenic_polar_abyss");

        // Subterranean 3D Biomes
        public static final ResourceKey<Biome> DIAMOND_RAIN_CAVERNS = key("neptune_diamond_rain_caverns");
        public static final ResourceKey<Biome> CRYO_MAGMA_TUBES = key("neptune_cryo_magma_tubes");
        public static final ResourceKey<Biome> ABYSSAL_MANTLE_DEPTHS = key("neptune_abyssal_mantle_depths");

        // Aliases used by NeptuneBiomes & NeptuneSurfaceRules
        public static final ResourceKey<Biome> DARK_SPOT_VORTEX = GREAT_DARK_SPOT;
        public static final ResourceKey<Biome> SUPERSONIC_WIND_PLAINS = AZURE_METHANE_PLAINS;
        public static final ResourceKey<Biome> TRITON_GEYSER_FIELDS = TRITON_NITROGEN_GEYSERS;
        public static final ResourceKey<Biome> NITROGEN_ICE_SHELVES = CRYOGENIC_POLAR_ABYSS;
        public static final ResourceKey<Biome> COBALT_STORM_HIGHLANDS = SUPERSONIC_WIND_RIDGES;
    }

    public static final class Phobos {
        public static final ResourceKey<Biome> STICKNEY_BASIN = key("phobos_stickney_basin");
        public static final ResourceKey<Biome> TIDAL_GROOVES = key("phobos_tidal_grooves");
        public static final ResourceKey<Biome> REGOLITH_HIGHLANDS = key("phobos_regolith_highlands");
        public static final ResourceKey<Biome> FRACTURE_CAVERNS = key("phobos_fracture_caverns");
    }

    public static final class Deimos {
        public static final ResourceKey<Biome> SMOOTH_PLAINS = key("deimos_smooth_plains");
        public static final ResourceKey<Biome> CRATER_SLOPES = key("deimos_crater_slopes");
        public static final ResourceKey<Biome> CORE_CAVERNS = key("deimos_core_caverns");
    }

    public static final class Io {
        public static final ResourceKey<Biome> SULFUR_PLAINS = key("io_sulfur_plains");
        public static final ResourceKey<Biome> VOLCANIC_CALDERA = key("io_volcanic_caldera");
        public static final ResourceKey<Biome> SILICATE_FLOWS = key("io_silicate_flows");
        public static final ResourceKey<Biome> MAGMA_TUBES = key("io_magma_tubes");
    }

    public static final class Europa {
        public static final ResourceKey<Biome> ICE_PLAINS = key("europa_ice_plains");
        public static final ResourceKey<Biome> LINEAE_RIDGES = key("europa_lineae_ridges");
        public static final ResourceKey<Biome> CHAOS_TERRAIN = key("europa_chaos_terrain");
        public static final ResourceKey<Biome> SUBSURFACE_OCEAN = key("europa_subsurface_ocean");
    }

    public static final class Ganymede {
        public static final ResourceKey<Biome> SULCI_RIDGES = key("ganymede_sulci_ridges");
        public static final ResourceKey<Biome> DARK_REGIO = key("ganymede_dark_regio");
        public static final ResourceKey<Biome> POLAR_FROST = key("ganymede_polar_frost");
        public static final ResourceKey<Biome> ICE_CAVERNS = key("ganymede_ice_caverns");
    }

    public static final class Callisto {
        public static final ResourceKey<Biome> CRATERED_PLAINS = key("callisto_cratered_plains");
        public static final ResourceKey<Biome> VALHALLA_RINGS = key("callisto_valhalla_rings");
        public static final ResourceKey<Biome> ICE_PEAKS = key("callisto_ice_peaks");
        public static final ResourceKey<Biome> ANCIENT_CAVERNS = key("callisto_ancient_caverns");
    }

    public static final class Titan {
        public static final ResourceKey<Biome> METHANE_SEAS = key("titan_methane_seas");
        public static final ResourceKey<Biome> THOLIN_DUNES = key("titan_tholin_dunes");
        public static final ResourceKey<Biome> ICE_HIGHLANDS = key("titan_ice_highlands");
        public static final ResourceKey<Biome> CRYO_CAVERNS = key("titan_cryo_caverns");
    }

    public static final class Enceladus {
        public static final ResourceKey<Biome> SNOW_PLAINS = key("enceladus_snow_plains");
        public static final ResourceKey<Biome> TIGER_STRIPES = key("enceladus_tiger_stripes");
        public static final ResourceKey<Biome> CRATERED_RIDGES = key("enceladus_cratered_ridges");
        public static final ResourceKey<Biome> HYDROTHERMAL_OCEAN = key("enceladus_hydrothermal_ocean");
    }

    public static final class Titania {
        public static final ResourceKey<Biome> MESSINA_CHASMATA = key("titania_messina_chasmata");
        public static final ResourceKey<Biome> FROST_PLAINS = key("titania_frost_plains");
        public static final ResourceKey<Biome> SCARP_HIGHLANDS = key("titania_scarp_highlands");
        public static final ResourceKey<Biome> FAULT_CAVERNS = key("titania_fault_caverns");
    }

    public static final class Oberon {
        public static final ResourceKey<Biome> RED_PLAINS = key("oberon_red_plains");
        public static final ResourceKey<Biome> IMPACT_PEAKS = key("oberon_impact_peaks");
        public static final ResourceKey<Biome> DARK_BASINS = key("oberon_dark_basins");
        public static final ResourceKey<Biome> ANCIENT_CAVERNS = key("oberon_ancient_caverns");
    }

    public static final class Triton {
        public static final ResourceKey<Biome> CANTALOUPE_TERRAIN = key("triton_cantaloupe_terrain");
        public static final ResourceKey<Biome> NITROGEN_GEYSERS = key("triton_nitrogen_geysers");
        public static final ResourceKey<Biome> POLAR_ICE_CAP = key("triton_polar_ice_cap");
        public static final ResourceKey<Biome> SUPERIONIC_CAVERNS = key("triton_superionic_caverns");
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
        context.register(Mars.MARS_IMPACT_BASIN, MarsBiomes.createMarsImpactBasin(featureLookup, carverLookup));
        context.register(Mars.MARS_DUNE_SEA, MarsBiomes.createMarsDuneSea(featureLookup, carverLookup));
        context.register(Mars.MARS_THARSIS, MarsBiomes.createMarsTharsis(featureLookup, carverLookup));
        context.register(Mars.MARS_POLAR_ICE_CAP, MarsBiomes.createMarsPolarIceCap(featureLookup, carverLookup));
        context.register(Mars.MARS_LAVA_TUBES, MarsBiomes.createMarsLavaTubes(featureLookup, carverLookup));
        context.register(Mars.MARS_DEEP_CAVERNS, MarsBiomes.createMarsDeepCaverns(featureLookup, carverLookup));
        context.register(Mars.MARS_ICE_CAVERNS, MarsBiomes.createMarsIceCaverns(featureLookup, carverLookup));

        context.register(Venus.VENUS_VALLEY, VenusBiomes.createVenusValley(featureLookup, carverLookup));
        context.register(Venus.VENUS_FLAT, VenusBiomes.createVenusFlat(featureLookup, carverLookup));
        context.register(Venus.VENUS_MOUNTAIN, VenusBiomes.createVenusMountain(featureLookup, carverLookup));

        context.register(Asteroid.ASTEROID_FIELD, AsteroidBiomes.asteroid(featureLookup, carverLookup));
        context.register(Asteroid.INNER_SILICATE_BELT, AsteroidBiomes.innerSilicateBelt(featureLookup, carverLookup));
        context.register(Asteroid.METALLIC_CORE_CLUSTER, AsteroidBiomes.metallicCoreCluster(featureLookup, carverLookup));
        context.register(Asteroid.KIRKWOOD_RESONANCE_GAP, AsteroidBiomes.kirkwoodGap(featureLookup, carverLookup));

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

        JupiterBiomes.bootstrapRegistries(context);
        SaturnBiomes.bootstrapRegistries(context);
        UranusBiomes.bootstrapRegistries(context);
        NeptuneBiomes.bootstrapRegistries(context);
        InnerAndJovianMoonBiomes.bootstrapRegistries(context);
        OuterMoonBiomes.bootstrapRegistries(context);
    }

    @Contract(pure = true)
    public static @NotNull ResourceKey<Biome> key(String id) {
        return Constant.key(Registries.BIOME, id);
    }
}
