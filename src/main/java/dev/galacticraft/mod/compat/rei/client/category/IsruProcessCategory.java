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

package dev.galacticraft.mod.compat.rei.client.category;

import dev.galacticraft.mod.compat.rei.common.GalacticraftREIServerPlugin;
import dev.galacticraft.mod.compat.rei.common.display.IsruProcessDisplay;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class IsruProcessCategory implements DisplayCategory<IsruProcessDisplay> {
    private final CategoryIdentifier<IsruProcessDisplay> id;
    private final ItemLike icon;
    private final Component title;

    public IsruProcessCategory(CategoryIdentifier<IsruProcessDisplay> id, ItemLike icon, Component title) {
        this.id = id;
        this.icon = icon;
        this.title = title;
    }

    @Override
    public CategoryIdentifier<? extends IsruProcessDisplay> getCategoryIdentifier() {
        return this.id;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(this.icon);
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public @NotNull List<Widget> setupDisplay(IsruProcessDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        int ox = bounds.x + 6;
        int oy = bounds.y + 8;

        List<EntryIngredient> inputs = display.getInputEntries();
        List<EntryIngredient> outputs = display.getOutputEntries();

        if (this.id.equals(GalacticraftREIServerPlugin.ELECTROLYTIC)) {
            if (!inputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 10, oy + 18)).markInput().entries(inputs.get(0)));
            }
            widgets.add(Widgets.createArrow(new Point(ox + 38, oy + 18)).animationDurationTicks(40));
            for (int i = 0; i < Math.min(outputs.size(), 3); i++) {
                widgets.add(Widgets.createSlot(new Point(ox + 74 + i * 24, oy + 18)).markOutput().entries(outputs.get(i)));
            }
            widgets.add(Widgets.createLabel(new Point(ox + 82, oy + 52),
                    Component.literal(display.getSummary())).noShadow().color(0xFF1D6C8A, 0xFF7BE8FF));
        } else {
            for (int i = 0; i < Math.min(inputs.size(), 3); i++) {
                widgets.add(Widgets.createSlot(new Point(ox + 4 + i * 21, oy + 18)).markInput().entries(inputs.get(i)));
            }
            widgets.add(Widgets.createArrow(new Point(ox + 72, oy + 18)).animationDurationTicks(45));
            for (int i = 0; i < Math.min(outputs.size(), 2); i++) {
                widgets.add(Widgets.createSlot(new Point(ox + 106 + i * 24, oy + 18)).markOutput().entries(outputs.get(i)));
            }
            widgets.add(Widgets.createLabel(new Point(ox + 82, oy + 52),
                    Component.literal(display.getSummary())).noShadow().color(0xFF1B7050, 0xFF66F2BA));
        }

        return widgets;
    }

    @Override
    public int getDisplayWidth(IsruProcessDisplay display) {
        return 176;
    }

    @Override
    public int getDisplayHeight() {
        return 76;
    }
}
