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

package dev.galacticraft.mod.content.block.platform;

import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable structural blueprint describing the local-space skeleton and clearance volume
 * of one of the 8 modular rocket construction and launch platforms.
 *
 * <p>Local coordinates are centered at {@code padCenter (0, 0, 0)} with {@code +Z} as
 * terminal/rocket front, {@code -Z} as rear, {@code +X} as east, {@code -X} as west,
 * and {@code Y = 0} at the launch pad deck level.
 */
public final class PlatformBlueprint {
    private final PlatformType type;
    private final PlatformSize size;
    private final List<Entry> entries;
    private final BlockPos clearanceMin;
    private final BlockPos clearanceMax;
    private final int maxY;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final Map<PlatformBlockRole, Integer> requiredCounts;
    private final LongSet entryLocalPosSet;

    PlatformBlueprint(
            PlatformType type,
            PlatformSize size,
            List<Entry> entries,
            BlockPos clearanceMin,
            BlockPos clearanceMax
    ) {
        this.type = type;
        this.size = size;
        this.entries = List.copyOf(entries);
        this.clearanceMin = clearanceMin.immutable();
        this.clearanceMax = clearanceMax.immutable();

        EnumMap<PlatformBlockRole, Integer> counts = new EnumMap<>(PlatformBlockRole.class);
        for (PlatformBlockRole role : PlatformBlockRole.values()) {
            counts.put(role, 0);
        }

        int computedMaxY = 0;
        int computedMinX = 0;
        int computedMaxX = 0;
        int computedMinZ = 0;
        int computedMaxZ = 0;
        LongSet posSet = new LongOpenHashSet(this.entries.size());
        for (Entry entry : this.entries) {
            counts.merge(entry.role(), 1, Integer::sum);
            BlockPos p = entry.localPos();
            if (p.getY() > computedMaxY) {
                computedMaxY = p.getY();
            }
            if (p.getX() < computedMinX) {
                computedMinX = p.getX();
            }
            if (p.getX() > computedMaxX) {
                computedMaxX = p.getX();
            }
            if (p.getZ() < computedMinZ) {
                computedMinZ = p.getZ();
            }
            if (p.getZ() > computedMaxZ) {
                computedMaxZ = p.getZ();
            }
            posSet.add(p.asLong());
        }

        this.maxY = computedMaxY;
        this.minX = computedMinX;
        this.maxX = computedMaxX;
        this.minZ = computedMinZ;
        this.maxZ = computedMaxZ;
        this.requiredCounts = Collections.unmodifiableMap(counts);
        this.entryLocalPosSet = posSet;
    }

    public PlatformType type() {
        return this.type;
    }

    public PlatformSize size() {
        return this.size;
    }

    public List<Entry> entries() {
        return this.entries;
    }

    public BlockPos clearanceMin() {
        return this.clearanceMin;
    }

    public BlockPos clearanceMax() {
        return this.clearanceMax;
    }

    public int maxY() {
        return this.maxY;
    }

    public int minX() {
        return this.minX;
    }

    public int maxX() {
        return this.maxX;
    }

    public int minZ() {
        return this.minZ;
    }

    public int maxZ() {
        return this.maxZ;
    }

    public Map<PlatformBlockRole, Integer> requiredCounts() {
        return this.requiredCounts;
    }

    /**
     * Rotates a blueprint-local offset around the Y axis according to {@code facing}
     * ({@code NORTH = 0 deg}, {@code EAST = 90 deg}, {@code SOUTH = 180 deg}, {@code WEST = 270 deg})
     * and translates it to world coordinates relative to {@code padCenter}.
     */
    public static BlockPos toWorldPos(BlockPos padCenter, Direction facing, BlockPos localPos) {
        int lx = localPos.getX();
        int ly = localPos.getY();
        int lz = localPos.getZ();
        return switch (facing) {
            case EAST -> padCenter.offset(-lz, ly, lx);
            case SOUTH -> padCenter.offset(-lx, ly, -lz);
            case WEST -> padCenter.offset(lz, ly, -lx);
            default -> padCenter.offset(lx, ly, lz);
        };
    }

    /**
     * Inverse of {@link #toWorldPos(BlockPos, Direction, BlockPos)}.
     */
    public static BlockPos toLocalPos(BlockPos padCenter, Direction facing, BlockPos worldPos) {
        int dx = worldPos.getX() - padCenter.getX();
        int dy = worldPos.getY() - padCenter.getY();
        int dz = worldPos.getZ() - padCenter.getZ();
        return switch (facing) {
            case EAST -> new BlockPos(dz, dy, -dx);
            case SOUTH -> new BlockPos(-dx, dy, -dz);
            case WEST -> new BlockPos(-dz, dy, dx);
            default -> new BlockPos(dx, dy, dz);
        };
    }

    /**
     * Validates all structural skeleton blocks and the rocket bay clearance volume in the world.
     * Any entry coinciding with {@code terminalPos} is skipped so the terminal itself is never flagged as wrong.
     */
    public ValidationSummary validate(LevelReader level, BlockPos padCenter, Direction facing, @Nullable BlockPos terminalPos) {
        EnumMap<PlatformBlockRole, Integer> placedCounts = new EnumMap<>(PlatformBlockRole.class);
        EnumMap<PlatformBlockRole, Integer> effectiveRequired = new EnumMap<>(PlatformBlockRole.class);
        for (PlatformBlockRole role : PlatformBlockRole.values()) {
            placedCounts.put(role, 0);
            effectiveRequired.put(role, this.requiredCounts.getOrDefault(role, 0));
        }

        int missingCount = 0;
        int wrongCount = 0;

        for (Entry entry : this.entries) {
            BlockPos worldPos = toWorldPos(padCenter, facing, entry.localPos());
            if (worldPos.equals(terminalPos)) {
                effectiveRequired.computeIfPresent(entry.role(), (r, c) -> Math.max(0, c - 1));
                continue;
            }
            BlockState state = level.getBlockState(worldPos);
            if (entry.role().matches(state)) {
                placedCounts.merge(entry.role(), 1, Integer::sum);
            } else if (state.isAir() || state.canBeReplaced()) {
                missingCount++;
            } else {
                wrongCount++;
            }
        }

        int clearanceBlockedCount = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int cx = padCenter.getX();
        int cy = padCenter.getY();
        int cz = padCenter.getZ();

        for (int ly = this.clearanceMin.getY(); ly <= this.clearanceMax.getY(); ly++) {
            for (int lz = this.clearanceMin.getZ(); lz <= this.clearanceMax.getZ(); lz++) {
                for (int lx = this.clearanceMin.getX(); lx <= this.clearanceMax.getX(); lx++) {
                    if (this.entryLocalPosSet.contains(BlockPos.asLong(lx, ly, lz))) {
                        continue;
                    }
                    setWorldMutable(cursor, cx, cy, cz, facing, lx, ly, lz);
                    if (terminalPos != null && cursor.equals(terminalPos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir() && !state.canBeReplaced()) {
                        clearanceBlockedCount++;
                    }
                }
            }
        }

        return new ValidationSummary(
                Collections.unmodifiableMap(placedCounts),
                Collections.unmodifiableMap(effectiveRequired),
                missingCount,
                wrongCount,
                clearanceBlockedCount
        );
    }

    /**
     * Returns an all-missing summary without inspecting any {@link LevelReader}, useful before a level is attached.
     */
    public ValidationSummary emptySummary() {
        EnumMap<PlatformBlockRole, Integer> placedCounts = new EnumMap<>(PlatformBlockRole.class);
        for (PlatformBlockRole role : PlatformBlockRole.values()) {
            placedCounts.put(role, 0);
        }
        return new ValidationSummary(
                Collections.unmodifiableMap(placedCounts),
                this.requiredCounts,
                this.entries.size(),
                0,
                0
        );
    }

    private static void setWorldMutable(
            BlockPos.MutableBlockPos target,
            int cx,
            int cy,
            int cz,
            Direction facing,
            int lx,
            int ly,
            int lz
    ) {
        switch (facing) {
            case EAST -> target.set(cx - lz, cy + ly, cz + lx);
            case SOUTH -> target.set(cx - lx, cy + ly, cz - lz);
            case WEST -> target.set(cx + lz, cy + ly, cz - lx);
            default -> target.set(cx + lx, cy + ly, cz + lz);
        }
    }

    public record Entry(BlockPos localPos, PlatformBlockRole role, BoundsShape formedShape) {
        public Entry(int x, int y, int z, PlatformBlockRole role) {
            this(new BlockPos(x, y, z), role, role.defaultFormedShape());
        }

        public Entry(int x, int y, int z, PlatformBlockRole role, BoundsShape formedShape) {
            this(new BlockPos(x, y, z), role, formedShape);
        }
    }

    public record ValidationSummary(
            Map<PlatformBlockRole, Integer> placedCounts,
            Map<PlatformBlockRole, Integer> requiredCounts,
            int missingCount,
            int wrongCount,
            int clearanceBlockedCount
    ) {
        public boolean readyToAssemble() {
            return this.missingCount == 0 && this.wrongCount == 0 && this.clearanceBlockedCount == 0;
        }

        public int totalRequired() {
            int sum = 0;
            for (int count : this.requiredCounts.values()) {
                sum += count;
            }
            return sum;
        }

        public int totalPlaced() {
            int sum = 0;
            for (int count : this.placedCounts.values()) {
                sum += count;
            }
            return sum;
        }
    }

    static Builder builder(PlatformType type, PlatformSize size) {
        return new Builder(type, size);
    }

    static final class Builder {
        private final PlatformType type;
        private final PlatformSize size;
        private final Map<BlockPos, Entry> entries = new LinkedHashMap<>();
        private BlockPos clearanceMin = BlockPos.ZERO;
        private BlockPos clearanceMax = BlockPos.ZERO;

        private Builder(PlatformType type, PlatformSize size) {
            this.type = type;
            this.size = size;
        }

        Builder add(int x, int y, int z, PlatformBlockRole role) {
            return this.add(x, y, z, role, role.defaultFormedShape());
        }

        Builder add(int x, int y, int z, PlatformBlockRole role, BoundsShape formedShape) {
            BlockPos pos = new BlockPos(x, y, z);
            this.entries.put(pos, new Entry(pos, role, formedShape));
            return this;
        }

        Builder fill(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, PlatformBlockRole role) {
            return this.fill(minX, maxX, minY, maxY, minZ, maxZ, role, role.defaultFormedShape());
        }

        Builder fill(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, PlatformBlockRole role, BoundsShape formedShape) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        this.add(x, y, z, role, formedShape);
                    }
                }
            }
            return this;
        }

        Builder clearance(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.clearanceMin = new BlockPos(minX, minY, minZ);
            this.clearanceMax = new BlockPos(maxX, maxY, maxZ);
            return this;
        }

        PlatformBlueprint build() {
            return new PlatformBlueprint(
                    this.type,
                    this.size,
                    new ArrayList<>(this.entries.values()),
                    this.clearanceMin,
                    this.clearanceMax
            );
        }
    }
}
