package io.github.thedarkhorse99.umbralworld.effect;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

/**
 * Potions of the Nightstalker. The brewing recipes that make them are data files in
 * data/umbral_world/recipe/brewing/. Splash, lingering and arrow versions come for free.
 */
public final class ModPotions {
    // "nightstalker" is the name used for the potion's in-game title in lang/en_us.json
    public static final Holder<Potion> NIGHTSTALKER = Registry.registerForHolder(
            BuiltInRegistries.POTION, UmbralWorld.id("nightstalker"),
            new Potion("nightstalker", new MobEffectInstance(ModEffects.NIGHTSTALKER, 3600))); // 3:00

    // Brewed with redstone. Same name, so it shows as the same potion with a longer time.
    public static final Holder<Potion> LONG_NIGHTSTALKER = Registry.registerForHolder(
            BuiltInRegistries.POTION, UmbralWorld.id("long_nightstalker"),
            new Potion("nightstalker", new MobEffectInstance(ModEffects.NIGHTSTALKER, 9600))); // 8:00

    private ModPotions() {
    }

    public static void initialize() {
    }
}
