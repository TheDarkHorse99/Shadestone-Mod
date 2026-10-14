package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ModBlocks {
	public static final Block SHADESTONE_ORE = register(
			"shadestone_ore",
			properties -> new ShadestoneOreBlock(UniformInt.of(3, 7), properties),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE)
	);

	public static final Block SHADESTONE_BLOCK = register(
			"shadestone_block",
			ShadestoneBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
	);

	public static final Block BARROW_DIRT = register(
			"barrow_dirt",
			Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.COARSE_DIRT)
	);

	// The Miasma's soil. A plain block for now; it doesn't spread like grass.
	public static final Block EFFLUVIUM = register(
			"effluvium",
			Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
					.mapColor(MapColor.COLOR_PURPLE)
					.sound(SoundType.GRASS)
	);

	// The Chasm's rock: its flat land and the first few layers down. Below it is Shadestone.
	public static final Block DEAD_SEA_SHALE = register(
			"dead_sea_shale",
			Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF)
	);

	// ---- Ghost Tree (Miasma) ----

	public static final Block GHOST_LOG = register(
			"ghost_log",
			GhostLogBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_LIGHT_BLUE)
					.strength(2.0F)
					.sound(SoundType.CHERRY_WOOD)
					.noOcclusion()
	);

	public static final Block GHOST_PLANKS = register(
			"ghost_planks",
			GhostPlanksBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
					.mapColor(MapColor.COLOR_LIGHT_BLUE)
					.sound(SoundType.CHERRY_WOOD)
					.noOcclusion()
	);

	// Block only: players never hold Ghost Vapor
	public static final Block GHOST_VAPOR = registerBlockOnly(
			"ghost_vapor",
			properties -> new GhostVaporBlock(ParticleTypes.END_ROD, properties),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.NONE)
					.noCollision()
					.noOcclusion()
					.replaceable()
					.randomTicks()
					.lightLevel(_ -> 3)
					.sound(SoundType.EMPTY)
					.pushReaction(PushReaction.POPPED)
	);

	public static final Block GHOST_SAPLING = register(
			"ghost_sapling",
			properties -> new GhostSaplingBlock(GhostSaplingBlock.GHOST_TREE_GROWER, properties),
			BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)
	);

	// ---- Phantom Tree (Miasma): the rare purple Ghost Tree, the only one that gives Ectoplasm ----

	public static final Block PHANTOM_LOG = register(
			"phantom_log",
			GhostLogBlock::new,
			BlockBehaviour.Properties.ofFullCopy(GHOST_LOG)
					.mapColor(MapColor.COLOR_PURPLE)
	);

	public static final Block PHANTOM_PLANKS = register(
			"phantom_planks",
			GhostPlanksBlock::new,
			BlockBehaviour.Properties.ofFullCopy(GHOST_PLANKS)
					.mapColor(MapColor.COLOR_PURPLE)
	);

	public static final Block PHANTOM_VAPOR = registerBlockOnly(
			"phantom_vapor",
			properties -> new GhostVaporBlock(ParticleTypes.REVERSE_PORTAL, properties),
			BlockBehaviour.Properties.ofFullCopy(GHOST_VAPOR)
	);

	public static final Block PHANTOM_SAPLING = register(
			"phantom_sapling",
			properties -> new GhostSaplingBlock(GhostSaplingBlock.PHANTOM_TREE_GROWER, properties),
			BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)
	);

	// ---- Ectoplasm: a spile on a Phantom Log drips it into a cauldron ----

	public static final Block SPILE = register(
			"spile",
			SpileBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(0.5F)
					.sound(SoundType.LANTERN)
					.noCollision()
					.noOcclusion()
					.randomTicks()
					.pushReaction(PushReaction.POPPED)
	);

	// Block only: picking it up gives a normal cauldron, like vanilla's Water Cauldron
	public static final Block ECTOPLASM_CAULDRON = registerBlockOnly(
			"ectoplasm_cauldron",
			EctoplasmCauldronBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)
	);

	// ---- Miasma plants: each gives an effect to players standing nearby ----

	public static final Block FRAIL_BRAMBLE = register(
			"frail_bramble",
			properties -> new MiasmaPlantBlock(Block.column(14.0, 0.0, 13.0), MobEffects.WEAKNESS, 0x9B4DCA, properties),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH)
					.mapColor(MapColor.COLOR_PURPLE)
					.sound(SoundType.SWEET_BERRY_BUSH)
	);

	public static final Block PALE_BLOSSOMS = register(
			"pale_blossoms",
			properties -> new MiasmaPlantBlock(Block.column(6.0, 0.0, 10.0), MobEffects.NAUSEA, 0xF2F2F2, properties),
			BlockBehaviour.Properties.ofFullCopy(Blocks.OXEYE_DAISY)
	);

	public static final Block TORPOR_SPROUT = register(
			"torpor_sprout",
			TorporSproutBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_PETALS)
					.mapColor(MapColor.COLOR_RED)
	);

	// ---- Corpse Candle: a floating orb of fire, dropped by Ghouls ----

	public static final Block CORPSE_CANDLE = register(
			"corpse_candle",
			CorpseCandleBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.FIRE)
					.noCollision()
					.noOcclusion()
					.instabreak()
					.randomTicks()
					.lightLevel(_ -> 4)
					.sound(SoundType.CANDLE)
					.pushReaction(PushReaction.POPPED)
	);

	public static final Block UMBRAL_PORTAL = registerBlockOnly(
			"umbral_portal",
			UmbralPortalBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL).lightLevel(_ -> 0)
	);

	public static final Block DARK_FURNACE = register(
			"dark_furnace",
			DarkFurnaceBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
	);

	public static final Block WITHERED_TUFTS = register(
			"withered_tufts",
			properties -> new GravePlantBlock(Block.column(12.0, 0.0, 13.0), Blocks.ROSE_BUSH, properties),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH)
					.randomTicks() // to come back to life on vanilla soil
	);

	public static final Block GRAVE_THORNS = register(
			"grave_thorns",
			GraveThornsBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK)
					.noCollision()
					.randomTicks() // to come back to life on vanilla soil
					.sound(SoundType.SWEET_BERRY_BUSH)
					.pushReaction(PushReaction.POPPED)
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
				.register(output -> {
					output.accept(BARROW_DIRT);
					output.accept(EFFLUVIUM);
					output.accept(DEAD_SEA_SHALE);
					output.accept(GHOST_LOG);
					output.accept(GHOST_SAPLING);
					output.accept(PHANTOM_LOG);
					output.accept(PHANTOM_SAPLING);
					output.accept(FRAIL_BRAMBLE);
					output.accept(PALE_BLOSSOMS);
					output.accept(TORPOR_SPROUT);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
				.register(output -> {
					output.accept(GHOST_PLANKS);
					output.accept(PHANTOM_PLANKS);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS)
				.register(output -> {
					output.accept(WITHERED_TUFTS);
					output.accept(GRAVE_THORNS);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(output -> {
					output.accept(DARK_FURNACE);
					output.accept(SPILE);
					output.accept(CORPSE_CANDLE);
				});
		// Middle-clicking an Ectoplasm Cauldron picks a normal cauldron, like vanilla's filled cauldrons
		Item.BY_BLOCK.put(ECTOPLASM_CAULDRON, Items.CAULDRON);
		PoiHelper.register(UmbralWorld.id("umbral_portal"), 0, 1, UMBRAL_PORTAL);
	}
}
