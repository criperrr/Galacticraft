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

package dev.galacticraft.api.vehicle.ai;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent, network-synchronized configuration for an autonomous modular vehicle's
 * {@code AI_CORE} mission controller.
 */
public record VehicleMissionConfig(
        boolean enabled,
        @NotNull VehicleMissionRole role,
        @NotNull List<BlockPos> waypoints,
        @NotNull Optional<BlockPos> homeChargerPos,
        @NotNull CargoLoadCondition cargoLoadCondition,
        int patrolRadius,
        boolean chunkLoadingEnabled,
        boolean autoBoardNpcs,
        double escortOffsetBlocks,
        @NotNull String ownerUuid,
        int runwayHeadingDeg
) {
    public static final int MAX_WAYPOINTS = 16;

    public static final VehicleMissionConfig DEFAULT = new VehicleMissionConfig(
            false,
            VehicleMissionRole.SCIENCE_EXPLORER,
            List.of(),
            Optional.empty(),
            CargoLoadCondition.INACTIVITY_60_TICKS,
            48,
            false,
            true,
            6.0,
            "",
            0
    );

    public VehicleMissionConfig {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(waypoints, "waypoints");
        Objects.requireNonNull(homeChargerPos, "homeChargerPos");
        Objects.requireNonNull(cargoLoadCondition, "cargoLoadCondition");
        Objects.requireNonNull(ownerUuid, "ownerUuid");
        List<BlockPos> trimmed = waypoints.size() > MAX_WAYPOINTS
                ? waypoints.subList(0, MAX_WAYPOINTS)
                : waypoints;
        waypoints = List.copyOf(trimmed);
        patrolRadius = Math.clamp(patrolRadius, 16, 160);
        escortOffsetBlocks = Double.isFinite(escortOffsetBlocks) ? Math.clamp(escortOffsetBlocks, 3.0, 24.0) : 6.0;
        runwayHeadingDeg = Math.floorMod(runwayHeadingDeg, 360);
    }

    public @Nullable UUID parsedOwnerUuid() {
        if (this.ownerUuid.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(this.ownerUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public @NotNull VehicleMissionConfig withEnabled(boolean nextEnabled) {
        return new VehicleMissionConfig(nextEnabled, this.role, this.waypoints, this.homeChargerPos,
                this.cargoLoadCondition, this.patrolRadius, this.chunkLoadingEnabled, this.autoBoardNpcs,
                this.escortOffsetBlocks, this.ownerUuid, this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig withRole(@NotNull VehicleMissionRole nextRole) {
        return new VehicleMissionConfig(this.enabled, nextRole, this.waypoints, this.homeChargerPos,
                this.cargoLoadCondition, this.patrolRadius, this.chunkLoadingEnabled, this.autoBoardNpcs,
                this.escortOffsetBlocks, this.ownerUuid, this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig withWaypoints(@NotNull List<BlockPos> nextWaypoints) {
        return new VehicleMissionConfig(this.enabled, this.role, nextWaypoints, this.homeChargerPos,
                this.cargoLoadCondition, this.patrolRadius, this.chunkLoadingEnabled, this.autoBoardNpcs,
                this.escortOffsetBlocks, this.ownerUuid, this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig addWaypoint(@NotNull BlockPos pos) {
        List<BlockPos> next = new ArrayList<>(this.waypoints);
        if (next.size() < MAX_WAYPOINTS) {
            next.add(pos.immutable());
        }
        return this.withWaypoints(next);
    }

    public @NotNull VehicleMissionConfig removeWaypoint(int index) {
        if (index < 0 || index >= this.waypoints.size()) {
            return this;
        }
        List<BlockPos> next = new ArrayList<>(this.waypoints);
        next.remove(index);
        return this.withWaypoints(next);
    }

    public @NotNull VehicleMissionConfig withHomeChargerPos(@Nullable BlockPos pos) {
        return new VehicleMissionConfig(this.enabled, this.role, this.waypoints,
                Optional.ofNullable(pos == null ? null : pos.immutable()),
                this.cargoLoadCondition, this.patrolRadius, this.chunkLoadingEnabled, this.autoBoardNpcs,
                this.escortOffsetBlocks, this.ownerUuid, this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig withCargoLoadCondition(@NotNull CargoLoadCondition nextCondition) {
        return new VehicleMissionConfig(this.enabled, this.role, this.waypoints, this.homeChargerPos,
                nextCondition, this.patrolRadius, this.chunkLoadingEnabled, this.autoBoardNpcs,
                this.escortOffsetBlocks, this.ownerUuid, this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig withPatrolRadius(int nextRadius) {
        return new VehicleMissionConfig(this.enabled, this.role, this.waypoints, this.homeChargerPos,
                this.cargoLoadCondition, nextRadius, this.chunkLoadingEnabled, this.autoBoardNpcs,
                this.escortOffsetBlocks, this.ownerUuid, this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig withChunkLoadingEnabled(boolean nextChunkLoading) {
        return new VehicleMissionConfig(this.enabled, this.role, this.waypoints, this.homeChargerPos,
                this.cargoLoadCondition, this.patrolRadius, nextChunkLoading, this.autoBoardNpcs,
                this.escortOffsetBlocks, this.ownerUuid, this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig withAutoBoardNpcs(boolean nextAutoBoard) {
        return new VehicleMissionConfig(this.enabled, this.role, this.waypoints, this.homeChargerPos,
                this.cargoLoadCondition, this.patrolRadius, this.chunkLoadingEnabled, nextAutoBoard,
                this.escortOffsetBlocks, this.ownerUuid, this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig withEscortOffsetBlocks(double nextOffset) {
        return new VehicleMissionConfig(this.enabled, this.role, this.waypoints, this.homeChargerPos,
                this.cargoLoadCondition, this.patrolRadius, this.chunkLoadingEnabled, this.autoBoardNpcs,
                nextOffset, this.ownerUuid, this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig withOwnerUuid(@Nullable UUID uuid) {
        return new VehicleMissionConfig(this.enabled, this.role, this.waypoints, this.homeChargerPos,
                this.cargoLoadCondition, this.patrolRadius, this.chunkLoadingEnabled, this.autoBoardNpcs,
                this.escortOffsetBlocks, uuid == null ? "" : uuid.toString(), this.runwayHeadingDeg);
    }

    public @NotNull VehicleMissionConfig withRunwayHeadingDeg(int nextHeading) {
        return new VehicleMissionConfig(this.enabled, this.role, this.waypoints, this.homeChargerPos,
                this.cargoLoadCondition, this.patrolRadius, this.chunkLoadingEnabled, this.autoBoardNpcs,
                this.escortOffsetBlocks, this.ownerUuid, nextHeading);
    }

    public static final Codec<VehicleMissionConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(VehicleMissionConfig::enabled),
            VehicleMissionRole.CODEC.optionalFieldOf("role", VehicleMissionRole.SCIENCE_EXPLORER).forGetter(VehicleMissionConfig::role),
            BlockPos.CODEC.listOf().optionalFieldOf("waypoints", List.of()).forGetter(VehicleMissionConfig::waypoints),
            BlockPos.CODEC.optionalFieldOf("home_charger_pos").forGetter(VehicleMissionConfig::homeChargerPos),
            CargoLoadCondition.CODEC.optionalFieldOf("cargo_load_condition", CargoLoadCondition.INACTIVITY_60_TICKS).forGetter(VehicleMissionConfig::cargoLoadCondition),
            Codec.INT.optionalFieldOf("patrol_radius", 48).forGetter(VehicleMissionConfig::patrolRadius),
            Codec.BOOL.optionalFieldOf("chunk_loading_enabled", false).forGetter(VehicleMissionConfig::chunkLoadingEnabled),
            Codec.BOOL.optionalFieldOf("auto_board_npcs", true).forGetter(VehicleMissionConfig::autoBoardNpcs),
            Codec.DOUBLE.optionalFieldOf("escort_offset_blocks", 6.0).forGetter(VehicleMissionConfig::escortOffsetBlocks),
            Codec.STRING.optionalFieldOf("owner_uuid", "").forGetter(VehicleMissionConfig::ownerUuid),
            Codec.INT.optionalFieldOf("runway_heading_deg", 0).forGetter(VehicleMissionConfig::runwayHeadingDeg)
    ).apply(instance, VehicleMissionConfig::new));

    public static final StreamCodec<ByteBuf, VehicleMissionConfig> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull VehicleMissionConfig decode(@NotNull ByteBuf buf) {
            boolean enabled = ByteBufCodecs.BOOL.decode(buf);
            VehicleMissionRole role = VehicleMissionRole.STREAM_CODEC.decode(buf);
            int wpCount = Math.clamp(ByteBufCodecs.VAR_INT.decode(buf), 0, MAX_WAYPOINTS);
            List<BlockPos> waypoints = new ArrayList<>(wpCount);
            for (int i = 0; i < wpCount; i++) {
                waypoints.add(BlockPos.STREAM_CODEC.decode(buf));
            }
            boolean hasHome = ByteBufCodecs.BOOL.decode(buf);
            Optional<BlockPos> home = hasHome ? Optional.of(BlockPos.STREAM_CODEC.decode(buf)) : Optional.empty();
            CargoLoadCondition condition = CargoLoadCondition.STREAM_CODEC.decode(buf);
            int patrolRadius = ByteBufCodecs.VAR_INT.decode(buf);
            boolean chunkLoading = ByteBufCodecs.BOOL.decode(buf);
            boolean autoBoard = ByteBufCodecs.BOOL.decode(buf);
            double escortOffset = ByteBufCodecs.DOUBLE.decode(buf);
            String owner = ByteBufCodecs.STRING_UTF8.decode(buf);
            int runwayDeg = ByteBufCodecs.VAR_INT.decode(buf);
            return new VehicleMissionConfig(enabled, role, waypoints, home, condition, patrolRadius,
                    chunkLoading, autoBoard, escortOffset, owner, runwayDeg);
        }

        @Override
        public void encode(@NotNull ByteBuf buf, @NotNull VehicleMissionConfig value) {
            ByteBufCodecs.BOOL.encode(buf, value.enabled());
            VehicleMissionRole.STREAM_CODEC.encode(buf, value.role());
            ByteBufCodecs.VAR_INT.encode(buf, value.waypoints().size());
            for (BlockPos wp : value.waypoints()) {
                BlockPos.STREAM_CODEC.encode(buf, wp);
            }
            ByteBufCodecs.BOOL.encode(buf, value.homeChargerPos().isPresent());
            value.homeChargerPos().ifPresent(pos -> BlockPos.STREAM_CODEC.encode(buf, pos));
            CargoLoadCondition.STREAM_CODEC.encode(buf, value.cargoLoadCondition());
            ByteBufCodecs.VAR_INT.encode(buf, value.patrolRadius());
            ByteBufCodecs.BOOL.encode(buf, value.chunkLoadingEnabled());
            ByteBufCodecs.BOOL.encode(buf, value.autoBoardNpcs());
            ByteBufCodecs.DOUBLE.encode(buf, value.escortOffsetBlocks());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.ownerUuid());
            ByteBufCodecs.VAR_INT.encode(buf, value.runwayHeadingDeg());
        }
    };
}
