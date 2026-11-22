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

package dev.galacticraft.api.rocket.part.modular;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Aerospace hull paint livery selectable for modular rocket parts.
 */
public enum RocketLivery implements StringRepresentable {
    WHITE("white", "White", 0xF0F2F5),
    BLACK("black", "Black", 0x22262E),
    ORANGE("orange", "Orange", 0xD96B27),
    BLUE("blue", "Blue", 0x2B5EA8),
    OLIVE("olive", "Olive", 0x5E6B47);

    public static final Codec<RocketLivery> CODEC = StringRepresentable.fromEnum(RocketLivery::values);
    public static final StreamCodec<ByteBuf, RocketLivery> STREAM_CODEC = ByteBufCodecs.idMapper(
            RocketLivery::byOrdinal,
            Enum::ordinal
    );

    private final String serializedName;
    private final String displayName;
    private final int rgbColor;

    RocketLivery(String serializedName, String displayName, int rgbColor) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.rgbColor = rgbColor & 0x00FFFFFF;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }

    public @NotNull String displayName() {
        return this.displayName;
    }

    public int rgbColor() {
        return this.rgbColor;
    }

    public int argbColor() {
        return 0xFF000000 | this.rgbColor;
    }

    public static @NotNull RocketLivery byOrdinal(int ordinal) {
        RocketLivery[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    public static @Nullable RocketLivery byName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (RocketLivery value : values()) {
            if (value.serializedName.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static @NotNull RocketLivery byName(@Nullable String name, @NotNull RocketLivery fallback) {
        RocketLivery found = byName(name);
        return found != null ? found : fallback;
    }
}
