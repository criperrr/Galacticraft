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
import dev.galacticraft.mod.content.block.entity.machine.AerospaceRdTerminalBlockEntity;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.network.c2s.ResearchTerminalActionPayload;
import dev.galacticraft.mod.research.AstroDiscovery;
import dev.galacticraft.mod.research.ClientResearchCache;
import dev.galacticraft.mod.research.ExperimentRecord;
import dev.galacticraft.mod.research.ResearchBranch;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchDomain;
import dev.galacticraft.mod.research.ResearchMilestone;
import dev.galacticraft.mod.research.ResearchNode;
import dev.galacticraft.mod.screen.AerospaceRdTerminalMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wide KSP-style Aerospace R&amp;D Command Terminal HUD ({@code 416x254}):
 * <ul>
 *   <li><b>Tab 0 — TECH TREE:</b> Interactive draggable <b>Pan &amp; Zoom</b> left-to-right
 *       Era 1 &rarr; Era 6 DAG showing primary Science Points ({@code SP}), secondary Domain costs,
 *       Milestone stars, luminous prerequisite bus lines, and a full-height right Engineering
 *       Inspector with {@code RESEARCH} &amp; {@code ETCH SCHEMATIC} actions.</li>
 *   <li><b>Tab 1 — SCIENCE ARCHIVES:</b> KSP-style filterable archive of every planetary experiment,
 *       showing collected vs remaining {@code SP} bars and diminishing-return yields.</li>
 *   <li><b>Tab 2 — ASTRO ATLAS &amp; COMMNET:</b> First-person Observatory Telescope photo catalog
 *       (planets, moons, comets, stars, nebulae, galaxies) alongside active interplanetary
 *       {@code RESEARCH_DATA_LINK} relays and live telemetry logs.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class AerospaceRdTerminalScreen extends MachineScreen<AerospaceRdTerminalBlockEntity, AerospaceRdTerminalMenu> {
    private static final int SCREEN_W = 416;
    private static final int SCREEN_H = 254;

    private static final int VIEW_TREE = 0;
    private static final int VIEW_ARCHIVES = 1;
    private static final int VIEW_ATLAS = 2;

    // Main left KSP viewport (scissored)
    private static final int VP_X = 6;
    private static final int VP_Y = 24;
    private static final int VP_W = 276;
    private static final int VP_H = 142;

    // Full-height right Engineering Inspector panel
    private static final int INS_X = 286;
    private static final int INS_Y = 24;
    private static final int INS_W = 124;
    private static final int INS_H = 224;

    // World-space Radial Constellation Tech Tree cell spacing (generous & airy)
    private static final float COL_STEP = 68.0F;
    private static final float ROW_STEP = 56.0F;
    private static final int NODE_W = 34;
    private static final int NODE_H = 28;
    private static final float DEFAULT_ZOOM = 0.68F;

    private static final String[] PLANET_FILTERS = {"ALL", "Earth", "Moon", "Mars", "Venus", "Mercury", "Orbit", "Outer"};

    private int activeView = VIEW_TREE;
    private int branchFilterIdx = 0; // 0 = ALL, 1..5 = ResearchBranch.values()[idx - 1]
    private @Nullable ResourceLocation selectedNodeId = ResearchCatalog.EARTH_PROTOTYPING;

    // Pan & Zoom camera centered directly on (0, 0) core hub
    private float zoom = DEFAULT_ZOOM;
    private float panX = VP_W * 0.5F - (NODE_W * 0.5F) * DEFAULT_ZOOM;
    private float panY = VP_H * 0.5F - (NODE_H * 0.5F) * DEFAULT_ZOOM;
    private boolean draggingTree = false;
    private double dragStartMouseX;
    private double dragStartMouseY;
    private float dragStartPanX;
    private float dragStartPanY;

    // Archives filter + scroll
    private int planetFilterIdx = 0;
    private int archiveScroll = 0;

    // Astro Atlas selection
    private int selectedPlanetIdx = 0;
    private int atlasScroll = 0;

    public AerospaceRdTerminalScreen(AerospaceRdTerminalMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.AEROSPACE_RD_TERMINAL_SCREEN);
        this.imageWidth = SCREEN_W;
        this.imageHeight = SCREEN_H;
        this.capacitorX = 176;
        this.capacitorY = 172;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 162;
    }

    @Override
    protected void drawTitle(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font, "R&D CENTER", 8, 6, 0xFFEAF6FF, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int x = this.leftPos;
        int y = this.topPos;
        long timeMs = System.currentTimeMillis();

        // Full procedural aerospace dark-slate chassis (cleanly covers 416x254 beyond 256px base blit)
        graphics.fill(x, y, x + SCREEN_W, y + SCREEN_H, 0xFF1A222D);
        graphics.renderOutline(x, y, SCREEN_W, SCREEN_H, 0xFF475B73);
        graphics.renderOutline(x + 1, y + 1, SCREEN_W - 2, SCREEN_H - 2, 0xFF0E141C);

        // Top Header Bar
        graphics.fill(x + 2, y + 2, x + SCREEN_W - 2, y + 21, 0xFF101822);
        graphics.fill(x + 2, y + 21, x + SCREEN_W - 2, y + 22, 0xFF28435C);

        // Science Points (SP) glowing KSP pill
        int sp = ClientResearchCache.sciencePoints();
        int spPillX = x + 74;
        graphics.fill(spPillX, y + 4, spPillX + 66, y + 18, 0xFF0A2A3B);
        graphics.renderOutline(spPillX, y + 4, 66, 14, 0xFF38BDF8);
        String spTxt = sp + " SP";
        graphics.drawString(this.font, spTxt, spPillX + (66 - this.font.width(spTxt)) / 2, y + 7, 0xFF7BE8FF, true);

        // 3 Top Mode Tabs
        drawTopTab(graphics, x + 146, y + 4, 72, 14, "TECH TREE", this.activeView == VIEW_TREE, 0xFF38BDF8, mouseX, mouseY);
        drawTopTab(graphics, x + 222, y + 4, 92, 14, "SCIENCE ARCHIVES", this.activeView == VIEW_ARCHIVES, 0xFF34D399, mouseX, mouseY);
        drawTopTab(graphics, x + 318, y + 4, 92, 14, "ATLAS & COMMNET", this.activeView == VIEW_ATLAS, 0xFFC084FC, mouseX, mouseY);

        // Main left viewport frame
        int vpX = x + VP_X;
        int vpY = y + VP_Y;
        graphics.fill(vpX, vpY, vpX + VP_W, vpY + VP_H, 0xFF081019);
        graphics.renderOutline(vpX, vpY, VP_W, VP_H, 0xFF28435C);

        switch (this.activeView) {
            case VIEW_TREE -> renderKspTechTree(graphics, vpX, vpY, VP_W, VP_H, mouseX, mouseY, timeMs);
            case VIEW_ARCHIVES -> renderScienceArchives(graphics, vpX, vpY, VP_W, VP_H, mouseX, mouseY);
            case VIEW_ATLAS -> renderAtlasAndCommNet(graphics, vpX, vpY, VP_W, VP_H, mouseX, mouseY);
            default -> {
            }
        }

        // Full-height Right Engineering Inspector Panel
        renderInspectorPanel(graphics, x + INS_X, y + INS_Y, INS_W, INS_H, mouseX, mouseY);

        // Bottom Left Player Inventory & Utility Bay (y=168..248, x=6..282)
        graphics.fill(x + 6, y + 168, x + 282, y + 248, 0xFF131B25);
        graphics.renderOutline(x + 6, y + 168, 276, 80, 0xFF25374B);

        // Draw crisp recessed slot backgrounds for 3x9 inv + 9 hotbar
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(graphics, x + 7 + col * 18, y + 171 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBox(graphics, x + 7 + col * 18, y + 229);
        }

        // Capacitor frame + Charge slot
        graphics.fill(x + 175, y + 171, x + 193, y + 221, 0xFF0B111A);
        graphics.renderOutline(x + 175, y + 171, 18, 50, 0xFF2E435A);
        drawSlotBox(graphics, x + 175, y + 227);

        // Etch Bay & Archive Core Upgrade Bay (x=198..278)
        graphics.fill(x + 198, y + 171, x + 278, y + 245, 0xFF0E1621);
        graphics.renderOutline(x + 198, y + 171, 80, 74, 0xFF28435C);
        graphics.drawString(this.font, "ETCH & CORE", x + 204, y + 175, 0xFF9AC2D8, false);
        drawSlotBox(graphics, x + 203, y + 191);
        drawSlotBox(graphics, x + 243, y + 191);
        graphics.drawString(this.font, "->", x + 226, y + 196, 0xFF5CE0FF, false);
        if (this.menu.itemStorage.slot(AerospaceRdTerminalBlockEntity.SCHEMATIC_MEDIA_SLOT).isEmpty()) {
            graphics.drawString(this.font, "DOC", x + 204, y + 196, 0xFF536B85, false);
        }
        if (this.menu.itemStorage.slot(AerospaceRdTerminalBlockEntity.SCHEMATIC_OUTPUT_SLOT).isEmpty()) {
            graphics.drawString(this.font, "SCH", x + 244, y + 196, 0xFF536B85, false);
        }

        // Archive Core Upgrade Slot
        drawSlotBox(graphics, x + 203, y + 227);
        int upTier = this.menu.getUpgradeTier();
        String upTxt = upTier == 2 ? "QUANTUM" : (upTier == 1 ? "COPROC" : "NO CORE");
        int upCol = upTier == 2 ? 0xFFC084FC : (upTier == 1 ? 0xFF34D399 : 0xFF64748B);
        graphics.drawString(this.font, upTxt, x + 224, y + 231, upCol, false);
        String unlockedSummary = ClientResearchCache.unlockedNodes().size() + "/" + ResearchCatalog.totalNodeCount();
        graphics.drawString(this.font, unlockedSummary, x + 224, y + 216, 0xFF34D399, false);
    }

    private void renderKspTechTree(GuiGraphics graphics, int vpX, int vpY, int vpW, int vpH, int mouseX, int mouseY, long timeMs) {
        graphics.enableScissor(vpX + 1, vpY + 1, vpX + vpW - 1, vpY + vpH - 1);

        // Blueprint grid background that moves with Pan & Zoom
        int gridStep = Math.max(10, Math.round(24.0F * this.zoom));
        int offX = Math.floorMod(Math.round(this.panX), gridStep);
        int offY = Math.floorMod(Math.round(this.panY), gridStep);
        for (int gx = vpX + offX; gx < vpX + vpW; gx += gridStep) {
            graphics.fill(gx, vpY, gx + 1, vpY + vpH, 0xFF0E1A28);
        }
        for (int gy = vpY + offY; gy < vpY + vpH; gy += gridStep) {
            graphics.fill(vpX, gy, vpX + vpW, gy + 1, 0xFF0E1A28);
        }

        // Concentric radial Era rings centered around Core Hub (0, 0)
        int hubCenterX = worldToScreenX(vpX, NODE_W * 0.5F);
        int hubCenterY = worldToScreenY(vpY, NODE_H * 0.5F);
        float[] ringRadiiWorld = {160.0F, 340.0F, 560.0F, 820.0F, 1080.0F};
        for (float rWorld : ringRadiiWorld) {
            int rPx = Math.round(rWorld * this.zoom);
            if (rPx >= 12 && rPx <= 1600) {
                drawDottedCircle(graphics, hubCenterX, hubCenterY, rPx, 0xFF15273B);
            }
        }

        // 5 Wing Sector Banners in world-space around the central hub
        drawSectorBanner(graphics, vpX, vpY, vpW, vpH, 0.0F, -1.05F, "ENERGY & COMMNET", 0xFF38BDF8);
        drawSectorBanner(graphics, vpX, vpY, vpW, vpH, 4.8F, -0.05F, "METALLURGY", 0xFFF59E0B);
        drawSectorBanner(graphics, vpX, vpY, vpW, vpH, -4.8F, -0.05F, "LIFE SUPPORT", 0xFF34D399);
        drawSectorBanner(graphics, vpX, vpY, vpW, vpH, -4.8F, 2.35F, "SUITS & OPTICS", 0xFFC084FC);
        drawSectorBanner(graphics, vpX, vpY, vpW, vpH, 4.8F, 2.35F, "AEROSPACE", 0xFF60A5FA);

        int nw = Math.max(10, Math.round(NODE_W * this.zoom));
        int nh = Math.max(9, Math.round(NODE_H * this.zoom));

        // Subtle radial spokes from Center Hub (0,0) to independent starter anchors
        ResearchNode hubNode = ResearchCatalog.get(ResearchCatalog.EARTH_PROTOTYPING);
        if (hubNode != null) {
            int hx = nodeScreenX(vpX, hubNode) + nw / 2;
            int hy = nodeScreenY(vpY, hubNode) + nh / 2;
            for (ResourceLocation anchorId : List.of(ResearchCatalog.BASIC_OXYGEN_GEAR, ResearchCatalog.EARTH_LUNAR_EPHEMERIS)) {
                ResearchNode anchor = ResearchCatalog.get(anchorId);
                if (anchor != null) {
                    int ax = nodeScreenX(vpX, anchor) + nw / 2;
                    int ay = nodeScreenY(vpY, anchor) + nh / 2;
                    boolean anchorUnlocked = ClientResearchCache.isUnlocked(anchorId);
                    drawVectorLine(graphics, hx, hy, ax, ay, anchorUnlocked ? 0xFF286E58 : 0xFF1D3248);
                }
            }
        }

        // Draw crisp 360-degree center-to-center constellation prerequisite links
        for (ResearchNode node : ResearchCatalog.allNodes()) {
            int ncx = nodeScreenX(vpX, node) + nw / 2;
            int ncy = nodeScreenY(vpY, node) + nh / 2;
            boolean nodeUnlocked = ClientResearchCache.isUnlocked(node.id());

            for (ResourceLocation prereqId : node.prerequisites()) {
                ResearchNode prereq = ResearchCatalog.get(prereqId);
                if (prereq == null) {
                    continue;
                }
                int pcx = nodeScreenX(vpX, prereq) + nw / 2;
                int pcy = nodeScreenY(vpY, prereq) + nh / 2;
                boolean prereqUnlocked = ClientResearchCache.isUnlocked(prereqId);
                int lineCol = (nodeUnlocked && prereqUnlocked) ? 0xFF34D399
                        : (prereqUnlocked ? 0xFF38BDF8 : 0xFF283A52);

                drawVectorLine(graphics, pcx, pcy, ncx, ncy, lineCol);
            }
        }

        // Draw all 55 spaced constellation nodes (dimming non-matching branches when a branch filter is active)
        ResearchBranch activeBranchFilter = this.branchFilterIdx > 0
                ? ResearchBranch.values()[this.branchFilterIdx - 1]
                : null;
        for (ResearchNode node : ResearchCatalog.allNodes()) {
            int nx = nodeScreenX(vpX, node);
            int ny = nodeScreenY(vpY, node);
            if (nx + nw < vpX || nx > vpX + vpW || ny + nh < vpY || ny > vpY + vpH) {
                continue;
            }

            boolean branchMatched = activeBranchFilter == null || node.branch() == activeBranchFilter || (node.gridX() == 0 && node.gridY() == 0);
            boolean isCoreHub = node.gridX() == 0 && node.gridY() == 0;
            boolean unlocked = ClientResearchCache.isUnlocked(node.id());
            boolean selected = node.id().equals(this.selectedNodeId);
            boolean prereqsMet = arePrereqsMet(node);
            boolean canResearch = canResearchNow(node);

            if (isCoreHub) {
                graphics.renderOutline(nx - 2, ny - 2, nw + 4, nh + 4, 0x8838BDF8);
            }

            int bgCol = !branchMatched ? 0xFF0A1018
                    : (unlocked ? 0xFF0E3829 : (selected ? 0xFF1A3A58 : (canResearch ? 0xFF11283B : 0xFF0E1722)));
            int borderCol;
            if (selected) {
                borderCol = 0xFFFFFFFF;
            } else if (!branchMatched) {
                borderCol = 0xFF1B2838;
            } else if (unlocked) {
                borderCol = 0xFF34D399;
            } else if (canResearch) {
                borderCol = (((timeMs / 350L) & 1L) == 0L) ? 0xFF38BDF8 : 0xFF7BE8FF;
            } else if (prereqsMet) {
                borderCol = 0xFFF59E0B;
            } else {
                borderCol = 0xFF2F4156;
            }

            graphics.fill(nx, ny, nx + nw, ny + nh, bgCol);
            graphics.renderOutline(nx, ny, nw, nh, borderCol);

            // Domain color accent bar at top of node
            int barH = Math.max(1, Math.min(3, nh / 8));
            graphics.fill(nx + 1, ny + 1, nx + nw - 1, ny + 1 + barH, node.domain().colorArgb());

            // Milestone star badge at top-right if node requires a physical milestone
            if (node.hasMilestoneRequirement() && nw >= 14) {
                boolean msDone = ClientResearchCache.hasMilestone(node.requiredMilestone());
                graphics.fill(nx + nw - 4, ny + 3, nx + nw - 1, ny + 6, msDone ? 0xFF34D399 : 0xFFF59E0B);
            }

            // Smooth zoom-scaled item icon centered inside node
            Item iconItem = BuiltInRegistries.ITEM.get(node.iconItemId());
            float iconScale = Mth.clamp(this.zoom * 1.15F, 0.42F, 1.0F);
            float iconDrawSize = 16.0F * iconScale;
            float iconX = nx + (nw - iconDrawSize) * 0.5F;
            float iconY = ny + (nh >= 24 ? (nh - 8 - iconDrawSize) * 0.5F + 2.0F : (nh - iconDrawSize) * 0.5F + 1.0F);
            graphics.pose().pushPose();
            graphics.pose().translate(iconX, iconY, 0.0F);
            graphics.pose().scale(iconScale, iconScale, 1.0F);
            graphics.renderItem(new ItemStack(iconItem == Items.AIR ? Items.PAPER : iconItem), 0, 0);
            graphics.pose().popPose();

            // SP cost pill at bottom if zoomed in enough
            if (nh >= 23 && nw >= 24) {
                String badge = unlocked ? "OK" : (node.spCost() + "SP");
                int bCol = unlocked ? 0xFF68F5B2 : (canResearch ? 0xFF7BE8FF : 0xFF94A3B8);
                graphics.drawString(this.font, badge, nx + (nw - this.font.width(badge)) / 2, ny + nh - 9, bCol, false);
            }

            if (mouseIn(mouseX, mouseY, vpX, vpY, vpW, vpH) && mouseIn(mouseX, mouseY, nx, ny, nw, nh)) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(node.title() + " [Era " + node.era() + "]").withStyle(unlocked ? ChatFormatting.GREEN : ChatFormatting.AQUA));
                tip.add(Component.literal("Cost: " + node.spCost() + " SP"
                        + (node.requiredDomainPoints() > 0 ? " + " + node.requiredDomainPoints() + " " + node.domain().shortCode() : ""))
                        .withStyle(ChatFormatting.YELLOW));
                if (node.hasMilestoneRequirement()) {
                    boolean msDone = ClientResearchCache.hasMilestone(node.requiredMilestone());
                    tip.add(Component.literal("Milestone: " + ResearchMilestone.label(node.requiredMilestone()))
                            .withStyle(msDone ? ChatFormatting.GREEN : ChatFormatting.GOLD));
                }
                this.setTooltipForNextRenderPass(tip.stream().map(Component::getVisualOrderText).toList());
            }
        }

        graphics.disableScissor();

        // Top overlay strip on KSP viewport: 5-Branch Filter Pills (ALL + 5 Branches)
        int bfx = vpX + 3;
        int bfy = vpY + 3;
        ResearchBranch[] branches = ResearchBranch.values();
        for (int i = 0; i <= branches.length; i++) {
            String lbl = i == 0 ? "ALL" : branches[i - 1].tabLabel();
            int accent = i == 0 ? 0xFF38BDF8 : branches[i - 1].accentColor();
            int bw = Math.max(22, this.font.width(lbl) + 6);
            boolean active = this.branchFilterIdx == i;
            boolean hov = mouseIn(mouseX, mouseY, bfx, bfy, bw, 11);
            graphics.fill(bfx, bfy, bfx + bw, bfy + 11, active ? 0xEE143248 : (hov ? 0xDD112334 : 0xCC0A131E));
            graphics.renderOutline(bfx, bfy, bw, 11, active ? accent : 0xFF283A52);
            graphics.drawString(this.font, lbl, bfx + 3, bfy + 2, active ? accent : 0xFF94A3B8, false);
            bfx += bw + 2;
        }

        // Bottom-left overlay controls on KSP viewport: zoom info + Reset View button
        graphics.fill(vpX + 4, vpY + vpH - 14, vpX + 104, vpY + vpH - 3, 0xCC0A131E);
        String hint = String.format(Locale.ROOT, "DRAG/SCROLL %.0f%%", this.zoom * 100.0F);
        graphics.drawString(this.font, hint, vpX + 7, vpY + vpH - 12, 0xFF7B90A8, false);

        int rstX = vpX + vpW - 48;
        int rstY = vpY + vpH - 15;
        boolean rstHov = mouseIn(mouseX, mouseY, rstX, rstY, 44, 12);
        graphics.fill(rstX, rstY, rstX + 44, rstY + 12, rstHov ? 0xFF1D3A54 : 0xFF102030);
        graphics.renderOutline(rstX, rstY, 44, 12, 0xFF38BDF8);
        String cLbl = "CENTER";
        graphics.drawString(this.font, cLbl, rstX + (44 - this.font.width(cLbl)) / 2, rstY + 2, 0xFFEAF6FF, false);
    }

    private void renderScienceArchives(GuiGraphics graphics, int vpX, int vpY, int vpW, int vpH, int mouseX, int mouseY) {
        // Planet filter strip across top
        int fx = vpX + 4;
        int fy = vpY + 4;
        for (int i = 0; i < PLANET_FILTERS.length; i++) {
            String lbl = PLANET_FILTERS[i];
            int fw = Math.max(26, this.font.width(lbl) + 6);
            boolean active = i == this.planetFilterIdx;
            boolean hov = mouseIn(mouseX, mouseY, fx, fy, fw, 12);
            graphics.fill(fx, fy, fx + fw, fy + 12, active ? 0xFF164038 : (hov ? 0xFF142436 : 0xFF0D1622));
            graphics.renderOutline(fx, fy, fw, 12, active ? 0xFF34D399 : 0xFF283A52);
            graphics.drawString(this.font, lbl, fx + 3, fy + 2, active ? 0xFF68F5B2 : 0xFF94A3B8, false);
            fx += fw + 2;
        }

        // Lifetime SP & diminishing boost summary
        String totalLine = "LIFETIME: " + ClientResearchCache.totalScienceEarned() + " SP"
                + (ClientResearchCache.quantumArchiveBoost() ? " | QUANTUM 50%" : " | DIM 35%");
        graphics.drawString(this.font, totalLine, vpX + 5, vpY + 19, 0xFF7BE8FF, false);

        List<ExperimentRecord> filtered = filteredArchives();
        int listX = vpX + 4;
        int listY = vpY + 30;
        int listW = vpW - 8;
        int rowH = 21;
        int visibleRows = 5;

        if (filtered.isEmpty()) {
            graphics.drawString(this.font, "No scientific reports recorded yet.", listX + 6, listY + 24, 0xFF64748B, false);
            graphics.drawString(this.font, "Complete interactive experiments in", listX + 6, listY + 38, 0xFF9AC2D8, false);
            graphics.drawString(this.font, "Telescope, Workbench, Geo, Env or 0G.", listX + 6, listY + 50, 0xFF9AC2D8, false);
            return;
        }

        int maxScroll = Math.max(0, filtered.size() - visibleRows);
        this.archiveScroll = Mth.clamp(this.archiveScroll, 0, maxScroll);

        for (int i = 0; i < visibleRows && (i + this.archiveScroll) < filtered.size(); i++) {
            ExperimentRecord rec = filtered.get(i + this.archiveScroll);
            int ry = listY + i * rowH;
            graphics.fill(listX, ry, listX + listW, ry + rowH - 2, 0xFF0D1724);
            graphics.renderOutline(listX, ry, listW, rowH - 2, 0xFF1F3247);

            String header = this.font.plainSubstrByWidth(rec.title(), listW - 88);
            graphics.drawString(this.font, header, listX + 4, ry + 2, 0xFFEAF6FF, false);

            String tag = rec.planetLabel() + " | " + rec.domain().shortCode();
            graphics.drawString(this.font, tag, listX + listW - this.font.width(tag) - 4, ry + 2, rec.domain().colorArgb(), false);

            // Progress bar (collected / max SP)
            int barX = listX + 4;
            int barY = ry + 12;
            int barW = listW - 102;
            graphics.fill(barX, barY, barX + barW, barY + 4, 0xFF162230);
            float frac = rec.maxSpPool() > 0 ? Mth.clamp((float) rec.collectedSp() / rec.maxSpPool(), 0.0F, 1.0F) : 1.0F;
            int fillW = Math.round(barW * frac);
            if (fillW > 0) {
                graphics.fill(barX, barY, barX + fillW, barY + 4, frac >= 0.99F ? 0xFF34D399 : 0xFF38BDF8);
            }

            int nextPct = rec.nextDiminishingPct(ClientResearchCache.quantumArchiveBoost());
            String spInfo = rec.collectedSp() + "/" + rec.maxSpPool() + " SP (" + (nextPct > 0 ? nextPct + "%" : "MAX") + ")";
            graphics.drawString(this.font, spInfo, barX + barW + 6, ry + 10, nextPct > 0 ? 0xFF9AC2D8 : 0xFF34D399, false);
        }
    }

    private void renderAtlasAndCommNet(GuiGraphics graphics, int vpX, int vpY, int vpW, int vpH, int mouseX, int mouseY) {
        // Left column: Solar System Ephemeris + Telescope Photo Catalog
        int leftW = 148;
        graphics.fill(vpX + 3, vpY + 3, vpX + 3 + leftW, vpY + vpH - 3, 0xFF0A121C);
        graphics.renderOutline(vpX + 3, vpY + 3, leftW, vpH - 6, 0xFF1F3247);

        List<AstroDiscovery> atlas = ClientResearchCache.astroAtlas();
        graphics.drawString(this.font, "ASTRO ATLAS (" + atlas.size() + ")", vpX + 7, vpY + 6, 0xFFC084FC, false);
        String mapCnt = ClientResearchCache.mappedBodies().size() + "/10 BODIES";
        graphics.drawString(this.font, mapCnt, vpX + 3 + leftW - this.font.width(mapCnt) - 4, vpY + 6, 0xFF34D399, false);

        if (atlas.isEmpty()) {
            // Fall back to showing Solar System mapping status
            List<PlanetaryEnvironment> presets = PlanetaryEnvironment.SOLAR_SYSTEM_PRESETS;
            for (int i = 0; i < Math.min(10, presets.size()); i++) {
                PlanetaryEnvironment env = presets.get(i);
                int col = i / 5;
                int row = i % 5;
                int px = vpX + 7 + col * 70;
                int py = vpY + 20 + row * 22;
                boolean mapped = ClientResearchCache.isBodyMapped(env.id());
                boolean sel = i == this.selectedPlanetIdx;
                graphics.fill(px, py, px + 66, py + 18, sel ? 0xFF1B334B : 0xFF0D1724);
                graphics.renderOutline(px, py, 66, 18, mapped ? 0xFF34D399 : 0xFF283A52);
                graphics.drawString(this.font, this.font.plainSubstrByWidth(env.displayName(), 58), px + 4, py + 5, mapped ? 0xFFEAF6FF : 0xFF64748B, false);
            }
        } else {
            int visible = 6;
            this.atlasScroll = Mth.clamp(this.atlasScroll, 0, Math.max(0, atlas.size() - visible));
            for (int i = 0; i < visible && (i + this.atlasScroll) < atlas.size(); i++) {
                AstroDiscovery d = atlas.get(i + this.atlasScroll);
                int ry = vpY + 19 + i * 19;
                graphics.fill(vpX + 6, ry, vpX + leftW, ry + 17, 0xFF0F1B2A);
                graphics.renderOutline(vpX + 6, ry, leftW - 6, 17, 0xFF28435C);
                graphics.drawString(this.font, this.font.plainSubstrByWidth(d.displayName(), 82), vpX + 9, ry + 4, 0xFFEAF6FF, false);
                String meta = d.category().substring(0, Math.min(4, d.category().length())) + " " + d.sharpnessPct() + "%";
                graphics.drawString(this.font, meta, vpX + leftW - this.font.width(meta) - 3, ry + 4, 0xFF5CE0FF, false);
            }
        }

        // Right column: CommNet Relays + Live Telemetry Feed
        int rx = vpX + leftW + 7;
        int rw = vpW - leftW - 10;
        graphics.fill(rx, vpY + 3, rx + rw, vpY + 62, 0xFF0A121C);
        graphics.renderOutline(rx, vpY + 3, rw, 59, 0xFF1F3247);
        graphics.drawString(this.font, "COMMNET " + ClientResearchCache.networkBonusPct() + "%", rx + 4, vpY + 6, 0xFF34D399, false);
        List<String> relays = ClientResearchCache.relaySummaries();
        if (relays.isEmpty()) {
            graphics.drawString(this.font, "No active Data Links", rx + 4, vpY + 20, 0xFF64748B, false);
            graphics.drawString(this.font, "Lock dish phase to", rx + 4, vpY + 32, 0xFF475569, false);
            graphics.drawString(this.font, "boost SP by +20%/body", rx + 4, vpY + 44, 0xFF475569, false);
        } else {
            for (int i = 0; i < Math.min(4, relays.size()); i++) {
                graphics.drawString(this.font, this.font.plainSubstrByWidth(relays.get(i), rw - 8), rx + 4, vpY + 18 + i * 10, 0xFF9AC2D8, false);
            }
        }

        int ly = vpY + 66;
        int lh = vpH - 69;
        graphics.fill(rx, ly, rx + rw, ly + lh, 0xFF0A121C);
        graphics.renderOutline(rx, ly, rw, lh, 0xFF1F3247);
        graphics.drawString(this.font, "TELEMETRY LOG", rx + 4, ly + 4, 0xFF38BDF8, false);
        List<String> logs = ClientResearchCache.experimentLog();
        for (int i = 0; i < Math.min(5, logs.size()); i++) {
            graphics.drawString(this.font, this.font.plainSubstrByWidth(logs.get(i), rw - 8), rx + 4, ly + 16 + i * 11, i == 0 ? 0xFFEAF6FF : 0xFF7B90A8, false);
        }
    }

    private void renderInspectorPanel(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        graphics.fill(x, y, x + w, y + h, 0xFF0C1521);
        graphics.renderOutline(x, y, w, h, 0xFF28435C);

        // Top 5 Scientific Domains compact bar grid (y..y+44)
        graphics.fill(x + 2, y + 2, x + w - 2, y + 44, 0xFF08101A);
        graphics.drawString(this.font, "SCIENTIFIC DOMAINS", x + 5, y + 4, 0xFF7B90A8, false);
        ResearchDomain[] domains = ResearchDomain.values();
        for (int i = 0; i < domains.length; i++) {
            ResearchDomain dom = domains[i];
            int col = i % 2;
            int row = i / 2;
            int dx = x + 5 + col * 58;
            int dy = y + 14 + row * 10;
            String txt = dom.shortCode() + ":" + ClientResearchCache.domainPoints(dom);
            graphics.drawString(this.font, txt, dx, dy, dom.colorArgb(), false);
        }
        graphics.fill(x + 2, y + 45, x + w - 2, y + 46, 0xFF1E3247);

        ResearchNode sel = ResearchCatalog.get(this.selectedNodeId);
        if (sel == null) {
            return;
        }
        boolean unlocked = ClientResearchCache.isUnlocked(sel.id());
        boolean prereqsMet = arePrereqsMet(sel);
        boolean canResearch = canResearchNow(sel);

        // Node header with icon & wrapped title (up to 2 crisp lines)
        Item iconItem = BuiltInRegistries.ITEM.get(sel.iconItemId());
        graphics.renderItem(new ItemStack(iconItem == Items.AIR ? Items.PAPER : iconItem), x + 5, y + 50);
        var wrappedTitle = this.font.split(Component.literal(sel.title()), w - 28);
        for (int li = 0; li < Math.min(2, wrappedTitle.size()); li++) {
            graphics.drawString(this.font, wrappedTitle.get(li), x + 24, y + 48 + li * 9, unlocked ? 0xFF34D399 : 0xFFEAF6FF, false);
        }
        String eraStr = "ERA " + sel.era() + " | " + sel.branch().tabLabel();
        graphics.drawString(this.font, eraStr, x + 5, y + 68, sel.branch().accentColor(), false);

        // Cost breakdown: SP + Secondary Domain + Milestone
        int cy = y + 79;
        int haveSp = ClientResearchCache.sciencePoints();
        boolean spOk = haveSp >= sel.spCost();
        graphics.drawString(this.font, "SP: " + haveSp + " / " + sel.spCost(), x + 5, cy, spOk || unlocked ? 0xFF7BE8FF : 0xFFFF6B6B, false);
        cy += 11;

        if (sel.requiredDomainPoints() > 0) {
            int haveDom = ClientResearchCache.domainPoints(sel.domain());
            boolean domOk = haveDom >= sel.requiredDomainPoints();
            graphics.drawString(this.font, sel.domain().shortCode() + ": " + haveDom + " / " + sel.requiredDomainPoints(),
                    x + 5, cy, domOk || unlocked ? sel.domain().colorArgb() : 0xFFFF6B6B, false);
            cy += 11;
        }

        if (sel.hasMilestoneRequirement()) {
            boolean msOk = ClientResearchCache.hasMilestone(sel.requiredMilestone());
            String msLine = (msOk ? "[OK] " : "[!] ") + ResearchMilestone.label(sel.requiredMilestone());
            graphics.drawString(this.font, this.font.plainSubstrByWidth(msLine, w - 10), x + 5, cy, msOk ? 0xFF34D399 : 0xFFF59E0B, false);
            cy += 11;
        }

        // Lab & Location hint
        graphics.drawString(this.font, this.font.plainSubstrByWidth("Lab: " + sel.sourceLabName(), w - 10), x + 5, cy, 0xFF9AC2D8, false);
        cy += 10;
        graphics.drawString(this.font, this.font.plainSubstrByWidth("@ " + sel.locationRequirement().label(), w - 10), x + 5, cy, 0xFF7B90A8, false);
        cy += 13;

        // Unlocks section
        graphics.drawString(this.font, "UNLOCKS:", x + 5, cy, 0xFF5CE0FF, false);
        cy += 10;
        for (int k = 0; k < Math.min(4, sel.unlocks().size()); k++) {
            String u = this.font.plainSubstrByWidth("- " + sel.unlocks().get(k), w - 10);
            graphics.drawString(this.font, u, x + 5, cy + k * 10, unlocked ? 0xFF68F5B2 : 0xFFCBD5E1, false);
        }

        // Bottom Action Button (RESEARCH or ETCH SCHEMATIC)
        int btnX = x + 6;
        int btnY = y + h - 24;
        int btnW = w - 12;
        int btnH = 18;
        boolean hov = mouseIn(mouseX, mouseY, btnX, btnY, btnW, btnH);
        if (unlocked) {
            graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, hov ? 0xFF1F4E5F : 0xFF143542);
            graphics.renderOutline(btnX, btnY, btnW, btnH, 0xFF38BDF8);
            String lbl = "ETCH SCHEMATIC";
            graphics.drawString(this.font, lbl, btnX + (btnW - this.font.width(lbl)) / 2, btnY + 5, 0xFF7BE8FF, true);
        } else {
            int bg = canResearch ? (hov ? 0xFF15803D : 0xFF166534) : 0xFF1E293B;
            int brd = canResearch ? 0xFF34D399 : 0xFF475569;
            graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, bg);
            graphics.renderOutline(btnX, btnY, btnW, btnH, brd);
            String lbl = canResearch ? ("RESEARCH (" + sel.spCost() + " SP)") : (prereqsMet ? "REQUIREMENTS" : "PREREQ LOCKED");
            graphics.drawString(this.font, lbl, btnX + (btnW - this.font.width(lbl)) / 2, btnY + 5, canResearch ? 0xFF68F5B2 : 0xFF94A3B8, true);
        }
    }

    private boolean canResearchNow(@NotNull ResearchNode node) {
        if (ClientResearchCache.isUnlocked(node.id()) || !arePrereqsMet(node)) {
            return false;
        }
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative()) {
            return true;
        }
        return ClientResearchCache.sciencePoints() >= node.spCost()
                && ClientResearchCache.domainPoints(node.domain()) >= node.requiredDomainPoints()
                && ClientResearchCache.hasMilestone(node.requiredMilestone());
    }

    private static boolean arePrereqsMet(@NotNull ResearchNode node) {
        for (ResourceLocation prereq : node.prerequisites()) {
            if (!ClientResearchCache.isUnlocked(prereq)) {
                return false;
            }
        }
        return true;
    }

    private @NotNull List<ExperimentRecord> filteredArchives() {
        String filter = PLANET_FILTERS[this.planetFilterIdx];
        List<ExperimentRecord> all = ClientResearchCache.scienceArchives();
        if ("ALL".equals(filter)) {
            return all;
        }
        String low = filter.toLowerCase(Locale.ROOT);
        List<ExperimentRecord> out = new ArrayList<>();
        for (ExperimentRecord rec : all) {
            String pl = rec.planetLabel().toLowerCase(Locale.ROOT);
            if ("outer".equals(low)) {
                if (pl.contains("jupiter") || pl.contains("saturn") || pl.contains("uranus") || pl.contains("neptune")) {
                    out.add(rec);
                }
            } else if ("orbit".equals(low)) {
                if (pl.contains("orbit") || pl.contains("station") || pl.contains("asteroid")) {
                    out.add(rec);
                }
            } else if (pl.contains(low)) {
                out.add(rec);
            }
        }
        return out;
    }

    private void focusBranch(@Nullable ResearchBranch branch) {
        if (branch == null) {
            this.zoom = DEFAULT_ZOOM;
            this.panX = VP_W * 0.5F - (NODE_W * 0.5F) * this.zoom;
            this.panY = VP_H * 0.5F - (NODE_H * 0.5F) * this.zoom;
            return;
        }
        List<ResearchNode> nodes = ResearchCatalog.nodesForBranch(branch);
        if (nodes.isEmpty()) {
            return;
        }
        float sumX = 0.0F;
        float sumY = 0.0F;
        for (ResearchNode n : nodes) {
            sumX += n.gridX() * COL_STEP + NODE_W * 0.5F;
            sumY += n.gridY() * ROW_STEP + NODE_H * 0.5F;
        }
        float avgX = sumX / nodes.size();
        float avgY = sumY / nodes.size();
        this.zoom = 0.56F;
        this.panX = Mth.clamp(VP_W * 0.5F - avgX * this.zoom, -1350.0F, 1350.0F);
        this.panY = Mth.clamp(VP_H * 0.5F - avgY * this.zoom, -980.0F, 980.0F);
        this.selectedNodeId = nodes.get(0).id();
    }

    private int worldToScreenX(int vpX, float worldX) {
        return vpX + Math.round(worldX * this.zoom + this.panX);
    }

    private int worldToScreenY(int vpY, float worldY) {
        return vpY + Math.round(worldY * this.zoom + this.panY);
    }

    private int nodeScreenX(int vpX, @NotNull ResearchNode node) {
        return worldToScreenX(vpX, node.gridX() * COL_STEP);
    }

    private int nodeScreenY(int vpY, @NotNull ResearchNode node) {
        return worldToScreenY(vpY, node.gridY() * ROW_STEP);
    }

    private void drawSectorBanner(GuiGraphics g, int vpX, int vpY, int vpW, int vpH, float gridX, float gridY, String label, int accentColor) {
        int cx = worldToScreenX(vpX, gridX * COL_STEP + NODE_W * 0.5F);
        int cy = worldToScreenY(vpY, gridY * ROW_STEP + NODE_H * 0.5F);
        int w = this.font.width(label);
        if (cx + w / 2 >= vpX && cx - w / 2 <= vpX + vpW && cy + 10 >= vpY && cy - 10 <= vpY + vpH) {
            g.fill(cx - w / 2 - 3, cy - 2, cx + w / 2 + 3, cy + 9, 0xAA0A1624);
            g.drawString(this.font, label, cx - w / 2, cy, accentColor, false);
        }
    }

    private static void drawDottedCircle(GuiGraphics g, int cx, int cy, int r, int col) {
        int stepDeg = r > 300 ? 3 : 6;
        for (int deg = 0; deg < 360; deg += stepDeg) {
            double rad = Math.toRadians(deg);
            int px = cx + (int) Math.round(Math.cos(rad) * r);
            int py = cy + (int) Math.round(Math.sin(rad) * r);
            g.fill(px, py, px + 1, py + 1, col);
        }
    }

    private static void drawVectorLine(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) {
            g.fill(x0, y0, x0 + 2, y0 + 2, col);
            return;
        }
        int inc = Math.max(1, steps > 120 ? 2 : 1);
        for (int i = 0; i <= steps; i += inc) {
            int px = x0 + (dx * i) / steps;
            int py = y0 + (dy * i) / steps;
            g.fill(px, py, px + 2, py + 2, col);
        }
    }

    private static void drawSlotBox(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF0A111A);
        graphics.renderOutline(x, y, 18, 18, 0xFF2B3D52);
    }

    private void drawTopTab(GuiGraphics graphics, int x, int y, int w, int h, String label, boolean active, int accent, int mouseX, int mouseY) {
        boolean hov = mouseIn(mouseX, mouseY, x, y, w, h);
        graphics.fill(x, y, x + w, y + h, active ? 0xFF1C334A : (hov ? 0xFF152436 : 0xFF0D1622));
        graphics.renderOutline(x, y, w, h, active ? accent : 0xFF283A52);
        graphics.drawString(this.font, label, x + (w - this.font.width(label)) / 2, y + 3, active ? accent : 0xFF94A3B8, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            int x = this.leftPos;
            int y = this.topPos;

            // Top tabs
            if (mouseIn(mx, my, x + 146, y + 4, 72, 14)) {
                this.activeView = VIEW_TREE;
                return true;
            }
            if (mouseIn(mx, my, x + 222, y + 4, 92, 14)) {
                this.activeView = VIEW_ARCHIVES;
                return true;
            }
            if (mouseIn(mx, my, x + 318, y + 4, 92, 14)) {
                this.activeView = VIEW_ATLAS;
                ClientPlayNetworking.send(new ResearchTerminalActionPayload(this.menu.be.getBlockPos(), ResearchTerminalActionPayload.ACTION_SYNC_NOW, Constant.id("sync")));
                return true;
            }

            // Right Inspector action button
            int btnX = x + INS_X + 6;
            int btnY = y + INS_Y + INS_H - 24;
            if (this.selectedNodeId != null && mouseIn(mx, my, btnX, btnY, INS_W - 12, 18)) {
                boolean unlocked = ClientResearchCache.isUnlocked(this.selectedNodeId);
                int action = unlocked
                        ? ResearchTerminalActionPayload.ACTION_ETCH_SCHEMATIC
                        : ResearchTerminalActionPayload.ACTION_UNLOCK_NODE;
                ClientPlayNetworking.send(new ResearchTerminalActionPayload(this.menu.be.getBlockPos(), action, this.selectedNodeId));
                return true;
            }

            int vpX = x + VP_X;
            int vpY = y + VP_Y;
            if (this.activeView == VIEW_TREE && mouseIn(mx, my, vpX, vpY, VP_W, VP_H)) {
                // 5-Branch filter pill strip across top of Tech Tree viewport
                int bfx = vpX + 3;
                int bfy = vpY + 3;
                ResearchBranch[] branches = ResearchBranch.values();
                for (int i = 0; i <= branches.length; i++) {
                    String lbl = i == 0 ? "ALL" : branches[i - 1].tabLabel();
                    int bw = Math.max(22, this.font.width(lbl) + 6);
                    if (mouseIn(mx, my, bfx, bfy, bw, 11)) {
                        this.branchFilterIdx = i;
                        focusBranch(i == 0 ? null : branches[i - 1]);
                        return true;
                    }
                    bfx += bw + 2;
                }
                // Center button snaps cleanly back onto (0, 0) Core Hub
                if (mouseIn(mx, my, vpX + VP_W - 48, vpY + VP_H - 15, 44, 12)) {
                    this.branchFilterIdx = 0;
                    this.zoom = DEFAULT_ZOOM;
                    this.panX = VP_W * 0.5F - (NODE_W * 0.5F) * this.zoom;
                    this.panY = VP_H * 0.5F - (NODE_H * 0.5F) * this.zoom;
                    return true;
                }
                // Check node click
                int nw = Math.max(10, Math.round(NODE_W * this.zoom));
                int nh = Math.max(9, Math.round(NODE_H * this.zoom));
                for (ResearchNode node : ResearchCatalog.allNodes()) {
                    int nx = nodeScreenX(vpX, node);
                    int ny = nodeScreenY(vpY, node);
                    if (mouseIn(mx, my, nx, ny, nw, nh)) {
                        this.selectedNodeId = node.id();
                        return true;
                    }
                }
                // Otherwise begin dragging Pan
                this.draggingTree = true;
                this.dragStartMouseX = mouseX;
                this.dragStartMouseY = mouseY;
                this.dragStartPanX = this.panX;
                this.dragStartPanY = this.panY;
                return true;
            } else if (this.activeView == VIEW_ARCHIVES && mouseIn(mx, my, vpX, vpY, VP_W, VP_H)) {
                int fx = vpX + 4;
                int fy = vpY + 4;
                for (int i = 0; i < PLANET_FILTERS.length; i++) {
                    int fw = Math.max(26, this.font.width(PLANET_FILTERS[i]) + 6);
                    if (mouseIn(mx, my, fx, fy, fw, 12)) {
                        this.planetFilterIdx = i;
                        this.archiveScroll = 0;
                        return true;
                    }
                    fx += fw + 2;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingTree && button == 0 && this.activeView == VIEW_TREE) {
            this.panX = Mth.clamp(this.dragStartPanX + (float) (mouseX - this.dragStartMouseX), -1350.0F, 1350.0F);
            this.panY = Mth.clamp(this.dragStartPanY + (float) (mouseY - this.dragStartMouseY), -980.0F, 980.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingTree) {
            this.draggingTree = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int vpX = this.leftPos + VP_X;
        int vpY = this.topPos + VP_Y;
        if (mouseIn(mouseX, mouseY, vpX, vpY, VP_W, VP_H)) {
            if (this.activeView == VIEW_TREE) {
                float oldZoom = this.zoom;
                float nextZoom = Mth.clamp(oldZoom + (float) scrollY * 0.08F, 0.22F, 1.45F);
                if (nextZoom != oldZoom) {
                    float localX = (float) mouseX - vpX;
                    float localY = (float) mouseY - vpY;
                    float worldX = (localX - this.panX) / oldZoom;
                    float worldY = (localY - this.panY) / oldZoom;
                    this.zoom = nextZoom;
                    this.panX = Mth.clamp(localX - worldX * nextZoom, -1350.0F, 1350.0F);
                    this.panY = Mth.clamp(localY - worldY * nextZoom, -980.0F, 980.0F);
                }
                return true;
            } else if (this.activeView == VIEW_ARCHIVES) {
                this.archiveScroll = Math.max(0, this.archiveScroll - (scrollY > 0 ? 1 : -1));
                return true;
            } else if (this.activeView == VIEW_ATLAS) {
                this.atlasScroll = Math.max(0, this.atlasScroll - (scrollY > 0 ? 1 : -1));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
