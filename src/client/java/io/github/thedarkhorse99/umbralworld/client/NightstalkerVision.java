package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import io.github.thedarkhorse99.umbralworld.entity.FadeInLight;

/**
 * What the Nightstalker effect looks like on your own screen:
 * - the whole view goes washed-out and purple (assets/umbral_world/post_effect/nightstalker.json)
 * - hostile mobs you can actually see get a glowing outline, like the Glowing effect,
 *   but never through walls
 */
public final class NightstalkerVision {
    /** The screen filter. Points to assets/umbral_world/post_effect/nightstalker.json */
    public static final Identifier POST_EFFECT = UmbralWorld.id("nightstalker");
    /** How far away (in blocks) enemies can be outlined. */
    private static final double OUTLINE_RANGE = 32.0;

    private NightstalkerVision() {
    }

    public static boolean isActive() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(ModEffects.NIGHTSTALKER);
    }

    /** Should this mob get an outline on our screen right now? */
    public static boolean shouldOutline(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && entity != player
                && (entity instanceof Enemy || entity.getType().builtInRegistryHolder().is(FadeInLight.FADES_IN_LIGHT)) // and every Umbral mob, even the Ghoul (Mac)
                && player.hasEffect(ModEffects.NIGHTSTALKER)
                && player.distanceToSqr(entity) < OUTLINE_RANGE * OUTLINE_RANGE
                && player.hasLineOfSight(entity);
    }
}
