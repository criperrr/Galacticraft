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

package dev.galacticraft.mod.content.rocket.launch;

import dev.galacticraft.api.rocket.assembly.MountedPartNode;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Atomic state transition manager converting a static Master-Slave modular rocket on a launchpad
 * into a live {@link ModularRocketFlightEntity} at {@code T = 0.00s} (and back into static blocks
 * upon safe propulsive touchdown).
 *
 * <p>Enforces the strict rollback protocol:
 * <ol>
 *   <li>Snapshot {@link RocketAssemblyGraph}, per-stage fuel, energy, oxygen, science drives, and crew UUIDs.</li>
 *   <li>Execute {@code level.addFreshEntity(flightEntity)} <b>before</b> mutating or removing any block.
 *       If entity addition fails, abort immediately with zero world mutations.</li>
 *   <li>Unlink all proxy voxels via {@link MultiblockLinkManager#unlinkAll} first, then clear all proxy and
 *       master blocks using {@code Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS}
 *       (flag 50: zero ghost blocks, zero neighbor shape storms, zero item drops/duplication).</li>
 *   <li>Mount pilot and all multi-crew passengers into resolved vehicle seats.</li>
 * </ol>
 */
public final class RocketStateConverter {
    /**
     * Exact Minecraft block update flags for atomic T-0 block clearing:
     * {@code UPDATE_CLIENTS (2) | UPDATE_KNOWN_SHAPE (16) | UPDATE_SUPPRESS_DROPS (32) = 50}.
     */
    public static final int ATOMIC_CLEAR_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /**
     * Minimum natural planetary terrain block hardness required under the Landing Leg Foundation Exemption.
     */
    public static final float LANDING_LEG_MIN_TERRAIN_HARDNESS = 0.50F;

    private RocketStateConverter() {
    }

    /**
     * Atomically converts the static modular rocket assembled at {@code padCenter} into a live
     * {@link ModularRocketFlightEntity} at {@code T = 0.00s}.
     */
    public static @Nullable ModularRocketFlightEntity convertToFlightEntity(
            @NotNull ServerLevel level,
            @NotNull BlockPos padCenter,
            @NotNull Direction padFacing,
            @Nullable BlockPos originTerminalPos,
            @NotNull RocketAssemblyGraph graph,
            long energyGj,
            long oxygenDroplets,
            @NotNull List<BlockPos> assembledPartPositions,
            @NotNull List<UUID> passengerUuids
    ) {
        return convertToFlightEntity(
                level,
                padCenter,
                padFacing,
                originTerminalPos,
                graph,
                energyGj,
                oxygenDroplets,
                assembledPartPositions,
                passengerUuids,
                level::addFreshEntity
        );
    }

    /**
     * Overload accepting an explicit {@code entitySpawner} predicate (defaults to {@code level::addFreshEntity})
     * to permit deterministic rollback failure-injection verification in automated tests.
     */
    public static @Nullable ModularRocketFlightEntity convertToFlightEntity(
            @NotNull ServerLevel level,
            @NotNull BlockPos padCenter,
            @NotNull Direction padFacing,
            @Nullable BlockPos originTerminalPos,
            @NotNull RocketAssemblyGraph graph,
            long energyGj,
            long oxygenDroplets,
            @NotNull List<BlockPos> assembledPartPositions,
            @NotNull List<UUID> passengerUuids,
            @NotNull Predicate<ModularRocketFlightEntity> entitySpawner
    ) {
        if (graph.isEmpty() || assembledPartPositions.isEmpty()) {
            return null;
        }

        // 1. Snapshot science drives & proxy block positions BEFORE touching world state
        Map<BlockPos, ItemStack> scienceDrives = new LinkedHashMap<>();
        Set<BlockPos> allProxies = new LinkedHashSet<>();
        List<BlockPos> validMasters = new ArrayList<>(assembledPartPositions.size());
        int minMasterY = Integer.MAX_VALUE;

        for (BlockPos partPos : assembledPartPositions) {
            if (!level.isLoaded(partPos)) {
                continue;
            }
            if (level.getBlockEntity(partPos) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
                validMasters.add(partPos.immutable());
                minMasterY = Math.min(minMasterY, partPos.getY());
                if (!partBe.getScienceDrive().isEmpty()) {
                    scienceDrives.put(partPos.subtract(padCenter).immutable(), partBe.getScienceDrive().copy());
                }
                allProxies.addAll(partBe.getOccupiedProxies());
            }
        }
        if (validMasters.isEmpty()) {
            return null;
        }

        double spawnY = (minMasterY != Integer.MAX_VALUE ? minMasterY : (padCenter.getY() + 1)) + 0.05;
        ModularRocketFlightEntity flightEntity = new ModularRocketFlightEntity(GCEntityTypes.MODULAR_ROCKET_FLIGHT, level);
        flightEntity.setPos(padCenter.getX() + 0.5, spawnY, padCenter.getZ() + 0.5);
        flightEntity.initializeFlightState(
                graph,
                energyGj,
                oxygenDroplets,
                padFacing,
                originTerminalPos,
                padCenter.getY(),
                ModularRocketFlightEntity.FlightPhase.ASCENT,
                passengerUuids,
                scienceDrives
        );

        // 2. STRICT ROLLBACK ORDER: Spawn entity into the world BEFORE mutating or clearing any block!
        if (!entitySpawner.test(flightEntity)) {
            return null;
        }

        // 3. Entity spawn certified successful -> unlink proxies & atomically clear blocks with flag 50
        if (!allProxies.isEmpty()) {
            MultiblockLinkManager.unlinkAll(level, allProxies);
        }
        for (BlockPos masterPos : validMasters) {
            if (level.getBlockEntity(masterPos) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
                partBe.clearAssembledTerminalPosQuiet();
            }
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos proxyPos : allProxies) {
            if (level.isLoaded(proxyPos)) {
                level.setBlock(proxyPos, air, ATOMIC_CLEAR_FLAGS);
            }
        }
        for (BlockPos masterPos : validMasters) {
            if (level.isLoaded(masterPos)) {
                level.setBlock(masterPos, air, ATOMIC_CLEAR_FLAGS);
            }
        }

        // 4. Mount pilot + all multi-crew passengers into resolved rocket seats
        for (UUID uuid : passengerUuids) {
            if (uuid != null && level.getPlayerByUUID(uuid) instanceof ServerPlayer player && player.isAlive()) {
                player.startRiding(flightEntity, true);
            }
        }

        return flightEntity;
    }

    /**
     * Atomically reconverts a live {@link ModularRocketFlightEntity} back into static Master-Slave
     * {@link ModularRocketPartBlock}s upon safe propulsive touchdown ({@code v <= 5.00 m/s}).
     *
     * <p>Evaluates the <b>Landing Leg Foundation Exemption</b>: vehicles with {@code landing_gear_*}
     * may land directly on solid extraterrestrial terrain with block hardness {@code >= 0.50F},
     * bypassing the {@code ReinforcedLaunchPadBlock} requirement.
     *
     * @return {@code true} if the static structure was successfully reconstructed and the entity discarded.
     */
    public static boolean convertToStaticBlocks(
            @NotNull ServerLevel level,
            @NotNull BlockPos landingBasePos,
            @NotNull ModularRocketFlightEntity flightEntity
    ) {
        RocketAssemblyGraph graph = flightEntity.getAssemblyGraph();
        List<MountedPartNode> activeParts = graph.allActiveParts();
        if (activeParts.isEmpty()) {
            return false;
        }

        // 1. Validate foundation (Landing Leg Foundation Exemption vs Reinforced Launch Pad)
        BlockPos groundPos = landingBasePos.below();
        BlockState groundState = level.getBlockState(groundPos);
        boolean isPadFoundation = groundState.is(GCBlocks.REINFORCED_LAUNCH_PAD)
                || groundState.is(GCBlocks.ROCKET_LAUNCH_PAD)
                || groundState.is(GCBlocks.PLATFORM_PROXY);

        if (!isPadFoundation) {
            if (!graph.hasLandingGear()) {
                return false;
            }
            // Landing Leg Foundation Exemption: requires solid natural terrain with hardness >= 0.50F
            float hardness = groundState.getDestroySpeed(level, groundPos);
            if (groundState.isAir() || !groundState.getFluidState().isEmpty() || (hardness >= 0.0F && hardness < LANDING_LEG_MIN_TERRAIN_HARDNESS)) {
                return false;
            }
        }

        // 2. Normalize lowest part Y to landingBasePos.getY()
        int minPartY = Integer.MAX_VALUE;
        for (MountedPartNode part : activeParts) {
            minPartY = Math.min(minPartY, part.relativePos().getY());
        }
        int yShift = (minPartY != Integer.MAX_VALUE) ? (landingBasePos.getY() - minPartY) : landingBasePos.getY();

        // Capture passengers before dismounting so we can place them safely beside/inside the landed vehicle
        List<Entity> passengers = new ArrayList<>(flightEntity.getPassengers());
        Direction facing = flightEntity.getPadFacing();
        Map<BlockPos, ItemStack> scienceDrives = flightEntity.getPartScienceDrives();
        List<BlockPos> placedMasters = new ArrayList<>(activeParts.size());

        // 3. Reconstruct every active Master part + its multi-voxel proxy footprint
        for (MountedPartNode part : activeParts) {
            BlockPos targetPos = new BlockPos(
                    landingBasePos.getX() + part.relativePos().getX(),
                    part.relativePos().getY() + yShift,
                    landingBasePos.getZ() + part.relativePos().getZ()
            ).immutable();
            if (!level.isInWorldBounds(targetPos)) {
                continue;
            }
            Block partBlock = ModularRocketPartBlock.getBlockForCategory(part.config().category());
            BlockState masterState = partBlock.defaultBlockState()
                    .setValue(ModularRocketPartBlock.FACING, facing)
                    .setValue(ModularRocketPartBlock.HATCH_OPEN, false);
            level.setBlock(targetPos, masterState, Block.UPDATE_ALL);

            if (level.getBlockEntity(targetPos) instanceof ModularRocketPartBlockEntity partBe) {
                partBe.initializePart(
                        part.config(),
                        part.stats(),
                        part.integrity(),
                        part.composition().orElse(null),
                        facing
                );
                ItemStack drive = scienceDrives.get(part.relativePos());
                if (drive != null && !drive.isEmpty()) {
                    partBe.setScienceDrive(drive);
                }
                placedMasters.add(targetPos);
            }
        }

        if (placedMasters.isEmpty()) {
            return false;
        }

        // 4. If landed back on a formed HangarTerminal's launchpad, restore exact per-stage fuel, energy & oxygen
        HangarTerminalBlockEntity nearbyTerminal = findFormedTerminalForPad(level, groundPos);
        if (nearbyTerminal != null) {
            nearbyTerminal.adoptLandedModularRocket(
                    placedMasters,
                    graph,
                    flightEntity.getStoredEnergyGj(),
                    flightEntity.getStoredOxygenDroplets()
            );
        }

        // 5. Safely dismount all passengers onto solid ground beside the rocket and discard flight entity
        flightEntity.ejectPassengers();
        double safeOffset = Math.max(2.0, graph.maxRadiusBlocks() + 1.25);
        for (int i = 0; i < passengers.size(); i++) {
            Entity passenger = passengers.get(i);
            double angle = (Math.PI * 2.0 * i) / Math.max(1, passengers.size());
            double px = landingBasePos.getX() + 0.5 + Math.cos(angle) * safeOffset;
            double pz = landingBasePos.getZ() + 0.5 + Math.sin(angle) * safeOffset;
            passenger.setDeltaMovement(0.0, 0.0, 0.0);
            passenger.fallDistance = 0.0F;
            passenger.teleportTo(px, landingBasePos.getY() + 0.5, pz);
        }

        flightEntity.discard();
        return true;
    }

    private static @Nullable HangarTerminalBlockEntity findFormedTerminalForPad(ServerLevel level, BlockPos groundPos) {
        BlockPos master = MultiblockLinkManager.getMasterPos(level, groundPos);
        if (master != null && level.getBlockEntity(master) instanceof HangarTerminalBlockEntity terminal && terminal.isFormed()) {
            return terminal;
        }
        if (level.getBlockEntity(groundPos) instanceof HangarTerminalBlockEntity terminal && terminal.isFormed()) {
            return terminal;
        }
        return null;
    }
}
