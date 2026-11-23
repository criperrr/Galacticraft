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

package dev.galacticraft.mod.content.item;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.rocket.part.data.PartDamageState;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.FootprintMode;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.block.multiblock.PlatformProxyBlock;
import dev.galacticraft.mod.content.block.multiblock.RocketPartBoundsBlock;
import dev.galacticraft.mod.content.block.platform.PlatformPadDetector;
import dev.galacticraft.mod.content.block.platform.ReinforcedLaunchPadBlock;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * {@link BlockItem} for {@link ModularRocketPartBlock} supporting DataComponent-backed part
 * identity, coaxial auto-snapping onto existing rocket parts and launch platforms, multi-voxel
 * footprint clearance validation, and rich aerospace telemetry tooltips.
 */
public class ModularRocketPartBlockItem extends BlockItem {
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final double DROPLETS_PER_BUCKET = 81_000.0;

    private final RocketPartCategory category;

    public ModularRocketPartBlockItem(@NotNull Block block, Properties properties) {
        super(block, properties);
        this.category = block instanceof ModularRocketPartBlock partBlock
                ? partBlock.getCategory()
                : RocketPartCategory.COMMAND;
    }

    public @NotNull RocketPartCategory getCategory() {
        return this.category;
    }

    public @NotNull RocketPartConfig getConfig(@NotNull ItemStack stack) {
        return getConfig(stack, this.category);
    }

    public static @NotNull RocketPartConfig getConfig(@NotNull ItemStack stack, @NotNull RocketPartCategory fallbackCategory) {
        RocketPartConfig config = stack.get(GCDataComponents.ROCKET_PART_CONFIG);
        return config != null ? config : RocketPartCatalog.getInstance().defaultConfigFor(fallbackCategory);
    }

    /**
     * Creates a configured {@link ItemStack} for the supplied {@link RocketPartConfig} with derived
     * physical stats and pristine structural integrity.
     */
    public static @NotNull ItemStack createConfiguredStack(@NotNull RocketPartConfig config) {
        return createConfiguredStack(config, null, null, null);
    }

    /**
     * Creates a configured {@link ItemStack} with optional custom physical stats, integrity, and
     * per-slot {@link RocketPartComposition}.
     */
    public static @NotNull ItemStack createConfiguredStack(
            @NotNull RocketPartConfig config,
            @Nullable RocketPartStats stats,
            @Nullable PartIntegrity integrity,
            @Nullable RocketPartComposition composition
    ) {
        Objects.requireNonNull(config, "config");
        ItemStack stack = new ItemStack(ModularRocketPartBlock.getBlockForCategory(config.category()));
        stack.set(GCDataComponents.ROCKET_PART_CONFIG, config);
        stack.set(GCDataComponents.ROCKET_PART_STATS, stats != null ? stats : RocketPartCostCalculator.calculate(config).stats());
        stack.set(GCDataComponents.PART_INTEGRITY, integrity != null ? integrity : PartIntegrity.pristine());
        if (composition != null && !composition.isEmpty()) {
            stack.set(GCDataComponents.ROCKET_PART_COMPOSITION, composition);
        }
        return stack;
    }

    /**
     * Resolves the horizontal facing for placing {@code config}: radial decouplers orient outward
     * along the clicked horizontal face, while standard coaxial parts face the player.
     */
    public static @NotNull Direction resolvePlacementFacing(
            @NotNull BlockPlaceContext context,
            @NotNull RocketPartConfig config
    ) {
        Direction clickedFace = context.getClickedFace();
        if (config.radial() && clickedFace.getAxis().isHorizontal()) {
            return clickedFace;
        }
        // When snapping coaxially onto an existing rocket part, inherit its facing so vertical
        // hull seams, raceways, and directional liveries connect seamlessly from bottom to top.
        Level level = context.getLevel();
        if (level != null && !config.radial()) {
            BlockPos hitPos = context.replacingClickedOnBlock()
                    ? context.getClickedPos()
                    : context.getClickedPos().relative(clickedFace.getOpposite());
            ModularRocketPartBlockEntity existingMaster = resolveRocketPartMaster(level, hitPos, level.getBlockState(hitPos));
            if (existingMaster != null && !existingMaster.getConfig().radial()) {
                return existingMaster.getFacing();
            }
            BlockPos belowPos = context.getClickedPos().below();
            ModularRocketPartBlockEntity belowMaster = resolveRocketPartMaster(level, belowPos, level.getBlockState(belowPos));
            if (belowMaster != null && !belowMaster.getConfig().radial()) {
                return belowMaster.getFacing();
            }
        }
        return context.getHorizontalDirection().getOpposite();
    }

    /**
     * Resolves the world-space bottom-center master anchor origin for placing {@code config},
     * applying coaxial auto-snapping onto existing rocket parts and launch platforms, as well as
     * lateral attachment onto radial decouplers.
     */
    public static @NotNull BlockPos resolvePlacementOrigin(
            @NotNull Level level,
            @NotNull BlockPlaceContext context,
            @NotNull RocketPartConfig config,
            @NotNull Direction facing
    ) {
        Direction clickedFace = context.getClickedFace();
        BlockPos hitPos = context.replacingClickedOnBlock()
                ? context.getClickedPos()
                : context.getClickedPos().relative(clickedFace.getOpposite());
        BlockState hitState = level.getBlockState(hitPos);

        // 1. Coaxial Auto-Snap or Radial Decoupler Lateral Snap onto an existing rocket part
        ModularRocketPartBlockEntity existingMaster = resolveRocketPartMaster(level, hitPos, hitState);
        if (existingMaster != null) {
            if (config.radial() && clickedFace.getAxis().isHorizontal()) {
                return context.getClickedPos();
            }
            if (existingMaster.getConfig().radial() && clickedFace.getAxis().isHorizontal() && !config.radial()) {
                // Attach booster/tank/engine laterally flush against the radial decoupler
                int halfSpan = Math.max(0, config.diameter() / 2);
                BlockPos decouplerPos = existingMaster.getMasterPos();
                return decouplerPos.relative(clickedFace, 1 + halfSpan);
            }
            BlockPos masterPos = existingMaster.getMasterPos();
            int targetY = clickedFace == Direction.DOWN
                    ? masterPos.getY() - config.heightBlocks()
                    : masterPos.getY() + existingMaster.getConfig().heightBlocks();
            return new BlockPos(masterPos.getX(), targetY, masterPos.getZ());
        }

        // 1b. If the block directly below the clicked placement spot is a rocket part, snap coaxially to it
        if (!config.radial()) {
            BlockPos belowPos = context.getClickedPos().below();
            ModularRocketPartBlockEntity belowMaster = resolveRocketPartMaster(level, belowPos, level.getBlockState(belowPos));
            if (belowMaster != null && !belowMaster.getConfig().radial()) {
                BlockPos masterPos = belowMaster.getMasterPos();
                return new BlockPos(masterPos.getX(), masterPos.getY() + belowMaster.getConfig().heightBlocks(), masterPos.getZ());
            }
        }

        // 2. Coaxial Auto-Snap onto an assembled PlatformProxyBlock linked to a HangarTerminalBlockEntity
        if (!config.radial() && hitState.getBlock() instanceof PlatformProxyBlock) {
            if (MultiblockLinkManager.getMaster(level, hitPos) instanceof HangarTerminalBlockEntity terminal) {
                BlockPos padCenter = terminal.getPadCenterPos();
                return new BlockPos(padCenter.getX(), padCenter.getY() + 1, padCenter.getZ());
            }
        }

        // 3. Coaxial Auto-Snap onto an unformed ReinforcedLaunchPadBlock deck
        if (!config.radial() && hitState.getBlock() instanceof ReinforcedLaunchPadBlock) {
            BlockPos padCenter = detectPadCenterFromBlock(level, hitPos);
            return new BlockPos(padCenter.getX(), padCenter.getY() + 1, padCenter.getZ());
        }

        return context.getClickedPos();
    }

    /**
     * Validates that every cell in the rotated multi-voxel footprint of {@code config} anchored at
     * {@code origin} is within world build bounds and replaceable.
     */
    public static boolean canPlaceFootprint(
            @NotNull LevelReader level,
            @NotNull BlockPos origin,
            @NotNull RocketPartConfig config,
            @NotNull Direction facing
    ) {
        Map<BlockPos, BoundsShape> localFootprint = FootprintMode.computeLocalFootprint(config, false);
        for (BlockPos localPos : localFootprint.keySet()) {
            BlockPos worldPos = FootprintMode.toWorldPos(origin, facing, localPos);
            if (worldPos.getY() < level.getMinBuildHeight() || worldPos.getY() >= level.getMaxBuildHeight()
                    || !level.getWorldBorder().isWithinBounds(worldPos)) {
                return false;
            }
            BlockState existing = level.getBlockState(worldPos);
            if (!existing.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        RocketPartConfig config = this.getConfig(stack);
        Direction facing = resolvePlacementFacing(context, config);
        BlockPos origin = resolvePlacementOrigin(level, context, config, facing);

        if (!canPlaceFootprint(level, origin, config, facing)) {
            return InteractionResult.FAIL;
        }

        Block targetBlock = ModularRocketPartBlock.getBlockForCategory(config.category());
        BlockState placementState = targetBlock.defaultBlockState()
                .setValue(ModularRocketPartBlock.FACING, facing)
                .setValue(ModularRocketPartBlock.HATCH_OPEN, false);

        if (!level.setBlock(origin, placementState, Block.UPDATE_ALL_IMMEDIATE)) {
            return InteractionResult.FAIL;
        }

        if (level.getBlockEntity(origin) instanceof ModularRocketPartBlockEntity be) {
            RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
            PartIntegrity integrity = stack.get(GCDataComponents.PART_INTEGRITY);
            RocketPartComposition composition = stack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
            be.initializePart(config, stats, integrity, composition, facing);
        }

        if (!level.isClientSide && player != null) {
            pushPlayerOutOfFootprint(level, player, origin, config, facing);
        }

        targetBlock.setPlacedBy(level, origin, placementState, player, stack);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, origin, stack);
        }

        SoundType soundType = placementState.getSoundType();
        level.playSound(
                player,
                origin,
                soundType.getPlaceSound(),
                SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F
        );
        level.gameEvent(GameEvent.BLOCK_PLACE, origin, GameEvent.Context.of(player, placementState));

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        RocketPartConfig config = stack.get(GCDataComponents.ROCKET_PART_CONFIG);
        if (config == null) {
            return super.getName(stack);
        }

        RocketPartSpec spec = RocketPartCatalog.getInstance().getPart(config.modelId());
        String baseName = spec != null ? spec.displayName() : formatIdentifier(config.familyId());

        String dimSuffix;
        if (config.radial()) {
            dimSuffix = String.format(Locale.ROOT, "(%db Radial)", config.heightBlocks());
        } else if (config.diameter() != config.topDiameter()) {
            dimSuffix = String.format(
                    Locale.ROOT,
                    "(%dx%d\u2192%dx%d, %db)",
                    config.diameter(), config.diameter(),
                    config.topDiameter(), config.topDiameter(),
                    config.heightBlocks()
            );
        } else {
            dimSuffix = String.format(
                    Locale.ROOT,
                    "(%dx%d, %db)",
                    config.diameter(), config.diameter(),
                    config.heightBlocks()
            );
        }

        return Component.literal(baseName + " " + dimSuffix);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RocketPartConfig config = this.getConfig(stack);
        RocketPartStats stats = stack.get(GCDataComponents.ROCKET_PART_STATS);
        if (stats == null) {
            stats = RocketPartCostCalculator.calculate(config).stats();
        }
        PartIntegrity integrity = stack.get(GCDataComponents.PART_INTEGRITY);

        tooltip.add(Component.literal("Category: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(config.category().displayName()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" | Livery: ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(config.livery().displayName())
                        .setStyle(Style.EMPTY.withColor(config.livery().rgbColor()))));

        String dimText = config.radial()
                ? String.format(Locale.ROOT, "1x1 Radial, Height %db", config.heightBlocks())
                : config.diameter() != config.topDiameter()
                ? String.format(Locale.ROOT, "%dx%d \u2192 %dx%d, Height %db",
                config.diameter(), config.diameter(), config.topDiameter(), config.topDiameter(), config.heightBlocks())
                : String.format(Locale.ROOT, "%dx%d, Height %db", config.diameter(), config.diameter(), config.heightBlocks());
        tooltip.add(Component.literal("Dimensions: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(dimText).withStyle(ChatFormatting.WHITE)));

        if (config.walkableInterior()) {
            tooltip.add(Component.literal("Walkable Interior (Right-Click Hatch)").withStyle(ChatFormatting.GREEN));
        }

        tooltip.add(Component.literal("Dry Mass: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(Locale.ROOT, "%.1f kg", stats.dryWeightKg())).withStyle(ChatFormatting.WHITE)));

        if (stats.thrustKn() > 0.0) {
            tooltip.add(Component.literal("Thrust: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.ROOT, "%.1f kN", stats.thrustKn())).withStyle(ChatFormatting.GOLD)));
        }

        if (stats.fuelCapacityDroplets() > 0L) {
            double buckets = stats.fuelCapacityDroplets() / DROPLETS_PER_BUCKET;
            tooltip.add(Component.literal("Propellant Capacity: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.ROOT, "%.1f B", buckets)).withStyle(ChatFormatting.YELLOW)));
        }

        tooltip.add(Component.literal("Tolerance: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(Locale.ROOT, "%.0f K / %.1f atm",
                        stats.thermalToleranceKelvin(), stats.pressureToleranceAtm())).withStyle(ChatFormatting.DARK_AQUA)));

        if (integrity != null) {
            ChatFormatting integrityColor = switch (integrity.damageState()) {
                case NOMINAL -> ChatFormatting.GREEN;
                case WORN -> ChatFormatting.YELLOW;
                case COMPROMISED -> ChatFormatting.GOLD;
                case DAMAGED -> ChatFormatting.RED;
            };
            int pct = (int) Math.round(integrity.fraction() * 100.0);
            tooltip.add(Component.literal("Integrity: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(pct + "% (" + formatDamageState(integrity.damageState()) + ")")
                            .withStyle(integrityColor)));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }

    /**
     * Prevents the placing player from getting trapped/suffocated inside newly placed large
     * multi-voxel rocket parts by smoothly moving them just outside the footprint or onto the top.
     */
    private static void pushPlayerOutOfFootprint(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull BlockPos origin,
            @NotNull RocketPartConfig config,
            @NotNull Direction facing
    ) {
        AABB playerBox = player.getBoundingBox();
        Map<BlockPos, BoundsShape> localFootprint = FootprintMode.computeLocalFootprint(config, false);
        boolean trapped = false;
        for (Map.Entry<BlockPos, BoundsShape> entry : localFootprint.entrySet()) {
            if (entry.getValue() == BoundsShape.OPEN_HATCH) {
                continue;
            }
            BlockPos worldPos = FootprintMode.toWorldPos(origin, facing, entry.getKey());
            if (playerBox.intersects(new AABB(worldPos))) {
                trapped = true;
                break;
            }
        }
        if (!trapped) {
            return;
        }

        double centerX = origin.getX() + 0.5;
        double centerZ = origin.getZ() + 0.5;
        double dx = player.getX() - centerX;
        double dz = player.getZ() - centerZ;
        double horizDist = Math.hypot(dx, dz);
        double dirX;
        double dirZ;
        if (horizDist > 0.05) {
            dirX = dx / horizDist;
            dirZ = dz / horizDist;
        } else {
            dirX = facing.getStepX();
            dirZ = facing.getStepZ();
        }
        double safeRadius = (Math.max(config.diameter(), config.topDiameter()) * 0.5) + 0.85;
        double targetX = centerX + dirX * safeRadius;
        double targetZ = centerZ + dirZ * safeRadius;
        double targetY = player.getY();

        AABB horizontalCandidate = playerBox.move(targetX - player.getX(), 0.0, targetZ - player.getZ());
        if (level.noCollision(player, horizontalCandidate)) {
            player.teleportTo(targetX, targetY, targetZ);
            return;
        }

        double topY = origin.getY() + config.heightBlocks() + 0.05;
        player.teleportTo(player.getX(), topY, player.getZ());
    }

    private static ModularRocketPartBlockEntity resolveRocketPartMaster(Level level, BlockPos hitPos, BlockState hitState) {
        if (hitState.getBlock() instanceof ModularRocketPartBlock
                && level.getBlockEntity(hitPos) instanceof ModularRocketPartBlockEntity be) {
            return be;
        }
        if (hitState.getBlock() instanceof RocketPartBoundsBlock
                && MultiblockLinkManager.getMaster(level, hitPos) instanceof ModularRocketPartBlockEntity be) {
            return be;
        }
        return null;
    }

    private static BlockPos detectPadCenterFromBlock(LevelReader level, BlockPos seedPad) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        LongSet visited = new LongOpenHashSet();
        queue.add(seedPad);
        visited.add(seedPad.asLong());

        int minX = seedPad.getX();
        int maxX = seedPad.getX();
        int minZ = seedPad.getZ();
        int maxZ = seedPad.getZ();
        int count = 0;

        while (!queue.isEmpty() && count < PlatformPadDetector.MAX_BFS_BLOCKS) {
            BlockPos current = queue.poll();
            count++;

            int x = current.getX();
            int z = current.getZ();
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (z > maxZ) {
                maxZ = z;
            }

            if (visited.size() >= PlatformPadDetector.MAX_BFS_BLOCKS) {
                continue;
            }

            for (Direction dir : HORIZONTAL) {
                BlockPos next = current.relative(dir);
                if (visited.size() >= PlatformPadDetector.MAX_BFS_BLOCKS) {
                    break;
                }
                long packed = next.asLong();
                if (!visited.contains(packed) && level.getBlockState(next).getBlock() instanceof ReinforcedLaunchPadBlock) {
                    visited.add(packed);
                    queue.add(next);
                }
            }
        }

        return new BlockPos(Math.floorDiv(minX + maxX, 2), seedPad.getY(), Math.floorDiv(minZ + maxZ, 2));
    }

    private static String formatDamageState(PartDamageState state) {
        String name = state.getSerializedName();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String formatIdentifier(String id) {
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.isEmpty() ? id : sb.toString();
    }
}
