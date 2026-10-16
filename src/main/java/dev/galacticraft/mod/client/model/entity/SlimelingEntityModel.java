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

package dev.galacticraft.mod.client.model.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.galacticraft.mod.content.entity.SlimelingEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

public class SlimelingEntityModel<T extends SlimelingEntity> extends EntityModel<T> {
    private static final String NECK = "neck";
    private static final String TAIL_1 = "tail_1";
    private static final String TAIL_2 = "tail_2";
    private static final String TAIL_3 = "tail_3";

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;

    private int tintColor = -1;

    public SlimelingEntityModel(ModelPart root) {
        this.body = root.getChild(PartNames.BODY);
        this.neck = this.body.getChild(NECK);
        this.head = this.neck.getChild(PartNames.HEAD);
        this.tail1 = this.body.getChild(TAIL_1);
        this.tail2 = this.tail1.getChild(TAIL_2);
        this.tail3 = this.tail2.getChild(TAIL_3);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                PartNames.BODY,
                CubeListBuilder.create().texOffs(82, 0).mirror().addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F)
        );
        PartDefinition neck = body.addOrReplaceChild(
                NECK,
                CubeListBuilder.create().texOffs(122, 0).mirror().addBox(-4.5F, -9.5F, -2.0F, 9.0F, 9.0F, 8.0F),
                PartPose.ZERO
        );
        neck.addOrReplaceChild(
                PartNames.HEAD,
                CubeListBuilder.create().texOffs(156, 0).mirror().addBox(-3.5F, -14.7F, 1.5F, 7.0F, 7.0F, 7.0F),
                PartPose.ZERO
        );
        PartDefinition tail1 = body.addOrReplaceChild(
                TAIL_1,
                CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-4.5F, -2.0F, -10.0F, 9.0F, 7.0F, 8.0F),
                PartPose.ZERO
        );
        PartDefinition tail2 = tail1.addOrReplaceChild(
                TAIL_2,
                CubeListBuilder.create().texOffs(20, 0).mirror().addBox(-3.5F, 0.0F, -14.0F, 7.0F, 5.0F, 7.0F),
                PartPose.ZERO
        );
        tail2.addOrReplaceChild(
                TAIL_3,
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-2.5F, 2.0F, -16.0F, 5.0F, 3.0F, 5.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshDefinition, 256, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float swing = Mth.cos(limbSwing * 0.6662F) * 0.2F * limbSwingAmount;
        this.tail1.yRot = swing;
        this.tail2.yRot = swing;
        this.tail3.yRot = swing;
        this.tintColor = FastColor.ARGB32.colorFromFloat(1.0F, entity.getColorRed(), entity.getColorGreen(), entity.getColorBlue());
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertices, int light, int overlay, int color) {
        this.body.render(poseStack, vertices, light, overlay, FastColor.ARGB32.multiply(color, this.tintColor));
    }
}
