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

import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.world.biome.GCBiomes;
import dev.galacticraft.mod.world.gen.feature.GCOrePlacedFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class PlanetaryMultiOreStratigraphyTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void innerPlanetBiomesHaveValidMultiOrePlacedFeatures(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        int stepIdx = GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();

        Map<ResourceKey<Biome>, List<ResourceKey<PlacedFeature>>> expected = Map.of(
                GCBiomes.Moon.LUNAR_HIGHLANDS, List.of(
                        GCOrePlacedFeatures.ORE_ALUMINUM_MOON,
                        GCOrePlacedFeatures.ORE_ALUMINUM_HIGHLANDS_MOON,
                        GCOrePlacedFeatures.ORE_SILICON_MOON,
                        GCOrePlacedFeatures.ORE_ILMENITE_MOON,
                        GCOrePlacedFeatures.ORE_NIOBIUM_MOON,
                        GCOrePlacedFeatures.ORE_GOLD_MOON,
                        GCOrePlacedFeatures.ORE_REDSTONE_MOON,
                        GCOrePlacedFeatures.ORE_IRON_MOON
                ),
                GCBiomes.Mars.MARS_FLAT, List.of(
                        GCOrePlacedFeatures.ORE_IRON_MIDDLE_MARS,
                        GCOrePlacedFeatures.ORE_REDSTONE_MARS,
                        GCOrePlacedFeatures.ORE_SILICON_MARS,
                        GCOrePlacedFeatures.ORE_ALUMINUM_MARS,
                        GCOrePlacedFeatures.ORE_NIOBIUM_MARS,
                        GCOrePlacedFeatures.ORE_GOLD_MARS,
                        GCOrePlacedFeatures.ORE_TITANIUM_MARS,
                        GCOrePlacedFeatures.ORE_GRAPHITE_MARS,
                        GCOrePlacedFeatures.ORE_DESH_MIDDLE_MARS,
                        GCOrePlacedFeatures.ORE_ICE_MARS
                ),
                GCBiomes.Mercury.INTER_CRATER_PLAINS, List.of(
                        GCOrePlacedFeatures.ORE_NATIVE_IRON_MERCURY,
                        GCOrePlacedFeatures.ORE_GRAPHITE_MERCURY,
                        GCOrePlacedFeatures.ORE_TITANIUM_MERCURY,
                        GCOrePlacedFeatures.ORE_NIOBIUM_MERCURY,
                        GCOrePlacedFeatures.ORE_GOLD_MERCURY,
                        GCOrePlacedFeatures.ORE_SULFIDE_MERCURY,
                        GCOrePlacedFeatures.ORE_LEAD_MERCURY
                ),
                GCBiomes.Venus.VENUS_FLAT, List.of(
                        GCOrePlacedFeatures.ORE_GALENA_VENUS,
                        GCOrePlacedFeatures.ORE_COPPER_VENUS,
                        GCOrePlacedFeatures.ORE_TIN_VENUS,
                        GCOrePlacedFeatures.ORE_ALUMINUM_VENUS,
                        GCOrePlacedFeatures.ORE_GOLD_VENUS,
                        GCOrePlacedFeatures.ORE_REDSTONE_VENUS,
                        GCOrePlacedFeatures.ORE_SILICON_VENUS,
                        GCOrePlacedFeatures.ORE_NIOBIUM_VENUS,
                        GCOrePlacedFeatures.ORE_IRON_VENUS,
                        GCOrePlacedFeatures.ORE_TITANIUM_VENUS,
                        GCOrePlacedFeatures.ORE_SOLAR_VENUS
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
                    context.fail("Biome " + entry.getKey().location() + " missing bound PlacedFeature " + req.location());
                    return;
                }
            }
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void newPlanetaryOresDropExpectedRawItemsAndSmelt(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        RecipeManager manager = level.getRecipeManager();
        var registryAccess = level.registryAccess();
        BlockPos origin = context.absolutePos(BlockPos.ZERO);
        ItemStack diamondPick = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack silkPick = new ItemStack(Items.DIAMOND_PICKAXE);
        silkPick.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);

        List<OreExpectation> expectations = List.of(
                new OreExpectation(GCBlocks.MOON_ALUMINUM_ORE, GCItems.RAW_ALUMINUM, GCItems.ALUMINUM_INGOT),
                new OreExpectation(GCBlocks.MOON_SILICON_ORE, GCItems.SILICON, GCItems.SILICON),
                new OreExpectation(GCBlocks.MOON_ILMENITE_ORE, GCItems.RAW_TITANIUM, GCItems.TITANIUM_INGOT),
                new OreExpectation(GCBlocks.MOON_NIOBIUM_ORE, GCItems.RAW_NIOBIUM, GCItems.NIOBIUM_OXIDE),
                new OreExpectation(GCBlocks.MOON_GOLD_ORE, Items.RAW_GOLD, Items.GOLD_INGOT),
                new OreExpectation(GCBlocks.MOON_REDSTONE_ORE, Items.REDSTONE, Items.REDSTONE),
                new OreExpectation(GCBlocks.MOON_IRON_ORE, Items.RAW_IRON, Items.IRON_INGOT),
                new OreExpectation(GCBlocks.MARS_SILICON_ORE, GCItems.SILICON, GCItems.SILICON),
                new OreExpectation(GCBlocks.MARS_ALUMINUM_ORE, GCItems.RAW_ALUMINUM, GCItems.ALUMINUM_INGOT),
                new OreExpectation(GCBlocks.MARS_NIOBIUM_ORE, GCItems.RAW_NIOBIUM, GCItems.NIOBIUM_OXIDE),
                new OreExpectation(GCBlocks.MARS_GOLD_ORE, Items.RAW_GOLD, Items.GOLD_INGOT),
                new OreExpectation(GCBlocks.MARS_REDSTONE_ORE, Items.REDSTONE, Items.REDSTONE),
                new OreExpectation(GCBlocks.MARS_TITANIUM_ORE, GCItems.RAW_TITANIUM, GCItems.TITANIUM_INGOT),
                new OreExpectation(GCBlocks.MARS_GRAPHITE_ORE, GCItems.CARBON_FRAGMENTS, GCItems.CARBON_FRAGMENTS),
                new OreExpectation(GCBlocks.ASTEROID_NIOBIUM_ORE, GCItems.RAW_NIOBIUM, GCItems.NIOBIUM_OXIDE),
                new OreExpectation(GCBlocks.ASTEROID_GOLD_ORE, Items.RAW_GOLD, Items.GOLD_INGOT),
                new OreExpectation(GCBlocks.ASTEROID_GRAPHITE_ORE, GCItems.CARBON_FRAGMENTS, GCItems.CARBON_FRAGMENTS),
                new OreExpectation(GCBlocks.ASTEROID_COPPER_ORE, Items.RAW_COPPER, Items.COPPER_INGOT),
                new OreExpectation(GCBlocks.ASTEROID_REDSTONE_ORE, Items.REDSTONE, Items.REDSTONE),
                new OreExpectation(GCBlocks.MERCURY_NIOBIUM_ORE, GCItems.RAW_NIOBIUM, GCItems.NIOBIUM_OXIDE),
                new OreExpectation(GCBlocks.MERCURY_GOLD_ORE, Items.RAW_GOLD, Items.GOLD_INGOT),
                new OreExpectation(GCBlocks.MERCURY_LEAD_ORE, GCItems.RAW_LEAD, GCItems.LEAD_INGOT),
                new OreExpectation(GCBlocks.VENUS_IRON_ORE, Items.RAW_IRON, Items.IRON_INGOT),
                new OreExpectation(GCBlocks.VENUS_NIOBIUM_ORE, GCItems.RAW_NIOBIUM, GCItems.NIOBIUM_OXIDE),
                new OreExpectation(GCBlocks.VENUS_GOLD_ORE, Items.RAW_GOLD, Items.GOLD_INGOT),
                new OreExpectation(GCBlocks.VENUS_REDSTONE_ORE, Items.REDSTONE, Items.REDSTONE),
                new OreExpectation(GCBlocks.VENUS_SILICON_ORE, GCItems.SILICON, GCItems.SILICON),
                new OreExpectation(GCBlocks.VENUS_TITANIUM_ORE, GCItems.RAW_TITANIUM, GCItems.TITANIUM_INGOT)
        );

        for (OreExpectation exp : expectations) {
            // Normal drop
            LootParams.Builder normalParams = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                    .withParameter(LootContextParams.TOOL, diamondPick);
            List<ItemStack> drops = exp.block().defaultBlockState().getDrops(normalParams);
            if (drops.isEmpty() || !drops.get(0).is(exp.expectedDrop())) {
                context.fail("Unexpected normal drop for " + exp.block() + ": expected " + exp.expectedDrop() + ", got " + drops);
                return;
            }

            // Silk Touch drop
            LootParams.Builder silkParams = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                    .withParameter(LootContextParams.TOOL, silkPick);
            List<ItemStack> silkDrops = exp.block().defaultBlockState().getDrops(silkParams);
            if (silkDrops.size() != 1 || !silkDrops.get(0).is(exp.block().asItem())) {
                context.fail("Silk Touch failed for " + exp.block() + ": got " + silkDrops);
                return;
            }

            // Smelting & Blasting
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(exp.block()));
            var smelt = manager.getRecipeFor(RecipeType.SMELTING, input, level);
            if (smelt.isEmpty() || !smelt.get().value().getResultItem(registryAccess).is(exp.expectedSmelt())) {
                context.fail("Smelting recipe missing or wrong for " + exp.block());
                return;
            }
            var blast = manager.getRecipeFor(RecipeType.BLASTING, input, level);
            if (blast.isEmpty() || !blast.get().value().getResultItem(registryAccess).is(exp.expectedSmelt())) {
                context.fail("Blasting recipe missing or wrong for " + exp.block());
                return;
            }
        }

        context.succeed();
    }

    private record OreExpectation(Block block, Item expectedDrop, Item expectedSmelt) {
    }
}
