package io.github.thedarkhorse99.umbralworld.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class ShadestoneBlock extends Block {
    /** true = placed by a player (can be portal frame), false = natural terrain. */
    public static final BooleanProperty PLACED = BooleanProperty.create("placed");
    /** How often a waiting frame checks for darkness, in ticks (20 = 1 second). */
    private static final int WATCH_INTERVAL = 20;

    public ShadestoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PLACED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PLACED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PLACED, true);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        ShadestoneSmoke.spawn(level, pos, random, 0.15f);
    }

    // ---- Portal activation ----

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(PLACED);
    }

    /** Safety net: finds frames nobody told us about yet. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        findFrame(level, pos).ifPresent(shape -> {
            if (shape.getBrightestLight(level) <= UmbralPortalShape.MAX_LIGHT) {
                open(level, shape);
            } else {
                watch(level, shape);
            }
        });
    }

    /** Runs once per second on a waiting frame's anchor block. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Optional<UmbralPortalShape> found = UmbralPortalShape.findEmptyShape(level, pos.above());
        if (found.isEmpty()) {
            return; // frame was broken or is already open: stop watching
        }
        UmbralPortalShape shape = found.get();
        if (shape.getBrightestLight(level) <= UmbralPortalShape.MAX_LIGHT) {
            open(level, shape);
        } else {
            level.scheduleTick(pos, this, WATCH_INTERVAL);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Optional<UmbralPortalShape> found = findFrame(serverLevel, pos);
        if (found.isEmpty()) {
            return;
        }
        UmbralPortalShape shape = found.get();
        int light = shape.getBrightestLight(serverLevel);
        if (light <= UmbralPortalShape.MAX_LIGHT) {
            open(serverLevel, shape);
            return;
        }
        // Frame is complete but too bright: fizzle, say why, and start watching.
        BlockPos center = shape.getCenter();
        serverLevel.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.6f);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                20, 0.4, 0.6, 0.4, 0.01);
        if (placer instanceof Player player) {
            player.sendOverlayMessage(Component.translatable("umbral_world.portal.too_bright", light));
        }
        watch(serverLevel, shape);
    }

    /** Starts the once-per-second darkness check on this frame's anchor block (if not already running). */
    public static void watch(ServerLevel level, UmbralPortalShape shape) {
        BlockPos anchor = shape.getAnchor();
        if (!level.getBlockTicks().hasScheduledTick(anchor, ModBlocks.SHADESTONE_BLOCK)) {
            level.scheduleTick(anchor, ModBlocks.SHADESTONE_BLOCK, WATCH_INTERVAL);
        }
    }

    /** Looks in all 6 directions from a frame block for an empty, complete frame. */
    private static Optional<UmbralPortalShape> findFrame(Level level, BlockPos framePos) {
        for (Direction direction : Direction.values()) {
            BlockPos inside = framePos.relative(direction);
            if (level.getBlockState(inside).isAir()) {
                Optional<UmbralPortalShape> shape = UmbralPortalShape.findEmptyShape(level, inside);
                if (shape.isPresent()) {
                    return shape;
                }
            }
        }
        return Optional.empty();
    }

    private static void open(ServerLevel level, UmbralPortalShape shape) {
        shape.createPortalBlocks(level);
        level.playSound(null, shape.getCenter(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6f, 0.5f);
    }
}