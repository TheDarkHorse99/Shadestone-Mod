package io.github.thedarkhorse99.umbralworld.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The darkness beneath the world: a player who falls out of the bottom of the Overworld
 * or the End lands in the Umbral World instead of dying in the void. They arrive on the ground at the
 * same x and z, with their fall forgotten. Called from mixin/LivingEntityMixin.
 */
public final class UmbralVoidFall {
    private UmbralVoidFall() {
    }

    /** Returns true if the player was caught (so the void doesn't hurt them). */
    public static boolean tryCatch(ServerPlayer player) {
        if ((player.level().dimension() != Level.OVERWORLD && player.level().dimension() != Level.END)
                || player.isSpectator()
                || player.getAbilities().flying // a creative player flying about down there isn't falling
                || !player.isAlive()) {
            return false;
        }
        ServerLevel umbral = player.level().getServer().getLevel(ModDimensions.UMBRAL_WORLD);
        if (umbral == null) {
            return false;
        }
        int x = player.getBlockX();
        int z = player.getBlockZ();
        umbral.getChunk(x >> 4, z >> 4); // load the landing spot so the ground height is known
        int y = umbral.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        player.teleport(new TeleportTransition(umbral, new Vec3(x + 0.5, y, z + 0.5), Vec3.ZERO,
                player.getYRot(), player.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND.then(entity -> entity.resetFallDistance())));
        return true;
    }
}
