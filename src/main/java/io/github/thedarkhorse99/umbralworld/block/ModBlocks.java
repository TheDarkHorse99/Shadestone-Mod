package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.world.entity.ai.village.poi.PoiType;

public class ModBlocks {
	public static final Block SHADESTONE_ORE = register(
			"shadestone_ore",
			properties -> new ShadestoneOreBlock(UniformInt.of(3, 7), properties),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE)
	);

	public static final Block SHADESTONE_BLOCK = register(
			"shadestone_block",
			ShadestoneBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK)
	);

	public static final Block BARROW_DIRT = register(
			"barrow_dirt",
			Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.COARSE_DIRT)
	);

	public static final Block UMBRAL_PORTAL = registerBlockOnly(
			"umbral_portal",
			UmbralPortalBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL).lightLevel(state -> 0)
	);

	public static final ResourceKey<PoiType> UMBRAL_PORTAL_POI =
			ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, UmbralWorld.id("umbral_portal"));

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, UmbralWorld.id(name));
		Block block = factory.apply(properties.setId(blockKey));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, UmbralWorld.id(name));
		BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	private static Block registerBlockOnly(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, UmbralWorld.id(name));
		Block block = factory.apply(properties.setId(blockKey));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS)
				.register(output -> output.accept(SHADESTONE_ORE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
				.register(output -> output.accept(SHADESTONE_BLOCK));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS)
				.register(output -> output.accept(BARROW_DIRT));
		PoiHelper.register(UmbralWorld.id("umbral_portal"), 0, 1, UMBRAL_PORTAL);
	}
}
