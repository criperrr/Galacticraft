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
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("moon/continents");
        public static final ResourceKey<DensityFunction> EROSION = createKey("moon/erosion");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("moon/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("moon/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("moon/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("moon/smooth_relief");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("moon/solid_terrain");
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
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("venus/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("venus/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("venus/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("venus/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("venus/smooth_relief");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("venus/solid_terrain");
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
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("mercury/solid_terrain");

        // Subterranean monumental cave systems
        public static final ResourceKey<DensityFunction> CRAWLWAYS = createKey("mercury/crawlways");
        public static final ResourceKey<DensityFunction> NOODLES = createKey("mercury/noodles");
        public static final ResourceKey<DensityFunction> LAVA_TUBES = createKey("mercury/lava_tubes");
        public static final ResourceKey<DensityFunction> MEGABRECCIA_DOMES = createKey("mercury/megabreccia_domes");
        public static final ResourceKey<DensityFunction> TECTONIC_CHASMS = createKey("mercury/tectonic_chasms");
        public static final ResourceKey<DensityFunction> MAGMA_DEPTHS = createKey("mercury/magma_depths");
        public static final ResourceKey<DensityFunction> HOLLOW_LABYRINTH = createKey("mercury/hollow_labyrinth");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("mercury/pillars");
        public static final ResourceKey<DensityFunction> CAVES = createKey("mercury/caves");

        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("mercury/final_density");
    }

    public static final class Jupiter {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("jupiter/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("jupiter/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("jupiter/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("jupiter/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("jupiter/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("jupiter/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("jupiter/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("jupiter/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("jupiter/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("jupiter/final_density");
    }

    public static final class Saturn {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("saturn/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("saturn/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("saturn/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("saturn/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("saturn/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("saturn/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("saturn/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("saturn/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("saturn/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("saturn/final_density");
    }

    public static final class Uranus {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("uranus/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("uranus/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("uranus/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("uranus/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("uranus/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("uranus/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("uranus/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("uranus/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("uranus/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("uranus/final_density");
    }

    public static final class Neptune {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("neptune/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("neptune/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("neptune/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("neptune/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("neptune/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("neptune/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("neptune/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("neptune/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("neptune/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("neptune/final_density");
    }

    public static final class Phobos {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("phobos/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("phobos/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("phobos/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("phobos/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("phobos/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("phobos/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("phobos/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("phobos/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("phobos/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("phobos/final_density");
    }

    public static final class Deimos {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("deimos/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("deimos/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("deimos/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("deimos/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("deimos/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("deimos/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("deimos/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("deimos/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("deimos/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("deimos/final_density");
    }

    public static final class Io {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("io/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("io/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("io/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("io/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("io/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("io/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("io/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("io/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("io/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("io/final_density");
    }

    public static final class Europa {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("europa/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("europa/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("europa/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("europa/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("europa/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("europa/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("europa/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("europa/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("europa/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("europa/final_density");
    }

    public static final class Ganymede {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("ganymede/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("ganymede/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("ganymede/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("ganymede/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("ganymede/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("ganymede/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("ganymede/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("ganymede/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("ganymede/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("ganymede/final_density");
    }

    public static final class Callisto {
        public static final ResourceKey<DensityFunction> CONTINENTS = createKey("callisto/continents");
        public static final ResourceKey<DensityFunction> BASE_HEIGHT = createKey("callisto/base_height");
        public static final ResourceKey<DensityFunction> OFFSET = createKey("callisto/offset");
        public static final ResourceKey<DensityFunction> FACTOR = createKey("callisto/factor");
        public static final ResourceKey<DensityFunction> SMOOTH_RELIEF = createKey("callisto/smooth_relief");
        public static final ResourceKey<DensityFunction> DEPTH = createKey("callisto/depth");
        public static final ResourceKey<DensityFunction> SOLID_TERRAIN = createKey("callisto/solid_terrain");
        public static final ResourceKey<DensityFunction> CAVES = createKey("callisto/caves");
        public static final ResourceKey<DensityFunction> PILLARS = createKey("callisto/pillars");
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("callisto/final_density");
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

        DensityFunction moonShiftX = DensityFunctions.cache2d(
                DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.25, 0.0)
        );
        DensityFunction moonShiftZ = DensityFunctions.cache2d(
                DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.0, 0.25)
        );
        DensityFunction moonContinents = registerAndWrap(context, Moon.CONTINENTS,
                DensityFunctions.cache2d(
                        DensityFunctions.shiftedNoise2d(moonShiftX, moonShiftZ, 0.25, noiseRegistry.getOrThrow(Noises.CONTINENTALNESS))
                )
        );
        var moonContinentsCoord = new DensityFunctions.Spline.Coordinate(
                context.lookup(Registries.DENSITY_FUNCTION).getOrThrow(Moon.CONTINENTS)
        );
        var vanillaContinentsCoord = new DensityFunctions.Spline.Coordinate(vanillaRegistry.getOrThrow(NoiseRouterData.CONTINENTS));
        var continentsCoord = vanillaContinentsCoord;

        DensityFunction erosion = registerAndWrap(context, Moon.EROSION, DensityFunctions.flatCache(
                DensityFunctions.shiftedNoise2d(
                        moonShiftX, moonShiftZ, 1.0, noiseRegistry.getOrThrow(GCNoiseData.EROSION)
                )
        ));

        // Moon vertical gradient anchor: monotonically decreases from +1.5 at Y=-32 to -1.5 at Y=224 (0.0 at Y=96, 1 block = 0.01171875).
        DensityFunction moonBaseHeight = registerAndWrap(context, Moon.BASE_HEIGHT,
                DensityFunctions.yClampedGradient(-32, 224, 1.5, -1.5)
        );

        // Smooth C1 cubic Hermite spline for Moon base elevation replicating GC4 ChunkProviderMoon (MID_HEIGHT = 63..76)
        // and smooth transitions between Lunar Maria, Lowlands, and Highlands:
        // - BASALTIC_MARE & OLIVINE_SPIKES [-1.0, -0.35]: low-lying basaltic maria basins (Y ~ 59..68)
        // - LUNAR_LOWLANDS & COMET_TUNDRA [-0.35, 0.25]: classic rolling lunar regolith plains (Y ~ 68..88)
        // - LUNAR_HIGHLANDS [0.25, 1.0]: elevated rugged cratered highlands & mountain massifs (Y ~ 88..137)
        var moonOffsetSpline = CubicSpline.builder(moonContinentsCoord)
                .addPoint(-1.00F, -0.43F, 0.05F)
                .addPoint(-0.55F, -0.39F, 0.14F)
                .addPoint(-0.35F, -0.33F, 0.28F)
                .addPoint(-0.05F, -0.22F, 0.25F)
                .addPoint(0.25F, -0.08F, 0.42F)
                .addPoint(0.50F, 0.12F, 0.80F)
                .addPoint(0.75F, 0.32F, 0.72F)
                .addPoint(1.00F, 0.48F, 0.50F)
                .build();
        DensityFunction moonOffset = registerAndWrap(context, Moon.OFFSET, DensityFunctions.cache2d(DensityFunctions.spline(moonOffsetSpline)));

        // Smooth C1 cubic Hermite spline for Moon relief amplitude scaling:
        // - Maria [-1.0, -0.35]: smooth undulations with crater basins (factor ~0.36..0.56)
        // - Lowlands [-0.35, 0.25]: rolling lunar plains and craters (factor ~0.56..1.05)
        // - Highlands [0.25, 1.0]: rugged crater rims, mountain peaks and ridges (factor ~1.05..2.05)
        var moonFactorSpline = CubicSpline.builder(moonContinentsCoord)
                .addPoint(-1.00F, 0.36F, 0.06F)
                .addPoint(-0.55F, 0.44F, 0.18F)
                .addPoint(-0.35F, 0.56F, 0.32F)
                .addPoint(-0.05F, 0.75F, 0.45F)
                .addPoint(0.25F, 1.05F, 0.85F)
                .addPoint(0.50F, 1.45F, 1.10F)
                .addPoint(0.75F, 1.75F, 0.85F)
                .addPoint(1.00F, 2.05F, 0.60F)
                .build();
        DensityFunction moonFactor = registerAndWrap(context, Moon.FACTOR, DensityFunctions.cache2d(DensityFunctions.spline(moonFactorSpline)));

        // GC4 ChunkProviderMoon multi-generator relief:
        // - noiseGen1 (MOON_LOWLAND_NOISE, frequency 0.0125, amplitude 12 blocks)
        // - noiseGen2 (MOON_HIGHLAND_NOISE, frequency 0.015, amplitude 32 blocks)
        // - highland ridge absolute relief & basalt mare undulation
        DensityFunction moonLowlandNoise = DensityFunctions.shiftedNoise2d(moonShiftX, moonShiftZ, 0.75, noiseRegistry.getOrThrow(GCNoiseData.MOON_LOWLAND_NOISE));
        DensityFunction moonHighlandNoise = DensityFunctions.shiftedNoise2d(moonShiftX, moonShiftZ, 0.90, noiseRegistry.getOrThrow(GCNoiseData.MOON_HIGHLAND_NOISE));
        DensityFunction moonHighlandRidges = DensityFunctions.shiftedNoise2d(moonShiftX, moonShiftZ, 0.55, noiseRegistry.getOrThrow(GCNoiseData.MOON_HIGHLAND_NOISE)).abs();
        DensityFunction moonMareNoise = DensityFunctions.shiftedNoise2d(moonShiftX, moonShiftZ, 0.60, noiseRegistry.getOrThrow(GCNoiseData.BASALT_MARE));

        DensityFunction moonSmoothRelief = registerAndWrap(context, Moon.SMOOTH_RELIEF, DensityFunctions.cache2d(
                DensityFunctions.mul(
                        moonFactor,
                        DensityFunctions.add(
                                DensityFunctions.mul(DensityFunctions.constant(0.15), moonLowlandNoise),
                                DensityFunctions.add(
                                        DensityFunctions.mul(DensityFunctions.constant(0.17), moonHighlandNoise),
                                        DensityFunctions.add(
                                                DensityFunctions.mul(DensityFunctions.constant(0.14), moonHighlandRidges),
                                                DensityFunctions.mul(DensityFunctions.constant(0.10), moonMareNoise)
                                        )
                                )
                        )
                )
        ));

        // 3D Organic Roughness: Active across lunar surface elevations for natural crater walls, scarps and rocky massifs
        DensityFunction moonRaw3dRoughness = DensityFunctions.mul(
                moonFactor,
                DensityFunctions.mul(
                        DensityFunctions.constant(0.12),
                        BlendedNoise.createUnseeded(0.25, 0.125, 80.0, 160.0, 8.0)
                )
        );

        DensityFunction moonRawSolidTerrain = DensityFunctions.add(
                moonBaseHeight,
                DensityFunctions.add(
                        moonOffset,
                        DensityFunctions.add(moonSmoothRelief, moonRaw3dRoughness)
                )
        );

        DensityFunction moonInterpolatedSolidTerrain = registerAndWrap(context, Moon.SOLID_TERRAIN, DensityFunctions.interpolated(moonRawSolidTerrain));

        context.register(Moon.FINAL_DENSITY, DensityFunctions.min(
                moonInterpolatedSolidTerrain,
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

        // Segregated 2D horizontal noise coordinates with cache2d to prevent multi-planet coordinate entanglement
        // and eliminate 90-degree orthogonal staircases on Venus mountain slopes (The Golden Triangle)
        DensityFunction venusShiftX = DensityFunctions.cache2d(
                DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.25, 0.0)
        );
        DensityFunction venusShiftZ = DensityFunctions.cache2d(
                DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.0, 0.25)
        );
        DensityFunction venusContinents = registerAndWrap(context, Venus.CONTINENTS,
                DensityFunctions.cache2d(
                        DensityFunctions.shiftedNoise2d(venusShiftX, venusShiftZ, 0.25, noiseRegistry.getOrThrow(Noises.CONTINENTALNESS))
                )
        );
        var venusContinentsCoord = new DensityFunctions.Spline.Coordinate(
                context.lookup(Registries.DENSITY_FUNCTION).getOrThrow(Venus.CONTINENTS)
        );

        // Smooth C1 cubic Hermite spline for base elevation (replicates GC4 ChunkProviderVenus 5x5 parabolicField kernel
        // across BiomeGenVenusValley [rootHeight=-0.4 -> Y~60], BiomeGenFlatVenus [rootHeight=0.5 -> Y~73],
        // and BiomeGenVenusMountain [rootHeight=2.0 -> Y~95..106]).
        // Wrapped with cache2d to eliminate 97% of redundant 3D Perlin evaluations per chunk.
        var offsetSpline = CubicSpline.builder(venusContinentsCoord)
                .addPoint(-1.00F, -0.42F, 0.02F)
                .addPoint(-0.35F, -0.39F, 0.12F)
                .addPoint(-0.15F, -0.32F, 0.22F)
                .addPoint(0.05F, -0.27F, 0.08F)
                .addPoint(0.25F, -0.24F, 0.18F)
                .addPoint(0.40F, -0.16F, 0.32F)
                .addPoint(0.70F, -0.02F, 0.35F)
                .addPoint(1.00F, 0.12F, 0.25F)
                .build();
        DensityFunction venusOffset = registerAndWrap(context, Venus.OFFSET, DensityFunctions.cache2d(DensityFunctions.spline(offsetSpline)));

        // Smooth C1 cubic Hermite spline for terrain heightVariation (replicates GC4 d9: Valley=0.2, Flat=0.4, Mountain=1.0..1.35)
        var factorSpline = CubicSpline.builder(venusContinentsCoord)
                .addPoint(-1.00F, 0.24F, 0.0F)
                .addPoint(-0.35F, 0.28F, 0.15F)
                .addPoint(-0.15F, 0.36F, 0.25F)
                .addPoint(0.05F, 0.44F, 0.15F)
                .addPoint(0.25F, 0.52F, 0.35F)
                .addPoint(0.40F, 0.72F, 0.75F)
                .addPoint(0.70F, 1.08F, 0.75F)
                .addPoint(1.00F, 1.35F, 0.45F)
                .build();
        DensityFunction venusFactor = registerAndWrap(context, Venus.FACTOR, DensityFunctions.cache2d(DensityFunctions.spline(factorSpline)));

        // GC4 ChunkProviderVenus noiseGenSmooth1 (4 octaves, persistence 0.25, freq 0.015, +/- 20 blocks) +
        // GalaxySpace WE_Biome volcanic terrain layer (5 octaves, persistence 0.6) + ridge detail on mountains.
        DensityFunction gc4SmoothNoise = DensityFunctions.shiftedNoise2d(venusShiftX, venusShiftZ, 0.96, noiseRegistry.getOrThrow(GCNoiseData.VENUS_SMOOTH_NOISE));
        DensityFunction gsVolcanicNoise = DensityFunctions.shiftedNoise2d(venusShiftX, venusShiftZ, 0.55, noiseRegistry.getOrThrow(GCNoiseData.VENUS_VOLCANIC_NOISE));
        DensityFunction ridgeNoise = DensityFunctions.shiftedNoise2d(venusShiftX, venusShiftZ, 0.40, noiseRegistry.getOrThrow(Noises.RIDGE));

        DensityFunction venusSmoothRelief = registerAndWrap(context, Venus.SMOOTH_RELIEF,
                DensityFunctions.cache2d(
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

        DensityFunction venusRawSolidTerrain = DensityFunctions.add(
                venusBaseHeight,
                DensityFunctions.add(
                        venusOffset,
                        DensityFunctions.add(venusSmoothRelief, subtle3dRoughness)
                )
        );

        // Trilinear cell interpolation (4x4x8 blocks): reduces 3D evaluations from 73,728 to ~825 per chunk
        DensityFunction venusSolidTerrain = DensityFunctions.interpolated(venusRawSolidTerrain);
        registerAndWrap(context, Venus.SOLID_TERRAIN, venusSolidTerrain);
        context.register(Venus.FINAL_DENSITY, venusSolidTerrain);

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

        // Linear Y coordinate gradient for instant O(1) rangeChoice culling of height-bounded subterranean features
        DensityFunction altitudeY = DensityFunctions.yClampedGradient(-64, 320, -64.0, 320.0);

        // 1. Coordinate displacement & continuous continentalness with cache2d (The Golden Triangle)
        DensityFunction mercuryShiftX = DensityFunctions.cache2d(DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.25, 0.0));
        DensityFunction mercuryShiftZ = DensityFunctions.cache2d(DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.0, 0.25));
        DensityFunction mercuryContinents = registerAndWrap(context, Mercury.CONTINENTS,
                DensityFunctions.cache2d(DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.25, noiseRegistry.getOrThrow(Noises.CONTINENTALNESS)))
        );
        var mercuryContinentsCoord = new DensityFunctions.Spline.Coordinate(
                context.lookup(Registries.DENSITY_FUNCTION).getOrThrow(Mercury.CONTINENTS)
        );

        // 2. Base surface elevation gradient: crosses 0 at Y = 128 (linear midpoint between Y = -64 and Y = 320)
        DensityFunction mercuryBaseHeight = registerAndWrap(context, Mercury.BASE_HEIGHT,
                DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5)
        );

        // 3. Offset spline for macro regional elevation:
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
        DensityFunction mercuryOffset = registerAndWrap(context, Mercury.OFFSET, DensityFunctions.cache2d(DensityFunctions.spline(mercuryOffsetSpline)));

        // 4. Factor spline for relief amplitude scaling
        var mercuryFactorSpline = CubicSpline.builder(mercuryContinentsCoord)
                .addPoint(-1.00F, 0.30F, 0.0F)
                .addPoint(-0.35F, 0.22F, 0.10F)
                .addPoint(0.00F, 0.35F, 0.20F)
                .addPoint(0.45F, 0.65F, 0.50F)
                .addPoint(0.80F, 1.15F, 0.60F)
                .addPoint(1.00F, 1.35F, 0.40F)
                .build();
        DensityFunction mercuryFactor = registerAndWrap(context, Mercury.FACTOR, DensityFunctions.cache2d(DensityFunctions.spline(mercuryFactorSpline)));

        // 5. Multi-generator surface relief using continuous coordinate displacement
        DensityFunction mercuryBasePlainsNoise = DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.80, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_BASE_NOISE));
        DensityFunction mercuryScarpNoise = DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.45, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_SCARP_NOISE)).abs();
        DensityFunction mercuryCalorisNoise = DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.55, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CALORIS_NOISE));
        DensityFunction mercuryChaoticNoise = DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 1.10, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CHAOTIC_NOISE));

        DensityFunction mercurySmoothRelief = registerAndWrap(context, Mercury.SMOOTH_RELIEF,
                DensityFunctions.cache2d(
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
                )
        );

        // 3D Organic Roughness: Active across surface elevations (Y = 60 to 240)
        // High 3D amplitude (0.28) completely shatters flat cliff faces into natural rugged terrain.
        // Range-choice pruned: outside [58, 250], BlendedNoise (32-40 octaves) is completely bypassed!
        DensityFunction roughnessYFade = DensityFunctions.mul(
                DensityFunctions.yClampedGradient(58, 70, 0.0, 1.0),
                DensityFunctions.yClampedGradient(220, 250, 1.0, 0.0)
        );
        DensityFunction raw3dRoughness = DensityFunctions.mul(
                roughnessYFade,
                DensityFunctions.mul(
                        mercuryFactor,
                        DensityFunctions.mul(
                                DensityFunctions.constant(0.28),
                                BlendedNoise.createUnseeded(0.25, 0.25, 80.0, 160.0, 8.0)
                        )
                )
        );
        DensityFunction mercury3dRoughness = DensityFunctions.rangeChoice(
                altitudeY,
                58.0,
                250.0,
                raw3dRoughness,
                DensityFunctions.zero()
        );

        DensityFunction mercuryRawSolidTerrain = DensityFunctions.add(
                mercuryBaseHeight,
                DensityFunctions.add(
                        mercuryOffset,
                        DensityFunctions.add(mercurySmoothRelief, mercury3dRoughness)
                )
        );

        // Trilinear cell interpolation (4x4x8 blocks): reduces 3D evaluations from 98,304 to ~1,225 per chunk
        DensityFunction mercurySolidTerrain = registerAndWrap(
                context,
                Mercury.SOLID_TERRAIN,
                DensityFunctions.interpolated(mercuryRawSolidTerrain)
        );

        // ==========================================
        // MONUMENTAL SUBTERRANEAN 3D CAVERNS (CONTINUOUS NOISE CAVES)
        // Overworld-style continuous 3D density carving (zero step-discontinuities),
        // regional geological domain selectors (probabilities preventing infinite chains),
        // smooth parabolic vertical falloffs (no flat horizontal shelves),
        // impenetrable bedrock protection, and surface plain preservation.
        // ==========================================

        // Bedrock shield: guarantees 100% solid floor at Y = -64 to -50, preventing any void leaks
        DensityFunction bedrockShield = DensityFunctions.yClampedGradient(-64, -50, 6.0, 0.0);

        // Regional geological selectors: partitions Mercury into subterranean provinces using continuous noise
        DensityFunction caveSelector = DensityFunctions.cache2d(DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.35, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CAVE_SELECTOR)));
        DensityFunction caveRarity = DensityFunctions.cache2d(DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.50, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CAVE_RARITY)));

        // (A) Tubos de Lava Monumentais (Mercury Colossal Lava Tubes) [Y = 30 to 80]
        // Continuous horizontal conduits with smooth elliptical/vaulted cross-section (~40-50 wide by ~20-25 tall)
        DensityFunction tubeYDist = DensityFunctions.add(
                DensityFunctions.yClampedGradient(30, 55, 1.2, 0.0),
                DensityFunctions.yClampedGradient(55, 80, 0.0, 1.2)
        );
        DensityFunction tube2d = DensityFunctions.cache2d(DensityFunctions.shiftedNoise2d(mercuryShiftX, mercuryShiftZ, 0.22, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_LAVA_TUBES)).abs());
        DensityFunction tubeSelectorDist = caveSelector.abs();
        DensityFunction tubeMetric = DensityFunctions.add(
                DensityFunctions.mul(DensityFunctions.constant(4.5), tube2d),
                DensityFunctions.add(
                        DensityFunctions.mul(DensityFunctions.constant(1.0), tubeYDist),
                        DensityFunctions.mul(DensityFunctions.constant(2.5), tubeSelectorDist)
                )
        );
        DensityFunction rawLavaTubes = DensityFunctions.min(
                DensityFunctions.zero(),
                DensityFunctions.mul(
                        DensityFunctions.constant(2.8),
                        DensityFunctions.add(tubeMetric, DensityFunctions.constant(-0.50))
                )
        );
        DensityFunction lavaTubes = registerAndWrap(context, Mercury.LAVA_TUBES,
                DensityFunctions.rangeChoice(
                        altitudeY,
                        28.0,
                        82.0,
                        rawLavaTubes,
                        DensityFunctions.zero()
                )
        );

        // (B) Salões Abobadados de Megabrecha (Megabreccia Impact Caverns) [Y = -25 to 45]
        // Majestic vaulted domed chambers centered at Y = 10, active in major impact basins (caveSelector < -0.25)
        DensityFunction domeYDist = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-25, 10, 1.4, 0.0),
                DensityFunctions.yClampedGradient(10, 45, 0.0, 1.4)
        );
        DensityFunction cheese3d = DensityFunctions.interpolated(DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_MEGABRECCIA_CHEESE)));
        DensityFunction domeMetric = DensityFunctions.add(
                cheese3d,
                DensityFunctions.add(
                        domeYDist,
                        DensityFunctions.mul(DensityFunctions.constant(2.0), DensityFunctions.add(caveSelector, DensityFunctions.constant(0.25)))
                )
        );
        DensityFunction rawMegabrecciaDomes = DensityFunctions.min(
                DensityFunctions.zero(),
                DensityFunctions.mul(
                        DensityFunctions.constant(2.4),
                        DensityFunctions.add(domeMetric, DensityFunctions.constant(0.15))
                )
        );
        DensityFunction megabrecciaDomes = registerAndWrap(context, Mercury.MEGABRECCIA_DOMES,
                DensityFunctions.rangeChoice(
                        altitudeY,
                        -26.0,
                        46.0,
                        rawMegabrecciaDomes,
                        DensityFunctions.zero()
                )
        );

        // (C) Fendas Tectônicas Abissais (Tectonic Chasm Caverns) [Y = -20 to 100]
        // Narrow vertical rifts 15-25 blocks wide with a sheer plunge, along tectonic fault lines (caveSelector > 0.35)
        DensityFunction chasmYDist = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-20, 5, 1.2, 0.0),
                DensityFunctions.yClampedGradient(75, 100, 0.0, 1.2)
        );
        DensityFunction chasm2d = DensityFunctions.cache2d(DensityFunctions.shiftedNoise2d(
                DensityFunctions.mul(mercuryShiftX, DensityFunctions.constant(2.5)),
                DensityFunctions.mul(mercuryShiftZ, DensityFunctions.constant(0.35)),
                0.25,
                noiseRegistry.getOrThrow(GCNoiseData.MERCURY_TECTONIC_FISSURE)
        ).abs());
        DensityFunction chasmMetric = DensityFunctions.add(
                DensityFunctions.mul(DensityFunctions.constant(8.0), chasm2d),
                DensityFunctions.add(
                        chasmYDist,
                        DensityFunctions.mul(DensityFunctions.constant(2.2), DensityFunctions.add(DensityFunctions.constant(0.30), DensityFunctions.mul(DensityFunctions.constant(-1.0), caveSelector)))
                )
        );
        DensityFunction rawTectonicChasms = DensityFunctions.min(
                DensityFunctions.zero(),
                DensityFunctions.mul(
                        DensityFunctions.constant(3.0),
                        DensityFunctions.add(chasmMetric, DensityFunctions.constant(-0.35))
                )
        );
        DensityFunction tectonicChasms = registerAndWrap(context, Mercury.TECTONIC_CHASMS,
                DensityFunctions.rangeChoice(
                        altitudeY,
                        -22.0,
                        102.0,
                        rawTectonicChasms,
                        DensityFunctions.zero()
                )
        );

        // (D) Abismo Magmático Profundo (Mercury Magma Depths) [Y = -54 to -14]
        // Basal volcanic complex: colossal chambers, magma cauldrons and vast subterranean lava halls
        // structured by massive basalt massifs, natural rock pillars, and organic vaulted 3D roof
        DensityFunction magmaRoofRoughness = DensityFunctions.interpolated(DensityFunctions.mul(
                DensityFunctions.constant(0.85),
                DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_MEGABRECCIA_CHEESE), 1.2, 1.2)
        ));
        DensityFunction magmaYDist = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-54, -40, 1.4, 0.0),
                DensityFunctions.add(
                        DensityFunctions.yClampedGradient(-28, -14, 0.0, 1.6),
                        magmaRoofRoughness // Smooth 3D dome roof variation: eliminates flat ceiling!
                )
        );
        // Low-frequency 3D chamber noise for colossal rooms (60-120 blocks wide)
        DensityFunction chamber3d = DensityFunctions.interpolated(DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_MAGMA_CHAMBERS), 0.65, 0.55));
        // Structural basalt spines and massifs within the chambers (high amplitude prevents empty hollow voids)
        DensityFunction basaltSpines = DensityFunctions.interpolated(DensityFunctions.mul(
                DensityFunctions.constant(0.85),
                DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_BASAL_PILLARS), 1.2, 0.8)
        ));
        DensityFunction magmaMetric = DensityFunctions.add(
                magmaYDist,
                DensityFunctions.add(
                        chamber3d,
                        DensityFunctions.add(
                                basaltSpines,
                                DensityFunctions.mul(DensityFunctions.constant(0.6), caveRarity)
                        )
                )
        );
        DensityFunction rawMagmaDepths = DensityFunctions.min(
                DensityFunctions.zero(),
                DensityFunctions.mul(
                        DensityFunctions.constant(2.8),
                        DensityFunctions.add(magmaMetric, DensityFunctions.constant(0.18))
                )
        );
        DensityFunction magmaDepths = registerAndWrap(context, Mercury.MAGMA_DEPTHS,
                DensityFunctions.rangeChoice(
                        altitudeY,
                        -55.0,
                        -13.0,
                        rawMagmaDepths,
                        DensityFunctions.zero()
                )
        );

        // (E) Labirinto Esponjoso de Voláteis (Volatile Hollow Labyrinth) [Y = 75 to 110]
        // Porous globular network in shallow crust under sublimation hollows (caveRarity > 0.25)
        DensityFunction spongeYDist = DensityFunctions.add(
                DensityFunctions.yClampedGradient(75, 92, 1.2, 0.0),
                DensityFunctions.yClampedGradient(92, 110, 0.0, 1.2)
        );
        DensityFunction spongeNoise = DensityFunctions.interpolated(DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_HOLLOW_SPONGE)));
        DensityFunction spongeMetric = DensityFunctions.add(
                spongeNoise,
                DensityFunctions.add(
                        spongeYDist,
                        DensityFunctions.mul(DensityFunctions.constant(2.0), DensityFunctions.add(DensityFunctions.constant(0.20), DensityFunctions.mul(DensityFunctions.constant(-1.0), caveRarity)))
                )
        );
        DensityFunction rawHollowLabyrinth = DensityFunctions.min(
                DensityFunctions.zero(),
                DensityFunctions.mul(
                        DensityFunctions.constant(2.2),
                        DensityFunctions.add(spongeMetric, DensityFunctions.constant(-0.15))
                )
        );
        DensityFunction hollowLabyrinth = registerAndWrap(context, Mercury.HOLLOW_LABYRINTH,
                DensityFunctions.rangeChoice(
                        altitudeY,
                        74.0,
                        112.0,
                        rawHollowLabyrinth,
                        DensityFunctions.zero()
                )
        );

        // (F) Cavernas Tubulares Conectoras Menores (Mercury Connecting Noodle Caves) [Y = -45 to 112]
        // Winding 3D tubular conduits modeled on vanilla Minecraft Overworld noodle caves,
        // weaving seamlessly throughout the subterranean crust to connect all monumental chambers.
        // Enlarged to diameter 5-8 blocks and made continuous to reliably link large caves together.
        DensityFunction noodleYDist = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-48, -36, 0.35, 0.0),
                DensityFunctions.yClampedGradient(96, 114, 0.0, 0.35)
        );
        DensityFunction noodleRidgeA = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_A), 2.0, 2.0).abs();
        DensityFunction noodleRidgeB = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_B), 2.0, 2.0).abs();
        DensityFunction noodleDist = DensityFunctions.interpolated(DensityFunctions.max(noodleRidgeA, noodleRidgeB));
        DensityFunction noodleThickness = DensityFunctions.add(
                DensityFunctions.constant(-0.145),
                DensityFunctions.mul(
                        DensityFunctions.constant(-0.035),
                        DensityFunctions.interpolated(DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_THICKNESS), 1.0, 1.0))
                )
        );
        DensityFunction noodleMetric = DensityFunctions.add(
                noodleThickness,
                DensityFunctions.add(
                        DensityFunctions.mul(DensityFunctions.constant(1.05), noodleDist),
                        noodleYDist
                )
        );
        DensityFunction rawMercuryNoodles = DensityFunctions.min(
                DensityFunctions.zero(),
                DensityFunctions.mul(
                        DensityFunctions.constant(16.0),
                        noodleMetric
                )
        );
        DensityFunction mercuryNoodles = registerAndWrap(context, Mercury.NOODLES,
                DensityFunctions.rangeChoice(
                        altitudeY,
                        -49.0,
                        115.0,
                        rawMercuryNoodles,
                        DensityFunctions.zero()
                )
        );

        // (G) Redes de Cavernas Horizontais Estreitas (Mercury Horizontal Crawlways) [Y = -40 to 114]
        // Tight 2-3 block diameter horizontal crawlways and tubes weaving through the rock strata,
        // compressed vertically to form continuous horizontal tunnel networks throughout the subterranean crust.
        DensityFunction crawlYDist = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-45, -32, 0.25, 0.0),
                DensityFunctions.yClampedGradient(104, 116, 0.0, 0.25)
        );
        DensityFunction crawlRidgeA = DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CRAWLWAY_A), 2.2, 7.5).abs();
        DensityFunction crawlRidgeB = DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CRAWLWAY_B), 2.2, 7.5).abs();
        DensityFunction crawlDist = DensityFunctions.interpolated(DensityFunctions.max(crawlRidgeA, crawlRidgeB));
        DensityFunction crawlMetric = DensityFunctions.add(
                crawlDist,
                DensityFunctions.add(
                        crawlYDist,
                        DensityFunctions.constant(-0.075)
                )
        );
        DensityFunction rawMercuryCrawlways = DensityFunctions.min(
                DensityFunctions.zero(),
                DensityFunctions.mul(
                        DensityFunctions.constant(18.0),
                        crawlMetric
                )
        );
        DensityFunction mercuryCrawlways = registerAndWrap(context, Mercury.CRAWLWAYS,
                DensityFunctions.rangeChoice(
                        altitudeY,
                        -46.0,
                        117.0,
                        rawMercuryCrawlways,
                        DensityFunctions.zero()
                )
        );

        // Combined cave carve offset: negative inside caves, 0.0 everywhere else in solid rock
        DensityFunction totalCaveCarve = registerAndWrap(context, Mercury.CAVES,
                DensityFunctions.min(
                        mercuryCrawlways,
                        DensityFunctions.min(
                                mercuryNoodles,
                                DensityFunctions.min(
                                        lavaTubes,
                                        DensityFunctions.min(
                                                megabrecciaDomes,
                                                DensityFunctions.min(
                                                         tectonicChasms,
                                                        DensityFunctions.min(magmaDepths, hollowLabyrinth)
                                                )
                                        )
                                )
                        )
                )
        );

        // Dynamic Crust Surface Ceiling Taper (from SKILL.md Section 3.1):
        // Automatically scales cave ceiling closed relative to local terrain elevation (mercurySolidTerrain)
        // preventing surface breaches even in deep impact craters (Caloris Basin) or valleys!
        DensityFunction surfaceCeilingTaper = DensityFunctions.cacheOnce(DensityFunctions.add(
                DensityFunctions.mul(DensityFunctions.constant(-25.0), mercurySolidTerrain),
                DensityFunctions.constant(3.5)
        ).clamp(0.0, 3.5));

        DensityFunction boundedCaveCarve = DensityFunctions.min(
                DensityFunctions.zero(),
                DensityFunctions.add(totalCaveCarve, surfaceCeilingTaper)
        );

        // Range Choice Optimization (Universal Rule from SKILL.md Section 2.3):
        // 1. Altitude Culling: Above Y = 117 (altitudeY >= 117.0) or below Y = -56, caves do not exist. Bypass all 7 cave networks!
        DensityFunction altitudeCaves = DensityFunctions.rangeChoice(
                altitudeY,
                -56.0,
                117.0,
                boundedCaveCarve,
                DensityFunctions.zero()
        );

        // 2. Open Sky Culling: In the open air far above terrain, do not evaluate cave density.
        DensityFunction activeCaves = DensityFunctions.rangeChoice(
                mercurySolidTerrain,
                -0.70,
                10.0,
                altitudeCaves,
                DensityFunctions.zero()
        );

        // (H) Pilares Naturais de Sustentação Geológica e Magmática (Mercury Geological & Basal Pillars)
        // 1. General subterranean cave pillars (Mojang 1.21 vanilla cave pillars math)
        // Vertically stretched fluted columns (XZ frequency 24.0, Y scale 0.3) spanning ceiling to floor
        DensityFunction pillarNoise = DensityFunctions.interpolated(DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.PILLAR), 24.0, 0.3));
        DensityFunction pillarRareness = DensityFunctions.interpolated(DensityFunctions.mappedNoise(noiseRegistry.getOrThrow(Noises.PILLAR_RARENESS), 0.0, -2.0));
        DensityFunction pillarThickness = DensityFunctions.interpolated(DensityFunctions.mappedNoise(noiseRegistry.getOrThrow(Noises.PILLAR_THICKNESS), 0.0, 1.1));
        DensityFunction rawCavePillars = DensityFunctions.mul(
                DensityFunctions.add(
                        DensityFunctions.mul(pillarNoise, DensityFunctions.constant(2.0)),
                        pillarRareness
                ),
                pillarThickness.cube()
        );

        // 2. Colossal Basalt Pillars in Magma Depths (Y = -54 to -14):
        // Massive monolithic volcanic basalt columns rising from bedrock through lava oceans into the vaulted ceiling
        DensityFunction magmaPillarNoise = DensityFunctions.interpolated(DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_BASAL_PILLARS), 16.0, 0.25));
        DensityFunction magmaPillarRareness = DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CAVE_RARITY), 1.5, 1.5);
        DensityFunction magmaPillarHourglass = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-54, -42, 0.15, 0.0),
                DensityFunctions.yClampedGradient(-26, -14, 0.0, 0.15)
        );
        DensityFunction rawMagmaPillars = DensityFunctions.add(
                DensityFunctions.mul(magmaPillarNoise, DensityFunctions.constant(2.2)),
                DensityFunctions.add(magmaPillarRareness, magmaPillarHourglass)
        );
        DensityFunction magmaPillarYRange = DensityFunctions.mul(
                DensityFunctions.yClampedGradient(-54, -48, 0.0, 1.0),
                DensityFunctions.yClampedGradient(-20, -12, 1.0, 0.0)
        );
        DensityFunction magmaPillars = DensityFunctions.rangeChoice(
                altitudeY,
                -55.0,
                -13.0,
                DensityFunctions.mul(rawMagmaPillars, magmaPillarYRange),
                DensityFunctions.zero()
        );

        // Subterranean height bounds: active from bedrock (Y = -54) up to Y = 68
        DensityFunction subterraneanYBounds = DensityFunctions.mul(
                DensityFunctions.yClampedGradient(-54, -46, 0.0, 1.0),
                DensityFunctions.yClampedGradient(55, 68, 1.0, 0.0)
        );
        // Safety taper: stops pillars from breaking the surface crust
        DensityFunction crustSafetyTaper = DensityFunctions.add(
                DensityFunctions.constant(1.0),
                DensityFunctions.mul(DensityFunctions.constant(-4.0), surfaceCeilingTaper)
        ).clamp(0.0, 1.0);

        DensityFunction boundedCavePillars = DensityFunctions.rangeChoice(
                altitudeY,
                -55.0,
                69.0,
                DensityFunctions.mul(
                        rawCavePillars,
                        DensityFunctions.mul(subterraneanYBounds, crustSafetyTaper)
                ),
                DensityFunctions.zero()
        );

        // Combined pillar field: general cave columns + massive basal magma columns
        DensityFunction combinedPillars = DensityFunctions.rangeChoice(
                altitudeY,
                -55.0,
                69.0,
                DensityFunctions.max(boundedCavePillars, magmaPillars),
                DensityFunctions.constant(-1000000.0)
        );

        // Range choice: if < 0.03 set to -1000000.0 (negative infinity), else retain positive pillar density
        DensityFunction clampedPillars = DensityFunctions.rangeChoice(
                combinedPillars,
                -1000000.0,
                0.03,
                DensityFunctions.constant(-1000000.0),
                combinedPillars
        );

        // 3. Pillar Cavity Culling: Pillars are only evaluated where a cave has actually carved open a void (activeCaves < -0.05).
        // In 90% of the world (solid rock or open sky), pillar noise evaluation is skipped entirely!
        DensityFunction activePillars = DensityFunctions.rangeChoice(
                activeCaves,
                -1000.0,
                -0.05,
                clampedPillars,
                DensityFunctions.constant(-1000000.0)
        );

        // Merge pillars with terrain and caves using MAX (Vanilla 1.21 pattern):
        // Inside cave (negative terrain) + pillar (positive) -> solid pillar!
        // Inside cave (negative terrain) + no pillar (-1000000.0) -> carved cave!
        // Ceiling/floor (positive terrain) + pillar -> smoothly welded into rock with ZERO gaps or disconnection!
        DensityFunction terrainWithCaves = DensityFunctions.add(mercurySolidTerrain, activeCaves);
        DensityFunction terrainWithPillars = registerAndWrap(context, Mercury.PILLARS,
                DensityFunctions.max(terrainWithCaves, activePillars)
        );

        // Final density: terrain with caves and pillars + impenetrable bedrock protection
        context.register(Mercury.FINAL_DENSITY, DensityFunctions.add(
                terrainWithPillars,
                bedrockShield
        ));

        // ==========================================
        // JUPITER, SATURN, URANUS & NEPTUNE
        // Realistic Gas & Ice Giants:
        //   Y = 260..320 : Exosphere (< 0.2 bar)
        //   Y = 180..260 : Upper Atmosphere / Aerostatic Zone (0.5 - 2.5 bar)
        //   Y =  40..180 : Dense Cloud Decks & Zonal Jet Streams (3 - 50 bar)
        //   Y = -64..-20 : Sparse Abyssal Hyperbaric Core (~15% fragmented islets/ridges, 80-200+ bar)
        // O(1) rangeChoice(altitudeY, -64, 0, coreDensity, constant(-1.0)) -> <10 ms/chunk!
        // ==========================================
        bootstrapOuterPlanet(
                context,
                noiseRegistry,
                altitudeY,
                Jupiter.CONTINENTS,
                Jupiter.BASE_HEIGHT,
                Jupiter.OFFSET,
                Jupiter.FACTOR,
                Jupiter.SMOOTH_RELIEF,
                Jupiter.DEPTH,
                Jupiter.SOLID_TERRAIN,
                Jupiter.CAVES,
                Jupiter.PILLARS,
                Jupiter.FINAL_DENSITY,
                0.35D
        );

        bootstrapOuterPlanet(
                context,
                noiseRegistry,
                altitudeY,
                Saturn.CONTINENTS,
                Saturn.BASE_HEIGHT,
                Saturn.OFFSET,
                Saturn.FACTOR,
                Saturn.SMOOTH_RELIEF,
                Saturn.DEPTH,
                Saturn.SOLID_TERRAIN,
                Saturn.CAVES,
                Saturn.PILLARS,
                Saturn.FINAL_DENSITY,
                0.36D
        );

        bootstrapOuterPlanet(
                context,
                noiseRegistry,
                altitudeY,
                Uranus.CONTINENTS,
                Uranus.BASE_HEIGHT,
                Uranus.OFFSET,
                Uranus.FACTOR,
                Uranus.SMOOTH_RELIEF,
                Uranus.DEPTH,
                Uranus.SOLID_TERRAIN,
                Uranus.CAVES,
                Uranus.PILLARS,
                Uranus.FINAL_DENSITY,
                0.34D
        );

        bootstrapOuterPlanet(
                context,
                noiseRegistry,
                altitudeY,
                Neptune.CONTINENTS,
                Neptune.BASE_HEIGHT,
                Neptune.OFFSET,
                Neptune.FACTOR,
                Neptune.SMOOTH_RELIEF,
                Neptune.DEPTH,
                Neptune.SOLID_TERRAIN,
                Neptune.CAVES,
                Neptune.PILLARS,
                Neptune.FINAL_DENSITY,
                0.34D
        );

        // ==========================================
        // INNER & JOVIAN MOONS (PHASE 3)
        // ==========================================
        bootstrapMoon(
                context, noiseRegistry, altitudeY,
                Phobos.CONTINENTS, Phobos.BASE_HEIGHT, Phobos.OFFSET, Phobos.FACTOR,
                Phobos.SMOOTH_RELIEF, Phobos.DEPTH, Phobos.SOLID_TERRAIN, Phobos.CAVES,
                Phobos.PILLARS, Phobos.FINAL_DENSITY,
                new MoonTerrainProfile(0.32D, -0.42F, -0.18F, 0.24F, 0.30F, 0.55F, 1.15F, 0.14D, false)
        );
        bootstrapMoon(
                context, noiseRegistry, altitudeY,
                Deimos.CONTINENTS, Deimos.BASE_HEIGHT, Deimos.OFFSET, Deimos.FACTOR,
                Deimos.SMOOTH_RELIEF, Deimos.DEPTH, Deimos.SOLID_TERRAIN, Deimos.CAVES,
                Deimos.PILLARS, Deimos.FINAL_DENSITY,
                new MoonTerrainProfile(0.28D, -0.34F, -0.20F, 0.08F, 0.20F, 0.35F, 0.70F, 0.08D, false)
        );
        bootstrapMoon(
                context, noiseRegistry, altitudeY,
                Io.CONTINENTS, Io.BASE_HEIGHT, Io.OFFSET, Io.FACTOR,
                Io.SMOOTH_RELIEF, Io.DEPTH, Io.SOLID_TERRAIN, Io.CAVES,
                Io.PILLARS, Io.FINAL_DENSITY,
                new MoonTerrainProfile(0.26D, -0.36F, -0.14F, 0.32F, 0.28F, 0.52F, 1.25F, 0.15D, false)
        );
        bootstrapMoon(
                context, noiseRegistry, altitudeY,
                Europa.CONTINENTS, Europa.BASE_HEIGHT, Europa.OFFSET, Europa.FACTOR,
                Europa.SMOOTH_RELIEF, Europa.DEPTH, Europa.SOLID_TERRAIN, Europa.CAVES,
                Europa.PILLARS, Europa.FINAL_DENSITY,
                new MoonTerrainProfile(0.24D, -0.32F, -0.22F, 0.06F, 0.18F, 0.32F, 0.75F, 0.10D, true)
        );
        bootstrapMoon(
                context, noiseRegistry, altitudeY,
                Ganymede.CONTINENTS, Ganymede.BASE_HEIGHT, Ganymede.OFFSET, Ganymede.FACTOR,
                Ganymede.SMOOTH_RELIEF, Ganymede.DEPTH, Ganymede.SOLID_TERRAIN, Ganymede.CAVES,
                Ganymede.PILLARS, Ganymede.FINAL_DENSITY,
                new MoonTerrainProfile(0.25D, -0.34F, -0.16F, 0.26F, 0.24F, 0.48F, 1.05F, 0.12D, false)
        );
        bootstrapMoon(
                context, noiseRegistry, altitudeY,
                Callisto.CONTINENTS, Callisto.BASE_HEIGHT, Callisto.OFFSET, Callisto.FACTOR,
                Callisto.SMOOTH_RELIEF, Callisto.DEPTH, Callisto.SOLID_TERRAIN, Callisto.CAVES,
                Callisto.PILLARS, Callisto.FINAL_DENSITY,
                new MoonTerrainProfile(0.27D, -0.38F, -0.18F, 0.34F, 0.28F, 0.54F, 1.20F, 0.14D, false)
        );
    }

    private record MoonTerrainProfile(
            double continentScale,
            float lowOffset,
            float midOffset,
            float highOffset,
            float lowFactor,
            float midFactor,
            float highFactor,
            double roughnessAmp,
            boolean subsurfaceOcean
    ) {
    }

    /**
     * Builds an optimized DensityFunction DAG for solid rocky/icy moons strictly adhering to
     * {@code minecraft-worldgen-and-caves/SKILL.md}:
     * <ul>
     *   <li>{@code cache2d} on all 2D layers ({@code shift}, {@code continents}, {@code offset}, {@code factor},
     *       {@code smooth_relief}, cave selectors).</li>
     *   <li>{@code interpolated} on 3D noises ({@code SOLID_TERRAIN}, caves, pillars) with NO outer
     *       {@code interpolated} wrapper on {@code FINAL_DENSITY}.</li>
     *   <li>{@code cacheOnce} on all reused heavy sub-expressions ({@code surfaceCeilingTaper},
     *       {@code activeCaves}, {@code clampedPillars}).</li>
     *   <li>{@code rangeChoice(altitudeY, ...)} pruning on {@code BlendedNoise} and every cave/pillar layer.</li>
     *   <li>{@code SOLID_TERRAIN} completely decoupled from caves for {@code initialDensityWithoutJaggedness}.</li>
     * </ul>
     */
    private static void bootstrapMoon(
            BootstrapContext<DensityFunction> context,
            HolderGetter<net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters> noiseRegistry,
            DensityFunction altitudeY,
            ResourceKey<DensityFunction> continentsKey,
            ResourceKey<DensityFunction> baseHeightKey,
            ResourceKey<DensityFunction> offsetKey,
            ResourceKey<DensityFunction> factorKey,
            ResourceKey<DensityFunction> smoothReliefKey,
            ResourceKey<DensityFunction> depthKey,
            ResourceKey<DensityFunction> solidTerrainKey,
            ResourceKey<DensityFunction> cavesKey,
            ResourceKey<DensityFunction> pillarsKey,
            ResourceKey<DensityFunction> finalDensityKey,
            MoonTerrainProfile profile
    ) {
        // 1. 2D Caching for shift & continents
        DensityFunction shiftX = DensityFunctions.cache2d(DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.25, 0.0));
        DensityFunction shiftZ = DensityFunctions.cache2d(DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.0, 0.25));
        DensityFunction continents = registerAndWrap(context, continentsKey,
                DensityFunctions.cache2d(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, profile.continentScale(), noiseRegistry.getOrThrow(Noises.CONTINENTALNESS)))
        );
        var continentsCoord = new DensityFunctions.Spline.Coordinate(context.lookup(Registries.DENSITY_FUNCTION).getOrThrow(continentsKey));

        // 2. Vertical base height anchor: 0.0 at Y = 96 (1 block = 0.0125)
        DensityFunction baseHeight = registerAndWrap(context, baseHeightKey,
                DensityFunctions.yClampedGradient(-64, 256, 2.0, -2.0)
        );
        DensityFunction bedrockShield = DensityFunctions.yClampedGradient(-64, -50, 6.0, 0.0);

        // 3. Smooth C1 Hermite offset spline for basins, plains, and highlands
        var offsetSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, profile.lowOffset(), 0.05F)
                .addPoint(-0.25F, (profile.lowOffset() + profile.midOffset()) * 0.5F, 0.20F)
                .addPoint(0.25F, profile.midOffset(), 0.28F)
                .addPoint(0.70F, (profile.midOffset() + profile.highOffset()) * 0.55F, 0.45F)
                .addPoint(1.00F, profile.highOffset(), 0.30F)
                .build();
        DensityFunction offset = registerAndWrap(context, offsetKey, DensityFunctions.cache2d(DensityFunctions.spline(offsetSpline)));

        // 4. Surface-anchored DEPTH function (0.0 at surface, >0.18 subterranean)
        registerAndWrap(context, depthKey, DensityFunctions.add(baseHeight, offset));

        // 5. Smooth factor spline for regional relief scaling
        var factorSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, profile.lowFactor(), 0.05F)
                .addPoint(0.00F, profile.midFactor(), 0.20F)
                .addPoint(1.00F, profile.highFactor(), 0.30F)
                .build();
        DensityFunction factor = registerAndWrap(context, factorKey, DensityFunctions.cache2d(DensityFunctions.spline(factorSpline)));

        // 6. 2D multi-generator relief wrapped in cache2d
        DensityFunction baseNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.65, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_BASE_NOISE));
        DensityFunction ridgeNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.45, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_SCARP_NOISE)).abs();
        DensityFunction smoothRelief = registerAndWrap(context, smoothReliefKey,
                DensityFunctions.cache2d(
                        DensityFunctions.mul(
                                factor,
                                DensityFunctions.add(
                                        DensityFunctions.mul(DensityFunctions.constant(0.15), baseNoise),
                                        DensityFunctions.mul(DensityFunctions.constant(0.14), ridgeNoise)
                                )
                        )
                )
        );

        // 7. 3D roughness strictly pruned to surface elevation envelope via rangeChoice
        DensityFunction roughnessYFade = DensityFunctions.mul(
                DensityFunctions.yClampedGradient(40, 56, 0.0, 1.0),
                DensityFunctions.yClampedGradient(160, 184, 1.0, 0.0)
        );
        DensityFunction raw3dRoughness = DensityFunctions.mul(
                roughnessYFade,
                DensityFunctions.mul(
                        factor,
                        DensityFunctions.mul(
                                DensityFunctions.constant(profile.roughnessAmp()),
                                BlendedNoise.createUnseeded(0.24, 0.16, 80.0, 160.0, 8.0)
                        )
                )
        );
        DensityFunction pruned3dRoughness = DensityFunctions.rangeChoice(altitudeY, 40.0, 184.0, raw3dRoughness, DensityFunctions.zero());

        DensityFunction rawSolidTerrain = DensityFunctions.add(
                baseHeight,
                DensityFunctions.add(
                        offset,
                        DensityFunctions.add(smoothRelief, pruned3dRoughness)
                )
        );

        // 8. Interpolated SOLID_TERRAIN (decoupled from caves for initialDensityWithoutJaggedness)
        DensityFunction solidTerrain = registerAndWrap(context, solidTerrainKey, DensityFunctions.interpolated(rawSolidTerrain));

        // 9. Subterranean caves (or Europa's global subsurface ocean cavern below Y = 24)
        DensityFunction totalCaveCarve;
        if (profile.subsurfaceOcean()) {
            // Global sub-glacial ocean cavern from Y = -44 to Y = 23 beneath Europa's solid ice crust
            DensityFunction oceanYDist = DensityFunctions.add(
                    DensityFunctions.yClampedGradient(-46, -34, 1.5, 0.0),
                    DensityFunctions.yClampedGradient(14, 24, 0.0, 1.5)
            );
            DensityFunction oceanUndulation = DensityFunctions.interpolated(
                    DensityFunctions.mul(
                            DensityFunctions.constant(0.35),
                            DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_MEGABRECCIA_CHEESE), 0.5, 0.4)
                    )
            );
            DensityFunction rawOceanCavern = DensityFunctions.min(
                    DensityFunctions.zero(),
                    DensityFunctions.mul(
                            DensityFunctions.constant(2.6),
                            DensityFunctions.add(DensityFunctions.add(oceanYDist, oceanUndulation), DensityFunctions.constant(-0.85))
                    )
            );
            DensityFunction prunedOcean = DensityFunctions.rangeChoice(altitudeY, -46.0, 24.0, rawOceanCavern, DensityFunctions.zero());

            // Upper ice fractures connecting crust to ocean ceiling
            DensityFunction iceRidgeA = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_A), 1.8, 1.8).abs();
            DensityFunction iceRidgeB = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_B), 1.8, 1.8).abs();
            DensityFunction iceNoodleDist = DensityFunctions.interpolated(DensityFunctions.max(iceRidgeA, iceRidgeB));
            DensityFunction iceNoodleYDist = DensityFunctions.add(
                    DensityFunctions.yClampedGradient(16, 26, 0.25, 0.0),
                    DensityFunctions.yClampedGradient(48, 60, 0.0, 0.45)
            );
            DensityFunction rawIceNoodles = DensityFunctions.min(
                    DensityFunctions.zero(),
                    DensityFunctions.mul(
                            DensityFunctions.constant(14.0),
                            DensityFunctions.add(DensityFunctions.constant(-0.12), DensityFunctions.add(iceNoodleDist, iceNoodleYDist))
                    )
            );
            DensityFunction prunedIceNoodles = DensityFunctions.rangeChoice(altitudeY, 16.0, 60.0, rawIceNoodles, DensityFunctions.zero());

            totalCaveCarve = registerAndWrap(context, cavesKey, DensityFunctions.min(prunedOcean, prunedIceNoodles));
        } else {
            DensityFunction caveSelector = DensityFunctions.cache2d(
                    DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.30, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_CAVE_SELECTOR))
            );

            // (A) Horizontal conduits / lava or fracture tubes [Y = 12 to 58]
            DensityFunction tubeYDist = DensityFunctions.add(
                    DensityFunctions.yClampedGradient(12, 28, 1.2, 0.0),
                    DensityFunctions.yClampedGradient(42, 58, 0.0, 1.2)
            );
            DensityFunction tube2d = DensityFunctions.cache2d(
                    DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.20, noiseRegistry.getOrThrow(GCNoiseData.MERCURY_LAVA_TUBES)).abs()
            );
            DensityFunction rawTubes = DensityFunctions.min(
                    DensityFunctions.zero(),
                    DensityFunctions.mul(
                            DensityFunctions.constant(2.6),
                            DensityFunctions.add(
                                    DensityFunctions.add(
                                            DensityFunctions.mul(DensityFunctions.constant(4.4), tube2d),
                                            DensityFunctions.add(tubeYDist, DensityFunctions.mul(DensityFunctions.constant(2.0), caveSelector.abs()))
                                    ),
                                    DensityFunctions.constant(-0.48)
                            )
                    )
            );
            DensityFunction prunedTubes = DensityFunctions.rangeChoice(altitudeY, 10.0, 60.0, rawTubes, DensityFunctions.zero());

            // (B) Deep vaulted caverns [Y = -46 to 24]
            DensityFunction vaultYDist = DensityFunctions.add(
                    DensityFunctions.yClampedGradient(-46, -24, 1.35, 0.0),
                    DensityFunctions.yClampedGradient(4, 24, 0.0, 1.35)
            );
            DensityFunction cheese3d = DensityFunctions.interpolated(
                    DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_MEGABRECCIA_CHEESE), 0.75, 0.65)
            );
            DensityFunction rawVaults = DensityFunctions.min(
                    DensityFunctions.zero(),
                    DensityFunctions.mul(
                            DensityFunctions.constant(2.4),
                            DensityFunctions.add(
                                    DensityFunctions.add(
                                            cheese3d,
                                            DensityFunctions.add(vaultYDist, DensityFunctions.mul(DensityFunctions.constant(1.6), DensityFunctions.add(caveSelector, DensityFunctions.constant(0.25))))
                                    ),
                                    DensityFunctions.constant(0.18)
                            )
                    )
            );
            DensityFunction prunedVaults = DensityFunctions.rangeChoice(altitudeY, -48.0, 26.0, rawVaults, DensityFunctions.zero());

            // (C) Connecting 3D noodle passages [Y = -46 to 56]
            DensityFunction noodleYDist = DensityFunctions.add(
                    DensityFunctions.yClampedGradient(-46, -34, 0.35, 0.0),
                    DensityFunctions.yClampedGradient(42, 56, 0.0, 0.35)
            );
            DensityFunction ridgeA = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_A), 1.85, 1.85).abs();
            DensityFunction ridgeB = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_B), 1.85, 1.85).abs();
            DensityFunction noodleDist = DensityFunctions.interpolated(DensityFunctions.max(ridgeA, ridgeB));
            DensityFunction rawNoodles = DensityFunctions.min(
                    DensityFunctions.zero(),
                    DensityFunctions.mul(
                            DensityFunctions.constant(15.0),
                            DensityFunctions.add(DensityFunctions.constant(-0.135), DensityFunctions.add(noodleDist, noodleYDist))
                    )
            );
            DensityFunction prunedNoodles = DensityFunctions.rangeChoice(altitudeY, -48.0, 58.0, rawNoodles, DensityFunctions.zero());

            totalCaveCarve = registerAndWrap(context, cavesKey, DensityFunctions.min(prunedNoodles, DensityFunctions.min(prunedTubes, prunedVaults)));
        }

        // 10. Dynamic surface ceiling taper + cacheOnce on reused sub-expressions
        DensityFunction surfaceCeilingTaper = DensityFunctions.cacheOnce(DensityFunctions.add(
                DensityFunctions.mul(DensityFunctions.constant(-24.0), solidTerrain),
                DensityFunctions.constant(3.2)
        ).clamp(0.0, 3.2));

        DensityFunction boundedCaveCarve = DensityFunctions.min(
                DensityFunctions.zero(),
                DensityFunctions.add(totalCaveCarve, surfaceCeilingTaper)
        );
        DensityFunction altitudeCaves = DensityFunctions.rangeChoice(altitudeY, -50.0, 62.0, boundedCaveCarve, DensityFunctions.zero());
        DensityFunction activeCaves = DensityFunctions.cacheOnce(
                DensityFunctions.rangeChoice(solidTerrain, -0.60, 10.0, altitudeCaves, DensityFunctions.zero())
        );

        // 11. Subterranean pillars with double pruning (altitude + activeCaves < -0.05) and cacheOnce
        DensityFunction pillarNoise = DensityFunctions.interpolated(DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.PILLAR), 18.0, 0.25));
        DensityFunction pillarRareness = DensityFunctions.interpolated(DensityFunctions.mappedNoise(noiseRegistry.getOrThrow(Noises.PILLAR_RARENESS), 0.0, -2.0));
        DensityFunction pillarThickness = DensityFunctions.interpolated(DensityFunctions.mappedNoise(noiseRegistry.getOrThrow(Noises.PILLAR_THICKNESS), 0.0, 1.1));
        DensityFunction rawPillars = DensityFunctions.mul(
                DensityFunctions.add(DensityFunctions.mul(pillarNoise, DensityFunctions.constant(2.0)), pillarRareness),
                pillarThickness.cube()
        );
        DensityFunction pillarYBounds = DensityFunctions.mul(
                DensityFunctions.yClampedGradient(-48, -38, 0.0, 1.0),
                DensityFunctions.yClampedGradient(20, 34, 1.0, 0.0)
        );
        DensityFunction crustSafety = DensityFunctions.add(
                DensityFunctions.constant(1.0),
                DensityFunctions.mul(DensityFunctions.constant(-4.0), surfaceCeilingTaper)
        ).clamp(0.0, 1.0);
        DensityFunction boundedPillars = DensityFunctions.cacheOnce(
                DensityFunctions.rangeChoice(
                        altitudeY,
                        -48.0,
                        36.0,
                        DensityFunctions.mul(rawPillars, DensityFunctions.mul(pillarYBounds, crustSafety)),
                        DensityFunctions.constant(-1000000.0)
                )
        );
        DensityFunction clampedPillars = DensityFunctions.rangeChoice(
                boundedPillars,
                -1000000.0,
                0.03,
                DensityFunctions.constant(-1000000.0),
                boundedPillars
        );
        DensityFunction activePillars = DensityFunctions.rangeChoice(
                activeCaves,
                -1000.0,
                -0.05,
                clampedPillars,
                DensityFunctions.constant(-1000000.0)
        );

        DensityFunction terrainWithCaves = DensityFunctions.add(solidTerrain, activeCaves);
        DensityFunction terrainWithPillars = registerAndWrap(context, pillarsKey, DensityFunctions.max(terrainWithCaves, activePillars));

        // 12. Final Density: NO outer interpolated wrapper!
        context.register(finalDensityKey, DensityFunctions.add(terrainWithPillars, bedrockShield));
    }

    /**
     * Builds an ultra-fast (<10 ms/chunk) DensityFunction DAG for realistic Gas &amp; Ice Giants:
     * <ul>
     *   <li>Above {@code Y = 0}: pruned in O(1) via {@code rangeChoice(altitudeY, -64, 0, coreDensity, constant(-1.0))}
     *       leaving open sky/cloud atmosphere from {@code Y = 0} to {@code Y = 320}.</li>
     *   <li>Below {@code Y = 0} ({@code Y = -64..-20}): sparse (~15% areal coverage) fragmented hyperbaric core
     *       islets and ridges near bedrock using {@code cache2d} 2D masks and {@code interpolated} 3D roughness.</li>
     *   <li>No outer {@code interpolated(...)} wrapper on {@code FINAL_DENSITY} and pure composite root nodes
     *       for DataGen codec compliance.</li>
     * </ul>
     */
    private static void bootstrapOuterPlanet(
            BootstrapContext<DensityFunction> context,
            HolderGetter<net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters> noiseRegistry,
            DensityFunction altitudeY,
            ResourceKey<DensityFunction> continentsKey,
            ResourceKey<DensityFunction> baseHeightKey,
            ResourceKey<DensityFunction> offsetKey,
            ResourceKey<DensityFunction> factorKey,
            ResourceKey<DensityFunction> smoothReliefKey,
            ResourceKey<DensityFunction> depthKey,
            ResourceKey<DensityFunction> solidTerrainKey,
            ResourceKey<DensityFunction> cavesKey,
            ResourceKey<DensityFunction> pillarsKey,
            ResourceKey<DensityFunction> finalDensityKey,
            double islandThreshold
    ) {
        // 1. 2D Caching (shiftX, shiftZ, continents) for Multi-Noise atmospheric belt biomes
        DensityFunction planetShiftX = DensityFunctions.cache2d(DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.25, 0.0));
        DensityFunction planetShiftZ = DensityFunctions.cache2d(DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.0, 0.25));
        DensityFunction planetContinents = registerAndWrap(context, continentsKey,
                DensityFunctions.cache2d(DensityFunctions.shiftedNoise2d(planetShiftX, planetShiftZ, 0.18, noiseRegistry.getOrThrow(Noises.CONTINENTALNESS_LARGE)))
        );

        // 2. Continuous deep hyperbaric mantle floor (Y = -64..-46 always solid so there is NEVER empty void!)
        //    tapering smoothly up to ridges/crags between Y = -44 and Y = -14.
        DensityFunction planetBaseHeight = registerAndWrap(context, baseHeightKey,
                DensityFunctions.yClampedGradient(-64, -14, 1.35, -1.75)
        );

        // 3. Undulating abyssal ridges & pressurized mantle basins (cache2d, medium ~75-block wavelength)
        DensityFunction rawIslandField = DensityFunctions.shiftedNoise2d(
                planetShiftX,
                planetShiftZ,
                0.85,
                noiseRegistry.getOrThrow(GCNoiseData.MERCURY_BASE_NOISE)
        );
        DensityFunction planetOffset = registerAndWrap(context, offsetKey,
                DensityFunctions.cache2d(
                        DensityFunctions.mul(
                                DensityFunctions.constant(1.35),
                                DensityFunctions.add(rawIslandField, DensityFunctions.constant(-islandThreshold * 0.55D))
                        )
                )
        );

        // 4. Atmospheric altitude DEPTH function for 3D Multi-Noise biome bands:
        // Y >= 186 -> depth <= 0.18 (upper aerostatic atmosphere),
        // Y = 21..186 -> depth in (0.18, 1.05) (mid cloud decks & storm bands),
        // Y <= 20 -> depth >= 1.05 (deep hyperbaric abyssal core).
        registerAndWrap(context, depthKey,
                DensityFunctions.yClampedGradient(220, -64, 0.0, 1.5)
        );

        // 5. Factor & craggy 2D ridge relief for abyssal core formations
        DensityFunction planetFactor = registerAndWrap(context, factorKey,
                DensityFunctions.cache2d(
                        DensityFunctions.add(
                                DensityFunctions.constant(0.35),
                                DensityFunctions.mul(DensityFunctions.constant(0.20), planetContinents)
                        )
                )
        );

        DensityFunction cragNoise = DensityFunctions.shiftedNoise2d(
                planetShiftX,
                planetShiftZ,
                1.35,
                noiseRegistry.getOrThrow(GCNoiseData.MERCURY_SCARP_NOISE)
        ).abs();
        DensityFunction planetSmoothRelief = registerAndWrap(context, smoothReliefKey,
                DensityFunctions.cache2d(
                        DensityFunctions.mul(
                                planetFactor,
                                DensityFunctions.mul(DensityFunctions.constant(0.22), cragNoise)
                        )
                )
        );

        // 6. Lightweight interpolated 3D overhang roughness + impenetrable deep mantle floor shield (Y = -64..-50)
        //    so the abyssal core is a continuous undulating hyperbaric floor with ZERO empty void holes.
        DensityFunction core3dRoughness = DensityFunctions.interpolated(
                DensityFunctions.mul(
                        DensityFunctions.constant(0.20),
                        DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.MERCURY_HOLLOW_SPONGE), 1.2, 1.4)
                )
        );
        DensityFunction mantleFloorShield = DensityFunctions.yClampedGradient(-64, -50, 3.0, 0.0);

        DensityFunction coreDensity = DensityFunctions.add(
                mantleFloorShield,
                DensityFunctions.add(
                        planetBaseHeight,
                        DensityFunctions.add(
                                planetOffset,
                                DensityFunctions.add(planetSmoothRelief, core3dRoughness)
                        )
                )
        );

        // 7. O(1) vertical pruning: everything at Y >= 0 is guaranteed open atmosphere (-1.0) with ZERO noise evaluations!
        DensityFunction planetSolidTerrain = registerAndWrap(
                context,
                solidTerrainKey,
                DensityFunctions.rangeChoice(
                        altitudeY,
                        -64.0,
                        0.0,
                        coreDensity,
                        DensityFunctions.constant(-1.0)
                )
        );

        // 8. Caves & Pillars disabled for sparse abyssal core (registered as pure composite nodes for registry completeness)
        registerAndWrap(context, cavesKey, DensityFunctions.add(DensityFunctions.zero(), DensityFunctions.zero()));
        registerAndWrap(context, pillarsKey, DensityFunctions.add(DensityFunctions.constant(-1000000.0), DensityFunctions.zero()));

        // 9. Final Density: O(1) rangeChoice(altitudeY, -64, 0, coreDensity, -1.0), NO outer interpolated wrapper!
        context.register(finalDensityKey, DensityFunctions.rangeChoice(
                altitudeY,
                -64.0,
                0.0,
                DensityFunctions.add(planetSolidTerrain, DensityFunctions.zero()),
                DensityFunctions.constant(-1.0)
        ));
    }

    private static DensityFunction registerAndWrap(BootstrapContext<DensityFunction> context, ResourceKey<DensityFunction> key, DensityFunction densityFunction) {
        return new DensityFunctions.HolderHolder(context.register(key, densityFunction));
    }

    public static DensityFunction getFunction(HolderGetter<DensityFunction> densityFunctions, ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(key));
    }
}
