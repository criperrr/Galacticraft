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

import dev.galacticraft.api.vehicle.VehicleBenchCategory;
import dev.galacticraft.mod.content.block.entity.VehiclePartFabricatorBlockEntity;
import dev.galacticraft.mod.content.block.machine.VehiclePartFabricatorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VehiclePartFabricatorMenu extends AbstractContainerMenu {
    public final BlockPos blockPos;
    private final @Nullable VehiclePartFabricatorBlockEntity blockEntity;
    private final Container container;
    private final VehicleBenchCategory benchCategory;

    public VehiclePartFabricatorMenu(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, pos,
                playerInventory.player.level().getBlockEntity(pos) instanceof VehiclePartFabricatorBlockEntity be ? be : null);
    }

    public VehiclePartFabricatorMenu(int syncId, Inventory playerInventory, VehiclePartFabricatorBlockEntity blockEntity) {
        this(syncId, playerInventory, blockEntity.getBlockPos(), blockEntity);
    }

    private VehiclePartFabricatorMenu(int syncId, Inventory playerInventory, BlockPos pos, @Nullable VehiclePartFabricatorBlockEntity blockEntity) {
        super(GCMenuTypes.VEHICLE_PART_FABRICATOR, syncId);
        this.blockPos = pos;
        this.blockEntity = blockEntity;
        this.container = blockEntity != null ? blockEntity.getInventory() : new SimpleContainer(VehiclePartFabricatorBlockEntity.TOTAL_SLOTS);
        if (blockEntity != null) {
            this.benchCategory = blockEntity.getCategory();
        } else if (playerInventory.player.level().getBlockState(pos).getBlock() instanceof VehiclePartFabricatorBlock bench) {
            this.benchCategory = bench.getCategory();
        } else {
            this.benchCategory = VehicleBenchCategory.UNIVERSAL;
        }

        // 4 Material input slots (right-side work bay)
        for (int i = 0; i < VehiclePartFabricatorBlockEntity.INPUT_SLOTS; i++) {
            this.addSlot(new Slot(this.container, i, 114 + i * 20, 102));
        }

        // 1 Output slot (right edge)
        this.addSlot(new Slot(this.container, VehiclePartFabricatorBlockEntity.OUTPUT_SLOT, 208, 102) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory (centered in 236px aerospace console)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 38 + col * 18, 134 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 38 + col * 18, 192));
        }
    }

    public @NotNull VehicleBenchCategory getBenchCategory() {
        return this.benchCategory;
    }

    public Container getFabricatorContainer() {
        return this.container;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < VehiclePartFabricatorBlockEntity.TOTAL_SLOTS) {
            if (!this.moveItemStackTo(stack, VehiclePartFabricatorBlockEntity.TOTAL_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, VehiclePartFabricatorBlockEntity.INPUT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity == null || player.distanceToSqr(this.blockPos.getX() + 0.5, this.blockPos.getY() + 0.5, this.blockPos.getZ() + 0.5) <= 64.0;
    }
}
