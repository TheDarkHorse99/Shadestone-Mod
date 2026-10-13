package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A single-block Miasma plant (Frail Bramble, Pale Blossoms) with an effect aura. */
public class MiasmaPlantBlock extends VegetationBlock implements AuraPlant {
    private final VoxelShape shape;
    private final Holder<MobEffect> effect;
    private final int color;

    public MiasmaPlantBlock(VoxelShape shape, Holder<MobEffect> effect, int color, BlockBehaviour.Properties properties) {
        super(properties);
        this.shape = shape;
        this.effect = effect;
        this.color = color;
    }

    @Override
    public Holder<MobEffect> auraEffect() {
        return effect;
    }

    @Override
    public int auraColor() {
        return color;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Flowers sit slightly off-centre at random; move the outline to match
        return shape.move(state.getOffset(pos));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(SUPPORTS_MIASMA_PLANTS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        AuraPlant.spawnAuraParticle(level, pos, random, color);
    }
}
