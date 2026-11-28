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

import dev.galacticraft.api.vehicle.ai.VehicleMissionConfig;
import dev.galacticraft.impl.network.c2s.C2SPayload;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * C2S payload updating an autonomous modular vehicle's {@code AI_CORE} {@link VehicleMissionConfig}.
 */
public record VehicleAiConfigPayload(int entityId, @NotNull VehicleMissionConfig config) implements C2SPayload {
    public static final ResourceLocation ID = Constant.id("vehicle_ai_config");
    public static final CustomPacketPayload.Type<VehicleAiConfigPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<ByteBuf, VehicleAiConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VehicleAiConfigPayload::entityId,
            VehicleMissionConfig.STREAM_CODEC, VehicleAiConfigPayload::config,
            VehicleAiConfigPayload::new
    );

    @Override
    public void handle(ServerPlayNetworking.@NotNull Context context) {
        var player = context.player();
        Entity entity = player.level().getEntity(this.entityId);
        if (!(entity instanceof ModularVehicleEntity vehicle)) {
            return;
        }
        if (player.distanceToSqr(vehicle) > 4096.0 && player.getVehicle() != vehicle) {
            return;
        }
        if (!vehicle.getDerivedStats().hasAiCore()) {
            return;
        }
        vehicle.setMissionConfig(this.config);
        player.level().playSound(null, vehicle.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.55F, 1.35F);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
