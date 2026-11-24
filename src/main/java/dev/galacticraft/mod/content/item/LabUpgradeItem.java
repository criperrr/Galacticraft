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

package dev.galacticraft.mod.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Bench-specific laboratory upgrade module. Each upgrade belongs exclusively to one
 * planetary laboratory bench and slot role, modifying its interactive scientific mechanic.
 */
public class LabUpgradeItem extends Item {
    private final String targetLab;
    private final String slotRole;
    private final String effectSummary;

    public LabUpgradeItem(Properties properties, @NotNull String targetLab, @NotNull String slotRole, @NotNull String effectSummary) {
        super(properties.stacksTo(1));
        this.targetLab = targetLab;
        this.slotRole = slotRole;
        this.effectSummary = effectSummary;
    }

    public @NotNull String getTargetLab() {
        return this.targetLab;
    }

    public @NotNull String getSlotRole() {
        return this.slotRole;
    }

    public @NotNull String getEffectSummary() {
        return this.effectSummary;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Lab: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(this.targetLab).withStyle(ChatFormatting.AQUA)));
        tooltip.add(Component.literal("Bay: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(this.slotRole).withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.literal(this.effectSummary).withStyle(ChatFormatting.GREEN));
    }
}
