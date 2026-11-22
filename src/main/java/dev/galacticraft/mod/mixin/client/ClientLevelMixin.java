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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.galacticraft.mod.accessor.GCLevelAccessor;
import dev.galacticraft.mod.client.render.astro.AstroLightingManager;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.misc.footprint.FootprintManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
@Environment(EnvType.CLIENT)
public abstract class ClientLevelMixin implements GCLevelAccessor {
    private final @Unique FootprintManager footprintManager = new FootprintManager();

    @Inject(method = "tickTime", at = @At("RETURN"))
    private void update(CallbackInfo ci) {
        ((Level) (Object) this).updateSkyBrightness();
    }

    @ModifyVariable(method = "getSkyColor", ordinal = 2, at = @At(value = "STORE", ordinal = 1))
    private float gc$modifyAstroSkyBrightness(float g) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld((ClientLevel) (Object) this) && AstroLightingManager.hasComputedLighting()) {
            return AstroLightingManager.getSkyBrightness();
        }
        return g;
    }

    @ModifyVariable(method = "getCloudColor", ordinal = 2, at = @At(value = "STORE", ordinal = 0))
    private float gc$modifyAstroCloudBrightness(float g) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld((ClientLevel) (Object) this) && AstroLightingManager.hasComputedLighting()) {
            return AstroLightingManager.getSkyBrightness();
        }
        return g;
    }

    @ModifyReturnValue(method = "getStarBrightness", at = @At("RETURN"))
    private float gc$modifyAstroStarBrightness(float original) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld((ClientLevel) (Object) this) && AstroLightingManager.hasComputedLighting()) {
            return AstroLightingManager.getStarVisibility();
        }
        return original;
    }

    @ModifyVariable(method = "getSkyDarken", ordinal = 2, at = @At(value = "STORE", ordinal = 0))
    private float gc$modifyAstroBlockBrightness(float g) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld((ClientLevel) (Object) this) && AstroLightingManager.hasComputedLighting()) {
            return 1.0F - AstroLightingManager.getBlockBrightness();
        }
        return g;
    }

    @ModifyReturnValue(method = "getSkyDarken", at = @At("RETURN"))
    private float gc$modifyAstroAmbientBrightness(float g) {
        if (AstroSkyEngine.isInitialized() && AstroSkyEngine.isOverworld((ClientLevel) (Object) this) && AstroLightingManager.hasComputedLighting()) {
            float raw = (g - 0.2F) / 0.8F;
            float ambient = 0.01F * (float) AstroSkyEngine.CONFIG.lightingOptions.ambientLight;
            return raw * (1.0F - ambient) + ambient;
        }
        return g;
    }

    @Override
    public FootprintManager galacticraft$getFootprintManager() {
        return footprintManager;
    }
}
