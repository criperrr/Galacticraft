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

package dev.galacticraft.mod.gametest;

import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.FluidCanisterItem;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.recipe.GCRecipes;
import dev.galacticraft.mod.world.biome.GCBiomes;
import dev.galacticraft.mod.world.gen.feature.GCOrePlacedFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

public class OuterPlanetsExoticMineralogyTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void outerPlanetBiomesHaveExoticAndMultiOrePlacedFeatures(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        int stepIdx = GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();

        Map<ResourceKey<Biome>, List<ResourceKey<PlacedFeature>>> expected = Map.of(
                GCBiomes.Jupiter.RED_STORM_VORTEX, List.of(
                        GCOrePlacedFeatures.ORE_COPPER_JUPITER,
                        GCOrePlacedFeatures.ORE_TIN_JUPITER,
                        GCOrePlacedFeatures.ORE_IRON_JUPITER,
                        GCOrePlacedFeatures.ORE_ALUMINUM_JUPITER,
                        GCOrePlacedFeatures.ORE_NIOBIUM_JUPITER,
                        GCOrePlacedFeatures.ORE_TITANIUM_JUPITER,
                        GCOrePlacedFeatures.ORE_PALLADIUM_JUPITER
                ),
                GCBiomes.Saturn.HEXAGON_POLAR_PLATEAU, List.of(
                        GCOrePlacedFeatures.ORE_COPPER_SATURN,
                        GCOrePlacedFeatures.ORE_TIN_SATURN,
                        GCOrePlacedFeatures.ORE_IRON_SATURN,
                        GCOrePlacedFeatures.ORE_TITANIUM_SATURN,
                        GCOrePlacedFeatures.ORE_ALUMINUM_SATURN,
                        GCOrePlacedFeatures.ORE_NIOBIUM_SATURN,
                        GCOrePlacedFeatures.ORE_MAGNESIUM_SATURN
                ),
                GCBiomes.Uranus.DIAMOND_MANTLE_CAVERNS, List.of(
                        GCOrePlacedFeatures.ORE_COPPER_URANUS,
                        GCOrePlacedFeatures.ORE_TIN_URANUS,
                        GCOrePlacedFeatures.ORE_ALUMINUM_URANUS,
                        GCOrePlacedFeatures.ORE_TITANIUM_URANUS,
                        GCOrePlacedFeatures.ORE_IRON_URANUS,
                        GCOrePlacedFeatures.ORE_NIOBIUM_URANUS,
                        GCOrePlacedFeatures.ORE_URANIUM_URANUS
                ),
                GCBiomes.Neptune.ABYSSAL_MANTLE_DEPTHS, List.of(
                        GCOrePlacedFeatures.ORE_COPPER_NEPTUNE,
                        GCOrePlacedFeatures.ORE_TIN_NEPTUNE,
                        GCOrePlacedFeatures.ORE_IRON_NEPTUNE,
                        GCOrePlacedFeatures.ORE_TITANIUM_NEPTUNE,
                        GCOrePlacedFeatures.ORE_ALUMINUM_NEPTUNE,
                        GCOrePlacedFeatures.ORE_NIOBIUM_NEPTUNE
                )
        );

        for (var entry : expected.entrySet()) {
            Holder<Biome> biome = biomeRegistry.getHolderOrThrow(entry.getKey());
            var features = biome.value().getGenerationSettings().features();
            if (stepIdx >= features.size()) {
                context.fail("Biome missing UNDERGROUND_ORES step: " + entry.getKey().location());
                return;
            }
            var oreHolders = features.get(stepIdx);
            for (ResourceKey<PlacedFeature> req : entry.getValue()) {
                boolean found = false;
                for (Holder<PlacedFeature> holder : oreHolders) {
                    if (holder.is(req) && holder.isBound()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    context.fail("Outer planet biome " + entry.getKey().location() + " missing bound PlacedFeature " + req.location());
                    return;
                }
            }
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void outerPlanetExoticOresAndMantleRocksDropExpectedItems(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BlockPos origin = context.absolutePos(BlockPos.ZERO);
        ItemStack titaniumPick = new ItemStack(GCItems.TITANIUM_PICKAXE);
        ItemStack silkPick = new ItemStack(GCItems.TITANIUM_PICKAXE);
        silkPick.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);

        Map<Block, Item> expectedDrops = Map.of(
                GCBlocks.JUPITER_PALLADIUM_ORE, GCItems.RAW_PALLADIUM,
                GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK, GCItems.METALLIC_HYDROGEN_CELL,
                GCBlocks.SATURN_MAGNESIUM_ORE, GCItems.RAW_MAGNESIUM,
                GCBlocks.SATURN_HELIUM_MANTLE_ROCK, GCBlocks.SATURN_HELIUM_MANTLE_ROCK.asItem(),
                GCBlocks.URANUS_URANIUM_ORE, GCItems.RAW_URANIUM,
                GCBlocks.URANUS_DIAMOND_BEARING_ROCK, GCItems.LONSDALEITE_GEM,
                GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK, GCItems.SUPERIONIC_ICE_CRYSTAL
        );

        for (var entry : expectedDrops.entrySet()) {
            Block block = entry.getKey();
            Item expectedItem = entry.getValue();

            LootParams.Builder normalParams = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                    .withParameter(LootContextParams.TOOL, titaniumPick);
            List<ItemStack> drops = block.defaultBlockState().getDrops(normalParams);
            if (drops.isEmpty() || !drops.get(0).is(expectedItem)) {
                context.fail("Unexpected drop for " + block + ": expected " + expectedItem + ", got " + drops);
                return;
            }

            LootParams.Builder silkParams = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                    .withParameter(LootContextParams.TOOL, silkPick);
            List<ItemStack> silkDrops = block.defaultBlockState().getDrops(silkParams);
            if (silkDrops.size() != 1 || !silkDrops.get(0).is(block.asItem())) {
                context.fail("Silk Touch failed for " + block + ": got " + silkDrops);
                return;
            }
        }

        // Verify Helium-3, Deuterium, Solar Volatile Gas, and Xenon are registered real GasFluids usable in FluidCanisterItem
        for (var gas : List.of(Gases.HELIUM_3, Gases.DEUTERIUM, Gases.SOLAR_VOLATILE_GAS, Gases.XENON)) {
            if (BuiltInRegistries.FLUID.getResourceKey(gas).isEmpty()) {
                context.fail("Expected gas fluid to be registered: " + gas);
                return;
            }
            ItemStack filled = FluidCanisterItem.getFilledCanister(GCItems.FLUID_CANISTER, gas);
            var storage = FluidCanisterItem.getStorage(filled);
            if (!storage.getResource().isOf(gas) || storage.getAmount() <= 0) {
                context.fail("FluidCanisterItem failed to store gas fluid: " + gas);
                return;
            }
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void exoticSmeltingCompressingAndHelium3ChainRecipesWork(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        RecipeManager manager = level.getRecipeManager();
        var registryAccess = level.registryAccess();

        Map<Item, Item> expectedBlasting = Map.ofEntries(
                Map.entry(GCItems.RAW_PALLADIUM, GCItems.PALLADIUM_INGOT),
                Map.entry(GCItems.RAW_MAGNESIUM, GCItems.MAGNESIUM_INGOT),
                Map.entry(GCItems.RAW_URANIUM, GCItems.URANIUM_INGOT),
                Map.entry(GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK.asItem(), GCItems.METALLIC_HYDROGEN_CELL),
                Map.entry(GCBlocks.URANUS_DIAMOND_BEARING_ROCK.asItem(), GCItems.LONSDALEITE_GEM),
                Map.entry(GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK.asItem(), GCItems.SUPERIONIC_ICE_CRYSTAL),
                Map.entry(GCBlocks.NEPTUNE_CRYO_SLATE.asItem(), GCItems.SUPERIONIC_ICE_CRYSTAL)
        );

        for (var entry : expectedBlasting.entrySet()) {
            var recipe = manager.getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(new ItemStack(entry.getKey())), level);
            if (recipe.isEmpty() || !recipe.get().value().getResultItem(registryAccess).is(entry.getValue())) {
                context.fail("Blasting recipe missing or wrong for " + entry.getKey() + " -> expected " + entry.getValue());
                return;
            }
        }

        // Compressing Palladium & Magnesium plates
        CraftingInput palladiumComp = CraftingInput.of(2, 1, List.of(new ItemStack(GCItems.PALLADIUM_INGOT), new ItemStack(GCItems.PALLADIUM_INGOT)));
        var compPd = manager.getRecipeFor(GCRecipes.COMPRESSING_TYPE, palladiumComp, level);
        if (compPd.isEmpty() || !compPd.get().value().getResultItem(registryAccess).is(GCItems.COMPRESSED_PALLADIUM)) {
            context.fail("Compressing recipe missing for COMPRESSED_PALLADIUM");
            return;
        }

        CraftingInput magnesiumComp = CraftingInput.of(2, 1, List.of(new ItemStack(GCItems.MAGNESIUM_INGOT), new ItemStack(GCItems.MAGNESIUM_INGOT)));
        var compMg = manager.getRecipeFor(GCRecipes.COMPRESSING_TYPE, magnesiumComp, level);
        if (compMg.isEmpty() || !compMg.get().value().getResultItem(registryAccess).is(GCItems.COMPRESSED_MAGNESIUM)) {
            context.fail("Compressing recipe missing for COMPRESSED_MAGNESIUM");
            return;
        }

        // Helium-3 Fusion Cell synthesis (HELIUM3_CELL + DENSE_ICE + SUPERCONDUCTING_COIL)
        CraftingInput fusionInput = CraftingInput.of(3, 1, List.of(
                new ItemStack(GCItems.HELIUM3_CELL),
                new ItemStack(GCBlocks.DENSE_ICE),
                new ItemStack(GCItems.SUPERCONDUCTING_COIL)
        ));
        var fusionRecipe = manager.getRecipeFor(RecipeType.CRAFTING, fusionInput, level);
        if (fusionRecipe.isEmpty() || !fusionRecipe.get().value().getResultItem(registryAccess).is(GCItems.HELIUM3_FUSION_CELL)) {
            context.fail("Crafting recipe missing for HELIUM3_FUSION_CELL");
            return;
        }

        context.succeed();
    }
}
