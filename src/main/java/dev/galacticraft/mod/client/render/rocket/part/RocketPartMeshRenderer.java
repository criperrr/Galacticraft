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
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Shared zero-allocation mesh renderer for {@link RocketPartRuntimeModel} instances.
 *
 * <p>Uses standard Minecraft {@link RenderType} buffers with primitive-only {@link VertexConsumer}
 * calls and thread-local pre-allocated scratch buffers for full Sodium 0.6, Iris, and
 * ImmediatelyFast compatibility.
 */
@Environment(EnvType.CLIENT)
public final class RocketPartMeshRenderer {
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private RocketPartMeshRenderer() {
    }

    /**
     * Renders a modular rocket part model with full white vertex tint and the specified opacity.
     */
    public static void render(
            RocketPartRuntimeModel model,
            ResourceLocation texture,
            PoseStack poseStack,
            MultiBufferSource consumers,
            int packedLight,
            int packedOverlay,
            float animTimeSeconds,
            boolean useLod,
            float alpha
    ) {
        render(
                model,
                texture,
                poseStack,
                consumers,
                packedLight,
                packedOverlay,
                animTimeSeconds,
                useLod,
                1.0F,
                1.0F,
                1.0F,
                alpha
        );
    }

    /**
     * Renders a modular rocket part model with explicit RGBA tint multipliers in {@code [0, 1]}.
     */
    public static void render(
            RocketPartRuntimeModel model,
            ResourceLocation texture,
            PoseStack poseStack,
            MultiBufferSource consumers,
            int packedLight,
            int packedOverlay,
            float animTimeSeconds,
            boolean useLod,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        render(
                model,
                texture,
                poseStack,
                consumers,
                packedLight,
                packedOverlay,
                animTimeSeconds,
                useLod,
                red,
                green,
                blue,
                alpha,
                1.0F
        );
    }

    /**
     * Renders a modular rocket part model with explicit RGBA tint multipliers and optional dynamic Y scale
     * for {@code shroud_sleeve} bones (KSP-style engine shroud interstages). Passing {@code <= 0} hides the sleeve.
     */
    public static void render(
            RocketPartRuntimeModel model,
            ResourceLocation texture,
            PoseStack poseStack,
            MultiBufferSource consumers,
            int packedLight,
            int packedOverlay,
            float animTimeSeconds,
            boolean useLod,
            float red,
            float green,
            float blue,
            float alpha,
            float shroudSleeveScaleY
    ) {
        if (model == null || texture == null || poseStack == null || consumers == null) {
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

        float duration = model.duration();
        float animTime = animTimeSeconds;
        if (duration > 0.0F && model.hasAnimations()) {
            animTime = animTimeSeconds % duration;
            if (animTime < 0.0F) {
                animTime += duration;
            }
        }

        int[] roots = model.rootBoneIndices();
        if (roots.length == 0) {
            return;
        }

        Scratch scratch = SCRATCH.get();

        // Pass 1: Opaque geometry submitted consecutively on a single RenderType buffer.
        if (model.hasOpaqueQuads()) {
            RenderType opaqueType = clampedAlpha < 0.99F
                    ? RenderType.entityTranslucent(texture)
                    : RenderType.entityCutoutNoCull(texture);
            VertexConsumer opaqueConsumer = consumers.getBuffer(opaqueType);
            for (int i = 0; i < roots.length; i++) {
                renderBoneTree(
                        model,
                        roots[i],
                        animTime,
                        duration,
                        useLod,
                        shroudSleeveScaleY,
                        poseStack,
                        opaqueConsumer,
                        packedLight,
                        packedOverlay,
                        r,
                        g,
                        b,
                        a,
                        false,
                        scratch
                );
            }
        }

        // Pass 2: Translucent geometry submitted consecutively after opaque pass completes.
        if (model.hasTranslucentQuads()) {
            VertexConsumer translucentConsumer = consumers.getBuffer(RenderType.entityTranslucent(texture));
            for (int i = 0; i < roots.length; i++) {
                renderBoneTree(
                        model,
                        roots[i],
                        animTime,
                        duration,
                        useLod,
                        shroudSleeveScaleY,
                        poseStack,
                        translucentConsumer,
                        packedLight,
                        packedOverlay,
                        r,
                        g,
                        b,
                        a,
                        true,
                        scratch
                );
            }
        }
    }

    /**
     * Converts horizontal block facing into the matching Y rotation angle in degrees, mirroring
     * {@code FootprintMode.rotateLocalPos} and {@code PlatformBlueprint.toWorldPos}.
     */
    public static float facingToYRotDegrees(@Nullable Direction facing) {
        if (facing == null) {
            return 0.0F;
        }
        return switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    /**
     * Rotates a local-space bounding box (relative to master block {@code (0, 0, 0)} with pivot at
     * block center {@code (0.5, y, 0.5)}) according to {@code facing}, matching
     * {@code FootprintMode.rotateLocalPos}.
     */
    public static AABB rotateLocalBounds(AABB localBounds, @Nullable Direction facing) {
        if (facing == null || facing == Direction.NORTH || facing == Direction.UP || facing == Direction.DOWN) {
            return localBounds;
        }
        double x0 = localBounds.minX - 0.5;
        double x1 = localBounds.maxX - 0.5;
        double z0 = localBounds.minZ - 0.5;
        double z1 = localBounds.maxZ - 0.5;
        return switch (facing) {
            case EAST -> new AABB(0.5 - z1, localBounds.minY, 0.5 + x0, 0.5 - z0, localBounds.maxY, 0.5 + x1);
            case SOUTH -> new AABB(0.5 - x1, localBounds.minY, 0.5 - z1, 0.5 - x0, localBounds.maxY, 0.5 - z0);
            case WEST -> new AABB(0.5 + z0, localBounds.minY, 0.5 - x1, 0.5 + z1, localBounds.maxY, 0.5 - x0);
            default -> localBounds;
        };
    }

    private static void renderBoneTree(
            RocketPartRuntimeModel model,
            int boneIndex,
            float animTime,
            float duration,
            boolean useLod,
            float shroudSleeveScaleY,
            PoseStack poseStack,
            VertexConsumer consumer,
            int light,
            int overlay,
            int red,
            int green,
            int blue,
            int alpha,
            boolean translucentPass,
            Scratch scratch
    ) {
        RocketPartRuntimeModel.Bone bone = model.bones()[boneIndex];
        if (useLod && bone.lodDetail) {
            return;
        }

        boolean isShroudSleeve = "shroud_sleeve".equals(bone.name);
        if (isShroudSleeve && shroudSleeveScaleY <= 0.001F) {
            return;
        }

        int[] children = model.childIndices(boneIndex);
        boolean scaleSleeve = isShroudSleeve && Math.abs(shroudSleeveScaleY - 1.0F) > 0.001F;
        boolean needsTransform = bone.hasOriginOffset || bone.hasRestRotation || bone.hasAnimPos || bone.hasAnimRot || scaleSleeve;
        if (needsTransform) {
            poseStack.pushPose();

            float tx = bone.originX;
            float ty = bone.originY;
            float tz = bone.originZ;
            if (bone.hasAnimPos) {
                bone.samplePosition(animTime, duration, scratch.animPos);
                tx += scratch.animPos[0];
                ty += scratch.animPos[1];
                tz += scratch.animPos[2];
            }
            if (tx != 0.0F || ty != 0.0F || tz != 0.0F) {
                poseStack.translate(tx, ty, tz);
            }

            float rx = bone.rotX;
            float ry = bone.rotY;
            float rz = bone.rotZ;
            if (bone.hasAnimRot) {
                bone.sampleRotation(animTime, duration, scratch.animRot);
                rx += scratch.animRot[0];
                ry += scratch.animRot[1];
                rz += scratch.animRot[2];
            }
            // Match Blockbench 5.0 ZYX Euler rotation order (Rz * Ry * Rx).
            if (rz != 0.0F) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(rz));
            }
            if (ry != 0.0F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(ry));
            }
            if (rx != 0.0F) {
                poseStack.mulPose(Axis.XP.rotationDegrees(rx));
            }
            if (scaleSleeve) {
                poseStack.scale(1.0F, shroudSleeveScaleY, 1.0F);
            }
        }

        float[] quads = translucentPass ? bone.translucentQuads : bone.opaqueQuads;
        if (quads.length > 0) {
            emitQuads(quads, poseStack.last(), consumer, light, overlay, red, green, blue, alpha, scratch);
        }

        for (int i = 0; i < children.length; i++) {
            renderBoneTree(
                    model,
                    children[i],
                    animTime,
                    duration,
                    useLod,
                    shroudSleeveScaleY,
                    poseStack,
                    consumer,
                    light,
                    overlay,
                    red,
                    green,
                    blue,
                    alpha,
                    translucentPass,
                    scratch
            );
        }

        if (needsTransform) {
            poseStack.popPose();
        }
    }

    private static void emitQuads(
            float[] quads,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int light,
            int overlay,
            int red,
            int green,
            int blue,
            int alpha,
            Scratch scratch
    ) {
        int len = quads.length;
        Matrix4f mat = pose.pose();
        Vector3f pos = scratch.pos;
        Vector3f norm = scratch.normal;

        for (int i = 0; i < len; i += RocketPartRuntimeModel.FLOATS_PER_QUAD) {
            pose.transformNormal(quads[i + 20], quads[i + 21], quads[i + 22], norm);
            float nx = norm.x;
            float ny = norm.y;
            float nz = norm.z;

            mat.transformPosition(quads[i], quads[i + 1], quads[i + 2], pos);
            consumer.addVertex(pos.x, pos.y, pos.z)
                    .setColor(red, green, blue, alpha)
                    .setUv(quads[i + 3], quads[i + 4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(nx, ny, nz);

            mat.transformPosition(quads[i + 5], quads[i + 6], quads[i + 7], pos);
            consumer.addVertex(pos.x, pos.y, pos.z)
                    .setColor(red, green, blue, alpha)
                    .setUv(quads[i + 8], quads[i + 9])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(nx, ny, nz);

            mat.transformPosition(quads[i + 10], quads[i + 11], quads[i + 12], pos);
            consumer.addVertex(pos.x, pos.y, pos.z)
                    .setColor(red, green, blue, alpha)
                    .setUv(quads[i + 13], quads[i + 14])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(nx, ny, nz);

            mat.transformPosition(quads[i + 15], quads[i + 16], quads[i + 17], pos);
            consumer.addVertex(pos.x, pos.y, pos.z)
                    .setColor(red, green, blue, alpha)
                    .setUv(quads[i + 18], quads[i + 19])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(nx, ny, nz);
        }
    }

    private static final class Scratch {
        final float[] animPos = new float[3];
        final float[] animRot = new float[3];
        final Vector3f pos = new Vector3f();
        final Vector3f normal = new Vector3f();
    }
}
