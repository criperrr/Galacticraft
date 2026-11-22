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

package dev.galacticraft.mod.client.render.astro.deepsky;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import dev.galacticraft.mod.Constant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DSOLoader {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(DeepskyObject.class, new DSODeserializer())
            .create();

    public static void loadDSO(ArrayList<DeepskyObject> ls, ResourceManager manager, InputStream stream) {
        try (InputStreamReader isr = new InputStreamReader(stream)) {
            JsonElement root = JsonParser.parseReader(isr);
            if (root.isJsonArray()) {
                List<DeepskyObject> list = GSON.fromJson(root, new TypeToken<List<DeepskyObject>>() {}.getType());
                if (list != null) {
                    for (DeepskyObject dso : list) {
                        if (dso != null && dso.process(manager)) {
                            ls.add(dso);
                        }
                    }
                }
            } else if (root.isJsonObject()) {
                DeepskyObject dso = GSON.fromJson(root, DeepskyObject.class);
                if (dso != null && dso.process(manager)) {
                    ls.add(dso);
                }
            }
        } catch (Exception e) {
            Constant.LOGGER.error("[AstroSky] Failed to parse DSO json", e);
        }
    }

    public static Iterable<DeepskyObject> loadBackground(ResourceManager manager, ResourceLocation id) {
        List<String> loadList;
        try (InputStream stream = manager.open(id);
             JsonReader reader = new JsonReader(new InputStreamReader(stream))) {
            loadList = GSON.fromJson(reader, new TypeToken<List<String>>() {}.getType());
        } catch (Exception e) {
            Constant.LOGGER.error("[AstroSky] Failed to load background list {}", id, e);
            return Collections.emptyList();
        }

        if (loadList == null) {
            return Collections.emptyList();
        }

        ArrayList<DeepskyObject> objects = new ArrayList<>();

        for (String path : loadList) {
            if (path.endsWith("/")) {
                String directory = "astro/background/" + path.substring(0, path.length() - 1);
                for (Resource resource : manager.listResources(directory, p -> p.getNamespace().equals(Constant.MOD_ID) && p.getPath().endsWith(".json")).values()) {
                    try (InputStream is = resource.open()) {
                        loadDSO(objects, manager, is);
                    } catch (IOException ignored) {
                    }
                }
            } else if (path.endsWith(".json")) {
                Optional<Resource> res = manager.getResource(Constant.id("astro/background/" + path));
                if (res.isPresent()) {
                    try (InputStream is = res.get().open()) {
                        loadDSO(objects, manager, is);
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        return objects;
    }

    public static class AngleDeserializer implements JsonDeserializer<Double> {
        @Override
        public Double deserialize(JsonElement json, Type typeOf, JsonDeserializationContext context) {
            if (json.isJsonPrimitive()) {
                return json.getAsDouble();
            }
            JsonObject obj = json.getAsJsonObject();
            if (obj == null) {
                return 0.0;
            }
            try {
                Angle angle = context.deserialize(obj, Angle.class);
                return angle.getDegrees();
            } catch (Exception e) {
                return 0.0;
            }
        }

        public static class Angle {
            double d;
            double m;
            double s;
            double r;

            public double getDegrees() {
                return (double) (this.d < 0.0 ? -1 : 1)
                        * (Math.abs(this.d) + Math.abs(this.m) / 60.0 + Math.abs(this.s) / 3600.0 + Math.abs(this.r) / Math.PI * 180.0);
            }
        }
    }

    public static class DSODeserializer implements JsonDeserializer<DeepskyObject> {
        @Override
        public DeepskyObject deserialize(JsonElement json, Type typeOf, JsonDeserializationContext context) {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();
            return switch (type) {
                case "compact_star_file" -> context.deserialize(obj, CompactStarFile.class);
                case "skybox" -> context.deserialize(obj, Skybox.class);
                case "globular" -> context.deserialize(obj, Globular.class);
                default -> context.deserialize(obj, Nebula.class);
            };
        }
    }

    public static class RADeserializer implements JsonDeserializer<Double> {
        @Override
        public Double deserialize(JsonElement json, Type typeOf, JsonDeserializationContext context) {
            if (json.isJsonPrimitive()) {
                return json.getAsDouble();
            }
            JsonObject obj = json.getAsJsonObject();
            if (obj == null) {
                return 0.0;
            }
            try {
                RA ra = context.deserialize(obj, RA.class);
                return ra.getDegrees();
            } catch (Exception e) {
                return 0.0;
            }
        }

        public static class RA {
            double d;
            double h;
            double m;
            double s;

            public double getDegrees() {
                return this.d + this.h * 15.0 + this.m / 4.0 + this.s / 240.0;
            }
        }
    }
}
