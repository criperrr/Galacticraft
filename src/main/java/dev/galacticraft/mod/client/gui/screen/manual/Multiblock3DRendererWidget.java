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

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.galacticraft.mod.manual.ManualPage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ponder-style interactive 3D isometric multiblock preview widget.
 * Supports mouse-drag rotation, layer-by-layer inspection ({@code [-]}, {@code [+]}, {@code All}),
 * material legend counts, and Sodium/Iris-safe {@code buffer.endBatch()} + {@code Lighting.setupForFlatItems()}.
 */
@Environment(EnvType.CLIENT)
public final class Multiblock3DRendererWidget {
    private final ManualPage.Multiblock3DPage page;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final Map<ResourceLocation, Integer> blockCounts = new LinkedHashMap<>();
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    private float yawDeg = 35.0F;
    private float pitchDeg = 28.0F;
    private int maxVisibleLayer;
    private boolean dragging = false;

    private int lastX;
    private int lastY;
    private int lastW;
    private int lastH;

    public Multiblock3DRendererWidget(@NotNull ManualPage.Multiblock3DPage page) {
        this.page = page;
        int maxX = 1;
        int maxZ = 1;
        int layers = page.layers().size();
        for (int y = 0; y < layers; y++) {
            List<String> rows = page.layers().get(y);
            maxZ = Math.max(maxZ, rows.size());
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                maxX = Math.max(maxX, row.length());
                for (int x = 0; x < row.length(); x++) {
                    String key = String.valueOf(row.charAt(x));
                    if (key.isBlank()) {
                        continue;
                    }
                    ResourceLocation blockId = page.palette().get(key);
                    if (blockId == null) {
                        continue;
                    }
                    Block block = BuiltInRegistries.BLOCK.get(blockId);
                    if (block != Blocks.AIR) {
                        this.blocks.put(new BlockPos(x, y, z), block.defaultBlockState());
                        this.blockCounts.merge(blockId, 1, Integer::sum);
                    }
                }
            }
        }
        this.sizeX = maxX;
        this.sizeY = Math.max(1, layers);
        this.sizeZ = maxZ;
        this.maxVisibleLayer = this.sizeY - 1;
    }

    public int preferredHeight() {
        int legendRows = Math.max(1, (this.blockCounts.size() + 1) / 2);
        return 132 + legendRows * 16;
    }

    public @NotNull List<ResourceLocation> materialBlockIds() {
        return new ArrayList<>(this.blockCounts.keySet());
    }

    public @NotNull List<String> materialSummaryLines() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> e : this.blockCounts.entrySet()) {
            Block b = BuiltInRegistries.BLOCK.get(e.getKey());
            lines.add(e.getValue() + "x " + b.getName().getString());
        }
        return lines;
    }

    public void render(@NotNull GuiGraphics graphics, @NotNull Font font, int x, int y, int w, int mouseX, int mouseY) {
        int h = preferredHeight();
        this.lastX = x;
        this.lastY = y;
        this.lastW = w;
        this.lastH = h;

        graphics.fill(x, y, x + w, y + h, 0xCC0D1829);
        graphics.fill(x, y, x + w, y + 1, 0xFF10B981);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF1E3A5F);

        Component title = Component.translatable(this.page.titleKey());
        graphics.drawString(font, title, x + 6, y + 5, 0xFF34D399, false);

        // Layer control buttons in top-right
        int btnY = y + 3;
        drawMiniButton(graphics, font, x + w - 82, btnY, 18, 12, "-", mouseX, mouseY);
        drawMiniButton(graphics, font, x + w - 62, btnY, 18, 12, "+", mouseX, mouseY);
        drawMiniButton(graphics, font, x + w - 42, btnY, 36, 12,
                this.maxVisibleLayer >= this.sizeY - 1 ? "ALL" : ("Y:" + (this.maxVisibleLayer + 1) + "/" + this.sizeY),
                mouseX, mouseY);

        // 3D viewport stage
        int stageX = x + 6;
        int stageY = y + 19;
        int stageW = w - 12;
        int stageH = 92;
        graphics.fill(stageX, stageY, stageX + stageW, stageY + stageH, 0xAA070E1A);
        graphics.renderOutline(stageX, stageY, stageW, stageH, 0xFF1D4E5E);

        int maxDim = Math.max(this.sizeX, Math.max(this.sizeY, this.sizeZ));
        float scale = Math.min(20.0F, 56.0F / Math.max(1.5F, maxDim));
        renderMultiblock3D(graphics, stageX + stageW / 2, stageY + stageH / 2 + 8, scale);

        String dragHint = Component.translatable("manual.galacticraft.multiblock.drag_hint").getString();
        graphics.drawString(font, dragHint, stageX + 4, stageY + stageH - 10, 0xFF64748B, false);

        // Caption
        int cursorY = stageY + stageH + 4;
        if (!this.page.captionKey().isEmpty()) {
            String cap = font.plainSubstrByWidth(Component.translatable(this.page.captionKey()).getString(), w - 12);
            graphics.drawString(font, cap, x + 6, cursorY, 0xFFCBD5E1, false);
            cursorY += 12;
        }

        // Material legend (2 columns)
        int colW = (w - 14) / 2;
        int idx = 0;
        for (Map.Entry<ResourceLocation, Integer> e : this.blockCounts.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(e.getKey());
            ItemStack icon = new ItemStack(block.asItem());
            int col = idx % 2;
            int row = idx / 2;
            int lx = x + 6 + col * colW;
            int ly = cursorY + row * 15;
            if (!icon.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(lx, ly, 0);
                graphics.pose().scale(0.75F, 0.75F, 1.0F);
                graphics.renderFakeItem(icon, 0, 0);
                graphics.pose().popPose();
            }
            String label = e.getValue() + "x " + block.getName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(label, colW - 18), lx + 15, ly + 2, 0xFFE2E8F0, false);
            idx++;
        }
    }

    private void renderMultiblock3D(@NotNull GuiGraphics graphics, int centerX, int centerY, float scale) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 150.0F);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(this.pitchDeg));
        pose.mulPose(Axis.YP.rotationDegrees(this.yawDeg));

        Lighting.setupFor3DItems();
        MultiBufferSource.BufferSource buffer = graphics.bufferSource();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        float cx = (this.sizeX - 1) * 0.5F;
        float cy = (this.sizeY - 1) * 0.5F;
        float cz = (this.sizeZ - 1) * 0.5F;

        for (Map.Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() > this.maxVisibleLayer) {
                continue;
            }
            pose.pushPose();
            pose.translate(pos.getX() - cx - 0.5F, pos.getY() - cy - 0.5F, pos.getZ() - cz - 0.5F);
            dispatcher.renderSingleBlock(
                    entry.getValue(),
                    pose,
                    buffer,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY
            );
            pose.popPose();
        }

        buffer.endBatch();
        Lighting.setupForFlatItems();
        pose.popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int btnY = this.lastY + 3;
        if (inRect(mouseX, mouseY, this.lastX + this.lastW - 82, btnY, 18, 12)) {
            this.maxVisibleLayer = Math.max(0, this.maxVisibleLayer - 1);
            return true;
        }
        if (inRect(mouseX, mouseY, this.lastX + this.lastW - 62, btnY, 18, 12)) {
            this.maxVisibleLayer = Math.min(this.sizeY - 1, this.maxVisibleLayer + 1);
            return true;
        }
        if (inRect(mouseX, mouseY, this.lastX + this.lastW - 42, btnY, 36, 12)) {
            this.maxVisibleLayer = (this.maxVisibleLayer >= this.sizeY - 1) ? 0 : (this.sizeY - 1);
            return true;
        }
        if (inRect(mouseX, mouseY, this.lastX + 6, this.lastY + 19, this.lastW - 12, 92)) {
            this.dragging = true;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double dragX, double dragY) {
        if (!this.dragging) {
            return false;
        }
        this.yawDeg = (this.yawDeg + (float) dragX * 0.9F) % 360.0F;
        this.pitchDeg = Mth.clamp(this.pitchDeg + (float) dragY * 0.65F, -65.0F, 65.0F);
        return true;
    }

    public void mouseReleased() {
        this.dragging = false;
    }

    private static void drawMiniButton(GuiGraphics g, Font font, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hovered = inRect(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hovered ? 0xFF1E3A5F : 0xFF132238);
        g.renderOutline(x, y, w, h, hovered ? 0xFF38BDF8 : 0xFF2B4C73);
        int tw = font.width(label);
        g.drawString(font, label, x + (w - tw) / 2, y + 2, hovered ? 0xFFFFFFFF : 0xFFCBD5E1, false);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
