package io.github.thedarkhorse99.umbralworld.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import io.github.thedarkhorse99.umbralworld.particle.ModParticles;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import io.github.thedarkhorse99.umbralworld.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import io.github.thedarkhorse99.umbralworld.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class UmbralWorldClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModRenderPipelines.initialize();
		ParticleProviderRegistry.getInstance().register(ModParticles.CORPSE_FLAME, CorpseFlameParticle.Provider::new);
		// Each biome's drifting dust: vanilla's ash, recoloured (the Grave keeps vanilla's grey ash)
		ParticleProviderRegistry.getInstance().register(ModParticles.MIASMA_DUST, sprites -> new TintedAshProvider(sprites, 0.80f, 0.66f, 0.95f));
		ParticleProviderRegistry.getInstance().register(ModParticles.CHASM_DUST, sprites -> new TintedAshProvider(sprites, 0.74f, 0.84f, 0.95f));
		MenuScreens.register(ModMenus.DARK_FURNACE, DarkFurnaceScreen::new);
		EntityRenderers.register(ModEntities.WIGHT, WightRenderer::new);
		ModelLayerRegistry.registerModelLayer(GhoulModel.LAYER, GhoulModel::createBodyLayer);
		EntityRenderers.register(ModEntities.GHOUL, GhoulRenderer::new);
		ModelLayerRegistry.registerModelLayer(CrawlerModel.LAYER, CrawlerModel::createBodyLayer);
		EntityRenderers.register(ModEntities.CRAWLER, CrawlerRenderer::new);
		EntityRenderers.register(ModEntities.CURSED_SOUL, CursedSoulRenderer::new);
	}
}