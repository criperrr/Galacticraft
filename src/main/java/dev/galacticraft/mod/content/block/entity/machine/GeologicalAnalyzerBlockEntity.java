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
import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchDomain;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.research.ResearchMilestone;
import dev.galacticraft.mod.screen.GeologicalAnalyzerMenu;
import dev.galacticraft.mod.util.FluidUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
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
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Geological &amp; Chemical Laser Mass Spectrometer ({@code GEOLOGICAL_ANALYZER}).
 * Features interactive Laser Wavelength ({@code 200-700 nm}) &amp; Power spectrometry matching
 * hidden characteristic mineral peaks, +50% In-Situ native planet bonus, and 2 exclusive upgrade bays:
 * <ul>
 *   <li><b>Laser Emitter:</b> {@code TUNABLE_DIODE_LASER} or {@code UV_EXCIMER_LASER}</li>
 *   <li><b>Mass Detector:</b> {@code DIFFRACTION_GRATING_PRISM} or {@code QUADRUPOLE_MASS_FILTER}</li>
 * </ul>
 */
public class GeologicalAnalyzerBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int SPECIMEN_SLOT = 1;
    public static final int REAGENT_SLOT = 2;
    public static final int FLUID_CANISTER_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int LASER_UPGRADE_SLOT = 5;
    public static final int DETECTOR_UPGRADE_SLOT = 6;

    public static final int REAGENT_FLUID_TANK = 0;

    public static final long ENERGY_CAPACITY = 60_000L;
    public static final long ENERGY_CONSUMPTION = 70L;
    public static final long TANK_CAPACITY = FluidUtil.bucketsToDroplets(8);
    public static final long ACID_CONSUME_MB = 100L;
    public static final int ANALYZE_TICKS = 80;

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER).pos(8, 78).capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(32, 26),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(32, 52)
                            .filter((item, c) -> isReagentItem(item)),
                    ItemResourceSlot.builder(TransferType.PROCESSING).pos(32, 78).capacity(1)
                            .filter(ResourceFilters.providesApi(FluidStorage.ITEM))
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.OUTPUT).pos(198, 26),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(198, 54).capacity(1)
                            .filter((item, c) -> item == GCItems.TUNABLE_DIODE_LASER || item == GCItems.UV_EXCIMER_LASER),
                    ItemResourceSlot.builder(TransferType.INPUT).pos(198, 78).capacity(1)
                            .filter((item, c) -> item == GCItems.DIFFRACTION_GRATING_PRISM || item == GCItems.QUADRUPOLE_MASS_FILTER)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(174, 26)
                            .capacity(TANK_CAPACITY)
                            .filter((fluid, c) -> fluid == GCFluids.SULFURIC_ACID || fluid == Fluids.WATER)
            )
    );

    private static final Map<Item, GeologicalAssay> ASSAYS = new LinkedHashMap<>();

    static {
        // Earth
        register(GCItems.RAW_NIOBIUM, new GeologicalAssay(1, "Pyrochlore Nb2O5", GCCelestialBodies.EARTH.location(), ResearchCatalog.FERRONIOBIUM_ALUMINOTHERMY, ResearchDomain.THERMODYNAMICS, 24, GCItems.NIOBIUM_OXIDE, 1, 0xFFF59E0B, 465, null));
        register(GCItems.SILICON, new GeologicalAssay(2, "Crystalline Si", GCCelestialBodies.EARTH.location(), ResearchCatalog.EARTH_PROTOTYPING, ResearchDomain.ELECTROMAGNETISM, 22, GCItems.BASIC_WAFER, 1, 0xFF60A5FA, 545, null));

        // Moon
        register(GCItems.OLIVINE_SHARD, new GeologicalAssay(3, "Lunar Olivine", GCCelestialBodies.MOON.location(), ResearchCatalog.LUNAR_OLIVINE_OPTICS, ResearchDomain.THERMODYNAMICS, 36, GCItems.OLIVINE_SHARD, 0, 0xFF84CC16, 515, null));
        register(GCItems.LUNAR_SAPPHIRE, new GeologicalAssay(4, "Al2O3 Sapphire", GCCelestialBodies.MOON.location(), ResearchCatalog.LUNAR_OLIVINE_OPTICS, ResearchDomain.ELECTROMAGNETISM, 42, GCItems.SENSOR_LENS, 1, 0xFF38BDF8, 442, null));
        register(GCBlocks.MOON_ILMENITE_ORE.asItem(), new GeologicalAssay(5, "Moon Ilmenite 3He", GCCelestialBodies.MOON.location(), ResearchCatalog.LUNAR_HE3_THERMAL_DESORPTION, ResearchDomain.NUCLEAR_PLASMA, 54, GCItems.RAW_TITANIUM, 1, 0xFF7CE8FF, 389, ResearchMilestone.LUNAR_HE3_SPECTRUM));
        register(GCBlocks.MOON_TURF.asItem(), new GeologicalAssay(6, "Regolith 3He Trace", GCCelestialBodies.MOON.location(), ResearchCatalog.LUNAR_HE3_THERMAL_DESORPTION, ResearchDomain.NUCLEAR_PLASMA, 26, GCItems.SILICON, 1, 0xFFA3B4C6, 588, null));
        register(GCBlocks.MOON_TANGLE.asItem(), new GeologicalAssay(7, "Lunar Flora", GCCelestialBodies.MOON.location(), ResearchCatalog.LUNAR_BIO_HABITATION, ResearchDomain.CRYOGENICS, 34, GCItems.MOON_TANGLE_FRUIT, 1, 0xFF34D399, 662, null));
        register(GCBlocks.DENSE_ICE.asItem(), new GeologicalAssay(8, "Polar Dense Ice", GCCelestialBodies.MOON.location(), ResearchCatalog.LUNAR_POLAR_ELECTROLYSIS, ResearchDomain.CRYOGENICS, 38, Items.ICE, 1, 0xFF7DD3FC, 412, null));
        register(GCItems.RAW_METEORIC_IRON, new GeologicalAssay(9, "Meteoric Fe-Ni", GCCelestialBodies.MOON.location(), ResearchCatalog.LUNAR_VACUUM_METALLURGY, ResearchDomain.STRUCTURES, 36, GCItems.METEORIC_IRON_NUGGET, 2, 0xFF94A3B8, 492, null));

        // Mars
        register(GCItems.RAW_DESH, new GeologicalAssay(10, "Martian Desh", GCCelestialBodies.MARS.location(), ResearchCatalog.MARTIAN_DESH_ELASTOMERS, ResearchDomain.STRUCTURES, 50, GCItems.DESH_NUGGET, 2, 0xFFEF6C4D, 618, null));
        register(GCBlocks.MARS_STONE.asItem(), new GeologicalAssay(11, "Ferric Mars Core", GCCelestialBodies.MARS.location(), ResearchCatalog.MARTIAN_SABATIER_ISRU, ResearchDomain.CRYOGENICS, 36, Items.IRON_NUGGET, 2, 0xFFD97757, 635, null));
        register(GCBlocks.CAVERNOUS_VINES.asItem(), new GeologicalAssay(12, "Mars Bio-Vines", GCCelestialBodies.MARS.location(), ResearchCatalog.LUNAR_BIO_HABITATION, ResearchDomain.CRYOGENICS, 38, Items.SLIME_BALL, 1, 0xFF4ADE80, 558, null));

        // Asteroids
        register(GCBlocks.ILMENITE_ORE.asItem(), new GeologicalAssay(13, "Asteroid Ilmenite", GCCelestialBodies.ASTEROID.location(), ResearchCatalog.ASTEROID_TITANIUM_COMPOSITES, ResearchDomain.STRUCTURES, 56, GCItems.RAW_TITANIUM, 1, 0xFFCBD5E1, 430, null));
        register(GCItems.RAW_TITANIUM, new GeologicalAssay(14, "Titanium Matrix", GCCelestialBodies.ASTEROID.location(), ResearchCatalog.ASTEROID_TITANIUM_COMPOSITES, ResearchDomain.STRUCTURES, 52, GCItems.TITANIUM_NUGGET, 2, 0xFFE2E8F0, 454, null));

        // Mercury
        register(GCBlocks.MERCURY_HOLLOW_DEPOSIT.asItem(), new GeologicalAssay(15, "Mercury Hollow 3He", GCCelestialBodies.MERCURY.location(), ResearchCatalog.MERCURY_SOLAR_VOLATILES_HE3, ResearchDomain.NUCLEAR_PLASMA, 64, GCItems.CARBON_FRAGMENTS, 2, 0xFFFBBF24, 318, ResearchMilestone.LUNAR_HE3_SPECTRUM));
        register(GCBlocks.MERCURY_SULFIDE_ORE.asItem(), new GeologicalAssay(16, "Oldhamite Thorium", GCCelestialBodies.MERCURY.location(), ResearchCatalog.MERCURY_THORIUM_ISOTOPES, ResearchDomain.NUCLEAR_PLASMA, 62, GCItems.SOLAR_DUST, 1, 0xFFFACC15, 295, null));
        register(GCBlocks.MERCURY_GRAPHITE_ORE.asItem(), new GeologicalAssay(17, "Primordial Graphite", GCCelestialBodies.MERCURY.location(), ResearchCatalog.ZERO_G_CARBON_SOFI, ResearchDomain.THERMODYNAMICS, 44, GCItems.CARBON_FRAGMENTS, 2, 0xFF64748B, 658, null));

        // Venus
        register(GCBlocks.GALENA_ORE.asItem(), new GeologicalAssay(18, "Venusian Galena PbS", GCCelestialBodies.VENUS.location(), ResearchCatalog.VENUS_ACID_LEACHING, ResearchDomain.CRYOGENICS, 60, GCItems.RAW_LEAD, 1, 0xFF94A3B8, 405, null));
        register(GCItems.RAW_LEAD, new GeologicalAssay(19, "Acid-Shield Lead", GCCelestialBodies.VENUS.location(), ResearchCatalog.VENUS_HYPERBARIC_HULLS, ResearchDomain.STRUCTURES, 54, GCItems.LEAD_NUGGET, 2, 0xFF7C8B9E, 406, null));
        register(GCItems.SOLAR_DUST, new GeologicalAssay(20, "Photovoltaic Dust", GCCelestialBodies.VENUS.location(), ResearchCatalog.VENUS_ACID_LEACHING, ResearchDomain.ELECTROMAGNETISM, 56, GCItems.SOLAR_ARRAY_WAFER, 1, 0xFFFDE047, 575, null));

        // Outer Planets
        register(GCItems.RAW_PALLADIUM, new GeologicalAssay(21, "Jovian Palladium", GCCelestialBodies.JUPITER.location(), ResearchCatalog.JUPITER_PALLADIUM_CATALYSIS, ResearchDomain.THERMODYNAMICS, 78, GCItems.PALLADIUM_INGOT, 1, 0xFFE5A46B, 340, null));
        register(GCItems.METALLIC_HYDROGEN_CELL, new GeologicalAssay(22, "Metallic H2 Phase", GCCelestialBodies.JUPITER.location(), ResearchCatalog.JUPITER_PALLADIUM_CATALYSIS, ResearchDomain.ELECTROMAGNETISM, 88, GCItems.METALLIC_HYDROGEN_CELL, 1, 0xFF60A5FA, 656, null));
        register(GCBlocks.SATURN_HELIUM_MANTLE_ROCK.asItem(), new GeologicalAssay(23, "Saturn 3He Mantle", GCCelestialBodies.SATURN.location(), ResearchCatalog.SATURN_HELIUM3_HARVESTING, ResearchDomain.NUCLEAR_PLASMA, 90, GCItems.RAW_MAGNESIUM, 1, 0xFFE8D08A, 389, ResearchMilestone.LUNAR_HE3_SPECTRUM));
        register(GCItems.RAW_MAGNESIUM, new GeologicalAssay(24, "Mg-Li Ultralight", GCCelestialBodies.SATURN.location(), ResearchCatalog.SATURN_HELIUM3_HARVESTING, ResearchDomain.STRUCTURES, 78, GCItems.MAGNESIUM_INGOT, 1, 0xFFD9F99D, 518, null));
        register(GCItems.LONSDALEITE_GEM, new GeologicalAssay(25, "Hex Lonsdaleite", GCCelestialBodies.URANUS.location(), ResearchCatalog.URANUS_LONSDALEITE_PRESSURE, ResearchDomain.STRUCTURES, 96, GCItems.LONSDALEITE_GEM, 1, 0xFF7BE8FF, 260, null));
        register(GCItems.RAW_URANIUM, new GeologicalAssay(26, "Uraninite Fission", GCCelestialBodies.URANUS.location(), ResearchCatalog.MERCURY_THORIUM_ISOTOPES, ResearchDomain.NUCLEAR_PLASMA, 84, GCItems.URANIUM_INGOT, 1, 0xFF4ADE80, 358, null));
        register(GCItems.SUPERIONIC_ICE_CRYSTAL, new GeologicalAssay(27, "Superionic Ice", GCCelestialBodies.NEPTUNE.location(), ResearchCatalog.NEPTUNE_SUPERIONIC_MAGNETOPLASMA, ResearchDomain.NUCLEAR_PLASMA, 105, GCItems.SUPERIONIC_ICE_CRYSTAL, 1, 0xFF818CF8, 240, null));
    }

    private static void register(Item item, GeologicalAssay assay) {
        ASSAYS.put(item, assay);
    }

    public static boolean isReagentItem(@Nullable Item item) {
        return item == GCItems.NIOBIUM_OXIDE || item == GCItems.CARBON_FRAGMENTS || item == Items.REDSTONE;
    }

    public static @Nullable GeologicalAssay lookupAssay(@Nullable Item item) {
        return item == null ? null : ASSAYS.get(item);
    }

    public static @Nullable GeologicalAssay lookupById(int id) {
        for (GeologicalAssay assay : ASSAYS.values()) {
            if (assay.id() == id) {
                return assay;
            }
        }
        return null;
    }

    private int progress = 0;
    private int activeAssayId = 0;
    private int reagentBoostPct = 100;
    private int nativeInSituBonus = 0;
    private int lastAwardedPoints = 0;
    private int laserTier = 0;    // 0=base, 1=Tunable Diode, 2=UV Excimer
    private int detectorTier = 0; // 0=base, 1=Diffraction Grating Prism, 2=Quadrupole Mass Filter

    private transient @Nullable PlanetaryEnvironment testEnvOverride = null;

    public GeologicalAnalyzerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.GEOLOGICAL_ANALYZER, pos, state, SPEC);
    }

    public void setEnvironmentContextForTest(@NotNull ResourceKey<Level> dim, @Nullable CelestialBody<?, ?> body) {
        this.testEnvOverride = body != null
                ? PlanetaryEnvironment.fromCelestialBody(dim.location(), body)
                : PlanetaryEnvironment.byId(dim.location());
    }

    private PlanetaryEnvironment resolveEnvironment(Level level) {
        return this.testEnvOverride != null ? this.testEnvOverride : PlanetaryEnvironment.fromLevel(level);
    }

    /**
     * Server-validated interactive laser spectrometry confirmation when the player sweeps
     * wavelength &amp; power onto the mineral's emission peak and clicks {@code LOCK SPECTRUM}.
     */
    public boolean confirmLaserSpectrum(@NotNull ServerLevel level, int snrPct) {
        ItemResourceSlot specSlot = this.itemStorage().slot(SPECIMEN_SLOT);
        if (specSlot.isEmpty()) {
            return false;
        }
        Item specItem = specSlot.getResource();
        GeologicalAssay assay = lookupAssay(specItem);
        if (assay == null) {
            return false;
        }
        int clampedSnr = Mth.clamp(snrPct, 40, 100);
        float quality = 0.80F + (clampedSnr / 100.0F) * 0.50F;
        if (this.laserTier == 2 && assay.peakWavelengthNm() < 400) {
            quality *= 1.30F; // UV Excimer Laser deep-UV isotope boost!
        }
        return completeAssay(level, specSlot, specItem, assay, quality);
    }

    private boolean completeAssay(
            @NotNull ServerLevel level,
            ItemResourceSlot specSlot,
            Item specItem,
            GeologicalAssay assay,
            float qualityMultiplier
    ) {
        ItemResourceSlot reagentSlot = this.itemStorage().slot(REAGENT_SLOT);
        ItemResourceSlot outSlot = this.itemStorage().slot(OUTPUT_SLOT);
        FluidResourceSlot tank = this.fluidStorage().slot(REAGENT_FLUID_TANK);

        int byproductCount = assay.byproductCount() * (this.detectorTier == 2 ? 2 : 1);
        if (byproductCount > 0 && !outSlot.canInsert(assay.byproduct(), DataComponentPatch.EMPTY, byproductCount)) {
            return false;
        }

        PlanetaryEnvironment env = this.resolveEnvironment(level);
        boolean isNativePlanet = env.id().getPath().equalsIgnoreCase(assay.nativeBodyId().getPath());
        boolean hasSolidReagent = !reagentSlot.isEmpty();
        boolean hasAcid = !tank.isEmpty() && tank.getResource() == GCFluids.SULFURIC_ACID && tank.getAmount() >= ACID_CONSUME_MB * FluidUtil.MILLIBUCKET;
        boolean hasWater = !tank.isEmpty() && tank.getResource() == Fluids.WATER && tank.getAmount() >= ACID_CONSUME_MB * FluidUtil.MILLIBUCKET;

        int boost = 100;
        if (hasSolidReagent) boost += 35;
        if (hasAcid) boost += 40;
        else if (hasWater) boost += 15;
        if (isNativePlanet) boost += 50;
        if (this.detectorTier == 2) boost += 20;

        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (drawn < ENERGY_CONSUMPTION) {
                return false;
            }
            long eSpec = specSlot.extract(specItem, DataComponentPatch.EMPTY, 1L, tx);
            if (eSpec < 1L) {
                return false;
            }
            if (hasSolidReagent) {
                reagentSlot.extract(reagentSlot.getResource(), DataComponentPatch.EMPTY, 1L, tx);
            }
            if (hasAcid) {
                tank.extract(GCFluids.SULFURIC_ACID, DataComponentPatch.EMPTY, ACID_CONSUME_MB * FluidUtil.MILLIBUCKET, tx);
            } else if (hasWater) {
                tank.extract(Fluids.WATER, DataComponentPatch.EMPTY, ACID_CONSUME_MB * FluidUtil.MILLIBUCKET, tx);
            }
            if (byproductCount > 0) {
                long ins = outSlot.insert(assay.byproduct(), DataComponentPatch.EMPTY, byproductCount, tx);
                if (ins < byproductCount) {
                    return false;
                }
            }
            tx.commit();
        }

        int scaledSp = Math.max(1, (assay.basePoints() * boost) / 100);
        int scaledDom = Math.max(6, (int) Math.round(scaledSp * 0.70));
        ResearchManager.ScienceAwardResult result = ResearchManager.recordScienceExperiment(
                level,
                "geo_assay_" + assay.id(),
                "Mass Spec: " + assay.specimenLabel(),
                "Geological Analyzer",
                env.displayName(),
                assay.domain(),
                scaledSp,
                scaledDom,
                qualityMultiplier,
                assay.unlockNodeId(),
                assay.milestoneId()
        );
        this.lastAwardedPoints = Math.max(result.spAwarded(), result.domainPointsAwarded());
        this.progress = 0;
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
        this.takeFluidFromSlot(FLUID_CANISTER_SLOT, REAGENT_FLUID_TANK, GCFluids.SULFURIC_ACID);
        this.takeFluidFromSlot(FLUID_CANISTER_SLOT, REAGENT_FLUID_TANK, Fluids.WATER);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        Item laser = this.itemStorage().slot(LASER_UPGRADE_SLOT).getResource();
        this.laserTier = laser == GCItems.UV_EXCIMER_LASER ? 2 : (laser == GCItems.TUNABLE_DIODE_LASER ? 1 : 0);
        Item det = this.itemStorage().slot(DETECTOR_UPGRADE_SLOT).getResource();
        this.detectorTier = det == GCItems.QUADRUPOLE_MASS_FILTER ? 2 : (det == GCItems.DIFFRACTION_GRATING_PRISM ? 1 : 0);

        ItemResourceSlot specSlot = this.itemStorage().slot(SPECIMEN_SLOT);
        ItemResourceSlot reagentSlot = this.itemStorage().slot(REAGENT_SLOT);
        ItemResourceSlot outSlot = this.itemStorage().slot(OUTPUT_SLOT);
        FluidResourceSlot tank = this.fluidStorage().slot(REAGENT_FLUID_TANK);

        if (specSlot.isEmpty()) {
            this.progress = 0;
            this.activeAssayId = 0;
            this.reagentBoostPct = 100;
            this.nativeInSituBonus = 0;
            return GCMachineStatuses.MISSING_SPECIMEN;
        }

        Item specItem = specSlot.getResource();
        GeologicalAssay assay = lookupAssay(specItem);
        if (assay == null) {
            this.progress = 0;
            this.activeAssayId = 0;
            return MachineStatuses.INVALID_RECIPE;
        }

        int byproductCount = assay.byproductCount() * (this.detectorTier == 2 ? 2 : 1);
        if (byproductCount > 0 && !outSlot.canInsert(assay.byproduct(), DataComponentPatch.EMPTY, byproductCount)) {
            return MachineStatuses.OUTPUT_FULL;
        }

        PlanetaryEnvironment env = this.resolveEnvironment(level);
        boolean isNativePlanet = env.id().getPath().equalsIgnoreCase(assay.nativeBodyId().getPath());
        boolean hasSolidReagent = !reagentSlot.isEmpty();
        boolean hasAcid = !tank.isEmpty() && tank.getResource() == GCFluids.SULFURIC_ACID && tank.getAmount() >= ACID_CONSUME_MB * FluidUtil.MILLIBUCKET;
        boolean hasWater = !tank.isEmpty() && tank.getResource() == Fluids.WATER && tank.getAmount() >= ACID_CONSUME_MB * FluidUtil.MILLIBUCKET;

        int boost = 100;
        if (hasSolidReagent) boost += 35;
        if (hasAcid) boost += 40;
        else if (hasWater) boost += 15;
        if (isNativePlanet) boost += 50;
        if (this.detectorTier == 2) boost += 20;

        int prevAssay = this.activeAssayId;
        this.activeAssayId = assay.id();
        this.reagentBoostPct = boost;
        this.nativeInSituBonus = isNativePlanet ? 1 : 0;
        if (this.activeAssayId != prevAssay) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        if (this.progress + 1 >= ANALYZE_TICKS) {
            if (!completeAssay(level, specSlot, specItem, assay, 0.90F)) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            return GCMachineStatuses.ANALYZING_SPECIMEN;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (drawn < ENERGY_CONSUMPTION) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        this.progress++;
        return GCMachineStatuses.ANALYZING_SPECIMEN;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getActiveAssayId() {
        return this.activeAssayId;
    }

    public int getReagentBoostPct() {
        return this.reagentBoostPct;
    }

    public int getNativeInSituBonus() {
        return this.nativeInSituBonus;
    }

    public int getLastAwardedPoints() {
        return this.lastAwardedPoints;
    }

    public int getLaserTier() {
        return this.laserTier;
    }

    public int getDetectorTier() {
        return this.detectorTier;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("progress", this.progress);
        tag.putInt("active_assay_id", this.activeAssayId);
        tag.putInt("last_awarded_points", this.lastAwardedPoints);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = Math.max(0, tag.getInt("progress"));
        this.activeAssayId = tag.getInt("active_assay_id");
        this.lastAwardedPoints = tag.getInt("last_awarded_points");
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("active_assay_id", this.activeAssayId);
        return tag;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new GeologicalAnalyzerMenu(syncId, (ServerPlayer) player, this);
    }

    public record GeologicalAssay(
            int id,
            @NotNull String specimenLabel,
            @NotNull ResourceLocation nativeBodyId,
            @NotNull ResourceLocation unlockNodeId,
            @NotNull ResearchDomain domain,
            int basePoints,
            @NotNull Item byproduct,
            int byproductCount,
            int spectrumColorArgb,
            int peakWavelengthNm,
            @Nullable String milestoneId
    ) {
    }
}
