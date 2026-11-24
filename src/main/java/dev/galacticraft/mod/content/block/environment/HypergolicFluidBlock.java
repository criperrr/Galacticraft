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

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Hypergolic hydrazine monopropellant block (N2H4, boiling point 113.5°C at 1 bar) that remains liquid
 * under temperate atmospheres, evaporates into toxic trapped vapor inside enclosed domes when exposed
 * to vacuum or extreme heat (Venus, Nether, lava/fire), and poisons unprotected living entities.
 */
public class HypergolicFluidBlock extends LiquidBlock {
    private final PropellantGasBlock.GasKind gasKind;
    private final double boilingPointC;

    public HypergolicFluidBlock(
            FlowingFluid fluid,
            Properties properties,
            PropellantGasBlock.GasKind gasKind,
            double boilingPointC
    ) {
        super(fluid, properties);
        this.gasKind = gasKind;
        this.boilingPointC = boilingPointC;
    }

    public PropellantGasBlock.GasKind getGasKind() {
        return this.gasKind;
    }

    public double getBoilingPointC() {
        return this.boilingPointC;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && PropellantEvaporation.shouldEvaporate(level, pos, this.boilingPointC)) {
            int delay = PropellantEvaporation.computeBoilDelayTicks(level, pos, this.boilingPointC);
            level.scheduleTick(pos, this, delay);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && PropellantEvaporation.shouldEvaporate(level, pos, this.boilingPointC)) {
            int delay = PropellantEvaporation.computeBoilDelayTicks(level, pos, this.boilingPointC);
            level.scheduleTick(pos, this, delay);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PropellantEvaporation.evaporateLiquidBlock(level, pos, state, this.gasKind, this.boilingPointC);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        PropellantEvaporation.evaporateLiquidBlock(level, pos, state, this.gasKind, this.boilingPointC);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!level.isClientSide && entity instanceof LivingEntity living && (level.getGameTime() % 20L) == 0L) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        boolean boiling = PropellantEvaporation.shouldEvaporate(level, pos, this.boilingPointC);
        float chance = boiling ? 0.45F : 0.16F;
        if (random.nextFloat() < chance && !level.getBlockState(pos.above()).isCollisionShapeFullBlock(level, pos.above())) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.WITCH, x, y, z, 0.0, boiling ? 0.04 : 0.02, 0.0);
            if (boiling) {
                level.addParticle(this.gasKind.createMistParticle(), x, y + 0.05, z, 0.0, 0.025, 0.0);
            }
        }
    }
}
