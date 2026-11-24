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

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroSpyglassManager;
import dev.galacticraft.mod.client.render.astro.ClientTelescopeController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @WrapMethod(method = "onScroll")
    private void gc$onAstroSpyglassScroll(long window, double horizontal, double vertical, Operation<Void> original) {
        if (ClientTelescopeController.isActive()) {
            AstroSpyglassManager.onMouseScroll(vertical);
            return;
        }
        if (!AstroSkyEngine.CONFIG.enableSpyglassZoom || !AstroSpyglassManager.isInUse() || !AstroSpyglassManager.onMouseScroll(vertical)) {
            original.call(window, horizontal, vertical);
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void gc$blockMouseButtonsInTelescope(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (ClientTelescopeController.isActive()) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "turnPlayer", ordinal = 3, at = @At("STORE"))
    private double gc$modifySpyglassSensitivityE(double e) {
        return (AstroSpyglassManager.isInUse() && !ClientTelescopeController.isActive()) ? e / AstroSpyglassManager.getZoom() * 10.0 : e;
    }

    @ModifyVariable(method = "turnPlayer", ordinal = 4, at = @At("STORE"))
    private double gc$modifySpyglassSensitivityF(double f) {
        return (AstroSpyglassManager.isInUse() && !ClientTelescopeController.isActive()) ? f * AstroSpyglassManager.getZoom() / 10.0 : f;
    }

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void gc$routeMouseTurnToTelescope(LocalPlayer player, double yawDelta, double pitchDelta, Operation<Void> original) {
        if (ClientTelescopeController.isActive()) {
            ClientTelescopeController.onMouseTurn(yawDelta, pitchDelta);
            return;
        }
        original.call(player, yawDelta, pitchDelta);
    }
}
