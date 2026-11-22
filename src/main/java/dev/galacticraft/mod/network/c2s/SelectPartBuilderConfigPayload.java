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

import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.impl.network.c2s.C2SPayload;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.block.entity.RocketWorkbenchBlockEntity;
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

import java.util.Objects;

/**
 * Client-to-server configuration payload sent from {@code PartAssemblerScreen} when the player
 * selects a modular rocket part family, parametric dimensions, or hull livery.
 */
public record SelectPartBuilderConfigPayload(
        @NotNull BlockPos pos,
        @NotNull String familyId,
        int diameter,
        int topDiameter,
        int heightBlocks,
        @NotNull RocketLivery livery
) implements C2SPayload {
    public static final ResourceLocation ID = Constant.id("select_part_builder_config");
    public static final CustomPacketPayload.Type<SelectPartBuilderConfigPayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<ByteBuf, SelectPartBuilderConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SelectPartBuilderConfigPayload::pos,
            ByteBufCodecs.STRING_UTF8,
            SelectPartBuilderConfigPayload::familyId,
            ByteBufCodecs.VAR_INT,
            SelectPartBuilderConfigPayload::diameter,
            ByteBufCodecs.VAR_INT,
            SelectPartBuilderConfigPayload::topDiameter,
            ByteBufCodecs.VAR_INT,
            SelectPartBuilderConfigPayload::heightBlocks,
            RocketLivery.STREAM_CODEC,
            SelectPartBuilderConfigPayload::livery,
            SelectPartBuilderConfigPayload::new
    );

    private static final double MAX_INTERACT_DISTANCE_SQ = 64.0;

    public SelectPartBuilderConfigPayload {
        Objects.requireNonNull(pos, "pos");
        familyId = familyId != null ? familyId : "";
        diameter = Math.max(1, diameter);
        topDiameter = Math.max(1, topDiameter);
        heightBlocks = Math.max(1, heightBlocks);
        livery = livery != null ? livery : RocketLivery.WHITE;
    }

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
        if (!(level.getBlockEntity(this.pos) instanceof RocketWorkbenchBlockEntity workbench) || workbench.isRemoved()) {
            return;
        }

        workbench.setModularSelection(
                this.familyId,
                this.diameter,
                this.topDiameter,
                this.heightBlocks,
                this.livery
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
