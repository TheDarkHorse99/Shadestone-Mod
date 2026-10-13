package io.github.thedarkhorse99.umbralworld.entity;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> WIGHT_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UmbralWorld.id("wight"));

	public static final EntityType<Wight> WIGHT = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			WIGHT_KEY,
			EntityType.Builder.of(Wight::new, MobCategory.MONSTER)
					.sized(0.6f, 2.9f)     // an Enderman's hitbox: 0.6 wide, 2.9 tall
					.eyeHeight(2.55f)      // an Enderman's eye height
					.clientTrackingRange(8)
					.notInPeaceful()
					.build(WIGHT_KEY));

	public static final ResourceKey<EntityType<?>> GHOUL_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UmbralWorld.id("ghoul"));

	// A "creature" like cows and pigs, not a monster: it never attacks, and it doesn't use up the Wights' spawn slots
	public static final EntityType<Ghoul> GHOUL = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			GHOUL_KEY,
			EntityType.Builder.of(Ghoul::new, MobCategory.CREATURE)
					.sized(0.7f, 1.6f)     // hunched over: shorter than a zombie (1.95)
					.eyeHeight(1.35f)
					.clientTrackingRange(10)
					.build(GHOUL_KEY));

	public static final ResourceKey<EntityType<?>> CRAWLER_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UmbralWorld.id("crawler"));

	public static final EntityType<Crawler> CRAWLER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			CRAWLER_KEY,
			EntityType.Builder.of(Crawler::new, MobCategory.MONSTER)
					.sized(0.8f, 0.6f)     // lying down: low and wide
					.eyeHeight(0.4f)
					.clientTrackingRange(8)
					.notInPeaceful()
					.build(CRAWLER_KEY));

	private ModEntities() {
	}

	public static void initialize() {
		// Health, damage, speed... Every living mob type must have its stats registered.
		FabricDefaultAttributeRegistry.register(WIGHT, Wight.createAttributes());

		// Where it may spawn naturally: on solid ground, in the dark, like vanilla monsters.
		SpawnPlacements.register(WIGHT, SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);

		FabricDefaultAttributeRegistry.register(GHOUL, Ghoul.createAttributes());
		// Ghouls only need solid ground: they don't care about light
		SpawnPlacements.register(GHOUL, SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);

		FabricDefaultAttributeRegistry.register(CRAWLER, Crawler.createAttributes());
		// Like vanilla monsters: on solid ground, in the dark
		SpawnPlacements.register(CRAWLER, SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
	}
}
