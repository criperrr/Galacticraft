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
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.SlimelingEntity;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.AgeableMob;

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

        boolean marsDimTypeRegistered = context.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).containsKey(Constant.id("mars"));
        boolean marsBiomeRegistered = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME).containsKey(Constant.id("mars_flat"));
        if (!marsDimTypeRegistered) {
            context.fail("Expected Mars dimension type to be registered in server registries!");
            return;
        }
        if (!marsBiomeRegistered) {
            context.fail("Expected Mars biomes to be registered in server registries!");
            return;
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
