package io.github.thedarkhorse99.umbralworld.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

/**
 * The Corpse Candle's flame: a small pale blue glow, the same colour family as the Ghost
 * Tree. It uses vanilla's white "glow" picture (the glow squid's sparkle), tinted in code,
 * so it can be any colour. It always shines at full brightness, drifts very slowly,
 * shrinks and fades out. Its picture is set in assets/umbral_world/particles/corpse_flame.json.
 */
public class CorpseFlameParticle extends SingleQuadParticle {
	/** Pale light blue (red, green, blue from 0 to 1). */
	private static final float RED = 0.70f;
	private static final float GREEN = 0.87f;
	private static final float BLUE = 1.0f;

	private final float startAlpha;

	protected CorpseFlameParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
	                              TextureAtlasSprite sprite, RandomSource random) {
		super(level, x, y, z, sprite);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.hasPhysics = false;
		this.gravity = 0.0f;
		this.friction = 0.96f;
		this.lifetime = 16 + random.nextInt(16);
		this.quadSize = 0.06f + random.nextFloat() * 0.05f;
		// A touch of variety: some specks almost white, some a little bluer
		float shade = 0.9f + random.nextFloat() * 0.1f;
		setColor(RED * shade, GREEN * shade, BLUE);
		this.startAlpha = 0.85f;
		setAlpha(startAlpha);
	}

	@Override
	public void tick() {
		super.tick();
		// Fade out over the last half of its life
		float life = (float) age / lifetime;
		setAlpha(life < 0.5f ? startAlpha : startAlpha * (1.0f - life) * 2.0f);
	}

	/** Shrinks a little as it burns out. */
	@Override
	public float getQuadSize(float partialTick) {
		float life = (age + partialTick) / lifetime;
		return quadSize * (1.0f - life * life * 0.5f);
	}

	/** Always fully lit, so it glows in the dark. */
	@Override
	public int getLightCoords(float partialTick) {
		return LightCoordsUtil.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
		                               double xd, double yd, double zd, RandomSource random) {
			return new CorpseFlameParticle(level, x, y, z, xd, yd, zd, sprites.get(random), random);
		}
	}
}
