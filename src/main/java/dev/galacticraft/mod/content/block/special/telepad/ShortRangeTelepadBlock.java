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

package dev.galacticraft.mod.content.block.special.telepad;

import com.mojang.serialization.MapCodec;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.block.entity.machine.ShortRangeTelepadBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShortRangeTelepadBlock extends BaseEntityBlock {
    public static final MapCodec<ShortRangeTelepadBlock> CODEC = simpleCodec(ShortRangeTelepadBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    private static final Map<UUID, BlockPos> PENDING_LINKS = new ConcurrentHashMap<>();

    public ShortRangeTelepadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShortRangeTelepadBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, GCBlockEntityTypes.SHORT_RANGE_TELEPAD, ShortRangeTelepadBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player) {
            BlockPos pending = PENDING_LINKS.get(player.getUUID());
            if (pending != null && !pending.equals(pos)
                    && pos.distSqr(pending) <= ShortRangeTelepadBlockEntity.MAX_RANGE * ShortRangeTelepadBlockEntity.MAX_RANGE
                    && level.getBlockEntity(pending) instanceof ShortRangeTelepadBlockEntity otherTelepad
                    && otherTelepad.getTargetPos() == null
                    && level.getBlockEntity(pos) instanceof ShortRangeTelepadBlockEntity thisTelepad) {
                thisTelepad.setTargetPos(pending.immutable());
                otherTelepad.setTargetPos(pos.immutable());
                PENDING_LINKS.remove(player.getUUID());
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
                player.sendSystemMessage(Component.translatable("chat.galacticraft.telepad.target", pending.toShortString()));
            } else {
                PENDING_LINKS.put(player.getUUID(), pos.immutable());
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ShortRangeTelepadBlockEntity telepad) {
                telepad.teleport(entity);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShortRangeTelepadBlockEntity telepad) {
            BlockPos pending = PENDING_LINKS.get(player.getUUID());
            if (pending != null && !pending.equals(pos)
                    && pos.distSqr(pending) <= ShortRangeTelepadBlockEntity.MAX_RANGE * ShortRangeTelepadBlockEntity.MAX_RANGE
                    && level.getBlockEntity(pending) instanceof ShortRangeTelepadBlockEntity otherTelepad) {
                telepad.setTargetPos(pending.immutable());
                otherTelepad.setTargetPos(pos.immutable());
                PENDING_LINKS.remove(player.getUUID());
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
                player.sendSystemMessage(Component.translatable("chat.galacticraft.telepad.target", pending.toShortString()));
            } else {
                PENDING_LINKS.put(player.getUUID(), pos.immutable());
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.6F, 1.4F);
                if (telepad.getTargetPos() != null) {
                    player.sendSystemMessage(Component.translatable("chat.galacticraft.telepad.target", telepad.getTargetPos().toShortString()));
                } else {
                    player.sendSystemMessage(Component.literal("Telepad selecionado em [" + pos.toShortString() + "]. Clique com botão direito em outro Telepad para vincular."));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
