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

package dev.galacticraft.mod.client.render.rocket.part;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.galacticraft.api.rocket.part.modular.FootprintMode;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * World-space coaxial ghost placement preview renderer for held {@link ModularRocketPartBlockItem} stacks.
 *
 * <p>Hooked into {@code WorldRenderEvents.AFTER_TRANSLUCENT}. Renders a translucent ghost mesh at the
 * resolved coaxial origin alongside a green (valid) or red (obstructed) wireframe bounding box.
 */
@Environment(EnvType.CLIENT)
public final class RocketPartPlacementPreviewRenderer {
    private static final float GHOST_ALPHA = 0.55F;

    private RocketPartPlacementPreviewRenderer() {
    }

    public static void render(WorldRenderContext context) {
        ClientLevel level = context.world();
        PoseStack poseStack = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        if (level == null || poseStack == null || consumers == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }

        InteractionHand hand;
        ItemStack heldStack;
        ModularRocketPartBlockItem partItem;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (mainHand.getItem() instanceof ModularRocketPartBlockItem mainPart) {
            hand = InteractionHand.MAIN_HAND;
            heldStack = mainHand;
            partItem = mainPart;
        } else if (offHand.getItem() instanceof ModularRocketPartBlockItem offPart) {
            hand = InteractionHand.OFF_HAND;
            heldStack = offHand;
            partItem = offPart;
        } else {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        RocketPartConfig config = partItem.getConfig(heldStack);
        BlockPlaceContext placeContext = new BlockPlaceContext(level, player, hand, heldStack, hit);
        Direction facing = ModularRocketPartBlockItem.resolvePlacementFacing(placeContext, config);
        BlockPos origin = ModularRocketPartBlockItem.resolvePlacementOrigin(level, placeContext, config, facing);
        boolean valid = ModularRocketPartBlockItem.canPlaceFootprint(level, origin, config, facing);

        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        RocketPartSpec spec = catalog.getPart(config.modelId());
        if (spec == null) {
            spec = catalog.defaultSpecFor(config.category());
        }

        RocketPartRuntimeModel model = spec != null ? RocketPartModelCache.INSTANCE.getOrLoad(spec.meshLocation()) : null;
        ResourceLocation texture = spec != null ? spec.textureFor(config.livery()) : null;

        Vec3 cameraPos = context.camera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Sub-pass 1: Translucent static ghost mesh submitted before acquiring RenderType.lines().
        if (model != null && texture != null) {
            float animTime = 0.0F;

            poseStack.pushPose();
            poseStack.translate(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            float yRotDeg = RocketPartMeshRenderer.facingToYRotDegrees(facing);
            if (yRotDeg != 0.0F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(yRotDeg));
            }

            float tintR = valid ? 0.85F : 1.0F;
            float tintG = valid ? 1.0F : 0.45F;
            float tintB = valid ? 0.90F : 0.45F;
            float shroudScaleY = "interstage_engine_shroud".equals(config.familyId()) ? 0.0F : 1.0F;
            RocketPartMeshRenderer.render(
                    model,
                    texture,
                    poseStack,
                    consumers,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    animTime,
                    false,
                    tintR,
                    tintG,
                    tintB,
                    GHOST_ALPHA,
                    shroudScaleY
            );

            // Preview automatic engine shroud when placing an adapter under an existing engine
            if (RocketPartShroudRenderer.supportsEngineShroud(config)) {
                ModularRocketPartBlockEntity aboveBe = resolvePartAt(level, origin.above(Math.max(1, config.heightBlocks())));
                if (aboveBe != null && RocketPartShroudRenderer.needsEngineShroud(aboveBe.getConfig())) {
                    RocketPartShroudRenderer.render(
                            config,
                            aboveBe.getConfig(),
                            texture,
                            poseStack,
                            consumers,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY,
                            tintR,
                            tintG,
                            tintB,
                            GHOST_ALPHA
                    );
                }
            } else if (RocketPartShroudRenderer.needsEngineShroud(config)) {
                // Preview automatic engine shroud when placing an engine on top of an existing adapter
                ModularRocketPartBlockEntity belowBe = resolvePartAt(level, origin.below());
                if (belowBe != null && RocketPartShroudRenderer.supportsEngineShroud(belowBe.getConfig())) {
                    RocketPartConfig lowerCfg = belowBe.getConfig();
                    RocketPartSpec lowerSpec = catalog.getPart(lowerCfg.modelId());
                    ResourceLocation lowerTex = lowerSpec != null ? lowerSpec.textureFor(lowerCfg.livery()) : texture;
                    poseStack.pushPose();
                    poseStack.translate(0.0F, -Math.max(1, lowerCfg.heightBlocks()), 0.0F);
                    RocketPartShroudRenderer.render(
                            lowerCfg,
                            config,
                            lowerTex,
                            poseStack,
                            consumers,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY,
                            tintR,
                            tintG,
                            tintB,
                            GHOST_ALPHA
                    );
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }

        // Sub-pass 2: Crisp green (valid) or red (invalid) wireframe bounding box.
        AABB localBounds = FootprintMode.computeBoundsAABB(config);
        AABB worldBounds = RocketPartMeshRenderer.rotateLocalBounds(localBounds, facing)
                .move(origin)
                .inflate(0.002);

        VertexConsumer lineConsumer = consumers.getBuffer(RenderType.lines());
        float lineR = valid ? 0.15F : 1.0F;
        float lineG = valid ? 0.95F : 0.20F;
        float lineB = valid ? 0.35F : 0.20F;
        float lineA = valid ? 0.85F : 0.95F;

        LevelRenderer.renderLineBox(
                poseStack,
                lineConsumer,
                worldBounds.minX,
                worldBounds.minY,
                worldBounds.minZ,
                worldBounds.maxX,
                worldBounds.maxY,
                worldBounds.maxZ,
                lineR,
                lineG,
                lineB,
                lineA
        );

        poseStack.popPose();
    }

    private static ModularRocketPartBlockEntity resolvePartAt(ClientLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
            return partBe;
        }
        BlockPos masterPos = MultiblockLinkManager.getMasterPos(level, pos);
        if (masterPos != null && level.getBlockEntity(masterPos) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
            return partBe;
        }
        return null;
    }
}
