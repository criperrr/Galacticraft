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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public class AstroRenderUtils {
    public static BufferBuilder setupBuffer(VertexFormat.Mode drawMode, VertexFormat format) {
        return Tesselator.getInstance().begin(drawMode, format);
    }

    public static double getStarBrightness() {
        Minecraft client = Minecraft.getInstance();
        return client.level != null ? (double) client.level.getStarBrightness(0.0F) : 1.0;
    }

    public static void useBlend(boolean obscurable, boolean opaque, boolean visible) {
        GlStateManager.SourceFactor colorSrc = visible
                ? (obscurable ? GlStateManager.SourceFactor.DST_ALPHA : GlStateManager.SourceFactor.SRC_ALPHA)
                : GlStateManager.SourceFactor.ZERO;
        GlStateManager.DestFactor colorDst = GlStateManager.DestFactor.ONE;
        GlStateManager.SourceFactor alphaSrc = GlStateManager.SourceFactor.ZERO;
        GlStateManager.DestFactor alphaDst = opaque ? GlStateManager.DestFactor.ZERO : GlStateManager.DestFactor.ONE;
        RenderSystem.blendFuncSeparate(colorSrc, colorDst, alphaSrc, alphaDst);
    }

    public static void useBlend(boolean obscurable, boolean opaque) {
        useBlend(obscurable, opaque, true);
    }

    public static Supplier<ShaderInstance> getPositionProgram() {
        return GameRenderer::getPositionShader;
    }

    public static Supplier<ShaderInstance> getPositionColorProgram() {
        return GameRenderer::getPositionColorShader;
    }

    public static Supplier<ShaderInstance> getPositionTexProgram() {
        return GameRenderer::getPositionTexShader;
    }

    public static Supplier<ShaderInstance> getPositionTexColorProgram() {
        return GameRenderer::getPositionTexColorShader;
    }

    public static void setShaderTexture(ResourceLocation texture) {
        RenderSystem.setShaderTexture(0, texture);
    }

    public static void setShaderTextureWithName(ResourceLocation textureName) {
        setShaderTexture(textureName.withPrefix("textures/").withSuffix(".png"));
    }

    public static void clearColorBuffer() {
        RenderSystem.clear(16384, Minecraft.ON_OSX);
    }

    public static class BufferHelper {
        private final float x0;
        private final float x1;
        private final float z0;
        private final float z1;
        private boolean useTexture = false;
        private float u0 = 0.0F;
        private float v0 = 0.0F;
        private float u1 = 1.0F;
        private float v1 = 1.0F;

        public BufferHelper(float x0, float z0, float x1, float z1) {
            this.x0 = x0;
            this.z0 = z0;
            this.x1 = x1;
            this.z1 = z1;
        }

        public BufferHelper(float rwidth, float rheight) {
            this(-rwidth, -rheight, rwidth, rheight);
        }

        public BufferHelper(float radius) {
            this(radius, radius);
        }

        public BufferHelper texture() {
            this.useTexture = true;
            return this;
        }

        public BufferHelper texture(float u0, float v0, float u1, float v1) {
            this.texture();
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            return this;
        }

        private void genVertices(BufferBuilder bb, Matrix4f matrix) {
            float depth = 100.0F;
            if (this.useTexture) {
                bb.addVertex(matrix, this.x0 * depth, -depth, this.z1 * depth).setUv(this.u0, this.v0);
                bb.addVertex(matrix, this.x1 * depth, -depth, this.z1 * depth).setUv(this.u0, this.v1);
                bb.addVertex(matrix, this.x1 * depth, -depth, this.z0 * depth).setUv(this.u1, this.v1);
                bb.addVertex(matrix, this.x0 * depth, -depth, this.z0 * depth).setUv(this.u1, this.v0);
            } else {
                bb.addVertex(matrix, this.x0 * depth, -depth, this.z1 * depth);
                bb.addVertex(matrix, this.x1 * depth, -depth, this.z1 * depth);
                bb.addVertex(matrix, this.x1 * depth, -depth, this.z0 * depth);
                bb.addVertex(matrix, this.x0 * depth, -depth, this.z0 * depth);
            }
        }

        public MeshData build(Matrix4f matrix) {
            VertexFormat format = this.useTexture ? DefaultVertexFormat.POSITION_TEX : DefaultVertexFormat.POSITION;
            BufferBuilder bb = setupBuffer(VertexFormat.Mode.QUADS, format);
            this.genVertices(bb, matrix);
            return bb.buildOrThrow();
        }

        public void draw(Matrix4f matrix) {
            BufferUploader.drawWithShader(this.build(matrix));
        }
    }
}
