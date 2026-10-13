package io.github.thedarkhorse99.umbralworld.entity;

import io.github.thedarkhorse99.umbralworld.block.GraveThornsBlock;
import io.github.thedarkhorse99.umbralworld.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A tall, skeletal undead of the Grave. You hear it before you see it. */
public class Wight extends Monster {
	public Wight(EntityType<? extends Wight> type, Level level) {
		super(type, level);
	}

	// ---- Stats ----

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)      // zombie: 20
				.add(Attributes.ATTACK_DAMAGE, 8.0)    // zombie: 3
				.add(Attributes.ARMOR, 4.0)            // zombie: 2
				.add(Attributes.MOVEMENT_SPEED, 0.27)  // zombie: 0.23, enderman walking: 0.3
				.add(Attributes.FOLLOW_RANGE, 35.0);   // same as a zombie
	}

	// ---- Behaviour ----

	@Override
	protected void registerGoals() {
		// What it does, lower number = higher priority
		goalSelector.addGoal(0, new FloatGoal(this));                              // swim up instead of drowning
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));            // chase and hit its target
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));     // wander slowly when idle
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));   // stare at nearby players
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));

		// Who it picks as a target
		targetSelector.addGoal(1, new HurtByTargetGoal(this));                     // whoever hurt it
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true)); // the nearest visible player
	}

	// ---- Grave Thorns don't slow or hurt it ----

	@Override
	public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
		if (state.is(ModBlocks.GRAVE_THORNS)) {
			return;
		}
		super.makeStuckInBlock(state, speedMultiplier);
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return source.is(GraveThornsBlock.DAMAGE_TYPE) || super.isInvulnerableTo(level, source);
	}

	// ---- Sounds (vanilla sounds, re-pitched) ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.HUSK_AMBIENT; // a dry groan
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		// A skeleton's steps are quiet (0.15). The Wight's are louder and deeper, so you hear it coming.
		playSound(SoundEvents.SKELETON_STEP, 0.5f, 0.6f);
	}

	@Override
	public float getVoicePitch() {
		// Deeper than normal (1.0), with a little random variation each time
		return 0.6f + (getRandom().nextFloat() - getRandom().nextFloat()) * 0.1f;
	}

	@Override
	protected float getSoundVolume() {
		return 0.6f; // a faint groan rather than a shout
	}
}
