package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.entity.Wight;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;

/** Draws the Wight using vanilla's Enderman body with the Wight texture. */
public class WightRenderer extends MobRenderer<Wight, EndermanRenderState, EndermanModel<EndermanRenderState>> {
	private static final Identifier TEXTURE = UmbralWorld.id("textures/entity/wight/wight.png");

	public WightRenderer(EntityRendererProvider.Context context) {
		super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5f);
	}

	@Override
	public Identifier getTextureLocation(EndermanRenderState state) {
		return TEXTURE;
	}

	@Override
	public EndermanRenderState createRenderState() {
		return new EndermanRenderState();
	}

	@Override
	public void extractRenderState(Wight entity, EndermanRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, itemModelResolver);
	}
}
