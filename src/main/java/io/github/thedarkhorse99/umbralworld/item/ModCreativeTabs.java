package io.github.thedarkhorse99.umbralworld.item;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** The mod's own tab in the creative inventory. */
public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> UMBRAL_WORLD_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, UmbralWorld.id("umbral_world"));

	public static final CreativeModeTab UMBRAL_WORLD = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			UMBRAL_WORLD_KEY,
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.umbral_world"))
					.icon(() -> new ItemStack(ModItems.SHADESTONE_SHARD))
					.displayItems((parameters, output) -> {
						// Blocks first, then items, like vanilla's own tabs
						output.accept(ModBlocks.SHADESTONE_BLOCK);
						output.accept(ModBlocks.SHADESTONE_ORE);
						output.accept(ModBlocks.BARROW_DIRT);
						output.accept(ModBlocks.DARK_FURNACE);
						output.accept(ModItems.SHADESTONE_SHARD);
						output.accept(ModItems.SHADESTONE_BLADE);
					})
					.build());

	private ModCreativeTabs() {
	}

	public static void initialize() {
	}
}
