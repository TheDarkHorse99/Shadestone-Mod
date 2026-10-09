package io.github.thedarkhorse99.umbralworld.menu;

import io.github.thedarkhorse99.umbralworld.block.entity.DarkFurnaceBlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipePropertySet;

public class DarkFurnaceMenu extends AbstractContainerMenu {
    // Slot numbers inside this menu: 0 = input, 1 = result, then the player's inventory.
    private static final int INPUT = DarkFurnaceBlockEntity.SLOT_INPUT;
    private static final int RESULT = DarkFurnaceBlockEntity.SLOT_RESULT;
    private static final int INV_START = 2;     // 27 backpack slots: 2..28
    private static final int HOTBAR_START = 29; // 9 hotbar slots: 29..37
    private static final int HOTBAR_END = 38;   // (end numbers are "stop before")

    private final Container container;
    private final ContainerData data;
    private final RecipePropertySet smeltable;

    /** Used on the player's computer, which starts with empty placeholders the server then fills in. */
    public DarkFurnaceMenu(int containerId, Inventory inventory) {
        this(containerId, inventory,
                new SimpleContainer(DarkFurnaceBlockEntity.SLOT_COUNT),
                new SimpleContainerData(DarkFurnaceBlockEntity.DATA_COUNT));
    }

    /** Used on the server, connected to the real furnace. */
    @SuppressWarnings("resource")
    public DarkFurnaceMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.DARK_FURNACE, containerId);
        checkContainerSize(container, DarkFurnaceBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, DarkFurnaceBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.smeltable = inventory.player.level().recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT);

        addSlot(new Slot(container, INPUT, 56, 17));
        addSlot(new FurnaceResultSlot(inventory.player, container, RESULT, 116, 35));
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    /** Shift-click behaviour. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == RESULT) {
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index == INPUT) {
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (smeltable.test(stack)) {
            if (!moveItemStackTo(stack, INPUT, INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    /** 0.0 to 1.0: how far along the current item is. Drives the arrow. */
    public float getCookProgress() {
        int progress = data.get(DarkFurnaceBlockEntity.DATA_PROGRESS);
        int total = data.get(DarkFurnaceBlockEntity.DATA_TOTAL_TIME);
        if (total == 0 || progress == 0) {
            return 0.0f;
        }
        return Mth.clamp((float) progress / total, 0.0f, 1.0f);
    }

    /** True while it is dark enough to cook. Drives the darkness icon. */
    public boolean isDark() {
        return data.get(DarkFurnaceBlockEntity.DATA_DARK) != 0;
    }
}