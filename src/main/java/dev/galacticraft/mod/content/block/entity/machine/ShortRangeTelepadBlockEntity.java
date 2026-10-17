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

package dev.galacticraft.mod.content.block.entity.machine;

import dev.galacticraft.mod.content.GCBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ShortRangeTelepadBlockEntity extends BlockEntity {
    public static final int ENERGY_USAGE = 2500;
    public static final int MAX_RANGE = 256;

    private @Nullable BlockPos targetPos;
    private long energy = 10000;
    private int cooldown = 0;

    public ShortRangeTelepadBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.SHORT_RANGE_TELEPAD, pos, state);
    }

    public @Nullable BlockPos getTargetPos() {
        return this.targetPos;
    }

    public void setTargetPos(@Nullable BlockPos targetPos) {
        this.targetPos = targetPos;
        this.setChanged();
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShortRangeTelepadBlockEntity telepad) {
        if (telepad.cooldown > 0) {
            telepad.cooldown--;
            return;
        }
        if (!level.isClientSide && telepad.canTeleport()) {
            var entities = level.getEntitiesOfClass(
                    Entity.class,
                    new net.minecraft.world.phys.AABB(
                            pos.getX() + 0.1D, pos.getY() + 0.4D, pos.getZ() + 0.1D,
                            pos.getX() + 0.9D, pos.getY() + 1.8D, pos.getZ() + 0.9D
                    ),
                    Entity::isAlive
            );
            if (!entities.isEmpty()) {
                telepad.teleport(entities.get(0));
            }
        }
    }

    public boolean canTeleport() {
        return this.cooldown <= 0 && this.targetPos != null && this.energy >= ENERGY_USAGE;
    }

    public void teleport(Entity entity) {
        if (this.level == null || this.level.isClientSide || !this.canTeleport() || this.targetPos == null) {
            return;
        }

        double distanceSq = this.worldPosition.distSqr(this.targetPos);
        if (distanceSq > MAX_RANGE * MAX_RANGE) {
            return;
        }

        this.energy -= ENERGY_USAGE;
        this.cooldown = 60; // 3 seconds cooldown

        if (this.level.getBlockEntity(this.targetPos) instanceof ShortRangeTelepadBlockEntity destTelepad) {
            destTelepad.setCooldown(60);
        }

        // Play departure sound and particles
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, 30, 0.2, 0.5, 0.2, 0.1);
            serverLevel.playSound(null, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        // Teleport entity above destination telepad
        entity.teleportTo(this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5);

        // Play arrival sound and particles
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5, 30, 0.2, 0.5, 0.2, 0.1);
            serverLevel.playSound(null, this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.targetPos != null) {
            tag.put("TargetPos", NbtUtils.writeBlockPos(this.targetPos));
        }
        tag.putLong("Energy", this.energy);
        tag.putInt("Cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("TargetPos")) {
            this.targetPos = NbtUtils.readBlockPos(tag, "TargetPos").orElse(null);
        } else {
            this.targetPos = null;
        }
        this.energy = tag.getLong("Energy");
        this.cooldown = tag.getInt("Cooldown");
    }
}
