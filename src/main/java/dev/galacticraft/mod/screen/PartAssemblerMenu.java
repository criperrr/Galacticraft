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

import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.machinelib.api.menu.MenuData;
import dev.galacticraft.machinelib.api.menu.RecipeMachineMenu;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.impl.compat.vanilla.StorageSlot;
import dev.galacticraft.mod.content.block.entity.RocketWorkbenchBlockEntity;
import dev.galacticraft.mod.content.rocket.part.SchematicGridLayout;
import dev.galacticraft.mod.recipe.PartAssemblerRecipe;
import dev.galacticraft.mod.recipe.PartAssemblerRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Custom {@link RecipeMachineMenu} for the Rocket Part Builder supporting both legacy 3x3
 * {@link PartAssemblerRecipe} crafting and Tinkers'-style dynamic schematic silhouettes backed
 * by fixed-index {@link DynamicSchematicSlot}s.
 */
public class PartAssemblerMenu extends RecipeMachineMenu<PartAssemblerRecipeInput, PartAssemblerRecipe, RocketWorkbenchBlockEntity> {
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;

    private int syncedEffectiveProgress;
    private int syncedEffectiveMaxProgress;

    public PartAssemblerMenu(int syncId, @NotNull Inventory inventory, @NotNull RocketWorkbenchBlockEntity machine) {
        super(GCMenuTypes.PART_ASSEMBLER, syncId, inventory.player, machine);
        this.refreshDynamicSlots();
    }

    public PartAssemblerMenu(int syncId, @NotNull Inventory inventory, @NotNull BlockPos pos) {
        super(GCMenuTypes.PART_ASSEMBLER, syncId, inventory, pos, PLAYER_INV_X, PLAYER_INV_Y);
        this.refreshDynamicSlots();
    }

    @Override
    protected void generateSlots(MachineItemStorage storage) {
        ItemResourceSlot[] resourceSlots = storage.getSlots();
        for (int i = 0; i < resourceSlots.length; i++) {
            ItemResourceSlot resourceSlot = resourceSlots[i];
            if (resourceSlot.getDisplay() != null) {
                this.internalSlots++;
                if (i >= RocketWorkbenchBlockEntity.INPUT_GRID_START
                        && i < RocketWorkbenchBlockEntity.INPUT_GRID_START + RocketWorkbenchBlockEntity.INPUT_GRID_LEN) {
                    int gridIndex = i - RocketWorkbenchBlockEntity.INPUT_GRID_START;
                    this.addSlot(new DynamicSchematicSlot(
                            storage,
                            resourceSlot,
                            resourceSlot.getDisplay(),
                            i,
                            this.player,
                            gridIndex,
                            this::getActiveGridLayout
                    ));
                } else {
                    this.addSlot(new StorageSlot(storage, resourceSlot, resourceSlot.getDisplay(), i, this.player));
                }
            }
        }
    }

    @Override
    public void registerData(@NotNull MenuData data) {
        super.registerData(data);
        data.registerInt(this.be::getEffectiveProgress, v -> {
            this.syncedEffectiveProgress = v;
            this.be.setClientProgress(v, this.syncedEffectiveMaxProgress);
        });
        data.registerInt(this.be::getEffectiveMaxProgress, v -> {
            this.syncedEffectiveMaxProgress = v;
            this.be.setClientProgress(this.syncedEffectiveProgress, v);
        });
        data.register(ByteBufCodecs.STRING_UTF8, this.be::getSelectedFamilyId, this.be::setClientSelectedFamilyId);
        data.registerInt(this.be::getSelectedDiameter, this.be::setClientSelectedDiameter);
        data.registerInt(this.be::getSelectedTopDiameter, this.be::setClientSelectedTopDiameter);
        data.registerInt(this.be::getSelectedHeight, this.be::setClientSelectedHeight);
        data.registerEnum(RocketLivery.values(), this.be::getSelectedLivery, this.be::setClientSelectedLivery);
    }

    public @NotNull SchematicGridLayout getActiveGridLayout() {
        if (this.be == null) {
            return SchematicGridLayout.DEFAULT_3X3;
        }
        return this.be.getActiveGridLayout();
    }

    public int getEffectiveProgress() {
        if (this.be != null && this.be.getActiveSchematicCategory() != null) {
            return this.syncedEffectiveProgress;
        }
        return this.getProgress();
    }

    public int getEffectiveMaxProgress() {
        if (this.be != null && this.be.getActiveSchematicCategory() != null) {
            return this.syncedEffectiveMaxProgress;
        }
        return this.getMaxProgress();
    }

    /**
     * Updates the screen coordinates of all 9 {@link DynamicSchematicSlot}s to match the current
     * schematic in Slot 1 without mutating {@link #slots} membership or indices, and returns any
     * items left in newly inactive slots back to the player on the server.
     */
    public void refreshDynamicSlots() {
        int end = Math.min(this.slots.size(), RocketWorkbenchBlockEntity.INPUT_GRID_START + RocketWorkbenchBlockEntity.INPUT_GRID_LEN);
        for (int i = RocketWorkbenchBlockEntity.INPUT_GRID_START; i < end; i++) {
            if (this.slots.get(i) instanceof DynamicSchematicSlot dynamicSlot) {
                dynamicSlot.updateLayoutPosition();
            }
        }
        if (this.be != null && this.player != null && !this.player.level().isClientSide) {
            this.be.ejectInactiveGridItems(this.player);
        }
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        super.clicked(slotId, button, clickType, player);
        this.refreshDynamicSlots();
    }

    @Override
    public void broadcastChanges() {
        this.refreshDynamicSlots();
        super.broadcastChanges();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        this.refreshDynamicSlots();
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        // Legacy 3x3 mode or moving from machine slots into player inventory: preserve exact MachineLib behavior.
        if (index < this.internalSlots || !this.getActiveGridLayout().isModular()) {
            Slot clicked = this.slots.get(index);
            if (clicked instanceof DynamicSchematicSlot dynamicSlot && !dynamicSlot.isActive()) {
                return ItemStack.EMPTY;
            }
            return super.quickMoveStack(player, index);
        }

        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = sourceSlot.getItem();
        ItemStack originalCopy = stack.copy();
        long remaining = originalCopy.getCount();
        long initial = remaining;

        // Pass 1: Merge into existing matching active machine slots.
        for (int i = 0; i < this.internalSlots; i++) {
            Slot targetSlot = this.slots.get(i);
            if (!(targetSlot instanceof StorageSlot storageSlot) || !targetSlot.isActive() || !targetSlot.mayPlace(originalCopy)) {
                continue;
            }
            ItemResourceSlot wrapped = storageSlot.getWrapped();
            if (wrapped.transferMode().playerInsertion()
                    && wrapped.contains(originalCopy.getItem(), originalCopy.getComponentsPatch())) {
                remaining -= wrapped.insert(originalCopy.getItem(), originalCopy.getComponentsPatch(), remaining);
                if (remaining == 0L) {
                    sourceSlot.setByPlayer(ItemStack.EMPTY);
                    return ItemStack.EMPTY;
                }
            }
        }

        // Pass 2: Insert into empty active machine slots that accept the item.
        for (int i = 0; i < this.internalSlots; i++) {
            Slot targetSlot = this.slots.get(i);
            if (!(targetSlot instanceof StorageSlot storageSlot) || !targetSlot.isActive() || !targetSlot.mayPlace(originalCopy)) {
                continue;
            }
            ItemResourceSlot wrapped = storageSlot.getWrapped();
            remaining -= wrapped.insert(originalCopy.getItem(), originalCopy.getComponentsPatch(), remaining);
            if (remaining == 0L) {
                sourceSlot.setByPlayer(ItemStack.EMPTY);
                return ItemStack.EMPTY;
            }
        }

        if (remaining == initial) {
            int totalSlots = this.slots.size();
            int hotbarStart = totalSlots - 9;
            if (index >= this.internalSlots && index < hotbarStart) {
                if (!this.moveItemStackTo(stack, hotbarStart, totalSlots, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= hotbarStart && index < totalSlots) {
                if (!this.moveItemStackTo(stack, this.internalSlots, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else {
            stack.setCount((int) remaining);
        }

        if (stack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        if (stack.getCount() == originalCopy.getCount() && remaining == initial) {
            return ItemStack.EMPTY;
        }
        return originalCopy;
    }
}
