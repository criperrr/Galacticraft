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

import dev.galacticraft.api.rocket.LaunchStage;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.entity.vehicle.RocketEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public class CountdownOverlay {
    public static void renderCountdown(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.player.getVehicle() instanceof RocketEntity rocket && rocket.getLaunchStage() == LaunchStage.IGNITED) {
            int count = (int) Math.floor(((float) rocket.getTimeAsState()) / 20.0f);

            final int width = mc.getWindow().getGuiScaledWidth();
            final int height = mc.getWindow().getGuiScaledHeight();

            graphics.pose().pushPose();

            count = 20 - count;

            if (count <= 10) {
                graphics.pose().scale(4.0F, 4.0F, 0.0F);
                graphics.drawString(mc.font, String.valueOf(count), width / 8 - mc.font.width(String.valueOf(count)) / 2, height / 20, FastColor.ARGB32.color(255, 255, 0, 0), false);
            } else {
                graphics.pose().scale(2.0F, 2.0F, 0.0F);
                graphics.drawString(mc.font, String.valueOf(count), width / 4 - mc.font.width(String.valueOf(count)) / 2, height / 8, FastColor.ARGB32.color(255, 255, 0, 0), false);
            }

            graphics.pose().popPose();
            return;
        }

        // Render modular launchpad T-10.00s..T-0.00s countdown banner when near an active Hangar Terminal
        Vec3 playerPos = mc.player.position();
        HangarTerminalBlockEntity nearest = null;
        double nearestDistSq = 64.0 * 64.0;
        for (HangarTerminalBlockEntity terminal : HangarTerminalBlockEntity.getActiveClientTerminals()) {
            if (terminal.isRemoved() || !terminal.isLaunchSequenceActive()) {
                continue;
            }
            double d2 = playerPos.distanceToSqr(Vec3.atCenterOf(terminal.getPadCenterPos()));
            if (d2 < nearestDistSq) {
                nearestDistSq = d2;
                nearest = terminal;
            }
        }
        if (nearest != null) {
            final int width = mc.getWindow().getGuiScaledWidth();
            float secs = nearest.getLaunchCountdownSeconds();
            String mainText = String.format(Locale.ROOT, "T-%.1fs", secs);
            String phaseText = nearest.getLaunchPhase() == HangarTerminalBlockEntity.LaunchSequencePhase.THRUST_BUILDUP
                    ? "THRUST BUILDUP — HOLD-DOWN CLAMPS TRAVADOS"
                    : "WATER DELUGE — PRÉ-VOO EM ANDAMENTO";
            int mainColor = nearest.getLaunchPhase() == HangarTerminalBlockEntity.LaunchSequencePhase.THRUST_BUILDUP
                    ? 0xFFFF5533
                    : 0xFF38BDF8;

            graphics.pose().pushPose();
            graphics.pose().scale(2.5F, 2.5F, 1.0F);
            int cx = Math.round((width / 2.5F) * 0.5F);
            graphics.drawString(mc.font, mainText, cx - mc.font.width(mainText) / 2, 10, mainColor, true);
            graphics.pose().popPose();

            int pw = mc.font.width(phaseText);
            graphics.fill((width - pw) / 2 - 5, 48, (width + pw) / 2 + 5, 60, 0xBB0D1420);
            graphics.drawString(mc.font, phaseText, (width - pw) / 2, 50, 0xFFE2E8F0, false);
        }
    }
}
