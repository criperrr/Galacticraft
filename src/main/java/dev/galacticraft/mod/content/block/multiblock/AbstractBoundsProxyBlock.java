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

package dev.galacticraft.mod.content.block.multiblock;

import com.mojang.serialization.MapCodec;
import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.multiblock.MultiVoxelMaster;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.content.block.entity.ModularRocketPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public abstract class AbstractBoundsProxyBlock extends Block {
    public static final EnumProperty<BoundsShape> SHAPE = EnumProperty.create("shape", BoundsShape.class);

    protected AbstractBoundsProxyBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, BoundsShape.FULL));
    }

    @Override
    protected abstract @NotNull MapCodec<? extends AbstractBoundsProxyBlock> codec();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SHAPE).getShape();
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SHAPE).getCollisionShape();
    }

    @Override
    protected @NotNull VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    private @Nullable MultiVoxelMaster resolveMasterWithFallback(Level level, BlockPos pos) {
        MultiVoxelMaster master = MultiblockLinkManager.getMaster(level, pos);
        if (master != null) {
            return master;
        }
        if (!(this instanceof RocketPartBoundsBlock)) {
            return null;
        }
        int baseChunkX = SectionPos.blockToSectionCoord(pos.getX());
        int baseChunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getChunk(baseChunkX + dx, baseChunkZ + dz, ChunkStatus.FULL, false) instanceof LevelChunk chunk) {
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof ModularRocketPartBlockEntity partBe && partBe.isPartOfStructure(pos)) {
                            return partBe;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            MultiVoxelMaster master = this.resolveMasterWithFallback(level, pos);
            if (master != null) {
                master.onProxyBroken(level, pos, player, !player.isCreative());
            }
            MultiblockLinkManager.unlink(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            MultiVoxelMaster master = this.resolveMasterWithFallback(level, pos);
            if (master != null) {
                master.onProxyBroken(level, pos, null, true);
            }
            MultiblockLinkManager.unlink(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
        if (!level.isClientSide) {
            MultiVoxelMaster master = MultiblockLinkManager.getMaster(level, pos);
            if (master != null) {
                Player sourcePlayer = explosion.getIndirectSourceEntity() instanceof Player player ? player : null;
                boolean dropItems = explosion.getBlockInteraction() != Explosion.BlockInteraction.KEEP;
                master.onProxyBroken(level, pos, sourcePlayer, dropItems);
            }
            MultiblockLinkManager.unlink(level, pos);
        }
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) {
            MultiVoxelMaster master = MultiblockLinkManager.getMaster(level, pos);
            if (master != null) {
                Player sourcePlayer = explosion.getIndirectSourceEntity() instanceof Player player ? player : null;
                boolean dropItems = explosion.getBlockInteraction() != Explosion.BlockInteraction.KEEP;
                master.onProxyBroken(level, pos, sourcePlayer, dropItems);
            }
            MultiblockLinkManager.unlink(level, pos);
        }
        super.wasExploded(level, pos, explosion);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        MultiVoxelMaster master = this.resolveMasterWithFallback(level, pos);
        if (master != null) {
            ItemInteractionResult res = master.onProxyUseItemOn(stack, level, pos, player, hand, hit);
            if (res != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                return res;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        MultiVoxelMaster master = this.resolveMasterWithFallback(level, pos);
        if (master != null) {
            return master.onProxyUse(level, pos, player, hit);
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        MultiVoxelMaster master = MultiblockLinkManager.getMaster(level, pos);
        if (master != null) {
            return master.getProxyCloneItemStack(level, pos);
        }
        return ItemStack.EMPTY;
    }
}
