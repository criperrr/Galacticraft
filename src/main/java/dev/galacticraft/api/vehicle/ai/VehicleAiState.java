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
 * Unified server-authoritative FSM states for autonomous modular planetary vehicles
 * controlled by an installed {@code AI_CORE} module.
 */
public enum VehicleAiState implements StringRepresentable {
    IDLE("idle"),
    PLANNING("planning"),
    TRANSIT("transit"),
    WORKING("working"),
    BOARDING("boarding"),
    STUCK_RECOVERY("stuck_recovery"),
    HOVER_HOLD("hover_hold"),
    GO_AROUND("go_around"),
    RETURN_TO_BASE("return_to_base"),
    CHARGING("charging");

    public static final Codec<VehicleAiState> CODEC = StringRepresentable.fromEnum(VehicleAiState::values);
    public static final StreamCodec<ByteBuf, VehicleAiState> STREAM_CODEC = ByteBufCodecs.idMapper(
            i -> values()[Math.floorMod(i, values().length)],
            Enum::ordinal
    );

    private final String serializedName;

    VehicleAiState(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }

    public boolean isMovingMissionState() {
        return this == PLANNING || this == TRANSIT || this == STUCK_RECOVERY
                || this == HOVER_HOLD || this == GO_AROUND || this == RETURN_TO_BASE;
    }
}
