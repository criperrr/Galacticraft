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

package dev.galacticraft.api.rocket.part.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.APIConstants;
import dev.galacticraft.api.component.GCDataComponents;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rocket Part Data & Components Test Suite")
class RocketPartDataTest {

    private static final double EPSILON = 1e-6;

    @BeforeAll
    public static void setupMinecraftBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
    }

    @Nested
    @DisplayName("PartDamageState Specifications")
    class PartDamageStateTests {

        @Test
        @DisplayName("Enum serialized names conform to StringRepresentable")
        void testSerializedNames() {
            assertEquals("nominal", PartDamageState.NOMINAL.getSerializedName());
            assertEquals("worn", PartDamageState.WORN.getSerializedName());
            assertEquals("compromised", PartDamageState.COMPROMISED.getSerializedName());
            assertEquals("damaged", PartDamageState.DAMAGED.getSerializedName());
        }

        @Test
        @DisplayName("Mojang Codec roundtrip with JsonOps")
        void testJsonCodecRoundtrip() {
            for (PartDamageState state : PartDamageState.values()) {
                JsonElement json = PartDamageState.CODEC.encodeStart(JsonOps.INSTANCE, state)
                        .getOrThrow(AssertionError::new);
                assertTrue(json.isJsonPrimitive());
                assertEquals(state.getSerializedName(), json.getAsString());

                PartDamageState decoded = PartDamageState.CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(AssertionError::new);
                assertSame(state, decoded);
            }
        }

        @Test
        @DisplayName("StreamCodec roundtrip over Netty ByteBuf")
        void testStreamCodecRoundtrip() {
            for (PartDamageState state : PartDamageState.values()) {
                ByteBuf buf = Unpooled.buffer();
                try {
                    PartDamageState.STREAM_CODEC.encode(buf, state);
                    assertTrue(buf.readableBytes() > 0);
                    PartDamageState decoded = PartDamageState.STREAM_CODEC.decode(buf);
                    assertSame(state, decoded);
                    assertEquals(0, buf.readableBytes(), "Buffer should be fully consumed");
                } finally {
                    buf.release();
                }
            }
        }

        @Test
        @DisplayName("Integrity fraction state transitions")
        void testFromIntegrityBoundaries() {
            assertEquals(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(1.50));
            assertEquals(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(1.00));
            assertEquals(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(0.95));
            assertEquals(PartDamageState.WORN, PartDamageState.fromIntegrity(0.94999));
            assertEquals(PartDamageState.WORN, PartDamageState.fromIntegrity(0.85));
            assertEquals(PartDamageState.WORN, PartDamageState.fromIntegrity(0.75));
            assertEquals(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(0.74999));
            assertEquals(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(0.55));
            assertEquals(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(0.40));
            assertEquals(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(0.39999));
            assertEquals(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(0.10));
            assertEquals(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(0.00));
            assertEquals(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(-0.50));
        }

        @Test
        @DisplayName("Threshold and safety predicate checks")
        void testThresholdAndPredicates() {
            assertEquals(1.0, PartDamageState.NOMINAL.threshold(), EPSILON);
            assertEquals(1.0, PartDamageState.NOMINAL.getThreshold(), EPSILON);
            assertTrue(PartDamageState.NOMINAL.isSafe());
            assertFalse(PartDamageState.NOMINAL.isCritical());

            assertEquals(0.75, PartDamageState.WORN.threshold(), EPSILON);
            assertFalse(PartDamageState.WORN.isSafe());
            assertFalse(PartDamageState.WORN.isCritical());

            assertEquals(0.40, PartDamageState.COMPROMISED.threshold(), EPSILON);
            assertFalse(PartDamageState.COMPROMISED.isSafe());
            assertFalse(PartDamageState.COMPROMISED.isCritical());

            assertEquals(0.10, PartDamageState.DAMAGED.threshold(), EPSILON);
            assertFalse(PartDamageState.DAMAGED.isSafe());
            assertTrue(PartDamageState.DAMAGED.isCritical());
        }
    }

    @Nested
    @DisplayName("RocketPartStats Serialization & Network Protocols")
    class RocketPartStatsTests {

        private final RocketPartStats sampleStats = new RocketPartStats(
                1250.75,
                3800.50,
                750000L,
                4.25,
                1950.0
        );

        @Test
        @DisplayName("Mojang Codec roundtrip via NbtOps")
        void testNbtCodecRoundtrip() {
            Tag tag = RocketPartStats.CODEC.encodeStart(NbtOps.INSTANCE, sampleStats)
                    .getOrThrow(AssertionError::new);
            assertTrue(tag instanceof CompoundTag, "Encoded NBT must be a CompoundTag");
            CompoundTag compound = (CompoundTag) tag;

            assertEquals(sampleStats.thrustKn(), compound.getDouble("thrust_kn"), EPSILON);
            assertEquals(sampleStats.dryWeightKg(), compound.getDouble("dry_weight_kg"), EPSILON);
            assertEquals(sampleStats.fuelCapacityDroplets(), compound.getLong("fuel_capacity_droplets"));
            assertEquals(sampleStats.pressureToleranceAtm(), compound.getDouble("pressure_tolerance_atm"), EPSILON);
            assertEquals(sampleStats.thermalToleranceKelvin(), compound.getDouble("thermal_tolerance_kelvin"), EPSILON);

            RocketPartStats decoded = RocketPartStats.CODEC.parse(NbtOps.INSTANCE, compound)
                    .getOrThrow(AssertionError::new);
            assertEquals(sampleStats, decoded);
        }

        @Test
        @DisplayName("Mojang Codec roundtrip via JsonOps")
        void testJsonCodecRoundtrip() {
            JsonElement json = RocketPartStats.CODEC.encodeStart(JsonOps.INSTANCE, sampleStats)
                    .getOrThrow(AssertionError::new);
            assertTrue(json.isJsonObject(), "Encoded JSON must be an object");
            JsonObject obj = json.getAsJsonObject();

            assertEquals(sampleStats.thrustKn(), obj.get("thrust_kn").getAsDouble(), EPSILON);
            assertEquals(sampleStats.dryWeightKg(), obj.get("dry_weight_kg").getAsDouble(), EPSILON);
            assertEquals(sampleStats.fuelCapacityDroplets(), obj.get("fuel_capacity_droplets").getAsLong());
            assertEquals(sampleStats.pressureToleranceAtm(), obj.get("pressure_tolerance_atm").getAsDouble(), EPSILON);
            assertEquals(sampleStats.thermalToleranceKelvin(), obj.get("thermal_tolerance_kelvin").getAsDouble(), EPSILON);

            RocketPartStats decoded = RocketPartStats.CODEC.parse(JsonOps.INSTANCE, obj)
                    .getOrThrow(AssertionError::new);
            assertEquals(sampleStats, decoded);
        }

        @Test
        @DisplayName("Network StreamCodec roundtrip via ByteBuf")
        void testStreamCodecRoundtrip() {
            ByteBuf buf = Unpooled.buffer();
            try {
                RocketPartStats.STREAM_CODEC.encode(buf, sampleStats);
                assertTrue(buf.readableBytes() > 0);

                RocketPartStats decoded = RocketPartStats.STREAM_CODEC.decode(buf);
                assertEquals(sampleStats, decoded);
                assertEquals(0, buf.readableBytes(), "All written bytes must be read");
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("RocketPartStats.ZERO constant integrity and roundtrips")
        void testZeroConstant() {
            assertEquals(0.0, RocketPartStats.ZERO.thrustKn(), EPSILON);
            assertEquals(0.0, RocketPartStats.ZERO.dryWeightKg(), EPSILON);
            assertEquals(0L, RocketPartStats.ZERO.fuelCapacityDroplets());
            assertEquals(1.0, RocketPartStats.ZERO.pressureToleranceAtm(), EPSILON);
            assertEquals(300.0, RocketPartStats.ZERO.thermalToleranceKelvin(), EPSILON);

            ByteBuf buf = Unpooled.buffer();
            try {
                RocketPartStats.STREAM_CODEC.encode(buf, RocketPartStats.ZERO);
                RocketPartStats decoded = RocketPartStats.STREAM_CODEC.decode(buf);
                assertEquals(RocketPartStats.ZERO, decoded);
                assertEquals(0, buf.readableBytes());
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("Parsing corrupted / empty NBT results in error")
        void testCorruptedNbt() {
            CompoundTag empty = new CompoundTag();
            assertTrue(RocketPartStats.CODEC.parse(NbtOps.INSTANCE, empty).isError(),
                    "Parsing empty compound should fail due to missing fields");
        }

        @Test
        @DisplayName("Builder and wither methods work correctly")
        void testBuilderAndWithers() {
            RocketPartStats built = RocketPartStats.builder()
                    .thrust(500.0)
                    .dryWeight(1200.0)
                    .fuelBuckets(10.0)
                    .pressureTolerance(2.0)
                    .thermalTolerance(1500.0)
                    .build();

            assertEquals(500.0, built.thrustKn(), EPSILON);
            assertEquals(1200.0, built.dryWeightKg(), EPSILON);
            assertEquals(810000L, built.fuelCapacityDroplets());
            assertEquals(2.0, built.pressureToleranceAtm(), EPSILON);
            assertEquals(1500.0, built.thermalToleranceKelvin(), EPSILON);

            RocketPartStats modified = built
                    .withThrust(600.0)
                    .withDryWeight(1100.0)
                    .withFuelCapacity(900000L)
                    .withPressureTolerance(2.5)
                    .withThermalTolerance(1600.0);

            assertEquals(600.0, modified.thrustKn(), EPSILON);
            assertEquals(1100.0, modified.dryWeightKg(), EPSILON);
            assertEquals(900000L, modified.fuelCapacityDroplets());
            assertEquals(2.5, modified.pressureToleranceAtm(), EPSILON);
            assertEquals(1600.0, modified.thermalToleranceKelvin(), EPSILON);
        }
    }

    @Nested
    @DisplayName("PartIntegrity Mechanics & Transitions")
    class PartIntegrityTests {

        @Test
        @DisplayName("Factory pristine(max) and pristine() initial state")
        void testPristineFactory() {
            PartIntegrity pristine = PartIntegrity.pristine(250.0);
            assertEquals(250.0, pristine.currentIntegrity(), EPSILON);
            assertEquals(250.0, pristine.maxIntegrity(), EPSILON);
            assertSame(PartDamageState.NOMINAL, pristine.damageState());
            assertTrue(pristine.isPristine());
            assertEquals(1.0, pristine.fraction(), EPSILON);

            PartIntegrity defaultPristine = PartIntegrity.pristine();
            assertEquals(PartIntegrity.DEFAULT_MAX_INTEGRITY, defaultPristine.maxIntegrity(), EPSILON);
            assertEquals(PartIntegrity.DEFAULT, defaultPristine);
        }

        @Test
        @DisplayName("Stepwise damage calculations and state transitions")
        void testStepwiseDamageProgression() {
            PartIntegrity current = PartIntegrity.pristine(100.0);

            // Step 1: Minor wear (96/100 = 0.96 -> NOMINAL)
            current = current.damage(4.0);
            assertEquals(96.0, current.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.NOMINAL, current.damageState());
            assertTrue(current.isPristine());

            // Step 2: Transition to WORN (85/100 = 0.85 -> WORN)
            current = current.damage(11.0);
            assertEquals(85.0, current.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.WORN, current.damageState());
            assertFalse(current.isPristine());

            // Step 3: Transition to COMPROMISED (55/100 = 0.55 -> COMPROMISED)
            current = current.damage(30.0);
            assertEquals(55.0, current.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.COMPROMISED, current.damageState());

            // Step 4: Transition to DAMAGED (30/100 = 0.30 -> DAMAGED)
            current = current.damage(25.0);
            assertEquals(30.0, current.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.DAMAGED, current.damageState());

            // Step 5: Exhaustion to 0 (0/100 = 0.0 -> DAMAGED)
            current = current.damage(40.0);
            assertEquals(0.0, current.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.DAMAGED, current.damageState());
            assertEquals(0.0, current.fraction(), EPSILON);
        }

        @Test
        @DisplayName("Over-damage clamps currentIntegrity to 0.0 without mutating max")
        void testOverDamageClamping() {
            PartIntegrity p = PartIntegrity.pristine(100.0);
            PartIntegrity destroyed = p.damage(500.0);

            assertEquals(0.0, destroyed.currentIntegrity(), EPSILON);
            assertEquals(100.0, destroyed.maxIntegrity(), EPSILON);
            assertSame(PartDamageState.DAMAGED, destroyed.damageState());
        }

        @Test
        @DisplayName("Zero damage returns equivalent integrity")
        void testZeroDamage() {
            PartIntegrity p = PartIntegrity.pristine(100.0).damage(20.0);
            PartIntegrity untouched = p.damage(0.0);

            assertEquals(p.currentIntegrity(), untouched.currentIntegrity(), EPSILON);
            assertEquals(p.maxIntegrity(), untouched.maxIntegrity(), EPSILON);
            assertSame(p.damageState(), untouched.damageState());
        }

        @Test
        @DisplayName("Negative damage amount does not heal the part")
        void testNegativeDamage() {
            PartIntegrity p = PartIntegrity.pristine(100.0).damage(20.0);
            PartIntegrity untouched = p.damage(-15.0);

            assertEquals(p.currentIntegrity(), untouched.currentIntegrity(), EPSILON);
            assertEquals(p.maxIntegrity(), untouched.maxIntegrity(), EPSILON);
            assertSame(p.damageState(), untouched.damageState());
        }

        @Test
        @DisplayName("Zero max integrity does not trigger division by zero")
        void testZeroMaxIntegritySafety() {
            PartIntegrity weird = new PartIntegrity(0.0, 0.0, PartDamageState.DAMAGED);
            assertDoesNotThrow(() -> weird.damage(5.0));
            PartIntegrity damaged = weird.damage(5.0);
            assertEquals(0.0, damaged.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.DAMAGED, damaged.damageState());
        }

        @Test
        @DisplayName("Repair restores integrity with full or capped ceiling")
        void testRepair() {
            PartIntegrity damaged = PartIntegrity.pristine(100.0).damage(50.0); // 50/100 COMPROMISED

            // Full repair
            PartIntegrity repaired = damaged.repair(30.0); // 80/100 WORN
            assertEquals(80.0, repaired.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.WORN, repaired.damageState());

            // Field repair capped at 85%
            PartIntegrity fieldRepaired = damaged.repair(50.0, 0.85);
            assertEquals(85.0, fieldRepaired.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.WORN, fieldRepaired.damageState());

            // Full repair overflow capped at max
            PartIntegrity overRepaired = repaired.repair(50.0);
            assertEquals(100.0, overRepaired.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.NOMINAL, overRepaired.damageState());
            assertTrue(overRepaired.isPristine());
        }

        @Test
        @DisplayName("Repair when current integrity exceeds or equals ceiling must not degrade health")
        void testRepairWhenCurrentIntegrityExceedsCeiling() {
            // Setup part with 95.0 HP (NOMINAL) on 100.0 max
            PartIntegrity highIntegrity = PartIntegrity.pristine(100.0).damage(5.0);
            assertEquals(95.0, highIntegrity.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.NOMINAL, highIntegrity.damageState());

            // 1. Repairing with an 85% ceiling when health is already 95% must preserve health and state
            PartIntegrity repairedAboveCeiling = highIntegrity.repair(10.0, 0.85);
            assertEquals(95.0, repairedAboveCeiling.currentIntegrity(), EPSILON,
                    "Field repair capped below current integrity must not degrade health");
            assertSame(PartDamageState.NOMINAL, repairedAboveCeiling.damageState(),
                    "Damage state must remain NOMINAL");
            assertSame(highIntegrity, repairedAboveCeiling,
                    "Should return the exact same instance when current integrity is already at or above ceiling");

            // 2. Part exactly at ceiling (85.0 HP / WORN) must remain at 85.0 HP
            PartIntegrity atCeiling = PartIntegrity.pristine(100.0).damage(15.0);
            assertEquals(85.0, atCeiling.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.WORN, atCeiling.damageState());

            PartIntegrity repairedAtCeiling = atCeiling.repair(25.0, 0.85);
            assertEquals(85.0, repairedAtCeiling.currentIntegrity(), EPSILON,
                    "Repair when already at ceiling must remain unchanged");
            assertSame(PartDamageState.WORN, repairedAtCeiling.damageState());
            assertSame(atCeiling, repairedAtCeiling,
                    "Must return identical instance when already at ceiling");

            // 3. Repairing with non-positive ceiling fraction (0.0 or negative) must not degrade health
            PartIntegrity zeroFractionRepaired = highIntegrity.repair(50.0, 0.0);
            assertEquals(95.0, zeroFractionRepaired.currentIntegrity(), EPSILON,
                    "Zero maxFraction must not degrade structural health");
            assertSame(highIntegrity, zeroFractionRepaired);

            // 4. Normal repair below ceiling still functions as expected
            PartIntegrity damaged = PartIntegrity.pristine(100.0).damage(30.0); // 70 HP
            PartIntegrity normalRepair = damaged.repair(10.0, 0.85); // 80 HP
            assertEquals(80.0, normalRepair.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.WORN, normalRepair.damageState());
        }

        @Test
        @DisplayName("Mojang Codec roundtrip with JsonOps")
        void testPartIntegrityJsonRoundtrip() {
            PartIntegrity original = PartIntegrity.pristine(150.0).damage(30.0);
            JsonElement json = PartIntegrity.CODEC.encodeStart(JsonOps.INSTANCE, original)
                    .getOrThrow(AssertionError::new);
            assertTrue(json.isJsonObject());
            JsonObject obj = json.getAsJsonObject();

            assertEquals(120.0, obj.get("current").getAsDouble(), EPSILON);
            assertEquals(150.0, obj.get("max").getAsDouble(), EPSILON);
            assertEquals("worn", obj.get("state").getAsString());

            PartIntegrity decoded = PartIntegrity.CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(AssertionError::new);
            assertEquals(original, decoded);
        }

        @Test
        @DisplayName("Mojang Codec roundtrip with NbtOps")
        void testPartIntegrityNbtRoundtrip() {
            PartIntegrity original = PartIntegrity.pristine(120.0).damage(50.0);
            Tag tag = PartIntegrity.CODEC.encodeStart(NbtOps.INSTANCE, original)
                    .getOrThrow(AssertionError::new);
            assertTrue(tag instanceof CompoundTag);
            CompoundTag compound = (CompoundTag) tag;

            assertEquals(70.0, compound.getDouble("current"), EPSILON);
            assertEquals(120.0, compound.getDouble("max"), EPSILON);
            assertEquals("compromised", compound.getString("state"));

            PartIntegrity decoded = PartIntegrity.CODEC.parse(NbtOps.INSTANCE, compound)
                    .getOrThrow(AssertionError::new);
            assertEquals(original, decoded);
        }

        @Test
        @DisplayName("Network StreamCodec roundtrip over ByteBuf")
        void testPartIntegrityStreamCodec() {
            PartIntegrity original = PartIntegrity.pristine(200.0).damage(90.0);
            ByteBuf buf = Unpooled.buffer();
            try {
                PartIntegrity.STREAM_CODEC.encode(buf, original);
                assertTrue(buf.readableBytes() > 0);

                PartIntegrity decoded = PartIntegrity.STREAM_CODEC.decode(buf);
                assertEquals(original, decoded);
                assertEquals(0, buf.readableBytes());
            } finally {
                buf.release();
            }
        }
    }

    @Nested
    @DisplayName("ItemStack DataComponent Attachment & Persistence")
    class DataComponentItemStackIntegrationTests {

        @Test
        @DisplayName("DataComponentTypes are registered in BuiltInRegistries")
        void testRegistrationPresence() {
            assertSame(
                    GCDataComponents.ROCKET_PART_STATS,
                    BuiltInRegistries.DATA_COMPONENT_TYPE.get(APIConstants.id("rocket_part_stats")),
                    "ROCKET_PART_STATS must be registered under galacticraft:rocket_part_stats"
            );
            assertSame(
                    GCDataComponents.PART_INTEGRITY,
                    BuiltInRegistries.DATA_COMPONENT_TYPE.get(APIConstants.id("part_integrity")),
                    "PART_INTEGRITY must be registered under galacticraft:part_integrity"
            );
        }

        @Test
        @DisplayName("Clean ItemStack defaults without components")
        void testCleanItemStack() {
            ItemStack stack = new ItemStack(Items.IRON_INGOT);
            assertFalse(stack.has(GCDataComponents.ROCKET_PART_STATS));
            assertNull(stack.get(GCDataComponents.ROCKET_PART_STATS));
            assertEquals(RocketPartStats.ZERO, stack.getOrDefault(GCDataComponents.ROCKET_PART_STATS, RocketPartStats.ZERO));

            assertFalse(stack.has(GCDataComponents.PART_INTEGRITY));
            assertNull(stack.get(GCDataComponents.PART_INTEGRITY));
        }

        @Test
        @DisplayName("Attaching and retrieving ROCKET_PART_STATS and PART_INTEGRITY on ItemStack")
        void testAttachAndRetrieve() {
            ItemStack stack = new ItemStack(Items.IRON_INGOT);
            RocketPartStats stats = new RocketPartStats(800.0, 1500.0, 200000L, 2.0, 1200.0);
            PartIntegrity integrity = PartIntegrity.pristine(100.0);

            stack.set(GCDataComponents.ROCKET_PART_STATS, stats);
            stack.set(GCDataComponents.PART_INTEGRITY, integrity);

            assertTrue(stack.has(GCDataComponents.ROCKET_PART_STATS));
            assertEquals(stats, stack.get(GCDataComponents.ROCKET_PART_STATS));

            assertTrue(stack.has(GCDataComponents.PART_INTEGRITY));
            assertEquals(integrity, stack.get(GCDataComponents.PART_INTEGRITY));
        }

        @Test
        @DisplayName("Component removal via ItemStack.remove")
        void testComponentRemoval() {
            ItemStack stack = new ItemStack(Items.IRON_INGOT);
            RocketPartStats stats = new RocketPartStats(500.0, 1000.0, 100000L, 1.5, 900.0);
            PartIntegrity integrity = PartIntegrity.pristine(100.0);

            stack.set(GCDataComponents.ROCKET_PART_STATS, stats);
            stack.set(GCDataComponents.PART_INTEGRITY, integrity);

            stack.remove(GCDataComponents.ROCKET_PART_STATS);

            assertFalse(stack.has(GCDataComponents.ROCKET_PART_STATS));
            assertNull(stack.get(GCDataComponents.ROCKET_PART_STATS));
            assertTrue(stack.has(GCDataComponents.PART_INTEGRITY), "Independent component should remain intact");
        }

        @Test
        @DisplayName("Simulate in-flight / field disassembly damage mutation on ItemStack")
        void testItemStackDamageMutation() {
            ItemStack stack = new ItemStack(Items.IRON_INGOT);
            stack.set(GCDataComponents.PART_INTEGRITY, PartIntegrity.pristine(100.0));

            // Simulate rough field disassembly removing 30% integrity
            PartIntegrity current = stack.get(GCDataComponents.PART_INTEGRITY);
            assertNotNull(current);
            stack.set(GCDataComponents.PART_INTEGRITY, current.damage(30.0));

            PartIntegrity updated = stack.get(GCDataComponents.PART_INTEGRITY);
            assertNotNull(updated);
            assertEquals(70.0, updated.currentIntegrity(), EPSILON);
            assertSame(PartDamageState.COMPROMISED, updated.damageState());
        }

        @Test
        @DisplayName("ItemStack.copy provides defensive component isolation")
        void testCopyDefensiveIsolation() {
            ItemStack original = new ItemStack(Items.IRON_INGOT);
            original.set(GCDataComponents.PART_INTEGRITY, PartIntegrity.pristine(100.0).damage(30.0));

            ItemStack clone = original.copy();
            assertEquals(original.get(GCDataComponents.PART_INTEGRITY), clone.get(GCDataComponents.PART_INTEGRITY));

            // Mutate clone back to pristine
            clone.set(GCDataComponents.PART_INTEGRITY, PartIntegrity.pristine(100.0));

            assertEquals(100.0, clone.get(GCDataComponents.PART_INTEGRITY).currentIntegrity(), EPSILON);
            assertEquals(70.0, original.get(GCDataComponents.PART_INTEGRITY).currentIntegrity(), EPSILON);
            assertSame(PartDamageState.COMPROMISED, original.get(GCDataComponents.PART_INTEGRITY).damageState());
        }

        @Test
        @DisplayName("DataComponentPatch transfer across ItemStacks")
        void testComponentPatchTransfer() {
            ItemStack source = new ItemStack(Items.IRON_INGOT);
            RocketPartStats stats = new RocketPartStats(650.0, 1100.0, 300000L, 3.0, 1400.0);
            PartIntegrity integrity = PartIntegrity.pristine(120.0).damage(50.0);

            source.set(GCDataComponents.ROCKET_PART_STATS, stats);
            source.set(GCDataComponents.PART_INTEGRITY, integrity);

            DataComponentPatch patch = source.getComponentsPatch();

            ItemStack destination = new ItemStack(Items.COPPER_INGOT);
            destination.applyComponents(patch);

            assertTrue(destination.has(GCDataComponents.ROCKET_PART_STATS));
            assertEquals(stats, destination.get(GCDataComponents.ROCKET_PART_STATS));

            assertTrue(destination.has(GCDataComponents.PART_INTEGRITY));
            assertEquals(integrity, destination.get(GCDataComponents.PART_INTEGRITY));
        }
    }
}
