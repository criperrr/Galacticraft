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

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.component.GCDataComponents;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adversarial Challenger Stress Suite: Rocket Part Data & Components")
class RocketPartAdversarialTest {

    @BeforeAll
    public static void setupMinecraftBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
    }

    @Nested
    @DisplayName("1. Threshold Boundary Transitions & Precision Stress")
    class ThresholdBoundaryStressTests {

        @Test
        @DisplayName("Exact boundary transitions")
        void testExactBoundaries() {
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(1.0));
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(0.95));
            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(0.75));
            assertSame(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(0.40));
            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(0.0));
        }

        @Test
        @DisplayName("Micro-epsilon transitions across all critical boundaries")
        void testMicroEpsilonTransitions() {
            // Boundary 0.95: NOMINAL vs WORN
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(0.9500000000000001));
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(0.95000000001));
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(Math.nextUp(0.95)));
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(0.95));

            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(Math.nextDown(0.95)));
            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(0.9499999999999999));
            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(0.94999999999));

            // Boundary 0.75: WORN vs COMPROMISED
            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(0.7500000000000001));
            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(0.75000000001));
            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(Math.nextUp(0.75)));
            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(0.75));

            assertSame(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(Math.nextDown(0.75)));
            assertSame(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(0.7499999999999999));
            assertSame(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(0.74999999999));

            // Boundary 0.40: COMPROMISED vs DAMAGED
            assertSame(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(0.4000000000000001));
            assertSame(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(0.40000000001));
            assertSame(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(Math.nextUp(0.40)));
            assertSame(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(0.40));

            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(Math.nextDown(0.40)));
            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(0.3999999999999999));
            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(0.39999999999));
        }

        @Test
        @DisplayName("Float vs double cast boundary pitfalls")
        void testFloatToDoubleCastPitfalls() {
            // 0.95f in IEEE 754 float is 0.949999988079071, strictly < 0.95 double
            float f95 = 0.95f;
            double d95 = (double) f95;
            assertTrue(d95 < 0.95, "0.95f promoted to double must be strictly less than 0.95d");
            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(d95));

            // 0.75 is an exact dyadic fraction (3/4 = 2^-1 + 2^-2), so 0.75f == 0.75d
            float f75 = 0.75f;
            double d75 = (double) f75;
            assertEquals(0.75, d75);
            assertSame(PartDamageState.WORN, PartDamageState.fromIntegrity(d75));

            // 0.40f promoted to double is 0.4000000059604645, strictly > 0.40 double
            float f40 = 0.40f;
            double d40 = (double) f40;
            assertTrue(d40 > 0.40, "0.40f promoted to double must be strictly greater than 0.40d");
            assertSame(PartDamageState.COMPROMISED, PartDamageState.fromIntegrity(d40));
        }

        @Test
        @DisplayName("Extreme, infinite, and NaN values handling")
        void testExtremeAndSpecialValues() {
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(Double.POSITIVE_INFINITY));
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(Double.MAX_VALUE));
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(100.0));
            assertSame(PartDamageState.NOMINAL, PartDamageState.fromIntegrity(1.0000001));

            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(0.0));
            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(-0.0));
            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(-0.000001));
            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(-1.0));
            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(-Double.MAX_VALUE));
            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(Double.NEGATIVE_INFINITY));

            // IEEE 754 NaN comparisons all return false, gracefully falling through to DAMAGED
            assertSame(PartDamageState.DAMAGED, PartDamageState.fromIntegrity(Double.NaN));
        }

        @Test
        @DisplayName("Predicate invariants across all enum states")
        void testPredicateInvariants() {
            for (PartDamageState state : PartDamageState.values()) {
                if (state == PartDamageState.NOMINAL) {
                    assertTrue(state.isSafe());
                    assertFalse(state.isCritical());
                } else if (state == PartDamageState.DAMAGED) {
                    assertFalse(state.isSafe());
                    assertTrue(state.isCritical());
                } else {
                    assertFalse(state.isSafe());
                    assertFalse(state.isCritical());
                }
            }
        }
    }

    @Nested
    @DisplayName("2. PartIntegrity Stress & Arithmetic Edge Cases")
    class PartIntegrityStressTests {

        @Test
        @DisplayName("Continuous degradation over 1,000 steps")
        void testContinuousDegradationSteps() {
            PartIntegrity integrity = PartIntegrity.pristine(1000.0);
            double step = 1.0;
            for (int i = 0; i < 1000; i++) {
                integrity = integrity.damage(step);
                double expectedRemaining = 1000.0 - (i + 1) * step;
                assertEquals(expectedRemaining, integrity.currentIntegrity(), 1e-9);
                assertEquals(expectedRemaining / 1000.0, integrity.fraction(), 1e-9);

                PartDamageState expectedState = PartDamageState.fromIntegrity(expectedRemaining / 1000.0);
                assertSame(expectedState, integrity.damageState());
            }
            assertEquals(0.0, integrity.currentIntegrity());
            assertSame(PartDamageState.DAMAGED, integrity.damageState());
        }

        @Test
        @DisplayName("Zero max integrity division-by-zero stress")
        void testZeroMaxIntegrity() {
            PartIntegrity zeroMax = new PartIntegrity(0.0, 0.0, null);
            assertFalse(Double.isNaN(zeroMax.fraction()));
            assertEquals(0.0, zeroMax.fraction());
            assertSame(PartDamageState.DAMAGED, zeroMax.damageState());
            assertFalse(zeroMax.isPristine());

            PartIntegrity damaged = zeroMax.damage(10.0);
            assertEquals(0.0, damaged.currentIntegrity());
            assertEquals(0.0, damaged.fraction());

            PartIntegrity repaired = zeroMax.repair(50.0);
            assertEquals(0.0, repaired.currentIntegrity());
            assertEquals(0.0, repaired.fraction());
        }

        @Test
        @DisplayName("Damage with Infinite, Max, and NaN amounts")
        void testExtremeDamageAmounts() {
            PartIntegrity base = PartIntegrity.pristine(100.0);

            // Infinite damage clamps to 0.0 without throwing
            PartIntegrity infDamaged = base.damage(Double.POSITIVE_INFINITY);
            assertEquals(0.0, infDamaged.currentIntegrity());
            assertSame(PartDamageState.DAMAGED, infDamaged.damageState());

            // Max double damage clamps to 0.0
            PartIntegrity maxDamaged = base.damage(Double.MAX_VALUE);
            assertEquals(0.0, maxDamaged.currentIntegrity());
            assertSame(PartDamageState.DAMAGED, maxDamaged.damageState());

            // Negative damage is ignored (cannot repair via damage)
            PartIntegrity negDamaged = base.damage(-50.0);
            assertEquals(100.0, negDamaged.currentIntegrity());
            assertSame(PartDamageState.NOMINAL, negDamaged.damageState());
        }

        @Test
        @DisplayName("Field repair ceiling behavior and integrity retention probe")
        void testFieldRepairCeilingBehavior() {
            PartIntegrity damaged = PartIntegrity.pristine(100.0).damage(60.0); // 40.0 current (COMPROMISED)
            assertEquals(40.0, damaged.currentIntegrity());

            // Field repair capped at 0.85
            PartIntegrity repaired = damaged.repair(30.0, 0.85); // 40 + 30 = 70.0 (COMPROMISED)
            assertEquals(70.0, repaired.currentIntegrity());
            assertSame(PartDamageState.COMPROMISED, repaired.damageState());

            // Repairing with massive amount caps at 85.0 (WORN)
            PartIntegrity maxFieldRepaired = damaged.repair(1000.0, 0.85);
            assertEquals(85.0, maxFieldRepaired.currentIntegrity());
            assertSame(PartDamageState.WORN, maxFieldRepaired.damageState());

            // Repairing with ceiling = 1.0 (Hangar) restores to full 100.0
            PartIntegrity hangarRepaired = maxFieldRepaired.repair(100.0, 1.0);
            assertEquals(100.0, hangarRepaired.currentIntegrity());
            assertSame(PartDamageState.NOMINAL, hangarRepaired.damageState());
            assertTrue(hangarRepaired.isPristine());
        }

        @Test
        @DisplayName("Repair when current integrity already exceeds ceiling preserves existing integrity")
        void testRepairWhenCurrentIntegrityExceedsCeiling() {
            PartIntegrity highIntegrityPart = PartIntegrity.pristine(100.0).damage(5.0); // 95.0 HP, NOMINAL
            assertEquals(95.0, highIntegrityPart.currentIntegrity());
            assertSame(PartDamageState.NOMINAL, highIntegrityPart.damageState());

            // Attempting to repair with a field repair kit capped at 85% must not degrade the component
            PartIntegrity result = highIntegrityPart.repair(10.0, 0.85);
            assertEquals(95.0, result.currentIntegrity(), "Repair must not lower integrity below current health");
            assertSame(PartDamageState.NOMINAL, result.damageState());
            assertSame(highIntegrityPart, result, "Should return identical instance when repair is a no-op");
        }

        @Test
        @DisplayName("Exorbitant ceiling fraction is safely clamped to maxIntegrity")
        void testExorbitantCeilingFraction() {
            PartIntegrity base = PartIntegrity.pristine(100.0).damage(50.0); // 50.0
            PartIntegrity overRepaired = base.repair(200.0, 5.0); // maxFraction 500%
            assertEquals(100.0, overRepaired.currentIntegrity(), "Must not exceed maxIntegrity even if maxFraction > 1.0");
        }
    }

    @Nested
    @DisplayName("3. Immutability, Copy-on-Write, and Concurrency Stress")
    class ImmutabilityAndConcurrencyTests {

        @Test
        @DisplayName("PartIntegrity methods enforce strict immutability")
        void testPartIntegrityImmutability() {
            PartIntegrity original = PartIntegrity.pristine(100.0);
            PartIntegrity damaged = original.damage(20.0); // 80.0 -> WORN
            PartIntegrity repaired = damaged.repair(10.0);

            assertNotSame(original, damaged);
            assertNotSame(damaged, repaired);
            assertNotSame(original, repaired);
            assertSame(original, original.repair(10.0));

            assertEquals(100.0, original.currentIntegrity());
            assertEquals(100.0, original.maxIntegrity());
            assertSame(PartDamageState.NOMINAL, original.damageState());
            assertTrue(original.isPristine());

            assertEquals(80.0, damaged.currentIntegrity());
            assertSame(PartDamageState.WORN, damaged.damageState());

            assertEquals(90.0, repaired.currentIntegrity());
            assertSame(PartDamageState.WORN, repaired.damageState());
        }

        @Test
        @DisplayName("RocketPartStats wither methods enforce strict immutability")
        void testRocketPartStatsImmutability() {
            RocketPartStats base = RocketPartStats.ZERO;

            RocketPartStats s1 = base.withThrust(1000.0);
            RocketPartStats s2 = base.withDryWeight(5000.0);
            RocketPartStats s3 = base.withFuelCapacity(810000L);
            RocketPartStats s4 = base.withPressureTolerance(15.0);
            RocketPartStats s5 = base.withThermalTolerance(2500.0);

            assertEquals(0.0, base.thrustKn());
            assertEquals(0.0, base.dryWeightKg());
            assertEquals(0L, base.fuelCapacityDroplets());
            assertEquals(1.0, base.pressureToleranceAtm());
            assertEquals(300.0, base.thermalToleranceKelvin());

            assertEquals(1000.0, s1.thrustKn());
            assertEquals(5000.0, s2.dryWeightKg());
            assertEquals(810000L, s3.fuelCapacityDroplets());
            assertEquals(15.0, s4.pressureToleranceAtm());
            assertEquals(2500.0, s5.thermalToleranceKelvin());
        }

        @Test
        @DisplayName("Concurrent multi-threaded operations on shared immutable instances")
        void testConcurrencyStress() throws Exception {
            final PartIntegrity sharedIntegrity = PartIntegrity.pristine(1000.0);
            final RocketPartStats sharedStats = RocketPartStats.ZERO;

            int threadCount = 16;
            int iterationsPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Callable<Boolean>> tasks = new ArrayList<>();

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                tasks.add(() -> {
                    PartIntegrity localIntegrity = sharedIntegrity;
                    RocketPartStats localStats = sharedStats;
                    for (int i = 0; i < iterationsPerThread; i++) {
                        localIntegrity = localIntegrity.damage((threadId + 1) * 0.1);
                        localIntegrity = localIntegrity.repair((threadId + 1) * 0.05);
                        localStats = localStats.withThrust(localStats.thrustKn() + 1.0)
                                .withFuelCapacity(localStats.fuelCapacityDroplets() + 81L);
                    }
                    assertTrue(localIntegrity.currentIntegrity() <= 1000.0);
                    assertTrue(localStats.thrustKn() > 0.0);
                    return true;
                });
            }

            List<Future<Boolean>> results = executor.invokeAll(tasks);
            for (Future<Boolean> future : results) {
                assertTrue(future.get());
            }
            executor.shutdown();

            // Verify shared objects remained completely untouched
            assertEquals(1000.0, sharedIntegrity.currentIntegrity());
            assertEquals(0.0, sharedStats.thrustKn());
        }
    }

    @Nested
    @DisplayName("4. DataComponentPatch & ItemStack Deep Integration")
    class ItemStackAndPatchIntegrationTests {

        @Test
        @DisplayName("DataComponentPatch bidirectional application and removal")
        void testPatchApplicationAndRemoval() {
            ItemStack stack = new ItemStack(Items.IRON_BLOCK);
            RocketPartStats stats = new RocketPartStats(1500.0, 4200.0, 810000L, 8.5, 1750.0);
            PartIntegrity integrity = PartIntegrity.pristine(250.0).damage(50.0);

            // Build patch adding both components
            DataComponentPatch patch = DataComponentPatch.builder()
                    .set(GCDataComponents.ROCKET_PART_STATS, stats)
                    .set(GCDataComponents.PART_INTEGRITY, integrity)
                    .build();

            stack.applyComponents(patch);

            assertTrue(stack.has(GCDataComponents.ROCKET_PART_STATS));
            assertTrue(stack.has(GCDataComponents.PART_INTEGRITY));
            assertEquals(stats, stack.get(GCDataComponents.ROCKET_PART_STATS));
            assertEquals(integrity, stack.get(GCDataComponents.PART_INTEGRITY));

            // Build patch removing PART_INTEGRITY
            DataComponentPatch removalPatch = DataComponentPatch.builder()
                    .remove(GCDataComponents.PART_INTEGRITY)
                    .build();

            stack.applyComponents(removalPatch);

            assertFalse(stack.has(GCDataComponents.PART_INTEGRITY));
            assertTrue(stack.has(GCDataComponents.ROCKET_PART_STATS));
            assertEquals(stats, stack.get(GCDataComponents.ROCKET_PART_STATS));
        }

        @Test
        @DisplayName("ItemStack copy-on-write defensive isolation")
        void testItemStackCopyDefensiveIsolation() {
            ItemStack source = new ItemStack(Items.DIAMOND_CHESTPLATE);
            source.set(GCDataComponents.PART_INTEGRITY, PartIntegrity.pristine(100.0));
            source.set(GCDataComponents.ROCKET_PART_STATS, RocketPartStats.ZERO.withThrust(500.0));

            ItemStack clone = source.copy();

            // Mutate source components
            source.set(GCDataComponents.PART_INTEGRITY, source.get(GCDataComponents.PART_INTEGRITY).damage(80.0));
            source.set(GCDataComponents.ROCKET_PART_STATS, source.get(GCDataComponents.ROCKET_PART_STATS).withThrust(999.0));

            // Verify clone is fully isolated
            PartIntegrity cloneIntegrity = clone.get(GCDataComponents.PART_INTEGRITY);
            assertNotNull(cloneIntegrity);
            assertEquals(100.0, cloneIntegrity.currentIntegrity());
            assertSame(PartDamageState.NOMINAL, cloneIntegrity.damageState());

            RocketPartStats cloneStats = clone.get(GCDataComponents.ROCKET_PART_STATS);
            assertNotNull(cloneStats);
            assertEquals(500.0, cloneStats.thrustKn());

            // Mutate clone components, verify source is unaffected
            clone.set(GCDataComponents.PART_INTEGRITY, cloneIntegrity.damage(30.0));
            assertEquals(20.0, source.get(GCDataComponents.PART_INTEGRITY).currentIntegrity());
            assertEquals(70.0, clone.get(GCDataComponents.PART_INTEGRITY).currentIntegrity());
        }

        @Test
        @DisplayName("ItemStack full NBT Codec roundtrip with bit-exact preservation")
        void testItemStackNbtCodecRoundtrip() {
            ItemStack originalStack = new ItemStack(Items.COPPER_BLOCK, 1);
            RocketPartStats stats = new RocketPartStats(1234.5678, 8765.4321, 243000L, 12.34, 1890.12);
            PartIntegrity integrity = PartIntegrity.pristine(150.0).damage(35.5);

            originalStack.set(GCDataComponents.ROCKET_PART_STATS, stats);
            originalStack.set(GCDataComponents.PART_INTEGRITY, integrity);

            Tag nbtTag = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, originalStack)
                    .getOrThrow(AssertionError::new);
            assertTrue(nbtTag instanceof CompoundTag);

            ItemStack restoredStack = ItemStack.CODEC.parse(NbtOps.INSTANCE, nbtTag)
                    .getOrThrow(AssertionError::new);

            assertEquals(originalStack.getItem(), restoredStack.getItem());
            assertEquals(originalStack.getCount(), restoredStack.getCount());

            RocketPartStats restoredStats = restoredStack.get(GCDataComponents.ROCKET_PART_STATS);
            assertNotNull(restoredStats);
            assertEquals(Double.doubleToRawLongBits(stats.thrustKn()), Double.doubleToRawLongBits(restoredStats.thrustKn()));
            assertEquals(Double.doubleToRawLongBits(stats.dryWeightKg()), Double.doubleToRawLongBits(restoredStats.dryWeightKg()));
            assertEquals(stats.fuelCapacityDroplets(), restoredStats.fuelCapacityDroplets());
            assertEquals(Double.doubleToRawLongBits(stats.pressureToleranceAtm()), Double.doubleToRawLongBits(restoredStats.pressureToleranceAtm()));
            assertEquals(Double.doubleToRawLongBits(stats.thermalToleranceKelvin()), Double.doubleToRawLongBits(restoredStats.thermalToleranceKelvin()));

            PartIntegrity restoredIntegrity = restoredStack.get(GCDataComponents.PART_INTEGRITY);
            assertNotNull(restoredIntegrity);
            assertEquals(Double.doubleToRawLongBits(integrity.currentIntegrity()), Double.doubleToRawLongBits(restoredIntegrity.currentIntegrity()));
            assertEquals(Double.doubleToRawLongBits(integrity.maxIntegrity()), Double.doubleToRawLongBits(restoredIntegrity.maxIntegrity()));
            assertSame(integrity.damageState(), restoredIntegrity.damageState());
        }
    }

    @Nested
    @DisplayName("5. Codec & StreamCodec Edge Case Stress")
    class CodecAndStreamCodecStressTests {

        @Test
        @DisplayName("StreamCodec roundtrip with extreme double bit patterns")
        void testStreamCodecExtremeDoubles() {
            RocketPartStats extreme = new RocketPartStats(
                    Double.MAX_VALUE,
                    Double.MIN_VALUE,
                    Long.MAX_VALUE,
                    Double.MIN_NORMAL,
                    Double.POSITIVE_INFINITY
            );

            ByteBuf buf = Unpooled.buffer();
            try {
                RocketPartStats.STREAM_CODEC.encode(buf, extreme);
                assertTrue(buf.readableBytes() > 0);

                RocketPartStats decoded = RocketPartStats.STREAM_CODEC.decode(buf);
                assertEquals(0, buf.readableBytes(), "All bytes must be consumed");

                assertEquals(Double.doubleToRawLongBits(extreme.thrustKn()), Double.doubleToRawLongBits(decoded.thrustKn()));
                assertEquals(Double.doubleToRawLongBits(extreme.dryWeightKg()), Double.doubleToRawLongBits(decoded.dryWeightKg()));
                assertEquals(extreme.fuelCapacityDroplets(), decoded.fuelCapacityDroplets());
                assertEquals(Double.doubleToRawLongBits(extreme.pressureToleranceAtm()), Double.doubleToRawLongBits(decoded.pressureToleranceAtm()));
                assertEquals(Double.doubleToRawLongBits(extreme.thermalToleranceKelvin()), Double.doubleToRawLongBits(decoded.thermalToleranceKelvin()));
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("PartDamageState Codec rejects invalid string tokens")
        void testInvalidEnumTokens() {
            JsonObject badJson = new JsonObject();
            badJson.addProperty("state", "non_existent_state");

            assertThrows(Exception.class, () ->
                    PartDamageState.CODEC.parse(JsonOps.INSTANCE, badJson.get("state")).getOrThrow(IllegalArgumentException::new)
            );
        }

        @Test
        @DisplayName("PartDamageState StreamCodec gracefully handles out-of-bounds IDs")
        void testStreamCodecOutOfBoundsHandling() {
            // OutOfBoundsStrategy.ZERO maps invalid ordinals to index 0 (NOMINAL)
            ByteBuf buf = Unpooled.buffer();
            try {
                buf.writeInt(9999); // Out-of-bounds ordinal
                PartDamageState decoded = PartDamageState.STREAM_CODEC.decode(buf);
                assertSame(PartDamageState.NOMINAL, decoded);
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("RocketPartStats Codec rejects JSON with missing required fields")
        void testMissingRequiredFieldsRejection() {
            JsonObject incompleteJson = new JsonObject();
            incompleteJson.addProperty("thrust_kn", 100.0);
            incompleteJson.addProperty("dry_weight_kg", 200.0);
            // Missing fuel_capacity_droplets, pressure_tolerance_atm, thermal_tolerance_kelvin

            assertThrows(Exception.class, () ->
                    RocketPartStats.CODEC.parse(JsonOps.INSTANCE, incompleteJson).getOrThrow(IllegalArgumentException::new)
            );
        }
    }
}
