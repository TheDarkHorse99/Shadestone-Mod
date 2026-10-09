package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.effect.ModEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class DeepBlindnessFogEnvironment extends MobEffectFogEnvironment {
    private static final float FOG_END = 2.0f; // vanilla Blindness uses 5.0

    @Override
    public Holder<MobEffect> getMobEffect() {
        return ModEffects.DEEP_BLINDNESS;
    }

    @Override
    public void setupFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
        Entity entity = camera.entity();
        if (entity instanceof LivingEntity living) {
            MobEffectInstance effect = living.getEffect(this.getMobEffect());
            if (effect != null) {
                float distance = effect.isInfiniteDuration()
                        ? FOG_END
                        : Mth.lerp(Math.min(1.0f, effect.getDuration() / 20.0f), renderDistance, FOG_END);
                fog.environmentalStart = 0.0f;
                fog.environmentalEnd = distance;
                fog.skyEnd = distance * 0.8f;
                fog.cloudEnd = distance * 0.8f;
            }
        }
    }

    @Override
    public float getModifiedDarkness(LivingEntity entity, float darkness, float partialTickTime) {
        MobEffectInstance instance = entity.getEffect(this.getMobEffect());
        if (instance != null) {
            darkness = instance.endsWithin(19) ? Math.max(instance.getDuration() / 20.0f, darkness) : 1.0f;
        }
        return darkness;
    }
}