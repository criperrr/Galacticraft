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

import dev.galacticraft.api.entity.ControllableEntity;
import dev.galacticraft.api.vehicle.HardpointType;
import dev.galacticraft.api.vehicle.SeatRole;
import dev.galacticraft.api.vehicle.SeatSpec;
import dev.galacticraft.api.vehicle.VehicleAnimState;
import dev.galacticraft.api.vehicle.VehicleChassisDefinition;
import dev.galacticraft.api.vehicle.VehicleConfiguration;
import dev.galacticraft.api.vehicle.VehicleDerivedStats;
import dev.galacticraft.api.vehicle.VehicleHardpoint;
import dev.galacticraft.api.vehicle.VehiclePartDefinition;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.api.vehicle.VehicleSizeClass;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.data.GCEntityDataSerializers;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.VehicleCrateItem;
import dev.galacticraft.mod.content.item.VehiclePartItem;
import dev.galacticraft.mod.content.item.VehicleWrenchItem;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Modular planetary vehicle entity supporting physical hardpoint assembly in-world,
 * {@link PlanetaryEnvironment}-coupled ground physics, multi-seat mounting,
 * multi-part child hitboxes for {@code MEDIUM}/{@code LARGE} chassis, and
 * packing into a {@link VehicleCrateItem} for rocket cargo transport.
 */
public class ModularVehicleEntity extends GCVehicle implements ControllableEntity {
    private static final EntityDataAccessor<VehicleConfiguration> DATA_CONFIG = SynchedEntityData.defineId(
            ModularVehicleEntity.class, GCEntityDataSerializers.VEHICLE_CONFIGURATION
    );
    private static final EntityDataAccessor<VehicleAnimState> DATA_ANIM_STATE = SynchedEntityData.defineId(
            ModularVehicleEntity.class, GCEntityDataSerializers.VEHICLE_ANIM_STATE
    );
    private static final EntityDataAccessor<Float> DATA_STEERING = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Boolean> DATA_ENGINE_ON = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_HEADLIGHTS_ON = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_DOORS_OPEN = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_PARKING_BRAKE = SynchedEntityData.defineId(
            ModularVehicleEntity.class, EntityDataSerializers.BOOLEAN
    );

    private VehicleDerivedStats derivedStats = VehicleRegistry.computeStats(VehicleConfiguration.emptyChassis(VehicleRegistry.CHASSIS_SCOUT_ROVER));
    private SimpleContainer cargoInventory = new SimpleContainer(0);
    private final List<VehiclePartEntity> childHitboxes = new ArrayList<>();

    // Transient input state from pilot C2S/Client inputTick
    private boolean inputForward;
    private boolean inputBackward;
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputBrake;
    private int inputHoldTicks;

    // Physics state
    private double forwardSpeed;
    private float steeringAngle;
    public float clientWheelSpin;
    public float clientDoorProgress;
    public float clientPitch;
    public float clientRoll;

    public ModularVehicleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CONFIG, VehicleConfiguration.emptyChassis(VehicleRegistry.CHASSIS_SCOUT_ROVER));
        builder.define(DATA_ANIM_STATE, VehicleAnimState.NONE);
        builder.define(DATA_STEERING, 0.0F);
        builder.define(DATA_SPEED, 0.0F);
        builder.define(DATA_ENGINE_ON, true);
        builder.define(DATA_HEADLIGHTS_ON, true);
        builder.define(DATA_DOORS_OPEN, false);
        builder.define(DATA_PARKING_BRAKE, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_CONFIG.equals(key)) {
            this.recomputeDerivedState(false);
        }
    }

    public @NotNull VehicleConfiguration getConfiguration() {
        return this.entityData.get(DATA_CONFIG);
    }

    public void setConfiguration(@NotNull VehicleConfiguration config) {
        this.entityData.set(DATA_CONFIG, config);
        this.recomputeDerivedState(true);
    }

    public @NotNull VehicleDerivedStats getDerivedStats() {
        return this.derivedStats;
    }

    /**
     * Returns {@code true} when this rover has an enclosed pressurized cabin
     * (e.g. NASA SEV Lunar Cruiser or Mars 6x6 Mobile Habitat), allowing passengers
     * to breathe safely in vacuum/toxic atmospheres without consuming suit oxygen.
     */
    public boolean isPressurizedCabin() {
        return this.derivedStats.pressurizedCabin();
    }

    public @NotNull VehicleChassisDefinition getChassisDefinition() {
        return VehicleRegistry.getChassisOrDefault(this.getConfiguration().chassisId());
    }

    public @NotNull VehicleAnimState getAnimState() {
        return this.entityData.get(DATA_ANIM_STATE);
    }

    public void setAnimState(@NotNull VehicleAnimState state) {
        this.entityData.set(DATA_ANIM_STATE, state);
    }

    public float getSteeringAngle() {
        return this.isControlledByLocalInstance() ? this.steeringAngle : this.entityData.get(DATA_STEERING);
    }

    public float getSyncedSpeed() {
        return this.isControlledByLocalInstance() ? (float) this.forwardSpeed : this.entityData.get(DATA_SPEED);
    }

    public boolean isEngineRunning() {
        return this.entityData.get(DATA_ENGINE_ON);
    }

    public boolean areHeadlightsOn() {
        return this.entityData.get(DATA_HEADLIGHTS_ON);
    }

    public boolean areDoorsOpen() {
        return this.entityData.get(DATA_DOORS_OPEN);
    }

    public boolean isParkingBrakeOn() {
        return this.entityData.get(DATA_PARKING_BRAKE);
    }

    public void handlePanelAction(@NotNull Player player, @NotNull String action, @NotNull String targetSlot, @NotNull String value) {
        switch (action) {
            case "toggle_ignition" -> {
                boolean next = !this.isEngineRunning();
                this.entityData.set(DATA_ENGINE_ON, next);
                this.level().playSound(null, this.blockPosition(), next ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.55F, 1.25F);
            }
            case "toggle_headlights" -> {
                this.entityData.set(DATA_HEADLIGHTS_ON, !this.areHeadlightsOn());
                this.level().playSound(null, this.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.5F, 1.3F);
            }
            case "toggle_doors" -> {
                boolean next = !this.areDoorsOpen();
                this.entityData.set(DATA_DOORS_OPEN, next);
                this.level().playSound(null, this.blockPosition(), next ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 0.7F, 0.95F);
            }
            case "toggle_brake" -> {
                this.entityData.set(DATA_PARKING_BRAKE, !this.isParkingBrakeOn());
                this.level().playSound(null, this.blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 0.45F, 1.4F);
            }
            case "open_cargo" -> {
                if (this.cargoInventory.getContainerSize() > 0) {
                    this.openCargoMenu(player);
                }
            }
            case "set_gauge" -> {
                VehicleHardpoint hp = this.getChassisDefinition().findHardpoint(targetSlot);
                if (hp == null || hp.type() != HardpointType.DASHBOARD) {
                    return;
                }
                VehicleConfiguration cfg = this.getConfiguration();
                if (value.isBlank()) {
                    ResourceLocation prev = cfg.getPart(targetSlot);
                    if (prev != null) {
                        this.setConfiguration(cfg.withoutPart(targetSlot));
                        if (!player.getAbilities().instabuild) {
                            ItemStack ret = GCItems.createVehiclePartStack(prev);
                            if (!ret.isEmpty() && !player.getInventory().add(ret)) {
                                this.spawnAtLocation(ret);
                            }
                        }
                    }
                } else {
                    ResourceLocation nextId = ResourceLocation.tryParse(value);
                    VehiclePartDefinition part = nextId != null ? VehicleRegistry.getPart(nextId) : null;
                    if (part != null && hp.accepts(part.type(), part.sizeClass())) {
                        ResourceLocation prev = cfg.getPart(targetSlot);
                        if (nextId.equals(prev)) {
                            return;
                        }
                        if (!player.getAbilities().instabuild) {
                            ItemStack needed = GCItems.createVehiclePartStack(nextId);
                            int slotIdx = -1;
                            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                ItemStack inv = player.getInventory().getItem(i);
                                if (!inv.isEmpty() && inv.is(needed.getItem())) {
                                    slotIdx = i;
                                    break;
                                }
                            }
                            if (slotIdx < 0) {
                                player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.missing_gauge_item", needed.getHoverName()), true);
                                return;
                            }
                            player.getInventory().getItem(slotIdx).shrink(1);
                            if (prev != null) {
                                ItemStack ret = GCItems.createVehiclePartStack(prev);
                                if (!ret.isEmpty() && !player.getInventory().add(ret)) {
                                    this.spawnAtLocation(ret);
                                }
                            }
                        }
                        this.setConfiguration(cfg.withPart(targetSlot, nextId));
                        this.level().playSound(null, this.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.6F, 1.2F);
                    }
                }
            }
            default -> {
            }
        }
    }

    public @NotNull SimpleContainer getCargoInventory() {
        return this.cargoInventory;
    }

    private void recomputeDerivedState(boolean resizeCargo) {
        VehicleConfiguration cfg = this.getConfiguration();
        this.derivedStats = VehicleRegistry.computeStats(cfg);
        this.refreshDimensions();
        if (resizeCargo) {
            this.resizeCargoInventory(this.derivedStats.totalCargoSlots());
        }
        if (!this.level().isClientSide) {
            this.rebuildChildHitboxes();
        }
    }

    private void resizeCargoInventory(int targetSlots) {
        int clamped = Math.max(0, Math.min(54, targetSlots));
        if (this.cargoInventory.getContainerSize() == clamped) {
            return;
        }
        SimpleContainer previous = this.cargoInventory;
        this.cargoInventory = new SimpleContainer(clamped);
        int keep = Math.min(previous.getContainerSize(), clamped);
        for (int i = 0; i < keep; i++) {
            this.cargoInventory.setItem(i, previous.getItem(i).copy());
        }
        if (!this.level().isClientSide && previous.getContainerSize() > clamped) {
            for (int i = clamped; i < previous.getContainerSize(); i++) {
                ItemStack overflow = previous.getItem(i);
                if (!overflow.isEmpty()) {
                    this.spawnAtLocation(overflow);
                }
            }
        }
    }

    private void rebuildChildHitboxes() {
        for (VehiclePartEntity child : this.childHitboxes) {
            if (!child.isRemoved()) {
                child.discard();
            }
        }
        this.childHitboxes.clear();

        VehicleSizeClass size = this.derivedStats.sizeClass();
        if (size == VehicleSizeClass.SMALL) {
            return;
        }
        double zSpan = size == VehicleSizeClass.LARGE ? 1.45 : 0.95;
        float boxW = size == VehicleSizeClass.LARGE ? 1.95F : 1.55F;
        float boxH = size == VehicleSizeClass.LARGE ? 1.45F : 1.15F;

        VehiclePartEntity front = GCEntityTypes.VEHICLE_PART.create(this.level());
        VehiclePartEntity rear = GCEntityTypes.VEHICLE_PART.create(this.level());
        if (front != null) {
            front.attachTo(this, "front", new Vec3(0.0, 0.1, zSpan), boxW, boxH);
            this.level().addFreshEntity(front);
            this.childHitboxes.add(front);
        }
        if (rear != null) {
            rear.attachTo(this, "cargo", new Vec3(0.0, 0.1, -zSpan), boxW, boxH);
            this.level().addFreshEntity(rear);
            this.childHitboxes.add(rear);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        for (VehiclePartEntity child : this.childHitboxes) {
            if (!child.isRemoved()) {
                child.discard();
            }
        }
        this.childHitboxes.clear();
        super.remove(reason);
    }

    @Override
    public float maxUpStep() {
        return this.derivedStats.sizeClass().maxUpStep();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(Pose pose) {
        VehicleSizeClass size = this.derivedStats.sizeClass();
        return EntityDimensions.scalable(size.baseWidth(), size.baseHeight());
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void inputTick(float leftImpulse, float forwardImpulse, boolean up, boolean down, boolean left, boolean right, boolean jumping, boolean shiftKeyDown, boolean invertControls) {
        this.inputForward = up || forwardImpulse > 0.05F;
        this.inputBackward = down || forwardImpulse < -0.05F;
        this.inputLeft = left || leftImpulse > 0.05F;
        this.inputRight = right || leftImpulse < -0.05F;
        this.inputBrake = jumping;
        this.inputHoldTicks = 2;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isControlledByLocalInstance()) {
            this.tickDrivePhysics();
        }

        if (!this.level().isClientSide) {
            this.tickPowerGeneration();
            for (VehiclePartEntity child : this.childHitboxes) {
                child.syncPositionToParent();
            }
        } else {
            this.tickClientVisuals();
        }

        if (this.inputHoldTicks > 0) {
            this.inputHoldTicks--;
        } else {
            this.inputForward = false;
            this.inputBackward = false;
            this.inputLeft = false;
            this.inputRight = false;
            this.inputBrake = false;
        }
    }

    private void tickDrivePhysics() {
        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(this.level());
        boolean hasPilot = this.getControllingPassenger() != null;
        boolean canDrive = hasPilot && this.isEngineRunning() && !this.isParkingBrakeOn()
                && this.derivedStats.isDrivable() && this.hasAvailablePower();

        // Steering update
        float targetSteer = 0.0F;
        if (hasPilot) {
            if (this.inputLeft) {
                targetSteer -= 32.0F;
            }
            if (this.inputRight) {
                targetSteer += 32.0F;
            }
        }
        this.steeringAngle = Mth.lerp(0.32F, this.steeringAngle, targetSteer);

        // Acceleration & braking
        double accel = this.derivedStats.computeGroundAcceleration(env.gravityG());
        double maxSpeed = this.derivedStats.maxSpeedBlocksPerTick();
        if (canDrive) {
            if (this.inputForward) {
                this.forwardSpeed = Math.min(maxSpeed, this.forwardSpeed + accel);
                if (!this.level().isClientSide) {
                    this.consumeDrivePower();
                }
            } else if (this.inputBackward) {
                this.forwardSpeed = Math.max(-maxSpeed * 0.55, this.forwardSpeed - accel * 0.85);
                if (!this.level().isClientSide) {
                    this.consumeDrivePower();
                }
            } else {
                this.forwardSpeed *= this.onGround() ? 0.90 : 0.98;
            }
            if (this.inputBrake) {
                this.forwardSpeed *= 0.72;
            }
        } else {
            this.forwardSpeed *= this.onGround() ? 0.85 : 0.98;
        }

        if (Math.abs(this.forwardSpeed) < 0.002) {
            this.forwardSpeed = 0.0;
        }

        // Yaw turning: Ackermann vs 6x6/Tracked Skid-Steer (allows in-place point turn)
        if (Math.abs(this.steeringAngle) > 0.5F && this.onGround()) {
            if (this.derivedStats.skidSteerEnabled()) {
                float turnRate = (this.steeringAngle / 32.0F) * (Math.abs(this.forwardSpeed) < 0.03 ? 3.2F : 2.6F);
                this.setYRot(this.getYRot() + turnRate);
            } else if (Math.abs(this.forwardSpeed) > 0.01) {
                double wheelbase = this.getChassisDefinition().wheelbaseBlocks();
                double turnRad = (this.forwardSpeed * Math.tan(Math.toRadians(this.steeringAngle))) / wheelbase;
                this.setYRot(this.getYRot() + (float) Math.toDegrees(turnRad));
            }
        }

        // Convert forward speed to world velocity + planetary gravity
        double yawRad = Math.toRadians(this.getYRot());
        double vx = -Math.sin(yawRad) * this.forwardSpeed;
        double vz = Math.cos(yawRad) * this.forwardSpeed;
        double gravityStep = -0.08 * Math.clamp(env.gravityG(), 0.16, 2.5);
        double vy = this.onGround() ? Math.max(0.0, this.getDeltaMovement().y) : Math.max(-1.5, this.getDeltaMovement().y + gravityStep);

        this.setDeltaMovement(vx, vy, vz);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision) {
            this.forwardSpeed *= 0.35;
        }

        if (!this.level().isClientSide) {
            this.entityData.set(DATA_STEERING, this.steeringAngle);
            this.entityData.set(DATA_SPEED, (float) this.forwardSpeed);
        }
    }

    private void tickPowerGeneration() {
        if (this.tickCount % 20 != 0) {
            return;
        }
        VehicleConfiguration cfg = this.getConfiguration();
        long cap = this.derivedStats.energyCapacity();
        if (cap <= 0L || cfg.storedEnergy() >= cap) {
            return;
        }
        long gain = 0L;
        if (this.derivedStats.hasRtgTrickle()) {
            gain += 45L;
        }
        if (this.derivedStats.hasSolarRecharge() && this.level().isDay() && this.level().canSeeSky(this.blockPosition().above())) {
            PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(this.level());
            gain += Math.round(60.0 * env.solarIrradianceFactor());
        }
        if (gain > 0L) {
            long nextEnergy = Math.min(cap, cfg.storedEnergy() + gain);
            this.entityData.set(DATA_CONFIG, cfg.withResources(nextEnergy, cfg.storedFuel()));
        }
    }

    private boolean hasAvailablePower() {
        if (this.derivedStats.energyCapacity() <= 0L) {
            return true;
        }
        return this.getConfiguration().storedEnergy() > 0L;
    }

    private void consumeDrivePower() {
        if (this.tickCount % 10 != 0) {
            return;
        }
        VehicleConfiguration cfg = this.getConfiguration();
        long energy = cfg.storedEnergy();
        if (energy > 0L) {
            this.entityData.set(DATA_CONFIG, cfg.withResources(Math.max(0L, energy - 8L), 0L));
        }
    }

    private void tickClientVisuals() {
        float speed = this.getSyncedSpeed();
        this.clientWheelSpin += speed * 1.45F;
        float targetDoor = this.areDoorsOpen() ? 1.0F : 0.0F;
        this.clientDoorProgress = Mth.lerp(0.18F, this.clientDoorProgress, targetDoor);
    }

    /**
     * Returns the exact vehicle-local bounding box for each individual hardpoint
     * (wheel-by-wheel hubs, electric motor, powerplant, seat, cargo, dashboard dial),
     * shared between the server-side 3D raycast and client-side wireframe highlights.
     */
    public @NotNull List<net.minecraft.world.phys.AABB> getHardpointLocalBoxes(@NotNull VehicleHardpoint hp) {
        VehicleSizeClass size = this.getChassisDefinition().sizeClass();
        double x = hp.localX();
        double y = hp.localY();
        double z = hp.localZ();
        return switch (hp.type()) {
            case LOCOMOTION -> {
                double wheelR = size == VehicleSizeClass.LARGE ? 0.66 : (size == VehicleSizeClass.MEDIUM ? 0.54 : 0.42);
                double pad = wheelR + 0.08;
                yield List.of(new net.minecraft.world.phys.AABB(x - 0.34, Math.max(0.0, y - pad), z - pad, x + 0.34, y + pad, z + pad));
            }
            case MOTOR -> List.of(new net.minecraft.world.phys.AABB(x - 0.42, y, z - 0.38, x + 0.42, y + 0.52, z + 0.38));
            case DASHBOARD -> List.of(new net.minecraft.world.phys.AABB(x - 0.24, y - 0.22, z - 0.22, x + 0.24, y + 0.22, z + 0.22));
            case SEAT -> List.of(new net.minecraft.world.phys.AABB(x - 0.36, y, z - 0.34, x + 0.36, y + 0.86, z + 0.34));
            case POWERPLANT -> List.of(new net.minecraft.world.phys.AABB(x - 0.46, y, z - 0.40, x + 0.46, y + 0.62, z + 0.40));
            case CARGO -> {
                double cw = size == VehicleSizeClass.LARGE ? 1.06 : 0.64;
                double ch = size == VehicleSizeClass.LARGE ? 0.74 : 0.50;
                double cd = cw * 0.65;
                yield List.of(new net.minecraft.world.phys.AABB(x - cw, y, z - cd, x + cw, y + ch, z + cd));
            }
            default -> List.of(new net.minecraft.world.phys.AABB(x - 0.38, y - 0.12, z - 0.38, x + 0.38, y + 0.60, z + 0.38));
        };
    }

    /**
     * Checks if the player is aiming directly at an animated cabin door (NASA SEV) or
     * rear airlock ramp (Mars 6x6) so Shift+Right-Click can open/close doors directly in-world.
     */
    public boolean isAimingAtDoor(@NotNull Player player) {
        VehicleSizeClass size = this.getChassisDefinition().sizeClass();
        if (size == VehicleSizeClass.SMALL) {
            return false;
        }
        Vec3 eyeWorld = player.getEyePosition(1.0F);
        Vec3 lookWorld = player.getLookAngle();
        double yawRad = Math.toRadians(this.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        Vec3 rel = eyeWorld.subtract(this.getX(), this.getY(), this.getZ());
        Vec3 localOrigin = new Vec3(rel.x * cos + rel.z * sin, rel.y, -rel.x * sin + rel.z * cos);
        Vec3 localDir = new Vec3(lookWorld.x * cos + lookWorld.z * sin, lookWorld.y, -lookWorld.x * sin + lookWorld.z * cos);
        Vec3 localEnd = localOrigin.add(localDir.scale(6.5));

        if (size == VehicleSizeClass.MEDIUM) {
            var leftDoor = new net.minecraft.world.phys.AABB(-1.42, 0.96, -0.22, -1.06, 2.26, 1.84);
            var rightDoor = new net.minecraft.world.phys.AABB(1.06, 0.96, -0.22, 1.42, 2.26, 1.84);
            return leftDoor.clip(localOrigin, localEnd).isPresent() || rightDoor.clip(localOrigin, localEnd).isPresent();
        }
        var rearRamp = new net.minecraft.world.phys.AABB(-0.82, 1.10, -2.92, 0.82, 2.66, -2.45);
        return rearRamp.clip(localOrigin, localEnd).isPresent();
    }

    /**
     * Immersive-Vehicles-style 3D per-slot raycast: transforms the player's eye ray into
     * vehicle-local coordinates and returns the closest intersecting {@link VehicleHardpoint}.
     */
    public @Nullable VehicleHardpoint findTargetedHardpoint(
            @NotNull Player player,
            boolean requireEmpty,
            @Nullable VehiclePartDefinition filterPart
    ) {
        Vec3 eyeWorld = player.getEyePosition(1.0F);
        Vec3 lookWorld = player.getLookAngle();
        double yawRad = Math.toRadians(this.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);

        // Inverse transform from world space into vehicle-local space
        Vec3 rel = eyeWorld.subtract(this.getX(), this.getY(), this.getZ());
        Vec3 localOrigin = new Vec3(rel.x * cos + rel.z * sin, rel.y, -rel.x * sin + rel.z * cos);
        Vec3 localDir = new Vec3(lookWorld.x * cos + lookWorld.z * sin, lookWorld.y, -lookWorld.x * sin + lookWorld.z * cos);
        Vec3 localEnd = localOrigin.add(localDir.scale(7.5));

        VehicleConfiguration cfg = this.getConfiguration();
        VehicleHardpoint closest = null;
        double closestDistSqr = Double.MAX_VALUE;
        for (VehicleHardpoint hp : this.getChassisDefinition().hardpoints()) {
            boolean occupied = cfg.hasPart(hp.id());
            if (requireEmpty && occupied) {
                continue;
            }
            if (!requireEmpty && !occupied) {
                continue;
            }
            if (filterPart != null && !hp.accepts(filterPart.type(), filterPart.sizeClass())) {
                continue;
            }
            for (net.minecraft.world.phys.AABB box : this.getHardpointLocalBoxes(hp)) {
                var hit = box.clip(localOrigin, localEnd);
                if (hit.isPresent()) {
                    double d2 = hit.get().distanceToSqr(localOrigin);
                    if (d2 < closestDistSqr) {
                        closestDistSqr = d2;
                        closest = hp;
                    }
                }
            }
        }
        return closest;
    }

    @Override
    public @NotNull InteractionResult interact(Player player, InteractionHand hand) {
        return this.interactFromSection(player, hand, "body");
    }

    public @NotNull InteractionResult interactFromSection(@NotNull Player player, @NotNull InteractionHand hand, @NotNull String section) {
        ItemStack held = player.getItemInHand(hand);

        // 1. Physical in-world part installation
        if (held.getItem() instanceof VehiclePartItem partItem) {
            if (!this.level().isClientSide) {
                return this.tryInstallPart(player, held, partItem.getPartId());
            }
            return InteractionResult.SUCCESS;
        }

        // 2. Wrench: Shift+Right-Click packs into VehicleCrateItem; Right-Click detaches last part
        if (held.getItem() instanceof VehicleWrenchItem) {
            if (!this.level().isClientSide) {
                if (player.isShiftKeyDown()) {
                    return this.packIntoCrate(player);
                }
                return this.tryDetachPart(player);
            }
            return InteractionResult.SUCCESS;
        }

        // 3. Clicking rear cargo section opens cargo inventory directly
        if ("cargo".equals(section) && this.cargoInventory.getContainerSize() > 0) {
            if (!this.level().isClientSide) {
                this.openCargoMenu(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 4. Sneak + Right-Click with empty hand: toggles door/ramp directly when aimed at it,
        // otherwise opens the bottom Immersive-Vehicles-style Cockpit Control & Instrument Bar
        if (player.isShiftKeyDown()) {
            if (this.isAimingAtDoor(player)) {
                if (!this.level().isClientSide) {
                    this.handlePanelAction(player, "toggle_doors", "", "");
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (this.level().isClientSide) {
                dev.galacticraft.mod.client.gui.screen.ingame.VehicleClientBridge.openControlPanel(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 4. Mount seat
        if (!this.level().isClientSide) {
            if (this.derivedStats.seats().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.no_seat_installed"), true);
                return InteractionResult.CONSUME;
            }
            if (!this.derivedStats.missingRequiredHardpoints().isEmpty()) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.galacticraft.vehicle.missing_parts",
                                String.join(", ", this.derivedStats.missingRequiredHardpoints())
                        ),
                        true
                );
            }
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    public @NotNull InteractionResult tryInstallPart(@NotNull Player player, @NotNull ItemStack stack, @NotNull ResourceLocation partId) {
        VehiclePartDefinition part = VehicleRegistry.getPart(partId);
        if (part == null) {
            return InteractionResult.PASS;
        }
        VehicleChassisDefinition chassis = this.getChassisDefinition();
        VehicleConfiguration current = this.getConfiguration();

        VehicleHardpoint bestTarget = this.findTargetedHardpoint(player, true, part);
        if (bestTarget == null) {
            for (VehicleHardpoint hp : chassis.hardpoints()) {
                if (!current.hasPart(hp.id()) && hp.accepts(part.type(), part.sizeClass())) {
                    bestTarget = hp;
                    break;
                }
            }
        }
        if (bestTarget == null) {
            player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.no_matching_hardpoint"), true);
            return InteractionResult.CONSUME;
        }

        VehicleConfiguration updated = current.withPart(bestTarget.id(), partId);
        // Top up starter energy/fuel on initial powerplant install so freshly assembled vehicles can be tested right away
        if (part.type() == HardpointType.POWERPLANT) {
            long nextEnergy = Math.min(updated.storedEnergy() + part.energyCapacity() / 2, this.derivedStats.energyCapacity() + part.energyCapacity());
            long nextFuel = Math.min(updated.storedFuel() + part.fuelCapacity() / 2, this.derivedStats.fuelCapacity() + part.fuelCapacity());
            updated = updated.withResources(nextEnergy, nextFuel);
        }
        this.setConfiguration(updated);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.55F, 1.35F);
        player.displayClientMessage(
                Component.translatable("message.galacticraft.vehicle.part_installed", Component.translatable(part.translationKey()), bestTarget.id()),
                true
        );
        return InteractionResult.CONSUME;
    }

    public @NotNull InteractionResult tryDetachPart(@NotNull Player player) {
        VehicleConfiguration current = this.getConfiguration();
        if (current.installedParts().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.empty_chassis"), true);
            return InteractionResult.CONSUME;
        }
        // Remove ONLY the exact installed hardpoint the player is aiming at (Immersive Vehicles wrench behavior)
        VehicleHardpoint aimed = this.findTargetedHardpoint(player, false, null);
        if (aimed == null) {
            player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.aim_at_part"), true);
            return InteractionResult.CONSUME;
        }
        String targetHp = aimed.id();
        ResourceLocation targetPartId = current.getPart(targetHp);
        if (targetHp == null || targetPartId == null) {
            return InteractionResult.PASS;
        }
        this.setConfiguration(current.withoutPart(targetHp));
        ItemStack partStack = GCItems.createVehiclePartStack(targetPartId);
        if (!partStack.isEmpty()) {
            if (!player.getInventory().add(partStack)) {
                this.spawnAtLocation(partStack);
            }
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.NEUTRAL, 0.7F, 1.2F);
        player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.part_removed", targetHp), true);
        return InteractionResult.CONSUME;
    }

    public @NotNull InteractionResult packIntoCrate(@NotNull Player player) {
        if (!this.getPassengers().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.galacticraft.vehicle.cannot_pack_occupied"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack crate = VehicleCrateItem.createCrate(this.getConfiguration());
        // Drop any stored cargo items safely so nothing is lost when crating
        if (!this.cargoInventory.isEmpty()) {
            Containers.dropContents(this.level(), this.blockPosition(), this.cargoInventory);
            this.cargoInventory.clearContent();
        }
        if (!player.getInventory().add(crate)) {
            this.spawnAtLocation(crate);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.NEUTRAL, 0.9F, 0.9F);
        VehicleSizeClass size = this.derivedStats.sizeClass();
        player.displayClientMessage(
                Component.translatable("message.galacticraft.vehicle.packed_crate", size.cargoWidth(), size.cargoHeight()),
                true
        );
        this.discard();
        return InteractionResult.CONSUME;
    }

    private void openCargoMenu(@NotNull Player player) {
        int rows = Math.max(1, Math.min(6, (this.cargoInventory.getContainerSize() + 8) / 9));
        MenuType<ChestMenu> menuType = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ChestMenu(menuType, syncId, inv, this.cargoInventory, rows),
                Component.translatable("container.galacticraft.vehicle_cargo")
        ));
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.derivedStats.seats().size();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        List<Entity> passengers = this.getPassengers();
        List<SeatSpec> seats = this.derivedStats.seats();
        for (int i = 0; i < passengers.size() && i < seats.size(); i++) {
            if (seats.get(i).role() == SeatRole.PILOT && passengers.get(i) instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        int idx = this.getPassengers().indexOf(passenger);
        List<SeatSpec> seats = this.derivedStats.seats();
        Vec3 local = (idx >= 0 && idx < seats.size()) ? seats.get(idx).localOffset() : new Vec3(0.0, 0.65, 0.0);

        // Match exact renderer transform Axis.YP.rotationDegrees(-yRot):
        // local +Z is forward (-sin(yRot), +cos(yRot)), local +X is right (+cos(yRot), +sin(yRot))
        double yawRad = Math.toRadians(this.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double rx = local.x * cos - local.z * sin;
        double rz = local.x * sin + local.z * cos;

        // Place passenger's hips directly on the seat cushion using MC 1.21.1 vehicle attachment point
        Vec3 attach = passenger.getVehicleAttachmentPoint(this);
        double seatSurfaceY = this.getY() + local.y + 0.14;
        moveFunction.accept(passenger, this.getX() + rx - attach.x, seatSurfaceY - attach.y, this.getZ() + rz - attach.z);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        int idx = this.getPassengers().indexOf(passenger);
        List<SeatSpec> seats = this.derivedStats.seats();
        double sideOffset = (idx >= 0 && idx < seats.size()) ? seats.get(idx).exitSideOffset() : 1.5;
        if (Math.abs(sideOffset) < 0.5) {
            sideOffset = 1.5;
        }
        double yawRad = Math.toRadians(this.getYRot());
        double dx = Math.cos(yawRad) * sideOffset;
        double dz = Math.sin(yawRad) * sideOffset;
        BlockPos candidate = BlockPos.containing(this.getX() + dx, this.getY() + 0.5, this.getZ() + dz);
        Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), candidate, true);
        if (safe != null) {
            return safe;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public ItemStack getDropItem() {
        return VehicleCrateItem.createCrate(this.getConfiguration());
    }

    @Override
    protected void destroy(DamageSource source) {
        if (!this.level().isClientSide && !this.cargoInventory.isEmpty()) {
            Containers.dropContents(this.level(), this.blockPosition(), this.cargoInventory);
            this.cargoInventory.clearContent();
        }
        super.destroy(source);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("VehicleConfig", Tag.TAG_COMPOUND)) {
            VehicleConfiguration.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("VehicleConfig"))
                    .result()
                    .ifPresent(this::setConfiguration);
        }
        if (tag.contains("CargoItems", Tag.TAG_LIST)) {
            ListTag list = tag.getList("CargoItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < this.cargoInventory.getContainerSize()) {
                    ItemStack.parse(this.registryAccess(), itemTag).ifPresent(stack -> this.cargoInventory.setItem(slot, stack));
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        VehicleConfiguration.CODEC.encodeStart(NbtOps.INSTANCE, this.getConfiguration())
                .result()
                .ifPresent(encoded -> tag.put("VehicleConfig", encoded));
        ListTag list = new ListTag();
        for (int i = 0; i < this.cargoInventory.getContainerSize(); i++) {
            ItemStack stack = this.cargoInventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                list.add(stack.save(this.registryAccess(), itemTag));
            }
        }
        tag.put("CargoItems", list);
    }
}
