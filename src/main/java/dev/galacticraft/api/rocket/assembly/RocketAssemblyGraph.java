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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable Directed Acyclic Graph (DAG) representing a unified modular spacecraft across both
 * static launchpad assembly ({@code T < 0}) and powered multi-stage flight ({@code T >= 0}).
 */
public record RocketAssemblyGraph(
        @NotNull List<RocketStageNode> stages,
        @NotNull List<LocalSeatOffset> crewSeats,
        int totalHeightBlocks,
        double maxRadiusBlocks,
        boolean hasLandingGear,
        boolean hasLesAbort,
        boolean hasFairing
) {
    public static final RocketAssemblyGraph EMPTY = new RocketAssemblyGraph(
            List.of(), List.of(), 0, 0.5, false, false, false
    );

    public RocketAssemblyGraph {
        stages = List.copyOf(Objects.requireNonNull(stages, "stages"));
        crewSeats = List.copyOf(Objects.requireNonNull(crewSeats, "crewSeats"));
        totalHeightBlocks = Math.max(0, totalHeightBlocks);
        maxRadiusBlocks = (Double.isFinite(maxRadiusBlocks) && maxRadiusBlocks > 0.0) ? maxRadiusBlocks : 0.5;
    }

    public boolean isEmpty() {
        return this.stages.isEmpty();
    }

    /**
     * Returns the lowest non-decoupled stage (the currently active propulsion stage), or {@code null}
     * if no active stages remain.
     */
    public @Nullable RocketStageNode activeStage() {
        for (RocketStageNode stage : this.stages) {
            if (!stage.decoupled()) {
                return stage;
            }
        }
        return null;
    }

    public @NotNull List<RocketStageNode> activeStages() {
        List<RocketStageNode> active = new ArrayList<>(this.stages.size());
        for (RocketStageNode stage : this.stages) {
            if (!stage.decoupled()) {
                active.add(stage);
            }
        }
        return List.copyOf(active);
    }

    public @NotNull List<MountedPartNode> allActiveParts() {
        List<MountedPartNode> parts = new ArrayList<>();
        for (RocketStageNode stage : this.stages) {
            if (!stage.decoupled()) {
                parts.addAll(stage.allActiveParts());
            }
        }
        return List.copyOf(parts);
    }

    public double totalDryMassKg() {
        double sum = 0.0;
        for (RocketStageNode stage : this.stages) {
            if (!stage.decoupled()) {
                sum += stage.totalDryMassKg();
            }
        }
        return sum;
    }

    public long totalFuelCapacityDroplets() {
        long sum = 0L;
        for (RocketStageNode stage : this.stages) {
            if (!stage.decoupled()) {
                sum += stage.totalFuelCapacityDroplets();
            }
        }
        return sum;
    }

    public long totalCurrentFuelDroplets() {
        long sum = 0L;
        for (RocketStageNode stage : this.stages) {
            if (!stage.decoupled()) {
                sum += stage.totalCurrentFuelDroplets();
            }
        }
        return sum;
    }

    public @NotNull RocketAssemblyGraph withoutStage(int stageIndex) {
        List<RocketStageNode> updated = new ArrayList<>(this.stages.size());
        for (RocketStageNode stage : this.stages) {
            updated.add(stage.stageIndex() == stageIndex ? stage.asDecoupled() : stage);
        }
        return recomputeEnvelope(updated, this.crewSeats, this.hasLandingGear, this.hasLesAbort, this.hasFairing);
    }

    public @NotNull RocketAssemblyGraph withoutRadialGroup(int stageIndex, @NotNull String groupId) {
        Objects.requireNonNull(groupId, "groupId");
        List<RocketStageNode> updated = new ArrayList<>(this.stages.size());
        for (RocketStageNode stage : this.stages) {
            updated.add(stage.stageIndex() == stageIndex ? stage.withoutRadialGroup(groupId) : stage);
        }
        return recomputeEnvelope(updated, this.crewSeats, this.hasLandingGear, this.hasLesAbort, this.hasFairing);
    }

    public @NotNull RocketAssemblyGraph withUpdatedStage(@NotNull RocketStageNode updatedStage) {
        Objects.requireNonNull(updatedStage, "updatedStage");
        List<RocketStageNode> updated = new ArrayList<>(this.stages.size());
        for (RocketStageNode stage : this.stages) {
            updated.add(stage.stageIndex() == updatedStage.stageIndex() ? updatedStage : stage);
        }
        return new RocketAssemblyGraph(
                updated,
                this.crewSeats,
                this.totalHeightBlocks,
                this.maxRadiusBlocks,
                this.hasLandingGear,
                this.hasLesAbort,
                this.hasFairing
        );
    }

    /**
     * Returns a copy of this graph with all aerodynamic fairing parts stripped from active stages
     * and {@link #hasFairing()} set to {@code false} (used during automatic fairing jettison).
     */
    public @NotNull RocketAssemblyGraph withoutFairings() {
        if (!this.hasFairing) {
            return this;
        }
        List<RocketStageNode> updated = new ArrayList<>(this.stages.size());
        for (RocketStageNode stage : this.stages) {
            if (stage.decoupled()) {
                updated.add(stage);
                continue;
            }
            List<MountedPartNode> keptCore = new ArrayList<>(stage.coreParts().size());
            for (MountedPartNode part : stage.coreParts()) {
                if (!isFairingPart(part)) {
                    keptCore.add(part);
                }
            }
            updated.add(stage.withCoreParts(keptCore));
        }
        return recomputeEnvelope(updated, this.crewSeats, this.hasLandingGear, this.hasLesAbort, false);
    }

    /**
     * Returns a copy of this graph retaining only the top stage containing command/capsule parts
     * for emergency Launch Escape System (LES) abort separation.
     */
    public @NotNull RocketAssemblyGraph forLesAbortCapsule() {
        RocketStageNode commandStage = null;
        for (int i = this.stages.size() - 1; i >= 0; i--) {
            RocketStageNode stage = this.stages.get(i);
            if (!stage.decoupled()) {
                for (MountedPartNode part : stage.coreParts()) {
                    if (part.config().category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.COMMAND) {
                        commandStage = stage;
                        break;
                    }
                }
            }
            if (commandStage != null) {
                break;
            }
        }
        if (commandStage == null) {
            return this;
        }
        List<MountedPartNode> capsuleParts = new ArrayList<>();
        for (MountedPartNode part : commandStage.coreParts()) {
            String family = part.config().familyId().toLowerCase(java.util.Locale.ROOT);
            if (part.config().category() == dev.galacticraft.api.rocket.part.modular.RocketPartCategory.COMMAND
                    || family.contains("parachute")
                    || family.contains("les")
                    || family.contains("abort")
                    || family.contains("docking")
                    || family.contains("heat_shield")
                    || family.contains("heatshield")) {
                capsuleParts.add(part);
            }
        }
        if (capsuleParts.isEmpty()) {
            capsuleParts.addAll(commandStage.coreParts());
        }
        RocketStageNode isolatedCapsule = new RocketStageNode(
                0,
                java.util.Optional.empty(),
                false,
                "solid",
                0L,
                0L,
                capsuleParts,
                List.of(),
                false
        );
        return recomputeEnvelope(List.of(isolatedCapsule), this.crewSeats, false, false, false);
    }

    public static boolean isFairingPart(@NotNull MountedPartNode part) {
        String id = (part.config().familyId() + "_" + part.config().modelId()).toLowerCase(java.util.Locale.ROOT);
        return id.contains("fairing_nose_cone") || id.contains("fairing_barrel") || id.contains("fairing_");
    }

    private static @NotNull RocketAssemblyGraph recomputeEnvelope(
            List<RocketStageNode> stages,
            List<LocalSeatOffset> crewSeats,
            boolean hasLandingGear,
            boolean hasLesAbort,
            boolean hasFairing
    ) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        double maxRadius = 0.5;
        for (RocketStageNode stage : stages) {
            if (stage.decoupled()) {
                continue;
            }
            for (MountedPartNode part : stage.allActiveParts()) {
                int y0 = part.relativePos().getY();
                int y1 = y0 + Math.max(1, part.config().heightBlocks());
                minY = Math.min(minY, y0);
                maxY = Math.max(maxY, y1);
                double radialDist = Math.hypot(part.relativePos().getX(), part.relativePos().getZ())
                        + Math.max(part.config().diameter(), part.config().topDiameter()) * 0.5;
                maxRadius = Math.max(maxRadius, radialDist);
            }
        }
        int height = minY <= maxY ? (maxY - minY) : 0;
        return new RocketAssemblyGraph(stages, crewSeats, height, maxRadius, hasLandingGear, hasLesAbort, hasFairing);
    }

    public record LocalSeatOffset(
            double x,
            double y,
            double z,
            @NotNull String moduleModelId
    ) {
        public LocalSeatOffset {
            x = Double.isFinite(x) ? x : 0.0;
            y = Double.isFinite(y) ? y : 1.0;
            z = Double.isFinite(z) ? z : 0.0;
            moduleModelId = Objects.requireNonNull(moduleModelId, "moduleModelId");
        }

        public static final Codec<LocalSeatOffset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter(LocalSeatOffset::x),
                Codec.DOUBLE.fieldOf("y").forGetter(LocalSeatOffset::y),
                Codec.DOUBLE.fieldOf("z").forGetter(LocalSeatOffset::z),
                Codec.STRING.fieldOf("module_model_id").forGetter(LocalSeatOffset::moduleModelId)
        ).apply(instance, LocalSeatOffset::new));

        public static final StreamCodec<ByteBuf, LocalSeatOffset> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, LocalSeatOffset::x,
                ByteBufCodecs.DOUBLE, LocalSeatOffset::y,
                ByteBufCodecs.DOUBLE, LocalSeatOffset::z,
                ByteBufCodecs.STRING_UTF8, LocalSeatOffset::moduleModelId,
                LocalSeatOffset::new
        );
    }

    public static final Codec<RocketAssemblyGraph> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RocketStageNode.CODEC.listOf().fieldOf("stages").forGetter(RocketAssemblyGraph::stages),
            LocalSeatOffset.CODEC.listOf().optionalFieldOf("crew_seats", List.of()).forGetter(RocketAssemblyGraph::crewSeats),
            Codec.INT.optionalFieldOf("total_height_blocks", 0).forGetter(RocketAssemblyGraph::totalHeightBlocks),
            Codec.DOUBLE.optionalFieldOf("max_radius_blocks", 0.5).forGetter(RocketAssemblyGraph::maxRadiusBlocks),
            Codec.BOOL.optionalFieldOf("has_landing_gear", false).forGetter(RocketAssemblyGraph::hasLandingGear),
            Codec.BOOL.optionalFieldOf("has_les_abort", false).forGetter(RocketAssemblyGraph::hasLesAbort),
            Codec.BOOL.optionalFieldOf("has_fairing", false).forGetter(RocketAssemblyGraph::hasFairing)
    ).apply(instance, RocketAssemblyGraph::new));

    public static final StreamCodec<ByteBuf, RocketAssemblyGraph> STREAM_CODEC = StreamCodec.of(
            (buf, graph) -> {
                RocketStageNode.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, graph.stages);
                LocalSeatOffset.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, graph.crewSeats);
                ByteBufCodecs.VAR_INT.encode(buf, graph.totalHeightBlocks);
                ByteBufCodecs.DOUBLE.encode(buf, graph.maxRadiusBlocks);
                ByteBufCodecs.BOOL.encode(buf, graph.hasLandingGear);
                ByteBufCodecs.BOOL.encode(buf, graph.hasLesAbort);
                ByteBufCodecs.BOOL.encode(buf, graph.hasFairing);
            },
            buf -> new RocketAssemblyGraph(
                    RocketStageNode.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    LocalSeatOffset.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );
}
