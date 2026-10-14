package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.world.ModDimensions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Umbral World's darkness snuffs out vanilla flames: a candle, candle cake or campfire
 * that gets lit there flickers for a moment, then goes out with a puff of smoke.
 *
 * Every block change passes through {@link #onBlockChanged} (from mixin/LevelMixin). A
 * newly lit flame in the Umbral World goes on a short waiting list, and the tick handler
 * puts it out when its time is up. Catching the change itself means every way of lighting
 * works the same: flint and steel, fire charges, burning arrows, placing a campfire.
 */
public final class UmbralSnuffing {
    /** How long a flame lasts before it's snuffed, in ticks (20 = 1 second): 0.5 to 1.5 seconds. */
    private static final int MIN_DELAY = 10;
    private static final int MAX_DELAY = 30;

    /** Flames waiting to be snuffed, and the game time when they go out. */
    private static final Map<BlockPos, Long> WAITING = new HashMap<>();

    private UmbralSnuffing() {
    }

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(UmbralSnuffing::tick);
        // Forget the list when a world is closed, so nothing carries over into the next one
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> WAITING.clear());
    }

    /** Called after any block in any level changes. */
    public static void onBlockChanged(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel
                && serverLevel.dimension() == ModDimensions.UMBRAL_WORLD
                && isLitFlame(state)
                && !WAITING.containsKey(pos)) {
            int delay = MIN_DELAY + serverLevel.getRandom().nextInt(MAX_DELAY - MIN_DELAY + 1);
            WAITING.put(pos.immutable(), serverLevel.getGameTime() + delay);
        }
    }

    private static boolean isLitFlame(BlockState state) {
        return (state.getBlock() instanceof AbstractCandleBlock && AbstractCandleBlock.isLit(state))
                || CampfireBlock.isLitCampfire(state);
    }

    private static void tick(ServerLevel level) {
        if (WAITING.isEmpty() || level.dimension() != ModDimensions.UMBRAL_WORLD) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Long>> entries = WAITING.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<BlockPos, Long> entry = entries.next();
            if (entry.getValue() > level.getGameTime()) {
                continue;
            }
            entries.remove();
            BlockPos pos = entry.getKey();
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isLitFlame(state)) {
                continue; // already out, or broken
            }
            if (state.getBlock() instanceof AbstractCandleBlock) {
                // Vanilla's own "blown out" (sound and all), as if a player had put it out
                AbstractCandleBlock.extinguish(null, state, level, pos);
            } else {
                // Vanilla's own dousing (sound, and drops whatever was cooking), then set it unlit
                CampfireBlock.douse(null, level, pos, state);
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), Block.UPDATE_ALL);
            }
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.2, 0.2, 0.2, 0.01);
        }
    }
}
