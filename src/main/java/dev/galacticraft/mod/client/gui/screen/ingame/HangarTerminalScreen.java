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

import com.mojang.blaze3d.vertex.PoseStack;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartGuiPreviewRenderer;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartGuiPreviewRenderer.ProjectedBand;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartGuiPreviewRenderer.StackPart;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartPlacementPreviewRenderer;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.platform.PlatformBlockRole;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprint;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.assembly.RocketAssemblyGraphBuilder;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator;
import dev.galacticraft.mod.network.c2s.AttachRadialBoosterPayload;
import dev.galacticraft.mod.network.c2s.BuildRocketFromInventoryPayload;
import dev.galacticraft.mod.network.c2s.EditRocketCorePayload;
import dev.galacticraft.mod.network.c2s.HangarTerminalActionPayload;
import dev.galacticraft.mod.network.c2s.HangarTerminalActionPayload.Action;
import dev.galacticraft.mod.screen.HangarTerminalMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aerospace dark-slate configuration and KSP-style 3D VAB rocket construction screen
 * for the Hangar Terminal.
 *
 * <p>When unformed, provides platform configuration, hologram controls, and assembly
 * (with Creative-mode direct assembly bypass). When formed, transitions into an interactive
 * Kerbal-style 3D Vehicle Assembly viewport where players preview and position an unlimited
 * number of modular rocket parts directly from their inventory.
 */
@Environment(EnvType.CLIENT)
public class HangarTerminalScreen extends AbstractContainerScreen<HangarTerminalMenu> {
    private static final int PANEL_WIDTH_CONFIG = 284;
    private static final int PANEL_HEIGHT_CONFIG = 200;

    private static final int PANEL_WIDTH_VAB = 388;
    private static final int PANEL_HEIGHT_VAB = 258;

    // Config-mode column layout
    private static final int COL_LEFT_X = 8;
    private static final int COL_LEFT_W = 138;
    private static final int COL_RIGHT_X = 152;
    private static final int COL_RIGHT_W = 124;
    private static final int COL_TOP_Y = 25;
    private static final int COL_HEIGHT = 141;

    // Formed-mode top tab bar (388x258) — 3 equal aerospace tabs
    private static final int TAB_Y = 23;
    private static final int TAB_H = 16;
    private static final int TAB_ASSEMBLY_X = 8;
    private static final int TAB_ROCKET_X = 133;
    private static final int TAB_SIM_X = 258;
    private static final int TAB_W = 122;

    // Simulation-tab 3-column layout (388x258)
    private static final int SIM_DEST_X = 8;
    private static final int SIM_DEST_Y = 42;
    private static final int SIM_DEST_W = 104;
    private static final int SIM_DEST_H = 208;
    private static final int SIM_DEST_ROW_H = 18;

    private static final int SIM_METRIC_X = 116;
    private static final int SIM_METRIC_Y = 42;
    private static final int SIM_METRIC_W = 146;
    private static final int SIM_METRIC_H = 208;

    private static final int SIM_DIAG_X = 266;
    private static final int SIM_DIAG_Y = 42;
    private static final int SIM_DIAG_W = 114;
    private static final int SIM_DIAG_H = 208;

    // VAB-mode 3-column layout (388x258)
    private static final int VAB_INV_X = 8;
    private static final int VAB_INV_Y = 42;
    private static final int VAB_INV_W = 112;
    private static final int VAB_INV_H = 208;
    private static final int VAB_INV_ROW_H = 24;
    private static final int VAB_INV_VISIBLE_ROWS = 7;

    private static final int VAB_STAGE_X = 124;
    private static final int VAB_STAGE_Y = 42;
    private static final int VAB_STAGE_W = 152;
    private static final int VAB_STAGE_H = 208;
    private static final int VAB_CANVAS_X = VAB_STAGE_X + 4;
    private static final int VAB_CANVAS_Y = VAB_STAGE_Y + 16;
    private static final int VAB_CANVAS_W = VAB_STAGE_W - 8;
    private static final int VAB_CANVAS_H = 166;

    private static final int VAB_INFO_X = 280;
    private static final int VAB_INFO_Y = 42;
    private static final int VAB_INFO_W = 100;
    private static final int VAB_INFO_H = 208;

    // Rocket-tab 3-column layout (388x258)
    private static final int RKT_SYS_X = 8;
    private static final int RKT_SYS_Y = 42;
    private static final int RKT_SYS_W = 132;
    private static final int RKT_SYS_H = 208;

    private static final int RKT_VIEW_X = 144;
    private static final int RKT_VIEW_Y = 42;
    private static final int RKT_VIEW_W = 132;
    private static final int RKT_VIEW_H = 208;
    private static final int RKT_CANVAS_X = RKT_VIEW_X + 4;
    private static final int RKT_CANVAS_Y = RKT_VIEW_Y + 16;
    private static final int RKT_CANVAS_W = RKT_VIEW_W - 8;
    private static final int RKT_CANVAS_H = 166;

    private static final float VAB_DEFAULT_YAW = 225.0F;
    private static final float VAB_SPIN_DEG_PER_SEC = 18.0F;
    private static final float VAB_Z_OFFSET = 150.0F;
    private static final float VAB_MAX_HALF_DEPTH = 128.0F;
    private static final float VAB_HUD_Z = 295.0F;
    private static final float VAB_PIP_BG_Z = 305.0F;
    private static final float VAB_PIP_MODEL_Z = 342.0F;
    private static final float VAB_PIP_MAX_HALF_DEPTH = 26.0F;
    private static final float VAB_PIP_TEXT_Z = 375.0F;

    private static final int COLOR_BG_PANEL = 0xF0101620;
    private static final int COLOR_BG_HEADER = 0xFF182436;
    private static final int COLOR_BG_SECTION = 0xFF141D2B;
    private static final int COLOR_BG_INSET = 0xFF0C121C;
    private static final int COLOR_BORDER_OUTER = 0xFF3A4D66;
    private static final int COLOR_BORDER_INNER = 0xFF1B2636;
    private static final int COLOR_BORDER_SECTION = 0xFF28394F;
    private static final int COLOR_ACCENT_CYAN = 0xFF2D8AC4;

    private static final int COLOR_TEXT_TITLE = 0xFFE8F4FF;
    private static final int COLOR_TEXT_HEADER = 0xFF8FB8DE;
    private static final int COLOR_TEXT_PRIMARY = 0xFFE2E8F0;
    private static final int COLOR_TEXT_MUTED = 0xFF8A9CB5;
    private static final int COLOR_TEXT_OK = 0xFF4ADE80;
    private static final int COLOR_TEXT_WARN = 0xFFFB923C;
    private static final int COLOR_TEXT_ERROR = 0xFFF87171;
    private static final int COLOR_TEXT_AMBER = 0xFFFACC15;
    private static final int COLOR_TEXT_BADGE = 0xFF5EEAD4;

    private static final PlatformBlockRole[] ROLE_ORDER = {
            PlatformBlockRole.PAD,
            PlatformBlockRole.TRUSS,
            PlatformBlockRole.CRANE_RAIL,
            PlatformBlockRole.UPGRADE_SOCKET
    };

    private final Map<PlatformBlockRole, ItemStack> roleIcons = new EnumMap<>(PlatformBlockRole.class);

    // Config-mode widgets
    private TerminalButton typeButton;
    private TerminalButton sizePrevButton;
    private TerminalButton sizeNextButton;
    private TerminalButton detectPadButton;
    private TerminalButton previewToggleButton;
    private TerminalButton layerPrevButton;
    private TerminalButton layerNextButton;
    private TerminalButton actionButton;

    // Formed-mode tab buttons
    private TerminalButton assemblyTabButton;
    private TerminalButton rocketTabButton;
    private TerminalButton simTabButton;
    private TerminalButton goToAssemblyButton;

    // VAB-mode & Rocket-tab widgets
    private TerminalButton moveUpButton;
    private TerminalButton moveDownButton;
    private TerminalButton removePartButton;
    private TerminalButton clearVabButton;
    private TerminalButton vabAnimButton;
    private TerminalButton vabResetViewButton;
    private TerminalButton vabModeButton;
    private TerminalButton buildRocketButton;
    private TerminalButton symmetryButton;
    private TerminalButton crossfeedButton;
    private TerminalButton attachBoostersButton;
    private TerminalButton cancelAssemblyButton;
    private TerminalButton recallRocketButton;
    private TerminalButton triggerAnimButton;
    private TerminalButton toggleHatchButton;
    private TerminalButton creativeFillButton;
    private TerminalButton launchRocketButton;
    private TerminalButton axisGuideButton;

    // Pre-flight emergent physics destination simulator index (defaults to Moon = 1)
    private int simDestinationIndex = 1;

    // Formed-mode active tab & VAB assembly mode (Core coaxial vs Radial Boosters)
    private FormedTab activeTab = FormedTab.ASSEMBLY;
    private boolean vabBoosterMode = false;
    private boolean vabEditingMounted = false;
    private boolean vabAwaitingEditSync = false;
    private boolean initializedTabSelection = false;

    // VAB interactive state (bottom-to-top list of inventory slot indices; NO fixed cap!)
    private final List<VabEntry> vabDraft = new ArrayList<>();
    private final List<InventoryPartEntry> inventoryParts = new ArrayList<>();
    private final List<ProjectedBand> lastCanvasBands = new ArrayList<>();
    private final List<ProjectedBand> lastBaseBands = new ArrayList<>();
    private int selectedInventorySlot = -1;
    private int selectedVabIndex = -1;
    private int inventoryScrollOffset = 0;

    // Interactive 3D VAB viewport camera & animation state
    private float vabYaw = VAB_DEFAULT_YAW;
    private float vabPitch = RocketPartGuiPreviewRenderer.DEFAULT_PITCH_DEG;
    private float vabZoom = 1.0F;
    private boolean vabAutoSpin = true;
    private boolean vabAnimEnabled = false;
    private float vabAnimTime = 0.0F;
    private long lastVabFrameNanos = 0L;
    private boolean vabPointerDown = false;
    private boolean vabDragged = false;
    private int vabPressButton = -1;
    private double vabPressX = 0.0;
    private double vabPressY = 0.0;

    private PlatformBlueprint.ValidationSummary cachedSummary;
    private HangarTerminalBlockEntity.RocketPadSummary cachedMountedSummary = HangarTerminalBlockEntity.RocketPadSummary.EMPTY;
    private boolean lastFormedLayout = false;
    private int selectedMountedIndex = 0;
    private long lastObservedConfigVersion = Long.MIN_VALUE;
    private int validationTickCounter = 0;

    public HangarTerminalScreen(HangarTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        boolean formed = menu.blockEntity != null && menu.blockEntity.isFormed();
        this.imageWidth = formed ? PANEL_WIDTH_VAB : PANEL_WIDTH_CONFIG;
        this.imageHeight = formed ? PANEL_HEIGHT_VAB : PANEL_HEIGHT_CONFIG;
        this.lastFormedLayout = formed;

        for (PlatformBlockRole role : ROLE_ORDER) {
            Block block = role.getBlock();
            this.roleIcons.put(role, block != null ? new ItemStack(block) : ItemStack.EMPTY);
        }
    }

    @Override
    protected void init() {
        boolean formed = this.menu.blockEntity != null && this.menu.blockEntity.isFormed();
        this.imageWidth = formed ? PANEL_WIDTH_VAB : PANEL_WIDTH_CONFIG;
        this.imageHeight = formed ? PANEL_HEIGHT_VAB : PANEL_HEIGHT_CONFIG;
        this.lastFormedLayout = formed;
        super.init();

        int x = this.leftPos;
        int y = this.topPos;
        int lx = x + COL_LEFT_X + 6;
        int ly = y + COL_TOP_Y;

        // Config-mode widgets
        this.typeButton = this.addRenderableWidget(new TerminalButton(
                lx, ly + 17, 126, 18,
                Component.empty(),
                ButtonStyle.STANDARD,
                btn -> this.onToggleType()
        ));

        this.sizePrevButton = this.addRenderableWidget(new TerminalButton(
                lx, ly + 39, 18, 18,
                Component.literal("<"),
                ButtonStyle.STANDARD,
                btn -> this.onStepSize(-1)
        ));

        this.sizeNextButton = this.addRenderableWidget(new TerminalButton(
                lx + 108, ly + 39, 18, 18,
                Component.literal(">"),
                ButtonStyle.STANDARD,
                btn -> this.onStepSize(1)
        ));

        this.detectPadButton = this.addRenderableWidget(new TerminalButton(
                lx, ly + 74, 126, 18,
                Component.literal("Detectar Pad"),
                ButtonStyle.STANDARD,
                btn -> this.sendAction(Action.DETECT_PAD, 0)
        ));

        this.previewToggleButton = this.addRenderableWidget(new TerminalButton(
                lx, ly + 98, 126, 18,
                Component.empty(),
                ButtonStyle.STANDARD,
                btn -> this.onTogglePreview()
        ));

        this.layerPrevButton = this.addRenderableWidget(new TerminalButton(
                lx, ly + 119, 18, 18,
                Component.literal("<"),
                ButtonStyle.STANDARD,
                btn -> this.onStepLayer(-1)
        ));

        this.layerNextButton = this.addRenderableWidget(new TerminalButton(
                lx + 108, ly + 119, 18, 18,
                Component.literal(">"),
                ButtonStyle.STANDARD,
                btn -> this.onStepLayer(1)
        ));

        this.actionButton = this.addRenderableWidget(new TerminalButton(
                x + 8, y + 172, PANEL_WIDTH_CONFIG - 16, 20,
                Component.empty(),
                ButtonStyle.PRIMARY_ASSEMBLE,
                btn -> this.onPrimaryAction()
        ));

        // Formed-mode tab bar buttons
        this.assemblyTabButton = this.addRenderableWidget(new TerminalButton(
                x + TAB_ASSEMBLY_X, y + TAB_Y, TAB_W, TAB_H,
                Component.literal("Montagem (VAB)"),
                ButtonStyle.TAB_INACTIVE,
                btn -> {
                    this.activeTab = FormedTab.ASSEMBLY;
                    this.refreshState(false);
                }
        ));
        this.rocketTabButton = this.addRenderableWidget(new TerminalButton(
                x + TAB_ROCKET_X, y + TAB_Y, TAB_W, TAB_H,
                Component.literal("Foguete"),
                ButtonStyle.TAB_INACTIVE,
                btn -> {
                    this.activeTab = FormedTab.ROCKET;
                    this.refreshState(true);
                }
        ));
        this.simTabButton = this.addRenderableWidget(new TerminalButton(
                x + TAB_SIM_X, y + TAB_Y, TAB_W, TAB_H,
                Component.literal("Simulação"),
                ButtonStyle.TAB_INACTIVE,
                btn -> {
                    this.activeTab = FormedTab.SIMULATION;
                    this.refreshState(true);
                }
        ));
        this.goToAssemblyButton = this.addRenderableWidget(new TerminalButton(
                x + 124, y + 148, 140, 20,
                Component.literal("Ir para Montagem (VAB)"),
                ButtonStyle.PRIMARY_ASSEMBLE,
                btn -> {
                    this.activeTab = FormedTab.ASSEMBLY;
                    this.refreshState(false);
                }
        ));

        // VAB-mode widgets (center stage bottom toolbar + right telemetry controls)
        int toolY = y + VAB_CANVAS_Y + VAB_CANVAS_H + 4;
        this.moveUpButton = this.addRenderableWidget(new TerminalButton(
                x + VAB_CANVAS_X, toolY, 19, 16,
                Component.literal("^"),
                ButtonStyle.STANDARD,
                btn -> this.moveSelectedVabPart(1)
        ));
        this.moveDownButton = this.addRenderableWidget(new TerminalButton(
                x + VAB_CANVAS_X + 21, toolY, 19, 16,
                Component.literal("v"),
                ButtonStyle.STANDARD,
                btn -> this.moveSelectedVabPart(-1)
        ));
        this.removePartButton = this.addRenderableWidget(new TerminalButton(
                x + VAB_CANVAS_X + 42, toolY, 20, 16,
                Component.literal("X"),
                ButtonStyle.DANGER_DISASSEMBLE,
                btn -> this.removeSelectedVabPart()
        ));
        this.clearVabButton = this.addRenderableWidget(new TerminalButton(
                x + VAB_CANVAS_X + 64, toolY, 44, 16,
                Component.literal("Limpar"),
                ButtonStyle.STANDARD,
                btn -> {
                    HangarTerminalBlockEntity curBe = this.menu.blockEntity;
                    if (this.vabEditingMounted && curBe != null && !this.vabBoosterMode) {
                        this.loadMountedCoreIntoVab(curBe);
                    } else {
                        this.vabDraft.clear();
                        this.selectedVabIndex = -1;
                        this.lastBaseBands.clear();
                    }
                    this.refreshState(false);
                }
        ));
        this.vabAnimButton = this.addRenderableWidget(new TerminalButton(
                x + VAB_CANVAS_X + 110, toolY, 16, 16,
                Component.literal(">"),
                ButtonStyle.STANDARD,
                btn -> {
                    this.vabAnimEnabled = !this.vabAnimEnabled;
                    if (this.vabAnimEnabled) {
                        this.vabAnimTime = 0.0F;
                    }
                    this.refreshState(false);
                }
        ));
        this.vabResetViewButton = this.addRenderableWidget(new TerminalButton(
                x + VAB_CANVAS_X + 128, toolY, 16, 16,
                Component.literal("R"),
                ButtonStyle.STANDARD,
                btn -> {
                    this.vabYaw = VAB_DEFAULT_YAW;
                    this.vabPitch = RocketPartGuiPreviewRenderer.DEFAULT_PITCH_DEG;
                    this.vabZoom = 1.0F;
                    this.vabAutoSpin = true;
                }
        ));

        int rx = x + VAB_INFO_X + 4;
        int ry = y + VAB_INFO_Y;
        int rw = VAB_INFO_W - 8;

        this.vabModeButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 112, rw, 15,
                Component.empty(),
                ButtonStyle.STANDARD,
                btn -> {
                    this.vabBoosterMode = !this.vabBoosterMode;
                    this.vabDraft.clear();
                    this.vabEditingMounted = false;
                    this.selectedVabIndex = -1;
                    this.lastBaseBands.clear();
                    this.refreshState(false);
                }
        ));

        this.buildRocketButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 129, rw, 16,
                Component.literal("Construir Pad"),
                ButtonStyle.PRIMARY_ASSEMBLE,
                btn -> this.onBuildVabRocket()
        ));

        this.attachBoostersButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 129, rw, 16,
                Component.literal("+ Boosters"),
                ButtonStyle.PRIMARY_ASSEMBLE,
                btn -> this.onAttachRadialBoosters()
        ));

        this.cancelAssemblyButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 129, rw, 16,
                Component.literal("Cancelar Solda"),
                ButtonStyle.DANGER_DISASSEMBLE,
                btn -> this.sendAction(Action.CANCEL_ASSEMBLY, 0)
        ));

        this.symmetryButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 147, 45, 15,
                Component.empty(),
                ButtonStyle.STANDARD,
                btn -> this.onCycleSymmetry()
        ));

        this.crossfeedButton = this.addRenderableWidget(new TerminalButton(
                rx + 47, ry + 147, 45, 15,
                Component.empty(),
                ButtonStyle.STANDARD,
                btn -> this.sendAction(Action.TOGGLE_CROSSFEED, 0)
        ));

        this.launchRocketButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 94, rw, 16,
                Component.literal("Lançar (T-10s)"),
                ButtonStyle.PRIMARY_ASSEMBLE,
                btn -> {
                    HangarTerminalBlockEntity be = this.menu.blockEntity;
                    if (be != null && be.isLaunchSequenceActive()) {
                        this.sendAction(Action.ABORT_LAUNCH, 0);
                    } else {
                        this.sendAction(Action.START_LAUNCH, 0);
                        this.onClose();
                    }
                }
        ));

        this.triggerAnimButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 112, rw, 15,
                Component.empty(),
                ButtonStyle.STANDARD,
                btn -> this.onTriggerSelectedPartAnim()
        ));

        this.toggleHatchButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 129, rw, 15,
                Component.empty(),
                ButtonStyle.STANDARD,
                btn -> this.onToggleSelectedPartHatch()
        ));

        this.creativeFillButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 146, rw, 15,
                Component.literal("Abastecer (Cr.)"),
                ButtonStyle.PRIMARY_ASSEMBLE,
                btn -> {
                    this.sendAction(Action.CREATIVE_FILL_ROCKET, 0);
                    this.validationTickCounter = 4;
                }
        ));

        this.recallRocketButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 164, rw, 16,
                Component.literal("Recolher Foguete"),
                ButtonStyle.STANDARD,
                btn -> {
                    this.sendAction(Action.RECALL_ROCKET, 0);
                    this.validationTickCounter = 4;
                }
        ));

        this.axisGuideButton = this.addRenderableWidget(new TerminalButton(
                rx, ry + 164, rw, 15,
                Component.empty(),
                ButtonStyle.STANDARD,
                btn -> this.sendAction(Action.TOGGLE_AXIS_GUIDE, 0)
        ));

        if (formed && !this.initializedTabSelection) {
            this.activeTab = this.menu.blockEntity.isRocketAssembled() ? FormedTab.ROCKET : FormedTab.ASSEMBLY;
            this.initializedTabSelection = true;
        }

        this.refreshState(true);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.validationTickCounter++;
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be != null && be.isFormed() != this.lastFormedLayout) {
            this.rebuildWidgets();
            return;
        }
        boolean configChanged = be != null && be.getClientConfigVersion() != this.lastObservedConfigVersion;
        if (configChanged || this.validationTickCounter >= 5) {
            this.validationTickCounter = 0;
            this.refreshState(true);
        } else {
            this.refreshState(false);
        }
    }

    private void refreshInventoryParts() {
        this.inventoryParts.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean creative = mc.player.isCreative();
        Inventory inv = mc.player.getInventory();

        // Count how many times each inventory slot is currently used in the VAB draft (skip already-mounted core parts)
        Map<Integer, Integer> usedCounts = new HashMap<>();
        for (int i = this.vabDraft.size() - 1; i >= 0; i--) {
            VabEntry entry = this.vabDraft.get(i);
            if (entry.isMounted()) {
                continue;
            }
            ItemStack st = entry.inventorySlot >= 0 && entry.inventorySlot < inv.getContainerSize()
                    ? inv.getItem(entry.inventorySlot)
                    : ItemStack.EMPTY;
            if (st.isEmpty() || !(st.getItem() instanceof ModularRocketPartBlockItem)) {
                this.vabDraft.remove(i);
                continue;
            }
            int nextUsed = usedCounts.getOrDefault(entry.inventorySlot, 0) + 1;
            if (!creative && nextUsed > st.getCount()) {
                this.vabDraft.remove(i);
                continue;
            }
            usedCounts.put(entry.inventorySlot, nextUsed);
        }

        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ModularRocketPartBlockItem partItem)) {
                continue;
            }
            RocketPartConfig cfg = partItem.getConfig(stack);
            RocketPartSpec spec = catalog.getPart(cfg.modelId());
            String name = spec != null ? spec.displayName() : cfg.modelId();
            int used = usedCounts.getOrDefault(slot, 0);
            int remaining = creative ? 99 : Math.max(0, stack.getCount() - used);
            this.inventoryParts.add(new InventoryPartEntry(slot, stack.copyWithCount(1), cfg, name, remaining, creative));
        }

        int maxScroll = Math.max(0, this.inventoryParts.size() - VAB_INV_VISIBLE_ROWS);
        this.inventoryScrollOffset = Mth.clamp(this.inventoryScrollOffset, 0, maxScroll);

        if (this.selectedInventorySlot >= 0) {
            boolean stillAvailable = false;
            for (InventoryPartEntry e : this.inventoryParts) {
                if (e.slotIndex == this.selectedInventorySlot && (e.creative || e.remaining > 0)) {
                    stillAvailable = true;
                    break;
                }
            }
            if (!stillAvailable) {
                this.selectedInventorySlot = -1;
            }
        }
        if (this.selectedVabIndex >= this.vabDraft.size()) {
            this.selectedVabIndex = this.vabDraft.size() - 1;
        }
    }

    private void refreshState(boolean revalidate) {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null) {
            return;
        }

        if (revalidate || this.cachedSummary == null) {
            this.cachedSummary = be.validateStructure();
            if (be.isFormed()) {
                this.cachedMountedSummary = be.scanRocketOnPad();
            } else {
                this.cachedMountedSummary = HangarTerminalBlockEntity.RocketPadSummary.EMPTY;
            }
            this.lastObservedConfigVersion = be.getClientConfigVersion();
        }

        boolean formed = be.isFormed();
        if (formed) {
            this.refreshInventoryParts();
        }

        PlatformType type = be.getPlatformType() != null ? be.getPlatformType() : PlatformType.VERTICAL;
        boolean previewEnabled = be.isPreviewEnabled();

        // Config-mode widgets
        if (this.typeButton != null) {
            this.typeButton.visible = !formed;
            this.typeButton.setMessage(Component.literal(
                    type == PlatformType.HORIZONTAL ? "Tipo: Horizontal" : "Tipo: Vertical"
            ));
            this.typeButton.active = !formed;
        }

        boolean canStepSize = !formed && supportedSizes(type).size() > 1;
        if (this.sizePrevButton != null) {
            this.sizePrevButton.visible = !formed;
            this.sizePrevButton.active = canStepSize;
        }
        if (this.sizeNextButton != null) {
            this.sizeNextButton.visible = !formed;
            this.sizeNextButton.active = canStepSize;
        }
        if (this.detectPadButton != null) {
            this.detectPadButton.visible = !formed;
            this.detectPadButton.active = !formed;
        }
        if (this.previewToggleButton != null) {
            this.previewToggleButton.visible = !formed;
            this.previewToggleButton.setMessage(Component.literal(
                    previewEnabled ? "Preview: ON" : "Preview: OFF"
            ));
            this.previewToggleButton.active = !formed;
        }
        if (this.layerPrevButton != null) {
            this.layerPrevButton.visible = !formed;
            this.layerPrevButton.active = !formed;
        }
        if (this.layerNextButton != null) {
            this.layerNextButton.visible = !formed;
            this.layerNextButton.active = !formed;
        }
        if (this.actionButton != null) {
            this.actionButton.visible = !formed;
            boolean ready = this.cachedSummary != null && this.cachedSummary.readyToAssemble();
            boolean creativeBypass = !ready && this.isCreativePlayer();
            this.actionButton.setMessage(Component.literal(
                    creativeBypass ? "Montar (Criativo)" : "Montar Plataforma"
            ));
            this.actionButton.setStyle(ButtonStyle.PRIMARY_ASSEMBLE);
            this.actionButton.active = !formed && (ready || creativeBypass);
        }

        // Formed-mode tab buttons
        List<HangarTerminalBlockEntity.MountedRocketPart> mounted = this.cachedMountedSummary.parts();
        boolean hasMounted = !mounted.isEmpty();
        boolean rocketTab = formed && this.activeTab == FormedTab.ROCKET;
        boolean assemblyTab = formed && this.activeTab == FormedTab.ASSEMBLY;
        boolean simTab = formed && this.activeTab == FormedTab.SIMULATION;

        // Auto-load mounted core stack into the live VAB editor when in Core mode
        if (assemblyTab && !this.vabBoosterMode && !be.isAssemblyInProgress()) {
            if (hasMounted) {
                if ((!this.vabEditingMounted && this.vabDraft.isEmpty()) || (this.vabAwaitingEditSync && revalidate)) {
                    this.loadMountedCoreIntoVab(be);
                    this.vabAwaitingEditSync = false;
                }
            } else if (this.vabEditingMounted) {
                this.vabEditingMounted = false;
                this.vabAwaitingEditSync = false;
                this.vabDraft.clear();
                this.selectedVabIndex = -1;
            }
        } else if (!hasMounted && this.vabEditingMounted) {
            this.vabEditingMounted = false;
            this.vabAwaitingEditSync = false;
        }

        boolean vabEditDirty = this.vabEditingMounted && this.isVabEditDirty(be);
        boolean hasMountedBoosters = hasMounted && this.hasMountedRadialBoosters(be);

        if (this.assemblyTabButton != null) {
            this.assemblyTabButton.visible = formed;
            this.assemblyTabButton.active = formed;
            this.assemblyTabButton.setStyle(assemblyTab ? ButtonStyle.TAB_ACTIVE : ButtonStyle.TAB_INACTIVE);
            this.assemblyTabButton.setMessage(Component.literal(
                    this.vabDraft.isEmpty() ? "Montagem (VAB)" : "VAB (" + this.vabDraft.size() + ")"
            ));
        }
        if (this.rocketTabButton != null) {
            this.rocketTabButton.visible = formed;
            this.rocketTabButton.active = formed;
            this.rocketTabButton.setStyle(rocketTab ? ButtonStyle.TAB_ACTIVE : ButtonStyle.TAB_INACTIVE);
            String rocketTabLabel = hasMounted
                    ? (be.isRocketAssembled() ? "Foguete [" + mounted.size() + "]" : "No Pad (" + mounted.size() + ")")
                    : "Foguete";
            this.rocketTabButton.setMessage(Component.literal(rocketTabLabel));
        }
        if (this.simTabButton != null) {
            this.simTabButton.visible = formed;
            this.simTabButton.active = formed;
            this.simTabButton.setStyle(simTab ? ButtonStyle.TAB_ACTIVE : ButtonStyle.TAB_INACTIVE);
            this.simTabButton.setMessage(Component.literal("Simulação"));
        }
        if (this.goToAssemblyButton != null) {
            boolean showGoToVab = (rocketTab && !hasMounted) || (simTab && !hasMounted && this.vabDraft.isEmpty());
            this.goToAssemblyButton.visible = showGoToVab;
            this.goToAssemblyButton.active = showGoToVab;
        }

        // VAB-mode & Rocket-tab widgets
        boolean hasDraft = !this.vabDraft.isEmpty();
        if (mounted.isEmpty()) {
            this.selectedMountedIndex = 0;
        } else {
            this.selectedMountedIndex = Mth.clamp(this.selectedMountedIndex, 0, mounted.size() - 1);
        }
        HangarTerminalBlockEntity.MountedRocketPart selectedMounted = mounted.isEmpty() ? null : mounted.get(this.selectedMountedIndex);

        if (this.moveUpButton != null) {
            this.moveUpButton.visible = assemblyTab;
            this.moveUpButton.active = assemblyTab && hasDraft && this.selectedVabIndex >= 0 && this.selectedVabIndex < this.vabDraft.size() - 1;
        }
        if (this.moveDownButton != null) {
            this.moveDownButton.visible = assemblyTab;
            this.moveDownButton.active = assemblyTab && hasDraft && this.selectedVabIndex > 0;
        }
        if (this.removePartButton != null) {
            this.removePartButton.visible = assemblyTab;
            this.removePartButton.active = assemblyTab && hasDraft && this.selectedVabIndex >= 0 && this.selectedVabIndex < this.vabDraft.size();
        }
        if (this.clearVabButton != null) {
            this.clearVabButton.visible = assemblyTab;
            this.clearVabButton.active = assemblyTab && (this.vabEditingMounted ? vabEditDirty : hasDraft);
            this.clearVabButton.setMessage(Component.literal(this.vabEditingMounted ? "Reverter" : "Limpar"));
        }
        int toolY = this.topPos + VAB_CANVAS_Y + VAB_CANVAS_H + 4;
        if (this.vabAnimButton != null) {
            boolean showCamBtn = assemblyTab || (rocketTab && hasMounted);
            this.vabAnimButton.visible = showCamBtn;
            this.vabAnimButton.active = showCamBtn;
            this.vabAnimButton.setPosition(
                    assemblyTab ? this.leftPos + VAB_CANVAS_X + 110 : this.leftPos + RKT_CANVAS_X + RKT_CANVAS_W - 34,
                    toolY
            );
            this.vabAnimButton.setMessage(Component.literal(this.vabAnimEnabled ? "||" : ">"));
        }
        if (this.vabResetViewButton != null) {
            boolean showCamBtn = assemblyTab || (rocketTab && hasMounted);
            this.vabResetViewButton.visible = showCamBtn;
            this.vabResetViewButton.active = showCamBtn;
            this.vabResetViewButton.setPosition(
                    assemblyTab ? this.leftPos + VAB_CANVAS_X + 128 : this.leftPos + RKT_CANVAS_X + RKT_CANVAS_W - 16,
                    toolY
            );
        }
        boolean assembling = be.isAssemblyInProgress();
        RadialSymmetry activeBoosterSym = resolveBoosterSymmetry(be.getSelectedSymmetry());
        if (this.vabModeButton != null) {
            this.vabModeButton.visible = assemblyTab;
            this.vabModeButton.active = assemblyTab;
            this.vabModeButton.setStyle(this.vabBoosterMode ? ButtonStyle.TAB_ACTIVE : ButtonStyle.STANDARD);
            this.vabModeButton.setMessage(Component.literal(this.vabBoosterMode ? "Modo: Boosters" : "Modo: Núcleo"));
        }
        if (this.buildRocketButton != null) {
            this.buildRocketButton.visible = assemblyTab && !assembling && !this.vabBoosterMode;
            if (this.vabEditingMounted) {
                this.buildRocketButton.active = assemblyTab && !assembling && !this.vabBoosterMode && vabEditDirty;
                if (!vabEditDirty) {
                    this.buildRocketButton.setStyle(ButtonStyle.STANDARD);
                    this.buildRocketButton.setMessage(Component.literal("Editando Pad"));
                } else if (this.vabDraft.isEmpty()) {
                    this.buildRocketButton.setStyle(ButtonStyle.DANGER_DISASSEMBLE);
                    this.buildRocketButton.setMessage(Component.literal("Recolher Tudo"));
                } else {
                    this.buildRocketButton.setStyle(ButtonStyle.PRIMARY_ASSEMBLE);
                    this.buildRocketButton.setMessage(Component.literal("Aplicar Edição"));
                }
            } else {
                this.buildRocketButton.active = assemblyTab && !assembling && !this.vabBoosterMode && hasDraft;
                this.buildRocketButton.setStyle(ButtonStyle.PRIMARY_ASSEMBLE);
                this.buildRocketButton.setMessage(Component.literal("Construir Pad"));
            }
        }
        if (this.attachBoostersButton != null) {
            this.attachBoostersButton.visible = assemblyTab && !assembling && this.vabBoosterMode;
            if (!hasDraft && hasMountedBoosters) {
                this.attachBoostersButton.active = assemblyTab && !assembling && this.vabBoosterMode;
                this.attachBoostersButton.setStyle(ButtonStyle.DANGER_DISASSEMBLE);
                this.attachBoostersButton.setMessage(Component.literal("Rem. Boosters"));
            } else {
                boolean canAttach = assemblyTab && !assembling && this.vabBoosterMode && hasDraft && hasMounted;
                this.attachBoostersButton.active = canAttach;
                this.attachBoostersButton.setStyle(ButtonStyle.PRIMARY_ASSEMBLE);
                this.attachBoostersButton.setMessage(Component.literal("+ Boosters (" + activeBoosterSym.count() + "x)"));
            }
        }
        if (this.cancelAssemblyButton != null) {
            this.cancelAssemblyButton.visible = assemblyTab && assembling;
            this.cancelAssemblyButton.active = assemblyTab && assembling;
        }
        if (this.symmetryButton != null) {
            this.symmetryButton.visible = assemblyTab;
            this.symmetryButton.active = assemblyTab;
            String symLabel = activeBoosterSym == RadialSymmetry.FOUR_DIAGONAL ? "4xD" : activeBoosterSym.getSerializedName();
            this.symmetryButton.setMessage(Component.literal("Sim:" + symLabel));
        }
        if (this.crossfeedButton != null) {
            this.crossfeedButton.visible = assemblyTab;
            this.crossfeedButton.active = assemblyTab;
            boolean asparagus = be.getSelectedCrossfeed() == CrossfeedMode.CONTINUOUS_CROSSFEED;
            this.crossfeedButton.setMessage(Component.literal(asparagus ? "Asp:ON" : "Asp:OFF"));
        }
        if (this.launchRocketButton != null) {
            boolean showLaunch = rocketTab && hasMounted && be.isRocketAssembled();
            this.launchRocketButton.visible = showLaunch;
            boolean launching = be.isLaunchSequenceActive();
            this.launchRocketButton.active = showLaunch && !assembling;
            if (launching) {
                this.launchRocketButton.setStyle(ButtonStyle.DANGER_DISASSEMBLE);
                this.launchRocketButton.setMessage(Component.literal(
                        String.format(java.util.Locale.ROOT, "ABORTAR (T-%.1fs)", be.getLaunchCountdownSeconds())
                ));
            } else {
                this.launchRocketButton.setStyle(ButtonStyle.PRIMARY_ASSEMBLE);
                this.launchRocketButton.setMessage(Component.literal("Lançar (T-10s)"));
            }
        }
        if (this.triggerAnimButton != null) {
            this.triggerAnimButton.visible = rocketTab && hasMounted;
            boolean canTrigger = rocketTab && selectedMounted != null && selectedMounted.supportsAnimation();
            this.triggerAnimButton.active = canTrigger;
            if (canTrigger) {
                this.triggerAnimButton.setMessage(Component.literal(
                        selectedMounted.animationActive() ? "Recolher Mec." : "Acionar Mec."
                ));
            } else {
                this.triggerAnimButton.setMessage(Component.literal("Sem Trigger"));
            }
        }
        if (this.toggleHatchButton != null) {
            this.toggleHatchButton.visible = rocketTab && hasMounted;
            boolean canHatch = rocketTab && selectedMounted != null && selectedMounted.walkableInterior();
            this.toggleHatchButton.active = canHatch;
            if (canHatch) {
                this.toggleHatchButton.setMessage(Component.literal(
                        selectedMounted.hatchOpen() ? "Fechar Escot." : "Abrir Escot."
                ));
            } else {
                this.toggleHatchButton.setMessage(Component.literal("Sem Escotilha"));
            }
        }
        if (this.creativeFillButton != null) {
            boolean showCreativeFill = rocketTab && hasMounted && be.isRocketAssembled() && this.isCreativePlayer();
            this.creativeFillButton.visible = showCreativeFill;
            long fuelCap = this.cachedMountedSummary.totalFuelDroplets();
            long energyCap = this.cachedMountedSummary.energyCapacity();
            long oxygenCap = this.cachedMountedSummary.oxygenCapacityDroplets();
            boolean hasAnyCap = fuelCap > 0L || energyCap > 0L || oxygenCap > 0L;
            this.creativeFillButton.active = showCreativeFill && hasAnyCap;
            boolean full = hasAnyCap
                    && (fuelCap <= 0L || this.cachedMountedSummary.currentFuelDroplets() >= fuelCap)
                    && (energyCap <= 0L || this.cachedMountedSummary.currentEnergy() >= energyCap)
                    && (oxygenCap <= 0L || this.cachedMountedSummary.currentOxygenDroplets() >= oxygenCap);
            this.creativeFillButton.setMessage(Component.literal(full ? "Esvaziar (Cr.)" : "Abastecer (Cr.)"));
        }
        if (this.recallRocketButton != null) {
            boolean showRecall = rocketTab && hasMounted;
            this.recallRocketButton.visible = showRecall;
            this.recallRocketButton.active = showRecall;
            this.recallRocketButton.setMessage(Component.literal(
                    be.isRocketAssembled() ? "Recolher Foguete" : "Recolher Pad"
            ));
        }
        if (this.axisGuideButton != null) {
            this.axisGuideButton.visible = assemblyTab;
            this.axisGuideButton.active = assemblyTab;
            this.axisGuideButton.setMessage(Component.literal(
                    be.isAxisGuideEnabled() ? "Guia: ON" : "Guia: OFF"
            ));
        }
    }

    private void moveSelectedVabPart(int delta) {
        int target = this.selectedVabIndex + delta;
        if (this.selectedVabIndex < 0 || this.selectedVabIndex >= this.vabDraft.size()
                || target < 0 || target >= this.vabDraft.size()) {
            return;
        }
        VabEntry moving = this.vabDraft.remove(this.selectedVabIndex);
        this.vabDraft.add(target, moving);
        this.selectedVabIndex = target;
        this.refreshState(false);
    }

    private void removeSelectedVabPart() {
        if (this.selectedVabIndex >= 0 && this.selectedVabIndex < this.vabDraft.size()) {
            this.vabDraft.remove(this.selectedVabIndex);
            this.selectedVabIndex = Math.min(this.selectedVabIndex, this.vabDraft.size() - 1);
            this.refreshState(false);
        }
    }

    private List<HangarTerminalBlockEntity.MountedRocketPart> collectMountedCoreParts(HangarTerminalBlockEntity be) {
        net.minecraft.core.BlockPos padCenter = be.getPadCenterPos();
        List<HangarTerminalBlockEntity.MountedRocketPart> core = new ArrayList<>();
        for (HangarTerminalBlockEntity.MountedRocketPart part : this.cachedMountedSummary.parts()) {
            net.minecraft.core.BlockPos rel = part.pos().subtract(padCenter);
            boolean radial = part.config() != null && part.config().radial();
            if (rel.getX() == 0 && rel.getZ() == 0 && !radial) {
                core.add(part);
            }
        }
        core.sort(java.util.Comparator.comparingInt(HangarTerminalBlockEntity.MountedRocketPart::relativeY));
        return core;
    }

    private boolean hasMountedRadialBoosters(HangarTerminalBlockEntity be) {
        net.minecraft.core.BlockPos padCenter = be.getPadCenterPos();
        for (HangarTerminalBlockEntity.MountedRocketPart part : this.cachedMountedSummary.parts()) {
            net.minecraft.core.BlockPos rel = part.pos().subtract(padCenter);
            boolean radial = part.config() != null && part.config().radial();
            if (rel.getX() != 0 || rel.getZ() != 0 || radial) {
                return true;
            }
        }
        return false;
    }

    private void loadMountedCoreIntoVab(HangarTerminalBlockEntity be) {
        List<HangarTerminalBlockEntity.MountedRocketPart> coreParts = this.collectMountedCoreParts(be);
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        this.vabDraft.clear();
        for (HangarTerminalBlockEntity.MountedRocketPart part : coreParts) {
            RocketPartConfig cfg = part.config() != null ? part.config() : catalog.defaultConfigFor(part.category());
            ItemStack icon = ModularRocketPartBlockItem.createConfiguredStack(cfg);
            this.vabDraft.add(new VabEntry(-1, part.pos().immutable(), icon, cfg, part.displayName()));
        }
        this.vabEditingMounted = !this.vabDraft.isEmpty();
        this.selectedVabIndex = this.vabDraft.isEmpty() ? -1 : Mth.clamp(this.selectedVabIndex, 0, this.vabDraft.size() - 1);
        this.lastBaseBands.clear();
    }

    private boolean isVabEditDirty(HangarTerminalBlockEntity be) {
        if (!this.vabEditingMounted) {
            return !this.vabDraft.isEmpty();
        }
        List<HangarTerminalBlockEntity.MountedRocketPart> mountedCore = this.collectMountedCoreParts(be);
        if (mountedCore.size() != this.vabDraft.size()) {
            return true;
        }
        for (int i = 0; i < mountedCore.size(); i++) {
            VabEntry e = this.vabDraft.get(i);
            if (!e.isMounted() || !mountedCore.get(i).pos().equals(e.mountedPos)) {
                return true;
            }
        }
        return false;
    }

    private void onBuildVabRocket() {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null || !be.isFormed()) {
            return;
        }
        if (this.vabEditingMounted) {
            List<EditRocketCorePayload.CoreEntry> entries = new ArrayList<>(this.vabDraft.size());
            for (VabEntry entry : this.vabDraft) {
                if (entry.isMounted() && entry.mountedPos != null) {
                    entries.add(EditRocketCorePayload.CoreEntry.mounted(entry.mountedPos));
                } else {
                    entries.add(EditRocketCorePayload.CoreEntry.inventory(entry.inventorySlot));
                }
            }
            ClientPlayNetworking.send(new EditRocketCorePayload(be.getBlockPos(), entries, false));
            this.vabAwaitingEditSync = true;
            this.selectedInventorySlot = -1;
            this.validationTickCounter = 4;
            this.refreshState(true);
            return;
        }
        if (this.vabDraft.isEmpty()) {
            return;
        }
        List<Integer> slots = new ArrayList<>(this.vabDraft.size());
        for (VabEntry entry : this.vabDraft) {
            slots.add(entry.inventorySlot);
        }
        ClientPlayNetworking.send(new BuildRocketFromInventoryPayload(be.getBlockPos(), slots));
        this.vabDraft.clear();
        this.selectedVabIndex = -1;
        this.selectedInventorySlot = -1;
        this.activeTab = be.isAssemblyInProgress() ? FormedTab.ASSEMBLY : FormedTab.ROCKET;
        this.validationTickCounter = 4;
        this.refreshState(true);
    }

    private static RadialSymmetry resolveBoosterSymmetry(@Nullable RadialSymmetry raw) {
        if (raw == null || raw == RadialSymmetry.NONE) {
            return RadialSymmetry.TWO;
        }
        return raw;
    }

    private void onCycleSymmetry() {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null || !be.isFormed()) {
            return;
        }
        RadialSymmetry current = resolveBoosterSymmetry(be.getSelectedSymmetry());
        RadialSymmetry next = switch (current) {
            case TWO -> RadialSymmetry.FOUR_CARDINAL;
            case FOUR_CARDINAL -> RadialSymmetry.FOUR_DIAGONAL;
            case FOUR_DIAGONAL -> RadialSymmetry.EIGHT;
            default -> RadialSymmetry.TWO;
        };
        RocketPartPlacementPreviewRenderer.setActiveSymmetry(next);
        this.sendAction(Action.SET_SYMMETRY, next.ordinal());
    }

    private void onAttachRadialBoosters() {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null || !be.isFormed()) {
            return;
        }
        if (this.vabDraft.isEmpty()) {
            if (this.hasMountedRadialBoosters(be)) {
                ClientPlayNetworking.send(new EditRocketCorePayload(be.getBlockPos(), List.of(), true));
                this.validationTickCounter = 4;
                this.refreshState(true);
            }
            return;
        }
        RadialSymmetry sym = resolveBoosterSymmetry(be.getSelectedSymmetry());
        int limit = Math.min(this.vabDraft.size(), AttachRadialBoosterPayload.MAX_PARTS_PER_BOOSTER_COLUMN);
        List<Integer> slots = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            slots.add(this.vabDraft.get(i).inventorySlot);
        }
        ClientPlayNetworking.send(new AttachRadialBoosterPayload(
                be.getBlockPos(),
                slots,
                sym,
                be.getSelectedCrossfeed()
        ));
        this.vabDraft.clear();
        this.selectedVabIndex = -1;
        this.selectedInventorySlot = -1;
        this.validationTickCounter = 4;
        this.refreshState(true);
    }

    private void attachInventoryPart(int slotIndex, int insertIndex) {
        if (this.vabDraft.size() >= BuildRocketFromInventoryPayload.MAX_PARTS_PER_BUILD) {
            return;
        }
        InventoryPartEntry found = null;
        for (InventoryPartEntry e : this.inventoryParts) {
            if (e.slotIndex == slotIndex && (e.creative || e.remaining > 0)) {
                found = e;
                break;
            }
        }
        if (found == null) {
            return;
        }
        int clampedIdx = Mth.clamp(insertIndex, 0, this.vabDraft.size());
        this.vabDraft.add(clampedIdx, new VabEntry(found.slotIndex, null, found.icon, found.config, found.displayName));
        this.selectedVabIndex = clampedIdx;
        this.refreshState(false);
    }

    private int activeCanvasX() {
        return this.activeTab == FormedTab.ROCKET ? RKT_CANVAS_X : VAB_CANVAS_X;
    }

    private int activeCanvasY() {
        return this.activeTab == FormedTab.ROCKET ? RKT_CANVAS_Y : VAB_CANVAS_Y;
    }

    private int activeCanvasW() {
        return this.activeTab == FormedTab.ROCKET ? RKT_CANVAS_W : VAB_CANVAS_W;
    }

    private int activeCanvasH() {
        return this.activeTab == FormedTab.ROCKET ? RKT_CANVAS_H : VAB_CANVAS_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be != null && be.isFormed()) {
            int x = this.leftPos;
            int y = this.topPos;

            // 0. Click inside Left Destination List (Simulation tab)
            if (this.activeTab == FormedTab.SIMULATION
                    && (!this.vabDraft.isEmpty() || !this.cachedMountedSummary.parts().isEmpty())) {
                int listX = x + SIM_DEST_X + 4;
                int listY = y + SIM_DEST_Y + 18;
                int listW = SIM_DEST_W - 8;
                int count = PlanetaryEnvironment.SOLAR_SYSTEM_PRESETS.size();
                if (mouseX >= listX && mouseX < listX + listW
                        && mouseY >= listY && mouseY < listY + count * SIM_DEST_ROW_H) {
                    int row = (int) ((mouseY - listY) / SIM_DEST_ROW_H);
                    if (row >= 0 && row < count) {
                        this.simDestinationIndex = row;
                        return true;
                    }
                }
            }

            // 1. Click inside Left Inventory Parts List (Assembly VAB tab only)
            if (this.activeTab == FormedTab.ASSEMBLY) {
                int invListX = x + VAB_INV_X + 4;
                int invListY = y + VAB_INV_Y + 16;
                int invListW = VAB_INV_W - 8;
                int invListH = VAB_INV_VISIBLE_ROWS * VAB_INV_ROW_H;
                if (mouseX >= invListX && mouseX < invListX + invListW && mouseY >= invListY && mouseY < invListY + invListH) {
                    int row = (int) ((mouseY - invListY) / VAB_INV_ROW_H);
                    int idx = this.inventoryScrollOffset + row;
                    if (idx >= 0 && idx < this.inventoryParts.size()) {
                        InventoryPartEntry entry = this.inventoryParts.get(idx);
                        if (entry.creative || entry.remaining > 0) {
                            if (button == 1 || hasShiftDown() || this.selectedInventorySlot == entry.slotIndex) {
                                // Right-click, Shift-click, or second click attaches directly to top of rocket!
                                this.attachInventoryPart(entry.slotIndex, this.vabDraft.size());
                            } else {
                                this.selectedInventorySlot = entry.slotIndex;
                            }
                            return true;
                        }
                    }
                }
            }

            // 2. Click / drag inside Center 3D Viewport (VAB & Rocket tabs only)
            boolean hasRocketCanvas = this.activeTab == FormedTab.ASSEMBLY
                    || (this.activeTab == FormedTab.ROCKET && !this.cachedMountedSummary.parts().isEmpty());
            if (hasRocketCanvas) {
                int cx = x + this.activeCanvasX();
                int cy = y + this.activeCanvasY();
                int cw = this.activeCanvasW();
                int ch = this.activeCanvasH();
                if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch) {
                    if (button == 0 || button == 1 || button == 2) {
                        this.vabPointerDown = true;
                        this.vabDragged = false;
                        this.vabPressButton = button;
                        this.vabPressX = mouseX;
                        this.vabPressY = mouseY;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.vabPointerDown) {
            if (!this.vabDragged && Math.hypot(mouseX - this.vabPressX, mouseY - this.vabPressY) >= 4.0) {
                this.vabDragged = true;
                this.vabAutoSpin = false;
            }
            if (this.vabDragged) {
                this.vabYaw = (this.vabYaw + (float) dragX * 1.15F) % 360.0F;
                this.vabPitch = Mth.clamp(
                        this.vabPitch + (float) dragY * 0.85F,
                        RocketPartGuiPreviewRenderer.MIN_PITCH_DEG,
                        RocketPartGuiPreviewRenderer.MAX_PITCH_DEG
                );
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.vabPointerDown && button == this.vabPressButton) {
            boolean wasDrag = this.vabDragged;
            this.vabPointerDown = false;
            this.vabDragged = false;
            this.vabPressButton = -1;

            if (!wasDrag) {
                this.handleVabCanvasClick(mouseX, mouseY, button);
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void handleVabCanvasClick(double mouseX, double mouseY, int button) {
        int cx = this.leftPos + this.activeCanvasX();
        int cy = this.topPos + this.activeCanvasY();
        int cw = this.activeCanvasW();
        int ch = this.activeCanvasH();
        if (mouseX < cx || mouseX >= cx + cw || mouseY < cy || mouseY >= cy + ch) {
            return;
        }

        // Rocket status tab: left-click selects a mounted module for mechanical/hatch controls
        if (this.activeTab == FormedTab.ROCKET) {
            if (button == 0) {
                for (ProjectedBand band : this.lastCanvasBands) {
                    if (!band.ghost() && band.sourceIndex() >= 0 && band.contains(mouseX, mouseY)) {
                        this.selectedMountedIndex = band.sourceIndex();
                        this.refreshState(false);
                        return;
                    }
                }
            }
            return;
        }

        // Right-click cancels active selection or removes clicked draft part
        if (button == 1) {
            if (this.selectedInventorySlot >= 0) {
                this.selectedInventorySlot = -1;
                return;
            }
            if (!this.vabDraft.isEmpty()) {
                for (ProjectedBand band : this.lastCanvasBands) {
                    if (!band.ghost() && band.sourceIndex() >= 0 && band.sourceIndex() < this.vabDraft.size()
                            && band.contains(mouseX, mouseY)) {
                        this.vabDraft.remove(band.sourceIndex());
                        this.selectedVabIndex = Math.min(this.selectedVabIndex, this.vabDraft.size() - 1);
                        this.refreshState(false);
                        return;
                    }
                }
            }
            return;
        }

        // Left-click: if holding a selected inventory part, snap/insert it at the hovered slot!
        if (button == 0) {
            if (this.selectedInventorySlot >= 0) {
                int insertIdx = this.computeHoverInsertIndex(mouseY);
                this.attachInventoryPart(this.selectedInventorySlot, insertIdx);
                return;
            }
            // Otherwise select the clicked part (either VAB draft or mounted rocket)
            for (ProjectedBand band : this.lastCanvasBands) {
                if (!band.ghost() && band.sourceIndex() >= 0 && band.contains(mouseX, mouseY)) {
                    if (!this.vabDraft.isEmpty()) {
                        this.selectedVabIndex = band.sourceIndex();
                    } else {
                        this.selectedMountedIndex = band.sourceIndex();
                    }
                    this.refreshState(false);
                    return;
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be != null && be.isFormed()) {
            boolean hasRocketCanvas = this.activeTab == FormedTab.ASSEMBLY
                    || (this.activeTab == FormedTab.ROCKET && !this.cachedMountedSummary.parts().isEmpty());
            if (hasRocketCanvas) {
                int cx = this.leftPos + this.activeCanvasX();
                int cy = this.topPos + this.activeCanvasY();
                int cw = this.activeCanvasW();
                int ch = this.activeCanvasH();
                if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch) {
                    this.vabZoom = Mth.clamp(
                            this.vabZoom + (float) scrollY * 0.12F,
                            RocketPartGuiPreviewRenderer.MIN_ZOOM,
                            RocketPartGuiPreviewRenderer.MAX_ZOOM
                    );
                    return true;
                }
            }
            if (this.activeTab == FormedTab.ASSEMBLY) {
                int maxScroll = Math.max(0, this.inventoryParts.size() - VAB_INV_VISIBLE_ROWS);
                if (maxScroll > 0) {
                    this.inventoryScrollOffset = Mth.clamp(this.inventoryScrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int computeHoverInsertIndex(double mouseY) {
        List<ProjectedBand> refBands = !this.lastBaseBands.isEmpty() ? this.lastBaseBands : this.lastCanvasBands;
        if (this.vabDraft.isEmpty() || refBands.isEmpty()) {
            return 0;
        }
        // Parts are ordered bottom-to-top: index 0 is at the bottom (highest Y), last index is at top (lowest Y)
        for (ProjectedBand band : refBands) {
            if (band.ghost() || band.sourceIndex() < 0) {
                continue;
            }
            double midY = (band.y0() + band.y1()) * 0.5;
            if (mouseY > midY) {
                return band.sourceIndex();
            }
        }
        return this.vabDraft.size();
    }

    private void onTriggerSelectedPartAnim() {
        List<HangarTerminalBlockEntity.MountedRocketPart> parts = this.cachedMountedSummary.parts();
        if (this.selectedMountedIndex < 0 || this.selectedMountedIndex >= parts.size()) {
            return;
        }
        this.sendAction(Action.TRIGGER_PART_ANIM, this.selectedMountedIndex);
        this.validationTickCounter = 4;
    }

    private void onToggleSelectedPartHatch() {
        List<HangarTerminalBlockEntity.MountedRocketPart> parts = this.cachedMountedSummary.parts();
        if (this.selectedMountedIndex < 0 || this.selectedMountedIndex >= parts.size()) {
            return;
        }
        this.sendAction(Action.TOGGLE_PART_HATCH, this.selectedMountedIndex);
        this.validationTickCounter = 4;
    }

    private void onToggleType() {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null || be.isFormed()) {
            return;
        }
        PlatformType current = be.getPlatformType() != null ? be.getPlatformType() : PlatformType.VERTICAL;
        PlatformType next = current == PlatformType.VERTICAL ? PlatformType.HORIZONTAL : PlatformType.VERTICAL;
        this.sendAction(Action.SET_TYPE, next.ordinal());
    }

    private void onStepSize(int delta) {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null || be.isFormed()) {
            return;
        }
        PlatformType type = be.getPlatformType() != null ? be.getPlatformType() : PlatformType.VERTICAL;
        PlatformSize current = be.getPlatformSize() != null ? be.getPlatformSize() : PlatformSize.SIZE_1X1;
        List<PlatformSize> valid = supportedSizes(type);
        if (valid.isEmpty()) {
            return;
        }
        int idx = valid.indexOf(current);
        if (idx < 0) {
            idx = 0;
        }
        int nextIdx = Math.floorMod(idx + delta, valid.size());
        this.sendAction(Action.SET_SIZE, valid.get(nextIdx).ordinal());
    }

    private void onTogglePreview() {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null) {
            return;
        }
        this.sendAction(Action.TOGGLE_PREVIEW, be.isPreviewEnabled() ? 0 : 1);
    }

    private void onStepLayer(int delta) {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null) {
            return;
        }
        PlatformBlueprint blueprint = be.getActiveBlueprint();
        int maxY = blueprint != null ? Math.max(0, blueprint.maxY()) : 0;
        int current = be.getPreviewLayerY();
        int next = current + delta;
        if (next < -1) {
            next = maxY;
        } else if (next > maxY) {
            next = -1;
        }
        this.sendAction(Action.SET_LAYER_Y, next);
    }

    private void onPrimaryAction() {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null) {
            return;
        }
        if (be.isFormed()) {
            this.sendAction(Action.DISASSEMBLE, 0);
        } else if ((this.cachedSummary != null && this.cachedSummary.readyToAssemble()) || this.isCreativePlayer()) {
            this.sendAction(Action.ASSEMBLE, 0);
        }
    }

    private boolean isCreativePlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isCreative();
    }

    private void sendAction(Action action, int value) {
        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null) {
            return;
        }
        ClientPlayNetworking.send(new HangarTerminalActionPayload(be.getBlockPos(), action, value));
    }

    private static List<PlatformSize> supportedSizes(PlatformType type) {
        List<PlatformSize> list = new ArrayList<>();
        for (PlatformSize size : PlatformSize.values()) {
            if (size.supports(type)) {
                list.add(size);
            }
        }
        return list;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (this.menu.blockEntity != null && this.menu.blockEntity.isFormed()) {
            if (this.vabAnimButton != null && this.vabAnimButton.visible && this.vabAnimButton.isHovered()) {
                graphics.renderTooltip(
                        this.font,
                        Component.literal(this.vabAnimEnabled ? "Pausar Animação 3D" : "Testar Animação 3D"),
                        mouseX,
                        mouseY
                );
            } else if (this.vabResetViewButton != null && this.vabResetViewButton.visible && this.vabResetViewButton.isHovered()) {
                graphics.renderTooltip(
                        this.font,
                        Component.literal("Resetar Câmera 3D"),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Custom aerospace header and section labels are drawn in renderBg.
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int panelW = this.imageWidth;
        int panelH = this.imageHeight;

        // Main outer panel
        graphics.fill(x, y, x + panelW, y + panelH, COLOR_BG_PANEL);
        graphics.renderOutline(x, y, panelW, panelH, COLOR_BORDER_OUTER);
        graphics.renderOutline(x + 1, y + 1, panelW - 2, panelH - 2, COLOR_BORDER_INNER);

        HangarTerminalBlockEntity be = this.menu.blockEntity;
        if (be == null) {
            return;
        }

        boolean formed = be.isFormed();

        // Header bar
        graphics.fill(x + 2, y + 2, x + panelW - 2, y + 20, COLOR_BG_HEADER);
        graphics.fill(x + 2, y + 19, x + panelW - 2, y + 20, COLOR_ACCENT_CYAN);
        String headerTitle;
        if (!formed) {
            headerTitle = "Terminal do Hangar";
        } else if (this.activeTab == FormedTab.ROCKET) {
            headerTitle = "Plataforma — Status do Foguete (" + be.getPlatformSize().getSerializedName() + ")";
        } else if (this.activeTab == FormedTab.SIMULATION) {
            headerTitle = "Simulador de Física de Voo (" + be.getPlatformSize().getSerializedName() + ")";
        } else {
            headerTitle = "VAB — Montagem de Foguete (" + be.getPlatformSize().getSerializedName() + ")";
        }
        graphics.drawString(this.font, Component.literal(headerTitle), x + 8, y + 7, COLOR_TEXT_TITLE, false);

        if (formed) {
            if (this.activeTab == FormedTab.ROCKET) {
                this.renderRocketTabMode(graphics, x, y, mouseX, mouseY, be);
            } else if (this.activeTab == FormedTab.SIMULATION) {
                this.renderSimulationTabMode(graphics, x, y, mouseX, mouseY, be);
            } else {
                this.renderVabMode(graphics, x, y, mouseX, mouseY, be);
            }
            return;
        }

        this.renderConfigMode(graphics, x, y, be);
    }

    private void renderConfigMode(GuiGraphics graphics, int x, int y, HangarTerminalBlockEntity be) {
        PlatformBlueprint.ValidationSummary summary = this.cachedSummary != null
                ? this.cachedSummary
                : be.validateStructure();

        String pillText;
        int pillBg;
        int pillBorder;
        int pillColor;
        if (summary.readyToAssemble()) {
            pillText = "PRONTA";
            pillBg = 0xFF123344;
            pillBorder = 0xFF38BDF8;
            pillColor = 0xFF7DD3FC;
        } else if (this.isCreativePlayer()) {
            pillText = "CRIATIVO";
            pillBg = 0xFF143826;
            pillBorder = 0xFF34D399;
            pillColor = COLOR_TEXT_OK;
        } else {
            pillText = "CONFIG";
            pillBg = 0xFF1E293B;
            pillBorder = 0xFF475569;
            pillColor = COLOR_TEXT_MUTED;
        }
        int pillW = 60;
        int pillX = x + PANEL_WIDTH_CONFIG - pillW - 6;
        int pillY = y + 5;
        graphics.fill(pillX, pillY, pillX + pillW, pillY + 11, pillBg);
        graphics.renderOutline(pillX, pillY, pillW, 11, pillBorder);
        graphics.drawCenteredString(this.font, pillText, pillX + pillW / 2, pillY + 2, pillColor);

        // Left column: Configuration
        int lx = x + COL_LEFT_X;
        int ly = y + COL_TOP_Y;
        graphics.fill(lx, ly, lx + COL_LEFT_W, ly + COL_HEIGHT, COLOR_BG_SECTION);
        graphics.renderOutline(lx, ly, COL_LEFT_W, COL_HEIGHT, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Configuração", lx + 6, ly + 5, COLOR_TEXT_HEADER, false);

        PlatformSize size = be.getPlatformSize() != null ? be.getPlatformSize() : PlatformSize.SIZE_1X1;
        int sizeBoxX = lx + 26;
        int sizeBoxY = ly + 39;
        int sizeBoxW = 86;
        graphics.fill(sizeBoxX, sizeBoxY, sizeBoxX + sizeBoxW, sizeBoxY + 18, COLOR_BG_INSET);
        graphics.renderOutline(sizeBoxX, sizeBoxY, sizeBoxW, 18, COLOR_BORDER_SECTION);
        String sizeLabel = size.getSerializedName() + "  (Ø" + size.diameter() + "m)";
        graphics.drawCenteredString(this.font, sizeLabel, sizeBoxX + sizeBoxW / 2, sizeBoxY + 5, COLOR_TEXT_PRIMARY);

        int detectedSpan = be.getDetectedPadSpan();
        PlatformType type = be.getPlatformType() != null ? be.getPlatformType() : PlatformType.VERTICAL;
        if (detectedSpan > 0) {
            String badge = type == PlatformType.HORIZONTAL
                    ? "Pad detectado: " + detectedSpan + " bl."
                    : "Pad detectado: " + detectedSpan + "x" + detectedSpan;
            graphics.fill(lx + 6, ly + 60, lx + COL_LEFT_W - 6, ly + 71, 0xFF102E2C);
            graphics.renderOutline(lx + 6, ly + 60, COL_LEFT_W - 12, 11, 0xFF2DD4BF);
            graphics.drawCenteredString(this.font, badge, lx + COL_LEFT_W / 2, ly + 62, COLOR_TEXT_BADGE);
        } else {
            graphics.drawString(this.font, "Pad: não detectado", lx + 8, ly + 62, COLOR_TEXT_MUTED, false);
        }

        int layerY = be.getPreviewLayerY();
        int layerBoxX = lx + 26;
        int layerBoxY = ly + 119;
        int layerBoxW = 86;
        graphics.fill(layerBoxX, layerBoxY, layerBoxX + layerBoxW, layerBoxY + 18, COLOR_BG_INSET);
        graphics.renderOutline(layerBoxX, layerBoxY, layerBoxW, 18, COLOR_BORDER_SECTION);
        String layerLabel = layerY < 0 ? "Camada: Todas" : ("Camada: Y=" + layerY);
        graphics.drawCenteredString(this.font, layerLabel, layerBoxX + layerBoxW / 2, layerBoxY + 5, COLOR_TEXT_PRIMARY);

        // Right column: Live material checklist
        int rx = x + COL_RIGHT_X;
        int ry = y + COL_TOP_Y;
        graphics.fill(rx, ry, rx + COL_RIGHT_W, ry + COL_HEIGHT, COLOR_BG_SECTION);
        graphics.renderOutline(rx, ry, COL_RIGHT_W, COL_HEIGHT, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Materiais", rx + 6, ry + 5, COLOR_TEXT_HEADER, false);

        int totalRequired = Math.max(0, summary.totalRequired());
        int totalPlaced = Math.max(0, summary.totalPlaced());
        int pct = totalRequired > 0 ? Math.min(100, (totalPlaced * 100) / totalRequired) : 0;
        String pctText = pct + "%";
        int pctColor = summary.readyToAssemble() ? COLOR_TEXT_OK : COLOR_TEXT_PRIMARY;
        graphics.drawString(this.font, pctText, rx + COL_RIGHT_W - 6 - this.font.width(pctText), ry + 5, pctColor, false);

        for (int i = 0; i < ROLE_ORDER.length; i++) {
            PlatformBlockRole role = ROLE_ORDER[i];
            int rowY = ry + 18 + i * 22;

            graphics.fill(rx + 5, rowY, rx + COL_RIGHT_W - 5, rowY + 20, COLOR_BG_INSET);
            graphics.renderOutline(rx + 5, rowY, COL_RIGHT_W - 10, 20, 0xFF1E2D40);

            ItemStack icon = this.roleIcons.getOrDefault(role, ItemStack.EMPTY);
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, rx + 7, rowY + 2);
            }

            int req = summary.requiredCounts().getOrDefault(role, 0);
            int placed = summary.placedCounts().getOrDefault(role, 0);
            int countColor = placed >= req ? COLOR_TEXT_OK : (placed > 0 ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED);

            graphics.drawString(this.font, roleLabel(role), rx + 26, rowY + 2, COLOR_TEXT_PRIMARY, false);
            graphics.drawString(this.font, placed + " / " + req, rx + 26, rowY + 11, countColor, false);
        }

        int barX = rx + 6;
        int barY = ry + 109;
        int barW = COL_RIGHT_W - 12;
        int barH = 7;
        graphics.fill(barX, barY, barX + barW, barY + barH, COLOR_BG_INSET);
        graphics.renderOutline(barX, barY, barW, barH, COLOR_BORDER_SECTION);

        float ratio = totalRequired > 0 ? Math.min(1.0F, (float) totalPlaced / (float) totalRequired) : 0.0F;
        int fillW = Math.round((barW - 2) * ratio);
        int barColor = summary.readyToAssemble()
                ? 0xFF22C55E
                : (summary.wrongCount() > 0 ? 0xFFEF4444 : (summary.clearanceBlockedCount() > 0 ? 0xFFF59E0B : 0xFF38BDF8));
        if (fillW > 0) {
            graphics.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, barColor);
        }

        int statusY = ry + 120;
        if (summary.readyToAssemble()) {
            graphics.drawString(this.font, "Pronto para Montar!", rx + 6, statusY, COLOR_TEXT_OK, false);
            graphics.drawString(this.font, "Total: " + totalPlaced + "/" + totalRequired, rx + 6, statusY + 10, COLOR_TEXT_MUTED, false);
        } else {
            String primaryText;
            int primaryColor;
            String secondaryText = "";
            int secondaryColor = COLOR_TEXT_MUTED;

            if (summary.wrongCount() > 0) {
                primaryText = "Errados: " + summary.wrongCount();
                primaryColor = COLOR_TEXT_ERROR;
                if (summary.missingCount() > 0) {
                    secondaryText = "Faltam: " + summary.missingCount();
                    secondaryColor = COLOR_TEXT_AMBER;
                }
            } else if (summary.clearanceBlockedCount() > 0) {
                primaryText = "Obstrução: " + summary.clearanceBlockedCount();
                primaryColor = COLOR_TEXT_WARN;
            } else {
                primaryText = "Faltam: " + summary.missingCount();
                primaryColor = COLOR_TEXT_AMBER;
                secondaryText = "Total: " + totalPlaced + "/" + totalRequired;
            }
            graphics.drawString(this.font, primaryText, rx + 6, statusY, primaryColor, false);
            if (!secondaryText.isEmpty()) {
                graphics.drawString(this.font, secondaryText, rx + 6, statusY + 10, secondaryColor, false);
            }
        }
    }

    private void advanceVabPreviewClock() {
        long now = System.nanoTime();
        if (this.lastVabFrameNanos == 0L) {
            this.lastVabFrameNanos = now;
            return;
        }
        float dtSec = Mth.clamp((now - this.lastVabFrameNanos) / 1_000_000_000.0F, 0.0F, 0.1F);
        this.lastVabFrameNanos = now;
        if (!this.vabDragged && this.vabAutoSpin) {
            this.vabYaw = (this.vabYaw + dtSec * VAB_SPIN_DEG_PER_SEC) % 360.0F;
        }
        boolean anyMountedAnim = false;
        for (HangarTerminalBlockEntity.MountedRocketPart part : this.cachedMountedSummary.parts()) {
            if (part.animationActive()) {
                anyMountedAnim = true;
                break;
            }
        }
        if (this.vabAnimEnabled || anyMountedAnim) {
            this.vabAnimTime += dtSec;
        }
    }

    private @Nullable InventoryPartEntry findInventoryEntry(int slotIndex) {
        if (slotIndex < 0) {
            return null;
        }
        for (InventoryPartEntry entry : this.inventoryParts) {
            if (entry.slotIndex == slotIndex && (entry.creative || entry.remaining > 0)) {
                return entry;
            }
        }
        return null;
    }

    private void renderVabMode(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, HangarTerminalBlockEntity be) {
        this.advanceVabPreviewClock();
        boolean showingDraft = !this.vabDraft.isEmpty() || this.vabEditingMounted;
        boolean editDirty = this.vabEditingMounted && this.isVabEditDirty(be);
        VabTelemetry telemetry = (showingDraft && (!this.vabEditingMounted || editDirty))
                ? this.computeDraftTelemetry(be)
                : this.fromMountedSummary(this.cachedMountedSummary);

        // Header status pill
        String pillText;
        int pillBg;
        int pillBorder;
        int pillColor;
        if (this.vabEditingMounted && editDirty) {
            pillText = "EDITAR (" + this.vabDraft.size() + ")";
            pillBg = 0xFF2B2310;
            pillBorder = 0xFFFACC15;
            pillColor = COLOR_TEXT_AMBER;
        } else if (!this.vabEditingMounted && !this.vabDraft.isEmpty()) {
            pillText = "VAB (" + this.vabDraft.size() + ")";
            pillBg = 0xFF2B2310;
            pillBorder = 0xFFFACC15;
            pillColor = COLOR_TEXT_AMBER;
        } else if (telemetry.ready) {
            pillText = "PRONTO";
            pillBg = 0xFF143826;
            pillBorder = 0xFF34D399;
            pillColor = COLOR_TEXT_OK;
        } else if (telemetry.partCount > 0) {
            pillText = "NO PAD";
            pillBg = 0xFF123344;
            pillBorder = 0xFF38BDF8;
            pillColor = 0xFF7DD3FC;
        } else {
            pillText = "VAZIO";
            pillBg = 0xFF1E293B;
            pillBorder = 0xFF475569;
            pillColor = COLOR_TEXT_MUTED;
        }
        int pillW = 64;
        int pillX = x + PANEL_WIDTH_VAB - pillW - 6;
        int pillY = y + 5;
        graphics.fill(pillX, pillY, pillX + pillW, pillY + 11, pillBg);
        graphics.renderOutline(pillX, pillY, pillW, 11, pillBorder);
        graphics.drawCenteredString(this.font, pillText, pillX + pillW / 2, pillY + 2, pillColor);

        // --- Column 1: Player Inventory Rocket Parts ---
        int ix = x + VAB_INV_X;
        int iy = y + VAB_INV_Y;
        graphics.fill(ix, iy, ix + VAB_INV_W, iy + VAB_INV_H, COLOR_BG_SECTION);
        graphics.renderOutline(ix, iy, VAB_INV_W, VAB_INV_H, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Peças (Inv.)", ix + 5, iy + 4, COLOR_TEXT_HEADER, false);
        String invCount = String.valueOf(this.inventoryParts.size());
        graphics.drawString(this.font, invCount, ix + VAB_INV_W - 5 - this.font.width(invCount), iy + 4, COLOR_TEXT_MUTED, false);

        int listX = ix + 4;
        int listY = iy + 16;
        int listW = VAB_INV_W - 8;

        InventoryPartEntry hoveredInventoryEntry = null;
        if (this.inventoryParts.isEmpty()) {
            graphics.drawCenteredString(this.font, "Sem peças", ix + VAB_INV_W / 2, iy + 84, COLOR_TEXT_MUTED);
            graphics.drawCenteredString(this.font, "no inventário", ix + VAB_INV_W / 2, iy + 96, COLOR_TEXT_MUTED);
        } else {
            for (int row = 0; row < VAB_INV_VISIBLE_ROWS; row++) {
                int idx = this.inventoryScrollOffset + row;
                if (idx >= this.inventoryParts.size()) {
                    break;
                }
                InventoryPartEntry entry = this.inventoryParts.get(idx);
                int ry = listY + row * VAB_INV_ROW_H;
                boolean hovered = mouseX >= listX && mouseX < listX + listW && mouseY >= ry && mouseY < ry + VAB_INV_ROW_H - 2;
                boolean selected = entry.slotIndex == this.selectedInventorySlot;
                boolean available = entry.creative || entry.remaining > 0;
                if (hovered) {
                    hoveredInventoryEntry = entry;
                }

                int bg = selected ? 0xFF17354D : (hovered && available ? 0xFF162436 : COLOR_BG_INSET);
                int border = selected ? 0xFF38BDF8 : (hovered && available ? 0xFF47698A : 0xFF1E2D40);
                graphics.fill(listX, ry, listX + listW, ry + VAB_INV_ROW_H - 2, bg);
                graphics.renderOutline(listX, ry, listW, VAB_INV_ROW_H - 2, border);

                graphics.renderItem(entry.icon, listX + 2, ry + 3);
                String shortName = this.font.plainSubstrByWidth(entry.displayName, listW - 24);
                graphics.drawString(this.font, shortName, listX + 20, ry + 2, available ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED, false);

                String meta = entry.config.diameter() + "x" + entry.config.diameter()
                        + " h" + entry.config.heightBlocks()
                        + " " + categoryShort(entry.config.category());
                graphics.drawString(this.font, meta, listX + 20, ry + 12, categoryColor(entry.config.category()), false);

                String qty = entry.creative ? "∞" : ("x" + entry.remaining);
                int qtyColor = available ? COLOR_TEXT_OK : COLOR_TEXT_ERROR;
                graphics.drawString(this.font, qty, listX + listW - 3 - this.font.width(qty), ry + 12, qtyColor, false);
            }
        }

        // Bottom hint inside left panel
        String hint = this.selectedInventorySlot >= 0 ? "Clique no VAB ->" : "Clique/Shift+Clk";
        graphics.drawCenteredString(this.font, hint, ix + VAB_INV_W / 2, iy + VAB_INV_H - 12,
                this.selectedInventorySlot >= 0 ? COLOR_TEXT_OK : COLOR_TEXT_MUTED);

        // --- Column 2: KSP-Style Interactive 3D VAB Viewport ---
        int sx = x + VAB_STAGE_X;
        int sy = y + VAB_STAGE_Y;
        graphics.fill(sx, sy, sx + VAB_STAGE_W, sy + VAB_STAGE_H, COLOR_BG_SECTION);
        graphics.renderOutline(sx, sy, VAB_STAGE_W, VAB_STAGE_H, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "VAB 3D", sx + 5, sy + 4, COLOR_TEXT_HEADER, false);
        String camHint = "Arraste/Scroll";
        graphics.drawString(this.font, camHint, sx + VAB_STAGE_W - 5 - this.font.width(camHint), sy + 4, COLOR_TEXT_MUTED, false);

        int cx = x + VAB_CANVAS_X;
        int cy = y + VAB_CANVAS_Y;
        boolean canvasHovered = mouseX >= cx && mouseX < cx + VAB_CANVAS_W && mouseY >= cy && mouseY < cy + VAB_CANVAS_H;
        RocketPartGuiPreviewRenderer.renderViewportFrame(graphics, cx, cy, VAB_CANVAS_W, VAB_CANVAS_H, canvasHovered || this.vabDragged, true);

        int centerX = cx + VAB_CANVAS_W / 2;
        int floorY = cy + VAB_CANVAS_H - 7;
        graphics.fill(cx + 8, floorY, cx + VAB_CANVAS_W - 8, floorY + 2, 0xFF3B4D63);

        InventoryPartEntry selectedInventoryEntry = this.findInventoryEntry(this.selectedInventorySlot);

        this.lastCanvasBands.clear();
        boolean hasMounted = !this.cachedMountedSummary.parts().isEmpty();
        if (this.vabBoosterMode && hasMounted) {
            this.renderBoosterPreview3D(graphics, cx, cy, VAB_CANVAS_W, VAB_CANVAS_H, be, selectedInventoryEntry, canvasHovered);
            if (hoveredInventoryEntry != null) {
                this.renderPartPreviewPip(graphics, cx, cy, hoveredInventoryEntry);
            }
        } else if (showingDraft) {
            this.renderVabStack3D(graphics, cx, cy, centerX, mouseX, mouseY, canvasHovered, selectedInventoryEntry, be);
            if (hoveredInventoryEntry != null) {
                this.renderPartPreviewPip(graphics, cx, cy, hoveredInventoryEntry);
            }
        } else if (hasMounted) {
            if (selectedInventoryEntry != null && canvasHovered) {
                this.renderSinglePartVabPreview(graphics, cx, cy, centerX, selectedInventoryEntry, true);
            } else {
                this.renderMountedStack3D(graphics, cx, cy, VAB_CANVAS_W, VAB_CANVAS_H, be);
                if (hoveredInventoryEntry != null) {
                    this.renderPartPreviewPip(graphics, cx, cy, hoveredInventoryEntry);
                }
            }
        } else {
            InventoryPartEntry previewEntry = hoveredInventoryEntry != null ? hoveredInventoryEntry : selectedInventoryEntry;
            if (previewEntry != null) {
                boolean ghostSnap = selectedInventoryEntry != null && canvasHovered;
                this.renderSinglePartVabPreview(graphics, cx, cy, centerX, previewEntry, ghostSnap);
            } else {
                graphics.drawCenteredString(this.font, "Selecione peças", centerX, cy + 68, COLOR_TEXT_MUTED);
                graphics.drawCenteredString(this.font, "à esquerda para ver", centerX, cy + 80, COLOR_TEXT_MUTED);
                graphics.drawCenteredString(this.font, "o preview 3D", centerX, cy + 92, COLOR_TEXT_BADGE);
            }
        }

        if (be.isAssemblyInProgress()) {
            this.renderAssemblyProgressOverlay(graphics, cx, cy, VAB_CANVAS_W, VAB_CANVAS_H, be);
        }

        // --- Column 3: Telemetry & Controls ---
        int rx = x + VAB_INFO_X;
        int ry = y + VAB_INFO_Y;
        graphics.fill(rx, ry, rx + VAB_INFO_W, ry + VAB_INFO_H, COLOR_BG_SECTION);
        graphics.renderOutline(rx, ry, VAB_INFO_W, VAB_INFO_H, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Telemetria", rx + 5, ry + 4, COLOR_TEXT_HEADER, false);

        int boxX = rx + 4;
        int boxY = ry + 16;
        int boxW = VAB_INFO_W - 8;
        int boxH = 92;
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, COLOR_BG_INSET);
        graphics.renderOutline(boxX, boxY, boxW, boxH, 0xFF1E2D40);

        RocketAssemblyGraph graph = showingDraft
                ? this.buildPreviewGraph(be).graph()
                : be.getAssemblyGraph();
        int stageCount = graph.activeStages().size();
        graphics.drawString(this.font, "Pçs:" + telemetry.partCount + " Est:" + Math.max(stageCount, telemetry.partCount > 0 ? 1 : 0),
                boxX + 4, boxY + 4, COLOR_TEXT_PRIMARY, false);
        graphics.drawString(this.font, "Alt: " + telemetry.totalHeight + "bl  Ø" + telemetry.maxDiameter + "/" + telemetry.allowedDiameter,
                boxX + 4, boxY + 14, telemetry.diameterOk ? COLOR_TEXT_PRIMARY : COLOR_TEXT_ERROR, false);
        graphics.drawString(this.font, String.format("Seca: %.1ft", telemetry.dryMassKg / 1000.0), boxX + 4, boxY + 25, COLOR_TEXT_PRIMARY, false);
        graphics.drawString(this.font, String.format("Total: %.1ft", telemetry.wetMassKg / 1000.0), boxX + 4, boxY + 35, COLOR_TEXT_PRIMARY, false);
        graphics.drawString(this.font, String.format("Emp: %.0fkN", telemetry.thrustKn), boxX + 4, boxY + 45, COLOR_TEXT_PRIMARY, false);
        int twrColor = telemetry.partCount == 0 ? COLOR_TEXT_MUTED
                : (telemetry.twr >= 1.2 ? COLOR_TEXT_OK : (telemetry.twr >= 1.0 ? COLOR_TEXT_AMBER : COLOR_TEXT_ERROR));
        graphics.drawString(this.font, String.format("TWR: %.2f", telemetry.twr), boxX + 4, boxY + 55, twrColor, false);
        graphics.drawString(this.font, String.format("dV: %.0fm/s", telemetry.deltaV), boxX + 4, boxY + 65, COLOR_TEXT_BADGE, false);

        drawMiniBadge(graphics, boxX + 3, boxY + 77, "CMD", telemetry.hasCommand);
        drawMiniBadge(graphics, boxX + 25, boxY + 77, "TNK", telemetry.hasTank);
        drawMiniBadge(graphics, boxX + 47, boxY + 77, "ENG", telemetry.hasEngine);
        drawMiniBadge(graphics, boxX + 69, boxY + 77, "TWR", telemetry.twr >= 1.0 && telemetry.diameterOk);

        // Compact live mode / booster hint box at bottom of right column (replaces Desmontar Pad)
        int hintX = rx + 4;
        int hintY = ry + 182;
        int hintW = VAB_INFO_W - 8;
        int hintH = 22;
        graphics.fill(hintX, hintY, hintX + hintW, hintY + hintH, COLOR_BG_INSET);
        graphics.renderOutline(hintX, hintY, hintW, hintH, this.vabBoosterMode ? 0xFF2D8AC4 : 0xFF1E2D40);
        RadialSymmetry sym = resolveBoosterSymmetry(be.getSelectedSymmetry());
        String line1;
        String line2;
        int l1Color;
        if (this.vabBoosterMode) {
            if (!hasMounted) {
                line1 = "Sem núcleo no pad";
                line2 = "Construa núcleo 1º";
                l1Color = COLOR_TEXT_AMBER;
            } else if (this.vabDraft.isEmpty()) {
                line1 = "Boosters " + sym.count() + "x";
                line2 = this.hasMountedRadialBoosters(be) ? "Rem. ou clique pç" : "Clique peças à esq.";
                l1Color = COLOR_TEXT_BADGE;
            } else if (!telemetry.diameterOk) {
                line1 = "Excede folga!";
                line2 = "Use peça menor";
                l1Color = COLOR_TEXT_ERROR;
            } else {
                line1 = "Boosters " + sym.count() + "x OK";
                line2 = be.getSelectedCrossfeed() == CrossfeedMode.CONTINUOUS_CROSSFEED ? "Asparagus ativo" : "Dreno isolado";
                l1Color = COLOR_TEXT_OK;
            }
        } else if (this.vabEditingMounted) {
            line1 = editDirty ? "Edição Pendente" : "Editando Núcleo";
            line2 = editDirty ? "Clique Aplicar Ed." : "Clique/Dir. p/ editar";
            l1Color = editDirty ? COLOR_TEXT_AMBER : COLOR_TEXT_BADGE;
        } else {
            line1 = !this.vabDraft.isEmpty() ? "Rascunho Núcleo" : (hasMounted ? "Núcleo no Pad" : "Pad Pronto");
            line2 = hasMounted ? "Use Modo: Boosters" : "Clique peças à esq.";
            l1Color = !this.vabDraft.isEmpty() ? COLOR_TEXT_AMBER : COLOR_TEXT_HEADER;
        }
        graphics.drawCenteredString(this.font, this.font.plainSubstrByWidth(line1, hintW - 4), hintX + hintW / 2, hintY + 3, l1Color);
        graphics.drawCenteredString(this.font, this.font.plainSubstrByWidth(line2, hintW - 4), hintX + hintW / 2, hintY + 12, COLOR_TEXT_MUTED);
    }

    private void renderAssemblyProgressOverlay(
            GuiGraphics graphics,
            int cx,
            int cy,
            int cw,
            int ch,
            HangarTerminalBlockEntity be
    ) {
        int barBoxW = cw - 12;
        int barBoxH = 24;
        int bx = cx + 6;
        int by = cy + ch - barBoxH - 6;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, VAB_HUD_Z + 5.0F);
        graphics.fill(bx, by, bx + barBoxW, by + barBoxH, 0xE60B1522);
        graphics.renderOutline(bx, by, barBoxW, barBoxH, 0xFF38BDF8);

        float pct = be.getAssemblyProgress();
        int pctInt = Math.round(pct * 100.0F);
        String label = "Soldando: " + be.getAssemblyCompletedSteps() + "/" + be.getAssemblyTotalSteps() + " (" + pctInt + "%)";
        graphics.drawCenteredString(this.font, label, bx + barBoxW / 2, by + 3, COLOR_TEXT_AMBER);

        int pBarX = bx + 4;
        int pBarY = by + 14;
        int pBarW = barBoxW - 8;
        graphics.fill(pBarX, pBarY, pBarX + pBarW, pBarY + 6, COLOR_BG_INSET);
        graphics.renderOutline(pBarX, pBarY, pBarW, 6, COLOR_BORDER_SECTION);
        int fillW = Math.round((pBarW - 2) * Mth.clamp(pct, 0.0F, 1.0F));
        if (fillW > 0) {
            graphics.fill(pBarX + 1, pBarY + 1, pBarX + 1 + fillW, pBarY + 5, 0xFF38BDF8);
        }
        pose.popPose();
    }

    private List<ItemStack> collectDraftStacks() {
        List<ItemStack> stacks = new ArrayList<>(this.vabDraft.size());
        for (VabEntry entry : this.vabDraft) {
            stacks.add(entry.icon);
        }
        return stacks;
    }

    private void renderRocketTabMode(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, HangarTerminalBlockEntity be) {
        this.advanceVabPreviewClock();
        HangarTerminalBlockEntity.RocketPadSummary summary = this.cachedMountedSummary;
        List<HangarTerminalBlockEntity.MountedRocketPart> parts = summary.parts();
        boolean hasMounted = !parts.isEmpty();

        // Header status pill
        String pillText;
        int pillBg;
        int pillBorder;
        int pillColor;
        if (!hasMounted) {
            pillText = "VAZIO";
            pillBg = 0xFF1E293B;
            pillBorder = 0xFF475569;
            pillColor = COLOR_TEXT_MUTED;
        } else if (summary.unifiedAssembly() && summary.readyForLaunch()) {
            pillText = "PRONTO";
            pillBg = 0xFF143826;
            pillBorder = 0xFF34D399;
            pillColor = COLOR_TEXT_OK;
        } else if (summary.unifiedAssembly()) {
            pillText = "UNIFICADO";
            pillBg = 0xFF102E2C;
            pillBorder = 0xFF2DD4BF;
            pillColor = COLOR_TEXT_BADGE;
        } else {
            pillText = "NO PAD";
            pillBg = 0xFF2B2310;
            pillBorder = 0xFFFACC15;
            pillColor = COLOR_TEXT_AMBER;
        }
        int pillW = 68;
        int pillX = x + PANEL_WIDTH_VAB - pillW - 6;
        int pillY = y + 5;
        graphics.fill(pillX, pillY, pillX + pillW, pillY + 11, pillBg);
        graphics.renderOutline(pillX, pillY, pillW, 11, pillBorder);
        graphics.drawCenteredString(this.font, pillText, pillX + pillW / 2, pillY + 2, pillColor);

        if (!hasMounted) {
            int cardX = x + 36;
            int cardY = y + 72;
            int cardW = PANEL_WIDTH_VAB - 72;
            int cardH = 114;
            graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, COLOR_BG_SECTION);
            graphics.renderOutline(cardX, cardY, cardW, cardH, COLOR_BORDER_SECTION);
            graphics.drawCenteredString(this.font, "Nenhum foguete construído na plataforma", x + PANEL_WIDTH_VAB / 2, cardY + 24, COLOR_TEXT_TITLE);
            graphics.drawCenteredString(this.font, "Monte as peças na aba Montagem (VAB) para", x + PANEL_WIDTH_VAB / 2, cardY + 42, COLOR_TEXT_MUTED);
            graphics.drawCenteredString(this.font, "criar uma estrutura unificada com telemetria.", x + PANEL_WIDTH_VAB / 2, cardY + 54, COLOR_TEXT_MUTED);
            return;
        }

        // --- Column 1: Rocket Resources & Subsystems (Fuel, Energy, Oxygen, Integrity, Tolerances) ---
        int sx = x + RKT_SYS_X;
        int sy = y + RKT_SYS_Y;
        graphics.fill(sx, sy, sx + RKT_SYS_W, sy + RKT_SYS_H, COLOR_BG_SECTION);
        graphics.renderOutline(sx, sy, RKT_SYS_W, RKT_SYS_H, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Sistemas & Recursos", sx + 5, sy + 4, COLOR_TEXT_HEADER, false);

        // Unified assembly badge
        int badgeX = sx + 5;
        int badgeY = sy + 16;
        int badgeW = RKT_SYS_W - 10;
        boolean unified = summary.unifiedAssembly();
        graphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 11, unified ? 0xFF102E2C : 0xFF2B2310);
        graphics.renderOutline(badgeX, badgeY, badgeW, 11, unified ? 0xFF2DD4BF : 0xFFFACC15);
        graphics.drawCenteredString(
                this.font,
                unified ? "ESTRUTURA UNIFICADA" : "PEÇAS SOLTAS NO PAD",
                badgeX + badgeW / 2,
                badgeY + 2,
                unified ? COLOR_TEXT_BADGE : COLOR_TEXT_AMBER
        );

        // 1) Combustível (Fuel)
        double curFuelB = summary.currentFuelDroplets() / 81_000.0;
        double maxFuelB = summary.totalFuelDroplets() / 81_000.0;
        float fuelRatio = summary.totalFuelDroplets() > 0L
                ? Mth.clamp((float) summary.currentFuelDroplets() / (float) summary.totalFuelDroplets(), 0.0F, 1.0F)
                : 0.0F;
        String fuelValue = summary.totalFuelDroplets() > 0L
                ? String.format("%.1f / %.1f B", curFuelB, maxFuelB)
                : "Sem tanques";
        this.drawResourceGauge(
                graphics, sx + 5, sy + 32, RKT_SYS_W - 10,
                "Combustível",
                summary.totalFuelDroplets() > 0L ? (Math.round(fuelRatio * 100.0F) + "%") : "--",
                fuelValue,
                fuelRatio,
                0xFFF59E0B,
                COLOR_TEXT_AMBER
        );

        // 2) Energia (Energy)
        long curEnergy = summary.currentEnergy();
        long maxEnergy = summary.energyCapacity();
        float energyRatio = maxEnergy > 0L
                ? Mth.clamp((float) curEnergy / (float) maxEnergy, 0.0F, 1.0F)
                : 0.0F;
        String energyValue = maxEnergy > 0L
                ? formatCompactEnergy(curEnergy) + " / " + formatCompactEnergy(maxEnergy) + " GJ"
                : "Sem bateria";
        this.drawResourceGauge(
                graphics, sx + 5, sy + 64, RKT_SYS_W - 10,
                "Energia",
                maxEnergy > 0L ? (Math.round(energyRatio * 100.0F) + "%") : "--",
                energyValue,
                energyRatio,
                0xFF38BDF8,
                0xFF7DD3FC
        );

        // 3) Oxigênio (O2)
        double curO2B = summary.currentOxygenDroplets() / 81_000.0;
        double maxO2B = summary.oxygenCapacityDroplets() / 81_000.0;
        float o2Ratio = summary.oxygenCapacityDroplets() > 0L
                ? Mth.clamp((float) summary.currentOxygenDroplets() / (float) summary.oxygenCapacityDroplets(), 0.0F, 1.0F)
                : 0.0F;
        String o2Value = summary.oxygenCapacityDroplets() > 0L
                ? String.format("%.1f / %.1f B", curO2B, maxO2B)
                : "Não tripulado";
        this.drawResourceGauge(
                graphics, sx + 5, sy + 96, RKT_SYS_W - 10,
                "Oxigênio (O2)",
                summary.oxygenCapacityDroplets() > 0L ? (Math.round(o2Ratio * 100.0F) + "%") : "N/A",
                o2Value,
                o2Ratio,
                0xFF2DD4BF,
                COLOR_TEXT_BADGE
        );

        // 4) Structural Integrity & Environmental Tolerances inset box
        int envX = sx + 5;
        int envY = sy + 130;
        int envW = RKT_SYS_W - 10;
        int envH = 72;
        graphics.fill(envX, envY, envX + envW, envY + envH, COLOR_BG_INSET);
        graphics.renderOutline(envX, envY, envW, envH, 0xFF1E2D40);

        int integrityPct = (int) Math.round(Mth.clamp(summary.minIntegrityFraction(), 0.0, 1.0) * 100.0);
        int integrityColor = integrityPct >= 80 ? COLOR_TEXT_OK : (integrityPct >= 50 ? COLOR_TEXT_AMBER : COLOR_TEXT_ERROR);
        graphics.drawString(this.font, "Integridade:", envX + 4, envY + 5, COLOR_TEXT_MUTED, false);
        String intStr = integrityPct + "%";
        graphics.drawString(this.font, intStr, envX + envW - 4 - this.font.width(intStr), envY + 5, integrityColor, false);

        int ibX = envX + 4;
        int ibY = envY + 15;
        int ibW = envW - 8;
        graphics.fill(ibX, ibY, ibX + ibW, ibY + 5, 0xFF070B12);
        graphics.renderOutline(ibX, ibY, ibW, 5, 0xFF1E2D40);
        int ibFill = Math.round((ibW - 2) * (float) Mth.clamp(summary.minIntegrityFraction(), 0.0, 1.0));
        if (ibFill > 0) {
            graphics.fill(ibX + 1, ibY + 1, ibX + 1 + ibFill, ibY + 4, integrityColor);
        }

        graphics.drawString(
                this.font,
                String.format("Térmica: %.0f K", summary.minThermalToleranceKelvin()),
                envX + 4, envY + 25, COLOR_TEXT_PRIMARY, false
        );
        graphics.drawString(
                this.font,
                String.format("Pressão: %.1f atm", summary.minPressureToleranceAtm()),
                envX + 4, envY + 36, COLOR_TEXT_PRIMARY, false
        );
        String genText = summary.energyGenerationPerTick() > 0L
                ? ("Ger. Elét: +" + summary.energyGenerationPerTick() + " GJ/t")
                : "Ger. Elét: 0 GJ/t";
        graphics.drawString(
                this.font,
                genText,
                envX + 4, envY + 47,
                summary.energyGenerationPerTick() > 0L ? COLOR_TEXT_OK : COLOR_TEXT_MUTED,
                false
        );
        graphics.drawString(
                this.font,
                "Módulos: " + parts.size() + " | Ø" + summary.maxDiameter() + "/" + summary.allowedDiameter(),
                envX + 4, envY + 58,
                summary.diameterWithinLimit() ? COLOR_TEXT_MUTED : COLOR_TEXT_ERROR,
                false
        );

        // --- Column 2: Interactive 3D Unified Rocket Viewport ---
        int vx = x + RKT_VIEW_X;
        int vy = y + RKT_VIEW_Y;
        graphics.fill(vx, vy, vx + RKT_VIEW_W, vy + RKT_VIEW_H, COLOR_BG_SECTION);
        graphics.renderOutline(vx, vy, RKT_VIEW_W, RKT_VIEW_H, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Veículo 3D", vx + 5, vy + 4, COLOR_TEXT_HEADER, false);
        String camHint = "360°";
        graphics.drawString(this.font, camHint, vx + RKT_VIEW_W - 5 - this.font.width(camHint), vy + 4, COLOR_TEXT_MUTED, false);

        int cx = x + RKT_CANVAS_X;
        int cy = y + RKT_CANVAS_Y;
        boolean canvasHovered = mouseX >= cx && mouseX < cx + RKT_CANVAS_W && mouseY >= cy && mouseY < cy + RKT_CANVAS_H;
        RocketPartGuiPreviewRenderer.renderViewportFrame(graphics, cx, cy, RKT_CANVAS_W, RKT_CANVAS_H, canvasHovered || this.vabDragged, true);

        int floorY = cy + RKT_CANVAS_H - 7;
        graphics.fill(cx + 8, floorY, cx + RKT_CANVAS_W - 8, floorY + 2, 0xFF3B4D63);

        this.lastCanvasBands.clear();
        this.renderMountedStack3D(graphics, cx, cy, RKT_CANVAS_W, RKT_CANVAS_H, be);
        if (be.isAssemblyInProgress()) {
            this.renderAssemblyProgressOverlay(graphics, cx, cy, RKT_CANVAS_W, RKT_CANVAS_H, be);
        }

        // Selected module caption along the bottom left of column 2 (left of > and R buttons)
        if (this.selectedMountedIndex >= 0 && this.selectedMountedIndex < parts.size()) {
            HangarTerminalBlockEntity.MountedRocketPart sel = parts.get(this.selectedMountedIndex);
            String selLabel = this.font.plainSubstrByWidth(sel.displayName(), RKT_CANVAS_W - 40);
            graphics.drawString(this.font, selLabel, cx + 2, cy + RKT_CANVAS_H + 8, COLOR_TEXT_PRIMARY, false);
        }

        // --- Column 3: Flight Telemetry & Unified Controls ---
        int rx = x + VAB_INFO_X;
        int ry = y + VAB_INFO_Y;
        graphics.fill(rx, ry, rx + VAB_INFO_W, ry + VAB_INFO_H, COLOR_BG_SECTION);
        graphics.renderOutline(rx, ry, VAB_INFO_W, VAB_INFO_H, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Voo & Controle", rx + 5, ry + 4, COLOR_TEXT_HEADER, false);

        int boxX = rx + 4;
        int boxY = ry + 16;
        int boxW = VAB_INFO_W - 8;
        int boxH = 92;
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, COLOR_BG_INSET);
        graphics.renderOutline(boxX, boxY, boxW, boxH, 0xFF1E2D40);

        int totalHeight = 0;
        for (HangarTerminalBlockEntity.MountedRocketPart p : parts) {
            totalHeight += Math.max(1, p.heightBlocks());
        }
        graphics.drawString(this.font, "Alt: " + totalHeight + "bl  Ø" + summary.maxDiameter(), boxX + 4, boxY + 4, COLOR_TEXT_PRIMARY, false);
        graphics.drawString(this.font, String.format("Seca: %.1ft", summary.dryMassKg() / 1000.0), boxX + 4, boxY + 14, COLOR_TEXT_PRIMARY, false);
        graphics.drawString(this.font, String.format("Atual: %.1ft", summary.currentWetMassKg() / 1000.0), boxX + 4, boxY + 24, COLOR_TEXT_PRIMARY, false);
        graphics.drawString(this.font, String.format("Emp: %.0fkN", summary.totalThrustKn()), boxX + 4, boxY + 34, COLOR_TEXT_PRIMARY, false);

        double liveTwr = summary.currentTwr();
        int twrColor = liveTwr >= 1.2 ? COLOR_TEXT_OK : (liveTwr >= 1.0 ? COLOR_TEXT_AMBER : COLOR_TEXT_ERROR);
        graphics.drawString(this.font, String.format("TWR: %.2f", liveTwr), boxX + 4, boxY + 44, twrColor, false);

        int dvColor = summary.currentDeltaV() > 0.0 ? COLOR_TEXT_BADGE : COLOR_TEXT_MUTED;
        graphics.drawString(this.font, String.format("dV: %.0fm/s", summary.currentDeltaV()), boxX + 4, boxY + 54, dvColor, false);
        graphics.drawString(this.font, String.format("Máx: %.0fm/s", summary.estimatedDeltaV()), boxX + 4, boxY + 64, COLOR_TEXT_MUTED, false);

        drawMiniBadge(graphics, boxX + 3, boxY + 77, "CMD", summary.hasCommand());
        drawMiniBadge(graphics, boxX + 25, boxY + 77, "TNK", summary.hasTank());
        drawMiniBadge(graphics, boxX + 47, boxY + 77, "ENG", summary.hasEngine());
        drawMiniBadge(graphics, boxX + 69, boxY + 77, "TWR", summary.twr() >= 1.0 && summary.diameterWithinLimit());

        // Stage & Radial Booster summary card at bottom of right column (replaces Desmontar Pad)
        int stX = rx + 4;
        int stY = ry + 183;
        int stW = VAB_INFO_W - 8;
        int stH = 21;
        graphics.fill(stX, stY, stX + stW, stY + stH, COLOR_BG_INSET);
        graphics.renderOutline(stX, stY, stW, stH, 0xFF1E2D40);
        RocketAssemblyGraph graph = be.getAssemblyGraph();
        int stageCount = Math.max(1, graph.activeStages().size());
        int radialCount = 0;
        for (RocketStageNode st : graph.activeStages()) {
            for (var g : st.radialGroups()) {
                if (!g.decoupled()) {
                    radialCount += g.symmetry().count();
                }
            }
        }
        graphics.drawCenteredString(this.font, "Estágios: " + stageCount, stX + stW / 2, stY + 2, COLOR_TEXT_HEADER);
        String radLine = radialCount > 0 ? ("Boosters: " + radialCount + "x") : "Sem boosters";
        graphics.drawCenteredString(this.font, radLine, stX + stW / 2, stY + 11, radialCount > 0 ? COLOR_TEXT_BADGE : COLOR_TEXT_MUTED);
    }

    private void renderSimulationTabMode(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, HangarTerminalBlockEntity be) {
        boolean hasDraft = !this.vabDraft.isEmpty();
        boolean hasMounted = !this.cachedMountedSummary.parts().isEmpty();
        boolean hasVehicle = hasDraft || hasMounted;
        boolean useDraft = hasDraft;

        // Top-right header status pill
        PlanetaryEnvironment selectedDest = this.getSelectedSimulationDestination();
        RocketFlightValidator.FlightValidationReport activeReport = hasVehicle
                ? this.evaluateCurrentVehicleReport(be, useDraft, selectedDest)
                : null;

        String pillText;
        int pillBg;
        int pillBorder;
        int pillColor;
        if (!hasVehicle || activeReport == null) {
            pillText = "SEM VEÍCULO";
            pillBg = 0xFF1E293B;
            pillBorder = 0xFF475569;
            pillColor = COLOR_TEXT_MUTED;
        } else if (activeReport.hazards().isEmpty()) {
            pillText = "VIÁVEL";
            pillBg = 0xFF143826;
            pillBorder = 0xFF34D399;
            pillColor = COLOR_TEXT_OK;
        } else if (!activeReport.hasCriticalHazards()) {
            pillText = "ALERTAS";
            pillBg = 0xFF2B2310;
            pillBorder = 0xFFFACC15;
            pillColor = COLOR_TEXT_AMBER;
        } else {
            pillText = "ALTO RISCO";
            pillBg = 0xFF3B1318;
            pillBorder = 0xFFF87171;
            pillColor = COLOR_TEXT_ERROR;
        }
        int pillW = 74;
        int pillX = x + PANEL_WIDTH_VAB - pillW - 6;
        int pillY = y + 5;
        graphics.fill(pillX, pillY, pillX + pillW, pillY + 11, pillBg);
        graphics.renderOutline(pillX, pillY, pillW, 11, pillBorder);
        graphics.drawCenteredString(this.font, pillText, pillX + pillW / 2, pillY + 2, pillColor);

        if (!hasVehicle || activeReport == null) {
            int cardX = x + 36;
            int cardY = y + 72;
            int cardW = PANEL_WIDTH_VAB - 72;
            int cardH = 114;
            graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, COLOR_BG_SECTION);
            graphics.renderOutline(cardX, cardY, cardW, cardH, COLOR_BORDER_SECTION);
            graphics.drawCenteredString(this.font, "Nenhum veículo disponível para simulação", x + PANEL_WIDTH_VAB / 2, cardY + 24, COLOR_TEXT_TITLE);
            graphics.drawCenteredString(this.font, "Monte um rascunho na aba Montagem (VAB)", x + PANEL_WIDTH_VAB / 2, cardY + 42, COLOR_TEXT_MUTED);
            graphics.drawCenteredString(this.font, "ou construa um foguete na plataforma.", x + PANEL_WIDTH_VAB / 2, cardY + 54, COLOR_TEXT_MUTED);
            return;
        }

        // --- Column 1: Solar System Destinations List ---
        int dx = x + SIM_DEST_X;
        int dy = y + SIM_DEST_Y;
        graphics.fill(dx, dy, dx + SIM_DEST_W, dy + SIM_DEST_H, COLOR_BG_SECTION);
        graphics.renderOutline(dx, dy, SIM_DEST_W, SIM_DEST_H, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Destinos", dx + 5, dy + 4, COLOR_TEXT_HEADER, false);
        String countStr = String.valueOf(PlanetaryEnvironment.SOLAR_SYSTEM_PRESETS.size());
        graphics.drawString(this.font, countStr, dx + SIM_DEST_W - 5 - this.font.width(countStr), dy + 4, COLOR_TEXT_MUTED, false);

        int listX = dx + 4;
        int listY = dy + 18;
        int listW = SIM_DEST_W - 8;
        int selectedIdx = Math.floorMod(this.simDestinationIndex, PlanetaryEnvironment.SOLAR_SYSTEM_PRESETS.size());
        for (int i = 0; i < PlanetaryEnvironment.SOLAR_SYSTEM_PRESETS.size(); i++) {
            PlanetaryEnvironment env = PlanetaryEnvironment.SOLAR_SYSTEM_PRESETS.get(i);
            RocketFlightValidator.FlightValidationReport rowReport = (i == selectedIdx)
                    ? activeReport
                    : this.evaluateCurrentVehicleReport(be, useDraft, env);

            int rowY = listY + i * SIM_DEST_ROW_H;
            int rowH = SIM_DEST_ROW_H - 1;
            boolean selected = i == selectedIdx;
            boolean hovered = mouseX >= listX && mouseX < listX + listW && mouseY >= rowY && mouseY < rowY + rowH;

            int rowBg = selected ? 0xFF17304A : (hovered ? 0xFF182538 : COLOR_BG_INSET);
            int rowBorder = selected ? 0xFF38BDF8 : (hovered ? 0xFF2E4968 : 0xFF1B283A);
            graphics.fill(listX, rowY, listX + listW, rowY + rowH, rowBg);
            graphics.renderOutline(listX, rowY, listW, rowH, rowBorder);

            int dotColor = rowReport.hazards().isEmpty()
                    ? COLOR_TEXT_OK
                    : (rowReport.hasCriticalHazards() ? COLOR_TEXT_ERROR : COLOR_TEXT_AMBER);
            graphics.fill(listX + 4, rowY + 6, listX + 8, rowY + 10, dotColor);

            String gravStr = String.format(java.util.Locale.ROOT, "%.2fG", env.gravityG());
            int gravW = this.font.width(gravStr);
            int maxNameW = listW - 16 - gravW;
            String planetName = this.font.plainSubstrByWidth(env.displayName(), Math.max(24, maxNameW));
            graphics.drawString(this.font, planetName, listX + 11, rowY + 4,
                    selected ? COLOR_TEXT_TITLE : COLOR_TEXT_PRIMARY, false);
            graphics.drawString(this.font, gravStr, listX + listW - 3 - gravW, rowY + 4,
                    selected ? COLOR_TEXT_BADGE : COLOR_TEXT_MUTED, false);
        }

        // --- Column 2: Physical Telemetry Cards ---
        int mx = x + SIM_METRIC_X;
        int my = y + SIM_METRIC_Y;
        graphics.fill(mx, my, mx + SIM_METRIC_W, my + SIM_METRIC_H, COLOR_BG_SECTION);
        graphics.renderOutline(mx, my, SIM_METRIC_W, SIM_METRIC_H, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Análise Física", mx + 5, my + 4, COLOR_TEXT_HEADER, false);
        String sourceTag = useDraft ? "RASCUNHO" : "NO PAD";
        graphics.drawString(this.font, sourceTag, mx + SIM_METRIC_W - 5 - this.font.width(sourceTag), my + 4,
                useDraft ? COLOR_TEXT_BADGE : COLOR_TEXT_OK, false);

        int cardX = mx + 4;
        int cardW = SIM_METRIC_W - 8;

        // Route & heliocentric distance strip
        int routeY = my + 16;
        graphics.fill(cardX, routeY, cardX + cardW, routeY + 14, COLOR_BG_INSET);
        graphics.renderOutline(cardX, routeY, cardW, 14, 0xFF23354D);
        String auStr = String.format(java.util.Locale.ROOT, "%.2f AU", selectedDest.orbitalDistanceAu());
        int auW = this.font.width(auStr);
        String routeText = this.font.plainSubstrByWidth(
                activeReport.origin().displayName() + " \u2192 " + selectedDest.displayName(),
                cardW - 10 - auW
        );
        graphics.drawString(this.font, routeText, cardX + 4, routeY + 3, COLOR_TEXT_TITLE, false);
        graphics.drawString(this.font, auStr, cardX + cardW - 4 - auW, routeY + 3, COLOR_TEXT_MUTED, false);

        int cy = my + 33;
        int cardStep = 34;

        // Card 1: Thrust-to-Weight (T/W)
        double origTwr = Double.isFinite(activeReport.originTwr()) ? activeReport.originTwr() : 99.9;
        double destTwr = Double.isFinite(activeReport.destinationTwr()) ? activeReport.destinationTwr() : 99.9;
        boolean twrOk = activeReport.canLiftoffOrigin() && activeReport.canLiftoffDestination();
        float twrRatio = (float) Mth.clamp(Math.min(origTwr, destTwr) / 2.5, 0.0, 1.0);
        int twrBarColor = twrOk ? (Math.min(origTwr, destTwr) >= 1.2 ? COLOR_TEXT_OK : COLOR_TEXT_AMBER) : COLOR_TEXT_ERROR;
        this.drawSimMetricCard(
                graphics, cardX, cy, cardW,
                "T/W Orig / Dest",
                String.format(java.util.Locale.ROOT, "%.2f / %.2f", origTwr, destTwr),
                twrOk ? COLOR_TEXT_OK : COLOR_TEXT_ERROR,
                twrRatio,
                twrBarColor,
                0.4F,
                String.format(java.util.Locale.ROOT, "Emp: %.0fkN | Grav: %.2fG", activeReport.totalThrustKn(), selectedDest.gravityG()),
                COLOR_TEXT_MUTED
        );

        // Card 2: Delta-V Budget
        cy += cardStep;
        float dvRatio = activeReport.requiredDeltaV() > 0.0
                ? (float) Mth.clamp(activeReport.maxDeltaV() / activeReport.requiredDeltaV(), 0.0, 1.0)
                : 1.0F;
        int dvColor = activeReport.hasEnoughDeltaV() ? COLOR_TEXT_OK : COLOR_TEXT_AMBER;
        this.drawSimMetricCard(
                graphics, cardX, cy, cardW,
                "\u0394v Máx / Req",
                String.format(java.util.Locale.ROOT, "%.0f/%.0f", activeReport.maxDeltaV(), activeReport.requiredDeltaV()),
                dvColor,
                dvRatio,
                activeReport.hasEnoughDeltaV() ? 0xFF2DD4BF : COLOR_TEXT_AMBER,
                -1.0F,
                String.format(java.util.Locale.ROOT, "Atual: %.0fm/s | Isp: %.0fs", activeReport.currentDeltaV(), activeReport.effectiveIspSeconds()),
                COLOR_TEXT_MUTED
        );

        // Card 3: Thermal, Pressure & Venus Acid
        cy += cardStep;
        boolean envSafe = activeReport.thermalSafe() && activeReport.pressureSafe() && !activeReport.acidHazard();
        double tempRatio = activeReport.destinationPeakTempKelvin() > 0.0
                ? activeReport.minThermalToleranceKelvin() / activeReport.destinationPeakTempKelvin()
                : 1.0;
        double pressRatio = activeReport.destinationPressureAtm() > 0.01
                ? activeReport.minPressureToleranceAtm() / activeReport.destinationPressureAtm()
                : 1.0;
        float envBarRatio = activeReport.acidHazard()
                ? 0.25F
                : (float) Mth.clamp(Math.min(tempRatio, pressRatio), 0.0, 1.0);
        String envDetail;
        if (selectedDest.corrosiveAcid()) {
            envDetail = String.format(
                    java.util.Locale.ROOT,
                    "%.0fK %.0fatm H2SO4:%s",
                    activeReport.minThermalToleranceKelvin(),
                    activeReport.minPressureToleranceAtm(),
                    activeReport.acidProtected() ? "OK" : "SEM"
            );
        } else {
            envDetail = String.format(
                    java.util.Locale.ROOT,
                    "%.0f/%.0fK | %.1f/%.1fatm",
                    activeReport.minThermalToleranceKelvin(),
                    activeReport.destinationPeakTempKelvin(),
                    activeReport.minPressureToleranceAtm(),
                    activeReport.destinationPressureAtm()
            );
        }
        this.drawSimMetricCard(
                graphics, cardX, cy, cardW,
                "Térmica / Pressão",
                envSafe ? "SEGURO" : (activeReport.acidHazard() ? "ÁCIDO" : "EXPOSTO"),
                envSafe ? COLOR_TEXT_OK : COLOR_TEXT_ERROR,
                envBarRatio,
                envSafe ? COLOR_TEXT_OK : COLOR_TEXT_ERROR,
                -1.0F,
                envDetail,
                envSafe ? COLOR_TEXT_MUTED : COLOR_TEXT_WARN
        );

        // Card 4: Power (1/r² Solar & DFD)
        cy += cardStep;
        long totalGen = activeReport.passivePowerGenPerTick() + activeReport.dfdBurnPowerGenPerTick();
        float powerRatio = activeReport.powerDrawPerTick() > 0L
                ? (float) Mth.clamp((double) totalGen / (double) activeReport.powerDrawPerTick(), 0.0, 1.0)
                : 1.0F;
        String powerDetail = activeReport.dfdBurnPowerGenPerTick() > 0L
                ? String.format(java.util.Locale.ROOT, "Sol %.3fx | DFD +%d GJ/t", activeReport.solarIrradianceFactor(), activeReport.dfdBurnPowerGenPerTick())
                : String.format(java.util.Locale.ROOT, "Sol %.3fx | Cons -%d GJ/t", activeReport.solarIrradianceFactor(), activeReport.powerDrawPerTick());
        this.drawSimMetricCard(
                graphics, cardX, cy, cardW,
                "Energia (1/r\u00b2)",
                String.format(java.util.Locale.ROOT, "%+d GJ/t", activeReport.netPowerPerTick()),
                activeReport.powerDeficit() ? COLOR_TEXT_AMBER : 0xFF7DD3FC,
                powerRatio,
                activeReport.powerDeficit() ? COLOR_TEXT_AMBER : 0xFF38BDF8,
                -1.0F,
                powerDetail,
                COLOR_TEXT_MUTED
        );

        // Card 5: Cryogenics & Propellant
        cy += cardStep;
        String cryoVal;
        int cryoColor;
        float cryoRatio;
        if (!activeReport.propellantCompatible()) {
            cryoVal = "INCOMP.";
            cryoColor = COLOR_TEXT_ERROR;
            cryoRatio = 0.15F;
        } else if (activeReport.superfluidCryo2kActive()) {
            cryoVal = "<2K 0.0x";
            cryoColor = COLOR_TEXT_BADGE;
            cryoRatio = 1.0F;
        } else if (activeReport.zeroBoilOffActive()) {
            cryoVal = "CRYO 0.0x";
            cryoColor = COLOR_TEXT_OK;
            cryoRatio = 1.0F;
        } else if (activeReport.boilOffDropletsPerMinute() > 0.0) {
            double bPerMin = activeReport.boilOffDropletsPerMinute() / RocketFlightValidator.DROPLETS_PER_BUCKET;
            cryoVal = String.format(java.util.Locale.ROOT, "-%.2fB/m", bPerMin);
            cryoColor = COLOR_TEXT_AMBER;
            cryoRatio = (float) Mth.clamp(1.0 - (bPerMin / 2.0), 0.15, 0.85);
        } else {
            cryoVal = "ESTÁVEL";
            cryoColor = COLOR_TEXT_OK;
            cryoRatio = 1.0F;
        }
        this.drawSimMetricCard(
                graphics, cardX, cy, cardW,
                "Criogenia / Prop.",
                cryoVal,
                cryoColor,
                cryoRatio,
                cryoColor,
                -1.0F,
                activeReport.propellantStatusMessage(),
                activeReport.propellantCompatible() ? COLOR_TEXT_MUTED : COLOR_TEXT_ERROR
        );

        // --- Column 3: Mission Verdict & Detailed Hazards ---
        int gx = x + SIM_DIAG_X;
        int gy = y + SIM_DIAG_Y;
        graphics.fill(gx, gy, gx + SIM_DIAG_W, gy + SIM_DIAG_H, COLOR_BG_SECTION);
        graphics.renderOutline(gx, gy, SIM_DIAG_W, SIM_DIAG_H, COLOR_BORDER_SECTION);
        graphics.drawString(this.font, "Diagnóstico", gx + 5, gy + 4, COLOR_TEXT_HEADER, false);

        int innerX = gx + 4;
        int innerW = SIM_DIAG_W - 8;

        // Verdict banner
        int vY = gy + 16;
        int vH = 24;
        String verdictTitle;
        String verdictSub;
        int vBg;
        int vBorder;
        int vColor;
        if (activeReport.hazards().isEmpty()) {
            verdictTitle = "MISSÃO VIÁVEL";
            verdictSub = "Parâmetros nominais";
            vBg = 0xFF123023;
            vBorder = 0xFF34D399;
            vColor = COLOR_TEXT_OK;
        } else if (!activeReport.hasCriticalHazards()) {
            verdictTitle = "COM ALERTAS";
            verdictSub = activeReport.hazards().size() + " aviso(s) ativo(s)";
            vBg = 0xFF2B2210;
            vBorder = 0xFFFACC15;
            vColor = COLOR_TEXT_AMBER;
        } else {
            verdictTitle = "ALTO RISCO";
            verdictSub = activeReport.hazards().size() + " risco(s) crítico(s)";
            vBg = 0xFF381318;
            vBorder = 0xFFF87171;
            vColor = COLOR_TEXT_ERROR;
        }
        graphics.fill(innerX, vY, innerX + innerW, vY + vH, vBg);
        graphics.renderOutline(innerX, vY, innerW, vH, vBorder);
        graphics.drawCenteredString(this.font, verdictTitle, innerX + innerW / 2, vY + 3, vColor);
        graphics.drawCenteredString(this.font, verdictSub, innerX + innerW / 2, vY + 13, COLOR_TEXT_MUTED);

        // Estimated entry integrity wear pill
        int wearY = gy + 43;
        int wearH = 14;
        int wearPct = (int) Math.round(Mth.clamp(activeReport.estimatedEntryIntegrityLoss(), 0.0, 1.0) * 100.0);
        int wearColor = wearPct == 0 ? COLOR_TEXT_OK : (wearPct <= 30 ? COLOR_TEXT_AMBER : COLOR_TEXT_ERROR);
        graphics.fill(innerX, wearY, innerX + innerW, wearY + wearH, COLOR_BG_INSET);
        graphics.renderOutline(innerX, wearY, innerW, wearH, 0xFF1E2D40);
        graphics.drawString(this.font, "Dano entrada:", innerX + 4, wearY + 3, COLOR_TEXT_MUTED, false);
        String wearVal = "~" + wearPct + "%";
        graphics.drawString(this.font, wearVal, innerX + innerW - 4 - this.font.width(wearVal), wearY + 3, wearColor, false);

        // Hazards list container
        int hzY = gy + 60;
        int hzH = 128;
        graphics.fill(innerX, hzY, innerX + innerW, hzY + hzH, COLOR_BG_INSET);
        graphics.renderOutline(innerX, hzY, innerW, hzH, 0xFF1E2D40);

        if (activeReport.hazards().isEmpty()) {
            graphics.drawCenteredString(this.font, "Todos os parâmetros", innerX + innerW / 2, hzY + 42, COLOR_TEXT_OK);
            graphics.drawCenteredString(this.font, "físicos dentro dos", innerX + innerW / 2, hzY + 53, COLOR_TEXT_PRIMARY);
            graphics.drawCenteredString(this.font, "limites de segurança.", innerX + innerW / 2, hzY + 64, COLOR_TEXT_PRIMARY);
        } else {
            int curY = hzY + 4;
            int maxY = hzY + hzH - 12;
            int shown = 0;
            for (RocketFlightValidator.FlightHazard hazard : activeReport.hazards()) {
                int sevColor = switch (hazard.severity()) {
                    case CRITICAL -> COLOR_TEXT_ERROR;
                    case WARNING -> COLOR_TEXT_AMBER;
                    case INFO -> COLOR_TEXT_MUTED;
                };
                List<net.minecraft.util.FormattedCharSequence> wrapped = this.font.split(
                        Component.literal(hazard.summary()),
                        innerW - 10
                );
                int blockH = Math.max(1, wrapped.size()) * 9;
                if (curY + blockH > maxY && shown > 0) {
                    int remaining = activeReport.hazards().size() - shown;
                    graphics.drawString(this.font, "+" + remaining + " alerta(s)...", innerX + 5, hzY + hzH - 10, COLOR_TEXT_MUTED, false);
                    break;
                }
                int barEnd = Math.min(hzY + hzH - 3, curY + blockH - 1);
                graphics.fill(innerX + 3, curY, innerX + 5, barEnd, sevColor);
                for (net.minecraft.util.FormattedCharSequence line : wrapped) {
                    if (curY + 8 > hzY + hzH - 3) {
                        break;
                    }
                    graphics.drawString(this.font, line, innerX + 7, curY, sevColor, false);
                    curY += 9;
                }
                curY += 3;
                shown++;
            }
        }

        // Bottom emergent physics footer pill
        int footY = gy + 191;
        graphics.fill(innerX, footY, innerX + innerW, footY + 13, 0xFF102233);
        graphics.renderOutline(innerX, footY, innerW, 13, 0xFF23425E);
        graphics.drawCenteredString(this.font, "Física real \u2022 Sem Tier", innerX + innerW / 2, footY + 3, COLOR_TEXT_BADGE);
    }

    private void drawSimMetricCard(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            String label,
            String value,
            int valueColor,
            float barRatio,
            int barColor,
            float markerRatio,
            String detail,
            int detailColor
    ) {
        int h = 32;
        graphics.fill(x, y, x + w, y + h, COLOR_BG_INSET);
        graphics.renderOutline(x, y, w, h, 0xFF1E2D40);

        int valW = this.font.width(value);
        String trimmedLabel = this.font.plainSubstrByWidth(label, Math.max(24, w - 10 - valW));
        graphics.drawString(this.font, trimmedLabel, x + 4, y + 3, COLOR_TEXT_PRIMARY, false);
        graphics.drawString(this.font, value, x + w - 4 - valW, y + 3, valueColor, false);

        int barX = x + 4;
        int barY = y + 13;
        int barW = w - 8;
        int barH = 5;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF070B12);
        graphics.renderOutline(barX, barY, barW, barH, 0xFF1E2D40);
        int fillW = Math.round((barW - 2) * Mth.clamp(barRatio, 0.0F, 1.0F));
        if (fillW > 0) {
            graphics.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, barColor);
        }
        if (markerRatio > 0.0F && markerRatio < 1.0F) {
            int markerX = barX + 1 + Math.round((barW - 2) * markerRatio);
            graphics.fill(markerX, barY, markerX + 1, barY + barH, 0xFFE2E8F0);
        }

        String trimmedDetail = this.font.plainSubstrByWidth(detail, w - 8);
        graphics.drawString(this.font, trimmedDetail, x + 4, y + 21, detailColor, false);
    }

    private PlanetaryEnvironment getSelectedSimulationDestination() {
        int idx = Math.floorMod(this.simDestinationIndex, PlanetaryEnvironment.SOLAR_SYSTEM_PRESETS.size());
        return PlanetaryEnvironment.SOLAR_SYSTEM_PRESETS.get(idx);
    }

    private RocketFlightValidator.FlightValidationReport evaluateCurrentVehicleReport(
            HangarTerminalBlockEntity be,
            boolean useDraft,
            PlanetaryEnvironment destination
    ) {
        PlanetaryEnvironment origin = PlanetaryEnvironment.fromLevel(be.getLevel());
        if (useDraft && !this.vabDraft.isEmpty()) {
            RocketAssemblyGraphBuilder.BuildResult built = this.buildPreviewGraph(be);
            return RocketFlightValidator.evaluateGraph(
                    built.graph(),
                    origin,
                    destination,
                    built.padMaxMassKg(),
                    0L
            ).summary();
        }
        return be.evaluateGraphFlight(destination).summary();
    }

    private void drawResourceGauge(
            GuiGraphics graphics,
            int x,
            int y,
            int w,
            String label,
            String pctText,
            String valueText,
            float ratio,
            int fillColor,
            int pctColor
    ) {
        graphics.drawString(this.font, label, x, y, COLOR_TEXT_PRIMARY, false);
        graphics.drawString(this.font, pctText, x + w - this.font.width(pctText), y, pctColor, false);

        int barY = y + 10;
        int barH = 7;
        graphics.fill(x, barY, x + w, barY + barH, COLOR_BG_INSET);
        graphics.renderOutline(x, barY, w, barH, COLOR_BORDER_SECTION);
        int fillW = Math.round((w - 2) * Mth.clamp(ratio, 0.0F, 1.0F));
        if (fillW > 0) {
            graphics.fill(x + 1, barY + 1, x + 1 + fillW, barY + barH - 1, fillColor);
        }
        graphics.drawString(this.font, valueText, x, barY + 9, COLOR_TEXT_MUTED, false);
    }

    private static String formatCompactEnergy(long energyGj) {
        if (energyGj >= 1_000_000L) {
            return String.format("%.1fM", energyGj / 1_000_000.0);
        }
        if (energyGj >= 1_000L) {
            return String.format("%.1fk", energyGj / 1_000.0);
        }
        return String.valueOf(energyGj);
    }

    private void renderSinglePartVabPreview(
            GuiGraphics graphics,
            int cx,
            int cy,
            int centerX,
            InventoryPartEntry entry,
            boolean ghostSnap
    ) {
        float anim = this.vabAnimEnabled ? this.vabAnimTime : 0.0F;
        float r = ghostSnap ? 0.55F : 1.0F;
        float g = ghostSnap ? 1.0F : 1.0F;
        float b = ghostSnap ? 0.72F : 1.0F;
        float a = ghostSnap ? 0.68F : 1.0F;

        RocketPartGuiPreviewRenderer.renderSinglePart(
                graphics,
                entry.config,
                cx + 2,
                cy + 14,
                VAB_CANVAS_W - 4,
                VAB_CANVAS_H - 26,
                this.vabYaw,
                this.vabPitch,
                this.vabZoom,
                anim,
                VAB_Z_OFFSET,
                VAB_MAX_HALF_DEPTH,
                r,
                g,
                b,
                a
        );

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, VAB_HUD_Z);
        String topLabel = ghostSnap ? "+ Clique para Encaixar" : "Preview 3D da Peça";
        graphics.drawCenteredString(this.font, topLabel, centerX, cy + 4, ghostSnap ? COLOR_TEXT_OK : COLOR_TEXT_BADGE);

        String nameLine = this.font.plainSubstrByWidth(entry.displayName, VAB_CANVAS_W - 10);
        graphics.drawCenteredString(this.font, nameLine, centerX, cy + VAB_CANVAS_H - 22, COLOR_TEXT_TITLE);
        String specLine = entry.config.diameter() + "x" + entry.config.diameter()
                + " | " + entry.config.heightBlocks() + "b | " + entry.config.livery().displayName();
        graphics.drawCenteredString(this.font, this.font.plainSubstrByWidth(specLine, VAB_CANVAS_W - 10),
                centerX, cy + VAB_CANVAS_H - 12, COLOR_TEXT_MUTED);
        pose.popPose();
    }

    private void renderPartPreviewPip(GuiGraphics graphics, int cx, int cy, InventoryPartEntry entry) {
        int pipW = 54;
        int pipH = 58;
        int pipX = cx + VAB_CANVAS_W - pipW - 3;
        int pipY = cy + 14;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, VAB_PIP_BG_Z);
        graphics.fill(pipX, pipY, pipX + pipW, pipY + pipH, 0xEA09111D);
        graphics.renderOutline(pipX, pipY, pipW, pipH, 0xFF38BDF8);
        graphics.fill(pipX + 1, pipY + 1, pipX + pipW - 1, pipY + 10, 0xFF15253A);
        pose.popPose();

        float anim = this.vabAnimEnabled ? this.vabAnimTime : 0.0F;
        RocketPartGuiPreviewRenderer.renderSinglePart(
                graphics,
                entry.config,
                pipX + 2,
                pipY + 11,
                pipW - 4,
                pipH - 21,
                this.vabYaw,
                this.vabPitch,
                1.0F,
                anim,
                VAB_PIP_MODEL_Z,
                VAB_PIP_MAX_HALF_DEPTH,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        pose.pushPose();
        pose.translate(0.0F, 0.0F, VAB_PIP_TEXT_Z);
        graphics.drawCenteredString(this.font, "PREVIEW", pipX + pipW / 2, pipY + 2, COLOR_TEXT_BADGE);
        String dim = entry.config.diameter() + "x" + entry.config.diameter() + " " + entry.config.heightBlocks() + "b";
        graphics.drawCenteredString(this.font, dim, pipX + pipW / 2, pipY + pipH - 9, COLOR_TEXT_PRIMARY);
        pose.popPose();
    }

    private void renderVabStack3D(
            GuiGraphics graphics,
            int cx,
            int cy,
            int centerX,
            int mouseX,
            int mouseY,
            boolean canvasHovered,
            @Nullable InventoryPartEntry selectedInvEntry,
            HangarTerminalBlockEntity be
    ) {
        boolean showGhost = selectedInvEntry != null && canvasHovered;
        int insertIdx = showGhost ? this.computeHoverInsertIndex(mouseY) : -1;
        float anim = this.vabAnimEnabled ? this.vabAnimTime : 0.0F;

        List<StackPart> stackParts = new ArrayList<>(this.vabDraft.size() + (showGhost ? 1 : 0));
        float curY = 0.0F;
        for (int i = 0; i <= this.vabDraft.size(); i++) {
            if (showGhost && i == insertIdx) {
                RocketPartConfig ghostCfg = selectedInvEntry.config;
                stackParts.add(new StackPart(-1, ghostCfg, curY, anim, false, true));
                curY += Math.max(1, ghostCfg.heightBlocks());
            }
            if (i < this.vabDraft.size()) {
                VabEntry entry = this.vabDraft.get(i);
                stackParts.add(new StackPart(i, entry.config, curY, anim, i == this.selectedVabIndex, false));
                curY += Math.max(1, entry.config.heightBlocks());
            }
        }

        // When editing a mounted rocket, also render surviving radial boosters around the editable core
        if (this.vabEditingMounted && !this.cachedMountedSummary.parts().isEmpty()) {
            net.minecraft.core.BlockPos padCenter = be.getPadCenterPos();
            net.minecraft.core.Direction facing = be.getFacing();
            RocketPartCatalog catalog = RocketPartCatalog.getInstance();
            int minRelY = Integer.MAX_VALUE;
            for (HangarTerminalBlockEntity.MountedRocketPart part : this.cachedMountedSummary.parts()) {
                minRelY = Math.min(minRelY, part.relativeY());
            }
            if (minRelY == Integer.MAX_VALUE) {
                minRelY = 1;
            }
            for (HangarTerminalBlockEntity.MountedRocketPart part : this.cachedMountedSummary.parts()) {
                net.minecraft.core.BlockPos rel = part.pos().subtract(padCenter);
                boolean radial = part.config() != null && part.config().radial();
                if (rel.getX() != 0 || rel.getZ() != 0 || radial) {
                    RocketPartConfig cfg = part.config() != null ? part.config() : catalog.defaultConfigFor(part.category());
                    float[] visXz = resolveLocalVisualXz(be, padCenter, facing, part.pos());
                    float baseY = Math.max(0, part.relativeY() - minRelY);
                    stackParts.add(new StackPart(-1, cfg, visXz[0], baseY, visXz[1], anim, false, false, false));
                }
            }
        }

        RocketPartGuiPreviewRenderer.renderRocketStack(
                graphics,
                stackParts,
                cx + 2,
                cy + 14,
                VAB_CANVAS_W - 4,
                VAB_CANVAS_H - 22,
                this.vabYaw,
                this.vabPitch,
                this.vabZoom,
                VAB_Z_OFFSET,
                VAB_MAX_HALF_DEPTH,
                this.lastCanvasBands
        );

        if (!showGhost) {
            this.lastBaseBands.clear();
            this.lastBaseBands.addAll(this.lastCanvasBands);
        }

        for (ProjectedBand band : this.lastCanvasBands) {
            if (band.ghost()) {
                RocketPartGuiPreviewRenderer.renderBandBrackets(graphics, band, 0xFF4ADE80, VAB_HUD_Z);
            } else if (band.selected()) {
                RocketPartGuiPreviewRenderer.renderBandBrackets(graphics, band, 0xFF38BDF8, VAB_HUD_Z);
            }
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, VAB_HUD_Z);
        if (showGhost) {
            String snapLabel = "+ Encaixar (#" + (insertIdx + 1) + ")";
            graphics.drawCenteredString(this.font, snapLabel, centerX, cy + 4, COLOR_TEXT_OK);
        } else if (this.selectedVabIndex >= 0 && this.selectedVabIndex < this.vabDraft.size()) {
            VabEntry sel = this.vabDraft.get(this.selectedVabIndex);
            String label = "#" + (this.selectedVabIndex + 1) + " " + sel.displayName;
            graphics.drawCenteredString(this.font, this.font.plainSubstrByWidth(label, VAB_CANVAS_W - 10),
                    centerX, cy + 4, COLOR_TEXT_BADGE);
        } else {
            graphics.drawCenteredString(this.font, "Foguete (" + this.vabDraft.size() + " pçs)", centerX, cy + 4, COLOR_TEXT_HEADER);
        }
        pose.popPose();
    }

    private void renderBoosterPreview3D(
            GuiGraphics graphics,
            int cx,
            int cy,
            int cw,
            int ch,
            HangarTerminalBlockEntity be,
            @Nullable InventoryPartEntry selectedInvEntry,
            boolean canvasHovered
    ) {
        List<HangarTerminalBlockEntity.MountedRocketPart> mounted = this.cachedMountedSummary.parts();
        if (mounted.isEmpty()) {
            return;
        }
        int centerX = cx + cw / 2;
        int minRelY = Integer.MAX_VALUE;
        int coreBaseDiam = 1;
        int lowestCoreY = Integer.MAX_VALUE;
        net.minecraft.core.BlockPos padCenter = be.getPadCenterPos();
        net.minecraft.core.Direction facing = be.getFacing();

        for (HangarTerminalBlockEntity.MountedRocketPart part : mounted) {
            minRelY = Math.min(minRelY, part.relativeY());
            net.minecraft.core.BlockPos local = PlatformBlueprint.toLocalPos(padCenter, facing, part.pos());
            if (local.getX() == 0 && local.getZ() == 0 && part.relativeY() < lowestCoreY) {
                lowestCoreY = part.relativeY();
                coreBaseDiam = Math.max(1, part.diameter());
            }
        }
        if (minRelY == Integer.MAX_VALUE) {
            minRelY = 0;
        }

        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        List<StackPart> stackParts = new ArrayList<>();
        // 1. Add existing mounted parts at their true pad-local (lx, baseY, lz) + flush visual offset coordinates
        for (int i = 0; i < mounted.size(); i++) {
            HangarTerminalBlockEntity.MountedRocketPart part = mounted.get(i);
            RocketPartConfig cfg = part.config() != null ? part.config() : catalog.defaultConfigFor(part.category());
            float anim = (this.vabAnimEnabled || part.animationActive()) ? this.vabAnimTime : 0.0F;
            float[] visXz = resolveLocalVisualXz(be, padCenter, facing, part.pos());
            float baseY = Math.max(0, part.relativeY() - minRelY);
            stackParts.add(new StackPart(-1, cfg, visXz[0], baseY, visXz[1], anim, false, false, false));
        }

        // 2. Add mirrored radial booster preview ghosts from vabDraft (plus optional hovered part)
        List<RocketPartConfig> boosterConfigs = new ArrayList<>();
        for (VabEntry entry : this.vabDraft) {
            boosterConfigs.add(entry.config);
        }
        if (boosterConfigs.isEmpty() && selectedInvEntry != null && canvasHovered) {
            boosterConfigs.add(selectedInvEntry.config);
        }

        RadialSymmetry sym = resolveBoosterSymmetry(be.getSelectedSymmetry());
        boolean boosterFits = true;
        if (!boosterConfigs.isEmpty()) {
            int boosterMaxDiam = 1;
            int boosterTotalHeight = 0;
            for (RocketPartConfig cfg : boosterConfigs) {
                boosterMaxDiam = Math.max(boosterMaxDiam, Math.max(cfg.diameter(), cfg.topDiameter()));
                boosterTotalHeight += Math.max(1, cfg.heightBlocks());
            }
            int boosterTopRelY = (lowestCoreY == Integer.MAX_VALUE ? 1 : lowestCoreY) + Math.max(1, boosterTotalHeight);
            for (HangarTerminalBlockEntity.MountedRocketPart part : mounted) {
                net.minecraft.core.BlockPos local = PlatformBlueprint.toLocalPos(padCenter, facing, part.pos());
                if (local.getX() == 0 && local.getZ() == 0) {
                    int pMin = part.relativeY();
                    int pMax = pMin + Math.max(1, part.heightBlocks());
                    if (pMin < boosterTopRelY && pMax > (lowestCoreY == Integer.MAX_VALUE ? 1 : lowestCoreY)) {
                        coreBaseDiam = Math.max(coreBaseDiam, Math.max(1, part.diameter()));
                    }
                }
            }
            List<net.minecraft.core.BlockPos> origins = RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(
                    net.minecraft.core.BlockPos.ZERO, coreBaseDiam, boosterMaxDiam, sym
            );
            double allowedRad = RocketAssemblyGraphBuilder.computeAllowedClearanceRadius(be.getPlatformSize(), be.getActiveBlueprint());
            for (net.minecraft.core.BlockPos o : origins) {
                if (!RocketAssemblyGraphBuilder.fitsWithinPadClearance(o.getX(), o.getZ(), boosterMaxDiam, allowedRad)) {
                    boosterFits = false;
                    break;
                }
            }
            float anim = this.vabAnimEnabled ? this.vabAnimTime : 0.0F;
            for (net.minecraft.core.BlockPos o : origins) {
                double[] flush = RocketAssemblyGraphBuilder.computeFlushRadialVisualPosition(
                        coreBaseDiam, boosterMaxDiam, o.getX(), o.getZ(), sym.count()
                );
                float curY = 0.0F;
                for (int j = 0; j < boosterConfigs.size(); j++) {
                    RocketPartConfig cfg = boosterConfigs.get(j);
                    stackParts.add(new StackPart(
                            j, cfg, (float) flush[0], curY, (float) flush[1], anim,
                            j == this.selectedVabIndex, true, !boosterFits
                    ));
                    curY += Math.max(1, cfg.heightBlocks());
                }
            }
        }

        RocketPartGuiPreviewRenderer.renderRocketStack(
                graphics, stackParts, cx + 2, cy + 14, cw - 4, ch - 22,
                this.vabYaw, this.vabPitch, this.vabZoom, VAB_Z_OFFSET, VAB_MAX_HALF_DEPTH, this.lastCanvasBands
        );

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, VAB_HUD_Z);
        String topLabel;
        int labelColor;
        if (boosterConfigs.isEmpty()) {
            topLabel = "Modo Booster (" + sym.count() + "x) — Clique à esq.";
            labelColor = COLOR_TEXT_BADGE;
        } else if (!boosterFits) {
            topLabel = "ERRO: Excede folga (" + sym.count() + "x)";
            labelColor = COLOR_TEXT_ERROR;
        } else {
            topLabel = "Preview Boosters " + sym.count() + "x (" + this.vabDraft.size() + " pç/col)";
            labelColor = COLOR_TEXT_OK;
        }
        graphics.drawCenteredString(this.font, this.font.plainSubstrByWidth(topLabel, cw - 10), centerX, cy + 4, labelColor);
        pose.popPose();
    }

    private void renderMountedStack3D(GuiGraphics graphics, int cx, int cy, int cw, int ch, HangarTerminalBlockEntity be) {
        List<HangarTerminalBlockEntity.MountedRocketPart> parts = this.cachedMountedSummary.parts();
        if (parts.isEmpty()) {
            return;
        }
        int centerX = cx + cw / 2;

        int minRelY = Integer.MAX_VALUE;
        for (HangarTerminalBlockEntity.MountedRocketPart part : parts) {
            minRelY = Math.min(minRelY, part.relativeY());
        }
        if (minRelY == Integer.MAX_VALUE) {
            minRelY = 0;
        }

        net.minecraft.core.BlockPos padCenter = be.getPadCenterPos();
        net.minecraft.core.Direction facing = be.getFacing();
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        List<StackPart> stackParts = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            HangarTerminalBlockEntity.MountedRocketPart part = parts.get(i);
            RocketPartConfig cfg = part.config() != null ? part.config() : catalog.defaultConfigFor(part.category());
            float anim = (this.vabAnimEnabled || part.animationActive()) ? this.vabAnimTime : 0.0F;
            float[] visXz = resolveLocalVisualXz(be, padCenter, facing, part.pos());
            float baseY = Math.max(0, part.relativeY() - minRelY);
            stackParts.add(new StackPart(i, cfg, visXz[0], baseY, visXz[1], anim, i == this.selectedMountedIndex, false, false));
        }

        RocketPartGuiPreviewRenderer.renderRocketStack(
                graphics,
                stackParts,
                cx + 2,
                cy + 14,
                cw - 4,
                ch - 22,
                this.vabYaw,
                this.vabPitch,
                this.vabZoom,
                VAB_Z_OFFSET,
                VAB_MAX_HALF_DEPTH,
                this.lastCanvasBands
        );

        for (ProjectedBand band : this.lastCanvasBands) {
            if (band.selected()) {
                RocketPartGuiPreviewRenderer.renderBandBrackets(graphics, band, 0xFF4ADE80, VAB_HUD_Z);
            }
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, VAB_HUD_Z);
        String topText = "No Pad (Clique p/ Sel)";
        if (this.selectedMountedIndex >= 0 && this.selectedMountedIndex < parts.size()) {
            topText = (this.cachedMountedSummary.unifiedAssembly() ? "Mód: " : "Pad: ")
                    + parts.get(this.selectedMountedIndex).displayName();
        }
        graphics.drawCenteredString(this.font, this.font.plainSubstrByWidth(topText, cw - 10),
                centerX, cy + 4, COLOR_TEXT_BADGE);
        pose.popPose();
    }

    private static float[] resolveLocalVisualXz(
            HangarTerminalBlockEntity be,
            net.minecraft.core.BlockPos padCenter,
            net.minecraft.core.Direction facing,
            net.minecraft.core.BlockPos partPos
    ) {
        net.minecraft.core.BlockPos local = PlatformBlueprint.toLocalPos(padCenter, facing, partPos);
        float visX = local.getX();
        float visZ = local.getZ();
        if (be.getLevel() != null && be.getLevel().getBlockEntity(partPos) instanceof dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity partBe) {
            float ox = partBe.getRadialVisualOffsetX();
            float oz = partBe.getRadialVisualOffsetZ();
            switch (facing) {
                case EAST -> {
                    visX += oz;
                    visZ -= ox;
                }
                case SOUTH -> {
                    visX -= ox;
                    visZ -= oz;
                }
                case WEST -> {
                    visX -= oz;
                    visZ += ox;
                }
                default -> {
                    visX += ox;
                    visZ += oz;
                }
            }
        }
        return new float[]{visX, visZ};
    }

    private RocketAssemblyGraphBuilder.BuildResult buildPreviewGraph(HangarTerminalBlockEntity be) {
        if (this.vabBoosterMode && !this.cachedMountedSummary.parts().isEmpty() && !this.vabDraft.isEmpty()) {
            List<RocketAssemblyGraphBuilder.PartInput> combined = new ArrayList<>();
            net.minecraft.core.BlockPos padCenter = be.getPadCenterPos();
            int lowestCoreY = Integer.MAX_VALUE;
            for (HangarTerminalBlockEntity.MountedRocketPart part : this.cachedMountedSummary.parts()) {
                if (part.config() == null) {
                    continue;
                }
                net.minecraft.core.BlockPos rel = part.pos().subtract(padCenter);
                combined.add(RocketAssemblyGraphBuilder.PartInput.fromConfig(rel, part.config()));
                if (rel.getX() == 0 && rel.getZ() == 0 && !part.config().radial() && rel.getY() < lowestCoreY) {
                    lowestCoreY = rel.getY();
                }
            }
            if (lowestCoreY == Integer.MAX_VALUE) {
                lowestCoreY = 1;
            }
            int boosterMaxDiam = 1;
            int boosterTotalHeight = 0;
            for (VabEntry e : this.vabDraft) {
                boosterMaxDiam = Math.max(boosterMaxDiam, Math.max(e.config.diameter(), e.config.topDiameter()));
                boosterTotalHeight += Math.max(1, e.config.heightBlocks());
            }
            int coreBaseDiam = 1;
            int boosterTopRelY = lowestCoreY + Math.max(1, boosterTotalHeight);
            for (HangarTerminalBlockEntity.MountedRocketPart part : this.cachedMountedSummary.parts()) {
                if (part.config() == null) {
                    continue;
                }
                net.minecraft.core.BlockPos rel = part.pos().subtract(padCenter);
                if (rel.getX() == 0 && rel.getZ() == 0 && !part.config().radial()) {
                    int pMin = rel.getY();
                    int pMax = pMin + Math.max(1, part.heightBlocks());
                    if (pMin < boosterTopRelY && pMax > lowestCoreY) {
                        coreBaseDiam = Math.max(coreBaseDiam, Math.max(part.config().diameter(), part.config().topDiameter()));
                    }
                }
            }
            RadialSymmetry sym = resolveBoosterSymmetry(be.getSelectedSymmetry());
            List<net.minecraft.core.BlockPos> origins = RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(
                    new net.minecraft.core.BlockPos(0, lowestCoreY, 0), coreBaseDiam, boosterMaxDiam, sym
            );
            for (net.minecraft.core.BlockPos base : origins) {
                int curY = base.getY();
                for (VabEntry e : this.vabDraft) {
                    RocketAssemblyGraphBuilder.PartInput pi = RocketAssemblyGraphBuilder.PartInput.fromStack(
                            new net.minecraft.core.BlockPos(base.getX(), curY, base.getZ()), e.icon
                    );
                    if (pi != null) {
                        combined.add(pi);
                    }
                    curY += Math.max(1, e.config.heightBlocks());
                }
            }
            return RocketAssemblyGraphBuilder.build(
                    combined, be.getPlatformSize(), be.getActiveBlueprint(),
                    Map.of(), Map.of(), be.getSelectedCrossfeed()
            );
        }
        if (this.vabEditingMounted && !this.vabBoosterMode && this.hasMountedRadialBoosters(be)) {
            List<RocketAssemblyGraphBuilder.PartInput> combined = new ArrayList<>();
            int curY = 1;
            for (VabEntry e : this.vabDraft) {
                RocketAssemblyGraphBuilder.PartInput pi = RocketAssemblyGraphBuilder.PartInput.fromStack(
                        new net.minecraft.core.BlockPos(0, curY, 0), e.icon
                );
                if (pi != null) {
                    combined.add(pi);
                }
                curY += Math.max(1, e.config.heightBlocks());
            }
            net.minecraft.core.BlockPos padCenter = be.getPadCenterPos();
            for (HangarTerminalBlockEntity.MountedRocketPart part : this.cachedMountedSummary.parts()) {
                if (part.config() == null) {
                    continue;
                }
                net.minecraft.core.BlockPos rel = part.pos().subtract(padCenter);
                if (rel.getX() != 0 || rel.getZ() != 0 || part.config().radial()) {
                    combined.add(RocketAssemblyGraphBuilder.PartInput.fromConfig(rel, part.config()));
                }
            }
            return RocketAssemblyGraphBuilder.build(
                    combined, be.getPlatformSize(), be.getActiveBlueprint(),
                    Map.of(), Map.of(), be.getSelectedCrossfeed()
            );
        }
        return RocketAssemblyGraphBuilder.buildFromStacks(
                this.collectDraftStacks(),
                be.getPlatformSize(),
                be.getActiveBlueprint()
        );
    }

    private VabTelemetry computeDraftTelemetry(HangarTerminalBlockEntity be) {
        PlanetaryEnvironment localEnv = PlanetaryEnvironment.fromLevel(be.getLevel());
        RocketAssemblyGraphBuilder.BuildResult build = this.buildPreviewGraph(be);
        RocketAssemblyGraph graph = build.graph();
        RocketFlightValidator.FlightValidationReport report = RocketFlightValidator.evaluateGraph(
                graph,
                localEnv,
                localEnv,
                build.padMaxMassKg(),
                0L
        ).summary();

        int maxDiam = 0;
        boolean hasCommand = false;
        boolean hasTank = false;
        boolean hasEngine = false;
        var allParts = graph.allActiveParts();
        for (var p : allParts) {
            RocketPartConfig cfg = p.config();
            maxDiam = Math.max(maxDiam, Math.max(cfg.diameter(), cfg.topDiameter()));
            if (cfg.category() == RocketPartCategory.COMMAND) hasCommand = true;
            else if (cfg.category() == RocketPartCategory.TANK) hasTank = true;
            else if (cfg.category() == RocketPartCategory.ENGINE) hasEngine = true;
        }

        int allowedDiam = be.getPlatformSize().diameter();
        boolean diamOk = !build.hasIssue(RocketAssemblyGraphBuilder.IssueCode.CLEARANCE_OBSTRUCTED)
                && !build.hasIssue(RocketAssemblyGraphBuilder.IssueCode.DIAMETER_MISMATCH)
                && (maxDiam == 0 || maxDiam <= allowedDiam);
        double twr = Double.isFinite(report.originTwr()) ? report.originTwr() : 99.99;
        boolean ready = !allParts.isEmpty() && hasCommand && hasTank && hasEngine
                && twr >= 1.0 && diamOk && !build.hasIssue(RocketAssemblyGraphBuilder.IssueCode.PAD_OVERWEIGHT);
        return new VabTelemetry(allParts.size(), graph.totalHeightBlocks(), maxDiam, allowedDiam, diamOk,
                report.dryMassKg(), report.wetMassKg(), report.totalThrustKn(), twr, report.maxDeltaV(),
                hasCommand, hasTank, hasEngine, ready);
    }

    private VabTelemetry fromMountedSummary(HangarTerminalBlockEntity.RocketPadSummary s) {
        int totalHeight = 0;
        for (HangarTerminalBlockEntity.MountedRocketPart p : s.parts()) {
            totalHeight += Math.max(1, p.heightBlocks());
        }
        return new VabTelemetry(s.parts().size(), totalHeight, s.maxDiameter(), s.allowedDiameter(),
                s.diameterWithinLimit(), s.dryMassKg(), s.wetMassKg(), s.totalThrustKn(), s.twr(),
                s.estimatedDeltaV(), s.hasCommand(), s.hasTank(), s.hasEngine(), s.readyForLaunch());
    }

    private void drawMiniBadge(GuiGraphics graphics, int x, int y, String label, boolean ok) {
        int bg = ok ? 0xFF143826 : 0xFF1A2230;
        int border = ok ? 0xFF34D399 : 0xFF334155;
        int text = ok ? COLOR_TEXT_OK : COLOR_TEXT_MUTED;
        graphics.fill(x, y, x + 21, y + 11, bg);
        graphics.renderOutline(x, y, 21, 11, border);
        graphics.drawCenteredString(this.font, label, x + 11, y + 2, text);
    }

    private static String categoryShort(RocketPartCategory cat) {
        return switch (cat) {
            case COMMAND -> "CMD";
            case TANK -> "TNK";
            case ENGINE -> "ENG";
            case ADAPTER -> "ADP";
            case PAYLOAD -> "PLD";
            case UTILITY -> "UTL";
        };
    }

    private static int categoryColor(RocketPartCategory cat) {
        return switch (cat) {
            case COMMAND -> 0xFF38BDF8;
            case TANK -> 0xFFCBD5E1;
            case ENGINE -> 0xFFFB923C;
            case ADAPTER -> 0xFFFACC15;
            case PAYLOAD -> 0xFFC084FC;
            case UTILITY -> 0xFF2DD4BF;
        };
    }

    private static int categoryFillColor(RocketPartCategory cat) {
        return switch (cat) {
            case COMMAND -> 0xFF13384D;
            case TANK -> 0xFF253244;
            case ENGINE -> 0xFF472512;
            case ADAPTER -> 0xFF3D3414;
            case PAYLOAD -> 0xFF341C47;
            case UTILITY -> 0xFF133D3B;
        };
    }

    private static String roleLabel(PlatformBlockRole role) {
        return switch (role) {
            case PAD -> "Launch Pad";
            case TRUSS -> "Gantry Truss";
            case CRANE_RAIL -> "Crane Rail";
            case UPGRADE_SOCKET -> "Upgrade Socket";
        };
    }

    private record InventoryPartEntry(
            int slotIndex,
            ItemStack icon,
            RocketPartConfig config,
            String displayName,
            int remaining,
            boolean creative
    ) {
    }

    private record VabEntry(
            int inventorySlot,
            @Nullable net.minecraft.core.BlockPos mountedPos,
            ItemStack icon,
            RocketPartConfig config,
            String displayName
    ) {
        boolean isMounted() {
            return this.mountedPos != null;
        }
    }

    private record VabTelemetry(
            int partCount,
            int totalHeight,
            int maxDiameter,
            int allowedDiameter,
            boolean diameterOk,
            double dryMassKg,
            double wetMassKg,
            double thrustKn,
            double twr,
            double deltaV,
            boolean hasCommand,
            boolean hasTank,
            boolean hasEngine,
            boolean ready
    ) {
    }

    private enum FormedTab {
        ASSEMBLY,
        ROCKET,
        SIMULATION
    }

    private enum ButtonStyle {
        STANDARD,
        PRIMARY_ASSEMBLE,
        DANGER_DISASSEMBLE,
        TAB_ACTIVE,
        TAB_INACTIVE
    }

    private static final class TerminalButton extends Button {
        private ButtonStyle style;

        TerminalButton(int x, int y, int width, int height, Component message, ButtonStyle style, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.style = style;
        }

        void setStyle(ButtonStyle style) {
            this.style = style;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            int bg;
            int border;
            int textColor;

            if (!this.active) {
                bg = 0xFF111823;
                border = 0xFF222E40;
                textColor = 0xFF5A6B82;
            } else if (this.style == ButtonStyle.TAB_ACTIVE) {
                bg = 0xFF19324A;
                border = 0xFF38BDF8;
                textColor = 0xFFE8F4FF;
            } else if (this.style == ButtonStyle.TAB_INACTIVE) {
                bg = this.isHoveredOrFocused() ? 0xFF182436 : 0xFF111926;
                border = this.isHoveredOrFocused() ? 0xFF3B5575 : 0xFF25354A;
                textColor = this.isHoveredOrFocused() ? 0xFFCBD5E1 : 0xFF8A9CB5;
            } else if (this.style == ButtonStyle.PRIMARY_ASSEMBLE) {
                bg = this.isHoveredOrFocused() ? 0xFF1B5E3F : 0xFF144730;
                border = this.isHoveredOrFocused() ? 0xFF4ADE80 : 0xFF22C55E;
                textColor = 0xFFECFDF5;
            } else if (this.style == ButtonStyle.DANGER_DISASSEMBLE) {
                bg = this.isHoveredOrFocused() ? 0xFF6B2129 : 0xFF4F181E;
                border = this.isHoveredOrFocused() ? 0xFFF87171 : 0xFFDC2626;
                textColor = 0xFFFEF2F2;
            } else {
                bg = this.isHoveredOrFocused() ? 0xFF243852 : 0xFF1B2A3E;
                border = this.isHoveredOrFocused() ? 0xFF4EA8DE : 0xFF334B6B;
                textColor = 0xFFE8F4FF;
            }

            graphics.fill(x, y, x + w, y + h, bg);
            graphics.renderOutline(x, y, w, h, border);
            if (this.style == ButtonStyle.TAB_ACTIVE) {
                graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF38BDF8);
            }
            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    this.getMessage(),
                    x + w / 2,
                    y + (h - 8) / 2,
                    textColor
            );
        }
    }
}
