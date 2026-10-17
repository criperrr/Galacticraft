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

import dev.galacticraft.mod.content.entity.GrappleHookEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GrappleHookItem extends Item {
    public GrappleHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        boolean isCreative = player.getAbilities().instabuild;
        boolean hasString = isCreative || player.getInventory().contains(new ItemStack(Items.STRING));

        if (!hasString) {
            return InteractionResultHolder.fail(held);
        }

        player.getCooldowns().addCooldown(this, 20);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);

        if (!level.isClientSide) {
            for (GrappleHookEntity existing : level.getEntitiesOfClass(GrappleHookEntity.class, player.getBoundingBox().inflate(128.0D), e -> e.getOwner() == player)) {
                existing.discard();
            }

            if (!isCreative) {
                // Consume 1 string from inventory
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.is(Items.STRING)) {
                        stack.shrink(1);
                        break;
                    }
                }
            }

            GrappleHookEntity grapple = new GrappleHookEntity(player, level);
            Vec3 look = player.getLookAngle();
            grapple.shoot(look.x, look.y, look.z, 2.0F, 0.5F);
            level.addFreshEntity(grapple);

            held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }

        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
