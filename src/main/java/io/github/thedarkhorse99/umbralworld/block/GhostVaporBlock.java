package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
 * The Ghost Tree's "leaves": an invisible block you can't touch or aim at that only
 * gives off faint light and drifting particles. It is still a real leaves block
 * underneath, so it fades away when its tree is chopped down (sometimes dropping a
 * Ghost Sapling as it goes).
 */
public class GhostVaporBlock extends LeavesBlock {
    /** 1 in this many display ticks makes a particle. Lower = more particles. */
    private static final int PARTICLE_CHANCE = 6;

    public GhostVaporBlock(BlockBehaviour.Properties properties) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
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
        level.addParticle(ParticleTypes.END_ROD, x, y, z, dx, dy, dz);
    }
}
