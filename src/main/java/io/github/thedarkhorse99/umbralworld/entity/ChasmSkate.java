package io.github.thedarkhorse99.umbralworld.entity;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A skate whose back is camouflaged to look like the chasm floor. It lies still on the
 * bottom until a player swims close (over it or right beside it), then shoots up in a
 * quick burst of speed to attack. After the burst it's slower than a swimming player,
 * so you can get away; once you're far enough off (or out of the water) it loses
 * interest and settles back onto the floor.
 */
public class ChasmSkate extends ChasmSwimmer {
    /** How close a player must swim to set it off: sideways, and how far above it. */
    private static final double AMBUSH_SIDEWAYS = 4.0;
    private static final double AMBUSH_ABOVE = 5.0;
    /** The burst: how long it lasts and how long before it can burst again (ticks; 20 = 1 second). */
    private static final int BURST_TICKS = 30;
    private static final int BURST_COOLDOWN = 100;
    /** It gives up on a target this far away. */
    private static final double GIVE_UP_DISTANCE = 12.0;

    private static final AttributeModifier BURST = new AttributeModifier(
            UmbralWorld.id("chasm_skate_burst"), 4.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE); // 5x speed

    private int burstTicks;
    private int cooldown;

    public ChasmSkate(EntityType<? extends ChasmSkate> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    // ---- Stats ----

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)    // one hard sting from the ambush
                .add(Attributes.ARMOR, 4.0)            // tough hide
                // In water, top speed is about 3.6 x this squared (below 1), or 3.6 x this (above 1),
                // in blocks per second: 0.4 gives about 0.6-1, slower than a swimming player (2.0);
                // the burst (5x, so 2.0) could reach 7, but it only lasts 1.5 seconds
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // ---- Behaviour ----

    @Override
    protected void registerGoals() {
        // No wandering and no looking around: lying still is the whole trick
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // It picks its own targets in watchForPrey(), only when someone swims close
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (cooldown > 0) {
            cooldown--;
        }
        if (burstTicks > 0 && --burstTicks == 0) {
            setBurst(false);
        }
        if (tickCount % 5 == 0) {
            LivingEntity target = getTarget();
            if (target == null) {
                watchForPrey(level);
            } else if (!target.isAlive() || !target.isInWater() || distanceTo(target) > GIVE_UP_DISTANCE) {
                setTarget(null); // lost interest: it will sink back to the floor
            } else if (cooldown == 0 && distanceTo(target) <= AMBUSH_SIDEWAYS) {
                startBurst(); // its prey came close again: another lunge
            }
        }
    }

    /** Looks for a player swimming close above it, that it can see. */
    private void watchForPrey(ServerLevel level) {
        if (cooldown > 0) {
            return;
        }
        for (Player player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !player.isInWater()) {
                continue;
            }
            double dx = player.getX() - getX();
            double dz = player.getZ() - getZ();
            double above = player.getY() - getY();
            if (dx * dx + dz * dz <= AMBUSH_SIDEWAYS * AMBUSH_SIDEWAYS && above >= -1.0 && above <= AMBUSH_ABOVE
                    && hasLineOfSight(player)) {
                setTarget(player);
                startBurst();
                return;
            }
        }
    }

    private void startBurst() {
        setBurst(true);
        burstTicks = BURST_TICKS;
        cooldown = BURST_COOLDOWN;
    }

    private void setBurst(boolean on) {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (on && !speed.hasModifier(BURST.id())) {
            speed.addTransientModifier(BURST);
        } else if (!on) {
            speed.removeModifier(BURST.id());
        }
    }

    /** True during the burst (used by the tests). */
    public boolean isBursting() {
        return burstTicks > 0;
    }

    /** Spawns on the floor of the Chasm's water: in water, with solid ground right under it, in the dark. */
    public static boolean checkChasmSkateSpawnRules(EntityType<? extends ChasmSkate> type, ServerLevelAccessor level,
                                                    EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && (EntitySpawnReason.isSpawner(reason) || Monster.isDarkEnoughToSpawn(level, pos, random));
    }

    // ---- Sounds: a fish's, and the pufferfish's sting when it attacks ----

    @Override
    public void playAttackSound() {
        playSound(SoundEvents.PUFFER_FISH_STING, 1.0f, 0.8f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }
}
