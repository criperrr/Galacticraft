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

import dev.galacticraft.api.entity.attribute.GcApiEntityAttributes;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.GCSounds;
import dev.galacticraft.mod.content.item.GCItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SlimelingEntity extends TamableAnimal {
    private static final EntityDataAccessor<Float> COLOR_RED = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_GREEN = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_BLUE = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SLIMELING_AGE = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FAV_FOOD_INDEX = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KILLS = SynchedEntityData.defineId(SlimelingEntity.class, EntityDataSerializers.INT);

    public static final int MAX_SLIMELING_AGE = 100000;

    private static final List<Item> FAVORITE_FOODS = List.of(
            Items.GOLD_INGOT,
            Items.FLINT_AND_STEEL,
            Items.BAKED_POTATO,
            Items.STONE_SWORD,
            Items.GUNPOWDER,
            Items.WOODEN_HOE,
            Items.EMERALD,
            Items.COD,
            Items.REPEATER,
            GCItems.RAW_DESH
    );

    public SlimelingEntity(EntityType<? extends SlimelingEntity> entityType, Level level) {
        super(entityType, level);
        this.setTame(false, false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR_RED, 1.0F);
        builder.define(COLOR_GREEN, 0.0F);
        builder.define(COLOR_BLUE, 0.0F);
        builder.define(SLIMELING_AGE, 0);
        builder.define(FAV_FOOD_INDEX, 0);
        builder.define(KILLS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(GcApiEntityAttributes.CAN_BREATHE_IN_SPACE, 1.0D);
    }

    public boolean canBreatheInSpace() {
        return this.getAttributeValue(GcApiEntityAttributes.CAN_BREATHE_IN_SPACE) > 0.5D;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.randomizeInitialGenes();
        return data;
    }

    public void randomizeInitialGenes() {
        switch (this.random.nextInt(3)) {
            case 0 -> this.setColor(1.0F, 0.0F, 0.0F);
            case 1 -> this.setColor(0.0F, 0.0F, 1.0F);
            default -> this.setColor(1.0F, 1.0F, 0.0F);
        }
        this.setFavoriteFoodIndex(this.random.nextInt(FAVORITE_FOODS.size()));
    }

    public void setColor(float red, float green, float blue) {
        this.entityData.set(COLOR_RED, Mth.clamp(red, 0.0F, 1.0F));
        this.entityData.set(COLOR_GREEN, Mth.clamp(green, 0.0F, 1.0F));
        this.entityData.set(COLOR_BLUE, Mth.clamp(blue, 0.0F, 1.0F));
    }

    public float getColorRed() {
        return this.entityData.get(COLOR_RED);
    }

    public float getColorGreen() {
        return this.entityData.get(COLOR_GREEN);
    }

    public float getColorBlue() {
        return this.entityData.get(COLOR_BLUE);
    }

    public int getSlimelingAge() {
        return this.entityData.get(SLIMELING_AGE);
    }

    public void setSlimelingAge(int age) {
        this.entityData.set(SLIMELING_AGE, Mth.clamp(age, 0, MAX_SLIMELING_AGE));
    }

    public int getFavoriteFoodIndex() {
        return Mth.clamp(this.entityData.get(FAV_FOOD_INDEX), 0, FAVORITE_FOODS.size() - 1);
    }

    public void setFavoriteFoodIndex(int index) {
        this.entityData.set(FAV_FOOD_INDEX, Mth.clamp(index, 0, FAVORITE_FOODS.size() - 1));
    }

    public Item getFavoriteFood() {
        return FAVORITE_FOODS.get(this.getFavoriteFoodIndex());
    }

    public int getKillCount() {
        return this.entityData.get(KILLS);
    }

    public void setKillCount(int kills) {
        this.entityData.set(KILLS, Math.max(0, kills));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 2 == 0) {
            int currentAge = this.getSlimelingAge();
            if (currentAge < MAX_SLIMELING_AGE) {
                this.setSlimelingAge(currentAge + 1);
                this.updateSlimelingAttributes();
            }
        }
    }

    public double getCalculatedMaxHealth() {
        if (this.isTame()) {
            return 20.0D + 30.0D * ((double) this.getSlimelingAge() / (double) MAX_SLIMELING_AGE);
        }
        return 8.0D;
    }

    private void updateSlimelingAttributes() {
        AttributeInstance maxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(this.getCalculatedMaxHealth());
        }
        AttributeInstance attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double ageBonus = 3.0D * ((double) this.getSlimelingAge() / (double) MAX_SLIMELING_AGE);
            attackAttr.setBaseValue((this.isTame() ? 4.0D : 2.0D) + ageBonus);
        }
    }

    @Override
    public void setTame(boolean tame, boolean applyTamingSideEffects) {
        super.setTame(tame, applyTamingSideEffects);
        this.updateSlimelingAttributes();
    }

    @Override
    protected void applyTamingSideEffects() {
        this.updateSlimelingAttributes();
        this.setHealth((float) this.getCalculatedMaxHealth());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SLIME_BALL) || stack.is(GCItems.RAW_DESH) || stack.is(this.getFavoriteFood());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.level().isClientSide()) {
            boolean canInteract = this.isOwnedBy(player) || this.isTame() || (stack.is(Items.SLIME_BALL) && !this.isTame());
            return canInteract ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        if (this.isTame()) {
            if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
                this.heal(4.0F);
                stack.consume(1, player);
                if (this.random.nextInt(3) == 0) {
                    this.setFavoriteFoodIndex(this.random.nextInt(FAVORITE_FOODS.size()));
                }
                return InteractionResult.SUCCESS;
            }

            InteractionResult superResult = super.mobInteract(player, hand);
            if (!superResult.consumesAction() && this.isOwnedBy(player)) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
                return InteractionResult.SUCCESS;
            }
            return superResult;
        } else if (stack.is(Items.SLIME_BALL)) {
            stack.consume(1, player);
            this.tryToTame(player);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    public void tryToTame(Player player) {
        if (this.random.nextInt(3) == 0) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget(null);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte) 7);
        } else {
            this.level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity) {
        boolean result = super.killedEntity(level, entity);
        this.setKillCount(this.getKillCount() + 1);
        return result;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        SlimelingEntity baby = GCEntityTypes.SLIMELING.create(level);
        if (baby != null && otherParent instanceof SlimelingEntity otherSlimeling) {
            float r = Mth.clamp((this.getColorRed() + otherSlimeling.getColorRed()) * 0.5F, 0.0F, 1.0F);
            float g = Mth.clamp((this.getColorGreen() + otherSlimeling.getColorGreen()) * 0.5F, 0.0F, 1.0F);
            float b = Mth.clamp((this.getColorBlue() + otherSlimeling.getColorBlue()) * 0.5F, 0.0F, 1.0F);
            baby.setColor(r, g, b);
            baby.setFavoriteFoodIndex(this.random.nextInt(FAVORITE_FOODS.size()));

            UUID ownerUuid = this.getOwnerUUID();
            if (ownerUuid != null) {
                baby.setOwnerUUID(ownerUuid);
                baby.setTame(true, true);
            }
        }
        return baby;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return GCSounds.SLIMELING_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("SlimeRed", this.getColorRed());
        compound.putFloat("SlimeGreen", this.getColorGreen());
        compound.putFloat("SlimeBlue", this.getColorBlue());
        compound.putInt("SlimelingAge", this.getSlimelingAge());
        compound.putInt("FavFoodIndex", this.getFavoriteFoodIndex());
        compound.putInt("SlimelingKills", this.getKillCount());
        ResourceLocation favFoodKey = BuiltInRegistries.ITEM.getKey(this.getFavoriteFood());
        compound.putString("FavFoodId", favFoodKey.toString());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SlimeRed")) {
            this.setColor(
                    compound.getFloat("SlimeRed"),
                    compound.getFloat("SlimeGreen"),
                    compound.getFloat("SlimeBlue")
            );
        }
        this.setSlimelingAge(compound.getInt("SlimelingAge"));
        this.setFavoriteFoodIndex(compound.getInt("FavFoodIndex"));
        this.setKillCount(compound.getInt("SlimelingKills"));
        this.updateSlimelingAttributes();
    }
}
