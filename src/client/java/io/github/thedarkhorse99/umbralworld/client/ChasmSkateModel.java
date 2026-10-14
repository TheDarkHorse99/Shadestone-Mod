package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * The Chasm Skate: a wide, flat body with a wing on each side, a long thin tail with a
 * stinger at the end, and two little eye bumps on top. Lying still, its wings barely
 * move; swimming, they ripple. Its top faces are the ones to camouflage.
 * Texture: 64 x 64; see texture_guides/chasm_skate.png for where each part goes.
 * Units are pixels; y points down, the ground is at y = 24, and the front is -z.
 */
public class ChasmSkateModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UmbralWorld.id("chasm_skate"), "main");

	private final ModelPart body;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart tail;

	public ChasmSkateModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.rightWing = body.getChild("right_wing");
		this.leftWing = body.getChild("left_wing");
		this.tail = body.getChild("tail");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Flat on the ground: 14 wide, 2 thick, 12 long
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, -1.0f, -6.0f, 14, 2, 12),
				PartPose.offset(0.0f, 23.0f, 0.0f));
		body.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(28, 14).addBox(-6.0f, -0.5f, -4.0f, 6, 1, 8),
				PartPose.offset(-7.0f, 0.0f, 0.0f));
		body.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(0, 14).addBox(0.0f, -0.5f, -4.0f, 6, 1, 8),
				PartPose.offset(7.0f, 0.0f, 0.0f));
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(0, 23).addBox(-0.5f, -0.5f, 0.0f, 1, 1, 12),
				PartPose.offset(0.0f, 0.0f, 6.0f));
		tail.addOrReplaceChild("stinger",
				CubeListBuilder.create().texOffs(26, 23).addBox(-0.5f, -1.0f, 0.0f, 1, 1, 3),
				PartPose.offset(0.0f, 0.0f, 12.0f));
		body.addOrReplaceChild("right_eye",
				CubeListBuilder.create().texOffs(34, 23).addBox(-1.0f, -1.0f, -1.0f, 2, 1, 2),
				PartPose.offset(-3.0f, -1.0f, -4.0f));
		body.addOrReplaceChild("left_eye",
				CubeListBuilder.create().texOffs(42, 23).addBox(-1.0f, -1.0f, -1.0f, 2, 1, 2),
				PartPose.offset(3.0f, -1.0f, -4.0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state); // back to the flat pose above

		body.xRot += state.xRot * 0.5f * Mth.DEG_TO_RAD; // tilts a little when rising or diving

		// The wings ripple: barely at all lying still (so it stays hidden), strongly when swimming
		float moving = Math.min(1.0f, state.walkAnimationSpeed * 2.0f);
		float wave = state.ageInTicks * (0.1f + 0.4f * moving);
		float flap = Mth.sin(wave) * (0.03f + 0.45f * moving);
		leftWing.zRot += flap;
		rightWing.zRot -= flap;
		tail.yRot += Mth.sin(wave * 0.7f) * (0.05f + 0.2f * moving);
	}
}
