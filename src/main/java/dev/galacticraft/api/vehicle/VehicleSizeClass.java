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
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Vehicle size class controlling hardpoint compatibility, entity footprint,
 * and 2D spatial slot footprint when packed into a {@code VehicleCrateItem}
 * inside a rocket cargo grid.
 */
public enum VehicleSizeClass implements StringRepresentable {
    SMALL("small", 2, 2, 2.20F, 1.85F, 1.05F),
    MEDIUM("medium", 3, 2, 2.85F, 2.20F, 1.05F),
    LARGE("large", 4, 3, 3.55F, 2.75F, 1.10F);

    public static final Codec<VehicleSizeClass> CODEC = StringRepresentable.fromEnum(VehicleSizeClass::values);
    public static final StreamCodec<ByteBuf, VehicleSizeClass> STREAM_CODEC = ByteBufCodecs.idMapper(
            i -> values()[Math.floorMod(i, values().length)],
            Enum::ordinal
    );

    private final String serializedName;
    private final int cargoWidth;
    private final int cargoHeight;
    private final float baseWidth;
    private final float baseHeight;
    private final float maxUpStep;

    VehicleSizeClass(String serializedName, int cargoWidth, int cargoHeight, float baseWidth, float baseHeight, float maxUpStep) {
        this.serializedName = serializedName;
        this.cargoWidth = cargoWidth;
        this.cargoHeight = cargoHeight;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.maxUpStep = maxUpStep;
    }

    public int cargoWidth() {
        return this.cargoWidth;
    }

    public int cargoHeight() {
        return this.cargoHeight;
    }

    public float baseWidth() {
        return this.baseWidth;
    }

    public float baseHeight() {
        return this.baseHeight;
    }

    public float maxUpStep() {
        return this.maxUpStep;
    }

    public boolean fitsIn(@NotNull VehicleSizeClass maxAllowed) {
        return this.ordinal() <= maxAllowed.ordinal();
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }
}
