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

package dev.galacticraft.mod.content.block.entity;

import dev.galacticraft.api.vehicle.VehicleBenchCategory;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.block.machine.VehiclePartFabricatorBlock;
import dev.galacticraft.mod.content.vehicle.VehicleFabricatorRecipes;
import dev.galacticraft.mod.screen.VehiclePartFabricatorMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class VehiclePartFabricatorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOT = 4;
    public static final int TOTAL_SLOTS = 5;

    private final SimpleContainer inventory = new SimpleContainer(TOTAL_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            VehiclePartFabricatorBlockEntity.this.setChanged();
        }
    };

    public VehiclePartFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.VEHICLE_PART_FABRICATOR, pos, state);
    }

    public @NotNull VehicleBenchCategory getCategory() {
        if (this.getBlockState().getBlock() instanceof VehiclePartFabricatorBlock bench) {
            return bench.getCategory();
        }
        return VehicleBenchCategory.UNIVERSAL;
    }

    public @NotNull SimpleContainer getInventory() {
        return this.inventory;
    }

    public boolean tryFabricate(@NotNull Player player, @NotNull ResourceLocation blueprintId) {
        VehicleFabricatorRecipes.Blueprint bp = VehicleFabricatorRecipes.find(blueprintId);
        if (bp == null || !VehicleFabricatorRecipes.isAllowedOn(bp, this.getCategory())) {
            return false;
        }
        ItemStack result = bp.resultSupplier().get();
        ItemStack currentOut = this.inventory.getItem(OUTPUT_SLOT);
        if (!currentOut.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(currentOut, result)
                    || currentOut.getCount() + result.getCount() > currentOut.getMaxStackSize()) {
                return false;
            }
        }
        boolean freeCreative = player.getAbilities().instabuild;
        if (!freeCreative && !VehicleFabricatorRecipes.consumeMaterials(this.inventory, INPUT_SLOTS, bp)) {
            return false;
        }
        if (currentOut.isEmpty()) {
            this.inventory.setItem(OUTPUT_SLOT, result);
        } else {
            currentOut.grow(result.getCount());
            this.inventory.setChanged();
        }
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                list.add(stack.save(registries, itemTag));
            }
        }
        tag.put("Items", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.clearContent();
        if (tag.contains("Items", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                    ItemStack.parse(registries, itemTag).ifPresent(stack -> this.inventory.setItem(slot, stack));
                }
            }
        }
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.worldPosition;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new VehiclePartFabricatorMenu(syncId, playerInventory, this);
    }
}
