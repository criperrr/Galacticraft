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

package dev.galacticraft.mod.client.gui.screen.manual;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Optional zero-dependency soft bridge for opening item recipes in EMI, REI, or JEI
 * from inside the Aerospace Manual Index tab without requiring any recipe viewer at runtime.
 */
@Environment(EnvType.CLIENT)
public final class RecipeViewerBridge {
    private RecipeViewerBridge() {
    }

    public static @NotNull String activeViewerName() {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("emi")) {
            return "EMI";
        }
        if (loader.isModLoaded("roughlyenoughitems")) {
            return "REI";
        }
        if (loader.isModLoaded("jei")) {
            return "JEI";
        }
        return "";
    }

    public static boolean isAnyViewerLoaded() {
        return !activeViewerName().isEmpty();
    }

    public static boolean openRecipeFor(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("emi") && tryOpenEmi(stack)) {
            return true;
        }
        if (loader.isModLoaded("roughlyenoughitems") && tryOpenRei(stack)) {
            return true;
        }
        return false;
    }

    private static boolean tryOpenEmi(ItemStack stack) {
        try {
            Class<?> emiStackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            Method ofStack = emiStackClass.getMethod("of", ItemStack.class);
            Object emiStack = ofStack.invoke(null, stack);

            Class<?> emiIngredientClass = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            Class<?> emiApiClass = Class.forName("dev.emi.emi.api.EmiApi");
            Method displayRecipes = emiApiClass.getMethod("displayRecipes", emiIngredientClass);
            displayRecipes.invoke(null, emiStack);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryOpenRei(ItemStack stack) {
        try {
            Class<?> entryStacksClass = Class.forName("me.shedaniel.rei.api.common.util.EntryStacks");
            Method ofMethod = entryStacksClass.getMethod("of", ItemStack.class);
            Object entryStack = ofMethod.invoke(null, stack);

            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
            Class<?> viewSearchBuilderClass = Class.forName("me.shedaniel.rei.api.client.view.ViewSearchBuilder");
            Object builder = viewSearchBuilderClass.getMethod("builder").invoke(null);
            viewSearchBuilderClass.getMethod("addRecipesFor", entryStackClass).invoke(builder, entryStack);
            viewSearchBuilderClass.getMethod("open").invoke(builder);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
