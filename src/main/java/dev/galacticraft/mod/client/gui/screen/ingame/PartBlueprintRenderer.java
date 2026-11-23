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

import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Procedural aerospace CAD / technical blueprint renderer for the Rocket Part Assembler HUD.
 *
 * <p>Replaces the vanilla crafting-table aesthetic with a dark spacecraft engineering workstation
 * and draws detailed background technical silhouettes (capsule, rocket engine bell, cryogenic tank,
 * conical adapter, split payload fairing, utility actuator array) behind the anatomical component
 * slots whenever a {@link RocketPartCategory} schematic is inserted.
 */
@Environment(EnvType.CLIENT)
public final class PartBlueprintRenderer {
    // Main aerospace console colors
    public static final int COLOR_CONSOLE_BG = 0xF5101722;
    public static final int COLOR_CONSOLE_HEADER = 0xFF172538;
    public static final int COLOR_CONSOLE_INV_TRAY = 0xFF0C121B;
    public static final int COLOR_CONSOLE_BORDER = 0xFF3A5270;
    public static final int COLOR_CONSOLE_INNER = 0xFF1B293C;
    public static final int COLOR_ACCENT_CYAN = 0xFF38BDF8;

    // CAD Blueprint canvas colors
    public static final int COLOR_BP_TOP = 0xFF071324;
    public static final int COLOR_BP_BOTTOM = 0xFF0D213B;
    public static final int COLOR_BP_GRID_MINOR = 0xFF102845;
    public static final int COLOR_BP_GRID_MAJOR = 0xFF183B63;
    public static final int COLOR_BP_AXIS = 0x554FC3F7;
    public static final int COLOR_BP_SILHOUETTE_FILL = 0x78174673;
    public static final int COLOR_BP_SILHOUETTE_SHADE = 0x90103254;
    public static final int COLOR_BP_SILHOUETTE_LINE = 0xCC58C8FA;
    public static final int COLOR_BP_DETAIL_LINE = 0x883990C9;
    public static final int COLOR_BP_WARM_ACCENT = 0x88F59E0B;

    // Socket colors
    public static final int COLOR_SOCKET_BG = 0xE00A1422;
    public static final int COLOR_SOCKET_BORDER = 0xFF326A99;
    public static final int COLOR_SOCKET_HIGHLIGHT = 0xFF7DD3FC;
    public static final int COLOR_SOCKET_OK = 0xFF4ADE80;
    public static final int COLOR_SOCKET_WARN = 0xFFFACC15;
    public static final int COLOR_SOCKET_BAD = 0xFFF87171;

    // Blueprint Canvas region relative to main console (leftPos, topPos)
    public static final int CANVAS_X = 25;
    public static final int CANVAS_Y = 18;
    public static final int CANVAS_W = 112;
    public static final int CANVAS_H = 94;

    private PartBlueprintRenderer() {
    }

    /**
     * Renders the complete dark aerospace workstation chassis replacing the legacy compressor texture.
     */
    public static void renderWorkstationChassis(
            @NotNull GuiGraphics graphics,
            @NotNull Font font,
            int leftPos,
            int topPos,
            int width,
            int height,
            @Nullable RocketPartCategory activeCategory
    ) {
        // Outer metallic aerospace frame
        graphics.fill(leftPos, topPos, leftPos + width, topPos + height, COLOR_CONSOLE_BG);
        graphics.renderOutline(leftPos, topPos, width, height, COLOR_CONSOLE_BORDER);
        graphics.renderOutline(leftPos + 1, topPos + 1, width - 2, height - 2, COLOR_CONSOLE_INNER);

        // Header bar
        graphics.fill(leftPos + 2, topPos + 2, leftPos + width - 2, topPos + 15, COLOR_CONSOLE_HEADER);
        graphics.fill(leftPos + 2, topPos + 14, leftPos + width - 2, topPos + 15, COLOR_ACCENT_CYAN);

        // Corner precision rivets
        drawRivet(graphics, leftPos + 4, topPos + 4);
        drawRivet(graphics, leftPos + width - 6, topPos + 4);
        drawRivet(graphics, leftPos + 4, topPos + height - 6);
        drawRivet(graphics, leftPos + width - 6, topPos + height - 6);

        // Energy capacitor housing on left (around MachineLib capacitor at x=7, y=20, w=16, h=48)
        graphics.fill(leftPos + 5, topPos + 18, leftPos + 23, topPos + 70, 0xFF0A1019);
        graphics.renderOutline(leftPos + 5, topPos + 18, 18, 52, 0xFF283C54);
        // Battery slot socket (Slot 0 at 7, 74)
        drawTechSlotSocket(graphics, leftPos + 7, topPos + 74, 0xFF2D4663, false);

        // Right operational column: Schematic dock, Output dock, Scrap dock
        int rightColX = leftPos + 140;
        graphics.fill(rightColX, topPos + 18, leftPos + width - 4, topPos + 112, 0xFF0C1522);
        graphics.renderOutline(rightColX, topPos + 18, width - 144, 94, 0xFF253A54);

        // Slot 1: PLANTA (Schematic Blueprint at 149, 25)
        drawCenteredMicroLabel(graphics, font, "PLANTA", leftPos + 157, topPos + 19,
                activeCategory != null ? 0xFF7DD3FC : 0xFFFACC15);
        drawTechSlotSocket(graphics, leftPos + 149, topPos + 25,
                activeCategory != null ? COLOR_ACCENT_CYAN : 0xFF6482A3, activeCategory != null);

        // Slot 11: PEÇA (Manufactured Output at 149, 63)
        drawCenteredMicroLabel(graphics, font, "PE\u00c7A", leftPos + 157, topPos + 56, 0xFF4ADE80);
        drawTechSlotSocket(graphics, leftPos + 149, topPos + 63, 0xFF2B6E4B, false);

        // Slot 12: SUCATA (Byproduct at 149, 92)
        drawCenteredMicroLabel(graphics, font, "SUCATA", leftPos + 157, topPos + 85, 0xFF8A9CB5);
        drawTechSlotSocket(graphics, leftPos + 149, topPos + 92, 0xFF3A4B60, false);

        // Recessed Player Inventory Tray (y = 115..200)
        graphics.fill(leftPos + 4, topPos + 115, leftPos + width - 4, topPos + height - 4, COLOR_CONSOLE_INV_TRAY);
        graphics.renderOutline(leftPos + 4, topPos + 115, width - 8, height - 119, 0xFF23344A);
        graphics.drawString(font, "INVENT\u00c1RIO", leftPos + 8, topPos + 114, 0xFF6B84A3, false);

        // Dark recessed player inventory slots (3 rows + hotbar at x=8, y=122)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawInventorySlotSocket(graphics, leftPos + 8 + col * 18, topPos + 122 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawInventorySlotSocket(graphics, leftPos + 8 + col * 18, topPos + 180);
        }
    }

    /**
     * Renders the CAD blueprint background grid and the category-specific aerospace part drawing
     * inside the central blueprint bay ({@code CANVAS_X..CANVAS_X+CANVAS_W}).
     */
    public static void renderBlueprintCanvas(
            @NotNull GuiGraphics graphics,
            @NotNull Font font,
            int leftPos,
            int topPos,
            @Nullable RocketPartCategory category
    ) {
        int bx = leftPos + CANVAS_X;
        int by = topPos + CANVAS_Y;
        int bw = CANVAS_W;
        int bh = CANVAS_H;

        // Deep blueprint navy vertical gradient
        graphics.fillGradient(bx, by, bx + bw, by + bh, COLOR_BP_TOP, COLOR_BP_BOTTOM);

        // Minor 8px blueprint grid
        for (int gx = bx + 8; gx < bx + bw; gx += 8) {
            graphics.fill(gx, by + 1, gx + 1, by + bh - 1, COLOR_BP_GRID_MINOR);
        }
        for (int gy = by + 8; gy < by + bh; gy += 8) {
            graphics.fill(bx + 1, gy, bx + bw - 1, gy + 1, COLOR_BP_GRID_MINOR);
        }
        // Major 24px grid lines
        for (int gx = bx + 24; gx < bx + bw; gx += 24) {
            graphics.fill(gx, by + 1, gx + 1, by + bh - 1, COLOR_BP_GRID_MAJOR);
        }
        for (int gy = by + 24; gy < by + bh; gy += 24) {
            graphics.fill(bx + 1, gy, bx + bw - 1, gy + 1, COLOR_BP_GRID_MAJOR);
        }

        // Center dashed vertical engineering axis
        int cx = bx + bw / 2;
        for (int y = by + 4; y < by + bh - 4; y += 6) {
            graphics.fill(cx, y, cx + 1, Math.min(y + 3, by + bh - 4), COLOR_BP_AXIS);
        }

        // Category technical illustration behind the slots
        if (category == null) {
            drawIdleBlueprintDiagram(graphics, font, bx, by, bw, bh);
        } else {
            switch (category) {
                case COMMAND -> drawCommandCapsuleBlueprint(graphics, bx, by, bw, bh);
                case ENGINE -> drawEngineBellBlueprint(graphics, bx, by, bw, bh);
                case TANK -> drawCryoTankBlueprint(graphics, bx, by, bw, bh);
                case ADAPTER -> drawAdapterConeBlueprint(graphics, bx, by, bw, bh);
                case PAYLOAD -> drawPayloadFairingBlueprint(graphics, bx, by, bw, bh);
                case UTILITY -> drawUtilityArrayBlueprint(graphics, bx, by, bw, bh);
            }
            // Subtle CAD blueprint watermark in top-left corner
            String cadTag = "CAD:" + categoryShortCode(category);
            graphics.drawString(font, cadTag, bx + 3, by + 2, 0x777DD3FC, false);
        }

        // Crisp CAD outer frame + glowing corner registration brackets
        graphics.renderOutline(bx, by, bw, bh, 0xFF2B6294);
        drawCornerBracket(graphics, bx, by, 1, 1, COLOR_ACCENT_CYAN);
        drawCornerBracket(graphics, bx + bw - 1, by, -1, 1, COLOR_ACCENT_CYAN);
        drawCornerBracket(graphics, bx, by + bh - 1, 1, -1, COLOR_ACCENT_CYAN);
        drawCornerBracket(graphics, bx + bw - 1, by + bh - 1, -1, -1, COLOR_ACCENT_CYAN);
    }

    /**
     * Renders a sleek aerospace socket frame for a modular component slot and its numbered callout
     * badge ({@code 1..6}) matching the Left Engineering Guide Panel.
     */
    public static void renderBlueprintSlotSocket(
            @NotNull GuiGraphics graphics,
            @NotNull Font font,
            int slotScreenX,
            int slotScreenY,
            int calloutNumber,
            int borderColor,
            boolean highlighted
    ) {
        if (highlighted) {
            graphics.fill(slotScreenX - 3, slotScreenY - 3, slotScreenX + 19, slotScreenY + 19, 0x4438BDF8);
            graphics.renderOutline(slotScreenX - 3, slotScreenY - 3, 22, 22, COLOR_SOCKET_HIGHLIGHT);
        }
        graphics.fill(slotScreenX - 1, slotScreenY - 1, slotScreenX + 17, slotScreenY + 17, COLOR_SOCKET_BG);
        graphics.renderOutline(slotScreenX - 1, slotScreenY - 1, 18, 18, highlighted ? COLOR_SOCKET_HIGHLIGHT : borderColor);

        // Subtle inner corner ticks
        graphics.fill(slotScreenX, slotScreenY, slotScreenX + 2, slotScreenY + 1, 0x557DD3FC);
        graphics.fill(slotScreenX + 14, slotScreenY + 15, slotScreenX + 16, slotScreenY + 16, 0x557DD3FC);

        // Tiny numbered callout badge on upper-left corner of socket
        if (calloutNumber > 0) {
            int bx = slotScreenX - 4;
            int by = slotScreenY - 4;
            int badgeBg = highlighted ? 0xFF0EA5E9 : 0xFF102944;
            int badgeBorder = highlighted ? 0xFFE0F2FE : borderColor;
            graphics.fill(bx, by, bx + 8, by + 9, badgeBg);
            graphics.renderOutline(bx, by, 8, 9, badgeBorder);
            graphics.drawString(font, String.valueOf(calloutNumber), bx + 2, by + 1, 0xFFE8F4FF, false);
        }
    }

    // =========================================================================
    // 6 CATEGORY CAD BLUEPRINT SILHOUETTE DRAWINGS
    // =========================================================================

    /**
     * COMMAND: Apollo/Gemini conical space capsule with nose cap, sloped pressure hull,
     * central cockpit viewport circle, RCS pods, and curved ablative heatshield base.
     */
    private static void drawCommandCapsuleBlueprint(GuiGraphics g, int bx, int by, int bw, int bh) {
        int cx = bx + bw / 2;
        int topY = by + 4;
        int botY = by + bh - 6;

        // 1. Nose docking / parachute cap (y = topY .. topY + 16)
        drawTrapezoidBand(g, cx, topY, topY + 6, 8, 16, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_SILHOUETTE_LINE);
        drawTrapezoidBand(g, cx, topY + 6, topY + 18, 16, 24, COLOR_BP_SILHOUETTE_FILL, COLOR_BP_SILHOUETTE_LINE);

        // 2. Main conical pressure cabin hull (y = topY + 18 .. topY + 68)
        drawTrapezoidBand(g, cx, topY + 18, topY + 68, 24, 50, COLOR_BP_SILHOUETTE_FILL, COLOR_BP_SILHOUETTE_LINE);

        // Internal structural bulkhead ribs
        g.fill(cx - 28, topY + 26, cx + 28, topY + 27, COLOR_BP_DETAIL_LINE);
        g.fill(cx - 42, topY + 56, cx + 42, topY + 57, COLOR_BP_DETAIL_LINE);
        // Diagonal interior frame struts
        drawLine(g, cx - 18, topY + 18, cx - 38, topY + 67, COLOR_BP_DETAIL_LINE);
        drawLine(g, cx + 18, topY + 18, cx + 38, topY + 67, COLOR_BP_DETAIL_LINE);

        // Central cockpit viewport octagonal ring (around slot 4 at cx, by+42)
        int vpY = by + 42;
        drawEllipseOutline(g, cx, vpY, 15, 13, COLOR_BP_SILHOUETTE_LINE);
        drawEllipseOutline(g, cx, vpY, 12, 10, COLOR_BP_DETAIL_LINE);

        // 3. Curved ablative heatshield base (y = topY + 68 .. botY)
        drawTrapezoidBand(g, cx, topY + 68, topY + 76, 50, 44, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_WARM_ACCENT);
        drawTrapezoidBand(g, cx, topY + 76, botY, 44, 28, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_WARM_ACCENT);

        // Side RCS attitude thruster pods
        g.fill(cx - 47, topY + 58, cx - 42, topY + 65, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx + 42, topY + 58, cx + 47, topY + 65, COLOR_BP_SILHOUETTE_LINE);
    }

    /**
     * ENGINE: Rocket engine silhouette with top injector dome, turbopump volute (left),
     * combustion chamber (right), pinched converging-diverging nozzle throat, and flared
     * regenerative-cooled parabolic exhaust bell.
     */
    private static void drawEngineBellBlueprint(GuiGraphics g, int bx, int by, int bw, int bh) {
        int cx = bx + bw / 2;
        int topY = by + 3;
        int botY = by + bh - 4;

        // 1. Top gimbal mount & injector manifold dome
        drawTrapezoidBand(g, cx, topY, topY + 6, 12, 22, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_SILHOUETTE_LINE);
        drawTrapezoidBand(g, cx, topY + 6, topY + 20, 22, 26, COLOR_BP_SILHOUETTE_FILL, COLOR_BP_SILHOUETTE_LINE);

        // 2. Powerhead: left turbopump housing & right combustion chamber
        g.fill(cx - 34, topY + 24, cx - 10, topY + 48, COLOR_BP_SILHOUETTE_SHADE);
        g.renderOutline(cx - 34, topY + 24, 24, 24, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx + 10, topY + 22, cx + 34, topY + 50, COLOR_BP_SILHOUETTE_FILL);
        g.renderOutline(cx + 10, topY + 22, 24, 28, COLOR_BP_SILHOUETTE_LINE);

        // Feed manifold pipes bridging turbopump <-> injector <-> chamber
        drawLine(g, cx - 22, topY + 24, cx - 12, topY + 14, COLOR_BP_SILHOUETTE_LINE);
        drawLine(g, cx + 22, topY + 22, cx + 12, topY + 14, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx - 10, topY + 35, cx + 10, topY + 37, COLOR_BP_SILHOUETTE_LINE);

        // Central cylindrical core neck into throat
        drawTrapezoidBand(g, cx, topY + 20, topY + 52, 16, 18, COLOR_BP_SILHOUETTE_FILL, COLOR_BP_DETAIL_LINE);

        // 3. Pinched converging-diverging thermal throat (hot orange/cyan accent)
        drawTrapezoidBand(g, cx, topY + 52, topY + 62, 18, 14, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_WARM_ACCENT);

        // 4. Wide parabolic flared exhaust bell nozzle (y = topY + 62 .. botY)
        int bellTop = topY + 62;
        int bellH = botY - bellTop;
        for (int dy = 0; dy <= bellH; dy++) {
            float t = (float) dy / Math.max(1, bellH);
            // Parabolic bell contour: fast initial flare, smoother skirt exit
            int halfW = Math.round(14.0F + 34.0F * (float) Math.pow(t, 0.72));
            int y = bellTop + dy;
            g.fill(cx - halfW + 1, y, cx + halfW, y + 1, COLOR_BP_SILHOUETTE_FILL);
            g.fill(cx - halfW, y, cx - halfW + 1, y + 1, COLOR_BP_SILHOUETTE_LINE);
            g.fill(cx + halfW - 1, y, cx + halfW, y + 1, COLOR_BP_SILHOUETTE_LINE);
        }
        // Bell exit lip ring
        g.fill(cx - 48, botY, cx + 48, botY + 1, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx - 46, botY - 3, cx + 46, botY - 2, COLOR_BP_DETAIL_LINE);

        // Regenerative cooling channel ribs radiating down the bell
        for (int rib = -3; rib <= 3; rib++) {
            int xTop = cx + rib * 4;
            int xBot = cx + rib * 13;
            drawLine(g, xTop, bellTop + 1, xBot, botY - 1, COLOR_BP_DETAIL_LINE);
        }
    }

    /**
     * TANK: Pressurized cylindrical cryogenic propellant tank with elliptical domes,
     * twin internal cryo cells, common bulkhead weld seam, downcomer line, and valve manifolds.
     */
    private static void drawCryoTankBlueprint(GuiGraphics g, int bx, int by, int bw, int bh) {
        int cx = bx + bw / 2;
        int topY = by + 5;
        int botY = by + bh - 5;
        int halfW = 42;

        // 1. Upper elliptical pressure dome (y = topY .. topY + 16)
        drawDomeBand(g, cx, topY, 16, halfW, true, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_SILHOUETTE_LINE);
        // Top vent flange
        g.fill(cx - 7, topY - 2, cx + 7, topY + 1, COLOR_BP_SILHOUETTE_LINE);

        // 2. Tall cylindrical pressurized tank wall (y = topY + 16 .. botY - 16)
        int cylTop = topY + 16;
        int cylBot = botY - 16;
        g.fill(cx - halfW + 1, cylTop, cx + halfW, cylBot, COLOR_BP_SILHOUETTE_FILL);
        g.fill(cx - halfW, cylTop, cx - halfW + 1, cylBot, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx + halfW - 1, cylTop, cx + halfW, cylBot, COLOR_BP_SILHOUETTE_LINE);

        // Double-wall SOFI cryogenic insulation liner & center downcomer pipe
        g.fill(cx - halfW + 4, cylTop, cx - halfW + 5, cylBot, COLOR_BP_DETAIL_LINE);
        g.fill(cx + halfW - 5, cylTop, cx + halfW - 4, cylBot, COLOR_BP_DETAIL_LINE);
        g.fill(cx - 2, cylTop + 2, cx + 3, cylBot - 2, COLOR_BP_SILHOUETTE_SHADE);
        g.fill(cx - 2, cylTop + 2, cx - 1, cylBot - 2, COLOR_BP_DETAIL_LINE);
        g.fill(cx + 2, cylTop + 2, cx + 3, cylBot - 2, COLOR_BP_DETAIL_LINE);

        // Common bulkhead weld rings & liquid gauge ticks
        g.fill(cx - halfW, cylTop + 12, cx + halfW, cylTop + 13, COLOR_BP_DETAIL_LINE);
        g.fill(cx - halfW, cylBot - 12, cx + halfW, cylBot - 11, COLOR_BP_DETAIL_LINE);
        for (int ty = cylTop + 18; ty < cylBot - 14; ty += 6) {
            g.fill(cx - 6, ty, cx - 3, ty + 1, COLOR_BP_DETAIL_LINE);
            g.fill(cx + 4, ty, cx + 7, ty + 1, COLOR_BP_DETAIL_LINE);
        }

        // 3. Lower elliptical bulkhead dome (y = cylBot .. botY)
        drawDomeBand(g, cx, cylBot, 16, halfW, false, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_SILHOUETTE_LINE);
        // Bottom propellant feed valves
        g.fill(cx - 18, botY - 1, cx - 12, botY + 2, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx + 12, botY - 1, cx + 18, botY + 2, COLOR_BP_SILHOUETTE_LINE);
    }

    /**
     * ADAPTER: Conical interstage / frustum transition shroud with upper mating collar,
     * sloped orthogrid walls with X-bracing struts, and reinforced lower thrust ring.
     */
    private static void drawAdapterConeBlueprint(GuiGraphics g, int bx, int by, int bw, int bh) {
        int cx = bx + bw / 2;
        int topY = by + 6;
        int botY = by + bh - 6;

        // 1. Upper mating flange collar
        g.fill(cx - 24, topY, cx + 24, topY + 12, COLOR_BP_SILHOUETTE_SHADE);
        g.renderOutline(cx - 24, topY, 48, 12, COLOR_BP_SILHOUETTE_LINE);
        for (int tx = cx - 20; tx <= cx + 20; tx += 8) {
            g.fill(tx, topY + 2, tx + 1, topY + 4, COLOR_BP_DETAIL_LINE);
        }

        // 2. Tapered conical frustum shell
        drawTrapezoidBand(g, cx, topY + 12, botY - 14, 24, 46, COLOR_BP_SILHOUETTE_FILL, COLOR_BP_SILHOUETTE_LINE);

        // Internal orthogrid X-bracing trusses
        drawLine(g, cx - 20, topY + 13, cx + 38, botY - 15, COLOR_BP_DETAIL_LINE);
        drawLine(g, cx + 20, topY + 13, cx - 38, botY - 15, COLOR_BP_DETAIL_LINE);
        g.fill(cx - 34, (topY + botY) / 2, cx + 34, (topY + botY) / 2 + 1, COLOR_BP_DETAIL_LINE);

        // 3. Reinforced lower thrust / separation ring
        g.fill(cx - 46, botY - 14, cx + 46, botY, COLOR_BP_SILHOUETTE_SHADE);
        g.renderOutline(cx - 46, botY - 14, 92, 14, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx - 44, botY - 4, cx + 44, botY - 3, COLOR_BP_WARM_ACCENT);
    }

    /**
     * PAYLOAD: Split aerodynamic clamshell fairing enclosing a central scientific instrument bay
     * suspended on radial shock-mount trusses above a heavy payload adapter deck.
     */
    private static void drawPayloadFairingBlueprint(GuiGraphics g, int bx, int by, int bw, int bh) {
        int cx = bx + bw / 2;
        int topY = by + 4;
        int botY = by + bh - 5;

        // 1. Ogive / tapered upper clamshell halves (split down center seam)
        drawTrapezoidBand(g, cx, topY, topY + 18, 10, 44, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_SILHOUETTE_LINE);
        // Vertical clamshell separation seam
        g.fill(cx - 1, topY + 1, cx + 1, topY + 24, COLOR_BP_AXIS);

        // 2. Hollow cylindrical fairing walls framing open central payload bay
        int bayTop = topY + 18;
        int bayBot = botY - 16;
        g.fill(cx - 44, bayTop, cx - 32, bayBot, COLOR_BP_SILHOUETTE_FILL);
        g.renderOutline(cx - 44, bayTop, 12, bayBot - bayTop, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx + 32, bayTop, cx + 44, bayBot, COLOR_BP_SILHOUETTE_FILL);
        g.renderOutline(cx + 32, bayTop, 12, bayBot - bayTop, COLOR_BP_SILHOUETTE_LINE);

        // Translucent recessed payload cavity & radial shock-mount arms to center core
        g.fill(cx - 32, bayTop, cx + 32, bayBot, 0x380E2A47);
        int midY = (bayTop + bayBot) / 2;
        drawLine(g, cx - 32, bayTop + 6, cx - 12, midY - 6, COLOR_BP_DETAIL_LINE);
        drawLine(g, cx + 32, bayTop + 6, cx + 12, midY - 6, COLOR_BP_DETAIL_LINE);
        drawLine(g, cx - 32, bayBot - 6, cx - 12, midY + 6, COLOR_BP_DETAIL_LINE);
        drawLine(g, cx + 32, bayBot - 6, cx + 12, midY + 6, COLOR_BP_DETAIL_LINE);
        g.renderOutline(cx - 16, midY - 14, 32, 28, COLOR_BP_SILHOUETTE_LINE);

        // 3. Heavy lower payload bulkhead deck
        g.fill(cx - 44, bayBot, cx + 44, botY, COLOR_BP_SILHOUETTE_SHADE);
        g.renderOutline(cx - 44, bayBot, 88, botY - bayBot, COLOR_BP_SILHOUETTE_LINE);
    }

    /**
     * UTILITY: Precision mounting pylon, central control/power core hub, and symmetric left/right
     * deployable wing/actuator/radiator arrays with articulated truss segments.
     */
    private static void drawUtilityArrayBlueprint(GuiGraphics g, int bx, int by, int bw, int bh) {
        int cx = bx + bw / 2;
        int hubY = by + 53;

        // 1. Upper precision attachment pylon
        drawTrapezoidBand(g, cx, by + 6, by + 32, 16, 22, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_SILHOUETTE_LINE);
        // Connecting vertical mast to central hub
        g.fill(cx - 5, by + 32, cx + 5, hubY - 12, COLOR_BP_SILHOUETTE_FILL);
        g.renderOutline(cx - 5, by + 32, 10, (hubY - 12) - (by + 32), COLOR_BP_SILHOUETTE_LINE);

        // 2. Left & right articulated actuator / solar / radiator wing arrays
        int wingTop = hubY - 16;
        int wingBot = hubY + 16;
        // Left wing panel
        g.fill(bx + 6, wingTop, cx - 15, wingBot, COLOR_BP_SILHOUETTE_FILL);
        g.renderOutline(bx + 6, wingTop, (cx - 15) - (bx + 6), wingBot - wingTop, COLOR_BP_SILHOUETTE_LINE);
        // Right wing panel
        g.fill(cx + 15, wingTop, bx + bw - 6, wingBot, COLOR_BP_SILHOUETTE_FILL);
        g.renderOutline(cx + 15, wingTop, (bx + bw - 6) - (cx + 15), wingBot - wingTop, COLOR_BP_SILHOUETTE_LINE);

        // Panel grid ribs & folding hinge segments
        for (int seg = 1; seg <= 3; seg++) {
            int lx = bx + 6 + seg * 9;
            if (lx < cx - 16) {
                g.fill(lx, wingTop + 1, lx + 1, wingBot - 1, COLOR_BP_DETAIL_LINE);
            }
            int rx = cx + 15 + seg * 9;
            if (rx < bx + bw - 7) {
                g.fill(rx, wingTop + 1, rx + 1, wingBot - 1, COLOR_BP_DETAIL_LINE);
            }
        }
        g.fill(bx + 7, hubY, bx + bw - 7, hubY + 1, COLOR_BP_DETAIL_LINE);

        // 3. Central hexagonal / octagonal control & power hub
        drawTrapezoidBand(g, cx, hubY - 17, hubY - 11, 10, 17, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx - 17, hubY - 11, cx + 17, hubY + 11, COLOR_BP_SILHOUETTE_SHADE);
        g.fill(cx - 17, hubY - 11, cx - 16, hubY + 11, COLOR_BP_SILHOUETTE_LINE);
        g.fill(cx + 16, hubY - 11, cx + 17, hubY + 11, COLOR_BP_SILHOUETTE_LINE);
        drawTrapezoidBand(g, cx, hubY + 11, hubY + 17, 17, 10, COLOR_BP_SILHOUETTE_SHADE, COLOR_BP_SILHOUETTE_LINE);

        // Lower stabilizer strut ticks
        drawLine(g, cx - 12, hubY + 17, cx - 24, by + bh - 6, COLOR_BP_DETAIL_LINE);
        drawLine(g, cx + 12, hubY + 17, cx + 24, by + bh - 6, COLOR_BP_DETAIL_LINE);
    }

    /**
     * Idle blueprint backdrop when no modular category schematic is inserted yet.
     */
    private static void drawIdleBlueprintDiagram(GuiGraphics g, Font font, int bx, int by, int bw, int bh) {
        int cx = bx + bw / 2;
        // Faint generic rocket silhouette watermark behind the 3x3 legacy assembly matrix
        drawTrapezoidBand(g, cx, by + 6, by + 22, 4, 18, 0x26174673, 0x444FC3F7);
        g.fill(cx - 18, by + 22, cx + 18, by + bh - 16, 0x22174673);
        g.renderOutline(cx - 18, by + 22, 36, bh - 38, 0x444FC3F7);
        drawTrapezoidBand(g, cx, by + bh - 16, by + bh - 6, 18, 28, 0x26174673, 0x444FC3F7);

        String prompt = "Insira uma PLANTA \u2192";
        g.drawCenteredString(font, prompt, cx, by + 5, 0xCC7DD3FC);
        String sub = "ou receita 3x3";
        g.drawCenteredString(font, sub, cx, by + bh - 11, 0x888A9CB5);
    }

    // =========================================================================
    // PRIMITIVE CAD DRAWING HELPERS
    // =========================================================================

    private static void drawTrapezoidBand(
            GuiGraphics g, int cx, int yTop, int yBot, int halfWTop, int halfWBot, int fillColor, int outlineColor
    ) {
        int h = Math.max(1, yBot - yTop);
        for (int dy = 0; dy <= h; dy++) {
            float t = (float) dy / (float) h;
            int hw = Math.round(Mth.lerp(t, halfWTop, halfWBot));
            int y = yTop + dy;
            if (hw > 1) {
                g.fill(cx - hw + 1, y, cx + hw, y + 1, fillColor);
            }
            g.fill(cx - hw, y, cx - hw + 1, y + 1, outlineColor);
            g.fill(cx + hw - 1, y, cx + hw, y + 1, outlineColor);
        }
        g.fill(cx - halfWTop, yTop, cx + halfWTop, yTop + 1, outlineColor);
        g.fill(cx - halfWBot, yBot, cx + halfWBot, yBot + 1, outlineColor);
    }

    private static void drawDomeBand(
            GuiGraphics g, int cx, int yStart, int height, int maxHalfW, boolean topDome, int fillColor, int outlineColor
    ) {
        for (int dy = 0; dy <= height; dy++) {
            float norm = (float) dy / (float) Math.max(1, height);
            float sinVal = topDome ? (float) Math.sqrt(Math.max(0.0, 1.0 - (1.0 - norm) * (1.0 - norm)))
                    : (float) Math.sqrt(Math.max(0.0, 1.0 - norm * norm));
            int hw = Math.max(4, Math.round(maxHalfW * sinVal));
            int y = yStart + dy;
            g.fill(cx - hw + 1, y, cx + hw, y + 1, fillColor);
            g.fill(cx - hw, y, cx - hw + 1, y + 1, outlineColor);
            g.fill(cx + hw - 1, y, cx + hw, y + 1, outlineColor);
        }
    }

    private static void drawEllipseOutline(GuiGraphics g, int cx, int cy, int rx, int ry, int color) {
        int steps = 48;
        for (int i = 0; i < steps; i++) {
            double angle = (Math.PI * 2.0 * i) / steps;
            int px = cx + (int) Math.round(Math.cos(angle) * rx);
            int py = cy + (int) Math.round(Math.sin(angle) * ry);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int steps = Math.max(1, Math.max(dx, dy));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawCornerBracket(GuiGraphics g, int x, int y, int dirX, int dirY, int color) {
        int len = 5;
        int x1 = dirX > 0 ? x : x - len;
        int x2 = dirX > 0 ? x + len + 1 : x + 1;
        g.fill(x1, y, x2, y + 1, color);
        int y1 = dirY > 0 ? y : y - len;
        int y2 = dirY > 0 ? y + len + 1 : y + 1;
        g.fill(x, y1, x + 1, y2, color);
    }

    private static void drawTechSlotSocket(GuiGraphics g, int sx, int sy, int borderColor, boolean glow) {
        if (glow) {
            g.fill(sx - 2, sy - 2, sx + 18, sy + 18, 0x3338BDF8);
        }
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF0A111C);
        g.renderOutline(sx - 1, sy - 1, 18, 18, borderColor);
    }

    private static void drawInventorySlotSocket(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF0D1624);
        g.fill(sx - 1, sy - 1, sx + 17, sy, 0xFF060B12);
        g.fill(sx - 1, sy - 1, sx, sy + 17, 0xFF060B12);
        g.fill(sx - 1, sy + 16, sx + 17, sy + 17, 0xFF26384F);
        g.fill(sx + 16, sy - 1, sx + 17, sy + 17, 0xFF26384F);
    }

    private static void drawRivet(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 2, y + 2, 0xFF3E5673);
        g.fill(x, y, x + 1, y + 1, 0xFF6B8FB5);
    }

    private static void drawCenteredMicroLabel(GuiGraphics g, Font font, String text, int centerX, int y, int color) {
        g.pose().pushPose();
        float scale = 0.72F;
        g.pose().scale(scale, scale, 1.0F);
        int scaledX = Math.round(centerX / scale);
        int scaledY = Math.round(y / scale);
        g.drawCenteredString(font, text, scaledX, scaledY, color);
        g.pose().popPose();
    }

    private static String categoryShortCode(RocketPartCategory category) {
        return switch (category) {
            case COMMAND -> "CAPSULA";
            case ENGINE -> "MOTOR";
            case TANK -> "TANQUE";
            case ADAPTER -> "ADAPTADOR";
            case PAYLOAD -> "CARGA";
            case UTILITY -> "UTILIDADE";
        };
    }
}
