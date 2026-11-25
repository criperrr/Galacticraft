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

package dev.galacticraft.mod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.galacticraft.mod.client.gui.screen.manual.AerospaceTabletAnimator;
import dev.galacticraft.mod.client.render.item.AerospaceTabletItemRenderer;
import dev.galacticraft.mod.content.entity.vehicle.AdvancedVehicle;
import dev.galacticraft.mod.content.item.AerospaceManualItem;
import dev.galacticraft.mod.content.item.RocketItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void gc$renderAerospaceTabletInHand(
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            CallbackInfo ci
    ) {
        boolean mainIsTablet = player.getMainHandItem().getItem() instanceof AerospaceManualItem;
        boolean offIsTablet = player.getOffhandItem().getItem() instanceof AerospaceManualItem;
        boolean animActive = AerospaceTabletAnimator.isAnimatingOrOpen();

        if (!mainIsTablet && !offIsTablet && !animActive) {
            return;
        }

        HumanoidArm mainArm = player.getMainArm();

        if (hand == InteractionHand.OFF_HAND) {
            // Only hide the off-hand item when the player is actively opening the tablet with both hands!
            if (animActive) {
                ci.cancel();
                return;
            }
            if (offIsTablet && !mainIsTablet) {
                AerospaceTabletItemRenderer.INSTANCE.renderFirstPersonTablet(
                        player, mainArm.getOpposite(), partialTicks, equippedProgress, swingProgress, poseStack, buffer, combinedLight
                );
                ci.cancel();
            }
            return;
        }

        // Main hand: render one-handed when idle, transitioning to two-handed when animActive
        if (mainIsTablet || animActive) {
            HumanoidArm primaryArm = (!mainIsTablet && offIsTablet) ? mainArm.getOpposite() : mainArm;
            AerospaceTabletItemRenderer.INSTANCE.renderFirstPersonTablet(
                    player, primaryArm, partialTicks, equippedProgress, swingProgress, poseStack, buffer, combinedLight
            );
            ci.cancel();
        }
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void gc$hideRocketItem(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, boolean bl, PoseStack poseStack, MultiBufferSource multiBufferSource, int n, CallbackInfo ci) {
        if (itemStack.getItem() instanceof RocketItem) {
            if (livingEntity.getVehicle() instanceof AdvancedVehicle) {
                ci.cancel();
                return;
            }

            // If there is a rocket in both hands, only render the one in the off-hand
            switch (itemDisplayContext) {
                case THIRD_PERSON_RIGHT_HAND:
                case FIRST_PERSON_RIGHT_HAND: {
                    if (livingEntity.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof RocketItem) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }
}
