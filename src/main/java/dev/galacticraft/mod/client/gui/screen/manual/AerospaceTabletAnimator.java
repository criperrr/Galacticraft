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

package dev.galacticraft.mod.client.gui.screen.manual;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Smooth frame-rate-independent animation controller coordinating:
 * <ul>
 *   <li>Two-handed 3D Aerospace Tablet raise/lower in first-person ({@code 0.0 -> 1.0 -> 0.0})</li>
 *   <li>Camera FOV zoom smoothly focusing onto the 3D tablet display</li>
 *   <li>Seamless scale/reveal lock-in of the crisp interactive {@link AerospaceManualScreen}</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class AerospaceTabletAnimator {
    private static final float OPEN_DURATION_SEC = 0.34F;
    private static final float CLOSE_DURATION_SEC = 0.24F;

    private static boolean targetOpen = false;
    private static float rawProgress = 0.0F;
    private static long lastUpdateNanos = System.nanoTime();

    private AerospaceTabletAnimator() {
    }

    public static void beginOpen() {
        update();
        targetOpen = true;
    }

    public static void beginClose() {
        update();
        targetOpen = false;
    }

    public static void reset() {
        targetOpen = false;
        rawProgress = 0.0F;
        lastUpdateNanos = System.nanoTime();
    }

    private static void update() {
        Minecraft mc = Minecraft.getInstance();
        if (targetOpen && !(mc.screen instanceof AerospaceManualScreen)) {
            targetOpen = false;
        }
        long now = System.nanoTime();
        float dt = Math.min(0.05F, Math.max(0.0F, (now - lastUpdateNanos) / 1_000_000_000.0F));
        lastUpdateNanos = now;

        if (targetOpen) {
            rawProgress = Math.min(1.0F, rawProgress + dt / OPEN_DURATION_SEC);
        } else {
            rawProgress = Math.max(0.0F, rawProgress - dt / CLOSE_DURATION_SEC);
        }
    }

    /**
     * Smooth ease-out-cubic progress in {@code [0.0, 1.0]}.
     */
    public static float getProgress() {
        update();
        float t = Mth.clamp(rawProgress, 0.0F, 1.0F);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
    }

    public static boolean isTargetOpen() {
        update();
        return targetOpen;
    }

    public static boolean isAnimatingOrOpen() {
        return getProgress() > 0.002F;
    }

    /**
     * Smooth camera FOV multiplier zooming ~22% into the 3D tablet screen as it rises to eye level.
     */
    public static double fovMultiplier() {
        float p = getProgress();
        if (p <= 0.001F) {
            return 1.0;
        }
        return Mth.lerp(p, 1.0, 0.78);
    }

    /**
     * Interactive GUI screen reveal factor ({@code 0.0} while tablet is rising, smoothly {@code 0 -> 1}
     * once the tablet nears eye level so the 3D raise animation is clearly visible first).
     */
    public static float screenRevealAlpha() {
        float p = getProgress();
        return Mth.clamp((p - 0.52F) / 0.48F, 0.0F, 1.0F);
    }

    /**
     * Interactive GUI zoom scale synced with the final lock-in of the 3D tablet.
     */
    public static float screenZoomScale() {
        float a = screenRevealAlpha();
        return Mth.lerp(a, 0.84F, 1.0F);
    }
}
