package io.github.thedarkhorse99.umbralworld.item;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

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

	public static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, UmbralWorld.id(name));
		Item item = factory.apply(properties.setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(output -> output.accept(SHADESTONE_SHARD));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(output -> output.accept(SHADESTONE_BLADE));
		ShadestoneBladeEvents.initialize();
	}
}
