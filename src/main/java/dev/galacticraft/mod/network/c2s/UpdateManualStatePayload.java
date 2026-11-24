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
import dev.galacticraft.mod.attachments.GCAttachments;
import dev.galacticraft.mod.attachments.ManualReaderAttachment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * C2S payload updating a player's persistent {@link ManualReaderAttachment} (read chapters,
 * bookmarked favorites, and pinned HUD mission card).
 */
@SuppressWarnings("UnstableApiUsage")
public record UpdateManualStatePayload(
        int action,
        @NotNull ResourceLocation entryId,
        @NotNull Optional<ManualReaderAttachment.PinnedMission> pinnedMission
) implements C2SPayload {
    public static final int ACTION_MARK_READ = 0;
    public static final int ACTION_TOGGLE_FAVORITE = 1;
    public static final int ACTION_SET_PIN = 2;
    public static final int ACTION_CLEAR_PIN = 3;
    public static final int ACTION_TOGGLE_PIN_COLLAPSE = 4;

    public static final ResourceLocation ID = Constant.id("update_manual_state");
    public static final Type<UpdateManualStatePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateManualStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ByteBufCodecs.VAR_INT.encode(buf, val.action);
                ResourceLocation.STREAM_CODEC.encode(buf, val.entryId);
                ByteBufCodecs.BOOL.encode(buf, val.pinnedMission.isPresent());
                val.pinnedMission.ifPresent(pin -> ManualReaderAttachment.PinnedMission.STREAM_CODEC.encode(buf, pin));
            },
            buf -> {
                int act = ByteBufCodecs.VAR_INT.decode(buf);
                ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                boolean hasPin = ByteBufCodecs.BOOL.decode(buf);
                Optional<ManualReaderAttachment.PinnedMission> pin = hasPin
                        ? Optional.of(ManualReaderAttachment.PinnedMission.STREAM_CODEC.decode(buf))
                        : Optional.empty();
                return new UpdateManualStatePayload(act, id, pin);
            }
    );

    public static UpdateManualStatePayload markRead(@NotNull ResourceLocation entryId) {
        return new UpdateManualStatePayload(ACTION_MARK_READ, entryId, Optional.empty());
    }

    public static UpdateManualStatePayload toggleFavorite(@NotNull ResourceLocation entryId) {
        return new UpdateManualStatePayload(ACTION_TOGGLE_FAVORITE, entryId, Optional.empty());
    }

    public static UpdateManualStatePayload setPin(@NotNull ManualReaderAttachment.PinnedMission mission) {
        return new UpdateManualStatePayload(ACTION_SET_PIN, mission.entryId(), Optional.of(mission));
    }

    public static UpdateManualStatePayload clearPin() {
        return new UpdateManualStatePayload(ACTION_CLEAR_PIN, Constant.id("none"), Optional.empty());
    }

    public static UpdateManualStatePayload togglePinCollapse() {
        return new UpdateManualStatePayload(ACTION_TOGGLE_PIN_COLLAPSE, Constant.id("none"), Optional.empty());
    }

    @Override
    public void handle(ServerPlayNetworking.@NotNull Context context) {
        ManualReaderAttachment current = context.player().getAttachedOrCreate(GCAttachments.MANUAL_READER);
        ManualReaderAttachment updated = switch (this.action) {
            case ACTION_MARK_READ -> current.withRead(this.entryId);
            case ACTION_TOGGLE_FAVORITE -> current.withFavoriteToggled(this.entryId);
            case ACTION_SET_PIN -> current.withPinnedMission(this.pinnedMission.orElse(null));
            case ACTION_CLEAR_PIN -> current.withPinnedMission(null);
            case ACTION_TOGGLE_PIN_COLLAPSE -> current.withPinCollapsedToggled();
            default -> current;
        };
        if (!updated.equals(current)) {
            context.player().setAttached(GCAttachments.MANUAL_READER, updated);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
