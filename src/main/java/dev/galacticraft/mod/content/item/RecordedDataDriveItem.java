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

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.component.TelemetryData;
import dev.galacticraft.mod.research.ResearchDomain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Recorded aerospace telemetry drive ({@code RECORDED_DATA_DRIVE}) carrying immutable
 * {@link GCDataComponents#TELEMETRY_DATA} captured by onboard {@code science_*} modules.
 * Fire-resistant so recovered {@code science_flight_recorder} black boxes survive crashes.
 */
public class RecordedDataDriveItem extends Item {
    public RecordedDataDriveItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        TelemetryData data = stack.get(GCDataComponents.TELEMETRY_DATA);
        return data != null && data.isThermalFailureBlackBox();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        TelemetryData data = stack.get(GCDataComponents.TELEMETRY_DATA);
        if (data == null) {
            tooltip.add(Component.literal("No telemetry payload encoded").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (data.isThermalFailureBlackBox()) {
            tooltip.add(Component.literal("BLACK BOX RECOVERY (+200% DISASTER DATA)").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        tooltip.add(Component.literal("Body: " + data.planetName() + " (" + data.planetId().getPath() + ")").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Event: " + data.eventType().displayName()).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal("Instrument: " + data.scienceModuleId()).withStyle(ChatFormatting.GRAY));
        ResearchDomain domain = ResearchDomain.byId(data.primaryDomainId());
        String domLabel = domain != null ? domain.shortCode() : data.primaryDomainId();
        ChatFormatting domColor = domain != null ? domain.chatColor() : ChatFormatting.GREEN;
        tooltip.add(Component.literal("Yield: +" + data.sciencePoints() + " SP | +" + data.domainPoints() + " " + domLabel
                + " (" + data.qualityPct() + "%)").withStyle(domColor));
        if (!data.gasCompositionSummary().isEmpty()) {
            tooltip.add(Component.literal("Atm: " + data.gasCompositionSummary()).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (!data.diagnosticNote().isEmpty()) {
            tooltip.add(Component.literal("Note: " + data.diagnosticNote()).withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.literal("Analyze in Telemetry Analyzer with planetary sample").withStyle(ChatFormatting.DARK_GRAY));
    }
}
