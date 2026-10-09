package io.github.thedarkhorse99.umbralworld.block.entity;

import com.mojang.serialization.Codec;
import io.github.thedarkhorse99.umbralworld.block.UmbralPortalShape;
import io.github.thedarkhorse99.umbralworld.menu.DarkFurnaceMenu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DarkFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    // ---- Slots ----
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_RESULT = 1;
    public static final int SLOT_COUNT = 2;

    // ---- Numbers sent to the open GUI ----
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL_TIME = 1;
    public static final int DATA_DARK = 2;
    public static final int DATA_COUNT = 3;

    // ---- Tuning knobs ----
    /** Cooks only while the brightest spot touching the furnace is at or below this. Same as the portal. */
    public static final int MAX_LIGHT = UmbralPortalShape.MAX_LIGHT;
    /** Vanilla smelting takes 200 ticks (10 s). 1.5x makes it 300 ticks (15 s). */
    private static final float COOK_TIME_MULTIPLIER = 1.5f;
    /** How often to re-check the light, in ticks (10 = twice a second). */
    private static final int LIGHT_CHECK_INTERVAL = 10;

    private static final int[] SLOTS_FOR_UP_AND_SIDES = {SLOT_INPUT};
    private static final int[] SLOTS_FOR_DOWN = {SLOT_RESULT};

    /** How the "which recipes were cooked, and how many times" list is written into the save file. */
    private static final Codec<Map<ResourceKey<Recipe<?>>, Integer>> RECIPES_USED_CODEC =
            Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int cookingProgress;
    private int cookingTotalTime;
    private boolean dark;
    private int lightCheckTimer;
    /** Every recipe cooked since XP was last paid out, and how many times. XP is paid from this. */
    private final Map<ResourceKey<Recipe<?>>, Integer> recipesUsed = new HashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipeCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> cookingProgress;
                case DATA_TOTAL_TIME -> cookingTotalTime;
                case DATA_DARK -> dark ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> cookingProgress = value;
                case DATA_TOTAL_TIME -> cookingTotalTime = value;
                case DATA_DARK -> dark = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DarkFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DARK_FURNACE, pos, state);
    }

    // ---- The cooking loop: runs 20 times a second on the server ----

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, DarkFurnaceBlockEntity furnace) {
        furnace.lightCheckTimer--;
        if (furnace.lightCheckTimer <= 0) {
            furnace.lightCheckTimer = LIGHT_CHECK_INTERVAL;
            furnace.dark = brightestNeighbor(level, pos) <= MAX_LIGHT;
        }

        boolean cooking = false;
        boolean changed = false;
        ItemStack input = furnace.items.get(SLOT_INPUT);

        if (furnace.dark && !input.isEmpty()) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            Optional<RecipeHolder<SmeltingRecipe>> recipe = furnace.recipeCheck.getRecipeFor(recipeInput, level);
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().assemble(recipeInput);
                if (!result.isEmpty() && furnace.canFit(result)) {
                    cooking = true;
                    furnace.cookingTotalTime = (int) Math.ceil(recipe.get().value().cookingTime() * COOK_TIME_MULTIPLIER);
                    furnace.cookingProgress++;
                    if (furnace.cookingProgress >= furnace.cookingTotalTime) {
                        furnace.cookingProgress = 0;
                        furnace.finishCooking(input, result);
                        furnace.recipesUsed.merge(recipe.get().id(), 1, Integer::sum); // remember it for XP
                        changed = true;
                    }
                }
            }
        }

        if (!cooking && furnace.cookingProgress > 0) {
            // Light (or nothing to cook) makes progress fade, like a vanilla furnace cooling down.
            furnace.cookingProgress = Math.max(0, furnace.cookingProgress - 2);
        }

        if (state.getValue(AbstractFurnaceBlock.LIT) != cooking) {
            state = state.setValue(AbstractFurnaceBlock.LIT, cooking);
            level.setBlockAndUpdate(pos, state);
            changed = true;
        }

        if (changed) {
            setChanged(level, pos, state);
        }
    }

    private static int brightestNeighbor(Level level, BlockPos pos) {
        int brightest = 0;
        for (Direction direction : Direction.values()) {
            brightest = Math.max(brightest, level.getMaxLocalRawBrightness(pos.relative(direction)));
        }
        return brightest;
    }

    private boolean canFit(ItemStack result) {
        ItemStack current = items.get(SLOT_RESULT);
        if (current.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(current, result)) {
            return false;
        }
        return current.getCount() + result.getCount() <= Math.min(getMaxStackSize(), result.getMaxStackSize());
    }

    private void finishCooking(ItemStack input, ItemStack result) {
        ItemStack current = items.get(SLOT_RESULT);
        if (current.isEmpty()) {
            items.set(SLOT_RESULT, result.copy());
        } else {
            current.grow(result.getCount());
        }
        input.shrink(1);
    }

    // ---- Experience (copied from vanilla's furnace) ----

    /** Called when a player takes items out of the result slot: pays the stored XP and unlocks the recipes. */
    public void awardUsedRecipesAndPopExperience(ServerPlayer player) {
        List<RecipeHolder<?>> recipes = popExperience(player.level(), player.position());
        player.awardRecipes(recipes);
        for (RecipeHolder<?> recipe : recipes) {
            player.triggerRecipeCrafted(recipe, items);
        }
    }

    /** Drops XP orbs for everything cooked since the last payout, then clears the list. */
    private List<RecipeHolder<?>> popExperience(ServerLevel level, Vec3 position) {
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        for (Map.Entry<ResourceKey<Recipe<?>>, Integer> entry : recipesUsed.entrySet()) {
            level.recipeAccess().byKey(entry.getKey()).ifPresent(recipe -> {
                recipes.add(recipe);
                if (recipe.value() instanceof AbstractCookingRecipe cooking) {
                    dropExperience(level, position, entry.getValue(), cooking.experience());
                }
            });
        }
        recipesUsed.clear();
        setChanged();
        return recipes;
    }

    /** e.g. 3 iron ingots x 0.7 XP = 2.1 -> 2 XP, plus a 10% chance of 1 more. Same maths as vanilla. */
    private static void dropExperience(ServerLevel level, Vec3 position, int count, float xpEach) {
        float total = count * xpEach;
        int xp = Mth.floor(total);
        float leftover = Mth.frac(total);
        if (leftover != 0.0f && level.getRandom().nextFloat() < leftover) {
            xp++;
        }
        ExperienceOrb.award(level, position, xp);
    }

    /** Breaking the furnace drops its items (vanilla does that in super) and any XP it was holding. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel serverLevel) {
            popExperience(serverLevel, Vec3.atCenterOf(pos));
        }
    }

    // ---- Inventory ----

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack old = items.get(slot);
        boolean sameItem = !stack.isEmpty() && ItemStack.isSameItemSameComponents(old, stack);
        super.setItem(slot, stack);
        if (slot == SLOT_INPUT && !sameItem) {
            cookingProgress = 0; // a different item starts from scratch
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT; // nothing can be put into the result slot
    }

    // ---- Hoppers: top and sides feed the input, bottom pulls results ----

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_UP_AND_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_RESULT;
    }

    // ---- GUI ----

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.umbral_world.dark_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DarkFurnaceMenu(containerId, inventory, this, dataAccess);
    }

    // ---- Saving and loading ----

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        cookingProgress = input.getIntOr("cooking_progress", 0);
        cookingTotalTime = input.getIntOr("cooking_total_time", 0);
        recipesUsed.clear();
        recipesUsed.putAll(input.read("recipes_used", RECIPES_USED_CODEC).orElse(Map.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("cooking_progress", cookingProgress);
        output.putInt("cooking_total_time", cookingTotalTime);
        output.store("recipes_used", RECIPES_USED_CODEC, recipesUsed);
    }
}
