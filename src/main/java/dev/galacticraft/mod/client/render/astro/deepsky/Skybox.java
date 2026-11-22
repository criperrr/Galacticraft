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

package dev.galacticraft.mod.client.render.astro.deepsky;

import com.google.gson.annotations.JsonAdapter;
import com.mojang.math.Axis;
import dev.galacticraft.mod.client.render.astro.AstroRenderUtils;
import dev.galacticraft.mod.client.render.astro.AstroSpyglassManager;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class Skybox extends DeepskyObject {
    public Textures textures;
    public Textures spyglass;

    @Override
    public double getArea() {
        return 64800.0;
    }

    @Override
    public void render(Matrix4f matrix, float scale, double bonus) {
        this.setupRender((double) scale, bonus);
        AstroRenderUtils.BufferHelper bufferHelper = new AstroRenderUtils.BufferHelper(1.0F, 1.0F).texture();
        Textures textureSet = AstroSpyglassManager.isInUse() && this.spyglass != null ? this.spyglass : this.textures;
        Matrix4f base = this.genMatrix(matrix);

        AstroRenderUtils.setShaderTextureWithName(textureSet.pz);
        bufferHelper.draw(new Matrix4f(base));
        AstroRenderUtils.setShaderTextureWithName(textureSet.py);
        bufferHelper.draw(new Matrix4f(base).rotate(Axis.ZN.rotationDegrees(90.0F)));
        AstroRenderUtils.setShaderTextureWithName(textureSet.px);
        bufferHelper.draw(new Matrix4f(base).rotate(Axis.XP.rotationDegrees(90.0F)));
        AstroRenderUtils.setShaderTextureWithName(textureSet.nz);
        bufferHelper.draw(new Matrix4f(base).rotate(Axis.XP.rotationDegrees(180.0F)));
        AstroRenderUtils.setShaderTextureWithName(textureSet.ny);
        bufferHelper.draw(new Matrix4f(base).rotate(Axis.ZP.rotationDegrees(90.0F)));
        AstroRenderUtils.setShaderTextureWithName(textureSet.nx);
        bufferHelper.draw(new Matrix4f(base).rotate(Axis.XN.rotationDegrees(90.0F)));
    }

    public static class Textures {
        @JsonAdapter(ResourceLocation.Serializer.class)
        public ResourceLocation pz;
        @JsonAdapter(ResourceLocation.Serializer.class)
        public ResourceLocation nz;
        @JsonAdapter(ResourceLocation.Serializer.class)
        public ResourceLocation py;
        @JsonAdapter(ResourceLocation.Serializer.class)
        public ResourceLocation ny;
        @JsonAdapter(ResourceLocation.Serializer.class)
        public ResourceLocation px;
        @JsonAdapter(ResourceLocation.Serializer.class)
        public ResourceLocation nx;
    }
}
