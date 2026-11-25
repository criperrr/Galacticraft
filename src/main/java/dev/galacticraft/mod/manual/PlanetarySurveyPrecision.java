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

import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.research.AstroDiscovery;
import dev.galacticraft.mod.research.ClientResearchCache;
import dev.galacticraft.mod.research.ExperimentRecord;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchNode;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Progressive scientific precision model for the Planetary Atlas.
 *
 * <p>Unstudied bodies start with wide probable error bands (Tier 0 — Theoretical Estimate)
 * and progressively narrow toward exact 100% measurements (Tier 3 — Full In-Situ Confirmation)
 * as the player observes the planet with the {@code OBSERVATORY_TELESCOPE}, transmits
 * {@code science_*} probe telemetry via the {@code TELEMETRY_ANALYZER}, runs in-situ
 * {@code GEOLOGICAL_ANALYZER} / {@code ENVIRONMENTAL_TEST_CHAMBER} assays, and unlocks
 * planetary research nodes.
 */
public record PlanetarySurveyPrecision(
        int tier,
        int precisionPct,
        double errorFraction,
        int oreYUncertainty,
        boolean atmosphereConfirmed,
        boolean hazardsConfirmed,
        boolean oresIdentified,
        @NotNull String statusKey,
        @NotNull String nextMachineHintKey
) {
    public static final int TIER_THEORETICAL = 0;
    public static final int TIER_OPTICAL = 1;
    public static final int TIER_TELEMETRY = 2;
    public static final int TIER_VERIFIED = 3;

    public static @NotNull PlanetarySurveyPrecision evaluateLive(@NotNull ResourceLocation bodyId) {
        return evaluate(
                bodyId,
                ClientResearchCache.isBodyMapped(bodyId),
                ClientResearchCache.astroAtlas(),
                ClientResearchCache.scienceArchives(),
                ClientResearchCache.unlockedNodes()
        );
    }

    public static @NotNull PlanetarySurveyPrecision evaluate(
            @NotNull ResourceLocation bodyId,
            boolean mapped,
            @NotNull List<AstroDiscovery> astroAtlas,
            @NotNull List<ExperimentRecord> scienceArchives,
            @NotNull Set<ResourceLocation> unlockedNodes
    ) {
        if (GCCelestialBodies.EARTH.location().equals(bodyId) || "earth".equalsIgnoreCase(bodyId.getPath())) {
            return new PlanetarySurveyPrecision(
                    TIER_VERIFIED, 100, 0.0, 0, true, true, true,
                    "manual.galacticraft.precision.verified",
                    "manual.galacticraft.precision.hint.complete"
            );
        }

        String path = bodyId.getPath().toLowerCase(Locale.ROOT);
        int bestOpticalSharpness = 0;
        for (AstroDiscovery disc : astroAtlas) {
            String oid = disc.objectId().toLowerCase(Locale.ROOT);
            String dname = disc.displayName().toLowerCase(Locale.ROOT);
            if (oid.contains(path) || dname.contains(path)) {
                bestOpticalSharpness = Math.max(bestOpticalSharpness, disc.sharpnessPct());
            }
        }

        int labArchiveHits = 0;
        boolean hasGeoOrEnvScan = false;
        for (ExperimentRecord rec : scienceArchives) {
            String planet = rec.planetLabel().toLowerCase(Locale.ROOT);
            String key = rec.key().toLowerCase(Locale.ROOT);
            if (planet.contains(path) || key.contains(path)) {
                labArchiveHits++;
                String lab = rec.labLabel().toLowerCase(Locale.ROOT);
                if (lab.contains("geological") || lab.contains("environmental") || lab.contains("telemetry")) {
                    hasGeoOrEnvScan = true;
                }
            }
        }

        int unlockedPlanetNodes = 0;
        for (ResourceLocation nodeId : unlockedNodes) {
            ResearchNode node = ResearchCatalog.get(nodeId);
            if (node == null) {
                continue;
            }
            boolean matchesBody = node.locationRequirement().allowedBodies().contains(bodyId)
                    || node.locationRequirement().label().toLowerCase(Locale.ROOT).contains(path)
                    || nodeId.getPath().toLowerCase(Locale.ROOT).contains(path);
            if (matchesBody) {
                unlockedPlanetNodes++;
            }
        }

        boolean hasOptical = mapped || bestOpticalSharpness >= 35;
        int deepScore = labArchiveHits + unlockedPlanetNodes + (hasGeoOrEnvScan ? 1 : 0);

        // Tier 3: Optical/Mapped + 2+ deep planetary assays/research nodes (100% Exact)
        if (hasOptical && deepScore >= 2) {
            return new PlanetarySurveyPrecision(
                    TIER_VERIFIED, 100, 0.0, 0, true, true, true,
                    "manual.galacticraft.precision.verified",
                    "manual.galacticraft.precision.hint.complete"
            );
        }

        // Tier 2: Telemetry / Geological / Environmental assay or planetary node unlocked (80-92%)
        if (deepScore >= 1) {
            double err = hasOptical ? 0.06 : 0.09;
            int pct = hasOptical ? 88 : 80;
            return new PlanetarySurveyPrecision(
                    TIER_TELEMETRY, pct, err, 5, true, true, true,
                    "manual.galacticraft.precision.telemetry",
                    "manual.galacticraft.precision.hint.insitu"
            );
        }

        // Tier 1: Mapped via Observatory Telescope (45-70% depending on telescope sharpness)
        if (hasOptical) {
            double sharpnessBonus = Math.min(0.10, (bestOpticalSharpness / 100.0) * 0.10);
            double err = Math.max(0.14, 0.24 - sharpnessBonus);
            int pct = Math.min(72, 52 + bestOpticalSharpness / 5);
            return new PlanetarySurveyPrecision(
                    TIER_OPTICAL, pct, err, 14, true, false, true,
                    "manual.galacticraft.precision.optical",
                    "manual.galacticraft.precision.hint.probe_or_geo"
            );
        }

        // Tier 0: Unsurveyed theoretical range (20% — wide probable bounds)
        return new PlanetarySurveyPrecision(
                TIER_THEORETICAL, 22, 0.42, 26, false, false, false,
                "manual.galacticraft.precision.estimated",
                "manual.galacticraft.precision.hint.telescope"
        );
    }

    public boolean isExact() {
        return this.tier >= TIER_VERIFIED || this.errorFraction <= 0.0001;
    }

    public int badgeColor() {
        return switch (this.tier) {
            case TIER_VERIFIED -> 0xFF4ADE80;
            case TIER_TELEMETRY -> 0xFF38BDF8;
            case TIER_OPTICAL -> 0xFFFBBF24;
            default -> 0xFFF97316;
        };
    }

    public @NotNull String formatGravity(@NotNull PlanetaryEnvironment env) {
        double g = env.gravityG();
        if (isExact()) {
            return String.format(Locale.ROOT, "%.2fg", g);
        }
        if (g <= 0.005) {
            double hi = Math.max(0.02, 0.12 * this.errorFraction * 2.0);
            return String.format(Locale.ROOT, "0.00-%.2fg", hi);
        }
        double lo = Math.max(0.0, g * (1.0 - this.errorFraction));
        double hi = g * (1.0 + this.errorFraction);
        return String.format(Locale.ROOT, "%.2f-%.2fg", lo, hi);
    }

    public @NotNull String formatPressure(@NotNull PlanetaryEnvironment env) {
        double p = env.pressureAtm();
        if (isExact()) {
            return String.format(Locale.ROOT, "%.3f atm", p);
        }
        if (p <= 0.001) {
            return this.atmosphereConfirmed ? "<0.01 atm" : "0.0-0.08 atm";
        }
        double lo = Math.max(0.0, p * (1.0 - this.errorFraction));
        double hi = p * (1.0 + this.errorFraction);
        if (p >= 1.0) {
            return String.format(Locale.ROOT, "%.0f-%.0f atm", lo, hi);
        }
        return String.format(Locale.ROOT, "%.3f-%.3f atm", lo, hi);
    }

    public @NotNull String formatSolar(@NotNull PlanetaryEnvironment env) {
        double s = env.solarIrradianceFactor();
        if (isExact()) {
            return String.format(Locale.ROOT, "%.3fx", s);
        }
        double lo = Math.max(0.0005, s * (1.0 - this.errorFraction * 0.75));
        double hi = s * (1.0 + this.errorFraction * 0.75);
        return s >= 0.1
                ? String.format(Locale.ROOT, "%.2f-%.2fx", lo, hi)
                : String.format(Locale.ROOT, "%.4f-%.4fx", lo, hi);
    }

    public @NotNull String formatTemperatureC(@NotNull PlanetaryEnvironment env) {
        int dayC = (int) Math.round(env.dayTempKelvin() - PlanetaryEnvironment.KELVIN_OFFSET);
        int nightC = (int) Math.round(env.nightTempKelvin() - PlanetaryEnvironment.KELVIN_OFFSET);
        if (isExact()) {
            return String.format(Locale.ROOT, "%d°C / %d°C", dayC, nightC);
        }
        int deltaC = (int) Math.round(Math.max(12.0, 140.0 * this.errorFraction));
        int lo = Math.max(-270, Math.min(dayC, nightC) - deltaC);
        int hi = Math.max(dayC, nightC) + deltaC;
        return String.format(Locale.ROOT, "%d°C .. %d°C (±%d°)", lo, hi, deltaC);
    }

    public @NotNull String formatDeltaV(@NotNull PlanetaryEnvironment env) {
        double dv = env.orbitAscentDeltaV();
        if (isExact()) {
            return String.format(Locale.ROOT, "%.0f m/s", dv);
        }
        double lo = Math.max(100.0, Math.round((dv * (1.0 - this.errorFraction)) / 50.0) * 50.0);
        double hi = Math.round((dv * (1.0 + this.errorFraction)) / 50.0) * 50.0;
        return String.format(Locale.ROOT, "%.0f-%.0f m/s", lo, hi);
    }

    public @NotNull String formatOreRarity(double rarity) {
        if (isExact()) {
            return String.format(Locale.ROOT, "%.1fx", rarity);
        }
        double lo = Math.max(0.1, rarity * (1.0 - this.errorFraction));
        double hi = rarity * (1.0 + this.errorFraction);
        return String.format(Locale.ROOT, "%.1f-%.1fx", lo, hi);
    }
}
