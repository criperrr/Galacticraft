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

package dev.galacticraft.mod.network.c2s;

import dev.galacticraft.impl.network.c2s.C2SPayload;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Client-to-server flight control payload for {@link ModularRocketFlightEntity}:
 * manual WASD pitch/yaw override, throttle adjustments, G manual staging / autopilot toggle,
 * Backspace LES emergency abort, landing gear toggle, and retro-burn fire.
 */
public record ModularRocketControlPayload(
        int entityId,
        float pitchInput,
        float yawInput,
        float throttleDelta,
        int actionFlags
) implements C2SPayload {
    public static final int FLAG_MANUAL_STAGE = 1;
    public static final int FLAG_TOGGLE_AUTOPILOT = 1 << 1;
    public static final int FLAG_LES_ABORT = 1 << 2;
    public static final int FLAG_TOGGLE_LANDING_GEAR = 1 << 3;
    public static final int FLAG_RETRO_BURN = 1 << 4;
    public static final int FLAG_ENGINE_CUTOFF = 1 << 5;

    public static final ResourceLocation ID = Constant.id("modular_rocket_control");
    public static final CustomPacketPayload.Type<ModularRocketControlPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<ByteBuf, ModularRocketControlPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ModularRocketControlPayload::entityId,
            ByteBufCodecs.FLOAT, ModularRocketControlPayload::pitchInput,
            ByteBufCodecs.FLOAT, ModularRocketControlPayload::yawInput,
            ByteBufCodecs.FLOAT, ModularRocketControlPayload::throttleDelta,
            ByteBufCodecs.VAR_INT, ModularRocketControlPayload::actionFlags,
            ModularRocketControlPayload::new
    );

    @Override
    public void handle(@NotNull ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof ModularRocketFlightEntity rocket)) {
            return;
        }
        if (rocket.getId() != this.entityId) {
            return;
        }
        // Only the controlling pilot (first passenger) may issue flight commands,
        // except LES Abort which any seated crew member may trigger in an emergency.
        boolean isPilot = rocket.getFirstPassenger() == player;
        boolean lesAbort = (this.actionFlags & FLAG_LES_ABORT) != 0;
        if (!isPilot && !lesAbort) {
            return;
        }

        if (isPilot) {
            float safePitch = Float.isFinite(this.pitchInput) ? Math.clamp(this.pitchInput, -1.0F, 1.0F) : 0.0F;
            float safeYaw = Float.isFinite(this.yawInput) ? Math.clamp(this.yawInput, -1.0F, 1.0F) : 0.0F;
            float safeThrottle = Float.isFinite(this.throttleDelta) ? Math.clamp(this.throttleDelta, -1.0F, 1.0F) : 0.0F;
            rocket.applyPilotControl(
                    player,
                    safePitch,
                    safeYaw,
                    safeThrottle,
                    (this.actionFlags & FLAG_MANUAL_STAGE) != 0,
                    (this.actionFlags & FLAG_TOGGLE_AUTOPILOT) != 0,
                    (this.actionFlags & FLAG_TOGGLE_LANDING_GEAR) != 0,
                    (this.actionFlags & FLAG_RETRO_BURN) != 0,
                    (this.actionFlags & FLAG_ENGINE_CUTOFF) != 0
            );
        }
        if (lesAbort) {
            rocket.triggerLesAbort(player);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
