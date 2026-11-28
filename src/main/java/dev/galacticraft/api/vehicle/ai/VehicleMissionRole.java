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

package dev.galacticraft.api.vehicle.ai;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The four autonomous mission roles executable by an installed {@code AI_CORE} module
 * on any modular ground, rotorcraft, or fixed-wing planetary vehicle.
 */
public enum VehicleMissionRole implements StringRepresentable {
    SCIENCE_EXPLORER("science_explorer"),
    CARGO_SHUTTLE("cargo_shuttle"),
    PASSENGER_TAXI("passenger_taxi"),
    ESCORT_PATROL("escort_patrol");

    public static final Codec<VehicleMissionRole> CODEC = StringRepresentable.fromEnum(VehicleMissionRole::values);
    public static final StreamCodec<ByteBuf, VehicleMissionRole> STREAM_CODEC = ByteBufCodecs.idMapper(
            i -> values()[Math.floorMod(i, values().length)],
            Enum::ordinal
    );

    private final String serializedName;

    VehicleMissionRole(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }

    public @NotNull VehicleMissionRole next() {
        VehicleMissionRole[] all = values();
        return all[(this.ordinal() + 1) % all.length];
    }
}
