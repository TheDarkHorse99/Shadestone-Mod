package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Ghost and Phantom Trees' "leaves": an invisible block you can't touch or aim at that only
 * gives off faint light and drifting particles. It is still a real leaves block
 * underneath, so it fades away when its tree is chopped down (sometimes dropping a
 * sapling as it goes), within a few seconds rather than a leaf's minute or two.
 * Each tree passes in its own particle.
 */
public class GhostVaporBlock extends LeavesBlock {
    /** 1 in this many display ticks makes a particle. Lower = more particles. */
    private static final int PARTICLE_CHANCE = 3;
    /**
     * Vapor further than this from a log (counted in steps through vapor) fades away. Vanilla leaves
     * last up to 6 steps; vapor gives up sooner, so a cloud that only touches the next tree's vapor
     * doesn't keep it alive after its own tree is gone.
     */
    public static final int MAX_DISTANCE = 4;

    private final ParticleOptions particle;

    public GhostVaporBlock(ParticleOptions particle, BlockBehaviour.Properties properties) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
        this.particle = particle;
    }

    /** Fading: too far from any log (vanilla leaves only fade at 7). */
    @Override
    protected boolean decaying(BlockState state) {
        return !state.getValue(PERSISTENT) && state.getValue(DISTANCE) > MAX_DISTANCE;
    }

    /**
     * Vanilla works out a leaf's distance here, then leaves it to a random tick (often a minute or
     * more away) to make it fade. Vapor that has just lost its tree fades within a few seconds instead.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        BlockState now = level.getBlockState(pos);
        if (!now.is(this) || !decaying(now)) {
            return;
        }
        if (decaying(state)) {
            // It was already cut off at the last tick: gone now (sometimes leaving a sapling)
            dropResources(now, level, pos);
            level.removeBlock(pos, false);
        } else {
            level.scheduleTick(pos, this, 10 + random.nextInt(50));
        }
    }

    /** No model at all, so nothing is drawn. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** No outline: players can't aim at it, so they can't break it. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Nothing to bump into. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Runs on the player's screen only, for blocks near them. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(PARTICLE_CHANCE) != 0) {
            return;
        }
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        // Slow, aimless drift
        double dx = (random.nextDouble() - 0.5) * 0.02;
        double dy = (random.nextDouble() - 0.3) * 0.02;
        double dz = (random.nextDouble() - 0.5) * 0.02;
        level.addParticle(particle, x, y, z, dx, dy, dz);
    }
}
