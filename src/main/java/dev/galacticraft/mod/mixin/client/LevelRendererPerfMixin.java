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

package dev.galacticraft.mod.mixin.client;

import dev.galacticraft.mod.telemetry.GCPerformanceTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererPerfMixin {
    @Unique
    private static long gc$renderLevelStart = 0;
    @Unique
    private static long gc$renderSkyStart = 0;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void gc$renderLevelStart(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        gc$renderLevelStart = System.nanoTime();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void gc$renderLevelEnd(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (gc$renderLevelStart > 0) {
            long duration = System.nanoTime() - gc$renderLevelStart;
            GCPerformanceTracker.recordClientRender("level_frame", duration);
        }
    }

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void gc$renderSkyStart(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        gc$renderSkyStart = System.nanoTime();
    }

    @Inject(method = "renderSky", at = @At("RETURN"))
    private void gc$renderSkyEnd(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        if (gc$renderSkyStart > 0) {
            long duration = System.nanoTime() - gc$renderSkyStart;
            GCPerformanceTracker.recordClientRender("sky_render", duration);
        }
    }
}
