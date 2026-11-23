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

package dev.galacticraft.mod.compat.rei.common;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.compat.rei.common.display.*;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.CannedFoodItem;
import dev.galacticraft.mod.content.item.GCItems;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REIServerPlugin;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GalacticraftREIServerPlugin implements REIServerPlugin {
    public static final CategoryIdentifier<DefaultFabricationDisplay> FABRICATION = CategoryIdentifier.of(Constant.MOD_ID, "plugins/" + Constant.Recipe.FABRICATION);
    public static final CategoryIdentifier<DefaultCompressingDisplay> COMPRESSING = CategoryIdentifier.of(Constant.MOD_ID, "plugins/" + Constant.Recipe.COMPRESSING);
    public static final CategoryIdentifier<ElectricCompressingDisplay> ELECTRIC_COMPRESSING = CategoryIdentifier.of(Constant.MOD_ID, "plugins/" + Constant.Recipe.ELECTRIC_COMPRESSING);
    public static final CategoryIdentifier<ElectricFurnaceDisplay> ELECTRIC_SMELTING = CategoryIdentifier.of(Constant.MOD_ID, "plugins/" + Constant.Recipe.ELECTRIC_SMELTING);
    public static final CategoryIdentifier<ElectricArcFurnaceDisplay> ELECTRIC_BLASTING = CategoryIdentifier.of(Constant.MOD_ID, "plugins/" + Constant.Recipe.ELECTRIC_BLASTING);
    public static final CategoryIdentifier<DefaultCanningDisplay> CANNING = CategoryIdentifier.of(Constant.MOD_ID, "plugins/" + Constant.Recipe.CANNING);
//    public static final CategoryIdentifier<DefaultCompressingDisplay> COAL_GENERATOR_FUEL = CategoryIdentifier.of(Constant.MOD_ID, "plugins/coal_generator_fuel");
    public static final CategoryIdentifier<DefaultRocketDisplay> ROCKET = CategoryIdentifier.of(Constant.MOD_ID, "plugins/" + Constant.Recipe.ROCKET);
    public static final CategoryIdentifier<Helium3ProcessDisplay> THERMAL_DESORPTION = CategoryIdentifier.of(Constant.MOD_ID, "plugins/" + Constant.Recipe.THERMAL_DESORPTION);
    public static final CategoryIdentifier<Helium3ProcessDisplay> ISOTOPIC_SEPARATION = CategoryIdentifier.of(Constant.MOD_ID, "plugins/isotopic_separation");
    public static final CategoryIdentifier<Helium3ProcessDisplay> MHD_FUSION = CategoryIdentifier.of(Constant.MOD_ID, "plugins/" + Constant.Recipe.FUSION_REACTOR);

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.register(FABRICATION, DefaultFabricationDisplay.SERIALIZER);
        registry.register(COMPRESSING, DefaultCompressingDisplay.Serializer.INSTANCE);
        registry.register(ELECTRIC_COMPRESSING, ElectricCompressingDisplay.Serializer.INSTANCE);
        registry.register(ELECTRIC_SMELTING, ElectricFurnaceDisplay.SERIALIZER);
        registry.register(ELECTRIC_BLASTING, ElectricArcFurnaceDisplay.SERIALIZER);
        registry.register(CANNING, DefaultCanningDisplay.SERIALIZER);
        registry.register(ROCKET, DefaultRocketDisplay.SERIALIZER);
    }

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        registry.register((context, cannedFood) -> {
            long hash = 0;
            for (ItemStack stack : CannedFoodItem.getContents(cannedFood)) {
                hash = hash * 31 + ItemStack.hashItemAndComponents(stack);
            }
            return hash;
        }, GCItems.CANNED_FOOD);

        registry.register(
                (context, stack) -> hashRocketPartConfig(stack.get(GCDataComponents.ROCKET_PART_CONFIG)),
                GCBlocks.ROCKET_PART_COMMAND.asItem(),
                GCBlocks.ROCKET_PART_ENGINE.asItem(),
                GCBlocks.ROCKET_PART_TANK.asItem(),
                GCBlocks.ROCKET_PART_ADAPTER.asItem(),
                GCBlocks.ROCKET_PART_PAYLOAD.asItem(),
                GCBlocks.ROCKET_PART_UTILITY.asItem()
        );
    }

    private static long hashRocketPartConfig(@Nullable RocketPartConfig config) {
        if (config == null) {
            return 0L;
        }
        long hash = 0xCBF29CE484222325L;
        hash = mixString(hash, config.modelId());
        hash = mixString(hash, config.familyId());
        hash = (hash ^ config.category().ordinal()) * 0x100000001B3L;
        hash = (hash ^ config.diameter()) * 0x100000001B3L;
        hash = (hash ^ config.topDiameter()) * 0x100000001B3L;
        hash = (hash ^ config.heightBlocks()) * 0x100000001B3L;
        hash = (hash ^ config.livery().ordinal()) * 0x100000001B3L;
        hash = (hash ^ (config.walkableInterior() ? 1L : 0L)) * 0x100000001B3L;
        hash = (hash ^ (config.radial() ? 1L : 0L)) * 0x100000001B3L;
        return hash == 0L ? 1L : hash;
    }

    private static long mixString(long hash, String value) {
        for (int i = 0; i < value.length(); i++) {
            hash = (hash ^ value.charAt(i)) * 0x100000001B3L;
        }
        return (hash ^ 0xFFL) * 0x100000001B3L;
    }
}
