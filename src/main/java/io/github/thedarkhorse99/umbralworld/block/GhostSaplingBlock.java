package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.world.ModWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Grows into a Ghost or Phantom Tree. Unlike vanilla saplings it doesn't need light to grow. */
public class GhostSaplingBlock extends SaplingBlock {
    /** Blocks it can be planted on. Filled in by data/umbral_world/tags/block/supports_ghost_sapling.json */
    public static final TagKey<Block> SUPPORTS_GHOST_SAPLING =
            TagKey.create(Registries.BLOCK, UmbralWorld.id("supports_ghost_sapling"));

    /** Which tree to grow: always the one shape, no big or flowering variants. */
    public static final TreeGrower GHOST_TREE_GROWER = new TreeGrower(
            "umbral_world:ghost",
            WeightedList.of(ModWorldGen.GHOST_TREE),
            WeightedList.of(),
            WeightedList.of(),
            ModWorldGen.GHOST_TREE);

    public static final TreeGrower PHANTOM_TREE_GROWER = new TreeGrower(
            "umbral_world:phantom",
            WeightedList.of(ModWorldGen.PHANTOM_TREE),
            WeightedList.of(),
            WeightedList.of(),
            ModWorldGen.PHANTOM_TREE);

    public GhostSaplingBlock(TreeGrower treeGrower, BlockBehaviour.Properties properties) {
        super(treeGrower, properties);
    }

    /** Same speed as vanilla saplings, but skips their "needs light level 9" rule. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) == 0) {
            advanceTree(level, pos, state, random);
        }
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(SUPPORTS_GHOST_SAPLING);
    }
}
