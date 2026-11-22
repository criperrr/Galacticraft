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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reinforced 8-pixel-tall structural launch pad slab block that auto-connects with adjacent
 * {@link ReinforcedLaunchPadBlock} tiles into seamless center, perimeter side, and corner variants.
 */
public class ReinforcedLaunchPadBlock extends Block {
    public static final EnumProperty<PadPart> PART = EnumProperty.create("part", PadPart.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public ReinforcedLaunchPadBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PART, PadPart.STANDALONE)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction defaultFacing = context.getHorizontalDirection().getOpposite();
        BlockState base = this.defaultBlockState().setValue(FACING, defaultFacing);
        return this.computeConnectedState(base, context.getLevel(), context.getClickedPos());
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
            return this.computeConnectedState(state, level, pos);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private BlockState computeConnectedState(BlockState currentState, BlockGetter level, BlockPos pos) {
        boolean n = isPad(level, pos.north());
        boolean e = isPad(level, pos.east());
        boolean s = isPad(level, pos.south());
        boolean w = isPad(level, pos.west());

        int count = (n ? 1 : 0) + (e ? 1 : 0) + (s ? 1 : 0) + (w ? 1 : 0);
        Direction currentFacing = currentState.getValue(FACING);

        if (count == 4) {
            return currentState.setValue(PART, PadPart.CENTER);
        }
        if (count == 3) {
            Direction exposedSide;
            if (!n) {
                exposedSide = Direction.NORTH;
            } else if (!e) {
                exposedSide = Direction.EAST;
            } else if (!s) {
                exposedSide = Direction.SOUTH;
            } else {
                exposedSide = Direction.WEST;
            }
            return currentState.setValue(PART, PadPart.SIDE).setValue(FACING, exposedSide);
        }
        if (count == 2) {
            if (s && e) {
                return currentState.setValue(PART, PadPart.CORNER).setValue(FACING, Direction.NORTH);
            }
            if (s && w) {
                return currentState.setValue(PART, PadPart.CORNER).setValue(FACING, Direction.EAST);
            }
            if (n && w) {
                return currentState.setValue(PART, PadPart.CORNER).setValue(FACING, Direction.SOUTH);
            }
            if (n && e) {
                return currentState.setValue(PART, PadPart.CORNER).setValue(FACING, Direction.WEST);
            }
            return currentState.setValue(PART, PadPart.CENTER).setValue(FACING, currentFacing);
        }
        return currentState.setValue(PART, PadPart.STANDALONE).setValue(FACING, currentFacing);
    }

    private static boolean isPad(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ReinforcedLaunchPadBlock;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public enum PadPart implements StringRepresentable {
        STANDALONE("standalone"),
        CENTER("center"),
        SIDE("side"),
        CORNER("corner");

        private final String serializedName;

        PadPart(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }
    }
}
