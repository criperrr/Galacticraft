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

package dev.galacticraft.mod.client.render.astro;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Physically inspired 3D planetary ring arch renderer for ringed gas/ice giants
 * (Saturn and Uranus) as viewed from within their atmosphere or high orbit.
 *
 * Features:
 * - Full horizon-to-horizon arched ring bands with authentic radial structure
 *   (Saturn's C, B, Cassini Division, A, and Encke Gap; Uranus's narrow icy ringlets)
 * - Dynamic planetary umbra/penumbra shadow cast onto the rings according to solar angle
 * - Atmospheric extinction near the horizon and smooth tinting/clarification with altitude
 */
public class PlanetaryRingSkyRenderer {
    public static final RingProfile SATURN = new RingProfile(
            38.0F,  // Tilt from zenith (peaks ~52 deg above horizon — iconic sweeping Saturnian arch)
            18.0F,  // Azimuth orientation
            new RingBand[]{
                    // C Ring (inner crepe ring - faint dusty veil)
                    new RingBand(0.00F, 0.20F, 0.72F, 0.64F, 0.50F, 0.22F, 6),
                    // B Ring (brightest, densest icy band with subtle ringlets)
                    new RingBand(0.20F, 0.56F, 0.98F, 0.90F, 0.74F, 0.86F, 12),
                    // Cassini Division (dark translucent gap where stars shine through)
                    new RingBand(0.56F, 0.64F, 0.45F, 0.40F, 0.32F, 0.04F, 2),
                    // A Ring inner
                    new RingBand(0.64F, 0.86F, 0.88F, 0.81F, 0.67F, 0.66F, 8),
                    // Encke Gap
                    new RingBand(0.86F, 0.89F, 0.50F, 0.45F, 0.36F, 0.06F, 2),
                    // A Ring outer + F Ring edge
                    new RingBand(0.89F, 1.00F, 0.82F, 0.75F, 0.62F, 0.50F, 4)
            },
            24.0F, // Angular band width across the sky dome (degrees)
            135.0F // Ring arch sky radius
    );

    public static final RingProfile URANUS = new RingProfile(
            14.0F,  // Steep near-vertical arch (peaks ~76 deg above horizon due to Uranus's 97.8 deg axial tilt)
            -35.0F,
            new RingBand[]{
                    new RingBand(0.00F, 0.14F, 0.62F, 0.88F, 0.96F, 0.22F, 3),
                    new RingBand(0.14F, 0.36F, 0.55F, 0.82F, 0.92F, 0.04F, 2),
                    new RingBand(0.36F, 0.52F, 0.70F, 0.93F, 1.00F, 0.34F, 4),
                    new RingBand(0.52F, 0.78F, 0.55F, 0.82F, 0.92F, 0.03F, 2),
                    new RingBand(0.78F, 1.00F, 0.78F, 0.96F, 1.00F, 0.46F, 4)
            },
            10.0F,
            140.0F
    );

    private static final int ARC_SEGMENTS = 96;

    public static void render(
            PoseStack basePoseStack,
            RingProfile profile,
            float celestialAngle,
            float spaceProgress,
            float atmR, float atmG, float atmB
    ) {
        render(basePoseStack, profile, celestialAngle, spaceProgress, atmR, atmG, atmB, 1.0F);
    }

    public static void render(
            PoseStack basePoseStack,
            RingProfile profile,
            float celestialAngle,
            float spaceProgress,
            float atmR, float atmG, float atmB,
            float opticalTransmittance
    ) {
        if (profile == null || opticalTransmittance <= 0.01F) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(basePoseStack.last().pose());

        // Orient the planetary ring plane across the sky
        poseStack.mulPose(Axis.YP.rotationDegrees(profile.azimuthDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(profile.tiltDeg));

        Matrix4f matrix = poseStack.last().pose();

        // Solar phase angle along the ring arc for casting the planet's own shadow across the rings
        // At midnight (celestialAngle ~0.5), shadow sits high near the arch zenith;
        // at dusk/dawn (~0.25 / 0.75), shadow sweeps down toward the horizon.
        float antiSolarAngleRad = (celestialAngle * Mth.TWO_PI) + Mth.PI;
        Vector3f antiSolarDir = new Vector3f(Mth.sin(antiSolarAngleRad), Mth.cos(antiSolarAngleRad), 0.0F).normalize();

        // Atmospheric scattering tint weakens smoothly as player ascends into space
        float atmosphereFactor = 1.0F - spaceProgress;
        float dayFactor = Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 1.8F + 0.55F, 0.22F, 1.0F);
        float ringGlobalBrightness = Mth.lerp(spaceProgress, 0.68F + 0.32F * dayFactor, 1.0F);

        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Flatten all sub-bands into concentric radial strips for crisp Cassini/Encke gaps and fine ringlets
        for (RingBand band : profile.bands) {
            int subSteps = Math.max(1, band.radialSteps);
            for (int s = 0; s < subSteps; s++) {
                float f0 = Mth.lerp((float) s / subSteps, band.innerFrac, band.outerFrac);
                float f1 = Mth.lerp((float) (s + 1) / subSteps, band.innerFrac, band.outerFrac);
                float fMid = 0.5F * (f0 + f1);

                // Subtle fine ringlet texture modulation
                float ringletMod = 0.88F + 0.12F * Mth.sin(fMid * 95.0F) * Mth.cos(fMid * 37.0F);
                // Softly feather inner/outer extreme edges of the whole ring system
                float edgeFeather0 = Mth.clamp(f0 * 14.0F, 0.0F, 1.0F) * Mth.clamp((1.0F - f0) * 14.0F, 0.0F, 1.0F);
                float edgeFeather1 = Mth.clamp(f1 * 14.0F, 0.0F, 1.0F) * Mth.clamp((1.0F - f1) * 14.0F, 0.0F, 1.0F);

                float lat0 = (float) Math.toRadians((f0 - 0.5F) * profile.angularWidthDeg);
                float lat1 = (float) Math.toRadians((f1 - 0.5F) * profile.angularWidthDeg);

                float cosLat0 = Mth.cos(lat0);
                float sinLat0 = Mth.sin(lat0);
                float cosLat1 = Mth.cos(lat1);
                float sinLat1 = Mth.sin(lat1);

                for (int i = 0; i < ARC_SEGMENTS; i++) {
                    float a0 = ((float) i / ARC_SEGMENTS) * Mth.TWO_PI;
                    float a1 = ((float) (i + 1) / ARC_SEGMENTS) * Mth.TWO_PI;

                    float cosA0 = Mth.cos(a0);
                    float sinA0 = Mth.sin(a0);
                    float cosA1 = Mth.cos(a1);
                    float sinA1 = Mth.sin(a1);

                    // 4 corners on the celestial ring arch sphere
                    float x00 = profile.radius * cosLat0 * cosA0;
                    float y00 = profile.radius * cosLat0 * sinA0;
                    float z00 = profile.radius * sinLat0;

                    float x10 = profile.radius * cosLat0 * cosA1;
                    float y10 = profile.radius * cosLat0 * sinA1;
                    float z10 = profile.radius * sinLat0;

                    float x11 = profile.radius * cosLat1 * cosA1;
                    float y11 = profile.radius * cosLat1 * sinA1;
                    float z11 = profile.radius * sinLat1;

                    float x01 = profile.radius * cosLat1 * cosA0;
                    float y01 = profile.radius * cosLat1 * sinA0;
                    float z01 = profile.radius * sinLat1;

                    // Compute horizon extinction & planet umbra shadow for both arc angles
                    float vis0 = computeArcVisibility(cosA0, sinA0, profile.tiltDeg, atmosphereFactor);
                    float vis1 = computeArcVisibility(cosA1, sinA1, profile.tiltDeg, atmosphereFactor);
                    if (vis0 <= 0.002F && vis1 <= 0.002F) {
                        continue;
                    }

                    float shadow0 = computePlanetShadow(cosA0, sinA0, antiSolarDir);
                    float shadow1 = computePlanetShadow(cosA1, sinA1, antiSolarDir);

                    // Blend ring ice color with subtle atmospheric scattering tint when below cloud top
                    float atmBlend = atmosphereFactor * 0.28F;
                    float baseR = Mth.lerp(atmBlend, band.r * ringletMod, atmR);
                    float baseG = Mth.lerp(atmBlend, band.g * ringletMod, atmG);
                    float baseB = Mth.lerp(atmBlend, band.b * ringletMod, atmB);

                    float light0 = ringGlobalBrightness * shadow0;
                    float light1 = ringGlobalBrightness * shadow1;

                    float alpha00 = Mth.clamp(band.alpha * edgeFeather0 * vis0 * (0.35F + 0.65F * shadow0) * opticalTransmittance, 0.0F, 0.95F);
                    float alpha10 = Mth.clamp(band.alpha * edgeFeather0 * vis1 * (0.35F + 0.65F * shadow1) * opticalTransmittance, 0.0F, 0.95F);
                    float alpha11 = Mth.clamp(band.alpha * edgeFeather1 * vis1 * (0.35F + 0.65F * shadow1) * opticalTransmittance, 0.0F, 0.95F);
                    float alpha01 = Mth.clamp(band.alpha * edgeFeather1 * vis0 * (0.35F + 0.65F * shadow0) * opticalTransmittance, 0.0F, 0.95F);

                    float r0 = Mth.clamp(baseR * light0, 0.0F, 1.0F);
                    float g0 = Mth.clamp(baseG * light0, 0.0F, 1.0F);
                    float b0 = Mth.clamp(baseB * light0, 0.0F, 1.0F);

                    float r1 = Mth.clamp(baseR * light1, 0.0F, 1.0F);
                    float g1 = Mth.clamp(baseG * light1, 0.0F, 1.0F);
                    float b1 = Mth.clamp(baseB * light1, 0.0F, 1.0F);

                    // Triangle 1
                    bb.addVertex(matrix, x00, y00, z00).setColor(r0, g0, b0, alpha00);
                    bb.addVertex(matrix, x10, y10, z10).setColor(r1, g1, b1, alpha10);
                    bb.addVertex(matrix, x11, y11, z11).setColor(r1, g1, b1, alpha11);

                    // Triangle 2
                    bb.addVertex(matrix, x00, y00, z00).setColor(r0, g0, b0, alpha00);
                    bb.addVertex(matrix, x11, y11, z11).setColor(r1, g1, b1, alpha11);
                    bb.addVertex(matrix, x01, y01, z01).setColor(r0, g0, b0, alpha01);
                }
            }
        }

        MeshData mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Fades ring segments smoothly near and below the atmospheric horizon (airmass extinction),
     * while allowing the full 360-degree ring to become visible once in high space orbit.
     */
    private static float computeArcVisibility(float cosA, float sinA, float tiltDeg, float atmosphereFactor) {
        // Approximate world-space elevation of this ring point after X-axis tilt
        float worldY = sinA * Mth.cos((float) Math.toRadians(tiltDeg));
        float horizonFade = Mth.clamp((worldY + 0.08F) / 0.28F, 0.0F, 1.0F);
        // In deep space (atmosphereFactor -> 0), the full ring arch wraps around cleanly
        return Mth.lerp(atmosphereFactor, 1.0F, horizonFade);
    }

    /**
     * Computes the planet's own shadow (umbra + soft penumbra) cast onto the rings.
     * Returns 1.0 in full sunlight and ~0.08 inside the planetary umbra.
     */
    private static float computePlanetShadow(float cosA, float sinA, Vector3f antiSolarDir) {
        float dot = cosA * antiSolarDir.x + sinA * antiSolarDir.y;
        // Shadow cone spans roughly +/- 22 deg around the anti-solar point on the ring
        float umbraEdge = 0.927F;     // cos(22 deg)
        float penumbraEdge = 0.875F;  // cos(29 deg)
        if (dot <= penumbraEdge) {
            return 1.0F;
        }
        if (dot >= umbraEdge) {
            return 0.08F;
        }
        float t = (dot - penumbraEdge) / (umbraEdge - penumbraEdge);
        // Smoothstep penumbra transition
        t = t * t * (3.0F - 2.0F * t);
        return Mth.lerp(t, 1.0F, 0.08F);
    }

    public record RingProfile(
            float tiltDeg,
            float azimuthDeg,
            RingBand[] bands,
            float angularWidthDeg,
            float radius
    ) {
    }

    public record RingBand(
            float innerFrac,
            float outerFrac,
            float r, float g, float b,
            float alpha,
            int radialSteps
    ) {
    }
}
