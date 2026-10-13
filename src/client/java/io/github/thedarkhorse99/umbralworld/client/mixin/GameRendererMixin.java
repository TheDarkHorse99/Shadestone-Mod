package io.github.thedarkhorse99.umbralworld.client.mixin;

import io.github.thedarkhorse99.umbralworld.client.NightstalkerVision;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every frame the game rebuilds its list of screen filters (the spectator creeper view
 * and the like). While Nightstalker is active, add ours to the list.
 */
@NullMarked
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void umbral_world$addNightstalkerFilter(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (NightstalkerVision.isActive()) {
            ((GameRenderer) (Object) this).getRequestedPostEffects().add(NightstalkerVision.POST_EFFECT);
        }
    }
}
