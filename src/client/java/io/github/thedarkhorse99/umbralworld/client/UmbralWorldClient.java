package io.github.thedarkhorse99.umbralworld.client;

import net.fabricmc.api.ClientModInitializer;
import io.github.thedarkhorse99.umbralworld.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;

public class UmbralWorldClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModRenderPipelines.initialize();
		MenuScreens.register(ModMenus.DARK_FURNACE, DarkFurnaceScreen::new);
	}
}