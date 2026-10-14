package io.github.thedarkhorse99.umbralworld.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * What the Chasm's water monsters have in common. They swim like a dolphin (smoothly, in
 * any direction, and only through water: they never path onto land), breathe water, and
 * can't survive out of it: on land they flop about like a fish and dry out, taking damage
 * every half second until they die or get back into the water.
 */
public abstract class ChasmSwimmer extends Monster {
    /** How long it can be out of water before it starts drying out (ticks; 20 = 1 second). */
    private static final int DRY_GRACE = 20;
    /** Damage every half second once it's drying out. */
    private static final float DRY_DAMAGE = 3.0f;

    private int dryTicks;

    protected ChasmSwimmer(EntityType<? extends ChasmSwimmer> type, Level level) {
        super(type, level);
        // The dolphin's swimming controls: turns smoothly, moves in 3D, sinks slowly when idle
        this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.02f, 0.1f, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        setPathfindingMalus(PathType.WATER, 0.0f);
    }

    /** Plans paths through water only, like a dolphin or a guardian. */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    /** Swimming, copied from the dolphin: push in the way it wants to go, then slow down a little like in water. */
    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        moveRelative(getSpeed(), input);
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.9));
        if (getTarget() == null) {
            setDeltaMovement(getDeltaMovement().add(0.0, -0.005, 0.0)); // sinks gently when it has nothing to do
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /** Currents don't push it about: it holds its place in the water. */
    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /** Like other water mobs: may spawn as long as it isn't stuck in a block or another mob. */
    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public void aiStep() {
        // On land: flop about like a fish
        if (!isInWater() && onGround() && verticalCollision) {
            setDeltaMovement(getDeltaMovement().add((random.nextFloat() * 2.0f - 1.0f) * 0.05f, 0.4f,
                    (random.nextFloat() * 2.0f - 1.0f) * 0.05f));
            setOnGround(false);
            needsSync = true;
            makeSound(getFlopSound());
        }
        super.aiStep();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Out of the water it dries out and dies
        if (isInWater()) {
            dryTicks = 0;
        } else if (++dryTicks > DRY_GRACE && dryTicks % 10 == 0) {
            hurtServer(level, damageSources().dryOut(), DRY_DAMAGE);
        }
    }

    protected abstract SoundEvent getFlopSound();

    /**
     * Spawn rule: in water (with water under it too, so not in a puddle), in the dark,
     * not on Peaceful. Used for both Chasm water monsters; the Chasm Skate adds its own check.
     */
    public static boolean checkChasmWaterSpawnRules(EntityType<? extends ChasmSwimmer> type, ServerLevelAccessor level,
                                                    EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.below()).is(FluidTags.WATER)
                && (EntitySpawnReason.isSpawner(reason) || Monster.isDarkEnoughToSpawn(level, pos, random));
    }
}
