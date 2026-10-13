package io.github.thedarkhorse99.umbralworld.block.entity;

import net.minecraft.core.BlockPos;
import io.github.thedarkhorse99.umbralworld.particle.ModParticles;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Corpse Candle has no model: it is drawn only with pale blue flame particles, packed into a
 * little ball. A block entity is needed because it can run every tick on the player's
 * screen. A plain block only gets the occasional random display tick, which would leave
 * gaps in the flame, especially from a few blocks away.
 * It stores nothing.
 */
public class CorpseCandleBlockEntity extends BlockEntity {
    /** Height of the orb's centre above the bottom of the block (5 pixels). */
    private static final double CENTER_Y = 0.32;
    /** Radius of the orb, in blocks (about 2 pixels). */
    private static final double RADIUS = 0.12;

    public CorpseCandleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORPSE_CANDLE, pos, state);
    }

    /** Runs every tick, on the player's screen only. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CorpseCandleBlockEntity candle) {
        RandomSource random = level.getRandom();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + CENTER_Y;
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
