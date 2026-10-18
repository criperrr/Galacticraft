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

package dev.galacticraft.mod.world.gen.feature;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCRegistry;
import dev.galacticraft.mod.world.gen.feature.config.PlanetaryMeteorConfig;
import dev.galacticraft.mod.world.gen.feature.config.PlanetaryVolcanoConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GCFeatures {
    public static final GCRegistry<Feature<?>> FEATURES = new GCRegistry<>(BuiltInRegistries.FEATURE);

    public static final Feature<NoneFeatureConfiguration> MARS_CAVERN_VINES = FEATURES.register(
            "mars_cavern_vines",
            new MarsCavernVinesFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> VENUS_LAVA_LAKE = FEATURES.register(
            "venus_lava_lake",
            new VenusLavaLakeFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> VENUS_VAPOR_POOL = FEATURES.register(
            "venus_vapor_pool",
            new VenusVaporPoolFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<PlanetaryVolcanoConfig> PLANETARY_VOLCANO = FEATURES.register(
            "venus_volcano",
            new PlanetaryVolcanoFeature(PlanetaryVolcanoConfig.CODEC)
    );
    public static final Feature<PlanetaryMeteorConfig> PLANETARY_METEOR = FEATURES.register(
            "planetary_meteor",
            new PlanetaryMeteorFeature(PlanetaryMeteorConfig.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> VENUS_SURFACE_OUTCROP = FEATURES.register(
            "venus_surface_outcrop",
            new VenusSurfaceOutcropFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> VENUS_CRASHED_PROBE = FEATURES.register(
            "venus_crashed_probe",
            new VenusCrashedProbeFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> MOON_SURFACE_OUTCROP = FEATURES.register(
            "moon_surface_outcrop",
            new MoonSurfaceOutcropFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> MOON_OLIVINE_SPIKE = FEATURES.register(
            "moon_olivine_spike",
            new MoonOlivineSpikeFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> MOON_CAVE_FLORA = FEATURES.register(
            "moon_cave_flora",
            new MoonCaveFloraFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> MERCURY_SPELEOTHEM = FEATURES.register(
            "mercury_speleothem",
            new MercurySpeleothemFeature(NoneFeatureConfiguration.CODEC)
    );

    public static void register() {
        Constant.LOGGER.debug("Registering Galacticraft worldgen features");
    }
}
