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

import dev.galacticraft.api.vehicle.VehicleChassisDefinition;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * Bare modular vehicle chassis item placed onto the ground to begin physical part-by-part assembly.
 */
public class VehicleChassisItem extends Item {
    private final ResourceLocation chassisId;

    public VehicleChassisItem(@NotNull ResourceLocation chassisId, Properties properties) {
        super(properties.stacksTo(1));
        this.chassisId = chassisId;
    }

    public @NotNull ResourceLocation getChassisId() {
        return this.chassisId;
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
        vehicle.setPos(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
        vehicle.setYRot(context.getRotation());
        vehicle.setConfiguration(VehicleConfiguration.emptyChassis(this.chassisId));
        level.addFreshEntity(vehicle);
        level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.7F, 1.1F);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        VehicleChassisDefinition chassis = VehicleRegistry.getChassisOrDefault(this.chassisId);
        if (chassis.pressurizedCabin()) {
            tooltip.add(Component.translatable("tooltip.galacticraft.vehicle.pressurized").withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("tooltip.galacticraft.vehicle_chassis.hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.galacticraft.vehicle_crate.footprint",
                chassis.sizeClass().cargoWidth(),
                chassis.sizeClass().cargoHeight()
        ).withStyle(ChatFormatting.GOLD));
    }
}
