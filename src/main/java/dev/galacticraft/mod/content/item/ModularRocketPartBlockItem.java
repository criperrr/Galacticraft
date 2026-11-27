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
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition.SlotMaterialEntry;
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
import dev.galacticraft.mod.content.rocket.part.RocketMaterialRegistry;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.CostProfile;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.SlotContribution;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import java.util.LinkedHashMap;
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
            PartIntegrity integrity = stack.get(GCDataComponents.PART_INTEGRITY);
            RocketPartComposition composition = stack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
            RocketPartStats stats = RocketPartCostCalculator.calculate(config, composition).stats();
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
        buildHoverTooltip(stack, this.category, tooltip, isShiftKeyDown());
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /**
     * Populates the full Tinkers'-style aerospace engineering tooltip for {@code stack}.
     * Exposed as a static helper so unit tests and headless suites can verify both summary and
     * {@code SHIFT}-expanded breakdowns deterministically.
     */
    public static void buildHoverTooltip(
            @NotNull ItemStack stack,
            @NotNull RocketPartCategory fallbackCategory,
            @NotNull List<Component> tooltip,
            boolean shiftDown
    ) {
        RocketPartConfig config = getConfig(stack, fallbackCategory);
        RocketPartComposition composition = stack.get(GCDataComponents.ROCKET_PART_COMPOSITION);
        boolean hasComposition = composition != null && !composition.isEmpty();

        CostProfile baseCost = RocketPartCostCalculator.calculate(config);
        CostProfile effectiveCost = hasComposition
                ? RocketPartCostCalculator.calculate(config, composition)
                : baseCost;

        RocketPartStats baseStats = baseCost.stats();
        RocketPartStats stats = effectiveCost.stats();
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

        if (hasComposition && !shiftDown) {
            appendWrappedConstructionLines(composition, tooltip);
            tooltip.add(Component.literal("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500").withStyle(ChatFormatting.DARK_GRAY));
        }

        // Dry Mass with % vs Base delta
        MutableComponent massLine = Component.literal("Dry Mass: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(Locale.ROOT, "%.1f kg", stats.dryWeightKg())).withStyle(ChatFormatting.WHITE));
        if (hasComposition && baseStats.dryWeightKg() > 0.0) {
            int massPct = (int) Math.round(((stats.dryWeightKg() - baseStats.dryWeightKg()) / baseStats.dryWeightKg()) * 100.0);
            if (massPct != 0) {
                ChatFormatting color = massPct < 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                String sign = massPct > 0 ? "+" : "";
                massLine.append(Component.literal(" (" + sign + massPct + "% vs Base)").withStyle(color));
            }
        }
        tooltip.add(massLine);

        // Thrust with % delta
        if (stats.thrustKn() > 0.0) {
            MutableComponent thrustLine = Component.literal("Thrust: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.ROOT, "%.1f kN", stats.thrustKn())).withStyle(ChatFormatting.GOLD));
            if (hasComposition && baseStats.thrustKn() > 0.0) {
                int thrustPct = (int) Math.round(((stats.thrustKn() - baseStats.thrustKn()) / baseStats.thrustKn()) * 100.0);
                if (thrustPct != 0) {
                    ChatFormatting color = thrustPct > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                    String sign = thrustPct > 0 ? "+" : "";
                    thrustLine.append(Component.literal(" (" + sign + thrustPct + "%)").withStyle(color));
                }
            }
            tooltip.add(thrustLine);
        }

        // Propellant Capacity with % delta
        if (stats.fuelCapacityDroplets() > 0L) {
            double buckets = stats.fuelCapacityDroplets() / DROPLETS_PER_BUCKET;
            MutableComponent fuelLine = Component.literal("Propellant: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format(Locale.ROOT, "%.1f B", buckets)).withStyle(ChatFormatting.YELLOW));
            if (hasComposition && baseStats.fuelCapacityDroplets() > 0L) {
                int capPct = (int) Math.round(((double) (stats.fuelCapacityDroplets() - baseStats.fuelCapacityDroplets())
                        / (double) baseStats.fuelCapacityDroplets()) * 100.0);
                if (capPct != 0) {
                    ChatFormatting color = capPct > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                    String sign = capPct > 0 ? "+" : "";
                    fuelLine.append(Component.literal(" (" + sign + capPct + "%)").withStyle(color));
                }
            }
            tooltip.add(fuelLine);
        }

        // Tolerance (K / atm) + separate compact Boil-Off line when relevant
        tooltip.add(Component.literal("Tolerance: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(Locale.ROOT, "%,.0f K / %.1f atm",
                        stats.thermalToleranceKelvin(), stats.pressureToleranceAtm())).withStyle(ChatFormatting.DARK_AQUA)));
        if (hasComposition && (config.category() == RocketPartCategory.TANK || effectiveCost.boilOffMultiplier() < 0.999)) {
            double boilOff = effectiveCost.boilOffMultiplier();
            MutableComponent boilLine = Component.literal("Boil-Off: ").withStyle(ChatFormatting.GRAY);
            if (boilOff <= 0.001) {
                boilLine.append(Component.literal("0.00x (Zero)").withStyle(ChatFormatting.AQUA));
            } else {
                ChatFormatting boilColor = boilOff < 1.0 ? ChatFormatting.GREEN : ChatFormatting.WHITE;
                boilLine.append(Component.literal(String.format(Locale.ROOT, "%.2fx", boilOff)).withStyle(boilColor));
            }
            tooltip.add(boilLine);
        }

        if (hasComposition && composition.hasTrait(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR)) {
            tooltip.add(Component.literal("Power: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("+" + RocketPartCostCalculator.ANEUTRONIC_BOARD_REACTOR_GEN_PER_TICK + " GJ/t (D-3He)")
                            .withStyle(ChatFormatting.AQUA)));
        }

        if (integrity != null) {
            ChatFormatting integrityColor = switch (integrity.damageState()) {
                case NOMINAL -> ChatFormatting.GREEN;
                case WORN -> ChatFormatting.YELLOW;
                case COMPROMISED -> ChatFormatting.GOLD;
                case DAMAGED -> ChatFormatting.RED;
            };
            int pct = (int) Math.round(integrity.fraction() * 100.0);
            String extraMax = Math.abs(integrity.maxIntegrity() - PartIntegrity.DEFAULT_MAX_INTEGRITY) >= 0.5
                    ? String.format(Locale.ROOT, " [%.0f/%.0f]", integrity.currentIntegrity(), integrity.maxIntegrity())
                    : "";
            tooltip.add(Component.literal("Integrity: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(pct + "% (" + formatDamageState(integrity.damageState()) + ")" + extraMax)
                            .withStyle(integrityColor)));
        }

        // Engineering Traits list (compact badges; keeps tooltip cleanly inside screen vertically & horizontally)
        if (hasComposition) {
            List<RocketMaterialTrait> traits = composition.allTraits();
            if (!traits.isEmpty()) {
                tooltip.add(Component.literal("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Engineering Traits:").withStyle(ChatFormatting.GRAY));
                for (RocketMaterialTrait trait : traits) {
                    tooltip.add(Component.literal(" \u2726 [" + trait.displayName() + "]")
                            .setStyle(Style.EMPTY.withColor(trait.rgbColor())));
                }
            }

            if (shiftDown) {
                tooltip.add(Component.literal("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Sub-Component Breakdown (" + composition.size() + " slots):").withStyle(ChatFormatting.AQUA));
                List<SlotContribution> contributions = RocketPartCostCalculator.computeSlotContributions(config, composition);
                for (SlotContribution c : contributions) {
                    String matName = resolveEntryDisplayName(c.profile(), c.itemId());
                    int matColor = c.profile() != null ? c.profile().rgbColor() : 0xCCCCCC;
                    String headerPrefix = " \u2022 " + c.roleLabel() + ": ";
                    if (headerPrefix.length() + matName.length() <= 46) {
                        tooltip.add(Component.literal(headerPrefix).withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(matName).setStyle(Style.EMPTY.withColor(matColor))));
                    } else {
                        tooltip.add(Component.literal(" \u2022 " + c.roleLabel() + ":").withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.literal("   " + matName).setStyle(Style.EMPTY.withColor(matColor)));
                    }
                    appendSlotContributionLines(c, tooltip);
                }
            } else {
                tooltip.add(Component.literal("[Hold SHIFT to inspect " + composition.size() + " sub-components]")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    private static void appendWrappedConstructionLines(RocketPartComposition composition, List<Component> tooltip) {
        Map<String, Integer> distinctMaterials = new LinkedHashMap<>();
        for (SlotMaterialEntry entry : composition.entries()) {
            RocketMaterialProfile profile = RocketMaterialRegistry.resolve(entry).orElse(null);
            String name = resolveEntryDisplayName(profile, entry.itemId());
            int color = profile != null ? profile.rgbColor() : 0xCCCCCC;
            distinctMaterials.putIfAbsent(name, color);
        }
        final int maxLineChars = 42;
        MutableComponent currentLine = Component.literal("Build: ").withStyle(ChatFormatting.GRAY);
        int currentLen = 7;
        boolean firstOnLine = true;
        for (Map.Entry<String, Integer> entry : distinctMaterials.entrySet()) {
            String name = entry.getKey();
            int extraLen = (firstOnLine ? 0 : 3) + name.length();
            if (!firstOnLine && currentLen + extraLen > maxLineChars) {
                tooltip.add(currentLine);
                currentLine = Component.literal("  ");
                currentLen = 2;
                firstOnLine = true;
            }
            if (!firstOnLine) {
                currentLine.append(Component.literal(" \u2022 ").withStyle(ChatFormatting.DARK_GRAY));
                currentLen += 3;
            }
            currentLine.append(Component.literal(name).setStyle(Style.EMPTY.withColor(entry.getValue())));
            currentLen += name.length();
            firstOnLine = false;
        }
        tooltip.add(currentLine);
    }

    private static String resolveEntryDisplayName(@Nullable RocketMaterialProfile profile, net.minecraft.resources.ResourceLocation itemId) {
        if (profile != null) {
            return profile.displayName();
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item != Items.AIR) {
            return new ItemStack(item).getHoverName().getString();
        }
        return formatIdentifier(itemId.getPath());
    }

    private static void appendSlotContributionLines(SlotContribution c, List<Component> tooltip) {
        record DeltaToken(String text, ChatFormatting color) {
        }
        List<DeltaToken> tokens = new java.util.ArrayList<>(6);

        int massPct = (int) Math.round(c.massDeltaFraction() * 100.0);
        if (massPct != 0) {
            tokens.add(new DeltaToken((massPct > 0 ? "+" : "") + massPct + "% Mass", massPct < 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (Math.abs(c.pressureDeltaAtm()) >= 0.1) {
            String sign = c.pressureDeltaAtm() > 0 ? "+" : "";
            tokens.add(new DeltaToken(String.format(Locale.ROOT, "%s%.1f atm", sign, c.pressureDeltaAtm()),
                    c.pressureDeltaAtm() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        int thermalK = (int) Math.round(c.thermalDeltaKelvin());
        if (thermalK != 0) {
            tokens.add(new DeltaToken((thermalK > 0 ? "+" : "") + thermalK + " K", thermalK > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        int thrustPct = (int) Math.round(c.thrustDeltaFraction() * 100.0);
        if (thrustPct != 0) {
            tokens.add(new DeltaToken((thrustPct > 0 ? "+" : "") + thrustPct + "% Thrust", thrustPct > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        int capPct = (int) Math.round(c.capacityDeltaFraction() * 100.0);
        if (capPct != 0) {
            tokens.add(new DeltaToken((capPct > 0 ? "+" : "") + capPct + "% Cap", capPct > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (c.boilOffFactor() < 0.999) {
            if (c.boilOffFactor() <= 0.001) {
                tokens.add(new DeltaToken("0% Boil-Off", ChatFormatting.AQUA));
            } else {
                tokens.add(new DeltaToken(String.format(Locale.ROOT, "%.2fx Boil-Off", c.boilOffFactor()), ChatFormatting.GREEN));
            }
        }
        int integPct = (int) Math.round(c.integrityDeltaFraction() * 100.0);
        if (integPct != 0 && tokens.size() < 4) {
            tokens.add(new DeltaToken((integPct > 0 ? "+" : "") + integPct + "% Integ", integPct > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }

        if (tokens.isEmpty()) {
            tooltip.add(Component.literal("   Baseline component").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        final int maxChars = 44;
        MutableComponent line = Component.literal("   ");
        int lineLen = 3;
        boolean first = true;
        for (DeltaToken token : tokens) {
            int extra = (first ? 0 : 3) + token.text().length();
            if (!first && lineLen + extra > maxChars) {
                tooltip.add(line);
                line = Component.literal("   ");
                lineLen = 3;
                first = true;
            }
            if (!first) {
                line.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
                lineLen += 3;
            }
            line.append(Component.literal(token.text()).withStyle(token.color()));
            lineLen += token.text().length();
            first = false;
        }
        tooltip.add(line);
    }

    private static boolean isShiftKeyDown() {
        try {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
                return false;
            }
            return Screen.hasShiftDown();
        } catch (Throwable ignored) {
            return false;
        }
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
