package io.github.thedarkhorse99.umbralworld.block.entity;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.block.ModBlocks;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<DarkFurnaceBlockEntity> DARK_FURNACE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            UmbralWorld.id("dark_furnace"),
            new BlockEntityType<>(DarkFurnaceBlockEntity::new, Set.of(ModBlocks.DARK_FURNACE)));
    private ModBlockEntities() {
    }

    public static void initialize() {
    }
}