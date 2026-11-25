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

import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
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

/**
 * Client-to-server control payload dispatched from the Hangar Terminal screen
 * to configure, detect, preview, assemble, or disassemble a modular rocket platform.
 */
public record HangarTerminalActionPayload(BlockPos pos, Action action, int value) implements C2SPayload {
    public static final ResourceLocation ID = Constant.id("hangar_terminal_action");
    public static final CustomPacketPayload.Type<HangarTerminalActionPayload> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<ByteBuf, HangarTerminalActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            HangarTerminalActionPayload::pos,
            Action.STREAM_CODEC,
            HangarTerminalActionPayload::action,
            ByteBufCodecs.VAR_INT,
            HangarTerminalActionPayload::value,
            HangarTerminalActionPayload::new
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

        switch (this.action) {
            case SET_TYPE -> {
                if (!terminal.isFormed()) {
                    terminal.setPlatformType(this.value == 1 ? PlatformType.HORIZONTAL : PlatformType.VERTICAL);
                }
            }
            case SET_SIZE -> {
                if (!terminal.isFormed()) {
                    terminal.setPlatformSize(PlatformSize.byOrdinal(this.value));
                }
            }
            case DETECT_PAD -> {
                if (!terminal.isFormed()) {
                    terminal.autoDetectPad();
                }
            }
            case TOGGLE_PREVIEW -> terminal.togglePreview();
            case SET_LAYER_Y -> terminal.setPreviewLayerY(this.value);
            case ASSEMBLE -> {
                if (!terminal.isFormed()) {
                    terminal.assemblePlatform(player.isCreative());
                }
            }
            case DISASSEMBLE -> {
                if (terminal.isFormed()) {
                    terminal.disassemblePlatform(null, false);
                }
            }
            case TOGGLE_AXIS_GUIDE -> terminal.toggleAxisGuide();
            case TRIGGER_PART_ANIM -> {
                if (terminal.isFormed()) {
                    terminal.triggerRocketPartAnimation(this.value);
                }
            }
            case TOGGLE_PART_HATCH -> {
                if (terminal.isFormed()) {
                    terminal.toggleRocketPartHatch(this.value);
                }
            }
            case BUILD_ROCKET -> {
                if (terminal.isFormed()) {
                    terminal.buildRocketFromSlots();
                }
            }
            case RECALL_ROCKET -> {
                if (terminal.isFormed()) {
                    terminal.recallRocketToSlots(player);
                }
            }
            case CREATIVE_FILL_ROCKET -> {
                if (terminal.isFormed() && player.isCreative()) {
                    terminal.creativeToggleFillRocket();
                }
            }
            case SET_SYMMETRY -> {
                if (terminal.isFormed()) {
                    terminal.setSelectedSymmetry(RadialSymmetry.byOrdinal(this.value));
                }
            }
            case TOGGLE_CROSSFEED -> {
                if (terminal.isFormed()) {
                    terminal.toggleSelectedCrossfeed();
                }
            }
            case CANCEL_ASSEMBLY -> {
                if (terminal.isFormed()) {
                    terminal.cancelProgressiveAssembly(player);
                }
            }
            case START_LAUNCH -> {
                if (terminal.isFormed()) {
                    terminal.startLaunchSequence(player);
                }
            }
            case ABORT_LAUNCH -> {
                if (terminal.isFormed()) {
                    terminal.abortLaunchSequence("Aborto manual solicitado pelo operador");
                }
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        SET_TYPE,
        SET_SIZE,
        DETECT_PAD,
        TOGGLE_PREVIEW,
        SET_LAYER_Y,
        ASSEMBLE,
        DISASSEMBLE,
        TOGGLE_AXIS_GUIDE,
        TRIGGER_PART_ANIM,
        TOGGLE_PART_HATCH,
        BUILD_ROCKET,
        RECALL_ROCKET,
        CREATIVE_FILL_ROCKET,
        SET_SYMMETRY,
        TOGGLE_CROSSFEED,
        CANCEL_ASSEMBLY,
        START_LAUNCH,
        ABORT_LAUNCH;

        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(
                i -> values()[Math.floorMod(i, values().length)],
                Enum::ordinal
        );
    }
}
