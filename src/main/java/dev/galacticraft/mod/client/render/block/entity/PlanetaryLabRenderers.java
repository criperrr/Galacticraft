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
import dev.galacticraft.mod.client.render.astro.ClientTelescopeController;
import dev.galacticraft.mod.content.block.entity.machine.AerospaceRdTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.EnvironmentalTestChamberBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.GeologicalAnalyzerBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ObservatoryTelescopeBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ResearchDataLinkBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ResearchWorkbenchBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ZeroGResearchChamberBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;

/**
 * Sodium/Iris/ImmediatelyFast-safe 3D animated {@link BlockEntityRenderer} suite for all 7
 * planetary scientific laboratories (compact 3D benches + scalable {@code 2x2 / 3x3} Observatory
 * Telescope and Zero-G Microgravity Dome).
 */
@Environment(EnvType.CLIENT)
public final class PlanetaryLabRenderers {
    private static final ResourceLocation WHITE_TEX = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private PlanetaryLabRenderers() {
    }

    private static float facingYaw(BlockState state) {
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return 0.0F;
        }
        Direction dir = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return switch (dir) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }

    private static float timeSec(net.minecraft.world.level.block.entity.BlockEntity be, float partialTick) {
        long gt = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
        return (gt + partialTick) / 20.0F;
    }

    // =========================================================================
    // 1. OBSERVATORY TELESCOPE (Scalable 1x2 -> 2x2 -> 3x3 Articulated Optical Tube)
    // =========================================================================
    public static class TelescopeRenderer implements BlockEntityRenderer<ObservatoryTelescopeBlockEntity> {
        public TelescopeRenderer(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public boolean shouldRenderOffScreen(ObservatoryTelescopeBlockEntity be) {
            return be.getApertureTier() >= 2;
        }

        @Override
        public void render(ObservatoryTelescopeBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int ov) {
            if (ClientTelescopeController.isObservingPos(be.getBlockPos())) {
                return;
            }
            float t = timeSec(be, pt);
            int ap = Math.max(1, be.getApertureTier());
            float scale = ap == 3 ? 1.85F : (ap == 2 ? 1.35F : 1.0F);
            int opt = be.getOpticsTier();

            ps.pushPose();
            ps.translate(0.5F, 0.92F, 0.5F);
            ps.scale(scale, scale, scale);

            // Azimuth rotation from aimed coordinate (or gentle patrol sweep if idle)
            float yaw = be.getAimYawDeg();
            float pitch = be.getAimPitchDeg();
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));

            VertexConsumer solid = buf.getBuffer(RenderType.entitySolid(WHITE_TEX));
            // Equatorial fork yoke
            drawBox(ps, solid, -0.24F, -0.06F, -0.06F, 0.24F, 0.04F, 0.06F, 0.22F, 0.27F, 0.34F, light);
            drawBox(ps, solid, -0.24F, 0.04F, -0.05F, -0.17F, 0.34F, 0.05F, 0.26F, 0.31F, 0.39F, light);
            drawBox(ps, solid, 0.17F, 0.04F, -0.05F, 0.24F, 0.34F, 0.05F, 0.26F, 0.31F, 0.39F, light);

            // Pitching optical tube assembly
            ps.translate(0.0F, 0.32F, 0.0F);
            ps.mulPose(Axis.XP.rotationDegrees(Mth.clamp(-pitch, 10.0F, 82.0F) - 90.0F));

            // Main optical barrel (octagonal prism)
            drawPrism(ps, solid, 8, 0.14F, 0.16F, -0.34F, 0.62F, 0.86F, 0.89F, 0.93F, light);
            // Dark dew shield & reinforcement rings
            drawPrism(ps, solid, 8, 0.17F, 0.175F, 0.62F, 0.86F, 0.16F, 0.21F, 0.28F, light);
            drawPrism(ps, solid, 8, 0.165F, 0.165F, -0.05F, 0.08F, 0.18F, 0.24F, 0.33F, light);
            // Gold counterweight bar & weight at rear
            drawBox(ps, solid, -0.025F, -0.52F, -0.025F, 0.025F, -0.34F, 0.025F, 0.30F, 0.34F, 0.40F, light);
            drawPrism(ps, solid, 8, 0.09F, 0.09F, -0.54F, -0.44F, 0.82F, 0.66F, 0.24F, light);
            // Side finder scope
            drawPrism(ps, solid, 6, 0.035F, 0.035F, 0.05F, 0.48F, 0.20F, 0.26F, 0.34F, light, 0.19F, 0.0F, 0.0F);

            // Glowing Primary Lens / Mirror disc at front of barrel
            float lr = opt == 3 ? 0.78F : (opt == 2 ? 0.32F : 0.24F);
            float lg = opt == 3 ? 0.42F : (opt == 2 ? 0.56F : 0.78F);
            float lb = 1.0F;
            drawPrism(ps, solid, 8, 0.145F, 0.145F, 0.83F, 0.865F, lr, lg, lb, LightTexture.FULL_BRIGHT);

            // Great Observatory 3x3 translucent dome ribs when tier 3
            if (ap == 3) {
                ps.popPose();
                ps.pushPose();
                ps.translate(0.5F, 0.0F, 0.5F);
                VertexConsumer glass = buf.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
                drawPrismAlpha(ps, glass, 8, 1.42F, 1.25F, 0.05F, 1.35F, 0.35F, 0.72F, 0.96F, 0.14F, light);
                drawPrismAlpha(ps, glass, 8, 1.25F, 0.65F, 1.35F, 2.15F, 0.35F, 0.72F, 0.96F, 0.12F, light);
            }
            ps.popPose();
        }
    }

    // =========================================================================
    // 2. RESEARCH WORKBENCH (Holographic Blueprint + Brass Lamp + 3D Prototype Preview)
    // =========================================================================
    public static class WorkbenchRenderer implements BlockEntityRenderer<ResearchWorkbenchBlockEntity> {
        public WorkbenchRenderer(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public void render(ResearchWorkbenchBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int ov) {
            float t = timeSec(be, pt);
            ps.pushPose();
            ps.translate(0.5F, 0.88F, 0.5F);
            ps.mulPose(Axis.YP.rotationDegrees(facingYaw(be.getBlockState())));

            VertexConsumer solid = buf.getBuffer(RenderType.entitySolid(WHITE_TEX));
            // Articulated brass magnifying lamp on right corner
            drawBox(ps, solid, 0.30F, 0.0F, 0.22F, 0.36F, 0.32F, 0.28F, 0.76F, 0.60F, 0.24F, light);
            drawBox(ps, solid, 0.16F, 0.30F, 0.22F, 0.36F, 0.36F, 0.28F, 0.76F, 0.60F, 0.24F, light);
            drawBox(ps, solid, 0.10F, 0.26F, 0.19F, 0.22F, 0.31F, 0.31F, 0.95F, 0.92F, 0.68F, LightTexture.FULL_BRIGHT);

            // Glowing blueprint drafting overlay
            VertexConsumer holo = buf.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
            ps.pushPose();
            ps.translate(0.0F, 0.08F, 0.0F);
            ps.mulPose(Axis.XP.rotationDegrees(22.5F));
            drawBoxAlpha(ps, holo, -0.34F, 0.0F, -0.24F, 0.34F, 0.015F, 0.24F, 0.16F, 0.62F, 0.95F, 0.45F, LightTexture.FULL_BRIGHT);
            ps.popPose();

            // Floating 3D prototype preview item when an experiment is active
            var exp = ResearchWorkbenchBlockEntity.byId(be.getActiveExperimentId());
            if (exp != null) {
                ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
                ps.pushPose();
                ps.translate(0.0F, 0.28F + Mth.sin(t * 2.5F) * 0.03F, 0.0F);
                ps.mulPose(Axis.YP.rotationDegrees(t * 48.0F));
                ps.scale(0.55F, 0.55F, 0.55F);
                ir.renderStatic(new ItemStack(exp.prototypeOutput()), ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ps, buf, be.getLevel(), 0);
                ps.popPose();
            }
            ps.popPose();
        }
    }

    // =========================================================================
    // 3. GEOLOGICAL ANALYZER (Glass Chamber + Spinning Mineral + Animated Laser Beam)
    // =========================================================================
    public static class GeoAnalyzerRenderer implements BlockEntityRenderer<GeologicalAnalyzerBlockEntity> {
        public GeoAnalyzerRenderer(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public void render(GeologicalAnalyzerBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int ov) {
            float t = timeSec(be, pt);
            ps.pushPose();
            ps.translate(0.5F, 0.52F, 0.5F);
            ps.mulPose(Axis.YP.rotationDegrees(facingYaw(be.getBlockState())));

            // Translucent glass spectrometer casing
            VertexConsumer glass = buf.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
            drawBoxAlpha(ps, glass, -0.40F, 0.02F, -0.40F, 0.40F, 0.42F, 0.40F, 0.45F, 0.80F, 0.96F, 0.20F, light);

            // Side reagent ampoule glowing acid-green or cyan
            drawBoxAlpha(ps, glass, 0.30F, 0.04F, -0.10F, 0.42F, 0.36F, 0.10F, 0.40F, 0.95F, 0.35F, 0.48F, LightTexture.FULL_BRIGHT);

            // Articulated top laser emitter head + animated laser beam
            VertexConsumer solid = buf.getBuffer(RenderType.entitySolid(WHITE_TEX));
            float sweep = Mth.sin(t * 3.6F) * 0.10F;
            drawBox(ps, solid, sweep - 0.05F, 0.34F, -0.05F, sweep + 0.05F, 0.42F, 0.05F, 0.20F, 0.26F, 0.34F, light);

            var assay = GeologicalAnalyzerBlockEntity.lookupById(be.getActiveAssayId());
            if (assay != null) {
                int argb = assay.spectrumColorArgb();
                float r = ((argb >> 16) & 0xFF) / 255.0F;
                float g = ((argb >> 8) & 0xFF) / 255.0F;
                float b = (argb & 0xFF) / 255.0F;
                drawBox(ps, solid, sweep - 0.015F, 0.14F, -0.015F, sweep + 0.015F, 0.35F, 0.015F, r, g, b, LightTexture.FULL_BRIGHT);

                // Spinning 3D ore/crystal on pedestal
                ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
                ps.pushPose();
                ps.translate(0.0F, 0.14F, 0.0F);
                ps.mulPose(Axis.YP.rotationDegrees(t * 72.0F));
                ps.scale(0.55F, 0.55F, 0.55F);
                ir.renderStatic(new ItemStack(assay.byproduct()), ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ps, buf, be.getLevel(), 0);
                ps.popPose();
            }
            ps.popPose();
        }
    }

    // =========================================================================
    // 4. ENVIRONMENTAL TEST CHAMBER (1x2 Pressurized Glass Cylinder + Reciprocating Pistons)
    // =========================================================================
    public static class EnvChamberRenderer implements BlockEntityRenderer<EnvironmentalTestChamberBlockEntity> {
        public EnvChamberRenderer(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public void render(EnvironmentalTestChamberBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int ov) {
            float t = timeSec(be, pt);
            boolean acid = be.getCorrosiveAcidFlag() != 0;
            int mode = be.getEnvModeCode();
            float pistonLift = Mth.sin(t * 4.2F) * 0.08F;

            ps.pushPose();
            ps.translate(0.5F, 0.42F, 0.5F);

            VertexConsumer solid = buf.getBuffer(RenderType.entitySolid(WHITE_TEX));
            // 4 Reciprocating hydraulic pressurization pistons around cylinder
            for (int i = 0; i < 4; i++) {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees(i * 90.0F + 45.0F));
                drawBox(ps, solid, 0.30F, 0.05F, -0.04F, 0.40F, 0.55F + pistonLift, 0.04F, 0.65F, 0.70F, 0.76F, light);
                drawBox(ps, solid, 0.28F, 0.55F + pistonLift, -0.06F, 0.42F, 0.64F + pistonLift, 0.06F, 0.24F, 0.30F, 0.38F, light);
                ps.popPose();
            }
            // Top pressure manifold dome cap
            drawPrism(ps, solid, 8, 0.42F, 0.34F, 1.08F, 1.24F, 0.22F, 0.28F, 0.36F, light);

            // Translucent pressurized vertical glass cylinder + atmosphere tint
            VertexConsumer glass = buf.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
            float r = acid ? 0.90F : (mode == EnvironmentalTestChamberBlockEntity.ENV_MODE_REAL_VACUUM ? 0.30F : 0.45F);
            float g = acid ? 0.82F : (mode == EnvironmentalTestChamberBlockEntity.ENV_MODE_REAL_VACUUM ? 0.72F : 0.80F);
            float b = acid ? 0.22F : 0.98F;
            drawPrismAlpha(ps, glass, 8, 0.34F, 0.34F, 0.04F, 1.08F, r, g, b, 0.26F, LightTexture.FULL_BRIGHT);

            // Suspended glowing pressure core inside chamber
            drawBox(ps, solid, -0.10F, 0.46F + pistonLift * 0.5F, -0.10F, 0.10F, 0.66F + pistonLift * 0.5F, 0.10F, r, g, b, LightTexture.FULL_BRIGHT);
            ps.popPose();
        }
    }

    // =========================================================================
    // 5. ZERO-G RESEARCH CHAMBER (Scalable 1x2 -> 2x2 -> 3x3 Cryo Dome + 3 Gimbal Rings + Levitating Crystal)
    // =========================================================================
    public static class ZeroGRenderer implements BlockEntityRenderer<ZeroGResearchChamberBlockEntity> {
        public ZeroGRenderer(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public boolean shouldRenderOffScreen(ZeroGResearchChamberBlockEntity be) {
            return be.getDomeTier() >= 2;
        }

        @Override
        public void render(ZeroGResearchChamberBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int ov) {
            float t = timeSec(be, pt);
            int dome = Math.max(1, be.getDomeTier());
            float scale = dome == 3 ? 1.85F : (dome == 2 ? 1.38F : 1.0F);
            boolean ok = be.getMicrogravityValidFlag() != 0;

            ps.pushPose();
            ps.translate(0.5F, 0.95F + (scale - 1.0F) * 0.45F, 0.5F);
            ps.scale(scale, scale, scale);

            VertexConsumer solid = buf.getBuffer(RenderType.entitySolid(WHITE_TEX));
            float cr = ok ? 0.24F : 0.95F;
            float cg = ok ? 0.88F : 0.24F;
            float cb = ok ? 1.0F : 0.24F;

            // 3 Counter-rotating superconducting gimbal confinement rings
            for (int rIdx = 0; rIdx < 3; rIdx++) {
                ps.pushPose();
                float spd = ok ? (36.0F + rIdx * 24.0F) : 8.0F;
                if (rIdx == 0) ps.mulPose(Axis.YP.rotationDegrees(t * spd));
                else if (rIdx == 1) ps.mulPose(Axis.XP.rotationDegrees(t * spd));
                else ps.mulPose(Axis.ZP.rotationDegrees(-t * spd));
                float rad = 0.42F - rIdx * 0.07F;
                drawOctRing(ps, solid, rad, 0.025F, cr, cg, cb, LightTexture.FULL_BRIGHT);
                ps.popPose();
            }

            // Weightless tumbling glowing crystal core at center
            ps.pushPose();
            ps.translate(0.0F, Mth.sin(t * 2.2F) * 0.05F, 0.0F);
            ps.mulPose(Axis.YP.rotationDegrees(t * 55.0F));
            ps.mulPose(Axis.XP.rotationDegrees(45.0F + t * 28.0F));
            drawBox(ps, solid, -0.09F, -0.09F, -0.09F, 0.09F, 0.09F, 0.09F,
                    ok ? 0.55F : 1.0F, ok ? 0.96F : 0.35F, ok ? 1.0F : 0.35F, LightTexture.FULL_BRIGHT);
            ps.popPose();

            // Translucent octagonal cryogenic vacuum dome
            VertexConsumer glass = buf.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
            drawPrismAlpha(ps, glass, 8, 0.52F, 0.48F, -0.45F, 0.25F, 0.28F, 0.76F, 0.98F, 0.18F, light);
            drawPrismAlpha(ps, glass, 8, 0.48F, 0.22F, 0.25F, 0.55F, 0.28F, 0.76F, 0.98F, 0.16F, light);
            ps.popPose();
        }
    }

    // =========================================================================
    // 6. RESEARCH DATA LINK (Motorized 3D Parabolic Dish + Skyward Carrier Beam)
    // =========================================================================
    public static class DataLinkRenderer implements BlockEntityRenderer<ResearchDataLinkBlockEntity> {
        public DataLinkRenderer(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public void render(ResearchDataLinkBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int ov) {
            float t = timeSec(be, pt);
            ps.pushPose();
            ps.translate(0.5F, 0.95F, 0.5F);

            // Motorized Azimuth + Elevation aiming
            ps.mulPose(Axis.YP.rotationDegrees(-be.getDishAzimuthDeg()));
            ps.mulPose(Axis.XP.rotationDegrees(90.0F - be.getDishElevationDeg()));

            VertexConsumer solid = buf.getBuffer(RenderType.entitySolid(WHITE_TEX));
            // Parabolic dish back hub + 8 radial dish petals
            drawPrism(ps, solid, 8, 0.14F, 0.48F, 0.0F, 0.18F, 0.82F, 0.86F, 0.90F, light);
            drawPrism(ps, solid, 8, 0.48F, 0.52F, 0.18F, 0.22F, 0.20F, 0.28F, 0.38F, light);
            // Central feedhorn mast + glowing RF emitter tip
            drawBox(ps, solid, -0.025F, 0.05F, -0.025F, 0.025F, 0.42F, 0.025F, 0.26F, 0.32F, 0.40F, light);
            boolean active = be.getSignalStrengthPct() > 0;
            drawBox(ps, solid, -0.055F, 0.40F, -0.055F, 0.055F, 0.48F, 0.055F,
                    active ? 0.22F : 0.45F, active ? 0.95F : 0.45F, active ? 0.72F : 0.48F,
                    active ? LightTexture.FULL_BRIGHT : light);

            // Pulsing translucent carrier signal beam shooting skyward when active
            if (active) {
                VertexConsumer beam = buf.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
                float pulse = 0.03F + 0.015F * Mth.sin(t * 7.0F);
                drawPrismAlpha(ps, beam, 6, pulse, pulse * 2.2F, 0.48F, 2.20F, 0.24F, 0.95F, 0.82F, 0.35F, LightTexture.FULL_BRIGHT);
            }
            ps.popPose();
        }
    }

    // =========================================================================
    // 7. AEROSPACE R&D TERMINAL (3 Floating Translucent KSP Holographic Displays)
    // =========================================================================
    public static class RdTerminalRenderer implements BlockEntityRenderer<AerospaceRdTerminalBlockEntity> {
        public RdTerminalRenderer(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public void render(AerospaceRdTerminalBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int ov) {
            float t = timeSec(be, pt);
            float hover = Mth.sin(t * 2.4F) * 0.02F;

            ps.pushPose();
            ps.translate(0.5F, 1.05F + hover, 0.5F);
            ps.mulPose(Axis.YP.rotationDegrees(facingYaw(be.getBlockState())));

            VertexConsumer holo = buf.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
            // Center wide KSP Tech Tree holographic display
            drawBoxAlpha(ps, holo, -0.34F, 0.02F, 0.16F, 0.34F, 0.46F, 0.18F, 0.12F, 0.68F, 0.98F, 0.42F, LightTexture.FULL_BRIGHT);
            // Glowing tech tree nodes inside center screen
            drawBoxAlpha(ps, holo, -0.24F, 0.20F, 0.14F, -0.14F, 0.28F, 0.16F, 0.30F, 0.98F, 0.68F, 0.85F, LightTexture.FULL_BRIGHT);
            drawBoxAlpha(ps, holo, -0.05F, 0.26F, 0.14F, 0.05F, 0.34F, 0.16F, 0.38F, 0.92F, 1.0F, 0.85F, LightTexture.FULL_BRIGHT);
            drawBoxAlpha(ps, holo, 0.14F, 0.16F, 0.14F, 0.24F, 0.24F, 0.16F, 0.78F, 0.52F, 1.0F, 0.85F, LightTexture.FULL_BRIGHT);

            // Left angled telemetry holographic screen
            ps.pushPose();
            ps.translate(-0.42F, 0.0F, 0.04F);
            ps.mulPose(Axis.YP.rotationDegrees(32.0F));
            drawBoxAlpha(ps, holo, -0.16F, 0.04F, -0.01F, 0.16F, 0.40F, 0.01F, 0.18F, 0.82F, 0.72F, 0.38F, LightTexture.FULL_BRIGHT);
            ps.popPose();

            // Right angled Astro Atlas holographic screen
            ps.pushPose();
            ps.translate(0.42F, 0.0F, 0.04F);
            ps.mulPose(Axis.YP.rotationDegrees(-32.0F));
            drawBoxAlpha(ps, holo, -0.16F, 0.04F, -0.01F, 0.16F, 0.40F, 0.01F, 0.65F, 0.45F, 0.98F, 0.38F, LightTexture.FULL_BRIGHT);
            ps.popPose();

            ps.popPose();
        }
    }

    // =========================================================================
    // ZERO-ALLOCATION PRIMITIVE GEOMETRY HELPERS (Sodium / Iris Safe)
    // =========================================================================
    private static void drawOctRing(PoseStack ps, VertexConsumer vc, float radius, float halfThick, float r, float g, float b, int light) {
        int segs = 12;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * 2.0 * Math.PI / segs);
            float a1 = (float) ((i + 1) * 2.0 * Math.PI / segs);
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float mx = (x0 + x1) * 0.5F;
            float mz = (z0 + z1) * 0.5F;
            drawBox(ps, vc, mx - halfThick * 1.8F, -halfThick, mz - halfThick * 1.8F,
                    mx + halfThick * 1.8F, halfThick, mz + halfThick * 1.8F, r, g, b, light);
        }
    }

    private static void drawPrism(PoseStack ps, VertexConsumer vc, int sides, float rBot, float rTop, float y0, float y1,
                                  float r, float g, float b, int light) {
        drawPrism(ps, vc, sides, rBot, rTop, y0, y1, r, g, b, light, 0.0F, 0.0F, 0.0F);
    }

    private static void drawPrism(PoseStack ps, VertexConsumer vc, int sides, float rBot, float rTop, float y0, float y1,
                                  float r, float g, float b, int light, float ox, float oy, float oz) {
        drawPrismInternal(ps, vc, sides, rBot, rTop, y0, y1, r, g, b, 1.0F, light, ox, oy, oz);
    }

    private static void drawPrismAlpha(PoseStack ps, VertexConsumer vc, int sides, float rBot, float rTop, float y0, float y1,
                                       float r, float g, float b, float a, int light) {
        drawPrismInternal(ps, vc, sides, rBot, rTop, y0, y1, r, g, b, a, light, 0.0F, 0.0F, 0.0F);
    }

    private static void drawPrismInternal(PoseStack ps, VertexConsumer vc, int sides, float rBot, float rTop, float y0, float y1,
                                          float r, float g, float b, float a, int light, float ox, float oy, float oz) {
        Matrix4f m = ps.last().pose();
        PoseStack.Pose pose = ps.last();
        for (int i = 0; i < sides; i++) {
            double a0 = i * 2.0 * Math.PI / sides;
            double a1 = (i + 1) * 2.0 * Math.PI / sides;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float nx = (c0 + c1) * 0.5F, nz = (s0 + s1) * 0.5F;
            quad(m, pose, vc,
                    ox + c0 * rBot, oy + y0, oz + s0 * rBot,
                    ox + c1 * rBot, oy + y0, oz + s1 * rBot,
                    ox + c1 * rTop, oy + y1, oz + s1 * rTop,
                    ox + c0 * rTop, oy + y1, oz + s0 * rTop,
                    nx, 0.0F, nz, r, g, b, a, light);
            // Top cap triangle-as-degenerate-quad
            quad(m, pose, vc,
                    ox, oy + y1, oz,
                    ox + c0 * rTop, oy + y1, oz + s0 * rTop,
                    ox + c1 * rTop, oy + y1, oz + s1 * rTop,
                    ox, oy + y1, oz,
                    0.0F, 1.0F, 0.0F, r * 0.95F, g * 0.95F, b * 0.95F, a, light);
        }
    }

    private static void drawBox(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                                float r, float g, float b, int light) {
        drawBoxAlpha(ps, vc, x0, y0, z0, x1, y1, z1, r, g, b, 1.0F, light);
    }

    private static void drawBoxAlpha(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                                     float r, float g, float b, float a, int light) {
        Matrix4f m = ps.last().pose();
        PoseStack.Pose p = ps.last();
        // Top & Bottom
        quad(m, p, vc, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, r, g, b, a, light);
        quad(m, p, vc, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, r * 0.72F, g * 0.72F, b * 0.72F, a, light);
        // North & South
        quad(m, p, vc, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, r * 0.88F, g * 0.88F, b * 0.88F, a, light);
        quad(m, p, vc, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, r * 0.88F, g * 0.88F, b * 0.88F, a, light);
        // West & East
        quad(m, p, vc, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, r * 0.80F, g * 0.80F, b * 0.80F, a, light);
        quad(m, p, vc, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, r * 0.80F, g * 0.80F, b * 0.80F, a, light);
    }

    private static void quad(Matrix4f m, PoseStack.Pose p, VertexConsumer vc,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float nx, float ny, float nz,
                             float r, float g, float b, float a, int light) {
        vc.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, a).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(m, x3, y3, z3).setColor(r, g, b, a).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
