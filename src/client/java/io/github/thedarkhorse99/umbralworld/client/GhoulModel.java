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
 * The Ghoul's body: a zombie's parts, bent over. Uses the same 64x64 texture layout
 * as vanilla's zombie, so its texture can start from a copy of zombie.png.
 * Units are pixels; y points down, and the ground is at y = 24.
 */
public class GhoulModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UmbralWorld.id("ghoul"), "main");

	/** How far the back bends forward, in radians (1.0 = about 57 degrees). */
	private static final float HUNCH = 1.0f;

	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public GhoulModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body pivots at the hips and leans forward, so the shoulders end up low and in front
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0f, -12.0f, -2.0f, 8, 12, 4),
				PartPose.offsetAndRotation(0.0f, 12.0f, 7.0f, HUNCH, 0.0f, 0.0f));
		// The head juts forward from the top of the bent back
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8),
				PartPose.offset(0.0f, 6.0f, -5.0f));
		// Arms hang down from the low shoulders
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0f, -2.0f, -2.0f, 4, 12, 4),
				PartPose.offset(-5.0f, 6.6f, -1.4f));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0f, -2.0f, -2.0f, 4, 12, 4),
				PartPose.offset(5.0f, 6.6f, -1.4f));
		// Legs stand straight under the hips
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4),
				PartPose.offset(-1.9f, 12.0f, 7.0f));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4),
				PartPose.offset(1.9f, 12.0f, 7.0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state); // back to the hunched pose above

		// Look where it's looking
		head.yRot += state.yRot * Mth.DEG_TO_RAD;
		head.xRot += state.xRot * Mth.DEG_TO_RAD;

		// Walking: legs swing like a zombie's, the dangling arms swing the other way
		float pos = state.walkAnimationPos * 0.6662f;
		float speed = state.walkAnimationSpeed;
		rightLeg.xRot += Mth.cos(pos) * 1.4f * speed;
		leftLeg.xRot += Mth.cos(pos + Mth.PI) * 1.4f * speed;
		rightArm.xRot += Mth.cos(pos + Mth.PI) * 0.8f * speed;
		leftArm.xRot += Mth.cos(pos) * 0.8f * speed;

		// The arms sway a little even when it stands still
		float sway = Mth.cos(state.ageInTicks * 0.09f) * 0.05f + 0.05f;
		rightArm.zRot += sway;
		leftArm.zRot -= sway;
	}
}
