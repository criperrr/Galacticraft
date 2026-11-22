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

import com.mojang.blaze3d.vertex.*;
import dev.galacticraft.mod.client.render.astro.deepsky.DeepskyManager;
import dev.galacticraft.mod.client.render.astro.deepsky.DeepskyObject;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;

public class AstroStarRenderer {
    public static final AstroSkyConfig.StarRenderingConfig options = AstroSkyEngine.CONFIG.starRenderingOptions;
    public static Vector3f baseStarColor = AstroColorUtils.fromHex(AstroSkyEngine.CONFIG.skyboxColor);
    public static float saturation = Mth.sqrt(0.01F * (float) options.starSaturation);

    private static final ArrayList<Star> dynamicStars = new ArrayList<>();
    private static final ArrayList<Star> liveStars = new ArrayList<>();
    private static final HashMap<BufferKey, VertexBuffer> buffers = new HashMap<>();
    private static final HashMap<BufferKey, Boolean> dirty = new HashMap<>();

    public static double getResolution() {
        return options.minimumRadius;
    }

    public static void reloadConfig() {
        baseStarColor = AstroColorUtils.fromHex(AstroSkyEngine.CONFIG.skyboxColor);
        saturation = Mth.sqrt(0.01F * (float) options.starSaturation);
    }

    public static Vector3f getPosition(double rightAscension, double declination) {
        return new Vector3f(
                (float) (-Math.sin(declination / 180.0 * Math.PI)),
                (float) (Math.cos(declination / 180.0 * Math.PI) * Math.cos(rightAscension / 180.0 * Math.PI)),
                (float) (Math.cos(declination / 180.0 * Math.PI) * -Math.sin(rightAscension / 180.0 * Math.PI))
        ).normalize(100.0F);
    }

    public static void initDynamicStars() {
        dynamicStars.clear();
    }

    public static void addDynamicStar(double rightAscension, double declination, double magnitude, double radius, Vector3f color) {
        Vector3f pos = getPosition(rightAscension, declination);
        dynamicStars.add(new Star(pos, color, (float) magnitude, (float) radius));
    }

    public static void addDynamicStar(Vector3d viewPos, double magnitude, double radius, Vector3f color) {
        Vector3f pos = new Vector3f((float) viewPos.x, (float) viewPos.y, (float) viewPos.z).normalize(100.0F);
        pos.y *= -1.0F;
        pos.rotateX((float) Math.toRadians(23.45));
        pos.rotateZ((float) (Math.PI / 2)).negate();
        dynamicStars.add(new Star(pos, color, (float) magnitude, (float) radius));
    }

    public static void initLiveStars() {
        liveStars.clear();
    }

    public static void addLiveStar(Vector3d viewPos, double magnitude, double radius, Vector3f color) {
        Vector3f pos = new Vector3f((float) viewPos.x, (float) viewPos.y, (float) viewPos.z).normalize(100.0F);
        pos.y *= -1.0F;
        pos.rotateX((float) Math.toRadians(23.45));
        pos.rotateZ((float) (Math.PI / 2)).negate();
        liveStars.add(new Star(pos, color, (float) magnitude, (float) radius));
    }

    private static void genStarVertices(BufferBuilder bufferBuilder, Vector3f pos, Vector3f color, float radius, float rotation, float ra) {
        Quaternionf quaternionf = new Quaternionf();
        quaternionf.identity().rotateTo(new Vector3f(-1.0F, 0.0F, 0.0F), pos).rotateX(rotation - ra);
        bufferBuilder.addVertex(new Vector3f(0.0F, radius, -radius).rotate(quaternionf).add(pos)).setColor(color.x, color.y, color.z, 1.0F);
        bufferBuilder.addVertex(new Vector3f(0.0F, radius, radius).rotate(quaternionf).add(pos)).setColor(color.x, color.y, color.z, 1.0F);
        bufferBuilder.addVertex(new Vector3f(0.0F, -radius, radius).rotate(quaternionf).add(pos)).setColor(color.x, color.y, color.z, 1.0F);
        bufferBuilder.addVertex(new Vector3f(0.0F, -radius, -radius).rotate(quaternionf).add(pos)).setColor(color.x, color.y, color.z, 1.0F);
    }

    public static void genDummyVertices(BufferBuilder bufferBuilder) {
        Vector3f pos = new Vector3f(0.0F, 0.0F, 0.0F);
        bufferBuilder.addVertex(pos).setColor(0);
        bufferBuilder.addVertex(pos).setColor(0);
        bufferBuilder.addVertex(pos).setColor(0);
        bufferBuilder.addVertex(pos).setColor(0);
    }

    public static void renderStar(BufferBuilder bufferBuilder, Star star, float scale, float bonus) {
        double magn = (double) (star.magn + bonus);
        if (magn < 0.0) {
            magn = -Math.log1p(-magn);
        }

        float total = (float) Math.pow((double) options.brightnessPower, -magn);
        float lightRadius = options.baseRadius * Mth.sqrt(total) * scale;
        if (star.radius > 0.0F) {
            lightRadius -= star.radius;
        }

        float radius = Math.max(lightRadius, Math.max(star.radius, options.minimumRadius * scale));
        float brightness = Mth.clamp(lightRadius / radius, 0.0F, 1.0F);
        float purkinje = brightness / (brightness + 1.0F);
        Vector3f renderColor = new Vector3f(baseStarColor)
                .lerp(
                        new Vector3f(star.color)
                                .mul(
                                        baseStarColor.x + (1.0F - baseStarColor.x) * purkinje,
                                        baseStarColor.y + (1.0F - baseStarColor.y) * purkinje,
                                        baseStarColor.z + (1.0F - baseStarColor.z) * purkinje
                                ),
                        saturation
                );
        renderColor.mul(brightness);
        float ra = star.pos.angleSigned(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(1.0F, 0.0F, 0.0F));
        float rotation = (float) (-magn) * 2.0F * (float) Math.PI;
        if (star.radius > 0.0F && radius * radius < 2.0F * star.radius * star.radius) {
            rotation = Mth.clamp(rotation, 0.0F, (float) Math.acos((double) (star.radius / radius)));
        }

        float tradius = 100.0F * radius / 90.0F * (float) (Math.PI / 2);
        genStarVertices(bufferBuilder, star.pos, renderColor, tradius, rotation, ra);
    }

    private static boolean inRegion(Vector3f position, Vector3f region, float regionSizeSquared) {
        return position == null || region == null || position.distanceSquared(region) < regionSizeSquared;
    }

    private static MeshData renderBuffer(BufferKey key) {
        float maxMagn = (float) AstroSkyEngine.CONFIG.magnitudeLimit - (float) key.bonus;
        float scale = 1.0F / (float) Math.max(1L, key.scale);
        Vector3f region = key.region != null ? new Vector3f((float) key.region.x, (float) key.region.y, (float) key.region.z).normalize(100.0F) : null;
        float regionSize = 50000.0F * scale * scale;
        double resolutionSquared = getResolution() * getResolution() * (double) scale * (double) scale;
        BufferBuilder bufferBuilder = AstroRenderUtils.setupBuffer(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        switch (key.level) {
            case LIVE -> {
                for (Star star : liveStars) {
                    if (star.magn <= maxMagn) {
                        renderStar(bufferBuilder, star, scale, (float) key.bonus);
                    }
                }
            }
            case DYNAMIC -> {
                for (Star star : dynamicStars) {
                    if (star.magn <= maxMagn) {
                        renderStar(bufferBuilder, star, scale, (float) key.bonus);
                    }
                }
            }
            case STATIC -> {
                for (DeepskyObject dso : DeepskyManager.getObjects()) {
                    if (!(dso.magnitude > (double) maxMagn) && inRegion(dso.starPos, region, regionSize)) {
                        if (dso.getArea() <= resolutionSquared) {
                            renderStar(bufferBuilder, dso.renderAsStar(), scale, (float) key.bonus);
                        }

                        for (Star star : dso) {
                            if (star.magn > maxMagn) {
                                break;
                            }
                            if (inRegion(star.pos, region, regionSize)) {
                                renderStar(bufferBuilder, star, scale, (float) key.bonus);
                            }
                        }
                    }
                }
            }
        }

        genDummyVertices(bufferBuilder);
        return bufferBuilder.buildOrThrow();
    }

    private static VertexBuffer getBuffer(BufferKey key) {
        if (!buffers.containsKey(key)) {
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            buffers.put(key, buffer);
            dirty.put(key, true);
        }

        if (Boolean.TRUE.equals(dirty.get(key)) || key.level == Level.LIVE) {
            VertexBuffer buffer = buffers.get(key);
            buffer.bind();
            buffer.upload(renderBuffer(key));
            VertexBuffer.unbind();
            dirty.put(key, false);
        }

        return buffers.get(key);
    }

    private static BufferKey genKey(Level level) {
        long scale = Math.max(1L, Math.round(AstroSpyglassManager.getZoom()));
        long bonus = (long) AstroSpyglassManager.getMagnitudeBonus();
        Vector3i region = null;
        if (AstroSpyglassManager.isInUse() && AstroSkyEngine.CONFIG.enableSpyglassCulling && AstroSkyRenderer.getEquatorialMatrix() != null) {
            Vector3f dir = AstroSpyglassManager.getEquatorialDirection().normalize((float) scale);
            region = new Vector3i((int) dir.x, (int) dir.y, (int) dir.z);
        }
        return new BufferKey(level, region, scale, bonus);
    }

    public static VertexBuffer getBuffer(Level level) {
        return getBuffer(genKey(level));
    }

    private static void draw(VertexBuffer buffer, Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderInstance program) {
        buffer.bind();
        buffer.drawWithShader(viewMatrix, projectionMatrix, program);
        VertexBuffer.unbind();
    }

    public static void drawAll(Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderInstance program) {
        if (!AstroSkyEngine.isInitialized() || viewMatrix == null || program == null) {
            return;
        }
        VertexBuffer staticBuffer = getBuffer(Level.STATIC);
        VertexBuffer dynamicBuffer = getBuffer(Level.DYNAMIC);
        VertexBuffer liveBuffer = getBuffer(Level.LIVE);
        AstroRenderUtils.useBlend(true, false);
        if (AstroSkyEngine.CONFIG.enableRealStars) {
            draw(staticBuffer, viewMatrix, projectionMatrix, program);
        }
        draw(dynamicBuffer, viewMatrix, projectionMatrix, program);
        AstroRenderUtils.useBlend(false, false);
        draw(liveBuffer, viewMatrix, projectionMatrix, program);
    }

    public static void updateBuffers(Level level) {
        for (BufferKey key : dirty.keySet()) {
            if (key.level == level) {
                dirty.put(key, true);
            }
        }
    }

    private record BufferKey(Level level, Vector3i region, long scale, long bonus) {
    }

    public enum Level {
        LIVE,
        DYNAMIC,
        STATIC
    }

    public record Star(Vector3f pos, Vector3f color, float magn, float radius) {
    }
}
