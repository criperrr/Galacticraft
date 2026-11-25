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

import dev.galacticraft.api.registry.AddonRegistries;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.api.universe.celestialbody.landable.Landable;
import dev.galacticraft.impl.universe.celestialbody.config.PlanetConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.world.biome.GCBiomes;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import dev.galacticraft.mod.world.gen.GCDensityFunctions;
import dev.galacticraft.mod.world.gen.GCNoiseGeneratorSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.List;

/**
 * Headless GameTest suite validating Phase 3 Inner &amp; Jovian Moons:
 * Phobos, Deimos (Mars) and Io, Europa, Ganymede, Callisto (Jupiter).
 */
public class InnerAndJovianMoonsTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void testPhobosAndDeimosAstrophysics(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        assertMoonParams(context, registry, GCCelestialBodies.PHOBOS, GCCelestialBodies.MARS, GCDimensions.PHOBOS, 0.015F, 7654L, 2, -4, -112);
        assertMoonParams(context, registry, GCCelestialBodies.DEIMOS, GCCelestialBodies.MARS, GCDimensions.DEIMOS, 0.012F, 30312L, 2, -20, -120);
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testGalileanMoonsAstrophysics(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        assertMoonParams(context, registry, GCCelestialBodies.IO, GCCelestialBodies.JUPITER, GCDimensions.IO, 0.183F, 42460L, 4, -130, -163);
        assertMoonParams(context, registry, GCCelestialBodies.EUROPA, GCCelestialBodies.JUPITER, GCDimensions.EUROPA, 0.134F, 85230L, 4, -160, -220);
        assertMoonParams(context, registry, GCCelestialBodies.GANYMEDE, GCCelestialBodies.JUPITER, GCDimensions.GANYMEDE, 0.146F, 171710L, 4, -113, -193);
        assertMoonParams(context, registry, GCCelestialBodies.CALLISTO, GCCelestialBodies.JUPITER, GCDimensions.CALLISTO, 0.126F, 400540L, 4, -108, -193);
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testMoonDimensionTypesAndBounds(GameTestHelper context) {
        var dimTypeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        for (String id : List.of("phobos", "deimos", "io", "europa", "ganymede", "callisto")) {
            DimensionType type = dimTypeRegistry.get(Constant.id(id));
            if (type == null) {
                context.fail("Expected DimensionType to be registered: " + id);
                return;
            }
            if (type.minY() != -64 || type.height() != 384 || type.logicalHeight() != 384) {
                context.fail("Expected vertical bounds [-64, 320] (height 384) for " + id);
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testAllPhase3MoonBiomesRegistered(GameTestHelper context) {
        var biomeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceKey<Biome>> expected = List.of(
                GCBiomes.Phobos.STICKNEY_BASIN,
                GCBiomes.Phobos.TIDAL_GROOVES,
                GCBiomes.Phobos.REGOLITH_HIGHLANDS,
                GCBiomes.Phobos.FRACTURE_CAVERNS,
                GCBiomes.Deimos.SMOOTH_PLAINS,
                GCBiomes.Deimos.CRATER_SLOPES,
                GCBiomes.Deimos.CORE_CAVERNS,
                GCBiomes.Io.SULFUR_PLAINS,
                GCBiomes.Io.VOLCANIC_CALDERA,
                GCBiomes.Io.SILICATE_FLOWS,
                GCBiomes.Io.MAGMA_TUBES,
                GCBiomes.Europa.ICE_PLAINS,
                GCBiomes.Europa.LINEAE_RIDGES,
                GCBiomes.Europa.CHAOS_TERRAIN,
                GCBiomes.Europa.SUBSURFACE_OCEAN,
                GCBiomes.Ganymede.SULCI_RIDGES,
                GCBiomes.Ganymede.DARK_REGIO,
                GCBiomes.Ganymede.POLAR_FROST,
                GCBiomes.Ganymede.ICE_CAVERNS,
                GCBiomes.Callisto.CRATERED_PLAINS,
                GCBiomes.Callisto.VALHALLA_RINGS,
                GCBiomes.Callisto.ICE_PEAKS,
                GCBiomes.Callisto.ANCIENT_CAVERNS
        );
        for (var key : expected) {
            if (!biomeRegistry.containsKey(key)) {
                context.fail("Missing registered moon biome: " + key.location());
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testEuropaSubsurfaceOceanAndIoVolcanicNoiseSettings(GameTestHelper context) {
        var noiseSettingsRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.NOISE_SETTINGS);

        NoiseGeneratorSettings europa = noiseSettingsRegistry.get(GCNoiseGeneratorSettings.EUROPA);
        if (europa == null) {
            context.fail("Missing NoiseGeneratorSettings for Europa!");
            return;
        }
        if (europa.seaLevel() != 24 || !europa.defaultFluid().is(Blocks.WATER)) {
            context.fail("Expected Europa subsurface ocean seaLevel=24 with WATER, found seaLevel=" + europa.seaLevel() + ", fluid=" + europa.defaultFluid());
            return;
        }

        NoiseGeneratorSettings io = noiseSettingsRegistry.get(GCNoiseGeneratorSettings.IO);
        if (io == null) {
            context.fail("Missing NoiseGeneratorSettings for Io!");
            return;
        }
        if (!io.defaultFluid().is(Blocks.LAVA)) {
            context.fail("Expected Io default fluid to be LAVA for magma tubes, found: " + io.defaultFluid());
            return;
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testMoonDensityFunctionsAndPlanetaryEnvironments(GameTestHelper context) {
        var densityRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);
        List<ResourceKey<DensityFunction>> keys = List.of(
                GCDensityFunctions.Phobos.SOLID_TERRAIN, GCDensityFunctions.Phobos.FINAL_DENSITY,
                GCDensityFunctions.Deimos.SOLID_TERRAIN, GCDensityFunctions.Deimos.FINAL_DENSITY,
                GCDensityFunctions.Io.SOLID_TERRAIN, GCDensityFunctions.Io.FINAL_DENSITY,
                GCDensityFunctions.Europa.SOLID_TERRAIN, GCDensityFunctions.Europa.FINAL_DENSITY,
                GCDensityFunctions.Ganymede.SOLID_TERRAIN, GCDensityFunctions.Ganymede.FINAL_DENSITY,
                GCDensityFunctions.Callisto.SOLID_TERRAIN, GCDensityFunctions.Callisto.FINAL_DENSITY
        );
        for (var key : keys) {
            if (!densityRegistry.containsKey(key)) {
                context.fail("Missing moon DensityFunction: " + key.location());
                return;
            }
        }

        // Physical radiation hierarchy in Jovian magnetosphere: Io > Europa > Ganymede > Callisto
        if (!(PlanetaryEnvironment.IO.radiationFlux() > PlanetaryEnvironment.EUROPA.radiationFlux()
                && PlanetaryEnvironment.EUROPA.radiationFlux() > PlanetaryEnvironment.GANYMEDE.radiationFlux()
                && PlanetaryEnvironment.GANYMEDE.radiationFlux() > PlanetaryEnvironment.CALLISTO.radiationFlux())) {
            context.fail("Expected Jovian radiation gradient Io > Europa > Ganymede > Callisto!");
            return;
        }
        context.succeed();
    }

    private static void assertMoonParams(
            GameTestHelper context,
            Registry<CelestialBody<?, ?>> registry,
            ResourceKey<CelestialBody<?, ?>> moonKey,
            ResourceKey<CelestialBody<?, ?>> expectedParent,
            ResourceKey<Level> expectedWorld,
            float expectedGravity,
            long expectedDayLength,
            int expectedTier,
            int expectedDayTemp,
            int expectedNightTemp
    ) {
        CelestialBody<?, ?> body = registry.get(moonKey);
        if (body == null || !(body.type() instanceof Landable<?>) || !(body.config() instanceof PlanetConfig config)) {
            context.fail("Moon celestial body missing or invalid: " + moonKey.location());
            return;
        }
        if (config.parent().isEmpty() || !config.parent().get().equals(expectedParent)) {
            context.fail("Expected parent " + expectedParent.location() + " for " + moonKey.location() + ", found " + config.parent());
            return;
        }
        if (!expectedWorld.equals(config.world())) {
            context.fail("Expected world " + expectedWorld.location() + " for " + moonKey.location());
            return;
        }
        if (Math.abs(config.gravity() - expectedGravity) > 1.0E-4F) {
            context.fail("Expected gravity " + expectedGravity + " for " + moonKey.location() + ", found " + config.gravity());
            return;
        }
        if (config.dayLength() != expectedDayLength) {
            context.fail("Expected dayLength " + expectedDayLength + " for " + moonKey.location() + ", found " + config.dayLength());
            return;
        }
        if (config.accessWeight() != expectedTier) {
            context.fail("Expected tier " + expectedTier + " for " + moonKey.location() + ", found " + config.accessWeight());
            return;
        }
        if (config.dayTemperature() != expectedDayTemp || config.nightTemperature() != expectedNightTemp) {
            context.fail("Unexpected day/night temperature for " + moonKey.location() + ": " + config.dayTemperature() + "/" + config.nightTemperature());
        }
    }
}
