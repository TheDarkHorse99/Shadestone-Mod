package io.github.thedarkhorse99.umbralworld.entity;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.item.WightsShroud;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

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
                if (WightsShroud.isLit(living)) { // light 5+, not counting Corpse Candles
                    WightsShroud.makeInvisible(living);
                } else {
                    WightsShroud.endShroudInvisibility(living);
                }
            }
        }
    }
}
