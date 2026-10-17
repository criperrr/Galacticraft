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

package dev.galacticraft.mod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.block.environment.FallenMeteorBlock;
import dev.galacticraft.mod.content.entity.FallingMeteorEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import static dev.galacticraft.mod.content.GCBlocks.FALLEN_METEOR;

@Environment(EnvType.CLIENT)
public class FallingMeteorRenderer extends EntityRenderer<FallingMeteorEntity> {
    private static final ResourceLocation TEXTURE = Constant.id("textures/block/fallen_meteor.png");
    private static final BlockState METEOR_BLOCK_STATE = FALLEN_METEOR.defaultBlockState().setValue(FallenMeteorBlock.HEAT, 5);

    private final BlockRenderDispatcher dispatcher;

    public FallingMeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.0f;
        this.dispatcher = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(FallingMeteorEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        float scale = Math.max(0.85f, entity.getSize() * 0.55f);
        float yRot = Mth.lerp(tickDelta, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(tickDelta, entity.xRotO, entity.getXRot());

        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(yRot));
        matrices.mulPose(Axis.XP.rotationDegrees(xRot));
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5);

        this.dispatcher.renderSingleBlock(
                METEOR_BLOCK_STATE,
                matrices,
                vertexConsumers,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
        );

        matrices.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FallingMeteorEntity entity) {
        return TEXTURE;
    }
}
