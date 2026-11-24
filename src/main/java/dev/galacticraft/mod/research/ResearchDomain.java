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

package dev.galacticraft.mod.research;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The 5 Scientific Domains that aggregate experimental research points across all
 * planetary laboratories and telemetry instruments in Galacticraft 5.
 */
public enum ResearchDomain implements StringRepresentable {
    THERMODYNAMICS("thermodynamics", "THERM", "Thermodynamics & Ablation", 0xFFF59E0B, ChatFormatting.GOLD),
    CRYOGENICS("cryogenics", "CRYO", "Cryogenics & ISRU Chemistry", 0xFF38BDF8, ChatFormatting.AQUA),
    STRUCTURES("structures", "STRUC", "Structures & High Pressure", 0xFF34D399, ChatFormatting.GREEN),
    ELECTROMAGNETISM("electromagnetism", "EMAG", "Electromagnetism & Avionics", 0xFFC084FC, ChatFormatting.LIGHT_PURPLE),
    NUCLEAR_PLASMA("nuclear_plasma", "NUCL", "Nuclear Physics & Plasma", 0xFFFACC15, ChatFormatting.YELLOW);

    public static final Codec<ResearchDomain> CODEC = StringRepresentable.fromEnum(ResearchDomain::values);
    public static final StreamCodec<ByteBuf, ResearchDomain> STREAM_CODEC = ByteBufCodecs.idMapper(
            i -> values()[Math.floorMod(i, values().length)],
            ResearchDomain::ordinal
    );

    private final String id;
    private final String shortCode;
    private final String displayName;
    private final int colorArgb;
    private final ChatFormatting chatColor;

    ResearchDomain(String id, String shortCode, String displayName, int colorArgb, ChatFormatting chatColor) {
        this.id = id;
        this.shortCode = shortCode;
        this.displayName = displayName;
        this.colorArgb = colorArgb;
        this.chatColor = chatColor;
    }

    public @NotNull String id() {
        return this.id;
    }

    public @NotNull String shortCode() {
        return this.shortCode;
    }

    public @NotNull String displayName() {
        return this.displayName;
    }

    public int colorArgb() {
        return this.colorArgb;
    }

    public @NotNull ChatFormatting chatColor() {
        return this.chatColor;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.id;
    }

    public static @Nullable ResearchDomain byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (ResearchDomain domain : values()) {
            if (domain.id.equals(normalized)) {
                return domain;
            }
        }
        return null;
    }
}
