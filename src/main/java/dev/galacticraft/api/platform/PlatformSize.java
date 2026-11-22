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

package dev.galacticraft.api.platform;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Discrete rocket-bay diameter classes supported by modular launch and integration platforms.
 */
public enum PlatformSize implements StringRepresentable {
    SIZE_1X1("1x1", 1, 3, 1, 3),
    SIZE_3X3("3x3", 3, 5, 2, 4),
    SIZE_5X5("5x5", 5, 7, 3, 6),
    SIZE_7X7("7x7", 7, 9, 4, 8),
    SIZE_9X9("9x9", 9, 11, 5, 10),
    SIZE_11X11("11x11", 11, 13, 6, 14);

    public static final Codec<PlatformSize> CODEC = StringRepresentable.fromEnum(PlatformSize::values);
    public static final StreamCodec<ByteBuf, PlatformSize> STREAM_CODEC = ByteBufCodecs.idMapper(
            i -> values()[Math.floorMod(i, values().length)],
            Enum::ordinal
    );

    private final String serializedName;
    private final int diameter;
    private final int padDeckWidth;
    private final int maxTier;
    private final int wingSocketCount;

    PlatformSize(String serializedName, int diameter, int padDeckWidth, int maxTier, int wingSocketCount) {
        this.serializedName = serializedName;
        this.diameter = diameter;
        this.padDeckWidth = padDeckWidth;
        this.maxTier = maxTier;
        this.wingSocketCount = wingSocketCount;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }

    public int diameter() {
        return this.diameter;
    }

    public int padDeckWidth() {
        return this.padDeckWidth;
    }

    public int maxTier() {
        return this.maxTier;
    }

    public int wingSocketCount() {
        return this.wingSocketCount;
    }

    public int wingSocketCount(PlatformType type) {
        if (type == PlatformType.HORIZONTAL && this == SIZE_1X1) {
            return 2;
        }
        return this.wingSocketCount;
    }

    public boolean supports(PlatformType type) {
        if (type == PlatformType.HORIZONTAL) {
            return this == SIZE_1X1 || this == SIZE_3X3;
        }
        return true;
    }

    public @NotNull String modelKey(PlatformType type) {
        return type.getSerializedName() + "_" + this.serializedName;
    }

    public static @NotNull PlatformSize clampForType(PlatformType type, PlatformSize size) {
        if (size == null) {
            return SIZE_1X1;
        }
        if (size.supports(type)) {
            return size;
        }
        return SIZE_3X3;
    }

    public static @NotNull PlatformSize forPadSpan(int spanBlocks) {
        if (spanBlocks <= 3) {
            return SIZE_1X1;
        }
        if (spanBlocks <= 5) {
            return SIZE_3X3;
        }
        if (spanBlocks <= 7) {
            return SIZE_5X5;
        }
        if (spanBlocks <= 9) {
            return SIZE_7X7;
        }
        if (spanBlocks <= 11) {
            return SIZE_9X9;
        }
        return SIZE_11X11;
    }

    public static @NotNull PlatformSize byOrdinal(int ordinal) {
        PlatformSize[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    public static @NotNull PlatformSize byName(String name, PlatformSize fallback) {
        for (PlatformSize value : values()) {
            if (value.serializedName.equals(name)) {
                return value;
            }
        }
        return fallback;
    }
}
