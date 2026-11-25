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

package dev.galacticraft.mod.client.render.astro;

import dev.galacticraft.mod.client.render.astro.planets.ConfigurablePlanet;
import dev.galacticraft.mod.client.render.astro.planets.ConfigurableStar;

import java.util.List;
import java.util.function.Supplier;

public class AstroSkyConfig {
    public SkyRotationOptions skyRotationOptions = new SkyRotationOptions();
    public PlanetTimeOptions planetTimeOptions = new PlanetTimeOptions();
    public ConfigurableStar.ConfigurableAppearance sunAppearance = new ConfigurableStar.ConfigurableAppearance("#ffe992", 4.83);

    public boolean enableEclipses = true;
    public boolean enableSunspots = true;
    public boolean enableMoonlight = true;
    public int diameterScale = 10;

    public boolean enablePlanets = true;
    public boolean enableAsteroids = true;
    public String observerPlanet = "Earth";
    public boolean bigPlanets = false;

    public List<ConfigurablePlanet> planets = List.of(
            new ConfigurablePlanet(
                    "Mercury",
                    new ConfigurablePlanet.ConfigurableOrbit(0.308, 0.467, 0.240846, 7.005, 48.331, 77.456, 252.251),
                    new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 4880.0, AstroColorUtils.fromRelativeAlbedo(0xFFE4C4, 0.142))
            ),
            new ConfigurablePlanet(
                    "Venus",
                    new ConfigurablePlanet.ConfigurableOrbit(0.718, 0.728, 0.615198, 3.394, 76.68, 131.564, 181.679),
                    new ConfigurablePlanet.ConfigurableAppearance("Smooth", 12104.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFF7, 0.689))
            ),
            new ConfigurablePlanet(
                    "Earth",
                    new ConfigurablePlanet.ConfigurableOrbit(0.9833, 1.0167, 1.0, 0.0, 0.0, 102.93768, 100.46435),
                    new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 12742.0, AstroColorUtils.fromRelativeAlbedo(0x2F97FF, 0.434))
            ).setSpecialID("earth").addMoons(
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Moon",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(350000.0, 418000.0, 29.530589, 10.32, 125.04452, 83.35324, 218.31665, 18.5996, 8.8504),
                            new ConfigurablePlanet.ConfigurableAppearance("Moon", 3474.8, AstroColorUtils.fromRelativeAlbedo(0xC3CEB5, 0.136))
                    ).setSpecialID("moon")
            ),
            new ConfigurablePlanet(
                    "Mars",
                    new ConfigurablePlanet.ConfigurableOrbit(1.381, 1.666, 1.88085, 1.85, 49.558, 336.06, 355.433),
                    new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 6779.0, AstroColorUtils.fromRelativeAlbedo(0xFF9FBF, 0.17))
            ).addMoons(
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Phobos",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(9234.0, 9518.0, 0.31891, 1.1, 169.2, 25.5, 215.1, 2.262, 1.132),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 22.16, AstroColorUtils.fromRelativeAlbedo(0xC3CEB5, 0.929))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Deimos",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(23500.0, 23500.0, 1.262361, 1.8, 54.4, 54.4, 259.4, 54.537, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 12.54, AstroColorUtils.fromRelativeAlbedo(0xC3CEB5, 0.932))
                    )
            ),
            new ConfigurablePlanet(
                    "Jupiter",
                    new ConfigurablePlanet.ConfigurableOrbit(4.95, 5.459, 11.862, 1.303, 100.464, 14.331, 34.352),
                    new ConfigurablePlanet.ConfigurableAppearance("Banded", 139820.0, AstroColorUtils.fromRelativeAlbedo(0xFFE99F, 0.538))
            ).addMoons(
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Io",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(420000.0, 423400.0, 1.769861, 0.0, 99.8, 84.1, 171.0, 0.0, 1.333),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 3642.0, AstroColorUtils.fromRelativeAlbedo(0xFFFF40, 0.63))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Europa",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(664862.0, 676938.0, 3.554095, 0.5, 99.4, 89.0, 231.2, 30.202, 1.394),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 3122.0, AstroColorUtils.fromRelativeAlbedo(0xC0D7FF, 0.67))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Ganymede",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(1070400.0, 1070400.0, 7.166389, 0.2, 100.0, 192.4, 30.5, 137.812, 68.301),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 5268.0, AstroColorUtils.fromRelativeAlbedo(0xFFD740, 0.43))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Callisto",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(1882700.0, 1882700.0, 16.753563, 0.3, 100.1, 52.6, 342.0, 577.264, 277.921),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 4821.0, AstroColorUtils.fromRelativeAlbedo(0xE0FFE0, 0.22))
                    )
            ),
            new ConfigurablePlanet(
                    "Saturn",
                    new ConfigurablePlanet.ConfigurableOrbit(9.048, 10.124, 29.448, 2.485, 113.666, 93.057, 50.077),
                    new ConfigurablePlanet.ConfigurableAppearance("Banded", 116460.0, AstroColorUtils.fromRelativeAlbedo(0xCFFFDF, 0.499)).addRings()
            ).addMoons(
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Mimas",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(181000.0, 189176.0, 0.94, 1.57, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 396.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 1.0))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Enceladus",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(237948.0, 239157.0, 1.37, 0.02, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 504.0, AstroColorUtils.fromRelativeAlbedo(0xF0F7FF, 1.0))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Tethys",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(294619.0, 294675.0, 1.89, 1.09, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 1062.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 1.0))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Dione",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(377396.0, 377515.0, 2.73, 0.02, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 1123.0, AstroColorUtils.fromRelativeAlbedo(0xA0C7FF, 1.0))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Rhea",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(527108.0, 527163.0, 4.52, 0.35, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 1527.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 1.0))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Titan",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(1186680.0, 1222000.0, 15.95, 0.33, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Smooth", 5151.0, AstroColorUtils.fromRelativeAlbedo(0xFFC770, 0.22))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Hyperion",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(1462000.0, 1500000.0, 21.28, 0.43, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 270.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 0.3))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Iapetus",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(3560820.0, 3561302.0, 79.32, 15.47, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 1471.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 0.3))
                    )
            ),
            new ConfigurablePlanet(
                    "Uranus",
                    new ConfigurablePlanet.ConfigurableOrbit(18.376, 20.083, 84.02, 0.773, 74.006, 171.005, 313.243),
                    new ConfigurablePlanet.ConfigurableAppearance("Smooth", 50724.0, AstroColorUtils.fromRelativeAlbedo(0x3FCFFF, 0.488)).addRings()
            ).addMoons(
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Ariel",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(191020.0, 191240.0, 2.52, 0.26, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 1158.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 0.39))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Umbriel",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(266000.0, 266300.0, 4.14, 0.36, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 1169.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 0.18))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Titania",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(435840.0, 436170.0, 8.71, 0.08, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 1578.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 0.27))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Oberon",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(582620.0, 584020.0, 13.46, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 1523.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 0.23))
                    ),
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Miranda",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(129390.0, 130090.0, 1.41, 4.22, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 471.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 0.32))
                    )
            ),
            new ConfigurablePlanet(
                    "Neptune",
                    new ConfigurablePlanet.ConfigurableOrbit(29.811, 30.333, 164.8, 1.77, 131.783, 44.97, 304.853),
                    new ConfigurablePlanet.ConfigurableAppearance("Smooth", 49244.0, AstroColorUtils.fromRelativeAlbedo(0x1F7FFF, 0.442))
            ).addMoons(
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Triton",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(354800.0, 354800.0, 5.88, 157.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 2706.0, AstroColorUtils.fromRelativeAlbedo(0x80C7FF, 0.76))
                    )
            ),
            new ConfigurablePlanet(
                    "Pluto",
                    new ConfigurablePlanet.ConfigurableOrbit(29.658, 39.482, 247.94, 17.16, 110.299, 224.133, 238.663),
                    new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 2376.6, AstroColorUtils.fromRelativeAlbedo(0xFFCFBF, 0.52))
            ).addMoons(
                    new ConfigurablePlanet.ConfigurableMoon(
                            "Charon",
                            new ConfigurablePlanet.ConfigurableMoonOrbit(17536.0, 17536.0, 6.39, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                            new ConfigurablePlanet.ConfigurableAppearance("Terrestrial", 1212.0, AstroColorUtils.fromRelativeAlbedo(0xFFFFFF, 0.35))
                    )
            )
    );

    public int magnitudeLimit = 7;
    public boolean enableRealStars = true;
    public boolean enableVanillaStars = false;
    public boolean daytimeStars = false;
    public StarRenderingConfig starRenderingOptions = new StarRenderingConfig();
    public boolean enableDeepsky = true;
    public int deepskyBrightness = 50;
    public int skyboxColor = 0x9FB0CF;
    public boolean enableCustomStars = true;
    public List<ConfigurableStar> customStars = List.of();

    public boolean enableComets = true;
    public boolean enableMeteors = true;
    public boolean enableAurora = true;
    public double auroraChance = 0.40;
    public float auroraMaxBiomeTemp = 0.15F;
    public int totalMeteors = 100;
    public int sporadicMeteors = 6;
    public int spaceStart = 400;
    public int spaceScale = 100;

    public AstroLightingManager.LightingConfig lightingOptions = new AstroLightingManager.LightingConfig();

    public boolean showConstellations = false;
    public GridOptions azimuthalGridOptions = new GridOptions(0x00FFFF);
    public GridOptions equatorialGridOptions = new GridOptions(0x00FF00);
    public GridOptions eclipticGridOptions = new GridOptions(0xFFFF00);
    public GridOptions galacticGridOptions = new GridOptions(0xFF00FF);

    public int updateTime = 240;
    public boolean updateInterpolation = true;
    public boolean enableSpyglassZoom = true;
    public boolean enableSpyglassCulling = true;
    public double asteroidBonus = -3.0;
    public double cometBonus = 0.0;

    public List<DimensionObserver> dimensionObservers = List.of(
            new DimensionObserver("minecraft:overworld", "Earth"),
            new DimensionObserver("galacticraft:moon", "Moon"),
            new DimensionObserver("galacticraft:mars", "Mars"),
            new DimensionObserver("galacticraft:venus", "Venus"),
            new DimensionObserver("galacticraft:mercury", "Mercury"),
            new DimensionObserver("galacticraft:jupiter", "Jupiter"),
            new DimensionObserver("galacticraft:saturn", "Saturn"),
            new DimensionObserver("galacticraft:uranus", "Uranus"),
            new DimensionObserver("galacticraft:neptune", "Neptune"),
            new DimensionObserver("galacticraft:asteroid", "Mars"),
            new DimensionObserver("galacticraft:phobos", "Phobos"),
            new DimensionObserver("galacticraft:deimos", "Deimos"),
            new DimensionObserver("galacticraft:io", "Io"),
            new DimensionObserver("galacticraft:europa", "Europa"),
            new DimensionObserver("galacticraft:ganymede", "Ganymede"),
            new DimensionObserver("galacticraft:callisto", "Callisto")
    );

    public boolean vanillaSun = false;
    public boolean vanillaMoon = false;

    public Supplier<List<? extends Object>> getPlanetSupplier() {
        return () -> this.planets;
    }

    public Supplier<List<? extends Object>> getCustomStarSupplier() {
        return () -> this.enableCustomStars ? this.customStars : List.of();
    }

    public static class DimensionObserver {
        public String dimension;
        public String observer;

        public DimensionObserver(String dimension, String observer) {
            this.dimension = dimension;
            this.observer = observer;
        }
    }

    public static class Globe {
        public double meridianX = 0.0;
        public double equatorZ = 0.0;
        public double circumference = 65536.0;
    }

    public static class GridOptions {
        public boolean showCircle = false;
        public boolean showGrid = false;
        public int color;

        public GridOptions(int color) {
            this.color = color;
        }
    }

    public static class PlanetTimeOptions {
        public boolean liveMode = false;
        public boolean useWorldSeed = true;
        public double yearLength = 365.25636;
        public int seasonOffset = 0;
        public double monthLength = 29.53059;
        public int moonPhaseOffset = 180;
        public double seedLimit = 18.04;
        public double seedOffset = 50.0;
    }

    public static class SkyRotationOptions {
        public boolean liveMode = false;
        public int latitude = -30;
        public int longitude = 0;
        public boolean globeModeLatitude = false;
        public boolean globeModeLongitude = false;
        public Globe globe = new Globe();
        public int axialTilt = 23;
    }

    public static class StarRenderingConfig {
        public int starOpacity = 80;
        public int starSaturation = 80;
        public float baseRadius = 0.3F;
        public float minimumRadius = 0.1F;
        public float brightnessPower = 3.0F;
    }
}
