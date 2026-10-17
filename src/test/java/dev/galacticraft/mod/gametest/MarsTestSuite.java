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
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.SlimelingEntity;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import dev.galacticraft.mod.world.gen.GCDensityFunctions;
import dev.galacticraft.mod.world.gen.GCNoiseData;
import dev.galacticraft.mod.world.gen.carver.GCConfiguredCarvers;
import dev.galacticraft.mod.world.gen.feature.GCFeatures;
import dev.galacticraft.mod.world.gen.feature.GCOrePlacedFeatures;
import dev.galacticraft.mod.world.gen.feature.GCPlacedFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Optional;

public class MarsTestSuite implements GalacticraftGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void marsCelestialBodyAndDimensionTest(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> celestialRegistry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> mars = celestialRegistry.get(GCCelestialBodies.MARS);
        if (mars == null) {
            context.fail("Expected GCCelestialBodies.MARS to be registered in CELESTIAL_BODY registry!");
            return;
        }
        if (!(mars.type() instanceof Landable<?>)) {
            context.fail("Expected Mars celestial body type to implement Landable, found: " + mars.type());
            return;
        }
        if (!(mars.config() instanceof PlanetConfig config)) {
            context.fail("Expected Mars config to be PlanetConfig, found: " + mars.config());
            return;
        }
        if (!GCDimensions.MARS.equals(config.world())) {
            context.fail("Expected Mars target dimension to be GCDimensions.MARS, found: " + config.world());
            return;
        }
        if (config.accessWeight() != 2) {
            context.fail("Expected Mars accessWeight (rocket tier) to be 2, found: " + config.accessWeight());
            return;
        }
        if (Math.abs(config.gravity() - 0.38F) > 1.0E-4F) {
            context.fail("Expected Mars gravity to be 0.38F, found: " + config.gravity());
            return;
        }
        if (config.dayLength() != 24660L) {
            context.fail("Expected Mars day length to be 24660L, found: " + config.dayLength());
            return;
        }
        if (config.atmosphere().breathable()) {
            context.fail("Expected Mars CO2 atmosphere to be unbreathable!");
            return;
        }
        double co2Ppm = config.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.CARBON_DIOXIDE_ID), 0.0);
        if (co2Ppm < 900000.0) {
            context.fail("Expected Mars atmosphere to be dominated by CO2 (>900,000 ppm), found: " + co2Ppm);
            return;
        }

        var registryAccess = context.getLevel().registryAccess();
        boolean marsDimTypeRegistered = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE).containsKey(Constant.id("mars"));
        var biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        boolean marsBiomesRegistered = biomeRegistry.containsKey(Constant.id("mars_flat"))
                && biomeRegistry.containsKey(Constant.id("mars_canyon"))
                && biomeRegistry.containsKey(Constant.id("mars_highlands"));
        if (!marsDimTypeRegistered) {
            context.fail("Expected Mars dimension type to be registered in server registries!");
            return;
        }
        if (!marsBiomesRegistered) {
            context.fail("Expected Mars biomes (mars_flat, mars_canyon, mars_highlands) to be registered in server registries!");
            return;
        }

        var densityRegistry = registryAccess.registryOrThrow(Registries.DENSITY_FUNCTION);
        if (!densityRegistry.containsKey(GCDensityFunctions.Mars.BASE_HEIGHT)
                || !densityRegistry.containsKey(GCDensityFunctions.Mars.OFFSET)
                || !densityRegistry.containsKey(GCDensityFunctions.Mars.FACTOR)
                || !densityRegistry.containsKey(GCDensityFunctions.Mars.SMOOTH_RELIEF)
                || !densityRegistry.containsKey(GCDensityFunctions.Mars.FINAL_DENSITY)) {
            context.fail("Expected all Mars density functions (BASE_HEIGHT, OFFSET, FACTOR, SMOOTH_RELIEF, FINAL_DENSITY) to be registered!");
            return;
        }

        var noiseRegistry = registryAccess.registryOrThrow(Registries.NOISE);
        if (!noiseRegistry.containsKey(GCNoiseData.MARS_BASE_NOISE)
                || !noiseRegistry.containsKey(GCNoiseData.MARS_HILL_NOISE)
                || !noiseRegistry.containsKey(GCNoiseData.MARS_VALLEY_NOISE)
                || !noiseRegistry.containsKey(GCNoiseData.MARS_WE_TERRAIN_NOISE)) {
            context.fail("Expected all Mars noise parameters to be registered!");
            return;
        }

        var carverRegistry = registryAccess.registryOrThrow(Registries.CONFIGURED_CARVER);
        if (!carverRegistry.containsKey(GCConfiguredCarvers.MARS_CAVERN_CARVER)) {
            context.fail("Expected MARS_CAVERN_CARVER to be registered in CONFIGURED_CARVER registry!");
            return;
        }

        var placedFeatureRegistry = registryAccess.registryOrThrow(Registries.PLACED_FEATURE);
        if (!placedFeatureRegistry.containsKey(GCPlacedFeatures.MARS_CAVERN_VINES)
                || !placedFeatureRegistry.containsKey(GCOrePlacedFeatures.ORE_SUB_SURFACE_MARS)) {
            context.fail("Expected MARS_CAVERN_VINES and ORE_SUB_SURFACE_MARS placed features to be registered!");
            return;
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void marsCavernVinesFeatureTest(GameTestHelper context) {
        BlockPos ceilingRelative = new BlockPos(2, 6, 2);
        context.setBlock(ceilingRelative, GCBlocks.MARS_STONE.defaultBlockState());
        for (int dy = 1; dy <= 5; dy++) {
            context.setBlock(ceilingRelative.below(dy), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        context.setBlock(ceilingRelative.below(6), GCBlocks.MARS_STONE.defaultBlockState());

        BlockPos worldStartPos = context.absolutePos(ceilingRelative.below());
        if (worldStartPos.getY() <= 65) {
            boolean placed = GCFeatures.MARS_CAVERN_VINES.place(new FeaturePlaceContext<>(
                    Optional.empty(),
                    context.getLevel(),
                    context.getLevel().getChunkSource().getGenerator(),
                    RandomSource.create(42L),
                    worldStartPos,
                    NoneFeatureConfiguration.INSTANCE
            ));
            if (!placed) {
                context.fail("Expected MarsCavernVinesFeature to place hanging vines beneath MARS_STONE cavern ceiling!");
                return;
            }
            var topVineState = context.getBlockState(ceilingRelative.below());
            if (!topVineState.is(GCBlocks.CAVERNOUS_VINES) && !topVineState.is(GCBlocks.CAVERNOUS_VINES_PLANT)) {
                context.fail("Expected CAVERNOUS_VINES or CAVERNOUS_VINES_PLANT below ceiling, found: " + topVineState);
                return;
            }
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void slimelingGeneticsAndTamingTest(GameTestHelper context) {
        SlimelingEntity parentA = context.spawn(GCEntityTypes.SLIMELING, new BlockPos(0, 1, 0));
        SlimelingEntity parentB = context.spawn(GCEntityTypes.SLIMELING, new BlockPos(0, 1, 0));

        if (!parentA.canBreatheInSpace()) {
            context.fail("Expected SlimelingEntity to have CAN_BREATHE_IN_SPACE attribute!");
            return;
        }

        parentA.setColor(1.0F, 0.0F, 0.0F);
        parentA.setSlimelingAge(50000);
        parentA.setKillCount(7);
        parentA.setTame(true, true);

        if (parentA.getMaxHealth() < 34.9F) {
            context.fail("Expected tamed half-grown Slimeling maxHealth >= 35.0, got: " + parentA.getMaxHealth());
            return;
        }

        CompoundTag saved = new CompoundTag();
        parentA.addAdditionalSaveData(saved);

        parentB.readAdditionalSaveData(saved);
        if (Math.abs(parentB.getColorRed() - 1.0F) > 1.0E-4F || parentB.getSlimelingAge() != 50000 || parentB.getKillCount() != 7) {
            context.fail("Expected Slimeling NBT round-trip to preserve RGB color, age, and kill count!");
            return;
        }

        parentB.setColor(0.0F, 0.0F, 1.0F);
        AgeableMob childMob = parentA.getBreedOffspring(context.getLevel(), parentB);
        if (!(childMob instanceof SlimelingEntity child)) {
            context.fail("Expected Slimeling getBreedOffspring to return SlimelingEntity!");
            return;
        }
        if (Math.abs(child.getColorRed() - 0.5F) > 1.0E-4F || Math.abs(child.getColorBlue() - 0.5F) > 1.0E-4F) {
            context.fail(String.format("Expected child Slimeling blended RGB (0.5, 0.0, 0.5), got (%f, %f, %f)",
                    child.getColorRed(), child.getColorGreen(), child.getColorBlue()));
            return;
        }

        parentA.discard();
        parentB.discard();
        child.discard();
        context.succeed();
    }
}
