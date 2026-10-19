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

package dev.galacticraft.mod.world.gen.feature;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.tag.GCBlockTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GCOreConfiguredFeature {
    public static final RuleTest BASE_STONE_MOON = new TagMatchTest(GCBlockTags.BASE_STONE_MOON);
    public static final RuleTest BASE_STONE_MARS = new TagMatchTest(GCBlockTags.BASE_STONE_MARS);
    public static final RuleTest STONE_ORE_REPLACEABLE = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
    public static final RuleTest DEEPSLATE_ORE_REPLACEABLE = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    public static final RuleTest MOON_ROCK_ORE_REPLACEABLE = new TagMatchTest(GCBlockTags.MOON_ROCK_ORE_REPLACEABLES);
    public static final RuleTest LUNASLATE_ORE_REPLACEABLE = new TagMatchTest(GCBlockTags.LUNASLATE_ORE_REPLACEABLES);
    public static final RuleTest MOON_BASALT_ORE_REPLACEABLE = new TagMatchTest(GCBlockTags.MOON_BASALT_ORE_REPLACEABLES);
    public static final RuleTest MARS_STONE_ORE_REPLACEABLE = new TagMatchTest(GCBlockTags.MARS_STONE_ORE_REPLACEABLES);

    public static final RuleTest SOFT_VENUS_ROCK_ORE_REPLACEABLE = new TagMatchTest(GCBlockTags.SOFT_VENUS_ROCK_ORE_REPLACEABLES);
    public static final RuleTest HARD_VENUS_ROCK_ORE_REPLACEABLE = new TagMatchTest(GCBlockTags.HARD_VENUS_ROCK_ORE_REPLACEABLES);

    public static final RuleTest MERCURY_BASALT_REPLACEABLE = new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(GCBlocks.MERCURY_BASALT);
    public static final RuleTest MERCURY_SHOCK_BRECCIA_REPLACEABLE = new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(GCBlocks.MERCURY_SHOCK_BRECCIA);

    public static final List<OreConfiguration.TargetBlockState> TIN_ORES = List.of(OreConfiguration.target(STONE_ORE_REPLACEABLE, GCBlocks.TIN_ORE.defaultBlockState()), OreConfiguration.target(DEEPSLATE_ORE_REPLACEABLE, GCBlocks.DEEPSLATE_TIN_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> ALUMINUM_ORES = List.of(OreConfiguration.target(STONE_ORE_REPLACEABLE, GCBlocks.ALUMINUM_ORE.defaultBlockState()), OreConfiguration.target(DEEPSLATE_ORE_REPLACEABLE, GCBlocks.DEEPSLATE_ALUMINUM_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> SILICON_ORES = List.of(OreConfiguration.target(STONE_ORE_REPLACEABLE, GCBlocks.SILICON_ORE.defaultBlockState()), OreConfiguration.target(DEEPSLATE_ORE_REPLACEABLE, GCBlocks.DEEPSLATE_SILICON_ORE.defaultBlockState()));

    public static final List<OreConfiguration.TargetBlockState> COPPER_ORES_MOON = List.of(OreConfiguration.target(MOON_ROCK_ORE_REPLACEABLE, GCBlocks.MOON_COPPER_ORE.defaultBlockState()), OreConfiguration.target(LUNASLATE_ORE_REPLACEABLE, GCBlocks.LUNASLATE_COPPER_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> TIN_ORES_MOON = List.of(OreConfiguration.target(MOON_ROCK_ORE_REPLACEABLE, GCBlocks.MOON_TIN_ORE.defaultBlockState()), OreConfiguration.target(LUNASLATE_ORE_REPLACEABLE, GCBlocks.LUNASLATE_TIN_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> CHEESE_ORES_MOON = List.of(OreConfiguration.target(MOON_ROCK_ORE_REPLACEABLE, GCBlocks.MOON_CHEESE_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> LUNAR_SAPPHIRE_ORES_MOON = List.of(OreConfiguration.target(MOON_ROCK_ORE_REPLACEABLE, GCBlocks.LUNAR_SAPPHIRE_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> DIRT_ORES_MOON = List.of(OreConfiguration.target(MOON_ROCK_ORE_REPLACEABLE, GCBlocks.MOON_DIRT.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> OLIVINE_BASALT_ORES_MOON = List.of(
            OreConfiguration.target(MOON_BASALT_ORE_REPLACEABLE, GCBlocks.OLIVINE_BASALT.defaultBlockState()),
            OreConfiguration.target(MOON_ROCK_ORE_REPLACEABLE, GCBlocks.OLIVINE_BASALT.defaultBlockState()),
            OreConfiguration.target(LUNASLATE_ORE_REPLACEABLE, GCBlocks.OLIVINE_BASALT.defaultBlockState())
    );
    public static final List<OreConfiguration.TargetBlockState> RICH_OLIVINE_BASALT_ORES_MOON = List.of(
            OreConfiguration.target(MOON_BASALT_ORE_REPLACEABLE, GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState()),
            OreConfiguration.target(MOON_ROCK_ORE_REPLACEABLE, GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState()),
            OreConfiguration.target(LUNASLATE_ORE_REPLACEABLE, GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState())
    );
    public static final List<OreConfiguration.TargetBlockState> DENSE_ICE_ORES_MOON = List.of(
            OreConfiguration.target(MOON_ROCK_ORE_REPLACEABLE, GCBlocks.DENSE_ICE.defaultBlockState())
    );

    public static final List<OreConfiguration.TargetBlockState> COPPER_ORES_MARS = List.of(OreConfiguration.target(MARS_STONE_ORE_REPLACEABLE, GCBlocks.MARS_COPPER_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> TIN_ORES_MARS = List.of(OreConfiguration.target(MARS_STONE_ORE_REPLACEABLE, GCBlocks.MARS_TIN_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> IRON_ORES_MARS = List.of(OreConfiguration.target(MARS_STONE_ORE_REPLACEABLE, GCBlocks.MARS_IRON_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> DESH_ORES_MARS = List.of(OreConfiguration.target(MARS_STONE_ORE_REPLACEABLE, GCBlocks.DESH_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> ICE_ORES_MARS = List.of(OreConfiguration.target(MARS_STONE_ORE_REPLACEABLE, GCBlocks.DENSE_ICE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> SUB_SURFACE_ORES_MARS = List.of(OreConfiguration.target(BASE_STONE_MARS, GCBlocks.MARS_SUB_SURFACE_ROCK.defaultBlockState()));

    public static final List<OreConfiguration.TargetBlockState> COPPER_ORES_VENUS = List.of(OreConfiguration.target(SOFT_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.VENUS_COPPER_ORE.defaultBlockState()), OreConfiguration.target(HARD_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.VENUS_COPPER_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> TIN_ORES_VENUS = List.of(OreConfiguration.target(SOFT_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.VENUS_TIN_ORE.defaultBlockState()), OreConfiguration.target(HARD_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.VENUS_TIN_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> ALUMINUM_ORES_VENUS = List.of(OreConfiguration.target(SOFT_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.VENUS_ALUMINUM_ORE.defaultBlockState()), OreConfiguration.target(HARD_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.VENUS_ALUMINUM_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> GALENA_ORES_VENUS = List.of(OreConfiguration.target(HARD_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.GALENA_ORE.defaultBlockState()), OreConfiguration.target(SOFT_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.GALENA_ORE.defaultBlockState()));
    public static final List<OreConfiguration.TargetBlockState> SOLAR_ORES_VENUS = List.of(OreConfiguration.target(SOFT_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.SOLAR_ORE.defaultBlockState()), OreConfiguration.target(HARD_VENUS_ROCK_ORE_REPLACEABLE, GCBlocks.SOLAR_ORE.defaultBlockState()));

    // OVERWORLD
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SILICON_SMALL = key("ore_silicon_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SILICON_LARGE = key("ore_silicon_large");

    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TIN = key("ore_tin");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TIN_SMALL = key("ore_tin_small");

    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ALUMINUM = key("ore_aluminum");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ALUMINUM_SMALL = key("ore_aluminum_small");

    // MOON
    public static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_DISK_MOON = key("basalt_disk_moon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIRT_MOON = key("ore_dirt_moon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_OLIVINE_BASALT_MOON = key("ore_olivine_basalt_moon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_RICH_OLIVINE_BASALT_MOON = key("ore_rich_olivine_basalt_moon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DENSE_ICE_MOON = key("ore_dense_ice_moon");

    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPER_SMALL_MOON = key("ore_copper_small_moon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPER_LARGE_MOON = key("ore_copper_large_moon");

    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TIN_MOON = key("ore_tin_moon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TIN_SMALL_MOON = key("ore_tin_small_moon");

    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_CHEESE_SMALL_MOON = key("ore_cheese_small_moon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_CHEESE_LARGE_MOON = key("ore_cheese_large_moon");

    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_LUNAR_SAPPHIRE_MOON = key("ore_lunar_sapphire_moon");

    // MARS
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPER_SMALL_MARS = key("ore_copper_small_mars");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPER_LARGE_MARS = key("ore_copper_large_mars");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TIN_MARS = key("ore_tin_mars");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TIN_SMALL_MARS = key("ore_tin_small_mars");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_IRON_MARS = key("ore_iron_mars");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_IRON_SMALL_MARS = key("ore_iron_small_mars");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DESH_MARS = key("ore_desh_mars");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DESH_SMALL_MARS = key("ore_desh_small_mars");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ICE_MARS = key("ore_ice_mars");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SUB_SURFACE_MARS = key("ore_sub_surface_mars");

    // VENUS
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPER_VENUS = key("ore_copper_venus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPER_LARGE_VENUS = key("ore_copper_large_venus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TIN_VENUS = key("ore_tin_venus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ALUMINUM_VENUS = key("ore_aluminum_venus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GALENA_VENUS = key("ore_galena_venus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SOLAR_VENUS = key("ore_solar_venus");

    // MERCURY
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ALUMINUM_MERCURY = key("ore_aluminum_mercury");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPER_MERCURY = key("ore_copper_mercury");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_TITANIUM_MERCURY = key("ore_titanium_mercury");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_NATIVE_IRON_MERCURY = key("ore_native_iron_mercury");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GRAPHITE_MERCURY = key("ore_graphite_mercury");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SULFIDE_MERCURY = key("ore_sulfide_mercury");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MAGMA_PATCH_MERCURY = key("magma_patch_mercury");

    @Contract(pure = true)
    private static @NotNull ResourceKey<ConfiguredFeature<?, ?>> key(String s) {
        return Constant.key(Registries.CONFIGURED_FEATURE, s);
    }

    public static void bootstrapRegistries(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // OVERWORLD
        context.register(ORE_SILICON_SMALL, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(SILICON_ORES, 6, 0.5F)));
        context.register(ORE_SILICON_LARGE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(SILICON_ORES, 9, 0.7F)));
        context.register(ORE_TIN, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(TIN_ORES, 7)));
        context.register(ORE_TIN_SMALL, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(TIN_ORES, 3)));
        context.register(ORE_ALUMINUM, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(ALUMINUM_ORES, 7)));
        context.register(ORE_ALUMINUM_SMALL, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(ALUMINUM_ORES, 3)));

        // MOON (GC4 BiomeDecoratorMoon + Addons)
        context.register(BASALT_DISK_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(BASE_STONE_MOON, GCBlocks.MOON_BASALT.defaultBlockState(), 33)));
        context.register(ORE_DIRT_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(DIRT_ORES_MOON, 32)));
        context.register(ORE_OLIVINE_BASALT_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(OLIVINE_BASALT_ORES_MOON, 24)));
        context.register(ORE_RICH_OLIVINE_BASALT_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(RICH_OLIVINE_BASALT_ORES_MOON, 10)));
        context.register(ORE_DENSE_ICE_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(DENSE_ICE_ORES_MOON, 20)));
        context.register(ORE_COPPER_SMALL_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(COPPER_ORES_MOON, 10)));
        context.register(ORE_COPPER_LARGE_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(COPPER_ORES_MOON, 20)));
        context.register(ORE_TIN_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(TIN_ORES_MOON, 6)));
        context.register(ORE_TIN_SMALL_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(TIN_ORES_MOON, 4)));
        context.register(ORE_CHEESE_SMALL_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(CHEESE_ORES_MOON, 4)));
        context.register(ORE_CHEESE_LARGE_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(CHEESE_ORES_MOON, 9)));
        context.register(ORE_LUNAR_SAPPHIRE_MOON, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(LUNAR_SAPPHIRE_ORES_MOON, 3)));

        // MARS
        context.register(ORE_COPPER_SMALL_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(COPPER_ORES_MARS, 6)));
        context.register(ORE_COPPER_LARGE_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(COPPER_ORES_MARS, 12)));
        context.register(ORE_TIN_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(TIN_ORES_MARS, 6)));
        context.register(ORE_TIN_SMALL_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(TIN_ORES_MARS, 4)));
        context.register(ORE_IRON_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(IRON_ORES_MARS, 8)));
        context.register(ORE_IRON_SMALL_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(IRON_ORES_MARS, 4)));
        context.register(ORE_DESH_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(DESH_ORES_MARS, 6)));
        context.register(ORE_DESH_SMALL_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(DESH_ORES_MARS, 3)));
        context.register(ORE_ICE_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(ICE_ORES_MARS, 18)));
        context.register(ORE_SUB_SURFACE_MARS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(SUB_SURFACE_ORES_MARS, 32)));

        // VENUS
        context.register(ORE_COPPER_VENUS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(COPPER_ORES_VENUS, 6)));
        context.register(ORE_COPPER_LARGE_VENUS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(COPPER_ORES_VENUS, 12)));
        context.register(ORE_TIN_VENUS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(TIN_ORES_VENUS, 6)));
        context.register(ORE_ALUMINUM_VENUS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(ALUMINUM_ORES_VENUS, 6)));
        context.register(ORE_GALENA_VENUS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(GALENA_ORES_VENUS, 6)));
        context.register(ORE_SOLAR_VENUS, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(SOLAR_ORES_VENUS, 5)));

        // MERCURY
        context.register(ORE_ALUMINUM_MERCURY, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(MERCURY_BASALT_REPLACEABLE, GCBlocks.MERCURY_ALUMINUM_ORE.defaultBlockState(), 8)));
        context.register(ORE_COPPER_MERCURY, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(MERCURY_BASALT_REPLACEABLE, GCBlocks.MERCURY_COPPER_ORE.defaultBlockState(), 9)));
        context.register(ORE_TITANIUM_MERCURY, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(MERCURY_BASALT_REPLACEABLE, GCBlocks.MERCURY_TITANIUM_ORE.defaultBlockState(), 6)));
        context.register(ORE_NATIVE_IRON_MERCURY, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(MERCURY_BASALT_REPLACEABLE, GCBlocks.MERCURY_NATIVE_IRON_ORE.defaultBlockState()),
                OreConfiguration.target(MERCURY_SHOCK_BRECCIA_REPLACEABLE, GCBlocks.MERCURY_NATIVE_IRON_ORE.defaultBlockState())
        ), 10)));
        context.register(ORE_GRAPHITE_MERCURY, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(MERCURY_BASALT_REPLACEABLE, GCBlocks.MERCURY_GRAPHITE_ORE.defaultBlockState(), 7)));
        context.register(ORE_SULFIDE_MERCURY, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(MERCURY_SHOCK_BRECCIA_REPLACEABLE, GCBlocks.MERCURY_SULFIDE_ORE.defaultBlockState(), 5)));
        context.register(MAGMA_PATCH_MERCURY, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(MERCURY_BASALT_REPLACEABLE, Blocks.MAGMA_BLOCK.defaultBlockState()),
                OreConfiguration.target(MERCURY_SHOCK_BRECCIA_REPLACEABLE, Blocks.MAGMA_BLOCK.defaultBlockState())
        ), 32)));
    }
}
