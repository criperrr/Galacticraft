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
import dev.galacticraft.mod.content.block.entity.machine.EnvironmentalTestChamberBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.GeologicalAnalyzerBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ObservatoryTelescopeBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ResearchDataLinkBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ResearchWorkbenchBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ZeroGResearchChamberBlockEntity;
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
 * C2S interactive control packet for all 6 planetary scientific laboratory benches.
 */
public record ResearchMachineActionPayload(
        @NotNull BlockPos pos,
        int actionType,
        int value
) implements C2SPayload {
    public static final int TELESCOPE_CYCLE_TARGET = 0;
    public static final int ENV_CHAMBER_CYCLE_MODE = 1;
    public static final int DATA_LINK_ADJUST_CHANNEL = 2;
    public static final int WORKBENCH_SEAL_PROTOTYPE = 3;
    public static final int GEO_CONFIRM_SPECTRUM = 4;
    public static final int ENV_CERTIFY_SAMPLE = 5;
    public static final int ZEROG_HARVEST_CRYSTAL = 6;
    public static final int DATA_LINK_LOCK_PHASE = 7;
    public static final int DATA_LINK_ADJUST_AIM = 8;

    public static final ResourceLocation ID = Constant.id("research_machine_action");
    public static final Type<ResearchMachineActionPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<ByteBuf, ResearchMachineActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ResearchMachineActionPayload::pos,
            ByteBufCodecs.VAR_INT, ResearchMachineActionPayload::actionType,
            ByteBufCodecs.VAR_INT, ResearchMachineActionPayload::value,
            ResearchMachineActionPayload::new
    );

    @Override
    public void handle(ServerPlayNetworking.@NotNull Context context) {
        ServerPlayer player = context.player();
        if (player.distanceToSqr(this.pos.getCenter()) > 100.0) {
            return;
        }
        BlockEntity be = player.serverLevel().getBlockEntity(this.pos);
        switch (this.actionType) {
            case TELESCOPE_CYCLE_TARGET -> {
                if (be instanceof ObservatoryTelescopeBlockEntity telescope) {
                    telescope.cycleTarget(this.value >= 0 ? 1 : -1);
                }
            }
            case ENV_CHAMBER_CYCLE_MODE -> {
                if (be instanceof EnvironmentalTestChamberBlockEntity chamber) {
                    chamber.cycleTestMode();
                }
            }
            case DATA_LINK_ADJUST_CHANNEL -> {
                if (be instanceof ResearchDataLinkBlockEntity link) {
                    link.adjustFrequencyChannel(this.value >= 0 ? 1 : -1);
                }
            }
            case WORKBENCH_SEAL_PROTOTYPE -> {
                if (be instanceof ResearchWorkbenchBlockEntity wb) {
                    wb.sealCalibratedPrototype(player.serverLevel(), this.value);
                }
            }
            case GEO_CONFIRM_SPECTRUM -> {
                if (be instanceof GeologicalAnalyzerBlockEntity geo) {
                    geo.confirmLaserSpectrum(player.serverLevel(), this.value);
                }
            }
            case ENV_CERTIFY_SAMPLE -> {
                if (be instanceof EnvironmentalTestChamberBlockEntity chamber) {
                    chamber.certifyControlledTest(player.serverLevel(), this.value);
                }
            }
            case ZEROG_HARVEST_CRYSTAL -> {
                if (be instanceof ZeroGResearchChamberBlockEntity zerog) {
                    zerog.harvestLevitatingCrystal(player.serverLevel(), this.value);
                }
            }
            case DATA_LINK_LOCK_PHASE -> {
                if (be instanceof ResearchDataLinkBlockEntity link) {
                    link.lockCarrierPhase(player.serverLevel(), this.value);
                }
            }
            case DATA_LINK_ADJUST_AIM -> {
                if (be instanceof ResearchDataLinkBlockEntity link) {
                    // Packed yaw/pitch delta: lower 16 bits = yaw delta, upper 16 bits = pitch delta
                    int dyaw = (short) (this.value & 0xFFFF);
                    int dpitch = (short) ((this.value >> 16) & 0xFFFF);
                    link.adjustDishAim(dyaw, dpitch);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
