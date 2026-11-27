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
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;

/**
 * MTS/Immersive-Vehicles-style animation transform bound to a named {@code .obj} group.
 * Rotates or translates a sub-object around an exact pivot point based on a live vehicle variable.
 */
public record VehicleAnimationBinding(
        @NotNull String groupName,
        @NotNull Type type,
        float pivotX,
        float pivotY,
        float pivotZ,
        float axisX,
        float axisY,
        float axisZ,
        float scale
) {
    public enum Type {
        ROTATION,
        TRANSLATION
    }

    public void apply(@NotNull PoseStack poseStack, float variableValue) {
        float value = variableValue * this.scale;
        if (Math.abs(value) < 1.0E-4F) {
            return;
        }
        if (this.type == Type.TRANSLATION) {
            poseStack.translate(this.axisX * value, this.axisY * value, this.axisZ * value);
        } else {
            poseStack.translate(this.pivotX, this.pivotY, this.pivotZ);
            if (Math.abs(this.axisX) > 1.0E-4F) {
                poseStack.mulPose(Axis.XP.rotationDegrees(this.axisX * value));
            }
            if (Math.abs(this.axisY) > 1.0E-4F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(this.axisY * value));
            }
            if (Math.abs(this.axisZ) > 1.0E-4F) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(this.axisZ * value));
            }
            poseStack.translate(-this.pivotX, -this.pivotY, -this.pivotZ);
        }
    }
}
