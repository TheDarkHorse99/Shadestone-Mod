package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The Wight's Shroud as a real 3D garment, worn by players and by the Wight itself: a tattered poncho.
 * A mantle lies over the shoulders, a flap hangs over each upper arm, and two long panels of cloth hang
 * down the front and back, past the waist. The panels are flat sheets (no thickness, seen from both
 * sides); their ragged edges come from see-through pixels in the texture. Walking makes them swing out
 * a little so the legs don't poke through.
 * The model is drawn from the body's own position (see ShroudLayer), so it leans and turns with the body.
 * Units are pixels; y points down, 0 is the top of the shoulders, and the front is -z.
 * Texture: 64 x 64, textures/entity/wights_shroud.png.
 */
public class ShroudModel extends Model<HumanoidRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UmbralWorld.id("wights_shroud"), "main");

	private final ModelPart front;
	private final ModelPart back;
	private final ModelPart rightFlap;
	private final ModelPart leftFlap;

	public ShroudModel(ModelPart root) {
		super(root, RenderTypes::entityCutout);
		this.front = root.getChild("front");
		this.back = root.getChild("back");
		this.rightFlap = root.getChild("right_flap");
		this.leftFlap = root.getChild("left_flap");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Over the shoulders: 14 wide (a little past the body on each side), 7 deep, with a hole for the neck
		root.addOrReplaceChild("mantle",
				CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, -1.0f, -3.5f, 14, 1, 7),
				PartPose.ZERO);
		// The front panel: 10 wide, hanging 18 down from the shoulders (the body is 12 tall)
		root.addOrReplaceChild("front",
				CubeListBuilder.create().texOffs(0, 8).addBox(-5.0f, 0.0f, 0.0f, 10, 18, 0),
				PartPose.offset(0.0f, -1.0f, -3.5f));
		// The back panel: wider and longer, 12 wide and 20 down
		root.addOrReplaceChild("back",
				CubeListBuilder.create().texOffs(20, 8).addBox(-6.0f, 0.0f, 0.0f, 12, 20, 0),
				PartPose.offset(0.0f, -1.0f, 3.5f));
		// A short ragged flap over each upper arm, hanging from the mantle's edge
		root.addOrReplaceChild("right_flap",
				CubeListBuilder.create().texOffs(44, 8).addBox(0.0f, 0.0f, -3.5f, 0, 6, 7),
				PartPose.offset(-7.0f, -1.0f, 0.0f));
		root.addOrReplaceChild("left_flap",
				CubeListBuilder.create().texOffs(44, 22).addBox(0.0f, 0.0f, -3.5f, 0, 6, 7),
				PartPose.offset(7.0f, -1.0f, 0.0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(HumanoidRenderState state) {
		super.setupAnim(state); // back to the hanging pose above
		// The panels swing out with each step (so the legs stay under them) and stir a little when still
		float step = state.walkAnimationSpeed * Math.abs(Mth.cos(state.walkAnimationPos * 0.6662f));
		float stir = Mth.sin(state.ageInTicks * 0.06f) * 0.03f;
		front.xRot -= 0.06f + 0.9f * step + stir;
		back.xRot += 0.06f + 0.9f * step - stir;
		rightFlap.zRot += 0.15f + 0.3f * step;
		leftFlap.zRot -= 0.15f + 0.3f * step;
	}
}
