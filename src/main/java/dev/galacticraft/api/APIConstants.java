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

package dev.galacticraft.api;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface APIConstants {
    String MOD_ID = "galacticraft";
    String COMMON_NAMESPACE = "c";
    Logger LOGGER = LogManager.getLogger("Galacticraft-API");

    @Contract(value = "_ -> new", pure = true)
    static @NotNull ResourceLocation id(String id) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, id);
    }

    @Contract(value = "_, _ -> new", pure = true)
    static @NotNull <T> ResourceKey<T> key(ResourceKey<Registry<T>> registry, String id) {
        return ResourceKey.create(registry, APIConstants.id(id));
    }

    interface Gas {
        String ARGON = "gas.galacticraft.argon";
        String CARBON_DIOXIDE = "gas.galacticraft.carbon_dioxide";
        String CARBON_MONOXIDE = "gas.galacticraft.carbon_monoxide";
        String HELIUM = "gas.galacticraft.helium";
        String HYDROGEN = "gas.galacticraft.hydrogen";
        String HYDROGEN_DEUTERIUM_OXIDE = "gas.galacticraft.hydrogen_deuterium_oxide";
        String IODINE = "gas.galacticraft.iodine";
        String KRYPTON = "gas.galacticraft.krypton";
        String METHANE = "gas.galacticraft.methane";
        String NEON = "gas.galacticraft.neon";
        String NITRIC_OXIDE = "gas.galacticraft.nitric_oxide";
        String NITROGEN = "gas.galacticraft.nitrogen";
        String NITROGEN_DIOXIDE = "gas.galacticraft.nitrogen_dioxide";
        String NITROUS_OXIDE = "gas.galacticraft.nitrous_oxide";
        String OXYGEN = "gas.galacticraft.oxygen";
        String OZONE = "gas.galacticraft.ozone";
        String WATER_VAPOR = "gas.galacticraft.water_vapor";
        String XENON = "gas.galacticraft.xenon";
    }

    interface Ui {
        String CONE = "ui.galacticraft.cone";
        String BODY = "ui.galacticraft.body";
        String FINS = "ui.galacticraft.fins";
        String BOOSTER = "ui.galacticraft.booster";
        String ENGINE = "ui.galacticraft.engine";
        String UPGRADE = "ui.galacticraft.upgrade";
    }
}
