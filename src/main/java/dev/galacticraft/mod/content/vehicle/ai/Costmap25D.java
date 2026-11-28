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

import dev.galacticraft.mod.content.entity.vehicle.GCVehicle;
import dev.galacticraft.mod.content.entity.vehicle.VehiclePartEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * NASA JPL ENav-inspired 2.5D planetary surface costmap with pre-inflated entity footprint.
 * Stores flat primitive arrays ({@code short[]} heights and {@code byte[]} traversal costs)
 * so budgeted A* / Theta* expansion evaluates wide-entity clearance in O(1) per cell.
 */
public final class Costmap25D {
    public static final byte IMPASSABLE = 0;
    public static final byte BASE_FLAT = 10;
    public static final byte LOW_TRACTION = 25;
    public static final short NO_SURFACE = Short.MIN_VALUE;

    public static final double SLOPE_UP_PENALTY = 2.50;
    public static final double SLOPE_DOWN_PENALTY = 0.50;
    public static final int DEFAULT_PADDING = 16;
    public static final int MAX_SEARCH_RADIUS = 96;
    public static final int VERTICAL_SCAN_RANGE = 12;

    private final int minX;
    private final int minZ;
    private final int sizeX;
    private final int sizeZ;
    private final int inflationRadius;
    private final short[] surfaceY;
    private final byte[] rawCost;
    private final byte[] inflatedCost;

    private Costmap25D(
            int minX,
            int minZ,
            int sizeX,
            int sizeZ,
            int inflationRadius,
            short[] surfaceY,
            byte[] rawCost,
            byte[] inflatedCost
    ) {
        this.minX = minX;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.inflationRadius = inflationRadius;
        this.surfaceY = surfaceY;
        this.rawCost = rawCost;
        this.inflatedCost = inflatedCost;
    }

    /**
     * Builds an immutable 2.5D costmap snapshot on the main server thread around {@code start} and {@code goal},
     * including footprints of other vehicles in the search envelope.
     */
    public static @NotNull Costmap25D buildFromLevel(
            @NotNull ServerLevel level,
            @NotNull BlockPos start,
            @NotNull BlockPos goal,
            float entityWidth,
            float entityHeight,
            @NotNull Map<BlockPos, Long> tempBlacklist,
            long gameTime
    ) {
        return buildFromLevel(level, null, start, goal, entityWidth, entityHeight, tempBlacklist, gameTime);
    }

    /**
     * Builds an immutable 2.5D costmap snapshot on the main server thread around {@code start} and {@code goal},
     * stamping other {@link GCVehicle} and {@link VehiclePartEntity} hitboxes as impassable before footprint inflation.
     */
    public static @NotNull Costmap25D buildFromLevel(
            @NotNull ServerLevel level,
            @Nullable Entity selfEntity,
            @NotNull BlockPos start,
            @NotNull BlockPos goal,
            float entityWidth,
            float entityHeight,
            @NotNull Map<BlockPos, Long> tempBlacklist,
            long gameTime
    ) {
        int midX = (start.getX() + goal.getX()) >> 1;
        int midZ = (start.getZ() + goal.getZ()) >> 1;
        int minX = Math.max(midX - MAX_SEARCH_RADIUS, Math.min(start.getX(), goal.getX()) - DEFAULT_PADDING);
        int maxX = Math.min(midX + MAX_SEARCH_RADIUS, Math.max(start.getX(), goal.getX()) + DEFAULT_PADDING);
        int minZ = Math.max(midZ - MAX_SEARCH_RADIUS, Math.min(start.getZ(), goal.getZ()) - DEFAULT_PADDING);
        int maxZ = Math.min(midZ + MAX_SEARCH_RADIUS, Math.max(start.getZ(), goal.getZ()) + DEFAULT_PADDING);

        int sizeX = Math.max(1, maxX - minX + 1);
        int sizeZ = Math.max(1, maxZ - minZ + 1);
        int total = sizeX * sizeZ;
        short[] heights = new short[total];
        byte[] raw = new byte[total];
        Arrays.fill(heights, NO_SURFACE);

        int headroom = Math.max(2, Mth.ceil(entityHeight));
        int refY = start.getY();
        int minScanY = Math.max(level.getMinBuildHeight() + 1, Math.min(start.getY(), goal.getY()) - VERTICAL_SCAN_RANGE);
        int maxScanY = Math.min(level.getMaxBuildHeight() - headroom - 1, Math.max(start.getY(), goal.getY()) + VERTICAL_SCAN_RANGE);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int gx = 0; gx < sizeX; gx++) {
            int wx = minX + gx;
            for (int gz = 0; gz < sizeZ; gz++) {
                int wz = minZ + gz;
                int idx = gx * sizeZ + gz;
                if (!level.isLoaded(cursor.set(wx, refY, wz))) {
                    raw[idx] = IMPASSABLE;
                    continue;
                }

                int bestY = Integer.MIN_VALUE;
                int bestDist = Integer.MAX_VALUE;
                BlockState bestFloor = null;

                for (int y = minScanY; y <= maxScanY; y++) {
                    cursor.set(wx, y - 1, wz);
                    BlockState below = level.getBlockState(cursor);
                    if (!isStandableFloor(level, cursor, below)) {
                        continue;
                    }
                    if (!hasClearHeadroom(level, cursor, wx, y, wz, headroom)) {
                        continue;
                    }
                    int dist = Math.abs(y - refY);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestY = y;
                        bestFloor = below;
                    }
                }

                if (bestY != Integer.MIN_VALUE && bestFloor != null) {
                    heights[idx] = (short) bestY;
                    raw[idx] = classifySurfaceCost(bestFloor);
                } else {
                    heights[idx] = NO_SURFACE;
                    raw[idx] = IMPASSABLE;
                }
            }
        }

        // Stamp footprints of other vehicles (including multi-part hitboxes) into rawCost BEFORE footprint inflation
        AABB searchBox = new AABB(minX, minScanY, minZ, maxX + 1.0, maxScanY + headroom, maxZ + 1.0);
        List<Entity> otherVehicles = level.getEntities(selfEntity, searchBox, e ->
                !e.isRemoved()
                        && (e instanceof GCVehicle || e instanceof VehiclePartEntity)
                        && (selfEntity == null || (!e.is(selfEntity) && !selfEntity.hasPassenger(e)))
        );
        for (Entity other : otherVehicles) {
            AABB bb = other.getBoundingBox();
            int bx0 = Math.max(0, Mth.floor(bb.minX) - minX);
            int bx1 = Math.min(sizeX - 1, Mth.floor(bb.maxX) - minX);
            int bz0 = Math.max(0, Mth.floor(bb.minZ) - minZ);
            int bz1 = Math.min(sizeZ - 1, Mth.floor(bb.maxZ) - minZ);
            for (int gx = bx0; gx <= bx1; gx++) {
                for (int gz = bz0; gz <= bz1; gz++) {
                    raw[gx * sizeZ + gz] = IMPASSABLE;
                }
            }
        }

        int inflationRadius = Math.max(1, Mth.ceil(entityWidth / 2.0F));
        byte[] inflated = inflateFootprint(sizeX, sizeZ, heights, raw, inflationRadius);

        // Apply temporary obstacle blacklist (e.g., from STUCK_RECOVERY)
        for (Map.Entry<BlockPos, Long> entry : tempBlacklist.entrySet()) {
            if (entry.getValue() <= gameTime) {
                continue;
            }
            BlockPos bp = entry.getKey();
            int gx = bp.getX() - minX;
            int gz = bp.getZ() - minZ;
            if (gx >= 0 && gx < sizeX && gz >= 0 && gz < sizeZ) {
                inflated[gx * sizeZ + gz] = IMPASSABLE;
            }
        }

        return new Costmap25D(minX, minZ, sizeX, sizeZ, inflationRadius, heights, raw, inflated);
    }

    /**
     * Pure-Java factory for deterministic unit tests without requiring a running Minecraft server.
     */
    public static @NotNull Costmap25D fromSyntheticGrid(
            int minX,
            int minZ,
            int sizeX,
            int sizeZ,
            short[] surfaceY,
            byte[] rawCost,
            int inflationRadius
    ) {
        int expected = sizeX * sizeZ;
        if (surfaceY.length != expected || rawCost.length != expected) {
            throw new IllegalArgumentException("Grid arrays must have length sizeX * sizeZ");
        }
        short[] yCopy = Arrays.copyOf(surfaceY, expected);
        byte[] rawCopy = Arrays.copyOf(rawCost, expected);
        byte[] inflated = inflateFootprint(sizeX, sizeZ, yCopy, rawCopy, Math.max(0, inflationRadius));
        return new Costmap25D(minX, minZ, sizeX, sizeZ, Math.max(0, inflationRadius), yCopy, rawCopy, inflated);
    }

    private static byte[] inflateFootprint(int sizeX, int sizeZ, short[] heights, byte[] raw, int radius) {
        byte[] inflated = Arrays.copyOf(raw, raw.length);
        if (radius <= 0) {
            return inflated;
        }
        for (int gx = 0; gx < sizeX; gx++) {
            for (int gz = 0; gz < sizeZ; gz++) {
                int idx = gx * sizeZ + gz;
                if (raw[idx] == IMPASSABLE || heights[idx] == NO_SURFACE) {
                    inflated[idx] = IMPASSABLE;
                    continue;
                }
                int centerY = heights[idx];
                boolean blocked = false;
                for (int dx = -radius; dx <= radius && !blocked; dx++) {
                    int nx = gx + dx;
                    for (int dz = -radius; dz <= radius; dz++) {
                        int nz = gz + dz;
                        if (nx < 0 || nx >= sizeX || nz < 0 || nz >= sizeZ) {
                            blocked = true;
                            break;
                        }
                        int nIdx = nx * sizeZ + nz;
                        if (raw[nIdx] == IMPASSABLE || heights[nIdx] == NO_SURFACE) {
                            blocked = true;
                            break;
                        }
                        if (Math.abs(heights[nIdx] - centerY) > 1) {
                            blocked = true;
                            break;
                        }
                    }
                }
                if (blocked) {
                    inflated[idx] = IMPASSABLE;
                }
            }
        }
        return inflated;
    }

    public static boolean isStandableFloor(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state) {
        if (!state.getFluidState().isEmpty() || isHazardousBlock(state)) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean hasClearHeadroom(
            @NotNull ServerLevel level,
            @NotNull BlockPos.MutableBlockPos cursor,
            int wx,
            int baseY,
            int wz,
            int headroom
    ) {
        for (int h = 0; h < headroom; h++) {
            cursor.set(wx, baseY + h, wz);
            BlockState st = level.getBlockState(cursor);
            if (!st.getFluidState().isEmpty() || isHazardousBlock(st)) {
                return false;
            }
            if (!st.getCollisionShape(level, cursor).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isHazardousBlock(@NotNull BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.LAVA
                || block == Blocks.MAGMA_BLOCK
                || block == Blocks.FIRE
                || block == Blocks.SOUL_FIRE
                || block == Blocks.CAMPFIRE
                || block == Blocks.SOUL_CAMPFIRE
                || block == Blocks.CACTUS
                || block == Blocks.SWEET_BERRY_BUSH
                || block == Blocks.WITHER_ROSE
                || block == Blocks.POWDER_SNOW) {
            return true;
        }
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase(Locale.ROOT);
        return path.contains("acid") || path.contains("lava") || path.contains("magma");
    }

    public static byte classifySurfaceCost(@NotNull BlockState floorState) {
        Block block = floorState.getBlock();
        if (block == Blocks.ICE
                || block == Blocks.PACKED_ICE
                || block == Blocks.BLUE_ICE
                || block == Blocks.FROSTED_ICE
                || block == Blocks.SNOW_BLOCK
                || block == Blocks.SAND
                || block == Blocks.RED_SAND
                || block == Blocks.GRAVEL
                || block == Blocks.SOUL_SAND) {
            return LOW_TRACTION;
        }
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase(Locale.ROOT);
        if (path.contains("ice") || path.contains("snow") || path.contains("regolith") || path.contains("sand") || path.contains("dust")) {
            return LOW_TRACTION;
        }
        return BASE_FLAT;
    }

    public int minX() {
        return this.minX;
    }

    public int minZ() {
        return this.minZ;
    }

    public int sizeX() {
        return this.sizeX;
    }

    public int sizeZ() {
        return this.sizeZ;
    }

    public int inflationRadius() {
        return this.inflationRadius;
    }

    public boolean inBounds(int gx, int gz) {
        return gx >= 0 && gx < this.sizeX && gz >= 0 && gz < this.sizeZ;
    }

    public int toGridX(int worldX) {
        return worldX - this.minX;
    }

    public int toGridZ(int worldZ) {
        return worldZ - this.minZ;
    }

    public int toWorldX(int gx) {
        return this.minX + gx;
    }

    public int toWorldZ(int gz) {
        return this.minZ + gz;
    }

    public int index(int gx, int gz) {
        return gx * this.sizeZ + gz;
    }

    public short surfaceY(int gx, int gz) {
        return this.surfaceY[gx * this.sizeZ + gz];
    }

    public byte rawCost(int gx, int gz) {
        return this.rawCost[gx * this.sizeZ + gz];
    }

    public byte inflatedCost(int gx, int gz) {
        return this.inflatedCost[gx * this.sizeZ + gz];
    }

    public boolean isInflatedPassable(int gx, int gz) {
        return inBounds(gx, gz) && this.inflatedCost[gx * this.sizeZ + gz] > IMPASSABLE;
    }

    public boolean isRawPassable(int gx, int gz) {
        return inBounds(gx, gz) && this.rawCost[gx * this.sizeZ + gz] > IMPASSABLE;
    }

    /**
     * Evaluates edge traversal cost from {@code (gx, gz)} to neighbor {@code (nx, nz)}.
     * Returns {@link Double#POSITIVE_INFINITY} if the transition is impassable or drops into a crater
     * ({@code dy < -1}) or hits a cliff ({@code dy > 1}).
     */
    public double transitionCost(int gx, int gz, int nx, int nz, boolean allowRawStartEscape) {
        if (!inBounds(nx, nz)) {
            return Double.POSITIVE_INFINITY;
        }
        byte nCost = this.inflatedCost(nx, nz);
        boolean usedRawEscape = false;
        if (nCost == IMPASSABLE) {
            if (allowRawStartEscape && this.rawCost(nx, nz) > IMPASSABLE) {
                nCost = this.rawCost(nx, nz);
                usedRawEscape = true;
            } else {
                return Double.POSITIVE_INFINITY;
            }
        }
        int curY = this.surfaceY(gx, gz);
        int nextY = this.surfaceY(nx, nz);
        if (curY == NO_SURFACE || nextY == NO_SURFACE) {
            return Double.POSITIVE_INFINITY;
        }
        int dy = nextY - curY;
        if (dy > 1 || dy < -1) {
            return Double.POSITIVE_INFINITY;
        }

        int dx = nx - gx;
        int dz = nz - gz;
        boolean diagonal = dx != 0 && dz != 0;
        if (diagonal) {
            if (!isStepNeighborPassable(gx + dx, gz, curY, allowRawStartEscape)
                    || !isStepNeighborPassable(gx, gz + dz, curY, allowRawStartEscape)) {
                return Double.POSITIVE_INFINITY;
            }
        }

        double dist = diagonal ? 1.41421356 : 1.0;
        double slopePenalty = dy == 1 ? SLOPE_UP_PENALTY : (dy == -1 ? SLOPE_DOWN_PENALTY : 0.0);
        double terrainFactor = nCost / 10.0;
        double escapePenalty = usedRawEscape ? 6.0 : 0.0;
        return dist * terrainFactor + slopePenalty + escapePenalty;
    }

    private boolean isStepNeighborPassable(int gx, int gz, int refY, boolean allowRaw) {
        if (!inBounds(gx, gz)) {
            return false;
        }
        byte cost = allowRaw ? this.rawCost(gx, gz) : this.inflatedCost(gx, gz);
        if (cost == IMPASSABLE) {
            return false;
        }
        int y = this.surfaceY(gx, gz);
        return y != NO_SURFACE && Math.abs(y - refY) <= 1;
    }

    /**
     * Supercover 2.5D Bresenham line-of-sight check over the inflated grid.
     * Guarantees every traversed cell (including orthogonal corner guards on diagonal steps)
     * is inflated-passable with consecutive height delta at most 1 block and no low-traction shortcuts
     * skipped illegally.
     */
    public boolean hasInflatedLineOfSight(int x0, int z0, int x1, int z1) {
        if (!isInflatedPassable(x0, z0) || !isInflatedPassable(x1, z1)) {
            return false;
        }
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx - dz;

        int cx = x0;
        int cz = z0;
        int prevY = this.surfaceY(cx, cz);

        while (cx != x1 || cz != z1) {
            int e2 = err << 1;
            int nx = cx;
            int nz = cz;
            boolean stepX = e2 > -dz;
            boolean stepZ = e2 < dx;
            if (stepX) {
                err -= dz;
                nx += sx;
            }
            if (stepZ) {
                err += dx;
                nz += sz;
            }
            if (stepX && stepZ) {
                // Supercover guard: never clip corners across craters or walls
                if (!isInflatedPassable(nx, cz) || !isInflatedPassable(cx, nz)) {
                    return false;
                }
                if (Math.abs(this.surfaceY(nx, cz) - prevY) > 1 || Math.abs(this.surfaceY(cx, nz) - prevY) > 1) {
                    return false;
                }
            }
            if (!isInflatedPassable(nx, nz)) {
                return false;
            }
            int nextY = this.surfaceY(nx, nz);
            if (Math.abs(nextY - prevY) > 1) {
                return false;
            }
            cx = nx;
            cz = nz;
            prevY = nextY;
        }
        return true;
    }
}
