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
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.galacticraft.api.rocket.assembly.MountedPartNode;
import dev.galacticraft.api.rocket.assembly.RadialBoosterGroup;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartMeshRenderer;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartModelCache;
import dev.galacticraft.mod.client.render.rocket.part.RocketPartRuntimeModel;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.physics.ModularFlightPhysics;
import dev.galacticraft.mod.content.rocket.physics.PropellantType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 100% Sodium / ImmediatelyFast / Iris compatible entity renderer for {@link ModularRocketFlightEntity}.
 *
 * <p>Uses {@link RocketPartMeshRenderer} exclusively via standard {@link MultiBufferSource}
 * {@link RenderType} buffers (never calling raw {@code VertexBuffer.draw()}). Caches resolved
 * per-stage part descriptors per entity ID (invalidated only on staging/fairing changes and freed
 * on {@code Entity#onClientRemoval()}), samples multi-point world lightmaps per stage elevation,
 * animates TVC nozzle gimbal, renders chemistry-coded volumetric exhaust plumes, hot-staging
 * radial lattice vents, and a transonic Max-Q Mach condensation cone.
 */
@Environment(EnvType.CLIENT)
public class ModularRocketFlightEntityRenderer extends EntityRenderer<ModularRocketFlightEntity> {
    private static final double LOD_DISTANCE_SQ = 72.0 * 72.0;
    private static final ResourceLocation WHITE_TEX = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final Map<Integer, CachedRocketRenderPlan> STAGE_CACHE = new ConcurrentHashMap<>();

    static {
        ModularRocketFlightEntity.CLIENT_REMOVAL_HOOK = STAGE_CACHE::remove;
    }

    public ModularRocketFlightEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull ModularRocketFlightEntity entity, @NotNull Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(
            @NotNull ModularRocketFlightEntity entity,
            float entityYaw,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource bufferSource,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        RocketAssemblyGraph graph = entity.getAssemblyGraph();
        if (graph.isEmpty()) {
            return;
        }

        CachedRocketRenderPlan plan = resolvePlan(entity.getId(), graph);
        if (plan.stages.isEmpty()) {
            return;
        }

        Level level = entity.level();
        float timeSec = ((float) entity.tickCount + partialTick) / 20.0F;
        float pitch = entity.getInterpolatedPitchDeg(partialTick);
        float yaw = entity.getInterpolatedYawDeg(partialTick);
        float roll = entity.getInterpolatedRollDeg(partialTick);
        float tvcPitch = entity.getTvcPitchDeg();
        float tvcYaw = entity.getTvcYawDeg();
        float throttle = entity.getThrottle();
        float shake = entity.getCameraShake();
        ModularRocketFlightEntity.FlightPhase phase = entity.getFlightPhase();

        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distSq = entity.position().distanceToSqr(camPos);
        boolean useLod = distSq > LOD_DISTANCE_SQ;

        poseStack.pushPose();

        // Subtle high-frequency rocket structural vibration during thrust/Max-Q
        if (shake > 0.02F) {
            float vib = shake * 0.045F;
            float sx = Mth.sin(timeSec * 67.0F) * vib;
            float sz = Mth.cos(timeSec * 73.0F) * vib;
            poseStack.translate(sx, 0.0F, sz);
        }

        // Apply spacecraft attitude quaternion around base
        Quaternionf attitude = ModularRocketFlightEntity.buildAttitudeQuaternion(yaw, pitch, roll);
        poseStack.mulPose(attitude);

        // Shift so lowest active stage rests at local Y = 0
        float baseYOffset = -plan.minPartY;
        float yRotFacing = RocketPartMeshRenderer.facingToYRotDegrees(entity.getPadFacing());
        if (yRotFacing != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotFacing));
        }

        int activeStageIndex = graph.activeStage() != null ? graph.activeStage().stageIndex() : -1;
        boolean enginesFiring = (phase == ModularRocketFlightEntity.FlightPhase.ASCENT
                || phase == ModularRocketFlightEntity.FlightPhase.HOT_STAGING
                || phase == ModularRocketFlightEntity.FlightPhase.RETRO_DESCENT)
                && throttle > 0.02F;

        // Render each stage with its own multi-point sampled lightmap (prevents dark noses on tall rockets!)
        for (CachedStageRender stage : plan.stages) {
            int stageLight = sampleStageLightmap(level, entity, stage.midY + baseYOffset, packedLight);
            boolean isActiveStage = stage.stageIndex == activeStageIndex;
            boolean isHotStageUpper = phase == ModularRocketFlightEntity.FlightPhase.HOT_STAGING
                    && stage.stageIndex == activeStageIndex + 1;

            for (CachedPartRender part : stage.parts) {
                RocketPartRuntimeModel model = RocketPartModelCache.INSTANCE.getOrLoad(part.meshLocation);
                if (model == null) {
                    continue;
                }
                float px = part.relX;
                float py = part.relY + baseYOffset;
                float pz = part.relZ;

                poseStack.pushPose();
                poseStack.translate(px, py, pz);

                // Dynamic TVC gimbal deflection on active stage engines
                if (part.isEngine && isActiveStage && enginesFiring && (Math.abs(tvcPitch) > 0.05F || Math.abs(tvcYaw) > 0.05F)) {
                    float pivotY = Math.max(0.5F, part.heightBlocks * 0.75F);
                    poseStack.translate(0.0F, pivotY, 0.0F);
                    poseStack.mulPose(Axis.XP.rotationDegrees(Mth.clamp(tvcPitch, -12.0F, 12.0F)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.clamp(tvcYaw, -12.0F, 12.0F)));
                    poseStack.translate(0.0F, -pivotY, 0.0F);
                }

                float animTime = 0.0F;
                if (part.isLandingGear && entity.isLandingGearDeployed()) {
                    animTime = model.duration() * 0.5F;
                } else if (part.isEngine && isActiveStage && enginesFiring) {
                    animTime = timeSec;
                }

                RocketPartMeshRenderer.render(
                        model,
                        part.texture,
                        poseStack,
                        bufferSource,
                        stageLight,
                        OverlayTexture.NO_OVERLAY,
                        animTime,
                        useLod,
                        1.0F
                );

                // Chemistry-coded volumetric exhaust plume beneath firing engines
                if (part.isEngine && ((isActiveStage && enginesFiring) || isHotStageUpper)) {
                    float plumePower = isHotStageUpper ? 0.65F : (part.propellant == PropellantType.SOLID ? 1.0F : throttle);
                    renderExhaustPlume(
                            poseStack,
                            bufferSource,
                            part.propellant,
                            Math.max(0.45F, part.diameter * 0.36F),
                            Math.max(2.2F, part.diameter * 1.9F * (0.55F + 0.65F * plumePower)),
                            timeSec,
                            entity.getAmbientPressureAtm()
                    );
                }

                // Hot-staging radial exhaust jets venting out of lattice interstage ports
                if (phase == ModularRocketFlightEntity.FlightPhase.HOT_STAGING && part.isHotStagingRing) {
                    renderHotStagingVents(poseStack, bufferSource, Math.max(0.8F, part.diameter * 0.5F), timeSec);
                }

                poseStack.popPose();
            }
        }

        // LES abort escape plume above capsule
        if (phase == ModularRocketFlightEntity.FlightPhase.LES_ABORT && throttle > 0.05F) {
            float topY = Math.max(2.0F, entity.getTotalHeightBlocks());
            poseStack.pushPose();
            poseStack.translate(0.0F, topY - 0.5F, 0.0F);
            renderExhaustPlume(poseStack, bufferSource, PropellantType.SOLID, 0.55F, 3.8F, timeSec, entity.getAmbientPressureAtm());
            poseStack.popPose();
        }

        // Transonic Max-Q Mach condensation cone around upper vehicle
        float qPa = entity.getDynamicPressurePa();
        if (qPa > 9_000.0F && entity.getAmbientPressureAtm() > 0.08F && phase == ModularRocketFlightEntity.FlightPhase.ASCENT) {
            float alpha = Mth.clamp((qPa - 9_000.0F) / 22_000.0F, 0.0F, 0.42F);
            renderMachCone(poseStack, bufferSource, entity.getTotalHeightBlocks() * 0.72F, Math.max(1.4F, entity.getMaxRadiusBlocks() * 1.45F), alpha);
        }

        // Deployed recovery/LES parachute canopy above capsule
        if (entity.isParachutesDeployed()) {
            renderParachuteCanopy(poseStack, bufferSource, entity.getTotalHeightBlocks() + 3.2F, Math.max(2.2F, entity.getMaxRadiusBlocks() * 2.1F), packedLight);
        }

        poseStack.popPose();
    }

    private static int sampleStageLightmap(Level level, ModularRocketFlightEntity entity, float localStageY, int fallback) {
        if (level == null) {
            return fallback;
        }
        BlockPos samplePos = BlockPos.containing(entity.getX(), entity.getY() + localStageY, entity.getZ());
        if (!level.isInWorldBounds(samplePos)) {
            return LightTexture.FULL_BRIGHT;
        }
        int sampled = LevelRenderer.getLightColor(level, samplePos);
        int block = Math.max(LightTexture.block(fallback), LightTexture.block(sampled));
        int sky = Math.max(LightTexture.sky(fallback), LightTexture.sky(sampled));
        return LightTexture.pack(block, sky);
    }

    /**
     * Renders a chemistry-coded multi-layer volumetric exhaust cone with Mach shock diamonds
     * strictly through {@link MultiBufferSource#getBuffer(RenderType)} at {@code FULL_BRIGHT}.
     */
    public static void renderExhaustPlume(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            PropellantType propellant,
            float baseRadius,
            float length,
            float timeSec,
            float ambientPressureAtm
    ) {
        // Plume expands wider in vacuum (under-expansion) and stays tighter at sea level
        float vacExpand = 1.0F + Mth.clamp(1.0F - ambientPressureAtm, 0.0F, 1.0F) * 0.65F;
        float flicker = 0.92F + 0.08F * Mth.sin(timeSec * 48.0F);
        float effLen = length * flicker;
        float tipRadius = baseRadius * 0.35F * vacExpand;

        int[] outerRgba = outerPlumeColor(propellant);
        int[] coreRgba = corePlumeColor(propellant);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
        Matrix4f mat = poseStack.last().pose();

        // Outer luminous exhaust sheath (8-sided tapered cone pointing -Y)
        emitPlumeCone(vc, mat, baseRadius, tipRadius, -effLen, outerRgba[0], outerRgba[1], outerRgba[2], outerRgba[3]);
        // Inner hot core with shock diamond pinch
        emitPlumeCone(vc, mat, baseRadius * 0.58F, baseRadius * 0.14F, -effLen * 0.68F, coreRgba[0], coreRgba[1], coreRgba[2], coreRgba[3]);
    }

    private static void renderHotStagingVents(PoseStack poseStack, MultiBufferSource bufferSource, float radius, float timeSec) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
        float pulse = 0.85F + 0.15F * Mth.sin(timeSec * 55.0F);
        float jetLen = radius * 1.4F * pulse;
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 90.0F));
            poseStack.translate(0.0F, 0.5F, radius);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            emitPlumeCone(vc, poseStack.last().pose(), 0.25F, 0.55F, -jetLen, 255, 190, 90, 195);
            poseStack.popPose();
        }
    }

    private static void renderMachCone(PoseStack poseStack, MultiBufferSource bufferSource, float yPos, float radius, float alpha) {
        int a = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        if (a <= 4) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, yPos, 0.0F);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEX));
        emitPlumeCone(vc, poseStack.last().pose(), radius * 0.35F, radius * 1.35F, -radius * 1.15F, 225, 244, 255, a);
        poseStack.popPose();
    }

    public static void renderParachuteCanopy(PoseStack poseStack, MultiBufferSource bufferSource, float canopyY, float radius, int packedLight) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(WHITE_TEX));
        poseStack.pushPose();
        poseStack.translate(0.0F, canopyY, 0.0F);
        Matrix4f mat = poseStack.last().pose();
        int segs = 8;
        float domeH = radius * 0.55F;
        for (int i = 0; i < segs; i++) {
            double a0 = (Math.PI * 2.0 * i) / segs;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segs;
            float x0 = (float) (Math.cos(a0) * radius);
            float z0 = (float) (Math.sin(a0) * radius);
            float x1 = (float) (Math.cos(a1) * radius);
            float z1 = (float) (Math.sin(a1) * radius);
            boolean orangePanel = (i % 2) == 0;
            int r = orangePanel ? 245 : 240;
            int g = orangePanel ? 118 : 240;
            int b = orangePanel ? 38 : 242;
            emitQuad(vc, mat, x0, 0.0F, z0, x1, 0.0F, z1, x1 * 0.25F, domeH, z1 * 0.25F, x0 * 0.25F, domeH, z0 * 0.25F,
                    r, g, b, 255, packedLight);
        }
        poseStack.popPose();
    }

    private static void emitPlumeCone(
            VertexConsumer vc,
            Matrix4f mat,
            float topR,
            float botR,
            float endY,
            int r,
            int g,
            int b,
            int a
    ) {
        int segs = 8;
        for (int i = 0; i < segs; i++) {
            double a0 = (Math.PI * 2.0 * i) / segs;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segs;
            float tx0 = (float) (Math.cos(a0) * topR);
            float tz0 = (float) (Math.sin(a0) * topR);
            float tx1 = (float) (Math.cos(a1) * topR);
            float tz1 = (float) (Math.sin(a1) * topR);
            float bx0 = (float) (Math.cos(a0) * botR);
            float bz0 = (float) (Math.sin(a0) * botR);
            float bx1 = (float) (Math.cos(a1) * botR);
            float bz1 = (float) (Math.sin(a1) * botR);
            emitQuad(vc, mat, tx0, 0.0F, tz0, tx1, 0.0F, tz1, bx1, endY, bz1, bx0, endY, bz0, r, g, b, a, LightTexture.FULL_BRIGHT);
        }
    }

    private static void emitQuad(
            VertexConsumer vc,
            Matrix4f mat,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            int r, int g, int b, int a,
            int light
    ) {
        vc.addVertex(mat, x0, y0, z0).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(mat, x1, y1, z1).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(mat, x2, y2, z2).setColor(r, g, b, Math.max(0, a / 3)).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(mat, x3, y3, z3).setColor(r, g, b, Math.max(0, a / 3)).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
    }

    private static int[] outerPlumeColor(PropellantType type) {
        return switch (type) {
            case RP1 -> new int[]{255, 165, 45, 200};
            case METHALOX -> new int[]{115, 145, 255, 195};
            case HYDROLOX -> new int[]{160, 230, 255, 155};
            case SOLID -> new int[]{255, 232, 160, 225};
            case NERVA -> new int[]{85, 240, 255, 205};
            case XENON_ELECTRIC -> new int[]{185, 105, 255, 185};
            case MONOPROPELLANT -> new int[]{255, 140, 80, 170};
        };
    }

    private static int[] corePlumeColor(PropellantType type) {
        return switch (type) {
            case RP1 -> new int[]{255, 245, 195, 240};
            case METHALOX -> new int[]{215, 235, 255, 240};
            case HYDROLOX -> new int[]{235, 250, 255, 210};
            case SOLID -> new int[]{255, 255, 240, 250};
            case NERVA -> new int[]{205, 255, 255, 245};
            case XENON_ELECTRIC -> new int[]{235, 195, 255, 235};
            case MONOPROPELLANT -> new int[]{255, 220, 170, 220};
        };
    }

    private static CachedRocketRenderPlan resolvePlan(int entityId, RocketAssemblyGraph graph) {
        int signature = computeGraphSignature(graph);
        CachedRocketRenderPlan existing = STAGE_CACHE.get(entityId);
        if (existing != null && existing.signature == signature) {
            return existing;
        }
        CachedRocketRenderPlan built = buildPlan(signature, graph);
        STAGE_CACHE.put(entityId, built);
        return built;
    }

    private static int computeGraphSignature(RocketAssemblyGraph graph) {
        int h = graph.totalHeightBlocks() * 31 + (graph.hasFairing() ? 1 : 0);
        for (RocketStageNode stage : graph.stages()) {
            h = 31 * h + (stage.decoupled() ? 0 : (stage.coreParts().size() * 17 + stage.stageIndex()));
            for (RadialBoosterGroup g : stage.radialGroups()) {
                h = 31 * h + (g.decoupled() ? 0 : g.parts().size());
            }
        }
        return h;
    }

    private static CachedRocketRenderPlan buildPlan(int signature, RocketAssemblyGraph graph) {
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        List<CachedStageRender> stageList = new ArrayList<>();
        int minY = Integer.MAX_VALUE;

        for (RocketStageNode stage : graph.stages()) {
            if (stage.decoupled()) {
                continue;
            }
            List<CachedPartRender> parts = new ArrayList<>();
            int stageMinY = Integer.MAX_VALUE;
            int stageMaxY = Integer.MIN_VALUE;
            for (MountedPartNode node : stage.allActiveParts()) {
                RocketPartConfig cfg = node.config();
                RocketPartSpec spec = catalog.getPart(cfg.modelId());
                if (spec == null) {
                    spec = catalog.defaultSpecFor(cfg.category());
                }
                if (spec == null) {
                    continue;
                }
                int ry = node.relativePos().getY();
                int h = Math.max(1, cfg.heightBlocks());
                minY = Math.min(minY, ry);
                stageMinY = Math.min(stageMinY, ry);
                stageMaxY = Math.max(stageMaxY, ry + h);
                String famLower = cfg.familyId().toLowerCase(java.util.Locale.ROOT);
                boolean isEngine = cfg.category() == RocketPartCategory.ENGINE && node.stats().thrustKn() > 0.0;
                PropellantType prop = isEngine ? PropellantType.forEngine(cfg) : ModularFlightPhysics.resolvePropellant(stage.propellantId());
                parts.add(new CachedPartRender(
                        spec.meshLocation(),
                        spec.textureFor(cfg.livery()),
                        node.relativePos().getX(),
                        ry,
                        node.relativePos().getZ(),
                        Math.max(1, cfg.diameter()),
                        h,
                        isEngine,
                        famLower.contains("landing_gear") || famLower.contains("landing_leg"),
                        famLower.contains("hotstaging"),
                        prop
                ));
            }
            if (!parts.isEmpty()) {
                float midY = (stageMinY + stageMaxY) * 0.5F;
                stageList.add(new CachedStageRender(stage.stageIndex(), midY, List.copyOf(parts)));
            }
        }
        return new CachedRocketRenderPlan(signature, minY == Integer.MAX_VALUE ? 0.0F : (float) minY, List.copyOf(stageList));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ModularRocketFlightEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private record CachedPartRender(
            ResourceLocation meshLocation,
            ResourceLocation texture,
            float relX,
            float relY,
            float relZ,
            int diameter,
            int heightBlocks,
            boolean isEngine,
            boolean isLandingGear,
            boolean isHotStagingRing,
            PropellantType propellant
    ) {
    }

    private record CachedStageRender(int stageIndex, float midY, List<CachedPartRender> parts) {
    }

    private record CachedRocketRenderPlan(int signature, float minPartY, List<CachedStageRender> stages) {
    }
}
