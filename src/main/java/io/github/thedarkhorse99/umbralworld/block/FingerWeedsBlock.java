package io.github.thedarkhorse99.umbralworld.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Chasm's only plant: knotted roots and twigs growing out of the fissure walls.
 * Climbable like vines (it's in the #minecraft:climbable tag), and you can walk through it.
 * FACING is the way it sticks out (away from the wall), like a ladder. Like a ladder it
 * can also be underwater (WATERLOGGED), so the columns reach down into the water and a
 * swimming player can grab hold.
 *
 * It spreads very slowly onto bare wall next to it, mostly upward, so a single one
 * planted at the bottom of a wall will, given a long time, climb it. Thick patches
 * (like the columns that generate in the Chasm) don't spread any further.
 */
public class FingerWeedsBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Chance per random tick to spread. A block gets a random tick about every 68 seconds, so about once every 30 minutes. */
    private static final float SPREAD_CHANCE = 1.0f / 30.0f;
    /** No spreading if this many Finger Weeds are already within 2 blocks: keeps the generated columns as they are. */
    private static final int CROWDED = 8;

    /** Hitbox: the half of the block nearest the wall, drawn pointing north (wall to the south), turned to match FACING. */
    private static final Map<Direction, VoxelShape> SHAPES =
            Shapes.rotateHorizontal(Block.box(0.0, 0.0, 9.0, 16.0, 16.0, 16.0));

    public FingerWeedsBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** Needs a solid wall behind it. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction toWall = state.getValue(FACING).getOpposite();
        BlockPos wall = pos.relative(toWall);
        return level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING));
    }

    /** Placed on the side of a block it sticks out of that side; otherwise it tries the walls around. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
        BlockState placed = choosePlacement(context);
        return placed == null ? null : placed.setValue(WATERLOGGED, water);
    }

    private BlockState choosePlacement(BlockPlaceContext context) {
        if (context.getClickedFace().getAxis().isHorizontal()) {
            BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    /** Falls off if its wall is removed. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Very slowly creeps onto the bare wall beside it: up half the time, sideways or down otherwise. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= SPREAD_CHANCE || countNearby(level, pos) >= CROWDED) {
            return;
        }
        Direction facing = state.getValue(FACING);
        float roll = random.nextFloat();
        Direction way;
        if (roll < 0.5f) {
            way = Direction.UP;
        } else if (roll < 0.7f) {
            way = facing.getClockWise();
        } else if (roll < 0.9f) {
            way = facing.getCounterClockWise();
        } else {
            way = Direction.DOWN;
        }
        BlockPos target = pos.relative(way);
        BlockState there = level.getBlockState(target);
        boolean water = there.is(Blocks.WATER) && there.getFluidState().isSource();
        BlockState grown = defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, water);
        if ((there.isAir() || water) && grown.canSurvive(level, target)) {
            level.setBlockAndUpdate(target, grown);
        }
    }

    private int countNearby(ServerLevel level, BlockPos pos) {
        int count = 0;
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            if (level.getBlockState(nearby).is(this)) {
                count++;
            }
        }
        return count;
    }
}
