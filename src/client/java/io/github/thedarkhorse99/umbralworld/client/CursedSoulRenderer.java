package io.github.thedarkhorse99.umbralworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.entity.CursedSoul;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Draws the Cursed Soul as one flat picture that always faces the camera (like the
 * Ender Dragon's fireball), at full brightness so its glow shows in the dark.
 * The texture may use see-through and half-see-through pixels for a soft glow.
 */
public class CursedSoulRenderer extends EntityRenderer<CursedSoul, EntityRenderState> {
	private static final Identifier TEXTURE = UmbralWorld.id("textures/entity/cursed_soul/cursed_soul.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE);
	/** Size of the picture, in blocks. */
	private static final float SIZE = 0.75f;

	public CursedSoulRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	/** Always fully lit: it glows. */
	@Override
	protected int getBlockLightLevel(CursedSoul soul, BlockPos pos) {
		return 15;
	}

	@Override
	public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0f, state.boundingBoxHeight / 2, 0.0f); // centre of its hitbox
		poseStack.rotate(camera.orientation);                           // face the camera
		poseStack.scale(SIZE, SIZE, SIZE);
		collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> quad(state, pose, consumer, -1));
		// The Glowing / Nightstalker outline
		if (state.outlineColor != 0 && RENDER_TYPE.outline().isPresent()) {
			collector.submitCustomGeometry(poseStack, RENDER_TYPE.outline().get(),
					(pose, consumer) -> quad(state, pose, consumer, state.outlineColor));
		}
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	/** One square, centred on the soul. */
	private static void quad(EntityRenderState state, PoseStack.Pose pose, VertexConsumer consumer, int color) {
		vertex(consumer, pose, state.lightCoords, color, -0.5f, -0.5f, 0.0f, 1.0f);
		vertex(consumer, pose, state.lightCoords, color, 0.5f, -0.5f, 1.0f, 1.0f);
		vertex(consumer, pose, state.lightCoords, color, 0.5f, 0.5f, 1.0f, 0.0f);
		vertex(consumer, pose, state.lightCoords, color, -0.5f, 0.5f, 0.0f, 0.0f);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light, int color, float x, float y, float u, float v) {
		consumer.addVertex(pose, x, y, 0.0f)
				.setColor(color)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0.0f, 1.0f, 0.0f);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}
