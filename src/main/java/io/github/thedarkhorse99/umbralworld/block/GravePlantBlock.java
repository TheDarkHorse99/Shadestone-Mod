package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small plant that grows on Barrow Dirt (and anything vanilla plants grow on).
 *
 * Planted on living soil (vanilla dirt, grass, moss or mud), it slowly comes back to life
 * as a tall vanilla flower: Withered Tufts become a Rose Bush, Grave Thorns a Lilac.
 */
public class GravePlantBlock extends VegetationBlock {
    /** Blocks these plants can be planted on. Filled in by data/umbral_world/tags/block/supports_grave_plants.json */
    public static final TagKey<Block> SUPPORTS_GRAVE_PLANTS =
            TagKey.create(Registries.BLOCK, UmbralWorld.id("supports_grave_plants"));
    /** Soil that brings the plant back to life. Filled in by data/umbral_world/tags/block/revives_grave_plants.json */
    public static final TagKey<Block> REVIVES_GRAVE_PLANTS =
            TagKey.create(Registries.BLOCK, UmbralWorld.id("revives_grave_plants"));
    /**
     * Chance per random tick. A block gets a random tick about every 68 seconds on average,
     * so 1 in 5 means a revived flower after about 6 minutes.
     */
    private static final float REVIVE_CHANCE = 0.2f;

    private final VoxelShape shape;
    /** The tall vanilla flower this plant turns into on living soil. */
    private final Block revivesInto;

    public GravePlantBlock(VoxelShape shape, Block revivesInto, BlockBehaviour.Properties properties) {
        super(properties);
        this.shape = shape;
        this.revivesInto = revivesInto;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(SUPPORTS_GRAVE_PLANTS);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= REVIVE_CHANCE
                || !level.getBlockState(pos.below()).is(REVIVES_GRAVE_PLANTS)
                || !level.getBlockState(pos.above()).isAir()) {
            return; // the flower is two blocks tall, so it needs room above
        }
        DoublePlantBlock.placeAt(level, revivesInto.defaultBlockState(), pos, Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                10, 0.3, 0.6, 0.3, 0.0);
        level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}
