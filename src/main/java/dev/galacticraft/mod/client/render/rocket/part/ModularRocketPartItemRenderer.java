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
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Dynamic item renderer for all 6 {@link ModularRocketPartBlockItem} categories.
 *
 * <p>Reads {@link GCDataComponents#ROCKET_PART_CONFIG} from the {@link ItemStack}, lazily loads the
 * matching {@code .gcrm} runtime model, and applies bounds-derived auto-fit normalization so every
 * part from {@code 1x1x1} up to {@code 11x11x6} renders centered and crisp in inventory slots,
 * dropped item entities, item frames, and first/third-person hands.
 */
@Environment(EnvType.CLIENT)
public class ModularRocketPartItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    public static final ModularRocketPartItemRenderer INSTANCE = new ModularRocketPartItemRenderer();

    private static final float COS_45 = 0.70710678F;
    private static final float COS_30 = 0.8660254F;
    private static final float COS_45_SIN_30 = 0.35355339F;

    public ModularRocketPartItemRenderer() {
    }

    @Override
    public void render(
            ItemStack stack,
            ItemDisplayContext mode,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            int overlay
    ) {
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        RocketPartConfig config = stack.get(GCDataComponents.ROCKET_PART_CONFIG);
        if (config == null) {
            config = catalog.defaultConfigFor(resolveCategory(stack));
        }

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

        float sizeX = model.boundsSizeX();
        float sizeY = model.boundsSizeY();
        float sizeZ = model.boundsSizeZ();
        float centerX = model.boundsCenterX();
        float centerY = model.boundsCenterY();
        float centerZ = model.boundsCenterZ();

        float maxExtent = Math.max(0.25F, Math.max(sizeX, Math.max(sizeY, sizeZ)));
        float normScale = 1.0F / maxExtent;

        matrices.pushPose();
        switch (mode) {
            case GUI -> {
                float projectedX = COS_45 * (sizeX + sizeZ);
                float projectedY = COS_30 * sizeY + COS_45_SIN_30 * (sizeX + sizeZ);
                float isoExtent = Math.max(0.25F, Math.max(maxExtent, Math.max(projectedX, projectedY)));
                float guiScale = 0.94F / isoExtent;

                matrices.translate(0.5F, 0.5F, 0.5F);
                matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                matrices.mulPose(Axis.YP.rotationDegrees(225.0F));
                matrices.scale(guiScale, guiScale, guiScale);
            }
            case GROUND -> {
                float s = 0.55F * normScale;
                matrices.translate(0.5F, 0.45F, 0.5F);
                matrices.scale(s, s, s);
            }
            case FIXED -> {
                float s = 0.72F * normScale;
                matrices.translate(0.5F, 0.5F, 0.5F);
                matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
                matrices.scale(s, s, s);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                float s = 0.50F * normScale;
                matrices.translate(0.5F, 0.5F, 0.5F);
                matrices.mulPose(Axis.YP.rotationDegrees(45.0F));
                matrices.scale(s, s, s);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                float s = 0.50F * normScale;
                matrices.translate(0.5F, 0.5F, 0.5F);
                matrices.mulPose(Axis.YP.rotationDegrees(225.0F));
                matrices.scale(s, s, s);
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                float s = 0.42F * normScale;
                matrices.translate(0.5F, 0.45F, 0.5F);
                matrices.mulPose(Axis.XP.rotationDegrees(75.0F));
                matrices.mulPose(Axis.YP.rotationDegrees(45.0F));
                matrices.scale(s, s, s);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                float s = 0.42F * normScale;
                matrices.translate(0.5F, 0.45F, 0.5F);
                matrices.mulPose(Axis.XP.rotationDegrees(75.0F));
                matrices.mulPose(Axis.YP.rotationDegrees(225.0F));
                matrices.scale(s, s, s);
            }
            case HEAD -> {
                float s = 0.85F * normScale;
                matrices.translate(0.5F, 0.5F, 0.5F);
                matrices.scale(s, s, s);
            }
            default -> {
                float s = 0.75F * normScale;
                matrices.translate(0.5F, 0.5F, 0.5F);
                matrices.scale(s, s, s);
            }
        }

        matrices.translate(-centerX, -centerY, -centerZ);

        boolean useLod = mode == ItemDisplayContext.GUI;
        RocketPartMeshRenderer.render(
                model,
                texture,
                matrices,
                vertexConsumers,
                light,
                overlay,
                0.0F,
                useLod,
                1.0F
        );

        matrices.popPose();
    }

    private static RocketPartCategory resolveCategory(ItemStack stack) {
        if (stack.getItem() instanceof ModularRocketPartBlockItem partItem) {
            return partItem.getCategory();
        }
        if (Block.byItem(stack.getItem()) instanceof ModularRocketPartBlock partBlock) {
            return partBlock.getCategory();
        }
        return RocketPartCategory.COMMAND;
    }
}
