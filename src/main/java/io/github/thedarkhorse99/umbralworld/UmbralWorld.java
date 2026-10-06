package io.github.thedarkhorse99.umbralworld;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.thedarkhorse99.umbralworld.item.ModItems;


public class UmbralWorld implements ModInitializer {
	public static final String MOD_ID = "umbral_world";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.initialize();
		LOGGER.info("The darkness stirs beneath the bedrock...");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
