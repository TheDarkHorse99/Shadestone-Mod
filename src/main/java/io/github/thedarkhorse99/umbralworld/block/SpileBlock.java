package io.github.thedarkhorse99.umbralworld.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A little spout tapped into the side of a log. On a Phantom Log it slowly drips
 * Ectoplasm, like pointed dripstone drips water: a cauldron up to 11 blocks below
 * fills up one level at a time. On any other log it's just decoration.
 * FACING is the way the spout points (away from the log).
 */
public class SpileBlock extends HorizontalDirectionalBlock {
    /** Chance per random tick to fill the cauldron by one level. Same as dripstone dripping water. */
    private static final float FILL_CHANCE = 0.17578125F;
    /** How far below the spile a cauldron can be. Same as dripstone. */
    private static final int MAX_CAULDRON_DISTANCE = 11;
    /** Chance per display tick to show a drip. Same as dripstone. */
    private static final float DRIP_PARTICLE_CHANCE = 0.12F;

    /** Hitbox: the spout drawn pointing north (log to the south), turned to match FACING. */
    private static final Map<Direction, VoxelShape> SHAPES =
            Shapes.rotateHorizontal(Block.box(6.5, 6.0, 7.0, 9.5, 9.0, 16.0));

    public SpileBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** It hangs off the side of a log, like a torch on a wall. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).is(BlockTags.LOGS);
    }

    /** Try each side the player is looking towards until one has a log behind it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
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

    /** Pops off if its log is removed. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** Only a spile tapped into a Phantom Log gives Ectoplasm. */
    public static boolean isTapped(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).is(ModBlocks.PHANTOM_LOG);
    }

    /** Every so often, drip one level of Ectoplasm into the cauldron below. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= FILL_CHANCE || !isTapped(state, level, pos)) {
            return;
        }
        BlockPos cauldronPos = findCauldronBelow(level, pos);
        if (cauldronPos == null) {
            return;
        }
        BlockState cauldron = level.getBlockState(cauldronPos);
        BlockState filled;
        if (cauldron.is(Blocks.CAULDRON)) {
            filled = ModBlocks.ECTOPLASM_CAULDRON.defaultBlockState();
        } else {
            filled = cauldron.setValue(LayeredCauldronBlock.LEVEL, cauldron.getValue(LayeredCauldronBlock.LEVEL) + 1);
        }
        level.setBlockAndUpdate(cauldronPos, filled);
        level.gameEvent(GameEvent.BLOCK_CHANGE, cauldronPos, GameEvent.Context.of(filled));
        level.playSound(null, cauldronPos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS,
                2.0F, 0.9F + random.nextFloat() * 0.1F);
    }

    /** Looks straight down through air for an empty cauldron or a not-yet-full Ectoplasm one. */
    private static BlockPos findCauldronBelow(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos check = pos.mutable();
        for (int i = 0; i < MAX_CAULDRON_DISTANCE; i++) {
            check.move(Direction.DOWN);
            BlockState state = level.getBlockState(check);
            if (state.isAir()) {
                continue;
            }
            boolean fillable = state.is(Blocks.CAULDRON)
                    || (state.is(ModBlocks.ECTOPLASM_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL);
            return fillable ? check.immutable() : null;
        }
        return null;
    }

    /** Runs on the player's screen only: a drop forming under the tip of the spout. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= DRIP_PARTICLE_CHANCE || !isTapped(state, level, pos)) {
            return;
        }
        // The tip of the spout is at the middle of the block, just under the spout
        level.addParticle(ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                pos.getX() + 0.5, pos.getY() + 6.0 / 16.0, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
    }
}
