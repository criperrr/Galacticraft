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

import dev.galacticraft.api.accessor.ServerResearchAccessor;
import dev.galacticraft.mod.network.s2c.SyncResearchProgressPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Server-side coordinator for the KSP+ in-situ planetary laboratory research economy.
 * Persists primary Science Points ({@code SP}), secondary 5 Scientific Domains, physical Milestones,
 * KSP Science Archives with diminishing returns, and Telescope Astro Atlas discoveries in
 * {@link TeamResearchState}, syncing live snapshots via {@link SyncResearchProgressPayload}.
 */
public final class ResearchManager {
    private ResearchManager() {
    }

    public record ScienceAwardResult(
            int spAwarded,
            int domainPointsAwarded,
            int runCount,
            int diminishingPct,
            int remainingSp,
            boolean newMilestone
    ) {
    }

    public static @NotNull TeamResearchState get(@NotNull MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TeamResearchState.factory(), TeamResearchState.DATA_NAME);
    }

    public static @NotNull TeamResearchState get(@NotNull ServerLevel level) {
        return get(level.getServer());
    }

    /**
     * Backward-compatible experiment recording hook (awards both SP and Domain points with diminishing returns).
     */
    public static int recordExperiment(
            @NotNull ServerLevel level,
            @Nullable ResourceLocation unlockedNodeId,
            @NotNull ResearchDomain domain,
            int basePoints,
            @NotNull String labLabel,
            @NotNull String planetLabel,
            @NotNull String experimentSummary
    ) {
        String expKey = (labLabel + ":" + experimentSummary).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9:_]+", "_");
        ScienceAwardResult result = recordScienceExperiment(
                level,
                expKey,
                experimentSummary,
                labLabel,
                planetLabel,
                domain,
                basePoints,
                Math.max(4, (int) Math.round(basePoints * 0.65)),
                1.0F,
                unlockedNodeId,
                null
        );
        return Math.max(result.spAwarded(), result.domainPointsAwarded());
    }

    /**
     * Records a completed interactive laboratory experiment using the hybrid KSP+ economy:
     * applies KSP diminishing returns per unique {@code experimentId@planetLabel}, scales by
     * player execution quality ({@code 0.6x..1.35x}) and CommNet relay bonus, logs into the
     * Science Archives, optionally awards a physical Milestone, and broadcasts to all players.
     */
    public static @NotNull ScienceAwardResult recordScienceExperiment(
            @NotNull ServerLevel level,
            @NotNull String experimentId,
            @NotNull String experimentTitle,
            @NotNull String labLabel,
            @NotNull String planetLabel,
            @NotNull ResearchDomain domain,
            int baseSp,
            int baseDomainPoints,
            float qualityMultiplier,
            @Nullable ResourceLocation autoUnlockNodeId,
            @Nullable String milestoneId
    ) {
        MinecraftServer server = level.getServer();
        TeamResearchState state = get(server);
        state.pruneStaleRelays(server.overworld().getGameTime());

        String archiveKey = experimentId + "@" + planetLabel.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        ExperimentRecord existing = state.getArchiveRecord(archiveKey);
        int priorRuns = existing == null ? 0 : existing.runCount();
        float diminishing = ExperimentRecord.diminishingFactor(priorRuns, state.isQuantumArchiveBoost());
        float clampedQuality = Math.max(0.5F, Math.min(1.5F, qualityMultiplier));
        double netMult = state.getNetworkBonusMultiplier();

        int maxSpPool = Math.max(baseSp, (int) Math.round(baseSp * 1.45 * netMult));
        int spAwarded = diminishing <= 0.0F ? 0 : Math.max(1, (int) Math.round(baseSp * diminishing * clampedQuality * netMult));
        int domainAwarded = diminishing <= 0.0F
                ? Math.max(1, (int) Math.round(baseDomainPoints * 0.10F))
                : Math.max(1, (int) Math.round(baseDomainPoints * Math.max(0.25F, diminishing) * clampedQuality * netMult));

        if (spAwarded > 0) {
            state.addSciencePoints(spAwarded);
        }
        state.addDomainPoints(domain, domainAwarded);

        ExperimentRecord updated = state.recordArchiveRun(
                archiveKey,
                experimentTitle,
                planetLabel,
                labLabel,
                domain,
                maxSpPool,
                spAwarded
        );

        boolean newMilestone = state.completeMilestone(milestoneId);
        boolean newlyUnlocked = autoUnlockNodeId != null && state.unlockNode(autoUnlockNodeId);

        String logLine = "[" + planetLabel + "] " + labLabel + ": " + experimentTitle
                + " (+" + spAwarded + " SP, +" + domainAwarded + " " + domain.shortCode() + ")";
        state.appendLog(logLine);

        broadcastToAll(server, state);

        if (newMilestone && milestoneId != null) {
            Component msg = Component.literal("[MILESTONE] ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(ResearchMilestone.label(milestoneId) + " (+" + spAwarded + " SP)")
                            .withStyle(ChatFormatting.AQUA));
            for (ServerPlayer player : PlayerLookup.all(server)) {
                player.displayClientMessage(msg, true);
            }
        } else if (newlyUnlocked && autoUnlockNodeId != null) {
            ResearchNode node = ResearchCatalog.get(autoUnlockNodeId);
            String title = node != null ? node.title() : autoUnlockNodeId.getPath();
            Component msg = Component.literal("[R&D] ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("Unlocked: " + title + " (+" + spAwarded + " SP)")
                            .withStyle(ChatFormatting.GREEN));
            for (ServerPlayer player : PlayerLookup.all(server)) {
                player.displayClientMessage(msg, true);
            }
        }

        return new ScienceAwardResult(
                spAwarded,
                domainAwarded,
                updated.runCount(),
                Math.round(diminishing * 100.0F),
                updated.remainingSp(),
                newMilestone
        );
    }

    /**
     * Records a celestial body mapped by an Observatory Telescope (legacy/automated helper).
     */
    public static int recordMappedCelestialBody(
            @NotNull ServerLevel level,
            @NotNull ResourceLocation bodyId,
            @NotNull String bodyDisplayName,
            @Nullable ResourceLocation ephemerisNodeId,
            int basePoints,
            @NotNull String observerPlanetLabel
    ) {
        String milestone = resolveAstroMilestone(bodyId.getPath(), "PLANET");
        ScienceAwardResult res = recordAstroDiscovery(
                level,
                "body:" + bodyId.getPath(),
                bodyDisplayName,
                "PLANET",
                bodyId,
                ephemerisNodeId,
                milestone,
                basePoints,
                Math.max(6, (int) Math.round(basePoints * 0.65)),
                -1.0F,
                92,
                observerPlanetLabel
        );
        return Math.max(res.spAwarded(), res.domainPointsAwarded());
    }

    /**
     * Records a real-sky photometric discovery captured through the 1st-person {@code OBSERVATORY_TELESCOPE}.
     */
    public static @NotNull ScienceAwardResult recordAstroDiscovery(
            @NotNull ServerLevel level,
            @NotNull String objectId,
            @NotNull String displayName,
            @NotNull String category,
            @Nullable ResourceLocation mappedBodyId,
            @Nullable ResourceLocation ephemerisNodeId,
            @Nullable String milestoneId,
            int baseSp,
            int baseEmag,
            float apparentMagnitude,
            int sharpnessPct,
            @NotNull String observerPlanetLabel
    ) {
        MinecraftServer server = level.getServer();
        TeamResearchState state = get(server);
        state.pruneStaleRelays(server.overworld().getGameTime());

        boolean firstDiscovery = state.recordAstroDiscovery(new AstroDiscovery(
                objectId,
                displayName,
                category,
                observerPlanetLabel,
                apparentMagnitude,
                Math.max(0, Math.min(100, sharpnessPct))
        ));
        if (mappedBodyId != null) {
            state.markBodyMapped(mappedBodyId);
        }

        float quality = 0.70F + (Math.max(0, Math.min(100, sharpnessPct)) / 100.0F) * 0.55F;
        if (firstDiscovery) {
            quality *= 1.15F;
        }
        String resolvedMilestone = milestoneId != null ? milestoneId : resolveAstroMilestone(objectId, category);

        ScienceAwardResult res = recordScienceExperiment(
                level,
                "astro_" + objectId,
                "Telescope Survey: " + displayName + " [" + category + "]",
                "Observatory Telescope",
                observerPlanetLabel,
                ResearchDomain.ELECTROMAGNETISM,
                baseSp,
                baseEmag,
                quality,
                ephemerisNodeId,
                resolvedMilestone
        );

        if (firstDiscovery) {
            Component msg = Component.literal("[OBSERVATORY] ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal("Cataloged " + displayName + " (" + sharpnessPct + "% focus) +" + res.spAwarded() + " SP!")
                            .withStyle(ChatFormatting.AQUA));
            for (ServerPlayer player : PlayerLookup.all(server)) {
                player.displayClientMessage(msg, true);
            }
        }
        return res;
    }

    private static @Nullable String resolveAstroMilestone(@NotNull String idOrPath, @NotNull String category) {
        String lower = idOrPath.toLowerCase(Locale.ROOT);
        if (lower.contains("moon")) {
            return ResearchMilestone.MOON_OBSERVED;
        }
        if (lower.contains("mars")) {
            return ResearchMilestone.MARS_OBSERVED;
        }
        if (lower.contains("jupiter") || lower.contains("saturn") || lower.contains("uranus") || lower.contains("neptune")) {
            return ResearchMilestone.OUTER_PLANET_OBSERVED;
        }
        if ("COMET".equalsIgnoreCase(category) || "NEBULA".equalsIgnoreCase(category)
                || "GALAXY".equalsIgnoreCase(category) || "CLUSTER".equalsIgnoreCase(category)) {
            return ResearchMilestone.DEEP_SKY_CATALOGED;
        }
        return null;
    }

    /**
     * Unlocks a KSP+ Tech Tree node inside {@code AEROSPACE_RD_TERMINAL} by verifying prerequisites,
     * primary Science Points ({@code SP}), secondary Domain points, and optional physical Milestone.
     */
    public static boolean tryUnlockNodeFromTerminal(
            @NotNull ServerLevel level,
            @Nullable ServerPlayer player,
            @NotNull ResourceLocation nodeId
    ) {
        ResearchNode node = ResearchCatalog.get(nodeId);
        if (node == null) {
            return false;
        }
        MinecraftServer server = level.getServer();
        TeamResearchState state = get(server);
        if (state.isUnlocked(nodeId)) {
            return false;
        }
        for (ResourceLocation prereq : node.prerequisites()) {
            if (!state.isUnlocked(prereq)) {
                return false;
            }
        }
        boolean creativeFree = player != null && player.isCreative();
        if (!creativeFree) {
            if (state.getSciencePoints() < node.spCost()) {
                return false;
            }
            if (state.getDomainPoints(node.domain()) < node.requiredDomainPoints()) {
                return false;
            }
            if (node.hasMilestoneRequirement() && !state.hasMilestone(node.requiredMilestone())) {
                return false;
            }
            state.addSciencePoints(-node.spCost());
            if (node.requiredDomainPoints() > 0) {
                state.addDomainPoints(node.domain(), -node.requiredDomainPoints());
            }
        }
        state.unlockNode(nodeId);
        state.appendLog("[R&D TERMINAL] Researched " + node.title()
                + " (-" + (creativeFree ? 0 : node.spCost()) + " SP"
                + (node.requiredDomainPoints() > 0 ? ", -" + (creativeFree ? 0 : node.requiredDomainPoints()) + " " + node.domain().shortCode() : "")
                + ")");
        broadcastToAll(server, state);
        return true;
    }

    public static void pulseRelay(
            @NotNull ServerLevel level,
            @NotNull String planetLabel,
            @NotNull BlockPos pos,
            int channel,
            int nearbyLabs
    ) {
        pulseRelay(level, planetLabel, pos, channel, nearbyLabs, false, false);
    }

    public static void pulseRelay(
            @NotNull ServerLevel level,
            @NotNull String planetLabel,
            @NotNull BlockPos pos,
            int channel,
            int nearbyLabs,
            boolean kaBandFeed,
            boolean laserTransponder
    ) {
        MinecraftServer server = level.getServer();
        TeamResearchState state = get(server);
        long now = server.overworld().getGameTime();
        int beforeCount = state.getActiveRelayCount();
        double beforeMult = state.getNetworkBonusMultiplier();
        state.pruneStaleRelays(now);
        state.pulseRelay(level.dimension().location(), planetLabel, pos, channel, nearbyLabs, now, kaBandFeed, laserTransponder);
        if (state.getActiveRelayCount() != beforeCount || Math.abs(state.getNetworkBonusMultiplier() - beforeMult) > 0.001) {
            broadcastToAll(server, state);
        }
    }

    public static void removeRelay(@NotNull ServerLevel level, @NotNull BlockPos pos) {
        MinecraftServer server = level.getServer();
        TeamResearchState state = get(server);
        state.removeRelay(level.dimension().location(), pos);
        broadcastToAll(server, state);
    }

    public static void syncToPlayer(@NotNull ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TeamResearchState state = get(server);
        state.pruneStaleRelays(server.overworld().getGameTime());
        applyToPlayerAccessor(player, state);
        ServerPlayNetworking.send(player, createPayload(state));
    }

    public static void broadcastToAll(@NotNull MinecraftServer server, @NotNull TeamResearchState state) {
        SyncResearchProgressPayload payload = createPayload(state);
        for (ServerPlayer player : PlayerLookup.all(server)) {
            applyToPlayerAccessor(player, state);
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void applyToPlayerAccessor(@NotNull ServerPlayer player, @NotNull TeamResearchState state) {
        if ((Object) player instanceof ServerResearchAccessor accessor) {
            accessor.galacticraft$applyResearchState(
                    state.getUnlockedNodes(),
                    state.getDomainPointsById(),
                    state.getMappedBodies()
            );
        }
    }

    public static @NotNull SyncResearchProgressPayload createPayload(@NotNull TeamResearchState state) {
        int[] points = new int[ResearchDomain.values().length];
        for (ResearchDomain domain : ResearchDomain.values()) {
            points[domain.ordinal()] = state.getDomainPoints(domain);
        }
        return new SyncResearchProgressPayload(
                state.getSciencePoints(),
                state.getTotalScienceEarned(),
                state.isQuantumArchiveBoost(),
                new ArrayList<>(state.getUnlockedNodes()),
                points,
                new ArrayList<>(state.getCompletedMilestones()),
                new ArrayList<>(state.getMappedBodies()),
                state.getScienceArchives(),
                state.getAstroAtlas(),
                state.getActiveRelayCount(),
                (int) Math.round(state.getNetworkBonusMultiplier() * 100.0),
                state.getActiveRelaySummaries(),
                state.getExperimentLog()
        );
    }
}
