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

package dev.galacticraft.mod.compat.jei.subtypes;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ModularRocketPartSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
    public static final ModularRocketPartSubtypeInterpreter INSTANCE = new ModularRocketPartSubtypeInterpreter();

    @Override
    public @Nullable RocketPartConfig getSubtypeData(ItemStack itemStack, UidContext context) {
        return itemStack.get(GCDataComponents.ROCKET_PART_CONFIG);
    }

    @Override
    public String getLegacyStringSubtypeInfo(ItemStack itemStack, UidContext context) {
        RocketPartConfig config = itemStack.get(GCDataComponents.ROCKET_PART_CONFIG);
        if (config == null) {
            return "";
        }
        return config.modelId() + "|" + config.livery().getSerializedName();
    }
}
