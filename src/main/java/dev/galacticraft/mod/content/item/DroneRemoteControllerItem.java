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

package dev.galacticraft.mod.content.item;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.network.c2s.DroneRemoteControlPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Handheld radio flight controller for {@code SMALL} unmanned electric rotorcraft drones.
 * Right-click a Micro-Drone to pair its UUID, then right-click in the air to fly remotely
 * with toggleable 1st-person FPV gimbal view or 3rd-person chase view.
 */
public class DroneRemoteControllerItem extends Item {
    public DroneRemoteControllerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void linkDrone(@NotNull ItemStack stack, @NotNull ModularVehicleEntity drone) {
        stack.set(GCDataComponents.LINKED_DRONE_UUID, drone.getUUID().toString());
    }

    public static @Nullable UUID getLinkedDroneUuid(@NotNull ItemStack stack) {
        String raw = stack.get(GCDataComponents.LINKED_DRONE_UUID);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static @Nullable ModularVehicleEntity findLinkedDrone(@NotNull Level level, @NotNull Player player, @NotNull ItemStack stack) {
        UUID targetUuid = getLinkedDroneUuid(stack);
        if (targetUuid == null) {
            return null;
        }
        double range = DroneRemoteControlPayload.MAX_RANGE_BLOCKS;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<ModularVehicleEntity> candidates = level.getEntitiesOfClass(
                ModularVehicleEntity.class,
                searchBox,
                v -> !v.isRemoved() && v.isRemoteControllableDrone() && targetUuid.equals(v.getUUID())
        );
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        UUID linked = getLinkedDroneUuid(stack);
        if (linked == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.galacticraft.drone_remote.not_linked").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.pass(stack);
        }

        ModularVehicleEntity drone = findLinkedDrone(level, player, stack);
        if (drone == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.galacticraft.drone_remote.out_of_range").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            dev.galacticraft.mod.client.gui.screen.ingame.VehicleClientBridge.enterDroneRemote(drone);
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getLinkedDroneUuid(stack) != null || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UUID linked = getLinkedDroneUuid(stack);
        if (linked != null) {
            String shortId = linked.toString().substring(0, 8).toUpperCase();
            tooltip.add(Component.translatable("tooltip.galacticraft.drone_remote.linked", shortId).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.galacticraft.drone_remote.unlinked").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.galacticraft.drone_remote.pair_hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.galacticraft.drone_remote.fly_hint").withStyle(ChatFormatting.AQUA));
    }
}
