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

package dev.galacticraft.mod.content.block.rocket;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Master anchor block for a world-placed modular rocket part. One concrete block instance is
 * registered per {@link RocketPartCategory}, while exact geometry, dimensions, livery, and
 * physical stats are stored on the backing {@link ModularRocketPartBlockEntity}.
 */
public class ModularRocketPartBlock extends BaseEntityBlock {
    public static final MapCodec<ModularRocketPartBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RocketPartCategory.CODEC.fieldOf("category").forGetter(ModularRocketPartBlock::getCategory),
            propertiesCodec()
    ).apply(instance, ModularRocketPartBlock::new));

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HATCH_OPEN = BooleanProperty.create("hatch_open");

    private final RocketPartCategory category;

    public ModularRocketPartBlock(@NotNull RocketPartCategory category, Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        this.category = Objects.requireNonNull(category, "category");
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HATCH_OPEN, false));
    }

    public @NotNull RocketPartCategory getCategory() {
        return this.category;
    }

    /**
     * Resolves the canonical registered {@link ModularRocketPartBlock} backing {@code category}.
     */
    public static @NotNull Block getBlockForCategory(@NotNull RocketPartCategory category) {
        Objects.requireNonNull(category, "category");
        return switch (category) {
            case COMMAND -> GCBlocks.ROCKET_PART_COMMAND;
            case ENGINE -> GCBlocks.ROCKET_PART_ENGINE;
            case TANK -> GCBlocks.ROCKET_PART_TANK;
            case ADAPTER -> GCBlocks.ROCKET_PART_ADAPTER;
            case PAYLOAD -> GCBlocks.ROCKET_PART_PAYLOAD;
            case UTILITY -> GCBlocks.ROCKET_PART_UTILITY;
        };
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HATCH_OPEN);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HATCH_OPEN) ? BoundsShape.FLOOR_4PX.getShape() : Shapes.block();
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HATCH_OPEN) ? BoundsShape.FLOOR_4PX.getCollisionShape() : Shapes.block();
    }

    @Override
    protected @NotNull VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HATCH_OPEN, false);
    }

    @Override
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModularRocketPartBlockEntity(pos, state);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity be && be.isSciencePayloadModule()) {
            ItemInteractionResult res = be.handleScienceModuleInteraction(level, player, hand, stack);
            if (res != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                return res;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity be) {
            if (be.isSciencePayloadModule() && !be.getScienceDrive().isEmpty()) {
                ItemInteractionResult res = be.handleScienceModuleInteraction(level, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                if (res.consumesAction()) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            if (be.getConfig().walkableInterior()) {
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                be.toggleHatch();
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity be) {
            be.tearDown(level, pos, player, !player.isCreative());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity be) {
                be.tearDown(level, pos, null, true);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (state.isAir() || explosion.getBlockInteraction() == Explosion.BlockInteraction.TRIGGER_BLOCK) {
            return;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity be) {
            Player sourcePlayer = explosion.getIndirectSourceEntity() instanceof Player player ? player : null;
            boolean dropItems = explosion.getBlockInteraction() != Explosion.BlockInteraction.KEEP;
            be.tearDown(level, pos, sourcePlayer, dropItems);
        }
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity be) {
            Player sourcePlayer = explosion.getIndirectSourceEntity() instanceof Player player ? player : null;
            boolean dropItems = explosion.getBlockInteraction() != Explosion.BlockInteraction.KEEP;
            be.tearDown(level, pos, sourcePlayer, dropItems);
        }
        super.wasExploded(level, pos, explosion);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof ModularRocketPartBlockEntity be) {
            return be.createConfiguredStack();
        }
        return ModularRocketPartBlockItem.createConfiguredStack(
                RocketPartCatalog.getInstance().defaultConfigFor(this.category)
        );
    }
}
