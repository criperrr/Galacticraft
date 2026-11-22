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

package dev.galacticraft.mod.client.render.astro.planets;

import dev.galacticraft.mod.client.render.astro.AstroColorUtils;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroUnits;
import dev.galacticraft.mod.client.render.astro.planets.position.Orbit;
import dev.galacticraft.mod.client.render.astro.planets.position.SatelliteOrbit;
import org.joml.Vector3f;

import java.util.List;

public class ConfigurablePlanet {
    public String name;
    public ConfigurableOrbit orbit;
    public ConfigurableAppearance appearance;
    public List<ConfigurableMoon> moons;
    public String specialID = null;

    public ConfigurablePlanet() {
        this("", new ConfigurableOrbit(), new ConfigurableAppearance());
    }

    public ConfigurablePlanet(String name, ConfigurableOrbit orbit, ConfigurableAppearance appearance) {
        this.name = name;
        this.orbit = orbit;
        this.appearance = appearance;
        this.moons = List.of();
    }

    public ConfigurablePlanet setSpecialID(String specialID) {
        this.specialID = specialID;
        return this;
    }

    public ConfigurablePlanet addMoons(ConfigurableMoon... moons) {
        this.moons = List.of(moons);
        return this;
    }

    public Body genPlanet(String mount) {
        String id = genID(this, this.name, mount);
        Orbit orb = this.orbit.genOrbit();
        if (this.specialID != null) {
            this.orbit.modifyOrbit(orb, this.specialID);
        }

        Body planet = genPlanet(id, orb, this.appearance);
        if (planet instanceof Planet p && "earth".equals(this.specialID)) {
            p.scale(1.0, 1.0 / (double) AstroSkyEngine.CONFIG.diameterScale, 0.0);
        }

        planet.alias(this.name.replace(" ", "-").toLowerCase());
        return planet;
    }

    public static String genID(Object obj, String name, String mount) {
        String localID = name.isEmpty() ? "config-" + obj.hashCode() : name.toLowerCase().replace(' ', '-');
        return mount + "." + localID;
    }

    private static double estimateMagnitudeMainSequence(double diameter) {
        return 4.83 - 10.94 * Math.log10(diameter / 0.0093);
    }

    public static Body genPlanet(String id, Orbit orbit, ConfigurableAppearance appearance) {
        Vector3f color = AstroColorUtils.fromHex(appearance.color);
        Vector3f norm = AstroColorUtils.normalize(color);
        double albedo = AstroColorUtils.getRelativeAlbedo(color);
        double diameter = AstroUnits.convert(appearance.diameter, AstroUnits.Length.KM, AstroUnits.Length.AU);
        if ("star".equalsIgnoreCase(appearance.modelID)) {
            return new StarBody(id, orbit, estimateMagnitudeMainSequence(diameter), diameter, norm);
        }
        Planet planet = new Planet(id, orbit, appearance.modelID.toLowerCase(), diameter, albedo, norm);
        if (appearance.rings) {
            planet.addRings();
        }
        return planet;
    }

    public static class ConfigurableAppearance {
        public String modelID;
        public double diameter;
        public int color;
        public boolean rings = false;

        public ConfigurableAppearance(String modelID, double diameter, int color) {
            this.modelID = modelID;
            this.diameter = diameter;
            this.color = color;
        }

        public ConfigurableAppearance() {
            this("Terrestrial", 10000.0, 0xFFFFFF);
        }

        public ConfigurableAppearance addRings() {
            this.rings = true;
            return this;
        }
    }

    public static class ConfigurableMoon {
        public String name;
        public ConfigurableMoonOrbit orbit;
        public ConfigurableAppearance appearance;
        public String specialID = null;

        public ConfigurableMoon() {
            this("", new ConfigurableMoonOrbit(), new ConfigurableAppearance());
        }

        public ConfigurableMoon(String name, ConfigurableMoonOrbit orbit, ConfigurableAppearance appearance) {
            this.name = name;
            this.orbit = orbit;
            this.appearance = appearance;
        }

        public ConfigurableMoon setSpecialID(String specialID) {
            this.specialID = specialID;
            return this;
        }

        public Body genPlanet(String mount, ConfigurablePlanet primary) {
            String id = ConfigurablePlanet.genID(this, this.name, mount);
            SatelliteOrbit orb = this.orbit.genOrbit(primary.orbit);
            if (this.specialID != null) {
                this.orbit.modifyOrbit(orb, this.specialID);
            }
            Body planet = ConfigurablePlanet.genPlanet(id, orb, this.appearance);
            planet.alias(this.name.replace(" ", "-").toLowerCase());
            return planet;
        }
    }

    public static class ConfigurableMoonOrbit {
        public double perigeeRadius;
        public double apogeeRadius;
        public double synodicPeriod;
        public double inclination;
        public AdvancedMoonOrbitConfig advanced;

        public ConfigurableMoonOrbit(
                double perigee,
                double apogee,
                double synodicPeriod,
                double inclination,
                double longAscending,
                double longPeriapsis,
                double longMean,
                double nodalPP,
                double apsidalPP
        ) {
            this(perigee, apogee, synodicPeriod, inclination);
            this.advanced = new AdvancedMoonOrbitConfig(longAscending, longPeriapsis, longMean, nodalPP, apsidalPP);
        }

        public ConfigurableMoonOrbit(double perigee, double apogee, double synodicPeriod, double inclination) {
            this.perigeeRadius = perigee;
            this.apogeeRadius = apogee;
            this.synodicPeriod = synodicPeriod;
            this.inclination = inclination;
            this.advanced = new AdvancedMoonOrbitConfig();
        }

        public ConfigurableMoonOrbit() {
            this(500000.0, 500000.0, 100.0, 0.0);
        }

        public SatelliteOrbit genOrbit(ConfigurableOrbit primary) {
            if (this.apogeeRadius < this.perigeeRadius) {
                double minimum = this.perigeeRadius;
                this.perigeeRadius = this.apogeeRadius;
                this.apogeeRadius = minimum;
            }

            double sma = AstroUnits.convert((this.perigeeRadius + this.apogeeRadius) / 2.0, AstroUnits.Length.KM, AstroUnits.Length.AU);
            double ecc = (this.apogeeRadius - this.perigeeRadius) / (this.apogeeRadius + this.perigeeRadius);
            double synodic = AstroSkyEngine.CONFIG.planetTimeOptions.liveMode
                    ? this.synodicPeriod * 0.0027379079515
                    : this.synodicPeriod / AstroSkyEngine.CONFIG.planetTimeOptions.yearLength * AstroSkyEngine.CONFIG.planetTimeOptions.monthLength / 29.53059;
            double primaryPeriod = primary.getPeriod();
            double siderealPeriod = synodic * primaryPeriod / (primaryPeriod + (this.inclination <= 90.0 ? synodic : -synodic));
            SatelliteOrbit orbit = new SatelliteOrbit(sma, siderealPeriod, ecc, this.inclination);
            orbit.sync(0.0, this.advanced.longAscending, this.advanced.longPeriapsis, this.advanced.longMean);
            orbit.addPrecession(this.advanced.nodalPrecessionPeriod, this.advanced.apsidalPrecessionPeriod);
            return orbit;
        }

        public void modifyOrbit(SatelliteOrbit orbit, String specialID) {
            double midnight = AstroSkyEngine.getSeedTime() + 0.75 / AstroSkyEngine.CONFIG.planetTimeOptions.yearLength;
            if (!AstroSkyEngine.CONFIG.planetTimeOptions.liveMode && "moon".equals(specialID)) {
                orbit.sync(
                        midnight,
                        this.advanced.longAscending,
                        this.advanced.longPeriapsis,
                        (double) (AstroSkyEngine.CONFIG.planetTimeOptions.seasonOffset + AstroSkyEngine.CONFIG.planetTimeOptions.moonPhaseOffset)
                );
            }
        }

        public static class AdvancedMoonOrbitConfig {
            public double longAscending;
            public double longPeriapsis;
            public double longMean;
            public double nodalPrecessionPeriod;
            public double apsidalPrecessionPeriod;

            public AdvancedMoonOrbitConfig(double longAscending, double longPeriapsis, double longMean, double nodalPP, double apsidalPP) {
                this.longAscending = longAscending;
                this.longPeriapsis = longPeriapsis;
                this.longMean = longMean;
                this.nodalPrecessionPeriod = nodalPP;
                this.apsidalPrecessionPeriod = apsidalPP;
            }

            public AdvancedMoonOrbitConfig() {
                this(360.0 * Math.random(), 360.0 * Math.random(), 360.0 * Math.random(), 0.0, 0.0);
            }
        }
    }

    public static class ConfigurableOrbit {
        public double perihelionRadius;
        public double aphelionRadius;
        public double siderealPeriod;
        public double inclination;
        public AdvancedOrbitConfig advanced;

        public ConfigurableOrbit(
                double perihelion, double aphelion, double siderealPeriod, double inclination,
                double longAscending, double longPeriapsis, double longMean
        ) {
            this(perihelion, aphelion, siderealPeriod, inclination);
            this.advanced = new AdvancedOrbitConfig(longAscending, longPeriapsis, longMean);
        }

        public ConfigurableOrbit(double perihelion, double aphelion, double siderealPeriod, double inclination) {
            this.perihelionRadius = perihelion;
            this.aphelionRadius = aphelion;
            this.siderealPeriod = siderealPeriod;
            this.inclination = inclination;
            this.advanced = new AdvancedOrbitConfig();
        }

        public ConfigurableOrbit() {
            this(2.8, 2.8, 0.0, 0.0);
        }

        public double getPeriod() {
            double sma = (this.perihelionRadius + this.aphelionRadius) / 2.0;
            return this.siderealPeriod > 0.0 ? this.siderealPeriod : Math.sqrt(sma * sma * sma);
        }

        public Orbit genOrbit() {
            if (this.aphelionRadius < this.perihelionRadius) {
                double minimum = this.perihelionRadius;
                this.perihelionRadius = this.aphelionRadius;
                this.aphelionRadius = minimum;
            }
            double sma = (this.perihelionRadius + this.aphelionRadius) / 2.0;
            double ecc = (this.aphelionRadius - this.perihelionRadius) / (this.aphelionRadius + this.perihelionRadius);
            Orbit orbit = new Orbit(sma, this.getPeriod(), ecc, this.inclination);
            orbit.sync(0.0, this.advanced.longAscending, this.advanced.longPeriapsis, this.advanced.longMean);
            return orbit;
        }

        public void modifyOrbit(Orbit orbit, String specialID) {
            if (!AstroSkyEngine.CONFIG.planetTimeOptions.liveMode && "earth".equals(specialID)) {
                orbit.sync(
                        AstroSkyEngine.getSeedTime(),
                        this.advanced.longAscending,
                        this.advanced.longPeriapsis,
                        180.0 + (double) AstroSkyEngine.CONFIG.planetTimeOptions.seasonOffset
                );
            }
        }

        public static class AdvancedOrbitConfig {
            public double longAscending;
            public double longPeriapsis;
            public double longMean;

            public AdvancedOrbitConfig(double longAscending, double longPeriapsis, double longMean) {
                this.longAscending = longAscending;
                this.longPeriapsis = longPeriapsis;
                this.longMean = longMean;
            }

            public AdvancedOrbitConfig() {
                this(360.0 * Math.random(), 360.0 * Math.random(), 360.0 * Math.random());
            }
        }
    }
}
