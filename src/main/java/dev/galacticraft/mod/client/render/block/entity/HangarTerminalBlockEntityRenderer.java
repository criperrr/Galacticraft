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

package dev.galacticraft.mod.client.render.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import dev.galacticraft.mod.client.render.platform.PlatformModelLoader;
import dev.galacticraft.mod.client.render.platform.PlatformRuntimeModel;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Renders the full animated 3D platform structure when a {@link HangarTerminalBlockEntity} is formed.
 *
 * <p>Uses standard Minecraft {@link RenderType} buffers with primitive-only {@link VertexConsumer}
 * calls and pre-allocated scratch vectors for zero per-frame allocations and full Sodium / Iris /
 * ImmediatelyFast compatibility.
 */
@Environment(EnvType.CLIENT)
public class HangarTerminalBlockEntityRenderer implements BlockEntityRenderer<HangarTerminalBlockEntity> {
    private static final int VIEW_DISTANCE = 256;

    // Pre-allocated render-thread scratch buffers for zero-allocation vertex submission and FK sampling.
    private final float[] animPosScratch = new float[3];
    private final float[] animRotScratch = new float[3];
    private final Vector3f posScratch = new Vector3f();
    private final Vector3f normalScratch = new Vector3f();

    public HangarTerminalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(HangarTerminalBlockEntity blockEntity) {
        return blockEntity.isFormed();
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public void render(
            HangarTerminalBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!blockEntity.isFormed()) {
            return;
        }

        PlatformType type = blockEntity.getPlatformType();
        PlatformSize size = blockEntity.getPlatformSize();
        if (type == null || size == null) {
            return;
        }

        PlatformRuntimeModel model = PlatformModelLoader.INSTANCE.getModel(size.modelKey(type));
        if (model == null) {
            return;
        }

        Level level = blockEntity.getLevel();
        BlockPos terminalPos = blockEntity.getBlockPos();
        BlockPos padCenter = blockEntity.getPadCenterPos();
        if (padCenter == null) {
            padCenter = terminalPos;
        }

        int light = level != null
                ? LevelRenderer.getLightColor(level, padCenter.above())
                : packedLight;

        float duration = model.duration();
        float animTime = 0.0F;
        if (level != null && duration > 0.0F) {
            double seconds = ((double) level.getGameTime() + (double) partialTick) / 20.0;
            animTime = (float) (seconds % (double) duration);
            if (animTime < 0.0F) {
                animTime += duration;
            }
        }

        int offsetX = padCenter.getX() - terminalPos.getX();
        int offsetY = padCenter.getY() - terminalPos.getY();
        int offsetZ = padCenter.getZ() - terminalPos.getZ();

        poseStack.pushPose();
        poseStack.translate(offsetX + 0.5F, offsetY, offsetZ + 0.5F);

        float yRotDeg = facingToYRotDegrees(blockEntity.getFacing());
        if (yRotDeg != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotDeg));
        }

        int[] roots = model.rootBoneIndices();

        // Pass 1: Opaque geometry submitted consecutively on a single RenderType buffer.
        VertexConsumer opaqueConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(model.texture()));
        for (int i = 0; i < roots.length; i++) {
            this.renderBoneTree(model, roots[i], animTime, duration, poseStack, opaqueConsumer, light, OverlayTexture.NO_OVERLAY, false);
        }

        // Pass 2: Translucent geometry submitted consecutively after opaque pass completes.
        if (model.hasTranslucentQuads()) {
            VertexConsumer translucentConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(model.texture()));
            for (int i = 0; i < roots.length; i++) {
                this.renderBoneTree(model, roots[i], animTime, duration, poseStack, translucentConsumer, light, OverlayTexture.NO_OVERLAY, true);
            }
        }

        poseStack.popPose();
    }

    private void renderBoneTree(
            PlatformRuntimeModel model,
            int boneIndex,
            float animTime,
            float duration,
            PoseStack poseStack,
            VertexConsumer consumer,
            int light,
            int overlay,
            boolean translucentPass
    ) {
        PlatformRuntimeModel.Bone bone = model.bones()[boneIndex];
        int[] children = model.childIndices(boneIndex);

        boolean needsTransform = bone.hasOriginOffset || bone.hasRestRotation || bone.hasAnimPos || bone.hasAnimRot;
        if (needsTransform) {
            poseStack.pushPose();

            float tx = bone.originX;
            float ty = bone.originY;
            float tz = bone.originZ;
            if (bone.hasAnimPos) {
                bone.samplePosition(animTime, duration, this.animPosScratch);
                tx += this.animPosScratch[0];
                ty += this.animPosScratch[1];
                tz += this.animPosScratch[2];
            }
            if (tx != 0.0F || ty != 0.0F || tz != 0.0F) {
                poseStack.translate(tx, ty, tz);
            }

            float rx = bone.rotX;
            float ry = bone.rotY;
            float rz = bone.rotZ;
            if (bone.hasAnimRot) {
                bone.sampleRotation(animTime, duration, this.animRotScratch);
                rx += this.animRotScratch[0];
                ry += this.animRotScratch[1];
                rz += this.animRotScratch[2];
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
        }

        float[] quads = translucentPass ? bone.translucentQuads : bone.opaqueQuads;
        if (quads.length > 0) {
            this.emitQuads(quads, poseStack.last(), consumer, light, overlay);
        }

        for (int i = 0; i < children.length; i++) {
            this.renderBoneTree(model, children[i], animTime, duration, poseStack, consumer, light, overlay, translucentPass);
        }

        if (needsTransform) {
            poseStack.popPose();
        }
    }

    private void emitQuads(float[] quads, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay) {
        int len = quads.length;
        Matrix4f mat = pose.pose();
        Vector3f pos = this.posScratch;
        Vector3f norm = this.normalScratch;

        for (int i = 0; i < len; i += PlatformRuntimeModel.FLOATS_PER_QUAD) {
            pose.transformNormal(quads[i + 20], quads[i + 21], quads[i + 22], norm);
            float nx = norm.x;
            float ny = norm.y;
            float nz = norm.z;

            mat.transformPosition(quads[i], quads[i + 1], quads[i + 2], pos);
            consumer.addVertex(pos.x, pos.y, pos.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(quads[i + 3], quads[i + 4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(nx, ny, nz);

            mat.transformPosition(quads[i + 5], quads[i + 6], quads[i + 7], pos);
            consumer.addVertex(pos.x, pos.y, pos.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(quads[i + 8], quads[i + 9])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(nx, ny, nz);

            mat.transformPosition(quads[i + 10], quads[i + 11], quads[i + 12], pos);
            consumer.addVertex(pos.x, pos.y, pos.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(quads[i + 13], quads[i + 14])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(nx, ny, nz);

            mat.transformPosition(quads[i + 15], quads[i + 16], quads[i + 17], pos);
            consumer.addVertex(pos.x, pos.y, pos.z)
                    .setColor(255, 255, 255, 255)
                    .setUv(quads[i + 18], quads[i + 19])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(nx, ny, nz);
        }
    }

    /**
     * Converts terminal facing into the matching Y rotation angle in degrees, mirroring
     * {@link dev.galacticraft.mod.content.block.platform.PlatformBlueprint#toWorldPos(BlockPos, Direction, BlockPos)}.
     */
    private static float facingToYRotDegrees(Direction facing) {
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
}
