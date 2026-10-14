package io.github.thedarkhorse99.umbralworld.mixin;

import io.github.thedarkhorse99.umbralworld.effect.ModEffects;
import io.github.thedarkhorse99.umbralworld.effect.NightstalkerEffect;
import io.github.thedarkhorse99.umbralworld.world.UmbralVoidFall;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mobs decide how far away they can spot you as (their follow range) x (your
 * "visibility"). Invisibility shrinks visibility; Nightstalker makes it bigger, so
 * mobs notice you from further away. That's the price of the power.
 *
 * Also: a player falling out of the bottom of the Overworld lands in the Umbral World
 * instead of dying in the void (see UmbralVoidFall).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void umbral_world$nightstalkerIsNoticed(ServerLevel level, Entity looker, CallbackInfoReturnable<Double> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(ModEffects.NIGHTSTALKER)) {
            cir.setReturnValue(cir.getReturnValue() * NightstalkerEffect.DETECTION_MULTIPLIER);
        }
    }

    @Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
    private void umbral_world$fallIntoUmbralWorld(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && UmbralVoidFall.tryCatch(player)) {
            ci.cancel();
        }
    }
}
