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
import dev.galacticraft.mod.content.block.entity.CrashedProbeBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.GeothermalGeneratorBlockEntity;
import dev.galacticraft.mod.content.item.AtomicBatteryItem;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

public class VenusTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE)
    public void venusCelestialBodyAndDimensionTest(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> celestialRegistry = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> venus = celestialRegistry.get(GCCelestialBodies.VENUS);
        if (venus == null) {
            context.fail("Expected GCCelestialBodies.VENUS to be registered in CELESTIAL_BODY registry!");
            return;
        }
        if (!(venus.type() instanceof Landable<?>)) {
            context.fail("Expected Venus celestial body type to implement Landable, found: " + venus.type());
            return;
        }
        if (!(venus.config() instanceof PlanetConfig config)) {
            context.fail("Expected Venus config to be PlanetConfig, found: " + venus.config());
            return;
        }
        if (!GCDimensions.VENUS.equals(config.world())) {
            context.fail("Expected Venus target dimension to be GCDimensions.VENUS, found: " + config.world());
            return;
        }
        if (config.accessWeight() != 3) {
            context.fail("Expected Venus accessWeight (rocket tier) to be 3, found: " + config.accessWeight());
            return;
        }
        if (Math.abs(config.gravity() - 0.904F) > 1.0E-4F) {
            context.fail("Expected Venus gravity to be 0.904F, found: " + config.gravity());
            return;
        }
        if (Math.abs(config.atmosphere().temperature() - 462.0F) > 1.0E-4F) {
            context.fail("Expected Venus temperature to be 462.0F, found: " + config.atmosphere().temperature());
            return;
        }
        if (Math.abs(config.atmosphere().pressure() - 92.0F) > 1.0E-4F) {
            context.fail("Expected Venus pressure to be 92.0F, found: " + config.atmosphere().pressure());
            return;
        }
        if (config.atmosphere().breathable()) {
            context.fail("Expected Venus atmosphere to be unbreathable!");
            return;
        }
        double co2Ppm = config.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.CARBON_DIOXIDE_ID), 0.0);
        if (co2Ppm < 900000.0) {
            context.fail("Expected Venus atmosphere to be dominated by CO2 (>900,000 ppm), found: " + co2Ppm);
            return;
        }

        boolean venusDimTypeRegistered = context.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).containsKey(Constant.id("venus"));
        boolean venusFlatRegistered = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME).containsKey(Constant.id("venus_flat"));
        boolean venusMountainRegistered = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME).containsKey(Constant.id("venus_mountain"));
        boolean venusValleyRegistered = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME).containsKey(Constant.id("venus_valley"));

        if (!venusDimTypeRegistered) {
            context.fail("Expected Venus dimension type to be registered in server registries!");
            return;
        }
        if (!venusFlatRegistered || !venusMountainRegistered || !venusValleyRegistered) {
            context.fail("Expected Venus biomes (flat, mountain, valley) to be registered in server registries!");
            return;
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void atomicBatteryExtractionTest(GameTestHelper context) {
        ItemStack battery = new ItemStack(GCItems.ATOMIC_BATTERY);
        EnergyStorage storage = EnergyStorage.ITEM.find(battery, null);
        if (storage == null) {
            context.fail("Expected EnergyStorage to be present on ATOMIC_BATTERY ItemStack!");
            return;
        }
        if (!storage.supportsExtraction()) {
            context.fail("Expected Atomic Battery to support energy extraction!");
            return;
        }
        if (storage.supportsInsertion()) {
            context.fail("Expected Atomic Battery to NOT support energy insertion (generator only)!");
            return;
        }

        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = storage.extract(50L, transaction);
            if (extracted != 50L) {
                context.fail("Expected extraction of 50 EU, got: " + extracted);
                return;
            }
        }

        try (Transaction transaction = Transaction.openOuter()) {
            long extractedCapped = storage.extract(500L, transaction);
            if (extractedCapped != AtomicBatteryItem.MAX_OUTPUT) {
                context.fail("Expected extraction capped at MAX_OUTPUT (" + AtomicBatteryItem.MAX_OUTPUT + "), got: " + extractedCapped);
                return;
            }
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void crashedProbeContainerTest(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, GCBlocks.CRASHED_PROBE);
        if (!(context.getBlockEntity(pos) instanceof CrashedProbeBlockEntity probe)) {
            context.fail("Expected CrashedProbeBlockEntity at " + pos);
            return;
        }
        if (probe.getContainerSize() != 9) {
            context.fail("Expected Crashed Probe container size 9, got: " + probe.getContainerSize());
            return;
        }

        probe.setItem(0, new ItemStack(GCItems.RADIOISOTOPE_CORE));
        if (!probe.getItem(0).is(GCItems.RADIOISOTOPE_CORE)) {
            context.fail("Expected Crashed Probe slot 0 to contain RADIOISOTOPE_CORE!");
            return;
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void geothermalGeneratorSpoutTest(GameTestHelper context) {
        BlockPos spoutPos = new BlockPos(1, 1, 1);
        BlockPos genPos = new BlockPos(1, 2, 1);

        context.setBlock(spoutPos, GCBlocks.VAPOR_SPOUT);
        context.setBlock(genPos, GCBlocks.GEOTHERMAL_GENERATOR);

        if (!(context.getBlockEntity(genPos) instanceof GeothermalGeneratorBlockEntity generator)) {
            context.fail("Expected GeothermalGeneratorBlockEntity at " + genPos);
            return;
        }

        context.runAtTickTime(10, () -> {
            if (generator.energyStorage().getAmount() <= 0) {
                context.fail("Expected Geothermal Generator over Vapor Spout to generate energy (>0 EU) after 10 ticks, got: " + generator.energyStorage().getAmount());
                return;
            }
            context.succeed();
        });
    }
}
