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

package dev.galacticraft.mod.world.biome.source;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.mixin.MultiNoiseBiomeSourceParameterListPresetAccessor;
import dev.galacticraft.mod.world.biome.GCBiomes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class GCMultiNoiseBiomeSourceParameterLists {
    private static final Parameter FULL_RANGE = Parameter.span(-1.0F, 1.0F);

    private static final Parameter RIVER_CONTINENTALNESS = Parameter.span(-0.11F, 0.55F);
    private static final Parameter SHORE_CONTINENTALNESS = Parameter.span(-0.19F, -0.11F);
    private static final Parameter NEAR_INLAND_CONTINENTALNESS = Parameter.span(-0.11F, 0.03F);
    private static final Parameter MID_INLAND_CONTINENTALNESS = Parameter.span(0.03F, 0.3F);
    private static final Parameter FAR_INLAND_CONTINENTALNESS = Parameter.span(0.3F, 1.0F);

    private static final Parameter DRY = Parameter.span(-1.0F, -0.35F);
    private static final Parameter SOMEWHAT_DRY = Parameter.span(-0.35F, -0.1F);
    private static final Parameter DEFAULT_HUMIDITY = Parameter.span(-0.1F, 0.1F);
    private static final Parameter SOMEWHAT_HUMID = Parameter.span(0.1F, 0.3F);
    private static final Parameter HUMID = Parameter.span(0.3F, 1.0F);

    private static final Parameter COLD = Parameter.span(-1.0F, -0.45F);
    private static final Parameter COOL = Parameter.span(-0.45F, -0.15F);
    private static final Parameter DEFAULT_TEMPERATURE = Parameter.span(-0.15F, 0.2F);
    private static final Parameter WARM = Parameter.span(0.2F, 0.55F);
    private static final Parameter HOT = Parameter.span(0.55F, 1.0F);


    private static final Parameter MAX_EROSION = Parameter.span(-1.0F, -0.78F); //todo better names
    private static final Parameter ERODED = Parameter.span(-0.78F, -0.375F);
    private static final Parameter MOSTLY_ERODED = Parameter.span(-0.375F, -0.2225F);
    private static final Parameter MED_EROSION = Parameter.span(-0.2225F, 0.05F);
    private static final Parameter SOMEWHAT_ERODED = Parameter.span(0.05F, 0.45F);
    private static final Parameter NOT_VERY_ERODED = Parameter.span(0.45F, 0.55F);
    private static final Parameter MIN_EROSION = Parameter.span(0.55F, 1.0F);

    private static final Parameter CAVE_DEPTH = Parameter.span(0.2F, 0.9F);

    private static final Parameter WEIRDNESS_L_MOUNTAINS = Parameter.span(-0.7666667F, -0.56666666F);
    private static final Parameter WEIRDNESS_L_PLAINS = Parameter.span(-0.56666666F, -0.4F);
    private static final Parameter WEIRDNESS_L_MIXED = Parameter.span(-0.4F, -0.26666668F);
    private static final Parameter WEIRDNESS_L_ADJ_RIVER = Parameter.span(-0.26666668F, -0.05F);
    private static final Parameter WEIRDNESS_RIVER = Parameter.span(-0.05F, 0.05F);
    private static final Parameter WEIRDNESS_H_ADJ_RIVER = Parameter.span(0.05F, 0.26666668F);
    private static final Parameter WEIRDNESS_H_MIXED = Parameter.span(0.26666668F, 0.4F);
    private static final Parameter WEIRDNESS_H_PLAINS = Parameter.span(0.4F, 0.56666666F);
    private static final Parameter WEIRDNESS_H_MOUNTAINS = Parameter.span(0.56666666F, 0.7666667F);

    public static final ResourceLocation MOON_PRESET_ID = Constant.id("moon");
    public static final ResourceLocation MARS_PRESET_ID = Constant.id("mars");
    public static final ResourceLocation VENUS_PRESET_ID = Constant.id("venus");
    public static final ResourceLocation ASTEROID_PRESET_ID = Constant.id("asteroid");
    public static final ResourceLocation MERCURY_PRESET_ID = Constant.id("mercury");
    public static final MultiNoiseBiomeSourceParameterList.Preset MOON_PRESET = new MultiNoiseBiomeSourceParameterList.Preset(
            MOON_PRESET_ID, GCMultiNoiseBiomeSourceParameterLists::generateMoon
    );
    public static final MultiNoiseBiomeSourceParameterList.Preset MARS_PRESET = new MultiNoiseBiomeSourceParameterList.Preset(
            MARS_PRESET_ID, GCMultiNoiseBiomeSourceParameterLists::generateMars
    );
    public static final MultiNoiseBiomeSourceParameterList.Preset VENUS_PRESET = new MultiNoiseBiomeSourceParameterList.Preset(
            VENUS_PRESET_ID, GCMultiNoiseBiomeSourceParameterLists::generateVenus
    );
    public static final MultiNoiseBiomeSourceParameterList.Preset ASTEROID_PRESET = new MultiNoiseBiomeSourceParameterList.Preset(
            ASTEROID_PRESET_ID, GCMultiNoiseBiomeSourceParameterLists::generateAsteroid
    );
    public static final MultiNoiseBiomeSourceParameterList.Preset MERCURY_PRESET = new MultiNoiseBiomeSourceParameterList.Preset(
            MERCURY_PRESET_ID, GCMultiNoiseBiomeSourceParameterLists::generateMercury
    );

    public static final ResourceKey<MultiNoiseBiomeSourceParameterList> MOON = Constant.key(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, "moon");
    public static final ResourceKey<MultiNoiseBiomeSourceParameterList> MARS = Constant.key(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, "mars");
    public static final ResourceKey<MultiNoiseBiomeSourceParameterList> VENUS = Constant.key(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, "venus");
    public static final ResourceKey<MultiNoiseBiomeSourceParameterList> ASTEROID = Constant.key(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, "asteroid");
    public static final ResourceKey<MultiNoiseBiomeSourceParameterList> MERCURY = Constant.key(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, "mercury");

    @Contract("_ -> new")
    private static <T> Climate.@NotNull ParameterList<T> generateMoon(Function<ResourceKey<Biome>, T> biomeRegistry) {
        ImmutableList.Builder<Pair<Climate.ParameterPoint, T>> builder = ImmutableList.builder();
        // Aligns all 5 Moon biomes with GCDensityFunctions.Moon elevation splines:
        // 1. Maria Basins [-1.0, -0.35]:
        //    - BASALTIC_MARE: vast dark basaltic maria plains (temperature [-0.35, 1.0])
        //    - OLIVINE_SPIKES: mantle-uplift olivine basalt & crystalline spike basins (temperature [-1.0, -0.35])
        // 2. Lunar Regolith Plains [-0.35, 0.25]:
        //    - LUNAR_LOWLANDS: classic GC4 ChunkProviderMoon rolling regolith plains (temperature [-0.45, 1.0])
        //    - COMET_TUNDRA: shadowed cometary ice & regolith plains (temperature [-1.0, -0.45])
        // 3. Lunar Highlands [0.25, 1.0]:
        //    - LUNAR_HIGHLANDS: elevated cratered highlands & lunar mountain ranges
        writeBiomeParameters(builder::add,
                Parameter.span(-0.35F, 1.0F),
                FULL_RANGE,
                Parameter.span(-1.0F, -0.35F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Moon.BASALTIC_MARE));
        writeBiomeParameters(builder::add,
                Parameter.span(-1.0F, -0.35F),
                FULL_RANGE,
                Parameter.span(-1.0F, -0.35F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Moon.OLIVINE_SPIKES));
        writeBiomeParameters(builder::add,
                Parameter.span(-0.45F, 1.0F),
                FULL_RANGE,
                Parameter.span(-0.35F, 0.25F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Moon.LUNAR_LOWLANDS));
        writeBiomeParameters(builder::add,
                Parameter.span(-1.0F, -0.45F),
                FULL_RANGE,
                Parameter.span(-0.35F, 0.25F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Moon.COMET_TUNDRA));
        writeBiomeParameters(builder::add,
                FULL_RANGE,
                FULL_RANGE,
                Parameter.span(0.25F, 1.0F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Moon.LUNAR_HIGHLANDS));

        return new Climate.ParameterList<>(builder.build());
    }

    @Contract("_ -> new")
    private static <T> Climate.@NotNull ParameterList<T> generateMars(Function<ResourceKey<Biome>, T> biomeRegistry) {
        ImmutableList.Builder<Pair<Climate.ParameterPoint, T>> builder = ImmutableList.builder();
        // Aligns Mars biome boundaries 100% with GCDensityFunctions.Mars elevation splines and GalaxySpace WorldProviderMars_WE bands:
        // - MARS_CANYON [-1.0, -0.20]: deep Martian canyons/ravines (GC4 VALLEY_HEIGHT_MOD & GalaxySpace Mars_Ravine)
        // - MARS_FLAT [-0.20, 0.30]: rusty plains & high plains (GC4 BiomeGenFlatMars & GalaxySpace Mars_Plains / Mars_High_Plains)
        // - MARS_HIGHLANDS [0.30, 1.0]: rugged highlands & mountains (GC4 MOUNTAIN_HEIGHT_MOD & GalaxySpace Mars_Mountains / Mars_Triangle_Mountains)
        writeBiomeParameters(builder::add,
                HOT,
                FULL_RANGE,
                Parameter.span(-1.0F, -0.20F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Mars.MARS_CANYON));
        writeBiomeParameters(builder::add,
                HOT,
                FULL_RANGE,
                Parameter.span(-0.20F, 0.30F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Mars.MARS_FLAT));
        writeBiomeParameters(builder::add,
                HOT,
                FULL_RANGE,
                Parameter.span(0.30F, 1.0F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Mars.MARS_HIGHLANDS));

        return new Climate.ParameterList<>(builder.build());
    }

    @Contract("_ -> new")
    private static <T> Climate.@NotNull ParameterList<T> generateVenus(Function<ResourceKey<Biome>, T> biomeRegistry) {
        ImmutableList.Builder<Pair<Climate.ParameterPoint, T>> builder = ImmutableList.builder();
        // Replicates GC4 GenLayerVenusSurround: VENUS_FLAT [-0.15, 0.35] always surrounds VENUS_MOUNTAIN [0.35, 1.0]
        // and separates it from VENUS_VALLEY [-1.0, -0.15], aligning biome boundaries 100% with terrain elevation splines.
        writeBiomeParameters(builder::add,
                HOT,
                FULL_RANGE,
                Parameter.span(-1.0F, -0.15F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Venus.VENUS_VALLEY));
        writeBiomeParameters(builder::add,
                HOT,
                FULL_RANGE,
                Parameter.span(-0.15F, 0.35F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Venus.VENUS_FLAT));
        writeBiomeParameters(builder::add,
                HOT,
                FULL_RANGE,
                Parameter.span(0.35F, 1.0F),
                FULL_RANGE,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Venus.VENUS_MOUNTAIN));

        return new Climate.ParameterList<>(builder.build());
    }

    @Contract("_ -> new")
    private static <T> Climate.@NotNull ParameterList<T> generateAsteroid(Function<ResourceKey<Biome>, T> biomeRegistry) {
        ImmutableList.Builder<Pair<Climate.ParameterPoint, T>> builder = ImmutableList.builder();
        writeBiomeParameters(builder::add,
                HOT, // hot to prevent snow
                DRY,// ,
                Parameter.span(SHORE_CONTINENTALNESS, MID_INLAND_CONTINENTALNESS),
                MIN_EROSION,
                WEIRDNESS_H_MIXED,
                0.0F,
                biomeRegistry.apply(GCBiomes.Asteroid.ASTEROID_FIELD));

        return new Climate.ParameterList<>(builder.build());
    }

    @Contract("_ -> new")
    private static <T> Climate.@NotNull ParameterList<T> generateMercury(Function<ResourceKey<Biome>, T> biomeRegistry) {
        ImmutableList.Builder<Pair<Climate.ParameterPoint, T>> builder = ImmutableList.builder();

        Parameter surfaceDepth = Parameter.span(-2.0F, 0.18F);

        // ==========================================
        // 1. SURFACE BIOMES (depth <= 0.18, planetary surface & atmosphere)
        // ==========================================

        // Polar Dark Craters: Cold and Polar shadows
        writeMercury3dBiome(
                builder::add,
                COLD,
                FULL_RANGE,
                FULL_RANGE,
                ERODED,
                surfaceDepth,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.POLAR_DARK_CRATERS)
        );

        // Caloris Basin: Deep impact depression, low continentalness (-1.0 to -0.15), elevated daytime temp
        writeMercury3dBiome(
                builder::add,
                WARM,
                DRY,
                Parameter.span(-1.0F, -0.15F),
                SOMEWHAT_ERODED,
                surfaceDepth,
                Parameter.span(-0.6F, 0.4F),
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.CALORIS_BASIN)
        );

        // Smooth Lava Plains: Low-relief volcanic smooth plains, transition lowlands (-0.35 to 0.05)
        writeMercury3dBiome(
                builder::add,
                Parameter.span(-0.2F, 0.5F),
                SOMEWHAT_DRY,
                Parameter.span(-0.35F, 0.05F),
                Parameter.span(0.1F, 1.0F),
                surfaceDepth,
                Parameter.span(-0.7F, -0.1F),
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.SMOOTH_LAVA_PLAINS)
        );

        // Chaotic Terrain: Antipodal jagged broken blocks, high continentalness (0.35 to 1.0), high weirdness
        writeMercury3dBiome(
                builder::add,
                HOT,
                DRY,
                Parameter.span(0.35F, 1.0F),
                NOT_VERY_ERODED,
                surfaceDepth,
                Parameter.span(0.30F, 1.0F),
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.CHAOTIC_TERRAIN)
        );

        // Inter-Crater Plains: Primary base undulating cratered terrain, bridging the entire mid-crust
        writeMercury3dBiome(
                builder::add,
                Parameter.span(-0.2F, 0.6F),
                FULL_RANGE,
                Parameter.span(-0.15F, 0.45F),
                FULL_RANGE,
                surfaceDepth,
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.INTER_CRATER_PLAINS)
        );

        // ==========================================
        // 2. MONUMENTAL SUBTERRANEAN 3D BIOMES (depth > 0.18, complete subterranean coverage)
        // ==========================================

        // (A) Volatile Hollow Labyrinth: Shallow subsurface (depth 0.18 to 0.45)
        writeMercury3dBiome(
                builder::add,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                Parameter.span(0.18F, 0.45F),
                Parameter.span(-0.4F, 0.4F),
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.HOLLOW_LABYRINTH)
        );

        // (B) Colossal Lava Tubes: Horizontal conduits (depth 0.40 to 0.75)
        writeMercury3dBiome(
                builder::add,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                Parameter.span(0.40F, 0.75F),
                Parameter.span(-0.4F, 0.4F),
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.COLOSSAL_LAVA_TUBES)
        );

        // (C) Tectonic Chasms: Deep vertical canyons (depth 0.18 to 1.20, high weirdness fault zones)
        writeMercury3dBiome(
                builder::add,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                Parameter.span(0.18F, 1.20F),
                Parameter.span(0.40F, 1.0F),
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.TECTONIC_CHASMS)
        );
        writeMercury3dBiome(
                builder::add,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                Parameter.span(0.18F, 1.20F),
                Parameter.span(-1.0F, -0.40F),
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.TECTONIC_CHASMS)
        );

        // (D) Megabreccia Caverns: Domed halls (depth 0.70 to 1.20)
        writeMercury3dBiome(
                builder::add,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                Parameter.span(0.70F, 1.20F),
                Parameter.span(-0.40F, 0.40F),
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.MEGABRECCIA_CAVERNS)
        );

        // (E) Magma Depths: Basal gallery (depth 1.15 to 2.00)
        writeMercury3dBiome(
                builder::add,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                FULL_RANGE,
                Parameter.span(1.15F, 2.00F),
                FULL_RANGE,
                0.0F,
                biomeRegistry.apply(GCBiomes.Mercury.MAGMA_DEPTHS)
        );

        return new Climate.ParameterList<>(builder.build());
    }

    private static <T> void writeMercury3dBiome(
            Consumer<Pair<Climate.ParameterPoint, T>> parameters,
            Parameter temperature,
            Parameter humidity,
            Parameter continentalness,
            Parameter erosion,
            Parameter depth,
            Parameter weirdness,
            float offset,
            T biome
    ) {
        parameters.accept(
                Pair.of(
                        Climate.parameters(temperature, humidity, continentalness, erosion, depth, weirdness, offset),
                        biome
                )
        );
    }

    private static <T> void writeBiomeParameters(
            Consumer<Pair<Climate.ParameterPoint, T>> parameters,
            Parameter temperature,
            Parameter humidity,
            Parameter continentalness,
            Parameter erosion,
            Parameter weirdness,
            float offset,
            T biome
    ) {
        parameters.accept(
                Pair.of(
                        Climate.parameters(temperature, humidity, continentalness, erosion, Parameter.point(0.0F), weirdness, offset),
                        biome
                )
        );
        parameters.accept(
                Pair.of(
                        Climate.parameters(temperature, humidity, continentalness, erosion, Parameter.point(1.0F), weirdness, offset),
                        biome
                )
        );
    }

    public static void register() {
        MultiNoiseBiomeSourceParameterListPresetAccessor.getByName().put(MOON_PRESET_ID, MOON_PRESET);
        MultiNoiseBiomeSourceParameterListPresetAccessor.getByName().put(MARS_PRESET_ID, MARS_PRESET);
        MultiNoiseBiomeSourceParameterListPresetAccessor.getByName().put(VENUS_PRESET_ID, VENUS_PRESET);
        MultiNoiseBiomeSourceParameterListPresetAccessor.getByName().put(ASTEROID_PRESET_ID, ASTEROID_PRESET);
        MultiNoiseBiomeSourceParameterListPresetAccessor.getByName().put(MERCURY_PRESET_ID, MERCURY_PRESET);
    }

    public static void bootstrapRegistries(BootstrapContext<MultiNoiseBiomeSourceParameterList> context) {
        HolderGetter<Biome> lookup = context.lookup(Registries.BIOME);
        context.register(MOON, new MultiNoiseBiomeSourceParameterList(MOON_PRESET, lookup));
        context.register(MARS, new MultiNoiseBiomeSourceParameterList(MARS_PRESET, lookup));
        context.register(VENUS, new MultiNoiseBiomeSourceParameterList(VENUS_PRESET, lookup));
        context.register(ASTEROID, new MultiNoiseBiomeSourceParameterList(ASTEROID_PRESET, lookup));
        context.register(MERCURY, new MultiNoiseBiomeSourceParameterList(MERCURY_PRESET, lookup));
    }
}
