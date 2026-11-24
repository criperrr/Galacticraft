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
import dev.galacticraft.api.gas.GasComposition;
import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.machinelib.api.util.FluidSource;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.screen.AtmosphericFractionatorMenu;
import dev.galacticraft.mod.util.FluidUtil;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Cryogenic Atmospheric Fractionator: queries the current dimension's CelestialBody GasComposition
 * via LevelBodyAccessor and separates ambient planetary gases into 4 fractional distillation columns:
 * - Earth: N2, O2, Ar, CO2
 * - Mars: CO2 (95.3%), N2, Ar, O2
 * - Venus: CO2 (96.5%), N2, H2SO4 acid vapor, CO
 * - Jupiter / Saturn (3.5x He-3) / Uranus / Neptune: H2, Gases.HELIUM_3, CH4, Gases.XENON
 * - Vacuum bodies (Moon, Mercury, Asteroids): reports NO_ATMOSPHERE.
 */
public class AtmosphericFractionatorBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int CANISTER_SLOT_0 = 1;
    public static final int CANISTER_SLOT_1 = 2;
    public static final int CANISTER_SLOT_2 = 3;
    public static final int CANISTER_SLOT_3 = 4;

    public static final int OUTPUT_TANK_0 = 0;
    public static final int OUTPUT_TANK_1 = 1;
    public static final int OUTPUT_TANK_2 = 2;
    public static final int OUTPUT_TANK_3 = 3;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 100L;
    public static final long TANK_CAPACITY = FluidUtil.bucketsToDroplets(16);
    public static final int CYCLE_TICKS = 20;

    // Planet code constants synced to HUD
    public static final int PLANET_VACUUM = 0;
    public static final int PLANET_EARTH = 1;
    public static final int PLANET_MARS = 2;
    public static final int PLANET_VENUS = 3;
    public static final int PLANET_JUPITER = 4;
    public static final int PLANET_SATURN = 5;
    public static final int PLANET_URANUS = 6;
    public static final int PLANET_NEPTUNE = 7;
    public static final int PLANET_EXO = 8;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 76)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(80, 76)
                            .capacity(1)
                            .filter(ResourceFilters.providesApi(FluidStorage.ITEM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(104, 76)
                            .capacity(1)
                            .filter(ResourceFilters.providesApi(FluidStorage.ITEM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(128, 76)
                            .capacity(1)
                            .filter(ResourceFilters.providesApi(FluidStorage.ITEM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(152, 76)
                            .capacity(1)
                            .filter(ResourceFilters.providesApi(FluidStorage.ITEM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET))
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(80, 22)
                            .capacity(TANK_CAPACITY),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(104, 22)
                            .capacity(TANK_CAPACITY),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(128, 22)
                            .capacity(TANK_CAPACITY),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(152, 22)
                            .capacity(TANK_CAPACITY)
            )
    );

    private final FluidSource fluidSource = new FluidSource(this);
    private int progress = 0;
    private int intakePressurePct = 0;
    private int planetCode = PLANET_VACUUM;
    private int pressureMilliAtm = 0;
    private final int[] channelFluidIds = new int[]{-1, -1, -1, -1};
    private transient @Nullable AtmosphereProfile cachedProfile = null;

    public AtmosphericFractionatorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.ATMOSPHERIC_FRACTIONATOR, pos, state, SPEC);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.cachedProfile = null;
        if (level != null) {
            this.refreshAtmosphereProfile(level);
        }
    }

    private AtmosphereProfile getAtmosphereProfile(Level level) {
        if (this.cachedProfile == null) {
            this.refreshAtmosphereProfile(level);
        }
        return this.cachedProfile;
    }

    private void refreshAtmosphereProfile(Level level) {
        this.applyAtmosphereProfile(resolveAtmosphereProfile(level));
    }

    private void applyAtmosphereProfile(AtmosphereProfile profile) {
        this.cachedProfile = profile;
        this.planetCode = profile.planetCode();
        this.pressureMilliAtm = profile.pressureMilliAtm();
        for (int i = 0; i < 4; i++) {
            if (i < profile.channels().size()) {
                this.channelFluidIds[i] = BuiltInRegistries.FLUID.getId(profile.channels().get(i).fluid());
            } else {
                this.channelFluidIds[i] = -1;
            }
        }
    }

    public void setAtmosphereContextForTest(ResourceKey<Level> dim, @Nullable CelestialBody<?, ?> body) {
        this.applyAtmosphereProfile(resolveAtmosphereProfile(dim, body));
    }

    public static AtmosphereProfile resolveAtmosphereProfile(Level level) {
        Holder<CelestialBody<?, ?>> bodyHolder = level.galacticraft$getCelestialBody();
        return resolveAtmosphereProfile(level.dimension(), bodyHolder != null ? bodyHolder.value() : null);
    }

    public static AtmosphereProfile resolveAtmosphereProfile(ResourceKey<Level> dim, @Nullable CelestialBody<?, ?> body) {
        GasComposition gasComp = body != null ? body.atmosphere() : null;
        float pressure = gasComp != null ? gasComp.pressure() : 0.0F;

        // Explicit planetary profiles aligned with GCCelestialBodies real astrophysics + ISRU balance
        if (dim.equals(GCDimensions.MOON) || dim.equals(GCDimensions.MERCURY) || dim.equals(GCDimensions.ASTEROID)) {
            return new AtmosphereProfile(PLANET_VACUUM, 0, List.of());
        }
        if (dim.equals(Level.OVERWORLD)) {
            int pMilli = gasComp != null ? Math.round(pressure * 1000.0F) : 1000;
            return new AtmosphereProfile(PLANET_EARTH, pMilli, List.of(
                    new GasChannel(Gases.NITROGEN, 45L),
                    new GasChannel(Gases.OXYGEN, 15L),
                    new GasChannel(Gases.ARGON, 5L),
                    new GasChannel(Gases.CARBON_DIOXIDE, 2L)
            ));
        }
        if (dim.equals(GCDimensions.MARS)) {
            int pMilli = gasComp != null ? Math.max(1, Math.round(pressure * 1000.0F)) : 6;
            return new AtmosphereProfile(PLANET_MARS, pMilli, List.of(
                    new GasChannel(Gases.CARBON_DIOXIDE, 60L),
                    new GasChannel(Gases.NITROGEN, 10L),
                    new GasChannel(Gases.ARGON, 5L),
                    new GasChannel(Gases.OXYGEN, 2L)
            ));
        }
        if (dim.equals(GCDimensions.VENUS)) {
            int pMilli = gasComp != null ? Math.round(pressure * 1000.0F) : 92000;
            return new AtmosphereProfile(PLANET_VENUS, pMilli, List.of(
                    new GasChannel(Gases.CARBON_DIOXIDE, 85L),
                    new GasChannel(Gases.NITROGEN, 18L),
                    new GasChannel(GCFluids.SULFURIC_ACID, 15L),
                    new GasChannel(Gases.CARBON_MONOXIDE, 4L)
            ));
        }
        if (dim.equals(GCDimensions.JUPITER)) {
            int pMilli = gasComp != null ? Math.round(pressure * 1000.0F) : 12000;
            return new AtmosphereProfile(PLANET_JUPITER, pMilli, List.of(
                    new GasChannel(Gases.HYDROGEN, 70L),
                    new GasChannel(Gases.HELIUM_3, 18L),
                    new GasChannel(Gases.METHANE, 12L),
                    new GasChannel(Gases.XENON, 4L)
            ));
        }
        if (dim.equals(GCDimensions.SATURN)) {
            // Maximum 3.5x Helium-3 atmospheric enrichment on Saturn
            int pMilli = gasComp != null ? Math.round(pressure * 1000.0F) : 8500;
            return new AtmosphereProfile(PLANET_SATURN, pMilli, List.of(
                    new GasChannel(Gases.HYDROGEN, 80L),
                    new GasChannel(Gases.HELIUM_3, 35L),
                    new GasChannel(Gases.METHANE, 16L),
                    new GasChannel(Gases.XENON, 6L)
            ));
        }
        if (dim.equals(GCDimensions.URANUS)) {
            int pMilli = gasComp != null ? Math.round(pressure * 1000.0F) : 5000;
            return new AtmosphereProfile(PLANET_URANUS, pMilli, List.of(
                    new GasChannel(Gases.HYDROGEN, 60L),
                    new GasChannel(Gases.HELIUM_3, 26L),
                    new GasChannel(Gases.METHANE, 28L),
                    new GasChannel(Gases.XENON, 9L)
            ));
        }
        if (dim.equals(GCDimensions.NEPTUNE)) {
            int pMilli = gasComp != null ? Math.round(pressure * 1000.0F) : 7500;
            return new AtmosphereProfile(PLANET_NEPTUNE, pMilli, List.of(
                    new GasChannel(Gases.HYDROGEN, 55L),
                    new GasChannel(Gases.HELIUM_3, 28L),
                    new GasChannel(Gases.METHANE, 22L),
                    new GasChannel(Gases.XENON, 14L)
            ));
        }

        // Dynamic fallback for any addon or custom celestial body with GasComposition
        if (gasComp == null || pressure <= 0.0001F || gasComp.composition().isEmpty()) {
            return new AtmosphereProfile(PLANET_VACUUM, 0, List.of());
        }

        List<Object2DoubleMap.Entry<ResourceKey<Fluid>>> entries = new ArrayList<>(gasComp.composition().object2DoubleEntrySet());
        entries.sort(Comparator.comparingDouble((Object2DoubleMap.Entry<ResourceKey<Fluid>> e) -> e.getDoubleValue()).reversed());
        List<GasChannel> channels = new ArrayList<>(4);
        for (Object2DoubleMap.Entry<ResourceKey<Fluid>> entry : entries) {
            if (channels.size() >= 4) {
                break;
            }
            Fluid fluid = BuiltInRegistries.FLUID.get(entry.getKey().location());
            if (fluid != Fluids.EMPTY) {
                long mb = Math.max(2L, Math.min(80L, Math.round(entry.getDoubleValue() / 15_000.0)));
                channels.add(new GasChannel(fluid, mb));
            }
        }
        if (channels.isEmpty()) {
            return new AtmosphereProfile(PLANET_VACUUM, 0, List.of());
        }
        return new AtmosphereProfile(PLANET_EXO, Math.max(1, Math.round(pressure * 1000.0F)), channels);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
        this.drainFluidToSlot(CANISTER_SLOT_0, OUTPUT_TANK_0);
        this.drainFluidToSlot(CANISTER_SLOT_1, OUTPUT_TANK_1);
        this.drainFluidToSlot(CANISTER_SLOT_2, OUTPUT_TANK_2);
        this.drainFluidToSlot(CANISTER_SLOT_3, OUTPUT_TANK_3);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        profiler.push("transfer");
        this.fluidSource.trySpreadFluids(level, pos, state);
        profiler.pop();

        AtmosphereProfile profile = this.getAtmosphereProfile(level);
        if (profile.channels().isEmpty()) {
            this.progress = 0;
            this.intakePressurePct = Math.max(0, this.intakePressurePct - 10);
            return GCMachineStatuses.NO_ATMOSPHERE;
        }

        // Check whether at least one active atmospheric channel has output tank space
        boolean anySpace = false;
        for (int i = 0; i < profile.channels().size(); i++) {
            GasChannel ch = profile.channels().get(i);
            FluidResourceSlot tank = this.fluidStorage().slot(i);
            if (tank.tryInsert(ch.fluid(), ch.droplets()) > 0L) {
                anySpace = true;
                break;
            }
        }
        if (!anySpace) {
            this.intakePressurePct = Math.max(0, this.intakePressurePct - 6);
            return GCMachineStatuses.GAS_TANK_FULL;
        }

        // Final tick of cycle: atomic energy draw + multi-column fractional gas condensation
        if (this.progress + 1 >= CYCLE_TICKS) {
            boolean insertedAny = false;
            try (Transaction tx = Transaction.openOuter()) {
                long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
                if (drawn < ENERGY_CONSUMPTION) {
                    this.intakePressurePct = Math.max(0, this.intakePressurePct - 8);
                    return MachineStatuses.NOT_ENOUGH_ENERGY;
                }
                for (int i = 0; i < profile.channels().size(); i++) {
                    GasChannel ch = profile.channels().get(i);
                    FluidResourceSlot tank = this.fluidStorage().slot(i);
                    long inserted = tank.insert(ch.fluid(), DataComponentPatch.EMPTY, ch.droplets(), tx);
                    if (inserted > 0L) {
                        insertedAny = true;
                    }
                }
                if (insertedAny) {
                    tx.commit();
                }
            }
            if (!insertedAny) {
                return GCMachineStatuses.GAS_TANK_FULL;
            }
            this.progress = 0;
            this.intakePressurePct = Math.min(100, this.intakePressurePct + 12);
            this.setChanged();
            return GCMachineStatuses.FRACTIONATING;
        }

        // Intermediate tick: atomic energy draw
        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (drawn < ENERGY_CONSUMPTION) {
                this.intakePressurePct = Math.max(0, this.intakePressurePct - 8);
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        this.intakePressurePct = Math.min(100, this.intakePressurePct + 8);
        return GCMachineStatuses.FRACTIONATING;
    }

    @Override
    protected void tickDisabled(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        this.intakePressurePct = Math.max(0, this.intakePressurePct - 8);
    }

    public int getProgress() {
        return this.progress;
    }

    public int getIntakePressurePct() {
        return this.intakePressurePct;
    }

    public int getPlanetCode() {
        return this.planetCode;
    }

    public int getPressureMilliAtm() {
        return this.pressureMilliAtm;
    }

    public int getChannelFluidId(int index) {
        return index >= 0 && index < 4 ? this.channelFluidIds[index] : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("progress", this.progress);
        tag.putInt("intake_pressure_pct", this.intakePressurePct);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = tag.getInt("progress");
        this.intakePressurePct = tag.getInt("intake_pressure_pct");
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new AtmosphericFractionatorMenu(syncId, (ServerPlayer) player, this);
    }

    public record GasChannel(Fluid fluid, long mbPerCycle) {
        public long droplets() {
            return this.mbPerCycle * FluidUtil.MILLIBUCKET;
        }
    }

    public record AtmosphereProfile(int planetCode, int pressureMilliAtm, List<GasChannel> channels) {
    }
}
