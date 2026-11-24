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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Physical in-situ scientific milestones required as prerequisites for advanced KSP+ Tech Tree nodes.
 */
public final class ResearchMilestone {
    public static final String MOON_OBSERVED = "moon_observed";
    public static final String VACUUM_TRIAL_CERTIFIED = "vacuum_trial_certified";
    public static final String LUNAR_HE3_SPECTRUM = "lunar_he3_spectrum";
    public static final String MARS_OBSERVED = "mars_observed";
    public static final String COMMNET_RELAY_LOCKED = "commnet_relay_locked";
    public static final String ZERO_G_LEVITATION_CERTIFIED = "zero_g_levitation_certified";
    public static final String VENUS_92ATM_CERTIFIED = "venus_92atm_certified";
    public static final String OUTER_PLANET_OBSERVED = "outer_planet_observed";
    public static final String DEEP_SKY_CATALOGED = "deep_sky_cataloged";
    public static final String FLIGHT_TELEMETRY_ANALYZED = "flight_telemetry_analyzed";
    public static final String THERMAL_FAILURE_STUDIED = "thermal_failure_studied";
    public static final String ATMOSPHERIC_SPECTRUM_CAPTURED = "atmospheric_spectrum_captured";
    public static final String RADIATION_FLUX_MEASURED = "radiation_flux_measured";
    public static final String MAGNETOSPHERE_MAPPED = "magnetosphere_mapped";

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry(MOON_OBSERVED, "Observe Moon via Telescope"),
            Map.entry(VACUUM_TRIAL_CERTIFIED, "Certify Sample in Vacuum Chamber"),
            Map.entry(LUNAR_HE3_SPECTRUM, "Lock Lunar 3He Spectral Peak"),
            Map.entry(MARS_OBSERVED, "Observe Mars via Telescope"),
            Map.entry(COMMNET_RELAY_LOCKED, "Lock Interplanetary Data Link"),
            Map.entry(ZERO_G_LEVITATION_CERTIFIED, "Synthesize Crystal in 0.0G"),
            Map.entry(VENUS_92ATM_CERTIFIED, "Certify 92 atm Venus Sample"),
            Map.entry(OUTER_PLANET_OBSERVED, "Observe Outer Gas/Ice Giant"),
            Map.entry(DEEP_SKY_CATALOGED, "Photograph Comet/Nebula/Galaxy"),
            Map.entry(FLIGHT_TELEMETRY_ANALYZED, "Analyze Flight Telemetry Drive"),
            Map.entry(THERMAL_FAILURE_STUDIED, "Recover Flight Recorder Black Box (+200%)"),
            Map.entry(ATMOSPHERIC_SPECTRUM_CAPTURED, "Capture Planetary GasComposition via Probe"),
            Map.entry(RADIATION_FLUX_MEASURED, "Record Radiation Belt Flux"),
            Map.entry(MAGNETOSPHERE_MAPPED, "Map Planetary Magnetosphere via Boom")
    );

    private ResearchMilestone() {
    }

    public static @NotNull String label(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return "None";
        }
        return LABELS.getOrDefault(id, id);
    }
}
