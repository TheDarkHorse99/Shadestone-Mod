package io.github.thedarkhorse99.umbralworld.entity;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.block.AuraPlant;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The top half of a zombie, dragging itself along on its arms. It is hooked on the
 * Miasma's aura plants: it crawls over to them and lies next to them, and attacks
 * anyone who comes too close to its plant. Being next to a plant makes it faster,
 * stronger and sharper-eyed. Hit it and it crawls away for a moment.
 */
public class Crawler extends Monster {
	/** How far from a plant still counts as "next to" it (same as the plants' aura on players). */
	private static final int NEAR_SIDEWAYS = 2;
	private static final int NEAR_UP_DOWN = 1;
	/** How far away it looks for a plant to crawl to. */
	private static final int SEEK_SIDEWAYS = 10;
	private static final int SEEK_UP_DOWN = 2;
	/** A player this close to its plant gets attacked. */
	private static final double GUARD_RANGE = 4.0;
	/** How long it crawls away after being hit (ticks; 20 = 1 second). */
	private static final int FLEE_TICKS = 60;

	// The plant buff, added while it's next to a plant and removed when it leaves
	private static final Identifier BUFF_ID = UmbralWorld.id("crawler_plant_buff");
	private static final AttributeModifier SPEED_BUFF =
			new AttributeModifier(BUFF_ID, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);  // +50% speed
	private static final AttributeModifier STRENGTH_BUFF =
			new AttributeModifier(BUFF_ID, 3.0, AttributeModifier.Operation.ADD_VALUE);            // +3 damage, like Strength I
	private static final AttributeModifier VISION_BUFF =
			new AttributeModifier(BUFF_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);  // sees twice as far

	/** The plant it's lying next to right now, or null. */
	@Nullable
	private BlockPos plant;
	@Nullable
	private LivingEntity fleeFrom;
	private int fleeTicks;

	public Crawler(EntityType<? extends Crawler> type, Level level) {
		super(type, level);
	}

	// ---- Stats ----

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 16.0)      // zombie: 20 (it's only half of one)
				.add(Attributes.ATTACK_DAMAGE, 5.0)    // zombie: 3; 8 next to a plant
				.add(Attributes.ARMOR, 2.0)            // same as a zombie
				// A mob's real speed goes with the square of this: 0.2 is under half a walking player,
				// 0.3 (next to a plant) is about a walking player
				.add(Attributes.MOVEMENT_SPEED, 0.2)
				.add(Attributes.FOLLOW_RANGE, 16.0);   // 32 next to a plant
	}

	// ---- Behaviour ----

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));                            // swim up instead of drowning
		goalSelector.addGoal(1, new CrawlAwayGoal());                            // after being hit
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));          // chase off whoever came near its plant
		goalSelector.addGoal(4, new SeekPlantGoal());                            // crawl back to the plants
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));  // shift around now and then
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		// No target goals: it only picks targets itself, in guardPlant()
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (fleeTicks > 0) {
			fleeTicks--;
		}
		// Twice a second: am I next to a plant? Is anyone too close to it?
		if (tickCount % 10 == 0) {
			plant = findPlant(level, NEAR_SIDEWAYS, NEAR_UP_DOWN);
			setBuffed(plant != null);
			guardPlant(level);
			dropLostTarget();
		}
	}

	/** The closest aura plant within the given distance of it, or null. */
	@Nullable
	private BlockPos findPlant(ServerLevel level, int sideways, int upDown) {
		BlockPos here = blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(here.offset(-sideways, -upDown, -sideways), here.offset(sideways, upDown, sideways))) {
			if (level.getBlockState(pos).getBlock() instanceof AuraPlant) {
				double distance = pos.distSqr(here);
				if (distance < bestDistance) {
					best = pos.immutable();
					bestDistance = distance;
				}
			}
		}
		return best;
	}

	private void setBuffed(boolean buffed) {
		setModifier(Attributes.MOVEMENT_SPEED, SPEED_BUFF, buffed);
		setModifier(Attributes.ATTACK_DAMAGE, STRENGTH_BUFF, buffed);
		setModifier(Attributes.FOLLOW_RANGE, VISION_BUFF, buffed);
	}

	private void setModifier(Holder<Attribute> attribute, AttributeModifier modifier, boolean on) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (on && !instance.hasModifier(modifier.id())) {
			instance.addTransientModifier(modifier);
		} else if (!on) {
			instance.removeModifier(modifier.id());
		}
	}

	/** Attacks the nearest player standing too close to its plant (if it can see them). */
	private void guardPlant(ServerLevel level) {
		if (plant == null || fleeTicks > 0 || getTarget() != null) {
			return;
		}
		Vec3 plantCenter = Vec3.atCenterOf(plant);
		Player intruder = null;
		double closest = GUARD_RANGE * GUARD_RANGE;
		for (Player player : level.players()) {
			double distance = player.distanceToSqr(plantCenter);
			if (distance < closest && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && hasLineOfSight(player)) {
				intruder = player;
				closest = distance;
			}
		}
		if (intruder != null) {
			setTarget(intruder);
		}
	}

	/** Gives up the chase once its target is out of sight range (shorter once it leaves the plants). */
	private void dropLostTarget() {
		LivingEntity target = getTarget();
		if (target == null) {
			return;
		}
		double range = getAttributeValue(Attributes.FOLLOW_RANGE);
		if (!target.isAlive() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target) || distanceToSqr(target) > range * range) {
			setTarget(null);
		}
	}

	/** Is it crawling away after being hit? (Used by the in-game tests.) */
	public boolean isFleeing() {
		return fleeTicks > 0;
	}

	/** Getting hit makes it crawl away for a moment, whoever it was fighting. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof LivingEntity attacker) {
			fleeFrom = attacker;
			fleeTicks = FLEE_TICKS;
			setTarget(null);
		}
		return hurt;
	}

	/** Drags itself away from whoever hit it. */
	private class CrawlAwayGoal extends Goal {
		CrawlAwayGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return fleeTicks > 0 && fleeFrom != null;
		}

		@Override
		public void start() {
			crawlFurther();
		}

		@Override
		public void tick() {
			if (getNavigation().isDone()) {
				crawlFurther();
			}
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		private void crawlFurther() {
			if (fleeFrom == null) {
				return;
			}
			Vec3 away = DefaultRandomPos.getPosAway(Crawler.this, 8, 4, fleeFrom.position());
			if (away != null) {
				getNavigation().moveTo(away.x, away.y, away.z, 1.3);
			}
		}
	}

	/** When it isn't next to a plant, it looks around for one (every 2 seconds) and crawls over to it. */
	private class SeekPlantGoal extends Goal {
		@Nullable
		private BlockPos goal;
		private int cooldown;

		SeekPlantGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (plant != null || --cooldown > 0 || !(level() instanceof ServerLevel level)) {
				return false;
			}
			cooldown = 40;
			goal = findPlant(level, SEEK_SIDEWAYS, SEEK_UP_DOWN);
			return goal != null;
		}

		@Override
		public boolean canContinueToUse() {
			return plant == null && goal != null && !getNavigation().isDone();
		}

		@Override
		public void start() {
			if (goal != null) {
				getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.8);
			}
		}

		@Override
		public void stop() {
			goal = null;
			getNavigation().stop();
		}
	}

	// ---- Sounds (vanilla zombie sounds, a little lower) ----

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
		// A soft drag rather than footsteps
		playSound(SoundEvents.ZOMBIE_STEP, 0.1f, 0.6f);
	}

	@Override
	public float getVoicePitch() {
		return 0.85f + (getRandom().nextFloat() - getRandom().nextFloat()) * 0.1f;
	}
}
