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

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.assembly.MountedPartNode;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import dev.galacticraft.api.rocket.assembly.RocketStageNode;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.block.entity.HangarTerminalBlockEntity;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.entity.data.GCEntityDataSerializers;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntConsumer;

/**
 * Lightweight ballistic entity representing a decoupled radial booster, expended lower stage,
 * clamshell fairing half, or jettisoned LES booster stack.
 *
 * <p>Implements Korolev Cross radial separation with anti-boomerang outward nose pitch-out
 * ({@code ~18 deg/s}) and optional parachute/propulsive stage recovery back to the origin launch
 * terminal with minor thermal wear ({@code 0.05..0.10}).
 */
public class DetachedStageEntity extends Entity {
    public static final float KOROLEV_PITCH_OUT_DEG_PER_SEC = 18.0F;
    public static final float KOROLEV_PITCH_OUT_DEG_PER_TICK = KOROLEV_PITCH_OUT_DEG_PER_SEC / 20.0F;
    public static final double RADIAL_SEPARATION_SPEED_MS = 3.0;
    public static final double INLINE_SEPARATION_SPEED_MS = 2.5;
    public static final double FAIRING_SEPARATION_SPEED_MS = 4.0;
    public static final int MAX_LIFETIME_TICKS = 400;

    /**
     * Optional client-only cache cleanup hook invoked from {@link #onClientRemoval()}.
     */
    public static volatile @Nullable IntConsumer CLIENT_REMOVAL_HOOK = null;

    public enum SeparationKind {
        RADIAL_BOOSTER,
        INLINE_STAGE,
        FAIRING_HALF,
        LES_JETTISONED_BOOSTER
    }

    private static final EntityDataAccessor<RocketAssemblyGraph> STAGE_GRAPH = SynchedEntityData.defineId(
            DetachedStageEntity.class, GCEntityDataSerializers.ROCKET_ASSEMBLY_GRAPH);
    private static final EntityDataAccessor<Integer> SEPARATION_KIND = SynchedEntityData.defineId(
            DetachedStageEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> PITCH_DEG = SynchedEntityData.defineId(
            DetachedStageEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW_DEG = SynchedEntityData.defineId(
            DetachedStageEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL_DEG = SynchedEntityData.defineId(
            DetachedStageEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> PARACHUTE_OPEN = SynchedEntityData.defineId(
            DetachedStageEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETRO_FIRING = SynchedEntityData.defineId(
            DetachedStageEntity.class, EntityDataSerializers.BOOLEAN);

    private float angVelPitchDegPerTick = 0.0F;
    private float angVelYawDegPerTick = 0.0F;
    private float angVelRollDegPerTick = 0.0F;
    private float pitchDegO = 0.0F;
    private float yawDegO = 0.0F;
    private float rollDegO = 0.0F;

    private boolean hasParachutes = false;
    private boolean hasLandingGear = false;
    private double residualFuelFraction = 0.0;
    private @Nullable BlockPos originTerminalPos = null;
    private int ageTicks = 0;
    private boolean recovered = false;

    public DetachedStageEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = false;
    }

    /**
     * Spawns a detached stage entity inheriting parent velocity plus separation impulse and Korolev
     * Cross pitch-out angular velocity.
     */
    public static @NotNull DetachedStageEntity create(
            @NotNull ServerLevel level,
            @NotNull Vec3 spawnPos,
            @NotNull Vec3 initialVelocity,
            @NotNull List<MountedPartNode> parts,
            @NotNull SeparationKind kind,
            float initialPitchDeg,
            float initialYawDeg,
            float initialRollDeg,
            float angVelPitchDegPerTick,
            float angVelYawDegPerTick,
            float angVelRollDegPerTick,
            double residualFuelFraction,
            @Nullable BlockPos originTerminalPos
    ) {
        DetachedStageEntity entity = new DetachedStageEntity(GCEntityTypes.DETACHED_STAGE, level);
        entity.setPos(spawnPos);
        entity.setDeltaMovement(initialVelocity);

        boolean parachutes = false;
        boolean landingGear = false;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        double maxRadius = 0.5;
        for (MountedPartNode part : parts) {
            String id = (part.config().familyId() + "_" + part.config().modelId()).toLowerCase(Locale.ROOT);
            if (id.contains("parachute") || id.contains("chute")) {
                parachutes = true;
            }
            if (id.contains("landing_gear") || id.contains("landing_leg")) {
                landingGear = true;
            }
            int y0 = part.relativePos().getY();
            int y1 = y0 + Math.max(1, part.config().heightBlocks());
            minY = Math.min(minY, y0);
            maxY = Math.max(maxY, y1);
            maxRadius = Math.max(maxRadius, Math.max(part.config().diameter(), part.config().topDiameter()) * 0.5);
        }
        int height = minY <= maxY ? Math.max(1, maxY - minY) : 2;
        RocketStageNode stageNode = new RocketStageNode(
                0, Optional.empty(), false, "rp1", 0L, 0L, parts, List.of(), false
        );
        RocketAssemblyGraph singleGraph = new RocketAssemblyGraph(
                List.of(stageNode), List.of(), height, maxRadius, landingGear, false, false
        );

        entity.entityData.set(STAGE_GRAPH, singleGraph);
        entity.entityData.set(SEPARATION_KIND, kind.ordinal());
        entity.entityData.set(PITCH_DEG, initialPitchDeg);
        entity.entityData.set(YAW_DEG, initialYawDeg);
        entity.entityData.set(ROLL_DEG, initialRollDeg);
        entity.pitchDegO = initialPitchDeg;
        entity.yawDegO = initialYawDeg;
        entity.rollDegO = initialRollDeg;
        entity.angVelPitchDegPerTick = angVelPitchDegPerTick;
        entity.angVelYawDegPerTick = angVelYawDegPerTick;
        entity.angVelRollDegPerTick = angVelRollDegPerTick;
        entity.hasParachutes = parachutes;
        entity.hasLandingGear = landingGear;
        entity.residualFuelFraction = Math.clamp(residualFuelFraction, 0.0, 1.0);
        entity.originTerminalPos = originTerminalPos != null ? originTerminalPos.immutable() : null;
        entity.refreshDimensions();
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STAGE_GRAPH, RocketAssemblyGraph.EMPTY);
        builder.define(SEPARATION_KIND, SeparationKind.RADIAL_BOOSTER.ordinal());
        builder.define(PITCH_DEG, 0.0F);
        builder.define(YAW_DEG, 0.0F);
        builder.define(ROLL_DEG, 0.0F);
        builder.define(PARACHUTE_OPEN, false);
        builder.define(RETRO_FIRING, false);
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        RocketAssemblyGraph g = this.getStageGraph();
        float w = Math.max(1.0F, (float) (g.maxRadiusBlocks() * 2.0));
        float h = Math.max(1.0F, g.totalHeightBlocks());
        return EntityDimensions.scalable(w, h);
    }

    public @NotNull RocketAssemblyGraph getStageGraph() {
        return this.entityData.get(STAGE_GRAPH);
    }

    public @NotNull SeparationKind getSeparationKind() {
        int ord = this.entityData.get(SEPARATION_KIND);
        SeparationKind[] values = SeparationKind.values();
        return (ord >= 0 && ord < values.length) ? values[ord] : SeparationKind.RADIAL_BOOSTER;
    }

    public float getPitchDeg() {
        return this.entityData.get(PITCH_DEG);
    }

    public float getYawDeg() {
        return this.entityData.get(YAW_DEG);
    }

    public float getRollDeg() {
        return this.entityData.get(ROLL_DEG);
    }

    public float getInterpolatedPitchDeg(float partialTick) {
        return Mth.lerp(partialTick, this.pitchDegO, this.getPitchDeg());
    }

    public float getInterpolatedYawDeg(float partialTick) {
        return Mth.lerp(partialTick, this.yawDegO, this.getYawDeg());
    }

    public float getInterpolatedRollDeg(float partialTick) {
        return Mth.lerp(partialTick, this.rollDegO, this.getRollDeg());
    }

    public float getAngularVelocityPitchDegPerTick() {
        return this.angVelPitchDegPerTick;
    }

    public boolean isParachuteOpen() {
        return this.entityData.get(PARACHUTE_OPEN);
    }

    public boolean isRetroFiring() {
        return this.entityData.get(RETRO_FIRING);
    }

    public boolean isRecoverable() {
        return this.hasParachutes || (this.hasLandingGear && this.residualFuelFraction >= 0.10);
    }

    @Override
    public void tick() {
        this.pitchDegO = this.getPitchDeg();
        this.yawDegO = this.getYawDeg();
        this.rollDegO = this.getRollDeg();
        super.tick();
        this.ageTicks++;

        PlanetaryEnvironment env = PlanetaryEnvironment.fromLevel(this.level());
        double gravityPerTick = env.gravityG() * 0.04;
        Vec3 vel = this.getDeltaMovement();

        boolean descending = vel.y < -0.05;
        boolean chuteActive = false;
        boolean retroActive = false;

        if (descending && this.ageTicks >= 20) {
            if (this.hasParachutes && env.pressureAtm() >= 0.01) {
                chuteActive = true;
                vel = new Vec3(vel.x * 0.90, Math.max(vel.y * 0.85, -0.22), vel.z * 0.90);
                // Parachute stabilizes tumbling back upright
                this.angVelPitchDegPerTick *= 0.88F;
                this.angVelRollDegPerTick *= 0.88F;
            } else if (this.hasLandingGear && this.residualFuelFraction >= 0.10 && vel.y < -0.20) {
                retroActive = true;
                vel = new Vec3(vel.x * 0.92, Math.min(-0.18, vel.y + gravityPerTick * 1.45 + 0.025), vel.z * 0.92);
                this.residualFuelFraction = Math.max(0.0, this.residualFuelFraction - 0.0005);
            }
        }

        if (!chuteActive && !retroActive) {
            double drag = env.pressureAtm() > 0.01 ? 0.992 : 0.9995;
            vel = new Vec3(vel.x * drag, vel.y - gravityPerTick, vel.z * drag);
        }

        this.setDeltaMovement(vel);
        this.move(MoverType.SELF, vel);

        if (!this.level().isClientSide) {
            this.entityData.set(PARACHUTE_OPEN, chuteActive);
            this.entityData.set(RETRO_FIRING, retroActive);
            this.entityData.set(PITCH_DEG, this.getPitchDeg() + this.angVelPitchDegPerTick);
            this.entityData.set(YAW_DEG, this.getYawDeg() + this.angVelYawDegPerTick);
            this.entityData.set(ROLL_DEG, this.getRollDeg() + this.angVelRollDegPerTick);

            if (this.ageTicks <= 25 && (this.ageTicks % 3) == 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 2, 0.25, 0.25, 0.25, 0.02);
            }
            if (retroActive && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() - 0.5, this.getZ(), 4, 0.2, 0.1, 0.2, 0.04);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() - 1.0, this.getZ(), 2, 0.3, 0.2, 0.3, 0.02);
            }

            if (this.onGround() || this.ageTicks >= MAX_LIFETIME_TICKS) {
                if (this.isRecoverable() && !this.recovered && this.level() instanceof ServerLevel serverLevel) {
                    this.completeStageRecovery(serverLevel);
                } else if (this.onGround() && this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 3, 0.5, 0.5, 0.5, 0.05);
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8F, 0.9F);
                }
                this.discard();
            }
        }
    }

    private void completeStageRecovery(ServerLevel serverLevel) {
        this.recovered = true;
        List<MountedPartNode> parts = this.getStageGraph().allActiveParts();
        if (parts.isEmpty()) {
            return;
        }

        HangarTerminalBlockEntity terminal = null;
        if (this.originTerminalPos != null && serverLevel.isLoaded(this.originTerminalPos)
                && serverLevel.getBlockEntity(this.originTerminalPos) instanceof HangarTerminalBlockEntity be
                && !be.isRemoved()) {
            terminal = be;
        }

        int recoveredCount = 0;
        for (MountedPartNode node : parts) {
            // Apply minor 0.05..0.10 thermal recovery wear
            double wearFraction = 0.05 + serverLevel.random.nextDouble() * 0.05;
            double wearPoints = node.integrity().maxIntegrity() * wearFraction;
            PartIntegrity worn = node.integrity().damage(wearPoints);

            ItemStack stack = new ItemStack(ModularRocketPartBlock.getBlockForCategory(node.config().category()));
            stack.set(GCDataComponents.ROCKET_PART_CONFIG, node.config());
            stack.set(GCDataComponents.ROCKET_PART_STATS, node.stats());
            stack.set(GCDataComponents.PART_INTEGRITY, worn);
            node.composition().ifPresent(comp -> {
                if (!comp.isEmpty()) {
                    stack.set(GCDataComponents.ROCKET_PART_COMPOSITION, comp);
                }
            });

            boolean deposited = false;
            if (terminal != null) {
                var slots = terminal.getRocketSlots();
                for (int i = 0; i < slots.getContainerSize(); i++) {
                    if (slots.getItem(i).isEmpty()) {
                        slots.setItem(i, stack);
                        deposited = true;
                        break;
                    }
                }
            }
            if (!deposited) {
                BlockPos dropAt = terminal != null ? terminal.getBlockPos().above() : this.blockPosition().above();
                Block.popResource(serverLevel, dropAt, stack);
            }
            recoveredCount++;
        }

        if (recoveredCount > 0) {
            BlockPos notifyPos = terminal != null ? terminal.getBlockPos() : this.blockPosition();
            for (ServerPlayer player : serverLevel.players()) {
                if (player.blockPosition().distSqr(notifyPos) <= 256.0 * 256.0) {
                    player.displayClientMessage(
                            Component.literal("[RECUPERAÇÃO] Estágio recuperado (" + recoveredCount + " módulos) com desgaste térmico leve.")
                                    .withStyle(ChatFormatting.GREEN),
                            false
                    );
                }
            }
        }
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        IntConsumer hook = CLIENT_REMOVAL_HOOK;
        if (hook != null) {
            hook.accept(this.getId());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("StageGraph")) {
            RocketAssemblyGraph.CODEC.parse(NbtOps.INSTANCE, tag.get("StageGraph"))
                    .result()
                    .ifPresent(g -> this.entityData.set(STAGE_GRAPH, g));
        }
        this.entityData.set(SEPARATION_KIND, tag.getInt("SeparationKind"));
        this.entityData.set(PITCH_DEG, tag.getFloat("PitchDeg"));
        this.entityData.set(YAW_DEG, tag.getFloat("YawDeg"));
        this.entityData.set(ROLL_DEG, tag.getFloat("RollDeg"));
        this.angVelPitchDegPerTick = tag.getFloat("AngVelPitch");
        this.angVelYawDegPerTick = tag.getFloat("AngVelYaw");
        this.angVelRollDegPerTick = tag.getFloat("AngVelRoll");
        this.hasParachutes = tag.getBoolean("HasParachutes");
        this.hasLandingGear = tag.getBoolean("HasLandingGear");
        this.residualFuelFraction = tag.getDouble("ResidualFuel");
        this.ageTicks = tag.getInt("AgeTicks");
        this.recovered = tag.getBoolean("Recovered");
        if (tag.contains("OriginTerminalPos", Tag.TAG_LONG)) {
            this.originTerminalPos = BlockPos.of(tag.getLong("OriginTerminalPos"));
        }
        this.refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        RocketAssemblyGraph.CODEC.encodeStart(NbtOps.INSTANCE, this.getStageGraph())
                .result()
                .ifPresent(encoded -> tag.put("StageGraph", encoded));
        tag.putInt("SeparationKind", this.entityData.get(SEPARATION_KIND));
        tag.putFloat("PitchDeg", this.getPitchDeg());
        tag.putFloat("YawDeg", this.getYawDeg());
        tag.putFloat("RollDeg", this.getRollDeg());
        tag.putFloat("AngVelPitch", this.angVelPitchDegPerTick);
        tag.putFloat("AngVelYaw", this.angVelYawDegPerTick);
        tag.putFloat("AngVelRoll", this.angVelRollDegPerTick);
        tag.putBoolean("HasParachutes", this.hasParachutes);
        tag.putBoolean("HasLandingGear", this.hasLandingGear);
        tag.putDouble("ResidualFuel", this.residualFuelFraction);
        tag.putInt("AgeTicks", this.ageTicks);
        tag.putBoolean("Recovered", this.recovered);
        if (this.originTerminalPos != null) {
            tag.putLong("OriginTerminalPos", this.originTerminalPos.asLong());
        }
    }
}
