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
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.component.TelemetryData;
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
import dev.galacticraft.mod.content.entity.orbital.FlightTelemetryEngine;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.recipe.TelemetryAnalysisRecipe;
import dev.galacticraft.mod.recipe.TelemetryAnalysisRecipeInput;
import dev.galacticraft.mod.research.ResearchDomain;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.research.ResearchMilestone;
import dev.galacticraft.mod.screen.TelemetryAnalyzerMenu;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Flight Telemetry Analyzer ({@code TELEMETRY_ANALYZER}).
 * Uses {@link RecipeManager#createCheck} to cross-correlate a {@code RECORDED_DATA_DRIVE}
 * ({@link GCDataComponents#TELEMETRY_DATA}) + physical planetary specimen + chemical reagent
 * into primary Science Points ({@code SP}), 5 Scientific Domain points, telemetry milestones,
 * and recycled/survey output items.
 */
public class TelemetryAnalyzerBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int DATA_DRIVE_SLOT = 1;
    public static final int SAMPLE_SLOT = 2;
    public static final int REAGENT_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;

    public static final long ENERGY_CAPACITY = 45_000L;
    public static final long ENERGY_CONSUMPTION = 75L;
    public static final int DEFAULT_ANALYSIS_TICKS = 80;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 70)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(36, 26)
                            .capacity(1)
                            .filter((item, c) -> item == GCItems.RECORDED_DATA_DRIVE),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(36, 52),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(62, 52),
                    ItemResourceSlot.builder(TransferType.OUTPUT)
                            .pos(148, 39)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            )
    );

    private final RecipeManager.CachedCheck<TelemetryAnalysisRecipeInput, TelemetryAnalysisRecipe> quickCheck =
            RecipeManager.createCheck(GCRecipes.TELEMETRY_ANALYSIS_TYPE);

    private int progress = 0;
    private int maxProgress = DEFAULT_ANALYSIS_TICKS;
    private int lastSpAwarded = 0;
    private int lastDomainAwarded = 0;
    private int lastDomainOrdinal = 0;
    private int lastMatchedRecipeFlag = 0;

    public TelemetryAnalyzerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.TELEMETRY_ANALYZER, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        ItemResourceSlot driveSlot = this.itemStorage().slot(DATA_DRIVE_SLOT);
        if (driveSlot.isEmpty()) {
            this.progress = 0;
            return GCMachineStatuses.MISSING_SPECIMEN;
        }

        ItemStack driveStack = this.itemStorage().getItem(DATA_DRIVE_SLOT);
        TelemetryData telemetry = driveStack.get(GCDataComponents.TELEMETRY_DATA);
        if (telemetry == null) {
            this.progress = 0;
            return MachineStatuses.INVALID_RECIPE;
        }

        ItemResourceSlot sampleSlot = this.itemStorage().slot(SAMPLE_SLOT);
        ItemResourceSlot reagentSlot = this.itemStorage().slot(REAGENT_SLOT);
        ItemStack sampleStack = sampleSlot.isEmpty() ? ItemStack.EMPTY : this.itemStorage().getItem(SAMPLE_SLOT);
        ItemStack reagentStack = reagentSlot.isEmpty() ? ItemStack.EMPTY : this.itemStorage().getItem(REAGENT_SLOT);

        TelemetryAnalysisRecipeInput recipeInput = new TelemetryAnalysisRecipeInput(driveStack, sampleStack, reagentStack);
        Optional<RecipeHolder<TelemetryAnalysisRecipe>> matched = this.quickCheck.getRecipeFor(recipeInput, level);

        TelemetryAnalysisRecipe recipe = matched.map(RecipeHolder::value).orElse(null);
        this.lastMatchedRecipeFlag = recipe != null ? 1 : 0;

        // Require either a matching data-driven TelemetryAnalysisRecipe OR a valid sample + reagent fallback
        if (recipe == null && (sampleStack.isEmpty() || !isFallbackReagent(reagentStack))) {
            this.progress = 0;
            return MachineStatuses.INVALID_RECIPE;
        }

        ItemStack outputPreview = resolveOutputStack(level, pos, telemetry, recipe);
        ItemResourceSlot outSlot = this.itemStorage().slot(OUTPUT_SLOT);
        if (!outputPreview.isEmpty() && !outSlot.canInsert(outputPreview.getItem(), outputPreview.getComponentsPatch(), outputPreview.getCount())) {
            return MachineStatuses.OUTPUT_FULL;
        }

        this.maxProgress = recipe != null ? recipe.getProcessingTime() : DEFAULT_ANALYSIS_TICKS;

        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (drawn < ENERGY_CONSUMPTION) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        if (this.progress >= this.maxProgress) {
            completeAnalysis(level, pos, telemetry, recipe, driveStack, sampleStack, reagentStack, outputPreview);
            this.progress = 0;
            this.setChanged();
        }
        return GCMachineStatuses.ANALYZING_SPECIMEN;
    }

    private void completeAnalysis(
            @NotNull ServerLevel level,
            @NotNull BlockPos pos,
            @NotNull TelemetryData telemetry,
            @Nullable TelemetryAnalysisRecipe recipe,
            @NotNull ItemStack driveStack,
            @NotNull ItemStack sampleStack,
            @NotNull ItemStack reagentStack,
            @NotNull ItemStack outputStack
    ) {
        ItemResourceSlot driveSlot = this.itemStorage().slot(DATA_DRIVE_SLOT);
        ItemResourceSlot sampleSlot = this.itemStorage().slot(SAMPLE_SLOT);
        ItemResourceSlot reagentSlot = this.itemStorage().slot(REAGENT_SLOT);
        ItemResourceSlot outSlot = this.itemStorage().slot(OUTPUT_SLOT);

        try (Transaction tx = Transaction.openOuter()) {
            long dEx = driveSlot.extract(driveStack.getItem(), driveStack.getComponentsPatch(), 1L, tx);
            long sEx = sampleStack.isEmpty() ? 1L : sampleSlot.extract(sampleStack.getItem(), sampleStack.getComponentsPatch(), 1L, tx);
            long rEx = reagentStack.isEmpty() ? 1L : reagentSlot.extract(reagentStack.getItem(), reagentStack.getComponentsPatch(), 1L, tx);
            if (dEx < 1L || sEx < 1L || rEx < 1L) {
                return;
            }
            if (!outputStack.isEmpty()) {
                long ins = outSlot.insert(outputStack.getItem(), outputStack.getComponentsPatch(), outputStack.getCount(), tx);
                if (ins < outputStack.getCount()) {
                    return;
                }
            }
            tx.commit();
        }

        ResearchDomain domain = ResearchDomain.byId(telemetry.primaryDomainId());
        if (domain == null) {
            domain = FlightTelemetryEngine.resolveDomainForModule(telemetry.scienceModuleId(), telemetry.eventType());
        }

        int bonusSp = recipe != null ? recipe.getBonusSp() : 10;
        int bonusDom = recipe != null ? recipe.getBonusDomainPoints() : 8;
        int totalBaseSp = telemetry.sciencePoints() + bonusSp;
        int totalBaseDom = telemetry.domainPoints() + bonusDom;

        float quality = 1.0F;
        if (recipe != null && recipe.getTargetPlanet().isPresent()
                && recipe.getTargetPlanet().get().getPath().equalsIgnoreCase(telemetry.planetId().getPath())) {
            quality += 0.25F; // Exact planetary sample cross-correlation bonus
        }
        if (reagentStack.is(GCItems.LUNAR_SAPPHIRE)) {
            quality += 0.10F;
        }

        String milestone = resolveTelemetryMilestone(telemetry);
        String expId = "telemetry_" + telemetry.scienceModuleId() + "_" + telemetry.eventType().id();
        String expTitle = "Telemetry: " + telemetry.scienceModuleId() + " (" + telemetry.eventType().displayName() + ")";

        var result = ResearchManager.recordScienceExperiment(
                level,
                expId,
                expTitle,
                "Telemetry Analyzer",
                telemetry.planetName(),
                domain,
                totalBaseSp,
                totalBaseDom,
                quality,
                null,
                milestone
        );

        // Also award the universal telemetry analyzed milestone and mark body mapped if spectrometer/camera
        var state = ResearchManager.get(level);
        state.completeMilestone(ResearchMilestone.FLIGHT_TELEMETRY_ANALYZED);
        if (telemetry.eventType() == TelemetryData.EventType.ATMOSPHERE_ENTRY
                || telemetry.eventType() == TelemetryData.EventType.ORBITAL_SCAN) {
            state.markBodyMapped(telemetry.planetId());
        }
        ResearchManager.broadcastToAll(level.getServer(), state);

        this.lastSpAwarded = result.spAwarded();
        this.lastDomainAwarded = result.domainPointsAwarded();
        this.lastDomainOrdinal = domain.ordinal();
    }

    private static @NotNull ItemStack resolveOutputStack(
            @NotNull ServerLevel level,
            @NotNull BlockPos pos,
            @NotNull TelemetryData telemetry,
            @Nullable TelemetryAnalysisRecipe recipe
    ) {
        if (telemetry.scienceModuleId().equals(FlightTelemetryEngine.MOD_ORBITAL_CAMERA)) {
            var env = dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment.byId(telemetry.planetId());
            if (env != null) {
                return FlightTelemetryEngine.createOrbitalSurveyMap(level, env, pos);
            }
        }
        if (recipe != null && !recipe.getOutputItem().isEmpty()) {
            return recipe.getOutputItem().copy();
        }
        return new ItemStack(GCItems.BLANK_DATA_DRIVE);
    }

    private static boolean isFallbackReagent(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == GCItems.NIOBIUM_OXIDE
                || item == GCItems.LUNAR_SAPPHIRE
                || item == GCItems.CARBON_FRAGMENTS
                || item == GCItems.SILICON
                || item == GCItems.SOLAR_DUST;
    }

    private static @NotNull String resolveTelemetryMilestone(@NotNull TelemetryData telemetry) {
        return switch (telemetry.eventType()) {
            case THERMAL_FAILURE -> ResearchMilestone.THERMAL_FAILURE_STUDIED;
            case ATMOSPHERE_ENTRY -> ResearchMilestone.ATMOSPHERIC_SPECTRUM_CAPTURED;
            case RADIATION_BELT -> ResearchMilestone.RADIATION_FLUX_MEASURED;
            case MAGNETOSPHERE -> ResearchMilestone.MAGNETOSPHERE_MAPPED;
            case ZERO_G_ORBITAL_LAB -> ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED;
            default -> ResearchMilestone.FLIGHT_TELEMETRY_ANALYZED;
        };
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    public int getLastSpAwarded() {
        return this.lastSpAwarded;
    }

    public int getLastDomainAwarded() {
        return this.lastDomainAwarded;
    }

    public int getLastDomainOrdinal() {
        return this.lastDomainOrdinal;
    }

    public int getLastMatchedRecipeFlag() {
        return this.lastMatchedRecipeFlag;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("Progress", this.progress);
        tag.putInt("MaxProgress", this.maxProgress);
        tag.putInt("LastSp", this.lastSpAwarded);
        tag.putInt("LastDom", this.lastDomainAwarded);
        tag.putInt("LastDomOrd", this.lastDomainOrdinal);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = tag.getInt("Progress");
        this.maxProgress = Math.max(1, tag.getInt("MaxProgress"));
        this.lastSpAwarded = tag.getInt("LastSp");
        this.lastDomainAwarded = tag.getInt("LastDom");
        this.lastDomainOrdinal = tag.getInt("LastDomOrd");
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResearchManager.syncToPlayer(serverPlayer);
            return new TelemetryAnalyzerMenu(syncId, serverPlayer, this);
        }
        return null;
    }
}
