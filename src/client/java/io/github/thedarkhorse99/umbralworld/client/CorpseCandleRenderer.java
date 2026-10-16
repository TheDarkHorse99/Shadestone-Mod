package io.github.thedarkhorse99.umbralworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.block.entity.CorpseCandleBlockEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Corpse Candle (Mac, 2026-10-16): a see-through, glowing pale blue candle the size of a
 * vanilla candle, floating just above the ground and slowly bobbing up and down. Its flame is the
 * sparkle particles that CorpseCandleBlockEntity makes at the wick, which bob along with it.
 * Texture: textures/entity/corpse_candle.png (16 x 16; the candle's sides use the top-left 8 x 8, its
 * wick the strip below). Its see-through-ness comes from the texture's alpha.
 */
public class CorpseCandleRenderer implements BlockEntityRenderer<CorpseCandleBlockEntity, CorpseCandleRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UmbralWorld.id("corpse_candle"), "main");
	private static final Identifier TEXTURE = UmbralWorld.id("textures/entity/corpse_candle.png");
	/** Glows like a light source, ignores the darkness around it. */
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TEXTURE);

	private final ModelPart candle;

	public CorpseCandleRenderer(BlockEntityRendererProvider.Context context) {
		this.candle = context.bakeLayer(LAYER);
	}

	/**
	 * A vanilla-sized candle (2 x 6 x 2) with a 1-pixel wick. Like every entity model, y points down
	 * (the texture's top row is the top of the candle), so it hangs from the origin and is turned the
	 * right way up when drawn. Units are pixels (a model part turns them into blocks itself).
	 */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -6.0f, -1.0f, 2, 6, 2), PartPose.ZERO);
		mesh.getRoot().addOrReplaceChild("wick",
				CubeListBuilder.create().texOffs(0, 8).addBox(-0.5f, -7.0f, -0.5f, 1, 1, 1), PartPose.ZERO);
		return LayerDefinition.create(mesh, 16, 16);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CorpseCandleBlockEntity candle, State state, float partialTicks, Vec3 cameraPos,
								   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(candle, state, partialTicks, cameraPos, breakProgress);
		state.height = CorpseCandleBlockEntity.floatHeight(candle.getLevel(), candle.getBlockPos(), partialTicks);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.5, state.height, 0.5);
		poseStack.rotateDegrees(Axis.ZP, 180.0f); // y down (model) to y up (world), without mirroring
		collector.submitModelPart(candle, poseStack, RENDER_TYPE, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null, -1, 0);
		poseStack.popPose();
	}

	public static class State extends BlockEntityRenderState {
		/** How far above the bottom of the block the candle's base is right now, in blocks. */
		float height;
	}
}
