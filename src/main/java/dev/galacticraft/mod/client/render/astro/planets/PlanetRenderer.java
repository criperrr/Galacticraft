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

package dev.galacticraft.mod.client.render.astro.planets;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;

public class PlanetRenderer {
    private static final float DEPTH = 100.0F;
    private static final ResourceLocation SHADOW_TEXTURE = Constant.id("textures/astro/shadow.png");
    private static final ArrayList<Render> RENDERS = new ArrayList<>();
    private static RenderState sunRenderState = null;
    private static RenderState moonRenderState = null;
    private static Matrix4f matrix;
    private static double loadTime = 0.0;
    private static double renderTime = 0.0;

    public static void setSunRenderState(int texture, float[] color) {
        sunRenderState = new RenderState(texture, color.clone());
    }

    public static void setMoonRenderState(int texture, float[] color) {
        moonRenderState = new RenderState(texture, color.clone());
    }

    public static void init() {
        loadTime = PlanetManager.getPlanetTime();
        renderTime = loadTime;
        RENDERS.clear();
    }

    private static double getElapsed() {
        return renderTime - loadTime;
    }

    public static void compile() {
        RENDERS.sort(Comparator.comparingDouble(Render::getDistance));
    }

    public static Render genRender(Vector3d position, Vector3d sunPosition, double magnitude, Vector3f color) {
        Render render = new Render(position, sunPosition, magnitude, color);
        if (Double.isFinite(magnitude)) {
            RENDERS.add(render);
        }
        return render;
    }

    public static void renderAll(Matrix4f equatorialMatrix) {
        matrix = equatorialMatrix;
        AstroLightingManager.reset();
        AstroStarRenderer.initLiveStars();
        if (AstroSkyEngine.CONFIG.updateInterpolation) {
            renderTime = PlanetManager.getPlanetTime();
        }

        for (Render render : RENDERS) {
            if (AstroSkyEngine.CONFIG.enableEclipses) {
                render.calcOccultations();
            }
            addLight(render);
        }

        for (Render render : RENDERS) {
            AstroStarRenderer.addLiveStar(render.getPosition(), render.getMagnitude(), getRadiusAngle(render), render.color);
            if (render.getDisk() != null) {
                renderDisk(render, render.getDisk());
            }
            for (Tail tail : render.getTails()) {
                renderTail(render, tail);
            }
        }

        RenderSystem.defaultBlendFunc();
    }

    private static double getRadiusAngle(double radius, double distance) {
        return (180.0 / Math.PI) * (radius / distance);
    }

    private static double getRadiusAngle(Render render) {
        return render.disk == null ? 0.0 : getRadiusAngle(render.disk.diameter / 2.0, render.getPosition().length());
    }

    private static void addLight(Render render) {
        AstroLightingManager.addLight(genMatrix(render.getPosition()), render.getMagnitude(), 0.0);
    }

    private static void renderDisk(Render render, Disk disk) {
        if (disk.model == null) {
            return;
        }
        float zoom = (float) AstroSpyglassManager.getZoom();
        double bonus = AstroSpyglassManager.getMagnitudeBonus();
        float diskRadius = (float) disk.diameter / 2.0F / (float) render.getDistance();
        if ((double) (diskRadius * zoom) < (Math.PI / 180.0) * AstroStarRenderer.getResolution()) {
            return;
        }

        Matrix4f direction = genMatrix(render.getPosition());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        float total = (float) Math.pow((double) AstroStarRenderer.options.brightnessPower, -render.getMagnitude() - bonus);
        float lradius = (float) (Math.PI / 180.0) * AstroStarRenderer.options.baseRadius * Mth.sqrt(total) / zoom;
        float brightness = 0.5F * (float) Math.cbrt((double) (lradius / diskRadius)) * AstroLightingManager.getDiskVisibility();
        if (ClientTelescopeController.isActive()) {
            // Enhanced planetary albedo contrast & surface detail visibility under high-magnification observatory optics
            brightness *= 1.45F;
        }
        brightness /= brightness + 1.0F;
        Vector3f color = new Vector3f(render.color).mul(brightness);
        float inShadow = (float) render.getInShadow();
        if (inShadow > 0.0F) {
            color.mul(1.0F - 0.5F * inShadow, 1.0F - 0.75F * inShadow, 1.0F - 0.875F * inShadow);
        }

        for (Ring ring : render.getRings()) {
            if (ring.model != null) {
                ring.model.useColor(color);
                renderRingBottom(ring, direction, diskRadius);
            }
        }

        long renderCount = disk.model.useColor(color);
        switch (disk.model.getRenderer()) {
            case SUN -> {
                if (AstroSkyEngine.CONFIG.vanillaSun) {
                    return;
                }
                if (AstroSkyEngine.CONFIG.enableSunspots && AstroSpyglassManager.isInUse()) {
                    new SunspotRenderer(render.color.hashCode()).render(direction, diskRadius);
                }
                renderDiskSun(disk, direction, diskRadius);
                renderDiskOcclusion(direction, diskRadius);
            }
            case MOON -> {
                if (AstroSkyEngine.CONFIG.vanillaMoon) {
                    return;
                }
                renderDiskMoon(render, disk, direction, diskRadius);
                renderDiskOcclusion(direction, diskRadius);
                if (inShadow > 0.0F) {
                    renderShadow(render, genMatrix(render.sunPosition));
                }
            }
            case MAIN -> {
                renderDiskGlare(disk, direction, diskRadius);
                if (disk.model.hasPhases()) {
                    renderDiskPhase(render, disk, direction, diskRadius);
                }
                for (int i = 0; (long) i < renderCount; i++) {
                    renderDiskTexture(disk, direction, diskRadius);
                }
            }
        }

        for (Ring ring : render.getRings()) {
            if (ring.model != null) {
                ring.model.useColor(color);
                renderRingTop(ring, direction, diskRadius);
            }
        }
    }

    private static void renderDiskGlare(Disk disk, Matrix4f matrix, float diskRadius) {
        disk.model.useGlare();
        AstroRenderUtils.useBlend(true, false);
        new AstroRenderUtils.BufferHelper(4.0F * diskRadius).texture().draw(matrix);
    }

    private static void renderDiskOcclusion(Matrix4f matrix, float diskRadius) {
        AstroRenderUtils.useBlend(false, true, false);
        new AstroRenderUtils.BufferHelper(diskRadius).texture().draw(matrix);
    }

    private static void renderShadow(Render render, Matrix4f matrix) {
        double length = 0.0094 / (double) AstroSkyEngine.CONFIG.diameterScale;
        double dist = render.getDistance();
        if (dist > length) {
            return;
        }
        double localDiameter = 8.52E-5 * (length - dist) / length;
        float diskRadius = (float) (localDiameter / 2.0 / dist);
        RenderSystem.setShaderTexture(0, SHADOW_TEXTURE);
        RenderSystem.blendEquation(32779);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE_MINUS_DST_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE
        );
        new AstroRenderUtils.BufferHelper(diskRadius / 2.0F).texture().draw(matrix);
        RenderSystem.blendEquation(32774);
    }

    private static void renderDiskPhase(Render render, Disk disk, Matrix4f matrix, float diskRadius) {
        disk.model.usePhases();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.DST_ALPHA,
                GlStateManager.DestFactor.ZERO
        );
        double phase = render.getPhase();
        long phaseIndex = Math.round(Math.abs(phase) / 45.0);
        phaseIndex = phase >= 0.0 ? phaseIndex : (8L - phaseIndex) % 8L;
        float ustart = 0.125F * (float) phaseIndex;
        float uend = ustart + 0.125F;
        new AstroRenderUtils.BufferHelper(diskRadius).texture(ustart, 0.0F, uend, 1.0F).draw(matrix);
    }

    private static void renderDiskTexture(Disk disk, Matrix4f matrix, float diskRadius) {
        disk.model.useTexture();
        AstroRenderUtils.useBlend(true, true);
        new AstroRenderUtils.BufferHelper(diskRadius).texture().draw(matrix);
    }

    private static void renderDiskSun(Disk disk, Matrix4f matrix, float diskRadius) {
        disk.model.useTexture();
        AstroRenderUtils.useBlend(true, false);
        if (sunRenderState != null) {
            sunRenderState.use();
        }
        new AstroRenderUtils.BufferHelper(4.0F * diskRadius).texture().draw(matrix);
    }

    private static void renderDiskMoon(Render render, Disk disk, Matrix4f matrix, float diskRadius) {
        disk.model.useTexture();
        double phase = render.getPhase();
        long phaseIndex = Math.round(Math.abs(phase) / 45.0);
        phaseIndex = phase >= 0.0 ? phaseIndex : (8L - phaseIndex) % 8L;
        float u0 = 0.25F * (float) (phaseIndex % 4L);
        float v0 = 0.5F * (float) (phaseIndex / 4L);
        float u1 = u0 + 0.25F;
        float v1 = v0 + 0.5F;
        AstroRenderUtils.useBlend(true, false);
        if (moonRenderState != null) {
            moonRenderState.use();
        }
        new AstroRenderUtils.BufferHelper(4.0F * diskRadius).texture(u0, v0, u1, v1).draw(matrix);
    }

    private static void renderTail(Render render, Tail tail) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        float brightness = 0.5F * AstroLightingManager.getDiskVisibility();
        Vector3f color = new Vector3f(tail.color).mul(brightness);
        RenderSystem.setShaderColor(color.x, color.y, color.z, 1.0F);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

        Vector3d startPos = new Vector3d(render.getPosition());
        Vector3d frontPos = new Vector3d(tail.frontOffset).add(startPos);
        Vector3d rearPos = new Vector3d(tail.rearOffset).add(startPos);
        Matrix4f start = genMatrix(startPos);
        Matrix4f front = genMatrix(frontPos);
        Matrix4f rear = genMatrix(rearPos);

        BufferBuilder bb = AstroRenderUtils.setupBuffer(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        bb.addVertex(start, 0.0F, -DEPTH, 0.0F).setColor(-1);
        bb.addVertex(front, 0.0F, -DEPTH, 0.0F).setColor(0x00FFFFFF);
        bb.addVertex(rear, 0.0F, -DEPTH, 0.0F).setColor(0);
        bb.addVertex(rear, 0.0F, -DEPTH, 0.0F).setColor(0);
        bb.addVertex(front, 0.0F, -DEPTH, 0.0F).setColor(0x00FFFFFF);
        bb.addVertex(start, 0.0F, -DEPTH, 0.0F).setColor(-1);
        BufferUploader.drawWithShader(bb.buildOrThrow());

        bb = AstroRenderUtils.setupBuffer(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        bb.addVertex(start, 0.0F, -DEPTH, 0.0F).setColor(-1);
        bb.addVertex(front, 0.0F, -DEPTH, 0.0F).setColor(0);
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    private static void renderRingTop(Ring ring, Matrix4f matrix, float diskRadius) {
        ring.model.useTexture();
        AstroRenderUtils.useBlend(true, false);
        new AstroRenderUtils.BufferHelper(-diskRadius * ring.radius * ring.height, -diskRadius * ring.radius, 0.0F, diskRadius * ring.radius)
                .texture(0.0F, 0.0F, 1.0F, 0.5F)
                .draw(matrix);
    }

    private static void renderRingBottom(Ring ring, Matrix4f matrix, float diskRadius) {
        ring.model.useTexture();
        AstroRenderUtils.useBlend(true, false);
        new AstroRenderUtils.BufferHelper(0.0F, -diskRadius * ring.radius, diskRadius * ring.radius * ring.height, diskRadius * ring.radius)
                .texture(0.0F, 0.5F, 1.0F, 1.0F)
                .draw(matrix);
    }

    private static Matrix4f genMatrix(Vector3d viewPos) {
        Vector3d angPos = getAngularPosition(viewPos);
        return genMatrix(angPos.x, angPos.y);
    }

    private static Vector3d getAngularPosition(Vector3d direction) {
        direction = new Vector3d(direction).negate();
        direction.rotateX(Math.toRadians(-23.45F));
        double rightAscension = AstroMath.toDegrees(direction.angleSigned(new Vector3d(1.0, 0.0, 0.0), new Vector3d(0.0, -1.0, 0.0)));
        double declination = -AstroMath.atan2(direction.y, new Vector3d(direction.x, 0.0, direction.z).length());
        double distance = direction.length();
        return new Vector3d(rightAscension, declination, distance);
    }

    private static Matrix4f genMatrix(double rightAscension, double declination) {
        Matrix4f mat = new Matrix4f(matrix);
        mat.rotate(Axis.XN.rotationDegrees((float) (180.0 + rightAscension)));
        mat.rotate(Axis.ZN.rotationDegrees((float) declination));
        return mat;
    }

    public record Disk(ModelManager.Model model, double diameter) {
    }

    public record Motion(double elapsed, Vector3d velocity, double radialVelocity, double magnChange) {
    }

    public static class Render {
        private final Vector3d position;
        private final Vector3d sunPosition;
        private final double distance;
        private Motion motion;
        private final double magnitude;
        private double occulted;
        private double inShadow;
        private final Vector3f color;
        private Disk disk = null;
        private final ArrayList<Tail> tails = new ArrayList<>();
        private final ArrayList<Ring> rings = new ArrayList<>();

        public Render(Vector3d viewPos, Vector3d sunPosition, double magn, Vector3f color) {
            this.position = viewPos;
            this.sunPosition = sunPosition;
            this.distance = viewPos.length();
            this.magnitude = magn;
            this.color = color;
        }

        public Render setMotion(double startTime, double finishTime, Vector3d finishPosition) {
            if (Double.compare(finishTime, startTime) == 0) {
                return this;
            }
            Vector3d velocity = new Vector3d(finishPosition).sub(this.position);
            double radialVelocity = finishPosition.length() - this.position.length();
            double magnChange = 2.5 * Math.log10(finishPosition.lengthSquared() / this.position.lengthSquared());
            double elapsed = finishTime - startTime;
            this.motion = new Motion(elapsed, velocity.div(elapsed), radialVelocity / elapsed, magnChange / elapsed);
            return this;
        }

        public Render setDisk(ModelManager.Model model, double diameter) {
            this.disk = new Disk(model, diameter);
            return this;
        }

        public Render addTail(Vector3d frontOffset, Vector3d rearOffset, Vector3f color) {
            this.tails.add(new Tail(frontOffset, rearOffset, color));
            return this;
        }

        public Render addRing(double radius, double inclination, Vector3f color) {
            ModelManager.Model model = ModelManager.getModel("ring");
            this.rings.add(new Ring(model, (float) radius, (float) Math.sin(inclination / 180.0 * Math.PI), color));
            return this;
        }

        public Vector3d getPosition() {
            if (this.motion == null) {
                return this.position;
            }
            double newDistance = this.distance + this.motion.radialVelocity * PlanetRenderer.getElapsed();
            return new Vector3d(this.motion.velocity).mul(PlanetRenderer.getElapsed()).add(this.position).normalize(newDistance);
        }

        public double getDistance() {
            return this.motion == null ? this.distance : Math.abs(this.distance + this.motion.radialVelocity * PlanetRenderer.getElapsed());
        }

        public double getPhase() {
            double phase = Math.toDegrees(this.position.angle(this.sunPosition));
            return phase * (this.position.angleSigned(this.sunPosition, new Vector3d(0.0, -1.0, 0.0)) > 0.0 ? 1.0 : -1.0);
        }

        private double calcOccultation(Vector3d infrontPos, double infrontRadius) {
            double angle = (180.0 / Math.PI) * this.getPosition().angle(infrontPos);
            double behindRadius = PlanetRenderer.getRadiusAngle(this);
            if (this.disk == null || behindRadius <= 0.0) {
                return angle < infrontRadius ? 1.0 : 0.0;
            }
            double rightBound = Math.min(behindRadius, angle + infrontRadius);
            double leftBound = Math.max(-behindRadius, angle - infrontRadius);
            double overlap = Math.max(0.0, rightBound - leftBound);
            return overlap / behindRadius / 2.0;
        }

        private double calcOccultation(Render infront) {
            if (infront.disk == null) {
                return 0.0;
            }
            Vector3d pos = infront.getPosition();
            double infrontRadius = PlanetRenderer.getRadiusAngle(infront);
            return this.calcOccultation(pos, infrontRadius);
        }

        private double calcShadow() {
            double length = 0.0094 / (double) AstroSkyEngine.CONFIG.diameterScale;
            double dist = this.getDistance();
            if (dist > length || dist <= 1.0E-12) {
                return 0.0;
            }
            double localDiameter = 8.52E-5 * (length - dist) / length;
            double inUmbra = this.calcOccultation(this.sunPosition, 28.647888 * localDiameter / dist / 2.0);
            double inHalfUmbra = this.calcOccultation(this.sunPosition, 0.0024408000732421874 / dist / 2.0);
            return 0.5 * inHalfUmbra + 0.5 * inUmbra;
        }

        public void calcOccultations() {
            double sum = 0.0;
            for (Render infront : PlanetRenderer.RENDERS) {
                if (infront == this || sum >= 1.0) {
                    break;
                }
                sum += this.calcOccultation(infront);
            }
            this.occulted = Math.min(1.0, sum);
            this.inShadow = this.calcShadow();
        }

        public double getOccultationMagnitude() {
            double occulationMagn = Math.min(50.0, -2.5 * AstroSkyEngine.CONFIG.lightingOptions.occultationPower * Math.log10(Math.max(1.0E-12, 1.0 - this.occulted)));
            double umbraMagn = Math.min(10.0, -2.5 * AstroSkyEngine.CONFIG.lightingOptions.occultationPower * Math.log10(Math.max(1.0E-12, 1.0 - this.inShadow)));
            return occulationMagn + umbraMagn;
        }

        public double getMagnitude() {
            return this.motion == null
                    ? this.magnitude + this.getOccultationMagnitude()
                    : this.magnitude + this.motion.magnChange * PlanetRenderer.getElapsed() + this.getOccultationMagnitude();
        }

        public double getInShadow() {
            return this.inShadow;
        }

        public Disk getDisk() {
            return this.disk;
        }

        public Iterable<Tail> getTails() {
            return this.tails;
        }

        public Iterable<Ring> getRings() {
            return this.rings;
        }
    }

    public record RenderState(int texture, float[] color) {
        public void use() {
            RenderSystem.setShaderTexture(0, this.texture);
            float[] oldColor = RenderSystem.getShaderColor();
            RenderSystem.setShaderColor(
                    oldColor[0] * this.color[0],
                    oldColor[1] * this.color[1],
                    oldColor[2] * this.color[2],
                    oldColor[3] * this.color[3]
            );
        }
    }

    public record Ring(ModelManager.Model model, float radius, float height, Vector3f color) {
    }

    public record Tail(Vector3d frontOffset, Vector3d rearOffset, Vector3f color) {
    }
}
