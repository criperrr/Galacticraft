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

package dev.galacticraft.mod.client.render.entity.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.galacticraft.api.rocket.assembly.MountedPartNode;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartMeshRenderer;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartModelCache;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartRuntimeModel;
import dev.galacticraft.mod.content.entity.vehicle.DetachedStageEntity;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.physics.PropellantType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sodium / ImmediatelyFast / Iris compatible renderer for {@link DetachedStageEntity}, reusing
 * {@link RocketPartMeshRenderer} for Korolev Cross radial boosters, spent lower stages, and
 * ejected aerodynamic fairing halves.
 */
@Environment(EnvType.CLIENT)
public class DetachedStageEntityRenderer extends EntityRenderer<DetachedStageEntity> {
    private static final Map<Integer, List<CachedPart>> PART_CACHE = new ConcurrentHashMap<>();

    static {
        DetachedStageEntity.CLIENT_REMOVAL_HOOK = PART_CACHE::remove;
    }

    public DetachedStageEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull DetachedStageEntity entity, @NotNull Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(
            @NotNull DetachedStageEntity entity,
            float entityYaw,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource bufferSource,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        RocketAssemblyGraph graph = entity.getStageGraph();
        if (graph.isEmpty()) {
            return;
        }

        List<CachedPart> parts = PART_CACHE.computeIfAbsent(entity.getId(), id -> buildParts(graph));
        if (parts.isEmpty()) {
            return;
        }

        float pitch = entity.getInterpolatedPitchDeg(partialTick);
        float yaw = entity.getInterpolatedYawDeg(partialTick);
        float roll = entity.getInterpolatedRollDeg(partialTick);
        float timeSec = ((float) entity.tickCount + partialTick) / 20.0F;

        Level level = entity.level();
        int sampledLight = packedLight;
        if (level != null) {
            BlockPos samplePos = BlockPos.containing(entity.getX(), entity.getY() + graph.totalHeightBlocks() * 0.5, entity.getZ());
            if (level.isInWorldBounds(samplePos)) {
                int s = LevelRenderer.getLightColor(level, samplePos);
                sampledLight = LightTexture.pack(
                        Math.max(LightTexture.block(packedLight), LightTexture.block(s)),
                        Math.max(LightTexture.sky(packedLight), LightTexture.sky(s))
                );
            }
        }

        poseStack.pushPose();
        Quaternionf rot = ModularRocketFlightEntity.buildAttitudeQuaternion(yaw, pitch, roll);
        poseStack.mulPose(rot);

        for (CachedPart part : parts) {
            RocketPartRuntimeModel model = RocketPartModelCache.INSTANCE.getOrLoad(part.meshLocation);
            if (model == null) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(part.relX, part.relY, part.relZ);
            RocketPartMeshRenderer.render(
                    model,
                    part.texture,
                    poseStack,
                    bufferSource,
                    sampledLight,
                    OverlayTexture.NO_OVERLAY,
                    0.0F,
                    false,
                    1.0F
            );
            poseStack.popPose();
        }

        if (entity.isRetroFiring()) {
            ModularRocketFlightEntityRenderer.renderExhaustPlume(
                    poseStack,
                    bufferSource,
                    PropellantType.RP1,
                    Math.max(0.45F, (float) graph.maxRadiusBlocks() * 0.55F),
                    3.2F,
                    timeSec,
                    1.0F
            );
        }

        if (entity.isParachuteOpen()) {
            ModularRocketFlightEntityRenderer.renderParachuteCanopy(
                    poseStack,
                    bufferSource,
                    Math.max(2.0F, graph.totalHeightBlocks() + 2.5F),
                    Math.max(1.8F, (float) graph.maxRadiusBlocks() * 1.9F),
                    sampledLight
            );
        }

        poseStack.popPose();
    }

    private static List<CachedPart> buildParts(RocketAssemblyGraph graph) {
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        List<MountedPartNode> nodes = graph.allActiveParts();
        List<CachedPart> out = new ArrayList<>(nodes.size());
        int minY = Integer.MAX_VALUE;
        for (MountedPartNode node : nodes) {
            minY = Math.min(minY, node.relativePos().getY());
        }
        if (minY == Integer.MAX_VALUE) {
            minY = 0;
        }
        for (MountedPartNode node : nodes) {
            RocketPartConfig cfg = node.config();
            RocketPartSpec spec = catalog.getPart(cfg.modelId());
            if (spec == null) {
                spec = catalog.defaultSpecFor(cfg.category());
            }
            if (spec == null) {
                continue;
            }
            out.add(new CachedPart(
                    spec.meshLocation(),
                    spec.textureFor(cfg.livery()),
                    node.relativePos().getX(),
                    node.relativePos().getY() - minY,
                    node.relativePos().getZ()
            ));
        }
        return List.copyOf(out);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DetachedStageEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private record CachedPart(ResourceLocation meshLocation, ResourceLocation texture, float relX, float relY, float relZ) {
    }
}
