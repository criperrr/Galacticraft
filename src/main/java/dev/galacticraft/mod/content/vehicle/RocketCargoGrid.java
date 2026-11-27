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

package dev.galacticraft.mod.content.vehicle;

import dev.galacticraft.api.vehicle.SpatialCargoItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * 2D spatial cargo grid for rockets supporting both standard {@code 1x1} items and
 * multi-slot {@link SpatialCargoItem} crates (e.g. {@code 2x2} SMALL, {@code 3x2} MEDIUM, {@code 4x3} LARGE).
 */
public class RocketCargoGrid implements Container {
    private final int columns;
    private final int rows;
    private final NonNullList<ItemStack> items;
    private final int[] ownerAnchor;

    public RocketCargoGrid(int columns, int rows) {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException("Grid dimensions must be positive");
        }
        this.columns = columns;
        this.rows = rows;
        int total = columns * rows;
        this.items = NonNullList.withSize(total, ItemStack.EMPTY);
        this.ownerAnchor = new int[total];
        Arrays.fill(this.ownerAnchor, -1);
    }

    public int getColumns() {
        return this.columns;
    }

    public int getRows() {
        return this.rows;
    }

    public int toIndex(int col, int row) {
        return row * this.columns + col;
    }

    public int colOf(int index) {
        return index % this.columns;
    }

    public int rowOf(int index) {
        return index / this.columns;
    }

    public int getOwnerAnchor(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.ownerAnchor.length) {
            return -1;
        }
        return this.ownerAnchor[slotIndex];
    }

    public boolean isAnchorSlot(int slotIndex) {
        return slotIndex >= 0
                && slotIndex < this.ownerAnchor.length
                && this.ownerAnchor[slotIndex] == slotIndex
                && !this.items.get(slotIndex).isEmpty();
    }

    public boolean isOccupied(int slotIndex) {
        return getOwnerAnchor(slotIndex) != -1;
    }

    public boolean canPlaceAt(int col, int row, @NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        int w = SpatialCargoItem.widthOf(stack);
        int h = SpatialCargoItem.heightOf(stack);
        if (col < 0 || row < 0 || col + w > this.columns || row + h > this.rows) {
            return false;
        }
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int idx = toIndex(col + dx, row + dy);
                if (this.ownerAnchor[idx] != -1) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean placeAt(int col, int row, @NotNull ItemStack stack) {
        if (!canPlaceAt(col, row, stack)) {
            return false;
        }
        int anchor = toIndex(col, row);
        int w = SpatialCargoItem.widthOf(stack);
        int h = SpatialCargoItem.heightOf(stack);
        this.items.set(anchor, stack.copy());
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                this.ownerAnchor[toIndex(col + dx, row + dy)] = anchor;
            }
        }
        setChanged();
        return true;
    }

    public boolean tryAutoInsert(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        int w = SpatialCargoItem.widthOf(stack);
        int h = SpatialCargoItem.heightOf(stack);
        // Merge into existing 1x1 stacks first
        if (w == 1 && h == 1 && stack.isStackable()) {
            for (int i = 0; i < this.items.size(); i++) {
                ItemStack existing = this.items.get(i);
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    int move = Math.min(space, stack.getCount());
                    existing.grow(move);
                    stack.shrink(move);
                    if (stack.isEmpty()) {
                        setChanged();
                        return true;
                    }
                }
            }
        }
        for (int r = 0; r <= this.rows - h; r++) {
            for (int c = 0; c <= this.columns - w; c++) {
                if (canPlaceAt(c, r, stack)) {
                    placeAt(c, r, stack);
                    stack.setCount(0);
                    return true;
                }
            }
        }
        return false;
    }

    public @NotNull ItemStack removeAt(int slotIndex) {
        int anchor = getOwnerAnchor(slotIndex);
        if (anchor < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(anchor);
        this.items.set(anchor, ItemStack.EMPTY);
        for (int i = 0; i < this.ownerAnchor.length; i++) {
            if (this.ownerAnchor[i] == anchor) {
                this.ownerAnchor[i] = -1;
            }
        }
        setChanged();
        return removed;
    }

    private void rebuildOccupancy() {
        Arrays.fill(this.ownerAnchor, -1);
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int col = colOf(i);
            int row = rowOf(i);
            int w = SpatialCargoItem.widthOf(stack);
            int h = SpatialCargoItem.heightOf(stack);
            for (int dy = 0; dy < h && row + dy < this.rows; dy++) {
                for (int dx = 0; dx < w && col + dx < this.columns; dx++) {
                    this.ownerAnchor[toIndex(col + dx, row + dy)] = i;
                }
            }
        }
    }

    public @NotNull CompoundTag saveToTag(@NotNull HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", i);
                list.add(stack.save(registries, entry));
            }
        }
        root.put("Items", list);
        return root;
    }

    public void loadFromTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        clearContent();
        if (!tag.contains("Items", Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < this.items.size()) {
                ItemStack.parse(registries, entry).ifPresent(stack -> this.items.set(slot, stack));
            }
        }
        rebuildOccupancy();
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot < 0 || slot >= this.items.size()) {
            return ItemStack.EMPTY;
        }
        return this.items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        int anchor = getOwnerAnchor(slot);
        if (anchor < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(anchor);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (amount >= stack.getCount() || SpatialCargoItem.widthOf(stack) > 1 || SpatialCargoItem.heightOf(stack) > 1) {
            return removeAt(anchor);
        }
        ItemStack split = stack.split(amount);
        if (stack.isEmpty()) {
            removeAt(anchor);
        }
        setChanged();
        return split;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return removeAt(slot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= this.items.size()) {
            return;
        }
        if (stack.isEmpty()) {
            removeAt(slot);
            return;
        }
        int col = colOf(slot);
        int row = rowOf(slot);
        // Clear existing occupant at target anchor before setting
        if (this.ownerAnchor[slot] == slot) {
            removeAt(slot);
        }
        if (canPlaceAt(col, row, stack)) {
            placeAt(col, row, stack);
        } else {
            this.items.set(slot, stack);
            rebuildOccupancy();
            setChanged();
        }
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        Arrays.fill(this.ownerAnchor, -1);
    }
}
