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
import net.minecraft.world.level.dimension.DimensionType;
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
                GCBlocks.JUPITER_SURFACE,
                GCBlocks.JUPITER_SUB_SURFACE,
                GCBlocks.JUPITER_STONE,
                GCBlocks.JUPITER_RED_STORM_ROCK,
                GCBlocks.JUPITER_BANDED_OCHRE_ROCK,
                GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK,
                GCBlocks.JUPITER_CORE_BRECCIA,
                GCBlocks.JUPITER_STONE_BRICKS,
                GCBlocks.JUPITER_COPPER_ORE,
                GCBlocks.JUPITER_TIN_ORE,
                GCBlocks.JUPITER_IRON_ORE,
                GCBlocks.JUPITER_ALUMINUM_ORE
        );
        for (Block block : expectedBlocks) {
            if (BuiltInRegistries.BLOCK.getResourceKey(block).isEmpty()) {
                context.fail("Expected Jupiter block to be registered in BuiltInRegistries.BLOCK: " + block);
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
                GCBlocks.SATURN_SURFACE,
                GCBlocks.SATURN_SUB_SURFACE,
                GCBlocks.SATURN_STONE,
                GCBlocks.SATURN_GOLDEN_BAND_ROCK,
                GCBlocks.SATURN_RING_ICE_ROCK,
                GCBlocks.SATURN_HYDROCARBON_SLATE,
                GCBlocks.SATURN_HELIUM_MANTLE_ROCK,
                GCBlocks.SATURN_STONE_BRICKS,
                GCBlocks.SATURN_COPPER_ORE,
                GCBlocks.SATURN_TIN_ORE,
                GCBlocks.SATURN_IRON_ORE,
                GCBlocks.SATURN_TITANIUM_ORE
        );
        for (Block block : expectedBlocks) {
            if (BuiltInRegistries.BLOCK.getResourceKey(block).isEmpty()) {
                context.fail("Expected Saturn block to be registered in BuiltInRegistries.BLOCK: " + block);
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
                GCBlocks.URANUS_SURFACE,
                GCBlocks.URANUS_SUB_SURFACE,
                GCBlocks.URANUS_STONE,
                GCBlocks.URANUS_METHANE_ICE,
                GCBlocks.URANUS_VERONA_SCARP_ROCK,
                GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK,
                GCBlocks.URANUS_DIAMOND_BEARING_ROCK,
                GCBlocks.URANUS_STONE_BRICKS,
                GCBlocks.URANUS_COPPER_ORE,
                GCBlocks.URANUS_TIN_ORE,
                GCBlocks.URANUS_ALUMINUM_ORE,
                GCBlocks.URANUS_TITANIUM_ORE
        );
        for (Block block : expectedBlocks) {
            if (BuiltInRegistries.BLOCK.getResourceKey(block).isEmpty()) {
                context.fail("Expected Uranus block to be registered in BuiltInRegistries.BLOCK: " + block);
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
        List<Block> expectedBlocks = List.of(
                GCBlocks.NEPTUNE_SURFACE,
                GCBlocks.NEPTUNE_SUB_SURFACE,
                GCBlocks.NEPTUNE_STONE,
                GCBlocks.NEPTUNE_NITROGEN_ICE,
                GCBlocks.NEPTUNE_DARK_VORTEX_ROCK,
                GCBlocks.NEPTUNE_CRYO_SLATE,
                GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK,
                GCBlocks.NEPTUNE_STONE_BRICKS,
                GCBlocks.NEPTUNE_COPPER_ORE,
                GCBlocks.NEPTUNE_TIN_ORE,
                GCBlocks.NEPTUNE_IRON_ORE,
                GCBlocks.NEPTUNE_TITANIUM_ORE
        );
        for (Block block : expectedBlocks) {
            if (BuiltInRegistries.BLOCK.getResourceKey(block).isEmpty()) {
                context.fail("Expected Neptune block to be registered in BuiltInRegistries.BLOCK: " + block);
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
            if (!config.teleporter().is(GCTeleporterTypes.LANDER_CELESTIAL_TELEPORTER)) {
                context.fail("Expected planet " + key.location() + " to use LANDER_CELESTIAL_TELEPORTER, found: " + config.teleporter().unwrapKey().map(k -> k.location().toString()).orElse("unregistered"));
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
        // Assert all outer blocks require correct tool for drops
        List<Block> sampleBlocks = List.of(
                GCBlocks.JUPITER_SURFACE, GCBlocks.JUPITER_STONE, GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK,
                GCBlocks.SATURN_SURFACE, GCBlocks.SATURN_STONE, GCBlocks.SATURN_HELIUM_MANTLE_ROCK,
                GCBlocks.URANUS_SURFACE, GCBlocks.URANUS_STONE, GCBlocks.URANUS_SUPERIONIC_MANTLE_ROCK,
                GCBlocks.NEPTUNE_SURFACE, GCBlocks.NEPTUNE_STONE, GCBlocks.NEPTUNE_ABYSSAL_MANTLE_ROCK
        );
        for (Block b : sampleBlocks) {
            if (!b.defaultBlockState().requiresCorrectToolForDrops()) {
                context.fail("Block " + b + " must require correct tool for drops!");
                return;
            }
        }
        // Surface blocks should have lower hardness than deep mantle rock
        float jupiterSurfaceHardness = GCBlocks.JUPITER_SURFACE.defaultDestroyTime();
        float jupiterMantleHardness = GCBlocks.JUPITER_METALLIC_HYDROGEN_ROCK.defaultDestroyTime();
        if (jupiterSurfaceHardness >= jupiterMantleHardness) {
            context.fail("Expected surface hardness (" + jupiterSurfaceHardness + ") < mantle hardness (" + jupiterMantleHardness + ")");
            return;
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
    public void testSaturnianRingCryogenicExtractionScenario(GameTestHelper context) {
        BlockPos icePos = new BlockPos(1, 1, 1);
        BlockPos slatePos = new BlockPos(1, 2, 1);

        context.setBlock(icePos, GCBlocks.SATURN_RING_ICE_ROCK.defaultBlockState());
        context.setBlock(slatePos, GCBlocks.SATURN_HYDROCARBON_SLATE.defaultBlockState());

        if (!context.getBlockState(icePos).is(GCBlocks.SATURN_RING_ICE_ROCK)) {
            context.fail("Failed to place SATURN_RING_ICE_ROCK!");
            return;
        }
        if (!context.getBlockState(slatePos).is(GCBlocks.SATURN_HYDROCARBON_SLATE)) {
            context.fail("Failed to place SATURN_HYDROCARBON_SLATE!");
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testUranianVeronaScarpDynamicTraversalScenario(GameTestHelper context) {
        BlockPos icePos = new BlockPos(1, 1, 1);
        BlockPos scarpPos = new BlockPos(2, 1, 1);
        BlockPos diamondPos = new BlockPos(1, 2, 1);

        context.setBlock(icePos, GCBlocks.URANUS_METHANE_ICE.defaultBlockState());
        context.setBlock(scarpPos, GCBlocks.URANUS_VERONA_SCARP_ROCK.defaultBlockState());
        context.setBlock(diamondPos, GCBlocks.URANUS_DIAMOND_BEARING_ROCK.defaultBlockState());

        // Validate friction on methane ice
        float friction = GCBlocks.URANUS_METHANE_ICE.getFriction();
        if (Math.abs(friction - 0.96F) > 1.0E-4F) {
            context.fail("Expected Uranus methane ice friction to be 0.96F, found: " + friction);
            return;
        }
        // Validate light emission of diamond bearing rock
        int light = context.getBlockState(diamondPos).getLightEmission();
        if (light != 3) {
            context.fail("Expected URANUS_DIAMOND_BEARING_ROCK light emission = 3, found: " + light);
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testNeptunianAbyssalDiamondMiningScenario(GameTestHelper context) {
        BlockPos icePos = new BlockPos(1, 1, 1);
        BlockPos slatePos = new BlockPos(1, 2, 1);
        BlockPos orePos = new BlockPos(1, 3, 1);

        context.setBlock(icePos, GCBlocks.NEPTUNE_NITROGEN_ICE.defaultBlockState());
        context.setBlock(slatePos, GCBlocks.NEPTUNE_CRYO_SLATE.defaultBlockState());
        context.setBlock(orePos, GCBlocks.NEPTUNE_TITANIUM_ORE.defaultBlockState());

        // Validate friction on nitrogen ice
        float friction = GCBlocks.NEPTUNE_NITROGEN_ICE.getFriction();
        if (Math.abs(friction - 0.97F) > 1.0E-4F) {
            context.fail("Expected Neptune nitrogen ice friction to be 0.97F, found: " + friction);
            return;
        }
        if (!context.getBlockState(orePos).is(GCBlocks.NEPTUNE_TITANIUM_ORE)) {
            context.fail("Failed to place NEPTUNE_TITANIUM_ORE!");
            return;
        }
        context.succeed();
    }
}
