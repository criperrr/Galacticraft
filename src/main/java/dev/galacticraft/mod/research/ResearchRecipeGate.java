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

package dev.galacticraft.mod.research;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Soft research-gating service linking {@code AEROSPACE_RD_TERMINAL} node unlocks to
 * advanced Era 2-6 crafting recipes and modular rocket part families.
 *
 * <p>Design guarantees:
 * <ul>
 *   <li>All Earth survival items, early machines, and Era 1 rocket parts remain 100% accessible.</li>
 *   <li>Creative-mode players always bypass soft recipe gating.</li>
 *   <li>Recipes stay visible in recipe viewers (EMI/REI/JEI) with clear guidance toward the
 *       exact R&amp;D node and laboratory required to unlock them.</li>
 * </ul>
 */
public final class ResearchRecipeGate {
    private static final Map<String, ResourceLocation> GATED_PART_FAMILIES = Map.ofEntries(
            Map.entry("tank_methalox_bulkhead", ResearchCatalog.MARTIAN_SABATIER_ISRU),
            Map.entry("engine_raptor", ResearchCatalog.METHALOX_HEAVY_ENGINES),
            Map.entry("engine_be4", ResearchCatalog.METHALOX_HEAVY_ENGINES),
            Map.entry("tank_hydrolox_supercryo", ResearchCatalog.ASTEROID_TITANIUM_COMPOSITES),
            Map.entry("engine_nerva", ResearchCatalog.MERCURY_THORIUM_ISOTOPES),
            Map.entry("power_rtg", ResearchCatalog.MERCURY_THORIUM_ISOTOPES),
            Map.entry("tank_upgrade_cryocooler", ResearchCatalog.ZERO_G_DILUTION_CRYOGENICS),
            Map.entry("tank_upgrade_copv_helium", ResearchCatalog.SATURN_HELIUM3_HARVESTING),
            Map.entry("tank_xenon_copv", ResearchCatalog.NEPTUNE_SUPERIONIC_MAGNETOPLASMA),
            Map.entry("engine_hall_ion", ResearchCatalog.NEPTUNE_SUPERIONIC_MAGNETOPLASMA),
            Map.entry("engine_plasma_vasimr", ResearchCatalog.NEPTUNE_SUPERIONIC_MAGNETOPLASMA),
            Map.entry("cockpit_stargarrison", ResearchCatalog.ZERO_G_HEAVY_ORBITAL_WELDING),
            Map.entry("science_orbital_laboratory", ResearchCatalog.ORBITAL_SHIPYARD_DOCKS)
    );

    private ResearchRecipeGate() {
    }

    /**
     * Resolves the required {@link ResearchNode} ID for a crafting recipe or output item,
     * or {@code null} if the item/recipe is ungated (basic Earth survival / Era 1).
     */
    public static @Nullable ResourceLocation getRequiredNode(
            @Nullable ResourceLocation recipeId,
            @Nullable ItemStack resultStack
    ) {
        if (recipeId != null) {
            ResourceLocation byRecipe = ResearchCatalog.getRequiredNodeForRecipe(recipeId);
            if (byRecipe != null) {
                return byRecipe;
            }
            // Also normalize paths like "decorations/sabatier_chemical_reactor" -> "sabatier_chemical_reactor"
            String path = recipeId.getPath();
            int slash = path.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < path.length()) {
                ResourceLocation stripped = ResourceLocation.fromNamespaceAndPath(recipeId.getNamespace(), path.substring(slash + 1));
                ResourceLocation byStripped = ResearchCatalog.getRequiredNodeForRecipe(stripped);
                if (byStripped != null) {
                    return byStripped;
                }
            }
        }
        if (resultStack != null && !resultStack.isEmpty()) {
            return getRequiredNodeForItem(resultStack.getItem());
        }
        return null;
    }

    public static @Nullable ResourceLocation getRequiredNodeForItem(@Nullable Item item) {
        if (item == null) {
            return null;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        return ResearchCatalog.getRequiredNodeForRecipe(itemId);
    }

    public static @Nullable ResourceLocation getRequiredNodeForPartFamily(@Nullable String familyId) {
        if (familyId == null || familyId.isEmpty()) {
            return null;
        }
        return GATED_PART_FAMILIES.get(familyId.toLowerCase(Locale.ROOT));
    }

    /**
     * Server-side check for whether a crafting recipe / output item is unlocked for {@code player}.
     */
    public static boolean isRecipeUnlocked(
            @NotNull ServerLevel level,
            @Nullable Player player,
            @Nullable ResourceLocation recipeId,
            @Nullable ItemStack resultStack
    ) {
        if (player != null && player.isCreative()) {
            return true;
        }
        ResourceLocation requiredNode = getRequiredNode(recipeId, resultStack);
        if (requiredNode == null) {
            return true;
        }
        return ResearchManager.get(level).isUnlocked(requiredNode);
    }

    /**
     * Server-side check for whether a modular rocket part family is unlocked in the Part Assembler.
     */
    public static boolean isPartFamilyUnlocked(
            @NotNull ServerLevel level,
            @Nullable Player player,
            @Nullable String familyId
    ) {
        if (player != null && player.isCreative()) {
            return true;
        }
        ResourceLocation requiredNode = getRequiredNodeForPartFamily(familyId);
        if (requiredNode == null) {
            return true;
        }
        return ResearchManager.get(level).isUnlocked(requiredNode);
    }

    /**
     * Client-side check using synchronized {@link ClientResearchCache}.
     */
    public static boolean isPartFamilyUnlockedClient(@Nullable Player player, @Nullable String familyId) {
        if (player != null && player.isCreative()) {
            return true;
        }
        ResourceLocation requiredNode = getRequiredNodeForPartFamily(familyId);
        return requiredNode == null || ClientResearchCache.isUnlocked(requiredNode);
    }

    public static @NotNull Component lockedHintMessage(@NotNull ResourceLocation requiredNodeId) {
        ResearchNode node = ResearchCatalog.get(requiredNodeId);
        if (node == null) {
            return Component.literal("[R&D] Requires research: " + requiredNodeId.getPath()).withStyle(ChatFormatting.GOLD);
        }
        return Component.literal("[R&D] Locked — Research '" + node.title() + "' (Era " + node.era()
                + ", " + node.sourceLabName() + ") in Aerospace R&D Terminal").withStyle(ChatFormatting.GOLD);
    }
}
