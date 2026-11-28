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

package dev.galacticraft.mod.content.vehicle.ai;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe, opt-in moving chunk ticket manager for long-range autonomous vehicles (MC 1.21.1).
 * Uses a finite-TTL {@link TicketType} ({@code 40} ticks safety net), only updates tickets
 * when crossing a {@link ChunkPos} boundary, enforces a configurable per-dimension cap,
 * and guarantees release on entity removal, dimension transfer, {@code IDLE}/{@code CHARGING},
 * or battery depletion.
 */
public final class VehicleChunkTicketManager {
    public static final int TICKET_RADIUS = 1;
    public static final int TICKET_TTL_TICKS = 40;

    public static final TicketType<ChunkPos> ROVER_TICKET = TicketType.create(
            "gc_rover",
            Comparator.comparingLong(ChunkPos::toLong),
            TICKET_TTL_TICKS
    );

    private static volatile int maxActivePerDimension = 16;
    private static final Map<ResourceKey<Level>, Set<UUID>> ACTIVE_VEHICLES_BY_DIM = new ConcurrentHashMap<>();

    private @Nullable ChunkPos loadedTicketChunk;
    private @Nullable ResourceKey<Level> loadedDimension;

    public static int getMaxActivePerDimension() {
        return maxActivePerDimension;
    }

    public static void setMaxActivePerDimension(int max) {
        maxActivePerDimension = Math.max(1, max);
    }

    public static int getActiveCount(@NotNull ResourceKey<Level> dimension) {
        Set<UUID> set = ACTIVE_VEHICLES_BY_DIM.get(dimension);
        return set == null ? 0 : set.size();
    }

    public boolean hasActiveTicket() {
        return this.loadedTicketChunk != null;
    }

    public @Nullable ChunkPos getLoadedTicketChunk() {
        return this.loadedTicketChunk;
    }

    /**
     * Acquires, moves, refreshes, or releases the moving chunk ticket according to mission state.
     */
    public void update(
            @NotNull ServerLevel level,
            @NotNull UUID vehicleId,
            @NotNull ChunkPos currentChunk,
            int tickCount,
            boolean shouldForceLoad
    ) {
        if (!shouldForceLoad) {
            this.release(level, vehicleId);
            return;
        }

        ResourceKey<Level> dim = level.dimension();
        if (this.loadedDimension != null && !this.loadedDimension.equals(dim)) {
            this.clearBookkeeping(vehicleId);
        }

        Set<UUID> dimSet = ACTIVE_VEHICLES_BY_DIM.computeIfAbsent(dim, k -> ConcurrentHashMap.newKeySet());
        if (!dimSet.contains(vehicleId) && dimSet.size() >= maxActivePerDimension) {
            return;
        }

        if (!currentChunk.equals(this.loadedTicketChunk)) {
            if (this.loadedTicketChunk != null) {
                level.getChunkSource().removeRegionTicket(ROVER_TICKET, this.loadedTicketChunk, TICKET_RADIUS, this.loadedTicketChunk);
            }
            level.getChunkSource().addRegionTicket(ROVER_TICKET, currentChunk, TICKET_RADIUS, currentChunk);
            this.loadedTicketChunk = currentChunk;
            this.loadedDimension = dim;
            dimSet.add(vehicleId);
        } else if (tickCount % 20 == 0) {
            // Refresh finite 40-tick safety-net TTL while actively moving
            level.getChunkSource().addRegionTicket(ROVER_TICKET, currentChunk, TICKET_RADIUS, currentChunk);
        }
    }

    /**
     * Mandatory cleanup hook: releases any active {@link #ROVER_TICKET} from the server chunk cache.
     */
    public void release(@NotNull ServerLevel level, @NotNull UUID vehicleId) {
        if (this.loadedTicketChunk != null) {
            level.getChunkSource().removeRegionTicket(ROVER_TICKET, this.loadedTicketChunk, TICKET_RADIUS, this.loadedTicketChunk);
            this.loadedTicketChunk = null;
        }
        this.clearBookkeeping(vehicleId);
    }

    private void clearBookkeeping(@NotNull UUID vehicleId) {
        if (this.loadedDimension != null) {
            Set<UUID> set = ACTIVE_VEHICLES_BY_DIM.get(this.loadedDimension);
            if (set != null) {
                set.remove(vehicleId);
            }
            this.loadedDimension = null;
        }
    }
}
