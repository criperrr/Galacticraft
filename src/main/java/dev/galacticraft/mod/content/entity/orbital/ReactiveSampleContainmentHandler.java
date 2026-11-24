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

package dev.galacticraft.mod.content.entity.orbital;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime containment handler for {@code science_sample_containment_bay}.
 * Reactive/unstable exotic samples ({@code METALLIC_HYDROGEN_CELL}, {@code SUPERIONIC_ICE_CRYSTAL},
 * {@code SULFURIC_ACID_BUCKET}) gradually destabilize and degrade in an unprotected player inventory
 * unless stabilized by a {@code science_sample_containment_bay} (carried or placed nearby) or a
 * {@link RocketMaterialTrait#MILLIKELVIN_CONTAINMENT} payload.
 */
public final class ReactiveSampleContainmentHandler {
    public static final int CHECK_INTERVAL_TICKS = 40;
    public static final int DEGRADATION_STRIKES_THRESHOLD = 15; // ~30s of uncontained exposure
    private static final Map<UUID, Integer> UNCONTAINED_STRIKES = new ConcurrentHashMap<>();

    private ReactiveSampleContainmentHandler() {
    }

    public static boolean isReactiveSample(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(GCItems.METALLIC_HYDROGEN_CELL)
                || stack.is(GCItems.SUPERIONIC_ICE_CRYSTAL)
                || stack.is(GCItems.SULFURIC_ACID_BUCKET);
    }

    /**
     * Returns {@code true} if the player has active containment protection via Creative/Spectator,
     * a carried {@code science_sample_containment_bay}, a {@link RocketMaterialTrait#MILLIKELVIN_CONTAINMENT}
     * payload, or a placed {@code science_sample_containment_bay} within 16 blocks.
     */
    public static boolean isProtected(@NotNull ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ModularRocketPartBlockItem partItem) {
                RocketPartConfig cfg = partItem.getConfig(stack);
                if (FlightTelemetryEngine.normalizeModuleId(cfg.familyId()).equals(FlightTelemetryEngine.MOD_SAMPLE_BAY)) {
                    return true;
                }
                RocketPartComposition comp = stack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
                if (comp != null && comp.hasTrait(RocketMaterialTrait.MILLIKELVIN_CONTAINMENT)) {
                    return true;
                }
            }
        }
        return hasNearbyPlacedContainmentBay(player.serverLevel(), player.blockPosition());
    }

    public static boolean hasNearbyPlacedContainmentBay(@NotNull ServerLevel level, @NotNull BlockPos center) {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx + dx, cz + dz).getBlockEntities().values()) {
                    if (be instanceof ModularRocketPartBlockEntity partBe
                            && !partBe.isRemoved()
                            && be.getBlockPos().distSqr(center) <= 256.0
                            && FlightTelemetryEngine.normalizeModuleId(partBe.getConfig().familyId()).equals(FlightTelemetryEngine.MOD_SAMPLE_BAY)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void tickWorld(@NotNull ServerLevel level) {
        if ((level.getGameTime() % CHECK_INTERVAL_TICKS) != 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            evaluatePlayerContainmentStep(player);
        }
    }

    /**
     * Evaluates one containment tick step for {@code player}.
     * Returns {@code true} if an uncontained reactive sample degraded on this step.
     */
    public static boolean evaluatePlayerContainmentStep(@NotNull ServerPlayer player) {
        int reactiveSlot = findFirstReactiveSampleSlot(player);
        if (reactiveSlot < 0 || isProtected(player)) {
            UNCONTAINED_STRIKES.remove(player.getUUID());
            return false;
        }
        int strikes = UNCONTAINED_STRIKES.merge(player.getUUID(), 1, Integer::sum);
        if (strikes == 1 || strikes == 5 || strikes == 10) {
            player.displayClientMessage(
                    Component.literal("[CONTAINMENT WARNING] Unstable sample degrading! Carry a Sample Containment Bay.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
        if (strikes >= DEGRADATION_STRIKES_THRESHOLD) {
            UNCONTAINED_STRIKES.put(player.getUUID(), 0);
            ItemStack sample = player.getInventory().getItem(reactiveSlot);
            String name = sample.getHoverName().getString();
            sample.shrink(1);
            player.displayClientMessage(
                    Component.literal("[CONTAINMENT LOST] 1x " + name + " destabilized without Sample Containment Bay!")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    true
            );
            return true;
        }
        return false;
    }

    public static void clearPlayer(@NotNull UUID playerId) {
        UNCONTAINED_STRIKES.remove(playerId);
    }

    private static int findFirstReactiveSampleSlot(@NotNull ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isReactiveSample(inv.getItem(i))) {
                return i;
            }
        }
        return -1;
    }
}
