package io.github.thedarkhorse99.umbralworld.block.entity;

import net.minecraft.core.BlockPos;
import io.github.thedarkhorse99.umbralworld.particle.ModParticles;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Corpse Candle: a see-through pale blue candle floating just above the ground, slowly bobbing
 * up and down (drawn by the client's CorpseCandleRenderer), with a little ball of pale blue flame
 * sparkles at its wick that bobs along with it. A block entity is needed for both: the renderer
 * draws it, and it can make sparkles every tick on the player's screen (a plain block only gets the
 * occasional random display tick, which would leave gaps in the flame).
 * It stores nothing.
 */
public class CorpseCandleBlockEntity extends BlockEntity {
    /** Height of the candle's base above the bottom of the block, in the middle of its bob (2 pixels). */
    private static final double FLOAT_HEIGHT = 2.0 / 16.0;
    /** How far it bobs up and down from there (1 pixel). */
    private static final double BOB = 1.0 / 16.0;
    /** One slow bob up and down every 4 seconds. */
    private static final double BOB_TICKS = 80.0;
    /** The flame's centre above the candle's base: just over the 7-pixel candle and wick. */
    private static final double FLAME_Y = 8.0 / 16.0;
    /** Radius of the flame, in blocks (about 1.5 pixels). */
    private static final double RADIUS = 0.09;

    public CorpseCandleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORPSE_CANDLE, pos, state);
    }

    /**
     * How high the candle's base floats above the bottom of its block right now, in blocks. Each candle
     * bobs a little out of step with its neighbours, so a row of them doesn't move together.
     */
    public static float floatHeight(Level level, BlockPos pos, float partialTicks) {
        double time = (level == null ? 0 : level.getGameTime()) + partialTicks;
        double phase = (pos.asLong() * 31L % 1000L) / 1000.0 * Math.PI * 2.0;
        return (float) (FLOAT_HEIGHT + BOB * Math.sin(time / BOB_TICKS * Math.PI * 2.0 + phase));
    }

    /** Runs every tick, on the player's screen only. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CorpseCandleBlockEntity candle) {
        RandomSource random = level.getRandom();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + floatHeight(level, pos, 0.0f) + FLAME_Y;
        double z = pos.getZ() + 0.5;

        // The outside of the orb: a speck of pale blue flame at a random spot inside the ball, drifting very slowly
        double dx = (random.nextDouble() * 2 - 1) * RADIUS;
        double dy = (random.nextDouble() * 2 - 1) * RADIUS;
        double dz = (random.nextDouble() * 2 - 1) * RADIUS;
        level.addParticle(ModParticles.CORPSE_FLAME, x + dx, y + dy, z + dz,
                -dx * 0.02, 0.002, -dz * 0.02);

        // The bright core: one more speck right in the middle every other tick
        if (level.getGameTime() % 2 == 0) {
            level.addParticle(ModParticles.CORPSE_FLAME,
                    x + (random.nextDouble() - 0.5) * 0.04,
                    y + (random.nextDouble() - 0.5) * 0.04,
                    z + (random.nextDouble() - 0.5) * 0.04,
                    0.0, 0.0, 0.0);
        }
    }
}
