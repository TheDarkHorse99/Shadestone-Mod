package io.github.thedarkhorse99.umbralworld.menu;

import io.github.thedarkhorse99.umbralworld.block.entity.DarkFurnaceBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;

/** Vanilla's furnace output slot, plus: taking items out pays the Dark Furnace's stored XP. */
public class DarkFurnaceResultSlot extends FurnaceResultSlot {
    private final Player player;

    public DarkFurnaceResultSlot(Player player, Container container, int slot, int x, int y) {
        super(player, container, slot, x, y);
        this.player = player;
    }

    @Override
    protected void checkTakeAchievements(ItemStack carried) {
        super.checkTakeAchievements(carried);
        if (player instanceof ServerPlayer serverPlayer && container instanceof DarkFurnaceBlockEntity furnace) {
            furnace.awardUsedRecipesAndPopExperience(serverPlayer);
        }
    }
}
