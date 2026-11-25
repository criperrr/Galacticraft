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
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.block.entity.machine.ShortRangeTelepadBlockEntity;
import dev.galacticraft.mod.content.entity.GrappleHookEntity;
import dev.galacticraft.mod.content.entity.SlimelingEntity;
import dev.galacticraft.mod.events.GCEventHandlers;
import dev.galacticraft.mod.world.biome.GCBiomes;
import dev.galacticraft.mod.world.biome.source.GCMultiNoiseBiomeSourceParameterLists;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import dev.galacticraft.mod.world.gen.custom.AsteroidChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

public class AsteroidsTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void asteroidsCelestialBodyAndZeroGTest(GameTestHelper helper) {
        Registry<CelestialBody<?, ?>> registry = helper.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> asteroid = registry.get(GCCelestialBodies.ASTEROID);
        if (asteroid == null) {
            throw new GameTestAssertException("Asteroids celestial body was not registered!");
        }

        if (!(asteroid.type() instanceof Landable<?>)) {
            throw new GameTestAssertException("Asteroids celestial body must be a Landable type, but was: " + asteroid.type().getClass().getName());
        }

        if (!(asteroid.config() instanceof PlanetConfig config)) {
            throw new GameTestAssertException("Expected Asteroids config to be PlanetConfig, found: " + asteroid.config());
        }
        if (!GCDimensions.ASTEROID.equals(config.world())) {
            throw new GameTestAssertException("Asteroids world key mismatch!");
        }

        if (Math.abs(asteroid.gravity() - 0.0F) > 0.001F) {
            throw new GameTestAssertException("Expected Asteroids gravity ~0.0F (Zero-G), got: " + asteroid.gravity());
        }

        if (asteroid.atmosphere().breathable()) {
            throw new GameTestAssertException("Asteroids atmosphere should not be breathable!");
        }

        if (asteroid.atmosphere().pressure() != 0.0F) {
            throw new GameTestAssertException("Asteroids atmosphere pressure should be 0.0F vacuum!");
        }

        var asteroidHolder = registry.getHolderOrThrow(GCCelestialBodies.ASTEROID);
        if (GCEventHandlers.computePlanetaryMeteorFrequency(helper.getLevel(), asteroidHolder) != 0.0F) {
            throw new GameTestAssertException("Falling meteors must be disabled in Zero-G Asteroid Belt!");
        }

        if (config.dayTemperature() != -73 || config.nightTemperature() != -200) {
            throw new GameTestAssertException("Expected Asteroids day/night temperatures -73/-200, got: "
                    + config.dayTemperature() + "/" + config.nightTemperature());
        }

        Registry<DimensionType> dimTypes = helper.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        DimensionType asteroidDimType = dimTypes.get(Constant.id("asteroid"));
        if (asteroidDimType == null) {
            throw new GameTestAssertException("Asteroids DimensionType was not registered!");
        }
        if (!asteroidDimType.hasSkyLight()) {
            throw new GameTestAssertException("Asteroids DimensionType should have hasSkyLight == true for vacuum solar contrast!");
        }
        if (Math.abs(asteroidDimType.ambientLight() - 0.02F) > 0.001F) {
            throw new GameTestAssertException("Expected Asteroids ambientLight == 0.02F, got: " + asteroidDimType.ambientLight());
        }

        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        if (biomes.get(GCBiomes.Asteroid.ASTEROID_FIELD) == null) {
            throw new GameTestAssertException("Asteroids biome 'asteroid_field' was not registered!");
        }
        if (biomes.get(GCBiomes.Asteroid.INNER_SILICATE_BELT) == null) {
            throw new GameTestAssertException("Asteroids biome 'asteroid_inner_silicate_belt' was not registered!");
        }
        if (biomes.get(GCBiomes.Asteroid.METALLIC_CORE_CLUSTER) == null) {
            throw new GameTestAssertException("Asteroids biome 'asteroid_metallic_core_cluster' was not registered!");
        }
        if (biomes.get(GCBiomes.Asteroid.KIRKWOOD_RESONANCE_GAP) == null) {
            throw new GameTestAssertException("Asteroids biome 'asteroid_kirkwood_gap' was not registered!");
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void asteroidBeltGeneratorBoundsAndSpectralCoverageTest(GameTestHelper helper) {
        Holder<MultiNoiseBiomeSourceParameterList> preset = helper.getLevel().registryAccess()
                .registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(GCMultiNoiseBiomeSourceParameterLists.ASTEROID);
        AsteroidChunkGenerator generator = new AsteroidChunkGenerator(
                MultiNoiseBiomeSource.createFromPreset(preset),
                GCDimensions.ASTEROID,
                1000L
        );

        if (generator.getMinY() != -64 || generator.getGenDepth() != 384 || generator.getSeaLevel() != -64) {
            throw new GameTestAssertException("Unexpected AsteroidChunkGenerator bounds: minY=" + generator.getMinY()
                    + ", depth=" + generator.getGenDepth() + ", seaLevel=" + generator.getSeaLevel());
        }

        NoiseColumn baseColumn = generator.getBaseColumn(0, 0, helper.getLevel(), helper.getLevel().getChunkSource().randomState());
        if (baseColumn == null) {
            throw new GameTestAssertException("AsteroidChunkGenerator.getBaseColumn returned null!");
        }

        // Verify all 4 spectral belt biomes are reachable across the belt
        Set<AsteroidChunkGenerator.SpectralType> foundTypes = EnumSet.noneOf(AsteroidChunkGenerator.SpectralType.class);
        for (int x = -3200; x <= 3200 && foundTypes.size() < 4; x += 160) {
            for (int z = -3200; z <= 3200 && foundTypes.size() < 4; z += 160) {
                foundTypes.add(generator.classifySpectralType(x, 128, z));
            }
        }
        if (foundTypes.size() != 4) {
            throw new GameTestAssertException("Expected all 4 spectral asteroid types to be generated, found only: " + foundTypes);
        }

        // Verify deterministic large asteroid lookup
        var largeAsteroid = generator.isLargeAsteroidAt(0, 0);
        if (largeAsteroid == null) {
            throw new GameTestAssertException("Expected at least one large asteroid (R >= 14) near origin!");
        }
        var repeatLookup = generator.isLargeAsteroidAt(0, 0);
        if (!largeAsteroid.equals(repeatLookup)) {
            throw new GameTestAssertException("AsteroidChunkGenerator.isLargeAsteroidAt is non-deterministic!");
        }

        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void grappleHookAndShortRangeTelepadTest(GameTestHelper helper) {
        // 1. Test Short-Range Telepad
        BlockPos pad1Pos = new BlockPos(1, 2, 1);
        BlockPos pad2Pos = new BlockPos(5, 2, 5);
        helper.setBlock(pad1Pos, GCBlocks.SHORT_RANGE_TELEPAD.defaultBlockState());
        helper.setBlock(pad2Pos, GCBlocks.SHORT_RANGE_TELEPAD.defaultBlockState());

        BlockEntity be1 = helper.getBlockEntity(pad1Pos);
        if (!(be1 instanceof ShortRangeTelepadBlockEntity telepad1)) {
            throw new GameTestAssertException("Expected ShortRangeTelepadBlockEntity at pad1Pos!");
        }

        BlockPos absPad2 = helper.absolutePos(pad2Pos);
        telepad1.setTargetPos(absPad2);

        SlimelingEntity traveler = helper.spawn(GCEntityTypes.SLIMELING, new BlockPos(1, 3, 1));
        telepad1.teleport(traveler);

        double distAfterTeleport = traveler.position().distanceTo(new Vec3(absPad2.getX() + 0.5, absPad2.getY() + 0.5, absPad2.getZ() + 0.5));
        if (distAfterTeleport > 1.0) {
            throw new GameTestAssertException("Telepad failed to teleport entity! Distance to target: " + distAfterTeleport);
        }

        // 2. Test Grapple Hook Entity pulling
        BlockPos anchorPos = helper.absolutePos(new BlockPos(1, 5, 1));
        GrappleHookEntity grapple = new GrappleHookEntity(traveler, helper.getLevel());
        grapple.setPos(anchorPos.getX() + 0.5, anchorPos.getY() + 0.5, anchorPos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(grapple);

        // Simulate hitting a block
        grapple.onHitBlock(new BlockHitResult(grapple.position(), Direction.DOWN, anchorPos, false));
        if (!grapple.isInGround() || !grapple.isPulling()) {
            throw new GameTestAssertException("GrappleHookEntity should be inGround and pulling after hitting a block!");
        }

        grapple.tick();
        if (traveler.getDeltaMovement().lengthSqr() < 0.01) {
            throw new GameTestAssertException("GrappleHookEntity failed to apply pulling velocity to owner!");
        }

        grapple.discard();
        traveler.discard();
        helper.succeed();
    }
}
