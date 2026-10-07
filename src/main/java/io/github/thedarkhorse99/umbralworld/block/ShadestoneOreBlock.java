package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ShadestoneOreBlock extends DropExperienceBlock {
    public ShadestoneOreBlock(IntProvider xpRange, BlockBehaviour.Properties properties) {
        super(xpRange, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction side = Direction.getRandom(random);
        double x = pos.getX() + 0.5 + side.getStepX() * 0.75 + (random.nextDouble() - 0.5) * 0.5;
        double y = pos.getY() + 0.5 + side.getStepY() * 0.75 + (random.nextDouble() - 0.5) * 0.5;
        double z = pos.getZ() + 0.5 + side.getStepZ() * 0.75 + (random.nextDouble() - 0.5) * 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.01, 0.0);
    }
}