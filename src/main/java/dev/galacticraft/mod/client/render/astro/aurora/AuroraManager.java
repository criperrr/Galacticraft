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

package dev.galacticraft.mod.client.render.astro.aurora;

import dev.galacticraft.mod.client.render.astro.AstroLightingManager;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Controls aurora borealis spawning probability per night, cold-biome detection,
 * smooth visibility transitions, and geomagnetic color palettes.
 */
public class AuroraManager {
    private static long cachedNight = Long.MIN_VALUE;
    private static long cachedSeed = Long.MIN_VALUE;
    private static boolean activeTonight = false;
    private static float tonightIntensity = 0.0F;
    private static Palette tonightPalette = Palette.POLAR_GREEN;
    private static long tonightLayoutSeed = 0L;

    private static float smoothedBiomeFactor = 0.0F;
    private static float smoothedVisibility = 0.0F;
    private static long lastFrameNanos = 0L;

    public static void update(ClientLevel level) {
        if (level == null || !AstroSkyEngine.CONFIG.enableAurora || !AstroSkyEngine.isOverworld(level)) {
            smoothedVisibility = 0.0F;
            return;
        }

        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.016F : Mth.clamp((now - lastFrameNanos) / 1.0E9F, 0.001F, 0.25F);
        lastFrameNanos = now;

        long nightIndex = (level.getDayTime() + 6000L) / 24000L;
        long worldSeed = AstroSkyEngine.getSeed();
        if (nightIndex != cachedNight || worldSeed != cachedSeed) {
            cachedNight = nightIndex;
            cachedSeed = worldSeed;
            rollForNight(worldSeed, nightIndex);
        }

        // Check if player is in a very cold biome
        float targetBiome = 0.0F;
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            BlockPos pos = player.blockPosition();
            Biome biome = level.getBiome(pos).value();
            float temp = biome.getBaseTemperature();
            float maxColdTemp = AstroSkyEngine.CONFIG.auroraMaxBiomeTemp;
            if (temp <= maxColdTemp) {
                // Colder biomes get stronger aurora (full intensity at <= 0.0, smooth ramp up to maxColdTemp)
                targetBiome = Mth.clamp((maxColdTemp - temp) / Math.max(0.05F, maxColdTemp + 0.15F) + 0.45F, 0.0F, 1.0F);
            }
        }

        // Smooth transition when crossing biome borders (~3s)
        float lerpRate = Mth.clamp(dt * 0.45F, 0.0F, 1.0F);
        smoothedBiomeFactor = Mth.lerp(lerpRate, smoothedBiomeFactor, targetBiome);

        // Compute night darkness & weather clearness
        float clearness = 1.0F - level.getRainLevel(0.0F) * 0.85F;
        float skyBright = AstroLightingManager.hasComputedLighting()
                ? AstroLightingManager.getSkyBrightness()
                : 1.0F - level.getStarBrightness(0.0F);
        float nightFactor = Mth.clamp((0.28F - skyBright) / 0.24F, 0.0F, 1.0F);

        float targetVis = activeTonight ? smoothedBiomeFactor * nightFactor * clearness * tonightIntensity : 0.0F;
        smoothedVisibility = Mth.lerp(Mth.clamp(dt * 0.65F, 0.0F, 1.0F), smoothedVisibility, targetVis);
    }

    private static void rollForNight(long worldSeed, long nightIndex) {
        long hash = worldSeed ^ (nightIndex * 0x9E3779B97F4A7C15L) ^ 0xA0B0C0D0E0F01234L;
        Random rand = new Random(hash);
        activeTonight = rand.nextDouble() < AstroSkyEngine.CONFIG.auroraChance;
        tonightLayoutSeed = rand.nextLong();
        if (activeTonight) {
            tonightIntensity = 0.65F + rand.nextFloat() * 0.35F;
            float roll = rand.nextFloat();
            if (roll < 0.60F) {
                tonightPalette = Palette.POLAR_GREEN;
            } else if (roll < 0.85F) {
                tonightPalette = Palette.ARCTIC_BLUE;
            } else {
                tonightPalette = Palette.SOLAR_STORM;
            }
        } else {
            tonightIntensity = 0.0F;
        }
    }

    public static float getVisibility() {
        return smoothedVisibility;
    }

    public static Palette getPalette() {
        return tonightPalette;
    }

    public static long getLayoutSeed() {
        return tonightLayoutSeed;
    }

    public enum Palette {
        POLAR_GREEN(
                new Vector3f(0.10F, 0.95F, 0.48F),
                new Vector3f(0.08F, 0.78F, 0.78F),
                new Vector3f(0.58F, 0.20F, 0.90F)
        ),
        ARCTIC_BLUE(
                new Vector3f(0.12F, 0.72F, 1.00F),
                new Vector3f(0.16F, 0.92F, 0.64F),
                new Vector3f(0.42F, 0.18F, 0.92F)
        ),
        SOLAR_STORM(
                new Vector3f(0.14F, 1.00F, 0.42F),
                new Vector3f(0.95F, 0.18F, 0.52F),
                new Vector3f(0.62F, 0.12F, 0.95F)
        );

        public final Vector3f lowColor;
        public final Vector3f midColor;
        public final Vector3f highColor;

        Palette(Vector3f lowColor, Vector3f midColor, Vector3f highColor) {
            this.lowColor = lowColor;
            this.midColor = midColor;
            this.highColor = highColor;
        }
    }
}
