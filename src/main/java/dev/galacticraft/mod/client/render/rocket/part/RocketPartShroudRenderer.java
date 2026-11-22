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

package dev.galacticraft.mod.client.render.rocket.part;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Procedural zero-allocation KSP-style interstage engine shroud renderer.
 *
 * <p>Whenever an adapter, interstage, thrust puck, fairing barrel, or inline decoupler sits directly
 * beneath an exposed rocket engine, synthesizes a double-walled, watertight polygonal fairing sleeve
 * connecting flush from the top rim of the adapter right up to the structural mounting deck of the
 * engine above. Uses exact {@code hull_sides(R)} facet progression and 1:1 vertical Region A texel
 * density so textures connect seamlessly with zero vertical stretching or Z-fighting.
 */
@Environment(EnvType.CLIENT)
public final class RocketPartShroudRenderer {
    private static final float OCT_INV_COS = (float) (1.0 / Math.cos(Math.PI / 8.0));
    private static final float SQRT2 = (float) Math.sqrt(2.0);
    private static final float PIXELS_PER_BLOCK = 16.0F;

    // Region A plain panel courses (skip top label courses k=0..1 so shroud displays clean seamless hull skin)
    private static final float HULL_V_BASE = 32.0F / 256.0F;
    private static final float HULL_V_MAX = 128.0F / 256.0F;
    private static final float HULL_V_PER_BLOCK = 16.0F / 256.0F;

    // Region B inner orthogrid truss & machined rim subpatches matching scripts/module_meshes.py
    private static final float TRUSS_U0 = 2.0F / 256.0F;
    private static final float TRUSS_V0 = 130.0F / 256.0F;
    private static final float TRUSS_U1 = 32.0F / 256.0F;
    private static final float TRUSS_V1 = 188.0F / 256.0F;

    private static final float STRUT_U0 = 96.0F / 256.0F;
    private static final float STRUT_V0 = 130.0F / 256.0F;
    private static final float STRUT_U1 = 126.0F / 256.0F;
    private static final float STRUT_V1 = 188.0F / 256.0F;

    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private RocketPartShroudRenderer() {
    }

    /**
     * Returns whether {@code lowerConfig} is an adapter/interstage/decoupler capable of generating
     * an automatic protective shroud around an engine mounted directly above it.
     */
    public static boolean supportsEngineShroud(@Nullable RocketPartConfig lowerConfig) {
        if (lowerConfig == null || lowerConfig.radial()) {
            return false;
        }
        String family = lowerConfig.familyId();
        if ("fairing_nose_cone".equals(family)) {
            return false;
        }
        return lowerConfig.category() == RocketPartCategory.ADAPTER
                || "decoupler_inline".equals(family);
    }

    /**
     * Returns whether {@code upperConfig} is an exposed engine whose nozzle/turbopump bay should be
     * enclosed by a shroud when mounted on top of an adapter.
     */
    public static boolean needsEngineShroud(@Nullable RocketPartConfig upperConfig) {
        if (upperConfig == null || upperConfig.radial() || upperConfig.category() != RocketPartCategory.ENGINE) {
            return false;
        }
        String family = upperConfig.familyId();
        // Full-body solid booster segments and recessed hull RCS rings already have complete exterior hulls
        return !family.startsWith("rcs_quad")
                && !family.equals("srb_goliath_segment")
                && !family.equals("srb_ares")
                && !family.equals("srb_dart");
    }

    /**
     * Computes the exact height (in Minecraft blocks from the engine's bottom exhaust plane {@code y = 0})
     * up to the underside of the engine's structural mounting deck/collar, matching {@code scripts/engine_meshes.py}.
     */
    public static float computeShroudHeightBlocks(@NotNull RocketPartConfig engineConfig) {
        float spanYPx = Math.max(1, engineConfig.heightBlocks()) * PIXELS_PER_BLOCK;
        String family = engineConfig.familyId();
        float deckHPx;
        if (family.contains("vulcain")) {
            deckHPx = 5.0F;
        } else if (family.contains("centaur")) {
            deckHPx = spanYPx <= 18.0F ? 2.5F : 3.5F;
        } else if (family.contains("nerva")) {
            deckHPx = 4.5F;
        } else if (family.contains("srb_goliath_nozzle")) {
            deckHPx = Math.min(13.5F, 0.34F * spanYPx);
        } else if (family.contains("octaweb9") || family.contains("novaf5")) {
            deckHPx = spanYPx >= 48.0F ? 6.0F : 5.0F;
        } else if (family.contains("starcluster33")) {
            deckHPx = 5.5F;
        } else if (family.contains("draco")) {
            deckHPx = 4.5F;
        } else if (family.contains("vasimr")) {
            deckHPx = 3.0F;
        } else if (family.contains("ion")) {
            deckHPx = 3.2F;
        } else {
            // falcon1d, merlinvac, raptor, generic single liquid engines
            deckHPx = 4.0F;
        }
        return Math.max(0.25F, (spanYPx - deckHPx) / PIXELS_PER_BLOCK);
    }

    /**
     * Mirrors {@code hull_sides(R)} from {@code scripts/module_meshes.py} so every shroud facet edge
     * aligns cleanly with adjacent adapters, tanks, and engine decks.
     */
    public static int hullSidesForRadiusBlocks(float radiusBlocks) {
        float rPx = radiusBlocks * PIXELS_PER_BLOCK;
        if (rPx <= 12.0F) {
            return 8;
        }
        if (rPx <= 28.0F) {
            return 12;
        }
        if (rPx <= 60.0F) {
            return 16;
        }
        return 20;
    }

    /**
     * Renders a double-walled, watertight engine shroud extending from the top of {@code lowerConfig}
     * ({@code y = lowerConfig.heightBlocks()}) up to the structural mounting collar of {@code engineConfig}.
     *
     * <p>Expects {@code poseStack} to already be transformed to the lower adapter's local origin and facing.
     */
    public static void render(
            @NotNull RocketPartConfig lowerConfig,
            @NotNull RocketPartConfig engineConfig,
            @NotNull ResourceLocation texture,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource consumers,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        if (!supportsEngineShroud(lowerConfig) || !needsEngineShroud(engineConfig)) {
            return;
        }
        float clampedAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
        int a = Math.round(clampedAlpha * 255.0F);
        if (a <= 0) {
            return;
        }
        int r = Math.round(Mth.clamp(red, 0.0F, 1.0F) * 255.0F);
        int g = Math.round(Mth.clamp(green, 0.0F, 1.0F) * 255.0F);
        int b = Math.round(Mth.clamp(blue, 0.0F, 1.0F) * 255.0F);

        float y0 = Math.max(1, lowerConfig.heightBlocks());
        float shroudHeight = computeShroudHeightBlocks(engineConfig);
        float y1 = y0 + shroudHeight;

        float rBot = Math.max(0.5F, lowerConfig.topDiameter() * 0.5F);
        float rTop = Math.max(0.5F, engineConfig.diameter() * 0.5F);
        float maxR = Math.max(rBot, rTop);
        int n = hullSidesForRadiusBlocks(maxR);
        boolean octagonal = n == 8;

        float wallT = Math.max(0.055F, Math.min(0.14F, Math.min(rBot, rTop) * 0.10F));
        float rBotIn = Math.max(0.18F, rBot - wallT);
        float rTopIn = Math.max(0.18F, rTop - wallT);

        Scratch s = SCRATCH.get();
        computeRing(rBot, n, octagonal, s.outBotX, s.outBotZ);
        computeRing(rTop, n, octagonal, s.outTopX, s.outTopZ);
        computeRing(rBotIn, n, octagonal, s.inBotX, s.inBotZ);
        computeRing(rTopIn, n, octagonal, s.inTopX, s.inTopZ);

        RenderType renderType = clampedAlpha < 0.99F
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = consumers.getBuffer(renderType);
        PoseStack.Pose pose = poseStack.last();

        // Tile outer hull wall in clean <= 6-block vertical bands so V stays strictly inside plain Region A
        // (V = 32/256 .. 128/256) with exact 1:1 vertical texel density and zero texture stretching.
        int bands = Math.max(1, Mth.ceil(shroudHeight / 6.0F));
        for (int band = 0; band < bands; band++) {
            float f0 = (float) band / (float) bands;
            float f1 = (float) (band + 1) / (float) bands;
            float by0 = Mth.lerp(f0, y0, y1);
            float by1 = Mth.lerp(f1, y0, y1);
            float bandH = by1 - by0;

            float vTop = HULL_V_BASE;
            float vBot = Math.min(HULL_V_MAX, HULL_V_BASE + bandH * HULL_V_PER_BLOCK);

            for (int j = 0; j < n; j++) {
                int jn = (j + 1) % n;
                float u0 = (float) j / (float) n;
                float u1 = (float) (j + 1) / (float) n;

                float bx0 = Mth.lerp(f0, s.outBotX[j], s.outTopX[j]);
                float bz0 = Mth.lerp(f0, s.outBotZ[j], s.outTopZ[j]);
                float bx1 = Mth.lerp(f0, s.outBotX[jn], s.outTopX[jn]);
                float bz1 = Mth.lerp(f0, s.outBotZ[jn], s.outTopZ[jn]);

                float tx0 = Mth.lerp(f1, s.outBotX[j], s.outTopX[j]);
                float tz0 = Mth.lerp(f1, s.outBotZ[j], s.outTopZ[j]);
                float tx1 = Mth.lerp(f1, s.outBotX[jn], s.outTopX[jn]);
                float tz1 = Mth.lerp(f1, s.outBotZ[jn], s.outTopZ[jn]);

                // Outward-facing outer fairing wall
                emitQuad(
                        pose, consumer,
                        bx0, by0, bz0, u0, vBot,
                        tx0, by1, tz0, u0, vTop,
                        tx1, by1, tz1, u1, vTop,
                        bx1, by0, bz1, u1, vBot,
                        packedLight, packedOverlay, r, g, b, a, s
                );
            }
        }

        // Inner structural orthogrid wall + top/bottom watertight rims
        for (int j = 0; j < n; j++) {
            int jn = (j + 1) % n;

            // Inward-facing inner wall (reversed winding so normals face inward)
            emitQuad(
                    pose, consumer,
                    s.inBotX[jn], y0, s.inBotZ[jn], TRUSS_U0, TRUSS_V1,
                    s.inTopX[jn], y1, s.inTopZ[jn], TRUSS_U0, TRUSS_V0,
                    s.inTopX[j], y1, s.inTopZ[j], TRUSS_U1, TRUSS_V0,
                    s.inBotX[j], y0, s.inBotZ[j], TRUSS_U1, TRUSS_V1,
                    packedLight, packedOverlay, r, g, b, a, s
            );

            // Top mating rim (facing +Y against underside of engine structural deck)
            emitQuad(
                    pose, consumer,
                    s.outTopX[j], y1, s.outTopZ[j], STRUT_U0, STRUT_V0,
                    s.inTopX[j], y1, s.inTopZ[j], STRUT_U0, STRUT_V1,
                    s.inTopX[jn], y1, s.inTopZ[jn], STRUT_U1, STRUT_V1,
                    s.outTopX[jn], y1, s.outTopZ[jn], STRUT_U1, STRUT_V0,
                    packedLight, packedOverlay, r, g, b, a, s
            );

            // Bottom mating rim (facing -Y flush with top of adapter)
            emitQuad(
                    pose, consumer,
                    s.inBotX[j], y0, s.inBotZ[j], STRUT_U0, STRUT_V0,
                    s.outBotX[j], y0, s.outBotZ[j], STRUT_U0, STRUT_V1,
                    s.outBotX[jn], y0, s.outBotZ[jn], STRUT_U1, STRUT_V1,
                    s.inBotX[jn], y0, s.inBotZ[jn], STRUT_U1, STRUT_V0,
                    packedLight, packedOverlay, r, g, b, a, s
            );
        }
    }

    private static void computeRing(float apothem, int n, boolean octagonal, float[] outX, float[] outZ) {
        float phaseDeg = (octagonal && n == 8) ? 22.5F : (180.0F / n);
        float rEff = (octagonal && n == 8) ? (apothem * OCT_INV_COS) : apothem;
        float lim = Math.max(0.0F, apothem - 1.0e-4F);
        float maxL1 = lim * SQRT2;

        for (int j = 0; j < n; j++) {
            double ang = Math.toRadians(phaseDeg + (360.0 * j) / n);
            float x = (float) (rEff * Math.cos(ang));
            float z = (float) (rEff * Math.sin(ang));
            // Exact octagonal containment clamp matching clamp_to_octagon in module_meshes.py
            float ax = Math.abs(x);
            float az = Math.abs(z);
            float scale = 1.0F;
            if (ax > lim && ax > 1.0e-6F) {
                scale = Math.min(scale, lim / ax);
            }
            if (az > lim && az > 1.0e-6F) {
                scale = Math.min(scale, lim / az);
            }
            float l1 = ax + az;
            if (l1 > maxL1 && l1 > 1.0e-6F) {
                scale = Math.min(scale, maxL1 / l1);
            }
            outX[j] = x * scale;
            outZ[j] = z * scale;
        }
    }

    private static void emitQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            int light,
            int overlay,
            int r,
            int g,
            int b,
            int a,
            Scratch s
    ) {
        // Compute outward unit normal via (p1 - p0) x (p2 - p0)
        float e1x = x1 - x0;
        float e1y = y1 - y0;
        float e1z = z1 - z0;
        float e2x = x2 - x0;
        float e2y = y2 - y0;
        float e2z = z2 - z0;
        float nx = e1y * e2z - e1z * e2y;
        float ny = e1z * e2x - e1x * e2z;
        float nz = e1x * e2y - e1y * e2x;
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1.0e-6F) {
            nx /= len;
            ny /= len;
            nz /= len;
        } else {
            nx = 0.0F;
            ny = 1.0F;
            nz = 0.0F;
        }

        Matrix4f mat = pose.pose();
        Vector3f pos = s.pos;
        Vector3f norm = s.normal;
        pose.transformNormal(nx, ny, nz, norm);

        mat.transformPosition(x0, y0, z0, pos);
        consumer.addVertex(pos.x, pos.y, pos.z)
                .setColor(r, g, b, a)
                .setUv(u0, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(norm.x, norm.y, norm.z);

        mat.transformPosition(x1, y1, z1, pos);
        consumer.addVertex(pos.x, pos.y, pos.z)
                .setColor(r, g, b, a)
                .setUv(u1, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(norm.x, norm.y, norm.z);

        mat.transformPosition(x2, y2, z2, pos);
        consumer.addVertex(pos.x, pos.y, pos.z)
                .setColor(r, g, b, a)
                .setUv(u2, v2)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(norm.x, norm.y, norm.z);

        mat.transformPosition(x3, y3, z3, pos);
        consumer.addVertex(pos.x, pos.y, pos.z)
                .setColor(r, g, b, a)
                .setUv(u3, v3)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(norm.x, norm.y, norm.z);
    }

    private static final class Scratch {
        final float[] outBotX = new float[24];
        final float[] outBotZ = new float[24];
        final float[] outTopX = new float[24];
        final float[] outTopZ = new float[24];
        final float[] inBotX = new float[24];
        final float[] inBotZ = new float[24];
        final float[] inTopX = new float[24];
        final float[] inTopZ = new float[24];
        final Vector3f pos = new Vector3f();
        final Vector3f normal = new Vector3f();
    }
}
