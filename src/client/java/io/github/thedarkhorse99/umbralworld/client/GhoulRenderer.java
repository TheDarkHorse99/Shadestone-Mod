package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.entity.Ghoul;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** Draws the Ghoul: its dark body, then its glowing eyes on top, which show through fog. */
public class GhoulRenderer extends MobRenderer<Ghoul, LivingEntityRenderState, GhoulModel> {
	private static final Identifier TEXTURE = UmbralWorld.id("textures/entity/ghoul/ghoul.png");
	private static final Identifier EYES_TEXTURE = UmbralWorld.id("textures/entity/ghoul/ghoul_eyes.png");

	private static final RenderType EYES = RenderType.create("umbral_world_ghoul_eyes",
			RenderSetup.builder(ModRenderPipelines.GLOWING_EYES)
					.withTexture("Sampler0", EYES_TEXTURE)
					.sortOnUpload()
					.createRenderSetup());

	public GhoulRenderer(EntityRendererProvider.Context context) {
		super(context, new GhoulModel(context.bakeLayer(GhoulModel.LAYER)), 0.5f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return EYES;
			}
		});
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
