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

package dev.galacticraft.mod.client.sounds;

import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * Procedural looping electric vehicle sound instance that dynamically modulates pitch and volume
 * from live telemetry (speed, rotor/propeller RPM, throttle, VTOL plasma thrusters, and planetary
 * atmospheric pressure).
 */
@Environment(EnvType.CLIENT)
public class VehiclePropulsionSound extends AbstractTickableSoundInstance {
    public enum Channel {
        ELECTRIC_MOTOR,
        ROTOR_BLADES,
        PROPELLER,
        VTOL_THRUSTER
    }

    private final ModularVehicleEntity vehicle;
    private final Channel channel;

    public VehiclePropulsionSound(@NotNull ModularVehicleEntity vehicle, @NotNull SoundEvent soundEvent, @NotNull Channel channel) {
        super(soundEvent, SoundSource.NEUTRAL, vehicle.level().getRandom());
        this.vehicle = vehicle;
        this.channel = channel;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = 0.75F;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }

    public @NotNull Channel getChannel() {
        return this.channel;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (!this.vehicle.isAlive() || this.vehicle.isRemoved()) {
            this.stop();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.level() != this.vehicle.level() || mc.player.distanceToSqr(this.vehicle) > 2304.0) {
            this.stop();
            return;
        }

        this.x = this.vehicle.getX();
        this.y = this.vehicle.getY();
        this.z = this.vehicle.getZ();

        boolean engineOn = this.vehicle.isEngineRunning();
        boolean riding = mc.player.getVehicle() == this.vehicle;
        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(this.vehicle.level());
        // In vacuum, external aerodynamic noise is heavily muted, while riders still feel chassis vibration
        float acousticFactor = env.pressureAtm() < 0.005
                ? (riding ? (this.vehicle.isPressurizedCabin() ? 0.55F : 0.35F) : 0.14F)
                : (float) Mth.clamp(0.42 + env.pressureAtm() * 0.58, 0.42, 1.0);

        float speed = Math.abs(this.vehicle.getSyncedSpeed());
        float rpm = this.vehicle.getRotorRpm();
        float rpmNorm = Mth.clamp(rpm / 900.0F, 0.0F, 1.25F);
        float throttle = Mth.clamp(this.vehicle.getThrottle(), 0.0F, 1.0F);

        switch (this.channel) {
            case ELECTRIC_MOTOR -> {
                if (!engineOn && speed < 0.02F) {
                    this.stop();
                    return;
                }
                float speedFrac = Mth.clamp(speed / 0.65F, 0.0F, 1.0F);
                this.pitch = Mth.lerp(0.18F, this.pitch, 0.68F + speedFrac * 1.05F + (engineOn ? 0.08F : 0.0F));
                float targetVol = (0.08F + speedFrac * 0.44F) * acousticFactor;
                this.volume = Mth.lerp(0.20F, this.volume, targetVol);
            }
            case ROTOR_BLADES -> {
                if (rpm < 25.0F && !engineOn) {
                    this.stop();
                    return;
                }
                this.pitch = Mth.lerp(0.16F, this.pitch, 0.52F + rpmNorm * 1.12F);
                float targetVol = Mth.clamp(rpmNorm * 0.72F, 0.03F, 0.75F) * acousticFactor;
                this.volume = Mth.lerp(0.18F, this.volume, targetVol);
            }
            case PROPELLER -> {
                if (rpm < 25.0F && !engineOn) {
                    this.stop();
                    return;
                }
                this.pitch = Mth.lerp(0.18F, this.pitch, 0.72F + rpmNorm * 0.92F + throttle * 0.35F);
                float targetVol = Mth.clamp((0.12F + rpmNorm * 0.45F + throttle * 0.25F), 0.03F, 0.78F) * acousticFactor;
                this.volume = Mth.lerp(0.18F, this.volume, targetVol);
            }
            case VTOL_THRUSTER -> {
                if (!engineOn || rpm < 80.0F) {
                    this.stop();
                    return;
                }
                // Electric plasma thruster hum transmits through hull & plume even in vacuum
                float thrusterFactor = Math.max(acousticFactor, riding ? 0.62F : 0.32F);
                this.pitch = Mth.lerp(0.20F, this.pitch, 1.45F + rpmNorm * 0.62F);
                float targetVol = Mth.clamp(rpmNorm * 0.52F, 0.05F, 0.58F) * thrusterFactor;
                this.volume = Mth.lerp(0.20F, this.volume, targetVol);
            }
        }
    }
}
