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

package dev.galacticraft.mod.content.rocket.assembly;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.rocket.assembly.AttachmentNode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.AttachmentNodeType;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import dev.galacticraft.api.rocket.assembly.MountedPartNode;
import dev.galacticraft.api.rocket.assembly.RadialBoosterGroup;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph.LocalSeatOffset;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.FootprintMode;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprint;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.physics.PropellantType;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Compiles world-placed modular rocket parts or staged {@link ItemStack} lists into an immutable
 * {@link RocketAssemblyGraph} DAG while validating diameter continuity, hangar clearance bounds,
 * pad mass limits ({@code M_max}), stage separation rings, and octagonal radial booster symmetry
 * ({@code 2x / 4x / 8x}) with Asparagus crossfeed drain priorities.
 */
public final class RocketAssemblyGraphBuilder {
    /**
     * Standard radial decoupler standoff distance (4 px = 0.25 blocks) matching GC5 octagonal geometry.
     */
    public static final double DECOUPLER_STANDOFF_BLOCKS = 0.25;

    /**
     * Maximum magnetic attachment node snap radius in blocks for in-world ghost previews.
     */
    public static final double MAGNETIC_SNAP_RADIUS_BLOCKS = 0.50;

    private RocketAssemblyGraphBuilder() {
    }

    public enum IssueCode {
        EMPTY_ASSEMBLY,
        DIAMETER_MISMATCH,
        CLEARANCE_OBSTRUCTED,
        PAD_OVERWEIGHT,
        UNSUPPORTED_NODE,
        NO_ENGINE,
        NO_COMMAND
    }

    public enum IssueSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public record ValidationIssue(
            @NotNull IssueCode code,
            @NotNull IssueSeverity severity,
            @NotNull String message,
            @Nullable BlockPos relativePos
    ) {
        public ValidationIssue {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(severity, "severity");
            Objects.requireNonNull(message, "message");
            relativePos = relativePos != null ? relativePos.immutable() : null;
        }
    }

    public record PartInput(
            @NotNull BlockPos relativePos,
            @NotNull RocketPartConfig config,
            @NotNull RocketPartStats stats,
            @NotNull PartIntegrity integrity,
            @Nullable RocketPartComposition composition
    ) {
        public PartInput {
            relativePos = Objects.requireNonNull(relativePos, "relativePos").immutable();
            Objects.requireNonNull(config, "config");
            Objects.requireNonNull(stats, "stats");
            Objects.requireNonNull(integrity, "integrity");
        }

        public static @NotNull PartInput fromConfig(@NotNull BlockPos relativePos, @NotNull RocketPartConfig config) {
            RocketPartCostCalculator.CostProfile profile = RocketPartCostCalculator.calculate(config);
            return new PartInput(
                    relativePos,
                    config,
                    profile.stats(),
                    PartIntegrity.pristine(profile.maxIntegrity()),
                    null
            );
        }

        public static @Nullable PartInput fromStack(@NotNull BlockPos relativePos, @Nullable ItemStack stack) {
            if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
                return null;
            }
            RocketPartConfig cfg = partItem.getConfig(stack);
            RocketPartComposition comp = stack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
            RocketPartCostCalculator.CostProfile profile = RocketPartCostCalculator.calculate(cfg, comp);
            RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
            PartIntegrity integrity = stack.get(GCDataComponents.PART_INTEGRITY);
            return new PartInput(
                    relativePos,
                    cfg,
                    stats != null ? stats : profile.stats(),
                    integrity != null ? integrity : PartIntegrity.pristine(profile.maxIntegrity()),
                    (comp != null && !comp.isEmpty()) ? comp : null
            );
        }

        public static @NotNull PartInput fromBlockEntity(@NotNull BlockPos padCenter, @NotNull ModularRocketPartBlockEntity be) {
            RocketPartConfig cfg = be.getConfig();
            RocketPartComposition comp = be.getComposition();
            RocketPartCostCalculator.CostProfile profile = RocketPartCostCalculator.calculate(cfg, comp);
            RocketPartStats stats = be.getStats();
            PartIntegrity integrity = be.getIntegrity();
            BlockPos rel = be.getBlockPos().subtract(padCenter);
            return new PartInput(
                    rel,
                    cfg,
                    stats != null ? stats : profile.stats(),
                    integrity != null ? integrity : PartIntegrity.pristine(profile.maxIntegrity()),
                    (comp != null && !comp.isEmpty()) ? comp : null
            );
        }
    }

    public record BuildResult(
            @NotNull RocketAssemblyGraph graph,
            @NotNull List<ValidationIssue> issues,
            double totalWetMassKg,
            double padMaxMassKg
    ) {
        public BuildResult {
            Objects.requireNonNull(graph, "graph");
            issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
        }

        public boolean hasErrors() {
            for (ValidationIssue issue : this.issues) {
                if (issue.severity() == IssueSeverity.ERROR) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasIssue(@NotNull IssueCode code) {
            for (ValidationIssue issue : this.issues) {
                if (issue.code() == code) {
                    return true;
                }
            }
            return false;
        }

        public boolean isValid() {
            return !this.graph.isEmpty() && !this.hasErrors();
        }
    }

    /**
     * Maximum wet spacecraft mass (in kg) structurally supported by a launch pad of {@code size}.
     */
    public static double maxMassKgForPad(@Nullable PlatformSize size) {
        if (size == null) {
            return Double.POSITIVE_INFINITY;
        }
        return switch (size) {
            case SIZE_1X1 -> 45_000.0;
            case SIZE_3X3 -> 250_000.0;
            case SIZE_5X5 -> 1_200_000.0;
            case SIZE_7X7 -> 4_500_000.0;
            case SIZE_9X9 -> 12_000_000.0;
            case SIZE_11X11 -> 30_000_000.0;
        };
    }

    /**
     * Analytical octagonal radial booster center offset:
     * {@code d_radial = R_core + R_booster + delta_decoupler}.
     */
    public static double computeRadialOffsetBlocks(int coreDiameter, int boosterDiameter) {
        double rCore = Math.max(1, coreDiameter) * 0.5;
        double rBooster = Math.max(1, boosterDiameter) * 0.5;
        return rCore + rBooster + DECOUPLER_STANDOFF_BLOCKS;
    }

    /**
     * Computes integer grid world/relative origin positions for a symmetric ring of radial boosters
     * surrounding {@code coreBasePos} matching GC5's 8-sided octagonal faces.
     *
     * <p>Guarantees non-overlapping integer footprints with the central core and among adjacent
     * diagonal/cardinal boosters.
     */
    public static @NotNull List<BlockPos> computeRadialBoosterOrigins(
            @NotNull BlockPos coreBasePos,
            int coreDiameter,
            int boosterDiameter,
            @NotNull RadialSymmetry symmetry
    ) {
        Objects.requireNonNull(coreBasePos, "coreBasePos");
        Objects.requireNonNull(symmetry, "symmetry");

        int safeCore = Math.max(1, coreDiameter);
        int safeBooster = Math.max(1, boosterDiameter);

        // Find exact minimum integer cardinal offset with zero octagonal footprint overlap against core
        int cardinalGridOffset = 1;
        while (octagonsOverlap(0, 0, safeCore, cardinalGridOffset, 0, safeBooster)) {
            cardinalGridOffset++;
        }

        // Find exact minimum integer diagonal offset with zero overlap against core, other diagonals, and (for 8x) cardinals
        int diagStep = 1;
        boolean includeCardinals = symmetry == RadialSymmetry.EIGHT;
        while (diagonalStepOverlaps(diagStep, safeCore, safeBooster, cardinalGridOffset, includeCardinals)) {
            diagStep++;
        }

        float[] azimuths = symmetry.azimuthDegrees();
        List<BlockPos> origins = new ArrayList<>(azimuths.length);
        for (float azDeg : azimuths) {
            double rad = Math.toRadians(azDeg);
            double sin = Math.sin(rad);
            double cos = Math.cos(rad);
            boolean diagonal = Math.abs(Math.abs(sin) - Math.abs(cos)) < 0.25;
            int dx;
            int dz;
            if (diagonal) {
                dx = sin >= 0.0 ? diagStep : -diagStep;
                dz = cos >= 0.0 ? diagStep : -diagStep;
            } else {
                dx = (int) Math.round(sin * cardinalGridOffset);
                dz = (int) Math.round(cos * cardinalGridOffset);
            }
            origins.add(coreBasePos.offset(dx, 0, dz).immutable());
        }
        return List.copyOf(origins);
    }

    /**
     * Computes the exact analytical flush visual center {@code [x, z]} (in blocks relative to pad center)
     * for a radial booster mounted at integer offset {@code (relX, relZ)} around a core of {@code coreDiameter},
     * eliminating voxel stair-step gaps on 45-degree octagonal diagonals while preventing neighbor clipping.
     */
    public static double[] computeFlushRadialVisualPosition(
            int coreDiameter,
            int boosterDiameter,
            int relX,
            int relZ,
            int symmetryCount
    ) {
        double dist = Math.hypot(relX, relZ);
        if (dist < 1.0e-4) {
            return new double[]{0.0, 0.0};
        }
        double contactDist = (Math.max(1, coreDiameter) * 0.5) + (Math.max(1, boosterDiameter) * 0.5) + 0.0625;
        double minAngularClearance = 0.0;
        if (symmetryCount >= 8) {
            // 2 * d * sin(22.5 deg) >= boosterDiameter -> prevents adjacent 8x boosters from intersecting
            minAngularClearance = Math.max(1, boosterDiameter) / (2.0 * Math.sin(Math.PI / 8.0));
        } else if (symmetryCount >= 4) {
            minAngularClearance = Math.max(1, boosterDiameter) / (2.0 * Math.sin(Math.PI / 4.0));
        }
        double targetDist = Math.max(contactDist, minAngularClearance);
        double ux = relX / dist;
        double uz = relZ / dist;
        return new double[]{ux * targetDist, uz * targetDist};
    }

    /**
     * Tests whether a radial booster placed at relative {@code (relX, relZ)} fits within the square
     * launch pad clearance envelope {@code allowedClearanceRadius}.
     */
    public static boolean fitsWithinPadClearance(
            int relX,
            int relZ,
            int boosterDiameter,
            double allowedClearanceRadius
    ) {
        double maxAxis = Math.max(Math.abs(relX), Math.abs(relZ));
        return maxAxis + (Math.max(1, boosterDiameter) * 0.5) <= allowedClearanceRadius + 1.0e-3;
    }

    private static boolean diagonalStepOverlaps(
            int diagStep,
            int coreDiam,
            int boosterDiam,
            int cardinalOffset,
            boolean includeCardinals
    ) {
        if (octagonsOverlap(0, 0, coreDiam, diagStep, diagStep, boosterDiam)) {
            return true;
        }
        if (octagonsOverlap(diagStep, diagStep, boosterDiam, -diagStep, diagStep, boosterDiam)
                || octagonsOverlap(diagStep, diagStep, boosterDiam, diagStep, -diagStep, boosterDiam)) {
            return true;
        }
        if (includeCardinals) {
            if (octagonsOverlap(diagStep, diagStep, boosterDiam, cardinalOffset, 0, boosterDiam)
                    || octagonsOverlap(diagStep, diagStep, boosterDiam, 0, cardinalOffset, boosterDiam)) {
                return true;
            }
        }
        return false;
    }

    private static boolean octagonsOverlap(int ax, int az, int aDiam, int bx, int bz, int bDiam) {
        int ra = aDiam / 2;
        int rb = bDiam / 2;
        for (int dxA = -ra; dxA <= ra; dxA++) {
            for (int dzA = -ra; dzA <= ra; dzA++) {
                if (!FootprintMode.isInsideOctagon(dxA, dzA, aDiam)) {
                    continue;
                }
                int wx = ax + dxA;
                int wz = az + dzA;
                int dxB = wx - bx;
                int dzB = wz - bz;
                if (Math.abs(dxB) <= rb && Math.abs(dzB) <= rb && FootprintMode.isInsideOctagon(dxB, dzB, bDiam)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Derives all typed KSP-style {@link AttachmentNode} interfaces exposed by a modular part.
     */
    public static @NotNull List<AttachmentNode> computeAttachmentNodes(@NotNull RocketPartConfig config) {
        Objects.requireNonNull(config, "config");
        List<AttachmentNode> nodes = new ArrayList<>(8);
        String family = config.familyId().toLowerCase(Locale.ROOT);

        if (config.radial()) {
            nodes.add(new AttachmentNode(
                    "radial_inner", AttachmentNodeType.RADIAL,
                    0.0, config.heightBlocks() * 0.5, -0.5, Direction.NORTH,
                    1, true, CrossfeedMode.ISOLATED, RadialSymmetry.EIGHT
            ));
            nodes.add(new AttachmentNode(
                    "radial_outer", AttachmentNodeType.RADIAL,
                    0.0, config.heightBlocks() * 0.5, 0.5, Direction.SOUTH,
                    1, true, CrossfeedMode.ISOLATED, RadialSymmetry.EIGHT
            ));
            return List.copyOf(nodes);
        }

        // Standard coaxial bottom & top interfaces
        nodes.add(new AttachmentNode(
                "coaxial_bottom", AttachmentNodeType.COAXIAL_BOTTOM,
                0.0, 0.0, 0.0, Direction.DOWN,
                config.diameter(), true, CrossfeedMode.ISOLATED, RadialSymmetry.NONE
        ));
        nodes.add(new AttachmentNode(
                "coaxial_top", AttachmentNodeType.COAXIAL_TOP,
                0.0, config.heightBlocks(), 0.0, Direction.UP,
                config.topDiameter(), true, CrossfeedMode.ISOLATED, RadialSymmetry.NONE
        ));

        // Radial perimeter nodes on core tanks, adapters, and interstages
        if (config.category() == RocketPartCategory.TANK
                || (config.category() == RocketPartCategory.ADAPTER && !family.contains("nose_cone"))) {
            double r = config.diameter() * 0.5;
            double midY = config.heightBlocks() * 0.5;
            boolean allowsCrossfeed = config.category() == RocketPartCategory.TANK;
            nodes.add(new AttachmentNode("radial_north", AttachmentNodeType.RADIAL, 0.0, midY, -r, Direction.NORTH, config.diameter(), allowsCrossfeed, CrossfeedMode.ISOLATED, RadialSymmetry.EIGHT));
            nodes.add(new AttachmentNode("radial_south", AttachmentNodeType.RADIAL, 0.0, midY, r, Direction.SOUTH, config.diameter(), allowsCrossfeed, CrossfeedMode.ISOLATED, RadialSymmetry.EIGHT));
            nodes.add(new AttachmentNode("radial_east", AttachmentNodeType.RADIAL, r, midY, 0.0, Direction.EAST, config.diameter(), allowsCrossfeed, CrossfeedMode.ISOLATED, RadialSymmetry.EIGHT));
            nodes.add(new AttachmentNode("radial_west", AttachmentNodeType.RADIAL, -r, midY, 0.0, Direction.WEST, config.diameter(), allowsCrossfeed, CrossfeedMode.ISOLATED, RadialSymmetry.EIGHT));
        }

        if (family.contains("thrust_puck")) {
            nodes.add(new AttachmentNode(
                    "engine_cluster", AttachmentNodeType.ENGINE_CLUSTER,
                    0.0, 0.0, 0.0, Direction.DOWN,
                    config.diameter(), true, CrossfeedMode.ONE_WAY_DOWN, RadialSymmetry.NONE
            ));
        }

        if (family.startsWith("cargo_bay_") || family.startsWith("fairing_barrel")) {
            nodes.add(new AttachmentNode(
                    "payload_rail", AttachmentNodeType.PAYLOAD,
                    0.0, 0.25, 0.0, Direction.UP,
                    Math.max(1, config.diameter() - 2), false, CrossfeedMode.ISOLATED, RadialSymmetry.NONE
            ));
        }

        return List.copyOf(nodes);
    }

    public static boolean isStageSeparator(@NotNull RocketPartConfig config) {
        String family = config.familyId().toLowerCase(Locale.ROOT);
        return family.equals("decoupler_inline")
                || family.equals("decoupler_stage_inline")
                || family.equals("interstage_hollow")
                || family.equals("interstage_hotstaging")
                || family.equals("interstage_engine_shroud");
    }

    public static boolean isHotStagingSeparator(@NotNull RocketPartConfig config) {
        return config.familyId().toLowerCase(Locale.ROOT).contains("hotstaging");
    }

    public static boolean isSolidBoosterPart(@NotNull RocketPartConfig config) {
        String id = (config.familyId() + "_" + config.modelId()).toLowerCase(Locale.ROOT);
        return id.contains("srb_") || id.contains("solid")
                || (config.category() == RocketPartCategory.ENGINE && PropellantType.forEngine(config) == PropellantType.SOLID);
    }

    public static boolean isLandingGearPart(@NotNull RocketPartConfig config) {
        return config.familyId().toLowerCase(Locale.ROOT).contains("landing_gear");
    }

    public static boolean isLesAbortPart(@NotNull RocketPartConfig config) {
        String family = config.familyId().toLowerCase(Locale.ROOT);
        return family.contains("las_escape_tower") || family.contains("les_");
    }

    public static boolean isFairingPart(@NotNull RocketPartConfig config) {
        return config.familyId().toLowerCase(Locale.ROOT).startsWith("fairing_");
    }

    /**
     * Compiles a bottom-to-top coaxial list of {@link ItemStack}s into a {@link BuildResult}.
     */
    public static @NotNull BuildResult buildFromStacks(
            @NotNull List<ItemStack> coaxialStacks,
            @Nullable PlatformSize platformSize,
            @Nullable PlatformBlueprint blueprint
    ) {
        return buildFromStacksWithRadial(
                coaxialStacks, List.of(), RadialSymmetry.NONE, CrossfeedMode.ISOLATED, platformSize, blueprint
        );
    }

    /**
     * Compiles coaxial {@link ItemStack}s plus an optional radial booster column template replicated
     * around the core with {@code symmetry} and {@code crossfeedMode}.
     */
    public static @NotNull BuildResult buildFromStacksWithRadial(
            @NotNull List<ItemStack> coaxialStacks,
            @NotNull List<ItemStack> radialBoosterTemplate,
            @NotNull RadialSymmetry symmetry,
            @NotNull CrossfeedMode crossfeedMode,
            @Nullable PlatformSize platformSize,
            @Nullable PlatformBlueprint blueprint
    ) {
        List<PartInput> inputs = new ArrayList<>();
        int curY = 1;
        int coreBaseDiameter = 1;
        boolean foundCore = false;

        for (ItemStack stack : coaxialStacks) {
            PartInput input = PartInput.fromStack(new BlockPos(0, curY, 0), stack);
            if (input == null) {
                continue;
            }
            if (!foundCore) {
                coreBaseDiameter = input.config().diameter();
                foundCore = true;
            }
            inputs.add(input);
            curY += Math.max(1, input.config().heightBlocks());
        }

        if (!radialBoosterTemplate.isEmpty() && symmetry != RadialSymmetry.NONE) {
            List<PartInput> templateParts = new ArrayList<>();
            int maxBoosterDiam = 1;
            for (ItemStack stack : radialBoosterTemplate) {
                PartInput sample = PartInput.fromStack(BlockPos.ZERO, stack);
                if (sample != null) {
                    templateParts.add(sample);
                    maxBoosterDiam = Math.max(maxBoosterDiam, Math.max(sample.config().diameter(), sample.config().topDiameter()));
                }
            }
            if (!templateParts.isEmpty()) {
                List<BlockPos> boosterBases = computeRadialBoosterOrigins(new BlockPos(0, 1, 0), coreBaseDiameter, maxBoosterDiam, symmetry);
                for (BlockPos base : boosterBases) {
                    int bY = base.getY();
                    for (PartInput tpl : templateParts) {
                        inputs.add(new PartInput(
                                new BlockPos(base.getX(), bY, base.getZ()),
                                tpl.config(),
                                tpl.stats(),
                                tpl.integrity(),
                                tpl.composition()
                        ));
                        bY += Math.max(1, tpl.config().heightBlocks());
                    }
                }
            }
        }

        return build(inputs, platformSize, blueprint, Map.of(), Map.of(), crossfeedMode);
    }

    /**
     * Compiles world-placed {@link ModularRocketPartBlockEntity} instances standing above {@code padCenter}.
     */
    public static @NotNull BuildResult buildFromPad(
            @NotNull BlockPos padCenter,
            @NotNull List<ModularRocketPartBlockEntity> entities,
            @Nullable PlatformSize platformSize,
            @Nullable PlatformBlueprint blueprint,
            @Nullable Map<Integer, Long> stageCurrentFuel,
            @Nullable Map<String, Long> boosterCurrentFuel,
            @NotNull CrossfeedMode defaultRadialCrossfeed
    ) {
        List<PartInput> inputs = new ArrayList<>(entities.size());
        for (ModularRocketPartBlockEntity be : entities) {
            if (be != null && !be.isRemoved()) {
                inputs.add(PartInput.fromBlockEntity(padCenter, be));
            }
        }
        return build(inputs, platformSize, blueprint, stageCurrentFuel, boosterCurrentFuel, defaultRadialCrossfeed);
    }

    /**
     * Core deterministic compiler from normalized {@link PartInput} records into a validated
     * {@link RocketAssemblyGraph}.
     */
    public static @NotNull BuildResult build(
            @NotNull List<PartInput> inputs,
            @Nullable PlatformSize platformSize,
            @Nullable PlatformBlueprint blueprint,
            @Nullable Map<Integer, Long> stageCurrentFuel,
            @Nullable Map<String, Long> boosterCurrentFuel,
            @NotNull CrossfeedMode defaultRadialCrossfeed
    ) {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(defaultRadialCrossfeed, "defaultRadialCrossfeed");

        double padMaxMassKg = maxMassKgForPad(platformSize);
        if (inputs.isEmpty()) {
            return new BuildResult(
                    RocketAssemblyGraph.EMPTY,
                    List.of(new ValidationIssue(IssueCode.EMPTY_ASSEMBLY, IssueSeverity.ERROR, "Nenhum módulo montado no pad", null)),
                    0.0,
                    padMaxMassKg
            );
        }

        List<ValidationIssue> issues = new ArrayList<>();
        List<PartInput> coreInputs = new ArrayList<>();
        List<PartInput> radialInputs = new ArrayList<>();

        for (PartInput input : inputs) {
            BlockPos rel = input.relativePos();
            if (rel.getX() == 0 && rel.getZ() == 0 && !input.config().radial()) {
                coreInputs.add(input);
            } else {
                radialInputs.add(input);
            }
        }

        coreInputs.sort(Comparator.comparingInt(p -> p.relativePos().getY()));

        // 1. Validate coaxial diameter continuity: below.topDiameter() == current.diameter()
        for (int i = 1; i < coreInputs.size(); i++) {
            PartInput below = coreInputs.get(i - 1);
            PartInput current = coreInputs.get(i);
            int expectedDiam = below.config().topDiameter();
            int actualDiam = current.config().diameter();
            if (expectedDiam != actualDiam) {
                issues.add(new ValidationIssue(
                        IssueCode.DIAMETER_MISMATCH,
                        IssueSeverity.ERROR,
                        String.format(
                                Locale.ROOT,
                                "Diâmetro incompatível (%dx%d -> %dx%d): use um adaptador ou interestágio",
                                expectedDiam, expectedDiam, actualDiam, actualDiam
                        ),
                        current.relativePos()
                ));
            }
        }

        // 2. Split coaxial core parts bottom-to-top into RocketStageNode builders
        List<StageAccumulator> stageAccumulators = new ArrayList<>();
        StageAccumulator currentStage = new StageAccumulator(0);
        for (int i = 0; i < coreInputs.size(); i++) {
            PartInput part = coreInputs.get(i);
            boolean separator = isStageSeparator(part.config());
            boolean hasPartsAbove = i + 1 < coreInputs.size();

            if (separator && !currentStage.coreInputs.isEmpty()) {
                currentStage.coreInputs.add(part);
                currentStage.decouplerRelativePos = Optional.of(part.relativePos());
                currentStage.hotStaging = isHotStagingSeparator(part.config());
                if (hasPartsAbove) {
                    stageAccumulators.add(currentStage);
                    currentStage = new StageAccumulator(stageAccumulators.size());
                }
            } else {
                currentStage.coreInputs.add(part);
            }
        }
        if (!currentStage.coreInputs.isEmpty() || stageAccumulators.isEmpty()) {
            stageAccumulators.add(currentStage);
        }

        // 3. Group off-axis radial booster columns and assign them to parent stages
        Map<Long, List<PartInput>> columnsByXz = new LinkedHashMap<>();
        List<PartInput> radialDecouplers = new ArrayList<>();
        for (PartInput radial : radialInputs) {
            if (radial.config().radial() || radial.config().familyId().toLowerCase(Locale.ROOT).contains("decoupler_radial")) {
                radialDecouplers.add(radial);
            } else {
                long xzKey = packXz(radial.relativePos().getX(), radial.relativePos().getZ());
                columnsByXz.computeIfAbsent(xzKey, ignored -> new ArrayList<>()).add(radial);
            }
        }

        // Attach radial columns into ring groups per parent stage
        Map<Integer, Map<String, List<BoosterColumn>>> stageRings = new LinkedHashMap<>();
        for (Map.Entry<Long, List<PartInput>> entry : columnsByXz.entrySet()) {
            List<PartInput> colParts = entry.getValue();
            colParts.sort(Comparator.comparingInt(p -> p.relativePos().getY()));
            // Validate diameter continuity within each booster column as well
            for (int i = 1; i < colParts.size(); i++) {
                PartInput below = colParts.get(i - 1);
                PartInput cur = colParts.get(i);
                if (below.config().topDiameter() != cur.config().diameter()) {
                    issues.add(new ValidationIssue(
                            IssueCode.DIAMETER_MISMATCH,
                            IssueSeverity.ERROR,
                            String.format(
                                    Locale.ROOT,
                                    "Booster radial com diâmetro incompatível (%dx%d -> %dx%d)",
                                    below.config().topDiameter(), below.config().topDiameter(),
                                    cur.config().diameter(), cur.config().diameter()
                            ),
                            cur.relativePos()
                    ));
                }
            }
            PartInput base = colParts.get(0);
            int colX = base.relativePos().getX();
            int colZ = base.relativePos().getZ();
            int baseY = base.relativePos().getY();
            double radialDist = Math.hypot(colX, colZ);

            int parentStageIdx = resolveParentStageIndex(stageAccumulators, baseY);
            int ringRadiusBucket = (int) Math.round(radialDist * 2.0);
            int baseYBucket = baseY / 3;
            String ringKey = "r" + ringRadiusBucket + "_y" + baseYBucket;

            stageRings.computeIfAbsent(parentStageIdx, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(ringKey, ignored -> new ArrayList<>())
                    .add(new BoosterColumn(colX, colZ, baseY, radialDist, colParts));
        }

        // Include standalone radial decouplers on Stage 0 if no booster body was attached yet
        if (!radialDecouplers.isEmpty() && columnsByXz.isEmpty()) {
            StageAccumulator s0 = stageAccumulators.get(0);
            s0.looseRadialDecouplers.addAll(radialDecouplers);
        }

        // 4. Materialize RocketStageNodes, RadialBoosterGroups, crew seats, and flags
        List<RocketStageNode> stages = new ArrayList<>(stageAccumulators.size());
        List<LocalSeatOffset> crewSeats = new ArrayList<>();
        boolean hasLandingGear = false;
        boolean hasLesAbort = false;
        boolean hasFairing = false;
        boolean hasEngine = false;
        boolean hasCommand = false;

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        double maxRadiusBlocks = 0.5;
        double totalWetMassKg = 0.0;

        for (StageAccumulator acc : stageAccumulators) {
            int stageIdx = acc.stageIndex;
            List<MountedPartNode> coreNodes = new ArrayList<>(acc.coreInputs.size());
            long stageFuelCap = 0L;

            for (PartInput p : acc.coreInputs) {
                coreNodes.add(new MountedPartNode(
                        p.relativePos(),
                        p.config(),
                        p.stats(),
                        p.integrity(),
                        Optional.ofNullable(p.composition()),
                        stageIdx,
                        false
                ));
                stageFuelCap += p.stats().fuelCapacityDroplets();
                hasLandingGear |= isLandingGearPart(p.config());
                hasLesAbort |= isLesAbortPart(p.config());
                hasFairing |= isFairingPart(p.config());
                if (p.config().category() == RocketPartCategory.ENGINE && p.stats().thrustKn() > 0.0) {
                    hasEngine = true;
                }
                if (p.config().category() == RocketPartCategory.COMMAND) {
                    hasCommand = true;
                    appendCrewSeats(p, crewSeats);
                }

                int y0 = p.relativePos().getY();
                int y1 = y0 + Math.max(1, p.config().heightBlocks());
                minY = Math.min(minY, y0);
                maxY = Math.max(maxY, y1);
                double partRadius = Math.max(p.config().diameter(), p.config().topDiameter()) * 0.5;
                maxRadiusBlocks = Math.max(maxRadiusBlocks, partRadius);
            }

            PropellantType stageProp = resolvePropellantForInputs(acc.coreInputs);
            long stageFuelCur = stageCurrentFuel != null
                    ? Math.clamp(stageCurrentFuel.getOrDefault(stageIdx, 0L), 0L, stageFuelCap)
                    : 0L;
            totalWetMassKg += computeInputsDryMass(acc.coreInputs)
                    + (stageFuelCap / RocketFlightValidator.DROPLETS_PER_BUCKET) * stageProp.densityKgPerBucket();

            // Build radial booster groups sorted outermost-first for Asparagus drainPriorityLayer
            Map<String, List<BoosterColumn>> rings = stageRings.getOrDefault(stageIdx, Map.of());
            List<RingCandidate> candidates = mergeCompanionEightWayRings(rings);
            // Outermost rings receive drainPriorityLayer = 0 so Asparagus drains outer pairs first!
            candidates.sort(Comparator.comparingDouble(RingCandidate::avgRadialDistance).reversed());

            List<RadialBoosterGroup> radialGroups = new ArrayList<>(candidates.size());
            for (int ringIdx = 0; ringIdx < candidates.size(); ringIdx++) {
                RingCandidate cand = candidates.get(ringIdx);
                RadialSymmetry symmetry = inferSymmetry(cand.columns(), issues);
                List<PartInput> groupInputs = new ArrayList<>();
                List<MountedPartNode> groupNodes = new ArrayList<>();
                long groupFuelCap = 0L;
                boolean solid = false;

                for (BoosterColumn col : cand.columns()) {
                    for (PartInput p : col.parts()) {
                        groupInputs.add(p);
                        groupNodes.add(new MountedPartNode(
                                p.relativePos(),
                                p.config(),
                                p.stats(),
                                p.integrity(),
                                Optional.ofNullable(p.composition()),
                                stageIdx,
                                true
                        ));
                        groupFuelCap += p.stats().fuelCapacityDroplets();
                        solid |= isSolidBoosterPart(p.config());
                        hasLandingGear |= isLandingGearPart(p.config());
                        hasFairing |= isFairingPart(p.config());
                        if (p.config().category() == RocketPartCategory.ENGINE && p.stats().thrustKn() > 0.0) {
                            hasEngine = true;
                        }
                        int y0 = p.relativePos().getY();
                        int y1 = y0 + Math.max(1, p.config().heightBlocks());
                        minY = Math.min(minY, y0);
                        maxY = Math.max(maxY, y1);
                        double outerEdge = col.radialDistance()
                                + Math.max(p.config().diameter(), p.config().topDiameter()) * 0.5;
                        maxRadiusBlocks = Math.max(maxRadiusBlocks, outerEdge);
                    }
                }

                // Attach any nearby radial decouplers matching this ring
                for (PartInput dec : radialDecouplers) {
                    if (isDecouplerNearRing(dec.relativePos(), cand.columns())) {
                        groupNodes.add(new MountedPartNode(
                                dec.relativePos(),
                                dec.config(),
                                dec.stats(),
                                dec.integrity(),
                                Optional.ofNullable(dec.composition()),
                                stageIdx,
                                true
                        ));
                    }
                }

                PropellantType groupProp = solid ? PropellantType.SOLID : resolvePropellantForInputs(groupInputs);
                CrossfeedMode effectiveCrossfeed = solid ? CrossfeedMode.ISOLATED : defaultRadialCrossfeed;
                String groupId = "stage" + stageIdx + "_radial_" + ringIdx;
                long groupFuelCur = boosterCurrentFuel != null
                        ? Math.clamp(boosterCurrentFuel.getOrDefault(groupId, solid ? groupFuelCap : 0L), 0L, groupFuelCap)
                        : (solid ? groupFuelCap : 0L);

                totalWetMassKg += computeInputsDryMass(groupInputs)
                        + (groupFuelCap / RocketFlightValidator.DROPLETS_PER_BUCKET) * groupProp.densityKgPerBucket();

                radialGroups.add(new RadialBoosterGroup(
                        groupId,
                        symmetry,
                        effectiveCrossfeed,
                        solid,
                        ringIdx,
                        cand.avgRadialDistance(),
                        groupProp.name().toLowerCase(Locale.ROOT),
                        groupFuelCap,
                        groupFuelCur,
                        groupNodes,
                        false
                ));
            }

            stages.add(new RocketStageNode(
                    stageIdx,
                    acc.decouplerRelativePos,
                    acc.hotStaging,
                    stageProp.name().toLowerCase(Locale.ROOT),
                    stageFuelCap,
                    stageFuelCur,
                    coreNodes,
                    radialGroups,
                    false
            ));
        }

        // 5. Clearance & Pad Mass Limit Checks
        if (platformSize != null) {
            double allowedCoreHalf = platformSize.diameter() * 0.5;
            double allowedTotalRadius = computeAllowedClearanceRadius(platformSize, blueprint);
            for (PartInput core : coreInputs) {
                int maxDiam = Math.max(core.config().diameter(), core.config().topDiameter());
                if (maxDiam > platformSize.diameter()) {
                    issues.add(new ValidationIssue(
                            IssueCode.CLEARANCE_OBSTRUCTED,
                            IssueSeverity.ERROR,
                            String.format(
                                    Locale.ROOT,
                                    "Módulo Ø%d excede o diâmetro da plataforma (Ø%d)",
                                    maxDiam, platformSize.diameter()
                            ),
                            core.relativePos()
                    ));
                    break;
                }
            }
            boolean radialClearanceExceeded = false;
            for (RocketStageNode stage : stages) {
                for (RadialBoosterGroup group : stage.radialGroups()) {
                    for (MountedPartNode node : group.parts()) {
                        int partDiam = Math.max(node.config().diameter(), node.config().topDiameter());
                        if (!fitsWithinPadClearance(
                                node.relativePos().getX(),
                                node.relativePos().getZ(),
                                partDiam,
                                allowedTotalRadius
                        )) {
                            radialClearanceExceeded = true;
                            break;
                        }
                    }
                    if (radialClearanceExceeded) {
                        break;
                    }
                }
                if (radialClearanceExceeded) {
                    break;
                }
            }
            if (radialClearanceExceeded) {
                issues.add(new ValidationIssue(
                        IssueCode.CLEARANCE_OBSTRUCTED,
                        IssueSeverity.ERROR,
                        String.format(
                                Locale.ROOT,
                                "Boosters excedem a folga da plataforma (%.1fb)",
                                allowedTotalRadius * 2.0
                        ),
                        null
                ));
            }
            if (Double.isFinite(padMaxMassKg) && totalWetMassKg > padMaxMassKg) {
                issues.add(new ValidationIssue(
                        IssueCode.PAD_OVERWEIGHT,
                        IssueSeverity.ERROR,
                        String.format(
                                Locale.ROOT,
                                "Massa úmida %.1ft excede limite do pad (%.1ft)",
                                totalWetMassKg / 1000.0, padMaxMassKg / 1000.0
                        ),
                        null
                ));
            }
            // Also check vertical clearance for horizontal hangars (overhead crane roof)
            if (blueprint != null && blueprint.type() == dev.galacticraft.api.platform.PlatformType.HORIZONTAL) {
                int maxClearanceY = blueprint.clearanceMax().getY();
                if (maxY > maxClearanceY + 1) {
                    issues.add(new ValidationIssue(
                            IssueCode.CLEARANCE_OBSTRUCTED,
                            IssueSeverity.ERROR,
                            "Altura do foguete excede o teto do hangar horizontal",
                            null
                    ));
                }
            }
        }

        if (!hasEngine) {
            issues.add(new ValidationIssue(IssueCode.NO_ENGINE, IssueSeverity.WARNING, "Nenhum motor instalado", null));
        }
        if (!hasCommand) {
            issues.add(new ValidationIssue(IssueCode.NO_COMMAND, IssueSeverity.WARNING, "Sem módulo de comando", null));
        }

        int totalHeight = minY <= maxY ? (maxY - minY) : 0;
        RocketAssemblyGraph graph = new RocketAssemblyGraph(
                stages,
                crewSeats,
                totalHeight,
                maxRadiusBlocks,
                hasLandingGear,
                hasLesAbort,
                hasFairing
        );

        return new BuildResult(graph, issues, totalWetMassKg, padMaxMassKg);
    }

    /**
     * Computes the horizontal clearance radius (from pad center to edge) allowed for coaxial + radial parts.
     */
    public static double computeAllowedClearanceRadius(@NotNull PlatformSize size, @Nullable PlatformBlueprint blueprint) {
        double padHalf = size.padDeckWidth() * 0.5;
        if (blueprint == null) {
            return padHalf + 1.0;
        }
        int maxHalfX = Math.max(Math.abs(blueprint.clearanceMin().getX()), Math.abs(blueprint.clearanceMax().getX()));
        int maxHalfZ = Math.max(Math.abs(blueprint.clearanceMin().getZ()), Math.abs(blueprint.clearanceMax().getZ()));
        double blueprintHalf = Math.min(maxHalfX, maxHalfZ) + 1.0;
        return Math.max(padHalf + 1.0, blueprintHalf);
    }

    private static int resolveParentStageIndex(List<StageAccumulator> stages, int boosterBaseY) {
        for (StageAccumulator stage : stages) {
            if (stage.coreInputs.isEmpty()) {
                continue;
            }
            int sMinY = stage.coreInputs.get(0).relativePos().getY();
            PartInput top = stage.coreInputs.get(stage.coreInputs.size() - 1);
            int sMaxY = top.relativePos().getY() + Math.max(1, top.config().heightBlocks());
            if (boosterBaseY >= sMinY - 1 && boosterBaseY < sMaxY) {
                return stage.stageIndex;
            }
        }
        return 0;
    }

    private static List<RingCandidate> mergeCompanionEightWayRings(Map<String, List<BoosterColumn>> rings) {
        List<RingCandidate> raw = new ArrayList<>(rings.size());
        for (Map.Entry<String, List<BoosterColumn>> rEntry : rings.entrySet()) {
            List<BoosterColumn> cols = rEntry.getValue();
            double avgDist = 0.0;
            for (BoosterColumn col : cols) {
                avgDist += col.radialDistance();
            }
            avgDist = cols.isEmpty() ? 0.0 : (avgDist / cols.size());
            raw.add(new RingCandidate(rEntry.getKey(), avgDist, cols));
        }

        boolean[] consumed = new boolean[raw.size()];
        List<RingCandidate> merged = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            if (consumed[i]) {
                continue;
            }
            RingCandidate a = raw.get(i);
            int cardStep = cardinalFourStep(a.columns());
            if (cardStep > 0) {
                int yBucketA = a.columns().get(0).baseY() / 3;
                int matchIdx = -1;
                for (int j = 0; j < raw.size(); j++) {
                    if (i == j || consumed[j]) {
                        continue;
                    }
                    RingCandidate b = raw.get(j);
                    int diagStep = diagonalFourStep(b.columns());
                    if (diagStep > 0 && (b.columns().get(0).baseY() / 3) == yBucketA
                            && (diagStep == cardStep || diagStep == cardStep - 1)) {
                        matchIdx = j;
                        break;
                    }
                }
                if (matchIdx >= 0) {
                    RingCandidate b = raw.get(matchIdx);
                    consumed[i] = true;
                    consumed[matchIdx] = true;
                    List<BoosterColumn> combined = new ArrayList<>(a.columns().size() + b.columns().size());
                    combined.addAll(a.columns());
                    combined.addAll(b.columns());
                    double avgDist = (a.avgRadialDistance() * a.columns().size() + b.avgRadialDistance() * b.columns().size())
                            / combined.size();
                    merged.add(new RingCandidate(a.key() + "+" + b.key(), avgDist, combined));
                    continue;
                }
            }
            consumed[i] = true;
            merged.add(a);
        }
        return merged;
    }

    private static int cardinalFourStep(List<BoosterColumn> cols) {
        if (cols.size() != 4) {
            return -1;
        }
        int step = -1;
        for (BoosterColumn col : cols) {
            int ax = Math.abs(col.x());
            int az = Math.abs(col.z());
            if (!((ax > 0 && az == 0) || (az > 0 && ax == 0))) {
                return -1;
            }
            int s = Math.max(ax, az);
            if (step < 0) {
                step = s;
            } else if (step != s) {
                return -1;
            }
        }
        return step;
    }

    private static int diagonalFourStep(List<BoosterColumn> cols) {
        if (cols.size() != 4) {
            return -1;
        }
        int step = -1;
        for (BoosterColumn col : cols) {
            int ax = Math.abs(col.x());
            int az = Math.abs(col.z());
            if (ax == 0 || ax != az) {
                return -1;
            }
            if (step < 0) {
                step = ax;
            } else if (step != ax) {
                return -1;
            }
        }
        return step;
    }

    private static RadialSymmetry inferSymmetry(List<BoosterColumn> columns, List<ValidationIssue> issues) {
        int count = columns.size();
        if (count <= 1) {
            return RadialSymmetry.NONE;
        }
        if (count == 2) {
            return RadialSymmetry.TWO;
        }
        if (count == 4) {
            boolean allDiagonal = true;
            for (BoosterColumn col : columns) {
                if (col.x() == 0 || col.z() == 0) {
                    allDiagonal = false;
                    break;
                }
            }
            return allDiagonal ? RadialSymmetry.FOUR_DIAGONAL : RadialSymmetry.FOUR_CARDINAL;
        }
        if (count == 8) {
            return RadialSymmetry.EIGHT;
        }
        issues.add(new ValidationIssue(
                IssueCode.UNSUPPORTED_NODE,
                IssueSeverity.WARNING,
                "Simetria radial assimétrica (" + count + " boosters; recomendado 2x/4x/8x)",
                columns.get(0).parts().get(0).relativePos()
        ));
        return count < 4 ? RadialSymmetry.TWO : (count < 8 ? RadialSymmetry.FOUR_CARDINAL : RadialSymmetry.EIGHT);
    }

    private static boolean isDecouplerNearRing(BlockPos decPos, List<BoosterColumn> columns) {
        for (BoosterColumn col : columns) {
            int dx = Math.abs(decPos.getX() - col.x());
            int dz = Math.abs(decPos.getZ() - col.z());
            int colTopY = col.baseY() + 16;
            if (dx <= 3 && dz <= 3 && decPos.getY() >= col.baseY() - 1 && decPos.getY() <= colTopY) {
                return true;
            }
        }
        return false;
    }

    private static PropellantType resolvePropellantForInputs(List<PartInput> parts) {
        PropellantType dominantEngine = null;
        double maxThrust = -1.0;
        PropellantType dominantTank = null;
        long maxCap = -1L;

        for (PartInput part : parts) {
            RocketPartConfig cfg = part.config();
            if (cfg.category() == RocketPartCategory.ENGINE && part.stats().thrustKn() > maxThrust) {
                maxThrust = part.stats().thrustKn();
                dominantEngine = PropellantType.forEngine(cfg);
            }
            if (cfg.category() == RocketPartCategory.TANK && part.stats().fuelCapacityDroplets() > maxCap) {
                maxCap = part.stats().fuelCapacityDroplets();
                dominantTank = PropellantType.forTank(cfg);
            }
        }
        if (dominantEngine != null) {
            return dominantEngine;
        }
        return dominantTank != null ? dominantTank : PropellantType.RP1;
    }

    private static double computeInputsDryMass(List<PartInput> parts) {
        double sum = 0.0;
        for (PartInput part : parts) {
            sum += part.stats().dryWeightKg();
        }
        return sum;
    }

    private static void appendCrewSeats(PartInput commandPart, List<LocalSeatOffset> outSeats) {
        RocketPartConfig cfg = commandPart.config();
        String id = cfg.familyId().toLowerCase(Locale.ROOT);
        if (id.contains("probe_") || id.contains("avionics_ring") || id.contains("docking_port") || id.contains("las_escape")) {
            return;
        }
        int seatCount;
        if (id.contains("stargarrison") || cfg.diameter() >= 7) {
            seatCount = 6;
        } else if (id.contains("orion") || cfg.diameter() >= 5) {
            seatCount = 4;
        } else if (id.contains("gemini") || cfg.diameter() >= 3) {
            seatCount = 2;
        } else {
            seatCount = 1;
        }
        double seatY = commandPart.relativePos().getY() + Math.max(0.5, cfg.heightBlocks() * 0.40);
        if (seatCount == 1) {
            outSeats.add(new LocalSeatOffset(0.0, seatY, 0.0, cfg.modelId()));
            return;
        }
        double ringRadius = Math.max(0.45, (cfg.diameter() - 2) * 0.25);
        for (int s = 0; s < seatCount; s++) {
            double angle = (2.0 * Math.PI * s) / seatCount;
            outSeats.add(new LocalSeatOffset(
                    Math.cos(angle) * ringRadius,
                    seatY,
                    Math.sin(angle) * ringRadius,
                    cfg.modelId()
            ));
        }
    }

    private static long packXz(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFF_FFFFL);
    }

    private static final class StageAccumulator {
        final int stageIndex;
        final List<PartInput> coreInputs = new ArrayList<>();
        final List<PartInput> looseRadialDecouplers = new ArrayList<>();
        Optional<BlockPos> decouplerRelativePos = Optional.empty();
        boolean hotStaging = false;

        StageAccumulator(int stageIndex) {
            this.stageIndex = stageIndex;
        }
    }

    private record BoosterColumn(int x, int z, int baseY, double radialDistance, List<PartInput> parts) {
    }

    private record RingCandidate(String key, double avgRadialDistance, List<BoosterColumn> columns) {
    }
}
