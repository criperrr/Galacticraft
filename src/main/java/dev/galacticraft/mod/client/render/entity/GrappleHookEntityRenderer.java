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

package dev.galacticraft.mod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.galacticraft.mod.content.entity.GrappleHookEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class GrappleHookEntityRenderer extends ThrownItemRenderer<GrappleHookEntity> {
    public GrappleHookEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GrappleHookEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        Entity owner = entity.getOwner();
        if (owner != null) {
            poseStack.pushPose();

            double ownerX = Mth.lerp(partialTick, owner.xOld, owner.getX());
            double ownerY = Mth.lerp(partialTick, owner.yOld, owner.getY()) + owner.getEyeHeight() * 0.7;
            double ownerZ = Mth.lerp(partialTick, owner.zOld, owner.getZ());

            double hookX = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double hookY = Mth.lerp(partialTick, entity.yOld, entity.getY()) + 0.25;
            double hookZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

            float dx = (float) (ownerX - hookX);
            float dy = (float) (ownerY - hookY);
            float dz = (float) (ownerZ - hookZ);

            VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
            PoseStack.Pose last = poseStack.last();

            int segments = 16;
            for (int i = 0; i < segments; i++) {
                float f1 = (float) i / (float) segments;
                float f2 = (float) (i + 1) / (float) segments;

                float x1 = dx * f1;
                float y1 = dy * (f1 * f1 + f1) * 0.5F;
                float z1 = dz * f1;

                float x2 = dx * f2;
                float y2 = dy * (f2 * f2 + f2) * 0.5F;
                float z2 = dz * f2;

                consumer.addVertex(last.pose(), x1, y1, z1).setColor(203, 203, 192, 255).setNormal(last, 0.0F, 1.0F, 0.0F);
                consumer.addVertex(last.pose(), x2, y2, z2).setColor(203, 203, 192, 255).setNormal(last, 0.0F, 1.0F, 0.0F);
            }

            poseStack.popPose();
        }
    }
}
