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
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Specialization category for Galacticraft aerospace vehicle crafting benches,
 * inspired by Immersive Vehicles' dedicated part/chassis/instrument benches.
 */
public enum VehicleBenchCategory implements StringRepresentable {
    CHASSIS("chassis", 0xFF2ED1FF, "gui.galacticraft.vehicle_bench.category.chassis"),
    POWERPLANT("powerplant", 0xFFFFB830, "gui.galacticraft.vehicle_bench.category.powerplant"),
    LOCOMOTION("locomotion", 0xFF4CE08A, "gui.galacticraft.vehicle_bench.category.locomotion"),
    ACCESSORIES("accessories", 0xFF7B8CFF, "gui.galacticraft.vehicle_bench.category.accessories"),
    UNIVERSAL("universal", 0xFF8AA2C0, "gui.galacticraft.vehicle_bench.category.universal");

    public static final Codec<VehicleBenchCategory> CODEC = StringRepresentable.fromEnum(VehicleBenchCategory::values);

    private final String serializedName;
    private final int accentColor;
    private final String translationKey;

    VehicleBenchCategory(String serializedName, int accentColor, String translationKey) {
        this.serializedName = serializedName;
        this.accentColor = accentColor;
        this.translationKey = translationKey;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }

    public int accentColor() {
        return this.accentColor;
    }

    public @NotNull String translationKey() {
        return this.translationKey;
    }

    public boolean accepts(@NotNull VehicleBenchCategory blueprintCategory) {
        return this == UNIVERSAL || this == blueprintCategory;
    }
}
