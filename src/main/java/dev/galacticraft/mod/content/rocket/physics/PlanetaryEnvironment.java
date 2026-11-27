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

package dev.galacticraft.mod.content.rocket.physics;

import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.impl.universe.celestialbody.config.PlanetConfig;
import dev.galacticraft.impl.universe.position.config.OrbitalCelestialPositionConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCCelestialBodies;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Physical astrophysical parameters of a planetary body or dimension used by
 * {@link RocketFlightValidator} to compute emergent flight constraints:
 * surface gravity ({@code g}), ambient pressure ({@code atm}), day/night temperature ({@code K}),
 * heliocentric distance ({@code AU}) for {@code 1/r^2} solar irradiance, corrosive acid risk,
 * radiation flux, and transfer delta-V budgets.
 */
public record PlanetaryEnvironment(
        @NotNull ResourceLocation id,
        @NotNull String displayName,
        double gravityG,
        double pressureAtm,
        double dayTempKelvin,
        double nightTempKelvin,
        double orbitalDistanceAu,
        boolean corrosiveAcid,
        double radiationFlux,
        double orbitAscentDeltaV
) {
    public static final double KELVIN_OFFSET = 273.15;
    public static final double STANDARD_GRAVITY_MS2 = 9.80665;

    // Canonical Solar System presets matching GCCelestialBodies.java and real heliocentric AU distances
    public static final PlanetaryEnvironment EARTH = new PlanetaryEnvironment(
            GCCelestialBodies.EARTH.location(), "Terra", 1.0, 1.0, 294.15, 288.15, 1.000, false, 0.0, 2800.0);
    public static final PlanetaryEnvironment MOON = new PlanetaryEnvironment(
            GCCelestialBodies.MOON.location(), "Lua", 0.166, 0.0, 378.15, 93.15, 1.000, false, 0.25, 900.0);
    public static final PlanetaryEnvironment MARS = new PlanetaryEnvironment(
            GCCelestialBodies.MARS.location(), "Marte", 0.38, 0.006, 293.15, 173.15, 1.524, false, 0.25, 1700.0);
    public static final PlanetaryEnvironment ASTEROIDS = new PlanetaryEnvironment(
            GCCelestialBodies.ASTEROID.location(), "Asteroides", 0.0, 0.0, 271.15, 93.15, 2.770, false, 0.35, 350.0);
    public static final PlanetaryEnvironment MERCURY = new PlanetaryEnvironment(
            GCCelestialBodies.MERCURY.location(), "Mercúrio", 0.38, 0.0, 703.15, 93.15, 0.387, false, 0.70, 1800.0);
    public static final PlanetaryEnvironment VENUS = new PlanetaryEnvironment(
            GCCelestialBodies.VENUS.location(), "Vênus", 0.904, 92.0, 735.15, 735.15, 0.723, true, 0.15, 3600.0);
    public static final PlanetaryEnvironment JUPITER = new PlanetaryEnvironment(
            GCCelestialBodies.JUPITER.location(), "Júpiter", 2.36, 12.0, 168.15, 153.15, 5.203, false, 1.50, 5600.0);
    public static final PlanetaryEnvironment SATURN = new PlanetaryEnvironment(
            GCCelestialBodies.SATURN.location(), "Saturno", 0.916, 8.5, 138.15, 123.15, 9.582, false, 0.85, 4200.0);
    public static final PlanetaryEnvironment URANUS = new PlanetaryEnvironment(
            GCCelestialBodies.URANUS.location(), "Urano", 0.886, 5.0, 83.15, 49.15, 19.201, false, 0.55, 3900.0);
    public static final PlanetaryEnvironment NEPTUNE = new PlanetaryEnvironment(
            GCCelestialBodies.NEPTUNE.location(), "Netuno", 1.14, 7.5, 75.15, 58.15, 30.047, false, 0.50, 4400.0);
    public static final PlanetaryEnvironment PHOBOS = new PlanetaryEnvironment(
            GCCelestialBodies.PHOBOS.location(), "Fobos", 0.015, 0.0, 269.15, 161.15, 1.524, false, 0.30, 350.0);
    public static final PlanetaryEnvironment DEIMOS = new PlanetaryEnvironment(
            GCCelestialBodies.DEIMOS.location(), "Deimos", 0.012, 0.0, 253.15, 153.15, 1.524, false, 0.30, 350.0);
    public static final PlanetaryEnvironment IO = new PlanetaryEnvironment(
            GCCelestialBodies.IO.location(), "Io", 0.183, 1.0E-9, 143.15, 110.15, 5.203, false, 1.80, 1150.0);
    public static final PlanetaryEnvironment EUROPA = new PlanetaryEnvironment(
            GCCelestialBodies.EUROPA.location(), "Europa", 0.134, 1.0E-12, 113.15, 53.15, 5.203, false, 1.10, 950.0);
    public static final PlanetaryEnvironment GANYMEDE = new PlanetaryEnvironment(
            GCCelestialBodies.GANYMEDE.location(), "Ganimedes", 0.146, 1.0E-11, 160.15, 80.15, 5.203, false, 0.45, 1050.0);
    public static final PlanetaryEnvironment CALLISTO = new PlanetaryEnvironment(
            GCCelestialBodies.CALLISTO.location(), "Calisto", 0.126, 7.0E-12, 165.15, 80.15, 5.203, false, 0.15, 900.0);
    public static final PlanetaryEnvironment TITAN = new PlanetaryEnvironment(
            GCCelestialBodies.TITAN.location(), "Titã", 0.138, 1.45, 94.15, 93.15, 9.582, false, 0.12, 1650.0);
    public static final PlanetaryEnvironment ENCELADUS = new PlanetaryEnvironment(
            GCCelestialBodies.ENCELADUS.location(), "Encélado", 0.012, 0.0, 75.15, 72.15, 9.582, false, 0.38, 420.0);
    public static final PlanetaryEnvironment TITANIA = new PlanetaryEnvironment(
            GCCelestialBodies.TITANIA.location(), "Titânia", 0.039, 0.0, 89.15, 60.15, 19.201, false, 0.25, 650.0);
    public static final PlanetaryEnvironment OBERON = new PlanetaryEnvironment(
            GCCelestialBodies.OBERON.location(), "Oberon", 0.035, 0.0, 80.15, 70.15, 19.201, false, 0.22, 620.0);
    public static final PlanetaryEnvironment TRITON = new PlanetaryEnvironment(
            GCCelestialBodies.TRITON.location(), "Tritão", 0.079, 0.000014, 38.15, 35.15, 30.047, false, 0.28, 920.0);

    public static final List<PlanetaryEnvironment> SOLAR_SYSTEM_PRESETS = List.of(
            EARTH, MOON, MARS, PHOBOS, DEIMOS, VENUS, MERCURY, ASTEROIDS,
            JUPITER, IO, EUROPA, GANYMEDE, CALLISTO,
            SATURN, TITAN, ENCELADUS, URANUS, TITANIA, OBERON, NEPTUNE, TRITON
    );

    private static final Map<String, PlanetaryEnvironment> PRESETS_BY_PATH = Map.ofEntries(
            Map.entry("earth", EARTH),
            Map.entry("overworld", EARTH),
            Map.entry("moon", MOON),
            Map.entry("mars", MARS),
            Map.entry("phobos", PHOBOS),
            Map.entry("deimos", DEIMOS),
            Map.entry("asteroid", ASTEROIDS),
            Map.entry("asteroids", ASTEROIDS),
            Map.entry("mercury", MERCURY),
            Map.entry("venus", VENUS),
            Map.entry("jupiter", JUPITER),
            Map.entry("io", IO),
            Map.entry("europa", EUROPA),
            Map.entry("ganymede", GANYMEDE),
            Map.entry("callisto", CALLISTO),
            Map.entry("saturn", SATURN),
            Map.entry("titan", TITAN),
            Map.entry("enceladus", ENCELADUS),
            Map.entry("uranus", URANUS),
            Map.entry("titania", TITANIA),
            Map.entry("oberon", OBERON),
            Map.entry("neptune", NEPTUNE),
            Map.entry("triton", TRITON)
    );

    public PlanetaryEnvironment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        gravityG = Double.isFinite(gravityG) ? Math.max(0.0, gravityG) : 1.0;
        pressureAtm = Double.isFinite(pressureAtm) ? Math.max(0.0, pressureAtm) : 0.0;
        dayTempKelvin = Double.isFinite(dayTempKelvin) ? Math.max(2.7, dayTempKelvin) : 293.15;
        nightTempKelvin = Double.isFinite(nightTempKelvin) ? Math.max(2.7, nightTempKelvin) : 293.15;
        orbitalDistanceAu = Double.isFinite(orbitalDistanceAu) ? Math.max(0.15, orbitalDistanceAu) : 1.0;
        radiationFlux = Double.isFinite(radiationFlux) ? Math.max(0.0, radiationFlux) : 0.0;
        orbitAscentDeltaV = Double.isFinite(orbitAscentDeltaV) ? Math.max(100.0, orbitAscentDeltaV) : 2000.0;
    }

    /**
     * Peak operational ambient temperature (Kelvin) encountered on this body.
     */
    public double peakTempKelvin() {
        return Math.max(this.dayTempKelvin, this.nightTempKelvin);
    }

    /**
     * Mean surface temperature (Kelvin), useful for steady-state boil-off calculations.
     */
    public double meanTempKelvin() {
        return (this.dayTempKelvin + this.nightTempKelvin) * 0.5;
    }

    /**
     * Inverse-square solar irradiance factor {@code 1 / r^2} relative to Earth (1.0 AU = 1.0x).
     * Mercury ~ 6.67x, Venus ~ 1.91x, Earth/Moon = 1.0x, Mars ~ 0.43x,
     * Jupiter ~ 0.037x, Saturn ~ 0.011x, Uranus ~ 0.0027x, Neptune ~ 0.0011x.
     */
    public double solarIrradianceFactor() {
        double raw = 1.0 / (this.orbitalDistanceAu * this.orbitalDistanceAu);
        return Math.clamp(raw, 0.0005, 10.0);
    }

    /**
     * Additional aerodynamic stagnation heating (Kelvin) added during high-speed atmospheric entry.
     */
    public double atmosphericEntryHeatingKelvin() {
        if (this.pressureAtm <= 0.002) {
            return 0.0;
        }
        return Math.min(700.0, 220.0 + 185.0 * Math.log10(1.0 + this.pressureAtm * 10.0));
    }

    /**
     * Computes a realistic KSP-scaled delta-V budget (m/s) to launch from {@code origin}
     * and reach/enter {@code this} destination body.
     */
    public double requiredDeltaVFrom(@NotNull PlanetaryEnvironment origin) {
        Objects.requireNonNull(origin, "origin");
        if (origin.id().equals(this.id())) {
            return this.orbitAscentDeltaV;
        }
        double ascent = origin.orbitAscentDeltaV();
        double auRatio = Math.max(this.orbitalDistanceAu, origin.orbitalDistanceAu)
                / Math.max(0.2, Math.min(this.orbitalDistanceAu, origin.orbitalDistanceAu));
        // Logarithmic Hohmann-like transfer budget scaled for playable modular rockets
        double transfer;
        if (Math.abs(this.orbitalDistanceAu - origin.orbitalDistanceAu) < 0.05) {
            // Earth <-> Moon local system transfer
            transfer = 900.0;
        } else {
            transfer = 1400.0 + 2150.0 * Math.log(auRatio);
        }
        // Powered descent budget if destination has no thick atmosphere to aerobrake
        double captureAndLanding = this.pressureAtm >= 0.5
                ? 300.0
                : Math.min(1800.0, this.orbitAscentDeltaV * 0.45);
        return Math.round(ascent + transfer + captureAndLanding);
    }

    /**
     * Resolves the physical {@link PlanetaryEnvironment} for a Minecraft {@link Level}.
     */
    public static @NotNull PlanetaryEnvironment fromLevel(@Nullable Level level) {
        if (level == null) {
            return EARTH;
        }
        Holder<CelestialBody<?, ?>> holder = level.galacticraft$getCelestialBody();
        if (holder != null && holder.isBound()) {
            ResourceLocation id = holder.unwrapKey()
                    .map(k -> k.location())
                    .orElse(level.dimension().location());
            return fromCelestialBody(id, holder.value());
        }
        PlanetaryEnvironment byDim = byId(level.dimension().location());
        return byDim != null ? byDim : EARTH;
    }

    /**
     * Resolves a {@link PlanetaryEnvironment} directly from a {@link CelestialBody} definition,
     * reading real {@code gravity}, {@code pressure}, {@code dayTemperature}, {@code nightTemperature},
     * and {@link OrbitalCelestialPositionConfig} while anchoring canonical Solar System bodies to
     * true heliocentric AU distances.
     */
    public static @NotNull PlanetaryEnvironment fromCelestialBody(
            @Nullable ResourceLocation id,
            @Nullable CelestialBody<?, ?> body
    ) {
        if (body == null) {
            return id != null && byId(id) != null ? byId(id) : EARTH;
        }
        ResourceLocation resolvedId = id != null ? id : Constant.id("unknown");
        PlanetaryEnvironment preset = byId(resolvedId);

        double gravity = Math.max(0.0, body.gravity());
        double pressure = Math.max(0.0, body.atmosphere().pressure());
        double dayTempK;
        double nightTempK;
        if (body.config() instanceof PlanetConfig pc) {
            dayTempK = pc.dayTemperature() + KELVIN_OFFSET;
            nightTempK = pc.nightTemperature() + KELVIN_OFFSET;
        } else {
            double t = body.atmosphere().temperature() + KELVIN_OFFSET;
            dayTempK = t;
            nightTempK = t;
        }

        double au = resolveHeliocentricAu(resolvedId, body, preset);
        boolean acid = (preset != null && preset.corrosiveAcid())
                || (pressure >= 50.0 && Math.max(dayTempK, nightTempK) >= 650.0);
        double radiation = preset != null
                ? preset.radiationFlux()
                : (pressure < 0.05 ? 0.30 : (gravity > 1.8 ? 1.2 : 0.1));
        double ascentDv = preset != null
                ? preset.orbitAscentDeltaV()
                : Math.max(350.0, 2600.0 * gravity + (pressure > 5.0 ? 650.0 : 0.0));
        String name = preset != null ? preset.displayName() : body.name().getString();

        return new PlanetaryEnvironment(
                resolvedId,
                name,
                gravity,
                pressure,
                dayTempK,
                nightTempK,
                au,
                acid,
                radiation,
                ascentDv
        );
    }

    /**
     * Looks up a built-in Solar System environment preset by celestial body or dimension ID.
     */
    public static @Nullable PlanetaryEnvironment byId(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return PRESETS_BY_PATH.get(id.getPath().toLowerCase(Locale.ROOT));
    }

    private static double resolveHeliocentricAu(
            ResourceLocation id,
            CelestialBody<?, ?> body,
            @Nullable PlanetaryEnvironment preset
    ) {
        if (preset != null) {
            return preset.orbitalDistanceAu();
        }
        if (body.position().config() instanceof OrbitalCelestialPositionConfig orbital) {
            // Moons/satellites (planet == false) orbit their host planet at ~1.0 AU unless preset overrides
            if (!orbital.planet()) {
                return 1.0;
            }
            return Math.max(0.15, orbital.distance());
        }
        return 1.0;
    }
}
