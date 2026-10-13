package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ground cover like Pink Petals: 1-4 little sprouts per block (use another on it
 * to add more), facing any way. Gives Slowness to players nearby.
 */
public class TorporSproutBlock extends FlowerBedBlock implements AuraPlant {
    public static final int COLOR = 0xC8323C; // red

    public TorporSproutBlock(BlockBehaviour.Properties properties) {
        // 3 = height of the hitbox in pixels, same as Pink Petals
        super(properties, 3);
    }

    @Override
    public Holder<MobEffect> auraEffect() {
        return MobEffects.SLOWNESS;
    }

    @Override
    public int auraColor() {
        return COLOR;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(SUPPORTS_MIASMA_PLANTS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        AuraPlant.spawnAuraParticle(level, pos, random, COLOR);
    }
}
