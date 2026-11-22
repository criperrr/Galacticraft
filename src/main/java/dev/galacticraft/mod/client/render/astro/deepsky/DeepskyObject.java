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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroColorUtils;
import dev.galacticraft.mod.client.render.astro.AstroRenderUtils;
import dev.galacticraft.mod.client.render.astro.AstroStarRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Iterator;

public abstract class DeepskyObject implements Iterable<AstroStarRenderer.Star> {
    public String id;
    public String type;
    public double magnitude;
    @JsonAdapter(DSOLoader.RADeserializer.class)
    public double right_ascension;
    @JsonAdapter(DSOLoader.AngleDeserializer.class)
    public double declination;
    @JsonAdapter(DSOLoader.AngleDeserializer.class)
    public double orientation;
    public double populatefrom = 0.0;
    @JsonAdapter(ResourceLocation.Serializer.class)
    public ResourceLocation texture = null;
    public Vector3f starPos;

    public boolean process(ResourceManager manager) {
        this.starPos = AstroStarRenderer.getPosition(this.right_ascension, this.declination);
        if (this.populatefrom == 0.0) {
            this.populatefrom = 12.0;
        }
        if (this.texture == null) {
            this.texture = this.getDefaultTexture();
        }
        return true;
    }

    private static Matrix4f genMatrix(Matrix4f equatorial, double rightAscension, double declination, double orientation) {
        Matrix4f mat = new Matrix4f(equatorial);
        mat.rotate(Axis.XP.rotationDegrees((float) (180.0 - rightAscension)));
        mat.rotate(Axis.ZN.rotationDegrees((float) declination));
        mat.rotate(Axis.YN.rotationDegrees((float) orientation));
        return mat;
    }

    protected Matrix4f genMatrix(Matrix4f equatorial) {
        return genMatrix(equatorial, this.right_ascension, this.declination, this.orientation);
    }

    protected void setupRender(double scale, double bonus) {
        AstroRenderUtils.useBlend(true, false);
        AstroRenderUtils.setShaderTextureWithName(this.texture);
        float b = DeepskyManager.useTextureBrightness(this.magnitude + bonus, this.getArea() * scale * scale);
        Vector3f color = new Vector3f(this.getColor()).mul(AstroStarRenderer.baseStarColor).mul(b);
        RenderSystem.setShaderColor(color.x, color.y, color.z, 1.0F);
    }

    protected ResourceLocation getDefaultTexture() {
        return Constant.id("astro/placeholder");
    }

    public abstract double getArea();

    public abstract void render(Matrix4f matrix, float scale, double bonus);

    public Vector3f getColor() {
        return AstroColorUtils.WHITE;
    }

    public AstroStarRenderer.Star renderAsStar() {
        return new AstroStarRenderer.Star(this.starPos, this.getColor(), (float) this.magnitude, 0.0F);
    }

    @Override
    public Iterator<AstroStarRenderer.Star> iterator() {
        return Collections.emptyIterator();
    }
}
