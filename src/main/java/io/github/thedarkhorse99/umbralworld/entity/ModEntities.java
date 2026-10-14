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

	public static final ResourceKey<EntityType<?>> CURSED_SOUL_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UmbralWorld.id("cursed_soul"));

	public static final EntityType<CursedSoul> CURSED_SOUL = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			CURSED_SOUL_KEY,
			EntityType.Builder.of(CursedSoul::new, MobCategory.MONSTER)
					.sized(0.5f, 0.5f)     // a small floating wisp
					.eyeHeight(0.25f)
					.fireImmune()
					.clientTrackingRange(8)
					.notInPeaceful()
					.build(CURSED_SOUL_KEY));

	public static final ResourceKey<EntityType<?>> MURKFIEND_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UmbralWorld.id("murkfiend"));

	// Swims lying flat, like a dolphin, so its hitbox is as wide as it is tall
	public static final EntityType<Murkfiend> MURKFIEND = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			MURKFIEND_KEY,
			EntityType.Builder.of(Murkfiend::new, MobCategory.MONSTER)
					.sized(0.9f, 0.9f)
					.eyeHeight(0.5f)
					.clientTrackingRange(8)
					.notInPeaceful()
					.build(MURKFIEND_KEY));

	public static final ResourceKey<EntityType<?>> CHASM_SKATE_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UmbralWorld.id("chasm_skate"));

	public static final EntityType<ChasmSkate> CHASM_SKATE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			CHASM_SKATE_KEY,
			EntityType.Builder.of(ChasmSkate::new, MobCategory.MONSTER)
					.sized(1.2f, 0.35f)    // wide and flat, lying on the bottom
					.eyeHeight(0.25f)
					.clientTrackingRange(8)
					.notInPeaceful()
					.build(CHASM_SKATE_KEY));

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

		FabricDefaultAttributeRegistry.register(CURSED_SOUL, CursedSoul.createAttributes());
		// In mid-air is fine (it flies), but only near Ghost or Phantom Vapor
		SpawnPlacements.register(CURSED_SOUL, SpawnPlacementTypes.NO_RESTRICTIONS,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CursedSoul::checkCursedSoulSpawnRules);

		// The Chasm's water monsters: only in the water at the bottom of the fissures, in the dark
		FabricDefaultAttributeRegistry.register(MURKFIEND, Murkfiend.createAttributes());
		SpawnPlacements.register(MURKFIEND, SpawnPlacementTypes.IN_WATER,
				Heightmap.Types.OCEAN_FLOOR, ChasmSwimmer::checkChasmWaterSpawnRules);

		FabricDefaultAttributeRegistry.register(CHASM_SKATE, ChasmSkate.createAttributes());
		// On the floor of the water
		SpawnPlacements.register(CHASM_SKATE, SpawnPlacementTypes.IN_WATER,
				Heightmap.Types.OCEAN_FLOOR, ChasmSkate::checkChasmSkateSpawnRules);
	}
}
