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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Typed mounting socket declared by a modular vehicle chassis.
 * Sibling pattern to {@link dev.galacticraft.api.rocket.assembly.AttachmentNode}.
 */
public record VehicleHardpoint(
        @NotNull String id,
        @NotNull HardpointType type,
        double localX,
        double localY,
        double localZ,
        @NotNull VehicleSizeClass maxPartSize,
        boolean required
) {
    public VehicleHardpoint {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("hardpoint id must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(maxPartSize, "maxPartSize");
    }

    public @NotNull Vec3 localOffset() {
        return new Vec3(this.localX, this.localY, this.localZ);
    }

    public boolean accepts(@NotNull HardpointType partType, @NotNull VehicleSizeClass partSize) {
        return this.type == partType && partSize.fitsIn(this.maxPartSize);
    }

    public static final Codec<VehicleHardpoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(VehicleHardpoint::id),
            HardpointType.CODEC.fieldOf("type").forGetter(VehicleHardpoint::type),
            Codec.DOUBLE.fieldOf("local_x").forGetter(VehicleHardpoint::localX),
            Codec.DOUBLE.fieldOf("local_y").forGetter(VehicleHardpoint::localY),
            Codec.DOUBLE.fieldOf("local_z").forGetter(VehicleHardpoint::localZ),
            VehicleSizeClass.CODEC.fieldOf("max_part_size").forGetter(VehicleHardpoint::maxPartSize),
            Codec.BOOL.optionalFieldOf("required", false).forGetter(VehicleHardpoint::required)
    ).apply(instance, VehicleHardpoint::new));

    public static final StreamCodec<ByteBuf, VehicleHardpoint> STREAM_CODEC = StreamCodec.of(
            (buf, hp) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, hp.id);
                HardpointType.STREAM_CODEC.encode(buf, hp.type);
                ByteBufCodecs.DOUBLE.encode(buf, hp.localX);
                ByteBufCodecs.DOUBLE.encode(buf, hp.localY);
                ByteBufCodecs.DOUBLE.encode(buf, hp.localZ);
                VehicleSizeClass.STREAM_CODEC.encode(buf, hp.maxPartSize);
                ByteBufCodecs.BOOL.encode(buf, hp.required);
            },
            buf -> new VehicleHardpoint(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    HardpointType.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    VehicleSizeClass.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );
}
