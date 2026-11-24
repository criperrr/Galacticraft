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
import dev.galacticraft.mod.content.block.entity.machine.AerospaceRdTerminalBlockEntity;
import dev.galacticraft.mod.research.ResearchManager;
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
 * C2S payload for interactive actions inside the {@code AEROSPACE_RD_TERMINAL} screen
 * (unlocking research nodes with Domain Points, requesting a live network sync, or etching schematics).
 */
public record ResearchTerminalActionPayload(
        @NotNull BlockPos pos,
        int actionId,
        @NotNull ResourceLocation targetId
) implements C2SPayload {
    public static final int ACTION_UNLOCK_NODE = 0;
    public static final int ACTION_SYNC_NOW = 1;
    public static final int ACTION_ETCH_SCHEMATIC = 2;

    public static final ResourceLocation ID = Constant.id("research_terminal_action");
    public static final Type<ResearchTerminalActionPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, ResearchTerminalActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ResearchTerminalActionPayload::pos,
            ByteBufCodecs.VAR_INT, ResearchTerminalActionPayload::actionId,
            ResourceLocation.STREAM_CODEC, ResearchTerminalActionPayload::targetId,
            ResearchTerminalActionPayload::new
    );

    @Override
    public void handle(ServerPlayNetworking.@NotNull Context context) {
        ServerPlayer player = context.player();
        if (player.distanceToSqr(this.pos.getCenter()) > 100.0) {
            return;
        }
        BlockEntity be = player.serverLevel().getBlockEntity(this.pos);
        if (!(be instanceof AerospaceRdTerminalBlockEntity terminal)) {
            return;
        }
        switch (this.actionId) {
            case ACTION_UNLOCK_NODE -> ResearchManager.tryUnlockNodeFromTerminal(player.serverLevel(), player, this.targetId);
            case ACTION_SYNC_NOW -> ResearchManager.syncToPlayer(player);
            case ACTION_ETCH_SCHEMATIC -> terminal.tryEtchSchematicForNode(player.serverLevel(), player, this.targetId);
            default -> {
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
