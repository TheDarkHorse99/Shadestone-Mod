package io.github.thedarkhorse99.umbralworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.item.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the Wight's Shroud (ShroudModel) round the body of players and armor stands wearing it in the
 * chest slot. (The Wight's own shroud is painted on its texture.) Like the rest of the wearer it isn't drawn while a player is
 * invisible (vanilla would otherwise still draw worn things on an invisible player). An invisible armor
 * stand still shows it, like vanilla armor, so it can be put on display.
 */
public class ShroudLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	public static final Identifier TEXTURE = UmbralWorld.id("textures/entity/wights_shroud.png");

	private final ShroudModel model;
	private final boolean alwaysWorn;

	/** alwaysWorn: draw it whatever is in the chest slot (the Wight wears its shroud as part of itself). */
	public ShroudLayer(RenderLayerParent<S, M> parent, EntityModelSet models, boolean alwaysWorn) {
		super(parent);
		this.model = new ShroudModel(models.bakeLayer(ShroudModel.LAYER));
		this.alwaysWorn = alwaysWorn;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		boolean hidden = state.isInvisible && !(state instanceof ArmorStandRenderState);
		if (hidden || !(alwaysWorn || state.chestEquipment.is(ModItems.WIGHTS_SHROUD))) {
			return;
		}
		poseStack.pushPose();
		// Move to where the body is (it leans when sneaking, and sits higher on the Wight's long legs)
		getParentModel().root().translateAndRotate(poseStack);
		getParentModel().body.translateAndRotate(poseStack);
		renderColoredCutoutModel(model, TEXTURE, poseStack, collector, light, state, -1, 1);
		poseStack.popPose();
	}
}
