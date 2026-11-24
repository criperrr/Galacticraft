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

import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.machinelib.client.api.screen.MachineScreen;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.machine.AtmosphericFractionatorBlockEntity;
import dev.galacticraft.mod.screen.AtmosphericFractionatorMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Environment(EnvType.CLIENT)
public class AtmosphericFractionatorScreen extends MachineScreen<AtmosphericFractionatorBlockEntity, AtmosphericFractionatorMenu> {
    private static final int[] TANK_X = {80, 104, 128, 152};
    private static final int[] SPECTRUM_COLORS = {0xFF5CE0FF, 0xFF68F5B2, 0xFFF2C854, 0xFFBB84FF};

    public AtmosphericFractionatorScreen(AtmosphericFractionatorMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.ATMOSPHERIC_FRACTIONATOR_SCREEN);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.capacitorX = 8;
        this.capacitorY = 22;
        this.titleLabelY = 6;
        this.inventoryLabelY = 112;
    }

    @Override
    protected void drawTitle(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFEAFBF5, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        long timeMs = System.currentTimeMillis();
        int planetCode = this.menu.getPlanetCode();
        int pressureMilli = this.menu.getPressureMilliAtm();
        int intakePct = this.menu.getIntakePressurePct();
        boolean vacuum = planetCode == AtmosphericFractionatorBlockEntity.PLANET_VACUUM;
        boolean spinning = !vacuum && intakePct > 0;

        // Right-header telemetry LED
        int ledColor;
        if (vacuum) {
            ledColor = (((timeMs / 400L) & 1L) == 0L) ? 0xFFFF4242 : 0xFF8C1D1D;
        } else if (spinning) {
            ledColor = 0xFF4AF2B2;
        } else {
            ledColor = 0xFF4A5568;
        }
        graphics.fill(this.leftPos + 166, this.topPos + 7, this.leftPos + 169, this.topPos + 14, ledColor);

        // Planetary Spectrograph OLED (x=28..74, y=22..70)
        int oledX = this.leftPos + 31;
        int oledY = this.topPos + 24;
        String planetName = getPlanetShortName(planetCode);
        int nameColor = vacuum ? 0xFFFF5E5E : 0xFF6EF5C8;
        graphics.drawString(this.font, planetName, oledX + 2, oledY + 1, nameColor, false);

        String pressureStr = formatPressure(pressureMilli, vacuum);
        graphics.drawString(this.font, pressureStr, oledX + 2, oledY + 11, vacuum ? 0xFFD94A4A : 0xFF9AC2D8, false);

        // Animated radial intake turbine fan (centered at oledX + 11, oledY + 31)
        int cx = oledX + 11;
        int cy = oledY + 31;
        graphics.fill(cx - 7, cy - 7, cx + 8, cy + 8, 0xFF0A1018);
        graphics.renderOutline(cx - 7, cy - 7, 15, 15, vacuum ? 0xFF6E2424 : 0xFF28485E);
        double angle = spinning ? (timeMs / Math.max(45.0, 220.0 - intakePct * 1.6)) : 0.0;
        int bladeColor = vacuum ? 0xFF5A3232 : (spinning ? 0xFF7BE8FF : 0xFF56687A);
        for (int b = 0; b < 4; b++) {
            double a = angle + b * (Math.PI / 2.0);
            for (int r = 2; r <= 5; r++) {
                int px = cx + (int) Math.round(Math.cos(a) * r);
                int py = cy + (int) Math.round(Math.sin(a) * r);
                graphics.fill(px, py, px + 1, py + 1, bladeColor);
            }
        }
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, spinning ? 0xFF4AF2B2 : 0xFF4C5B6B);

        // Live 4-channel gas spectrum equalizer bars (oledX + 23 .. +40, oledY + 23..39)
        int eqBaseY = oledY + 39;
        for (int ch = 0; ch < 4; ch++) {
            int fluidId = this.menu.getChannelFluidId(ch);
            int bx = oledX + 22 + ch * 5;
            graphics.fill(bx, eqBaseY - 15, bx + 3, eqBaseY, 0xFF141E2C);
            if (!vacuum && fluidId >= 0) {
                int baseH = switch (ch) {
                    case 0 -> 13;
                    case 1 -> 9;
                    case 2 -> 6;
                    default -> 4;
                };
                int pulse = spinning ? (int) (Math.sin(timeMs / 180.0 + ch * 1.4) * 2.0) : 0;
                int barH = Mth.clamp(spinning ? baseH + pulse : Math.max(2, baseH / 2), 2, 15);
                graphics.fill(bx, eqBaseY - barH, bx + 3, eqBaseY, SPECTRUM_COLORS[ch]);
            }
        }

        // Intake compressor pressure bar (x=29, y=74, w=44, h=6)
        int barX = this.leftPos + 29;
        int barY = this.topPos + 74;
        int barW = 44;
        int barH = 6;
        int pFill = Mth.clamp((int) ((float) intakePct / 100.0F * barW), 0, barW);
        if (pFill > 0 && !vacuum) {
            graphics.fillGradient(barX, barY, barX + pFill, barY + barH, 0xFF5CF2B8, 0xFF1A9E8F);
        } else if (vacuum) {
            graphics.fill(barX, barY, barX + barW, barY + barH, 0x66A82424);
        }

        // Thin extraction cycle line below pressure bar
        int cycFill = Mth.clamp((int) ((float) this.menu.getProgress() / (float) AtmosphericFractionatorBlockEntity.CYCLE_TICKS * barW), 0, barW);
        if (cycFill > 0 && !vacuum) {
            graphics.fill(barX, barY + 7, barX + cycFill, barY + 8, 0xFF66E8FF);
        }

        // Planet bonus badge below bar
        String badge = getPlanetBadge(planetCode);
        int badgeColor = vacuum ? 0xFFC62828 : 0xFF146458;
        graphics.drawString(this.font, badge, this.leftPos + 51 - this.font.width(badge) / 2, this.topPos + 84, badgeColor, false);

        // Bottom obsidian OLED dynamic gas formula strip
        int lcdY = this.topPos + 100;
        if (vacuum) {
            String warn = "VACUUM - NO ATMOSPHERE";
            graphics.drawString(this.font, warn, this.leftPos + 88 - this.font.width(warn) / 2, lcdY, 0xFFFF5C5C, false);
        } else {
            graphics.drawString(this.font, "IN:" + intakePct + "%", this.leftPos + 14, lcdY, 0xFF6CF2C2, false);
            for (int ch = 0; ch < 4; ch++) {
                String sym = getFluidShortSymbol(this.menu.getChannelFluidId(ch));
                if (!sym.isEmpty()) {
                    int sx = this.leftPos + TANK_X[ch] + 8 - this.font.width(sym) / 2;
                    graphics.drawString(this.font, sym, sx, lcdY, SPECTRUM_COLORS[ch], false);
                }
            }
        }

        // Rich hover tooltip on atmospheric spectrograph OLED
        if (mouseIn(mouseX, mouseY, this.leftPos + 28, this.topPos + 22, 46, 58)) {
            if (vacuum) {
                this.setTooltipForNextRenderPass(List.of(
                        Component.literal("No Planetary Atmosphere").withStyle(ChatFormatting.RED).getVisualOrderText(),
                        Component.literal("Vacuum body detected (0.000 atm)").withStyle(ChatFormatting.GRAY).getVisualOrderText(),
                        Component.literal("Deploy on Earth, Mars, Venus, or Gas Giants").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText()
                ));
            } else {
                this.setTooltipForNextRenderPass(List.of(
                        Component.literal("Atmospheric Spectrograph: " + planetName).withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                        Component.literal("Ambient Pressure: " + formatPressureLong(pressureMilli)).withStyle(ChatFormatting.GRAY).getVisualOrderText(),
                        Component.literal("Turbine Intake: " + intakePct + "% | 100 GJ/t").withStyle(ChatFormatting.GREEN).getVisualOrderText()
                ));
            }
        }
    }

    private static String getPlanetShortName(int code) {
        return switch (code) {
            case AtmosphericFractionatorBlockEntity.PLANET_EARTH -> "EARTH";
            case AtmosphericFractionatorBlockEntity.PLANET_MARS -> "MARS";
            case AtmosphericFractionatorBlockEntity.PLANET_VENUS -> "VENUS";
            case AtmosphericFractionatorBlockEntity.PLANET_JUPITER -> "JUPITER";
            case AtmosphericFractionatorBlockEntity.PLANET_SATURN -> "SATURN";
            case AtmosphericFractionatorBlockEntity.PLANET_URANUS -> "URANUS";
            case AtmosphericFractionatorBlockEntity.PLANET_NEPTUNE -> "NEPTUNE";
            case AtmosphericFractionatorBlockEntity.PLANET_EXO -> "EXO-ATM";
            default -> "VACUUM";
        };
    }

    private static String getPlanetBadge(int code) {
        return switch (code) {
            case AtmosphericFractionatorBlockEntity.PLANET_EARTH -> "N2/O2";
            case AtmosphericFractionatorBlockEntity.PLANET_MARS -> "95% CO2";
            case AtmosphericFractionatorBlockEntity.PLANET_VENUS -> "H2SO4+";
            case AtmosphericFractionatorBlockEntity.PLANET_JUPITER -> "H2/3He";
            case AtmosphericFractionatorBlockEntity.PLANET_SATURN -> "3.5x 3He";
            case AtmosphericFractionatorBlockEntity.PLANET_URANUS -> "CH4/3He";
            case AtmosphericFractionatorBlockEntity.PLANET_NEPTUNE -> "Xe MAX";
            case AtmosphericFractionatorBlockEntity.PLANET_EXO -> "ACTIVE";
            default -> "NO ATM";
        };
    }

    private static String formatPressure(int milliAtm, boolean vacuum) {
        if (vacuum || milliAtm <= 0) {
            return "0.00a";
        }
        if (milliAtm >= 10_000) {
            return String.format("%.1fa", milliAtm / 1000.0F);
        }
        if (milliAtm >= 1_000) {
            return String.format("%.2fa", milliAtm / 1000.0F);
        }
        return String.format("%.3fa", milliAtm / 1000.0F);
    }

    private static String formatPressureLong(int milliAtm) {
        return String.format("%.3f atm", milliAtm / 1000.0F);
    }

    private static String getFluidShortSymbol(int fluidId) {
        if (fluidId < 0) {
            return "";
        }
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        if (fluid == Fluids.EMPTY) {
            return "";
        }
        if (fluid == Gases.CARBON_DIOXIDE) return "CO2";
        if (fluid == Gases.NITROGEN) return "N2";
        if (fluid == Gases.OXYGEN) return "O2";
        if (fluid == Gases.ARGON) return "Ar";
        if (fluid == Gases.HYDROGEN) return "H2";
        if (fluid == Gases.HELIUM_3) return "3He";
        if (fluid == Gases.HELIUM) return "He";
        if (fluid == Gases.METHANE) return "CH4";
        if (fluid == Gases.XENON) return "Xe";
        if (fluid == Gases.CARBON_MONOXIDE) return "CO";
        if (fluid == GCFluids.SULFURIC_ACID) return "H2SO4";
        return "GAS";
    }
}
