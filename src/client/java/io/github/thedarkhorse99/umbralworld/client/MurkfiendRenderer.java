package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.entity.Murkfiend;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Draws the Murkfiend with {@link MurkfiendModel}. */
public class MurkfiendRenderer extends MobRenderer<Murkfiend, LivingEntityRenderState, MurkfiendModel> {
	private static final Identifier TEXTURE = UmbralWorld.id("textures/entity/murkfiend/murkfiend.png");

	public MurkfiendRenderer(EntityRendererProvider.Context context) {
		super(context, new MurkfiendModel(context.bakeLayer(MurkfiendModel.LAYER)), 0.5f);
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
