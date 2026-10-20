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
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

public class GCDensityFunctions {
    public static final ResourceKey<DensityFunction> NOODLES = createKey("caves/noodles");

    public static final class Moon {
        public static final ResourceKey<DensityFunction> EROSION = createKey("moon/erosion");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("moon/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("moon/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("moon/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("moon/smooth_relief");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("moon/final_density");
    }

    public static final class Mars {
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("mars/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("mars/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("mars/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("mars/smooth_relief");
        // Final Density handles overall terrain shape
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("mars/final_density");
    }

    public static final class Venus {
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("venus/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("venus/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("venus/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("venus/smooth_relief");
        // Final Density handles overall terrain shape
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("venus/final_density");
    }

    public static final class Asteroid {
        // Final Density handles overall terrain shape
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("asteroid/final_density");
    }

    public static final class Mercury {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("mercury/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("mercury/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("mercury/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("mercury/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("mercury/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("mercury/depth");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("mercury/final_density");
    }

    private static ResourceKey<DensityFunction> createKey(String id) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, Constant.id(id));
    }

    public static void bootstrapRegistries(BootstrapContext<DensityFunction> context) {
        var vanillaRegistry = context.lookup(Registries.DENSITY_FUNCTION);
        var noiseRegistry = context.lookup(Registries.NOISE);
        DensityFunction shiftX = getFunction(vanillaRegistry, NoiseRouterData.SHIFT_X);
        DensityFunction shiftZ = getFunction(vanillaRegistry, NoiseRouterData.SHIFT_Z);
        DensityFunction y = getFunction(vanillaRegistry, NoiseRouterData.Y);

        DensityFunction noodles = registerAndWrap(context, NOODLES, DensityFunctions.rangeChoice(
                DensityFunctions.interpolated(
                        DensityFunctions.rangeChoice(
                                y, -25, 45,
                                DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE), 1, 1),
                                DensityFunctions.constant(-1)
                        )
                ),
                -1000000, 0, DensityFunctions.constant(64),
                DensityFunctions.add(
                        DensityFunctions.interpolated(
                                DensityFunctions.rangeChoice(
                                        y, -25, 45,
                                        DensityFunctions.add(
                                                DensityFunctions.constant(-0.07500000000000001),
                                                DensityFunctions.mul(
                                                        DensityFunctions.constant(-0.025),
                                                        DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_THICKNESS), 1, 1)
                                                )
                                        ),
                                        DensityFunctions.constant(0)
                                )
                        ),
                        DensityFunctions.mul(
                                DensityFunctions.constant(1.5),
                                DensityFunctions.max(
                                        DensityFunctions.interpolated(
                                                DensityFunctions.rangeChoice(
                                                        y, -25, 45,
                                                        DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665, 2.6666666666666665),
                                                        DensityFunctions.zero()
                                                )
                                        ).abs(),
                                        DensityFunctions.interpolated(
                                                DensityFunctions.rangeChoice(
                                                        y, -25, 45,
                                                        DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665, 2.6666666666666665),
                                                        DensityFunctions.zero()
                                                )
                                        ).abs()
                                )
                        )
                )
        ));

        DensityFunction erosion = registerAndWrap(context, Moon.EROSION, DensityFunctions.flatCache(
                DensityFunctions.shiftedNoise2d(
                        shiftX, shiftZ, 1.0, noiseRegistry.getOrThrow(GCNoiseData.EROSION)
                )
        ));

        DensityFunction continents = getFunction(vanillaRegistry, NoiseRouterData.CONTINENTS);
        var continentsCoord = new DensityFunctions.Spline.Coordinate(vanillaRegistry.getOrThrow(NoiseRouterData.CONTINENTS));

        // Moon vertical gradient anchor: monotonically decreases from +1.5 at Y=-32 to -1.5 at Y=224 (0.0 at Y=96, 1 block = 0.01171875).
        DensityFunction moonBaseHeight = registerAndWrap(context, Moon.BASE_HEIGHT,
                DensityFunctions.yClampedGradient(-32, 224, 1.5, -1.5)
        );

        // Smooth C1 cubic Hermite spline for Moon base elevation replicating GC4 ChunkProviderMoon (MID_HEIGHT = 63..76)
        // and smooth transitions between Lunar Maria, Lowlands, and Highlands:
        // - BASALTIC_MARE & OLIVINE_SPIKES [-1.0, -0.35]: low-lying flat basaltic maria basins (Y ~ 62..68)
        // - LUNAR_LOWLANDS & COMET_TUNDRA [-0.35, 0.25]: classic GC4 rolling lunar regolith plains (Y ~ 68..80)
        // - LUNAR_HIGHLANDS [0.25, 1.0]: elevated rugged cratered highlands & lunar mountains (Y ~ 82..135)
        var moonOffsetSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, -0.39F, 0.04F)
                .addPoint(-0.55F, -0.36F, 0.12F)
                .addPoint(-0.35F, -0.32F, 0.24F)
                .addPoint(-0.05F, -0.25F, 0.22F)
                .addPoint(0.25F, -0.17F, 0.36F)
                .addPoint(0.50F, 0.02F, 0.72F)
                .addPoint(0.75F, 0.22F, 0.65F)
                .addPoint(1.00F, 0.36F, 0.45F)
                .build();
        DensityFunction moonOffset = registerAndWrap(context, Moon.OFFSET, DensityFunctions.spline(moonOffsetSpline));

        // Smooth C1 cubic Hermite spline for Moon relief amplitude replicating GC4 ChunkProviderMoon dual-scale blending:
        // - Maria [-1.0, -0.35]: smooth plains (noiseGen1 * 8 -> factor ~0.24..0.32)
        // - Lowlands [-0.35, 0.25]: rolling lunar hills (blended noiseGen1 & noiseGen2 -> factor ~0.35..0.62)
        // - Highlands [0.25, 1.0]: rugged lunar hills & mountain massifs (noiseGen2 * 24 -> factor ~0.68..1.35)
        var moonFactorSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, 0.24F, 0.04F)
                .addPoint(-0.55F, 0.28F, 0.14F)
                .addPoint(-0.35F, 0.35F, 0.28F)
                .addPoint(-0.05F, 0.46F, 0.35F)
                .addPoint(0.25F, 0.64F, 0.72F)
                .addPoint(0.50F, 0.94F, 0.95F)
                .addPoint(0.75F, 1.18F, 0.75F)
                .addPoint(1.00F, 1.35F, 0.50F)
                .build();
        DensityFunction moonFactor = registerAndWrap(context, Moon.FACTOR, DensityFunctions.spline(moonFactorSpline));

        // GC4 ChunkProviderMoon multi-generator relief:
        // - noiseGen1 (MOON_LOWLAND_NOISE, frequency 0.0125, amplitude 8)
        // - noiseGen2 (MOON_HIGHLAND_NOISE, frequency 0.015, amplitude 24)
        // - highland ridge absolute relief & basalt mare undulation
        DensityFunction moonLowlandNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.75, noiseRegistry.getOrThrow(GCNoiseData.MOON_LOWLAND_NOISE));
        DensityFunction moonHighlandNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.90, noiseRegistry.getOrThrow(GCNoiseData.MOON_HIGHLAND_NOISE));
        DensityFunction moonHighlandRidges = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.55, noiseRegistry.getOrThrow(GCNoiseData.MOON_HIGHLAND_NOISE)).abs();
        DensityFunction moonMareNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.60, noiseRegistry.getOrThrow(GCNoiseData.BASALT_MARE));

        DensityFunction moonSmoothRelief = registerAndWrap(context, Moon.SMOOTH_RELIEF,
                DensityFunctions.mul(
                        moonFactor,
                        DensityFunctions.add(
                                DensityFunctions.mul(DensityFunctions.constant(0.10), moonLowlandNoise),
                                DensityFunctions.add(
                                        DensityFunctions.mul(DensityFunctions.constant(0.11), moonHighlandNoise),
                                        DensityFunctions.add(
                                                DensityFunctions.mul(DensityFunctions.constant(0.08), moonHighlandRidges),
                                                DensityFunctions.mul(DensityFunctions.constant(0.05), moonMareNoise)
                                        )
                                )
                        )
                )
        );

        // Subtle 3D BlendedNoise weighted by moonFactor for natural highland scarps without floating islands
        DensityFunction moonSubtle3dRoughness = DensityFunctions.mul(
                moonFactor,
                DensityFunctions.mul(
                        DensityFunctions.constant(0.038),
                        BlendedNoise.createUnseeded(0.25, 0.125, 80.0, 160.0, 8.0)
                )
        );

        context.register(Moon.FINAL_DENSITY, DensityFunctions.min(
                DensityFunctions.add(
                        moonBaseHeight,
                        DensityFunctions.add(
                                moonOffset,
                                DensityFunctions.add(moonSmoothRelief, moonSubtle3dRoughness)
                        )
                ),
                noodles
        ));

        // Mars vertical gradient anchor: monotonically decreases from +1.5 at Y=-32 to -1.5 at Y=224 (0.0 at Y=96, 1 block = 0.01171875).
        DensityFunction marsBaseHeight = registerAndWrap(context, Mars.BASE_HEIGHT,
                DensityFunctions.yClampedGradient(-32, 224, 1.5, -1.5)
        );

        // Smooth C1 cubic Hermite spline for Mars base elevation replicating GC4 ChunkProviderMars + GalaxySpace WorldProviderMars_WE:
        // - MARS_CANYON [-1.0, -0.20]: GC4 VALLEY_HEIGHT_MOD (floor Y~52) & GalaxySpace Mars_Ravine (biomeSurfaceHeight=45 -> Y~48..58)
        // - MARS_FLAT [-0.20, 0.30]: GalaxySpace Mars_Plains (Y~80), GC4 MID_HEIGHT (Y=93), & GalaxySpace Mars_High_Plains (Y~100)
        // - MARS_HIGHLANDS [0.30, 1.0]: GC4 MOUNTAIN_HEIGHT_MOD=95 & GalaxySpace Mars_Mountains / Mars_Triangle_Mountains (Y~110..175)
        var marsOffsetSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, -0.50F, 0.04F)
                .addPoint(-0.40F, -0.45F, 0.25F)
                .addPoint(-0.20F, -0.18F, 0.65F)
                .addPoint(0.00F, -0.035F, 0.35F)
                .addPoint(0.30F, 0.05F, 0.45F)
                .addPoint(0.55F, 0.28F, 0.90F)
                .addPoint(0.80F, 0.48F, 0.70F)
                .addPoint(1.00F, 0.60F, 0.50F)
                .build();
        DensityFunction marsOffset = registerAndWrap(context, Mars.OFFSET, DensityFunctions.spline(marsOffsetSpline));

        // Smooth C1 cubic Hermite spline for Mars relief amplitude (GalaxySpace Ravine=1.2, Plains=1.5, Mountains=1.8..2.4 & GC4 mountain/hill mods)
        var marsFactorSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, 0.32F, 0.0F)
                .addPoint(-0.40F, 0.36F, 0.20F)
                .addPoint(-0.20F, 0.42F, 0.30F)
                .addPoint(0.00F, 0.52F, 0.35F)
                .addPoint(0.30F, 0.75F, 0.90F)
                .addPoint(0.55F, 1.15F, 1.20F)
                .addPoint(0.80F, 1.45F, 0.90F)
                .addPoint(1.00F, 1.65F, 0.60F)
                .build();
        DensityFunction marsFactor = registerAndWrap(context, Mars.FACTOR, DensityFunctions.spline(marsFactorSpline));

        // GC4 ChunkProviderMars multi-generator relief:
        // - noiseGen1 (MARS_BASE_NOISE, TERRAIN_HEIGHT_MOD = 12)
        // - noiseGen2 (MARS_HILL_NOISE, SMALL_FEATURE_HEIGHT_MOD = 26)
        // - |noiseGen3| (MARS_BASE_NOISE.abs(), MOUNTAIN_HEIGHT_MOD = 95 & GalaxySpace Mars_Triangle_Mountains ridges)
        // - GalaxySpace WE_Biome 4-octave persistence=0.65 layer (MARS_WE_TERRAIN_NOISE)
        DensityFunction marsBaseNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.85, noiseRegistry.getOrThrow(GCNoiseData.MARS_BASE_NOISE));
        DensityFunction marsHillNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 1.00, noiseRegistry.getOrThrow(GCNoiseData.MARS_HILL_NOISE));
        DensityFunction marsMountainRidges = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.55, noiseRegistry.getOrThrow(GCNoiseData.MARS_BASE_NOISE)).abs();
        DensityFunction marsWeTerrainNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.65, noiseRegistry.getOrThrow(GCNoiseData.MARS_WE_TERRAIN_NOISE));

        DensityFunction marsSmoothRelief = registerAndWrap(context, Mars.SMOOTH_RELIEF,
                DensityFunctions.mul(
                        marsFactor,
                        DensityFunctions.add(
                                DensityFunctions.mul(DensityFunctions.constant(0.11), marsBaseNoise),
                                DensityFunctions.add(
                                        DensityFunctions.mul(DensityFunctions.constant(0.09), marsHillNoise),
                                        DensityFunctions.add(
                                                DensityFunctions.mul(DensityFunctions.constant(0.08), marsMountainRidges),
                                                DensityFunctions.mul(DensityFunctions.constant(0.07), marsWeTerrainNoise)
                                        )
                                )
                        )
                )
        );

        // Subtle 3D BlendedNoise weighted by marsFactor for natural Martian cliff faces and canyon walls without floating islands
        DensityFunction marsSubtle3dRoughness = DensityFunctions.mul(
                marsFactor,
                DensityFunctions.mul(
                        DensityFunctions.constant(0.045),
                        BlendedNoise.createUnseeded(0.25, 0.125, 80.0, 160.0, 8.0)
                )
        );

        context.register(Mars.FINAL_DENSITY, DensityFunctions.add(
                marsBaseHeight,
                DensityFunctions.add(
                        marsOffset,
                        DensityFunctions.add(marsSmoothRelief, marsSubtle3dRoughness)
                )
        ));

        // Vertical gradient anchor: monotonically decreases from +1.5 at Y=-32 to -1.5 at Y=224 (0.0 at Y=96, 1 block = 0.01171875).
        DensityFunction venusBaseHeight = registerAndWrap(context, Venus.BASE_HEIGHT,
                DensityFunctions.yClampedGradient(-32, 224, 1.5, -1.5)
        );

        // Smooth C1 cubic Hermite spline for base elevation (replicates GC4 ChunkProviderVenus 5x5 parabolicField kernel
        // across BiomeGenVenusValley [rootHeight=-0.4 -> Y~60], BiomeGenFlatVenus [rootHeight=0.5 -> Y~73],
        // and BiomeGenVenusMountain [rootHeight=2.0 -> Y~95..106]).
        // Gentle derivatives prevent low-frequency continent transitions from appearing as diagonal ramps.
        var offsetSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, -0.42F, 0.02F)
                .addPoint(-0.35F, -0.39F, 0.12F)
                .addPoint(-0.15F, -0.32F, 0.22F)
                .addPoint(0.05F, -0.27F, 0.08F)
                .addPoint(0.25F, -0.24F, 0.18F)
                .addPoint(0.40F, -0.16F, 0.32F)
                .addPoint(0.70F, -0.02F, 0.35F)
                .addPoint(1.00F, 0.12F, 0.25F)
                .build();
        DensityFunction venusOffset = registerAndWrap(context, Venus.OFFSET, DensityFunctions.spline(offsetSpline));

        // Smooth C1 cubic Hermite spline for terrain heightVariation (replicates GC4 d9: Valley=0.2, Flat=0.4, Mountain=1.0..1.35)
        var factorSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, 0.24F, 0.0F)
                .addPoint(-0.35F, 0.28F, 0.15F)
                .addPoint(-0.15F, 0.36F, 0.25F)
                .addPoint(0.05F, 0.44F, 0.15F)
                .addPoint(0.25F, 0.52F, 0.35F)
                .addPoint(0.40F, 0.72F, 0.75F)
                .addPoint(0.70F, 1.08F, 0.75F)
                .addPoint(1.00F, 1.35F, 0.45F)
                .build();
        DensityFunction venusFactor = registerAndWrap(context, Venus.FACTOR, DensityFunctions.spline(factorSpline));

        // GC4 ChunkProviderVenus noiseGenSmooth1 (4 octaves, persistence 0.25, freq 0.015, +/- 20 blocks) +
        // GalaxySpace WE_Biome volcanic terrain layer (5 octaves, persistence 0.6) + ridge detail on mountains.
        DensityFunction gc4SmoothNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.96, noiseRegistry.getOrThrow(GCNoiseData.VENUS_SMOOTH_NOISE));
        DensityFunction gsVolcanicNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.55, noiseRegistry.getOrThrow(GCNoiseData.VENUS_VOLCANIC_NOISE));
        DensityFunction ridgeNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.40, noiseRegistry.getOrThrow(Noises.RIDGE));

        DensityFunction venusSmoothRelief = registerAndWrap(context, Venus.SMOOTH_RELIEF,
                DensityFunctions.mul(
                        venusFactor,
                        DensityFunctions.add(
                                DensityFunctions.mul(DensityFunctions.constant(0.26), gc4SmoothNoise),
                                DensityFunctions.add(
                                        DensityFunctions.mul(DensityFunctions.constant(0.15), gsVolcanicNoise),
                                        DensityFunctions.mul(DensityFunctions.constant(0.08), ridgeNoise)
                                )
                        )
                )
        );

        // GC4 ChunkProviderVenus 3D BlendedNoise (octaves1/octaves2/octaves3 in initializeNoiseField)
        // weighted by venusFactor for authentic 3D GC4 hills and craggy mountains while staying strictly monotonic vertically
        DensityFunction subtle3dRoughness = DensityFunctions.mul(
                venusFactor,
                DensityFunctions.mul(
                        DensityFunctions.constant(0.16),
                        BlendedNoise.createUnseeded(0.25, 0.125, 80.0, 160.0, 8.0)
                )
        );

        context.register(Venus.FINAL_DENSITY, DensityFunctions.add(
                venusBaseHeight,
                DensityFunctions.add(
                        venusOffset,
                        DensityFunctions.add(venusSmoothRelief, subtle3dRoughness)
                )
        ));

        context.register(Asteroid.FINAL_DENSITY, DensityFunctions.add(
                DensityFunctions.yClampedGradient(0, 90, 1, -1),
                BlendedNoise.createUnseeded(0.25, 0.375, 80.0, 160.0, 8.0)
        ));

        // ==========================================
        // MERCURY PROCEDURAL WORLDGEN & MONUMENTAL CAVES
        // Base plains at Y = 120, height Y = -64 to 320
        // ==========================================

        // 1. Depth function: 0.0 at surface Y = 128, scaling to 1.5 at bedrock Y = -64
        // Used by MultiNoise climate sampler for vertical 3D biome transitions
        DensityFunction mercuryDepth = registerAndWrap(context, Mercury.DEPTH,
                DensityFunctions.yClampedGradient(128, -64, 0.0, 1.5)
        );

        // 1. Continuous coordinate displacement & continuous continentalness
        // Eliminates 4x4 flat_cache stepping, chunk-border lines, and orthogonal 90-degree cliff staircases
        DensityFunction mercuryShiftX = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.25, 0.0);
        DensityFunction mercuryShiftZ = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.0, 0.25);
        DensityFunction mercuryContinents = registerAndWrap(context, Mercury.CONTINENTS,
                DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.25, noiseRegistry.getOrThrow(Noises.CONTINENTALNESS))
        );
        var mercuryContinentsCoord = new DensityFunctions.Spline.Coordinate(
                context.lookup(Registries.DENSITY_FUNCTION).getOrThrow(Mercury.CONTINENTS)
        );

        // 2. Base surface elevation gradient: crosses 0 at Y = 128 (linear midpoint between Y = -64 and Y = 320)
        DensityFunction mercuryBaseHeight = registerAndWrap(context, Mercury.BASE_HEIGHT,
                DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5)
        );

        // 3. Offset spline for macro regional elevation (evaluated continuously without flat_cache):
        // - Caloris Basin [-1.0 to -0.4]: depression down to Y = 70 to 85, surrounded by lobate scarp peaks (up to Y = 220)
        // - Smooth Lava Plains [-0.35 to 0.0]: level basalt floors at Y = 115
        // - Inter-Crater Plains [0.0 to 0.45]: base undulating plains around Y = 120
        // - Chaotic Terrain [0.45 to 1.0]: rugged fractured peaks (Y = 160 to 220)
        var mercuryOffsetSpline = CubicSpline.builder(mercuryContinentsCoord)
                .addPoint(-1.00F, -0.38F, 0.05F)
                .addPoint(-0.65F, -0.32F, 0.15F)
                .addPoint(-0.35F, -0.04F, 0.20F)
                .addPoint(-0.05F, 0.00F, 0.10F)
                .addPoint(0.25F, 0.03F, 0.18F)
                .addPoint(0.50F, 0.22F, 0.45F)
                .addPoint(0.80F, 0.55F, 0.50F)
                .addPoint(1.00F, 0.72F, 0.30F)
                .build();
        DensityFunction mercuryOffset = registerAndWrap(context, Mercury.OFFSET, DensityFunctions.spline(mercuryOffsetSpline));

        // 4. Factor spline for relief amplitude scaling
        var mercuryFactorSpline = CubicSpline.builder(mercuryContinentsCoord)
                .addPoint(-1.00F, 0.30F, 0.0F)
                .addPoint(-0.35F, 0.22F, 0.10F)
                .addPoint(0.00F, 0.35F, 0.20F)
                .addPoint(0.45F, 0.65F, 0.50F)
                .addPoint(0.80F, 1.15F, 0.60F)
                .addPoint(1.00F, 1.35F, 0.40F)
                .build();
        DensityFunction mercuryFactor = registerAndWrap(context, Mercury.FACTOR, DensityFunctions.spline(mercuryFactorSpline));

        // 5. Multi-generator surface relief using continuous coordinate displacement
        DensityFunction mercuryBasePlainsNoise = DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.80, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_BASE_NOISE));
        DensityFunction mercuryScarpNoise = DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.45, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_SCARP_NOISE)).abs();
        DensityFunction mercuryCalorisNoise = DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.55, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CALORIS_NOISE));
        DensityFunction mercuryChaoticNoise = DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 1.10, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CHAOTIC_NOISE));

        DensityFunction mercurySmoothRelief = registerAndWrap(context, Mercury.SMOOTH_RELIEF,
                DensityFunctions.mul(
                        mercuryFactor,
                        DensityFunctions.add(
                                DensityFunctions.mul(DensityFunctions.constant(0.18), mercuryBasePlainsNoise),
                                DensityFunctions.add(
                                         DensityFunctions.mul(DensityFunctions.constant(0.25), mercuryScarpNoise),
                                        DensityFunctions.add(
                                                DensityFunctions.mul(DensityFunctions.constant(0.12), mercuryCalorisNoise),
                                                DensityFunctions.mul(DensityFunctions.constant(0.15), mercuryChaoticNoise)
                                        )
                                )
                        )
                )
        );

        // 3D Organic Roughness: Active across all surface elevations (Y = 60 to 260)
        // High 3D amplitude (0.28) completely shatters flat cliff faces and rectilinear step artifacts into natural rugged terrain
        DensityFunction mercury3dRoughness = DensityFunctions.mul(
                DensityFunctions.yClampedGradient(60, 260, 1.0, 0.0),
                DensityFunctions.mul(
                        mercuryFactor,
                        DensityFunctions.mul(
                                DensityFunctions.constant(0.28),
                                BlendedNoise.createUnseeded(0.25, 0.25, 80.0, 160.0, 8.0)
                        )
                )
        );

        DensityFunction mercurySolidTerrain = DensityFunctions.add(
                mercuryBaseHeight,
                DensityFunctions.add(
                        mercuryOffset,
                        DensityFunctions.add(mercurySmoothRelief, mercury3dRoughness)
                )
        );

        // Bedrock shield: guarantees 100% solid floor at Y = -64 to -50, preventing any void leaks
        DensityFunction bedrockShield = DensityFunctions.yClampedGradient(-64, -50, 6.0, 0.0);

        // Final density: solid terrain with 3D organic roughness + impenetrable bedrock floor
        // Cavities and monumental chasms/tubes are generated via dedicated high-performance Carvers
        context.register(Mercury.FINAL_DENSITY, DensityFunctions.add(
                mercurySolidTerrain,
                bedrockShield
        ));
    }

    private static DensityFunction registerAndWrap(BootstrapContext<DensityFunction> context, ResourceKey<DensityFunction> key, DensityFunction densityFunction) {
        return new DensityFunctions.HolderHolder(context.register(key, densityFunction));
    }

    public static DensityFunction getFunction(HolderGetter<DensityFunction> densityFunctions, ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(key));
    }
}
