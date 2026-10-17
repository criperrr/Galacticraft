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

import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.item.GCItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class GrappleHookEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> IN_GROUND = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PULLING = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.BOOLEAN);

    private int ticksInGround = 0;

    public GrappleHookEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public GrappleHookEntity(LivingEntity shooter, Level level) {
        super(GCEntityTypes.GRAPPLE, shooter, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IN_GROUND, false);
        builder.define(PULLING, false);
    }

    @Override
    protected Item getDefaultItem() {
        return GCItems.GRAPPLE_HOOK;
    }

    public boolean isInGround() {
        return this.entityData.get(IN_GROUND);
    }

    public boolean isPulling() {
        return this.entityData.get(PULLING);
    }

    @Override
    public void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.entityData.set(IN_GROUND, true);
            this.entityData.set(PULLING, true);
            this.setDeltaMovement(Vec3.ZERO);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            this.entityData.set(IN_GROUND, true);
            this.entityData.set(PULLING, true);
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity owner = (LivingEntity) this.getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) {
                this.discard();
            }
            return;
        }

        if (this.isInGround()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.ticksInGround++;

            // Release if player sneaks or after 20 seconds (400 ticks)
            if (owner.isShiftKeyDown() || this.ticksInGround > 400) {
                if (!this.level().isClientSide) {
                    this.discard();
                }
                return;
            }

            if (this.isPulling()) {
                Vec3 toHook = this.position().subtract(owner.position());
                double dist = toHook.length();

                if (dist < 2.0) {
                    if (!this.level().isClientSide) {
                        this.discard();
                    }
                } else {
                    Vec3 pullMotion = toHook.normalize().scale(0.85);
                    owner.setDeltaMovement(pullMotion);
                    owner.hurtMarked = true;
                    owner.resetFallDistance();

                    if (this.level().isClientSide && this.random.nextInt(2) == 0) {
                        this.level().addParticle(ParticleTypes.CRIT, owner.getX(), owner.getY() + 0.5, owner.getZ(), 0, 0, 0);
                    }
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("InGround", this.isInGround());
        compound.putBoolean("Pulling", this.isPulling());
        compound.putInt("TicksInGround", this.ticksInGround);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(IN_GROUND, compound.getBoolean("InGround"));
        this.entityData.set(PULLING, compound.getBoolean("Pulling"));
        this.ticksInGround = compound.getInt("TicksInGround");
    }
}
