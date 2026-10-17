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
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class GCNoiseData {
    public static final ResourceKey<NormalNoise.NoiseParameters> EROSION = createKey("moon/erosion");
    public static final ResourceKey<NormalNoise.NoiseParameters> BASALT_MARE = createKey("moon/basalt_mare");
    public static final ResourceKey<NormalNoise.NoiseParameters> BASALT_MARE_HEIGHT = createKey("moon/basalt_mare_height");
    public static final ResourceKey<NormalNoise.NoiseParameters> MARS_BASE_NOISE = createKey("mars/base_noise");
    public static final ResourceKey<NormalNoise.NoiseParameters> MARS_HILL_NOISE = createKey("mars/hill_noise");
    public static final ResourceKey<NormalNoise.NoiseParameters> MARS_VALLEY_NOISE = createKey("mars/valley_noise");
    public static final ResourceKey<NormalNoise.NoiseParameters> MARS_WE_TERRAIN_NOISE = createKey("mars/we_terrain_noise");
    public static final ResourceKey<NormalNoise.NoiseParameters> VENUS_SMOOTH_NOISE = createKey("venus/smooth_noise");
    public static final ResourceKey<NormalNoise.NoiseParameters> VENUS_VOLCANIC_NOISE = createKey("venus/volcanic_noise");

    private static ResourceKey<NormalNoise.NoiseParameters> createKey(String id) {
        return ResourceKey.create(Registries.NOISE, Constant.id(id));
    }

    public static void bootstrapRegistries(BootstrapContext<NormalNoise.NoiseParameters> context) {
//        register(context, EROSION, -11, 1, 1, 0, 1, 1);
//        register(context, BASALT_MARE, 5, 0, 0.1, 0.2, 0.1, 0, 0, 0, 0);
//        register(context, BASALT_MARE_HEIGHT, -12, 0.3);
        // GC4 ChunkProviderSpace noiseGen1 & noiseGen3: 4 octaves, persistence 0.25, base frequency 0.015 / 0.01
        register(context, MARS_BASE_NOISE, -6, 1.0, 0.25, 0.0625, 0.015625);
        // GC4 ChunkProviderSpace noiseGen2 (SMALL_FEATURE_HEIGHT_MOD = 26): 4 octaves, persistence 0.25, base frequency 0.003
        register(context, MARS_HILL_NOISE, -8, 1.0, 0.25, 0.0625, 0.015625);
        // GC4 ChunkProviderSpace noiseGen4 (VALLEY_HEIGHT_MOD = 50): 2 octaves, persistence 0.25, base frequency 0.01
        register(context, MARS_VALLEY_NOISE, -7, 1.0, 0.25);
        // GalaxySpace WorldProviderMars_WE WE_Biome layer (Mars_Plains / Mars_High_Plains / Mars_Mountains: 4 octaves, scaleX=280)
        register(context, MARS_WE_TERRAIN_NOISE, -8, 1.0, 0.65, 0.4225, 0.274625);
        // GC4 ChunkProviderVenus noiseGenSmooth1: 4 octaves, persistence 0.25, base frequency 0.015 (0.96 * 2^-6)
        register(context, VENUS_SMOOTH_NOISE, -6, 1.0, 0.25, 0.0625, 0.015625);
        // GalaxySpace WE_Biome volcanic terrain layer: 5 octaves, persistence 0.6
        register(context, VENUS_VOLCANIC_NOISE, -7, 1.0, 0.6, 0.36, 0.216, 0.1296);
    }

    private static void register(
            BootstrapContext<NormalNoise.NoiseParameters> context,
            ResourceKey<NormalNoise.NoiseParameters> key,
            int firstOctave,
            double amplitude,
            double... otherAmplitudes
    ) {
        context.register(key, new NormalNoise.NoiseParameters(firstOctave, amplitude, otherAmplitudes));
    }
}
