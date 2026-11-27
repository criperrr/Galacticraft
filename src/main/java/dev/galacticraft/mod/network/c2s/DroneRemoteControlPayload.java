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
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * C2S payload streaming live remote-pilot flight commands (cyclic pitch/roll, yaw, collective,
 * and auxiliary switches) from a player holding a linked {@code DroneRemoteControllerItem}
 * to a {@code SMALL} unmanned rotorcraft drone within radio link range.
 */
public record DroneRemoteControlPayload(
        int entityId,
        float forward,
        float strafe,
        float yawDelta,
        float collective,
        boolean toggleLights,
        boolean toggleEngine
) implements C2SPayload {
    public static final double MAX_RANGE_BLOCKS = 128.0;
    public static final ResourceLocation ID = Constant.id("drone_remote_control");
    public static final CustomPacketPayload.Type<DroneRemoteControlPayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<ByteBuf, DroneRemoteControlPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.entityId);
                ByteBufCodecs.FLOAT.encode(buf, p.forward);
                ByteBufCodecs.FLOAT.encode(buf, p.strafe);
                ByteBufCodecs.FLOAT.encode(buf, p.yawDelta);
                ByteBufCodecs.FLOAT.encode(buf, p.collective);
                ByteBufCodecs.BOOL.encode(buf, p.toggleLights);
                ByteBufCodecs.BOOL.encode(buf, p.toggleEngine);
            },
            buf -> new DroneRemoteControlPayload(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );

    @Override
    public void handle(ServerPlayNetworking.@NotNull Context context) {
        var player = context.player();
        Entity entity = player.level().getEntity(this.entityId);
        if (!(entity instanceof ModularVehicleEntity vehicle) || !vehicle.isRemoteControllableDrone()) {
            return;
        }
        if (player.distanceToSqr(vehicle) > MAX_RANGE_BLOCKS * MAX_RANGE_BLOCKS) {
            return;
        }
        vehicle.applyRemoteControlInput(
                player,
                this.forward,
                this.strafe,
                this.yawDelta,
                this.collective,
                this.toggleLights,
                this.toggleEngine
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
