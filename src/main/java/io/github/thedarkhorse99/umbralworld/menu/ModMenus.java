package io.github.thedarkhorse99.umbralworld.menu;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final MenuType<DarkFurnaceMenu> DARK_FURNACE = Registry.register(
            BuiltInRegistries.MENU,
            UmbralWorld.id("dark_furnace"),
            new MenuType<>(DarkFurnaceMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }

    public static void initialize() {
    }
}