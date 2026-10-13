package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A cauldron holding 1-3 levels of Ectoplasm, filled by a spile on a Phantom Log.
 * Use a glass bottle on it to take one level out as an Ectoplasm Bottle, the same
 * way you'd take water from a water cauldron.
 */
public class EctoplasmCauldronBlock extends LayeredCauldronBlock {
    public EctoplasmCauldronBlock(BlockBehaviour.Properties properties) {
        // NONE: rain and snow don't fill it. The empty Dispatcher: no vanilla item uses (dyeing, buckets...)
        super(Biome.Precipitation.NONE, new CauldronInteraction.Dispatcher(), properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.GLASS_BOTTLE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (!level.isClientSide()) {
            // Swap one glass bottle for an Ectoplasm Bottle (or add it to the inventory if there's a stack)
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.ECTOPLASM_BOTTLE)));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Unlike water, Ectoplasm doesn't put out burning mobs (which would also use it up). */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    }
}
