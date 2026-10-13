package io.github.thedarkhorse99.umbralworld.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import io.github.thedarkhorse99.umbralworld.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import io.github.thedarkhorse99.umbralworld.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class UmbralWorldClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModRenderPipelines.initialize();
		MenuScreens.register(ModMenus.DARK_FURNACE, DarkFurnaceScreen::new);
		EntityRenderers.register(ModEntities.WIGHT, WightRenderer::new);
		ModelLayerRegistry.registerModelLayer(GhoulModel.LAYER, GhoulModel::createBodyLayer);
		EntityRenderers.register(ModEntities.GHOUL, GhoulRenderer::new);
	}
}