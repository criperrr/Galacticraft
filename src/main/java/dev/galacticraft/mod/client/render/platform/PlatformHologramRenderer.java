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

package dev.galacticraft.mod.client.render.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprint;
import dev.galacticraft.mod.content.block.platform.PlatformCollisionRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * World-space mini-block hologram guide renderer for unformed modular rocket platforms.
 *
 * <p>Hooked into {@code WorldRenderEvents.AFTER_TRANSLUCENT}. Uses standard Minecraft block and line
 * rendering pipelines with per-terminal 5-tick diff caching for zero per-frame allocations and full
 * compatibility with Sodium 0.6, Iris, and ImmediatelyFast.
 */
@Environment(EnvType.CLIENT)
public final class PlatformHologramRenderer {
    private static final double MAX_TERMINAL_DIST_SQ = 96.0 * 96.0;
    private static final double MAX_CELL_DIST_SQ = 64.0 * 64.0;
    private static final long DIFF_REFRESH_INTERVAL_TICKS = 5L;

    private static final float MISSING_MINI_SCALE = 0.40F;
    private static final double MISSING_MINI_OFFSET = (1.0 - MISSING_MINI_SCALE) * 0.5;
    private static final float WRONG_MINI_SCALE = 0.35F;
    private static final double WRONG_MINI_OFFSET = (1.0 - WRONG_MINI_SCALE) * 0.5;

    private static final Map<HangarTerminalBlockEntity, DiffCache> CACHE = new WeakHashMap<>();

    private PlatformHologramRenderer() {
    }

    /**
     * Clears all cached hologram diffs (e.g. on client world unload or resource reload).
     */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Renders hologram mini-block guides and error wireframes for all nearby unformed hangar terminals.
     */
    public static void render(WorldRenderContext context) {
        ClientLevel level = context.world();
        PoseStack poseStack = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        if (level == null || poseStack == null || consumers == null) {
            return;
        }

        Set<HangarTerminalBlockEntity> terminals = HangarTerminalBlockEntity.getActiveClientTerminals();
        if (terminals.isEmpty()) {
            return;
        }

        Vec3 cameraPos = context.camera().getPosition();
        double camX = cameraPos.x;
        double camY = cameraPos.y;
        double camZ = cameraPos.z;
        Frustum frustum = context.frustum();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        long gameTime = level.getGameTime();
        float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(false);
        float animPhase = ((float) (gameTime % 24000L) + partialTick) * 0.25F;
        float pulse = 0.65F + 0.35F * Mth.sin(animPhase);

        boolean renderedAny = false;

        for (HangarTerminalBlockEntity terminal : terminals) {
            if (terminal == null || terminal.isRemoved() || terminal.getLevel() != level) {
                continue;
            }
            if (terminal.isFormed()) {
                if (!terminal.isAxisGuideEnabled()) {
                    continue;
                }
                BlockPos padCenter = terminal.getPadCenterPos();
                if (padCenter == null) {
                    continue;
                }
                BlockPos terminalPos = terminal.getBlockPos();
                if (terminalPos.distToCenterSqr(camX, camY, camZ) > MAX_TERMINAL_DIST_SQ
                        && padCenter.distToCenterSqr(camX, camY, camZ) > MAX_TERMINAL_DIST_SQ) {
                    continue;
                }
                PlatformBlueprint bp = terminal.getActiveBlueprint();
                int guideTopY = padCenter.getY() + Math.max(12, bp.maxY() + 8);
                VertexConsumer axisBuf = consumers.getBuffer(RenderType.lines());
                // Subtle vertical coaxial alignment column above the pad center
                LevelRenderer.renderLineBox(
                        poseStack,
                        axisBuf,
                        padCenter.getX() + 0.44 - camX,
                        padCenter.getY() + 0.52 - camY,
                        padCenter.getZ() + 0.44 - camZ,
                        padCenter.getX() + 0.56 - camX,
                        guideTopY - camY,
                        padCenter.getZ() + 0.56 - camZ,
                        0.20F,
                        0.88F,
                        1.0F,
                        0.45F
                );
                // Center pad alignment ring on deck level
                double half = terminal.getPlatformSize().diameter() * 0.5;
                LevelRenderer.renderLineBox(
                        poseStack,
                        axisBuf,
                        padCenter.getX() + 0.5 - half - camX,
                        padCenter.getY() + 0.52 - camY,
                        padCenter.getZ() + 0.5 - half - camZ,
                        padCenter.getX() + 0.5 + half - camX,
                        padCenter.getY() + 0.56 - camY,
                        padCenter.getZ() + 0.5 + half - camZ,
                        0.25F,
                        0.95F,
                        0.65F,
                        0.55F
                );
                renderedAny = true;
                continue;
            }
            if (!terminal.isPreviewEnabled()) {
                continue;
            }

            BlockPos terminalPos = terminal.getBlockPos();
            BlockPos padCenter = terminal.getPadCenterPos();
            if (padCenter == null) {
                continue;
            }

            if (terminalPos.distToCenterSqr(camX, camY, camZ) > MAX_TERMINAL_DIST_SQ
                    && padCenter.distToCenterSqr(camX, camY, camZ) > MAX_TERMINAL_DIST_SQ) {
                continue;
            }

            DiffCache diff = getOrUpdateDiff(terminal, level, padCenter, gameTime);
            List<MissingCell> missingCells = diff.missingCells;
            List<WrongCell> wrongCells = diff.wrongCells;

            // Sub-pass 1: Render rock-solid 3D mini-blocks using entityCutoutNoCull on BLOCK_ATLAS
            // with double-precision camera-relative coordinates so vertices never drift or leak across passes.
            if (!missingCells.isEmpty() || !wrongCells.isEmpty()) {
                VertexConsumer blockBuf = consumers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
                int missingSize = missingCells.size();
                for (int i = 0; i < missingSize; i++) {
                    MissingCell cell = missingCells.get(i);
                    if (!isCellVisible(cell.x, cell.y, cell.z, cell.bounds, camX, camY, camZ, frustum)) {
                        continue;
                    }
                    poseStack.pushPose();
                    poseStack.translate(
                            (double) cell.x - camX + MISSING_MINI_OFFSET,
                            (double) cell.y - camY + MISSING_MINI_OFFSET,
                            (double) cell.z - camZ + MISSING_MINI_OFFSET
                    );
                    poseStack.scale(MISSING_MINI_SCALE, MISSING_MINI_SCALE, MISSING_MINI_SCALE);
                    BakedModel bakedModel = blockRenderer.getBlockModel(cell.expectedState);
                    blockRenderer.getModelRenderer().renderModel(
                            poseStack.last(),
                            blockBuf,
                            cell.expectedState,
                            bakedModel,
                            1.0F,
                            1.0F,
                            1.0F,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY
                    );
                    poseStack.popPose();
                }

                int wrongSize = wrongCells.size();
                for (int i = 0; i < wrongSize; i++) {
                    WrongCell cell = wrongCells.get(i);
                    if (!isCellVisible(cell.x, cell.y, cell.z, cell.bounds, camX, camY, camZ, frustum)) {
                        continue;
                    }
                    poseStack.pushPose();
                    poseStack.translate(
                            (double) cell.x - camX + WRONG_MINI_OFFSET,
                            (double) cell.y - camY + 1.05,
                            (double) cell.z - camZ + WRONG_MINI_OFFSET
                    );
                    poseStack.scale(WRONG_MINI_SCALE, WRONG_MINI_SCALE, WRONG_MINI_SCALE);
                    BakedModel bakedModel = blockRenderer.getBlockModel(cell.expectedState);
                    blockRenderer.getModelRenderer().renderModel(
                            poseStack.last(),
                            blockBuf,
                            cell.expectedState,
                            bakedModel,
                            1.0F,
                            0.55F,
                            0.55F,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY
                    );
                    poseStack.popPose();
                }
                renderedAny = true;
            }

            // Sub-pass 2: Render full occupied platform footprint & 3D volume box + guide/error boxes
            VertexConsumer lineBuf = consumers.getBuffer(RenderType.lines());

            // Full occupied platform bounding volume (shows total 3D space the assembled platform will occupy)
            PlatformCollisionRegistry.Bounds occ = PlatformCollisionRegistry.getOccupiedBounds(
                    terminal.getPlatformType(),
                    terminal.getPlatformSize()
            );
            Direction facing = terminal.getFacing();
            BlockPos cornerA = PlatformBlueprint.toWorldPos(padCenter, facing, new BlockPos(occ.minX(), occ.minY(), occ.minZ()));
            BlockPos cornerB = PlatformBlueprint.toWorldPos(padCenter, facing, new BlockPos(occ.maxX(), occ.maxY(), occ.maxZ()));
            double occMinX = Math.min(cornerA.getX(), cornerB.getX()) - camX;
            double occMinY = Math.min(cornerA.getY(), cornerB.getY()) - camY;
            double occMinZ = Math.min(cornerA.getZ(), cornerB.getZ()) - camZ;
            double occMaxX = Math.max(cornerA.getX(), cornerB.getX()) + 1.0 - camX;
            double occMaxY = Math.max(cornerA.getY(), cornerB.getY()) + 1.0 - camY;
            double occMaxZ = Math.max(cornerA.getZ(), cornerB.getZ()) + 1.0 - camZ;

            // Ground footprint perimeter (bright amber-gold at deck base)
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuf,
                    occMinX - 0.01,
                    occMinY + 0.02,
                    occMinZ - 0.01,
                    occMaxX + 0.01,
                    occMinY + 0.12,
                    occMaxZ + 0.01,
                    1.0F,
                    0.78F,
                    0.18F,
                    0.85F
            );
            // Full 3D occupied volume outer wireframe (soft amber-cyan envelope)
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuf,
                    occMinX - 0.01,
                    occMinY + 0.02,
                    occMinZ - 0.01,
                    occMaxX + 0.01,
                    occMaxY,
                    occMaxZ + 0.01,
                    0.95F,
                    0.65F,
                    0.15F,
                    0.35F
            );
            renderedAny = true;

            int missingSize = missingCells.size();
            for (int i = 0; i < missingSize; i++) {
                MissingCell cell = missingCells.get(i);
                if (!isCellVisible(cell.x, cell.y, cell.z, cell.bounds, camX, camY, camZ, frustum)) {
                    continue;
                }
                LevelRenderer.renderLineBox(
                        poseStack,
                        lineBuf,
                        (double) cell.x - camX + 0.28,
                        (double) cell.y - camY + 0.28,
                        (double) cell.z - camZ + 0.28,
                        (double) cell.x - camX + 0.72,
                        (double) cell.y - camY + 0.72,
                        (double) cell.z - camZ + 0.72,
                        0.15F,
                        0.85F,
                        1.0F,
                        0.75F
                );
            }

            float redAlpha = 0.55F + 0.35F * pulse;
            int wrongSize = wrongCells.size();
            for (int i = 0; i < wrongSize; i++) {
                WrongCell cell = wrongCells.get(i);
                if (!isCellVisible(cell.x, cell.y, cell.z, cell.bounds, camX, camY, camZ, frustum)) {
                    continue;
                }
                LevelRenderer.renderLineBox(
                        poseStack,
                        lineBuf,
                        (double) cell.x - camX - 0.002,
                        (double) cell.y - camY - 0.002,
                        (double) cell.z - camZ - 0.002,
                        (double) cell.x - camX + 1.002,
                        (double) cell.y - camY + 1.002,
                        (double) cell.z - camZ + 1.002,
                        1.0F,
                        0.15F,
                        0.15F,
                        redAlpha
                );
            }
        }

        if (renderedAny && consumers instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
            bufferSource.endBatch(RenderType.lines());
        }
    }

    private static boolean isCellVisible(
            int x,
            int y,
            int z,
            AABB bounds,
            double camX,
            double camY,
            double camZ,
            Frustum frustum
    ) {
        double dx = (x + 0.5) - camX;
        double dy = (y + 0.5) - camY;
        double dz = (z + 0.5) - camZ;
        if (dx * dx + dy * dy + dz * dz > MAX_CELL_DIST_SQ) {
            return false;
        }
        return frustum == null || frustum.isVisible(bounds);
    }

    private static DiffCache getOrUpdateDiff(
            HangarTerminalBlockEntity terminal,
            ClientLevel level,
            BlockPos padCenter,
            long gameTime
    ) {
        DiffCache cache = CACHE.get(terminal);
        if (cache == null) {
            cache = new DiffCache();
            CACHE.put(terminal, cache);
        }

        long configVersion = terminal.getClientConfigVersion();
        int previewLayerY = terminal.getPreviewLayerY();
        Direction facing = terminal.getFacing();

        boolean stale = !cache.initialized
                || cache.lastConfigVersion != configVersion
                || cache.lastPreviewLayerY != previewLayerY
                || cache.lastFacing != facing
                || !Objects.equals(cache.lastPadCenter, padCenter)
                || Math.abs(gameTime - cache.lastUpdateTick) >= DIFF_REFRESH_INTERVAL_TICKS;

        if (!stale) {
            return cache;
        }

        cache.initialized = true;
        cache.lastUpdateTick = gameTime;
        cache.lastConfigVersion = configVersion;
        cache.lastPreviewLayerY = previewLayerY;
        cache.lastFacing = facing;
        cache.lastPadCenter = padCenter.immutable();
        cache.missingCells.clear();
        cache.wrongCells.clear();

        PlatformBlueprint blueprint = terminal.getActiveBlueprint();
        if (blueprint == null) {
            return cache;
        }

        BlockPos terminalPos = terminal.getBlockPos();
        List<PlatformBlueprint.Entry> entries = blueprint.entries();
        for (int i = 0; i < entries.size(); i++) {
            PlatformBlueprint.Entry entry = entries.get(i);
            if (previewLayerY >= 0 && entry.localPos().getY() != previewLayerY) {
                continue;
            }

            BlockPos worldPos = PlatformBlueprint.toWorldPos(padCenter, facing, entry.localPos());
            if (worldPos.equals(terminalPos) || !level.isLoaded(worldPos)) {
                continue;
            }

            Block expectedBlock = entry.role().getBlock();
            if (expectedBlock == null) {
                continue;
            }

            BlockState current = level.getBlockState(worldPos);
            if (current.is(expectedBlock)) {
                // Matching blocks are completely hidden from the hologram guide.
                continue;
            }

            BlockState expectedState = expectedBlock.defaultBlockState();
            int x = worldPos.getX();
            int y = worldPos.getY();
            int z = worldPos.getZ();

            if (current.isAir() || current.canBeReplaced()) {
                cache.missingCells.add(new MissingCell(x, y, z, expectedState, new AABB(x, y, z, x + 1.0, y + 1.0, z + 1.0)));
            } else {
                cache.wrongCells.add(new WrongCell(x, y, z, expectedState, new AABB(x, y, z, x + 1.0, y + 1.45, z + 1.0)));
            }
        }

        return cache;
    }

    private static final class DiffCache {
        boolean initialized;
        long lastUpdateTick;
        long lastConfigVersion;
        int lastPreviewLayerY;
        Direction lastFacing;
        BlockPos lastPadCenter;
        final List<MissingCell> missingCells = new ArrayList<>();
        final List<WrongCell> wrongCells = new ArrayList<>();
    }

    private record MissingCell(int x, int y, int z, BlockState expectedState, AABB bounds) {
    }

    private record WrongCell(int x, int y, int z, BlockState expectedState, AABB bounds) {
    }
}
