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
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.machinelib.api.block.entity.BasicRecipeMachineBlockEntity;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.machine.configuration.IOFace;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.SlottedStorageAccess;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.PartCategorySchematicItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.CostProfile;
import dev.galacticraft.mod.content.rocket.part.RocketPartFamily;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.part.SchematicGridLayout;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.machine.storage.VariableSizedContainer;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.recipe.PartAssemblerRecipe;
import dev.galacticraft.mod.recipe.PartAssemblerRecipeInput;
import dev.galacticraft.mod.screen.PartAssemblerMenu;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    private String selectedFamilyId = "";
    private int selectedDiameter = 1;
    private int selectedTopDiameter = 1;
    private int selectedHeight = 1;
    private RocketLivery selectedLivery = RocketLivery.WHITE;
    private int modularProgress = 0;
    private int modularMaxProgress = 0;
    private @Nullable String lastModularModelId = null;

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
        if (this.getActiveSchematicCategory() != null && !this.getState().isActive() && this.modularProgress > 0) {
            this.modularProgress = Math.max(0, this.modularProgress - this.decreaseProgressAmount());
        }
        int currentProgress = this.getEffectiveProgress();
        if (currentProgress != this.lastSyncedProgress && (currentProgress == 0 || currentProgress == 1 || currentProgress % 5 == 0)) {
            this.lastSyncedProgress = currentProgress;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        RocketPartCategory modularCategory = this.getActiveSchematicCategory();
        if (modularCategory == null) {
            if (this.modularProgress != 0 || this.modularMaxProgress != 0 || this.lastModularModelId != null) {
                this.modularProgress = 0;
                this.modularMaxProgress = 0;
                this.lastModularModelId = null;
            }
            return super.tick(level, pos, state, profiler);
        }

        if (this.getProgress() != 0 || this.getActiveRecipe() != null) {
            this.setActiveRecipe(null);
            this.setProgress(0);
        }
        return this.tickModularFabrication(profiler);
    }

    private @NotNull MachineStatus tickModularFabrication(@NotNull ProfilerFiller profiler) {
        profiler.push("modular_config");
        RocketPartConfig config = this.getResolvedModularConfig();
        profiler.pop();
        if (config == null) {
            this.modularProgress = 0;
            this.modularMaxProgress = 0;
            this.lastModularModelId = null;
            return MachineStatuses.INVALID_RECIPE;
        }

        if (!config.modelId().equals(this.lastModularModelId)) {
            this.lastModularModelId = config.modelId();
            this.modularProgress = 0;
        }

        CostProfile cost = RocketPartCostCalculator.calculate(config);
        this.modularMaxProgress = cost.assemblyTimeTicks();

        SchematicGridLayout layout = SchematicGridLayout.forCategory(config.category());
        SlottedStorageAccess<Item, ItemResourceSlot> gridStorage = this.itemStorage().subStorage(INPUT_GRID_START, INPUT_GRID_LEN);

        profiler.push("modular_match");
        boolean gridMatches = layout.matchesGrid(gridStorage, cost.slotItemMultiplier());
        profiler.pop();
        if (!gridMatches) {
            this.modularProgress = 0;
            return MachineStatuses.INVALID_RECIPE;
        }

        ItemStack assembled = createModularOutputStack(config, cost);
        ItemStack scrap = new ItemStack(GCItems.ALUMINUM_NUGGET, cost.scrapCount());
        boolean canFitOutput = this.itemStorage().slot(OUTPUT_SLOT)
                .canInsert(assembled.getItem(), assembled.getComponentsPatch(), assembled.getCount());
        boolean canFitByproduct = scrap.isEmpty() || this.itemStorage().slot(BYPRODUCT_SLOT)
                .canInsert(scrap.getItem(), scrap.getComponentsPatch(), scrap.getCount());
        if (!canFitOutput || !canFitByproduct) {
            this.modularProgress = 0;
            return MachineStatuses.OUTPUT_FULL;
        }

        if (!this.energyStorage().canExtract(ENERGY_CONSUMPTION_RATE)) {
            return MachineStatuses.NOT_ENOUGH_ENERGY;
        }

        profiler.push("modular_working");
        this.energyStorage().extract(ENERGY_CONSUMPTION_RATE);
        this.modularProgress++;
        if (this.modularProgress >= cost.assemblyTimeTicks()) {
            layout.consumeGrid(gridStorage, cost.slotItemMultiplier());
            this.itemStorage().slot(OUTPUT_SLOT)
                    .insert(assembled.getItem(), assembled.getComponentsPatch(), assembled.getCount());
            if (!scrap.isEmpty()) {
                this.itemStorage().slot(BYPRODUCT_SLOT)
                        .insert(scrap.getItem(), scrap.getComponentsPatch(), scrap.getCount());
            }
            this.modularProgress = 0;
            this.setChanged();
        }
        profiler.pop();
        return GCMachineStatuses.FABRICATING;
    }

    private static @NotNull ItemStack createModularOutputStack(@NotNull RocketPartConfig config, @NotNull CostProfile cost) {
        ItemStack stack = new ItemStack(ModularRocketPartBlock.getBlockForCategory(config.category()));
        stack.set(GCDataComponents.ROCKET_PART_CONFIG, config);
        stack.set(GCDataComponents.ROCKET_PART_STATS, cost.stats());
        stack.set(GCDataComponents.PART_INTEGRITY, PartIntegrity.pristine(100.0));
        return stack;
    }

    public @Nullable RocketPartCategory getActiveSchematicCategory() {
        ItemStack schematic = this.itemStorage().getItem(SCHEMATIC_SLOT);
        if (!schematic.isEmpty() && schematic.getItem() instanceof PartCategorySchematicItem categorySchematic) {
            return categorySchematic.getCategory();
        }
        return null;
    }

    public @NotNull SchematicGridLayout getActiveGridLayout() {
        return SchematicGridLayout.forSchematic(this.itemStorage().getItem(SCHEMATIC_SLOT));
    }

    /**
     * Ejects any items currently residing in crafting grid slots that are hidden/inactive under
     * the active modular schematic layout, returning them to {@code player}'s inventory (or
     * dropping them in-world if {@code player} is {@code null}).
     *
     * @return the number of non-empty stacks ejected.
     */
    public int ejectInactiveGridItems(@Nullable Player player) {
        if (this.level == null || this.level.isClientSide) {
            return 0;
        }
        SchematicGridLayout layout = this.getActiveGridLayout();
        if (!layout.isModular()) {
            return 0;
        }
        int ejectedStacks = 0;
        for (int gridIndex = 0; gridIndex < INPUT_GRID_LEN; gridIndex++) {
            if (layout.isSlotActive(gridIndex)) {
                continue;
            }
            ItemResourceSlot slot = this.itemStorage().slot(INPUT_GRID_START + gridIndex);
            if (slot.isEmpty()) {
                continue;
            }
            ItemStack current = this.itemStorage().getItem(INPUT_GRID_START + gridIndex);
            if (current.isEmpty()) {
                continue;
            }
            ItemStack copy = current.copy();
            long extracted = slot.extract(copy.getItem(), copy.getComponentsPatch(), copy.getCount());
            if (extracted > 0L) {
                ItemStack toReturn = copy.copyWithCount((int) extracted);
                if (player != null) {
                    player.getInventory().placeItemBackInInventory(toReturn);
                } else {
                    Block.popResource(this.level, this.worldPosition.above(), toReturn);
                }
                ejectedStacks++;
            }
        }
        if (ejectedStacks > 0) {
            this.setChanged();
        }
        return ejectedStacks;
    }

    public @Nullable RocketPartFamily getResolvedModularFamily() {
        RocketPartCategory category = this.getActiveSchematicCategory();
        if (category == null) {
            return null;
        }
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        RocketPartFamily family = catalog.getFamily(this.selectedFamilyId);
        if (family == null || family.category() != category) {
            List<RocketPartFamily> families = catalog.familiesFor(category);
            if (families.isEmpty()) {
                return null;
            }
            family = families.get(0);
        }
        return family;
    }

    public @Nullable RocketPartConfig getResolvedModularConfig() {
        RocketPartFamily family = this.getResolvedModularFamily();
        if (family == null) {
            return null;
        }
        RocketPartSpec spec = family.findBestMatch(this.selectedDiameter, this.selectedTopDiameter, this.selectedHeight);
        return spec.toConfig(this.selectedLivery);
    }

    public void setModularSelection(
            @Nullable String familyId,
            int diameter,
            int topDiameter,
            int heightBlocks,
            @Nullable RocketLivery livery
    ) {
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        RocketPartCategory activeCategory = this.getActiveSchematicCategory();
        RocketPartFamily family = catalog.getFamily(familyId);
        if (family == null || (activeCategory != null && family.category() != activeCategory)) {
            if (activeCategory != null) {
                List<RocketPartFamily> families = catalog.familiesFor(activeCategory);
                if (!families.isEmpty()) {
                    family = families.get(0);
                }
            }
        }
        if (family == null) {
            return;
        }

        RocketPartSpec matched = family.findBestMatch(
                Math.max(1, diameter),
                Math.max(1, topDiameter),
                Math.max(1, heightBlocks)
        );
        this.selectedFamilyId = family.familyId();
        this.selectedDiameter = matched.diameter();
        this.selectedTopDiameter = matched.topDiameter();
        this.selectedHeight = matched.heightBlocks();
        this.selectedLivery = livery != null ? livery : RocketLivery.WHITE;
        this.modularProgress = 0;
        this.modularMaxProgress = 0;
        this.lastModularModelId = matched.modelId();
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel && serverLevel.getChunkSource() != null) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public @NotNull String getSelectedFamilyId() {
        return this.selectedFamilyId;
    }

    public int getSelectedDiameter() {
        return this.selectedDiameter;
    }

    public int getSelectedTopDiameter() {
        return this.selectedTopDiameter;
    }

    public int getSelectedHeight() {
        return this.selectedHeight;
    }

    public @NotNull RocketLivery getSelectedLivery() {
        return this.selectedLivery;
    }

    public int getModularProgress() {
        return this.modularProgress;
    }

    public int getModularMaxProgress() {
        return this.modularMaxProgress;
    }

    public int getEffectiveProgress() {
        if (this.getActiveSchematicCategory() != null) {
            return this.modularProgress;
        }
        return this.getProgress();
    }

    public int getEffectiveMaxProgress() {
        if (this.getActiveSchematicCategory() != null) {
            if (this.modularMaxProgress > 0) {
                return this.modularMaxProgress;
            }
            RocketPartConfig config = this.getResolvedModularConfig();
            return config != null ? RocketPartCostCalculator.calculate(config).assemblyTimeTicks() : 0;
        }
        RecipeHolder<PartAssemblerRecipe> recipe = this.getActiveRecipe();
        return recipe != null ? this.getProcessingTime(recipe) : 0;
    }

    public void setClientSelectedFamilyId(@Nullable String familyId) {
        this.selectedFamilyId = familyId != null ? familyId : "";
    }

    public void setClientSelectedDiameter(int diameter) {
        this.selectedDiameter = Math.max(1, diameter);
    }

    public void setClientSelectedTopDiameter(int topDiameter) {
        this.selectedTopDiameter = Math.max(1, topDiameter);
    }

    public void setClientSelectedHeight(int height) {
        this.selectedHeight = Math.max(1, height);
    }

    public void setClientSelectedLivery(@Nullable RocketLivery livery) {
        this.selectedLivery = livery != null ? livery : RocketLivery.WHITE;
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
        return new PartAssemblerMenu(syncId, inventory, this);
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
        this.writeModularState(tag);
    }

    @Override
    public void populateUpdateTag(CompoundTag tag) {
        super.populateUpdateTag(tag);
        tag.putInt("Progress", this.getEffectiveProgress());
        tag.putInt("MaxProgress", this.getEffectiveMaxProgress());
        this.writeModularState(tag);
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
        this.readModularState(tag);
    }

    private void writeModularState(CompoundTag tag) {
        tag.putString("ModularFamily", this.selectedFamilyId);
        tag.putInt("ModularDiameter", this.selectedDiameter);
        tag.putInt("ModularTopDiameter", this.selectedTopDiameter);
        tag.putInt("ModularHeight", this.selectedHeight);
        tag.putString("ModularLivery", this.selectedLivery.getSerializedName());
        tag.putInt("ModularProgress", this.modularProgress);
        tag.putInt("ModularMaxProgress", this.modularMaxProgress);
    }

    private void readModularState(CompoundTag tag) {
        if (tag.contains("ModularFamily", Tag.TAG_STRING)) {
            this.selectedFamilyId = tag.getString("ModularFamily");
        }
        if (tag.contains("ModularDiameter", Tag.TAG_INT)) {
            this.selectedDiameter = Math.max(1, tag.getInt("ModularDiameter"));
        }
        if (tag.contains("ModularTopDiameter", Tag.TAG_INT)) {
            this.selectedTopDiameter = Math.max(1, tag.getInt("ModularTopDiameter"));
        }
        if (tag.contains("ModularHeight", Tag.TAG_INT)) {
            this.selectedHeight = Math.max(1, tag.getInt("ModularHeight"));
        }
        if (tag.contains("ModularLivery", Tag.TAG_STRING)) {
            this.selectedLivery = RocketLivery.byName(tag.getString("ModularLivery"), RocketLivery.WHITE);
        }
        if (tag.contains("ModularProgress", Tag.TAG_INT)) {
            this.modularProgress = Math.max(0, tag.getInt("ModularProgress"));
        }
        if (tag.contains("ModularMaxProgress", Tag.TAG_INT)) {
            this.modularMaxProgress = Math.max(0, tag.getInt("ModularMaxProgress"));
        }
    }

    public float getClientProgressRatio(float tickDelta) {
        if (this.level != null && !this.level.isClientSide) {
            int max = this.getEffectiveMaxProgress();
            return max > 0 ? Mth.clamp((float) this.getEffectiveProgress() / (float) max, 0.0F, 1.0F) : 0.0F;
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
