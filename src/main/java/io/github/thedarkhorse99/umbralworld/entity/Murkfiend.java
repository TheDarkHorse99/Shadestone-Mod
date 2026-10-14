package io.github.thedarkhorse99.umbralworld.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A fiendish merfolk with claws and sharp teeth that lives in the water at the bottom of
 * the Chasm. It hunts anyone swimming in its water and chases them down: it's faster
 * than a swimming player, though a player sprint-swimming can pull away. It
 * can't leave the water: it never swims onto land, gives up on anyone who climbs out,
 * and if it ends up on land anyway, it dries out and dies.
 */
public class Murkfiend extends ChasmSwimmer {
    public Murkfiend(EntityType<? extends Murkfiend> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    // ---- Stats ----

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)      // drowned: 20
                .add(Attributes.ATTACK_DAMAGE, 6.0)    // drowned: 3; claws and teeth
                .add(Attributes.ARMOR, 2.0)
                // In water, top speed is about 3.6 x this squared (below 1), in blocks per second:
                // 0.92 gives about 3. A player swims at 2.0 and sprint-swims at 3.8 (measured in
                // the tests), so it catches swimmers, but sprint-swimming gets away
                .add(Attributes.MOVEMENT_SPEED, 0.92)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // ---- Behaviour ----

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.6, 20));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Only hunts players who are in the water
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, level) -> target.isInWater()));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Twice a second: if its target has climbed out of the water, give up on them
        if (tickCount % 10 == 0) {
            LivingEntity target = getTarget();
            if (target != null && !target.isInWater()) {
                setTarget(null);
            }
        }
    }

    // ---- Sounds: the Drowned's underwater sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DROWNED_AMBIENT_WATER;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DROWNED_HURT_WATER;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DROWNED_DEATH_WATER;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DROWNED_SWIM;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.GUARDIAN_FLOP;
    }
}
