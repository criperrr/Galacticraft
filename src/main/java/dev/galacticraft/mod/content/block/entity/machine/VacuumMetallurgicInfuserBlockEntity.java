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

package dev.galacticraft.mod.content.block.entity.machine;

import com.mojang.datafixers.util.Pair;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.recipe.MetallurgicInfusingRecipe;
import dev.galacticraft.mod.screen.VacuumMetallurgicInfuserMenu;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class VacuumMetallurgicInfuserBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int INFUSION_AGENT_SLOT = 1;
    public static final int BASE_METAL_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 60L;
    public static final int MAX_INFUSION = 1000;

    public static final String INFUSION_NONE = "";
    public static final String INFUSION_CARBON = "carbon";
    public static final String INFUSION_NIOBIUM_OXIDE = "niobium_oxide";
    public static final String INFUSION_TITANIUM = "titanium";
    public static final String INFUSION_TIN = "tin";

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 74)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(34, 42)
                            .filter((item, components) -> resolveAgent(item) != null),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(74, 42),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(136, 42)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            )
    );

    private final RecipeManager.CachedCheck<MetallurgicInfusingRecipe.Input, MetallurgicInfusingRecipe> quickCheck =
            RecipeManager.createCheck(GCRecipes.METALLURGIC_INFUSING_TYPE);
    private String infusionType = INFUSION_NONE;
    private int infusionAmount = 0;
    private int progress = 0;
    private int maxProgress = 100;

    public VacuumMetallurgicInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.VACUUM_METALLURGIC_INFUSER, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
        this.absorbInfusionAgent();
    }

    private void absorbInfusionAgent() {
        ItemResourceSlot agentSlot = this.itemStorage().slot(INFUSION_AGENT_SLOT);
        if (agentSlot.isEmpty()) {
            return;
        }
        Item agentItem = agentSlot.getResource();
        InfusionAgentSpec spec = resolveAgent(agentItem);
        if (spec == null) {
            return;
        }
        if (this.infusionAmount > 0 && !this.infusionType.equals(spec.type)) {
            return;
        }
        if (this.infusionAmount + spec.units > MAX_INFUSION) {
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = agentSlot.extract(agentItem, null, 1L, tx);
            if (extracted == 1L) {
                tx.commit();
                this.infusionType = spec.type;
                this.infusionAmount += spec.units;
                this.setChanged();
            }
        }
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        ItemResourceSlot baseSlot = this.itemStorage().slot(BASE_METAL_SLOT);
        if (baseSlot.isEmpty()) {
            this.progress = 0;
            return MachineStatuses.INVALID_RECIPE;
        }

        if (this.infusionAmount <= 0 || this.infusionType.isEmpty()) {
            this.progress = 0;
            return GCMachineStatuses.MISSING_INFUSION_AGENT;
        }

        Item baseItem = baseSlot.getResource();
        ItemStack baseStack = new ItemStack(baseItem, 1);
        MetallurgicInfusingRecipe.Input recipeInput = new MetallurgicInfusingRecipe.Input(baseStack, this.infusionType, this.infusionAmount);
        Optional<RecipeHolder<MetallurgicInfusingRecipe>> match = this.quickCheck.getRecipeFor(recipeInput, level);

        if (match.isEmpty()) {
            this.progress = 0;
            return MachineStatuses.INVALID_RECIPE;
        }

        MetallurgicInfusingRecipe recipe = match.get().value();
        this.maxProgress = Math.max(1, recipe.getProcessingTime());

        ItemStack result = recipe.getResult();
        ItemResourceSlot outputSlot = this.itemStorage().slot(OUTPUT_SLOT);
        if (!outputSlot.canInsert(result.getItem(), result.getCount())) {
            return MachineStatuses.OUTPUT_FULL;
        }

        if (this.progress + 1 >= this.maxProgress) {
            try (Transaction tx = Transaction.openOuter()) {
                long usedEnergy = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
                if (usedEnergy < ENERGY_CONSUMPTION) {
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                if (this.infusionAmount < recipe.getInfusionAmount()) {
                    return GCMachineStatuses.MISSING_INFUSION_AGENT;
                }
                long extractedBase = baseSlot.extract(baseItem, null, 1L, tx);
                if (extractedBase < 1L) {
                    return MachineStatuses.INVALID_RECIPE;
                }
                long insertedOut = outputSlot.insert(result.getItem(), DataComponentPatch.EMPTY, result.getCount(), tx);
                if (insertedOut < result.getCount()) {
                    return MachineStatuses.OUTPUT_FULL;
                }
                tx.commit();
            }
            this.infusionAmount -= recipe.getInfusionAmount();
            if (this.infusionAmount <= 0) {
                this.infusionAmount = 0;
                this.infusionType = INFUSION_NONE;
            }
            this.progress = 0;
            this.setChanged();
            return GCMachineStatuses.INFUSING;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long usedEnergy = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (usedEnergy < ENERGY_CONSUMPTION) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        return GCMachineStatuses.INFUSING;
    }

    public static @Nullable InfusionAgentSpec resolveAgent(@Nullable Item item) {
        if (item == null) {
            return null;
        }
        if (item == GCItems.CARBON_FRAGMENTS) {
            return new InfusionAgentSpec(INFUSION_CARBON, 20);
        }
        if (item == Items.COAL || item == Items.CHARCOAL) {
            return new InfusionAgentSpec(INFUSION_CARBON, 10);
        }
        if (item == GCItems.NIOBIUM_OXIDE) {
            return new InfusionAgentSpec(INFUSION_NIOBIUM_OXIDE, 20);
        }
        if (item == GCItems.TITANIUM_INGOT) {
            return new InfusionAgentSpec(INFUSION_TITANIUM, 20);
        }
        if (item == GCItems.TITANIUM_NUGGET) {
            return new InfusionAgentSpec(INFUSION_TITANIUM, 2);
        }
        if (item == GCItems.TIN_INGOT) {
            return new InfusionAgentSpec(INFUSION_TIN, 20);
        }
        if (item == GCItems.TIN_NUGGET) {
            return new InfusionAgentSpec(INFUSION_TIN, 2);
        }
        return null;
    }

    public static int typeToId(String type) {
        if (INFUSION_CARBON.equals(type)) return 1;
        if (INFUSION_NIOBIUM_OXIDE.equals(type)) return 2;
        if (INFUSION_TITANIUM.equals(type)) return 3;
        if (INFUSION_TIN.equals(type)) return 4;
        return 0;
    }

    public static String idToDisplayName(int id) {
        return switch (id) {
            case 1 -> "Carbon";
            case 2 -> "Nb2O5";
            case 3 -> "Titanium";
            case 4 -> "Tin";
            default -> "Empty";
        };
    }

    public static int idToBarColor(int id) {
        return switch (id) {
            case 1 -> 0xFF3E444E;
            case 2 -> 0xFF8AB8E6;
            case 3 -> 0xFF9AC5DC;
            case 4 -> 0xFFD0D6DC;
            default -> 0xFF23252B;
        };
    }

    public static Item representativeAgentItem(String infusionType) {
        return switch (infusionType) {
            case INFUSION_CARBON -> GCItems.CARBON_FRAGMENTS;
            case INFUSION_NIOBIUM_OXIDE -> GCItems.NIOBIUM_OXIDE;
            case INFUSION_TITANIUM -> GCItems.TITANIUM_INGOT;
            case INFUSION_TIN -> GCItems.TIN_INGOT;
            default -> GCItems.CARBON_FRAGMENTS;
        };
    }

    public record InfusionAgentSpec(String type, int units) {
    }

    public String getInfusionType() {
        return this.infusionType;
    }

    public int getInfusionTypeId() {
        return typeToId(this.infusionType);
    }

    public int getInfusionAmount() {
        return this.infusionAmount;
    }

    public void setInfusionBuffer(String type, int amount) {
        this.infusionAmount = Math.max(0, Math.min(MAX_INFUSION, amount));
        this.infusionType = this.infusionAmount > 0 ? type : INFUSION_NONE;
        this.setChanged();
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putString("infusion_type", this.infusionType);
        tag.putInt("infusion_amount", this.infusionAmount);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.infusionType = tag.getString("infusion_type");
        this.infusionAmount = Math.max(0, Math.min(MAX_INFUSION, tag.getInt("infusion_amount")));
        if (this.infusionAmount == 0) {
            this.infusionType = INFUSION_NONE;
        }
        this.progress = tag.getInt("progress");
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : 100;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new VacuumMetallurgicInfuserMenu(syncId, (ServerPlayer) player, this);
    }
}
