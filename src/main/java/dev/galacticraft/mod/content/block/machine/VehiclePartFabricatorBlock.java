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

package dev.galacticraft.mod.content.block.machine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.api.vehicle.VehicleBenchCategory;
import dev.galacticraft.mod.content.block.entity.VehiclePartFabricatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 2-block-wide ({@code 2x1}) Galacticraft aerospace fabrication bench used to manufacture
 * specialized categories of modular electric vehicle hardware (Chassis, Electric Propulsion,
 * Wheels/Suspension, Avionics/Accessories).
 * <p>
 * {@link BedPart#FOOT} is the main left-hand block owning the {@link VehiclePartFabricatorBlockEntity};
 * {@link BedPart#HEAD} is the right-hand extension block placed at {@code facing.getClockWise()}.
 */
public class VehiclePartFabricatorBlock extends BaseEntityBlock {
    public static final MapCodec<VehiclePartFabricatorBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            VehicleBenchCategory.CODEC.fieldOf("category").forGetter(VehiclePartFabricatorBlock::getCategory),
            propertiesCodec()
    ).apply(instance, VehiclePartFabricatorBlock::new));
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;

    private static final VoxelShape BENCH_SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0),
            Block.box(0.5, 10.0, 0.5, 15.5, 16.0, 15.5)
    );

    private final VehicleBenchCategory category;

    public VehiclePartFabricatorBlock(Properties properties) {
        this(VehicleBenchCategory.UNIVERSAL, properties);
    }

    public VehiclePartFabricatorBlock(VehicleBenchCategory category, Properties properties) {
        super(properties);
        this.category = category;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BedPart.FOOT));
    }

    public @NotNull VehicleBenchCategory getCategory() {
        return this.category;
    }

    public boolean isMultiblock() {
        return this.category != VehicleBenchCategory.UNIVERSAL;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (this.isMultiblock()) {
            BlockPos rightPos = context.getClickedPos().relative(facing.getClockWise());
            Level level = context.getLevel();
            if (!level.getBlockState(rightPos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(rightPos)) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, BedPart.FOOT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && this.isMultiblock() && state.getValue(PART) == BedPart.FOOT) {
            BlockPos rightPos = pos.relative(state.getValue(FACING).getClockWise());
            level.setBlock(rightPos, state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
        }
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.isMultiblock() ? BENCH_SHAPE : Shapes.block();
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (this.isMultiblock() && state.getValue(PART) == BedPart.HEAD) {
            return null;
        }
        return new VehiclePartFabricatorBlockEntity(pos, state);
    }

    private BlockPos getMainPos(BlockPos pos, BlockState state) {
        if (this.isMultiblock() && state.getValue(PART) == BedPart.HEAD) {
            return pos.relative(state.getValue(FACING).getCounterClockWise());
        }
        return pos;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockPos mainPos = this.getMainPos(pos, state);
            if (level.getBlockEntity(mainPos) instanceof VehiclePartFabricatorBlockEntity be) {
                player.openMenu(be);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && this.isMultiblock()) {
            Direction facing = state.getValue(FACING);
            BlockPos otherPos = state.getValue(PART) == BedPart.FOOT
                    ? pos.relative(facing.getClockWise())
                    : pos.relative(facing.getCounterClockWise());
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(PART) != state.getValue(PART)) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof VehiclePartFabricatorBlockEntity be) {
                Containers.dropContents(level, pos, be.getInventory());
            }
            if (!level.isClientSide && this.isMultiblock()) {
                Direction facing = state.getValue(FACING);
                BlockPos otherPos = state.getValue(PART) == BedPart.FOOT
                        ? pos.relative(facing.getClockWise())
                        : pos.relative(facing.getCounterClockWise());
                BlockState otherState = level.getBlockState(otherPos);
                if (otherState.is(this) && otherState.getValue(PART) != state.getValue(PART)) {
                    level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
