package io.github.thedarkhorse99.umbralworld.entity;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
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

	private ModEntities() {
	}

	public static void initialize() {
		// Health, damage, speed... Every living mob type must have its stats registered.
		FabricDefaultAttributeRegistry.register(WIGHT, Wight.createAttributes());

		// Where it may spawn naturally: on solid ground, in the dark, like vanilla monsters.
		SpawnPlacements.register(WIGHT, SpawnPlacementTypes.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
	}
}
