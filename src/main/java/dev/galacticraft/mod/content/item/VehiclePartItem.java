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

import dev.galacticraft.api.vehicle.VehiclePartDefinition;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Craftable modular vehicle component installed directly onto a chassis hardpoint in the world.
 */
public class VehiclePartItem extends Item {
    private final ResourceLocation partId;

    public VehiclePartItem(@NotNull ResourceLocation partId, Properties properties) {
        super(properties.stacksTo(16));
        this.partId = partId;
    }

    public @NotNull ResourceLocation getPartId() {
        return this.partId;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        VehiclePartDefinition part = VehicleRegistry.getPart(this.partId);
        if (part == null) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.galacticraft.vehicle_part.type", part.type().getSerializedName())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.galacticraft.vehicle_part.size", part.sizeClass().getSerializedName())
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.galacticraft.vehicle_crate.mass", Math.round(part.massKg()))
                .withStyle(ChatFormatting.GRAY));
    }
}
