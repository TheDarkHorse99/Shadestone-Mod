package io.github.thedarkhorse99.umbralworld.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A hunched, timid scavenger of the Grave. It never fights: it listens for players
 * and runs when it hears one. Sprinting is heard from far away, walking only up close,
 * and sneaking not at all, so the way to catch one is to creep up on it.
 */
public class Ghoul extends PathfinderMob {
	/** How far away it hears a sprinting player. */
	private static final double HEAR_SPRINTING = 16.0;
	/** How far away it hears a walking player. */
	private static final double HEAR_WALKING = 8.0;
	/** A player moving slower than this (blocks per tick) is standing still and makes no noise. */
	private static final double STILL_SPEED = 0.03;
	/** How long it keeps running after it last heard you (ticks; 20 ticks = 1 second). */
	private static final int SCARED_TICKS = 100;

	/** Speed while wandering, as a fraction of its full speed. A slow shuffle. */
	private static final double WANDER_SPEED = 0.45;

	@Nullable
	private LivingEntity scaredOf;
	private int scaredTicks;

	public Ghoul(EntityType<? extends Ghoul> type, Level level) {
		super(type, level);
		this.xpReward = 3;
	}

	// ---- Stats ----

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)      // a diamond sword does 7, or 10.5 with a critical hit
				// Full speed (only when running away). A mob's real speed goes with the square of this,
				// so 0.35 is about 96% of a sprinting player, and wandering (0.45x) is very slow.
				.add(Attributes.MOVEMENT_SPEED, 0.35)
				.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	// ---- Behaviour ----

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));                                  // swim up instead of drowning
		goalSelector.addGoal(1, new RunAwayGoal());                                    // flee whatever scared it
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, WANDER_SPEED)); // shuffle around slowly
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (scaredTicks > 0) {
			scaredTicks--;
		}
		// Listen 4 times a second
		if (tickCount % 5 == 0) {
			for (Player player : level.players()) {
				if (EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && canHear(player)) {
					scare(player);
				}
			}
		}
	}

	private boolean canHear(Player player) {
		if (player.isShiftKeyDown()) {
			return false; // sneaking is silent
		}
		if (player.getKnownMovement().horizontalDistance() < STILL_SPEED) {
			return false; // standing still
		}
		double range = player.isSprinting() ? HEAR_SPRINTING : HEAR_WALKING;
		return distanceToSqr(player) < range * range;
	}

	private void scare(LivingEntity from) {
		scaredOf = from;
		scaredTicks = SCARED_TICKS;
	}

	/** Is it running away right now? (Used by the in-game tests.) */
	public boolean isScared() {
		return scaredTicks > 0;
	}

	/** Getting hit always scares it, even by a sneaking player. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof LivingEntity attacker) {
			scare(attacker);
		}
		return hurt;
	}

	/** Runs away from whatever scared it, picking a new spot further away whenever it gets there. */
	private class RunAwayGoal extends Goal {
		RunAwayGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return scaredTicks > 0 && scaredOf != null && scaredOf.isAlive();
		}

		@Override
		public void start() {
			runFurther();
		}

		@Override
		public void tick() {
			if (getNavigation().isDone()) {
				runFurther();
			}
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		private void runFurther() {
			if (scaredOf == null) {
				return;
			}
			Vec3 away = DefaultRandomPos.getPosAway(Ghoul.this, 16, 7, scaredOf.position());
			if (away != null) {
				getNavigation().moveTo(away.x, away.y, away.z, 1.0);
			}
		}
	}

	/**
	 * Ghouls stay once they've appeared, like animals: creatures mostly appear when land is first made and
	 * only rarely after, so if they despawned when you walked away there would soon be none left.
	 */
	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	// ---- Sounds (vanilla zombie sounds, quieter and lower) ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ZOMBIE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ZOMBIE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ZOMBIE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ZOMBIE_STEP, 0.1f, 1.0f);
	}

	@Override
	public float getVoicePitch() {
		return 0.7f + (getRandom().nextFloat() - getRandom().nextFloat()) * 0.1f;
	}

	@Override
	protected float getSoundVolume() {
		return 0.4f;
	}
}
