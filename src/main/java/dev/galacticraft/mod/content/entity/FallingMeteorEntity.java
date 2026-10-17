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

package dev.galacticraft.mod.content.entity;

import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.environment.FallenMeteorBlock;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static dev.galacticraft.mod.content.GCBlocks.FALLEN_METEOR;
import static dev.galacticraft.mod.content.entity.damage.GCDamageTypes.METEOR_STRIKE;

public class FallingMeteorEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData.defineId(FallingMeteorEntity.class, EntityDataSerializers.INT);

    public FallingMeteorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public int getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSize(int size) {
        this.entityData.set(DATA_SIZE, Mth.clamp(size, 1, 7));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Ensure falling meteors remain visible high in the sky up to 256 blocks away
        return distance < 65536.0;
    }

    private void fallTo(Position position) {
        int size = this.getSize();
        float explosionRadius = Mth.clamp(1.6F + size * 0.65F, 2.0F, 6.0F);

        this.level().explode(
                this,
                position.x(), position.y(), position.z(),
                explosionRadius,
                false,
                Level.ExplosionInteraction.BLOCK);

        this.level().playSound(
                null,
                position.x(), position.y(), position.z(),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.WEATHER,
                4.0F,
                0.55F + this.random.nextFloat() * 0.2F
        );

        BlockPos impactCenter = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(position));
        decorateImpactCrater(impactCenter, size);

        BlockPos meteorPos = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, impactCenter);
        BlockPos belowPos = meteorPos.below();
        if (this.level().getBlockState(belowPos).isAir()) {
            this.level().setBlock(belowPos, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (this.level().isEmptyBlock(meteorPos) || this.level().getBlockState(meteorPos).canBeReplaced()) {
            this.level().setBlock(
                    meteorPos,
                    FALLEN_METEOR.defaultBlockState().setValue(FallenMeteorBlock.HEAT, 5),
                    Block.UPDATE_ALL);
        }

        this.discard();
    }

    private void decorateImpactCrater(BlockPos center, int size) {
        boolean isVenus = this.level().dimension().equals(GCDimensions.VENUS);
        int scorchRadius = Mth.clamp(1 + size / 2, 2, 4);

        for (int dx = -scorchRadius; dx <= scorchRadius; dx++) {
            for (int dz = -scorchRadius; dz <= scorchRadius; dz++) {
                double distSq = dx * dx + dz * dz;
                if (distSq > (double) (scorchRadius * scorchRadius)) continue;

                BlockPos topPos = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz)).below();
                BlockState existing = this.level().getBlockState(topPos);
                if (existing.isAir() || existing.is(Blocks.BEDROCK) || existing.is(FALLEN_METEOR)) continue;

                float roll = this.random.nextFloat();
                if (distSq <= 2.2 && size >= 3 && roll < 0.35F) {
                    this.level().setBlock(topPos, pickMeteorImpactOre(), Block.UPDATE_CLIENTS);
                } else if (roll < 0.45F) {
                    BlockState scorched = roll < 0.14F
                            ? Blocks.MAGMA_BLOCK.defaultBlockState()
                            : (roll < 0.26F
                                    ? Blocks.OBSIDIAN.defaultBlockState()
                                    : (isVenus ? GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState() : Blocks.BASALT.defaultBlockState()));
                    this.level().setBlock(topPos, scorched, Block.UPDATE_CLIENTS);
                }
            }
        }

        // Guarantee at least one meteoric ore block embedded beneath larger meteors (size >= 3)
        if (size >= 3) {
            BlockPos corePos = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center).below();
            if (!this.level().getBlockState(corePos).is(Blocks.BEDROCK)) {
                this.level().setBlock(corePos, pickMeteorImpactOre(), Block.UPDATE_ALL);
            }
        }
    }

    private BlockState pickMeteorImpactOre() {
        float roll = this.random.nextFloat();
        if (roll < 0.25F) return GCBlocks.RAW_METEORIC_IRON_BLOCK.defaultBlockState();
        if (roll < 0.45F) return GCBlocks.GALENA_ORE.defaultBlockState();
        if (roll < 0.62F) return GCBlocks.ILMENITE_ORE.defaultBlockState();
        if (roll < 0.78F) return GCBlocks.SOLAR_ORE.defaultBlockState();
        if (roll < 0.90F) return Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState();
        return Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
    }

    private void onImpact(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            entityHitResult.getEntity().hurt(this.damageSources().source(METEOR_STRIKE), 20.0f);
        }

        this.fallTo(hitResult.getLocation());
    }

    private void spawnParticles() {
        if (this.level().isClientSide && this.isAlive()) {
            Vec3 motion = this.getDeltaMovement();
            double x = this.getX();
            double y = this.getY() + 0.5;
            double z = this.getZ();

            // Long-range fiery contrail visible across the sky (up to 256 blocks)
            for (int i = 0; i < 4; i++) {
                double progress = i * 0.25;
                double px = x - motion.x * progress + (this.random.nextDouble() - 0.5) * 0.6;
                double py = y - motion.y * progress + (this.random.nextDouble() - 0.5) * 0.6;
                double pz = z - motion.z * progress + (this.random.nextDouble() - 0.5) * 0.6;

                this.level().addAlwaysVisibleParticle(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        true,
                        px, py, pz,
                        -motion.x * 0.08 + (this.random.nextDouble() - 0.5) * 0.03,
                        0.04,
                        -motion.z * 0.08 + (this.random.nextDouble() - 0.5) * 0.03
                );
                this.level().addAlwaysVisibleParticle(
                        ParticleTypes.LARGE_SMOKE,
                        true,
                        px, py, pz,
                        -motion.x * 0.15,
                        -motion.y * 0.1,
                        -motion.z * 0.15
                );
                this.level().addAlwaysVisibleParticle(
                        ParticleTypes.FLAME,
                        true,
                        px, py, pz,
                        -motion.x * 0.2 + (this.random.nextDouble() - 0.5) * 0.08,
                        -motion.y * 0.15,
                        -motion.z * 0.2 + (this.random.nextDouble() - 0.5) * 0.08
                );
            }
            if (this.random.nextInt(2) == 0) {
                this.level().addAlwaysVisibleParticle(
                        ParticleTypes.LAVA,
                        true,
                        x, y, z,
                        (this.random.nextDouble() - 0.5) * 0.2,
                        0.1,
                        (this.random.nextDouble() - 0.5) * 0.2
                );
            }
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.035;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.tickCount > 400) {
                this.discard();
                return;
            }
            if (this.tickCount % 12 == 1) {
                this.level().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH,
                        SoundSource.WEATHER,
                        2.5F,
                        0.45F + this.random.nextFloat() * 0.15F
                );
            }
            HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, Entity::canBeHitByProjectile);
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.onImpact(hitResult);
                return;
            } else if (this.onGround() || this.verticalCollision || this.horizontalCollision) {
                this.fallTo(this.position());
                return;
            }
        }

        this.setRot(this.getYRot() + 4.5f, this.getXRot() + 5.5f);

        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.handlePortal();

        this.spawnParticles();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder compositeStateBuilder) {
        compositeStateBuilder.define(DATA_SIZE, 1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("Size")) {
            this.setSize(compound.getInt("Size"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("Size", this.getSize());
    }
}
