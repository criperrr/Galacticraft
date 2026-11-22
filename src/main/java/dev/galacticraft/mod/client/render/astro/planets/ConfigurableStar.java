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
import dev.galacticraft.mod.client.render.astro.AstroUnits;
import dev.galacticraft.mod.client.render.astro.planets.position.Equatorial;
import dev.galacticraft.mod.client.render.astro.planets.position.Positioner;
import org.joml.Vector3f;

import java.util.List;

public class ConfigurableStar {
    public String name;
    public ConfigurablePosition position;
    public ConfigurableAppearance appearance;
    public List<ConfigurableCompanion> companions;
    public List<ConfigurablePlanet> planets;

    public ConfigurableStar() {
        this("", new ConfigurablePosition(), new ConfigurableAppearance());
    }

    public ConfigurableStar(String name, ConfigurablePosition position, ConfigurableAppearance appearance) {
        this.name = name;
        this.position = position;
        this.appearance = appearance;
        this.companions = List.of();
        this.planets = List.of();
    }

    public Body genStar(String mount) {
        String id = genID(this, this.name, mount);
        Positioner pos = this.position.genPositioner();
        Body star = genStar(id, pos, this.appearance);
        star.alias(this.name.replace(" ", "-").toLowerCase());
        return star;
    }

    public static String genID(Object obj, String name, String mount) {
        String localID = name.isEmpty() ? "config-" + obj.hashCode() : name.toLowerCase().replace(' ', '-');
        return mount + "." + localID;
    }

    public static Body genStar(String id, Positioner position, ConfigurableAppearance appearance) {
        Vector3f norm = AstroColorUtils.normalize(appearance.getColor());
        double diameter = appearance.estimateDiameter();
        return new StarBody(id, position, appearance.magnitude - 0.9, AstroUnits.convert(diameter, AstroUnits.Length.KM, AstroUnits.Length.AU), norm);
    }

    public static class ConfigurableAppearance {
        public double magnitude;
        public String color;

        public ConfigurableAppearance(String color, double magnitude) {
            this.color = color;
            this.magnitude = magnitude;
        }

        public ConfigurableAppearance() {
            this("6000K", 0.0);
        }

        private double getSpectralTypeIndex(char chr) {
            return switch (chr) {
                case 'a' -> 20.0;
                case 'b' -> 30.0;
                case 'c' -> -24.0;
                case 'f' -> 10.0;
                case 'k' -> -10.0;
                case 'l' -> -30.0;
                case 'm' -> -20.0;
                case 'o', 'w' -> 40.0;
                case 't' -> -40.0;
                default -> 0.0;
            };
        }

        public double getSpectralTypeTemperature(char chr, double num) {
            double index = this.getSpectralTypeIndex(chr) - num;
            if (index >= 40.0) {
                return 1.0E9;
            }
            return index < 0.0 ? 6050.0 + 90.0 * index - index * index : -70.0 * index + 235950.0 / (39.0 - index);
        }

        public double getColorTemperature() {
            String str = this.color.strip().toLowerCase();
            if (str.length() > 1 && Character.isAlphabetic(str.charAt(0)) && Character.isDigit(str.charAt(1))) {
                return this.getSpectralTypeTemperature(str.charAt(0), (double) Integer.parseInt(str.substring(1)));
            }
            if (str.startsWith("#")) {
                return 5772.0;
            }
            if (str.endsWith("k")) {
                return Double.parseDouble(str.substring(0, str.length() - 1));
            }
            try {
                double value = Double.parseDouble(str);
                return value < 100.0 ? AstroColorUtils.temperatureFromIndex(value) : value;
            } catch (Exception e) {
                return 5772.0;
            }
        }

        public Vector3f getColor() {
            try {
                String str = this.color.strip().toLowerCase();
                return str.startsWith("#")
                        ? AstroColorUtils.fromHex(Integer.parseInt(str.substring(1), 16))
                        : AstroColorUtils.fromTemperature(this.getColorTemperature());
            } catch (Exception e) {
                return new Vector3f(1.0F, 0.0F, 1.0F);
            }
        }

        public double estimateDiameter() {
            double tempK = this.getColorTemperature();
            if (this.magnitude >= 6.0 && tempK < 4000.0) {
                return 755507.2923201618 * Math.pow(10.0, -0.03504486729 * this.magnitude);
            }
            double lum = Math.pow(2.512, 4.83 - this.magnitude);
            return 1391400.0 * Math.sqrt(lum / Math.pow(tempK / 5772.0, 4.0));
        }
    }

    public static class ConfigurableCompanion {
        public String name;
        public ConfigurableAppearance appearance;
        public ConfigurablePlanet.ConfigurableOrbit orbit;
        public List<ConfigurableCompanion> companions;
        public List<ConfigurablePlanet> planets;

        public ConfigurableCompanion() {
            this("", new ConfigurablePlanet.ConfigurableOrbit(), new ConfigurableAppearance());
        }

        public ConfigurableCompanion(String name, ConfigurablePlanet.ConfigurableOrbit orbit, ConfigurableAppearance appearance) {
            this.name = name;
            this.orbit = orbit;
            this.appearance = appearance;
            this.companions = List.of();
            this.planets = List.of();
        }

        public Body genStar(String mount) {
            String id = ConfigurableStar.genID(this, this.name, mount);
            Positioner pos = this.orbit.genOrbit();
            Body star = ConfigurableStar.genStar(id, pos, this.appearance);
            star.alias(this.name.replace(" ", "-").toLowerCase());
            return star;
        }
    }

    public static class ConfigurablePosition {
        public String rightAscension;
        public String declination;
        public double distance;

        public ConfigurablePosition(double rightAscension, double declination, double distance) {
            this.rightAscension = String.valueOf(rightAscension);
            this.declination = String.valueOf(declination);
            this.distance = distance;
        }

        public ConfigurablePosition() {
            this(0.0, 0.0, 0.0);
        }

        public Equatorial genPositioner() {
            double ra = Double.parseDouble(this.rightAscension);
            double dec = Double.parseDouble(this.declination);
            double dist = this.distance < 0.0 ? 1000.0 / -this.distance : (this.distance > 0.0 ? this.distance : 10.0);
            return new Equatorial(ra, dec, AstroUnits.convert(dist, AstroUnits.Length.PC, AstroUnits.Length.AU));
        }
    }
}
