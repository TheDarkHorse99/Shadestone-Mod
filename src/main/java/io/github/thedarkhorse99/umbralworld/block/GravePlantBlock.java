package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small plant that grows on Barrow Dirt (and anything vanilla plants grow on). */
public class GravePlantBlock extends VegetationBlock {
    /** Blocks these plants can be planted on. Filled in by data/umbral_world/tags/block/supports_grave_plants.json */
    public static final TagKey<Block> SUPPORTS_GRAVE_PLANTS =
            TagKey.create(Registries.BLOCK, UmbralWorld.id("supports_grave_plants"));

    private final VoxelShape shape;

    public GravePlantBlock(VoxelShape shape, BlockBehaviour.Properties properties) {
        super(properties);
        this.shape = shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(SUPPORTS_GRAVE_PLANTS);
    }
}