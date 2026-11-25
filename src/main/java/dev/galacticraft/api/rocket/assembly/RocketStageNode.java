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

package dev.galacticraft.api.rocket.assembly;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable coaxial stage inside a {@link RocketAssemblyGraph}, owning its own propellant chemistry,
 * fuel storage, core parts, and any attached {@link RadialBoosterGroup} children.
 */
public record RocketStageNode(
        int stageIndex,
        @NotNull Optional<BlockPos> decouplerRelativePos,
        boolean hotStaging,
        @NotNull String propellantId,
        long fuelCapacityDroplets,
        long currentFuelDroplets,
        @NotNull List<MountedPartNode> coreParts,
        @NotNull List<RadialBoosterGroup> radialGroups,
        boolean decoupled
) {
    public RocketStageNode {
        if (stageIndex < 0) {
            throw new IllegalArgumentException("stageIndex must be >= 0, got " + stageIndex);
        }
        decouplerRelativePos = Objects.requireNonNull(decouplerRelativePos, "decouplerRelativePos").map(BlockPos::immutable);
        propellantId = (propellantId == null || propellantId.isBlank()) ? "rp1" : propellantId;
        fuelCapacityDroplets = Math.max(0L, fuelCapacityDroplets);
        currentFuelDroplets = Math.clamp(currentFuelDroplets, 0L, fuelCapacityDroplets);
        coreParts = List.copyOf(Objects.requireNonNull(coreParts, "coreParts"));
        radialGroups = List.copyOf(Objects.requireNonNull(radialGroups, "radialGroups"));
    }

    public double coreDryMassKg() {
        double sum = 0.0;
        for (MountedPartNode part : this.coreParts) {
            sum += part.stats().dryWeightKg();
        }
        return sum;
    }

    public double totalDryMassKg() {
        double sum = this.coreDryMassKg();
        for (RadialBoosterGroup group : this.radialGroups) {
            if (!group.decoupled()) {
                sum += group.dryMassKg();
            }
        }
        return sum;
    }

    public double coreThrustKn() {
        double sum = 0.0;
        for (MountedPartNode part : this.coreParts) {
            sum += part.stats().thrustKn();
        }
        return sum;
    }

    public double activeThrustKn() {
        if (this.decoupled) {
            return 0.0;
        }
        double sum = this.coreThrustKn();
        for (RadialBoosterGroup group : this.radialGroups) {
            if (!group.decoupled()) {
                sum += group.totalThrustKn();
            }
        }
        return sum;
    }

    public long totalFuelCapacityDroplets() {
        long sum = this.fuelCapacityDroplets;
        for (RadialBoosterGroup group : this.radialGroups) {
            if (!group.decoupled()) {
                sum += group.fuelCapacityDroplets();
            }
        }
        return sum;
    }

    public long totalCurrentFuelDroplets() {
        long sum = this.currentFuelDroplets;
        for (RadialBoosterGroup group : this.radialGroups) {
            if (!group.decoupled()) {
                sum += group.currentFuelDroplets();
            }
        }
        return sum;
    }

    public @NotNull List<MountedPartNode> allActiveParts() {
        if (this.decoupled) {
            return List.of();
        }
        List<MountedPartNode> all = new ArrayList<>(this.coreParts);
        for (RadialBoosterGroup group : this.radialGroups) {
            if (!group.decoupled()) {
                all.addAll(group.parts());
            }
        }
        return List.copyOf(all);
    }

    public @NotNull RocketStageNode withCoreFuel(long nextFuel) {
        return new RocketStageNode(
                this.stageIndex,
                this.decouplerRelativePos,
                this.hotStaging,
                this.propellantId,
                this.fuelCapacityDroplets,
                Math.clamp(nextFuel, 0L, this.fuelCapacityDroplets),
                this.coreParts,
                this.radialGroups,
                this.decoupled
        );
    }

    public @NotNull RocketStageNode withUpdatedFuelAndBoosters(
            long nextCoreFuel,
            @NotNull List<RadialBoosterGroup> updatedRadialGroups
    ) {
        return new RocketStageNode(
                this.stageIndex,
                this.decouplerRelativePos,
                this.hotStaging,
                this.propellantId,
                this.fuelCapacityDroplets,
                Math.clamp(nextCoreFuel, 0L, this.fuelCapacityDroplets),
                this.coreParts,
                Objects.requireNonNull(updatedRadialGroups, "updatedRadialGroups"),
                this.decoupled
        );
    }

    public @NotNull RocketStageNode withCoreParts(@NotNull List<MountedPartNode> updatedCoreParts) {
        return new RocketStageNode(
                this.stageIndex,
                this.decouplerRelativePos,
                this.hotStaging,
                this.propellantId,
                this.fuelCapacityDroplets,
                this.currentFuelDroplets,
                Objects.requireNonNull(updatedCoreParts, "updatedCoreParts"),
                this.radialGroups,
                this.decoupled
        );
    }

    public @NotNull RocketStageNode withoutRadialGroup(@NotNull String groupId) {
        Objects.requireNonNull(groupId, "groupId");
        List<RadialBoosterGroup> updated = new ArrayList<>(this.radialGroups.size());
        for (RadialBoosterGroup group : this.radialGroups) {
            updated.add(group.groupId().equals(groupId) ? group.asDecoupled() : group);
        }
        return new RocketStageNode(
                this.stageIndex,
                this.decouplerRelativePos,
                this.hotStaging,
                this.propellantId,
                this.fuelCapacityDroplets,
                this.currentFuelDroplets,
                this.coreParts,
                updated,
                this.decoupled
        );
    }

    public @NotNull RocketStageNode asDecoupled() {
        if (this.decoupled) {
            return this;
        }
        List<RadialBoosterGroup> detachedBoosters = new ArrayList<>(this.radialGroups.size());
        for (RadialBoosterGroup group : this.radialGroups) {
            detachedBoosters.add(group.asDecoupled());
        }
        return new RocketStageNode(
                this.stageIndex,
                this.decouplerRelativePos,
                this.hotStaging,
                this.propellantId,
                this.fuelCapacityDroplets,
                this.currentFuelDroplets,
                this.coreParts,
                detachedBoosters,
                true
        );
    }

    public static final Codec<RocketStageNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("stage_index").forGetter(RocketStageNode::stageIndex),
            BlockPos.CODEC.optionalFieldOf("decoupler_relative_pos").forGetter(RocketStageNode::decouplerRelativePos),
            Codec.BOOL.optionalFieldOf("hot_staging", false).forGetter(RocketStageNode::hotStaging),
            Codec.STRING.optionalFieldOf("propellant_id", "rp1").forGetter(RocketStageNode::propellantId),
            Codec.LONG.optionalFieldOf("fuel_capacity_droplets", 0L).forGetter(RocketStageNode::fuelCapacityDroplets),
            Codec.LONG.optionalFieldOf("current_fuel_droplets", 0L).forGetter(RocketStageNode::currentFuelDroplets),
            MountedPartNode.CODEC.listOf().fieldOf("core_parts").forGetter(RocketStageNode::coreParts),
            RadialBoosterGroup.CODEC.listOf().optionalFieldOf("radial_groups", List.of()).forGetter(RocketStageNode::radialGroups),
            Codec.BOOL.optionalFieldOf("decoupled", false).forGetter(RocketStageNode::decoupled)
    ).apply(instance, RocketStageNode::new));

    public static final StreamCodec<ByteBuf, RocketStageNode> STREAM_CODEC = StreamCodec.of(
            (buf, stage) -> {
                ByteBufCodecs.VAR_INT.encode(buf, stage.stageIndex);
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buf, stage.decouplerRelativePos);
                ByteBufCodecs.BOOL.encode(buf, stage.hotStaging);
                ByteBufCodecs.STRING_UTF8.encode(buf, stage.propellantId);
                ByteBufCodecs.VAR_LONG.encode(buf, stage.fuelCapacityDroplets);
                ByteBufCodecs.VAR_LONG.encode(buf, stage.currentFuelDroplets);
                MountedPartNode.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, stage.coreParts);
                RadialBoosterGroup.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, stage.radialGroups);
                ByteBufCodecs.BOOL.encode(buf, stage.decoupled);
            },
            buf -> new RocketStageNode(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    MountedPartNode.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    RadialBoosterGroup.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );
}
