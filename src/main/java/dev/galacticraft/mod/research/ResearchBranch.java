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
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * The 5 unified technology tree branches displayed inside the Aerospace R&amp;D Terminal.
 */
public enum ResearchBranch implements StringRepresentable {
    METALLURGY_MACHINES("metallurgy_machines", "METALLURGY", "Metallurgy, Chemistry & Machines", 0xFFF59E0B),
    LIFE_SUPPORT_BASES("life_support_bases", "LIFE SUPP", "Life Support, Bio-Domes & Bases", 0xFF34D399),
    ENERGY_SUPERCONDUCTORS("energy_superconductors", "ENERGY", "Energy, Lasers & Superconductors", 0xFFFACC15),
    SUITS_TOOLS("suits_tools", "SUITS", "Space Suits, Armor & Tools", 0xFF38BDF8),
    AEROSPACE_PROPULSION("aerospace_propulsion", "AERO", "Aerospace & Modular Propulsion", 0xFFC084FC);

    public static final Codec<ResearchBranch> CODEC = StringRepresentable.fromEnum(ResearchBranch::values);

    private final String id;
    private final String tabLabel;
    private final String displayName;
    private final int accentColor;

    ResearchBranch(String id, String tabLabel, String displayName, int accentColor) {
        this.id = id;
        this.tabLabel = tabLabel;
        this.displayName = displayName;
        this.accentColor = accentColor;
    }

    public @NotNull String id() {
        return this.id;
    }

    public @NotNull String tabLabel() {
        return this.tabLabel;
    }

    public @NotNull String displayName() {
        return this.displayName;
    }

    public int accentColor() {
        return this.accentColor;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.id;
    }
}
