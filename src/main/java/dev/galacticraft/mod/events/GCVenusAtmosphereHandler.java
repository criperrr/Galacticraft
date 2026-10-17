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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

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
        boolean tick20 = (gameTime % 20 == 0);
        boolean tick40 = (gameTime % 40 == 0);
        boolean isRaining = level.isRaining();

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;

            BlockPos pos = player.blockPosition();
            boolean exposedToSky = level.canSeeSky(pos);
            boolean breathable = level.isBreathable(pos);
            boolean protectedPlayer = isProtected(player);

            // Supercritical Fluid Drag on the Surface (Y < 180)
            // Supercritical CO2 fluid has density ~65 kg/m3. Apply subtle fluid drag when moving on the ground.
            if (player.getY() < 180.0 && player.onGround() && !breathable) {
                Vec3 delta = player.getDeltaMovement();
                if (Math.abs(delta.x) > 0.001 || Math.abs(delta.z) > 0.001) {
                    player.setDeltaMovement(delta.x * 0.94, delta.y, delta.z * 0.94);
                    player.hurtMarked = true;
                }
                if (player.isSprinting()) {
                    player.causeFoodExhaustion(0.02F);
                }
            }

            // Cloud Layer Super-Rotation Jet Stream (Y >= 220)
            // At high altitudes when exposed to sky, apply westward (-X) wind drift acceleration simulating 360 km/h winds.
            if (player.getY() >= 220.0 && exposedToSky && !player.getAbilities().flying) {
                Vec3 delta = player.getDeltaMovement();
                double newX = Math.max(delta.x - 0.025, -0.6);
                player.setDeltaMovement(newX, delta.y, delta.z);
                player.hurtMarked = true;
            }

            // If player is protected (SHIELD_CONTROLLER or full ISOTHERMAL_PADDING),
            // they are completely immune to acid, heat, and crushing pressure!
            if (protectedPlayer) continue;

            // Ambient corrosive vapor at Y < 180 corrodes unshielded armor durability (1 dura every 40 ticks)
            if (tick40 && !breathable && player.getY() < 180.0) {
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    ItemStack armor = player.getItemBySlot(slot);
                    if (!armor.isEmpty() && armor.isDamageableItem()) {
                        armor.hurtAndBreak(1, player, slot);
                    }
                }
            }

            // 20-tick atmospheric hazards
            if (tick20) {
                // Hyperbaric Pressure System:
                // Surface/crust (Y < 200): If player is not protected, apply barometric crushing damage (2.0F)
                // every 20 ticks and slowness effect (MobEffects.MOVEMENT_SLOWDOWN).
                // Geoffrey Landis Cloud Layer (Y >= 200): Ambient pressure is 1.0 bar (Earth standard!).
                // NO crushing damage at Y >= 200!
                if (!breathable && player.getY() < 200.0) {
                    player.hurt(level.damageSources().generic(), 2.0F);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                }

                // Acid Virga vs Surface Vapor:
                // At Y >= 200: if raining and exposed to sky, direct sulfuric acid rain burns unshielded players (2.0F damage).
                if (player.getY() >= 200.0 && isRaining && exposedToSky) {
                    player.hurt(level.damageSources().source(GCDamageTypes.SULFURIC_ACID), 2.0F);
                }
            }
        }
    }
}
