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
import dev.galacticraft.api.registry.AddonRegistries;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.api.universe.celestialbody.landable.Landable;
import dev.galacticraft.impl.universe.celestialbody.config.PlanetConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.GCTeleporterTypes;
import dev.galacticraft.mod.content.block.entity.machine.AtmosphericFractionatorBlockEntity;
import dev.galacticraft.mod.content.teleporters.AerostaticCelestialTeleporterType;
import dev.galacticraft.mod.events.GCGasGiantAtmosphereHandler;
import dev.galacticraft.mod.world.biome.GCBiomes;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import dev.galacticraft.mod.world.gen.GCDensityFunctions;
import dev.galacticraft.mod.world.gen.GCNoiseGeneratorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.List;

/**
 * Headless GameTest suite for the Outer Solar System (Jupiter, Saturn, Uranus, Neptune).
 * Implements opaque-box 4-tier testing (Feature Coverage, Boundary Cases, Cross-Feature Combinations, Real-World Application Scenarios).
 */
public class OuterPlanetsTestSuite implements GalacticraftGameTest {

    // ==========================================
    // TIER 1: FEATURE COVERAGE (JUPITER)
    // ==========================================

    @GameTest(template = EMPTY_STRUCTURE)
    public void testJupiterCelestialBodyRegistration(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> jupiter = registry.get(GCCelestialBodies.JUPITER);
        if (jupiter == null) {
            context.fail("Expected GCCelestialBodies.JUPITER to be registered in CELESTIAL_BODY registry!");
            return;
        }
        if (!(jupiter.type() instanceof Landable<?>)) {
            context.fail("Expected Jupiter celestial body type to implement Landable, found: " + jupiter.type());
            return;
        }
        if (!(jupiter.config() instanceof PlanetConfig)) {
            context.fail("Expected Jupiter config to be PlanetConfig, found: " + jupiter.config());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testJupiterAstrophysicalParameters(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> jupiter = registry.get(GCCelestialBodies.JUPITER);
        if (jupiter == null || !(jupiter.config() instanceof PlanetConfig config)) {
            context.fail("Jupiter celestial body is missing or invalid!");
            return;
        }
        if (config.accessWeight() != 4) {
            context.fail("Expected Jupiter accessWeight (rocket tier) to be 4, found: " + config.accessWeight());
            return;
        }
        if (Math.abs(config.gravity() - 2.36F) > 1.0E-4F) {
            context.fail("Expected Jupiter gravity to be 2.36F (24.79 m/s²), found: " + config.gravity());
            return;
        }
        if (config.dayLength() != 9925L) {
            context.fail("Expected Jupiter day length to be 9925L (9.925h solar rotation), found: " + config.dayLength());
            return;
        }
        if (!GCDimensions.JUPITER.equals(config.world())) {
            context.fail("Expected Jupiter target world to be GCDimensions.JUPITER, found: " + config.world());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testJupiterAtmosphereComposition(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> jupiter = registry.get(GCCelestialBodies.JUPITER);
        if (jupiter == null || !(jupiter.config() instanceof PlanetConfig config)) {
            context.fail("Jupiter celestial body missing or invalid!");
            return;
        }
        if (config.atmosphere().breathable()) {
            context.fail("Expected Jupiter hyperbaric gas atmosphere to be unbreathable!");
            return;
        }
        if (Math.abs(config.atmosphere().pressure() - 12.0F) > 1.0E-4F) {
            context.fail("Expected Jupiter atmospheric pressure to be 12.0 bar, found: " + config.atmosphere().pressure());
            return;
        }
        if (Math.abs(config.atmosphere().temperature() - (-110.0F)) > 1.0E-4F) {
            context.fail("Expected Jupiter atmospheric temperature to be -110.0°C, found: " + config.atmosphere().temperature());
            return;
        }
        double h2Ppm = config.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.HYDROGEN_ID), 0.0);
        double hePpm = config.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.HELIUM_ID), 0.0);
        if (h2Ppm < 890000.0) {
            context.fail("Expected Jupiter atmosphere to be dominated by Hydrogen (>890,000 ppm), found: " + h2Ppm);
            return;
        }
        if (hePpm < 100000.0) {
            context.fail("Expected Jupiter atmosphere to contain Helium (>100,000 ppm), found: " + hePpm);
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testJupiterDimensionAndType(GameTestHelper context) {
        var registryAccess = context.getLevel().registryAccess();
        var dimTypeRegistry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
        DimensionType jupiterDimType = dimTypeRegistry.get(Constant.id("jupiter"));
        if (jupiterDimType == null) {
            context.fail("Expected Jupiter DimensionType to be registered in server registries!");
            return;
        }
        if (jupiterDimType.minY() != -64) {
            context.fail("Expected Jupiter minY to be -64, found: " + jupiterDimType.minY());
            return;
        }
        if (jupiterDimType.height() != 384) {
            context.fail("Expected Jupiter total height to be 384, found: " + jupiterDimType.height());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testJupiterBiomes(GameTestHelper context) {
        var biomeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceKey<net.minecraft.world.level.biome.Biome>> expectedBiomes = List.of(
                GCBiomes.Jupiter.AMMONIA_PLAINS,
                GCBiomes.Jupiter.GREAT_RED_SPOT,
                GCBiomes.Jupiter.SULFURIC_BELTS,
                GCBiomes.Jupiter.METALLIC_RIDGES,
                GCBiomes.Jupiter.POLAR_AURORA_CRATERS,
                GCBiomes.Jupiter.METALLIC_HYDROGEN_CAVERNS,
                GCBiomes.Jupiter.CYCLONIC_RIFTS,
                GCBiomes.Jupiter.CORE_MAGMA_DEPTHS
        );
        for (var biomeKey : expectedBiomes) {
            if (!biomeRegistry.containsKey(biomeKey)) {
                context.fail("Expected Jupiter biome to be registered: " + biomeKey.location());
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testJupiterLithologySuite(GameTestHelper context) {
        List<Block> expectedBlocks = List.of(
                GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK,
                GCBlocks.JUPITER_CORE_BRECCIA,
                GCBlocks.JUPITER_PALLADIUM_ORE
        );
        for (Block block : expectedBlocks) {
            if (BuiltInRegistries.BLOCK.getResourceKey(block).isEmpty()) {
                context.fail("Expected Jupiter hyperbaric core block to be registered: " + block);
                return;
            }
        }
        for (String removedId : List.of("jupiter_surface", "jupiter_stone", "jupiter_stone_bricks", "jupiter_copper_ore", "jupiter_iron_ore")) {
            if (BuiltInRegistries.BLOCK.containsKey(Constant.id(removedId))) {
                context.fail("Removed nonsensical Jupiter block must not be registered: " + removedId);
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testJupiterDensityRouter(GameTestHelper context) {
        var densityRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);
        List<ResourceKey<net.minecraft.world.level.levelgen.DensityFunction>> keys = List.of(
                GCDensityFunctions.Jupiter.CONTINENTS,
                GCDensityFunctions.Jupiter.BASE_HEIGHT,
                GCDensityFunctions.Jupiter.OFFSET,
                GCDensityFunctions.Jupiter.FACTOR,
                GCDensityFunctions.Jupiter.SMOOTH_RELIEF,
                GCDensityFunctions.Jupiter.DEPTH,
                GCDensityFunctions.Jupiter.SOLID_TERRAIN,
                GCDensityFunctions.Jupiter.CAVES,
                GCDensityFunctions.Jupiter.PILLARS,
                GCDensityFunctions.Jupiter.FINAL_DENSITY
        );
        for (var key : keys) {
            if (!densityRegistry.containsKey(key)) {
                context.fail("Expected Jupiter density function to be registered: " + key.location());
                return;
            }
        }
        context.succeed();
    }

    // ==========================================
    // TIER 1: FEATURE COVERAGE (SATURN)
    // ==========================================

    @GameTest(template = EMPTY_STRUCTURE)
    public void testSaturnCelestialBodyRegistration(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> saturn = registry.get(GCCelestialBodies.SATURN);
        if (saturn == null) {
            context.fail("Expected GCCelestialBodies.SATURN to be registered in CELESTIAL_BODY registry!");
            return;
        }
        if (!(saturn.type() instanceof Landable<?>)) {
            context.fail("Expected Saturn celestial body type to implement Landable, found: " + saturn.type());
            return;
        }
        if (!(saturn.config() instanceof PlanetConfig)) {
            context.fail("Expected Saturn config to be PlanetConfig, found: " + saturn.config());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testSaturnAstrophysicalParameters(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> saturn = registry.get(GCCelestialBodies.SATURN);
        if (saturn == null || !(saturn.config() instanceof PlanetConfig config)) {
            context.fail("Saturn celestial body missing or invalid!");
            return;
        }
        if (config.accessWeight() != 5) {
            context.fail("Expected Saturn accessWeight (rocket tier) to be 5, found: " + config.accessWeight());
            return;
        }
        if (Math.abs(config.gravity() - 0.916F) > 1.0E-4F) {
            context.fail("Expected Saturn gravity to be 0.916F (10.44 m/s²), found: " + config.gravity());
            return;
        }
        if (config.dayLength() != 10656L) {
            context.fail("Expected Saturn day length to be 10656L (10.656h solar day), found: " + config.dayLength());
            return;
        }
        if (!GCDimensions.SATURN.equals(config.world())) {
            context.fail("Expected Saturn target world to be GCDimensions.SATURN, found: " + config.world());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testSaturnAtmosphereComposition(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> saturn = registry.get(GCCelestialBodies.SATURN);
        if (saturn == null || !(saturn.config() instanceof PlanetConfig config)) {
            context.fail("Saturn celestial body missing or invalid!");
            return;
        }
        if (config.atmosphere().breathable()) {
            context.fail("Expected Saturn atmosphere to be unbreathable!");
            return;
        }
        if (Math.abs(config.atmosphere().pressure() - 8.5F) > 1.0E-4F) {
            context.fail("Expected Saturn atmospheric pressure to be 8.5 bar, found: " + config.atmosphere().pressure());
            return;
        }
        if (Math.abs(config.atmosphere().temperature() - (-140.0F)) > 1.0E-4F) {
            context.fail("Expected Saturn atmospheric temperature to be -140.0°C, found: " + config.atmosphere().temperature());
            return;
        }
        double h2Ppm = config.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.HYDROGEN_ID), 0.0);
        if (h2Ppm < 950000.0) {
            context.fail("Expected Saturn atmosphere to be dominated by Hydrogen (>950,000 ppm), found: " + h2Ppm);
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testSaturnDimensionAndType(GameTestHelper context) {
        var registryAccess = context.getLevel().registryAccess();
        var dimTypeRegistry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
        DimensionType saturnDimType = dimTypeRegistry.get(Constant.id("saturn"));
        if (saturnDimType == null) {
            context.fail("Expected Saturn DimensionType to be registered in server registries!");
            return;
        }
        if (saturnDimType.minY() != -64 || saturnDimType.height() != 384) {
            context.fail("Expected Saturn bounds [-64, 320] (height 384), found: minY=" + saturnDimType.minY() + ", height=" + saturnDimType.height());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testSaturnBiomes(GameTestHelper context) {
        var biomeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceKey<net.minecraft.world.level.biome.Biome>> expectedBiomes = List.of(
                GCBiomes.Saturn.GOLDEN_HAZE_PLAINS,
                GCBiomes.Saturn.HYDROCARBON_SEAS,
                GCBiomes.Saturn.RING_SHADOW_HIGHLANDS,
                GCBiomes.Saturn.AMMONIA_STORM_BANDS,
                GCBiomes.Saturn.HEXAGON_POLAR_VORTEX,
                GCBiomes.Saturn.HELIUM_RAIN_CAVERNS,
                GCBiomes.Saturn.CRYSTALLINE_RING_TUBES,
                GCBiomes.Saturn.MANTLE_CORE_ABYSS
        );
        for (var biomeKey : expectedBiomes) {
            if (!biomeRegistry.containsKey(biomeKey)) {
                context.fail("Expected Saturn biome to be registered: " + biomeKey.location());
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testSaturnLithologySuite(GameTestHelper context) {
        List<Block> expectedBlocks = List.of(
                GCBlocks.SATURN_HELIUM_MANTLE_ROCK,
                GCBlocks.SATURN_MAGNESIUM_ORE
        );
        for (Block block : expectedBlocks) {
            if (BuiltInRegistries.BLOCK.getResourceKey(block).isEmpty()) {
                context.fail("Expected Saturn hyperbaric core block to be registered: " + block);
                return;
            }
        }
        for (String removedId : List.of("saturn_surface", "saturn_stone", "saturn_stone_bricks", "saturn_copper_ore", "saturn_iron_ore")) {
            if (BuiltInRegistries.BLOCK.containsKey(Constant.id(removedId))) {
                context.fail("Removed nonsensical Saturn block must not be registered: " + removedId);
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testSaturnDensityRouter(GameTestHelper context) {
        var densityRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);
        List<ResourceKey<net.minecraft.world.level.levelgen.DensityFunction>> keys = List.of(
                GCDensityFunctions.Saturn.CONTINENTS,
                GCDensityFunctions.Saturn.BASE_HEIGHT,
                GCDensityFunctions.Saturn.OFFSET,
                GCDensityFunctions.Saturn.FACTOR,
                GCDensityFunctions.Saturn.SMOOTH_RELIEF,
                GCDensityFunctions.Saturn.DEPTH,
                GCDensityFunctions.Saturn.SOLID_TERRAIN,
                GCDensityFunctions.Saturn.CAVES,
                GCDensityFunctions.Saturn.PILLARS,
                GCDensityFunctions.Saturn.FINAL_DENSITY
        );
        for (var key : keys) {
            if (!densityRegistry.containsKey(key)) {
                context.fail("Expected Saturn density function to be registered: " + key.location());
                return;
            }
        }
        context.succeed();
    }

    // ==========================================
    // TIER 1: FEATURE COVERAGE (URANUS)
    // ==========================================

    @GameTest(template = EMPTY_STRUCTURE)
    public void testUranusCelestialBodyRegistration(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> uranus = registry.get(GCCelestialBodies.URANUS);
        if (uranus == null) {
            context.fail("Expected GCCelestialBodies.URANUS to be registered in CELESTIAL_BODY registry!");
            return;
        }
        if (!(uranus.type() instanceof Landable<?>)) {
            context.fail("Expected Uranus celestial body type to implement Landable, found: " + uranus.type());
            return;
        }
        if (!(uranus.config() instanceof PlanetConfig)) {
            context.fail("Expected Uranus config to be PlanetConfig, found: " + uranus.config());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testUranusAstrophysicalParameters(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> uranus = registry.get(GCCelestialBodies.URANUS);
        if (uranus == null || !(uranus.config() instanceof PlanetConfig config)) {
            context.fail("Uranus celestial body missing or invalid!");
            return;
        }
        if (config.accessWeight() != 6) {
            context.fail("Expected Uranus accessWeight (rocket tier) to be 6, found: " + config.accessWeight());
            return;
        }
        if (Math.abs(config.gravity() - 0.886F) > 1.0E-4F) {
            context.fail("Expected Uranus gravity to be 0.886F (8.69 m/s²), found: " + config.gravity());
            return;
        }
        if (config.dayLength() != 17240L) {
            context.fail("Expected Uranus day length to be 17240L (17.24h calibrated retrograde day), found: " + config.dayLength());
            return;
        }
        if (!GCDimensions.URANUS.equals(config.world())) {
            context.fail("Expected Uranus target world to be GCDimensions.URANUS, found: " + config.world());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testUranusAtmosphereComposition(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> uranus = registry.get(GCCelestialBodies.URANUS);
        if (uranus == null || !(uranus.config() instanceof PlanetConfig config)) {
            context.fail("Uranus celestial body missing or invalid!");
            return;
        }
        if (config.atmosphere().breathable()) {
            context.fail("Expected Uranus atmosphere to be unbreathable!");
            return;
        }
        if (Math.abs(config.atmosphere().pressure() - 5.0F) > 1.0E-4F) {
            context.fail("Expected Uranus atmospheric pressure to be 5.0 bar, found: " + config.atmosphere().pressure());
            return;
        }
        if (Math.abs(config.atmosphere().temperature() - (-195.0F)) > 1.0E-4F) {
            context.fail("Expected Uranus atmospheric temperature to be -195.0°C, found: " + config.atmosphere().temperature());
            return;
        }
        double ch4Ppm = config.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.METHANE_ID), 0.0);
        if (ch4Ppm < 20000.0) {
            context.fail("Expected Uranus atmosphere to contain high Methane fraction (>20,000 ppm), found: " + ch4Ppm);
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testUranusDimensionAndType(GameTestHelper context) {
        var registryAccess = context.getLevel().registryAccess();
        var dimTypeRegistry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
        DimensionType uranusDimType = dimTypeRegistry.get(Constant.id("uranus"));
        if (uranusDimType == null) {
            context.fail("Expected Uranus DimensionType to be registered in server registries!");
            return;
        }
        if (uranusDimType.minY() != -64 || uranusDimType.height() != 384) {
            context.fail("Expected Uranus bounds [-64, 320] (height 384), found: minY=" + uranusDimType.minY() + ", height=" + uranusDimType.height());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testUranusBiomes(GameTestHelper context) {
        var biomeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceKey<net.minecraft.world.level.biome.Biome>> expectedBiomes = List.of(
                GCBiomes.Uranus.FROZEN_METHANE_PLAINS,
                GCBiomes.Uranus.MIRANDA_VERONA_CLIFFS,
                GCBiomes.Uranus.CRYO_GEYSER_BASIN,
                GCBiomes.Uranus.TILTED_POLAR_CAPS,
                GCBiomes.Uranus.RING_DEBRIS_BADLANDS,
                GCBiomes.Uranus.DIAMOND_MANTLE_CAVERNS,
                GCBiomes.Uranus.GLACIAL_FAULT_CHASMS,
                GCBiomes.Uranus.SUPERIONIC_ICE_DEPTHS
        );
        for (var biomeKey : expectedBiomes) {
            if (!biomeRegistry.containsKey(biomeKey)) {
                context.fail("Expected Uranus biome to be registered: " + biomeKey.location());
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testUranusLithologySuite(GameTestHelper context) {
        List<Block> expectedBlocks = List.of(
                GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK,
                GCBlocks.URANUS_DIAMOND_BEARING_ROCK,
                GCBlocks.URANUS_URANIUM_ORE
        );
        for (Block block : expectedBlocks) {
            if (BuiltInRegistries.BLOCK.getResourceKey(block).isEmpty()) {
                context.fail("Expected Uranus hyperbaric core block to be registered: " + block);
                return;
            }
        }
        for (String removedId : List.of("uranus_surface", "uranus_stone", "uranus_methane_ice", "uranus_copper_ore")) {
            if (BuiltInRegistries.BLOCK.containsKey(Constant.id(removedId))) {
                context.fail("Removed nonsensical Uranus block must not be registered: " + removedId);
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testUranusDensityRouter(GameTestHelper context) {
        var densityRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);
        List<ResourceKey<net.minecraft.world.level.levelgen.DensityFunction>> keys = List.of(
                GCDensityFunctions.Uranus.CONTINENTS,
                GCDensityFunctions.Uranus.BASE_HEIGHT,
                GCDensityFunctions.Uranus.OFFSET,
                GCDensityFunctions.Uranus.FACTOR,
                GCDensityFunctions.Uranus.SMOOTH_RELIEF,
                GCDensityFunctions.Uranus.DEPTH,
                GCDensityFunctions.Uranus.SOLID_TERRAIN,
                GCDensityFunctions.Uranus.CAVES,
                GCDensityFunctions.Uranus.PILLARS,
                GCDensityFunctions.Uranus.FINAL_DENSITY
        );
        for (var key : keys) {
            if (!densityRegistry.containsKey(key)) {
                context.fail("Expected Uranus density function to be registered: " + key.location());
                return;
            }
        }
        context.succeed();
    }

    // ==========================================
    // TIER 1: FEATURE COVERAGE (NEPTUNE)
    // ==========================================

    @GameTest(template = EMPTY_STRUCTURE)
    public void testNeptuneCelestialBodyRegistration(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> neptune = registry.get(GCCelestialBodies.NEPTUNE);
        if (neptune == null) {
            context.fail("Expected GCCelestialBodies.NEPTUNE to be registered in CELESTIAL_BODY registry!");
            return;
        }
        if (!(neptune.type() instanceof Landable<?>)) {
            context.fail("Expected Neptune celestial body type to implement Landable, found: " + neptune.type());
            return;
        }
        if (!(neptune.config() instanceof PlanetConfig)) {
            context.fail("Expected Neptune config to be PlanetConfig, found: " + neptune.config());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testNeptuneAstrophysicalParameters(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> neptune = registry.get(GCCelestialBodies.NEPTUNE);
        if (neptune == null || !(neptune.config() instanceof PlanetConfig config)) {
            context.fail("Neptune celestial body missing or invalid!");
            return;
        }
        if (config.accessWeight() != 7) {
            context.fail("Expected Neptune accessWeight (rocket tier) to be 7, found: " + config.accessWeight());
            return;
        }
        if (Math.abs(config.gravity() - 1.14F) > 1.0E-4F) {
            context.fail("Expected Neptune gravity to be 1.14F (11.15 m/s²), found: " + config.gravity());
            return;
        }
        if (config.dayLength() != 16110L) {
            context.fail("Expected Neptune day length to be 16110L (16.11h solar day), found: " + config.dayLength());
            return;
        }
        if (!GCDimensions.NEPTUNE.equals(config.world())) {
            context.fail("Expected Neptune target world to be GCDimensions.NEPTUNE, found: " + config.world());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testNeptuneAtmosphereComposition(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> neptune = registry.get(GCCelestialBodies.NEPTUNE);
        if (neptune == null || !(neptune.config() instanceof PlanetConfig config)) {
            context.fail("Neptune celestial body missing or invalid!");
            return;
        }
        if (config.atmosphere().breathable()) {
            context.fail("Expected Neptune atmosphere to be unbreathable!");
            return;
        }
        if (Math.abs(config.atmosphere().pressure() - 7.5F) > 1.0E-4F) {
            context.fail("Expected Neptune atmospheric pressure to be 7.5 bar, found: " + config.atmosphere().pressure());
            return;
        }
        if (Math.abs(config.atmosphere().temperature() - (-200.0F)) > 1.0E-4F) {
            context.fail("Expected Neptune atmospheric temperature to be -200.0°C, found: " + config.atmosphere().temperature());
            return;
        }
        double n2Ppm = config.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.NITROGEN_ID), 0.0);
        if (n2Ppm < 250.0) {
            context.fail("Expected Neptune atmosphere to contain trace Nitrogen (>=250 ppm), found: " + n2Ppm);
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testNeptuneDimensionAndType(GameTestHelper context) {
        var registryAccess = context.getLevel().registryAccess();
        var dimTypeRegistry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
        DimensionType neptuneDimType = dimTypeRegistry.get(Constant.id("neptune"));
        if (neptuneDimType == null) {
            context.fail("Expected Neptune DimensionType to be registered in server registries!");
            return;
        }
        if (neptuneDimType.minY() != -64 || neptuneDimType.height() != 384) {
            context.fail("Expected Neptune bounds [-64, 320] (height 384), found: minY=" + neptuneDimType.minY() + ", height=" + neptuneDimType.height());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testNeptuneBiomes(GameTestHelper context) {
        var biomeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceKey<net.minecraft.world.level.biome.Biome>> expectedBiomes = List.of(
                GCBiomes.Neptune.AZURE_METHANE_PLAINS,
                GCBiomes.Neptune.GREAT_DARK_SPOT,
                GCBiomes.Neptune.TRITON_NITROGEN_GEYSERS,
                GCBiomes.Neptune.SUPERSONIC_WIND_RIDGES,
                GCBiomes.Neptune.CRYOGENIC_POLAR_ABYSS,
                GCBiomes.Neptune.DIAMOND_RAIN_CAVERNS,
                GCBiomes.Neptune.CRYO_MAGMA_TUBES,
                GCBiomes.Neptune.ABYSSAL_MANTLE_DEPTHS
        );
        for (var biomeKey : expectedBiomes) {
            if (!biomeRegistry.containsKey(biomeKey)) {
                context.fail("Expected Neptune biome to be registered: " + biomeKey.location());
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testNeptuneLithologySuite(GameTestHelper context) {
        if (BuiltInRegistries.BLOCK.getResourceKey(GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK).isEmpty()) {
            context.fail("Expected Neptune hyperbaric core block to be registered: NEPTUNE_ABYSSAL_MANTLE_ROCK");
            return;
        }
        for (String removedId : List.of("neptune_surface", "neptune_stone", "neptune_nitrogen_ice", "neptune_titanium_ore")) {
            if (BuiltInRegistries.BLOCK.containsKey(Constant.id(removedId))) {
                context.fail("Removed nonsensical Neptune block must not be registered: " + removedId);
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testNeptuneDensityRouter(GameTestHelper context) {
        var densityRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);
        List<ResourceKey<net.minecraft.world.level.levelgen.DensityFunction>> keys = List.of(
                GCDensityFunctions.Neptune.CONTINENTS,
                GCDensityFunctions.Neptune.BASE_HEIGHT,
                GCDensityFunctions.Neptune.OFFSET,
                GCDensityFunctions.Neptune.FACTOR,
                GCDensityFunctions.Neptune.SMOOTH_RELIEF,
                GCDensityFunctions.Neptune.DEPTH,
                GCDensityFunctions.Neptune.SOLID_TERRAIN,
                GCDensityFunctions.Neptune.CAVES,
                GCDensityFunctions.Neptune.PILLARS,
                GCDensityFunctions.Neptune.FINAL_DENSITY
        );
        for (var key : keys) {
            if (!densityRegistry.containsKey(key)) {
                context.fail("Expected Neptune density function to be registered: " + key.location());
                return;
            }
        }
        context.succeed();
    }

    // ==========================================
    // TIER 2: BOUNDARY & CORNER CASES
    // ==========================================

    @GameTest(template = EMPTY_STRUCTURE)
    public void testVerticalDimensionBoundsAndHeightEnvelope(GameTestHelper context) {
        var dimTypeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        List<String> planetIds = List.of("jupiter", "saturn", "uranus", "neptune");
        for (String id : planetIds) {
            DimensionType type = dimTypeRegistry.get(Constant.id(id));
            if (type == null) {
                context.fail("DimensionType " + id + " not registered!");
                return;
            }
            if (type.minY() != -64) {
                context.fail("DimensionType " + id + " minY must be -64, found: " + type.minY());
                return;
            }
            if (type.height() != 384) {
                context.fail("DimensionType " + id + " height must be 384, found: " + type.height());
                return;
            }
            if (type.logicalHeight() != 384) {
                context.fail("DimensionType " + id + " logicalHeight must be 384, found: " + type.logicalHeight());
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testHyperbaricAtmospherePressureHierarchy(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> jupiter = registry.get(GCCelestialBodies.JUPITER);
        CelestialBody<?, ?> saturn = registry.get(GCCelestialBodies.SATURN);
        CelestialBody<?, ?> uranus = registry.get(GCCelestialBodies.URANUS);
        CelestialBody<?, ?> neptune = registry.get(GCCelestialBodies.NEPTUNE);

        if (jupiter == null || saturn == null || uranus == null || neptune == null) {
            context.fail("One or more outer celestial bodies are missing from registry!");
            return;
        }

        float pJupiter = ((PlanetConfig) jupiter.config()).atmosphere().pressure();
        float pSaturn = ((PlanetConfig) saturn.config()).atmosphere().pressure();
        float pUranus = ((PlanetConfig) uranus.config()).atmosphere().pressure();
        float pNeptune = ((PlanetConfig) neptune.config()).atmosphere().pressure();

        // Physical assertion: Jupiter (12.0) > Saturn (8.5) > Neptune (7.5) > Uranus (5.0) > 1.0 (Earth standard)
        if (!(pJupiter > pSaturn && pSaturn > pNeptune && pNeptune > pUranus && pUranus > 1.0F)) {
            context.fail(String.format("Pressure hierarchy violated! Expected Jupiter(%.1f) > Saturn(%.1f) > Neptune(%.1f) > Uranus(%.1f) > 1.0",
                    pJupiter, pSaturn, pNeptune, pUranus));
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testCryogenicTemperatureLimitsAndMobSpawning(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        List<ResourceKey<CelestialBody<?, ?>>> outerKeys = List.of(
                GCCelestialBodies.JUPITER,
                GCCelestialBodies.SATURN,
                GCCelestialBodies.URANUS,
                GCCelestialBodies.NEPTUNE
        );

        for (var key : outerKeys) {
            CelestialBody<?, ?> body = registry.get(key);
            if (body == null || !(body.config() instanceof PlanetConfig config)) {
                context.fail("Body " + key.location() + " missing or not PlanetConfig!");
                return;
            }
            // Cryogenic assertion: all outer planets are below -100°C
            if (config.atmosphere().temperature() > -100.0) {
                context.fail("Expected cryogenic temperature (< -100°C) for " + key.location() + ", found: " + config.atmosphere().temperature());
                return;
            }
            // Atmosphere breathability assertion: unbreathable
            if (config.atmosphere().breathable()) {
                context.fail("Outer planet " + key.location() + " must have unbreathable atmosphere!");
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testGravityHierarchyAndScalingBounds(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        float gJupiter = ((PlanetConfig) registry.get(GCCelestialBodies.JUPITER).config()).gravity();
        float gSaturn = ((PlanetConfig) registry.get(GCCelestialBodies.SATURN).config()).gravity();
        float gUranus = ((PlanetConfig) registry.get(GCCelestialBodies.URANUS).config()).gravity();
        float gNeptune = ((PlanetConfig) registry.get(GCCelestialBodies.NEPTUNE).config()).gravity();

        // Astrophysics order: Jupiter (2.36g) > Neptune (1.14g) > 1.0g > Saturn (0.916g) > Uranus (0.886g)
        if (!(gJupiter > gNeptune && gNeptune > 1.0F && 1.0F > gSaturn && gSaturn > gUranus && gUranus > 0.0F)) {
            context.fail(String.format("Gravity hierarchy violated! Expected Jupiter(%.3f) > Neptune(%.3f) > 1.0 > Saturn(%.3f) > Uranus(%.3f) > 0",
                    gJupiter, gNeptune, gSaturn, gUranus));
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testZeroOxygenAsphyxiationEnvelope(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        List<ResourceKey<CelestialBody<?, ?>>> outerKeys = List.of(
                GCCelestialBodies.JUPITER,
                GCCelestialBodies.SATURN,
                GCCelestialBodies.URANUS,
                GCCelestialBodies.NEPTUNE
        );

        for (var key : outerKeys) {
            CelestialBody<?, ?> body = registry.get(key);
            PlanetConfig config = (PlanetConfig) body.config();
            double o2Ppm = config.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.OXYGEN_ID), 0.0);
            if (o2Ppm > 0.0) {
                context.fail("Expected zero free O2 in reducing outer atmosphere of " + key.location() + ", found: " + o2Ppm);
                return;
            }
        }
        context.succeed();
    }

    // ==========================================
    // TIER 3: CROSS-FEATURE COMBINATIONS
    // ==========================================

    @GameTest(template = EMPTY_STRUCTURE)
    public void testCelestialTeleporterBindingAndDimensionTarget(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        List<ResourceKey<CelestialBody<?, ?>>> outerKeys = List.of(
                GCCelestialBodies.JUPITER,
                GCCelestialBodies.SATURN,
                GCCelestialBodies.URANUS,
                GCCelestialBodies.NEPTUNE
        );

        for (var key : outerKeys) {
            CelestialBody<?, ?> body = registry.get(key);
            PlanetConfig config = (PlanetConfig) body.config();
            if (!config.teleporter().is(GCTeleporterTypes.AEROSTATIC_CELESTIAL_TELEPORTER)) {
                context.fail("Expected gas/ice giant " + key.location() + " to use AEROSTATIC_CELESTIAL_TELEPORTER, found: " + config.teleporter().unwrapKey().map(k -> k.location().toString()).orElse("unregistered"));
                return;
            }
            if (config.world() == null) {
                context.fail("Planet " + key.location() + " has null world destination!");
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testLithologyHardnessScalingAndHarvestingTools(GameTestHelper context) {
        // Assert all kept hyperbaric abyssal core blocks require correct tool for drops and have high pressure-hardened durability
        List<Block> coreBlocks = List.of(
                GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK, GCBlocks.JUPITER_CORE_BRECCIA, GCBlocks.JUPITER_PALLADIUM_ORE,
                GCBlocks.SATURN_HELIUM_MANTLE_ROCK, GCBlocks.SATURN_MAGNESIUM_ORE,
                GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK, GCBlocks.URANUS_DIAMOND_BEARING_ROCK, GCBlocks.URANUS_URANIUM_ORE,
                GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK
        );
        for (Block b : coreBlocks) {
            if (!b.defaultBlockState().requiresCorrectToolForDrops()) {
                context.fail("Hyperbaric core block " + b + " must require correct tool for drops!");
                return;
            }
            if (b.defaultDestroyTime() < 4.0F) {
                context.fail("Hyperbaric core block " + b + " hardness too low (" + b.defaultDestroyTime() + ")");
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testMultiNoiseBiomeSourceSeparation(GameTestHelper context) {
        var biomeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        // Ensure all 32 biomes exist and are distinct
        int count = 0;
        List<ResourceKey<net.minecraft.world.level.biome.Biome>> allOuterBiomes = List.of(
                GCBiomes.Jupiter.AMMONIA_PLAINS, GCBiomes.Jupiter.GREAT_RED_SPOT, GCBiomes.Jupiter.SULFURIC_BELTS, GCBiomes.Jupiter.METALLIC_RIDGES,
                GCBiomes.Jupiter.POLAR_AURORA_CRATERS, GCBiomes.Jupiter.METALLIC_HYDROGEN_CAVERNS, GCBiomes.Jupiter.CYCLONIC_RIFTS, GCBiomes.Jupiter.CORE_MAGMA_DEPTHS,
                GCBiomes.Saturn.GOLDEN_HAZE_PLAINS, GCBiomes.Saturn.HYDROCARBON_SEAS, GCBiomes.Saturn.RING_SHADOW_HIGHLANDS, GCBiomes.Saturn.AMMONIA_STORM_BANDS,
                GCBiomes.Saturn.HEXAGON_POLAR_VORTEX, GCBiomes.Saturn.HELIUM_RAIN_CAVERNS, GCBiomes.Saturn.CRYSTALLINE_RING_TUBES, GCBiomes.Saturn.MANTLE_CORE_ABYSS,
                GCBiomes.Uranus.FROZEN_METHANE_PLAINS, GCBiomes.Uranus.MIRANDA_VERONA_CLIFFS, GCBiomes.Uranus.CRYO_GEYSER_BASIN, GCBiomes.Uranus.TILTED_POLAR_CAPS,
                GCBiomes.Uranus.RING_DEBRIS_BADLANDS, GCBiomes.Uranus.DIAMOND_MANTLE_CAVERNS, GCBiomes.Uranus.GLACIAL_FAULT_CHASMS, GCBiomes.Uranus.SUPERIONIC_ICE_DEPTHS,
                GCBiomes.Neptune.AZURE_METHANE_PLAINS, GCBiomes.Neptune.GREAT_DARK_SPOT, GCBiomes.Neptune.TRITON_NITROGEN_GEYSERS, GCBiomes.Neptune.SUPERSONIC_WIND_RIDGES,
                GCBiomes.Neptune.CRYOGENIC_POLAR_ABYSS, GCBiomes.Neptune.DIAMOND_RAIN_CAVERNS, GCBiomes.Neptune.CRYO_MAGMA_TUBES, GCBiomes.Neptune.ABYSSAL_MANTLE_DEPTHS
        );
        for (var biomeKey : allOuterBiomes) {
            if (biomeRegistry.containsKey(biomeKey)) {
                count++;
            }
        }
        if (count != 32) {
            context.fail("Expected 32 registered outer planet biomes, found: " + count);
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testSurfaceRulesAndNoiseGeneratorSettingsIntegration(GameTestHelper context) {
        var noiseSettingsRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.NOISE_SETTINGS);
        List<ResourceKey<NoiseGeneratorSettings>> settingsKeys = List.of(
                GCNoiseGeneratorSettings.JUPITER,
                GCNoiseGeneratorSettings.SATURN,
                GCNoiseGeneratorSettings.URANUS,
                GCNoiseGeneratorSettings.NEPTUNE
        );
        for (var key : settingsKeys) {
            NoiseGeneratorSettings settings = noiseSettingsRegistry.get(key);
            if (settings == null) {
                context.fail("Expected NoiseGeneratorSettings registered for: " + key.location());
                return;
            }
            if (settings.noiseSettings().minY() != -64) {
                context.fail("Expected NoiseSettings minY = -64 for " + key.location() + ", got: " + settings.noiseSettings().minY());
                return;
            }
        }
        context.succeed();
    }

    // ==========================================
    // TIER 4: REAL-WORLD APPLICATION SCENARIOS
    // ==========================================

    @GameTest(template = EMPTY_STRUCTURE)
    public void testJovianHyperbaricDescentScenario(GameTestHelper context) {
        BlockPos testPos = new BlockPos(1, 1, 1);
        context.setBlock(testPos, GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK.defaultBlockState());
        context.setBlock(testPos.above(), GCBlocks.JUPITER_CORE_BRECCIA.defaultBlockState());

        if (!context.getBlockState(testPos).is(GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK)) {
            context.fail("Failed to place JUPITER_METALLIC_HYDROGEN_ROCK!");
            return;
        }
        if (!context.getBlockState(testPos.above()).is(GCBlocks.JUPITER_CORE_BRECCIA)) {
            context.fail("Failed to place JUPITER_CORE_BRECCIA!");
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testGasGiantBarometricPressureAndOpticalExtinction(GameTestHelper context) {
        for (var dim : List.of(GCDimensions.JUPITER, GCDimensions.SATURN, GCDimensions.URANUS, GCDimensions.NEPTUNE)) {
            double pExo = GCGasGiantAtmosphereHandler.computePressureBar(dim, 320.0D);
            double pAero = GCGasGiantAtmosphereHandler.computePressureBar(dim, 216.0D);
            double pCloud = GCGasGiantAtmosphereHandler.computePressureBar(dim, 100.0D);
            double pCore = GCGasGiantAtmosphereHandler.computePressureBar(dim, -20.0D);

            if (pExo >= 0.2D) {
                context.fail("Expected exosphere pressure < 0.2 bar at Y=320 for " + dim.location() + ", got: " + pExo);
                return;
            }
            if (pAero < 0.5D || pAero > 2.5D) {
                context.fail("Expected safe aerostatic pressure [0.5, 2.5] bar at Y=216 for " + dim.location() + ", got: " + pAero);
                return;
            }
            if (!(pCloud > pAero * 5.0D && pCore >= 80.0D)) {
                context.fail("Expected exponential pressure rise into clouds/core for " + dim.location()
                        + ": aero=" + pAero + ", cloud=" + pCloud + ", core=" + pCore);
                return;
            }

            float transAero = GCGasGiantAtmosphereHandler.computeOpticalTransmittance(dim, 220.0D);
            float transCloud = GCGasGiantAtmosphereHandler.computeOpticalTransmittance(dim, 100.0D);
            float transCore = GCGasGiantAtmosphereHandler.computeOpticalTransmittance(dim, -20.0D);
            if (!(transAero == 1.0F && transCloud < transAero && transCore < 0.05F)) {
                context.fail("Optical transmittance failed to decay with depth on " + dim.location()
                        + ": aero=" + transAero + ", cloud=" + transCloud + ", core=" + transCore);
                return;
            }

            float fogAero = GCGasGiantAtmosphereHandler.computeFogDistanceScale(dim, 216.0D);
            float fogCloud = GCGasGiantAtmosphereHandler.computeFogDistanceScale(dim, 100.0D);
            float fogCore = GCGasGiantAtmosphereHandler.computeFogDistanceScale(dim, -20.0D);
            if (!(fogAero == 1.0F && fogCloud < fogAero && fogCore < fogCloud && fogCore <= 0.30F)) {
                context.fail("Fog distance scale failed to tighten with depth on " + dim.location()
                        + ": aero=" + fogAero + ", cloud=" + fogCloud + ", core=" + fogCore);
                return;
            }

            double windAero = GCGasGiantAtmosphereHandler.computeZonalWindAccel(dim, 216.0D, 50.0D);
            double windCloud = GCGasGiantAtmosphereHandler.computeZonalWindAccel(dim, 100.0D, 50.0D);
            if (windAero != 0.0D || Math.abs(windCloud) < 0.01D) {
                context.fail("Expected calm aerostatic zone (0 wind) and active cloud jet stream on " + dim.location()
                        + ": aero=" + windAero + ", cloud=" + windCloud);
                return;
            }
        }

        // Verify Jupiter > Saturn > Neptune > Uranus pressure hierarchy at cloud and core altitudes
        double pJ = GCGasGiantAtmosphereHandler.computePressureBar(GCDimensions.JUPITER, -20.0D);
        double pS = GCGasGiantAtmosphereHandler.computePressureBar(GCDimensions.SATURN, -20.0D);
        double pN = GCGasGiantAtmosphereHandler.computePressureBar(GCDimensions.NEPTUNE, -20.0D);
        double pU = GCGasGiantAtmosphereHandler.computePressureBar(GCDimensions.URANUS, -20.0D);
        if (!(pJ > pS && pS > pN && pN > pU)) {
            context.fail("Expected core pressure order Jupiter > Saturn > Neptune > Uranus, got: " + pJ + ", " + pS + ", " + pN + ", " + pU);
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testAerostaticPlatformGenerationAndNonDestructiveRevisit(GameTestHelper context) {
        var level = context.getLevel();
        BlockPos absOrigin = context.absolutePos(new BlockPos(4, 0, 4));
        int cx = absOrigin.getX();
        int cz = absOrigin.getZ();
        int py = AerostaticCelestialTeleporterType.PLATFORM_Y;
        BlockPos center = new BlockPos(cx, py, cz);

        // Clear any leftover blocks from prior test runs in this 7x7 column
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    level.setBlock(new BlockPos(cx + dx, py + dy, cz + dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        AerostaticCelestialTeleporterType.generateAerostaticPlatform(level, cx, cz);

        // Verify 7x7 deck composition: center aluminum, inner ring steel, outer rim tin, gantry trusses below, corner torches above
        if (!level.getBlockState(center).is(GCBlocks.ALUMINUM_DECORATION.block())) {
            context.fail("Expected center of aerostatic platform to be ALUMINUM_DECORATION, got: " + level.getBlockState(center));
            return;
        }
        if (!level.getBlockState(center.offset(2, 0, 2)).is(GCBlocks.STEEL_DECORATION.block())) {
            context.fail("Expected inner deck ring to be STEEL_DECORATION, got: " + level.getBlockState(center.offset(2, 0, 2)));
            return;
        }
        if (!level.getBlockState(center.offset(3, 0, 0)).is(GCBlocks.TIN_DECORATION.block())) {
            context.fail("Expected outer rim to be TIN_DECORATION, got: " + level.getBlockState(center.offset(3, 0, 0)));
            return;
        }
        if (!level.getBlockState(center.below()).is(GCBlocks.GANTRY_TRUSS)) {
            context.fail("Expected structural GANTRY_TRUSS beneath platform center");
            return;
        }
        if (!level.getBlockState(center.offset(3, 1, 3)).is(GCBlocks.GLOWSTONE_TORCH)) {
            context.fail("Expected GLOWSTONE_TORCH beacon on platform corner");
            return;
        }

        // Verify revisit is non-destructive (does not overwrite player-built blocks)
        BlockPos playerBlock = center.offset(2, 0, 0);
        level.setBlock(playerBlock, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        AerostaticCelestialTeleporterType.generateAerostaticPlatform(level, cx, cz);
        if (!level.getBlockState(playerBlock).is(Blocks.GOLD_BLOCK)) {
            context.fail("Aerostatic platform regeneration must not overwrite existing player-placed blocks!");
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testAtmosphericFractionatorAltitudeYieldScaling(GameTestHelper context) {
        var saturnAero = AtmosphericFractionatorBlockEntity.resolveAtmosphereProfile(GCDimensions.SATURN, null, 216);
        var saturnCloud = AtmosphericFractionatorBlockEntity.resolveAtmosphereProfile(GCDimensions.SATURN, null, 80);
        var saturnCore = AtmosphericFractionatorBlockEntity.resolveAtmosphereProfile(GCDimensions.SATURN, null, -20);

        long he3Aero = saturnAero.channels().get(1).mbPerCycle();
        long he3Cloud = saturnCloud.channels().get(1).mbPerCycle();
        long he3Core = saturnCore.channels().get(1).mbPerCycle();

        if (he3Aero != 35L) {
            context.fail("Expected Saturn baseline 3He yield = 35 mB at Y=216, got: " + he3Aero);
            return;
        }
        if (!(he3Cloud > he3Aero && he3Core > he3Cloud && he3Core <= 88L)) {
            context.fail("Expected Saturn 3He yield to scale monotonically with depth up to 2.5x: aero=" + he3Aero + ", cloud=" + he3Cloud + ", core=" + he3Core);
            return;
        }
        if (!(saturnCore.pressureMilliAtm() > saturnCloud.pressureMilliAtm() && saturnCloud.pressureMilliAtm() > saturnAero.pressureMilliAtm())) {
            context.fail("Expected fractionator reported ambient pressure to increase with depth!");
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testGasGiantNoiseSettingsHaveEmptyAtmosphereAboveZero(GameTestHelper context) {
        var noiseSettingsRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.NOISE_SETTINGS);
        var expectedDefaults = java.util.Map.of(
                GCNoiseGeneratorSettings.JUPITER, GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK,
                GCNoiseGeneratorSettings.SATURN, GCBlocks.SATURN_HELIUM_MANTLE_ROCK,
                GCNoiseGeneratorSettings.URANUS, GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK,
                GCNoiseGeneratorSettings.NEPTUNE, GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK
        );

        for (var entry : expectedDefaults.entrySet()) {
            NoiseGeneratorSettings settings = noiseSettingsRegistry.get(entry.getKey());
            if (settings == null) {
                context.fail("Missing NoiseGeneratorSettings: " + entry.getKey().location());
                return;
            }
            if (!settings.defaultBlock().is(entry.getValue())) {
                context.fail("Expected defaultBlock of " + entry.getKey().location() + " to be " + entry.getValue() + ", got: " + settings.defaultBlock());
                return;
            }
            DensityFunction finalDensity = settings.noiseRouter().finalDensity();
            for (int y : new int[]{10, 100, 216, 300}) {
                for (int x = -128; x <= 128; x += 64) {
                    double d = finalDensity.compute(new DensityFunction.SinglePointContext(x, y, x));
                    if (d >= 0.0D) {
                        context.fail("Gas giant " + entry.getKey().location() + " produced solid density (" + d + ") above Y=0 at (" + x + "," + y + "," + x + ")!");
                        return;
                    }
                }
            }
            // Verify continuous abyssal mantle floor near bedrock (zero void holes at Y = -60)
            for (int x = -128; x <= 128; x += 64) {
                double floorD = finalDensity.compute(new DensityFunction.SinglePointContext(x, -60, x));
                if (floorD <= 0.0D) {
                    context.fail("Gas giant " + entry.getKey().location() + " has a void hole at Y=-60 (" + x + ", -60, " + x + "): density=" + floorD);
                    return;
                }
            }
        }
        context.succeed();
    }
}
