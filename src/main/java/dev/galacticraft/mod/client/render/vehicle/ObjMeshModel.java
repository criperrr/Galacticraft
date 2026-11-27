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

package dev.galacticraft.mod.client.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed Wavefront {@code .obj} 3D model split into named object groups ({@code o}/{@code g}),
 * mirroring Immersive Vehicles' sub-object animation architecture while emitting quads/triangles
 * cleanly through Minecraft 1.21.1's {@link VertexConsumer} (Sodium &amp; Iris safe).
 */
public final class ObjMeshModel {
    public record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
    }

    public record Quad(Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
    }

    public record Group(@NotNull String name, @NotNull List<Quad> quads) {
        public void render(PoseStack poseStack, VertexConsumer consumer, float r, float g, float b, float a, int light) {
            Matrix4f mat = poseStack.last().pose();
            PoseStack.Pose pose = poseStack.last();
            for (Quad q : this.quads) {
                emit(mat, pose, consumer, q.v0, r, g, b, a, light);
                emit(mat, pose, consumer, q.v1, r, g, b, a, light);
                emit(mat, pose, consumer, q.v2, r, g, b, a, light);
                emit(mat, pose, consumer, q.v3, r, g, b, a, light);
            }
        }

        private static void emit(Matrix4f mat, PoseStack.Pose pose, VertexConsumer c, Vertex v, float r, float g, float b, float a, int light) {
            c.addVertex(mat, v.x, v.y, v.z)
                    .setColor(r, g, b, a)
                    .setUv(v.u, v.v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, v.nx, v.ny, v.nz);
        }
    }

    private final Map<String, Group> groups;

    public ObjMeshModel(@NotNull Map<String, Group> groups) {
        this.groups = Collections.unmodifiableMap(new LinkedHashMap<>(groups));
    }

    public @NotNull Map<String, Group> groups() {
        return this.groups;
    }

    public @Nullable Group getGroup(@NotNull String name) {
        return this.groups.get(name);
    }

    public boolean isEmpty() {
        return this.groups.isEmpty();
    }
}
