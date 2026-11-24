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
import dev.galacticraft.mod.content.block.entity.machine.ObservatoryTelescopeBlockEntity;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * C2S payload sent by the first-person Observatory Telescope eyepiece when a player
 * aims the optical tube or locks focus on a real {@code AstroSkyEngine} celestial object
 * (planet, moon, comet, star, nebula, or galaxy).
 */
public record ObservatoryCapturePayload(
        @NotNull BlockPos pos,
        boolean capture,
        @NotNull String objectId,
        @NotNull String displayName,
        @NotNull String category,
        float apparentMagnitude,
        int sharpnessPct,
        float yawDeg,
        float pitchDeg
) implements C2SPayload {
    public static final ResourceLocation ID = Constant.id("observatory_capture");
    public static final Type<ObservatoryCapturePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, ObservatoryCapturePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull ObservatoryCapturePayload decode(@NotNull ByteBuf buf) {
            return new ObservatoryCapturePayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf)
            );
        }

        @Override
        public void encode(@NotNull ByteBuf buf, @NotNull ObservatoryCapturePayload payload) {
            BlockPos.STREAM_CODEC.encode(buf, payload.pos);
            ByteBufCodecs.BOOL.encode(buf, payload.capture);
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.objectId);
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.displayName);
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.category);
            ByteBufCodecs.FLOAT.encode(buf, payload.apparentMagnitude);
            ByteBufCodecs.VAR_INT.encode(buf, payload.sharpnessPct);
            ByteBufCodecs.FLOAT.encode(buf, payload.yawDeg);
            ByteBufCodecs.FLOAT.encode(buf, payload.pitchDeg);
        }
    };

    @Override
    public void handle(ServerPlayNetworking.@NotNull Context context) {
        ServerPlayer player = context.player();
        if (player.distanceToSqr(this.pos.getCenter()) > 144.0) {
            return;
        }
        BlockEntity be = player.serverLevel().getBlockEntity(this.pos);
        if (!(be instanceof ObservatoryTelescopeBlockEntity telescope)) {
            return;
        }
        telescope.updateAimOrientation(this.yawDeg, this.pitchDeg);
        if (this.capture && !this.objectId.isEmpty()) {
            telescope.handlePhotometricCapture(
                    player.serverLevel(),
                    player,
                    this.objectId,
                    this.displayName,
                    this.category,
                    this.apparentMagnitude,
                    this.sharpnessPct
            );
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
