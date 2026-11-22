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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Baked, allocation-free runtime representation of a binary {@code .gcrm} modular rocket part model.
 *
 * <p>Geometry is stored in flat primitive {@code float[]} arrays (23 floats per quad:
 * four vertices of {@code (x, y, z, u, v)} followed by unit face normal {@code (nx, ny, nz)}).
 * Bone transforms chain hierarchically using parent-relative origins and Blockbench ZYX Euler angles.
 */
@Environment(EnvType.CLIENT)
public final class RocketPartRuntimeModel {
    /**
     * Packed quad stride: 4 vertices * (x, y, z, u, v) + (nx, ny, nz) = 23 floats.
     */
    public static final int FLOATS_PER_QUAD = 23;
    public static final float[] EMPTY_FLOATS = new float[0];
    private static final int[] EMPTY_INTS = new int[0];

    private final ResourceLocation location;
    private final float duration;
    private final float minX;
    private final float minY;
    private final float minZ;
    private final float maxX;
    private final float maxY;
    private final float maxZ;
    private final Bone[] bones;
    private final int[] rootBoneIndices;
    private final int[][] childBoneIndices;
    private final boolean hasOpaqueQuads;
    private final boolean hasTranslucentQuads;
    private final boolean hasAnimations;
    private final boolean hasLodDetailBones;
    private final int totalQuadCount;

    public RocketPartRuntimeModel(
            ResourceLocation location,
            float duration,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            List<Bone> boneList
    ) {
        this.location = location;
        this.duration = duration > 0.0F ? duration : 30.0F;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;

        int count = boneList.size();
        this.bones = boneList.toArray(new Bone[count]);

        int[] parentIndices = new int[count];
        for (int i = 0; i < count; i++) {
            int p = this.bones[i].parentIndex;
            parentIndices[i] = (p >= 0 && p < count && p != i) ? p : -1;
        }

        // Break any accidental cycles defensively so rendering can never infinite-loop.
        for (int i = 0; i < count; i++) {
            int cursor = parentIndices[i];
            int steps = 0;
            while (cursor != -1 && steps <= count) {
                if (cursor == i) {
                    parentIndices[i] = -1;
                    break;
                }
                cursor = parentIndices[cursor];
                steps++;
            }
            if (steps > count) {
                parentIndices[i] = -1;
            }
        }

        List<Integer> roots = new ArrayList<>();
        List<List<Integer>> childrenLists = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            childrenLists.add(new ArrayList<>());
        }

        boolean opaque = false;
        boolean translucent = false;
        boolean animated = false;
        boolean lod = false;
        int quads = 0;

        for (int i = 0; i < count; i++) {
            int parentIdx = parentIndices[i];
            if (parentIdx == -1) {
                roots.add(i);
            } else {
                childrenLists.get(parentIdx).add(i);
            }
            Bone bone = this.bones[i];
            quads += bone.opaqueQuadCount() + bone.translucentQuadCount();
            if (bone.opaqueQuads.length > 0) {
                opaque = true;
            }
            if (bone.translucentQuads.length > 0) {
                translucent = true;
            }
            if (bone.hasAnimPos || bone.hasAnimRot) {
                animated = true;
            }
            if (bone.lodDetail) {
                lod = true;
            }
        }

        this.rootBoneIndices = toIntArray(roots);
        this.childBoneIndices = new int[count][];
        for (int i = 0; i < count; i++) {
            this.childBoneIndices[i] = toIntArray(childrenLists.get(i));
        }
        this.hasOpaqueQuads = opaque;
        this.hasTranslucentQuads = translucent;
        this.hasAnimations = animated;
        this.hasLodDetailBones = lod;
        this.totalQuadCount = quads;
    }

    private static int[] toIntArray(List<Integer> list) {
        if (list.isEmpty()) {
            return EMPTY_INTS;
        }
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public ResourceLocation location() {
        return this.location;
    }

    public float duration() {
        return this.duration;
    }

    public float minX() {
        return this.minX;
    }

    public float minY() {
        return this.minY;
    }

    public float minZ() {
        return this.minZ;
    }

    public float maxX() {
        return this.maxX;
    }

    public float maxY() {
        return this.maxY;
    }

    public float maxZ() {
        return this.maxZ;
    }

    /**
     * Returns a copy of {@code [minX, minY, minZ, maxX, maxY, maxZ]} in Minecraft blocks.
     */
    public float[] boundsMinMax() {
        return new float[]{this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ};
    }

    public float boundsSizeX() {
        return Math.max(0.0F, this.maxX - this.minX);
    }

    public float boundsSizeY() {
        return Math.max(0.0F, this.maxY - this.minY);
    }

    public float boundsSizeZ() {
        return Math.max(0.0F, this.maxZ - this.minZ);
    }

    public float boundsCenterX() {
        return 0.5F * (this.minX + this.maxX);
    }

    public float boundsCenterY() {
        return 0.5F * (this.minY + this.maxY);
    }

    public float boundsCenterZ() {
        return 0.5F * (this.minZ + this.maxZ);
    }

    public Bone[] bones() {
        return this.bones;
    }

    public int[] rootBoneIndices() {
        return this.rootBoneIndices;
    }

    public int[] childIndices(int boneIndex) {
        return this.childBoneIndices[boneIndex];
    }

    public boolean hasOpaqueQuads() {
        return this.hasOpaqueQuads;
    }

    public boolean hasTranslucentQuads() {
        return this.hasTranslucentQuads;
    }

    public boolean hasAnimations() {
        return this.hasAnimations;
    }

    public boolean hasLodDetailBones() {
        return this.hasLodDetailBones;
    }

    public int totalQuadCount() {
        return this.totalQuadCount;
    }

    /**
     * Single bone node inside a {@link RocketPartRuntimeModel}.
     */
    public static final class Bone {
        public final String name;
        public final int parentIndex;
        public final boolean lodDetail;
        public final float originX;
        public final float originY;
        public final float originZ;
        public final float rotX;
        public final float rotY;
        public final float rotZ;
        public final float[] opaqueQuads;
        public final float[] translucentQuads;
        public final float[] posTimes;
        public final float[] posValues;
        public final float[] rotTimes;
        public final float[] rotValues;
        public final boolean hasAnimPos;
        public final boolean hasAnimRot;
        public final boolean hasOriginOffset;
        public final boolean hasRestRotation;

        public Bone(
                String name,
                int parentIndex,
                boolean lodDetail,
                float originX,
                float originY,
                float originZ,
                float rotX,
                float rotY,
                float rotZ,
                float[] opaqueQuads,
                float[] translucentQuads,
                float[] posTimes,
                float[] posValues,
                float[] rotTimes,
                float[] rotValues
        ) {
            this.name = name;
            this.parentIndex = parentIndex;
            this.lodDetail = lodDetail;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.opaqueQuads = opaqueQuads != null ? opaqueQuads : EMPTY_FLOATS;
            this.translucentQuads = translucentQuads != null ? translucentQuads : EMPTY_FLOATS;
            this.posTimes = posTimes != null ? posTimes : EMPTY_FLOATS;
            this.posValues = posValues != null ? posValues : EMPTY_FLOATS;
            this.rotTimes = rotTimes != null ? rotTimes : EMPTY_FLOATS;
            this.rotValues = rotValues != null ? rotValues : EMPTY_FLOATS;
            this.hasAnimPos = this.posTimes.length > 0 && this.posValues.length >= this.posTimes.length * 3;
            this.hasAnimRot = this.rotTimes.length > 0 && this.rotValues.length >= this.rotTimes.length * 3;
            this.hasOriginOffset = originX != 0.0F || originY != 0.0F || originZ != 0.0F;
            this.hasRestRotation = rotX != 0.0F || rotY != 0.0F || rotZ != 0.0F;
        }

        public int opaqueQuadCount() {
            return this.opaqueQuads.length / FLOATS_PER_QUAD;
        }

        public int translucentQuadCount() {
            return this.translucentQuads.length / FLOATS_PER_QUAD;
        }

        /**
         * Samples this bone's animated position offset (in blocks) into {@code out3} without heap allocation.
         */
        public void samplePosition(float time, float duration, float[] out3) {
            if (!this.hasAnimPos) {
                out3[0] = 0.0F;
                out3[1] = 0.0F;
                out3[2] = 0.0F;
                return;
            }
            sampleTrack(this.posTimes, this.posValues, time, duration, out3);
        }

        /**
         * Samples this bone's animated rotation offset (in degrees) into {@code out3} without heap allocation.
         */
        public void sampleRotation(float time, float duration, float[] out3) {
            if (!this.hasAnimRot) {
                out3[0] = 0.0F;
                out3[1] = 0.0F;
                out3[2] = 0.0F;
                return;
            }
            sampleTrack(this.rotTimes, this.rotValues, time, duration, out3);
        }

        /**
         * Evaluates a sorted keyframe track using smooth Catmull-Rom interpolation (matching Blockbench 5.0
         * {@code smooth} curves with cyclic closing-seam awareness) for 3+ keyframes, or linear interpolation
         * for 2 keyframes. Performs zero heap allocations.
         */
        public static void sampleTrack(float[] times, float[] values, float time, float duration, float[] out3) {
            int n = times.length;
            if (n == 0) {
                out3[0] = 0.0F;
                out3[1] = 0.0F;
                out3[2] = 0.0F;
                return;
            }
            if (n == 1) {
                out3[0] = values[0];
                out3[1] = values[1];
                out3[2] = values[2];
                return;
            }

            float t = time;
            if (duration > 0.0F) {
                t = time % duration;
                if (t < 0.0F) {
                    t += duration;
                }
            }

            if (t <= times[0]) {
                out3[0] = values[0];
                out3[1] = values[1];
                out3[2] = values[2];
                return;
            }
            int last = n - 1;
            if (t >= times[last]) {
                int base = last * 3;
                out3[0] = values[base];
                out3[1] = values[base + 1];
                out3[2] = values[base + 2];
                return;
            }

            int lo = 0;
            int hi = last - 1;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (times[mid] <= t) {
                    lo = mid;
                } else {
                    hi = mid - 1;
                }
            }
            int i = lo;
            float span = times[i + 1] - times[i];
            float u = span > 1.0e-6F ? (t - times[i]) / span : 0.0F;

            int base1 = i * 3;
            int base2 = (i + 1) * 3;

            if (n == 2) {
                out3[0] = values[base1] + (values[base2] - values[base1]) * u;
                out3[1] = values[base1 + 1] + (values[base2 + 1] - values[base1 + 1]) * u;
                out3[2] = values[base1 + 2] + (values[base2 + 2] - values[base1 + 2]) * u;
                return;
            }

            int lastBase = last * 3;
            boolean closingSeam = Math.abs(times[0]) < 1.0e-4F
                    && Math.abs(values[0] - values[lastBase]) < 0.05F
                    && Math.abs(values[1] - values[lastBase + 1]) < 0.05F
                    && Math.abs(values[2] - values[lastBase + 2]) < 0.05F;

            int idxPrev = closingSeam ? (i == 0 ? n - 2 : i - 1) : Math.max(0, i - 1);
            int idxNext = closingSeam ? (i + 1 == last ? 1 : i + 2) : Math.min(last, i + 2);

            int base0 = idxPrev * 3;
            int base3 = idxNext * 3;
            float u2 = u * u;
            float u3 = u2 * u;

            for (int k = 0; k < 3; k++) {
                float p0 = values[base0 + k];
                float p1 = values[base1 + k];
                float p2 = values[base2 + k];
                float p3 = values[base3 + k];
                out3[k] = 0.5F * (
                        2.0F * p1
                                + (-p0 + p2) * u
                                + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * u2
                                + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * u3
                );
            }
        }
    }
}
