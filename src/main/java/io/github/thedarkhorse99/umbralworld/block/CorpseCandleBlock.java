package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A little orb of fire floating just above the ground. Dropped by Ghouls.
 * On Effluvium it slowly spreads: now and then it makes a new candle on a
 * nearby Effluvium block, and the Effluvium under the old one dies into Barrow Dirt.
 */
public class CorpseCandleBlock extends Block {
    /** Chance per random tick to spread. A block gets a random tick about once a minute, so this is roughly every 5 minutes. */
    private static final float SPREAD_CHANCE = 0.2F;

    /** Hitbox: the orb, floating 1 pixel above the ground. */
    private static final VoxelShape SHAPE = Block.box(4.0, 1.0, 4.0, 12.0, 9.0, 12.0);

    public CorpseCandleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Only on the ground: the block below must have a solid top. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    /** Pops off if the ground under it is removed. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** Spreading: only from a candle on Effluvium, only onto Effluvium with nothing on top of it. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= SPREAD_CHANCE || !level.getBlockState(pos.below()).is(ModBlocks.EFFLUVIUM)) {
            return;
        }
        // Pick one of the spots around it (1 block sideways, and up to 1 block up or down, like grass spreading)
        BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
        if (target.getX() == pos.getX() && target.getZ() == pos.getZ()) {
            return;
        }
        if (level.getBlockState(target.below()).is(ModBlocks.EFFLUVIUM) && level.getBlockState(target).isAir()) {
            level.setBlockAndUpdate(target, defaultBlockState());
            // The candle feeds on the soil under it
            level.setBlockAndUpdate(pos.below(), ModBlocks.BARROW_DIRT.defaultBlockState());
        }
    }

    /** Runs on the player's screen only: a tiny flame now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.25,
                    pos.getY() + 0.55,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.25,
                    0.0, 0.01, 0.0);
        }
    }
}
