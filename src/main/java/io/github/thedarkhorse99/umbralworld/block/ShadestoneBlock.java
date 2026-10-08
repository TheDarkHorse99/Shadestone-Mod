package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.item.ModItems;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ShadestoneBlock extends Block {
    public ShadestoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        ShadestoneSmoke.spawn(level, pos, random, 1.0f);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(ModItems.SHADESTONE_SHARD)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        BlockPos inside = pos.relative(hitResult.getDirection());
        Optional<UmbralPortalShape> found = UmbralPortalShape.findEmptyShape(level, inside);
        if (found.isEmpty()) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        UmbralPortalShape shape = found.get();
        BlockPos center = shape.getCenter();
        int light = shape.getBrightestLight(serverLevel);

        if (light > UmbralPortalShape.MAX_LIGHT) {
            serverLevel.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.6f);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    20, 0.4, 0.6, 0.4, 0.01);
            player.sendOverlayMessage(Component.translatable("umbral_world.portal.too_bright", light));
            return InteractionResult.SUCCESS;
        }

        shape.createPortalBlocks(serverLevel);
        stack.consume(1, player);
        serverLevel.playSound(null, center, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6f, 0.5f);
        return InteractionResult.SUCCESS;
    }
}