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

package dev.galacticraft.mod.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.PartDamageState;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.mod.Constant;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Part Assembler Recipe & Input Test Suite")
class PartAssemblerRecipeTest {

    private static final double EPSILON = 1e-6;
    private static RegistryAccess registryAccess;
    private static RegistryOps<JsonElement> registryOps;

    @BeforeAll
    public static void setupMinecraftBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(Constant.id(Constant.Recipe.PART_ASSEMBLER))) {
            GCRecipes.register();
        }
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        registryOps = registryAccess.createSerializationContext(JsonOps.INSTANCE);
    }

    // --- Helper Fixtures ---

    private static ShapedRecipePattern create3x3Pattern(Ingredient fillIngredient) {
        Map<Character, Ingredient> key = Map.of('X', fillIngredient);
        return ShapedRecipePattern.of(
                key,
                "XXX",
                "XXX",
                "XXX"
        );
    }

    private static ShapedRecipePattern createAsymmetricPattern() {
        Map<Character, Ingredient> key = Map.of(
                'I', Ingredient.of(Items.IRON_INGOT),
                'C', Ingredient.of(Items.COPPER_INGOT)
        );
        return ShapedRecipePattern.of(
                key,
                "III",
                "ICI",
                "III"
        );
    }

    private static List<ItemStack> createGrid9(ItemStack fillStack) {
        List<ItemStack> list = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            list.add(fillStack.copy());
        }
        return list;
    }

    private static PartAssemblerRecipe createSampleRecipe(
            Optional<RocketPartStats> stats,
            ItemStack result,
            ItemStack scrap
    ) {
        return new PartAssemblerRecipe(
                "rocket_engines",
                Ingredient.of(Items.PAPER),
                create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                result,
                scrap,
                200,
                stats
        );
    }

    // =========================================================================
    // 1. PartAssemblerRecipeInput Tests
    // =========================================================================

    @Nested
    @DisplayName("1. PartAssemblerRecipeInput Indexing & Bounds")
    class RecipeInputTests {

        @Test
        @DisplayName("Input indexing maps index 0 to schematic and indices 1..9 to 3x3 crafting grid")
        void testRecipeInputIndexing() {
            ItemStack schematic = new ItemStack(Items.PAPER, 1);
            List<ItemStack> gridItems = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) {
                gridItems.add(new ItemStack(Items.IRON_INGOT, i + 1));
            }

            PartAssemblerRecipeInput input = new PartAssemblerRecipeInput(schematic, 3, 3, gridItems);

            assertEquals(10, input.size(), "Size must be 1 (schematic) + 9 (grid) = 10");
            assertSame(schematic, input.schematic());
            assertSame(schematic, input.getItem(0), "Index 0 must return schematic");

            assertSame(input.grid(), input.craftingInput(), "craftingInput() alias must return grid()");
            assertEquals(3, input.craftingInput().width());
            assertEquals(3, input.craftingInput().height());

            for (int i = 0; i < 9; i++) {
                ItemStack gridItem = input.getItem(i + 1);
                assertSame(gridItems.get(i), gridItem, "Index " + (i + 1) + " must map to grid index " + i);
                assertEquals(i + 1, gridItem.getCount());
            }

            assertTrue(input.getItem(10).isEmpty(), "Index 10 is out of bounds and must return EMPTY");
            assertTrue(input.getItem(99).isEmpty(), "Large index is out of bounds and must return EMPTY");
            assertTrue(input.getItem(-1).isEmpty(), "Negative index must return EMPTY");
        }

        @Test
        @DisplayName("Input emptiness checks both schematic and grid items")
        void testRecipeInputEmptiness() {
            PartAssemblerRecipeInput totallyEmpty = new PartAssemblerRecipeInput(
                    ItemStack.EMPTY,
                    3,
                    3,
                    createGrid9(ItemStack.EMPTY)
            );
            assertTrue(totallyEmpty.isEmpty(), "Input with empty schematic and empty grid must be empty");

            PartAssemblerRecipeInput onlySchematic = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    createGrid9(ItemStack.EMPTY)
            );
            assertFalse(onlySchematic.isEmpty(), "Input with schematic must not be empty");

            List<ItemStack> oneItemGrid = createGrid9(ItemStack.EMPTY);
            oneItemGrid.set(4, new ItemStack(Items.IRON_INGOT));
            PartAssemblerRecipeInput onlyGridItem = new PartAssemblerRecipeInput(
                    ItemStack.EMPTY,
                    3,
                    3,
                    oneItemGrid
            );
            assertFalse(onlyGridItem.isEmpty(), "Input with a grid item must not be empty");
        }

        @Test
        @DisplayName("Null arguments in constructor trigger NullPointerException")
        void testConstructorNullGuards() {
            CraftingInput dummyGrid = CraftingInput.of(3, 3, createGrid9(ItemStack.EMPTY));
            assertThrows(NullPointerException.class, () -> new PartAssemblerRecipeInput(null, dummyGrid));
            assertThrows(NullPointerException.class, () -> new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), (CraftingInput) null));
        }
    }

    // =========================================================================
    // 2. Recipe Matching Tests
    // =========================================================================

    @Nested
    @DisplayName("2. Pattern and Schematic Matching")
    class RecipeMatchingTests {

        @Test
        @DisplayName("Matches returns true for valid schematic and matching 3x3 pattern")
        void testRecipeMatchingSuccess() {
            PartAssemblerRecipe recipe = createSampleRecipe(
                    Optional.empty(),
                    new ItemStack(Items.DIAMOND),
                    ItemStack.EMPTY
            );

            PartAssemblerRecipeInput validInput = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    createGrid9(new ItemStack(Items.IRON_INGOT))
            );

            assertTrue(recipe.matches(validInput, null), "Recipe must match when schematic and grid match exactly");
        }

        @Test
        @DisplayName("Matches returns false when schematic item does not match")
        void testRecipeMatchingWrongSchematic() {
            PartAssemblerRecipe recipe = createSampleRecipe(
                    Optional.empty(),
                    new ItemStack(Items.DIAMOND),
                    ItemStack.EMPTY
            );

            PartAssemblerRecipeInput wrongSchematic = new PartAssemblerRecipeInput(
                    new ItemStack(Items.STICK),
                    3,
                    3,
                    createGrid9(new ItemStack(Items.IRON_INGOT))
            );
            assertFalse(recipe.matches(wrongSchematic, null), "Recipe must reject mismatched schematic item");

            PartAssemblerRecipeInput emptySchematic = new PartAssemblerRecipeInput(
                    ItemStack.EMPTY,
                    3,
                    3,
                    createGrid9(new ItemStack(Items.IRON_INGOT))
            );
            assertFalse(recipe.matches(emptySchematic, null), "Recipe must reject empty schematic");
        }

        @Test
        @DisplayName("Matches returns false when crafting grid is incomplete or contains wrong items")
        void testRecipeMatchingWrongGrid() {
            PartAssemblerRecipe recipe = createSampleRecipe(
                    Optional.empty(),
                    new ItemStack(Items.DIAMOND),
                    ItemStack.EMPTY
            );

            // 1. Missing item in grid
            List<ItemStack> missingOne = createGrid9(new ItemStack(Items.IRON_INGOT));
            missingOne.set(4, ItemStack.EMPTY);
            PartAssemblerRecipeInput missingInput = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    missingOne
            );
            assertFalse(recipe.matches(missingInput, null), "Recipe must reject grid with missing item");

            // 2. Wrong material in one slot
            List<ItemStack> wrongMaterial = createGrid9(new ItemStack(Items.IRON_INGOT));
            wrongMaterial.set(0, new ItemStack(Items.COPPER_INGOT));
            PartAssemblerRecipeInput wrongMaterialInput = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    wrongMaterial
            );
            assertFalse(recipe.matches(wrongMaterialInput, null), "Recipe must reject grid with wrong item");
        }

        @Test
        @DisplayName("Asymmetric recipe pattern accurately enforces spatial arrangement")
        void testAsymmetricPatternMatching() {
            PartAssemblerRecipe asymmetricRecipe = new PartAssemblerRecipe(
                    "asymmetric",
                    Ingredient.of(Items.PAPER),
                    createAsymmetricPattern(), // 8 iron around 1 center copper
                    new ItemStack(Items.NETHER_STAR),
                    ItemStack.EMPTY,
                    100,
                    Optional.empty()
            );

            List<ItemStack> validGrid = createGrid9(new ItemStack(Items.IRON_INGOT));
            validGrid.set(4, new ItemStack(Items.COPPER_INGOT)); // Center slot
            PartAssemblerRecipeInput matchingInput = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    validGrid
            );
            assertTrue(asymmetricRecipe.matches(matchingInput, null), "Asymmetric pattern must match correct center copper");

            List<ItemStack> shiftedGrid = createGrid9(new ItemStack(Items.IRON_INGOT));
            shiftedGrid.set(0, new ItemStack(Items.COPPER_INGOT)); // Top-left slot instead of center
            PartAssemblerRecipeInput shiftedInput = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    shiftedGrid
            );
            assertFalse(asymmetricRecipe.matches(shiftedInput, null), "Asymmetric pattern must reject shifted copper position");
        }
    }

    // =========================================================================
    // 3. Assemble & DataComponent Attachment Tests
    // =========================================================================

    @Nested
    @DisplayName("3. Assembly & DataComponent Attachment")
    class AssemblyTests {

        private final RocketPartStats sampleStats = new RocketPartStats(
                1200.0,
                3500.0,
                500000L,
                3.5,
                1800.0
        );

        @Test
        @DisplayName("assemble attaches ROCKET_PART_STATS and pristine PART_INTEGRITY to result stack")
        void testAssembleAttachesDataComponents() {
            PartAssemblerRecipe recipe = createSampleRecipe(
                    Optional.of(sampleStats),
                    new ItemStack(Items.IRON_BLOCK),
                    ItemStack.EMPTY
            );

            PartAssemblerRecipeInput input = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    createGrid9(new ItemStack(Items.IRON_INGOT))
            );

            ItemStack assembled = recipe.assemble(input, registryAccess);

            assertNotNull(assembled);
            assertEquals(Items.IRON_BLOCK, assembled.getItem());
            assertTrue(assembled.has(GCDataComponents.ROCKET_PART_STATS), "Must attach ROCKET_PART_STATS");
            assertEquals(sampleStats, assembled.get(GCDataComponents.ROCKET_PART_STATS));

            assertTrue(assembled.has(GCDataComponents.PART_INTEGRITY), "Must attach PART_INTEGRITY");
            PartIntegrity integrity = assembled.get(GCDataComponents.PART_INTEGRITY);
            assertNotNull(integrity);
            assertEquals(100.0, integrity.currentIntegrity(), EPSILON);
            assertEquals(100.0, integrity.maxIntegrity(), EPSILON);
            assertSame(PartDamageState.NOMINAL, integrity.damageState());
            assertTrue(integrity.isPristine());
        }

        @Test
        @DisplayName("assemble without partStats does not attach components to clean result stack")
        void testAssembleWithoutStatsDoesNotAttachComponents() {
            PartAssemblerRecipe recipe = createSampleRecipe(
                    Optional.empty(),
                    new ItemStack(Items.IRON_BLOCK),
                    ItemStack.EMPTY
            );

            PartAssemblerRecipeInput input = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    createGrid9(new ItemStack(Items.IRON_INGOT))
            );

            ItemStack assembled = recipe.assemble(input, registryAccess);

            assertNotNull(assembled);
            assertFalse(assembled.has(GCDataComponents.ROCKET_PART_STATS), "Must not attach stats when Optional is empty");
            assertFalse(assembled.has(GCDataComponents.PART_INTEGRITY), "Must not attach integrity when no stats present");
        }

        @Test
        @DisplayName("assemble preserves pre-existing PART_INTEGRITY on template result stack")
        void testAssemblePreservesExistingIntegrity() {
            ItemStack customTemplate = new ItemStack(Items.IRON_BLOCK);
            PartIntegrity damagedIntegrity = PartIntegrity.pristine(100.0).damage(25.0); // 75.0 HP (WORN)
            customTemplate.set(GCDataComponents.PART_INTEGRITY, damagedIntegrity);

            PartAssemblerRecipe recipe = createSampleRecipe(
                    Optional.of(sampleStats),
                    customTemplate,
                    ItemStack.EMPTY
            );

            PartAssemblerRecipeInput input = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    createGrid9(new ItemStack(Items.IRON_INGOT))
            );

            ItemStack assembled = recipe.assemble(input, registryAccess);

            assertTrue(assembled.has(GCDataComponents.PART_INTEGRITY));
            PartIntegrity attached = assembled.get(GCDataComponents.PART_INTEGRITY);
            assertNotNull(attached);
            assertEquals(75.0, attached.currentIntegrity(), EPSILON, "Pre-existing integrity must not be overwritten to 100.0");
            assertSame(PartDamageState.WORN, attached.damageState());
        }

        @Test
        @DisplayName("assemble and getResultItem return defensive copies")
        void testDefensiveCopies() {
            PartAssemblerRecipe recipe = createSampleRecipe(
                    Optional.of(sampleStats),
                    new ItemStack(Items.IRON_BLOCK),
                    ItemStack.EMPTY
            );

            PartAssemblerRecipeInput input = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    createGrid9(new ItemStack(Items.IRON_INGOT))
            );

            ItemStack assembled1 = recipe.assemble(input, registryAccess);
            assembled1.setCount(5);

            ItemStack assembled2 = recipe.assemble(input, registryAccess);
            assertEquals(1, assembled2.getCount(), "Mutating first assembled stack must not affect second");

            ItemStack preview = recipe.getResultItem(registryAccess);
            assertEquals(1, preview.getCount());
            assertEquals(sampleStats, preview.get(GCDataComponents.ROCKET_PART_STATS));
            assertTrue(preview.has(GCDataComponents.PART_INTEGRITY));
        }
    }

    // =========================================================================
    // 4. Scrap Byproduct & Recipe Attributes Tests
    // =========================================================================

    @Nested
    @DisplayName("4. Scrap Byproduct & Recipe Attributes")
    class ScrapAndAttributeTests {

        @Test
        @DisplayName("Scrap byproduct stack is returned correctly and defensively copied")
        void testScrapByproduct() {
            ItemStack scrap = new ItemStack(Items.IRON_NUGGET, 3);
            PartAssemblerRecipe recipe = createSampleRecipe(
                    Optional.empty(),
                    new ItemStack(Items.IRON_BLOCK),
                    scrap
            );

            ItemStack retrieved = recipe.getScrapByproduct();
            assertEquals(Items.IRON_NUGGET, retrieved.getItem());
            assertEquals(3, retrieved.getCount());

            retrieved.setCount(10);
            assertEquals(3, recipe.getScrapByproduct().getCount(), "Scrap byproduct must be defensively copied");
        }

        @Test
        @DisplayName("Scrap byproduct defaults to EMPTY when null is supplied")
        void testScrapByproductDefault() {
            PartAssemblerRecipe recipe = new PartAssemblerRecipe(
                    "group",
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                    new ItemStack(Items.IRON_BLOCK),
                    null, // null scrap
                    100,
                    Optional.empty()
            );

            assertTrue(recipe.getScrapByproduct().isEmpty(), "Null scrap must default to ItemStack.EMPTY");
        }

        @Test
        @DisplayName("Assembly time and group defaults and clamping")
        void testAssemblyTimeAndGroup() {
            PartAssemblerRecipe clamped = new PartAssemblerRecipe(
                    null,
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                    new ItemStack(Items.IRON_BLOCK),
                    ItemStack.EMPTY,
                    -50, // Non-positive time
                    null
            );

            assertEquals("", clamped.getGroup(), "Null group must default to empty string");
            assertEquals(1, clamped.getAssemblyTime(), "Non-positive assembly time must be clamped to at least 1");
            assertTrue(clamped.getPartStats().isEmpty(), "Null stats must default to Optional.empty()");

            assertSame(GCRecipes.PART_ASSEMBLER_TYPE, clamped.getType());
            assertSame(GCRecipes.PART_ASSEMBLER_SERIALIZER, clamped.getSerializer());
            assertTrue(clamped.canCraftInDimensions(3, 3));
            assertFalse(clamped.canCraftInDimensions(2, 2));

            NonNullList<Ingredient> ingredients = clamped.getIngredients();
            assertEquals(10, ingredients.size(), "Ingredients must contain schematic + 9 pattern ingredients");
            assertSame(clamped.getSchematic(), ingredients.get(0));
        }
    }

    // =========================================================================
    // 5. Mojang Codec Serialization Tests
    // =========================================================================

    @Nested
    @DisplayName("5. Mojang Codec Serialization (JsonOps & RegistryOps)")
    class CodecTests {

        @Test
        @DisplayName("Full recipe with stats and scrap roundtrips via MapCodec")
        void testCodecFullRecipeRoundtrip() {
            RocketPartStats stats = new RocketPartStats(1500.0, 2800.0, 450000L, 2.8, 1600.0);
            ItemStack result = new ItemStack(Items.DIAMOND_BLOCK, 1);
            ItemStack scrap = new ItemStack(Items.IRON_NUGGET, 4);

            PartAssemblerRecipe original = new PartAssemblerRecipe(
                    "tier1_engines",
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                    result,
                    scrap,
                    250,
                    Optional.of(stats)
            );

            JsonElement json = PartAssemblerRecipe.Serializer.CODEC.codec()
                    .encodeStart(registryOps, original)
                    .getOrThrow(AssertionError::new);

            assertTrue(json.isJsonObject(), "Encoded recipe must be a JSON object");
            JsonObject obj = json.getAsJsonObject();
            assertEquals("tier1_engines", obj.get("group").getAsString());
            assertEquals(250, obj.get("assembly_time").getAsInt());
            assertTrue(obj.has("part_stats"), "Must serialize part_stats");
            assertTrue(obj.has("scrap_byproduct"), "Must serialize scrap_byproduct");

            PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.CODEC.codec()
                    .parse(registryOps, json)
                    .getOrThrow(AssertionError::new);

            assertEquals(original.getGroup(), decoded.getGroup());
            assertEquals(original.getAssemblyTime(), decoded.getAssemblyTime());
            assertEquals(original.getPartStats(), decoded.getPartStats());
            assertEquals(original.getScrapByproduct().getItem(), decoded.getScrapByproduct().getItem());
            assertEquals(original.getScrapByproduct().getCount(), decoded.getScrapByproduct().getCount());
            assertEquals(original.getResultItem(registryAccess).getItem(), decoded.getResultItem(registryAccess).getItem());
        }

        @Test
        @DisplayName("Minimal recipe with optional fields omitted roundtrips via MapCodec")
        void testCodecMinimalRecipeRoundtrip() {
            PartAssemblerRecipe minimal = new PartAssemblerRecipe(
                    "",
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                    new ItemStack(Items.GOLD_INGOT),
                    ItemStack.EMPTY,
                    200,
                    Optional.empty()
            );

            JsonElement json = PartAssemblerRecipe.Serializer.CODEC.codec()
                    .encodeStart(registryOps, minimal)
                    .getOrThrow(AssertionError::new);

            PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.CODEC.codec()
                    .parse(registryOps, json)
                    .getOrThrow(AssertionError::new);

            assertEquals("", decoded.getGroup());
            assertEquals(200, decoded.getAssemblyTime());
            assertTrue(decoded.getPartStats().isEmpty());
            assertTrue(decoded.getScrapByproduct().isEmpty());
        }
    }

    // =========================================================================
    // 6. Network StreamCodec Roundtrip Tests
    // =========================================================================

    @Nested
    @DisplayName("6. Network StreamCodec Serialization")
    class StreamCodecTests {

        @Test
        @DisplayName("Full recipe with stats roundtrips over RegistryFriendlyByteBuf")
        void testStreamCodecFullRoundtrip() {
            RocketPartStats stats = new RocketPartStats(900.0, 1500.0, 200000L, 1.8, 1200.0);
            PartAssemblerRecipe original = new PartAssemblerRecipe(
                    "tier2_boosters",
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                    new ItemStack(Items.NETHERITE_INGOT),
                    new ItemStack(Items.IRON_NUGGET, 2),
                    180,
                    Optional.of(stats)
            );

            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            try {
                PartAssemblerRecipe.Serializer.STREAM_CODEC.encode(buf, original);
                assertTrue(buf.readableBytes() > 0, "Buffer should contain encoded recipe bytes");

                PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.STREAM_CODEC.decode(buf);
                assertEquals(0, buf.readableBytes(), "StreamCodec should consume all encoded bytes");

                assertEquals(original.getGroup(), decoded.getGroup());
                assertEquals(original.getAssemblyTime(), decoded.getAssemblyTime());
                assertEquals(original.getPartStats(), decoded.getPartStats());
                assertEquals(original.getScrapByproduct().getItem(), decoded.getScrapByproduct().getItem());
                assertEquals(original.getScrapByproduct().getCount(), decoded.getScrapByproduct().getCount());
                assertEquals(original.getResultItem(registryAccess).getItem(), decoded.getResultItem(registryAccess).getItem());
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("Recipe without stats roundtrips over RegistryFriendlyByteBuf")
        void testStreamCodecWithoutStatsRoundtrip() {
            PartAssemblerRecipe original = new PartAssemblerRecipe(
                    "basic_parts",
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.COPPER_INGOT)),
                    new ItemStack(Items.COPPER_BLOCK),
                    ItemStack.EMPTY,
                    120,
                    Optional.empty()
            );

            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            try {
                PartAssemblerRecipe.Serializer.STREAM_CODEC.encode(buf, original);
                assertTrue(buf.readableBytes() > 0);

                PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.STREAM_CODEC.decode(buf);
                assertEquals(0, buf.readableBytes());

                assertEquals(original.getGroup(), decoded.getGroup());
                assertEquals(original.getAssemblyTime(), decoded.getAssemblyTime());
                assertTrue(decoded.getPartStats().isEmpty());
                assertTrue(decoded.getScrapByproduct().isEmpty());
            } finally {
                buf.release();
            }
        }
    }

    // =========================================================================
    // 7. Adversarial & Edge Case Stress Tests
    // =========================================================================

    @Nested
    @DisplayName("7. Adversarial & Edge Case Stress Tests")
    class AdversarialStressTests {

        private final RocketPartStats stats1 = new RocketPartStats(1000.0, 2000.0, 300000L, 2.5, 1500.0);
        private final RocketPartStats stats2 = new RocketPartStats(1500.0, 2500.0, 400000L, 3.0, 1800.0);

        @Test
        @DisplayName("PartAssemblerRecipeInput handles extreme, negative, and out-of-bounds indices safely")
        void testRecipeInputExtremeIndices() {
            ItemStack schematic = new ItemStack(Items.PAPER);
            List<ItemStack> gridItems = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) {
                gridItems.add(new ItemStack(Items.IRON_INGOT, i + 1));
            }

            PartAssemblerRecipeInput input = new PartAssemblerRecipeInput(schematic, 3, 3, gridItems);

            // Extreme negative numbers
            assertTrue(input.getItem(Integer.MIN_VALUE).isEmpty(), "Integer.MIN_VALUE must return EMPTY without throwing");
            assertTrue(input.getItem(Integer.MIN_VALUE + 1).isEmpty(), "Integer.MIN_VALUE + 1 must return EMPTY");
            assertTrue(input.getItem(-1).isEmpty(), "Index -1 must return EMPTY");
            assertTrue(input.getItem(-50).isEmpty(), "Index -50 must return EMPTY");

            // Valid indices
            assertSame(schematic, input.getItem(0), "Index 0 must return schematic");
            for (int i = 1; i <= 9; i++) {
                assertSame(gridItems.get(i - 1), input.getItem(i));
            }

            // Boundary and extreme positive numbers
            assertTrue(input.getItem(10).isEmpty(), "Index 10 (size) must return EMPTY");
            assertTrue(input.getItem(11).isEmpty(), "Index 11 must return EMPTY");
            assertTrue(input.getItem(100).isEmpty(), "Index 100 must return EMPTY");
            assertTrue(input.getItem(Integer.MAX_VALUE - 1).isEmpty(), "Integer.MAX_VALUE - 1 must return EMPTY");
            assertTrue(input.getItem(Integer.MAX_VALUE).isEmpty(), "Integer.MAX_VALUE must return EMPTY without throwing");
        }

        @Test
        @DisplayName("PartAssemblerRecipeInput handles non-standard dimensions (1x1, 2x2, empty)")
        void testRecipeInputNonStandardDimensions() {
            ItemStack schematic = new ItemStack(Items.PAPER);

            // 1x1 input
            ItemStack singleItem = new ItemStack(Items.IRON_INGOT);
            PartAssemblerRecipeInput input1x1 = new PartAssemblerRecipeInput(schematic, 1, 1, List.of(singleItem));
            assertEquals(2, input1x1.size(), "Size must be 1 (schematic) + 1 (grid) = 2");
            assertSame(schematic, input1x1.getItem(0));
            assertSame(singleItem, input1x1.getItem(1));
            assertTrue(input1x1.getItem(2).isEmpty());
            assertTrue(input1x1.getItem(-1).isEmpty());
            assertTrue(input1x1.getItem(Integer.MIN_VALUE).isEmpty());

            // 2x2 input
            List<ItemStack> items2x2 = List.of(
                    new ItemStack(Items.IRON_INGOT, 1),
                    new ItemStack(Items.IRON_INGOT, 2),
                    new ItemStack(Items.IRON_INGOT, 3),
                    new ItemStack(Items.IRON_INGOT, 4)
            );
            PartAssemblerRecipeInput input2x2 = new PartAssemblerRecipeInput(schematic, 2, 2, items2x2);
            assertEquals(5, input2x2.size(), "Size must be 1 (schematic) + 4 (grid) = 5");
            assertSame(schematic, input2x2.getItem(0));
            for (int i = 0; i < 4; i++) {
                assertSame(items2x2.get(i), input2x2.getItem(i + 1));
            }
            assertTrue(input2x2.getItem(5).isEmpty());

            // 0x0 empty input
            PartAssemblerRecipeInput input0x0 = new PartAssemblerRecipeInput(schematic, 0, 0, List.of());
            assertEquals(1, input0x0.size());
            assertSame(schematic, input0x0.getItem(0));
            assertTrue(input0x0.getItem(1).isEmpty());
        }

        @Test
        @DisplayName("PartAssemblerRecipeInput backing list semantics and immutable copy isolation")
        void testRecipeInputListMutation() {
            ItemStack schematic = new ItemStack(Items.PAPER);
            List<ItemStack> mutableList = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) {
                mutableList.add(new ItemStack(Items.IRON_INGOT));
            }

            PartAssemblerRecipeInput liveInput = new PartAssemblerRecipeInput(schematic, 3, 3, mutableList);
            assertEquals(Items.IRON_INGOT, liveInput.getItem(1).getItem());

            // Vanilla CraftingInput.of retains the backing list reference (required by MachineLib ItemList live-view)
            mutableList.set(0, new ItemStack(Items.DIRT));
            assertEquals(Items.DIRT, liveInput.getItem(1).getItem(),
                    "CraftingInput.of retains backing list reference, enabling MachineLib ItemList live slot polling");

            // When isolated input is required, passing an unmodifiable copy guarantees immutability
            List<ItemStack> isolatedList = List.copyOf(mutableList);
            PartAssemblerRecipeInput isolatedInput = new PartAssemblerRecipeInput(schematic, 3, 3, isolatedList);
            assertEquals(Items.DIRT, isolatedInput.getItem(1).getItem());
            assertThrows(UnsupportedOperationException.class, () -> isolatedList.set(0, new ItemStack(Items.GOLD_INGOT)),
                    "List.copyOf produces an unmodifiable backing list");
        }

        @Test
        @DisplayName("Hollow ring pattern matches valid input and rejects unexpected center or missing items")
        void testHollowRingPatternMatching() {
            // 8 Iron Ingots in a ring around empty center
            Map<Character, Ingredient> key = Map.of('I', Ingredient.of(Items.IRON_INGOT));
            ShapedRecipePattern hollowPattern = ShapedRecipePattern.of(
                    key,
                    "III",
                    "I I",
                    "III"
            );

            PartAssemblerRecipe recipe = new PartAssemblerRecipe(
                    "hollow",
                    Ingredient.of(Items.PAPER),
                    hollowPattern,
                    new ItemStack(Items.IRON_BLOCK),
                    ItemStack.EMPTY,
                    100,
                    Optional.empty()
            );

            // Valid grid: center slot (index 4) empty
            List<ItemStack> validGrid = createGrid9(new ItemStack(Items.IRON_INGOT));
            validGrid.set(4, ItemStack.EMPTY);
            PartAssemblerRecipeInput validInput = new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, validGrid);
            assertTrue(recipe.matches(validInput, null), "Hollow ring must match when center is empty");

            // Invalid grid: center slot occupied by copper
            List<ItemStack> centerOccupied = createGrid9(new ItemStack(Items.IRON_INGOT));
            centerOccupied.set(4, new ItemStack(Items.COPPER_INGOT));
            PartAssemblerRecipeInput centerOccupiedInput = new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, centerOccupied);
            assertFalse(recipe.matches(centerOccupiedInput, null), "Hollow ring must reject recipe when center slot is occupied");

            // Invalid grid: missing outer item
            List<ItemStack> missingOuter = createGrid9(new ItemStack(Items.IRON_INGOT));
            missingOuter.set(0, ItemStack.EMPTY);
            missingOuter.set(4, ItemStack.EMPTY);
            PartAssemblerRecipeInput missingOuterInput = new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, missingOuter);
            assertFalse(recipe.matches(missingOuterInput, null), "Hollow ring must reject recipe when outer item is missing");
        }

        @Test
        @DisplayName("Plus/Cross pattern matches valid input and rejects extra corner items")
        void testPlusPatternMatching() {
            // Plus pattern: center and cardinal directions
            Map<Character, Ingredient> key = Map.of('X', Ingredient.of(Items.GOLD_INGOT));
            ShapedRecipePattern plusPattern = ShapedRecipePattern.of(
                    key,
                    " X ",
                    "XXX",
                    " X "
            );

            PartAssemblerRecipe recipe = new PartAssemblerRecipe(
                    "plus",
                    Ingredient.of(Items.PAPER),
                    plusPattern,
                    new ItemStack(Items.GOLD_BLOCK),
                    ItemStack.EMPTY,
                    100,
                    Optional.empty()
            );

            // Valid grid: slots 1, 3, 4, 5, 7 have Gold; corners (0, 2, 6, 8) empty
            List<ItemStack> validGrid = createGrid9(ItemStack.EMPTY);
            validGrid.set(1, new ItemStack(Items.GOLD_INGOT));
            validGrid.set(3, new ItemStack(Items.GOLD_INGOT));
            validGrid.set(4, new ItemStack(Items.GOLD_INGOT));
            validGrid.set(5, new ItemStack(Items.GOLD_INGOT));
            validGrid.set(7, new ItemStack(Items.GOLD_INGOT));
            PartAssemblerRecipeInput validInput = new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, validGrid);
            assertTrue(recipe.matches(validInput, null), "Plus pattern must match when only plus slots are filled");

            // Extra item in top-left corner
            List<ItemStack> extraCorner = new ArrayList<>(validGrid);
            extraCorner.set(0, new ItemStack(Items.DIRT));
            PartAssemblerRecipeInput extraCornerInput = new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, extraCorner);
            assertFalse(recipe.matches(extraCornerInput, null), "Plus pattern must reject extra item in corner");
        }

        @Test
        @DisplayName("2x2 pattern inside 3x3 grid matches in all 4 quadrants and rejects extraneous items")
        void test2x2PatternIn3x3Grid() {
            Map<Character, Ingredient> key = Map.of('G', Ingredient.of(Items.GOLD_INGOT));
            ShapedRecipePattern pattern2x2 = ShapedRecipePattern.of(
                    key,
                    "GG",
                    "GG"
            );

            PartAssemblerRecipe recipe = new PartAssemblerRecipe(
                    "2x2",
                    Ingredient.of(Items.PAPER),
                    pattern2x2,
                    new ItemStack(Items.GOLD_BLOCK),
                    ItemStack.EMPTY,
                    100,
                    Optional.empty()
            );

            // Quadrant 1: Top-Left (slots 0, 1, 3, 4)
            List<ItemStack> q1 = createGrid9(ItemStack.EMPTY);
            q1.set(0, new ItemStack(Items.GOLD_INGOT));
            q1.set(1, new ItemStack(Items.GOLD_INGOT));
            q1.set(3, new ItemStack(Items.GOLD_INGOT));
            q1.set(4, new ItemStack(Items.GOLD_INGOT));
            assertTrue(recipe.matches(new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, q1), null), "Top-Left 2x2 must match");

            // Quadrant 2: Top-Right (slots 1, 2, 4, 5)
            List<ItemStack> q2 = createGrid9(ItemStack.EMPTY);
            q2.set(1, new ItemStack(Items.GOLD_INGOT));
            q2.set(2, new ItemStack(Items.GOLD_INGOT));
            q2.set(4, new ItemStack(Items.GOLD_INGOT));
            q2.set(5, new ItemStack(Items.GOLD_INGOT));
            assertTrue(recipe.matches(new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, q2), null), "Top-Right 2x2 must match");

            // Quadrant 3: Bottom-Left (slots 3, 4, 6, 7)
            List<ItemStack> q3 = createGrid9(ItemStack.EMPTY);
            q3.set(3, new ItemStack(Items.GOLD_INGOT));
            q3.set(4, new ItemStack(Items.GOLD_INGOT));
            q3.set(6, new ItemStack(Items.GOLD_INGOT));
            q3.set(7, new ItemStack(Items.GOLD_INGOT));
            assertTrue(recipe.matches(new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, q3), null), "Bottom-Left 2x2 must match");

            // Quadrant 4: Bottom-Right (slots 4, 5, 7, 8)
            List<ItemStack> q4 = createGrid9(ItemStack.EMPTY);
            q4.set(4, new ItemStack(Items.GOLD_INGOT));
            q4.set(5, new ItemStack(Items.GOLD_INGOT));
            q4.set(7, new ItemStack(Items.GOLD_INGOT));
            q4.set(8, new ItemStack(Items.GOLD_INGOT));
            assertTrue(recipe.matches(new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, q4), null), "Bottom-Right 2x2 must match");

            // Rejection: 2x2 with extraneous item outside
            List<ItemStack> withExtra = new ArrayList<>(q1);
            withExtra.set(8, new ItemStack(Items.IRON_INGOT));
            assertFalse(recipe.matches(new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 3, 3, withExtra), null), "Must reject extraneous item outside 2x2");
        }

        @Test
        @DisplayName("Horizontally mirrored asymmetric pattern matches both orientations")
        void testHorizontallyMirroredPattern() {
            Map<Character, Ingredient> key = Map.of(
                    'A', Ingredient.of(Items.IRON_INGOT),
                    'B', Ingredient.of(Items.GOLD_INGOT)
            );
            // 2x2 diagonal
            ShapedRecipePattern pattern = ShapedRecipePattern.of(
                    key,
                    "A ",
                    " B"
            );

            PartAssemblerRecipe recipe = new PartAssemblerRecipe(
                    "mirror",
                    Ingredient.of(Items.PAPER),
                    pattern,
                    new ItemStack(Items.DIAMOND),
                    ItemStack.EMPTY,
                    100,
                    Optional.empty()
            );

            // Normal orientation in 2x2:
            // A .
            // . B
            List<ItemStack> normal2x2 = List.of(
                    new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
                    ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT)
            );
            assertTrue(recipe.matches(new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 2, 2, normal2x2), null), "Normal orientation must match");

            // Mirrored orientation in 2x2:
            // . A
            // B .
            List<ItemStack> mirrored2x2 = List.of(
                    ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY
            );
            assertTrue(recipe.matches(new PartAssemblerRecipeInput(new ItemStack(Items.PAPER), 2, 2, mirrored2x2), null), "Horizontally mirrored orientation must match");
        }

        @Test
        @DisplayName("DataComponent matrix: Stats override template stats, and pre-existing integrity is preserved")
        void testDataComponentIntegrityMatrix() {
            PartAssemblerRecipeInput input = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3,
                    3,
                    createGrid9(new ItemStack(Items.IRON_INGOT))
            );

            // Case 1: Recipe has stats2, template already has stats1 -> stats2 overrides stats1, integrity is pristine
            ItemStack templateWithStats1 = new ItemStack(Items.IRON_BLOCK);
            templateWithStats1.set(GCDataComponents.ROCKET_PART_STATS, stats1);
            PartAssemblerRecipe recipeWithStats2 = createSampleRecipe(Optional.of(stats2), templateWithStats1, ItemStack.EMPTY);
            ItemStack assembled1 = recipeWithStats2.assemble(input, registryAccess);
            assertEquals(stats2, assembled1.get(GCDataComponents.ROCKET_PART_STATS), "Recipe stats must override template stats");
            assertTrue(assembled1.has(GCDataComponents.PART_INTEGRITY));
            assertTrue(assembled1.get(GCDataComponents.PART_INTEGRITY).isPristine());

            // Case 2: Recipe has stats2, template has stats1 AND damaged integrity (40.0, DAMAGED) -> stats2 overrides stats1, damaged integrity preserved!
            ItemStack templateDamaged = new ItemStack(Items.IRON_BLOCK);
            templateDamaged.set(GCDataComponents.ROCKET_PART_STATS, stats1);
            PartIntegrity customDamaged = PartIntegrity.pristine(100.0).damage(60.0); // 40.0 remaining -> DAMAGED
            templateDamaged.set(GCDataComponents.PART_INTEGRITY, customDamaged);
            PartAssemblerRecipe recipeWithStatsDamaged = createSampleRecipe(Optional.of(stats2), templateDamaged, ItemStack.EMPTY);
            ItemStack assembled2 = recipeWithStatsDamaged.assemble(input, registryAccess);
            assertEquals(stats2, assembled2.get(GCDataComponents.ROCKET_PART_STATS));
            assertEquals(customDamaged, assembled2.get(GCDataComponents.PART_INTEGRITY), "Pre-existing damaged integrity must be preserved even when recipe provides stats");

            // Case 3: Recipe has NO stats, template has stats1 and NO integrity -> template stats preserved, pristine integrity attached
            ItemStack templateWithStatsOnly = new ItemStack(Items.IRON_BLOCK);
            templateWithStatsOnly.set(GCDataComponents.ROCKET_PART_STATS, stats1);
            PartAssemblerRecipe recipeNoStats = createSampleRecipe(Optional.empty(), templateWithStatsOnly, ItemStack.EMPTY);
            ItemStack assembled3 = recipeNoStats.assemble(input, registryAccess);
            assertEquals(stats1, assembled3.get(GCDataComponents.ROCKET_PART_STATS));
            assertTrue(assembled3.has(GCDataComponents.PART_INTEGRITY), "Pristine integrity must be attached when template has stats");
            assertTrue(assembled3.get(GCDataComponents.PART_INTEGRITY).isPristine());

            // Case 4: Recipe has NO stats, template has NO stats, template has damaged integrity -> integrity preserved, no stats
            ItemStack templateOnlyIntegrity = new ItemStack(Items.IRON_BLOCK);
            templateOnlyIntegrity.set(GCDataComponents.PART_INTEGRITY, customDamaged);
            PartAssemblerRecipe recipeOnlyIntegrity = createSampleRecipe(Optional.empty(), templateOnlyIntegrity, ItemStack.EMPTY);
            ItemStack assembled4 = recipeOnlyIntegrity.assemble(input, registryAccess);
            assertFalse(assembled4.has(GCDataComponents.ROCKET_PART_STATS));
            assertEquals(customDamaged, assembled4.get(GCDataComponents.PART_INTEGRITY));
        }

        @Test
        @DisplayName("Extreme assembly_time values are clamped and roundtrip cleanly")
        void testExtremeAssemblyTimeValues() {
            // Negative values and 0 clamp to 1
            PartAssemblerRecipe r0 = createSampleRecipeWithTime(0);
            assertEquals(1, r0.getAssemblyTime(), "0 must clamp to 1");

            PartAssemblerRecipe rNeg = createSampleRecipeWithTime(-1);
            assertEquals(1, rNeg.getAssemblyTime(), "-1 must clamp to 1");

            PartAssemblerRecipe rMin = createSampleRecipeWithTime(Integer.MIN_VALUE);
            assertEquals(1, rMin.getAssemblyTime(), "Integer.MIN_VALUE must clamp to 1");

            // Integer.MAX_VALUE preserved
            PartAssemblerRecipe rMax = createSampleRecipeWithTime(Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, rMax.getAssemblyTime(), "Integer.MAX_VALUE must be preserved");

            // StreamCodec roundtrip with Integer.MAX_VALUE
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            try {
                PartAssemblerRecipe.Serializer.STREAM_CODEC.encode(buf, rMax);
                PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.STREAM_CODEC.decode(buf);
                assertEquals(Integer.MAX_VALUE, decoded.getAssemblyTime(), "Integer.MAX_VALUE must roundtrip through StreamCodec");
            } finally {
                buf.release();
            }

            // StreamCodec roundtrip with 1
            RegistryFriendlyByteBuf buf1 = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            try {
                PartAssemblerRecipe.Serializer.STREAM_CODEC.encode(buf1, r0);
                PartAssemblerRecipe decoded1 = PartAssemblerRecipe.Serializer.STREAM_CODEC.decode(buf1);
                assertEquals(1, decoded1.getAssemblyTime(), "1 must roundtrip through StreamCodec");
            } finally {
                buf1.release();
            }
        }

        @Test
        @DisplayName("MapCodec parses recipes with negative assembly_time and clamps to 1")
        void testMapCodecNegativeAssemblyTimeClamping() {
            PartAssemblerRecipe recipe = createSampleRecipeWithTime(-500);
            JsonElement json = PartAssemblerRecipe.Serializer.CODEC.codec()
                    .encodeStart(registryOps, recipe)
                    .getOrThrow(AssertionError::new);

            assertEquals(1, json.getAsJsonObject().get("assembly_time").getAsInt());

            // If JSON explicitly has -500 in assembly_time:
            JsonObject modifiedJson = json.getAsJsonObject().deepCopy();
            modifiedJson.addProperty("assembly_time", -500);

            PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.CODEC.codec()
                    .parse(registryOps, modifiedJson)
                    .getOrThrow(AssertionError::new);

            assertEquals(1, decoded.getAssemblyTime(), "Decoded recipe with negative assembly_time must clamp to 1");
        }

        @Test
        @DisplayName("StreamCodec roundtrips UTF-8 unicode group strings and large scrap stacks")
        void testStreamCodecUnicodeAndLargeScrap() {
            ItemStack largeScrap = new ItemStack(Items.IRON_NUGGET, 64);
            PartAssemblerRecipe unicodeRecipe = new PartAssemblerRecipe(
                    "special_\u2605_\uD83D\uDE80_engines",
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                    new ItemStack(Items.IRON_BLOCK),
                    largeScrap,
                    350,
                    Optional.of(stats1)
            );

            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            try {
                PartAssemblerRecipe.Serializer.STREAM_CODEC.encode(buf, unicodeRecipe);
                PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.STREAM_CODEC.decode(buf);

                assertEquals("special_\u2605_\uD83D\uDE80_engines", decoded.getGroup());
                assertEquals(64, decoded.getScrapByproduct().getCount());
                assertEquals(Items.IRON_NUGGET, decoded.getScrapByproduct().getItem());
                assertEquals(350, decoded.getAssemblyTime());
                assertEquals(stats1, decoded.getPartStats().orElseThrow());
            } finally {
                buf.release();
            }
        }

        private static PartAssemblerRecipe createSampleRecipeWithTime(int assemblyTime) {
            return new PartAssemblerRecipe(
                    "test",
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                    new ItemStack(Items.IRON_BLOCK),
                    ItemStack.EMPTY,
                    assemblyTime,
                    Optional.empty()
            );
        }
    }

    // =========================================================================
    // 8. Adversarial Serialization, Codecs, Buffer Leaks & Concurrency Tests
    // =========================================================================

    @Nested
    @DisplayName("8. Adversarial Serialization, Codecs, Buffer Leaks & Concurrency")
    class SerializationAndConcurrencyTests {

        @Test
        @DisplayName("MapCodec rejects JSON payloads missing required fields (schematic, result, pattern)")
        void testMapCodecMissingRequiredFields() {
            // Missing schematic
            JsonObject missingSchematic = new JsonObject();
            missingSchematic.addProperty("group", "test_group");
            missingSchematic.add("pattern", ShapedRecipePattern.MAP_CODEC.codec().encodeStart(registryOps, create3x3Pattern(Ingredient.of(Items.IRON_INGOT))).getOrThrow(AssertionError::new));
            missingSchematic.add("result", ItemStack.STRICT_CODEC.encodeStart(registryOps, new ItemStack(Items.IRON_BLOCK)).getOrThrow(AssertionError::new));

            assertTrue(PartAssemblerRecipe.Serializer.CODEC.codec().parse(registryOps, missingSchematic).isError(),
                    "MapCodec must reject JSON missing 'schematic'");

            // Missing result
            JsonObject missingResult = new JsonObject();
            missingResult.add("schematic", Ingredient.CODEC_NONEMPTY.encodeStart(registryOps, Ingredient.of(Items.PAPER)).getOrThrow(AssertionError::new));
            missingResult.add("pattern", ShapedRecipePattern.MAP_CODEC.codec().encodeStart(registryOps, create3x3Pattern(Ingredient.of(Items.IRON_INGOT))).getOrThrow(AssertionError::new));

            assertTrue(PartAssemblerRecipe.Serializer.CODEC.codec().parse(registryOps, missingResult).isError(),
                    "MapCodec must reject JSON missing 'result'");

            // Missing pattern
            JsonObject missingPattern = new JsonObject();
            missingPattern.add("schematic", Ingredient.CODEC_NONEMPTY.encodeStart(registryOps, Ingredient.of(Items.PAPER)).getOrThrow(AssertionError::new));
            missingPattern.add("result", ItemStack.STRICT_CODEC.encodeStart(registryOps, new ItemStack(Items.IRON_BLOCK)).getOrThrow(AssertionError::new));

            assertTrue(PartAssemblerRecipe.Serializer.CODEC.codec().parse(registryOps, missingPattern).isError(),
                    "MapCodec must reject JSON missing 'pattern'");
        }

        @Test
        @DisplayName("MapCodec rejects invalid item IDs and empty ingredients")
        void testMapCodecInvalidPayloads() {
            // Non-existent item in result
            JsonObject invalidResult = new JsonObject();
            invalidResult.add("schematic", Ingredient.CODEC_NONEMPTY.encodeStart(registryOps, Ingredient.of(Items.PAPER)).getOrThrow(AssertionError::new));
            invalidResult.add("pattern", ShapedRecipePattern.MAP_CODEC.codec().encodeStart(registryOps, create3x3Pattern(Ingredient.of(Items.IRON_INGOT))).getOrThrow(AssertionError::new));
            JsonObject fakeItem = new JsonObject();
            fakeItem.addProperty("id", "galacticraft:non_existent_fake_part_404");
            fakeItem.addProperty("count", 1);
            invalidResult.add("result", fakeItem);

            assertTrue(PartAssemblerRecipe.Serializer.CODEC.codec().parse(registryOps, invalidResult).isError(),
                    "MapCodec must reject non-existent item id in result");

            // Empty schematic ingredient []
            JsonObject emptySchematic = new JsonObject();
            emptySchematic.add("schematic", new JsonArray());
            emptySchematic.add("pattern", ShapedRecipePattern.MAP_CODEC.codec().encodeStart(registryOps, create3x3Pattern(Ingredient.of(Items.IRON_INGOT))).getOrThrow(AssertionError::new));
            emptySchematic.add("result", ItemStack.STRICT_CODEC.encodeStart(registryOps, new ItemStack(Items.IRON_BLOCK)).getOrThrow(AssertionError::new));

            assertTrue(PartAssemblerRecipe.Serializer.CODEC.codec().parse(registryOps, emptySchematic).isError(),
                    "Ingredient.CODEC_NONEMPTY must reject empty array schematic");

            // Corrupted part_stats (primitive string instead of record object)
            JsonObject corruptStats = new JsonObject();
            corruptStats.add("schematic", Ingredient.CODEC_NONEMPTY.encodeStart(registryOps, Ingredient.of(Items.PAPER)).getOrThrow(AssertionError::new));
            corruptStats.add("pattern", ShapedRecipePattern.MAP_CODEC.codec().encodeStart(registryOps, create3x3Pattern(Ingredient.of(Items.IRON_INGOT))).getOrThrow(AssertionError::new));
            corruptStats.add("result", ItemStack.STRICT_CODEC.encodeStart(registryOps, new ItemStack(Items.IRON_BLOCK)).getOrThrow(AssertionError::new));
            corruptStats.addProperty("part_stats", "not_a_valid_stats_object");

            assertTrue(PartAssemblerRecipe.Serializer.CODEC.codec().parse(registryOps, corruptStats).isError(),
                    "RocketPartStats codec must reject string primitive for record");
        }

        @Test
        @DisplayName("StreamCodec cleanly rejects truncated Netty ByteBuf at all boundary offsets")
        void testStreamCodecTruncatedBuffers() {
            RocketPartStats stats = new RocketPartStats(1200.0, 3000.0, 500000L, 3.5, 1800.0);
            PartAssemblerRecipe fullRecipe = new PartAssemblerRecipe(
                    "engine_tier1",
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                    new ItemStack(Items.IRON_BLOCK),
                    new ItemStack(Items.IRON_NUGGET, 2),
                    250,
                    Optional.of(stats)
            );

            // Encode complete recipe to byte array
            RegistryFriendlyByteBuf fullBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            byte[] fullBytes;
            try {
                PartAssemblerRecipe.Serializer.STREAM_CODEC.encode(fullBuf, fullRecipe);
                fullBytes = new byte[fullBuf.readableBytes()];
                fullBuf.readBytes(fullBytes);
            } finally {
                fullBuf.release();
            }

            assertTrue(fullBytes.length > 20, "Encoded recipe must be non-trivial length");

            // Test truncated buffers from 0 bytes up to fullBytes.length - 1
            int[] testCutoffs = {0, 1, 2, 5, 10, 15, 20, fullBytes.length / 2, fullBytes.length - 2, fullBytes.length - 1};
            for (int cutoff : testCutoffs) {
                RegistryFriendlyByteBuf truncated = new RegistryFriendlyByteBuf(
                        Unpooled.wrappedBuffer(fullBytes, 0, cutoff),
                        registryAccess
                );
                try {
                    assertThrows(Exception.class, () -> PartAssemblerRecipe.Serializer.STREAM_CODEC.decode(truncated),
                            "StreamCodec must throw exception when buffer is truncated at " + cutoff + " bytes");
                } finally {
                    truncated.release();
                }
            }
        }

        @Test
        @DisplayName("StreamCodec decodes successfully in presence of extra trailing bytes and preserves buffer boundaries")
        void testStreamCodecExtraTrailingBytes() {
            PartAssemblerRecipe sample = createSampleRecipe(Optional.empty(), new ItemStack(Items.DIAMOND), ItemStack.EMPTY);
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            try {
                PartAssemblerRecipe.Serializer.STREAM_CODEC.encode(buf, sample);
                int encodedLength = buf.readableBytes();
                assertTrue(encodedLength > 0);

                // Append 16 bytes of trailing data
                byte[] trailing = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
                buf.writeBytes(trailing);
                assertEquals(encodedLength + 16, buf.readableBytes());

                PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.STREAM_CODEC.decode(buf);
                assertNotNull(decoded);
                assertEquals(sample.getResultItem(registryAccess).getItem(), decoded.getResultItem(registryAccess).getItem());
                assertEquals(16, buf.readableBytes(), "Exactly 16 trailing bytes must remain unconsumed in buffer");
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("StreamCodec operates with zero ByteBuf reference count leaks")
        void testByteBufReferenceCountIntegrity() {
            PartAssemblerRecipe sample = createSampleRecipe(
                    Optional.of(new RocketPartStats(100.0, 200.0, 1000L, 1.0, 300.0)),
                    new ItemStack(Items.GOLD_BLOCK),
                    new ItemStack(Items.GOLD_NUGGET)
            );

            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            assertEquals(1, buf.refCnt(), "Fresh buffer must have refCnt 1");

            PartAssemblerRecipe.Serializer.STREAM_CODEC.encode(buf, sample);
            assertEquals(1, buf.refCnt(), "Buffer refCnt must remain 1 after encode");

            PartAssemblerRecipe decoded = PartAssemblerRecipe.Serializer.STREAM_CODEC.decode(buf);
            assertNotNull(decoded);
            assertEquals(1, buf.refCnt(), "Buffer refCnt must remain 1 after decode");

            buf.release();
            assertEquals(0, buf.refCnt(), "Buffer must cleanly reach refCnt 0 upon release");
        }

        @Test
        @DisplayName("Concurrent matches() and assemble() across multiple threads on a shared recipe instance are thread-safe")
        void testConcurrentMatchesAndAssemble() throws Exception {
            RocketPartStats stats = new RocketPartStats(2000.0, 4000.0, 600000L, 4.0, 2000.0);
            ItemStack baseResult = new ItemStack(Items.NETHERITE_BLOCK);
            ItemStack baseScrap = new ItemStack(Items.NETHERITE_SCRAP, 2);

            PartAssemblerRecipe sharedRecipe = new PartAssemblerRecipe(
                    "heavy_thrusters",
                    Ingredient.of(Items.PAPER),
                    create3x3Pattern(Ingredient.of(Items.IRON_INGOT)),
                    baseResult,
                    baseScrap,
                    300,
                    Optional.of(stats)
            );

            int threadCount = 16;
            int iterationsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger matchSuccessCount = new AtomicInteger(0);
            AtomicInteger matchFailCount = new AtomicInteger(0);
            AtomicInteger assembleCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            List<Future<?>> futures = new ArrayList<>(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        try {
                            if (threadId % 3 == 0) {
                                // Match test with valid input
                                PartAssemblerRecipeInput matchingInput = new PartAssemblerRecipeInput(
                                        new ItemStack(Items.PAPER),
                                        3, 3,
                                        createGrid9(new ItemStack(Items.IRON_INGOT))
                                );
                                if (sharedRecipe.matches(matchingInput, null)) {
                                    matchSuccessCount.incrementAndGet();
                                } else {
                                    errorCount.incrementAndGet();
                                }
                            } else if (threadId % 3 == 1) {
                                // Match test with invalid input (mismatched schematic)
                                PartAssemblerRecipeInput invalidInput = new PartAssemblerRecipeInput(
                                        new ItemStack(Items.STICK),
                                        3, 3,
                                        createGrid9(new ItemStack(Items.IRON_INGOT))
                                );
                                if (!sharedRecipe.matches(invalidInput, null)) {
                                    matchFailCount.incrementAndGet();
                                } else {
                                    errorCount.incrementAndGet();
                                }
                            } else {
                                // Assemble test
                                PartAssemblerRecipeInput input = new PartAssemblerRecipeInput(
                                        new ItemStack(Items.PAPER),
                                        3, 3,
                                        createGrid9(new ItemStack(Items.IRON_INGOT))
                                );
                                ItemStack assembled = sharedRecipe.assemble(input, registryAccess);
                                if (assembled.getItem() == Items.NETHERITE_BLOCK
                                        && assembled.getCount() == 1
                                        && assembled.has(GCDataComponents.ROCKET_PART_STATS)
                                        && assembled.has(GCDataComponents.PART_INTEGRITY)) {
                                    assembleCount.incrementAndGet();
                                } else {
                                    errorCount.incrementAndGet();
                                }

                                // Aggressively mutate returned stack to verify lack of cross-thread contamination
                                assembled.setCount(64);
                                assembled.remove(GCDataComponents.ROCKET_PART_STATS);
                                assembled.remove(GCDataComponents.PART_INTEGRITY);

                                ItemStack preview = sharedRecipe.getResultItem(registryAccess);
                                assertEquals(1, preview.getCount());
                                assertTrue(preview.has(GCDataComponents.ROCKET_PART_STATS));

                                ItemStack scrap = sharedRecipe.getScrapByproduct();
                                assertEquals(2, scrap.getCount());
                                scrap.setCount(99);
                            }
                        } catch (Throwable th) {
                            errorCount.incrementAndGet();
                            th.printStackTrace();
                        }
                    }
                }));
            }

            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(0, errorCount.get(), "No errors or race conditions should occur during concurrent execution");
            assertTrue(matchSuccessCount.get() > 0, "Concurrent matches should succeed");
            assertTrue(matchFailCount.get() > 0, "Concurrent mismatches should fail cleanly");
            assertTrue(assembleCount.get() > 0, "Concurrent assemblies should succeed without race conditions");
        }

        @Test
        @DisplayName("Constructor without defensive copy preserves external stack reference")
        void testConstructorExternalStackReference() {
            ItemStack mutableResult = new ItemStack(Items.IRON_BLOCK, 1);
            PartAssemblerRecipe recipe = createSampleRecipe(Optional.empty(), mutableResult, ItemStack.EMPTY);

            PartAssemblerRecipeInput input = new PartAssemblerRecipeInput(
                    new ItemStack(Items.PAPER),
                    3, 3,
                    createGrid9(new ItemStack(Items.IRON_INGOT))
            );

            // Initially outputs count 1
            assertEquals(1, recipe.assemble(input, registryAccess).getCount());

            // If external caller mutates original mutableResult stack:
            mutableResult.setCount(16);

            // Confirms whether PartAssemblerRecipe takes a defensive copy in constructor
            int assembledCount = recipe.assemble(input, registryAccess).getCount();
            assertEquals(16, assembledCount, "Documents that PartAssemblerRecipe retains the direct reference passed to constructor");
        }
    }
}
