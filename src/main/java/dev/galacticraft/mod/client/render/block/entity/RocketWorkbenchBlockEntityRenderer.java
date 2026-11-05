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

package dev.galacticraft.mod.client.render.block.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.entity.model.GCEntityModelLayer;
import dev.galacticraft.mod.content.block.entity.RocketWorkbenchBlockEntity;
import dev.galacticraft.mod.content.block.special.RocketWorkbench;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

@Environment(EnvType.CLIENT)
public class RocketWorkbenchBlockEntityRenderer implements BlockEntityRenderer<RocketWorkbenchBlockEntity> {
    private static final ResourceLocation TEXTURE = Constant.id("textures/model/rocket_workbench.png");
    private final ModelPart top;
    private final ModelPart plierArm, plierSmallArm;
    private final ModelPart drillArm, drillTool, drillBit;
    private final ModelPart flashlightArm, flashlight;
    private final ModelPart display;
    private final Font font;

    public RocketWorkbenchBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(GCEntityModelLayer.ROCKET_WORKBENCH);
        this.top = root.getChild(Constant.ModelPartName.ROCKET_WORKBENCH_TOP);
        this.plierArm = root.getChild(Constant.ModelPartName.ROCKET_WORKBENCH_PLIER_TOOL_ARM);
        this.plierSmallArm = root.getChild(Constant.ModelPartName.ROCKET_WORKBENCH_PLIER_TOOL_SMALL_ARM);
        this.drillArm = root.getChild(Constant.ModelPartName.ROCKET_WORKBENCH_DRILL_TOOL_SMALL_ARM);
        this.drillTool = root.getChild(Constant.ModelPartName.ROCKET_WORKBENCH_DRILL_TOOL_DRILL);
        this.drillBit = this.drillTool.getChild(Constant.ModelPartName.ROCKET_WORKBENCH_DRILL_TOOL_DRILL_BIT);
        this.flashlightArm = root.getChild(Constant.ModelPartName.ROCKET_WORKBENCH_FLASHLIGHT_HOLDER);
        this.flashlight = root.getChild(Constant.ModelPartName.ROCKET_WORKBENCH_FLASHLIGHT_HANDLE);
        this.display = root.getChild(Constant.ModelPartName.ROCKET_WORKBENCH_DISPLAY);
        this.font = context.getFont();
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        partDefinition.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_TOP, CubeListBuilder.create().texOffs(0, 32).addBox(2.0F, -4.0F, -14.0F, 12, 4, 12, CubeDeformation.NONE), PartPose.rotation(Mth.PI, 0.0F, 0.0F));

        partDefinition.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_PLIER_TOOL_ARM, CubeListBuilder.create().texOffs(48, 32).addBox(-2.0F, -15.0F, -4.0F, 4, 16, 4, CubeDeformation.NONE), PartPose.rotation(157.5F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition plierSmallArm = partDefinition.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_PLIER_TOOL_SMALL_ARM, CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -12.0F, -1.0F, 2, 13, 2, CubeDeformation.NONE), PartPose.rotation(Mth.PI, 0.0F, 0.0F));
        plierSmallArm.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_PLIER_TOOL_PLIERS, CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, 12.01F, 4, 4, 3, CubeDeformation.NONE), PartPose.rotation(Mth.HALF_PI, Mth.PI, 0.0F));

        PartDefinition drillArm = partDefinition.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_DRILL_TOOL_SMALL_ARM, CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -14.0F, -1.0F, 2, 14, 2, CubeDeformation.NONE), PartPose.rotation(202.5F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition drillTool = partDefinition.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_DRILL_TOOL_DRILL, CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, -2.0F, -2.0F, 4, 8, 4, CubeDeformation.NONE), PartPose.rotation(Mth.HALF_PI, Mth.PI, 0.0F));
        drillTool.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_DRILL_TOOL_DRILL_BIT, CubeListBuilder.create().texOffs(0, 6).addBox(0.0F, 6.0F, -0.5F, 0, 4, 1, CubeDeformation.NONE), PartPose.ZERO);

        PartDefinition flashlightArm = partDefinition.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_FLASHLIGHT_HOLDER, CubeListBuilder.create().texOffs(56, 0).addBox(-1.0F, -14.0F, -0.5F, 1, 14, 1, CubeDeformation.NONE), PartPose.rotation(0.0F, 0.0F, 202.5F * Mth.DEG_TO_RAD));
        PartDefinition flashlight = partDefinition.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_FLASHLIGHT_HANDLE, CubeListBuilder.create().texOffs(0, 40).addBox(-2.0F, -1.0F, -1.0F, 4, 2, 2, CubeDeformation.NONE), PartPose.rotation(0.0F, 0.0F, Mth.PI));
        flashlight.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_FLASHLIGHT_LIGHT, CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, -2.0F, -2.0F, 2, 4, 4, CubeDeformation.NONE), PartPose.ZERO);

        partDefinition.addOrReplaceChild(Constant.ModelPartName.ROCKET_WORKBENCH_DISPLAY, CubeListBuilder.create().texOffs(16, 48).addBox(0.0F, -6.0F, -4.0F, 1, 6, 8, CubeDeformation.NONE), PartPose.rotation(0.0F, 0.0F, 202.5F * Mth.DEG_TO_RAD));
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void render(RocketWorkbenchBlockEntity blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        Level level = blockEntity.getLevel();
        light = LevelRenderer.getLightColor(level, blockEntity.getBlockPos().relative(Direction.UP, 1));
        Direction facing = blockEntity.getBlockState().getValue(RocketWorkbench.FACING);
        boolean active = blockEntity.getBlockState().getValue(RocketWorkbench.ACTIVE);

        float targetBlend = active ? 1.0F : 0.0F;
        blockEntity.animationBlend = Mth.lerp(0.08F, blockEntity.animationBlend, targetBlend);
        if (Math.abs(blockEntity.animationBlend - targetBlend) < 0.001F) {
            blockEntity.animationBlend = targetBlend;
        }
        float blend = blockEntity.animationBlend;
        float time = (level != null ? (float) level.getGameTime() : 0.0F) + tickDelta;

        matrices.pushPose();
        matrices.translate(0.0F, 1.0F, 0.0F);
        matrices.rotateAround(Axis.YP.rotationDegrees(90.0F - facing.toYRot()), 0.5F, 0.0F, 0.5F);
        RenderSystem.setShaderTexture(0, RocketWorkbenchBlockEntityRenderer.TEXTURE);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        if (blend <= 0.001F) {
            this.render(matrices, vertexConsumer, light, overlay);
        } else {
            this.renderAnimated(matrices, vertexConsumer, light, overlay, time, blend);
        }

        this.renderCrtOverlay(blockEntity, tickDelta, matrices, vertexConsumers, active, time);

        matrices.popPose();
    }

    private void renderCrtOverlay(RocketWorkbenchBlockEntity blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, boolean active, float time) {
        float alpha;
        if (active) {
            float syncedAlpha = blockEntity.getClientProgressRatio(tickDelta);
            alpha = syncedAlpha > 0.0F ? syncedAlpha : (Math.floorMod((long) time, 80) + tickDelta) / 80.0F;
        } else {
            alpha = 0.0F;
        }

        String statusText = active ? "[ FABRICATING ]" : "[ STANDBY ]";
        int statusColor = active ? 0x00FF66 : 0x33AA33;

        int pct = (int) (alpha * 100.0F);
        int bars = Mth.clamp(pct / 10, 0, 10);
        String barText = "[" + "=".repeat(bars) + " ".repeat(10 - bars) + "] " + pct + "%";
        int barColor = active ? 0x66FF99 : 0x448844;

        matrices.pushPose();
        matrices.translate(1.0F, 0.0F, 0.5F);
        matrices.mulPose(Axis.ZP.rotationDegrees(202.5F));

        // Inner screen face (facing workbench center, x = 1/16 + 0.002 = 0.0645F, y = -3/16 = -0.1875F)
        matrices.pushPose();
        matrices.translate(0.0645F, -0.1875F, 0.0F);
        matrices.mulPose(Axis.YP.rotationDegrees(-90.0F));
        matrices.scale(0.0042F, 0.0042F, 0.0042F);
        this.font.drawInBatch(statusText, -this.font.width(statusText) / 2.0F, -9.0F, statusColor, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        this.font.drawInBatch(barText, -this.font.width(barText) / 2.0F, 1.0F, barColor, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        matrices.popPose();

        // Outer display face (facing outward, x = -0.002F, y = -3/16 = -0.1875F)
        matrices.pushPose();
        matrices.translate(-0.002F, -0.1875F, 0.0F);
        matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
        matrices.scale(0.0042F, 0.0042F, 0.0042F);
        this.font.drawInBatch(statusText, -this.font.width(statusText) / 2.0F, -9.0F, statusColor, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        this.font.drawInBatch(barText, -this.font.width(barText) / 2.0F, 1.0F, barColor, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        matrices.popPose();

        matrices.popPose();
    }

    private void renderAnimated(PoseStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float time, float blend) {
        // 1. Table Top
        this.top.render(matrices, vertexConsumer, light, overlay);

        // 2. Articulated Plier Arm Assembly (Base bolted at (0.5, 0.0, 0.0); elbow hinged at (0.5, 0.55, -0.69) in 40° frame)
        float plierShoulderPitch = (6.0F + 2.5F * Mth.sin(time * 0.10F)) * blend;
        float plierShoulderYaw = (4.0F * Mth.sin(time * 0.07F)) * blend;
        float plierElbowPitch = (18.0F + 6.0F * Mth.sin(time * 0.20F)) * blend;

        matrices.pushPose();
        matrices.rotateAround(Axis.YP.rotationDegrees(plierShoulderYaw), 0.5F, 0.0F, 0.0F);
        matrices.rotateAround(Axis.XP.rotationDegrees(plierShoulderPitch), 0.5F, 0.0F, 0.0F);

        matrices.pushPose();
        matrices.translate(0.5F, 0.0F, 0.0F);
        this.plierArm.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.pushPose();
        matrices.mulPose(Axis.XP.rotationDegrees(40.0F));
        matrices.translate(0.5F, 0.55F, -0.69F);
        matrices.mulPose(Axis.XP.rotationDegrees(plierElbowPitch));
        this.plierSmallArm.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.popPose();

        // 3. Articulated Drill Assembly (Base bolted at (0.5, 0.0, 0.875); wrist hinged at (0.5, 0.875, 1.25))
        float drillShoulderPitch = (-7.0F + 3.0F * Mth.sin(time * 0.14F)) * blend;
        float drillShoulderYaw = (3.5F * Mth.cos(time * 0.09F)) * blend;
        float drillPeck = Mth.sin(time * 0.35F);
        float drillWristPitch = (-14.0F + 7.0F * drillPeck + 0.6F * Mth.sin(time * 1.8F)) * blend;

        matrices.pushPose();
        matrices.rotateAround(Axis.YP.rotationDegrees(drillShoulderYaw), 0.5F, 0.0F, 0.875F);
        matrices.rotateAround(Axis.XP.rotationDegrees(drillShoulderPitch), 0.5F, 0.0F, 0.875F);

        matrices.pushPose();
        matrices.translate(0.5F, 0.0F, 0.875F);
        this.drillArm.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.pushPose();
        matrices.translate(0.5F, 0.875F, 1.25F);
        matrices.mulPose(Axis.XP.rotationDegrees(drillWristPitch));
        this.drillBit.yRot = (time * 0.95F) * blend;
        this.drillTool.render(matrices, vertexConsumer, light, overlay);
        this.drillBit.yRot = 0.0F;
        matrices.popPose();

        matrices.popPose();

        // 4. Articulated Inspection Lamp Assembly (Base bolted at (0.125, 0.0, 0.5); neck hinged at (-0.4375, 0.75, 0.5) in -20° frame)
        float lampShoulderRoll = (-5.0F + 2.0F * Mth.sin(time * 0.06F)) * blend;
        float lampNeckTilt = (-8.0F + 3.0F * Mth.cos(time * 0.12F)) * blend;
        float lampNeckPan = (6.0F * Mth.sin(time * 0.08F)) * blend;

        matrices.pushPose();
        matrices.rotateAround(Axis.ZP.rotationDegrees(lampShoulderRoll), 0.125F, 0.0F, 0.5F);

        matrices.pushPose();
        matrices.translate(0.125F, 0.0F, 0.5F);
        this.flashlightArm.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.pushPose();
        matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F));
        matrices.translate(-0.4375F, 0.75F, 0.5F);
        matrices.mulPose(Axis.ZP.rotationDegrees(lampNeckTilt));
        matrices.mulPose(Axis.YP.rotationDegrees(lampNeckPan));
        this.flashlight.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.popPose();

        // 5. Display Panel
        matrices.pushPose();
        matrices.translate(1.0F, 0.0F, 0.5F);
        this.display.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();
    }

    public void render(PoseStack matrices, VertexConsumer vertexConsumer, int light, int overlay) {
        this.drillBit.yRot = 0.0F;
        this.top.render(matrices, vertexConsumer, light, overlay);

        matrices.pushPose();
        matrices.translate(0.5F, 0.0F, 0.0F);
        this.plierArm.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.pushPose();
        matrices.mulPose(Axis.XP.rotationDegrees(40.0F));
        matrices.translate(0.5F, 0.55F, -0.69F);
        this.plierSmallArm.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.pushPose();
        matrices.translate(0.5F, 0.0F, 0.875F);
        this.drillArm.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.pushPose();
        matrices.translate(0.5F, 0.875F, 1.25F);
        this.drillTool.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.pushPose();
        matrices.translate(0.125F, 0.0F, 0.5F);
        this.flashlightArm.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.pushPose();
        matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F));
        matrices.translate(-0.4375F, 0.75F, 0.5F);
        this.flashlight.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();

        matrices.pushPose();
        matrices.translate(1.0F, 0.0F, 0.5F);
        this.display.render(matrices, vertexConsumer, light, overlay);
        matrices.popPose();
    }
}
