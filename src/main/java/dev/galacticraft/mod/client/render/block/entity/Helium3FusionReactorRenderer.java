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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renders the unified 3x3x3 3D Tokamak / MHD Fusion Reactor multiblock object once formed:
 * - Octagonal niobium-titanium base plinth and top cryo-distribution crown
 * - Central superconducting solenoid column
 * - 8 radial 3D D-shaped toroidal field magnet coils (copper + cyan Nb3Sn windings)
 * - Translucent octagonal sapphire plasma containment chamber
 * - Live animated 3D toroidal plasma ring and magnetic flux hoops
 */
@Environment(EnvType.CLIENT)
public class Helium3FusionReactorRenderer implements BlockEntityRenderer<Helium3FusionReactorBlockEntity> {
    private static final ResourceLocation WHITE_TEX = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final int TORUS_SEGMENTS = 24;
    private static final int OCTAGON = 8;

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
        float time = gameTime + partialTick;
        boolean active = be.getBurnTime() > 0 || be.getPlasmaTempMk() >= Helium3FusionReactorBlockEntity.IGNITION_TEMP_MK;
        boolean cryo = be.isCryoCooled();
        int fullBright = LightTexture.FULL_BRIGHT;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);

        // --- 1. SOLID 3D TOKAMAK STRUCTURE ---
        VertexConsumer solid = buffer.getBuffer(RenderType.entitySolid(WHITE_TEX));
        Matrix4f mat = poseStack.last().pose();

        // Bottom octagonal base plinth (y = -1.50 .. -1.10)
        drawPrism(mat, poseStack.last(), solid, OCTAGON, 1.44f, 1.44f, -1.50f, -1.24f, 0.16f, 0.20f, 0.27f, packedLight);
        drawPrism(mat, poseStack.last(), solid, OCTAGON, 1.32f, 1.18f, -1.24f, -1.04f, 0.22f, 0.27f, 0.36f, packedLight);

        // Top octagonal cryo-distribution crown (y = +1.04 .. +1.50)
        drawPrism(mat, poseStack.last(), solid, OCTAGON, 1.18f, 1.32f, 1.04f, 1.24f, 0.22f, 0.27f, 0.36f, packedLight);
        drawPrism(mat, poseStack.last(), solid, OCTAGON, 1.44f, 1.44f, 1.24f, 1.50f, 0.16f, 0.20f, 0.27f, packedLight);

        // Top cryo manifold cap
        drawPrism(mat, poseStack.last(), solid, OCTAGON, 0.65f, 0.52f, 1.50f, 1.62f, 0.28f, 0.34f, 0.44f, packedLight);

        // Central superconducting solenoid column running through the torus core
        drawPrism(mat, poseStack.last(), solid, OCTAGON, 0.34f, 0.34f, -1.10f, 1.10f, 0.18f, 0.23f, 0.32f, packedLight);
        // Glowing cyan-copper solenoid bands on central column
        for (float bandY : new float[]{-0.65f, -0.22f, 0.22f, 0.65f}) {
            drawPrism(mat, poseStack.last(), solid, OCTAGON, 0.39f, 0.39f, bandY - 0.06f, bandY + 0.06f,
                    active ? 0.22f : 0.15f, active ? 0.78f : 0.42f, active ? 0.96f : 0.56f, active ? fullBright : packedLight);
        }

        // 8 Radial D-shaped Superconducting Toroidal Field Coils wrapping around the torus
        for (int i = 0; i < 8; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 45.0f + 22.5f));
            Matrix4f cm = poseStack.last().pose();
            PoseStack.Pose cp = poseStack.last();

            // Outer vertical D-coil structural rib (dark niobium frame)
            drawBox(cm, cp, solid, 1.16f, -1.12f, -0.11f, 1.42f, 1.12f, 0.11f, 0.20f, 0.25f, 0.34f, packedLight);
            // Copper winding core inside D-coil
            drawBox(cm, cp, solid, 1.13f, -0.96f, -0.07f, 1.45f, 0.96f, 0.07f, 0.72f, 0.44f, 0.22f, packedLight);
            // Superconducting Nb3Sn cyan accent strip on outer face
            drawBox(cm, cp, solid, 1.44f, -0.82f, -0.04f, 1.47f, 0.82f, 0.04f,
                    active ? (cryo ? 0.30f : 0.20f) : 0.12f,
                    active ? (cryo ? 0.98f : 0.76f) : 0.38f,
                    active ? 1.0f : 0.52f,
                    active ? fullBright : packedLight);

            // Top & bottom radial magnetic yoke arms connecting D-coil to central solenoid
            drawBox(cm, cp, solid, 0.30f, 0.96f, -0.09f, 1.38f, 1.18f, 0.09f, 0.24f, 0.30f, 0.40f, packedLight);
            drawBox(cm, cp, solid, 0.30f, -1.18f, -0.09f, 1.38f, -0.96f, 0.09f, 0.24f, 0.30f, 0.40f, packedLight);
            poseStack.popPose();
        }

        // --- 2. TRANSLUCENT SAPPHIRE VACUUM CONTAINMENT VESSEL ---
        VertexConsumer glass = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
        drawPrismTranslucent(mat, poseStack.last(), glass, OCTAGON, 1.12f, 1.12f, -0.94f, 0.94f,
                0.28f, 0.68f, 0.96f, 0.20f, packedLight);

        // --- 3. LIVE ANIMATED 3D TOROIDAL PLASMA RING & MAGNETIC FLUX HOOPS ---
        VertexConsumer plasma = buffer.getBuffer(RenderType.lightning());
        float pulse = 1.0f + 0.07f * Mth.sin(time * (active ? 0.24f : 0.08f));
        float spinSpeed = active ? (cryo ? 4.8f : 2.6f) : 0.6f;

        // Primary plasma torus orbiting the central solenoid (majorR = 0.74, minorR = 0.16)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * spinSpeed));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(time * 0.05f) * 5.0f));
        float r1 = cryo ? 0.30f : (active ? 0.35f : 0.14f);
        float g1 = cryo ? 0.96f : (active ? 0.72f : 0.42f);
        float b1 = 1.0f;
        float a1 = active ? 0.78f : 0.26f;
        drawTorusRing(poseStack.last().pose(), plasma, 0.74f * pulse, 0.15f * pulse, r1, g1, b1, a1);
        poseStack.popPose();

        // Inner intense plasma filament + helical magnetic flux rings when active
        if (active) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-time * spinSpeed * 1.5f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(9.0f));
            drawTorusRing(poseStack.last().pose(), plasma, 0.74f, 0.07f * pulse,
                    cryo ? 0.85f : 0.70f, 0.98f, 1.0f, 0.90f);
            poseStack.popPose();

            // Upper & lower poloidal containment rings
            for (float ringY : new float[]{-0.42f, 0.42f}) {
                poseStack.pushPose();
                poseStack.translate(0.0, ringY, 0.0);
                poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.2f));
                drawTorusRing(poseStack.last().pose(), plasma, 0.74f, 0.028f,
                        0.32f, 0.65f, 1.0f, 0.55f);
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private static void drawPrism(Matrix4f mat, PoseStack.Pose pose, VertexConsumer vc, int sides,
                                  float rBottom, float rTop, float y0, float y1,
                                  float r, float g, float b, int light) {
        for (int i = 0; i < sides; i++) {
            float a0 = (float) (i * 2.0 * Math.PI / sides + Math.PI / sides);
            float a1 = (float) ((i + 1) * 2.0 * Math.PI / sides + Math.PI / sides);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);

            float nx = (c0 + c1) * 0.5f;
            float nz = (s0 + s1) * 0.5f;
            float len = Mth.sqrt(nx * nx + nz * nz);
            if (len > 0) { nx /= len; nz /= len; }

            // Side face
            litQuad(mat, pose, vc,
                    rBottom * c0, y0, rBottom * s0,
                    rBottom * c1, y0, rBottom * s1,
                    rTop * c1, y1, rTop * s1,
                    rTop * c0, y1, rTop * s0,
                    nx, 0f, nz, r, g, b, 1.0f, light);

            // Top cap triangle (as degenerate quad)
            litQuad(mat, pose, vc,
                    0f, y1, 0f,
                    0f, y1, 0f,
                    rTop * c1, y1, rTop * s1,
                    rTop * c0, y1, rTop * s0,
                    0f, 1f, 0f, r * 1.1f, g * 1.1f, b * 1.1f, 1.0f, light);

            // Bottom cap
            litQuad(mat, pose, vc,
                    0f, y0, 0f,
                    rBottom * c0, y0, rBottom * s0,
                    rBottom * c1, y0, rBottom * s1,
                    0f, y0, 0f,
                    0f, -1f, 0f, r * 0.75f, g * 0.75f, b * 0.75f, 1.0f, light);
        }
    }

    private static void drawPrismTranslucent(Matrix4f mat, PoseStack.Pose pose, VertexConsumer vc, int sides,
                                             float rBottom, float rTop, float y0, float y1,
                                             float r, float g, float b, float a, int light) {
        for (int i = 0; i < sides; i++) {
            float a0 = (float) (i * 2.0 * Math.PI / sides + Math.PI / sides);
            float a1 = (float) ((i + 1) * 2.0 * Math.PI / sides + Math.PI / sides);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);

            float nx = (c0 + c1) * 0.5f;
            float nz = (s0 + s1) * 0.5f;
            float len = Mth.sqrt(nx * nx + nz * nz);
            if (len > 0) { nx /= len; nz /= len; }

            litQuad(mat, pose, vc,
                    rBottom * c0, y0, rBottom * s0,
                    rBottom * c1, y0, rBottom * s1,
                    rTop * c1, y1, rTop * s1,
                    rTop * c0, y1, rTop * s0,
                    nx, 0f, nz, r, g, b, a, light);
        }
    }

    private static void drawBox(Matrix4f mat, PoseStack.Pose pose, VertexConsumer vc,
                                float x0, float y0, float z0, float x1, float y1, float z1,
                                float r, float g, float b, int light) {
        // +Y
        litQuad(mat, pose, vc, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, r * 1.05f, g * 1.05f, b * 1.05f, 1.0f, light);
        // -Y
        litQuad(mat, pose, vc, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, r * 0.7f, g * 0.7f, b * 0.7f, 1.0f, light);
        // +X
        litQuad(mat, pose, vc, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, r * 0.9f, g * 0.9f, b * 0.9f, 1.0f, light);
        // -X
        litQuad(mat, pose, vc, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, r * 0.85f, g * 0.85f, b * 0.85f, 1.0f, light);
        // +Z
        litQuad(mat, pose, vc, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, r * 0.95f, g * 0.95f, b * 0.95f, 1.0f, light);
        // -Z
        litQuad(mat, pose, vc, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, r * 0.8f, g * 0.8f, b * 0.8f, 1.0f, light);
    }

    private static void litQuad(Matrix4f mat, PoseStack.Pose pose, VertexConsumer vc,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float nx, float ny, float nz,
                                float r, float g, float b, float a, int light) {
        float cr = Mth.clamp(r, 0f, 1f);
        float cg = Mth.clamp(g, 0f, 1f);
        float cb = Mth.clamp(b, 0f, 1f);
        vc.addVertex(mat, x0, y0, z0).setColor(cr, cg, cb, a).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1).setColor(cr, cg, cb, a).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(cr, cg, cb, a).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3).setColor(cr, cg, cb, a).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    private static void drawTorusRing(Matrix4f mat, VertexConsumer vc, float majorR, float minorR, float r, float g, float b, float a) {
        for (int i = 0; i < TORUS_SEGMENTS; i++) {
            float theta0 = (float) (i * 2.0 * Math.PI / TORUS_SEGMENTS);
            float theta1 = (float) ((i + 1) * 2.0 * Math.PI / TORUS_SEGMENTS);

            float c0 = Mth.cos(theta0);
            float s0 = Mth.sin(theta0);
            float c1 = Mth.cos(theta1);
            float s1 = Mth.sin(theta1);

            float outerR = majorR + minorR;
            float innerR = majorR - minorR;

            plasmaQuad(mat, vc,
                    innerR * c0, minorR, innerR * s0,
                    outerR * c0, minorR, outerR * s0,
                    outerR * c1, minorR, outerR * s1,
                    innerR * c1, minorR, innerR * s1,
                    r, g, b, a);
            plasmaQuad(mat, vc,
                    innerR * c1, -minorR, innerR * s1,
                    outerR * c1, -minorR, outerR * s1,
                    outerR * c0, -minorR, outerR * s0,
                    innerR * c0, -minorR, innerR * s0,
                    r, g, b, a);
            plasmaQuad(mat, vc,
                    outerR * c0, -minorR, outerR * s0,
                    outerR * c1, -minorR, outerR * s1,
                    outerR * c1, minorR, outerR * s1,
                    outerR * c0, minorR, outerR * s0,
                    r, g, b, a);
            plasmaQuad(mat, vc,
                    innerR * c1, -minorR, innerR * s1,
                    innerR * c0, -minorR, innerR * s0,
                    innerR * c0, minorR, innerR * s0,
                    innerR * c1, minorR, innerR * s1,
                    r, g, b, a);
        }
    }

    private static void plasmaQuad(Matrix4f mat, VertexConsumer vc,
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

    @Override
    public int getViewDistance() {
        return 128;
    }
}
