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
        public static final ResourceKey<DensityFunction> SLOPES = createKey("venus/slopes");
        public static final ResourceKey<DensityFunction> MONS = createKey("venus/mons");
        public static final ResourceKey<DensityFunction> CANALI = createKey("venus/canali");
        public static final ResourceKey<DensityFunction> CALDERA = createKey("venus/caldera");
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

        // Vertical gradient anchor: monotonically decreases from +1.5 at Y=-32 to -1.5 at Y=224.
        // Guarantees that density decreases strictly with height: ZERO floating islands, 100% solid ground.
        DensityFunction venusBaseHeight = registerAndWrap(context, Venus.BASE_HEIGHT,
                DensityFunctions.yClampedGradient(-32, 224, 1.5, -1.5)
        );

        // 2D gentle surface roll for the vast Basalt Plains (VENUS_FLAT): subtle +/- 3.5 block undulations around Y=70
        DensityFunction surfaceNoise2d = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noiseRegistry.getOrThrow(Noises.SURFACE));
        DensityFunction venusSlopes = registerAndWrap(context, Venus.SLOPES,
                DensityFunctions.mul(DensityFunctions.constant(0.04), surfaceNoise2d)
        );

        // 2D Mountain Uplift for Shield Volcanoes (VENUS_MOUNTAIN):
        // Ramps up smoothly when continents > 0.15, lifting the terrain solidly by +70 to +95 blocks (Y=140 to Y=168)
        DensityFunction mountainFactor = DensityFunctions.mul(
                DensityFunctions.constant(2.5),
                DensityFunctions.add(continents, DensityFunctions.constant(-0.15))
        ).clamp(0.0, 1.0);
        DensityFunction mountainBase = DensityFunctions.mul(mountainFactor, DensityFunctions.constant(0.85));
        DensityFunction ridge2d = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noiseRegistry.getOrThrow(Noises.RIDGE));
        DensityFunction mountainRidges = DensityFunctions.mul(
                mountainFactor,
                DensityFunctions.mul(DensityFunctions.constant(0.25), ridge2d)
        );
        DensityFunction venusMons = registerAndWrap(context, Venus.MONS,
                DensityFunctions.add(mountainBase, mountainRidges)
        );

        // 2D Rift Valleys (VENUS_VALLEY / Canali):
        // Drops the valley floor smoothly down to Y=46-52 when continents < 0.05
        DensityFunction valleyFactor = DensityFunctions.mul(
                DensityFunctions.constant(4.0),
                DensityFunctions.add(
                        DensityFunctions.constant(0.05),
                        DensityFunctions.mul(DensityFunctions.constant(-1.0), continents)
                )
        ).clamp(0.0, 1.0);
        DensityFunction venusCanali = registerAndWrap(context, Venus.CANALI,
                DensityFunctions.mul(valleyFactor, DensityFunctions.constant(-0.25))
        );

        // Summit Caldera depression: smooth subtle depression at peak tops (above Y=145)
        DensityFunction calderaFactor = DensityFunctions.mul(
                mountainFactor,
                DensityFunctions.yClampedGradient(140, 175, 0.0, 1.0)
        );
        DensityFunction calciteNoise = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.35, noiseRegistry.getOrThrow(Noises.CALCITE));
        DensityFunction venusCaldera = registerAndWrap(context, Venus.CALDERA,
                DensityFunctions.mul(
                        DensityFunctions.constant(-0.15),
                        DensityFunctions.mul(calderaFactor, DensityFunctions.max(calciteNoise, DensityFunctions.zero()))
                )
        );

        // Final density: Baseline offset of -0.305 centers flat plains at Y=70.
        // All terrain offsets are strictly 2D, ensuring monotonic vertical density falloff and zero floating debris.
        DensityFunction terrainHeightOffset = DensityFunctions.add(
                DensityFunctions.constant(-0.305),
                DensityFunctions.add(
                        venusSlopes,
                        DensityFunctions.add(
                                venusMons,
                                DensityFunctions.add(venusCanali, venusCaldera)
                        )
                )
        );

        context.register(Venus.FINAL_DENSITY, DensityFunctions.add(
                venusBaseHeight,
                terrainHeightOffset
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
