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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class AstroSpyglassManager {
    private static final float MIN_ZOOM = 10.0F;
    private static final float MAX_ZOOM = 500.0F;
    private static final float SCROLL_MULT = 1.5848932F;
    private static float zoom = 10.0F;

    public static boolean isInUse() {
        Minecraft client = Minecraft.getInstance();
        if (ClientTelescopeController.isActive()) {
            return true;
        }
        return client.player != null && client.options.getCameraType().isFirstPerson() && client.player.isScoping();
    }

    public static void setTelescopeZoom(float newZoom) {
        float cap = ClientTelescopeController.isActive() ? ClientTelescopeController.getMaxZoom() : MAX_ZOOM;
        zoom = Mth.clamp(newZoom, MIN_ZOOM, cap);
    }

    public static boolean onMouseScroll(double vertical) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.SPYGLASS_USE, 0.25F, 1.0F);
        }

        if (vertical > 0.0) {
            zoom *= SCROLL_MULT;
        } else if (vertical < 0.0) {
            if (!(zoom > MIN_ZOOM)) {
                return ClientTelescopeController.isActive();
            }
            zoom /= SCROLL_MULT;
        }

        float cap = ClientTelescopeController.isActive() ? ClientTelescopeController.getMaxZoom() : MAX_ZOOM;
        zoom = Mth.clamp(zoom, MIN_ZOOM, cap);
        return true;
    }

    public static void resetZoom() {
        zoom = MIN_ZOOM;
    }

    public static float getFovMultiplier() {
        if (ClientTelescopeController.isActive()) {
            return 1.0F / zoom;
        }
        return isInUse() ? MIN_ZOOM / zoom : 1.0F;
    }

    public static double getZoom() {
        if (ClientTelescopeController.isActive()) {
            return zoom;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return 1.0;
        }
        double mult = client.player.getFieldOfViewModifier();
        return mult > 1.0E-4 ? 1.0 / mult : 1.0;
    }

    private static double getBonusLimit() {
        return -5.0 * Math.log10((double) zoom);
    }

    public static double getMagnitudeBonus() {
        if (!isInUse()) {
            resetZoom();
            return 0.0;
        }
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        LocalPlayer player = client.player;
        if (world != null && player != null) {
            double bonus = -4.0;
            bonus -= player.getY() / 128.0;
            bonus -= 4.0 * (double) AstroLightingManager.getDeepskyVisibility();
            if (ClientTelescopeController.isActive()) {
                bonus += ClientTelescopeController.getOpticsMagnitudeBonus();
            }
            return Math.max(bonus, getBonusLimit());
        }
        return 0.0;
    }

    public static Vector3f getEquatorialDirection() {
        Matrix4f eq = AstroSkyRenderer.getEquatorialMatrix();
        if (eq == null) {
            return new Vector3f(0.0F, 0.0F, -1.0F);
        }
        Vector4f equ = new Vector4f(0.0F, 0.0F, 1.0F, 0.0F).mul(new Matrix4f(eq).invert());
        return new Vector3f(equ.x, equ.y, equ.z).negate();
    }
}
