package io.github.thedarkhorse99.umbralworld.world;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class ModWorldGen {
	public static final ResourceKey<PlacedFeature> SHADESTONE_ORE_PLACED =
			ResourceKey.create(Registries.PLACED_FEATURE, UmbralWorld.id("shadestone_ore"));

	/** The Ghost Tree feature defined in data/umbral_world/worldgen/feature/ghost_tree.json */
	public static final ResourceKey<Feature> GHOST_TREE =
			ResourceKey.create(Registries.FEATURE, UmbralWorld.id("ghost_tree"));

	public static void initialize() {
		// Lets JSON files use "type": "umbral_world:ghost_tree"
		Registry.register(BuiltInRegistries.FEATURE_TYPE, UmbralWorld.id("ghost_tree"), GhostTreeFeature.CODEC);

		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.UNDERGROUND_ORES,
				SHADESTONE_ORE_PLACED
		);
	}
}
