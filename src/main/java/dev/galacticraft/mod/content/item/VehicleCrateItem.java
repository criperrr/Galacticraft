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
import dev.galacticraft.api.vehicle.SpatialCargoItem;
import dev.galacticraft.api.vehicle.VehicleChassisDefinition;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import dev.galacticraft.api.vehicle.VehicleDerivedStats;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.api.vehicle.VehicleSizeClass;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Packed vehicle shipping crate (hand-carriable and loadable into a rocket's
 * 2D spatial cargo grid, occupying {@code 2x2}, {@code 3x2}, or {@code 4x3} slots).
 */
public class VehicleCrateItem extends Item implements SpatialCargoItem {
    public VehicleCrateItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static @NotNull ItemStack createCrate(@NotNull VehicleConfiguration config) {
        ItemStack stack = new ItemStack(GCItems.VEHICLE_CRATE);
        stack.set(GCDataComponents.VEHICLE_CONFIGURATION, config);
        return stack;
    }

    public static @NotNull VehicleConfiguration getConfiguration(@NotNull ItemStack stack) {
        VehicleConfiguration cfg = stack.get(GCDataComponents.VEHICLE_CONFIGURATION);
        return cfg != null ? cfg : VehicleRegistry.createCompletePreset(VehicleRegistry.CHASSIS_SCOUT_ROVER);
    }

    @Override
    public int getCargoWidth(@NotNull ItemStack stack) {
        VehicleChassisDefinition chassis = VehicleRegistry.getChassisOrDefault(getConfiguration(stack).chassisId());
        return chassis.sizeClass().cargoWidth();
    }

    @Override
    public int getCargoHeight(@NotNull ItemStack stack) {
        VehicleChassisDefinition chassis = VehicleRegistry.getChassisOrDefault(getConfiguration(stack).chassisId());
        return chassis.sizeClass().cargoHeight();
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        ModularVehicleEntity vehicle = GCEntityTypes.MODULAR_VEHICLE.create(level);
        if (vehicle == null) {
            return InteractionResult.FAIL;
        }
        VehicleConfiguration config = getConfiguration(context.getItemInHand());
        vehicle.setPos(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
        vehicle.setYRot(context.getRotation());
        vehicle.setConfiguration(config);
        level.addFreshEntity(vehicle);
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.9F, 1.0F);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        VehicleConfiguration cfg = getConfiguration(stack);
        VehicleChassisDefinition chassis = VehicleRegistry.getChassisOrDefault(cfg.chassisId());
        VehicleDerivedStats stats = VehicleRegistry.computeStats(cfg);
        VehicleSizeClass size = chassis.sizeClass();

        tooltip.add(Component.translatable("tooltip.galacticraft.vehicle_crate.chassis", Component.translatable(chassis.translationKey()))
                .withStyle(ChatFormatting.AQUA));
        if (chassis.pressurizedCabin()) {
            tooltip.add(Component.translatable("tooltip.galacticraft.vehicle.pressurized")
                    .withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("tooltip.galacticraft.vehicle_crate.footprint", size.cargoWidth(), size.cargoHeight())
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.galacticraft.vehicle_crate.parts", cfg.installedParts().size(), chassis.hardpoints().size())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.galacticraft.vehicle_crate.mass", Math.round(stats.totalMassKg()))
                .withStyle(ChatFormatting.GRAY));
    }
}
