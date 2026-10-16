package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.block.entity.CorpseCandleBlockEntity;
import io.github.thedarkhorse99.umbralworld.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A little orb of fire floating just above the ground. Dropped by Ghouls.
 * It has no model: the orb is made of flame particles, drawn by {@link CorpseCandleBlockEntity}.
 * On Effluvium it slowly spreads: now and then it makes a new candle on a
 * nearby Effluvium block, and the Effluvium under the old one dies into Barrow Dirt.
 */
public class CorpseCandleBlock extends Block implements EntityBlock {
    /** As bright as a torch (Mac, 2026-10-16). Its light never makes Umbral mobs fade (see entity/FadeInLight). */
    public static final int LIGHT = 14;

    /** Chance per random tick to spread. A block gets a random tick about once a minute, so this is roughly every 5 minutes. */
    private static final float SPREAD_CHANCE = 0.2F;

    /** Hitbox (only the outline you aim at; you can walk through it): the orb, floating 1 pixel above the ground. */
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

    /** No model at all: only the particles show. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CorpseCandleBlockEntity(pos, state);
    }

    /** The flame particles are made every tick, on the player's screen only (nothing to do on the server). */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() || type != ModBlockEntities.CORPSE_CANDLE) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<CorpseCandleBlockEntity>) CorpseCandleBlockEntity::clientTick;
    }

    /**
     * The light at pos as if Corpse Candles gave none (Mac, 2026-10-16): their light never makes
     * Umbral mobs fade, never turns a Shroud wearer invisible and never shuts an Umbral portal.
     * Light fades by 1 per block, and walls only make it fade faster, so a candle n blocks away
     * (counting each step along x, y and z) gives at most 14 - n here. If the block light here is no
     * brighter than the nearest candle could make it, the candles may be all there is, so it doesn't
     * count. Sunlight always counts.
     */
    public static int lightIgnoringCandles(Level level, BlockPos pos) {
        int sky = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        if (block > sky && block <= brightestCandle(level, pos)) {
            block = 0;
        }
        return Math.max(sky, block);
    }

    /** The most light any Corpse Candle within reach could give at pos (0 if none). */
    private static int brightestCandle(Level level, BlockPos pos) {
        int best = 0;
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof CorpseCandleBlockEntity) {
                        best = Math.max(best, LIGHT - blockEntity.getBlockPos().distManhattan(pos));
                    }
                }
            }
        }
        return best;
    }
}
