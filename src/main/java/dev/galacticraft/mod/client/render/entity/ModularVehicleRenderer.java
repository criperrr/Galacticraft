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
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.galacticraft.api.vehicle.HardpointType;
import dev.galacticraft.api.vehicle.VehicleChassisDefinition;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import dev.galacticraft.api.vehicle.VehicleHardpoint;
import dev.galacticraft.api.vehicle.VehiclePartDefinition;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.api.vehicle.VehicleSizeClass;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.vehicle.ObjMeshModel;
import dev.galacticraft.mod.client.render.vehicle.ObjModelLoader;
import dev.galacticraft.mod.client.render.vehicle.VehicleAnimationBinding;
import dev.galacticraft.mod.client.render.vehicle.VehicleInstrumentRenderer;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.item.VehiclePartItem;
import dev.galacticraft.mod.content.item.VehicleWrenchItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * Immersive-Vehicles-inspired Wavefront {@code .obj} modular vehicle renderer (Sodium/Iris-safe):
 * <ul>
 *   <li>Loads real {@code .obj} meshes with named sub-object groups ({@code body}, {@code steering_wheel},
 *       {@code door_left}, {@code door_right}, {@code rear_ramp}, {@code headlights}, {@code taillights}, {@code glass}).</li>
 *   <li>Drives named groups with live variable-bound {@link VehicleAnimationBinding}s.</li>
 *   <li>Renders layered 128x128 analog/digital dial gauges via {@link VehicleInstrumentRenderer}.</li>
 *   <li>Highlights the exact 3D-raycasted {@link VehicleHardpoint} when installing parts or using the Wrench.</li>
 * </ul>
 */
public class ModularVehicleRenderer extends EntityRenderer<ModularVehicleEntity> {
    private static final ResourceLocation ATLAS_TEX = Constant.id("textures/entity/modular_vehicle_atlas.png");
    private static final ResourceLocation ROTOR_BLUR_TEX = Constant.id("textures/entity/rotor_blur_disc.png");
    private static final ResourceLocation MODEL_APOLLO_LRV = Constant.id("models/vehicle/apollo_lrv.obj");
    private static final ResourceLocation MODEL_NASA_SEV = Constant.id("models/vehicle/nasa_sev.obj");
    private static final ResourceLocation MODEL_MARS_HABITAT = Constant.id("models/vehicle/mars_habitat_6x6.obj");
    private static final ResourceLocation MODEL_LUNAR_WHEEL = Constant.id("models/vehicle/lunar_wheel.obj");
    private static final ResourceLocation MODEL_MICRO_DRONE = Constant.id("models/vehicle/micro_drone.obj");
    private static final ResourceLocation MODEL_LIGHT_HELICOPTER = Constant.id("models/vehicle/light_helicopter.obj");
    private static final ResourceLocation MODEL_HEAVY_VTOL = Constant.id("models/vehicle/heavy_vtol.obj");
    private static final ResourceLocation MODEL_ROTOR_BLADES = Constant.id("models/vehicle/rotor_blades.obj");
    private static final ResourceLocation MODEL_VTOL_THRUSTER = Constant.id("models/vehicle/vtol_thruster.obj");

    // MTS-style animation bindings per chassis
    private static final VehicleAnimationBinding STEER_LRV = new VehicleAnimationBinding("steering_wheel", VehicleAnimationBinding.Type.ROTATION, 0.0F, 1.06F, 0.36F, 0.0F, 0.0F, 1.0F, 3.2F);
    private static final VehicleAnimationBinding STEER_SEV = new VehicleAnimationBinding("steering_wheel", VehicleAnimationBinding.Type.ROTATION, -0.56F, 1.48F, 1.25F, 0.0F, 0.0F, 1.0F, 3.2F);
    private static final VehicleAnimationBinding STEER_MARS = new VehicleAnimationBinding("steering_wheel", VehicleAnimationBinding.Type.ROTATION, -0.68F, 1.82F, 1.74F, 0.0F, 0.0F, 1.0F, 3.2F);
    private static final VehicleAnimationBinding DOOR_LEFT_SEV = new VehicleAnimationBinding("door_left", VehicleAnimationBinding.Type.ROTATION, -1.26F, 1.28F, 1.81F, 0.0F, 1.0F, 0.0F, 55.0F);
    private static final VehicleAnimationBinding DOOR_RIGHT_SEV = new VehicleAnimationBinding("door_right", VehicleAnimationBinding.Type.ROTATION, 1.26F, 1.28F, 1.81F, 0.0F, 1.0F, 0.0F, -55.0F);
    private static final VehicleAnimationBinding DOOR_LEFT_HELI = new VehicleAnimationBinding("door_left", VehicleAnimationBinding.Type.ROTATION, -1.02F, 1.15F, 1.16F, 0.0F, 1.0F, 0.0F, 55.0F);
    private static final VehicleAnimationBinding DOOR_RIGHT_HELI = new VehicleAnimationBinding("door_right", VehicleAnimationBinding.Type.ROTATION, 1.02F, 1.15F, 1.16F, 0.0F, 1.0F, 0.0F, -55.0F);
    private static final VehicleAnimationBinding RAMP_MARS = new VehicleAnimationBinding("rear_ramp", VehicleAnimationBinding.Type.ROTATION, 0.0F, 1.30F, -2.68F, 1.0F, 0.0F, 0.0F, -82.0F);
    private static final VehicleAnimationBinding RAMP_VTOL = new VehicleAnimationBinding("rear_ramp", VehicleAnimationBinding.Type.ROTATION, 0.0F, 0.85F, -2.68F, 1.0F, 0.0F, 0.0F, -80.0F);

    public ModularVehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.4F;
    }

    @Override
    public void render(ModularVehicleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        VehicleConfiguration cfg = entity.getConfiguration();
        VehicleChassisDefinition chassis = entity.getChassisDefinition();
        VehicleSizeClass size = chassis.sizeClass();
        this.shadowRadius = size == VehicleSizeClass.LARGE ? 2.8F : (size == VehicleSizeClass.MEDIUM ? 2.0F : 1.45F);

        poseStack.pushPose();
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        boolean isRotorcraft = entity.isRotorcraft();
        if (isRotorcraft) {
            float pivotY = size == VehicleSizeClass.LARGE ? 1.45F : (size == VehicleSizeClass.MEDIUM ? 1.15F : 0.45F);
            poseStack.translate(0.0F, pivotY, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.clientPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-entity.clientRoll));
            poseStack.translate(0.0F, -pivotY, 0.0F);
        }

        ResourceLocation chassisId = chassis.id();
        ResourceLocation modelLoc = resolveChassisModel(chassisId, size);
        ObjMeshModel chassisModel = ObjModelLoader.getOrLoad(modelLoc);
        VertexConsumer opaque = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ATLAS_TEX));

        float steer = entity.getSteeringAngle();
        float doorOpen = entity.clientDoorProgress;
        float gearProgress = entity.clientGearProgress;
        boolean headlights = entity.areHeadlightsOn();
        boolean isLightHeli = VehicleRegistry.CHASSIS_LIGHT_HELICOPTER.equals(chassisId);
        boolean isHeavyVtol = VehicleRegistry.CHASSIS_HEAVY_VTOL.equals(chassisId);

        // 1. Render all opaque Wavefront .obj groups with live MTS animation bindings
        for (Map.Entry<String, ObjMeshModel.Group> entry : chassisModel.groups().entrySet()) {
            String name = entry.getKey();
            if ("glass".equals(name) || "door_left_glass".equals(name) || "door_right_glass".equals(name)) {
                continue; // Drawn in translucent pass after interior & instruments
            }
            poseStack.pushPose();
            if ("steering_wheel".equals(name)) {
                VehicleAnimationBinding bind = switch (size) {
                    case SMALL -> STEER_LRV;
                    case MEDIUM -> STEER_SEV;
                    case LARGE -> STEER_MARS;
                };
                bind.apply(poseStack, steer);
            } else if ("door_left".equals(name)) {
                (isLightHeli ? DOOR_LEFT_HELI : DOOR_LEFT_SEV).apply(poseStack, doorOpen);
            } else if ("door_right".equals(name)) {
                (isLightHeli ? DOOR_RIGHT_HELI : DOOR_RIGHT_SEV).apply(poseStack, doorOpen);
            } else if ("rear_ramp".equals(name)) {
                (isHeavyVtol ? RAMP_VTOL : RAMP_MARS).apply(poseStack, doorOpen);
            } else if ("landing_gear".equals(name)) {
                float retractY = gearProgress * (size == VehicleSizeClass.LARGE ? 0.54F : 0.40F);
                poseStack.translate(0.0F, retractY, 0.0F);
            }

            int groupLight = packedLight;
            float shade = 1.0F;
            if ("headlights".equals(name)) {
                groupLight = headlights ? LightTexture.FULL_BRIGHT : packedLight;
                shade = headlights ? 1.0F : 0.45F;
            } else if ("taillights".equals(name)) {
                groupLight = headlights ? LightTexture.FULL_BRIGHT : packedLight;
                shade = headlights ? 1.0F : 0.55F;
            }
            entry.getValue().render(poseStack, opaque, shade, shade, shade, 1.0F, groupLight);
            poseStack.popPose();
        }

        // 2. Render installed modular parts (wheels, rotors, VTOL thrusters, motor, batteries, seats, cargo, instruments)
        float halfW = size == VehicleSizeClass.LARGE ? 1.55F : (size == VehicleSizeClass.MEDIUM ? 1.26F : 0.96F);
        float halfL = size == VehicleSizeClass.LARGE ? 3.05F : (size == VehicleSizeClass.MEDIUM ? 2.18F : 1.56F);
        float rotorRpm = entity.getRotorRpm();
        float rotorAngle = entity.clientRotorAngle + (float) (rotorRpm * (Math.PI / 600.0)) * partialTick;
        for (VehicleHardpoint hp : chassis.hardpoints()) {
            if (hp.type() == HardpointType.DASHBOARD) {
                continue;
            }
            ResourceLocation partId = cfg.getPart(hp.id());
            if (partId == null) {
                if (hp.type() == HardpointType.LOCOMOTION) {
                    renderBareWheelAxle(poseStack, opaque, hp, halfW, packedLight);
                } else if (hp.type() == HardpointType.ROTOR) {
                    renderBareRotorMount(poseStack, opaque, hp, packedLight);
                }
                continue;
            }
            VehiclePartDefinition part = VehicleRegistry.getPart(partId);
            if (part == null) {
                continue;
            }
            if (part.type() == HardpointType.ROTOR) {
                renderInstalledRotorOrThruster(poseStack, opaque, hp, part, chassisId, rotorAngle, rotorRpm, entity.isEngineRunning(), packedLight);
            } else {
                renderInstalledPart(poseStack, opaque, hp, part, size, halfW, halfL, entity.clientWheelSpin, steer, packedLight);
            }
        }

        // 3. Render MTS-style 128x128 layered analog/digital dial gauges + rotating needles
        VehicleInstrumentRenderer.renderAll(poseStack, bufferSource, entity, chassis, cfg);

        // 4. Render translucent .obj glass groups (windshield + animated door glass swinging with doors!)
        VertexConsumer glassConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(ATLAS_TEX));
        ObjMeshModel.Group glassGroup = chassisModel.getGroup("glass");
        if (glassGroup != null) {
            glassGroup.render(poseStack, glassConsumer, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
        }
        ObjMeshModel.Group doorLeftGlass = chassisModel.getGroup("door_left_glass");
        if (doorLeftGlass != null) {
            poseStack.pushPose();
            (isLightHeli ? DOOR_LEFT_HELI : DOOR_LEFT_SEV).apply(poseStack, doorOpen);
            doorLeftGlass.render(poseStack, glassConsumer, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
            poseStack.popPose();
        }
        ObjMeshModel.Group doorRightGlass = chassisModel.getGroup("door_right_glass");
        if (doorRightGlass != null) {
            poseStack.pushPose();
            (isLightHeli ? DOOR_RIGHT_HELI : DOOR_RIGHT_SEV).apply(poseStack, doorOpen);
            doorRightGlass.render(poseStack, glassConsumer, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
            poseStack.popPose();
        }

        // 4b. Render translucent radial Rotor Blur Discs above 600 RPM (Sodium/Iris safe!)
        if (isRotorcraft && rotorRpm >= ModularVehicleEntity.BLUR_DISC_RPM_THRESHOLD) {
            float blurAlpha = Mth.clamp((rotorRpm - ModularVehicleEntity.BLUR_DISC_RPM_THRESHOLD) / 360.0F, 0.15F, 0.85F);
            VertexConsumer blurConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(ROTOR_BLUR_TEX));
            for (VehicleHardpoint hp : chassis.hardpoints()) {
                if (hp.type() != HardpointType.ROTOR) {
                    continue;
                }
                ResourceLocation partId = cfg.getPart(hp.id());
                if (partId == null || VehicleRegistry.PART_VTOL_THRUSTER.equals(partId)) {
                    continue;
                }
                renderRotorBlurDisc(poseStack, blurConsumer, hp, chassisId, partId, rotorAngle, blurAlpha, packedLight);
            }
        }

        // 5. Surgical per-slot 3D Raycast Holograms (bright highlight on exact aimed slot!)
        Player player = Minecraft.getInstance().player;
        if (player != null && player.distanceToSqr(entity) <= 81.0) {
            ItemStack main = player.getMainHandItem();
            boolean holdingWrench = main.getItem() instanceof VehicleWrenchItem;
            VehiclePartDefinition heldPart = main.getItem() instanceof VehiclePartItem pi ? VehicleRegistry.getPart(pi.getPartId()) : null;
            if (holdingWrench || heldPart != null) {
                VehicleHardpoint aimed = holdingWrench
                        ? entity.findTargetedHardpoint(player, false, null)
                        : entity.findTargetedHardpoint(player, true, heldPart);
                VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
                for (VehicleHardpoint hp : chassis.hardpoints()) {
                    boolean occupied = cfg.hasPart(hp.id());
                    boolean isAimed = aimed != null && aimed.id().equals(hp.id());
                    if (heldPart != null && !occupied && hp.accepts(heldPart.type(), heldPart.sizeClass())) {
                        for (net.minecraft.world.phys.AABB box : entity.getHardpointLocalBoxes(hp)) {
                            LevelRenderer.renderLineBox(
                                    poseStack, lines,
                                    box.minX, box.minY, box.minZ,
                                    box.maxX, box.maxY, box.maxZ,
                                    isAimed ? 0.15F : 0.15F, isAimed ? 1.0F : 0.72F, isAimed ? 0.35F : 0.85F, isAimed ? 1.0F : 0.55F
                            );
                        }
                    } else if (holdingWrench && occupied) {
                        for (net.minecraft.world.phys.AABB box : entity.getHardpointLocalBoxes(hp)) {
                            LevelRenderer.renderLineBox(
                                    poseStack, lines,
                                    box.minX, box.minY, box.minZ,
                                    box.maxX, box.maxY, box.maxZ,
                                    isAimed ? 1.0F : 0.88F, isAimed ? 0.25F : 0.68F, isAimed ? 0.10F : 0.20F, isAimed ? 1.0F : 0.40F
                            );
                        }
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private void renderInstalledPart(
            PoseStack ps, VertexConsumer c,
            VehicleHardpoint hp, VehiclePartDefinition part,
            VehicleSizeClass size, float halfW, float halfL,
            float wheelSpin, float steerDeg, int light
    ) {
        if (part.type() == HardpointType.LOCOMOTION) {
            renderSingleObjWheel(ps, c, hp, part, size, halfW, wheelSpin, steerDeg, light);
            return;
        }

        ps.pushPose();
        ps.translate(hp.localX(), hp.localY(), hp.localZ());
        switch (part.type()) {
            case MOTOR -> {
                // High-output electric traction motor housing + copper stator fins + inverter block
                drawAtlasBox(ps, c, 1, 0, -0.34F, 0.0F, -0.30F, 0.34F, 0.34F, 0.30F, light);
                drawAtlasBox(ps, c, 3, 0, -0.26F, 0.06F, -0.33F, 0.26F, 0.28F, 0.33F, light);
                drawAtlasBox(ps, c, 2, 0, -0.20F, 0.34F, -0.20F, 0.20F, 0.44F, 0.20F, light);
            }
            case POWERPLANT -> {
                if (part.solarRecharging()) {
                    drawAtlasBox(ps, c, 2, 0, -0.07F, 0.0F, -0.07F, 0.07F, 0.38F, 0.07F, light);
                    drawAtlasBox(ps, c, 2, 1, -0.78F, 0.38F, -0.50F, 0.78F, 0.44F, 0.50F, light);
                } else if (part.rtgTrickle()) {
                    drawAtlasBox(ps, c, 3, 0, -0.22F, 0.0F, -0.22F, 0.22F, 0.58F, 0.22F, light);
                    drawAtlasBox(ps, c, 2, 0, -0.38F, 0.08F, -0.04F, 0.38F, 0.50F, 0.04F, light);
                    drawAtlasBox(ps, c, 2, 0, -0.04F, 0.08F, -0.38F, 0.04F, 0.50F, 0.38F, light);
                } else {
                    drawAtlasBox(ps, c, 1, 0, -0.38F, 0.0F, -0.28F, 0.38F, 0.32F, 0.28F, light);
                    drawAtlasBox(ps, c, 3, 2, -0.32F, 0.32F, -0.22F, 0.32F, 0.36F, 0.22F, light);
                }
            }
            case SEAT -> {
                drawAtlasBox(ps, c, 1, 0, -0.28F, 0.0F, -0.26F, 0.28F, 0.14F, 0.26F, light);
                drawAtlasBox(ps, c, 1, 0, -0.28F, 0.14F, -0.26F, 0.28F, 0.66F, -0.15F, light);
                drawAtlasBox(ps, c, 2, 0, -0.18F, 0.66F, -0.24F, 0.18F, 0.82F, -0.15F, light);
                drawAtlasBox(ps, c, 2, 0, -0.32F, 0.24F, -0.18F, -0.26F, 0.32F, 0.18F, light);
                drawAtlasBox(ps, c, 2, 0, 0.26F, 0.24F, -0.18F, 0.32F, 0.32F, 0.18F, light);
            }
            case CARGO -> {
                float cw = part.sizeClass() == VehicleSizeClass.LARGE ? 1.04F : 0.60F;
                float ch = part.sizeClass() == VehicleSizeClass.LARGE ? 0.70F : 0.44F;
                float cd = cw * 0.62F;
                drawAtlasBox(ps, c, 3, 1, -cw, 0.02F, -cd, cw, ch, cd, light);
                drawAtlasBox(ps, c, 2, 0, -cw - 0.03F, 0.0F, -cd - 0.03F, cw + 0.03F, 0.08F, cd + 0.03F, light);
            }
            case INSTRUMENT -> {
                if (VehicleRegistry.PART_DRILL_ARM.equals(part.id())) {
                    drawAtlasBox(ps, c, 2, 0, -0.14F, 0.0F, -0.14F, 0.14F, 0.34F, 0.14F, light);
                    drawAtlasBox(ps, c, 3, 0, -0.08F, 0.28F, 0.0F, 0.08F, 0.42F, 0.62F, light);
                    drawAtlasBox(ps, c, 2, 0, -0.07F, -0.22F, 0.50F, 0.07F, 0.38F, 0.66F, light);
                } else {
                    drawAtlasBox(ps, c, 2, 0, -0.06F, 0.0F, -0.06F, 0.06F, 0.84F, 0.06F, light);
                    drawAtlasBox(ps, c, 0, 0, -0.24F, 0.80F, -0.12F, 0.24F, 0.98F, 0.14F, light);
                }
            }
            case AI_CORE -> {
                drawAtlasBox(ps, c, 1, 0, -0.20F, 0.0F, -0.20F, 0.20F, 0.26F, 0.20F, light);
                drawAtlasBox(ps, c, 0, 2, -0.14F, 0.26F, -0.14F, 0.14F, 0.30F, 0.14F, LightTexture.FULL_BRIGHT);
            }
            default -> {
            }
        }
        ps.popPose();
    }

    private static ResourceLocation resolveChassisModel(ResourceLocation chassisId, VehicleSizeClass size) {
        if (VehicleRegistry.CHASSIS_MICRO_DRONE.equals(chassisId)) {
            return MODEL_MICRO_DRONE;
        }
        if (VehicleRegistry.CHASSIS_LIGHT_HELICOPTER.equals(chassisId)) {
            return MODEL_LIGHT_HELICOPTER;
        }
        if (VehicleRegistry.CHASSIS_HEAVY_VTOL.equals(chassisId)) {
            return MODEL_HEAVY_VTOL;
        }
        return switch (size) {
            case SMALL -> MODEL_APOLLO_LRV;
            case MEDIUM -> MODEL_NASA_SEV;
            case LARGE -> MODEL_MARS_HABITAT;
        };
    }

    private static float rotorScaleFor(ResourceLocation chassisId, String hardpointId) {
        if (VehicleRegistry.CHASSIS_MICRO_DRONE.equals(chassisId)) {
            return 0.56F;
        }
        if (VehicleRegistry.CHASSIS_LIGHT_HELICOPTER.equals(chassisId)) {
            return "rotor_tail".equals(hardpointId) ? 0.52F : 2.25F;
        }
        if (VehicleRegistry.CHASSIS_HEAVY_VTOL.equals(chassisId)) {
            return 1.48F;
        }
        return 1.0F;
    }

    private void renderBareRotorMount(PoseStack ps, VertexConsumer c, VehicleHardpoint hp, int light) {
        float x = (float) hp.localX();
        float y = (float) hp.localY();
        float z = (float) hp.localZ();
        drawAtlasBox(ps, c, 2, 0, x - 0.12F, y - 0.06F, z - 0.12F, x + 0.12F, y + 0.10F, z + 0.12F, light);
        drawAtlasBox(ps, c, 1, 0, x - 0.06F, y + 0.10F, z - 0.06F, x + 0.06F, y + 0.18F, z + 0.06F, light);
    }

    private void renderInstalledRotorOrThruster(
            PoseStack ps, VertexConsumer c,
            VehicleHardpoint hp, VehiclePartDefinition part,
            ResourceLocation chassisId,
            float rotorAngle, float rotorRpm, boolean engineOn, int light
    ) {
        ps.pushPose();
        ps.translate(hp.localX(), hp.localY(), hp.localZ());
        boolean isTail = "rotor_tail".equals(hp.id());

        if (VehicleRegistry.PART_VTOL_THRUSTER.equals(part.id())) {
            ObjMeshModel thruster = ObjModelLoader.getOrLoad(MODEL_VTOL_THRUSTER);
            float tScale = VehicleRegistry.CHASSIS_HEAVY_VTOL.equals(chassisId) ? 1.32F
                    : (VehicleRegistry.CHASSIS_MICRO_DRONE.equals(chassisId) ? 0.72F : 1.02F);
            if (isTail) {
                ps.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                tScale *= 0.65F;
            }
            ps.scale(tScale, tScale, tScale);
            ObjMeshModel.Group housing = thruster.getGroup("housing");
            if (housing != null) {
                housing.render(ps, c, 1.0F, 1.0F, 1.0F, 1.0F, light);
            }
            ObjMeshModel.Group plasma = thruster.getGroup("plasma_ring");
            if (plasma != null) {
                boolean active = engineOn && rotorRpm > 120.0F;
                float glow = active ? 1.0F : 0.35F;
                plasma.render(ps, c, glow, glow, glow, 1.0F, active ? LightTexture.FULL_BRIGHT : light);
            }
            ps.popPose();
            return;
        }

        ObjMeshModel rotorObj = ObjModelLoader.getOrLoad(MODEL_ROTOR_BLADES);
        float scale = rotorScaleFor(chassisId, hp.id());
        if (isTail) {
            ps.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
        ps.scale(scale, scale, scale);

        ObjMeshModel.Group hub = rotorObj.getGroup("hub");
        if (hub != null) {
            hub.render(ps, c, 1.0F, 1.0F, 1.0F, 1.0F, light);
        }

        // Counter-rotating direction per corner for quadcopters / VTOLs
        float dir = (hp.id().endsWith("_fr") || hp.id().endsWith("_rl")) ? -1.0F : 1.0F;
        if (VehicleRegistry.PART_COAXIAL_MARS_ROTOR.equals(part.id())) {
            ObjMeshModel.Group lower = rotorObj.getGroup("blades_lower");
            if (lower != null) {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotation(rotorAngle * dir));
                lower.render(ps, c, 1.0F, 1.0F, 1.0F, 1.0F, light);
                ps.popPose();
            }
            ObjMeshModel.Group upper = rotorObj.getGroup("blades_upper");
            if (upper != null) {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotation(-rotorAngle * dir));
                upper.render(ps, c, 1.0F, 1.0F, 1.0F, 1.0F, light);
                ps.popPose();
            }
        } else {
            ObjMeshModel.Group heavy = rotorObj.getGroup("blades_heavy");
            if (heavy != null) {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotation(rotorAngle * dir));
                heavy.render(ps, c, 1.0F, 1.0F, 1.0F, 1.0F, light);
                ps.popPose();
            }
        }
        ps.popPose();
    }

    private void renderRotorBlurDisc(
            PoseStack ps, VertexConsumer c,
            VehicleHardpoint hp, ResourceLocation chassisId, ResourceLocation partId,
            float rotorAngle, float alpha, int light
    ) {
        float scale = rotorScaleFor(chassisId, hp.id());
        float radius = (VehicleRegistry.PART_HEAVY_LIFT_ROTOR.equals(partId) ? 1.24F : 1.06F) * scale;
        boolean isTail = "rotor_tail".equals(hp.id());

        ps.pushPose();
        ps.translate(hp.localX(), hp.localY(), hp.localZ());
        if (isTail) {
            ps.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
        ps.translate(0.0F, 0.075F * scale, 0.0F);
        ps.mulPose(Axis.YP.rotation(rotorAngle * 0.35F));
        drawBlurDiscQuad(ps, c, radius, alpha, light);
        if (VehicleRegistry.PART_COAXIAL_MARS_ROTOR.equals(partId)) {
            ps.translate(0.0F, 0.10F * scale, 0.0F);
            ps.mulPose(Axis.YP.rotation(-rotorAngle * 0.70F));
            drawBlurDiscQuad(ps, c, radius, alpha * 0.85F, light);
        }
        ps.popPose();
    }

    private static void drawBlurDiscQuad(PoseStack ps, VertexConsumer c, float r, float a, int light) {
        Matrix4f m = ps.last().pose();
        PoseStack.Pose pose = ps.last();
        // Top face (+Y)
        c.addVertex(m, -r, 0.0F, r).setColor(1.0F, 1.0F, 1.0F, a).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        c.addVertex(m, r, 0.0F, r).setColor(1.0F, 1.0F, 1.0F, a).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        c.addVertex(m, r, 0.0F, -r).setColor(1.0F, 1.0F, 1.0F, a).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        c.addVertex(m, -r, 0.0F, -r).setColor(1.0F, 1.0F, 1.0F, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
        // Bottom face (-Y) so disc is visible from below too
        c.addVertex(m, -r, 0.0F, -r).setColor(1.0F, 1.0F, 1.0F, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        c.addVertex(m, r, 0.0F, -r).setColor(1.0F, 1.0F, 1.0F, a).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        c.addVertex(m, r, 0.0F, r).setColor(1.0F, 1.0F, 1.0F, a).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
        c.addVertex(m, -r, 0.0F, r).setColor(1.0F, 1.0F, 1.0F, a).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, -1, 0);
    }

    private void renderBareWheelAxle(PoseStack ps, VertexConsumer c, VehicleHardpoint hp, float halfW, int light) {
        float hubX = (float) hp.localX();
        float hubY = (float) hp.localY();
        float hubZ = (float) hp.localZ();
        float side = hubX < 0.0F ? -1.0F : 1.0F;
        float innerX = side * (halfW - 0.12F);
        float x0 = Math.min(innerX, hubX);
        float x1 = Math.max(innerX, hubX);
        drawAtlasBox(ps, c, 2, 0, x0, hubY - 0.05F, hubZ - 0.06F, x1, hubY + 0.05F, hubZ + 0.06F, light);
        drawAtlasBox(ps, c, 1, 0, hubX - 0.06F, hubY - 0.12F, hubZ - 0.12F, hubX + 0.06F, hubY + 0.12F, hubZ + 0.12F, light);
    }

    private void renderSingleObjWheel(
            PoseStack ps, VertexConsumer c,
            VehicleHardpoint hp, VehiclePartDefinition part,
            VehicleSizeClass size, float halfW,
            float wheelSpin, float steerDeg, int light
    ) {
        float hubX = (float) hp.localX();
        float hubY = (float) hp.localY();
        float hubZ = (float) hp.localZ();
        float side = hubX < 0.0F ? -1.0F : 1.0F;
        float strutX0 = Math.min(side * (halfW - 0.10F), hubX);
        float strutX1 = Math.max(side * (halfW - 0.10F), hubX);
        drawAtlasBox(ps, c, 2, 0, strutX0, hubY - 0.05F, hubZ - 0.07F, strutX1, hubY + 0.07F, hubZ + 0.07F, light);

        if (VehicleRegistry.PART_HEAVY_TRACKS.equals(part.id())) {
            float trackW = size == VehicleSizeClass.LARGE ? 0.50F : 0.40F;
            float trackH = size == VehicleSizeClass.LARGE ? 0.72F : 0.56F;
            float trackL = size == VehicleSizeClass.LARGE ? 0.85F : 0.68F;
            drawAtlasBox(ps, c, 0, 1, hubX - trackW * 0.5F, 0.02F, hubZ - trackL, hubX + trackW * 0.5F, trackH, hubZ + trackL, light);
            return;
        }

        ObjMeshModel wheelObj = ObjModelLoader.getOrLoad(MODEL_LUNAR_WHEEL);
        float wheelScale = size == VehicleSizeClass.LARGE ? 1.32F : (size == VehicleSizeClass.MEDIUM ? 1.08F : 0.84F);
        boolean isFront = hp.id().contains("_f");

        ps.pushPose();
        ps.translate(hubX, hubY, hubZ);
        if (isFront) {
            ps.mulPose(Axis.YP.rotationDegrees(-steerDeg));
        }
        ps.mulPose(Axis.XP.rotation(wheelSpin));
        ps.scale(wheelScale, wheelScale, wheelScale);
        for (ObjMeshModel.Group g : wheelObj.groups().values()) {
            g.render(ps, c, 1.0F, 1.0F, 1.0F, 1.0F, light);
        }
        ps.popPose();
    }

    private static void drawAtlasBox(PoseStack ps, VertexConsumer c, int col, int row, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        float eps = 2.0F / 256.0F;
        float u0 = col * 0.25F + eps;
        float v0 = row * 0.25F + eps;
        float u1 = (col + 1) * 0.25F - eps;
        float v1 = (row + 1) * 0.25F - eps;
        Matrix4f m = ps.last().pose();
        PoseStack.Pose pose = ps.last();
        quad(m, pose, c, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, u0, v0, u1, v1, 1.0F, light);
        quad(m, pose, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, u0, v0, u1, v1, 0.70F, light);
        quad(m, pose, c, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, u0, v0, u1, v1, 0.94F, light);
        quad(m, pose, c, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, u0, v0, u1, v1, 0.86F, light);
        quad(m, pose, c, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, u0, v0, u1, v1, 0.82F, light);
        quad(m, pose, c, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, u0, v0, u1, v1, 0.88F, light);
    }

    private static void quad(
            Matrix4f m, PoseStack.Pose pose, VertexConsumer c,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float nx, float ny, float nz,
            float u0, float v0, float u1, float v1,
            float s, int light
    ) {
        c.addVertex(m, x0, y0, z0).setColor(s, s, s, 1.0F).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        c.addVertex(m, x1, y1, z1).setColor(s, s, s, 1.0F).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        c.addVertex(m, x2, y2, z2).setColor(s, s, s, 1.0F).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        c.addVertex(m, x3, y3, z3).setColor(s, s, s, 1.0F).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(ModularVehicleEntity entity) {
        return ATLAS_TEX;
    }
}
