package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * The Wight's Shroud as worn by players and armor stands: the same cloth Mac drew on the Wight's body,
 * wrapped round the wearer's body like a tunic (not a cape). The player's body is the same size as the
 * Wight's (8 x 12 x 4), so the texture uses the Wight's body layout and lines up exactly.
 * It sits a little outside the body (and the player's jacket layer). See-through pixels are the gaps.
 * Drawn from the body's own position (see ShroudLayer).
 * Texture: 64 x 32, textures/entity/wights_shroud.png (the cloth from textures/entity/wight/wight.png).
 */
public class ShroudModel extends Model<HumanoidRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UmbralWorld.id("wights_shroud"), "main");

	public ShroudModel(ModelPart root) {
		super(root, RenderTypes::entityCutout);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		// The body box, like vanilla's (Enderman and player bodies share it), 0.4 pixels bigger all round
		mesh.getRoot().addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(32, 16).addBox(-4.0f, 0.0f, -2.0f, 8, 12, 4, new CubeDeformation(0.4f)),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}
}
