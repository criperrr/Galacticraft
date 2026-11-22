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

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroMath;
import dev.galacticraft.mod.client.render.astro.AstroSkyConfig;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.planets.position.Rotator;
import dev.galacticraft.mod.client.render.astro.planets.position.Static;
import dev.galacticraft.mod.client.render.astro.planets.sources.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PlanetManager extends AstroMath {
    private static final List<Source> CORE_SOURCES = List.of(new SunLoader(), new EssentialBodies());
    private static final ArrayList<Source> SOURCES = new ArrayList<>();
    private static final HashMap<String, String> ALIASES = new HashMap<>();

    private static Body root;
    private static StarBody sun;
    private static Body obsMount;
    private static Body obsBody;

    public static void addAlias(String alias, String id) {
        ALIASES.put(alias, id);
    }

    private static String getObserverID() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return AstroSkyEngine.CONFIG.observerPlanet;
        }
        String dimensionID = client.level.dimension().location().toString();
        for (AstroSkyConfig.DimensionObserver dimObs : AstroSkyEngine.CONFIG.dimensionObservers) {
            if (dimObs.dimension.equals(dimensionID)) {
                return dimObs.observer;
            }
        }
        return AstroSkyEngine.CONFIG.observerPlanet;
    }

    private static void resolveObserver() {
        String obsID = getObserverID();
        String[] split = obsID.split("@", 2);
        String bodyName = split[0].toLowerCase().replace(' ', '-');
        String bodyID = ALIASES.get(bodyName);
        Body body = root.findChild(bodyID != null ? bodyID : bodyName);
        if (body == null) {
            body = root.findChild("sun.earth");
        }

        if (body != null) {
            String mountID = split.length > 1 ? split[1] : "default";
            String[] mountSplit = mountID.split(":", 2);
            String mountType = mountSplit[0];
            double[] mountArgs = new double[0];
            if (mountSplit.length > 1) {
                mountArgs = Arrays.stream(mountSplit[1].split(",")).mapToDouble(Double::parseDouble).toArray();
            }

            obsBody = body;
            obsMount = body.genMount(mountType, mountArgs);
            Body obsSun = obsBody;
            while (obsSun != null && !(obsSun instanceof StarBody)) {
                obsSun = obsSun.getParent();
            }
            sun = obsSun != null ? (StarBody) obsSun : (StarBody) root.findChild("sun");
        }
    }

    public static void loadDynamics(ResourceManager resources, InputStream stream) {
        SOURCES.clear();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] words = line.split("#")[0].trim().split("\\s+");
                    if (words.length >= 2 && words[0].equals("json")) {
                        JsonLoader loader = new JsonLoader();
                        loader.loadJson(resources, words[1]);
                        SOURCES.add(loader);
                    } else if (words.length >= 4 && words[0].equals("comet-generator")) {
                        SOURCES.add(CometGenerator.buildWithArgs(words[1], words[2], words[3]));
                    }
                } catch (Exception e) {
                    Constant.LOGGER.warn("[AstroSky] Unable to load dynamic entry: {}", line, e);
                }
            }
        } catch (IOException e) {
            Constant.LOGGER.error("[AstroSky] Failed reading dynamic.load", e);
        }

        SOURCES.add(
                new ConfigLoader()
                        .mountList(AstroSkyEngine.CONFIG.getPlanetSupplier(), "sun")
                        .mountList(AstroSkyEngine.CONFIG.getCustomStarSupplier(), "custom-star")
        );
    }

    public static void loadSources() {
        ALIASES.clear();
        if (root != null) {
            root.removeChildren();
        }

        root = new Body("", new Static(0.0, 0.0, 0.0));
        Body stars = new Namespace("star", new Rotator(new Static(0.0, 0.0, 0.0), 23.45, 0.0, 0.0), () -> AstroSkyEngine.CONFIG.enableRealStars);
        root.putChild(stars.getID(), stars);
        Body customStars = new Namespace("custom-star", new Rotator(new Static(0.0, 0.0, 0.0), 23.45, 0.0, 0.0), () -> AstroSkyEngine.CONFIG.enableCustomStars);
        root.putChild(customStars.getID(), customStars);

        ArrayList<Body> nextQueue = new ArrayList<>();

        for (Source source : CORE_SOURCES) {
            for (Body body : source.getBodies()) {
                nextQueue.add(body);
                for (String alias : body.getAliases()) {
                    addAlias(alias, body.getID());
                }
            }
        }

        for (Source source : SOURCES) {
            for (Body body : source.getBodies()) {
                nextQueue.add(body);
                for (String alias : body.getAliases()) {
                    addAlias(alias, body.getID());
                }
            }
        }

        for (int depth = 0; !nextQueue.isEmpty() && depth < 16; depth++) {
            ArrayList<Body> queue = nextQueue;
            nextQueue = new ArrayList<>();
            for (Body body : queue) {
                if (body.getDepth() > depth) {
                    nextQueue.add(body);
                } else {
                    root.putChild(body.getID(), body);
                }
            }
        }

        resolveObserver();
    }

    public static double getPlanetTime() {
        double worldTime = (double) AstroSkyEngine.getWorldTime() / 24000.0 / AstroSkyEngine.CONFIG.planetTimeOptions.yearLength;
        double seedTime = AstroSkyEngine.getSeedTime();
        double liveTime = ((double) System.currentTimeMillis() / 1000.0 - 9.46728E8 + 64.0) / 3.15569411136E7;
        return AstroSkyEngine.CONFIG.planetTimeOptions.liveMode ? liveTime : seedTime + worldTime;
    }

    public static double getUpdateInterval() {
        int ticks = AstroSkyEngine.CONFIG.updateTime;
        return (double) ticks / 24000.0 / AstroSkyEngine.CONFIG.planetTimeOptions.yearLength;
    }

    public static double getTropicalAngle() {
        return sun == null || obsBody == null ? 0.0 : sun.getAngularPosition(obsBody, getPlanetTime()).x;
    }

    public static Vector3d getSunPosition() {
        return sun == null ? null : sun.getPosition(getPlanetTime());
    }

    public static Vector3d getObserverPosition() {
        return obsMount == null ? new Vector3d() : obsMount.getPosition(getPlanetTime());
    }

    public static void renderAll() {
        double time = getPlanetTime();
        PlanetRenderer.init();
        if (obsMount != null && sun != null && root != null) {
            root.renderTree(sun, obsMount, time);
            PlanetRenderer.compile();
        }
    }
}
