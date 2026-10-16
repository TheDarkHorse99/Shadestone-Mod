package io.github.thedarkhorse99.umbralworld.entity;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.item.WightsShroud;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import io.github.thedarkhorse99.umbralworld.block.CorpseCandleBlock;
import io.github.thedarkhorse99.umbralworld.block.entity.CorpseCandleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The Umbral World's creatures fade out in the light (Mac, 2026-10-16): standing in light level 5 or
 * brighter they turn invisible, exactly like a player wearing the Wight's Shroud, and show again in the
 * dark. Which mobs do it is the entity tag #umbral_world:fades_in_light
 * (data/umbral_world/tags/entity_type/fades_in_light.json).
 * Corpse Candles are as bright as a torch but their light never hides these mobs (Mac, 2026-10-16).
 */
public final class FadeInLight {
    public static final TagKey<EntityType<?>> FADES_IN_LIGHT =
            TagKey.create(Registries.ENTITY_TYPE, UmbralWorld.id("fades_in_light"));
    /**
     * Only for the in-game tests, which photograph the mobs in daylight to show their art: while true,
     * nothing fades. Never set in normal play.
     */
    public static boolean pausedForTests = false;
    /** How often to check, in ticks: the same as the Shroud. */
    private static final int CHECK_INTERVAL = 5;

    private FadeInLight() {
    }

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(FadeInLight::tick);
    }

    private static void tick(ServerLevel level) {
        if (pausedForTests || level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && living.isAlive()
                    && living.getType().builtInRegistryHolder().is(FADES_IN_LIGHT)) {
                if (isLit(level, living)) {
                    WightsShroud.makeInvisible(living);
                } else {
                    WightsShroud.endShroudInvisibility(living);
                }
            }
        }
    }

    /** Light 5+ at its feet or head, not counting light that could all be coming from Corpse Candles. */
    private static boolean isLit(ServerLevel level, LivingEntity entity) {
        if (!WightsShroud.isLit(entity)) {
            return false; // quick check first: dark is dark
        }
        BlockPos feet = entity.blockPosition();
        BlockPos head = BlockPos.containing(entity.getEyePosition());
        return isLitIgnoringCandles(level, feet) || isLitIgnoringCandles(level, head);
    }

    private static boolean isLitIgnoringCandles(ServerLevel level, BlockPos pos) {
        int sky = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
        if (sky >= WightsShroud.MIN_LIGHT) {
            return true;
        }
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        // Light fades by 1 per block, and walls only make it fade faster, so a candle n blocks away
        // (counting each step along x, y and z) gives at most 14 - n here. If the light here is no
        // brighter than the nearest candle could make it, the candles may be all there is.
        return block >= WightsShroud.MIN_LIGHT && block > brightestCandle(level, pos);
    }

    /** The most light any Corpse Candle within reach could give at pos (0 if none). */
    private static int brightestCandle(ServerLevel level, BlockPos pos) {
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
                        best = Math.max(best, CorpseCandleBlock.LIGHT - blockEntity.getBlockPos().distManhattan(pos));
                    }
                }
            }
        }
        return best;
    }
}
