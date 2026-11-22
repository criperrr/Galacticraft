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

import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Container menu binding a player session to a {@link HangarTerminalBlockEntity}.
 *
 * <p>When the launch platform is formed, exposes 8 rocket assembly slots (ordered bottom-to-top:
 * slot 0 = Base -> slot 7 = Top) and the player's inventory so rockets can be assembled directly
 * from the terminal menu without placing multi-block parts by hand.
 */
public class HangarTerminalMenu extends AbstractContainerMenu {
    public static final int ROCKET_SLOT_START = 0;
    public static final int ROCKET_SLOT_END = HangarTerminalBlockEntity.ROCKET_SLOT_COUNT;
    public static final int PLAYER_INV_START = ROCKET_SLOT_END;
    public static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    public static final int PLAYER_HOTBAR_END = PLAYER_INV_END + 9;

    // Slot coordinates matching HangarTerminalScreen's formed-mode layout (284x248 panel)
    public static final int ROCKET_GRID_X = 16;
    public static final int ROCKET_GRID_Y = 42;
    public static final int PLAYER_INV_X = 61;
    public static final int PLAYER_INV_Y = 166;
    public static final int PLAYER_HOTBAR_Y = 224;

    public final HangarTerminalBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public HangarTerminalMenu(int syncId, Inventory playerInventory, HangarTerminalOpeningData data) {
        this(syncId, playerInventory, resolveBlockEntity(playerInventory, data));
    }

    public HangarTerminalMenu(int syncId, Inventory playerInventory, HangarTerminalBlockEntity blockEntity) {
        super(GCMenuTypes.HANGAR_TERMINAL, syncId);
        this.blockEntity = blockEntity;
        Level level = blockEntity.getLevel();
        this.access = level != null
                ? ContainerLevelAccess.create(level, blockEntity.getBlockPos())
                : ContainerLevelAccess.NULL;

        // 8 rocket staging slots (2 rows x 4 columns, bottom-to-top: 0..7)
        Container rocketSlots = blockEntity.getRocketSlots();
        for (int i = 0; i < HangarTerminalBlockEntity.ROCKET_SLOT_COUNT; i++) {
            int col = i % 4;
            int row = i / 4;
            int sx = ROCKET_GRID_X + col * 20;
            int sy = ROCKET_GRID_Y + row * 20;
            this.addSlot(new RocketPartSlot(rocketSlots, i, sx, sy, blockEntity));
        }

        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = PLAYER_INV_X + col * 18;
                int sy = PLAYER_INV_Y + row * 18;
                this.addSlot(new FormedOnlySlot(playerInventory, col + row * 9 + 9, sx, sy, blockEntity));
            }
        }

        // Player hotbar (1x9)
        for (int col = 0; col < 9; col++) {
            int sx = PLAYER_INV_X + col * 18;
            this.addSlot(new FormedOnlySlot(playerInventory, col, sx, PLAYER_HOTBAR_Y, blockEntity));
        }
    }

    private static HangarTerminalBlockEntity resolveBlockEntity(Inventory playerInventory, HangarTerminalOpeningData data) {
        Level level = playerInventory.player.level();
        BlockEntity be = level.getBlockEntity(data.pos());
        if (be instanceof HangarTerminalBlockEntity terminal) {
            return terminal;
        }
        throw new IllegalStateException("Expected HangarTerminalBlockEntity at " + data.pos() + ", found " + be);
    }

    public HangarTerminalBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (!this.blockEntity.isFormed()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (slotIndex < ROCKET_SLOT_END) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.getItem() instanceof ModularRocketPartBlockItem) {
                if (!this.moveItemStackTo(stack, ROCKET_SLOT_START, ROCKET_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < PLAYER_INV_END) {
                if (!this.moveItemStackTo(stack, PLAYER_INV_END, PLAYER_HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.blockEntity.isRemoved()
                && stillValid(this.access, player, GCBlocks.HANGAR_TERMINAL);
    }

    private static class FormedOnlySlot extends Slot {
        protected final HangarTerminalBlockEntity terminal;

        FormedOnlySlot(Container container, int slot, int x, int y, HangarTerminalBlockEntity terminal) {
            super(container, slot, x, y);
            this.terminal = terminal;
        }

        @Override
        public boolean isActive() {
            // The KSP-style VAB screen reads directly from the player's inventory without fixed slots.
            return false;
        }
    }

    private static final class RocketPartSlot extends FormedOnlySlot {
        RocketPartSlot(Container container, int slot, int x, int y, HangarTerminalBlockEntity terminal) {
            super(container, slot, x, y, terminal);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.isActive() && !stack.isEmpty() && stack.getItem() instanceof ModularRocketPartBlockItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
