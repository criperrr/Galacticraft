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

package dev.galacticraft.mod.events;

import dev.galacticraft.mod.content.entity.damage.GCDamageTypes;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class GCVenusAtmosphereHandler {
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static boolean isProtected(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return true;

        Container accessories = player.galacticraft$getAccessories();
        if (accessories != null) {
            for (int i = 0; i < accessories.getContainerSize(); i++) {
                if (accessories.getItem(i).is(GCItems.SHIELD_CONTROLLER)) {
                    return true;
                }
            }
        }

        Container thermal = player.galacticraft$getThermalArmor();
        if (thermal != null && thermal.getContainerSize() >= 4) {
            return thermal.getItem(0).is(GCItems.ISOTHERMAL_PADDING_HELMET)
                    && thermal.getItem(1).is(GCItems.ISOTHERMAL_PADDING_CHESTPIECE)
                    && thermal.getItem(2).is(GCItems.ISOTHERMAL_PADDING_LEGGINGS)
                    && thermal.getItem(3).is(GCItems.ISOTHERMAL_PADDING_BOOTS);
        }
        return false;
    }

    public static void onWorldTick(ServerLevel level) {
        if (!level.dimension().equals(GCDimensions.VENUS)) return;

        long gameTime = level.getGameTime();
        if (gameTime % 20 != 0) return;

        boolean isRaining = level.isRaining();

        for (ServerPlayer player : level.players()) {
            if (!isProtected(player)) {
                // Corrode equipped armor durability
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    ItemStack armor = player.getItemBySlot(slot);
                    if (!armor.isEmpty() && armor.isDamageableItem()) {
                        armor.hurtAndBreak(1, player, slot);
                    }
                }

                BlockPos pos = player.blockPosition();
                boolean exposedToSky = level.canSeeSky(pos);
                boolean breathable = level.isBreathable(pos);

                if (isRaining && exposedToSky) {
                    player.hurt(level.damageSources().source(GCDamageTypes.SULFURIC_ACID), 2.0F);
                } else if (!breathable) {
                    player.hurt(level.damageSources().source(GCDamageTypes.SULFURIC_ACID), 1.0F);
                }
            }
        }
    }
}
