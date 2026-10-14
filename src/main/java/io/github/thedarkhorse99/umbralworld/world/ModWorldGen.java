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

	/** The rare Phantom Tree: same shape, purple blocks. data/umbral_world/worldgen/feature/phantom_tree.json */
	public static final ResourceKey<Feature> PHANTOM_TREE =
			ResourceKey.create(Registries.FEATURE, UmbralWorld.id("phantom_tree"));

	public static void initialize() {
		// Lets JSON files use "type": "umbral_world:ghost_tree" (used by both the Ghost and Phantom Trees)
		Registry.register(BuiltInRegistries.FEATURE_TYPE, UmbralWorld.id("ghost_tree"), GhostTreeFeature.CODEC);
		// The Chasm: Finger Weeds columns up the fissure walls, and Cinderite Ore in the fissure bottoms
		Registry.register(BuiltInRegistries.FEATURE_TYPE, UmbralWorld.id("finger_weeds"), FingerWeedsFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, UmbralWorld.id("cinderite_ore"), CinderiteOreFeature.CODEC);
		// Lets terrain JSON use "type": "umbral_world:chasm_web" (the Chasm's connected fissures)
		Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, UmbralWorld.id("chasm_web"), ChasmWebFunction.CODEC);

		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.UNDERGROUND_ORES,
				SHADESTONE_ORE_PLACED
		);
	}
}
