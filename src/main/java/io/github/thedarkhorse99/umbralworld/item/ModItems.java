package io.github.thedarkhorse99.umbralworld.item;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import io.github.thedarkhorse99.umbralworld.entity.ModEntities;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

public class ModItems {
	public static final TagKey<Item> REPAIRS_SHADESTONE =
			TagKey.create(Registries.ITEM, UmbralWorld.id("repairs_shadestone"));

	public static final ToolMaterial SHADESTONE_MATERIAL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_IRON_TOOL,
			150,   // durability
			6.0f,  // mining speed
			3.0f,  // attack damage bonus (same as diamond)
			15,    // enchantability
			REPAIRS_SHADESTONE
	);

	public static final Item SHADESTONE_SHARD = register("shadestone_shard", Item::new, new Item.Properties());

	public static final Item SHADESTONE_BLADE = register("shadestone_blade", Item::new,
			new Item.Properties().sword(SHADESTONE_MATERIAL, 3.0f, -2.4f));

	// Sap from the Phantom Tree. A brewing ingredient; the brewing stand hands back the glass bottle.
	public static final Item ECTOPLASM_BOTTLE = register("ectoplasm_bottle", Item::new,
			new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16));

	// What the Shroud looks like when worn: assets/umbral_world/equipment/wights_shroud.json
	public static final ResourceKey<EquipmentAsset> WIGHTS_SHROUD_ASSET =
			ResourceKey.create(EquipmentAssets.ROOT_ID, UmbralWorld.id("wights_shroud"));

	// Worn in the chest slot; invisible in the light (see WightsShroud). No armor and no durability, like an Elytra.
	public static final Item WIGHTS_SHROUD = register("wights_shroud", Item::new,
			new Item.Properties()
					.stacksTo(1)
					.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
							.setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
							.setAsset(WIGHTS_SHROUD_ASSET)
							.setDamageOnHurt(false)
							.build()));

	public static final Item WIGHT_SPAWN_EGG = register("wight_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.WIGHT));

	public static final Item GHOUL_SPAWN_EGG = register("ghoul_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.GHOUL));

	public static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, UmbralWorld.id(name));
		Item item = factory.apply(properties.setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(output -> output.accept(SHADESTONE_SHARD));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(output -> output.accept(ECTOPLASM_BOTTLE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(output -> {
					output.accept(SHADESTONE_BLADE);
					output.accept(WIGHTS_SHROUD);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(output -> {
					output.accept(WIGHT_SPAWN_EGG);
					output.accept(GHOUL_SPAWN_EGG);
				});
		ShadestoneBladeEvents.initialize();
		WightsShroud.initialize();
	}
}
