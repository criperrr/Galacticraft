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
 * Functional classification of a modular world-placeable rocket part.
 */
public enum RocketPartCategory implements StringRepresentable {
    COMMAND("command", "Command", "command"),
    ENGINE("engine", "Engine", "engines"),
    TANK("tank", "Tank", "tanks"),
    ADAPTER("adapter", "Adapter", "adapters"),
    PAYLOAD("payload", "Payload", "payloads"),
    UTILITY("utility", "Utility", "utility");

    public static final Codec<RocketPartCategory> CODEC = StringRepresentable.fromEnum(RocketPartCategory::values);
    public static final StreamCodec<ByteBuf, RocketPartCategory> STREAM_CODEC = ByteBufCodecs.idMapper(
            RocketPartCategory::byOrdinal,
            Enum::ordinal
    );

    private final String serializedName;
    private final String displayName;
    private final String assetFolder;

    RocketPartCategory(String serializedName, String displayName, String assetFolder) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.assetFolder = assetFolder;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }

    public @NotNull String displayName() {
        return this.displayName;
    }

    public @NotNull String assetFolder() {
        return this.assetFolder;
    }

    public static @NotNull RocketPartCategory byOrdinal(int ordinal) {
        RocketPartCategory[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    public static @Nullable RocketPartCategory byName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (RocketPartCategory value : values()) {
            if (value.serializedName.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static @NotNull RocketPartCategory byName(@Nullable String name, @NotNull RocketPartCategory fallback) {
        RocketPartCategory found = byName(name);
        return found != null ? found : fallback;
    }
}
