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

package dev.galacticraft.mod.world.gen;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.world.gen.surfacerule.EnceladusSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.JupiterSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.MarsSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.MercurySurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.MoonSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.NeptuneSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.OberonSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.SaturnSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.TitanSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.TitaniaSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.TritonSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.UranusSurfaceRules;
import dev.galacticraft.mod.world.gen.surfacerule.VenusSurfaceRules;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * This class describes how terrain should be shaped based on the given density fuctions
 */
public class GCNoiseGeneratorSettings {
    public static final ResourceKey<NoiseGeneratorSettings> MOON = key("moon");
    public static final ResourceKey<NoiseGeneratorSettings> MARS = key("mars");
    public static final ResourceKey<NoiseGeneratorSettings> VENUS = key("venus");
    public static final ResourceKey<NoiseGeneratorSettings> MERCURY = key("mercury");
    public static final ResourceKey<NoiseGeneratorSettings> JUPITER = key("jupiter");
    public static final ResourceKey<NoiseGeneratorSettings> SATURN = key("saturn");
    public static final ResourceKey<NoiseGeneratorSettings> URANUS = key("uranus");
    public static final ResourceKey<NoiseGeneratorSettings> NEPTUNE = key("neptune");
    public static final ResourceKey<NoiseGeneratorSettings> TITAN = key("titan");
    public static final ResourceKey<NoiseGeneratorSettings> ENCELADUS = key("enceladus");
    public static final ResourceKey<NoiseGeneratorSettings> TITANIA = key("titania");
    public static final ResourceKey<NoiseGeneratorSettings> OBERON = key("oberon");
    public static final ResourceKey<NoiseGeneratorSettings> TRITON = key("triton");

    public static void bootstrapRegistries(BootstrapContext<NoiseGeneratorSettings> context) {
        HolderGetter<DensityFunction> densityLookup = context.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise.NoiseParameters> noiseLookup = context.lookup(Registries.NOISE);

        context.register(MOON, new NoiseGeneratorSettings(
                NoiseSettings.create(-32, 256, 1, 2),
                GCBlocks.MOON_ROCK.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.moon(densityLookup, noiseLookup),
                MoonSurfaceRules.MOON,
                new OverworldBiomeBuilder().spawnTarget(),
                -32,
                false,
                false,
                false,
                false
        ));

        context.register(MARS, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2),
                GCBlocks.MARS_STONE.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.mars(densityLookup, noiseLookup),
                MarsSurfaceRules.MARS,
                new OverworldBiomeBuilder().spawnTarget(),
                -64,
                false,
                false,
                false,
                false
        ));

        context.register(VENUS, new NoiseGeneratorSettings(
                NoiseSettings.create(-32, 256, 1, 2),
                GCBlocks.HARD_VENUS_ROCK.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.venus(densityLookup, noiseLookup),
                VenusSurfaceRules.VENUS,
                new OverworldBiomeBuilder().spawnTarget(),
                -32,
                false,
                false,
                false,
                false
        ));

        context.register(MERCURY, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2), // Limits from Y = -64 to 320 (height = 384)
                GCBlocks.MERCURY_BASALT.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.mercury(densityLookup, noiseLookup),
                MercurySurfaceRules.MERCURY,
                new OverworldBiomeBuilder().spawnTarget(),
                -64, // Sea level / fluid floor
                false,
                false,
                false,
                false
        ));

        context.register(JUPITER, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2), // Open gas atmosphere Y=0..320 + sparse hyperbaric core Y=-64..-20
                GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.outerPlanetRouter(
                        densityLookup,
                        noiseLookup,
                        GCDensityFunctions.Jupiter.CONTINENTS,
                        GCDensityFunctions.Jupiter.DEPTH,
                        GCDensityFunctions.Jupiter.SOLID_TERRAIN,
                        GCDensityFunctions.Jupiter.FINAL_DENSITY
                ),
                JupiterSurfaceRules.JUPITER,
                new OverworldBiomeBuilder().spawnTarget(),
                -64,
                false,
                false,
                false,
                false
        ));

        context.register(SATURN, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2), // Open gas atmosphere Y=0..320 + sparse hyperbaric core Y=-64..-20
                GCBlocks.SATURN_HELIUM_MANTLE_ROCK.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.outerPlanetRouter(
                        densityLookup,
                        noiseLookup,
                        GCDensityFunctions.Saturn.CONTINENTS,
                        GCDensityFunctions.Saturn.DEPTH,
                        GCDensityFunctions.Saturn.SOLID_TERRAIN,
                        GCDensityFunctions.Saturn.FINAL_DENSITY
                ),
                SaturnSurfaceRules.SATURN,
                new OverworldBiomeBuilder().spawnTarget(),
                -64,
                false,
                false,
                false,
                false
        ));

        context.register(URANUS, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2), // Open ice-giant atmosphere Y=0..320 + sparse hyperbaric core Y=-64..-20
                GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.outerPlanetRouter(
                        densityLookup,
                        noiseLookup,
                        GCDensityFunctions.Uranus.CONTINENTS,
                        GCDensityFunctions.Uranus.DEPTH,
                        GCDensityFunctions.Uranus.SOLID_TERRAIN,
                        GCDensityFunctions.Uranus.FINAL_DENSITY
                ),
                UranusSurfaceRules.URANUS,
                new OverworldBiomeBuilder().spawnTarget(),
                -64,
                false,
                false,
                false,
                false
        ));

        context.register(NEPTUNE, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2), // Open ice-giant atmosphere Y=0..320 + sparse hyperbaric core Y=-64..-20
                GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.outerPlanetRouter(
                        densityLookup,
                        noiseLookup,
                        GCDensityFunctions.Neptune.CONTINENTS,
                        GCDensityFunctions.Neptune.DEPTH,
                        GCDensityFunctions.Neptune.SOLID_TERRAIN,
                        GCDensityFunctions.Neptune.FINAL_DENSITY
                ),
                NeptuneSurfaceRules.NEPTUNE,
                new OverworldBiomeBuilder().spawnTarget(),
                -64,
                false,
                false,
                false,
                false
        ));

        // TITAN: Dense N2/CH4 atmosphere + Liquid Methane lakes (seaLevel = 62)
        context.register(TITAN, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2),
                GCBlocks.TITAN_WATER_ICE_ROCK.defaultBlockState(),
                GCBlocks.LIQUID_METHANE.defaultBlockState(),
                GCNoiseGeneratorSettings.outerPlanetRouter(
                        densityLookup,
                        noiseLookup,
                        GCDensityFunctions.Titan.CONTINENTS,
                        GCDensityFunctions.Titan.DEPTH,
                        GCDensityFunctions.Titan.SOLID_TERRAIN,
                        GCDensityFunctions.Titan.FINAL_DENSITY
                ),
                TitanSurfaceRules.TITAN,
                new OverworldBiomeBuilder().spawnTarget(),
                62,
                false,
                false,
                false,
                false
        ));

        // ENCELADUS: High-albedo ice shell + hydrothermal subsurface ocean (seaLevel = 20)
        context.register(ENCELADUS, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2),
                GCBlocks.ENCELADUS_ICE_CRUST.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                GCNoiseGeneratorSettings.outerPlanetRouter(
                        densityLookup,
                        noiseLookup,
                        GCDensityFunctions.Enceladus.CONTINENTS,
                        GCDensityFunctions.Enceladus.DEPTH,
                        GCDensityFunctions.Enceladus.SOLID_TERRAIN,
                        GCDensityFunctions.Enceladus.FINAL_DENSITY
                ),
                EnceladusSurfaceRules.ENCELADUS,
                new OverworldBiomeBuilder().spawnTarget(),
                20,
                false,
                false,
                false,
                false
        ));

        // TITANIA: Messina Chasmata tectonic canyons
        context.register(TITANIA, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2),
                GCBlocks.TITANIA_ICE_SILICATE.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.outerPlanetRouter(
                        densityLookup,
                        noiseLookup,
                        GCDensityFunctions.Titania.CONTINENTS,
                        GCDensityFunctions.Titania.DEPTH,
                        GCDensityFunctions.Titania.SOLID_TERRAIN,
                        GCDensityFunctions.Titania.FINAL_DENSITY
                ),
                TitaniaSurfaceRules.TITANIA,
                new OverworldBiomeBuilder().spawnTarget(),
                -64,
                false,
                false,
                false,
                false
        ));

        // OBERON: Dark reddish cratered world
        context.register(OBERON, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2),
                GCBlocks.OBERON_DARK_ROCK.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.outerPlanetRouter(
                        densityLookup,
                        noiseLookup,
                        GCDensityFunctions.Oberon.CONTINENTS,
                        GCDensityFunctions.Oberon.DEPTH,
                        GCDensityFunctions.Oberon.SOLID_TERRAIN,
                        GCDensityFunctions.Oberon.FINAL_DENSITY
                ),
                OberonSurfaceRules.OBERON,
                new OverworldBiomeBuilder().spawnTarget(),
                -64,
                false,
                false,
                false,
                false
        ));

        // TRITON: Cantaloupe terrain & nitrogen cryogeysers
        context.register(TRITON, new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 1, 2),
                GCBlocks.TRITON_NITROGEN_ICE.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                GCNoiseGeneratorSettings.outerPlanetRouter(
                        densityLookup,
                        noiseLookup,
                        GCDensityFunctions.Triton.CONTINENTS,
                        GCDensityFunctions.Triton.DEPTH,
                        GCDensityFunctions.Triton.SOLID_TERRAIN,
                        GCDensityFunctions.Triton.FINAL_DENSITY
                ),
                TritonSurfaceRules.TRITON,
                new OverworldBiomeBuilder().spawnTarget(),
                -64,
                false,
                false,
                false,
                false
        ));
    }

    public static NoiseRouter outerPlanetRouter(
            HolderGetter<DensityFunction> densityLookup,
            HolderGetter<NormalNoise.NoiseParameters> noiseLookup,
            ResourceKey<DensityFunction> continentsKey,
            ResourceKey<DensityFunction> depthKey,
            ResourceKey<DensityFunction> solidTerrainKey,
            ResourceKey<DensityFunction> finalDensityKey
    ) {
        DensityFunction shiftX = GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.SHIFT_X);
        DensityFunction shiftZ = GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.SHIFT_Z);
        DensityFunction finalDensity = GCDensityFunctions.getFunction(densityLookup, finalDensityKey);

        return new NoiseRouter(
                DensityFunctions.zero(), // barrierNoise = zero() per SKILL.md Section 3.2
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.14, noiseLookup.getOrThrow(Noises.TEMPERATURE_LARGE))),
                DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.14, noiseLookup.getOrThrow(Noises.VEGETATION_LARGE))),
                GCDensityFunctions.getFunction(densityLookup, continentsKey),
                DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.16, noiseLookup.getOrThrow(Noises.EROSION_LARGE))),
                GCDensityFunctions.getFunction(densityLookup, depthKey),
                GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.RIDGES),
                GCDensityFunctions.getFunction(densityLookup, solidTerrainKey), // initialDensityWithoutJaggedness decoupled from caves!
                DensityFunctions.blendDensity(finalDensity),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero()
        );
    }

    public static NoiseRouter mercury(HolderGetter<DensityFunction> densityLookup, HolderGetter<NormalNoise.NoiseParameters> noiseLookup) {
        DensityFunction shiftX = GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.SHIFT_X);
        DensityFunction shiftZ = GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.SHIFT_Z);
        DensityFunction finalDensity = GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mercury.FINAL_DENSITY);

        return new NoiseRouter(
                DensityFunctions.zero(), // barrierNoise (disabled aquifer barrier checks for celestial bodies without water)
                DensityFunctions.zero(), // fluidLevelFloodednessNoise
                DensityFunctions.zero(), // fluidLevelSpreadNoise
                DensityFunctions.zero(), // lavaNoise
                DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noiseLookup.getOrThrow(Noises.TEMPERATURE))),
                DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noiseLookup.getOrThrow(Noises.VEGETATION))),
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mercury.CONTINENTS),
                GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.EROSION),
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mercury.DEPTH),
                GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.RIDGES),
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mercury.SOLID_TERRAIN), // initialDensityWithoutJaggedness (true planetary surface for preliminarySurfaceLevel)
                DensityFunctions.interpolated(DensityFunctions.blendDensity(finalDensity)),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero()
        );
    }

    public static NoiseRouter moon(HolderGetter<DensityFunction> densityLookup, HolderGetter<NormalNoise.NoiseParameters> noiseLookup) {
        DensityFunction shiftX = GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.SHIFT_X);
        DensityFunction shiftZ = GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.SHIFT_Z);
        DensityFunction finalDensity = GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Moon.FINAL_DENSITY);
        return new NoiseRouter(
                DensityFunctions.zero(), // barrierNoise
                DensityFunctions.zero(), // fluidLevelFloodednessNoise
                DensityFunctions.zero(), // fluidLevelSpreadNoise
                DensityFunctions.zero(), // lavaNoise
                DensityFunctions.shiftedNoise2d(
                        shiftX, shiftZ, 0.25, noiseLookup.getOrThrow(Noises.TEMPERATURE)
                ), // temperature
                DensityFunctions.shiftedNoise2d(
                        shiftX, shiftZ, 0.25, noiseLookup.getOrThrow(Noises.VEGETATION)
                ), // vegetation
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Moon.CONTINENTS), // continents
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Moon.EROSION), // erosion
                GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.DEPTH), // depth
                GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.RIDGES), // ridges
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Moon.SOLID_TERRAIN), // initialDensityWithoutJaggedness
                DensityFunctions.blendDensity(finalDensity), // finalDensity
                DensityFunctions.zero(), // veinToggle
                DensityFunctions.zero(), // veinRidged
                DensityFunctions.zero()  // veinGap
        );
    }

    public static NoiseRouter mars(HolderGetter<DensityFunction> densityLookup, HolderGetter<NormalNoise.NoiseParameters> noiseLookup) {
        DensityFunction solidTerrain = GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mars.SOLID_TERRAIN);
        DensityFunction finalDensity = GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mars.FINAL_DENSITY);
        return new NoiseRouter(
                DensityFunctions.zero(), // barrierNoise
                DensityFunctions.zero(), // fluidLevelFloodednessNoise
                DensityFunctions.zero(), // fluidLevelSpreadNoise
                DensityFunctions.zero(), // lavaNoise
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mars.TEMPERATURE), // temperature (flatCache)
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mars.VEGETATION), // vegetation (flatCache)
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mars.CONTINENTS), // continents (cache2d)
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mars.EROSION), // erosion (cache2d)
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mars.DEPTH), // depth
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Mars.RIDGES), // ridges (cache2d)
                solidTerrain, // initialDensityWithoutJaggedness (cave-free SOLID_TERRAIN)
                DensityFunctions.blendDensity(finalDensity), // finalDensity (no outer interpolated)
                DensityFunctions.zero(), // veinToggle
                DensityFunctions.zero(), // veinRidged
                DensityFunctions.zero()  // veinGap
        );
    }

    public static NoiseRouter venus(HolderGetter<DensityFunction> densityLookup, HolderGetter<NormalNoise.NoiseParameters> noiseLookup) {
        DensityFunction shiftX = GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.SHIFT_X);
        DensityFunction shiftZ = GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.SHIFT_Z);
        DensityFunction finalDensity = GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Venus.FINAL_DENSITY);
        return new NoiseRouter(
                DensityFunctions.zero(), // barrierNoise
                DensityFunctions.zero(), // fluidLevelFloodednessNoise
                DensityFunctions.zero(), // fluidLevelSpreadNoise
                DensityFunctions.zero(), // lavaNoise
                DensityFunctions.shiftedNoise2d(
                        shiftX, shiftZ, 0.25, noiseLookup.getOrThrow(Noises.TEMPERATURE)
                ), // temperature
                DensityFunctions.shiftedNoise2d(
                        shiftX, shiftZ, 0.25, noiseLookup.getOrThrow(Noises.VEGETATION)
                ), // vegetation
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Venus.CONTINENTS), // continents
                GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.EROSION), // erosion
                GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.DEPTH), // depth
                GCDensityFunctions.getFunction(densityLookup, NoiseRouterData.RIDGES), // ridges
                GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Venus.SOLID_TERRAIN), // initialDensityWithoutJaggedness
                DensityFunctions.blendDensity(GCDensityFunctions.getFunction(densityLookup, GCDensityFunctions.Venus.FINAL_DENSITY)), // finalDensity
                DensityFunctions.zero(), // veinToggle
                DensityFunctions.zero(), // veinRidged
                DensityFunctions.zero()  // veinGap
        );
    }

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull ResourceKey<NoiseGeneratorSettings> key(String id) {
        return Constant.key(Registries.NOISE_SETTINGS, id);
    }
}
