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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.machinelib.client.api.screen.MachineScreen;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartGuiPreviewRenderer;
import dev.galacticraft.mod.content.block.entity.RocketWorkbenchBlockEntity;
import dev.galacticraft.mod.content.rocket.part.RocketMaterialRegistry;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.CostProfile;
import dev.galacticraft.mod.content.rocket.part.RocketPartFamily;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.part.SchematicGridLayout;
import dev.galacticraft.mod.content.rocket.part.SchematicGridLayout.SlotRequirement;
import dev.galacticraft.mod.network.c2s.SelectPartBuilderConfigPayload;
import dev.galacticraft.mod.screen.DynamicSchematicSlot;
import dev.galacticraft.mod.screen.PartAssemblerMenu;
import dev.galacticraft.mod.util.DrawableUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.galacticraft.mod.Constant.ElectricCompressor.*;

/**
 * Rocket Part Builder screen supporting both legacy 3x3 {@code PartAssemblerRecipe} fabrication
 * and Tinkers'-style dynamic schematic silhouettes paired with a parametric aerospace configurator
 * and live interactive 3D part preview.
 */
@Environment(EnvType.CLIENT)
public class PartAssemblerScreen extends MachineScreen<RocketWorkbenchBlockEntity, PartAssemblerMenu> {
    private static final int PANEL_OFFSET_X = 4;
    private static final int PANEL_WIDTH = 138;
    private static final int PREVIEW_HEIGHT_NORMAL = 74;
    private static final int PREVIEW_HEIGHT_COMPACT = 50;
    private static final int PANEL_BASE_CONTROLS_HEIGHT = 186;
    private static final int STATS_BOX_HEIGHT = 70;
    private static final double DROPLETS_PER_BUCKET = 81_000.0;

    private static final float PREVIEW_DEFAULT_YAW = 225.0F;
    private static final float PREVIEW_SPIN_DEG_PER_SEC = 22.0F;
    private static final float PREVIEW_Z_OFFSET = 100.0F;
    private static final float PREVIEW_MAX_HALF_DEPTH = 56.0F;
    private static final float PREVIEW_HUD_Z = 175.0F;

    private static final int COLOR_BG_PANEL = 0xF0101620;
    private static final int COLOR_BG_HEADER = 0xFF182436;
    private static final int COLOR_BG_INSET = 0xFF0C121C;
    private static final int COLOR_BG_GRID_WELL = 0xFF141D2B;
    private static final int COLOR_BORDER_OUTER = 0xFF3A4D66;
    private static final int COLOR_BORDER_INNER = 0xFF1B2636;
    private static final int COLOR_BORDER_SECTION = 0xFF28394F;
    private static final int COLOR_ACCENT_CYAN = 0xFF2D8AC4;

    private static final int COLOR_TEXT_TITLE = 0xFFE8F4FF;
    private static final int COLOR_TEXT_PRIMARY = 0xFFE2E8F0;
    private static final int COLOR_TEXT_MUTED = 0xFF8A9CB5;
    private static final int COLOR_TEXT_SKY = 0xFF7DD3FC;
    private static final int COLOR_TEXT_GOLD = 0xFFFACC15;
    private static final int COLOR_TEXT_OK = 0xFF4ADE80;
    private static final int COLOR_TEXT_BAD = 0xFFF87171;
    private static final int COLOR_TEXT_GHOST_COUNT = 0xFF93C5FD;

    private static final RocketLivery[] LIVERIES = RocketLivery.values();

    private ConfigArrowButton resetViewButton;
    private ConfigArrowButton animToggleButton;
    private ConfigArrowButton familyPrevButton;
    private ConfigArrowButton familyNextButton;
    private ConfigArrowButton diameterPrevButton;
    private ConfigArrowButton diameterNextButton;
    private ConfigArrowButton topDiameterPrevButton;
    private ConfigArrowButton topDiameterNextButton;
    private ConfigArrowButton heightPrevButton;
    private ConfigArrowButton heightNextButton;
    private final LiverySwatchButton[] liveryButtons = new LiverySwatchButton[LIVERIES.length];

    private boolean showTopDiameterRow = false;
    private int currentHeightOffset = 52;

    // Interactive 3D part preview camera & animation state
    private float previewYaw = PREVIEW_DEFAULT_YAW;
    private float previewPitch = RocketPartGuiPreviewRenderer.DEFAULT_PITCH_DEG;
    private float previewZoom = 1.0F;
    private boolean autoSpin = true;
    private boolean draggingPreview = false;
    private boolean previewAnimEnabled = false;
    private float previewAnimTime = 0.0F;
    private long lastPreviewFrameNanos = 0L;

    public PartAssemblerScreen(PartAssemblerMenu handler, Inventory inv, Component title) {
        super(handler, title, SCREEN_TEXTURE);
    }

    private int getPreviewHeight() {
        return this.height >= 268 ? PREVIEW_HEIGHT_NORMAL : PREVIEW_HEIGHT_COMPACT;
    }

    private int getPanelHeight() {
        return PANEL_BASE_CONTROLS_HEIGHT + this.getPreviewHeight();
    }

    private int getPanelX() {
        return this.leftPos + this.imageWidth + PANEL_OFFSET_X;
    }

    private int getPanelY() {
        int panelH = this.getPanelHeight();
        int ideal = this.topPos - (panelH - this.imageHeight) / 2;
        return Mth.clamp(ideal, 4, Math.max(4, this.height - panelH - 4));
    }

    private int getPreviewX() {
        return this.getPanelX() + 5;
    }

    private int getPreviewY() {
        return this.getPanelY() + 19;
    }

    private int getPreviewWidth() {
        return PANEL_WIDTH - 10;
    }

    private int getControlsTopY() {
        return this.getPreviewY() + this.getPreviewHeight() + 4;
    }

    @Override
    protected void init() {
        super.init();

        int px = this.getPanelX();
        int py = this.getPanelY();
        int cy = this.getControlsTopY();

        // Compact 3D camera & animation buttons placed cleanly in the header bar (outside scissor viewport)
        this.resetViewButton = this.addRenderableWidget(new ConfigArrowButton(
                px + PANEL_WIDTH - 32, py + 3, 13, 12, Component.literal("R"), btn -> this.resetPreviewCamera()
        ));
        this.animToggleButton = this.addRenderableWidget(new ConfigArrowButton(
                px + PANEL_WIDTH - 17, py + 3, 14, 12, Component.literal(">"), btn -> this.togglePreviewAnimation()
        ));

        this.familyPrevButton = this.addRenderableWidget(new ConfigArrowButton(
                px + 5, cy, 14, 15, Component.literal("<"), btn -> this.stepFamily(-1)
        ));
        this.familyNextButton = this.addRenderableWidget(new ConfigArrowButton(
                px + 119, cy, 14, 15, Component.literal(">"), btn -> this.stepFamily(1)
        ));

        this.diameterPrevButton = this.addRenderableWidget(new ConfigArrowButton(
                px + 48, cy + 18, 14, 14, Component.literal("<"), btn -> this.stepDiameter(-1)
        ));
        this.diameterNextButton = this.addRenderableWidget(new ConfigArrowButton(
                px + 119, cy + 18, 14, 14, Component.literal(">"), btn -> this.stepDiameter(1)
        ));

        this.topDiameterPrevButton = this.addRenderableWidget(new ConfigArrowButton(
                px + 48, cy + 35, 14, 14, Component.literal("<"), btn -> this.stepTopDiameter(-1)
        ));
        this.topDiameterNextButton = this.addRenderableWidget(new ConfigArrowButton(
                px + 119, cy + 35, 14, 14, Component.literal(">"), btn -> this.stepTopDiameter(1)
        ));

        this.heightPrevButton = this.addRenderableWidget(new ConfigArrowButton(
                px + 48, cy + 52, 14, 14, Component.literal("<"), btn -> this.stepHeight(-1)
        ));
        this.heightNextButton = this.addRenderableWidget(new ConfigArrowButton(
                px + 119, cy + 52, 14, 14, Component.literal(">"), btn -> this.stepHeight(1)
        ));

        int swatchWidth = 23;
        int swatchHeight = 16;
        int swatchGap = 3;
        int swatchStartX = px + 5;
        int swatchY = cy + 70;
        for (int i = 0; i < LIVERIES.length; i++) {
            RocketLivery livery = LIVERIES[i];
            int bx = swatchStartX + i * (swatchWidth + swatchGap);
            this.liveryButtons[i] = this.addRenderableWidget(new LiverySwatchButton(
                    bx, swatchY, swatchWidth, swatchHeight, livery, btn -> this.selectLivery(livery)
            ));
        }

        this.menu.refreshDynamicSlots();
        this.syncConfiguratorWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.menu.refreshDynamicSlots();
        this.syncConfiguratorWidgets();
    }

    public @Nullable Rect2i getConfiguratorPanelBounds() {
        if (!this.isModularModeActive()) {
            return null;
        }
        return new Rect2i(this.getPanelX(), this.getPanelY(), PANEL_WIDTH, this.getPanelHeight());
    }

    @Override
    public @NotNull List<Rect2i> getExclusionZones() {
        List<Rect2i> zones = super.getExclusionZones();
        Rect2i panel = this.getConfiguratorPanelBounds();
        if (panel != null) {
            zones.add(panel);
        }
        return zones;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        Rect2i panel = this.getConfiguratorPanelBounds();
        if (panel != null && panel.contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    @Override
    public boolean checkConfigurationPanelClick(double mouseX, double mouseY, int button) {
        if (this.isModularModeActive() && mouseX >= this.leftPos + this.imageWidth) {
            return false;
        }
        return super.checkConfigurationPanelClick(mouseX, mouseY, button);
    }

    @Override
    protected void drawConfigurationPanels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.isModularModeActive()) {
            graphics.enableScissor(0, 0, this.leftPos + this.imageWidth, this.height);
            super.drawConfigurationPanels(graphics, mouseX, mouseY);
            graphics.disableScissor();
        } else {
            super.drawConfigurationPanels(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void drawConfigurationPanelTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.isModularModeActive() && mouseX >= this.leftPos + this.imageWidth) {
            return;
        }
        super.drawConfigurationPanelTooltips(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isModularModeActive()
                && (button == 0 || button == 1)
                && this.isInsidePreviewViewport(mouseX, mouseY)) {
            this.draggingPreview = true;
            this.autoSpin = false;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingPreview) {
            this.previewYaw = (this.previewYaw + (float) dragX * 1.2F) % 360.0F;
            this.previewPitch = Mth.clamp(
                    this.previewPitch + (float) dragY * 0.85F,
                    RocketPartGuiPreviewRenderer.MIN_PITCH_DEG,
                    RocketPartGuiPreviewRenderer.MAX_PITCH_DEG
            );
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingPreview) {
            this.draggingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isModularModeActive() && this.isInsidePreviewViewport(mouseX, mouseY)) {
            this.previewZoom = Mth.clamp(
                    this.previewZoom + (float) scrollY * 0.12F,
                    RocketPartGuiPreviewRenderer.MIN_ZOOM,
                    RocketPartGuiPreviewRenderer.MAX_ZOOM
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isInsidePreviewViewport(double mouseX, double mouseY) {
        int vx = this.getPreviewX();
        int vy = this.getPreviewY();
        int vw = this.getPreviewWidth();
        int vh = this.getPreviewHeight();
        return mouseX >= vx && mouseX < vx + vw && mouseY >= vy && mouseY < vy + vh;
    }

    private void resetPreviewCamera() {
        this.previewYaw = PREVIEW_DEFAULT_YAW;
        this.previewPitch = RocketPartGuiPreviewRenderer.DEFAULT_PITCH_DEG;
        this.previewZoom = 1.0F;
        this.autoSpin = true;
    }

    private void togglePreviewAnimation() {
        this.previewAnimEnabled = !this.previewAnimEnabled;
        if (this.previewAnimEnabled) {
            this.previewAnimTime = 0.0F;
        }
        this.syncConfiguratorWidgets();
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);
        this.menu.refreshDynamicSlots();
        this.syncConfiguratorWidgets();
        this.advancePreviewClock();

        // Blit 18x18 slot border for Slot 1 (Schematic slot at 104, 53)
        graphics.blit(SCREEN_TEXTURE, this.leftPos + 103, this.topPos + 52, 29, 16, 18, 18);

        if (this.isModularModeActive()) {
            this.renderDynamicSilhouetteGrid(graphics);
            this.renderConfiguratorSidePanel(graphics, mouseX, mouseY);
        }

        this.drawCraftProgressBar(graphics);
    }

    private void advancePreviewClock() {
        long now = System.nanoTime();
        if (this.lastPreviewFrameNanos == 0L) {
            this.lastPreviewFrameNanos = now;
            return;
        }
        float dtSec = Mth.clamp((now - this.lastPreviewFrameNanos) / 1_000_000_000.0F, 0.0F, 0.1F);
        this.lastPreviewFrameNanos = now;
        if (!this.draggingPreview && this.autoSpin) {
            this.previewYaw = (this.previewYaw + dtSec * PREVIEW_SPIN_DEG_PER_SEC) % 360.0F;
        }
        if (this.previewAnimEnabled) {
            this.previewAnimTime += dtSec;
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (!this.isModularModeActive() || this.menu.be == null) {
            return;
        }

        // Blueprint requirement tooltip when hovering an empty active dynamic slot
        if (this.menu.getCarried().isEmpty()
                && this.hoveredSlot instanceof DynamicSchematicSlot dynamicSlot
                && dynamicSlot.isActive()
                && !dynamicSlot.hasItem()) {
            SlotRequirement req = dynamicSlot.getRequirement();
            if (req != null) {
                RocketPartConfig config = this.menu.be.getResolvedModularConfig();
                int multiplier = config != null ? RocketPartCostCalculator.calculate(config).slotItemMultiplier() : 1;
                int required = req.requiredCount(multiplier);
                RocketMaterialProfile cycledProfile = this.cycleHintProfile(req);
                ItemStack hintStack = cycledProfile != null
                        ? RocketMaterialRegistry.createDisplayStack(cycledProfile)
                        : this.cycleHintStack(req);
                List<Component> lines = new ArrayList<>(5);
                lines.add(Component.literal(req.hintLabel()).withStyle(ChatFormatting.AQUA));
                if (!hintStack.isEmpty()) {
                    Style matStyle = cycledProfile != null
                            ? Style.EMPTY.withColor(cycledProfile.rgbColor())
                            : Style.EMPTY.withColor(ChatFormatting.WHITE);
                    lines.add(Component.literal("Requires: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(required + "x ").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(cycledProfile != null ? cycledProfile.displayName() : hintStack.getHoverName().getString())
                                    .setStyle(matStyle)));
                }
                if (cycledProfile != null) {
                    Component quickMod = formatProfileQuickModifiers(cycledProfile);
                    if (quickMod != null) {
                        lines.add(quickMod);
                    }
                    for (RocketMaterialTrait trait : cycledProfile.traits()) {
                        lines.add(Component.literal(" \u2726 [" + trait.displayName() + "]")
                                .setStyle(Style.EMPTY.withColor(trait.rgbColor())));
                    }
                }
                int optionCount = req.acceptedProfiles().size();
                if (optionCount > 1) {
                    lines.add(Component.literal(optionCount + " compatible materials accepted")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
                graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }

        // 3D header buttons tooltips
        if (this.resetViewButton != null && this.resetViewButton.visible && this.resetViewButton.isHovered()) {
            graphics.renderTooltip(this.font, Component.literal("Resetar Câmera 3D"), mouseX, mouseY);
            return;
        }
        if (this.animToggleButton != null && this.animToggleButton.visible && this.animToggleButton.isHovered()) {
            Component msg = this.animToggleButton.active
                    ? Component.literal(this.previewAnimEnabled ? "Pausar Animação 3D" : "Testar Animação 3D")
                    : Component.literal("Peça sem animação").withStyle(ChatFormatting.GRAY);
            graphics.renderTooltip(this.font, msg, mouseX, mouseY);
            return;
        }

        // Livery button tooltip
        for (LiverySwatchButton button : this.liveryButtons) {
            if (button != null && button.visible && button.isHovered()) {
                graphics.renderTooltip(
                        this.font,
                        Component.literal("Livery: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(button.livery.displayName()).withStyle(ChatFormatting.WHITE)),
                        mouseX,
                        mouseY
                );
                return;
            }
        }

        // Full family name tooltip when hovering the family name box
        int px = this.getPanelX();
        int cy = this.getControlsTopY();
        if (mouseIn(mouseX, mouseY, px + 21, cy, 96, 15)) {
            RocketPartFamily family = this.menu.be.getResolvedModularFamily();
            if (family != null) {
                List<Component> lines = List.of(
                        Component.literal(family.displayName()).withStyle(ChatFormatting.AQUA),
                        Component.literal(family.variants().size() + (family.variants().size() == 1 ? " variant" : " parametric variants"))
                                .withStyle(ChatFormatting.GRAY)
                );
                graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }

        // Detailed live telemetry & active traits tooltip when hovering the stats readout box
        int statsX = px + 5;
        int statsY = cy + 90;
        int statsW = PANEL_WIDTH - 10;
        if (mouseIn(mouseX, mouseY, statsX, statsY, statsW, STATS_BOX_HEIGHT)) {
            RocketPartConfig config = this.menu.be.getResolvedModularConfig();
            if (config != null) {
                RocketPartComposition previewComp = this.menu.be.getPreviewComposition();
                CostProfile baseCost = RocketPartCostCalculator.calculate(config);
                CostProfile liveCost = previewComp.isEmpty() ? baseCost : RocketPartCostCalculator.calculate(config, previewComp);
                List<Component> lines = buildLiveTelemetryTooltip(config, previewComp, baseCost, liveCost);
                graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            }
        }
    }

    protected void drawCraftProgressBar(GuiGraphics graphics) {
        int progress = this.menu.getEffectiveProgress();
        int maxProgress = this.menu.getEffectiveMaxProgress();
        if (this.menu.be != null) {
            this.menu.be.setClientProgress(progress, maxProgress);
        }
        if (maxProgress > 0 && progress > 0) {
            float progressScale = Mth.clamp((float) progress / (float) maxProgress, 0.0F, 1.0F);
            int width = Mth.ceil(PROGRESS_WIDTH * progressScale);
            RenderSystem.setShaderTexture(0, SCREEN_TEXTURE);
            DrawableUtil.drawProgressTexture(graphics.pose(), this.leftPos + PROGRESS_X, this.topPos + PROGRESS_Y, PROGRESS_U, PROGRESS_V, width, PROGRESS_HEIGHT);
        }
    }

    private void renderDynamicSilhouetteGrid(GuiGraphics graphics) {
        SchematicGridLayout layout = this.menu.getActiveGridLayout();
        RocketPartConfig config = this.menu.be != null ? this.menu.be.getResolvedModularConfig() : null;
        int multiplier = config != null ? RocketPartCostCalculator.calculate(config).slotItemMultiplier() : 1;

        // Mask the static 3x3 crafting grid texture with a sleek recessed blueprint well
        int wellX = this.leftPos + 29;
        int wellY = this.topPos + 16;
        int wellSize = 54;
        graphics.fill(wellX, wellY, wellX + wellSize, wellY + wellSize, COLOR_BG_GRID_WELL);
        graphics.renderOutline(wellX, wellY, wellSize, wellSize, COLOR_BORDER_SECTION);
        // Subtle blueprint center crosshairs
        graphics.fill(wellX + wellSize / 2, wellY + 2, wellX + wellSize / 2 + 1, wellY + wellSize - 2, 0x332D8AC4);
        graphics.fill(wellX + 2, wellY + wellSize / 2, wellX + wellSize - 2, wellY + wellSize / 2 + 1, 0x332D8AC4);

        for (int i = 0; i < SchematicGridLayout.GRID_SIZE; i++) {
            if (!layout.isSlotActive(i)) {
                continue;
            }
            int sx = this.leftPos + layout.slotX(i);
            int sy = this.topPos + layout.slotY(i);

            // Blit crisp 18x18 slot border strictly at active silhouette coordinates
            graphics.blit(SCREEN_TEXTURE, sx - 1, sy - 1, 29, 16, 18, 18);

            SlotRequirement req = layout.requirement(i);
            if (req == null || this.menu.be == null) {
                continue;
            }
            int required = req.requiredCount(multiplier);
            ItemStack present = this.menu.be.itemStorage().getItem(RocketWorkbenchBlockEntity.INPUT_GRID_START + i);
            if (present.isEmpty()) {
                ItemStack hint = this.cycleHintStack(req);
                if (!hint.isEmpty()) {
                    graphics.renderFakeItem(hint, sx, sy);
                    graphics.pose().pushPose();
                    graphics.pose().translate(0.0F, 0.0F, 200.0F);
                    graphics.fill(sx, sy, sx + 16, sy + 16, 0x88141E2B);
                    String countLabel = String.valueOf(required);
                    graphics.drawString(
                            this.font,
                            countLabel,
                            sx + 16 - this.font.width(countLabel),
                            sy + 8,
                            COLOR_TEXT_GHOST_COUNT,
                            true
                    );
                    graphics.pose().popPose();
                }
            } else {
                boolean compatible = req.matches(present);
                int indicatorColor = !compatible
                        ? COLOR_TEXT_BAD
                        : (present.getCount() >= required ? COLOR_TEXT_OK : COLOR_TEXT_GOLD);
                graphics.fill(sx, sy + 15, sx + 16, sy + 16, indicatorColor);
            }
        }
    }

    private void renderConfiguratorSidePanel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.menu.be == null) {
            return;
        }
        RocketPartCategory category = this.menu.be.getActiveSchematicCategory();
        RocketPartFamily family = this.menu.be.getResolvedModularFamily();
        RocketPartConfig config = this.menu.be.getResolvedModularConfig();
        if (category == null || family == null || config == null) {
            return;
        }

        CostProfile baseCost = RocketPartCostCalculator.calculate(config);
        RocketPartComposition previewComp = this.menu.be.getPreviewComposition();
        boolean hasPreview = !previewComp.isEmpty();
        CostProfile cost = hasPreview ? RocketPartCostCalculator.calculate(config, previewComp) : baseCost;
        RocketPartStats baseStats = baseCost.stats();
        RocketPartStats stats = cost.stats();

        int px = this.getPanelX();
        int py = this.getPanelY();
        int panelH = this.getPanelHeight();
        int vpX = this.getPreviewX();
        int vpY = this.getPreviewY();
        int vpW = this.getPreviewWidth();
        int vpH = this.getPreviewHeight();
        int cy = this.getControlsTopY();

        // Outer aerospace panel frame
        graphics.fill(px, py, px + PANEL_WIDTH, py + panelH, COLOR_BG_PANEL);
        graphics.renderOutline(px, py, PANEL_WIDTH, panelH, COLOR_BORDER_OUTER);
        graphics.renderOutline(px + 1, py + 1, PANEL_WIDTH - 2, panelH - 2, COLOR_BORDER_INNER);

        // Header bar (leaves space on right for [R] and [>] 3D controls)
        graphics.fill(px + 2, py + 2, px + PANEL_WIDTH - 2, py + 17, COLOR_BG_HEADER);
        graphics.fill(px + 2, py + 16, px + PANEL_WIDTH - 2, py + 17, COLOR_ACCENT_CYAN);
        String header = this.font.plainSubstrByWidth(
                category.displayName().toUpperCase(Locale.ROOT) + " 3D",
                PANEL_WIDTH - 40
        );
        graphics.drawString(this.font, header, px + 5, py + 5, COLOR_TEXT_TITLE, false);

        // Interactive 3D Part Preview Viewport
        boolean previewHovered = this.isInsidePreviewViewport(mouseX, mouseY) || this.draggingPreview;
        RocketPartGuiPreviewRenderer.renderViewportFrame(graphics, vpX, vpY, vpW, vpH, previewHovered, true);

        float animSeconds = this.previewAnimEnabled ? this.previewAnimTime : 0.0F;
        boolean rendered3d = RocketPartGuiPreviewRenderer.renderSinglePart(
                graphics,
                config,
                vpX + 1,
                vpY + 1,
                vpW - 2,
                vpH - 2,
                this.previewYaw,
                this.previewPitch,
                this.previewZoom,
                animSeconds,
                PREVIEW_Z_OFFSET,
                PREVIEW_MAX_HALF_DEPTH,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        // Elevated 2D HUD overlay inside viewport (drawn above 3D mesh, below carried items & tooltips)
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, PREVIEW_HUD_Z);
        if (!rendered3d) {
            graphics.drawCenteredString(this.font, "Sem Modelo 3D", vpX + vpW / 2, vpY + vpH / 2 - 4, COLOR_TEXT_MUTED);
        }

        int progress = this.menu.getEffectiveProgress();
        int maxProgress = this.menu.getEffectiveMaxProgress();
        if (maxProgress > 0 && progress > 0) {
            float ratio = Mth.clamp((float) progress / (float) maxProgress, 0.0F, 1.0F);
            int barW = Math.round((vpW - 6) * ratio);
            graphics.fill(vpX + 3, vpY + vpH - 4, vpX + vpW - 3, vpY + vpH - 2, 0xFF142030);
            if (barW > 0) {
                graphics.fill(vpX + 3, vpY + vpH - 4, vpX + 3 + barW, vpY + vpH - 2, COLOR_TEXT_OK);
            }
            String pctText = "MONTANDO " + Math.round(ratio * 100.0F) + "%";
            graphics.drawString(this.font, pctText, vpX + 4, vpY + 3, COLOR_TEXT_OK, true);
        } else if (previewHovered) {
            String hint = "Arraste/Scroll";
            graphics.drawString(this.font, hint, vpX + vpW - 4 - this.font.width(hint), vpY + 3, COLOR_TEXT_SKY, true);
        }

        String sizeTag = config.radial()
                ? "1x1R " + config.heightBlocks() + "b"
                : config.diameter() != config.topDiameter()
                ? config.diameter() + "->" + config.topDiameter() + " " + config.heightBlocks() + "b"
                : config.diameter() + "x" + config.diameter() + " " + config.heightBlocks() + "b";
        graphics.drawString(this.font, sizeTag, vpX + 4, vpY + vpH - 12, COLOR_TEXT_SKY, true);
        pose.popPose();

        // Family selector display inset between < and >
        int famBoxX = px + 21;
        int famBoxW = 96;
        graphics.fill(famBoxX, cy, famBoxX + famBoxW, cy + 15, COLOR_BG_INSET);
        graphics.renderOutline(famBoxX, cy, famBoxW, 15, COLOR_BORDER_SECTION);
        String famLabel = this.font.plainSubstrByWidth(family.displayName(), famBoxW - 6);
        graphics.drawCenteredString(this.font, famLabel, famBoxX + famBoxW / 2, cy + 4, COLOR_TEXT_PRIMARY);

        // Parametric rows or fixed-spec badge
        if (this.hasParametricControls(family)) {
            this.drawParametricRow(
                    graphics,
                    px,
                    cy + 18,
                    "Diam:",
                    config.radial() ? "1x1 R" : (config.diameter() + "x" + config.diameter())
            );
            if (this.showTopDiameterRow) {
                this.drawParametricRow(
                        graphics,
                        px,
                        cy + 35,
                        "Top:",
                        config.topDiameter() + "x" + config.topDiameter()
                );
            }
            this.drawParametricRow(
                    graphics,
                    px,
                    cy + this.currentHeightOffset,
                    "Height:",
                    config.heightBlocks() + "b"
            );
        } else {
            int fixedY = cy + 20;
            graphics.fill(px + 5, fixedY, px + PANEL_WIDTH - 5, fixedY + 42, COLOR_BG_INSET);
            graphics.renderOutline(px + 5, fixedY, PANEL_WIDTH - 10, 42, COLOR_BORDER_SECTION);
            graphics.drawCenteredString(this.font, "Fixed Dimensions", px + PANEL_WIDTH / 2, fixedY + 9, COLOR_TEXT_MUTED);
            String dimSummary = config.radial()
                    ? String.format(Locale.ROOT, "1x1 Radial, %db", config.heightBlocks())
                    : config.diameter() != config.topDiameter()
                    ? String.format(Locale.ROOT, "%dx%d -> %dx%d, %db", config.diameter(), config.diameter(), config.topDiameter(), config.topDiameter(), config.heightBlocks())
                    : String.format(Locale.ROOT, "%dx%d, %db", config.diameter(), config.diameter(), config.heightBlocks());
            graphics.drawCenteredString(this.font, dimSummary, px + PANEL_WIDTH / 2, fixedY + 23, COLOR_TEXT_SKY);
        }

        // Live Tinkers' telemetry, green/red deltas & active traits readout box
        int statsX = px + 5;
        int statsY = cy + 90;
        int statsW = PANEL_WIDTH - 10;
        int statsH = STATS_BOX_HEIGHT;
        graphics.fill(statsX, statsY, statsX + statsW, statsY + statsH, COLOR_BG_INSET);
        graphics.renderOutline(statsX, statsY, statsW, statsH, hasPreview ? COLOR_ACCENT_CYAN : COLOR_BORDER_SECTION);

        // Line 1: Dry Mass + green/red % delta
        String massText = String.format(Locale.ROOT, "Mass: %.1fkg", stats.dryWeightKg());
        graphics.drawString(this.font, massText, statsX + 4, statsY + 4, COLOR_TEXT_PRIMARY, false);
        if (hasPreview && baseStats.dryWeightKg() > 0.0) {
            int massPct = (int) Math.round(((stats.dryWeightKg() - baseStats.dryWeightKg()) / baseStats.dryWeightKg()) * 100.0);
            if (massPct != 0) {
                String delta = (massPct > 0 ? "+" : "") + massPct + "%";
                int deltaColor = massPct < 0 ? COLOR_TEXT_OK : COLOR_TEXT_BAD;
                graphics.drawString(this.font, delta, statsX + statsW - 4 - this.font.width(delta), statsY + 4, deltaColor, false);
            }
        }

        // Line 2: Thrust / Fuel / Max Integrity + green/red % delta
        if (stats.thrustKn() > 0.0) {
            String thrustText = String.format(Locale.ROOT, "Thrust: %.0fkN", stats.thrustKn());
            graphics.drawString(this.font, thrustText, statsX + 4, statsY + 14, COLOR_TEXT_GOLD, false);
            if (hasPreview && baseStats.thrustKn() > 0.0) {
                int thrustPct = (int) Math.round(((stats.thrustKn() - baseStats.thrustKn()) / baseStats.thrustKn()) * 100.0);
                if (thrustPct != 0) {
                    String delta = (thrustPct > 0 ? "+" : "") + thrustPct + "%";
                    int deltaColor = thrustPct > 0 ? COLOR_TEXT_OK : COLOR_TEXT_BAD;
                    graphics.drawString(this.font, delta, statsX + statsW - 4 - this.font.width(delta), statsY + 14, deltaColor, false);
                }
            }
        } else if (stats.fuelCapacityDroplets() > 0L) {
            double buckets = stats.fuelCapacityDroplets() / DROPLETS_PER_BUCKET;
            String fuelText = String.format(Locale.ROOT, "Fuel: %.1fB", buckets);
            graphics.drawString(this.font, fuelText, statsX + 4, statsY + 14, COLOR_TEXT_SKY, false);
            if (hasPreview && baseStats.fuelCapacityDroplets() > 0L) {
                int capPct = (int) Math.round(((double) (stats.fuelCapacityDroplets() - baseStats.fuelCapacityDroplets())
                        / (double) baseStats.fuelCapacityDroplets()) * 100.0);
                if (capPct != 0) {
                    String delta = (capPct > 0 ? "+" : "") + capPct + "%";
                    int deltaColor = capPct > 0 ? COLOR_TEXT_OK : COLOR_TEXT_BAD;
                    graphics.drawString(this.font, delta, statsX + statsW - 4 - this.font.width(delta), statsY + 14, deltaColor, false);
                }
            }
        } else {
            String integText = String.format(Locale.ROOT, "Integ: %.0f", cost.maxIntegrity());
            graphics.drawString(this.font, integText, statsX + 4, statsY + 14, COLOR_TEXT_SKY, false);
            if (hasPreview) {
                int integPct = (int) Math.round(((cost.maxIntegrity() - PartIntegrity.DEFAULT_MAX_INTEGRITY)
                        / PartIntegrity.DEFAULT_MAX_INTEGRITY) * 100.0);
                if (integPct != 0) {
                    String delta = (integPct > 0 ? "+" : "") + integPct + "%";
                    int deltaColor = integPct > 0 ? COLOR_TEXT_OK : COLOR_TEXT_BAD;
                    graphics.drawString(this.font, delta, statsX + statsW - 4 - this.font.width(delta), statsY + 14, deltaColor, false);
                }
            }
        }

        // Line 3: Thermal tolerance + green/red K delta
        String thermalText = String.format(Locale.ROOT, "Temp: %.0fK", stats.thermalToleranceKelvin());
        graphics.drawString(this.font, thermalText, statsX + 4, statsY + 24, COLOR_TEXT_PRIMARY, false);
        if (hasPreview) {
            int dK = (int) Math.round(stats.thermalToleranceKelvin() - baseStats.thermalToleranceKelvin());
            if (dK != 0) {
                String delta = (dK > 0 ? "+" : "") + dK + "K";
                int deltaColor = dK > 0 ? COLOR_TEXT_OK : COLOR_TEXT_BAD;
                graphics.drawString(this.font, delta, statsX + statsW - 4 - this.font.width(delta), statsY + 24, deltaColor, false);
            }
        }

        // Line 4: Pressure tolerance + green/red % delta
        String presText = String.format(Locale.ROOT, "Pres: %.1fatm", stats.pressureToleranceAtm());
        graphics.drawString(this.font, presText, statsX + 4, statsY + 34, COLOR_TEXT_PRIMARY, false);
        if (hasPreview && baseStats.pressureToleranceAtm() > 0.0) {
            int pPct = (int) Math.round(((stats.pressureToleranceAtm() - baseStats.pressureToleranceAtm())
                    / baseStats.pressureToleranceAtm()) * 100.0);
            if (pPct != 0) {
                String delta = (pPct > 0 ? "+" : "") + pPct + "%";
                int deltaColor = pPct > 0 ? COLOR_TEXT_OK : COLOR_TEXT_BAD;
                graphics.drawString(this.font, delta, statsX + statsW - 4 - this.font.width(delta), statsY + 34, deltaColor, false);
            }
        }

        // Line 5: Crafting requirement / Boil-Off / Aneutronic Power highlight
        int seconds = Math.max(1, Math.round(cost.assemblyTimeTicks() / 20.0F));
        if (hasPreview && (config.category() == RocketPartCategory.TANK || cost.boilOffMultiplier() < 0.999) && cost.boilOffMultiplier() < 0.999) {
            String boilText = cost.boilOffMultiplier() <= 0.001
                    ? String.format(Locale.ROOT, "x%d | Boil: 0%%", cost.slotItemMultiplier())
                    : String.format(Locale.ROOT, "x%d | Boil: %.2fx", cost.slotItemMultiplier(), cost.boilOffMultiplier());
            graphics.drawString(this.font, boilText, statsX + 4, statsY + 44, COLOR_TEXT_OK, false);
        } else if (hasPreview && previewComp.hasTrait(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR)) {
            String pwrText = String.format(Locale.ROOT, "x%d | +%dGJ/t", cost.slotItemMultiplier(), RocketPartCostCalculator.ANEUTRONIC_BOARD_REACTOR_GEN_PER_TICK);
            graphics.drawString(this.font, pwrText, statsX + 4, statsY + 44, COLOR_TEXT_SKY, false);
        } else {
            String reqLine = String.format(Locale.ROOT, "Req x%d/slot (%ds)", cost.slotItemMultiplier(), seconds);
            graphics.drawString(this.font, reqLine, statsX + 4, statsY + 44, COLOR_TEXT_MUTED, false);
        }

        // Line 6: Active Trait chips strip (when materials with traits are installed) OR part badge
        int badgeY = statsY + 55;
        List<RocketMaterialTrait> activeTraits = hasPreview ? previewComp.allTraits() : List.of();
        if (!activeTraits.isEmpty()) {
            this.drawActiveTraitStrip(graphics, statsX + 3, badgeY, statsW - 6, activeTraits);
        } else if (config.walkableInterior()) {
            graphics.fill(statsX + 3, badgeY, statsX + statsW - 3, badgeY + 12, 0xFF143826);
            graphics.renderOutline(statsX + 3, badgeY, statsW - 6, 12, 0xFF34D399);
            graphics.drawCenteredString(this.font, "Walkable Interior", statsX + statsW / 2, badgeY + 2, COLOR_TEXT_OK);
        } else if (config.radial()) {
            graphics.fill(statsX + 3, badgeY, statsX + statsW - 3, badgeY + 12, 0xFF123344);
            graphics.renderOutline(statsX + 3, badgeY, statsW - 6, 12, 0xFF38BDF8);
            graphics.drawCenteredString(this.font, "Radial Mount", statsX + statsW / 2, badgeY + 2, COLOR_TEXT_SKY);
        } else {
            String dimLine = config.diameter() != config.topDiameter()
                    ? String.format(Locale.ROOT, "%dx%d->%dx%d | %db", config.diameter(), config.diameter(), config.topDiameter(), config.topDiameter(), config.heightBlocks())
                    : String.format(Locale.ROOT, "Footprint: %dx%d | %db", config.diameter(), config.diameter(), config.heightBlocks());
            graphics.drawString(this.font, dimLine, statsX + 4, badgeY + 2, COLOR_TEXT_MUTED, false);
        }
    }

    private void drawActiveTraitStrip(GuiGraphics graphics, int x, int y, int width, List<RocketMaterialTrait> traits) {
        graphics.fill(x, y, x + width, y + 12, 0xFF101B29);
        graphics.renderOutline(x, y, width, 12, COLOR_BORDER_SECTION);
        String countLabel = traits.size() + (traits.size() == 1 ? " Trait" : " Traits");
        int countW = this.font.width(countLabel);
        int chipMaxX = x + width - countW - 6;
        int cursorX = x + 3;
        for (RocketMaterialTrait trait : traits) {
            if (cursorX + 10 > chipMaxX) {
                break;
            }
            int argb = 0xFF000000 | trait.rgbColor();
            graphics.fill(cursorX, y + 2, cursorX + 8, y + 10, argb);
            graphics.renderOutline(cursorX, y + 2, 8, 8, 0x80000000);
            cursorX += 10;
        }
        graphics.drawString(this.font, countLabel, x + width - countW - 3, y + 2, COLOR_TEXT_SKY, false);
    }

    private void drawParametricRow(GuiGraphics graphics, int panelX, int rowY, String label, String value) {
        graphics.drawString(this.font, label, panelX + 6, rowY + 3, COLOR_TEXT_MUTED, false);
        int boxX = panelX + 64;
        int boxW = 53;
        graphics.fill(boxX, rowY, boxX + boxW, rowY + 14, COLOR_BG_INSET);
        graphics.renderOutline(boxX, rowY, boxW, 14, COLOR_BORDER_SECTION);
        graphics.drawCenteredString(this.font, value, boxX + boxW / 2, rowY + 3, COLOR_TEXT_PRIMARY);
    }

    private void syncConfiguratorWidgets() {
        if (this.familyPrevButton == null) {
            return;
        }
        boolean modular = this.isModularModeActive();
        RocketPartCategory category = modular && this.menu.be != null ? this.menu.be.getActiveSchematicCategory() : null;
        RocketPartFamily family = modular && this.menu.be != null ? this.menu.be.getResolvedModularFamily() : null;
        RocketPartConfig config = modular && this.menu.be != null ? this.menu.be.getResolvedModularConfig() : null;

        boolean valid = modular && category != null && family != null && config != null;
        int familyCount = valid ? RocketPartCatalog.getInstance().familiesFor(category).size() : 0;

        if (this.resetViewButton != null) {
            this.resetViewButton.visible = valid;
            this.resetViewButton.active = valid;
        }
        if (this.animToggleButton != null) {
            RocketPartGuiPreviewRenderer.ResolvedModel resolved = valid ? RocketPartGuiPreviewRenderer.resolve(config) : null;
            boolean hasAnims = resolved != null && resolved.model().hasAnimations();
            this.animToggleButton.visible = valid;
            this.animToggleButton.active = valid && hasAnims;
            this.animToggleButton.setMessage(Component.literal(this.previewAnimEnabled && hasAnims ? "||" : ">"));
        }

        this.familyPrevButton.visible = valid;
        this.familyNextButton.visible = valid;
        this.familyPrevButton.active = valid && familyCount > 1;
        this.familyNextButton.active = valid && familyCount > 1;

        boolean parametric = valid && this.hasParametricControls(family);
        boolean showTop = parametric && (family.availableTopDiameters().size() > 1 || category == RocketPartCategory.ADAPTER);
        this.showTopDiameterRow = showTop;
        this.currentHeightOffset = showTop ? 52 : 35;

        int cy = this.getControlsTopY();
        this.heightPrevButton.setY(cy + this.currentHeightOffset);
        this.heightNextButton.setY(cy + this.currentHeightOffset);

        boolean canStepDiam = parametric && family.availableDiameters().size() > 1;
        this.diameterPrevButton.visible = parametric;
        this.diameterNextButton.visible = parametric;
        this.diameterPrevButton.active = canStepDiam;
        this.diameterNextButton.active = canStepDiam;

        boolean canStepTop = showTop && family.availableTopDiametersFor(config.diameter()).size() > 1;
        this.topDiameterPrevButton.visible = showTop;
        this.topDiameterNextButton.visible = showTop;
        this.topDiameterPrevButton.active = canStepTop;
        this.topDiameterNextButton.active = canStepTop;

        boolean canStepHeight = parametric && family.availableHeightsFor(config.diameter(), config.topDiameter()).size() > 1;
        this.heightPrevButton.visible = parametric;
        this.heightNextButton.visible = parametric;
        this.heightPrevButton.active = canStepHeight;
        this.heightNextButton.active = canStepHeight;

        RocketLivery selectedLivery = valid ? config.livery() : RocketLivery.WHITE;
        for (LiverySwatchButton button : this.liveryButtons) {
            if (button != null) {
                button.visible = valid;
                button.active = valid;
                button.setSelected(valid && button.livery == selectedLivery);
            }
        }
    }

    private boolean hasParametricControls(@NotNull RocketPartFamily family) {
        return family.parametric()
                || family.availableDiameters().size() > 1
                || family.availableTopDiameters().size() > 1
                || family.availableHeights().size() > 1;
    }

    private boolean isModularModeActive() {
        return this.menu.be != null && this.menu.be.getActiveSchematicCategory() != null;
    }

    private void stepFamily(int delta) {
        if (this.menu.be == null) {
            return;
        }
        RocketPartCategory category = this.menu.be.getActiveSchematicCategory();
        RocketPartFamily current = this.menu.be.getResolvedModularFamily();
        if (category == null || current == null) {
            return;
        }
        List<RocketPartFamily> families = RocketPartCatalog.getInstance().familiesFor(category);
        if (families.size() <= 1) {
            return;
        }
        int idx = 0;
        for (int i = 0; i < families.size(); i++) {
            if (families.get(i).familyId().equals(current.familyId())) {
                idx = i;
                break;
            }
        }
        RocketPartFamily nextFamily = families.get(Math.floorMod(idx + delta, families.size()));
        RocketPartSpec nextSpec = nextFamily.defaultVariant();
        this.sendConfigUpdate(
                nextFamily.familyId(),
                nextSpec.diameter(),
                nextSpec.topDiameter(),
                nextSpec.heightBlocks(),
                this.menu.be.getSelectedLivery()
        );
    }

    private void stepDiameter(int delta) {
        if (this.menu.be == null) {
            return;
        }
        RocketPartFamily family = this.menu.be.getResolvedModularFamily();
        RocketPartConfig config = this.menu.be.getResolvedModularConfig();
        if (family == null || config == null) {
            return;
        }
        List<Integer> options = family.availableDiameters();
        if (options.size() <= 1) {
            return;
        }
        int nextDiam = cycleOption(options, config.diameter(), delta);
        RocketPartSpec matched = family.findBestMatch(nextDiam, config.topDiameter(), config.heightBlocks());
        this.sendConfigUpdate(
                family.familyId(),
                matched.diameter(),
                matched.topDiameter(),
                matched.heightBlocks(),
                config.livery()
        );
    }

    private void stepTopDiameter(int delta) {
        if (this.menu.be == null) {
            return;
        }
        RocketPartFamily family = this.menu.be.getResolvedModularFamily();
        RocketPartConfig config = this.menu.be.getResolvedModularConfig();
        if (family == null || config == null) {
            return;
        }
        List<Integer> options = family.availableTopDiametersFor(config.diameter());
        if (options.size() <= 1) {
            return;
        }
        int nextTop = cycleOption(options, config.topDiameter(), delta);
        RocketPartSpec matched = family.findBestMatch(config.diameter(), nextTop, config.heightBlocks());
        this.sendConfigUpdate(
                family.familyId(),
                matched.diameter(),
                matched.topDiameter(),
                matched.heightBlocks(),
                config.livery()
        );
    }

    private void stepHeight(int delta) {
        if (this.menu.be == null) {
            return;
        }
        RocketPartFamily family = this.menu.be.getResolvedModularFamily();
        RocketPartConfig config = this.menu.be.getResolvedModularConfig();
        if (family == null || config == null) {
            return;
        }
        List<Integer> options = family.availableHeightsFor(config.diameter(), config.topDiameter());
        if (options.size() <= 1) {
            return;
        }
        int nextHeight = cycleOption(options, config.heightBlocks(), delta);
        RocketPartSpec matched = family.findBestMatch(config.diameter(), config.topDiameter(), nextHeight);
        this.sendConfigUpdate(
                family.familyId(),
                matched.diameter(),
                matched.topDiameter(),
                matched.heightBlocks(),
                config.livery()
        );
    }

    private void selectLivery(@NotNull RocketLivery livery) {
        if (this.menu.be == null) {
            return;
        }
        RocketPartConfig config = this.menu.be.getResolvedModularConfig();
        if (config == null) {
            return;
        }
        this.sendConfigUpdate(
                config.familyId(),
                config.diameter(),
                config.topDiameter(),
                config.heightBlocks(),
                livery
        );
    }

    private void sendConfigUpdate(
            @NotNull String familyId,
            int diameter,
            int topDiameter,
            int heightBlocks,
            @NotNull RocketLivery livery
    ) {
        if (this.menu.be == null) {
            return;
        }
        // Optimistic client update for snappy UI feedback before server delta sync confirms
        this.menu.be.setClientSelectedFamilyId(familyId);
        this.menu.be.setClientSelectedDiameter(diameter);
        this.menu.be.setClientSelectedTopDiameter(topDiameter);
        this.menu.be.setClientSelectedHeight(heightBlocks);
        this.menu.be.setClientSelectedLivery(livery);

        ClientPlayNetworking.send(new SelectPartBuilderConfigPayload(
                this.menu.be.getBlockPos(),
                familyId,
                diameter,
                topDiameter,
                heightBlocks,
                livery
        ));
        this.syncConfiguratorWidgets();
    }

    private static int cycleOption(List<Integer> options, int currentValue, int delta) {
        int idx = options.indexOf(currentValue);
        if (idx < 0) {
            idx = 0;
        }
        return options.get(Math.floorMod(idx + delta, options.size()));
    }

    private @Nullable RocketMaterialProfile cycleHintProfile(SlotRequirement req) {
        List<RocketMaterialProfile> profiles = req.acceptedProfiles();
        if (profiles.isEmpty()) {
            return null;
        }
        if (profiles.size() == 1) {
            return profiles.getFirst();
        }
        long slotTick = (System.currentTimeMillis() / 1200L);
        return profiles.get((int) Math.floorMod(slotTick, profiles.size()));
    }

    private ItemStack cycleHintStack(SlotRequirement req) {
        RocketMaterialProfile profile = this.cycleHintProfile(req);
        if (profile != null) {
            ItemStack stack = RocketMaterialRegistry.createDisplayStack(profile);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        ItemStack[] stacks = req.displayStacks();
        if (stacks.length == 0) {
            return ItemStack.EMPTY;
        }
        if (stacks.length == 1) {
            return stacks[0];
        }
        long slotTick = (System.currentTimeMillis() / 1200L);
        return stacks[(int) Math.floorMod(slotTick, stacks.length)];
    }

    private static @Nullable Component formatProfileQuickModifiers(RocketMaterialProfile profile) {
        MutableComponent line = Component.empty();
        int count = 0;

        int massPct = (int) Math.round(profile.massModifier() * 100.0);
        if (massPct != 0) {
            line.append(Component.literal((massPct > 0 ? "+" : "") + massPct + "% Mass")
                    .withStyle(massPct < 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
            count++;
        }
        int thermalK = (int) Math.round(profile.thermalBonusKelvin());
        if (thermalK != 0) {
            if (count > 0) {
                line.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            }
            line.append(Component.literal((thermalK > 0 ? "+" : "") + thermalK + "K")
                    .withStyle(thermalK > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
            count++;
        }
        if (Math.abs(profile.pressureMultiplier() - 1.0) >= 0.01) {
            if (count > 0) {
                line.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            }
            line.append(Component.literal(String.format(Locale.ROOT, "%.2fx atm", profile.pressureMultiplier()))
                    .withStyle(profile.pressureMultiplier() > 1.0 ? ChatFormatting.GREEN : ChatFormatting.RED));
            count++;
        }
        if (profile.boilOffMultiplier() < 0.999) {
            if (count > 0) {
                line.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            }
            String boil = profile.boilOffMultiplier() <= 0.001 ? "0% Boil-Off" : String.format(Locale.ROOT, "%.2fx Boil", profile.boilOffMultiplier());
            line.append(Component.literal(boil).withStyle(ChatFormatting.AQUA));
            count++;
        }
        int thrustPct = (int) Math.round(profile.thrustBonus() * 100.0);
        if (thrustPct != 0) {
            if (count > 0) {
                line.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            }
            line.append(Component.literal("+" + thrustPct + "% Thrust").withStyle(ChatFormatting.GOLD));
            count++;
        }
        int capPct = (int) Math.round(profile.capacityBonus() * 100.0);
        if (capPct != 0) {
            if (count > 0) {
                line.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            }
            line.append(Component.literal("+" + capPct + "% Cap").withStyle(ChatFormatting.YELLOW));
            count++;
        }
        return count > 0 ? line : null;
    }

    private static List<Component> buildLiveTelemetryTooltip(
            RocketPartConfig config,
            RocketPartComposition composition,
            CostProfile baseCost,
            CostProfile liveCost
    ) {
        List<Component> lines = new ArrayList<>();
        RocketPartStats base = baseCost.stats();
        RocketPartStats live = liveCost.stats();
        boolean modified = !composition.isEmpty();

        lines.add(Component.literal(modified ? "Live Material Telemetry Preview" : "Baseline Part Telemetry")
                .withStyle(ChatFormatting.AQUA));

        lines.add(formatComparisonLine("Dry Mass", String.format(Locale.ROOT, "%.1f kg", live.dryWeightKg()),
                base.dryWeightKg(), live.dryWeightKg(), true, modified));
        if (live.thrustKn() > 0.0) {
            lines.add(formatComparisonLine("Thrust", String.format(Locale.ROOT, "%.1f kN", live.thrustKn()),
                    base.thrustKn(), live.thrustKn(), false, modified));
        }
        if (live.fuelCapacityDroplets() > 0L) {
            lines.add(formatComparisonLine("Propellant", String.format(Locale.ROOT, "%.1f B", live.fuelCapacityDroplets() / DROPLETS_PER_BUCKET),
                    base.fuelCapacityDroplets(), live.fuelCapacityDroplets(), false, modified));
        }
        int dK = (int) Math.round(live.thermalToleranceKelvin() - base.thermalToleranceKelvin());
        MutableComponent tempLine = Component.literal("Thermal: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(Locale.ROOT, "%,.0f K", live.thermalToleranceKelvin())).withStyle(ChatFormatting.WHITE));
        if (modified && dK != 0) {
            tempLine.append(Component.literal(" (" + (dK > 0 ? "+" : "") + dK + " K)")
                    .withStyle(dK > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        lines.add(tempLine);

        lines.add(formatComparisonLine("Pressure", String.format(Locale.ROOT, "%.1f atm", live.pressureToleranceAtm()),
                base.pressureToleranceAtm(), live.pressureToleranceAtm(), false, modified));
        lines.add(formatComparisonLine("Max Integrity", String.format(Locale.ROOT, "%.0f", liveCost.maxIntegrity()),
                PartIntegrity.DEFAULT_MAX_INTEGRITY, liveCost.maxIntegrity(), false, modified));

        if (config.category() == RocketPartCategory.TANK || liveCost.boilOffMultiplier() < 0.999) {
            MutableComponent boilLine = Component.literal("Boil-Off: ").withStyle(ChatFormatting.GRAY);
            if (liveCost.boilOffMultiplier() <= 0.001) {
                boilLine.append(Component.literal("0.00x (Zero)").withStyle(ChatFormatting.AQUA));
            } else {
                boilLine.append(Component.literal(String.format(Locale.ROOT, "%.2fx", liveCost.boilOffMultiplier()))
                        .withStyle(liveCost.boilOffMultiplier() < 1.0 ? ChatFormatting.GREEN : ChatFormatting.WHITE));
            }
            lines.add(boilLine);
        }

        if (modified) {
            List<RocketMaterialTrait> traits = composition.allTraits();
            if (!traits.isEmpty()) {
                lines.add(Component.literal("Active Engineering Traits:").withStyle(ChatFormatting.GRAY));
                for (RocketMaterialTrait trait : traits) {
                    lines.add(Component.literal(" \u2726 [" + trait.displayName() + "]")
                            .setStyle(Style.EMPTY.withColor(trait.rgbColor()))
                            .append(Component.literal(": " + trait.description()).withStyle(ChatFormatting.GRAY)));
                }
            }
        } else {
            lines.add(Component.literal("Place materials in the grid to preview stats & traits")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        return lines;
    }

    private static Component formatComparisonLine(
            String label,
            String valueText,
            double baseVal,
            double liveVal,
            boolean lowerIsBetter,
            boolean modified
    ) {
        MutableComponent line = Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(valueText).withStyle(ChatFormatting.WHITE));
        if (modified && baseVal > 0.0) {
            int pct = (int) Math.round(((liveVal - baseVal) / baseVal) * 100.0);
            if (pct != 0) {
                boolean beneficial = lowerIsBetter ? pct < 0 : pct > 0;
                line.append(Component.literal(" (" + (pct > 0 ? "+" : "") + pct + "%)")
                        .withStyle(beneficial ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        }
        return line;
    }

    private static String liveryShortCode(RocketLivery livery) {
        return switch (livery) {
            case WHITE -> "W";
            case BLACK -> "B";
            case ORANGE -> "O";
            case BLUE -> "U";
            case OLIVE -> "G";
        };
    }

    private static final class ConfigArrowButton extends Button {
        ConfigArrowButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
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
                textColor = 0xFF4B5C73;
            } else {
                bg = this.isHoveredOrFocused() ? 0xFF243852 : 0xFF1B2A3E;
                border = this.isHoveredOrFocused() ? 0xFF4EA8DE : 0xFF334B6B;
                textColor = 0xFFE8F4FF;
            }
            graphics.fill(x, y, x + w, y + h, bg);
            graphics.renderOutline(x, y, w, h, border);
            this.renderString(graphics, Minecraft.getInstance().font, textColor);
        }
    }

    private static final class LiverySwatchButton extends Button {
        final RocketLivery livery;
        private boolean selected;

        LiverySwatchButton(int x, int y, int width, int height, RocketLivery livery, OnPress onPress) {
            super(x, y, width, height, Component.literal(liveryShortCode(livery)), onPress, DEFAULT_NARRATION);
            this.livery = livery;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();

            int fill = this.livery.argbColor();
            int outerBorder = this.selected ? 0xFFFACC15 : (this.isHoveredOrFocused() ? 0xFF38BDF8 : 0xFF3A4D66);
            int textColor = this.livery == RocketLivery.WHITE ? 0xFF101620 : 0xFFF8FAFC;

            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
            graphics.renderOutline(x, y, w, h, outerBorder);
            if (this.selected) {
                graphics.renderOutline(x + 1, y + 1, w - 2, h - 2, 0xFF101620);
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
