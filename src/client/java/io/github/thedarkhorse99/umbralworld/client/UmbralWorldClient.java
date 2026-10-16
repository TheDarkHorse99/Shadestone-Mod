package io.github.thedarkhorse99.umbralworld.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import io.github.thedarkhorse99.umbralworld.particle.ModParticles;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import io.github.thedarkhorse99.umbralworld.block.entity.ModBlockEntities;
import io.github.thedarkhorse99.umbralworld.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import io.github.thedarkhorse99.umbralworld.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;

public class UmbralWorldClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModRenderPipelines.initialize();
		ShadestoneToolSmoke.initialize();
		ParticleProviderRegistry.getInstance().register(ModParticles.CORPSE_FLAME, CorpseFlameParticle.Provider::new);
		// Each biome's drifting dust: vanilla's ash, recoloured (the Grave keeps vanilla's grey ash)
		ParticleProviderRegistry.getInstance().register(ModParticles.MIASMA_DUST, sprites -> new TintedAshProvider(sprites, 0.80f, 0.66f, 0.95f));
		ParticleProviderRegistry.getInstance().register(ModParticles.CHASM_DUST, sprites -> new TintedAshProvider(sprites, 0.74f, 0.84f, 0.95f));
		MenuScreens.register(ModMenus.DARK_FURNACE, DarkFurnaceScreen::new);
		// The Corpse Candle: a see-through blue candle bobbing above the ground
		ModelLayerRegistry.registerModelLayer(CorpseCandleRenderer.LAYER, CorpseCandleRenderer::createLayer);
		BlockEntityRendererRegistry.register(ModBlockEntities.CORPSE_CANDLE, CorpseCandleRenderer::new);
		// The Wight's Shroud: a 3D poncho on the Wight (see WightRenderer) and on players and armor stands wearing it
		ModelLayerRegistry.registerModelLayer(ShroudModel.LAYER, ShroudModel::createLayer);
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new ShroudLayer<>(avatar, context.getModelSet(), false));
			} else if (renderer instanceof ArmorStandRenderer stand) {
				helper.register(new ShroudLayer<>(stand, context.getModelSet(), false));
			}
		});
		EntityRenderers.register(ModEntities.WIGHT, WightRenderer::new);
		ModelLayerRegistry.registerModelLayer(GhoulModel.LAYER, GhoulModel::createBodyLayer);
		EntityRenderers.register(ModEntities.GHOUL, GhoulRenderer::new);
		ModelLayerRegistry.registerModelLayer(CrawlerModel.LAYER, CrawlerModel::createBodyLayer);
		EntityRenderers.register(ModEntities.CRAWLER, CrawlerRenderer::new);
		EntityRenderers.register(ModEntities.CURSED_SOUL, CursedSoulRenderer::new);
		ModelLayerRegistry.registerModelLayer(MurkfiendModel.LAYER, MurkfiendModel::createBodyLayer);
		EntityRenderers.register(ModEntities.MURKFIEND, MurkfiendRenderer::new);
		ModelLayerRegistry.registerModelLayer(ChasmSkateModel.LAYER, ChasmSkateModel::createBodyLayer);
		EntityRenderers.register(ModEntities.CHASM_SKATE, ChasmSkateRenderer::new);
	}
}