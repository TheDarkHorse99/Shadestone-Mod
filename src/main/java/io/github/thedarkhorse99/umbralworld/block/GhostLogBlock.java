package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A thin, chorus-style log. It joins up with any Ghost Log next to it in all six
 * directions, and also reaches down into solid ground. Unlike chorus, nothing
 * falls apart when you break a piece.
 */
public class GhostLogBlock extends PipeBlock {
    public GhostLogBlock(BlockBehaviour.Properties properties) {
        // 6.0 = the post is 6 pixels thick (chorus is 8)
        super(6.0F, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /** When a player places one, look around and connect to whatever is there. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    /** When a neighbour changes, connect to it or let go of it. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction),
                connectsTo(level, neighborPos, neighborState, direction));
    }

    /** Works out all six connections for a log at this spot. Also used by the tree generator. */
    public static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction),
                    connectsTo(level, neighborPos, level.getBlockState(neighborPos), direction));
        }
        return state;
    }

    private static boolean connectsTo(BlockGetter level, BlockPos neighborPos, BlockState neighbor, Direction direction) {
        if (neighbor.getBlock() instanceof GhostLogBlock) {
            return true;
        }
        // Reach down into the ground so trees look rooted
        return direction == Direction.DOWN && neighbor.isFaceSturdy(level, neighborPos, Direction.UP);
    }
}
