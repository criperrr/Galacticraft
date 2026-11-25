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
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import dev.galacticraft.api.rocket.part.modular.FootprintMode;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.assembly.RocketAssemblyGraphBuilder;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * World-space KSP-VAB style ghost placement preview renderer for held {@link ModularRocketPartBlockItem} stacks.
 *
 * <p>Hooked into {@code WorldRenderEvents.AFTER_TRANSLUCENT} and {@code HudRenderCallback}. Supports:
 * <ul>
 *   <li>Magnetic coaxial and radial node snapping</li>
 *   <li>Diameter continuity validation ({@code below.topDiameter() == current.diameter()})</li>
 *   <li>Octagonal radial symmetry mirroring ({@code 1x / 2x / 4x / 4x_diag / 8x}, cycled via {@code Shift+R})</li>
 *   <li>Atomic green (valid) / red (invalid) tinting across all mirrored ghosts + compact HUD error tag</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class RocketPartPlacementPreviewRenderer {
    private static final float GHOST_ALPHA = 0.55F;

    private static RadialSymmetry activeSymmetry = RadialSymmetry.NONE;
    private static @Nullable String lastStatusTag = null;
    private static boolean lastPlacementValid = true;
    private static boolean lastPreviewVisible = false;

    private RocketPartPlacementPreviewRenderer() {
    }

    public static RadialSymmetry getActiveSymmetry() {
        return activeSymmetry;
    }

    public static RadialSymmetry cycleSymmetry() {
        activeSymmetry = activeSymmetry.next();
        return activeSymmetry;
    }

    public static void setActiveSymmetry(RadialSymmetry symmetry) {
        if (symmetry != null) {
            activeSymmetry = symmetry;
        }
    }

    public static void render(WorldRenderContext context) {
        lastPreviewVisible = false;
        lastStatusTag = null;

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
        BlockPos primaryOrigin = ModularRocketPartBlockItem.resolvePlacementOrigin(level, placeContext, config, facing);

        HangarTerminalBlockEntity nearbyTerminal = findNearbyFormedTerminal(primaryOrigin);
        List<BlockPos> ghostOrigins = resolveMirroredOrigins(level, primaryOrigin, config, nearbyTerminal);

        // Atomic validation across ALL mirrored ghost positions
        String errorTag = validateAtomicPlacement(level, ghostOrigins, config, facing, nearbyTerminal);
        boolean valid = errorTag == null;
        lastPreviewVisible = true;
        lastPlacementValid = valid;
        lastStatusTag = errorTag;

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

        float tintR = valid ? 0.65F : 1.0F;
        float tintG = valid ? 1.0F : 0.35F;
        float tintB = valid ? 0.85F : 0.35F;
        float shroudScaleY = "interstage_engine_shroud".equals(config.familyId()) ? 0.0F : 1.0F;

        // Sub-pass 1: All translucent ghost meshes submitted before RenderType.lines()
        if (model != null && texture != null) {
            for (BlockPos origin : ghostOrigins) {
                poseStack.pushPose();
                poseStack.translate(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
                float yRotDeg = RocketPartMeshRenderer.facingToYRotDegrees(facing);
                if (yRotDeg != 0.0F) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(yRotDeg));
                }

                RocketPartMeshRenderer.render(
                        model,
                        texture,
                        poseStack,
                        consumers,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        0.0F,
                        false,
                        tintR,
                        tintG,
                        tintB,
                        GHOST_ALPHA,
                        shroudScaleY
                );

                // Preview automatic engine shroud on primary coaxial snap
                if (origin.equals(primaryOrigin)) {
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
                }
                poseStack.popPose();
            }
        }

        // Sub-pass 2: Crisp green/cyan (valid) or red (invalid) wireframe boxes for every mirrored ghost
        AABB localBounds = FootprintMode.computeBoundsAABB(config);
        VertexConsumer lineConsumer = consumers.getBuffer(RenderType.lines());
        float lineR = valid ? 0.15F : 1.0F;
        float lineG = valid ? 0.95F : 0.20F;
        float lineB = valid ? 0.55F : 0.20F;
        float lineA = valid ? 0.85F : 0.95F;

        for (BlockPos origin : ghostOrigins) {
            AABB worldBounds = RocketPartMeshRenderer.rotateLocalBounds(localBounds, facing)
                    .move(origin)
                    .inflate(0.002);
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
        }

        poseStack.popPose();
    }

    /**
     * Renders a compact aerospace HUD badge above the hotbar whenever a player holds a modular
     * rocket part, showing active symmetry ({@code 1x/2x/4x/8x [Shift+R]}) and live placement status.
     */
    public static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || !lastPreviewVisible) {
            return;
        }
        boolean holdingPart = player.getMainHandItem().getItem() instanceof ModularRocketPartBlockItem
                || player.getOffhandItem().getItem() instanceof ModularRocketPartBlockItem;
        if (!holdingPart) {
            return;
        }

        String symText = "Simetria: " + activeSymmetry.getSerializedName() + " [Shift+R]";
        String statusText = lastPlacementValid
                ? "SNAP OK"
                : ("ERRO: " + (lastStatusTag != null ? lastStatusTag : "INVALIDO"));
        String combined = symText + "  |  " + statusText;

        int textW = mc.font.width(combined);
        int boxW = textW + 12;
        int boxH = 16;
        int bx = (graphics.guiWidth() - boxW) / 2;
        int by = graphics.guiHeight() - 62;

        int bg = lastPlacementValid ? 0xD00D1E2B : 0xD82B1116;
        int border = lastPlacementValid ? 0xFF38BDF8 : 0xFFF87171;
        int textColor = lastPlacementValid ? 0xFF5EEAD4 : 0xFFFCA5A5;

        graphics.fill(bx, by, bx + boxW, by + boxH, bg);
        graphics.renderOutline(bx, by, boxW, boxH, border);
        graphics.drawCenteredString(mc.font, combined, bx + boxW / 2, by + 4, textColor);
    }

    private static List<BlockPos> resolveMirroredOrigins(
            ClientLevel level,
            BlockPos primaryOrigin,
            RocketPartConfig config,
            @Nullable HangarTerminalBlockEntity terminal
    ) {
        if (activeSymmetry == RadialSymmetry.NONE || terminal == null) {
            return List.of(primaryOrigin);
        }
        BlockPos padCenter = terminal.getPadCenterPos();
        int dx = primaryOrigin.getX() - padCenter.getX();
        int dz = primaryOrigin.getZ() - padCenter.getZ();

        // If snapping off-axis around a formed pad, mirror symmetrically across all octagonal azimuths!
        if (dx != 0 || dz != 0) {
            int coreDiam = Math.max(1, resolveCoreDiameterAtY(level, padCenter, primaryOrigin.getY()));
            int boosterDiam = Math.max(config.diameter(), config.topDiameter());
            BlockPos coreBase = new BlockPos(padCenter.getX(), primaryOrigin.getY(), padCenter.getZ());
            return RocketAssemblyGraphBuilder.computeRadialBoosterOrigins(coreBase, coreDiam, boosterDiam, activeSymmetry);
        }
        return List.of(primaryOrigin);
    }

    private static @Nullable String validateAtomicPlacement(
            ClientLevel level,
            List<BlockPos> origins,
            RocketPartConfig config,
            Direction facing,
            @Nullable HangarTerminalBlockEntity terminal
    ) {
        for (BlockPos origin : origins) {
            if (!ModularRocketPartBlockItem.canPlaceFootprint(level, origin, config, facing)) {
                return "CLEARANCE_OBSTRUCTED";
            }
        }

        // Coaxial diameter continuity check when snapping atop an existing rocket part
        if (!config.radial() && origins.size() == 1) {
            BlockPos primary = origins.get(0);
            ModularRocketPartBlockEntity belowBe = resolvePartAt(level, primary.below());
            if (belowBe != null && !belowBe.getConfig().radial()) {
                int belowTop = belowBe.getConfig().topDiameter();
                if (belowTop != config.diameter()) {
                    return "DIAMETER_MISMATCH (" + belowTop + "x" + belowTop + " -> " + config.diameter() + "x" + config.diameter() + ")";
                }
            }
        }

        // Hangar clearance envelope check
        if (terminal != null && terminal.isFormed()) {
            BlockPos padCenter = terminal.getPadCenterPos();
            double allowedRadius = RocketAssemblyGraphBuilder.computeAllowedClearanceRadius(
                    terminal.getPlatformSize(),
                    terminal.getActiveBlueprint()
            );
            double partHalf = Math.max(config.diameter(), config.topDiameter()) * 0.5;
            for (BlockPos origin : origins) {
                double dist = Math.hypot(origin.getX() - padCenter.getX(), origin.getZ() - padCenter.getZ());
                if (dist + partHalf > allowedRadius + 1.0e-3) {
                    return "CLEARANCE_OBSTRUCTED";
                }
            }
        }

        return null;
    }

    private static @Nullable HangarTerminalBlockEntity findNearbyFormedTerminal(BlockPos targetPos) {
        HangarTerminalBlockEntity nearest = null;
        double bestDistSq = 28.0 * 28.0;
        for (HangarTerminalBlockEntity terminal : HangarTerminalBlockEntity.getActiveClientTerminals()) {
            if (terminal == null || terminal.isRemoved() || !terminal.isFormed()) {
                continue;
            }
            BlockPos padCenter = terminal.getPadCenterPos();
            double dx = targetPos.getX() - padCenter.getX();
            double dz = targetPos.getZ() - padCenter.getZ();
            double dSq = dx * dx + dz * dz;
            if (dSq < bestDistSq) {
                bestDistSq = dSq;
                nearest = terminal;
            }
        }
        return nearest;
    }

    private static int resolveCoreDiameterAtY(ClientLevel level, BlockPos padCenter, int worldY) {
        BlockPos axisPos = new BlockPos(padCenter.getX(), worldY, padCenter.getZ());
        ModularRocketPartBlockEntity coreBe = resolvePartAt(level, axisPos);
        if (coreBe != null) {
            return coreBe.getConfig().diameter();
        }
        ModularRocketPartBlockEntity baseBe = resolvePartAt(level, padCenter.above());
        if (baseBe != null) {
            return baseBe.getConfig().diameter();
        }
        return 1;
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
