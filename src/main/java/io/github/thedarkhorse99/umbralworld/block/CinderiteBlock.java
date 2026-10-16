package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.GeyserBaseParticleOptions;
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
 * steam rises off it, as if it were boiling the water (Mac asked for steam, not bubbles).
 */
public class CinderiteBlock extends Block {
    /** Chance per display tick of a puff of steam from each wet side. */
    private static final float STEAM_CHANCE = 0.3f;

    public CinderiteBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (Direction side : Direction.values()) {
            if (side == Direction.DOWN || random.nextFloat() >= STEAM_CHANCE) {
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
            // Puffs of steam (vanilla's geyser steam), rising slowly off the hot rock
            level.addParticle(new GeyserBaseParticleOptions(ParticleTypes.GEYSER_POOF, 1, 0.02f), x, y, z,
                    side.getStepX() * 0.01, 0.03, side.getStepZ() * 0.01);
        }
    }
}
