package io.github.thedarkhorse99.umbralworld.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.AshParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * A biome's drifting dust: vanilla's ash particle (the Grave's grey specks, which drift and
 * fall the same way), recoloured. Its picture is set in assets/umbral_world/particles/<name>.json.
 */
public class TintedAshProvider implements ParticleProvider<SimpleParticleType> {
	private final AshParticle.Provider ash;
	private final float red;
	private final float green;
	private final float blue;

	public TintedAshProvider(SpriteSet sprites, float red, float green, float blue) {
		this.ash = new AshParticle.Provider(sprites);
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	@Override
	public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
	                               double xd, double yd, double zd, RandomSource random) {
		Particle particle = ash.createParticle(type, level, x, y, z, xd, yd, zd, random);
		if (particle instanceof SingleQuadParticle quad) {
			// Some specks a little darker than others, like vanilla's ash
			float shade = 0.75f + random.nextFloat() * 0.25f;
			quad.setColor(red * shade, green * shade, blue * shade);
		}
		return particle;
	}
}
