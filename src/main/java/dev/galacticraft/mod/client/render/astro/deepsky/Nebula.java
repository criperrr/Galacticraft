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
import dev.galacticraft.mod.client.render.astro.AstroRenderUtils;
import org.joml.Matrix4f;

public class Nebula extends DeepskyObject {
    @JsonAdapter(DSOLoader.AngleDeserializer.class)
    public double width;
    @JsonAdapter(DSOLoader.AngleDeserializer.class)
    public double height;

    @Override
    public double getArea() {
        return this.width * this.height;
    }

    @Override
    public void render(Matrix4f matrix, float scale, double bonus) {
        this.setupRender((double) scale, bonus);
        new AstroRenderUtils.BufferHelper(
                (float) (Math.PI / 180.0) * (float) this.height / 2.0F,
                (float) (Math.PI / 180.0) * (float) this.width / 2.0F
        ).texture().draw(this.genMatrix(matrix));
    }
}
