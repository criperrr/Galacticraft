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

package dev.galacticraft.api.rocket.part.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

/**
 * Represents the structural degradation and maintenance classification of a rocket component.
 */
public enum PartDamageState implements StringRepresentable {
    NOMINAL("nominal", 1.0),
    WORN("worn", 0.75),
    COMPROMISED("compromised", 0.40),
    DAMAGED("damaged", 0.10);

    public static final Codec<PartDamageState> CODEC = StringRepresentable.fromEnum(PartDamageState::values);

    private static final IntFunction<PartDamageState> BY_ID = ByIdMap.continuous(
            PartDamageState::ordinal,
            values(),
            ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final StreamCodec<ByteBuf, PartDamageState> STREAM_CODEC = ByteBufCodecs.idMapper(
            BY_ID,
            PartDamageState::ordinal
    );

    private final String name;
    private final double threshold;

    PartDamageState(String name, double threshold) {
        this.name = name;
        this.threshold = threshold;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    /**
     * @return The baseline integrity threshold fraction associated with this state.
     */
    public double threshold() {
        return this.threshold;
    }

    /**
     * Getter alias for {@link #threshold()}.
     *
     * @return The baseline integrity threshold fraction.
     */
    public double getThreshold() {
        return this.threshold;
    }

    /**
     * Resolves the damage state corresponding to a given integrity ratio (current / max).
     *
     * @param fraction The integrity fraction (typically between 0.0 and 1.0).
     * @return The corresponding {@link PartDamageState}.
     */
    public static @NotNull PartDamageState fromIntegrity(double fraction) {
        if (fraction >= 0.95) return NOMINAL;
        if (fraction >= 0.75) return WORN;
        if (fraction >= 0.40) return COMPROMISED;
        return DAMAGED;
    }

    /**
     * @return Whether this part is certified space-ready with nominal launch safety.
     */
    public boolean isSafe() {
        return this == NOMINAL;
    }

    /**
     * @return Whether this part suffers from severe micro-fissures or structural deformation.
     */
    public boolean isCritical() {
        return this == DAMAGED;
    }
}
