package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.entity.ChasmSkate;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Draws the Chasm Skate with {@link ChasmSkateModel}. */
public class ChasmSkateRenderer extends MobRenderer<ChasmSkate, LivingEntityRenderState, ChasmSkateModel> {
	private static final Identifier TEXTURE = UmbralWorld.id("textures/entity/chasm_skate/chasm_skate.png");

	public ChasmSkateRenderer(EntityRendererProvider.Context context) {
		super(context, new ChasmSkateModel(context.bakeLayer(ChasmSkateModel.LAYER)), 0.6f);
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
