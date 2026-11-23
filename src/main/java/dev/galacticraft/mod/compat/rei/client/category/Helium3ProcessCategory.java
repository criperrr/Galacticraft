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

import dev.galacticraft.mod.compat.rei.common.display.Helium3ProcessDisplay;
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
public class Helium3ProcessCategory implements DisplayCategory<Helium3ProcessDisplay> {
    private final CategoryIdentifier<Helium3ProcessDisplay> id;
    private final ItemLike icon;
    private final Component title;

    public Helium3ProcessCategory(CategoryIdentifier<Helium3ProcessDisplay> id, ItemLike icon, Component title) {
        this.id = id;
        this.icon = icon;
        this.title = title;
    }

    @Override
    public CategoryIdentifier<? extends Helium3ProcessDisplay> getCategoryIdentifier() {
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
    public @NotNull List<Widget> setupDisplay(Helium3ProcessDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        // Summary header label
        widgets.add(Widgets.createLabel(new Point(bounds.getCenterX(), bounds.y + 6),
                Component.literal(display.getSummary())).noShadow().color(0xFF404040, 0xFFBBBBBB));

        int slotY = bounds.y + 24;
        int inputX = bounds.x + 10;
        List<EntryIngredient> inputs = display.getInputEntries();
        for (int i = 0; i < inputs.size(); i++) {
            widgets.add(Widgets.createSlot(new Point(inputX + i * 20, slotY)).markInput().entries(inputs.get(i)));
        }

        int arrowX = inputX + Math.max(1, inputs.size()) * 20 + 4;
        widgets.add(Widgets.createArrow(new Point(arrowX, slotY + 1)).animationDurationTicks(40));

        int outputX = arrowX + 28;
        List<EntryIngredient> outputs = display.getOutputEntries();
        for (int i = 0; i < outputs.size(); i++) {
            widgets.add(Widgets.createSlot(new Point(outputX + i * 20, slotY)).markOutput().entries(outputs.get(i)));
        }

        return widgets;
    }

    @Override
    public int getDisplayWidth(Helium3ProcessDisplay display) {
        return 176;
    }

    @Override
    public int getDisplayHeight() {
        return 52;
    }
}
