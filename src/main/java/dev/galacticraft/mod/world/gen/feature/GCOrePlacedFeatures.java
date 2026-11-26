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
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public class GCOrePlacedFeatures {
    // OVERWORLD
    public static final ResourceKey<PlacedFeature> ORE_SILICON = key("ore_silicon"); //todo actual ore numbers
    public static final ResourceKey<PlacedFeature> ORE_SILICON_LARGE = key("ore_silicon_large");

    public static final ResourceKey<PlacedFeature> ORE_TIN_UPPER = key("ore_tin_upper");
    public static final ResourceKey<PlacedFeature> ORE_TIN_MIDDLE = key("ore_tin_middle");
    public static final ResourceKey<PlacedFeature> ORE_TIN_SMALL = key("ore_tin_small");

    public static final ResourceKey<PlacedFeature> ORE_ALUMINUM_MIDDLE = key("ore_aluminum_middle");
    public static final ResourceKey<PlacedFeature> ORE_ALUMINUM_SMALL = key("ore_aluminum_small");

    public static final ResourceKey<PlacedFeature> ORE_NIOBIUM = key("ore_niobium");
    public static final ResourceKey<PlacedFeature> ORE_NIOBIUM_DEEP = key("ore_niobium_deep");

    // MOON
    public static final ResourceKey<PlacedFeature> BASALT_DISK_MOON = key("disk_basalt_moon");
    public static final ResourceKey<PlacedFeature> ORE_DIRT_MOON = key("ore_dirt_moon");
    public static final ResourceKey<PlacedFeature> ORE_OLIVINE_BASALT_MOON = key("ore_olivine_basalt_moon");
    public static final ResourceKey<PlacedFeature> ORE_RICH_OLIVINE_BASALT_MOON = key("ore_rich_olivine_basalt_moon");
    public static final ResourceKey<PlacedFeature> ORE_DENSE_ICE_MOON = key("ore_dense_ice_moon");

    public static final ResourceKey<PlacedFeature> ORE_COPPER_MOON = key("ore_copper_moon");
    public static final ResourceKey<PlacedFeature> ORE_COPPER_LARGE_MOON = key("ore_copper_large_moon");

    public static final ResourceKey<PlacedFeature> ORE_TIN_UPPER_MOON = key("ore_tin_upper_moon");
    public static final ResourceKey<PlacedFeature> ORE_TIN_MIDDLE_MOON = key("ore_tin_middle_moon");
    public static final ResourceKey<PlacedFeature> ORE_TIN_SMALL_MOON = key("ore_tin_small_moon");

    public static final ResourceKey<PlacedFeature> ORE_CHEESE_MOON = key("ore_cheese_moon");
    public static final ResourceKey<PlacedFeature> ORE_CHEESE_LARGE_MOON = key("ore_cheese_large_moon");

    public static final ResourceKey<PlacedFeature> ORE_LUNAR_SAPPHIRE_MOON = key("ore_lunar_sapphire_moon");
    public static final ResourceKey<PlacedFeature> ORE_ALUMINUM_MOON = key("ore_aluminum_moon");
    public static final ResourceKey<PlacedFeature> ORE_ALUMINUM_HIGHLANDS_MOON = key("ore_aluminum_highlands_moon");
    public static final ResourceKey<PlacedFeature> ORE_SILICON_MOON = key("ore_silicon_moon");
    public static final ResourceKey<PlacedFeature> ORE_ILMENITE_MOON = key("ore_ilmenite_moon");
    public static final ResourceKey<PlacedFeature> ORE_NIOBIUM_MOON = key("ore_niobium_moon");
    public static final ResourceKey<PlacedFeature> ORE_GOLD_MOON = key("ore_gold_moon");
    public static final ResourceKey<PlacedFeature> ORE_REDSTONE_MOON = key("ore_redstone_moon");
    public static final ResourceKey<PlacedFeature> ORE_IRON_MOON = key("ore_iron_moon");

    // MARS
    public static final ResourceKey<PlacedFeature> ORE_COPPER_MARS = key("ore_copper_mars");
    public static final ResourceKey<PlacedFeature> ORE_COPPER_LARGE_MARS = key("ore_copper_large_mars");
    public static final ResourceKey<PlacedFeature> ORE_TIN_UPPER_MARS = key("ore_tin_upper_mars");
    public static final ResourceKey<PlacedFeature> ORE_TIN_MIDDLE_MARS = key("ore_tin_middle_mars");
    public static final ResourceKey<PlacedFeature> ORE_TIN_SMALL_MARS = key("ore_tin_small_mars");
    public static final ResourceKey<PlacedFeature> ORE_IRON_MIDDLE_MARS = key("ore_iron_middle_mars");
    public static final ResourceKey<PlacedFeature> ORE_IRON_SMALL_MARS = key("ore_iron_small_mars");
    public static final ResourceKey<PlacedFeature> ORE_SILICON_MARS = key("ore_silicon_mars");
    public static final ResourceKey<PlacedFeature> ORE_ALUMINUM_MARS = key("ore_aluminum_mars");
    public static final ResourceKey<PlacedFeature> ORE_NIOBIUM_MARS = key("ore_niobium_mars");
    public static final ResourceKey<PlacedFeature> ORE_GOLD_MARS = key("ore_gold_mars");
    public static final ResourceKey<PlacedFeature> ORE_REDSTONE_MARS = key("ore_redstone_mars");
    public static final ResourceKey<PlacedFeature> ORE_TITANIUM_MARS = key("ore_titanium_mars");
    public static final ResourceKey<PlacedFeature> ORE_GRAPHITE_MARS = key("ore_graphite_mars");
    public static final ResourceKey<PlacedFeature> ORE_DESH_MIDDLE_MARS = key("ore_desh_middle_mars");
    public static final ResourceKey<PlacedFeature> ORE_DESH_SMALL_MARS = key("ore_desh_small_mars");
    public static final ResourceKey<PlacedFeature> ORE_ICE_MARS = key("ore_ice_mars");
    public static final ResourceKey<PlacedFeature> ORE_SUB_SURFACE_MARS = key("ore_sub_surface_mars");

    // VENUS
    public static final ResourceKey<PlacedFeature> ORE_COPPER_VENUS = key("ore_copper_venus");
    public static final ResourceKey<PlacedFeature> ORE_COPPER_LARGE_VENUS = key("ore_copper_large_venus");
    public static final ResourceKey<PlacedFeature> ORE_TIN_VENUS = key("ore_tin_venus");
    public static final ResourceKey<PlacedFeature> ORE_ALUMINUM_VENUS = key("ore_aluminum_venus");
    public static final ResourceKey<PlacedFeature> ORE_IRON_VENUS = key("ore_iron_venus");
    public static final ResourceKey<PlacedFeature> ORE_NIOBIUM_VENUS = key("ore_niobium_venus");
    public static final ResourceKey<PlacedFeature> ORE_GOLD_VENUS = key("ore_gold_venus");
    public static final ResourceKey<PlacedFeature> ORE_REDSTONE_VENUS = key("ore_redstone_venus");
    public static final ResourceKey<PlacedFeature> ORE_SILICON_VENUS = key("ore_silicon_venus");
    public static final ResourceKey<PlacedFeature> ORE_TITANIUM_VENUS = key("ore_titanium_venus");
    public static final ResourceKey<PlacedFeature> ORE_GALENA_VENUS = key("ore_galena_venus");
    public static final ResourceKey<PlacedFeature> ORE_SOLAR_VENUS = key("ore_solar_venus");

    // MERCURY
    public static final ResourceKey<PlacedFeature> ORE_ALUMINUM_MERCURY = key("ore_aluminum_mercury");
    public static final ResourceKey<PlacedFeature> ORE_COPPER_MERCURY = key("ore_copper_mercury");
    public static final ResourceKey<PlacedFeature> ORE_TITANIUM_MERCURY = key("ore_titanium_mercury");
    public static final ResourceKey<PlacedFeature> ORE_NATIVE_IRON_MERCURY = key("ore_native_iron_mercury");
    public static final ResourceKey<PlacedFeature> ORE_GRAPHITE_MERCURY = key("ore_graphite_mercury");
    public static final ResourceKey<PlacedFeature> ORE_SULFIDE_MERCURY = key("ore_sulfide_mercury");
    public static final ResourceKey<PlacedFeature> ORE_NIOBIUM_MERCURY = key("ore_niobium_mercury");
    public static final ResourceKey<PlacedFeature> ORE_GOLD_MERCURY = key("ore_gold_mercury");
    public static final ResourceKey<PlacedFeature> ORE_LEAD_MERCURY = key("ore_lead_mercury");
    public static final ResourceKey<PlacedFeature> MAGMA_PATCH_MERCURY = key("magma_patch_mercury");

    // GAS & ICE GIANT HYPERBARIC CORE EXOTIC ORES
    public static final ResourceKey<PlacedFeature> ORE_PALLADIUM_JUPITER = key("ore_palladium_jupiter");
    public static final ResourceKey<PlacedFeature> ORE_MAGNESIUM_SATURN = key("ore_magnesium_saturn");
    public static final ResourceKey<PlacedFeature> ORE_URANIUM_URANUS = key("ore_uranium_uranus");

    // PHASE 4 OUTER MOONS
    public static final ResourceKey<PlacedFeature> ORE_MAGNESIUM_TITAN = key("ore_magnesium_titan");
    public static final ResourceKey<PlacedFeature> ORE_IRON_TITAN = key("ore_iron_titan");
    public static final ResourceKey<PlacedFeature> ORE_SILICON_TITAN = key("ore_silicon_titan");
    public static final ResourceKey<PlacedFeature> ORE_DENSE_ICE_TITAN = key("ore_dense_ice_titan");

    public static final ResourceKey<PlacedFeature> ORE_MAGNESIUM_ENCELADUS = key("ore_magnesium_enceladus");
    public static final ResourceKey<PlacedFeature> ORE_NIOBIUM_ENCELADUS = key("ore_niobium_enceladus");
    public static final ResourceKey<PlacedFeature> ORE_DENSE_ICE_ENCELADUS = key("ore_dense_ice_enceladus");

    public static final ResourceKey<PlacedFeature> ORE_URANIUM_TITANIA = key("ore_uranium_titania");
    public static final ResourceKey<PlacedFeature> ORE_LONSDALEITE_TITANIA = key("ore_lonsdaleite_titania");
    public static final ResourceKey<PlacedFeature> ORE_TITANIUM_TITANIA = key("ore_titanium_titania");
    public static final ResourceKey<PlacedFeature> ORE_DENSE_ICE_TITANIA = key("ore_dense_ice_titania");

    public static final ResourceKey<PlacedFeature> ORE_URANIUM_OBERON = key("ore_uranium_oberon");
    public static final ResourceKey<PlacedFeature> ORE_NIOBIUM_OBERON = key("ore_niobium_oberon");
    public static final ResourceKey<PlacedFeature> ORE_DENSE_ICE_OBERON = key("ore_dense_ice_oberon");

    public static final ResourceKey<PlacedFeature> ORE_SUPERIONIC_TRITON = key("ore_superionic_triton");
    public static final ResourceKey<PlacedFeature> ORE_XENON_CLATHRATE_TRITON = key("ore_xenon_clathrate_triton");
    public static final ResourceKey<PlacedFeature> ORE_DENSE_ICE_TRITON = key("ore_dense_ice_triton");

    public static void register() {
        BiomeModifications.create(Constant.id("ores")).add(ModificationPhase.ADDITIONS, BiomeSelectors.foundInOverworld(), context -> {
            context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_SILICON);
            context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_SILICON_LARGE);
            context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_TIN_UPPER);
            context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_TIN_MIDDLE);
            context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_TIN_SMALL);
            context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_ALUMINUM_MIDDLE);
            context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_ALUMINUM_SMALL);
            context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_NIOBIUM);
            context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_NIOBIUM_DEEP);
        });
    }

    public static void bootstrapRegistries(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> lookup = context.lookup(Registries.CONFIGURED_FEATURE);

        // OVERWORLD
        context.register(ORE_SILICON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SILICON_SMALL), modifiersWithCount(5, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))));
        context.register(ORE_SILICON_LARGE, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SILICON_LARGE), modifiersWithRarity(11, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))));
        context.register(ORE_TIN_UPPER, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN), modifiersWithCount(99, HeightRangePlacement.triangle(VerticalAnchor.absolute(90), VerticalAnchor.absolute(384)))));
        context.register(ORE_TIN_MIDDLE, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN), modifiersWithCount(11, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(56)))));
        context.register(ORE_TIN_SMALL, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN_SMALL), modifiersWithCount(11, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(80)))));
        context.register(ORE_ALUMINUM_MIDDLE, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_ALUMINUM), modifiersWithCount(9, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-32), VerticalAnchor.absolute(0)))));
        context.register(ORE_ALUMINUM_SMALL, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_ALUMINUM_SMALL), modifiersWithCount(30, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(32)))));
        context.register(ORE_NIOBIUM, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_NIOBIUM), modifiersWithCount(7, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(48)))));
        context.register(ORE_NIOBIUM_DEEP, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_NIOBIUM_DEEP), modifiersWithCount(8, HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(0)))));

        // MOON (aligned with GC4 BiomeDecoratorMoon: copper, tin, sapphire strictly subterranean below Y=58)
        context.register(BASALT_DISK_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.BASALT_DISK_MOON), modifiersWithCount(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_DIRT_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_DIRT_MOON), modifiersWithCount(20, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(200)))));
        context.register(ORE_OLIVINE_BASALT_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_OLIVINE_BASALT_MOON), modifiersWithCount(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_RICH_OLIVINE_BASALT_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_RICH_OLIVINE_BASALT_MOON), modifiersWithCount(6, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(55)))));
        context.register(ORE_DENSE_ICE_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_DENSE_ICE_MOON), modifiersWithCount(8, HeightRangePlacement.uniform(VerticalAnchor.absolute(20), VerticalAnchor.absolute(58)))));
        context.register(ORE_COPPER_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_COPPER_SMALL_MOON), modifiersWithCount(18, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_COPPER_LARGE_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_COPPER_LARGE_MOON), modifiersWithCount(12, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_TIN_UPPER_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN_MOON), modifiersWithCount(12, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_TIN_MIDDLE_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN_MOON), modifiersWithCount(8, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_TIN_SMALL_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN_SMALL_MOON), modifiersWithCount(6, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_CHEESE_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_CHEESE_SMALL_MOON), modifiersWithCount(14, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(85)))));
        context.register(ORE_CHEESE_LARGE_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_CHEESE_LARGE_MOON), modifiersWithCount(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(85)))));
        context.register(ORE_LUNAR_SAPPHIRE_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_LUNAR_SAPPHIRE_MOON), modifiersWithCount(12, HeightRangePlacement.triangle(VerticalAnchor.absolute(4), VerticalAnchor.absolute(36)))));
        context.register(ORE_ALUMINUM_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_ALUMINUM_MOON), modifiersWithCount(16, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_ALUMINUM_HIGHLANDS_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_ALUMINUM_HIGHLANDS_MOON), modifiersWithCount(14, HeightRangePlacement.uniform(VerticalAnchor.absolute(20), VerticalAnchor.absolute(80)))));
        context.register(ORE_SILICON_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SILICON_MOON), modifiersWithCount(12, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_ILMENITE_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_ILMENITE_MOON), modifiersWithCount(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(58)))));
        context.register(ORE_NIOBIUM_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_NIOBIUM_MOON), modifiersWithCount(3, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(32)))));
        context.register(ORE_GOLD_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_GOLD_MOON), modifiersWithCount(3, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(32)))));
        context.register(ORE_REDSTONE_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_REDSTONE_MOON), modifiersWithCount(3, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(28)))));
        context.register(ORE_IRON_MOON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_IRON_MOON), modifiersWithCount(5, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(48)))));

        // MARS (Multi-ore Martian stratigraphy: massive iron 2.5x, redstone & copper 2.0x, tin 1.5x, silicon 1.2x, aluminum 0.7x, niobium 0.6x, deep gold/titanium 0.5x, trace graphite 0.2x + Desh & Ice)
        context.register(ORE_COPPER_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_COPPER_SMALL_MARS), modifiersWithCount(20, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(60)))));
        context.register(ORE_COPPER_LARGE_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_COPPER_LARGE_MARS), modifiersWithCount(10, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(60)))));
        context.register(ORE_TIN_UPPER_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN_MARS), modifiersWithCount(15, HeightRangePlacement.triangle(VerticalAnchor.absolute(30), VerticalAnchor.absolute(120)))));
        context.register(ORE_TIN_MIDDLE_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN_MARS), modifiersWithCount(12, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(60)))));
        context.register(ORE_TIN_SMALL_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN_SMALL_MARS), modifiersWithCount(11, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(60)))));
        context.register(ORE_IRON_MIDDLE_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_IRON_MARS), modifiersWithCount(26, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))));
        context.register(ORE_IRON_SMALL_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_IRON_SMALL_MARS), modifiersWithCount(16, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))));
        context.register(ORE_SILICON_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SILICON_MARS), modifiersWithCount(10, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(56)))));
        context.register(ORE_ALUMINUM_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_ALUMINUM_MARS), modifiersWithCount(7, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(52)))));
        context.register(ORE_NIOBIUM_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_NIOBIUM_MARS), modifiersWithCount(6, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(36)))));
        context.register(ORE_GOLD_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_GOLD_MARS), modifiersWithCount(5, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(20)))));
        context.register(ORE_REDSTONE_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_REDSTONE_MARS), modifiersWithCount(18, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(48)))));
        context.register(ORE_TITANIUM_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TITANIUM_MARS), modifiersWithCount(5, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(16)))));
        context.register(ORE_GRAPHITE_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_GRAPHITE_MARS), modifiersWithCount(2, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(40)))));
        context.register(ORE_DESH_MIDDLE_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_DESH_MARS), modifiersWithCount(14, HeightRangePlacement.triangle(VerticalAnchor.absolute(-56), VerticalAnchor.absolute(64)))));
        context.register(ORE_DESH_SMALL_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_DESH_SMALL_MARS), modifiersWithCount(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(52)))));
        context.register(ORE_ICE_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_ICE_MARS), modifiersWithCount(5, HeightRangePlacement.uniform(VerticalAnchor.absolute(40), VerticalAnchor.absolute(150)))));
        context.register(ORE_SUB_SURFACE_MARS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SUB_SURFACE_MARS), modifiersWithCount(20, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(240)))));

        // VENUS (Multi-ore volcanic stratigraphy: galena 3.0x, copper & tin 2.5x, aluminum/gold/redstone 1.8x, silicon 1.5x, niobium & solar 1.0x, iron & titanium 0.8x)
        context.register(ORE_COPPER_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_COPPER_VENUS), modifiersWithCount(22, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(60)))));
        context.register(ORE_COPPER_LARGE_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_COPPER_LARGE_VENUS), modifiersWithCount(10, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(60)))));
        context.register(ORE_TIN_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TIN_VENUS), modifiersWithCount(24, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(60)))));
        context.register(ORE_ALUMINUM_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_ALUMINUM_VENUS), modifiersWithCount(18, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(60)))));
        context.register(ORE_IRON_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_IRON_VENUS), modifiersWithCount(8, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(56)))));
        context.register(ORE_NIOBIUM_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_NIOBIUM_VENUS), modifiersWithCount(10, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(44)))));
        context.register(ORE_GOLD_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_GOLD_VENUS), modifiersWithCount(16, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(48)))));
        context.register(ORE_REDSTONE_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_REDSTONE_VENUS), modifiersWithCount(16, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(48)))));
        context.register(ORE_SILICON_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SILICON_VENUS), modifiersWithCount(14, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(56)))));
        context.register(ORE_TITANIUM_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TITANIUM_VENUS), modifiersWithCount(7, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(36)))));
        context.register(ORE_GALENA_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_GALENA_VENUS), modifiersWithCount(26, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(60)))));
        context.register(ORE_SOLAR_VENUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SOLAR_VENUS), modifiersWithCount(8, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(50)))));

        // MERCURY (Multi-ore metallic core stratigraphy: native iron & graphite 3.5x, titanium/niobium/gold/sulfide 2.0x, copper & aluminum 1.0x at Y=80..160, lead 0.8x)
        // Alumínio & Cobre: Y = 80 a 160
        context.register(ORE_ALUMINUM_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_ALUMINUM_MERCURY), modifiersWithCount(14, HeightRangePlacement.uniform(VerticalAnchor.absolute(80), VerticalAnchor.absolute(160)))));
        context.register(ORE_COPPER_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_COPPER_MERCURY), modifiersWithCount(12, HeightRangePlacement.uniform(VerticalAnchor.absolute(80), VerticalAnchor.absolute(160)))));
        // Titânio: Y = -20 a 70 (2.0x)
        context.register(ORE_TITANIUM_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TITANIUM_MERCURY), modifiersWithCount(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-20), VerticalAnchor.absolute(70)))));
        // Ferro Nativo metálico em veios densos nas profundezas: Y = -64 a 10 (3.5x)
        context.register(ORE_NATIVE_IRON_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_NATIVE_IRON_MERCURY), modifiersWithCount(28, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(10)))));
        // Grafite Primordial / Carbono: Y = 0 a 90 (3.5x)
        context.register(ORE_GRAPHITE_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_GRAPHITE_MERCURY), modifiersWithCount(24, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(90)))));
        // Sulfetos Exóticos de Oldhamita e Niningerita no subsolo profundo: Y = -64 a 30 (2.0x)
        context.register(ORE_SULFIDE_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SULFIDE_MERCURY), modifiersWithCount(14, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(30)))));
        // Nióbio, Ouro (2.0x) e Chumbo (0.8x)
        context.register(ORE_NIOBIUM_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_NIOBIUM_MERCURY), modifiersWithCount(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(50)))));
        context.register(ORE_GOLD_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_GOLD_MERCURY), modifiersWithCount(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(60)))));
        context.register(ORE_LEAD_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_LEAD_MERCURY), modifiersWithCount(8, HeightRangePlacement.triangle(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(70)))));
        // Bolsões massivos de Magma no abismo basal profundo: Y = -56 a -16
        context.register(MAGMA_PATCH_MERCURY, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.MAGMA_PATCH_MERCURY), modifiersWithCount(14, HeightRangePlacement.uniform(VerticalAnchor.absolute(-56), VerticalAnchor.absolute(-16)))));

        // GAS & ICE GIANT HYPERBARIC ABYSSAL CORE EXOTIC ORES (strictly Y = -60..-20)
        context.register(ORE_PALLADIUM_JUPITER, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_PALLADIUM_JUPITER), modifiersWithCount(16, HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(4), VerticalAnchor.absolute(-20)))));
        context.register(ORE_MAGNESIUM_SATURN, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_MAGNESIUM_SATURN), modifiersWithCount(16, HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(4), VerticalAnchor.absolute(-20)))));
        context.register(ORE_URANIUM_URANUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_URANIUM_URANUS), modifiersWithCount(16, HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(4), VerticalAnchor.absolute(-20)))));

        // PHASE 4 OUTER MOONS
        context.register(ORE_MAGNESIUM_TITAN, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_MAGNESIUM_TITAN), modifiersWithCount(16, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))));
        context.register(ORE_IRON_TITAN, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_IRON_TITAN), modifiersWithCount(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))));
        context.register(ORE_SILICON_TITAN, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SILICON_TITAN), modifiersWithCount(12, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))));
        context.register(ORE_DENSE_ICE_TITAN, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_DENSE_ICE_TITAN), modifiersWithCount(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(80)))));

        context.register(ORE_MAGNESIUM_ENCELADUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_MAGNESIUM_ENCELADUS), modifiersWithCount(15, HeightRangePlacement.uniform(VerticalAnchor.absolute(22), VerticalAnchor.absolute(84)))));
        context.register(ORE_NIOBIUM_ENCELADUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_NIOBIUM_ENCELADUS), modifiersWithCount(12, HeightRangePlacement.uniform(VerticalAnchor.absolute(22), VerticalAnchor.absolute(80)))));
        context.register(ORE_DENSE_ICE_ENCELADUS, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_DENSE_ICE_ENCELADUS), modifiersWithCount(16, HeightRangePlacement.uniform(VerticalAnchor.absolute(22), VerticalAnchor.absolute(92)))));

        context.register(ORE_URANIUM_TITANIA, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_URANIUM_TITANIA), modifiersWithCount(14, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(56)))));
        context.register(ORE_LONSDALEITE_TITANIA, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_LONSDALEITE_TITANIA), modifiersWithCount(10, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(36)))));
        context.register(ORE_TITANIUM_TITANIA, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_TITANIUM_TITANIA), modifiersWithCount(12, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(68)))));
        context.register(ORE_DENSE_ICE_TITANIA, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_DENSE_ICE_TITANIA), modifiersWithCount(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(84)))));

        context.register(ORE_URANIUM_OBERON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_URANIUM_OBERON), modifiersWithCount(15, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(56)))));
        context.register(ORE_NIOBIUM_OBERON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_NIOBIUM_OBERON), modifiersWithCount(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))));
        context.register(ORE_DENSE_ICE_OBERON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_DENSE_ICE_OBERON), modifiersWithCount(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(80)))));

        context.register(ORE_SUPERIONIC_TRITON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_SUPERIONIC_TRITON), modifiersWithCount(14, HeightRangePlacement.triangle(VerticalAnchor.bottom(), VerticalAnchor.absolute(48)))));
        context.register(ORE_XENON_CLATHRATE_TRITON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_XENON_CLATHRATE_TRITON), modifiersWithCount(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))));
        context.register(ORE_DENSE_ICE_TRITON, new PlacedFeature(lookup.getOrThrow(GCOreConfiguredFeature.ORE_DENSE_ICE_TRITON), modifiersWithCount(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(88)))));
    }

    @Contract(pure = true)
    private static @NotNull ResourceKey<PlacedFeature> key(String id) {
        return Constant.key(Registries.PLACED_FEATURE, id);
    }

    private static @Unmodifiable List<PlacementModifier> modifiers(PlacementModifier countModifier, PlacementModifier heightModifier) {
        return List.of(countModifier, InSquarePlacement.spread(), heightModifier, BiomeFilter.biome());
    }

    private static @Unmodifiable List<PlacementModifier> modifiersWithCount(int count, PlacementModifier heightModfier) {
        return modifiers(CountPlacement.of(count), heightModfier);
    }

    private static @Unmodifiable List<PlacementModifier> modifiersWithRarity(int chance, PlacementModifier heightModifier) {
        return modifiers(RarityFilter.onAverageOnceEvery(chance), heightModifier);
    }
}
