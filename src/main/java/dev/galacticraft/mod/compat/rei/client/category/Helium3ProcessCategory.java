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
import net.minecraft.resources.ResourceLocation;
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

        int ox = bounds.x + 4;
        int oy = bounds.y + 4;

        // Blit the actual machine GUI upper panel (cropped cleanly below title bar: u=4, v=18, w=168, h=82)
        ResourceLocation bgTex = this.getMachineTexture();
        widgets.add(Widgets.createTexturedWidget(bgTex, ox, oy, 4, 18, 168, 82));

        List<EntryIngredient> inputs = display.getInputEntries();
        List<EntryIngredient> outputs = display.getOutputEntries();

        if (this.id.equals(GalacticraftREIServerPlugin.THERMAL_DESORPTION)) {
            // Input regolith in INPUT_SLOT_1 (36, 26 -> rel 32, 8)
            if (!inputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 32, oy + 8)).disableBackground().markInput().entries(inputs.get(0)));
            }
            // Animated arrow between input and outputs
            widgets.add(Widgets.createArrow(new Point(ox + 64, oy + 18)).animationDurationTicks(60));
            // Gas output in GAS_TANK area (148, 20 -> rel 144, 18) + byproducts in BYPRODUCT_SLOT_1/2 (108, 26 / 48 -> rel 104, 8 / 30)
            if (!outputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 144, oy + 18)).disableBackground().markOutput().entries(outputs.get(0)));
            }
            if (outputs.size() > 1) {
                widgets.add(Widgets.createSlot(new Point(ox + 104, oy + 8)).disableBackground().markOutput().entries(outputs.get(1)));
            }
            if (outputs.size() > 2) {
                widgets.add(Widgets.createSlot(new Point(ox + 104, oy + 30)).disableBackground().markOutput().entries(outputs.get(2)));
            }
            // Compact telemetry bar inside machine panel
            widgets.add(Widgets.createLabel(new Point(ox + 84, oy + 72),
                    Component.literal(display.getSummary())).noShadow().color(0xFF404040, 0xFFBBBBBB));
        } else if (this.id.equals(GalacticraftREIServerPlugin.ISOTOPIC_SEPARATION)) {
            // Input (gas in left tank 32,20 -> rel 28,18 OR solid in 68,52 -> rel 64,34) + Coil in 68,22 -> rel 64,4
            if (!inputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 28, oy + 18)).disableBackground().markInput().entries(inputs.get(0)));
            }
            if (inputs.size() > 1) {
                widgets.add(Widgets.createSlot(new Point(ox + 64, oy + 4)).disableBackground().markInput().entries(inputs.get(1)));
            }
            widgets.add(Widgets.createArrow(new Point(ox + 88, oy + 6)).animationDurationTicks(40));
            // Primary gas output in 3He tank (126,20 -> rel 122,18), secondary in 4He tank (150,20 -> rel 146,18) or byproduct slot (98,52 -> rel 94,34)
            if (!outputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 122, oy + 18)).disableBackground().markOutput().entries(outputs.get(0)));
            }
            if (outputs.size() > 1) {
                widgets.add(Widgets.createSlot(new Point(ox + 146, oy + 18)).disableBackground().markOutput().entries(outputs.get(1)));
            }
            widgets.add(Widgets.createLabel(new Point(ox + 84, oy + 72),
                    Component.literal(display.getSummary())).noShadow().color(0xFF007B9E, 0xFF55E6FF));
        } else {
            // MHD Fusion Reactor: 3He (32,20 -> 28,18), D2 (54,20 -> 50,18), Coil (82,22 -> 78,4), Cryo <2K (82,48 -> 78,30) -> Output/Charge (148,35 -> 144,17)
            if (!inputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 28, oy + 18)).disableBackground().markInput().entries(inputs.get(0)));
            }
            if (inputs.size() > 1) {
                widgets.add(Widgets.createSlot(new Point(ox + 50, oy + 18)).disableBackground().markInput().entries(inputs.get(1)));
            }
            if (inputs.size() > 2) {
                widgets.add(Widgets.createSlot(new Point(ox + 78, oy + 4)).disableBackground().markInput().entries(inputs.get(2)));
            }
            if (inputs.size() > 3) {
                widgets.add(Widgets.createSlot(new Point(ox + 78, oy + 30)).disableBackground().markInput().entries(inputs.get(3)));
            }
            widgets.add(Widgets.createArrow(new Point(ox + 108, oy + 17)).animationDurationTicks(40));
            if (!outputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 144, oy + 17)).disableBackground().markOutput().entries(outputs.get(0)));
            }
            widgets.add(Widgets.createLabel(new Point(ox + 84, oy + 72),
                    Component.literal(display.getSummary())).noShadow().color(0xFF1B8A3A, 0xFF64FFDA));
        }

        return widgets;
    }

    private ResourceLocation getMachineTexture() {
        if (this.id.equals(GalacticraftREIServerPlugin.THERMAL_DESORPTION)) {
            return Constant.ScreenTexture.REGOLITH_THERMAL_DESORBER_SCREEN;
        }
        if (this.id.equals(GalacticraftREIServerPlugin.ISOTOPIC_SEPARATION)) {
            return Constant.ScreenTexture.ISOTOPIC_CENTRIFUGE_SCREEN;
        }
        return Constant.ScreenTexture.HELIUM3_FUSION_REACTOR_SCREEN;
    }

    @Override
    public int getDisplayWidth(Helium3ProcessDisplay display) {
        return 176;
    }

    @Override
    public int getDisplayHeight() {
        return 90;
    }
}
