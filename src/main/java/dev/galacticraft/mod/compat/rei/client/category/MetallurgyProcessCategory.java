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

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.compat.rei.common.GalacticraftREIServerPlugin;
import dev.galacticraft.mod.compat.rei.common.display.MetallurgyProcessDisplay;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class MetallurgyProcessCategory implements DisplayCategory<MetallurgyProcessDisplay> {
    private final CategoryIdentifier<MetallurgyProcessDisplay> id;
    private final ItemLike icon;
    private final Component title;

    public MetallurgyProcessCategory(CategoryIdentifier<MetallurgyProcessDisplay> id, ItemLike icon, Component title) {
        this.id = id;
        this.icon = icon;
        this.title = title;
    }

    @Override
    public CategoryIdentifier<? extends MetallurgyProcessDisplay> getCategoryIdentifier() {
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
    public @NotNull List<Widget> setupDisplay(MetallurgyProcessDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        int ox = bounds.x + 4;
        int oy = bounds.y + 4;

        widgets.add(Widgets.createTexturedWidget(this.getMachineTexture(), ox, oy, 4, 18, 168, 82));

        List<EntryIngredient> inputs = display.getInputEntries();
        List<EntryIngredient> outputs = display.getOutputEntries();

        if (this.id.equals(GalacticraftREIServerPlugin.METALLURGIC_INFUSING)) {
            if (!inputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 30, oy + 24)).disableBackground().markInput().entries(inputs.get(0)));
            }
            if (inputs.size() > 1) {
                widgets.add(Widgets.createSlot(new Point(ox + 70, oy + 24)).disableBackground().markInput().entries(inputs.get(1)));
            }
            widgets.add(Widgets.createArrow(new Point(ox + 96, oy + 24)).animationDurationTicks(60));
            if (!outputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 132, oy + 24)).disableBackground().markOutput().entries(outputs.get(0)));
            }
            widgets.add(Widgets.createLabel(new Point(ox + 84, oy + 72),
                    Component.literal(display.getSummary())).noShadow().color(0xFF404040, 0xFFBBBBBB));
        } else if (this.id.equals(GalacticraftREIServerPlugin.CHEMICAL_LEACHING)) {
            if (!inputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 28, oy + 18)).disableBackground().markInput().entries(inputs.get(0)));
            }
            if (inputs.size() > 1) {
                widgets.add(Widgets.createSlot(new Point(ox + 62, oy + 24)).disableBackground().markInput().entries(inputs.get(1)));
            }
            widgets.add(Widgets.createArrow(new Point(ox + 88, oy + 24)).animationDurationTicks(60));
            if (!outputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 124, oy + 12)).disableBackground().markOutput().entries(outputs.get(0)));
            }
            if (outputs.size() > 1) {
                widgets.add(Widgets.createSlot(new Point(ox + 124, oy + 36)).disableBackground().markOutput().entries(outputs.get(1)));
            }
            widgets.add(Widgets.createLabel(new Point(ox + 84, oy + 72),
                    Component.literal(display.getSummary())).noShadow().color(0xFF2E6B1E, 0xFF8CFF66));
        } else {
            int[][] gridPos = {{34, 12}, {52, 12}, {34, 30}, {52, 30}};
            for (int i = 0; i < Math.min(inputs.size(), 4); i++) {
                widgets.add(Widgets.createSlot(new Point(ox + gridPos[i][0], oy + gridPos[i][1]))
                        .disableBackground().markInput().entries(inputs.get(i)));
            }
            widgets.add(Widgets.createArrow(new Point(ox + 86, oy + 21)).animationDurationTicks(60));
            if (!outputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 132, oy + 21)).disableBackground().markOutput().entries(outputs.get(0)));
            }
            widgets.add(Widgets.createLabel(new Point(ox + 84, oy + 72),
                    Component.literal(display.getSummary())).noShadow().color(0xFF994411, 0xFFFFAE66));
        }

        return widgets;
    }

    private ResourceLocation getMachineTexture() {
        if (this.id.equals(GalacticraftREIServerPlugin.METALLURGIC_INFUSING)) {
            return Constant.ScreenTexture.VACUUM_METALLURGIC_INFUSER_SCREEN;
        }
        if (this.id.equals(GalacticraftREIServerPlugin.CHEMICAL_LEACHING)) {
            return Constant.ScreenTexture.CHEMICAL_LEACHING_PLANT_SCREEN;
        }
        return Constant.ScreenTexture.COMPOSITE_AUTOCLAVE_SCREEN;
    }

    @Override
    public int getDisplayWidth(MetallurgyProcessDisplay display) {
        return 176;
    }

    @Override
    public int getDisplayHeight() {
        return 90;
    }
}
