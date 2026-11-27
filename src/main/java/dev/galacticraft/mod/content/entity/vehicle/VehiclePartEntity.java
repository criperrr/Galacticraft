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

package dev.galacticraft.mod.content.entity.vehicle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Lightweight child hitbox entity attached to {@code MEDIUM} and {@code LARGE}
 * {@link ModularVehicleEntity} instances to forward damage and localized interactions
 * (e.g., clicking the rear cargo bay vs. front cockpit).
 */
public class VehiclePartEntity extends Entity {
    private @Nullable ModularVehicleEntity parent;
    private @Nullable UUID parentUuid;
    private String partSection = "body";
    private Vec3 localOffset = Vec3.ZERO;
    private EntityDimensions partDimensions = EntityDimensions.scalable(1.2F, 1.0F);

    public VehiclePartEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    public void attachTo(@NotNull ModularVehicleEntity parent, @NotNull String partSection, @NotNull Vec3 localOffset, float width, float height) {
        this.parent = parent;
        this.parentUuid = parent.getUUID();
        this.partSection = partSection;
        this.localOffset = localOffset;
        this.partDimensions = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.syncPositionToParent();
    }

    public @NotNull String getPartSection() {
        return this.partSection;
    }

    public void syncPositionToParent() {
        if (this.parent == null || this.parent.isRemoved()) {
            return;
        }
        float yawRad = (float) Math.toRadians(-this.parent.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double wx = this.parent.getX() + this.localOffset.x * cos - this.localOffset.z * sin;
        double wy = this.parent.getY() + this.localOffset.y;
        double wz = this.parent.getZ() + this.localOffset.x * sin + this.localOffset.z * cos;
        this.setPos(wx, wy, wz);
        this.setYRot(this.parent.getYRot());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.parent == null || this.parent.isRemoved()) {
                this.discard();
                return;
            }
            this.syncPositionToParent();
        }
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.parent != null && !this.parent.isRemoved()) {
            return this.parent.hurt(source, amount);
        }
        return false;
    }

    @Override
    public @NotNull InteractionResult interact(Player player, InteractionHand hand) {
        if (this.parent != null && !this.parent.isRemoved()) {
            return this.parent.interactFromSection(player, hand, this.partSection);
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(Pose pose) {
        return this.partDimensions;
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || this.parent == entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Parent")) {
            this.parentUuid = tag.getUUID("Parent");
        }
        if (tag.contains("Section")) {
            this.partSection = tag.getString("Section");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.parentUuid != null) {
            tag.putUUID("Parent", this.parentUuid);
        }
        tag.putString("Section", this.partSection);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
