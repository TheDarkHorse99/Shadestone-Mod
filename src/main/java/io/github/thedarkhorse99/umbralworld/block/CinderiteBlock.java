package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A smouldering Cinderite block (the ore for now; later the other Cinderite blocks too).
 * It glows faintly (its light level is set in ModBlocks), and wherever it touches water
 * the water fizzes: streams of bubbles rise off it, as if it were boiling the water.
 */
public class CinderiteBlock extends Block {
    /** Chance per display tick of bubbles from each wet side. */
    private static final float BUBBLE_CHANCE = 0.5f;

    public CinderiteBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (Direction side : Direction.values()) {
            if (side == Direction.DOWN || random.nextFloat() >= BUBBLE_CHANCE) {
                continue;
            }
            BlockPos next = pos.relative(side);
            if (!level.getFluidState(next).is(FluidTags.WATER)) {
                continue;
            }
            // Just outside this side of the block, somewhere across its face
            double x = pos.getX() + 0.5 + side.getStepX() * 0.55 + (side.getStepX() == 0 ? (random.nextDouble() - 0.5) * 0.9 : 0.0);
            double y = pos.getY() + 0.5 + side.getStepY() * 0.55 + (side.getStepY() == 0 ? (random.nextDouble() - 0.5) * 0.9 : 0.0);
            double z = pos.getZ() + 0.5 + side.getStepZ() * 0.55 + (side.getStepZ() == 0 ? (random.nextDouble() - 0.5) * 0.9 : 0.0);
            // A rising bubble stream, plus the odd loose bubble drifting off
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, x, y, z, 0.0, 0.04, 0.0);
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.BUBBLE, x, y, z,
                        side.getStepX() * 0.05, 0.05, side.getStepZ() * 0.05);
            }
        }
    }
}
