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

import dev.galacticraft.api.vehicle.VehicleBenchCategory;
import dev.galacticraft.mod.content.block.entity.VehiclePartFabricatorBlockEntity;
import dev.galacticraft.mod.content.vehicle.VehicleFabricatorRecipes;
import dev.galacticraft.mod.network.c2s.CraftVehiclePartPayload;
import dev.galacticraft.mod.screen.VehiclePartFabricatorMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Galacticraft 5 NASA CAD Console UI for specialized vehicle workbenches.
 * Shows only the blueprints belonging to this bench's {@link VehicleBenchCategory},
 * with a scrollable catalog on the left and a schematic/materials bay on the right.
 */
public class VehiclePartFabricatorScreen extends AbstractContainerScreen<VehiclePartFabricatorMenu> {
    private static final int VISIBLE_ROWS = 5;
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 22;
    private static final int LIST_W = 98;

    private List<VehicleFabricatorRecipes.Blueprint> blueprints = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public VehiclePartFabricatorScreen(VehiclePartFabricatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 236;
        this.imageHeight = 216;
    }

    @Override
    protected void init() {
        super.init();
        this.blueprints = VehicleFabricatorRecipes.forCategory(this.menu.getBenchCategory());
        this.selectedIndex = Math.min(this.selectedIndex, Math.max(0, this.blueprints.size() - 1));
        this.scrollOffset = Math.min(this.scrollOffset, Math.max(0, this.blueprints.size() - VISIBLE_ROWS));

        this.titleLabelY = 6;
        this.inventoryLabelX = 38;
        this.inventoryLabelY = 123;

        int x = this.leftPos;
        int y = this.topPos;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.galacticraft.vehicle_fabricator.craft"), btn -> {
            if (!this.blueprints.isEmpty()) {
                VehicleFabricatorRecipes.Blueprint bp = this.blueprints.get(this.selectedIndex);
                ClientPlayNetworking.send(new CraftVehiclePartPayload(this.menu.blockPos, bp.id()));
            }
        }).bounds(x + 114, y + 78, 112, 18).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !this.blueprints.isEmpty()) {
            int lx = this.leftPos + LIST_X;
            int ly = this.topPos + LIST_Y;
            if (mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ly && mouseY < ly + VISIBLE_ROWS * ROW_HEIGHT) {
                int row = (int) ((mouseY - ly) / ROW_HEIGHT);
                int idx = this.scrollOffset + row;
                if (idx >= 0 && idx < this.blueprints.size()) {
                    this.selectedIndex = idx;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.blueprints.size() > VISIBLE_ROWS) {
            int maxScroll = this.blueprints.size() - VISIBLE_ROWS;
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        VehicleBenchCategory category = this.menu.getBenchCategory();
        int accent = category.accentColor();

        // Main aerospace titanium/carbon chassis frame
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1F252E);
        graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 3, accent);
        graphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF56647A);

        // Left blueprint catalog bay
        graphics.fill(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + VISIBLE_ROWS * ROW_HEIGHT, 0xFF141920);
        graphics.renderOutline(x + LIST_X, y + LIST_Y, LIST_W, VISIBLE_ROWS * ROW_HEIGHT, 0xFF3E4A5C);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int idx = this.scrollOffset + row;
            if (idx >= this.blueprints.size()) {
                break;
            }
            VehicleFabricatorRecipes.Blueprint bp = this.blueprints.get(idx);
            int ry = y + LIST_Y + row * ROW_HEIGHT;
            boolean selected = idx == this.selectedIndex;
            boolean hovered = mouseX >= x + LIST_X && mouseX < x + LIST_X + LIST_W && mouseY >= ry && mouseY < ry + ROW_HEIGHT;

            int rowBg = selected ? 0xFF28364A : (hovered ? 0xFF1E2734 : 0xFF161C24);
            graphics.fill(x + LIST_X + 1, ry + 1, x + LIST_X + LIST_W - 1, ry + ROW_HEIGHT - 1, rowBg);
            if (selected) {
                graphics.fill(x + LIST_X + 1, ry + 1, x + LIST_X + 3, ry + ROW_HEIGHT - 1, accent);
            }

            graphics.renderItem(bp.resultSupplier().get(), x + LIST_X + 4, ry + 1);
            String label = Component.translatable(bp.translationKey()).getString();
            if (label.length() > 11) {
                label = label.substring(0, 10) + "..";
            }
            graphics.drawString(this.font, label, x + LIST_X + 23, ry + 5, selected ? 0xFFFFFFFF : 0xFFB5C2D4, false);
        }

        // Scrollbar track if needed
        if (this.blueprints.size() > VISIBLE_ROWS) {
            int trackX = x + LIST_X + LIST_W - 3;
            int trackH = VISIBLE_ROWS * ROW_HEIGHT - 2;
            int thumbH = Math.max(12, trackH * VISIBLE_ROWS / this.blueprints.size());
            int maxScroll = this.blueprints.size() - VISIBLE_ROWS;
            int thumbY = y + LIST_Y + 1 + (trackH - thumbH) * this.scrollOffset / Math.max(1, maxScroll);
            graphics.fill(trackX, y + LIST_Y + 1, trackX + 2, y + LIST_Y + 1 + trackH, 0xFF0E1217);
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, accent);
        }

        // Right CAD Schematic & Ingredients bay
        int cadX = x + 112;
        int cadY = y + 22;
        int cadW = 116;
        int cadH = 53;
        graphics.fill(cadX, cadY, cadX + cadW, cadY + cadH, 0xFF121822);
        // Subtle blueprint grid lines
        for (int gx = cadX + 12; gx < cadX + cadW; gx += 16) {
            graphics.fill(gx, cadY + 1, gx + 1, cadY + cadH - 1, 0x182ED1FF);
        }
        for (int gy = cadY + 12; gy < cadY + cadH; gy += 16) {
            graphics.fill(cadX + 1, gy, cadX + cadW - 1, gy + 1, 0x182ED1FF);
        }
        graphics.renderOutline(cadX, cadY, cadW, cadH, 0xFF3E4A5C);

        if (!this.blueprints.isEmpty()) {
            VehicleFabricatorRecipes.Blueprint bp = this.blueprints.get(this.selectedIndex);
            graphics.renderItem(bp.resultSupplier().get(), cadX + 4, cadY + 4);

            boolean creative = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.getAbilities().instabuild;
            int costY = cadY + 23;
            for (VehicleFabricatorRecipes.IngredientCost cost : bp.costs()) {
                int have = VehicleFabricatorRecipes.countIngredient(this.menu.getFabricatorContainer(), VehiclePartFabricatorBlockEntity.INPUT_SLOTS, cost.item());
                boolean enough = creative || have >= cost.count();
                int color = enough ? 0xFF7CE89A : 0xFFE87C7C;
                String itemName = cost.item().getDescription().getString();
                if (itemName.length() > 14) {
                    itemName = itemName.substring(0, 13) + ".";
                }
                graphics.drawString(this.font, cost.count() + "x " + itemName, cadX + 6, costY, color, false);
                costY += 10;
            }
        }

        // Input slots (4) + Output slot (1)
        for (int i = 0; i < VehiclePartFabricatorBlockEntity.INPUT_SLOTS; i++) {
            int sx = x + 113 + i * 20;
            int sy = y + 101;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF141820);
            graphics.renderOutline(sx, sy, 18, 18, 0xFF465063);
        }
        graphics.fill(x + 207, y + 101, x + 225, y + 119, 0xFF141820);
        graphics.renderOutline(x + 207, y + 101, 18, 18, accent);

        // Player inventory outlines
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 37 + col * 18;
                int sy = y + 133 + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF141820);
                graphics.renderOutline(sx, sy, 18, 18, 0xFF465063);
            }
        }
        for (int col = 0; col < 9; col++) {
            int sx = x + 37 + col * 18;
            int sy = y + 191;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF141820);
            graphics.renderOutline(sx, sy, 18, 18, 0xFF465063);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        VehicleBenchCategory category = this.menu.getBenchCategory();
        graphics.drawString(this.font, this.title, 8, this.titleLabelY, 0xFFEBF2FA, false);

        Component badge = Component.translatable(category.translationKey());
        int badgeW = this.font.width(badge);
        graphics.drawString(this.font, badge, this.imageWidth - badgeW - 8, this.titleLabelY, category.accentColor(), false);

        if (!this.blueprints.isEmpty()) {
            VehicleFabricatorRecipes.Blueprint bp = this.blueprints.get(this.selectedIndex);
            String label = Component.translatable(bp.translationKey()).getString();
            if (label.length() > 15) {
                label = label.substring(0, 14) + "..";
            }
            graphics.drawString(this.font, label, 134, 30, 0xFFFFDF8A, false);
        }
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFB8C4D4, false);
    }
}
