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

package dev.galacticraft.mod.manual;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The 6 interactive tabs of the Galacticraft 5 Aerospace Manual ({@code AEROSPACE_MANUAL}).
 */
public enum ManualTab implements StringRepresentable {
    MISSION("mission", "manual.galacticraft.tab.mission", 0xFF38BDF8),
    MATERIALS("materials", "manual.galacticraft.tab.materials", 0xFFF59E0B),
    MACHINES("machines", "manual.galacticraft.tab.machines", 0xFF10B981),
    ATLAS("atlas", "manual.galacticraft.tab.atlas", 0xFFA855F7),
    LAB_LOG("lab_log", "manual.galacticraft.tab.lab_log", 0xFFEC4899),
    INDEX("index", "manual.galacticraft.tab.index", 0xFF60A5FA);

    public static final Codec<ManualTab> CODEC = StringRepresentable.fromEnum(ManualTab::values);

    private final String serializedName;
    private final String translationKey;
    private final int accentColor;

    ManualTab(String serializedName, String translationKey, int accentColor) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
        this.accentColor = accentColor;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }

    public @NotNull String translationKey() {
        return this.translationKey;
    }

    public int accentColor() {
        return this.accentColor;
    }

    public static @NotNull ManualTab byName(@NotNull String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (ManualTab tab : values()) {
            if (tab.serializedName.equals(lower) || tab.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return tab;
            }
        }
        return MISSION;
    }
}
