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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroLightingManager;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.dimension.OverworldRenderer;
import dev.galacticraft.mod.events.GCGasGiantAtmosphereHandler;
import dev.galacticraft.mod.tag.GCFluidTags;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    @WrapOperation(
            method = "setupColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F")
    )
    private static float gc$modifyAstroFogSkyAngle(ClientLevel world, float partialTick, Operation<Float> original) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(world) && AstroLightingManager.hasComputedLighting()) {
            return AstroLightingManager.getSunsetAltitude();
        }
        return original.call(world, partialTick);
    }

    @ModifyVariable(method = "setupColor", index = 12, at = @At("STORE"))
    private static float gc$modifyAstroFogColor(float original, @Local ClientLevel clientLevel) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(clientLevel) && AstroLightingManager.hasComputedLighting()) {
            return AstroLightingManager.getFogBrightness();
        }
        return original;
    }

    @ModifyVariable(method = "setupColor", index = 18, at = @At(value = "STORE", ordinal = 1))
    private static float gc$modifyAstroSunsetFog(float original, @Local ClientLevel clientLevel) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld(clientLevel) && AstroLightingManager.hasComputedLighting()) {
            return AstroLightingManager.getSkyBrightness();
        }
        return original;
    }

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void gc$setupColor(Camera camera, float partialTicks, ClientLevel clientLevel, int renderDistanceChunks, float bossColorModifier, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            double playerY = player.getVehicle() != null ? player.getVehicle().getY() : player.getY();
            if (playerY > Constant.ATMOSPHERE_FADE_START) {
                if (clientLevel.dimension() == Level.OVERWORLD) {
                    Vec3 vec3 = OverworldRenderer.getFogColor(clientLevel, partialTicks, camera.getPosition());
                    fogRed = (float) vec3.x();
                    fogGreen = (float) vec3.y();
                    fogBlue = (float) vec3.z();
                } else {
                    float progress = net.minecraft.util.Mth.clamp(
                            (float) ((playerY - Constant.ATMOSPHERE_FADE_START) / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FADE_START)),
                            0.0F,
                            1.0F
                    );
                    float remaining = 1.0F - progress * progress * (3.0F - 2.0F * progress);
                    fogRed *= remaining;
                    fogGreen *= remaining;
                    fogBlue *= remaining;
                }
                com.mojang.blaze3d.systems.RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
            }
        }
    }

    @ModifyExpressionValue(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getLookVector()Lorg/joml/Vector3f;"))
    private static Vector3f gc$getSunriseColor(Vector3f original, @Local ClientLevel clientLevel) {
        Player player = Minecraft.getInstance().player;
        if (player != null && clientLevel.dimension() == Level.OVERWORLD) {
            double playerY = player.getVehicle() != null ? player.getVehicle().getY() : player.getY();
            if (playerY > Constant.OVERWORLD_SKYPROVIDER_STARTHEIGHT) {
                return original.zero();
            }
        }
        return original;
    }

    @Inject(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FogRenderer;getPriorityFogFunction(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/FogRenderer$MobEffectFogFunction;"))
    private static void gc$setupFluidFog(Camera camera, float partialTicks, ClientLevel clientLevel, int renderDistanceChunks, float bossColorModifier, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player.isEyeInFluid(GCFluidTags.OIL)) {
            fogRed = 0.0F;
            fogGreen = 0.0F;
            fogBlue = 0.0F;
        } else if (player.isEyeInFluid(GCFluidTags.FUEL)) {
            fogRed = 0.72F;
            fogGreen = 0.58F;
            fogBlue = 0.18F;
        } else if (player.isEyeInFluid(GCFluidTags.SULFURIC_ACID)) {
            fogRed = 0.41F;
            fogGreen = 0.78F;
            fogBlue = 0.25F;
        }
    }

    @ModifyArg(method = "setupFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V", remap = false), index = 0)
    private static float gc$setShaderFogStart(float start, @Local FogType fogType) {
        Player player = Minecraft.getInstance().player;
        if (fogType != FogType.NONE) {
            if (player.isEyeInFluid(GCFluidTags.OIL)) {
                return -8.0F;
            } else if (player.isEyeInFluid(GCFluidTags.FUEL)) {
                return -8.0F;
            } else if (player.isEyeInFluid(GCFluidTags.SULFURIC_ACID)) {
                return -8.0F;
            }
        } else if (player != null) {
            double playerY = player.getVehicle() != null ? player.getVehicle().getY() : player.getY();
            if (playerY >= Constant.ATMOSPHERE_EXIT_END) {
                return Float.MAX_VALUE;
            } else if (playerY > Constant.ATMOSPHERE_FOG_START) {
                // Atmospheric exit fog band (Y=450 -> Y=780):
                // Softly brings in upper-atmosphere fog around the midpoint, then clears into pure space at Y=780
                float progress = (float) ((playerY - Constant.ATMOSPHERE_FOG_START) / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FOG_START));
                float exitHaze = (float) Math.sin(progress * Math.PI);
                if (progress > 0.85F) {
                    float clearT = (progress - 0.85F) / 0.15F;
                    return net.minecraft.util.Mth.lerp(clearT * clearT, start, 4096.0F);
                }
                return net.minecraft.util.Mth.lerp(exitHaze, start, 12.0F);
            } else if (GCGasGiantAtmosphereHandler.isGasGiant(player.level().dimension()) && playerY < 210.0D) {
                float scale = GCGasGiantAtmosphereHandler.computeFogDistanceScale(player.level().dimension(), playerY);
                return Math.min(start, Math.max(4.0F, start * scale * 0.55F));
            }
        }
        return start;
    }

    @ModifyArg(method = "setupFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogEnd(F)V", remap = false), index = 0)
    private static float gc$setShaderFogEnd(float end, @Local FogType fogType) {
        Player player = Minecraft.getInstance().player;
        if (fogType != FogType.NONE) {
            if (player.isEyeInFluid(GCFluidTags.OIL)) {
                return 8.0F;
            } else if (player.isEyeInFluid(GCFluidTags.FUEL)) {
                return 12.0F;
            } else if (player.isEyeInFluid(GCFluidTags.SULFURIC_ACID)) {
                return 12.0F;
            }
        } else if (player != null) {
            double playerY = player.getVehicle() != null ? player.getVehicle().getY() : player.getY();
            if (playerY >= Constant.ATMOSPHERE_EXIT_END) {
                return Float.MAX_VALUE;
            } else if (playerY > Constant.ATMOSPHERE_FOG_START) {
                float progress = (float) ((playerY - Constant.ATMOSPHERE_FOG_START) / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FOG_START));
                float exitHaze = (float) Math.sin(progress * Math.PI);
                if (progress > 0.85F) {
                    float clearT = (progress - 0.85F) / 0.15F;
                    return net.minecraft.util.Mth.lerp(clearT * clearT, end, 8192.0F);
                }
                return net.minecraft.util.Mth.lerp(exitHaze, end, 110.0F);
            } else if (GCGasGiantAtmosphereHandler.isGasGiant(player.level().dimension()) && playerY < 210.0D) {
                float scale = GCGasGiantAtmosphereHandler.computeFogDistanceScale(player.level().dimension(), playerY);
                return Math.min(end, Math.max(42.0F, end * scale));
            }
        }
        return end;
    }
}
