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

package dev.galacticraft.mod.research;

import dev.galacticraft.mod.content.GCCelestialBodies;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client-side synchronized cache of the agency's KSP+ research tree, primary Science Points ({@code SP}),
 * 5 Scientific Domains, physical Milestones, Science Archives, Astro Atlas discoveries, CommNet relays,
 * and live telemetry feed.
 */
public final class ClientResearchCache {
    private static int sciencePoints = 0;
    private static int totalScienceEarned = 0;
    private static boolean quantumArchiveBoost = false;
    private static final Set<ResourceLocation> UNLOCKED_NODES = new LinkedHashSet<>();
    private static final EnumMap<ResearchDomain, Integer> DOMAIN_POINTS = new EnumMap<>(ResearchDomain.class);
    private static final Set<String> COMPLETED_MILESTONES = new LinkedHashSet<>();
    private static final Set<ResourceLocation> MAPPED_BODIES = new LinkedHashSet<>();
    private static List<ExperimentRecord> scienceArchives = List.of();
    private static List<AstroDiscovery> astroAtlas = List.of();
    private static int activeRelayCount = 0;
    private static int networkBonusPct = 100;
    private static List<String> relaySummaries = List.of();
    private static List<String> experimentLog = List.of();

    static {
        reset();
    }

    private ClientResearchCache() {
    }

    public static void applySnapshot(
            int sp,
            int totalSp,
            boolean quantumBoost,
            @NotNull List<ResourceLocation> unlockedNodes,
            int @NotNull [] domainPoints,
            @NotNull List<String> milestones,
            @NotNull List<ResourceLocation> mappedBodies,
            @NotNull List<ExperimentRecord> archives,
            @NotNull List<AstroDiscovery> atlas,
            int relays,
            int bonusPct,
            @NotNull List<String> relaysInfo,
            @NotNull List<String> log
    ) {
        sciencePoints = Math.max(0, sp);
        totalScienceEarned = Math.max(sciencePoints, totalSp);
        quantumArchiveBoost = quantumBoost;
        UNLOCKED_NODES.clear();
        UNLOCKED_NODES.addAll(unlockedNodes);
        for (ResearchDomain domain : ResearchDomain.values()) {
            int val = domain.ordinal() < domainPoints.length ? domainPoints[domain.ordinal()] : 0;
            DOMAIN_POINTS.put(domain, Math.max(0, val));
        }
        COMPLETED_MILESTONES.clear();
        COMPLETED_MILESTONES.addAll(milestones);
        MAPPED_BODIES.clear();
        MAPPED_BODIES.add(GCCelestialBodies.EARTH.location());
        MAPPED_BODIES.addAll(mappedBodies);
        scienceArchives = List.copyOf(archives);
        astroAtlas = List.copyOf(atlas);
        activeRelayCount = Math.max(0, relays);
        networkBonusPct = Math.max(100, bonusPct);
        relaySummaries = List.copyOf(relaysInfo);
        experimentLog = List.copyOf(log);
    }

    public static void reset() {
        sciencePoints = 0;
        totalScienceEarned = 0;
        quantumArchiveBoost = false;
        UNLOCKED_NODES.clear();
        for (ResearchDomain domain : ResearchDomain.values()) {
            DOMAIN_POINTS.put(domain, 0);
        }
        COMPLETED_MILESTONES.clear();
        MAPPED_BODIES.clear();
        MAPPED_BODIES.add(GCCelestialBodies.EARTH.location());
        scienceArchives = List.of();
        astroAtlas = List.of();
        activeRelayCount = 0;
        networkBonusPct = 100;
        relaySummaries = List.of();
        experimentLog = List.of();
    }

    public static int sciencePoints() {
        return sciencePoints;
    }

    public static int totalScienceEarned() {
        return totalScienceEarned;
    }

    public static boolean quantumArchiveBoost() {
        return quantumArchiveBoost;
    }

    public static boolean isUnlocked(@NotNull ResourceLocation nodeId) {
        return UNLOCKED_NODES.contains(nodeId);
    }

    public static @NotNull Set<ResourceLocation> unlockedNodes() {
        return Collections.unmodifiableSet(UNLOCKED_NODES);
    }

    public static int domainPoints(@NotNull ResearchDomain domain) {
        return DOMAIN_POINTS.getOrDefault(domain, 0);
    }

    public static @NotNull Map<String, Integer> domainPointsById() {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<ResearchDomain, Integer> entry : DOMAIN_POINTS.entrySet()) {
            map.put(entry.getKey().id(), entry.getValue());
        }
        return map;
    }

    public static boolean hasMilestone(@Nullable String milestoneId) {
        return milestoneId == null || milestoneId.isEmpty() || COMPLETED_MILESTONES.contains(milestoneId);
    }

    public static @NotNull Set<String> completedMilestones() {
        return Collections.unmodifiableSet(COMPLETED_MILESTONES);
    }

    public static boolean isBodyMapped(@NotNull ResourceLocation bodyId) {
        return MAPPED_BODIES.contains(bodyId);
    }

    public static @NotNull Set<ResourceLocation> mappedBodies() {
        return Collections.unmodifiableSet(MAPPED_BODIES);
    }

    public static @NotNull List<ExperimentRecord> scienceArchives() {
        return scienceArchives;
    }

    public static @NotNull List<AstroDiscovery> astroAtlas() {
        return astroAtlas;
    }

    public static int activeRelayCount() {
        return activeRelayCount;
    }

    public static int networkBonusPct() {
        return networkBonusPct;
    }

    public static @NotNull List<String> relaySummaries() {
        return relaySummaries;
    }

    public static @NotNull List<String> experimentLog() {
        return experimentLog;
    }
}
