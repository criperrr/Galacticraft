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

import com.mojang.datafixers.util.Pair;
import dev.galacticraft.machinelib.api.block.entity.BasicRecipeMachineBlockEntity;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.machine.configuration.IOFace;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.menu.RecipeMachineMenu;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.machine.storage.VariableSizedContainer;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.recipe.PartAssemblerRecipe;
import dev.galacticraft.mod.recipe.PartAssemblerRecipeInput;
import dev.galacticraft.mod.screen.GCMenuTypes;
import dev.galacticraft.mod.tag.GCItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RocketWorkbenchBlockEntity extends BasicRecipeMachineBlockEntity<PartAssemblerRecipeInput, PartAssemblerRecipe> implements VariableSizedContainer.Listener {
    public static final int CHARGE_SLOT = 0;
    public static final int SCHEMATIC_SLOT = 1;
    public static final int INPUT_GRID_START = 2;
    public static final int INPUT_GRID_LEN = 9;
    public static final int OUTPUT_SLOT = 11;
    public static final int BYPRODUCT_SLOT = 12;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION_RATE = 120L;

    private int clientProgress = 0;
    private int clientMaxProgress = 0;
    private int lastSyncedProgress = -1;
    public float animationBlend = 0.0F;

    public static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.builder()
                    // Slot 0: Charge / Battery
                    .add(ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 62)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY))
                    )
                    // Slot 1: Schematic Blueprint (NON-CONSUMED, sits outside inputSlots)
                    .add(ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(104, 53)
                            .capacity(1)
                            .filter(ResourceFilters.itemTag(GCItemTags.ROCKET_SCHEMATICS))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ROCKET_CONE))
                    )
                    // Slots 2..10: 3x3 Component Crafting Grid
                    .add3x3Grid(TransferType.INPUT, 30, 17)
                    // Slot 11: Manufactured Rocket Part Output
                    .add(ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(148, 22)
                            .capacity(1)
                    )
                    // Slot 12: Scrap Metal Byproduct Output
                    .add(ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(148, 48)
                            .capacity(64)
                    ),
            MachineEnergyStorage.spec(ENERGY_CAPACITY, ENERGY_CONSUMPTION_RATE * 4, 0)
    );

    // ==========================================
    // Legacy caller compatibility bridge members
    // ==========================================
    @Deprecated
    public final SimpleContainer output = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return false;
        }
    };

    @Deprecated
    public final SimpleContainer chests = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof TntBlock) {
                return true;
            }
            return stack.is(GCItemTags.ROCKET_STORAGE_UPGRADE_ITEMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };

    @Deprecated
    public final VariableSizedContainer ingredients = new VariableSizedContainer(14);

    public RocketWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.ROCKET_WORKBENCH, pos, state, GCRecipes.PART_ASSEMBLER_TYPE, SPEC,
                INPUT_GRID_START, INPUT_GRID_LEN, OUTPUT_SLOT, 2);
        this.ingredients.addListener(this);
        this.chests.addListener(sender -> this.setChanged());
        byte defaultFace = IOFace.pack(ResourceType.ANY, ResourceFlow.BOTH);
        for (BlockFace face : BlockFace.values()) {
            this.getIOConfig().get(face).readTag(ByteTag.valueOf(defaultFace));
        }
    }

    @Deprecated
    public void resizeInventory(int newSize) {
        if (newSize >= this.ingredients.getTargetSize()) {
            this.ingredients.resize(newSize);
        } else {
            this.ingredients.resize(newSize);
        }
    }

    @Override
    protected @NotNull PartAssemblerRecipeInput craftingInv() {
        return new PartAssemblerRecipeInput(
                this.itemStorage().getItem(SCHEMATIC_SLOT),
                this.itemStorage().subStorage(INPUT_GRID_START, INPUT_GRID_LEN)
        );
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
        int currentProgress = this.getProgress();
        if (currentProgress != this.lastSyncedProgress && (currentProgress == 0 || currentProgress == 1 || currentProgress % 5 == 0)) {
            this.lastSyncedProgress = currentProgress;
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected @NotNull MachineStatus workingStatus(RecipeHolder<PartAssemblerRecipe> recipe) {
        return GCMachineStatuses.FABRICATING;
    }

    @Override
    protected @Nullable MachineStatus hasResourcesToWork() {
        return this.energyStorage().canExtract(ENERGY_CONSUMPTION_RATE) ? null : MachineStatuses.NOT_ENOUGH_ENERGY;
    }

    @Override
    protected void extractResourcesToWork() {
        this.energyStorage().extract(ENERGY_CONSUMPTION_RATE);
    }

    @Override
    protected void outputStacks(@NotNull RecipeHolder<PartAssemblerRecipe> recipe) {
        ItemStack assembled = recipe.value().assemble(this.craftingInv(), this.level.registryAccess());
        this.itemStorage().slot(OUTPUT_SLOT).insert(assembled.getItem(), assembled.getComponentsPatch(), assembled.getCount());

        ItemStack byproduct = recipe.value().getScrapByproduct();
        if (!byproduct.isEmpty()) {
            this.itemStorage().slot(BYPRODUCT_SLOT).insert(byproduct.getItem(), byproduct.getComponentsPatch(), byproduct.getCount());
        }
    }

    @Override
    protected boolean canOutputStacks(@NotNull RecipeHolder<PartAssemblerRecipe> recipe) {
        ItemStack assembled = recipe.value().assemble(this.craftingInv(), this.level.registryAccess());
        boolean canFitOutput = this.itemStorage().slot(OUTPUT_SLOT).canInsert(assembled.getItem(), assembled.getComponentsPatch(), assembled.getCount());

        ItemStack byproduct = recipe.value().getScrapByproduct();
        boolean canFitByproduct = byproduct.isEmpty() || this.itemStorage().slot(BYPRODUCT_SLOT).canInsert(byproduct.getItem(), byproduct.getComponentsPatch(), byproduct.getCount());

        return canFitOutput && canFitByproduct;
    }

    @Override
    public int getProcessingTime(@NotNull RecipeHolder<PartAssemblerRecipe> recipe) {
        return recipe.value().getAssemblyTime();
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inventory, Player player) {
        return new RecipeMachineMenu<>(
                GCMenuTypes.PART_ASSEMBLER,
                syncId,
                player,
                this
        );
    }

    @Override
    public void onSizeChanged() {
        this.setChanged();
    }

    @Override
    public void onItemChanged() {
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registryLookup) {
        super.saveAdditional(tag, registryLookup);

        ListTag outputTag = this.output.createTag(registryLookup);
        if (!outputTag.isEmpty()) tag.put("Output", outputTag.getFirst());
        tag.put("Inventory", this.ingredients.toTag(registryLookup));
        tag.put("Chests", this.chests.createTag(registryLookup));
    }

    @Override
    public void populateUpdateTag(CompoundTag tag) {
        super.populateUpdateTag(tag);
        tag.putInt("Progress", this.getProgress());
        RecipeHolder<PartAssemblerRecipe> recipe = this.getActiveRecipe();
        tag.putInt("MaxProgress", recipe != null ? this.getProcessingTime(recipe) : 0);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registryLookup) {
        super.loadAdditional(tag, registryLookup);
        if (tag.contains("Output")) {
            ListTag list = new ListTag();
            list.add(tag.get("Output"));
            this.output.fromTag(list, registryLookup);
        }
        if (tag.contains("Inventory")) {
            this.ingredients.readTag(tag.getCompound("Inventory"), registryLookup);
        }
        if (tag.contains("Chests")) {
            this.chests.fromTag(tag.getList("Chests", Tag.TAG_COMPOUND), registryLookup);
        }
        if (tag.contains("Progress")) {
            this.clientProgress = tag.getInt("Progress");
        }
        if (tag.contains("MaxProgress")) {
            this.clientMaxProgress = tag.getInt("MaxProgress");
        }
    }

    public float getClientProgressRatio(float tickDelta) {
        if (this.level != null && !this.level.isClientSide) {
            RecipeHolder<PartAssemblerRecipe> recipe = this.getActiveRecipe();
            int max = recipe != null ? this.getProcessingTime(recipe) : 0;
            return max > 0 ? (float) this.getProgress() / (float) max : 0.0F;
        }
        if (this.clientMaxProgress > 0) {
            return Mth.clamp((this.clientProgress + tickDelta) / (float) this.clientMaxProgress, 0.0F, 1.0F);
        }
        return 0.0F;
    }

    public int getClientProgress() {
        return this.clientProgress;
    }

    public int getClientMaxProgress() {
        return this.clientMaxProgress;
    }

    public void setClientProgress(int clientProgress, int clientMaxProgress) {
        this.clientProgress = clientProgress;
        this.clientMaxProgress = clientMaxProgress;
    }
}
