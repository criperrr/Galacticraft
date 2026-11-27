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

package dev.galacticraft.api.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.api.APIConstants;
import dev.galacticraft.api.fluid.FluidData;
import dev.galacticraft.api.rocket.RocketData;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.api.util.StreamCodecs;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.function.UnaryOperator;

public class GCDataComponents {
    public static final DataComponentType<RocketData> ROCKET_DATA = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            APIConstants.id("rocket_data"),
            DataComponentType.<RocketData>builder()
                    .persistent(RocketData.CODEC)
                    .networkSynchronized(RocketData.STREAM_CODEC)
                    .build()
    );
    public static final DataComponentType<Long> AMOUNT = register("amount", b -> b
            .persistent(Codec.LONG).networkSynchronized(StreamCodecs.LONG));
    public static final DataComponentType<Integer> COLOR = register("color", b -> b
            .persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.INT));
    public static final DataComponentType<Integer> TICKS_UNTIL_COOL = register("ticks_until_cool", b -> b
            .persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.INT));
    public static final DataComponentType<Boolean> CREATIVE = register("creative", b -> b
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DataComponentType<ResourceKey<?>> KEY = register("key", b -> b
            .persistent(RecordCodecBuilder.create(i ->
                    i.group(
                            ResourceLocation.CODEC.fieldOf("registry").forGetter(ResourceKey::registry),
                            ResourceLocation.CODEC.fieldOf("location").forGetter(ResourceKey::location)
                    ).apply(i, (r, l) -> ResourceKey.create(ResourceKey.createRegistryKey(r), l))
            )).networkSynchronized(StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    ResourceKey::registry,
                    ResourceLocation.STREAM_CODEC,
                    ResourceKey::location,
                    (r, l) -> ResourceKey.create(ResourceKey.createRegistryKey(r), l)
            )));

    public static final DataComponentType<FluidData> FLUID_DATA = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            APIConstants.id("fluid_data"),
            DataComponentType.<FluidData>builder()
                    .persistent(FluidData.CODEC)
                    .networkSynchronized(FluidData.STREAM_CODEC)
                    .build()
    );
    public static final DataComponentType<RocketPartStats> ROCKET_PART_STATS = register("rocket_part_stats", b -> b
            .persistent(RocketPartStats.CODEC)
            .networkSynchronized(RocketPartStats.STREAM_CODEC));
    public static final DataComponentType<PartIntegrity> PART_INTEGRITY = register("part_integrity", b -> b
            .persistent(PartIntegrity.CODEC)
            .networkSynchronized(PartIntegrity.STREAM_CODEC));
    public static final DataComponentType<RocketPartConfig> ROCKET_PART_CONFIG = register("rocket_part_config", b -> b
            .persistent(RocketPartConfig.CODEC)
            .networkSynchronized(RocketPartConfig.STREAM_CODEC));
    public static final DataComponentType<RocketPartComposition> ROCKET_PART_COMPOSITION = register("rocket_part_composition", b -> b
            .persistent(RocketPartComposition.CODEC)
            .networkSynchronized(RocketPartComposition.STREAM_CODEC));
    public static final DataComponentType<TelemetryData> TELEMETRY_DATA = register("telemetry_data", b -> b
            .persistent(TelemetryData.CODEC)
            .networkSynchronized(TelemetryData.STREAM_CODEC));
    public static final DataComponentType<OrbitalSurveyData> ORBITAL_SURVEY_DATA = register("orbital_survey_data", b -> b
            .persistent(OrbitalSurveyData.CODEC)
            .networkSynchronized(OrbitalSurveyData.STREAM_CODEC));
    public static final DataComponentType<RocketAssemblyGraph> ROCKET_ASSEMBLY_GRAPH = register("rocket_assembly_graph", b -> b
            .persistent(RocketAssemblyGraph.CODEC)
            .networkSynchronized(RocketAssemblyGraph.STREAM_CODEC));
    public static final DataComponentType<VehicleConfiguration> VEHICLE_CONFIGURATION = register("vehicle_configuration", b -> b
            .persistent(VehicleConfiguration.CODEC)
            .networkSynchronized(VehicleConfiguration.STREAM_CODEC));

    private static <T> DataComponentType<T> register(String id, UnaryOperator<DataComponentType.Builder<T>> op) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, APIConstants.id(id), op.apply(DataComponentType.builder()).build());
    }

    public static void init() {
    }
}
