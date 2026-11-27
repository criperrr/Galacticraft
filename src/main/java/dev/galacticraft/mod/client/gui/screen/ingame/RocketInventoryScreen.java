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

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.galacticraft.api.vehicle.SpatialCargoItem;
import dev.galacticraft.machinelib.client.api.util.DisplayUtil;
import dev.galacticraft.machinelib.client.api.util.GraphicsUtil;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.vehicle.RocketCargoGrid;
import dev.galacticraft.mod.screen.RocketMenu;
import dev.galacticraft.mod.util.DrawableUtil;
import dev.galacticraft.mod.util.Translations;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RocketInventoryScreen extends AbstractContainerScreen<RocketMenu> {
    public RocketInventoryScreen(RocketMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        this.imageWidth = 176;
        this.imageHeight = 216;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 0;
        this.inventoryLabelY = 124;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        final Component fuel = Component.translatable(Translations.Ui.ROCKET_FUEL);
        graphics.drawString(this.font, fuel, 37 - this.font.width(fuel) / 2, 12, 0xD8DCE3, false);

        final float percentage = this.menu.rocket.getScaledFuelLevel(100);
        final Style style = Constant.Text.getStorageLevelStyle(1.0F - 0.01F * percentage);
        final MutableComponent text = Component.literal(String.format("%.1f", percentage)).append(Component.translatable(Translations.Ui.ROCKET_FULL));
        graphics.drawString(this.font, text.withStyle(style), 37 - this.font.width(text) / 2, 24, 0xFFFFFF, false);

        graphics.drawString(this.font, Component.translatable("container.galacticraft.rocket_cargo"), 114, 18, 0xC8D4E3, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float v, int mouseX, int mouseY) {
        // Clean industrial GC5 frame supporting fuel tank + 9x4 spatial cargo grid + player inventory
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF2B313C);
        graphics.renderOutline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF5A6578);

        // Fuel gauge panel
        final int fuelX = this.leftPos + 71;
        final int fuelY = this.topPos + 7;
        final int fuelW = 34;
        final int fuelH = 38;
        graphics.fill(fuelX - 1, fuelY - 1, fuelX + fuelW + 1, fuelY + fuelH + 1, 0xFF1A1E26);
        final long amount = this.menu.rocket.getFuel();
        final long capacity = this.menu.rocket.getFuelTankCapacity();
        GraphicsUtil.drawFluid(graphics, fuelX, fuelY, fuelW, fuelH, capacity, FluidVariant.of(GCFluids.FUEL), amount);

        boolean primary = true;
        for (int y = fuelY + fuelH - 3; y > fuelY; y -= 3) {
            graphics.hLine(fuelX, fuelX + (primary ? 9 : 7), y, 0xFFB31212);
            primary = !primary;
        }

        // Spatial 2D Rocket Cargo Grid slots
        RocketCargoGrid grid = this.menu.cargoGrid;
        for (int r = 0; r < grid.getRows(); r++) {
            for (int c = 0; c < grid.getColumns(); c++) {
                int sx = this.leftPos + RocketMenu.CARGO_START_X + c * 18 - 1;
                int sy = this.topPos + RocketMenu.CARGO_START_Y + r * 18 - 1;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF1F242E);
                graphics.renderOutline(sx, sy, 18, 18, 0xFF424B5B);
            }
        }

        // Highlight multi-slot spatial crates (2x2 / 3x2 / 4x3) across their full footprint
        for (int i = 0; i < grid.getContainerSize(); i++) {
            if (!grid.isAnchorSlot(i)) {
                continue;
            }
            ItemStack stack = grid.getItem(i);
            int w = SpatialCargoItem.widthOf(stack);
            int h = SpatialCargoItem.heightOf(stack);
            if (w > 1 || h > 1) {
                int col = grid.colOf(i);
                int row = grid.rowOf(i);
                int rx = this.leftPos + RocketMenu.CARGO_START_X + col * 18 - 1;
                int ry = this.topPos + RocketMenu.CARGO_START_Y + row * 18 - 1;
                int rw = w * 18;
                int rh = h * 18;
                graphics.fill(rx + 1, ry + 1, rx + rw - 1, ry + rh - 1, 0xFF3A4E63);
                graphics.renderOutline(rx, ry, rw, rh, 0xFFE6A23C);
                String badge = w + "x" + h;
                graphics.drawString(this.font, badge, rx + rw - this.font.width(badge) - 2, ry + rh - 9, 0xFFFFD27D, false);
            }
        }

        // Player inventory & hotbar slot outlines
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = this.leftPos + 8 + col * 18 - 1;
                int sy = this.topPos + 134 + row * 18 - 1;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF1F242E);
                graphics.renderOutline(sx, sy, 18, 18, 0xFF424B5B);
            }
        }
        for (int col = 0; col < 9; col++) {
            int sx = this.leftPos + 8 + col * 18 - 1;
            int sy = this.topPos + 192 - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF1F242E);
            graphics.renderOutline(sx, sy, 18, 18, 0xFF424B5B);
        }

        if (DrawableUtil.mouseIn(mouseX, mouseY, fuelX - 1, fuelY - 1, fuelW + 2, fuelH + 2)) {
            RenderSystem.disableDepthTest();
            graphics.fill(fuelX, fuelY, fuelX + fuelW, fuelY + fuelH, 0x80ffffff);
            RenderSystem.enableDepthTest();

            List<Component> list = new ArrayList<>();
            DisplayUtil.createFluidTooltip(list, GCFluids.FUEL, null, amount, capacity);
            this.setTooltipForNextRenderPass(Lists.transform(list, Component::getVisualOrderText));
        }
    }
}
