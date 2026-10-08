package io.github.thedarkhorse99.umbralworld.world;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ModDimensions {
    public static final ResourceKey<Level> UMBRAL_WORLD =
            ResourceKey.create(Registries.DIMENSION, UmbralWorld.id("umbral_world"));

    private ModDimensions() {
    }
}