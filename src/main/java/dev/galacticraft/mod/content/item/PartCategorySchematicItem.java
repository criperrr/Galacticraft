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

import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Reusable general-category schematic inserted into the Rocket Part Builder to reconfigure the
 * crafting grid layout and unlock parametric fabrication for a {@link RocketPartCategory}.
 */
public class PartCategorySchematicItem extends Item {
    private final RocketPartCategory category;

    public PartCategorySchematicItem(@NotNull RocketPartCategory category, Properties properties) {
        super(properties);
        this.category = Objects.requireNonNull(category, "category");
    }

    public @NotNull RocketPartCategory getCategory() {
        return this.category;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Category: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(this.category.displayName()).withStyle(ChatFormatting.AQUA)));
        tooltip.add(Component.literal("Reconfigures the Rocket Part Builder grid for "
                + this.category.displayName().toLowerCase(Locale.ROOT) + " parts.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Reusable blueprint (not consumed on craft).")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
