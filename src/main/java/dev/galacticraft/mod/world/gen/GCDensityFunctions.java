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
        public static final ResourceKey<DensityFunction> FINAL_DENSITY = createKey("moon/final_density");
    }

    public static final class Mars {
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

    private static ResourceKey<DensityFunction> createKey(String id) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, Constant.id(id));
    }

    public static void bootstrapRegistries(BootstrapContext<DensityFunction> context) {
        var vanillaRegistry = context.lookup(Registries.DENSITY_FUNCTION);
        var noiseRegistry = context.lookup(Registries.NOISE);
        DensityFunction shiftX = getFunction(vanillaRegistry, NoiseRouterData.SHIFT_X);
        DensityFunction shiftZ = getFunction(vanillaRegistry, NoiseRouterData.SHIFT_Z);
        DensityFunction y = getFunction(vanillaRegistry, NoiseRouterData.Y);

//        DensityFunction noodles = registerAndWrap(context, NOODLES, DensityFunctions.rangeChoice(
//                DensityFunctions.interpolated(
//                        DensityFunctions.rangeChoice(
//                                y, -25, 45,
//                                DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE), 1, 1),
//                                DensityFunctions.constant(-1)
//                        )
//                ),
//                -1000000, 0, DensityFunctions.constant(64),
//                DensityFunctions.add(
//                        DensityFunctions.interpolated(
//                                DensityFunctions.rangeChoice(
//                                        y, -25, 45,
//                                        DensityFunctions.add(
//                                                DensityFunctions.constant(-0.07500000000000001),
//                                                DensityFunctions.mul(
//                                                        DensityFunctions.constant(-0.025),
//                                                        DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_THICKNESS), 1, 1)
//                                                )
//                                        ),
//                                        DensityFunctions.constant(0)
//                                )
//                        ),
//                        DensityFunctions.mul(
//                                DensityFunctions.constant(1.5),
//                                DensityFunctions.max(
//                                        DensityFunctions.interpolated(
//                                                DensityFunctions.rangeChoice(
//                                                        y, -25, 45,
//                                                        DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665, 2.6666666666666665),
//                                                        DensityFunctions.zero()
//                                                )
//                                        ).abs(),
//                                        DensityFunctions.interpolated(
//                                                DensityFunctions.rangeChoice(
//                                                        y, -25, 45,
//                                                        DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665, 2.6666666666666665),
//                                                        DensityFunctions.zero()
//                                                    )
//                                        ).abs()
//                                )
//                        )
//                )
//        ));
//        DensityFunction erosion = registerAndWrap(context, Moon.EROSION, DensityFunctions.flatCache(
//              DensityFunctions.shiftedNoise2d(
//                      shiftX, shiftZ, 1.0, noiseRegistry.getOrThrow(GCNoiseData.EROSION)
//              )
//        ));
//        context.register(Moon.FINAL_DENSITY, DensityFunctions.min(
//                DensityFunctions.add(
//                        DensityFunctions.interpolated(
//                                DensityFunctions.blendDensity(
//                                        DensityFunctions.rangeChoice(
//                                                erosion, 0.05, 2.000000000000001,
//                                                DensityFunctions.yClampedGradient(-64, 190, 1, -1),
//                                                DensityFunctions.yClampedGradient(-64, 200, 1, -1)
//                                        )
//                                )
//                        ),
//                        DensityFunctions.add(
//                                DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.BASALT_MARE), 0.00005, 0.0007),
//                                DensityFunctions.noise(noiseRegistry.getOrThrow(GCNoiseData.BASALT_MARE_HEIGHT), 0, 0)
//                        )
//                ),
//                noodles
//            )
//        );

        context.register(Mars.FINAL_DENSITY, DensityFunctions.add(
                DensityFunctions.yClampedGradient(0, 96, 1, -1),
                BlendedNoise.createUnseeded(0.25, 0.375, 80.0, 160.0, 8.0)
        ));

        DensityFunction continents = getFunction(vanillaRegistry, NoiseRouterData.CONTINENTS);
        var continentsCoord = new DensityFunctions.Spline.Coordinate(vanillaRegistry.getOrThrow(NoiseRouterData.CONTINENTS));

        // Vertical gradient anchor: monotonically decreases from +1.5 at Y=-32 to -1.5 at Y=224 (0.0 at Y=96, 1 block = 0.01171875).
        DensityFunction venusBaseHeight = registerAndWrap(context, Venus.BASE_HEIGHT,
                DensityFunctions.yClampedGradient(-32, 224, 1.5, -1.5)
        );

        // Smooth C1 cubic Hermite spline for base elevation (replicates GC4 ChunkProviderVenus 5x5 parabolicField kernel
        // across BiomeGenVenusValley [baseHeight=-0.4 -> Y~57], BiomeGenFlatVenus [baseHeight=0.5 -> Y~73],
        // and BiomeGenVenusMountain + GalaxySpace Io_Mountains [baseHeight=2.0..2.8 -> Y~98..122]).
        var offsetSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, -0.45F, 0.02F)
                .addPoint(-0.35F, -0.43F, 0.15F)
                .addPoint(-0.15F, -0.34F, 0.40F)
                .addPoint(0.05F, -0.27F, 0.08F)
                .addPoint(0.25F, -0.25F, 0.25F)
                .addPoint(0.35F, -0.18F, 0.65F)
                .addPoint(0.65F, 0.03F, 0.65F)
                .addPoint(1.00F, 0.25F, 0.45F)
                .build();
        DensityFunction venusOffset = registerAndWrap(context, Venus.OFFSET, DensityFunctions.spline(offsetSpline));

        // Smooth C1 cubic Hermite spline for terrain heightVariation (replicates GC4 d9: Valley=0.2, Flat=0.4, Mountain=1.0..1.35)
        var factorSpline = CubicSpline.builder(continentsCoord)
                .addPoint(-1.00F, 0.25F, 0.0F)
                .addPoint(-0.35F, 0.28F, 0.20F)
                .addPoint(-0.15F, 0.36F, 0.40F)
                .addPoint(0.05F, 0.45F, 0.15F)
                .addPoint(0.25F, 0.50F, 0.50F)
                .addPoint(0.35F, 0.65F, 1.10F)
                .addPoint(0.65F, 1.05F, 1.00F)
                .addPoint(1.00F, 1.35F, 0.60F)
                .build();
        DensityFunction venusFactor = registerAndWrap(context, Venus.FACTOR, DensityFunctions.spline(factorSpline));

        // GC4 ChunkProviderVenus noiseGenSmooth1 (4 octaves, persistence 0.25, freq 0.015) +
        // GalaxySpace WE_Biome volcanic terrain layer (5 octaves, persistence 0.6) + ridge detail on mountains.
        DensityFunction gc4SmoothNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.96, noiseRegistry.getOrThrow(GCNoiseData.VENUS_SMOOTH_NOISE));
        DensityFunction gsVolcanicNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.50, noiseRegistry.getOrThrow(GCNoiseData.VENUS_VOLCANIC_NOISE));
        DensityFunction ridgeNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.35, noiseRegistry.getOrThrow(Noises.RIDGE));

        DensityFunction venusSmoothRelief = registerAndWrap(context, Venus.SMOOTH_RELIEF,
                DensityFunctions.mul(
                        venusFactor,
                        DensityFunctions.add(
                                DensityFunctions.mul(DensityFunctions.constant(0.16), gc4SmoothNoise),
                                DensityFunctions.add(
                                        DensityFunctions.mul(DensityFunctions.constant(0.09), gsVolcanicNoise),
                                        DensityFunctions.mul(DensityFunctions.constant(0.05), ridgeNoise)
                                )
                        )
                )
        );

        // Subtle 3D BlendedNoise weighted by venusFactor for natural craggy volcanic rock faces without ever overhanging into floating islands
        DensityFunction subtle3dRoughness = DensityFunctions.mul(
                venusFactor,
                DensityFunctions.mul(
                        DensityFunctions.constant(0.045),
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
    }

    private static DensityFunction registerAndWrap(BootstrapContext<DensityFunction> context, ResourceKey<DensityFunction> key, DensityFunction densityFunction) {
        return new DensityFunctions.HolderHolder(context.register(key, densityFunction));
    }

    public static DensityFunction getFunction(HolderGetter<DensityFunction> densityFunctions, ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(key));
    }
}
