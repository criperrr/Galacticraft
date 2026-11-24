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

package dev.galacticraft.mod.client.gui.screen.ingame;

import dev.galacticraft.machinelib.client.api.screen.MachineScreen;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.block.entity.machine.ResearchDataLinkBlockEntity;
import dev.galacticraft.mod.network.c2s.ResearchMachineActionPayload;
import dev.galacticraft.mod.research.ClientResearchCache;
import dev.galacticraft.mod.screen.ResearchDataLinkMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 3-Parameter CommNet Orbital Satellite Acquisition &amp; Carrier Phase Lock HUD ({@code 224x210})
 * for {@code RESEARCH_DATA_LINK}. Players must find the relay satellite's Azimuth + Elevation to achieve
 * {@code GAIN >= 85%} while simultaneously tracking the live drifting carrier wave to {@code PHASE >= 88%}.
 */
@Environment(EnvType.CLIENT)
public class ResearchDataLinkScreen extends MachineScreen<ResearchDataLinkBlockEntity, ResearchDataLinkMenu> {
    private static final int SCREEN_W = 224;
    private static final int SCREEN_H = 210;

    private float phaseOffset = 1.9F;
    private long lastMs = 0L;

    public ResearchDataLinkScreen(ResearchDataLinkMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.RESEARCH_DATA_LINK_SCREEN);
        this.imageWidth = SCREEN_W;
        this.imageHeight = SCREEN_H;
        this.capacitorX = 8;
        this.capacitorY = 24;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 32;
        this.inventoryLabelY = 116;
    }

    @Override
    protected void drawTitle(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font, "COMMNET DATA LINK", 8, 6, 0xFFEAF6FF, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;
        long now = System.currentTimeMillis();
        float dt = this.lastMs == 0L ? 0.016F : Mth.clamp((now - this.lastMs) / 1000.0F, 0.001F, 0.1F);
        this.lastMs = now;

        int ch = this.menu.getFrequencyChannel();
        int az = this.menu.getDishAzimuthDeg();
        int el = this.menu.getDishElevationDeg();
        int rfTier = this.menu.getRfFeedTier();
        int modTier = this.menu.getModulatorTier();
        boolean hasFreq = !this.menu.itemStorage.slot(ResearchDataLinkBlockEntity.FREQUENCY_MODULE_SLOT).isEmpty();

        // Live orbital carrier drift (cancelled only by Phase-Locked Loop / Laser Comm!)
        if (modTier >= 1) {
            this.phaseOffset *= Math.max(0.0F, 1.0F - dt * 1.4F);
        } else {
            this.phaseOffset += dt * 0.18F;
        }

        // Hidden target relay satellite azimuth/elevation per frequency channel
        int targetAz = Math.floorMod(ch * 67 + 35, 360);
        int targetEl = 24 + ((ch * 17) % 52);
        float azErr = Math.abs(Mth.wrapDegrees(az - targetAz));
        float elErr = Math.abs(el - targetEl);
        float beamWidth = rfTier >= 1 ? 36.0F : 22.0F;
        int dishGainPct = Mth.clamp(Math.round(100.0F - ((azErr * 0.7F + elErr * 1.3F) / beamWidth) * 35.0F), 0, 100);

        float phaseErr = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(this.phaseOffset)));
        int phasePct = Mth.clamp(Math.round((1.0F - phaseErr / 180.0F) * 100.0F), 0, 100);
        boolean locked = hasFreq && dishGainPct >= 85 && phasePct >= 88;

        ResearchWorkbenchScreen.drawChassis(graphics, x, y, SCREEN_W, SCREEN_H);

        graphics.fill(x + 7, y + 23, x + 25, y + 73, 0xFF0B111A);
        graphics.renderOutline(x + 7, y + 23, 18, 50, 0xFF2E435A);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 7, y + 77, 0xFF2B3D52);

        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 25, 0xFF34D399);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 53, 0xFF38BDF8);
        ResearchWorkbenchScreen.drawSlot(graphics, x + 197, y + 77, 0xFFC084FC);
        if (this.menu.itemStorage.slot(ResearchDataLinkBlockEntity.FREQUENCY_MODULE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "FRQ", x + 198, y + 30, 0xFF5A7A8C, false);
        }
        if (this.menu.itemStorage.slot(ResearchDataLinkBlockEntity.RF_FEED_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "RF ", x + 198, y + 58, 0xFF5A7A8C, false);
        }
        if (this.menu.itemStorage.slot(ResearchDataLinkBlockEntity.MODULATOR_UPGRADE_SLOT).isEmpty()) {
            graphics.drawString(this.font, "MOD", x + 198, y + 82, 0xFF5A7A8C, false);
        }

        ResearchWorkbenchScreen.drawPlayerInvGrid(graphics, x + 31, y + 125);

        int ox = x + 30;
        int oy = y + 24;
        int ow = 160;
        int oh = 92;
        graphics.fill(ox, oy, ox + ow, oy + oh, 0xFF08121B);
        graphics.renderOutline(ox, oy, ow, oh, locked ? 0xFF34D399 : 0xFF28435C);

        String hdr = "CH-" + String.format("%02d", ch) + " GAIN:" + dishGainPct + "% NET:" + ClientResearchCache.networkBonusPct() + "%";
        graphics.drawString(this.font, hdr, ox + 4, oy + 3, dishGainPct >= 85 ? 0xFF34D399 : 0xFF7BE8FF, false);

        int wx = ox + 4;
        int wy = oy + 14;
        int ww = ow - 8;
        int wh = 34;
        graphics.fill(wx, wy, wx + ww, wy + wh, 0xFF050B11);
        graphics.fill(wx, wy + wh / 2, wx + ww, wy + wh / 2 + 1, 0xFF152536);

        double anim = now * 0.0055;
        float ampScale = 0.14F + 0.24F * (dishGainPct / 100.0F);
        for (int i = 0; i < ww; i += 2) {
            double theta = i * 0.10 + anim;
            int refY = wy + wh / 2 + (int) Math.round(Math.sin(theta) * (wh * 0.36));
            int dishY = wy + wh / 2 + (int) Math.round(Math.sin(theta + this.phaseOffset) * (wh * ampScale));
            graphics.fill(wx + i, refY, wx + i + 2, refY + 2, 0xFF34D399);
            graphics.fill(wx + i, dishY, wx + i + 2, dishY + 2, locked ? 0xFF68F5B2 : (dishGainPct >= 85 ? 0xFF38BDF8 : 0xFFF59E0B));
        }

        drawCtrl(graphics, ox + 4, oy + 51, 22, 11, "<AZ", mouseX, mouseY);
        drawCtrl(graphics, ox + 28, oy + 51, 22, 11, "AZ>", mouseX, mouseY);
        drawCtrl(graphics, ox + 54, oy + 51, 22, 11, "-EL", mouseX, mouseY);
        drawCtrl(graphics, ox + 78, oy + 51, 22, 11, "+EL", mouseX, mouseY);
        drawCtrl(graphics, ox + 104, oy + 51, 24, 11, "<PH", mouseX, mouseY);
        drawCtrl(graphics, ox + 130, oy + 51, 24, 11, "PH>", mouseX, mouseY);

        String aimInfo = "AZ:" + az + " EL:" + el + " PH:" + phasePct + "%";
        graphics.drawString(this.font, aimInfo, ox + 4, oy + 65, 0xFF9AC2D8, false);
        drawCtrl(graphics, ox + 126, oy + 63, 14, 10, "-", mouseX, mouseY);
        drawCtrl(graphics, ox + 142, oy + 63, 14, 10, "+", mouseX, mouseY);

        int btnX = ox + 4;
        int btnY = oy + 76;
        int btnW = ow - 8;
        int btnH = 13;
        boolean hov = mouseIn(mouseX, mouseY, btnX, btnY, btnW, btnH);
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, locked ? (hov ? 0xFF15803D : 0xFF166534) : 0xFF1E293B);
        graphics.renderOutline(btnX, btnY, btnW, btnH, locked ? 0xFF34D399 : 0xFF475569);
        String bLbl = !hasFreq ? "INSERT FREQUENCY MODULE"
                : (locked ? "LOCK CARRIER UPLINK"
                : (dishGainPct < 85 ? "AIM DISH AZ/EL (GAIN>=85%)" : "SYNC PHASE WAVE (>=88%)"));
        graphics.drawString(this.font, bLbl, btnX + (btnW - this.font.width(bLbl)) / 2, btnY + 3, locked ? 0xFF68F5B2 : 0xFF94A3B8, false);

        if (this.menu.itemStorage.slot(ResearchDataLinkBlockEntity.RF_FEED_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 197, y + 53, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("RF Feedhorn Bay").withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Low-Noise RF Amp | Deep-Space Ka-Band Feed").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        } else if (this.menu.itemStorage.slot(ResearchDataLinkBlockEntity.MODULATOR_UPGRADE_SLOT).isEmpty()
                && mouseIn(mouseX, mouseY, x + 197, y + 77, 18, 18)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Carrier Modulator Bay").withStyle(ChatFormatting.LIGHT_PURPLE).getVisualOrderText(),
                    Component.literal("Phase-Locked Loop (stops drift) | Laser Comm Transponder").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        }
    }

    private void drawCtrl(GuiGraphics g, int x, int y, int w, int h, String lbl, int mx, int my) {
        boolean hov = mouseIn(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hov ? 0xFF1F4260 : 0xFF122334);
        g.renderOutline(x, y, w, h, hov ? 0xFF38BDF8 : 0xFF2D4660);
        g.drawString(this.font, lbl, x + (w - this.font.width(lbl)) / 2, y + 2, 0xFFEAF6FF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int ox = this.leftPos + 30;
            int oy = this.topPos + 24;
            if (mouseIn(mouseX, mouseY, ox + 4, oy + 51, 22, 11)) {
                sendAim(-12, 0);
                return true;
            }
            if (mouseIn(mouseX, mouseY, ox + 28, oy + 51, 22, 11)) {
                sendAim(12, 0);
                return true;
            }
            if (mouseIn(mouseX, mouseY, ox + 54, oy + 51, 22, 11)) {
                sendAim(0, -6);
                return true;
            }
            if (mouseIn(mouseX, mouseY, ox + 78, oy + 51, 22, 11)) {
                sendAim(0, 6);
                return true;
            }
            if (mouseIn(mouseX, mouseY, ox + 104, oy + 51, 24, 11)) {
                this.phaseOffset -= 0.25F;
                return true;
            }
            if (mouseIn(mouseX, mouseY, ox + 130, oy + 51, 24, 11)) {
                this.phaseOffset += 0.25F;
                return true;
            }
            if (mouseIn(mouseX, mouseY, ox + 126, oy + 63, 14, 10)) {
                ClientPlayNetworking.send(new ResearchMachineActionPayload(this.menu.be.getBlockPos(), ResearchMachineActionPayload.DATA_LINK_ADJUST_CHANNEL, -1));
                return true;
            }
            if (mouseIn(mouseX, mouseY, ox + 142, oy + 63, 14, 10)) {
                ClientPlayNetworking.send(new ResearchMachineActionPayload(this.menu.be.getBlockPos(), ResearchMachineActionPayload.DATA_LINK_ADJUST_CHANNEL, 1));
                return true;
            }
            if (mouseIn(mouseX, mouseY, ox + 4, oy + 76, 152, 13)) {
                int ch = this.menu.getFrequencyChannel();
                int targetAz = Math.floorMod(ch * 67 + 35, 360);
                int targetEl = 24 + ((ch * 17) % 52);
                float azErr = Math.abs(Mth.wrapDegrees(this.menu.getDishAzimuthDeg() - targetAz));
                float elErr = Math.abs(this.menu.getDishElevationDeg() - targetEl);
                float beamWidth = this.menu.getRfFeedTier() >= 1 ? 36.0F : 22.0F;
                int dishGainPct = Mth.clamp(Math.round(100.0F - ((azErr * 0.7F + elErr * 1.3F) / beamWidth) * 35.0F), 0, 100);
                float phaseErr = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(this.phaseOffset)));
                int phasePct = Mth.clamp(Math.round((1.0F - phaseErr / 180.0F) * 100.0F), 0, 100);
                if (dishGainPct >= 85 && phasePct >= 88 && !this.menu.itemStorage.slot(ResearchDataLinkBlockEntity.FREQUENCY_MODULE_SLOT).isEmpty()) {
                    ClientPlayNetworking.send(new ResearchMachineActionPayload(
                            this.menu.be.getBlockPos(),
                            ResearchMachineActionPayload.DATA_LINK_LOCK_PHASE,
                            (dishGainPct + phasePct) / 2
                    ));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendAim(int dyaw, int dpitch) {
        int packed = (dyaw & 0xFFFF) | ((dpitch & 0xFFFF) << 16);
        ClientPlayNetworking.send(new ResearchMachineActionPayload(
                this.menu.be.getBlockPos(),
                ResearchMachineActionPayload.DATA_LINK_ADJUST_AIM,
                packed
        ));
    }
}
