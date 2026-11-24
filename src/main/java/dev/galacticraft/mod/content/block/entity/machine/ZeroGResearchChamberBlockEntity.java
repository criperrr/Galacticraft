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
import dev.galacticraft.mod.screen.ZeroGResearchChamberMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Microgravity &amp; Crystallography Research Chamber ({@code ZERO_G_RESEARCH_CHAMBER}).
 * Strictly requires local gravity {@code <= 0.01G}, scales visually up to a {@code 2x2 / 3x3}
 * cryogenic superconducting dome, features interactive Magnetic Levitation Confinement, and houses
 * 2 exclusive upgrade bays (the ONLY bench utilizing {@code SUPERCONDUCTING_COIL} upgrades!):
 * <ul>
 *   <li><b>Levitation Ring:</b> {@code ELECTROMAGNETIC_STABILIZER_RING} or {@code SUPERCONDUCTING_CONFINEMENT_COIL}</li>
 *   <li><b>Cryo / Gimbal:</b> {@code GYROSCOPIC_DAMPENING_GIMBAL} or {@code HELIUM_DILUTION_CRYOSTAT}</li>
 * </ul>
 */
public class ZeroGResearchChamberBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int MATERIAL_A_SLOT = 1;
    public static final int MATERIAL_B_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int CONFINEMENT_UPGRADE_SLOT = 4;
    public static final int CRYO_GIMBAL_UPGRADE_SLOT = 5;

    public static final double MAX_ALLOWED_GRAVITY_G = 0.01;
    public static final long ENERGY_CAPACITY = 90_000L;
    public static final long ENERGY_CONSUMPTION = 110L;
    public static final int EXPERIMENT_TICKS = 100;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER).pos(8, 78).capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(32, 30),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(32, 62),
                    ItemResourceSlot.builder(TransferType.OUTPUT).pos(198, 26),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(198, 54).capacity(1)
                            .filter((item, c) -> item == GCItems.ELECTROMAGNETIC_STABILIZER_RING || item == GCItems.SUPERCONDUCTING_CONFINEMENT_COIL),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(198, 78).capacity(1)
                            .filter((item, c) -> item == GCItems.GYROSCOPIC_DAMPENING_GIMBAL || item == GCItems.HELIUM_DILUTION_CRYOSTAT)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            )
    );

    public static final List<ZeroGExperiment> EXPERIMENTS = List.of(
            new ZeroGExperiment(
                    1, "Pure Carbon & SOFI", "Carbon + Fabric -> 0G Polymer",
                    GCItems.CARBON_FRAGMENTS, GCItems.ISOTHERMAL_FABRIC,
                    ResearchCatalog.ZERO_G_CARBON_SOFI, ResearchDomain.STRUCTURES, 70,
                    GCItems.CARBON_FIBER_PLATE, 1, 0xFF34D399
            ),
            new ZeroGExperiment(
                    2, "Pure Carbon Weave", "Carbon -> 0G Carbon Fiber",
                    GCItems.CARBON_FRAGMENTS, null,
                    ResearchCatalog.ZERO_G_CARBON_SOFI, ResearchDomain.STRUCTURES, 56,
                    GCItems.CARBON_FIBER_PLATE, 1, 0xFF34D399
            ),
            new ZeroGExperiment(
                    3, "Nb3Sn Superconductor", "Niobium + Tin -> 0G Nb3Sn",
                    GCItems.NIOBIUM_INGOT, GCItems.TIN_INGOT,
                    ResearchCatalog.ZERO_G_SUPERCONDUCTORS_NB3SN, ResearchDomain.ELECTROMAGNETISM, 82,
                    GCItems.SUPERCONDUCTING_COIL, 1, 0xFFC084FC
            ),
            new ZeroGExperiment(
                    4, "Dilution Cryo <2K", "He-3 Cell + Nb -> <2K Cryo",
                    GCItems.HELIUM3_CELL, GCItems.COMPRESSED_NIOBIUM,
                    ResearchCatalog.ZERO_G_DILUTION_CRYOGENICS, ResearchDomain.CRYOGENICS, 95,
                    GCItems.SOFI_INSULATION, 1, 0xFF38BDF8
            ),
            new ZeroGExperiment(
                    5, "Aneutronic MHD Fusion", "He-3 + Nb3Sn Coil -> MHD",
                    GCItems.HELIUM3_CELL, GCItems.SUPERCONDUCTING_COIL,
                    ResearchCatalog.ZERO_G_ANEUTRONIC_MHD_FUSION, ResearchDomain.NUCLEAR_PLASMA, 115,
                    GCItems.HELIUM3_FUSION_CELL, 1, 0xFFFACC15
            ),
            new ZeroGExperiment(
                    6, "7x7-11x11 0G Welding", "Titanium + C-103 -> Heavy Weld",
                    GCItems.COMPRESSED_TITANIUM, GCItems.COMPRESSED_NIOBIUM,
                    ResearchCatalog.ZERO_G_HEAVY_ORBITAL_WELDING, ResearchDomain.STRUCTURES, 100,
                    GCItems.TIER_3_HEAVY_DUTY_PLATE, 1, 0xFF5CE0FF
            ),
            new ZeroGExperiment(
                    7, "0G Monocrystal Optics", "Sapphire -> Defect-Free Lattice",
                    GCItems.LUNAR_SAPPHIRE, null,
                    ResearchCatalog.ZERO_G_SUPERCONDUCTORS_NB3SN, ResearchDomain.ELECTROMAGNETISM, 64,
                    GCItems.BEAM_CORE, 1, 0xFF7BE8FF
            )
    );

    private int progress = 0;
    private int activeExperimentId = 0;
    private int localGravityMilliG = 1000;
    private int microgravityValidFlag = 0;
    private int domeTier = 1;           // 1=compact, 2=2x2 dome, 3=3x3 cryogenic dome
    private int confinementTier = 0;    // 0=base, 1=EM ring, 2=Superconducting Confinement Coil
    private int cryoGimbalTier = 0;     // 0=base, 1=Gyro Gimbal, 2=Helium Dilution Cryostat

    private transient @Nullable PlanetaryEnvironment testEnvOverride = null;

    public ZeroGResearchChamberBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.ZERO_G_RESEARCH_CHAMBER, pos, state, SPEC);
    }

    public void setEnvironmentContextForTest(@NotNull ResourceKey<Level> dim, @Nullable CelestialBody<?, ?> body) {
        this.testEnvOverride = body != null
                ? PlanetaryEnvironment.fromCelestialBody(dim.location(), body)
                : PlanetaryEnvironment.byId(dim.location());
    }

    private PlanetaryEnvironment resolveEnvironment(Level level) {
        return this.testEnvOverride != null ? this.testEnvOverride : PlanetaryEnvironment.fromLevel(level);
    }

    public static int detectDomeTier(@NotNull Level level, @NotNull BlockPos pos) {
        int ring = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                Block b = level.getBlockState(pos.offset(dx, 0, dz)).getBlock();
                if (b == GCBlocks.VACUUM_GLASS || b == GCBlocks.OLIVINE_GLASS || b == GCBlocks.FUSION_PLASMA_GLASS
                        || b == GCBlocks.FUSION_MAGNET_COIL || b == GCBlocks.TITANIUM_BLOCK || b == GCBlocks.AIR_LOCK_FRAME) {
                    ring++;
                }
            }
        }
        return ring >= 8 ? 3 : (ring >= 3 ? 2 : 1);
    }

    public static @Nullable ZeroGExperiment matchExperiment(@Nullable Item itemA, @Nullable Item itemB) {
        if (itemA == null && itemB == null) {
            return null;
        }
        for (ZeroGExperiment exp : EXPERIMENTS) {
            if (exp.materialB() != null) {
                if ((itemA == exp.materialA() && itemB == exp.materialB())
                        || (itemB == exp.materialA() && itemA == exp.materialB())) {
                    return exp;
                }
            }
        }
        for (ZeroGExperiment exp : EXPERIMENTS) {
            if (exp.materialB() == null && (itemA == exp.materialA() || itemB == exp.materialA())) {
                return exp;
            }
        }
        return null;
    }

    public static @Nullable ZeroGExperiment byId(int id) {
        for (ZeroGExperiment exp : EXPERIMENTS) {
            if (exp.id() == id) {
                return exp;
            }
        }
        return null;
    }

    /**
     * Server-validated magnetic levitation harvest triggered by the interactive confinement arena.
     */
    public boolean harvestLevitatingCrystal(@NotNull ServerLevel level, int purityPct) {
        PlanetaryEnvironment env = this.resolveEnvironment(level);
        if (env.gravityG() > MAX_ALLOWED_GRAVITY_G) {
            return false;
        }
        ItemResourceSlot slotA = this.itemStorage().slot(MATERIAL_A_SLOT);
        ItemResourceSlot slotB = this.itemStorage().slot(MATERIAL_B_SLOT);
        Item itemA = slotA.isEmpty() ? null : slotA.getResource();
        Item itemB = slotB.isEmpty() ? null : slotB.getResource();
        ZeroGExperiment exp = matchExperiment(itemA, itemB);
        if (exp == null) {
            return false;
        }
        int maxCap = this.cryoGimbalTier == 2 ? 125 : 100;
        int clampedPurity = Mth.clamp(purityPct, 50, maxCap);
        float quality = 0.75F + (clampedPurity / 100.0F) * 0.55F;
        return finishSynthesis(level, env, slotA, slotB, itemA, itemB, exp, quality);
    }

    private boolean finishSynthesis(
            @NotNull ServerLevel level,
            @NotNull PlanetaryEnvironment env,
            ItemResourceSlot slotA,
            ItemResourceSlot slotB,
            @Nullable Item itemA,
            @Nullable Item itemB,
            ZeroGExperiment exp,
            float qualityMultiplier
    ) {
        ItemResourceSlot outSlot = this.itemStorage().slot(OUTPUT_SLOT);
        if (!outSlot.canInsert(exp.outputItem(), DataComponentPatch.EMPTY, exp.outputCount())) {
            return false;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (drawn < ENERGY_CONSUMPTION) {
                return false;
            }
            if (exp.materialB() != null) {
                long e1 = slotA.extract(itemA, DataComponentPatch.EMPTY, 1L, tx);
                long e2 = slotB.extract(itemB, DataComponentPatch.EMPTY, 1L, tx);
                if (e1 < 1L || e2 < 1L) {
                    return false;
                }
            } else {
                if (itemA == exp.materialA()) {
                    if (slotA.extract(itemA, DataComponentPatch.EMPTY, 1L, tx) < 1L) {
                        return false;
                    }
                } else if (itemB == exp.materialA()) {
                    if (slotB.extract(itemB, DataComponentPatch.EMPTY, 1L, tx) < 1L) {
                        return false;
                    }
                }
            }
            long ins = outSlot.insert(exp.outputItem(), DataComponentPatch.EMPTY, exp.outputCount(), tx);
            if (ins < exp.outputCount()) {
                return false;
            }
            tx.commit();
        }

        ResearchManager.recordScienceExperiment(
                level,
                "zerog_crystal_" + exp.id(),
                "0G Levitation: " + exp.shortName(),
                "Zero-G Chamber",
                env.displayName(),
                exp.domain(),
                exp.points(),
                Math.max(12, (int) Math.round(exp.points() * 0.72)),
                qualityMultiplier,
                exp.unlockNodeId(),
                ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED
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
        int prevDome = this.domeTier;
        int prevValid = this.microgravityValidFlag;
        int prevExp = this.activeExperimentId;

        this.domeTier = detectDomeTier(level, pos);
        Item cUp = this.itemStorage().slot(CONFINEMENT_UPGRADE_SLOT).getResource();
        this.confinementTier = cUp == GCItems.SUPERCONDUCTING_CONFINEMENT_COIL ? 2
                : (cUp == GCItems.ELECTROMAGNETIC_STABILIZER_RING ? 1 : 0);
        Item gUp = this.itemStorage().slot(CRYO_GIMBAL_UPGRADE_SLOT).getResource();
        this.cryoGimbalTier = gUp == GCItems.HELIUM_DILUTION_CRYOSTAT ? 2
                : (gUp == GCItems.GYROSCOPIC_DAMPENING_GIMBAL ? 1 : 0);

        PlanetaryEnvironment env = this.resolveEnvironment(level);
        double gravity = env.gravityG();
        this.localGravityMilliG = (int) Math.round(gravity * 1000.0);
        boolean isZeroG = gravity <= MAX_ALLOWED_GRAVITY_G;
        this.microgravityValidFlag = isZeroG ? 1 : 0;

        if (this.domeTier != prevDome || this.microgravityValidFlag != prevValid) {
            this.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        if (!isZeroG) {
            this.progress = 0;
            this.activeExperimentId = 0;
            return GCMachineStatuses.GRAVITY_TOO_HIGH;
        }

        ItemResourceSlot slotA = this.itemStorage().slot(MATERIAL_A_SLOT);
        ItemResourceSlot slotB = this.itemStorage().slot(MATERIAL_B_SLOT);
        ItemResourceSlot outSlot = this.itemStorage().slot(OUTPUT_SLOT);

        Item itemA = slotA.isEmpty() ? null : slotA.getResource();
        Item itemB = slotB.isEmpty() ? null : slotB.getResource();
        ZeroGExperiment exp = matchExperiment(itemA, itemB);
        if (exp == null) {
            this.progress = 0;
            this.activeExperimentId = 0;
            return (itemA == null && itemB == null) ? GCMachineStatuses.MISSING_SPECIMEN : MachineStatuses.INVALID_RECIPE;
        }

        if (!outSlot.canInsert(exp.outputItem(), DataComponentPatch.EMPTY, exp.outputCount())) {
            return MachineStatuses.OUTPUT_FULL;
        }

        this.activeExperimentId = exp.id();

        if (this.progress + 1 >= EXPERIMENT_TICKS) {
            if (!finishSynthesis(level, env, slotA, slotB, itemA, itemB, exp, 0.90F)) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            return GCMachineStatuses.MICROGRAVITY_SYNTHESIS;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (drawn < ENERGY_CONSUMPTION) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        return GCMachineStatuses.MICROGRAVITY_SYNTHESIS;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getActiveExperimentId() {
        return this.activeExperimentId;
    }

    public int getLocalGravityMilliG() {
        return this.localGravityMilliG;
    }

    public int getMicrogravityValidFlag() {
        return this.microgravityValidFlag;
    }

    public int getDomeTier() {
        return this.domeTier;
    }

    public int getConfinementTier() {
        return this.confinementTier;
    }

    public int getCryoGimbalTier() {
        return this.cryoGimbalTier;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("progress", this.progress);
        tag.putInt("active_experiment_id", this.activeExperimentId);
        tag.putInt("dome_tier", this.domeTier);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = Math.max(0, tag.getInt("progress"));
        this.activeExperimentId = tag.getInt("active_experiment_id");
        this.domeTier = Mth.clamp(tag.contains("dome_tier") ? tag.getInt("dome_tier") : 1, 1, 3);
        if (tag.contains("microgravity")) {
            this.microgravityValidFlag = tag.getInt("microgravity") & 1;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("active_experiment_id", this.activeExperimentId);
        tag.putInt("dome_tier", this.domeTier);
        tag.putInt("microgravity", this.microgravityValidFlag);
        return tag;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new ZeroGResearchChamberMenu(syncId, (ServerPlayer) player, this);
    }

    public record ZeroGExperiment(
            int id,
            @NotNull String shortName,
            @NotNull String formula,
            @NotNull Item materialA,
            @Nullable Item materialB,
            @NotNull ResourceLocation unlockNodeId,
            @NotNull ResearchDomain domain,
            int points,
            @NotNull Item outputItem,
            int outputCount,
            int crystalColorArgb
    ) {
    }
}
