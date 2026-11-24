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
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent agency/team-wide KSP+ scientific research state stored on Overworld {@link SavedData}.
 * Tracks primary Science Points ({@code SP}), secondary 5 Scientific Domain pools, unlocked nodes,
 * completed physical milestones, KSP-style Science Archives with diminishing returns, Telescope
 * Astro Atlas discoveries, active CommNet relays, and rolling telemetry logs.
 */
public class TeamResearchState extends SavedData {
    public static final String DATA_NAME = "galacticraft_research_state";
    public static final int MAX_LOG_ENTRIES = 16;
    public static final long RELAY_TIMEOUT_TICKS = 600L; // 30s without heartbeat

    private int sciencePoints = 0;
    private int totalScienceEarned = 0;
    private boolean quantumArchiveBoost = false;
    private final Set<ResourceLocation> unlockedNodes = new LinkedHashSet<>();
    private final EnumMap<ResearchDomain, Integer> domainPoints = new EnumMap<>(ResearchDomain.class);
    private final Set<String> completedMilestones = new LinkedHashSet<>();
    private final Set<ResourceLocation> mappedBodies = new LinkedHashSet<>();
    private final Map<String, ExperimentRecord> scienceArchives = new LinkedHashMap<>();
    private final Map<String, AstroDiscovery> astroAtlas = new LinkedHashMap<>();
    private final Map<String, RelayEntry> activeRelays = new HashMap<>();
    private final Deque<String> experimentLog = new ArrayDeque<>();

    public TeamResearchState() {
        for (ResearchDomain domain : ResearchDomain.values()) {
            this.domainPoints.put(domain, 0);
        }
        this.mappedBodies.add(GCCelestialBodies.EARTH.location());
    }

    public static Factory<TeamResearchState> factory() {
        return new Factory<>(TeamResearchState::new, TeamResearchState::load, DataFixTypes.LEVEL);
    }

    // --- Primary Science Points (SP) ---
    public int getSciencePoints() {
        return this.sciencePoints;
    }

    public int getTotalScienceEarned() {
        return this.totalScienceEarned;
    }

    public int addSciencePoints(int delta) {
        if (delta == 0) {
            return this.sciencePoints;
        }
        this.sciencePoints = Math.max(0, this.sciencePoints + delta);
        if (delta > 0) {
            this.totalScienceEarned += delta;
        }
        this.setDirty();
        return this.sciencePoints;
    }

    public boolean isQuantumArchiveBoost() {
        return this.quantumArchiveBoost;
    }

    public void setQuantumArchiveBoost(boolean active) {
        if (this.quantumArchiveBoost != active) {
            this.quantumArchiveBoost = active;
            this.setDirty();
        }
    }

    // --- Unlocked Tech Tree Nodes ---
    public boolean isUnlocked(@NotNull ResourceLocation nodeId) {
        return this.unlockedNodes.contains(nodeId);
    }

    public boolean unlockNode(@NotNull ResourceLocation nodeId) {
        boolean added = this.unlockedNodes.add(nodeId);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    public @NotNull Set<ResourceLocation> getUnlockedNodes() {
        return Collections.unmodifiableSet(this.unlockedNodes);
    }

    // --- Secondary 5 Scientific Domains ---
    public int getDomainPoints(@NotNull ResearchDomain domain) {
        return this.domainPoints.getOrDefault(domain, 0);
    }

    public int addDomainPoints(@NotNull ResearchDomain domain, int delta) {
        if (delta == 0) {
            return this.getDomainPoints(domain);
        }
        int next = Math.max(0, this.getDomainPoints(domain) + delta);
        this.domainPoints.put(domain, next);
        this.setDirty();
        return next;
    }

    public @NotNull Map<ResearchDomain, Integer> getAllDomainPoints() {
        return Collections.unmodifiableMap(this.domainPoints);
    }

    public @NotNull Map<String, Integer> getDomainPointsById() {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<ResearchDomain, Integer> entry : this.domainPoints.entrySet()) {
            map.put(entry.getKey().id(), entry.getValue());
        }
        return map;
    }

    // --- Physical Milestones ---
    public boolean hasMilestone(@Nullable String milestoneId) {
        return milestoneId == null || milestoneId.isEmpty() || this.completedMilestones.contains(milestoneId);
    }

    public boolean completeMilestone(@Nullable String milestoneId) {
        if (milestoneId == null || milestoneId.isEmpty()) {
            return false;
        }
        boolean added = this.completedMilestones.add(milestoneId);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    public @NotNull Set<String> getCompletedMilestones() {
        return Collections.unmodifiableSet(this.completedMilestones);
    }

    // --- KSP Science Archives (Diminishing Returns) ---
    public @Nullable ExperimentRecord getArchiveRecord(@NotNull String key) {
        return this.scienceArchives.get(key);
    }

    public @NotNull ExperimentRecord recordArchiveRun(
            @NotNull String key,
            @NotNull String title,
            @NotNull String planetLabel,
            @NotNull String labLabel,
            @NotNull ResearchDomain domain,
            int maxSpPool,
            int spAwarded
    ) {
        ExperimentRecord prev = this.scienceArchives.get(key);
        ExperimentRecord updated;
        if (prev == null) {
            updated = new ExperimentRecord(key, title, planetLabel, labLabel, domain, Math.max(maxSpPool, spAwarded), spAwarded, 1);
        } else {
            int newPool = Math.max(prev.maxSpPool(), maxSpPool);
            int newCollected = Math.min(newPool, prev.collectedSp() + spAwarded);
            updated = new ExperimentRecord(key, title, planetLabel, labLabel, domain, newPool, newCollected, prev.runCount() + 1);
        }
        this.scienceArchives.put(key, updated);
        this.setDirty();
        return updated;
    }

    public @NotNull List<ExperimentRecord> getScienceArchives() {
        return new ArrayList<>(this.scienceArchives.values());
    }

    // --- Telescope Astro Atlas ---
    public boolean hasAstroDiscovery(@NotNull String objectId) {
        return this.astroAtlas.containsKey(objectId);
    }

    public boolean recordAstroDiscovery(@NotNull AstroDiscovery discovery) {
        AstroDiscovery prev = this.astroAtlas.get(discovery.objectId());
        if (prev == null || discovery.sharpnessPct() > prev.sharpnessPct()) {
            this.astroAtlas.put(discovery.objectId(), discovery);
            this.setDirty();
            return prev == null;
        }
        return false;
    }

    public @NotNull List<AstroDiscovery> getAstroAtlas() {
        return new ArrayList<>(this.astroAtlas.values());
    }

    // --- Mapped Solar System Bodies ---
    public boolean isBodyMapped(@NotNull ResourceLocation bodyId) {
        return this.mappedBodies.contains(bodyId);
    }

    public boolean markBodyMapped(@NotNull ResourceLocation bodyId) {
        boolean added = this.mappedBodies.add(bodyId);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    public @NotNull Set<ResourceLocation> getMappedBodies() {
        return Collections.unmodifiableSet(this.mappedBodies);
    }

    // --- CommNet Relays ---
    public void pulseRelay(
            @NotNull ResourceLocation dimensionId,
            @NotNull String planetLabel,
            @NotNull BlockPos pos,
            int channel,
            int nearbyLabs,
            long gameTime
    ) {
        this.pulseRelay(dimensionId, planetLabel, pos, channel, nearbyLabs, gameTime, false, false);
    }

    public void pulseRelay(
            @NotNull ResourceLocation dimensionId,
            @NotNull String planetLabel,
            @NotNull BlockPos pos,
            int channel,
            int nearbyLabs,
            long gameTime,
            boolean kaBandFeed,
            boolean laserTransponder
    ) {
        String key = relayKey(dimensionId, pos);
        RelayEntry prev = this.activeRelays.get(key);
        RelayEntry updated = new RelayEntry(dimensionId, planetLabel, pos.asLong(), channel, nearbyLabs, gameTime, kaBandFeed, laserTransponder);
        this.activeRelays.put(key, updated);
        if (prev == null || prev.channel() != channel || prev.nearbyLabs() != nearbyLabs
                || prev.kaBandFeed() != kaBandFeed || prev.laserTransponder() != laserTransponder) {
            this.setDirty();
        }
    }

    public void removeRelay(@NotNull ResourceLocation dimensionId, @NotNull BlockPos pos) {
        if (this.activeRelays.remove(relayKey(dimensionId, pos)) != null) {
            this.setDirty();
        }
    }

    public void pruneStaleRelays(long currentGameTime) {
        boolean removed = this.activeRelays.values().removeIf(
                entry -> currentGameTime - entry.lastHeartbeatTick() > RELAY_TIMEOUT_TICKS
        );
        if (removed) {
            this.setDirty();
        }
    }

    public int getActiveRelayCount() {
        return this.activeRelays.size();
    }

    public int getDistinctRelayDimensionCount() {
        Set<ResourceLocation> dims = new LinkedHashSet<>();
        for (RelayEntry entry : this.activeRelays.values()) {
            dims.add(entry.dimensionId());
        }
        return dims.size();
    }

    public boolean hasKaBandNetwork() {
        for (RelayEntry entry : this.activeRelays.values()) {
            if (entry.kaBandFeed()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasLaserCommNetwork() {
        for (RelayEntry entry : this.activeRelays.values()) {
            if (entry.laserTransponder()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Interplanetary CommNet multiplier: +20% per distinct connected planetary relay (up to 2.0x),
     * upgraded to +25% per planet (up to 2.5x) when any relay has a Deep-Space Ka-Band Feed.
     */
    public double getNetworkBonusMultiplier() {
        int distinct = this.getDistinctRelayDimensionCount();
        boolean ka = this.hasKaBandNetwork();
        double step = ka ? 0.25 : 0.20;
        double maxBonus = ka ? 1.50 : 1.00;
        double laserExtra = this.hasLaserCommNetwork() ? 0.15 : 0.0;
        return 1.0 + Math.min(maxBonus, distinct * step) + laserExtra;
    }

    public @NotNull List<String> getActiveRelaySummaries() {
        List<String> list = new ArrayList<>(this.activeRelays.size());
        for (RelayEntry entry : this.activeRelays.values()) {
            String tags = (entry.kaBandFeed() ? " Ka" : "") + (entry.laserTransponder() ? " LASER" : "");
            list.add(entry.planetLabel() + " [CH-" + String.format("%02d", entry.channel()) + " | " + entry.nearbyLabs() + " LABS" + tags + "]");
        }
        Collections.sort(list);
        return list;
    }

    public void appendLog(@NotNull String entry) {
        this.experimentLog.addFirst(entry);
        while (this.experimentLog.size() > MAX_LOG_ENTRIES) {
            this.experimentLog.removeLast();
        }
        this.setDirty();
    }

    public @NotNull List<String> getExperimentLog() {
        return List.copyOf(this.experimentLog);
    }

    public void resetAll() {
        this.sciencePoints = 0;
        this.totalScienceEarned = 0;
        this.quantumArchiveBoost = false;
        this.unlockedNodes.clear();
        for (ResearchDomain domain : ResearchDomain.values()) {
            this.domainPoints.put(domain, 0);
        }
        this.completedMilestones.clear();
        this.mappedBodies.clear();
        this.mappedBodies.add(GCCelestialBodies.EARTH.location());
        this.scienceArchives.clear();
        this.astroAtlas.clear();
        this.activeRelays.clear();
        this.experimentLog.clear();
        this.setDirty();
    }

    private static String relayKey(ResourceLocation dimId, BlockPos pos) {
        return dimId.toString() + "@" + pos.asLong();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putInt("science_points", this.sciencePoints);
        tag.putInt("total_science_earned", this.totalScienceEarned);
        tag.putBoolean("quantum_archive_boost", this.quantumArchiveBoost);

        ListTag nodesTag = new ListTag();
        for (ResourceLocation id : this.unlockedNodes) {
            nodesTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("unlocked_nodes", nodesTag);

        CompoundTag domainsTag = new CompoundTag();
        for (Map.Entry<ResearchDomain, Integer> entry : this.domainPoints.entrySet()) {
            domainsTag.putInt(entry.getKey().id(), entry.getValue());
        }
        tag.put("domain_points", domainsTag);

        ListTag milestonesTag = new ListTag();
        for (String m : this.completedMilestones) {
            milestonesTag.add(StringTag.valueOf(m));
        }
        tag.put("completed_milestones", milestonesTag);

        ListTag mappedTag = new ListTag();
        for (ResourceLocation id : this.mappedBodies) {
            mappedTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("mapped_bodies", mappedTag);

        ListTag archivesTag = new ListTag();
        for (ExperimentRecord rec : this.scienceArchives.values()) {
            CompoundTag aTag = new CompoundTag();
            aTag.putString("key", rec.key());
            aTag.putString("title", rec.title());
            aTag.putString("planet", rec.planetLabel());
            aTag.putString("lab", rec.labLabel());
            aTag.putString("domain", rec.domain().id());
            aTag.putInt("max_sp", rec.maxSpPool());
            aTag.putInt("collected_sp", rec.collectedSp());
            aTag.putInt("runs", rec.runCount());
            archivesTag.add(aTag);
        }
        tag.put("science_archives", archivesTag);

        ListTag atlasTag = new ListTag();
        for (AstroDiscovery disc : this.astroAtlas.values()) {
            CompoundTag dTag = new CompoundTag();
            dTag.putString("id", disc.objectId());
            dTag.putString("name", disc.displayName());
            dTag.putString("cat", disc.category());
            dTag.putString("obs", disc.observerPlanet());
            dTag.putFloat("mag", disc.apparentMagnitude());
            dTag.putInt("sharp", disc.sharpnessPct());
            atlasTag.add(dTag);
        }
        tag.put("astro_atlas", atlasTag);

        ListTag relaysTag = new ListTag();
        for (RelayEntry relay : this.activeRelays.values()) {
            CompoundTag rTag = new CompoundTag();
            rTag.putString("dim", relay.dimensionId().toString());
            rTag.putString("label", relay.planetLabel());
            rTag.putLong("pos", relay.posLong());
            rTag.putInt("channel", relay.channel());
            rTag.putInt("labs", relay.nearbyLabs());
            rTag.putLong("tick", relay.lastHeartbeatTick());
            rTag.putBoolean("ka", relay.kaBandFeed());
            rTag.putBoolean("laser", relay.laserTransponder());
            relaysTag.add(rTag);
        }
        tag.put("active_relays", relaysTag);

        ListTag logTag = new ListTag();
        for (String line : this.experimentLog) {
            logTag.add(StringTag.valueOf(line));
        }
        tag.put("experiment_log", logTag);
        return tag;
    }

    public static TeamResearchState load(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        TeamResearchState state = new TeamResearchState();
        state.sciencePoints = Math.max(0, tag.getInt("science_points"));
        state.totalScienceEarned = Math.max(state.sciencePoints, tag.getInt("total_science_earned"));
        state.quantumArchiveBoost = tag.getBoolean("quantum_archive_boost");

        ListTag nodesTag = tag.getList("unlocked_nodes", Tag.TAG_STRING);
        for (int i = 0; i < nodesTag.size(); i++) {
            ResourceLocation parsed = ResourceLocation.tryParse(nodesTag.getString(i));
            if (parsed != null) {
                state.unlockedNodes.add(parsed);
            }
        }

        if (tag.contains("domain_points", Tag.TAG_COMPOUND)) {
            CompoundTag domainsTag = tag.getCompound("domain_points");
            for (ResearchDomain domain : ResearchDomain.values()) {
                state.domainPoints.put(domain, Math.max(0, domainsTag.getInt(domain.id())));
            }
        }

        ListTag milestonesTag = tag.getList("completed_milestones", Tag.TAG_STRING);
        for (int i = 0; i < milestonesTag.size(); i++) {
            String m = milestonesTag.getString(i);
            if (!m.isEmpty()) {
                state.completedMilestones.add(m);
            }
        }

        ListTag mappedTag = tag.getList("mapped_bodies", Tag.TAG_STRING);
        for (int i = 0; i < mappedTag.size(); i++) {
            ResourceLocation parsed = ResourceLocation.tryParse(mappedTag.getString(i));
            if (parsed != null) {
                state.mappedBodies.add(parsed);
            }
        }
        state.mappedBodies.add(GCCelestialBodies.EARTH.location());

        ListTag archivesTag = tag.getList("science_archives", Tag.TAG_COMPOUND);
        for (int i = 0; i < archivesTag.size(); i++) {
            CompoundTag aTag = archivesTag.getCompound(i);
            String key = aTag.getString("key");
            if (!key.isEmpty()) {
                state.scienceArchives.put(key, new ExperimentRecord(
                        key,
                        aTag.getString("title"),
                        aTag.getString("planet"),
                        aTag.getString("lab"),
                        ResearchDomain.byId(aTag.getString("domain")),
                        Math.max(1, aTag.getInt("max_sp")),
                        Math.max(0, aTag.getInt("collected_sp")),
                        Math.max(1, aTag.getInt("runs"))
                ));
            }
        }

        ListTag atlasTag = tag.getList("astro_atlas", Tag.TAG_COMPOUND);
        for (int i = 0; i < atlasTag.size(); i++) {
            CompoundTag dTag = atlasTag.getCompound(i);
            String id = dTag.getString("id");
            if (!id.isEmpty()) {
                state.astroAtlas.put(id, new AstroDiscovery(
                        id,
                        dTag.getString("name"),
                        dTag.getString("cat"),
                        dTag.getString("obs"),
                        dTag.getFloat("mag"),
                        Math.max(0, Math.min(100, dTag.getInt("sharp")))
                ));
            }
        }

        ListTag relaysTag = tag.getList("active_relays", Tag.TAG_COMPOUND);
        for (int i = 0; i < relaysTag.size(); i++) {
            CompoundTag rTag = relaysTag.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(rTag.getString("dim"));
            if (dim != null) {
                long posLong = rTag.getLong("pos");
                BlockPos pos = BlockPos.of(posLong);
                state.activeRelays.put(
                        relayKey(dim, pos),
                        new RelayEntry(
                                dim,
                                rTag.getString("label"),
                                posLong,
                                Math.max(1, rTag.getInt("channel")),
                                Math.max(0, rTag.getInt("labs")),
                                rTag.getLong("tick"),
                                rTag.getBoolean("ka"),
                                rTag.getBoolean("laser")
                        )
                );
            }
        }

        ListTag logTag = tag.getList("experiment_log", Tag.TAG_STRING);
        for (int i = 0; i < logTag.size() && i < MAX_LOG_ENTRIES; i++) {
            state.experimentLog.addLast(logTag.getString(i));
        }

        return state;
    }

    public record RelayEntry(
            @NotNull ResourceLocation dimensionId,
            @NotNull String planetLabel,
            long posLong,
            int channel,
            int nearbyLabs,
            long lastHeartbeatTick,
            boolean kaBandFeed,
            boolean laserTransponder
    ) {
    }
}
