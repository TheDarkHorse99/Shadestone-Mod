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
 * The Murkfiend: a Drowned-sized head, body and clawed arms, with a fish tail instead of
 * legs, swimming lying flat like a dolphin. The tail is three pieces (two segments and a
 * flat fin at the end) that beat up and down in a wave, and there's a fin on its back.
 * Texture: 64 x 64; see texture_guides/murkfiend.png for where each part goes.
 * Units are pixels; y points down, the ground is at y = 24, and the front is -z.
 */
public class MurkfiendModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UmbralWorld.id("murkfiend"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart tail;
	private final ModelPart tailEnd;
	private final ModelPart fin;

	public MurkfiendModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.tail = body.getChild("tail");
		this.tailEnd = tail.getChild("tail_end");
		this.fin = tailEnd.getChild("tail_fin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Everything hangs off the body, so tilting the body (swimming up or down) tilts it all
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 16).addBox(-4.0f, -2.0f, -6.0f, 8, 4, 12),
				PartPose.offset(0.0f, 17.0f, 0.0f));
		body.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -4.0f, -8.0f, 8, 8, 8),
				PartPose.offset(0.0f, 0.0f, -6.0f));
		body.addOrReplaceChild("back_fin",
				CubeListBuilder.create().texOffs(32, 0).addBox(-0.5f, -4.0f, -3.0f, 1, 4, 6),
				PartPose.offset(0.0f, -2.0f, -1.0f));
		// The arms lie back along its sides while it swims; the claws are at the far ends
		body.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(0, 32).addBox(-3.0f, -1.5f, -1.0f, 3, 3, 10),
				PartPose.offset(-4.0f, 0.0f, -4.0f));
		body.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(26, 32).addBox(0.0f, -1.5f, -1.0f, 3, 3, 10),
				PartPose.offset(4.0f, 0.0f, -4.0f));
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(0, 45).addBox(-3.0f, -2.0f, 0.0f, 6, 4, 8),
				PartPose.offset(0.0f, 0.0f, 6.0f));
		PartDefinition tailEnd = tail.addOrReplaceChild("tail_end",
				CubeListBuilder.create().texOffs(28, 45).addBox(-2.0f, -1.5f, 0.0f, 4, 3, 8),
				PartPose.offset(0.0f, 0.0f, 8.0f));
		tailEnd.addOrReplaceChild("tail_fin",
				CubeListBuilder.create().texOffs(0, 57).addBox(-5.0f, -0.5f, 0.0f, 10, 1, 6),
				PartPose.offset(0.0f, 0.0f, 8.0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state); // back to the pose above

		// Tilt with the way it's swimming (nose up when rising), and turn its head to look
		body.xRot += state.xRot * Mth.DEG_TO_RAD;
		head.yRot += Mth.clamp(state.yRot, -45.0f, 45.0f) * Mth.DEG_TO_RAD;

		// The tail beats in a wave: each piece a little behind the one before. Faster and
		// bigger when it's moving (one beat about every 1.5 seconds), a slow sway when it's still
		// (about every 4 seconds). Mac found the old beat (twice a second) too fast.
		float moving = Math.min(1.0f, state.walkAnimationSpeed * 2.0f);
		float wave = state.ageInTicks * (0.08f + 0.13f * moving);
		float size = 0.15f + 0.3f * moving;
		tail.xRot += Mth.sin(wave) * size * 0.6f;
		tailEnd.xRot += Mth.sin(wave - 0.9f) * size;
		fin.xRot += Mth.sin(wave - 1.8f) * size * 1.2f;

		// The arms stroke out and back, and the claws flex
		float stroke = Mth.sin(wave * 0.5f) * (0.1f + 0.25f * moving);
		rightArm.yRot += 0.15f + stroke;
		leftArm.yRot -= 0.15f + stroke;
		rightArm.xRot += Mth.cos(wave * 0.5f) * 0.1f;
		leftArm.xRot += Mth.cos(wave * 0.5f) * 0.1f;
	}
}
