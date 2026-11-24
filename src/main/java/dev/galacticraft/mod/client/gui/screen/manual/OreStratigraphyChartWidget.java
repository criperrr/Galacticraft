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

import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.manual.ManualPage;
import dev.galacticraft.mod.research.ClientResearchCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Planetary Atlas &amp; Ore Stratigraphy Chart widget displaying live astrophysical parameters
 * from {@link PlanetaryEnvironment} alongside visual Y-depth strata bars (-64 to +128)
 * and relative rarity multipliers ({@code 0.2x} to {@code 3.5x}).
 */
@Environment(EnvType.CLIENT)
public final class OreStratigraphyChartWidget {
    private static final int MIN_WORLD_Y = -64;
    private static final int MAX_WORLD_Y = 128;

    private final ManualPage.PlanetStratigraphyPage page;
    private final PlanetaryEnvironment env;

    public OreStratigraphyChartWidget(@NotNull ManualPage.PlanetStratigraphyPage page) {
        this.page = page;
        PlanetaryEnvironment resolved = PlanetaryEnvironment.byId(page.bodyId());
        this.env = resolved != null ? resolved : PlanetaryEnvironment.EARTH;
    }

    public int preferredHeight() {
        return 92 + this.page.ores().size() * 20;
    }

    public void render(
            @NotNull GuiGraphics graphics,
            @NotNull Font font,
            int x,
            int y,
            int w,
            int mouseX,
            int mouseY,
            @Nullable List<Component> outTooltip
    ) {
        int h = preferredHeight();
        boolean mapped = ClientResearchCache.isBodyMapped(this.page.bodyId())
                || this.env == PlanetaryEnvironment.EARTH
                || this.env == PlanetaryEnvironment.MOON;

        graphics.fill(x, y, x + w, y + h, 0xCC0D1829);
        graphics.fill(x, y, x + w, y + 1, 0xFFA855F7);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF1E3A5F);

        // Header + live telemetry badge
        String header = Component.translatable(this.page.titleKey()).getString();
        graphics.drawString(font, header, x + 6, y + 5, 0xFFD8B4FE, false);

        String scanBadge = mapped
                ? Component.translatable("manual.galacticraft.atlas.surveyed").getString()
                : Component.translatable("manual.galacticraft.atlas.unmapped").getString();
        int badgeW = font.width(scanBadge);
        graphics.drawString(font, scanBadge, x + w - badgeW - 6, y + 5, mapped ? 0xFF4ADE80 : 0xFFF59E0B, false);

        // Live physical parameters grid (2 rows)
        int dayC = (int) Math.round(this.env.dayTempKelvin() - PlanetaryEnvironment.KELVIN_OFFSET);
        int nightC = (int) Math.round(this.env.nightTempKelvin() - PlanetaryEnvironment.KELVIN_OFFSET);
        String row1 = String.format(Locale.ROOT, "G: %.2fg  |  P: %.3f atm  |  Solar: %.3fx",
                this.env.gravityG(), this.env.pressureAtm(), this.env.solarIrradianceFactor());
        String row2 = String.format(Locale.ROOT, "Temp: %d°C / %d°C  |  dV Orbit: %.0f m/s%s",
                dayC, nightC, this.env.orbitAscentDeltaV(), this.env.corrosiveAcid() ? "  |  H2SO4!" : "");
        graphics.drawString(font, font.plainSubstrByWidth(row1, w - 12), x + 6, y + 18, 0xFFE2E8F0, false);
        graphics.drawString(font, font.plainSubstrByWidth(row2, w - 12), x + 6, y + 29,
                this.env.corrosiveAcid() ? 0xFFFBBF24 : 0xFF94A3B8, false);

        // Hazards line
        if (!this.page.hazardsKey().isEmpty()) {
            String haz = Component.translatable(this.page.hazardsKey()).getString();
            graphics.drawString(font, font.plainSubstrByWidth(haz, w - 12), x + 6, y + 41, 0xFFFCA5A5, false);
        }

        // Depth axis header (-64 -> +128)
        int chartX = x + 84;
        int chartW = Math.max(60, w - 92);
        int axisY = y + 55;
        graphics.drawString(font, "Y Stratigraphy", x + 6, axisY, 0xFF7DD3FC, false);
        graphics.drawString(font, "-64", chartX, axisY, 0xFF64748B, false);
        graphics.drawString(font, "0", chartX + mapYToX(0, chartW) - 3, axisY, 0xFF64748B, false);
        graphics.drawString(font, "+64", chartX + mapYToX(64, chartW) - 8, axisY, 0xFF64748B, false);
        graphics.drawString(font, "+128", chartX + chartW - font.width("+128"), axisY, 0xFF64748B, false);

        int rowsStartY = axisY + 12;
        int zeroX = chartX + mapYToX(0, chartW);
        int totalRowsH = Math.max(12, this.page.ores().size() * 20);
        graphics.fill(zeroX, rowsStartY, zeroX + 1, rowsStartY + totalRowsH, 0x5538BDF8);

        for (int i = 0; i < this.page.ores().size(); i++) {
            ManualPage.PlanetStratigraphyPage.OreBand band = this.page.ores().get(i);
            int ry = rowsStartY + i * 20;

            Item item = BuiltInRegistries.ITEM.get(band.oreItem());
            if (item != Items.AIR) {
                graphics.pose().pushPose();
                graphics.pose().translate(x + 5, ry, 0);
                graphics.pose().scale(0.8F, 0.8F, 1.0F);
                graphics.renderFakeItem(new ItemStack(item), 0, 0);
                graphics.pose().popPose();
            }

            String oreName = Component.translatable(band.labelKey()).getString();
            graphics.drawString(font, font.plainSubstrByWidth(oreName, 60), x + 21, ry + 3, 0xFFE2E8F0, false);

            // Background rail
            graphics.fill(chartX, ry + 2, chartX + chartW, ry + 13, 0xFF0A1220);
            int bx1 = chartX + mapYToX(band.minY(), chartW);
            int bx2 = chartX + Math.max(bx1 - chartX + 4, mapYToX(band.maxY(), chartW));
            int fillColor = (0xCC << 24) | (band.color() & 0x00FFFFFF);
            graphics.fill(bx1, ry + 3, bx2, ry + 12, fillColor);
            graphics.renderOutline(bx1, ry + 3, Math.max(4, bx2 - bx1), 9, 0xFF000000 | (band.color() & 0x00FFFFFF));

            String rarity = String.format(Locale.ROOT, "%.1fx", band.rarityMultiplier());
            graphics.drawString(font, rarity, bx2 + 3 <= chartX + chartW - 18 ? bx2 + 3 : bx1 + 2, ry + 4, 0xFFFFFFFF, false);

            if (outTooltip != null && mouseX >= x + 4 && mouseX <= x + w - 4 && mouseY >= ry && mouseY < ry + 18) {
                outTooltip.add(Component.literal(oreName).withColor(band.color()));
                outTooltip.add(Component.literal(String.format(Locale.ROOT, "Y: %d .. %d  |  Abundance: %.2fx",
                        band.minY(), band.maxY(), band.rarityMultiplier())));
            }
        }
    }

    private static int mapYToX(int yVal, int width) {
        float frac = (float) (yVal - MIN_WORLD_Y) / (float) (MAX_WORLD_Y - MIN_WORLD_Y);
        return Math.round(Mth.clamp(frac, 0.0F, 1.0F) * width);
    }
}
