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
import dev.galacticraft.mod.content.block.entity.machine.Helium3FusionReactorBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renders a live 3D toroidal MHD plasma ring and rotating superconducting magnetic containment
 * hoops inside the hollow center of the 3x3x3 Helium-3 Fusion Reactor multiblock.
 */
@Environment(EnvType.CLIENT)
public class Helium3FusionReactorRenderer implements BlockEntityRenderer<Helium3FusionReactorBlockEntity> {
    private static final int SEGMENTS = 16;

    public Helium3FusionReactorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Helium3FusionReactorBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!be.isMultiblockFormed()) {
            return;
        }

        BlockPos pos = be.getBlockPos();
        BlockPos core = Helium3FusionReactorBlockEntity.getCorePos(pos, be.getBlockState());
        double dx = core.getX() - pos.getX() + 0.5;
        double dy = core.getY() - pos.getY() + 0.5;
        double dz = core.getZ() - pos.getZ() + 0.5;

        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
        float time = (gameTime + partialTick);
        boolean active = be.getBurnTime() > 0 || be.getPlasmaTempMk() >= Helium3FusionReactorBlockEntity.IGNITION_TEMP_MK;
        boolean cryo = be.isCryoCooled();

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());

        float pulse = 1.0f + 0.08f * Mth.sin(time * (active ? 0.25f : 0.08f));
        float spinSpeed = active ? (cryo ? 4.5f : 2.8f) : 0.6f;

        // 1. Primary 3D Toroidal Plasma Ring
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * spinSpeed));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(time * 0.05f) * 8.0f));
        float r1 = cryo ? 0.25f : (active ? 0.35f : 0.15f);
        float g1 = cryo ? 0.95f : (active ? 0.72f : 0.45f);
        float b1 = 1.0f;
        float a1 = active ? 0.75f : 0.28f;
        drawTorusRing(poseStack.last().pose(), vc, 0.34f * pulse, 0.075f * pulse, r1, g1, b1, a1);
        poseStack.popPose();

        // 2. Inner Hot Plasma Core Ring (brighter, counter-rotating)
        if (active) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-time * spinSpeed * 1.4f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(22.0f));
            drawTorusRing(poseStack.last().pose(), vc, 0.22f * pulse, 0.045f * pulse,
                    cryo ? 0.75f : 0.65f, 0.96f, 1.0f, 0.85f);
            poseStack.popPose();

            // 3. Orthogonal Magnetic Field Containment Ring
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.8f));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            drawTorusRing(poseStack.last().pose(), vc, 0.42f, 0.022f,
                    0.35f, 0.55f, 1.0f, 0.45f);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void drawTorusRing(Matrix4f mat, VertexConsumer vc, float majorR, float minorR, float r, float g, float b, float a) {
        for (int i = 0; i < SEGMENTS; i++) {
            float theta0 = (float) (i * 2.0 * Math.PI / SEGMENTS);
            float theta1 = (float) ((i + 1) * 2.0 * Math.PI / SEGMENTS);

            float c0 = Mth.cos(theta0);
            float s0 = Mth.sin(theta0);
            float c1 = Mth.cos(theta1);
            float s1 = Mth.sin(theta1);

            float outerR = majorR + minorR;
            float innerR = majorR - minorR;

            // Top face quad
            quad(mat, vc,
                    innerR * c0, minorR, innerR * s0,
                    outerR * c0, minorR, outerR * s0,
                    outerR * c1, minorR, outerR * s1,
                    innerR * c1, minorR, innerR * s1,
                    r, g, b, a);
            // Bottom face quad
            quad(mat, vc,
                    innerR * c1, -minorR, innerR * s1,
                    outerR * c1, -minorR, outerR * s1,
                    outerR * c0, -minorR, outerR * s0,
                    innerR * c0, -minorR, innerR * s0,
                    r, g, b, a);
            // Outer rim quad
            quad(mat, vc,
                    outerR * c0, -minorR, outerR * s0,
                    outerR * c1, -minorR, outerR * s1,
                    outerR * c1, minorR, outerR * s1,
                    outerR * c0, minorR, outerR * s0,
                    r, g, b, a);
            // Inner rim quad
            quad(mat, vc,
                    innerR * c1, -minorR, innerR * s1,
                    innerR * c0, -minorR, innerR * s0,
                    innerR * c0, minorR, innerR * s0,
                    innerR * c1, minorR, innerR * s1,
                    r, g, b, a);
        }
    }

    private static void quad(Matrix4f mat, VertexConsumer vc,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float r, float g, float b, float a) {
        vc.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        vc.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        vc.addVertex(mat, x2, y2, z2).setColor(r, g, b, a);
        vc.addVertex(mat, x3, y3, z3).setColor(r, g, b, a);
    }

    @Override
    public boolean shouldRenderOffScreen(Helium3FusionReactorBlockEntity blockEntity) {
        return true;
    }
}
