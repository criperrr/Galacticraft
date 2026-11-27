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

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Implemented by items (such as {@code VehicleCrateItem}) that occupy a multi-slot
 * 2D rectangle (width x height) inside spatial cargo inventories such as a rocket cargo grid.
 */
public interface SpatialCargoItem {
    int getCargoWidth(@NotNull ItemStack stack);

    int getCargoHeight(@NotNull ItemStack stack);

    static int widthOf(@NotNull ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof SpatialCargoItem spatial) {
            return Math.max(1, spatial.getCargoWidth(stack));
        }
        return 1;
    }

    static int heightOf(@NotNull ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof SpatialCargoItem spatial) {
            return Math.max(1, spatial.getCargoHeight(stack));
        }
        return 1;
    }
}
