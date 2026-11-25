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

package dev.galacticraft.mod.manual;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.accessor.ResearchAccessor;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.attachments.ManualReaderAttachment;
import dev.galacticraft.mod.research.ResearchCatalog;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Plan 12 — Aerospace Manual Codec validation, generated JSON integrity,
 * Tech Tree {@code required_research} referential integrity, {@link ResearchAccessor} lock/unlock
 * reflection, and {@link ManualReaderAttachment} persistence Codec.
 */
class AerospaceManualCodecAndDataTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void allBuiltInEntriesRoundTripCodecAndCoverAllTabsAndPageTypes() {
        List<ManualEntry> entries = ManualCatalog.builtInEntries();
        assertFalse(entries.isEmpty(), "ManualCatalog must not be empty");

        EnumSet<ManualTab> coveredTabs = EnumSet.noneOf(ManualTab.class);
        EnumSet<ManualPage.Type> coveredPageTypes = EnumSet.noneOf(ManualPage.Type.class);

        for (ManualEntry entry : entries) {
            coveredTabs.add(entry.tab());
            assertTrue(entry.era() >= 1 && entry.era() <= 6, "Entry era must be in 1..6: " + entry.id());
            assertFalse(entry.titleKey().isBlank(), "Missing title_key on " + entry.id());
            assertFalse(entry.lockHintKey().isBlank(), "Missing lock_hint_key on " + entry.id());
            assertFalse(entry.pages().isEmpty(), "Entry must have at least one page: " + entry.id());

            for (ManualPage page : entry.pages()) {
                coveredPageTypes.add(page.type());
            }

            DataResult<JsonElement> encoded = ManualEntry.CODEC.encodeStart(JsonOps.INSTANCE, entry);
            assertTrue(encoded.result().isPresent(), "Failed to encode " + entry.id() + ": " + encoded.error());

            DataResult<ManualEntry> decoded = ManualEntry.CODEC.parse(JsonOps.INSTANCE, encoded.result().get());
            assertTrue(decoded.result().isPresent(), "Failed to decode " + entry.id() + ": " + decoded.error());
            assertEquals(entry, decoded.result().get(), "Codec round-trip mismatch for " + entry.id());
        }

        assertEquals(EnumSet.allOf(ManualTab.class), coveredTabs, "All 6 ManualTabs must have entries");
        assertEquals(EnumSet.allOf(ManualPage.Type.class), coveredPageTypes, "All 8 polymorphic ManualPage types must be represented");
    }

    @Test
    void allRequiredResearchReferencesExistInResearchCatalog() {
        int gatedCount = 0;
        for (ManualEntry entry : ManualCatalog.builtInEntries()) {
            ResourceLocation req = entry.requiredResearchId();
            if (req != null) {
                gatedCount++;
                assertNotNull(ResearchCatalog.get(req),
                        "Manual entry " + entry.id() + " references unknown research node: " + req);
            }
        }
        assertTrue(gatedCount >= 10, "Expected progressive research-gated manual entries, found " + gatedCount);
    }

    @Test
    void lockedToUnlockedTransitionReflectsResearchAccessor() {
        ManualEntry sabatier = ManualCatalog.builtInEntries().stream()
                .filter(e -> e.id().equals(ManualCatalog.ENTRY_CHAIN_SABATIER))
                .findFirst()
                .orElseThrow();
        assertTrue(sabatier.requiresResearch());
        assertEquals(ResearchCatalog.MARTIAN_SABATIER_ISRU, sabatier.requiredResearchId());

        Set<ResourceLocation> unlocked = new HashSet<>();
        ResearchAccessor accessor = unlocked::contains;

        assertFalse(sabatier.isUnlocked(accessor), "Entry must be locked before research is unlocked");

        unlocked.add(ResearchCatalog.MARTIAN_SABATIER_ISRU);
        assertTrue(sabatier.isUnlocked(accessor), "Entry must immediately unlock once ResearchAccessor reports node unlocked");
    }

    @Test
    void generatedManualJsonFilesParseCleanlyThroughCodec() throws IOException {
        Path manualDir = Stream.of(
                        Path.of("src/main/generated/data/galacticraft/manual_entries"),
                        Path.of("../src/main/generated/data/galacticraft/manual_entries"),
                        Path.of("Galacticraft/src/main/generated/data/galacticraft/manual_entries")
                )
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(Path.of("../src/main/generated/data/galacticraft/manual_entries"));
        assertTrue(Files.isDirectory(manualDir), "Generated manual_entries directory must exist after runDatagen: " + manualDir.toAbsolutePath());

        List<Path> jsonFiles;
        try (Stream<Path> walk = Files.walk(manualDir)) {
            jsonFiles = walk.filter(p -> p.toString().endsWith(".json")).toList();
        }
        assertEquals(ManualCatalog.builtInEntries().size(), jsonFiles.size(),
                "Generated JSON file count must match ManualCatalog entries");

        for (Path file : jsonFiles) {
            String raw = Files.readString(file);
            JsonElement json = JsonParser.parseString(raw);
            DataResult<ManualEntry> parsed = ManualEntry.CODEC.parse(JsonOps.INSTANCE, json);
            assertTrue(parsed.result().isPresent(), "Generated JSON failed Codec parse (" + file + "): " + parsed.error());
            ManualEntry entry = parsed.result().get();
            if (entry.requiresResearch()) {
                assertNotNull(ResearchCatalog.get(entry.requiredResearchId()),
                        "Generated JSON " + file + " references invalid research node " + entry.requiredResearchId());
            }
        }
    }

    @Test
    void manualReaderAttachmentCodecPersistsReadFavoritesAndPinnedMission() {
        ManualReaderAttachment.PinnedMission pin = new ManualReaderAttachment.PinnedMission(
                ManualCatalog.ENTRY_LAUNCH_CHECKLIST_MOON,
                "Moon Mission",
                List.of("Oxygen Mask", "Parachute"),
                List.of(Constant.id("oxygen_mask"), Constant.id("parachute")),
                false
        );
        ManualReaderAttachment state = ManualReaderAttachment.EMPTY
                .withRead(ManualCatalog.ENTRY_MISSION_OVERVIEW)
                .withFavoriteToggled(ManualCatalog.ENTRY_CHAIN_NIOBIUM)
                .withPinnedMission(pin)
                .withPinCollapsedToggled();

        assertTrue(state.isRead(ManualCatalog.ENTRY_MISSION_OVERVIEW));
        assertTrue(state.isFavorite(ManualCatalog.ENTRY_CHAIN_NIOBIUM));
        assertTrue(state.pinnedMission().isPresent());
        assertTrue(state.pinnedMission().get().collapsed());

        DataResult<JsonElement> encoded = ManualReaderAttachment.CODEC.encodeStart(JsonOps.INSTANCE, state);
        assertTrue(encoded.result().isPresent(), "Failed to encode ManualReaderAttachment: " + encoded.error());

        DataResult<ManualReaderAttachment> decoded = ManualReaderAttachment.CODEC.parse(JsonOps.INSTANCE, encoded.result().get());
        assertTrue(decoded.result().isPresent(), "Failed to decode ManualReaderAttachment: " + decoded.error());
        assertEquals(state, decoded.result().get());
    }

    @Test
    void planetarySurveyPrecisionTightensProgressivelyWithResearchAndMachines() {
        ResourceLocation mars = Constant.id("mars");
        PlanetarySurveyPrecision tier0 = PlanetarySurveyPrecision.evaluate(mars, false, List.of(), List.of(), Set.of());
        assertEquals(PlanetarySurveyPrecision.TIER_THEORETICAL, tier0.tier());
        assertFalse(tier0.isExact());
        assertTrue(tier0.oreYUncertainty() > 15);
        assertTrue(tier0.formatGravity(dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment.MARS).contains("-"));

        PlanetarySurveyPrecision tier1 = PlanetarySurveyPrecision.evaluate(mars, true, List.of(), List.of(), Set.of());
        assertEquals(PlanetarySurveyPrecision.TIER_OPTICAL, tier1.tier());
        assertTrue(tier1.precisionPct() > tier0.precisionPct());
        assertTrue(tier1.errorFraction() < tier0.errorFraction());
        assertTrue(tier1.oreYUncertainty() < tier0.oreYUncertainty());

        PlanetarySurveyPrecision tier2 = PlanetarySurveyPrecision.evaluate(
                mars,
                true,
                List.of(),
                List.of(),
                Set.of(ResearchCatalog.MARTIAN_SABATIER_ISRU)
        );
        assertEquals(PlanetarySurveyPrecision.TIER_TELEMETRY, tier2.tier());
        assertTrue(tier2.precisionPct() > tier1.precisionPct());
        assertTrue(tier2.errorFraction() < tier1.errorFraction());

        PlanetarySurveyPrecision tier3 = PlanetarySurveyPrecision.evaluate(
                mars,
                true,
                List.of(),
                List.of(),
                Set.of(ResearchCatalog.MARTIAN_SABATIER_ISRU, ResearchCatalog.MARTIAN_DESH_ELASTOMERS)
        );
        assertEquals(PlanetarySurveyPrecision.TIER_VERIFIED, tier3.tier());
        assertTrue(tier3.isExact());
        assertEquals(100, tier3.precisionPct());
        assertEquals(0, tier3.oreYUncertainty());
        assertFalse(tier3.formatGravity(dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment.MARS).contains("-"));
    }
}
