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
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Client-to-server payload dispatched from the Hangar Terminal VAB screen to edit an already-mounted
 * rocket in-place (reorder/remove/insert core modules while preserving radial boosters, or remove
 * only radial boosters without dismantling the core).
 */
public record EditRocketCorePayload(
        BlockPos pos,
        List<CoreEntry> entries,
        boolean removeRadialBoostersOnly
) implements C2SPayload {
    public static final int MAX_CORE_ENTRIES = 64;
    public static final ResourceLocation ID = Constant.id("edit_rocket_core");
    public static final CustomPacketPayload.Type<EditRocketCorePayload> TYPE = new CustomPacketPayload.Type<>(ID);

    public record CoreEntry(boolean fromPad, BlockPos padPos, int inventorySlot) {
        public static final StreamCodec<ByteBuf, CoreEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                CoreEntry::fromPad,
                BlockPos.STREAM_CODEC,
                CoreEntry::padPos,
                ByteBufCodecs.VAR_INT,
                CoreEntry::inventorySlot,
                CoreEntry::new
        );

        public static CoreEntry mounted(BlockPos padPos) {
            return new CoreEntry(true, padPos.immutable(), -1);
        }

        public static CoreEntry inventory(int inventorySlot) {
            return new CoreEntry(false, BlockPos.ZERO, inventorySlot);
        }
    }

    public static final StreamCodec<ByteBuf, EditRocketCorePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            EditRocketCorePayload::pos,
            CoreEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CORE_ENTRIES)),
            EditRocketCorePayload::entries,
            ByteBufCodecs.BOOL,
            EditRocketCorePayload::removeRadialBoostersOnly,
            EditRocketCorePayload::new
    );

    private static final double MAX_INTERACT_DISTANCE_SQ = 64.0;

    @Override
    public void handle(@NotNull ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        Level level = player.level();
        if (!level.isLoaded(this.pos)) {
            return;
        }
        double distSq = player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);
        if (distSq > MAX_INTERACT_DISTANCE_SQ) {
            return;
        }
        if (!(level.getBlockEntity(this.pos) instanceof HangarTerminalBlockEntity terminal) || terminal.isRemoved()) {
            return;
        }
        if (!terminal.isFormed()) {
            return;
        }
        if (this.removeRadialBoostersOnly) {
            terminal.removeRadialBoosters(player);
        } else {
            terminal.applyEditedRocketCore(player, this.entries);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
