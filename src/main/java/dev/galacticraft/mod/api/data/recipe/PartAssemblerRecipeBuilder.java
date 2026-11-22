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

package dev.galacticraft.mod.api.data.recipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.mod.recipe.PartAssemblerRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Recipe builder for {@link PartAssemblerRecipe}, configuring a required schematic blueprint,
 * a 3x3 crafting pattern, assembly duration, optional scrap byproduct, and physical
 * {@link RocketPartStats}.
 */
public class PartAssemblerRecipeBuilder extends GCRecipeBuilder<PartAssemblerRecipeBuilder> {
    private final List<String> rows = Lists.newArrayList();
    private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
    private Ingredient schematic;
    private ItemStack scrapByproduct = ItemStack.EMPTY;
    private int assemblyTime = 200;
    @Nullable
    private RocketPartStats partStats;

    protected PartAssemblerRecipeBuilder(ItemLike result, int count) {
        super("part_assembler/", null, result, count);
    }

    public static PartAssemblerRecipeBuilder create(ItemLike result) {
        return new PartAssemblerRecipeBuilder(result, 1);
    }

    public static PartAssemblerRecipeBuilder create(ItemLike result, int count) {
        return new PartAssemblerRecipeBuilder(result, count);
    }

    public PartAssemblerRecipeBuilder schematic(Ingredient schematic) {
        this.schematic = schematic;
        return this;
    }

    public PartAssemblerRecipeBuilder schematic(ItemLike schematic) {
        return this.schematic(Ingredient.of(schematic));
    }

    public PartAssemblerRecipeBuilder schematic(TagKey<Item> schematicTag) {
        return this.schematic(Ingredient.of(schematicTag));
    }

    public PartAssemblerRecipeBuilder define(Character character, TagKey<Item> tagKey) {
        return this.define(character, Ingredient.of(tagKey));
    }

    public PartAssemblerRecipeBuilder define(Character character, ItemLike itemLike) {
        return this.define(character, Ingredient.of(itemLike));
    }

    public PartAssemblerRecipeBuilder define(Character character, Ingredient ingredient) {
        if (this.key.containsKey(character)) {
            throw new IllegalArgumentException("Symbol '" + character + "' is already defined!");
        } else if (character == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        } else {
            this.key.put(character, ingredient);
            return this;
        }
    }

    public PartAssemblerRecipeBuilder pattern(String patternRow) {
        if (!this.rows.isEmpty() && patternRow.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            this.rows.add(patternRow);
            return this;
        }
    }

    public PartAssemblerRecipeBuilder assemblyTime(int assemblyTime) {
        this.assemblyTime = Math.max(1, assemblyTime);
        return this;
    }

    public PartAssemblerRecipeBuilder scrapByproduct(ItemStack scrapByproduct) {
        this.scrapByproduct = (scrapByproduct != null && !scrapByproduct.isEmpty()) ? scrapByproduct.copy() : ItemStack.EMPTY;
        return this;
    }

    public PartAssemblerRecipeBuilder scrapByproduct(ItemLike itemLike) {
        return this.scrapByproduct(new ItemStack(itemLike));
    }

    public PartAssemblerRecipeBuilder scrapByproduct(ItemLike itemLike, int count) {
        return this.scrapByproduct(new ItemStack(itemLike, count));
    }

    public PartAssemblerRecipeBuilder partStats(@Nullable RocketPartStats partStats) {
        this.partStats = partStats;
        return this;
    }

    @Override
    public @NotNull Recipe<?> createRecipe(ResourceLocation id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
        if (this.schematic == null || this.schematic.isEmpty()) {
            throw new IllegalStateException("Part assembler recipe " + id + " has no schematic defined");
        }
        if (this.rows.isEmpty()) {
            throw new IllegalStateException("No pattern is defined for part assembler recipe " + id);
        }
        if (this.rows.size() > 3) {
            throw new IllegalArgumentException("Pattern cannot have more than 3 rows for part assembler recipe " + id);
        }
        int width = this.rows.get(0).length();
        if (width == 0 || width > 3) {
            throw new IllegalArgumentException("Pattern width must be between 1 and 3 columns for part assembler recipe " + id);
        }
        for (String row : this.rows) {
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c != ' ' && !this.key.containsKey(c)) {
                    throw new IllegalArgumentException("Pattern references symbol '" + c + "' but it's not defined in the key");
                }
            }
        }
        for (Character definedKey : this.key.keySet()) {
            boolean used = false;
            for (String row : this.rows) {
                if (row.indexOf(definedKey) >= 0) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                throw new IllegalArgumentException("Key defines symbol '" + definedKey + "' that is not used in any pattern rows");
            }
        }

        ShapedRecipePattern shapedRecipePattern = ShapedRecipePattern.of(this.key, this.rows);
        ItemStack resultStack = new ItemStack(this.result, this.count);
        return new PartAssemblerRecipe(
                Objects.requireNonNullElse(this.group, ""),
                this.schematic,
                shapedRecipePattern,
                resultStack,
                this.scrapByproduct.isEmpty() ? ItemStack.EMPTY : this.scrapByproduct.copy(),
                this.assemblyTime,
                Optional.ofNullable(this.partStats)
        );
    }
}
