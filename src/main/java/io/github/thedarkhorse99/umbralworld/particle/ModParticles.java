package io.github.thedarkhorse99.umbralworld.particle;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The mod's own particle types. Each one needs a matching
 * assets/umbral_world/particles/<name>.json (which picture it uses) and a client-side
 * class that says how it moves and glows (see the client's CorpseFlameParticle).
 */
public final class ModParticles {
	/** The Corpse Candle's pale blue flame. */
	public static final SimpleParticleType CORPSE_FLAME = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE, UmbralWorld.id("corpse_flame"), FabricParticleTypes.simple());

	private ModParticles() {
	}

	public static void initialize() {
	}
}
