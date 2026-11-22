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

package dev.galacticraft.mod.content.rocket.part;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.FootprintMode;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartModelCache;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartRuntimeModel;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartShroudRenderer;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.CostProfile;
import dev.galacticraft.mod.network.c2s.SelectPartBuilderConfigPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Modular Rocket Part Catalog, Codecs, Footprints, Costs, Grids & Binary GCRM Decoder")
class ModularRocketPartSystemTest {
    private static RegistryAccess registryAccess;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
        GCBlocks.register();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    @DisplayName("Catalog loads all 299 parts, 65 families across 6 categories, 26 walkable interiors, 1 radial, and all 5 livery textures")
    void catalogLoadsAll299PartsAnd65Families() {
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        assertNotNull(catalog);
        assertEquals(299, catalog.size(), "Catalog must contain all 299 exported modular rocket parts");
        assertEquals(299, catalog.allParts().size());

        // Verify all 6 KSP-style Engine Shroud Interstages (1x1..11x11) are present
        for (int n : new int[]{1, 3, 5, 7, 9, 11}) {
            String id = "interstage_engine_shroud_" + n + "x" + n + "_1b";
            RocketPartSpec shroud = catalog.getPart(id);
            assertNotNull(shroud, "Missing KSP engine shroud interstage: " + id);
            assertEquals("interstage_engine_shroud", shroud.familyId());
            assertEquals(n, shroud.diameter());
            assertEquals(1, shroud.heightBlocks());
        }

        int totalFamilies = 0;
        for (RocketPartCategory category : RocketPartCategory.values()) {
            List<RocketPartFamily> families = catalog.familiesFor(category);
            assertNotNull(families);
            assertFalse(families.isEmpty(), "Category " + category + " must have at least one part family");
            totalFamilies += families.size();

            for (RocketPartFamily family : families) {
                assertEquals(category, family.category());
                assertFalse(family.variants().isEmpty(), "Family " + family.familyId() + " must have variants");
                assertSame(family, catalog.getFamily(family.familyId()),
                        "getFamily(" + family.familyId() + ") must resolve the registered family instance");
                assertNotNull(family.defaultVariant());
                assertFalse(family.availableDiameters().isEmpty());
                assertFalse(family.availableTopDiameters().isEmpty());
                assertFalse(family.availableHeights().isEmpty());
            }

            RocketPartSpec defaultSpec = catalog.defaultSpecFor(category);
            assertNotNull(defaultSpec, "Default spec for " + category + " must not be null");
            assertEquals(category, defaultSpec.category());

            RocketPartConfig defaultConfig = catalog.defaultConfigFor(category);
            assertNotNull(defaultConfig, "Default config for " + category + " must not be null");
            assertEquals(category, defaultConfig.category());
            assertEquals(RocketLivery.WHITE, defaultConfig.livery());
        }
        assertEquals(65, totalFamilies, "Total part families across all 6 categories must equal 65");

        int walkableCount = 0;
        int radialCount = 0;
        Set<String> seenModelIds = new HashSet<>();

        for (RocketPartSpec spec : catalog.allParts()) {
            assertTrue(seenModelIds.add(spec.modelId()), "Duplicate modelId in catalog: " + spec.modelId());
            assertSame(spec, catalog.getPart(spec.modelId()));
            assertFalse(spec.familyId().isBlank());
            assertFalse(spec.displayName().isBlank());
            assertNotNull(spec.category());
            assertTrue(spec.diameter() >= 1);
            assertTrue(spec.topDiameter() >= 1);
            assertTrue(spec.heightBlocks() >= 1);

            if (spec.walkableInterior()) {
                walkableCount++;
            }
            if (spec.radial()) {
                radialCount++;
            }

            assertNotNull(spec.meshLocation(), "Part " + spec.modelId() + " must have a non-null meshLocation");
            assertTrue(spec.meshLocation().getPath().endsWith(".gcrm"),
                    "Part " + spec.modelId() + " meshLocation must end with .gcrm, got " + spec.meshLocation());

            Map<RocketLivery, ResourceLocation> textures = spec.textures();
            assertNotNull(textures);
            assertEquals(RocketLivery.values().length, textures.size(),
                    "Part " + spec.modelId() + " must declare all 5 livery textures");
            for (RocketLivery livery : RocketLivery.values()) {
                ResourceLocation texLoc = textures.get(livery);
                assertNotNull(texLoc, "Part " + spec.modelId() + " missing texture for livery " + livery);
                assertTrue(texLoc.getPath().endsWith(".png"),
                        "Part " + spec.modelId() + " livery " + livery + " texture must be a .png path");
                assertEquals(texLoc, spec.textureFor(livery),
                        "textureFor(" + livery + ") must return exact registered livery texture");
            }
        }

        assertEquals(26, walkableCount, "Catalog must contain exactly 26 walkable-interior parts");
        assertEquals(1, radialCount, "Catalog must contain exactly 1 radial part");
    }

    @Test
    @DisplayName("RocketPartConfig CODEC (NbtOps & JsonOps) and STREAM_CODEC roundtrip all 9 fields with 0 unread bytes")
    void rocketPartConfigCodecAndStreamCodecRoundtrip() {
        List<RocketPartConfig> samples = List.of(
                new RocketPartConfig(
                        "adapter_5x5_to_3x3_2b",
                        "adapter",
                        RocketPartCategory.ADAPTER,
                        5,
                        3,
                        2,
                        RocketLivery.ORANGE,
                        false,
                        false
                ),
                new RocketPartConfig(
                        "capsule_orion_5x5_3b",
                        "capsule_orion",
                        RocketPartCategory.COMMAND,
                        5,
                        5,
                        3,
                        RocketLivery.BLUE,
                        true,
                        false
                ),
                new RocketPartConfig(
                        "decoupler_radial_1b",
                        "decoupler_radial",
                        RocketPartCategory.UTILITY,
                        1,
                        1,
                        2,
                        RocketLivery.OLIVE,
                        false,
                        true
                ),
                new RocketPartConfig(
                        "engine_starcluster33_11x11_4b",
                        "engine_starcluster33",
                        RocketPartCategory.ENGINE,
                        11,
                        11,
                        4,
                        RocketLivery.BLACK,
                        false,
                        false
                )
        );

        for (RocketPartConfig original : samples) {
            // 1. NbtOps CODEC roundtrip & field verification
            Tag encodedNbt = RocketPartConfig.CODEC.encodeStart(NbtOps.INSTANCE, original)
                    .getOrThrow(AssertionError::new);
            assertTrue(encodedNbt instanceof CompoundTag, "Encoded RocketPartConfig NBT must be a CompoundTag");
            CompoundTag tag = (CompoundTag) encodedNbt;
            assertEquals(original.modelId(), tag.getString("model_id"));
            assertEquals(original.familyId(), tag.getString("family_id"));
            assertEquals(original.category().getSerializedName(), tag.getString("category"));
            assertEquals(original.diameter(), tag.getInt("diameter"));
            assertEquals(original.topDiameter(), tag.getInt("top_diameter"));
            assertEquals(original.heightBlocks(), tag.getInt("height_blocks"));

            RocketPartConfig decodedNbt = RocketPartConfig.CODEC.parse(NbtOps.INSTANCE, tag)
                    .getOrThrow(AssertionError::new);
            assertAllNineFieldsEqual(original, decodedNbt);

            // 2. JsonOps CODEC roundtrip
            JsonElement encodedJson = RocketPartConfig.CODEC.encodeStart(JsonOps.INSTANCE, original)
                    .getOrThrow(AssertionError::new);
            RocketPartConfig decodedJson = RocketPartConfig.CODEC.parse(JsonOps.INSTANCE, encodedJson)
                    .getOrThrow(AssertionError::new);
            assertAllNineFieldsEqual(original, decodedJson);

            // 3. Raw ByteBuf STREAM_CODEC roundtrip
            ByteBuf rawBuf = Unpooled.buffer();
            try {
                RocketPartConfig.STREAM_CODEC.encode(rawBuf, original);
                assertTrue(rawBuf.readableBytes() > 0);
                RocketPartConfig decodedRaw = RocketPartConfig.STREAM_CODEC.decode(rawBuf);
                assertEquals(0, rawBuf.readableBytes(), "Raw ByteBuf must have 0 unread bytes after decode");
                assertAllNineFieldsEqual(original, decodedRaw);
            } finally {
                rawBuf.release();
            }

            // 4. RegistryFriendlyByteBuf STREAM_CODEC roundtrip
            RegistryFriendlyByteBuf regBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            try {
                RocketPartConfig.STREAM_CODEC.encode(regBuf, original);
                assertTrue(regBuf.readableBytes() > 0);
                RocketPartConfig decodedReg = RocketPartConfig.STREAM_CODEC.decode(regBuf);
                assertEquals(0, regBuf.readableBytes(), "RegistryFriendlyByteBuf must have 0 unread bytes after decode");
                assertAllNineFieldsEqual(original, decodedReg);
            } finally {
                regBuf.release();
            }
        }

        // Corrupted empty CompoundTag must fail decoding
        assertTrue(RocketPartConfig.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).isError());

        // Validation guards
        assertThrows(IllegalArgumentException.class, () -> new RocketPartConfig(
                "", "family", RocketPartCategory.TANK, 1, 1, 1, RocketLivery.WHITE, false, false));
        assertThrows(IllegalArgumentException.class, () -> new RocketPartConfig(
                "model", "family", RocketPartCategory.TANK, 0, 1, 1, RocketLivery.WHITE, false, false));
    }

    private static void assertAllNineFieldsEqual(RocketPartConfig expected, RocketPartConfig actual) {
        assertEquals(expected, actual);
        assertEquals(expected.modelId(), actual.modelId());
        assertEquals(expected.familyId(), actual.familyId());
        assertEquals(expected.category(), actual.category());
        assertEquals(expected.diameter(), actual.diameter());
        assertEquals(expected.topDiameter(), actual.topDiameter());
        assertEquals(expected.heightBlocks(), actual.heightBlocks());
        assertEquals(expected.livery(), actual.livery());
        assertEquals(expected.walkableInterior(), actual.walkableInterior());
        assertEquals(expected.radial(), actual.radial());
    }

    @Test
    @DisplayName("SelectPartBuilderConfigPayload STREAM_CODEC roundtrips cleanly with 0 unread bytes")
    void selectPartBuilderConfigPayloadStreamCodecRoundtrip() {
        List<SelectPartBuilderConfigPayload> payloads = List.of(
                new SelectPartBuilderConfigPayload(
                        new BlockPos(128, 72, -340),
                        "adapter",
                        5,
                        3,
                        2,
                        RocketLivery.ORANGE
                ),
                new SelectPartBuilderConfigPayload(
                        new BlockPos(-30_000_000, -64, 30_000_000),
                        "capsule_orion",
                        5,
                        5,
                        3,
                        RocketLivery.BLUE
                ),
                new SelectPartBuilderConfigPayload(
                        BlockPos.ZERO,
                        "engine_starcluster33",
                        11,
                        11,
                        6,
                        RocketLivery.OLIVE
                )
        );

        for (SelectPartBuilderConfigPayload original : payloads) {
            assertEquals(SelectPartBuilderConfigPayload.TYPE, original.type());

            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            try {
                SelectPartBuilderConfigPayload.STREAM_CODEC.encode(buf, original);
                assertTrue(buf.readableBytes() > 0);

                SelectPartBuilderConfigPayload decoded = SelectPartBuilderConfigPayload.STREAM_CODEC.decode(buf);
                assertEquals(0, buf.readableBytes(), "SelectPartBuilderConfigPayload decode must leave 0 unread bytes");
                assertEquals(original, decoded);
                assertEquals(original.pos(), decoded.pos());
                assertEquals(original.familyId(), decoded.familyId());
                assertEquals(original.diameter(), decoded.diameter());
                assertEquals(original.topDiameter(), decoded.topDiameter());
                assertEquals(original.heightBlocks(), decoded.heightBlocks());
                assertEquals(original.livery(), decoded.livery());
            } finally {
                buf.release();
            }
        }
    }

    @Test
    @DisplayName("FootprintMode computes 1x1x1 (1), 3x3x2 (18), 5x5x3 octagonal (63) and 5x5x3 walkable interior with hatch toggle")
    void footprintModeOctagonalAndWalkableInterior() {
        // 1. 1x1x1 solid -> 1 cell (0, 0, 0)
        RocketPartConfig solid1x1x1 = new RocketPartConfig(
                "tank_canister_1x1_1b", "tank_canister", RocketPartCategory.TANK,
                1, 1, 1, RocketLivery.WHITE, false, false
        );
        assertEquals(FootprintMode.SOLID_OCTAGON, solid1x1x1.footprintMode());
        Map<BlockPos, BoundsShape> fp1 = FootprintMode.computeLocalFootprint(solid1x1x1, false);
        assertEquals(1, fp1.size(), "1x1x1 solid footprint must have exactly 1 cell");
        assertEquals(BoundsShape.FULL, fp1.get(BlockPos.ZERO));
        assertTrue(FootprintMode.computeLocalHatchPositions(solid1x1x1).isEmpty());

        // 2. 3x3x2 solid -> 18 cells
        RocketPartConfig solid3x3x2 = new RocketPartConfig(
                "tank_fuel_rp1_3x3_2b", "tank_fuel_rp1", RocketPartCategory.TANK,
                3, 3, 2, RocketLivery.WHITE, false, false
        );
        assertEquals(FootprintMode.SOLID_OCTAGON, solid3x3x2.footprintMode());
        Map<BlockPos, BoundsShape> fp3 = FootprintMode.computeLocalFootprint(solid3x3x2, false);
        assertEquals(18, fp3.size(), "3x3x2 solid footprint must have 3*3*2 = 18 cells");
        for (int y = 0; y < 2; y++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    assertEquals(BoundsShape.FULL, fp3.get(new BlockPos(dx, y, dz)));
                }
            }
        }

        // 3. 5x5x3 octagonal solid -> 63 cells (21 per layer * 3 layers; 4 corners chamfered out per layer)
        RocketPartConfig solid5x5x3 = new RocketPartConfig(
                "tank_lox_cryo_5x5_3b", "tank_lox_cryo", RocketPartCategory.TANK,
                5, 5, 3, RocketLivery.WHITE, false, false
        );
        assertEquals(FootprintMode.SOLID_OCTAGON, solid5x5x3.footprintMode());
        Map<BlockPos, BoundsShape> fp5 = FootprintMode.computeLocalFootprint(solid5x5x3, false);
        assertEquals(63, fp5.size(), "5x5x3 octagonal solid footprint must have (25 - 4) * 3 = 63 cells");
        for (int y = 0; y < 3; y++) {
            for (int sx : new int[]{-2, 2}) {
                for (int sz : new int[]{-2, 2}) {
                    BlockPos corner = new BlockPos(sx, y, sz);
                    assertFalse(FootprintMode.isInsideOctagon(sx, sz, 5), "Corner (" + sx + ", " + sz + ") must be chamfered out");
                    assertFalse(fp5.containsKey(corner), "Chamfered corner " + corner + " must not be present in 5x5x3 footprint");
                }
            }
            assertEquals(BoundsShape.FULL, fp5.get(new BlockPos(2, y, 1)));
            assertEquals(BoundsShape.FULL, fp5.get(new BlockPos(-1, y, -2)));
        }

        // 4. 5x5x3 walkable interior (capsule_orion_5x5_3b)
        RocketPartSpec orionSpec = RocketPartCatalog.getInstance().getPart("capsule_orion_5x5_3b");
        assertNotNull(orionSpec, "capsule_orion_5x5_3b must exist in catalog");
        RocketPartConfig orionConfig = orionSpec.toConfig(RocketLivery.WHITE);
        assertTrue(orionConfig.walkableInterior());
        assertEquals(5, orionConfig.diameter());
        assertEquals(3, orionConfig.heightBlocks());
        assertEquals(FootprintMode.HOLLOW_WITH_HATCH, orionConfig.footprintMode());

        Map<BlockPos, BoundsShape> closedFootprint = FootprintMode.computeLocalFootprint(orionConfig, false);
        Map<BlockPos, BoundsShape> openFootprint = FootprintMode.computeLocalFootprint(orionConfig, true);

        // 63 total octagonal cells minus 9 interior air cells at y=1 = 54 occupied cells
        assertEquals(54, closedFootprint.size(), "5x5x3 hollow capsule must omit the 9 interior cells at y=1");
        assertEquals(54, openFootprint.size());

        int interiorAirCountAtY1 = 0;
        int interiorFloorCountAtY0 = 0;
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (!FootprintMode.isInsideOctagon(dx, dz, 5)) {
                    continue;
                }
                boolean interior = FootprintMode.isStrictlyInterior(dx, dz, 5);
                BlockPos floorPos = new BlockPos(dx, 0, dz);
                BlockPos midPos = new BlockPos(dx, 1, dz);
                if (interior) {
                    interiorFloorCountAtY0++;
                    assertEquals(BoundsShape.FLOOR_4PX, closedFootprint.get(floorPos),
                            "Floor interior cell " + floorPos + " must use FLOOR_4PX");
                    assertEquals(BoundsShape.FLOOR_4PX, openFootprint.get(floorPos));

                    interiorAirCountAtY1++;
                    assertFalse(closedFootprint.containsKey(midPos),
                            "Interior cabin cell " + midPos + " at y=1 must be absent (walkable air)");
                    assertFalse(openFootprint.containsKey(midPos),
                            "Interior cabin cell " + midPos + " at y=1 must be absent when hatch is open");
                } else {
                    assertEquals(BoundsShape.FULL, closedFootprint.get(floorPos),
                            "Floor perimeter wall cell " + floorPos + " must be FULL");
                }
            }
        }
        assertEquals(9, interiorFloorCountAtY0);
        assertEquals(9, interiorAirCountAtY1);

        List<BlockPos> hatchPositions = FootprintMode.computeLocalHatchPositions(orionConfig);
        assertEquals(List.of(new BlockPos(0, 1, 2), new BlockPos(0, 2, 2)), hatchPositions);
        for (BlockPos hatchPos : hatchPositions) {
            assertEquals(BoundsShape.FULL, closedFootprint.get(hatchPos),
                    "Closed hatch cell " + hatchPos + " must be BoundsShape.FULL");
            assertEquals(BoundsShape.OPEN_HATCH, openFootprint.get(hatchPos),
                    "Open hatch cell " + hatchPos + " must switch to BoundsShape.OPEN_HATCH");
        }
    }

    @Test
    @DisplayName("RocketPartCostCalculator hits slotItemMultiplier 1, 2, 4, 8, 12, 16 monotonically with valid ticks and category stats")
    void costCalculatorScalesMonotonically() {
        int[][] dims = {
                {1, 1},
                {3, 2},
                {5, 3},
                {7, 4},
                {9, 5},
                {11, 6}
        };
        int[] expectedMultipliers = {1, 2, 4, 8, 12, 16};

        int prevTicks = 0;
        double prevEngineDryMass = 0.0;
        double prevEngineThrust = 0.0;
        long prevTankFuel = 0L;

        for (int i = 0; i < dims.length; i++) {
            int d = dims[i][0];
            int h = dims[i][1];

            RocketPartConfig engineCfg = new RocketPartConfig(
                    "engine_test_" + d + "x" + d + "_" + h + "b",
                    "engine_test",
                    RocketPartCategory.ENGINE,
                    d,
                    d,
                    h,
                    RocketLivery.WHITE,
                    false,
                    false
            );
            CostProfile engineCost = RocketPartCostCalculator.calculate(engineCfg);
            assertEquals(expectedMultipliers[i], engineCost.slotItemMultiplier(),
                    "Expected slotItemMultiplier==" + expectedMultipliers[i] + " for " + d + "x" + d + "x" + h);
            assertTrue(engineCost.assemblyTimeTicks() >= RocketPartCostCalculator.MIN_ASSEMBLY_TICKS
                            && engineCost.assemblyTimeTicks() <= RocketPartCostCalculator.MAX_ASSEMBLY_TICKS,
                    "assemblyTimeTicks out of [160..800] range: " + engineCost.assemblyTimeTicks());
            assertTrue(engineCost.assemblyTimeTicks() > prevTicks,
                    "assemblyTimeTicks must increase monotonically across size tiers");
            prevTicks = engineCost.assemblyTimeTicks();

            assertEquals(RocketPartCostCalculator.BASE_ENERGY_PER_TICK, engineCost.energyPerTick());
            assertTrue(engineCost.scrapCount() >= 1);

            RocketPartStats engineStats = engineCost.stats();
            assertTrue(engineStats.dryWeightKg() > prevEngineDryMass, "Engine dry mass must scale monotonically");
            assertTrue(engineStats.thrustKn() > prevEngineThrust, "Engine thrust must scale monotonically and be positive");
            prevEngineDryMass = engineStats.dryWeightKg();
            prevEngineThrust = engineStats.thrustKn();

            RocketPartConfig tankCfg = new RocketPartConfig(
                    "tank_test_" + d + "x" + d + "_" + h + "b",
                    "tank_test",
                    RocketPartCategory.TANK,
                    d,
                    d,
                    h,
                    RocketLivery.WHITE,
                    false,
                    false
            );
            CostProfile tankCost = RocketPartCostCalculator.calculate(tankCfg);
            assertEquals(expectedMultipliers[i], tankCost.slotItemMultiplier());
            RocketPartStats tankStats = tankCost.stats();
            assertTrue(tankStats.dryWeightKg() > 0.0, "Tank dry mass must be positive");
            assertEquals(0.0, tankStats.thrustKn(), "Tank must have zero thrust");
            assertTrue(tankStats.fuelCapacityDroplets() > prevTankFuel,
                    "Tank propellant capacity must be positive and scale monotonically");
            prevTankFuel = tankStats.fuelCapacityDroplets();
        }
    }

    @Test
    @DisplayName("SchematicGridLayout defines 6 distinct category silhouettes (4..6 slots) and DEFAULT_3X3 (9 slots) inside [30..84]x[17..71] without overlap")
    void schematicGridLayoutsAreDistinctAndValid() {
        SchematicGridLayout defaultLayout = SchematicGridLayout.DEFAULT_3X3;
        assertFalse(defaultLayout.isModular());
        assertEquals(9, defaultLayout.activeSlotCount(), "DEFAULT_3X3 must have all 9 slots active");
        assertValidNonOverlappingCoordinates(defaultLayout, false);

        Set<Integer> distinctMasks = new HashSet<>();
        for (RocketPartCategory category : RocketPartCategory.values()) {
            SchematicGridLayout layout = SchematicGridLayout.forCategory(category);
            assertNotNull(layout);
            assertTrue(layout.isModular());
            assertEquals(category, layout.category());

            int activeCount = layout.activeSlotCount();
            assertTrue(activeCount >= 4 && activeCount <= 6,
                    "Category " + category + " activeSlotCount must be in [4..6], got " + activeCount);

            int bitmask = 0;
            for (int i = 0; i < SchematicGridLayout.GRID_SIZE; i++) {
                if (layout.isSlotActive(i)) {
                    bitmask |= (1 << i);
                }
            }
            assertEquals(activeCount, Integer.bitCount(bitmask));
            assertTrue(distinctMasks.add(bitmask),
                    "Category " + category + " shares an identical active slot mask with another category");

            assertValidNonOverlappingCoordinates(layout, true);
        }
        assertEquals(RocketPartCategory.values().length, distinctMasks.size(),
                "All 6 categories must have distinct active slot silhouettes");
    }

    private static void assertValidNonOverlappingCoordinates(SchematicGridLayout layout, boolean requireRequirements) {
        for (int i = 0; i < SchematicGridLayout.GRID_SIZE; i++) {
            if (!layout.isSlotActive(i)) {
                continue;
            }
            int x = layout.slotX(i);
            int y = layout.slotY(i);
            assertTrue(x >= 30 && x <= 84,
                    "Slot " + i + " X coordinate " + x + " must fall inside [30..84]");
            assertTrue(y >= 17 && y <= 71,
                    "Slot " + i + " Y coordinate " + y + " must fall inside [17..71]");

            if (requireRequirements) {
                SchematicGridLayout.SlotRequirement req = layout.requirement(i);
                assertNotNull(req, "Active modular slot " + i + " in " + layout.category() + " must declare a SlotRequirement");
                assertTrue(req.baseCount() >= 1);
                assertFalse(req.hintLabel().isBlank());
                assertFalse(req.primaryDisplayStack().isEmpty());
                assertEquals(req.baseCount(), req.requiredCount(1));
                assertTrue(req.requiredCount(16) <= SchematicGridLayout.MAX_SLOT_STACK_SIZE);
            }

            for (int j = i + 1; j < SchematicGridLayout.GRID_SIZE; j++) {
                if (!layout.isSlotActive(j)) {
                    continue;
                }
                int ox = layout.slotX(j);
                int oy = layout.slotY(j);
                boolean nonOverlapping18x18 = Math.abs(x - ox) >= 18 || Math.abs(y - oy) >= 18;
                assertTrue(nonOverlapping18x18,
                        "Active slots " + i + " (" + x + "," + y + ") and " + j + " (" + ox + "," + oy + ") overlap");
            }
        }
    }

    @Test
    @DisplayName("RocketPartModelCache.parseBinary decodes real exported .gcrm meshes with opaque/translucent quads, finite bounds, and lodDetail bones")
    void binaryGcrmDecoderReadsRealExportedMeshes() throws IOException {
        // 1. capsule_orion_5x5_3b.gcrm (opaque + translucent viewport glass quads)
        String orionResource = "/assets/galacticraft/models/rocket_part/command/capsule_orion_5x5_3b.gcrm";
        ResourceLocation orionId = Constant.id("models/rocket_part/command/capsule_orion_5x5_3b.gcrm");
        RocketPartRuntimeModel orionModel = decodeClasspathGcrm(orionResource, orionId);

        assertEquals(orionId, orionModel.location());
        assertEquals(30.0F, orionModel.duration(), 1.0e-4F);
        assertTrue(orionModel.bones().length > 0, "capsule_orion_5x5_3b must have bones");
        assertTrue(orionModel.hasOpaqueQuads(), "capsule_orion_5x5_3b must have opaque hull quads");
        assertTrue(orionModel.hasTranslucentQuads(), "capsule_orion_5x5_3b must have translucent viewport quads");
        assertTrue(orionModel.totalQuadCount() > 0);
        assertFiniteNonDegenerateBounds(orionModel);

        int orionOpaque = 0;
        int orionTranslucent = 0;
        for (RocketPartRuntimeModel.Bone bone : orionModel.bones()) {
            assertNotNull(bone.name);
            orionOpaque += bone.opaqueQuadCount();
            orionTranslucent += bone.translucentQuadCount();
        }
        assertTrue(orionOpaque > 0, "Expected positive opaque quad count on capsule_orion_5x5_3b");
        assertTrue(orionTranslucent > 0, "Expected positive translucent quad count on capsule_orion_5x5_3b");
        assertEquals(orionOpaque + orionTranslucent, orionModel.totalQuadCount());

        // 2. engine_starcluster33_11x11_4b.gcrm (>6000 quads with lodDetail bones)
        String starclusterResource = "/assets/galacticraft/models/rocket_part/engines/engine_starcluster33_11x11_4b.gcrm";
        ResourceLocation starclusterId = Constant.id("models/rocket_part/engines/engine_starcluster33_11x11_4b.gcrm");
        RocketPartRuntimeModel starclusterModel = decodeClasspathGcrm(starclusterResource, starclusterId);

        assertEquals(starclusterId, starclusterModel.location());
        assertTrue(starclusterModel.bones().length > 0);
        assertTrue(starclusterModel.hasOpaqueQuads());
        assertTrue(starclusterModel.totalQuadCount() > 6000,
                "engine_starcluster33_11x11_4b must have >6000 quads, got " + starclusterModel.totalQuadCount());
        assertTrue(starclusterModel.hasLodDetailBones(),
                "engine_starcluster33_11x11_4b must contain lodDetail bones");
        assertFiniteNonDegenerateBounds(starclusterModel);

        int lodDetailBones = 0;
        int coreBones = 0;
        for (RocketPartRuntimeModel.Bone bone : starclusterModel.bones()) {
            if (bone.lodDetail) {
                lodDetailBones++;
            } else {
                coreBones++;
            }
        }
        assertTrue(lodDetailBones > 0, "Expected at least one lodDetail=true bone on engine_starcluster33_11x11_4b");
        assertTrue(coreBones > 0, "Expected core bells/body bones to retain lodDetail=false on engine_starcluster33_11x11_4b");

        // 3. Invalid header rejection
        byte[] bogus = "BAD!0000".getBytes(StandardCharsets.US_ASCII);
        try (DataInputStream badIn = new DataInputStream(new ByteArrayInputStream(bogus))) {
            assertThrows(IOException.class, () -> RocketPartModelCache.parseBinary(Constant.id("invalid.gcrm"), badIn));
        }
    }

    @Test
    @DisplayName("Adapter-under-engine shroud detection, height alignment to engine deck, and hull facet scaling")
    void adapterEngineShroudRulesAndHeights() {
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();

        RocketPartConfig adapter = catalog.getPart("adapter_5x5_to_3x3_2b").toConfig(RocketLivery.WHITE);
        RocketPartConfig interstage = catalog.getPart("interstage_hollow_5x5_to_3x3_1b").toConfig(RocketLivery.WHITE);
        RocketPartConfig shroudInterstage = catalog.getPart("interstage_engine_shroud_3x3_1b").toConfig(RocketLivery.WHITE);
        RocketPartConfig noseCone = catalog.getPart("fairing_nose_cone_3x3_2b").toConfig(RocketLivery.WHITE);
        RocketPartConfig tank = catalog.getPart("tank_fuel_rp1_3x3_2b").toConfig(RocketLivery.WHITE);

        assertTrue(RocketPartShroudRenderer.supportsEngineShroud(adapter), "Standard conical adapter must generate engine shroud");
        assertTrue(RocketPartShroudRenderer.supportsEngineShroud(interstage), "Hollow interstage must generate engine shroud");
        assertTrue(RocketPartShroudRenderer.supportsEngineShroud(shroudInterstage), "Engine shroud interstage must generate engine shroud");
        assertFalse(RocketPartShroudRenderer.supportsEngineShroud(noseCone), "Fairing nose cone must not generate engine shroud");
        assertFalse(RocketPartShroudRenderer.supportsEngineShroud(tank), "Propellant tank must not generate engine shroud");

        RocketPartConfig falconEngine = catalog.getPart("engine_falcon1d_3x3_2b").toConfig(RocketLivery.WHITE);
        RocketPartConfig vulcainEngine = catalog.getPart("engine_vulcain_ssme_5x5_3b").toConfig(RocketLivery.WHITE);
        RocketPartConfig rcsRing = catalog.getPart("rcs_quad_3x3_1b").toConfig(RocketLivery.WHITE);

        assertTrue(RocketPartShroudRenderer.needsEngineShroud(falconEngine), "Liquid engine must receive shroud cover");
        assertTrue(RocketPartShroudRenderer.needsEngineShroud(vulcainEngine), "SSME engine must receive shroud cover");
        assertFalse(RocketPartShroudRenderer.needsEngineShroud(rcsRing), "Full-hull RCS ring must not receive shroud cover");
        assertFalse(RocketPartShroudRenderer.needsEngineShroud(tank), "Non-engine tank must not receive shroud cover");

        // Falcon 1D 2b (32px) has 4px deck -> shroud reaches exactly 28px = 1.75 blocks (bottom of engine deck)
        assertEquals(1.75F, RocketPartShroudRenderer.computeShroudHeightBlocks(falconEngine), 1.0e-4F);
        // Vulcain SSME 3b (48px) has 5px deck -> shroud reaches exactly 43px = 2.6875 blocks
        assertEquals(43.0F / 16.0F, RocketPartShroudRenderer.computeShroudHeightBlocks(vulcainEngine), 1.0e-4F);

        // Facet count scaling must match module_meshes.py hull_sides(R) exactly
        assertEquals(8, RocketPartShroudRenderer.hullSidesForRadiusBlocks(0.5F));
        assertEquals(12, RocketPartShroudRenderer.hullSidesForRadiusBlocks(1.5F));
        assertEquals(16, RocketPartShroudRenderer.hullSidesForRadiusBlocks(2.5F));
        assertEquals(16, RocketPartShroudRenderer.hullSidesForRadiusBlocks(3.5F));
        assertEquals(20, RocketPartShroudRenderer.hullSidesForRadiusBlocks(4.5F));
        assertEquals(20, RocketPartShroudRenderer.hullSidesForRadiusBlocks(5.5F));
    }

    private static RocketPartRuntimeModel decodeClasspathGcrm(String classpathPath, ResourceLocation id) throws IOException {
        InputStream raw = ModularRocketPartSystemTest.class.getResourceAsStream(classpathPath);
        assertNotNull(raw, "Missing exported .gcrm resource on classpath: " + classpathPath);
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(raw))) {
            RocketPartRuntimeModel model = RocketPartModelCache.parseBinary(id, in);
            assertNotNull(model);
            assertEquals(-1, in.read(), "Binary .gcrm decoder must consume the entire stream with 0 trailing bytes");
            return model;
        }
    }

    private static void assertFiniteNonDegenerateBounds(RocketPartRuntimeModel model) {
        float[] bounds = model.boundsMinMax();
        assertEquals(6, bounds.length);
        for (float v : bounds) {
            assertTrue(Float.isFinite(v), "Bounds coordinate must be finite, got " + v);
        }
        assertTrue(model.minX() < model.maxX() && model.boundsSizeX() > 0.0F, "Model X bounds must be non-degenerate");
        assertTrue(model.minY() < model.maxY() && model.boundsSizeY() > 0.0F, "Model Y bounds must be non-degenerate");
        assertTrue(model.minZ() < model.maxZ() && model.boundsSizeZ() > 0.0F, "Model Z bounds must be non-degenerate");
    }
}
