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

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.deepsky.DeepskyManager;
import dev.galacticraft.mod.client.render.astro.meteors.MeteorGenerator;
import dev.galacticraft.mod.client.render.astro.overlays.OverlayManager;
import dev.galacticraft.mod.client.render.astro.planets.ModelManager;
import dev.galacticraft.mod.client.render.astro.planets.PlanetManager;
import dev.galacticraft.mod.mixin.client.BiomeManagerAccessor;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;

import java.io.InputStream;
import java.util.Random;

public class AstroSkyEngine implements SimpleSynchronousResourceReloadListener {
    public static final AstroSkyEngine INSTANCE = new AstroSkyEngine();
    public static final AstroSkyConfig CONFIG = new AstroSkyConfig();

    private static boolean initialized = false;
    private static boolean reloadStars = false;
    private static int lastTS = Integer.MIN_VALUE;
    private static long lastSeed = Long.MIN_VALUE;
    private static String lastDimension = "";

    @Override
    public ResourceLocation getFabricId() {
        return Constant.id("astro_sky_loader");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        ModelManager.clearModelData();

        for (ResourceLocation id : manager.listResources("astro/planet_models", path -> path.getPath().endsWith(".json")).keySet()) {
            try (InputStream stream = manager.open(id)) {
                ModelManager.addModelData(id, stream);
            } catch (Exception e) {
                Constant.LOGGER.error("[AstroSky] Failed to load planet model json {}", id, e);
            }
        }

        ModelManager.genModels();
        DeepskyManager.loadBackground(manager, Constant.id("astro/background/background.json"));

        try (InputStream stream = manager.open(Constant.id("astro/dynamic.load"))) {
            PlanetManager.loadDynamics(manager, stream);
        } catch (Exception e) {
            Constant.LOGGER.error("[AstroSky] Failed to load dynamic.load", e);
        }

        OverlayManager.load(manager);
        initialized = true;
        loadConfigData();
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean isOverworld(ClientLevel level) {
        return level != null && level.dimension() == Level.OVERWORLD;
    }

    public static boolean isSupportedDimension(ClientLevel level) {
        if (level == null) {
            return false;
        }
        String dimId = level.dimension().location().toString();
        for (AstroSkyConfig.DimensionObserver obs : CONFIG.dimensionObservers) {
            if (obs.dimension.equals(dimId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldReloadConfig() {
        long seed = getSeed();
        if (seed != lastSeed) {
            lastSeed = seed;
            ClientLevel world = getWorld();
            if (world != null) {
                lastDimension = world.dimension().location().toString();
            }
            return true;
        }
        ClientLevel world = getWorld();
        if (world == null) {
            return false;
        }
        String dimensionID = world.dimension().location().toString();
        if (!dimensionID.equals(lastDimension)) {
            lastDimension = dimensionID;
            return true;
        }
        return false;
    }

    public static boolean shouldReloadDynamics() {
        if (CONFIG.updateTime <= 0) {
            return true;
        }
        if (reloadStars) {
            reloadStars = false;
            return true;
        }
        long time = getWorldTime();
        int ts = (int) (time / CONFIG.updateTime);
        if (ts != lastTS) {
            lastTS = ts;
            return true;
        }
        return false;
    }

    public static void ensureUpToDate() {
        if (!initialized) {
            return;
        }
        if (shouldReloadConfig()) {
            loadConfigData();
        } else if (shouldReloadDynamics()) {
            loadRenderData();
        }
    }

    public static void loadRenderData() {
        AstroStarRenderer.initDynamicStars();
        if (CONFIG.enableMeteors && MeteorGenerator.shouldGenForNight()) {
            MeteorGenerator.genForNight();
        }
        PlanetManager.renderAll();
        AstroStarRenderer.updateBuffers(AstroStarRenderer.Level.DYNAMIC);
    }

    public static void loadConfigData() {
        PlanetManager.loadSources();
        if (CONFIG.enableMeteors) {
            MeteorGenerator.genForNight();
        }
        AstroStarRenderer.reloadConfig();
        AstroStarRenderer.updateBuffers(AstroStarRenderer.Level.STATIC);
        loadRenderData();
    }

    public static ClientLevel getWorld() {
        return Minecraft.getInstance().level;
    }

    public static long getWorldTime() {
        ClientLevel world = getWorld();
        return world == null ? 0L : world.getDayTime();
    }

    public static double getSeedTime() {
        return CONFIG.planetTimeOptions.seedOffset + (double) getSeed() * Math.PI % CONFIG.planetTimeOptions.seedLimit;
    }

    public static float getClearness() {
        ClientLevel world = getWorld();
        return world == null ? 1.0F : 1.0F - world.getRainLevel(0.0F);
    }

    public static float getVisibility() {
        double starBrightness = AstroRenderUtils.getStarBrightness();
        return (float) ((double) getClearness() * AstroMath.clamp(starBrightness * 2.0, 0.5, 1.0));
    }

    public static double getSolarHourAngle(ClientLevel world, float tickDelta) {
        return (double) world.getTimeOfDay(tickDelta) * 360.0;
    }

    public static double getSolarHourAngle() {
        ClientLevel world = getWorld();
        return world != null ? getSolarHourAngle(world, 0.0F) : 0.0;
    }

    public static double getTimeAngle(double years) {
        return 360.0 * (double) getWorldTime() / 24000.0 / years / CONFIG.planetTimeOptions.yearLength;
    }

    public static double getTimeAngle(double years, double startAngle) {
        return getTimeAngle(years) + startAngle;
    }

    public static boolean isUsingSpyglass() {
        return AstroSpyglassManager.isInUse();
    }

    public static long getSeed() {
        ClientLevel world = getWorld();
        if (world == null || !CONFIG.planetTimeOptions.useWorldSeed) {
            return 0L;
        }
        return ((BiomeManagerAccessor) world.getBiomeManager()).galacticraft$getBiomeZoomSeed();
    }

    public static double getRandomAngle(long seed) {
        Random rand = new Random(getSeed() + seed);
        return rand.nextDouble() * 360.0;
    }
}
