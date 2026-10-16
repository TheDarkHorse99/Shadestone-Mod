package io.github.thedarkhorse99.umbralworld.block;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Twice a second, looks at the few blocks around each player. If any of them is an
 * {@link AuraPlant}, the player gets that plant's effect for a few seconds. Standing
 * near keeps topping it up; walk away and it wears off.
 *
 * Checking around players (instead of having every plant look for players) means
 * thousands of plants in the world cost nothing, and plants from world generation
 * work straight away.
 */
public final class MiasmaPlantAuras {
    /** How often to check, in ticks (20 = 1 second). */
    private static final int CHECK_INTERVAL = 10;
    /** How far the aura reaches sideways, in blocks. */
    private static final int RADIUS = 2;
    /** How long the effect lasts after the last check: 4 seconds. */
    private static final int EFFECT_TICKS = 80;
    /** 1 = level II (Mac wanted stronger Slowness and Weakness; Nausea is the same at any level). */
    private static final int AMPLIFIER = 1;

    private MiasmaPlantAuras() {
    }

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(MiasmaPlantAuras::tick);
    }

    private static void tick(ServerLevel level) {
        if (level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            BlockPos feet = player.blockPosition();
            Set<Holder<MobEffect>> effects = new HashSet<>();
            for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-RADIUS, -1, -RADIUS), feet.offset(RADIUS, 1, RADIUS))) {
                if (level.getBlockState(pos).getBlock() instanceof AuraPlant plant) {
                    effects.add(plant.auraEffect());
                }
            }
            for (Holder<MobEffect> effect : effects) {
                // ambient = true: fainter swirl particles on the player, like a beacon's
                player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, AMPLIFIER, true, true));
            }
        }
    }
}
