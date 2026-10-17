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

package dev.galacticraft.mod.content.block.environment;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FallenMeteorBlock extends FallingBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<FallenMeteorBlock> CODEC = simpleCodec(FallenMeteorBlock::new);
    // Matches the 9-element GC4 irregular meteorite model bounds (2.5..13.5 X/Z, 0.0..11.5 Y)
    private static final VoxelShape SHAPE = Shapes.or(
            box(3.0, 0.5, 3.0, 13.0, 10.5, 13.0),
            box(5.0, 0.0, 5.0, 11.0, 0.5, 11.5),
            box(2.5, 2.0, 2.5, 13.5, 11.5, 13.5)
    );
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty HEAT = IntegerProperty.create("heat", 0, 5);
    private static final float FALL_DAMAGE_PER_DISTANCE = 2.0F;
    private static final int FALL_DAMAGE_MAX = 40;
    private final int[] COOLING_RATES = {0, 10, 8, 6, 4, 2};

    public FallenMeteorBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, false).setValue(HEAT, 0));
    }

    @Override
    protected MapCodec<FallenMeteorBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HEAT) > 0;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int i = state.getValue(HEAT);

        int coolingRate = COOLING_RATES[i];

        if (state.getValue(WATERLOGGED)) {
            coolingRate /= 2;
        }

        if (random.nextInt(coolingRate) == 0) {
            level.setBlock(pos, state.setValue(HEAT, i - 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void falling(FallingBlockEntity entity) {
        entity.setHurtsEntities(FALL_DAMAGE_PER_DISTANCE, FALL_DAMAGE_MAX);
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState fallingBlockState, BlockState currentStateInPos, FallingBlockEntity fallingBlockEntity) {
        super.onLand(level, pos, fallingBlockState, currentStateInPos, fallingBlockEntity);

        level.playSound(fallingBlockEntity,
                pos,
                SoundEvents.METAL_FALL,
                SoundSource.BLOCKS,
                0.6F,
                1.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int heat = state.getValue(HEAT);
        if (heat <= 0) {
            return;
        }

        double spread = heat > 3 ? (heat - 3) * 0.03D : 0.01D;

        if (state.getValue(WATERLOGGED)) {
            for (var i = 0; i < heat; ++i) {
                level.addParticle(
                        ParticleTypes.CLOUD,
                        pos.getX() + random.nextDouble(),
                        pos.getY() + 0.2D + random.nextDouble(),
                        pos.getZ() + random.nextDouble(),
                        (random.nextDouble() - 0.5D) * spread,
                        heat * 0.01D,
                        (random.nextDouble() - 0.5D) * spread
                );
            }

            if (random.nextInt(40) == 0) {
                level.playLocalSound(
                        pos,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        heat * 0.12F,
                        0.8F,
                        true
                );
            }
        } else {
            // Hot fallen meteors in dry/planetary environments emit rising smoke and embers
            if (random.nextInt(7 - heat) == 0) {
                double px = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
                double py = pos.getY() + 0.55D + random.nextDouble() * 0.3D;
                double pz = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
                level.addParticle(
                        heat >= 4 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                        px, py, pz,
                        (random.nextDouble() - 0.5D) * spread,
                        0.02D + heat * 0.008D,
                        (random.nextDouble() - 0.5D) * spread
                );
                if (heat >= 4 && random.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.FLAME, px, py, pz, 0.0D, 0.015D, 0.0D);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, HEAT);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState newState, LevelAccessor level, BlockPos pos, BlockPos posFrom) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, newState, level, pos, posFrom);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidState.is(FluidTags.WATER) && fluidState.getAmount() == 8);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockView, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(HEAT) <= 0) {
            return;
        }

        if (entity instanceof Player player) {
            if (player.isCreative()) return;
        }

        if (entity instanceof LivingEntity livingEntity) {

            if (entity.fireImmune() || livingEntity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                return;
            }

            level.playSound(null, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);

            for (var i = 0; i < 8; ++i) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + Math.random(), pos.getY() + 0.2D + Math.random(), pos.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
            }

            livingEntity.igniteForSeconds(1);

            var knockX = livingEntity.getX() - (pos.getX());
            var knockY = livingEntity.getY() + livingEntity.getBbHeight() / 2.0D - (pos.getY());
            double knockZ;

            for (knockZ = livingEntity.getZ() - (pos.getZ()); knockX * knockX + knockZ * knockZ < 1.0E-4D; knockZ = (Math.random() - Math.random()) * 0.01D) {
                knockX = (Math.random() - Math.random()) * 0.01D;
            }

            meteorKnockback(livingEntity, 0.3F, knockX, knockY, knockZ);
        }
    }

    protected void meteorKnockback(LivingEntity entity, double strength, double x, double y, double z) {
        strength *= 1.0D - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);

        if (strength <= 0.0D) {
            return;
        }

        Vec3 direction = new Vec3(x, y, z);

        if (direction.horizontalDistanceSqr() < 1.0E-5D) {
            direction = new Vec3(
                    (Math.random() - Math.random()) * 0.01D,
                    y,
                    (Math.random() - Math.random()) * 0.01D
            );
        }

        direction = direction.normalize().scale(strength);

        Vec3 velocity = entity.getDeltaMovement();

        entity.hasImpulse = true;

        entity.setDeltaMovement(
                velocity.scale(0.5D).add(direction)
        );
    }

    @Override
    public void spawnAfterBreak(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, ItemStack itemStack, boolean bl) {
        if (bl && EnchantmentHelper.getItemEnchantmentLevel(serverLevel.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), itemStack) == 0) {
            var i = Mth.nextInt(serverLevel.random, 3, 7);
            if (i > 0) {
                this.popExperience(serverLevel, blockPos, i);
            }
        }
    }

    public static int colorMultiplier(BlockState state, BlockAndTintGetter blockView, BlockPos pos) {
        return switch (state.getValue(HEAT)) {
            case 1 -> FastColor.ARGB32.color(255, 235, 220, 205);
            case 2 -> FastColor.ARGB32.color(255, 245, 200, 165);
            case 3 -> FastColor.ARGB32.color(255, 255, 175, 125);
            case 4 -> FastColor.ARGB32.color(255, 255, 145, 90);
            case 5 -> FastColor.ARGB32.color(255, 255, 115, 65);
            default -> FastColor.ARGB32.color(255, 215, 210, 205);
        };
    }
}