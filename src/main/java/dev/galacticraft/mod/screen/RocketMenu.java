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

package dev.galacticraft.mod.screen;

import dev.galacticraft.api.vehicle.SpatialCargoItem;
import dev.galacticraft.mod.content.entity.vehicle.RocketEntity;
import dev.galacticraft.mod.content.vehicle.RocketCargoGrid;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RocketMenu extends AbstractContainerMenu {
    public static final int CARGO_START_X = 8;
    public static final int CARGO_START_Y = 52;

    public final Player player;
    public final RocketEntity rocket;
    public final RocketCargoGrid cargoGrid;
    public final int cargoSlotCount;

    protected RocketMenu(int syncId, Inventory playerInventory, int id) {
        this(syncId, playerInventory, playerInventory.player, (RocketEntity) playerInventory.player.level().getEntity(id));
    }

    public RocketMenu(int syncId, Inventory playerInventory, Player player, RocketEntity rocket) {
        super(GCMenuTypes.ROCKET, syncId);

        this.player = player;
        this.rocket = rocket;
        this.cargoGrid = rocket.getCargoGrid();
        this.cargoSlotCount = this.cargoGrid.getContainerSize();

        // Spatial 2D Rocket Cargo Grid (9x4 = 36 slots, supports 2x2 / 3x2 / 4x3 vehicle crates)
        for (int row = 0; row < this.cargoGrid.getRows(); row++) {
            for (int col = 0; col < this.cargoGrid.getColumns(); col++) {
                int idx = this.cargoGrid.toIndex(col, row);
                this.addSlot(new Slot(this.cargoGrid, idx, CARGO_START_X + col * 18, CARGO_START_Y + row * 18));
            }
        }

        // Player main inv
        for (int slotY = 0; slotY < 3; ++slotY) {
            for (int slotX = 0; slotX < 9; ++slotX) {
                this.addSlot(new Slot(playerInventory, slotX + (slotY + 1) * 9, 8 + slotX * 18, 134 + slotY * 18));
            }
        }

        // Player hotbar
        for (int slotY = 0; slotY < 9; ++slotY) {
            this.addSlot(new Slot(playerInventory, slotY, 8 + slotY * 18, 192));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.PICKUP && slotId >= 0 && slotId < this.cargoSlotCount) {
            ItemStack carried = this.getCarried();
            if (carried.isEmpty()) {
                ItemStack removed = this.cargoGrid.removeAt(slotId);
                if (!removed.isEmpty()) {
                    this.setCarried(removed);
                    this.broadcastChanges();
                }
                return;
            }
            int col = this.cargoGrid.colOf(slotId);
            int row = this.cargoGrid.rowOf(slotId);
            if (this.cargoGrid.canPlaceAt(col, row, carried)) {
                int w = SpatialCargoItem.widthOf(carried);
                int h = SpatialCargoItem.heightOf(carried);
                if (button == 1 && w == 1 && h == 1 && carried.getCount() > 1) {
                    ItemStack single = carried.split(1);
                    this.cargoGrid.placeAt(col, row, single);
                } else {
                    this.cargoGrid.placeAt(col, row, carried);
                    this.setCarried(ItemStack.EMPTY);
                }
                this.broadcastChanges();
                return;
            }
            // Allow merging stackable 1x1 items onto matching anchor slot
            int anchor = this.cargoGrid.getOwnerAnchor(slotId);
            if (anchor >= 0 && SpatialCargoItem.widthOf(carried) == 1 && SpatialCargoItem.heightOf(carried) == 1) {
                ItemStack existing = this.cargoGrid.getItem(anchor);
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, carried) && existing.isStackable()) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    if (space > 0) {
                        int move = button == 1 ? 1 : Math.min(space, carried.getCount());
                        existing.grow(move);
                        carried.shrink(move);
                        this.broadcastChanges();
                    }
                }
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        if (index < this.cargoSlotCount) {
            int anchor = this.cargoGrid.getOwnerAnchor(index);
            if (anchor < 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = this.cargoGrid.getItem(anchor);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack copy = stack.copy();
            if (!this.moveItemStackTo(stack, this.cargoSlotCount, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                this.cargoGrid.removeAt(anchor);
            }
            this.broadcastChanges();
            return copy;
        } else {
            Slot slot = this.slots.get(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack copy = stack.copy();
            if (this.cargoGrid.tryAutoInsert(stack)) {
                slot.setChanged();
                this.broadcastChanges();
                return copy;
            }
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getVehicle() instanceof RocketEntity;
    }
}
