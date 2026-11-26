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
import dev.galacticraft.mod.content.block.entity.machine.AtmosphericFractionatorBlockEntity;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.manual.ManualCatalog;
import dev.galacticraft.mod.research.ResearchCatalog;
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
 * Headless GameTest suite validating Phase 4 Outer Moons (Titan, Enceladus, Titania, Oberon, Triton)
 * and final integration with ResearchCatalog, AtmosphericFractionator, PlanetaryEnvironment, and ManualCatalog.
 */
public class OuterMoonsAndIntegrationTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void testOuterMoonsAstrophysics(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> registry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        assertMoonParams(context, registry, GCCelestialBodies.TITAN, GCCelestialBodies.SATURN, GCDimensions.TITAN, 0.138F, 382690L, 5, -179, -180, 1.45F);
        assertMoonParams(context, registry, GCCelestialBodies.ENCELADUS, GCCelestialBodies.SATURN, GCDimensions.ENCELADUS, 0.012F, 32890L, 5, -198, -201, 0.0F);
        assertMoonParams(context, registry, GCCelestialBodies.TITANIA, GCCelestialBodies.URANUS, GCDimensions.TITANIA, 0.039F, 208940L, 6, -184, -213, 0.0F);
        assertMoonParams(context, registry, GCCelestialBodies.OBERON, GCCelestialBodies.URANUS, GCDimensions.OBERON, 0.035F, 323120L, 6, -193, -203, 0.0F);
        assertMoonParams(context, registry, GCCelestialBodies.TRITON, GCCelestialBodies.NEPTUNE, GCDimensions.TRITON, 0.079F, 141050L, 7, -235, -238, 0.000014F);
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testOuterMoonDimensionBoundsAndBiomes(GameTestHelper context) {
        var dimTypeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        for (String id : List.of("titan", "enceladus", "titania", "oberon", "triton")) {
            DimensionType type = dimTypeRegistry.get(Constant.id(id));
            if (type == null || type.minY() != -64 || type.height() != 384 || type.logicalHeight() != 384) {
                context.fail("Invalid or missing DimensionType for outer moon: " + id);
                return;
            }
        }

        var biomeRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        List<ResourceKey<Biome>> expected = List.of(
                GCBiomes.Titan.METHANE_SEAS, GCBiomes.Titan.THOLIN_DUNES, GCBiomes.Titan.ICE_HIGHLANDS, GCBiomes.Titan.CRYO_CAVERNS,
                GCBiomes.Enceladus.SNOW_PLAINS, GCBiomes.Enceladus.TIGER_STRIPES, GCBiomes.Enceladus.CRATERED_RIDGES, GCBiomes.Enceladus.HYDROTHERMAL_OCEAN,
                GCBiomes.Titania.MESSINA_CHASMATA, GCBiomes.Titania.FROST_PLAINS, GCBiomes.Titania.SCARP_HIGHLANDS, GCBiomes.Titania.FAULT_CAVERNS,
                GCBiomes.Oberon.RED_PLAINS, GCBiomes.Oberon.IMPACT_PEAKS, GCBiomes.Oberon.DARK_BASINS, GCBiomes.Oberon.ANCIENT_CAVERNS,
                GCBiomes.Triton.CANTALOUPE_TERRAIN, GCBiomes.Triton.NITROGEN_GEYSERS, GCBiomes.Triton.POLAR_ICE_CAP, GCBiomes.Triton.SUPERIONIC_CAVERNS
        );
        for (var key : expected) {
            if (!biomeRegistry.containsKey(key)) {
                context.fail("Missing registered outer moon biome: " + key.location());
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testTitanMethaneLakesAndEnceladusOceanNoiseSettings(GameTestHelper context) {
        var noiseRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.NOISE_SETTINGS);

        NoiseGeneratorSettings titan = noiseRegistry.get(GCNoiseGeneratorSettings.TITAN);
        if (titan == null || titan.seaLevel() != 62 || !titan.defaultFluid().is(GCBlocks.LIQUID_METHANE)
                || !titan.defaultBlock().is(GCBlocks.TITAN_WATER_ICE_ROCK)) {
            context.fail("Expected Titan to have seaLevel=62 with LIQUID_METHANE and TITAN_WATER_ICE_ROCK!");
            return;
        }

        NoiseGeneratorSettings enceladus = noiseRegistry.get(GCNoiseGeneratorSettings.ENCELADUS);
        if (enceladus == null || enceladus.seaLevel() != 20 || !enceladus.defaultFluid().is(Blocks.WATER)
                || !enceladus.defaultBlock().is(GCBlocks.ENCELADUS_ICE_CRUST)) {
            context.fail("Expected Enceladus to have hydrothermal ocean seaLevel=20 with WATER and ENCELADUS_ICE_CRUST!");
            return;
        }

        if (!noiseRegistry.get(GCNoiseGeneratorSettings.TITANIA).defaultBlock().is(GCBlocks.TITANIA_ICE_SILICATE)
                || !noiseRegistry.get(GCNoiseGeneratorSettings.OBERON).defaultBlock().is(GCBlocks.OBERON_DARK_ROCK)
                || !noiseRegistry.get(GCNoiseGeneratorSettings.TRITON).defaultBlock().is(GCBlocks.TRITON_NITROGEN_ICE)) {
            context.fail("Expected Titania, Oberon, and Triton to use their dedicated default rock/ice blocks!");
            return;
        }

        var densityRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);
        for (ResourceKey<DensityFunction> key : List.of(
                GCDensityFunctions.Titan.SOLID_TERRAIN, GCDensityFunctions.Titan.FINAL_DENSITY,
                GCDensityFunctions.Enceladus.SOLID_TERRAIN, GCDensityFunctions.Enceladus.FINAL_DENSITY,
                GCDensityFunctions.Titania.SOLID_TERRAIN, GCDensityFunctions.Titania.FINAL_DENSITY,
                GCDensityFunctions.Oberon.SOLID_TERRAIN, GCDensityFunctions.Oberon.FINAL_DENSITY,
                GCDensityFunctions.Triton.SOLID_TERRAIN, GCDensityFunctions.Triton.FINAL_DENSITY
        )) {
            if (!densityRegistry.containsKey(key)) {
                context.fail("Missing outer moon DensityFunction: " + key.location());
                return;
            }
        }
        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void testTitanFractionatorAndResearchCatalogEquivalents(GameTestHelper context) {
        // 1. Titan 1.45 bar atmosphere yields N2 + CH4 in AtmosphericFractionator
        var titanProfile = AtmosphericFractionatorBlockEntity.resolveAtmosphereProfile(GCDimensions.TITAN, null, 70);
        if (titanProfile.planetCode() != AtmosphericFractionatorBlockEntity.PLANET_TITAN
                || titanProfile.pressureMilliAtm() != 1450
                || titanProfile.channels().size() < 2
                || !titanProfile.channels().get(0).fluid().equals(Gases.NITROGEN)
                || !titanProfile.channels().get(1).fluid().equals(Gases.METHANE)) {
            context.fail("Expected Titan AtmosphericFractionator profile with 1450 mAtm and N2/CH4 primary channels!");
            return;
        }

        // Vacuum outer moons report PLANET_VACUUM
        for (var vacuumMoon : List.of(GCDimensions.ENCELADUS, GCDimensions.TITANIA, GCDimensions.OBERON, GCDimensions.TRITON)) {
            var p = AtmosphericFractionatorBlockEntity.resolveAtmosphereProfile(vacuumMoon, null, 70);
            if (p.planetCode() != AtmosphericFractionatorBlockEntity.PLANET_VACUUM || !p.channels().isEmpty()) {
                context.fail("Expected vacuum fractionator profile on " + vacuumMoon.location());
                return;
            }
        }

        // 2. ResearchCatalog accepts equivalent moons for outer nodes
        var jupNode = ResearchCatalog.get(ResearchCatalog.JUPITER_PALLADIUM_CATALYSIS);
        var satNode = ResearchCatalog.get(ResearchCatalog.SATURN_HELIUM3_HARVESTING);
        var uraNode = ResearchCatalog.get(ResearchCatalog.URANUS_LONSDALEITE_PRESSURE);
        var nepNode = ResearchCatalog.get(ResearchCatalog.NEPTUNE_SUPERIONIC_MAGNETOPLASMA);
        if (jupNode == null || !jupNode.locationRequirement().allowedBodies().contains(Constant.id("io"))
                || !jupNode.locationRequirement().allowedBodies().contains(Constant.id("ganymede"))) {
            context.fail("Expected JUPITER_PALLADIUM_CATALYSIS to accept Io and Ganymede!");
            return;
        }
        if (satNode == null || !satNode.locationRequirement().allowedBodies().contains(GCCelestialBodies.TITAN.location())
                || !satNode.locationRequirement().allowedBodies().contains(GCCelestialBodies.ENCELADUS.location())) {
            context.fail("Expected SATURN_HELIUM3_HARVESTING to accept Titan and Enceladus!");
            return;
        }
        if (uraNode == null || !uraNode.locationRequirement().allowedBodies().contains(GCCelestialBodies.TITANIA.location())
                || !uraNode.locationRequirement().allowedBodies().contains(GCCelestialBodies.OBERON.location())) {
            context.fail("Expected URANUS_LONSDALEITE_PRESSURE to accept Titania and Oberon!");
            return;
        }
        if (nepNode == null || !nepNode.locationRequirement().allowedBodies().contains(GCCelestialBodies.TRITON.location())) {
            context.fail("Expected NEPTUNE_SUPERIONIC_MAGNETOPLASMA to accept Triton!");
            return;
        }

        // 3. ManualCatalog includes all 11 moons + Asteroid Taxonomy + Gas Giant Aerostatic Engineering
        var manualIds = ManualCatalog.builtInEntries().stream().map(e -> e.id()).toList();
        for (var expectedId : List.of(
                ManualCatalog.ENTRY_ATLAS_PHOBOS, ManualCatalog.ENTRY_ATLAS_DEIMOS,
                ManualCatalog.ENTRY_ATLAS_IO, ManualCatalog.ENTRY_ATLAS_EUROPA,
                ManualCatalog.ENTRY_ATLAS_GANYMEDE, ManualCatalog.ENTRY_ATLAS_CALLISTO,
                ManualCatalog.ENTRY_ATLAS_TITAN, ManualCatalog.ENTRY_ATLAS_ENCELADUS,
                ManualCatalog.ENTRY_ATLAS_TITANIA, ManualCatalog.ENTRY_ATLAS_OBERON,
                ManualCatalog.ENTRY_ATLAS_TRITON,
                ManualCatalog.ENTRY_GUIDE_ASTEROID_TAXONOMY, ManualCatalog.ENTRY_GUIDE_AEROSTATIC_GIANTS
        )) {
            if (!manualIds.contains(expectedId)) {
                context.fail("Missing ManualCatalog entry: " + expectedId);
                return;
            }
        }

        // 4. PlanetaryEnvironment presets
        if (PlanetaryEnvironment.byId(GCCelestialBodies.TITAN.location()) == null
                || PlanetaryEnvironment.byId(GCCelestialBodies.TRITON.location()) == null) {
            context.fail("Expected PlanetaryEnvironment presets for Titan and Triton!");
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
            int expectedNightTemp,
            float expectedPressure
    ) {
        CelestialBody<?, ?> body = registry.get(moonKey);
        if (body == null || !(body.type() instanceof Landable<?>) || !(body.config() instanceof PlanetConfig config)) {
            context.fail("Outer moon celestial body missing or invalid: " + moonKey.location());
            return;
        }
        if (config.parent().isEmpty() || !config.parent().get().equals(expectedParent)) {
            context.fail("Expected parent " + expectedParent.location() + " for " + moonKey.location());
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
        if (config.dayLength() != expectedDayLength || config.accessWeight() != expectedTier) {
            context.fail("Unexpected dayLength or tier for " + moonKey.location());
            return;
        }
        if (config.dayTemperature() != expectedDayTemp || config.nightTemperature() != expectedNightTemp) {
            context.fail("Unexpected temperature for " + moonKey.location());
            return;
        }
        if (Math.abs(config.atmosphere().pressure() - expectedPressure) > 1.0E-5F) {
            context.fail("Expected pressure " + expectedPressure + " for " + moonKey.location() + ", found " + config.atmosphere().pressure());
        }
    }
}
