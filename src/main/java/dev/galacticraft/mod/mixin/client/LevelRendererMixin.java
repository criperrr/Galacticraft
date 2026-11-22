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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroLightingManager;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroSkyRenderer;
import dev.galacticraft.mod.client.render.astro.AstroStarRenderer;
import dev.galacticraft.mod.client.render.astro.planets.PlanetRenderer;
import dev.galacticraft.mod.client.render.dimension.OverworldRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Nullable
    private ClientLevel level;

    @Unique
    private OverworldRenderer worldRenderer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void gc$setupRenderer(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        this.worldRenderer = new OverworldRenderer();
    }

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    public void gc$renderSky(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        AstroSkyEngine.ensureUpToDate();

        Player player = Minecraft.getInstance().player;
        if (player != null && player.level().dimension() == Level.OVERWORLD) {
            double playerY = player.getVehicle() != null ? player.getVehicle().getY() : player.getY();
            if (playerY > Constant.OVERWORLD_SKYPROVIDER_STARTHEIGHT) {
                this.worldRenderer.renderOverworldSky(player, matrix4f, projectionMatrix, tickDelta, camera, thickFog, fogCallback);
                ci.cancel();
            }
        }
    }

    @WrapOperation(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F", ordinal = 0)
    )
    private float gc$modifyAstroSunsetTime(ClientLevel instance, float partialTick, Operation<Float> original) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(instance) && AstroLightingManager.hasComputedLighting()) {
            return AstroLightingManager.getSunsetAltitude();
        }
        return original.call(instance, partialTick);
    }

    @Redirect(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 0)
    )
    private void gc$modifyAstroSunsetAzimuth(PoseStack poseStack, Quaternionf quaternion) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(this.level) && AstroLightingManager.hasComputedLighting()) {
            poseStack.mulPose(Axis.YN.rotation(AstroLightingManager.getSunsetAzimuth()));
        }
        poseStack.mulPose(quaternion);
    }

    @Redirect(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 1)
    )
    private void gc$setAstroSunsetBrightness(PoseStack poseStack, Quaternionf quaternion) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(this.level) && AstroLightingManager.hasComputedLighting()) {
            float brightness = Mth.sqrt(AstroLightingManager.getSkyBrightness());
            RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);
        } else {
            poseStack.mulPose(quaternion);
        }
    }

    @Redirect(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;last()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;", ordinal = 2)
    )
    private PoseStack.Pose gc$modifyAstroSkyOrientation(PoseStack matrices) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(this.level)) {
            matrices.pushPose();
            matrices.mulPose(Axis.XN.rotationDegrees(this.level.getTimeOfDay(0.0F) * 360.0F));
            matrices.mulPose(Axis.YN.rotationDegrees(-90.0F));
            AstroSkyRenderer.calcPositionMatrices(matrices);
            matrices.popPose();
            PoseStack.Pose eq = AstroSkyRenderer.getEquatorialMatrixEntry();
            if (eq != null) {
                return eq;
            }
        }
        return matrices.last();
    }

    @WrapOperation(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V", ordinal = 1)
    )
    private void gc$renderAstroSun(MeshData meshData, Operation<Void> original) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(this.level)) {
            PlanetRenderer.setSunRenderState(RenderSystem.getShaderTexture(0), RenderSystem.getShaderColor());
            if (!AstroSkyEngine.CONFIG.vanillaSun) {
                RenderSystem.colorMask(false, false, false, false);
            }
            original.call(meshData);
            RenderSystem.colorMask(true, true, true, true);
            return;
        }
        original.call(meshData);
    }

    @WrapOperation(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V", ordinal = 2)
    )
    private void gc$renderAstroMoonAndMain(MeshData meshData, Operation<Void> original) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(this.level)) {
            PlanetRenderer.setMoonRenderState(RenderSystem.getShaderTexture(0), RenderSystem.getShaderColor());
            if (!AstroSkyEngine.CONFIG.vanillaMoon) {
                RenderSystem.colorMask(false, false, false, false);
            }
            original.call(meshData);
            RenderSystem.colorMask(true, true, true, true);
            AstroSkyRenderer.renderMain();
            return;
        }
        original.call(meshData);
    }

    @WrapOperation(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V", ordinal = 1)
    )
    private void gc$drawAstroStarBuffers(VertexBuffer instance, Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderInstance program, Operation<Void> original) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(this.level)) {
            Matrix4f eqMatrix = AstroSkyRenderer.getEquatorialMatrix();
            if (eqMatrix != null) {
                if (AstroSkyEngine.CONFIG.enableVanillaStars) {
                    original.call(instance, eqMatrix, projectionMatrix, program);
                }
                AstroStarRenderer.drawAll(eqMatrix, projectionMatrix, GameRenderer.getPositionColorShader());
                return;
            }
        }
        original.call(instance, viewMatrix, projectionMatrix, program);
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    public void gc$preventCloudRendering(PoseStack matrices, Matrix4f matrix4f, Matrix4f matrix4f2, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            double playerY = player.getVehicle() != null ? player.getVehicle().getY() : player.getY();
            if (playerY > Constant.CLOUD_HEIGHT) {
                // Have clouds slowly fade out
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, normalize((float) playerY, Constant.CLOUD_HEIGHT, Constant.CLOUD_LIMIT, 1, 0F));
                if (playerY > Constant.CLOUD_LIMIT)
                    ci.cancel();
            }
        }
    }

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    public void gc$cancelRainAndSnow(LightTexture lightTexture, float f, double d, double e, double g, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            double playerY = player.getVehicle() != null ? player.getVehicle().getY() : player.getY();
            if (playerY > Constant.CLOUD_HEIGHT)
                ci.cancel();
        }
    }


    @Unique
    private float normalize(float x, float inMin, float inMax, float outMin, float outMax) {
        float outRange = outMax - outMin;
        float inRange = inMax - inMin;
        return (x - inMin) * outRange / inRange + outMin;
    }
}
