package io.github.thedarkhorse99.umbralworld.world;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class ModWorldGen {
	public static final ResourceKey<PlacedFeature> SHADESTONE_ORE_PLACED =
			ResourceKey.create(Registries.PLACED_FEATURE, UmbralWorld.id("shadestone_ore"));

	public static void initialize() {
		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.UNDERGROUND_ORES,
				SHADESTONE_ORE_PLACED
		);
	}
}
