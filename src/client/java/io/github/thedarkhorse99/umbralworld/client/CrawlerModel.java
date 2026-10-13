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
 * The Crawler's body: a zombie's head, body and arms with no legs, lying face down
 * and reaching forward. Uses the same 64x64 texture layout as vanilla's zombie
 * (the leg areas are simply unused), so its texture can start from a copy of zombie.png.
 * Units are pixels; y points down, the ground is at y = 24, and the front is -z.
 */
public class CrawlerModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UmbralWorld.id("crawler"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public CrawlerModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body lies flat on its belly, neck at the front, torn-off waist at the back
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0f, 0.0f, -2.0f, 8, 12, 4),
				PartPose.offsetAndRotation(0.0f, 22.0f, -2.0f, Mth.HALF_PI, 0.0f, 0.0f));
		// The head is lifted a little off the ground, looking ahead
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -7.0f, -7.0f, 8, 8, 8),
				PartPose.offset(0.0f, 22.0f, -2.0f));
		// The arms reach out in front, flat on the ground, slightly spread
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0f, -2.0f, -2.0f, 4, 12, 4),
				PartPose.offsetAndRotation(-5.0f, 22.0f, -1.0f, -Mth.HALF_PI, 0.15f, 0.0f));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0f, -2.0f, -2.0f, 4, 12, 4),
				PartPose.offsetAndRotation(5.0f, 22.0f, -1.0f, -Mth.HALF_PI, -0.15f, 0.0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state); // back to the lying pose above

		// Look where it's looking (only a little up and down: its chin is on the ground)
		head.yRot += state.yRot * Mth.DEG_TO_RAD;
		head.xRot += Mth.clamp(state.xRot, -30.0f, 10.0f) * Mth.DEG_TO_RAD;

		// Crawling: the arms take turns reaching forward and dragging back, and the body twists with them
		float pos = state.walkAnimationPos * 0.6662f;
		float speed = state.walkAnimationSpeed;
		float pull = Mth.cos(pos) * speed;
		rightArm.yRot += pull * 0.6f;
		leftArm.yRot += pull * 0.6f;
		rightArm.xRot -= Math.max(0.0f, Mth.sin(pos)) * 0.4f * speed;          // lifts the reaching arm
		leftArm.xRot -= Math.max(0.0f, -Mth.sin(pos)) * 0.4f * speed;
		body.zRot += pull * 0.1f;

		// Even lying still, its fingers twitch now and then
		float twitch = Mth.cos(state.ageInTicks * 0.15f) * 0.04f;
		rightArm.yRot += twitch;
		leftArm.yRot -= twitch;
	}
}
