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

package dev.galacticraft.mod.recipe;

import dev.galacticraft.machinelib.api.compat.vanilla.RecipeHelper;
import dev.galacticraft.machinelib.api.storage.SlottedStorageAccess;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Recipe input for the Part Assembler, combining a non-consumed schematic blueprint
 * with a 3x3 crafting grid of engineering materials.
 */
public record PartAssemblerRecipeInput(@NotNull ItemStack schematic, @NotNull CraftingInput grid) implements RecipeInput {

    public PartAssemblerRecipeInput {
        Objects.requireNonNull(schematic, "schematic cannot be null");
        Objects.requireNonNull(grid, "grid cannot be null");
    }

    public PartAssemblerRecipeInput(@NotNull ItemStack schematic, @NotNull SlottedStorageAccess<Item, ItemResourceSlot> gridStorage) {
        this(schematic, RecipeHelper.craftingInput(3, 3, gridStorage.getSlots()));
    }

    public PartAssemblerRecipeInput(@NotNull ItemStack schematic, int width, int height, @NotNull List<ItemStack> gridItems) {
        this(schematic, CraftingInput.of(width, height, gridItems));
    }

    @Override
    public @NotNull ItemStack getItem(int index) {
        if (index == 0) {
            return this.schematic;
        }
        int gridIndex = index - 1;
        if (gridIndex >= 0 && gridIndex < this.grid.size()) {
            return this.grid.getItem(gridIndex);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1 + this.grid.size();
    }

    @Override
    public boolean isEmpty() {
        return this.schematic.isEmpty() && this.grid.isEmpty();
    }

    /**
     * Convenience alias for {@link #grid()} to align with standard crafting input terminology.
     *
     * @return the crafting grid input
     */
    public @NotNull CraftingInput craftingInput() {
        return this.grid();
    }
}
