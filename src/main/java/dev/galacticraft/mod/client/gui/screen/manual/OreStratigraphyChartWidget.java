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
import dev.galacticraft.mod.manual.PlanetarySurveyPrecision;
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
 * Planetary Atlas &amp; Ore Stratigraphy Chart widget displaying progressive probable ranges
 * ({@link PlanetarySurveyPrecision}) that tighten as the player observes the planet with the
 * Observatory Telescope, sends science probes, and runs Geological / Environmental lab assays.
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
        return 116 + this.page.ores().size() * 20;
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
        PlanetarySurveyPrecision precision = PlanetarySurveyPrecision.evaluateLive(this.page.bodyId());
        int badgeColor = precision.badgeColor();

        graphics.fill(x, y, x + w, y + h, 0xCC0D1829);
        graphics.fill(x, y, x + w, y + 1, 0xFFA855F7);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF1E3A5F);

        // Header + live precision tier badge
        String header = Component.translatable(this.page.titleKey()).getString();
        String badge = Component.translatable(precision.statusKey(), precision.precisionPct()).getString();
        int badgeW = font.width(badge);
        graphics.drawString(font, font.plainSubstrByWidth(header, Math.max(40, w - badgeW - 18)), x + 6, y + 5, 0xFFD8B4FE, false);
        graphics.drawString(font, badge, x + w - badgeW - 6, y + 5, badgeColor, false);

        // Precision progress bar + next-machine instruction
        int barY = y + 16;
        int barW = w - 12;
        graphics.fill(x + 6, barY, x + 6 + barW, barY + 4, 0xFF0A1220);
        int fillW = Math.round(barW * (precision.precisionPct() / 100.0F));
        graphics.fill(x + 6, barY, x + 6 + fillW, barY + 4, badgeColor);

        String nextHint = Component.translatable(precision.nextMachineHintKey()).getString();
        graphics.drawString(font, font.plainSubstrByWidth(nextHint, w - 12), x + 6, y + 23,
                precision.isExact() ? 0xFF86EFAC : 0xFF93C5FD, false);

        // Progressive probable-range astrophysical parameters
        String row1 = "G: " + precision.formatGravity(this.env)
                + " | P: " + precision.formatPressure(this.env)
                + " | Solar: " + precision.formatSolar(this.env);
        String acidNote = this.env.corrosiveAcid()
                ? (precision.hazardsConfirmed() ? " | H2SO4!" : " | Acid?")
                : "";
        String row2 = "Temp: " + precision.formatTemperatureC(this.env)
                + " | dV: " + precision.formatDeltaV(this.env) + acidNote;

        graphics.drawString(font, font.plainSubstrByWidth(row1, w - 12), x + 6, y + 36, 0xFFE2E8F0, false);
        graphics.drawString(font, font.plainSubstrByWidth(row2, w - 12), x + 6, y + 47,
                this.env.corrosiveAcid() && precision.hazardsConfirmed() ? 0xFFFBBF24 : 0xFF94A3B8, false);

        // Hazards line (progressive disclosure)
        String hazardsLine;
        if (precision.hazardsConfirmed() && !this.page.hazardsKey().isEmpty()) {
            hazardsLine = Component.translatable(this.page.hazardsKey()).getString();
        } else if (precision.atmosphereConfirmed()) {
            hazardsLine = Component.translatable("manual.galacticraft.precision.hazards.partial").getString();
        } else {
            hazardsLine = Component.translatable("manual.galacticraft.precision.hazards.unknown").getString();
        }
        graphics.drawString(font, font.plainSubstrByWidth(hazardsLine, w - 12), x + 6, y + 59,
                precision.hazardsConfirmed() ? 0xFFFCA5A5 : 0xFFF59E0B, false);

        // Depth axis header (-64 -> +128)
        int chartX = x + 84;
        int chartW = Math.max(60, w - 92);
        int axisY = y + 75;
        String stratLabel = precision.isExact() ? "Y Stratigraphy" : "Y Est. (±" + precision.oreYUncertainty() + ")";
        graphics.drawString(font, stratLabel, x + 6, axisY, 0xFF7DD3FC, false);
        graphics.drawString(font, "-64", chartX, axisY, 0xFF64748B, false);
        graphics.drawString(font, "0", chartX + mapYToX(0, chartW) - 3, axisY, 0xFF64748B, false);
        graphics.drawString(font, "+64", chartX + mapYToX(64, chartW) - 8, axisY, 0xFF64748B, false);
        graphics.drawString(font, "+128", chartX + chartW - font.width("+128"), axisY, 0xFF64748B, false);

        int rowsStartY = axisY + 12;
        int zeroX = chartX + mapYToX(0, chartW);
        int totalRowsH = Math.max(12, this.page.ores().size() * 20);
        graphics.fill(zeroX, rowsStartY, zeroX + 1, rowsStartY + totalRowsH, 0x5538BDF8);

        int yUncert = precision.oreYUncertainty();
        for (int i = 0; i < this.page.ores().size(); i++) {
            ManualPage.PlanetStratigraphyPage.OreBand band = this.page.ores().get(i);
            int ry = rowsStartY + i * 20;

            boolean identified = precision.oresIdentified() || i == 0;
            Item item = BuiltInRegistries.ITEM.get(band.oreItem());
            if (identified && item != Items.AIR) {
                graphics.pose().pushPose();
                graphics.pose().translate(x + 5, ry, 0);
                graphics.pose().scale(0.8F, 0.8F, 1.0F);
                graphics.renderFakeItem(new ItemStack(item), 0, 0);
                graphics.pose().popPose();
            } else {
                graphics.drawString(font, "?", x + 9, ry + 3, 0xFFF59E0B, false);
            }

            String oreName = identified
                    ? Component.translatable(band.labelKey()).getString()
                    : Component.translatable("manual.galacticraft.precision.unknown_mineral").getString();
            graphics.drawString(font, font.plainSubstrByWidth(oreName, 60), x + 21, ry + 3,
                    identified ? 0xFFE2E8F0 : 0xFF94A3B8, false);

            // Background rail
            graphics.fill(chartX, ry + 2, chartX + chartW, ry + 13, 0xFF0A1220);

            int estMinY = Math.max(MIN_WORLD_Y, band.minY() - yUncert);
            int estMaxY = Math.min(MAX_WORLD_Y, band.maxY() + yUncert);
            int ux1 = chartX + mapYToX(estMinY, chartW);
            int ux2 = chartX + Math.max(ux1 - chartX + 4, mapYToX(estMaxY, chartW));

            // Translucent outer uncertainty band when not yet 100% verified
            if (yUncert > 0) {
                int softColor = (0x44 << 24) | (band.color() & 0x00FFFFFF);
                graphics.fill(ux1, ry + 3, ux2, ry + 12, softColor);
            }

            int bx1 = chartX + mapYToX(band.minY(), chartW);
            int bx2 = chartX + Math.max(bx1 - chartX + 4, mapYToX(band.maxY(), chartW));
            int coreAlpha = precision.isExact() ? 0xDD : (precision.tier() == PlanetarySurveyPrecision.TIER_THEORETICAL ? 0x66 : 0xAA);
            int fillColor = (coreAlpha << 24) | (band.color() & 0x00FFFFFF);
            graphics.fill(bx1, ry + 3, bx2, ry + 12, fillColor);
            graphics.renderOutline(ux1, ry + 3, Math.max(4, ux2 - ux1), 9,
                    (precision.isExact() ? 0xFF000000 : 0x88000000) | (band.color() & 0x00FFFFFF));

            String rarity = identified ? precision.formatOreRarity(band.rarityMultiplier()) : "?x";
            int rarityW = font.width(rarity);
            int labelX = (ux2 + 3 + rarityW <= chartX + chartW) ? (ux2 + 3) : Math.max(chartX + 2, ux1 + 2);
            graphics.drawString(font, rarity, labelX, ry + 4, 0xFFFFFFFF, false);

            if (outTooltip != null && mouseX >= x + 4 && mouseX <= x + w - 4 && mouseY >= ry && mouseY < ry + 18) {
                outTooltip.add(Component.literal(oreName).withColor(identified ? band.color() : 0xFFF59E0B));
                if (precision.isExact()) {
                    outTooltip.add(Component.literal(String.format(Locale.ROOT,
                            "Verified Y: %d .. %d | Abundance: %.2fx",
                            band.minY(), band.maxY(), band.rarityMultiplier())));
                } else {
                    outTooltip.add(Component.literal(String.format(Locale.ROOT,
                            "Probable Y: %d .. %d (±%d) | Est. Abundance: %s",
                            estMinY, estMaxY, yUncert, rarity)));
                    outTooltip.add(Component.translatable(precision.nextMachineHintKey()).withColor(0xFF7DD3FC));
                }
            }
        }
    }

    private static int mapYToX(int yVal, int width) {
        float frac = (float) (yVal - MIN_WORLD_Y) / (float) (MAX_WORLD_Y - MIN_WORLD_Y);
        return Math.round(Mth.clamp(frac, 0.0F, 1.0F) * width);
    }
}
