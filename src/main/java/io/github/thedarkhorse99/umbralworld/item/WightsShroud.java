package io.github.thedarkhorse99.umbralworld.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The Wight's Shroud, worn in the chest slot, turns its wearer invisible while they
 * stand in light level 5 or brighter. In the dark they can be seen as normal.
 *
 * It works by handing out vanilla Invisibility in short bursts, so mobs, other players
 * and the game itself treat the wearer exactly like someone who drank the potion.
 * (The shroud is hidden along with them: see the client's HumanoidArmorLayerMixin.)
 */
public final class WightsShroud {
    /** Invisible at this light level or brighter. */
    public static final int MIN_LIGHT = 5;
    /** How often to check the light, in ticks (20 = 1 second). */
    private static final int CHECK_INTERVAL = 5;
    /** How long each burst of Invisibility lasts. A little longer than the check, so it never flickers off. */
    private static final int EFFECT_TICKS = CHECK_INTERVAL * 3;

    private WightsShroud() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(WightsShroud::tick);
    }

    public static boolean isWearing(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.WIGHTS_SHROUD);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isWearing(player) && isLit(player)) {
                // ambient, no swirls, no icon: it's the shroud, not a potion
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_TICKS, 0, true, false, false));
            } else {
                endShroudInvisibility(player);
            }
        }
    }

    /** Light where the player's feet or head are. Sunlight counts, so the shroud works in the Overworld by day. */
    private static boolean isLit(ServerPlayer player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        BlockPos head = BlockPos.containing(player.getEyePosition());
        return Math.max(level.getMaxLocalRawBrightness(feet), level.getMaxLocalRawBrightness(head)) >= MIN_LIGHT;
    }

    /**
     * Stepping into the dark (or taking the shroud off) ends the shroud's invisibility straight away.
     * Only a burst from the shroud is removed: a real Invisibility potion isn't ambient, so it's left alone.
     */
    private static void endShroudInvisibility(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.INVISIBILITY);
        if (current != null && current.isAmbient() && !current.isVisible() && current.getDuration() <= EFFECT_TICKS) {
            player.removeEffect(MobEffects.INVISIBILITY);
        }
    }
}
