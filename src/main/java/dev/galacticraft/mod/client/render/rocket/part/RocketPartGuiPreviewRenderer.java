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

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Reusable 3D GUI viewport renderer for modular {@link RocketPartRuntimeModel} parts and stacked
 * multi-part VAB rocket previews inside Minecraft {@link GuiGraphics} screens.
 *
 * <p>Uses exact bounds-derived auto-fit scaling, scissor clipping with explicit buffer flushes,
 * and safe orthographic Z-clamping so 3D meshes never clip into GUI backgrounds, carried items,
 * or tooltips.
 */
@Environment(EnvType.CLIENT)
public final class RocketPartGuiPreviewRenderer {
    public static final float DEFAULT_PITCH_DEG = 20.0F;
    public static final float MIN_PITCH_DEG = -55.0F;
    public static final float MAX_PITCH_DEG = 55.0F;
    public static final float MIN_ZOOM = 0.60F;
    public static final float MAX_ZOOM = 2.20F;

    private static final float REF_COS_PITCH = 0.9396926F; // cos(20 deg)
    private static final float REF_SIN_PITCH = 0.3420201F; // sin(20 deg)

    private RocketPartGuiPreviewRenderer() {
    }

    /**
     * Resolves the runtime {@code .gcrm} model, livery texture, and catalog spec for a part config.
     */
    public static @Nullable ResolvedModel resolve(@Nullable RocketPartConfig config) {
        if (config == null) {
            return null;
        }
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        RocketPartSpec spec = catalog.getPart(config.modelId());
        if (spec == null) {
            spec = catalog.defaultSpecFor(config.category());
        }
        if (spec == null) {
            return null;
        }
        RocketPartRuntimeModel model = RocketPartModelCache.INSTANCE.getOrLoad(spec.meshLocation());
        if (model == null) {
            return null;
        }
        ResourceLocation texture = spec.textureFor(config.livery());
        return new ResolvedModel(model, texture, spec);
    }

    /**
     * Draws a dark aerospace blueprint chamber backdrop with grid lines and subtle cyan corner ticks.
     */
    public static void renderViewportFrame(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            boolean hovered,
            boolean showCenterAxis
    ) {
        graphics.fill(x, y, x + width, y + height, 0xFF080E18);
        int border = hovered ? 0xFF38BDF8 : 0xFF23364D;
        graphics.renderOutline(x, y, width, height, border);

        // Subtle engineering blueprint grid
        int step = 14;
        for (int gx = x + step; gx < x + width - 1; gx += step) {
            graphics.fill(gx, y + 1, gx + 1, y + height - 1, 0xFF0F1B2B);
        }
        for (int gy = y + step; gy < y + height - 1; gy += step) {
            graphics.fill(x + 1, gy, x + width - 1, gy + 1, 0xFF0F1B2B);
        }

        if (showCenterAxis) {
            int cx = x + width / 2;
            graphics.fill(cx, y + 3, cx + 1, y + height - 3, 0x332D8AC4);
        }

        // Crisp cyan corner reticle ticks
        int corner = hovered ? 0xFF38BDF8 : 0xFF2D8AC4;
        int tick = 5;
        // Top-left
        graphics.fill(x + 1, y + 1, x + 1 + tick, y + 2, corner);
        graphics.fill(x + 1, y + 1, x + 2, y + 1 + tick, corner);
        // Top-right
        graphics.fill(x + width - 1 - tick, y + 1, x + width - 1, y + 2, corner);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + 1 + tick, corner);
        // Bottom-left
        graphics.fill(x + 1, y + height - 2, x + 1 + tick, y + height - 1, corner);
        graphics.fill(x + 1, y + height - 1 - tick, x + 2, y + height - 1, corner);
        // Bottom-right
        graphics.fill(x + width - 1 - tick, y + height - 2, x + width - 1, y + height - 1, corner);
        graphics.fill(x + width - 2, y + height - 1 - tick, x + width - 1, y + height - 1, corner);
    }

    /**
     * Computes a stable bounds-derived auto-fit pixel scale (GUI px per Minecraft block) that fits a
     * rotated 3D box inside {@code (width, height)} while clamping its 3D half-depth within
     * {@code maxHalfDepthPx} so orthographic Z never clips GUI layers.
     */
    public static float computeFitScale(
            float sizeX,
            float sizeY,
            float sizeZ,
            int width,
            int height,
            float paddingPx,
            float zoom,
            float maxHalfDepthPx
    ) {
        float sx = Math.max(0.25F, sizeX);
        float sy = Math.max(0.25F, sizeY);
        float sz = Math.max(0.25F, sizeZ);

        float horizSpan = (float) Math.hypot(sx, sz);
        float fitWidth = Math.max(0.25F, horizSpan);
        float fitHeight = Math.max(0.25F, sy * REF_COS_PITCH + horizSpan * REF_SIN_PITCH);

        float usableW = Math.max(8.0F, width - paddingPx * 2.0F);
        float usableH = Math.max(8.0F, height - paddingPx * 2.0F);

        float baseScale = Math.min(usableW / fitWidth, usableH / fitHeight);
        float scaled = baseScale * Mth.clamp(zoom, MIN_ZOOM, MAX_ZOOM);

        if (maxHalfDepthPx > 0.0F) {
            float halfDiagBlocks = 0.5F * (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (halfDiagBlocks > 1.0e-4F) {
                float maxSafeScale = maxHalfDepthPx / halfDiagBlocks;
                scaled = Math.min(scaled, maxSafeScale);
            }
        }
        return scaled;
    }

    /**
     * Renders a single modular rocket part in 3D inside a scissored GUI viewport.
     *
     * @return {@code true} if the part model was resolved and rendered.
     */
    public static boolean renderSinglePart(
            GuiGraphics graphics,
            @Nullable RocketPartConfig config,
            int x,
            int y,
            int width,
            int height,
            float yawDeg,
            float pitchDeg,
            float zoom,
            float animTimeSeconds,
            float zOffset,
            float maxHalfDepthPx,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        if (width <= 4 || height <= 4) {
            return false;
        }
        ResolvedModel resolved = resolve(config);
        if (resolved == null) {
            return false;
        }
        RocketPartRuntimeModel model = resolved.model();
        float scale = computeFitScale(
                model.boundsSizeX(),
                model.boundsSizeY(),
                model.boundsSizeZ(),
                width,
                height,
                5.0F,
                zoom,
                maxHalfDepthPx
        );

        float shroudScaleY = config != null && "interstage_engine_shroud".equals(config.familyId()) ? 0.0F : 1.0F;

        graphics.flush();
        graphics.enableScissor(x, y, x + width, y + height);
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + width * 0.5F, y + height * 0.5F, zOffset);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(Mth.clamp(pitchDeg, MIN_PITCH_DEG, MAX_PITCH_DEG)));
        pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
        pose.translate(-model.boundsCenterX(), -model.boundsCenterY(), -model.boundsCenterZ());

        RocketPartMeshRenderer.render(
                model,
                resolved.texture(),
                pose,
                graphics.bufferSource(),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                animTimeSeconds,
                false,
                red,
                green,
                blue,
                alpha,
                shroudScaleY
        );

        graphics.flush();
        pose.popPose();
        graphics.disableScissor();
        Lighting.setupFor3DItems();
        return true;
    }

    /**
     * Renders a coaxial multi-part rocket assembly in 3D inside a scissored VAB viewport and
     * optionally populates projected screen-space vertical hit bands for click selection.
     *
     * @return {@code true} if at least one part model was resolved and rendered.
     */
    public static boolean renderRocketStack(
            GuiGraphics graphics,
            List<StackPart> parts,
            int x,
            int y,
            int width,
            int height,
            float yawDeg,
            float pitchDeg,
            float zoom,
            float zOffset,
            float maxHalfDepthPx,
            @Nullable List<ProjectedBand> outBands
    ) {
        if (parts == null || parts.isEmpty() || width <= 4 || height <= 4) {
            return false;
        }

        int count = parts.size();
        ResolvedModel[] resolvedModels = new ResolvedModel[count];
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        boolean anyValid = false;

        for (int i = 0; i < count; i++) {
            StackPart part = parts.get(i);
            ResolvedModel rm = resolve(part.config());
            resolvedModels[i] = rm;
            if (rm == null) {
                continue;
            }
            anyValid = true;
            RocketPartRuntimeModel m = rm.model();
            float by = part.baseYBlocks();
            minX = Math.min(minX, m.minX());
            maxX = Math.max(maxX, m.maxX());
            minZ = Math.min(minZ, m.minZ());
            maxZ = Math.max(maxZ, m.maxZ());
            minY = Math.min(minY, by + m.minY());
            maxY = Math.max(maxY, by + m.maxY());
        }

        if (!anyValid) {
            return false;
        }

        float sizeX = Math.max(0.25F, maxX - minX);
        float sizeY = Math.max(0.25F, maxY - minY);
        float sizeZ = Math.max(0.25F, maxZ - minZ);
        float centerX = 0.5F * (minX + maxX);
        float centerY = 0.5F * (minY + maxY);
        float centerZ = 0.5F * (minZ + maxZ);

        float scale = computeFitScale(
                sizeX,
                sizeY,
                sizeZ,
                width,
                height,
                8.0F,
                zoom,
                maxHalfDepthPx
        );

        float clampedPitch = Mth.clamp(pitchDeg, MIN_PITCH_DEG, MAX_PITCH_DEG);
        float vpCenterX = x + width * 0.5F;
        float vpCenterY = y + height * 0.5F + 2.0F;

        graphics.flush();
        graphics.enableScissor(x, y, x + width, y + height);
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(vpCenterX, vpCenterY, zOffset);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(clampedPitch));
        pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
        pose.translate(-centerX, -centerY, -centerZ);

        // Pass 1: Solid rocket parts first, then translucent ghost parts on top for clean depth blending
        for (int pass = 0; pass < 2; pass++) {
            boolean ghostPass = pass == 1;
            for (int i = 0; i < count; i++) {
                StackPart part = parts.get(i);
                if (part.ghost() != ghostPass) {
                    continue;
                }
                ResolvedModel rm = resolvedModels[i];
                if (rm == null) {
                    continue;
                }

                float shroudScaleY = "interstage_engine_shroud".equals(part.config().familyId()) ? 0.0F : 1.0F;
                RocketPartConfig engineAbove = resolveStackEngineAbove(parts, i);
                float r = ghostPass ? 0.55F : 1.0F;
                float g = ghostPass ? 1.0F : 1.0F;
                float b = ghostPass ? 0.72F : 1.0F;
                float a = ghostPass ? 0.62F : 1.0F;

                pose.pushPose();
                if (part.baseYBlocks() != 0.0F) {
                    pose.translate(0.0F, part.baseYBlocks(), 0.0F);
                }
                RocketPartMeshRenderer.render(
                        rm.model(),
                        rm.texture(),
                        pose,
                        graphics.bufferSource(),
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        part.animTimeSeconds(),
                        false,
                        r,
                        g,
                        b,
                        a,
                        shroudScaleY
                );
                if (engineAbove != null) {
                    boolean aboveIsGhost = i + 1 < count && parts.get(i + 1).ghost();
                    float sr = (ghostPass || aboveIsGhost) ? 0.55F : 1.0F;
                    float sg = 1.0F;
                    float sb = (ghostPass || aboveIsGhost) ? 0.72F : 1.0F;
                    float sa = (ghostPass || aboveIsGhost) ? 0.62F : 1.0F;
                    RocketPartShroudRenderer.render(
                            part.config(),
                            engineAbove,
                            rm.texture(),
                            pose,
                            graphics.bufferSource(),
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY,
                            sr,
                            sg,
                            sb,
                            sa
                    );
                }
                pose.popPose();
            }
        }

        graphics.flush();
        pose.popPose();
        graphics.disableScissor();
        Lighting.setupFor3DItems();

        if (outBands != null) {
            float cosPitch = Mth.cos(clampedPitch * Mth.DEG_TO_RAD);
            for (int i = 0; i < count; i++) {
                StackPart part = parts.get(i);
                float partH = Math.max(1, part.config().heightBlocks());
                float yBotScreen = vpCenterY - (part.baseYBlocks() - centerY) * cosPitch * scale;
                float yTopScreen = vpCenterY - (part.baseYBlocks() + partH - centerY) * cosPitch * scale;
                int y0 = Mth.clamp(Math.round(Math.min(yTopScreen, yBotScreen)), y + 2, y + height - 2);
                int y1 = Mth.clamp(Math.round(Math.max(yTopScreen, yBotScreen)), y + 2, y + height - 2);
                if (y1 - y0 < 6) {
                    int mid = (y0 + y1) / 2;
                    y0 = Mth.clamp(mid - 3, y + 2, y + height - 8);
                    y1 = y0 + 6;
                }
                int halfW = Math.max(18, Math.round(0.55F * Math.max(1, part.config().diameter()) * scale));
                int x0 = Mth.clamp(Math.round(vpCenterX - halfW), x + 4, x + width - 4);
                int x1 = Mth.clamp(Math.round(vpCenterX + halfW), x + 4, x + width - 4);
                outBands.add(new ProjectedBand(part.sourceIndex(), x0, y0, x1, y1, part.selected(), part.ghost()));
            }
        }

        return true;
    }

    /**
     * Draws subtle glowing selection corner brackets around a projected part band at an elevated Z
     * so overlays always appear cleanly in front of the 3D rocket mesh.
     */
    public static void renderBandBrackets(GuiGraphics graphics, ProjectedBand band, int color, float overlayZ) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, overlayZ);

        int x0 = band.x0();
        int y0 = band.y0();
        int x1 = band.x1();
        int y1 = band.y1();
        int arm = Math.min(6, Math.max(3, (y1 - y0) / 3));

        // Left bracket [
        graphics.fill(x0, y0, x0 + 2, y1, color);
        graphics.fill(x0, y0, x0 + arm, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x0 + arm, y1, color);

        // Right bracket ]
        graphics.fill(x1 - 2, y0, x1, y1, color);
        graphics.fill(x1 - arm, y0, x1, y0 + 1, color);
        graphics.fill(x1 - arm, y1 - 1, x1, y1, color);

        pose.popPose();
    }

    private static @Nullable RocketPartConfig resolveStackEngineAbove(List<StackPart> parts, int index) {
        RocketPartConfig cfg = parts.get(index).config();
        if (!RocketPartShroudRenderer.supportsEngineShroud(cfg)) {
            return null;
        }
        if (index + 1 < parts.size()) {
            RocketPartConfig above = parts.get(index + 1).config();
            if (RocketPartShroudRenderer.needsEngineShroud(above)) {
                return above;
            }
        }
        return null;
    }

    public record ResolvedModel(
            RocketPartRuntimeModel model,
            ResourceLocation texture,
            RocketPartSpec spec
    ) {
    }

    public record StackPart(
            int sourceIndex,
            RocketPartConfig config,
            float baseYBlocks,
            float animTimeSeconds,
            boolean selected,
            boolean ghost
    ) {
    }

    public record ProjectedBand(
            int sourceIndex,
            int x0,
            int y0,
            int x1,
            int y1,
            boolean selected,
            boolean ghost
    ) {
        public boolean contains(double mx, double my) {
            return mx >= this.x0 && mx <= this.x1 && my >= this.y0 && my <= this.y1;
        }
    }
}
