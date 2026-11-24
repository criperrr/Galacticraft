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

import com.mojang.blaze3d.platform.InputConstants;
import dev.galacticraft.mod.client.render.astro.deepsky.DeepskyManager;
import dev.galacticraft.mod.client.render.astro.deepsky.DeepskyObject;
import dev.galacticraft.mod.client.render.astro.planets.PlanetManager;
import dev.galacticraft.mod.content.block.entity.machine.ObservatoryTelescopeBlockEntity;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.network.c2s.ObservatoryCapturePayload;
import dev.galacticraft.mod.research.ClientResearchCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * First-person astronomical eyepiece controller for {@code OBSERVATORY_TELESCOPE}.
 * Mounts the live camera directly on the telescope's front objective lens (never the player's body),
 * locks all player movement/inventory/hotbar/drop controls while operating the bench, uses safe
 * focus controls ({@code Z / C}, {@code [ / ]}, {@code - / =}), and enforces magnification,
 * atmospheric seeing shimmer, and 4-second photometric lock.
 */
@Environment(EnvType.CLIENT)
public final class ClientTelescopeController {
    private record BrightStarRef(String id, String name, double raDeg, double decDeg, float mag) {
    }

    private static final List<BrightStarRef> BRIGHT_STARS = List.of(
            new BrightStarRef("star:sirius", "Sirius (Alpha CMa)", 101.287, -16.716, -1.46F),
            new BrightStarRef("star:canopus", "Canopus (Alpha Car)", 95.988, -52.696, -0.74F),
            new BrightStarRef("star:alpha_centauri", "Alpha Centauri", 219.902, -60.834, -0.27F),
            new BrightStarRef("star:arcturus", "Arcturus (Alpha Boo)", 213.915, 19.182, -0.05F),
            new BrightStarRef("star:vega", "Vega (Alpha Lyr)", 279.235, 38.784, 0.03F),
            new BrightStarRef("star:capella", "Capella (Alpha Aur)", 79.172, 45.998, 0.08F),
            new BrightStarRef("star:rigel", "Rigel (Beta Ori)", 78.634, -8.202, 0.13F),
            new BrightStarRef("star:procyon", "Procyon (Alpha CMi)", 114.825, 5.225, 0.34F),
            new BrightStarRef("star:betelgeuse", "Betelgeuse (Alpha Ori)", 88.793, 7.407, 0.42F),
            new BrightStarRef("star:altair", "Altair (Alpha Aql)", 297.696, 8.868, 0.76F),
            new BrightStarRef("star:aldebaran", "Aldebaran (Alpha Tau)", 68.980, 16.509, 0.86F),
            new BrightStarRef("star:antares", "Antares (Alpha Sco)", 247.352, -26.432, 0.96F),
            new BrightStarRef("star:polaris", "Polaris (Alpha UMi)", 37.954, 89.264, 1.98F),
            new BrightStarRef("star:deneb", "Deneb (Alpha Cyg)", 310.358, 45.280, 1.25F)
    );

    private static final float REQUIRED_EXPOSURE_TICKS = 80.0F; // 4.0 seconds steady lock

    private static boolean active = false;
    private static @Nullable BlockPos activePos = null;
    private static @Nullable CameraType savedCameraType = null;
    private static int apertureTier = 1;
    private static int opticsTier = 0;
    private static int auxDriveType = ObservatoryTelescopeBlockEntity.AUX_NONE;
    private static int clarityPct = 70;
    private static float airmass = 1.0F;

    // Telescope's own independent optical mount orientation (decoupled from player body!)
    private static float aimYaw = 0.0F;
    private static float aimPitch = -38.0F;

    private static float focusRing = 20.0F; // 0..100 controlled by Z/C or [/] or -/=
    private static float exposureTicks = 0.0F;
    private static int flashTicks = 0;
    private static int aimSyncCooldown = 0;
    private static int captureCooldownTicks = 0;
    private static int enterGraceTicks = 0;

    private static @Nullable LockedSkyObject currentTarget = null;
    private static int currentSharpnessPct = 0;
    private static boolean zoomSufficient = true;
    private static int requiredZoomForTarget = 10;

    private ClientTelescopeController() {
    }

    public static void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTelescopeController::onClientTick);
        HudRenderCallback.EVENT.register(ClientTelescopeController::onRenderHud);
    }

    public static void enterEyepiece(@NotNull BlockPos pos, int aperture, int optics, int aux, int clarity, float effAirmass) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        active = true;
        activePos = pos.immutable();
        savedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);

        apertureTier = Mth.clamp(aperture, 1, 3);
        opticsTier = Mth.clamp(optics, 0, 3);
        auxDriveType = Mth.clamp(aux, 0, 3);
        clarityPct = Mth.clamp(clarity, 0, 100);
        airmass = Math.max(0.0F, effAirmass);

        BlockEntity be = mc.level.getBlockEntity(activePos);
        if (be instanceof ObservatoryTelescopeBlockEntity tele) {
            aimYaw = tele.getAimYawDeg();
            aimPitch = Mth.clamp(Math.min(-28.0F, tele.getAimPitchDeg()), -89.5F, -2.0F);
        } else {
            aimYaw = mc.player.getYRot();
            aimPitch = -38.0F;
        }

        focusRing = 20.0F; // Start out-of-focus so player must actively dial focus
        exposureTicks = 0.0F;
        flashTicks = 0;
        captureCooldownTicks = 0;
        enterGraceTicks = 10;
        mc.setScreen(null);
        mc.mouseHandler.grabMouse();
        AstroSpyglassManager.setTelescopeZoom(20.0F);
        mc.player.playSound(SoundEvents.SPYGLASS_USE, 0.7F, 1.15F);
    }

    public static void exitEyepiece() {
        if (!active) {
            return;
        }
        active = false;
        activePos = null;
        currentTarget = null;
        exposureTicks = 0.0F;
        AstroSpyglassManager.resetZoom();
        Minecraft mc = Minecraft.getInstance();
        if (savedCameraType != null) {
            mc.options.setCameraType(savedCameraType);
            savedCameraType = null;
        }
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.SPYGLASS_STOP_USING, 0.6F, 1.0F);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isObservingPos(@Nullable BlockPos pos) {
        return active && activePos != null && activePos.equals(pos);
    }

    public static float getAimYaw() {
        return aimYaw;
    }

    public static float getAimPitch() {
        return aimPitch;
    }

    /**
     * Computes the exact world-space position of the telescope's front objective lens
     * so the camera sees directly from the telescope mount looking into unobstructed sky.
     */
    public static @NotNull Vec3 getCameraPosition() {
        if (activePos == null) {
            return Vec3.ZERO;
        }
        float scale = apertureTier == 3 ? 1.85F : (apertureTier == 2 ? 1.35F : 1.0F);
        double pivotX = activePos.getX() + 0.5;
        double pivotY = activePos.getY() + 0.92 + 0.32 * scale;
        double pivotZ = activePos.getZ() + 0.5;

        // Forward look vector from telescope aimYaw & aimPitch (Minecraft convention)
        float yawRad = aimYaw * Mth.DEG_TO_RAD;
        float pitchRad = aimPitch * Mth.DEG_TO_RAD;
        float cosPitch = Mth.cos(pitchRad);
        double lookX = -Mth.sin(yawRad) * cosPitch;
        double lookY = -Mth.sin(pitchRad);
        double lookZ = Mth.cos(yawRad) * cosPitch;

        double tubeLen = 1.05 * scale;
        return new Vec3(pivotX + lookX * tubeLen, pivotY + lookY * tubeLen, pivotZ + lookZ * tubeLen);
    }

    /**
     * Routes mouse motion directly to the telescope's equatorial mount with smooth zoom scaling,
     * keeping the player entity's head and body completely locked in place.
     */
    public static void onMouseTurn(double yawDelta, double pitchDelta) {
        if (!active) {
            return;
        }
        double zoom = Math.max(10.0, AstroSpyglassManager.getZoom());
        // Smooth optical slew rate inversely proportional to telescope magnification
        double scale = 0.15 * (10.0 / zoom);
        aimYaw = Mth.wrapDegrees(aimYaw + (float) (yawDelta * scale));
        aimPitch = Mth.clamp(aimPitch + (float) (pitchDelta * scale), -89.5F, -1.5F);
    }

    public static float getMaxZoom() {
        return ObservatoryTelescopeBlockEntity.maxZoomForAperture(apertureTier);
    }

    public static boolean hasSiderealDrive() {
        return auxDriveType == ObservatoryTelescopeBlockEntity.AUX_SIDEREAL_DRIVE;
    }

    public static double getOpticsMagnitudeBonus() {
        double base = switch (apertureTier) {
            case 3 -> -5.0;
            case 2 -> -3.2;
            default -> -1.8;
        };
        double optics = switch (opticsTier) {
            case 3 -> -3.0;
            case 2 -> -1.8;
            case 1 -> -0.9;
            default -> 0.0;
        };
        double ir = auxDriveType == ObservatoryTelescopeBlockEntity.AUX_INFRARED_BOLOMETER ? -1.4 : 0.0;
        return base + optics + ir;
    }

    private static void onClientTick(Minecraft mc) {
        if (!active || activePos == null) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            exitEyepiece();
            return;
        }
        if (player.distanceToSqr(activePos.getCenter()) > 48.0) {
            exitEyepiece();
            return;
        }

        BlockEntity be = mc.level.getBlockEntity(activePos);
        if (be instanceof ObservatoryTelescopeBlockEntity tele) {
            apertureTier = tele.getApertureTier();
            opticsTier = tele.getOpticsTier();
            auxDriveType = tele.getAuxDriveType();
            PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(mc.level);
            airmass = (float) ObservatoryTelescopeBlockEntity.computeEffectiveAirmass(env, activePos.getY());
            long dayTime = mc.level.getDayTime() % 24000L;
            boolean isNight = env.pressureAtm() <= 0.01 || (dayTime >= 12500L && dayTime <= 23500L);
            clarityPct = mc.level.canSeeSky(activePos.above())
                    ? ObservatoryTelescopeBlockEntity.computeOpticalClarityPct(env, activePos.getY(), isNight, opticsTier, auxDriveType)
                    : 0;
        } else {
            exitEyepiece();
            return;
        }

        if (enterGraceTicks > 0) {
            enterGraceTicks--;
        }

        long window = mc.getWindow().getWindow();
        if (enterGraceTicks == 0
                && (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_X)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT))) {
            exitEyepiece();
            return;
        }

        // Safe non-conflicting Focus Ring controls: Z / C, [ / ], or - / = (NEVER Q or E!)
        float focusSpeed = 1.15F;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_Z)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_BRACKET)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_MINUS)) {
            focusRing = Mth.clamp(focusRing - focusSpeed, 0.0F, 100.0F);
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_C)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_BRACKET)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_EQUAL)) {
            focusRing = Mth.clamp(focusRing + focusSpeed, 0.0F, 100.0F);
        }

        // Fine slow-motion keyboard pan (arrow keys slew the telescope mount directly)
        float fineRate = (float) (0.75 / Math.max(1.0, AstroSpyglassManager.getZoom() / 12.0));
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT)) {
            aimYaw = Mth.wrapDegrees(aimYaw - fineRate);
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT)) {
            aimYaw = Mth.wrapDegrees(aimYaw + fineRate);
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_UP)) {
            aimPitch = Mth.clamp(aimPitch - fineRate, -89.5F, -1.5F);
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DOWN)) {
            aimPitch = Mth.clamp(aimPitch + fineRate, -89.5F, -1.5F);
        }

        // Real atmospheric turbulence & seeing shimmer (strongly suppressed by Adaptive Optics)
        if (airmass > 0.05F) {
            float turbAmp = airmass * (auxDriveType == ObservatoryTelescopeBlockEntity.AUX_ADAPTIVE_OPTICS ? 0.008F : 0.032F);
            double t = System.currentTimeMillis() * 0.0045;
            aimYaw = Mth.wrapDegrees(aimYaw + (float) (Math.sin(t * 2.3) * turbAmp));
            aimPitch = Mth.clamp(aimPitch + (float) (Math.cos(t * 1.9) * turbAmp), -89.5F, -1.5F);
        }

        if (flashTicks > 0) {
            flashTicks--;
        }
        if (captureCooldownTicks > 0) {
            captureCooldownTicks--;
        }

        if (++aimSyncCooldown >= 12) {
            aimSyncCooldown = 0;
            ClientPlayNetworking.send(new ObservatoryCapturePayload(
                    activePos, false, "", "", "", 0.0F, 0, aimYaw, aimPitch
            ));
        }

        currentTarget = findTargetInReticle();
        if (currentTarget != null && clarityPct > 0) {
            requiredZoomForTarget = minRequiredZoomFor(currentTarget);
            double curZoom = AstroSpyglassManager.getZoom();
            zoomSufficient = curZoom >= requiredZoomForTarget;

            float targetFocus = idealFocusFor(currentTarget);
            // Atmospheric seeing gently shifts optimal focus unless Adaptive Optics installed
            if (airmass > 0.05F && auxDriveType != ObservatoryTelescopeBlockEntity.AUX_ADAPTIVE_OPTICS) {
                targetFocus += (float) Math.sin(System.currentTimeMillis() * 0.0022) * airmass * 3.2F;
            }
            float focusDiff = Math.abs(focusRing - targetFocus);
            float focusTolerance = opticsTier >= 1 ? 8.0F : 5.5F;
            float focusScore = Mth.clamp(1.0F - (focusDiff / (focusTolerance * 2.1F)), 0.0F, 1.0F);
            currentSharpnessPct = Mth.clamp(Math.round(focusScore * 100.0F * (0.45F + 0.55F * (clarityPct / 100.0F))), 0, 100);

            if (zoomSufficient && currentSharpnessPct >= 78 && captureCooldownTicks == 0) {
                exposureTicks += 1.0F + (currentSharpnessPct - 78) * 0.02F;
                if (exposureTicks >= REQUIRED_EXPOSURE_TICKS) {
                    exposureTicks = 0.0F;
                    flashTicks = 12;
                    captureCooldownTicks = 60;
                    player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.85F, 1.35F);
                    ClientPlayNetworking.send(new ObservatoryCapturePayload(
                            activePos,
                            true,
                            currentTarget.objectId(),
                            currentTarget.displayName(),
                            currentTarget.category(),
                            currentTarget.apparentMagnitude(),
                            currentSharpnessPct,
                            aimYaw,
                            aimPitch
                    ));
                }
            } else {
                exposureTicks = Math.max(0.0F, exposureTicks - 2.4F);
            }
        } else {
            currentSharpnessPct = 0;
            zoomSufficient = true;
            requiredZoomForTarget = 10;
            exposureTicks = Math.max(0.0F, exposureTicks - 3.0F);
        }
    }

    private static int minRequiredZoomFor(LockedSkyObject obj) {
        String id = obj.objectId().toLowerCase(Locale.ROOT);
        if (id.contains("moon") || id.contains("sun") || id.contains("earth")) {
            return 15;
        }
        if ("PLANET".equals(obj.category())) {
            if (id.contains("jupiter") || id.contains("saturn") || id.contains("uranus") || id.contains("neptune")) {
                return 100;
            }
            return 40;
        }
        if ("COMET".equals(obj.category())) {
            return 90;
        }
        if ("NEBULA".equals(obj.category()) || "GALAXY".equals(obj.category()) || "CLUSTER".equals(obj.category())) {
            return 160;
        }
        return 30; // Bright stars
    }

    private static float idealFocusFor(LockedSkyObject obj) {
        String id = obj.objectId().toLowerCase(Locale.ROOT);
        float base;
        if (id.contains("moon") || id.contains("earth")) {
            base = 28.0F;
        } else if ("PLANET".equals(obj.category())) {
            base = (id.contains("jupiter") || id.contains("saturn") || id.contains("uranus") || id.contains("neptune")) ? 76.0F : 52.0F;
        } else if ("COMET".equals(obj.category())) {
            base = 65.0F;
        } else if ("MOON".equals(obj.category())) {
            base = 60.0F;
        } else {
            base = 90.0F;
        }
        return Mth.clamp(base - airmass * 4.5F, 12.0F, 95.0F);
    }

    private static @Nullable LockedSkyObject findTargetInReticle() {
        if (AstroSkyRenderer.getEquatorialMatrix() == null) {
            return null;
        }
        Vector3f look = AstroSpyglassManager.getEquatorialDirection().normalize();
        double zoom = Math.max(10.0, AstroSpyglassManager.getZoom());
        double maxAngleDeg = Mth.clamp(16.0 / Math.sqrt(zoom), 0.55, 4.0);
        double minCos = Math.cos(Math.toRadians(maxAngleDeg));

        double limitingMag = 4.5 + apertureTier * 2.0 + opticsTier * 1.6 + (auxDriveType == ObservatoryTelescopeBlockEntity.AUX_INFRARED_BOLOMETER ? 1.5 : 0.0);

        LockedSkyObject best = null;
        double bestScore = -1.0;

        for (PlanetManager.LiveSkyTarget body : PlanetManager.collectLiveSkyTargets()) {
            if (body.apparentMagnitude() > limitingMag) {
                continue;
            }
            double dot = look.dot(body.equatorialDir());
            if (dot >= minCos) {
                double angDeg = Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0, 1.0)));
                double score = dot + ("PLANET".equals(body.category()) || "COMET".equals(body.category()) ? 0.002 : 0.0);
                if (score > bestScore) {
                    bestScore = score;
                    best = new LockedSkyObject(body.objectId(), body.displayName(), body.category(), body.apparentMagnitude(), (float) angDeg);
                }
            }
        }

        for (DeepskyObject dso : DeepskyManager.getObjects()) {
            if (dso.starPos == null || dso.id == null || dso.id.isEmpty()) {
                continue;
            }
            if (dso.magnitude > limitingMag + 1.5) {
                continue;
            }
            Vector3f dir = new Vector3f(dso.starPos).normalize();
            double dot = look.dot(dir);
            if (dot >= minCos) {
                double angDeg = Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0, 1.0)));
                if (dot > bestScore) {
                    bestScore = dot;
                    String cat = dso.type != null ? dso.type.toUpperCase(Locale.ROOT) : "NEBULA";
                    if (cat.contains("GAL")) {
                        cat = "GALAXY";
                    } else if (cat.contains("CLUST")) {
                        cat = "CLUSTER";
                    } else {
                        cat = "NEBULA";
                    }
                    String nice = dso.id.replace('_', ' ').replace('-', ' ');
                    best = new LockedSkyObject("dso:" + dso.id, nice, cat, (float) dso.magnitude, (float) angDeg);
                }
            }
        }

        for (BrightStarRef star : BRIGHT_STARS) {
            Vector3f dir = AstroStarRenderer.getPosition(star.raDeg(), star.decDeg()).normalize();
            double dot = look.dot(dir);
            if (dot >= minCos) {
                double angDeg = Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0, 1.0)));
                if (dot > bestScore) {
                    bestScore = dot;
                    best = new LockedSkyObject(star.id(), star.name(), "STAR", star.mag(), (float) angDeg);
                }
            }
        }

        return best;
    }

    private static void onRenderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Font font = mc.font;
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int cx = w / 2;
        int cy = h / 2;
        int radius = Math.min(w, h) * 40 / 100;

        // Clean circular optical eyepiece aperture vignette
        int step = 2;
        for (int y = 0; y < h; y += step) {
            int dy = (y + step / 2) - cy;
            int dySq = dy * dy;
            int rSq = radius * radius;
            if (dySq >= rSq) {
                graphics.fill(0, y, w, Math.min(h, y + step), 0xFC030509);
            } else {
                int halfChord = (int) Math.sqrt(rSq - dySq);
                int leftEdge = cx - halfChord;
                int rightEdge = cx + halfChord;
                graphics.fill(0, y, Math.max(0, leftEdge), Math.min(h, y + step), 0xFC030509);
                graphics.fill(Math.min(w, rightEdge), y, w, Math.min(h, y + step), 0xFC030509);
                graphics.fill(Math.max(0, leftEdge - 2), y, Math.max(0, leftEdge + 1), Math.min(h, y + step), 0x9938BDF8);
                graphics.fill(Math.min(w, rightEdge - 1), y, Math.min(w, rightEdge + 2), Math.min(h, y + step), 0x9938BDF8);
            }
        }

        // Optical defocus blur veil when out of focus
        if (currentTarget != null && currentSharpnessPct < 88) {
            int hazeAlpha = Mth.clamp((90 - currentSharpnessPct) * 2, 0, 125);
            graphics.fill(cx - radius / 2, cy - radius / 2, cx + radius / 2, cy + radius / 2, (hazeAlpha << 24) | 0x001B324A);
        }

        if (flashTicks > 0) {
            int a = Mth.clamp(flashTicks * 16, 0, 180);
            graphics.fill(0, 0, w, h, (a << 24) | 0x00B8F5FF);
        }

        // Precision astronomical crosshair reticle
        boolean locked = currentTarget != null && zoomSufficient && currentSharpnessPct >= 78;
        int retCol = locked ? 0xFF34D399 : (currentTarget != null ? 0xFFF59E0B : 0x8838BDF8);
        graphics.fill(cx - 24, cy, cx - 7, cy + 1, retCol);
        graphics.fill(cx + 8, cy, cx + 25, cy + 1, retCol);
        graphics.fill(cx, cy - 24, cx + 1, cy - 7, retCol);
        graphics.fill(cx, cy + 8, cx + 1, cy + 25, retCol);
        graphics.renderOutline(cx - 12, cy - 12, 25, 25, retCol);

        // Top Center Azimuth Compass Strip (from telescope mount orientation)
        float az = Mth.positiveModulo(aimYaw + 180.0F, 360.0F);
        float alt = -aimPitch;
        String compass = String.format(Locale.ROOT, "TELESCOPE CAM  |  AZ %05.1f  |  ALT %+05.1f  |  ZOOM %dx/%dx",
                az, alt, Math.round(AstroSpyglassManager.getZoom()), Math.round(getMaxZoom()));
        int cw = font.width(compass) + 14;
        graphics.fill(cx - cw / 2, 6, cx + cw / 2, 20, 0xCC07101C);
        graphics.renderOutline(cx - cw / 2, 6, cw, 14, 0xFF28435C);
        graphics.drawString(font, compass, cx - font.width(compass) / 2, 9, 0xFFEAF6FF, true);

        // Exposure bar below reticle
        if (exposureTicks > 0.5F && currentTarget != null) {
            int bw = 72;
            int bx = cx - bw / 2;
            int by = cy + 20;
            graphics.fill(bx - 1, by - 1, bx + bw + 1, by + 6, 0xCC06101B);
            int fill = Mth.clamp((int) ((exposureTicks / REQUIRED_EXPOSURE_TICKS) * bw), 0, bw);
            graphics.fill(bx, by, bx + fill, by + 5, 0xFF34D399);
            String cap = "CAPTURING " + Math.min(100, Math.round((exposureTicks / REQUIRED_EXPOSURE_TICKS) * 100.0F)) + "%";
            graphics.drawString(font, cap, cx - font.width(cap) / 2, by + 8, 0xFF68F5B2, true);
        }

        // Top-Left Compact Telemetry Card
        graphics.fill(8, 8, 138, 66, 0xCC07101C);
        graphics.renderOutline(8, 8, 130, 58, 0xFF28435C);
        String apName = apertureTier == 3 ? "3x3 CASSEGRAIN" : (apertureTier == 2 ? "2x2 REFLECTOR" : "1x2 REFRACTOR");
        graphics.drawString(font, apName, 14, 13, 0xFF5CE0FF, true);
        graphics.drawString(font, String.format(Locale.ROOT, "Y=%d | AIR %.2f", activePos != null ? activePos.getY() : 64, airmass), 14, 25, 0xFF9AC2D8, false);
        graphics.drawString(font, "CLARITY: " + clarityPct + "%", 14, 37, clarityPct >= 65 ? 0xFF34D399 : 0xFFF59E0B, false);
        graphics.drawString(font, hasSiderealDrive() ? "SIDEREAL LOCK" : "MANUAL TRACK", 14, 49, hasSiderealDrive() ? 0xFF34D399 : 0xFFF59E0B, false);

        // Top-Right Target Identification Card
        int rx = w - 156;
        graphics.fill(rx, 8, w - 8, 72, 0xCC07101C);
        graphics.renderOutline(rx, 8, 148, 64, retCol);
        if (currentTarget != null) {
            boolean cataloged = ClientResearchCache.astroAtlas().stream().anyMatch(d -> d.objectId().equals(currentTarget.objectId()));
            graphics.drawString(font, font.plainSubstrByWidth(currentTarget.displayName(), 138), rx + 6, 13, 0xFFEAF6FF, true);
            graphics.drawString(font, currentTarget.category() + String.format(Locale.ROOT, " | m=%.1f", currentTarget.apparentMagnitude()), rx + 6, 25, 0xFFC084FC, false);
            if (!zoomSufficient) {
                graphics.drawString(font, "NEED >= " + requiredZoomForTarget + "x ZOOM!", rx + 6, 38, 0xFFFF6B6B, true);
            } else {
                graphics.drawString(font, "FOCUS: " + currentSharpnessPct + "%" + (currentSharpnessPct >= 78 ? " [LOCK]" : " [Z/C]"), rx + 6, 38, currentSharpnessPct >= 78 ? 0xFF34D399 : 0xFFF59E0B, false);
            }
            graphics.drawString(font, cataloged ? "IN ASTRO ATLAS" : "NEW DISCOVERY!", rx + 6, 52, cataloged ? 0xFF9AC2D8 : 0xFF68F5B2, false);
        } else {
            graphics.drawString(font, "SKY SURVEY ACTIVE", rx + 6, 15, 0xFF7BE8FF, true);
            graphics.drawString(font, "Mouse/Arrows: Slew", rx + 6, 30, 0xFF94A3B8, false);
            graphics.drawString(font, "Scroll: Zoom | Z/C: Focus", rx + 6, 42, 0xFF94A3B8, false);
        }

        // Bottom Center Vernier Focus Ring Bar + Safe Controls Hint
        int fbw = 180;
        int fbx = cx - fbw / 2;
        int fby = h - 30;
        graphics.fill(fbx - 4, fby - 10, fbx + fbw + 4, fby + 12, 0xCC07101C);
        graphics.renderOutline(fbx - 4, fby - 10, fbw + 8, 22, 0xFF28435C);
        graphics.drawString(font, "FOCUS [Z / C]  " + Math.round(focusRing) + "%", fbx, fby - 8, 0xFF5CE0FF, false);
        graphics.fill(fbx, fby + 2, fbx + fbw, fby + 8, 0xFF102030);
        if (currentTarget != null && opticsTier >= 1) {
            int idealX = fbx + Math.round((idealFocusFor(currentTarget) / 100.0F) * fbw);
            graphics.fill(Math.max(fbx, idealX - 8), fby + 2, Math.min(fbx + fbw, idealX + 8), fby + 8, 0x4434D399);
        }
        int knobX = fbx + Math.round((focusRing / 100.0F) * (fbw - 4));
        graphics.fill(knobX, fby + 1, knobX + 4, fby + 9, 0xFFFFFFFF);

        String controls = "[MOUSE / ARROWS] Slew   [SCROLL] Zoom   [Z/C] Focus   [ESC / X / SHIFT] Exit";
        graphics.drawString(font, controls, cx - font.width(controls) / 2, h - 12, 0xFFB8D4E8, true);
    }

    private record LockedSkyObject(
            String objectId,
            String displayName,
            String category,
            float apparentMagnitude,
            float angularSeparationDeg
    ) {
    }
}
