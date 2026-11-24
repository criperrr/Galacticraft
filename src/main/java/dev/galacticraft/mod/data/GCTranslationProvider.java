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

package dev.galacticraft.mod.data;

import dev.galacticraft.api.data.TranslationProvider;
import dev.galacticraft.api.rocket.part.RocketPart;
import dev.galacticraft.mod.api.block.entity.PipeColor;
import dev.galacticraft.mod.content.GCBlockRegistry.DecorationSet;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.GCRocketParts;
import dev.galacticraft.mod.content.GCStats;
import dev.galacticraft.mod.content.entity.damage.GCDamageTypes;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.tag.GCFluidTags;
import dev.galacticraft.mod.tag.GCItemTags;
import dev.galacticraft.mod.world.biome.GCBiomes;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.galacticraft.mod.Constant.Cape;
import static dev.galacticraft.mod.util.Translations.*;

public class GCTranslationProvider extends TranslationProvider {
    public GCTranslationProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void generateTranslations(HolderLookup.@NotNull Provider registries) {
        this.generateBlockTranslations();
        this.generateItemTranslations();
        this.generateItemTagTranslations();
        this.generateFluidTagTranslations();
        this.generateGasTranslations();
        this.generateEntityTranslations();
        this.generateBannerPatternTranslations();
        this.generateCelestialBodyTranslations();
        this.generateBiomeTranslations();
        this.generateChatTranslations();
        this.generateRocketPartTranslations();
        this.generateSmithingTranslations();
        this.generateSubtitleTranslations();
        this.generateTooltipTranslations();
        this.generateConfigTranslations();
        this.generateSpaceRaceTranslations();
        this.generateSolarPanelTranslations();
        this.generateMachineStatusTranslations();
        this.generateCelestialSelectionTranslations();
        this.generateUiTranslations();
        this.generateWailaTranslations();
        this.generateStatsTranslations();
        this.generateAdvancementsTranslations();
        this.generateManualTranslations();

        // Tabs in the creative inventory
        this.add(ItemGroup.BLOCKS, "Galacticraft Blocks");
        this.add(ItemGroup.ITEMS, "Galacticraft Items");
        this.add(ItemGroup.MACHINES, "Galacticraft Machines");
        this.add(ItemGroup.CANNED_FOOD, "Canned Food");

        this.add(RecipeCategory.CIRCUIT_FABRICATOR, "Circuit Fabrication");
        this.add(RecipeCategory.COMPRESSOR, "Compressing");
        this.add(RecipeCategory.ELECTRIC_COMPRESSOR, "Compressing (Electric)");
        this.add(RecipeCategory.ELECTRIC_FURNACE, "Smelting (Electric)");
        this.add(RecipeCategory.ELECTRIC_ARC_FURNACE, "Blasting (Electric)");
        this.add(RecipeCategory.CANNING, "Canning");
        this.add(RecipeCategory.ROCKET_WORKBENCH, "Rocket Crafting");
        this.add(RecipeCategory.METALLURGIC_INFUSING, "Vacuum Metallurgic Infusing");
        this.add(RecipeCategory.CHEMICAL_LEACHING, "Chemical Acid Leaching");
        this.add(RecipeCategory.COMPOSITE_AUTOCLAVE, "Composite Autoclave Curing");
        this.add(RecipeCategory.ELECTROLYTIC, "Electrolytic Separation");
        this.add(RecipeCategory.SABATIER, "Sabatier Chemical Synthesis");
        this.add(RecipeCategory.CENTRIFUGE, "Isotopic Centrifugation");
        this.add(RecipeCategory.ELECTRIC_ARC_FURNACE_BONUS_CHANCE, "%s%% Chance");
    }

    protected void generateBlockTranslations() {
        // TORCHES
        this.block(GCBlocks.GLOWSTONE_TORCH, "Glowstone Torch");
        this.block(GCBlocks.GLOWSTONE_WALL_TORCH, "Glowstone Torch");
        this.block(GCBlocks.UNLIT_TORCH, "Unlit Torch");
        this.block(GCBlocks.UNLIT_WALL_TORCH, "Unlit Torch");
        this.block(GCBlocks.UNLIT_SOUL_TORCH, "Unlit Soul Torch");
        this.block(GCBlocks.UNLIT_SOUL_WALL_TORCH, "Unlit Soul Torch");

        // LANTERNS
        this.block(GCBlocks.GLOWSTONE_LANTERN, "Glowstone Lantern");
        this.block(GCBlocks.UNLIT_LANTERN, "Unlit Lantern");
        this.block(GCBlocks.UNLIT_SOUL_LANTERN, "Unlit Soul Lantern");

        // FLUIDS
        this.block(GCBlocks.CRUDE_OIL, "Crude Oil");
        this.block(GCBlocks.FUEL, "Fuel");
        this.block(GCBlocks.SULFURIC_ACID, "Sulfuric Acid");
        this.block(GCBlocks.LIQUID_OXYGEN, "Liquid Oxygen");
        this.block(GCBlocks.LIQUID_HYDROGEN, "Liquid Hydrogen");
        this.block(GCBlocks.LIQUID_METHANE, "Liquid Methane");
        this.block(GCBlocks.MONOPROPELLANT, "Monopropellant (Hydrazine)");
        this.block(GCBlocks.LIQUID_XENON, "Liquid Xenon");
        this.block(GCBlocks.PROPELLANT_GAS, "Propellant Vapor");

        // DECORATION BLOCKS
        this.decorationSet(GCBlocks.ALUMINUM_DECORATION, "Aluminum Decoration");
        this.decorationSet(GCBlocks.BRONZE_DECORATION, "Bronze Decoration");
        this.decorationSet(GCBlocks.COPPER_DECORATION, "Copper Decoration");
        this.decorationSet(GCBlocks.IRON_DECORATION, "Iron Decoration");
        this.decorationSet(GCBlocks.METEORIC_IRON_DECORATION, "Meteoric Iron Decoration");
        this.decorationSet(GCBlocks.STEEL_DECORATION, "Steel Decoration");
        this.decorationSet(GCBlocks.TIN_DECORATION, "Tin Decoration");
        this.decorationSet(GCBlocks.TITANIUM_DECORATION, "Titanium Decoration");
        this.decorationSet(GCBlocks.DARK_DECORATION, "Dark Decoration");

        // MOON NATURAL
        this.block(GCBlocks.MOON_TURF, "Moon Turf");
        this.block(GCBlocks.MOON_DIRT, "Moon Dirt");
        this.block(GCBlocks.MOON_DIRT_PATH, "Moon Dirt Path");
        this.block(GCBlocks.MOON_DUNGEON_BRICK, "Moon Dungeon Brick");

        this.block(GCBlocks.MOON_SURFACE_ROCK, "Moon Surface Rock");
        this.block(GCBlocks.MOON_SURFACE_ROCK_SLAB, "Moon Surface Rock Slab");
        this.block(GCBlocks.MOON_SURFACE_ROCK_STAIRS, "Moon Surface Rock Stairs");
        this.block(GCBlocks.MOON_SURFACE_ROCK_WALL, "Moon Surface Rock Wall");

        this.block(GCBlocks.MOON_ROCK, "Moon Rock");
        this.block(GCBlocks.MOON_ROCK_SLAB, "Moon Rock Slab");
        this.block(GCBlocks.MOON_ROCK_STAIRS, "Moon Rock Stairs");
        this.block(GCBlocks.MOON_ROCK_WALL, "Moon Rock Wall");

        this.block(GCBlocks.MOON_ROCK_BRICK, "Moon Rock Brick");
        this.block(GCBlocks.MOON_ROCK_BRICK_SLAB, "Moon Rock Brick Slab");
        this.block(GCBlocks.MOON_ROCK_BRICK_STAIRS, "Moon Rock Brick Stairs");
        this.block(GCBlocks.MOON_ROCK_BRICK_WALL, "Moon Rock Brick Wall");

        this.block(GCBlocks.CRACKED_MOON_ROCK_BRICK, "Cracked Moon Rock Brick");
        this.block(GCBlocks.CRACKED_MOON_ROCK_BRICK_SLAB, "Cracked Moon Rock Brick Slab");
        this.block(GCBlocks.CRACKED_MOON_ROCK_BRICK_STAIRS, "Cracked Moon Rock Brick Stairs");
        this.block(GCBlocks.CRACKED_MOON_ROCK_BRICK_WALL, "Cracked Moon Rock Brick Wall");

        this.block(GCBlocks.POLISHED_MOON_ROCK, "Polished Moon Rock");
        this.block(GCBlocks.POLISHED_MOON_ROCK_SLAB, "Polished Moon Rock Slab");
        this.block(GCBlocks.POLISHED_MOON_ROCK_STAIRS, "Polished Moon Rock Stairs");
        this.block(GCBlocks.POLISHED_MOON_ROCK_WALL, "Polished Moon Rock Wall");

        this.block(GCBlocks.CHISELED_MOON_ROCK_BRICK, "Chiseled Moon Rock Brick");
        this.block(GCBlocks.MOON_ROCK_PILLAR, "Moon Rock Pillar");

        this.block(GCBlocks.COBBLED_MOON_ROCK, "Cobbled Moon Rock");
        this.block(GCBlocks.COBBLED_MOON_ROCK_SLAB, "Cobbled Moon Rock Slab");
        this.block(GCBlocks.COBBLED_MOON_ROCK_STAIRS, "Cobbled Moon Rock Stairs");
        this.block(GCBlocks.COBBLED_MOON_ROCK_WALL, "Cobbled Moon Rock Wall");

        this.block(GCBlocks.LUNASLATE, "Lunaslate");
        this.block(GCBlocks.LUNASLATE_SLAB, "Lunaslate Slab");
        this.block(GCBlocks.LUNASLATE_STAIRS, "Lunaslate Stairs");
        this.block(GCBlocks.LUNASLATE_WALL, "Lunaslate Wall");

        this.block(GCBlocks.COBBLED_LUNASLATE, "Cobbled Lunaslate");
        this.block(GCBlocks.COBBLED_LUNASLATE_SLAB, "Cobbled Lunaslate Slab");
        this.block(GCBlocks.COBBLED_LUNASLATE_STAIRS, "Cobbled Lunaslate Stairs");
        this.block(GCBlocks.COBBLED_LUNASLATE_WALL, "Cobbled Lunaslate Wall");

        this.block(GCBlocks.MOON_BASALT, "Moon Basalt");
        this.block(GCBlocks.MOON_BASALT_SLAB, "Moon Basalt Slab");
        this.block(GCBlocks.MOON_BASALT_STAIRS, "Moon Basalt Stairs");
        this.block(GCBlocks.MOON_BASALT_WALL, "Moon Basalt Wall");

        this.block(GCBlocks.MOON_BASALT_BRICK, "Moon Basalt Brick");
        this.block(GCBlocks.MOON_BASALT_BRICK_SLAB, "Moon Basalt Brick Slab");
        this.block(GCBlocks.MOON_BASALT_BRICK_STAIRS, "Moon Basalt Brick Stairs");
        this.block(GCBlocks.MOON_BASALT_BRICK_WALL, "Moon Basalt Brick Wall");

        this.block(GCBlocks.CRACKED_MOON_BASALT_BRICK, "Cracked Moon Basalt Brick");
        this.block(GCBlocks.CRACKED_MOON_BASALT_BRICK_SLAB, "Cracked Moon Basalt Brick Slab");
        this.block(GCBlocks.CRACKED_MOON_BASALT_BRICK_STAIRS, "Cracked Moon Basalt Brick Stairs");
        this.block(GCBlocks.CRACKED_MOON_BASALT_BRICK_WALL, "Cracked Moon Basalt Brick Wall");

        // MOON PLANTS

        this.block(GCBlocks.MOON_WEED, "Moon Weed");
        this.block(GCBlocks.MOON_SHRUBS, "Moon Shrubs");
        this.block(GCBlocks.MOON_MOSS, "Moon Moss");
        this.block(GCBlocks.MOON_TANGLE, "Moon Tangle");

        this.block(GCBlocks.FALLEN_METEOR, "Fallen Meteor");

        // MARS NATURAL
        this.block(GCBlocks.MARS_SURFACE_ROCK, "Mars Surface Rock");
        this.block(GCBlocks.MARS_SUB_SURFACE_ROCK, "Mars Sub-Surface Rock");

        this.block(GCBlocks.MARS_STONE, "Mars Stone");
        this.block(GCBlocks.MARS_STONE_SLAB, "Mars Stone Slab");
        this.block(GCBlocks.MARS_STONE_STAIRS, "Mars Stone Stairs");
        this.block(GCBlocks.MARS_STONE_WALL, "Mars Stone Wall");

        this.block(GCBlocks.MARS_COBBLESTONE, "Mars Cobblestone");
        this.block(GCBlocks.MARS_COBBLESTONE_SLAB, "Mars Cobblestone Slab");
        this.block(GCBlocks.MARS_COBBLESTONE_STAIRS, "Mars Cobblestone Stairs");
        this.block(GCBlocks.MARS_COBBLESTONE_WALL, "Mars Cobblestone Wall");

        // ASTEROID NATURAL
        this.block(GCBlocks.ASTEROID_ROCK, "Asteroid Rock");
        this.block(GCBlocks.ASTEROID_ROCK_1, "Asteroid Rock");
        this.block(GCBlocks.ASTEROID_ROCK_2, "Asteroid Rock");

        // VENUS NATURAL
        this.block(GCBlocks.SOFT_VENUS_ROCK, "Soft Venus Rock");
        this.block(GCBlocks.HARD_VENUS_ROCK, "Hard Venus Rock");
        this.block(GCBlocks.SCORCHED_VENUS_ROCK, "Scorched Venus Rock");
        this.block(GCBlocks.VOLCANIC_ROCK, "Volcanic Rock");
        this.block(GCBlocks.PUMICE, "Pumice");
        this.block(GCBlocks.VAPOR_SPOUT, "Vapor Spout");
        this.block(GCBlocks.CRASHED_PROBE, "Crashed Probe");

        // JUPITER NATURAL & ORES
        this.block(GCBlocks.JUPITER_SURFACE, "Jupiter Surface Rock");
        this.block(GCBlocks.JUPITER_SUB_SURFACE, "Jupiter Sub-Surface Rock");
        this.block(GCBlocks.JUPITER_STONE, "Jupiter Stone");
        this.block(GCBlocks.JUPITER_RED_STORM_ROCK, "Jupiter Red Storm Rock");
        this.block(GCBlocks.JUPITER_BANDED_OCHRE_ROCK, "Jupiter Banded Ochre Rock");
        this.block(GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK, "Jupiter Metallic Hydrogen Rock");
        this.block(GCBlocks.JUPITER_CORE_BRECCIA, "Jupiter Core Breccia");
        this.block(GCBlocks.JUPITER_STONE_BRICKS, "Jupiter Stone Bricks");
        this.block(GCBlocks.JUPITER_COPPER_ORE, "Jupiter Copper Ore");
        this.block(GCBlocks.JUPITER_TIN_ORE, "Jupiter Tin Ore");
        this.block(GCBlocks.JUPITER_IRON_ORE, "Jupiter Iron Ore");
        this.block(GCBlocks.JUPITER_ALUMINUM_ORE, "Jupiter Aluminum Ore");
        this.block(GCBlocks.JUPITER_NIOBIUM_ORE, "Jupiter Niobium Ore");
        this.block(GCBlocks.JUPITER_TITANIUM_ORE, "Jupiter Titanium Ore");
        this.block(GCBlocks.JUPITER_PALLADIUM_ORE, "Jupiter Palladium Ore");

        // SATURN NATURAL & ORES
        this.block(GCBlocks.SATURN_SURFACE, "Saturn Surface Rock");
        this.block(GCBlocks.SATURN_SUB_SURFACE, "Saturn Sub-Surface Rock");
        this.block(GCBlocks.SATURN_STONE, "Saturn Stone");
        this.block(GCBlocks.SATURN_GOLDEN_BAND_ROCK, "Saturn Golden Band Rock");
        this.block(GCBlocks.SATURN_RING_ICE_ROCK, "Saturn Ring Ice Rock");
        this.block(GCBlocks.SATURN_HYDROCARBON_SLATE, "Saturn Hydrocarbon Slate");
        this.block(GCBlocks.SATURN_HELIUM_MANTLE_ROCK, "Saturn Helium Mantle Rock");
        this.block(GCBlocks.SATURN_STONE_BRICKS, "Saturn Stone Bricks");
        this.block(GCBlocks.SATURN_COPPER_ORE, "Saturn Copper Ore");
        this.block(GCBlocks.SATURN_TIN_ORE, "Saturn Tin Ore");
        this.block(GCBlocks.SATURN_IRON_ORE, "Saturn Iron Ore");
        this.block(GCBlocks.SATURN_TITANIUM_ORE, "Saturn Titanium Ore");
        this.block(GCBlocks.SATURN_ALUMINUM_ORE, "Saturn Aluminum Ore");
        this.block(GCBlocks.SATURN_NIOBIUM_ORE, "Saturn Niobium Ore");
        this.block(GCBlocks.SATURN_MAGNESIUM_ORE, "Saturn Magnesium Ore");

        // URANUS NATURAL & ORES
        this.block(GCBlocks.URANUS_SURFACE, "Uranus Surface Rock");
        this.block(GCBlocks.URANUS_SUB_SURFACE, "Uranus Sub-Surface Rock");
        this.block(GCBlocks.URANUS_STONE, "Uranus Stone");
        this.block(GCBlocks.URANUS_METHANE_ICE, "Uranus Methane Ice");
        this.block(GCBlocks.URANUS_VERONA_SCARP_ROCK, "Uranus Verona Scarp Rock");
        this.block(GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK, "Uranus Superionic Mantle Rock");
        this.block(GCBlocks.URANUS_DIAMOND_BEARING_ROCK, "Uranus Diamond-Bearing Rock");
        this.block(GCBlocks.URANUS_STONE_BRICKS, "Uranus Stone Bricks");
        this.block(GCBlocks.URANUS_COPPER_ORE, "Uranus Copper Ore");
        this.block(GCBlocks.URANUS_TIN_ORE, "Uranus Tin Ore");
        this.block(GCBlocks.URANUS_ALUMINUM_ORE, "Uranus Aluminum Ore");
        this.block(GCBlocks.URANUS_TITANIUM_ORE, "Uranus Titanium Ore");
        this.block(GCBlocks.URANUS_IRON_ORE, "Uranus Iron Ore");
        this.block(GCBlocks.URANUS_NIOBIUM_ORE, "Uranus Niobium Ore");
        this.block(GCBlocks.URANUS_URANIUM_ORE, "Uranus Uranium Ore");

        // NEPTUNE NATURAL & ORES
        this.block(GCBlocks.NEPTUNE_SURFACE, "Neptune Surface Rock");
        this.block(GCBlocks.NEPTUNE_SUB_SURFACE, "Neptune Sub-Surface Rock");
        this.block(GCBlocks.NEPTUNE_STONE, "Neptune Stone");
        this.block(GCBlocks.NEPTUNE_NITROGEN_ICE, "Neptune Nitrogen Ice");
        this.block(GCBlocks.NEPTUNE_DARK_VORTEX_ROCK, "Neptune Dark Vortex Rock");
        this.block(GCBlocks.NEPTUNE_CRYO_SLATE, "Neptune Cryo-Slate");
        this.block(GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK, "Neptune Abyssal Mantle Rock");
        this.block(GCBlocks.NEPTUNE_STONE_BRICKS, "Neptune Stone Bricks");
        this.block(GCBlocks.NEPTUNE_COPPER_ORE, "Neptune Copper Ore");
        this.block(GCBlocks.NEPTUNE_TIN_ORE, "Neptune Tin Ore");
        this.block(GCBlocks.NEPTUNE_IRON_ORE, "Neptune Iron Ore");
        this.block(GCBlocks.NEPTUNE_TITANIUM_ORE, "Neptune Titanium Ore");
        this.block(GCBlocks.NEPTUNE_ALUMINUM_ORE, "Neptune Aluminum Ore");
        this.block(GCBlocks.NEPTUNE_NIOBIUM_ORE, "Neptune Niobium Ore");

        // MISC DECOR
        this.block(GCBlocks.WALKWAY, "Walkway");
        this.block(GCBlocks.FLUID_PIPE_WALKWAY, "Fluid Pipe Walkway");
        this.block(GCBlocks.WIRE_WALKWAY, "Wire Walkway");
        this.block(GCBlocks.HEAVY_WIRE_WALKWAY, "Heavy Wire Walkway");
        this.block(GCBlocks.TIN_LADDER, "Tin Ladder");
        this.block(GCBlocks.IRON_GRATING, "Iron Grating");
        this.block(GCBlocks.METEORIC_IRON_DOOR, "Meteoric Iron Door");

        // SPECIAL
        this.block(GCBlocks.ALUMINUM_WIRE, "Aluminum Wire");
        this.block(GCBlocks.HEAVY_ALUMINUM_WIRE, "Heavy Aluminum Wire");
        this.block(GCBlocks.SEALABLE_ALUMINUM_WIRE, "Sealable Aluminum Wire");
        this.block(GCBlocks.HEAVY_SEALABLE_ALUMINUM_WIRE, "Heavy Sealable Aluminum Wire");
        for (Map.Entry<PipeColor, Block> entry : GCBlocks.SEALABLE_FLUID_PIPES.entrySet()) {
            PipeColor color = entry.getKey();
            this.block(entry.getValue(), color == PipeColor.CLEAR ? "Sealable Fluid Pipe" : TranslationProvider.normalizeName(color.getName()) + " Sealable Fluid Pipe");
        }
        this.block(GCBlocks.GLASS_FLUID_PIPE, "Glass Fluid Pipe");
        this.block(GCBlocks.FUELING_PAD, "Fueling Pad");
        this.block(GCBlocks.ROCKET_LAUNCH_PAD, "Rocket Launch Pad");
        this.block(GCBlocks.REINFORCED_LAUNCH_PAD, "Reinforced Launch Pad");
        this.block(GCBlocks.GANTRY_TRUSS, "Gantry Truss");
        this.block(GCBlocks.GANTRY_CRANE_RAIL, "Gantry Crane Rail");
        this.block(GCBlocks.PLATFORM_UPGRADE_SOCKET, "Platform Upgrade Socket");
        this.block(GCBlocks.HANGAR_TERMINAL, "Hangar Terminal");
        this.block(GCBlocks.PLATFORM_PROXY, "Platform Structure");
        this.block(GCBlocks.ROCKET_PART_COMMAND, "Modular Command Module");
        this.block(GCBlocks.ROCKET_PART_ENGINE, "Modular Rocket Engine");
        this.block(GCBlocks.ROCKET_PART_TANK, "Modular Propellant Tank");
        this.block(GCBlocks.ROCKET_PART_ADAPTER, "Modular Structural Adapter");
        this.block(GCBlocks.ROCKET_PART_PAYLOAD, "Modular Payload Bay");
        this.block(GCBlocks.ROCKET_PART_UTILITY, "Modular Utility Component");
        this.block(GCBlocks.ROCKET_PART_BOUNDS, "Rocket Part Structure");
        this.block(GCBlocks.SHORT_RANGE_TELEPAD, "Short Range Telepad");
        this.block(GCBlocks.ROCKET_WORKBENCH, "Rocket Workbench");
        this.block(GCBlocks.PARACHEST, "Parachest");

        for (Map.Entry<PipeColor, Block> entry : GCBlocks.GLASS_FLUID_PIPES.entrySet()) {
            PipeColor color = entry.getKey();
            if (color != PipeColor.CLEAR) {
                this.block(entry.getValue(), TranslationProvider.normalizeName(color.getName()) + " Stained Glass Fluid Pipe");
            }
        }

        // VACUUM GLASS
        this.block(GCBlocks.VACUUM_GLASS, "Vacuum Glass");
        this.block(GCBlocks.CLEAR_VACUUM_GLASS, "Vacuum Glass (Clear)");
        this.block(GCBlocks.STRONG_VACUUM_GLASS, "Vacuum Glass (Strong)");

        // MOON GLASSES
        this.block(GCBlocks.OLIVINE_GLASS, "Olivine Glass");
        this.block(GCBlocks.OLIVINE_GLASS_PANE, "Olivine Glass Pane");
        this.block(GCBlocks.MOON_GLASS, "Moon Glass");
        this.block(GCBlocks.MOON_GLASS_PANE, "Moon Glass Pane");

        // ORES
        this.block(GCBlocks.MOON_IRON_ORE, "Moon Iron Ore");
        this.block(GCBlocks.MARS_IRON_ORE, "Mars Iron Ore");
        this.block(GCBlocks.ASTEROID_IRON_ORE, "Asteroid Iron Ore");
        this.block(GCBlocks.VENUS_IRON_ORE, "Venus Iron Ore");

        this.block(GCBlocks.MOON_COPPER_ORE, "Moon Copper Ore");
        this.block(GCBlocks.LUNASLATE_COPPER_ORE, "Lunaslate Copper Ore");
        this.block(GCBlocks.MARS_COPPER_ORE, "Mars Copper Ore");
        this.block(GCBlocks.ASTEROID_COPPER_ORE, "Asteroid Copper Ore");
        this.block(GCBlocks.VENUS_COPPER_ORE, "Venus Copper Ore");

        this.block(GCBlocks.SILICON_ORE, "Silicon Ore");
        this.block(GCBlocks.DEEPSLATE_SILICON_ORE, "Deepslate Silicon Ore");
        this.block(GCBlocks.MOON_SILICON_ORE, "Moon Silicon Ore");
        this.block(GCBlocks.MARS_SILICON_ORE, "Mars Silicon Ore");
        this.block(GCBlocks.VENUS_SILICON_ORE, "Venus Silicon Ore");

        this.block(GCBlocks.TIN_ORE, "Tin Ore");
        this.block(GCBlocks.DEEPSLATE_TIN_ORE, "Deepslate Tin Ore");
        this.block(GCBlocks.MOON_TIN_ORE, "Moon Tin Ore");
        this.block(GCBlocks.LUNASLATE_TIN_ORE, "Lunaslate Tin Ore");
        this.block(GCBlocks.MARS_TIN_ORE, "Mars Tin Ore");
        this.block(GCBlocks.VENUS_TIN_ORE, "Venus Tin Ore");

        this.block(GCBlocks.ALUMINUM_ORE, "Aluminum Ore");
        this.block(GCBlocks.DEEPSLATE_ALUMINUM_ORE, "Deepslate Aluminum Ore");
        this.block(GCBlocks.MOON_ALUMINUM_ORE, "Moon Aluminum Ore");
        this.block(GCBlocks.MARS_ALUMINUM_ORE, "Mars Aluminum Ore");
        this.block(GCBlocks.ASTEROID_ALUMINUM_ORE, "Asteroid Aluminum Ore");
        this.block(GCBlocks.VENUS_ALUMINUM_ORE, "Venus Aluminum Ore");

        this.block(GCBlocks.NIOBIUM_ORE, "Niobium Ore");
        this.block(GCBlocks.DEEPSLATE_NIOBIUM_ORE, "Deepslate Niobium Ore");
        this.block(GCBlocks.MOON_NIOBIUM_ORE, "Moon Niobium Ore");
        this.block(GCBlocks.MARS_NIOBIUM_ORE, "Mars Niobium Ore");
        this.block(GCBlocks.ASTEROID_NIOBIUM_ORE, "Asteroid Niobium Ore");
        this.block(GCBlocks.MERCURY_NIOBIUM_ORE, "Mercury Niobium Ore");
        this.block(GCBlocks.VENUS_NIOBIUM_ORE, "Venus Niobium Ore");

        this.block(GCBlocks.MOON_GOLD_ORE, "Moon Gold Ore");
        this.block(GCBlocks.MARS_GOLD_ORE, "Mars Gold Ore");
        this.block(GCBlocks.ASTEROID_GOLD_ORE, "Asteroid Gold Ore");
        this.block(GCBlocks.MERCURY_GOLD_ORE, "Mercury Gold Ore");
        this.block(GCBlocks.VENUS_GOLD_ORE, "Venus Gold Ore");

        this.block(GCBlocks.MOON_REDSTONE_ORE, "Moon Redstone Ore");
        this.block(GCBlocks.MARS_REDSTONE_ORE, "Mars Redstone Ore");
        this.block(GCBlocks.ASTEROID_REDSTONE_ORE, "Asteroid Redstone Ore");
        this.block(GCBlocks.VENUS_REDSTONE_ORE, "Venus Redstone Ore");

        this.block(GCBlocks.MARS_GRAPHITE_ORE, "Mars Graphite Ore");
        this.block(GCBlocks.ASTEROID_GRAPHITE_ORE, "Asteroid Graphite Ore");

        this.block(GCBlocks.MERCURY_LEAD_ORE, "Mercury Lead Ore");

        this.block(GCBlocks.MOON_CHEESE_ORE, "Moon Cheese Ore");

        this.block(GCBlocks.LUNAR_SAPPHIRE_ORE, "Lunar Sapphire Ore");

        this.block(GCBlocks.DESH_ORE, "Desh Ore");

        this.block(GCBlocks.ILMENITE_ORE, "Ilmenite Ore");
        this.block(GCBlocks.MOON_ILMENITE_ORE, "Moon Ilmenite Ore");
        this.block(GCBlocks.MARS_TITANIUM_ORE, "Mars Titanium Ore");
        this.block(GCBlocks.VENUS_TITANIUM_ORE, "Venus Titanium Ore");

        this.block(GCBlocks.GALENA_ORE, "Galena Ore");

        this.block(GCBlocks.SOLAR_ORE, "Solar Ore");

        this.block(GCBlocks.OLIVINE_CLUSTER, "Olivine Cluster");
        this.block(GCBlocks.OLIVINE_BASALT, "Olivine Basalt");
        this.block(GCBlocks.RICH_OLIVINE_BASALT, "Rich Olivine Basalt");

        // COMPACT MINERAL BLOCKS
        this.block(GCBlocks.SILICON_BLOCK, "Block of Silicon");
        this.block(GCBlocks.METEORIC_IRON_BLOCK, "Block of Meteoric Iron");
        this.block(GCBlocks.DESH_BLOCK, "Block of Desh");
        this.block(GCBlocks.ALUMINUM_BLOCK, "Block of Aluminum");
        this.block(GCBlocks.TIN_BLOCK, "Block of Tin");
        this.block(GCBlocks.TITANIUM_BLOCK, "Block of Titanium");
        this.block(GCBlocks.LEAD_BLOCK, "Block of Lead");
        this.block(GCBlocks.NIOBIUM_BLOCK, "Block of Niobium");
        this.block(GCBlocks.FERRONIOBIUM_BLOCK, "Block of Ferroniobium");
        this.block(GCBlocks.PALLADIUM_BLOCK, "Block of Palladium");
        this.block(GCBlocks.MAGNESIUM_BLOCK, "Block of Magnesium");
        this.block(GCBlocks.URANIUM_BLOCK, "Block of Uranium");
        this.block(GCBlocks.LONSDALEITE_BLOCK, "Block of Lonsdaleite");
        this.block(GCBlocks.LUNAR_SAPPHIRE_BLOCK, "Block of Lunar Sapphire");
        this.block(GCBlocks.OLIVINE_BLOCK, "Block of Olivine");
        this.block(GCBlocks.BUDDING_OLIVINE, "Budding Olivine");
        this.block(GCBlocks.RAW_METEORIC_IRON_BLOCK, "Block of Raw Meteoric Iron");
        this.block(GCBlocks.RAW_DESH_BLOCK, "Block of Raw Desh");
        this.block(GCBlocks.RAW_ALUMINUM_BLOCK, "Block of Raw Aluminum");
        this.block(GCBlocks.RAW_TIN_BLOCK, "Block of Raw Tin");
        this.block(GCBlocks.RAW_TITANIUM_BLOCK, "Block of Raw Titanium");
        this.block(GCBlocks.RAW_LEAD_BLOCK, "Block of Raw Lead");
        this.block(GCBlocks.RAW_NIOBIUM_BLOCK, "Block of Raw Niobium");
        this.block(GCBlocks.RAW_PALLADIUM_BLOCK, "Block of Raw Palladium");
        this.block(GCBlocks.RAW_MAGNESIUM_BLOCK, "Block of Raw Magnesium");
        this.block(GCBlocks.RAW_URANIUM_BLOCK, "Block of Raw Uranium");

        // CHEESE BLOCKS
        this.block(GCBlocks.MOON_CHEESE_BLOCK, "Moon Cheese Block");
        this.block(GCBlocks.MOON_CHEESE_LOG, "Moon Cheese Log");
        this.block(GCBlocks.MOON_CHEESE_LEAVES, "Moon Cheese Leaves");

        this.block(GCBlocks.MOON_CHEESE_WHEEL, "Moon Cheese Wheel");
        this.block(GCBlocks.CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Candle");
        this.block(GCBlocks.WHITE_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with White Candle");
        this.block(GCBlocks.ORANGE_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Orange Candle");
        this.block(GCBlocks.MAGENTA_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Magenta Candle");
        this.block(GCBlocks.LIGHT_BLUE_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Light Blue Candle");
        this.block(GCBlocks.YELLOW_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Yellow Candle");
        this.block(GCBlocks.LIME_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Lime Candle");
        this.block(GCBlocks.PINK_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Pink Candle");
        this.block(GCBlocks.GRAY_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Gray Candle");
        this.block(GCBlocks.LIGHT_GRAY_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Light Gray Candle");
        this.block(GCBlocks.CYAN_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Cyan Candle");
        this.block(GCBlocks.PURPLE_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Purple Candle");
        this.block(GCBlocks.BLUE_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Blue Candle");
        this.block(GCBlocks.BROWN_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Brown Candle");
        this.block(GCBlocks.GREEN_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Green Candle");
        this.block(GCBlocks.RED_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Red Candle");
        this.block(GCBlocks.BLACK_CANDLE_MOON_CHEESE_WHEEL, "Moon Cheese Wheel with Black Candle");

        // MOON VILLAGER SPECIAL
        this.block(GCBlocks.LUNAR_CARTOGRAPHY_TABLE, "Lunar Cartography Table");

        // MISC WORLD GEN
        this.block(GCBlocks.CAVERNOUS_VINES, "Cavernous Vines");
        this.block(GCBlocks.CAVERNOUS_VINES_PLANT, "Cavernous Vines Plant");
        this.block(GCBlocks.BOSS_SPAWNER, "Boss Spawner");

        // MULTIBLOCK PARTS
        this.block(GCBlocks.SOLAR_PANEL_PART, "Solar Panel");
        this.block(GCBlocks.CRYOGENIC_CHAMBER_PART, "Cryogenic Chamber");

        // MISC MACHINES
        this.block(GCBlocks.CRYOGENIC_CHAMBER, "Cryogenic Chamber");
        this.block(GCBlocks.PLAYER_TRANSPORT_TUBE, "Player Transport Tube");

        // MACHINES
        this.block(GCBlocks.CIRCUIT_FABRICATOR, "Circuit Fabricator");
        this.block(GCBlocks.COMPRESSOR, "Compressor");
        this.block(GCBlocks.ELECTRIC_COMPRESSOR, "Electric Compressor");
        this.block(GCBlocks.COAL_GENERATOR, "Coal Generator");
        this.block(GCBlocks.BASIC_SOLAR_PANEL, "Basic Solar Panel");
        this.block(GCBlocks.ADVANCED_SOLAR_PANEL, "Advanced Solar Panel");
        this.block(GCBlocks.GEOTHERMAL_GENERATOR, "Geothermal Generator");
        this.block(GCBlocks.ENERGY_STORAGE_MODULE, "Energy Storage Module");
        this.block(GCBlocks.ELECTRIC_FURNACE, "Electric Furnace");
        this.block(GCBlocks.ELECTRIC_ARC_FURNACE, "Electric Arc Furnace");
        this.block(GCBlocks.REFINERY, "Refinery");
        this.block(GCBlocks.REGOLITH_THERMAL_DESORBER, "Regolith Thermal Desorber");
        this.block(GCBlocks.ISOTOPIC_CENTRIFUGE, "Isotopic Centrifuge");
        this.block(GCBlocks.HELIUM3_FUSION_REACTOR, "Helium-3 Fusion Reactor");
        this.block(GCBlocks.VACUUM_METALLURGIC_INFUSER, "Vacuum Metallurgic Infuser");
        this.block(GCBlocks.CHEMICAL_LEACHING_PLANT, "Chemical Leaching Plant");
        this.block(GCBlocks.COMPOSITE_AUTOCLAVE, "Composite Autoclave");
        this.block(GCBlocks.ELECTROLYTIC_SEPARATOR, "Electrolytic Separator");
        this.block(GCBlocks.ATMOSPHERIC_FRACTIONATOR, "Atmospheric Fractionator");
        this.block(GCBlocks.SABATIER_CHEMICAL_REACTOR, "Sabatier Chemical Reactor");
        this.block(GCBlocks.AERO_REFURBISHMENT_BENCH, "Aerospace Refurbishment Bench");
        this.block(GCBlocks.RESEARCH_WORKBENCH, "Research & Prototyping Workbench");
        this.block(GCBlocks.OBSERVATORY_TELESCOPE, "Planetary Observatory Telescope");
        this.block(GCBlocks.GEOLOGICAL_ANALYZER, "Geological & Chemical Analyzer");
        this.block(GCBlocks.ENVIRONMENTAL_TEST_CHAMBER, "Environmental Test Chamber");
        this.block(GCBlocks.ZERO_G_RESEARCH_CHAMBER, "Zero-G Research Chamber");
        this.block(GCBlocks.AEROSPACE_RD_TERMINAL, "Aerospace R&D Terminal");
        this.block(GCBlocks.RESEARCH_DATA_LINK, "Interplanetary Research Data Link");
        this.block(GCBlocks.TELEMETRY_ANALYZER, "Flight Telemetry Analyzer");
        this.block(GCBlocks.FUSION_REACTOR_CASING, "MHD Fusion Casing");
        this.block(GCBlocks.FUSION_MAGNET_COIL, "Superconducting Toroid Coil");
        this.block(GCBlocks.FUSION_PLASMA_GLASS, "MHD Plasma Containment Glass");
        this.block(GCBlocks.OXYGEN_COLLECTOR, "Oxygen Collector");
        this.block(GCBlocks.OXYGEN_DETECTOR, "Oxygen Detector");
        this.block(GCBlocks.OXYGEN_SEALER, "Oxygen Sealer");
        this.block(GCBlocks.OXYGEN_BUBBLE_DISTRIBUTOR, "Bubble Distributor");
        this.block(GCBlocks.OXYGEN_DECOMPRESSOR, "Oxygen Decompressor");
        this.block(GCBlocks.OXYGEN_COMPRESSOR, "Oxygen Compressor");
        this.block(GCBlocks.OXYGEN_STORAGE_MODULE, "Oxygen Storage Module");
        this.block(GCBlocks.FUEL_LOADER, "Fuel Loader");

        this.block(GCBlocks.AIR_LOCK_CONTROLLER, "Airlock Controller");
        this.block(GCBlocks.AIR_LOCK_FRAME, "Airlock Frame");
        this.block(GCBlocks.AIR_LOCK_SEAL, "Airlock Seal");
    }

    protected void generateItemTranslations() {
        // MATERIALS
        this.item(GCItems.SILICON, "Silicon");

        this.item(GCItems.RAW_METEORIC_IRON, "Raw Meteoric Iron");
        this.item(GCItems.METEORIC_IRON_INGOT, "Meteoric Iron Ingot");
        this.item(GCItems.METEORIC_IRON_NUGGET, "Meteoric Iron Nugget");
        this.item(GCItems.COMPRESSED_METEORIC_IRON, "Compressed Meteoric Iron");

        this.item(GCItems.OLIVINE_SHARD, "Olivine Shard");

        this.item(GCItems.RAW_DESH, "Raw Desh");
        this.item(GCItems.DESH_INGOT, "Desh Ingot");
        this.item(GCItems.DESH_NUGGET, "Desh Nugget");
        this.item(GCItems.COMPRESSED_DESH, "Compressed Desh");

        this.item(GCItems.RAW_LEAD, "Raw Lead");
        this.item(GCItems.LEAD_INGOT, "Lead Ingot");
        this.item(GCItems.LEAD_NUGGET, "Lead Nugget");

        this.item(GCItems.RAW_NIOBIUM, "Raw Niobium");
        this.item(GCItems.NIOBIUM_OXIDE, "Niobium Oxide");
        this.item(GCItems.FERRONIOBIUM_INGOT, "Ferroniobium Ingot");
        this.item(GCItems.COMPRESSED_FERRONIOBIUM, "Compressed Ferroniobium");
        this.item(GCItems.NIOBIUM_INGOT, "Niobium Ingot");
        this.item(GCItems.NIOBIUM_NUGGET, "Niobium Nugget");
        this.item(GCItems.COMPRESSED_NIOBIUM, "Compressed Niobium");
        this.item(GCItems.SUPERCONDUCTING_COIL, "Superconducting Coil");

        // BENCH-SPECIFIC LABORATORY UPGRADES
        this.item(GCItems.OPTICAL_ACHROMAT_LENS, "Optical Achromat Lens");
        this.item(GCItems.SAPPHIRE_CASSEGRAIN_MIRROR, "Sapphire Cassegrain Mirror");
        this.item(GCItems.LONSDALEITE_PRIMARY_MIRROR, "Lonsdaleite Primary Mirror");
        this.item(GCItems.EQUATORIAL_SIDEREAL_DRIVE, "Equatorial Sidereal Drive");
        this.item(GCItems.ADAPTIVE_OPTICS_ACTUATOR, "Adaptive Optics Actuator");
        this.item(GCItems.INFRARED_BOLOMETER_ARRAY, "Infrared Bolometer Array");

        this.item(GCItems.PRECISION_VERNIER_CALIPER, "Precision Vernier Caliper");
        this.item(GCItems.OPTICAL_BLUEPRINT_PROJECTOR, "Optical Blueprint Projector");
        this.item(GCItems.CAD_HOLOGRAPHIC_STYLUS, "CAD Holographic Stylus");

        this.item(GCItems.TUNABLE_DIODE_LASER, "Tunable Diode Laser");
        this.item(GCItems.UV_EXCIMER_LASER, "UV Excimer Laser");
        this.item(GCItems.DIFFRACTION_GRATING_PRISM, "Diffraction Grating Prism");
        this.item(GCItems.QUADRUPOLE_MASS_FILTER, "Quadrupole Mass Filter");

        this.item(GCItems.TURBOMOLECULAR_VACUUM_PUMP, "Turbomolecular Vacuum Pump");
        this.item(GCItems.HYPERBARIC_PISTON_MANIFOLD, "Hyperbaric Piston Manifold");
        this.item(GCItems.REGENERATIVE_THERMAL_EXCHANGER, "Regenerative Thermal Exchanger");
        this.item(GCItems.ABLATIVE_ACID_LINER, "Ablative Acid Liner");

        this.item(GCItems.ELECTROMAGNETIC_STABILIZER_RING, "Electromagnetic Stabilizer Ring");
        this.item(GCItems.SUPERCONDUCTING_CONFINEMENT_COIL, "Superconducting Confinement Coil");
        this.item(GCItems.GYROSCOPIC_DAMPENING_GIMBAL, "Gyroscopic Dampening Gimbal");
        this.item(GCItems.HELIUM_DILUTION_CRYOSTAT, "Helium Dilution Cryostat");

        this.item(GCItems.LOW_NOISE_RF_AMPLIFIER, "Low-Noise RF Amplifier");
        this.item(GCItems.DEEP_SPACE_KA_BAND_FEED, "Deep-Space Ka-Band Feed");
        this.item(GCItems.PHASE_LOCKED_LOOP_MODULE, "Phase-Locked Loop Module");
        this.item(GCItems.LASER_COMM_TRANSPONDER, "Laser Comm Transponder");

        this.item(GCItems.TELEMETRY_COPROCESSOR_CARD, "Telemetry Coprocessor Card");
        this.item(GCItems.QUANTUM_ARCHIVE_CORE, "Quantum Archive Core");
        this.item(GCItems.BLANK_DATA_DRIVE, "Blank Data Drive");
        this.item(GCItems.RECORDED_DATA_DRIVE, "Recorded Data Drive");
        this.item(GCItems.BLANK_SCHEMATIC_DISC, "Blank Schematic Disc");
        this.item(GCItems.ORBITAL_SURVEY_MAP, "Orbital Survey Map");

        this.item(GCItems.RAW_PALLADIUM, "Raw Palladium");
        this.item(GCItems.PALLADIUM_INGOT, "Palladium Ingot");
        this.item(GCItems.COMPRESSED_PALLADIUM, "Compressed Palladium");
        this.item(GCItems.METALLIC_HYDROGEN_CELL, "Metallic Hydrogen Cell");

        this.item(GCItems.RAW_MAGNESIUM, "Raw Magnesium");
        this.item(GCItems.MAGNESIUM_INGOT, "Magnesium Ingot");
        this.item(GCItems.COMPRESSED_MAGNESIUM, "Compressed Magnesium");
        this.item(GCItems.HELIUM3_CELL, "Helium-3 Cell");
        this.item(GCItems.HELIUM3_FUSION_CELL, "Helium-3 Fusion Cell");

        this.item(GCItems.LONSDALEITE_GEM, "Lonsdaleite Gem");
        this.item(GCItems.RAW_URANIUM, "Raw Uranium");
        this.item(GCItems.URANIUM_INGOT, "Uranium Ingot");

        this.item(GCItems.SUPERIONIC_ICE_CRYSTAL, "Superionic Ice Crystal");

        this.item(GCItems.RAW_ALUMINUM, "Raw Aluminum");
        this.item(GCItems.ALUMINUM_INGOT, "Aluminum Ingot");
        this.item(GCItems.ALUMINUM_NUGGET, "Aluminum Nugget");
        this.item(GCItems.COMPRESSED_ALUMINUM, "Compressed Aluminum");

        this.item(GCItems.RAW_TIN, "Raw Tin");
        this.item(GCItems.TIN_INGOT, "Tin Ingot");
        this.item(GCItems.TIN_NUGGET, "Tin Nugget");
        this.item(GCItems.COMPRESSED_TIN, "Compressed Tin");

        this.item(GCItems.RAW_TITANIUM, "Raw Titanium");
        this.item(GCItems.TITANIUM_INGOT, "Titanium Ingot");
        this.item(GCItems.TITANIUM_NUGGET, "Titanium Nugget");
        this.item(GCItems.COMPRESSED_TITANIUM, "Compressed Titanium");

        this.item(GCItems.COMPRESSED_BRONZE, "Compressed Bronze");
        this.item(GCItems.COMPRESSED_COPPER, "Compressed Copper");
        this.item(GCItems.COMPRESSED_IRON, "Compressed Iron");
        this.item(GCItems.COMPRESSED_STEEL, "Compressed Steel");

        this.item(GCItems.LUNAR_SAPPHIRE, "Lunar Sapphire");
        this.item(GCItems.DESH_STICK, "Desh Stick");
        this.item(GCItems.CARBON_FRAGMENTS, "Carbon Fragments");
        this.item(GCItems.CARBON_FIBER_PLATE, "Carbon Fiber Plate");
        this.item(GCItems.ABLATIVE_HEAT_TILE, "Ablative Heat Tile");
        this.item(GCItems.SOFI_INSULATION, "SOFI Cryogenic Insulation");
        this.item(GCItems.SOLAR_DUST, "Solar Dust");
        this.item(GCItems.BASIC_WAFER, "Basic Wafer");
        this.item(GCItems.ADVANCED_WAFER, "Advanced Wafer");
        this.item(GCItems.BEAM_CORE, "Beam Core");
        this.item(GCItems.CANVAS, "Canvas");

        this.item(GCItems.FLUID_MANIPULATOR, "Fluid Manipulator");
        this.item(GCItems.OXYGEN_CONCENTRATOR, "Oxygen Concentrator");
        this.item(GCItems.OXYGEN_FAN, "Oxygen Fan");
        this.item(GCItems.OXYGEN_VENT, "Oxygen Vent");
        this.item(GCItems.SENSOR_LENS, "Sensor Lens");
        this.item(GCItems.BLUE_SOLAR_WAFER, "Blue Solar Wafer");
        this.item(GCItems.SINGLE_SOLAR_MODULE, "Single Solar Module");
        this.item(GCItems.FULL_SOLAR_PANEL, "Full Solar Panel");
        this.item(GCItems.SOLAR_ARRAY_WAFER, "Solar Array Wafer");
        this.item(GCItems.SOLAR_ARRAY_PANEL, "Solar Array Panel");
        this.item(GCItems.STEEL_POLE, "Steel Pole");
        this.item(GCItems.COPPER_CANISTER, "Copper Canister");
        this.item(GCItems.TIN_CANISTER, "Tin Canister");
        this.item(GCItems.THERMAL_CLOTH, "Thermal Cloth");
        this.item(GCItems.ISOTHERMAL_FABRIC, "Isothermal Fabric");
        this.item(GCItems.ORION_DRIVE, "Orion Drive");
        this.item(GCItems.ATMOSPHERIC_VALVE, "Atmospheric Valve");
        this.item(GCItems.AMBIENT_THERMAL_CONTROLLER, "Ambient Thermal Controller");
        this.item(GCItems.RADIOISOTOPE_CORE, "Radioisotope Core");

        // FOOD
        this.add(Items.CANNED_FOOD_TEMPLATE, "Canned %s");
        this.item(GCItems.CANNED_FOOD, "Canned Food");
        this.item(GCItems.EMPTY_CAN, "Empty Can");

        this.item(GCItems.MOON_CHEESE_CURD, "Moon Cheese Curd");
        this.item(GCItems.MOON_CHEESE_SLICE, "Moon Cheese Slice");

        this.item(GCItems.CRACKER, "Cracker");
        this.item(GCItems.CHEESE_CRACKER, "Cracker with Moon Cheese");

        this.item(GCItems.BURGER_BUN, "Burger Bun");
        this.item(GCItems.GROUND_BEEF, "Raw Beef Patty");
        this.item(GCItems.BEEF_PATTY, "Cooked Beef Patty");
        this.item(GCItems.CHEESEBURGER, "Cheeseburger");
        this.item(GCItems.MOON_TANGLE_FRUIT, "Moon Tangle Fruit");

        // ROCKET PLATES
        this.item(GCItems.TIER_1_HEAVY_DUTY_PLATE, "Heavy Plating");
        this.item(GCItems.TIER_2_HEAVY_DUTY_PLATE, "Thick Heavy Plating");
        this.item(GCItems.TIER_3_HEAVY_DUTY_PLATE, "Reinforced Heavy Plating");

        // THROWABLE METEOR CHUNKS
        this.item(GCItems.THROWABLE_METEOR_CHUNK, "Throwable Meteor Chunk");
        this.item(GCItems.HOT_THROWABLE_METEOR_CHUNK, "Hot Throwable Meteor Chunk");

        // ARMOR
        this.item(GCItems.HEAVY_DUTY_HELMET, "Heavy Duty Helmet");
        this.item(GCItems.HEAVY_DUTY_CHESTPLATE, "Heavy Duty Chestplate");
        this.item(GCItems.HEAVY_DUTY_LEGGINGS, "Heavy Duty Leggings");
        this.item(GCItems.HEAVY_DUTY_BOOTS, "Heavy Duty Boots");

        this.item(GCItems.DESH_HELMET, "Desh Helmet");
        this.item(GCItems.DESH_CHESTPLATE, "Desh Chestplate");
        this.item(GCItems.DESH_LEGGINGS, "Desh Leggings");
        this.item(GCItems.DESH_BOOTS, "Desh Boots");

        this.item(GCItems.TITANIUM_HELMET, "Titanium Helmet");
        this.item(GCItems.TITANIUM_CHESTPLATE, "Titanium Chestplate");
        this.item(GCItems.TITANIUM_LEGGINGS, "Titanium Leggings");
        this.item(GCItems.TITANIUM_BOOTS, "Titanium Boots");

        this.item(GCItems.SENSOR_GLASSES, "Sensor Glasses");

        // TOOLS + WEAPONS
        this.item(GCItems.HEAVY_DUTY_SWORD, "Heavy Duty Sword");
        this.item(GCItems.HEAVY_DUTY_SHOVEL, "Heavy Duty Shovel");
        this.item(GCItems.HEAVY_DUTY_PICKAXE, "Heavy Duty Pickaxe");
        this.item(GCItems.HEAVY_DUTY_AXE, "Heavy Duty Axe");
        this.item(GCItems.HEAVY_DUTY_HOE, "Heavy Duty Hoe");

        this.item(GCItems.DESH_SWORD, "Desh Sword");
        this.item(GCItems.DESH_SHOVEL, "Desh Shovel");
        this.item(GCItems.DESH_PICKAXE, "Desh Pickaxe");
        this.item(GCItems.DESH_AXE, "Desh Axe");
        this.item(GCItems.DESH_HOE, "Desh Hoe");

        this.item(GCItems.TITANIUM_SWORD, "Titanium Sword");
        this.item(GCItems.TITANIUM_SHOVEL, "Titanium Shovel");
        this.item(GCItems.TITANIUM_PICKAXE, "Titanium Pickaxe");
        this.item(GCItems.TITANIUM_AXE, "Titanium Axe");
        this.item(GCItems.TITANIUM_HOE, "Titanium Hoe");

        this.item(GCItems.STANDARD_WRENCH, "Standard Wrench");
        this.item(GCItems.GRAPPLE_HOOK, "Grapple Hook");

        this.item(GCItems.FLUID_CANISTER, "Empty Canister");
        this.add(Items.FLUID_CANISTER_FILLED, "%s Canister");

        // SMITHING TEMPLATES
        this.item(GCItems.TITANTIUM_UPGRADE_SMITHING_TEMPLATE, "Smithing Template");

        // BATTERIES
        this.item(GCItems.BATTERY, "Battery");
        this.item(GCItems.INFINITE_BATTERY, "Infinite Battery");
        this.item(GCItems.ATOMIC_BATTERY, "Atomic Battery");

        // FLUID BUCKETS
        this.item(GCItems.CRUDE_OIL_BUCKET, "Oil Bucket");
        this.item(GCItems.FUEL_BUCKET, "Fuel Bucket");
        this.item(GCItems.SULFURIC_ACID_BUCKET, "Sulfuric Acid Bucket");
        this.item(GCItems.LIQUID_OXYGEN_BUCKET, "Liquid Oxygen Bucket");
        this.item(GCItems.LIQUID_HYDROGEN_BUCKET, "Liquid Hydrogen Bucket");
        this.item(GCItems.LIQUID_METHANE_BUCKET, "Liquid Methane Bucket");
        this.item(GCItems.MONOPROPELLANT_BUCKET, "Monopropellant Bucket");
        this.item(GCItems.LIQUID_XENON_BUCKET, "Liquid Xenon Bucket");

        // GALACTICRAFT INVENTORY
        this.item(GCItems.PARACHUTE, "Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.WHITE), "White Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.ORANGE), "Orange Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.MAGENTA), "Magenta Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.LIGHT_BLUE), "Light Blue Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.YELLOW), "Yellow Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.LIME), "Lime Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.PINK), "Pink Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.GRAY), "Gray Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.LIGHT_GRAY), "Light Gray Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.CYAN), "Cyan Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.PURPLE), "Purple Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.BLUE), "Blue Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.BROWN), "Brown Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.GREEN), "Green Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.RED), "Red Parachute");
        this.item(GCItems.DYED_PARACHUTES.get(DyeColor.BLACK), "Black Parachute");

        this.item(GCItems.OXYGEN_MASK, "Oxygen Mask");
        this.item(GCItems.OXYGEN_GEAR, "Oxygen Gear");

        this.item(GCItems.SMALL_OXYGEN_TANK, "Small Oxygen Tank");
        this.item(GCItems.MEDIUM_OXYGEN_TANK, "Medium Oxygen Tank");
        this.item(GCItems.LARGE_OXYGEN_TANK, "Large Oxygen Tank");
        this.item(GCItems.INFINITE_OXYGEN_TANK, "Infinite Oxygen Tank");

        this.item(GCItems.SHIELD_CONTROLLER, "Shield Controller");
        this.item(GCItems.FREQUENCY_MODULE, "Frequency Module");

        this.item(GCItems.THERMAL_PADDING_HELMET, "Thermal Padding Helm");
        this.item(GCItems.THERMAL_PADDING_CHESTPIECE, "Thermal Padding Chestpiece");
        this.item(GCItems.THERMAL_PADDING_LEGGINGS, "Thermal Padding Leggings");
        this.item(GCItems.THERMAL_PADDING_BOOTS, "Thermal Padding Boots");

        this.item(GCItems.ISOTHERMAL_PADDING_HELMET, "Isothermal Padding Helm");
        this.item(GCItems.ISOTHERMAL_PADDING_CHESTPIECE, "Isothermal Padding Chestpiece");
        this.item(GCItems.ISOTHERMAL_PADDING_LEGGINGS, "Isothermal Padding Leggings");
        this.item(GCItems.ISOTHERMAL_PADDING_BOOTS, "Isothermal Padding Boots");

        // VEHICLES
        this.item(GCItems.BUGGY, "Buggy");
        this.item(GCItems.ROCKET, "Rocket");

        // ROCKET PARTS
        this.item(GCItems.NOSE_CONE, "Nose Cone");
        this.item(GCItems.HEAVY_NOSE_CONE, "Heavy Nose Cone");
        this.item(GCItems.ROCKET_FIN, "Rocket Fin");
        this.item(GCItems.HEAVY_ROCKET_FIN, "Heavy Rocket Fin");
        this.item(GCItems.ROCKET_ENGINE, "Rocket Engine");
        this.item(GCItems.HEAVY_ROCKET_ENGINE, "Heavy Rocket Engine");
        this.item(GCItems.ROCKET_BOOSTER, "Rocket Booster");

        // BUGGY PARTS
        this.item(GCItems.BUGGY_WHEEL, "Buggy Wheel");
        this.item(GCItems.BUGGY_SEAT, "Buggy Seat");
        this.item(GCItems.BUGGY_STORAGE, "Buggy Storage");

        // SCHEMATICS
        this.item(GCItems.BASIC_ROCKET_CONE_SCHEMATIC, "Basic Rocket Cone Schematic");
        this.item(GCItems.BASIC_ROCKET_BODY_SCHEMATIC, "Basic Rocket Body Schematic");
        this.item(GCItems.BASIC_ROCKET_FINS_SCHEMATIC, "Basic Rocket Fins Schematic");
        this.item(GCItems.BASIC_ROCKET_ENGINE_SCHEMATIC, "Basic Rocket Engine Schematic");

        this.item(GCItems.PART_SCHEMATIC_COMMAND, "Command Part Schematic");
        this.item(GCItems.PART_SCHEMATIC_ENGINE, "Engine Part Schematic");
        this.item(GCItems.PART_SCHEMATIC_TANK, "Propellant Tank Part Schematic");
        this.item(GCItems.PART_SCHEMATIC_ADAPTER, "Adapter Part Schematic");
        this.item(GCItems.PART_SCHEMATIC_PAYLOAD, "Payload Part Schematic");
        this.item(GCItems.PART_SCHEMATIC_UTILITY, "Utility Part Schematic");

        this.item(GCItems.TIER_2_ROCKET_SCHEMATIC, "Tier 2 Rocket Schematic");
        this.item(GCItems.CARGO_ROCKET_SCHEMATIC, "Cargo Rocket Schematic");
        this.item(GCItems.MOON_BUGGY_SCHEMATIC, "Moon Buggy Schematic");
        this.item(GCItems.TIER_3_ROCKET_SCHEMATIC, "Tier 3 Rocket Schematic");
        this.item(GCItems.ASTRO_MINER_SCHEMATIC, "Astro Miner Schematic");

        // LEGACY_MUSIC_DISCS
        this.musicDisc(GCItems.LEGACY_MUSIC_DISC_MARS, "Legacy Music Disc", "Jackson Cordes - Mars");
        this.add(JukeboxSong.LEGACY_MARS, "Jackson Cordes - Mars");
        this.musicDisc(GCItems.LEGACY_MUSIC_DISC_MIMAS, "Legacy Music Disc", "Jackson Cordes - Mimas");
        this.add(JukeboxSong.LEGACY_MIMAS, "Jackson Cordes - Mimas");
        this.musicDisc(GCItems.LEGACY_MUSIC_DISC_ORBIT, "Legacy Music Disc", "Jackson Cordes - Orbit");
        this.add(JukeboxSong.LEGACY_ORBIT, "Jackson Cordes - Orbit");
        this.musicDisc(GCItems.LEGACY_MUSIC_DISC_SPACERACE, "Legacy Music Disc", "Jackson Cordes - Space Race");
        this.add(JukeboxSong.LEGACY_SPACERACE, "Jackson Cordes - Space Race");

        // SPAWN EGGS
        this.item(GCItems.MOON_VILLAGER_SPAWN_EGG, "Moon Villager Spawn Egg");
        this.item(GCItems.EVOLVED_ZOMBIE_SPAWN_EGG, "Evolved Zombie Spawn Egg");
        this.item(GCItems.EVOLVED_CREEPER_SPAWN_EGG, "Evolved Creeper Spawn Egg");
        this.item(GCItems.EVOLVED_SKELETON_SPAWN_EGG, "Evolved Skeleton Spawn Egg");
        this.item(GCItems.EVOLVED_SPIDER_SPAWN_EGG, "Evolved Spider Spawn Egg");
        this.item(GCItems.EVOLVED_ENDERMAN_SPAWN_EGG, "Evolved Enderman Spawn Egg");
        this.item(GCItems.EVOLVED_WITCH_SPAWN_EGG, "Evolved Witch Spawn Egg");
        this.item(GCItems.EVOLVED_PILLAGER_SPAWN_EGG, "Evolved Pillager Spawn Egg");
        this.item(GCItems.EVOLVED_EVOKER_SPAWN_EGG, "Evolved Evoker Spawn Egg");
        this.item(GCItems.EVOLVED_VINDICATOR_SPAWN_EGG, "Evolved Vindicator Spawn Egg");
        this.item(GCItems.GAZER_SPAWN_EGG, "Gazer Spawn Egg");
        this.item(GCItems.RUMBLER_SPAWN_EGG, "Rumbler Spawn Egg");
        this.item(GCItems.COMET_CUBE_SPAWN_EGG, "Comet Cube Spawn Egg");
        this.item(GCItems.OLI_GRUB_SPAWN_EGG, "Oli Grub Spawn Egg");
        this.item(GCItems.GREY_SPAWN_EGG, "Grey Spawn Egg");
        this.item(GCItems.ARCH_GREY_SPAWN_EGG, "Arch Grey Spawn Egg");
        this.item(GCItems.SLIMELING_SPAWN_EGG, "Slimeling Spawn Egg");
    }

    protected void generateItemTagTranslations() {
        this.tag(GCItemTags.WRENCHES, "Wrenches");
        this.tag(GCItemTags.CUTS_CHEESE, "Cuts Cheese");

        this.tag(GCItemTags.THERMAL_HEAD, "Thermal Padding Helmets");
        this.tag(GCItemTags.THERMAL_CHEST, "Thermal Padding Chestpieces");
        this.tag(GCItemTags.THERMAL_PANTS, "Thermal Padding Pants");
        this.tag(GCItemTags.THERMAL_BOOTS, "Thermal Padding Boots");
        this.tag(GCItemTags.OXYGEN_MASKS, "Oxygen Masks");
        this.tag(GCItemTags.OXYGEN_GEAR, "Oxygen Gear");
        this.tag(GCItemTags.OXYGEN_TANKS, "Oxygen Tanks");
        this.tag(GCItemTags.ACCESSORIES, "Galacticraft Accessories");
        this.tag(GCItemTags.PARACHUTES, "Parachutes");
        this.tag(GCItemTags.FREQUENCY_MODULES, "Frequency Modules");
        this.tag(GCItemTags.SHIELD_CONTROLLERS, "Shield Controllers");

        this.tag(GCItemTags.ACID_RESISTANT, "Sulfuric Acid Resistant Items");

        this.tag(GCItemTags.GLASS_FLUID_PIPES, "Glass Fluid Pipes");
        this.tag(GCItemTags.STAINED_GLASS_FLUID_PIPES, "Stained Glass Fluid Pipes");
        this.tag(GCItemTags.SEALABLE_FLUID_PIPES, "Sealable Fluid Pipes");
        this.tag(GCItemTags.STAINED_SEALABLE_FLUID_PIPES, "Stained Sealable Fluid Pipes");

        this.tag(GCItemTags.FLAGS, "Flags");

        this.tag(GCItemTags.BATTERIES, "Batteries");

        this.tag(GCItemTags.SILICONS, "Silicon");
        this.tag(GCItemTags.OLIVINE_SHARDS, "Olivine Shards");
        this.tag(GCItemTags.LUNAR_SAPPHIRES, "Lunar Sapphires");

        this.tag(GCItemTags.ORE_BEARING_GROUND_MOON_ROCK, "Moon Rock Ore Bearing Ground");
        this.tag(GCItemTags.ORE_BEARING_GROUND_MOON_BASALT, "Moon Basalt Ore Bearing Ground");
        this.tag(GCItemTags.ORE_BEARING_GROUND_LUNASLATE, "Lunaslate Ore Bearing Ground");
        this.tag(GCItemTags.ORE_BEARING_GROUND_MARS_STONE, "Mars Stone Ore Bearing Ground");
        this.tag(GCItemTags.ORE_BEARING_GROUND_ASTEROID_ROCK, "Asteroid Rock Ore Bearing Ground");
        this.tag(GCItemTags.ORE_BEARING_GROUND_SOFT_VENUS_ROCK, "Soft Venus Rock Ore Bearing Ground");
        this.tag(GCItemTags.ORE_BEARING_GROUND_HARD_VENUS_ROCK, "Hard Venus Rock Ore Bearing Ground");

        this.tag(GCItemTags.ORES_IN_GROUND_MOON_ROCK, "Moon Rock Ores In Ground");
        this.tag(GCItemTags.ORES_IN_GROUND_MOON_BASALT, "Moon Basalt Ores In Ground");
        this.tag(GCItemTags.ORES_IN_GROUND_LUNASLATE, "Lunaslate Ores In Ground");
        this.tag(GCItemTags.ORES_IN_GROUND_MARS_STONE, "Mars Stone Ores In Ground");
        this.tag(GCItemTags.ORES_IN_GROUND_ASTEROID_ROCK, "Asteroid Rock Ores In Ground");
        this.tag(GCItemTags.ORES_IN_GROUND_SOFT_VENUS_ROCK, "Soft Venus Rock Ores In Ground");
        this.tag(GCItemTags.ORES_IN_GROUND_HARD_VENUS_ROCK, "Hard Venus Rock Ores In Ground");

        this.tag(GCItemTags.ALUMINUM_ORES, "Aluminum Ores");
        this.tag(GCItemTags.CHEESE_ORES, "Cheese Ores");
        this.tag(GCItemTags.DESH_ORES, "Desh Ores");
        this.tag(GCItemTags.LEAD_ORES, "Lead Ores");
        this.tag(GCItemTags.LUNAR_SAPPHIRE_ORES, "Lunar Sapphire Ores");
        this.tag(GCItemTags.METEORIC_IRON_ORES, "Meteoric Iron Ores");
        this.tag(GCItemTags.NIOBIUM_ORES, "Niobium Ores");
        this.tag(GCItemTags.OLIVINE_ORES, "Olivine Ores");
        this.tag(GCItemTags.SILICON_ORES, "Silicon Ores");
        this.tag(GCItemTags.SOLAR_ORES, "Solar Dust Ores");
        this.tag(GCItemTags.TIN_ORES, "Tin Ores");
        this.tag(GCItemTags.TITANIUM_ORES, "Titanium Ores");

        this.tag(GCItemTags.ALUMINUM_BLOCKS, "Aluminum Blocks");
        this.tag(GCItemTags.DESH_BLOCKS, "Desh Blocks");
        this.tag(GCItemTags.FERRONIOBIUM_BLOCKS, "Ferroniobium Blocks");
        this.tag(GCItemTags.LEAD_BLOCKS, "Lead Blocks");
        this.tag(GCItemTags.METEORIC_IRON_BLOCKS, "Meteoric Iron Blocks");
        this.tag(GCItemTags.NIOBIUM_BLOCKS, "Niobium Blocks");
        this.tag(GCItemTags.TIN_BLOCKS, "Tin Blocks");
        this.tag(GCItemTags.TITANIUM_BLOCKS, "Titanium Blocks");

        this.tag(GCItemTags.SILICON_BLOCKS, "Silicon Blocks");
        this.tag(GCItemTags.CHEESE_BLOCKS, "Cheese Blocks");
        this.tag(GCItemTags.LUNAR_SAPPHIRE_BLOCKS, "Lunar Sapphire Blocks");
        this.tag(GCItemTags.OLIVINE_BLOCKS, "Olivine Blocks");

        this.tag(GCItemTags.RAW_ALUMINUM_BLOCKS, "Raw Aluminum Blocks");
        this.tag(GCItemTags.RAW_DESH_BLOCKS, "Raw Desh Blocks");
        this.tag(GCItemTags.RAW_LEAD_BLOCKS, "Raw Lead Blocks");
        this.tag(GCItemTags.RAW_METEORIC_IRON_BLOCKS, "Raw Meteoric Iron Blocks");
        this.tag(GCItemTags.RAW_NIOBIUM_BLOCKS, "Raw Niobium Blocks");
        this.tag(GCItemTags.RAW_TIN_BLOCKS, "Raw Tin Blocks");
        this.tag(GCItemTags.RAW_TITANIUM_BLOCKS, "Raw Titanium Blocks");

        this.tag(GCItemTags.ALUMINUM_INGOTS, "Aluminum Ingots");
        this.tag(GCItemTags.BRONZE_INGOTS, "Bronze Ingots");
        this.tag(GCItemTags.DESH_INGOTS, "Desh Ingots");
        this.tag(GCItemTags.FERRONIOBIUM_INGOTS, "Ferroniobium Ingots");
        this.tag(GCItemTags.LEAD_INGOTS, "Lead Ingots");
        this.tag(GCItemTags.METEORIC_IRON_INGOTS, "Meteoric Iron Ingots");
        this.tag(GCItemTags.NIOBIUM_INGOTS, "Niobium Ingots");
        this.tag(GCItemTags.STEEL_INGOTS, "Steel Ingots");
        this.tag(GCItemTags.TIN_INGOTS, "Tin Ingots");
        this.tag(GCItemTags.TITANIUM_INGOTS, "Titanium Ingots");

        this.tag(GCItemTags.ALUMINUM_RAW_MATERIALS, "Raw Aluminum");
        this.tag(GCItemTags.DESH_RAW_MATERIALS, "Raw Desh");
        this.tag(GCItemTags.LEAD_RAW_MATERIALS, "Raw Lead");
        this.tag(GCItemTags.METEORIC_IRON_RAW_MATERIALS, "Raw Meteoric Iron");
        this.tag(GCItemTags.NIOBIUM_RAW_MATERIALS, "Raw Niobium");
        this.tag(GCItemTags.TIN_RAW_MATERIALS, "Raw Tin");
        this.tag(GCItemTags.TITANIUM_RAW_MATERIALS, "Raw Titanium");

        this.tag(GCItemTags.ALUMINUM_NUGGETS, "Aluminum Nuggets");
        this.tag(GCItemTags.DESH_NUGGETS, "Desh Nuggets");
        this.tag(GCItemTags.LEAD_NUGGETS, "Lead Nuggets");
        this.tag(GCItemTags.METEORIC_IRON_NUGGETS, "Meteoric Iron Nuggets");
        this.tag(GCItemTags.NIOBIUM_NUGGETS, "Niobium Nuggets");
        this.tag(GCItemTags.TIN_NUGGETS, "Tin Nuggets");
        this.tag(GCItemTags.TITANIUM_NUGGETS, "Titanium Nuggets");

        this.tag(GCItemTags.PLATES, "Metal Plates");
        this.tag(GCItemTags.ALUMINUM_PLATES, "Compressed Aluminum");
        this.tag(GCItemTags.BRONZE_PLATES, "Compressed Bronze");
        this.tag(GCItemTags.COPPER_PLATES, "Compressed Copper");
        this.tag(GCItemTags.DESH_PLATES, "Compressed Desh");
        this.tag(GCItemTags.FERRONIOBIUM_PLATES, "Compressed Ferroniobium");
        this.tag(GCItemTags.IRON_PLATES, "Compressed Iron");
        this.tag(GCItemTags.METEORIC_IRON_PLATES, "Compressed Meteoric Iron");
        this.tag(GCItemTags.NIOBIUM_PLATES, "Compressed Niobium");
        this.tag(GCItemTags.STEEL_PLATES, "Compressed Steel");
        this.tag(GCItemTags.TIN_PLATES, "Compressed Tin");
        this.tag(GCItemTags.TITANIUM_PLATES, "Compressed Titanium");
        this.tag(GCItemTags.HEAVY_DUTY_PLATES, "Heavy Duty Plates");
        this.tag(GCItemTags.TIER_1_HEAVY_DUTY_PLATES, "Tier 1 Heavy Duty Plates");
        this.tag(GCItemTags.TIER_2_HEAVY_DUTY_PLATES, "Tier 2 Heavy Duty Plates");
        this.tag(GCItemTags.TIER_3_HEAVY_DUTY_PLATES, "Tier 3 Heavy Duty Plates");

        this.tag(GCItemTags.DESH_RODS, "Desh Rods");
        this.tag(GCItemTags.STEEL_RODS, "Steel Rods");

        this.tag(GCItemTags.CANISTERS, "Canisters");
        this.tag(GCItemTags.COPPER_CANISTERS, "Copper Canisters");
        this.tag(GCItemTags.TIN_CANISTERS, "Tin Canisters");

        this.tag(GCItemTags.SOLAR_DUSTS, "Solar Dusts");
        this.tag(GCItemTags.NIOBIUM_OXIDE_DUSTS, "Niobium Oxide Dusts");

        this.tag(GCItemTags.CHEESE_FOODS, "Cheese Foods");
        this.tag(GCItemTags.CANNED_FOODS, "Canned Foods");
        this.tag(GCItemTags.UNCANNABLE_FOODS, "Uncannable Foods");

        this.tag(GCItemTags.BURGER_BUN_SEEDS, "Burger Bun Seeds");

        this.tag(GCItemTags.ROCKET_STORAGE_UPGRADE_ITEMS, "Rocket Storage Upgrade Items");

        this.tag(GCItemTags.EVOLVED_CREEPER_DROP_MUSIC_DISCS, "Evolved Creeper Music Disc Drops");

        this.tag(GCItemTags.SLABS, "Galacticraft Slabs");
        this.tag(GCItemTags.STAIRS, "Galacticraft Stairs");
        this.tag(GCItemTags.WALLS, "Galacticraft Walls");

        this.tag(GCItemTags.MOON_COBBLESTONES, "Moon Cobblestones");
        this.tag(GCItemTags.LUNASLATE_COBBLESTONES, "Lunaslate Cobblestones");
        this.tag(GCItemTags.MARS_COBBLESTONES, "Mars Cobblestones");

        this.tag(GCItemTags.ASTEROID_ROCKS, "Asteroid Rocks");

        this.tag(GCItemTags.DECORATION_BLOCKS, "Decoration Blocks");
        this.tag(GCItemTags.ALUMINUM_DECORATION_BLOCKS, "Aluminum Decoration Blocks");
        this.tag(GCItemTags.BRONZE_DECORATION_BLOCKS, "Bronze Decoration Blocks");
        this.tag(GCItemTags.COPPER_DECORATION_BLOCKS, "Copper Decoration Blocks");
        this.tag(GCItemTags.DARK_DECORATION_BLOCKS, "Dark Decoration Blocks");
        this.tag(GCItemTags.IRON_DECORATION_BLOCKS, "Iron Decoration Blocks");
        this.tag(GCItemTags.METEORIC_IRON_DECORATION_BLOCKS, "Meteoric Iron Decoration Blocks");
        this.tag(GCItemTags.STEEL_DECORATION_BLOCKS, "Steel Decoration Blocks");
        this.tag(GCItemTags.TIN_DECORATION_BLOCKS, "Tin Decoration Blocks");
        this.tag(GCItemTags.TITANIUM_DECORATION_BLOCKS, "Titanium Decoration Blocks");

        this.tag(GCItemTags.OIL_BUCKETS, "Oil Buckets");
        this.tag(GCItemTags.FUEL_BUCKETS, "Fuel Buckets");
        this.tag(GCItemTags.SULFURIC_ACID_BUCKETS, "Sulfuric Acid Buckets");
    }

    protected void generateFluidTagTranslations() {
        this.tag(GCFluidTags.OIL, "Galacticraft Oil");
        this.tag(GCFluidTags.OIL_COMMON, "Oil");
        this.tag(GCFluidTags.FUEL, "Galacticraft Fuel");
        this.tag(GCFluidTags.FUEL_COMMON, "Fuel");
        this.tag(GCFluidTags.SULFURIC_ACID, "Galacticraft Sulfuric Acid");
        this.tag(GCFluidTags.SULFURIC_ACID_COMMON, "Sulfuric Acid");
        this.tag(GCFluidTags.LIQUID_OXYGEN, "Liquid Oxygen");
        this.tag(GCFluidTags.LIQUID_HYDROGEN, "Galacticraft Liquid Hydrogen");
        this.tag(GCFluidTags.LIQUID_HYDROGEN_COMMON, "Liquid Hydrogen");
        this.tag(GCFluidTags.LIQUID_METHANE, "Galacticraft Liquid Methane");
        this.tag(GCFluidTags.LIQUID_METHANE_COMMON, "Liquid Methane");
        this.tag(GCFluidTags.MONOPROPELLANT, "Galacticraft Monopropellant");
        this.tag(GCFluidTags.MONOPROPELLANT_COMMON, "Monopropellant");
        this.tag(GCFluidTags.LIQUID_XENON, "Galacticraft Liquid Xenon");
        this.tag(GCFluidTags.LIQUID_XENON_COMMON, "Liquid Xenon");
        this.tag(GCFluidTags.OXYGEN, "Oxygen");
        this.tag(GCFluidTags.NON_BREATHABLE, "Non-Breathable Fluids");
        this.tag(GCFluidTags.FLUID_CANISTER_EXCLUSIONS, "Excluded from Fluid Canisters");
    }

    protected void generateGasTranslations() {
        this.add(Gas.ARGON, "Argon");
        this.add(Gas.CARBON_DIOXIDE, "Carbon Dioxide");
        this.add(Gas.CARBON_MONOXIDE, "Carbon Monoxide");
        this.add(Gas.DEUTERIUM, "Deuterium");
        this.add(Gas.HELIUM, "Helium");
        this.add(Gas.HELIUM_3, "Helium-3");
        this.add(Gas.HYDROGEN, "Hydrogen");
        this.add(Gas.HYDROGEN_DEUTERIUM_OXIDE, "Hydrogen Deuterium Oxide");
        this.add(Gas.IODINE, "Iodine");
        this.add(Gas.KRYPTON, "Krypton");
        this.add(Gas.METHANE, "Methane");
        this.add(Gas.NEON, "Neon");
        this.add(Gas.NITRIC_OXIDE, "Nitric Oxide");
        this.add(Gas.NITROGEN, "Nitrogen");
        this.add(Gas.NITROGEN_DIOXIDE, "Nitrogen Dioxide");
        this.add(Gas.NITROUS_OXIDE, "Nitrous Oxide");
        this.add(Gas.OXYGEN, "Oxygen");
        this.add(Gas.OZONE, "Ozone");
        this.add(Gas.SOLAR_VOLATILE_GAS, "Solar Volatile Gas");
        this.add(Gas.WATER_VAPOR, "Water Vapor");
        this.add(Gas.XENON, "Xenon");
    }

    protected void generateEntityTranslations() {
        this.entity(GCEntityTypes.ARCH_GREY, "Arch Grey");
        this.entity(GCEntityTypes.BUBBLE, "Bubble");
        this.entity(GCEntityTypes.BUGGY, "Buggy");
        this.entity(GCEntityTypes.COMET_CUBE, "Comet Cube");
        this.entity(GCEntityTypes.EVOLVED_CREEPER, "Evolved Creeper");
        this.entity(GCEntityTypes.EVOLVED_ENDERMAN, "Evolved Enderman");
        this.entity(GCEntityTypes.EVOLVED_EVOKER, "Evolved Evoker");
        this.entity(GCEntityTypes.EVOLVED_PILLAGER, "Evolved Pillager");
        this.entity(GCEntityTypes.EVOLVED_SKELETON, "Evolved Skeleton");
        this.entity(GCEntityTypes.SKELETON_BOSS, "Evolved Skeleton Boss");
        this.entity(GCEntityTypes.EVOLVED_SPIDER, "Evolved Spider");
        this.entity(GCEntityTypes.EVOLVED_VINDICATOR, "Evolved Vindicator");
        this.entity(GCEntityTypes.EVOLVED_WITCH, "Evolved Witch");
        this.entity(GCEntityTypes.EVOLVED_ZOMBIE, "Evolved Zombie");
        this.entity(GCEntityTypes.GAZER, "Gazer");
        this.entity(GCEntityTypes.GREY, "Grey");
        this.entity(GCEntityTypes.LANDER, "Lander");
        this.entity(GCEntityTypes.MOON_VILLAGER, "Moon Villager");
        this.add(GCEntityTypes.MOON_VILLAGER.getDescriptionId() + ".none", "Moon Villager");
        this.entity(GCEntityTypes.OLI_GRUB, "Oli Grub");
        this.entity(GCEntityTypes.PARACHEST, "Parachest");
        this.entity(GCEntityTypes.ROCKET, "Rocket");
        this.entity(GCEntityTypes.RUMBLER, "Rumbler");
        this.entity(GCEntityTypes.SLIMELING, "Slimeling");
        this.entity(GCEntityTypes.GRAPPLE, "Grapple Hook");
        this.entity(GCEntityTypes.THROWABLE_METEOR_CHUNK, "Meteor Chunk");
        this.add("chat.galacticraft.telepad.target", "Telepad Target: %s");
        this.add("chat.galacticraft.telepad.no_target", "Telepad has no target set.");
    }

    protected void generateBannerPatternTranslations() {
        for (DyeColor color : DyeColor.values()) {
            String normalized = TranslationProvider.normalizeName(color.getName());
            this.add(BannerPattern.ROCKET + "." + color.getName(), normalized + " Rocket");
            this.add(BannerPattern.ROCKET_INVERTED + "." + color.getName(), normalized + " Rocket Inverted");
            this.add(BannerPattern.ROCKET_SINISTER + "." + color.getName(), normalized + " Rocket Sinister");
            this.add(BannerPattern.ROCKET_SINISTER_INVERTED + "." + color.getName(), normalized + " Rocket Sinister Inverted");

            this.add(BannerPattern.CREEPER_SIDEWAYS + "." + color.getName(), normalized + " Sideways Creeper Charge");
            this.add(BannerPattern.FLOW_SIDEWAYS + "." + color.getName(), normalized + " Sideways Flow");
            this.add(BannerPattern.FLOWER_SIDEWAYS + "." + color.getName(), normalized + " Sideways Flower");
            this.add(BannerPattern.GLOBE_SIDEWAYS + "." + color.getName(), normalized + " Sideways Globe");
            this.add(BannerPattern.GUSTER_SIDEWAYS + "." + color.getName(), normalized + " Sideways Guster");
            this.add(BannerPattern.MOJANG_SIDEWAYS + "." + color.getName(), normalized + " Sideways Thing");
            this.add(BannerPattern.PIGLIN_SIDEWAYS + "." + color.getName(), normalized + " Sideways Snout");
            this.add(BannerPattern.SKULL_SIDEWAYS + "." + color.getName(), normalized + " Sideways Skull Charge");
        }
    }

    protected void generateCelestialBodyTranslations() {
        this.add(Galaxy.MILKY_WAY, "Milky Way");
        this.add(Galaxy.MILKY_WAY_DESCRIPTION, "");
        this.add(CelestialBody.SOL, "Sol");
        this.add(CelestialBody.SOL_DESC, "");
        this.add(CelestialBody.ASTEROID, "Asteroids");
        this.add(CelestialBody.ASTEROID_DESC, "");
        this.add(CelestialBody.EARTH, "Earth");
        this.add(CelestialBody.EARTH_DESC, "The Overworld");
        this.add(CelestialBody.JUPITER, "Jupiter");
        this.add(CelestialBody.JUPITER_DESC, "The undisputed titan of the Solar System, Jupiter is a colossal gas giant comprising over 317 Earth masses (1.898 x 10^27 kg). Rotating rapidly in just 9 hours and 55 minutes, its violent equatorial winds and ferocious Coriolis forces generate massive banded storm belts and anticyclonic vortices like the centuries-old Great Red Spot. Despite its vast volume, Jupiter exhibits an effective equatorial surface gravity of 2.36 g. Beneath a hyperbaric atmosphere of molecular hydrogen (89.8%) and helium (10.2%) laced with trace methane and ammonia, supercritical fluids transition under multi-megabar pressures into an immense liquid metallic hydrogen mantle. This superconducting sea powers a planetary dynamo 14 times stronger than Earth's, trapping lethal relativistic radiation belts above a diffuse, dissolved heavy-element core.");
        this.add(CelestialBody.MARS, "Mars");
        this.add(CelestialBody.MARS_DESC, "");
        this.add(CelestialBody.MERCURY, "Mercury");
        this.add(CelestialBody.MERCURY_DESC, "");
        this.add(CelestialBody.MOON, "Moon");
        this.add(CelestialBody.MOON_DESC, "");
        this.add(CelestialBody.NEPTUNE, "Neptune");
        this.add(CelestialBody.NEPTUNE_DESC, "The outermost major planet of the Solar System, Neptune is a dense, vivid ultramarine ice giant holding 17.1 Earth masses (1.024 x 10^26 kg) in a compact volume, resulting in an effective surface gravity of 1.14 g. Rotating once every 16 hours and 6 minutes, Neptune receives minimal solar irradiance yet radiates 2.6 times more internal heat than it absorbs from the Sun. This intense thermal engine fuels the most violent meteorology in the planetary system, driving supersonic retrograde winds exceeding 2,160 km/h and spawning massive anticyclonic Dark Spots across an atmosphere of hydrogen (80.0%), helium (19.0%), and methane (1.5%). In the abyss beneath high-altitude methane cirrus and frozen nitrogen ice lies a turbulent superionic volatile mantle, subject to extreme pyrolysis where carbon crystallizes into diamond hailstorms sinking toward a dense silicate-metal core.");
        this.add(CelestialBody.SATURN, "Saturn");
        this.add(CelestialBody.SATURN_DESC, "Crowned by an awe-inspiring ring system spanning thousands of kilometers of micronized water ice, Saturn is the Solar System's second-largest gas giant with a mass of 95.2 Earth masses (5.683 x 10^26 kg). Possessing the lowest mean density of any planet (0.687 g/cm^3), its rapid 10-hour and 33-minute rotation causes severe oblateness, reducing its effective equatorial surface gravity to a comfortable 0.916 g. Saturn's golden atmosphere consists primarily of molecular hydrogen (96.3%) and helium (3.3%), with upper layers depleted by deep helium phase separation. Within its interior, condensed helium droplets precipitate as continuous 'helium rain' through liquid metallic hydrogen, releasing gravitational potential energy that powers the planet's vast internal heat flux and drives perpetual jet streams, including the famous northern polar hexagon.");
        this.add(CelestialBody.URANUS, "Uranus");
        this.add(CelestialBody.URANUS_DESC, "A pale cyan ice giant rolling on its side with an extreme axial tilt of 97.8 degrees, Uranus revolves through 84-year orbital cycles while completing a retrograde sidereal rotation every 17 hours and 14 minutes. With a mass of 14.5 Earth masses (8.681 x 10^25 kg) and an effective equatorial gravity of 0.886 g, it harbors the coldest planetary atmosphere in the Solar System, plunging to a cryogenic 49 Kelvin (-224°C). Its atmosphere of molecular hydrogen (82.5%), helium (15.2%), and methane (2.3%) selectively absorbs red light to produce its serene aquamarine hue. Beneath toxic clouds of hydrogen sulfide ice lies a scalding, hyper-pressurized mantle of superionic water (Ice XVIII), where oxygen lattices and flowing protons generate an off-axis multipolar magnetic field, while thermal pyrolysis of methane generates torrential downpours of solid diamond rain.");
        this.add(CelestialBody.VENUS, "Venus");
        this.add(CelestialBody.VENUS_DESC, "");
        this.add(CelestialBody.SATELLITE, "Satellite");
        this.add(CelestialBody.SATELLITES, "Satellites");
        this.add(CelestialBody.SATELLITE_DESC, "");
    }

    protected void generateBiomeTranslations() {
        this.add(GCBiomes.Moon.BASALTIC_MARE, "Basaltic Mare");
        this.add(GCBiomes.Moon.COMET_TUNDRA, "Comet Tundra");
        this.add(GCBiomes.Moon.LUNAR_HIGHLANDS, "Lunar Highlands");
        this.add(GCBiomes.Moon.LUNAR_LOWLANDS, "Lunar Lowlands");
        this.add(GCBiomes.Moon.OLIVINE_SPIKES, "Olivine Spikes");
        this.add(GCBiomes.SPACE, "Space");
        this.add(GCBiomes.Mars.MARS_FLAT, "Mars Flat");
        this.add(GCBiomes.Mars.MARS_CANYON, "Mars Canyon");
        this.add(GCBiomes.Mars.MARS_HIGHLANDS, "Mars Highlands");
        this.add(GCBiomes.Venus.VENUS_FLAT, "Venus Flat");
        this.add(GCBiomes.Venus.VENUS_MOUNTAIN, "Venus Mountain");
        this.add(GCBiomes.Venus.VENUS_VALLEY, "Venus Valley");

        this.add(GCBiomes.Mercury.INTER_CRATER_PLAINS, "Inter-Crater Plains");
        this.add(GCBiomes.Mercury.SMOOTH_LAVA_PLAINS, "Smooth Lava Plains");
        this.add(GCBiomes.Mercury.CALORIS_BASIN, "Caloris Basin");
        this.add(GCBiomes.Mercury.CHAOTIC_TERRAIN, "Chaotic Terrain");
        this.add(GCBiomes.Mercury.POLAR_DARK_CRATERS, "Permanently Shadowed Polar Craters");

        this.add(GCBiomes.Mercury.COLOSSAL_LAVA_TUBES, "Colossal Lava Tubes");
        this.add(GCBiomes.Mercury.MEGABRECCIA_CAVERNS, "Megabreccia Caverns");
        this.add(GCBiomes.Mercury.TECTONIC_CHASMS, "Tectonic Chasms");
        this.add(GCBiomes.Mercury.MAGMA_DEPTHS, "Magma Depths");
        this.add(GCBiomes.Mercury.HOLLOW_LABYRINTH, "Volatile Hollow Labyrinth");

        this.add(GCBiomes.Jupiter.AMMONIA_PLAINS, "Ammonia Cloud Plains");
        this.add(GCBiomes.Jupiter.GREAT_RED_SPOT, "Great Red Spot Vortex");
        this.add(GCBiomes.Jupiter.SULFURIC_BELTS, "Sulfuric Storm Belts");
        this.add(GCBiomes.Jupiter.METALLIC_RIDGES, "Metallic Hydrogen Ridges");
        this.add(GCBiomes.Jupiter.POLAR_AURORA_CRATERS, "Polar Aurora Craters");
        this.add(GCBiomes.Jupiter.METALLIC_HYDROGEN_CAVERNS, "Metallic Hydrogen Caverns");
        this.add(GCBiomes.Jupiter.CYCLONIC_RIFTS, "Cyclonic Rifts");
        this.add(GCBiomes.Jupiter.CORE_MAGMA_DEPTHS, "Core Magma Depths");

        this.add(GCBiomes.Saturn.GOLDEN_HAZE_PLAINS, "Golden Haze Plains");
        this.add(GCBiomes.Saturn.HYDROCARBON_SEAS, "Hydrocarbon Seas");
        this.add(GCBiomes.Saturn.RING_SHADOW_HIGHLANDS, "Ring Shadow Highlands");
        this.add(GCBiomes.Saturn.AMMONIA_STORM_BANDS, "Ammonia Storm Bands");
        this.add(GCBiomes.Saturn.HEXAGON_POLAR_VORTEX, "Hexagon Polar Vortex");
        this.add(GCBiomes.Saturn.HELIUM_RAIN_CAVERNS, "Helium Rain Caverns");
        this.add(GCBiomes.Saturn.CRYSTALLINE_RING_TUBES, "Crystalline Ring Tubes");
        this.add(GCBiomes.Saturn.MANTLE_CORE_ABYSS, "Mantle Core Abyss");

        this.add(GCBiomes.Uranus.FROZEN_METHANE_PLAINS, "Frozen Methane Plains");
        this.add(GCBiomes.Uranus.MIRANDA_VERONA_CLIFFS, "Miranda Verona Cliffs");
        this.add(GCBiomes.Uranus.CRYO_GEYSER_BASIN, "Cryo-Geyser Basin");
        this.add(GCBiomes.Uranus.TILTED_POLAR_CAPS, "Tilted Polar Caps");
        this.add(GCBiomes.Uranus.RING_DEBRIS_BADLANDS, "Ring Debris Badlands");
        this.add(GCBiomes.Uranus.DIAMOND_MANTLE_CAVERNS, "Diamond Mantle Caverns");
        this.add(GCBiomes.Uranus.GLACIAL_FAULT_CHASMS, "Glacial Fault Chasms");
        this.add(GCBiomes.Uranus.SUPERIONIC_ICE_DEPTHS, "Superionic Ice Depths");

        this.add(GCBiomes.Neptune.AZURE_METHANE_PLAINS, "Azure Methane Plains");
        this.add(GCBiomes.Neptune.GREAT_DARK_SPOT, "Great Dark Spot");
        this.add(GCBiomes.Neptune.TRITON_NITROGEN_GEYSERS, "Triton Nitrogen Geysers");
        this.add(GCBiomes.Neptune.SUPERSONIC_WIND_RIDGES, "Supersonic Wind Ridges");
        this.add(GCBiomes.Neptune.CRYOGENIC_POLAR_ABYSS, "Cryogenic Polar Abyss");
        this.add(GCBiomes.Neptune.DIAMOND_RAIN_CAVERNS, "Diamond Rain Caverns");
        this.add(GCBiomes.Neptune.CRYO_MAGMA_TUBES, "Cryo-Magma Tubes");
        this.add(GCBiomes.Neptune.ABYSSAL_MANTLE_DEPTHS, "Abyssal Mantle Depths");
    }

    protected void generateChatTranslations() {
        this.add(Chat.BED_FAIL, "Uh oh, what if the oxygen runs out when I am asleep? I'll need a Cryogenic Chamber to sleep in space!");
        this.add(Chat.CHAMBER_HOT, "The chamber is way too hot right now! It needs %s seconds to cool down before I sleep again.");
        this.add(Chat.CHAMBER_OBSTRUCTED, "This cryogenic chamber is obstructed");
        this.add(Chat.CHAMBER_OCCUPIED, "This cryogenic chamber is occupied");
        this.add(Chat.CHAMBER_TOO_FAR_AWAY, "You may not rest now; the cryogenic chamber is too far away");
        this.add(Chat.ROCKET_WARNING, "Press again to launch.");

        this.add(Chat.CANNOT_EAT_IN_NO_ATMOSPHERE, "You can't eat that while holding your breath.");
        this.add(Chat.CANNOT_EAT_WITH_MASK, "You can't eat that while wearing a mask.");
        this.add(Chat.CANNOT_FEED_IN_NO_ATMOSPHERE, "You can't feed a pet that is holding its breath.");
        this.add(Chat.CANNOT_FEED_WITH_MASK, "You can't feed that to your pet while it is wearing a mask.");

        this.add(RegistryDebug.DUMP, "Dumped: %s");
        this.add(RegistryDebug.ID, "%s - %s: %s");

        this.add(SetOxygen.SUCCESS_MULTIPLE, "Set oxygen at blocks");
        this.add(SetOxygen.SUCCESS_SINGLE, "Set oxygen at block");
        this.add(SetOxygen.OXYGEN_EXISTS, "Oxygen exists at block");
        this.add(SetOxygen.NO_OXYGEN_EXISTS, "No oxygen at block");
        this.add(SetOxygen.FULL_OXYGEN, "Area is filled with oxygen");
        this.add(SetOxygen.PARTIAL_OXYGEN, "Area partially contains oxygen");
        this.add(SetOxygen.EMPTY_OXYGEN, "Area contains no oxygen");

        this.add(DimensionTp.SUCCESS_MULTIPLE, "Teleported %s entities to %s");
        this.add(DimensionTp.SUCCESS_SINGLE, "Teleported to %s");
        this.add(DimensionTp.INVALID_PACKET, "Invalid planet teleport packet received.");

        this.add(GcHouston.IN_OTHER_DIMENSION, "We cannot locate your signal! Are you sure you're in space?");
        this.add(GcHouston.CONFIRMATION, "Er, Houston, we have a problem... (Run this command again to confirm teleport)");
        this.add(GcHouston.IN_OVERWORLD, "I don't need to be rescued!");
        this.add(GcHouston.SUCCESS, "You have been rescued. Better luck next time...");

        this.deathBy(GCDamageTypes.CRASH_LANDING, "%s came in too hot");
        this.deathBy(GCDamageTypes.METEOR_STRIKE, "%s was struck by a meteor",
                "%s was struck by a meteor hurled by %s");
        this.deathBy(GCDamageTypes.OIL_BOOM, "%s tried to put out fire with a very flammable material",
                "%s tried to put out fire with a very flammable material while trying to escape %s");
        this.deathBy(GCDamageTypes.SUFFOCATION, "%s died from lack of oxygen",
                "%s ran out of oxygen while trying to escape %s");
        this.deathBy(GCDamageTypes.SULFURIC_ACID, "%s was dissolved by sulfuric acid",
                "%s was dissolved by sulfuric acid while trying to escape %s");
        this.deathBy(GCDamageTypes.VINE_POISON, "%s succumbed to the poison of some vines",
                "%s succumbed to the poison of some vines while trying to escape %s");

        this.add(Boss.SKELETON_BOSS_DESPAWN, "Boss despawned, don't leave the boss room while fighting! Re-enter room to respawn boss.");
    }

    protected void generateRocketPartTranslations() {
        this.rocketPart(GCRocketParts.TIER_1_BODY, "Basic Body");
        this.rocketPart(GCRocketParts.ADVANCED_CONE, "Advanced Cone");
        this.rocketPart(GCRocketParts.SLOPED_CONE, "Sloped Cone");
        this.rocketPart(GCRocketParts.TIER_1_CONE, "Basic Cone");
        this.rocketPart(GCRocketParts.TIER_1_ENGINE, "Basic Engine");
        this.rocketPart(GCRocketParts.TIER_1_FIN, "Basic Fins");
        this.rocketPart(GCRocketParts.STORAGE_UPGRADE, "Storage Upgrade");
        this.rocketPart(GCRocketParts.EXPLOSIVE_UPGRADE, "Explosive Upgrade");
    }

    protected void generateSmithingTranslations() {
        this.add(Misc.UPGRADE_TITANIUM_ADDITIONS_SLOT_DESCRIPTON, "Add Compressed Titanium");
        this.add(Misc.UPGRADE_TITANIUM_APPLIES_TO, "Desh Equipment");
        this.add(Misc.UPGRADE_TITANIUM_BASE_SLOT_DESCRIPTION, "Add desh armor, weapon, or tool");
        this.add(Misc.UPGRADE_TITANIUM_DESCRIPTION, "Titanium Upgrade");
        this.add(Misc.UPGRADE_TITANIUM_INGREDIENTS, "Compressed Titanium");
    }

    protected void generateSubtitleTranslations() {
        this.add(Subtitles.AIRLOCK_CLOSE, "Airlock closes");
        this.add(Subtitles.AIRLOCK_OPEN, "Airlock opens");
        this.add(Subtitles.AMBIENT_SCARYSCAPE, "Eerie noise");
        this.add(Subtitles.AMBIENT_SINGLE_DRIP, "Liquid drips");
        this.add(Subtitles.ASTROMINER, "Astrominer flies");
        this.add(Subtitles.CHEST_UNLOCK, "Chest unlocks");
        this.add(Subtitles.CIRCUIT_SCRITCH, "Circuit Fabricator scritches");
        this.add(Subtitles.MACHINE_BUZZ, "Machine buzzes");
        this.add(Subtitles.MACHINE_WHIR, "Machine whirs");
        this.add(Subtitles.METEOR_THROW, "Meteor Chunk flies");
        this.add(Subtitles.OXYGEN_FAN, "Oxygen Fan hums");
        this.add(Subtitles.PARACHUTE, "Parachute opens");
        this.add(Subtitles.ROCKET_IGNITE, "Rocket ignites");
        this.add(Subtitles.SKELETON_BOSS_DEATH, "Evolved Skeleton Boss dies");
        this.add(Subtitles.SKELETON_BOSS_LAUGH, "Evolved Skeleton Boss laughs");
        this.add(Subtitles.SKELETON_BOSS_OOH, "Ooh");
        this.add(Subtitles.SKELETON_BOSS_OUCH, "Ouch");
        this.add(Subtitles.SLIMELING_DEATH, "Slimeling dies");
    }

    protected void generateTooltipTranslations() {
        // Block Descriptions
        this.blockDesc(GCBlocks.ADVANCED_SOLAR_PANEL, "A more efficient solar panel that rotates to face the sun.");
        this.blockDesc(GCBlocks.BASIC_SOLAR_PANEL, "Converts energy from the sun into electricity. Most effective at noon.");
        this.blockDesc(GCBlocks.CIRCUIT_FABRICATOR, "Fabricates the silicon wafers needed for more advanced machines.");
        this.blockDesc(GCBlocks.COAL_GENERATOR, "Burns coal and charcoal to generate electricity.");
        this.blockDesc(GCBlocks.COMPRESSOR, "Compresses ingots to create stronger materials. Requires furnace fuel to run.");
        this.blockDesc(GCBlocks.ELECTRIC_COMPRESSOR, "Compresses ingots to create stronger materials.");
        this.blockDesc(GCBlocks.ELECTRIC_FURNACE, "Uses electricity instead of fuel to smelt items.");
        this.blockDesc(GCBlocks.ELECTRIC_ARC_FURNACE, "A faster, electric blast furnace for smelting ores.");
        this.blockDesc(GCBlocks.ENERGY_STORAGE_MODULE, "Stores a large amount of energy.");
        this.blockDesc(GCBlocks.FOOD_CANNER, "Compresses several food items into a single can. Canned foods can be eaten while wearing an oxygen mask.");
        this.blockDesc(GCBlocks.FUEL_LOADER, "Loads fuel into a rocket placed on an adjacent launch pad.");
        this.blockDesc(GCBlocks.GLOWSTONE_LANTERN, "Provides light, even in areas without oxygen.");
        this.blockDesc(GCBlocks.GLOWSTONE_TORCH, "Provides light, even in areas without oxygen.");
        this.blockDesc(GCBlocks.MOON_CHEESE_WHEEL, "Can be placed and eaten like a cake. Made with all-natural Moon cheese.");
        this.blockDesc(GCBlocks.OXYGEN_BUBBLE_DISTRIBUTOR, "Creates a breathable bubble of oxygen in a small area.");
        this.blockDesc(GCBlocks.OXYGEN_DECOMPRESSOR, "Empties oxygen tanks.");
        this.blockDesc(GCBlocks.OXYGEN_COLLECTOR, "Collects oxygen from nearby crops and trees.");
        this.blockDesc(GCBlocks.OXYGEN_DETECTOR, "Will emit a redstone signal when surrounded by oxygen.");
        this.blockDesc(GCBlocks.OXYGEN_COMPRESSOR, "Fills oxygen tanks.");
        this.blockDesc(GCBlocks.OXYGEN_SEALER, "Fills an airtight room with oxygen to make it breathable.");
        this.blockDesc(GCBlocks.OXYGEN_STORAGE_MODULE, "Stores a large amount of oxygen.");
        this.blockDesc(GCBlocks.PARACHEST, "Falls from the sky when you travel to certain planets, carrying your rocket, fuel, and cargo.");
        this.blockDesc(GCBlocks.REFINERY, "Refines crude oil into fuel used by rockets and vehicles.");
        this.blockDesc(GCBlocks.CRASHED_PROBE, "A damaged probe that landed on Venus. Salvageable for radioisotope parts.");
        this.blockDesc(GCBlocks.GEOTHERMAL_GENERATOR, "Harnesses geothermal energy from vapor spouts to generate electricity.");
        this.blockDesc(GCBlocks.REGOLITH_THERMAL_DESORBER, "Heats lunar and mercurian regolith above 700°C in vacuum to desorb solar volatile gases and recover titanium and silicon.");
        this.blockDesc(GCBlocks.ISOTOPIC_CENTRIFUGE, "Magnetically separates Helium-3 from solar volatile gas and enriched planetary ores using a superconducting coil.");
        this.blockDesc(GCBlocks.HELIUM3_FUSION_REACTOR, "3x3x3 multiblock aneutronic D-3He MHD fusion reactor controller generating 2,500-5,000 GJ/t cleanly with zero radiation.");
        this.blockDesc(GCBlocks.FUSION_REACTOR_CASING, "Niobium-titanium structural containment casing for the 3x3x3 Helium-3 Fusion Reactor torus.");
        this.blockDesc(GCBlocks.FUSION_MAGNET_COIL, "Nb3Sn superconducting toroidal field magnet required at the 4 corners and top/bottom core of the Helium-3 Fusion Reactor.");
        this.blockDesc(GCBlocks.FUSION_PLASMA_GLASS, "Sapphire-reinforced vacuum window for viewing the live MHD plasma core inside the Helium-3 Fusion Reactor.");

        this.add(Tooltip.CREATIVE_ONLY, "Creative Only");
        this.add(Tooltip.ENERGY_REMAINING, "Energy Remaining: %s");
        this.add(Tooltip.INFINITE, "Infinite");
        this.add(Tooltip.OXYGEN_REMAINING, "Oxygen Remaining: %s");
        this.add(Tooltip.PRESS_SHIFT, "Press LSHIFT for more information");
        this.add(Tooltip.STANDARD_WRENCH, "Rotates and configures various Galacticraft blocks.");
        this.add(Tooltip.TIME_UNTIL_COOL, "Time Until Cool: %s");
        this.add(Tooltip.SECONDS_UNIT, "%ss");
        this.add(Tooltip.INCORRECT_NUMBER_OF_SLOTS, "Incorrect number of slots in the workstation for this recipe.");

        this.add(Tooltip.FLUID_CANISTER_EMPTY, "Empty");
        this.add(Tooltip.FLUID_CANISTER_FLUID_INFO, "%s: %s");
        this.add(Tooltip.FLUID_CANISTER_LOX, "LOX");
        this.add(Tooltip.ATOMIC_BATTERY_OUTPUT, "Infinite Output: %s EU/t");
        this.add("tooltip.galacticraft.open_aerospace_manual", "Press [%s] to open in Aerospace Manual");
    }

    protected void generateConfigTranslations() {
        this.add(Config.TITLE, "Galacticraft Config");
        this.add(Config.RESET, "Reset");

        this.add(Config.CLIENT, "Client");
        this.add(Config.SQUARE_CANNED_FOOD, "Square Canned Food");
        this.add(Config.FLUID_CANISTER_CAPACITY, "Fluid Canister Capacity");
        this.add(Config.SKYBOX, "Skybox");
        this.add(Config.PLAYER, "Player");

        this.add(Config.COMMANDS, "Commands");
        this.add(Config.ENABLE_GC_HOUSTON, "Enable /gchouston");
        this.add(Config.MISC, "Misc");
        this.add(Config.CREATIVE, "Creative");
        this.add(Config.ENABLE_CREATIVE_GEARINV, "Enable Creative Gear Inventory");
        this.add(Config.DEBUG, "Debug");
        this.add(Config.DEBUG_LOGGING, "Debug Logging");
        this.add(Config.HIDE_ALPHA_WARNING, "Hide Alpha Warning");

        this.add(Config.ENERGY, "Energy");

        this.add(Config.WIRES, "Wires");
        this.add(Config.WIRE_ENERGY_TRANSFER_LIMIT, "Wire Transfer Limit/t");
        this.add(Config.HEAVY_WIRE_ENERGY_TRANSFER_LIMIT, "Heavy Wire Transfer Limit/t");

        this.add(Config.MACHINES, "Machines");
        this.add(Config.ENERGY_STORAGE_SIZE, "Default Machine Energy Storage Size");
        this.add(Config.ENERGY_STORAGE_MODULE_STORAGE_SIZE, "Energy Storage Module Energy Storage Size");
        this.add(Config.COAL_GENERATOR_ENERGY_PRODUCTION_RATE, "Coal Generator Energy Production Rate/t");
        this.add(Config.SOLAR_PANEL_ENERGY_PRODUCTION_RATE, "Solar Panel Energy Production Rate/t");
        this.add(Config.CIRCUIT_FABRICATOR_ENERGY_CONSUMPTION_RATE, "Circuit Fabricator Energy Consumption Rate/t");
        this.add(Config.ELECTRIC_COMPRESSOR_ENERGY_CONSUMPTION_RATE, "Electric Compressor Energy Consumption Rate/t");
        this.add(Config.ELECTRIC_FURNACE_ENERGY_CONSUMPTION_RATE, "Electric Furnace Energy Consumption Rate/t");
        this.add(Config.ELECTRIC_ARC_FURNACE_ENERGY_CONSUMPTION_RATE, "Electric Arc Furnace Energy Consumption Rate/t");
        this.add(Config.ELECTRIC_ARC_FURNACE_BONUS_CHANCE, "Electric Arc Furnace Bonus Chance");
        this.add(Config.OXYGEN_COLLECTOR_ENERGY_CONSUMPTION_RATE, "Oxygen Collector Energy Consumption Rate/t");
        this.add(Config.OXYGEN_COLLECTOR_PRODUCTION_LIMIT, "Oxygen Collector Production Limit/t");
        this.add(Config.OXYGEN_COMPRESSOR_ENERGY_CONSUMPTION_RATE, "Oxygen Compressor Energy Consumption Rate/t");
        this.add(Config.OXYGEN_DECOMPRESSOR_ENERGY_CONSUMPTION_RATE, "Oxygen Decompressor Energy Consumption Rate/t");
        this.add(Config.OXYGEN_SEALER_ENERGY_CONSUMPTION_RATE, "Oxygen Sealer Energy Consumption Rate/t");
        this.add(Config.OXYGEN_SEALER_OXYGEN_CONSUMPTION_RATE, "Oxygen Sealer Oxygen Consumption Rate/t");
        this.add(Config.MAX_SEALING_POWER, "Maximum Sealing Power");
        this.add(Config.REFINERY_ENERGY_CONSUMPTION_RATE, "Refinery Energy Consumption Rate/t");
        this.add(Config.FUEL_LOADER_ENERGY_CONSUMPTION_RATE, "Fuel Loader Energy Consumption Rate/t");
        this.add(Config.FOOD_CANNER_ENERGY_CONSUMPTION_RATE, "Food Canner Energy Consumption Rate/t");

        this.add(Config.LIFE_SUPPORT, "Life Support");
        this.add(Config.SMALL_OXYGEN_TANK_CAPACITY, "Small Oxygen Tank Capacity");
        this.add(Config.MEDIUM_OXYGEN_TANK_CAPACITY, "Medium Oxygen Tank Capacity");
        this.add(Config.LARGE_OXYGEN_TANK_CAPACITY, "Large Oxygen Tank Capacity");
        this.add(Config.PLAYER_OXYGEN_CONSUMPTION_RATE, "Player Oxygen Consumption Rate/t");
        this.add(Config.WOLF_OXYGEN_CONSUMPTION_RATE, "Wolf Oxygen Consumption Rate/t");
        this.add(Config.CAT_OXYGEN_CONSUMPTION_RATE, "Cat Oxygen Consumption Rate/t");
        this.add(Config.PARROT_OXYGEN_CONSUMPTION_RATE, "Parrot Oxygen Consumption Rate/t");
        this.add(Config.CANNOT_EAT_IN_NO_ATMOSPHERE, "Cannot Eat In No Atmosphere");
        this.add(Config.CANNOT_EAT_WITH_MASK, "Cannot Eat With Mask Except From Cans");

        this.add(Config.DIFFICULTY, "Difficulty");
        this.add(Config.METEOR_SPAWN_MULTIPLIER, "Meteor Spawn Multiplier");
        this.add(Config.BOSS_HEALTH_MODIFIER, "Boss Health Modifier");
        this.add(Config.BOSS_HEALTH_MODIFIER_DESC, "Multiplies All GC5 Bosses Health By This Value");
        this.add(Config.ENABLE_SPACE_STATION_CREATION, "Enable Space Station Creation");

        this.add(Config.DISABLED_DIMENSIONS, "Disabled Dimensions");
        this.add(Config.DISABLED_DIMENSIONS_DESC,
                "Dimensions listed here will not be loaded or generated when the world starts. " +
                        "Use IDs such as galacticraft:moon or galacticraft:venus. " +
                        "A server/world restart is required after changing this setting."
        );
        this.add(Config.DISABLED_CELESTIAL_SCREEN_DIMENSIONS, "Disabled Celestial Screen Dimensions");
        this.add(Config.DISABLED_CELESTIAL_SCREEN_DIMENSIONS_DESC,
                "Dimensions listed here will not be accessible via the celestial screen. " +
                        "Use IDs such as galacticraft:moon or galacticraft:venus. " +
                        "A server/world restart is required after changing this setting."
        );

        this.add(Config.SERVER, "Server");
        this.add(Config.SERVER_SIDE_DIMENSION, "Server Side Dimension Config");

        this.add(Keybindings.ROCKET_INVENTORY, "Open Rocket Inventory");
        this.add(Keybindings.OPEN_CELESTIAL_SCREEN, "Open Celestial Map");
    }

    protected void generateSpaceRaceTranslations() {
        this.add(SpaceRace.SPACE_RACE_MANAGER, "Space Race Manager");
        this.add(SpaceRace.ADD_PLAYERS, "Add Player(s)");
        this.add(SpaceRace.BACK, "Back");
        this.add(SpaceRace.BUTTON, "Space Race");
        this.add(SpaceRace.BUTTON_2, "Manager");
        this.add(SpaceRace.COMING_SOON, "Coming Soon™");
        this.add(SpaceRace.EXIT, "Exit");
        this.add(SpaceRace.GLOBAL_STATS, "Global Statistics");
        this.add(SpaceRace.REMOVE_PLAYERS, "Remove Player(s)");
        this.add(SpaceRace.SERVER_STATS, "Server Statistics");
        this.add(SpaceRace.FLAG_CONFIRM, "Use this team flag?");
        this.add(SpaceRace.FLAG_CONFIRM_MESSAGE, "");
        this.add(SpaceRace.CUSTOMIZE_FLAG, "Customize Flag");
        this.add(SpaceRace.DRAG_AND_DROP_FLAG, "Drag and drop an image to use as a flag");
        this.add(SpaceRace.TEAM_COLOR_1, "Change");
        this.add(SpaceRace.TEAM_COLOR_2, "Team");
        this.add(SpaceRace.TEAM_COLOR_3, "Color");
        this.add(SpaceRace.RED, "Red");
        this.add(SpaceRace.GREEN, "Green");
        this.add(SpaceRace.BLUE, "Blue");
    }

    protected void generateSolarPanelTranslations() {
        this.add(SolarPanel.ATMOSPHERIC_INTERFERENCE, "Atmospheric Interference: %s");
        this.add(SolarPanel.BLOCKED, "Blocked");
        this.add(SolarPanel.DAY, "Day");
        this.add(SolarPanel.MISSING_SOURCE, "Missing Light Source");
        this.add(SolarPanel.NIGHT, "Night");
        this.add(SolarPanel.OVERCAST, "Overcast");
        this.add(SolarPanel.LIGHT_SOURCE, "Light Source: ");
        this.add(SolarPanel.LIGHT_SOURCE_MOON, "Moon");
        this.add(SolarPanel.LIGHT_SOURCE_EARTH, "Earth");
        this.add(SolarPanel.LIGHT_SOURCE_NONE, "Unknown");
        this.add(SolarPanel.LIGHT_SOURCE_RAIN, "Sun (Overcast)");
        this.add(SolarPanel.LIGHT_SOURCE_SUN, "Sun");
        this.add(SolarPanel.LIGHT_SOURCE_THUNDER, "Sun (Stormy)");
        this.add(SolarPanel.STATUS, "Status: ");
        this.add(SolarPanel.STORMY, "Stormy");
        this.add(SolarPanel.STRENGTH, "Strength: %s");
    }

    protected void generateMachineStatusTranslations() {
        this.add(MachineStatus.GENERATING, "Generating");
        this.add(MachineStatus.NO_FUEL, "No Fuel");
        this.add(MachineStatus.WARMING_UP, "Warming Up");
        this.add(MachineStatus.COOLING_DOWN, "Cooling Down");
        this.add(MachineStatus.PARTIALLY_GENERATING, "Partially Generating");
        this.add(MachineStatus.NOT_GENERATING, "Not Generating");
        this.add(MachineStatus.BLOCKED, "Blocked");
        this.add(MachineStatus.FABRICATING, "Fabricating");
        this.add(MachineStatus.COMPRESSING, "Compressing");
        this.add(MachineStatus.SMELTING, "Smelting");
        this.add(MachineStatus.NOT_ENOUGH_OXYGEN, "Not Enough Oxygen");
        this.add(MachineStatus.COLLECTING, "Collecting");
        this.add(MachineStatus.COMPRESSING_OXYGEN, "Compressing");
        this.add(MachineStatus.DECOMPRESSING, "Decompressing");
        this.add(MachineStatus.OXYGEN_DETECTED, "Oxygen Detected");
        this.add(MachineStatus.NO_OXYGEN_DETECTED, "No Oxygen Detected");
        this.add(MachineStatus.MISSING_OXYGEN_TANK, "Missing Oxygen Tank");
        this.add(MachineStatus.OXYGEN_TANK_FULL, "Oxygen Tank Full");
        this.add(MachineStatus.EMPTY_OXYGEN_TANK, "Empty Oxygen Tank");
        this.add(MachineStatus.ALREADY_SEALED, "Already Sealed");
        this.add(MachineStatus.AREA_TOO_LARGE, "Area Too Large");
        this.add(MachineStatus.SEALED, "Sealed");
        this.add(MachineStatus.DISTRIBUTING, "Distributing");
        this.add(MachineStatus.REFINING, "Refining");
        this.add(MachineStatus.MISSING_OIL, "Missing Oil");
        this.add(MachineStatus.FUEL_TANK_FULL, "Fuel Tank Full");
        this.add(MachineStatus.PREPARING, "Preparing");
        this.add(MachineStatus.LOADING, "Loading");
        this.add(MachineStatus.NOT_ENOUGH_FUEL, "Not Enough Fuel");
        this.add(MachineStatus.NO_ROCKET, "No Rocket");
        this.add(MachineStatus.ROCKET_IS_FULL, "Rocket Is Full");
        this.add(MachineStatus.CANNING, "Canning");
        this.add(MachineStatus.TRANSFERRING_CAN, "Transferring Can");
        this.add(MachineStatus.NO_FOOD, "No Food");
        this.add(MachineStatus.MISSING_EMPTY_CAN, "Missing Empty Can");
        this.add(MachineStatus.DESORBING, "Desorbing Volatiles");
        this.add(MachineStatus.SEPARATING, "Separating Isotopes");
        this.add(MachineStatus.FUSING, "MHD Fusion Active");
        this.add(MachineStatus.FUSING_CRYO, "MHD Fusion (<2K Cryo)");
        this.add(MachineStatus.MISSING_COIL, "Missing Superconducting Coil");
        this.add(MachineStatus.MISSING_FUSION_FUEL, "Missing D / 3He Gas");
        this.add(MachineStatus.GAS_TANK_FULL, "Gas Tank Full");
        this.add(MachineStatus.MAGNETIC_QUENCH_SAFE, "Quench-Safe Shutdown");
        this.add(MachineStatus.MULTIBLOCK_INCOMPLETE, "Torus Structure Incomplete");
        this.add(MachineStatus.SPINNING_UP, "Spinning Up Rotor");
        this.add(MachineStatus.IGNITING_PLASMA, "Igniting MHD Plasma");
        this.add(MachineStatus.EVACUATING_CHAMBER, "Evacuating Vacuum Chamber");
        this.add(MachineStatus.INFUSING, "Metallurgic Infusing");
        this.add(MachineStatus.MISSING_INFUSION_AGENT, "Missing Infusion Agent");
        this.add(MachineStatus.LEACHING, "Acid Leaching Ore");
        this.add(MachineStatus.MISSING_LEACHING_FLUID, "Missing Leaching Fluid");
        this.add(MachineStatus.PRESSURIZING, "Pressurizing Autoclave");
        this.add(MachineStatus.CURING, "Curing Aerospace Composite");
        this.add(MachineStatus.ELECTROLYZING, "Electrolyzing Propellants");
        this.add(MachineStatus.MISSING_FEEDSTOCK, "Missing Water / Ice");
        this.add(MachineStatus.FRACTIONATING, "Fractionating Atmosphere");
        this.add(MachineStatus.NO_ATMOSPHERE, "No Atmosphere (Vacuum)");
        this.add(MachineStatus.HEATING_CATALYST, "Heating Catalyst Bed");
        this.add(MachineStatus.SYNTHESIZING, "Synthesizing Propellant");
        this.add(MachineStatus.MISSING_CATALYST, "Missing Nb / Pd Catalyst");
        this.add(MachineStatus.REFURBISHING, "Refurbishing Rocket Part");
        this.add(MachineStatus.APPLYING_COATING, "Applying Protective Coating");
        this.add(MachineStatus.PART_ALREADY_NOMINAL, "Part Already 100% Nominal");
        this.add(MachineStatus.RESEARCHING, "Prototyping & Researching");
        this.add(MachineStatus.MISSING_SPECIMEN, "Insert Research Specimen");
        this.add(MachineStatus.OBSERVING_SKY, "Tracking Orbital Ephemeris");
        this.add(MachineStatus.SKY_OBSTRUCTED, "Sky View Obstructed");
        this.add(MachineStatus.WAITING_FOR_NIGHT, "Waiting for Night Sky");
        this.add(MachineStatus.ATMOSPHERE_TOO_DENSE, "Atmosphere Too Dense");
        this.add(MachineStatus.ANALYZING_SPECIMEN, "Running Mass Spectroscopy");
        this.add(MachineStatus.RUNNING_ENV_TEST, "Running Environmental Stress Test");
        this.add(MachineStatus.MICROGRAVITY_SYNTHESIS, "Microgravity Synthesis (0.0G)");
        this.add(MachineStatus.GRAVITY_TOO_HIGH, "Gravity Too High (Requires 0.0G)");
        this.add(MachineStatus.UPLINK_ACTIVE, "Interplanetary Uplink Active");
        this.add(MachineStatus.MISSING_FREQUENCY_MODULE, "Missing Frequency Module");
    }

    protected void generateCelestialSelectionTranslations() {
        this.add(CelestialSelection.BACK, "Back");
        this.add(CelestialSelection.CANCEL, "Cancel");
        this.add(CelestialSelection.CATALOG, "Catalog");
        this.add(CelestialSelection.LAUNCH, "Launch");
        this.add(CelestialSelection.RENAME, "Rename");
        this.add(CelestialSelection.TIER, "Tier %s");
        this.add(CelestialSelection.ASSIGN_NAME, "Assign Name");
        this.add(CelestialSelection.APPLY, "Apply");
        this.add(CelestialSelection.EXIT, "Exit");
        this.add(CelestialSelection.SELECT_SS, "Select Space Station");
        this.add(CelestialSelection.SS_OWNER, "Space Station Owner");
        this.add(CelestialSelection.CAN_CREATE_SPACE_STATION, "A Space Station can be created here!");
        this.add(CelestialSelection.CANNOT_CREATE_SPACE_STATION, "Cannot Create Space Station");
        this.add(CelestialSelection.CREATE_SPACE_STATION, "Create");
        this.add(CelestialSelection.DAY_NIGHT_CYCLE, "Day/Night Cycle");
        this.add(CelestialSelection.SURFACE_GRAVITY, "Surface Gravity");
        this.add(CelestialSelection.SURFACE_COMPOSITION, "Surface Composition");
        this.add(CelestialSelection.ATMOSPHERE, "Atmosphere");
        this.add(CelestialSelection.MEAN_SURFACE_TEMP, "Mean Surface Temp.");
        this.add(CelestialSelection.CLICK_AGAIN, "Click again to zoom");
        this.add(CelestialSelection.CLICK_AGAIN_MOONS, "Click again to zoom (view moons)");
        this.add(CelestialSelection.CLICK_AGAIN_SATELLITES, "Click again to zoom (view satellites)");
        this.add(CelestialSelection.CLICK_AGAIN_MOONS_AND_SATELLITES, "Click again to zoom (view moons & satellites)");
        this.add(CelestialSelection.DISABLED, "Disabled");
    }

    protected void generateUiTranslations() {
        this.add(Ui.CONE, "Cone");
        this.add(Ui.BODY, "Body");
        this.add(Ui.FINS, "Fins");
        this.add(Ui.BOOSTER, "Booster");
        this.add(Ui.ENGINE, "Engine");
        this.add(Ui.UPGRADE, "Upgrade");
        this.add(Ui.COLOR, "Color");

        this.add(Ui.AIRLOCK_REDSTONE_SIGNAL, "Opens on Redstone Signal");
        this.add(Ui.AIRLOCK_OWNER, "%s's Airlock Controller");
        this.add(Ui.ALPHA_WARNING_1, "Galacticraft is currently in ALPHA.");
        this.add(Ui.ALPHA_WARNING_2, "Please report all issues you find.");
        this.add(Ui.ALPHA_WARNING_3, "Press [ESC] or click to continue.");
        this.add(Ui.ALPHA_WARNING_HEADER, "WARNING");
        this.add(Ui.BUBBLE_CURRENT_SIZE, "Current Size: %s");
        this.add(Ui.BUBBLE_NOT_VISIBLE, "Bubble Not Visible");
        this.add(Ui.BUBBLE_TARGET_SIZE, "Target Size: ");
        this.add(Ui.BUBBLE_VISIBLE, "Bubble Visible");

        this.add(Ui.COLLECTING, "Collecting: %s/t");
        this.add(Ui.CURRENT_OXYGEN, "Oxygen: %s");
        this.add(Ui.GJT, "%s gJ/t");
        this.add(Ui.MILLIBUCKETS, "mB");
        this.add(Ui.MAX_OXYGEN, "Maximum Oxygen: %s");
        this.add(Ui.DETECTOR_MODE, "Current Mode: %s");
        this.add(Ui.DETECTOR_AND, "AND");
        this.add(Ui.DETECTOR_OR, "OR");
        this.add(Ui.MACHINE_STATUS, "Status: %s");
        this.add(Ui.OXYGEN_TANK_1_LEVEL, "Oxygen Tank 1: %s");
        this.add(Ui.OXYGEN_TANK_2_LEVEL, "Oxygen Tank 2: %s");
        this.add(Ui.OXYGEN_WARNING, "WARNING");
        this.add(Ui.OXYGEN_SETUP_INVALID, "Oxygen Setup is Invalid!");
        this.add(Ui.ROCKET_FUEL, "Fuel:");
        this.add(Ui.ROCKET_FULL, "% full");
        this.add(Ui.ROCKET_NO_FUEL, "No fuel");
        this.add(Ui.LANDER_WARNING, "WARNING");
        this.add(Ui.LANDER_CONTROLS, "Hold \"%s\" to slow down!");
        this.add(Ui.LANDER_VELOCITY, "Entry Velocity: %s m/s");
        this.add(Ui.SMALL_STEP, "Taking one small step");
        this.add(Ui.GIANT_LEAP, "Taking one giant leap");
        this.add(Ui.PREPARE_FOR_ENTRY, "Prepare for entry!");
        this.add(Ui.TRAVELING_TO, "Traveling to: %s");

        this.add(Ui.TOTAL_NUTRITION, "Total Nutrition: %s");
        this.add(Ui.SPACE_STATION_NAME, "%s's Space Station");

        this.add(Ui.CAPE_BUTTON, "Cape");
        this.add(Ui.CAPES_TITLE, "Capes");
        this.add(Ui.CAPES_STATE + "vanilla", "Vanilla");
        this.add(Ui.CAPES_STATE + "gc", "Galacticraft");
        this.add(Ui.CAPES_STATE + "off", "Off");

        this.cape(Cape.EARTH, "Earth");
        this.cape(Cape.JUPITER, "Jupiter");
        this.cape(Cape.MARS, "Mars");
        this.cape(Cape.MERCURY, "Mercury");
        this.cape(Cape.MOON, "Moon");
        this.cape(Cape.NEPTUNE, "Neptune");
        this.cape(Cape.PLAIN, "Plain");
        this.cape(Cape.SPACE_STATION, "Space Station");
        this.cape(Cape.SUN, "Sun");
        this.cape(Cape.URANUS, "Uranus");
        this.cape(Cape.VENUS, "Venus");
        this.cape(Cape.DEVELOPER, "Developer");
        this.cape(Cape.REWOVEN, "Rewoven");
        this.cape(Cape.DEVELOPER_RED, "Developer Red");
    }

    protected void generateWailaTranslations() {
        this.add(Waila.PLUGIN_GALACTICRAFT, "Galacticraft");
        this.add(Waila.SHOW_OXYGEN_LEVEL, "Show Oxygen Tank Levels");
        this.add(Waila.OXYGEN_TANK_LABEL, "O₂ Tank %s");
    }

    protected void generateStatsTranslations() {
        this.stat(GCStats.CLEAN_PARACHUTE, "Parachutes Cleaned");
        this.stat(GCStats.OPEN_PARACHEST, "Parachests Opened");
        this.stat(GCStats.INTERACT_WITH_ROCKET_WORKBENCH, "Interactions with Rocket Workbench");
        this.stat(GCStats.LAUNCH_ROCKET, "Rockets Launched");
        this.stat(GCStats.CRASH_LANDING, "Crash Landings");
        this.stat(GCStats.SAFE_LANDING, "Safe Landings");
        this.stat(GCStats.EAT_CHEESE_WHEEL_SLICE, "Cheese Wheel Slices Eaten");
        this.stat(GCStats.CHEESE_SLICED, "Cheese Wheels Sliced");
    }

    protected void generateAdvancementsTranslations() {
        this.advancement(Advancements.ROOT, "Galacticraft", "Explore the Moon... and beyond!");
        this.advancement(Advancements.COAL_GENERATOR, "Coal Power", "Craft a Coal Generator");
        this.advancement(Advancements.CIRCUIT_FABRICATOR, "Fabricated", "Craft a Circuit Fabricator to make Wafers");
        this.advancement(Advancements.BASIC_WAFER, "Tasty Wafers", "Craft a Basic Wafer (it's Silicon so please don't eat it!)");
        this.advancement(Advancements.ADVANCED_WAFER, "Golden Wafers", "Craft an Advanced Wafer (you still can't eat it!)");
        this.advancement(Advancements.BASIC_SOLAR_PANEL, "Free Energy", "Craft a Basic Solar Panel");
        this.advancement(Advancements.ADVANCED_SOLAR_PANEL, "Fast + Efficient", "Craft an Advanced Solar Panel");
        this.advancement(Advancements.COMPRESSOR, "Compressed", "Craft a Compressor");
        this.advancement(Advancements.ELECTRIC_COMPRESSOR, "Electrical Upgrade", "Craft an Electric Compressor");
        this.advancement(Advancements.OIL, "Sticky Stuff", "Fall into some Oil");
        this.advancement(Advancements.REFINERY, "Time to Refine", "Craft a Refinery");
        this.advancement(Advancements.FUEL, "Rocket Fuel", "Refine Oil in the Refinery");
        this.advancement(Advancements.OXYGEN_COLLECTOR, "Oxygen Not Included", "Craft an Oxygen Collector");
        this.advancement(Advancements.OXYGEN_COMPRESSOR, "Compress the Air", "Craft an Oxygen Compressor");
        this.advancement(Advancements.FILL_TANK, "Air 2 Go", "Fill up an Oxygen Tank");
        this.advancement(Advancements.FILL_ALL_TANKS, "Oxygen Stockpile", "Fill up every kind of Oxygen Tank");
        this.advancement(Advancements.OXYGEN_GEAR, "All Set", "Create everything you need to breathe in space");
        this.advancement(Advancements.ROCKET_WORKBENCH, "Space Engineer", "Craft a Rocket Workbench");
        this.advancement(Advancements.ROCKET, "We Have a Rocket!", "Weld a Rocket together");
        this.advancement(Advancements.FUEL_LOADER, "Fill Her Up!", "Craft a Fuel Loader");
        this.advancement(Advancements.LEAVE_ROCKET_DURING_COUNTDOWN, "We've Forgotten the Crackers!", "Leave a Rocket during the countdown");
        this.advancement(Advancements.LAUNCH_ROCKET, "We Have Liftoff", "Launch your first Rocket");
        this.advancement(Advancements.MOON, "The Moon!", "Land on the Moon");
        this.advancement(Advancements.PARROT_LANDING, "The Parrot has Landed", "Land on the Moon with a Parrot on your shoulder");
        this.advancement(Advancements.EAT_MOON_CHEESE_CURD, "Wensleydale? Stilton?", "Eat Moon Cheese Curd");
        this.advancement(Advancements.CHEESE_AND_CRACKERS, "It's Like No Cheese I've Ever Tasted...", "Eat cheese and crackers");
        this.advancement(Advancements.CHEESE_TAX, "Cheese Tax", "Give Cheese to a pet Wolf");
        this.advancement(Advancements.THROW_METEOR_CHUNK, "Nice to Meteor You", "Hit a mob with a Throwable Meteor Chunk");
        this.advancement(Advancements.SPACE_STATION, "Space Station", "Create your own Space Station!");
        this.advancement(Advancements.MOON_DUNGEON, "Worse than a Wither", "Find an Evolved Skeleton Boss in a Moon Dungeon");
        this.advancement(Advancements.MOON_DUNGEON_KEY, "I Has the Key", "Kill the boss in the Moon Dungeon, and steal its key");
        this.advancement(Advancements.BUGGY_SCHEMATIC, "Glitchy Schematic", "Obtain a Moon Buggy Schematic");
        this.advancement(Advancements.BUGGY, "Buggy", "Weld a Moon Buggy together");
    }

    protected void generateManualTranslations() {
        this.item(GCItems.AEROSPACE_MANUAL, "Aerospace Engineering Manual");
        this.add("item.galacticraft.aerospace_manual.desc1", "Interactive progressive flight compendium & mission planner.");
        this.add("item.galacticraft.aerospace_manual.desc2", "Right-click or press [G] to open. Unlocks entries as you research.");
        this.add(Keybindings.OPEN_AEROSPACE_MANUAL, "Open Aerospace Manual");

        this.add("manual.galacticraft.title", "Aerospace Engineering Manual");
        this.add("manual.galacticraft.header", "AEROSPACE MANUAL  •  ERA %s");
        this.add("manual.galacticraft.search", "Search");
        this.add("manual.galacticraft.search_hint", "Search...");

        this.add("manual.galacticraft.tab.mission", "Mission");
        this.add("manual.galacticraft.tab.materials", "Materials");
        this.add("manual.galacticraft.tab.machines", "Machines");
        this.add("manual.galacticraft.tab.atlas", "Atlas");
        this.add("manual.galacticraft.tab.lab_log", "Labs");
        this.add("manual.galacticraft.tab.index", "Index");

        this.add("manual.galacticraft.toast.unlocked_header", "New Compendium Entry");
        this.add("manual.galacticraft.toast.open_hint", "Press [%s] to inspect");

        this.add("manual.galacticraft.advisor.headline", "Mission Control — What's My Next Step? (Era %s)");
        this.add("manual.galacticraft.advisor.complete", "All 55 Aerospace R&D technologies mastered! You command the Solar System.");

        this.add("manual.galacticraft.locked.banner", "LOCKED HYPOTHESIS — RESEARCH REQUIRED");
        this.add("manual.galacticraft.hint.default", "Build a Research Workbench and Aerospace R&D Terminal to begin unlocking Era 1 technologies.");
        this.add("manual.galacticraft.hint.era1_rocketry", "Research Orbital RP-1 Launch Architecture at your Research Workbench after mapping the Moon.");
        this.add("manual.galacticraft.hint.ferroniobium", "Prototype Raw Niobium and Aluminum at the Research Workbench to unlock Ferroniobium Aluminothermy.");
        this.add("manual.galacticraft.hint.vacuum_sealing", "Run a pump-down vacuum trial inside an Environmental Test Chamber to certify airlocks and seals.");
        this.add("manual.galacticraft.hint.early_solar", "Complete Base Industrial Prototyping and research Silicon Photovoltaics at the Research Workbench.");
        this.add("manual.galacticraft.hint.lunar_deep_sky", "Place an Observatory Telescope on the Moon (natural vacuum) to map Mars, Venus, Mercury, and Asteroids.");
        this.add("manual.galacticraft.hint.lunar_vacuum", "Operate an Environmental Test Chamber in natural vacuum (Moon) to unlock Vacuum Arc Metallurgy & C-103.");
        this.add("manual.galacticraft.hint.lunar_electrolysis", "Analyze lunar polar Dense Ice in a Geological Analyzer to unlock Polar Ice Electrolysis & Cryogenics.");
        this.add("manual.galacticraft.hint.martian_sabatier", "Build a Geological Analyzer on Mars (95% CO2) or launch an Atmospheric Spectrometer probe to unlock Sabatier ISRU.");
        this.add("manual.galacticraft.hint.venus_leaching", "Analyze Venusian sulfuric acid chemistry in a hyperbaric lab (>=50 atm) to unlock Chemical Acid Leaching.");
        this.add("manual.galacticraft.hint.venus_hyperbaric", "Run a live 92 atm hyperbaric trial in an Environmental Test Chamber on Venus to certify lead & T2 suits.");
        this.add("manual.galacticraft.hint.helium3_fusion", "Research Aneutronic D-3He MHD Fusion in a 0.0G Research Chamber after mastering Mercurian Isotopes.");
        this.add("manual.galacticraft.hint.outer_ephemeris", "Build an Observatory Telescope on Mars, Orbit, or Asteroids to chart Jupiter, Saturn, Uranus, and Neptune.");
        this.add("manual.galacticraft.hint.outer_exotic", "Analyze Jovian Palladium and outer-system superionic crystals in situ to unlock Era 5-6 exotic alloys.");
        this.add("manual.galacticraft.hint.moon_ephemeris", "Use an Observatory Telescope on Earth at night to lock Earth-Moon Orbital Ephemeris.");

        this.add("manual.galacticraft.multiblock.drag_hint", "Drag to rotate 3D • Use [-][+][ALL] for layers");
        this.add("manual.galacticraft.recipe.unavailable", "Recipe unavailable in current datapack: %s");
        this.add("manual.galacticraft.sim.title", "Planetary Flight Viability Calculator (Live Physics)");
        this.add("manual.galacticraft.atlas.surveyed", "SURVEYED");
        this.add("manual.galacticraft.atlas.unmapped", "UNMAPPED (Use Telescope/Probe)");
        this.add("manual.galacticraft.lablog.header", "In-Situ Laboratory Research Checklist by Location");
        this.add("manual.galacticraft.index.header", "Global Compendium Index (%s entries)");
        this.add("manual.galacticraft.index.no_results", "No matching entries found.");

        // Entries & Pages
        this.add("manual.galacticraft.entry.getting_started.title", "Agency Briefing & Era 1");
        this.add("manual.galacticraft.entry.getting_started.h1", "Welcome to Galacticraft 5 Aerospace R&D");
        this.add("manual.galacticraft.entry.getting_started.body1", "Your space program advances through 6 Eras across real planetary environments. Start on Earth with a Research Workbench, Coal Generator, Compressor, Circuit Fabricator, and Aerospace R&D Terminal. Every planet unlocks unique metallurgy, chemistry, and rocket materials.");
        this.add("manual.galacticraft.entry.getting_started.tip1", "Tip: Hover any Galacticraft item in your inventory and press [G] to jump straight to its manual entry!");
        this.add("manual.galacticraft.entry.getting_started.craft_manual", "Crafting the Aerospace Manual");
        this.add("manual.galacticraft.entry.getting_started.craft_manual_cap", "Shapeless: Book + Raw Niobium (or Aluminum Ingot).");

        this.add("manual.galacticraft.entry.lunar_checklist.title", "Lunar Pre-Launch Checklist");
        this.add("manual.galacticraft.entry.lunar_checklist.h1", "Before Your First Moon Liftoff");
        this.add("manual.galacticraft.entry.lunar_checklist.body1", "The Moon has 0.0 atm vacuum and 0.166G gravity. Without pressurized oxygen gear you will suffocate immediately, and without a Parachute you cannot survive Earth return re-entry.");
        this.add("manual.galacticraft.entry.lunar_checklist.warn1", "Always pack return fuel and a Launch Pad + Fuel Loader when departing for a new world!");
        this.add("manual.galacticraft.entry.lunar_checklist.check_title", "Moon Mission Live Inventory Check");

        this.add("manual.galacticraft.entry.mars_checklist.title", "Mars Expedition Checklist");
        this.add("manual.galacticraft.entry.mars_checklist.check_title", "Mars Mission Live Inventory Check");
        this.add("manual.galacticraft.entry.venus_checklist.title", "Venus 92 atm Checklist");
        this.add("manual.galacticraft.entry.venus_checklist.check_title", "Venus Hyperbaric Acid Check");

        this.add("manual.galacticraft.check.oxygen_mask", "Oxygen Mask (GC Accessory)");
        this.add("manual.galacticraft.check.oxygen_gear", "Oxygen Gear (GC Accessory)");
        this.add("manual.galacticraft.check.oxygen_tank", "Pressurized Oxygen Tank");
        this.add("manual.galacticraft.check.parachute", "Atmospheric Parachute");
        this.add("manual.galacticraft.check.return_fuel", "Return Propellant (Fuel / Canister)");
        this.add("manual.galacticraft.check.thermal_suit", "Thermal Padding T1+ Suit");
        this.add("manual.galacticraft.check.isothermal_t2", "Isothermal Padding T2 Suit");
        this.add("manual.galacticraft.check.lead_shielding", "Lead / Lonsdaleite Anti-Acid Hull");
        this.add("manual.galacticraft.check.data_drive", "Blank Data Drive (Flight Telemetry)");

        this.add("manual.galacticraft.entry.flight_simulator.title", "Flight Viability Simulator");
        this.add("manual.galacticraft.entry.flight_simulator.h1", "Emergent Physics — No Artificial Tiers");
        this.add("manual.galacticraft.entry.flight_simulator.body1", "Rockets are validated by true physics: Thrust-to-Weight (T/W > 1.0), Tsiolkovsky Delta-V, thermal survival (K), hyperbaric pressure (atm), sulfuric acid immunity, 1/r^2 solar power dropoff, and cryogenic boil-off.");
        this.add("manual.galacticraft.entry.flight_simulator.info1", "Click any parameter below (Left-click next / Right-click previous) to simulate a flight before building!");
        this.add("manual.galacticraft.entry.flight_simulator.calc_title", "Interactive Simulator");

        this.add("manual.galacticraft.entry.early_alloys.title", "Era 1-2 Structural Alloys");
        this.add("manual.galacticraft.entry.early_alloys.list_title", "Aluminum, Bronze, Steel, Ferroniobium & Meteoric Iron");
        this.add("manual.galacticraft.entry.advanced_composites.title", "Era 3-4 Alloys & Composites");
        this.add("manual.galacticraft.entry.advanced_composites.list_title", "Desh, Titanium, Lead, Carbon Fiber & Niobium C-103");
        this.add("manual.galacticraft.entry.exotic_outer.title", "Era 5-6 Exotic Materials");
        this.add("manual.galacticraft.entry.exotic_outer.list_title", "Palladium-Niobium, Mg-Li, Lonsdaleite & Superionic Ice");

        this.add("manual.galacticraft.entry.launch_pad_3d.title", "3D: Launch Pad & Fueling");
        this.add("manual.galacticraft.entry.launch_pad_3d.mb_title", "3x3 Rocket Launch Pad + Fuel Loader");
        this.add("manual.galacticraft.entry.launch_pad_3d.caption", "Place a flat 3x3 pad; attach a Fuel Loader adjacent to the rim.");
        this.add("manual.galacticraft.entry.launch_pad_3d.recipe", "Rocket Launch Pad Recipe");
        this.add("manual.galacticraft.entry.launch_pad_3d.recipe_cap", "Crafts 9 pads — exact count for a 3x3 platform.");

        this.add("manual.galacticraft.entry.airlock_3d.title", "3D: Pressurized Airlock");
        this.add("manual.galacticraft.entry.airlock_3d.mb_title", "Sealed Airlock Frame & Controller");
        this.add("manual.galacticraft.entry.airlock_3d.caption", "Closed rectangular ring of Airlock Frames with 1 Airlock Controller.");
        this.add("manual.galacticraft.entry.airlock_3d.h1", "Hermetic Room Sealing");
        this.add("manual.galacticraft.entry.airlock_3d.body1", "Pair Airlocks with Vacuum Glass and an Oxygen Sealer to maintain breathable pressure in vacuum or toxic atmospheres.");
        this.add("manual.galacticraft.entry.airlock_3d.tip1", "Right-click the Airlock Controller to configure automatic player proximity opening.");

        this.add("manual.galacticraft.entry.solar_panel_3d.title", "3D: Solar Power Array");
        this.add("manual.galacticraft.entry.solar_panel_3d.mb_title", "Solar Panel + Aluminum Wire + Storage");
        this.add("manual.galacticraft.entry.solar_panel_3d.caption", "Keep sky clear above the collector; output scales by 1/r^2 with heliocentric distance.");
        this.add("manual.galacticraft.entry.solar_panel_3d.h1", "Inverse-Square Solar Dropoff (1/r^2)");
        this.add("manual.galacticraft.entry.solar_panel_3d.body1", "Solar irradiance is 6.67x on Mercury and 1.0x on Earth/Moon, but drops to 0.43x on Mars and 0.037x on Jupiter.");
        this.add("manual.galacticraft.entry.solar_panel_3d.warn1", "Beyond Mars, transition to Radioisotope RTGs, Atomic Batteries, or Helium-3 MHD Fusion!");

        this.add("manual.galacticraft.entry.cryo_3d.title", "3D: Cryogenic Chamber");
        this.add("manual.galacticraft.entry.cryo_3d.mb_title", "Cryogenic Chamber + Oxygen Modules");
        this.add("manual.galacticraft.entry.cryo_3d.caption", "3-block tall stasis chamber allowing off-world sleep and fast-forwarding planetary nights.");

        this.add("manual.galacticraft.entry.circuit_fabricator.title", "Circuit Fabricator");
        this.add("manual.galacticraft.entry.circuit_fabricator.h1", "Silicon Wafer Photolithography");
        this.add("manual.galacticraft.entry.circuit_fabricator.body1", "The Circuit Fabricator transforms Diamond, Raw Silicon, Redstone, and specialty catalysts into Basic, Advanced, and Solar Array Wafers required across all machines and avionics.");
        this.add("manual.galacticraft.entry.circuit_fabricator.tip1", "Connect a Coal Generator or Basic Solar Panel via Aluminum Wire to keep fabrication continuous.");
        this.add("manual.galacticraft.entry.circuit_fabricator.recipe", "Basic Wafer Fabrication");
        this.add("manual.galacticraft.entry.circuit_fabricator.recipe_cap", "Used in Solar Panels, Oxygen Fans, Avionics, and Frequency Modules.");

        this.add("manual.galacticraft.entry.niobium_chain.title", "Flow: Niobium Metallurgy");
        this.add("manual.galacticraft.entry.niobium_chain.flow_title", "Pyrochlore Niobium -> Ferroniobium -> C-103 -> Nb3Sn");
        this.add("manual.galacticraft.entry.niobium_chain.caption", "Earth pyrochlore feeds Era 1 HSLA Ferroniobium and scales up to Era 5 Nb3Sn superconducting coils.");
        this.add("manual.galacticraft.entry.niobium_chain.infuser_title", "Vacuum Metallurgic Infusion");
        this.add("manual.galacticraft.entry.niobium_chain.infuser_note", "Aluminothermic reduction yields high-strength aerospace Ferroniobium.");
        this.add("manual.galacticraft.flow.nb.s1", "1. Mine Raw Niobium (Pyrochlore)");
        this.add("manual.galacticraft.flow.nb.d1", "Found deep in Overworld (Y -56..16) and outer planets.");
        this.add("manual.galacticraft.flow.nb.s2", "2. Roast to Niobium Oxide (Nb2O5)");
        this.add("manual.galacticraft.flow.nb.d2", "Smelt or leach with H2SO4 on Venus for 3x oxide yield.");
        this.add("manual.galacticraft.flow.nb.s3", "3. Aluminothermy -> Ferroniobium");
        this.add("manual.galacticraft.flow.nb.d3", "Combine with Aluminum for Era 1 HSLA aerospace hulls.");
        this.add("manual.galacticraft.flow.nb.s4", "4. Vacuum Arc -> Pure Niobium & C-103");
        this.add("manual.galacticraft.flow.nb.d4", "Electric Arc Furnace unlocks +1400K refractory nozzles.");
        this.add("manual.galacticraft.flow.nb.s5", "5. 0G Coiling -> Nb3Sn Superconductor");
        this.add("manual.galacticraft.flow.nb.d5", "Powers Telepads, Fusion Confinement, and Magnetoplasma.");

        this.add("manual.galacticraft.entry.sabatier_chain.title", "Flow: Martian Sabatier ISRU");
        this.add("manual.galacticraft.entry.sabatier_chain.flow_title", "CO2 + 4H2 -> CH4 (Liquid Methane) + 2H2O");
        this.add("manual.galacticraft.entry.sabatier_chain.caption", "Refuel Methalox Raptor/BE-4 rockets directly from Martian air and subsurface ice.");
        this.add("manual.galacticraft.entry.sabatier_chain.reactor_title", "Sabatier Chemical Reactor");
        this.add("manual.galacticraft.entry.sabatier_chain.reactor_note", "Requires Martian CO2 intake + Liquid Hydrogen from Electrolytic Separator.");
        this.add("manual.galacticraft.flow.sab.s1", "1. Capture 95.3% CO2 on Mars");
        this.add("manual.galacticraft.flow.sab.d1", "Atmospheric Fractionator condenses thin Martian air.");
        this.add("manual.galacticraft.flow.sab.s2", "2. Electrolyze Ice into LOX + LH2");
        this.add("manual.galacticraft.flow.sab.d2", "Electrolytic Separator splits water/ice into oxidizer & H2.");
        this.add("manual.galacticraft.flow.sab.s3", "3. Catalyze Sabatier Reaction");
        this.add("manual.galacticraft.flow.sab.d3", "Exothermic synthesis outputs Methane + recycled water.");
        this.add("manual.galacticraft.flow.sab.s4", "4. Load Methalox Return Rocket");
        this.add("manual.galacticraft.flow.sab.d4", "High Isp propellant for Asteroid Belt & beyond.");

        this.add("manual.galacticraft.entry.acid_leaching.title", "Flow: Venus Acid Leaching");
        this.add("manual.galacticraft.entry.acid_leaching.flow_title", "H2SO4 Ore Tripling Chemistry");
        this.add("manual.galacticraft.entry.acid_leaching.caption", "Harvest Venusian sulfuric acid to multiply Niobium and Titanium output.");
        this.add("manual.galacticraft.flow.acid.s1", "1. Pump Venusian Sulfuric Acid");
        this.add("manual.galacticraft.flow.acid.d1", "Requires Lead-lined tanks and T2 Isothermal suit.");
        this.add("manual.galacticraft.flow.acid.s2", "2. Chemical Leaching Plant");
        this.add("manual.galacticraft.flow.acid.d2", "Dissolves refractory gangue rock under heat.");
        this.add("manual.galacticraft.flow.acid.s3", "3. 3x Oxide & Purified Metals");
        this.add("manual.galacticraft.flow.acid.d3", "Feeds large-scale 5x5-11x11 fleet construction.");

        this.add("manual.galacticraft.entry.helium3_fusion.title", "Flow: Helium-3 MHD Fusion");
        this.add("manual.galacticraft.entry.helium3_fusion.flow_title", "Regolith -> 3He Isotope -> Aneutronic MHD Power");
        this.add("manual.galacticraft.entry.helium3_fusion.caption", "Clean 2,500-5,000 GJ/t power and sub-2K zero-boiloff superfluid cryogenics.");
        this.add("manual.galacticraft.flow.he3.s1", "1. Thermal Desorb Lunar/Mercurian Regolith");
        this.add("manual.galacticraft.flow.he3.d1", "Heats ilmenite >700C to liberate Solar Volatile Gas.");
        this.add("manual.galacticraft.flow.he3.s2", "2. Isotopic Centrifuge Enrichment");
        this.add("manual.galacticraft.flow.he3.d2", "Separates pure Helium-3 using Nb3Sn magnets.");
        this.add("manual.galacticraft.flow.he3.s3", "3. Pressurize Helium-3 Fusion Cells");
        this.add("manual.galacticraft.flow.he3.d3", "Pair with Deuterium for D-3He fuel & <2K cryo tanks.");
        this.add("manual.galacticraft.flow.he3.s4", "4. Ignite 3x3x3 MHD Fusion Reactor");
        this.add("manual.galacticraft.flow.he3.d4", "Powers outer-system megabases & Direct Fusion Drives.");

        // Atlas sheets (10 bodies)
        this.add("manual.galacticraft.atlas.earth.title", "Earth (Overworld)");
        this.add("manual.galacticraft.atlas.earth.hazards", "Nominal 1.0G / 1.0 atm breathable N2-O2. Deep pyrochlore Niobium veins.");
        this.add("manual.galacticraft.atlas.moon.title", "The Moon");
        this.add("manual.galacticraft.atlas.moon.hazards", "Vacuum (0 atm), 0.166G, meteor falls. Rich in Ilmenite (3He), Sapphire, Olivine.");
        this.add("manual.galacticraft.atlas.mars.title", "Mars");
        this.add("manual.galacticraft.atlas.mars.hazards", "95.3% CO2 (0.006 atm), 0.38G, cold nights (-100C). Native Desh & Sabatier ISRU.");
        this.add("manual.galacticraft.atlas.asteroids.title", "Asteroid Belt");
        this.add("manual.galacticraft.atlas.asteroids.hazards", "0.0G microgravity, vacuum, -180C natural cryo. Peak Ilmenite Titanium & Dense Ice.");
        this.add("manual.galacticraft.atlas.mercury.title", "Mercury");
        this.add("manual.galacticraft.atlas.mercury.hazards", "6.67x solar flux, +430C day / -180C night, radiation. Sulfide Thorium & 3x 3He.");
        this.add("manual.galacticraft.atlas.venus.title", "Venus");
        this.add("manual.galacticraft.atlas.venus.hazards", "CRITICAL: 92.0 atm crushing pressure, +462C, H2SO4 acid rain. Lead & Solar Ore.");
        this.add("manual.galacticraft.atlas.jupiter.title", "Jupiter");
        this.add("manual.galacticraft.atlas.jupiter.hazards", "CRITICAL: 2.36G gravity, 12 atm, intense radiation, 0.037x solar. Palladium & Metallic H2.");
        this.add("manual.galacticraft.atlas.saturn.title", "Saturn");
        this.add("manual.galacticraft.atlas.saturn.hazards", "0.92G, 8.5 atm, cryogenic rings, 0.011x solar. Magnesium-Lithium & 3.5x Helium-3.");
        this.add("manual.galacticraft.atlas.uranus.title", "Uranus");
        this.add("manual.galacticraft.atlas.uranus.hazards", "Abyssal cold (49K / -224C), diamond hail, 0.003x solar. Uranium & Lonsdaleite.");
        this.add("manual.galacticraft.atlas.neptune.title", "Neptune");
        this.add("manual.galacticraft.atlas.neptune.hazards", "Supersonic winds, 58K cryo, 0.001x solar. Superionic Ice Crystals & Xenon.");

        this.add("manual.galacticraft.entry.in_situ_labs.title", "In-Situ Laboratory Guide");
        this.add("manual.galacticraft.entry.in_situ_labs.h1", "Research Where the Physics Happen");
        this.add("manual.galacticraft.entry.in_situ_labs.body1", "Instead of grinding rockets blindly, build specialized laboratories on each world: Observatory Telescopes in vacuum, Geological Analyzers on planetary crusts, Environmental Chambers in 92 atm Venus, and Zero-G Chambers in orbit.");
        this.add("manual.galacticraft.entry.in_situ_labs.info1", "Use the location filter above to see every research node available at your current outpost!");

        this.add("manual.galacticraft.entry.flight_telemetry.title", "Flight Telemetry & Probes");
        this.add("manual.galacticraft.entry.flight_telemetry.h1", "8 Science Payload Modules & Data Drives");
        this.add("manual.galacticraft.entry.flight_telemetry.body1", "Load Blank Data Drives into science_* payload modules before launch. Flight Recorders earn +200% disaster data on thermal failure; Atmospheric Spectrometers sample gas composition remotely; Multispectral Cameras generate Orbital Survey Maps.");
        this.add("manual.galacticraft.entry.flight_telemetry.tip1", "Process Recorded Data Drives in a Telemetry Analyzer to feed SP & Domain points into your R&D Terminal.");

        this.add("manual.galacticraft.entry.recipe_index.title", "Global Search & EMI/REI");
        this.add("manual.galacticraft.entry.recipe_index.h1", "Cross-Linked Aerospace Index");
        this.add("manual.galacticraft.entry.recipe_index.body1", "Type in the top-right search bar to filter every chapter, machine, alloy, and planet instantaneously (accent-insensitive). Click any item icon anywhere in the manual to jump to its chapter, or use the EMI/REI button to inspect full crafting trees.");
        this.add("manual.galacticraft.entry.recipe_index.tip1", "Press [Shift + G] in-game to collapse or expand your Pinned Mission HUD card.");
    }

    protected void decorationSet(DecorationSet decoSet, String translation) {
        this.block(decoSet.block(), translation);
        this.block(decoSet.stairs(), translation + " Stairs");
        this.block(decoSet.slab(), translation + " Slab");
        this.block(decoSet.wall(), translation + " Wall");
        this.block(decoSet.detailedBlock(), "Detailed " + translation);
        this.block(decoSet.detailedStairs(), "Detailed " + translation + " Stairs");
        this.block(decoSet.detailedSlab(), "Detailed " + translation + " Slab");
        this.block(decoSet.detailedWall(), "Detailed " + translation + " Wall");
    }

    protected void blockDesc(Block block, String translation) {
        this.add(block.getDescriptionId() + ".description", translation);
    }

    protected void musicDisc(Item item, String translation, String description) {
        this.item(item, translation);
        this.add(item.getDescriptionId() + ".desc", description);
    }

    protected void deathBy(ResourceKey<DamageType> key, String translation) {
        if (!translation.contains("%s")) throw new IllegalArgumentException("Death message must contain %s");
        this.add("death.attack." + key.location().getPath(), translation);
    }

    protected void deathBy(ResourceKey<DamageType> key, String translation, String playerTranslation) {
        this.deathBy(key, translation);
        Matcher matcher = Pattern.compile("%s").matcher(playerTranslation);
        if (matcher.results().count() < 2)
            throw new IllegalArgumentException(".player death message must contain two instances of %s");
        this.add("death.attack." + key.location().getPath() + ".player", playerTranslation);
    }

    protected void rocketPart(ResourceKey<? extends RocketPart<?, ?>> key, String translation) {
        this.add(RocketPart.getKey(key), translation);
    }

    protected void advancement(String translationKey, String title, String description) {
        this.add(translationKey + ".title", title);
        this.add(translationKey + ".description", description);
    }

    protected void cape(String key, String translation) {
        this.add(Ui.CAPE + key, translation);
    }

    @Override
    public @NotNull String getName() {
        return "Translations";
    }
}
