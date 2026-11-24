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

import dev.galacticraft.mod.particle.GCParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * Non-solid trapped vapor block created when cryogenic or volatile liquid propellants evaporate
 * inside an enclosed room or dome. Persists while the dome remains airtight, grants breathability
 * when formed from Liquid Oxygen, exhibits realistic buoyancy (H2/CH4 rise to ceilings, Xe/Hydrazine
 * pool along floors), and depressurizes to air immediately if the dome wall is breached.
 */
public class PropellantGasBlock extends Block {
    public static final EnumProperty<GasKind> GAS = EnumProperty.create("gas", GasKind.class);

    public PropellantGasBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GAS, GasKind.OXYGEN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GAS);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && state.getValue(GAS).breathable()) {
            level.setBreathable(pos, true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !newState.is(this) && state.getValue(GAS).breathable()) {
            if (!level.getDefaultBreathable() && !level.galacticraft$getSealerManager().isBlockSealed(pos)) {
                level.setBreathable(pos, false);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        GasKind kind = state.getValue(GAS);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (kind.flammable() && PropellantEvaporation.isIgnitionSource(neighborState)) {
            PropellantEvaporation.igniteFlammableGas(serverLevel, pos);
            return;
        }

        // If an adjacent block opened up (potential dome wall breach), schedule containment re-check
        if (neighborState.isAir() || (!neighborState.is(this) && !PropellantEvaporation.isAirtightBoundary(level, neighborPos, neighborState))) {
            level.scheduleTick(pos, this, 3);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PropellantEvaporation.checkDomeContainment(level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (PropellantEvaporation.checkDomeContainment(level, pos)) {
            return;
        }
        if (state.getValue(GAS).breathable()) {
            level.setBreathable(pos, true);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        GasKind kind = state.getValue(GAS);
        if (kind.toxic() && (level.getGameTime() % 20L) == 0L) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, true));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        GasKind kind = state.getValue(GAS);
        if (random.nextFloat() < 0.28F) {
            double x = pos.getX() + 0.1 + random.nextDouble() * 0.8;
            double y = pos.getY() + 0.1 + random.nextDouble() * 0.8;
            double z = pos.getZ() + 0.1 + random.nextDouble() * 0.8;
            double vy = kind.buoyancy() * 0.015;
            level.addParticle(kind.createMistParticle(), x, y, z, (random.nextDouble() - 0.5) * 0.01, vy, (random.nextDouble() - 0.5) * 0.01);
        }
        if (kind == GasKind.OXYGEN && random.nextFloat() < 0.08F) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(GCParticleTypes.CRYOGENIC_PARTICLE, x, y, z, 0.0, 0.008, 0.0);
        }
    }

    public enum GasKind implements StringRepresentable {
        OXYGEN("oxygen", 0, 0.50F, 0.88F, 1.00F, false, false, true),
        HYDROGEN("hydrogen", 1, 0.80F, 0.96F, 1.00F, true, false, false),
        METHANE("methane", 1, 0.32F, 0.92F, 0.72F, true, false, false),
        HYDRAZINE("hydrazine", -1, 0.92F, 0.42F, 0.84F, true, true, false),
        XENON("xenon", -1, 0.70F, 0.52F, 1.00F, false, false, false);

        private final String serializedName;
        private final int buoyancy;
        private final float r;
        private final float g;
        private final float b;
        private final boolean flammable;
        private final boolean toxic;
        private final boolean breathable;

        GasKind(String serializedName, int buoyancy, float r, float g, float b, boolean flammable, boolean toxic, boolean breathable) {
            this.serializedName = serializedName;
            this.buoyancy = buoyancy;
            this.r = r;
            this.g = g;
            this.b = b;
            this.flammable = flammable;
            this.toxic = toxic;
            this.breathable = breathable;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }

        public int buoyancy() {
            return this.buoyancy;
        }

        public boolean flammable() {
            return this.flammable;
        }

        public boolean toxic() {
            return this.toxic;
        }

        public boolean breathable() {
            return this.breathable;
        }

        public DustParticleOptions createMistParticle() {
            return new DustParticleOptions(new Vector3f(this.r, this.g, this.b), 1.35F);
        }
    }
}
