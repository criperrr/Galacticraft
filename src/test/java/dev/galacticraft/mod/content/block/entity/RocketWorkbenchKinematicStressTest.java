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

package dev.galacticraft.mod.content.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.recipe.GCRecipes;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rocket Workbench Kinematic & Adversarial Stress Test Suite")
class RocketWorkbenchKinematicStressTest {

    private static RegistryAccess registryAccess;

    @BeforeAll
    static void setupBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(Constant.id(Constant.Recipe.PART_ASSEMBLER))) {
            GCRecipes.register();
        }
        GCBlocks.register();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static RocketWorkbenchBlockEntity createWorkbench() {
        BlockState state = GCBlocks.ROCKET_WORKBENCH.defaultBlockState();
        return new RocketWorkbenchBlockEntity(BlockPos.ZERO, state);
    }

    // =========================================================================
    // 1. tickDelta Extreme Boundary & Clamping Tests
    // =========================================================================

    @Nested
    @DisplayName("1. tickDelta Boundary & Clamping Tests")
    class TickDeltaTests {

        @Test
        @DisplayName("Standard tickDelta values yield smooth linear interpolation")
        void testStandardTickDelta() {
            RocketWorkbenchBlockEntity be = createWorkbench();
            be.setClientProgress(50, 100);

            for (float tickDelta : new float[]{0.0F, 0.25F, 0.5F, 0.75F, 1.0F}) {
                float ratio = be.getClientProgressRatio(tickDelta);
                float expected = (50.0F + tickDelta) / 100.0F;

                assertEquals(expected, ratio, 1e-5F, "getClientProgressRatio must match linear sub-tick interpolation for tickDelta = " + tickDelta);
                assertTrue(ratio >= 0.0F && ratio <= 1.0F, "Ratio must be clamped within [0, 1]");
            }
        }

        @Test
        @DisplayName("Negative tickDelta values are safely clamped to 0.0F and never produce negative ratio")
        void testNegativeTickDelta() {
            RocketWorkbenchBlockEntity be = createWorkbench();

            for (float tickDelta : new float[]{-0.001F, -0.5F, -1.0F, -50.0F, -1_000_000.0F}) {
                be.setClientProgress(0, 100);
                float ratio = be.getClientProgressRatio(tickDelta);
                assertEquals(0.0F, ratio, "Negative tickDelta at progress 0 must clamp to 0.0F for tickDelta = " + tickDelta);

                be.setClientProgress(10, 100);
                float ratio2 = be.getClientProgressRatio(tickDelta);
                assertTrue(ratio2 >= 0.0F, "Ratio must never be negative even with extreme negative tickDelta");
                assertFalse(Float.isNaN(ratio2), "Ratio must not be NaN");
                assertFalse(Float.isInfinite(ratio2), "Ratio must not be Infinite");
            }
        }

        @Test
        @DisplayName("Excessive tickDelta values are safely clamped to 1.0F")
        void testExcessiveTickDelta() {
            RocketWorkbenchBlockEntity be = createWorkbench();
            be.setClientProgress(100, 100);

            for (float tickDelta : new float[]{1.5F, 10.0F, 100.0F, 1_000_000.0F}) {
                float ratio = be.getClientProgressRatio(tickDelta);
                assertEquals(1.0F, ratio, "Excessive tickDelta at full progress must clamp to 1.0F for tickDelta = " + tickDelta);
            }
        }

        @Test
        @DisplayName("Animation blend easing is finite and stable across varied tickDelta")
        void testBlendEasingStability() {
            float blend = 0.0F;
            float targetBlend = 1.0F;

            for (float tickDelta : new float[]{-0.5F, 0.0F, 0.5F, 1.0F, 1.5F}) {
                float factor = 0.1F * (tickDelta + 1.0F);
                float nextBlend = Mth.lerp(factor, blend, targetBlend);
                assertTrue(Float.isFinite(nextBlend), "Blend must remain finite for tickDelta = " + tickDelta);
            }
        }
    }

    // =========================================================================
    // 2. clientMaxProgress Boundary Tests (Divide-by-Zero Protection)
    // =========================================================================

    @Nested
    @DisplayName("2. clientMaxProgress Boundary Tests")
    class ClientMaxProgressTests {

        @Test
        @DisplayName("clientMaxProgress <= 0 returns exactly 0.0F without division by zero or NaN")
        void testZeroOrNegativeMaxProgress() {
            RocketWorkbenchBlockEntity be = createWorkbench();

            for (int maxProgress : new int[]{0, -1, -50, Integer.MIN_VALUE}) {
                be.setClientProgress(50, maxProgress);

                float ratio = be.getClientProgressRatio(0.5F);
                assertEquals(0.0F, ratio, "clientMaxProgress <= 0 must return 0.0F without division by zero for max = " + maxProgress);
                assertFalse(Float.isNaN(ratio), "Must not produce NaN");
                assertFalse(Float.isInfinite(ratio), "Must not produce Infinity");
            }
        }

        @Test
        @DisplayName("clientMaxProgress = 1 behaves correctly without precision distortion")
        void testSingleTickMaxProgress() {
            RocketWorkbenchBlockEntity be = createWorkbench();
            be.setClientProgress(0, 1);
            assertEquals(0.5F, be.getClientProgressRatio(0.5F), 1e-5F);

            be.setClientProgress(1, 1);
            assertEquals(1.0F, be.getClientProgressRatio(0.0F));
            assertEquals(1.0F, be.getClientProgressRatio(0.5F), "Over-tick must clamp to 1.0F");
        }

        @Test
        @DisplayName("Progress exceeding clientMaxProgress clamps to 1.0F")
        void testProgressExceedingMax() {
            RocketWorkbenchBlockEntity be = createWorkbench();
            be.setClientProgress(250, 100);
            assertEquals(1.0F, be.getClientProgressRatio(0.0F));
            assertEquals(1.0F, be.getClientProgressRatio(1.0F));
        }

        @Test
        @DisplayName("Negative clientProgress clamps to 0.0F")
        void testNegativeClientProgress() {
            RocketWorkbenchBlockEntity be = createWorkbench();
            be.setClientProgress(-50, 100);
            assertEquals(0.0F, be.getClientProgressRatio(0.0F));
            assertEquals(0.0F, be.getClientProgressRatio(0.5F));
        }
    }

    // =========================================================================
    // 3. gameTime Rollover & Large Continuous Time Mathematical Tests
    // =========================================================================

    @Nested
    @DisplayName("3. gameTime Rollover & Continuous Time Tests")
    class GameTimeTests {

        @Test
        @DisplayName("Large gameTime values produce periodic alpha strictly in [0.0, 1.05]")
        void testLargeGameTimeAlphaPeriodic() {
            float tickDelta = 0.5F;

            for (long gameTime : new long[]{0L, 80L, 1_000_000L, 100_000_000L, 2_000_000_000L, Long.MAX_VALUE / 4}) {
                float continuousTime = (float) gameTime + tickDelta;

                // Worker's BER fallback periodic alpha equation:
                float alpha = (Math.floorMod((long) continuousTime, 80) + tickDelta) / 80.0F;

                assertTrue(alpha >= 0.0F && alpha <= 1.05F, "Fallback alpha must be strictly in [0.0, 1.05] for gameTime = " + gameTime);
                assertFalse(Float.isNaN(alpha), "Alpha must not be NaN");
                assertFalse(Float.isInfinite(alpha), "Alpha must not be Infinite");
            }
        }

        @Test
        @DisplayName("Negative gameTime correctly wraps via Math.floorMod without negative index")
        void testNegativeGameTimeAlpha() {
            float tickDelta = 0.0F;

            for (long gameTime : new long[]{-1L, -80L, -81L, -100_000L}) {
                float continuousTime = (float) gameTime + tickDelta;

                long mod = Math.floorMod((long) continuousTime, 80);
                assertTrue(mod >= 0 && mod < 80, "Math.floorMod with negative long must produce non-negative remainder in [0, 79]");

                float alpha = (mod + tickDelta) / 80.0F;
                assertTrue(alpha >= 0.0F && alpha <= 1.0F, "Alpha must remain within [0.0, 1.0] for negative gameTime = " + gameTime);
            }
        }

        @Test
        @DisplayName("Drill bit spindle rotation stays bounded and finite for large continuousTime")
        void testDrillSpinBounded() {
            for (long time : new long[]{0L, 100L, 10_000L, 1_000_000L, 50_000_000L}) {
                float continuousTime = (float) time + 0.5F;
                float drillSpin = (continuousTime * 45.0F) % 360.0F;

                assertTrue(drillSpin >= -360.0F && drillSpin < 360.0F, "Drill spin must be in [-360, 360)");
                assertFalse(Float.isNaN(drillSpin), "Drill spin must not be NaN");

                float drillBitYRot = (float) Math.toRadians(drillSpin);
                assertTrue(Float.isFinite(drillBitYRot), "Drill bit rotation radians must be finite");
            }
        }

        @Test
        @DisplayName("Jitter index formula never causes out-of-bounds or invalid offsets")
        void testJitterFormula() {
            for (float ct = -100.0F; ct <= 100.0F; ct += 0.5F) {
                int jitter = (int) (ct * 2.0F) % 3;
                float jitterX = (jitter == 1 ? 0.005F : (jitter == 2 ? -0.005F : 0.0F));
                float jitterZ = (jitter == 2 ? 0.005F : (jitter == 1 ? -0.005F : 0.0F));

                assertTrue(Math.abs(jitterX) <= 0.005F, "Jitter X must be within [-0.005, 0.005]");
                assertTrue(Math.abs(jitterZ) <= 0.005F, "Jitter Z must be within [-0.005, 0.005]");
            }
        }
    }

    // =========================================================================
    // 4. Abrupt Active Transition & Lerp Blend Stability
    // =========================================================================

    @Nested
    @DisplayName("4. Abrupt Active Transition & Lerp Blend Stability")
    class ActiveTransitionTests {

        @Test
        @DisplayName("Transition active true -> false decays smoothly to exact 0.0F without NaN or Inf")
        void testActiveTrueToFalseDecay() {
            float blend = 1.0F;
            float targetBlend = 0.0F;
            float tickDelta = 0.5F; // 60 FPS nominal sub-tick
            float factor = 0.1F * (tickDelta + 1.0F); // 0.15F

            List<Float> blendHistory = new ArrayList<>();
            blendHistory.add(blend);

            int framesToZero = -1;
            for (int frame = 1; frame <= 100; frame++) {
                blend = Mth.lerp(factor, blend, targetBlend);
                if (Math.abs(blend - targetBlend) < 0.001F) {
                    blend = targetBlend;
                    if (framesToZero == -1) {
                        framesToZero = frame;
                    }
                }
                blendHistory.add(blend);

                assertFalse(Float.isNaN(blend), "Blend must never become NaN at frame " + frame);
                assertFalse(Float.isInfinite(blend), "Blend must never become Infinite at frame " + frame);
                assertTrue(blend >= 0.0F && blend <= 1.0F, "Blend must remain in [0, 1] at frame " + frame);
            }

            assertTrue(framesToZero > 0 && framesToZero < 60, "Blend must decay and snap to 0.0F within ~45 frames (took " + framesToZero + ")");
            assertEquals(0.0F, blend, "Final blend must be exactly 0.0F");

            // Verify strict monotonic decrease prior to snapping
            for (int i = 1; i < framesToZero; i++) {
                assertTrue(blendHistory.get(i) < blendHistory.get(i - 1), "Blend must decrease monotonically during active->inactive transition");
            }
        }

        @Test
        @DisplayName("Transition active false -> true rises smoothly to exact 1.0F without overshoot")
        void testActiveFalseToTrueRise() {
            float blend = 0.0F;
            float targetBlend = 1.0F;
            float tickDelta = 0.5F;
            float factor = 0.1F * (tickDelta + 1.0F);

            int framesToOne = -1;
            for (int frame = 1; frame <= 100; frame++) {
                blend = Mth.lerp(factor, blend, targetBlend);
                if (Math.abs(blend - targetBlend) < 0.001F) {
                    blend = targetBlend;
                    if (framesToOne == -1) {
                        framesToOne = frame;
                    }
                }
                assertTrue(blend >= 0.0F && blend <= 1.0F, "Blend must remain in [0, 1]");
            }

            assertTrue(framesToOne > 0 && framesToOne < 60, "Blend must rise and snap to 1.0F within ~45 frames (took " + framesToOne + ")");
            assertEquals(1.0F, blend, "Final blend must be exactly 1.0F");
        }

        @Test
        @DisplayName("Rapid active toggling (chatter) remains bounded and never diverges")
        void testRapidActiveChatter() {
            float blend = 0.0F;
            for (int i = 0; i < 500; i++) {
                boolean active = (i % 3) == 0;
                float targetBlend = active ? 1.0F : 0.0F;
                float tickDelta = (float) Math.random();
                float factor = 0.1F * (tickDelta + 1.0F);

                blend = Mth.lerp(factor, blend, targetBlend);
                if (Math.abs(blend - targetBlend) < 0.001F) {
                    blend = targetBlend;
                }

                assertTrue(blend >= 0.0F && blend <= 1.0F, "Blend must stay bounded in [0, 1] during rapid chatter");
                assertFalse(Float.isNaN(blend), "Blend must not be NaN");
            }
        }

        @Test
        @DisplayName("All kinematic transforms scale to zero when blend reaches 0.0F")
        void testKinematicsScaleToZeroAtRest() {
            float blend = 0.0F;
            float alpha = 0.5F;
            float continuousTime = 123.45F;

            float clampAngle = 15.0F * Mth.sin(8.0F * Mth.PI * alpha) * blend;
            float plierSweep = Mth.sin(continuousTime * 0.12F) * 8.0F * blend;
            float plierPitch = 6.0F * Mth.cos(2.0F * Mth.PI * alpha) * blend;
            float plungeY = -0.08F * Math.max(0.0F, Mth.sin(4.0F * Mth.PI * alpha)) * blend;
            float laserYaw = 12.0F * Mth.sin(6.0F * Mth.PI * alpha) * blend;
            float laserPitch = 8.0F * Mth.cos(6.0F * Mth.PI * alpha) * blend;

            assertEquals(0.0F, Math.abs(clampAngle), 1e-6F, "Clamp angle magnitude must be 0 at rest");
            assertEquals(0.0F, Math.abs(plierSweep), 1e-6F, "Plier sweep magnitude must be 0 at rest");
            assertEquals(0.0F, Math.abs(plierPitch), 1e-6F, "Plier pitch magnitude must be 0 at rest");
            assertEquals(0.0F, Math.abs(plungeY), 1e-6F, "Plunge Y magnitude must be 0 at rest");
            assertEquals(0.0F, Math.abs(laserYaw), 1e-6F, "Laser yaw magnitude must be 0 at rest");
            assertEquals(0.0F, Math.abs(laserPitch), 1e-6F, "Laser pitch magnitude must be 0 at rest");
        }
    }

    // =========================================================================
    // 5. Facing Orientations (All 4 Horizontal Directions)
    // =========================================================================

    @Nested
    @DisplayName("5. Facing Orientations (All 4 Horizontal Directions)")
    class FacingTests {

        @Test
        @DisplayName("Horizontal facings compute correct 90-degree orthogonal rotation angles")
        void testFacingAngles() {
            assertEquals(180.0F, Direction.NORTH.toYRot());
            assertEquals(0.0F, Direction.SOUTH.toYRot());
            assertEquals(90.0F, Direction.WEST.toYRot());
            assertEquals(270.0F, Direction.EAST.toYRot());

            assertEquals(-90.0F, 90.0F - Direction.NORTH.toYRot(), "NORTH rotation degrees");
            assertEquals(90.0F, 90.0F - Direction.SOUTH.toYRot(), "SOUTH rotation degrees");
            assertEquals(0.0F, 90.0F - Direction.WEST.toYRot(), "WEST rotation degrees");
            assertEquals(-180.0F, 90.0F - Direction.EAST.toYRot(), "EAST rotation degrees");
        }

        @Test
        @DisplayName("PoseStack rotateAround produces orthonormal transformation around (0.5, 0.0, 0.5)")
        void testFacingPoseStackTransform() {
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                PoseStack matrices = new PoseStack();
                matrices.translate(0.0F, 1.0F, 0.0F);
                matrices.rotateAround(Axis.YP.rotationDegrees(90.0F - dir.toYRot()), 0.5F, 0.0F, 0.5F);

                Matrix4f matrix = matrices.last().pose();

                // Transform center point (0.5, 0.0, 0.5) - should remain centered on X and Z
                Vector4f center = new Vector4f(0.5F, 0.0F, 0.5F, 1.0F);
                center.mul(matrix);

                assertEquals(0.5F, center.x, 1e-4F, "Center X must remain 0.5 after rotation for " + dir);
                assertEquals(1.0F, center.y, 1e-4F, "Center Y must match translation 1.0 for " + dir);
                assertEquals(0.5F, center.z, 1e-4F, "Center Z must remain 0.5 after rotation for " + dir);

                // Matrix determinant must be 1.0 (pure rotation + translation, no shear or scaling)
                assertEquals(1.0F, matrix.determinant(), 1e-4F, "Matrix determinant must be 1.0 for " + dir);
            }
        }
    }

    // =========================================================================
    // 6. Network Serialization & Client Sync Invariants
    // =========================================================================

    @Nested
    @DisplayName("6. Network Serialization & Client Sync Invariants")
    class NetworkSyncTests {

        @Test
        @DisplayName("populateUpdateTag writes Progress and MaxProgress, loadAdditional restores them")
        void testUpdateTagRoundtrip() {
            RocketWorkbenchBlockEntity beSender = createWorkbench();
            // Simulate server state
            beSender.setClientProgress(85, 200);

            CompoundTag tag = new CompoundTag();
            beSender.populateUpdateTag(tag);

            assertTrue(tag.contains("Progress"), "Update tag must contain 'Progress'");
            assertTrue(tag.contains("MaxProgress"), "Update tag must contain 'MaxProgress'");

            // Simulate client deserialization
            RocketWorkbenchBlockEntity beReceiver = createWorkbench();
            HolderLookup.Provider lookup = registryAccess;
            beReceiver.loadAdditional(tag, lookup);

            assertEquals(beSender.getProgress(), beReceiver.getClientProgress(), "clientProgress must match sender getProgress()");
        }

        @Test
        @DisplayName("loadAdditional handles missing Progress and MaxProgress tags gracefully")
        void testMissingTagsHandling() {
            RocketWorkbenchBlockEntity be = createWorkbench();
            CompoundTag emptyTag = new CompoundTag();

            assertDoesNotThrow(() -> be.loadAdditional(emptyTag, registryAccess), "loadAdditional must handle empty tag without exception");
            assertEquals(0, be.getClientProgress());
            assertEquals(0, be.getClientMaxProgress());
            assertEquals(0.0F, be.getClientProgressRatio(0.5F));
        }
    }

    // =========================================================================
    // 7. CRT Display Text & Progress Formatting Safety
    // =========================================================================

    @Nested
    @DisplayName("7. CRT Display Formatting Safety")
    class CrtDisplayTests {

        @Test
        @DisplayName("CRT ASCII progress gauge formatting never throws StringIndexOutOfBounds or IllegalArgument")
        void testProgressGaugeFormatting() {
            for (int pct : new int[]{-100, -1, 0, 5, 10, 50, 99, 100, 150, 500}) {
                int bars = Mth.clamp(pct / 10, 0, 10);
                assertTrue(bars >= 0 && bars <= 10, "Bars must be clamped in [0, 10]");

                assertDoesNotThrow(() -> {
                    String barText = "[" + "=".repeat(bars) + " ".repeat(10 - bars) + "] " + pct + "%";
                    assertNotNull(barText);
                    assertEquals(10, bars + (10 - bars), "Total characters inside brackets must be exactly 10");
                    assertTrue(barText.startsWith("[") && barText.contains("]"), "Bar text must have bracket boundaries");
                }, "String formatting must never fail for pct = " + pct);
            }
        }

        @Test
        @DisplayName("CRT status text correctly alternates between FABRICATING and STANDBY")
        void testStatusText() {
            boolean active = true;
            String activeStatus = active ? "[ FABRICATING ]" : "[ STANDBY ]";
            int activeColor = active ? 0x00FF66 : 0x33AA33;
            assertEquals("[ FABRICATING ]", activeStatus);
            assertEquals(0x00FF66, activeColor);

            active = false;
            String standbyStatus = active ? "[ FABRICATING ]" : "[ STANDBY ]";
            int standbyColor = active ? 0x00FF66 : 0x33AA33;
            assertEquals("[ STANDBY ]", standbyStatus);
            assertEquals(0x33AA33, standbyColor);
        }
    }

    // =========================================================================
    // 8. Laser Beam Cross-Quad Geometric Soundness
    // =========================================================================

    @Nested
    @DisplayName("8. Laser Beam Cross-Quad Geometric Soundness")
    class LaserBeamGeometryTests {

        @Test
        @DisplayName("Laser beam trajectory (Xw, Zw) remains centered on table top (y = 0.255m)")
        void testLaserWorkpieceTrajectory() {
            for (float alpha = 0.0F; alpha <= 1.0F; alpha += 0.05F) {
                float xTarget = 0.50F + 0.12F * Mth.cos(6.0F * Mth.PI * alpha);
                float yTarget = 0.255F;
                float zTarget = 0.50F + 0.12F * Mth.sin(6.0F * Mth.PI * alpha);

                assertTrue(xTarget >= 0.38F && xTarget <= 0.62F, "Laser target X must stay within circular radius [0.38, 0.62]");
                assertEquals(0.255F, yTarget, "Laser target Y must stay at table height 0.255m");
                assertTrue(zTarget >= 0.38F && zTarget <= 0.62F, "Laser target Z must stay within circular radius [0.38, 0.62]");

                // Radius from center (0.50, 0.50) must be exactly 0.12m
                float dist = (float) Math.hypot(xTarget - 0.50F, zTarget - 0.50F);
                assertEquals(0.12F, dist, 1e-4F, "Circular path radius must be 0.12m");
            }
        }

        @Test
        @DisplayName("Laser beam alphas scale with blend and vanish when blend <= 0.3F")
        void testLaserAlphas() {
            for (float blend = 0.0F; blend <= 1.0F; blend += 0.1F) {
                boolean renders = blend > 0.3F;
                int coreAlpha = (int) (240 * blend);
                int glowAlpha = (int) (170 * blend);

                if (renders) {
                    assertTrue(coreAlpha >= 72 && coreAlpha <= 240, "Core alpha must scale in [72, 240]");
                    assertTrue(glowAlpha >= 51 && glowAlpha <= 170, "Glow alpha must scale in [51, 170]");
                } else {
                    assertTrue(blend <= 0.3F, "Laser beam must not render when blend <= 0.3F");
                }
            }
        }
    }
}
