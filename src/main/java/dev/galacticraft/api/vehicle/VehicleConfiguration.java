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

package dev.galacticraft.api.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable data-driven snapshot of an assembled (or partially assembled) modular vehicle.
 * Stored on {@code ModularVehicleEntity} via {@code SynchedEntityData} and packed into
 * {@code VehicleCrateItem} via {@code GCDataComponents.VEHICLE_CONFIGURATION}.
 */
public record VehicleConfiguration(
        @NotNull ResourceLocation chassisId,
        @NotNull Map<String, ResourceLocation> installedParts,
        int paintColor,
        long storedEnergy,
        long storedFuel
) {
    public static final int DEFAULT_PAINT_COLOR = 0xD8DCE3;

    public VehicleConfiguration {
        Objects.requireNonNull(chassisId, "chassisId");
        Objects.requireNonNull(installedParts, "installedParts");
        installedParts = Collections.unmodifiableMap(new LinkedHashMap<>(installedParts));
        storedEnergy = Math.max(0L, storedEnergy);
        storedFuel = Math.max(0L, storedFuel);
    }

    public static @NotNull VehicleConfiguration emptyChassis(@NotNull ResourceLocation chassisId) {
        return new VehicleConfiguration(chassisId, Map.of(), DEFAULT_PAINT_COLOR, 0L, 0L);
    }

    public @Nullable ResourceLocation getPart(@NotNull String hardpointId) {
        return this.installedParts.get(hardpointId);
    }

    public boolean hasPart(@NotNull String hardpointId) {
        return this.installedParts.containsKey(hardpointId);
    }

    public @NotNull VehicleConfiguration withPart(@NotNull String hardpointId, @NotNull ResourceLocation partId) {
        Map<String, ResourceLocation> next = new LinkedHashMap<>(this.installedParts);
        next.put(hardpointId, partId);
        return new VehicleConfiguration(this.chassisId, next, this.paintColor, this.storedEnergy, this.storedFuel);
    }

    public @NotNull VehicleConfiguration withoutPart(@NotNull String hardpointId) {
        if (!this.installedParts.containsKey(hardpointId)) {
            return this;
        }
        Map<String, ResourceLocation> next = new LinkedHashMap<>(this.installedParts);
        next.remove(hardpointId);
        return new VehicleConfiguration(this.chassisId, next, this.paintColor, this.storedEnergy, this.storedFuel);
    }

    public @NotNull VehicleConfiguration withResources(long energy, long fuel) {
        return new VehicleConfiguration(this.chassisId, this.installedParts, this.paintColor, energy, fuel);
    }

    public @NotNull VehicleConfiguration withPaintColor(int color) {
        return new VehicleConfiguration(this.chassisId, this.installedParts, color, this.storedEnergy, this.storedFuel);
    }

    public static final Codec<VehicleConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("chassis_id").forGetter(VehicleConfiguration::chassisId),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                    .optionalFieldOf("installed_parts", Map.of())
                    .forGetter(VehicleConfiguration::installedParts),
            Codec.INT.optionalFieldOf("paint_color", DEFAULT_PAINT_COLOR).forGetter(VehicleConfiguration::paintColor),
            Codec.LONG.optionalFieldOf("stored_energy", 0L).forGetter(VehicleConfiguration::storedEnergy),
            Codec.LONG.optionalFieldOf("stored_fuel", 0L).forGetter(VehicleConfiguration::storedFuel)
    ).apply(instance, VehicleConfiguration::new));

    public static final StreamCodec<ByteBuf, VehicleConfiguration> STREAM_CODEC = StreamCodec.of(
            (buf, cfg) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, cfg.chassisId);
                ByteBufCodecs.VAR_INT.encode(buf, cfg.installedParts.size());
                for (Map.Entry<String, ResourceLocation> entry : cfg.installedParts.entrySet()) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, entry.getKey());
                    ResourceLocation.STREAM_CODEC.encode(buf, entry.getValue());
                }
                ByteBufCodecs.INT.encode(buf, cfg.paintColor);
                ByteBufCodecs.VAR_LONG.encode(buf, cfg.storedEnergy);
                ByteBufCodecs.VAR_LONG.encode(buf, cfg.storedFuel);
            },
            buf -> {
                ResourceLocation chassisId = ResourceLocation.STREAM_CODEC.decode(buf);
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                Map<String, ResourceLocation> parts = new LinkedHashMap<>(count);
                for (int i = 0; i < count; i++) {
                    String hp = ByteBufCodecs.STRING_UTF8.decode(buf);
                    ResourceLocation part = ResourceLocation.STREAM_CODEC.decode(buf);
                    parts.put(hp, part);
                }
                int color = ByteBufCodecs.INT.decode(buf);
                long energy = ByteBufCodecs.VAR_LONG.decode(buf);
                long fuel = ByteBufCodecs.VAR_LONG.decode(buf);
                return new VehicleConfiguration(chassisId, parts, color, energy, fuel);
            }
    );
}
