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
import dev.galacticraft.mod.content.block.entity.machine.SabatierChemicalReactorBlockEntity;
import dev.galacticraft.mod.screen.SabatierChemicalReactorMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Environment(EnvType.CLIENT)
public class SabatierChemicalReactorScreen extends MachineScreen<SabatierChemicalReactorBlockEntity, SabatierChemicalReactorMenu> {
    public SabatierChemicalReactorScreen(SabatierChemicalReactorMenu handler, Inventory inv, Component title) {
        super(handler, title, Constant.ScreenTexture.SABATIER_CHEMICAL_REACTOR_SCREEN);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.capacitorX = 8;
        this.capacitorY = 22;
        this.titleLabelY = 6;
        this.inventoryLabelY = 112;
    }

    @Override
    protected void drawTitle(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFF4E0, true);
    }

    @Override
    protected void renderMachineBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderMachineBackground(graphics, mouseX, mouseY, delta);

        long timeMs = System.currentTimeMillis();
        int temp = this.menu.getTemperatureC();
        int targetTemp = this.menu.getTargetTempC();
        int progress = this.menu.getProgress();
        int maxProgress = this.menu.getMaxProgress();
        int catTier = this.menu.getCatalystTier();
        int reactionCode = this.menu.getActiveReactionCode();
        boolean hot = temp >= targetTemp;
        boolean reacting = reactionCode != SabatierChemicalReactorBlockEntity.REACTION_STANDBY && hot;

        // Right-header telemetry LED
        int ledColor = reacting
                ? (((timeMs / 280L) & 1L) == 0L ? 0xFFFFB74D : 0xFFFF7043)
                : (temp > 60 ? 0xFFD97B2B : 0xFF4A5568);
        graphics.fill(this.leftPos + 166, this.topPos + 7, this.leftPos + 169, this.topPos + 14, ledColor);

        // Catalyst speed multiplier badge beside CATALYST_SLOT (x=97, y=23..39)
        int badgeX = this.leftPos + 97;
        int badgeY = this.topPos + 23;
        graphics.fill(badgeX, badgeY, badgeX + 26, badgeY + 16, 0xFF141B26);
        graphics.renderOutline(badgeX, badgeY, 26, 16,
                catTier == SabatierChemicalReactorBlockEntity.CATALYST_PALLADIUM ? 0xFFFFC94A
                        : (catTier == SabatierChemicalReactorBlockEntity.CATALYST_NIOBIUM ? 0xFF5CC8FF : 0xFF485262));
        String catLabel = switch (catTier) {
            case SabatierChemicalReactorBlockEntity.CATALYST_PALLADIUM -> "Pd1.5";
            case SabatierChemicalReactorBlockEntity.CATALYST_NIOBIUM -> "Nb1.0";
            default -> "CAT";
        };
        int catColor = switch (catTier) {
            case SabatierChemicalReactorBlockEntity.CATALYST_PALLADIUM -> 0xFFFFD866;
            case SabatierChemicalReactorBlockEntity.CATALYST_NIOBIUM -> 0xFF8BE2FF;
            default -> 0xFF7A8598;
        };
        graphics.drawString(this.font, catLabel, badgeX + 13 - this.font.width(catLabel) / 2, badgeY + 4, catColor, false);

        // Central Catalytic Reaction Core OLED (x=101..120, y=44..68)
        int coreX = this.leftPos + 101;
        int coreY = this.topPos + 44;
        int coreW = 19;
        int coreH = 24;
        float heatRatio = Mth.clamp((float) (temp - SabatierChemicalReactorBlockEntity.AMBIENT_TEMP_C)
                / (float) (SabatierChemicalReactorBlockEntity.MAX_TEMP_C - SabatierChemicalReactorBlockEntity.AMBIENT_TEMP_C), 0.0F, 1.0F);
        int coilColor = lerpThermalColor(heatRatio);

        // 4 catalytic heater coils glowing inside the reactor bed
        for (int i = 0; i < 4; i++) {
            int cy = coreY + 3 + i * 5;
            graphics.fill(coreX + 2, cy, coreX + coreW - 2, cy + 2, coilColor);
            if (reacting) {
                int pulseX = coreX + 2 + (int) (((timeMs / 65L) + i * 3L) % (coreW - 6));
                graphics.fill(pulseX, cy, pulseX + 3, cy + 2, 0xFFFFF5C0);
            }
        }

        // Reaction progress fill overlay along bottom of core OLED
        int progFill = Mth.clamp((int) ((float) progress / (float) maxProgress * (coreW - 2)), 0, coreW - 2);
        if (progFill > 0) {
            graphics.fill(coreX + 1, coreY + coreH - 3, coreX + 1 + progFill, coreY + coreH - 1, 0xFF42F0C8);
        }

        // Thermal gauge bar (x=75, y=75, w=47, h=5)
        int tBarX = this.leftPos + 75;
        int tBarY = this.topPos + 75;
        int tBarW = 47;
        int tBarH = 5;
        int tFill = Mth.clamp((int) (heatRatio * tBarW), 0, tBarW);
        if (tFill > 0) {
            graphics.fillGradient(tBarX, tBarY, tBarX + tFill, tBarY + tBarH, 0xFFFFB74D, 0xFFE64A19);
        }
        // Target temperature tick marker
        int targetTick = tBarX + Mth.clamp((int) ((float) (targetTemp - 20) / (float) (SabatierChemicalReactorBlockEntity.MAX_TEMP_C - 20) * tBarW), 2, tBarW - 2);
        graphics.fill(targetTick, tBarY - 1, targetTick + 1, tBarY + tBarH + 1, 0xFFEAF6FF);

        // Temperature numeric readout
        String tempStr = temp + "°C";
        int tempTextColor = hot ? 0xFFC65102 : 0xFF525A66;
        graphics.drawString(this.font, tempStr, this.leftPos + 98 - this.font.width(tempStr) / 2, this.topPos + 84, tempTextColor, false);

        // Bottom obsidian OLED chemical formula bar
        int lcdY = this.topPos + 100;
        String equation = getEquationText(reactionCode);
        int eqColor = reacting ? 0xFF5CF2C8 : (reactionCode != 0 ? 0xFFFFBA5A : 0xFF6B7B90);
        graphics.drawString(this.font, equation, this.leftPos + 88 - this.font.width(equation) / 2, lcdY, eqColor, false);

        // Rich hover tooltip on reactor core / thermal bar
        if (mouseIn(mouseX, mouseY, this.leftPos + 96, this.topPos + 42, 27, 42)) {
            this.setTooltipForNextRenderPass(List.of(
                    Component.literal("Catalytic Sabatier Reactor Bed").withStyle(ChatFormatting.GOLD).getVisualOrderText(),
                    Component.literal("Bed Temp: " + temp + "°C / " + targetTemp + "°C").withStyle(hot ? ChatFormatting.GREEN : ChatFormatting.YELLOW).getVisualOrderText(),
                    Component.literal("Catalyst: " + (catTier == 2 ? "Palladium (1.5x Speed)" : (catTier == 1 ? "Niobium (1.0x)" : "None"))).withStyle(ChatFormatting.AQUA).getVisualOrderText(),
                    Component.literal("Cycle: " + equation + " | 110 GJ/t").withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ));
        }
    }

    private static int lerpThermalColor(float t) {
        int r = (int) Mth.lerp(t, 68, 255);
        int g = (int) Mth.lerp(t, 76, 152);
        int b = (int) Mth.lerp(t, 90, 36);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private static String getEquationText(int reactionCode) {
        return switch (reactionCode) {
            case SabatierChemicalReactorBlockEntity.REACTION_METHALOX -> "CO2 + 4H2 -> CH4 + 2H2O";
            case SabatierChemicalReactorBlockEntity.REACTION_HYDRAZINE -> "N2 + 2H2 -> N2H4 (MONO)";
            case SabatierChemicalReactorBlockEntity.REACTION_SULFURIC -> "S + H2O -> H2SO4";
            case SabatierChemicalReactorBlockEntity.REACTION_XENON -> "Xe + LOX -> LXe + O2";
            default -> "STANDBY - INSERT REAGENTS";
        };
    }
}
