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
import com.mojang.math.Axis;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * BlockEntity renderer for world-placed {@link ModularRocketPartBlockEntity} instances.
 *
 * <p>Lazily fetches the baked {@code .gcrm} binary mesh from {@link RocketPartModelCache} and binds
 * the selected KSP hull livery texture directly via standard {@link net.minecraft.client.renderer.RenderType}
 * buffers (never stitching part textures into the block atlas), with distance-based LOD culling at 48 blocks.
 */
@Environment(EnvType.CLIENT)
public class ModularRocketPartBlockEntityRenderer implements BlockEntityRenderer<ModularRocketPartBlockEntity> {
    private static final int VIEW_DISTANCE = 192;
    private static final double LOD_DISTANCE_SQ = 48.0 * 48.0;

    public ModularRocketPartBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(ModularRocketPartBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public void render(
            ModularRocketPartBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        RocketPartConfig config = blockEntity.getConfig();
        if (config == null) {
            return;
        }

        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        RocketPartSpec spec = catalog.getPart(config.modelId());
        if (spec == null) {
            spec = catalog.defaultSpecFor(config.category());
        }
        if (spec == null) {
            return;
        }

        RocketPartRuntimeModel model = RocketPartModelCache.INSTANCE.getOrLoad(spec.meshLocation());
        if (model == null) {
            return;
        }

        ResourceLocation texture = spec.textureFor(config.livery());
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();

        int light = resolveConnectedStackLight(level, pos, config, packedLight);

        float animTime = 0.0F;
        float duration = model.duration();
        if (blockEntity.isAnimationActive() && level != null && duration > 0.0F && model.hasAnimations()) {
            long elapsedTicks = Math.max(0L, level.getGameTime() - blockEntity.getAnimationStartTick());
            double seconds = ((double) elapsedTicks + (double) partialTick) / 20.0;
            animTime = (float) (seconds % (double) duration);
            if (animTime < 0.0F) {
                animTime += duration;
            }
        }

        boolean useLod = false;
        if (model.hasLodDetailBones()) {
            Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            double dx = (pos.getX() + 0.5) - cameraPos.x;
            double dy = (pos.getY() + config.heightBlocks() * 0.5) - cameraPos.y;
            double dz = (pos.getZ() + 0.5) - cameraPos.z;
            useLod = (dx * dx + dy * dy + dz * dz) > LOD_DISTANCE_SQ;
        }

        poseStack.pushPose();
        poseStack.translate(
                0.5F + blockEntity.getRadialVisualOffsetX(),
                0.0F,
                0.5F + blockEntity.getRadialVisualOffsetZ()
        );
        float yRotDeg = RocketPartMeshRenderer.facingToYRotDegrees(blockEntity.getFacing());
        if (yRotDeg != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotDeg));
        }

        RocketPartConfig engineAbove = resolveEngineAboveForShroud(level, pos, config);
        // Hide the baked interstage_engine_shroud sleeve bone so our 1:1 procedural fairing renders without vertical texture stretching
        float shroudSleeveScaleY = "interstage_engine_shroud".equals(config.familyId()) ? 0.0F : 1.0F;

        RocketPartMeshRenderer.render(
                model,
                texture,
                poseStack,
                bufferSource,
                light,
                packedOverlay,
                animTime,
                useLod,
                1.0F,
                1.0F,
                1.0F,
                1.0F,
                shroudSleeveScaleY
        );

        if (engineAbove != null) {
            RocketPartShroudRenderer.render(
                    config,
                    engineAbove,
                    texture,
                    poseStack,
                    bufferSource,
                    light,
                    packedOverlay,
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }

        poseStack.popPose();
    }

    /**
     * Checks whether this adapter/interstage/decoupler sits directly beneath an exposed rocket engine
     * and returns that engine's {@link RocketPartConfig} so a seamless protective shroud can be generated.
     */
    private static RocketPartConfig resolveEngineAboveForShroud(Level level, BlockPos pos, RocketPartConfig config) {
        if (level == null || !RocketPartShroudRenderer.supportsEngineShroud(config)) {
            return null;
        }
        BlockPos abovePos = pos.above(Math.max(1, config.heightBlocks()));
        ModularRocketPartBlockEntity aboveBe = resolvePartAt(level, abovePos);
        if (aboveBe != null && RocketPartShroudRenderer.needsEngineShroud(aboveBe.getConfig())) {
            return aboveBe.getConfig();
        }
        return null;
    }

    private static ModularRocketPartBlockEntity resolvePartAt(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
            return partBe;
        }
        BlockPos masterPos = MultiblockLinkManager.getMasterPos(level, pos);
        if (masterPos != null && level.getBlockEntity(masterPos) instanceof ModularRocketPartBlockEntity partBe && !partBe.isRemoved()) {
            return partBe;
        }
        return null;
    }

    /**
     * Computes seamless, shadow-seam-free world lighting across coaxially stacked rocket parts by
     * sampling exterior perimeter air cells around the rocket hull rather than dark interior faces.
     */
    private static int resolveConnectedStackLight(Level level, BlockPos pos, RocketPartConfig config, int fallbackLight) {
        if (level == null) {
            return fallbackLight;
        }
        int blockLight = LightTexture.block(fallbackLight);
        int skyLight = LightTexture.sky(fallbackLight);

        // Walk up to the top of any coaxial stack (capped to 16 steps) so stacked modules share smooth lighting
        BlockPos sampleBase = pos;
        int maxRadius = Math.max(1, (Math.max(config.diameter(), config.topDiameter()) / 2) + 1);
        int topY = pos.getY() + Math.max(1, config.heightBlocks());
        BlockPos cursor = pos.above(Math.max(1, config.heightBlocks()));
        for (int step = 0; step < 16; step++) {
            ModularRocketPartBlockEntity above = resolvePartAt(level, cursor);
            if (above == null || above.getConfig() == null || above.getConfig().radial()) {
                break;
            }
            int h = Math.max(1, above.getConfig().heightBlocks());
            topY = above.getBlockPos().getY() + h;
            maxRadius = Math.max(maxRadius, (Math.max(above.getConfig().diameter(), above.getConfig().topDiameter()) / 2) + 1);
            cursor = above.getBlockPos().above(h);
        }

        int midY = sampleBase.getY() + Math.max(1, config.heightBlocks()) / 2;
        BlockPos[] samplePoints = {
                sampleBase,
                new BlockPos(sampleBase.getX(), topY, sampleBase.getZ()),
                new BlockPos(sampleBase.getX() + maxRadius, midY, sampleBase.getZ()),
                new BlockPos(sampleBase.getX() - maxRadius, midY, sampleBase.getZ()),
                new BlockPos(sampleBase.getX(), midY, sampleBase.getZ() + maxRadius),
                new BlockPos(sampleBase.getX(), midY, sampleBase.getZ() - maxRadius)
        };
        for (BlockPos sp : samplePoints) {
            int sampled = LevelRenderer.getLightColor(level, sp);
            blockLight = Math.max(blockLight, LightTexture.block(sampled));
            skyLight = Math.max(skyLight, LightTexture.sky(sampled));
        }
        return LightTexture.pack(blockLight, skyLight);
    }
}
