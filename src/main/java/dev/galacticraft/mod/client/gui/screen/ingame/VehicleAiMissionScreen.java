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

import dev.galacticraft.api.vehicle.VehicleDerivedStats;
import dev.galacticraft.api.vehicle.ai.CargoLoadCondition;
import dev.galacticraft.api.vehicle.ai.VehicleAiState;
import dev.galacticraft.api.vehicle.ai.VehicleMissionConfig;
import dev.galacticraft.api.vehicle.ai.VehicleMissionRole;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.network.c2s.VehicleAiConfigPayload;
import dev.galacticraft.mod.network.c2s.VehicleControlActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Blur-free dark-titanium NASA aerospace console for configuring an installed {@code AI_CORE}
 * module across Ground 2.5D, Rotorcraft 3D, and Fixed-Wing 3D autonomous vehicles, with
 * one-click player/vehicle coordinate capture plus manual X/Y/Z coordinate entry for distant bases.
 */
@Environment(EnvType.CLIENT)
public class VehicleAiMissionScreen extends Screen {
    private static final int PANEL_W = 376;
    private static final int PANEL_H = 248;
    private static final int MAX_VISIBLE_WAYPOINTS = 5;

    private final ModularVehicleEntity vehicle;
    private VehicleMissionConfig draft;
    private int panelX;
    private int panelY;
    private EditBox inputX;
    private EditBox inputY;
    private EditBox inputZ;

    public VehicleAiMissionScreen(@NotNull ModularVehicleEntity vehicle) {
        super(Component.translatable("gui.galacticraft.vehicle_ai.title"));
        this.vehicle = vehicle;
        this.draft = vehicle.getMissionConfig();
    }

    @Override
    protected void init() {
        super.init();
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = Math.max(10, this.height - PANEL_H - 12);

        BlockPos defaultPos = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.blockPosition()
                : this.vehicle.blockPosition();
        int coordY = this.panelY + 190;
        this.inputX = this.addRenderableWidget(makeCoordBox(this.panelX + 24, coordY, 54, defaultPos.getX()));
        this.inputY = this.addRenderableWidget(makeCoordBox(this.panelX + 94, coordY, 42, defaultPos.getY()));
        this.inputZ = this.addRenderableWidget(makeCoordBox(this.panelX + 152, coordY, 54, defaultPos.getZ()));
    }

    private EditBox makeCoordBox(int x, int y, int w, int initial) {
        EditBox box = new EditBox(this.font, x, y, w, 15, Component.empty());
        box.setMaxLength(8);
        box.setValue(Integer.toString(initial));
        box.setTextColor(0xFFEAF6FF);
        return box;
    }

    private @Nullable BlockPos parseManualCoordinates() {
        try {
            int cx = Integer.parseInt(this.inputX.getValue().trim());
            int cy = Integer.parseInt(this.inputY.getValue().trim());
            int cz = Integer.parseInt(this.inputZ.getValue().trim());
            return new BlockPos(cx, cy, cz);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.vehicle.isRemoved() || !this.vehicle.getDerivedStats().hasAiCore()) {
            this.onClose();
        }
    }

    private void commitDraft() {
        ClientPlayNetworking.send(new VehicleAiConfigPayload(this.vehicle.getId(), this.draft));
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int x = this.panelX;
        int y = this.panelY;
        Player player = this.minecraft != null ? this.minecraft.player : null;

        // 1. Master autopilot switch, opt-in chunk ticket toggle, and back to cockpit
        int topRowY = y + 24;
        if (hit(mouseX, mouseY, x + 10, topRowY, 116, 22)) {
            this.draft = this.draft.withEnabled(!this.draft.enabled());
            this.commitDraft();
            return true;
        }
        if (hit(mouseX, mouseY, x + 132, topRowY, 120, 22)) {
            this.draft = this.draft.withChunkLoadingEnabled(!this.draft.chunkLoadingEnabled());
            this.commitDraft();
            return true;
        }
        if (hit(mouseX, mouseY, x + 258, topRowY, 108, 22)) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new VehicleControlPanelScreen(this.vehicle));
            }
            return true;
        }

        // 2. Four mission role selector switches
        int roleY = y + 52;
        VehicleMissionRole[] roles = VehicleMissionRole.values();
        for (int i = 0; i < roles.length; i++) {
            int rx = x + 10 + i * 90;
            if (hit(mouseX, mouseY, rx, roleY, 86, 20)) {
                this.draft = this.draft.withRole(roles[i]);
                if (roles[i] == VehicleMissionRole.ESCORT_PATROL && this.draft.ownerUuid().isEmpty() && player != null) {
                    this.draft = this.draft.withOwnerUuid(player.getUUID());
                }
                this.commitDraft();
                return true;
            }
        }

        // 3. Home Charger Pad controls
        int homeY = y + 78;
        if (hit(mouseX, mouseY, x + 200, homeY, 54, 18)) {
            this.draft = this.draft.withHomeChargerPos(this.vehicle.blockPosition());
            this.commitDraft();
            return true;
        }
        if (hit(mouseX, mouseY, x + 258, homeY, 54, 18) && player != null) {
            this.draft = this.draft.withHomeChargerPos(player.blockPosition());
            this.commitDraft();
            return true;
        }
        if (hit(mouseX, mouseY, x + 316, homeY, 50, 18)) {
            this.draft = this.draft.withHomeChargerPos(null);
            this.commitDraft();
            return true;
        }

        // 4. Waypoint list controls (add player pos, add vehicle pos, clear, remove row)
        int wpHeaderY = y + 102;
        if (hit(mouseX, mouseY, x + 166, wpHeaderY, 68, 16) && player != null) {
            this.draft = this.draft.addWaypoint(player.blockPosition());
            this.commitDraft();
            return true;
        }
        if (hit(mouseX, mouseY, x + 238, wpHeaderY, 68, 16)) {
            this.draft = this.draft.addWaypoint(this.vehicle.blockPosition());
            this.commitDraft();
            return true;
        }
        if (hit(mouseX, mouseY, x + 310, wpHeaderY, 56, 16)) {
            this.draft = this.draft.withWaypoints(List.of());
            this.commitDraft();
            return true;
        }

        List<BlockPos> wps = this.draft.waypoints();
        int rowStartY = y + 122;
        int visible = Math.min(MAX_VISIBLE_WAYPOINTS, wps.size());
        for (int i = 0; i < visible; i++) {
            int ry = rowStartY + i * 13;
            if (hit(mouseX, mouseY, x + PANEL_W - 26, ry, 16, 11)) {
                this.draft = this.draft.removeWaypoint(i);
                this.commitDraft();
                return true;
            }
        }

        // 4b. Manual XYZ coordinate entry buttons
        int coordY = y + 190;
        if (hit(mouseX, mouseY, x + 212, coordY, 76, 15)) {
            BlockPos parsed = this.parseManualCoordinates();
            if (parsed != null) {
                this.draft = this.draft.addWaypoint(parsed);
                this.commitDraft();
            }
            return true;
        }
        if (hit(mouseX, mouseY, x + 292, coordY, 74, 15)) {
            BlockPos parsed = this.parseManualCoordinates();
            if (parsed != null) {
                this.draft = this.draft.withHomeChargerPos(parsed);
                this.commitDraft();
            }
            return true;
        }

        // 5. Role-specific parameters bar
        int paramY = y + 212;
        switch (this.draft.role()) {
            case SCIENCE_EXPLORER -> {
                if (hit(mouseX, mouseY, x + 108, paramY, 22, 16)) {
                    this.draft = this.draft.withPatrolRadius(this.draft.patrolRadius() - 16);
                    this.commitDraft();
                    return true;
                }
                if (hit(mouseX, mouseY, x + 174, paramY, 22, 16)) {
                    this.draft = this.draft.withPatrolRadius(this.draft.patrolRadius() + 16);
                    this.commitDraft();
                    return true;
                }
            }
            case CARGO_SHUTTLE -> {
                if (hit(mouseX, mouseY, x + 10, paramY, 180, 16)) {
                    this.draft = this.draft.withCargoLoadCondition(this.draft.cargoLoadCondition().next());
                    this.commitDraft();
                    return true;
                }
            }
            case PASSENGER_TAXI -> {
                if (hit(mouseX, mouseY, x + 10, paramY, 110, 16)) {
                    this.draft = this.draft.withAutoBoardNpcs(!this.draft.autoBoardNpcs());
                    this.commitDraft();
                    return true;
                }
                if (hit(mouseX, mouseY, x + 126, paramY, 86, 16)) {
                    ClientPlayNetworking.send(new VehicleControlActionPayload(this.vehicle.getId(), "ai_depart_now", "", ""));
                    return true;
                }
            }
            case ESCORT_PATROL -> {
                if (hit(mouseX, mouseY, x + 10, paramY, 92, 16) && player != null) {
                    this.draft = this.draft.withOwnerUuid(player.getUUID());
                    this.commitDraft();
                    return true;
                }
                if (hit(mouseX, mouseY, x + 108, paramY, 20, 16)) {
                    this.draft = this.draft.withEscortOffsetBlocks(this.draft.escortOffsetBlocks() - 2.0);
                    this.commitDraft();
                    return true;
                }
                if (hit(mouseX, mouseY, x + 168, paramY, 20, 16)) {
                    this.draft = this.draft.withEscortOffsetBlocks(this.draft.escortOffsetBlocks() + 2.0);
                    this.commitDraft();
                    return true;
                }
            }
        }

        // Fixed-wing runway heading controls on right half of parameter bar
        if (this.vehicle.isFixedWing()) {
            if (hit(mouseX, mouseY, x + 220, paramY, 24, 16)) {
                this.draft = this.draft.withRunwayHeadingDeg(this.draft.runwayHeadingDeg() - 45);
                this.commitDraft();
                return true;
            }
            if (hit(mouseX, mouseY, x + 292, paramY, 24, 16)) {
                this.draft = this.draft.withRunwayHeadingDeg(this.draft.runwayHeadingDeg() + 45);
                this.commitDraft();
                return true;
            }
            if (hit(mouseX, mouseY, x + 320, paramY, 46, 16)) {
                this.draft = this.draft.withRunwayHeadingDeg(Math.round(this.vehicle.getYRot()));
                this.commitDraft();
                return true;
            }
        }

        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Blur-free aerospace console — unobstructed live view of the world and vehicle
        int x = this.panelX;
        int y = this.panelY;

        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xEA121821);
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + 3, 0xFF2ED1FF);
        g.renderOutline(x, y, PANEL_W, PANEL_H, 0xFF566B85);

        // Header: AI Core title + Navigator Domain + Live FSM State Badge
        String navTag = this.vehicle.isFixedWing() ? "FIXED-WING 3D" : (this.vehicle.isRotorcraft() ? "ROTOR 3D" : "GROUND 2.5D");
        VehicleAiState aiState = this.vehicle.getAiState();
        g.drawString(this.font, "AI CORE // " + navTag, x + 10, y + 8, 0xFFEAF6FF, false);

        String stateBadge = "[" + aiState.name() + "]";
        int stateColor = switch (aiState) {
            case WORKING, BOARDING -> 0xFF5BF5A0;
            case TRANSIT, PLANNING -> 0xFF2ED1FF;
            case STUCK_RECOVERY, HOVER_HOLD, GO_AROUND -> 0xFFFFB84D;
            case RETURN_TO_BASE, CHARGING -> 0xFFFF944D;
            case IDLE -> 0xFF8C9EB3;
        };
        g.drawString(this.font, stateBadge, x + PANEL_W - this.font.width(stateBadge) - 10, y + 8, stateColor, false);

        // Row 1: Master Autopilot, Chunk Ticket toggle, Back to Cockpit
        int topRowY = y + 24;
        drawSwitch(g, x + 10, topRowY, 116, 22, this.draft.enabled() ? "AI AUTO: ON" : "AI AUTO: OFF", this.draft.enabled(), 0xFF3BF078, mouseX, mouseY);
        drawSwitch(g, x + 132, topRowY, 120, 22, this.draft.chunkLoadingEnabled() ? "CHUNK LOAD: ON" : "CHUNK LOAD: OFF", this.draft.chunkLoadingEnabled(), 0xFFFFC14D, mouseX, mouseY);
        drawSwitch(g, x + 258, topRowY, 108, 22, "COCKPIT [P]", false, 0xFF7CE8FF, mouseX, mouseY);

        // Row 2: 4 Mission Role switches
        int roleY = y + 52;
        VehicleMissionRole[] roles = VehicleMissionRole.values();
        String[] roleLabels = {"CIÊNCIA", "CARGA", "TÁXI", "ESCOLTA"};
        int[] roleColors = {0xFF2ED1FF, 0xFFFFC14D, 0xFF5BF5A0, 0xFFFF6E9C};
        for (int i = 0; i < roles.length; i++) {
            int rx = x + 10 + i * 90;
            drawSwitch(g, rx, roleY, 86, 20, roleLabels[i], this.draft.role() == roles[i], roleColors[i], mouseX, mouseY);
        }

        // Row 3: Home Charger Pad
        int homeY = y + 78;
        String homeTxt = this.draft.homeChargerPos()
                .map(p -> String.format("BASE PAD: %d, %d, %d", p.getX(), p.getY(), p.getZ()))
                .orElse("BASE PAD: [NÃO DEFINIDA]");
        g.drawString(this.font, homeTxt, x + 10, homeY + 5, 0xFFB8D2EC, false);
        drawMiniButton(g, x + 200, homeY, 54, 18, "POS VEH", mouseX, mouseY);
        drawMiniButton(g, x + 258, homeY, 54, 18, "POS JOG", mouseX, mouseY);
        drawMiniButton(g, x + 316, homeY, 50, 18, "LIMPAR", mouseX, mouseY);

        // Row 4: Waypoints list box
        int wpHeaderY = y + 102;
        String wpTitle = switch (this.draft.role()) {
            case CARGO_SHUTTLE -> "ROTAS DE CARGA (" + this.draft.waypoints().size() + ")";
            case PASSENGER_TAXI -> "ESTAÇÕES TÁXI (" + this.draft.waypoints().size() + ")";
            case SCIENCE_EXPLORER -> "ALVOS CIÊNCIA (" + this.draft.waypoints().size() + ")";
            case ESCORT_PATROL -> "ESCOLTA ATIVA";
        };
        g.drawString(this.font, wpTitle, x + 10, wpHeaderY + 4, 0xFF7CE8FF, false);
        drawMiniButton(g, x + 166, wpHeaderY, 68, 16, "+ POS JOG", mouseX, mouseY);
        drawMiniButton(g, x + 238, wpHeaderY, 68, 16, "+ POS VEH", mouseX, mouseY);
        drawMiniButton(g, x + 310, wpHeaderY, 56, 16, "ZERAR", mouseX, mouseY);

        int listBoxY = y + 120;
        g.fill(x + 10, listBoxY, x + PANEL_W - 10, listBoxY + 66, 0xFF0C1017);
        g.renderOutline(x + 10, listBoxY, PANEL_W - 20, 66, 0xFF344459);

        List<BlockPos> wps = this.draft.waypoints();
        if (wps.isEmpty()) {
            String emptyHint = this.draft.role() == VehicleMissionRole.SCIENCE_EXPLORER
                    ? "Sem pontos manuais: patrulha espiral automática (" + this.draft.patrolRadius() + "m)"
                    : (this.draft.role() == VehicleMissionRole.ESCORT_PATROL
                    ? "Segue o dono vinculado mantendo distância segura"
                    : "Adicione 2+ pontos (Origem -> Destino) via botão ou XYZ");
            g.drawString(this.font, emptyHint, x + 16, listBoxY + 28, 0xFF70859E, false);
        } else {
            int visible = Math.min(MAX_VISIBLE_WAYPOINTS, wps.size());
            for (int i = 0; i < visible; i++) {
                BlockPos wp = wps.get(i);
                int ry = listBoxY + 2 + i * 13;
                String tag = waypointLabel(this.draft.role(), i);
                String line = String.format("%s -> [%d, %d, %d]", tag, wp.getX(), wp.getY(), wp.getZ());
                g.drawString(this.font, line, x + 16, ry + 2, 0xFFD9ECFF, false);
                drawMiniButton(g, x + PANEL_W - 26, ry, 16, 11, "X", mouseX, mouseY);
            }
        }

        // Row 4b: Manual XYZ coordinate fields & action buttons
        int coordY = y + 190;
        g.drawString(this.font, "X:", x + 12, coordY + 4, 0xFF7CE8FF, false);
        g.drawString(this.font, "Y:", x + 82, coordY + 4, 0xFF7CE8FF, false);
        g.drawString(this.font, "Z:", x + 140, coordY + 4, 0xFF7CE8FF, false);
        drawMiniButton(g, x + 212, coordY, 76, 15, "+ ADD XYZ", mouseX, mouseY);
        drawMiniButton(g, x + 292, coordY, 74, 15, "BASE XYZ", mouseX, mouseY);

        // Row 5: Role-specific controls + Fixed-Wing runway heading
        int paramY = y + 212;
        switch (this.draft.role()) {
            case SCIENCE_EXPLORER -> {
                g.drawString(this.font, "RAIO PATRULHA:", x + 10, paramY + 4, 0xFF9AC2E6, false);
                drawMiniButton(g, x + 108, paramY, 22, 16, "-", mouseX, mouseY);
                g.drawCenteredString(this.font, this.draft.patrolRadius() + "m", x + 152, paramY + 4, 0xFFFFFFFF);
                drawMiniButton(g, x + 174, paramY, 22, 16, "+", mouseX, mouseY);
            }
            case CARGO_SHUTTLE -> {
                String cond = "PARTIDA: " + formatCondition(this.draft.cargoLoadCondition());
                drawMiniButton(g, x + 10, paramY, 180, 16, cond, mouseX, mouseY);
            }
            case PASSENGER_TAXI -> {
                drawSwitch(g, x + 10, paramY, 110, 16, this.draft.autoBoardNpcs() ? "NPC AUTO: ON" : "NPC AUTO: OFF", this.draft.autoBoardNpcs(), 0xFF5BF5A0, mouseX, mouseY);
                drawSwitch(g, x + 126, paramY, 86, 16, "PARTIR AGORA", this.vehicle.getAiState() == VehicleAiState.BOARDING, 0xFF2ED1FF, mouseX, mouseY);
            }
            case ESCORT_PATROL -> {
                drawMiniButton(g, x + 10, paramY, 92, 16, "VINCULAR A MIM", mouseX, mouseY);
                drawMiniButton(g, x + 108, paramY, 20, 16, "-", mouseX, mouseY);
                g.drawCenteredString(this.font, String.format(Locale.ROOT, "%.0fm", this.draft.escortOffsetBlocks()), x + 148, paramY + 4, 0xFFFFFFFF);
                drawMiniButton(g, x + 168, paramY, 20, 16, "+", mouseX, mouseY);
            }
        }

        if (this.vehicle.isFixedWing()) {
            drawMiniButton(g, x + 220, paramY, 24, 16, "-45", mouseX, mouseY);
            g.drawCenteredString(this.font, String.format("PISTA %03d°", this.draft.runwayHeadingDeg()), x + 268, paramY + 4, 0xFF7CE8FF);
            drawMiniButton(g, x + 292, paramY, 24, 16, "+45", mouseX, mouseY);
            drawMiniButton(g, x + 320, paramY, 46, 16, "PROA", mouseX, mouseY);
        }

        // Bottom telemetry strip
        VehicleDerivedStats stats = this.vehicle.getDerivedStats();
        long energy = this.vehicle.getConfiguration().storedEnergy();
        long cap = Math.max(1L, stats.energyCapacity());
        int batPct = Math.round(Mth.clamp((float) energy / (float) cap, 0.0F, 1.0F) * 100.0F);
        String ticketStr = this.vehicle.isChunkTicketActive() ? "TICKET ATIVO" : "SEM TICKET";
        String footer = String.format("BAT %d%% | DADOS: %d | %s", batPct, this.vehicle.getCollectedScienceDataCount(), ticketStr);
        g.drawString(this.font, footer, x + 10, y + PANEL_H - 12, 0xFF8AA4BF, false);

        // Render EditBoxes last on top of dark console without background blur
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Deliberately empty: zero vanilla dark blur so players keep full visibility of the vehicle
    }

    private static String waypointLabel(VehicleMissionRole role, int index) {
        return switch (role) {
            case CARGO_SHUTTLE -> index == 0 ? "Origem" : "Destino #" + index;
            case PASSENGER_TAXI -> "Estação " + (index + 1);
            case SCIENCE_EXPLORER -> "Alvo #" + (index + 1);
            case ESCORT_PATROL -> "Ponto #" + (index + 1);
        };
    }

    private static String formatCondition(CargoLoadCondition c) {
        return switch (c) {
            case INACTIVITY_60_TICKS -> "INATIV. 60t";
            case FULL -> "CHEIO";
            case EMPTY -> "VAZIO";
            case TIMER_200_TICKS -> "TIMER 200t";
        };
    }

    private void drawSwitch(GuiGraphics g, int x, int y, int w, int h, String label, boolean on, int ledColor, int mx, int my) {
        boolean hover = hit(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? 0xFF283446 : 0xFF1A222E);
        g.renderOutline(x, y, w, h, on ? ledColor : 0xFF475569);
        g.fill(x + 2, y + 2, x + w - 2, y + 4, on ? ledColor : 0xFF2B3544);
        g.drawCenteredString(this.font, label, x + w / 2, y + (h - 6) / 2 + 1, on ? 0xFFFFFFFF : 0xFF94A3B8);
    }

    private void drawMiniButton(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = hit(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? 0xFF2A3A50 : 0xFF192230);
        g.renderOutline(x, y, w, h, hover ? 0xFF2ED1FF : 0xFF475A72);
        g.drawCenteredString(this.font, label, x + w / 2, y + (h - 7) / 2, hover ? 0xFFFFFFFF : 0xFFC7D9EC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
