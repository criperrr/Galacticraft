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

package dev.galacticraft.mod.client.render.dimension;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.galacticraft.mod.Constant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders a massive, crisp, high-detail square Minecraft-style planet directly underneath the player
 * when ascending into space (Y=500 -> Y=700+), using a cached 128x128 procedural pixel-art texture
 * per planet (no blurry 16x16 GUI icon or black square outline) plus an optional luminous atmospheric halo.
 */
public class PlanetUnderPlayerRenderer {
    private static final int TEXTURE_SIZE = 256;
    private static final Map<ResourceLocation, ResourceLocation> DETAILED_TEXTURE_CACHE = new HashMap<>();

    public static void renderPlanetBelow(
            PoseStack poseStack,
            Level level,
            ResourceLocation planetTexture,
            @Nullable ResourceLocation atmosphereTexture,
            double playerY,
            float partialTicks,
            float startHeight,
            float baseScaleMultiplier,
            float haloR,
            float haloG,
            float haloB
    ) {
        if (playerY <= startHeight) {
            return;
        }

        float bandSpan = Math.max(1.0F, (float) (Constant.ATMOSPHERE_EXIT_END - startHeight));
        float rawProgress = Mth.clamp((float) ((playerY - startHeight) / bandSpan), 0.0F, 1.0F);
        float alpha = rawProgress * rawProgress * (3.0F - 2.0F * rawProgress);
        if (alpha <= 0.005F) {
            return;
        }

        float celestialAngle = level.getTimeOfDay(partialTicks);
        float light = Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 0.65F + 0.65F, 0.22F, 1.0F);

        // Massive square planet underneath the player: colossal at Y=500..700, smoothly receding as altitude climbs
        float altitudeAboveStart = (float) Math.max(0.0D, playerY - startHeight);
        float size = (480.0F / (1.0F + altitudeAboveStart / 580.0F)) * baseScaleMultiplier;
        size = Math.max(size, 85.0F * baseScaleMultiplier);

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // 1. Optional luminous atmospheric glow halo around the square planet
        if (atmosphereTexture != null && (haloR > 0.01F || haloG > 0.01F || haloB > 0.01F)) {
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderColor(haloR * light, haloG * light, haloB * light, alpha * 0.55F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, atmosphereTexture);
            float haloSize = size * 1.12F;
            BufferBuilder haloBuffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            haloBuffer.addVertex(matrix, -haloSize, -100.3F, haloSize).setUv(0.0F, 1.0F);
            haloBuffer.addVertex(matrix, haloSize, -100.3F, haloSize).setUv(1.0F, 1.0F);
            haloBuffer.addVertex(matrix, haloSize, -100.3F, -haloSize).setUv(1.0F, 0.0F);
            haloBuffer.addVertex(matrix, -haloSize, -100.3F, -haloSize).setUv(0.0F, 0.0F);
            BufferUploader.drawWithShader(haloBuffer.buildOrThrow());
        }

        // 2. Massive high-detail 128x128 square planet quad
        ResourceLocation detailedTex = getOrCreateDetailedTexture(planetTexture);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(light, light, light, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, detailedTex);

        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -size, -100.0F, size).setUv(0.0F, 1.0F);
        buffer.addVertex(matrix, size, -100.0F, size).setUv(1.0F, 1.0F);
        buffer.addVertex(matrix, size, -100.0F, -size).setUv(1.0F, 0.0F);
        buffer.addVertex(matrix, -size, -100.0F, -size).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);

        poseStack.popPose();
    }

    private static ResourceLocation getOrCreateDetailedTexture(ResourceLocation planetId) {
        return DETAILED_TEXTURE_CACHE.computeIfAbsent(planetId, id -> {
            PlanetStyle style = PlanetStyle.fromTexture(id);
            NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, false);

            for (int py = 0; py < TEXTURE_SIZE; py++) {
                float v = (py + 0.5F) / TEXTURE_SIZE;
                for (int px = 0; px < TEXTURE_SIZE; px++) {
                    float u = (px + 0.5F) / TEXTURE_SIZE;
                    Vector3f col = sampleSquarePlanetPixel(style, u, v);
                    int r = Mth.clamp((int) (col.x * 255.0F), 0, 255);
                    int g = Mth.clamp((int) (col.y * 255.0F), 0, 255);
                    int b = Mth.clamp((int) (col.z * 255.0F), 0, 255);
                    // NativeImage ABGR pixel format
                    int abgr = (0xFF << 24) | (b << 16) | (g << 8) | r;
                    image.setPixelRGBA(px, py, abgr);
                }
            }

            DynamicTexture dynTex = new DynamicTexture(image);
            dynTex.setFilter(false, false); // Crisp pixel-art Minecraft aesthetic
            ResourceLocation registered = Constant.id("dynamic/planet_below_" + style.name().toLowerCase());
            Minecraft.getInstance().getTextureManager().register(registered, dynTex);
            return registered;
        });
    }

    private static Vector3f sampleSquarePlanetPixel(PlanetStyle style, float u, float v) {
        // Subtle vignette darkening near square edges for 3D depth without any hard black outline
        float dx = Math.abs(u - 0.5F) * 2.0F;
        float dy = Math.abs(v - 0.5F) * 2.0F;
        float edgeDist = Math.max(dx, dy);
        float edgeShade = 1.0F - 0.22F * (edgeDist * edgeDist);

        // Domain warping for organic continents, canyons, and turbulent cloud swirls
        float warpX = (fbm2(u * 4.0F + 3.1F, v * 4.0F - 7.4F) - 0.5F) * 0.14F;
        float warpY = (fbm2(u * 4.0F - 9.2F, v * 4.0F + 5.8F) - 0.5F) * 0.14F;
        float wu = u + warpX;
        float wv = v + warpY;

        float n1 = fbm2(wu * 4.5F, wv * 4.5F);
        float n2 = fbm2(wu * 12.0F + 17.3F, wv * 12.0F - 8.7F);
        float n3 = fbm2(u * 28.0F - 5.1F, v * 28.0F + 13.4F);
        float micro = fbm2(u * 64.0F + 41.2F, v * 64.0F - 19.6F);
        // Ridged noise for mountain ranges, tectonic canyons & fractures
        float ridge = 1.0F - Math.abs(fbm2(wu * 7.5F + 2.3F, wv * 7.5F + 11.7F) * 2.0F - 1.0F);
        ridge = ridge * ridge;

        Vector3f col = switch (style) {
            case EARTH -> {
                float continent = n1 * 0.62F + n2 * 0.24F + n3 * 0.10F + micro * 0.04F;
                float swirlU = u + (n2 - 0.5F) * 0.16F;
                float swirlV = v + (n1 - 0.5F) * 0.08F;
                float clouds = Mth.clamp((fbm2(swirlU * 7.5F + 31.0F, swirlV * 5.0F - 12.0F) - 0.50F) * 3.1F, 0.0F, 1.0F);
                clouds = Mth.clamp(clouds + (micro - 0.5F) * 0.25F * clouds, 0.0F, 1.0F);

                Vector3f base;
                if (continent < 0.48F) {
                    float depth = Mth.clamp((0.48F - continent) * 3.4F, 0.0F, 1.0F);
                    Vector3f abyss = new Vector3f(0.03F, 0.13F, 0.42F);
                    Vector3f ocean = new Vector3f(0.06F, 0.28F, 0.66F);
                    Vector3f reef = new Vector3f(0.14F, 0.58F, 0.78F);
                    base = depth > 0.35F
                            ? new Vector3f(ocean).lerp(abyss, (depth - 0.35F) / 0.65F)
                            : new Vector3f(reef).lerp(ocean, depth / 0.35F);
                } else if (continent < 0.64F) {
                    float lush = Mth.clamp((continent - 0.48F) / 0.16F, 0.0F, 1.0F);
                    Vector3f forest = new Vector3f(0.12F, 0.42F, 0.18F);
                    Vector3f savanna = new Vector3f(0.46F, 0.52F, 0.24F);
                    Vector3f desert = new Vector3f(0.76F, 0.64F, 0.40F);
                    base = new Vector3f(forest).lerp(savanna, Mth.clamp(lush * 1.4F, 0.0F, 1.0F));
                    base.lerp(desert, Mth.clamp((n2 - 0.48F) * 2.2F, 0.0F, 1.0F));
                } else {
                    float peak = Mth.clamp((continent - 0.64F) * 4.5F + ridge * 0.35F, 0.0F, 1.0F);
                    base = new Vector3f(0.56F, 0.48F, 0.36F).lerp(new Vector3f(0.94F, 0.96F, 0.98F), peak);
                }
                // Polar ice caps at top & bottom edges
                if (v < 0.07F + (n2 - 0.5F) * 0.04F || v > 0.93F + (n2 - 0.5F) * 0.04F) {
                    base.lerp(new Vector3f(0.94F, 0.97F, 1.00F), 0.85F);
                }
                // Cloud shadows + bright white cloud decks
                base.mul(1.0F - clouds * 0.18F);
                base.lerp(new Vector3f(0.97F, 0.98F, 1.00F), clouds * 0.82F);
                yield base;
            }
            case MARS -> {
                // Multi-scale iron-oxide plains, basaltic basins, Valles Marineris canyon scar, craters with bright rims
                float terrain = n1 * 0.46F + n2 * 0.28F + n3 * 0.16F + micro * 0.10F;
                float largeCrater = craterWithRim(u * 6.5F, v * 6.5F);
                float smallCrater = craterWithRim(u * 18.0F + 7.3F, v * 18.0F - 4.1F);
                // Equatorial tectonic canyon scar
                float canyonBand = Math.abs((v - 0.52F) + (n1 - 0.5F) * 0.14F);
                float canyon = canyonBand < 0.035F && u > 0.22F && u < 0.78F ? (1.0F - canyonBand / 0.035F) : 0.0F;

                terrain = Mth.clamp(terrain + largeCrater * 0.20F + smallCrater * 0.12F - canyon * 0.34F + ridge * 0.08F, 0.0F, 1.0F);

                Vector3f deepCanyon = new Vector3f(0.30F, 0.12F, 0.07F);
                Vector3f darkBasalt = new Vector3f(0.48F, 0.21F, 0.11F);
                Vector3f rustPlains = new Vector3f(0.78F, 0.36F, 0.18F);
                Vector3f brightOchre = new Vector3f(0.94F, 0.58F, 0.33F);
                Vector3f base;
                if (terrain < 0.30F) {
                    base = new Vector3f(deepCanyon).lerp(darkBasalt, terrain / 0.30F);
                } else if (terrain < 0.62F) {
                    base = new Vector3f(darkBasalt).lerp(rustPlains, (terrain - 0.30F) / 0.32F);
                } else {
                    base = new Vector3f(rustPlains).lerp(brightOchre, (terrain - 0.62F) / 0.38F);
                }
                // Polar ice caps with frost fractures
                if (v < 0.08F + (n2 - 0.5F) * 0.05F || v > 0.94F + (n3 - 0.5F) * 0.03F) {
                    base.lerp(new Vector3f(0.95F, 0.91F, 0.88F), 0.78F);
                }
                yield base;
            }
            case VENUS -> {
                float band = 0.5F + 0.5F * Mth.sin((wv * 9.5F + n1 * 3.2F) * Mth.PI);
                float chevron = 0.5F + 0.5F * Mth.cos((wu * 5.0F + Math.abs(v - 0.5F) * 6.0F) * Mth.PI);
                float swirl = band * 0.52F + chevron * 0.20F + n2 * 0.18F + micro * 0.10F;
                yield new Vector3f(0.70F, 0.48F, 0.16F).lerp(new Vector3f(0.99F, 0.90F, 0.56F), swirl);
            }
            case JUPITER -> {
                float latWave = v * 15.0F + n1 * 1.4F + n2 * 0.55F + micro * 0.15F;
                float b1 = 0.5F + 0.5F * Mth.sin(latWave * Mth.PI);
                float b2 = 0.5F + 0.5F * Mth.cos(latWave * 2.4F * Mth.PI);
                float eddy = 0.5F + 0.5F * Mth.sin((wu * 22.0F + v * 18.0F) * Mth.PI);
                Vector3f cream = new Vector3f(0.95F, 0.87F, 0.72F);
                Vector3f rustBelt = new Vector3f(0.68F, 0.34F, 0.18F);
                Vector3f ochre = new Vector3f(0.86F, 0.62F, 0.38F);
                Vector3f slate = new Vector3f(0.54F, 0.52F, 0.56F);
                Vector3f base = new Vector3f(rustBelt).lerp(cream, b1);
                base.lerp(ochre, b2 * 0.45F);
                base.lerp(slate, eddy * (1.0F - b1) * 0.16F);
                // Great Red Spot with swirling internal eye
                float grsDx = (u - 0.62F) / 0.12F;
                float grsDy = (v - 0.64F) / 0.072F;
                float grsDist = grsDx * grsDx + grsDy * grsDy;
                if (grsDist < 1.0F) {
                    float eye = 0.5F + 0.5F * Mth.sin(Mth.sqrt(grsDist) * 10.0F + n2 * 3.0F);
                    Vector3f grsCol = new Vector3f(0.82F, 0.24F, 0.14F).lerp(new Vector3f(0.96F, 0.52F, 0.30F), eye * 0.45F);
                    base.lerp(grsCol, (1.0F - grsDist) * 0.92F);
                }
                yield base;
            }
            case SATURN -> {
                float latWave = v * 13.0F + n1 * 0.65F + n3 * 0.18F;
                float b1 = 0.5F + 0.5F * Mth.sin(latWave * Mth.PI);
                float b2 = 0.5F + 0.5F * Mth.cos(latWave * 2.8F * Mth.PI);
                Vector3f base = new Vector3f(0.72F, 0.61F, 0.40F).lerp(new Vector3f(0.97F, 0.91F, 0.72F), b1 * 0.70F + b2 * 0.20F + micro * 0.10F);
                yield base;
            }
            case URANUS -> {
                float band = 0.5F + 0.5F * Mth.sin((v * 8.0F + n1 * 0.45F) * Mth.PI);
                float haze = Mth.clamp((n3 - 0.55F) * 2.5F, 0.0F, 1.0F);
                Vector3f base = new Vector3f(0.40F, 0.76F, 0.84F).lerp(new Vector3f(0.70F, 0.94F, 0.98F), band * 0.65F + micro * 0.35F);
                base.lerp(new Vector3f(0.85F, 0.98F, 1.00F), haze * 0.25F);
                yield base;
            }
            case NEPTUNE -> {
                float band = 0.5F + 0.5F * Mth.sin((v * 11.0F + n1 * 1.2F) * Mth.PI);
                Vector3f base = new Vector3f(0.10F, 0.24F, 0.70F).lerp(new Vector3f(0.24F, 0.52F, 0.96F), band * 0.8F + micro * 0.2F);
                // Great Dark Spot
                float dsDx = (u - 0.38F) / 0.10F;
                float dsDy = (v - 0.56F) / 0.06F;
                float dsDist = dsDx * dsDx + dsDy * dsDy;
                if (dsDist < 1.0F) {
                    base.lerp(new Vector3f(0.06F, 0.14F, 0.45F), (1.0F - dsDist) * 0.80F);
                }
                float streak = Mth.clamp((n2 - 0.64F) * 4.5F, 0.0F, 1.0F);
                base.lerp(new Vector3f(0.86F, 0.94F, 1.00F), streak * 0.72F);
                yield base;
            }
            case MERCURY -> {
                float c1 = craterWithRim(u * 8.0F, v * 8.0F);
                float c2 = craterWithRim(u * 22.0F + 5.1F, v * 22.0F - 9.3F);
                float reg = Mth.clamp(n1 * 0.42F + n2 * 0.28F + n3 * 0.18F + micro * 0.12F + c1 * 0.24F + c2 * 0.14F, 0.0F, 1.0F);
                yield new Vector3f(0.28F, 0.26F, 0.25F).lerp(new Vector3f(0.70F, 0.67F, 0.62F), reg);
            }
            case MOON, CERES -> {
                float c1 = craterWithRim(u * 7.5F, v * 7.5F);
                float c2 = craterWithRim(u * 20.0F - 3.7F, v * 20.0F + 8.2F);
                float reg = Mth.clamp(n1 * 0.46F + n2 * 0.26F + n3 * 0.16F + micro * 0.12F + c1 * 0.24F + c2 * 0.14F, 0.0F, 1.0F);
                yield new Vector3f(0.26F, 0.27F, 0.30F).lerp(new Vector3f(0.82F, 0.82F, 0.85F), reg);
            }
        };

        // Subtle crisp pixel-art micro grain
        float grain = 0.96F + 0.08F * micro;
        return col.mul(edgeShade * grain);
    }

    /**
     * Returns negative values inside crater bowls and positive bright ejecta rim highlights around the edge.
     */
    private static float craterWithRim(float x, float y) {
        int ix = Mth.floor(x);
        int iy = Mth.floor(y);
        float minDist = 1.0F;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = ix + dx;
                int cy = iy + dy;
                float rx = cx + hash2(cx, cy);
                float ry = cy + hash2(cx + 31, cy - 17);
                float d = Mth.sqrt((x - rx) * (x - rx) + (y - ry) * (y - ry));
                if (d < minDist) {
                    minDist = d;
                }
            }
        }
        if (minDist < 0.20F) {
            return -(1.0F - minDist / 0.20F);
        }
        if (minDist < 0.29F) {
            float rimT = (minDist - 0.20F) / 0.09F;
            return Mth.sin(rimT * Mth.PI) * 0.55F;
        }
        return 0.0F;
    }

    private static float fbm2(float x, float y) {
        float sum = 0.0F;
        float amp = 0.55F;
        float freq = 1.0F;
        for (int i = 0; i < 4; i++) {
            sum += amp * valueNoise2(x * freq, y * freq);
            freq *= 2.05F;
            amp *= 0.48F;
        }
        return Mth.clamp(sum, 0.0F, 1.0F);
    }

    private static float valueNoise2(float x, float y) {
        int ix = Mth.floor(x);
        int iy = Mth.floor(y);
        float fx = x - ix;
        float fy = y - iy;
        fx = fx * fx * (3.0F - 2.0F * fx);
        fy = fy * fy * (3.0F - 2.0F * fy);

        float c00 = hash2(ix, iy);
        float c10 = hash2(ix + 1, iy);
        float c01 = hash2(ix, iy + 1);
        float c11 = hash2(ix + 1, iy + 1);

        float x0 = Mth.lerp(fx, c00, c10);
        float x1 = Mth.lerp(fx, c01, c11);
        return Mth.lerp(fy, x0, x1);
    }

    private static float hash2(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h ^= (h >> 16);
        return (h & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }

    private enum PlanetStyle {
        EARTH,
        MOON,
        MARS,
        VENUS,
        MERCURY,
        JUPITER,
        SATURN,
        URANUS,
        NEPTUNE,
        CERES;

        static PlanetStyle fromTexture(ResourceLocation id) {
            if (id == null) {
                return MOON;
            }
            String path = id.getPath();
            if (path.contains("earth")) return EARTH;
            if (path.contains("mars")) return MARS;
            if (path.contains("venus")) return VENUS;
            if (path.contains("mercury")) return MERCURY;
            if (path.contains("jupiter")) return JUPITER;
            if (path.contains("saturn")) return SATURN;
            if (path.contains("uranus")) return URANUS;
            if (path.contains("neptune")) return NEPTUNE;
            if (path.contains("ceres")) return CERES;
            return MOON;
        }
    }

    /**
     * Specialized orbital projection for space stations / satellites in orbit around a planet.
     */
    public static void renderPlanetBelowOrbit(
            PoseStack poseStack,
            Level level,
            ResourceLocation planetTexture,
            @Nullable ResourceLocation atmosphereTexture,
            float partialTicks,
            float size,
            float haloR,
            float haloG,
            float haloB
    ) {
        float celestialAngle = level.getTimeOfDay(partialTicks);
        float light = Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 0.75F + 0.6F, 0.2F, 1.0F);

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (atmosphereTexture != null) {
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderColor(haloR * light, haloG * light, haloB * light, 0.6F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, atmosphereTexture);
            float haloSize = size * 1.1F;
            BufferBuilder haloBuffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            haloBuffer.addVertex(matrix, -haloSize, -100.2F, haloSize).setUv(0.0F, 1.0F);
            haloBuffer.addVertex(matrix, haloSize, -100.2F, haloSize).setUv(1.0F, 1.0F);
            haloBuffer.addVertex(matrix, haloSize, -100.2F, -haloSize).setUv(1.0F, 0.0F);
            haloBuffer.addVertex(matrix, -haloSize, -100.2F, -haloSize).setUv(0.0F, 0.0F);
            BufferUploader.drawWithShader(haloBuffer.buildOrThrow());
        }

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(light, light, light, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, getOrCreateDetailedTexture(planetTexture));

        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -size, -100.0F, size).setUv(0.0F, 1.0F);
        buffer.addVertex(matrix, size, -100.0F, size).setUv(1.0F, 1.0F);
        buffer.addVertex(matrix, size, -100.0F, -size).setUv(1.0F, 0.0F);
        buffer.addVertex(matrix, -size, -100.0F, -size).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);

        poseStack.popPose();
    }
}
