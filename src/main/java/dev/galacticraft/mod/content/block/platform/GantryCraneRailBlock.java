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

package dev.galacticraft.mod.content.block.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Overhead structural crane girder and hardened trolley rail block. Automatically switches to its
 * corner variant when connecting to perpendicular adjacent crane rails.
 */
public class GantryCraneRailBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty CORNER = BooleanProperty.create("corner");

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public GantryCraneRailBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CORNER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CORNER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(CORNER, false);
        return this.computeRailState(base, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (direction.getAxis().isHorizontal()) {
            return this.computeRailState(state, level, pos);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private BlockState computeRailState(BlockState currentState, BlockGetter level, BlockPos pos) {
        boolean n = isRail(level, pos.north());
        boolean e = isRail(level, pos.east());
        boolean s = isRail(level, pos.south());
        boolean w = isRail(level, pos.west());

        boolean hasZ = n || s;
        boolean hasX = e || w;
        Direction facing = currentState.getValue(FACING);

        if (hasZ && hasX) {
            Direction cornerFacing;
            if (n && w && !s && !e) {
                cornerFacing = Direction.NORTH;
            } else if (n && e && !s && !w) {
                cornerFacing = Direction.EAST;
            } else if (s && e && !n && !w) {
                cornerFacing = Direction.SOUTH;
            } else if (s && w && !n && !e) {
                cornerFacing = Direction.WEST;
            } else if (n && w) {
                cornerFacing = Direction.NORTH;
            } else if (n && e) {
                cornerFacing = Direction.EAST;
            } else if (s && e) {
                cornerFacing = Direction.SOUTH;
            } else {
                cornerFacing = Direction.WEST;
            }
            return currentState.setValue(CORNER, true).setValue(FACING, cornerFacing);
        }

        if (hasZ) {
            Direction aligned = facing.getAxis() == Direction.Axis.X ? facing : Direction.EAST;
            return currentState.setValue(CORNER, false).setValue(FACING, aligned);
        }
        if (hasX) {
            Direction aligned = facing.getAxis() == Direction.Axis.Z ? facing : Direction.NORTH;
            return currentState.setValue(CORNER, false).setValue(FACING, aligned);
        }
        return currentState.setValue(CORNER, false);
    }

    private static boolean isRail(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof GantryCraneRailBlock;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
