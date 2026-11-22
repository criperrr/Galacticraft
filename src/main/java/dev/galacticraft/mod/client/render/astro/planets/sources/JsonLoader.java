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

package dev.galacticraft.mod.client.render.astro.planets.sources;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroColorUtils;
import dev.galacticraft.mod.client.render.astro.planets.*;
import dev.galacticraft.mod.client.render.astro.planets.position.Equatorial;
import dev.galacticraft.mod.client.render.astro.planets.position.Orbit;
import dev.galacticraft.mod.client.render.astro.planets.position.Positioner;
import dev.galacticraft.mod.client.render.astro.planets.position.Static;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonLoader implements Source {
    private final ArrayList<Body> bodies = new ArrayList<>();

    public void loadJson(ResourceManager resources, String path) {
        ResourceLocation id = ResourceLocation.tryParse(path);
        if (id == null) {
            Constant.LOGGER.warn("[AstroSky] Invalid dynamic json id: {}", path);
            return;
        }
        try (InputStream stream = resources.open(id)) {
            this.loadStream(stream);
        } catch (IOException e) {
            Constant.LOGGER.warn("[AstroSky] Unable to load dynamic json {}", id, e);
        }
    }

    private void loadStream(InputStream stream) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(DynamicData.class, new DynamicDeserializer())
                .registerTypeAdapter(PositionData.class, new PositionDeserializer())
                .create();

        List<DynamicData> list = gson.fromJson(new JsonReader(new InputStreamReader(stream)), new TypeToken<List<DynamicData>>() {}.getType());
        if (list != null) {
            for (DynamicData dynamic : list) {
                if (dynamic != null && dynamic.position != null) {
                    this.bodies.add(dynamic.genBody());
                }
            }
        }
    }

    private static double getAstroEpoch(double julianDate) {
        return (julianDate - 2451545.0) / 365.25;
    }

    @Override
    public Iterable<Body> getBodies() {
        return this.bodies;
    }

    private static class AsteroidData extends DynamicData {
        public double H;
        public double G;

        @Override
        public Body genBody() {
            return new Asteroid(this.getFullID(), this.position.genPositioner(), this.H, this.G, this.getColor()).alias(this.aliases);
        }
    }

    private static class CometData extends DynamicData {
        public double M1;
        public double K1;

        @Override
        public Body genBody() {
            return new Comet(this.getFullID(), this.position.genPositioner(), this.M1, this.K1).alias(this.aliases);
        }
    }

    private static class DummyData extends DynamicData {
    }

    private static class DynamicData {
        public String type;
        public String id;
        public String primary;
        public String[] aliases = new String[0];
        public PositionData position;
        public double bvi;
        public double temp;
        public int hex;

        protected String getFullID() {
            return this.primary + "." + this.id;
        }

        protected Vector3f getColor() {
            if (this.hex > 0) {
                return AstroColorUtils.fromHex(this.hex);
            }
            return this.temp > 0.0 ? AstroColorUtils.fromTemperature(this.temp) : AstroColorUtils.fromIndex(this.bvi);
        }

        public Body genBody() {
            return new Body(this.getFullID(), this.position.genPositioner());
        }
    }

    private static class DynamicDeserializer implements JsonDeserializer<DynamicData> {
        @Override
        public DynamicData deserialize(JsonElement json, Type typeOf, JsonDeserializationContext context) {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();
            return switch (type) {
                case "asteroid" -> context.deserialize(obj, AsteroidData.class);
                case "comet" -> context.deserialize(obj, CometData.class);
                case "planet", "artificial" -> context.deserialize(obj, PlanetData.class);
                case "star" -> context.deserialize(obj, StarData.class);
                default -> context.deserialize(obj, DummyData.class);
            };
        }
    }

    private static class EquatorialData extends PositionData {
        public double ra;
        public double de;
        public double plx = 0.0;
        public double pmra = 0.0;
        public double pmde = 0.0;
        public double rv = 0.0;

        @Override
        public Positioner genPositioner() {
            double dist = 2.0626480624537998E8 / (this.plx > 1.0 ? this.plx : 1.0);
            Equatorial equatorial = new Equatorial(this.ra, this.de, dist);
            if (this.pmra != 0.0 || this.pmde != 0.0 || this.rv != 0.0) {
                equatorial.setProperMotion(getAstroEpoch(this.epoch), this.pmra * 2.7777778E-7, this.pmde * 2.7777778E-7, this.rv * 0.2109452857);
            }
            return equatorial;
        }
    }

    private static class OrbitData extends PositionData {
        public double e;
        public double a;
        public double q;
        public double T;
        public double i;
        public double n;
        public double w;
        public double M;

        @Override
        public Positioner genPositioner() {
            double sma = this.q > 0.0 ? this.q / (1.0 - this.e) : this.a;
            return new Orbit(sma, this.T, this.e, this.i).sync(getAstroEpoch(this.epoch), this.n, this.n + this.w, this.n + this.w + this.M);
        }
    }

    private static class PlanetData extends DynamicData {
        public double diameter;
        public double albedo;
        public String model;

        @Override
        public Body genBody() {
            double diamAU = 6.6846E-9 * this.diameter;
            String modelID = this.model != null ? this.model : "terrestrial";
            return new Planet(this.getFullID(), this.position.genPositioner(), modelID, diamAU, this.albedo, this.getColor()).alias(this.aliases);
        }
    }

    private static class PositionData {
        public String type;
        public double epoch = 2451545.0;

        public Positioner genPositioner() {
            return new Static(0.0, 0.0, 0.0);
        }
    }

    private static class PositionDeserializer implements JsonDeserializer<PositionData> {
        @Override
        public PositionData deserialize(JsonElement json, Type typeOf, JsonDeserializationContext context) {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();
            return switch (type) {
                case "orbit" -> context.deserialize(obj, OrbitData.class);
                case "equatorial" -> context.deserialize(obj, EquatorialData.class);
                default -> context.deserialize(obj, RectangularData.class);
            };
        }
    }

    private static class RectangularData extends PositionData {
        public double x;
        public double y;
        public double z;

        @Override
        public Positioner genPositioner() {
            return new Static(this.x, this.y, this.z);
        }
    }

    private static class StarData extends DynamicData {
        public double M;
        public double B;
        public double diameter;
        public Variability variability;

        @Override
        public Body genBody() {
            StarBody star = new StarBody(this.getFullID(), this.position.genPositioner(), this.M + this.B, 6.6846E-9 * this.diameter, this.getColor());
            if (this.variability != null && this.variability.type != null) {
                switch (this.variability.type) {
                    case "scatter" -> {
                        if (this.variability.period > 0.0) {
                            star.setPeriodicVariability(this.variability.period, this.variability.amplitude / 2.0);
                            star.setScatter(this.variability.amplitude / 2.0);
                        } else {
                            star.setScatter(this.variability.amplitude);
                        }
                    }
                    case "cepheid" -> star.setPeriodicVariability(this.variability.period, this.variability.amplitude);
                    case "eclipse" -> star.setPeriodicEclipse(
                            getAstroEpoch(this.variability.epoch),
                            this.variability.period,
                            this.variability.amplitude,
                            this.variability.duration,
                            this.variability.secondary
                    );
                }
            }
            star.alias(this.aliases);
            return star;
        }

        private static class Variability {
            public String type = null;
            public double period;
            public double amplitude;
            public double duration;
            public double secondary;
            public double epoch;
        }
    }
}
