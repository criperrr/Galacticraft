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
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
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
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchDomain;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.research.ResearchMilestone;
import dev.galacticraft.mod.screen.EnvironmentalTestChamberMenu;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Environmental &amp; Hyperbaric Stress Test Chamber ({@code ENVIRONMENTAL_TEST_CHAMBER}).
 * Features interactive live Pressure Valve &amp; Thermal Exchange control under real planetary
 * atmospheres (Earth 1 atm sim vac, Moon/Mercury 0 atm vac, Venus 92 atm + H2SO4 acid) and 2
 * exclusive upgrade bays:
 * <ul>
 *   <li><b>Pressure Bay:</b> {@code TURBOMOLECULAR_VACUUM_PUMP} or {@code HYPERBARIC_PISTON_MANIFOLD}</li>
 *   <li><b>Thermal Liner:</b> {@code REGENERATIVE_THERMAL_EXCHANGER} or {@code ABLATIVE_ACID_LINER}</li>
 * </ul>
 */
public class EnvironmentalTestChamberBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int TEST_SPECIMEN_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int PRESSURE_UPGRADE_SLOT = 3;
    public static final int THERMAL_UPGRADE_SLOT = 4;

    public static final long ENERGY_CAPACITY = 75_000L;
    public static final long SIMULATED_VACUUM_ENERGY = 120L;
    public static final long IN_SITU_ENERGY = 60L;
    public static final int TEST_DURATION_TICKS = 90;

    public static final int ENV_MODE_EARTH_SIM_VAC = 0;
    public static final int ENV_MODE_REAL_VACUUM = 1;
    public static final int ENV_MODE_VENUS_HYPERBARIC = 2;
    public static final int ENV_MODE_CRYO_HIGH_PRESSURE = 3;
    public static final int ENV_MODE_THIN_MARS = 4;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER).pos(8, 78).capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(32, 48)
                            .filter((item, c) -> isTestableSpecimen(item)),
                    ItemResourceSlot.builder(TransferType.OUTPUT).pos(198, 26),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(198, 54).capacity(1)
                            .filter((item, c) -> item == GCItems.TURBOMOLECULAR_VACUUM_PUMP || item == GCItems.HYPERBARIC_PISTON_MANIFOLD),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(198, 78).capacity(1)
                            .filter((item, c) -> item == GCItems.REGENERATIVE_THERMAL_EXCHANGER || item == GCItems.ABLATIVE_ACID_LINER)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    SIMULATED_VACUUM_ENERGY * 4,
                    0
            )
    );

    private int progress = 0;
    private int envModeCode = ENV_MODE_EARTH_SIM_VAC;
    private int ambientPressureMilliAtm = 1000;
    private int ambientTempCelsius = 21;
    private int corrosiveAcidFlag = 0;
    private int activeEnergyCost = (int) SIMULATED_VACUUM_ENERGY;
    private int manualSimOverride = 0;
    private int pressureUpgradeTier = 0; // 0=none, 1=turbomolecular pump, 2=hyperbaric piston
    private int thermalUpgradeTier = 0;  // 0=none, 1=regenerative exchanger, 2=ablative acid liner

    private transient @Nullable PlanetaryEnvironment testEnvOverride = null;

    public EnvironmentalTestChamberBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.ENVIRONMENTAL_TEST_CHAMBER, pos, state, SPEC);
    }

    public static boolean isTestableSpecimen(@Nullable Item item) {
        if (item == null) {
            return false;
        }
        return item == Items.GLASS
                || item == GCBlocks.VACUUM_GLASS.asItem()
                || item == GCBlocks.OLIVINE_GLASS.asItem()
                || item == GCItems.COMPRESSED_ALUMINUM
                || item == GCItems.COMPRESSED_STEEL
                || item == GCItems.COMPRESSED_FERRONIOBIUM
                || item == GCItems.COMPRESSED_NIOBIUM
                || item == GCItems.COMPRESSED_DESH
                || item == GCItems.COMPRESSED_TITANIUM
                || item == GCItems.ABLATIVE_HEAT_TILE
                || item == GCItems.OLIVINE_SHARD
                || item == GCItems.LEAD_INGOT
                || item == GCItems.THERMAL_CLOTH
                || item == GCItems.ISOTHERMAL_FABRIC
                || item == GCItems.SOFI_INSULATION
                || item == GCItems.LONSDALEITE_GEM
                || item == GCItems.OXYGEN_MASK
                || item == GCItems.TIN_CANISTER;
    }

    public void setEnvironmentContextForTest(@NotNull ResourceKey<Level> dim, @Nullable CelestialBody<?, ?> body) {
        this.testEnvOverride = body != null
                ? PlanetaryEnvironment.fromCelestialBody(dim.location(), body)
                : PlanetaryEnvironment.byId(dim.location());
    }

    public void cycleTestMode() {
        this.manualSimOverride = (this.manualSimOverride + 1) % 2;
        this.progress = 0;
        this.setChanged();
    }

    private PlanetaryEnvironment resolveEnvironment(Level level) {
        return this.testEnvOverride != null ? this.testEnvOverride : PlanetaryEnvironment.fromLevel(level);
    }

    public static int classifyEnvMode(@NotNull PlanetaryEnvironment env, boolean forceVacuumSim) {
        if (forceVacuumSim) {
            return ENV_MODE_EARTH_SIM_VAC;
        }
        if (env.corrosiveAcid() || env.pressureAtm() >= 50.0) {
            return ENV_MODE_VENUS_HYPERBARIC;
        }
        if (env.pressureAtm() <= 0.002) {
            return ENV_MODE_REAL_VACUUM;
        }
        if (env.pressureAtm() >= 4.0) {
            return ENV_MODE_CRYO_HIGH_PRESSURE;
        }
        if (env.pressureAtm() < 0.1) {
            return ENV_MODE_THIN_MARS;
        }
        return ENV_MODE_EARTH_SIM_VAC;
    }

    private static ChamberOutcome evaluateOutcome(int envMode, @NotNull PlanetaryEnvironment env, @NotNull Item specimen) {
        return switch (envMode) {
            case ENV_MODE_VENUS_HYPERBARIC -> new ChamberOutcome(
                    ResearchCatalog.VENUS_HYPERBARIC_HULLS,
                    ResearchDomain.STRUCTURES,
                    specimen == GCItems.LEAD_INGOT || specimen == GCItems.ISOTHERMAL_FABRIC ? 68 : 52,
                    "92atm Acid Hull Test",
                    specimen == GCItems.THERMAL_CLOTH ? GCItems.ISOTHERMAL_FABRIC : specimen,
                    ResearchMilestone.VENUS_92ATM_CERTIFIED
            );
            case ENV_MODE_REAL_VACUUM -> {
                if (env.peakTempKelvin() >= 600.0 || specimen == GCItems.ABLATIVE_HEAT_TILE || specimen == GCItems.OLIVINE_SHARD) {
                    yield new ChamberOutcome(
                            ResearchCatalog.MERCURY_SOLAR_VOLATILES_HE3,
                            ResearchDomain.THERMODYNAMICS,
                            56,
                            "Vacuum Thermal Shock",
                            specimen,
                            ResearchMilestone.VACUUM_TRIAL_CERTIFIED
                    );
                }
                yield new ChamberOutcome(
                        ResearchCatalog.LUNAR_VACUUM_METALLURGY,
                        ResearchDomain.THERMODYNAMICS,
                        50,
                        "Natural Vacuum TPS/C-103",
                        specimen == Items.GLASS ? GCBlocks.VACUUM_GLASS.asItem() : specimen,
                        ResearchMilestone.VACUUM_TRIAL_CERTIFIED
                );
            }
            case ENV_MODE_CRYO_HIGH_PRESSURE -> new ChamberOutcome(
                    ResearchCatalog.URANUS_LONSDALEITE_PRESSURE,
                    ResearchDomain.STRUCTURES,
                    72,
                    "Cryo-Abyssal Pressure",
                    specimen,
                    null
            );
            case ENV_MODE_THIN_MARS -> new ChamberOutcome(
                    ResearchCatalog.MARTIAN_DESH_ELASTOMERS,
                    ResearchDomain.STRUCTURES,
                    44,
                    "Low-P Dust & Thermal",
                    specimen,
                    null
            );
            default -> new ChamberOutcome(
                    ResearchCatalog.EARTH_VACUUM_SEALING,
                    ResearchDomain.STRUCTURES,
                    34,
                    "Simulated Vacuum Seal",
                    specimen == Items.GLASS ? GCBlocks.VACUUM_GLASS.asItem() : specimen,
                    ResearchMilestone.VACUUM_TRIAL_CERTIFIED
            );
        };
    }

    /**
     * Server-validated certification triggered when the player actively holds the pressure/thermal
     * point inside the green critical certification window.
     */
    public boolean certifyControlledTest(@NotNull ServerLevel level, int stabilityPct) {
        ItemResourceSlot specSlot = this.itemStorage().slot(TEST_SPECIMEN_SLOT);
        if (specSlot.isEmpty()) {
            return false;
        }
        PlanetaryEnvironment env = this.resolveEnvironment(level);
        this.envModeCode = classifyEnvMode(env, this.manualSimOverride == 1);
        Item specimen = specSlot.getResource();
        ChamberOutcome outcome = evaluateOutcome(this.envModeCode, env, specimen);

        int clamped = Mth.clamp(stabilityPct, 50, 100);
        float quality = 0.85F + (clamped / 100.0F) * 0.45F;
        if (this.thermalUpgradeTier == 2 && (this.envModeCode == ENV_MODE_VENUS_HYPERBARIC || env.peakTempKelvin() >= 500.0)) {
            quality *= 1.25F; // Ablative Acid Liner bonus
        }
        return finishTest(level, env, specSlot, specimen, outcome, quality);
    }

    private boolean finishTest(
            @NotNull ServerLevel level,
            @NotNull PlanetaryEnvironment env,
            ItemResourceSlot specSlot,
            Item specimen,
            ChamberOutcome outcome,
            float qualityMultiplier
    ) {
        ItemResourceSlot outSlot = this.itemStorage().slot(RESULT_SLOT);
        if (!outSlot.canInsert(outcome.outputItem(), DataComponentPatch.EMPTY, 1L)) {
            return false;
        }
        long energyCost = this.activeEnergyCost;
        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(energyCost, tx);
            if (drawn < energyCost) {
                return false;
            }
            long ex = specSlot.extract(specimen, DataComponentPatch.EMPTY, 1L, tx);
            if (ex < 1L) {
                return false;
            }
            long ins = outSlot.insert(outcome.outputItem(), DataComponentPatch.EMPTY, 1L, tx);
            if (ins < 1L) {
                return false;
            }
            tx.commit();
        }

        ResearchManager.recordScienceExperiment(
                level,
                "env_chamber_" + this.envModeCode + "_" + outcome.summary().toLowerCase().replaceAll("[^a-z0-9]+", "_"),
                "Env Chamber: " + outcome.summary(),
                "Environmental Chamber",
                env.displayName(),
                outcome.domain(),
                outcome.points(),
                Math.max(10, (int) Math.round(outcome.points() * 0.70)),
                qualityMultiplier,
                outcome.unlockNodeId(),
                outcome.milestoneId()
        );
        this.progress = 0;
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        Item pUp = this.itemStorage().slot(PRESSURE_UPGRADE_SLOT).getResource();
        this.pressureUpgradeTier = pUp == GCItems.HYPERBARIC_PISTON_MANIFOLD ? 2
                : (pUp == GCItems.TURBOMOLECULAR_VACUUM_PUMP ? 1 : 0);
        Item tUp = this.itemStorage().slot(THERMAL_UPGRADE_SLOT).getResource();
        this.thermalUpgradeTier = tUp == GCItems.ABLATIVE_ACID_LINER ? 2
                : (tUp == GCItems.REGENERATIVE_THERMAL_EXCHANGER ? 1 : 0);

        int prevMode = this.envModeCode;
        int prevAcid = this.corrosiveAcidFlag;
        PlanetaryEnvironment env = this.resolveEnvironment(level);
        this.envModeCode = classifyEnvMode(env, this.manualSimOverride == 1);
        this.ambientPressureMilliAtm = (int) Math.round(env.pressureAtm() * 1000.0);
        this.ambientTempCelsius = (int) Math.round(env.peakTempKelvin() - PlanetaryEnvironment.KELVIN_OFFSET);
        this.corrosiveAcidFlag = env.corrosiveAcid() ? 1 : 0;
        if (this.envModeCode != prevMode || this.corrosiveAcidFlag != prevAcid) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        long baseEnergy = this.envModeCode == ENV_MODE_EARTH_SIM_VAC ? SIMULATED_VACUUM_ENERGY : IN_SITU_ENERGY;
        if (this.pressureUpgradeTier == 1 && this.envModeCode == ENV_MODE_EARTH_SIM_VAC) {
            baseEnergy = Math.round(baseEnergy * 0.40); // Turbomolecular Vacuum Pump -60% Earth vacuum power!
        }
        this.activeEnergyCost = (int) baseEnergy;

        ItemResourceSlot specSlot = this.itemStorage().slot(TEST_SPECIMEN_SLOT);
        ItemResourceSlot outSlot = this.itemStorage().slot(RESULT_SLOT);
        if (specSlot.isEmpty()) {
            this.progress = 0;
            return GCMachineStatuses.MISSING_SPECIMEN;
        }

        Item specimen = specSlot.getResource();
        ChamberOutcome outcome = evaluateOutcome(this.envModeCode, env, specimen);
        if (!outSlot.canInsert(outcome.outputItem(), DataComponentPatch.EMPTY, 1L)) {
            return MachineStatuses.OUTPUT_FULL;
        }

        if (this.progress + 1 >= TEST_DURATION_TICKS) {
            if (!finishTest(level, env, specSlot, specimen, outcome, 0.85F)) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            return GCMachineStatuses.RUNNING_ENV_TEST;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(baseEnergy, tx);
            if (drawn < baseEnergy) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        return GCMachineStatuses.RUNNING_ENV_TEST;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getEnvModeCode() {
        return this.envModeCode;
    }

    public int getAmbientPressureMilliAtm() {
        return this.ambientPressureMilliAtm;
    }

    public int getAmbientTempCelsius() {
        return this.ambientTempCelsius;
    }

    public int getCorrosiveAcidFlag() {
        return this.corrosiveAcidFlag;
    }

    public int getActiveEnergyCost() {
        return this.activeEnergyCost;
    }

    public int getManualSimOverride() {
        return this.manualSimOverride;
    }

    public int getPressureUpgradeTier() {
        return this.pressureUpgradeTier;
    }

    public int getThermalUpgradeTier() {
        return this.thermalUpgradeTier;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("progress", this.progress);
        tag.putInt("manual_sim", this.manualSimOverride);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = Math.max(0, tag.getInt("progress"));
        this.manualSimOverride = tag.getInt("manual_sim") & 1;
        if (tag.contains("env_mode")) {
            this.envModeCode = tag.getInt("env_mode");
        }
        if (tag.contains("acid")) {
            this.corrosiveAcidFlag = tag.getInt("acid") & 1;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("env_mode", this.envModeCode);
        tag.putInt("acid", this.corrosiveAcidFlag);
        return tag;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new EnvironmentalTestChamberMenu(syncId, (ServerPlayer) player, this);
    }

    private record ChamberOutcome(
            @NotNull ResourceLocation unlockNodeId,
            @NotNull ResearchDomain domain,
            int points,
            @NotNull String summary,
            @NotNull Item outputItem,
            @Nullable String milestoneId
    ) {
    }
}
