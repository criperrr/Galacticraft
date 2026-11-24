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

package dev.galacticraft.mod.network.s2c;

import dev.galacticraft.impl.client.accessor.ClientResearchAccessor;
import dev.galacticraft.impl.network.s2c.S2CPayload;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.research.AstroDiscovery;
import dev.galacticraft.mod.research.ClientResearchCache;
import dev.galacticraft.mod.research.ExperimentRecord;
import dev.galacticraft.mod.research.ResearchDomain;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * S2C synchronization snapshot for the KSP+ planetary laboratory research economy:
 * primary Science Points ({@code SP}), 5 Scientific Domains, physical Milestones,
 * Science Archives, Astro Atlas discoveries, CommNet relays, and live experiment log.
 */
public record SyncResearchProgressPayload(
        int sciencePoints,
        int totalScienceEarned,
        boolean quantumArchiveBoost,
        @NotNull List<ResourceLocation> unlockedNodes,
        int @NotNull [] domainPoints,
        @NotNull List<String> completedMilestones,
        @NotNull List<ResourceLocation> mappedBodies,
        @NotNull List<ExperimentRecord> scienceArchives,
        @NotNull List<AstroDiscovery> astroAtlas,
        int activeRelayCount,
        int networkBonusPct,
        @NotNull List<String> relaySummaries,
        @NotNull List<String> experimentLog
) implements S2CPayload {
    public static final ResourceLocation ID = Constant.id("sync_research_progress");
    public static final Type<SyncResearchProgressPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, SyncResearchProgressPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SyncResearchProgressPayload decode(@NotNull ByteBuf buf) {
            int sp = ByteBufCodecs.VAR_INT.decode(buf);
            int totalSp = ByteBufCodecs.VAR_INT.decode(buf);
            boolean quantumBoost = ByteBufCodecs.BOOL.decode(buf);

            int nodeCount = ByteBufCodecs.VAR_INT.decode(buf);
            List<ResourceLocation> nodes = new ArrayList<>(Math.max(0, nodeCount));
            for (int i = 0; i < nodeCount; i++) {
                nodes.add(ResourceLocation.STREAM_CODEC.decode(buf));
            }
            int domainCount = ByteBufCodecs.VAR_INT.decode(buf);
            int[] points = new int[ResearchDomain.values().length];
            for (int i = 0; i < domainCount; i++) {
                int val = ByteBufCodecs.VAR_INT.decode(buf);
                if (i < points.length) {
                    points[i] = val;
                }
            }
            int milestoneCount = ByteBufCodecs.VAR_INT.decode(buf);
            List<String> milestones = new ArrayList<>(Math.max(0, milestoneCount));
            for (int i = 0; i < milestoneCount; i++) {
                milestones.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            int bodyCount = ByteBufCodecs.VAR_INT.decode(buf);
            List<ResourceLocation> bodies = new ArrayList<>(Math.max(0, bodyCount));
            for (int i = 0; i < bodyCount; i++) {
                bodies.add(ResourceLocation.STREAM_CODEC.decode(buf));
            }

            int archiveCount = ByteBufCodecs.VAR_INT.decode(buf);
            List<ExperimentRecord> archives = new ArrayList<>(Math.max(0, archiveCount));
            for (int i = 0; i < archiveCount; i++) {
                String key = ByteBufCodecs.STRING_UTF8.decode(buf);
                String title = ByteBufCodecs.STRING_UTF8.decode(buf);
                String planet = ByteBufCodecs.STRING_UTF8.decode(buf);
                String lab = ByteBufCodecs.STRING_UTF8.decode(buf);
                ResearchDomain dom = ResearchDomain.byId(ByteBufCodecs.STRING_UTF8.decode(buf));
                int maxSp = ByteBufCodecs.VAR_INT.decode(buf);
                int colSp = ByteBufCodecs.VAR_INT.decode(buf);
                int runs = ByteBufCodecs.VAR_INT.decode(buf);
                archives.add(new ExperimentRecord(key, title, planet, lab, dom, maxSp, colSp, runs));
            }

            int atlasCount = ByteBufCodecs.VAR_INT.decode(buf);
            List<AstroDiscovery> atlas = new ArrayList<>(Math.max(0, atlasCount));
            for (int i = 0; i < atlasCount; i++) {
                String id = ByteBufCodecs.STRING_UTF8.decode(buf);
                String name = ByteBufCodecs.STRING_UTF8.decode(buf);
                String cat = ByteBufCodecs.STRING_UTF8.decode(buf);
                String obs = ByteBufCodecs.STRING_UTF8.decode(buf);
                float mag = ByteBufCodecs.FLOAT.decode(buf);
                int sharp = ByteBufCodecs.VAR_INT.decode(buf);
                atlas.add(new AstroDiscovery(id, name, cat, obs, mag, sharp));
            }

            int relays = ByteBufCodecs.VAR_INT.decode(buf);
            int bonusPct = ByteBufCodecs.VAR_INT.decode(buf);

            int relaySummaryCount = ByteBufCodecs.VAR_INT.decode(buf);
            List<String> relaysInfo = new ArrayList<>(Math.max(0, relaySummaryCount));
            for (int i = 0; i < relaySummaryCount; i++) {
                relaysInfo.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }

            int logCount = ByteBufCodecs.VAR_INT.decode(buf);
            List<String> logs = new ArrayList<>(Math.max(0, logCount));
            for (int i = 0; i < logCount; i++) {
                logs.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            }

            return new SyncResearchProgressPayload(
                    sp, totalSp, quantumBoost, nodes, points, milestones, bodies,
                    archives, atlas, relays, bonusPct, relaysInfo, logs
            );
        }

        @Override
        public void encode(@NotNull ByteBuf buf, @NotNull SyncResearchProgressPayload payload) {
            ByteBufCodecs.VAR_INT.encode(buf, payload.sciencePoints);
            ByteBufCodecs.VAR_INT.encode(buf, payload.totalScienceEarned);
            ByteBufCodecs.BOOL.encode(buf, payload.quantumArchiveBoost);

            ByteBufCodecs.VAR_INT.encode(buf, payload.unlockedNodes.size());
            for (ResourceLocation id : payload.unlockedNodes) {
                ResourceLocation.STREAM_CODEC.encode(buf, id);
            }
            ByteBufCodecs.VAR_INT.encode(buf, payload.domainPoints.length);
            for (int val : payload.domainPoints) {
                ByteBufCodecs.VAR_INT.encode(buf, val);
            }
            ByteBufCodecs.VAR_INT.encode(buf, payload.completedMilestones.size());
            for (String m : payload.completedMilestones) {
                ByteBufCodecs.STRING_UTF8.encode(buf, m);
            }
            ByteBufCodecs.VAR_INT.encode(buf, payload.mappedBodies.size());
            for (ResourceLocation id : payload.mappedBodies) {
                ResourceLocation.STREAM_CODEC.encode(buf, id);
            }

            ByteBufCodecs.VAR_INT.encode(buf, payload.scienceArchives.size());
            for (ExperimentRecord rec : payload.scienceArchives) {
                ByteBufCodecs.STRING_UTF8.encode(buf, rec.key());
                ByteBufCodecs.STRING_UTF8.encode(buf, rec.title());
                ByteBufCodecs.STRING_UTF8.encode(buf, rec.planetLabel());
                ByteBufCodecs.STRING_UTF8.encode(buf, rec.labLabel());
                ByteBufCodecs.STRING_UTF8.encode(buf, rec.domain().id());
                ByteBufCodecs.VAR_INT.encode(buf, rec.maxSpPool());
                ByteBufCodecs.VAR_INT.encode(buf, rec.collectedSp());
                ByteBufCodecs.VAR_INT.encode(buf, rec.runCount());
            }

            ByteBufCodecs.VAR_INT.encode(buf, payload.astroAtlas.size());
            for (AstroDiscovery disc : payload.astroAtlas) {
                ByteBufCodecs.STRING_UTF8.encode(buf, disc.objectId());
                ByteBufCodecs.STRING_UTF8.encode(buf, disc.displayName());
                ByteBufCodecs.STRING_UTF8.encode(buf, disc.category());
                ByteBufCodecs.STRING_UTF8.encode(buf, disc.observerPlanet());
                ByteBufCodecs.FLOAT.encode(buf, disc.apparentMagnitude());
                ByteBufCodecs.VAR_INT.encode(buf, disc.sharpnessPct());
            }

            ByteBufCodecs.VAR_INT.encode(buf, payload.activeRelayCount);
            ByteBufCodecs.VAR_INT.encode(buf, payload.networkBonusPct);

            ByteBufCodecs.VAR_INT.encode(buf, payload.relaySummaries.size());
            for (String s : payload.relaySummaries) {
                ByteBufCodecs.STRING_UTF8.encode(buf, s);
            }
            ByteBufCodecs.VAR_INT.encode(buf, payload.experimentLog.size());
            for (String s : payload.experimentLog) {
                ByteBufCodecs.STRING_UTF8.encode(buf, s);
            }
        }
    };

    @Override
    public Runnable handle(ClientPlayNetworking.@NotNull Context context) {
        return () -> {
            List<ResourceLocation> newlyUnlocked = ClientResearchCache.applySnapshot(
                    this.sciencePoints,
                    this.totalScienceEarned,
                    this.quantumArchiveBoost,
                    this.unlockedNodes,
                    this.domainPoints,
                    this.completedMilestones,
                    this.mappedBodies,
                    this.scienceArchives,
                    this.astroAtlas,
                    this.activeRelayCount,
                    this.networkBonusPct,
                    this.relaySummaries,
                    this.experimentLog
            );
            if (context.player() != null && (Object) context.player() instanceof ClientResearchAccessor accessor) {
                accessor.galacticraft$applyResearchSnapshot(
                        new LinkedHashSet<>(this.unlockedNodes),
                        ClientResearchCache.domainPointsById(),
                        new LinkedHashSet<>(this.mappedBodies)
                );
            }
            if (!newlyUnlocked.isEmpty()) {
                dev.galacticraft.mod.manual.ManualClientBridge.onResearchNodesUnlocked(newlyUnlocked);
            }
        };
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
