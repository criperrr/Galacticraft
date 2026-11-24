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
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.research.TeamResearchState;
import dev.galacticraft.mod.screen.ObservatoryTelescopeMenu;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Planetary Observatory Telescope ({@code OBSERVATORY_TELESCOPE}).
 * Supports real first-person eyepiece observation coupled directly to {@code AstroSkyEngine}
 * (planets, moons, comets, stars, nebulae, galaxies), real altitude ({@code Y}) + atmospheric
 * pressure seeing physics, scalable physical aperture structures ({@code 1x2 -> 2x2 -> 3x3}),
 * and 2 dedicated telescope-only upgrade bays (Primary Optics + Auxiliary Drive/Sensor).
 */
public class ObservatoryTelescopeBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int OPTICS_SLOT = 1;
    public static final int AUX_DRIVE_SLOT = 2;

    public static final long ENERGY_CAPACITY = 40_000L;
    public static final long ENERGY_CONSUMPTION = 40L;
    public static final int BASE_SCAN_TICKS = 100;

    public static final int TIER_EARTH_ORBITAL = 1;
    public static final int TIER_INNER_SYSTEM = 2;
    public static final int TIER_OUTER_SYSTEM = 3;

    public static final int AUX_NONE = 0;
    public static final int AUX_SIDEREAL_DRIVE = 1;
    public static final int AUX_ADAPTIVE_OPTICS = 2;
    public static final int AUX_INFRARED_BOLOMETER = 3;

    public static final List<ObservatoryTarget> TARGETS = List.of(
            new ObservatoryTarget(0, GCCelestialBodies.MOON.location(), "Moon", "MN", TIER_EARTH_ORBITAL, ResearchCatalog.EARTH_LUNAR_EPHEMERIS, 30, 0xFFD8DEE9),
            new ObservatoryTarget(1, Constant.id("low_earth_orbit"), "Low Orbit", "LO", TIER_EARTH_ORBITAL, ResearchCatalog.EARTH_LUNAR_EPHEMERIS, 25, 0xFF60A5FA),
            new ObservatoryTarget(2, GCCelestialBodies.MARS.location(), "Mars", "MR", TIER_INNER_SYSTEM, ResearchCatalog.LUNAR_DEEP_SKY_SURVEY, 45, 0xFFEF6C4D),
            new ObservatoryTarget(3, GCCelestialBodies.VENUS.location(), "Venus", "VN", TIER_INNER_SYSTEM, ResearchCatalog.LUNAR_DEEP_SKY_SURVEY, 45, 0xFFF5C563),
            new ObservatoryTarget(4, GCCelestialBodies.MERCURY.location(), "Mercury", "MC", TIER_INNER_SYSTEM, ResearchCatalog.LUNAR_DEEP_SKY_SURVEY, 45, 0xFFB0A89F),
            new ObservatoryTarget(5, GCCelestialBodies.ASTEROID.location(), "Asteroids", "AS", TIER_INNER_SYSTEM, ResearchCatalog.LUNAR_DEEP_SKY_SURVEY, 50, 0xFF94A3B8),
            new ObservatoryTarget(6, GCCelestialBodies.JUPITER.location(), "Jupiter", "JP", TIER_OUTER_SYSTEM, ResearchCatalog.OUTER_SYSTEM_EPHEMERIS, 65, 0xFFE5A46B),
            new ObservatoryTarget(7, GCCelestialBodies.SATURN.location(), "Saturn", "ST", TIER_OUTER_SYSTEM, ResearchCatalog.OUTER_SYSTEM_EPHEMERIS, 70, 0xFFE8D08A),
            new ObservatoryTarget(8, GCCelestialBodies.URANUS.location(), "Uranus", "UR", TIER_OUTER_SYSTEM, ResearchCatalog.OUTER_SYSTEM_EPHEMERIS, 80, 0xFF7BE8FF),
            new ObservatoryTarget(9, GCCelestialBodies.NEPTUNE.location(), "Neptune", "NP", TIER_OUTER_SYSTEM, ResearchCatalog.OUTER_SYSTEM_EPHEMERIS, 90, 0xFF4F7CFF)
    );

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 78)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(220, 32)
                            .capacity(1)
                            .filter((item, c) -> item == GCItems.OPTICAL_ACHROMAT_LENS
                                    || item == GCItems.SAPPHIRE_CASSEGRAIN_MIRROR
                                    || item == GCItems.LONSDALEITE_PRIMARY_MIRROR),
                    ItemResourceSlot.builder(TransferType.INPUT)
                            .pos(220, 62)
                            .capacity(1)
                            .filter((item, c) -> item == GCItems.EQUATORIAL_SIDEREAL_DRIVE
                                    || item == GCItems.ADAPTIVE_OPTICS_ACTUATOR
                                    || item == GCItems.INFRARED_BOLOMETER_ARRAY)
            ),
            MachineEnergyStorage.spec(
                    ENERGY_CAPACITY,
                    ENERGY_CONSUMPTION * 4,
                    0
            )
    );

    private int progress = 0;
    private int maxProgress = BASE_SCAN_TICKS;
    private int selectedTargetIndex = 0;
    private int opticalClarityPct = 0;
    private int maxReachTier = TIER_EARTH_ORBITAL;
    private int skyClearFlag = 0;
    private int nightOrSpaceFlag = 0;
    private int apertureTier = 1; // 1 = 1x2 Refractor (80x), 2 = 2x2 Reflector (220x), 3 = 3x3 Great Observatory (500x)
    private int opticsTier = 0;   // 0=base, 1=Achromat, 2=Sapphire Cassegrain, 3=Lonsdaleite Primary
    private int auxDriveType = AUX_NONE;
    private int airmassHundredths = 100;
    private float aimYawDeg = 0.0F;
    private float aimPitchDeg = -38.0F;

    private transient @Nullable PlanetaryEnvironment testEnvOverride = null;
    private transient @Nullable ResourceKey<Level> testDimOverride = null;
    private transient @Nullable Boolean testNightOverride = null;

    public ObservatoryTelescopeBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.OBSERVATORY_TELESCOPE, pos, state, SPEC);
    }

    public void setEnvironmentContextForTest(
            @NotNull ResourceKey<Level> dim,
            @Nullable CelestialBody<?, ?> body,
            @Nullable Boolean forceNight
    ) {
        this.testDimOverride = dim;
        this.testEnvOverride = body != null
                ? PlanetaryEnvironment.fromCelestialBody(dim.location(), body)
                : PlanetaryEnvironment.byId(dim.location());
        this.testNightOverride = forceNight;
    }

    public PlanetaryEnvironment resolveEnvironment(Level level) {
        if (this.testEnvOverride != null) {
            return this.testEnvOverride;
        }
        return PlanetaryEnvironment.fromLevel(level);
    }

    public ResourceKey<Level> resolveDimension(Level level) {
        return this.testDimOverride != null ? this.testDimOverride : level.dimension();
    }

    /**
     * Effective atmospheric airmass accounting for real planetary surface pressure and
     * observatory block altitude {@code Y}: building on mountain peaks significantly reduces distortion.
     */
    public static double computeEffectiveAirmass(@NotNull PlanetaryEnvironment env, int blockY) {
        if (env.pressureAtm() <= 0.01) {
            return 0.0;
        }
        double altAboveSea = Math.max(0.0, blockY - 64.0);
        return env.pressureAtm() * Math.exp(-altAboveSea / 95.0);
    }

    /**
     * Detects the physical observatory mount size around the telescope base:
     * <ul>
     *   <li><b>Tier 1 (1x2 Field Refractor):</b> standalone block &rarr; 80x max zoom.</li>
     *   <li><b>Tier 2 (2x2 Equatorial Reflector):</b> 3+ observatory frame/metal/glass mount blocks &rarr; 220x max zoom.</li>
     *   <li><b>Tier 3 (3x3 Great Cassegrain Observatory):</b> 8+ surrounding ring blocks &rarr; 500x max zoom.</li>
     * </ul>
     */
    public static int detectApertureTier(@NotNull Level level, @NotNull BlockPos pos) {
        int ringBlocks = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (isMountBlock(level.getBlockState(pos.offset(dx, 0, dz)).getBlock())
                        || isMountBlock(level.getBlockState(pos.offset(dx, -1, dz)).getBlock())) {
                    ringBlocks++;
                }
            }
        }
        if (ringBlocks >= 8) {
            return 3;
        }
        if (ringBlocks >= 3) {
            return 2;
        }
        return 1;
    }

    private static boolean isMountBlock(@NotNull Block block) {
        return block == GCBlocks.AIR_LOCK_FRAME
                || block == GCBlocks.VACUUM_GLASS
                || block == GCBlocks.OLIVINE_GLASS
                || block == GCBlocks.TIN_BLOCK
                || block == GCBlocks.ALUMINUM_BLOCK
                || block == GCBlocks.TITANIUM_BLOCK
                || block == GCBlocks.DESH_BLOCK
                || block == GCBlocks.FUSION_REACTOR_CASING
                || block == Blocks.IRON_BLOCK
                || block == Blocks.COPPER_BLOCK
                || block == Blocks.SMOOTH_STONE
                || block == Blocks.POLISHED_DEEPSLATE;
    }

    public static float maxZoomForAperture(int apertureTier) {
        return switch (apertureTier) {
            case 3 -> 500.0F;
            case 2 -> 220.0F;
            default -> 80.0F;
        };
    }

    public static int computeReachTier(@NotNull ResourceKey<Level> dim, @NotNull PlanetaryEnvironment env) {
        return computeReachTier(dim, env, 64, 1, 0, AUX_NONE);
    }

    public static int computeReachTier(
            @NotNull ResourceKey<Level> dim,
            @NotNull PlanetaryEnvironment env,
            int blockY,
            int apertureTier,
            int opticsTier,
            int auxType
    ) {
        if (env.pressureAtm() >= 20.0) {
            // Venus 92 atm blocks optical surface astronomy unless high altitude + IR bolometer
            return (auxType == AUX_INFRARED_BOLOMETER && blockY >= 140) ? TIER_INNER_SYSTEM : 0;
        }
        if (dim.equals(GCDimensions.MARS)
                || dim.equals(GCDimensions.ASTEROID)
                || dim.equals(GCDimensions.JUPITER)
                || dim.equals(GCDimensions.SATURN)
                || dim.equals(GCDimensions.URANUS)
                || dim.equals(GCDimensions.NEPTUNE)
                || env.gravityG() <= 0.01
                || env.orbitalDistanceAu() >= 1.4) {
            return TIER_OUTER_SYSTEM;
        }
        if (dim.equals(GCDimensions.MOON) || dim.equals(GCDimensions.MERCURY) || env.pressureAtm() <= 0.01) {
            return (apertureTier >= 3 && opticsTier >= 3) ? TIER_OUTER_SYSTEM : TIER_INNER_SYSTEM;
        }
        // On Earth, building a 3x3 Great Observatory high on a mountain peak (Y >= 160) with Sapphire/Lonsdaleite
        // optics can even resolve inner planets!
        if (apertureTier >= 3 && opticsTier >= 2 && blockY >= 160) {
            return TIER_INNER_SYSTEM;
        }
        return TIER_EARTH_ORBITAL;
    }

    public static int computeOpticalClarityPct(@NotNull PlanetaryEnvironment env, boolean isNight, boolean hasLens) {
        return computeOpticalClarityPct(env, 64, isNight, hasLens ? 1 : 0, AUX_NONE);
    }

    public static int computeOpticalClarityPct(
            @NotNull PlanetaryEnvironment env,
            int blockY,
            boolean isNight,
            int opticsTier,
            int auxType
    ) {
        double airmass = computeEffectiveAirmass(env, blockY);
        if (env.pressureAtm() >= 20.0) {
            return (auxType == AUX_INFRARED_BOLOMETER && blockY >= 140) ? 58 : 0;
        }
        int base;
        if (airmass <= 0.01) {
            base = 100;
        } else {
            double seeingPenalty = Math.min(48.0, airmass * 34.0);
            if (auxType == AUX_ADAPTIVE_OPTICS) {
                seeingPenalty *= 0.40;
            } else if (auxType == AUX_INFRARED_BOLOMETER) {
                seeingPenalty *= 0.65;
            }
            if (!isNight && airmass > 0.15) {
                base = (int) Math.round(Math.max(18.0, 32.0 - seeingPenalty * 0.35));
            } else {
                base = (int) Math.round(Math.max(35.0, 100.0 - seeingPenalty));
            }
        }
        int opticsBonus = switch (opticsTier) {
            case 3 -> 28;
            case 2 -> 18;
            case 1 -> 10;
            default -> 0;
        };
        return Mth.clamp(base + opticsBonus, 0, 100);
    }

    public void updateAimOrientation(float yawDeg, float pitchDeg) {
        this.aimYawDeg = Mth.wrapDegrees(yawDeg);
        this.aimPitchDeg = Mth.clamp(pitchDeg, -89.0F, 15.0F);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Server-validated handler for first-person eyepiece photometric captures.
     */
    public void handlePhotometricCapture(
            @NotNull ServerLevel level,
            @NotNull ServerPlayer player,
            @NotNull String objectId,
            @NotNull String displayName,
            @NotNull String category,
            float apparentMag,
            int sharpnessPct
    ) {
        if (!level.canSeeSky(this.worldPosition.above())) {
            return;
        }
        PlanetaryEnvironment env = this.resolveEnvironment(level);
        if (this.opticalClarityPct <= 0 && !player.isCreative()) {
            return;
        }

        // Consume capture burst energy (small) unless creative
        if (!player.isCreative()) {
            try (Transaction tx = Transaction.openOuter()) {
                long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION * 5, tx);
                if (drawn < ENERGY_CONSUMPTION * 5) {
                    return;
                }
                tx.commit();
            }
        }

        // Resolve matching Solar System body & ephemeris unlock if applicable
        ResourceLocation mappedBody = null;
        ResourceLocation ephemerisNode = null;
        int baseSp = 32;
        int baseEmag = 20;

        String norm = objectId.toLowerCase(Locale.ROOT);
        for (ObservatoryTarget target : TARGETS) {
            String path = target.bodyId().getPath().toLowerCase(Locale.ROOT);
            if (norm.contains(path) || displayName.toLowerCase(Locale.ROOT).contains(target.displayName().toLowerCase(Locale.ROOT))) {
                mappedBody = target.bodyId();
                ephemerisNode = target.ephemerisNodeId();
                baseSp = target.emagPoints();
                baseEmag = Math.max(12, (int) Math.round(target.emagPoints() * 0.65));
                break;
            }
        }

        String catUpper = category.toUpperCase(Locale.ROOT);
        if ("COMET".equals(catUpper)) {
            baseSp = 55;
            baseEmag = 30;
        } else if ("NEBULA".equals(catUpper) || "GALAXY".equals(catUpper) || "CLUSTER".equals(catUpper)) {
            baseSp = 60;
            baseEmag = 35;
        } else if ("STAR".equals(catUpper)) {
            baseSp = 28;
            baseEmag = 16;
        }

        // Infrared Bolometer Array doubles SP on comets, nebulae, galaxies & outer gas giants!
        if (this.auxDriveType == AUX_INFRARED_BOLOMETER
                && ("COMET".equals(catUpper) || "NEBULA".equals(catUpper) || "GALAXY".equals(catUpper)
                || "CLUSTER".equals(catUpper) || norm.contains("jupiter") || norm.contains("saturn")
                || norm.contains("uranus") || norm.contains("neptune"))) {
            baseSp *= 2;
        }

        int finalSharpness = Mth.clamp(Math.round(sharpnessPct * (0.65F + 0.35F * (this.opticalClarityPct / 100.0F))), 30, 100);
        ResearchManager.recordAstroDiscovery(
                level,
                objectId,
                displayName,
                catUpper,
                mappedBody,
                ephemerisNode,
                null,
                baseSp,
                baseEmag,
                apparentMag,
                finalSharpness,
                env.displayName()
        );
    }

    public void cycleTarget(int delta) {
        int next = Math.floorMod(this.selectedTargetIndex + delta, TARGETS.size());
        this.selectedTargetIndex = next;
        this.progress = 0;
        this.setChanged();
    }

    public void selectTargetIndex(int index) {
        if (index >= 0 && index < TARGETS.size()) {
            this.selectedTargetIndex = index;
            this.progress = 0;
            this.setChanged();
        }
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(level, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        PlanetaryEnvironment env = this.resolveEnvironment(level);
        ResourceKey<Level> dim = this.resolveDimension(level);
        boolean canSeeSky = level.canSeeSky(pos.above());
        this.skyClearFlag = canSeeSky ? 1 : 0;

        int prevAp = this.apertureTier;
        int prevOpt = this.opticsTier;
        int prevAux = this.auxDriveType;

        this.apertureTier = detectApertureTier(level, pos);

        Item opticsItem = this.itemStorage().slot(OPTICS_SLOT).getResource();
        this.opticsTier = opticsItem == GCItems.LONSDALEITE_PRIMARY_MIRROR ? 3
                : (opticsItem == GCItems.SAPPHIRE_CASSEGRAIN_MIRROR ? 2
                : (opticsItem == GCItems.OPTICAL_ACHROMAT_LENS ? 1 : 0));

        Item auxItem = this.itemStorage().slot(AUX_DRIVE_SLOT).getResource();
        this.auxDriveType = auxItem == GCItems.INFRARED_BOLOMETER_ARRAY ? AUX_INFRARED_BOLOMETER
                : (auxItem == GCItems.ADAPTIVE_OPTICS_ACTUATOR ? AUX_ADAPTIVE_OPTICS
                : (auxItem == GCItems.EQUATORIAL_SIDEREAL_DRIVE ? AUX_SIDEREAL_DRIVE : AUX_NONE));

        if (this.apertureTier != prevAp || this.opticsTier != prevOpt || this.auxDriveType != prevAux) {
            this.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        double airmass = computeEffectiveAirmass(env, pos.getY());
        this.airmassHundredths = (int) Math.round(airmass * 100.0);

        boolean vacuumSky = env.pressureAtm() <= 0.01;
        long dayTime = level.getDayTime() % 24000L;
        boolean isNight = this.testNightOverride != null
                ? this.testNightOverride
                : (vacuumSky || (dayTime >= 12500L && dayTime <= 23500L));
        this.nightOrSpaceFlag = isNight ? 1 : 0;

        this.maxReachTier = computeReachTier(dim, env, pos.getY(), this.apertureTier, this.opticsTier, this.auxDriveType);
        this.opticalClarityPct = canSeeSky
                ? computeOpticalClarityPct(env, pos.getY(), isNight, this.opticsTier, this.auxDriveType)
                : 0;

        if (!canSeeSky) {
            this.progress = 0;
            return GCMachineStatuses.SKY_OBSTRUCTED;
        }
        if (this.maxReachTier <= 0) {
            this.progress = 0;
            return GCMachineStatuses.ATMOSPHERE_TOO_DENSE;
        }
        if (!vacuumSky && env.pressureAtm() >= 0.5 && !isNight && this.auxDriveType != AUX_INFRARED_BOLOMETER) {
            this.progress = 0;
            return GCMachineStatuses.WAITING_FOR_NIGHT;
        }

        // Automated ephemeris tracking fallback (also powers GameTests & passive tracking with Equatorial Drive)
        TeamResearchState research = ResearchManager.get(level);
        ObservatoryTarget target = TARGETS.get(Math.floorMod(this.selectedTargetIndex, TARGETS.size()));
        if (target.requiredTier() > this.maxReachTier) {
            ObservatoryTarget reachable = findFirstReachableTarget(research, this.maxReachTier);
            if (reachable != null) {
                this.selectedTargetIndex = reachable.index();
                target = reachable;
            } else {
                this.progress = 0;
                return GCMachineStatuses.ATMOSPHERE_TOO_DENSE;
            }
        }
        if (research.isBodyMapped(target.bodyId())) {
            ObservatoryTarget nextUnmapped = findFirstReachableUnmappedTarget(research, this.maxReachTier);
            if (nextUnmapped != null) {
                this.selectedTargetIndex = nextUnmapped.index();
                target = nextUnmapped;
            }
        }

        int speedDivisor = 1 + this.opticsTier + (this.auxDriveType == AUX_SIDEREAL_DRIVE ? 1 : 0);
        int clarityFactor = Math.max(40, this.opticalClarityPct);
        this.maxProgress = Math.max(20, (BASE_SCAN_TICKS * 100) / (clarityFactor * speedDivisor));

        try (Transaction tx = Transaction.openOuter()) {
            long drawn = this.energyStorage().extract(ENERGY_CONSUMPTION, tx);
            if (drawn < ENERGY_CONSUMPTION) {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
            tx.commit();
        }

        if (this.progress + 1 >= this.maxProgress) {
            boolean alreadyMapped = research.isBodyMapped(target.bodyId());
            int points = alreadyMapped ? Math.max(3, target.emagPoints() / 8) : Math.max(12, target.emagPoints() / 2);
            ResearchManager.recordMappedCelestialBody(
                    level,
                    target.bodyId(),
                    target.displayName(),
                    target.ephemerisNodeId(),
                    points,
                    env.displayName()
            );
            this.progress = 0;
            ObservatoryTarget next = findFirstReachableUnmappedTarget(research, this.maxReachTier);
            if (next != null) {
                this.selectedTargetIndex = next.index();
            }
            this.setChanged();
            return GCMachineStatuses.OBSERVING_SKY;
        }

        this.progress++;
        return GCMachineStatuses.OBSERVING_SKY;
    }

    private static @Nullable ObservatoryTarget findFirstReachableUnmappedTarget(TeamResearchState state, int maxTier) {
        for (ObservatoryTarget t : TARGETS) {
            if (t.requiredTier() <= maxTier && !state.isBodyMapped(t.bodyId())) {
                return t;
            }
        }
        return null;
    }

    private static @Nullable ObservatoryTarget findFirstReachableTarget(TeamResearchState state, int maxTier) {
        ObservatoryTarget unmapped = findFirstReachableUnmappedTarget(state, maxTier);
        if (unmapped != null) {
            return unmapped;
        }
        for (ObservatoryTarget t : TARGETS) {
            if (t.requiredTier() <= maxTier) {
                return t;
            }
        }
        return null;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getMaxProgress() {
        return this.maxProgress;
    }

    public int getSelectedTargetIndex() {
        return this.selectedTargetIndex;
    }

    public int getOpticalClarityPct() {
        return this.opticalClarityPct;
    }

    public int getMaxReachTier() {
        return this.maxReachTier;
    }

    public int getSkyClearFlag() {
        return this.skyClearFlag;
    }

    public int getNightOrSpaceFlag() {
        return this.nightOrSpaceFlag;
    }

    public int getApertureTier() {
        return this.apertureTier;
    }

    public int getOpticsTier() {
        return this.opticsTier;
    }

    public int getAuxDriveType() {
        return this.auxDriveType;
    }

    public int getAirmassHundredths() {
        return this.airmassHundredths;
    }

    public float getAimYawDeg() {
        return this.aimYawDeg;
    }

    public float getAimPitchDeg() {
        return this.aimPitchDeg;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("progress", this.progress);
        tag.putInt("max_progress", this.maxProgress);
        tag.putInt("selected_target", this.selectedTargetIndex);
        tag.putInt("aperture_tier", this.apertureTier);
        tag.putInt("optics_tier", this.opticsTier);
        tag.putInt("aux_drive_type", this.auxDriveType);
        tag.putFloat("aim_yaw", this.aimYawDeg);
        tag.putFloat("aim_pitch", this.aimPitchDeg);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        this.progress = Math.max(0, tag.getInt("progress"));
        this.maxProgress = tag.contains("max_progress") ? Math.max(1, tag.getInt("max_progress")) : BASE_SCAN_TICKS;
        this.selectedTargetIndex = Math.floorMod(tag.getInt("selected_target"), TARGETS.size());
        this.apertureTier = Mth.clamp(tag.contains("aperture_tier") ? tag.getInt("aperture_tier") : 1, 1, 3);
        this.opticsTier = Mth.clamp(tag.getInt("optics_tier"), 0, 3);
        this.auxDriveType = Mth.clamp(tag.getInt("aux_drive_type"), 0, 3);
        if (tag.contains("aim_yaw")) {
            this.aimYawDeg = tag.getFloat("aim_yaw");
        }
        if (tag.contains("aim_pitch")) {
            this.aimPitchDeg = tag.getFloat("aim_pitch");
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("aperture_tier", this.apertureTier);
        tag.putInt("optics_tier", this.opticsTier);
        tag.putInt("aux_drive_type", this.auxDriveType);
        tag.putFloat("aim_yaw", this.aimYawDeg);
        tag.putFloat("aim_pitch", this.aimPitchDeg);
        return tag;
    }

    @Override
    public @Nullable MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new ObservatoryTelescopeMenu(syncId, (ServerPlayer) player, this);
    }

    public record ObservatoryTarget(
            int index,
            @NotNull ResourceLocation bodyId,
            @NotNull String displayName,
            @NotNull String shortCode,
            int requiredTier,
            @NotNull ResourceLocation ephemerisNodeId,
            int emagPoints,
            int orbColorArgb
    ) {
    }
}
