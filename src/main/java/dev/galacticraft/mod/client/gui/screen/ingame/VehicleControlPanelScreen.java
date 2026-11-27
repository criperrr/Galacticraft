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

import dev.galacticraft.api.vehicle.HardpointType;
import dev.galacticraft.api.vehicle.VehicleDerivedStats;
import dev.galacticraft.api.vehicle.VehicleHardpoint;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.network.c2s.VehicleControlActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Immersive-Vehicles-inspired ({@code GUIPanelGround}) blur-free bottom Cockpit Control &amp;
 * Instrument Console: custom illuminated aerospace rocker switches + live circular 128x128 dial
 * sockets where players can install/swap/eject physical dashboard gauges from their inventory.
 */
public class VehicleControlPanelScreen extends Screen {
    private static final ResourceLocation DIAL_SPEED = Constant.id("textures/entity/instruments/dial_speedometer.png");
    private static final ResourceLocation DIAL_POWER = Constant.id("textures/entity/instruments/dial_power.png");
    private static final ResourceLocation DIAL_ATMO = Constant.id("textures/entity/instruments/dial_atmosphere.png");
    private static final ResourceLocation DIAL_COMPASS = Constant.id("textures/entity/instruments/dial_compass.png");
    private static final ResourceLocation DIAL_HORIZON = Constant.id("textures/entity/instruments/dial_horizon.png");

    private static final List<ResourceLocation> ALL_GAUGES = List.of(
            VehicleRegistry.PART_GAUGE_SPEEDOMETER,
            VehicleRegistry.PART_GAUGE_POWER,
            VehicleRegistry.PART_GAUGE_ATMOSPHERE,
            VehicleRegistry.PART_GAUGE_COMPASS,
            VehicleRegistry.PART_GAUGE_HORIZON
    );

    private final ModularVehicleEntity vehicle;
    private final List<VehicleHardpoint> dashSlots = new ArrayList<>();
    private final int panelW = 344;
    private final int panelH = 92;
    private int panelX;
    private int panelY;
    private @Nullable String activePickerSlot = null;

    public VehicleControlPanelScreen(@NotNull ModularVehicleEntity vehicle) {
        super(Component.translatable("gui.galacticraft.vehicle_panel.title"));
        this.vehicle = vehicle;
    }

    @Override
    protected void init() {
        super.init();
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = this.height - this.panelH - 12;
        this.dashSlots.clear();
        for (VehicleHardpoint hp : this.vehicle.getChassisDefinition().hardpoints()) {
            if (hp.type() == HardpointType.DASHBOARD) {
                this.dashSlots.add(hp);
            }
        }
    }

    private void sendAction(String action, String slot, String value) {
        ClientPlayNetworking.send(new VehicleControlActionPayload(this.vehicle.getId(), action, slot, value));
    }

    private boolean playerHasGauge(@NotNull ResourceLocation gaugeId) {
        Player player = this.minecraft != null ? this.minecraft.player : null;
        if (player == null) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        ItemStack needed = GCItems.createVehiclePartStack(gaugeId);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (!inv.isEmpty() && inv.is(needed.getItem())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. Check floating gauge picker tray first if open
        if (this.activePickerSlot != null) {
            int trayW = ALL_GAUGES.size() * 34 + 38;
            int trayX = this.panelX + this.panelW - trayW - 6;
            int trayY = this.panelY - 40;
            if (mouseX >= trayX && mouseX <= trayX + trayW && mouseY >= trayY && mouseY <= trayY + 36) {
                for (int i = 0; i < ALL_GAUGES.size(); i++) {
                    int gx = trayX + 6 + i * 34;
                    int gy = trayY + 4;
                    if (mouseX >= gx && mouseX <= gx + 28 && mouseY >= gy && mouseY <= gy + 28) {
                        ResourceLocation gid = ALL_GAUGES.get(i);
                        if (this.playerHasGauge(gid)) {
                            this.sendAction("set_gauge", this.activePickerSlot, gid.toString());
                            this.activePickerSlot = null;
                        }
                        return true;
                    }
                }
                // Eject button at right end of tray
                int ex = trayX + 6 + ALL_GAUGES.size() * 34;
                if (mouseX >= ex && mouseX <= ex + 26 && mouseY >= trayY + 4 && mouseY <= trayY + 32) {
                    this.sendAction("set_gauge", this.activePickerSlot, "");
                    this.activePickerSlot = null;
                    return true;
                }
                return true;
            }
            this.activePickerSlot = null;
        }

        // 2. Left aerospace rocker switches (5 switches in 2 rows)
        int swX = this.panelX + 10;
        int swY = this.panelY + 22;
        if (hit(mouseX, mouseY, swX, swY, 54, 24)) {
            this.sendAction("toggle_ignition", "", "");
            return true;
        }
        if (hit(mouseX, mouseY, swX + 58, swY, 54, 24)) {
            this.sendAction("toggle_headlights", "", "");
            return true;
        }
        if (hit(mouseX, mouseY, swX + 116, swY, 54, 24)) {
            this.sendAction("toggle_doors", "", "");
            return true;
        }
        if (hit(mouseX, mouseY, swX, swY + 28, 84, 22)) {
            this.sendAction("toggle_brake", "", "");
            return true;
        }
        if (hit(mouseX, mouseY, swX + 88, swY + 28, 82, 22) && this.vehicle.getCargoInventory().getContainerSize() > 0) {
            this.onClose();
            this.sendAction("open_cargo", "", "");
            return true;
        }

        // 3. Right circular analog dial sockets (Left-Click: open gauge picker; Right-Click: eject gauge)
        int dialAreaX = this.panelX + 190;
        int dialY = this.panelY + 22;
        for (int i = 0; i < this.dashSlots.size(); i++) {
            int dx = dialAreaX + i * 48;
            if (hit(mouseX, mouseY, dx, dialY, 44, 44)) {
                String slotId = this.dashSlots.get(i).id();
                if (button == 1) {
                    this.sendAction("set_gauge", slotId, "");
                } else {
                    this.activePickerSlot = slotId.equals(this.activePickerSlot) ? null : slotId;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.vehicle.isRemoved()) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Deliberately NO renderBackground blur — live unobstructed cockpit view like Immersive Vehicles!
        int x = this.panelX;
        int y = this.panelY;

        // Brushed aerospace dark-titanium console bezel
        g.fill(x, y, x + this.panelW, y + this.panelH, 0xE6161C24);
        g.fill(x + 1, y + 1, x + this.panelW - 1, y + 3, 0xFF2ED1FF);
        g.renderOutline(x, y, this.panelW, this.panelH, 0xFF566882);

        // Header strip: Chassis name + Pressurization badge
        g.drawString(this.font, Component.translatable(this.vehicle.getChassisDefinition().translationKey()), x + 10, y + 8, 0xFFEAF3FF, false);
        String cabTag = this.vehicle.isPressurizedCabin() ? "[O2 PRESSURIZADO]" : "[CABINE ABERTA]";
        int cabCol = this.vehicle.isPressurizedCabin() ? 0xFF5BF5A0 : 0xFFFFB84D;
        g.drawString(this.font, cabTag, x + this.panelW - this.font.width(cabTag) - 10, y + 8, cabCol, false);

        // Divider between switches & analog gauges
        g.fill(x + 184, y + 20, x + 185, y + this.panelH - 18, 0xFF3A485C);

        // Left aerospace switches
        int swX = x + 10;
        int swY = y + 22;
        drawRockerSwitch(g, swX, swY, 54, 24, "MOTOR", this.vehicle.isEngineRunning(), 0xFF3BF078, mouseX, mouseY);
        drawRockerSwitch(g, swX + 58, swY, 54, 24, "FARÓIS", this.vehicle.areHeadlightsOn(), 0xFFFFD84D, mouseX, mouseY);
        drawRockerSwitch(g, swX + 116, swY, 54, 24, "PORTAS", this.vehicle.areDoorsOpen(), 0xFF2ED1FF, mouseX, mouseY);
        drawRockerSwitch(g, swX, swY + 28, 84, 22, "FREIO MÃO", this.vehicle.isParkingBrakeOn(), 0xFFFF5252, mouseX, mouseY);
        boolean hasCargo = this.vehicle.getCargoInventory().getContainerSize() > 0;
        drawRockerSwitch(g, swX + 88, swY + 28, 82, 22, hasCargo ? "ABRIR CARGA" : "SEM CARGA", hasCargo, 0xFF8AA8FF, mouseX, mouseY);

        // Right circular analog instrument sockets
        int dialAreaX = x + 190;
        int dialY = y + 22;
        for (int i = 0; i < this.dashSlots.size(); i++) {
            VehicleHardpoint hp = this.dashSlots.get(i);
            int dx = dialAreaX + i * 48;
            boolean hovered = hit(mouseX, mouseY, dx, dialY, 44, 44);
            boolean active = hp.id().equals(this.activePickerSlot);
            g.fill(dx, dialY, dx + 44, dialY + 44, 0xFF0E131A);
            g.renderOutline(dx, dialY, 44, 44, active ? 0xFF2ED1FF : (hovered ? 0xFFB0C8E6 : 0xFF3E4E64));

            ResourceLocation installed = this.vehicle.getConfiguration().getPart(hp.id());
            if (installed != null) {
                g.blit(dialTextureFor(installed), dx + 2, dialY + 2, 0, 0, 40, 40, 40, 40);
            } else {
                g.drawCenteredString(this.font, "+", dx + 22, dialY + 14, 0xFF5A6D85);
                g.drawCenteredString(this.font, "TELA", dx + 22, dialY + 25, 0xFF5A6D85);
            }
        }

        // Bottom live telemetry / missing parts status bar
        VehicleDerivedStats stats = this.vehicle.getDerivedStats();
        int barY = y + this.panelH - 14;
        g.fill(x + 1, barY - 2, x + this.panelW - 1, y + this.panelH - 1, 0xFF10151C);
        if (!stats.missingRequiredHardpoints().isEmpty()) {
            String warn = "FALTAM PEÇAS: " + String.join(", ", stats.missingRequiredHardpoints());
            if (warn.length() > 54) {
                warn = warn.substring(0, 52) + "..";
            }
            g.drawString(this.font, warn, x + 10, barY, 0xFFFF6E6E, false);
        } else {
            double kmh = Math.abs(this.vehicle.getSyncedSpeed()) * 72.0;
            long energyKj = this.vehicle.getConfiguration().storedEnergy() / 1000L;
            long capKj = Math.max(1L, stats.energyCapacity() / 1000L);
            String telemetry = String.format("ELÉTRICO %.1f km/h  |  BAT %d/%d kJ  |  Clique nos mostradores (Esq: instalar / Dir: remover)", kmh, energyKj, capKj);
            if (this.font.width(telemetry) > this.panelW - 16) {
                telemetry = String.format("ELÉTRICO %.1f km/h | BAT %d/%d kJ | Esq: Tela / Dir: Remover", kmh, energyKj, capKj);
            }
            g.drawString(this.font, telemetry, x + 10, barY, 0xFF9AC2E6, false);
        }

        // Floating Instrument Picker Tray when a dial socket is selected
        if (this.activePickerSlot != null) {
            int trayW = ALL_GAUGES.size() * 34 + 38;
            int trayX = x + this.panelW - trayW - 6;
            int trayY = y - 40;
            g.fill(trayX, trayY, trayX + trayW, trayY + 36, 0xF0141A24);
            g.renderOutline(trayX, trayY, trayW, 36, 0xFF2ED1FF);
            for (int i = 0; i < ALL_GAUGES.size(); i++) {
                ResourceLocation gid = ALL_GAUGES.get(i);
                int gx = trayX + 6 + i * 34;
                int gy = trayY + 4;
                boolean owned = this.playerHasGauge(gid);
                boolean hover = hit(mouseX, mouseY, gx, gy, 28, 28);
                g.fill(gx, gy, gx + 28, gy + 28, owned ? (hover ? 0xFF26384E : 0xFF182230) : 0xFF111419);
                g.renderOutline(gx, gy, 28, 28, owned ? (hover ? 0xFF2ED1FF : 0xFF4D627E) : 0xFF333942);
                g.blit(dialTextureFor(gid), gx + 2, gy + 2, 0, 0, 24, 24, 24, 24);
                if (!owned) {
                    g.fill(gx + 1, gy + 1, gx + 27, gy + 27, 0xA0000000);
                }
            }
            int ex = trayX + 6 + ALL_GAUGES.size() * 34;
            boolean eHover = hit(mouseX, mouseY, ex, trayY + 4, 26, 28);
            g.fill(ex, trayY + 4, ex + 26, trayY + 32, eHover ? 0xFF5A2424 : 0xFF3A1A1A);
            g.renderOutline(ex, trayY + 4, 26, 28, 0xFFE05A5A);
            g.drawCenteredString(this.font, "X", ex + 13, trayY + 14, 0xFFFFB0B0);
        }
    }

    private void drawRockerSwitch(GuiGraphics g, int x, int y, int w, int h, String label, boolean on, int ledColor, int mx, int my) {
        boolean hover = hit(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? 0xFF2A3444 : 0xFF1D2430);
        g.renderOutline(x, y, w, h, on ? ledColor : 0xFF475569);
        // LED status indicator bar along top edge
        g.fill(x + 3, y + 3, x + w - 3, y + 6, on ? ledColor : 0xFF2C3440);
        g.drawCenteredString(this.font, label, x + w / 2, y + (h - 4) / 2 + 1, on ? 0xFFFFFFFF : 0xFF8B99AC);
    }

    private static ResourceLocation dialTextureFor(ResourceLocation id) {
        if (VehicleRegistry.PART_GAUGE_POWER.equals(id)) return DIAL_POWER;
        if (VehicleRegistry.PART_GAUGE_ATMOSPHERE.equals(id)) return DIAL_ATMO;
        if (VehicleRegistry.PART_GAUGE_COMPASS.equals(id)) return DIAL_COMPASS;
        if (VehicleRegistry.PART_GAUGE_HORIZON.equals(id)) return DIAL_HORIZON;
        return DIAL_SPEED;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
