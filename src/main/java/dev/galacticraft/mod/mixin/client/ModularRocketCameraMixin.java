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

import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dynamic third-person camera framing and procedural camera shake for {@link ModularRocketFlightEntity}
 * and nearby launchpad Thrust Buildup sequences.
 *
 * <p>Scales 3rd-person distance smoothly using {@code cameraDistance = 4.00 + heightBlocks * 0.45}
 * so tall 30-60 block modular rockets remain completely framed during liftoff and staging.
 */
@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class ModularRocketCameraMixin {
    @Shadow
    protected abstract void move(float forward, float up, float left);

    @Shadow
    protected abstract float getMaxZoom(float desiredDistance);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract float getYRot();

    @Shadow
    protected abstract float getXRot();

    @Unique
    private static final float BASE_THIRD_PERSON_DISTANCE = 4.00F;

    @Unique
    private static final float HEIGHT_DISTANCE_FACTOR = 0.45F;

    @Inject(method = "setup", at = @At("TAIL"))
    private void gc$modularRocketCamera(
            BlockGetter level,
            Entity entity,
            boolean detached,
            boolean thirdPersonReverse,
            float partialTick,
            CallbackInfo ci
    ) {
        if (entity == null) {
            return;
        }

        if (entity.isPassenger() && entity.getVehicle() instanceof ModularRocketFlightEntity rocket) {
            float heightBlocks = Math.max(2.0F, rocket.getTotalHeightBlocks());
            if (detached) {
                // Target total distance = 4.00 + height * 0.45 (vanilla already moved back 4.00)
                float desiredTotalDistance = BASE_THIRD_PERSON_DISTANCE + heightBlocks * HEIGHT_DISTANCE_FACTOR;
                float extraPullback = Math.max(0.0F, desiredTotalDistance - BASE_THIRD_PERSON_DISTANCE);
                float safeExtra = Math.max(0.0F, this.getMaxZoom(desiredTotalDistance) - BASE_THIRD_PERSON_DISTANCE);
                this.move(-Math.min(extraPullback, safeExtra), heightBlocks * 0.12F, 0.0F);
            }

            float shake = rocket.getCameraShake();
            if (shake > 0.01F) {
                float t = ((float) rocket.tickCount + partialTick) * 1.9F;
                float amp = shake * (detached ? 0.65F : 0.40F);
                float shakeYaw = Mth.sin(t * 11.3F) * amp;
                float shakePitch = Mth.cos(t * 13.7F) * amp;
                this.setRotation(this.getYRot() + shakeYaw, Mth.clamp(this.getXRot() + shakePitch, -90.0F, 90.0F));
            }
            return;
        }

        // Distance-scaled acoustic rumble camera shake when standing near a launchpad in Thrust Buildup
        Vec3 pos = entity.position();
        float padShake = 0.0F;
        for (HangarTerminalBlockEntity terminal : HangarTerminalBlockEntity.getActiveClientTerminals()) {
            if (terminal.isRemoved() || terminal.getLaunchPhase() != HangarTerminalBlockEntity.LaunchSequencePhase.THRUST_BUILDUP) {
                continue;
            }
            double dist = pos.distanceTo(Vec3.atCenterOf(terminal.getPadCenterPos()));
            if (dist < 48.0) {
                float falloff = (float) (1.0 - dist / 48.0);
                padShake = Math.max(padShake, falloff * falloff * 0.75F);
            }
        }
        if (padShake > 0.02F) {
            float t = ((float) entity.tickCount + partialTick) * 2.1F;
            this.setRotation(
                    this.getYRot() + Mth.sin(t * 14.1F) * padShake,
                    Mth.clamp(this.getXRot() + Mth.cos(t * 16.3F) * padShake, -90.0F, 90.0F)
            );
        }
    }
}
