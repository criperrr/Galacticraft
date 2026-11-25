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

package dev.galacticraft.mod.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.galacticraft.api.rocket.assembly.RadialBoosterGroup;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import dev.galacticraft.mod.content.entity.vehicle.RocketEntity;
import dev.galacticraft.mod.content.rocket.failure.LaunchFailureEngine;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Displays the rocket's progress to the left of the screen
 */
public class RocketOverlay {
    /**
     * The skin of the player in the rocket.
     * Use one of the defaults if the skin cannot be found.
     */
    private static CompletableFuture<PlayerSkin> playerSkin;

    /**
     * Get the player's spaceship height off ground
     *
     * @param player thePlayer
     * @return position of player's spaceship
     */
    protected static int getPlayerPositionY(Player player) {
        if (player.getVehicle() != null && player.getVehicle() instanceof RocketEntity rocket) {
            return (int) Math.floor(rocket.getY());
        }

        return (int) Math.floor(player.getY());
    }

    public static void onHudRender(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.player.getVehicle() instanceof ModularRocketFlightEntity modularRocket) {
            renderModularRocketHud(graphics, mc, modularRocket);
            return;
        }
        if (mc.player.getVehicle() instanceof RocketEntity rocketEntity) {
            if (playerSkin == null) {
                playerSkin = mc.getSkinManager().getOrLoad(mc.player.getGameProfile());
            }

            Holder<CelestialBody<?, ?>> body = rocketEntity.level().galacticraft$getCelestialBody();
            final int height = mc.getWindow().getGuiScaledHeight();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, body != null ? body.value().display().rocketOverlay() : Constant.CelestialOverlay.EARTH);

            float scale = 0.65F;
            float x0 = 0.0F;
            float x1 = x0 + 20.0F * scale;
            float y0 = height / 2 - 170 / 2;
            float y1 = y0 + 242.0F * scale;
            float u0 = 0.0F;
            float u1 = u0 + 1.0F;
            float v0 = 0.0F;
            float v1 = v0 + 1.0F;

            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            buffer.addVertex(x0, y1, 0.0F).setUv(u0, v1)
                    .addVertex(x1, y1, 0.0F).setUv(u1, v1)
                    .addVertex(x1, y0, 0.0F).setUv(u1, v0)
                    .addVertex(x0, y0, 0.0F).setUv(u0, v0);
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            EntityRenderer<RocketEntity> spaceshipRender = (EntityRenderer<RocketEntity>) mc.getEntityRenderDispatcher().getRenderer(rocketEntity);

            final int h = height / 2 + 60 - (int) Math.floor(getPlayerPositionY(mc.player) / 10.5F);
            scale = 1.0F / 8.0F;
            x0 = 2.5F;
            x1 = x0 + 8.0F;
            y0 = h - 10.0F;
            y1 = y0 + 8.0F;
            u0 = 5.0F * scale;
            u1 = u0 + scale;
            v0 = scale;
            v1 = v0 + scale;

            graphics.pose().pushPose();

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.pose().translate(x0 + 4.0F, y0 + 16.0F, 50.0F);
            graphics.pose().scale(5.0F, 5.0F, 5.0F);
            graphics.pose().mulPose(Axis.XP.rotationDegrees(180.0F));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(90.0F));

            try {
                MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
                spaceshipRender.render(rocketEntity, 0, 0, graphics.pose(), source, LightTexture.FULL_BRIGHT);
                source.endBatch();
            } catch (Exception e) {
                e.printStackTrace();
            }

            RenderSystem.setShaderTexture(0, playerSkin.getNow(DefaultPlayerSkin.get(mc.player.getUUID())).texture());

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 771);

            graphics.pose().translate(0.0F, -7.0F, 60.0F);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.addVertex(x0, y1, 0.0F).setUv(v0, v1)
                    .addVertex(x1, y1, 0.0F).setUv(v1, v1)
                    .addVertex(x1, y0, 0.0F).setUv(v1, v0)
                    .addVertex(x0, y0, 0.0F).setUv(v0, v0);
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            if (mc.player.isModelPartShown(PlayerModelPart.HAT)) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                buffer.addVertex(x0, y1, 0.0F).setUv(u0, v1)
                        .addVertex(x1, y1, 0.0F).setUv(u1, v1)
                        .addVertex(x1, y0, 0.0F).setUv(u1, v0)
                        .addVertex(x0, y0, 0.0F).setUv(u0, v0);
                BufferUploader.drawWithShader(buffer.buildOrThrow());
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            graphics.pose().popPose();
        }
    }

    private static void renderModularRocketHud(GuiGraphics graphics, Minecraft mc, ModularRocketFlightEntity rocket) {
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        var font = mc.font;

        // 1. Left-side vertical altitude progress track (Y=0 .. ESCAPE_HEIGHT=1200)
        int barX = 10;
        int barH = 140;
        int barY = (screenH - barH) / 2;
        int barW = 10;

        graphics.fill(barX - 2, barY - 12, barX + barW + 2, barY + barH + 14, 0xCC0D1420);
        graphics.renderOutline(barX - 2, barY - 12, barW + 4, barH + 26, 0xFF3A5270);
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF162233);

        // Max-Q band (Y=80..220) and Fairing Jettison line (Y=400)
        int maxQTopY = barY + barH - Math.round((220.0F / (float) Constant.ESCAPE_HEIGHT) * barH);
        int maxQBotY = barY + barH - Math.round((80.0F / (float) Constant.ESCAPE_HEIGHT) * barH);
        graphics.fill(barX, maxQTopY, barX + barW, maxQBotY, 0x66FB923C);
        int fairingY = barY + barH - Math.round((400.0F / (float) Constant.ESCAPE_HEIGHT) * barH);
        graphics.fill(barX - 1, fairingY, barX + barW + 1, fairingY + 1, 0xFF38BDF8);

        float altFrac = Mth.clamp((float) rocket.getY() / (float) Constant.ESCAPE_HEIGHT, 0.0F, 1.0F);
        int markerY = barY + barH - Math.round(altFrac * barH);
        graphics.fill(barX, markerY, barX + barW, barY + barH, 0xFF38BDF8);
        graphics.fill(barX - 2, markerY - 1, barX + barW + 2, markerY + 2, 0xFFFACC15);
        graphics.drawString(font, "ORB", barX - 2, barY - 10, 0xFF93C5FD, false);
        graphics.drawString(font, String.format(Locale.ROOT, "%.0fm", rocket.getY()), barX - 2, barY + barH + 4, 0xFFE2E8F0, false);

        // 2. Main aerospace telemetry panel (bottom-left)
        int panelX = 28;
        int panelW = 240;
        int panelH = 88;
        int panelY = screenH - panelH - 12;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC0D1420);
        graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFF3A5270);

        ModularRocketFlightEntity.FlightPhase phase = rocket.getFlightPhase();
        int phaseColor = switch (phase) {
            case LES_ABORT -> 0xFFF87171;
            case HOT_STAGING -> 0xFFFB923C;
            case RETRO_DESCENT -> 0xFF38BDF8;
            default -> 0xFF4ADE80;
        };
        String autoBadge = rocket.isManualOverrideActive()
                ? "MANUAL OVERRIDE"
                : (rocket.isAutopilotEnabled() ? "AUTO: ON" : "AUTO: OFF");
        int autoColor = rocket.isManualOverrideActive() ? 0xFFFACC15 : (rocket.isAutopilotEnabled() ? 0xFF4ADE80 : 0xFFFB923C);

        graphics.drawString(font, "FASE: " + phase.name(), panelX + 6, panelY + 5, phaseColor, false);
        graphics.drawString(font, autoBadge, panelX + panelW - font.width(autoBadge) - 6, panelY + 5, autoColor, false);

        double speedMs = rocket.getDeltaMovement().length() * 20.0;
        double vertMs = rocket.getDeltaMovement().y * 20.0;
        graphics.drawString(
                font,
                String.format(Locale.ROOT, "ALT: %.0fm   VEL: %.1f m/s (Vy %.1f)", rocket.getY(), speedMs, vertMs),
                panelX + 6, panelY + 17, 0xFFE2E8F0, false
        );
        graphics.drawString(
                font,
                String.format(Locale.ROOT, "THR: %.0f%%   TWR: %.2f   G: %.2f",
                        rocket.getThrottle() * 100.0F, rocket.getCurrentTwr(), rocket.getCurrentGLoad()),
                panelX + 6, panelY + 28, 0xFFBAE6FD, false
        );
        graphics.drawString(
                font,
                String.format(Locale.ROOT, "q: %.1f kPa   Pa: %.2f atm   PITCH: %.1f°",
                        rocket.getDynamicPressurePa() / 1000.0F, rocket.getAmbientPressureAtm(), rocket.getPitchDeg()),
                panelX + 6, panelY + 39, 0xFF94A3B8, false
        );

        // Active stage & propellant fuel bar
        RocketAssemblyGraph graph = rocket.getAssemblyGraph();
        RocketStageNode activeStage = graph.activeStage();
        String stageLine = "SEM ESTÁGIO ATIVO";
        float fuelFrac = 0.0F;
        if (activeStage != null) {
            int activeBoosters = 0;
            for (RadialBoosterGroup g : activeStage.radialGroups()) {
                if (!g.decoupled()) {
                    activeBoosters++;
                }
            }
            long cap = activeStage.totalFuelCapacityDroplets();
            long cur = activeStage.totalCurrentFuelDroplets();
            fuelFrac = cap > 0L ? Mth.clamp((float) cur / (float) cap, 0.0F, 1.0F) : 0.0F;
            stageLine = String.format(Locale.ROOT, "STG %d [%s]  Bst:%d  %.0f%%",
                    activeStage.stageIndex(),
                    activeStage.propellantId().toUpperCase(Locale.ROOT),
                    activeBoosters,
                    fuelFrac * 100.0F);
        }
        graphics.drawString(font, stageLine, panelX + 6, panelY + 51, 0xFFFDE047, false);
        int fBarX = panelX + 6;
        int fBarY = panelY + 62;
        int fBarW = panelW - 12;
        graphics.fill(fBarX, fBarY, fBarX + fBarW, fBarY + 6, 0xFF1E293B);
        graphics.fill(fBarX, fBarY, fBarX + Math.round(fBarW * fuelFrac), fBarY + 6, fuelFrac > 0.2F ? 0xFF22C55E : 0xFFEF4444);

        // Compact control legend (adapts between Ascent and Retro-Descent Landing Minigame)
        String keyHint = phase == ModularRocketFlightEntity.FlightPhase.RETRO_DESCENT
                ? "[Esp]+Pot [Shf]-Pot [X]0% [L]Trem [Shf+G]Auto"
                : "[WASD]Man [G]Stage [X]0% [Bksp]LES [L]Trem";
        graphics.drawString(
                font,
                keyHint,
                panelX + 6, panelY + 74, 0xFF94A3B8, false
        );

        // 2b. Interactive Propulsive Landing Minigame Panel (right side of screen during RETRO_DESCENT!)
        if (phase == ModularRocketFlightEntity.FlightPhase.RETRO_DESCENT) {
            renderLandingMinigameHud(graphics, font, rocket, screenW, screenH, vertMs);
        }

        // 3. Top-center emergency alert banner for Weibull failures / Max-Q / Hot-Staging / LES
        String alert = null;
        int alertColor = 0xFFFACC15;
        int failMask = rocket.getActiveFailureMask();
        if (phase == ModularRocketFlightEntity.FlightPhase.LES_ABORT) {
            alert = ">>> LES ABORT ATIVO — ESCAPE DA CÁPSULA <<<";
            alertColor = 0xFFF87171;
        } else if ((failMask & LaunchFailureEngine.FAILURE_CABIN_BREACH) != 0) {
            alert = "ALERTA: DESPRESSURIZAÇÃO DA CABINE!";
            alertColor = 0xFFF87171;
        } else if ((failMask & LaunchFailureEngine.FAILURE_ENGINE_FLAMEOUT) != 0) {
            alert = "ALERTA: ENGINE FLAMEOUT — PRESSIONE BACKSPACE (LES)!";
            alertColor = 0xFFF87171;
        } else if ((failMask & LaunchFailureEngine.FAILURE_VACUUM_NOZZLE) != 0) {
            alert = "AVISO: BOCAL DE VÁCUO EM ALTA PRESSÃO!";
            alertColor = 0xFFFB923C;
        } else if ((failMask & LaunchFailureEngine.FAILURE_FUEL_LEAK) != 0) {
            alert = "AVISO: VAZAMENTO DE PROPELENTE DETECTADO!";
            alertColor = 0xFFFB923C;
        } else if (phase == ModularRocketFlightEntity.FlightPhase.HOT_STAGING) {
            alert = String.format(Locale.ROOT, "HOT-STAGING PRE-IGNITION (%.1fs)", rocket.getHotStageTicks() / 20.0F);
            alertColor = 0xFFFB923C;
        } else if (rocket.getDynamicPressurePa() > 12_000.0F) {
            alert = "MAX-Q — AUTO-THROTTLE ATIVO";
            alertColor = 0xFF38BDF8;
        }
        if (alert != null) {
            int tw = font.width(alert);
            int ax = (screenW - tw) / 2;
            int ay = 16;
            graphics.fill(ax - 6, ay - 3, ax + tw + 6, ay + 12, 0xCC1E1016);
            graphics.drawString(font, alert, ax, ay, alertColor, false);
        }
    }

    private static void renderLandingMinigameHud(
            GuiGraphics graphics,
            net.minecraft.client.gui.Font font,
            ModularRocketFlightEntity rocket,
            int screenW,
            int screenH,
            double vertMs
    ) {
        int lw = 152;
        int lh = 132;
        int lx = screenW - lw - 12;
        int ly = (screenH - lh) / 2;

        graphics.fill(lx, ly, lx + lw, ly + lh, 0xD40B1320);
        graphics.renderOutline(lx, ly, lw, lh, 0xFF38BDF8);

        graphics.drawString(font, "COMPUTADOR DE POUSO", lx + 6, ly + 5, 0xFF38BDF8, false);

        float agl = rocket.getAglBlocks();
        float suicideAlt = rocket.getSuicideBurnAlt();
        float throttle = rocket.getThrottle();
        float hover = rocket.getHoverThrottle();
        double downSpeed = Math.max(0.0, -vertMs);
        double tiltDeg = Math.hypot(rocket.getPitchDeg(), rocket.getRollDeg());

        // 1. Vertical Speed Indicator (VSI) bar with Green (<= 5 m/s), Yellow (5..10 m/s), Red (> 10 m/s) zones
        int vsiX = lx + 10;
        int vsiY = ly + 20;
        int vsiW = 12;
        int vsiH = 82;
        graphics.fill(vsiX, vsiY, vsiX + vsiW, vsiY + vsiH, 0xFF141E2E);
        // Scale: 0 m/s at top -> 20 m/s downward at bottom
        int greenH = Math.round(vsiH * (5.0F / 20.0F));
        int yellowH = Math.round(vsiH * (5.0F / 20.0F));
        graphics.fill(vsiX, vsiY, vsiX + vsiW, vsiY + greenH, 0xAA22C55E);
        graphics.fill(vsiX, vsiY + greenH, vsiX + vsiW, vsiY + greenH + yellowH, 0xAAFB923C);
        graphics.fill(vsiX, vsiY + greenH + yellowH, vsiX + vsiW, vsiY + vsiH, 0xAAEF4444);
        int needleY = vsiY + Math.round(Mth.clamp((float) downSpeed / 20.0F, 0.0F, 1.0F) * (vsiH - 2));
        graphics.fill(vsiX - 2, needleY, vsiX + vsiW + 2, needleY + 2, 0xFFFFFFFF);
        graphics.drawString(font, "VEL", vsiX - 2, vsiY + vsiH + 3, 0xFF94A3B8, false);

        // 2. Throttle bar (0..100%) with golden HOVER sweet-spot marker!
        int thrX = lx + 30;
        int thrY = ly + 20;
        int thrW = 12;
        int thrH = 82;
        graphics.fill(thrX, thrY, thrX + thrW, thrY + thrH, 0xFF141E2E);
        int fillH = Math.round(Mth.clamp(throttle, 0.0F, 1.0F) * thrH);
        int thrColor = throttle <= 0.01F ? 0xFF475569 : (throttle > hover ? 0xFF38BDF8 : 0xFFFACC15);
        graphics.fill(thrX, thrY + thrH - fillH, thrX + thrW, thrY + thrH, thrColor);
        int hoverY = thrY + thrH - Math.round(Mth.clamp(hover, 0.0F, 1.0F) * thrH);
        graphics.fill(thrX - 2, hoverY - 1, thrX + thrW + 2, hoverY + 1, 0xFFFDE047);
        graphics.drawString(font, "POT", thrX - 2, thrY + thrH + 3, 0xFF94A3B8, false);

        // 3. Right telemetry & 4-item landing checklist LEDs
        int tx = lx + 50;
        graphics.drawString(font, String.format(Locale.ROOT, "AGL: %.0fm", agl), tx, ly + 20, 0xFFE2E8F0, false);
        graphics.drawString(font, String.format(Locale.ROOT, "POT: %.0f%%", throttle * 100.0F), tx, ly + 31, 0xFFBAE6FD, false);
        graphics.drawString(font, String.format(Locale.ROOT, "HOV: %.0f%%", hover * 100.0F), tx, ly + 42, 0xFFFDE047, false);

        boolean velOk = downSpeed <= 5.0;
        boolean tiltOk = tiltDeg <= 12.0;
        boolean hasGear = rocket.getAssemblyGraph().hasLandingGear();
        boolean gearOk = !hasGear || rocket.isLandingGearDeployed();

        graphics.drawString(font, velOk ? "[OK] Vel <= 5m/s" : "[!] FREAR AGORA", tx, ly + 56, velOk ? 0xFF4ADE80 : 0xFFF87171, false);
        graphics.drawString(font, tiltOk ? "[OK] Nivelado" : String.format(Locale.ROOT, "[!] Incl %.0f°", tiltDeg), tx, ly + 67, tiltOk ? 0xFF4ADE80 : 0xFFFB923C, false);
        String gearText = !hasGear ? "[--] Sem Trem" : (gearOk ? "[OK] Trem Aberto" : "[!] Trem [L]");
        int gearCol = !hasGear ? 0xFF94A3B8 : (gearOk ? 0xFF4ADE80 : 0xFFFACC15);
        graphics.drawString(font, gearText, tx, ly + 78, gearCol, false);

        // Artificial horizon leveling bar
        int horX = tx;
        int horY = ly + 93;
        int horW = 90;
        graphics.fill(horX, horY, horX + horW, horY + 6, 0xFF1E293B);
        graphics.fill(horX + horW / 2 - 12, horY, horX + horW / 2 + 12, horY + 6, 0x5522C55E);
        float tiltOffset = Mth.clamp(rocket.getPitchDeg() / 22.0F, -1.0F, 1.0F);
        int dotX = horX + horW / 2 + Math.round(tiltOffset * (horW / 2 - 3));
        graphics.fill(dotX - 2, horY - 1, dotX + 2, horY + 7, tiltOk ? 0xFF4ADE80 : 0xFFF87171);

        // Suicide Burn live cue at bottom of minigame panel
        boolean inBurnWindow = downSpeed > 5.0 && agl <= suicideAlt + 18.0;
        String burnCue = inBurnWindow
                ? ">>> QUEIMA DE FRENAGEM! <<<"
                : (velOk && agl < 25.0 ? "TOQUE SUAVE — SEGURE!" : String.format(Locale.ROOT, "Queima ideal: %.0fm AGL", suicideAlt));
        int cueColor = inBurnWindow ? 0xFFF87171 : (velOk && agl < 25.0 ? 0xFF4ADE80 : 0xFF93C5FD);
        graphics.drawString(font, burnCue, lx + 6, ly + lh - 12, cueColor, false);
    }
}
