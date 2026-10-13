package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.entity.Crawler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Draws the Crawler with {@link CrawlerModel}. */
public class CrawlerRenderer extends MobRenderer<Crawler, LivingEntityRenderState, CrawlerModel> {
	private static final Identifier TEXTURE = UmbralWorld.id("textures/entity/crawler/crawler.png");

	public CrawlerRenderer(EntityRendererProvider.Context context) {
		super(context, new CrawlerModel(context.bakeLayer(CrawlerModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}
}
