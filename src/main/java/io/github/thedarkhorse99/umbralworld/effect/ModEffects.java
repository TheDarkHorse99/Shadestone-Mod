package io.github.thedarkhorse99.umbralworld.effect;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

public final class ModEffects {
    public static final Holder<MobEffect> DEEP_BLINDNESS = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT, UmbralWorld.id("deep_blindness"), new DeepBlindnessEffect());

    private ModEffects() {
    }

    public static void initialize() {
    }
}