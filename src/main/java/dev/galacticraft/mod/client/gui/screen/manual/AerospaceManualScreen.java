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

import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.mod.attachments.GCAttachments;
import dev.galacticraft.mod.attachments.ManualReaderAttachment;
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.rocket.part.RocketMaterialRegistry;
import dev.galacticraft.mod.manual.ManualCatalog;
import dev.galacticraft.mod.manual.ManualEntry;
import dev.galacticraft.mod.manual.ManualManager;
import dev.galacticraft.mod.manual.ManualPage;
import dev.galacticraft.mod.manual.ManualTab;
import dev.galacticraft.mod.network.c2s.UpdateManualStatePayload;
import dev.galacticraft.mod.research.ClientResearchCache;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchNode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Interactive 6-Tab Sci-Fi Aerospace Manual ({@code AEROSPACE_MANUAL}) Screen.
 *
 * <ul>
 *   <li><b>Tab 1 (Mission):</b> "What's My Next Step?" Era advisor + Live Pre-Launch Checklists.</li>
 *   <li><b>Tab 2 (Materials):</b> 14 Tinkers' Structural Materials + Interactive Planetary Flight Viability Simulator.</li>
 *   <li><b>Tab 3 (Machines):</b> Ponder-style 3D Multiblock viewer + Chemical Flowcharts + Live Recipes.</li>
 *   <li><b>Tab 4 (Atlas):</b> 10 Solar System bodies with live telemetry &amp; Y-depth Ore Stratigraphy charts.</li>
 *   <li><b>Tab 5 (Lab Log):</b> All 55 research nodes organized by planetary location ({@code ✔}/{@code ▶}/{@code 🔒}).</li>
 *   <li><b>Tab 6 (Index):</b> Accent-insensitive global search, favorites/unread filters, item hyperlinks &amp; EMI/REI bridge.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
@SuppressWarnings("UnstableApiUsage")
public class AerospaceManualScreen extends Screen {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final String[] LAB_LOCATIONS = {
            "ALL", "Earth", "Moon", "0G / Asteroids", "Mars", "Mercury", "Venus", "Outer Giants"
    };

    // Preserved across re-opens during the same client session & mirrored live onto the 3D handheld tablet
    private static ManualTab sessionTab = ManualTab.MISSION;
    private static @Nullable ResourceLocation sessionEntryId = ManualCatalog.ENTRY_MISSION_OVERVIEW;
    private static float sessionScrollY = 0.0F;
    private static String lastFlightSummary = "Terra -> Lua | Ready to simulate";

    public static @NotNull ManualTab getSessionTab() {
        return sessionTab;
    }

    public static @Nullable ManualEntry getSessionEntry() {
        ManualEntry entry = ManualManager.INSTANCE.get(sessionEntryId);
        if (entry != null) {
            return entry;
        }
        List<ManualEntry> entries = ManualManager.INSTANCE.entriesForTab(sessionTab);
        return entries.isEmpty() ? null : entries.get(0);
    }

    public static int getCurrentEra() {
        int maxEra = 1;
        for (ResourceLocation id : ClientResearchCache.unlockedNodes()) {
            ResearchNode node = ResearchCatalog.get(id);
            if (node != null) {
                maxEra = Math.max(maxEra, node.era());
            }
        }
        return maxEra;
    }

    public static @NotNull String getLastFlightSummary() {
        return lastFlightSummary;
    }

    public static void updateLastFlightSummary(@NotNull String summary) {
        lastFlightSummary = summary;
    }

    private final Deque<ResourceLocation> backHistory = new ArrayDeque<>();
    private final Deque<ResourceLocation> forwardHistory = new ArrayDeque<>();
    private boolean openAnimationTriggered = false;

    private ManualTab activeTab;
    private @Nullable ManualEntry selectedEntry;

    // Responsive panel geometry
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int sidebarX;
    private int sidebarW;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    // Viewport scrolling & pre-measured layout blocks (NEVER recomputed in render())
    private float scrollY = 0.0F;
    private float targetScrollY = 0.0F;
    private int contentTotalHeight = 0;
    private int sidebarScroll = 0;
    private final List<MeasuredBlock> measuredBlocks = new ArrayList<>();
    private final List<ClickZone> activeClickZones = new ArrayList<>();

    // Interactive widgets
    private @Nullable EditBox searchBox;
    private String searchQuery = "";
    private boolean filterFavoritesOnly = false;
    private boolean filterUnreadOnly = false;
    private int labLocationIdx = 0;
    private @Nullable FlightViabilityCalculatorWidget flightCalculator;
    private final List<Multiblock3DRendererWidget> multiblockWidgets = new ArrayList<>();

    // Deferred tooltip state (always flushed AFTER disableScissor())
    private @Nullable ItemStack hoveredTooltipStack;
    private @Nullable List<Component> hoveredTooltipLines;

    public AerospaceManualScreen(@Nullable ResourceLocation initialEntryId) {
        super(Component.translatable("manual.galacticraft.title"));
        if (initialEntryId != null) {
            ManualEntry target = ManualManager.INSTANCE.get(initialEntryId);
            if (target != null) {
                this.activeTab = target.tab();
                this.selectedEntry = target;
            } else {
                this.activeTab = sessionTab;
                this.selectedEntry = ManualManager.INSTANCE.get(sessionEntryId);
            }
        } else {
            this.activeTab = sessionTab;
            this.selectedEntry = ManualManager.INSTANCE.get(sessionEntryId);
            this.scrollY = sessionScrollY;
            this.targetScrollY = sessionScrollY;
        }
        if (this.selectedEntry == null) {
            List<ManualEntry> entries = ManualManager.INSTANCE.entriesForTab(this.activeTab);
            if (!entries.isEmpty()) {
                this.selectedEntry = entries.get(0);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        if (!this.openAnimationTriggered) {
            this.openAnimationTriggered = true;
            AerospaceTabletAnimator.beginOpen();
        }
        this.panelW = Math.min(416, Math.max(296, this.width - 36));
        this.panelH = Math.min(250, Math.max(196, this.height - 34));
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        this.sidebarX = this.panelX + 6;
        this.sidebarW = Math.min(120, Math.max(96, this.panelW / 3 - 14));
        this.contentX = this.sidebarX + this.sidebarW + 6;
        this.contentY = this.panelY + 42;
        this.contentW = this.panelX + this.panelW - this.contentX - 6;
        this.contentH = this.panelY + this.panelH - this.contentY - 6;

        int searchW = Math.min(110, Math.max(72, this.panelW / 4));
        this.searchBox = new EditBox(this.font, this.panelX + this.panelW - searchW - 24, this.panelY + 5, searchW, 13,
                Component.translatable("manual.galacticraft.search"));
        this.searchBox.setBordered(true);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setHint(Component.translatable("manual.galacticraft.search_hint"));
        this.searchBox.setResponder(q -> {
            this.searchQuery = q;
            this.sidebarScroll = 0;
            if (!q.isBlank() && this.activeTab != ManualTab.INDEX) {
                this.activeTab = ManualTab.INDEX;
                rebuildLayout();
            } else if (this.activeTab == ManualTab.INDEX) {
                rebuildLayout();
            }
        });
        this.addRenderableWidget(this.searchBox);

        if (this.selectedEntry != null) {
            markEntryRead(this.selectedEntry.id());
        }
        rebuildLayout();
    }

    @Override
    public void resize(@NotNull Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        rebuildLayout();
    }

    @Override
    public void onClose() {
        sessionTab = this.activeTab;
        sessionEntryId = this.selectedEntry != null ? this.selectedEntry.id() : sessionEntryId;
        sessionScrollY = this.targetScrollY;
        AerospaceTabletAnimator.beginClose();
        super.onClose();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally skip Minecraft 1.21's Gaussian menu blur so the 3D tablet and text stay 100% razor sharp!
        float reveal = AerospaceTabletAnimator.screenRevealAlpha();
        if (reveal > 0.01F) {
            int alpha = Math.round(reveal * 96.0F);
            graphics.fill(0, 0, this.width, this.height, (alpha << 24) | 0x040914);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =========================================================================
    // NAVIGATION & PERSISTED PLAYER ATTACHMENT STATE
    // =========================================================================

    private void selectTab(@NotNull ManualTab tab) {
        if (this.activeTab == tab) {
            return;
        }
        this.activeTab = tab;
        sessionTab = tab;
        this.sidebarScroll = 0;
        this.scrollY = 0.0F;
        this.targetScrollY = 0.0F;
        List<ManualEntry> entries = ManualManager.INSTANCE.entriesForTab(tab);
        if (!entries.isEmpty()) {
            this.selectedEntry = entries.get(0);
            sessionEntryId = this.selectedEntry.id();
            markEntryRead(this.selectedEntry.id());
        }
        rebuildLayout();
    }

    private void navigateTo(@NotNull ResourceLocation entryId, boolean recordHistory) {
        ManualEntry next = ManualManager.INSTANCE.get(entryId);
        if (next == null) {
            return;
        }
        if (recordHistory && this.selectedEntry != null && !this.selectedEntry.id().equals(entryId)) {
            this.backHistory.push(this.selectedEntry.id());
            this.forwardHistory.clear();
        }
        this.activeTab = next.tab();
        this.selectedEntry = next;
        sessionTab = next.tab();
        sessionEntryId = next.id();
        this.scrollY = 0.0F;
        this.targetScrollY = 0.0F;
        markEntryRead(entryId);
        rebuildLayout();
    }

    private void navigateBack() {
        if (this.backHistory.isEmpty()) {
            return;
        }
        ResourceLocation prev = this.backHistory.pop();
        if (this.selectedEntry != null) {
            this.forwardHistory.push(this.selectedEntry.id());
        }
        navigateTo(prev, false);
    }

    private void navigateForward() {
        if (this.forwardHistory.isEmpty()) {
            return;
        }
        ResourceLocation next = this.forwardHistory.pop();
        if (this.selectedEntry != null) {
            this.backHistory.push(this.selectedEntry.id());
        }
        navigateTo(next, false);
    }

    private ManualReaderAttachment readerState() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return ManualReaderAttachment.EMPTY;
        }
        ManualReaderAttachment attached = player.getAttached(GCAttachments.MANUAL_READER);
        return attached != null ? attached : ManualReaderAttachment.EMPTY;
    }

    private void applyLocalReaderState(ManualReaderAttachment updated) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.setAttached(GCAttachments.MANUAL_READER, updated);
        }
    }

    private void markEntryRead(@NotNull ResourceLocation entryId) {
        ManualReaderAttachment cur = readerState();
        if (!cur.isRead(entryId)) {
            applyLocalReaderState(cur.withRead(entryId));
            ClientPlayNetworking.send(UpdateManualStatePayload.markRead(entryId));
        }
    }

    private void toggleFavorite(@NotNull ResourceLocation entryId) {
        applyLocalReaderState(readerState().withFavoriteToggled(entryId));
        ClientPlayNetworking.send(UpdateManualStatePayload.toggleFavorite(entryId));
    }

    private void pinMission(@NotNull ManualReaderAttachment.PinnedMission mission) {
        applyLocalReaderState(readerState().withPinnedMission(mission));
        ClientPlayNetworking.send(UpdateManualStatePayload.setPin(mission));
    }

    private void clearPin() {
        applyLocalReaderState(readerState().withPinnedMission(null));
        ClientPlayNetworking.send(UpdateManualStatePayload.clearPin());
    }

    private void togglePinCollapse() {
        applyLocalReaderState(readerState().withPinCollapsedToggled());
        ClientPlayNetworking.send(UpdateManualStatePayload.togglePinCollapse());
    }

    private int computeCurrentEra() {
        int maxEra = 1;
        for (ResourceLocation id : ClientResearchCache.unlockedNodes()) {
            ResearchNode node = ResearchCatalog.get(id);
            if (node != null) {
                maxEra = Math.max(maxEra, node.era());
            }
        }
        return maxEra;
    }

    // =========================================================================
    // PRE-MEASURED LAYOUT ENGINE (REBUILT ONLY ON TAB / ENTRY / RESIZE)
    // =========================================================================

    private void rebuildLayout() {
        this.measuredBlocks.clear();
        this.multiblockWidgets.clear();
        int blockW = Math.max(120, this.contentW - 14);

        if (this.activeTab == ManualTab.MISSION) {
            this.measuredBlocks.add(buildNextStepAdvisorBlock(blockW));
        }

        if (this.activeTab == ManualTab.LAB_LOG) {
            this.measuredBlocks.add(buildLabLogLocationBlock(blockW));
        }

        if (this.activeTab == ManualTab.INDEX) {
            this.measuredBlocks.add(buildGlobalIndexBlock(blockW));
        }

        ManualEntry entry = this.selectedEntry;
        if (entry == null) {
            recomputeTotalHeight();
            return;
        }

        boolean unlocked = entry.isUnlocked(ClientResearchCache.unlockedNodes());
        if (!unlocked) {
            this.measuredBlocks.add(buildLockedHintBlock(entry, blockW));
            recomputeTotalHeight();
            return;
        }

        for (ManualPage page : entry.pages()) {
            if (page instanceof ManualPage.TextPage textPage) {
                this.measuredBlocks.add(buildTextPageBlock(textPage, blockW));
            } else if (page instanceof ManualPage.CraftingPage craftingPage) {
                this.measuredBlocks.add(buildCraftingPageBlock(craftingPage, blockW));
            } else if (page instanceof ManualPage.InfuserPage infuserPage) {
                this.measuredBlocks.add(buildInfuserPageBlock(infuserPage, blockW));
            } else if (page instanceof ManualPage.PartMaterialPage matPage) {
                this.measuredBlocks.add(buildPartMaterialBlock(matPage, blockW));
            } else if (page instanceof ManualPage.Multiblock3DPage mbPage) {
                Multiblock3DRendererWidget widget = new Multiblock3DRendererWidget(mbPage);
                this.multiblockWidgets.add(widget);
                this.measuredBlocks.add(buildMultiblockBlock(mbPage, widget));
            } else if (page instanceof ManualPage.PlanetStratigraphyPage planetPage) {
                OreStratigraphyChartWidget widget = new OreStratigraphyChartWidget(planetPage);
                this.measuredBlocks.add(buildPlanetStratigraphyBlock(widget));
            } else if (page instanceof ManualPage.FlowchartPage flowPage) {
                this.measuredBlocks.add(buildFlowchartBlock(flowPage, blockW));
            } else if (page instanceof ManualPage.LaunchChecklistPage checkPage) {
                this.measuredBlocks.add(buildChecklistBlock(entry, checkPage, blockW));
            }
        }
        recomputeTotalHeight();
    }

    private void recomputeTotalHeight() {
        int total = 6;
        for (MeasuredBlock block : this.measuredBlocks) {
            total += block.height() + 8;
        }
        this.contentTotalHeight = total;
        int maxScroll = Math.max(0, this.contentTotalHeight - (this.contentH - 22));
        this.targetScrollY = Mth.clamp(this.targetScrollY, 0.0F, maxScroll);
        this.scrollY = Mth.clamp(this.scrollY, 0.0F, maxScroll);
    }

    private MeasuredBlock buildNextStepAdvisorBlock(int w) {
        int era = computeCurrentEra();
        ResearchNode recommended = findNextRecommendedNode();
        String headline = Component.translatable("manual.galacticraft.advisor.headline", era).getString();
        String detail = recommended != null
                ? ("Next Target: " + recommended.title() + " (" + recommended.sourceLabName()
                        + " | " + recommended.spCost() + " SP | " + recommended.locationRequirement().label() + ")")
                : Component.translatable("manual.galacticraft.advisor.complete").getString();
        List<FormattedCharSequence> wrapped = this.font.split(Component.literal(detail), w - 16);
        int h = 28 + wrapped.size() * 10 + (recommended != null ? 16 : 0);

        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            g.fill(x, y, x + bw, y + h, 0xCC0D2137);
            g.fill(x, y, x + bw, y + 1, 0xFF38BDF8);
            g.fill(x, y, x + 2, y + h, 0xFF38BDF8);
            String spInfo = "SP:" + ClientResearchCache.sciencePoints() + " | "
                    + ClientResearchCache.unlockedNodes().size() + "/" + ResearchCatalog.totalNodeCount();
            int spW = this.font.width(spInfo);
            String trimmedHeadline = this.font.plainSubstrByWidth(headline, Math.max(40, bw - spW - 18));
            g.drawString(this.font, trimmedHeadline, x + 7, y + 5, 0xFF7DD3FC, false);
            g.drawString(this.font, spInfo, x + bw - spW - 6, y + 5, 0xFF94A3B8, false);

            int cy = y + 18;
            for (FormattedCharSequence line : wrapped) {
                g.drawString(this.font, line, x + 7, cy, 0xFFE2E8F0, false);
                cy += 10;
            }
            if (recommended != null) {
                int bx = x + 7;
                int by = cy + 2;
                int btnW = Math.min(130, bw - 14);
                drawActionButton(g, bx, by, btnW, 13, "Pin Next Step to HUD", mx, my);
                this.activeClickZones.add(new ClickZone(bx, by, btnW, 13, () -> pinMission(
                        new ManualReaderAttachment.PinnedMission(
                                ManualCatalog.ENTRY_MISSION_OVERVIEW,
                                "Next: " + recommended.title(),
                                List.of("Lab: " + recommended.sourceLabName(),
                                        "Loc: " + recommended.locationRequirement().label(),
                                        "Cost: " + recommended.spCost() + " SP (" + recommended.domain().shortCode() + ")"),
                                List.of(recommended.iconItemId()),
                                false
                        )
                )));
            }
        });
    }

    private @Nullable ResearchNode findNextRecommendedNode() {
        Set<ResourceLocation> unlocked = ClientResearchCache.unlockedNodes();
        ResearchNode best = null;
        for (ResearchNode node : ResearchCatalog.allNodes()) {
            if (unlocked.contains(node.id())) {
                continue;
            }
            boolean prereqsMet = true;
            for (ResourceLocation p : node.prerequisites()) {
                if (!unlocked.contains(p)) {
                    prereqsMet = false;
                    break;
                }
            }
            if (prereqsMet) {
                if (best == null || node.era() < best.era() || (node.era() == best.era() && node.spCost() < best.spCost())) {
                    best = node;
                }
            }
        }
        return best;
    }

    private MeasuredBlock buildLockedHintBlock(ManualEntry entry, int w) {
        ResearchNode reqNode = ResearchCatalog.get(entry.requiredResearchId());
        String lockTitle = Component.translatable("manual.galacticraft.locked.banner").getString();
        String hintText = Component.translatable(entry.lockHintKey()).getString();
        List<FormattedCharSequence> wrappedHint = this.font.split(Component.literal(hintText), w - 16);
        int h = 64 + wrappedHint.size() * 10 + (reqNode != null ? 30 : 0);

        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            g.fill(x, y, x + bw, y + h, 0xCC1E1428);
            g.fill(x, y, x + bw, y + 1, 0xFFF59E0B);
            g.fill(x, y, x + 2, y + h, 0xFFF59E0B);
            g.drawString(this.font, lockTitle, x + 8, y + 6, 0xFFFBBF24, false);
            g.drawString(this.font, Component.translatable(entry.titleKey()), x + 8, y + 18, 0xFFF1F5F9, false);

            int cy = y + 32;
            for (FormattedCharSequence line : wrappedHint) {
                g.drawString(this.font, line, x + 8, cy, 0xFFE2E8F0, false);
                cy += 10;
            }
            if (reqNode != null) {
                cy += 6;
                g.drawString(this.font, "Research Node: " + reqNode.title(), x + 8, cy, 0xFF7DD3FC, false);
                g.drawString(this.font, "Lab: " + reqNode.sourceLabName() + " | " + reqNode.locationRequirement().label(),
                        x + 8, cy + 11, 0xFF94A3B8, false);
                g.drawString(this.font, "Cost: " + reqNode.spCost() + " SP + " + reqNode.requiredDomainPoints()
                        + " " + reqNode.domain().shortCode(), x + 8, cy + 22, 0xFFFBBF24, false);
            }
        });
    }

    private MeasuredBlock buildTextPageBlock(ManualPage.TextPage page, int w) {
        boolean hasHeading = !page.headingKey().isEmpty();
        List<FormattedCharSequence> bodyLines = this.font.split(Component.translatable(page.bodyKey()), w - 12);
        boolean hasCallout = !page.calloutKey().isEmpty();
        List<FormattedCharSequence> calloutLines = hasCallout
                ? this.font.split(Component.translatable(page.calloutKey()), w - 22)
                : List.of();
        boolean hasItems = !page.showcaseItems().isEmpty();
        boolean hasLinks = !page.linkEntries().isEmpty();

        int h = 6 + (hasHeading ? 15 : 0) + bodyLines.size() * 10 + 4
                + (hasCallout ? calloutLines.size() * 10 + 14 : 0)
                + (hasItems ? 24 : 0)
                + (hasLinks ? 18 : 0);

        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            int cy = y;
            if (hasHeading) {
                g.drawString(this.font, Component.translatable(page.headingKey()), x + 2, cy, 0xFF7DD3FC, false);
                g.fill(x + 2, cy + 11, x + bw - 2, cy + 12, 0x5538BDF8);
                cy += 15;
            }
            for (FormattedCharSequence line : bodyLines) {
                g.drawString(this.font, line, x + 2, cy, 0xFFE2E8F0, false);
                cy += 10;
            }
            cy += 4;
            if (hasCallout) {
                int boxH = calloutLines.size() * 10 + 8;
                int accent = switch (page.calloutStyle().toLowerCase(Locale.ROOT)) {
                    case "warn" -> 0xFFF59E0B;
                    case "tip" -> 0xFF10B981;
                    default -> 0xFF38BDF8;
                };
                int bg = switch (page.calloutStyle().toLowerCase(Locale.ROOT)) {
                    case "warn" -> 0xBB2B1D0A;
                    case "tip" -> 0xBB08241C;
                    default -> 0xBB0C1F33;
                };
                g.fill(x + 2, cy, x + bw - 2, cy + boxH, bg);
                g.fill(x + 2, cy, x + 4, cy + boxH, accent);
                int ty = cy + 4;
                for (FormattedCharSequence line : calloutLines) {
                    g.drawString(this.font, line, x + 8, ty, 0xFFF1F5F9, false);
                    ty += 10;
                }
                cy += boxH + 6;
            }
            if (hasItems) {
                int ix = x + 4;
                for (ResourceLocation itemId : page.showcaseItems()) {
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    if (item == Items.AIR) {
                        continue;
                    }
                    ItemStack stack = new ItemStack(item);
                    g.fill(ix - 1, cy - 1, ix + 17, cy + 17, 0xFF132238);
                    g.renderOutline(ix - 1, cy - 1, 18, 18, 0xFF2B4C73);
                    g.renderFakeItem(stack, ix, cy);
                    if (mx >= ix && mx < ix + 16 && my >= cy && my < cy + 16) {
                        this.hoveredTooltipStack = stack;
                    }
                    Optional<ResourceLocation> target = ManualManager.INSTANCE.findEntryForItemId(itemId);
                    if (target.isPresent()) {
                        ResourceLocation dest = target.get();
                        this.activeClickZones.add(new ClickZone(ix, cy, 16, 16, () -> navigateTo(dest, true)));
                    }
                    ix += 22;
                    if (ix + 18 > x + bw) {
                        break;
                    }
                }
                cy += 24;
            }
            if (hasLinks) {
                int lx = x + 2;
                for (ResourceLocation targetId : page.linkEntries()) {
                    ManualEntry target = ManualManager.INSTANCE.get(targetId);
                    if (target == null) {
                        continue;
                    }
                    String lbl = "-> " + Component.translatable(target.titleKey()).getString();
                    int btnW = Math.min(bw - 4, this.font.width(lbl) + 10);
                    if (lx + btnW > x + bw) {
                        break;
                    }
                    drawActionButton(g, lx, cy, btnW, 13, lbl, mx, my);
                    this.activeClickZones.add(new ClickZone(lx, cy, btnW, 13, () -> navigateTo(targetId, true)));
                    lx += btnW + 4;
                }
            }
        });
    }

    private MeasuredBlock buildCraftingPageBlock(ManualPage.CraftingPage page, int w) {
        // Resolve recipe ONCE during layout build (never in render())
        ClientLevel level = Minecraft.getInstance().level;
        Recipe<?> resolved = null;
        ItemStack resultStack = ItemStack.EMPTY;
        List<Ingredient> ingredients = List.of();
        int gridW = 3;

        if (level != null) {
            Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(page.recipeId());
            if (holder.isPresent()) {
                resolved = holder.get().value();
                RegistryAccess access = level.registryAccess();
                resultStack = resolved.getResultItem(access);
                ingredients = resolved.getIngredients();
                if (resolved instanceof ShapedRecipe shaped) {
                    gridW = shaped.getWidth();
                }
            }
        }

        final Recipe<?> finalRecipe = resolved;
        final ItemStack finalResult = resultStack;
        final List<Ingredient> finalIngredients = ingredients;
        final int finalGridW = Math.max(1, gridW);
        int h = finalRecipe != null ? 92 : 34;

        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            g.fill(x, y, x + bw, y + h, 0xCC0D1829);
            g.fill(x, y, x + bw, y + 1, 0xFF38BDF8);
            String title = page.titleKey().isEmpty()
                    ? page.recipeId().getPath()
                    : Component.translatable(page.titleKey()).getString();
            g.drawString(this.font, title, x + 6, y + 5, 0xFF7DD3FC, false);

            if (finalRecipe == null) {
                g.drawString(this.font, Component.translatable("manual.galacticraft.recipe.unavailable", page.recipeId().toString()),
                        x + 6, y + 18, 0xFFF59E0B, false);
                return;
            }

            int gx = x + 10;
            int gy = y + 20;
            for (int slot = 0; slot < Math.min(9, finalIngredients.size()); slot++) {
                int col = slot % finalGridW;
                int row = slot / finalGridW;
                int sx = gx + col * 19;
                int sy = gy + row * 19;
                g.fill(sx, sy, sx + 17, sy + 17, 0xFF132238);
                g.renderOutline(sx, sy, 17, 17, 0xFF2B4C73);
                ItemStack ingStack = pickIngredientStack(finalIngredients.get(slot));
                if (!ingStack.isEmpty()) {
                    g.renderFakeItem(ingStack, sx + 1, sy + 1);
                    if (mx >= sx && mx < sx + 17 && my >= sy && my < sy + 17) {
                        this.hoveredTooltipStack = ingStack;
                    }
                    Optional<ResourceLocation> linked = ManualManager.INSTANCE.findEntryForItem(ingStack.getItem());
                    if (linked.isPresent()) {
                        ResourceLocation dest = linked.get();
                        this.activeClickZones.add(new ClickZone(sx, sy, 17, 17, () -> navigateTo(dest, true)));
                    }
                }
            }

            // Arrow + result slot
            int ax = gx + 68;
            int ay = gy + 20;
            g.drawString(this.font, "=>", ax, ay + 4, 0xFF38BDF8, false);
            int rx = ax + 22;
            g.fill(rx - 1, ay - 1, rx + 19, ay + 19, 0xFF15324E);
            g.renderOutline(rx - 1, ay - 1, 20, 20, 0xFF38BDF8);
            g.renderFakeItem(finalResult, rx + 1, ay + 1);
            g.renderItemDecorations(this.font, finalResult, rx + 1, ay + 1);
            if (mx >= rx && mx < rx + 18 && my >= ay && my < ay + 18) {
                this.hoveredTooltipStack = finalResult;
            }

            // Pin recipe button
            int pinX = rx + 28;
            int pinW = Math.min(72, x + bw - pinX - 6);
            if (pinW >= 42) {
                drawActionButton(g, pinX, ay + 2, pinW, 13, "Pin HUD", mx, my);
                this.activeClickZones.add(new ClickZone(pinX, ay + 2, pinW, 13, () -> {
                    Set<ResourceLocation> tracked = new LinkedHashSet<>();
                    List<String> summary = new ArrayList<>();
                    summary.add("Craft: " + finalResult.getHoverName().getString());
                    for (Ingredient ing : finalIngredients) {
                        ItemStack s = pickIngredientStack(ing);
                        if (!s.isEmpty()) {
                            tracked.add(BuiltInRegistries.ITEM.getKey(s.getItem()));
                        }
                    }
                    ResourceLocation eid = this.selectedEntry != null ? this.selectedEntry.id() : ManualCatalog.ENTRY_MISSION_OVERVIEW;
                    pinMission(new ManualReaderAttachment.PinnedMission(eid, title, summary, new ArrayList<>(tracked), false));
                }));
            }

            if (!page.captionKey().isEmpty()) {
                g.drawString(this.font, this.font.plainSubstrByWidth(Component.translatable(page.captionKey()).getString(), bw - 12),
                        x + 6, y + h - 11, 0xFF94A3B8, false);
            }
        });
    }

    private MeasuredBlock buildInfuserPageBlock(ManualPage.InfuserPage page, int w) {
        int h = 68;
        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            g.fill(x, y, x + bw, y + h, 0xCC0D1829);
            g.fill(x, y, x + bw, y + 1, 0xFF10B981);
            String title = page.titleKey().isEmpty()
                    ? Component.translatable(page.machineKey()).getString()
                    : Component.translatable(page.titleKey()).getString();
            g.drawString(this.font, title, x + 6, y + 5, 0xFF34D399, false);
            String mach = Component.translatable(page.machineKey()).getString();
            g.drawString(this.font, mach, x + bw - this.font.width(mach) - 6, y + 5, 0xFF94A3B8, false);

            int sx = x + 10;
            int sy = y + 22;
            for (ResourceLocation inId : page.inputItems()) {
                Item item = BuiltInRegistries.ITEM.get(inId);
                if (item != Items.AIR) {
                    ItemStack st = new ItemStack(item);
                    g.fill(sx, sy, sx + 18, sy + 18, 0xFF132238);
                    g.renderOutline(sx, sy, 18, 18, 0xFF2B4C73);
                    g.renderFakeItem(st, sx + 1, sy + 1);
                    if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                        this.hoveredTooltipStack = st;
                    }
                    sx += 22;
                }
            }
            g.drawString(this.font, "=>", sx + 4, sy + 5, 0xFF34D399, false);
            int ox = sx + 24;
            Item outItem = BuiltInRegistries.ITEM.get(page.outputItem());
            if (outItem != Items.AIR) {
                ItemStack outStack = new ItemStack(outItem);
                g.fill(ox, sy, ox + 18, sy + 18, 0xFF133329);
                g.renderOutline(ox, sy, 18, 18, 0xFF10B981);
                g.renderFakeItem(outStack, ox + 1, sy + 1);
                if (mx >= ox && mx < ox + 18 && my >= sy && my < sy + 18) {
                    this.hoveredTooltipStack = outStack;
                }
            }
            if (!page.processNoteKey().isEmpty()) {
                g.drawString(this.font, this.font.plainSubstrByWidth(Component.translatable(page.processNoteKey()).getString(), bw - 12),
                        x + 6, y + h - 13, 0xFFCBD5E1, false);
            }
        });
    }

    private MeasuredBlock buildPartMaterialBlock(ManualPage.PartMaterialPage page, int w) {
        if (page.includeFlightSimulator()) {
            if (this.flightCalculator == null) {
                this.flightCalculator = new FlightViabilityCalculatorWidget();
            }
            FlightViabilityCalculatorWidget calc = this.flightCalculator;
            int h = calc.preferredHeight() + 18;
            return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
                calc.render(g, this.font, x, y, bw, mx, my);
                int by = y + calc.preferredHeight() + 3;
                drawActionButton(g, x + 4, by, 128, 13, "Pin Flight Plan to HUD", mx, my);
                this.activeClickZones.add(new ClickZone(x + 4, by, 128, 13, () -> pinMission(
                        new ManualReaderAttachment.PinnedMission(
                                ManualCatalog.ENTRY_MATERIALS_SIMULATOR,
                                "Flight Plan",
                                calc.buildPinSummaryLines(),
                                List.of(),
                                false
                        )
                )));
            });
        }

        List<RocketMaterialProfile> profiles = new ArrayList<>();
        if (page.materialIds().isEmpty()) {
            profiles.addAll(RocketMaterialRegistry.structuralProfiles());
        } else {
            for (ResourceLocation id : page.materialIds()) {
                RocketMaterialRegistry.getById(id).ifPresent(profiles::add);
            }
        }
        int rowH = 34;
        int h = 18 + profiles.size() * rowH;

        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            if (!page.titleKey().isEmpty()) {
                g.drawString(this.font, Component.translatable(page.titleKey()), x + 2, y + 2, 0xFFFBBF24, false);
            }
            int cy = y + 16;
            for (RocketMaterialProfile prof : profiles) {
                g.fill(x, cy, x + bw, cy + rowH - 3, 0xCC0D1829);
                g.fill(x, cy, x + 2, cy + rowH - 3, 0xFF000000 | (prof.rgbColor() & 0x00FFFFFF));
                ItemStack icon = RocketMaterialRegistry.createDisplayStack(prof);
                if (!icon.isEmpty()) {
                    g.renderFakeItem(icon, x + 5, cy + 7);
                    if (mx >= x + 5 && mx < x + 21 && my >= cy + 7 && my < cy + 23) {
                        this.hoveredTooltipStack = icon;
                    }
                }
                g.drawString(this.font, prof.displayName(), x + 25, cy + 4, 0xFFF1F5F9, false);
                String stats = String.format(Locale.ROOT, "Mass %+.0f%% | %+.0fK | %.2fx atm | %.2fx HP",
                        prof.massModifier() * 100.0, prof.thermalBonusKelvin(), prof.pressureMultiplier(), prof.integrityMultiplier());
                g.drawString(this.font, this.font.plainSubstrByWidth(stats, bw - 30), x + 25, cy + 14, 0xFF94A3B8, false);
                if (!prof.traits().isEmpty()) {
                    StringBuilder tb = new StringBuilder();
                    for (RocketMaterialTrait t : prof.traits()) {
                        if (!tb.isEmpty()) {
                            tb.append(", ");
                        }
                        tb.append(t.displayName());
                    }
                    g.drawString(this.font, this.font.plainSubstrByWidth("Trait: " + tb, bw - 30), x + 25, cy + 23, 0xFF38BDF8, false);
                }
                cy += rowH;
            }
        });
    }

    private MeasuredBlock buildMultiblockBlock(ManualPage.Multiblock3DPage page, Multiblock3DRendererWidget widget) {
        int h = widget.preferredHeight() + 18;
        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            widget.render(g, this.font, x, y, bw, mx, my);
            int by = y + widget.preferredHeight() + 3;
            drawActionButton(g, x + 4, by, 132, 13, "Pin Multiblock to HUD", mx, my);
            this.activeClickZones.add(new ClickZone(x + 4, by, 132, 13, () -> {
                ResourceLocation eid = this.selectedEntry != null ? this.selectedEntry.id() : ManualCatalog.ENTRY_MULTIBLOCK_LAUNCH_PAD;
                pinMission(new ManualReaderAttachment.PinnedMission(
                        eid,
                        Component.translatable(page.titleKey()).getString(),
                        widget.materialSummaryLines(),
                        widget.materialBlockIds(),
                        false
                ));
            }));
        });
    }

    private MeasuredBlock buildPlanetStratigraphyBlock(OreStratigraphyChartWidget widget) {
        int h = widget.preferredHeight();
        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            List<Component> tip = new ArrayList<>();
            widget.render(g, this.font, x, y, bw, mx, my, tip);
            if (!tip.isEmpty()) {
                this.hoveredTooltipLines = tip;
            }
        });
    }

    private MeasuredBlock buildFlowchartBlock(ManualPage.FlowchartPage page, int w) {
        int stepH = 26;
        int h = 24 + page.steps().size() * stepH + (page.captionKey().isEmpty() ? 0 : 14);
        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            g.fill(x, y, x + bw, y + h, 0xCC0D1829);
            g.fill(x, y, x + bw, y + 1, 0xFF38BDF8);
            g.drawString(this.font, Component.translatable(page.titleKey()), x + 6, y + 5, 0xFF7DD3FC, false);

            int cy = y + 19;
            for (int i = 0; i < page.steps().size(); i++) {
                ManualPage.FlowchartPage.FlowStep step = page.steps().get(i);
                g.fill(x + 6, cy, x + bw - 6, cy + 20, 0xFF132238);
                g.fill(x + 6, cy, x + 8, cy + 20, step.accentColor());
                Item item = BuiltInRegistries.ITEM.get(step.iconItem());
                if (item != Items.AIR) {
                    ItemStack st = new ItemStack(item);
                    g.renderFakeItem(st, x + 11, cy + 2);
                    if (mx >= x + 11 && mx < x + 27 && my >= cy + 2 && my < cy + 18) {
                        this.hoveredTooltipStack = st;
                    }
                }
                String title = Component.translatable(step.titleKey()).getString();
                g.drawString(this.font, this.font.plainSubstrByWidth(title, bw - 38), x + 30, cy + 2, 0xFFF1F5F9, false);
                if (!step.detailKey().isEmpty()) {
                    String det = Component.translatable(step.detailKey()).getString();
                    g.drawString(this.font, this.font.plainSubstrByWidth(det, bw - 38), x + 30, cy + 11, 0xFF94A3B8, false);
                }
                if (i + 1 < page.steps().size()) {
                    g.drawString(this.font, "v", x + 16, cy + 19, 0xFF38BDF8, false);
                }
                cy += stepH;
            }
            if (!page.captionKey().isEmpty()) {
                g.drawString(this.font, this.font.plainSubstrByWidth(Component.translatable(page.captionKey()).getString(), bw - 12),
                        x + 6, y + h - 11, 0xFF94A3B8, false);
            }
        });
    }

    private MeasuredBlock buildChecklistBlock(ManualEntry entry, ManualPage.LaunchChecklistPage page, int w) {
        int h = 36 + page.requirements().size() * 18;
        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            g.fill(x, y, x + bw, y + h, 0xCC0D1829);
            g.fill(x, y, x + bw, y + 1, 0xFF38BDF8);
            String title = Component.translatable(page.titleKey()).getString();
            g.drawString(this.font, title, x + 6, y + 5, 0xFF7DD3FC, false);

            drawActionButton(g, x + bw - 74, y + 3, 68, 12, "Pin HUD", mx, my);
            this.activeClickZones.add(new ClickZone(x + bw - 74, y + 3, 68, 12, () -> {
                List<String> lines = new ArrayList<>();
                List<ResourceLocation> tracked = new ArrayList<>();
                for (ManualPage.LaunchChecklistPage.ChecklistRequirement req : page.requirements()) {
                    lines.add(Component.translatable(req.labelKey()).getString());
                    if (!req.acceptedItems().isEmpty()) {
                        tracked.add(req.acceptedItems().get(0));
                    }
                }
                pinMission(new ManualReaderAttachment.PinnedMission(entry.id(), title, lines, tracked, false));
            }));

            Player player = Minecraft.getInstance().player;
            int cy = y + 21;
            for (ManualPage.LaunchChecklistPage.ChecklistRequirement req : page.requirements()) {
                int owned = 0;
                ItemStack preview = ItemStack.EMPTY;
                for (ResourceLocation itemId : req.acceptedItems()) {
                    Item it = BuiltInRegistries.ITEM.get(itemId);
                    if (it != Items.AIR) {
                        if (preview.isEmpty()) {
                            preview = new ItemStack(it);
                        }
                        if (player != null) {
                            owned += PinnedMissionHudOverlay.countItemInPlayer(player, it);
                        }
                    }
                }
                boolean ok = owned >= req.minCount();
                if (!preview.isEmpty()) {
                    g.pose().pushPose();
                    g.pose().translate(x + 6, cy - 1, 0);
                    g.pose().scale(0.75F, 0.75F, 1.0F);
                    g.renderFakeItem(preview, 0, 0);
                    g.pose().popPose();
                }
                String label = Component.translatable(req.labelKey()).getString();
                g.drawString(this.font, this.font.plainSubstrByWidth(label, bw - 68), x + 22, cy + 2,
                        ok ? 0xFF4ADE80 : (req.critical() ? 0xFFFCA5A5 : 0xFFFBBF24), false);
                String status = ok ? ("✔ " + owned + "/" + req.minCount()) : (req.critical() ? "✖ MISSING" : "○ OPT");
                g.drawString(this.font, status, x + bw - this.font.width(status) - 6, cy + 2,
                        ok ? 0xFF4ADE80 : (req.critical() ? 0xFFF87171 : 0xFFFBBF24), false);
                cy += 18;
            }
        });
    }

    private MeasuredBlock buildLabLogLocationBlock(int w) {
        List<ResearchNode> filtered = new ArrayList<>();
        for (ResearchNode node : ResearchCatalog.allNodes()) {
            if (matchesLabLocationFilter(node, this.labLocationIdx)) {
                filtered.add(node);
            }
        }
        int h = 42 + filtered.size() * 20;

        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            g.fill(x, y, x + bw, y + h, 0xCC0D1829);
            g.fill(x, y, x + bw, y + 1, 0xFFEC4899);
            g.drawString(this.font, Component.translatable("manual.galacticraft.lablog.header"), x + 6, y + 5, 0xFFF472B6, false);

            // Location filter pill buttons
            int fx = x + 6;
            int fy = y + 18;
            for (int i = 0; i < LAB_LOCATIONS.length; i++) {
                String loc = LAB_LOCATIONS[i];
                int pw = this.font.width(loc) + 6;
                if (fx + pw > x + bw - 4) {
                    break;
                }
                boolean sel = i == this.labLocationIdx;
                g.fill(fx, fy, fx + pw, fy + 12, sel ? 0xFF831843 : 0xFF132238);
                g.renderOutline(fx, fy, pw, 12, sel ? 0xFFEC4899 : 0xFF2B4C73);
                g.drawString(this.font, loc, fx + 3, fy + 2, sel ? 0xFFFFFFFF : 0xFF94A3B8, false);
                int idx = i;
                this.activeClickZones.add(new ClickZone(fx, fy, pw, 12, () -> {
                    this.labLocationIdx = idx;
                    rebuildLayout();
                }));
                fx += pw + 3;
            }

            Set<ResourceLocation> unlocked = ClientResearchCache.unlockedNodes();
            int cy = y + 36;
            for (ResearchNode node : filtered) {
                boolean done = unlocked.contains(node.id());
                boolean prereqsOk = true;
                for (ResourceLocation p : node.prerequisites()) {
                    if (!unlocked.contains(p)) {
                        prereqsOk = false;
                        break;
                    }
                }
                boolean avail = !done && prereqsOk;
                String icon = done ? "✔" : (avail ? "▶" : "🔒");
                int color = done ? 0xFF4ADE80 : (avail ? 0xFF38BDF8 : 0xFF64748B);

                g.drawString(this.font, icon, x + 6, cy + 2, color, false);
                g.drawString(this.font, this.font.plainSubstrByWidth(node.title(), bw - 92), x + 18, cy + 2,
                        done ? 0xFFD1FAE5 : (avail ? 0xFFF1F5F9 : 0xFF94A3B8), false);
                String meta = "E" + node.era() + " " + node.spCost() + "SP";
                g.drawString(this.font, meta, x + bw - this.font.width(meta) - 6, cy + 2, color, false);
                g.drawString(this.font, this.font.plainSubstrByWidth(node.sourceLabName() + " • " + node.locationRequirement().label(), bw - 26),
                        x + 18, cy + 11, 0xFF64748B, false);

                List<ManualEntry> linkedEntries = ManualManager.INSTANCE.entriesUnlockedBy(node.id());
                if (!linkedEntries.isEmpty()) {
                    ResourceLocation target = linkedEntries.get(0).id();
                    this.activeClickZones.add(new ClickZone(x + 4, cy, bw - 8, 19, () -> navigateTo(target, true)));
                }
                cy += 20;
            }
        });
    }

    private static boolean matchesLabLocationFilter(ResearchNode node, int idx) {
        if (idx == 0) {
            return true;
        }
        String label = node.locationRequirement().label().toLowerCase(Locale.ROOT);
        List<ResourceLocation> bodies = node.locationRequirement().allowedBodies();
        return switch (idx) {
            case 1 -> label.contains("any") || bodies.contains(GCCelestialBodies.EARTH.location());
            case 2 -> label.contains("moon") || label.contains("vacuum") || bodies.contains(GCCelestialBodies.MOON.location());
            case 3 -> label.contains("0.0g") || label.contains("asteroid") || bodies.contains(GCCelestialBodies.ASTEROID.location());
            case 4 -> label.contains("mars") || bodies.contains(GCCelestialBodies.MARS.location());
            case 5 -> label.contains("mercury") || bodies.contains(GCCelestialBodies.MERCURY.location());
            case 6 -> label.contains("venus") || label.contains("hyperbaric") || bodies.contains(GCCelestialBodies.VENUS.location());
            case 7 -> label.contains("jupiter") || label.contains("saturn") || label.contains("uranus") || label.contains("neptune")
                    || bodies.contains(GCCelestialBodies.JUPITER.location())
                    || bodies.contains(GCCelestialBodies.SATURN.location())
                    || bodies.contains(GCCelestialBodies.URANUS.location())
                    || bodies.contains(GCCelestialBodies.NEPTUNE.location());
            default -> true;
        };
    }

    private MeasuredBlock buildGlobalIndexBlock(int w) {
        String normQuery = normalizeForSearch(this.searchQuery);
        ManualReaderAttachment state = readerState();
        List<ManualEntry> matches = new ArrayList<>();
        for (ManualEntry e : ManualManager.INSTANCE.allEntries()) {
            if (this.filterFavoritesOnly && !state.isFavorite(e.id())) {
                continue;
            }
            if (this.filterUnreadOnly && state.isRead(e.id())) {
                continue;
            }
            if (normQuery.isEmpty() || matchesQuery(e, normQuery)) {
                matches.add(e);
            }
        }
        String viewerName = RecipeViewerBridge.activeViewerName();
        int h = 38 + Math.max(1, matches.size()) * 20;

        return new MeasuredBlock(h, (g, x, y, bw, mx, my) -> {
            g.fill(x, y, x + bw, y + h, 0xCC0D1829);
            g.fill(x, y, x + bw, y + 1, 0xFF60A5FA);
            g.drawString(this.font, Component.translatable("manual.galacticraft.index.header", matches.size()), x + 6, y + 5, 0xFF93C5FD, false);

            // Filter toggles
            drawActionButton(g, x + 6, y + 17, 68, 12, (this.filterFavoritesOnly ? "★ " : "☆ ") + "Favorites", mx, my);
            this.activeClickZones.add(new ClickZone(x + 6, y + 17, 68, 12, () -> {
                this.filterFavoritesOnly = !this.filterFavoritesOnly;
                rebuildLayout();
            }));
            drawActionButton(g, x + 78, y + 17, 62, 12, (this.filterUnreadOnly ? "● " : "○ ") + "Unread", mx, my);
            this.activeClickZones.add(new ClickZone(x + 78, y + 17, 62, 12, () -> {
                this.filterUnreadOnly = !this.filterUnreadOnly;
                rebuildLayout();
            }));

            int cy = y + 34;
            if (matches.isEmpty()) {
                g.drawString(this.font, Component.translatable("manual.galacticraft.index.no_results"), x + 8, cy + 2, 0xFF94A3B8, false);
                return;
            }
            for (ManualEntry entry : matches) {
                boolean unlocked = entry.isUnlocked(ClientResearchCache.unlockedNodes());
                Item item = BuiltInRegistries.ITEM.get(entry.iconItem());
                ItemStack stack = item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
                if (!stack.isEmpty()) {
                    g.renderFakeItem(stack, x + 6, cy + 1);
                    if (mx >= x + 6 && mx < x + 22 && my >= cy + 1 && my < cy + 17) {
                        this.hoveredTooltipStack = stack;
                    }
                }
                String title = (unlocked ? "" : "🔒 ") + Component.translatable(entry.titleKey()).getString();
                int btnReserve = viewerName.isEmpty() ? 10 : 48;
                g.drawString(this.font, this.font.plainSubstrByWidth(title, bw - 30 - btnReserve), x + 26, cy + 5,
                        unlocked ? 0xFFE2E8F0 : 0xFFF59E0B, false);
                this.activeClickZones.add(new ClickZone(x + 4, cy, bw - btnReserve - 8, 18, () -> navigateTo(entry.id(), true)));

                if (!viewerName.isEmpty() && !stack.isEmpty()) {
                    int vx = x + bw - 42;
                    drawActionButton(g, vx, cy + 3, 36, 12, viewerName, mx, my);
                    this.activeClickZones.add(new ClickZone(vx, cy + 3, 36, 12, () -> RecipeViewerBridge.openRecipeFor(stack)));
                }
                cy += 20;
            }
        });
    }

    private static boolean matchesQuery(ManualEntry entry, String normQuery) {
        String title = normalizeForSearch(Component.translatable(entry.titleKey()).getString());
        if (title.contains(normQuery) || entry.id().getPath().contains(normQuery)) {
            return true;
        }
        for (ResourceLocation rel : entry.relatedItems()) {
            if (rel.getPath().contains(normQuery)) {
                return true;
            }
            Item item = BuiltInRegistries.ITEM.get(rel);
            if (item != Items.AIR && normalizeForSearch(item.getDescription().getString()).contains(normQuery)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeForSearch(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        return DIACRITICS.matcher(nfd).replaceAll("").toLowerCase(Locale.ROOT).trim();
    }

    public static ItemStack pickIngredientStack(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) {
            return ItemStack.EMPTY;
        }
        int idx = (int) ((System.currentTimeMillis() / 1000L) % items.length);
        return items[idx];
    }

    // =========================================================================
    // RENDERING (SCISSORED VIEWPORT + DEFERRED TOOLTIPS)
    // =========================================================================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.activeClickZones.clear();
        this.hoveredTooltipStack = null;
        this.hoveredTooltipLines = null;

        float reveal = AerospaceTabletAnimator.screenRevealAlpha();
        if (this.searchBox != null) {
            this.searchBox.setVisible(reveal >= 0.88F);
        }
        if (reveal < 0.04F) {
            // Let the first-person 3D two-handed tablet raise & camera zoom play cleanly without overlay clutter
            return;
        }

        // Subtle crisp dark edge tint (no Gaussian blur!)
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        float zoomScale = AerospaceTabletAnimator.screenZoomScale();
        boolean scaling = zoomScale < 0.999F;
        if (scaling) {
            graphics.pose().pushPose();
            float cx = this.panelX + this.panelW * 0.5F;
            float cy = this.panelY + this.panelH * 0.5F;
            graphics.pose().translate(cx, cy, 0.0F);
            graphics.pose().scale(zoomScale, zoomScale, 1.0F);
            graphics.pose().translate(-cx, -cy, 0.0F);
        }

        // Metallic 3D Aerospace Tablet outer chassis bezel + ergonomic left/right grips + camera/LEDs
        renderTabletChassisBezel(graphics);

        // Inner backlit glass screen frame
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, 0xF508101D);
        graphics.renderOutline(this.panelX, this.panelY, this.panelW, this.panelH, 0xFF38BDF8);
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 22, 0xFF0E1B2F);
        graphics.fill(this.panelX, this.panelY + 22, this.panelX + this.panelW, this.panelY + 23, 0xFF38BDF8);

        // Top bar: Back [<], Forward [>], Title, Era badge, Close [X]
        drawNavButton(graphics, this.panelX + 5, this.panelY + 5, 14, 13, "<", !this.backHistory.isEmpty(), mouseX, mouseY);
        drawNavButton(graphics, this.panelX + 21, this.panelY + 5, 14, 13, ">", !this.forwardHistory.isEmpty(), mouseX, mouseY);
        graphics.drawString(this.font, Component.translatable("manual.galacticraft.header", computeCurrentEra()),
                this.panelX + 40, this.panelY + 8, 0xFF7DD3FC, false);

        // Close button
        int closeX = this.panelX + this.panelW - 18;
        drawActionButton(graphics, closeX, this.panelY + 5, 13, 13, "X", mouseX, mouseY);

        // 6 Tab Strip
        ManualTab[] tabs = ManualTab.values();
        int tabAreaW = this.panelW - 12;
        int tabW = tabAreaW / tabs.length;
        int tabY = this.panelY + 25;
        for (int i = 0; i < tabs.length; i++) {
            ManualTab tab = tabs[i];
            int tx = this.panelX + 6 + i * tabW;
            boolean active = tab == this.activeTab;
            boolean hovered = inRect(mouseX, mouseY, tx, tabY, tabW - 2, 14);
            graphics.fill(tx, tabY, tx + tabW - 2, tabY + 14, active ? 0xFF17324F : (hovered ? 0xFF12263C : 0xFF0B1626));
            graphics.fill(tx, tabY, tx + tabW - 2, tabY + (active ? 2 : 1), active ? tab.accentColor() : 0xFF243B55);
            String label = Component.translatable(tab.translationKey()).getString();
            String trimmed = this.font.plainSubstrByWidth(label, tabW - 8);
            graphics.drawString(this.font, trimmed, tx + (tabW - 2 - this.font.width(trimmed)) / 2, tabY + 3,
                    active ? 0xFFFFFFFF : 0xFF94A3B8, false);
        }

        // Left Sidebar (Chapter List)
        renderSidebar(graphics, mouseX, mouseY);

        // Right Content Panel + Header (Breadcrumb, Favorite ★, Unpin HUD)
        renderContentPanel(graphics, mouseX, mouseY, partialTick);

        if (scaling) {
            graphics.pose().popPose();
        }

        // Render EditBox directly (never calling super.render's blur!)
        if (this.searchBox != null && this.searchBox.isVisible()) {
            this.searchBox.render(graphics, mouseX, mouseY, partialTick);
        }

        // Flush tooltips LAST (always outside enableScissor)
        if (!scaling) {
            if (this.hoveredTooltipStack != null && !this.hoveredTooltipStack.isEmpty()) {
                graphics.renderTooltip(this.font, this.hoveredTooltipStack, mouseX, mouseY);
            } else if (this.hoveredTooltipLines != null && !this.hoveredTooltipLines.isEmpty()) {
                graphics.renderComponentTooltip(this.font, this.hoveredTooltipLines, mouseX, mouseY);
            }
        }
    }

    private void renderTabletChassisBezel(GuiGraphics g) {
        int bx0 = this.panelX - 10;
        int by0 = this.panelY - 10;
        int bx1 = this.panelX + this.panelW + 10;
        int by1 = this.panelY + this.panelH + 10;

        // Outer metallic titanium shell
        g.fill(bx0, by0, bx1, by1, 0xFF1E2B3E);
        g.renderOutline(bx0, by0, bx1 - bx0, by1 - by0, 0xFF475E7A);
        g.fill(bx0 + 2, by0 + 2, bx1 - 2, by1 - 2, 0xFF27384F);

        // Left & right ergonomic rubberized grips
        g.fill(bx0 - 4, by0 + 16, bx0 + 3, by1 - 16, 0xFF111824);
        g.fill(bx0 - 4, this.panelY + this.panelH / 2 - 22, bx0 - 2, this.panelY + this.panelH / 2 + 22, 0xFF38BDF8);
        g.fill(bx1 - 3, by0 + 16, bx1 + 4, by1 - 16, 0xFF111824);
        g.fill(bx1 + 2, this.panelY + this.panelH / 2 - 22, bx1 + 4, this.panelY + this.panelH / 2 + 22, 0xFF38BDF8);

        // Top bezel camera lens + green power LED + bottom pill bar
        int midX = this.panelX + this.panelW / 2;
        g.fill(midX - 3, by0 + 3, midX + 3, by0 + 8, 0xFF0A101A);
        g.fill(midX - 1, by0 + 4, midX + 1, by0 + 6, 0xFF38BDF8);
        g.fill(midX + 10, by0 + 4, midX + 14, by0 + 7, 0xFF4ADE80);
        g.fill(midX - 24, by1 - 6, midX + 24, by1 - 4, 0xFF475E7A);
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(this.sidebarX, this.contentY, this.sidebarX + this.sidebarW, this.contentY + this.contentH, 0xCC0A1322);
        graphics.renderOutline(this.sidebarX, this.contentY, this.sidebarW, this.contentH, 0xFF1E3A5F);

        List<ManualEntry> entries = ManualManager.INSTANCE.entriesForTab(this.activeTab);
        ManualReaderAttachment state = readerState();
        Set<ResourceLocation> unlockedNodes = ClientResearchCache.unlockedNodes();

        graphics.enableScissor(this.sidebarX + 1, this.contentY + 1, this.sidebarX + this.sidebarW - 1, this.contentY + this.contentH - 1);
        try {
            int rowH = 20;
            int cursorY = this.contentY + 4 - this.sidebarScroll;
            for (ManualEntry entry : entries) {
                boolean selected = this.selectedEntry != null && this.selectedEntry.id().equals(entry.id());
                boolean unlocked = entry.isUnlocked(unlockedNodes);
                boolean hov = inRect(mouseX, mouseY, this.sidebarX + 2, cursorY, this.sidebarW - 4, rowH - 2);
                if (cursorY + rowH >= this.contentY && cursorY <= this.contentY + this.contentH) {
                    int bg = selected ? 0xFF1A3A5C : (hov ? 0xFF132842 : 0x00000000);
                    if (bg != 0) {
                        graphics.fill(this.sidebarX + 2, cursorY, this.sidebarX + this.sidebarW - 2, cursorY + rowH - 2, bg);
                    }
                    if (selected) {
                        graphics.fill(this.sidebarX + 2, cursorY, this.sidebarX + 4, cursorY + rowH - 2, this.activeTab.accentColor());
                    }
                    Item item = BuiltInRegistries.ITEM.get(entry.iconItem());
                    if (item != Items.AIR) {
                        graphics.pose().pushPose();
                        graphics.pose().translate(this.sidebarX + 6, cursorY + 2, 0);
                        graphics.pose().scale(0.75F, 0.75F, 1.0F);
                        graphics.renderFakeItem(new ItemStack(item), 0, 0);
                        graphics.pose().popPose();
                    }
                    String prefix = !unlocked ? "🔒 " : (state.isFavorite(entry.id()) ? "★ " : (!state.isRead(entry.id()) ? "● " : ""));
                    String text = prefix + Component.translatable(entry.titleKey()).getString();
                    int col = !unlocked ? 0xFFF59E0B : (selected ? 0xFFFFFFFF : 0xFFCBD5E1);
                    graphics.drawString(this.font, this.font.plainSubstrByWidth(text, this.sidebarW - 24),
                            this.sidebarX + 20, cursorY + 5, col, false);
                }
                cursorY += rowH;
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderContentPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.contentX, this.contentY, this.contentX + this.contentW, this.contentY + this.contentH, 0xCC0A1322);
        graphics.renderOutline(this.contentX, this.contentY, this.contentW, this.contentH, 0xFF1E3A5F);

        // Sub-header bar (Breadcrumb + Favorite + HUD Pin Status)
        graphics.fill(this.contentX + 1, this.contentY + 1, this.contentX + this.contentW - 1, this.contentY + 18, 0xFF102036);
        String breadcrumb = Component.translatable(this.activeTab.translationKey()).getString();
        if (this.selectedEntry != null) {
            breadcrumb += " > " + Component.translatable(this.selectedEntry.titleKey()).getString();
        }
        ManualReaderAttachment state = readerState();
        int rightReserve = state.pinnedMission().isPresent() ? 78 : 22;
        graphics.drawString(this.font, this.font.plainSubstrByWidth(breadcrumb, this.contentW - rightReserve - 10),
                this.contentX + 6, this.contentY + 5, 0xFFE2E8F0, false);

        if (this.selectedEntry != null) {
            boolean fav = state.isFavorite(this.selectedEntry.id());
            int favX = this.contentX + this.contentW - 18;
            drawActionButton(graphics, favX, this.contentY + 3, 14, 12, fav ? "★" : "☆", mouseX, mouseY);
        }

        if (state.pinnedMission().isPresent()) {
            int unpinX = this.contentX + this.contentW - 72;
            drawActionButton(graphics, unpinX, this.contentY + 3, 52, 12, "Unpin HUD", mouseX, mouseY);
            this.activeClickZones.add(new ClickZone(unpinX, this.contentY + 3, 52, 12, this::clearPin));
        }

        // Smooth scrollable scissored viewport
        int vx = this.contentX + 2;
        int vy = this.contentY + 20;
        int vw = this.contentW - 4;
        int vh = this.contentH - 22;

        this.scrollY = Mth.lerp(Math.min(1.0F, partialTick * 0.45F), this.scrollY, this.targetScrollY);

        graphics.enableScissor(vx, vy, vx + vw, vy + vh);
        try {
            int cursorY = vy + 4 - Math.round(this.scrollY);
            int blockW = vw - 10;
            for (MeasuredBlock block : this.measuredBlocks) {
                int blockBottom = cursorY + block.height();
                if (blockBottom >= vy && cursorY <= vy + vh) {
                    block.renderer().render(graphics, vx + 4, cursorY, blockW, mouseX, mouseY);
                }
                cursorY += block.height() + 8;
            }
        } finally {
            graphics.disableScissor();
        }

        // Scrollbar
        if (this.contentTotalHeight > vh) {
            int barH = Math.max(18, (int) ((float) vh * vh / this.contentTotalHeight));
            int maxScroll = Math.max(1, this.contentTotalHeight - vh);
            int barY = vy + (int) ((this.scrollY / maxScroll) * (vh - barH));
            graphics.fill(vx + vw - 3, vy, vx + vw - 1, vy + vh, 0x44223344);
            graphics.fill(vx + vw - 3, barY, vx + vw - 1, barY + barH, 0xCC38BDF8);
        }
    }

    // =========================================================================
    // INPUT HANDLING
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 3) {
            navigateBack();
            return true;
        }
        if (button == 4) {
            navigateForward();
            return true;
        }

        // Top bar navigation
        if (button == 0) {
            if (inRect(mouseX, mouseY, this.panelX + 5, this.panelY + 5, 14, 13)) {
                navigateBack();
                return true;
            }
            if (inRect(mouseX, mouseY, this.panelX + 21, this.panelY + 5, 14, 13)) {
                navigateForward();
                return true;
            }
            if (inRect(mouseX, mouseY, this.panelX + this.panelW - 18, this.panelY + 5, 13, 13)) {
                this.onClose();
                return true;
            }
            // Favorite toggle
            if (this.selectedEntry != null && inRect(mouseX, mouseY, this.contentX + this.contentW - 18, this.contentY + 3, 14, 12)) {
                toggleFavorite(this.selectedEntry.id());
                return true;
            }
            // 6 Tabs
            ManualTab[] tabs = ManualTab.values();
            int tabW = (this.panelW - 12) / tabs.length;
            int tabY = this.panelY + 25;
            for (int i = 0; i < tabs.length; i++) {
                int tx = this.panelX + 6 + i * tabW;
                if (inRect(mouseX, mouseY, tx, tabY, tabW - 2, 14)) {
                    selectTab(tabs[i]);
                    return true;
                }
            }
            // Sidebar chapter selection
            if (inRect(mouseX, mouseY, this.sidebarX, this.contentY, this.sidebarW, this.contentH)) {
                List<ManualEntry> entries = ManualManager.INSTANCE.entriesForTab(this.activeTab);
                int idx = (int) ((mouseY - (this.contentY + 4) + this.sidebarScroll) / 20);
                if (idx >= 0 && idx < entries.size()) {
                    navigateTo(entries.get(idx).id(), true);
                    return true;
                }
            }
        }

        // Delegate to interactive widgets inside viewport
        if (inRect(mouseX, mouseY, this.contentX, this.contentY + 20, this.contentW, this.contentH - 20)) {
            if (this.flightCalculator != null && this.flightCalculator.mouseClicked(mouseX, mouseY, button)) {
                rebuildLayout();
                return true;
            }
            for (Multiblock3DRendererWidget mb : this.multiblockWidgets) {
                if (mb.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        if (button == 0) {
            for (ClickZone zone : this.activeClickZones) {
                if (inRect(mouseX, mouseY, zone.x, zone.y, zone.w, zone.h)) {
                    zone.action.run();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (Multiblock3DRendererWidget mb : this.multiblockWidgets) {
            if (mb.mouseDragged(dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Multiblock3DRendererWidget mb : this.multiblockWidgets) {
            mb.mouseReleased();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollYDelta) {
        if (inRect(mouseX, mouseY, this.sidebarX, this.contentY, this.sidebarW, this.contentH)) {
            int maxSide = Math.max(0, ManualManager.INSTANCE.entriesForTab(this.activeTab).size() * 20 - this.contentH + 12);
            this.sidebarScroll = Mth.clamp(this.sidebarScroll - (int) (scrollYDelta * 16.0), 0, maxSide);
            return true;
        }
        if (inRect(mouseX, mouseY, this.contentX, this.contentY, this.contentW, this.contentH)) {
            int maxScroll = Math.max(0, this.contentTotalHeight - (this.contentH - 22));
            this.targetScrollY = Mth.clamp(this.targetScrollY - (float) (scrollYDelta * 20.0), 0.0F, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollYDelta);
    }

    private void drawNavButton(GuiGraphics g, int x, int y, int w, int h, String label, boolean enabled, int mx, int my) {
        boolean hov = enabled && inRect(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hov ? 0xFF1E3A5F : 0xFF102036);
        g.renderOutline(x, y, w, h, enabled ? (hov ? 0xFF38BDF8 : 0xFF2B4C73) : 0xFF1A293C);
        g.drawString(this.font, label, x + (w - this.font.width(label)) / 2, y + 3, enabled ? 0xFFE2E8F0 : 0xFF475569, false);
    }

    private void drawActionButton(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hov = inRect(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hov ? 0xFF1D426B : 0xFF132238);
        g.renderOutline(x, y, w, h, hov ? 0xFF38BDF8 : 0xFF2B4C73);
        String trimmed = this.font.plainSubstrByWidth(label, w - 4);
        g.drawString(this.font, trimmed, x + (w - this.font.width(trimmed)) / 2, y + 2, hov ? 0xFFFFFFFF : 0xFFCBD5E1, false);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @FunctionalInterface
    private interface BlockRenderer {
        void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY);
    }

    private record MeasuredBlock(int height, BlockRenderer renderer) {
    }

    private record ClickZone(int x, int y, int w, int h, Runnable action) {
    }
}
