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

package dev.galacticraft.mod.client.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.galacticraft.api.vehicle.HardpointType;
import dev.galacticraft.api.vehicle.VehicleChassisDefinition;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import dev.galacticraft.api.vehicle.VehicleHardpoint;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.api.vehicle.VehicleSizeClass;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.entity.vehicle.ModularVehicleEntity;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Immersive-Vehicles-style layered dashboard instrument renderer:
 * draws a 128x128 backlit analog/digital dial face + dynamically rotated needle layer
 * (or rotating compass rose / attitude ball) + live digital readout on every {@link HardpointType#DASHBOARD} slot.
 */
public final class VehicleInstrumentRenderer {
    private static final ResourceLocation TEX_SPEED = Constant.id("textures/entity/instruments/dial_speedometer.png");
    private static final ResourceLocation TEX_POWER = Constant.id("textures/entity/instruments/dial_power.png");
    private static final ResourceLocation TEX_ATMOS = Constant.id("textures/entity/instruments/dial_atmosphere.png");
    private static final ResourceLocation TEX_COMPASS = Constant.id("textures/entity/instruments/dial_compass.png");
    private static final ResourceLocation TEX_HORIZON = Constant.id("textures/entity/instruments/dial_horizon.png");
    private static final ResourceLocation TEX_NEEDLE = Constant.id("textures/entity/instruments/dial_needle.png");

    private VehicleInstrumentRenderer() {
    }

    public static void renderAll(
            @NotNull PoseStack ps,
            @NotNull MultiBufferSource buffers,
            @NotNull ModularVehicleEntity entity,
            @NotNull VehicleChassisDefinition chassis,
            @NotNull VehicleConfiguration cfg
    ) {
        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(entity.level());
        var stats = entity.getDerivedStats();
        Font font = Minecraft.getInstance().font;
        float dialRadius = chassis.sizeClass() == VehicleSizeClass.SMALL ? 0.115F : 0.155F;

        for (VehicleHardpoint hp : chassis.hardpoints()) {
            if (hp.type() != HardpointType.DASHBOARD) {
                continue;
            }
            ResourceLocation gaugeId = cfg.getPart(hp.id());
            if (gaugeId == null) {
                continue;
            }

            ps.pushPose();
            // Position flush on the rear-facing dashboard surface facing the pilot (-Z)
            ps.translate(hp.localX(), hp.localY(), hp.localZ() - 0.035F);
            ps.mulPose(Axis.YP.rotationDegrees(180.0F));

            ResourceLocation faceTex;
            float needleDeg = 0.0F;
            boolean rotateFaceInsteadOfNeedle = false;
            boolean drawNeedle = true;
            String digitalLabel;

            if (VehicleRegistry.PART_GAUGE_SPEEDOMETER.equals(gaugeId)) {
                faceTex = TEX_SPEED;
                double kmh = Math.abs(entity.getSyncedSpeed()) * 72.0;
                double maxKmh = Math.max(20.0, stats.maxSpeedBlocksPerTick() * 72.0);
                float frac = (float) Mth.clamp(kmh / maxKmh, 0.0, 1.0);
                needleDeg = -135.0F + frac * 270.0F;
                digitalLabel = String.format("%.0f", kmh);
            } else if (VehicleRegistry.PART_GAUGE_POWER.equals(gaugeId)) {
                faceTex = TEX_POWER;
                long cap = Math.max(1L, stats.energyCapacity());
                float frac = Mth.clamp((float) cfg.storedEnergy() / (float) cap, 0.0F, 1.0F);
                needleDeg = -135.0F + frac * 270.0F;
                digitalLabel = Math.round(frac * 100.0F) + "%";
            } else if (VehicleRegistry.PART_GAUGE_ATMOSPHERE.equals(gaugeId)) {
                faceTex = TEX_ATMOS;
                float frac = entity.isPressurizedCabin() ? 0.85F : Mth.clamp((float) env.pressureAtm(), 0.0F, 1.0F);
                needleDeg = -135.0F + frac * 270.0F;
                digitalLabel = entity.isPressurizedCabin() ? "O2 OK" : String.format("%.2f", env.pressureAtm());
            } else if (VehicleRegistry.PART_GAUGE_COMPASS.equals(gaugeId)) {
                faceTex = TEX_COMPASS;
                rotateFaceInsteadOfNeedle = true;
                drawNeedle = false;
                needleDeg = entity.getYRot();
                int hdg = Math.floorMod(Math.round(entity.getYRot()), 360);
                digitalLabel = String.format("%03d°", hdg);
            } else {
                faceTex = TEX_HORIZON;
                rotateFaceInsteadOfNeedle = true;
                drawNeedle = false;
                needleDeg = entity.isAircraft() ? entity.clientRoll : entity.getSteeringAngle() * 0.85F;
                if (entity.isFixedWing() && entity.isStalling()) {
                    digitalLabel = "STALL";
                } else {
                    digitalLabel = entity.isAircraft()
                            ? String.format("A:%.0fm", entity.computeAltitudeAgl())
                            : String.format("Y:%d", entity.blockPosition().getY());
                }
            }

            // Layer 1: Dial Face (rotated for compass/horizon, static for needle gauges)
            ps.pushPose();
            if (rotateFaceInsteadOfNeedle) {
                ps.mulPose(Axis.ZP.rotationDegrees(-needleDeg));
            }
            drawQuad(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(faceTex)), dialRadius, LightTexture.FULL_BRIGHT);
            ps.popPose();

            // Layer 2: Rotating Analog Pointer Needle (slightly forward to prevent z-fighting)
            if (drawNeedle) {
                ps.pushPose();
                ps.translate(0.0F, 0.0F, 0.004F);
                ps.mulPose(Axis.ZP.rotationDegrees(-needleDeg));
                drawQuad(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_NEEDLE)), dialRadius, LightTexture.FULL_BRIGHT);
                ps.popPose();
            }

            // Layer 3: Crisp Digital Sub-Readout at bottom of dial
            if (font != null) {
                ps.pushPose();
                ps.translate(0.0F, -dialRadius * 0.42F, 0.008F);
                float ts = dialRadius * 0.032F;
                ps.scale(ts, -ts, ts);
                font.drawInBatch(digitalLabel, -font.width(digitalLabel) * 0.5F, -4.0F, 0xFF7DF9FF, false, ps.last().pose(), buffers, Font.DisplayMode.NORMAL, 0xAA0A121C, LightTexture.FULL_BRIGHT);
                ps.popPose();
            }

            ps.popPose();
        }
    }

    private static void drawQuad(PoseStack ps, VertexConsumer c, float r, int light) {
        Matrix4f m = ps.last().pose();
        PoseStack.Pose pose = ps.last();
        c.addVertex(m, -r, -r, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        c.addVertex(m, r, -r, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        c.addVertex(m, r, r, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        c.addVertex(m, -r, r, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
    }
}
