package io.github.thedarkhorse99.umbralworld.entity;

import io.github.thedarkhorse99.umbralworld.block.ModBlocks;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A dark wisp that haunts the Ghost Trees. It is built on vanilla's Vex, so it flies
 * through walls, hunts the nearest player it can see and charges straight at them.
 * Unlike a Vex it has no master, no sword and no time limit, it darts around faster when
 * idle, and it leaves a trail of dark specks.
 */
public class CursedSoul extends Vex {
	/** Spawns only this close to Ghost or Phantom Vapor (blocks, in every direction). */
	private static final int VAPOR_RANGE = 4;
	/** Won't spawn where block light (torches etc.) is brighter than this. The vapor itself gives 3. */
	private static final int MAX_SPAWN_BLOCK_LIGHT = 3;
	/** Trail colour (0xRRGGBB): a very dark purple. */
	private static final int TRAIL_COLOR = 0x2A0F3D;

	public CursedSoul(EntityType<? extends CursedSoul> type, Level level) {
		super(type, level);
		this.xpReward = 5;
	}

	// ---- Stats ----

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 14.0)    // same as a Vex
				.add(Attributes.ATTACK_DAMAGE, 6.0); // Vex: 4, plus its iron sword
	}

	// ---- Behaviour ----

	@Override
	protected void registerGoals() {
		super.registerGoals(); // the Vex's goals: charge at its target, hunt the nearest player, fight back
		goalSelector.addGoal(7, new DartAroundGoal()); // ahead of the Vex's slower drifting (8)
	}

	/** No sword: it attacks with its own touch. */
	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
	}

	/** Like the Vex's idle drifting, but quicker, so it never seems to stay still. Only when it has no target. */
	private class DartAroundGoal extends Goal {
		DartAroundGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			// Only while idle: mid-fight the Vex's own goals take over, so it keeps charging
			return getTarget() == null && !getMoveControl().hasWanted() && getRandom().nextInt(reducedTickDelay(5)) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return false; // one dash per start
		}

		@Override
		public void tick() {
			BlockPos from = blockPosition();
			for (int attempt = 0; attempt < 3; attempt++) {
				BlockPos to = from.offset(getRandom().nextInt(15) - 7, getRandom().nextInt(11) - 5, getRandom().nextInt(15) - 7);
				if (level().isEmptyBlock(to)) {
					getMoveControl().setWantedPosition(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5, 0.6);
					if (getTarget() == null) {
						getLookControl().setLookAt(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5, 180.0f, 20.0f);
					}
					break;
				}
			}
		}
	}

	/** The trail: dark specks left behind it, on the player's screen only. */
	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			level().addParticle(new DustParticleOptions(TRAIL_COLOR, 1.2f),
					xo + (getRandom().nextDouble() - 0.5) * 0.2,
					yo + getBbHeight() / 2 + (getRandom().nextDouble() - 0.5) * 0.2,
					zo + (getRandom().nextDouble() - 0.5) * 0.2,
					0.0, 0.0, 0.0);
		}
	}

	// ---- Spawning ----

	/** Not in Peaceful, not near torches, and only near Ghost or Phantom Vapor. */
	public static boolean checkCursedSoulSpawnRules(EntityType<CursedSoul> type, ServerLevelAccessor level,
	                                                EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || level.getBrightness(LightLayer.BLOCK, pos) > MAX_SPAWN_BLOCK_LIGHT) {
			return false;
		}
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-VAPOR_RANGE, -VAPOR_RANGE, -VAPOR_RANGE),
				pos.offset(VAPOR_RANGE, VAPOR_RANGE, VAPOR_RANGE))) {
			BlockState state = level.getBlockState(near);
			if (state.is(ModBlocks.GHOST_VAPOR) || state.is(ModBlocks.PHANTOM_VAPOR)) {
				return true;
			}
		}
		return false;
	}

	// ---- Sounds: the Vex's own, deeper ----

	@Override
	public float getVoicePitch() {
		return 0.6f + (getRandom().nextFloat() - getRandom().nextFloat()) * 0.1f;
	}
}
